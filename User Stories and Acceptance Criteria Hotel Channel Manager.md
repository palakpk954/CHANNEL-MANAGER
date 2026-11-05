# User Stories and Acceptance Criteria: Hotel Channel Manager

*Companion to the PRD and Business Rules. Each story has an ID, a priority (P0 = MVP, P1 = Phase 2, P2 = later), and acceptance criteria written as checks that a test or a person can mark pass or fail. Business rule IDs (BR-x.x) point to the Business Rules document.*

## How to use this document

- Build a story only when all its acceptance criteria can be tested.
- A story is **done** only when its criteria pass, tests exist, and the Definition of Done is met.
- Wording on screen must follow the plain-language guide (for example "Close this room for sale", not "Stop-sell").

## Epic 1: Sign in and team access

**US-1.1 (P0) Sign in.** As a user, I want to sign in with my email and password so that I can reach my properties.

- Given correct credentials, I land on the dashboard of my last used property.
- Given wrong credentials, I see "That email or password isn't right. Try again or reset your password." and no detail about which was wrong.
- After 5 failed attempts the account is locked for 15 minutes and I am told so.
- Sessions stay active with a refresh token and end after 30 days of inactivity.

**US-1.2 (P0) Roles and access.** As an Org Admin, I want to give each teammate a role so that they only see and do what they need.

- I can invite a user by email, choose a role and choose which properties they can access (BR-7.1).
- A Reservations Staff user cannot open rate or channel settings, even by typing the address in the browser (BR-7.5).
- Changing a role takes effect on the user's next request.

**US-1.3 (P0) Separate organizations.** As an owner, I want my data kept private from other customers.

- A user from organization A requesting any record from organization B receives "not found".
- Search, exports and notifications never include another organization's data (BR-9.3).

**US-1.4 (P0) History of changes.** As a manager, I want to see who changed what and when.

- The activity page lists changes to rates, availability, mappings, permissions and credentials with user, time, old and new value (BR-7.6).
- I can filter by date, user and type of change, and entries cannot be edited or deleted.

## Epic 2: Get set up quickly

**US-2.1 (P0) Guided setup.** As a new user, I want a step-by-step setup so that I'm live without help.

- The wizard shows a progress bar with steps: Add property, Add rooms, Set rates, Connect a channel, Match rooms, Test sync, Go live.
- I can leave and come back and resume at the same step.
- Every step offers sensible defaults and a short explanation of why it matters.
- A property with up to 20 rooms can complete setup in under 30 minutes in a usability test.

**US-2.2 (P0) Try with demo data.** As a curious user, I want a sample hotel so that I can explore safely.

- I can create a demo property with 3 room types, 2 rate plans, 90 days of inventory and a mock channel with sample bookings.
- Demo data is clearly labeled and can be deleted in one click without affecting real properties.

**US-2.3 (P0) Add a property.** As an owner, I want to add my hotel's details.

- Required: name, address, timezone, currency. Optional: check-in and check-out times, contact, photos.
- Timezone defaults from the address and can be changed. Invalid fields show a message next to the field.

**US-2.4 (P0) Add room types and rate plans.** As a manager, I want to define my rooms and prices.

- Room type requires a name, code, maximum guests and number of rooms; bed type and amenities are optional.
- Rate plan requires a name, linked room type, meal plan and cancellation policy.
- I can import rooms and rates from a CSV; errors are listed by row with a downloadable error file.
- I cannot delete a room type or rate plan that has future bookings or channel mappings; I am offered "Archive" instead (Business Rules edge case 6).

**US-2.5 (P1) Derived rates.** As a revenue manager, I want child rate plans to follow a parent.

- I can set a child as parent minus or plus a percentage or amount (BR-2.2).
- Editing the parent updates children within 5 seconds, and I can override a single day.

## Epic 3: Manage availability and rates

**US-3.1 (P0) See my calendar.** As a manager, I want one calendar of availability and rates.

- The calendar shows rooms and rate plans by date with colors: green available, amber few left, red sold out, grey closed, blue outline for restrictions, and a visible legend.
- 90 days of 50 rows load in under 2 seconds.
- It is usable with the keyboard, and colors are paired with icons or text.

**US-3.2 (P0) Change one day.** As a revenue manager, I want to click a day and change it.

- Clicking a cell opens a side panel to edit rooms available, rate, minimum stay, maximum stay, "Close to arrivals" and "Close to departures" (BR-2.4, 2.5).
- Before saving I see a preview of which channels will be updated.
- After saving I see "Updated. Sending to Booking.com and Agoda", then "All channels updated".

**US-3.3 (P0) Change many days at once.** As a revenue manager, I want to update a range quickly.

- A step-by-step panel asks for dates, weekdays, rooms and rate plans, channels, new value, then shows a review screen with the number of days and channels affected.
- Applying is all or nothing (BR-2.12). Large changes (over 50% price change) ask for confirmation (BR-2.10).
- I can undo the whole change within 24 hours (BR-7.7).

**US-3.4 (P0) Close a room for sale.** As a manager, I want to stop selling a room.

- I can close a room type or rate plan for chosen dates and chosen channels without changing the room count (BR-2.3).
- Closed days show grey with a tooltip "Closed for sale on: Booking.com".

**US-3.5 (P0) Block rooms.** As a front-desk user, I want to hold rooms for maintenance or owner use.

- I can create a block with room type, date range, number of rooms and reason (BR-1.4).
- Availability drops on all channels and the block is shown in the calendar and activity page.

**US-3.6 (P0) Can't break existing bookings.** As a manager, I want warnings when I reduce rooms.

- Reducing total rooms below rooms already sold lists the affected nights and refuses the change (BR-1.5).

**US-3.7 (P1) Channel-specific prices.** As a revenue manager, I want a different price on one channel.

- I can add a markup or markdown per channel, shown as the base rate and the final channel rate (BR-2.7).

**US-3.8 (P1) Seasonal rules.** As a revenue manager, I want to set rates for seasons and events once.

- I can create a rule for a date range, weekdays and an amount or percentage, then preview and apply it.
- I can copy rates from a previous period.

## Epic 4: Connect and keep channels in sync

**US-4.1 (P0) Connect a channel.** As a manager, I want to connect an OTA.

- I pick a channel from a list with logos and enter credentials; the system tests the connection and shows "Connected" or a clear reason for failure.
- Credentials are never shown again after saving and are stored encrypted.

**US-4.2 (P0) Match my rooms to theirs.** As a manager, I want to match my rooms and rates with the channel's.

- A two-column screen suggests matches automatically and lets me change them with a dropdown or drag and drop.
- Unmatched items are highlighted, and I cannot go live until every active room is matched or deliberately skipped.

**US-4.3 (P0) Updates reach every channel.** As a manager, I want changes to go everywhere automatically.

- A change to availability, rates or restrictions is sent to every active, mapped channel within 60 seconds for 95% of changes.
- Each channel shows Waiting, Sending, Updated, Retrying or Needs attention (BR-3.3).
- If a channel is down, updates retry automatically (BR-3.4) and I am told if they fail permanently.

**US-4.4 (P0) See channel health.** As a manager, I want to know all channels are working.

- A page shows each channel with last successful update time, pending updates, errors in the last 24 hours and credential status in green, amber or red.
- Clicking a problem shows what happened and what to do.

**US-4.5 (P0) Connect with a calendar link (iCal).** As a rental host, I want to sync channels that have no direct connection.

- I can paste an import link and copy an export link for each room type.
- Imported bookings block dates; I see a note that iCal updates can take up to 15 minutes (BR-3.10).

**US-4.6 (P1) Pause a channel.** As a manager, I want to stop sending to a channel temporarily.

- Pausing keeps mappings; resuming runs a full update before normal sending (BR-3.6).

**US-4.7 (P1) Automatic safety check.** As an owner, I want mismatches fixed automatically.

- A daily check compares and corrects differences and lists them in the activity page (BR-3.7).

## Epic 5: Bookings

**US-5.1 (P0) Receive bookings from all channels.** As a front-desk user, I want one list of bookings.

- New, changed and cancelled bookings from all channels appear in one list within 60 seconds of arriving.
- Duplicate messages never create duplicate bookings (BR-4.2).
- The same booking can never reduce inventory twice.

**US-5.2 (P0) Search and filter.** As a front-desk user, I want to find a booking quickly.

- I can search by guest name, confirmation number or channel reference and filter by channel, status, dates; quick filters: Today, New, Cancelled.
- Results appear in under 1 second for 10,000 bookings.

**US-5.3 (P0) Booking details.** As a front-desk user, I want everything about a booking in one place.

- The page shows guest, dates, room, rate plan, price breakdown, payment status, requests, channel and a history of changes.

**US-5.4 (P0) Add a booking by hand.** As a front-desk user, I want to record a phone or walk-in booking.

- I choose dates, room type, rate and guest; availability is checked live; saving updates all channels (BR-4.8).
- If no room is available I am told and nothing is saved.

**US-5.5 (P0) Cancellations and changes update availability.** As a manager, I want rooms to be freed or taken automatically.

- A cancellation restores rooms for all nights and updates other channels (BR-4.5).
- A change moves the stay to its new nights in one step (BR-4.4).

**US-5.6 (P0) Bookings with missing matches.** As a manager, I want to be warned about bookings I can't place.

- A booking for an unmatched room is saved as "Needs matching", I get an alert, and once I match the room it is processed automatically (BR-4.7).

**US-5.7 (P1) Export bookings.** As an accountant, I want a file of bookings.

- I can export filtered bookings to CSV and Excel with the columns shown on screen.

## Epic 6: Never double-book

**US-6.1 (P0) The last room is sold only once.** As an owner, I want protection from overbooking.

- In an automated test, two channels booking the last room at the same moment result in one confirmed booking and one conflict, never negative availability (BR-5.1, 5.2).

**US-6.2 (P0) Clear conflict alerts.** As a manager, I want to be told immediately about a conflict.

- An urgent in-app alert and an email are sent within 1 minute (BR-5.3).
- The dashboard shows open conflicts at the top until resolved.

**US-6.3 (P0) Easy conflict resolution.** As a manager, I want simple options.

- The conflict card offers Move to another room, Contact guest, Cancel booking, and Free a room, each recorded in the activity page (BR-5.4).
- After any conflict, availability is pushed again to all channels (BR-5.6).

**US-6.4 (P1) Safety buffer.** As a manager, I want to keep a room unsold.

- I can set a buffer per room type and a maximum per channel, and the channels receive the reduced number (BR-1.6, 1.7).

## Epic 7: Dashboard, alerts and reports

**US-7.1 (P0) Today at a glance.** As an owner, I want a simple home screen.

- It shows today's arrivals, departures, occupancy, new bookings, items needing attention and channel health, readable in under 10 seconds.

**US-7.2 (P0) Alerts.** As a manager, I want to be told when something needs attention.

- Notifications appear for new bookings, cancellations, conflicts and sync problems, are grouped when repeated (BR-8.3) and link to where I can fix them.

**US-7.3 (P1) Email and daily summary.** As an owner, I want updates in my inbox.

- I can choose Instant, Daily summary or Off per alert type (BR-8.2).

**US-7.4 (P1) Reports.** As an owner, I want to understand my business.

- Reports for occupancy, average daily rate, revenue per available room, revenue and bookings by channel, cancellations and commission cost, filterable by dates and compared with a previous period.
- Reports can be exported to CSV and PDF.

## Epic 8: Ease of use and trust

**US-8.1 (P0) Plain language everywhere.** As a first-time user, I want to understand every word.

- Technical terms show a friendly label and a tooltip; every error says what happened and what to do next in one sentence.

**US-8.2 (P0) Works on my phone.** As a manager on the move, I want to check bookings and rooms.

- The dashboard, bookings and calendar work on a 375 px wide screen without sideways scrolling, with tap targets of at least 44 px.

**US-8.3 (P0) Accessible.** As a user with accessibility needs, I want to use the product comfortably.

- Key screens meet WCAG 2.1 AA: contrast, keyboard navigation, screen reader labels, and no information carried by color alone.

**US-8.4 (P0) Feels fast.** As any user, I don't want to wait.

- Screens show loading placeholders; edits appear instantly and are confirmed in the background; no key page takes over 2 seconds to be usable.

**US-8.5 (P1) Help inside the product.** As a new user, I want answers without leaving.

- Every page has a help link, empty states explain the next step, and a setup checklist tracks progress.

## Story status tracker

| Story | Priority | Prompt # | Status | Notes |
| --- | --- | --- | --- | --- |
| US-1.1 to 1.4 | P0 | 1, 2 | Not started |  |
| US-2.1 to 2.4 | P0 | 3, 10 | Not started |  |
| US-3.1 to 3.6 | P0 | 4 | Not started |  |
| US-4.1 to 4.5 | P0 | 5, 6, 8 | Not started |  |
| US-5.1 to 5.6 | P0 | 7 | Not started |  |
| US-6.1 to 6.3 | P0 | 7 | Not started |  |
| US-7.1, 7.2 | P0 | 9 | Not started |  |
| US-8.1 to 8.4 | P0 | 11 | Not started |  |
| All P1 stories | P1 | 12 to 14 | Not started |  |
