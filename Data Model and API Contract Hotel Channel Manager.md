# Data Model and API Contract: Hotel Channel Manager

*Companion to the PRD (section 6) and Business Rules. This is the starting contract for backend and frontend. Treat it as a first draft: the agent may refine it, but must keep it in sync with Flyway migrations and the generated OpenAPI file, and record each change in the changelog at the end.*

## 1. Conventions

- **Database:** PostgreSQL 15+. Primary keys are UUIDs (`gen_random_uuid()`). Every table has `created_at` and `updated_at` (UTC, `timestamptz`).
- **Tenant scoping:** every business table carries `organization_id`. Every query filters by it. Consider PostgreSQL row-level security as a second line of defense.
- **Money:** integer minor units (for example paise or cents) plus a 3-letter currency code. Never floating point.
- **Dates:** stay nights are `date` values in the property's timezone. Timestamps are UTC.
- **Soft delete:** use `archived_at` for rooms, rate plans and channel accounts. Never hard-delete rows referenced by bookings.
- **Concurrency:** `version` integer columns for optimistic locking on inventory and rates; row locks for booking writes (BR-5.1).
- **Naming:** snake\_case tables and columns in the database, camelCase in JSON.
- **API:** `/api/v1`, JSON, Bearer JWT, UTC ISO-8601 timestamps, dates as `YYYY-MM-DD`, pagination with `page`, `size`, `sort`.

## 2. Entity overview

| Area | Tables |
| --- | --- |
| Identity | organizations, users, user\_property\_access, refresh\_tokens |
| Property setup | properties, room\_types, rate\_plans, tax\_rules |
| Calendar | daily\_inventory, daily\_rates, inventory\_blocks |
| Channels | channels, channel\_accounts, channel\_room\_mappings, channel\_rate\_mappings, sync\_jobs, channel\_payloads |
| Reservations | guests, reservations, reservation\_rooms, conflicts |
| System | outbox\_events, audit\_log, notifications, idempotency\_keys |

## 3. PostgreSQL schema (first migration draft)

```sql
-- Identity
CREATE TABLE organizations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  plan TEXT NOT NULL DEFAULT 'trial',
  status TEXT NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL REFERENCES organizations(id),
  email CITEXT NOT NULL,
  password_hash TEXT NOT NULL,
  full_name TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('SUPER_ADMIN','ORG_ADMIN','PROPERTY_MANAGER','REVENUE_MANAGER','RESERVATIONS_STAFF','READ_ONLY')),
  status TEXT NOT NULL DEFAULT 'active',
  failed_logins INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMPTZ,
  last_login_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (email)
);

CREATE TABLE user_property_access (
  user_id UUID NOT NULL REFERENCES users(id),
  property_id UUID NOT NULL,
  role TEXT NOT NULL,
  PRIMARY KEY (user_id, property_id)
);

CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  token_hash TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Property setup
CREATE TABLE properties (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL REFERENCES organizations(id),
  name TEXT NOT NULL,
  address JSONB NOT NULL DEFAULT '{}',
  timezone TEXT NOT NULL,
  currency CHAR(3) NOT NULL,
  checkin_time TIME,
  checkout_time TIME,
  calendar_horizon_days INT NOT NULL DEFAULT 365,
  settings JSONB NOT NULL DEFAULT '{}',   -- cutoff time, no-show release, price-change threshold
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  archived_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE room_types (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL REFERENCES organizations(id),
  property_id UUID NOT NULL REFERENCES properties(id),
  code TEXT NOT NULL,
  name TEXT NOT NULL,
  max_occupancy INT NOT NULL CHECK (max_occupancy > 0),
  included_occupancy INT NOT NULL DEFAULT 2,
  total_units INT NOT NULL CHECK (total_units >= 0),
  buffer_units INT NOT NULL DEFAULT 0,
  details JSONB NOT NULL DEFAULT '{}',     -- beds, amenities, photos
  archived_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (property_id, code)
);

CREATE TABLE rate_plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL REFERENCES organizations(id),
  property_id UUID NOT NULL REFERENCES properties(id),
  room_type_id UUID NOT NULL REFERENCES room_types(id),
  code TEXT NOT NULL,
  name TEXT NOT NULL,
  meal_plan TEXT NOT NULL DEFAULT 'ROOM_ONLY',
  cancellation_policy JSONB NOT NULL DEFAULT '{}',
  parent_rate_plan_id UUID REFERENCES rate_plans(id),
  derivation JSONB,                         -- {"type":"PERCENT","value":-10}
  archived_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (property_id, code)
);

CREATE TABLE tax_rules (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  property_id UUID NOT NULL REFERENCES properties(id),
  name TEXT NOT NULL,
  rate_basis_points INT NOT NULL,           -- 1200 = 12.00%
  included_in_price BOOLEAN NOT NULL DEFAULT FALSE,
  min_tariff_minor BIGINT,
  max_tariff_minor BIGINT,
  valid_from DATE, valid_to DATE
);

-- Calendar
CREATE TABLE daily_inventory (
  room_type_id UUID NOT NULL REFERENCES room_types(id),
  stay_date DATE NOT NULL,
  organization_id UUID NOT NULL,
  total INT NOT NULL,
  sold INT NOT NULL DEFAULT 0,
  blocked INT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (room_type_id, stay_date),
  CHECK (sold >= 0 AND blocked >= 0 AND sold + blocked <= total)   -- BR-1.2
);

CREATE TABLE daily_rates (
  rate_plan_id UUID NOT NULL REFERENCES rate_plans(id),
  stay_date DATE NOT NULL,
  organization_id UUID NOT NULL,
  amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
  min_stay INT, max_stay INT,
  closed_to_arrival BOOLEAN NOT NULL DEFAULT FALSE,
  closed_to_departure BOOLEAN NOT NULL DEFAULT FALSE,
  stop_sell BOOLEAN NOT NULL DEFAULT FALSE,
  is_manual_override BOOLEAN NOT NULL DEFAULT FALSE,
  version INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (rate_plan_id, stay_date)
);

CREATE TABLE inventory_blocks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  room_type_id UUID NOT NULL REFERENCES room_types(id),
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,                   -- exclusive, like a check-out date
  units INT NOT NULL CHECK (units > 0),
  reason TEXT NOT NULL,
  created_by UUID NOT NULL REFERENCES users(id),
  released_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Channels
CREATE TABLE channels (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code TEXT NOT NULL UNIQUE,                -- BOOKING_COM, EXPEDIA, AIRBNB, ICAL, MOCK
  name TEXT NOT NULL,
  capabilities JSONB NOT NULL DEFAULT '{}'  -- webhooks, batchSize, rateLimitPerMin, supportsRates
);

CREATE TABLE channel_accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  property_id UUID NOT NULL REFERENCES properties(id),
  channel_id UUID NOT NULL REFERENCES channels(id),
  display_name TEXT,
  encrypted_credentials BYTEA NOT NULL,
  credentials_key_id TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','ACTIVE','PAUSED','NEEDS_RECONNECT','ARCHIVED')),
  last_success_at TIMESTAMPTZ,
  last_full_sync_at TIMESTAMPTZ,
  settings JSONB NOT NULL DEFAULT '{}',    -- polling interval, markup, allotment caps
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE channel_room_mappings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  channel_account_id UUID NOT NULL REFERENCES channel_accounts(id),
  room_type_id UUID NOT NULL REFERENCES room_types(id),
  remote_room_id TEXT NOT NULL,
  allotment_cap INT,
  UNIQUE (channel_account_id, room_type_id),
  UNIQUE (channel_account_id, remote_room_id)
);

CREATE TABLE channel_rate_mappings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  channel_account_id UUID NOT NULL REFERENCES channel_accounts(id),
  rate_plan_id UUID NOT NULL REFERENCES rate_plans(id),
  remote_rate_id TEXT NOT NULL,
  adjustment JSONB,                         -- {"type":"PERCENT","value":5}
  UNIQUE (channel_account_id, rate_plan_id),
  UNIQUE (channel_account_id, remote_rate_id)
);

CREATE TABLE sync_jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  channel_account_id UUID NOT NULL REFERENCES channel_accounts(id),
  kind TEXT NOT NULL CHECK (kind IN ('AVAILABILITY','RATES','RESTRICTIONS','FULL','RESERVATION_PULL','ACK')),
  target_key TEXT NOT NULL,                 -- room/rate + date range, used for coalescing
  payload JSONB NOT NULL,
  source_version BIGINT NOT NULL,
  status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SENDING','SUCCESS','FAILED','DEAD','SUPERSEDED')),
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sync_jobs_due ON sync_jobs (status, next_attempt_at);

CREATE TABLE channel_payloads (             -- raw request/response, 90-day retention
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  channel_account_id UUID NOT NULL,
  direction TEXT NOT NULL,
  body JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Reservations
CREATE TABLE guests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  full_name TEXT NOT NULL,
  email TEXT, phone TEXT, country CHAR(2),
  anonymized_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reservations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  property_id UUID NOT NULL REFERENCES properties(id),
  channel_account_id UUID REFERENCES channel_accounts(id),   -- null for manual
  source TEXT NOT NULL,                      -- CHANNEL, MANUAL, BOOKING_ENGINE
  channel_reference TEXT,
  status TEXT NOT NULL CHECK (status IN ('CONFIRMED','MODIFIED','CANCELLED','NO_SHOW','CONFLICT','NEEDS_MAPPING')),
  guest_id UUID REFERENCES guests(id),
  check_in DATE NOT NULL,
  check_out DATE NOT NULL CHECK (check_out > check_in),
  currency CHAR(3) NOT NULL,
  base_amount_minor BIGINT NOT NULL,
  tax_amount_minor BIGINT NOT NULL DEFAULT 0,
  commission_amount_minor BIGINT,
  total_amount_minor BIGINT NOT NULL,
  payment_status TEXT,
  special_requests TEXT,
  raw_payload JSONB,
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (channel_account_id, channel_reference)               -- BR-4.2 idempotency
);
CREATE INDEX idx_res_property_dates ON reservations (property_id, check_in, check_out);

CREATE TABLE reservation_rooms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reservation_id UUID NOT NULL REFERENCES reservations(id),
  room_type_id UUID NOT NULL REFERENCES room_types(id),
  rate_plan_id UUID REFERENCES rate_plans(id),
  adults INT NOT NULL DEFAULT 1,
  children INT NOT NULL DEFAULT 0,
  nightly_breakdown JSONB NOT NULL          -- [{"date":"2026-12-12","amountMinor":450000}]
);

CREATE TABLE conflicts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  property_id UUID NOT NULL,
  reservation_id UUID NOT NULL REFERENCES reservations(id),
  status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED')),
  resolution TEXT,                           -- MOVED, GUEST_CONTACTED, CANCELLED, ROOM_FREED
  resolved_by UUID REFERENCES users(id),
  resolved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- System
CREATE TABLE outbox_events (
  id BIGSERIAL PRIMARY KEY,
  organization_id UUID NOT NULL,
  aggregate_type TEXT NOT NULL,
  aggregate_id UUID NOT NULL,
  event_type TEXT NOT NULL,
  payload JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_events (id) WHERE published_at IS NULL;

CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  organization_id UUID NOT NULL,
  property_id UUID,
  actor_user_id UUID,
  actor_type TEXT NOT NULL DEFAULT 'USER',  -- USER, SYSTEM, CHANNEL
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  action TEXT NOT NULL,
  batch_id UUID,                             -- groups a bulk change for undo (BR-2.12, 7.7)
  before JSONB, after JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id UUID NOT NULL,
  user_id UUID NOT NULL REFERENCES users(id),
  type TEXT NOT NULL,
  severity TEXT NOT NULL DEFAULT 'INFO',    -- INFO, WARNING, URGENT
  payload JSONB NOT NULL,
  group_key TEXT, occurrences INT NOT NULL DEFAULT 1,
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE idempotency_keys (
  scope TEXT NOT NULL, key TEXT NOT NULL,
  response JSONB, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (scope, key)
);
```

**Notes for the agent**

- The `daily_inventory` check constraint is the last line of defense for BR-1.2. The service layer must also lock rows (`SELECT ... FOR UPDATE`) in a consistent order (room type, then date ascending) to avoid deadlocks.
- `audit_log.before/after` must never contain credentials. Redact before writing.
- Create `daily_inventory` and `daily_rates` rows ahead of time for the property's calendar horizon, and extend them with a scheduled job.

## 4. API contract (v1)

### 4.1 Standard error format

Every error returns this shape. `message` is the plain-language text from the Plain-Language Guide; `code` is for developers and support.

```json
{
  "error": {
    "code": "INVENTORY_BELOW_SOLD",
    "message": "You have 3 rooms booked on 14 Dec, so you can't go below 3. Choose 3 or more.",
    "details": [{"field": "totalUnits", "dates": ["2026-12-14"]}],
    "supportCode": "7F3K",
    "traceId": "4bf92f3577b34da6"
  }
}
```

Status codes: 400 validation, 401 not signed in, 403 no permission, 404 not found (also used for other organizations' data), 409 conflict (version or business rule), 422 rule violation, 429 rate limited, 500 unexpected.

### 4.2 Authentication

| Method and path | Purpose | Request | Response |
| --- | --- | --- | --- |
| POST `/auth/login` | Sign in | `{ "email", "password" }` | `{ "accessToken", "refreshToken", "expiresIn", "user": {id, name, role, properties[]} }` |
| POST `/auth/refresh` | New access token | `{ "refreshToken" }` | `{ "accessToken", "expiresIn" }` |
| POST `/auth/logout` | End session | `{ "refreshToken" }` | 204 |
| POST `/auth/password/reset-request` | Start reset | `{ "email" }` | 202 (always, to avoid revealing accounts) |
| GET `/me` | Current user | none | user, role, accessible properties |

### 4.3 Property setup

| Method and path | Purpose |
| --- | --- |
| GET, POST `/properties` | List and create properties |
| GET, PUT `/properties/{pid}` | Read and update a property |
| POST `/properties/demo` | Create demo property with sample data |
| GET, POST `/properties/{pid}/room-types` | List and create room types |
| GET, PUT, DELETE(archive) `/properties/{pid}/room-types/{id}` | Read, update, archive |
| GET, POST `/properties/{pid}/rate-plans` | List and create price plans |
| GET, PUT, DELETE(archive) `/properties/{pid}/rate-plans/{id}` | Read, update, archive |
| POST `/properties/{pid}/import` | CSV import of rooms and rates; returns row errors |
| GET, PUT `/properties/{pid}/setup-progress` | Onboarding wizard state |

### 4.4 Calendar

**GET `/properties/{pid}/calendar?from=2026-12-01&to=2026-12-31&view=rooms|rates`**

```json
{
  "from": "2026-12-01", "to": "2026-12-31",
  "roomTypes": [{
    "id": "rt_1", "name": "Deluxe Room",
    "days": [{"date": "2026-12-12", "total": 10, "sold": 8, "blocked": 1, "available": 1, "level": "LOW"}],
    "ratePlans": [{
      "id": "rp_1", "name": "Room only",
      "days": [{"date": "2026-12-12", "amountMinor": 450000, "minStay": 2, "closedToArrival": false, "closedToDeparture": false, "stopSell": false}]
    }]
  }]
}
```

**POST `/properties/{pid}/calendar/preview`** returns what a change would do before saving.

```json
// request (same body as bulk update)
{ "dateFrom": "2026-12-10", "dateTo": "2026-12-20", "weekdays": ["FRI","SAT"],
  "roomTypeIds": ["rt_1"], "ratePlanIds": ["rp_1"], "channelAccountIds": ["ca_1","ca_2"],
  "set": { "amountMinor": 520000, "minStay": 2 } }
// response
{ "daysAffected": 4, "channelsAffected": ["Booking.com","Agoda"],
  "warnings": [{"type": "LARGE_PRICE_CHANGE", "message": "This price is 60% lower than before. Is that right?"}],
  "errors": [] }
```

**PUT `/properties/{pid}/calendar/bulk`** applies it (all or nothing). Returns `{ "batchId", "daysAffected", "syncJobsCreated" }`. Send `Idempotency-Key` header.

**POST `/properties/{pid}/calendar/undo/{batchId}`** undoes a bulk change within the allowed window.

**Blocks:** GET, POST `/properties/{pid}/blocks`, DELETE `/properties/{pid}/blocks/{id}` (release).

### 4.5 Channels

| Method and path | Purpose |
| --- | --- |
| GET `/channels` | Channels you can connect, with capabilities |
| GET, POST `/properties/{pid}/channel-accounts` | List and connect (credentials in body, never returned) |
| POST `/channel-accounts/{id}/test` | Test the connection; returns `{ok, message}` |
| PATCH `/channel-accounts/{id}` | Pause, resume, rename, update settings |
| GET `/channel-accounts/{id}/remote-options` | Rooms and rates available on the channel, for matching |
| GET, PUT `/channel-accounts/{id}/mappings` | Read and save room and rate matches (with suggestions) |
| POST `/channel-accounts/{id}/refresh` | Run a full update ("Refresh everything") |
| GET `/properties/{pid}/channel-health` | Status of every channel |
| GET `/channel-accounts/{id}/sync-jobs?status=` | Recent updates and failures |
| POST `/sync-jobs/{id}/retry` | Retry a failed update |
| GET `/properties/{pid}/rooms/{rtId}/ical-export-url` | Calendar link for export |

**Channel health response**

```json
{ "channels": [{
  "channelAccountId": "ca_1", "name": "Booking.com", "status": "ACTIVE", "health": "GREEN",
  "lastSuccessAt": "2026-12-01T09:12:00Z", "pendingUpdates": 0, "failedLast24h": 0, "credentialStatus": "OK",
  "unmappedRooms": 0
}]}
```

### 4.6 Reservations

| Method and path | Purpose |
| --- | --- |
| GET `/properties/{pid}/reservations?q=&status=&channel=&from=&to=&page=` | Search and filter |
| GET `/reservations/{id}` | Full detail with history |
| POST `/properties/{pid}/reservations` | Manual booking (checks availability) |
| POST `/reservations/{id}/cancel` | Cancel |
| POST `/reservations/{id}/no-show` | Mark guest didn't arrive |
| GET \`/properties/{pid}/reservations/export?format=csv | xlsx\` |
| POST `/webhooks/{channelCode}/{channelAccountId}` | Inbound channel messages (signature checked, idempotent) |

**Manual booking request**

```json
{ "checkIn": "2026-12-12", "checkOut": "2026-12-14",
  "rooms": [{"roomTypeId": "rt_1", "ratePlanId": "rp_1", "adults": 2, "children": 0}],
  "guest": {"fullName": "Priya Sharma", "email": "priya@example.com", "phone": "+919800000000"},
  "totalAmountMinor": 900000, "currency": "INR", "notes": "Late arrival" }
```

If no room is free the response is 422 with `NO_AVAILABILITY` and the booking is not saved.

### 4.7 Conflicts, dashboard, notifications, activity

| Method and path | Purpose |
| --- | --- |
| GET `/properties/{pid}/conflicts?status=OPEN` | Double bookings to fix |
| POST `/conflicts/{id}/resolve` | `{ "resolution": "MOVE", "targetRoomTypeId": "rt_2" }` or `CONTACTED`, `CANCEL`, `FREE_ROOM` |
| GET `/properties/{pid}/dashboard` | Arrivals, departures, occupancy, new bookings, attention items, channel health |
| GET `/notifications`, POST `/notifications/{id}/read`, POST `/notifications/read-all` | In-app alerts |
| GET, PUT `/me/notification-preferences` | Instant, daily summary, off |
| GET `/properties/{pid}/activity?from=&to=&user=&type=` | Activity history (audit log) |
| GET \`/properties/{pid}/reports/{occupancy | revenue-by-channel |

### 4.8 Users and settings

| Method and path | Purpose |
| --- | --- |
| GET, POST `/users`, PATCH `/users/{id}` | Manage team, invite, change role and property access |
| GET, PUT `/organization` | Organization settings |
| GET, PUT `/properties/{pid}/tax-rules` | Tax settings |

### 4.9 Real-time updates

Server-Sent Events at `GET /properties/{pid}/events` push: `sync.job.updated`, `reservation.created`, `reservation.updated`, `conflict.created`, `notification.created`. The frontend uses these to update sync status badges and alerts without refreshing.

## 5. Permissions matrix (summary)

| Action | Org Admin | Property Manager | Revenue Manager | Reservations Staff | Read-only |
| --- | --- | --- | --- | --- | --- |
| Manage users | Yes | No | No | No | No |
| Edit property setup | Yes | Yes | No | No | No |
| Edit rates and restrictions | Yes | Yes | Yes | No | No |
| Change room counts | Yes | Yes | No | No | No |
| Hold rooms (blocks) | Yes | Yes | Yes | Yes | No |
| Connect and edit channels | Yes | Yes | No | No | No |
| View and create bookings | Yes | Yes | Yes | Yes | View only |
| Resolve conflicts | Yes | Yes | No | Yes | No |
| View reports | Yes | Yes | Yes | No | Yes |
| View activity history | Yes | Yes | Yes | No | Yes |

## 6. Changelog

| Date | Change | By |
| --- | --- | --- |
|  | *(The agent records every schema or API change here and in the Flyway migration name.)* |  |
