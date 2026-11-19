# Phase 6e: Wide and foldable trip page

- **Branch:** `feat/phase-6e-trip-wide`, from the stack top once 6d is in the stack.
- **Depends on:** 6d (and through it 6a–6c, 1a–1e, 0a–0e, 2, 4a–4e). **Wave:** 12 (parallel with 7, 9f, 10b and 12b). **Milestone:** M3.
- **Plan entry:** `plan.md` "Phase 6e: Wide and foldable trip page (`feat/phase-6e-trip-wide`)" (l.2446–2477). Decisions **A8** (config changes handled in place), **A10** (window policy), **A12** (timeline architecture), **A19** (tests). Spec: "Android spec: window policy" (l.357–371), "Android spec: state survival" (l.373–383). Performance budget (l.491–538).
- **Tests:** every test follows `AGENTS.md` "Tests" (cheapest layer, budgets, exact queries, no sleeps, hang caps, one tier annotation per class, no JUnit `Timeout` rule). Read it before Task 1.
- **API endpoints:** none new. The remote states reuse 6c's `GET /v1/trips/{uuid}` (`fetchTripDetail`: 200 `ApiTripDetail {trip, plan, notices, timeline, checkState, detailVersion}`; 404 → not found; offline → remote offline; `03 §5` row 38).
- **Checked:** 2026-11-16 by an adversarial checker against iOS `f391a1fa` (`TripPageLayout`, `TripWindowSizeHost`, `WidePlanSkeleton`, `TripScrollTracker`, `TripDayRow`, `TripPageView`, `DayListView`, `DayTimelineView`, `PlanPageSafeArea`, `PlanMetrics.generated.swift`, `AppTests/TripPageWideLayoutTests.swift`, iOS `plan.md` 15e, the PlanUI catalog), the six boards, plan.md, AGENTS.md and the **checked** 6b/6c/6d briefs. Fixed: the page seams to 6b's real ones (separate `h:<dayId>`/`t:<dayId>` items with content types `dayHeader`/`dayTimeline`, `TripListIndex.headerIndex/timelineIndex`, `TripScrollMath`, `select(dayId, offsetDp)` + `saveAnchor()`, `CompactHeader(day, alpha, visible, horizontalPadding)`, probe kinds `SUMMARY_CLOCK`/`PILLS`/`COMPACT`), 6c's (`DayListRow`, `ZoneChangeCard(row, onBlock)`, `TripPageStateCard`, `TripPageSkeleton`/`trip.skeleton`, `RemoteSummaryCard`, `trip.remote.retry`, row keys `r:`/`z:<dayId>:<blockId>`), 6d's (`vm.sheet: StateFlow<TripSheet?>`, `openStep`/`openLegend`/`openTripActions`, tags `step.sheet`/`legend.priorities`/`trip.actions`), the tracker's capture/target rules for the split items, the fold-check freeze seed (`CF_PLAN_TEST_FREEZE_CLOCK=1`), the ±25 % perf rule (flagged, not a gate), iOS `plan.md` and board line references, the board's day menu (it is the system menu, not a custom popover), and added OQ-6e-18 (skeleton bar radius) and OQ-6e-19 (phone list outline, 6d vs iOS).

- **Stack check (2026-11-19, against 1bee5b8 + d47cc95, the 6d top).** P1, P2, P4, P5, P7, P8 and P9 hold as written. Deltas:
  - `TripSheet` (`Step(blockId)`, `Legend`, `StepsLegend`, `TripActions`) is `@Serializable` in `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/Route.kt:77`, not under `feature/trip/…/step/`. The sheets are hosted by `feature/trip/…/step/TripPageSheets.kt` through `CfModalBottomSheet` (`core/designsystem/…/system/CfDialogs.kt`; `sheetMaxWidth = SheetMetrics.maxWidth` = 640 dp; the sheet window exports test tags as resource ids, and the outer sheet tag is `trip.sheet`). The trip-actions content tag `trip.actions` is in `feature/trip/…/actions/TripActionsSheet.kt`.
  - **6d did not add a list-row `selected` outline** (its brief deferred it to 6e, l.102). `DayListRow(row, clock, onBlock, modifier, actions, onCheck)` and `ZoneChangeCard(row, onBlock, modifier)` have no `selected` parameter; the existing border helper is `Modifier.tripOutline(…)` in `feature/trip/…/list/TripOutline.kt`. Task 3 adds the `selected: () -> Boolean` lambda form itself; phone call sites pass `{ false }` (OQ-6e-19 default: no phone outline change).
  - **Window-size seam already exists** in the shell: `AppShellScaffold.kt:57–62` uses `currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)` (`core/designsystem/…/layout/WindowPolicy.kt`). Task 1 must read the window width from the same source (window, not container, as iOS) and reuse `WindowPolicy`; it adds only what the trip page needs beyond the boolean (the width in dp for `TripPageLayout.resolve`).
  - `TimelineProbe.Kind` is `STATIC, CLOCK, TARGETS, HEADER, ANCHOR, SUMMARY_CLOCK, PILLS, COMPACT, LIST_ROW` (6c added `LIST_ROW`).
  - `TimelineBenchmark` also has `listScroll` (6c). The baseline-profile generator (`benchmark/…/BaselineProfileGenerator.kt`) now opens the step sheet, checks and undoes a step through bounded structural waits (6d).
  - The shell's refusal snackbar host pads with the Rail bottom + horizontal safe insets (6d `d47cc95`); a wide trip page must not add a second inset for it.
  - OQ rows in `plan.md` end at OQ-A304; this phase's open questions start at OQ-A305.
  - **Implementation preflight (2026-11-19):** `DayTimeline` owns `trip.day.<id>` on its outer `Box` and accepts `headings`; there is no enclosing tagged item `Column`. Task 3 moves only that tag to `WideDayRow`, keeping the existing timeline and heading subtree in its first child at every width.
  - **Pin units/allocation:** 1b's immutable `TripWideListPosition` takes dp and caps at the generated 700 dp value. Task 4 keeps that public port unchanged and uses a cached mutable `WidePinPosition` in px, with a density-scaled max height, for the allocation-free layout/placement path; JVM tests compare its results with the 1b formula after unit conversion.
  - **Task 4 font-scale verification:** fixed band geometry uses generated dp metrics multiplied by the already-capped `LocalDensity.fontScale` (36 × 1.3 = 46.8 dp), shared by band height and pin. Platform `sp.toDp()` did not yield the specified fixed linear band in the hosted test. Phone typography and its existing 36 sp current-day threshold stay unchanged.
  - **Task 11 seed verification:** `TestSeed.parse` requires a canonical UUID; the earlier literal `fold-check` would disable all `CF_*` keys. The script uses `019a56e0-0000-7000-8000-00000000006e`, preserving the landed parser. A preparatory seeded launch observes the opening day and chooses its stable fixture block id; the second seeded launch opens that step before the measured fold sequence. All-window dumps use `uiautomator dump --windows` to keep the underlying day and the sheet in the same evidence file.

Tags: **[F]** fact verified in a source (path:line). **[R]** a rule this brief sets for Android. Paths: `feature/trip/…/X.kt` = `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/X.kt`; `core/plan/…/X.kt` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/X.kt`; iOS paths are relative to `/Users/rickgout/Sites/chronoflow-ios` and are read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`.

---

## 1. Goal

From a **window** width of 600 dp the trip page shows the timeline and that day's list side by side, and it keeps everything across folds, rotations and resizes:

1. **Wide layout** [F `PlanUI/TripPage/TripPageLayout.swift:10–15`, iOS `plan.md:1766`]: window width < 600 dp → phone (unchanged, with the Timeline/List toggle). ≥ 600 dp → each day is a row: timeline on the left (fills), the day's list in a **280 dp** column on the right (**320 dp** from a 900 dp window), column gap **16 dp**, page gutter **20 dp** (phone 16). No view toggle. Width only, never height or orientation.
2. **Pinned list** [F `TripPageLayout.swift:18–30`, `TripDayRow.swift:45–80`]: each day's list stays beside its day while the page scrolls, pinned below the compact header band, shrinking at the day's end, scrolling on its own when taller than the space (max 700 dp). A tap on a timeline block or a list row opens the step sheet and outlines that list row in dawn.
3. **Remote states on wide** [F `TripPageView.swift:150–157, 481–513`, `WidePlanSkeleton.swift`]: downloading shows `WidePlanSkeleton`; offline shows the wide variant of 6c's state card.
4. **In-place size changes** [F iOS `plan.md:1766–1775`; plan A8]: folding, unfolding, rotating, split-screen resizing and display-density changes do **not** recreate the activity and keep the anchor `(dayId, offsetInDay)` and any open sheet (step, legend, trip actions).
5. **Proof:** JVM tests for the math, Robolectric tests with a **window-size fake** (411 → 841 → 411 dp keeps the anchor and the sheet), guards at `foldInner` and `landscapeSideNav`, board captures, the wide invalidation guard, `make perf-timeline` (phone rows pass every emulator gate, the CPU band included; ± 25 % trends flagged with a cause; plus a report-only wide row). The `fold` AVD check is scripted and runs **only on request**.

The phase is done when §7 holds and every 6e checkbox in `plan.md` is ticked.

---

## 2. Before you start: check what earlier phases left

Do not rebuild earlier phases. Find each name below. If a name differs, use the existing one and say so in the commit body. If an item is **missing** (not just renamed), stop and report it to the orchestrator.

| # | Expected (phase) | Where (expected) | Used for |
|---|---|---|---|
| P1 | `PlanMetrics` (`object`, `Double` dp): `wideBreakpoint 600`, `wideLargeBreakpoint 900`, `wideListWidth 280`, `wideLargeListWidth 320`, `wideColumnGap 16`, `wideGutter 20`, `gutter 16`, `wideListMaxHeight 700`, `compactHeaderHeight 36`, `compactFadeDistance 24`, `timelineLabelBreakpoint 640`, `rowGap 8`, `sectionGap 12`, `smallGap 4`, `laneGap 4`, `gutterWidth 44`, `blockIcon 14`, `touch 44`, `minTouchTarget 48`, `rowRadius 22`, `cardRadius 26`, `textPlateRadius 10`, `tinyTileHeight 56`, `skeletonSummary 120`, `prioritySampleWidth 40`, `emphasisBorder 2`, `border 1`, `cardPadding 16`, `icon 16`, `wideRemoteBodyWidth 420`, `wideRemoteIcon 52`, `wideRemotePadding 28`, `wideSkeletonCaffeineOffset 150`, `wideSkeletonDayHeight 64`, `wideSkeletonHeight 360`, `wideSkeletonLightHeight 90`, `wideSkeletonLightOffset 60`, `wideSkeletonPillWidth 88`, `wideSkeletonTravelHeight 110`, `wideSkeletonTravelOffset 200`, `planListTitleLineHeight 18`, `planLaneLineHeight 14` (0b, 1b) | `core/plan/…/generated/PlanMetrics.generated.kt` | every value in this phase |
| P2 | `data class TripPageLayout(val listWidth: Double?)` with `isWide`, `gutter`, `companion fun resolve(width: Double)`; `data class TripWideListPosition(offset, height)` with `operator fun invoke(dayTop, dayHeight, viewportHeight, stickyTop, contentHeight)`; `data class TripPageAnchor(dayID, labelDate, offset)`; `TripPageLayoutTests` (1b) | `core/plan/…/trippage/` | layout math |
| P3 | `DayTimeline(day, layout, key, …)`, `NowLine`, `TimelineTargets`, `TimelineDrawingRepository`, `DayDrawingKey(…, availableWidthPx, …, compact, …)`, `TimelineProbe` (`Kind { STATIC, CLOCK, TARGETS, HEADER, ANCHOR }` from 1d plus `SUMMARY_CLOCK`, `PILLS`, `COMPACT` from 6b; `LocalTimelineProbe`), `CountingTimelineProbe` (test), `TimelineTestHost` (retargeted to `TripPage` by 6b Task 12), `TimelineInvalidationTest` (1d, extended by 6b). 1d's `TimelineListIndex` and `TimelinePage` no longer exist (6b deleted them) | `feature/trip/…/timeline/` | timeline item, probe |
| P4 | `TripPageViewModel` (`anchor: StateFlow<TripPageAnchor>`, `select(dayId, offsetDp)` = offset in a plain field, anchor changes only on a day change; `saveAnchor()` writes `trip.anchor.day`/`trip.anchor.labelDate`/`trip.anchor.offset` to `SavedStateHandle`; `jump(dayId)`; `scrollCommands`; `openingScroll()`), `TripPageSource` (DB and fixture), `TripListIndex` (`…/page/`: `summary` 0, `days` 1, `headerIndex(dayId)` = 2 + 2i, `timelineIndex(dayId)` = 3 + 2i, `end`, `hidden`, `dayAt`, `itemCount`), `TripScrollMath` (`currentDay(visible, index, thresholdPx)`, `headerBottomPx`, `compactAlpha`, `headerScrolled`, `offsetInDayPx(visible, timelineIndex)`, `restoreTarget(index, anchor, offsetPx)`), `TripPage(viewModel, …)` with its `LazyListState` and the item table: `h:<dayId>` (`dayHeader`) then `t:<dayId>` (`dayTimeline`, content `Column(Modifier.testTag("trip.day.$id")) { LaneHeadings(null); DayTimeline(...) }`), `TripTopBar` (title menu `trip.dayMenu`, `trip.dayMenu.day.<n>`), summary card (`trip.summary`, legend button `trip.legend`), `DayNavigation`, `DayHeader`, `CompactHeader(day, alpha, visible, horizontalPadding)` (`trip.compactHeader`, `heightIn(min = 36.sp→dp)`, alpha `compactAlpha`), `LaneHeadings(day: PreparedDay?)`, end card `trip.plan.end`, perf entry `cf.trip-page=…`, `CF_PLAN_TEST_POSITION=summary` (6b) | `feature/trip/…/page/` | page host |
| P5 | List mode (`timeline: StateFlow<Boolean>` + `toggleView()` in the ViewModel, not persisted; rows as lazy items keyed `r:<dayId>:<blockId>` / `z:<dayId>:<blockId>`, content types `listRow` / `zoneRow`; `trip.viewToggle`; `ListRows.build` → `DayListRows(dayId, rows)`; the list-mode index with the common `headerIndex`/`currentDayIndex` interface), `DayListRow(row, clock, onBlock)` (tag `trip.block.<blockId>`), `ZoneChangeCard(row, onBlock)` (tag `trip.zone.<blockId>`), the states, `TripPageStateCard(icon, iconTint, title, titleStyle, text, modifier, actions)` (offline tag `trip.remote.offline`, retry `trip.remote.retry`), `TripPageSkeleton(downloading)` (tag `trip.skeleton`), `RemoteSummaryCard` (tag `trip.remote.summary`) (6c) | `feature/trip/…/list/`, `…/state/` | list column, remote states |
| P6 | `TripPageViewModel.sheet: StateFlow<TripSheet?>` (`TripSheet.Step(blockId)`, `Legend`, `StepsLegend`, `TripActions`; mirrored into the route key), `openStep(id)`, `openLegend()`, `openTripActions()`; sheet tags `step.sheet`, `legend.priorities` (Legend), `legend.sheet` (StepsLegend), `trip.actions`; 6d's list-row `selected` outline; seed `CF_PLAN_TEST_SHEET=step:<id>`. Bottom sheets use `sheetMaxWidth = 640.dp` (plan "window policy") (6d) | `feature/trip/…/step/`, `…/actions/` | sheet survival |
| P7 | `DeviceProfiles` (`small 360×640`, `pixel8 411×914`, `foldInner 841×701`, `landscapeSideNav 914×411` with right inset 48 dp, `largeFont`), `ProfileRule`, `ProfileHost`, `LocalSafeInsets`, `Guards`, `BoardCapture`, tier annotations `@Logic`/`@Layout`/`@Capture`/`@Perf` (0e) | `core/testing/`, `core/testing-compose/`, `core/ui/` | tests |
| P8 | `TimelineBenchmark` (`timelineFirstFrame`, `timelineScroll`, `timelineLastDay`), `benchmark/budgets.json`, `Scripts/check-benchmarks.py`, `make perf-timeline`, `make baseline-profile` (1e) | `benchmark/`, `Scripts/` | perf |
| P9 | `MainActivity` declares `configChanges="orientation|screenSize|screenLayout|smallestScreenSize|density|keyboard|keyboardHidden|navigation|uiMode"` (0a/3a, plan A8) | `app/src/main/AndroidManifest.xml` | in-place changes |

If 6a already added a window-size seam for the shell (bar vs rail), reuse it in Task 1 instead of adding a second one, and say so.

---

## 3. Inputs to read

**This repo**
- `plan.md`: l.2017–2047 (this phase), A8 (l.95), A10 (l.97), A12 (l.99), "Android spec: window policy" (l.357–371), "Android spec: state survival" (l.373–383), "Performance budget" (l.491–538), "Standard acceptance for every phase" (l.636).
- `AGENTS.md`: "Working rules", "Performance", "Tests", "Traps" (measured sizes stay in layout; rethrow `CancellationException`; no `Timeout` rule).
- `docs/research/04-timeline.md` §3.16 (wide layout), §4.8 (resize, fold, replacement), §9.8 (where scroll values are read), §9.12 (wide and foldable layouts), §14 OQ-T1, OQ-T28. Quoted in Appendix A.
- `docs/research/14-gaps.md` §3.3 (config changes handled in place), §3.4 (per-screen survival table), §4.1–§4.2 (window sizes, per-screen policy). Quoted in Appendix A.
- The briefs of the phases you build on: `docs/phases/phase-1b-timeline-layout.md` (Task 9: `TripPageLayout`, `TripWideListPosition`), `phase-1d-timeline-host.md` (rules §5, Tasks 5–7), and the 6b, 6c and 6d briefs in `docs/phases/`.

**Boards** (`_design/canvas/`; 669 × 951 px = dp; read the HTML, not just the title)
- `PlanWide.dc.html`: the base. Columns `grid-template-columns: minmax(0, 1fr) 280px; gap: 16px` (l.121); list `position: sticky; top: {{listTop}}px` (`listTop: 144`, l.338) `max-height: 700px; overflow-y: auto; padding-top: 8px` (l.139); rows `gap: 8px` (l.140), `min-height: 56px; padding: 8px 12px; border-radius: 22px` (l.298); main padding `12px 20px 40px`, gap 16 (l.330, both 0 when scrolled); summary card, day pills, full day header, timeline with `hourHeight 52` (l.275, 341); compact line `height: 36px`, grid `minmax(0, 1fr) 280px` with lanes and "Steps" (l.83–90, 312); day menu (l.180–186): a glass `role="menu"` panel titled "Go to day" (`menuTitle`, l.332) whose board comment says it is the system menu ("SwiftUI Menu on iOS, Compose DropdownMenu on Android"), not a custom popover.
- `PlanWideDark.dc.html`: wrapper `<dc-import name="PlanWide" theme="dark" from-top="{{no}}">` (opening day at its top, dark).
- `PlanWideScrolled.dc.html`: wrapper `from-top=no scrolled=yes scroll-hour=9` (the compact band visible).
- `PlanWideDayMenu.dc.html`: wrapper `… scrolled=yes day-menu=yes` (title menu open).
- `PlanRemoteWide.dc.html`: remote loading (default) and offline states, `lang` en/nl, `theme` light/dark. Loading (l.53–58): spinner 16 + "Downloading this trip's plan…" 14/20 muted, 4 pills 88 × 44, a 64 dp bar (radius 18; iOS 22, OQ-6e-18), then `minmax(0,1fr) 280px` gap 16: lanes 360 tall with blocks (top/height) 0/120, 60/90, 150/40, 200/110, radius 10, inset 4; list: 5 rows 56 tall, radius 22, gap 8. Offline: dashed 1 px border card, radius 26, padding 28 × 20, gap 12, 52 icon circle on `muted`, title 22/28, body 15/22 max 420, a 44-tall outline "Try again" button, then "Your other trips keep working offline." 13/18.
- `PlanRemoteWideOffline.dc.html`: wrapper `state="offline"`.

**iOS** (the behaviour reference, at `f391a1fa`; key parts quoted in Appendix B)
- `plan.md:1765–1785` "15e: Wide trip page": the decision (l.1766), tests and repair rounds (l.1767–1774), the Duo fold check (l.1775), "15e source interpretation" (l.1777), and reviews 2, 3 and 5 (l.1781, 1783, 1785): what broke on iOS and why.
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageLayout.swift` (whole, 30 lines).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripWindowSizeHost.swift` (whole, 45 lines): the breakpoint reads the **window** size, not the container ("The native tab rail consumes container width; the breakpoint is in window points").
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/WidePlanSkeleton.swift` (whole, 58 lines).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripScrollTracker.swift` (whole, 252 lines): `track`, `beginResize`, `cancelRestoration`, `resize`, `updateChrome`, `TripScrollRestoration`. The CADisplayLink 60-tick loop is iOS-only (`04 §0.3`).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripDayRow.swift` l.35–101 (the wide row: `DayHeader` on top, then one HStack at both widths with the list beside `clockContent` = `LaneHeadings` + timeline, **not** beside the header; `dayTop` = the list column's top, `dayHeight` = that HStack's height; `TripWideListPosition` inputs). On Android this is exactly 6b's `t:<dayId>` item: the header is the separate `h:<dayId>` item.
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageView.swift` l.110–221 (window size, layout resolve, `stickyTop`, compact width, wide skeleton, `onPreferenceChange` → `scroll.resize`), l.229–246 (toggle hidden when wide), l.351–368 (wide compact header), l.481–513 (`remoteOffline(wide:)`, `stateCard` wide values).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/DayListView.swift` l.23–45, 74–85 (`selectedBlockID`: the zone card gets a `cardRadius` `emphasisBorder` `dawn` overlay, l.28–33; a row's own border switches from `border` 1 to `dawn` 2 and drops the optional-priority dash while selected, l.77–84).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/DayTimelineView.swift` l.199–230 (`LaneHeadings`; `day == nil` on wide: empty 44 gutter).
- `Packages/ChronoFlowKit/Sources/DesignSystem/PlanPageSafeArea.swift` l.57–82 (`planPageRail`: unconsumed side insets are added to the gutter).
- `AppTests/TripPageWideLayoutTests.swift` (whole, 247 lines): the assertions you port (§6 Tasks 5 and 8).
- `Packages/ChronoFlowKit/Tests/PlanUITests/TripPageLayoutTests.swift` l.1–58 (already ported in 1b).

---

## 4. Out of scope (do not build)

- **No new screens or states without a board.** There is no wide board for: no plan, local loading, not found, server error, the step/legend/trip-actions sheets, Today, My trips, Settings. They render exactly as 6b–6d built them (iOS uses the same views at every width, `TripPageView.swift:61–69, 141–149, 455–479`); this phase only adds fit guards for them at `foldInner`.
- **Posture UI:** no tabletop or book posture, no hinge avoidance, no `FoldingFeature` logic (`14 §4.2`: a fold is a width change).
- **Board values iOS overrides** (each is an open question in §9 with the iOS default): the 52 dp/h hour scale (stays 40 dp/h everywhere, OQ-T1), the "Steps" label over the list, the board's single-row compact line, the custom glass day-menu popover and its "Go to day" title, the board's page spacing (main padding 12/20/40, gap 16), the city-name title and back label, the board's full sticky day header with the list pinned at 144 px, list rows of min height 56, an animated skeleton shimmer, the Optional items switch (A24: never).
- **Process death** (always a cold launch, A8) and **locale or font-scale recreation** (anchor from `SavedStateHandle`, sheet from the route key): built in 6b/6d; not changed here.
- **The `fold`, `small`, `api29` AVDs and `make matrix`:** on request only. This phase writes the fold-check script and its procedure; it does not run it.
- **No new visible copy.** Every string below already exists in the iOS catalogs and in 0c's generated resources.

---

## 5. Rules fixed by this brief [R]

1. **The breakpoint reads the window, not the container.** `TripPageLayout.resolve(windowWidthDp)` and `compact = windowWidthDp < PlanMetrics.timelineLabelBreakpoint (640)` use the **window** width (iOS `TripWindowSizeHost`). The page's content width (after the rail and insets) comes from constraints and only sizes columns.
2. **One item tree at every width.** 6b's items keep their keys and content types on phone and wide: `h:<dayId>` (`dayHeader`) and `t:<dayId>` (`dayTimeline`). The `t:` item's content is one custom `Layout`, `WideDayRow`, whose **first** child is always the timeline column (`LaneHeadings(null)` + `DayTimeline`) and whose second child exists only when wide. No `if (wide) A() else B()` above `DayTimeline`, and no different `LazyColumn`, `LazyListState` or sheet host per layout. (iOS review 3: reparenting across the breakpoint destroyed the tap gesture, iOS `plan.md:1769, 1783`.)
3. **The sheets sit outside every width branch.** The step, legend and trip-actions sheets are hosted once at the page root and read the ViewModel's selection; a width change never removes their composition.
4. **Measured sizes stay in layout.** No `onSizeChanged`, `onGloballyPositioned` or `BoxWithConstraints` inside an item, and no measured size written to state. The pinned list reads scroll values only inside its own layout lambdas (§6 Task 4).
5. **Scrolling invalidates no static layer and no composition.** During scroll on wide, only the pinned list's layout runs. Day items, rows, headers, the clock and the static timeline layer are not recomposed or redrawn (extends 1d's invalidation table, Task 9).
6. **Values only from `PlanMetrics`, the generated type scale and `CfTheme` colors.** This phase adds no Android-only number: every dp has a `PlanMetrics` name (§2 P1; `minTouchTarget` 48 is 1b's existing Android-only value).
7. **Test tags are the iOS ids:** `trip.wide.timeline.<dayId>` (timeline column, wide only), `trip.wide.list.<dayId>` (the pinned list viewport), `trip.wide.list.scroll.<dayId>` (its scroll container), `trip.wide.loading` (skeleton root), `trip.wide.loading.timeline`, `trip.wide.loading.list`, `trip.remote.offline`, `trip.compactHeader`, `trip.day.<dayId>` (the `t:` item root, i.e. `WideDayRow`; 6b puts it on that item's `Column`), `trip.scroll`, `trip.viewToggle` (absent on wide), `trip.block.<blockId>` (timeline target **and** list row on wide, as on iOS; tests disambiguate with `hasAnyAncestor(hasTestTag("trip.wide.list.<dayId>"))`). 6c/6d tags are reused unchanged (`trip.skeleton`, `trip.remote.summary`, `trip.remote.retry`, `trip.zone.<blockId>`, `step.sheet`, `legend.priorities`, `trip.actions`).
8. **Tier annotations:** Robolectric behaviour tests `@Logic` (they run in `make quick`), guards `@Layout`, captures `@Capture`, the benchmark `@Perf`. Each Robolectric test < 5 s, each JVM test < 1 s.

---

## 6. Tasks

Each task is one atomic Conventional Commit with no trailer. Tick the named `plan.md` checkbox (Phase 6e "Tasks") in the same commit; "Ticks: none" ticks nothing. Run only what you touched (`make targeted ONLY=…`); run `make quick` once before handing back. If your sandbox cannot run Gradle, `adb` or the emulator, write the code and report the exact command and its exact output; the orchestrator runs it.

### Task 1: Window size seam

**Commit:** `feat(ui): provide the window size to screens`. **Ticks:** none.

**Files**
- `core/ui/src/main/kotlin/net/chronoflow/core/ui/window/LocalWindowSize.kt`:
  ```kotlin
  /** The whole window in dp (iOS TripWindowSizeHost reads window bounds, not the container). */
  val LocalWindowSize = compositionLocalOf<DpSize> { error("LocalWindowSize is provided at the root and by ProfileHost") }
  ```
  `compositionLocalOf` (not static): only the trip page reads it, so a size change recomposes only readers.
- `MainActivity.setContent` (next to `LocalSafeInsets`): provide `LocalWindowSize` from `LocalWindowInfo.current.containerSize` (px) converted with `LocalDensity`. [verify] that `containerSize` is public and non-experimental in Compose UI 1.12 and tracks in-place resizes; if not, use `androidx.compose.material3.adaptive.currentWindowSize()` (adaptive 1.3.0, A1) and record which one in the commit body.
- `core/testing-compose/…/ProfileHost.kt`: also provide `LocalWindowSize provides DpSize(profile.widthDp.dp, profile.heightDp.dp)`.
- `core/testing-compose/…/WindowSizeFakeHost.kt` (test helper, new):
  ```kotlin
  class WindowSizeFake(initial: DpSize) { var size by mutableStateOf(initial) }
  @Composable fun WindowSizeFakeHost(fake: WindowSizeFake, insets: SafeInsetsDp = SafeInsetsDp(0f, 0f, 0f, 0f), content: @Composable () -> Unit)
  ```
  It provides `LocalWindowSize provides fake.size`, `LocalSafeInsets` from `insets`, LTR, and lays `content` out in `Box(Modifier.size(fake.size))` aligned top-start. Changing `fake.size` is an in-place resize: same activity, same composition, same `ViewModel`.
- Konsist (`:konsist`): feature code must not read `LocalConfiguration.current.screenWidthDp`/`screenHeightDp` (geometry comes from `LocalWindowSize`, constraints and insets). Add the rule; fix any existing hit in `:feature:trip` in this commit.

**Edge cases:** the value is the full window including system bars (edge-to-edge); a split-screen window reports its own size, not the display's.

**Tests**
- `WindowSizeFakeHostTest` (Robolectric `@Logic`, `core/testing-compose/src/test`): a probe composable reading `LocalWindowSize` and `BoxWithConstraints` shows 411 × 914 dp, then 841 × 914 dp after `fake.size = …` and `waitForIdle()`; the probe's `remember { Any() }` identity is unchanged (no recreation).
- `ProfileHostTest` (existing, extend): under `foldInner`, `LocalWindowSize.current == DpSize(841.dp, 701.dp)`.

### Task 2: Trip page frame (pure)

**Commit:** `feat(trip): resolve the trip page frame from the window`. **Ticks:** none.

**File:** `feature/trip/…/wide/TripPageFrame.kt` (package `net.chronoflow.feature.trip.wide`, pure Kotlin, no Compose):
```kotlin
data class TripPageFrame(
    val layout: TripPageLayout,       // 1b: resolve(windowWidthDp)
    val gutterDp: Double,             // layout.gutter: 20 wide, 16 phone
    val compact: Boolean,             // windowWidthDp < PlanMetrics.timelineLabelBreakpoint (640): icon-only tiles
    val listWidthDp: Double?,         // 280 / 320 / null
    val timelineWidthDp: Double,      // contentWidthDp − 2·gutter − (wide ? wideColumnGap + listWidth : 0), floored at 0
) {
    companion object {
        fun resolve(windowWidthDp: Double, contentWidthDp: Double): TripPageFrame
    }
}
```
- `contentWidthDp` is the page's own width from constraints, minus the unconsumed horizontal safe insets (iOS `planPageRail`: the side insets the shell has not consumed are added to the gutter, `PlanPageSafeArea.swift:57–82`).
- `timelineWidthDp` is the `availableWidth` that `DayDrawingKey`/`TimelineGeometry` receive. If it is below the geometry's `minWidth`, 1d's horizontal day scroll applies (it does at 600–~683 dp windows with an 80 dp rail; OQ-6e-4).

**Tests:** `TripPageFrameTest` (JVM):
| window | content | layout | gutter | compact | list | timeline |
|---|---|---|---|---|---|---|
| 280 | 280 | phone | 16 | true | null | 248 |
| 411 | 411 | phone | 16 | true | null | 379 |
| 599 | 599 | phone | 16 | true | null | 567 |
| 600 | 600 | wide | 20 | true | 280 | 264 |
| 639 | 639 | wide | 20 | true | 280 | 303 |
| 640 | 640 | wide | 20 | false | 280 | 304 |
| 669 | 669 | wide | 20 | false | 280 | 333 |
| 841 | 761 (rail 80) | wide | 20 | false | 280 | 425 |
| 899 | 899 | wide | 20 | false | 280 | 563 |
| 900 | 900 | wide | 20 | false | 320 | 524 |
| 914 | 786 (rail 80, right inset 48) | wide | 20 | false | 320 | 410 |

Plus: a content width smaller than the gutters gives `timelineWidthDp == 0.0`, never negative; height never changes the result.

### Task 3: Wide day row and the selected outline

**Commit:** `feat(trip): lay out each day with its list on wide windows`. **Ticks:** none.

**Files** (`feature/trip/…/wide/`)
- `WideDayRow.kt`:
  ```kotlin
  @Composable fun WideDayRow(
      dayId: String, frame: TripPageFrame,
      timeline: @Composable () -> Unit,           // LaneHeadings(null) + DayTimeline, exactly 6b's t:<dayId> item content
      list: (@Composable () -> Unit)?,           // non-null only when frame.layout.isWide
      modifier: Modifier = Modifier,
  )
  ```
  One `Layout(contents = listOf(timeline, list ?: {}))` (or a single `content` that always emits `timeline()` first, then `list?.invoke()`):
  - Measure the timeline child with width `frame.timelineWidthDp` (wide) or the full item width (phone). Its height `H` is the item height. `H` does not depend on width (40 dp/h, lane headings 14 sp), which makes resize restoration exact.
  - Wide: measure the list child with `Constraints.fixed(listWidthPx, H)`, place it at `x = timelineWidthPx + wideColumnGapPx`, `y = 0`. Its size never changes while scrolling, so the item never re-measures for the pin.
  - Item size: full width × `H`.
  - Modifiers: `testTag("trip.day.$dayId")` on this row (move it here from `DayTimeline`'s outer `Box`); the timeline child gets `testTag("trip.wide.timeline.$dayId")` only when wide.
- 6b's timeline item (`item(key = "t:$dayId", contentType = "dayTimeline")`): replace its `Column` with `WideDayRow`. The `h:<dayId>` header item is unchanged and is never beside the list (iOS: `DayHeader` sits above the HStack, `TripDayRow.swift:37–45`). On wide the page emits the timeline items even when the ViewModel's `timeline` flag is false (iOS `timeline: listWidth != nil || timeline`, `TripDayRow.swift:98`); the flag itself is kept, so going back to phone restores list mode.
- On wide the list-mode item set (6c's `r:<dayId>:<blockId>` / `z:<dayId>:<blockId>` rows) is never emitted. The wide list column renders that day's `DayListRows.rows` (6c `ListRows.build`) with 6c's `DayListRow` and `ZoneChangeCard`.
- **Selected outline** (iOS `DayListView.swift:28–33, 77–84`; OQ-T28: list row only, never the tile). Selected id = `(vm.sheet.value as? TripSheet.Step)?.blockId` (6d). In the wide column, `DayListRow` and `ZoneChangeCard` read it through a `selected: () -> Boolean` lambda read **only** in their border/draw modifier (`drawWithContent`/`drawBehind`), so a selection change redraws rows and recomposes nothing. If 6d gave them a `selected: Boolean` parameter, change it to the lambda form and have 6d's phone call site pass `{ <its Boolean> }`; do not change 6d's phone behaviour (OQ-6e-19). Outline: the row's own border switches from `border` 1 dp to `PlanMetrics.emphasisBorder` (2 dp) in `dawn` and drops the optional-priority dash while selected (radius `rowRadius` 22); the zone card gets an extra `cardRadius` (26) `emphasisBorder` `dawn` stroke.
- Top bar: the view toggle (`trip.viewToggle`) is **absent** (not disabled) when `frame.layout.isWide` (iOS `TripPageView.swift:233`: `!isWide && (ready || loading || remoteLoading)`). The trip-actions button stays.

**Edge cases:** a day without a `DayLayout` (render failure) shows no list column, as iOS (`if let listWidth, layout != nil`); a day with zero list rows still gets the column (empty, pinned); `frame.timelineWidthDp` below the geometry minimum scrolls the day horizontally (1d) inside the timeline child only; the list column never scrolls horizontally.

**Tests:** `WideDayRowTest` (Robolectric `@Logic`, `ProfileHost` + `WindowSizeFakeHost`, S3 fixture, `FakeTextMetrics`):
- `wideRowPlacesListBesideTimeline` (fake 669 × 951, zero insets): `trip.wide.list.<dayId>` width = 280 dp ± 0.5 px; list left − timeline right = 16 dp ± 0.5 px; timeline left ≥ 20 dp − 0.5 px; list right ≤ 669 − 20 dp + 0.5 px; `trip.viewToggle` does not exist (port of iOS `captureBoard` l.159–165).
- `breakpointsSwitchTheLayout`: one test case per window width 599 / 600 / 899 / 900 (parameterized): 599 → no `trip.wide.list.*` and `trip.viewToggle` exists; 600 and 899 → list 280 dp; 900 → 320 dp.
- `timelineTapOutlinesTheListRow`: click the centre of a tile (`performTouchInput { click(…) }` on `trip.day.<dayId>` at the tile frame from the fixture) → `vm.sheet.value == TripSheet.Step(id)`, and the list row `onNode(hasTestTag("trip.block.$id") and hasAnyAncestor(hasTestTag("trip.wide.list.$dayId")))` exists. The outline itself is checked by the `PlanWide*` captures (Task 6) and by `wideSelectionRedrawsOneRow` (Task 9), not by semantics.

### Task 4: Pinned list column and the wide compact band

**Commit:** `feat(trip): pin each day's list below the compact header`. **Ticks:** none.

**Files** (`feature/trip/…/wide/`)
- `PinnedDayList.kt`:
  ```kotlin
  @Composable fun PinnedDayList(
      dayId: String, rows: @Composable ColumnScope.() -> Unit,
      pin: WidePinSource,                       // page-level, below
      modifier: Modifier = Modifier,
  )
  ```
  - Structure: an outer `Layout` (fills the `listWidth × H` slot) whose single child is `Box(Modifier.testTag("trip.wide.list.$dayId").clipToBounds()) { Column(Modifier.testTag("trip.wide.list.scroll.$dayId").verticalScroll(rememberScrollState()).padding(top = rowGap 8 dp), verticalArrangement = spacedBy(rowGap 8 dp)) { rows() } }` (iOS `TripDayRow.swift:55–72`, `DayListView` spacing `rowGap`). A plain `Column`, not a nested `LazyColumn` (OQ-6e-6).
  - **Measure** of the outer layout: `contentHeightPx` = the child's `maxIntrinsicHeight(listWidthPx)`, cached in a remembered holder keyed by `(dayId, listWidthPx, rows revision)` (never in snapshot state). Then `val p = pin.position(itemKey, dayHeightPx = constraints.maxHeight, contentHeightPx)` (`itemKey = remember(dayId) { "t:$dayId" }`) and measure the child with `Constraints.fixed(listWidthPx, p.heightPx)`.
  - **Placement:** `child.place(0, p.offsetPx)`. Read the offset in the placement block, so a scroll frame re-places one small node.
  - Because the outer layout always reports `listWidth × H`, its parent (`WideDayRow`) is never re-measured by the pin.
- `WidePinSource.kt` (created once per page, `@Stable`):
  ```kotlin
  @Stable class WidePinSource(private val listState: LazyListState) {
      var stickyTopPx: Int = 0            // plain fields, set by the page during its own layout
      var bottomInsetPx: Int = 0
      fun position(itemKey: String, dayHeightPx: Int, contentHeightPx: Int): WidePinPosition // cached px result; itemKey = "t:<dayId>"
  }
  ```
  `position` reads `listState.layoutInfo` (snapshot state, so the calling layout lambda re-runs when it changes) and computes, with no allocation (index loop, no lambdas, no boxing):
  - `dayTop = item.offset − layoutInfo.viewportStartOffset` for the visible item whose `key == "t:$dayId"` (6b's timeline item, the iOS list column's top: the header item `h:` is above it and not part of the day height; relative to the visible top of the list container; `viewportStartOffset = −beforeContentPadding`). Compare keys without building a string per frame: precompute `"t:$dayId"` once in the caller and pass it in. Not visible → cached `WidePinPosition(0, 0)` (nothing pinned).
  - `viewportHeight = layoutInfo.viewportSize.height − bottomInsetPx` (iOS measures its viewport inside the safe area).
  - The 1b `TripWideListPosition` formula, evaluated in px into the cached result (max height scaled by density): `offset = min(max(0, stickyTop − dayTop), max(0, dayHeight))`, `height = max(0, min(dayHeight − offset, viewportHeight − stickyTop, 700 dp in px, contentHeight))`.
- **Wide compact band** (iOS `TripPageView.swift:351–368`): extend 6b's `CompactHeader(day, alpha, visible, horizontalPadding)` with `wide: TripPageFrame? = null` (null on phone: 6b's and 6c's content unchanged). When non-null it shows, top to bottom with `smallGap` (4 dp) between:
  1. the compact day title row: `<title>` in `planListTitle` (Figtree 600 13/18) + `" · <date>"` in `planListLine` (13/18, `mutedForeground`) as one `AnnotatedString`, `maxLines = 1`, ellipsized (mark it `ellipsizable()`); a flexible spacer (min `smallGap`); the day zone in `planSubtitle` (12/15);
  2. a row: `LaneHeadings` **without** the date gutter (an empty 44 dp gutter, iOS `LaneHeadings(copy:)` with `day == nil`) at `timelineWidthDp`, then a spacer of `wideColumnGap + listWidth`. No "Steps" label (OQ-6e-1).
  - Horizontal padding: the page gutter (20) plus unconsumed side insets; background `card` (opaque, OQ-T13); alpha and accessibility hiding exactly as 6b's phone band (`clamp(1 − headerBottom / 24, 0, 1)`; Reduce Motion: instant switch).
  - **Height (fixed, never measured) [R]:** iOS uses `minHeight: compactHeight` and measures the band (`stickyTop = max(compactHeight, compactHeaderBottom)`, l.137, 367–376); Android replaces the measurement with the equivalent fixed value: `stickyTopPx = max(compactHeaderHeight (36).sp, (planListTitleLineHeight 18 + planLaneLineHeight 14).sp + smallGap (4).dp)` in px; on wide the band gets exactly this height (`Modifier.height`, replacing 6b's `heightIn(min = …)` only when `wide != null`), content centred. At font scale 1.0 this is 36 dp; at the 1.3 cap 46.8 dp (= 36 × 1.3). The page sets `pin.stickyTopPx` to this value and `pin.bottomInsetPx` to the bottom safe inset it pads for, in its own layout pass (iOS `stickyTop = max(compactHeight, compactHeaderBottom)`, `TripPageView.swift:137`).
  - The current-day threshold stays 1d's `36.sp` (iOS `compactHeight`), not `stickyTopPx`.

**Edge cases**
- A short list (content < space) stays at the top of its day while the day's top is below `stickyTop`, then pins; its height is the content height.
- A tall list scrolls inside its viewport; when it cannot scroll further, nested scroll hands the drag to the page (default `verticalScroll` nested-scroll behaviour; do not disable it).
- The list disappears (height 0) once its day's bottom passes `stickyTop` (1b test `listDisappearsWhenItsDayPassesTheHeader`).
- Landscape (`landscapeSideNav`, viewport ≈ 300 dp): height clamps to `viewport − stickyTop`.
- **[verify] no one-frame lag:** the pin reads `layoutInfo`, which `LazyList` updates in the same pass in which it places items. On the emulator (`timelineScrollWide`, Task 10) record a Perfetto trace and a screen recording of 3 flings; the list must not wobble against the band. If it does, apply the OQ-6e-7 fallback and record it.

**Tests:** `PinnedDayListTest` (Robolectric `@Logic`, fake 669 × 951, zero insets, S3, `@Config(qualifiers = "w841dp-h951dp-port-420dpi")` window big enough for the fake):
- `listPinsBelowTheCompactBand` (port of iOS l.144–174): scroll the page with `onNodeWithTag("trip.scroll").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 480.dp.toPx()) }` into the opening day; then `trip.wide.list.<dayId>` top ≥ `trip.compactHeader` bottom − 0.5 px and ≥ the `trip.scroll` top − 0.5 px; its bottom ≤ `trip.day.<dayId>` bottom + 0.5 px; its height > 0 and ≤ 700 dp; the timeline column top moved by exactly the scroll delta (± 0.5 px).
- `listFollowsTheDayEnd`: scroll until the day's bottom is 100 dp below `stickyTop`; the list height ≤ 100 dp + 0.5 px.
- `bandHeightFollowsTheFontScale`: at font scale 1.0 `trip.compactHeader` height = 36 dp ± 0.5 px; at `foldInner` with `fontScale = 2.0f` (capped to 1.3 by `ProfileRule`) = 46.8 dp ± 1 px.
- `positionMath` (JVM, `WidePinSourceTest` with fake `LazyListLayoutInfo`): `viewportStartOffset` −24 shifts `dayTop` by +24; an invisible day gives height 0; `bottomInsetPx` reduces the viewport.

### Task 5: Wide remote states

**Commit:** `feat(trip): add the wide download skeleton and offline card`. **Ticks:** none.

**Files**
- `feature/trip/…/wide/WidePlanSkeleton.kt`, port of `WidePlanSkeleton.swift` (static, no shimmer; OQ-6e-15). A `Column(verticalArrangement = spacedBy(blockIcon 14 dp))`:
  1. centred `Row(spacedBy(rowGap 8))`: an M3 `CircularProgressIndicator` (size `PlanMetrics.icon` 16 dp, stroke `emphasisBorder` 2 dp, color `mutedForeground`) and `Text(<"Downloading this trip’s plan…">)` in `authHint` (Figtree 400 14/20), `mutedForeground`;
  2. a start-aligned `Row(spacedBy(rowGap 8))` of 4 capsules filled `muted`, `wideSkeletonPillWidth` 88 × `touch` 44 dp (a visual, not a target);
  3. a `muted` rounded rect, radius `rowRadius` 22 (iOS; the board draws 18, OQ-6e-18), height `wideSkeletonDayHeight` 64, full width;
  4. a `Row(spacedBy(wideColumnGap 16), verticalAlignment = Top)`:
     - lanes `Row(spacedBy(laneGap 4))` of width `frame.timelineWidthDp`, height `wideSkeletonHeight` 360, `testTag("trip.wide.loading.timeline")`: an empty `gutterWidth` 44 spacer, then 4 equal-weight lanes filled `surface2`, each with a top-aligned `muted` rect of radius `textPlateRadius` 10, horizontal inset `smallGap` 4, heights `[skeletonSummary 120, wideSkeletonLightHeight 90, prioritySampleWidth 40, wideSkeletonTravelHeight 110]`, top offsets `[0, wideSkeletonLightOffset 60, wideSkeletonCaffeineOffset 150, wideSkeletonTravelOffset 200]`;
     - list `Column(spacedBy(rowGap 8))` of width `listWidthDp`, `testTag("trip.wide.loading.list")`: 5 `muted` rects, radius 22, height `tinyTileHeight` 56.
  - Root: `testTag("trip.wide.loading")`, `clearAndSetSemantics { contentDescription = <"Downloading this trip’s plan…"> }` (iOS `.accessibilityElement(children: .ignore)`).
- Page state switch (iOS `TripPageView.swift:150–157`, in 6c's state switch): `RemoteLoading` → `RemoteSummaryCard`, then `WidePlanSkeleton` when `frame.layout.isWide`, else `TripPageSkeleton(downloading = true)`. `RemoteOffline` → `RemoteSummaryCard`, then 6c's offline `TripPageStateCard` with `wide = frame.layout.isWide`. Local `Loading` keeps `TripPageSkeleton(downloading = false)` at every width. The non-ready content uses the same page gutter as Ready (`frame.gutterDp`: 20 wide, 16 phone; iOS `.padding(.horizontal, layout.gutter)`, l.165).
- 6c's `TripPageStateCard` gains `wide: Boolean = false` (iOS `stateCard(…, wide:)`, l.494–517): icon circle `wideRemoteIcon` 52 dp (else `touch` 44); horizontal padding `wideGutter` 20 (else `cardPadding` 16); vertical padding `wideRemotePadding` 28 (else 16); body `widthIn(max = wideRemoteBodyWidth 420.dp)` (else fill); and, for the offline card only (iOS `remoteOffline`, l.491), top padding `sectionGap` 12 when wide. Everything else unchanged (dashed 1 dp `border` stroke with dash `smallGap` 4, radius 26, centred text, 6c's retry button `trip.remote.retry`, the trailing line). The no-plan card always passes `wide = false` (iOS `noPlan` never passes it).

**Copy** (generated by 0c from `PlanUI/Resources/Localizable.xcstrings`; find the resource by its `<!-- plan: … -->` comment; already used by 6c, so nothing is added):

| iOS key | en | nl |
|---|---|---|
| `Downloading this trip’s plan…` | Downloading this trip’s plan… | Het plan van deze reis wordt gedownload… |
| `This plan downloads when you’re online` | This plan downloads when you’re online | Dit plan wordt gedownload zodra je online bent |
| `Plans for active trips and trips in the next 7 days are always on your phone. This one is further away, so its plan opens once you’re connected.` | (same) | Plannen van actieve reizen en reizen in de komende 7 dagen staan altijd op je telefoon. Deze reis ligt verder weg, dus het plan opent zodra je verbinding hebt. |
| `Try again` | Try again | Opnieuw proberen |
| `Your other trips keep working offline.` | Your other trips keep working offline. | Je andere reizen werken gewoon offline. |
| `Plan summary` | Plan summary | Plansamenvatting |

**Tests:** `WideRemoteStatesTest` (Robolectric `@Logic`, fake 669 × 951, zero insets, a fake `TripPageSource` in `remoteLoading` / `remoteOffline` with the mock trip `0199a3c0-0000-7000-8000-000000000002` (AMS → JFK) card and adaptation; port of iOS l.176–188):
- `remoteLoadingUsesTheWideSkeleton`: `trip.wide.loading.list` width 280 dp ± 0.5 px; list left − timeline right = 16 dp ± 0.5 px; `trip.wide.loading.timeline` height = 360 dp ± 0.5 px; the root's content description is the downloading copy (en) and its nl value under nl.
- `remoteOfflineCardSpansTheGutter`: `trip.remote.offline` left = 20 dp ± 0.5 px, right = 669 − 20 dp ± 0.5 px; neither `trip.wide.loading.*` tag exists.
- `phoneRemoteLoadingKeepsThePhoneSkeleton` at fake 411: no `trip.wide.loading`; `trip.skeleton` exists.
- `localLoadingKeepsThePhoneSkeletonOnWide` at fake 669, state `Loading`: `trip.skeleton` exists, no `trip.wide.loading`.

### Task 6: Wide guards and board captures

**Commit:** `test(trip): guard and capture the wide trip page`. **Ticks:** "Wide layout (`TripPageLayout`)".

**Fixture and position.** Use the fixture and frozen clock 6b uses for its `Plan` captures (S1, a ready AMS → HND plan with the now line on the opening day). Positions: *top* = `CF_PLAN_TEST_POSITION=summary` equivalent (the page at offset 0, `TripPageArgs.startAtSummary = true`); *day* = the opening day's `h:` item at the list top; *scrolled* = `scrollToItem(index.timelineIndex(openingDay), 360.dp px)`, i.e. 9 h × 40 dp into the timeline (the board's `scroll-hour` default `sh: 9`, `PlanWideScrolled.dc.html:22`), so the compact band is visible.

**Board profile** `board669`: `DeviceProfiles.foldInner.copy(name = "board669", widthDp = 669, heightDp = 951, insets = SafeInsetsDp(0f, 0f, 0f, 0f))`, the page only, no shell (the boards leave the tab rail out, `canvas.json` `reqWideTrip` "Done 2 Nov").

**`TripPageWideCaptureTest`** (`@Capture`, Robolectric, `BoardCapture.capture`; one test case per name):
- `PlanWide_board669_en_light` (top), `PlanWideDark_board669_en_dark` (day), `PlanWideScrolled_board669_en_light` (scrolled), `PlanWideDayMenu_title_board669_en_light` (scrolled, menu closed: the title and chevron; iOS also captures `PlanWideDayMenu-title` with the native menu closed, `TripPageWideLayoutTests.swift:23–25`);
- `PlanRemoteWide_board669_{en,nl}_{light,dark}` and `PlanRemoteWideOffline_board669_{en,nl}_{light,dark}` (8 files);
- iOS stress cases at the 1.3 cap (`fontScale = 2.0f`): `PlanWide_board669-xxl_nl_light`, `PlanRemoteWide_board669-xxl_nl_dark`, `PlanRemoteWideOffline_board669-xxl_nl_dark`;
- `PlanWide_foldInner_en_light`, `PlanWide_landscapeSideNav_en_light` (day position).

**`TripPageWideGuardTest`** (`@Layout`, Robolectric, parameterized, one case per row; `Guards.assertAll` = text fits, 48 dp targets, interactive inside safe area and not overlapping, headings, ink):
| State | Profiles × lang × theme |
|---|---|
| ready top, ready scrolled, remote loading, remote offline | `foldInner` × {en, nl} × {light, dark} |
| ready day, remote offline | `landscapeSideNav` × en light, and × nl dark |
| ready scrolled, remote offline | `foldInner` with `fontScale = 2.0f` × nl × dark |
| step sheet open (6d), legend sheet open | `foldInner` × en × light |
| no plan, local loading, not found, server (no wide board: fit only) | `foldInner` × en × light |

Also in this class: `noHorizontalPageOverflow` at `board669` and `foldInner` (iOS l.197: content width ≤ viewport): the `trip.day.<dayId>` node has no `HorizontalScrollAxisRange`; and `sheetIsCappedOnWide`: at `foldInner`, after `vm.openStep(<first block id>)`, the `step.sheet` node is ≤ 640 dp wide and horizontally centred ± 1 px (window policy table). Sheet states open through `vm.openStep(id)` / `vm.openLegend()` (6d); their guard heading tags are 6d's.

**`DayMenuWideTest`** (`@Logic`): at fake 841, scrolled: clicking `trip.dayMenu` shows `trip.dayMenu.day.0` and the current day's item is `selected` (6b's M3 `DropdownMenu`, OQ-6e-16); picking day 1 scrolls so `trip.headerItem.<day1>` (6b's outer `h:` item tag; `trip.dayHeader` is its padded title) top = `trip.scroll` top ± 1 px after `waitForIdle()`.

Run `make captures ONLY=PlanWide,PlanRemoteWide` and list the written PNGs in the commit body. Do not record stored references.

### Task 7: Scroll tracker (pure)

**Commit:** `feat(trip): port the trip scroll tracker for in-place resizes`. **Ticks:** none.

**File:** `feature/trip/…/wide/TripScrollTracker.kt` (keep the Swift name; if 6b already has a `TripScrollTracker` for the compact fade, add this API to it instead and keep its file). Main thread only, plain fields, no snapshot state. The CADisplayLink loop, `TripScrollRestoration.observe` and the 60-tick give-up are iOS-only (`04 §0.3`); Android restores in one pass because a day item's height does not depend on width.

```kotlin
class TripScrollTracker {
    data class SizeKey(val windowWidthDp: Int, val density: Float, val layout: TripPageLayout, val timelineMode: Boolean)
    data class ItemSample(val key: Any, val contentType: Any?, val offsetPx: Int)
    data class Restore(val index: Int, val offsetPx: Int)

    data class Captured(val dayId: String, val onHeader: Boolean, val offsetDp: Double)

    /** Called on every page composition with the new key; returns a request only when width, density, layout or the rendered mode changed. */
    fun onSize(
        key: SizeKey, previousItems: List<ItemSample>, viewportStartOffsetPx: Int,
        oldIndex: TripListIndex, newIndex: TripListIndex, thresholdPx: Int, scrollInProgress: Boolean,
    ): Restore?
    val lastCaptured: Captured?   // set when onSize returns a Restore; the page writes it to the ViewModel
    fun cancelRestoration()   // jumps (scroll commands), the view toggle and plan replacement call it (iOS jump/toggle/replacement)
}
```
`TripListIndex` here means 6b's index in timeline mode and 6c's list-mode index (the common `headerIndex`/`currentDayIndex` interface 6c defined; add `timelineIndex` to the interface, unused in list mode) in list mode. `ItemSample` carries the lazy item's `key` (`"h:<id>"`, `"t:<id>"`, 6c's row keys) and `contentType`.

Rules:
1. The first call stores the key and returns null (opening is 6b's `openingScroll()`).
2. A change of only the window height returns null (iOS triggers on viewport width or layout only, `TripScrollTracker.swift:48`).
3. `scrollInProgress == true` returns null and stores the key (iOS cancels while the user tracks or decelerates, l.85–88).
4. Otherwise capture the anchor from `previousItems`, which is the **old** layout, in **old** px (`rel = offsetPx − viewportStartOffsetPx`):
   - current day = 6b's rule (`TripScrollMath.currentDay`: the last `dayHeader`/`dayTimeline` item with `rel ≤ thresholdPx`; in list mode 6c's `currentDayIndex`). None (summary position) → null (iOS `beginResize` needs a day id).
   - Timeline mode: if that day's `t:` item is in `previousItems` with `rel ≤ 0` → `Captured(dayId, onHeader = false, offsetDp = −rel / oldDensity)` (iOS `anchor.offset = −frame.minY`, `TripScrollTracker.swift:26`). Otherwise its `h:` item is the visible one → `Captured(dayId, onHeader = true, offsetDp = −rel / oldDensity)`: negative while the header sits just below the top (down to −36 sp), positive while it is partly scrolled off (OQ-6e-17).
5. Target, same `timelineMode` before and after: `onHeader == false` → `Restore(newIndex.timelineIndex(dayId), round(offsetDp × newDensity))`; `onHeader == true` → `Restore(newIndex.headerIndex(dayId), round(offsetDp × newDensity))`. The `t:` item's height does not depend on width (40 dp/h), so this is exact; the header's height can change with wrapping, which only moves the boundary, never the day. Rendered mode changed (phone list ↔ wide timeline) → `Restore(newIndex.headerIndex(dayId), 0)`: the day's top, as the iOS toggle does (`TripPageView.swift:239–241`; OQ-6e-5). In list mode the offset is not carried over.
6. A day id missing from the new index → null.
7. After a `Restore`, the page calls `vm.select(dayId, if (onHeader) 0.0 else offsetDp)` and `vm.saveAnchor()` (6b: the anchor offset is measured from the timeline item, `offsetInDayPx`).

**Tests:** `TripScrollTrackerTest` (JVM, fake items and fake indexes; values in px at density 2.625 unless stated):
- `firstObservationNeverRestores`; `heightOnlyChangeNeverRestores` (411 × 914 → 411 × 600);
- `widthChangeRestoresTheDayAndOffset`: `t:` item at rel −472 px (≈ 180 dp) at 411 → `Restore(timelineIndex, 472)` at 841;
- `densityChangeConvertsThroughDp`: −472 px at 2.625 → 179.81 dp → `Restore(timelineIndex, 539)` at 3.0 (`round(472 / 2.625 × 3.0)`);
- `dayJustBelowTheTopKeepsANegativeOffset`: `h:` item at rel +20 px with threshold 94 px → `Restore(headerIndex, −20)`;
- `summaryPositionDoesNotRestore` (first header rel > threshold);
- `scrollInProgressSkips`; `cancelRestorationDropsThePendingRequest` (Android restores in one pass, so "pending" is `lastCaptured`: after `cancelRestoration()` it is null, and the next unchanged-key call returns null);
- `modeChangeLandsOnTheDayTop` (timelineMode false → wide) → `Restore(headerIndex, 0)`;
- `unknownDayDoesNotRestore`.

### Task 8: Keep the anchor and the sheets across in-place size changes

**Commit:** `feat(trip): keep the anchor and sheets across folds and resizes`. **Ticks:** "In-place size changes (A8 `configChanges`)".

**Wiring** (the trip page composable):
- Read `LocalWindowSize` and `LocalDensity`; build `TripPageFrame` and `SizeKey`. In a `SideEffect` (runs after composition is applied, before this frame's measure), call `tracker.onSize(...)` with `previousItems` taken from `Snapshot.withoutReadObservation { listState.layoutInfo }` (the last measured layout; no state read, so scrolling never recomposes the page) and the density stored with the previous key. If it returns a `Restore`, call `listState.requestScrollToItem(restore.index, restore.offsetPx)` (applied in the next measure, the same frame). Then write the captured anchor to the ViewModel per Task 7 rule 7 (`vm.select(dayId, offsetDp)`, then `vm.saveAnchor()`, which writes `SavedStateHandle`). Pass `oldIndex` = the index of the previous composition (keep it in a `remember`ed holder) and `newIndex` = the current one (they differ only on a mode change).
- [verify] `requestScrollToItem` accepts a negative `scrollOffset` (LazyList measures backwards for it). If it clamps or throws, clamp `offsetInDayDp` at 0 as iOS does (`max(0, −minY)`), keep `dayJustBelowTheTopKeepsANegativeOffset` as `…ClampsToZero`, and record it (OQ-6e-17).
- Jumps and plan replacements reach the list through 6b's `scrollCommands` collector: call `tracker.cancelRestoration()` there before scrolling. The view toggle (6c's `toggleView()` call site) calls it too.
- The step, legend and trip-actions sheets are composed once at the page root (rule 3). Check that no width branch wraps them; move them if one does.
- `MainActivity`: nothing changes if P9 holds. Do not add `locale`, `fontScale` or `layoutDirection` to `configChanges` (they must recreate so `ConfigurationPolicy` runs again, A8).

**Tests**
- `MainActivityConfigChangesTest` (Robolectric `@Logic`, `:app`; skip if 0a/3a already assert exactly this and say so): `packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0).configChanges` contains `CONFIG_ORIENTATION`, `CONFIG_SCREEN_SIZE`, `CONFIG_SCREEN_LAYOUT`, `CONFIG_SMALLEST_SCREEN_SIZE`, `CONFIG_DENSITY`, `CONFIG_UI_MODE`, `CONFIG_KEYBOARD`, `CONFIG_KEYBOARD_HIDDEN`, `CONFIG_NAVIGATION`, and none of `CONFIG_LOCALE`, `CONFIG_FONT_SCALE`, `CONFIG_LAYOUT_DIRECTION`.
- `TripPageResizeTest` (Robolectric `@Logic`, `WindowSizeFakeHost`, window `@Config(qualifiers = "w841dp-h914dp-port-420dpi")`, the 6b fixture, each method < 5 s), port of iOS `testSizeChangeKeepsDayScrollAndPresentedStep` (`TripPageWideLayoutTests.swift:46–109`):
  - `sizeChangeKeepsDayScrollAndPresentedStep`: start at 411 × 914; wait for `trip.timeline.ready`; `ScrollBy` 180 dp; `waitForIdle()` (the scroll-stop collector calls `saveAnchor()`); record `vm.anchor.value.dayID`, `saved = savedStateHandle.get<Double>("trip.anchor.offset")` (> 0; the test builds the ViewModel with its own `SavedStateHandle`), `dayTop` = `trip.day.<dayId>` top − `trip.scroll` top, and the `trip.scroll` semantics node id; `vm.openStep(<the opening day's first block id>)` and `waitUntilExactlyOneExists(hasTestTag("step.sheet"))`. Set the fake to 841 × 914 and `waitUntil(timeoutMillis = 2_000) { trip.wide.list.<dayId> exists with width 280 dp }`. Assert: anchor day id equal; `|saved offset − before| < 1 dp`; `|dayTop − before| ≤ 1 px`; `vm.sheet.value == TripSheet.Step(blockId)`; `step.sheet` exists; the `trip.scroll` node id equal (same `LazyColumn`). Back to 411 × 914: `trip.wide.list.<dayId>` gone, the same assertions. (iOS asserts the same at 466 × 678 ↔ 669 × 951; plan.md fixes 411 ↔ 841 for Android.)
  - `timelineKeepsItsIdentityAcrossTheBreakpoint`: the `trip.day.<dayId>` semantics node id is equal at 411, 841 and 411; after both changes a click on a tile centre still sets `vm.sheet` to `Step` (the pointer input survived, iOS review 3).
  - `listModeToWideLandsOnTheDayTop`: phone list mode (`vm.toggleView()`), scrolled into a day; → 841: `trip.dayHeader.<dayId>` top = `trip.scroll` top ± 1 px; → 411: list mode again (`trip.viewToggle` content description "Show as timeline").
  - `legendAndTripActionsSurviveResize`: `vm.openLegend()`, 411 → 841 → 411, `legend.priorities` (6d's tag for the trip-page Legend sheet content) still exists; same for `vm.openTripActions()` and `trip.actions` (the fixture trip has a card).
  - `densityChangeKeepsTheDayTop`: at 841, change only the density through `LocalDensity` in the host (2.625 → 3.0); `dayTop` in dp is unchanged ± 1 px.

### Task 9: Wide invalidation guard

**Commit:** `test(trip): extend the invalidation guard to wide windows`. **Ticks:** none.

- `TimelineProbe.Kind` gains `LIST`. `PinnedDayList`'s content root calls `SideEffect { probe?.composed(LIST, dayId) }` first (1d's probe pattern).
- `TimelineInvalidationTest` gains a wide variant (same class, `@Logic`, large60, fake 841 × 914), one method per row:

| Method | Action | Expected deltas for every key composed before |
|---|---|---|
| `wideScrollInvalidatesNoStaticLayerOrRow` | 120 × `ScrollBy(1 dp)` inside day 0, then across one day boundary | STATIC, HEADER, CLOCK, TARGETS, PILLS, LIST compositions 0; STATIC draws 0; COMPACT 0 during the 120 deltas and 1 after the boundary, ANCHOR ≤ 1 during the deltas (6b's phone expectations, unchanged on wide); positive control: ANCHOR ≥ 1 after the boundary. After deltas 1, 40, 80, 120 (four sampled checks, no node loops): `trip.wide.list.<day0>` top = `trip.compactHeader` bottom ± 1 px |
| `wideMinuteTickRedrawsOnlyTheClock` | advance the fake clock 1 min inside a running step | STATIC 0, CLOCK 1 (the now line); LIST compositions 0 unless a row's running state flipped |
| `wideSelectionRedrawsOneRow` | `vm.openStep(<a visible block of day 0>)` | STATIC compositions and draws 0; LIST compositions 0 (the sheet itself composes; it is not probed) |

Keep the 1d and 6b phone rows unchanged and passing. The wide variant uses 6b's `TimelineTestHost` inside `WindowSizeFakeHost`.

### Task 10: Wide scroll benchmark, perf run and baseline profile

**Commit:** `perf(trip): measure wide timeline scrolling`. **Ticks:** none.

- `TimelineBenchmark.timelineScrollWide` (`@Perf`, report-only on the emulator): in setup `device.executeShellCommand("wm size 1840x2208")` (701 × 841 dp at 420 dpi: wide, list 280), launch with `cf.trip-page=large60`, wait for `trip.timeline.ready` and `trip.wide.list.<first day id>`; measured: exactly 15 flings × 3 iterations on `trip.page` as `timelineScroll` does, `FrameTimingMetric`, `MemoryUsageMetric(Mode.Max)`, placeholder count. Teardown `wm size reset` in `@After` **and** in a `finally`. `make perf-timeline` also runs `adb shell wm size reset` after the run (Makefile trap), so a killed run never leaves the emulator resized.
- `benchmark/budgets.json`: a `timelineScrollWide` row: emulator peak RSS anon ≤ 150 MiB (the plan's emulator column for peak memory); everything else `report`. Device rows: the `timelineScroll` values (hard/aim), marked "open: device only" (OQ-A22).
- Run `make perf-timeline`. The phone rows (`timelineFirstFrame`, `timelineScroll`, `timelineLastDay`, 6c's `listScroll`) must pass the emulator gates of plan.md "Where gates apply" (first frame ≤ 800 ms, peak RSS ≤ 150 MiB, builds on main = 0, at most 6 placeholders per iteration, the CPU band). A change of more than ± 25 % against the baseline (the median of the last three `PASS` rows of the same configuration and Page) is **flagged**: list each flagged metric in the commit body and in the "Phase 6e" entry with a one-line cause; it does not fail the phase.
- Record in `docs/perf-timeline.md` "Phase 6e": date, commit, device (`pixel`, API 36, `-gpu host`), refresh rate, font scale 1.0, theme, build type `benchmarkRelease`, raw P50/P90/P99 `frameDurationCpuMs`, jank %, hitch ratio, placeholder count and peak RSS for all rows including `timelineScrollWide`, plus the Perfetto observation of Task 4 ("[verify] no one-frame lag").
- `make baseline-profile`; commit the regenerated profiles.

### Task 11: Fold check on request

**Commit:** `test(trip): script the fold check for the fold AVD`. **Ticks:** "Tests".

- `Scripts/fold-check.sh` (bash, `set -euo pipefail`; never run by `make quick`, `make check` or `make matrix`):
  1. Requires the `fold` AVD (`make avd-create AVD=fold`, Pixel 9 Pro Fold, serial `emulator-5556`) booted through `Scripts/emulator.sh ensure fold`; exits 2 naming the command otherwise.
  2. Installs the debug build and starts `SeedReceiverActivity` with `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_ID=019a56e0-0000-7000-8000-00000000006e`, `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1`, `CF_PLAN_TEST_ROUTE=trip:0199a3c0-0000-7000-8000-000000000001`, `CF_PLAN_TEST_SHEET=step:<a block id of that trip's opening day>` (6d), `CF_PLAN_TEST_FREEZE_CLOCK=1` (the route seeds' frozen clock, 6b Task 13; `cf.freezeAt` belongs to the `cf.trip-page` perf entry, not to routes). Pass them exactly as 6b's seeded smoke test passes them.
  3. Screenshots `build/captures/fold-check/{0-open,1-folded,2-open,3-folded,4-open}.png` around `adb -s emulator-5556 emu fold` / `emu unfold` [verify the console commands; fallback `adb shell cmd device_state state <id>` from `cmd device_state print-states`], waiting with `adb shell uiautomator dump` polling for the sheet node (no `sleep` loops longer than the poll interval; cap 30 s per step).
  4. Fails unless every dump contains the step sheet node and the same `trip.day.<dayId>` node near the top, and `adb logcat -d -s ActivityTaskManager` shows no relaunch of `MainActivity` [verify the log line; fallback: compare `dumpsys activity activities` task and activity record ids before and after].
- `docs/device-matrix.md` "Fold check (on request)": the command, what it proves, the expected screenshots, and "not run in Phase 6e unless requested".

---

## 7. Acceptance criteria

1. `make quick` passes and runs `TripPageFrameTest`, `TripScrollTrackerTest`, `WidePinSourceTest` (JVM; they live in `:feature:trip`'s `src/test`, which `make quick` runs through the Robolectric logic tier's module set [verify that the quick tier includes `:feature:trip` JVM tests; if it covers only `:core:*` JVM tests, add `:feature:trip` there or report it]) and the `@Logic` classes `WindowSizeFakeHostTest`, `WideDayRowTest`, `PinnedDayListTest`, `WideRemoteStatesTest`, `DayMenuWideTest`, `MainActivityConfigChangesTest`, `TripPageResizeTest`, `TimelineInvalidationTest` (phone and wide rows), each visible in `build/test-results/quick/…/timings.json`; JVM < 1 s and Robolectric < 5 s per test; `make quick` < 60 s warm.
2. `make targeted ONLY=":feature:trip:TripPageResizeTest :feature:trip:TimelineInvalidationTest"` passes.
3. `make check` passes with `pixel` booted: Lint (warnings as errors), Konsist (the new `screenWidthDp` rule; `:feature:trip` depends on no feature), `testReleaseUnitTest`, and `TripPageWideGuardTest` (`@Layout`) for every row of Task 6.
4. `make captures ONLY=PlanWide,PlanRemoteWide` writes every PNG named in Task 6 under `build/captures/`; the Opus reviewer compares each with its board, and every difference is either fixed or listed in §9 / the commit body with its board name.
5. `make perf-timeline` passes the emulator gates; any phone metric that moved more than ± 25 % against the last recorded row is flagged with its cause; `timelineScrollWide` is recorded with every metric (a missing metric fails, AGENTS "Tests" 9); `docs/perf-timeline.md` has the "Phase 6e" entry; the baseline profiles are regenerated and committed.
6. Debug runs are StrictMode-clean (`adb logcat -d | grep -c StrictMode` prints 0 after the benchmark's debug smoke launch of `cf.trip-page=large60`).
7. `Scripts/fold-check.sh` exists, is documented in `docs/device-matrix.md`, and was **not** run unless the user asked.
8. Every 6e checkbox in `plan.md` is ticked in its task's commit; no new visible string; no raw color, dp literal, `MaterialTheme.colorScheme` or `LocalConfiguration` size outside allowed files.

---

## 8. Review checklist (Opus reviewer)

**Behaviour parity (iOS at `f391a1fa`)**
- [ ] Breakpoint from the **window** width: 599 phone, 600 wide 280, 899 280, 900 320; `compact` (< 640) from the window too. No height or orientation input.
- [ ] Gutter 20 on wide, 16 on phone, plus unconsumed side insets (check `landscapeSideNav`: the list's right edge ≤ 914 − 48 − 20 dp).
- [ ] No view toggle on wide (absent, not disabled); trip actions present; the `timeline` flag survives a wide interlude.
- [ ] The list pins below the band (`stickyTop` = band height), shrinks at the day end, caps at 700 dp, scrolls inside when tall, hands drags to the page at its bounds.
- [ ] Outline on wide: list row (22, replaces its 1 dp border, no dash) and zone card (26) only, 2 dp `dawn`; never on the tile; 6d's phone list behaviour unchanged (OQ-6e-19).
- [ ] Remote loading on wide = `WidePlanSkeleton` values exactly (88 × 44 pills, 64 bar, 360 lanes with 120/90/40/110 at 0/60/150/200, 5 × 56 rows); local loading = phone skeleton; offline = 52 icon, 20 × 28 padding, 420 body, +12 top.
- [ ] Anchor: same day and offset after 411 → 841 → 411 (± 1 px); list ↔ wide lands on the day top; summary position not restored; height-only changes ignored.
- [ ] Sheets stay open across every size change; the `trip.scroll` node and the `trip.day.<id>` node keep their ids.

**Performance**
- [ ] `WideDayRow` never re-measures while scrolling; `PinnedDayList` reads `layoutInfo` only inside its layout/placement lambdas; offset in placement; no `derivedStateOf`/`State` written from layout.
- [ ] `WidePinSource.position` allocates nothing (no `filter`, `map`, `first { }` on boxed lists, no `Pair`/`IntOffset` boxing in a loop); `contentHeightPx` cached outside snapshot state.
- [ ] The page reads `listState.layoutInfo` in composition only under `Snapshot.withoutReadObservation` (otherwise every scroll frame recomposes the page).
- [ ] `TimelineInvalidationTest` wide rows assert per-key deltas and include the positive control; try once: reading `layoutInfo` in `WideDayRow`'s composition must make `wideScrollInvalidatesNoStaticLayerOrRow` fail.
- [ ] `make perf-timeline` phone rows pass every emulator gate (the CPU band included), and each ± 25 % flag has a cause; `timelineScrollWide` recorded; baseline profiles regenerated.

**Structure and rules**
- [ ] One `LazyColumn`, one `LazyListState`, one sheet host; items `h:<dayId>`/`dayHeader` and `t:<dayId>`/`dayTimeline` at every width; the timeline is the first child of `WideDayRow` at every width; `WidePinSource` keys on `t:<dayId>`.
- [ ] No `onSizeChanged`, `onGloballyPositioned`, per-item `BoxWithConstraints`, `LocalConfiguration.screenWidthDp`.
- [ ] Every dp is a `PlanMetrics` value; colors from `CfTheme`; spinner is an M3 system component.
- [ ] Tags exactly as rule 7; duplicate `trip.block.<id>` handled in tests with `hasAnyAncestor`.
- [ ] `configChanges` unchanged and complete (P9); `locale`, `fontScale`, `layoutDirection` absent.
- [ ] Nothing out of scope: no "Steps" label, no 52 dp/h, no custom day-menu popover, no posture code, no shimmer, no wide variants of boardless states.

**Tests and rules**
- [ ] AGENTS "Tests": no `Thread.sleep`; waits are `waitUntil`/`waitUntilExactlyOneExists`; no `onAllNodes(isRoot())`, `printToLog` or node loops in screen tests; one tier annotation per class; no `Timeout` rule; each case its own test (parameterized).
- [ ] The resize tests change the fake's size in place (no `ActivityScenario.recreate`, no `RuntimeEnvironment.setQualifiers` mid-test).
- [ ] `fold-check.sh` resets nothing on the user's other AVDs, never runs in `make check`, and the perf run always resets `wm size`.

**Typical mistakes to look for:** branching `if (wide) WidePage() else PhonePage()` (destroys the timeline and the sheet); a new item key such as `w:<dayId>` on wide; putting the list beside the `h:` header or measuring `dayTop` from it; reading the container width (after the rail) for the breakpoint; computing `compact` from the timeline column; `onSizeChanged` to learn the list height; reading `layoutInfo` in composition; measuring the band instead of using the fixed `stickyTopPx`; using `stickyTopPx` as the current-day threshold; a nested `LazyColumn` with unbounded height; restoring with `animateScrollToItem` or in a `LaunchedEffect` (one frame late); clamping the anchor to the summary; forgetting density in the anchor conversion; not cancelling restoration on a jump; forgetting `wm size reset`; a "Steps" label copied from the board.

---

## 9. Open questions (proceed on the default; the orchestrator adds them to `plan.md` "Open questions for the user")

| # | Question | Default |
|---|---|---|
| OQ-6e-1 | The board draws "Steps" above the list (PlanWide l.85, 111); iOS has the catalog key but never renders it (no `copy("Steps")` in `Sources/` at the pin). | **No label** (iOS). |
| OQ-6e-2 | Hour scale on wide: board 52 px/h, iOS 40 (`04` OQ-T1). | **40 dp/h** everywhere; it keeps day heights width-independent, which makes the resize restore exact. |
| OQ-6e-3 | Wide compact band: board one 36 px row (weekday/date in the gutter, lanes, "Steps"); iOS a title row over the lanes with a list-width spacer. | **iOS**, height `max(36 sp, 32 sp + 4 dp)`. |
| OQ-6e-4 | The breakpoint is the window width (iOS), but Android's rail is 80 dp: windows 600–~683 dp give a timeline column below its minimum width, so those days scroll sideways. | **Keep the window breakpoint** (parity); the horizontal day scroll of 1d covers it. Revisit if the designer prefers a content-width breakpoint. |
| OQ-6e-5 | A resize that changes the rendered mode (phone list ↔ wide timeline): iOS re-applies the list-mode offset to the new day. | **Land on the day's top** (offset 0), like the iOS view toggle. |
| OQ-6e-6 | Wide list container: `Column` + `verticalScroll` or a nested `LazyColumn` (`04 §9.12` suggests lazy above 12 rows). | **`Column`** (≤ ~15 rows per day; large60 averages 12). Switch only if `timelineScrollWide` shows row composition in the trace. |
| OQ-6e-7 | Pin lag: if the pinned list wobbles one frame behind the page on the emulator. | **Per-item layout read** (this brief). Fallback: measure the `LazyColumn` in a page-level `Layout`, then place a per-visible-day list overlay from the fresh `layoutInfo`, forwarding drags to `listState` through a `NestedScrollConnection`. |
| OQ-6e-8 | A wide benchmark needs a wide window on `pixel`. | **`wm size 1840x2208`** in setup, reset in teardown and in the Makefile; report-only except RSS. |
| OQ-6e-9 | Fold driving on the `fold` AVD: `adb emu fold`/`unfold` or `cmd device_state`. | Console commands first, `device_state` fallback; verify on the first requested run. |
| OQ-6e-10 | Page spacing: board main padding 12/20/40 and gap 16; iOS gap 12 and bottom 16 at every width. | **iOS.** |
| OQ-6e-11 | Board pins the full day header with the list at 144 px; iOS pins only the compact band. | **Whatever 6b ships for the header**; the list pins below the compact band (iOS). |
| OQ-6e-12 | Board title "Amsterdam → New York" with a "My trips" back label; iOS shows IATA codes and the system back. | **iOS** (6b/6c already do). |
| OQ-6e-13 | Dutch "Samenvatting van je plan" (board) vs "Plansamenvatting" (iOS catalog). | **iOS catalog.** |
| OQ-6e-14 | Wide list rows min 56 (board) vs the phone row (iOS uses one row at every width). | **iOS row.** |
| OQ-6e-15 | Skeleton shimmer (board `ChronoFlow.Skeleton`) vs static `muted` shapes (iOS). | **Static** (iOS). |
| OQ-6e-16 | Day menu on wide: the board draws a glass panel titled "Go to day" and labels it the system menu (Compose `DropdownMenu` on Android, `PlanWide.dc.html:180`); iOS shows the native menu titled "Go to a day". | **6b's M3 `DropdownMenu`**, "Go to a day" (OQ-T9). |
| OQ-6e-17 | Negative restore offsets (a header item just below the top) need `requestScrollToItem` with a negative offset. | Use it if supported [verify]; else clamp at 0 as iOS does (`max(0, −minY)`), a shift of at most 36 sp. |
| OQ-6e-18 | Wide skeleton day bar radius: board 18 (`PlanRemoteWide.dc.html:55`), iOS `rowRadius` 22 (`WidePlanSkeleton.swift:23`). | **22** (iOS). |
| OQ-6e-19 | Phone list mode outline: the original brief expected one in 6d, but landed 6d deferred it to 6e; iOS passes no `selectedBlockID` to the phone `DayListView` (`TripDayRow.swift:135`). | **Keep 6d's phone behaviour (no outline)**; add the selected outline only to the wide column (iOS). |

---

## Appendix A: research quoted

`docs/research/04-timeline.md` §3.16:
> Applies at a window width ≥ 600 (width only, either orientation; user decision 2026-11-11). The list is 280 wide, or 320 from 900. The column gap is 16 and the gutter 20. There is no view toggle. The timeline on the left fills the space; the list on the right sits beside its own day and scrolls independently when tall (`TripWideListPosition`): `offset = min(max(0, stickyTop − dayTop), dayHeight)`, `height = min(dayHeight − offset, viewport − stickyTop, 700, contentHeight)`. A tap opens the step sheet and outlines the selected list row in dawn. iOS restores the scroll position after a resize using consecutive stable display frames, and gives up after 60 ticks. Widths tested on iOS: 599/600/899/900/669/951.

§4.8:
> Folding must keep the selected day, the scroll anchor and any open sheet (`I/plan.md` trips gaps). Replacement keeps the day by `labelDate` (15c).

§9.8 (row): "Wide list column offset and height | per frame inside a day | `Modifier.layout` (placement offset; height clamps only near the day end) | placement (+ measure of one item near the end)".

§9.12:
> Breakpoints (window width, not layout constraints; iOS uses window width) … < 600 → phone; ≥ 600 → wide with a 280 list; ≥ 900 → 320 list; `compact` (icon-only tiles) = < 640. … No view toggle. A tap on either side opens the step sheet and outlines the list row. Fold / unfold / rotate: handle `screenSize|smallestScreenSize|screenLayout|orientation` config changes in place … Keep the anchor `(dayId, offsetInDay)` in the ViewModel and `SavedStateHandle` (process death). On a width change, every `RenderKey` changes. Rebuild off-main, and restore with `listState.requestScrollToItem(index(t:dayId), offsetInDayPx)`. Timeline height does not depend on width, so this restores exactly in one pass. iOS needs a 60-tick retry loop. The open step sheet survives because the selection lives in the ViewModel. No orientation lock. A landscape phone ≥ 600 dp wide gets the wide layout; the list height clamps to the viewport.

`docs/research/14-gaps.md` §3.3:
> Size and orientation (folds, rotation, split screen): handled in place. Recreating would drop frames on the timeline and risk the anchor. … Not listed: `locale` and `fontScale`. These recreate the activity, so `attachBaseContext` reapplies the configuration policy.

§3.4 rows: "Trip page | Anchor kept and re-applied after re-layout | Anchor from `SavedStateHandle`; sheet route reopens | Cold launch → Today"; "Step sheet / legend / trip actions | Stays open | Reopens from the route key | Gone".

§4.2: "No posture-specific UI. … A fold behaves like a width change. The only fold requirements are the anchor and sheet survival from §3.3."

## Appendix B: iOS references quoted (at `f391a1fa`)

`TripPageLayout.swift:9–29`:
```swift
// Width only: the unfolded Duo held flat is landscape (951 pt) and is the widest case, not a phone.
public static func resolve(width: Double) -> Self {
    guard width >= PlanMetrics.wideBreakpoint else { return Self(listWidth: nil) }
    return Self(listWidth: width >= PlanMetrics.wideLargeBreakpoint ? PlanMetrics.wideLargeListWidth : PlanMetrics.wideListWidth)
}
init(dayTop:dayHeight:viewportHeight:stickyTop:contentHeight:) {
    offset = min(max(0, stickyTop - dayTop), max(0, dayHeight))
    height = max(0, min(dayHeight - offset, viewportHeight - stickyTop, PlanMetrics.wideListMaxHeight, contentHeight))
}
```

`TripWindowSizeHost.swift:36–41`: `// The native tab rail consumes container width; the breakpoint is in window points.` `guard let size = window?.bounds.size, size != reportedSize …; if reportedSize != nil { willChange() }` (freezes the anchor before the width update).

`TripDayRow.swift:44–79` (abridged):
```swift
// Keep the timeline's gesture identity when the window crosses the wide breakpoint.
HStack(alignment: .top, spacing: PlanMetrics.wideColumnGap) {
    VStack(spacing: 0) { clockContent }.frame(maxWidth: .infinity)
        .planContentBounds(identifier: listWidth == nil ? nil : "trip.wide.timeline.\(day.id)")
    if let listWidth, layout != nil {
        GeometryReader { geometry in
            let position = TripWideListPosition(dayTop: listTop, dayHeight: geometry.size.height,
                viewportHeight: viewportHeight, stickyTop: stickyTop, contentHeight: listHeight ?? .infinity)
            ScrollView { DayListView(…, selectedBlockID: selectedBlockID, …).padding(.top, PlanMetrics.rowGap) }
                .scrollBounceBehavior(.basedOnSize)
                .accessibilityIdentifier("trip.wide.list.scroll.\(day.id)")
                .frame(height: position.height)
                .planContentBounds(identifier: "trip.wide.list.\(day.id)")
                .offset(y: position.offset)
        }.frame(width: listWidth)
    }
}
```
`clockContent` passes `timeline: listWidth != nil || timeline` (l.98).

`TripPageView.swift` (abridged): `let layout = TripPageLayout.resolve(width: size.width)` with `size = windowSize ?? containerSize` (l.116–117); `compact: size.width < PlanMetrics.timelineLabelBreakpoint` (l.134); `stickyTop: max(compactHeight, compactHeaderBottom)` (l.137); `.padding(.horizontal, layout.gutter).padding(.bottom, PlanMetrics.gutter).planPageRail()` (l.165); `remoteOffline(wide: layout.isWide)` / `WidePlanSkeleton(copy:listWidth:)` / `skeleton` (l.150–157); toggle only `if !isWide && (…)` (l.233); wide compact header (l.354–360):
```swift
VStack(spacing: PlanMetrics.smallGap) {
    compactDayTitle(day)
    HStack(spacing: PlanMetrics.wideColumnGap) {
        LaneHeadings(copy: copy)
        Color.clear.frame(width: layout.listWidth.map { CGFloat($0) }, height: 0)
    }
}
```
`stateCard` wide values (l.498–510): icon `wide ? wideRemoteIcon : touch`; body `maxWidth: wide ? wideRemoteBodyWidth : .infinity`; padding horizontal `wide ? wideGutter : cardPadding`, vertical `wide ? wideRemotePadding : cardPadding`; dashed `RoundedRectangle(cornerRadius: cardRadius).strokeBorder(border, lineWidth: PlanMetrics.border, dash: [smallGap])`; `remoteOffline` adds `.padding(.top, wide ? sectionGap : 0)`.

`TripScrollTracker.swift` (Android keeps the API shape, not the loop): `track(_:)` ignored while restoring (l.22–28); `beginResize()` freezes the anchor (l.30–36); `resize(…)` begins a restoration when the viewport width or layout changed (l.47–63); restoration cancels while the user tracks or decelerates (l.85–88) and after 60 ticks (l.76–80); `updateChrome` alpha `reduceMotion ? (remaining <= 0 ? 1 : 0) : clamp(1 − remaining / compactFadeDistance)` (l.126–134).

`plan.md:1766` (15e decision): "From a 600 pt window width, in either orientation (user, 2026-11-11): timeline and list side by side, a 280 pt list column growing to 320 pt from 900 pt, no view button. … Compose the existing timeline and list under each shared day header; keep the list beside its day while scrolling, with independent scrolling when tall, and outline the selected step in dawn. Retain the native day menu and step sheet. Downloading uses the wide lane/list skeleton; offline retains the known summary and full-width retry card. Dimensions come from generated tokens and the live window/container, with unconsumed safe-area rail padding."

`plan.md:1783` (review 3): "The phone/wide conditional also replaced `DayClockContent` with a new HStack subtree, destroying `DayTimelineView` and its tap gesture while the independently hosted step sheet remained presented. That hierarchy now stays stable."

`plan.md:1775` (orchestrator Duo fold check): "closed → open → closed → open … The step sheet persists throughout; open (951×669) is wide. … the resize restore's viewport guard compared the safe-area viewport with full scroll-view bounds and never ran on iOS, and its target ignored the top inset."
