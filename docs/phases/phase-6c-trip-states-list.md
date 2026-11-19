# Phase 6c: Trip page: list mode and states

- **Branch:** `feat/phase-6c-trip-states-list`, from the stack top once 6b is in it.
- **Depends on:** 6b (and through it 6a, 1a–1e, 0a–0e, 2, 4a–4e). **Wave:** 10. **Milestone:** M3.
- **Plan entry:** `plan.md` "Phase 6c: Trip page: list mode and states" (l.1943–1977). Related: OQ-A11 (more-button glyph `more_vert`, owned by 6c), standard acceptance (`plan.md` l.636–647), "Performance budget".
- **Tests:** every test you add follows AGENTS.md "Tests" (cheapest layer, budgets, exact queries, no sleeps, hang caps). Read it before Task 1.
- **API endpoint:** `GET /v1/trips/{trip}` only, through the existing `SessionSync.fetchDetail` (4d). Nothing new in `:core:network`.
- **Checked:** 2026-11-16 by an adversarial checker against iOS `f391a1fa`, the boards, web `e36368ec`, plan.md, AGENTS.md and the 4d/6b/6e briefs. Fixed: 6b's real seams (`TripPageUi`/`recompute()`, `TripListIndex`/`TripScrollMath` replace 1d's `TimelineListIndex`, content type `dayTimeline`, the non-ready seam `Box` without a list, `TripTopBar`, `SummaryClockState`), the page-level `trip.timeline.ready` marker that stays in list mode (tests and benchmark no longer wait for it to go), the path monitor (reuse 4d's `ConnectivitySyncPathMonitor`), iOS line ranges, missing inputs (iOS plan.md 15c), `Home`/`There` resource names, board colour delta D15.
- **Stack check (2026-11-19, against 87e4d7a):** P1–P11 exist. Deltas:
  - `TripPlanPipeline` is a `fun interface` implemented by `DefaultTripPlanPipeline` (`feature/trip/…/page/TripPlanPipeline.kt`).
  - `SessionSync.fetchDetail(tripId, session: StoredSession, epoch)` (`core/sync/…/SessionSync.kt:203`) delegates to `SyncEngine.fetchDetail(tripId)`. `events()` is at l.224 and `retention` at l.38.
  - `TripPagePriorityTag` was not ported by 6b. Port it here (the type spec `screenPriorityTag` and the opacity `priorityTagOpacity` exist).
  - 6b reads scroll state only through `snapshotFlow`, with synchronous `Snapshot.withoutReadObservation` seeds (1f7d790, 9c60131), guarded by `TimelineInvalidationTest#scrollingWithinOneDayDoesNotRecomposePageChrome`. List mode must keep that guard passing and must not read `LazyListState` in composition.
  - The 6b `trip` perf baseline is the 9c60131 row in `docs/perf-timeline.md`. OQ rows end at OQ-A271.
- **Implementation preflight (2026-11-19):** additional landed seams and current instructions:
  - `Route.TripPage.awaitingSync` already exists, and `ShellNav` already sets it for a saved trip. Add it only to `TripPageArgs` and forward the existing route value; preserve existing shell routing.
  - `TripPageViewModel` has no `hasLoaded` field yet. Set it after the first accepted source output; do not assume 6b supplied it.
  - `NowLineValue` and `projectDayClocks` live in `core/plan/…/trippage/DayClockProjection.kt`; `DayClock`/`DayClocks` live in `feature/trip/…/timeline/DayClock.kt`. Add the instant to the existing core value and update its callers.
  - `TimelineCategoryStyle` lives in `core/plan/…/drawing/`, with palette slots rather than Compose colours. Resolve those slots through the generated timeline palette.
  - In the landed tree `trip.timeline.ready` belongs to each `TimelineTargets`, not to a page-level marker. Keep those timeline semantics unchanged and emit the promised readiness marker only in ready list mode. Its presence means ready page content, never which mode is showing (OQ-A288).
  - `Modifier.skyBackground(dark: Boolean)` requires the current theme argument; pass it rather than adding another wrapper.
  - The narrow trailing test checks that the two elements stack. Pinned iOS emits the priority tag before the status icon, so the tag is above the icon; the older test wording below reverses that order. Add the specified 18 dp status size and 2.5 stroke through the generated Android metric overlay.
  - The auth heading is `CfTheme.type.style(CfTypeScale.authHeading)`, not an `authHeading` convenience property. Reuse that style. The native bar wrappers have no sky variant yet: add an optional sky treatment with unchanged defaults.
  - The shell has tab selection but no trip-error callbacks yet. Add callbacks that clear both Today and Trips stacks, then select the requested tab, exactly as pinned `AppTabs.tripDestination` does; existing navigation entry points keep their behaviour.
  - `Guards.assertAll` does not apply `exemptTags` to ink. The generated light skeleton fill differs from background by less than its 16-channel threshold. Preserve all UI values and existing guard defaults; add an optional ink threshold and use 1 only for PlanLoading, retaining the 0.5% area gate and proving absent shapes fail (OQ-A289).
  - Landed `timelineScroll` uses 15 `swipeTimeline` calls under bounded-swipe-v3 (five fixed drags each), not UiAutomator flings. Copy that current workload for listScroll/profile coverage, set the requested width/5 gesture margin, and keep the timeline benchmark unchanged (OQ-A290).
  - Generated Plan strings live in `core/ui/src/main/res/values{,-nl}/strings_ios.xml`; the expected resource names in §8 exist.
  - The current plan's standard acceptance overrides the older Cartesian guard matrix below: each board gets light guards at `small`/nl, `largeFont`/nl and `pixel8`/en; captures cover both themes. Wide/side-navigation guards belong to 6e. Parameter names must be unique.
  - Screen tests check tags, structure, semantics and callbacks only; copy variants are asserted in JVM `ListRowsTests` and generated-string tests. Do not add `TripRowAlpha` or any test hook to `src/main`; prove dimming with captured pixels if needed.
  - Fetch failures must also map the checked store/session/device exceptions and runtime/security failures the injected submit path can raise to `Failed`, with cancellation rethrown first (the user's 5b lesson).
  - The user runs `make baseline-profile`, `make perf-timeline` and `make check`; implement their inputs and leave measured results pending. Run targeted checks per task, and `make quick` once at the end.
- **Who builds on this phase:** 6d (step sheet opened from list rows), 6e (wide list column reuses the row composable and `ZoneChangeCard`; wide remote states reuse the state card, the phone skeleton and the remote summary; see `docs/phases/phase-6e-trip-wide.md` §2 row P5 and Task 5).

Tags: **[F]** a fact verified in a source (path and line). **[R]** a rule this brief sets for Android. Paths are abbreviated as in plan.md: `feature/trip/…/X.kt` = `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/X.kt`; `core/plan/…/X.kt` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/X.kt`. iOS paths are relative to `/Users/rickgout/Sites/chronoflow-ios` and read at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. `PU/` = `Packages/ChronoFlowKit/Sources/PlanUI/`.

---

## 1. Goal

Finish the phone trip page (`< 600 dp`) outside the ready timeline:

1. **List mode.** A view toggle in the top bar switches Timeline ⇄ List, keeps the current day, and is not persisted. In list mode each day's timeline item is replaced by one lazy item per block: an ordinary list row (port of `DayListView.row`) or a zone-change card (port of `ZoneChangeCard`). The running row reads "… · now".
2. **Every non-ready state** of `TripPageState`: `Loading` (skeleton), `NoPlan`, `RemoteLoading` and `RemoteOffline` (plan not on this phone, with the fetch, its retry and reconnect), `NotFound` and `Server` (full-screen error pages). A trip that is not on the phone after a finished sync resolves to `NotFound`, as on iOS. A `TripPageLoadingBar` covers the time before the ViewModel exists.
3. **Fetch and offline hint.** A port of the `TripPageModel` fetch logic (`online`, `fetch`, `retry`, reconnect). The `TripPagePathMonitor` port is 4d's `ConnectivitySyncPathMonitor`, one instance per page (Task 2).
4. **Perf.** A `listScroll` macrobenchmark: 15 flings over the 740 large60 rows, with the `timelineScroll` metrics and emulator gates.

The phase is done when the JVM state and fetch tests pass, every state and both list boards have Robolectric guards and captures, `listScroll` passes the emulator gates, and the 6b timeline numbers have not moved.

---

## 2. Before you start: check what 6b and earlier phases left

Do not rebuild earlier phases. Find each name below. If a name differs, use the existing one and say so in your commit body. If an item is missing (not just renamed), stop and report it to the orchestrator.

| # | Expected (phase) | Where (expected) | Used for |
|---|---|---|---|
| P1 | `enum class TripPageState { Loading, RemoteLoading, RemoteOffline, Ready, NoPlan, NotFound, Server }` with `companion fun resolve(keepOffline, stored, noPlan = false, online, fetch, missingAfterSync = false, tombstoned = false, awaitingSync = false)`; `enum class TripPageFetch { Idle, Loading, Loaded, Offline, NotFound, Failed }`; `TripPageAnchor`, `openingDay`, `replacementAnchor`; tests `TripPageTests.stateGatingTable`, `TripPageStateTests` (1b Task 9) | `core/plan/…/trippage/` | state resolution |
| P2 | `PreparedPlan` / `PreparedDay` / `PreparedBlock(block, title, interval, priority, reason, instructions, accessibilityLabel, status: CheckStatus?, continuesBefore, continuesAfter, zoneInstruction, zoneComparison)`, `PageCopy(language)(key, args)`, `clockShiftText(adaptation, formatter, options)`, `tripMetaText(...)`, `PlanMetrics` (generated), `LargeTimelineFixtures` (`large60`: 60 days, 740 rows of which 20 are zone rows) (1a/1b) | `core/plan/…`, `core/testing/…` | rows, copy |
| P3 | `DayClock` / `DayClocks` / `NowLineValue(offset, text)` / `projectDayClocks`, `TimelineProbe` (`Kind { STATIC, CLOCK, TARGETS, HEADER, ANCHOR }`, `LocalTimelineProbe`, `CountingTimelineProbe`), `TimelineInvalidationTest` (1d) | `feature/trip/…/timeline/…` | now marker, probe |
| P4 | `TimelineJankTagger.onVisibleDay(index, mode)`, `TimelineBenchmark` (`timelineFirstFrame`, `timelineScroll`, `timelineLastDay`), `benchmark/budgets.json`, `Scripts/check-benchmarks.py --suite timeline`, `make perf-timeline`, `make baseline-profile`, `docs/perf-timeline.md` (1e) | `feature/trip/…`, `benchmark/`, `Scripts/` | perf |
| P5 | `TripPageViewModel` (prepares off main with a revision guard, holds a `PlanRetention` lease, anchor `(dayId, offsetInDay)`, `select(dayId, offset)`, `ui: StateFlow<TripPageUi(state, title, plan)>` resolved in one private `recompute()` with fixed `online = true`/`fetch = Idle` inputs that 6c replaces, `hasLoaded`, `refresh()`), `TripPlanPipeline`/`PipelineOutput(trip, title, noPlan, plan)`, `TripPageArgs(tripId, dayId, blockId, startAtSummary)`, `TripListIndex` (`…/page/`: `summary` 0, `days` 1, `headerIndex`, `timelineIndex`, `end`, `hidden`, `dayAt`, `itemCount`; it replaced 1d's `TimelineListIndex`) and `TripScrollMath` (`currentDay`, `headerBottomPx`, `headerScrolled`, `offsetInDayPx`, `restoreTarget`), `TripTopBar` (non-ready title tag `trip.title` with the IATA title or "Loading your plan"; `trip.actions.open` with `MaterialSymbols.MoreVert`), `SummaryClockState` + `DualClockValue` (already supports `remote`), the non-ready seam (`Box` tagged `trip.page` holding a `trip.timeline.preparing` node, 6b Task 11 step 5), the page-level zero-size `trip.timeline.ready` marker (1e), the `TripPageSource` seam (DB-backed and fixture-backed), the page composable with its `LazyListState`, top bar (title menu `trip.dayMenu`), summary card (`trip.summary`) with `PlanCard` and `DualClocks`, `DayNavigation`, designed `DayHeader`, compact header overlay (`trip.compactHeader`), `LaneHeadings`, end card `trip.plan.end`, `CfPullToRefreshBox` around the page with `refresh`, perf entry `cf.trip-page=…`, seed `CF_PLAN_TEST_POSITION=summary` and the debug-options holder that reads it (6b) | `feature/trip/…` | page host |
| P6 | Shell navigation callbacks for "go to Trips" and "go to Today", and the `TripPage(tripId)` route (6a) | `app/…`, `feature/*/…` | error-page buttons |
| P7 | `SessionSync` (`fetchDetail(tripId, session, epoch)`, `events(): Flow<SyncEvent>` replaying the latest event, `retention`), `SyncEvent.Finished(accountId, epoch, result)`, `SyncState.Idle`, `SyncPathMonitoring { fun updates(): Flow<Boolean>; suspend fun stop() }`, `PlanRetention`/`PlanLease` (4d); `ConnectivitySyncPathMonitor(context)` (4d Task 9: current state first, `INTERNET && VALIDATED`, `distinctUntilChanged`, `stop()` closes the producer); `SessionSnapshot(session, epoch)` (4e); `DataFailure` with `Offline` and `Server(status: Int?)` (4b); `TripRecord(…, keepOffline, …, planEndDate?, deletedAt?, deletedLocally)` (4a/4d) | `core/sync/…`, `core/data/…`, `core/network/…` | fetch, offline hint |
| P8 | `CfButton(text, onClick, modifier, style, size, enabled, …, fillWidth, minHeight)`, `CfIconButton`, `CfSkeleton(modifier, shape)`, `CfTopAppBar`, `CfBackButton`, `CfPullToRefreshBox`, `Modifier.skyBackground()`, `cfClickable`, `CfTheme.type.*` (`planCardTitle`, `planRemoteTitle`, `planTitle`, `planListTitle`, `planListLine`, `planSubtitle`, `planPriority`, `authStrong`, `authHint`, `authBody`, `authHeading`, `eyebrow`, `welcomeWordmark`, `tnum()`), `LucideGlyphs` + `LucideGlyph.imageVector(strokeWidth)`, `MaterialSymbols` (`ArrowBack`, `MoreVert`, `ViewAgenda`, `ViewTimeline`), `tools/icons/lucide.json` (2) | `core/designsystem/…`, `tools/icons/` | components |
| P9 | `TimelineCategoryStyle` (fill, foreground, pattern, dashed, glyph per `BlockType`) and its dot/hatch pattern drawing (1c) | `feature/trip/…/timeline/…` | swatch |
| P10 | `ProfileRule`, `ProfileHost`, `Guards`, `BoardCapture`, tiers `@Logic`/`@Layout`/`@Capture`/`@Smoke`, `make captures ONLY=…`, `make targeted ONLY=…` (0e) | `core/testing*/` | tests |
| P11 | Generated Plan strings (0c), resource names in `tools/l10n/names.lock.json` | `core/strings` (or where 0c put them) | copy |

If 6b already ported something this brief asks for (for example `PlanCard`, `TripPagePriorityTag`, `DualClocks(remote = …)`, the trip-actions button with `more_vert`, or the non-ready title), reuse it and say so in the commit body. Do not create a second copy.

---

## 3. Inputs to read

**This repo**
- `AGENTS.md` (all), `plan.md` l.167–180 (conflict order), l.636–647 (standard acceptance), l.1898–1977 (6b and 6c).
- `docs/research/04-timeline.md`: §3.14 (list mode, l.562), §3.18 (states, l.616), §4.7 (toggle, l.743), §5 (accessibility), §7.4 (large60 list height, l.871), §9.5 item table (l.~1070–1090), §9.11 (l.1209), §9.14 (l.1249), §10.2 (invalidation, l.1341), §10.3 (benchmarks, l.1353), OQ-T18 (do not persist the toggle).
- `docs/research/screens/ios/05-trip-list-mode.png` (iOS list mode).
- 6b brief Tasks 3 (`TripPlanPipeline`, `TripPageViewModel`, `recompute()`), 4 (`TripListIndex`, `TripScrollMath`), 5 (`SummaryClockState`, `DualClockValue`), 9 (`TripTopBar`) and 11 (page assembly, the lazy item table, the non-ready seam). These are the code you change.
- Briefs: `phase-1b-timeline-layout.md` Task 9 and §Large fixtures, `phase-1d-timeline-host.md` §5 and Tasks 2, 5–7, `phase-1e-timeline-perf.md`, `phase-2-design-system.md` Tasks 10–11 and the `CfSkeleton`/`CfTopAppBar`/`CfPullToRefreshBox` sections, `phase-4d-sync.md` (SessionSync, SyncEvent, PlanRetention), `phase-4b-api-client.md` (DataFailure mapping, l.~410–425), `phase-0e-test-harness.md`, the 6b brief (`docs/phases/phase-6b-trip-page.md`), and `phase-6e-trip-wide.md` §2 row P5 and Task 5 (what 6e expects from you).

**iOS at `f391a1fa`**
- `plan.md:1692` "15c: Trip page frame and list view" (state rules: remote states only for non-keepOffline trips without a stored plan; `awaitingSync`; missing after sync → ErrorNotFound, OQ 35) and `plan.md:1498` "Trips gaps and risks" (plan.md 6c lists both).
- `PU/TripPage/DayListView.swift` (whole, 133 lines: `interval`/`rowLabel` l.47–59, `row` l.61–86, `trailing` l.88–96, `ZoneChangeCard` l.100–131).
- `PU/TripPage/TripDayRow.swift` l.35–140 (phone list = `VStack(spacing: 0) { DayHeader; DayListView }`; `DayClockState.now`).
- `PU/TripPage/TripPageLoadingBar.swift` (whole).
- `PU/TripPage/TripPagePathMonitor.swift` (whole).
- `PU/TripPage/TripPageState.swift` (whole; already ported in 1b).
- `PU/TripPage/TripPageModel.swift` l.17–37 (`online` didSet, `syncCompleted`, `timeline`, `reconnectPending`), l.68–75 (`state`), l.77–108 (`start`), l.110–137 (`retry`), `stop`.
- `PU/TripPage/TripPageView.swift` l.60–72 (error vs page switch), l.118–158 (state content), l.226–262 (toolbar, toggle, title), l.287–291 (`codeTitle`), l.351–392 (compact header, `compactDayTitle`), l.438–456 (`remoteSummary`), l.458–468 (`skeleton`), l.470–479 (`noPlan`), l.481–492 (`remoteOffline`), l.494–516 (`stateCard`), l.518–537 (`errorPage`).
- `PU/TripPage/PlanAppearance.swift` l.49–57 (`TripPagePriorityTag`), l.59–67 (`PlanCard`), l.93–150 (`TripPageCategorySwatch`).
- `PU/TripPage/TripPageComponents.swift` (`DualClocks`, `remote` labels).
- `PU/Models/TripPlanModel.swift` l.70–100 (`hasLoaded`, `onTripAvailable`), l.169–178 (`refreshMetadata`).
- `App/TripPageDestination.swift` l.15–22 (`TripPageRoute.awaitingSync`), l.70–92 (`makeModel`, `:list` seed), l.175–184 (`syncCompleted`).
- `App/AppTabs.swift` l.183, l.359 (who sets `awaitingSync = true`: the wizard save and the pending-link continuation; both arrive in later phases).
- `Packages/ChronoFlowKit/Sources/PlanCore/PlanMetrics.generated.swift` (values below).
- Tests: `Packages/ChronoFlowKit/Tests/PlanUITests/TripPageFetchTests.swift` (whole), `TripPageTests.swift` (`stateGatingTable`, already ported).

**Boards** (`_design/canvas/`; every board has `theme` light/dark)
- `PlanList.dc.html` (`Plan view=list from-top day=1`, 390 × 2600), `PlanListScrolled.dc.html` (`view=list scrolled`, 390 × 844).
- `PlanLoading.dc.html` (`Plan loading`), `PlanNoPlan.dc.html` (`Plan no-plan`); both import `Plan.dc.html`, which holds the markup (search `isList`, `noPlan`, `loading`).
- `PlanRemote.dc.html` (`state` = `loading` (default) | `offline`, `lang`), `PlanRemoteLoadingNL.dc.html`, `PlanRemoteOffline.dc.html`, `PlanRemoteOfflineNL.dc.html`.
- `ErrorNotFound.dc.html` (`kind` = `notfound` (default) | `server`, 375 × 812), `ErrorServer.dc.html`.

**API** (web at `e36368ec`, read-only)
- `GET /v1/trips/{trip}`: `routes/api.php:100`, `->middleware('verified')->whereUuid('trip')`. 200 → `ApiTripDetail {trip, plan, notices, timeline, checkState, detailVersion}`; 404 when the trip is unknown, deleted or not the caller's. Called only through `SessionSync.fetchDetail` (4d), which writes the detail to the store; the page then sees it through its source flow.
- `keepOffline`: `app/…/ReadMobileTrips.php:25–31` with `config/offline.php` `mobile_prefetch_days` (default 7). Active trips and trips starting in the next 7 days arrive with `keepOffline = true`; others are "remote" until fetched.

---

## 4. Out of scope (do not build)

- Wide layout (≥ 600 dp): the list column beside the timeline, `WidePlanSkeleton`, the wide state-card variant and the selected-row outline. All are 6e. Your composables must leave room for 6e's additions (§6 Task 5 and Task 7 name the parameters).
- The step sheet, legend sheet and trip-actions sheet (6d). A row tap calls the same `onBlock(blockId)` callback 6b's timeline uses; if 6b has no handler yet, pass a no-op and leave it to 6d.
- Making a plan from the no-plan card ("Check answers and make plan"). The button is shown disabled, as on iOS (`onMakePlan` is never set by `TripPageDestination`); the wizard hookup comes with 9c/9h.
- Setting `awaitingSync = true` from the wizard or from link continuation (9c and later). You add the route parameter and its handling only.
- Pull-to-refresh feedback capsule and announcements (owned by whoever built 6b's refresh; you only reuse it on the new states).
- Persisting the view mode (OQ-T18: not persisted, not even in `SavedStateHandle`).
- New copy. Every string here exists in the iOS catalogs and was generated by 0c.

---

## 5. Rules fixed by this brief [R]

1. **Source order.** Behaviour, element presence, copy and accessibility come from iOS at the pin. Visual values: where iOS sets an explicit value (a `PlanMetrics` constant, a `Typography` style, a color token), iOS wins over the board, because the conflict order puts iOS before boards (`AGENTS.md` l.17, `plan.md` l.167–176) and 6e already builds on the iOS values. Where iOS uses a stock control or system default (an `.borderedProminent` button, an SF Symbol at default size), the board's look wins. Glyphs come from the boards (lucide) for designed surfaces and from Material Symbols for system chrome (top bar). Every board difference that remains is in the table in §5.1. The reviewer records them; do not "fix" them toward the board.
2. **No formatting in composition.** Row strings, labels and the "now" variants are built off the main thread in `ListRows` (Task 4).
3. **One lazy item per list row**, keyed `r:<dayId>:<blockId>` (ordinary row) or `z:<dayId>:<blockId>` (zone card), content types `listRow` / `zoneRow`. The day id is part of the key because a block that crosses midnight appears in two days (`continuesBefore`/`continuesAfter`), and a duplicate key crashes `LazyColumn`. (Research §9.5 and 6e write `r:<blockId>`; 6e only needs that these items are absent on wide.)
4. **The clock stays narrow.** A row reads its day's `DayClock` inside `derivedStateOf`, so a minute tick recomposes only a row whose running state flips.
5. **The toggle state lives in the ViewModel** (`timeline: StateFlow<Boolean>`, default `true`), survives configuration changes (A8, in-place) and is never saved.
6. **Copy** comes from the generated Plan strings resources only (`stringResource`) for chrome and state pages, and from `PageCopy` for text built off main in `ListRows` (as 1b/6b do for prepared text). Expected resource names are listed in §8; confirm each in `tools/l10n/names.lock.json` by its `<!-- plan: … -->` comment before use.
7. **Online hint uses VALIDATED.** The page's path monitor (4d's `ConnectivitySyncPathMonitor`) reports `INTERNET && VALIDATED`, as 4d's `ConnectivitySyncPathMonitor` does (iOS `.satisfied` has no validation step). A captive portal therefore shows `RemoteOffline` without a request. Recorded as OQ-6c-9.
8. **Errors and cancellation.** Rethrow `CancellationException` everywhere. A fetch never runs on the main thread (`SessionSync.fetchDetail` is already off main; do not wrap it in `runBlocking`).

### 5.1 Board differences kept on purpose

| # | Surface | iOS (built) | Board | Why |
|---|---|---|---|---|
| D1 | List row title | `authStrong` 600 14/20 | 16/22 500 | iOS explicit style |
| D2 | List row interval | `planListLine` 13/18 tnum | 14/20 tnum | iOS explicit style |
| D3 | List row padding | horizontal `blockIcon` 14, vertical `sectionGap` 12; no min height (≈ 66 dp) | 8/12, min height 64 | iOS explicit metrics |
| D4 | Space between `DayHeader` and the first row | 0 (`VStack(spacing: 0)`) | 8 (`padding-top`) | iOS explicit; research §7.4 counts (740 − 60) × 8 of gaps |
| D5 | Zone card | `ZoneChangeCard` (pin circle 44, plates, sky) | board zone row | iOS explicit metrics |
| D6 | No-plan card | iOS `stateCard`: icon circle 44, padding 16/16, dashed 1 dp, dash 4, title `planCardTitle` | circle 56, padding 32/20, 2 px dashed, margin-top 24, Fraunces 24/32 | iOS explicit metrics; 6e extends this card |
| D7 | Remote offline card | iOS `stateCard` (44 circle, padding 16/16, 1 dp dash 4) | circle 52, padding 28/20, body 15/22 max 420, margin-top 12 | iOS explicit (the 52/28/420/+12 values are iOS's **wide** variant, built in 6e) |
| D8 | Remote loading | iOS phone skeleton 120/44/380 with the "Downloading…" line above | spinner + text, 3 pills 88 × 44, 64 bar, 360 lane grid | the board draws iOS's `WidePlanSkeleton` (6e) |
| D9 | Error page text | `eyebrow` (not uppercased), `authHeading` (Fraunces 36/40), `authBody` in `skyTitle`, no plate, no moon, no dots | `SkyFrame`: moon 40 in dawn, dot pattern 20 %, uppercase dawn eyebrow, Fraunces 30/36, body plate | iOS explicit styles; see OQ-6c-2 |
| D10 | Error page buttons | (board look) Dawn pill 40 + OnSky pill 44, sides 24, bottom 48, gap 8 | same | iOS uses stock `.borderedProminent` and plain buttons, so the board wins |
| D11 | No-plan primary button | (board look) `CfButton` Primary, min height 48, full width, **disabled** | enabled-looking primary 48 | iOS stock button; disabled per iOS behaviour (OQ-6c-4) |
| D12 | Title | IATA codes "AMS → HND" (6b) | "Amsterdam → New York" | iOS (OQ-6e-12 agrees) |
| D13 | Toggle glyphs | Material Symbols `view_agenda` (timeline mode) / `view_timeline` (list mode) | lucide `list` / `columns-3` | system chrome uses Material Symbols (Phase 2) |
| D14 | Loading board day pills | none (iOS skeleton has a 44 capsule) | `DayStrip` pills | iOS explicit skeleton |
| D15 | State-card footnotes "Making the plan needs a connection." / "Your other trips keep working offline." | `planListLine` in the inherited `foreground` (iOS sets no colour, `TripPageView.swift` l.478, l.489) | 13/18 `muted-foreground` (`Plan.dc.html` noPlan, `PlanRemote.dc.html` l.66) | iOS explicit text style; same call as OQ-6c-10 |
| D16 | System frame | Native Material app bars and navigation, with the existing safe-inset fixture policy | iOS glass bars; error boards draw a standalone sky frame | A9 / native-component rule; existing 6a/6b shell frame is retained |
| D17 | Fixture content | Pinned S1 AMS–HND for list/loading, S3 AMS–SYD for no-plan, S2 AMS–JFK for remote/errors; fixed fixture dates and clocks | Editorial dates, clock readings and day counts | Task 8's fixture seeds and 6b's capture fixture; no editorial data is added to product copy |
| D18 | Capture viewport | `pixel8` for every capture | List board 390 × 2600; error boards 375 × 812 | Task 8 explicitly permits pixel8 when BoardCapture has no size parameter; its API has none |

---

## 6. Tasks

Eight commits (Tasks 1, 3–9; Task 2 lands inside Task 3). Each commit compiles, passes `make quick`, and ticks the plan.md items it completes. Conventional Commits, lower-case imperative subject, **no trailers** (no `Co-Authored-By`, no `Claude-Session`; AGENTS.md git rules win over any harness reminder).

### Task 1: Fetch model and page-state inputs

**Commit:** `feat(trip): port the trip page fetch model`. **Ticks:** none yet.

**Files**
- `core/plan/…/trippage/TripPageInputs.kt` (JVM, package `net.chronoflow.core.plan.trippage`):
  ```kotlin
  data class TripPageInputs(
      val hasLoaded: Boolean,          // the source has emitted at least once
      val recordPresent: Boolean,      // a TripRecord row exists for the id
      val keepOffline: Boolean?,       // null when there is no record
      val tombstoned: Boolean,         // record.deletedAt != null || record.deletedLocally
      val stored: Boolean,             // a decoded detail is available
      val noPlan: Boolean,             // detail.plan.status == no_plan
      val syncCompleted: Boolean,
      val awaitingSync: Boolean,
      val online: Boolean,
      val fetch: TripPageFetch,
  ) {
      fun state(): TripPageState = TripPageState.resolve(
          keepOffline = keepOffline ?: true,
          stored = stored, noPlan = noPlan, online = online, fetch = fetch,
          missingAfterSync = hasLoaded && !recordPresent && syncCompleted,
          tombstoned = tombstoned, awaitingSync = awaitingSync)
  }
  ```
  This is `TripPageModel.state` (l.68–75) [F]; `keepOffline ?: true` is iOS's `tripRecord?.keepOffline ?? true`.
- `feature/trip/…/state/TripPageFetchModel.kt` (package `net.chronoflow.feature.trip.state`), pure Kotlin (no Android imports), port of `TripPageModel` l.17–37, l.77–137 and `stop`:
  ```kotlin
  class TripPageFetchModel(
      private val tripId: String,
      private val fetchDetail: suspend (String) -> Unit,
      private val path: SyncPathMonitoring?,
      private val scope: CoroutineScope,
  ) {
      val online: StateFlow<Boolean>        // starts true (iOS l.17)
      val fetch: StateFlow<TripPageFetch>   // starts Idle
      fun setOnline(value: Boolean)
      fun start()
      fun retry()
      fun stop()
  }
  ```

**Behaviour** (each line is iOS behaviour [F] unless marked)
- `setOnline(value)`: `old = online.value`; set it. If `value && !old && started`: if a fetch job is active set `reconnectPending = true`, else `retry()`. (`started` stands for iOS `lease != nil`; the lease itself stays in 6b's ViewModel.)
- `start()`: idempotent while started; re-runnable after `stop()`. Sets `started`. If `path != null`, launch one collector, waiting for previous stop cleanup before `path.updates().collect { setOnline(it) }`. Then `retry()`, resetting fetch to Loading or Offline for this appearance as iOS does.
- `retry()`: if a fetch job is active, return. If `!online.value`: `fetch = Offline`, return (no request). Else `fetch = Loading` and launch the job:
  - `fetchDetail(tripId)` returns → `fetch = Loaded`.
  - `CancellationException` → rethrow with no state change only when the page's own coroutine is cancelled (`!currentCoroutineContext().isActive`). Cancellation from sync/session work while the page job is active → `Failed`, then run the normal finally/reconnect logic (iOS's `guard !Task.isCancelled` and default error branch).
  - `DataFailure.Offline` → `Offline`; a `DataFailure` whose HTTP status is 404 (`DataFailure.Server(404)`) → `NotFound`; any other `DataFailure`, `IOException` or `SerializationException` → `Failed` [R: iOS's `default` branch]. Do not catch `Throwable` or `Error`.
  - In `finally` (not on cancellation): clear the job; if `reconnectPending`: clear it and, if `online.value`, `retry()`.
- `stop()`: cancel the path collector and the fetch job, clear `reconnectPending`, `started = false`, and call `path?.stop()` from a `NonCancellable` launch on `scope` (it is `suspend`). Join previous cleanup and the cancelled path collector there before stopping the monitor, so a quick reappearance cannot lose its new registration to older cleanup. `fetch` keeps its last value until the next start/retry.
- All state writes happen on the `scope`'s dispatcher (the ViewModel's `viewModelScope`, main). `fetchDetail` itself switches to IO inside 4d.

**Edge cases**
- `start()` called while offline: `fetch = Offline`, zero requests (iOS test below, 10 repeated starts).
- `setOnline(true)` when already true: nothing (no request).
- A reconnect during a fetch: one more request after the fetch ends, only if still online.
- `stop()` during a fetch: the job is cancelled, `fetch` stays `Loading` (iOS: `guard !Task.isCancelled` skips the write).

**Tests** (JVM, `@Logic`, `kotlinx-coroutines-test` `runTest(timeout = 60.seconds)` + `StandardTestDispatcher`; < 1 s each)
- `core/plan/src/test/…/trippage/TripPageInputsTests.kt`:
  - `noRecordDefaultsToKeepOffline`: `recordPresent=false, keepOffline=null, stored=false, fetch=Offline, online=true` → `Loading`.
  - `missingAfterSyncNeedsLoadAndSync`: `hasLoaded=true, recordPresent=false, syncCompleted=true, awaitingSync=false` → `NotFound`; same with `hasLoaded=false` → `Loading`; same with `syncCompleted=false` → `Loading`; same with `awaitingSync=true` → `Loading`.
  - `tombstoneWinsOverStoredDetail`: `tombstoned=true, stored=true` → `NotFound`.
  - `storedNoPlan`: `stored=true, noPlan=true` → `NoPlan`; `stored=true` → `Ready`.
- `feature/trip/src/test/…/state/TripPageFetchTests.kt` (Swift class name kept; fakes: `DetailRequests` counting calls and throwing `DataFailure.Offline` on the first, as iOS l.11–17; a `FakePath : SyncPathMonitoring` over a `Channel<Boolean>`):
  - `injectedOfflinePathAndClockStayFrozenAfterStarting` (port; the iOS test's clock assertion `model.plan.now == now` belongs to 6b and is dropped: name the dropped half in the commit body): `setOnline(false)`, `start()`; 10 × (`runCurrent()`, `start()`): `online == false`, inputs with `recordPresent=true, keepOffline=false` resolve to `RemoteOffline`, `requests.count == 0`. Then `setOnline(true)` → eventually `fetch == Offline`, state `RemoteOffline`, count 1. `retry()` → `fetch == Loaded`, state `RemoteLoading` (stored is still false), count 2.
  - `offlineFailureReconnectAndLivePreferences` (port of the fetch half; the preferences half belongs to 6b and is dropped): `start()` with `online=true` and `awaitingSync=true`, no record → `fetch == Offline`, `online == true`, state `Loading`. Record appears (`recordPresent=true, keepOffline=false`, `awaitingSync` cleared, as Task 3 does) → `RemoteOffline`. `setOnline(false)`, `setOnline(true)` → `fetch == Loaded`, count 2. `setOnline(true)` again → count still 2.
  - `awaitingSyncOnlySuppressesMissing` (port, whole): for `Offline/Failed/NotFound/Loading`, `resolve(keepOffline=false, stored=false, online=true, fetch, missingAfterSync=true, awaitingSync=true)` → `RemoteOffline/Server/NotFound/RemoteLoading`.
  - `notFoundAndServerMapping` (new): `Server(404)` → `NotFound`; `Server(500)`, `Server(null)` and an `IOException` → `Failed`.
  - `reconnectDuringFetchRetriesOnceAfterIt` (new): a gated fetch in flight; `setOnline(false)`, `setOnline(true)` → no second request yet; release the gate with an `Offline` failure → exactly one more request.
  - `stopCancelsFetchAndPath` (new): `stop()` during a gated fetch → the fake's coroutine sees cancellation, `fetch` stays `Loading`, `FakePath.stopped == true`, a later `setOnline(true)` makes no request.
  - `foreignCancellationIsFailed` (Android-only, renamed from `cancellationIsRethrown` in review H1): a fetch that throws `CancellationException` while the page coroutine is active sets `fetch == Failed`. This was never a Swift test name; the ported Swift names stay unchanged. `foreignCancellationHonorsPendingReconnect` proves the normal reconnect path still runs. Page-owned cancellation remains covered by `stopCancelsFetchAndPath`.

### Task 2: The page's path monitor (`TripPagePathMonitor`)

**Commit:** none of its own: the wiring below lands in Task 3's commit (it needs Task 3's ViewModel parameter). **Ticks:** none.

iOS `TripPagePathMonitor.swift` (whole, 29 lines) [F]: an actor wrapping a fresh `NWPathMonitor`; `updates()` first calls `stop()`, returns an `AsyncStream<Bool>` buffering the newest value, yields `path.status == .satisfied`; `stop()` cancels the monitor and finishes the stream. Each `TripPageModel` gets its own instance (`path: TripPagePathMonitor()` default argument).

4d Task 9 already ports exactly this as `ConnectivitySyncPathMonitor(context)` in `:core:data` (current state first, `INTERNET && VALIDATED`, `onLost` → false, `distinctUntilChanged()`, `stop()` unregisters and closes the producer, a later `updates()` registers afresh) with its own `ConnectivitySyncPathMonitorTest`. **Do not write a second monitor.**

**Files:** the place 6b's ViewModel factory builds its dependencies (the `TripPage` destination in `:app`, or 6b's factory): add `makePath: () -> SyncPathMonitoring?`, production value `{ ConnectivitySyncPathMonitor(appContext) }` (a new instance per page, as iOS), fixture/perf/Robolectric sources `{ null }` or a fake.

**Behaviour**
- Check 4d's class first. If its `updates()` collects `ConnectivityManager` on the caller's dispatcher, wrap the flow at the call site with `.flowOn(io)` (the IO dispatcher from `AppGraph`) so nothing touches `ConnectivityManager` on main (StrictMode stays clean). Change nothing inside 4d's class.
- If (and only if) 4d's class lacks one of the behaviours listed above, stop and report it to the orchestrator; do not patch it here.

**Tests:** none new (4d's `ConnectivitySyncPathMonitorTest` covers the monitor; Task 1 and Task 3 use a `FakePath`). Record in the commit body that `TripPagePathMonitor` maps to 4d's class.

### Task 3: ViewModel wiring: state, sync completion, awaiting sync, view mode

**Commit:** `feat(trip): resolve the trip page state in the view model`. **Ticks:** none.

**Files:** 6b's `TripPageViewModel`, 6b's `TripPlanPipeline`/`PipelineOutput` and `TripPageSource` (DB and fixture implementations), 6b's `TripPageArgs` and the `TripPage` route (6a), the ViewModel factory (Task 2's `makePath`), the debug-options holder that 6b uses for `CF_PLAN_TEST_POSITION`.

**Behaviour**
- **Route:** reuse `TripPage.awaitingSync`, add `awaitingSync: Boolean = false` to 6b's `TripPageArgs`, and pass it to the ViewModel (iOS `TripPageRoute.awaitingSync`, `TripPageDestination.swift` l.19, l.81). Preserve the already-landed `ShellNav` caller that sets it to `true`.
- **Source:** make sure 6b's `PipelineOutput` carries what `TripPageInputs` needs, plus the page data the states show. If 6b's output already has a field, reuse it (6b has `trip: TripRecord?`, `title`, `noPlan`, `plan`; add `hasLoaded` to the ViewModel):
  - `hasLoaded` (true after the first DB emission, iOS `TripPlanModel` l.82);
  - the record facts: `recordPresent`, `keepOffline`, `tombstoned` (`deletedAt != null || deletedLocally`);
  - `stored` and `noPlan` (`detail.plan.status == PlanStatus.NoPlan`, wire value `"no_plan"`);
  - `card` (from, to, iata/city), `adaptation`, and `metadata` = `tripMetaText(card.departLocal, detail?.plan.days.lastOrNull()?.labelDate ?: record.planEndDate, flights, language)` where `flights` = detail legs that are not return legs with a flight number, else `card.flightNumber` (iOS `TripPlanModel.refreshMetadata`, l.169–178); `""` when there is no card. Build it off main with the rest of the source mapping.
- **ViewModel:**
  - owns one `TripPageFetchModel(tripId, fetchDetail, path = makePath(), viewModelScope)`; `fetchDetail = { id -> sessionSync.fetchDetail(id, snapshot.session, snapshot.epoch) }` (iOS `TripPageDestination` l.81–84). Fixture sources pass a `fetchDetail` that throws `DataFailure.Server(404)` unless the fixture has the trip, or a test lambda.
  - `start()` order (iOS `TripPageModel.start`): 6b's lease and source collection first. `runFetch()` starts the fetch model and suspends until cancellation, stopping it in `finally`. `TripPage` runs this alongside `runClock()` inside `repeatOnLifecycle(STARTED)`, matching iOS appearance/disappearance. A retained off-screen ViewModel does not monitor paths or fetch, and reappearing re-fetches a missing plan. `onCleared()` keeps `fetchModel.stop()` as final cleanup before releasing the lease.
  - `awaitingSync`: a `MutableStateFlow(route.awaitingSync)` set to `false` the first time an emission has `recordPresent` (iOS `onTripAvailable`).
  - `syncCompleted`: a `MutableStateFlow(false)` set to `true` on any `SyncEvent.Finished` from `sessionSync.events()` whose `epoch == snapshot.epoch` and `result.state == SyncState.Idle` (iOS l.175–184). `events()` replays the latest event, so a sync that finished before the page opened counts. Fixture sources: `syncCompleted = true` from the start.
  - **State:** change 6b's single private `recompute()` to build `TripPageInputs(hasLoaded, recordPresent = out.trip != null, keepOffline = out.trip?.keepOffline, tombstoned, stored = out.plan != null (6b's existing `stored`), noPlan = out.noPlan, syncCompleted.value, awaitingSync.value, fetchModel.online.value, fetchModel.fetch.value).state()` and write it into 6b's `ui` (`TripPageUi.state`). Call `recompute()` from the pipeline's `apply` (as 6b does) **and** from one collector of `combine(fetchModel.online, fetchModel.fetch, awaitingSync, syncCompleted)`. Do not add a second state flow: "page state" in this brief means `ui.state`. 6b's ready content still renders only when `ui.state == Ready && ui.plan != null`.
  - `online: StateFlow<Boolean>` exposed for 6d (trip actions need it).
  - `retry()` → `fetchModel.retry()`.
  - **View mode:** `timeline: StateFlow<Boolean>` (default `true`) and `fun toggleView()`. Debug builds only: when the seed route ends in `:list` (`CF_PLAN_TEST_ROUTE=trip:<id>:list`, read where 6b reads its seeds), the initial value is `false` (iOS l.86–90). Release builds ignore the seed.

**Edge cases**
- A tombstone arriving while ready → `NotFound` (resolve order).
- A record disappearing after a finished sync → `NotFound`; while `awaitingSync` → stays `Loading`/remote.
- A successful fetch writes the detail; the page moves to `Ready` (or `NoPlan`) through the source, not through `fetch`. `fetch == Loaded` with no detail yet resolves to `RemoteLoading` (resolve's last branch), which is correct.
- Sign-out or epoch change: 6b's session gate tears the page down; `stop()` cancels the fetch.

**Tests** (JVM `@Logic`, `feature/trip/src/test/…/TripPageViewModelStateTest.kt`, fake source as a `MutableStateFlow`, fake `SessionSync` events as a `MutableSharedFlow(replay = 1)`, `FakePath`):
- `remoteTripFetchesOnceAndBecomesReady` (all via `vm.ui.value.state`): record `keepOffline=false`, no detail → `RemoteLoading`, 1 request; the fake then emits a detail → `Ready`.
- `offlinePathShowsRemoteOfflineWithoutRequest`: path emits `false` first → `RemoteOffline`, 0 requests; path `true` → 1 request.
- `notFoundAndServerFromFetch`: 404 → `NotFound`; 500 → `Server`; `retry()` from `Server` → `RemoteLoading` while in flight.
- `finishedSyncOfOtherEpochIsIgnored`: no record, `hasLoaded`, `Finished(epoch = other, Idle)` → `Loading`; `Finished(epoch = current, Offline)` → `Loading`; `Finished(current, Idle)` → `NotFound`.
- `replayedFinishedEventCounts`: the event is emitted before the ViewModel starts → `NotFound` once loaded with no record.
- `awaitingSyncClearsWhenRecordAppears`.
- `viewModeDefaultsToTimelineAndToggles`: `timeline == true`; `toggleView()` → `false`; a new ViewModel → `true` (not persisted, nothing in `SavedStateHandle`: assert `savedStateHandle.keys()` has no view-mode key).
- `listSeedStartsInListMode` (debug options fake with `trip:<id>:list`) → `false`.

### Task 4: `ListRows` and the now instant

**Commit:** `feat(plan): build list rows off the main thread`. **Ticks:** none.

**Files**
- `core/plan/…/trippage/ListRows.kt` (JVM):
  ```kotlin
  @Immutable  // or a plain data class if :core:plan has no Compose runtime; then mark it @Stable-equivalent via the compose-stability config file 1d uses
  data class ListRow(
      val key: String,              // "r:<dayId>:<blockId>" or "z:<dayId>:<blockId>"
      val dayId: String,
      val blockId: String,
      val zone: Boolean,            // block.type == zone_change
      val type: BlockType,
      val title: String,
      val interval: String,         // PreparedBlock.interval
      val intervalNow: String,      // copy("%@ · now", interval)
      val label: String,            // a11y, not running
      val labelNow: String,         // a11y, running
      val priorityTag: String?,     // PreparedBlock.priority only for core/optional, else null
      val status: CheckStatus?,
      val optional: Boolean,        // priority == optional (dashed border)
      val startMs: Long,
      val endMs: Long,
      val zoneInstruction: String?,
      val zoneComparison: String?,
  )
  data class DayListRows(val dayId: String, val rows: List<ListRow>)
  object ListRows { fun build(plan: PreparedPlan, copy: PageCopy): List<DayListRows> }
  ```
- `core/plan/…/trippage/DayClockProjection.kt` (1d): add `val nowMs: Long` to `NowLineValue` and set it in `projectDayClocks` (`now.toEpochMilli()`). The value already changes on every tick (its offset moves), so this adds no writes; update 1d's tests that compare whole values.

**Rules** (iOS `DayListView.swift` l.47–60 [F])
- One `DayListRows` per `PreparedDay`, rows in `day.blocks` order (all blocks: placed, markers and zone changes).
- `zone = block.block.type == BlockType.ZoneChange`.
- Ordinary label = `[title, interval or intervalNow, priorityTag, statusText].filterNotNull().joinToString(", ")`, `statusText` = `copy("Marked done")` for `Done`, `copy("Skipped")` for `Skipped`.
- Zone label = `[title, zoneInstruction, zoneComparison].filterNotNull().joinToString(". ")`; `labelNow = label`; `intervalNow = interval`.
- `startMs`/`endMs` = 1a's plan-date parse of `block.startUtc`/`block.endUtc`. An unparsable value makes the row never running (`startMs = Long.MAX_VALUE`, `endMs = Long.MIN_VALUE`).
- Running (applied in the row, Task 6) = the day's `DayClock` value is non-null and `startMs <= nowMs < endMs`. A day without now has no clock value, so none of its rows run (iOS passes `distantPast`).
- The ViewModel builds `ListRows.build(prepared, copy)` in the same off-main step and under the same revision guard as `PlanPreparer`, and publishes it with the prepared plan. Rows of unchanged days compare equal, so Compose skips them.

**Tests** (JVM, `core/plan/src/test/…/trippage/ListRowsTests.kt`)
- `large60Counts`: 60 `DayListRows`, 740 rows, 20 with `zone`, keys unique across the plan.
- `labelsMatchIos` (S3, en): one core row → `"<title>, <interval>, Important"`; with a `Done` mark → ends with `", Marked done"`; recommended row has no priority part; a zone row joins with `". "`.
- `nowVariants` (en and nl): `intervalNow == "<interval> · now"` / `"<interval> · nu"`; `labelNow` contains it in second position.
- `crossMidnightBlockHasTwoKeys`: a block with `continuesAfter` appears in two days with keys that differ only by day id.
- `unparsableTimesNeverRun`.

### Task 5: List row components and icons

**Commit:** `feat(trip): add list rows and zone change cards`. **Ticks:** none.

**Files** (`feature/trip/…/list/`, package `net.chronoflow.feature.trip.list`)
- `TripPageCategorySwatch.kt`: `@Composable fun TripPageCategorySwatch(type: BlockType, modifier: Modifier = Modifier)` (port of `PlanAppearance.swift` l.93–150). 28 × 28 dp (`prioritySampleHeight`), clipped to radius `tileRadius` 14 (a circle). Fill = `TimelineCategoryStyle(type).fill`; pattern dots (radius 1, step 7, foreground α `dotOpacity` 0.16) or hatch (45° lines, step `patternStep × √2`, width 2, foreground α `hatchOpacity` 0.12), reusing 1c's pattern code when it exists; glyph = the style's lucide glyph at `blockIcon` 14 dp in the style foreground; `no_caffeine` adds a 2 dp foreground line from (6, 22) to (22, 6); border: `no_caffeine` → `catCaffeine` 2 dp, `light_ok` → `catModerateFg` α 0.4 at 1 dp, `note` → `border` 1 dp; the border is drawn only for those three types, dashed `[smallGap 4, smallGap 4]` when the style is dashed (iOS l.124–130; no border, so no dash, for other types). Draw everything in one `drawBehind`/`drawWithCache` (no child layouts except the glyph `Icon`). Hidden from accessibility.
- `TripPagePriorityTag.kt` (skip if 6b has it): `planPriority` text, padding horizontal `tilePadding` 6 / vertical `border` 1, capsule fill `background` α 0.4, capsule stroke `foreground` α 0.4 at 1 dp.
- `DayListRow.kt`: `@Composable fun DayListRow(row: ListRow, clock: DayClock?, onBlock: (String) -> Unit, modifier: Modifier = Modifier)` (port of `DayListView.row` and `trailing`, l.61–96):
  - `val running by remember(row, clock) { derivedStateOf { clock?.value?.value?.let { row.startMs <= it.nowMs && it.nowMs < row.endMs } == true } }`.
  - `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(sectionGap 12))`: swatch; `Column(Modifier.weight(1f), spacedBy(smallGap 4))` [title `authStrong`, `cardForeground`; interval `planListLine.tnum()`, `mutedForeground`, text = `if (running) intervalNow else interval`]; trailing.
  - Trailing (iOS `ViewThatFits`): a custom `Layout` that places the tag and the status icon in a row with gap `rowGap` 8 when that row fits the remaining width, else stacks them end-aligned with gap 8. Tag only when `priorityTag != null`; status icon only when `status != null`: lucide `check` (Done) or `skip-forward` (Skipped), 18 dp, stroke 2.5, `dawnText`, hidden from accessibility.
  - Padding horizontal `blockIcon` 14, vertical `sectionGap` 12; background `card` with `RoundedCornerShape(rowRadius 22)`; border 1 dp `border`, dashed `[smallGap 4, smallGap 4]` when `optional`, drawn in `drawBehind` (6e adds a `selected: () -> Boolean = { false }` parameter read only there; leave the border in one draw modifier so that is a local change); `Modifier.alpha(markedRowOpacity 0.7)` when `status != null`.
  - Semantics: `clearAndSetSemantics { contentDescription = if (running) labelNow else label; role = Role.Button; onClick { onBlock(blockId); true } }`, `testTag("trip.block.$blockId")`, click through `cfClickable { onBlock(row.blockId) }`.
  - `SideEffect { LocalTimelineProbe.current?.<record>(TimelineProbe.Kind.LIST_ROW, row.key) }` using the probe call 1d's kinds use. Add `LIST_ROW` to `TimelineProbe.Kind`.
- `ZoneChangeCard.kt`: `@Composable fun ZoneChangeCard(row: ListRow, onBlock: (String) -> Unit, modifier: Modifier = Modifier)` (port of l.100–131):
  - `Row(verticalAlignment = Top, spacedBy(12))`: a 44 dp (`touch`) circle filled `dawn` with lucide `map-pin` 20 dp in `dawnForeground` (hidden); `Column(spacedBy(rowGap 8))` [title `planCardTitle`; instruction plate `authHint`; comparison plate `planSubtitle.tnum()`], each plate only when its text is non-null.
  - Plate: text `skyCopy` on `skyCopyBackground`, padding horizontal 8 / vertical 4, radius `textPlateRadius` 10.
  - Container: padding `zonePadding` 20, full width, content color `skyTitle`, `Modifier.clip(RoundedCornerShape(cardRadius 26)).skyBackground()`.
  - Semantics: `clearAndSetSemantics { contentDescription = row.label; role = Role.Button; onClick { … } }`, `testTag("trip.zone.${row.blockId}")` [R: iOS has no identifier on this card]; `cfClickable`. Probe `LIST_ROW` as above.
- Icons: add to `tools/icons/lucide.json` and run `make generate` in this commit:
  - `calendar`: `{"source": "board", "board": "Plan", "first": "<rect width=\"18\" height=\"18\" x=\"3\" y=\"4\" rx=\"2\">"}`;
  - `cloud-upload`: `{"source": "board", "board": "PlanRemote", "first": "M12 13v8"}` (children `M12 13v8`, `M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242`, `m8 17 4-4 4 4`).
  Update `LucideGlyphsTest`'s glyph count (+2). `check`, `skip-forward` and `map-pin` are already bundle glyphs.

**Tests** (Robolectric `@Logic`, `ProfileHost` at `pixel8`, en unless stated; < 5 s each)
- `DayListRowTest`:
  - `labelAndTagFollowPriority`: core row → node `trip.block.<id>` has the label, a click action, and `Role.Button`; one click calls `onBlock(id)` once.
  - `runningRowSaysNow`: a `DayClock` with `nowMs` inside the block → content description `labelNow` and the interval text `"… · now"`; move `nowMs` past `endMs` → back to `label`.
  - `markedRowIsDimmedAndShowsIcon`: `status = Done` → label ends with "Marked done"; the row's alpha is 0.7 (read from a test-only semantics property or by pixel ink at a known point; prefer the semantics property `TripRowAlpha` set in the alpha modifier).
  - `trailingStacksWhenNarrow`: in a 280 dp wide host with nl copy and a long title, the tag's top is below the status icon's top (stacked); at 411 dp they share a centre line.
  - `touchTargetAtLeast48`: the row height ≥ 48 dp (it is ≈ 66).
- `ZoneChangeCardTest`: label joins with `". "`; no plate node when `zoneComparison == null`; `trip.zone.<id>` click calls `onBlock`.

### Task 6: List mode in the page

**Commit:** `feat(trip): add list mode and the view toggle`. **Ticks:** plan.md 6c "**List mode**".

**Files:** 6b's page composable and its item emission, `feature/trip/…/list/ListModeIndex.kt`, 6b's `TripListIndex.kt` and `TripScrollMath.kt` (interface + list content types only), 6b's `TripPlanPipeline` (build `ListRows` and `ListModeIndex` off main next to 6b's `index`, carried in `ReadyPlan`), 6b's compact header, `TimelineJankTagger` call site, top bar actions.

**Behaviour**
- **Spacing.** 6b's page `LazyColumn` already uses `verticalArrangement = spacedBy(0.dp)` with the 12 dp `sectionGap` as each item's top padding (6b Task 11 step 7 table). Confirm it and change nothing; if 6b deviated, move the gap into item top padding so list rows can sit 8 dp apart. 6b's timeline-mode layout must not move by a pixel: re-run 6b's captures and guard tests unchanged in this commit.
- **Items in list mode** (iOS `TripDayRow` phone branch: `VStack(spacing: 0) { DayHeader; DayListView }`):
  - 6b's items before the days (`summary`, `days`) and each day's header item (`h:<dayId>`, `dayHeader`) stay as they are.
  - Each day's timeline item (`t:<dayId>`, `contentType "dayTimeline"`, including its `LaneHeadings`) is replaced by that day's rows: `item(key = row.key, contentType = if (row.zone) "zoneRow" else "listRow") { … }` with top padding 0 for the first row of the day and `rowGap` 8 for the others, horizontal padding = 6b's gutter.
  - A day with zero rows emits only its header.
  - The `end` and `hidden` items stay.
- **`ListModeIndex`** (JVM-testable, `feature/trip/…/list/ListModeIndex.kt`; a new name, because 6b's timeline-mode `TripListIndex` already exists in `…/page/`): built from the day ids and the `DayListRows`. Same members as 6b's `TripListIndex` that the page uses (`summary` 0, `days` 1, `headerIndex(dayId)`, `end`, `hidden`, `dayAt(index)`, `itemCount`) plus `rowIndex(key)`; `dayAt` returns the day of a header or row index, else null. Extract a small interface `PageIndex` from 6b's `TripListIndex` with exactly the members both share, implement it in both, and let the page pick the instance for the current mode. Nothing searches: lookups are `HashMap`/`IntArray`.
- **Current day:** extend 6b's `TripScrollMath.currentDay(visible, index, thresholdPx)` to accept `PageIndex` and to count items whose `contentType` is `"dayHeader"`, `"dayTimeline"`, `"listRow"` or `"zoneRow"` (iOS: the last day whose row top ≤ the compact header height; a header counts as part of its day). `restoreTarget` and `offsetInDayPx` stay timeline-only.
- **Anchor tracking** in list mode calls 6b's `select(dayId, offset = 0.0)` (the in-day offset is only restored in timeline mode).
- **Toggle** (iOS l.233–245):
  - In the top bar, before the trip-actions button: `CfIconButton(icon = if (timeline) MaterialSymbols.ViewAgenda else MaterialSymbols.ViewTimeline, contentDescription = if (timeline) "Show as list" else "Show as timeline", …)`, `testTag("trip.viewToggle")`.
  - Present only when `ui.state` is `Ready`, `Loading` or `RemoteLoading` (6e makes it absent on wide). Enabled only when `ui.state == Ready` and `ui.plan != null`.
  - On click, in one handler: cancel 6b's pending restoration if it has one; `vm.toggleView()`; then `listState.requestScrollToItem(newIndex.headerIndex(anchorDayId), 0)` where `newIndex` is the index of the new mode. `requestScrollToItem` overrides key-based position retention for the next measure, so the day's header lands at the top in the new mode.
- **Trip actions button:** if 6b did not add it: `CfIconButton(MaterialSymbols.MoreVert, "Trip actions", onClick = onActions ?: {}, enabled = onActions != null)`, `testTag("trip.actions.open")` (OQ-A11).
- **Compact header, list variant** (iOS `compactDayTitle`, l.384–392): when not wide and `!timeline`, 6b's compact overlay shows a `Row(verticalAlignment = CenterVertically)`: one `Text` with an `AnnotatedString` (`day.title` in `planListTitle`, then `" · " + day.date` in `planListLine` `mutedForeground`; strings come from `PreparedDay`, built off main), `Spacer(Modifier.weight(1f).widthIn(min = smallGap 4.dp))`, `day.zone` in `planSubtitle`; horizontal padding = gutter, min height `compactHeaderHeight` 36, background `card`. In timeline mode it keeps 6b's `LaneHeadings`. Same tag `trip.compactHeader`, same fade rule as 6b.
- **Replacement in list mode** (a new prepared revision while in list mode): keep the `LazyListState`. If the day ids are unchanged, key-based retention keeps the position (row keys are stable). If they changed, use 6b's `replacementAnchor` and `requestScrollToItem(index.headerIndex(anchor.dayId), 0)`. Recorded as OQ-6c-7.
- **Jank tagger:** call `TimelineJankTagger.onVisibleDay(index, mode)` with `mode = "list"` in list mode.
- **Clock:** each row gets `clocks[dayId]` (6b's `DayClocks`) and reads it only in its `derivedStateOf`.

**Edge cases**
- Toggle while the anchor day is null (empty plan): flip only, no scroll.
- Toggle during a fling: `requestScrollToItem` stops the fling (it is a scroll request); acceptable.
- Font scale 1.3 (`largeFont`): rows grow; nothing truncates (`Text` without `maxLines`).
- A check-off on day k (6d later): only day k's changed row recomposes, because unchanged `ListRow`s are equal.

**Tests**
- JVM `ListModeIndexTest`: S3 in list mode: `headerIndex` of each day, `dayAt` for rows, `null` for `summary`, `days`, `end`, `hidden`; large60 without hidden steps: `itemCount` = 2 + 60 headers + 740 rows + 1 end = 803. Extend 6b's `TripScrollMathTest` with list content types: header of day 2 at offset 0 → day 2; a `listRow` of day 1 at offset −10 and day 2's header at +200 with threshold 36 → day 1. 6b's existing `TripListIndexTest`/`TripScrollMathTest` cases stay unchanged and green.
- Robolectric `@Logic` `ViewToggleTest` (S3, `FixedPlanClock`, fixture source):
  - `toggleShowsListRowsAndKeepsDay`: open at day 2 (6b's opening anchor) → click `trip.viewToggle` → day 2's first row `trip.block.<id>` exists and day 2's `trip.day.<dayId>` (timeline item) does not (do not assert on `trip.timeline.ready`: 6b keeps that page-level marker in both modes); the first visible item is day 2's header; the button's content description is "Show as timeline"; click again → timeline, day 2's header at the top.
  - `toggleAbsentOrDisabledByState`: `NotFound`/`Server`/`NoPlan`/`RemoteOffline` → no `trip.viewToggle` node; `Loading`/`RemoteLoading` → node present and not enabled; `Ready` → enabled.
  - `compactHeaderListVariant`: in list mode scrolled into day 2, `trip.compactHeader` contains day 2's title and zone text and no lane heading text.
- Robolectric `@Logic` `ListInvalidationTest` (port of the research §10.2 table for List mode, `CountingTimelineProbe`, large60 fixture, list mode):
  - `scrollRecomposesNoComposedRow`: 120 scroll offsets of 37 px each through days 1–3 → for every key composed before a step and still composed after it, 0 extra `LIST_ROW` records.
  - `minuteTickWithinARowRecomposesNothing`: advance the fixed clock by 1 minute inside a running block → 0 `LIST_ROW` records.
  - `tickAcrossABoundaryRecomposesTheTwoRows`: advance to the next block's start → exactly the ending row and the starting row record once each (one record if the blocks overlap and only one flips).
  - `checkOffRecomposesOnlyThatRow`: feed a prepared plan with one more `Done` mark on day k → exactly that row records; no row of another day does.

### Task 7: State pages, loading bar and pull to refresh

**Commit:** `feat(trip): add the trip page loading, remote, no-plan and error states`. **Ticks:** plan.md 6c "**States**".

**Files** (`feature/trip/…/state/`): `TripPageLoadingBar.kt`, `TripPageSkeleton.kt`, `RemoteSummaryCard.kt`, `TripPageStateCard.kt`, `TripPageErrorPage.kt`; 6b's page composable (state switch).

**Page switch** (iOS `TripPageView` l.60–72, l.118–158)
This replaces 6b's non-ready seam (6b Task 11 step 5: a `Box` tagged `trip.page` holding only a `trip.timeline.preparing` node). 6b's Ready branch, its `LazyListState` (created inside the Ready branch) and its items are untouched.
- `NotFound` or `Server` → `TripPageErrorPage` (whole screen, no 6b top bar: iOS attaches `.toolbar` to `page` only, `TripPageView.swift` l.110, l.224).
- Every other non-ready state → inside 6b's `Scaffold` (6b's `TripTopBar`), the same `CfPullToRefreshBox(isRefreshing, onRefresh)` wiring as 6b's Ready branch (so pull to refresh works on every state, iOS `.refreshable` on both branches) → `Box(Modifier.fillMaxSize().padding(inner).testTag("trip.page"))` → its own `LazyColumn(state = rememberLazyListState(), Modifier.fillMaxSize().testTag("trip.scroll"), contentPadding as 6b's Ready list)`. Drop the `trip.timeline.preparing` seam node (no 1d/1e test reads the page-level one; 1d/1e's per-day `trip.timeline.preparing` tag inside `DayTimeline` stays). Content items for states other than `Ready`:
  1. `meta` (only when `metadata` is non-empty): `Text(metadata)` in `planSubtitle`, `mutedForeground`, centred, full width, `testTag("trip.meta")`.
  2. `NoPlan` → `TripPageStateCard` no-plan variant (below).
  3. `RemoteLoading` or `RemoteOffline` → `RemoteSummaryCard`.
  4. `RemoteOffline` → `TripPageStateCard` offline variant; otherwise (`Loading`, `RemoteLoading`) → `TripPageSkeleton(downloading = state == RemoteLoading)`.
  Items are separated by `sectionGap` 12 (as 6b spaces its items), horizontal padding = gutter 16, bottom padding 16.
- **Top bar for non-ready states** (iOS l.226–262): 6b's `TripTopBar` already does this (title `%@ → %@` from `TripTitleParts` = detail, else card, else "Loading your plan"; no subtitle; tag `trip.title`; `trip.dayMenu` only when ready). Verify it with the remote seeds below (card, no detail). Actions: the view toggle per Task 6, then 6b's trip-actions button.

**`TripPageLoadingBar`** (iOS `TripPageLoadingBar.swift`): the destination shows it while the ViewModel or session is not available yet (6b decides when; if 6b always has a ViewModel on the first frame, use it for the session-resolving case only): a `Scaffold` with `CfTopAppBar(title = "Loading your plan" in planTitle single line, testTag "trip.loadingTitle", navigationIcon = 6b's back button, actions = { disabled trip-actions button })` and an empty `background` body. No skeleton.

**`TripPageSkeleton`** (iOS `skeleton`, l.458–468): `Column(spacedBy(sectionGap 12))`: when `downloading`, `Text("Downloading this trip’s plan…")` in `authHint`, `foreground` [iOS default foreground; OQ-6c-10]; `CfSkeleton(Modifier.fillMaxWidth().height(skeletonSummary 120.dp), RoundedCornerShape(rowRadius 22))`; `CfSkeleton(Modifier.fillMaxWidth().height(touch 44.dp), CircleShape)`; `CfSkeleton(Modifier.fillMaxWidth().height(skeletonTimeline 380.dp), RoundedCornerShape(22))`. Root `clearAndSetSemantics { contentDescription = "Loading your plan" }`, `testTag("trip.skeleton")`. Static (no shimmer).

**`RemoteSummaryCard`** (iOS `remoteSummary`, l.438–456): 6b's `PlanCard` containing `Column(spacedBy(12))`: "Your plan" in exactly 6b's summary eyebrow treatment (`eyebrow`, `dawnText`); when `adaptation != null`, `clockShiftText(adaptation, formatter, options)` in `welcomeWordmark` (built off main by the source); when the card exists, 6b's `DualClocks(clocks)` fed by a `SummaryClockState` whose values come from 6b's `DualClockValue` computation with the card's from/to, `activeZone = null`, `remote = true` (labels "Home · %s" / "There · %s" with city, else IATA; 6b already supports `remote`, it only always passed `false`). Compute the values in the ViewModel on each clock tick, as 6b does for the ready summary; nothing is formatted in composition. Semantics: `contentDescription = "Plan summary"` on a container that keeps its children (`semantics { contentDescription = …; isTraversalGroup = true }`), `testTag("trip.remote.summary")` [R].

**`TripPageStateCard`** (iOS `stateCard`, l.494–516): `@Composable fun TripPageStateCard(icon: LucideGlyph, iconTint: Color, title: String, titleStyle: TextStyle, text: String, modifier: Modifier = Modifier, actions: @Composable ColumnScope.() -> Unit)`. Leave room for 6e's `wide: Boolean = false`.
- `Column(horizontalAlignment = CenterHorizontally, spacedBy(sectionGap 12))`, full width, padding horizontal `cardPadding` 16 / vertical `cardPadding` 16, text centred.
- Icon: a 44 dp (`touch`) circle filled `muted` holding the lucide glyph at 24 dp in `iconTint`, hidden from accessibility.
- Title: `titleStyle`, `foreground`, `semantics { heading() }`.
- Body: `authBody`, `mutedForeground`, full width.
- Actions.
- Border: 1 dp `border` stroke, dash `[smallGap 4, smallGap 4]`, radius `cardRadius` 26, drawn in `drawBehind`; no fill.

**No plan** (iOS `noPlan`, l.470–479): `TripPageStateCard(LucideGlyphs.Calendar, dawnText, "No plan yet", planCardTitle, "Your flights are saved. Check your answers to make the plan. Once it’s made, it’s stored on this phone and works offline.")` with actions `CfButton("Check answers and make plan", onClick = {}, style = Primary, size = Lg, enabled = false, fillWidth = true, minHeight = 48.dp)` tag `trip.noPlan.makePlan`, then `Text("Making the plan needs a connection.")` in `planListLine` (`foreground`). Card tag `trip.noPlan`, title tag `trip.noPlan.title`.

**Remote offline** (iOS `remoteOffline`, l.481–492): `TripPageStateCard(LucideGlyphs.CloudUpload, mutedForeground, "This plan downloads when you’re online", planRemoteTitle, "Plans for active trips and trips in the next 7 days are always on your phone. This one is further away, so its plan opens once you’re connected.")` with actions `CfButton("Try again", onClick = vm::retry, style = Outline, size = Touch)` tag `trip.remote.retry`, then `Text("Your other trips keep working offline.")` in `planListLine`. Card tag `trip.remote.offline` (iOS identifier), title tag `trip.remote.offline.title`.

**`TripPageErrorPage`** (iOS `errorPage`, l.518–537; board `ErrorNotFound` for the buttons):
- `missing = state == NotFound`.
- Root: `Box(Modifier.fillMaxSize().skyBackground())`, content color `skyTitle`, `testTag(if (missing) "trip.error.notFound" else "trip.error.server")`.
- A transparent `CfTopAppBar` with only 6b's back button (iOS keeps the system back and no toolbar items) [R].
- Below it, `CfPullToRefreshBox(isRefreshing, onRefresh = vm::refresh)` around a `Column(Modifier.fillMaxSize().verticalScroll(…).testTag("trip.scroll"))` whose content is at least the viewport tall (`heightIn(min = viewport)` via `BoxWithConstraints` or `Modifier.fillMaxHeight()` inside a `verticalScroll` with `IntrinsicSize`; measured sizes stay in layout):
  - `Spacer(weight 1)`;
  - text block, `Column(horizontalAlignment = CenterHorizontally, spacedBy(sectionGap 12))`, horizontal padding `gutter` 16, centred text: eyebrow "Not found" / "Something went wrong" in `eyebrow`; title "We can’t find that" / "We’re having trouble" in `authHeading`, `heading()`, tag `trip.error.title`; body "The link may be old, or the trip was deleted." / "It’s on our side, not yours, and your plans are safe. Try again in a few minutes." in `authBody`. All `skyTitle`.
  - `Spacer(weight 1)`;
  - buttons, `Column(spacedBy(rowGap 8))`, padding start/end 24, bottom 48 plus the navigation-bar inset: primary `CfButton("Go to my trips" / "Try again", style = Dawn, size = Lg, fillWidth = true)` tag `trip.error.primary`, action `onTrips` (6a shell: Trips tab root) / `vm.retry()`; secondary `CfButton("Open Today" / "Open my plan for today", style = OnSky, size = Touch, fillWidth = true, enabled = onToday != null)` tag `trip.error.secondary`, action `onToday` (6a shell: Today tab).
- `vm.retry()` from `Server` resolves to `RemoteLoading` while the request runs, so the page leaves the error screen immediately (iOS behaviour).

**Edge cases**
- `Loading` with no card: title "Loading your plan", no metadata.
- `RemoteLoading` with a record but no adaptation: the summary card shows the eyebrow and clocks only.
- A trip that becomes `keepOffline = true` while remote: resolves to `Loading` (skeleton) until sync writes the detail.
- `NoPlan` while offline: same card (the button is disabled anyway).
- Rotation or fold while on any state: the ViewModel keeps `fetch` and `online`; no new request (in-place config change).

**Tests** (Robolectric `@Logic`, `feature/trip/src/test/…/state/TripPageStatesTest.kt`, fake source + fake fetch, `FixedPlanClock`, `pixel8`, en; one test per case):
- `loadingShowsSkeletonWithoutDownloadingLine`: record `keepOffline=true`, no detail → `trip.skeleton` with content description "Loading your plan"; no "Downloading…" text; title `trip.title` "AMS → HND".
- `remoteLoadingShowsSummaryAndDownloadingLine`: trip `0199a3c0-0000-7000-8000-000000000002` card + adaptation, `keepOffline=false`, fetch suspended → `trip.remote.summary`, "Downloading this trip’s plan…", `trip.skeleton`, the toggle present and disabled.
- `remoteOfflineRetryFetchesAgain`: path offline → `trip.remote.offline`; set online is not needed: click `trip.remote.retry` while offline → still 0 requests (iOS `retry` offline branch); path online → 1 request; click retry after an `Offline` failure → 2 requests.
- `noPlanCardButtonDisabled`: S3 copy with status `no_plan` stored → `trip.noPlan` exists, `trip.noPlan.makePlan` is not enabled, `trip.noPlan.title` is a heading.
- `notFoundButtonsNavigate`: 404 → `trip.error.notFound`; `trip.error.primary` click → `onTrips` called once; `trip.error.secondary` → `onToday` once; no `trip.viewToggle`, no `trip.actions.open`.
- `serverTryAgainRetries`: 500 → `trip.error.server`; click `trip.error.primary` → `RemoteLoading` (skeleton visible), request count 2.
- `tombstonedTripIsNotFound`: stored detail plus `deletedLocally = true` → `trip.error.notFound`.
- `missingAfterSyncIsNotFound`: no record, loaded, `Finished(current epoch, Idle)` → `trip.error.notFound` (the "trip not on this phone" case).
- `metadataLineOnlyWhenNotReady`: `RemoteOffline` with a card → `trip.meta` text equals `tripMetaText(...)`; `Ready` → no `trip.meta`.
- `pullToRefreshOnStates`: on `RemoteOffline` and on `NotFound`, a swipe down on `trip.scroll` calls the fake `refresh` once.
- `loadingBarHasDisabledActions`: `TripPageLoadingBar` → `trip.loadingTitle` text "Loading your plan"; the trip-actions node is not enabled.
- nl spot check `remoteOfflineDutchCopy`: title "Dit plan wordt gedownload zodra je online bent", button "Opnieuw proberen".

### Task 8: Guards, board captures and the seeded emulator check

**Commit:** `test(trip): guard and capture list mode and trip states`. **Ticks:** plan.md 6c "**Tests**".

**Seeds per state** (fake `TripPageSource` and fetch, `FixedPlanClock`; Robolectric only):

| Capture / guard name | Board | Seed |
|---|---|---|
| `PlanList` | `PlanList` | 6b's capture fixture (ready AMS → HND), list mode, page at offset 0 (summary visible, then day 1). If `BoardCapture` takes a custom size, capture 390 × 2600 dp as the board; else `pixel8` |
| `PlanListScrolled` | `PlanListScrolled` | same, list mode, scrolled so day 1's header is under the compact header (compact header visible) |
| `PlanLoading` | `PlanLoading` | trip `…0001` record `keepOffline=true`, no detail |
| `PlanNoPlan` | `PlanNoPlan` | S3 copy with plan status `no_plan`, stored |
| `PlanRemote` | `PlanRemote` (`state=loading`) | trip `…0002` card + adaptation, `keepOffline=false`, fetch suspended |
| `PlanRemoteLoadingNL` | `PlanRemoteLoadingNL` | same, nl |
| `PlanRemoteOffline` / `…NL` | `PlanRemoteOffline` / `…NL` | same, path offline |
| `ErrorNotFound` | `ErrorNotFound` | trip `…0002`, fetch → `Server(404)` |
| `ErrorServer` | `ErrorServer` | trip `…0002`, fetch → `Server(500)` |

**Files**
- `feature/trip/src/test/…/TripStatesGuardsTest.kt` (`@Layout`): one parameterized test per state seed above (list boards included) × profile (`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`) × language (en, nl) × theme (light, dark). Each case runs `Guards.assertAll(rule, profile, headingTags, exemptTags)` with `headingTags`: no plan → `trip.noPlan.title`; offline → `trip.remote.offline.title`; errors → `trip.error.title`; list and loading → 6b's day header heading tags. `exemptTags`: `trip.skeleton` (decorative shapes, no ink check). `foldInner` is ≥ 600 dp: it renders 6c's phone composables until 6e lands; keep the case (it must pass now and will be re-baselined by 6e).
- `feature/trip/src/test/…/TripStatesCaptureTest.kt` (`@Capture`): every row of the table in light and dark at `pixel8` (`ErrorNotFound`/`ErrorServer` also at a 375 × 812 host if `BoardCapture` supports custom sizes), names `"<Board>_pixel8_<lang>_<theme>"` (`BoardCapture` naming).
- `app/src/androidTest/…/TripListSmokeTest.kt` (`@Smoke`, the one emulator journey of this feature, < 10 s): launch seeded with the mock data source and `CF_PLAN_TEST_ROUTE=trip:0199a3c0-0000-7000-8000-000000000001:list` (the same way 6b's smoke test seeds its route) → `waitUntilExactlyOneExists(hasTestTag("trip.viewToggle") and hasContentDescription("Show as timeline"))`; then wait for the first block row of the opening day by its exact tag (`trip.block.<id>`, id looked up in the mock fixture and hard-coded as a constant; no tag-prefix scan, AGENTS.md Tests 4); click `trip.viewToggle` → wait for that day's timeline item `trip.day.<dayId>` (not `trip.timeline.ready`, which 6b keeps in both modes).

**Run:** `make captures ONLY=PlanList,PlanListScrolled,PlanLoading,PlanNoPlan,PlanRemote,PlanRemoteLoadingNL,PlanRemoteOffline,PlanRemoteOfflineNL,ErrorNotFound,ErrorServer`. Attach the output folder path to the commit body for the reviewer. Every difference with a board is either in §5.1 or fixed.

### Task 9: `listScroll` benchmark, budgets and baseline profile

**Commit:** `perf(trip): benchmark list mode scrolling`. **Ticks:** plan.md 6c "**List benchmark**".

**Files:** `benchmark/src/main/…/TimelineBenchmark.kt`, `benchmark/budgets.json`, `Scripts/check-benchmarks.py`, the baseline-profile generator 1e wrote, `docs/perf-timeline.md`.

**`listScroll`** (copy `timelineScroll` and change only what is listed):
- Setup (not measured): `startActivityAndWait` with extra `cf.trip-page=large60`; `device.wait(Until.hasObject(By.res("trip.timeline.ready")), 10_000)`; click `By.res("trip.viewToggle")`; `device.wait(Until.hasObject(By.res("trip.viewToggle").desc("Show as timeline")), 5_000)` (the page-level `trip.timeline.ready` marker stays in list mode, so never wait for it to go); wait for the first block row of the opening day of large60 (`By.res("trip.block.<id>")`, id from `LargeTimelineFixtures.large60` and the perf clock's opening day, hard-coded as the constant `LARGE60_OPENING_FIRST_BLOCK`). Record the top-most visible `trip.block.*` resource id via `device.findObjects(By.res(Pattern.compile("trip\\.block\\..*")))` sorted by `visibleBounds.top` (one call, not a loop of queries).
- Measured: exactly 15 flings on `By.res("trip.page")` with gesture margin `width / 5`, as `timelineScroll`.
- After measuring: the top-most visible `trip.block.*` id differs from the one recorded in setup.
- Metrics: `FrameTimingMetric()` + `MemoryUsageMetric(MemoryUsageMetric.Mode.Max)`; 3 iterations, each a fresh launch; `CompilationMode.Partial(BaselineProfileMode.Require)`; `assertAnimatorsOn()` first. Drop any placeholder/build trace metrics `timelineScroll` has (list rows have no drawings).
- `budgets.json`: add `listScroll` rows equal to `timelineScroll`'s (emulator: functional + peak RSS ≤ 150 MiB gate; hitch ratio, P99 and jank report only; device rows as in plan.md "Performance budget"). `check-benchmarks.py`: add `listScroll` to the timeline suite's required tests and update its tests (`Scripts/test_scripts.sh`).
- Baseline profile: extend the generator's timeline journey with: toggle to list, 3 flings, toggle back. Run `make baseline-profile` and commit the regenerated profile.
- Run `make perf-timeline` on the `pixel` emulator. Record in `docs/perf-timeline.md` a new row per benchmark (device, build type, command, date): `listScroll` frame metrics and peak RSS, and the three timeline rows, which must pass every emulator gate of plan.md "Where gates apply" (the CPU band against the last three `trip` rows included); list each ± 25 % `FLAGGED` line with a one-line cause, which does not fail.

**If the sandbox cannot run the emulator:** commit the code and the budgets, and report the exact commands (`make baseline-profile`, `make perf-timeline`) with their output to the orchestrator; the orchestrator runs them and the numbers go in a follow-up `docs(perf): record list scroll results` commit on this branch.

---

## 7. Acceptance criteria

1. `make quick` passes after every commit; `make check` passes on the branch with the `pixel` emulator booted (orchestrator runs it if the sandbox cannot).
2. `make targeted ONLY=":core:plan:TripPageInputsTests :core:plan:ListRowsTests :core:plan:TripPageTests :core:plan:TripPageStateTests :feature:trip:TripPageFetchTests :feature:trip:TripPageViewModelStateTest :feature:trip:ListModeIndexTest :feature:trip:TripScrollMathTest :feature:trip:DayListRowTest :feature:trip:ZoneChangeCardTest :feature:trip:ViewToggleTest :feature:trip:ListInvalidationTest :feature:trip:TripPageStatesTest :feature:trip:TimelineInvalidationTest"` passes; every JVM test < 1 s, every Robolectric test < 5 s (`build/test-results/*/timings.json`).
3. `TripStatesGuardsTest` passes for all 10 boards × 4 profiles × en/nl × light/dark.
4. `make captures ONLY=PlanList,PlanListScrolled,PlanLoading,PlanNoPlan,PlanRemote,PlanRemoteLoadingNL,PlanRemoteOffline,PlanRemoteOfflineNL,ErrorNotFound,ErrorServer` produced light and dark images, and the Opus reviewer compared them with the boards; every difference is in §5.1 or fixed.
5. `TripListSmokeTest` passes on `pixel` in < 10 s.
6. `make perf-timeline`: `listScroll` exists and meets the emulator gates (functional assertion passes; peak RSS ≤ 150 MiB); `timelineFirstFrame` ≤ 800 ms; the timeline rows pass every emulator gate (the CPU band included) and each `FLAGGED` trend has a recorded cause; `Scripts/check-benchmarks.py --suite timeline` passes and fails if `listScroll` is missing. Numbers recorded in `docs/perf-timeline.md`.
7. The regenerated baseline profile contains list-row classes (`grep -c "feature/trip/list/DayListRow" <profile>` > 0).
8. A debug run (mock data source) opening a remote trip, toggling list mode and pulling to refresh is StrictMode-clean (`adb logcat -s StrictMode` empty for the app).
9. No new string literals reach `Text`; no raw colors or `MaterialTheme.colorScheme` outside `:core:designsystem` (Konsist, Lint).
10. plan.md 6c tasks "List mode", "States", "List benchmark" and "Tests" are ticked in the commits named above.

---

## 8. Copy (generated by 0c; nothing to add)

Find each resource by its `<!-- plan: … -->` comment in the generated Plan strings; the names below are the expected `plan_<slug>` names. If a name differs, use the generated one.

| iOS key | en | nl | Expected resource |
|---|---|---|---|
| Show as list | Show as list | Als lijst tonen | `plan_show_as_list` |
| Show as timeline | Show as timeline | Als tijdlijn tonen | `plan_show_as_timeline` |
| Trip actions | Trip actions | Reisacties | `plan_trip_actions` |
| Loading your plan | Loading your plan | Je plan laden | `plan_loading_your_plan` |
| Downloading this trip’s plan… | (same) | Het plan van deze reis wordt gedownload… | `plan_downloading_this_trips_plan` |
| No plan yet | No plan yet | Nog geen plan | `plan_no_plan_yet` |
| Your flights are saved. Check your answers to make the plan. Once it’s made, it’s stored on this phone and works offline. | (same) | Je vluchten zijn opgeslagen. Controleer je antwoorden om het plan te maken. Daarna staat het op deze telefoon en werkt het offline. | `plan_your_flights_are_saved_check_your_an_0357a9` |
| Check answers and make plan | (same) | Antwoorden controleren en plan maken | `plan_check_answers_and_make_plan` |
| Making the plan needs a connection. | (same) | Om het plan te maken heb je verbinding nodig. | `plan_making_the_plan_needs_a_connection` |
| This plan downloads when you’re online | (same) | Dit plan wordt gedownload zodra je online bent | `plan_this_plan_downloads_when_youre_online` |
| Plans for active trips and trips in the next 7 days are always on your phone. This one is further away, so its plan opens once you’re connected. | (same) | Plannen van actieve reizen en reizen in de komende 7 dagen staan altijd op je telefoon. Deze reis ligt verder weg, dus het plan opent zodra je verbinding hebt. | `plan_plans_for_active_trips_and_trips_in_aae908` |
| Try again | Try again | Opnieuw proberen | `plan_try_again` |
| Your other trips keep working offline. | (same) | Je andere reizen werken gewoon offline. | `plan_your_other_trips_keep_working_offline` |
| Not found | Not found | Niet gevonden | `plan_not_found` |
| Something went wrong | Something went wrong | Er ging iets mis | `plan_something_went_wrong` |
| We can’t find that | (same) | We kunnen dit niet vinden | `plan_we_cant_find_that` |
| We’re having trouble | (same) | Er is een probleem | `plan_were_having_trouble` |
| The link may be old, or the trip was deleted. | (same) | De link is misschien verouderd, of de reis is verwijderd. | `plan_the_link_may_be_old_or_the_trip_was_deleted` |
| It’s on our side, not yours, and your plans are safe. Try again in a few minutes. | (same) | Het ligt aan ons en je plannen zijn veilig. Probeer het over een paar minuten opnieuw. | `plan_its_on_our_side_not_yours_and_your_p_b30437` |
| Go to my trips | Go to my trips | Ga naar mijn reizen | `plan_go_to_my_trips` |
| Open Today | Open Today | Open Vandaag | `plan_open_today` |
| Open my plan for today | (same) | Open mijn plan voor vandaag | `plan_open_my_plan_for_today` |
| Plan summary | Plan summary | Plansamenvatting | `plan_plan_summary` |
| Your plan | Your plan | Je plan | `plan_your_plan` |
| Home · %@ / There · %@ | Home · %1$s / There · %1$s | Thuis · %1$s / Daar · %1$s | `plan_home_x` / `plan_there_x` (already used by 6b's `DualClocks`) |
| %@ · now | %1$s · now | %1$s · nu | `PageCopy` key (built in `ListRows`) |
| Marked done / Skipped | Marked done / Skipped | Als gedaan gemarkeerd / Overgeslagen | `PageCopy` keys |
| Important / Optional | Important / Optional | Belangrijk / Optioneel | `PrepareBlock.priority` (1b) |

The "7 days" in the offline copy is fixed copy; the server's `mobile_prefetch_days` may differ. Flag it to the copy owner, not the app (research §3.18).

---

## 9. Review checklist (Opus reviewer)

- [ ] **Resolve order** is untouched (1b), and `TripPageInputs.state()` passes `keepOffline ?: true`, `missingAfterSync = hasLoaded && !recordPresent && syncCompleted`, `tombstoned = deletedAt != null || deletedLocally`.
- [ ] **Fetch model** matches iOS: `online` starts true; `retry` is a no-op while a job runs; offline → `Offline` without a request; 404 → `NotFound`; other failures → `Failed`; reconnect only on a false → true transition while started; `reconnectPending` honoured after the fetch; page-owned cancellation rethrown, foreign cancellation → `Failed`; no `catch (Throwable)`.
- [ ] `syncCompleted` only from `Finished` with the current epoch and `SyncState.Idle`; the replayed event counts.
- [ ] `awaitingSync` is a route parameter (default false) and clears when a record appears.
- [ ] No second path monitor: each page gets a new 4d `ConnectivitySyncPathMonitor` (current state first, INTERNET + VALIDATED, `distinctUntilChanged`, `stop()` on page stop); collected off main (`flowOn(io)` at the call site if 4d's class does not already).
- [ ] **View mode** lives in the ViewModel, defaults to timeline, is not in `SavedStateHandle` or any store; the `:list` seed is debug-only.
- [ ] **Toggle**: present only for `Ready`/`Loading`/`RemoteLoading`, enabled only when ready with a prepared plan; flips and scrolls to the anchor day's header in the same handler; content descriptions "Show as list"/"Show as timeline"; Material Symbols glyphs; tag `trip.viewToggle`.
- [ ] **List items**: one item per row, keys `r:<dayId>:<blockId>`/`z:<dayId>:<blockId>`, content types `listRow`/`zoneRow`; first row of a day has no top gap, the rest 8; no `LaneHeadings` in list mode; 6b's timeline layout unchanged (6b captures identical).
- [ ] **Rows** match iOS metrics (§5.1 D1–D5): swatch 28, gap 12, padding 14/12, radius 22, 1 dp border dashed 4/4 for optional, alpha 0.7 when marked, tag only for core/optional, status icon check/skip-forward 18 in `dawnText`; trailing stacks only when it does not fit; label and "now" variants exactly as iOS; one semantics node per row with `Role.Button` and a click action.
- [ ] **Zone card**: pin circle 44 in dawn, title `planCardTitle`, plates `skyCopy` on `skyCopyBackground` radius 10 padding 8/4, padding 20, radius 26, sky gradient; label joined with ". ".
- [ ] **Clock**: rows read `DayClock` only in `derivedStateOf`; `ListInvalidationTest` proves 0 recompositions per minute tick inside a block and exactly the flipping rows across a boundary; scroll recomposes no composed row.
- [ ] **No formatting in composition**: all row strings come from `ListRows`, built off main under the revision guard.
- [ ] **States** render per §6 Task 7: metadata line only when not ready; skeleton 120/44/380 with the downloading line only when remote; remote summary with "Your plan", clock shift and remote clocks; state cards with the iOS metrics and board glyphs (calendar in `dawnText`, cloud-upload in `mutedForeground`); no-plan button disabled; retry is `Outline`/`Touch`; error page with Dawn and OnSky pills at 24/48/8, back only in the top bar, `trip.error.title` a heading.
- [ ] **Pull to refresh** works on every state, including the error pages.
- [ ] **A trip not on the phone** after a finished sync shows `ErrorNotFound`; a tombstoned trip too.
- [ ] Guards: 10 boards × 4 profiles × en/nl × light/dark, all pass; no exemption beyond `trip.skeleton`.
- [ ] Captures compared with every board; each remaining difference is one of §5.1 D1–D15 or recorded with its board name.
- [ ] `listScroll` exists, uses exactly 15 flings and the `timelineScroll` metrics and compilation mode, asserts progress, and is required by `check-benchmarks.py`; budgets rows added; numbers in `docs/perf-timeline.md`; baseline profile regenerated.
- [ ] Tests follow AGENTS.md: exact tag queries (no tag-prefix scans), no sleeps, one emulator journey, budgets met; ported tests keep Swift names (`TripPageFetchTests` methods).
- [ ] Every string is a resource with nl; no new copy; no raw colors or dimensions outside generated tokens (`PlanMetrics`, `CfTheme`).
- [ ] Commits: eight (plus an optional perf-record commit), Conventional, lower-case imperative subjects, no trailers; plan.md ticks in the named commits.

---

## 10. Open questions (proceed on the default; the orchestrator adds them to plan.md "Open questions for the user")

| # | Question | Default |
|---|---|---|
| OQ-6c-1 | List row metrics: iOS (14/20 600 title, 13/18 interval, padding 14/12, no min height) or board (16/22 500, 14/20, padding 8/12, min height 64)? | **iOS** (§5 rule 1; 6e builds on it). D1–D4. |
| OQ-6c-2 | Error page: iOS text styles without moon, dots or plate (built), or the board's `SkyFrame` (moon 40, dot pattern 20 %, uppercase eyebrow, Fraunces 30/36, body plate)? The user removed pattern dots from splash and Welcome (iOS S2, 2026-11-06). | **iOS text, board buttons** (D9, D10). |
| OQ-6c-3 | State cards: iOS metrics (44 circle, 16/16 padding, 1 dp dash 4) or the board's (56/52 circle, 32/20 or 28/20 padding, 2 px dash for no plan)? | **iOS** (D6, D7; 6e's wide variant extends iOS). |
| OQ-6c-4 | The no-plan "Check answers and make plan" button: disabled (iOS never wires it at the pin) or wired to the wizard when 9c lands? | **Disabled now**; 9c/9h wires it and enables it when online. |
| OQ-6c-5 | Remote loading on phone: iOS skeleton (built) or the board's pills/bar/lane grid? | **iOS phone skeleton**; the board's version is 6e's `WidePlanSkeleton` (D8). |
| OQ-6c-6 | Android-only test tags `trip.skeleton`, `trip.meta`, `trip.remote.summary`, `trip.remote.retry`, `trip.noPlan*`, `trip.error.*`, `trip.zone.<id>`: acceptable? | **Yes**; iOS identifiers are kept where they exist. |
| OQ-6c-7 | List mode after a plan replacement with changed day ids: jump to the replacement day's header (offset 0) or keep a row-level position? | **Header of `replacementAnchor`'s day**, offset 0. |
| OQ-6c-8 | Error page top bar: show a back arrow (iOS keeps the system back) although the board has no bar? | **Back arrow only**, transparent over the sky. |
| OQ-6c-9 | Offline hint: INTERNET + VALIDATED (Android, as 4d) vs iOS `satisfied` (no validation). A captive portal shows "downloads when you're online" without a request. | **VALIDATED** (consistent with sync). |
| OQ-6c-10 | "Downloading this trip’s plan…" colour: `foreground` (iOS default) or `mutedForeground` (board and iOS `WidePlanSkeleton`)? | **`foreground`** (iOS phone skeleton). |
| OQ-6c-11 | Eyebrows ("Your plan", error eyebrow) are not uppercased on iOS; the boards uppercase them. | **iOS (not uppercased)**, same as 6b's summary. |
| OQ-6c-12 | The Loading board shows the day pills; iOS's skeleton has a 44 capsule instead. | **iOS** (D14). |
| OQ-6c-13 | Row keys include the day id (`r:<dayId>:<blockId>`), unlike research §9.5 (`r:<blockId>`), because a block crossing midnight appears in two days. | **Include the day id.** |
| OQ-6c-14 | `TripPagePathMonitor`: a new Android class (earlier draft) or 4d's `ConnectivitySyncPathMonitor`, which already ports the same behaviour? | **Reuse 4d's class**, one instance per page (iOS makes one `TripPagePathMonitor` per model). |
| OQ-6c-15 | 6b's non-ready seam holds a page-level `trip.timeline.preparing` node; 6c replaces the seam with the state list. Keep the node? | **Drop it.** Only the per-day tag inside `DayTimeline` is read by 1d/1e tests and benchmarks; if a 6b test reads the page-level one, change that test to `trip.skeleton`. |
| OQ-6c-16 | List mode keeps 6b's page-level `trip.timeline.ready` marker, so it no longer means "a timeline is on screen". | **Keep it** (1e's benchmarks wait for it on launch); list-mode waits use the toggle's "Show as timeline" description and exact `trip.block.<id>` / `trip.day.<dayId>` tags. |


## Implementation results (2026-11-19)

- Preflight branch matched. Existing untracked later-phase briefs were left untouched. Mechanical deltas and safe resolutions are recorded in the preflight bullets above and their signed docs commits. OQ-A272–OQ-A287 carry this brief's defaults; OQ-A288 resolves readiness semantics, OQ-A289 resolves the skeleton ink threshold and OQ-A290 preserves the landed benchmark workload.
- The fetch/model, list values, rows, view toggle, state pages and shell destinations pass targeted checks. The cross-midnight key test was proven with a failing mutation and then restored. A late failure after fetch cancellation preserves Loading. Replacing day ids in list mode waits for the published replacement index and restores its header at offset zero.
- List invalidation: 124 independently named cases, 8.355 s total, max 2.920 s. The 7 existing timeline invalidation cases pass, including `scrollingWithinOneDayDoesNotRecomposePageChrome`. These are JVM/Compose results, not emulator performance claims.
- State structure: 14 cases pass (4.217 s total, max 3.058 s). Copy variants are checked on the JVM; screen tests use tags, structure, accessibility semantics and callbacks.
- Final layout evidence: **40 state/list guards + 64 unchanged timeline guards pass**, 13.19 s tier wall, no budget overrun. The new cases are light at small/nl, pixel8/en, largeFont/nl and an additional minimum280/nl, following the current plan rather than the superseded Cartesian matrix. The 16 shared guard controls pass. Hiding the loading skeleton shapes makes all four Loading ink checks fail; restoring them passes, without changing a UI token.
- The **single capture invocation** produced **52 PNGs** in `/Users/rickgout/Sites/chronoflow-android/build/captures/`: 36 new state/list images (en/nl, light/dark; NL wrappers stay nl) plus all 16 6b recaptures. New capture cases take 3.490 s total; the whole capture tier takes 15.56 s. Local inspection of all new variants against the boards' markup/metrics and pinned iOS appearance found the retained differences D1–D18 above and the inherited 6b Appendix C differences. Independent reviewer sign-off is not claimed.
- `make targeted ONLY="app:TripListSmokeTest"`: **PASS on pixel**, 2.440 s test / 10.85 s tier wall; the app's StrictMode IO counter is **0**. The journey seeds directly into list mode and toggles to the same day's timeline. It selects among three hardcoded first-row tags for the rebased mock's possible opening days; it never scans tag prefixes. Pixel remains running.
- Evidence is archived under `build/phase6c-validation/` (layout, shared guard controls, captures and pixel results). `make captures` was run once, with the ten named boards and the six existing timeline board names in ONLY.
- Performance/profile commands and the full commit gate remain explicitly deferred to the user: `make baseline-profile`, `make perf-timeline`, `make check`. No output or measurements are fabricated for commands not run. The separate remote-open/list/pull StrictMode manual pass is not claimed by the ready-list smoke journey.
- Task 9 implements `listScroll`, its frame/memory budgets, required-test/iteration checks and the profile journey extension. All **48 benchmark-checker tests pass** (2.406 s focused run). The large60 opening-row assertion passes against the actual fixture (0.243 s focused test). `:benchmark:compileBenchmarkReleaseKotlin :app:lintDebug :feature:trip:lintDebug` passes in **36 s** with JDK 17, Android SDK configured and the Kotlin compiler in-process.
- The **single** `make quick` invocation passes its **3,312 JVM/Robolectric cases** (32.40 s tier wall, 0 overruns) and **37 build-logic cases** (1.97 s). Its tooling suites pass 138 script cases (17.024 s), 25 contract cases (0.110 s), 49 localization cases (0.018 s), then 8 of 9 icon cases (0.032 s). It stops at `test_manifest_names_all_generate`, which still expects 35 glyphs instead of 37. A separate signed fix updates that count; the failed method passes (0.013 s). Quick was not repeated. No other tooling suite remained after that final method's suite.
- The remaining `make timing` recipe passes **all six gates**, 3.03 s tier wall. Host measurements are in `docs/perf-timeline.md`; these are JVM budgets, not emulator measurements. Evidence from the final checks is archived under `build/phase6c-validation/final/`.

Final quick results for every named host class (seconds are test wall time; all pass):

| Class | Cases | Total s | Maximum s |
|---|---:|---:|---:|
| `TripPageInputsTests` | 4 | 0.001 | 0.001 |
| `TripPageFetchTests` | 8 | 0.005 | 0.002 |
| `TripPageViewModelStateTest` | 10 | 0.080 | 0.037 |
| `ListRowsTests` | 5 | 0.072 | 0.056 |
| `ListModeIndexTest` | 3 | 0.191 | 0.118 |
| `TripScrollMathTest` | 7 | 0.001 | 0.001 |
| `DayListRowTest` | 5 | 4.473 | 4.107 |
| `ZoneChangeCardTest` | 2 | 0.030 | 0.017 |
| `ViewToggleTest` | 10 | 0.290 | 0.054 |
| `ListInvalidationTest` | 124 | 6.412 | 0.615 |
| `TripPageStatesTest` | 14 | 0.304 | 0.037 |
| `TimelineInvalidationTest` | 7 | 0.346 | 0.066 |
| `TripPageTests` | 4 | 0.087 | 0.061 |
| `TripPageStateTests` | 1 | 0.006 | 0.006 |
| `Large60FixtureTripPageSourceTest` | 2 | 0.101 | 0.082 |
