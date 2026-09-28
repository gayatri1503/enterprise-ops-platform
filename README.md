# Enterprise Operations Portal

Scalable microservices-based enterprise web platform with secure authentication and real-time analytics (B.Tech final-year project).

## Architecture in one glance

    Browser (React) -> API Gateway :8080 -> auth :8081 | user :8082 | core :8083 | analytics :8084 | notification :8085
                                             each service owns its own Postgres database
    core -> RabbitMQ (events) -> analytics (Redis + live push) and notification
    auth -> RabbitMQ (security audit events)

Full details: [`docs/architecture.md`](docs/architecture.md).

## Repo layout

| Path | What | Default owner |
| --- | --- | --- |
| `frontend/` | React + TypeScript UI | Student 1 |
| `services/api-gateway/` | Routing, JWT check, rate limit | Student 2 |
| `services/auth-service/` | Login, tokens, RBAC source | Student 2 |
| `services/user-service/` | Profiles, departments, roles | Student 2 |
| `services/core-service/` | Work items + domain events | Student 3 |
| `services/analytics-service/` | KPIs + real-time push | Student 4 |
| `services/notification-service/` | In-app notifications | Student 4 |
| `infra/`, `docker-compose.yml`, `tests/load/` | DevOps, load tests | Student 4 |
| `docs/` | Contracts, team plan | Everyone |

Work split and sprint tasks: [`docs/team-plan.md`](docs/team-plan.md).
Git rules: [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Prerequisites

Git, Docker Desktop, JDK 21, Maven 3.9+, Node 20+.

## Quick start

    cp .env.example .env            # then edit the passwords
    docker compose up -d            # postgres, redis, rabbitmq only

Run **your own** service from your IDE / terminal (fastest way to work independently):

    cd services/core-service && mvn spring-boot:run
    cd frontend && npm install && npm run dev

Or run everything in containers:

    docker compose --profile apps up --build

Smoke test: open http://localhost:3000 (Docker) or http://localhost:5173 (dev). All five services should show **UP**. RabbitMQ UI: http://localhost:15672.

## Ports

| Thing | Port |
| --- | --- |
| Gateway | 8080 |
| auth / user / core / analytics / notification | 8081 / 8082 / 8083 / 8084 / 8085 |
| Frontend (dev / docker) | 5173 / 3000 |
| Postgres / Redis / RabbitMQ | 5432 / 6379 / 5672 |
