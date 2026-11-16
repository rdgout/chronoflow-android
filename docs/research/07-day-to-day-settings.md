# 07 · Day-to-day, Settings and the app shell (Android research)

Date: 2026-11-15. Scope: everything outside auth, the wizard and the timeline. That covers the app shell (tab bar or rail), Today, My trips, Trip actions and Delete, every Settings page and state, reminders, Add to calendar, the Live Activity board, billing and paywall, data download, language, body clock, and pull to refresh with its sync feedback.

Sources (all read-only):

| Tag | Path |
|---|---|
| **B** | Design canvas `/Users/rickgout/Sites/chronoflow-android/_design/canvas` (version 1794774555-3fd6): `*.dc.html`, `canvas.json` notes |
| **I** | iOS app `/Users/rickgout/Sites/chronoflow-ios` (`plan.md`, `Docs/*.md`, `Packages/ChronoFlowKit/Sources/**`, String Catalogs) |
| **W** | Web/API `/Users/rickgout/Sites/chronoflow-new` (branch `feat/mobile-api-passkey-management`, HEAD `1f4a2684`) |
| **R** | React Native app `/Users/rickgout/Sites/chronoflow-mobile` |
| **S** | iOS simulator screenshots `/Users/rickgout/Sites/chronoflow-android/docs/research/screens/ios/*.png` |

How to read this document. Sentences marked **Fact** were checked in the source named next to them. Sentences marked **Rec** are recommendations for Android. **OQ-n** marks an open question; each one has a default, so the plan can proceed while the user is away (all collected in §14).

---

## 0. Answer to the user's question: what can be reused from the iOS research and plan?

A lot. iOS has already made most of the product decisions for these screens and recorded them, and much of its data can be copied or converted rather than rewritten.

| Reusable item | Where (I) | How Android uses it |
|---|---|---|
| **Settled product scope**: what ships and what is deliberately omitted (calendar row, trip complete/delete, billing hand-off, data download, Live Activity, practice mode, email change) | `plan.md` Phase 17c/17h, Phase 19 "Not planned", OQ 31/41/42 | Copy the same omissions so the two apps behave the same (§1.3) |
| **String Catalogs (en + nl), English sentence keys** | `Sources/Settings/Resources/Settings.xcstrings` (277), `Reminders/Resources/Reminders.xcstrings` (31), `PlanUI/Resources/Screens.xcstrings` (121), `PlanUI/Resources/Localizable.xcstrings` (104), `Settings/Resources/Terms.xcstrings`, `PlanCore/Resources/PlanCopy.xcstrings` | Generate `values/strings.xml` and `values-nl/strings.xml` with a converter: `{{x}}` and `%@`/`%lld` become positional `%1$s`/`%1$d`; plural variations become `<plurals>`; keys are slugged. Dutch is already human-written and approved, so nothing needs translating again |
| **Reminder engine spec and parity fixture** | `Sources/Reminders/ReminderSelection.swift`, `ReminderSchedule.swift`, `ReminderService.swift`; fixture `Tests/RemindersTests/Fixtures/selection.json` (1.0 MB, 110 RN selection vectors), exporter `Tools/ReminderParity/export-selection.cjs` | Port to pure Kotlin and run the same JSON vectors in JVM unit tests |
| **Plan, account and plan fixtures** | `Tests/DataAccessTests/Fixtures/account/{identity,snapshot}.json`, `plans/S1…S6*.json` (+ `SOURCE.md`) | Reuse as mock-source and decoder test fixtures |
| **Pure logic that is already specified** | Today selection (`RN src/plan/today.ts`, `today-trip.ts`), sync-line wording (`Settings/SettingsHomeState.swift:113-140`), lockout wording (canvas note `reqPasswordLockLine`), pending-change grouping (17e), body-clock readiness (no-health-data) | Port it once, with the same tests |
| **API contract map** | `Sources/DataAccess/API/APIDataSource*.swift`, W `routes/api.php:46-113` | §11 lists every endpoint |
| **Board-resolution decisions** (where the boards and the server disagree) | `Docs/settings-board-fidelity.md`, `Docs/no-health-data.md`, `Docs/phase-19-reminders.md` | Adopt them unchanged (age bands, single light-device answer, master reminder switch, and others) |
| **Lessons about Android itself** (from RN) | `R src/settings/useNotificationPermission.ts:27-34`, `R src/reminders/notifications.ts`, `R src/plan/calendar-service.ts` | Android 13 permission state, the `reminders` channel, calendar write rollback |

Not reusable: the SwiftUI views, GRDB specifics, `UNUserNotificationCenter` (which can list pending requests; Android's `AlarmManager` cannot, see §7), EventKit, Liquid Glass tab minimisation, and the iOS test harness.

---

## 1. Scope baseline

### 1.1 What iOS ships today (Fact, I)

- **Shell**: a system `TabView` with three tabs. Today uses `square.grid.2x2`, My trips `airplane`, Settings `gearshape`, tinted `ColorToken.primary` (`App/AppTabs.swift:143-168`). The bar minimises on scroll on iOS 26+ (15a review round 1). Routing gates, in order of precedence: verify → terms → walkthrough → Today (`SessionGateRouter`, 15a).
- **Today** (17b), **My trips** (15b), **Trip actions** (17c), **Edit and Duplicate** (17d).
- **Settings**: home and sign-out (17e), Account/Profile (name only), Language, Body clock (as revised by no-health-data), Preferences, Walkthrough replay, Security (password, 2FA, passkeys added 2026-11-12), Plan & credits (read-only), Delete account (17h and the 11-09 fidelity pass).
- **Reminders**: local notifications, the Connectivity & reminders page, and the first-trip "Turn on reminders?" sheet (Phase 19, 19f).
- **Pull to refresh** with a feedback capsule on Today, My trips and the trip page (2026-11-09).

### 1.2 Boards without an iOS implementation, and why (Fact, I `plan.md` Phase 19 "Not planned" and the 17h deferrals)

| Board(s) | iOS status | Reason recorded |
|---|---|---|
| CalendarExport/Denied/None/Failed, TripActions "Add to calendar" row | Row omitted | OQ 42 default: a separate calendar phase |
| TripActions "Mark trip as completed", TripDelete, SheetStates delete failed/offline | Omitted | OQ 31: the mobile API has no complete/delete endpoint; the card flags `canComplete`/`canDelete` exist but the API sends them false (W P13-D6) |
| SetPaywall, SetCancel, billing rows (Invoices, Update payment method, Cancel, Resume) | Omitted | OQ 41: no mobile billing hand-off endpoint |
| SetData, DataDownload, DataDownloadReady, SetStatesMore "data download failed" | Omitted | Only the session-authenticated web route `GET settings/data.json` exists (W `routes/settings.php`) |
| SetProfile email change, PasswordPromptEmail | Email is read-only | Deferred (17h); the server already supports it (W P15 `PATCH /v1/users/me` with `current_password`) |
| SetReminders "Live Activity" and "Practice in-flight mode" cards; LiveActivity board | Not built | Phase 19 board resolution |
| SetPreferences "Optional items" switch | Removed | Rick, 5 Nov: the app never shows optional steps (canvas `planCompactQuestions`) |

### 1.3 Android scope recommendation

**Rec:** reach parity with **iOS as built**, not with every board. "Behaves exactly like the native iOS app" means the same omissions as §1.2. Each omission is listed under OQ-1…OQ-6 with its default. Two items need Android-specific designs that no board supplies (OQ-7, OQ-8).

---

## 2. App shell: tab bar and rail

### 2.1 Boards and decisions (Fact, B)

- Every tab board draws `<nav aria-label="Main">` with three tabs: **Today** (`Today.dc.html`), **My trips** (`Trips.dc.html`), **Settings** (`SetHome.dc.html`). Icons are lucide LayoutDashboard, Plane and Settings. NL labels: **Vandaag**, **Mijn reizen**, **Instellingen**; the nav label is "Hoofdmenu" (`SetHomeNL`).
- Canvas note `dirNative` (2 Nov): "The tab bar is the system's: … Material on Android. We set its icons, labels and the amber selected colour; its shape, size, position and blur are the OS's." `reqSystemChrome` adds: "Done 2 Nov (iOS drawn; **Android Material not drawn yet**)". No Android shell board exists.
- On segmented controls, `reqSystemChrome` notes: "on Android it takes our colours".
- Tab-bar minimise is iOS 26 only. "Android keeps its full Material bar" (R `plan.md:1460`, Rick 5 Nov).
- Wide layouts (`reqWideTrip`): the breakpoint is a **600 pt window width**. The wide boards leave the tab bar out ("the OS places it"). Only the Day plan and Two-factor have wide boards (`Set2FAWide`, `Set2FAManageWide`: content in a 560 pt column).
- Navigation titles (I `plan.md:59-66`, user 2026-11-10): tab titles and every Settings subpage use the **plain system large title** (system font, bold), not Fraunces. Titles collapse on scroll. The S screenshots confirm this.

### 2.2 Android recommendation

- **Rec:** use `NavigationSuiteScaffold` (androidx.compose.material3.adaptive:adaptive-navigation-suite). It shows a `NavigationBar` when the window width class is compact and a `NavigationRail` from medium (≥600 dp), which matches the 600 pt breakpoint used on iOS and the canvas. Inner panes take only the width they are given.
- **Rec:** icons. Material Symbols Rounded `grid_view`, `flight` and `settings` are closest to the iOS glyphs; R used `today`/`flight`/`settings` (`R plan.md:939`). For the selected colour, iOS tints with `primary`, which is indigo in light and dawn in dark (S `10-settings-home.png` shows indigo), while the note says "amber". **OQ-9** (default: follow iOS and use `primary`). The indicator pill uses token `muted` (DS README "active nav pill … `muted`"). Map these through `NavigationBarItemDefaults.colors(...)`.
- **Rec:** give each tab its own back stack and save/restore state on reselect. Reselecting the current tab pops to its root and scrolls to the top. System back on a non-Today tab root goes to Today, then exits (Material guidance). Turn on predictive back (`android:enableOnBackInvokedCallback="true"`; the RN manifest had it false).
- **Rec:** top app bars use `LargeTopAppBar` with `exitUntilCollapsedScrollBehavior` (the system large title that collapses) in the default system typography, as decided on iOS on 2026-11-10. **OQ-10**: the boards still draw Fraunces large titles; default is the system font, as on iOS.
- **Fact (Android platform, to verify against current docs):** apps targeting API 36 have orientation locks and resizability restrictions ignored on displays with smallest width ≥600 dp. Android can therefore not stay portrait-only on tablets or unfolded foldables. **Rec:** phones stay portrait (iOS D16); every screen in this document must lay out at 600–1200 dp wide in both orientations. Settings pages get a 560 dp max-width centred column at ≥600 dp (the Set2FAWide pattern). **OQ-11** (default: apply the 560 dp column to every Settings page).
- **Rec:** targetSdk 35+ enforces edge-to-edge. Consume `WindowInsets.safeDrawing` once per screen. The scaffold handles nav-bar and rail insets; lists add bottom content padding instead of hand-made bar spacing (RN's "manual bottom padding" was removed for the same reason, `R plan.md:951`).

---

## 3. Today

### 3.1 Boards and states (Fact, B `Today.dc.html` states `active|empty|future|loading|completed`; wrappers TodayEmpty/TodayFuture/TodayLoading/TodayCompleted; `TodayRowStates`)

| State | Content (en) | Actions |
|---|---|---|
| **active** | Hero card (2 px `dawn` border, radius 26): `Badge status=active`, `MetaChip` flight number ("KL 861"), offset "+7 h" right, IATA codes in Fraunces 30/36 with city names, "12 Oct 14:40 → 13 Oct 09:15", inner row on `surface-2`: `CategorySwatch` plus eyebrow **"Next · in 30 min"** (or **"Next · now"**) and the step title, with a chevron | Hero → trip page (`Plan.dc.html`) |
| | `TodayPanel`: day label ("Arrival day"), `StatusPill` (adapting / partly adapted / adapted), progress "3/7 done", section **Right now** (current steps with "45 min left"), section **Up next** (max 4 steps, "In 30 min") | Row → step sheet (ItemDetail) |
| | Hidden-categories link row: **"Travel steps are hidden"** / **"Show them in Settings › Preferences"** | → Settings › Preferences (`gapTripPage`, 1 Nov) |
| **completed** (only completed trips) | **"No trips coming up"**; "Your last trip, {{route}}, ended on {{date}}. Plan the next one when you know your flights." | **Plan a trip** (primary), **See past trips** (ghost → My trips tab) |
| **empty** | Dashed card, moon icon, **"No trips yet"**, "Add your flights and you get a day-by-day plan for sleep, light and caffeine." | **Plan a trip** |
| **future** | Card: `Badge upcoming`, "In 6 days", "AMS → NRT", "Mon 12 Oct · KL 861 · +7 h", row with eyebrow "Plan starts Fri 9 Oct" and "Seek bright light · 07:30"; card **"Nothing to do today"**, "Keep your usual sleep times. Your 3 preparation days start on Friday; we'll remind you the evening before." | Card → trip page |
| **loading** | Skeletons: 248 px card, 40 % × 20 px line, three 64 px rows; `aria-label="Loading today"` | — |

**TodayRowStates** (1 Nov): done and skipped rows **stay in place** until their time is over. Done shows a check in a filled dawn circle, a dimmed swatch, a muted title and "Done" first in the second line ("Done · 07:30 → 10:30 · Tokyo"). Skipped shows an outlined skip icon and "Skipped · …". There is no strike-through and no check control on the row. Undo lives in the step sheet. Screen readers hear "{{title}}, done". Progress counts done steps only.

### 3.2 Behaviour (Fact, I 17b and R `src/plan/today.ts`, `today-trip.ts`)

- **Trip selection** (`selectTodayTrip`): drop completed trips. Active trips come first, then the soonest-ending active trip, then the soonest-starting upcoming trip, with id as the tie-break. Status is re-derived locally by `tripDisplayStatus(card, planStartUtc, planEndUtc, now)`: the server's `completed` is kept; otherwise `now ≥ planEnd` → completed, `planStart ≤ now` → active, anything else → upcoming.
- **`todayView`**: optional steps are removed (`withoutOptionalSteps`). Excluded types are the hidden categories plus `light_ok` and `light_neutral`. *current* = blocks with start ≤ now < end. *upcoming* = the next 4 blocks with start > now. *next step* = the first actionable block with a `checkKey` that is unmarked and has end > now; `minutesUntil = ceil((start-now)/60000)`, and "running" means it has started. Flags: `allCategoriesHidden`, `importantHidden` (some core block is hidden), `beforePlan`, `afterPlan`.
- `lastEndedTrip` feeds the completed state. `calendarDaysUntil` works in the plan-start zone and feeds "In N days".
- A minute tick drives "Next · in N min" and the relative times.
- iOS deviation: the future note **drops "we'll remind you the evening before"**. iOS uses "Keep your usual sleep times." plus the plural "Your {{count}} preparation days start on {{date}}." (I 17b).
- iOS hidden-steps copy: "Some important steps are hidden." / "All categories are hidden." / "Show them in Settings › Preferences" (PlanUI Screens catalog). The board's "Travel steps are hidden" is sample copy.
- "Plan a trip" was hidden until 16c and is shown now that the wizard ships.

### 3.3 Copy (Fact, I `PlanUI/Resources/Screens.xcstrings`, en → nl)

`Today`→Vandaag · `Right now`→Op dit moment · `Up next`→Hierna · `Next · in {{duration}}`→Volgende · over {{duration}} · `Next · now`→Volgende · nu · `In {{duration}}`→Over {{duration}} · `{{duration}} left`→Nog {{duration}} · `{{done}}/{{total}} done`→{{done}}/{{total}} gedaan · `{{done}} of {{total}} done`→{{done}} van {{total}} gedaan · `Done · {{time}} · {{zone}}`→Klaar · … · `Skipped · {{time}} · {{zone}}`→Overgeslagen · … · `{{title}}, done`/`{{title}}, skipped` · `No trips yet`→Nog geen reizen · `No trips coming up`→Geen reizen gepland · `Your last trip, {{route}}, ended on {{date}}. Plan the next one when you know your flights.` · `See past trips`→Eerdere reizen bekijken · `Plan a trip`→Plan een reis · `Nothing to do today`→Vandaag hoef je niets te doen · `Keep your usual sleep times.` · `Your {{count}} preparation days start on {{date}}.` (plural one/other) · `Plan starts today/tomorrow` · `Plan starts on {{date}}` · `In {{count}} days` (plural) · `Nothing scheduled right now.` · `No more scheduled items.` · `Adjusting`/`Partly adapted`/`Expected adapted` · `Loading today`→Vandaag laden · `Some important steps are hidden.` · `All categories are hidden.` · `Show them in Settings › Preferences`→Zet ze aan in Instellingen › Voorkeuren · `Check-off undone`.

### 3.4 Android recommendation

- **Rec:** one `LazyColumn` with stable keys (`block.id`, plus section keys) and `contentType` per row kind. Rows are full-width tap targets with min height 56 dp (`min-h-14`).
- **Rec (performance):** use one process-wide `PlanClock` exposing `StateFlow<Instant>` that emits at **minute boundaries** (the iOS PlanClock pattern). Read it only in the leaf composables that print relative times, through `derivedStateOf`, so a tick recomposes those texts and nothing else. Compute `todayView` off the main thread in the ViewModel (`Dispatchers.Default`) from Room `Flow`s (trip cards, plan version, check marks, preferences) combined with the clock.
- **Rec:** the step sheet (`ModalBottomSheet`) is shared with the trip page (research doc for the timeline). Done/Skip/Undo go through the outbox with optimistic marks.
- **Rec:** accessibility. Each row merges its descendants (`Modifier.semantics(mergeDescendants = true)`) with the label "{{title}}, done". The progress row is one static spoken summary (iOS 15b repair notes).

---

## 4. My trips

### 4.1 Boards (Fact, B `Trips.dc.html` states `default|empty|loading`)

- Header action: a **"+" button** with `aria-label="New trip"` → wizard. Large title **My trips**.
- Sections **Active**, **Upcoming**, **Completed** (h2). Cards show badge, `MetaChip` flight number, offset, IATA/city pairs and the date range. Active cards add the "Next · in 30 min" row.
- Empty: **"No trips yet"**, "Your upcoming, active and past trips show up here.", **Plan a trip**.
- Loading: four skeletons, `aria-label="Loading your trips"`.

### 4.2 Behaviour (Fact, I 15b and review round 1)

- Sections run in RN order (Active, Upcoming, Completed). Within a section, sort by **departure descending, then id descending** (I 15b). The server list order is also status rank, then `departureUtc` desc, then id desc (W P13-D7).
- Status is re-derived on clock ticks. Tombstoned trips (`deletedAt` or `deletedLocally`) vanish.
- The next step comes **only from the local plan**, with no server fallback.
- Completed cards are dimmed. A trailing chevron shows when there is no next-step row. Tapping a card opens the trip page.
- Pull to refresh → `SyncEngine.refreshNow()` (§10).
- Empty-state button text uses `primaryForeground` (a light-mode contrast bug fixed on iOS, 2026-11-09).
- Plans are stored only for trips that are active or start within 7 days (`keepOffline` on `TripListItem`). Other trips list from their card and download the plan on open (canvas `reqTripOffline`; timeline doc).

### 4.3 Android recommendation

`LazyColumn` with `stickyHeader`-free section headers (the boards have plain h2), card keys = trip id, `animateItem()` for list changes. Card composables take an immutable `TripCardUi` (`@Immutable`). The next-step text is derived in the ViewModel, so the list never touches a whole plan.

---

## 5. Trip actions, Delete trip and sheet states

### 5.1 Trip actions (Fact, B `TripActions.dc.html`; I 17c)

- Sheet header **"{{from}} → {{to}}"** with subtitle "12–22 Oct · via Dubai · with return" (`via %@`→via %@, `with return`→met terugreis). Close button.
- Rows:
  - **Edit trip** → wizard edit (17d).
  - **Duplicate trip**, "Same flights and answers, new dates".
  - *Add to calendar*, "Choose steps and alerts" (no plan: disabled, "Available once the plan is made").
  - *Mark trip as completed*.
  - *Delete trip* (destructive).
  - **Cancel**.
- iOS: Edit and Duplicate are always shown, **disabled with "Needs a connection."** when offline, busy, or the card's `canEdit`/`canDuplicate` is false. Complete, Delete and Add to calendar are **omitted**. Duplicate failure copy: "We could not duplicate this trip. Try again." / "Something went wrong on our side. Try again in a moment."
- Sheet chrome is native ("system corner radius and grabber"). The content is ours (`reqSystemChrome`).

### 5.2 Delete trip (Fact, B `TripDelete.dc.html`, `SheetStates`; not built on iOS)

- System alert **"Delete {{route}}?"**: "The trip, both plans and your check-offs are deleted on all your devices. This can't be undone." Buttons **Keep trip** / **Delete trip** (destructive) → My trips.
- Failure alert: **"Couldn't delete the trip"**, "Nothing was deleted. Try again in a moment." [OK] [Try again].
- Offline, shown *instead of* the confirmation: **"No connection"**, "Deleting a trip needs a connection. Try again when you're online." [OK].
- Canvas `gapSheets`: the Delete copy is still sample copy; "web wording" is pending.
- Endpoints: web `DELETE trips/{trip}` and `PATCH trips/{trip}/completion` exist (W `routes/trips.php`); there is **no `/v1` equivalent**.

### 5.3 SheetStates (Fact, B)

- Trip actions loading: skeletons, "Usually too quick to see".
- **"This trip isn't on this phone yet"** / "It shows up as soon as the phone syncs. Check that you have a connection." [Close].
- Item detail loading.
- **"This step is no longer in your plan"** / "Your plan changed after you opened it, for example because you edited the trip. Your other check-offs are kept." [See today's steps] (built on iOS 17a).

### 5.4 Android recommendation

`ModalBottomSheet` (M3) for Trip actions, holding the designed rows. `AlertDialog` for the system alerts. Use `ListItem`-like rows with our tokens, disabled state at 50 % alpha, and the reason as a supporting line. **OQ-2:** default omits Complete and Delete (parity with iOS), but builds the `AlertDialog` copy so it can be switched on once `/v1` endpoints exist.

---

## 6. Settings

### 6.1 Settings home (Fact, B `SetHome`, `SetHomeNL`, `DarkSetHome`; I 17e and the fidelity pass; S `10-settings-home.png`)

- Large title **Settings**. The profile card shows initials ("RG"), name and email, and pushes Profile.
- Under the card, the **sync line**: one quiet line with a cloud icon, never a banner.
- Rows (icon disc, title, subtitle, chevron):

  | Row | Subtitle (board) | Subtitle (iOS today) |
  |---|---|---|
  | Account | "Profile, language, your data" | "Profile, language" (data is deferred) |
  | Security | "Password, passkeys, two-factor" | "Password, two-factor" when passkeys aren't available (gated on `security.passkeyCount`) |
  | Body clock | "The answers every plan is built from" plus a dawn badge "{{n}} to answer" (plural; NL "Nog {{n}} vragen te beantwoorden") | Same |
  | Preferences | "Appearance, timeline, reminders" | Same |
  | Plan & credits | "{{n}} credits left" (plural) | Same, from the local `planCredits` |

- **Sign out**: an outline pill. There is no separate Reminders row; Reminders sits under Preferences (I 19f).
- NL row titles: Account, Beveiliging, Biologische klok, Voorkeuren, **Abonnement en tegoed**, Uitloggen. Long Dutch subtitles must wrap (`SetStates` "Long Dutch labels").

**Sync line wording** (Fact, B `SetStates` table; I `SettingsHomeState.swift:113-140`). Rules are checked in this order:

1. `Syncing…`
2. `{{n}} changes waiting to sync` (plural)
3. offline → `Saved on this phone · syncs when you're online`
4. `synced_at` age:

| Age | en | nl |
|---|---|---|
| <1 min | Synced just now | Zojuist gesynchroniseerd |
| 1–59 min | Synced {{n}} min ago | {{n}} min geleden gesynchroniseerd |
| 1–23 h | Synced {{n}} hour(s) ago | {{n}} uur geleden gesynchroniseerd |
| calendar yesterday | Synced yesterday | Gisteren gesynchroniseerd |
| 2–6 days | Synced {{n}} days ago | {{n}} dagen geleden gesynchroniseerd |
| ≥7 days | Synced on {{date}} ("24 Oct") | Gesynchroniseerd op {{date}} |

Never "0 min ago". The line updates once a minute while Settings is open.

**Sign out** (Fact, B `SignOutPending`/`SignOutPendingSettings`; I 17e and D25):

- Try a flush first. With nothing pending, sign out at once with no confirmation.
- Otherwise show a sheet: **"Sign out and lose {{n}} changes?"** (plural), "These haven't reached your account yet. If you sign out now, they're removed from this phone."
  - The list comes from the outbox: a check-off shows as `Checked off "{{step}}"` with its category swatch; a sleep patch shows as `Changed your usual sleep times`; other writes use the generic pending sentence. Group by the latest check-off per step and the first settings write per section.
  - Buttons **Stay signed in** / **Sign out and discard** (destructive). Footer: "Changes sync by themselves when you're online. Staying signed in keeps them."
- A forced 401 wipes without asking.
- The wipe deletes the account DB and secrets and cancels reminders *before* the best-effort `POST /v1/auth/logout` (3 s timeout). It keeps the device ID, language and theme.

### 6.2 Shared settings states (Fact, B `SetStates`, `SetStatesMore`, `SheetStates`)

- **Loading** (any page, before the account is stored): the real title, then skeleton rows in the page layout.
- **Not available**: "Your account isn't on this phone yet" / "We need a connection once to load your settings. After that they work offline." [Try again].
- **Toasts** (bottom): `Saved`, and `Couldn't save. Check your connection.` [Try again].
- **Refused change** (server 4xx on a synced change): the change is reverted, with no Try again, plus a Details action. Examples: `Couldn't save your sleep times, so we undid the change.` / `Couldn't save "{{step}}" as done, so we undid it.` iOS shows these in a shared floating toast, open step sheets included (quick win 2026-11-10).
- **Offline-disabled row**: the row is disabled with "Needs a connection." under it. Applies to server-only actions: 2FA, passkeys, password, delete, email change.
- **Validation error**: field error under the field.

### 6.3 Account → Profile, Language, Data, Delete

**Account page** (B `SetAccount`): rows Profile ("Rick Gout · rick@example.com"), Language ("Phone language (English)" → key `Phone language ({{language}})`), Your data & policies ("Accepted terms, download your data"), and a separate destructive row Delete account.

**Profile** (B `SetProfile`; I 17h):

- Fields Name and Email.
- The board's email-change flow: notice "We'll send a verification link to {{email}}. Use your new address to sign in, here and on the web.", heading "Verify your new email", primary **Save and send link** (opens the password prompt only when the email changed), "Changing your email asks for your password. Changing only your name doesn't.", ghost **Keep my current email**.
- **iOS:** name only, through the `profile` outbox → `PATCH /v1/users/me {name}`. Primary **Save changes**, validation "Enter your name.", email read-only. **OQ-5** (default: same as iOS).

**Language** (B `SetLanguage`; canvas `decLanguage`; I `SettingsDevicePreferences.swift`):

- Intro: "ChronoFlow follows your phone's language, and uses English if your phone is set to a language we don't offer yet. Pick a language here only if you want something different."
- `ChoiceCard` radio group:
  - **Phone language** — "Now {{language}} · changes with your phone"
  - **English** — "Always English"
  - **Nederlands** — "Always Dutch"
- Saved toasts: "Saved. ChronoFlow follows your phone." / "Saved. The app is in English." / "Opgeslagen. De app is in het Nederlands." (catalog en key `Saved. The app is in Dutch.`).
- iOS stores the choice **per device** (`UserDefaults` key `cf.settings.language` with values `system|en|nl`) and never sends it to the server. The phone mapping is `nl` → Dutch, anything else → English. The 12/24 h hour cycle follows the phone locale.
- **Fact (W):** `ApiPreferences.language` is read-only in `GET /v1/account`. `PatchPreferencesRequest` accepts only `time_format`, `hidden_categories` and `show_optional_items`, so **no mobile endpoint sets the account language** (used for emails and web).

**Rec (Android language):**

- Declare `res/xml/locales_config.xml` with `en` and `nl` and `android:localeConfig` in the manifest. This also exposes the system "App languages" page on API 33+.
- Use `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("nl"))`; an empty list means Phone language. On API ≤32, set `<service android:name="androidx.appcompat.app.AppLocalesMetadataHolderService" … autoStoreLocales=true>` and make `MainActivity` an `AppCompatActivity` (it is a `ComponentActivity`, so Compose is fine).
- The system becomes the source of truth for the choice, so the in-app page and the system page stay in sync. Read the current value with `AppCompatDelegate.getApplicationLocales()`.
- The activity is recreated after a change. Persist a one-shot "show saved toast for {choice}" flag in `SavedStateHandle` or DataStore so the toast appears after recreation.
- Fallback: resources default to English with `values-nl`, so nl-BE and others resolve to Dutch automatically and every other language to English. That matches iOS.
- After a locale change, re-localise the notification channel name and reschedule reminders. Bodies are built at schedule time, as on iOS (`ReminderService.setLanguage`).

**Your data & policies** (B `SetData`, DataDownload*, `SetStatesMore`):

- "What you accepted": Terms of service "Version 3.1 · accepted 2 Sep 2026" and Privacy policy "Version 2.4 · …" → PolicyReader; footnote "When a policy changes, we ask you to accept the new version before you build your next plan."
- "Your data": "Get a copy of your account, trips, plans and body-clock answers as one file." [Download my data] → *preparing* ("Preparing your file…", "This usually takes less than a minute. You can leave this screen; we'll keep going.") → *ready* ("Your file is ready", "chronoflow-data-2026-10-31.zip · 184 KB", [Save or share file]).
- Failure: "We couldn't prepare your download." / "Nothing was sent. Try again in a few minutes." [Try again].
- **Fact:** there is no `/v1` export endpoint, so iOS omits the row. The policy data is available: `settings.policies {termsVersion, privacyVersion, acceptedTermsVersion, acceptedTermsAt}`. Privacy acceptance has no accepted-at field.
- **Rec (when an endpoint exists):** stream into `cacheDir/exports/`, expose it through a `FileProvider` (`<cache-path>`), and open the **Android Sharesheet** (`Intent.ACTION_SEND` + `Intent.createChooser`, `FLAG_GRANT_READ_URI_PERMISSION`). Optionally add **Save to device** through SAF `ActivityResultContracts.CreateDocument("application/zip")`. Delete the temp file once the chooser returns. R did share-then-delete (`R src/settings/share-data.ts`). **OQ-3** (default: omit, as on iOS).

**Delete account** (B `SetDelete`, `PasswordPromptDelete`; I 17h):

- Sheet over Account: **"Delete your account?"**, "Your trips, plans, body-clock answers and any credits left are deleted for good, here and on the web. This can't be undone." [Delete my account] (destructive), "You'll be asked for your password next.", [Keep my account].
- Delete opens the shared password prompt, then `DELETE /v1/account {password}` → 204 → the local wipe path.
- Errors keep the account: wrong password, offline, throttle, server. A billing 422 shows "Your subscription could not be cancelled. Please try deleting your account again."
- **Fact (policy):** Google Play requires in-app account deletion plus a web deletion resource for the Data safety form. The web has `DELETE settings/profile` (W `routes/settings.php`).

### 6.4 Security

**Security page** (B `SetSecurity`):

- **Password** — "Change your password"
- **Passkeys** — "{{n}} passkeys"
- **Two-factor authentication** — "A code from an app when you sign in" + badge **On**/**Off**

**Password** (B `SetPassword`):

- Fields Current password, New password, Confirm new password; [Change password].
- Warning notice "Check the highlighted field." / "Your new password isn't saved yet."
- → `PUT /v1/account/password {current_password, password, password_confirmation}` → 204. Other tokens are revoked.
- Client copy: "The passwords don't match." · server-policy lines "Use at least {{min}} characters." … · success "Password updated".
- Policy string from `security.passwordRules` (W `ApiSecuritySummary`).

**Two-factor** (B `Set2FA`, `Set2FASetup`, `Set2FAOn`, `Set2FACodes`, `Set2FAManage`, NL and Wide variants; copy table embedded in `Set2FA.dc.html`):

- **Off**:
  - Intro copy (`offBody`): "Ask for a code from an authenticator app each time you sign in with your password, so a stolen password isn't enough."
  - Three bullets:
    - **b1** "You need an authenticator app, like the one built into Passwords on iPhone." (**iOS-specific → OQ-7**)
    - **b2** "You get recovery codes for when you lose your phone."
    - **b3** "Signing in with a passkey doesn't ask for a code."
  - [Turn on two-factor], with "You'll be asked for your password first."
- **Setup**:
  - "1 · Scan this code with your authenticator app": QR on a white tile in both themes, then "Using this phone? Copy the setup key into the app instead." with the monospace key and [Copy].
  - "2 · Enter the 6-digit code it shows": field placeholder 123456, then [Turn on two-factor].
- **On**:
  - "Since {{date}}", from `security.twoFactorConfirmedAt`.
  - Recovery codes (8, in two monospace columns): [Copy] [Download].
  - [Make new codes] "Your current codes stop working"; [Turn off two-factor] "Sign in with only your password or passkey" (red, lock icon).
  - "Each of these asks for your password first."
  - Toast "Two-factor is on. Save your recovery codes now." appears only right after turning it on.
- **Manage**: [Show recovery codes] "Each code works once and is then replaced", plus the Make new codes and Turn off rows.
- Errors:
  - Wrong code (field): "That code didn't work. Codes change every 30 seconds; try the newest one."
  - Server failure (banner): "Couldn't turn on two-factor." / "Nothing changed on your account. Try again in a moment." The QR stays on screen.
  - 409 keys: "Two-factor authentication is already on." / "Two-factor authentication is off." / "Start two-factor setup again."
- API:
  - `POST /v1/account/two-factor {current_password}` → setup (secret, QR)
  - `POST …/two-factor/confirmation {code}` → codes
  - `DELETE …/two-factor {current_password}` → 200 `ApiAccountSnapshot`
  - `POST …/recovery-codes/reveal {current_password}`
  - `POST …/recovery-codes {current_password}`

**Shared password prompt** (B PasswordPrompt* boards; canvas `reqPasswordPrompt`, `reqPasswordFollowUp`, `reqPasswordLockLine`):

- Native sheet **"Confirm it's you"** with Cancel, a purpose line, a secure field, **Forgot password?** and one action button.
- Purpose lines:
  - "Enter your password to turn on two-factor authentication."
  - "…to turn off two-factor authentication."
  - "…to show your recovery codes."
  - "…to make new recovery codes. Your current codes stop working."
  - "…to delete your account. This can't be undone."
  - "…to add a passkey on this phone." (unapproved, see I 11-12 addendum)
- Action labels: Continue / Turn off / Show codes / Make new codes / Delete account (destructive ones are red).
- States:
  - empty: button disabled
  - checking: "Checking…", spinner, field dimmed
  - wrong: "The provided password was incorrect." under a red ring
  - locked: "Too many attempts. Try again in {{n}} seconds/minutes/hours." Under 60 s show seconds; from 1 min show whole minutes rounded up; from 60 min show hours rounded up; all pluralised.
- **Forgot password?** opens an alert: "Reset your password?" / "We'll email a reset link to {{email}}. You stay signed in on this phone." [Cancel] [Send link] → `POST /v1/auth/forgot-password` → "Check your inbox."
- NL keeps "inloggen" everywhere (`reqPasswordLockLine`).

**Passkeys** (B `SetPasskeys`, `SetPasskeysEmpty`; I addendum 2026-11-12; W Phase 20):

- Intro: "Sign in with Face ID, a fingerprint or your screen lock instead of your password." (**iOS-specific "Face ID" → OQ-7**)
- Each row: name, then "Added {{date}} · used {{date}}" (today → "used today"), plus a ghost remove button "Remove passkey {{name}}".
- Empty: "No passkeys yet" / "Add one on this phone and you can sign in without typing a password."
- [Add a passkey]. Offline it is disabled with "Needs a connection.".
- Remove confirm: "Are you sure you want to remove the "{{name}}" passkey? You will no longer be able to use it to sign in." Toasts "Passkey added" / "Passkey removed".
- Errors (422): "The passkey registration request expired. Please try again." / "We couldn't register that passkey. Please try again."
- API:
  - `GET /v1/account/passkeys` → `{passkeys:[{id,name,authenticator,createdAt,lastUsedAt}]}`
  - `POST /v1/account/passkeys/options {current_password}` → `{challengeId, options}`
  - `POST /v1/account/passkeys {challengeId,name,credential}` → 201 item
  - `DELETE /v1/account/passkeys/{id} {current_password}` → 204
- The row is gated on `security.passkeyCount` being present.
- **Rec (Android):** `androidx.credentials.CredentialManager.createCredential(CreatePublicKeyCredentialRequest(optionsJson))` with the server's options JSON. The server must publish `/.well-known/assetlinks.json` with `delegate_permission/common.get_login_creds` for the Android package and signing-cert SHA-256 (the analogue of AASA). A user cancel shows no error. Note that the Phase 20 branch is not deployed.

**Recovery codes Copy/Download** (I 17h): codes stay in memory only and are cleared on leaving.

- **Rec:** `ClipboardManager.setPrimaryClip` with `ClipDescription.EXTRA_IS_SENSITIVE=true` (API 33+, hides the clipboard preview). Clear after 60 s if it is still our clip; iOS uses an expiring clipboard entry.
- **Rec:** Download builds an in-memory text document shared through the Sharesheet via a `FileProvider` cache file that is deleted afterwards, or SAF `CreateDocument("text/plain")`. Default: Sharesheet, matching the iOS share sheet.
- **Rec:** set `FLAG_SECURE` on windows that show recovery codes, setup keys or QR codes. This is not in iOS, but it is low-risk. **OQ-12** (default: yes).

### 6.5 Body clock (B `SetBodyClock`/`DarkSetBodyClock` *as superseded by* I `Docs/no-health-data.md` and the W phase 19 schema)

The board's health questions (pregnant, bipolar, eye condition, blood clots, shifts; age bands 18–29/30–44/…) are **superseded**. Follow iOS (`Sources/Settings/SettingsBodyClockView.swift:71-222`; S `11-settings-bodyclock.png`).

- Header: "Every plan is built from these answers. Changes save automatically." Each section saves automatically: a section patch goes to the outbox → `PATCH /v1/account/body-clock`. A check plus "Saved"/"Opgeslagen" shows beside the saved section heading, and the view scrolls that heading into view.
- **Readiness card** (top), from `GET /v1/account` `readiness.variant ∈ needs-answers|excluded|adjusted|ready`:
  - **needs-answers**: "Almost ready" (dawn eyebrow), "We need {{count}} more answers before we can build a plan" (or "…1 more answer…"), progress `{{done}}/{{total}} done` where total = 2 + invalid sections, and [Go to the first one].
  - **excluded**: "No plan available" + the server exclusion line: "This plan is not available for travellers under 18." / "This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule."
  - **ready / adjusted**: "Required answers complete" / "Your age range and shift-work answer are saved. You can add optional details below." Adjusted adds the server `adjustments` lines: "No caffeine anywhere in your plan.", "Regular movement breaks on long flights, with shorter in-flight sleep stretches.", "Your plan takes your age range into account in its timing." The adjusted heading is "Your plan is adjusted for your preferences and age range".
- Sections, with the server field each one writes (W `BodyClockRules::rules`, `ProfileInputs::fields`):

| Section (server enum) | Control | Field and values |
|---|---|---|
| **About you** (`about`) | ChoiceCards "When do you naturally feel most awake?": Early bird / In between / Night owl / Not sure (+hints) | `chronotype` ∈ `early,intermediate,late,null` |
| | Pills "Your age": Under 18 / 18–64 / 65 or older | `age_band` ∈ `under_18,18_64,65_plus` (required) |
| **Sleep schedule** (`sleep`) | "On a normal work or school day." time tiles Usual bedtime / Usual wake time → native time picker | `habitual_bed`, `habitual_wake` `HH:mm` (sent as a pair) |
| | "Do your free days follow the same schedule?" Yes/No; when No: "On free days" Bedtime / Wake time | `free_day_status` ∈ `same_as_workdays,declared` (+`unconfirmed`); `free_day_bed`/`free_day_wake` |
| **Caffeine** (`caffeine`) | "Do you usually drink caffeine?" Yes/No | `caffeine_habit` ∈ `cups,non_user` |
| | When Yes: "What do you drink on a normal day?" steppers Coffee/Tea/Soft drinks/Energy drinks, "{{count}} a day", 0–20, a11y "Fewer {{drink}}"/"More {{drink}}"; error "Add at least one drink to save this answer." | `cups_filter_coffee`, `cups_tea`, `cups_cola`, `cups_energy_drink` |
| | "Does caffeine affect you strongly?" Yes/No + hint | `caffeine_sensitive` (bool; the board's third answer "Not sure" can't be stored) |
| | Switch "Leave caffeine out of my plan" / "Your plan won't suggest any caffeine." | `exclude_caffeine` |
| **Light equipment** (`light`) | "Optional. If you have any of these, your plan can use them." "Do you have a light box, light visor or light glasses?": None / Light box or visor / Light glasses; eye-caution caption | `light_device` ∈ `none,box,glasses` |
| | "Blue-blocking glasses": No / Yes, rating unknown / Yes, I know the rating → field "Rating (mDFD)" + hint, decimal keypad, "Enter a rating between 0.1 and 9.9." | `blue_blockers` ∈ `no,unrated,rated`; `blue_blocker_mdfd` 0.1–9.9 one decimal |
| **Safety checks** (`health`) | Yes/No "In the past 14 days, have you worked night or rotating shifts, or had a very irregular sleep schedule?" | `shift_work` (required) |
| | Switch "Add movement breaks during long-flight sleep" + long hint | `flight_movement_breaks` |
| | Captions: audience notice and bipolar notice | — |

- A missing required answer shows a "Needs an answer" badge with a dawn-tinted box.
- Validation keys: "Choose one of the available options.", "Enter a valid time.", "Check your bedtime and wake-up time.", "Enter a rating between 0.1 and 9.9."
- D24 pending-value protection: a server pull never overwrites a field that has a pending outbox patch.
- **Rec (Android):** use the M3 `TimePicker` (dial or input) inside our sheet with Cancel/Done (the board says "the bedtime picker uses system wheels" on iOS; Android has no wheel, so `TimePicker` is the native equivalent). Use M3 `Switch` with an amber track (`SwitchDefaults.colors(checkedTrackColor = dawn)`). Pills and ChoiceCards are ours. Use `LazyColumn` sections with `animateScrollToItem` for "Go to the first one" and for scrolling to the saved section.

### 6.6 Preferences (B `SetPreferences`, `SetPreferencesNL`; I 17h, 19f; S `12-settings-preferences.png`)

- **Appearance**:
  - Theme pills: System / Light / Dark (Systeem/Licht/Donker), per device.
  - Time format pills: "24-hour (14:30)" / "12-hour (2:30 PM)", account-synced → `PATCH /v1/account/preferences {time_format: "24"|"12"}`.
- **Timeline categories**: hint "Hide a category to keep your timeline calmer. Its steps stay in your plan." Switches with swatches: Sleep, Light, Caffeine, Travel → `hidden_categories` (complete category-group list per patch). Category group → `BlockType`s is the RN `categoryStyles` mapping.
- Link row **Connectivity & reminders**, subtitle observed: "On, {{minutes}} min ahead · works offline" / "Off · works offline".
- Link row **Replay walkthrough**, "The short tour from your first trip". It replays without writing completion and returns to Preferences on Skip or Done. Each entry gets a fresh replay model (iOS bug, 2026-11-09).
- The Optional-items switch is removed. `show_optional_items` is never sent.
- **Rec (theme):** store the theme in DataStore (device-local, like iOS `cf.settings.theme`). Read it before the first frame: keep the `SplashScreen` `setKeepOnScreenCondition` until it is loaded, then apply it in the root `ChronoFlowTheme(darkTheme = …)`. Also call `AppCompatDelegate.setDefaultNightMode` so system dialogs and pickers match.

### 6.7 Plan & credits, paywall and cancel (B `SetPlanCredits` variants `credits|unlimited|offline|loading|nolink`, `SetPlan`, `SetPaywall`, `SetCancel`, `DarkSetPlan`; canvas `decBilling`; I 17h and fidelity)

- Data: `settings.planCredits {credits, unlimited}` and `billing.subscription {tier: free|monthly|annual, status, endsAt, renewsAt, canResume, canCancel, paymentFailed, unlimited}` (W `SubscriptionView`).
- **credits**:
  - Eyebrow "Your plan", "{{n}} credits left", "One credit plans one direction of a trip, so a return trip uses 2. Small time shifts are free."
  - Processing notice: "Payment processing." / "Your credit shows up here in a minute or two. You can leave this screen."
  - Link-allowed: [Buy credits or a plan] "Opens chronoflow.app in your browser, already signed in."
- **nolink**: "Credits and plans aren't sold in the app. Your balance updates here automatically."
- **unlimited**:
  - "Unlimited trips", badge **Cancelled** when cancelled, "Renews on {{date}} · yearly|monthly" or "Ends on {{date}}. After that, you can buy credits per trip."
  - Failed notice: "Your last payment failed." / "Update your payment method on the website to keep unlimited trips after {{date}}." (or the dateless variant).
  - Billing rows: Invoices and billing details, Update payment method ("Action needed" badge), Cancel subscription / Resume subscription, all "On the website".
- **offline**: billing rows disabled, "Needs a connection. Your plans and reminders still work."
- **loading**: skeletons, "Loading your plan".
- Hand-off sheets: **"Buy on chronoflow.app"** (3 steps, [Continue to the website] [Not now], "The sign-in link works once and expires after a few minutes.") and **"Cancel on chronoflow.app"** ([Continue to the website] [Keep unlimited trips]).
- **iOS**: cached balance, notices, the Cancelled badge, skeletons, pull to refresh, and NoLink for credits. **No** buy, billing, cancel or resume rows or sheets, because no hand-off endpoint exists.
- **Play policy** (from knowledge as of mid-2026; **verify before release**):
  - Google Play's Payments policy requires Play Billing for in-app digital goods. Steering to an external purchase is allowed only through specific programs: the EEA external-offers / alternative billing programs, and US changes after *Epic v. Google* (2025).
  - A credits/subscription service for trip plans is a digital good.
  - The safe design everywhere is the **NoLink** variant: show balance and status only, with no buy links, no prices and no "on the website" calls to action.
- **Rec:**
  - Android shows NoLink for credits and drops every website-steering row (same as iOS today).
  - For a failed payment, use the existing **website-free** catalog line "Update your payment method to keep unlimited trips." (already in `Settings.xcstrings`) instead of the "…on the website…" variant.
  - Wizard no-credits stays without a hand-off (iOS OQ 41).
  - Keep a build flag `BILLING_LINK_OUT=false` (RN had `EXPO_PUBLIC_BILLING_LINK_OUT`). **OQ-4.**

### 6.8 Connectivity & reminders page

See §7.4 (UI) and §7 (engine).

---

## 7. Reminders (local notifications)

### 7.1 Product rules (Fact, I `Docs/phase-19-reminders.md`, `Sources/Reminders/*.swift`; R `src/reminders/*`)

- Local notifications built from the **stored plan only** ("they also fire offline", canvas `decOffline`). They are never pushed by the server. The web has its own web push (`settings/push-devices`), which the app does not use.
- Settings live in `settings.reminders {light, avoid, caffeine, winddown, leadMin}`. Defaults are all true and 30 (`ReminderSettings.init`). They are written through the `reminders` outbox → `PATCH /v1/account/reminders {light, avoid, caffeine, winddown, lead_min ∈ 15|30|60}` (W `PreferenceRules`). The UI has **one master switch** that sets all four flags (RN behaviour kept).
- Kinds:
  - `seek_light` from block `seekLight`
  - `avoid_light` from `avoidLight`, except blocks whose reason has the "If you can" line
  - `caffeine_cutoff` from `noCaffeine`
  - `wind_down` from `sleep` blocks with `params.kind == main`
- Only `core` or `recommended` priority. Optional steps are removed first. Completed and deleted trips are excluded. Only trips with `keepOffline` and a stored current plan version count.
- **Timing** (`reminderDrafts`):
  - `send = max(start − leadMin, now)`; if `send` falls inside a sleep block, move it to that sleep's end.
  - `expires = min(end, start + 20 min)`; drop the draft when `send ≥ expires` or `expires ≤ now`.
  - Already-marked steps are dropped.
  - Drafts sorted by send time are **coalesced** when within **15 min** of the group's first send. A group is core if any member is.
  - Then priority-first, **cap 6 per local day** (the block-start day in its display zone).
- **Scheduling** (`reminderRequests`):
  - A 36 h lookback replay counts elapsed sends toward the day cap and suppresses members already sent; a ledger of delivered groups adds to this.
  - Schedule only `now+5 s < send ≤ now+48 h`, earliest **60** across all trips.
  - Title: `"{{from}} → {{to}}"` with city names (falling back to IATA).
  - Body: one sentence per distinct kind in start order, with the zone label appended when a member's zone differs from the group's:
    - "Seek bright light from {{time}}."
    - "Avoid bright light from {{time}}."
    - "Last caffeine before {{time}}."
    - "Wind down for bed: sleep from {{time}}."

    NL: "Zoek vanaf {{time}} fel licht." · "Vermijd vanaf {{time}} fel licht." · "Neem vóór {{time}} je laatste cafeïne." · "Kom tot rust: slapen vanaf {{time}}."
    Times use the account's 12/24 h format and the member's display zone.
- **Stable identifier**: `"chronoflow.reminder." + base64(accountId \n tripId \n inputHash \n memberKeys…)`. Unchanged requests are kept, changed ones replaced, stale ones removed.
- **Ledger**: private settings section `reminderLedger` `[{tripId, inputHash, sendAtMs, memberKeys}]`. Pulls never touch it; sign-out clears it.
- **Reschedule triggers**: stored settings/check/plan changes (DB observation), full sync, app foreground, significant time change, time-zone change, language change. Runs are serialised, and a newer request coalesces into one rerun.
- **Tap**: open the trip and its step sheet once the matching plan hash is loaded. A cold tap waits until launch readiness, which still applies the terms/verify/walkthrough gates. A tap that belongs to another account is ignored.
- **Permission is requested only** from "Turn on reminders" (Settings) or the first-trip sheet's "Turn on reminders". Never at launch or while scheduling.
- **Sign-out** cancels pending reminders and removes delivered ones before logout.

### 7.2 Android engine recommendation

**AlarmManager, not WorkManager, for delivery.** WorkManager one-time work is deferrable (Doze, standby buckets) and cannot promise "30 min before". Use it only for the reschedule job.

| Concern | Recommendation |
|---|---|
| Alarm type | Default `setAndAllowWhileIdle(RTC_WAKEUP, sendAt, pi)`: inexact, fires in Doze, and the OS limits it to about one per ~9 min while idle. The 15-min coalescing means our alarms are ≥15 min apart, so that limit never bites. If `AlarmManager.canScheduleExactAlarms()` is already true, use `setExactAndAllowWhileIdle`. |
| Exact-alarm permission | **Do not declare `USE_EXACT_ALARM`.** Play policy restricts it to apps whose core function is an alarm clock or calendar. **Do not declare `SCHEDULE_EXACT_ALARM`** for v1 either: on API 34+ it is denied by default for new installs, and asking for it needs a settings round-trip that has no board. **OQ-13** (default: inexact allow-while-idle; reminders have a 15/30/60 min lead and a 20 min expiry, so a few minutes of drift is acceptable). |
| Listing pending | AlarmManager cannot enumerate alarms. Keep a Room table `scheduled_reminder(identifier PK, request_code, send_at, title, body, trip_id, block_id, input_hash, member_keys, account_id)`. This is the Android `center.pending()`. Diff desired against stored rows, `cancel()` stale ones (matching `PendingIntent` request code + action), and set new or changed ones. Use the `requestCode` from a stable 31-bit hash of the identifier with collision checks. |
| Delivery | An exported=false `BroadcastReceiver` reads the row (so the content is correct even if the process was killed) and posts with `NotificationManagerCompat.notify(tag = identifier, id = requestCode, …)`. It then deletes the row and appends to the ledger in a `goAsync()` block. Content intent: an immutable `PendingIntent` to `MainActivity` carrying `tripId`, `blockId`, `inputHash` and `accountId`. Set `setAutoCancel(true)`, `setCategory(CATEGORY_REMINDER)`, and `setTimeoutAfter(expiresAt - now)`. The timeout is an Android-only nicety (it auto-removes the reminder after the step's expiry); **OQ-14** (default: yes). |
| Channel | Channel id **`reminders`**, name = localised "Reminders on this phone" (RN `notifications.ts:11-14`), `IMPORTANCE_DEFAULT`. Create it at startup (cheap; R did) and recreate it after a locale change so the name re-localises. |
| Reboot / time changes | Manifest receiver for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` and `LOCALE_CHANGED` (needs `RECEIVE_BOOT_COMPLETED`). It enqueues unique **expedited** work `reminders-reschedule` (`ExistingWorkPolicy.REPLACE`). Alarms are wiped at reboot, so recompute them from Room. DB files live in credential-encrypted storage, so wait for `BOOT_COMPLETED` (after unlock), not `LOCKED_BOOT_COMPLETED`. |
| Window refill | iOS refills its 48 h window only on foreground or sync, because iOS has no reliable background work. On Android, add a **periodic WorkManager job every ~12 h** that reruns scheduling. This is an Android improvement: reminders keep coming when the app isn't opened for days. **OQ-15** (default: yes). |
| Caps | Keep 48 h / 60 for parity, even though Android allows ~500 alarms per app. |
| Foreground | iOS shows a banner without sound or badge in the foreground. Android posts normally, and the channel decides sound. |
| Concurrency | One `ReminderScheduler` actor (a single-threaded coroutine scope with a `Mutex`) and a generation counter, exactly like `ReminderService` (re-entrancy coalescing, cancelled on account switch). |
| Port | Port `reminderDrafts` and `reminderRequests` as pure Kotlin over `java.time` (`ZoneId` for local days). Run the iOS parity vectors `Tests/RemindersTests/Fixtures/selection.json` in JVM unit tests. |

### 7.3 Permission model on Android

- `POST_NOTIFICATIONS` (API 33+) is a runtime permission. On API ≤32 notifications are on by default, so the **Ask** state never occurs: the status is `areNotificationsEnabled()` ? granted : blocked.
- **Status mapping** (adapting the R lesson `useNotificationPermission.ts:27-34`):
  - *granted* if the permission is granted **and** `areNotificationsEnabled()` **and** the `reminders` channel importance ≠ `NONE`.
  - *undetermined* if API 33+, not granted, and we have **never requested** it (DataStore flag `notificationPermissionRequested`).
  - Otherwise *blocked*.
  - Following iOS's "asks once" copy, a denial after our single request is treated as blocked, even though Android would allow one more prompt.
  - *unsupported* never happens on phones; it covers a missing `NotificationManager` only.
- Re-read the status on `ON_RESUME` of the reminders screen and app-wide on `ON_START`. "Come back here. Reminders switch on by themselves." holds: when we return granted with settings enabled, the switch shows on and scheduling reruns.
- **Open phone settings**: `Settings.ACTION_APP_NOTIFICATION_SETTINGS` with `EXTRA_APP_PACKAGE`. If only the channel is blocked, use `Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS` with `EXTRA_CHANNEL_ID="reminders"`.

### 7.4 Connectivity & reminders page (Fact, B `SetReminders` statuses `on|ask|blocked|unsupported`, `DarkSetReminders`; I Reminders catalog)

- Back → **Preferences**. Title **Connectivity & reminders** (Verbinding en herinneringen).
- Section **Reminders on this phone**: "A nudge before each step in your plan. Reminders are scheduled on this phone, so they arrive without a connection. Each device has its own setting."
- **on**: switch **Push reminders** (Pushherinneringen). When on: "Remind me ahead by" with options **15 min / 30 min / 1 hour**. iOS uses a native Picker; Android uses an M3 `SingleChoiceSegmentedButtonRow`, which the canvas allows ("on Android it takes our colours"), or the board's pills.
- **ask**: "Reminders are off. When you turn them on, your phone asks once whether ChronoFlow may send notifications." [Turn on reminders].
- **blocked**:
  - Warning "Notifications are blocked for ChronoFlow." / "Allow them in your phone's settings to get reminders."
  - Steps: 1 "Open your phone's Settings." 2 "Tap Notifications, then ChronoFlow." 3 "Turn on Allow notifications." 4 "Come back here. Reminders switch on by themselves."
  - [Open phone settings]. The switch shows dimmed.
  - Steps 1–3 describe iOS, not Android. **OQ-8.**
- **unsupported**: info "This device can't show reminders." / "You can still follow every step in your plan."
- Cards **Live Activity** ("… iPhone only.") and **Practice in-flight mode**: not built on iOS. Android hides both (Live Activity says "iPhone only").
- **First-trip sheet** (B `WizardReminders` → `WizardSheet`): "Turn on reminders?" / "Reminders are off. When you turn them on, your phone asks once whether ChronoFlow may send notifications." [Turn on reminders] [Not now]. Shown once after the first saved trip (I 16c/19). Reusable as an M3 `ModalBottomSheet`.
- **Inconsistency (Fact):** the copy says "Each device has its own setting", but the settings sync to the account through `PATCH /v1/account/reminders`, and iOS writes them through the outbox. **OQ-16** (default: follow iOS. The flags and lead time are account-level; the OS permission is per device. Keep the copy unchanged).

---

## 8. Add to calendar (CalendarExport, CalendarDenied, CalendarNone, CalendarFailed)

### 8.1 Board (Fact, B `CalendarExport.dc.html` states `ready|denied|nocalendar|failed`; canvas `reqExport`, `gapCalendar`)

- Sheet **"Add to calendar"**, subtitle "Amsterdam → Tokyo · 12–22 Oct".
- **What to add** (radio):
  - "Important and recommended" — "The steps your plan depends on" · "38 events"
  - "All steps" — "Also optional steps and notes" · "61 events"
  - "Flights only" — "Your 4 flights" · "4 events"
- **Calendar alerts** (segmented): None / At the start / 15 min before. "Separate from ChronoFlow reminders, which keep working either way."
- Row **Calendar · Personal** (choose the calendar).
- Info notice "A one-time copy": "Later changes to your plan don't update these events. Add the trip again after you edit it."
- Primary **"Add {{n}} events"**, then **Cancel**.
- Problem notices:
  - denied: "ChronoFlow can't use your calendar" / "You turned off calendar access. Allow it in your phone's settings to add your plan." [Open phone settings]
  - nocalendar: "No calendar to add to" / "There is no calendar on this phone that apps can add to. Turn one on in your phone's settings, then try again." [Open phone settings]
  - failed: "Couldn't add the events" / "Nothing was added to your calendar. Try again; if it keeps failing, restart the app." CTA "Try again"
- **iOS: not built** (OQ 42 default). **RN: built** with expo-calendar (`R src/plan/calendar-service.ts`, `calendar-events.ts`, `useCalendarExport.ts`).
- RN event mapping:
  - Filter by scope: *important* = actionable core/recommended plus all flight/transfer blocks; *flights* = flight; *all* = everything, though optional steps are hidden app-wide, so "All steps" now differs.
  - Sort by start.
  - Fields: `title` = the block's rendered text; start/end in UTC; `timeZone = displayTz`; alarms `[]`, `[0]` or `[-15 min]`; notes = note lines or `secondaryText` (rendered with the zone annotated).
  - Failed batch → delete the created events so a retry doesn't duplicate them.
  - Default calendar: iOS default or Android `isPrimary`, falling back to the first writable one.
  - Re-check permission when the app returns to the foreground while denied or empty.

### 8.2 Android recommendation (if OQ-1 turns it on)

- **Provider write, not the insert intent.** `Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)` adds **one** event through the calendar app's UI, which cannot add 38 events. A multi-event `.ics` through `ACTION_VIEW` is not reliably importable on Android.
- Permissions: `READ_CALENDAR` + `WRITE_CALENDAR`, requested **when the sheet opens** (the RN behaviour). There is no special Play policy for calendar permissions; declare them in the Data safety form only if data leaves the device (it doesn't).
- Writable calendars: query `CalendarContract.Calendars` with `CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR AND VISIBLE = 1 AND SYNC_EVENTS = 1`. Default `IS_PRIMARY = 1`, else the first. Show the picker as an in-app single-choice dialog (Android has no system calendar picker), listing `CALENDAR_DISPLAY_NAME` with `ACCOUNT_NAME` as supporting text.
- Insert with `ContentResolver.applyBatch(CalendarContract.AUTHORITY, ops)`:
  - per event, `Events` with `CALENDAR_ID`, `TITLE`, `DESCRIPTION`, `DTSTART`/`DTEND` (epoch ms), `EVENT_TIMEZONE = displayTz`, `HAS_ALARM`
  - plus a back-referenced `Reminders` row (`MINUTES` 0 or 15, `METHOD_ALERT`)
  - Batches are not transactional across providers. On exception, delete the URIs already returned (port the R rollback).
- States: denied, either permanently (`shouldShowRequestPermissionRationale` false after a request) or through the settings toggle → [Open phone settings] = `ACTION_APPLICATION_DETAILS_SETTINGS`. nocalendar (no writable calendar, e.g. no Google account) → `Settings.ACTION_ADD_ACCOUNT` (or `ACTION_SYNC_SETTINGS`). Re-query on resume.
- The no-plan state (TripActionsNoPlan) disables the row with "Available once the plan is made".

---

## 9. Live Activity → Android

### 9.1 Board (Fact, B `LiveActivity.dc.html`; canvas `reqLiveActivity`)

- An iOS-only board titled "iOS · Live Activity", "The step you're on, at a glance".
- Lock screen: "ChronoFlow · Amsterdam → Tokyo", "Day 2", swatch, **Avoid bright light**, "Sunglasses on, dim warm light", **45 min left**, a progress bar, "Next · Train to the hotel · 10:15".
- Dynamic Island compact, minimal and expanded ("Until 10:30 · Tokyo time").
- Rules:
  1. **Works offline**: built from the stored plan and advancing on schedule.
  2. **Shows one step**: the highest priority; ties go to the one that ends first.
  3. **Times in the local zone**: switching after a zone change.
  4. **Turn it off in Preferences**: separate from notifications.
- It starts with the first step of the plan day and ends after the last. Tapping it opens Today.
- **No Android board.** **iOS has not built it.**

### 9.2 Android equivalent (Rec; verify API names against the Android 16 SDK docs)

- The closest platform feature is **Android 16 "Live Updates"**: an ongoing notification that requests promotion (`NotificationCompat.Builder.setRequestPromotedOngoing(true)`, permission `android.permission.POST_PROMOTED_NOTIFICATIONS`, `NotificationManager.canPostPromotedNotifications()`), optionally `Notification.ProgressStyle` with segments and points. It shows as a status-bar chip (`setShortCriticalText`, e.g. "45m"), on the lock screen and at the top of the shade.
- Below API 36, fall back to a normal ongoing notification on its own low-importance channel `live` (no sound).
- **Offline updates without a running process**: set `setWhen(stepEnd)` + `setUsesChronometer(true)` + `setChronometerCountDown(true)` so the system counts down by itself. Use an `AlarmManager` alarm at each step boundary to rebuild the notification (next step, progress). Do **not** use a foreground service (it needs a matching FGS type, and none fits).
- Widgets (Glance) are an alternative, but no board covers them.
- **OQ-6** (default: **don't build in v1**, matching iOS; the SetReminders card says "iPhone only." and is hidden on Android). When it's wanted, ask the designer for an Android Live Update board (chip, shade and lock-screen rendering, light and dark).

---

## 10. Pull to refresh and sync feedback

### 10.1 Behaviour (Fact, I `plan.md` "Pull to refresh and sync feedback" 2026-11-09, rounds 1–5)

- Native pull to refresh on **Today**, **My trips** and the **trip page**, including the empty, loading and error states. Content is always scrollable.
- The pull calls `SyncEngine.refreshNow()` and **awaits** completion. Requests coalesce with in-flight syncs.
- A summary compares trip metadata, hashes or revisions and detail versions before and after: added, updated, removed, with each trip counted once. Receipt timestamps, cache downloads and eviction are ignored.
- **Feedback capsule** (user pulls only): an opaque `card`/`foreground` capsule with a `border`, in a **top inset below the navigation bar**, for about **2 s**, with no opacity animation. It is posted as an accessibility announcement and is one accessibility element (`refresh.feedback`).
- Copy: "Up to date" (Alles is bijgewerkt) · "{{n}} trips added/updated/removed" (plurals; e.g. "1 reis toegevoegd") · "Needs a connection" (Verbinding nodig) · "Something went wrong" (Er ging iets mis).
- Each refresh gets a new presentation ID, so an older timer cannot dismiss a newer capsule.

### 10.2 Android recommendation

- M3 `PullToRefreshBox` (material3 ≥1.3) around each screen's `LazyColumn`. Indicator colours from tokens. The `isRefreshing` state comes from the ViewModel, which awaits `syncEngine.refreshNow()`.
- Capsule: a composable overlay `Alignment.TopCenter` under the top app bar, with `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` for TalkBack (`View.announceForAccessibility` is deprecated on recent APIs). Keep it on screen for 2 s, keyed by presentation ID. Don't use a Snackbar: the DS puts toasts at the bottom, but iOS deliberately put this at the top.
- `SyncEngine`: coroutine-based with a 1 s debounce and exponential backoff `min(1s·2^n, 5 min)`, honouring Retry-After. Triggers: DB open or sign-in, `ON_START`, `ConnectivityManager.NetworkCallback` reconnect, outbox commit, pull. A periodic WorkManager sync is optional. Details belong to the sync/storage research doc.

---

## 11. API surface used by these screens (Fact, W `routes/api.php:46-113`; I `APIDataSource*.swift`)

Base URL: `https://api.chronoflow.net/v1` (I D20). Authentication is a Sanctum bearer token. Authenticated reads use ETag (`cache.headers:private;no_cache;etag`). Unless stated otherwise, every write below requires `auth:sanctum` and `throttle:api`, and most also require `verified`.

| Use | Method and path | Body | Response |
|---|---|---|---|
| Identity | `GET users/me` | — | identity |
| Account snapshot | `GET account` | — | `ApiAccountSnapshot {id,name,email,hasVerifiedEmail, settings{bodyClock, sleepAnswered, preferences{hiddenCategories,showOptionalItems,timeFormat,language}, reminders{light,avoid,caffeine,winddown,leadMin}, planCredits{credits,unlimited}, policies{termsVersion,privacyVersion,acceptedTermsVersion,acceptedTermsAt}}, readiness{variant,missing,exclusions,exclusionTarget,adjustments,invalidSections}, billing{subscription{tier,status,endsAt,renewsAt,canResume,canCancel,paymentFailed,unlimited}}, security{twoFactorEnabled,passwordRules,twoFactorConfirmedAt,passkeyCount}}` |
| Trips list | `GET trips` | — | `{trips:[TripListItem{card{…,canComplete,canReopen,canEdit,canDuplicate,canDelete}, planInputHash, planStartUtc, planStartZone, planEndUtc, planEndDate, preparationDays, adaptation?, keepOffline, detailVersion?, checkState?}]}` |
| Trip detail | `GET trips/{uuid}` | — | `{trip, plan, notices, timeline, checkState}` |
| Check-off | `PUT trips/{uuid}/checks` | `{plan_input_hash, type, local_date, ordinal, status: done|skipped|null}` | new check state |
| Body clock | `PATCH account/body-clock` | partial snake_case (§6.5) | 200 snapshot |
| Preferences | `PATCH account/preferences` | `{time_format?, hidden_categories?, show_optional_items?}` | 200 snapshot |
| Reminders | `PATCH account/reminders` | `{light?,avoid?,caffeine?,winddown?,lead_min?}` (at least one) | 200 snapshot |
| Profile | `PATCH users/me` | `{name}` or `{email, current_password}` (`throttle:api-credential`, no `verified`) | identity |
| Terms | `POST terms/acceptance` | `{accepted:true, version}` | 204 |
| Walkthrough | `POST walkthrough` | — | 204 |
| Password | `PUT account/password` | `{current_password,password,password_confirmation}` | 204 |
| 2FA | `POST account/two-factor` · `POST …/confirmation {code}` · `DELETE account/two-factor` · `POST …/recovery-codes` · `POST …/recovery-codes/reveal` | `current_password` (except confirmation) | setup / codes / snapshot |
| Passkeys | `GET account/passkeys` · `POST account/passkeys/options` · `POST account/passkeys` · `DELETE account/passkeys/{id}` | see §6.4 | |
| Delete account | `DELETE account` | `{password}` | 204; 422 `billing` |
| Forgot (signed in) | `POST auth/forgot-password` | `{email}` | |
| Logout | `POST auth/logout` | — | 204 (3 s timeout) |

**Not in `/v1`** (web only): trip delete/complete/duplicate (the app duplicates through the wizard), `calendar.ics`, the calendar feed, `plan.csv`, `settings/data.json`, billing checkout/portal/cancel/resume, push devices, and setting the account language.

Outbox kinds (I `AppCore/OutboxChange.swift:42-56`): `check-off`, `settings(bodyClock|preferences|reminders)`, `profile`, `policies`, `walkthrough`. `complete-trip`, `data-export` and `security` are decoded but **unsupported**: never acknowledged, never produced.

---

## 12. Copy and localisation pipeline (Rec)

1. Converter script (Python, in the Android repo's `tools/`). It reads the iOS `.xcstrings` (read-only), emits `values/strings.xml` and `values-nl/strings.xml`, and keeps a key map `english sentence → resource name` (slug plus short hash) as a committed JSON for traceability.
   - Placeholders: `{{name}}` → `%1$s` in order, `%lld` → `%1$d`, `%@` → `%1$s`.
   - Plural variations → `<plurals>` (en one/other; nl one/other).
   - Escape `'`, `"`, `@`, `?`, `%`, `&`, `<`.
   - Fail on a missing `nl`, like iOS's test.
2. Android-only strings (OQ-7, OQ-8) live in a separate `strings_android.xml`. Each needs Dutch that a person writes.
3. Notification bodies are built at schedule time with the **chosen app locale** (`createConfigurationContext`) rather than the system locale, so they follow the in-app override.
4. Dates and times: use `java.time` with `DateTimeFormatter.ofPattern` per locale. Keep the iOS/RN rule that the 12-hour form is built from the account's `time_format`, not the system 24-hour setting. The phone hour cycle applies only where iOS uses `Locale.hourCycle`.

---

## 13. Performance notes for these screens (Rec)

- All screens read Room `Flow`s mapped to immutable UI models off the main thread (`flowOn(Dispatchers.Default)`), then `stateIn(viewModelScope, WhileSubscribed(5_000))`. Composables receive `@Immutable` models and stable lambdas.
- Minute ticks feed a per-text `derivedStateOf`. The sync line ticks only while Settings is visible (`repeatOnLifecycle(STARTED)`).
- Settings pages are short: plain `Column` + `verticalScroll` is fine. Body clock is long, so use a `LazyColumn` (which also suits scroll-to-section).
- Reminder scheduling and calendar writes run only in the background (`Dispatchers.IO` / WorkManager). Never block launch with them: iOS starts its reminder service after the splash overlay commits.
- Baseline Profiles should cover Today → My trips → Settings → Body clock navigation.

---

## 14. Open questions (each with the default the plan proceeds on)

| # | Question | Default |
|---|---|---|
| OQ-1 | Build **Add to calendar** on Android (boards exist, RN had it), even though iOS omits it (OQ 42)? | **No**: omit the row as iOS does. §8.2 is ready if the user says yes. |
| OQ-2 | **Mark as completed / Delete trip**: no `/v1` endpoints. | Omit, as iOS does. Build the alert copy behind a flag. |
| OQ-3 | **Your data & policies / Download my data**: no bearer export endpoint. | Omit the data row (Account subtitle "Profile, language"), as iOS does. Policy rows also stay out (iOS shows none). |
| OQ-4 | **Billing on Android** under Play policy. | NoLink everywhere. No buy, cancel or website-steering rows. Use the website-free failed-payment line. Build flag `BILLING_LINK_OUT=false`. Check Play's then-current US/EEA programs before any link-out. |
| OQ-5 | **Email change** in Profile (the server supports it; iOS defers it). | Defer, as iOS does: email read-only, "Save changes" for the name. |
| OQ-6 | **Live Activity** equivalent (Android 16 Live Updates). | Don't build in v1. Hide the card. Ask for an Android board later. |
| OQ-7 | iOS-specific copy: 2FA b1 "…like the one built into Passwords on iPhone." and Passkeys "Sign in with **Face ID**, a fingerprint or your screen lock…". | Android variants: "You need an authenticator app." / "Je hebt een authenticator-app nodig." and "Sign in with your fingerprint, face or screen lock instead of your password." / "Log in met je vingerafdruk, gezicht of schermvergrendeling in plaats van je wachtwoord." Flag both for the user's approval. |
| OQ-8 | Blocked-reminders steps are iOS paths ("Tap Notifications, then ChronoFlow", "Turn on Allow notifications"). | Android: keep step 4 and [Open phone settings], which goes straight to the app's notification page. Replace steps 1–3 with one step: "Tap Open phone settings and turn on notifications for ChronoFlow." / "Tik op Telefooninstellingen openen en zet meldingen voor ChronoFlow aan." Needs approval. |
| OQ-9 | Selected tab colour: "amber" (canvas) or `primary` (iOS: indigo light, dawn dark). | `primary`, as iOS does. |
| OQ-10 | Large titles in Fraunces (boards) or the system font (iOS decision 2026-11-10)? | System font, M3 `LargeTopAppBar`. |
| OQ-11 | Wide Settings layout (only 2FA has wide boards; iOS has none yet). | A 560 dp centred column on every Settings page at ≥600 dp. The tab bar becomes a rail through `NavigationSuiteScaffold`. |
| OQ-12 | `FLAG_SECURE` on 2FA setup and recovery-code screens? | Yes. |
| OQ-13 | Exact alarms (`SCHEDULE_EXACT_ALARM`)? | No. Use `setAndAllowWhileIdle`, and exact only when it's already allowed. Never `USE_EXACT_ALARM`. |
| OQ-14 | Auto-remove a delivered reminder at its expiry (`setTimeoutAfter`)? | Yes (Android-only nicety; harmless). |
| OQ-15 | Periodic background refill of the 48 h reminder window (iOS can't)? | Yes, a 12 h periodic WorkManager job. |
| OQ-16 | "Each device has its own setting" vs. reminder flags synced to the account. | Follow iOS: flags and lead time are account-level through the outbox; permission is per device. The copy stays. |
| OQ-17 | The future-Today board promises "we'll remind you the evening before"; iOS dropped it. | Drop it, as iOS does. |
| OQ-18 | The language override is device-local; the account `language` (used by email and web) can't be set from mobile. | Device-local through `AppCompatDelegate.setApplicationLocales`. Don't change the server. Note for the backend. |
| OQ-19 | Delete-trip copy is the board's sample copy (canvas `gapSheets`, "web wording pending"). | Only relevant if OQ-2 changes. Use the board copy. |

---

## 15. Risks

1. **Play billing policy**. The board copy ("Buy on chronoflow.app", "on the website") would breach Google Play's Payments policy outside the special programs. A rejection risk if the link-out variant ships by mistake. Mitigation: NoLink-only Android build and a lint/test check that no `chronoflow.app` billing URL is present.
2. **Reminder timing drift**. Without exact alarms, Doze can delay delivery by minutes, and OEM battery killers (Xiaomi, Huawei, Samsung "sleeping apps") can suppress alarms. Mitigation: allow-while-idle alarms, the 12 h refill job, and a Samsung/Xiaomi device check. No board covers a battery-optimisation prompt; don't add one.
3. **No pending-alarm API**. Our Room mirror can drift from the OS (app data cleared, force-stop cancels alarms). Mitigation: reschedule unconditionally on every `ON_START`, boot and package replace; force-stop recovers at next launch.
4. **Permission-state heuristics on Android 13+** (`POST_NOTIFICATIONS`). They differ from iOS's tri-state. RN already hit "denied before the first prompt". Mitigation: the persisted `requested` flag and the channel-importance check.
5. **Large-screen orientation**. API 36 ignores the portrait lock on ≥600 dp, so every screen here needs landscape-safe layouts, which iOS never had to test.
6. **Unverified or undeployed server pieces**: passkey management (Phase 20 branch), assetlinks.json for Credential Manager, signed-in forgot-password behaviour. Mitigation: mock source first, then a live check against the local Valet server as on iOS.
7. **Scope drift from boards**. Many boards (calendar, delete, billing, data, Live Activity, practice mode) look buildable but are deliberately omitted on iOS. Building them would break "behaves exactly like iOS". The §1.2 table should be the gate in the Android plan.
8. **Converted strings**. Generated resource names are opaque. Placeholder reordering errors in Dutch (positional args) are possible. Mitigation: converter tests with argument-count checks and a lint rule that bans hard-coded strings.
