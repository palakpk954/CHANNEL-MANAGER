# Business Rules: Hotel Channel Manager

*Companion to the PRD. Every rule has an ID (BR-x.x) so the agent, tests and tickets can reference it. Where a rule says "configurable", build it as a setting, never hardcode it.*

## 1. How to use this document

- These rules decide what the system does in situations the PRD does not spell out. If code and a rule disagree, the rule wins.
- If a rule is missing for a situation, the agent must state its assumption in one line, add it to the "Assumptions log" at the end, and continue.
- Each rule should map to at least one automated test.

## 2. Inventory

| ID | Rule |
| --- | --- |
| BR-1.1 | Inventory is tracked per **room type per night**. A night is identified by its date in the **property's timezone**. |
| BR-1.2 | `available = total - sold - blocked`. It can never be stored below zero. Attempts to go below zero create a Conflict (see section 6), not a negative number. |
| BR-1.3 | A booking for check-in 10th and check-out 12th consumes the nights of the 10th and 11th. The check-out date is never consumed. |
| BR-1.4 | A **manual block** (maintenance, owner use) reduces availability on every channel and has a reason and a date range. Blocks are visible in the calendar and the audit log. |
| BR-1.5 | **Total units** can be changed at any time, but cannot be set lower than units already sold on any future night. The system must show the nights that would break and refuse. |
| BR-1.6 | An optional per-room-type **buffer** (default 0) is subtracted from availability before it is sent to channels. The internal calendar still shows the real number. |
| BR-1.7 | An optional **per-channel allotment cap** limits how many units one channel may sell for a night. The value sent to that channel is the lower of the real availability and the cap. |
| BR-1.8 | Inventory for dates in the past is read-only. Today's date is editable until the property's configurable cutoff time (default 23:59 local time). |
| BR-1.9 | The calendar horizon is configurable per property (default 365 days ahead). Dates beyond it are not pushed to channels. |

## 3. Rates and restrictions

| ID | Rule |
| --- | --- |
| BR-2.1 | A rate belongs to a **rate plan** and a **night**. The rate for a stay is the sum of its nightly rates. |
| BR-2.2 | **Derived rate plans** compute from a parent plan using a percentage or fixed amount (example: Non-refundable = parent minus 10%). Editing the parent updates children automatically. A manual override on a child date takes priority until cleared. |
| BR-2.3 | **Stop-sell** (shown to users as "Close this room for sale") removes the room type or rate plan from sale for that night on the selected channels. It does not change inventory counts. |
| BR-2.4 | **Minimum stay** and **maximum stay** are evaluated against the **arrival date** of the booking. |
| BR-2.5 | **Closed to arrival (CTA)** blocks arrivals on that date but allows guests already staying through it. **Closed to departure (CTD)** blocks departures on that date. |
| BR-2.6 | If restrictions conflict, the strictest wins (any stop-sell, CTA or CTD blocks the booking). |
| BR-2.7 | **Channel adjustments** (markup or markdown, as a percentage or fixed amount) are applied last, just before sending. The calendar shows the base rate, and the channel view shows the final rate. |
| BR-2.8 | Rates use the property's currency. Channels that need another currency use a stored, dated exchange rate set by the user; the system never fetches rates silently. |
| BR-2.9 | Rates are rounded to the property's configured precision (default 2 decimals, or 0 for INR if the user chooses) using round-half-up, after all adjustments. |
| BR-2.10 | A rate of zero or a negative rate is rejected with a clear message. A rate that differs from the previous value by more than a configurable threshold (default 50%) asks for confirmation before saving. |
| BR-2.11 | **Extra person and child pricing** is defined per room type: included occupancy, extra adult amount, extra child amount, and child age bands (configurable). |
| BR-2.12 | Bulk updates are applied atomically per property. Either all selected dates change or none do, and the audit log records one entry that can be undone as a group. |

## 4. Channel synchronization

| ID | Rule |
| --- | --- |
| BR-3.1 | The internal calendar is the **single source of truth**. Channels receive data from it; they never overwrite it, except through reservations. |
| BR-3.2 | Every change to inventory, rates or restrictions creates sync jobs for each active, mapped channel. Unmapped rooms or rate plans are skipped and reported on the channel health page. |
| BR-3.3 | Each sync job has a state: `pending`, `sending`, `success`, `failed`, `dead`. Users see these as: "Waiting", "Sending", "Updated", "Retrying", "Needs attention". |
| BR-3.4 | Failed jobs retry with exponential backoff (suggested: 30 seconds, 2 minutes, 10 minutes, 1 hour, 6 hours). After the last attempt the job becomes `dead`, a notification is created, and the channel health shows a warning. |
| BR-3.5 | Jobs for the same channel, room and date are **coalesced**: if a newer update is queued, older pending updates for the same target are dropped. Order is preserved by a version number. |
| BR-3.6 | A channel that is paused or disconnected receives no pushes. When re-enabled, the system performs a **full sync** for the horizon before resuming incremental updates. |
| BR-3.7 | A **scheduled full sync** runs at least once a day per channel (off-peak in the property's timezone) as a safety net, and a **reconciliation** compares channel-reported availability with the internal value where the channel allows it. Differences are corrected and logged. |
| BR-3.8 | Respect each channel's rate limits and batch sizes using the connector's declared capabilities. Never send more requests than a channel allows. |
| BR-3.9 | Credentials that fail authentication pause the channel immediately, create an urgent notification, and ask the user to reconnect. Retries do not continue with bad credentials. |
| BR-3.10 | **iCal channels** are limited to blocking dates. They cannot carry rates or restrictions. Import polling defaults to every 15 minutes (configurable, minimum 5). Export feeds are generated on request with caching of up to 5 minutes. The UI must explain that iCal updates are not instant. |
| BR-3.11 | Raw request and response payloads for sync and reservations are stored for 90 days (configurable) to support support and debugging. Credentials and card data are never included. |

## 5. Reservations lifecycle

| ID | Rule |
| --- | --- |
| BR-4.1 | A reservation has the status: `confirmed`, `modified`, `cancelled`, `no_show`, `conflict`. A modified booking keeps its history. |
| BR-4.2 | Every inbound reservation is identified by **channel + channel reference**. Receiving the same message twice must not create a duplicate (idempotency). |
| BR-4.3 | **New booking:** validate mapping, check availability, create the reservation, decrement inventory for every night, then enqueue availability updates for all other channels, in a single transaction (the pushes happen after commit through the outbox). |
| BR-4.4 | **Modification:** release the old nights and consume the new ones in one transaction. If the new nights are not available, create a Conflict and keep the old booking intact until staff decide. |
| BR-4.5 | **Cancellation:** restore the inventory for all nights and enqueue availability updates everywhere. Cancelling an already cancelled booking changes nothing. |
| BR-4.6 | **No-show:** staff may mark a booking as no-show after the check-in date. Inventory for the stay nights after the first night is released only if the property setting allows it (default: release). |
| BR-4.7 | A reservation from a channel whose rooms are **not mapped** is saved with status `needs_mapping`, a notification is sent, and inventory is not changed until the mapping is fixed. Then it is processed automatically. |
| BR-4.8 | **Manual reservations** (phone or walk-in) follow the same inventory rules as channel bookings and are tagged with source `manual` and the staff user. |
| BR-4.9 | Guest personal data is stored only as needed. Card numbers are never stored; only the payment status, virtual card reference or channel payment type provided by the OTA. |
| BR-4.10 | Where a channel requires an acknowledgement of a booking, send it after the reservation is committed. If acknowledgement fails, retry per BR-3.4 and show a warning on the reservation. |
| BR-4.11 | Group or multi-room bookings consume inventory per room line. If any line cannot be fulfilled, the whole booking becomes a Conflict. |

## 6. Overbooking and conflicts

| ID | Rule |
| --- | --- |
| BR-5.1 | Inventory changes for one room type and night are serialized with a lock (database row lock or distributed lock) so two channels cannot both take the last unit. |
| BR-5.2 | When a booking arrives for a room type with no availability, the booking is still **recorded** (the guest booked on the OTA), marked `conflict`, and does not make availability negative. |
| BR-5.3 | A conflict creates an urgent in-app alert and an email to Property Managers and Reservation Staff, showing the guest, dates, channel and options. |
| BR-5.4 | Resolution options, each recorded in the audit log: **Move to another room type** (if available, optionally with an upgrade note), **Contact guest**, **Cancel booking** (the user is reminded of the channel's cancellation policy and any penalty), **Free a room** (remove a manual block or lower the buffer). |
| BR-5.5 | A conflict stays open and visible on the dashboard until resolved. Unresolved conflicts older than 2 hours trigger a reminder (configurable). |
| BR-5.6 | After a conflict, the system immediately re-pushes availability (zero) to all channels to prevent further oversells. |

## 7. Taxes, fees and money

| ID | Rule |
| --- | --- |
| BR-6.1 | Tax rules are **configurable per property** (name, rate, whether tax is included in the displayed rate, and rate slabs by room tariff). Do not hardcode tax slabs such as GST bands; they change over time and should be confirmed with the owner's tax advisor. |
| BR-6.2 | The system records for each reservation: base amount, taxes, fees, commission (if provided by the channel) and total. It records what the channel reports and does not recalculate the OTA's amounts. |
| BR-6.3 | Commission is tracked for reporting only (configurable percentage per channel). It does not affect the rate sent to channels unless the user applies a markup rule. |
| BR-6.4 | Money is stored as integers in minor units (or exact decimals) with a currency code. Floating point is never used for money. |

## 8. Users, roles and permissions

| ID | Rule |
| --- | --- |
| BR-7.1 | Roles: Super Admin (platform), Org Admin, Property Manager, Revenue Manager, Reservations Staff, Read-only. |
| BR-7.2 | Reservations Staff can view and create reservations, view the calendar and add manual blocks, but cannot change rates or channel settings. |
| BR-7.3 | Revenue Managers can edit rates, restrictions and channel adjustments but cannot manage users or credentials. |
| BR-7.4 | Only Org Admins and Property Managers can connect, edit or delete channel credentials and mappings. |
| BR-7.5 | Permissions are enforced on the server. Hiding a button in the interface is never the only protection. |
| BR-7.6 | Every change to inventory, rates, restrictions, mappings, permissions and credentials is written to the audit log with user, time, old value and new value. Audit entries cannot be edited or deleted by users. |
| BR-7.7 | **Undo** is available for bulk changes made in the last 24 hours (configurable) by the same user or an Org Admin, and creates its own audit entry. |

## 9. Notifications

| ID | Rule |
| --- | --- |
| BR-8.1 | In-app notifications are created for: new booking, modification, cancellation, conflict, sync failure (dead job), channel credential problem, unmapped reservation, and daily digest. |
| BR-8.2 | Urgent events (conflict, credential failure) also send email immediately. Other events follow the user's preferences (instant, digest, or off). |
| BR-8.3 | Duplicate alerts for the same ongoing problem are grouped and shown with a count rather than repeated. |

## 10. Data retention and privacy

| ID | Rule |
| --- | --- |
| BR-9.1 | Guest personal data is kept for a configurable period after check-out (default 3 years), then anonymized. Financial totals are retained longer for reporting. |
| BR-9.2 | Users can export or delete a guest's personal data on request, and the action is logged. |
| BR-9.3 | All data is isolated per organization. A user can never see another organization's data, including through search, exports or notifications. |
| BR-9.4 | Backups follow the retention in the PRD (daily, point-in-time recovery) and restored environments must not send real emails or channel pushes. |

## 11. Edge cases checklist (each needs a test)

1. Two channels book the last room at the same moment.
2. The same webhook arrives twice, or arrives out of order (cancellation before creation).
3. A booking spans the daylight-saving or month boundary, or the property is in a different timezone than the server.
4. Total units reduced below already-sold units.
5. A channel is down for hours, then recovers with many queued updates.
6. A room or rate plan is deleted while it has future bookings or channel mappings (must be blocked or archived, not deleted).
7. A bulk update covers a date range partly in the past.
8. Credentials expire in the middle of a sync run.
9. A modification changes room type and dates at the same time.
10. A guest name or request contains special characters, emoji or very long text.

## 12. Assumptions log

| Date | Assumption | Decided by | Status |
| --- | --- | --- | --- |
|  | *(The agent adds each assumption here; the product owner confirms or changes it.)* |  | Open |
