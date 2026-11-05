# Engineering Handbook: Standards, Quality, Security and Decisions

*Companion to the PRD. This is the rulebook for how the agent (and any human developer) writes, tests, secures and ships the Hotel Channel Manager. It combines: coding standards, architecture decisions, sync design, Definition of Done, test plan, security and privacy checklist, performance targets, and deployment guide.*

## 1. Repository layout

```
channel-manager/
  backend/                  Spring Boot (Java 21, Maven or Gradle)
    src/main/java/com/<company>/cm/
      identity/  property/  inventory/  rates/  channels/
      reservations/  notifications/  reporting/  common/
    src/main/resources/db/migration/   Flyway: V001__init.sql ...
    src/test/java/...
  frontend/                 React + TypeScript + Vite
    src/features/  (auth, dashboard, calendar, channels, bookings, ...)
    src/components/  src/lib/  src/styles/
  docs/                     PRD, business rules, user stories, API spec, ADRs
  docker-compose.yml  .github/workflows/  README.md  CHANGELOG.md
```

Each backend module has the packages `api` (controllers, DTOs), `domain` (entities, business logic), `infra` (repositories, external clients) and `events`. Modules talk only through public service interfaces or events, never through each other's repositories.

## 2. Coding standards

### 2.1 Backend (Java and Spring Boot)

- Java 21, Spring Boot 3.x, constructor injection only, no field injection.
- Controllers are thin: validate input, call a service, return a DTO. Business logic lives in services and domain classes.
- DTOs are separate from JPA entities. Use records for DTOs. Map with MapStruct or hand-written mappers.
- Transactions are declared on service methods. Keep them short. No remote calls to channels inside a database transaction (use the outbox).
- One global `@ControllerAdvice` returns the standard error format from the API contract, with plain-language messages from a message bundle (`messages.properties`), not hardcoded strings.
- Validation with Jakarta Validation annotations plus explicit business rule checks that cite their rule ID in a comment, for example `// BR-1.5`.
- Money as `long` minor units plus currency, never `double` or `float`.
- Time: store UTC `Instant`; use the property's `ZoneId` to turn a stay date into a date. Inject a `Clock` so time can be faked in tests.
- Logging: structured JSON, include `traceId`, `organizationId`, `propertyId`. Never log credentials, tokens, guest emails, phone numbers or full payloads.
- No `System.out`, no swallowed exceptions, no TODO without a ticket reference.
- Static analysis: Checkstyle or Spotless, SpotBugs, and OWASP dependency check in CI.

### 2.2 Frontend (React and TypeScript)

- TypeScript in strict mode; no `any` without a comment explaining why.
- Feature folders; shared components in `components/`. Components under about 200 lines; extract hooks for logic.
- Server data through TanStack Query; no data fetching directly in components without it. Generate the API client from the OpenAPI file; never hand-write request types.
- Forms with React Hook Form and Zod schemas. Error messages come from the message catalog.
- All user-visible text lives in a translation file (i18next) from day one, using the wording from the Plain-Language Guide.
- Accessibility linting (`eslint-plugin-jsx-a11y`) and Storybook accessibility checks in CI.
- Formatting by Prettier and ESLint, enforced by a pre-commit hook and CI.
- Never store tokens in `localStorage`. Use httpOnly secure cookies for the refresh token, and keep the access token in memory.

### 2.3 Git and reviews

- Trunk-based with short-lived branches named `feat/`, `fix/`, `chore/`. Conventional commit messages (`feat(calendar): bulk update preview`).
- Every change goes through a pull request with description, linked story ID and screenshots for UI changes.
- A PR must pass CI, include tests and update docs when behavior changes.
- Review checklist: business rule cited, tenant scoping checked, error messages in plain language, no secrets, accessibility checked, no needless complexity.

## 3. Architecture decision records (initial decisions)

Each record: context, decision, consequences. The agent adds new ones in `docs/adr/` using the same format.

| ID | Decision | Reason | Consequence |
| --- | --- | --- | --- |
| ADR-001 | **Modular monolith** | Small team, one deployable, easier to test and run; modules can be split later | Strict module boundaries are enforced with ArchUnit tests |
| ADR-002 | **PostgreSQL as the source of truth**, with a check constraint on inventory | Strong transactions and constraints protect against overbooking | All inventory writes go through one service |
| ADR-003 | **Transactional outbox** for events to channels | Guarantees no lost or phantom updates between database and queue | A publisher process polls `outbox_events` and sends to the broker |
| ADR-004 | **Message broker: RabbitMQ** (Kafka only if volume demands it) | Simpler to run; supports retry queues and dead-letter exchanges well | If one property sends more than about 1,000 updates per second, revisit |
| ADR-005 | **Row-level locking on `daily_inventory`** (ordered locks) | Serializes bookings per room and night without global locks | Always lock in the order room type then date ascending to avoid deadlocks |
| ADR-006 | **Connector plugin interface** with capability flags | New channels are added without touching core code | Each connector has contract tests and a sandbox mode |
| ADR-007 | **Credentials encrypted with AES-256-GCM**, key from KMS or environment secret | Credentials must never be readable in the database | Key rotation procedure is documented and tested |
| ADR-008 | **JWT access tokens (15 min) + rotating refresh tokens** | Stateless API with revocable sessions | Refresh tokens stored hashed, rotated on use |
| ADR-009 | **Server-Sent Events for live status** | Simpler than WebSockets for one-way updates | Falls back to polling every 15 seconds |
| ADR-010 | **Idempotency keys** on writes and on inbound channel messages | Retries and duplicate webhooks must never double-apply | Stored in `idempotency_keys` and unique constraints |

## 4. Sync design

### 4.1 Outbound: a change reaches every channel

1. User saves a change. The API validates, then in **one database transaction** updates `daily_inventory` or `daily_rates`, writes an `audit_log` entry and inserts an `outbox_events` row.
2. The **outbox publisher** reads unpublished events in order and sends them to the broker, then marks them published.
3. The **sync planner** consumes each event and creates one `sync_jobs` row per active, mapped channel, applying allotment caps, buffers and price adjustments (BR-1.6, 1.7, 2.7). Older pending jobs for the same `target_key` are marked `SUPERSEDED` (BR-3.5).
4. A **channel worker** takes due jobs, respects the connector's rate limit and batch size, calls the connector, and records the result. Success sets `SUCCESS` and updates `last_success_at`.
5. On failure the job is rescheduled with backoff (30 s, 2 min, 10 min, 1 h, 6 h). After the last attempt it becomes `DEAD`, a notification is created and the channel health turns red.
6. An SSE event `sync.job.updated` updates the screen badge.

### 4.2 Inbound: a booking arrives

1. A webhook or poller fetches a reservation message and stores the raw payload.
2. The message is checked for a valid signature and a duplicate (`channel_account_id + channel_reference`, or a message ID). Duplicates are acknowledged and ignored.
3. The **reservation processor** runs in one transaction: look up mappings, lock the affected `daily_inventory` rows in order, check availability, create or update the reservation, adjust `sold`, write `audit_log` and outbox events. If availability is missing it creates the reservation as `CONFLICT` and a `conflicts` row, and does not change `sold`.
4. After commit, outbox events trigger availability updates to all other channels and, if required, an acknowledgement back to the source channel.
5. Notifications and SSE events inform the user.

### 4.3 Reconciliation

A nightly job per channel (off-peak in the property's timezone) re-sends the full horizon in batches, and, where the channel supports reading back, compares and logs differences. It also finds sync jobs stuck in `SENDING` for more than 10 minutes and resets them.

### 4.4 Connector contract tests

Every connector must pass the same shared test suite using a WireMock or sandbox server: connect and test, map options, push availability and rates, handle rate limiting (429), handle authentication failure, parse a new, modified and cancelled booking, and survive malformed payloads. Real OTA connectors also need recorded sample payloads in `src/test/resources/samples/<channel>/`.

## 5. Definition of Done (applies to every prompt and slice)

A slice is done only if **all** are true:

1. Every acceptance criterion of its user stories passes, and story IDs are listed in the PR.
2. Business rules it touches are implemented and referenced in code comments and tests.
3. Unit tests cover domain logic; integration tests (Testcontainers) cover repositories and queues; key flows have an end-to-end test.
4. Test coverage for new backend domain code is at least 80% lines and 70% branches; no decrease overall.
5. Tenant isolation is tested for every new endpoint (another organization gets 404).
6. Permissions are tested for every role on every new endpoint (matrix in the API contract).
7. All user-facing text follows the Plain-Language Guide and sits in the translation file.
8. UI passes the accessibility checklist: keyboard use, contrast, labels, screen reader announcements.
9. Loading, empty, error and offline states are built, not just the happy path.
10. OpenAPI file and database migrations are updated, and the API client is regenerated.
11. No new critical or high security findings (static scan, dependency check, secret scan).
12. Logging, metrics and audit entries exist for new actions, and contain no personal or secret data.
13. README, CHANGELOG and the progress file are updated; seed data covers the new feature.
14. The app starts from a clean checkout with `docker compose up` and the demo flow works.
15. A short summary lists what works, what is stubbed and what is next.

## 6. Test plan

### 6.1 Test pyramid

| Level | Tools | What it covers |
| --- | --- | --- |
| Unit | JUnit 5, Mockito, Vitest | Business rules (restrictions, derived rates, availability math, money rounding), React components and hooks |
| Integration | Spring Boot Test, Testcontainers (PostgreSQL, Redis, RabbitMQ), WireMock | Repositories, migrations, outbox, sync workers, connectors, security rules |
| Contract | OpenAPI validation, connector contract suite | Frontend and backend agree; every connector behaves the same |
| End to end | Playwright | Onboarding, rate update, inbound booking, conflict resolution, role restrictions |
| Concurrency | JUnit with parallel threads, Testcontainers | Overbooking: many simultaneous bookings for the last room |
| Performance | k6 or Gatling | API latency, calendar load, sync throughput |
| Accessibility | axe in Playwright and Storybook, manual screen reader checks | WCAG 2.1 AA |
| Usability | Moderated sessions with 5 to 8 hoteliers | Task success and the SUS score |

### 6.2 Must-have scenarios

| # | Scenario | Expected result |
| --- | --- | --- |
| T1 | 50 parallel bookings for the last room, split across 3 channels | Exactly one confirmed booking; others are conflicts; `available` never below 0 |
| T2 | Same webhook delivered 5 times | One reservation; one inventory decrement |
| T3 | Cancellation arrives before the booking | Handled without error (recorded and applied in order, or parked until booking arrives) |
| T4 | Channel down for 3 hours, then recovers | All queued updates delivered in order; no stale data overwrites newer data |
| T5 | Two users edit the same day at the same time | Second save gets a clear message (version conflict) and sees the latest values |
| T6 | Bulk update over 45 days, then undo | All dates return to previous values; audit shows both actions |
| T7 | Reduce total rooms below rooms already sold | Refused with the plain-language message listing nights |
| T8 | User from organization A requests organization B's booking by ID | 404 |
| T9 | Reservations Staff calls the rate update endpoint directly | 403 |
| T10 | Booking for an unmatched room | Saved as Needs matching; processed after matching |
| T11 | Booking across a month end and a property in a different timezone | Nights counted correctly |
| T12 | Credentials become invalid mid-run | Channel pauses, urgent alert, no endless retries |
| T13 | iCal feed with 500 events and a malformed entry | Valid events imported; malformed entry reported without failing everything |
| T14 | Setup wizard completed by a new user | Reaches Go live; resume works after leaving halfway |
| T15 | Calendar with 90 days by 50 rows | Loads in under 2 seconds, smooth scrolling |

### 6.3 Test data

Provide a seed script that creates: 2 organizations (to test isolation), 3 properties (a 10-room guesthouse, a 60-room hotel, a 5-unit rental), room types and rate plans, 90 days of inventory and prices, one mock channel that emits sample new, modified and cancelled bookings, and users for each role with documented demo passwords (development only).

## 7. Security and privacy checklist

| Area | Requirement | Verified by |
| --- | --- | --- |
| Authentication | Passwords hashed with Argon2id or bcrypt (cost 12+); lockout after 5 failures; password reset tokens expire in 30 minutes and are single-use | Integration tests |
| Sessions | Access token 15 minutes; refresh token rotation and revoke on logout and password change | Integration tests |
| Authorization | Role and property checks on the server for every endpoint; deny by default | Permission matrix tests |
| Tenant isolation | Organization filter in every query; consider PostgreSQL row-level security; tests per endpoint | Tenant tests, T8 |
| Credentials vault | Channel credentials encrypted with AES-256-GCM; never returned by the API; redacted in logs and audit | Unit and log scans |
| Transport | HTTPS only; HSTS; secure, httpOnly, SameSite cookies | Config review |
| Input handling | Validate and size-limit all input; parameterized queries only; output encoding in UI | Static scan, tests |
| Web security | CORS limited to the app origin; CSRF protection for cookie flows; content security policy; security headers | Header tests |
| Rate limiting | Login, password reset and webhooks limited by IP and account | Integration tests |
| Webhooks | Verify signatures or shared secrets; reject replay (timestamp window); idempotent processing | Contract tests |
| Secrets | No secrets in code or images; use environment variables or a secrets manager; secret scanning in CI | CI |
| Dependencies | Dependency and container scans in CI; fix critical and high findings before release | CI |
| Personal data | Collect only needed fields; no card numbers stored; export and delete a guest's data on request; anonymize after the retention period (BR-9.1) | Tests, review |
| Logging and audit | Audit every change to rates, inventory, mappings, permissions and credentials; logs contain no secrets or personal data | Log tests |
| Backups | Daily backups, point-in-time recovery, restore tested every quarter; restored copies do not send emails or channel updates | Runbook drill |
| Compliance | Design for India's DPDP Act and GDPR: consent, purpose limits, breach process, data processing records | Legal review before launch |
| Incident response | Documented process: detect, contain, notify customers and authorities within required time, post-mortem | Runbook |

## 8. Performance and reliability targets

| Measure | Target |
| --- | --- |
| Read API p95 | under 300 ms |
| Write API p95 | under 500 ms |
| Calendar (90 days by 50 rows) | usable in under 2 seconds |
| Change reaches channels | under 60 seconds at p95 |
| Inbound booking processed | under 30 seconds from arrival |
| Uptime | 99.9% monthly |
| Overbooking rate | under 0.1% of bookings |
| Sync success rate | above 99.5% |
| Recovery | RPO 15 minutes, RTO 4 hours |

Load test baseline: 1,000 properties, 20 rooms each, 100,000 room-night changes per day, and a burst of 500 bookings per minute.

## 9. Observability

- **Metrics (Micrometer to Prometheus):** sync jobs by status and channel, sync latency, queue depth and age of oldest job, inbound bookings per minute, conflicts created, API latency and errors, database pool usage.
- **Alerts:** oldest pending job above 5 minutes, dead jobs above 0, conflict unresolved over 2 hours, error rate above 2%, credential failures, database or broker down.
- **Tracing:** OpenTelemetry with the `traceId` shown to users as a short support code.
- **Dashboards:** one operations dashboard per environment (sync health, bookings, errors, performance).

## 10. Environments and deployment

| Environment | Purpose | Notes |
| --- | --- | --- |
| Local | Development with `docker compose up` | PostgreSQL, Redis, RabbitMQ, backend, frontend, mock channel |
| Test (CI) | Automated tests | Testcontainers; runs on every PR |
| Staging | Pre-release, real OTA sandboxes | Production-like data volume, anonymized |
| Production | Customers | Blue-green or rolling deploy, zero downtime, automatic rollback on failed health checks |

Configuration through environment variables (`.env.example` lists all, with no real values). Database migrations run with Flyway at deployment and must be backward compatible with the previous app version (expand, then contract). Feature flags control risky features and each real OTA connector.

## 11. Release checklist

1. All Definition of Done items pass for included slices.
2. Regression suite and end-to-end tests are green on staging.
3. Performance test results meet section 8.
4. Security scan clean; dependencies up to date.
5. Migration tested on a copy of production-sized data and a rollback plan exists.
6. Release notes written in plain language for customers.
7. Monitoring dashboards and alerts checked; on-call person named.
8. Support team briefed on changes and known issues.

## 12. Decision log (to be filled by the agent)

| Date | Decision or assumption | Reason | Approved by |
| --- | --- | --- | --- |
|  |  |  |  |
