# Team plan

Four people, four areas, one shared set of contracts (`docs/api-contract.md`, `docs/events.md`).
Because the contracts are agreed first, everyone can build and test **without waiting** for the others.

> Assign names here: Student 1 = ____ , Student 2 = ____ , Student 3 = ____ , Student 4 = ____
> Then update `.github/CODEOWNERS` with GitHub usernames.

## Ownership

| | Area | Owns (folders) | Also responsible for |
| --- | --- | --- | --- |
| **Student 1** | Frontend lead | `frontend/` | UI tests, usability, demo polish |
| **Student 2** | Security / Gateway lead | `api-gateway`, `auth-service`, `user-service` | Security test cases, audit records |
| **Student 3** | Backend / Data lead | `core-service`, DB schema of core, API docs (OpenAPI) | DB indexing/performance, domain events |
| **Student 4** | Analytics / DevOps lead | `analytics-service`, `notification-service`, `infra/`, `docker-compose.yml`, `tests/load/` | CI, observability, load testing |

Viva rule: everyone must be able to explain one full request flow end to end (login -> create item -> event -> dashboard).

## How each person works independently (no blockers)

| Student | Until the others are ready, use... |
| --- | --- |
| 1 Frontend | Mock API (fake JSON responses) that follows `api-contract.md`; switch to the real gateway when ready. |
| 2 Security | Self-contained: auth + user service, test with curl/Postman. Others get a pre-agreed dev JWT format from the contract. |
| 3 Core | Trust a hard-coded test JWT (correct claims) until auth is done; publish events to RabbitMQ from day one. |
| 4 Analytics | Publish **fake events** to RabbitMQ per `events.md` to build and test consumers without core. |

## Sprint 1 - Foundations (each deliverable is independent)

| Student | Deliverable |
| --- | --- |
| 1 | App shell: routing, login page, protected routes, single API client (token attach + refresh-once logic), layout with role-aware nav |
| 2 | auth-service: users table, BCrypt, `POST /login`, `/refresh` (rotation), `/logout`; user-service: `/users/me`; gateway JWT filter |
| 3 | core-service: WorkItem entity + Flyway migration, create/get/list with filters + pagination, validation, standard error body |
| 4 | RabbitMQ exchange/queues setup, fake event publisher script, analytics consumer skeleton with idempotency table, CI green, `docker compose --profile apps up` works |

## Sprint 2 - Core flow

1: work-item list/create/detail screens - 2: RBAC on all endpoints, admin user APIs, audit events - 3: status/assign/comment endpoints, publish real domain events, seed data - 4: KPI aggregation (Redis + Postgres), `/analytics/summary`, notification consumer + API

## Sprint 3 - Real-time + hardening

1: dashboard cards/charts, WS/SSE client, notifications bell - 2: rate limiting, secure headers/CORS, refresh-token reuse detection, security test report - 3: caching-friendly queries, indexes, OpenAPI docs, load-test endpoints - 4: WS/SSE push, load tests (k6), polling-vs-push experiment, cache on/off experiment

## Sprint 4 - Integrate, evaluate, finish

Everyone: integration on `main`, bug bash, demo script (see the guide's section 15), seeded demo data, report chapters split by ownership (each writes about what they built), slides, screen recording as demo fallback.

## Integration points (agree early, keep stable)

1. JWT claims (Student 2 <-> everyone)
2. Endpoint shapes + error body (Student 3 <-> Student 1)
3. Event envelope + types (Student 3 <-> Student 4)
4. Route table in gateway (Student 2 <-> everyone)
