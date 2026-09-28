        # analytics-service

        Consumes events, aggregates KPIs, pushes live updates (WebSocket/SSE)

        - Port: **8084**
        - Run locally: `mvn spring-boot:run` (start infra first: `docker compose up -d` from repo root)
        - Health: `http://localhost:8084/actuator/health`
        - Owner: see `docs/team-plan.md`
        