# Design System and Screen Specs: Hotel Channel Manager

*Companion to the PRD (Appendix A) and the Plain-Language Guide. This is the visual and interaction rulebook for the React app. The brand colors are green and light yellow, and the tone is formal and professional. Screenshots of the reference product, if supplied, may refine the values below.*

## 1. Design goals

1. **Calm and clear.** Hotel staff are busy. Lots of white space, one primary action per screen, no clutter.
2. **Status at a glance.** Color, icon and label together show what is fine and what needs attention.
3. **Familiar patterns.** Standard layouts (sidebar, tables, side panels) so nobody has to learn the interface.
4. **Fast.** Instant feedback, skeleton loaders, optimistic updates.
5. **For everyone.** WCAG 2.1 AA, keyboard friendly, works on phones.

## 2. Design tokens (brand: green and light yellow)

### 2.1 Color

The brand uses a deep green as the primary color and a light yellow as the warm background and highlight color. The look is formal: restrained surfaces, full sentences, no playful illustrations. Green is used for actions and navigation; status colors appear only in chips and calendar cells and are always paired with an icon and a label, so brand green is never confused with the "Available" status.

| Token | Value | Use |
| --- | --- | --- |
| `green-600` | #217A4D | Primary buttons (white text, 5.3:1 contrast), links, active navigation |
| `green-700` | #185C3A | Button hover and pressed, focus ring, headings accents |
| `green-50` | #EAF5EE | Selected rows, soft green highlights |
| `cream-50` | #FCFAF0 | Page background (light yellow tint) |
| `yellow-100` | #F8F0C6 | Soft highlight: banners, calendar header band, today marker |
| `yellow-500` | #E3C04A | Accent for progress bars and illustrations only; never used for text |
| `neutral-900` | #17211C | Main text |
| `neutral-600` | #4A5A52 | Secondary text |
| `neutral-500` | #7A867E | Input and control borders (3:1 or better) |
| `neutral-300` | #D5DAC8 | Dividers and card borders |
| `white` | #FFFFFF | Cards and panels |
| `success-600` | #187A43 | Available, Updated, Confirmed (always with a check icon) |
| `success-50` | #E6F5EC | Soft success background |
| `warning-600` | #8A5000 | Few rooms left, Retrying, Needs matching (always with an icon) |
| `warning-50` | #FFE9C7 | Soft warning background |
| `danger-600` | #B3261E | Sold out, Needs attention, Double booking |
| `danger-50` | #FDECEA | Soft error background |
| `info-600` | #1B6FA8 | Sending, Changed |
| `closed-200` | #E4E7E1 | Fill for closed-for-sale cells (with lock icon) |
| `closed-600` | #5C665F | Text and icon on closed cells |

All text and interface colors must meet a contrast ratio of at least 4.5:1 (3:1 for large text and icons). Dark theme tokens mirror these with adjusted values and must be tested separately.

### 2.2 Typography

| Style | Font | Size and weight | Use |
| --- | --- | --- | --- |
| Page title | Inter (fallback system-ui) | 24 px, semibold | One per page |
| Section title | Inter | 18 px, semibold | Cards and panels |
| Body | Inter | 14 px, regular | Default text |
| Small | Inter | 12 px, regular | Hints, legends |
| Numbers in tables | Inter with tabular numbers | 14 px | Prices and counts |

Line height 1.5 for body. Minimum text size 12 px. Never use all caps for long text.

### 2.3 Spacing, shape and motion

- **Spacing scale:** 4, 8, 12, 16, 24, 32, 48 px. Use only these.
- **Corner radius:** 8 px for cards and inputs, 999 px for chips.
- **Shadows:** one soft shadow for floating panels and menus; cards use a 1 px border.
- **Motion:** 150 to 200 ms ease-out for panels and toasts. Respect the "reduce motion" setting.
- **Grid:** 12 columns on desktop, 4 on mobile. Content max width 1440 px.
- **Breakpoints:** 375 (phone), 768 (tablet), 1024 (laptop), 1440 (desktop).
- **Tap targets:** at least 44 by 44 px on touch devices.

## 3. Core components

| Component | Rules |
| --- | --- |
| **Button** | Primary (filled blue, one per screen area), secondary (outlined), tertiary (text), danger (red, only for destructive actions). Always starts with a verb. Shows a spinner and disables itself while working. |
| **Status chip** | Rounded label with icon + word (see Plain-Language Guide status words). Never color only. |
| **Input** | Label above, hint below, error message replaces hint in red with an icon. Required fields marked in the label text, not just an asterisk. |
| **Select and combobox** | Searchable if more than 8 options. |
| **Date and range picker** | Shows the property's timezone, supports typing and clicking, quick ranges (Next 7 days, This month, Next 30 days, Custom). |
| **Table** | Sticky header, sortable columns, row hover, keyboard navigation, empty state, loading skeleton, pagination or virtual scroll. |
| **Side panel** | Slides from the right (full screen on phones), has title, content, sticky footer with the primary action. Used for editing a day, bulk update and booking details. |
| **Modal** | Only for confirmations and short decisions. Focus is trapped, Esc closes, primary button on the right. |
| **Toast** | Bottom-left, auto-dismiss in 6 seconds, never for errors that need action (use inline banners). Includes "Undo" when possible. |
| **Banner** | Inline strip at the top of a page for warnings and errors that need action, with one action button. |
| **Tooltip and "What's this?"** | One sentence under 20 words. Opens on hover, focus and tap. |
| **Empty state** | Simple illustration or icon, one sentence on what the page is for, one primary button. |
| **Skeleton loader** | Matches the shape of the content that will appear. |
| **Stepper** | Used in the setup wizard and bulk update. Shows current step, total steps and allows going back. |
| **Channel logo tile** | Logo, name, status chip and the last update time. |

## 4. Layout and navigation

**Desktop:** a left sidebar with a property switcher at the top, a top bar with the sync status badge, search, notifications and user menu, and the page content in the middle.

**Left sidebar items (in order):** Home, Calendar, Bookings, Channels, Rooms and prices, Reports, Activity, Settings. Items are hidden if the user's role has no access.

**Phone:** a bottom navigation bar with Home, Calendar, Bookings, Channels and More. The calendar becomes a vertical list of days grouped by week with horizontal swipe for rooms.

**Sync status badge (top bar):**

- Green check "All up to date"
- Blue arrow "Updating 3 changes"
- Amber "1 website retrying"
- Red "Needs attention" (opens the problem list)

## 5. Key screens (wireframes)

The wireframes show structure, not final styling. Square brackets are buttons, `( )` are chips, and `[ ]` inside a row is a checkbox or cell.

### 5.1 Home (dashboard)

```
+------------------------------------------------------------------+
| Good morning, Priya            Property: Hotel Rajmahal v        |
+------------------------------------------------------------------+
| NEEDS ATTENTION                                                  |
| ! Double booking: Deluxe Room, 12 Dec          [ Fix it ]        |
| ! Expedia needs attention: prices not sent     [ See what to do ]|
+----------------+----------------+----------------+---------------+
| Arrivals today | Departures     | Rooms filled   | New bookings  |
|      6         |      4         |     72%        |      3        |
+----------------+----------------+----------------+---------------+
| BOOKING WEBSITES                                                 |
| [Booking.com]  (Updated)  2 min ago                              |
| [Agoda]        (Updated)  2 min ago                              |
| [Expedia]      (Needs attention)                                 |
+------------------------------------------------------------------+
| LATEST BOOKINGS                                  [ View all ]    |
| Priya Sharma  Booking.com  12-14 Dec  Deluxe     (Confirmed)     |
| ...                                                              |
+------------------------------------------------------------------+
```

Rules: "Needs attention" appears only when something is wrong; when there is nothing to show it collapses to "Everything is in order". The four numbers answer the question "how is today?" in under 10 seconds.

### 5.2 Calendar

```
+------------------------------------------------------------------+
| Calendar   [< Prev] Dec 2026 [Next >]   View: (Rooms)(Prices)    |
|            [ Change many days ]   [ Hold rooms ]   Legend: ...   |
+------------------+-----+-----+-----+-----+-----+-----+-----+-----+
|                  | Mon | Tue | Wed | Thu | Fri | Sat | Sun | ... |
|                  |  8  |  9  | 10  | 11  | 12  | 13  | 14  |     |
+------------------+-----+-----+-----+-----+-----+-----+-----+-----+
| Deluxe Room      |  5  |  4  |  3  |  2  |  1! |  0x |  4  |     |
|  Room only       |4500 |4500 |4500 |4800 |5200 |5200 |4500 |     |
|  Non-refundable  |4050 |4050 |4050 |4320 |4680 |4680 |4050 |     |
+------------------+-----+-----+-----+-----+-----+-----+-----+-----+
| Standard Room    | ... |                                         |
+------------------------------------------------------------------+
Legend: green = available, amber = few left, red = sold out,
        grey = closed for sale, blue outline = special rule
```

Interactions:

- Click a cell: opens the **side panel** to edit that day (rooms, price, minimum nights, no check-ins, no check-outs, close for sale), with a preview line "Will update Booking.com, Agoda".
- Click and drag across cells: selects a range and opens the side panel for the range.
- Keyboard: arrow keys move, Enter edits, Esc cancels, `B` opens Change many days, `H` opens Hold rooms.
- Each cell shows an icon plus number so color is never the only signal (for example `!` few left, `x` sold out, lock for closed).
- Cells with unsent changes show a small clock icon until updated.
- Row groups collapse. The first column is sticky; the header row is sticky.
- Performance: virtualized rows and columns; 90 days by 50 rows loads in under 2 seconds.

### 5.3 Change many days (bulk update)

Side panel with a stepper (Step 1 of 5):

1. **Dates:** date range picker plus weekday chips (Mon to Sun).
2. **Rooms and prices:** checkboxes of room types and price plans.
3. **Booking websites:** checkboxes (default: all connected).
4. **What to change:** rooms available, price (set, increase by %, increase by amount), minimum nights, no check-ins, no check-outs, close or open for sale.
5. **Review:** "This changes prices for 45 days on 3 booking websites", with any warnings (for example a large price change) and the buttons **\[ Apply change \]** and **\[ Go back \]**.

After applying: toast "Saved. Sending to Booking.com and Agoda now." with an **Undo** link.

### 5.4 Channels list and health

```
+------------------------------------------------------------------+
| Channels                                  [ Connect a website ]  |
+------------------------------------------------------------------+
| [Booking.com]  (Updated)   Last update 2 min ago                 |
|   0 waiting  |  0 problems in 24h  |  Connection OK  [ Manage ]  |
| [Expedia]      (Needs attention)  Last update 3 h ago            |
|   14 waiting |  6 problems in 24h  |  Reconnect needed [ Fix ]   |
| [Airbnb calendar link]  (Updated)  Checks every 15 min           |
+------------------------------------------------------------------+
```

Clicking **Fix** opens a plain-language explanation ("Expedia stopped accepting our connection") and the single action to resolve it.

### 5.5 Match rooms (mapping)

```
+---------------------------+----+-------------------------------+
| YOUR ROOMS                |    | ON BOOKING.COM                |
| Deluxe Room               | -> | [ Deluxe Double Room      v ] |
|   Room only               | -> | [ Standard Rate           v ] |
|   Non-refundable          | -> | [ Non-refundable Rate     v ] |
| Standard Room             | -> | [ Choose a room...        v ] ! |
+---------------------------+----+-------------------------------+
| 1 room is not matched yet.        [ Save and continue ]        |
+------------------------------------------------------------------+
```

Auto-suggestions are marked "Suggested" and must be confirmed with one click ("Accept all suggestions"). Unmatched rows are highlighted amber with an icon, and the continue button explains what is missing.

### 5.6 Bookings list and detail

List: search box, quick filter chips (Today, New, Changed, Cancelled, Double booking), filters for website, status and dates, table with columns Guest, Website (logo), Dates, Room, Total, Status. Row click opens the detail in a side panel (or full page on phones).

Detail sections: guest, stay, rooms and prices, payment, requests, website and its reference number, history of changes. Primary actions in the footer: **Cancel booking**, **Mark as didn't arrive** (only after check-in date).

### 5.7 Double booking (conflict) screen

A focused card shows the guest, dates, website and the room that was already sold, then four large choices:

- **Move to another room** (shows available room types with the price difference)
- **Contact guest** (shows phone and email, copyable)
- **Cancel booking** (reminds about the website's cancellation rules)
- **Free a room** (shows holds and room limits that could be lifted)

Each choice records itself in the activity history and closes the alert when done.

### 5.8 Setup wizard

Full-screen, centered, one task per step. Left: step list with progress bar. Right: the step content and a **\[ Continue \]** button. Steps: Add property, Add rooms, Set prices, Connect a website, Match rooms, Test update, Go live. A "Save and finish later" link is always visible. The **Test update** step sends a safe test and shows "It worked" in green or a clear fix in amber.

## 6. Interaction rules

- **Instant feedback:** the interface updates immediately; the sync state follows quietly (clock icon, then check).
- **Preview before apply** for any bulk or risky change; **undo** for 24 hours.
- **Confirm only when it matters:** deleting, cancelling, disconnecting and very large price changes.
- **Errors:** inline near the field for forms, banners for page-level problems, toasts only for passing information.
- **Loading:** skeletons for lists and calendar; never a blank page; buttons show spinners.
- **Offline:** show a banner "You're offline. Changes will send when you're back" and queue changes locally where safe.
- **Persistent state:** remember the last property, calendar date range, filters and view mode per user.
- **Keyboard:** all actions reachable by keyboard; visible focus ring (2 px, primary color); skip-to-content link.

## 7. Accessibility checklist

1. Contrast at least 4.5:1 for text, 3:1 for icons and focus rings.
2. Never use color alone: add icon and text to every status and calendar cell.
3. Every input has a visible label and an error message linked with `aria-describedby`.
4. Calendar is a proper grid with row and column headers for screen readers, and announces changes ("Saved. Sending to 2 websites") in an `aria-live` region.
5. Modals and side panels trap focus and return it to the trigger on close.
6. Touch targets at least 44 px; no hover-only actions.
7. Text can be zoomed to 200% without breaking the layout.
8. Motion respects "reduce motion".
9. Test with a screen reader (NVDA or VoiceOver) and keyboard only before each release.

## 8. Component build order (with prompt map)

| Order | What to build first | Prompt # |
| --- | --- | --- |
| 1 | Tokens, typography, button, input, chip, toast, banner, table, modal, side panel | 1 |
| 2 | Layout: sidebar, top bar, sync badge, property switcher | 1 |
| 3 | Calendar grid and side panel | 4 |
| 4 | Stepper, bulk update flow | 4 |
| 5 | Channel tile, mapping screen | 6 |
| 6 | Bookings list, detail, conflict card | 7 |
| 7 | Dashboard cards | 9 |
| 8 | Wizard and empty states | 10 |

Document every component in a Storybook (or equivalent) with its states: default, hover, focus, disabled, loading, error, dark theme, and mobile width.
