# POC 01 — Unified Payment Processing Gateway
## In-depth requirements analysis & build plan

_Source: `POC01_Payment_Gateway.pptx` (blueprint) and `POC01_Payment_Gateway_Build_Design.pptx` (engineering reference v1.0)._
_This document captures the full requirement set, then narrows to what Step 1 (Module B — provider stubs) delivers._

---

## 1. Problem statement

The gateway exists to fix four concrete failures in the current payment setup:

1. **No fallback on failure** — when UPI or NEFT goes down, every transaction on that rail fails; there is no automatic rerouting to a healthy provider.
2. **Duplicate payments** — network retries cause double debits because there is no idempotency control.
3. **Zero visibility** — there is no single view of where a payment is across provider hops; debugging means trawling separate service logs.
4. **Hardcoded routing** — provider selection is baked into each service, so adding a rail requires code changes and redeploys.

The build must deliver dynamic rules-driven routing, resilient failure handling (circuit breakers + retry/DLQ), exactly-once semantics via idempotency keys, and end-to-end traceability.

## 2. Scope

**In scope:** gateway routing, three provider stubs (UPI / NEFT / Card), payment status tracking, retry/DLQ, notifications, the observability stack, and data lineage to Athena.

**Out of scope:** real provider integrations, settlement/reconciliation, refunds, multi-currency, PCI cardholder data storage, and production load testing.

## 3. Target architecture

Two paths run side by side:

- **Synchronous request path:** `Client → API Gateway (auth, throttle) → Gateway Routing Service (rules + idempotency) → Provider stub (UPI / NEFT / Card)`. Redis backs the idempotency store with a 24h TTL; a Resilience4j circuit breaker wraps each provider.
- **Asynchronous event/data path:** the Gateway and consumers exchange events over **Apache Kafka (AWS MSK)** on four topics — `payment.initiated`, `payment.success`, `payment.failed`, `payment.retry`. Downstream sit the Payment Status Service, Retry & DLQ Consumer, Notification Service, and an S3 lineage archive (Kafka Connect sink).

## 4. Service catalog

| # | Service | Responsibility | Trigger | Data store |
|---|---------|----------------|---------|------------|
| 1 | Gateway Routing Service | Receives requests, applies routing rules, enforces idempotency before any provider call | HTTP (sync) | Redis |
| 2 | **UPI Provider Stub** | Simulates UPI with configurable latency/failure rate to exercise the circuit breaker | HTTP (sync) | In-memory |
| 3 | **NEFT Provider Stub** | Simulates NEFT batch-style processing with delayed async acknowledgement | HTTP (async ack) | In-memory |
| 4 | **Card Provider Stub** | Simulates a card network incl. a 3DS-like challenge step and timeout scenarios | HTTP (sync) | In-memory |
| 5 | Payment Status Service | Tracks the payment state machine end to end with timestamps per transition | Kafka consumer | PostgreSQL |
| 6 | Retry & DLQ Consumer | Consumes `payment.retry` with exponential backoff; escalates to DLQ after 5 attempts | Kafka consumer | Kafka DLQ |
| 7 | Notification Service | Publishes payment outcome events via SNS to trigger customer-facing alerts | Kafka consumer | SNS |

**Bold rows (2, 3, 4) are Module B — the subject of Step 1.**

## 5. API contracts

### Gateway Routing Service (Module A — later)
- `POST /v1/payments` (headers `Idempotency-Key`, `X-Trace-Id`) → `201 Created` with `paymentId`, `status: PROCESSING`, `provider`, `traceId`. Duplicate key within 24h → `200` with `X-Idempotent-Replayed: true` and no provider call.
- `GET /v1/payments/{paymentId}` → current status.

### Provider stubs (Module B — this step)

| Stub | Endpoint | Success response | Config knobs |
|------|----------|------------------|--------------|
| UPI | `POST /internal/upi/debit` | `200 { status: SUCCESS, utr }` | `latencyMs` 200–800, `failureRate` 0–100% |
| NEFT | `POST /internal/neft/transfer` | `202 { status: PROCESSING, ackEta }` | `ackDelayMin` 5–120, `batchWindow` 30m |
| Card | `POST /internal/card/charge` | `200 { status: SUCCESS, challenge: 3DS_PASSED }` | `timeoutRate` 0–15%, `challengeRate` 0–30% |

Request bodies: UPI `{ amount, vpa, refId }`; NEFT `{ amount, ifsc, refId }`; Card `{ amount, panToken, refId }`.

### Payment Status Service (Module C — later)
Consumes all `payment.*` events, owns the state machine `INITIATED → PROCESSING → SUCCESS | FAILED`, and exposes `GET /v1/payments/{id}/timeline`.

### Retry & DLQ Consumer / Notification Service (Module C — later)
Kafka-only surfaces. Retry backoff `1s → 2s → 4s → 8s → DLQ (after attempt 5)`; a Spring Batch job replays the DLQ daily. Notifications fan out to an SNS topic.

## 6. Shared schemas

**Payment object** fields: `paymentId` (`PAY-`+UUID), `amount` (decimal, 2dp, >0), `currency` (ISO 4217), `rail` (`UPI|NEFT|CARD`), `status` (`INITIATED|PROCESSING|SUCCESS|FAILED`), `customerId`, `provider`, `retries` (int, default 0), `traceId`, `createdAt`, `updatedAt`.

**Error envelope:** `{ "error": { "code", "message", "traceId", "timestamp" } }`.

**Standard error codes:** `VALIDATION_ERROR` (400), `IDEMPOTENCY_CONFLICT` (409), `PROVIDER_UNAVAILABLE` (503), `PAYMENT_NOT_FOUND` (404), `RATE_LIMITED` (429). _This build adds `PROVIDER_TIMEOUT` (504) to model the Card stub's timeout scenario explicitly._

## 7. Kafka topics (AWS MSK)

| Topic | Producer | Consumers | Partitions | Retention |
|-------|----------|-----------|------------|-----------|
| `payment.initiated` | Gateway Routing | Status, S3 Sink | 3 | 7 days |
| `payment.success` | Gateway Routing | Status, Notification, S3 Sink | 3 | 7 days |
| `payment.failed` | Gateway Routing | Status, Notification, Retry | 3 | 7 days |
| `payment.retry` | Retry & DLQ Consumer | Retry & DLQ Consumer (self) | 3 | 3 days |

## 8. Data layer

- **PostgreSQL** holds durable payment state (`payments` table keyed by `payment_id`, indexed on `(customer_id, created_at DESC)`), plus a `dlq_records` table for the daily review job.
- **Redis** holds idempotency keys: `idem:{key} → { paymentId, status, responseBody }`, `TTL 86400s`, written with `SET … EX 86400 NX` so the first concurrent request atomically wins the key.

## 9. Resilience configuration

Resilience4j circuit breaker per provider: `slidingWindowSize 10`, `failureRateThreshold 50`, `waitDurationInOpenState 15s`, `permittedCallsInHalfOpenState 3`, `slowCallDurationThreshold 2s`, `slowCallRateThreshold 50`. When a breaker is OPEN the Gateway consults a fallback order (e.g. UPI → Card). Retry policy (Kafka consumer): `maxAttempts 5`, `waitDuration 1s`, exponential backoff multiplier 2.

## 10. Observability contract

Every service must emit:
- **Metrics (Prometheus):** `payment_requests_total{provider,status}`, `payment_latency_seconds{provider}` (histogram), `circuitbreaker_state{provider}`, `kafka_consumer_lag{topic,group}`, `dlq_depth{topic}`.
- **Traces (OpenTelemetry → Jaeger):** a span per hop; `traceId` propagated via `traceparent` header on HTTP and via Kafka message headers.
- **Structured JSON logs:** every line carries `traceId` + `paymentId`; no PII beyond `customerId`. Shipped to CloudWatch (30-day retention). CloudWatch alarm: success rate < 95% for 2 min pages on-call.

## 11. Infrastructure & deployment

One container per service on **AWS ECS Fargate**, provisioned via Terraform (`modules/ecs-service`, `msk-cluster`, `rds-postgres`, `elasticache-redis`; `envs/poc`). MSK is a 3-broker cluster, RDS a Multi-AZ `db.t3.medium`, Redis a single node. An ALB routes `/v1/*` to the Gateway. CI/CD (GitHub Actions): `build → test → docker push to ECR → terraform plan → manual approve → deploy`.

## 12. Security

AWS Cognito issues JWTs; the API Gateway validates the token before forwarding (no re-validation downstream). Secrets (DB creds, Redis auth, stub config) come from AWS Secrets Manager at container start. All services run in private subnets; only the ALB is public. Each ECS task has a scoped IAM role. No real cardholder data is stored — the Card stub uses tokenized placeholders, so PCI DSS scope does not apply.

## 13. Module breakdown & build sequencing

The seven services group into four buildable modules:

- **Module A — Routing & Idempotency Core:** Gateway Routing Service, Redis idempotency store, circuit breaker per provider.
- **Module B — Provider Stubs:** UPI, NEFT, Card stubs. _← Step 1._
- **Module C — Payment Lifecycle & Eventing:** Status Service, Retry & DLQ Consumer, Notification Service, Kafka backbone (4 topics).
- **Module D — Observability & Data Lineage:** tracing, metrics/dashboards, S3 → Glue → Athena lineage.

**Build order (per the sequencing slide):** **B → A → C → D**. Stubs are dependency-free and give every later module something real to call; routing is then tested against the stubs; eventing wires the full happy + failure paths; observability is added once there is real traffic to watch.

## 14. Technology decisions for this POC

| Area | Deck says | This build uses | Rationale |
|------|-----------|-----------------|-----------|
| Language | Java 17 | **Java 21 (LTS)** | Requested by the team; latest LTS, virtual-threads ready. |
| Framework | Spring Boot 3 | **Spring Boot 3.4.x** | Supports Java 21; built-in structured logging. |
| Build | (unspecified) | **Single Spring Boot app (Maven)** | One deployable; rails are packages (`upi`/`neft`/`card`) sharing `common`. Simplest to build and run for the POC. |
| Messaging | Kafka / MSK | Kafka (Module C) | Stubs in Module B don't need Kafka yet. |
| Idempotency | Redis | Redis (Module A) | Not needed by stubs. |
| Resilience | Resilience4j | Resilience4j (Module A) | Breakers live in the Gateway; stubs only *trigger* them. |
| Metrics | Prometheus | **Micrometer + Prometheus registry** | Already wired into the stubs so dashboards work from day one. |

## 15. What Step 1 (Module B) delivers

One Spring Boot application (port 8080) with four packages — `common`, `upi`, `neft`, `card`:

```
payment-gateway-poc/
└── src/main/java/com/sigmoid/paymentgateway/
    ├── PaymentGatewayPocApplication.java   single entry point (port 8080)
    ├── common/   shared enums, error envelope, trace filter, metric names, id helpers
    ├── upi/      POST /internal/upi/debit
    ├── neft/     POST /internal/neft/transfer  + GET .../transfer/{refId}
    └── card/     POST /internal/card/charge     + POST .../challenge/{id}
```

Each rail provides: its contract endpoint(s); **runtime-tunable config knobs** via `POST /internal/admin/{upi|neft|card}/config` (so a demo can open/close the circuit breaker without restarts); the shared error envelope and standard error codes; `X-Trace-Id` propagation on every request and response; and Prometheus metrics (`payment_requests_total`, `payment_latency_seconds`) exposed at `/actuator/prometheus`.

Faithful behaviours:
- **UPI** sleeps `latencyMs` then succeeds with a UTR or fails `failureRate`% of the time with `503 PROVIDER_UNAVAILABLE` — the input the breaker's `failureRateThreshold` needs.
- **NEFT** accepts with `202 PROCESSING` + `ackEta`, parks the transfer in memory, and a batch settler flips it to `SUCCESS` after the delay (a demo-only `ackDelaySeconds` override avoids waiting minutes).
- **Card** passes frictionlessly (`SUCCESS`/`3DS_PASSED`), or returns `REQUIRES_CHALLENGE` + a `challengeId` to complete via a second call, or hangs `timeoutMs` then fails `504 PROVIDER_TIMEOUT` — exercising the breaker's slow-call detection.

## 16. How Module B sets up the next modules

The `common` library deliberately front-loads cross-cutting contracts so Modules A/C/D inherit them:
- `Rail` / `PaymentStatus` enums and the **error envelope** are shared, so the Gateway and Status Service serialize identically.
- The **trace filter** establishes the `X-Trace-Id` propagation contract that Module D promotes to full OpenTelemetry.
- **Metric names** match the observability contract exactly, so Grafana dashboards built in Module D need no renaming.

When Module A is built next, the Gateway Routing Service calls these three stubs over HTTP, wraps each call in a Resilience4j breaker, and uses `failureRate` / `timeoutRate` to drive breaker state during demos — with no change to the stubs.
