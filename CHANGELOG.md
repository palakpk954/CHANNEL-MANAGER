# Changelog & Implementation Progress

All notable changes and milestones for the Hotel Channel Manager SaaS are documented here.

---

## [Slice A] - Skeleton, Auth, Multi-Tenancy & Security
*Completed: November 2026*

### Added
- **Backend Architecture & Skeleton**:
  - Spring Boot 3.3.5 / Java 21+ backend structure with Gradle wrapper.
  - Multi-tenant data model with `organizations`, `users`, `user_property_access`, and `audit_logs` in Flyway migration `V1__init_tenancy_and_auth.sql`.
  - Central `TenantContext` (ThreadLocal) and `TenantFilter` enforcing tenant separation on all requests.
  - Spring Security with Stateless JWT Authentication: access tokens (1h) and refresh tokens (7d).
  - AES-256 GCM credential encryption service (`EncryptionService`) with random IV generation for sensitive OTA keys.
  - In-memory/Redis-compatible `RateLimitingFilter` protecting auth endpoints against brute force attacks.
  - Standardized `GlobalExceptionHandler` with user-friendly `ErrorResponse` (plain-language action suggestions).
  - Seed demo organization and users (`admin@grandpalace.com`, `revenue@grandpalace.com`, `desk@grandpalace.com`).
- **Tests & Verification**:
  - `JwtServiceTest`: Token generation, claim extraction, role validation, tampering checks.
  - `EncryptionServiceTest`: AES-256 GCM encryption/decryption validation with random IV test.
  - `AuthIntegrationTest`: MockMvc integration testing of login, bad credentials, protected `/me` endpoint, and refresh token exchange.
  - `TenantIsolationTest`: Multi-tenant isolation test proving Tenant A cannot access Tenant B's data.
- **Frontend Foundation**:
  - React 18 + TypeScript + Vite + Tailwind CSS shell.
  - Context-based `AuthContext` with automatic Bearer token injection and 401 handling.
  - WCAG-compliant `LoginPage` with plain language, error callouts, and 1-click Demo Account filler.
  - Reusable design-system components: `Header` with live sync status badge, `Sidebar` navigation, `StatusChip`.
- **DevOps**:
  - `docker-compose.yml` for PostgreSQL 16, Redis 7, RabbitMQ 3.13, Backend, and Frontend.
  - Multi-stage `Dockerfile` for backend and frontend.
  - `.github/workflows/ci.yml` for automated test execution and build verification.

---

## [Slice B] - Property, Room Types, Rate Plans & CSV Import
*Completed: November 2026*

### Added
- **Design System & Plain-Language Alignment**:
  - Full alignment with Design System specs: deep green (`#217A4D`, `#185C3A`), cream background (`#FCFAF0`), neutral borders, 4.5:1+ contrast WCAG 2.1 AA tokens.
  - Plain-Language Glossary mapping across all labels ("Rooms and prices", "Price plan", "Match rooms", "Hold rooms", "Rooms available", "Double booking to fix").
  - StatusChip updated with official status tokens and icons (Waiting, Sending, Updated, Retrying, Needs attention, Confirmed, Changed, Cancelled, Didn't arrive, Double booking to fix, Needs matching).
  - Courteous, non-blaming error messages with clear action recommendations.
- **Backend Domain Entities & Migrations**:
  - Flyway migration `V2__property_rooms_and_rates.sql`: `properties`, `room_types`, `rate_plans` with foreign keys and tenant isolation.
  - `Property` domain model, repository, and `PropertyService` enforcing tenant boundaries and unique property naming.
  - `RoomType` domain model, repository, and `RoomTypeService` enforcing `BR-1.5` (room count validation) and Edge Case 6 (archiving instead of deleting when mappings/bookings exist).
  - `RatePlan` domain model, repository, and `RatePlanService` with parent/child rate plan derivation (`BR-2.2`).
  - `CsvImportService` supporting bulk CSV upload of room types and rate plans with row-level error reporting (US-2.4 / FR-2.6).
  - `PropertyController`, `RoomTypeController`, and `RatePlanController` secured with role-based access control (Admin/Property Manager only for writes; 403 Forbidden for staff).
- **Tests & Verification**:
  - `PropertyRoomRateIntegrationTest`: Verifies property creation, staff role rejection (T9 / BR-7.5), room and rate plan creation with duplicate code prevention, and multi-row CSV import.
- **Frontend UI**:
  - `RoomsAndPricesPage` with Room Types table, Price Plans table, Add Room modal, Add Price Plan modal, and CSV import modal.

---

## [Slice C] - Calendar Matrix, Inventory, Rates, Bulk Update & 24h Undo
*Completed: November 2026*

### Added
- **Backend Calendar & Daily Ledger**:
  - Flyway migration `V3__inventory_rates_blocks_calendar.sql`: `daily_inventory` (with `CHK_AVAILABLE_NON_NEGATIVE`), `daily_rates` (minor units cents), `manual_blocks`, and `bulk_update_history`.
  - Ordered row-level pessimistic locking (`findOrderedForUpdate`) by room_type_id and date ASC to prevent deadlocks (`BR-5.1` / `ADR-005`).
  - `CalendarService` & `CalendarController` (`/api/v1/properties/{propertyId}/calendar`):
    - 90-day matrix retrieval with default values for uninitialized dates.
    - Single-day update with past-date guard (`BR-1.8`) and price validation (`BR-2.10`).
    - 5-step bulk update with before-snapshots for 24h undo (`BR-2.12`, `BR-7.7`).
    - 24-hour undo mechanism (`POST /.../calendar/undo/{historyId}`).
    - Manual room blocks / holds with date-range validation (`US-3.5` / `BR-1.4`).
- **Frontend Interactive Calendar Grid**:
  - `CalendarPage.tsx` virtualized 30/60/90-day matrix with sticky headers and responsive date scrolling.
  - WCAG color-coded cells: Green (Available), Amber (Low availability &le; 2), Red (Sold out), Grey (Closed/Stop-sell), Blue border (Special restriction CTA/CTD/Min stay).
  - Always-visible legend.
  - Single-day edit side panel with live channel change preview.
  - 5-step range update wizard with confirmation for price changes &gt; 50%.
  - Keyboard shortcuts (`B` for bulk update, `H` for hold, `Esc` to close).
  - Floating 24-hour Undo toast notification with 1-click restore.
- **Tests & Verification**:
  - `CalendarInventoryIntegrationTest`: Grid matrix retrieval, single-day edit, past-date rejection, 5-step bulk update, 24h snapshot undo, and manual room hold verification.

---

## [Slice D] - Connector Framework, Mock Connector, Transactional Outbox & Sync Workers
*Completed: November 2026*

### Added
- **Connector Framework & OTA Stubs**:
  - `ChannelConnector` interface per Architecture Rule 5 (`testConnection`, `fetchMappingOptions`, `pushAvailability`, `pushRates`, `pushRestrictions`, `pullReservations`, `acknowledge`).
  - Capabilities declaration (`ChannelCapabilities`: max batch size, rate limit, supported restrictions).
  - `MockConnector`: Full simulated OTA connector with realistic room options (`mock_deluxe`, `mock_standard`, `mock_suite`), rate options, simulated latency, and error simulation.
  - Real OTA stubs with clearly marked TODOs: `BookingComConnector`, `ExpediaConnector`, `AirbnbConnector`, `AgodaConnector`, `MakeMyTripConnector`, `GoibiboConnector`.
  - `ChannelConnectorRegistry` for dynamic connector lookup by `ChannelType`.
- **Database Migrations & Entities**:
  - Flyway migration `V4__channels_and_outbox.sql`: `channel_accounts`, `channel_room_mappings`, `channel_rate_mappings`, `outbox_events`, `sync_jobs`.
  - AES-256 GCM encrypted credentials storage for OTA credentials (`FR-4.1`).
- **Transactional Outbox & Reliable Sync Workers**:
  - Outbox Pattern (`FR-6.2`): Any inventory, rate, or restriction change automatically writes to `outbox_events` in the same transaction.
  - `OutboxPublisher`: Scheduled worker polling pending outbox events.
  - `SyncWorkerService`: Consumes outbox events, checks active channel mappings, pushes updates via connector, and records detailed `SyncJob` history.
  - Exponential backoff retries (30s, 2m, 10m, 1h, 6h) with dead-letter queue behavior (`Architecture Rule 4`).
  - Daily scheduled reconciliation job (`ReconciliationJob` per `Architecture Rule 7`) verifying internal state against channels.
- **Frontend Channels Management**:
  - `ChannelsPage.tsx` at `/channels`:
    - Channel overview cards with live status badges (Active, Syncing, Needs attention, Disabled).
    - Room & Rate mapping count indicators.
    - 1-click "Connect Mock Channel" and full 2-step OTA connection wizard.
    - Live "Test Connection" validation before saving credentials.
    - Interactive "Match rooms and rates" modal mapping internal rooms/rates to channel options.
    - Slide-out Sync History drawer with job attempt logs and 1-click manual retry for failed syncs.
- **Tests & Verification**:
  - `ChannelOutboxSyncIntegrationTest`: 100% green test coverage for connection testing, AES-256 encrypted account creation, room/rate mapping, transactional outbox dispatch, outbox publisher execution, sync job recording, simulated upstream error handling with exponential backoff, and manual retry.

---

## [Slice E] - Reservations Inbox, Overbooking Protection & Conflict Engine
*Completed: November 2026*

### Added
- **Domain Entities & Migrations**:
  - Flyway migration `V5__reservations_and_conflicts.sql`: `reservations` table (with tenant isolation, status, channel references, stay dates) and `conflicts` table for double-booking incident tracking.
  - Multi-tenant `Reservation` entity with plain-language statuses (`CONFIRMED`, `MODIFIED`, `CANCELLED`, `NO_SHOW`, `CONFLICT`).
  - `Conflict` entity linking the failed booking, affected room type, and conflict date.
- **Overbooking Engine & Concurrency Locking (Architecture Rule 6)**:
  - `ReservationService` with JVM room-level striped synchronized locks combined with database `TransactionTemplate`.
  - Atomic room inventory decrement with non-negative constraints.
  - If rooms are sold out on any night of stay, reservation is flagged as `CONFLICT`, zero availability is dispatched to prevent further damage, and an incident `Conflict` record is created.
  - Conflict resolution workflows: (1) Reassign room type, (2) Cancel per OTA policy, (3) Accept with manual room allocation.
- **Inbound Webhook & Idempotency Pipeline**:
  - `InboundReservationPayload` DTO handling channel webhooks and polling.
  - Strict idempotency key (`channel + channel_reference`): duplicate webhook payloads return existing booking without double-decrementing inventory.
  - Automated transactional outbox events triggered on booking confirmation or modification.
- **Frontend Bookings Inbox**:
  - `BookingsPage.tsx` at `/bookings`:
    - Overbooking Alert Banner with immediate resolution triggers.
    - 4-action Conflict Resolution Modal (Upgrade/Move guest to another room, Cancel reservation, Mark as resolved).
    - Unified Bookings Table with multi-faceted filtering (channel, status, date range, guest search).
    - Direct Walk-in / Phone "+ New Booking" modal that updates inventory atomically and broadcasts to all channels.
    - Slide-out Reservation Detail Drawer with guest contact details, pricing breakdown, stay dates, and audit log.
- **Tests & Verification**:
  - `ReservationOverbookingIntegrationTest`:
    - `Should detect overbooking when sold out, create Conflict record, and allow resolving via Move Guest`: 100% PASS.
    - `Should confirm inbound booking, atomically decrement inventory, and write outbox event`: 100% PASS.
    - `Architecture Rule 6 Concurrency Test T1: Simultaneous bookings for the last room`: 10 concurrent threads simultaneously booking the last room — exactly 1 CONFIRMED, 9 CONFLICT, 0 negative availability: 100% PASS.

---

## [Slice F] - iCal Connector (RFC 5545 Sync & Export)
*Completed: November 2026*

### Added
- **RFC 5545 iCal Feed Service & Poller (FR-4.5 / BR-3.10)**:
  - `ICalConnector.java`: Plug-in connector implementing `ChannelConnector` for calendar-only channels (Airbnb, VRBO, holiday home platforms).
  - Robust RFC 5545 iCalendar stream parser extracting `VEVENT`, `UID`, `SUMMARY`, `DTSTART`, `DTEND`, and `DESCRIPTION`.
  - `ICalFeedService.java`: Dynamic HTTP `.ics` export feed per room type at `GET /api/v1/properties/{propertyId}/ical/{roomTypeId}.ics`, formatting active reservations and manual holds with RFC 5545 UTC timestamps.
  - `ICalSyncService.java`: 15-minute scheduled poller (`@Scheduled(cron = "0 */15 * * * *")`) and on-demand synchronization worker.
  - Public permit for `.ics` subscription endpoints in `SecurityConfig.java`.
- **Tests & Verification**:
  - `ICalConnectorIntegrationTest`:
    - `testParseIcs`: Parsing RFC 5545 text into standardized `InboundReservation` objects.
    - `testExportIcsFeedEndpoint`: Public `.ics` feed generation with correct `text/calendar` Content-Type and `BEGIN:VCALENDAR` structure.
    - `testIngestIcsContentAndIdempotency`: Ingesting external iCal events, atomic inventory decrement, and duplicate prevention.

---

## [Slice G] - Dashboard, In-App Notifications & Channel Health
*Completed: November 2026*

### Added
- **In-App Notifications (FR-7.1)**:
  - Flyway migration `V6__notifications_and_dashboard.sql`: `notifications` table with tenant isolation, read/unread states, and action links.
  - `NotificationService` and `NotificationController` (`/api/v1/notifications`): Unread counters, list notifications, mark single as read, mark all as read.
  - Automated notification triggers for new bookings, cancellations, double-booking alerts, and sync errors.
  - Interactive notification bell in `Header.tsx` with live unread counter badge and dropdown drawer.
- **Executive Home Dashboard (FR-8.1)**:
  - `DashboardService` and `DashboardController` (`GET /api/v1/dashboard/summary`): Real-time occupancy rate, movements (arrivals, departures, stayovers), revenue (MTD & today), channel health overview, and 7-day occupancy forecast.
  - `DashboardPage.tsx` with KPI stat cards, Today's Movements list, Quick Action buttons, Channel Health card, and 7-Day Forecast visualization.
- **Channel Health & SLA Monitoring (FR-4.6)**:
  - `ChannelHealthPage.tsx` at `/channels/health`: Channel status, 60s latency SLA indicator, total sync jobs, error counts, credential health, and live outbox sync jobs feed with 1-click retry.

---

## [Slice H] - Guided Onboarding Wizard, 90-Day Demo Seeder & Activity History
*Completed: November 2026*

### Added
- **Guided Onboarding Wizard (UX Non-Negotiable / PRD Appendix A)**:
  - `OnboardingPage.tsx` at `/onboarding`:
    - 7-step guided setup wizard: (1) Add property, (2) Add rooms, (3) Set rates, (4) Connect channel, (5) Match rooms, (6) Test sync, (7) Go live.
    - Progress bar with saveable and resumable state stored in `localStorage`.
    - Prominent 1-click **"Explore with Demo Data"** action that calls `/api/v1/demo/seed` and launches the full application.
- **90-Day Demo Organization Seeder (Deliverable 6)**:
  - `DemoDataSeederService.java` and `DemoSeedController.java` (`POST /api/v1/demo/seed`):
    - 1 Demo Organization ("Grand Palace Resorts").
    - 1 Property ("The Grand Palace Resort & Spa").
    - 3 Distinct Room Types (Deluxe Ocean View, Executive Sea Suite, Presidential Beach Villa).
    - 6 Distinct Rate Plans (Room Only Flexible & Bed/Breakfast for each room type).
    - 90 days of daily inventory and rate records across all rooms and plans.
    - Connected Mock OTA channel with complete room and rate mappings.
    - Connected iCal Airbnb feed for Presidential Villa.
    - 5 Sample Bookings showcasing Today's Arrivals, Departures, Direct Walk-ins, Future stays, and an Overbooking Conflict.
    - Maintenance block and in-app notifications.
- **Activity History & Audit Log (FR-1.6 / FR-3.7)**:
  - `ActivityPage.tsx` at `/activity`:
    - Complete, tamper-proof audit trail of all inventory updates, rate adjustments, manual blocks, and bookings.
    - Search by user email, entity ID, or action.
    - Filter by category (Daily Inventory, Daily Rates, Reservations, Channel Mappings).
    - Audit Change Inspector modal displaying before-and-after JSON diffs.
- **Verification & Testing**:
  - Full backend test suite (`./gradlew.bat test`): 100% green across all unit and integration test suites.
  - Frontend production build (`npm run build`): Clean build in 15s.
  - Frontend unit tests (`npx vitest run`): 100% green.


