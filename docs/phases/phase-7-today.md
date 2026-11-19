# Phase 7: Today

- **Branch:** `feat/phase-7-today`, from the stack top once **6d** (step sheet, check-offs, legend, trip actions) is in it. Through 6d it also relies on 6a (shell, My trips, trip cards, pull to refresh and its capsule), 6b/6c (trip page, `CategorySwatch`), 4d (repositories, outbox), 4e (graphs, routes, `RootNavigator`, seeds), 3b (launch hand-off, `ReportDrawnWhen` in `CfRoot`), 2 (primitives), 1a/1d (PlanCore port, `PlanClock`, `SystemPlanClock`), 0c (strings) and 0e (profiles, guards, captures). If 6d is missing, stop and report it.
- **Wave 12:** it can run beside 6e, 9f, 10b and 12b in its own worktree. Keep edits to shared files (`plan.md`, `Makefile`, `app/…/AppGraph.kt`, `SessionGraph.kt`, the nav entry provider, `stability_config.conf`, `docs/perf-startup.md`) small and on their own lines.
- **Plan entry:** `plan.md` "### Phase 7: Today" (l.2499–2530). Also read A3 (modules, Konsist rules), A8 (navigation), A9 (design-system mapping), A10 (window policy), A12 (clock reader rule), A21 (performance), "Android spec: window policy" (row "Today, My trips": cards max 640 dp, centred), "Android spec: process, activity and launch sequence" step 6 (TTFD), "Performance budget" (TTFD row), standard acceptance #1–7 (l.663–676).
- **Tests:** every test follows `AGENTS.md` "Tests" (cheapest tier, budgets, exact queries, no `Thread.sleep`, hang caps). Tier annotations are 0e's: Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`.
- **Tags:** **[F]** fact read in source (path given). **[R]** rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` at `f391a1fa` (read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`.
- **Paths:** `…/today/` = `feature/today/src/main/kotlin/net/chronoflow/feature/today/`; `…/plan/` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/`; `…/ui/` = `core/ui/src/main/kotlin/net/chronoflow/core/ui/`; `…/data/` = `core/data/src/main/kotlin/net/chronoflow/core/data/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`.
- **Checked 2026-11-16** against iOS `f391a1fa`, the `Today*` boards and `bundle.js`, web `e36368ec`, `plan.md`, `AGENTS.md` and the 6a/6b/6d briefs. Corrected: 6a already ports `Today.kt` (`todayView`, `calendarDaysUntil`, two `TodayTests` cases, OQ-6a-13), so Task 2 only adds the selection; the 640 dp constant is 6a's `WindowPolicy.listCardMaxWidthDp`; `CategorySwatch`, `MetaChip` and the refresh capsule are already in `:core:ui`, and the step sheet is 6d's `StepSheetContent` + `stepSheetState`; the hero card is 6a's `TripCard(TripCardUi)`; the detail flow is `session.trips.detailValues` (6b's `TripPageSource` is in `:feature:trip` and cannot be used); `DbTodaySource` lives in `:app`; the sheet and check-off follow the **selection's** trip (iOS `home.details[selection.tripID]`); trips decode only on record changes; list spacing per item instead of one `spacedBy`; future plan-start fallbacks; the pill padding; parameterised layout tests; the flight-number resource name.
- **Stack check (2026-11-19, against 160be918, the 6e top).** Every Task 0 row exists. Deltas:
  - `TripCardUi` is declared in `feature/trips/…/trips/TripsUiState.kt:25` (with `NextStepUi`, l.43). The pure card builder is `feature/trips/…/trips/TripCardFormatting.kt`, which builds `NextLabel.Now` / `NextLabel.In(formatter.formatMinutes(…))` (l.35–37). `TripCard` itself is in `feature/trips/…/trips/TripCard.kt:54`.
  - `PlanDecoder` (`core/plan/…/plan/PlanDecoder.kt`): `trips(…)` l.40, `detail(record: PlanVersionRecord): TripDetail` l.81, `detail(snapshot: TripPlanSnapshot): TripDetail?` l.99 (nullable).
  - `session.trips.detailValues(tripId)` is `TripRepository.detailValues` (`core/data/…/repository/TripRepository.kt:62`). `SettingsRepository.planPreferences(): Flow<PlanPreferences>` is at `core/data/…/repository/SettingsRepository.kt:26`.
  - 6d's check-off write is `TripPageViewModel.setCheck` (l.441), which calls the private `submitCheck(blockId, status, fromAction)` (l.455), with the busy guard and the foreign-vs-own cancellation rule. Extract `CheckOffWriter` from `submitCheck`'s enqueue path only. The busy flag, the refusal feedback and the cancellation handling stay in each ViewModel, and the trip page's behaviour must not change (its JVM tests `StepCheckTest` etc. stay green unchanged except for construction).
  - `WindowPolicy` is `core/designsystem/…/designsystem/layout/WindowPolicy.kt` (`wideBreakpointDp = 600`, `listCardMaxWidthDp = 640`). There is no `ds/layout` package.
  - Tab navigation is `ShellNav`/`ShellNavState` in `core/navigation/…/navigation/shell/` (`ShellNav.select(…)` l.16; `ShellTab.Today -> Route.Today`). The per-tab holder is in `app/…/app/shell/AppShellScaffold.kt`. 6e added `LocalWindowSize` (window, not container) for wide decisions.
  - `:feature:today` exists as a module with only its `build.gradle.kts`; there are no sources yet.
  - `TodayTests` already has three cases (`todaySeparatesCurrentAndFourUpcomingAndDoesNotHideTheNextImportantStep`, `calendarDaysUseThePlanZoneAcrossDaylightSaving`, `nextStepIgnoresHiddenCategories`).
  - **Test rule correction (AGENTS.md "Tests" 1: never assert copy on screen):** `TripCardEyebrowTest` must not read on-screen text. Make the eyebrow a pure function, for example `nextEyebrow(startUtc, now): NextLabel` (`Now` or `In(minutes)`), and test it as a JVM test with the three cases (30 min, 1 ms → 1 min, at the start → now). Add a Robolectric invalidation check that a minute tick recomposes only the eyebrow leaf, not the card (probe or recomposition counter, structural). Apply the same rule to any other test in this brief that names visible copy: assert the state or label model, never the rendered string.
  - `PlanClock` has only `fun values(): Flow<Instant>`; there is no `clock.now()`. Decode records using the latest shared tick (`ticks.first()`), keeping decoding dependent only on record emissions. This preserves 1a/1d's clock contract and avoids a second receiver.
  - `TripCardUi`, `AirportUi` and `NextStepUi` carry Compose `@Immutable`; the pure `tripCardUi` builder returns those Android models and accepts feature-local `TripsPreferences`. Move the models and builder together to `:core:ui` `…ui.trip`, accepting the existing core `PlanPreferences` instead. A JVM `:core:plan` builder cannot depend on `:core:ui`. My trips maps its existing preferences at the call site; its preference behaviour stays unchanged.
  - `TripPageViewModel` receives `enqueueCheck: suspend (CheckOffChange) -> Unit`; it does not own an `OutboxRepository`. Preserve that dependency and its existing tests. `CheckOffWriter` accepts an enqueue function as well as the repository/permit constructor, so the trip page can delegate the enqueue path without moving its busy, refusal or cancellation behaviour.
  - `plan.md` standard acceptance #5 supersedes the exhaustive guard matrix below: geometry guards run each state at `small`/`largeFont` in Dutch and `pixel8` in English, light only, with the wide cases at `foldInner`/`landscapeSideNav`. Captures retain both themes and languages.
  - `Scripts/targeted.py` selectors omit the Gradle-style leading colon (`core:plan:TodayTests`, not `:core:plan:TodayTests`); otherwise its module path becomes absolute. All targeted commands below use the landed selector syntax.
  - `AppDispatchers` is declared in `:app`, so a feature cannot take that type. Task 3 injects its `default` and `main` values as `CoroutineDispatcher` parameters via `TodayDependenciesKey`; `DbTodaySource` takes its IO dispatcher and mutation-permit provider from app wiring. No earlier dispatcher type moves or changes.
  - `CategorySwatch` takes `(type, modifier = Modifier, size = Regular)`, so Today uses named `size`/`modifier` arguments. Captures use the requested 390 × 844 board frame via a test-only copy of `pixel8`; the landed `pixel8` guard profile itself remains 411 × 914.
  - OQ rows in `plan.md` end at OQ-A323; this phase's open questions start at OQ-A324.
- **Names from earlier phases.** This brief uses the plan's and the iOS names (`TripListValue`, `PlanDecoder.trips`, `TripCard`/`TripCardUi`, `CategorySwatch`, `StepSheetContent`/`stepSheetState`, `CfPullToRefreshBox`, `RootNavigator`, `OutboxRepository.enqueue`, `FakePlanClock`, …). Before coding, open the merged code of 1a, 1d, 2, 4d, 4e, 6a–6d and use **their exact names and signatures**. Where a name differs, keep theirs and note it in the final commit body. Do not rename earlier code except where Task 1 says to move it.

---

## 1. Goal

The Today tab replaces 6a's debug seam. It behaves exactly like iOS 17b (`Kit/PlanUI/Today/TodayScreen.swift`, `Kit/PlanCore/Today.swift`) and looks like the `Today*` boards:

1. **Five states:** loading, empty, completed (only completed trips), future (the selected trip is upcoming) and active (the hero card plus the day panel).
2. **Logic ported 1:1 to `:core:plan`:** `selectTodayTrip` and `lastEndedTrip` are new here; `todayView` (current steps, the next four steps, the next step, `minutesUntilNext`, hidden flags, before/after plan, day progress) and `calendarDaysUntil` were ported by 6a (OQ-6a-13) and are verified, not re-ported. The three iOS `TodayTests` cases keep their names.
3. **Rows** keep done and skipped steps in place (`TodayRowStates`). Tapping any row opens the **step sheet** (6d's) over Today, where Mark as done / Skip / Undo write through the outbox (`OutboxRepository.enqueue(CheckOff)`). Tapping the hero card opens the trip page (`TripPage(tripId)`) on the Today back stack. See OQ-7-1 for why rows open the sheet, not the trip page.
4. **Performance:** the ViewModel builds a structural state off the main thread that does not change on a quiet minute tick; the minute clock is read **only** in leaf composables (relative-time texts). A probe test proves a minute tick recomposes only those leaves.
5. **Wide windows:** content is one column, at most 640 dp wide, centred.
6. **TTFD:** `reportFullyDrawn()` fires after the first frame of Today content on a signed-in launch, measured and recorded.

---

## 2. Inputs to read (in this order)

1. **This repo:** `AGENTS.md` (all of "Working rules", "Performance", "Tests", "Traps"); `plan.md` sections listed above; `docs/phases/phase-1a-plan-core.md` (Task 6 `tripDisplayStatus`, `withoutOptionalSteps`, Task 7 `PlanClock`/`FakePlanClock`); `docs/phases/phase-1d-timeline-host.md` (Task 1 `SystemPlanClock`, Task 3 `TimelineProbe` pattern, which Task 6 here copies); `docs/phases/phase-2-design-system.md` (tasks 7, 11, 13, 14, 15, 17: `cfClickable`, `CfButton`, `CfCard`, `CfBadge`, `CfSkeleton`, `CfLargeTopAppBar`, `CfPullToRefreshBox`, `CfLinearProgress`, `@BoardMetrics`); `docs/phases/phase-0c-strings.md` (resource names, `CopyIndex.find(CopyTable.Screens, key)`); `docs/phases/phase-3b-launch-readiness.md` §5.8 (`CfRoot`, `ReportDrawnWhen`) and Task 8; `docs/phases/phase-4d-sync.md` Task 3 (`OutboxRepository.enqueue`, CheckOff); `docs/phases/phase-6a-shell-trips.md` §5.2 (`Today.kt`, OQ-6a-13), the TripsViewModel pipeline and "Trip card" (`TripCard(card: TripCardUi, onClick)`), §5.5 (`RefreshFeedbackCapsule`), §6 (module table); `docs/phases/phase-6b-trip-page.md` Task 1 (`DbTripPageSource` in `:app`: `session.trips.detailValues`, `session.settings.planPreferences()`); `docs/phases/phase-6d-trip-interactions.md` §4 module table, Task 4 (`StepSheetContent`, `StepGoneContent`, `RefusedChangeFeedback`) and `stepSheetState(…)` in `:core:plan` `…plan.step`, Task 6 (`TripPageViewModel.setCheck`).
2. **Research:** `docs/research/07-day-to-day-settings.md` §3 (Today: boards, behaviour, copy, recommendation), §10 (pull-to-refresh capsule), §13 (performance notes).
3. **Boards** (`DC/`, read-only; open in a browser to render):

   | Board | What it is |
   |---|---|
   | `Today.dc.html` | The master board, prop `state` = `active` (default) \| `empty` \| `future` \| `loading` \| `completed`, prop `theme` = `light` \| `dark`, frame 390 × 844. Uses `ChronoFlow.TodayPanel`, `Badge`, `MetaChip`, `CategorySwatch`, `Skeleton`, `StatusPill`, `DayProgress` from `DC/ds/chronoflow/components/bundle.js` (functions `Na` = TodayPanel, `ie` = TodayRow, `xe` = StatusPill, `Le` = DayProgress, `ve` = CategorySwatch, `re` = Badge, `Re` = MetaChip, `va` = Skeleton). |
   | `TodayEmpty.dc.html` | `<dc-import name="Today" state="empty">` |
   | `TodayFuture.dc.html` | `<dc-import name="Today" state="future">` |
   | `TodayLoading.dc.html` | `<dc-import name="Today" state="loading">` |
   | `TodayCompleted.dc.html` | `<dc-import name="Today" state="completed">` |
   | `TodayRowStates.dc.html` | Row states (to do, now, done, skipped) in context and alone, 390 × 1500; its headings and notes are documentation, not app copy. |

4. **iOS references** (`I/` at `f391a1fa`). Behaviour, copy keys, test ids and test names come from these:
   - `plan.md:1920` "17b: Today" (and the two bullets under it: implementation note 2026-11-08 and "Review round 1").
   - `plan.md:1911` "17a: Step sheet, check-off and legend" (the sheet Today opens).
   - `Kit/PlanCore/Today.swift` (54 lines): `TodayPlan`, `todayView`, `calendarDaysUntil`.
   - `Kit/PlanUI/Today/TodaySelection.swift` (26 lines): `selectTodayTrip`, `lastEndedTrip`.
   - `Kit/PlanUI/Today/TodayScreen.swift` (249 lines): state order, empty, completed, future note, panel, headings, hidden link, `PlanLoadingView`.
   - `Kit/PlanUI/Today/TodayStepRow.swift` (94 lines): row text, marked state, accessibility label, test id.
   - `Kit/PlanUI/Today/TodayPanelComponents.swift` (120 lines): `DayStatusPill`, `DayProgressRow`.
   - `Kit/PlanUI/Today/TodayFutureTripCard.swift` (83 lines): the future hero.
   - `Kit/PlanUI/Components/TripSummaryCard.swift` (124 lines): the active hero (same card as My trips).
   - `Kit/PlanUI/Models/PlanHomeModel.swift` (95 lines): inputs (`synced`, `hidden`, `timeFormat`), hero selection, `setCheck` busy guard.
   - `Kit/PlanUI/Models/TripPlanModel.swift:132–141` (`setCheck` → `OutboxRepository.enqueue(.checkOff(…))`).
   - `Kit/PlanCore/PlanLogic.swift` (`isOptionalStep`, `withoutOptionalSteps`, `tripDisplayStatus`, `dayLabelKey`, `dayStatusLabelKey`).
   - `I/App/AppTabs.swift:398–463` (Today wiring: `openTrip` pushes `TripPageRoute` on the Today path, `openStep` presents `StepSheet` for the hero trip, `settings` selects the Settings tab, `pastTrips` selects the Trips tab, `create` opens the wizard, refresh with feedback).
   - Tests: `Packages/ChronoFlowKit/Tests/PlanUITests/TodayTests.swift` (3 cases), `I/AppTests/TodaySnapshotTests.swift` (fixture states), `I/AppTests/TodayLayoutTests.swift`, `I/AppTests/TodayControlTests.swift`, `I/AppTests/TodayRowSnapshotTests.swift`, `I/AppTests/PlanScreenFixture.swift` (`now = 2026-11-08T10:00:00Z`), `I/AppUITests/TodayTests.swift`.
5. **Reference screenshots:** `docs/research/screens/ios/02-today-en-3s.png`, `02-today-en-7s.png`, `21-today-AppleInterfaceStyle-ignored-light.png`, `22-today-nl.png`.
6. **API** (web at `e36368ec`, `routes/api.php`). Today makes **no network call itself**. It reads the local store that 4d fills, and its check-offs reach the server through the outbox flush:
   - `GET /v1/trips` (`trips.index`, l.99) and `GET /v1/trips/{trip}` (`trips.show`, l.100) feed `trips` and `plan_versions`.
   - `PUT /v1/trips/{trip}/checks` (`trips.checks.update`, l.91), body `{plan_input_hash, type, local_date, ordinal, status: "done"|"skipped"|null}`, sent by the 4d outbox for each queued `CheckOffChange`.

---

## 3. Out of scope

| Item | Owner / reason |
|---|---|
| The step sheet itself (content, Done/Skip/Undo, step-gone state, refused-change Snackbar, TalkBack custom actions on trip-page rows) | 6d. Today **hosts** it (Task 5). |
| The trip page and its opening position | 6b–6e. Today only pushes `TripPage(tripId)`. |
| Settings › Preferences page | 10b. Today's hidden-steps link selects the Settings **tab** (iOS `selection = .settings`). |
| The wizard | 9c. "Plan a trip" opens 6a's wizard seam (`Wizard(Create)` route). |
| "Notes for today" section of `ChronoFlow.TodayPanel` (`notes` prop) | Not on the `Today` board (`notes` not passed) and not on iOS. Not built. |
| The 44 dp glass icon at the top left of the `Today` board's nav bar | iOS has no Today toolbar item (`TodayScreen.swift` sets only `navigationTitle`). Not built (OQ-7-4). |
| Reminder promise "we'll remind you the evening before" (board future note) | iOS drops it (17b note; `07 §3.2`). Not built. |
| Legend entry on Today | iOS: "the legend entry remains on the future trip page … no invented Today toolbar action" (17a note). Not built. |
| Trip complete, delete, calendar | A24. |
| Haptics on check-off | None (6d, iOS). |

---

## 4. Facts the tasks rely on

### 4.1 Logic [F `Kit/PlanCore/Today.swift`, `Kit/PlanUI/Today/TodaySelection.swift`, `Kit/PlanCore/PlanLogic.swift:19–24`]

```swift
// Today.swift:18–45
public func todayView(_ detail: TripDetail, marks: [CheckMark], now: Date, hidden: [BlockType] = []) -> TodayPlan {
    let plan = withoutOptionalSteps(detail.plan)
    let state = buildCheckState(plan, marks: marks)
    let marked = Set(marks.map { checkIdentity($0.key) })
    let excluded = Set(hidden + [.lightOK, .lightNeutral])
    let blocks = plan.renderableBlocks.filter { !excluded.contains($0.type) }
    let day = plan.days.first { planDate($0.startUtc) <= now && now < planDate($0.endUtc) }
    let next = plan.blocks.first { block in
        block.actionable && block.checkKey.map { !marked.contains(checkIdentity($0)) } == true
            && planDate(block.endUtc) > now
    }
    let categories: [BlockType] = [ .sleep, .nap, .rest, .seekLight, .lightOK, .lightNeutral, .avoidLight,
        .caffeineServing, .caffeineRoutine, .caffeineOK, .noCaffeine, .flight, .layover, .transfer, .movementBreak,
        .zoneChange, .note ]
    return TodayPlan(
        currentDay: day,
        currentSteps: blocks.filter { planDate($0.startUtc) <= now && now < planDate($0.endUtc) },
        upcomingSteps: Array(blocks.filter { planDate($0.startUtc) > now }.prefix(4)),
        marks: state.marks, dayProgress: day.flatMap { day in state.days.first { $0.dayId == day.id } },
        allCategoriesHidden: categories.allSatisfy(hidden.contains),
        importantHidden: detail.plan.blocks.contains { $0.priority == .core && hidden.contains($0.type) },
        nextStep: next,
        minutesUntilNext: next.map { max(0, Int(ceil(planDate($0.startUtc).timeIntervalSince(now) / 60))) } ?? 0,
        beforePlan: now < planDate(plan.days.first?.startUtc ?? detail.trip.departureUtc),
        afterPlan: now >= planDate(plan.days.last?.endUtc ?? detail.trip.endUtc))
}
```

Points that are easy to get wrong:
- `next` searches `plan.blocks` (optional steps removed) **without** the hidden filter, so a hidden category never hides the next important step. Any mark (done **or** skipped) excludes a block.
- `importantHidden` reads `detail.plan.blocks` (optional steps **kept**).
- `currentSteps` and `upcomingSteps` keep marked blocks. That is what keeps done and skipped steps in place.
- `upcomingSteps` takes the first four in **plan order** after filtering, not re-sorted.
- `allCategoriesHidden` needs all 17 listed types in `hidden`.

`calendarDaysUntil(at, now, zone)` (`Today.swift:47–54`): Gregorian calendar in `zone` (fallback GMT when the id is invalid), whole days from start-of-day(`now`) to start-of-day(`at`), floored at 0.

`selectTodayTrip(trips, now)` (`TodaySelection.swift:5–14`): drop trips whose status is `completed`, where status = `tripDisplayStatus(card, planStartUtc, planEndUtc, now)` (re-derived, not the stored one). Sort: active before upcoming; among active, the earlier `planEndUtc ?: card.arrivalUtc` first; among upcoming, the earlier `planStartUtc ?: card.departureUtc` first; equal instants → smaller `card.id` first. Take the first.

`lastEndedTrip(trips, now)` (`:16–22`): only trips whose status is `completed`; sort by `min(planDate(planEndUtc ?: card.arrivalUtc), now)` **descending**; equal → smaller `card.id` first.

`tripDisplayStatus` (1a, `PlanLogic.swift:19–24`): the server's `completed` is kept, a missing plan bound keeps the server status; otherwise `now ≥ planEnd` → completed, `planStart ≤ now` → active, else upcoming.

### 4.2 Screen behaviour [F `TodayScreen.swift`, `AppTabs.swift:429–463`, `PlanHomeModel.swift`]

State order (first match wins):
1. **Loading** when `!synced || !tripsLoaded || formatter == null`. `synced` = `account?.syncedAt != null` (`PlanHomeModel.swift:42`); `tripsLoaded` = the trips flow has emitted once; the formatter is the `CopyLineFormatter` (iOS shows the loading wrapper until it exists, `AppTabs.swift:438–459`).
2. **Empty** when the decoded trip list is empty (tombstoned and locally deleted trips are already removed by `PlanDecoder.trips`).
3. **Hero** when `selectTodayTrip(trips, now)` returns a trip. Its `hero.status` decides (iOS `TodayScreen.swift:57,69` reads the stored `TripListValue.status`; `PlanDecoder.trips` sets it with `tripDisplayStatus` at decode time [F `PlanDecoder.swift`], and the ViewModel re-derives it each tick, so it equals the re-derived status in the app; a test fixture that sets `status` by hand, like iOS `todayFixture`, gets that status):
   - `upcoming` → future hero card (`TodayFutureTripCard`) + the "Nothing to do today" note card;
   - otherwise (active) → `TripSummaryCard` hero + the panel, when the hero's detail exists. With no detail (plan not on the phone, or it failed to decode) only the hero card shows (iOS `if let view { panel(view) }`).
4. **Completed** when `lastEndedTrip(trips, now)` returns a trip.

Inputs [F `PlanHomeModel.swift:46–58`]: settings section `preferences`: `hiddenCategories` (array of `BlockType` raw strings, missing → `[]`) and `timeFormat` (`"24"` default). The hero's detail = the trip's current plan with its marks merged with pending outbox check-offs (`mergedCheckState`, 1a), exactly what 6b's DB source produces. `todayView` gets `marks = detail.checkState.marks` (`TodayScreen.swift:55`).

Actions [F `AppTabs.swift:443–449`]:
- hero card (active or future) → push `TripPage(tripId = hero.id)` on the **Today** back stack;
- any row → open the step sheet for `(heroTripId, blockId)` (no-op without a hero, `AppTabs.swift:444–447`). The sheet then shows the detail of the **selection's** trip, `home.details[selection.tripID]?.detail`, and checks off through that trip (`home.setCheck(tripID: selection.tripID, …)`) [F `AppTabs.swift:398–406`], so a hero change while the sheet is open does not swap its trip;
- hidden-steps link → select the Settings tab;
- "See past trips" → select the My trips tab;
- "Plan a trip" (empty and completed states) → the wizard create route;
- pull to refresh → `refreshNow()` with the designed result capsule (iOS `planRefreshFeedback(todayFeedback)`), in every state including loading.

Check-off [F `TripPlanModel.swift:132–141`, `PlanHomeModel.swift:76–84`]: ignore while another check-off is in flight (`changingCheck`); find the block by id in the selection trip's detail (`detail.plan.blocks`, optional steps kept); require `actionable && checkKey != null`; enqueue `CheckOffChange(tripID, planInputHash = detail.plan.meta.inputHash, type = key.type, localDate = key.localDate, ordinal = key.ordinal, status)` with the session's mutation permit. A failure is kept as `mutationError` with **no UI** (no board), and the sheet stays open; a success clears it.

### 4.3 Copy [F `Kit/PlanUI/Resources/Screens.xcstrings`, table `Screens`]

Resolve through 0c's generated resources (`CopyIndex.find(CopyTable.Screens, "<key>")`, or the `R` name in `tools/l10n/names.lock.json`; the names below are computed with 0c's slug rule, **the lock wins**). Plan copy (day labels) comes from `CopyCatalog` (1a), not from resources.

| Key (en = key unless shown) | nl | Expected name |
|---|---|---|
| `Today` | Vandaag | `screens_today` |
| `Right now` | Op dit moment | `screens_right_now` |
| `Up next` | Hierna | `screens_up_next` |
| `Next · in {{duration}}` | Volgende · over {{duration}} | `screens_next_in_duration` |
| `Next · now` | Volgende · nu | `screens_next_now` |
| `In {{duration}}` | Over {{duration}} | `screens_in_duration` |
| `{{duration}} left` | Nog {{duration}} | `screens_duration_left` |
| `{{done}}/{{total}} done` | {{done}}/{{total}} gedaan | `screens_done_total_done` |
| `{{done}} of {{total}} done` | {{done}} van {{total}} gedaan | `screens_done_of_total_done` |
| `{{count}} skipped` (plural: `%lld skipped`) | `%lld overgeslagen` | `screens_count_skipped` |
| `Done · {{time}} · {{zone}}` | Klaar · {{time}} · {{zone}} | `screens_done_time_zone` |
| `Skipped · {{time}} · {{zone}}` | Overgeslagen · {{time}} · {{zone}} | `screens_skipped_time_zone` |
| `{{title}}, done` | {{title}}, klaar | `screens_title_done` |
| `{{title}}, skipped` | {{title}}, overgeslagen | `screens_title_skipped` |
| `No trips yet` | Nog geen reizen | `screens_no_trips_yet` |
| `Add your flights and you get a day-by-day plan for sleep, light and caffeine.` | Voeg je vluchten toe en je krijgt een dagplan voor slaap, licht en cafeïne. | `screens_add_your_flights_and_you_get_a_da_737cc7` |
| `No trips coming up` | Geen reizen gepland | `screens_no_trips_coming_up` |
| `Your last trip, {{route}}, ended on {{date}}. Plan the next one when you know your flights.` | Je laatste reis, {{route}}, eindigde op {{date}}. Plan de volgende zodra je je vluchten weet. | `screens_your_last_trip_route_ended_on_dat_d59ad3` |
| `See past trips` | Eerdere reizen bekijken | `screens_see_past_trips` |
| `Plan a trip` | Plan een reis | `screens_plan_a_trip` |
| `Nothing to do today` | Vandaag hoef je niets te doen | `screens_nothing_to_do_today` |
| `Keep your usual sleep times.` | Houd je gebruikelijke slaaptijden aan. | `screens_keep_your_usual_sleep_times` |
| `Your {{count}} preparation days start on {{date}}.` (plural: one "Your preparation day starts on {{date}}.", other "Your %lld preparation days start on {{date}}.") | one "Je voorbereidingsdag begint op {{date}}.", other "Je %lld voorbereidingsdagen beginnen op {{date}}." | `screens_your_count_preparation_days_start_df4f1b` (`%1$d`, `%2$s`) |
| `Plan starts today` / `Plan starts tomorrow` / `Plan starts on {{date}}` | Plan begint vandaag / morgen / op {{date}} | `screens_plan_starts_today` / `_tomorrow` / `_on_date` |
| `In {{count}} days` (plural: `In %lld day` / `In %lld days`) | `Over %lld dag` / `Over %lld dagen` | `screens_in_count_days` |
| `Nothing scheduled right now.` | Op dit moment staat er niets gepland. | `screens_nothing_scheduled_right_now` |
| `No more scheduled items.` | Er staan geen verdere stappen gepland. | `screens_no_more_scheduled_items` |
| `Adjusting` / `Partly adapted` / `Expected adapted` / `Protect a slower clock` | Aan het verschuiven / Gedeeltelijk aangepast / Naar verwachting aangepast / Bescherm een tragere klok | `screens_adjusting` … `screens_protect_a_slower_clock` |
| `Loading today` | Vandaag laden | `screens_loading_today` |
| `Some important steps are hidden.` / `All categories are hidden.` | Sommige belangrijke stappen zijn verborgen. / Alle categorieën zijn verborgen. | `screens_some_important_steps_are_hidden` / `screens_all_categories_are_hidden` |
| `Show them in Settings › Preferences` | Zet ze aan in Instellingen › Voorkeuren | `screens_show_them_in_settings_preferences` |
| `Upcoming` / `Active` | Binnenkort / Actief | `screens_upcoming` / `screens_active` |
| `{{from}} → {{to}}`, `{{date}} · {{offset}}`, `{{date}} · {{flightNumber}} · {{offset}}`, ` · {{time}}` (leading space) | same | `screens_from_to`, `screens_date_offset`, `screens_date_flightnumber_offset`, `screens_time` |

No new copy. Board sample text ("Travel steps are hidden", "Adapting", "Adapted", "we'll remind you the evening before") is **not** used; the iOS keys above are.

Formatter calls (1a `CopyLineFormatter`; read its Kotlin names): `format(block.text, CopyFormat(language, timeFormat, displayZone = block.displayTz, trip))`, `formatInstant`, `blockInterval(block, options, separator = " → ")`, `formatMinutes(minutes, language, duration = true)` ("30 min", "1 h 15 min"), `formatDayMonth`, `formatDate`, `formatWeekdayDate(utc, language, zone)`, `formatOffset(minutes, language)`, `zoneLabel(tz, trip)`, `catalog.text(dayLabelKey(phase), language, arguments = {"count": n}, count = n)` with `n = day.postArrivalIndex ?: 0`.

### 4.4 Test ids [F iOS]

`today.screen` (the scrolling list), `today.trip` (hero wrapper), `trip.<tripId>` (the card inside, as in My trips), `today.empty`, `today.create`, `today.completed`, `today.past`, `today.future` (note card), `today.hidden`, `today.progress`, `today.step.actionable.<blockId>` (actionable with a `checkKey`) else `today.step.<blockId>`, `plan.loading` (loading skeleton). The refresh capsule keeps 6a's `refresh.feedback`.

---

## 5. Conventions for every task

- **Modules.** Logic in `:core:plan` (JVM). UI and ViewModel in `:feature:today` (`net.chronoflow.feature.today`). Features never depend on features (Konsist): anything `:feature:today` needs from `:feature:trips` or `:feature:trip` must be in a core module first (Task 1).
- **Values.** Colors, type, spacing and shapes only from `CfTheme`, `PlanUITokens`, `AuthTokens`, `CfTypeScale`. Board-only numbers go in `@BoardMetrics("Today", "TodayRowStates") private object TodayMetrics` (Phase 2 Konsist rule `featureDpLiteralsComeFromBoardMetrics`). Never `MaterialTheme.colorScheme` in the feature.
- **Strings.** Every `Text` gets a resource (`stringResource`, `pluralStringResource`) or a formatter result. No literal reaches `Text`.
- **Main thread.** No disk, DB or JSON work on main. Flows run on `AppDispatchers.io` / `.default`.
- **Commits.** One task = one Conventional Commit, lower-case imperative subject, **no trailer**. Tick the named `plan.md` Phase 7 checkbox in that commit ("Ticks: none" = tick nothing).
- **Running tests.** Run only what you touched (`make targeted ONLY=…`); `make quick` once before handing back. If the sandbox cannot run Gradle, `adb` or the emulator, report the exact command and output; the orchestrator runs it.

---

## 6. Tasks

### Task 0 (no commit). Prerequisite check

Open the merged code and write down (for your final commit body) where each of these lives and its exact signature:

| Needed | Expected home (plan) | Phase |
|---|---|---|
| `TripListValue`, `PlanDecoder.trips(records, now)`, `PlanDecoder.detail(snapshot)` | `:core:plan` | 6a, 1a |
| `Today.kt` (`TodayPlan`, `todayView`, `calendarDaysUntil`) and `TodayTests` (two cases) | `:core:plan` `…/plan/Today.kt` | 6a §5.2 (OQ-6a-13) |
| `PlanClock`, `FakePlanClock`, `SystemPlanClock`, `AppGraph.planClock` | `:core:plan`, `:core:testing`, `:core:data` | 1a, 1d |
| `CopyLineFormatter` instance (lazy, loaded off main) | `AppGraph` / `SessionGraph` (`CopyCatalogProvider`) | 6a, 6b |
| Per-trip detail flow: `session.trips.detailValues(tripId): Flow<TripPlanSnapshot>` (plan + marks + pending check-offs), decoded with `PlanDecoder.detail` | `:core:data` (4d repository) | 6a, 6b (`DbTripPageSource` in `:app` uses it) |
| Preferences: `session.settings.planPreferences()` (hidden categories, time format) | `:core:data` | 6b |
| Check-off write (`OutboxRepository.enqueue(CheckOff)` with the permit) | `TripPageViewModel.setCheck` (or `TripPageCheckOff.kt`) in `:feature:trip` | 6d Task 6 |
| `TripCard(card: TripCardUi, onClick)` + the `TripCardUi` builder; `CategorySwatch(type, size: CategorySwatchSize)` (`…ui.plan`); `MetaChip` (`…ui.trip`); `CfBadge` variants | `TripCard` in `:feature:trips` `…/trips/TripCard.kt`; the rest in `:core:ui` / `:core:designsystem` | 6a |
| `StepSheetContent(state, busy, onCheck, onSeeToday)`, `StepGoneContent`, `RefusedChangeFeedback` (`:core:ui` `…ui.plan`); `stepSheetState(detail, blockId, nowMs, formatter, language, timeFormat)` (`:core:plan` `…plan.step`); `CfModalBottomSheet` | 6d, 2 | 6d |
| `RefreshFeedbackModel` (`:core:sync` `…sync.refresh`), `RefreshFeedbackCapsule` (`:core:ui` `…ui.refresh`, tag `refresh.feedback`), `RefreshDebugProbe` | 6a | 6a |
| Today seam, tab selection API (`selectTab`/equivalent), per-tab push | 6a / 4e `RootNavigator` | 6a |
| Language source used by the trip page (never `Locale.getDefault()`) | 6b | 6b |
| `ReportDrawnWhen` in `CfRoot` | 3b | 3b |
| `WindowPolicy.listCardMaxWidthDp` (640) and `wideBreakpointDp` (600) | `:core:designsystem` `…/ds/layout/WindowPolicy.kt` | 6a |

### Task 1. Shared pieces (`refactor(ui): share the trip card and the check-off writer across tabs`) — Ticks: none

Required end state (skip any item that is already true; if all are true, skip this commit):

`CategorySwatch` (`…ui.plan`), `MetaChip` (`…ui.trip`) and `RefreshFeedbackCapsule` (`…ui.refresh`) are already in `:core:ui` (6a §6), and 6d's `StepSheetContent`/`StepGoneContent`/`RefusedChangeFeedback` (`…ui.plan`) and `stepSheetState` (`:core:plan` `…plan.step`) are already shared (6d §4). Do not move them again.

1. **Trip card in `:core:ui`** (`…/ui/trip/TripCard.kt`): move 6a's `TripCard(card: TripCardUi, onClick)`, `TripCardUi` and the pure `TripCardUi` builder (the "Card strings" code in `TripsViewModel`) out of `:feature:trips`. The builder stays next to its Compose-annotated card models in `:core:ui` (see Stack check); it accepts `PlanPreferences` from `:core:plan` instead of feature-local `TripsPreferences`. My trips imports them from there. Move, do not copy; keep names and test ids (`trip.<id>`).
2. **`MinuteClock`** (`…/ui/clock/MinuteClock.kt`, new, Android-only):
   ```kotlin
   @Stable class MinuteClock(private val source: State<Instant>) { val now: Instant get() = source.value }
   ```
   6a builds the Next row's eyebrow label (`Now` / `In(formatMinutes(…))`) in `TripsViewModel` from `now`, so `TripCardUi` changes every minute. Change it: `TripCardUi.next` carries `startUtc: Instant` instead of the label, and `TripCard` takes `clock: MinuteClock` and draws the eyebrow in a **leaf** (`NextEyebrow`, Task 4 "Leaf rule": `"Next · now"` when `start ≤ now`, else `"Next · in {{duration}}"` with `formatMinutes(max(0, ceil((start − now) / 60 s)), duration = true)`, exactly 6a's rule). Update `TripsViewModel`, `TripsContent` and 6a's tests (the label assertions move from the VM test to a card test). A card must not recompose on a minute tick; only its eyebrow leaf may.
3. **Step sheet**: nothing to move. Today builds `StepSheetState` with 6d's `stepSheetState(…)` and shows `StepSheetContent` in `CfModalBottomSheet` (Task 3, Task 5).
4. **Check-off writer in `:core:data`** (`…/data/plan/CheckOffWriter.kt`): 6d enqueues the check-off in `TripPageViewModel.setCheck` (`:feature:trip`). Extract it as
   ```kotlin
   class CheckOffWriter(private val outbox: OutboxRepository, private val permit: SessionMutationPermit?) {
       suspend fun setCheck(tripId: String, detail: TripDetail, blockId: String, status: CheckStatus?)
   }
   ```
   (port of `TripPlanModel.swift:132–141`: no-op when the block is missing, not `actionable` or has no `checkKey`). 6d's ViewModel uses it.
5. **Card column width:** reuse 6a's `WindowPolicy.listCardMaxWidthDp` (640, `:core:designsystem` `…/ds/layout/WindowPolicy.kt`). Do not add a second constant.

Tests: existing 6a/6c/6d tests must pass, changed only for imports and for the eyebrow move (item 2). Add `TripCardEyebrowTest` (`@Logic`, Robolectric): with a `MinuteClock` over `mutableStateOf`, the eyebrow reads "Next · in 30 min" 30 min before the start, "Next · in 1 min" 1 ms before it, and "Next · now" at the start. If anything moved out of `:feature:trip`, run `make perf-timeline` and confirm the results stay within ±25% of the last row in `docs/perf-timeline.md` (append the row).

### Task 2. Today logic (`feat(plan): add the today trip selection`) — Ticks: "**Today.kt**"

6a already ported `Kit/PlanCore/Today.swift` into `…/plan/Today.kt` (`TodayPlan`, `todayView`, `calendarDaysUntil`) with the `TodayTests` cases `todaySeparatesCurrentAndFourUpcomingAndDoesNotHideTheNextImportantStep` and `calendarDaysUseThePlanZoneAcrossDaylightSaving` (6a §5.2, T2, OQ-6a-13). **Do not port them again.** First diff 6a's `Today.kt` against §4.1 line by line and fix any deviation in this commit (say which in the body). Check in particular: `minutesUntilNext` = `max(0, ceil(Duration.between(now, planDate(start)).toMillis() / 60_000.0).toInt())`; `calendarDaysUntil` = `val z = try { ZoneId.of(zone) } catch (e: DateTimeException) { ZoneOffset.UTC }` (no `runCatching`), then `max(0, ChronoUnit.DAYS.between(now.atZone(z).toLocalDate(), planDate(at).atZone(z).toLocalDate()).toInt())`.

**Files**
- `…/plan/TodaySelection.kt` (new): `fun selectTodayTrip(trips: List<TripListValue>, now: Instant): TripListValue?`, `fun lastEndedTrip(trips: List<TripListValue>, now: Instant): TripListValue?`. Comparators per §4.1; use `sortedWith`, then `firstOrNull()`. If `TripListValue` is not in `:core:plan`, move it there in this commit.

**Edge cases:** `now == block.end` is not current; `now == block.start` is current; a block with `start == end` is never current and is upcoming only while `start > now`; a plan with no days falls back to `trip.departureUtc` / `trip.endUtc` for `beforePlan` / `afterPlan`; unknown block types (`renderableBlocks` excludes them) never show; an invalid zone id falls back to UTC; `at` before `now` gives 0.

**Tests** (`core/plan/src/test/kotlin/net/chronoflow/core/plan/`, plain JVM, fixture `trips/detail.json` from `:core:testing`, which is the same blob as iOS `PlanUITests/Fixtures/detail.json`):
- `TodayTests` (6a's class; add the third iOS case, Swift name kept):
  - `todaySelectionUsesActiveEndThenUpcomingStartAndStableIdentity` [F `TodayTests.swift:9–27`]: `now = day[0].startUtc`; cards built like iOS `recoveryCard(detail)` with ids `a` (plan −60 s…+60 s), `b` (−60…+120), `future` (+240…+600), stored `status = upcoming` (selection re-derives it); assert `[future,b,a]@now → a`, `[b,a]@now+60 → b`, `[a,future]@now+60 → future`, `lastEndedTrip([a,b])@now+180 → b`.
- `TodayEdgeTests` (Android-only): a `skipped` mark also moves `nextStep`; marked blocks stay in `currentSteps`/`upcomingSteps`; a block with `priority == optional` of type `seek_light` never appears; `minutesUntilNext` for start − now = 1 ms → 1, = 60 000 ms → 1, = 60 001 ms → 2, started → 0; `importantHidden` is true when a core block's type is hidden even if the block is optional-stripped; `allCategoriesHidden` false with 16 of 17; invalid zone `"Mars/Base"` → UTC days; `at` in the past → 0; selection with `planStartUtc == null` uses the stored card status and `departureUtc`; equal ends tie-break on id.
- `TodayViewBudgetTest` (declared perf test): `todayView` on the large60 detail from 1b's `LargeTimelineFixtures`, 1 warm-up then 10 samples with `System.nanoTime`, every sample ≤ 20 ms (OQ-7-9).

Run: `make targeted ONLY="core:plan:TodayTests core:plan:TodayEdgeTests core:plan:TodayViewBudgetTest"`.

### Task 3. Source, reducer and ViewModel (`feat(today): add the today source, reducer and view model`) — Ticks: none

**Files** (`…/today/`):
- `data/TodaySource.kt`:
  ```kotlin
  interface TodaySource {
      fun synced(): Flow<Boolean>                    // account?.syncedAt != null
      fun preferences(): Flow<PlanPreferences>       // session.settings.planPreferences(): hiddenCategories + timeFormat, §4.2
      fun trips(): Flow<List<TripRecord>>            // the trips flow 6a's TripsViewModel uses
      fun detail(tripId: String): Flow<TripDetail?>  // session.trips.detailValues(tripId) → PlanDecoder.detail; null without a plan or on a decode error
      suspend fun setCheck(tripId: String, detail: TripDetail, blockId: String, status: CheckStatus?)
  }
  ```
  `DbTodaySource(session: SessionGraph)` lives in **`:app`** (`app/…/today/DbTodaySource.kt`), like 6b's `DbTripPageSource`, because it binds session members. It uses `session.trips.detailValues`, `session.settings.planPreferences()`, the account repository and Task 1's `CheckOffWriter`. Every flow is `flowOn(io)` and `conflate()`d; decoding runs on the decoder's worker. No plan retention lease (iOS `PlanHomeModel` holds none; OQ-7-10).
- `TodayUiState.kt` (all `@Immutable`):
  ```kotlin
  sealed interface TodayUiState {
      data object Loading : TodayUiState
      data object Empty : TodayUiState
      data class Completed(val route: String, val endDate: String) : TodayUiState   // endDate = planEndDate ?: arriveLocal.take(10)
      data class Future(val hero: TripListValue, val next: NextStepUi?, val preparationDays: Int?,
                        val planStartUtc: String, val planStartZone: String) : TodayUiState
      // planStartUtc = hero.planStartUtc ?: card.departureUtc; planStartZone = hero.planStartZone ?: card.from.zone
      // (TodayFutureTripCard.swift:74–75, TodayScreen.swift:146–147)
      data class Active(val hero: TripCardUi, val panel: TodayPanelUi?) : TodayUiState
      // hero = Task 1's TripCardUi builder with next = todayView(heroDetail…).nextStep (null without a detail)
  }
  data class NextStepUi(val blockId: String, val type: BlockType, val title: String, val startUtc: Instant, val startText: String)
  data class TodayPanelUi(val dayLabel: String?, val dayStatus: DayStatus?, val progress: ProgressUi?,
                          val current: List<TodayRowUi>, val upcoming: List<TodayRowUi>, val hidden: HiddenNotice)
  data class ProgressUi(val done: Int, val total: Int, val skipped: Int)
  enum class HiddenNotice { None, Some, All }   // All wins over Some (iOS order)
  data class TodayRowUi(val blockId: String, val type: BlockType, val actionable: Boolean, val title: String,
                        val zone: String, val running: Boolean, val target: Instant, val timeText: String,
                        val mark: CheckStatus?, val markedTimeText: String)
  ```
  `route` = `"${from.city ?: from.iata} → ${to.city ?: to.iata}"`. Row fields per `TodayStepRow.swift:35–54`: `running` = in `current`; `target` = `running ? end : start`; `timeText` = `formatInstant(target)`; `markedTimeText` = `formatInstant(start)` when the type is `caffeine_serving` or `zone_change` or `start == end`, else `blockInterval(block, " → ")`; `mark` = the mark whose `checkIdentity` equals the block's, only when `actionable`; `actionable` = `block.actionable && block.checkKey != null` (drives the test id). `progress` only when `total > 0`. `dayStatus` only when `dayStatusLabelKey` is non-null. `hidden` = `All` if `allCategoriesHidden`, else `Some` if `importantHidden`, else `None`. **No field depends on `now` beyond which rows are current/upcoming**, so a quiet minute tick yields an equal state.
- `TodayReducer.kt`: `object TodayReducer { fun reduce(input: TodayInput): TodayUiState }`, pure JVM, `TodayInput(trips: List<TripListValue>?, synced: Boolean, prefs, heroDetail: TripDetail?, now: Instant, language: Language, formatter: CopyLineFormatter?)`. `trips == null` = not loaded yet. It applies §4.2 and formats titles, times, zone labels and the day label. Ignore a `heroDetail` whose `trip.id` differs from the selected hero (stale emission).
- `TodayViewModel.kt`:
  ```kotlin
  class TodayViewModel(source: TodaySource, clock: PlanClock, formatter: suspend () -> CopyLineFormatter, dispatchers: AppDispatchers) : ViewModel() {
      val now: StateFlow<Instant>                // the minute clock for leaves
      val uiState: StateFlow<TodayUiState>       // initial Loading
      val stepSheet: StateFlow<StepSelection?>   // (tripId, blockId)
      val stepSheetState: StateFlow<StepSheetState?> // 6d's stepSheetState(detail of selection.tripId, blockId, …)
      val changingCheck: StateFlow<Boolean>
      fun setLanguage(language: Language)
      fun openStep(blockId: String)              // uses the current hero id; no-op without a hero
      fun dismissStep()
      fun setCheck(blockId: String, status: CheckStatus?)
  }
  ```
  - `ticks = clock.values().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)`; `now = ticks.stateIn(…, WhileSubscribed(5_000), Instant.EPOCH)`. Leaves never see `EPOCH`, because `uiState` leaves `Loading` only after a tick.
  - Trips: decode only when the records change, `source.trips().map { decoder.trips(it, ticks.first()).values }`, then `combine(decoded, ticks) { values, now -> values.map { it.copy(status = tripDisplayStatus(it.card, it.planStartUtc, it.planEndUtc, now)) } }` (status re-derived each tick without decoding; the same pipeline as 6a's `TripsViewModel` step 1, iOS `TripsModel.updateNow`).
  - Hero id: `selectTodayTrip(trips, now)?.card.id`, `distinctUntilChanged()`; `heroDetail = heroId.flatMapLatest { id -> id?.let(source::detail) ?: flowOf(null) }`.
  - `uiState = combine(…).map(TodayReducer::reduce).flowOn(dispatchers.default).distinctUntilChanged().stateIn(viewModelScope, WhileSubscribed(5_000), Loading)`.
  - Sheet: `selectionDetail = stepSheet.map { it?.tripId }.distinctUntilChanged().flatMapLatest { id -> id?.let(source::detail) ?: flowOf(null) }` (reuse `heroDetail` when the id equals `heroId`, so the hero's detail is not collected twice); `stepSheetState = combine(stepSheet, selectionDetail, ticks, prefs, language) { … stepSheetState(detail, sel.blockId, now.toEpochMilli(), formatter, language, timeFormat) }.flowOn(default)`, null without a selection or formatter.
  - `setCheck`: return if `changingCheck` or no selection; set it; call `source.setCheck(selection.tripId, selectionDetail, blockId, status)` (iOS `home.setCheck(tripID: selection.tripID, …)`); catch only the data layer's failure types (rethrow `CancellationException`), keep the error in a private field; clear `changingCheck` in `finally`.
  - The sheet selection lives in the ViewModel. It survives rotation, folding and locale/font-scale recreation (the ViewModel store is retained); process death is a cold launch (A8).
  - Factory through `viewModelFactory { initializer { … } }` reading `AppGraph`/`SessionGraph` from `CreationExtras`, like 6a's `TripsViewModel`. Never hold a repository across session epochs.

**Edge cases:** hero changes while the sheet is open → the sheet keeps its `tripId` and keeps showing that trip's detail (iOS `home.details[selection.tripID]`); only a block that left that plan gives 6d's step-gone state; the selected hero's plan is evicted → panel disappears, hero stays; `synced` flips to false (sign-out) is handled by the shell tearing the session down (4e), not here; a language change reformats without a new tick.

**Tests** (`feature/today/src/test/…`, plain JVM unless noted; `runTest(timeout = 60.seconds)`, Turbine, `FakePlanClock`, a `FakeTodaySource`):
- `TodayReducerTest`, one method per state and rule: not loaded → Loading; `synced = false` → Loading; formatter null → Loading; no trips → Empty; only completed trips (iOS fixture filter) → Completed with `route` "City → City" and `endDate` from `planEndDate`, falling back to `arriveLocal.take(10)`; only upcoming → Future with `preparationDays`; active hero without detail → Active with `panel == null`; hidden `All` beats `Some`; marked rows keep their position with `mark`; a non-actionable block gets `actionable = false` and no mark; stale `heroDetail` id → panel null.
- `TodayReducerQuietTickTest`: for the iOS fixture at `now` and `now + 60 s` (no block boundary in between, chosen from the fixture) the two states are `==`; at the next block start they differ.
- `TodayViewModelTest`: the first tick leaves Loading; a quiet `advance(+60 s)` emits **no** new `uiState` (`expectNoEvents()`) while `now` emits; a boundary tick emits once; `setCheck` twice while the first is suspended calls the source once; after the hero changes with the sheet open, `setCheck` still goes to `selection.tripId` and `stepSheetState` still comes from that trip's detail; `openStep` without a hero is a no-op; `setLanguage(Nl)` re-emits with Dutch titles.
- `CheckOffWriterTest` (`:core:data`, JVM on 4d's JDBC `SyncStore`; only if Task 1 created the writer): `setCheck(Done)` inserts one outbox row of kind check-off with `plan_input_hash`, `type`, `local_date`, `ordinal`, `status = "done"` and sets the local mark in the same transaction; `status = null` stores the undo; a non-actionable block writes nothing.

Run: `make targeted ONLY="feature:today:TodayReducerTest feature:today:TodayReducerQuietTickTest feature:today:TodayViewModelTest"`.

### Task 4. The five states (`feat(today): draw the today states`) — Ticks: none

**Files** (`…/today/ui/`; Swift names kept): `TodayScreen.kt` (stateless: `TodayScreen(state, clock: MinuteClock, language, formatter, timeFormat, actions: TodayActions, refreshing, onRefresh)`), `TodayFutureTripCard.kt`, `TodayPanel.kt` (`DayStatusPill`, `DayProgressRow`, section headings), `TodayStepRow.kt`, `TodayStates.kt` (`TodayEmptyCard`, `TodayCompletedCard`, `TodayFutureNote`, `PlanLoadingView`), `TodayClockText.kt` (the leaves), `TodayMetrics.kt`, `TodayProbe.kt`.

**Page** (board `Today` `main` + iOS `TodayScreen.body`):
- `Scaffold` with `CfLargeTopAppBar(title = screens_today, exitUntilCollapsedScrollBehavior)` and `nestedScroll`, as 6a's My trips does; `CfPullToRefreshBox` around a `LazyColumn` tagged `today.screen`, with 6a's capsule overlay.
- `contentPadding`: horizontal `space4` (16), top `space4`, bottom `space4`, plus `LocalSafeInsets.current.only(Horizontal + Bottom)` exactly as 6a's `TripsContent` does.
- **Spacing (no list-wide `spacedBy`):** rows are separate items but sit 4 dp apart, so each item carries its own top padding. Top-level blocks (hero, future note, panel header, the "Right now" heading, the "Up next" heading, empty/completed/loading) are `space6` (24) below the previous block [board `main` gap 24, `TodayPanel` `gap-6`]. A section's first row or its empty message is `space2` (8) below its heading [board `mb-2`]; each further row `space1` (4) below the previous row [board `gap-1`]. The progress row is `space1` below the day label [board `mt-1`]. The hidden link is `space3` (12) below the last Up next item [board `margin-top: -12px` on the 24 gap].
- **Width:** `horizontalAlignment = CenterHorizontally`; every item `Modifier.widthIn(max = WindowPolicy.listCardMaxWidthDp.dp).fillMaxWidth()`. No breakpoint branch: below 600 dp the cap never binds. Never measure into state.
- Items have stable keys (`"hero"`, `"note"`, `"panel.header"`, `"section.now"`, `"row.<blockId>"`, `"section.next"`, `"hidden"`, `"empty"`, `"completed"`, `"loading"`) and a `contentType` per kind.

**Leaf rule [R].** Only composables in `TodayClockText.kt` (and Task 1's card eyebrow) read `clock.now`. Each reads it inside `remember(<inputs>) { derivedStateOf { <int> } }` that yields a whole number (minutes or days), then formats that number in composition with `stringResource`/`pluralStringResource` and the formatter. Leaves: `RowRelativeText` (the unmarked row subtitle), `NextEyebrow` (hero; it lives in Task 1's `TripCard` file in `:core:ui`, not here), `DaysUntilText` ("Today" / "In N days"), `PlanStartsEyebrow`, `CompletedBodyText`. Nothing else receives `Instant`.

**Loading** (`PlanLoadingView`, board `isLoading`; iOS adds no 40% line, the board wins on looks, OQ-7-2): `Column(spacing = space4)`: `CfSkeleton` 248 dp (`space1 × 62`) high, `radius3Xl`; a line `fillMaxWidth(0.4f)` × 20 dp (`TodayMetrics`), `CfTheme.shapes.full`; three 64 dp (`space16`) rows, `radius2Xl`. One node: `clearAndSetSemantics { contentDescription = screens_loading_today }`, tag `plan.loading`.

**Empty** (board `isEmpty`, iOS `empty`): extra top margin `space6`; column centred, gap `space3`, padding vertical `space8` horizontal `space5`, radius `radius3Xl`, **dashed** 2 dp (`AuthTokens.focusWidth`) `border` stroke with dash and gap `space1` (iOS) and no fill; icon disc 56 dp (`space14`) `muted` with lucide `moon` 24 dp (`space6`) in `dawnText`, hidden from accessibility; title `screens_no_trips_yet` in `screenCardTitle` (Fraunces 24/32), `heading()`; body in `screenBodyLg` `mutedForeground`, centred; `CfButton(screens_plan_a_trip, Primary, Lg, fillWidth = true)` tag `today.create`. Container tag `today.empty`.

**Completed** (board `isCompleted`, iOS `completed`): same column as Empty but fill `card`, solid 1 dp `border`, no shadow; lucide `circle-check-big` (board path `M21.801 10A10 10 0 1 1 17 3.335` + `m9 11 3 3L22 4`); title `screens_no_trips_coming_up`; body leaf `CompletedBodyText`: `date = min(endDate, today's local date in the device zone)` (string compare of `yyyy-MM-dd`), formatted `formatDayMonth` when its year equals this year, else `formatDate`, into `screens_your_last_trip…(route, date)`; buttons in a column gap `space1`: `CfButton(screens_plan_a_trip, Primary, fillWidth, minHeight = actionMinHeight 48)` tag `today.create`, `CfButton(screens_see_past_trips, Ghost, fillWidth, minHeight = 48)` tag `today.past`. Container tag `today.completed`.

**Future** (board `isFuture`, iOS `TodayFutureTripCard` + `future`):
- Card wrapped in a `Box` tagged `today.trip`; card `cfClickable(Role.Button)` → `openTrip`, `mergeDescendants`, tag `trip.<id>`; fill `card`, 1 dp `border`, `radius3Xl`, padding `space5`, `softShadow`, gap `futureCardSpacing` (14).
- Row: badge **Outline** "Upcoming" (board `Badge status=upcoming`; iOS draws secondary, OQ-7-2) · spacer · `DaysUntilText` (`calendarDaysUntil(card.departureUtc, now, card.from.zone)`: 0 → `screens_today`, else `screens_in_count_days` plural) in `screenMeta` `mutedForeground` `tnum`.
- Title `screens_from_to(from.iata, to.iata)` in `screenPageTitle` (Fraunces 30/36).
- Meta `screens_date_offset` / the flight-number variant: `formatWeekdayDate(departureUtc, zone = from.zone)`, `flightNumber`, `formatOffset(offsetDifferenceMinutes)`; `screenBody` `mutedForeground` `tnum`.
- Next row when `next != null`: min height `space14` (56), padding vertical 10 (`TodayMetrics`) horizontal `space3`, radius `radius2Xl`, fill `surface2`, gap `space3`; `CategorySwatch(next.type, Small)` (28 dp); eyebrow leaf `PlanStartsEyebrow` (`calendarDaysUntil(planStartUtc, now, planStartZone)`: 0 → `screens_plan_starts_today`, 1 → `_tomorrow`, else `_on_date(formatWeekdayDate(planStartUtc, zone))`) in `screenEyebrow` (12/16 600, 0.1em) uppercase `dawnText`; text `title + screens_time(startText)` in 16/24 weight 500 (`screenBodyLg` + `FontWeight.Medium`); trailing lucide `chevron-right` 20 dp `mutedForeground`.
- Note card (tag `today.future`): fill `card`, 1 dp `border`, `radius3Xl`, padding `space5`, gap `space2`, no shadow; title `screens_nothing_to_do_today` in `screenSectionTitle` (Fraunces 20/28), `heading()`; body **one paragraph** in `screenBody` `mutedForeground`: `screens_keep_your_usual_sleep_times`, plus `" " + plural screens_your_count_preparation_days…(count, formatWeekdayDate(planStartUtc, planStartZone))` when `preparationDays > 0` (board paragraph; iOS uses two lines, OQ-7-2).

**Active hero:** Task 1's `TripCard(card = state.hero, clock, onClick = { actions.openTrip(id) })` wrapped in a `Box` tagged `today.trip`. It already matches the board (badge Dawn "Active", `MetaChip` flight number, offset, IATA 30/36 + city, dashed line + plane, times row, 2 dp `dawn` border, next row with `NextEyebrow` "Next · now" when `start ≤ now` else "Next · in {{duration}}" with `ceil` minutes). Verify against the board and fix the shared card if it differs.

**Panel** (board `ChronoFlow.TodayPanel` `Na`, iOS `panel`), only for `Active` with `panel != null`:
- **Header** (only when `dayLabel != null`): `FlowRow(spacing = space2)`: day label in `screenSectionTitle`, `heading()`; `DayStatusPill` when `dayStatus != null`. Then `DayProgressRow` (top `space1`) when `progress != null`.
- **`DayStatusPill`**: padding horizontal `space2`, vertical 2 dp (`space1 / 2`, iOS `TodayPanelComponents.swift:18`; board `py-0.5`), icon–text gap `space1`, pill shape, text 12/16 weight 500 (`screenMeta` + Medium). `adapting` → no fill, 1 dp `dawn` × `statusBorderOpacity` (0.5) border, text `foreground` (board; iOS `dawnText`, OQ-7-2); `partiallyAdapted` → `secondary` fill, `secondaryForeground`; `adapted` and `adaptedSlowerClock` → `dawn` fill, `dawnForeground`, lucide `check` 12 dp before the text. Text from `screens_adjusting` / `_partly_adapted` / `_expected_adapted` / `_protect_a_slower_clock`.
- **`DayProgressRow`** (tag `today.progress`): `FlowRow(spacing = space2)`, vertical centre: `CfLinearProgress(progress = { done / total })` sized `progressTrackWidth` (80) × 6 dp (`TodayMetrics`); `check` 12 dp + `screens_done_total_done(done, total)` 12/16 500 `mutedForeground` `tnum`; when `skipped > 0`, `screens_count_skipped` plural. One node: `clearAndSetSemantics { contentDescription = screens_done_of_total_done(done, total) + (if skipped > 0 ", " + screens_count_skipped(skipped)) }`.
- **Section heading** ("Right now", "Up next"): `screenLabel` (14/20 600) with letter spacing 0.05 em (`TodayMetrics`), uppercase, `mutedForeground`, `heading()`, bottom gap `space2`. Rows below with gap `space1`.
- **Right now empty:** column centred, padding `space6`, radius `radius3Xl`, dashed 1 dp `border` (dash `space1`); lucide `calendar-check` 24 dp, gap `space2`; `screens_nothing_scheduled_right_now` in `screenBodyLg` `mutedForeground`.
- **Up next empty:** `screens_no_more_scheduled_items` in `screenBodyLg` `mutedForeground`.
- **Hidden link** (tag `today.hidden`, when `hidden != None`), `space3` below the Up next section (board margin −12 on a 24 gap): min height `space14`, padding vertical `space2` horizontal `space3`, radius `radiusXl` (18), fill `surface2`, gap `space3`; lucide `eye-off` 18 dp `mutedForeground`; column: title (`All` → `screens_all_categories_are_hidden`, `Some` → `screens_some_important_steps_are_hidden`) 14/20 500, subtitle `screens_show_them_in_settings_preferences` 13/18 (`TodayMetrics`) `mutedForeground`; `chevron-right` 18 dp `mutedForeground`; `cfClickable(Role.Button)` → `settings`; merged.

**`TodayStepRow`** (board `TodayRowStates`, iOS `TodayStepRow`):
- `Row`, min height `space14` (56), padding `space2` (8), gap `space3`, shape radius 16 (`TodayMetrics`), `cfClickable(Role.Button)` → `openStep(blockId)` (full-width target, no check control on the row).
- `CategorySwatch(type, Small)` (28 dp) with alpha `markedOpacity` (0.45) when marked.
- Column: title 16 sp / 22 sp weight 500 (`screenBodyLg` + Medium + `TodayMetrics.rowTitleLineHeight`), `foreground`, or `mutedForeground` when marked; subtitle 14/20 `mutedForeground` `tnum`:
  - marked: `screens_done_time_zone(markedTimeText, zone)` / `screens_skipped_time_zone(…)` (static, no clock);
  - unmarked: leaf `RowRelativeText`: `(running ? "→ " : "") + timeText + " · " + zone + " · " + (running ? screens_duration_left(d) : screens_in_duration(d))`, `d = formatMinutes(max(0, ceil((target − now) / 60 s)), duration = true)`. The `"→ "` prefix and the `" · "` joins are literal in iOS (`TodayStepRow.swift:53–54`); build them in a `buildString` inside the leaf.
- Marked trailing icon, 28 dp (`space1 × 7`) circle: done = `dawn` fill, lucide `check` 16 dp `dawnForeground`; skipped = 2 dp `border` ring, no fill, lucide `skip-forward` 14 dp `mutedForeground`. Hidden from accessibility.
- Semantics: unmarked → `mergeDescendants` (TalkBack reads title then subtitle, which equals iOS `"\(title), \(subtitle)"`); marked → `clearAndSetSemantics { contentDescription = screens_title_done/skipped(title); role = Button; onClick }`. Tag `today.step.actionable.<id>` or `today.step.<id>`.
- No strike-through. Progress counts done steps only (1a `buildCheckState`).

**Icons:** lucide `moon`, `circle-check-big`, `calendar-check`, `eye-off`, `chevron-right`, `check`, `skip-forward`, `plane`. If one is missing from Phase 2's generated `LucideGlyphs`/vectors, add it to the generator's list (pinned lucide v0.511.0) and run `make generate` in this commit. If the generated vectors have a fixed 2-unit stroke, accept it for the 2.5 board stroke and record the delta.

**`TodayMetrics`** (`@BoardMetrics("Today", "TodayRowStates")`): `nextRowVerticalPadding = 10.dp`, `loadingLineHeight = 20.dp`, `loadingLineFraction = 0.4f`, `progressHeight = 6.dp`, `rowRadius = 16.dp`, `rowTitleLineHeight = 22.sp`, `hiddenIcon = 18.dp`, `hiddenSubtitleSize = 13.sp`, `hiddenSubtitleLineHeight = 18.sp`, `sectionTracking = 0.05.em`, `statusIcon = 12.dp`, `markGlyphDone = 16.dp`, `markGlyphSkipped = 14.dp`. Everything else is a token.

**Probe** (`TodayProbe.kt`, main source set, pattern of 1d Task 3):
```kotlin
interface TodayProbe { enum class Kind { SCREEN, HERO, PANEL, ROW, LEAF }; fun composed(kind: Kind, id: String) }
val LocalTodayProbe = staticCompositionLocalOf<TodayProbe?> { null }
```
First statement of `TodayScreen` content (SCREEN, "today"), the hero card wrapper (HERO, tripId), the panel header (PANEL, "panel"), each `TodayStepRow` (ROW, blockId) and each leaf in `TodayClockText.kt` plus the card eyebrow (LEAF, `"<leaf>.<id>"`): `SideEffect { probe?.composed(kind, id) }`. Null in production.

**Board deltas.** Record each place where the built screen differs from a board (OQ-7-2 list, plus anything the captures show) in `docs/board-deltas.md` (create it if 6a did not): `| Board | Element | Board | Built | Why |`.

**Tests**
- `TodayScreenTest` (`@Logic`, Robolectric, stateless screen with hand-built states and a `MinuteClock` over a `mutableStateOf`): each state shows its tag (`plan.loading` with content description "Loading today", `today.empty` + `today.create`, `today.completed` + `today.past`, `today.trip` + `today.future`, `today.trip` + `today.progress`); a marked done row's node has content description "Avoid bright light, done"-style text from `screens_title_done`; actionable rows carry `today.step.actionable.<id>`; the hidden link shows "All categories are hidden." when `All`; the future card shows "Today" when the departure is today and "In 6 days" six local days ahead.
- `TodayGuardsTest` (`@Layout`, parameterised, one case per state × profile × language × theme): states `active, empty, future, loading, completed`; one light case per state × (`small` Dutch, `largeFont` Dutch, `pixel8` English), 15 cases, each < 5 s, per plan.md standard acceptance #5; the separate wide tests cover `foldInner` and `landscapeSideNav`. Data = the iOS `todayFixture` port: `MockAppSeed.make(now = 2026-11-08T10:00:00Z, walkthroughSeen = true)`, first account by snapshot id, `TripListValue(status = card.progress.status)`, the active trip's detail; `empty` → no trips; `completed` → only `status == completed`; `future` → only `status == upcoming`. Guards: `assertTextFits`, `assertTouchTargets`, `assertInteractiveInSafeArea`, `assertNoInteractiveOverlap`, `assertInk`.
- `TodayLayoutTests` (`@Layout`, ports of `I/AppTests/TodayLayoutTests.swift`, names kept): `testActiveAtXXLarge` … `testNothingNowAtXXLarge` (states `active, empty, future, loading, completed, done, skipped, hidden, allHidden, nothingNow` from `TodaySnapshotTests.swift:25–46`) at `largeFont`, `nl`, profile `small`; `testActiveAtXXLarge` is parameterised over `small`, `pixel8`, `foldInner` (one case per profile, AGENTS Tests 5; no loop inside a case): every item lies inside the 16 dp gutters and the scroll content fits horizontally.
- `TodayRowSnapshotTests.testRunningDoneAndSkippedRowsAsOnTheRowStatesBoard` (`@Layout`, parameterised: `small`, `largeFont` × en, nl × light, dark = 8 cases): running, done and skipped rows of the first actionable block at `now = block.start`; guards per case.
- `TodayWideLayoutTest` (`@Layout`): at `foldInner` and `landscapeSideNav`, the hero card and panel rows are ≤ 640 dp wide and centred in the content area (left and right gaps equal ± 1 dp); at `pixel8` the card spans the width minus 2 × 16 dp.
- `TodayBoardCaptureTest` (`@Capture`, Robolectric `BoardCapture`): one PNG per board (`Today`, `TodayEmpty`, `TodayFuture`, `TodayLoading`, `TodayCompleted`, `TodayRowStates`) × light/dark × en/nl, at 390 × 844 dp equivalent (`pixel8`), named per 0e.

Run: `make targeted ONLY="feature:today:TodayScreenTest"` while building, then the layout classes, then `make captures ONLY=Today,TodayEmpty,TodayFuture,TodayLoading,TodayCompleted,TodayRowStates`.

### Task 5. Wire the tab (`feat(today): open trips, steps, settings and the wizard from today`) — Ticks: "**Today tab**"

**Files:** `…/today/TodayRoute.kt` (`@Composable fun TodayRoute(viewModel: TodayViewModel, actions: TodayActions)`: collects `uiState`, `stepSheet`, `changingCheck` with `collectAsStateWithLifecycle`; wraps `now` in `MinuteClock(viewModel.now.collectAsStateWithLifecycle())` without reading `.value`; `LaunchedEffect(language) { viewModel.setLanguage(language) }`); `…/today/TodayActions.kt`; `:app` nav entry for the `Today` key (replace 6a's seam).

- `data class TodayActions(val openTrip: (String) -> Unit, val settings: () -> Unit, val pastTrips: () -> Unit, val create: () -> Unit, val refresh: suspend () -> Unit)`; rows call the ViewModel (`openStep`), not an action.
- `:app` wiring: `openTrip` = push `TripPage(tripId)` on the **Today** stack; `settings` = select the Settings tab; `pastTrips` = select the My trips tab (its root); `create` = the wizard create route (6a's seam); `refresh` = 6a's `refreshNow` with the capsule feedback.
- Step sheet: when `stepSheetState != null`, show `CfModalBottomSheet(onDismissRequest = viewModel::dismissStep)` holding 6d's `StepSheetContent(state, busy = changingCheck, onCheck = { viewModel.setCheck(selection.blockId, it) }, onSeeToday = viewModel::dismissStep)`; `StepSheetState.Gone` renders 6d's `StepGoneContent` the same way the trip page does (copy 6d's host branch, do not re-implement). `StepSheetContent` already carries `RefusedChangeFeedback` as its bottom slot, so refusals show inside the sheet; nothing else to host.
- The hero opens the trip page with its default opening position (6b `openingDay`), not at a block (iOS `TripPageRoute(tripID:)`).

**Tests**
- `TodayNavigationTest` (`@Logic`, Robolectric, real nav host from `:app` with the debug route probe and a `FakeTodaySource`): tap `today.trip` → route `TripPage(tripId)` on the Today stack; system back → `Today`; tap `today.hidden` → Settings tab selected; tap `today.past` → My trips root; tap `today.create` → the wizard route.
- `TodayStepSheetTest` (`@Logic`): tap `today.step.actionable.<id>` → the sheet shows (6d's `step.done` tag); tap Done → `FakeTodaySource.setCheck(heroId, _, id, Done)` once; the fake then emits the mark → the row (still in the same section and position) shows the done icon and content description "…, done"; Undo → `setCheck(…, null)`.

### Task 6. Invalidation proof (`test(today): prove a minute tick recomposes only the clock leaves`) — Ticks: none

- `TodayInvalidationTest` (`@Logic`, Robolectric, `CountingTodayProbe` in `src/test`): render `TodayScreen` in the `Active` fixture state with 1 current and 4 upcoming unmarked rows and a `MinuteClock` over `mutableStateOf(now)`, where `now` is chosen so that no block starts or ends in the next minute. Snapshot counts; set `now + 60 s`; `waitForIdle()`. Assert: SCREEN, HERO, PANEL and every ROW delta = 0; each LEAF whose number changed delta = 1 (the 5 row subtitles, and the hero eyebrow when the next step has not started; pick `now` so it has not); marked rows have no LEAF at all.
- Negative control in the same class: passing `clock.now` as a plain `Instant` parameter to a test-only row variant makes its ROW delta 1 (proves the probe can fail).
- `TodayViewModelTest` already proves the structural state does not re-emit on a quiet tick (Task 3).

### Task 7. Journey and controls (`test(today): add the today journey and row control tests`) — Ticks: "**Tests**"

- `TodayControlTests.testEachRowStateKeepsOneLargeTapTargetAtXXLarge` (`@Layout`, port of `I/AppTests/TodayControlTests.swift`): the first actionable block as running unmarked, done and skipped rows; profile `small`, `nl`, `largeFont`: 3 rows, each height ≥ 56 dp and inside the 16 dp gutters (± 1 dp); each is exactly one clickable node.
- `TodayTests.testTodayOpensTheTripRouteAndReturnsToItsRows` (`@Smoke`, emulator `pixel`, `app/src/androidTest/…/today/`, port of `I/AppUITests/TodayTests.swift`): seeded launch `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1` (0e/4e `ScreenTest.launch`). `waitUntilExactlyOneExists(hasTestTag("today.trip"), 5_000)`; click; wait for `trip.scroll` (4 s); `Espresso.pressBack()` (system back); wait for `today.screen` (4 s); click `today.trip` again; predictive back with 6a's edge-swipe helper; wait for `today.screen`. Assert the route probe after each back. < 10 s. This is Today's only emulator journey.

### Task 8. Fully drawn (`perf(today): report fully drawn after the first today content`) — Ticks: "**TTFD**"

- In `TodayRoute`: `ReportDrawnWhen { uiState !is TodayUiState.Loading }`. 3b's `ReportDrawnWhen` in `CfRoot` stays: `FullyDrawnReporter` reports only when every active reporter is done, so a signed-in launch that lands on Today now reports at `max(hand-off end, first Today content frame)`, and launches that land elsewhere (gate screens, seeded routes, Welcome) are unchanged. Verify in the merged code that Today composes before the hand-off ends (it composes under the overlay, 3b §5.8); if it does not, report it as an OQ instead of moving 3b's reporter.
- `:benchmark` `StartupBenchmark`: add `startupColdSignedIn` (`StartupMode.COLD`, 10 iterations, perf variant, `CompilationMode.Partial(BaselineProfileMode.Require)`, `StartupTimingMetric()`), with the signed-in extras the module already uses (3b: `cf.session=in cf.stored=yes cf.gate=app`). It must report a finite `timeToFullDisplayMs`. If in the perf variant Today never leaves Loading (no session data behind the stub), add `src/perf/…/today/FixtureTodaySource.kt` serving `MockAppSeed`'s demo account at the device time, selected only when the session is the perf stub, as 6b's `cf.trip-page` entry does. Never fake the metric.
- `make perf`; append to `docs/perf-startup.md`: date, commit, `pixel` AVD API 36, refresh rate, build type `benchmarkRelease`, `timeToInitialDisplayMs` and `timeToFullDisplayMs` medians for `startupColdSignedIn`, and which Today source it used. Emulator numbers are reports, not device claims.
- `make baseline-profile`: the startup journey now ends on Today content (wait for `today.trip` or `today.empty`); commit the regenerated profiles.

---

## 7. Acceptance criteria

1. Commands pass on the branch (orchestrator runs what the sandbox cannot, with `pixel` booted for `make check`):
   - `make targeted ONLY="core:ui:TripCardEyebrowTest core:plan:TodayTests core:plan:TodayEdgeTests core:plan:TodayViewBudgetTest feature:today:TodayReducerTest feature:today:TodayReducerQuietTickTest feature:today:TodayViewModelTest feature:today:TodayScreenTest feature:today:TodayInvalidationTest feature:today:TodayNavigationTest feature:today:TodayStepSheetTest"`
   - `make quick` (< 60 s warm), `make check` (< 5 min warm), `Scripts/ci.sh` (no generated-file diff).
   - `make captures ONLY=Today,TodayEmpty,TodayFuture,TodayLoading,TodayCompleted,TodayRowStates` produces 24 PNGs in `build/captures/`.
   - `make perf` reports a finite `timeToFullDisplayMs` for `startupColdSignedIn`; `make baseline-profile` passes with `BaselineProfileMode.Require`.
   - If Task 1 moved code out of `:feature:trip`: `make perf-timeline` passes every emulator gate (the CPU band included); a ±25% flag is listed with its cause and does not fail.
2. **Behaviour:** the three ported iOS cases pass; a quiet minute tick produces no new `uiState` and recomposes only LEAF scopes (each changed leaf exactly once); a check-off from the Today sheet writes exactly one outbox row and the row stays in its section showing done; hero → trip page → system back and predictive back return to Today.
3. **Looks:** the five states match `Today`, `TodayEmpty`, `TodayFuture`, `TodayLoading`, `TodayCompleted` and the rows match `TodayRowStates`, in light and dark, en and nl, except the deltas recorded in `docs/board-deltas.md`. Guards pass at `small`, `pixel8`, `foldInner`, `largeFont`; cards are ≤ 640 dp and centred at `foldInner` and `landscapeSideNav`.
4. **Perf numbers:** TTFD signed-in is recorded in `docs/perf-startup.md` (emulator: report; reference device gate "hand-off end + ≤ 2 frames" stays open until a device is attached, OQ-A22). `todayView` on large60 ≤ 20 ms per sample on the JVM. StrictMode is clean on a debug launch to Today.
5. `plan.md` Phase 7 checkboxes ticked in the commits named above; every new string resolves in en and nl; no literal reaches `Text`; Konsist and Lint pass.

---

## 8. Review checklist (Opus reviewer)

- [ ] `todayView` matches §4.1 exactly: `next` from `plan.blocks` without the hidden filter; `importantHidden` from the **unstripped** plan; marks of either status exclude `next`; `upcoming` keeps plan order and caps at 4; `light_ok`/`light_neutral` excluded from current/upcoming; `ceil` (not round/floor) for minutes.
- [ ] `selectTodayTrip` re-derives status with `tripDisplayStatus` at the current tick; active trips sort by plan **end**, upcoming by plan **start**; id tie-break ascending; `lastEndedTrip` sorts descending by `min(end, now)`.
- [ ] `calendarDaysUntil` uses the plan/departure zone (not the device zone) and `LocalDate` difference; the completed card's date uses the **device** zone (iOS `TimeZone.current`).
- [ ] Loading requires `synced && tripsLoaded && formatter`; empty is "no trips", not "no hero".
- [ ] Rows open the step sheet, the hero opens `TripPage(tripId)` on the **Today** stack; "See past trips" and the hidden link **select tabs**, they do not push.
- [ ] No composable other than the listed leaves (and the shared card eyebrow) reads `clock.now`; no `Instant` parameter flows into `TodayScreen`, the panel or rows; `TodayUiState` has no now-derived text. `TodayInvalidationTest` has its negative control.
- [ ] `uiState` is computed on `default`, `distinctUntilChanged`, `WhileSubscribed(5_000)`; the clock is collected only while started (no leaked receiver: 1d's clock registers on collect).
- [ ] Check-off: one in flight at a time; `CancellationException` rethrown; no error UI added; no haptic.
- [ ] Marked rows: dimmed swatch 0.45, muted title, "Done · …"/"Skipped · …", trailing 28 dp icon, spoken "…, done"; no strike-through; no check control on the row; row ≥ 56 dp and one tap target.
- [ ] Copy uses the iOS keys (`Adjusting`, "Some important steps are hidden.", the two-sentence future note without the reminder promise); no board sample copy; Dutch present; plurals via `pluralStringResource`.
- [ ] Values: tokens or `TodayMetrics` only; no `MaterialTheme.colorScheme`; `@BoardMetrics` names existing boards; the 640 cap is 6a's `WindowPolicy.listCardMaxWidthDp`, not a new constant.
- [ ] `todayView`/`calendarDaysUntil` are 6a's (not re-ported); the sheet and the check-off use `selection.tripId`, not the current hero; trips are decoded only on record changes.
- [ ] `LazyColumn` keys are stable and unique (a block id can appear only once: current and upcoming are disjoint); `contentType` set; no `onSizeChanged` into state.
- [ ] Features do not depend on features; shared pieces moved (not duplicated) to `:core:ui` / `:core:data`, with My trips and the trip page still passing.
- [ ] TalkBack: headings on section titles and card titles; progress is one node with the long form; hero card is one button; icons hidden.
- [ ] `ReportDrawnWhen` added in Today; 3b's reporter not removed; `docs/perf-startup.md` row has device, build type and command; baseline profile regenerated and committed.
- [ ] Captures compared with all six boards in both themes and languages; every difference is fixed or listed in `docs/board-deltas.md`.
- [ ] Typical mistakes to look for: reading `now` in the screen to pick the hero (forces full recomposition each minute); formatting "In 30 min" in the ViewModel; using `Locale.getDefault()` for language; `rememberSaveable` for the sheet; `runCatching` around the check-off; re-sorting upcoming steps by time; dropping marked steps from the lists; using the stored trip status instead of the re-derived one; `Thread.sleep` in tests; one parameterised test that loops over profiles inside one case.

---

## 9. Open questions (proceed on the default)

| # | Question | Default |
|---|---|---|
| OQ-7-1 | The plan entry says rows "open the trip page at the block". iOS (`AppTabs.swift:444–447`, 17b note "Rows open the step sheet (RN behavior)") and the `TodayRowStates` board ("Tapping a done or skipped row opens the step sheet with Undo") open the step sheet over Today; only the hero opens the trip page. | iOS and board: row → step sheet; hero → `TripPage(tripId)` without a block. Recorded as plan.md OQ-A44 (review 2026-11-16). |
| OQ-7-2 | Board vs iOS look deltas: upcoming badge (board Outline, iOS Secondary); adapting pill text (board `foreground`, iOS `dawnText`); row title (board 16/22 500, iOS 16/24 400); swatch (board 28, iOS 32); loading 40% line (board yes, iOS no); future note (board one paragraph, iOS two lines); hidden link and completed card paddings (board); running subtitle (board shows the interval "→ 07:30 → 10:30", iOS "→ 10:30"); panel rows on the `Today` board use the `md` swatch (36, `bundle.js` `ie` → `ve` default) while `TodayRowStates` uses `sm` (28): build 28 (the dedicated row board); empty-state button (board `h-10` = `CfButtonSize.Lg` 40 visual, iOS `actionMinHeight` 48; the 48 dp touch target comes from `minimumInteractiveComponentSize`). | Looks from the boards; text content and copy from iOS (the running subtitle keeps iOS "→ {end}"). Record each in `docs/board-deltas.md`. |
| OQ-7-3 | Where do shared pieces live (cards, swatch, step sheet, capsule, check-off writer)? | Swatch, `MetaChip`, capsule (6a) and step-sheet content (6d) are already in `:core:ui`; Task 1 moves only `TripCard`/`TripCardUi` to `:core:ui` `…ui.trip`, adds `MinuteClock` (`…ui.clock`) and extracts `CheckOffWriter` to `:core:data` (`plan/`). |
| OQ-7-4 | The `Today` board's top-left 44 dp glass icon. | Not built (iOS has none; no behaviour defined). |
| OQ-7-5 | The hidden-steps link targets "Settings › Preferences", which arrives in 10b. | Select the Settings tab (iOS). Revisit in 10b only if iOS changes. |
| OQ-7-6 | Pull to refresh on Today has no board. | Build it (iOS behaviour, `AppTabs.swift:430–434`), reusing 6a's indicator and capsule. |
| OQ-7-7 | Step-sheet selection survival. | ViewModel state (survives configuration recreation); process death is a cold launch (A8). Not a route parameter, so 6a's tab-root and reselect logic stay untouched. |
| OQ-7-8 | TTFD in the perf variant if the stubbed session has no data. | Perf-only `FixtureTodaySource` from `MockAppSeed`; record which source produced the number. |
| OQ-7-9 | `todayView` budget is not in plan.md. | Android-only JVM guard: ≤ 20 ms per sample on large60. Add it to plan.md "Performance budget" as a JVM row. |
| OQ-7-10 | Plan retention lease for the hero plan. | None (iOS `PlanHomeModel` holds none; active and near trips are kept offline anyway). |
| OQ-7-11 | Progress bar: system `LinearProgressIndicator` or a drawn bar? | `CfLinearProgress` at 80 × 6 dp (system control, tinted); hidden from accessibility, the row speaks the summary. |
| OQ-7-12 | "Plan a trip" before the wizard lands (9c). | Shown (iOS since 16c) and routed to 6a's wizard seam. |
