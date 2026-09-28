        # core-service

        Work items, comments, assignment, status workflow, publishes domain events

        - Port: **8083**
        - Run locally: `mvn spring-boot:run` (start infra first: `docker compose up -d` from repo root)
        - Health: `http://localhost:8083/actuator/health`
        - Owner: see `docs/team-plan.md`
        