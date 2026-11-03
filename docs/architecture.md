# IskolarHub architecture decisions

Status: Java/Spring Boot backend; Phase 3 (eligibility rules engine) in progress, Cluster 3.1 (contracts).
Earlier Python/Celery/outbox design notes are historical (see `MIGRATION.md`) and are superseded by the
decisions below.

## Layering (backend, package root `ph.iskolarhub`)
Controller -> service -> Spring Data JPA repository -> PostgreSQL. The eligibility rules engine (Phase 3) is
plain Java with no Spring MVC or JPA dependencies. DTOs are used at the API boundary; entities are never returned.

## Time and deadline-timezone policy
- Timestamps are stored as `timestamptz` (UTC).
- A date-only scholarship deadline means the **end of that date in Asia/Manila (UTC+8)**, converted to a UTC instant.
- The API returns ISO-8601 UTC instants; the frontend displays deadlines in Asia/Manila.
- Business logic never uses the server's default time zone.

## Eligibility engine (Phase 3)
Package `ph.iskolarhub.eligibility`, plain Java: no Spring, JPA, Hibernate, repositories or I/O. The only
project classes it imports from outside its own package are the existing plain enums `GwaScale` and
`RuleType`; `EngineIndependenceTest` enforces this. The Spring service (Cluster 3.8) loads
`StudentProfile` and `ScholarshipRule` rows and copies them into the engine's own input records.

Contracts (Cluster 3.1):
- Input: `EvaluationProfile` (immutable copy of the student's values; null = not provided, never zero) and
  `RuleDefinition` (rule type + deep-copied, not yet validated JSON config).
- Evaluator: `RuleEvaluator` (`supportedRuleType()`, `evaluate(profile, rule)`). One implementation per rule
  type (Clusters 3.3 to 3.7). Missing or incomparable student data returns an INCOMPLETE result; an invalid
  config throws `InvalidRuleConfigurationException` and is never turned into a result or a pass.
- Output: `RuleEvaluationResult` (status PASSED/FAILED/INCOMPLETE, `RuleReason`, fixed safe message, missing
  `ProfileField`s) and `EligibilityResult` (overall ELIGIBLE/INELIGIBLE/INCOMPLETE, rule results, missing fields).
- Valid combinations are enforced by constructors: PASSED+REQUIREMENT_MET and FAILED+REQUIREMENT_NOT_MET carry
  no missing fields; INCOMPLETE carries PROFILE_VALUE_MISSING (or GWA_SCALE_MISMATCH for GWA) and exactly the
  one field the rule reads.
- Combination (AND) is `EligibilityResult.from(results)`: any FAILED gives INELIGIBLE (a failure beats missing
  information); else any INCOMPLETE gives INCOMPLETE; else ELIGIBLE (also when there are no rules). Rule results
  are ordered by `RuleType` declaration order; duplicate rule types are rejected.
- Messages are fixed text and never contain profile values, thresholds or configuration contents.
- Publication status (only PUBLISHED scholarships reach students) and deadlines are application-layer concerns,
  not engine concerns. The `EligibilityEngine` orchestrator arrives in Cluster 3.8.

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
