# IskolarHub

Scholarship eligibility platform for Filipino students. Eligibility results come from rules configured in
IskolarHub and do **not** guarantee acceptance by a scholarship provider.

**Implementation status: Phase 1 of 10 (backend foundation).** Implemented: Spring Boot app, PostgreSQL +
Flyway, health endpoints, global error handling, CORS, deny-by-default security, and a status page in the
React frontend. **Not implemented yet:** accounts, profiles, scholarships, eligibility rules, dashboard,
subscriptions, email. See `docs/MIGRATION.md` and `docs/architecture.md`.

| Part | Technology |
|---|---|
| Backend (`backend/`) | Java 21, Spring Boot 3.5.6, Maven (wrapper), PostgreSQL 16, Flyway |
| Frontend (`frontend/`) | React 18, TypeScript, Vite, Tailwind CSS, Vitest |
| Local services | Docker Compose: PostgreSQL 16, Mailpit |

## 1. Required software
Docker Desktop (Compose v2), JDK 21 (`java -version` must report 21), Node.js 20+ and npm, Git.
Maven is **not** required: `mvnw` downloads the pinned Maven 3.9.11 on first use (needs internet once).

| Service | Address |
|---|---|
| Frontend (Vite) | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
| Mailpit UI / SMTP | http://localhost:8025 / localhost:1025 |

## 2. Configure
Run from the repository root.

PowerShell:
```powershell
Copy-Item .env.example .env
```
macOS/Linux:
```bash
cp .env.example .env
```
Edit `.env` if you want a different database password (keep `POSTGRES_PASSWORD` and `DB_PASSWORD` equal).
`.env` is git-ignored. Never commit it.

## 3. Start PostgreSQL and Mailpit (terminal 1, repo root)
```
docker compose up -d
docker compose ps
```
Wait until `postgres` shows `healthy`.

## 4. Start the backend (terminal 2, `backend/` directory)
Flyway migrations run automatically at startup.

PowerShell:
```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```
macOS/Linux:
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
The backend reads `../.env`, so start it from `backend/`. Stop with `Ctrl+C`.

## 5. Start the frontend (terminal 3, `frontend/` directory)
```
cd frontend
npm install        # first time; commit the generated package-lock.json
npm run dev
```
Open http://localhost:5173. The page should say "Ready: the API and database are working."
(`npm ci` works once `package-lock.json` is committed.)

## 6. Verify
```
curl http://localhost:8080/api/v1/actuator/health/liveness     -> {"status":"UP"}
curl http://localhost:8080/api/v1/actuator/health/readiness    -> {"status":"UP"}  (includes a database check)
```
PowerShell users: use `curl.exe` (plain `curl` is an alias for `Invoke-WebRequest`).
Database check: `docker compose exec postgres psql -U iskolarhub -d iskolarhub -c "select version, success from flyway_schema_history"`
should list version `1` with `success = t`.
Mailpit (no emails are sent until Phase 8): http://localhost:8025.

## 7. Tests
```
cd backend
./mvnw test          # Windows: .\mvnw.cmd test
cd ../frontend
npm test
npm run typecheck
npm run build
```
Backend integration tests use **Testcontainers** (a throwaway `postgres:16-alpine`) and need a running Docker
daemon. Without Docker those tests are *skipped*, not failed, so check the Maven summary for "Skipped".
`ErrorHandlingTest` needs no Docker.

## 8. Stop and reset
Stop (data kept): `docker compose down`
**Destructive reset** — deletes ALL local database data (the `pgdata` volume): `docker compose down -v`

## 9. Docker launch of the backend (Mode B, unverified)
```
docker compose --profile app up -d --build
```
Builds and runs the backend in a container next to Postgres. This path has **not been tested yet**; the
frontend still runs with `npm run dev`. A full containerised stack is a Phase 10 item.

## 10. Troubleshooting
- `Could not resolve placeholder 'DB_URL'`: `.env` missing, or you did not start from `backend/`.
- `Connection refused` / `password authentication failed`: Postgres not healthy yet, or `.env` passwords differ
  from the ones the existing volume was created with (see reset above).
- `Port 5432/8080/5173 already in use`: stop the other program; ports are fixed by this guide.
- `mvnw: Permission denied` (macOS/Linux): `chmod +x backend/mvnw`.
- `mvnw` cannot download Maven: check your network/proxy. Alternative: install Maven 3.9.x and run `mvn` instead.
- Frontend shows "Unavailable": the backend is not running on port 8080.
- Wrong Java: `java -version` must report 21.

## 11. Local vs production
This setup is for development only: HTTP, dev passwords, `ddl-auto=validate` with Flyway, loopback-bound
services. Remaining production work (HTTPS, secret management, backups, monitoring, rate limiting, JWT
expiry/revocation strategy, real email provider) is listed in `docs/architecture.md`.
