# 06 — Trip wizard: create, edit, duplicate, guest; pickers and airport search

Research for the native Android ChronoFlow app (Kotlin, Jetpack Compose). Date: 2026-11-15.

Notation used throughout:

- **[F]** is a fact verified in source, with a path (and line numbers where useful).
- **[R]** is a recommendation for Android.
- **[OQ n]** is an open question. Each one has a recommended default, so the plan can go ahead without an answer (see §12).

Abbreviations:

| Short | Path |
|---|---|
| iOS | `/Users/rickgout/Sites/chronoflow-ios` |
| Kit | `iOS/Packages/ChronoFlowKit/Sources` |
| W | `/Users/rickgout/Sites/chronoflow-new` (Laravel; checked-out branch `feat/mobile-api-passkey-management` @ `1f4a2684`) |
| RN | `/Users/rickgout/Sites/chronoflow-mobile` |
| C | `/Users/rickgout/Sites/chronoflow-android/_design/canvas` (canvas version 1794774555-3fd6) |

---

## 0. Answer: can we reuse the iOS research and plan?

**Yes, a lot of it.** The iOS wizard is fully specified and built: `plan.md` Phases 13, 16 (16a–16c), 17d, 18 and "No health data", plus `Docs/wizard-*.md`. About 3.5k lines of it are pure, platform-free logic that ports line for line to Kotlin.

The table separates what we can port from what we must not copy.

| Reuse as is (port 1:1 to Kotlin, JVM unit tests) | Source |
|---|---|
| Form model: legs, stays, end, override, preparation; the 16-flight cap; stop insert/remove; add/remove return | `Kit/TripWizard/Model/WizardForm.swift` |
| Route editor: From/stops/To list, `routeComplete`, `setRouteAirport`, `makeDirect` | `Kit/TripWizard/Model/WizardRoute.swift` |
| Wall-clock date model (`YYYY-MM-DDTHH:mm`, fixed UTC calendar, `instant(value, zone)`, DST-gap rejection) | `Kit/TripWizard/Model/WizardDateTime.swift` |
| Picker bounds and initial values (`minimumLocal`, `initialLocal`) | `Kit/TripWizard/Model/WizardFlightTiming.swift` |
| Client validation and error-section labels | `Kit/TripWizard/Model/WizardValidation.swift` |
| Times-screen rules: arrival order, past arrival, connection/overnight labels, the 23:00–07:00 complete-night rule | `Kit/TripWizard/Model/WizardOutboundRules.swift` |
| Request building, snapshot → form, server error path → form path → step | `Kit/TripWizard/Model/WizardJourney.swift`, `Kit/DataAccess/TripWizardErrors.swift` |
| Navigation path derived from state; back targets | `Kit/TripWizard/Model/MemberWizardNavigation.swift`, `MemberWizardReview.swift` |
| Review application: routing on status/prompts/errors, exclusion "change answers", progress weights | `Kit/TripWizard/Model/MemberWizardReview.swift` |
| Checking timing (0.4/0.8/≥1.2 s ticks, +0.3 s pause, slow at 10 s, fail at 30 s) | `Kit/TripWizard/Model/WizardChecking.swift` |
| Fine-tune, Ready, save, re-check, credits state, fingerprint | `Kit/TripWizard/Model/MemberWizardCompletion.swift` |
| Edit/Duplicate load, source-change detection | `Kit/TripWizard/Model/TripWizardActions.swift` |
| Guest-mode rules, guest questions, guest progress | `Kit/TripWizard/Model/WizardMode.swift` |
| Optional-answer validation (drinks, blue-blocker rating, free-day times) | `Kit/TripWizard/Model/WizardOptionalAnswer.swift` |
| Generation service: review dedupe, `review_expired` retry, edit source refresh, save receipts, effects | `Kit/TripWizard/Generation/*.swift` |
| Airport search pacing (350 ms debounce, ≥1.02 s spacing, 429 wait) | `Kit/TripWizard/Model/AirportSearchService.swift` |
| Retired-key stripping for no-health-data | `Kit/AppCore/Contracts/ProfilePreferences.swift` |
| Recovery-row semantics (fixed id `member-wizard`, 30 min, instance/revision guards) | `Kit/TripWizard/Model/MemberWizardStore.swift`, `Kit/LocalStore/DraftRepository.swift:37` |
| **278-key en/nl wizard catalog** (all Dutch present) | `Kit/TripWizardUI/Resources/Localizable.xcstrings` |
| **313-key generated plan/review copy catalog** (server `CopyLine`s) | `Kit/PlanCore/Resources/PlanCopy.xcstrings` (+ `PlanCopy.json`) |
| Wire fixtures (review, save, 422s, 429, snapshot, guest request) | `iOS/Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures/wizard/*.json` |
| 172 host tests in 28 suites (port as JUnit test specs) | `iOS/Packages/ChronoFlowKit/Tests/TripWizardTests/*.swift` |
| Settled decisions and open-question answers (OQ 40, 41, 43, 44, 46, 47; the no-health-data scope; board resolutions) | `iOS/plan.md:1787-2210`, `:2376-2425`, `:2477-2530` |

What is **not** reusable (iOS-specific) or must change on Android:

- iOS's single combined date+time pill (OQ 46): WizardCopy prescribes a different Android rule (§9.5).
- SwiftUI `NavigationStack` stale-pop workarounds, `ViewThatFits`, the Duo rail and safe-area repairs (`plan.md:1882-1891`), and the XCUITest seeding seams (`CF_WIZARD_BOARD_CAPTURE`).
- Keychain storage for the guest secret: use Android Keystore instead (§9.3).
- Two iOS values differ from the boards: progress weights (§4.6) and the duplicate back button (§3.4). For these, Android should follow the boards (OQ 1, OQ 2).

---

## 1. Sources and which boards are binding

- **[F] WizardSpec is the binding behaviour contract.** `C/WizardSpec.dc.html` (text is quoted in §3). It ends with a **Replaces** list: TripEditor, TripEditorErrors, EditorStates, TripReview (+NL, Offline, Update), ReviewAnswer, ReviewStates, GuestFlight/Sleep/Health/Review/States, TripDiscard, TripDiscardEdit and TripCalculating ("now WizardCalculating"). The open-jaw flight-home board is removed.
  - The iOS plan treats this as binding: "The superseded TripEditor*, TripReview* and GuestFlight* boards are never built" (`iOS/plan.md:1789`).
- **[F] `C/Wizard.dc.html`** (80 KB, 529 lines) draws every wizard screen. Props are `screen`, `v`, `guest`, `pct`, `large`, `slow`, `alert`, `theme` and `lang` (`:225`). It holds the full en/nl copy table `T` with 246 keys (`:228`).
- **[F] `C/WizardSheet.dc.html`** draws every answer sheet and search sheet. Props are `kind` (airport, stop, seat, room, checkout, wake, busy, free, drinks, light, blue, reminders), `field` (from, to, home, second) and `err` (`:83`).
- **[F] `C/WizardCopy.dc.html`** holds the strings the boards show only one case of, plus the accessibility, **Android** and layout rules (§8, §9).
- **[F] Still valid and not in the Replaces list:** `PickerDateTime`, `PickerTime`, `TripAirportSearch` and `TripSearchStates`. They render over superseded backdrops (`TripEditor`, `GuestSleep`), but the pickers and search states they draw are still current.
- **[F] The canvas notes `dirNative` and `reqSystemChrome` (in `C/canvas.json`) set the platform direction:** "Switches, segmented controls, the date and time picker and confirmation alerts are the system's, tinted amber where the OS allows it. Sheets are native sheets: their corners, grabber and detents come from the OS." What stays custom: cards, answer lists, type, colour and icons.
- **[F] Retired by no-health-data (do not build):** WizardQCaffeine, WizardQEye, WizardQClot, WizardQBipolar and WizardQPregnant (`iOS/plan.md:2080`; W `docs/ui-plan/phase-19-no-health-data.md` P19-D1/D4). Only `age_band` and `shift_work` can block.
- **[F] Not built on iOS:** WizardCheckPractice and WizardCheckPracticeDarkNL, because practice mode does not exist (`iOS/plan.md:1801`, `:2231`).

---

## 2. Board → state map

Wrappers are 0.8 KB files that `<dc-import name="Wizard" …>` the big board with props. Every `*DarkNL` twin is the same state in `theme=dark lang=nl`, so it is omitted from the table. Here `pct` is the progress value the board draws.

### 2.1 Wizard (screen/v) boards

| Board | Props | State it represents |
|---|---|---|
| WizardWhere / Dark / NL | `where` pct 15 | Where, member, From prefilled ("From your last trip"), To set, direct |
| WizardWhereEmpty | `where v=empty` pct 8 | Nothing picked: placeholders, swap disabled (opacity .35), Continue disabled, × closes at once |
| WizardWhereGuest | `where guest` pct 9 | Guest Where: no last-trip hint |
| WizardWhereSame | `where v=same` pct 15 | From = To: red ring on the To row and the line "From and To are the same airport. Choose where you land."; Continue disabled |
| WizardWhereStop | `where v=stop1` | One stop row ("Stop", × remove), swap hidden, segmented control on Yes, "Add another stop" shown |
| WizardWhereStops | `where v=stop2` | Two stops ("Stop 1", "Stop 2") |
| WizardHomeRoute | `where v=home` pct 50 | Flight-home route: From locked (lock icon, "Where you landed"), To = outbound origin |
| WizardHomeRouteStop | `where v=homestop` pct 50 | Flight-home route with a stop |
| WizardOutbound / Dark / NL | `outbound` pct 30 | Times, one flight, complete, "Flight time 11 h 35 min" |
| WizardOutboundEmpty | `outbound v=empty` pct 22 | Pills empty ("Date", "Time", muted); Continue disabled |
| WizardOutboundErrors | `outbound v=errors` | Arrival before departure: arrival date pill red, line "Check the departure and arrival times." |
| WizardOutboundTwoErrors | `outbound v=twoerr` | Two flights, error under the 2nd card |
| WizardOutboundStop | `outbound v=stop` | Two flights + chip "Connection in Dubai · 2 h 40 min" |
| WizardOutboundStopEmpty | `outbound v=stopempty` | Two flights, all empty, chip "Stop in Dubai" |
| WizardOutboundStopNew | `outbound v=stopnew` | Stop just added: middle times empty |
| WizardOutboundOvernight | `outbound v=overnight` | Chip "Stop in Dubai · 1 complete night" |
| WizardOutboundThree | `outbound v=three` | Three flights: the content scrolls, Continue stays fixed |
| WizardOutboundPast | `outbound v=past` | After Checking: error banner with the member past-arrival copy |
| WizardOutboundPastGuest | `outbound v=past guest` pct 18 | Guest past-arrival copy (no "add your return flight") |
| WizardOutboundGuest | `outbound guest` pct 18 | Guest Times |
| WizardDuplicate | `outbound v=dup` pct 22 | Duplicate opens on Times: empty dates, sub "Copied from Amsterdam → Tokyo. Choose the new dates.", no back chevron (first screen) |
| WizardReturn / Dark / NL | `ret` pct 45 | Coming back?: One way / Add my return flight; no button; "Tap an answer to continue." |
| WizardReturnWeb | `ret v=editweb` | Edit of a web trip: third choice selected, "Ends after 9 nights" |
| WizardReturnFlight | `outbound v=retflight` pct 55 | Flight-home times ("When do you fly home?"), dates empty |
| WizardReturnFlightPast | `outbound v=retpast` pct 55 | Flight-home times, past banner with the guest/with-return copy |
| WizardSleep | `sleep guest` pct 30 | Your sleep (only the guest variant is drawn; the member variant uses the sub "On a work day. We save it for next time.") |
| WizardChecking / Dark / NL | `checking` pct 60 | Checking, ticks 1–2 done, 3 busy |
| WizardCheckingAgain | `checking v=again` pct 70 | "Checking your answers": line order Your answers, Flights and time zones, What we can tailor |
| WizardCheckingSlow | `checking slow` | Adds "This is taking longer than usual. You can keep this screen open." |
| WizardCheckingGuest | `checking guest` pct 42 | Line 2 is "Your answers" |
| WizardCheckingEdit | `checking v=edit` pct 100 | "Checking your changes" |
| WizardCheckOffline / Server / Throttle | `fail v=offline/server/throttle` pct 60 | Failure card (cloud/alert/clock icon) + "Try again" / "Try again in 42 s" (disabled) + Close |
| WizardCheckOfflineEdit / ServerEdit | `fail v=editoffline/editserver` pct 100 | Edit bodies add "If you close now, your trip stays as it was."; no back chevron |
| WizardCheckPractice | `fail v=practice` | **Not built** (no practice mode) |
| WizardQAge | `question v=age guest` pct 54 | Age, guest, with the disclaimer line |
| WizardQShift / NL / Large | `question v=shift guest` pct 64 | Shift-work question; Large = larger text (options 72 px high, 22/28 type) |
| WizardQCaffeine/Eye/Clot/Bipolar/Pregnant | — | **Retired** (no-health-data) |
| WizardExcluded | `excluded guest` pct 60 | No plan, under 18; note "If you answered by mistake…" |
| WizardExcludedShift | `excluded v=shift` | Member, *saved* shift-work answer: note "You gave this answer on an earlier trip. Change it to plan this trip." |
| WizardExcludedEdit | `excluded v=edit` | Lines: shift work + doctor; note "Your trip stays as it was." |
| WizardFineTune / Dark / NL | `finetune` pct 80 | Six member rows (room*, wake*, seat, checkout, busy, drinks); `*` = "Helps a lot" |
| WizardFineTuneMulti | `finetune v=multi` | One row per stay/flight: "Dark room · Dubai", "Side of the plane · AMS → DXB" |
| WizardFineTuneSleep | `finetune v=sleep` | "Use different sleep times for this trip" on: two time pills + note |
| WizardFineTuneBusy | `finetune v=busy` | After a change: Continue shows a spinner and "Checking…" (disabled) |
| WizardFineTuneNone | `finetune v=none` pct 95 | From Ready, no extras: sub "No extra questions…", button "Done" |
| WizardFineTuneGuest | `finetune guest` pct 86 | Guest rows: room, wake, seat, drinks, light, blue; no sleep switch |
| WizardReady / Dark / NL | `ready` pct 95 | Ready, 1 credit, "You have 3 credits" |
| WizardReadyReturn | `ready v=retnew` | Return trip, flight "· via DXB", summary-return copy |
| WizardReadyUnlimited | `ready v=unlimited` | "Included in your subscription." with no "Price" label |
| WizardReadyFree | `ready v=free` | "Free" + small-shift line |
| WizardReadyOneFree | `ready v=onefree` | Flight home free + balance |
| WizardReadyAdjusted | `ready v=adjusted` | Info banner "We adjusted your plan" + extra row "Latest wake-up 08:00 · assumed" |
| WizardReadyChecking | `ready v=checking` | Re-check: button spinner "Checking…", disabled |
| WizardReadyNoCredits | `ready v=nocredits` | Red "You have 0 credits"; button "Buy credits on the website" (→ SetPaywall) + Close |
| WizardReadyGuest / GuestFree | `ready guest [v=free]` | Guest rows + guest price lines; button → GuestSignup |
| WizardSaveOffline / SaveServer | `ready v=saveoffline/saveserver` | Error banner on Ready |
| WizardEdit | `ready v=edit` pct 100 | "Edit your trip"; "Update my plan" disabled; price "Free" + edit note; × closes at once |
| WizardEditChanged | `ready v=editchanged` | Info banner "Updated" / "On another device: …" |
| WizardEditNoReturn | `ready v=editnoreturn` | Return removed: "One way", "Free", "Removing a flight doesn't give a credit back." |
| WizardEditReturn | `ready v=editreturn` | Return added: 1 credit, balance, edit-return note |
| WizardEditReturnNoCredits | `ready v=editreturnnone` | Return added, 0 credits: no-credits treatment |
| WizardEditWeb | `ready v=editweb` | Return row "9 nights · until Wed 21 Oct", dates "· 9 nights" |
| WizardCalculating | `calc` pct 100 | Spinner card, "AMS → NRT · 6 days of steps"; no × or back |
| WizardDiscard | `outbound alert=discard` | "Discard this trip?" alert |
| WizardDiscardGuest | `outbound guest alert=guest` | Adds "You go back to the welcome screen." |
| WizardDiscardEdit | `ready v=editreturn alert=edit` | "Discard your changes?" / "Your trip stays as it was." / Keep editing / Discard changes |

### 2.2 WizardSheet (kind/field) boards

| Board | Props | Content |
|---|---|---|
| WizardAirportFrom / AirportHome | `airport field=from/home` | Full-height sheet: Cancel / title ("Where do you fly from?" / "…to?" / "Where do you fly home to?") / search field (query "Am") / result rows (code 44 px column, name, "City, Country") |
| WizardStopSearch / Second / Home | `stop field=—/second/home` | "Where's your stop?" + caption "Between Amsterdam and Tokyo" / "Between Dubai and Tokyo" / "Between Tokyo and Amsterdam" |
| WizardFineTuneSheet | `seat` | Caption "AMS → NRT"; Window left / Window right / Aisle or not sure ("We plan without the window") |
| WizardSheetRoom | `room` | When I land / From a time + "From 15:00" time row |
| WizardSheetCheckout | `checkout` | Yes / No |
| WizardSheetWake | `wake` | Inline hour/minute wheel (drawn 08:30) |
| WizardSheetBusy | `busy` | Ranges ("Fri 9 Oct · 09:00–17:30") with circular remove + "Add busy hours" |
| WizardSheetFreeSleep | `free` | Same as work days / Different times + Bedtime/Wake-up pills |
| WizardSheetDrinks / DrinksError | `drinks [err]` | "None" option + 4 steppers (Coffee, Tea, Soft drinks, Energy drinks) "Cups a day"; error ring + "Add at least one drink to save this answer.", Done disabled |
| WizardSheetLight | `light` | None / Light box / Light glasses |
| WizardSheetBlue / BlueError | `blue [err]` | No / Yes, with a rating / Yes, no rating + "Rating on the label 1.5"; error "Enter a rating between 0.1 and 9.9." |
| WizardReminders | `reminders` | Over the Plan page: "Turn on reminders?", body, "Turn on reminders" / "Not now" |

### 2.3 Other boards in scope

| Board | Status | Content |
|---|---|---|
| PickerDateTime | current (iOS-shaped) | Sheet: title "Flight 1 departs", clock icon + "Local time at Amsterdam Schiphol (AMS)", system calendar, "Time 14:40", summary chip "Mon 12 Oct · 14:40 Amsterdam time", Cancel/Done |
| PickerTime | current (iOS-shaped) | Sheet "Usual bedtime" / "On a normal work or school day." / hour+minute wheel / "Steps of 5 minutes." / Done/Cancel |
| TripAirportSearch | current | "Choose the arrival airport", "Flight 1 · To", "5 airports" count, rows |
| TripSearchStates | current | (1) under 2 chars: field hint "Type at least 2 letters to search."; (2) Searching: spinner "Searching airports…" + 3 skeleton rows (48 px, radius 14); (3) No results: dashed card "No airports found" / "Try the city name, like “Tokyo”, or the 3-letter code on your ticket, like “NRT”."; (4) Error: "**We couldn't search airports.** Check your connection and try again." + outline "Try again" |
| TripEditor, TripEditorErrors, EditorStates, DarkTripEditor | superseded | Do not build. EditorStates' "This trip can't be edited here yet" is only a reference for OQ 9 |
| TripReview(+NL/Offline/Update), DarkTripReview, ReviewStates, ReviewAnswer, TripCalculating, TripNoCredits | superseded | Do not build. TripNoCredits/TripCalculating are wrappers over TripReview |
| TripDiscard, TripDiscardEdit | superseded | Replaced by the WizardDiscard* alerts |
| GuestSignup, Failed, Retry, Review | current (auth doc) | Sheet over guest Ready (iOS OQ 47); its backdrop GuestReview is superseded |

---

## 3. Behaviour spec (WizardSpec, verified text)

**[F]** The following is quoted or condensed from `C/WizardSpec.dc.html`.

### 3.1 Paths

- Returning member, simple: Where (route) → Times → Coming back? (One way) → Checking → Fine-tune → Ready → Calculating.
- With a return: … → Coming back? (Add my return flight) → Your flight home (route) → Its times → Checking → …
- Longest guest path: Where → Times → Your sleep → Checking → Age → Shift work → *(Caffeine, now retired)* → Checking again → Fine-tune → Ready → sign-up sheet. Guests are one-way only.
- **Your sleep** shows only until a member has answered it once ("Not sure" counts). It is gated by `account.settings.sleepAnswered == false` (W phase-17 P17-D6). Guests always see it.
- **Fine-tune** is skipped when the server suggests no extras. Ready's Extras row then reads "3 days prep" and opens Fine-tune with only the preparation days and the switch (WizardFineTuneNone).
  - Exception: guests always get the drawn Fine-tune (iOS 18n, `plan.md:2201`).
- **Duplicate** opens on Times. Airports are filled in, dates cleared, the title is "New trip", there is no last-trip hint, and the sub reads "Copied from {route}. Choose the new dates." Coming back? is preselected when the original had a return.

### 3.2 No drafts; quiet recovery

- × asks "Discard this trip?" only when something was entered; otherwise it closes at once. There are no drafts, no "Keep draft" and no draft card.
- Guests get the same alert plus "You go back to the welcome screen." The ×, failure and No-plan exits go to Welcome.
- Edits ask "Discard your changes?" and the trip stays as it was.
- **Quiet recovery:** reopening within 30 minutes returns to the same step with the same values. The record is deleted after 30 minutes, on × or after a save. If the app was killed during the save, it reopens on Calculating and retries, with no extra message.

### 3.3 Header, progress, back

- Full-screen modal over the tabs. Close is always × on the right (Android too: canvas note `wiz4Rick`, "Android keeps × on the right, per your rule"; WizardCopy "Android").
- From step 2 there is a back chevron on the left, plus swipe-back.
- Progress is a filling bar that never jumps back. When Checking adds questions, the bar holds and the rest fills in smaller steps.
- **Back on Checking:** the chevron is hidden, but "swipe and Android back still work". Back cancels the check and returns to the last input step.
- **Back on the first question** goes to the last input step, skipping Checking.
- **During Calculating:** × and back are hidden; "swipe and Android back do nothing".
- The main button sits above the home indicator and rises with the keyboard.
- Single-choice screens have no button: a tap moves on after **250 ms**. Taps are ignored until the next screen shows, and a button disables on its first tap until the response arrives.

### 3.4 Back targets and first screens (board logic, `C/Wizard.dc.html:236-262`)

- A screen is a **first screen** (no back chevron) when it is:
  - Where (not the home route),
  - Duplicate's Times,
  - Edit's Ready,
  - an edit failure screen (`fail` in edit mode).
- Checking and Calculating never show the back chevron.
- `closeHref`:
  - closes at once when nothing was entered (`where v=empty`) or on an unchanged edit (`ready v=edit`);
  - otherwise opens the discard alert.
- **[F] iOS deviation:** iOS keeps Where on the stack under Duplicate's Times (`MemberWizardNavigation.swift:29-31` returns `[.outbound]` over the Where root), so back reaches Where. iOS also shows back on the edit Checking failure (`TripWizardUI/MemberWizardSteps.swift:133`). See OQ 2.

### 3.5 Checking

- One request. The screen stays up at least 1.2 s.
  - Line 1 ticks at 0.4 s, line 2 at 0.8 s, line 3 when the response arrives (never before 1.2 s).
  - After the last tick there is a 0.3 s pause, then the next screen.
- **Reduce Motion:** the arc dot stays put, ticks appear without scaling, the spinner becomes a static dot. The timing is the same.
- **Screen reader:** the title is read on arrival. Each line is announced as it ticks: "Flights and time zones, done" / "Vluchten en tijdzones, klaar". The open line reads "What we can tailor, in progress" / "Wat we kunnen afstemmen, bezig".
- **Slow:** after about 10 s, "This is taking longer than usual. You can keep this screen open." After 30 s it counts as a server error.
- **Re-checks:**
  - After the last blocking answer: "Checking your answers", with the bar held.
  - After a change in Fine-tune: Continue shows a spinner ("Checking…") and opens Ready.
  - An unchanged Fine-tune goes straight to Ready.
- **Trip-wide messages** go in the banner slot under the title of the *last flight screen*: Outbound for one-way, Your flight home for a return. The slot takes:
  - the past-arrival message;
  - any trip-wide server line: planner rejection, "Try saving again shortly.", "This setting cannot be changed here.", too many attempts.
  - Edits never get the past-arrival message.
  - "Check the departure and arrival times." stays at the field.
- **Too many attempts:**
  - With a wait time: "Too many attempts. Try again in {count} seconds." and the button counts down ("Try again in 42 s"), enabled at 0.
  - Without one: "Too many attempts. Please try again later." and Try again stays enabled.

### 3.6 Back to Ready, re-check rules

- From Ready (new, guest or edit), each row opens its step. Done/Continue on an unchanged step goes straight back to Ready.
- A changed step goes through Checking, then back to Ready, skipping later steps unless one is now needed (a new blocking question shows first).
- A checked trip **expires after 30 minutes**. Ready checks again:
  - after buying credits,
  - after quiet recovery,
  - when it was left open past 30 minutes,
  - when an edited trip changed on another phone.
- During a re-check the button shows "Checking…" and Ready stays. If the price or summary changed, a banner says so ("On another device: …" for remote changes).
- A no-credits Ready checks silently when the app comes back to the front.

### 3.7 Haptics (WizardSpec "Haptics")

| Event | Kind |
|---|---|
| Choice card, chronotype, answer, picker value, each stepper step, the switch | Selection |
| Swap, add stop, remove stop | Light impact |
| Checking finishes and the next screen appears; plan saved | Success |
| Inline error after Continue; trip-wide message; No plan | Warning |
| Checking or saving failed (offline, server, practice, throttle) | Error |
| "Make my plan", "Update my plan" | Medium impact |

### 3.8 Decisions (WizardSpec "Decisions")

- Questions use the server's wording. Titles over 80 characters use a smaller size (board: 24/30 vs 30/36; large text 29/35 vs 36/42). One yes/no question per screen.
- Editing opens on Ready ("Edit your trip"). "Update my plan" is disabled until something changes.
  - Editing is free, except adding a return to a one-way trip, which costs 1 credit.
  - Removing the return gives no credit back; Ready shows "Free".
- **Route first, then times (Rick, 9 Nov):**
  - Where sets the whole route. "Do you change planes on the way?" (No, direct / Yes) shows once From and To are picked.
  - Yes opens the stop search. A stop appears as a row between From and To, with ×.
  - "Add another stop" adds the next stop before the destination; its search names the two airports it splits.
  - "No, direct" removes all stops. Swap shows only on a direct route.
  - At 16 flights, "Add another stop" is replaced by "That's the most we can plan: 16 flights."
- **Times:**
  - One card per flight, labelled with its route.
  - The connection/overnight line between cards is information only.
  - New flights start with empty pills.
  - With 3+ flights the screen scrolls, Continue stays fixed, and an error after Continue scrolls to the first error.
- **Flight home:**
  - Its route step: From fixed (lock icon), the same stop question, To follows the outbound origin until changed, no stops copied.
  - Then its times ("When do you fly home?", dates empty).
  - There is no open jaw. A server rejection shows "Your flight home must leave from the airport you landed at." in the banner slot.
- **Web trips ending after nights or on a check-out date:**
  - Coming back? shows that end as a third, selected choice.
  - The Ready Return row reads "9 nights · until Wed 21 Oct" or "Check-out · Wed 21 Oct".
  - The app cannot create these ends.
- **Reminders:** after the first saved trip, a one-time sheet appears over the trip page once Calculating is done. It shows only when notification permission was never asked and reminders are on. "Not now" means never again.
- **Errors at the field:** a red outline on the pill or row, and one line under it.

---

## 4. State machine (Android model = port of iOS)

### 4.1 Modes and entries

**[F]** `Kit/TripWizard/Model/WizardMode.swift:6-9`, `TripWizardActions.swift:56-89` and `MemberWizardStore.swift:157-188`. The Android port keeps one wizard and one model, with a mode flag and an optional edit context.

| Entry | Start | Initial state |
|---|---|---|
| **Create** (My trips +, empty My trips, empty/completed Today "Plan a trip") | `whereTo` | From = latest non-deleted trip's origin (`fromLastTrip=true`); `firstTrip = no trips`; `sleepNeeded = !settings.sleepAnswered`; sleep prefilled from saved body clock |
| **Edit** (trip actions "Edit trip") | `checking` (immediate review) | `GET /v1/trips/{id}/input` → form; `edit = {tripID, sourceRevision}`; `originalInput = fingerprint`; `sleepNeeded=false`; `entered=false`; `progressFloor=0.95`; `returnToReady=true` |
| **Duplicate** (trip actions "Duplicate trip") | `outbound` (Times) | `GET input`, then `clearDuplicateDates()`: all depart/arrive/leave-for-airport cleared, declared room modes reset, busy cleared, nights/check-out cleared, the end becomes one-way unless it was `return_legs`; identity cleared; `entered=true`; `copiedRoute="Amsterdam → Tokyo"` |
| **Guest** (Welcome "Plan my trip", sign-in footer "Plan a trip first") | `whereTo` | `mode=guest`; profile 23:00/07:00; prep 3; `one_way`; `sleepNeeded=true`; `lastInputStep=.sleep` |
| **Seeded from guest** (after password/passkey login with a live guest draft and `tripId==null`) | `checking` | Guest form copied; identity cleared; override default; profile empty; `returnToReady=true` (`MemberWizardStore.start(seed:)`) |

Trip actions disable Edit/Duplicate with the reason "Needs a connection." when offline or busy, or when the card's `canEdit`/`canDuplicate` is false (`iOS/plan.md:1928-1929`).

### 4.2 Steps

**[F]** `enum MemberWizardStep { whereTo, outbound, returnChoice, returnRoute, returnFlight, sleep, checking, question, excluded, continuation, fineTune, ready, calculating }` (`WizardMode.swift:8-11`). `continuation` is legacy and decodes to `fineTune`/`ready` (`MemberWizardStore.swift:143-145`).

The derived navigation path (`MemberWizardNavigation.swift:27-56`):

```
whereTo                               → []
outbound                              → [outbound]
returnChoice   (member only)          → [outbound, returnChoice]
returnRoute    (end == return_legs)   → … + returnRoute
returnFlight                          → … + returnRoute, returnFlight
sleep (guest || sleepNeeded || lastInputStep==sleep || step==sleep) → … + sleep
checking                              → … + checking
question(i)                           → … + question(0..i)
excluded                              → … + excluded
fineTune                              → … + fineTune
ready / calculating                   → … + [fineTune if optionalPrompts non-empty] + ready [+ calculating]
```

Pops are rejected while `calculating`, and when the presented stack differs from the model's (a stale pop).

### 4.3 Transitions per step

**Where (`whereTo`)**

- Continue is enabled when `routeComplete()`: ≥2 airports, none empty, first ≠ last, no adjacent equal.
- Choosing an airport (`chooseAirport`, `MemberWizardModel.swift:107-138`) applies these rules:
  - **from:** sets route position 0 and clears `fromLastTrip`. When a return exists and `homeChanged` is false, the home To follows.
  - **to:** sets the last position.
  - **stop (new):** `insertStop(beforeDestinationOf: lastOutboundLeg)`. It splits the last leg; the previous leg's arrival is cleared; the new flight is empty.
  - **stop (replacing):** sets that route position.
  - Allowed airports: a stop may not equal either of the two airports it splits; other positions may not equal a neighbour (`airportIsAllowed`).
- Swap works only when `returnBoundary == 1` (direct) and both airports are set. When a return exists and `homeChanged` is false, the home To follows the new origin.
- "No, direct" calls `makeDirect()`, which repeatedly removes stop 1. Removing a stop joins two legs, keeping the first departure and the last arrival (`WizardForm.removeStop`).
- Continue → `outbound`. If revisiting from Ready with times complete, it goes to `finishFlightInput` instead.

**Times (`outbound`)**

- Continue requires every outbound leg to have valid depart and arrive values.
- `finishOutbound()` runs `WizardOutboundRules.errors(checkPast:false)`:
  - client validation on the outbound legs;
  - arrival ≤ departure in airport instants gives "Check the departure and arrival times.";
  - a departure before the previous arrival gives "This flight leaves before the previous one lands." on the later card.
- With no errors: member → `returnChoice`; guest → `finishFlightInput` (Sleep or Checking).

**Coming back? (`returnChoice`, member only; a tap auto-advances after 250 ms)**

- **One way:** `removeReturn()`, `end = one_way`, then `finishFlightInput`.
- **Add my return flight:** `addReturn()`. The new home leg starts from the last arrival, its To is set to the outbound origin, `returnRouteConfirmed=false`, `homeChanged=false`. Then → `returnRoute`.
- **Web end (edit only):** keep `nights`/`check_out`, then `finishFlightInput`.

**Your flight home, route (`returnRoute`)**

- From is locked. Continue requires `routeComplete(home:true)` and sets `returnRouteConfirmed=true` → `returnFlight`.

**Your flight home, times (`returnFlight`)**

- Same card rules as Times. `finishReturn()` runs `WizardFlightRules.errors(checkPast:false)` over all legs, then `finishFlightInput`.

**`finishFlightInput`** (`MemberWizardReview.swift:110-118`)

- If revisiting with an unchanged fingerprint, go to Ready.
- Else if `sleepNeeded`, go to `sleep`.
- Else `beginChecking()`.

**Your sleep (`sleep`)**

- Bed, wake and chronotype are required. "Not sure" sends `chronotype: null`, and the key must be present.
- Duration `(wake − bed + 1440) % 1440` must be in **180…840 min**. Otherwise the field error `profile.habitual_bed` = "Check your bedtime and wake-up time." (`MemberWizardReview.swift:92-108`).
- `sleepCompleted=true`, then `beginChecking()`.

**Checking (`checking`)**

- `beginChecking` records `lastInputStep` (returnChoice/returnFlight/sleep) and runs one review with the injected clock (§3.5). Duplicate starts are ignored while a check runs.
- `applyReview` (`MemberWizardReview.swift:209-262`):
  1. The edit source reload is applied first (§4.5).
  2. The form is rebuilt from the effective input (preserving local ids and cities). `itinerary` = review facts. The `profile` echo is kept. `updated` is true when the input is unchanged but the summary or price changed. `reviewedAt=now`, `checkedInput=fingerprint`.
  3. A member with `sleepCompleted` gets `sleepNeeded=false`.
  4. Client flight rules run with `checkPast:true`. Any error goes to the owning step (`WizardJourney.errorStep`): Where for route errors, Times for outbound, home route/times for return. The past-arrival message goes in `review` (the banner).
  5. `questions` = blocking prompts that `ProfilePreferences.isSupported` accepts.
  6. `needs_answers` with no supported questions is treated as a server failure.
  7. `no_plan` → `excluded`.
  8. Questions present → `question(0)`.
  9. Otherwise → `ready` if `returnToReady` or (member and no optional prompts); else `fineTune`.
- `applyFailure`:
  - 422 field errors → `fineTune` for `override.*`, `preparation.*`, `stays.*`, `seatWindow`.
  - `profile.habitual_bed/wake/chronotype` → `sleep`; other `profile.*` → `fineTune`.
  - Everything else goes to the step owning the field.
  - Offline, server and throttled stay on Checking as a failure state. Throttled carries `retryAt`.

**Question (`question(i)`; a tap auto-advances after 250 ms)**

- Accepted answers:
  - `age_band` ∈ shape options (`under_18`, `18_64`, `65_plus`);
  - `shift_work` ∈ {true, false}.
- The next question is `i+1`. After the last: `checkingAnswers=true` → `beginChecking()` ("Checking your answers").
- Back from `i>0` goes to `i−1`; back from 0 goes to `lastInputStep` (skipping Checking).

**No plan (`excluded`)**

- Shows the server `reasons` (CopyLines).
- "Change my answers":
  - Member: nulls the excluding answers (`age_band=under_18` → null, `shift_work=true` → null, including answers saved on the account), then `checkingAnswers=true` → re-check. The server then asks again.
  - Guest: nulls both answers and restarts at `question(0)` with the local guest questions.
- Close exits (to Welcome for guests).
- `exclusionWasSaved` (the excluding answer came from the saved profile and not this session) selects the WizardExcludedShift note.

**Fine-tune (`fineTune`)**

- Rows: `optionalPrompts` = non-blocking supported prompts, required ones first.
  - `cups` is hidden when the `exclude_caffeine` preference is on.
  - Guests only see room, latest_wake, seat_side, cups, light_device and blue_blockers.
- Plus:
  - preparation days 0–4;
  - the member per-trip sleep switch (override bed/wake);
  - two neutral preference toggles that have no board: "Leave caffeine out of my plan" / "Your plan won't suggest any caffeine." and "Add movement breaks during long-flight sleep" + help text (`TripWizardUI/MemberWizardFineTuneView.swift:33-41`; iOS design exception, `plan.md:2486-2488`).
- Done on a sheet applies only the prompt's declared fields (`answerOptional`); Cancel leaves the form alone.
- Continue: unchanged fingerprint → Ready; changed → inline review (`reviewInPlace`: the button shows "Checking…", the screen stays) → Ready, or questions if new blocking prompts appear.

**Ready (`ready`)**

- Rows open their steps through `revisit(step)`. This stores the `revisit` fingerprint and sets `returnToReady=true`.
  - Member Sleep row: `revisit(.fineTune, sleep:true)` turns the override on, prefilled from the profile.
  - Guest Sleep row: goes to `sleep`.
- `readyTick` runs on appear, on foreground and on a timer. When `needsReadyReview` holds (no identity, changed fingerprint, or `reviewedAt` ≥ 1800 s), it calls `reviewInPlace` (guest: re-run Checking).
- "Make my plan" / "Update my plan":
  - medium haptic;
  - re-review if needed;
  - else `calculating` + save (member) or `signUpRequested` (guest).
- `canSave = !reviewing && retrySeconds==0 && (!editing || hasChanges) && (savedTripId != nil || review.canSave)`.

**Calculating (`calculating`)**

- The save runs once. × and back are locked.
- `saved(id)`:
  - persist `savedTripId` *before* any pull;
  - `didSave` (queue the first-trip reminders offer, delete recovery);
  - close and open the trip page with `awaitingSync=true`.
- `reReviewNeeded` (review_expired / not enough credits): clear identity, `savedTripId`, `reviewedAt` and `checkedInput` → Ready + forced re-check.
- `failed`: → Ready with `saveFailure` {offline | server | throttle(retryAt)}. There is no automatic retry.

### 4.4 Credits / price states

**[F]** `MemberWizardCompletion.swift:22-41`, W `PresentMobileReview.php`.

| `WizardCredits` | Condition | Ready lines (WizardCopy "Price lines") |
|---|---|---|
| `unavailable` | `price == null` or (`!canSave && saveBlockedReason != null`), i.e. "Not enough credits" | "Price 1 credit"; red "You have 0 credits"; button "Buy credits on the website" + Close (board) **or** Close only (iOS, OQ 3) |
| `unlimited` | `price.unlimited` | "Included in your subscription." (no "Price" label) |
| `free` | `cost == 0` | "Price **Free**"; "This trip is free: only a small body-clock shift is needed." (not for edits) |
| `oneFree` | `freeDirections` contains `return` | "Your flight home is free: only a small shift is needed that way." + balance |
| `credits` | otherwise | "Price **1 credit**" / "{count} credits"; "You have {count} credits" (singular/plural) |
| guest | always, balance = `SIGNUP_CREDITS` = 3 | "Paid with {cost} of the 3 credits you get when you sign up." / free: "Your sign-up credits stay unused." |
| edit, no return added | — | "Free" + "Changes recalculate the plan. Check-offs on steps that stay in your plan are kept."; return removed: + "Removing a flight doesn't give a credit back." |
| edit, return added | cost 1 | "1 credit" + balance + "Adding a return flight costs 1 credit, because credits are per direction. Other changes are free." |

`price.state` is one of `unlimited | free | create | edit | paid` (generated enum `ReviewCreditState`).

### 4.5 Edit specifics

**[F]** `TripGenerationReview.swift`, `TripWizardActions.swift:92-112`.

- Every edit review first calls `GET /v1/trips/{id}/input` again. If `sourceRevision` changed, the stored input replaces the form, `identity` and `profile` are dropped, and `sourceChange` records which of route/flight/returnFlight/sleep/extras differ. That drives the "Updated / On another device: …" banner.
- If the review's `identity.sourceRevision` differs from the edit context, the app refreshes and re-reviews once. A second mismatch gives `reviewExpired`.
- The save is `PUT /v1/trips/{id}` with `source_revision`.
- "Update my plan" stays disabled while `originalInput == fingerprint`.
- Copy for the other "On another device" fields is app-authored (canvas note `wiz24cCopy` 1); the iOS catalog has it.

### 4.6 Progress

**[F] iOS member weights** (`MemberWizardReview.swift:30-46`):

| Step | Value |
|---|---|
| Where | .08 (incomplete) / .15 (complete) |
| Times | .22 / .30 |
| return steps | .45 |
| sleep | .50 |
| checking | .52 (.70 when checking answers) |
| question | .54 + .16·i/n |
| fineTune | .80 |
| ready | .95 |
| calculating | 1.0 |

- The floor `progressFloor` is set on entering checking/question/…/calculating, so progress never moves back.
- **iOS guest weights** (`WizardMode.swift:68-80`): where .09/.18, outbound .30, sleep .42, checking .54/.64, question 0 → .54, i>0 → .64, fineTune .86, ready .95.
- **[F] The boards differ.** Member boards: HomeRoute 50, ReturnFlight 55, Checking 60, CheckingAgain 70. Guest boards: Where 9, Outbound **18**, Sleep **30**, Checking **42**, QAge 54, QShift 64, FineTune 86, Ready 95.
  - The iOS guest table is shifted one step against its own stated source ("guest progress uses the board weights: 9 / 18 / 30 / 42 / 54 / 64 / 86 / 95", `plan.md:2080`).
  - **[R] Use the board values** (OQ 1) and keep the floor rule.

### 4.7 Recovery record (process death and app kill)

**[F]** `MemberWizardStore.swift`, `DraftRepository.swift:37`, `iOS/Docs/wizard-16a.md`.

- One row in the account DB `drafts` table, `id = "member-wizard"`. It holds the form, the full state (step, mode, sheet, `promptSheetID`, airports, profile, review, questions, index, `checkedInput`, `reviewedAt`, `revisit`, `saveFailure`, `savedTripId`, edit context, `originalForm`/`originalInput`, `copiedRoute`, `snapshotStayEnd`, `sourceChange`) and the field errors.
- It expires when `now − updatedAt ≥ 30 min` (ms precision). It is never listed.
- It is deleted on ×, on save and on expiry. A decode failure deletes it.
- Writes are serialized, and an instance id + revision guard stops a late write from resurrecting a discarded record.
- On recovery:
  - Ready forces a re-check.
  - Calculating re-runs the save with the stored `savedTripId` or the same identity. The server receipt makes the save idempotent.
  - Checking re-runs the check.
- **Guest recovery** is a separate app-level file outside any account (Application Support, excluded from backup), with the same 30-min rules. It never holds the guest secret (`plan.md:2132-2139`).
  - It opens after the Welcome intro on cold start (`GuestRecoveryGate`, 18m).
  - Sign-out deletes the guest record and all guest secrets.

---

## 5. Domain model and request shape

### 5.1 Form → `ApiTripReviewInput`

**[F]** `WizardJourney.swift:4-37`, W `TripJourneyRequest.php`.

The request body is snake_case and journey-shaped:

```json
{
  "outbound": { "legs": [ {
      "from_iata": "AMS", "to_iata": "HND",
      "depart_local": "2026-11-10T11:00", "arrive_local": "2026-11-11T06:00",
      "flight_number": null, "seat_lie_flat": false, "seat_window": null,
      "leave_for_airport_local": null, "airport_transfer_min": null,
      "stay": { "bed_available_mode": null, "bed_available_from_local": null, "check_out_local": null,
                "late_check_out_or_day_room": false, "arrival_transfer_min": null,
                "transfer_mode": null, "busy_intervals": [] } } ] },
  "return": null,
  "trip_end_kind": "one_way", "trip_end_nights": null, "trip_end_check_out_date": null,
  "preparation_days": 3, "fast_delay_opt_in": false,
  "override": { "use": false, "habitual_bed": null, "habitual_wake": null, "chronotype": null },
  "latest_wake": null, "feasible_night_bed": null, "feasible_night_wake": null,
  "pre_departure_busy_answered": false, "pre_departure_busy_intervals": [],
  "draft_token": null, "input_revision": 0, "review_revision": null,
  "profile": { "age_band": "18_64", "shift_work": false, "exclude_caffeine": false, "flight_movement_breaks": false }
}
```

This is verbatim from `iOS/…/Tests/DataAccessTests/Fixtures/wizard/review-request.json`.

- Each leg's `stay` is the stay opened where that leg lands. The form keeps a flat `legs[]`/`stays[]` with `returnBoundary = end.returnFromLeg ?? legs.count`.
- `profile` is sent only when it is non-empty (member). It is restricted to `ProfilePreferences.requestFields`:
  - `habitual_bed`, `habitual_wake`, `free_day_status`, `free_day_bed`, `free_day_wake`, `chronotype`;
  - `age_band`, `shift_work`, `exclude_caffeine`, `flight_movement_breaks`;
  - `caffeine_habit`, `cups_filter_coffee`, `cups_tea`, `cups_cola`, `cups_energy_drink`, `caffeine_sensitive`, `caffeine_non_user_opt_in`;
  - `light_device`, `blue_blockers`, `blue_blocker_mdfd`.
  - Absent means unchanged; `null` clears (P17-D6).
  - **Retired keys get a 422** and must be stripped: `bipolar`, `eye_disease`, `pregnancy`, `vte`, `sleep_phase_disorder`, `oral_contraception`, `caffeine_ruled_out` (W phase-19 P19-D12).
- The identity rule: the first call sends `null/0/null`; later calls echo `identity` from the last response.
- **Fingerprint:** sorted-keys JSON of the input with identity removed, used for "changed?" checks (`TripWizardActions.swift:30-36`). [R] Kotlin: kotlinx.serialization with a canonical sorted encoder; compare byte arrays.

### 5.2 Route editing rules (port verbatim)

**[F]** `WizardForm.swift:62-198`, `WizardRoute.swift`.

- Maximum 16 legs in total (`maxFlights`); `maxPreparationDays` = 4.
- `insertStop(airport, beforeDestinationOf: i)`: leg i's To becomes the stop and its `arriveLocal` is cleared; a new leg stop→old To is inserted with empty times; a new stay is inserted at i; `returnFromLeg += 1` if outbound.
- `removeStop(at: i)` (i>0, not the return boundary): leg i−1 takes leg i's To and `arriveLocal`; leg i and stay i−1 are removed.
- `addReturn`: the first home leg's from = last outbound to; `end.kind = return_legs`, `returnFromLeg = outboundCount`. `removeReturn` keeps the removed legs for a re-add and restores `nights`/`check_out` if present.
- `updateLeg` on the last outbound leg's To also moves the first home leg's From. Any edit clears `itinerary`.

### 5.3 Wall-clock date/time model

**[F]** `WizardDateTime.swift`.

- Values are strings: `YYYY-MM-DDTHH:mm` (airport-local) and `HH:mm` (clock fields). Regexes: `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$` with round-trip check, and `^([01]\d|2[0-3]):[0-5]\d$`.
- Calendars are fixed UTC, so the phone's zone never normalizes a value. A wall time that falls in the *phone's* DST gap stays valid.
- `instant(value, zone)` resolves through the airport's IANA zone and **rejects** wall times that don't exist there (gap).
- **[R] Kotlin:**
  - `LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME pattern "uuuu-MM-dd'T'HH:mm")` (strict).
  - `ZonedDateTime.ofStrict(ldt, offsetFor, zone)` or `zone.rules.isValidOffset(ldt, offset)` to reject gaps. `LocalDateTime.atZone` silently shifts gap times; do not use it for validation.
  - Never use `java.util.Date`/`Calendar` with the device zone.
- **[F] Server instants** look like `"2026-11-07T22:00Z"`, with no seconds (`review.json`). [R] Parse with `OffsetDateTime.parse` (seconds optional) or a lenient `DateTimeFormatter`, and add a unit test for the no-seconds form.

### 5.4 Picker bounds

**[F]** `WizardFlightTiming.swift`.

- Fields are ordered dep0, arr0, dep1, arr1, …
- `minimumLocal(field)` = the immediately preceding field's absolute instant, converted into this field's airport zone. There is no bound when the predecessor is unset, and no upper bound.
- `initialLocal(field)`:
  - the stored value if set;
  - else the nearest earlier set field converted to this zone;
  - else airport-local now, clamped to the minimum.
- Stored values below a new bound are kept, so validation can flag them.

---

## 6. Validation

### 6.1 Client (port of `WizardValidation.validate`, `WizardValidation.swift:5-95`)

| Path | Rule | Message key (en) |
|---|---|---|
| `legs` | 1…16 legs, `stays.count == legs.count` | The planner cannot use this value. |
| `legs.i.fromIata` | required | Choose the airport you leave from. |
| `legs.i.toIata` | required | Choose the airport you land at. |
| `legs.i.toIata` | ≠ from | Choose different departure and arrival airports. |
| `legs.i.departLocal` / `arriveLocal` | required; valid wall date-time | Please fill this in. / Enter a valid date and time. |
| `legs.i.leaveForAirportLocal` | optional; < departLocal | Check the departure and arrival times. |
| `legs.i.airportTransferMin`, `stays.i.arrivalTransferMin` | 0…65535 | The planner cannot use this value. |
| `legs.i.flightNumber` | ≤ 12 chars | The planner cannot use this value. |
| `legs.i.fromIata` (i>0) | = previous leg's To | Start this flight where the previous flight lands. |
| `legs.i.departLocal` (i>0) | instant > previous arrival instant | This flight leaves before the previous one lands. |
| `legs.i.arriveLocal` (Times rules) | arrival instant > departure instant | Check the departure and arrival times. |
| `stays.i.bedAvailableFromLocal` | required when mode = declared | Please fill this in. |
| `*.busyIntervals` | ≤ 32; each start/end required and valid; end > start | Busy time must end after it starts. |
| `end.nights` (kind nights) | 1…60 | The planner cannot use this value. |
| `end.checkOutDate` (kind check_out) | valid date | Enter a valid date and time. |
| `end.returnFromLeg` (return_legs) | 1 ≤ x < legs.count | The planner cannot use this value. |
| `override.habitualBed/Wake` (override on) | HH:mm | Check your bedtime and wake-up time. |
| `preparation.days` | 0…4 | The planner cannot use this value. |
| `preparation.latestWake/feasibleNight*` | HH:mm; bed and wake both or neither | Check your bedtime and wake-up time. |
| `review` (after Checking, no other errors) | last arrival instant > now (one-way member) | A new plan needs an upcoming arrival. Check your flight dates or add your return flight. |
| `review` (guest, or with a return) | same | A new plan needs an upcoming arrival. Check your flight dates. |
| Where (UI) | From = To | From and To are the same airport. Choose where you land. |
| Sleep (UI) | bed/wake/chronotype set; 180 ≤ duration ≤ 840 min | Check your bedtime and wake-up time. |
| Drinks sheet | not "None" and no count > 0 | Add at least one drink to save this answer. |
| Blue sheet | "rated": 0.1…9.9; a comma decimal is accepted | Enter a rating between 0.1 and 9.9. |
| Free-day sheet | declared: both times HH:mm | Check your bedtime and wake-up time. |
| Room sheet | declared: valid date-time (date = landing date + chosen time) | — (Done disabled) |
| Latest-wake sheet | HH:mm | — (Done disabled) |

**[F]** Error-section labels for summaries are in `WizardValidation.errorSection`: "Preparation", "Your sleep for this trip", "Nights at your destination", "End of stay", "Leaving for the airport", "Arrival", "Outbound, flight {{number}}", "Return, flight {{number}}" and "Your flights".

### 6.2 Server

**[F]** W `app/Http/Requests/Api/V1/TripJourneyRequest.php`, `ReviewTripRequest.php`, `GuestReviewTripRequest.php`, `SaveTripRequest.php`, `UpdateTripRequest.php`, `EditReviewTripRequest.php`, `SearchAirportsRequest.php`.

- IATA values are upper-cased and trimmed server-side. They must exist in the `airports` table: "Choose an airport from the search results."
- `outbound.legs` 1…16, `return.legs` 1…15, ≤16 legs in total ("Add no more than 16 flights in total.").
- `trip_end_kind` ∈ nights, check_out, one_way, return_legs:
  - `return_legs` without `return` → `return` "Please fill this in.";
  - `return` without `return_legs` → `trip_end_kind` "Choose one of the available options.".
- `return.legs.0.from_iata` ≠ last outbound `to_iata` → "Your flight home must leave from the airport you landed at." The app moves this to the `review` banner (`WizardJourney.swift:160-169`).
- Date fields use `Y-m-d\TH:i`; clock fields use `H:i`.
- `draft_token` is a uuid ("Your draft has expired. Start your trip again."); `input_revision` ≥0; `review_revision` ≥1.
- **Unknown top-level keys** → that key: "This setting cannot be changed here."
- Save body: exactly `draft_token`, `input_revision` ≥1, `review_revision` ≥1 (+ `source_revision` size 64 for PUT); anything else → "This setting cannot be changed here."
- Guest:
  - `return` and `override` are prohibited;
  - `trip_end_kind` ∈ nights, check_out, one_way;
  - `device_id` is a required uuid (lower-cased);
  - `guest_secret` ≤128;
  - `profile` is required and must include `habitual_bed` and `habitual_wake`.
- Airports: `query` 2…60 chars; `limit` 1…20 (default 8).
- The past rule is the core's: the last landing must not be before the calculation minute. A trip whose outbound has landed but whose return is ahead is accepted. Edits keep the original planning minute (P17-D5).

---

## 7. API

### 7.1 Endpoints

**[F]** W `routes/api.php:59-112`, `docs/ui-plan/phase-17-mobile-api-trip-wizard.md`. Host `api.chronoflow.net/v1`; requests are snake_case, responses camelCase. The exception is `ApiTripInputSnapshot.input`, which stays snake_case so it can be sent back.

| Method & path | Auth | Throttle | Request | Success |
|---|---|---|---|---|
| `GET /v1/airports?query=&limit=` | public (bearer ignored) | `airport-search` 60/min/IP | — | 200 `{airports:[{iata,name,city?,country,timezone}]}`, `Cache-Control: public, max-age=3600` |
| `POST /v1/trips/review` | sanctum + verified | `trip-review` 10/min/user (calculate) | `ApiTripReviewInput` + `profile?` | 200 `ApiTripReview`, `private, no-store` |
| `POST /v1/trips` | sanctum + verified | `api` 60/min + in-action `trip-save` 10/min/user | `{draft_token,input_revision,review_revision}` | **201** `{tripId}`; a retry with the same identity returns the same id (receipt, 7 days, 50/account) |
| `GET /v1/trips/{trip}/input` | sanctum + verified | `api` | — | 200 `{input, sourceRevision, airports:{IATA:ApiAirportSuggestion}, stayEnd?}` |
| `POST /v1/trips/{trip}/review` | sanctum + verified | `trip-review` | review body + `source_revision` (64 chars) | 200 `ApiTripReview` |
| `PUT /v1/trips/{trip}` | sanctum + verified | `api` + `trip-save` | identity + `source_revision` | **200** `{tripId}` |
| `POST /v1/guest/trips/review` | public | `api-guest-review` 5/min per IP+device, 30/min per IP | one-way body + `device_id`, `guest_secret?`, `profile` | 200 `ApiTripReview` (`identity.guestSecret` only on mint) |
| `POST /v1/auth/register` | public | `api-registration` 5/min | + optional `draft_token,input_revision,review_revision,guest_secret` | 200 `ApiSession` with `tripId` |
| `POST /v1/auth/login` (and passkey login) | public | — | + optional `draft_token, guest_secret` | 200 `ApiSession` with `tripId` (receipt replay only) |

`ApiTripReview` (generated, `Kit/AppCore/Contracts/Generated/TripPlan.swift:847`):

```
identity { token, inputRevision, reviewRevision?, sourceRevision?, guestSecret? }
status: ok | modified | needs_answers | no_plan
reasons: [CopyLine]
prompts: [ApiReviewPrompt]
summary?: { line: CopyLine, adaptation, preparationDays, planStartUtc, planStartZone, planEndDate, endCopy:[CopyLine], stayEnd?: {checkOutLocalDate, leaveUtc} }
price?: { state, cost, balance, unlimited, freeDirections:[outbound|return] }
canSave, saveBlockedReason? ("Not enough credits")
itinerary { legs:[{journey, legIndex, departUtc, arriveUtc, flightMin}], layovers:[{journey, afterLegIndex, kind, durationMin, nights}] }
```

`ApiReviewPrompt` (`PresentMobileReview.php`):

```
{ id: "kind:question:stayIndex:legIndex", kind, blocking, required, text: CopyLine,
  scope:{journey?, legIndex?, stayIndex?}, target: profile|trip, fields:[…], answer:{field:value},
  shape:{kind: options|boolean|clock_time|intervals, options:[…], min?, max?}, side?: left|right, deviceMin? }
```

| kind | target | fields | shape |
|---|---|---|---|
| `age_band` | profile | `age_band` | options `under_18, 18_64, 65_plus` — **blocking** |
| `screening` (shift work only) | profile | `shift_work` | boolean — **blocking** |
| `light_device` | profile | `light_device` | options (LightDevice cases) |
| `blue_blockers` | profile | `blue_blockers`, `blue_blocker_mdfd` | options + min 0.1, max 9.9 |
| `cups` | profile | `caffeine_habit`, `cups_filter_coffee`, `cups_tea`, `cups_cola`, `cups_energy_drink` | options `non_user, cups`, min 0, max 20 |
| `free_day_times` | profile | `free_day_status`, `free_day_bed`, `free_day_wake` | options `same_as_workdays, declared` |
| `room` | trip | `stay.bed_available_mode`, `stay.bed_available_from_local` | options `arrival, declared` |
| `late_check_out` | trip | `stay.late_check_out_or_day_room` | boolean |
| `latest_wake` | trip | `latest_wake` | clock_time |
| `pre_departure_busy_hours` | trip | `pre_departure_busy_answered`, `pre_departure_busy_intervals` | intervals |
| `seat_side` | trip | `seat_window` | options `left, right` (+ `side` suggestion) |
| `return_leg` | — | never sent | — |

**[F]** `blocking` is true only on a `needs_answers` review, for age_band/screening, and all missing ones arrive together (age first). A `needs_answers` review carries no optional prompts. Exclusions win: under-18 or recent shift work gives `no_plan` with reasons and no prompts (P17-D7).

### 7.2 Call order

1. **Airport search:** `GET /v1/airports` per debounced query (§9.7).
2. **First review:** `POST /v1/trips/review`, identity `null/0/null`, `profile` with the sleep answers if collected. The server writes the profile **before** calculating (P17-D6). After a profile-bearing review the app merges the profile into its local account snapshot, sets `sleepAnswered`, and requests a sync (13b effects).
3. **On `review_expired`** (422 with `errors.review_expired`, possibly inside `validation_failed`): retry once with the same input and no identity. A second expiry surfaces (as a server failure in Checking).
4. **Blocking answers:** `POST /v1/trips/review` with the echoed identity and `profile.{age_band|shift_work}`. That is one re-review for all answers.
5. **Fine-tune changes / revisits / 30-min expiry:** `POST /v1/trips/review` again (inline).
6. **Save:** `POST /v1/trips` with `{draft_token, input_revision, review_revision}` (all required; `review_revision` ≥1, else a local `reviewExpired`).
   - 201 → persist `savedTripId`.
   - Then pull the created trip (`GET /v1/trips` + `GET /v1/trips/{id}`; errors swallowed) and request a background sync.
   - Then open the trip page with `awaitingSync`.
7. **Edit:** `GET /v1/trips/{id}/input` → for every review: `GET …/input` (refresh) → `POST /v1/trips/{id}/review` (+`source_revision`) → maybe refresh + re-review → `PUT /v1/trips/{id}` (+`source_revision`) → 200.
8. **Guest:** `POST /v1/guest/trips/review` (no bearer, `device_id`, stored `guest_secret` for the current token).
   - A non-null `identity.guestSecret` → store it under the new token and delete the old one.
   - On `review_expired` for the same token, retry once without identity.
   - Then Register with the claim, or login with `draft_token + guest_secret` (`plan.md:2066-2077`).

### 7.3 Errors

**[F]** The envelope comes from W `app/Http/Responses/Api/ApiExceptionResponse.php`:

```json
{ "code": "validation_failed|throttled|server_error|email_unverified|not_found|…",
  "message": "…", "errors": { "field.path": "English key" },
  "args": { "field.path": { "name": "value" } }, "retryAfterSeconds": 42 }
```

- On 429, `retryAfterSeconds` = the `Retry-After` header (default **60** if the header is absent). A body value wins over the header.
- **[F] Typed mapping** (`Kit/DataAccess/TripWizardErrors.swift`, Phase 13a):

| Wire | Typed failure | UI |
|---|---|---|
| 422 `errors.review_expired` | `reviewExpired` | Silent retry once (review); save → re-review on Ready |
| 422 `errors.credits == "Not enough credits"` | `notEnoughCredits` | Save → re-review → no-credits Ready |
| 422 `errors.review == "Try saving again shortly."` | `saveCapacity` | Save-failure banner (server) |
| 422 `errors.profile*` | `profileInvalid(fields)` | Sleep or Fine-tune with field errors |
| 422 other fields | `validation(fields)` with journey→form paths (`return.legs.0.x` → `legs.{outboundCount}.x`, `…stay.x` → `stays.n.x`, top-level snake → form path, camelCased) | Owning step, field lines, banner for `review` |
| any 429 | `throttled(retryAfter)` | Checking "Wait a moment" with countdown; Ready save countdown; airport search waits |
| transport error/timeout | `offline` (no connectivity pre-check) | CheckOffline / SaveOffline |
| 5xx / other | `server` | CheckServer / SaveServer |
| 403 `email_unverified` | `emailUnverified` | Session gate (auth doc) |

- **[F] Timeouts:** review and save requests use a **90 s** HTTP timeout. The review *operation* is capped at **30 s** (service + Checking UI). Saves have no cap and no automatic retry (`plan.md:1590-1592`, `:1604-1611`).

---

## 8. Copy

- **[F] Sources and their precedence:**
  1. **Wizard UI copy:** the iOS catalog `Kit/TripWizardUI/Resources/Localizable.xcstrings`. It has 278 keys, the key is the English text, and every key has Dutch. It was built from WizardCopy → `Wizard.dc.html` `T` → RN `nl.json` → W `lang/nl.json`; "where a board differs from the app's existing Dutch, the app wins" (WizardCopy "Dutch terms").
  2. **Server copy** (question titles, options, exclusion reasons, summary line, validation messages): `CopyLine {key,args}`, rendered through the generated **PlanCopy** catalog (313 keys, `Kit/PlanCore/Resources/PlanCopy.xcstrings`, generated from W `lang/en.json`/`nl.json`). The review is language-free (P17-D9); the client localizes.
  3. **The server's wording wins over the boards for questions.**
     - Age: server "Which age group are you in?" / "In welke leeftijdsgroep zit je?" (W `lang/nl.json:681`) vs board "How old are you?".
     - Shift NL: server "Heb je de afgelopen 14 dagen in nacht- of wisseldiensten gewerkt, of een heel onregelmatig slaapritme gehad?" (W `lang/nl.json:276`) vs board "…in de afgelopen…had je…".
     - The "why" line under each question is board copy: "Light and sleep advice differs by age." / "Shift work changes how we can plan your clock."
- **[F] Key board strings** are listed by board key in `C/Wizard.dc.html:228` (246 keys, all with nl) and `C/WizardSheet.dc.html:87`. The full extraction is reproduced in the appendix (§A).
- **[F] Durations** (WizardCopy):
  - formats: "3 h", "45 min", "2 h 40 min"; nl "3 u", "45 min", "2 u 40 min";
  - spoken: "2 hours 40 minutes" / "2 uur en 40 minuten";
  - "Flight time {d}" / "Vliegtijd {d}".
- **[F] Dutch terms:** luchthaven (not vliegveld), lichtbox (not lichtbak), credit/credits, "Tijd in {city}" (not "Amsterdamse tijd"), "Lokale tijd" when the city is unknown.
- **[R] Android strings.** Generate `values/strings_wizard.xml` and `values-nl/strings_wizard.xml` from the iOS `.xcstrings` with a small script, checked in with its output:
  - Derive stable resource names (`wizard_` + slug, with a committed mapping file for collisions).
  - Convert `{{name}}` to positional `%1$s` (record argument order in the mapping).
  - Map plural variations to `<plurals>`.
  - Fail on missing nl.
  - Do the same for PlanCopy (shared with the plan/timeline docs).
  - Keep the iOS English key in an XML comment for traceability.

---

## 9. Android approach

### 9.1 Module and state holder

**[R]**

- `:feature:tripwizard` with three packages:
  - **`model`** (pure Kotlin, no Android imports; JVM unit tests): `WizardForm`, `WizardRoute`, `WizardDateTime` (java.time), `WizardFlightTiming`, `WizardValidation`, `WizardFlightRules`, `WizardJourney`, `WizardPresentation`, `WizardOptionalAnswer`, `WizardCredits`, `WizardChecking` (driven by an injected clock: `TimeSource.Monotonic` + `delay`, testable with `runTest`/`TestCoroutineScheduler`), and the reducer `WizardMachine`. The reducer ports `MemberWizardModel` + extensions as functions `(Draft, Event) -> (Draft, List<Effect>)`, or as a class with the same method names, for easier diffing against iOS.
  - **`generation`**: a port of `TripGenerationService`. Use a `Mutex`-guarded in-flight map keyed by `(instance, fingerprint, token, reviewRevision, edit)`; the 30 s cap via `withTimeout`; expiry retry; edit source refresh; save receipts; effects interface (merge profile, pull created trip, request sync).
  - **`ui`**: Compose screens and sheets.
- **`TripWizardViewModel`:**
  - It holds `MutableStateFlow<WizardDraft>` and exposes `StateFlow<WizardUiState>`, derived with `map`/`stateIn` and `@Immutable` UI models so Compose skips recomposition.
  - Every mutation runs on `Dispatchers.Main.immediate` (matching iOS `@MainActor` semantics and its guards: `closed`, `persistenceFailed`, `choicePending`, `reviewing`, check-id/attempt guards that drop late results).
  - Side effects run as `viewModelScope` jobs that can be cancelled: `checkJob`, `reviewJob`, `saveJob`, `choiceJob` (250 ms), `retryCountdownJob`.
  - The save job must survive navigation within the wizard but not process death. That is fine: recovery re-runs it.
- **Persistence:** after each mutation, `persist()` enqueues the draft into a single-consumer `Channel(CONFLATED)` writer coroutine that upserts the recovery row (Room, `drafts` table, id `member-wizard`, JSON via kotlinx.serialization, `updatedAt` epoch ms, instance + revision compare-and-set).
  - It must preserve the iOS ordering guarantee: a discard flushes pending writes, then deletes.
  - Before navigating away after a save, `savedTripId` is written **synchronously** (awaited) before the pull, mirroring `didCreate`.

### 9.2 Navigation

**[F]** WizardSpec: the wizard is a full-screen modal over the tabs; back from step 2; swipe-back.

**[R]**

- **App level:** one destination `TripWizardRoute(mode: create|edit|duplicate|guest, tripId: String?)` in the app's NavHost, shown full screen with the bottom bar hidden.
  - Enter animation: slide up (modal); exit: slide down.
  - When a recovery row exists at app start, open this destination automatically: member, after the shell is up (iOS AppTabs recovery); guest, after the Welcome intro (18m).
- **Inside the wizard, the step stack is derived from state, not imperatively pushed.**
  - Port `navigationPath` (§4.2) and render it with **Navigation 3** (`androidx.navigation3` `NavDisplay`, whose back stack is an app-owned list). Feed it `navigationPath` and route `onBack` to the model's `popNavigation`/back targets. This gives forward/back slide transitions and **predictive back** animations for free, and the stack can never disagree with the model. That disagreement caused the iOS "stale pop" bugs, round 3 (`plan.md:1825`).
  - If Navigation 3 is not adopted app-wide, use `AnimatedContent(targetState = step)` with direction computed from path length, plus `PredictiveBackHandler`.
  - Do not use a nested Navigation 2 NavHost per step: its saved back stack would duplicate the recovery state and drift.
- **Header:** a M3 `CenterAlignedTopAppBar`:
  - centred title "New trip"/"Edit trip" (15 sp, muted, weight 500 per board);
  - left: back `IconButton` (when the path is non-empty and the step allows it);
  - right: × `IconButton` (except during Calculating).
  - Below it, the 4 dp `LinearProgressIndicator(progress = { pct })` with 20 dp side insets, colour `dawn`, track `muted`, and stop indicator/gap removed to match the bar (WizardCopy "Android": "Progress: the native determinate linear progress indicator, styled like the 4 pt bar").
  - Animate progress changes with `animateFloatAsState`; never downward.
- **Footer:** a `Column` pinned to the bottom with `Modifier.navigationBarsPadding().imePadding()`. It holds the optional secondary text button (Close) and the 52 dp primary button (`Button`, full width, pill shape). The content above is a `verticalScroll` column (§9.9).

### 9.3 Process death and state survival

**[R]**

- **Source of truth = the recovery row**, not `SavedStateHandle`. It survives process death, a swipe-away and a reboot, which `SavedStateHandle` does not cover. `SavedStateHandle` holds only:
  - `instanceId` (to re-attach to the same row and reject a newer instance);
  - the entry args (`mode`, `tripId`, `action`);
  - transient UI-only state: open picker dialog ids, the in-progress text of the airport search query, the scroll anchor.
  - Sheet content edits (pending values before Done) use `rememberSaveable` inside the sheet, keyed by prompt id.
- **On ViewModel init:** load the row.
  - Missing or expired (≥30 min since `updatedAt`) → for a recovery launch, close quietly; for a fresh entry, `start()`.
  - Present → restore the step, open sheet (`sheet`, `promptSheetID`) and values, then `resumeCompletion()`: Calculating → re-save; Ready → forced re-check; Checking → re-run.
- **Account scoping:** the row lives in the signed-in account's DB, and sign-out deletes it.
- **Guest state:** a JSON file in `noBackupFilesDir` (not in any account DB). The guest secret goes in a separate encrypted store: an Android Keystore AES-GCM key + DataStore, one entry per draft token, deleted on discard, claim, sign-out and teardown. Never write the secret into the draft file or logs.
- **Configuration changes** (rotation, fold, dark mode) need nothing extra: the ViewModel survives. Locale changes recompose from resources. The design keeps phones portrait (WizardCopy: "Android tablets stay portrait for now").

### 9.4 Back handling and discard

**[R]** Use one `BackHandler`/`PredictiveBackHandler` at the wizard root, enabled whenever no sheet or dialog is showing (sheets and dialogs consume back themselves). Behaviour by state:

| State | System back / predictive back | × |
|---|---|---|
| A sheet is open | Dismiss the sheet = Cancel (no write) | — |
| The discard dialog is open | Dismiss = Keep editing | — |
| First screen (Where; Duplicate Times per OQ 2; Edit Ready) | Same as × | If `hasChanges` (create/guest: `entered`; edit: fingerprint ≠ original) → discard dialog, else close |
| Steps 2+ | Pop to the previous path element (question i → i−1; question 0 → last input step) | as above |
| Checking (running) | Cancel the check (+ server-side dedupe cancel) → last input step | as above |
| Checking failure | Back to the last input step (create); none on edit failure (board) | Close (discard rules) |
| Calculating | **Consumed, does nothing** | hidden |
| Excluded | Back to the last input step | Close at once (excluded is a dead end) |

- **Discard dialog:** M3 `AlertDialog`.
  - Create: title "Discard this trip?", text "What you entered is not saved.".
  - Guest adds "You go back to the welcome screen.".
  - Edit: "Discard your changes?" / "Your trip stays as it was.".
  - `dismissButton` = "Keep editing" (`TextButton`); `confirmButton` = "Discard" / "Discard changes" (`TextButton` with `colorScheme.error` content colour).
  - Confirming: `discard()` (cancel jobs, flush, delete the row) → navigate out (create → My trips; edit → trip page; guest → Welcome).
- The board's Close sits on the right on Android too (Rick's rule) even though Material puts navigation-close on the left. Use an `actions` slot × with content description "Close"/"Sluiten".

### 9.5 Date and time pickers

**[F]**

- `C/Wizard.dc.html` draws **separate date and time pills** per Departs/Arrives row ("12 Oct 2026" + "14:40"; empty "Date"/"Time" in muted text; error = 2 dp inset destructive ring + destructive text on the date pill).
- **WizardCopy "Android":** "Date and time pills have no compact form: a tap opens the Material date or time dialog." and "Pickers follow the app's language and the phone's 12/24-hour setting."
- PickerDateTime (iOS-shaped) shows the airport-local context: "Local time at Amsterdam Schiphol (AMS)" and a summary "Mon 12 Oct · 14:40 Amsterdam time".
- iOS settled a single combined pill + one sheet (OQ 46), but that answered a SwiftUI limitation (no empty `DatePicker`) that Android does not have.

**[R] Times screen**

- **Date pill** → `DatePickerDialog` + `DatePicker(state)`:
  - `rememberDatePickerState(initialSelectedDateMillis = storedDate?.atStartOfDay(UTC)?.toEpochMilli(), initialDisplayedMonthMillis = initialLocal(field).toLocalDate()…, selectableDates = object : SelectableDates { isSelectableDate(utcMillis) = date >= minimumLocal(field)?.toLocalDate() })`.
  - **All millis are UTC midnight of the airport wall date**, so the device zone never touches the value (the Material DatePicker works in UTC).
  - Title slot: "Flight 1 departs" style text (app copy: Departure date / Arrival date). Headline: the formatted wall date. Supporting line: "Time in {city}".
  - Confirm "Done"/"Klaar"; dismiss "Cancel"/"Annuleren".
- **Time pill** → a `TimePickerDialog`-style M3 dialog with `TimePicker(rememberTimePickerState(initialHour, initialMinute, is24Hour = DateFormat.is24HourFormat(context)))` and a toggle to `TimeInput` (Material's keyboard mode). Same title and supporting line.
- **Storage:** add `departDate`/`departTime`/`arriveDate`/`arriveTime` to the Android leg model (or a `pending` map) and derive `departLocal = "$date T$time"` only when both are set. iOS stores only complete strings because its pill always writes both. Validation treats a half-filled pair as "Please fill this in." The persisted JSON keeps both halves so recovery restores a half-entered row.
- **Only Done writes** (Cancel/back leaves the value). The write triggers a selection haptic and clears the field's error.
- Minimum bounds apply to the date only. Time-of-day bounds cannot be set on `TimePicker`, so the existing validation lines catch them (as iOS preserves values below bounds).
- **Accessibility labels:**
  - date: "Departure date, Monday 12 October 2026" / "Arrival date, not set";
  - time: "Departure time, 14:40" / "…not set" (WizardCopy "Accessibility").

**Other pickers**

- **Sleep step, Fine-tune trip sleep, free-day times, room "From", latest wake (sheet):** time pills → the same M3 time dialog. PickerTime draws 5-minute steps; M3 `TimePicker` has no step option. See OQ 5 (default: minute precision).
- **Latest-wake sheet (WizardSheetWake)** draws an inline wheel. **[R]** Embed an M3 `TimePicker` (`TimePickerLayoutType.Vertical`) inside the sheet, or `TimeInput` when height is short or the font scale is ≥ 1.5 (OQ 6).
- **Busy hours:** each range is two date-time picks. **[R]** Date dialog → time dialog for "From", then the same for "To", with "Busy time must end after it starts." validation. The bound is the first outbound departure (iOS opens at the existing start or `legs[0].departLocal`).
- **Room "From a time":** the stored value is a date-time; the date = the landing leg's `arriveLocal` date + the chosen time (`WizardAnswerSheet.swift:188-200`).
- **Display formatting:** pills show the date as `d MMM yyyy` and the time as `HH:mm`, or `h:mm a` when 12-hour. Format with `DateTimeFormatter.ofLocalizedDate`/pattern in the app locale with `ZoneOffset.UTC` for wall values.

**12/24-hour.** The app also has a stored `time_format` preference used for plan copy (Phase 14a). WizardCopy says pickers follow the phone setting. **[R]** The picker dialog follows the phone. Pill and row *display* follows the app preference, as iOS does (`WizardDateTime.formatWeekdayTime(twelveHour:)`). See OQ 7.

### 9.6 Bottom sheets

**[F]** Board: sheets with a grabber, 38 px top radius, Cancel left / title centre (17/22 600) / Done right (dawn-text 600), then the question (17/24 600), caption, controls, and an error line. Airport/stop search sheets are full height (top 56 px).

**[R]** Use M3 `ModalBottomSheet`:

- `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`; system drag handle; `containerColor = card`; content `Modifier.navigationBarsPadding().imePadding()`.
- Top row: Cancel / title / Done as `TextButton`s. Done is disabled while the sheet has a validation error (drinks, rating, free-day times, room/time missing).
- **Answer sheets:** a radio list card for options (`Modifier.selectableGroup()`, rows `selectable(role = Role.RadioButton)`, 52 dp min); drawn steppers for drinks (− / count / +, 48 dp targets); a rating `OutlinedTextField(keyboardType = Decimal)` accepting "," and "."; time rows (pill → dialog).
- **Seat sheet:** "Aisle or not sure" writes `seat_window = null` and the row then shows "Add".
- Sheets are driven by state (`draft.state.sheet` / `promptSheetID`), so recovery reopens them. `onDismissRequest` = Cancel.
- **Airport/stop search:** a full-height `ModalBottomSheet` (`skipPartiallyExpanded`).
  - Header: Cancel + title.
  - For stops, a caption "Between {from} and {to}" (the two airports the stop splits; for a replaced stop, the next leg's To).
  - A search field (`SearchBarDefaults.InputField` or `OutlinedTextField`, leading search icon, placeholder "Airport or city" / "Luchthaven of stad", `imeAction = Search`, autofocus with `FocusRequester` + keyboard shown on open).
  - Results in a bordered card (`LazyColumn`, 60 dp rows: bold IATA in a 44 dp column, name, "City, Country").
  - Selecting: light haptic for stops; set the airport; close.
  - Alternative: `SearchBar` expanded full screen. Default: the sheet, to match the board and iOS.
- **The first-trip reminders sheet** is a `ModalBottomSheet` over the trip page, owned by the reminders feature. The wizard only queues the offer at save (§4.3).

### 9.7 Airport search: debounce, pacing, states

**[F]**

- iOS `AirportSearchService`: trims the query; length 2…60 else `[]`; **350 ms** debounce; stale searches cancelled; requests spaced **≥1.02 s** (one shared actor across sheets and opens, keeping below the 60/min server limit); a 429 pushes the next allowed time by `retryAfter`; `limit=8`.
- RN used a 250 ms debounce (`RN/src/trips/useAirportSearch.ts:23-36`).
- Results are filtered by `airportIsAllowed` (no adjacent duplicates).

**[R]** One `AirportSearchRepository` singleton (app scope), with a `Mutex` + `nextAllowedAt` for pacing. In the sheet:

```kotlin
snapshotFlow { query.trim() }
  .distinctUntilChanged()
  .debounce(350)
  .mapLatest { q -> if (q.length !in 2..60) SearchState.Hint else { emit(Searching); repo.search(q) } }  // cancels stale
```

- Collect in `LaunchedEffect`, or better in the ViewModel with `flatMapLatest`.
- Enable the **OkHttp disk cache**, since the server sends `public, max-age=3600`, plus a small in-memory LRU (query → results). That makes repeat queries free and offline-tolerant.
- **Draw the TripSearchStates states inside the sheet** (OQ 8):

| State | Content |
|---|---|
| < 2 chars | supporting text "Type at least 2 letters to search." |
| searching | "Searching airports…" + spinner + 3 skeleton rows |
| empty | dashed card "No airports found" + hint |
| error | "We couldn't search airports. Check your connection and try again." + outline "Try again" |
| throttled | the same error card, with Try again disabled until `retryAfter` |

- No connectivity pre-check.

### 9.8 Checking screen, accessibility, haptics

**[R]**

- **Arc:** `Canvas` dashed quadratic path "AMS ⌒ NRT" (route endpoints in Fraunces 26/32), with an animated dot along the path using `rememberInfiniteTransition`.
  - Reduce Motion: check `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (exposed as a `LocalReduceMotion`). Then the dot is static, ticks don't scale, and the spinner becomes a static dot. Timing is unchanged (it's model-driven).
- **Tick lines:** 24 dp circle, dawn background with a check when done, muted when open.
  - Each line has `Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Flights and time zones, done" }`.
  - Do **not** use `View.announceForAccessibility` (deprecated in API 36).
  - The title gets `semantics { heading() }` and focus on arrival via `FocusRequester` (WizardCopy: "Focus goes to the title on each new step. When a trip-wide message arrives, focus goes to it first. After Continue with errors, focus goes to the first error.").
- **Banner:** `Modifier.semantics { liveRegion = Assertive }`.
  - Error tone: destructive 10 % over card, border destructive 35 %.
  - Info tone: dawn 12 % / 40 %. Corner radius 18; 14/20 text.
- **Haptics** (`LocalHapticFeedback`, Compose ≥1.8 `HapticFeedbackType`) and/or `View.performHapticFeedback`:

| Spec | Android |
|---|---|
| Selection | `SegmentTick` (fallback `TextHandleMove`) |
| Light impact | `ContextClick` (fallback `VirtualKey`) |
| Success | `Confirm` |
| Warning | `Reject` |
| Error | `Reject` |
| Medium impact | `LongPress` |

  The mapping is OQ 10.
- **Auto-advance choices** (Coming back?, questions): the tap marks the card selected (2 dp dawn border, `role=RadioButton`), sets `choicePending` (all taps ignored), and after 250 ms the model advances.
- **Double-tap guard on buttons:** disable on the first tap until the step changes or the response arrives (the iOS `continuing` flag).

### 9.9 Layout rules

**[F] WizardCopy "Layout"**

- Step content scrolls; header, progress and the main button stay fixed.
- At 390×844 nothing scrolls at the default text size; questions fit up to the largest standard size. At 375×667, Fine-tune and Ready scroll.
- Column at most 520 pt wide, centred from 669 pt wide.
- Date/time pills are inline up to xxxLarge; from AX1 they wrap under the label, side by side, full width.
- Ready label column 92 pt up to xLarge; above that the label sits above the value.
- Sleep 2×2 grid → one column at AX1+ and under 360 pt wide.

**[R]**

- `Modifier.widthIn(max = 520.dp)` centred.
- Use `fontScale` thresholds as the Dynamic Type stand-in: ≥1.3 ≈ xLarge (Ready labels stack); ≥1.8 ≈ AX1 (pills wrap, sleep grid single column). Measure with `LocalDensity.current.fontScale`.
- `FlowRow` for the pills.
- Times with 3+ flights: the content `verticalScroll(state)`. After a failed Continue, `scrollState.animateScrollTo(firstErrorY)` with `Modifier.onGloballyPositioned` / `BringIntoViewRequester` on the first error.
- **Sizes from the board, as dp:**

| Element | Size |
|---|---|
| Where rows | min 68, card radius 16 |
| Swap button | 40 |
| Segmented control | 44 (use M3 `SingleChoiceSegmentedButtonRow`, tinted dawn: "on Android it takes our colours") |
| Time card rows | min 64 |
| Coming back? cards | min 76, radius 22 |
| Chronotype cards | min 76, radius 18 |
| Question options | 64 (72 large), radius 20 |
| Fine-tune rows | 52 |
| Ready rows | 48 |
| Primary button | 52, pill |
| Secondary button | 44 |
| Hero/Checking/Calculating cards | radius 26 |
| Title | Fraunces 30/36 (24/30 when the title is > 80 chars) |
| Sub | 16/22 muted |
| Eyebrow | 12/16, 600, 0.1em tracking, uppercase, dawn-text |

  Tokens come from `ds/chronoflow/tokens.json` (other docs).
- **Preparation days:** drawn − and + buttons, **48 dp** each, no native stepper (WizardCopy "Android"). Value 0–4; selection haptic per step.
- **Switch:** M3 `Switch` with the dawn/amber track.

### 9.10 Performance notes

**[R]** The wizard is not performance-critical (the timeline is), but:

- Keep `WizardUiState` slices immutable and stable. Split per-screen state so a Checking tick does not recompose other steps.
- Compute derived values (navigation path, validation, summaries) in the ViewModel with `distinctUntilChanged`.
- Format dates with cached `DateTimeFormatter`s per locale.
- Persist off the main thread (Room on `Dispatchers.IO`); never block the UI on the recovery write, except the `savedTripId` write before navigation.
- Make airport results `LazyColumn` items with stable keys (`iata`).
- Use a baseline profile that covers wizard entry → Times → date dialog. The M3 picker classes are large; warming them avoids first-open jank.
- Let the trip page's `awaitingSync` path render immediately after the save; the pull runs in the background.

### 9.11 Tests

**[R]**

- Port the iOS host suites (`Tests/TripWizardTests`: WizardRouteTests, WizardFlightTimingTests, WizardPortTests, WizardCheckingTests, WizardReviewFlowTests, WizardCompletionTests, WizardSaveRecoveryTests, WizardRecoveryTests, WizardRetryTests, WizardCreditsTests, WizardHomeRouteTests, TripWizardActionsTests, Guest*Tests, TripGeneration*Tests…) as JVM JUnit5/kotlinx-coroutines-test tests over the `model` and `generation` packages.
- Copy the JSON fixtures in `Tests/DataAccessTests/Fixtures/wizard/` (review, save, expired, credits, capacity, throttled, unknown-field, profile, snapshot, journey, airports, guest-review-request) into `src/test/resources`. Use them for encoder/decoder and error-mapping tests against MockWebServer, asserting exact bodies.
- Compose UI tests: one per board state, via a debug seed that builds a `WizardDraft` (the iOS `CF_WIZARD_BOARD_CAPTURE` equivalent). Add Roborazzi/Paparazzi screenshots in light/dark × en/nl if the screenshot strategy in the other docs allows it.
- Add one end-to-end journey against the mock data source: Where → stop → Times → One way → Checking → Ready → save → trip page.

---

## 10. Guest specifics (summary; claim details belong to the auth doc)

**[F]** `iOS/plan.md:2062-2210`.

- **Path:** Where → Times → Sleep (always) → Checking → [Age, Shift work] → Fine-tune (always shown) → Ready → GuestSignup sheet. There is no Coming back?, flight home or Return row, and no Fine-tune sleep switch (the override is prohibited server-side; the guest sleep *becomes* the override, W `GuestReviewTripRequest::flatInput`).
- **Ready rows:** Route, Flight, Sleep "times · chronotype", Questions ("N answered" / "None needed"), Extras.
- **"Make my plan":**
  - medium haptic;
  - gate: token present, no failure, fingerprint unchanged, check under 30 min; otherwise re-run Checking;
  - then present GuestSignup.
- **Claim outcomes:**
  - `tripId` set → open the trip after the verify, terms and walkthrough gates.
  - `tripId == null` after a password or passkey login → seed the member wizard at Checking (§4.1).
  - `tripId == null` after two-factor → My trips.
- **Exits** (×, excluded Close, failure Close) go to Welcome. × on an empty Where closes at once.
- **"Change my answers"** resets only `age_band`/`shift_work`.
- **Guest past-arrival copy:** the server rewrites the member text into the guest variant (W `ReviewGuestTrip.php:70-76`).
- **Guest secret:** 64 hex chars, minted on the first review and returned once. Store it in the Keystore-backed store, rotate it on a new token, and delete it on discard (including a discard mid-flight: delete the new secret and drop the result).

---

## 11. Findings: discrepancies between sources

1. **Guest progress weights** (iOS vs boards): iOS shifts them one step (§4.6). Default: boards (OQ 1). Report this to the iOS session as a likely bug.
2. **Back on Duplicate's first screen and on edit failure:** the board hides it; iOS shows it (§3.4). OQ 2.
3. **No credits:** the board's button reads "Buy credits on the website" → SetPaywall ("Continue to the website", one-time sign-in link). iOS OQ 41 ships Close only, because the backend has no hand-off endpoint. `routes/api.php` still has none (checked). OQ 3.
4. **Date/time picking:** iOS uses a combined pill + sheet (OQ 46); the Android rule in WizardCopy says separate pills with Material dialogs, and the Wizard board draws separate pills. Default: the WizardCopy Android rule (§9.5, OQ 4).
5. **Question wording:** the board's "How old are you?" loses to the server's "Which age group are you in?". Shift-work Dutch differs between the board and the server; the server wins (§8).
6. **Preferences toggles** (exclude caffeine, movement breaks) exist on iOS Fine-tune without a board (an explicit design exception). Android mirrors iOS.
7. **The airport search failure/empty states** are drawn (TripSearchStates) but iOS did not build them ("Airport search failures leave the native search editable", `Docs/wizard-16a.md`). OQ 8.
8. **The 16-flight limit state** has no wrapper board (`atLimit` is always false in `Wizard.dc.html:306`). The copy exists ("That's the most we can plan: 16 flights."). Build it from the copy.
9. **The Overnight board's sample times** (23:25 → 02:05) contain no complete 23:00–07:00 night under the server rule. The app computes nights from facts and does not copy the illustration (`Docs/wizard-16a.md`).
10. **WizardCheckPractice** has no feature behind it. Not built.

---

## 12. Open questions (with defaults; the plan proceeds on the default)

| # | Question | Default |
|---|---|---|
| OQ 1 | Progress values: iOS weights or board `pct`? | **Board `pct` values** (member: 8/15, 22/30, 45, 50, 55, sleep 50, checking 60, again 70, question interpolate 60→70, fine-tune 80, ready 95, calc 100; guest: 9, 18, 30, 42, 54, 64, 86, 95), with the never-backwards floor |
| OQ 2 | Back on Duplicate's Times and on edit Checking failures: board (none) or iOS (shown)? | **Board**: no back chevron and no system-back pop on these first screens; system back acts as ×. Ready's Route row still reaches Where |
| OQ 3 | No-credits Ready action | **Match iOS (OQ 41)**: Close only, red "You have 0 credits", silent re-check on foreground. Revisit when a billing hand-off endpoint exists (then: "Buy credits on the website" → SetPaywall) |
| OQ 4 | One combined date-time control (iOS) or separate Material date/time dialogs (WizardCopy Android)? | **Separate pills → Material `DatePickerDialog` / time dialog**, wall values in UTC millis, Done-only writes, date lower bound from `minimumLocal` |
| OQ 5 | PickerTime draws 5-minute steps; Material `TimePicker` has none | **Minute precision** (the server accepts any `H:i`); no snapping |
| OQ 6 | The latest-wake sheet draws an inline wheel | **Inline M3 `TimePicker`** in the sheet; `TimeInput` at fontScale ≥ 1.5 or short height |
| OQ 7 | 12/24-hour for pickers vs the app's `time_format` preference | **Picker dialog = phone setting** (WizardCopy); **displayed values = app preference** (as iOS) |
| OQ 8 | Build the TripSearchStates hint/searching/empty/error states in the airport sheet? | **Yes**, inside the sheet, with the drawn copy (en + nl translated per the iOS OQ 28 order: RN nl.json → W lang/nl.json → translation) |
| OQ 9 | Edit/Duplicate when `GET /v1/trips/{id}/input` fails (offline/404) | Do not open the wizard. Keep the trip-actions row disabled offline ("Needs a connection."). On failure show the app's standard error snackbar/banner. No EditorStates screen (superseded) |
| OQ 10 | Haptic mapping | As in §9.8; behind a `WizardHaptics` interface so it can be tuned on a device |
| OQ 11 | Navigation 3 vs AnimatedContent for internal steps | **Navigation 3 `NavDisplay`** fed by the derived path, if the app adopts Navigation 3; otherwise `AnimatedContent` + `PredictiveBackHandler`. Never a nested Nav2 NavHost |
| OQ 12 | Where does the guest secret live? | Android Keystore AES-GCM key + DataStore, one entry per token; never in backups (`android:fullBackupContent`/`dataExtractionRules` exclude it) |
| OQ 13 | Minute/hour formats in pills: board "12 Oct 2026" (date pill with year) vs Ready "Mon 12 Oct 14:40" | Date pill `d MMM yyyy`; Ready and summary as WizardCopy "Ready values" (`EEE d MMM HH:mm`; year added across a year end) |
| OQ 14 | Room "From" when the landing leg has no arrival date yet | Disable the time row until the arrival date exists (it cannot occur after Checking, because prompts come from a valid review) |

---

## 13. Risks

- **Material DatePicker zone handling:** feeding device-zone millis would shift wall dates by a day east or west of UTC. Mitigation: a single `WallDate ↔ utcMillis` adapter with unit tests in UTC+14 and UTC−12, and a device-zone change test (as iOS 16a tested DST).
- **Split date/time storage diverges from iOS's complete-string model.** Mitigation: derive `departLocal` in one place. The request builder and validation only see complete strings plus "missing" errors.
- **Recovery write ordering:** a late write after discard resurrects the row. Mitigation: instance + revision compare-and-set in the DAO (as iOS), and tests for discard-during-write.
- **The save is idempotent only with the same identity.** Mitigation: persist `savedTripId` before the pull and never re-review between a lost response and a retry. A recovered Calculating re-sends the same identity.
- **Back-stack drift** if the steps use imperative navigation. Mitigation: a derived stack (§9.2).
- **Throttle budgets:** member review is 10/min; Fine-tune toggling plus revisits can hit it. Mitigation: re-review only on a changed fingerprint (as iOS), dedupe in-flight reviews, and show the countdown UI.
- **Server contract drift:** W is on `feat/mobile-api-passkey-management`; the wizard endpoints come from the trip-wizard branch (iOS OQ 37: live acceptance on local Valet). Mitigation: contract tests from the fixtures, and regenerate DTOs from W `resources/js/types/App/Data/Api/index.ts`.
- **No-health-data:** sending any retired profile key gives a 422. Mitigation: a shared `RetiredProfileKeys` strip on encode and on recovery decode (iOS NH2.1).

---

## Appendix A — board copy tables (en || nl)

### A.1 `Wizard.dc.html` `T` (selected; the full table has 246 keys, all with nl)

| Key | en | nl |
|---|---|---|
| newTrip / editTrip | New trip / Edit trip | Nieuwe reis / Reis bewerken |
| cont / close / back | Continue / Close / Back | Doorgaan / Sluiten / Terug |
| progressA11y | Progress, {pct} percent | Voortgang, {pct} procent |
| whereTitle / whereSub | Where are you flying? / Pick the airports from your booking. | Waar vlieg je heen? / Kies de luchthavens uit je boeking. |
| from / to / choose | From / To / Choose an airport | Van / Naar / Kies een luchthaven |
| fromHint | From your last trip | Van je vorige reis |
| sameErr | From and To are the same airport. Choose where you land. | Van en Naar zijn dezelfde luchthaven. Kies waar je landt. |
| swap | Swap airports | Luchthavens omwisselen |
| askStops / direct / withStop | Do you change planes on the way? / No, direct / Yes | Stap je onderweg over? / Nee, direct / Ja |
| stopLabel / stopN | Stop / Stop {n} | Tussenstop / Tussenstop {n} |
| addAnotherStop / removeStop | Add another stop / Remove stop in {city} | Nog een tussenstop toevoegen / Tussenstop in {city} verwijderen |
| maxFlights | That's the most we can plan: 16 flights. | Meer kunnen we niet plannen: 16 vluchten. |
| outTitle / outTitleMulti / homeTimesTitle | When is your flight? / When are your flights? / When do you fly home? | Wanneer vlieg je? / Wanneer vlieg je? / Wanneer vlieg je naar huis? |
| outSub | Times are local at each airport. | Tijden zijn lokaal op elke luchthaven. |
| dupSub | Copied from Amsterdam → Tokyo. Choose the new dates. | Gekopieerd van Amsterdam → Tokio. Kies de nieuwe data. |
| departs / arrives / timeIn / localTime | Departs / Arrives / Time in {city} / Local time | Vertrek / Aankomst / Tijd in {city} / Lokale tijd |
| date / time / notSet | Date / Time / not set | Datum / Tijd / niet ingesteld |
| depDateA11y / arrDateA11y | Departure date / Arrival date | Vertrekdatum / Aankomstdatum |
| routeA11y | {from} to {to} | {from} naar {to} |
| flightTime | Flight time {d} | Vliegtijd {d} |
| stopNew / connection | Stop in {airport} / Connection in {airport} · {duration} | Tussenstop in {airport} / Overstap in {airport} · {duration} |
| overnight1 / overnightN | Stop in {airport} · {count} complete night(s) | Tussenstop in {airport} · {count} volledige nacht(en) |
| orderErr / seqErr | Check the departure and arrival times. / This flight leaves before the previous one lands. | Controleer de vertrek- en aankomsttijden. / Deze vlucht vertrekt voordat de vorige landt. |
| pastMember | A new plan needs an upcoming arrival. Check your flight dates or add your return flight. | Voor een nieuw plan is een aankomst nodig die nog moet plaatsvinden. Controleer je vluchtdata of voeg je terugvlucht toe. |
| pastGuest | A new plan needs an upcoming arrival. Check your flight dates. | … Controleer je vluchtdata. |
| retFlightTitle / homeRouteSub | Your flight home / It leaves from where you landed. Add any stops on the way. | Je vlucht naar huis / Hij vertrekt van de luchthaven waar je landde. Voeg eventuele tussenstops toe. |
| whereLanded / homeA11y | Where you landed / Leaves from Tokyo, where you landed. You can't change this. | Waar je landde / Vertrekt vanuit Tokio, waar je landde. Dit kun je niet wijzigen. |
| retTitle / oneWay / oneWaySub | Coming back? / One way / Just this flight | Kom je terug? / Enkele reis / Alleen deze vlucht |
| addReturn / addReturnSub / addReturnSubEdit | Add my return flight / We plan the shift back home too / … · 1 credit | Terugvlucht toevoegen / We plannen ook de verschuiving terug naar huis / … · 1 credit |
| webTitle / webSub | Ends after 9 nights / Set on the website. Keep it, or add your flight home. | Eindigt na 9 nachten / Ingesteld op de website. Laat het zo, of voeg je vlucht naar huis toe. |
| tapHint | Tap an answer to continue. | Tik op een antwoord om door te gaan. |
| sleepTitle / sleepSub / sleepSubGuest | How do you usually sleep? / On a work day. We save it for next time. / On a normal work day. | Hoe slaap je meestal? / Op een werkdag. We bewaren het voor de volgende keer. / Op een normale werkdag. |
| bed / wake / awake | Usual bedtime / Usual wake-up / When do you naturally feel most awake? | Meestal naar bed / Meestal wakker / Wanneer voel je je van nature het fitst? |
| early / between / owl / unsure (+hints) | Early bird "Up early, tired in the evening" / In between "A normal working day" / Night owl "Late nights, slow mornings" / Not sure "We use your sleep times" | Ochtendmens / Ertussenin / Avondmens / Weet ik niet (+ hints) |
| checkTitle / againTitle / editCheckTitle / checkSub | Checking your trip / Checking your answers / Checking your changes / This takes a few seconds. | We bekijken je reis / … je antwoorden / … je wijzigingen / Dit duurt een paar seconden. |
| c1 / c2 / c2guest / c3 | Flights and time zones / Your saved answers / Your answers / What we can tailor | Vluchten en tijdzones / Je opgeslagen antwoorden / Je antwoorden / Wat we kunnen afstemmen |
| slow | This is taking longer than usual. You can keep this screen open. | Dit duurt langer dan normaal. Je kunt dit scherm open laten. |
| offTitle / offBody / offBodyEdit | Checking needs a connection / Connect to the internet and try again. / … If you close now, your trip stays as it was. | Voor controleren is een verbinding nodig / … |
| srvTitle / srvBody | We couldn't check your trip / Something went wrong on our side. Try again in a moment. | We konden je reis niet bekijken / Er ging iets mis bij ons. Probeer het zo opnieuw. |
| thrTitle / thrBody / thrLater | Wait a moment / Too many attempts. Try again in {count} seconds. / Too many attempts. Please try again later. | Even geduld / Te veel pogingen. Probeer het over {count} seconden opnieuw. / … later opnieuw. |
| retry / retryIn / closeBtn | Try again / Try again in {n} s / Close | Opnieuw proberen / Opnieuw proberen over {n} s / Sluiten |
| qEyebrow | A few quick questions | Een paar korte vragen |
| qAgeWhy / qShiftWhy | Light and sleep advice differs by age. / Shift work changes how we can plan your clock. | Advies over licht en slaap verschilt per leeftijd. / Ploegendiensten veranderen hoe we je klok kunnen plannen. |
| disclaimer | ChronoFlow gives timing guidance, not medical advice. If you're unsure about a question, check with your doctor. | ChronoFlow geeft advies over timing, geen medisch advies. … |
| exTitle / exNote / exNoteSaved / exNoteEdit | No plan available / If you answered by mistake, you can change your answers. / You gave this answer on an earlier trip. Change it to plan this trip. / Your trip stays as it was. | Geen plan beschikbaar / … |
| changeAnswers | Change my answers | Mijn antwoorden wijzigen |
| ftTitle / ftSub / ftSubNone | Fine-tune your plan / All optional. The ones marked help your plan most. / No extra questions for this trip. You can still change these. | Stem je plan af / … |
| helps / add / done | Helps a lot / Add / Done | Helpt veel / Toevoegen / Klaar |
| prep / prepSub | Preparation days / Before you leave | Voorbereidingsdagen / Voor vertrek |
| diffSleep / tripBed / tripWake / sleepNote | Use different sleep times for this trip / Bedtime on this trip / Wake-up on this trip / Only for this trip. Your usual sleep stays saved. | … |
| readyTitle / editTitle / yourPlan | Ready to plan / Edit your trip / Your plan | Klaar om te plannen / Je reis bewerken / Je plan |
| lbl.* | Route / Flight / Return / Sleep / Extras / Questions / Latest wake-up | Route / Vlucht / Terug / Slaap / Extra's / Vragen / Uiterlijk wakker |
| rowHint | Opens this step | Opent deze stap |
| price / credit1 / free / balance | Price / 1 credit / Free / You have 3 credits | Prijs / 1 credit / Gratis / Je hebt 3 credits |
| adjTitle / adjBody | We adjusted your plan / We assumed you can wake up at 08:00 at the latest. If that's wrong, change it below. | We hebben je plan aangepast / … |
| changedTitle | Updated | Bijgewerkt |
| saveOffTitle / saveOffBody | Saving needs a connection / Connect to the internet and tap Make my plan again. Keep this screen open: closing discards the trip. | Voor opslaan is een verbinding nodig / … |
| saveSrvTitle / saveSrvBody | We couldn't save your plan / Something went wrong on our side. Tap Make my plan to try again. | We konden je plan niet opslaan / … |
| checkingBtn / buyCredits / update / make | Checking… / Buy credits on the website / Update my plan / Make my plan | Controleren… / Credits kopen op de website / Mijn plan bijwerken / Maak mijn plan |
| calcTitle / calcSub / calcDays | Calculating your plan… / This can take up to a minute. You can keep this screen open. / {n} days of steps | Je plan wordt berekend… / … / {n} dagen met stappen |
| discardTitle / discardBody / discardGuestBody | Discard this trip? / What you entered is not saved. / … You go back to the welcome screen. | Deze reis weggooien? / … |
| discardEditTitle / discardEditBody | Discard your changes? / Your trip stays as it was. | Je wijzigingen weggooien? / Je reis blijft zoals hij was. |
| keepEditing / discard / discardChanges | Keep editing / Discard / Discard changes | Verder bewerken / Weggooien / Wijzigingen weggooien |

### A.2 `WizardSheet.dc.html`

| Key | en | nl |
|---|---|---|
| cancel / done / remove | Cancel / Done / Remove | Annuleren / Klaar / Verwijderen |
| airportTitle.from/to/home | Where do you fly from? / … to? / Where do you fly home to? | Waar vlieg je vandaan? / … naartoe? / Waar vlieg je naar huis? |
| stopTitle / stopSub | Where's your stop? / Between {from} and {to} | Waar is je tussenstop? / Tussen {from} en {to} |
| search field (WizardCopy) | Airport or city | Luchthaven of stad |
| rows.* | Side of the plane / Dark room at your stay / Late check-out / Latest you can wake up / Busy hours before you leave / Sleep on free days / Coffee, tea and soft drinks / Light box or light glasses / Blue-blocking glasses | Kant van het vliegtuig / Donkere kamer op je verblijf / Laat uitchecken / Uiterlijk wakker om / Drukke uren voor vertrek / Slaap op vrije dagen / Koffie, thee en frisdrank / Lichtbox of lichtbril / Blauwfilterbril |
| q.seat | Which side of the plane is your seat on? | Aan welke kant van het vliegtuig zit je? |
| q.room | Will you have a room in {city} when you land, and from what time? | Heb je een kamer in {city} als je landt, en vanaf hoe laat? |
| q.checkout | Do you have a late check-out or a day room on the day you leave {city}? | Heb je een late check-out of een dagkamer op de dag dat je uit {city} vertrekt? |
| q.wake | What's the latest you can wake up during your trip? | Hoe laat kun je tijdens je reis uiterlijk wakker worden? |
| q.busy | Are there hours before you leave when you can't follow the plan, like work or school? | Zijn er uren voor vertrek waarin je het plan niet kunt volgen, zoals werk of school? |
| q.free / q.drinks / q.light / q.blue | How do you sleep on free days? / How many caffeinated drinks do you have on a normal day? / Do you have a light box or light glasses? / Do you have blue-blocking glasses? | Hoe slaap je op vrije dagen? / Hoeveel drankjes met cafeïne neem je op een normale dag? / Heb je een lichtbox of lichtbril? / Heb je een blauwfilterbril? |
| o.seat | Window left / Window right / Aisle or not sure ("We plan without the window") | Raam links / Raam rechts / Gangpad of weet ik niet ("We plannen zonder het raam") |
| o.room / o.yesno / o.free | When I land, From a time / Yes, No / Same as work days, Different times | Als ik land, Vanaf een tijd / Ja, Nee / Zoals op werkdagen, Andere tijden |
| o.light / o.blue | None, Light box, Light glasses / No, Yes with a rating, Yes no rating | Geen, Lichtbox, Lichtbril / Nee, Ja met een waarde, Ja zonder waarde |
| drinks / cupsADay | Coffee, Tea, Soft drinks, Energy drinks / Cups a day | Koffie, Thee, Frisdrank, Energiedrank / Kopjes per dag |
| fromLabel / bedLabel / wakeLabel / ratingLabel | From / Bedtime / Wake-up / Rating on the label | Vanaf / Naar bed / Wakker / Waarde op het etiket |
| addBusy | Add busy hours | Drukke uren toevoegen |
| errDrinks / errRating | Add at least one drink to save this answer. / Enter a rating between 0.1 and 9.9. | Voeg minstens één drankje toe om dit antwoord te bewaren. / Vul een waarde tussen 0,1 en 9,9 in. |
| remTitle / remBody / turnOn / notNow | Turn on reminders? / Reminders are off. When you turn them on, your phone asks once whether ChronoFlow may send notifications. / Turn on reminders / Not now | Herinneringen aanzetten? / … / Herinneringen aanzetten / Niet nu |

### A.3 Fine-tune row values (WizardCopy "Fine-tune rows and answers")

| Row | Value once answered |
|---|---|
| Seat | Window left / Window right; "Aisle or not sure" counts as no answer, so the row shows Add |
| Room | When I land / From 15:00 (nl: Als ik land / Vanaf 15:00) |
| Check-out | Yes / No |
| Wake | 08:30 |
| Busy | 1 time range / 2 time ranges (1 tijdvak / 2 tijdvakken) |
| Free days | Same as work days / 00:30–09:00 |
| Drinks | None / 1 a day / 3 a day (1 per dag) |
| Light | None / Light box / Light glasses |
| Blue | No / Rated 1.5 / Yes, no rating (Nee / Waarde 1,5 / Ja, zonder waarde) |

- Several flights: "Side of the plane · AMS → DXB" (one row per flight). Several stays: "Dark room · Dubai", "Late check-out · Dubai" (one row per stay).
- Required prompts: a "★ Helps a lot" badge, listed first.

### A.4 Ready values (WizardCopy "Ready values")

| Row | Example |
|---|---|
| Flight | "Mon 12 Oct 14:40 → Tue 09:15"; same day "Thu 22 Oct 10:25 → 15:30"; with stops "… · via DXB" (two stops: "via DXB, HEL") |
| Return | "One way"; with a return, the home flight; web trip "9 nights · until Wed 21 Oct" / "Check-out · Wed 21 Oct" |
| Sleep | "23:30–07:15"; trip times on "This trip · 00:30–08:30"; guest "23:30–07:15 · In between" |
| Extras | "3 days prep" / "1 answered · 1 day prep" / "2 answered · No prep days" |
| Questions (guest) | "None needed" / "1 answered" / "3 answered" |

- Dates line: "Fri 9 Oct – Wed 14 Oct · one way" / "· return" / "· 9 nights". Across a year end: "Tue 29 Dec 2026 – Mon 4 Jan 2027 · one way".
- Summary line: the server CopyLine ("We shift your clock 7 hours earlier, starting 3 days before you leave." etc.).
