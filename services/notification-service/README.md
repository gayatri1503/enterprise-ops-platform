        # notification-service

        Consumes events, creates in-app notifications

        - Port: **8085**
        - Run locally: `mvn spring-boot:run` (start infra first: `docker compose up -d` from repo root)
        - Health: `http://localhost:8085/actuator/health`
        - Owner: see `docs/team-plan.md`
        