        # auth-service

        Credentials, login, refresh-token rotation, security audit events

        - Port: **8081**
        - Run locally: `mvn spring-boot:run` (start infra first: `docker compose up -d` from repo root)
        - Health: `http://localhost:8081/actuator/health`
        - Owner: see `docs/team-plan.md`
        