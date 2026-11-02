# Python → Java migration record

## Historical: original scaffold (removed in Phase 1 of the Java plan)
A FastAPI + SQLAlchemy + Alembic + Celery + Redis scaffold with 11 pytest tests, written but **never
executed** (no package registry was reachable). It contained no domain code and no data. It was deleted, not
ported; only the React frontend, Docker Compose infrastructure and policy documents were kept.

## Decisions fixed for the Java backend
- Health: Spring Boot Actuator under `/api/v1/actuator/health/{liveness,readiness}`; readiness = readinessState + db.
- Tests: Testcontainers `postgres:16-alpine` (Docker required; skipped when absent).
- Auth (Phase 4): Spring Security OAuth2 resource server (Nimbus JWT, HS256), Argon2id via BouncyCastle.
- Maven wrapper: minimal script-only wrapper written for this repo. If it fails on your machine, regenerate
  with `mvn -N wrapper:wrapper -Dmaven=3.9.11` and commit the result.
- `TimeConfig`/`DeadlinePolicy` are deferred to the first phase that uses them (Phase 2/5) rather than shipped as unused code.
- `OpenApiConfig` is deferred until the first real endpoint exists.
- Flyway: `V1__baseline.sql` (no tables); domain tables start at `V2`.

## Phase 1 file changes
Removed: all of `backend/` (Python), `docker/postgres/init-test-db.sql`, Redis service.
Created: `backend/` Java project (pom, wrapper, config, exception handling, security, tests, Dockerfile), `docs/MIGRATION.md`, `.gitattributes`.
Modified: `docker-compose.yml`, `.env.example`, `.gitignore`, `README.md`, `docs/architecture.md`,
`frontend/vite.config.ts` (proxy → 127.0.0.1:8080), `frontend/src/api/client.ts`, `frontend/src/App.tsx`, `frontend/src/App.test.tsx`.
