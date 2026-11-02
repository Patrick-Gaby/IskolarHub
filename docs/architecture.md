# IskolarHub architecture decisions

Status: Java/Spring Boot backend, Phase 1 (foundation). Earlier Python/Celery/outbox design notes are
historical (see `MIGRATION.md`) and are superseded by the decisions below.

## Layering (backend, package root `ph.iskolarhub`)
Controller -> service -> Spring Data JPA repository -> PostgreSQL. The eligibility rules engine (Phase 3) is
plain Java with no Spring MVC or JPA dependencies. DTOs are used at the API boundary; entities are never returned.

## Time and deadline-timezone policy
- Timestamps are stored as `timestamptz` (UTC).
- A date-only scholarship deadline means the **end of that date in Asia/Manila (UTC+8)**, converted to a UTC instant.
- The API returns ISO-8601 UTC instants; the frontend displays deadlines in Asia/Manila.
- Business logic never uses the server's default time zone.

## Notifications (Phase 8)
Persisted `notification_events` rows with unique idempotency keys; a Spring `@Scheduled` processor claims due
rows safely (row locking), retries with backoff, records attempts and failures. Reminders: 7 days and 1 day before
the deadline; hourly scan; none for expired scholarships. Redis is not used.

## Error format
Every failure returns `{code, message, fieldErrors?, requestId, timestamp}`. Exception messages and stack
traces are never returned. `X-Request-Id` is accepted (if well-formed) or generated, echoed, and logged.

## Remaining work before any public deployment
HTTPS, production secret management, database backups, logging/monitoring, rate limiting, JWT
expiry/revocation strategy, real email provider configuration, and a security review.
