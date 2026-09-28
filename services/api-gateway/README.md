        # api-gateway

        Single entry point: routing, JWT validation, rate limiting, correlation IDs

        - Port: **8080**
        - Run locally: `mvn spring-boot:run` (start infra first: `docker compose up -d` from repo root)
        - Health: `http://localhost:8080/actuator/health`
        - Owner: see `docs/team-plan.md`
        