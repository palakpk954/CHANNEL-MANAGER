# PRD: Hotel Channel Manager

*Version 0.1 (Draft) · November 5, 2026 · Frontend: React · Backend: Java Spring Boot*

## 1. Overview

### 1.1 Product summary

A cloud-based channel manager that lets hotels, resorts and vacation-rental operators manage room inventory, rates and restrictions from one dashboard and keep them synchronized in real time across Online Travel Agencies (OTAs) such as Booking.com, Expedia, Airbnb, Agoda, MakeMyTrip and Goibibo. Reservations from every channel flow back into one place, and availability is updated everywhere automatically to prevent overbooking.

### 1.2 Problem statement

Properties that sell on several OTAs update availability and rates manually in each extranet. This causes overbooking, rate parity violations, missed revenue, and hours of repetitive staff work every day. Existing tools are often expensive, bundled with a full PMS the property does not need, or weak on the Indian OTA ecosystem.

### 1.3 Reference product

The reference is MyHotelLine, an all-in-one hospitality suite (cloud PMS, channel manager, booking engine, POS, ERP, revenue intelligence). This PRD scopes the **channel manager as the core product**, with a lightweight booking engine and PMS-lite features as later phases. The reference's landing page could not be read directly (it renders client-side), so its feature set here is inferred from its public city pages. Please share screenshots or specific features you want mirrored.

### 1.4 Goals

- Real-time, two-way sync of inventory, rates and restrictions with major OTAs.
- Zero overbooking under normal operation, with clear handling when a channel fails.
- One dashboard for calendar, reservations, rates and channel health.
- Multi-property, multi-user with role-based access.
- A connector framework so new channels can be added in days, not months.

### 1.5 Non-goals (v1)

- Full PMS (front desk, housekeeping, folio and billing).
- POS and ERP modules.
- Payment processing beyond recording OTA payment status.
- AI-driven dynamic pricing (planned for a later phase).

## 2. Target users and personas

| Persona | Description | Key needs |
| --- | --- | --- |
| Property owner / GM | Runs a hotel or group of properties | Occupancy and revenue visibility, commission insight, control over rates |
| Revenue manager | Sets rates and restrictions | Bulk rate edits, rate plans, restrictions (min stay, CTA/CTD), parity checks |
| Front-desk / reservations staff | Handles bookings daily | Unified reservation list, quick availability view, manual block/unblock |
| Vacation-rental host | Few units, mostly Airbnb and iCal channels | Simple setup, calendar sync, low cost |
| Platform admin (internal) | Operates the SaaS | Tenant management, channel monitoring, support tooling |

## 3. Scope and release phases

| Phase | Scope | Target |
| --- | --- | --- |
| MVP (Phase 1) | Auth and tenancy, property and room setup, inventory/rate calendar, iCal sync, 1 mock OTA plus 1 real OTA connector, reservation inbox, overbooking protection, audit log | 3 to 4 months |
| Phase 2 | Additional OTA connectors (Booking.com, Expedia, Airbnb, Agoda, MakeMyTrip, Goibibo), bulk updates, rate rules and derived rates, notifications, reports | +3 months |
| Phase 3 | Booking engine (direct bookings), payment gateway, guest profiles, parity monitor, revenue analytics | +3 months |
| Phase 4 | PMS-lite (check-in/out, housekeeping), POS integrations, dynamic pricing recommendations, public API and webhooks | +4 months |

Real OTA APIs (Booking.com, Expedia, Agoda and others) require partner or certification approval, so the connector framework and mock connectors come first and certification runs in parallel.

## 4. Functional requirements

Priority key: **P0** = must have for MVP, **P1** = Phase 2, **P2** = later.

### 4.1 Authentication, tenancy and access control

- **FR-1.1 (P0)** Email and password login with JWT access and refresh tokens; optional Google SSO (P1).
- **FR-1.2 (P0)** Multi-tenant model: an Organization owns one or more Properties; all data is tenant-scoped.
- **FR-1.3 (P0)** Roles: Super Admin, Org Admin, Property Manager, Revenue Manager, Reservations Staff, Read-only.
- **FR-1.4 (P0)** Per-property permissions (a user may manage some properties and only view others).
- **FR-1.5 (P1)** Two-factor authentication, user invitations by email, session management.
- **FR-1.6 (P0)** Audit log of every inventory, rate, mapping and user change (who, what, when, before/after).

### 4.2 Property and room setup

- **FR-2.1 (P0)** Create and edit properties: name, address, timezone, currency, tax settings, check-in/out times, contact info, photos.
- **FR-2.2 (P0)** Room types with code, name, max occupancy, bed configuration, amenities, and total units.
- **FR-2.3 (P0)** Rate plans per room type (for example Room Only, Bed and Breakfast, Non-refundable), with meal plan and cancellation policy.
- **FR-2.4 (P1)** Derived rate plans (for example Non-refundable = Base minus 10%).
- **FR-2.5 (P1)** Extra-person and child pricing, taxes and fees configuration.
- **FR-2.6 (P1)** Import of rooms and rates from CSV or Excel.

### 4.3 Inventory, rates and restrictions calendar

- **FR-3.1 (P0)** Calendar grid (rooms and rate plans by date) showing availability, rate, and restrictions, editable inline.
- **FR-3.2 (P0)** Per date and room type: available units, base rate, stop-sell, minimum stay, maximum stay, closed to arrival (CTA), closed to departure (CTD).
- **FR-3.3 (P0)** Bulk update: select a date range, weekdays and room/rate plans, then apply values in one action.
- **FR-3.4 (P0)** Every change is queued and pushed to all mapped channels; the UI shows per-channel sync status (pending, success, failed).
- **FR-3.5 (P1)** Channel-specific adjustments: rate markup or markdown, allotment caps, and per-channel stop-sell.
- **FR-3.6 (P1)** Rate rules: seasonal, event, weekend and length-of-stay rules; copy rates from a previous period.
- **FR-3.7 (P1)** Undo and rollback of recent bulk changes using the audit trail.
- **FR-3.8 (P0)** Manual blocks (maintenance, owner use) that reduce availability on all channels.

### 4.4 Channel management and connectors

- **FR-4.1 (P0)** Channel directory with status, and a connect flow that captures credentials (API key, hotel ID, OAuth) stored encrypted.
- **FR-4.2 (P0)** Mapping screen: map internal room types and rate plans to each channel's room and rate IDs.
- **FR-4.3 (P0)** Outbound sync: availability, rates, restrictions (ARI) pushed on change, plus a scheduled full-sync as a safety net.
- **FR-4.4 (P0)** Inbound sync: new, modified and cancelled reservations via webhook or polling.
- **FR-4.5 (P0)** iCal import and export for channels without an API, with configurable polling interval.
- **FR-4.6 (P0)** Channel health dashboard: last successful sync, error counts, queue depth, credential expiry.
- **FR-4.7 (P0)** Connector framework with a common interface (see Section 6.3) so each OTA is a plugin.
- **FR-4.8 (P1)** Pause or disable a channel without losing mappings; initial full-sync when enabling.
- **FR-4.9 (P1)** Connectors in priority order: Booking.com, Expedia, Airbnb, Agoda, MakeMyTrip, Goibibo, Google Hotel Ads, plus a generic API channel.

### 4.5 Reservations

- **FR-5.1 (P0)** Unified reservation inbox across all channels with search and filters (channel, status, dates, guest name, confirmation number).
- **FR-5.2 (P0)** Reservation detail: guest info, stay dates, room and rate plan, price breakdown, payment status, special requests, channel reference, history.
- **FR-5.3 (P0)** Process new, modified and cancelled bookings and update inventory atomically.
- **FR-5.4 (P0)** Manual reservation entry (phone or walk-in) that also decrements availability on all channels.
- **FR-5.5 (P1)** Acknowledge bookings back to the OTA where the API requires it, with retries.
- **FR-5.6 (P1)** Export reservations to CSV and Excel.
- **FR-5.7 (P1)** No-show and virtual-card or payment-status tracking as provided by the channel.

### 4.6 Overbooking protection

- **FR-6.1 (P0)** Availability is decremented transactionally on each booking, with optimistic or pessimistic locking per room type and date.
- **FR-6.2 (P0)** If a booking arrives for a sold-out room (race between channels), flag it as a conflict, alert staff, and suggest actions (relocate to another room type, contact guest, cancel per OTA policy).
- **FR-6.3 (P0)** Idempotent processing so duplicate webhooks or retries never double-count a booking.
- **FR-6.4 (P1)** Configurable buffer (for example, keep one room unsold) and per-channel allotments.

### 4.7 Notifications

- **FR-7.1 (P0)** In-app notifications for new bookings, cancellations, sync failures and conflicts.
- **FR-7.2 (P1)** Email notifications and daily digest; WhatsApp or SMS alerts (P2).
- **FR-7.3 (P1)** User-level notification preferences.

### 4.8 Reports and dashboard

- **FR-8.1 (P0)** Home dashboard: today's arrivals and departures, occupancy, new bookings, channel health, and unresolved conflicts.
- **FR-8.2 (P1)** Reports: occupancy, ADR, RevPAR, revenue by channel, bookings by channel, cancellations, lead time, commission cost.
- **FR-8.3 (P1)** Date-range comparison (this month vs last month, year over year).
- **FR-8.4 (P1)** Scheduled report emails and CSV or PDF export.
- **FR-8.5 (P2)** Parity monitor comparing your rate across channels and flagging mismatches.

### 4.9 Booking engine (Phase 3)

- **FR-9.1** Embeddable widget and hosted booking page showing live availability and rates.
- **FR-9.2** Payment gateway integration (Razorpay, Stripe), promo codes, and add-ons.
- **FR-9.3** Direct bookings flow into the same reservation and inventory pipeline.

### 4.10 Settings and administration

- **FR-10.1 (P0)** Organization and property settings, user management, and API credential vault.
- **FR-10.2 (P1)** Webhooks and a public REST API for third-party PMS, accounting and revenue tools.
- **FR-10.3 (P1)** Subscription plans and billing for the SaaS (per property or per room).

## 5. User flows

1. **Onboarding:** sign up, create property, add room types and rate plans, connect first channel, map rooms and rates, run initial sync, review the calendar.
2. **Rate update:** open calendar, select dates, edit rate or restriction, save; system queues updates per channel and shows live sync status.
3. **Inbound booking:** OTA sends reservation, system validates and deduplicates, creates the booking, decrements inventory, pushes new availability to other channels, notifies staff.
4. **Cancellation:** OTA cancels, system restores inventory and re-pushes availability everywhere.
5. **Conflict handling:** a booking arrives for zero availability, system flags a conflict and alerts the manager with resolution options.

## 6. Technical architecture

### 6.1 Technology stack

| Layer | Choice |
| --- | --- |
| Frontend | React 18 with TypeScript, Vite, React Router, TanStack Query, a component library (MUI or Ant Design) or Tailwind, a virtualized grid for the calendar (for example TanStack Table or AG Grid), Recharts for reports |
| Backend | Java 21, Spring Boot 3.x, Spring Web, Spring Security (JWT/OAuth2), Spring Data JPA (Hibernate), Spring Validation, Spring Scheduling or Quartz |
| Database | PostgreSQL (primary), Flyway migrations |
| Cache and locks | Redis (caching, distributed locks, rate limiting) |
| Messaging | Kafka or RabbitMQ for sync queues, retries and dead-letter handling |
| Object storage | S3-compatible storage for images and exports |
| Observability | Micrometer, Prometheus, Grafana, centralized logs (ELK or Loki), OpenTelemetry tracing |
| API docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, Testcontainers, WireMock for OTA mocks; Vitest and React Testing Library; Playwright for end-to-end |
| DevOps | Docker, Kubernetes or ECS, GitHub Actions CI/CD, environment-based config and secrets manager |

### 6.2 High-level architecture

- **Modular monolith first**, structured by domain modules (identity, property, inventory, rates, channels, reservations, notifications, reporting) so it can be split into services later.
- **Outbound pipeline:** a change to inventory or rates writes to the database and an outbox table; a publisher sends events to the message queue; channel workers consume, transform to each OTA's format, call the OTA with rate limiting and retries, and record results.
- **Inbound pipeline:** webhook endpoints and pollers receive OTA reservations, put them on a queue, and a reservation processor validates, deduplicates, writes the booking, adjusts inventory in one transaction, and triggers outbound availability updates.
- **Transactional outbox pattern** guarantees no lost updates between database writes and queue events.
- **Scheduled reconciliation** compares internal and channel-side availability and corrects drift.

### 6.3 Connector interface (each OTA is a plugin)

```java
public interface ChannelConnector {
    ChannelType type();
    ConnectionResult testConnection(ChannelCredentials creds);
    List<RemoteRoomRate> fetchMappingOptions(ChannelAccount account);
    SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates);
    SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates);
    SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates);
    List<ChannelReservation> pullReservations(ChannelAccount account, Instant since);
    AckResult acknowledge(ChannelAccount account, String reservationRef);
}
```

Implementations: `MockConnector`, `ICalConnector`, then one class per OTA. Each declares its capabilities (for example, supports webhooks, supports rate push, max batch size, rate limit).

### 6.4 Core data model (simplified)

| Entity | Key fields |
| --- | --- |
| Organization | id, name, plan, status |
| User | id, org\_id, email, password\_hash, role, status |
| UserPropertyAccess | user\_id, property\_id, role |
| Property | id, org\_id, name, address, timezone, currency, checkin\_time, checkout\_time |
| RoomType | id, property\_id, code, name, max\_occupancy, total\_units |
| RatePlan | id, property\_id, room\_type\_id, code, name, meal\_plan, cancellation\_policy, parent\_rate\_plan\_id, derivation\_rule |
| DailyInventory | room\_type\_id, date, total, sold, blocked, available (unique on room\_type\_id + date) |
| DailyRate | rate\_plan\_id, date, amount, currency, min\_stay, max\_stay, cta, ctd, stop\_sell |
| Channel | id, type, name, capabilities |
| ChannelAccount | id, property\_id, channel\_id, encrypted\_credentials, status, last\_sync\_at |
| ChannelRoomMapping / ChannelRateMapping | internal id, channel account id, remote id, markup rule |
| Reservation | id, property\_id, channel\_account\_id, channel\_ref, status, guest\_id, check\_in, check\_out, total\_amount, currency, payment\_status, raw\_payload, created\_at, updated\_at |
| ReservationRoom | reservation\_id, room\_type\_id, rate\_plan\_id, adults, children, nightly\_breakdown |
| Guest | id, org\_id, name, email, phone, country |
| SyncJob | id, channel\_account\_id, type, payload, status, attempts, next\_retry\_at, error |
| OutboxEvent | id, aggregate, type, payload, published\_at |
| AuditLog | id, actor, entity, action, before, after, created\_at |
| Notification | id, user\_id, type, payload, read\_at |

### 6.5 REST API overview

Versioned under `/api/v1`, JSON, secured with Bearer JWT, tenant resolved from the token.

| Area | Example endpoints |
| --- | --- |
| Auth | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| Properties | `GET/POST /properties`, `GET/PUT /properties/{id}` |
| Rooms and rates | `GET/POST /properties/{id}/room-types`, `GET/POST /properties/{id}/rate-plans` |
| Calendar | `GET /properties/{id}/calendar?from=&to=`, `PUT /properties/{id}/calendar/bulk` |
| Channels | `GET /channels`, `POST /properties/{id}/channel-accounts`, `POST /channel-accounts/{id}/test`, `PUT /channel-accounts/{id}/mappings`, `POST /channel-accounts/{id}/sync` |
| Reservations | `GET /reservations`, `GET /reservations/{id}`, `POST /reservations` (manual), `POST /reservations/{id}/cancel` |
| Inbound | `POST /webhooks/{channelType}/{accountId}` |
| Reports | `GET /reports/occupancy`, `GET /reports/revenue-by-channel` |
| Notifications | `GET /notifications`, `POST /notifications/{id}/read` |

### 6.6 Frontend structure

- Feature-based folders: `auth`, `dashboard`, `properties`, `rooms-rates`, `calendar`, `channels`, `reservations`, `reports`, `settings`.
- Server state via TanStack Query; light client state via Zustand or Context.
- Generated API client from the OpenAPI spec for type safety.
- Real-time sync status via WebSocket or Server-Sent Events.
- Responsive layout, with a mobile-friendly reservations and calendar view for on-the-go staff.

## 7. Non-functional requirements

| Area | Requirement |
| --- | --- |
| Performance | Calendar loads 90 days across 50 room and rate rows in under 2 seconds; API p95 under 300 ms for reads |
| Sync latency | Inventory or rate change reaches all connected channels within 60 seconds (p95), excluding OTA-side delays |
| Availability | 99.9% uptime target; zero-downtime deployments |
| Scalability | Support 1,000 properties and 100,000 room-nights of inventory changes per day in v1 design |
| Reliability | At-least-once delivery with idempotent handlers, exponential backoff, dead-letter queues, nightly reconciliation |
| Security | HTTPS only, credentials encrypted at rest (AES-256 with KMS), password hashing (bcrypt or Argon2), OWASP Top 10 hardening, tenant isolation, rate limiting |
| Privacy and compliance | Guest PII protection, data retention policy, GDPR and India DPDP Act readiness, PCI scope avoided by not storing card data |
| Auditability | Immutable audit trail for inventory, rate, mapping and permission changes |
| Usability | Accessible (WCAG 2.1 AA), keyboard-friendly calendar editing, English first with i18n support |
| Observability | Metrics, structured logs and alerts for sync failures, queue lag and error rates |
| Backup and DR | Daily backups, point-in-time recovery, RPO of 15 minutes, RTO of 4 hours |

## 8. Success metrics

- **Overbooking rate** under 0.1% of bookings.
- **Sync success rate** above 99.5%.
- **Time to first sync** (signup to first channel live) under 30 minutes for a property with up to 20 rooms.
- **Staff time saved** on manual updates (target: 70% reduction in survey).
- **Activation:** 60% of new properties connect at least one channel within 7 days.
- **Retention:** 90-day logo retention above 85%.
- **Support load:** fewer than 2 sync-related tickets per property per month.

## 9. Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| OTA API partnership and certification delays | Cannot launch real connectors | Start applications early; ship iCal and mock connectors first; consider certified aggregators as an interim path |
| OTA API rate limits and format differences | Sync failures or throttling | Per-connector rate limiting, batching, and capability flags |
| Race conditions across channels | Overbooking | Transactional inventory updates, locking, idempotency, buffers, conflict workflow |
| Credential leakage | Security incident | Encrypted vault, least privilege, rotation, audit logs |
| Data drift between internal and OTA state | Wrong availability or rates | Scheduled reconciliation and full-sync, alerting on mismatches |
| Scope creep toward a full PMS | Delayed launch | Strict phase gates; PMS-lite only after the channel manager is stable |

## 10. Dependencies and assumptions

- Access to OTA partner programs and sandbox environments (Booking.com, Expedia, Agoda, Airbnb, MakeMyTrip, Goibibo).
- Cloud hosting account and a managed PostgreSQL, Redis and queue service.
- Payment gateway account for Phase 3.
- Assumed launch market is India first (INR, GST handling, Indian OTAs), expanding globally later.

## 11. Open questions

1. Which OTAs matter most for the first release, and do you already have partner access to any of them?
2. Is this a SaaS product for many hotels, or a tool for your own property or properties?
3. Which features from the reference product do you want in v1 beyond the channel manager (booking engine, PMS, POS)?
4. Are there pricing, branding or white-label requirements?
5. Preferred cloud provider and any hosting or data residency constraints?
6. Team size and target timeline, which determines how much of Phase 1 and 2 can run in parallel?

## 12. Suggested next steps

1. Confirm scope and the priority channel list.
2. Finalize the data model and OpenAPI contract.
3. Build the Spring Boot skeleton (auth, tenancy, property and room setup) and the React shell with the calendar view.
4. Implement the connector framework with the mock and iCal connectors, then the first real OTA.

## Appendix A: Experience and Usability Requirements

The product must feel simple to a hotel owner who has never used a channel manager. These requirements apply to every screen and every phase, and a feature is not "done" until it meets them.

### A.1 Design principles

1. **Plain language, no jargon.** Say "Close this room for sale" instead of "Stop-sell", with the industry term shown as a small hint for experts. Every label is tested with a non-technical user.
2. **One screen, one job.** Each page has one clear primary action (for example, the calendar is for changing rates and availability, the inbox is for reading bookings).
3. **Show, don't make me remember.** Status is always visible: what is synced, what is pending, and what needs attention.
4. **Safe by default.** Destructive or wide-reaching actions (bulk rate changes, closing sales) show a preview of what will change and where, and can be undone.
5. **Fast and calm.** Actions respond instantly in the interface, with background sync shown quietly, never blocking the user.
6. **Helpful when things go wrong.** Errors explain what happened, why, and exactly what to do next, in one sentence.

### A.2 Onboarding: live in under 30 minutes

- A guided setup wizard with a visible progress bar: Add property, Add rooms, Set rates, Connect a channel, Match rooms, Go live.
- Each step is short, has sensible defaults, and can be saved and resumed later.
- Smart help: import rooms from a CSV or from the first connected channel to avoid retyping.
- A "Test sync" button that confirms a channel is working before going live, with a clear success message.
- A sample demo property for exploring the product risk-free.

### A.3 Key screens

| Screen | What makes it easy |
| --- | --- |
| Home dashboard | Three big answers at a glance: today's bookings and arrivals, anything needing attention, and channel health shown as green, amber or red |
| Calendar | Color-coded cells (available, low availability, sold out, closed), click to edit one day, drag to select a range, a single "Update" panel with live preview |
| Bulk update | A step-by-step side panel: pick dates, pick rooms, pick channels, enter value, review, apply |
| Channel mapping | Side-by-side lists with auto-suggested matches and drag or dropdown matching; unmatched items highlighted |
| Reservations | One list for all channels with channel logos, clear status chips, search, and quick filters such as "Today", "New" and "Cancelled" |
| Conflicts | A dedicated alert card with plain options: "Move guest", "Contact guest", "Cancel booking" |

**Calendar color legend:** green = available, amber = few rooms left, red = sold out, grey = closed for sale, blue outline = has a special restriction. A legend is always visible on the page.

### A.4 Guidance and help built into the product

- Empty states that explain what the page does and offer the next action (for example, "No channels yet. Connect your first one").
- Short tooltips and "What is this?" links next to every technical term.
- A searchable help center and short video walkthroughs linked from the relevant screens.
- An in-app checklist that tracks setup progress and disappears when complete.
- Live chat or email support reachable from every page.

### A.5 Feedback and confidence

- Every save shows a clear confirmation, such as "Rates updated. Sending to Booking.com and Agoda", followed by "All channels updated" when complete.
- A "Recent activity" feed shows who changed what, with a one-click undo for recent changes.
- A "Sync status" badge in the header shows whether everything is up to date.
- Important alerts (sync failures, conflicts) appear in the app and by email, with a direct link to fix them.

### A.6 Accessibility, devices and performance

- Fully responsive: calendar and reservations work well on phones and tablets.
- Keyboard shortcuts for power users (for example, arrow keys in the calendar, "B" for bulk update).
- WCAG 2.1 AA: strong contrast, never relying on color alone (icons and labels accompany colors), screen reader support.
- Interface language: English first, Hindi and other regional languages in a later release; local date, currency and number formats.
- Light and dark themes.
- Perceived speed: skeleton loading states, optimistic updates, and no page needing more than 2 seconds to become usable.

### A.7 Role-based simplicity

Each role sees only what it needs. A front-desk user sees reservations and the calendar; a revenue manager also sees rate tools and reports; an owner sees the dashboard and reports. Hidden complexity is revealed through an "Advanced" toggle rather than shown to everyone.

### A.8 Experience success metrics

| Metric | Target |
| --- | --- |
| Time to first successful sync | Under 30 minutes |
| Setup wizard completion rate | Above 80% |
| Usability test task success (update a rate, close a room, find a booking) | Above 90% without help |
| System Usability Scale (SUS) score | 80 or above |
| Customer satisfaction (CSAT) | 4.5 out of 5 |
| Support tickets asking "how do I..." | Under 1 per property per month |

### A.9 How we will validate

- Clickable prototype tested with 5 to 8 real hoteliers and rental hosts before development starts on each major screen.
- Moderated usability sessions each release, with task lists drawn from Section 5 user flows.
- In-product feedback button on every page and a short survey after setup completes.
- Session analytics to find where users hesitate or abandon, reviewed monthly.
- A design system (shared components, spacing, color and writing guidelines) so the product feels consistent everywhere.
