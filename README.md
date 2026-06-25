# Unified Payment Processing Gateway — POC 01

Spring Boot (Java 21) implementation of POC 01, organized as **two applications** plus a shared library:

```
payment-gateway-poc/                 Maven reactor
├── common/            shared events, DTOs, error envelope, enums, trace, topic names
├── payment-platform/  :8090  the platform you own (modular monolith)
│   ├── gateway/        Module A — routing, Redis idempotency, circuit breakers, event publishing
│   ├── status/         Module C — Postgres state machine + timeline (owns payment reads)
│   ├── retry/          Module C — exponential backoff + DLQ + replay
│   └── notification/   Module C — channels (Push/SMS/Webhook) + AWS SNS
└── payment-stubs/     :8080  Module B — UPI / NEFT / Card (simulated external providers)
```

The platform and the stubs talk over HTTP (gateway → provider). Inside the platform, the domains
still communicate through **real Kafka topics** (`payment.initiated/success/failed/retry` + `payment.dlq`),
backed by **Redis** (idempotency), **PostgreSQL** (lifecycle), and **Resilience4j** (per-provider breakers).

## Prerequisites

- JDK 21, Maven 3.6.3+
- Docker (for Kafka / Redis / Postgres / Jaeger / Prometheus / Grafana)

## Run it (two ways)

**A) Everything in Docker:**
```bash
docker compose up -d --build      # builds 2 app images + infra
```

**B) Infra in Docker, apps from your IDE / Maven:**
```bash
docker compose up -d kafka kafka-init redis postgres jaeger prometheus grafana
mvn clean install
mvn -pl payment-stubs    spring-boot:run   # :8080
mvn -pl payment-platform spring-boot:run   # :8090  (separate terminal)
```

Tests (no infra needed): `mvn test`.

## The end-to-end flow

```bash
# 1) initiate a payment (gateway) — note the Idempotency-Key
curl -i -X POST localhost:8090/v1/payments -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: key-001' \
  -d '{"amount":1500,"currency":"INR","rail":"UPI","payeeVpa":"merchant@bank","customerId":"CUST-9182"}'
# → 201 PROCESSING  (re-send same key → 200 + X-Idempotent-Replayed: true)

# 2) read the lifecycle from the status domain (Postgres source of truth)
curl localhost:8090/v1/payments/<paymentId>
curl localhost:8090/v1/payments/<paymentId>/timeline

# 3) demo the breaker + fallback: make the UPI provider fail, then send another UPI payment
curl -X POST localhost:8080/internal/admin/upi/config -H 'Content-Type: application/json' -d '{"failureRate":100}'
#   → gateway breaker trips, routes to CARD; payment.failed + payment.retry flow to the retry domain

# 4) inspect / replay the DLQ
curl localhost:8090/admin/dlq
curl -X POST localhost:8090/admin/dlq/<paymentId>/replay
```

## Key endpoints

| App | Endpoint | Purpose |
|-----|----------|---------|
| platform | `POST /v1/payments` | Initiate (Idempotency-Key required) |
| platform | `GET /v1/payments/{id}` · `/{id}/timeline` | Status + lifecycle history |
| platform | `GET /admin/dlq` · `POST /admin/dlq/{id}/replay` | DLQ inspect + replay |
| platform | `GET /actuator/prometheus` · `/health` · `/circuitbreakers` | Metrics + breaker state |
| stubs | `POST /internal/{upi/debit, neft/transfer, card/charge}` | Provider rails |
| stubs | `GET/POST /internal/admin/{upi,neft,card}/config` | Tunable failure/latency/etc. |

## Deployment

See [`DEPLOYMENT.md`](./DEPLOYMENT.md) for Docker images, full Compose, and the AWS path
(Terraform modules under `infra/` + ECS Fargate + the GitHub Actions pipeline).

## Status vs the 100% roadmap

Done: Module B (stubs), Module A (routing/idempotency/breakers/events), Module C (status, retry/DLQ,
notification), Dockerization, Compose, Terraform/ECS blueprint, CI/CD. **Next: Module D** (OpenTelemetry →
Jaeger, Grafana dashboards, S3 → Glue → Athena lineage), then security (Cognito/Secrets Manager) and
OpenAPI docs.

> Java is **21** (decks specify 17, 21 requested). `PROVIDER_TIMEOUT` (504) was added to the standard
> error codes for the Card timeout scenario; everything else follows the design docs.
