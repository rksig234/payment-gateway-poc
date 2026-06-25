# Deployment guide

Three ways to run the system: **full local (Docker Compose)**, **hybrid (infra in Docker, apps in IDE)**,
and **AWS (Terraform + ECS Fargate)** driven by the CI/CD pipeline.

---

## 1. Full local — Docker Compose

Builds an image per service (parameterized `Dockerfile`) and starts everything on one network.

```bash
docker compose up -d --build        # first run builds 5 images — takes a few minutes
docker compose ps                   # all healthy?
```

Endpoints (published to the host):

| Service | URL |
|---------|-----|
| Platform (gateway + status + retry + notification) | http://localhost:8090 |
| Stubs   | http://localhost:8080 |
| Jaeger  | http://localhost:16686 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |

Smoke test the full flow:

```bash
curl -i -X POST localhost:8090/v1/payments -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: key-001' \
  -d '{"amount":1500,"currency":"INR","rail":"UPI","payeeVpa":"merchant@bank","customerId":"CUST-9182"}'

# then watch the lifecycle (status domain owns reads):
curl localhost:8090/v1/payments/<paymentId>/timeline
```

Teardown: `docker compose down -v`.

> Inside Compose, services reach Kafka on `kafka:9094` (internal listener) and the host reaches it on
> `localhost:9092` (external listener). That dual-listener setup is already configured.

## 2. Hybrid — infra in Docker, apps in the IDE

Best for development/debugging:

```bash
docker compose up -d kafka kafka-init redis postgres jaeger prometheus grafana
# then run each *Application main class from IntelliJ (they default to localhost:9092 / localhost:5432)
```

## 3. Build images manually

```bash
docker build --build-arg MODULE=gateway-routing-service --build-arg PORT=8090 -t gateway:dev .
# MODULE must equal the module dir name; repeat per service with its port.
```

## 4. AWS — Terraform + ECS Fargate

Layout (`infra/`):

```
infra/
├── backend.tf                 # S3 + DynamoDB remote state (uncomment after creating them)
├── modules/
│   ├── network/               # VPC, public/private subnets, NAT, security groups
│   ├── rds-postgres/          # Multi-AZ Postgres
│   ├── elasticache-redis/     # Redis
│   ├── msk-cluster/           # 3-broker Kafka
│   └── ecs-service/           # Fargate task def + service (+ optional ALB target group)
└── envs/poc/                  # wires the modules; one ECS service per app via for_each
```

Deploy:

```bash
cd infra/envs/poc
terraform init
terraform plan  -var="db_password=<choose>" -var="image_tag=<git-sha-or-latest>"
terraform apply -var="db_password=<choose>" -var="image_tag=<git-sha-or-latest>"
terraform output alb_dns_name      # public entry point for the gateway
```

Prerequisites: an AWS account + credentials, ECR repos (created by the CI pipeline or `aws ecr create-repository`),
and the images pushed for the chosen `image_tag`.

**Skeleton caveats (intended follow-ups):** this provisions the network, data stores, MSK, and ECS services.
Still to wire for a hardened deploy: ECS **Service Connect / Cloud Map** for service-to-service discovery
(so the gateway resolves `payment-stubs`), **Secrets Manager** for the DB/Redis credentials (currently passed
as task env), Cognito/JWT at the ALB, and HTTPS (ACM cert on the listener). Run `terraform validate` and review
before applying to a real account.

## 5. CI/CD — GitHub Actions (`.github/workflows/ci-cd.yml`)

Pipeline on push to `main`: **build & test** (`mvn verify`) → **build & push** one image per service to ECR
(matrix build, OIDC auth) → **terraform plan & apply** gated by the `production` GitHub Environment (manual approval).

Required repo configuration:
- Secret `AWS_DEPLOY_ROLE_ARN` — an IAM role trusted for GitHub OIDC with ECR + Terraform permissions.
- Variable `AWS_REGION` (defaults to `ap-south-1`).
- A `production` Environment with required reviewers (the manual approval gate).
