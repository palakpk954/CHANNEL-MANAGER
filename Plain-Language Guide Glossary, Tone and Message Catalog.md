# Plain-Language Guide: Glossary, Tone and Message Catalog

*Companion to the PRD (Appendix A). Every word a user reads must follow this guide. The goal: a hotel owner who has never used a channel manager understands every screen without help.*

## 1. Writing principles

1. **Say it the way a hotelier would say it.** Use "Close this room for sale", not "Stop-sell".
2. **Short and active.** One idea per sentence. Say "Rates updated", not "The rates have been successfully updated".
3. **Say what happens next.** Every message ends with what the user can do or what the system will do.
4. **Never blame the user.** Say "We were unable to connect to Booking.com", not "You entered invalid credentials".
5. **No codes or technical words** (HTTP 500, null, payload, idempotency) on screen. Keep them in the activity log for support.
6. **Same word, same meaning, everywhere.** Pick one label per concept from the glossary and use it in buttons, menus, emails and help.
7. **Reading level:** aim for a plain reading level that a non-native English speaker can follow. Avoid idioms.
8. **Formal, courteous, professional tone.** Polite and precise. Use full words instead of contractions ("cannot", not "can't"), avoid slang and exclamation marks, address the user as "you", and use "please" for requests.

## 2. Glossary: industry term to screen label

| Industry term | Label on screen | Short explanation (tooltip) |
| --- | --- | --- |
| Channel manager | Channels | Keeps your rooms and prices the same on every booking website |
| OTA (online travel agency) | Booking website | Sites like Booking.com, Expedia or MakeMyTrip that sell your rooms |
| Channel | Booking website | (same as above) |
| Inventory | Rooms available | How many rooms of each type you can still sell |
| ARI (availability, rates, inventory) | Rooms and prices | Everything we send to booking websites |
| Stop-sell | Close this room for sale | Guests cannot book this room on the dates you pick |
| Open for sale | Open this room for sale | Guests can book again |
| CTA (closed to arrival) | No check-ins on this day | Guests can stay through this day but cannot arrive on it |
| CTD (closed to departure) | No check-outs on this day | Guests cannot leave on this day |
| Min stay / LOS | Minimum nights | The fewest nights a guest must book when arriving on this day |
| Max stay | Maximum nights | The most nights a guest can book when arriving on this day |
| Rate plan | Price plan | A price with its own rules, such as "Room only" or "Non-refundable" |
| Derived rate | Linked price plan | A price plan that follows another one, for example 10% lower |
| Base rate | Standard price | Your price before any extra changes for a booking website |
| Markup / markdown | Price adjustment for this website | Add or reduce your price on one booking website only |
| Mapping | Match rooms | Tell us which of your rooms is which on the booking website |
| Unmapped | Not matched yet | We can't send or receive for this room until it is matched |
| Sync | Update | Sending your latest rooms and prices to the booking website |
| Full sync | Refresh everything | Sends all your dates again to be safe |
| Push / pull | Send / Receive | (avoid these words on screen) |
| Reservation | Booking | A guest's stay |
| Modification | Booking changed | The guest changed dates or room |
| No-show | Guest didn't arrive | The guest never checked in |
| Overbooking | Double booking | Two guests booked the same last room |
| Conflict | Double booking to fix | A booking arrived for a room that was already sold |
| Block | Hold rooms | Take rooms off sale for maintenance or your own use |
| Allotment | Room limit for this website | The most rooms one website may sell |
| Buffer | Rooms to keep back | Rooms we keep unsold as a safety margin |
| Parity | Same price everywhere | Having the same price on every website |
| Webhook | Instant notification link | Used by developers to get updates from us |
| iCal | Calendar link | A link that shares dates between calendars |
| ADR | Average room price | Room revenue divided by rooms sold |
| RevPAR | Revenue per available room | Room revenue divided by all rooms you could sell |
| Occupancy | Rooms filled | Percent of your rooms that are booked |
| Commission | Booking website fee | What the booking website charges you |
| Audit log | Activity history | A record of who changed what and when |
| Dead job / failed sync | Update needs attention | We couldn't send an update after several tries |

*Developers: keep the industry term in the code and in the optional hint. Show the screen label first.*

## 3. Status words

| Technical state | Label on screen | Color and icon |
| --- | --- | --- |
| pending | Waiting | Grey, clock |
| sending | Sending | Blue, arrow |
| success | Updated | Green, check |
| failed (will retry) | Retrying | Amber, circular arrow |
| dead | Needs attention | Red, warning triangle |
| confirmed | Confirmed | Green, check |
| modified | Changed | Blue, pencil |
| cancelled | Cancelled | Grey, cross |
| no\_show | Didn't arrive | Grey, empty person |
| conflict | Double booking to fix | Red, warning triangle |
| needs\_mapping | Needs matching | Amber, link |

Status is never shown by color alone. Always include the icon and the label.

## 4. Message catalog

### 4.1 Confirmations

| Moment | Message |
| --- | --- |
| Rate saved, sending | Saved. The updated prices are being sent to Booking.com and Agoda. |
| All sites updated | All booking websites are up to date. |
| Room closed for sale | The room is closed for sale from 12 to 15 Dec. 3 booking websites have been notified. |
| Rooms held | 2 rooms are on hold for maintenance from 3 to 5 Jan. |
| Booking added by hand | The booking has been saved and availability has been updated on all websites. |
| Change undone | Your last change has been undone. |
| Booking website connected | Booking.com has been connected. Please match your rooms next. |
| Setup finished | Setup is complete. Your rooms and prices will now be kept in sync automatically. |

### 4.2 Errors (what happened, then what to do)

| Moment | Message |
| --- | --- |
| Wrong sign-in | The email address or password is incorrect. Please try again or reset your password. |
| Account locked | There have been too many unsuccessful attempts. You may sign in again in 15 minutes, or reset your password now. |
| Cannot connect to a website | We were unable to connect to Booking.com. Please check your details and try again. |
| Credentials expired | Booking.com is no longer accepting our connection. Please reconnect to resume updates. |
| Update failed after retries | We were unable to update Expedia after several attempts. We will continue to retry. View the recommended steps. |
| Room not matched | This room has not been matched with Agoda. Please match it so that bookings and updates can proceed. |
| Fewer rooms than booked | 3 rooms are booked on 14 Dec, so the total cannot be lower than 3. Please enter 3 or more. |
| Price looks very different | This price is 60% lower than the previous price. Please confirm that this is correct. |
| Price is zero or below | Please enter a price greater than 0. |
| Date range in the past | Only today and future dates can be changed. |
| Cannot delete a room with bookings | This room has future bookings and cannot be deleted. You may archive it instead. |
| CSV problem | 4 rows contain errors. Please download the list, correct the errors and upload the file again. |
| No permission | You do not have access to this section. Please ask an administrator to change your role. |
| Connection lost | You are offline. Your changes will be sent when the connection is restored. |
| Something unexpected | An unexpected error occurred on our side. Your data is safe. Please try again, or contact support and quote code 7F3K. |

### 4.3 Alerts and notifications

| Event | Title | Body |
| --- | --- | --- |
| New booking | New booking from Booking.com | Priya Sharma, 12 to 14 Dec, Deluxe Room. |
| Booking changed | Booking changed | Priya Sharma moved to 13 to 15 Dec. Rooms updated. |
| Booking cancelled | Booking cancelled | Priya Sharma, 12 to 14 Dec. The rooms are available again. |
| Double booking | Double booking to fix | A guest booked Deluxe Room on 12 Dec but it was already sold. Choose what to do. |
| Update needs attention | Expedia needs attention | We couldn't send your latest prices. See what to do. |
| Credentials problem | Reconnect Booking.com | Booking.com stopped accepting our connection, so updates are paused. |
| Booking needs matching | Booking needs matching | A booking arrived for a room we couldn't match. Match it to continue. |
| Daily summary | Your day at a glance | 4 arrivals, 3 departures, 2 new bookings, 1 item needs attention. |

### 4.4 Empty states

| Screen | Message and action |
| --- | --- |
| No booking websites yet | No booking websites are connected. Connect a website to begin sending your rooms and prices. Button: Connect a website |
| No bookings yet | Bookings will appear here once guests make reservations. Button: Add a booking manually |
| No rooms yet | No room types have been added. Add your first room type to begin. Button: Add a room |
| No alerts | There are no alerts. Everything is in order. |
| No search results | No results were found. Please try a different name or number. |

### 4.5 Confirmation dialogs

| Action | Title | Body | Buttons |
| --- | --- | --- | --- |
| Bulk change | Apply this change? | This changes prices for 45 days on 3 booking websites. You can undo it for 24 hours. | Apply change / Go back |
| Cancel booking | Cancel this booking? | This frees the rooms. The booking website's cancellation rules may apply. | Cancel booking / Keep booking |
| Disconnect website | Disconnect Booking.com? | We'll stop sending updates. Your matches are saved. | Disconnect / Keep connected |
| Delete demo data | Delete the demo hotel? | This only removes the demo. Your real hotels aren't affected. | Delete demo / Keep it |

## 5. Tone by situation

| Situation | Tone | Example |
| --- | --- | --- |
| Success | Brief, assuring | All booking websites are up to date. |
| Warning | Clear, measured | This price is 60% lower than the previous price. Please confirm that this is correct. |
| Error | Courteous, never blaming | We were unable to connect to Booking.com. Please check your details and try again. |
| Urgent (double booking) | Direct, action-focused | A guest has booked a room that was already sold. Please choose how to proceed. |
| Onboarding | Respectful, simple | Next, please match your rooms so that bookings can be received. |
| Empty | Informative | No room types have been added. Add your first room type to begin. |

## 6. Style rules

- **Capitalization:** sentence case for buttons, titles and menus ("Add a room", not "Add A Room").
- **Buttons:** start with a verb and say what happens ("Apply change", "Connect a website"). Avoid "OK", "Submit" and "Yes/No" alone.
- **Dates:** show as "12 Dec 2026" or "12 to 14 Dec"; use the property's local format and week start. Never show raw timestamps.
- **Money:** show the currency symbol and the property's format (for example ₹4,500). Indian number grouping (1,25,000) when the property's language is English (India).
- **Numbers:** use digits for counts and amounts ("3 rooms"), and plural forms correctly ("1 room", "2 rooms").
- **Names:** always show guest names the way the guest entered them. Never change the case.
- **Lists and tables:** keep column headings to 1 to 3 words.
- **Tooltips:** one sentence, under 20 words, no jargon.
- **Help links:** put a small "What's this?" next to any term from the glossary.
- **Translations:** write messages as full sentences with placeholders ("{count} rooms"), never glue fragments together, so the text can be translated later (Hindi and others).

## 7. Checklist before shipping any screen

1. Every label comes from the glossary or is reviewed against it.
2. Every error says what happened and what to do next.
3. No technical words, codes or raw system states are visible (except a short support code).
4. The empty state explains what the page is for and offers one clear action.
5. Buttons describe the action.
6. The text was read by a person who is not a developer or hotelier-expert.
7. All text is in the translation file, not hardcoded.
