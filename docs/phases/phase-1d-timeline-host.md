# Phase 1d: Timeline host page, clock and invalidation probe

- **Branch:** `feat/phase-1d-timeline-host`, from the stack top once 1c and 0e are in the stack.
- **Depends on:** 1c, 0e (and through them 0a–0d, 1a, 1b). **Wave:** 6. **Milestone:** M1 (with 1a–1e).
- **Plan entry:** `plan.md` "Phase 1d: Timeline host page, clock and invalidation probe" (l.1132–1177). Decisions **A3** (modules), **A8** (config changes), **A12** (timeline architecture), **A19** (tests), **A21** (trace sections). Performance budget row "Invalidation (static/clock draws)". OQ-A30 (the guard runs in `make quick`).
- **Tests:** every test you add follows AGENTS.md "Tests" (cheapest layer, budgets, exact queries, no sleeps, hang caps). Read it before Task 1.
- **API endpoints:** none. All data comes from fixtures.
- **Re-verified:** 2026-11-16 (adversarial check, no subagents): iOS line references at `f391a1fa`, the `Plan`/`PlanScrolled` boards, the PlanUI catalog keys and their 0c resource names, and the names this brief consumes from the 0a, 0b, 0c, 0e, 1a, 1b, 1c and 1e briefs.
- **Who consumes this phase:** 1e benchmarks this host as it is (`docs/phases/phase-1e-timeline-perf.md` §2, preconditions P4–P7). 6b replaces the host's chrome and data source but keeps `DayTimeline`, `NowLine`, the clock, the probe and the invalidation test.

Tags: **[F]** fact verified in a source (path and line); **[R]** a rule this brief sets for Android. Paths are abbreviated as in plan.md: `feature/trip/…/X.kt` = `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/X.kt`; `core/plan/…/X.kt` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/X.kt`.

---

## 1. Goal

Host the 1c day drawings in a real Compose page, without UI polish, so that 1e can measure design B:

1. `TimelinePage`: a `LazyColumn` with one keyed item per day, a plain sticky day header, a minimal M3 day menu and a plain end item.
2. `DayTimeline`: its drawing is built off the main thread behind a placeholder and replayed in `drawWithCache` inside its own graphics layer, with one `pointerInput` per day, the isolated `NowLine` and an invisible semantics overlay (`TimelineTargets`).
3. `SystemPlanClock`: a minute ticker aligned to the wall minute plus time, zone and date broadcasts, only while the app is started.
4. `TimelineProbe` and `TimelineInvalidationTest` (Robolectric, in `make quick`): scroll 0/0, minute tick 0/1, day boundary 0/2, a check-off redraws one day only.
5. A perf entry: `MainActivity` with extra `cf.trip-page=S1…S6|large30|large60` opens `TimelinePage` from fixtures in debug and perf variants, and emits the `cf.timeline.*` trace sections.

The phase is done when the invalidation table holds, every fixture opens on `pixel`, a Perfetto trace shows `cf.timeline.build` on worker threads only, and the run is StrictMode-clean.

---

## 2. Before you start: check what 1a, 1b, 1c and 0e left

Do not rebuild earlier phases. Find each name below; if a name differs, use the existing one and say so in your commit body. If an item is missing (not just renamed), stop and report it to the orchestrator.

| # | Expected (phase) | Where | Used for |
|---|---|---|---|
| P1 | `interface PlanClock { fun values(): Flow<Instant> }`, `class FixedPlanClock(now)` (1a) | `core/plan/…/PlanClock.kt`, `FixedPlanClock.kt` | clock seam |
| P2 | `class FakePlanClock(start: Instant)` with `advance(to: Instant)` and `now` (1a) | `core/testing/…/FakePlanClock.kt` | tests |
| P3 | `nowLineOffset(day: DayView, now: Instant): Double?` (start ≤ now < end, else null), `CopyLineFormatter.nowLabel(layout, now, options)`, `layoutPlan`, `DayLayout(day, height, minWidth, placed, markers, changes, dividers, ticks)`, `TimelineGeometry(layout, availableWidth)` (1b) | `core/plan/…/Timeline.kt`, `TimelineLabels.kt`, `TimelineGeometry.kt` | now line, sizes |
| P4 | `PlanPreparer`, `PreparedPlan` (`revision = inputHash|detailVersion`, `days`), `PreparedDay` (`id`, `title`, `date`, `blocks`, `dividerLabels`, `ticks`), `openingDay(...)`, `TripPageLayout.resolve(width)` (`gutter`), `LargeTimelineFixtures` (1b) | `core/plan/…/PreparedPlan.kt`, `core/plan/…/trippage/`, `core/testing/…/LargeTimelineFixtures.kt` | page model |
| P5 | `DayDrawingKey(dayId, revision: String, availableWidthPx, density, textScale, dark, compact, language)`, `DayDrawing` (`key`, `widthPx`, `heightPx`, `targets`, `hits`, `estimatedBytes`), `DayDrawingBuilder.build(day, layout, key, palette, metrics)`, `DayDrawingCache` (`get`, `peek(key, day)`, `put`, `getOrBuild`, `buildCount`), `HitIndex.resolve(x, y): String?` (px), `TimelineTarget(kind, blockId, label, left, top, right, bottom /*px*/, traversalIndex, testTag)` (1c) | `core/plan/…/drawing/` (package `net.chronoflow.core.plan.drawing`) | drawing |
| P6 | `TimelineTypefaces.load(context)` (suspend, IO), `TextPaintMetrics(typefaces)` (one per build job), `timelineTextScale(density)`, `TimelineResources(density, dark, palette)` + `TimelineResources.Cache`, `fun DrawScope.replay(drawing, resources)` (1c) | `feature/trip/…/timeline/` (package `net.chronoflow.feature.trip.timeline`) | replay |
| P7 | `FakeTextMetrics` (1c), S1–S6 JSON fixtures with checksum manifest (0d) | `core/testing/…`, `core/testing/src/main/resources/fixtures/` | tests, perf entry |
| P8 | Tier annotations `@Logic` / `@Layout` (package `net.chronoflow.core.testing.tiers`; `make quick` = `-Pcf.tier=quick` = plain JVM + Robolectric `@Logic`, `make check` adds `@Layout`) and `TierRegistryTest`; `ProfileRule`, `ProfileHost`, `Guards` (`assertAll(rule, profile, headingTags, exemptTags)`, `assertNoInteractiveOverlap(rule, exemptTags)`) in `:core:testing-compose`; `profiles.json` (`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`); `LocalSafeInsets` in `:core:ui`; root `testTagsAsResourceId = true`; `SeedReceiverActivity` + `TestSeed`/`TestSeeds` in `app/src/debug` (`freezeAtMs` = `cf.freezeAt` as launch ms 0…60000, `freezeAtRaw` = the verbatim extra, OQ-0e-12); `ReleaseReadinessTest` in `app/src/testRelease`; perf variants `benchmarkRelease`/`nonMinifiedRelease` wired to `app/src/perf/kotlin` through `addPerfSources` with the marker `PerfEntry.kt` (package `net.chronoflow.app.perf`) and `BuildConfig.PERF_ENTRY` (0e T15, recorded in `docs/tooling.md`, OQ-A28). 0e does **not** compile `src/perf` into `debug` (OQ-0e-19): Task 8 adds that | `core/testing/`, `core/testing-compose/`, `core/ui/`, `app/` | harness |
| P11 | `class AppGraph` (0a: no members yet, created as `AppGraph()` in `ChronoFlowApp.onCreate`); `MockTripTemplates` and `LargeTimelineFixtures.fixture(named)` / `detailBytes(named)` for `S1`…`S7`, `large30`, `large60` (1b Task 2 and 10); `PlanPreparer.nowTexts` and stable day identity (unchanged days keep their `PreparedDay` instance, 1b Task 8) | `app/…`, `core/testing/…`, `core/plan/…` | graph, fixtures |
| P9 | Generated tokens (0b): `CfLightColors`/`CfDarkColors` (`background`, `card`, `foreground`, `mutedForeground`, `now`, `primaryForeground`, `popover`, `surface2`) in `core/designsystem/…/generated/CfColors.generated.kt`; `PlanTypeMetrics` (an `object` in `core/designsystem/…/generated/CfTypeScale.generated.kt`; each style is a `CfTypeMetrics` plus a `<name>Face: CfFace`): `planNow` 10/16 Figtree600, `planListTitle` 13/18 Figtree600, `planSubtitle` 12/15 Figtree400 (iOS `PlanTypeMetrics.generated.swift:17–26`); `PlanMetrics` (`object`, Double dp, package `net.chronoflow.core.plan.generated`, file `core/plan/…/generated/PlanMetrics.generated.kt`, 0b OQ-0b-1): `compactHeaderHeight` 36, `emphasisBorder` 2, `tilePadding` 6, `planNowLineHeight` 16, `gutter` 16, `sectionGap` 12, `timelineLabelBreakpoint` 640, `touch` **44** (iOS value, kept by 0b's golden) and the Android overlay `minTouchTarget` **48** (1b Task 1). Wherever this brief needs the 48 dp minimum, read `PlanMetrics.minTouchTarget`, never `touch` (1b OQ-1b-11) | `core/designsystem/…/generated/`, `core/plan/…/generated/` | styling |
| P10 | Generated strings in `core/ui/src/main/res/values{,-nl}/strings_ios.xml` (0c) | `:core:ui` | copy |

**Phase 2 is not a dependency of 1d.** If `ChronoFlowTheme`, `LocalCfColors`, `LocalCfType` and `LocalReduceMotion` exist at your base commit, use them. Otherwise add one private helper in `TimelinePage.kt` that picks `CfLightColors`/`CfDarkColors` with `isSystemInDarkTheme()`, builds the three `TextStyle`s from `PlanTypeMetrics.planNow`/`planListTitle`/`planSubtitle` (size and line height in sp, face from `<name>Face`) and the 0b Figtree fonts, and reads Reduce Motion as `!ValueAnimator.areAnimatorsEnabled()` once per page composition. Do not build any Phase 2 piece here.

---

## 3. Inputs to read

**This repo**
- `plan.md`: l.1132–1177 (this phase), A12 (l.99), A19, A21, "Performance budget" (l.491–538, row "Invalidation"), "Android spec: seeds and test hooks" (l.465–471), "Android spec: build" (perf variants, l.318–320), OQ-A28, OQ-A30.
- `AGENTS.md` "Performance", "Tests", "Traps" (splash keep condition; measured sizes stay in layout; rethrow `CancellationException`; Robolectric needs no `Timeout` rule).
- `docs/research/04-timeline.md` §4.1–§4.3, §5, §6, §9.5–§9.9, §9.15, §10.2 (key parts quoted in Appendix A).
- `docs/research/13-testing-tooling.md` §4.5 (the four guarantee layers).
- `docs/phases/phase-1c-timeline-drawing.md` (names you call: Tasks 1, 5, 6 and the Android-side task), `docs/phases/phase-1b-timeline-layout.md` Tasks 8 (`PlanPreparer`, stable day identity, `nowTexts`), 9 (`openingDay`, `TripPageLayout`) and 10 (`LargeTimelineFixtures`), `docs/phases/phase-0e-test-harness.md` §5 (tiers), T1 (`@Logic`), T6–T8 (`LocalSafeInsets`, `ProfileRule`, `Guards`) and T15 (perf sources), and `docs/phases/phase-1e-timeline-perf.md` §2 and Task 1 (what 1e expects from you).
- `docs/tooling.md` (0a/0e decisions: sandbox limits, perf variant wiring, tier annotations).

**Boards** (`_design/canvas/`, reference only, no fidelity work in this phase)
- `Plan.dc.html`: hour scale `(h − sh) * 40` px (40 dp/h); grid `44px repeat(4, minmax(0,1fr))`, gap 4; lanes `background: var(--surface-2)`; now line `height: 2px; background: var(--now)` with pill `padding: 0 6px; border-radius: 9999px; background: var(--now); color: var(--primary-foreground); font-size: 10px; line-height: 16px; font-weight: 600; tabular-nums; left: 0; transform: translateY(-50%)`; day header `position: sticky`; compact line `height: 36px`.
- `PlanScrolled.dc.html`: a wrapper, `<dc-import name="Plan" scrolled="{{yes}}" scroll-hour="{{sh}}">` with `sh = 9`. It shows the scrolled state (compact day line, subtitle = current day). 1d builds only what feeds it: the current-day state. The designed compact line and subtitle are 6b's.
- There is no board for the plain 1d header, menu or end item; they are throwaway host chrome, replaced in 6b by `Plan`, `PlanScrolled`, `PlanDayMenu`, `PlanEnd`.

**iOS** (read at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; key parts quoted in Appendix B)
- `plan.md:1705` "15d: Timeline drawing" and `plan.md:1713` "15 performance extension (user request, worktree B)"; also `plan.md:1730` "15 performance enforcement" (the invalidation guard).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/DayTimelineView.swift` (230 lines: horizontal overflow l.48–52, render task and `onDisappear { drawing = nil }` l.54–72, placeholder = plain `surface2` l.83–85, now line l.87–98, tap order l.102–113, accessibility children l.115–158).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripDayRow.swift` (`trip.day.<id>` l.93; `DayClockState` l.104–112 writes `now` and `text` only when changed; `DayClockContent` l.114–138: the isolated clock child).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageView.swift` (`trip.scroll` l.68 (error page) and l.170 (plan page); current day = last day whose row `minY <= compactHeight` l.187–191, `headerScrolled` = anchor row `minY < 0` l.192–194, `compactHeight` is `@ScaledMetric` 36 l.21; `compact = size.width < PlanMetrics.timelineLabelBreakpoint` l.134; day menu l.266–283; `codeTitle` l.287–291; title menu l.297–328 with its accessibility label/value l.318–324 and `dayMenuLabel` l.334–342; `jump` l.394–398).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripPageModel.swift` (`select(dayID:offset:)` l.159–164: the anchor changes only when the day changes).
- `Packages/ChronoFlowKit/Sources/PlanUI/TripPage/TripScrollTracker.swift` (`updateChrome` l.126–134 writes the compact line's alpha straight to a UIKit view, never to observable state: the reason scrolling invalidates nothing. Android analogue in 6b: read scroll inside `graphicsLayer { alpha = … }`).
- `Packages/ChronoFlowKit/Sources/PlanUI/Models/TripPlanModel.swift` (`dayClock(for:)` l.44–49, `updateDayClocks` l.51–56, clock task l.123–128).
- `Packages/ChronoFlowKit/Sources/PlanUI/Models/SystemPlanClock.swift` (whole file, 65 lines).
- `Packages/ChronoFlowKit/Sources/PlanCore/PlanClock.swift`, `FixedPlanClock.swift`.
- `Packages/ChronoFlowKit/Tests/PlanUITests/TimelineInvalidationTests.swift` (the test you port).
- `Docs/perf-timeline.md` ("Four guarantee layers", item 3).

---

## 4. Out of scope (do not build)

- The designed `DayHeader`, compact header line and its fade, lane headings, summary card, `DualClocks`, day pills, previous/next, the top-bar view toggle and trip actions, the designed day menu (`PlanDayMenu`) and end card (`PlanEnd`): **6b**.
- List mode and its rows, every non-ready state (loading skeleton, no plan, remote, errors), the render-failure list fallback: **6c**.
- Step sheet, check-off writes through the outbox, legend, TalkBack custom actions, focus after a jump: **6d / 6b**.
- Wide and foldable layout, anchor restoration on resize, `SavedStateHandle`: **6e / 6b**.
- Macrobenchmarks, baseline profile, JankStats, `trip.timeline.presented`, `budgets.json` rows: **1e**.
- `cf.trip-page=<uuid>` and `cf.timeline-fixtures` (they need the mock data source of 4c).
- Any new visible copy. Every string below already exists in the iOS catalogs.

---

## 5. Rules fixed by this brief [R]

1. **Item structure.** Per day: `stickyHeader(key = "h:<dayId>", contentType = "dayHeader")` then `item(key = <dayId>, contentType = "day")`. After the last day: `item(key = "end", contentType = "end")`. No other items. (Plan: "one item per day (`key = dayId`, `contentType = "day"`)" plus "a plain sticky day header"; see OQ-1d-1.)
2. **Sizes come from constraints.** One `BoxWithConstraints` at the page root (never per item) gives the content width. No `onSizeChanged`, `onGloballyPositioned` or `BoxWithConstraints` inside an item. A day's size is `TimelineGeometry(layout, availableWidthDp).width × layout.height` dp.
3. **The clock reaches one scope.** Only `NowLine` reads a `DayClock`. No composable outside `NowLine` reads clock state, and no inline lambda (`Box { }`, `Column { }` content) of `DayTimeline` reads it.
4. **Scroll state is read in layout or draw, or through `derivedStateOf` that changes only at boundaries.** Day items read no scroll state.
5. **No per-frame allocation in draw lambdas.** No `Paint()`, `Path()`, `RectF()`, string building, boxing, iterators or lambda creation inside `onDrawBehind`. No `TextMeasurer`, no `saveLayer`, no blur.
6. **Builds never run on main.** One `Dispatchers.Default.limitedParallelism(2)` instance, created once per ViewModel (calling `limitedParallelism` again creates another view with its own limit). Tests inject their dispatcher.
7. **Trace names are exact:** `cf.timeline.firstFrame` (async), `cf.timeline.build` (sync, on the worker), `cf.timeline.replay` (sync, in draw), `cf.timeline.placeholder` (a sync **section** around each placeholder draw, not a counter; 1e counts slices).
8. **Test tags are the iOS ids:** `trip.scroll` (the `LazyColumn`, iOS `TripPageView.swift:170`), `trip.page` (the page content root that holds the list; plan.md and 1e fling it, OQ-1d-13), `trip.timeline.preparing` / `trip.timeline.ready` (each day's target layer), `trip.block.<blockId>`, `trip.dayMenu` (menu anchor), `trip.dayMenu.day.<n>` (0-based), `trip.plan.end`, `trip.day.<dayId>` (day item root, iOS `TripDayRow.swift:93`).
9. **The probe is null in production.** `LocalTimelineProbe` defaults to `null`; the counting implementation lives in the unit-test source set only (OQ-1d-12).
10. **Tier annotation.** Every Robolectric class in this phase carries exactly one tier annotation: `@Logic` (runs in `make quick`) unless stated `@Layout`. Import `net.chronoflow.core.testing.tiers.Logic`. No JUnit `Timeout` rule (AGENTS "Traps").
11. **Stable parameters.** `DayTimeline`, `NowLine`, `TimelineTargets` and `DayHeaderLine` must be skippable with stable parameters. Add to the root `stability_config.conf` (0a; 1c added `DayDrawing`): `net.chronoflow.core.plan.PreparedDay`, `net.chronoflow.core.plan.PreparedBlock`, `net.chronoflow.core.plan.DayLayout`, `net.chronoflow.core.plan.drawing.DayDrawingKey`, `net.chronoflow.core.plan.drawing.TimelineTarget` (use the FQNs 1b/1c actually used). Mark `TimelineDrawingRepository`, `DayClock` and (if 1c did not) `TimelineResources` `@Stable`. This is safe only because 1b keeps unchanged `PreparedDay` instances and Kotlin data-class `equals` returns early on `===`.
12. **Tracing API.** Use `androidx.tracing` (`trace("…") { }`, `Trace.beginSection`/`endSection`, `Trace.beginAsyncSection(name, cookie)`/`endAsyncSection`) as 1e does. If the catalog's tracing artifact lacks one of these calls, use `android.os.Trace` (all exist at minSdk 29). Names are constant strings.

---

## 6. Tasks

Each task is one atomic Conventional Commit with no trailer. Tick the named plan.md checkbox (Phase 1d "Tasks") in the same commit; a task with "Ticks: none" ticks nothing. Run only what you touched (`make targeted ONLY=…`); run `make quick` once before handing back. If your sandbox cannot run Gradle, `adb` or the emulator, write the code and report the exact command and its exact output; the orchestrator runs it.

### Task 1: System plan clock

**Commit:** `feat(data): add the system plan clock`. **Ticks:** none (the plan.md box "SystemPlanClock" also covers "`cf.freezeAt` freezes it in debug and perf builds", which Task 8 delivers; Task 8 ticks it).

**Files**
- `core/data/src/main/kotlin/net/chronoflow/core/data/clock/MinuteClockPolicy.kt` (pure, no Android types):
  - `fun delayToNextMinuteMs(nowMs: Long): Long = 60_000 − floorMod(nowMs, 60_000) + 15`. The +15 ms lands after the boundary, never a minute late (`04 §9.15`).
  - `enum class ClockEvent { START, TIMER, TIME_TICK, TIME_CHANGED, TIMEZONE_CHANGED, DATE_CHANGED }`.
  - `fun shouldEmit(event: ClockEvent, lastEmittedMs: Long?, nowMs: Long): Boolean`: the two minute sources, `TIME_TICK` and `TIMER`, emit only when `lastEmittedMs == null` or `floorDiv(nowMs, 60_000) != floorDiv(lastEmittedMs, 60_000)`; `START`, `TIME_CHANGED`, `TIMEZONE_CHANGED` and `DATE_CHANGED` always emit. (Every refresh re-arms the timer, so a `TIME_TICK` just after the boundary already moves the timer to the next minute. The `TIMER` gate is defensive: if the wall clock is slewed without a `TIME_CHANGED`, the timer can fire a few ms before the boundary; it then emits nothing and re-arms a few ms later.)
- `core/data/…/clock/SystemPlanClock.kt`: `class SystemPlanClock(context: Context, private val wallMs: () -> Long = System::currentTimeMillis) : PlanClock`.
  - `values()` is a `callbackFlow` that, on collection:
    1. registers one receiver on `context.applicationContext` for `ACTION_TIME_TICK`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` and `ACTION_DATE_CHANGED`, with `ContextCompat.registerReceiver(…, ContextCompat.RECEIVER_NOT_EXPORTED)`;
    2. runs a `refresh(event)` driver, the port of iOS `MinuteClockDriver.refresh()`: read `now = wallMs()`; if `shouldEmit`, `trySend(Instant.ofEpochMilli(now))`; cancel the pending timer and re-arm it with `delay(delayToNextMinuteMs(now))`, which then calls `refresh(TIMER)`;
    3. calls `refresh(START)` immediately (emit on collection; this is iOS `willEnterForeground`);
    4. in `awaitClose`, cancels the timer and unregisters the receiver.
  - The receiver's `onReceive` maps the action to its `ClockEvent` and calls `refresh`. It does nothing else.
  - Apply `.conflate()` (iOS `bufferingNewest(1)`).
  - **"Registered only while the app is started"** is the collector's job: the page collects inside `repeatOnLifecycle(STARTED)` (Task 6). The clock itself registers on collect and unregisters on cancel. No `ProcessLifecycleOwner`: the clock is a screen concern, and the activity lifecycle is the narrower scope (OQ-1d-7).
- `AppGraph`: add `val planClock: PlanClock by lazy { SystemPlanClock(app) }`. If `AppGraph` still has no constructor parameter (0a: `class AppGraph`, `graph = AppGraph()`), change it to `class AppGraph(private val app: Application)` and `graph = AppGraph(this)` in `ChronoFlowApp.onCreate`, in this commit. Constructing it stays free: no I/O, no receiver, nothing eager (the receiver registers only when the flow is collected).
- `:core:data` gets `implementation(project(":core:plan"))` if it does not have it; `:app` gets `implementation(project(":core:data"))` if it does not have it.
- Test dependencies of `:core:data`, only if its convention plugin does not already add them: `testImplementation` of `project(":core:testing")` (tier annotations), Robolectric, `androidx.test.ext:junit`, `kotlinx-coroutines-test` and Turbine, all through existing catalog aliases (add a catalog alias only if one is missing, at the version 0a/0e pinned elsewhere).
- iOS emits on **every** refresh (`SystemPlanClock.swift:30–40`); the same-minute drop of `TIME_TICK`/`TIMER` and the +15 ms are Android additions (OQ-1d-11, `04 §9.15`).

**Edge cases**
- A wall-clock jump backwards (`TIME_CHANGED` to an earlier minute) emits and re-arms from the new time.
- `TIME_TICK` and the timer both fire near a boundary: a `TIME_TICK` in an already-emitted minute is dropped (`shouldEmit`) and still re-arms the timer; a `TIME_TICK` after the boundary emits and moves the timer to the next minute, so the minute emits once; a `TIMEZONE_CHANGED` in the same minute still emits.
- Cancelling the collector while the timer waits leaves no receiver and no job.
- `CancellationException` from `delay` propagates; never catch `Exception` around it.

**Tests**
- `MinuteClockPolicyTest` (JVM, `:core:data` `src/test`): `delayToNextMinuteMs` at `…:00.000` → 60,015; at `…:30.000` → 30,015; at `…:59.999` → 16; at a negative epoch → still in (15, 60,015]. `shouldEmit`: `TIME_TICK` in the same minute → false; `TIMER` in the same minute → false; either in the next minute → true; first event (`null`) → true; `TIMEZONE_CHANGED`, `TIME_CHANGED`, `DATE_CHANGED` and `START` in the same minute → true.
- `SystemPlanClockTest` (Robolectric, `@RunWith(AndroidJUnit4::class) @Config(sdk = [36]) @Logic`), with `runTest(timeout = 60.seconds)` and `wallMs` tied to the test scheduler's virtual time (start at `2026-10-13T08:00:30Z`):
  - collection emits once immediately; after `advanceTimeBy(30_015)` followed by `runCurrent()` (`advanceTimeBy` does not run tasks scheduled exactly at the target time) it emits `08:01:00.015`;
  - while collecting, `ShadowApplication` lists one receiver with exactly the four actions; after cancellation it lists none;
  - `sendBroadcast(Intent(ACTION_TIMEZONE_CHANGED))` + `shadowOf(getMainLooper()).idle()` emits once, and the next timer emission still lands on the next minute boundary;
  - a `TIME_TICK` in the minute already emitted emits nothing; a `TIME_TICK` sent at `08:02:00.005` (after the boundary, before the timer) emits once, and the next emission is the timer at `08:03:00.015` (no second emission in minute 08:02). The `TIMER` gate itself is covered by `MinuteClockPolicyTest`.
  Use Turbine; no `Thread.sleep`.

### Task 2: Clock projection onto days

**Commit:** `feat(plan): project the plan clock onto timeline days`. **Ticks:** none.

Port of iOS `TripPlanModel.updateDayClocks` (l.51–56) plus `PlanDecoder.nowTexts` (l.142–149; 1b ported it as `PlanPreparer.nowTexts`, whose per-day text rule this function must equal), changed in one way: offset and text are computed together and written once per tick, so a tick causes exactly one write per changed day (iOS writes `now` and then `text`).

**Files** (`core/plan/…/trippage/DayClockProjection.kt`, pure JVM):
- `data class NowLineValue(val offset: Double /* dp from the day's top */, val text: String)`.
- `fun projectDayClocks(layouts: Map<String, DayLayout>, now: Instant, formatter: CopyLineFormatter, format: CopyFormat): Map<String, NowLineValue>`: for each day where `nowLineOffset(layout.day, now)` is non-null, `NowLineValue(offset, formatter.nowLabel(layout, now, format))`. Days without now are absent.
- `fun changedDayIds(old: Map<String, NowLineValue>, new: Map<String, NowLineValue>): Set<String>`: ids whose value differs, including ids present in only one map.

**Edge cases:** `now == day.end` belongs to the next day (start ≤ now < end). A day with two zones uses the zone containing now, else the last zone (iOS `nowLabel`). `now` before the first day or after the last day → empty map. 23 h and 25 h DST days use UTC minutes (heights from `layoutDay`).

**Tests:** `DayClockProjectionTest` (JVM, large60 from `LargeTimelineFixtures`, en, 24 h):
- `now = day0.startUtc` → exactly `{day0}`, offset 0;
- `+60 s` → `changedDayIds` = `{day0}` (offset 2/3 dp, 40 dp/h);
- `= day1.startUtc` → `changedDayIds` = `{day0, day1}`;
- same instant twice → empty;
- before the plan and after the last day's end → empty map;
- large60 day 2 (13.5 h, 540 dp) and day 3 (25.5 h, 1,020 dp) (`04 §2.3` cycle 24, 24, 13.5, 25.5, 24, 24 h): offset at the day's end − 1 min equals 539.333… dp and 1,019.333… dp (± 1e-6);
- for every day of large60 (a minute tick then a boundary tick, walked over all 60 days): `changedDayIds` has exactly 1 entry for a mid-day minute and exactly 2 at each of the 59 inner boundaries (`day[i].endUtc == day[i+1].startUtc`, 1b fixture invariant); at the last day's end it has exactly 1 (the last day leaves, none enters);
- for one day, `projectDayClocks(...)[id].text` equals `PlanPreparer.nowTexts(...)[id]` (same rule as 1b).

### Task 3: Timeline probe and harness controls

**Commit:** `test(trip): add the timeline probe and harness controls`. **Ticks:** none.

**Files**
- `feature/trip/…/timeline/TimelineProbe.kt` (main):
  ```kotlin
  interface TimelineProbe {
      enum class Kind { STATIC, CLOCK, TARGETS, HEADER, ANCHOR }
      fun composed(kind: Kind, dayId: String)
      fun drew(kind: Kind, dayId: String)
  }
  val LocalTimelineProbe = staticCompositionLocalOf<TimelineProbe?> { null }
  ```
- `feature/trip/build.gradle.kts` (if absent): `testImplementation(project(":core:testing-compose"))` (brings `ProfileRule`, `ProfileHost`, `Guards`, Robolectric, Compose UI test and `:core:testing` as `api`, 0e T7) and `testImplementation` of `kotlinx-coroutines-test` and Turbine. `:feature:trip` main depends only on `core` modules (Konsist), never on another feature.
- `feature/trip/src/test/kotlin/net/chronoflow/feature/trip/timeline/CountingTimelineProbe.kt`: thread-safe counts per (kind, dayId) for `composed` and `drew`; `snapshot(): Counts`; `Counts.delta(since)`.
- Call sites (added by Tasks 5 and 6): `SideEffect { probe?.composed(kind, dayId) }` as the **first** statement of each probed composable (it runs after every composition of that scope), and `probe?.drew(STATIC, dayId)` as the first statement inside the static layer's `onDrawBehind`.

**Tests:** `TimelineProbeHarnessTest` (Robolectric, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`, `@Logic`). It proves what the harness can show (AGENTS "Tests" rule 6) before Task 7 relies on it:
1. *Draws run:* a `Spacer` whose `drawWithCache { onDrawBehind { read(s); count++ } }` reads a state `s`; writing `s` and `waitForIdle()` increments the count by exactly 1.
2. *Layer isolation is observable:* a parent `Box` with `drawBehind { read(p) }` contains a child with `graphicsLayer()` + `drawWithCache { onDrawBehind { childCount++ } }`; writing `p` redraws the parent and leaves `childCount` unchanged. Without `graphicsLayer()` (negative control in the same test) `childCount` increases.
3. *Placement does not redraw a layer:* moving the child with `Modifier.offset { }` from a state leaves `childCount` unchanged.

0e already sets `robolectric.graphicsMode=NATIVE` and `robolectric.pixelCopyRenderMode=hardware` for every test task. If control 2 or 3 cannot pass in Robolectric, keep control 1, record the outcome in `docs/perf-timeline.md` "Phase 1d", and apply OQ-1d-3: draw-count assertions move to an instrumented test; composition assertions stay in Robolectric. In the same commit, add `:feature:trip:connectedDebugAndroidTest` and its results glob to `make check`'s smoke run (OQ-1d-3), or `make check` never runs it.

### Task 4: Drawing repository

**Commit:** `feat(trip): build day drawings off the main thread`. **Ticks:** none.

**File:** `feature/trip/…/timeline/TimelineDrawingRepository.kt`:
```kotlin
class TimelineDrawingRepository(
    private val cache: DayDrawingCache,
    private val palette: (dark: Boolean) -> TimelinePalette,
    private val metrics: suspend () -> TextMetrics,  // a new TextPaintMetrics per build job; production first awaits 1c's TimelineTypefaces.load (IO, cached per process)
    private val buildDispatcher: CoroutineDispatcher, // Dispatchers.Default.limitedParallelism(2), created once
    private val scope: CoroutineScope,               // viewModelScope
)
```
- `fun peek(key: DayDrawingKey, day: PreparedDay): DayDrawing?` = `cache.peek(key, day)`. Never builds.
- `suspend fun drawing(key, day, layout): DayDrawing`: cache hit → return it; else join the in-flight build for `(key, day)` or start one in `scope` on `buildDispatcher`: `val m = metrics()` (may suspend for the typefaces; outside the trace), then `trace("cf.timeline.build") { DayDrawingBuilder.build(day, layout, key, palette(key.dark), m) }` (a synchronous slice on the worker thread, never async), then `cache.put`. The in-flight map is guarded (`Mutex` or `ConcurrentHashMap<Pair<…>, Deferred<DayDrawing>>`), and an entry is removed when its build ends.
- `data class DrawingRequest(val key: DayDrawingKey, val day: PreparedDay, val layout: DayLayout)` (same file).
- `fun prefetch(requests: List<DrawingRequest>)`: starts builds for the given requests without awaiting (cache hits and in-flight builds are skipped). The host ViewModel's `prefetchAround` (Task 6) calls it with the days at index ±1 and ±2 of a day whose drawing just arrived.
- Tracing: if `:feature:trip` lacks it, add `implementation` of the catalog's `androidx.tracing:tracing-ktx` alias (0a catalog, `tracing = "2.0.5"`) in this commit.
- If 1c's `DayDrawingCache` has no constructor parameters for its limits, add `maxBytes: Long = 4L * 1024 * 1024` and `maxEntries: Int = 8` in this commit (tests need a larger cache; production keeps the 1c values).

**Edge cases:** a build for a key nobody waits for still completes and lands in the cache (≤ 5 ms each). The caller's cancellation does not cancel a shared build. A builder exception propagates to the awaiting caller (Task 5 handles it); `CancellationException` is rethrown, never swallowed.

**Tests:** `TimelineDrawingRepositoryTest` (JVM, `:feature:trip` `src/test`, `FakeTextMetrics`, S3, `runTest(timeout = 60.seconds)`):
- two concurrent `drawing()` calls for the same key build once (`cache.buildCount == 1`) and return the same instance;
- the builder runs on the injected dispatcher (a named single-thread dispatcher; assert `Thread.currentThread().name` inside a wrapped `metrics` factory);
- `peek` returns null before and the instance after;
- `prefetch` of 4 days builds 4 and never more than `maxEntries` stay cached;
- a different `PreparedDay` with an equal key rebuilds (1c's equality guard).

### Task 5: DayTimeline, NowLine and TimelineTargets

**Commit:** `feat(trip): add the day timeline with an isolated now line`. **Ticks:** "DayTimeline composable" and "NowLine".

**Files** (`feature/trip/…/timeline/`)
- `DayClock.kt`:
  ```kotlin
  @Stable class DayClock internal constructor() { internal val value = mutableStateOf<NowLineValue?>(null) }
  class DayClocks {                       // main thread only
      fun clock(dayId: String): DayClock  // stable identity per day id (iOS dayClock(for:))
      fun retain(dayIds: Set<String>)     // drop clocks of days that left the plan
      fun apply(values: Map<String, NowLineValue>) // write each holder only if its value changed
  }
  ```
- `DayTimeline.kt`:
  ```kotlin
  @Composable fun DayTimeline(
      day: PreparedDay, layout: DayLayout, key: DayDrawingKey, dayIndex: Int, availableWidthDp: Double,
      repository: TimelineDrawingRepository, resources: TimelineResources,
      clock: DayClock, onBlock: (String) -> Unit, onFirstReplay: () -> Unit,
      prefetch: (dayIndex: Int, key: DayDrawingKey) -> Unit, modifier: Modifier = Modifier,
  )
  ```
  Behaviour:
  1. `SideEffect { probe?.composed(STATIC, day.id) }` first.
  2. `val drawing = remember(day.id) { mutableStateOf(repository.peek(key, day)) }`: a cached day draws in its first frame. The state is kept across key changes, so the old drawing stays on screen until the new one is ready (iOS keeps `drawing` until `.task(id:)` replaces it; no placeholder flash on a check-off).
  3. `LaunchedEffect(key, day) { val d = repository.drawing(key, day, layout); drawing.value = d; prefetch(dayIndex, key) }`. `repository.drawing` returns a cache hit at once (the cache's `get(key, day)` already checks the day), so no "built for which day" bookkeeping is needed. A change of `key` or `day` cancels the effect before it assigns, so a stale result never overwrites a newer one (the shared build itself keeps running, Task 4). On a builder exception other than `CancellationException` (catch the specific types the builder throws, e.g. `IllegalStateException`/`IllegalArgumentException`, never `Exception`): keep the current state, `Log.w("CfTimeline", …)` in debug, and do not retry in a loop (OQ-1d-6).
  4. Size: `val geometryWidthDp = remember(layout, availableWidthDp) { TimelineGeometry(layout, availableWidthDp).width }`; `Modifier.size(geometryWidthDp.dp, layout.height.dp)`. If `geometryWidthDp > availableWidthDp`, wrap the day in `Modifier.horizontalScroll(rememberScrollState())` (iOS `DayTimelineView.swift:48–52`; happens on the narrowest windows, e.g. a 280 dp window gives 248 dp of content).
  5. Root `Box` semantics (static, never recomposed by readiness): `Modifier.semantics { contentDescription = <"Plan timeline">; isTraversalGroup = true }` and `testTag("trip.day.<dayId>")` on the item root.
  6. The static layer is a `Spacer(Modifier.matchParentSize().graphicsLayer().drawWithCache { … }.pointerInput(day.id, drawing) { … })`. Build this modifier chain in a `remember(drawing, resources, placeholder, onFirstReplay)` so a recomposition with an equal chain does not invalidate the draw.
     - `onDrawBehind`: `probe?.drew(STATIC, day.id)`; `val d = drawing.value`; if `d == null`: `trace("cf.timeline.placeholder") { drawPlaceholder() }`; else `trace("cf.timeline.replay") { replay(d, resources) }` then `onFirstReplay()`.
     - **Placeholder** (iOS draws a plain `surface2` fill, `DayTimelineView.swift:83–85`; plan.md 1d asks for "background and ticks", which is the default here): the four lane rectangles in the palette's `surface2` and the visible tick lines, 1 dp, butt cap, in `border` at alpha `PlanMetrics.gridOpacity` (0.6) (same geometry and colors as the 1c builder's lanes and tick lines, 1c "Draw order" items 1–2; the two fill/stroke `Paint`s are created in the same `remember` as the geometry below, never in draw; precompute the rects and tick tops once per `(day, key)` in a `remember` as a `FloatArray`). No text, no tiles. No page background (the list paints it).
     - `pointerInput`: `detectTapGestures(onTap = { p -> drawing.value?.hits?.resolve(p.x, p.y)?.let(onBlock) })`. Resolution order is 1c's (tiles by closest midY, then markers `|Δy| ≤ 24 dp` and `x ≤ 44 dp`, then zone changes `|Δy| ≤ 24 dp`). No tap is resolved while the placeholder shows.
  7. `NowLine(clock, dayId = day.id, widthDp = geometryWidthDp)`: a separate restartable, skippable composable.
  8. `TimelineTargets(drawing, dayId = day.id, onBlock)`: a separate composable.
- `NowLine.kt` (port of `DayTimelineView.swift:87–98` and the accessibility child l.119–122, board `Plan.dc.html` now line):
  1. `SideEffect { probe?.composed(CLOCK, dayId) }` first; then `val value = clock.value.value ?: return`.
  2. Placed with `Modifier.offset { IntOffset(0, (value.offset.dp.toPx() − nowHeightPx / 2).roundToInt()) }` and `.graphicsLayer()` (own layer).
  3. Line: full day width, `PlanMetrics.emphasisBorder` (2 dp) tall, color `now`, centred on the offset.
  4. Pill: left-aligned at x = 0, `Text(value.text)` in `planNow` (Figtree 600, 10/16 sp, `fontFeatureSettings = "tnum"`), color `primaryForeground`, background `now` in a capsule, horizontal padding `PlanMetrics.tilePadding` (6 dp), `heightIn(min = 16.sp-scaled dp)` (= `PlanMetrics.planNowLineHeight` × font scale). The pill wraps its text (`fixedSize`), including 12-hour text.
  5. Semantics: `clearAndSetSemantics { contentDescription = <"Now, %1$s" with value.text>; traversalIndex = -1f }`.
- `TimelineTargets.kt`:
  1. `SideEffect { probe?.composed(TARGETS, dayId) }` first; reads `drawing.value` (this scope recomposes once when the drawing arrives or changes).
  2. A `Layout` whose modifier carries `testTag(if (d == null) "trip.timeline.preparing" else "trip.timeline.ready")`.
  3. One child per `d.targets` entry, measured exactly to its frame (px) and placed at `(left, top)`, with only `Modifier.semantics` (no `clickable`, no indication, no pointer input):
     - BLOCK: `contentDescription = label; role = Role.Button; traversalIndex = t.traversalIndex; testTag = t.testTag; onClick { onBlock(blockId); true }`;
     - DIVIDER and TICK: `contentDescription = label; traversalIndex = t.traversalIndex`.

**Copy** (generated by 0c from `PlanUI/Resources/Localizable.xcstrings`; find each name by its `<!-- plan: … -->` comment in `core/ui/src/main/res/values/strings_ios.xml`):

| iOS key | Expected resource | en | nl |
|---|---|---|---|
| `Plan timeline` | `plan_plan_timeline` | Plan timeline | Tijdlijn van het plan |
| `Now, %@` | `plan_now_x` (`%1$s`) | Now, %1$s | Nu, %1$s |

**Edge cases:** a day with no blocks still draws lanes and ticks and has no BLOCK targets. A day with the now line and a horizontal overflow scrolls the now line with the content. Disposal releases the drawing reference (the `remember` goes away). Theme, width, font scale or language changes give a new key and rebuild; the old drawing stays visible until then.

Also in this commit: the `stability_config.conf` lines and `@Stable` annotations of rule 11.

**Tests** (Robolectric, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`, `@Logic`, profile `pixel8` via `ProfileRule` + `ProfileHost`, `FakeTextMetrics`, build dispatcher `Dispatchers.Unconfined`, `prefetch = { _, _ -> }`, `availableWidthDp = 379.0` (pixel8: 411 − 2 × 16) unless stated; each < 5 s):
- `DayTimelineTest`:
  - `placeholderThenReady`: with an empty cache and a gated builder (the repository's `metrics` factory first awaits a `CompletableDeferred<Unit>` the test completes), `trip.timeline.preparing` exists and no `trip.block.*`; after releasing the builder and `waitForIdle()`, `trip.timeline.ready` exists and the S3 day's first block target `trip.block.<id>` exists with `Role.Button`;
  - `tapResolvesTile`: `performTouchInput { click(center of tile frame) }` on the day calls `onBlock` with that block id; a click in an empty lane area calls nothing;
  - `semanticsClickOpensBlock`: `onNodeWithTag("trip.block.<id>").performSemanticsAction(OnClick)` calls `onBlock(id)`;
  - `targetsAreAtLeast48dp`: the bounds of every `trip.block.*` node are ≥ 48 dp in both axes and inside the day (query each by its known tag from the fixture; no node loops);
  - `overflowScrollsHorizontally`: with `availableWidthDp = 248.0` (a 280 dp window minus 2 × 16; no profile has that width, so pass the width rather than change the profile) and an S3 day whose `TimelineGeometry(layout, 248.0).width > 248.0` (assert that first), the day's `trip.day.<id>` node or its scrolling child (`useUnmergedTree = true`) has `HorizontalScrollAxisRange`; with `availableWidthDp = 379.0` neither has it.
- `NowLineTest`:
  - the node with content description "Now, 08:00" exists when the clock value is set, and nothing exists when it is null;
  - nl gives "Nu, 08:00";
  - its top equals `offset − nowHeight/2` within 1 px.

### Task 6: TimelinePage and its host ViewModel

**Commit:** `feat(trip): host the timeline in a lazy page`. **Ticks:** "TimelinePage".

**Files** (`feature/trip/…/timeline/host/` unless stated)
- `TimelineSource.kt`:
  ```kotlin
  data class TimelineSourceInput(val detailBytes: ByteArray, val marks: List<CheckMark>, val timeFormat: TimeFormat, val hidden: List<BlockType>)
  interface TimelineSource { fun inputs(): Flow<TimelineSourceInput> }
  ```
  (6b replaces it with `TripPageSource`.)
- `TimelineHostViewModel.kt`:
  ```kotlin
  class TimelineHostViewModel(
      source: TimelineSource, private val clock: PlanClock, language: Language,
      compute: CoroutineDispatcher,          // Dispatchers.Default in production
      buildDispatcher: CoroutineDispatcher,  // Dispatchers.Default.limitedParallelism(2), created once
      metrics: suspend () -> TextMetrics, cache: DayDrawingCache = DayDrawingCache(),
      io: CoroutineDispatcher = Dispatchers.IO, // CopyCatalogProvider's resource read
  ) : ViewModel()
  ```
  - `init`: `Trace.beginAsyncSection("cf.timeline.firstFrame", 0)` as the **first** statement (ended by `firstFrame`, below), so the section covers the fixture read, decode, layout, prepare, typeface load and first build.
  - Collaborators, each created once in the ViewModel: `val catalogs = CopyCatalogProvider(io)` (1a); `val decoder = PlanDecoder(worker = compute)` (1a); `val preparer = PlanPreparer(decoder, catalogs, worker = compute)` (1b Task 8); `val firstFrame = FirstTimelineFrameTrace()`.
  - `val state: StateFlow<TimelineHostState>` with `Preparing` and `Ready(model: TimelineHostModel)`, `stateIn(viewModelScope, SharingStarted.Eagerly, Preparing)`. Pipeline: `source.inputs().mapLatest { input -> … }` with, in order: `detail = decoder.detail(PlanVersionRecord(tripId = "fixture", inputHash = "fixture", detail = input.detailBytes, receivedAt = ""))` (1a; equal bytes hit its cache, so a check-off input does not re-decode); `layouts = preparer.layouts(detail, input.hidden)`; `prepared = preparer.prepare(detail, input.hidden, language, input.timeFormat, input.marks)` (1b: unchanged days keep their instance). All three run on `compute` inside the 1a/1b classes. `mapLatest` drops stale work (iOS revision guard).
  - `TimelineHostModel(detail, prepared, layouts: Map<String, DayLayout>, index: TimelineListIndex, openingDayId: String, timeFormat)`. `openingDayId` = 1b `openingDay` with no requested day, block or kept day, and `now = clock.values().first()` (rule 4 "the day containing now", 5 "the last day if past", 6 "the first day").
  - `val clocks = DayClocks()`; on each Ready, `clocks.retain(day ids)`.
  - `suspend fun runClock()`: `clock.values().collect { now -> val v = withContext(compute) { projectDayClocks(model.layouts, now, formatter, format) }; clocks.apply(v) }` (apply on main). Re-projects when the model changes (`combine` the model and the clock).
  - `val anchorDayId: StateFlow<String?>` and `fun select(dayId: String)`: sets it only when it changes (iOS `select(dayID:)`, `TripPageModel.swift:159–164`).
  - `val repository: TimelineDrawingRepository` built with `viewModelScope`, `buildDispatcher`, `metrics`, `cache` and `palette = { dark -> if (dark) <TimelinePalette dark> else <light> }` (the generated `TimelinePalette` objects, 0b/1c).
  - `fun prefetchAround(dayIndex: Int, key: DayDrawingKey)` (main thread): reads the current `Ready` model; for `i` in `dayIndex − 2 … dayIndex + 2` except `dayIndex`, inside `0 until days.size`, builds `DrawingRequest(key.copy(dayId = days[i].id), days[i], layouts.getValue(days[i].id))` and passes the list to `repository.prefetch`. Every day of one page shares all key fields except `dayId`, so `copy` gives the neighbour's exact key.
  - The `CopyLineFormatter` for the clock projection is `CopyLineFormatter(catalogs.catalog())`, created once inside `withContext(compute)` on the first `Ready` (1a: the catalog loads once on `io`), never on main. `format = CopyFormat(language, model.timeFormat)`.
- `TimelineListIndex.kt` (pure, `:feature:trip` `main`): `headerIndex(dayId)`, `dayIndex(dayId)`, `endIndex`, and `fun currentDayIndex(visible: List<LazyListItemInfo>, thresholdPx: Int): Int?` = the last **day** item (contentType `"day"`) whose `offset ≤ thresholdPx`. Index math only; it never searches the plan. The threshold is `PlanMetrics.compactHeaderHeight` (36) scaled with the font like iOS `@ScaledMetric` (`TripPageView.swift:21, 187–191`; see OQ-1d-14 for the reference-point difference): `thresholdPx = with(density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }` (font scale already capped at 1.3 by `ConfigurationPolicy`; OQ-1d-14). The plain header's min height uses the same value, so a day becomes current exactly when its content reaches the pinned header's bottom.
- `FirstTimelineFrameTrace.kt`: `class FirstTimelineFrameTrace` with `fun onReplay()` (main thread). The first call posts `Choreographer.getInstance().postFrameCallback` twice (nested), then `Trace.endAsyncSection("cf.timeline.firstFrame", 0)`. Later calls return after one boolean check. A placeholder draw never calls it. One instance per ViewModel (`viewModel.firstFrame`); `TimelinePage` passes `onFirstReplay = viewModel.firstFrame::onReplay` to every `DayTimeline`.
- `TimelineKeys.kt` (pure helpers in `timeline/host/`, used by `TimelinePage` and by the tests, so both compute identical keys):
  - `fun timelineAvailableWidthDp(windowWidthDp: Double, safeLeftDp: Double, safeRightDp: Double): Double = windowWidthDp − 2 × TripPageLayout.resolve(windowWidthDp).gutter − safeLeftDp − safeRightDp`;
  - `fun timelineDrawingKey(dayId: String, revision: String, availableWidthDp: Double, density: Density, dark: Boolean, compact: Boolean, language: Language) = DayDrawingKey(dayId, revision, availableWidthPx = (availableWidthDp * density.density).roundToInt(), density = density.density, textScale = timelineTextScale(density), dark, compact, language)`.
- `feature/trip/…/timeline/TimelinePage.kt`:
  ```kotlin
  @Composable fun TimelinePage(
      viewModel: TimelineHostViewModel, onBlock: (String) -> Unit, modifier: Modifier = Modifier,
      onListState: ((LazyListState) -> Unit)? = null, // test seam: Tasks 6–7 drive dispatchRawDelta/scrollToItem through it
  )
  ```
  `onListState` is invoked once per list state from `LaunchedEffect(listState) { onListState?.invoke(listState) }` inside the Ready branch; it reads nothing and causes no recomposition. Production passes nothing.
  1. Clock gate: `val owner = LocalLifecycleOwner.current; LaunchedEffect(viewModel, owner) { owner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.runClock() } }`. Receivers exist only while the activity is started; a restart re-emits immediately.
  2. `Preparing`: the page background only, with `semantics { contentDescription = <"Loading your plan"> }`. No skeleton (6c, `PlanLoading`).
  3. `Ready`:
     - `Scaffold(topBar = { TopAppBar(...) }, contentWindowInsets = WindowInsets(0, 0, 0, 0))` with `TopAppBarDefaults.topAppBarColors(containerColor = background, titleContentColor = foreground)` and `windowInsets = LocalSafeInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)`, pinned (no scroll behaviour). All insets come from `LocalSafeInsets` (0e: production fills it from `WindowInsets.safeDrawing`; Robolectric gets the profile's insets only through it), never from `WindowInsets.systemBars`/`safeDrawing` directly, or the guards in `small`/`landscapeSideNav` see wrong positions and production pads twice. If `TopAppBar` is still `@ExperimentalMaterial3Api` in material3 1.4.0, opt in at the call site only. Title slot = the menu anchor (below).
     - Root `BoxWithConstraints` (once, around the list) → `windowWidthDp = maxWidth.value.toDouble()` (the Scaffold content has no insets applied, so its width is the window width); `availableWidthDp = timelineAvailableWidthDp(windowWidthDp, safeLeftDp, safeRightDp)` with the left/right of `LocalSafeInsets.current` converted to dp; `compact = windowWidthDp < PlanMetrics.timelineLabelBreakpoint` (640; iOS `TripPageView.swift:134` compares the container width).
     - `val listState = rememberLazyListState(initialFirstVisibleItemIndex = index.headerIndex(model.openingDayId))`, created inside the Ready branch, so the first frame composes only the opening day and its neighbours: no scroll animation, no retries (RN lesson 4).
     - The Scaffold content root is a `Box(Modifier.fillMaxSize().padding(innerPadding).testTag("trip.page"))` holding only the list (1e flings this node).
     - `LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("trip.scroll"), contentPadding = PaddingValues(start/end = gutter + safe left/right inset, bottom = safe bottom inset + PlanMetrics.gutter))` (iOS `.padding(.bottom, PlanMetrics.gutter)`, `TripPageView.swift:165`), items per rule 1:
       - **Header** `DayHeaderLine(day)`: `SideEffect { probe?.composed(HEADER, day.id) }` first; `Text` of `"<title> · <date>"` built with the untranslatable resource `plan_day_title_date` = `%1$s · %2$s` (OQ-1d-8) in `planListTitle`, color `foreground`, on `card` (iOS compact line is an opaque `card`, `04` OQ-T13), `heightIn(min = thresholdDp)` (`compactHeaderHeight` 36 scaled as above), horizontal padding 0 (the list pads), `semantics { heading() }`. If `stickyHeader` is still `@ExperimentalFoundationApi` in the BOM's foundation, opt in at the call site only (`@OptIn`), never module-wide.
       - **Day** `DayTimeline(...)` with `key = timelineDrawingKey(day.id, prepared.revision, availableWidthDp, LocalDensity.current, dark, compact, language)`, `availableWidthDp`, `dayIndex` = the day's index in `prepared.days`, `onFirstReplay = viewModel.firstFrame::onReplay`, `prefetch = viewModel::prefetchAround`, bottom padding `PlanMetrics.sectionGap` (12 dp; iOS `LazyVStack(spacing: PlanMetrics.sectionGap)`, l.119).
       - **End** `Text(<"End of this plan">, Modifier.testTag("trip.plan.end"))` in `planListTitle`, `foreground` (the designed end card is 6b).
     - `TimelineResources` from `TimelineResources.Cache` for `(density, dark)`, once per page composition, never in draw.
     - Current day: `val current by remember(listState, index) { derivedStateOf { index.currentDayIndex(listState.layoutInfo.visibleItemsInfo, thresholdPx) } }`; `LaunchedEffect(current) { current?.let { viewModel.select(dayIdAt(it)) } }`. Nothing in an item reads it.
     - **Day menu** `DayMenu(days, anchorDayId, onPick)`:
       - `SideEffect { probe?.composed(ANCHOR, anchorDayId ?: "") }` first;
       - anchor: the title `Text` of `"<from IATA> → <to IATA>"` via the iOS key `%@ → %@` (resource `plan_x_x`; iOS `codeTitle`, `TripPageView.swift:287–291`), with `Modifier.testTag("trip.dayMenu")`, `minimumInteractiveComponentSize()` (the title text is shorter than 48 dp; the 0e touch-target guard checks it) and a `clickable(role = Role.Button, onClickLabel = <"Go to a day">)`. Semantics port iOS l.317–323: `contentDescription` = `"%@ to %@, %@. Go to a day"` (from city ?: IATA, to city ?: IATA, `prepared.spokenDates`) while the current day's header has not scrolled, else `"%@ to %@, %@, %@. Go to a day"` (from, to, day title, day date); `stateDescription` = `"<title> · <date>"` of the current day. "Scrolled" = a `derivedStateOf` that is true when the current day's **day item** offset < `thresholdPx` (changes only at boundaries; never read in an item);
       - an M3 `DropdownMenu(expanded, containerColor = popover, scrollState = menuScroll)`; first row: a non-clickable `Text(<"Go to a day">)` in `planSubtitle`, `mutedForeground`; then one `DropdownMenuItem(text = { Text("<title> · <date>") }, onClick = { close; onPick(i) }, colors = MenuDefaults.itemColors(textColor = foreground), modifier = Modifier.testTag("trip.dayMenu.day.$i").semantics { selected = (day.id == anchorDayId) })` per day, `i` 0-based;
       - on open, `menuScroll.scrollTo(i_current × PlanMetrics.minTouchTarget.dp.roundToPx())` (pre-scrolled to the current day, `04 §9.10`; an M3 `DropdownMenuItem` is 48 dp tall at font scale 1, and an approximate position at larger fonts is acceptable). The "Go to a day" header row adds its own height; ignore it. No check icon (OQ-1d-10).
     - **Jump** (iOS `TripPageView.swift:394–398`: `select(dayID:)`, then `scrollTo(id, anchor: .top)`, animated unless Reduce Motion; the 50 ms drawing wait is the Android addition of `04 §9.6`): `viewModel.select(id)`; await the target day's drawing for at most 50 ms (`withTimeoutOrNull(50) { viewModel.repository.drawing(key, day, layout) }`, key from `timelineDrawingKey`); then `listState.scrollToItem(index.headerIndex(id))` if Reduce Motion, else `animateScrollToItem(index.headerIndex(id))`.
- `MainActivity` is not touched in this task.

**Copy:**

| iOS key | Expected resource | en | nl |
|---|---|---|---|
| `Go to a day` | `plan_go_to_a_day` | Go to a day | Ga naar een dag |
| `End of this plan` | `plan_end_of_this_plan` | End of this plan | Einde van dit plan |
| `Loading your plan` | `plan_loading_your_plan` | Loading your plan | Je plan laden |
| `%@ → %@` | `plan_x_x` (no collision: the 0c slug drops `→` and keeps the spaces as `_`; the key `%@–%@` slugs to `plan_xx`; verified with the 0c slug rule against the PlanUI catalog at the pin) | %1$s → %2$s | %1$s → %2$s |
| `%@ to %@, %@. Go to a day` | `plan_x_to_x_x_go_to_a_day` | %1$s to %2$s, %3$s. Go to a day | %1$s naar %2$s, %3$s. Ga naar een dag |
| `%@ to %@, %@, %@. Go to a day` | `plan_x_to_x_x_x_go_to_a_day` | %1$s to %2$s, %3$s, %4$s. Go to a day | %1$s naar %2$s, %3$s, %4$s. Ga naar een dag |
| (Android, untranslatable) | `plan_day_title_date` in `core/ui/src/main/res/values/strings_untranslatable.xml`, `translatable="false"` | %1$s · %2$s | (same) |

**Edge cases:** a trip that is entirely in the past opens on its last day; one in the future on its first day. A plan whose day set changes (new revision) keeps `listState` (its keys are day ids); anchor restoration on replacement is 6b. A config change (rotation, A8 handles it in place) gives new keys and rebuilds, and keeps `listState`. A menu pick of the current day still scrolls to its top.

**Tests**
- `TimelineListIndexTest` (JVM): header/day/end indices for 3 days (0/1, 2/3, 4/5, end 6); `currentDayIndex` with fake `LazyListItemInfo`s: a day item at offset 36 px with threshold 36 is current, at 37 it is not, and the last qualifying one wins.
- `TimelinePageTest` (Robolectric, `@Logic`, `pixel8`, `FakePlanClock`, `FakeTextMetrics`, dispatchers `Dispatchers.Unconfined` (also for `io`), large60 unless stated; each < 5 s). Get the list state through `onListState`; call `dispatchRawDelta`/`scrollToItem` inside `composeRule.runOnIdle { }` (main thread). Reduce Motion on = `LocalReduceMotion provides true` if Phase 2 is in the stack, else `ShadowValueAnimator.setDurationScale(0f)` before `setContent` (restore `1f` in `@After`):
  - `opensOnTheDayContainingNow`: clock at day 33's start + 2 h → `onNodeWithTag("trip.day.<day33Id>")` is displayed and its top equals the `trip.scroll` top + `thresholdPx` (±1 px; the pinned plain header sits above it), and `trip.day.<day32Id>` does not exist;
  - `pastTripOpensOnLastDay` (S3, clock after its end);
  - `menuListsEveryDay`: click `trip.dayMenu` → `trip.dayMenu.day.0` and `trip.dayMenu.day.59` exist (`waitUntilExactlyOneExists`);
  - `menuJumpReachesTheEnd`: pick `trip.dayMenu.day.59` with Reduce Motion on → `trip.day.<day59Id>` is displayed with its top at `trip.scroll` top + `thresholdPx` (±1 px), then `onNodeWithTag("trip.scroll").performScrollToNode(hasTestTag("trip.plan.end"))` succeeds;
  - `stickyHeaderStaysPinned`: after `dispatchRawDelta(300 dp)` into day 33, the node with text `"<day33 title> · <day33 date>"` (exact text from the fixture, `useUnmergedTree = true`) has its top at the `trip.scroll` top (±1 px);
  - `anchorFollowsTheCurrentDay`: scrolling until day 34's item offset ≤ `thresholdPx` sets `vm.anchorDayId.value` to day 34's id, and the `trip.dayMenu` node's `stateDescription` becomes `"<day34 title> · <day34 date>"`.
- `TimelinePageGuardTest` (`@Layout`, parameterized, one case per profile × language: `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en, nl; S3): call the 0e guards one by one, **not** `Guards.assertAll` (its `assertHeadings` requires at least one heading tag, and the plain header is a heading by semantics but has no tag; headings get their designed check in 6b): `assertTextFits(rule, profile)`, `assertTouchTargets(rule)`, `assertInteractiveInSafeArea(rule, profile)`, `assertNoInteractiveOverlap(rule, exemptTags = blockTags)`, `assertInk(rule, profile)`. Invisible timeline targets overlap by design (hit frames of at least 48 dp around small tiles; iOS's 44 pt frames overlap too), so `blockTags` = every `trip.block.<id>` of the S3 fixture, built from the fixture, not from the tree. Only the overlap guard gets the exemption; touch-target and safe-area guards run without exemptions (OQ-1d-5).

### Task 7: Invalidation guard

**Commit:** `test(trip): guard timeline invalidation with the probe`. **Ticks:** "TimelineProbe".

**File:** `feature/trip/src/test/kotlin/net/chronoflow/feature/trip/timeline/TimelineInvalidationTest.kt` (Robolectric, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`, profile `pixel8`, `@Logic` so `make quick` runs it; OQ-A30). Port of iOS `TimelineInvalidationTests.scrollingAndTicksInvalidateOnlyAffectedClockChildren`, split into one test per event to keep each under 5 s.

**Setup (shared helper `TimelineTestHost` in the same source set; 1e reuses it):**
- large60 through a test `TimelineSource` backed by a `MutableStateFlow<TimelineSourceInput>`; `FakePlanClock(start = day0.startUtc)`; `language = en`; `compute` and `buildDispatcher` = `Dispatchers.Unconfined`; `FakeTextMetrics`.
- Pre-warm: a `DayDrawingCache(maxBytes = 64 MiB, maxEntries = 64)` passed to the ViewModel. With the Unconfined dispatchers the ViewModel is `Ready` right after construction; take its `prepared` and `layouts`, and before `setContent` put all 60 drawings: `cache.put(key, day, DayDrawingBuilder.build(day, layout, key, <light TimelinePalette>, FakeTextMetrics()))` with `key = timelineDrawingKey(day.id, prepared.revision, timelineAvailableWidthDp(411.0, 0.0, 0.0), composeRule.density, dark = false, compact = true, Language.En)` (pixel8: 411 dp, no side insets, below 640). So every day draws its real drawing on its first frame (no placeholder in the measured window). Assert once, after the first `waitForIdle()`, that `cache.buildCount` did not grow (a key mismatch would show up as builds).
- `CompositionLocalProvider(LocalTimelineProbe provides probe) { TimelinePage(vm, onBlock = {}, onListState = { listState = it }) }`; `waitForIdle()`; take `val before = probe.snapshot()` immediately before each event and `val d = probe.snapshot().delta(before)` after `waitForIdle()`. "Composed before" = keys with a composition count > 0 in `before`. Scroll calls run inside `composeRule.runOnIdle { }`.

**Tests and assertions** (the `04 §10.2` table; "List mode" rows are added by 6c):

| Test method | Event | Assert |
|---|---|---|
| `scrollingAndTicksInvalidateOnlyAffectedClockChildrenWhileScrolling` | 120 × `listState.dispatchRawDelta(1 dp in px)`, each followed by `waitForIdle()`; then scroll until day 1's item offset ≤ `thresholdPx` (one current-day boundary) | For every key composed before: STATIC, HEADER, CLOCK, TARGETS composition deltas = 0 and STATIC draw delta = 0. Keys composed for the first time: STATIC compositions = 1 and STATIC draws ≤ 1. Positive control: ANCHOR composition delta ≥ 1 after the boundary and `vm.anchorDayId.value == day1Id`; during the 120 deltas (intermediate snapshot) ANCHOR delta ≤ 1 (the single "scrolled" flip of the menu's accessibility label) — never one per delta |
| `scrollingAndTicksInvalidateOnlyAffectedClockChildrenOnMinuteTick` | `clock.advance(day0.start + 60 s)` | STATIC, HEADER, TARGETS deltas = 0 for every key; STATIC draws = 0; CLOCK delta = 1 for day0 and 0 for every other key |
| `scrollingAndTicksInvalidateOnlyAffectedClockChildrenOnDayBoundary` | first `scrollToItem(dayIndex(day0), offset = day0Height − 300 dp)` so both day 0's end and day 1's start are composed; snapshot; then `clock.advance(day1.startUtc)` | STATIC, HEADER, TARGETS deltas = 0; STATIC draws = 0; CLOCK delta = 1 for day0 and 1 for day1, 0 elsewhere (exactly two) |
| `checkOffRedrawsOnlyThatDay` | emit a new input (same `detailBytes`) whose `marks` hold one `CheckMark(key = block.checkKey!!, status = CheckStatus.Done, markedAt = "")` for the first block with `actionable && checkKey != null` of the first day `k` that has one (pick both from the prepared fixture, not from the tree); first `scrollToItem(index.headerIndex(dayK))` and `waitForIdle()`, then snapshot, then emit | Day k: STATIC compositions = 1, STATIC draws 1..2, CLOCK = 0. Every other key: STATIC, HEADER, CLOCK compositions = 0 and STATIC draws = 0 |

**Also assert (JVM, cheaper, in `DayClockProjectionTest` from Task 2), over all 60 days:** a minute tick changes exactly one entry and a boundary exactly two. That covers the days the Robolectric viewport does not compose.

**Test budget:** each method < 5 s. If the 120-step scroll exceeds it, replace `waitForIdle()` by `composeRule.mainClock.advanceTimeByFrame()` per step with `autoAdvance = false`; never reduce the 120 steps. If Task 3 found that the harness cannot show layer isolation, apply OQ-1d-3.

**Wire into `make quick`** (OQ-A30): 0e's `cf.tier=quick` already selects `@Logic` in every module that has tests. Verify that `make quick` runs `:feature:trip:testDebugUnitTest -Pcf.tier=quick`: the class appears in `build/test-results/quick/timings.json` (the root file `Scripts/run_tier.py` writes; per-module listener files are `feature/trip/build/cf-timings/testDebugUnitTest/*.json`), and that file reports `"overBudget": 0`. If the Makefile lists modules explicitly, add `:feature:trip`. `TierRegistryTest` must still pass.

### Task 8: Perf entry from fixtures

**Commit:** `feat(app): open fixture trip pages from cf.trip-page`. **Ticks:** "Perf entry" and "SystemPlanClock" (its last bullet, the `cf.freezeAt` freeze, lands here).

**Files**
- `app/src/perf/kotlin/net/chronoflow/app/perf/FixtureTimelineSource.kt`: `class FixtureTimelineSource(name: String, worker: CoroutineDispatcher) : TimelineSource`. All eight names go through `LargeTimelineFixtures.detailBytes(name)` (1b Task 10: `S<n>` → the iOS mock template n, a full `TripDetail` with `trip` and `checkState`; `large30`/`large60` → generated), called inside `flow { emit(...) }.flowOn(worker)` with `worker = Dispatchers.Default` (the templates are a classpath resource read plus a decode: never on main). Do **not** read `plans/S<n>.json` directly: those hold only `plan`, `notices` and `timeline`, not a `TripDetail` (1b Task 2). One emission; marks empty; `TimeFormat` 24 h (iOS default `.value24`); hidden none.
- `app/src/perf/kotlin/net/chronoflow/app/perf/TripPageEntry.kt`:
  ```kotlin
  object TripPageEntry { fun content(intent: Intent, graph: AppGraph): (@Composable () -> Unit)? }
  ```
  - Returns null unless `intent.getStringExtra("cf.trip-page")` is one of `S1`…`S6`, `large30`, `large60` (any other value: `Log.w` and null).
  - Clock: `cf.freezeAt` holding an ISO-8601 instant (`Instant.parse`, e.g. `2026-11-15T08:00:00Z`) → `FixedPlanClock(instant)`; absent, numeric (0e's launch-timeline milliseconds) or anything else → `graph.planClock` (OQ-1d-2). The entry reads the raw extra itself: `TestSeeds` exists only in debug, and its `freezeAtMs` is `null` for an ISO value, so the launch timeline is unaffected.
  - Language from `LocalConfiguration.current.locales[0].language` ("nl" → nl, else en).
  - ViewModel through `viewModel(key = "cf.trip-page:$name:$language", factory = viewModelFactory { initializer { TimelineHostViewModel(FixtureTimelineSource(name, Dispatchers.Default), clock, language, Dispatchers.Default, Dispatchers.Default.limitedParallelism(2), metrics = { TextPaintMetrics(TimelineTypefaces.load(appContext)) }) } })`. `TimelineTypefaces.load` is 1c's suspend, IO-dispatched, once-per-process loader, so the first build waits for it on a worker and later builds get the cached value; the ViewModel is created immediately, so `cf.timeline.firstFrame` covers the font load. Never load typefaces on main or before `setContent`.
  - The ViewModel key includes the language: a locale change recreates the activity (A8 leaves `locale` out of `configChanges`) but keeps the ViewModel store, and the language is a constructor argument.
  - Content: `TimelinePage(vm, onBlock = { Log.d("CfTimeline", "block $it") })`.
- `app/src/release/kotlin/net/chronoflow/app/perf/TripPageEntry.kt`: the same signature, returning null. Every variant gets exactly one copy: `debug`, `benchmarkRelease` and `nonMinifiedRelease` compile `src/perf/kotlin` (0e T15 wired the two perf build types through `addPerfSources`; add `debug` the same way, `onVariants(selector().withBuildType("debug")) { it.sources.kotlin?.addStaticSourceDirectory("src/perf/kotlin") }`, without setting `PERF_ENTRY`; follow `docs/tooling.md` if 0e used the OQ-A28 fallback build type). The `release` source set is not seen by `benchmarkRelease`/`nonMinifiedRelease` (different build-type names), so there is no duplicate class. Keep 0e's `PerfEntry` marker.
- Dependencies: `:core:testing` as `debugImplementation(project(":core:testing"))`, `"benchmarkReleaseImplementation"(project(":core:testing"))` and `"nonMinifiedReleaseImplementation"(project(":core:testing"))` (string notation, next to 0e's `:core:data-mock` lines; with the OQ-A28 fallback, `"benchmarkImplementation"(…)`). Never `implementation` or `releaseImplementation`.
- **Konsist:** 0a's module-graph rule rejects `:app` depending on `:core:testing` outside test configurations (0a "`:core:testing` outside test configurations (A6)"). Amend it in this commit exactly as 0e amended it for `:core:data-mock` (`ModuleGraphTest.dataMockOnlyInDebugAndPerf`): add `testingOnlyInTestDebugAndPerf`, which allows, in `app/build.gradle.kts`, lines with `":core:testing"` that start with `testImplementation(`, `androidTestImplementation(`, `debugImplementation(`, `"benchmarkReleaseImplementation"(`, `"nonMinifiedReleaseImplementation"(` or `"benchmarkImplementation"(`, and nothing else; plus a planted violation `konsist/src/test/resources/violations/moduleGraph/testing/app/build.gradle.kts` with `implementation(project(":core:testing"))` and its `…RejectsPlantedViolation` test. plan.md 1d ("`:core:testing` is a perf and debug dependency only") is the authority for this change.
- `MainActivity`: inside the existing root (which sets `testTagsAsResourceId = true`), if `TripPageEntry.content(intent, graph)` is non-null, render it instead of the normal content. If the 3a launch overlay and splash keep condition are already in the stack, the fixture path skips the overlay and releases the keep condition from the trip page's `Modifier.onPlaced`, never from a draw (AGENTS "Traps"). Read the extra once in `onCreate`.
- `ReleaseReadinessTest` (`app/src/testRelease`): add `Class.forName("net.chronoflow.app.perf.FixtureTimelineSource")` throws `ClassNotFoundException`, and `net.chronoflow.core.testing.LargeTimelineFixtures` throws too.
- `docs/perf-timeline.md` (create the file if absent): a "Phase 1d" section with the Task 3 harness outcome, the commands below and their results, and no performance claims (`pending` until 1e).

**Tests**
- `PerfClockTest` (JVM, `app/src/testDebug`): `cf.freezeAt=2026-10-13T08:00:00Z` → `FixedPlanClock` with that instant; `cf.freezeAt=1500` → not frozen; `""`, `abc`, `2026-10-13` → not frozen; absent → not frozen. Put the parsing in a pure function `perfClockInstant(extra: String?): Instant?` so the test needs no Android.
- `FixtureTimelineSourceTest` (JVM, `app/src/testDebug`): every one of the eight names emits bytes that 1a's `PlanDecoder` decodes; large60 decodes to 60 days.
- `testReleaseUnitTest` passes with the two new assertions.

**Emulator checks** (orchestrator runs them if the worker sandbox cannot), on `pixel` with `make emu`:
```sh
./gradlew :app:installDebug
mkdir -p build/captures/phase-1d
for f in S1 S2 S3 S4 S5 S6 large30 large60; do
  adb logcat -c
  adb shell am start -W -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity --es cf.trip-page $f
  # am start -W returns at the first frame, possibly before the first drawing: poll up to 5 times (a shell script, not a test)
  for i in 1 2 3 4 5; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null
    n=$(adb shell grep -c 'trip.timeline.ready' /sdcard/ui.xml); [ "${n:-0}" -gt 0 ] && break; sleep 1
  done; echo "$f ready nodes: $n"   # must be > 0
  adb exec-out screencap -p > build/captures/phase-1d/$f.png
  adb logcat -d | grep -c StrictMode   # must print 0
done
adb shell perfetto -o /data/misc/perfetto-traces/phase-1d.pftrace -t 15s -a net.chronoflow.app.native.debug gfx view sched dalvik &
adb shell am start -S -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity --es cf.trip-page large60
for i in $(seq 1 15); do adb shell input swipe 540 1800 540 400 100; done
wait; adb pull /data/misc/perfetto-traces/phase-1d.pftrace build/
```
In the trace: every `cf.timeline.build` slice is on a `DefaultDispatch*` thread, none on the main thread; one `cf.timeline.firstFrame` async slice exists; `cf.timeline.replay` slices are on the main thread. Record each result in `docs/perf-timeline.md` "Phase 1d".

---

## 7. Acceptance criteria

1. `make quick` passes and runs the JVM tests `MinuteClockPolicyTest`, `DayClockProjectionTest`, `TimelineDrawingRepositoryTest`, `TimelineListIndexTest`, `PerfClockTest`, `FixtureTimelineSourceTest` and the `@Logic` Robolectric tests `SystemPlanClockTest`, `TimelineProbeHarnessTest`, `DayTimelineTest`, `NowLineTest`, `TimelinePageTest`, `TimelineInvalidationTest` (each class listed in `build/test-results/quick/timings.json`, which reports `"overBudget": 0`); under 60 s warm.
2. `make targeted ONLY="feature:trip:TimelineInvalidationTest"` (module path without the leading colon, 0a `Scripts/targeted.py`) passes, every method < 5 s in `build/test-results/targeted/timings.json` (`"overBudget": 0`).
3. `make check` passes with `pixel` booted, including Lint (warnings as errors), Konsist (`:feature:trip` depends on no feature; `:core:plan` imports no Android), `testReleaseUnitTest` (no fixture code in release) and the `TimelinePageGuardTest` cases. If OQ-1d-3's fallback applied, `TimelineDrawInvalidationTest` is listed in `build/test-results/smoke/timings.json`.
4. `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -c core:testing` prints 0.
5. All eight fixtures open on `pixel` through the `adb shell am start … --es cf.trip-page <name>` command; each shows `trip.timeline.ready`; screenshots in `build/captures/phase-1d/<name>.png`; `adb logcat -d | grep -c StrictMode` prints 0 for each.
6. The Perfetto trace shows `cf.timeline.build` on worker threads only and one `cf.timeline.firstFrame` async slice.
7. `./gradlew :feature:trip:compileReleaseKotlin -PcomposeReports=true`: `DayTimeline`, `NowLine`, `DayHeaderLine` and `TimelineTargets` are `restartable skippable` with all parameters `stable` in `feature/trip/build/compose_compiler/*-composables.txt`. Quote the lines in `docs/perf-timeline.md` "Phase 1d".
8. Every plan.md 1d checkbox is ticked in its task's commit; every visible string exists in en and nl.
9. No performance number is claimed; 1e measures.
10. No `make captures` comparison: this phase builds no designed board state (`Plan`/`PlanScrolled` fidelity is 6b). The adb screenshots are evidence that each fixture renders, not board captures.

---

## 8. Review checklist (Opus reviewer)

**Invalidation**
- [ ] Grep `\.value\.value` and `clock.value`: read only inside `NowLine`. No inline lambda of `DayTimeline` reads the clock or `drawing.value` in composition (only `TimelineTargets` and the draw/pointer lambdas do).
- [ ] No day item reads `listState`, `layoutInfo` or `firstVisibleItemScrollOffset`. `currentDayIndex` sits in `derivedStateOf`; its consumers are the ViewModel and `DayMenu` only.
- [ ] `DayClocks.apply` writes only changed holders, and one write per tick per day (offset and text together).
- [ ] The static layer's modifier chain is remembered, so a recomposition with equal inputs does not invalidate its draw; the `drawing` state survives key changes (no placeholder flash on check-off).
- [ ] `SideEffect { probe?… }` is the first statement of each probed composable, including before `NowLine`'s early return.
- [ ] `TimelineInvalidationTest` asserts per-key deltas, not totals, and includes the positive control; it fails if `graphicsLayer()` is removed (try it once).

**Threads and allocation**
- [ ] In main code, `DayDrawingBuilder.build` is only called inside `TimelineDrawingRepository` on `buildDispatcher` (tests may call it to pre-warm a cache); `limitedParallelism(2)` is created once.
- [ ] `onDrawBehind` allocates nothing: no `Paint()`, `Path()`, `RectF()`, `listOf`, string templates, `forEach` on boxed lists, lambdas. Placeholder geometry is precomputed.
- [ ] No `TextMeasurer`, `drawText`, `saveLayer`, `BlurMaskFilter`, `Modifier.blur`, `onSizeChanged`, `onGloballyPositioned`, per-item `BoxWithConstraints`.
- [ ] Stale build results are dropped (key check before assigning); `CancellationException` is never swallowed (`runCatching`, `catch (e: Exception)`).
- [ ] Typefaces, fixtures and the copy catalog load off main; StrictMode is clean.

**Clock**
- [ ] Receiver: `RECEIVER_NOT_EXPORTED`, application context, four actions, unregistered in `awaitClose`; collection only under `repeatOnLifecycle(STARTED)`.
- [ ] The timer re-arms from wall time on every refresh; a `TIME_TICK` or `TIMER` in an already-emitted minute is dropped; no `System.currentTimeMillis` in `:core:plan`.

**Structure and contracts**
- [ ] Keys: `h:<dayId>` / `<dayId>` / `end`; content types `dayHeader` / `day` / `end`.
- [ ] Tags exactly: `trip.page` (content root), `trip.scroll` (the `LazyColumn`), `trip.timeline.preparing|ready`, `trip.block.<id>`, `trip.dayMenu`, `trip.dayMenu.day.<n>` (0-based), `trip.plan.end`, `trip.day.<dayId>`.
- [ ] Traces exactly: `cf.timeline.firstFrame` async (begin in ViewModel `init`, end two frames after the first replay, once), `cf.timeline.build` sync on the worker, `cf.timeline.replay` sync, `cf.timeline.placeholder` as a section.
- [ ] Target nodes: BLOCK has `Role.Button`, `onClick`, `testTag`; traversal indices now −1, blocks 0…n, dividers 10,000+j, ticks 20,000+k; no `clickable` on targets.
- [ ] Release has no `FixtureTimelineSource`, no `LargeTimelineFixtures`, no `:core:testing`; the counting probe is test-only.

**Tests and rules**
- [ ] Tests obey AGENTS.md "Tests": no `Thread.sleep`, no node loops or `printToLog`, `waitUntil…` for waits, each Robolectric test < 5 s, one tier annotation per class, no `Timeout` rule.
- [ ] No string literal reaches `Text`; every string is a resource present in en and nl (`plan_day_title_date` is untranslatable).
- [ ] No color or dp literal outside generated tokens (the menu-row step is `PlanMetrics.minTouchTarget`, the bottom padding `PlanMetrics.gutter`); the only literal is the 50 ms jump wait. `PlanMetrics.touch` (44) is not used anywhere in this phase.
- [ ] Insets come only from `LocalSafeInsets` (top bar `windowInsets`, list padding); `Scaffold.contentWindowInsets` is zero.
- [ ] `:core:testing` appears in `app/build.gradle.kts` only in test, debug and perf configurations, and the amended Konsist module-graph rule passes with its planted violation.
- [ ] Nothing out of scope was built (designed header, compact line, pills, list mode, sheet, states).

**Typical mistakes to look for:** reading `clock.value` inside `DayTimeline`'s `Box {}`; creating `Dispatchers.Default.limitedParallelism(2)` per call; keying `pointerInput` on a lambda; resetting `drawing` to null on key change; using `Trace.setCounter` for the placeholder; `stickyHeader` content reading the current day; computing `compact` from the content width instead of the window width; a `LaunchedEffect(key)` without `day`, so a check-off never rebuilds (the key's `revision` is the plan revision and does not change on a mark); `PreparedDay`/`DayLayout` missing from `stability_config.conf`, so the compiler report shows `unstable` parameters; `trip.page` put on the `LazyColumn` instead of `trip.scroll`; a non-suspend `metrics` factory that blocks a worker on font loading, or typefaces loaded on main before `setContent`; reading `plans/S<n>.json` instead of `LargeTimelineFixtures.detailBytes`; a fixed 36 dp current-day threshold instead of `36.sp`; constructing `SystemPlanClock` registering its receiver eagerly instead of on collection; `Guards.assertAll` with an empty heading set (fails in `assertHeadings`); `PlanMetrics.touch` (44) used as the minimum target instead of `minTouchTarget` (48); `TopAppBar`/`Scaffold` default window insets instead of `LocalSafeInsets`; `:core:testing` added to `:app` without amending the Konsist module-graph rule.

---

## 9. Open questions (proceed on the default; the orchestrator adds them to plan.md "Open questions for the user")

| # | Question | Default |
|---|---|---|
| OQ-1d-1 | iOS does not pin the full day header (it fades a separate compact line; `TripScrollTracker.updateChrome`, `04 §9.18` J rejects `stickyHeader`), while plan.md 1d says "a plain sticky day header" and the `Plan` board makes the header `position: sticky` | **`stickyHeader` with a 36 dp plain line**, as plan.md says. 6b decides the designed behaviour and may replace it; 1e's numbers are re-run there |
| OQ-1d-2 | `-cf.freezeAt` on iOS freezes the **launch** timeline at t ms (0e parses it so, 0…60000); iOS freezes the plan clock with `CF_PLAN_TEST_FREEZE_CLOCK=1` → `FixedPlanClock(now: Date())` (`App/AppTabs.swift:283`, `TripPageDestination.swift:71`), a seed missing from plan.md's seed list. plan.md 1d says `cf.freezeAt` freezes the plan clock; 0e OQ-0e-12 suggests `CF_PLAN_TEST_FREEZE_CLOCK` | **The perf entry accepts an ISO-8601 instant in `cf.freezeAt`** (plan wording; numeric values keep the launch meaning and are ignored here). `CF_PLAN_TEST_FREEZE_CLOCK=1` (iOS parity, freeze at launch time) is added with the trip-route seeds in 4e/6b, not here. The orchestrator adds it to plan.md "Android spec: seeds" |
| OQ-1d-13 | iOS names the trip scroll view `trip.scroll`; plan.md 1d and 1e use `trip.page` and fling it | **Both:** `trip.scroll` on the `LazyColumn` (iOS id, for 6b's ported UI tests), `trip.page` on the content root that holds only the list (1e's `By.res("trip.page")` flings land on the list) |
| OQ-1d-14 | Current-day threshold and header height: iOS `@ScaledMetric` 36 (grows with Dynamic Type) vs a fixed 36 dp | **`36.sp` → px** (font scale capped at 1.3), for both the threshold and the plain header's min height. Note the reference point differs from iOS: iOS compares the **row** top (its full day header) with the 36 pt compact overlay (`TripPageView.swift:187–191`), while 1d compares the **day item** below a 36 dp pinned header, i.e. a day becomes current when its plain header pins. This keeps the anchor equal to the pinned header's day; 6b re-derives it with the designed header and compact line. `headerScrolled` maps exactly (iOS anchor row `minY < 0` ⇔ Android day item offset < header height) |
| OQ-1d-3 | Robolectric may not show RenderNode layer isolation (software drawing re-runs layer content) | **Task 3 decides.** If its controls fail: composition assertions stay in Robolectric (`make quick`); the draw-count assertions move to `TimelineDrawInvalidationTest` in `feature/trip/src/androidTest` (`@Smoke`, `pixel`, `make check`). `make check`'s smoke run only runs `:app:connectedDebugAndroidTest`, so the same task adds `:feature:trip:connectedDebugAndroidTest` to its Gradle call (the annotation argument already applies) and `--results 'feature/trip/build/outputs/androidTest-results/connected/**/*.xml'` to its run-tier call; acceptance: `TimelineDrawInvalidationTest` is listed in `build/test-results/smoke/timings.json` |
| OQ-1d-4 | *(resolved)* Which tier annotation puts a Robolectric test in `make quick` | **`@Logic`** (0e T1, `net.chronoflow.core.testing.tiers`; `cf.tier=quick` = plain JVM + `@Logic`) |
| OQ-1d-5 | The 0e overlap guard flags the invisible timeline targets (≥ 48 dp hit frames overlap around small tiles; iOS's 44 pt frames overlap too) | **Exempt the fixture's `trip.block.<id>` tags from the overlap rule only** (0e `exemptTags`); touch-target and safe-area rules still apply to them |
| OQ-1d-6 | A day whose drawing build throws: iOS falls back to the list view (`renderFailed`), which does not exist before 6c | **Keep the placeholder** and log in debug; 6c adds the list fallback |
| OQ-1d-7 | "Registered only while the app is started": process lifecycle (`ProcessLifecycleOwner`) or activity lifecycle | **Activity `repeatOnLifecycle(STARTED)`.** One activity, so the activity lifecycle is enough; the process lifecycle belongs to A7's sync triggers (4e), whose App Startup initializer A21 allows |
| OQ-1d-8 | iOS joins `title + " · " + date` in code; AGENTS forbids literals in `Text` | **Untranslatable resource `plan_day_title_date` = `%1$s · %2$s`** in `:core:ui` |
| OQ-1d-9 | large60 runs from `2026-10-13T05:00:00Z` (first day start) to `2026-12-08T11:00:00Z` (last day `day-29871120-tour-9` end; 1b Task 10 table). Unfrozen (1e OQ-1e-7), the page opens on the day containing now, and after the trip ends on its last day, where 15 flings cannot advance | **No change in 1d.** Flag to 1e: from `2026-12-08T11:00Z`, pass `--es cf.freezeAt 2026-11-15T08:00:00Z` or the scroll test's day-progress check fails |
| OQ-1d-10 | iOS marks the current day with a checkmark in the menu | **`selected` semantics only** until 6b (`PlanDayMenu`); no icon set exists before Phase 2 |
| OQ-1d-11 | plan.md lists `ACTION_TIME_TICK` and a wall-aligned ticker; both fire at the minute | **Both, with a same-minute `TIME_TICK` or `TIMER` dropped** (`MinuteClockPolicy.shouldEmit`) |
| OQ-1d-15 | plan.md 1d calls `cf.timeline.placeholder` "the counter"; 1e's `TraceSectionMetric(…, Mode.Count)` counts slices and reads a counter as 0 (1e Task 1 item 3) | **A synchronous section around each placeholder draw** (rule 7). The orchestrator rewords plan.md 1d to "the section `cf.timeline.placeholder`" |
| OQ-1d-12 | plan.md says "the debug implementation counts"; iOS keeps probes in the test target ("No Release counters") | **Counting probe in `src/test`** (shared by debug and release unit tests); production holds only the interface and a null local |

---

## Appendix A: research quoted (`docs/research/04-timeline.md`)

**§10.2 Layer 2: invalidation (Robolectric, native graphics)**
> A production `staticCompositionLocalOf<TimelineProbe?> { null }` counts compositions and **static draw executions** per day key. Over all 60 days, in Timeline and List modes, assert:
>
> | Event | Static recompositions | Static re-draws | `NowLine` recompositions |
> |---|---|---|---|
> | 120 scroll offsets + one current-day boundary | 0 | 0 (days newly entering may draw once) | 0 |
> | one minute tick | 0 | 0 | 1 |
> | a day boundary tick | 0 | 0 | 2 |
> | a check-off on day k | only day k | only day k | 0 |
> | positive control: anchor change | observed by subtitle, pills, compact header | – | – |

**§6 Minute tick and now-line update model [F]**
> `SystemPlanClock` yields `now`, then sleeps `60 − (epoch % 60)` s, so ticks land on minute boundaries; also yields on `NSSystemTimeZoneDidChange`, `NSCalendarDayChanged`, `willEnterForeground` and `significantTimeChange`. … only days containing now get the real `now`; the others get `distantPast` and ""; values are assigned only when they change. `DayClockContent` is the **only** reader of the clock.

**§9.6 Drawing a day** (sketch; names updated to 1c's)
> `Spacer(Modifier.matchParentSize().graphicsLayer().drawWithCache { val res = …; onDrawBehind { probe?.drew("static", day.id); val d = drawing.value; drawIntoCanvas { c -> if (d == null) drawPlaceholder(…) else d.replay(…) } } }.pointerInput(day.id) { detectTapGestures { p -> drawing.value?.hits?.resolve(p)?.let(onBlock) } })`, then `NowLine(clock)` ("the only clock reader") and `TimelineTargets(drawing)` ("semantics-only nodes, recomposes once when ready").
> Placeholder: frequency is a tracked counter `cf.timeline.placeholder`, target 0 during scripted flings on a Pixel 8. Jumps wait up to 50 ms for the target day's drawing before scrolling.

**§9.7 Now line: isolated invalidation**
> Gotcha: read the state **inside `NowLine`**, never inside an inline lambda of `DayTimeline` such as `Box { … }` content. Inline lambdas belong to the caller's recompose scope, so the whole day would recompose. … Other days get `null`. Crossing a day boundary writes two holders (iOS gate "2").

**§9.8 Scroll-dependent values**
> Current day (last day whose top ≤ 36 dp) changes at boundaries; read through `derivedStateOf { index.currentDay(listState.layoutInfo) }`. No `onScroll`-style listeners that write state per frame.

**§9.9 Hit testing and semantics overlay**
> One `pointerInput` per day, resolved against `DayDrawing.hits` with the iOS order. `TimelineTargets` emits one invisible `Layout` child per target with only `Modifier.semantics` (no `clickable`, no indication). Always composed by default (OQ-T25).

**§5 Android mapping**
> Targets: one invisible node each, with `Role.Button`, `contentDescription`, an `onClick` action, `testTag("trip.block.<id>")`, and bounds = hit frame with a **48 dp** minimum. Order with `traversalIndex`: now = −1; blocks = 0…n; dividers = 10,000 + j; ticks = 20,000 + k. Sighted taps never go through these nodes.

**§9.15 Clock (Android)**
> `callbackFlow` with a receiver for `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `ACTION_DATE_CHANGED` (`RECEIVER_NOT_EXPORTED`) and a ticker `delay(60_000 − now % 60_000 + 15)`; `.conflate()`. Collect with `flowWithLifecycle(STARTED)`. A restart re-emits, matching the iOS "enter foreground" refresh.

**§4.1–§4.3** (opening and jumps)
> `openingDay`, in order: the requested day; the requested block's day; the kept day; the day containing now; the last day, if the trip is past; the first day. On appear, the page scrolls to the anchor day aligned `.top`, without animation. … `select(dayID, offset)` changes the observable `anchor` only when the day changes. … The day menu, the pills and previous/next all jump to a day's top. The jump is animated unless Reduce Motion is on.

## Appendix B: iOS references quoted (at `f391a1fa`)

`PlanUI/Models/SystemPlanClock.swift` (the driver you port):
```swift
func refresh() {
    guard !stopped else { return }
    timer?.cancel()
    let now = Date()
    continuation.yield(now)
    let remainder = now.timeIntervalSince1970.truncatingRemainder(dividingBy: 60)
    timer = Task { [weak self] in
        do { try await Task.sleep(for: .seconds(60 - remainder)) } catch { return }
        await self?.refresh()
    }
}
// observers: NSSystemTimeZoneDidChange, NSCalendarDayChanged, willEnterForeground, significantTimeChange → refresh()
```

`PlanUI/Models/TripPlanModel.swift:51–56`:
```swift
private func updateDayClocks() {
    for (id, clock) in dayClocks {
        let active = layouts[id].flatMap { nowLineOffset($0.day, now: now) } != nil
        clock.update(now: active ? now : .distantPast, text: active ? nowTexts[id] ?? "" : "")
    }
}
```

`PlanUI/TripPage/DayTimelineView.swift:102–113` (tap order):
```swift
let candidates = geometry.tiles.filter { $0.hitFrame.contains(point) }
if let tile = candidates.min(by: { abs($0.frame.midY - point.y) < abs($1.frame.midY - point.y) }) { onBlock(tile.blockID) }
else if let marker = layout.markers.first(where: { abs($0.top - point.y) <= PlanMetrics.touch / 2 && point.x <= PlanMetrics.gutterWidth }) { onBlock(marker.block.id) }
else if let zone = layout.changes.first(where: { abs($0.top - point.y) <= PlanMetrics.touch / 2 }) { onBlock(zone.block.id) }
```
followed by `.accessibilityLabel(copy("Plan timeline"))`, `.accessibilityIdentifier(drawing == nil ? "trip.timeline.preparing" : "trip.timeline.ready")` and children: "Now, %@" at the now offset, one Button per block (`trip.block.<id>`), divider labels, visible tick labels.

`Tests/PlanUITests/TimelineInvalidationTests.swift` (flow you port): clock at `days[0].startUtc`; observe every row's `body` and `clockContent.body`; 120 offsets via `tracker.updateChrome` + `model.select(dayID: first.id, offset:)`, then `model.select(dayID: days[1].id)` → `control.ids("anchor") == [first.id]`, `static` and `clock` empty; `clock.advance(start + 60)` → `static` empty, `clock == [first.id]`; `clock.advance(days[1].startUtc)` → `static` empty, `clock == [first.id, next.id]`. Run for `timeline` in `[true, false]`.

`Docs/perf-timeline.md` "Four guarantee layers", item 3:
> `TimelineInvalidationTests` registers Observation probes around the actual `TripDayRow.body` and its actual clock child for all 60 days, in Timeline and List modes. 120 offset changes and an anchor boundary must invalidate **zero** static rows or clock children … One minute tick must invalidate **zero** static rows and **one** clock child; crossing a day boundary must invalidate **zero** static rows and **two** clock children. The positive control observes the anchor change. … All counters/probes live in the test target, with no Release instrumentation.
