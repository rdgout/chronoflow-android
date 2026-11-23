# Phase 1e brief: Timeline Macrobenchmarks, gates and baseline profile

- **Branch:** `feat/phase-1e-timeline-perf`, from the stack top once 1d is in the stack.
- **Depends on:** 1d (and through it 0a–0e, 1a–1c). **Wave:** 7. **Milestone:** closes M1. No trip-page phase (6b onward) starts before this phase records its decision.
- **Plan entry:** `plan.md` "### Phase 1e: Timeline Macrobenchmarks, gates and baseline profile". This brief expands it. Where they differ, plan.md wins; report the difference.
- **Boards:** none. This phase adds no visible UI, no copy and no tokens. The two invisible marker nodes of Task 1 are test hooks, not UI.
- **API endpoints:** none. Every run uses the synthetic `large60` fixture through 1d's `FixtureTimelineSource`.
- **Tests rules:** AGENTS.md "Tests" applies to every test you write. Read it before Task 1.
- **Brief revised:** 2026-11-16, against the revised 0e and 1d briefs and iOS at `f391a1fa`. Changes from the first version: the test paths follow OQ-0e-16; budgets use 0e's schema; Task 1 shrinks to the presented marker, because 1d now ships `trip.day.<id>`, the placeholder section and the synchronous build section; perf code follows 1d's `src/perf` + `src/release` stub pattern; `StartupBenchmark` moves to `Require`; perf runs freeze the clock (OQ-1e-7). Checker pass (2026-11-16): the presented marker takes `State<Boolean>` and is 1 dp; an async-slice [verify] with a fallback; `PERF_GRADLE_ARGS` for R4; R5 for an attached device; a timeout for profile generation; OQ-1e-13/14; `PictureReplayParityTest` is `@Layout`; plan Risks numbers corrected.

Tags: **[F]** is a fact verified in source (path and line given). **[R]** is a decision of this brief or of the plan. **[verify]** marks a library detail you confirm on the first build or run. When it turns out different, follow the stated fallback and record what you found in `docs/perf-timeline.md` "Tooling notes".

---

## 1. Goal

Prove with numbers that timeline design B (plan A12) meets the timeline rows of plan.md "Performance budget" on the `pixel` emulator. Design B is a `LazyColumn` with one item per day; each day's `DayDrawing` op list is built off the main thread and replayed in `drawWithCache` inside its own graphics layer. Then record whether B stands.

Deliverables:
1. `TimelineBenchmark` with `timelineFirstFrame`, `timelineScroll` (exactly 15 flings × 3 iterations on large60) and `timelineLastDay`. They use frame, memory, trace-section and placeholder metrics, plus a custom `TimelineTraceMetric`.
2. The timeline budgets in `benchmark/budgets.json` and the timeline mode of `Scripts/check-benchmarks.py`, for the emulator, reference and low-end classes.
3. `make perf-timeline` and `make baseline-profile`.
4. The committed `baseline-prof.txt` and `startup-prof.txt`, with every benchmark on `BaselineProfileMode.Require`.
5. JankStats in the debug and perf variants only.
6. The first runs, the Perfetto review and the compiler-report review in `docs/perf-timeline.md`. The 1c JVM limits are re-set from measured numbers.
7. The decision on design B, in `docs/perf-timeline.md` and in plan.md A12.

---

## 2. Preconditions (check first; do not rebuild earlier phases)

If an item is missing, **stop and report it to the orchestrator**. Use the names you find. The ones below are the 0e and 1d brief names.

| # | Must exist | Where | How to check |
|---|---|---|---|
| P1 | `:benchmark` (`com.android.test` + `androidx.baselineprofile` 1.5.0, package `net.chronoflow.benchmark`) with `StartupBenchmark` (`@Perf`, `targetAppId` argument, `Partial(UseIfAvailable)`); perf variants `benchmarkRelease`/`nonMinifiedRelease` compiling `app/src/perf/kotlin` (or the OQ-A28 fallback, recorded in `docs/tooling.md`) | 0e T15, T16 | `ls benchmark/src/main/kotlin/net/chronoflow/benchmark/StartupBenchmark.kt`; `grep -n "OQ-A28" docs/tooling.md` |
| P2 | `Scripts/check-benchmarks.py` with `--suite startup\|timeline`, `RAW_KEYS`, `jank_percent`, `hitch_ms_per_s`; `benchmark/budgets.json` (`"schema": 1`, a `timeline` array holding the first-frame row); `Scripts/tests/test_check_benchmarks.py`; fixture `Scripts/fixtures/benchmarks/timeline/first-frame-810/` | 0e T14 | `ls Scripts/check-benchmarks.py Scripts/tests/test_check_benchmarks.py Scripts/fixtures/benchmarks/timeline/first-frame-810` |
| P3 | `Scripts/perf-startup.sh` + `Scripts/tests/test_perf_startup.py` (dry-run with stubs); `Scripts/emulator.sh prep-perf\|prep-functional`; `ensure` writes `build/emulator/gpu-mode`; `Makefile` stubs `perf-timeline` and `baseline-profile` print "Phase 1e" and exit 1 | 0a, 0e T16 | `grep -n "perf-timeline\|baseline-profile" Makefile` |
| P4 | Tier annotations in `net.chronoflow.core.testing.tiers`: `@Logic` (Robolectric in quick), `@Layout`, `@Perf` (benchmark classes), `@PerfBudget`; `TierRegistryTest` | 0e T1 | `ls core/testing/src/main/kotlin/net/chronoflow/core/testing/tiers/` |
| P5 | Perf entry: `app/src/perf/kotlin/net/chronoflow/app/perf/TripPageEntry.kt` + `FixtureTimelineSource.kt`, a release stub `app/src/release/kotlin/net/chronoflow/app/perf/TripPageEntry.kt`; `src/perf/kotlin` compiled into `debug` too; `cf.trip-page=large60` opens `TimelinePage`; `cf.freezeAt=<ISO instant>` gives `FixedPlanClock` | 1d Task 8 | `grep -rn "cf.trip-page\|perfClockInstant" app/src` |
| P6 | Traces with exact names: `cf.timeline.firstFrame` (async, cookie 0, begun first in `TimelineHostViewModel.init`, ended by `FirstTimelineFrameTrace` two nested `postFrameCallback`s after the first replay); `cf.timeline.build` (**sync** slice on the worker); `cf.timeline.replay` (sync, in draw); `cf.timeline.placeholder` (a sync **section** around each placeholder draw, not a counter) | 1d rule 7, Tasks 4–6 | `grep -rn "\"cf\.timeline\." feature app` |
| P7 | Test tags: `trip.page` (content root holding only the list), `trip.scroll` (the `LazyColumn`), `trip.timeline.preparing`/`trip.timeline.ready` (per day), `trip.day.<dayId>` (day item root), `trip.dayMenu` (anchor), `trip.dayMenu.day.<n>` (0-based), `trip.plan.end`; root `testTagsAsResourceId = true` | 1d rule 8 | `grep -rn "\"trip\.\(page\|scroll\|day\|dayMenu\|plan.end\|timeline\)" feature app` |
| P8 | `TimelineProbe`/`LocalTimelineProbe`, `TimelineInvalidationTest` (`@Logic`) and the shared `TimelineTestHost` helper (test source set of `:feature:trip`) | 1d Tasks 3, 7 | `grep -rln "TimelineTestHost" feature/trip/src/test` |
| P9 | `TimelinePreparationBudgetTest` (prints `Timeline host: <name> mean=… max=…`), `DayDrawingBudgetTest` (prints `DayDrawing metric: W=<w> …`), both `@TimingGate` (0e OQ-0e-22, run serially by `make timing`); `docs/perf-timeline.md` with `# Timeline performance`, `## JVM budgets (1b, 1c), in quick` and a "Phase 1d" section | 1b, 1c, 1d | `grep -n "^#" docs/perf-timeline.md` |

Paths below assume `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/timeline/` (package `net.chronoflow.feature.trip.timeline`, host classes in `.host`) and `benchmark/src/main/kotlin/net/chronoflow/benchmark/`.

---

## 3. Inputs to read

**This repo**
- `plan.md`: A1 (versions), A12, A19, A21, "Performance budget", "Risks" 1, 2, 10 (baseline-profile plugin wiring) and 15 (stale baseline profiles), OQ-A22, OQ-A28, OQ-A29, and the Phase 1b, 1c, 1d, 1e and 13 entries.
- `AGENTS.md`: "Performance", "Tests", "Commands", "Traps" (animator scales, `src/perf`, release ships no perf code, configuration cache).
- `docs/phases/phase-0e-test-harness.md` T14–T16 (the shapes you extend); `docs/phases/phase-1d-timeline-host.md` §5 rules 7–12, Tasks 6–8 (the host you measure).
- `docs/research/04-timeline.md` §7 (iOS numbers), §9.17 (memory), §9.18 (alternatives B′/C/D), §10.3–§10.5, §11 (profiling protocol), §14 OQ-T15/T16/T25/T27, §15 (risks 1, 6, 7, 8, 14, 16).
- `docs/research/13-testing-tooling.md` §4.5–§4.6; `docs/research/11-android-stack.md` §7; `docs/research/12-built-apps.md` §4.2.
- `docs/tooling.md` (sandbox capabilities, perf variants, timeouts).

**iOS** (read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)
- `PerfUITests/TimelinePerfTests.swift`: the behaviour that `TimelineBenchmark` mirrors.
- `App/Debug/TripPageDiagnosticRoot.swift`: the first-frame probe and the presented marker.
- `Packages/ChronoFlowKit/Tests/PlanUITests/TimelineBudgetMetric.swift`: the gate semantics that the checker mirrors.
- `Docs/perf-timeline.md`: "Four guarantee layers", "Why the old end-to-end swipe test timed out" and "Optimized hosted UI run — user runs this".
- `plan.md:1730` "15 performance enforcement (worktree B, 2026-11-08)" and `plan.md:1741` "15 performance runner repair (worktree B, 2026-11-08)".
- `plan.md:1020` "Phase 7: Performance and comparison with RN (`feat/phase-7-performance`)": the device-run protocol only. The RN comparison is Android Phase 13.
- `plan.md:2286` "Performance budget": the launch rows, for context only.

### 3.1 Facts carried over from iOS [F]

- **Scroll workload** (`TimelinePerfTests.swift:29–65`): 3 iterations, each with a fresh `app.launch()`. Readiness, resolving the scroll frame and reading the start day (`trip.dayMenu` value) all happen **before** `startMeasuring()`. The measured block is exactly `for _ in 0..<15` fast vertical drags at x = 8 % of the scroll width, from 85 % to 15 % of its height. There are no UI queries inside the loop. After `stopMeasuring()` the test asserts that the day changed, logs `Timeline scroll progress after 15 swipes: <start> -> <end>`, then terminates the app.
- **Why** (`Docs/perf-timeline.md` "Why the old…", iOS `plan.md:1732`): the old 240-gesture traversal ran past 600 s, at about 2.7 s per gesture and about 689 s for one iteration. **Never write a traversal of hundreds of swipes.**
- **Last day** (`TimelinePerfTests.swift:67–98`, unmeasured): tap `trip.dayMenu`; scroll the **menu's** collection at most **12** times until the last entry is hittable; tap it; wait up to 10 s for the selection; then reach `trip.plan.end` in at most **6** swipes of the page. iOS finds the entry by its label `Day 2.0 · Tue 8 Dec`. Android has `trip.dayMenu.day.59` instead.
- **Readiness** (`TimelinePerfTests.swift:100–104`): wait for `trip.timeline.ready`, then `trip.timeline.presented`, 10 s each.
- **Presented marker** (`TripPageDiagnosticRoot.swift:26–28, 75–103`): the root's identifier is `trip.timeline.opening` until the probe finishes, then `trip.timeline.presented`. The probe ends the `time-to-first-timeline-frame` interval on the **second** display-link callback after the first rendered timeline frame, with the launch overlay gone.
- **Opening day of the perf page** (`TripPageDiagnosticRoot.swift:53–55`): `TripPageModel(…, dayID: detail.plan.days.first?.id)`. The iOS perf page always opens on **day 0**, whatever the clock says.
- **Language:** launch arguments `-AppleLanguages (en) -AppleLocale en_GB` (`TimelinePerfTests.swift:108`).
- **Per-test allowance:** `executionTimeAllowance = 300` s (`TimelinePerfTests.swift:7`).
- **First-frame iterations on iOS:** 3 (`TimelinePerfTests.swift:13`). The plan says 10 (OQ-1e-1).
- **Gates** (`TimelineBudgetMetric.swift:7–13, 41–56`): first frame ≤ 0.8 s, hitch ratio ≤ 5 ms/s, peak physical memory ≤ 150 × 1024 × 1024 B. A missing measurement fails, and so do a non-finite value, a negative value (except memory) and an unknown unit. The one exception: on the simulator a missing hitch measurement passes (l.44–47, "The simulator has no display hitch pipeline"); this is why Android's emulator class only reports jank and hitch. An expected-failure test injects 0.81 s (`TimelineMetricBudgetTests.swift:77`). **No baseline file decides pass or fail** (iOS `plan.md:1733`).
- **iOS numbers** (simulator; not device claims): first frame 0.34–0.41 s, and 0.528/0.525/0.536 s in the supplied pre-change run; peak memory 57.9 MB; drawing peak 39,081 B; host preparation means S3 19, large30 50, large60 92 ms (`04 §7.3`).
- **large60** (1b brief, golden [G]):
  - It runs from `2026-10-13T05:00:00Z` (`day-29864460-tour-0`) to `2026-12-08T11:00:00Z`; the last day is `day-29871120-tour-9`.
  - The cycle is 24, 24, 13.5, 25.5, 24 and 24 h. Grids total 54,000 dp.
  - Its lanes fit at 411 dp, so there is no horizontal overflow on `pixel`.

---

## 4. Out of scope

- Any visible UI: the designed header, pills, summary, list mode (6c), the step sheet (6d) and the wide layout (6e). There is no board for this phase.
- The trip journey in the baseline profile (My trips → S3 → list toggle → step sheet, `04 §10.5`). Those screens do not exist yet; Phase 13 adds that journey.
- Startup budgets, `LaunchJankBenchmark`, warm start and the RN comparison (3a, 3b, 13). `StartupBenchmark` changes only its compilation mode (Task 6).
- Fallbacks C (strips) and D (custom `View`). Only B′ (`Picture`) may be spiked here, and only on a missed gate (Task 10).
- Microbenchmark modules, Gradle Managed Devices, T2 AVDs and `make matrix`.
- Changing any budget value in plan.md "Performance budget". If you think a budget is wrong, raise it as an open question.

---

## 5. Budgets this phase enforces (from plan.md "Performance budget")

Device classes:
- `emulator`: the `pixel` AVD, `-gpu host`, animator scale 1, 60 Hz (`hw.lcd.vsync=60`);
- `reference`: Pixel 8 class, 120 Hz;
- `lowEnd`: Pixel 4a or Galaxy A15/A16 class, 60 Hz.

| Checker metric id | Test | Source | reference hard / aim | lowEnd hard / aim | emulator |
|---|---|---|---|---|---|
| `timelineFirstFrameMs` (each of 10) | firstFrame | `TraceSectionMetric("cf.timeline.firstFrame", Mode.First, label = "timelineFirstFrame")` | ≤ 800 / ≤ 450 | ≤ 1500 / ≤ 800 | **≤ 800 (gate)** |
| `frameDurationCpuMs` P50 / P90 / P99 | scroll | `FrameTimingMetric` | aim ≤ 3 / 5 / 8 | aim ≤ 6 / 10 / 16 | P50: report + trend; P90/P99: **CPU band (gate)** |
| `frameOverrunMs` P99 (signed) | scroll | same | ≤ 2 / ≤ 0 | ≤ 4 / ≤ 0 | report |
| `jankPercent` (each of 3) | scroll | derived: 100 × count(overrun > 0) ÷ frames | ≤ 3 / ≤ 1 | ≤ 5 / ≤ 3 | report |
| `hitchRatioMsPerS` (each) | scroll | derived: Σ max(0, overrun) ÷ (`timelineScrollWindowMs` / 1000) | ≤ 5 / ≤ 1 | ≤ 5 / ≤ 2 | report |
| `timelinePlaceholderCount` (each) | scroll | `TraceSectionMetric("cf.timeline.placeholder", Mode.Count, label = "timelinePlaceholder")` | ≤ 2 / 0 | ≤ 6 / ≤ 2 | **≤ 6 (gate)** |
| `timelineMainFrameMsP50` / `timelineMainFrameMsP99` | scroll | `TimelineTraceMetric` | aim ≤ 2 / ≤ 4 | same | report |
| `timelineRenderThreadMsP99` | scroll | `TimelineTraceMetric` | aim ≤ 4 | same | report |
| `timelineTargetsMaxMs` | scroll | `TraceSectionMetric("cf.timeline.targets", Mode.Max, label = "timelineTargets")` | report | aim ≤ 1 (OQ-A46 trigger) | report |
| `timelineDecodeMs`, `timelinePrepareMs`, `timelineTypefaceMs` | firstFrame | `TimelineTraceMetric(firstFrame = true)` | report | report | report |
| `memoryRssAnonMaxKb` (each) | scroll | `MemoryUsageMetric(Mode.Max)` | ≤ 153,600 / ≤ 81,920 | ≤ 153,600 / ≤ 102,400 | **≤ 153,600 (gate)** |
| `memoryRssAnonGrowthKb` | scroll | derived: `runs[-1] − runs[0]` | warn > 40,960 | same | warn |
| `timelineBuildOnMainCount` (each) | scroll | `TimelineTraceMetric` | **= 0 (all classes; OQ-1e-13)** | same | **= 0** |
| `timelineScroll` present | scroll | absent when the day-advanced `check` failed | functional | same | functional |
| `timelineLastDay` present | lastDay | absent when the ≤ 6-fling `check` failed | functional | same | functional |

**Provenance [R].** Every row comes from plan.md "Performance budget" and "Where gates apply", as amended by the 2026-11-16 review (the build-on-main row, the main-thread, RenderThread and `TimelineTargets` rows, the first-frame breakdown, the emulator placeholder gate and the CPU band). Background on two of them:
- `timelineBuildOnMainCount` = 0 turns the plan's 1e "Run and review" check ("builds on worker threads only") and 1d's rule 7 into a gate, because a build on main is a contract break, not a performance number (OQ-1e-13).
- The emulator RSS gate follows the plan's 1e Budgets task, the budget table's emulator cell "≤ 150 MiB" and `13 §4.5` ("Enforce the first-frame and memory gates"); "Where gates apply" now names it too.

**Report-only values** (no budget, but each must be present): `timelineScrollWindowMs`, `timelineBuildCount`, `timelineRecomposeCount`, `timelineGcCount`, `timelineReplayMaxMs`, `timelineReplayCount`, `frameCount`, `memoryHeapSizeMaxKb` (scroll) and `timeToInitialDisplayMs`, `timelineDecodeMs`, `timelinePrepareMs`, `timelineTypefaceMs` (firstFrame).

**Emulator rule** (plan "Where gates apply"):
- Only the bold gates, the CPU band and the functional rows fail on the emulator. Every other value prints `REPORT`.
- **Baseline:** per value, the median of the last three `PASS` rows with the same Class, Theme, Font and Page in `docs/perf-timeline.md` (fewer rows: their median; none: no band and no trend).
- **CPU band:** `frameDurationCpuMs` P90 or P99 above max(1.25 × baseline, baseline + 1 ms) prints `BAND`. The checker exits 4 when band misses are its only failures (any other failure exits 1), and `make perf-timeline` re-runs the configuration once on exit 4 and keeps the second result.
- **Trend:** a value that moves by more than ±25 % against its baseline prints `FLAGGED` and never fails; a counter whose baseline is 0 is flagged on an absolute rise instead (placeholders +2, GC +1).
- Emulator numbers are never device claims. Device rows stay "open: device only (OQ-A22)"; the first production upload needs one reference and one low-end run, or the user's waiver (OQ-A45).

---

## 6. Tasks

Each task is one atomic Conventional Commit with no trailer. Tick the plan.md 1e checkbox named under "Ticks" in that same commit.

Run only what you touched (`make targeted ONLY=…`), and run `make quick` once before you hand back. If your sandbox cannot run Gradle, `adb` or the emulator, write the code, then report the exact command and its exact output; the orchestrator runs it.

**Never type an unmeasured number into a doc. Write `pending` instead.**

### Task 1: presented marker

**Commit:** `feat(trip): mark the first presented timeline frame`. **Ticks:** none.

- **`FirstTimelineFrameTrace`** (1d, `…/timeline/host/FirstTimelineFrameTrace.kt`): add `val presented: State<Boolean>`, backed by a private `mutableStateOf(false)`.
  - Set it to `true` on the main thread, in the same callback, immediately after `Trace.endAsyncSection("cf.timeline.firstFrame", 0)`.
  - It flips once and never back. A placeholder draw never reaches it, because 1d calls `onReplay()` only after a real replay.
- **`…/timeline/TimelinePresentedMarker.kt`**: `@Composable internal fun TimelinePresentedMarker(presented: State<Boolean>, modifier: Modifier = Modifier)` = `Box(modifier.size(1.dp).testTag(if (presented.value) "trip.timeline.presented" else "trip.timeline.opening"))` (iOS ids, `TripPageDiagnosticRoot.swift:27`).
  - The parameter is the `State`, not the `FirstTimelineFrameTrace`: `State` is stable, so the Task 7 compiler line can read "every parameter stable". A plain class parameter would report `unstable`.
  - The state is read only inside this composable, so only this composable recomposes when it flips.
  - The size is 1 dp, not 0: UiAutomator skips nodes that are not visible to the user, and a zero-size node has empty bounds [verify on the first `pixel` run that `By.res("trip.timeline.opening")` is found right after launch]. Fallback if it is not found: move the tag to a `Box(Modifier.fillMaxSize().testTag(…))` that wraps the `trip.page` `Box` as its parent (never a sibling drawn over the list, which would cover the list's nodes for accessibility), and keep the list content in its own skippable composable so the flip recomposes only the wrapper. Record the outcome in "Tooling notes".
  - Put it in `TimelinePage`'s `trip.page` `Box`, as a sibling **after** the `LazyColumn`, with `Modifier.align(Alignment.TopStart)`, never inside a day item. It has no pointer input, so it takes no touches. Pass `viewModel.firstFrame.presented`: the trace instance 1d created.
- **Check the P6 and P7 names against 1d's contract.** Change nothing that matches. If one differs, fix it in this commit and say so in the commit body. Look in particular for a placeholder emitted with `Trace.setCounter`, or `cf.timeline.build` emitted async.
- **First-frame breakdown** (plan.md budget row; these two land in a separate commit, `perf(trip): trace the first-frame breakdown and the targets composition`): in 1d's host pipeline, for the first input only (a boolean guard in the ViewModel), wrap the decode (`decoder.detail(…)`) in the async section `cf.timeline.decode` (cookie 1), the layout and prepare (`preparer.layouts(…)` plus `preparer.prepare(…)`) in `cf.timeline.prepare` (cookie 2), and the first `metrics()` call in `cf.timeline.typeface` (cookie 3), with `Trace.beginAsyncSection`/`endAsyncSection` (use 1d's merged names). They are async because the pipeline suspends and changes threads; a sync section would mis-nest.
- **`cf.timeline.targets`** (plan.md OQ-A46): wrap the body of 1d's `TimelineTargets` composable in a sync section `cf.timeline.targets` (`androidx.tracing.trace { … }`, or `Trace.beginSection`/`endSection` in `try`/`finally`), so `timelineScroll` can report `timelineTargetsMaxMs`.

**Edge cases.**
- Activity recreation creates a new trace and a new marker. Only the cold path is measured.
- The marker must not change the invalidation counts. It lives outside every day item.

**Tests.**
- `TimelinePerfMarkersTest` (`@Logic`, Robolectric, `feature/trip/src/test/kotlin/net/chronoflow/feature/trip/timeline/`). Use `TimelineTestHost` with large60, a gated builder and `mainClock.autoAdvance = false`.
  1. Before the builder is released, `trip.timeline.opening` exists, `trip.timeline.presented` does not, the opening day's `trip.timeline.preparing` node exists, and the probe's STATIC draw count for the opening day is ≥ 1 (its placeholder drew).
  2. Release the builder, `waitForIdle()`, then call `mainClock.advanceTimeByFrame()` twice. Now `onNodeWithTag("trip.timeline.presented").assertExists()` passes, and `trip.timeline.opening` does not exist.
  3. `onNodeWithTag("trip.day.<openingDayId>").assertExists()`, with the id taken from the fixture (no loops over semantics nodes).

  [verify] whether Robolectric runs `Choreographer.postFrameCallback` on `advanceTimeByFrame`. If it does not, drive the main looper with `shadowOf(Looper.getMainLooper()).idleFor(16, MILLISECONDS)` twice instead, and record which one worked.
- Re-run `TimelineInvalidationTest` unchanged: scroll 0/0, minute 0/1, day boundary 0/2. Command: `make targeted ONLY=":feature:trip:TimelinePerfMarkersTest :feature:trip:TimelineInvalidationTest"`.

### Task 2: JankStats in debug and perf variants

**Commit:** `feat(app): track timeline jank with jankstats in debug and perf builds`. **Ticks:** "JankStats".

- **Dependency:** `androidx.metrics:metrics-performance:1.0.0` (0a catalog). Add it as `debugImplementation`, `"benchmarkReleaseImplementation"` and `"nonMinifiedReleaseImplementation"` (string notation, as 0e T15 did for `:core:data-mock`), or as `"benchmarkImplementation"` under the OQ-A28 fallback. Never `implementation` or `releaseImplementation`. Extend 0e's `ModuleGraphTest` file-text rule to cover `metrics-performance` the same way (method `metricsPerformanceOnlyInDebugAndPerf`; it matches lines naming the 0a catalog alias, e.g. `metrics.performance`, or the raw coordinate `metrics-performance`), with its own planted violation `konsist/src/test/resources/violations/moduleGraph/metricsPerformance/app/build.gradle.kts` holding `implementation(libs.androidx.metrics.performance)` (use the catalog alias 0a defined), and a `…RejectsPlantedViolation` test, as 0e did for `:core:data-mock`.
- **Seam** in `:feature:trip` main, file `…/timeline/TimelineJankTagger.kt`:
  ```kotlin
  fun interface TimelineJankTagger { fun onVisibleDay(index: Int, mode: String) }
  val LocalTimelineJankTagger = staticCompositionLocalOf<TimelineJankTagger?> { null }
  ```
  In `TimelinePage`'s Ready branch, when the local is non-null: `LaunchedEffect(viewModel, tagger) { viewModel.anchorDayId.filterNotNull().collect { id -> tagger.onVisibleDay(<0-based index of id in prepared.days>, "timeline") } }`.
  - `anchorDayId` is 1d's `StateFlow`, which changes only at day boundaries. There is no new scroll read.
  - Look the index up from a `Map<String, Int>` built once per model (`remember(model.prepared) { … }`), never with a search per call.
  - List mode reports `"list"` in 6c.
- **App side**, following 1d's `TripPageEntry` pattern:
  - `app/src/perf/kotlin/net/chronoflow/app/perf/JankStatsInstaller.kt`: `internal object JankStatsInstaller`, with:
    - `fun install(activity: ComponentActivity)`: `JankStats.createAndTrack(activity.window) { f -> if (f.isJank) Log.w("CfJank", f.toString()) }`. The string is built only for jank frames;
    - `fun tagger(activity: ComponentActivity) = TimelineJankTagger { i, mode -> PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView).state?.putState("timeline", "day=$i;mode=$mode") }`.
  - `app/src/perf/kotlin/net/chronoflow/app/perf/PerfJank.kt`: `object PerfJank { fun tagger(a: ComponentActivity): TimelineJankTagger? = JankStatsInstaller.tagger(a); fun install(a: ComponentActivity) = JankStatsInstaller.install(a) }`.
  - `app/src/release/kotlin/net/chronoflow/app/perf/PerfJank.kt`: the same signatures; `tagger` returns `null` and `install` does nothing.
  - `MainActivity.onCreate`:
    1. `val jankTagger = PerfJank.tagger(this)` before `setContent`;
    2. wrap the root content in `CompositionLocalProvider(LocalTimelineJankTagger provides jankTagger)`;
    3. call `PerfJank.install(this)` **after** `setContent`.

    Never call it from `Application.onCreate`.

**Tests.**
- `TimelineJankTaggerTest` (`@Logic`, Robolectric, `:feature:trip`). Use `TimelineTestHost` with large60, a recording fake tagger provided through the local, and the host's fixed clock.
  1. After the first frame there is exactly one call `(openingIndex, "timeline")`. `openingIndex` is the opening day's index as the host computes it.
  2. 120 `scrollBy(1f)` steps that stay inside that day add **0** calls, and the probe delta for the same steps stays 0 static / 0 clock.
  3. `scrollToItem(index.headerIndex(prepared.days[openingIndex + 3].id))` adds exactly one call, `(openingIndex + 3, "timeline")`. `prepared` is the fixture's prepared plan and `index` the host model's `TimelineListIndex`, both read from the ViewModel's `Ready` state, never from the semantics tree.
- `ReleaseReadinessTest` (`app/src/testRelease`): add that `Class.forName("androidx.metrics.performance.JankStats")` and `Class.forName("net.chronoflow.app.perf.JankStatsInstaller")` both throw `ClassNotFoundException`.

### Task 3: timeline budgets and the results checker

**Commit:** `feat(perf): check timeline benchmark budgets`. **Ticks:** none ("Budgets" is ticked in Task 5).

**`benchmark/budgets.json`**: keep 0e's schema (`"schema": 1`, `"labels"`, per-class objects with `hard`, `aim` and `report`). Append these rows to the existing `timeline` array, and add one `timelineReportOnly` object. Three keys are new: `"warn"` (prints `WARN` and never fails), the class `"all"` (it applies to every class) and `"band": true` (the emulator CPU band of plan.md "Where gates apply"). A top-level `"timelinePage": "host"` names the page under test for the history's Page column; 6b Task 13 sets it to `"trip"`.
```json
{"test": "timelineScroll", "metric": "frameDurationCpuMs", "stat": "P50", "unit": "ms", "reference": {"aim": 3}, "lowEnd": {"aim": 6}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "frameDurationCpuMs", "stat": "P90", "unit": "ms", "reference": {"aim": 5}, "lowEnd": {"aim": 10}, "emulator": {"band": true}},
{"test": "timelineScroll", "metric": "frameDurationCpuMs", "stat": "P99", "unit": "ms", "reference": {"aim": 8}, "lowEnd": {"aim": 16}, "emulator": {"band": true}},
{"test": "timelineScroll", "metric": "frameOverrunMs", "stat": "P99", "unit": "ms", "signed": true, "reference": {"hard": 2, "aim": 0}, "lowEnd": {"hard": 4, "aim": 0}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "jankPercent", "stat": "each", "unit": "%", "derived": true, "reference": {"hard": 3, "aim": 1}, "lowEnd": {"hard": 5, "aim": 3}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "hitchRatioMsPerS", "stat": "each", "unit": "ms/s", "derived": true, "reference": {"hard": 5, "aim": 1}, "lowEnd": {"hard": 5, "aim": 2}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "timelinePlaceholderCount", "stat": "each", "unit": "count", "reference": {"hard": 2, "aim": 0}, "lowEnd": {"hard": 6, "aim": 2}, "emulator": {"hard": 6}},
{"test": "timelineScroll", "metric": "timelineMainFrameMsP50", "stat": "each", "unit": "ms", "reference": {"aim": 2}, "lowEnd": {"aim": 2}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "timelineMainFrameMsP99", "stat": "each", "unit": "ms", "reference": {"aim": 4}, "lowEnd": {"aim": 4}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "timelineRenderThreadMsP99", "stat": "each", "unit": "ms", "reference": {"aim": 4}, "lowEnd": {"aim": 4}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "timelineTargetsMaxMs", "stat": "each", "unit": "ms", "reference": {"report": true}, "lowEnd": {"aim": 1}, "emulator": {"report": true}},
{"test": "timelineScroll", "metric": "memoryRssAnonMaxKb", "stat": "each", "unit": "KiB", "emulator": {"hard": 153600}, "reference": {"hard": 153600, "aim": 81920}, "lowEnd": {"hard": 153600, "aim": 102400}},
{"test": "timelineScroll", "metric": "memoryRssAnonGrowthKb", "stat": "single", "unit": "KiB", "derived": true, "all": {"warn": 40960}},
{"test": "timelineScroll", "metric": "timelineBuildOnMainCount", "stat": "each", "unit": "count", "all": {"hard": 0}},
{"test": "timelineLastDay", "metric": "frameCount", "stat": "present", "unit": "count"}
```
`"timelineReportOnly": {"timelineScroll": ["timelineScrollWindowMs", "timelineBuildCount", "timelineRecomposeCount", "timelineGcCount", "timelineReplayMaxMs", "timelineReplayCount", "frameCount", "memoryHeapSizeMaxKb"], "timelineFirstFrame": ["timeToInitialDisplayMs", "timelineDecodeMs", "timelinePrepareMs", "timelineTypefaceMs"]}`.

**`RAW_KEYS`** (one table in the checker). Expected keys with benchmark 1.5.0 [verify on the first run, then fix the table and the fixtures]:
- `timelineFirstFrameFirstMs → timelineFirstFrameMs` (0e put it there);
- `timelinePlaceholderCount`, `timelineReplayMaxMs`, `timelineReplayCount` and `timelineTargetsMaxMs` map to themselves;
- `TimelineTraceMetric` names map to themselves.

**`Scripts/check-benchmarks.py --suite timeline`** (stdlib only, `python3 -I`). Extend 0e's code; do not fork it.
- **Derived values per iteration `i` of `timelineScroll`:** use 0e's `jank_percent(frameOverrunMs.runs[i])` and `hitch_ms_per_s(frameOverrunMs.runs[i], timelineScrollWindowMs.runs[i])`, plus `memoryRssAnonGrowthKb = memoryRssAnonMaxKb.runs[-1] − runs[0]`. Keep iterations in the order they are written in `runs`.
- **Stats:** `each` checks every iteration and also prints the mean and max. `single` is one value. `present` means the benchmark entry exists and has the metric.
- **Fail (exit 1)** on any of these:
  - no result file;
  - one of `timelineFirstFrame`, `timelineScroll` or `timelineLastDay` absent;
  - a budgeted or report-only metric missing;
  - a non-finite value;
  - a negative duration or count (only `"signed": true` metrics may be negative);
  - a value over the `hard` limit of the selected class or of `all`.

  Usage errors exit 2. A missing `frameOverrunMs` says "needs API 31+" and still fails.
  A CPU band miss (emulator, the `"band": true` rows) exits **4** when it is the only kind of failure; with any other failure the exit is 1. Exit 3 is `run_tier.py`'s over-budget code, so the band uses 4.
- **Output:** `Timeline metric: <test> <metric> <stat>=<value> <unit> [hard=<h> aim=<a>] <PASS|FAIL|BAND|AIM-MISSED|WARN|REPORT>`.
- **History**, the `## Runs` table in `--history`, with this exact header:

  `| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Page | Build | First frame ms med/max | CPU P50/P90/P99 ms | Main/Render P99 ms | Overrun P99 ms | Jank % mean/max | Hitch ms/s mean/max | Placeholder max | Replay max ms | RSS anon MiB max | RSS growth MiB | Recompose max | GC max | Scroll | Last day | Result |`

  - Build is `benchmarkRelease, Partial(Require)`.
  - Scroll and Last day are `ok` when the test is present, else `missing`.
  - Result is `FAIL` if any value failed, `BAND` if only the CPU band missed, else `PASS`. Page is the `timelinePage` of `budgets.json`. Main/Render P99 is the max over iterations of `timelineMainFrameMsP99` and `timelineRenderThreadMsP99`.
  - The row is also written to `build/perf/timeline-row.md`. `--append` adds it as the table's last line, including a FAIL row. If the table is absent, `--append` creates it under `## Runs` and no trend is computed.
- **Baseline:** per value, the median of the last three `PASS` rows with the same Class, Theme, Font and Page (fewer rows: their median; none: no trend and no band).
- **Trend:** print `Timeline trend: <metric> <old> -> <new> (<±x%>) FLAGGED` when |Δ| > 25 % against the baseline. It covers the median first frame, CPU P50/P90/P99, the main-thread and RenderThread P99, max RSS, mean hitch and replay max. Placeholder max and GC max use absolute rises instead (+2 and +1 over the baseline), because their baseline is often 0. A trend never fails.
- **Band** (emulator only; the `"band": true` rows, CPU P90 and P99): print `Timeline band: <metric> <value> > <limit> (baseline <b>) BAND` when the value exceeds max(1.25 × baseline, baseline + 1 ms). Exit 4 when band misses are the only failures; any other failure exits 1.

**Tests**, in `Scripts/tests/test_check_benchmarks.py` (0e's file and loader). Fixtures go in `Scripts/fixtures/benchmarks/timeline/<name>/`; each holds one `*-benchmarkData.json` with all three tests. Keep every 0e test, including `first-frame-810`.
- `test_timeline_pass_emulator` (`pass/`): exit 0, and every metric of §5 printed once per value.
- `test_timeline_rss_over_fails` (`rss-153601/`): exit 1 naming `memoryRssAnonMaxKb`.
- `test_timeline_build_on_main_fails` (`build-on-main/`, one run = 1): exit 1 on every class.
- `test_timeline_missing_report_only_fails` (`no-scroll-window/`), `test_timeline_nan_fails`, `test_timeline_negative_count_fails`, `test_timeline_missing_last_day_fails`.
- `test_timeline_negative_overrun_allowed`: P99 = −2.1 passes `reference`.
- `test_timeline_device_gates` (`jank-4/`): the pass fixture with one iteration at jank 4 % (for example 4 of 100 frames at overrun 1 ms in a 1,667 ms window, so its hitch is 2.4 ms/s). With `--device-class reference` it exits 1 and prints a `FAIL` line for `jankPercent` (the `frameOverrunMs` P99 line may fail too; assert only the jank line). With `lowEnd` and `emulator` it exits 0 and prints no `FAIL` line. Build `pass/` so that every other value meets every `reference` and `lowEnd` hard limit.
- `test_timeline_growth_warns`: growth 41,000 KiB prints `WARN` and exits 0.
- `test_timeline_trend_and_append`: a history copy with three PASS rows (emulator, light, 1.0, Page `host`) whose first-frame medians are 380, 400 and 420 (baseline 400). A new median of 520 prints `FLAGGED` and exits 0, and `--append` adds exactly one 26-cell row.
- `test_timeline_cpu_band_exits_4`: three PASS rows with CPU P99 8, 9 and 10 ms (baseline 9, limit max(11.25, 10) = 11.25). P99 11.3 prints `BAND` and exits 4; 11.2 exits 0; 11.3 together with an RSS of 153,601 KiB exits 1.
- `test_timeline_band_needs_history_of_the_same_page`: the same rows with Page `trip` and a run of Page `host` → no band; P99 11.3 exits 0.
- `test_timeline_placeholder_gate_emulator`: one iteration with 7 placeholders exits 1 on `emulator`; 6 exits 0.
- `test_timeline_zero_baseline_counter_flags`: placeholder baseline 0 and a new max of 2 → `FLAGGED`; GC baseline 0 and a new max of 1 → `FLAGGED`; exit 0.

### Task 4: `TimelineTraceMetric`, journeys and `TimelineBenchmark`

**Commit:** `test(benchmark): add timeline macrobenchmarks`. **Ticks:** "TimelineBenchmark".

**`TimelineJourneys.kt`** (no tests in it, so no tier annotation):
- `const val FREEZE_AT = "2026-10-13T08:00:00Z"` (OQ-1e-7). The page opens on day 0 (`2026-10-13T05:00Z` → `2026-10-14T05:00Z`), as the iOS perf page does, and the now line sits 3 h × 40 dp/h = 120 dp below that day's grid top.
- `fun large60Intent(pkg: String) = Intent().setClassName(pkg, "net.chronoflow.app.MainActivity").putExtra("cf.trip-page", "large60").putExtra("cf.freezeAt", FREEZE_AT)`.
- `fun MacrobenchmarkScope.awaitPresented()`: `check(device.wait(Until.hasObject(By.res("trip.timeline.ready")), 10_000))`, then the same for `trip.timeline.presented`.
- `fun MacrobenchmarkScope.assertAnimatorsOn()`: `device.executeShellCommand("settings get global animator_duration_scale").trim()` must be `1` or `1.0`. Otherwise throw `IllegalStateException("Perf runs need animator_duration_scale = 1; run make perf-timeline")` (`04 §15` risk 8).
- `fun MacrobenchmarkScope.topDay(): String?` = `device.findObjects(By.res(Pattern.compile("trip\\.day\\..+"))).minByOrNull { it.visibleBounds.top }?.resourceName`. This is one query.
- `const val LAST_DAY_TAG = "trip.day.day-29871120-tour-9"` (1b golden).

**`TimelineTraceMetric.kt`**: `@OptIn(ExperimentalMetricApi::class) class TimelineTraceMetric(private val firstFrame: Boolean = false) : TraceMetric()`. Its `getMeasurements(captureInfo, traceSession)` [verify the abstract signature in 1.5.0] runs Perfetto SQL for `captureInfo.targetPackageName` and returns:
- `timelineScrollWindowMs`: (MAX(ts + dur) − MIN(ts)) / 1e6 over `Choreographer#doFrame%` slices on the app's main thread;
- `timelineBuildOnMainCount` and `timelineBuildCount`: `cf.timeline.build` slices on the main thread, and in total;
- `timelineRecomposeCount`: `Recomposer:recompose` slices on the main thread;
- `timelineGcCount`: slices on thread `HeapTaskDaemon` whose name is `LIKE '%GC%'` [verify the names in one trace].
- `timelineMainFrameMsP50` and `timelineMainFrameMsP99`: nearest-rank percentiles, computed in Kotlin, of the durations of the `Choreographer#doFrame%` slices on the app's main thread in the measured window, / 1e6;
- `timelineRenderThreadMsP99`: the same P99 over the `DrawFrame%` slices on the app's thread named `RenderThread` [verify the slice name in one trace];
- with `firstFrame = true`, only `timelineDecodeMs`, `timelinePrepareMs` and `timelineTypefaceMs`: the duration of the first slice named `cf.timeline.decode`, `cf.timeline.prepare` and `cf.timeline.typeface` on the app's process tracks (the process-track SQL of the async first-frame fallback below, with the name as a parameter), / 1e6. A missing slice gives no measurement, so the checker fails on it.

The main thread is `thread.is_main_thread = 1 OR thread.tid = process.pid`, joined `slice → thread_track → thread → process` with `process.name = '<pkg>'`. A count with no rows is 0. The window with no rows returns **no** measurement, so the checker fails on the missing value.

Fallback if `TraceMetric` cannot be subclassed in 1.5.0: `TraceSectionMetric(…, Mode.Count, targetPackageOnly = true)` for the totals, and `frameCount × 1000 / refreshHz` for the window. With this fallback build-on-main cannot be measured, so the window is computed by 0e's no-window path, and `timelineBuildOnMainCount` comes from the Task 7 SQL review of one trace per run. Record the fallback.

**`TimelineBenchmark.kt`** (`@Perf`, `@RunWith(AndroidJUnit4::class)`, `@get:Rule val rule = MacrobenchmarkRule()`):
- `packageName` comes from `InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app"`, as in `StartupBenchmark`.
- Every test uses `compilationMode = CompilationMode.Partial(BaselineProfileMode.Require)`.
- Metrics that need it get `@OptIn(ExperimentalMetricApi::class)`.
- **Async first-frame slice** [verify on the first run; `04 §15` risk 16]. `cf.timeline.firstFrame` is an async section (`Trace.beginAsyncSection`), which Perfetto puts on a process track, not a thread track. Check that `timelineFirstFrameFirstMs` appears with plausible values (hundreds of ms, never 0). Fallback if `TraceSectionMetric` misses process-track slices: add `timelineFirstFrameMs` to `TimelineTraceMetric`'s `firstFrame = true` mode (`SELECT dur FROM slice JOIN process_track ON slice.track_id = process_track.id JOIN process USING(upid) WHERE slice.name = 'cf.timeline.firstFrame' AND process.name = '<pkg>' ORDER BY ts LIMIT 1`, / 1e6), drop the `TraceSectionMetric` from `timelineFirstFrame`'s metrics, map the key to itself in `RAW_KEYS`, and record it. Never change the trace to a sync section: begin and end are in different main-thread messages, so a sync section would mis-nest.

| Test | Iterations | `startupMode` | Metrics | `setupBlock` | `measureBlock` |
|---|---|---|---|---|---|
| `timelineFirstFrame` | **10** | `COLD` | `TraceSectionMetric("cf.timeline.firstFrame", Mode.First, label = "timelineFirstFrame")`, `StartupTimingMetric()`, `TimelineTraceMetric(firstFrame = true)` | `assertAnimatorsOn(); pressHome()` | `startActivityAndWait(large60Intent(packageName)); awaitPresented()` |
| `timelineScroll` | **3** | `null` | `FrameTimingMetric()`, `MemoryUsageMetric(Mode.Max)`, `TraceSectionMetric("cf.timeline.placeholder", Mode.Count, label = "timelinePlaceholder")`, `TraceSectionMetric("cf.timeline.replay", Mode.Max, label = "timelineReplay")`, `TraceSectionMetric("cf.timeline.replay", Mode.Count, label = "timelineReplay")`, `TraceSectionMetric("cf.timeline.targets", Mode.Max, label = "timelineTargets")`, `TimelineTraceMetric()` | `assertAnimatorsOn(); killProcess(); pressHome(); startActivityAndWait(large60Intent(packageName)); awaitPresented(); page = device.findObject(By.res("trip.page")).also { it.setGestureMargin(device.displayWidth / 5) }; startDay = checkNotNull(topDay())` | `repeat(15) { page.fling(Direction.DOWN) }`, then `val endDay = topDay()`, `Log.i("CfBench", "Timeline scroll progress after 15 flings: $startDay -> $endDay")`, `check(endDay != null && endDay != startDay)` |
| `timelineLastDay` | 1 | `null` | `FrameTimingMetric()` (report only) | `assertAnimatorsOn(); killProcess(); pressHome(); startActivityAndWait(large60Intent(packageName)); awaitPresented()` | the steps below |

`page` and `startDay` are `lateinit var`s of the test class. The UI is queried only once before the loop and once after it.

**`timelineLastDay` measure block:**
1. `device.findObject(By.res("trip.dayMenu")).click()`. Then `check(device.wait(Until.hasObject(By.res(Pattern.compile("trip\\.dayMenu\\.day\\.\\d+"))), 5_000))`.
2. `val menu = device.findObject(By.scrollable(true).hasDescendant(By.res(Pattern.compile("trip\\.dayMenu\\.day\\.\\d+"))))`. [verify] that the M3 `DropdownMenu` column is exposed as scrollable. If it is not, swipe inside the menu's `visibleBounds` with `device.swipe(cx, bottom − 10 %, cx, top + 10 %, 20)`, bounded the same way.
3. `var n = 0; while (!device.hasObject(By.res("trip.dayMenu.day.59")) && n < 12) { menu.scroll(Direction.DOWN, 1f); n++ }`. Then click `trip.dayMenu.day.59`; a `null` object fails. [verify] that the entry is not reported before it is scrolled into view (the M3 menu composes all 60 rows, iOS also requires `isHittable`). If it is, extend the loop condition with `menu.visibleBounds.contains(obj.visibleBounds.centerX(), obj.visibleBounds.centerY())` for the found object, still bounded by 12.
4. `check(device.wait(Until.gone(By.res(Pattern.compile("trip\\.dayMenu\\.day\\.\\d+"))), 5_000))`, then `check(device.wait(Until.hasObject(By.res(LAST_DAY_TAG)), 10_000))` (the selection, iOS waits 10 s).
5. Re-resolve `trip.page`, with the width/5 gesture margin. Then: `var f = 0; while (!device.hasObject(By.res("trip.plan.end")) && f < 6) { page.fling(Direction.DOWN); f++ }`, and finally `check(device.hasObject(By.res("trip.plan.end")))`.

Never use `Thread.sleep` or `SystemClock.sleep`, and never write an unbounded loop.

**Edge cases.**
- The opening is frozen at day 0, so 15 flings always have 59 days ahead.
- If 15 flings do not advance, the `check` fails the test. Never raise the fling count.
- The 300 s instrumentation cap applies to the whole run (OQ-1e-2).

**Tests.** The benchmarks are the tests. `./gradlew :benchmark:assembleBenchmarkRelease` must compile them [verify the task name; use the one 0e's `perf-startup.sh` builds through]. They run only through `make perf-timeline`.

### Task 5: `make perf-timeline`

**Commit:** `build(perf): add make perf-timeline`. **Ticks:** "Budgets".

- **Makefile:** replace the stub with `$(RUN_TIER) perf-timeline --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/perf-timeline.sh`. Use no `--emulator` and no `--timings`, as in 0e's `make perf`. `--device` takes the host-wide emulator lock (0a OQ-0a-21).
- **`Scripts/perf-timeline.sh`** mirrors `Scripts/perf-startup.sh`. Factor its shared steps into `Scripts/perf-common.sh` only if that removes real duplication. In order:
  1. Require exactly one device (`adb get-state`).
  2. Set `IS_EMU=1` when `getprop ro.boot.qemu` or `ro.kernel.qemu` prints `1`. Then `CLASS=emulator`; otherwise `CLASS=${DEVICE_CLASS:-reference}` (`lowEnd` allowed).
  3. On an emulator, read `build/emulator/gpu-mode` (`unknown` if absent) and refuse unless it is `host`.
  4. Refuse when `getprop persist.sys.locale` starts with `nl`. The benchmark is defined in en (iOS `-AppleLanguages (en)`); every other locale coerces to en.
  5. `rm -rf benchmark/build/outputs/connected_android_test_additional_output`.
  6. `trap 'Scripts/emulator.sh prep-functional' EXIT`. Then run `Scripts/emulator.sh prep-perf` with `THEME=${THEME:-light}` (`cmd uimode night no|yes`) and `FONT_SCALE=${FONT_SCALE:-1.0}` (`settings put system font_scale`), plus `svc power stayon true`. Extend `prep-perf` with the theme and font scale, and `prep-functional` with night `no`, font scale `1.0` and stayon `false`.
  7. Stop unless `settings get global animator_duration_scale` is `1` or `1.0`.
  8. Run `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest` with:
     - `-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.TimelineBenchmark`;
     - `…androidx.benchmark.enabledRules=Macrobenchmark`;
     - `…timeout_msec=300000`;
     - `…androidx.benchmark.suppressErrors=EMULATOR`, only when `IS_EMU=1`;
     - the words of `PERF_GRADLE_ARGS`, if set, appended verbatim (used by R4 for `-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.fullTracing.enable=true`).
  9. Always run the checker: `python3 -I Scripts/check-benchmarks.py --suite timeline --device-class $CLASS --results-dir benchmark/build/outputs/connected_android_test_additional_output --history docs/perf-timeline.md --theme $THEME --font-scale $FONT_SCALE --refresh-hz $HZ --gpu $GPU --commit <sha>[-dirty] --append`.
     - `HZ` is `60` on the emulator (`hw.lcd.vsync`, 0e). On a device it is parsed from `dumpsys display` [verify the field], else `unknown`.
     - `-dirty` is added when `git status --porcelain` lists anything other than `docs/perf-timeline.md`.
     - If `PERF_NO_APPEND=1`, omit `--append` (used for R4).
  9a. If the checker exits 4 (only CPU band misses), print `perf-timeline: band miss; re-running once`, repeat steps 5, 8 and 9 once (the re-run appends its own row), and keep the second checker's exit code.
  10. Exit non-zero if Gradle or the checker failed.
- **AGENTS.md:** the `make perf-timeline` row already reads right. Change it only if the wording no longer matches.

**Tests.** `Scripts/tests/test_perf_timeline.py` drives the script with `PERF_TIMELINE_DRY_RUN=1` and stub `adb`, `gradlew` and `emulator.sh` on `PATH`, as `test_perf_startup.py` does. It asserts:
- `suppressErrors=EMULATOR` appears only when the stub `getprop` prints `1`;
- a non-`host` GPU refuses on an emulator;
- an `nl-NL` locale refuses;
- `THEME=dark FONT_SCALE=1.3` reach `prep-perf` and the checker arguments;
- `PERF_GRADLE_ARGS="-Pa=b"` reaches the Gradle command line, and `PERF_NO_APPEND=1` drops `--append`;
- after a stubbed Gradle failure the checker still runs, the trap calls `prep-functional`, and the exit code is non-zero.
- a stub checker that exits 4 once and then 0 makes the script run Gradle twice and exit 0; one that exits 4 twice makes it exit 4.

### Task 6: baseline and startup profiles

**Commit:** `perf(app): generate and commit baseline and startup profiles`. **Ticks:** "Baseline profile".

- **`BaselineProfileGenerator.kt`** (`@Perf`, `@RunWith(AndroidJUnit4::class)`, `@get:Rule val rule = BaselineProfileRule()`, same `packageName` source):
  - `startup()`: `rule.collect(packageName, includeInStartupProfile = true) { pressHome(); startActivityAndWait() }`. It covers whatever `MainActivity` first shows at this stack top; Phase 13 adds Today.
  - `timelineLarge60()`: `rule.collect(packageName) { pressHome(); startActivityAndWait(large60Intent(packageName)); awaitPresented(); val p = device.findObject(By.res("trip.page")); p.setGestureMargin(device.displayWidth / 5); repeat(15) { p.fling(Direction.DOWN) } }`.
- **`:app`:** `baselineProfile { mergeIntoMain = true; saveInSrc = true }`. The files land in `app/src/main/generated/baselineProfiles/baseline-prof.txt` and `startup-prof.txt`. `androidx.profileinstaller` 1.4.1 stays an `implementation` dependency.
- **`:benchmark`:** `baselineProfile { useConnectedDevices = true }`.
- **`make baseline-profile`:** `$(RUN_TIER) baseline-profile --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/baseline-profile.sh`. The script:
  1. requires one device;
  2. on an emulator, refuses unless the GPU mode is `host`;
  3. runs the same trap and `prep-perf` as Task 5;
  4. runs `./gradlew :app:generateBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile -Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000` (each `collect` runs up to 15 launches, far over the 60 s default; OQ-1e-2). If a journey still times out, record the elapsed time and raise it; do not lower `maxIterations`;
  5. fails if either file is missing or empty.
- **`StartupBenchmark`:** switch to `CompilationMode.Partial(BaselineProfileMode.Require)` (OQ-0e-11), and change the startup checker's Build string to `benchmarkRelease, Partial(Require)`. Update 0e's startup checker test if it asserts the old string.
- **Generate** on `pixel` and commit both files in this commit. Do not hand-edit them.

**Edge cases.**
- Generation runs on `nonMinifiedRelease`, and AGP rewrites the rules for the minified variants.
- If the files land in `src/release/generated/…` or `src/benchmarkRelease/…`, the merge configuration is wrong. Fix it; never move files by hand.

**Tests.** `BaselineProfileFilesTest` (plain JVM, `app/src/test/kotlin/net/chronoflow/app/BaselineProfileFilesTest.kt`). It reads `src/main/generated/baselineProfiles/…`, relative to the module directory, and asserts:
- `baseline-prof.txt` holds at least one line containing each of `Lnet/chronoflow/app/MainActivity;`, `Lnet/chronoflow/feature/trip/timeline/TimelinePageKt;`, `Lnet/chronoflow/core/plan/drawing/DayDrawingBuilder;` and `Lnet/chronoflow/feature/trip/timeline/DayDrawingReplayKt;`. Use the FQNs 1c and 1d actually used;
- `startup-prof.txt` is non-empty and contains `Lnet/chronoflow/app/MainActivity;`.

This guards against an empty or stale commit, not against coverage drift.

### Task 7: run, review and record

**Commit:** `docs(perf): record the first timeline benchmark runs`. **Ticks:** "Run and review".

**Runs** on `pixel` (`make emu`, `-gpu host`). The orchestrator runs them if you cannot.

| # | Command | Gated |
|---|---|---|
| R1 | `make perf-timeline` (light, 1.0) | yes |
| R2 | `THEME=dark make perf-timeline` | yes |
| R3 | `FONT_SCALE=1.3 make perf-timeline` | yes |
| R4 | `PERF_NO_APPEND=1 PERF_GRADLE_ARGS="-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.fullTracing.enable=true" make perf-timeline`, with a **temporary, uncommitted** `"benchmarkReleaseImplementation"("androidx.compose.runtime:runtime-tracing")` (BOM) | review only; revert afterwards. If the setup fails, record why and skip it |
| R5 | Only if a physical device is attached (OQ-A22), with the emulator stopped: `DEVICE_CLASS=reference make perf-timeline` or `DEVICE_CLASS=lowEnd make perf-timeline` | yes, the device class's hard limits. Otherwise the device rows stay "open: device only (OQ-A22)" |

If a gate misses, re-run that configuration once to rule out noise, and record both rows. Never change a budget.

**Perfetto review (SQL-backed).** Use R1, `timelineScroll` iteration 2's `.perfetto-trace` (its path is in the JSON's `profilerOutputs`). Record:
- `timelineBuildOnMainCount` (must be 0) and `timelineBuildCount`;
- `timelineRecomposeCount` per iteration. The plan (`04 §11` step 5) expects no `Recomposer:recompose` during steady flings; 15 flings cross day boundaries, so recompositions at anchor changes (the menu label and the jank tagger) are expected and are not a finding (OQ-1e-14). Name every other source from R4's trace; any source inside a day item is a finding;
- `timelineGcCount` per iteration (aim 0);
- `timelineReplayMaxMs` and `timelineReplayCount`. A replay above 1 ms is the B′ trigger on a low-end device (OQ-T27); on the emulator it is only recorded;
- the trace paths. They stay under `build/` and are not committed.

Leave the line `Perfetto UI review: pending (reviewer)` for the Opus reviewer.

**Compiler reports.** Run `./gradlew :feature:trip:compileReleaseKotlin -PcomposeReports=true`. Quote the lines for `DayTimeline`, `NowLine`, `DayHeaderLine`, `TimelineTargets` and `TimelinePresentedMarker` from `feature/trip/build/compose_compiler/*-composables.txt`.
- Each must be `restartable skippable` with every parameter `stable`.
- If one is not, that is a finding. Fix it in a separate `perf(trip): …` commit in this task, then re-run `TimelineInvalidationTest` and R1, and regenerate the profiles (Task 6 command, separate commit `perf(app): regenerate baseline profiles`).

**`docs/perf-timeline.md`**: restructure it into these sections, in this order, keeping 1b's, 1c's and 1d's recorded content:
1. Architecture under test: design B in three lines, citing plan A12.
2. Fixtures and direct launch: `cf.trip-page=S1…S6|large30|large60`, `cf.freezeAt`, and the `adb shell am start -n net.chronoflow.app.debug/net.chronoflow.app.MainActivity --es cf.trip-page large60 --es cf.freezeAt 2026-10-13T08:00:00Z` command.
3. Four guarantee layers: JVM budgets (1b, 1c, in quick); invalidation (1d, in quick); Macrobenchmarks (`make perf-timeline`, required for every timeline change); the device review (`04 §10.4`, before release).
4. Budgets: the §5 table.
5. How to run and read: the commands, and where the JSON and traces land.
6. `## Runs`: the Task 3 table (the checker appends to it).
7. Perfetto review.
8. Compiler reports.
9. `## JVM budgets (1b, 1c), in quick`: 1b's and 1c's rows plus Task 8's.
10. Phase 1d: 1d's section, unchanged.
11. Device rows: reference and lowEnd, each "open: device only (OQ-A22)".
12. Decision (Task 9).
13. Tooling notes: every `[verify]` outcome (raw keys, the `TraceMetric` signature, async-slice support for `cf.timeline.firstFrame`, GC slice names, the Hz source, the DropdownMenu scrollable flag and off-screen entry visibility, the presented marker's visibility to UiAutomator, the Robolectric frame callback, whether `-gpu host` held headless).

Write commands and raw numbers, never impressions.

### Task 8: re-set the 1c JVM limits from measured numbers

**Commit:** `test(plan): set day drawing build budgets from measured numbers`. **Ticks:** "Record".

1. Run `make timing` three times on an otherwise idle Mac (it runs `TimelinePreparationBudgetTest` and `DayDrawingBudgetTest` serially, 0e OQ-0e-22).
2. Record in §9 the mean and max of each 1b fixture and the `dayMedianMax`, `dayMax` and `totalMedian` of each 1c width, with each run's `java -version` and `uname -m`, next to the iOS means (S3 19, large30 50, large60 92 ms). Mark them "host process, not a device claim".
3. Set the `DayDrawingBudgetTest` limits (OQ-1e-4). They only ever tighten, never loosen:
   - per day: `max(1, ceil(2 × the largest measured dayMedianMax))` ms, capped at 5;
   - per width: `ceil(2 × the largest measured totalMedian)` ms, capped at 150.
4. Keep these unchanged: the 512 KiB per-drawing cap, the 4 MiB / 8-entry cache caps, and 1b's 40/100/200 ms preparation limits, which are budget rows.
5. Run `make timing` three more times at the new limits; every run must pass. If one fails, use 2.5× and record it.

### Task 9: decide on design B and amend A12

**Commit:** `docs(plan): record the timeline architecture decision`. **Ticks:** "Decide".

**Decision rule [R]:**
- **B stands** when R1–R3 pass every emulator gate and functional row of plan.md "Where gates apply" and the Task 7 compiler lines are skippable. The gates and rows are: every first frame ≤ 800 ms, every peak RSS anon ≤ 153,600 KiB, build on main = 0, at most 6 placeholders per iteration, the CPU band (no baseline yet in 1e: each configuration's first `PASS` row with Page `host` starts it), Scroll `ok` and Last day `ok`. Write in §12 of `docs/perf-timeline.md`:
  - "Design B stands (emulator, <date>, <commit>)";
  - the median and max first frame with its decode, prepare and typeface parts, the max RSS, the replay max, and the main-thread and RenderThread P99;
  - "Device rows open: device only (OQ-A22, OQ-A45); B′ is re-evaluated on the first low-end run if `timelineReplayMaxMs` > 1 ms (OQ-T27)".
- **A gate missed** after one re-run (a band miss has already been re-run once by the script):
  1. Rule out the harness first: animator scale, GPU mode, `Require` passed, results dir wiped, locale en. Record each check.
  2. Then fix what the evidence names. A fallback is never the fix for a miss outside replay or scroll:
     - first frame over 800 ms: compare `timelineDecodeMs`, `timelinePrepareMs` and `timelineTypefaceMs` with the first `cf.timeline.build` and the replay. Decode or prepare → the 1a/1b pipeline (`PlanDecoder`, `PlanPreparer`); typeface → load the timeline typefaces off the main thread before the first build; the first build → 1c's `DayDrawingBuilder`;
     - RSS over 153,600 KiB → the cache caps (4 MiB, 8 entries), the per-drawing size (512 KiB), and whatever else the trace or a heap dump names;
     - a build on main → a 1d contract bug (rule 7): fix the call site;
     - Scroll or Last day missing → the host or the journey (Task 4);
     - more than 6 placeholders → `prefetchAround` and the cache (1d);
     - only the CPU band, a missed device scroll row, or (on a low-end run) `timelineReplayMaxMs` above 1 ms (OQ-T27) → Task 10 (B′).
     Fix it in a separate `perf(…): …` commit, re-run R1–R3 and record both rows.
  3. If B′ also misses its gate, stop. Record the numbers and the trace evidence, and hand back to the orchestrator for a C (strips) brief. Do not start C or D.
- **plan.md:**
  - Append one sentence to the A12 "Reason" cell: "Phase 1e (<date>): <decision> on the emulator, see `docs/perf-timeline.md`; device rows open."
  - Add the §9 OQ-1e rows to "Open questions for the user", unless the orchestrator already did.
  - Change no other plan text, and no budget.

### Task 10 (only on replay or scroll evidence, Task 9): spike B′, replay through `Picture`

**Commit:** `perf(trip): replay day drawings through an off-main picture`, or `docs(perf): record the picture spike` if the code is reverted.
- After a `DayDrawing` is built on the worker, `TimelineDrawingRepository` records `android.graphics.Picture().apply { replayDayDrawing(beginRecording(widthPx, heightPx), drawing, resources); endRecording() }` on the same worker. `replayDayDrawing(Canvas, …)` is 1c's native-canvas function.
- The cache entry holds the drawing and the picture; the key is unchanged. The cache's byte estimate adds `drawing.estimatedBytes` once and not the picture [verify that no `Picture` size API exists; record it].
- The draw lambda calls `nativeCanvas.drawPicture(picture)` inside the same `drawWithCache` + `graphicsLayer`. Hardware `drawPicture` is supported at minSdk 29.
- Measure with R1 again, and record the row with `B′` appended to Build.
- Keep it only if it fixes the missed gate without breaking another; the peak RSS gate counts the recorded pictures, which the cache's byte estimate leaves out. Otherwise revert the code in this task and keep the numbers.
- **Tests:** 1c's `ReplayGuardTest` and `PrecompositedAlphaTest` pass unchanged. Add `PictureReplayParityTest` (`@Layout`, Robolectric, `@GraphicsMode(NATIVE)`, a pixel test like 1c's `PrecompositedAlphaTest`, so it runs in `make check`, not quick): S3 day 1 at 411 dp, in light and dark, gives pixel-identical bitmaps through the picture path and the direct path.
- If the code is kept, regenerate the profiles (Task 6 command) in a separate commit.

---

## 7. Acceptance criteria

The orchestrator runs any command your sandbox cannot.

1. `make quick` passes in under 60 s warm. It includes `Scripts/tests/test_check_benchmarks.py`, `Scripts/tests/test_perf_timeline.py`, `TimelinePerfMarkersTest`, `TimelineJankTaggerTest`, `TimelineInvalidationTest`, `BaselineProfileFilesTest` and the re-set `DayDrawingBudgetTest`. Each class is listed with `"overBudget": 0` in `build/test-results/quick/timings.json`, or, for `DayDrawingBudgetTest`, in `build/test-results/timing/timings.json` (`make timing`, the last step of quick).
2. `make check` passes with `pixel` booted. That covers Lint, Konsist (`ModuleGraphTest`), `TierRegistryTest` (with `TimelineBenchmark` and `BaselineProfileGenerator` as `@Perf`) and `testReleaseUnitTest` (no JankStats, no `JankStatsInstaller`).
3. `make baseline-profile` regenerates both files. The committed files are the output of the last generation on the final code.
4. R1, R2 and R3 exit 0. Each prints every `Timeline metric:` line of §5, including the report-only ones, and appends its row. `BaselineProfileMode.Require` did not fail. `make perf` (startup) still exits 0 with `Require`.
5. In R1–R3, every iteration has a first frame ≤ 800 ms, `memoryRssAnonMaxKb` ≤ 153,600, `timelineBuildOnMainCount` = 0 and at most 6 placeholders, and Scroll and Last day are `ok`.
6. The checker exits 1 on `first-frame-810` and on every planted Task 3 fixture, exits 0 on `pass` and on the trend fixture, and exits 4 on the band fixture at 11.3 ms.
7. `docs/perf-timeline.md` has all 13 sections. Device rows read "open: device only (OQ-A22)", unless R5 ran on an attached device. The decision is recorded, and plan.md A12 is amended.
8. All seven plan.md 1e boxes are ticked, each in the commit that completed it. The commits are atomic Conventional Commits with no trailers.
9. The debug build of large60 is StrictMode-clean:
   1. `adb logcat -c`;
   2. `adb shell am start -W -n net.chronoflow.app.debug/net.chronoflow.app.MainActivity --es cf.trip-page large60 --es cf.freezeAt 2026-10-13T08:00:00Z`;
   3. `adb shell input swipe 540 1800 540 400 100`, 15 times;
   4. `adb logcat -d | grep -c StrictMode` prints `0`.
10. Cleanup: `make emu-down`, then report `make du`.

---

## 8. Review checklist (Opus reviewer)

Reproduce each finding before you send it back.

**Benchmarks**
- [ ] All three tests and `StartupBenchmark` use `Partial(BaselineProfileMode.Require)`. `suppressErrors=EMULATOR` comes only from the script and only on an emulator, never from `build.gradle.kts`.
- [ ] `timelineScroll` does exactly **15** flings over **3** iterations. `killProcess()` runs in setup, launch and readiness run in setup, and the loop has no UI query. The progress check comes after the loop.
- [ ] `timelineFirstFrame` runs **10** `COLD` iterations and waits for `trip.timeline.presented`, so the async slice closes inside the trace.
- [ ] `timelineLastDay` scrolls the menu at most 12 times and the page at most 6 times, and waits for `LAST_DAY_TAG`. There is no unbounded loop.
- [ ] Every setup calls `assertAnimatorsOn()`. There is no `Thread.sleep` or `SystemClock.sleep`. `topDay()` is one `findObjects` call.
- [ ] The target is `net.chronoflow.app` (not `.debug`), and every intent carries `cf.freezeAt=2026-10-13T08:00:00Z`.

**Metrics and checker**
- [ ] `cf.timeline.placeholder` and `cf.timeline.build` are synchronous slices. The presented marker flips in the same callback that ends `cf.timeline.firstFrame`, and only after a real replay. It takes `State<Boolean>`, is 1 dp, and sits after the `LazyColumn`, outside every day item.
- [ ] Jank % and hitch are computed per iteration from `runs`. Recompute the derived values of the pass fixture by hand.
- [ ] The emulator class gates first frame, RSS, build-on-main, placeholders ≤ 6, the CPU band (exit 4 and one re-run) and the functional rows; nothing else fails there. Report-only metrics are still required. Trends and `WARN` never fail; zero-baseline counters use absolute rises.
- [ ] The results dir is wiped before each run. The trap restores the animator scales, the theme, the font scale and stayon after a failure.

**Profiles and variants**
- [ ] The profiles live in `app/src/main/generated/baselineProfiles/`, were generated rather than hand-written, and come from the final code. `includeInStartupProfile` is set only on the startup journey.
- [ ] JankStats is absent from `release` (`ReleaseReadinessTest`, `ModuleGraphTest`). It is installed after `setContent` and tags only on anchor changes. The invalidation counts are unchanged.

**Record and decision**
- [ ] Every number traces to a run output or a JSON file; `pending` appears where nothing ran. No emulator number is called a device result.
- [ ] The JVM limits were only tightened. The 40/100/200 ms limits, 512 KiB, 4 MiB and 8 entries are unchanged.
- [ ] The A12 amendment is one sentence. No budget in plan.md changed.

**Typical mistakes**
- Measuring a `.debug` build or `CompilationMode.None`.
- A `Trace.setCounter` placeholder (it counts as 0 slices), or an async `cf.timeline.build` (it loses thread attribution).
- `UiObject2.scroll(…, 100f)` or flinging until the end, which recreates the iOS 689 s traversal.
- Querying `trip.plan.end` or labels inside the 15-fling loop.
- An unfrozen clock: the page opens on a different day every day, and from 2026-12-08T11:00Z it opens on the last day, where flings cannot advance.
- Gating jank on the emulator, which gives a flaky red, or not printing it at all, which is missing evidence.
- `putState` on every scroll offset, or a jank log line built for every frame.
- Committing profiles generated before Tasks 1–2, or before a Task 7 or Task 10 code change.

---

## 9. Open questions (proceed on the default)

| # | Question | Default |
|---|---|---|
| OQ-1e-1 | `timelineFirstFrame` iterations: plan 10; iOS and `04 §10.3` 3 | **10** (plan wins) |
| OQ-1e-2 | AGENTS caps instrumented tests at 60 s; 10 cold starts plus profile install exceed it | **`timeout_msec=300000` for `:benchmark` only** (iOS `executionTimeAllowance = 300`), as 0e did for startup, in `perf-timeline.sh` and `baseline-profile.sh`. The run-tier caps still apply |
| OQ-1e-3 | "Placeholder frames" cannot be measured directly | **Count placeholder draw episodes, not frames.** A cached layer can show one placeholder for many frames. Also report first-placeholder-to-first-replay async duration and unfinished intervals; the count alone is not a conservative frame bound |
| OQ-1e-4 | Plan: "re-set 1c's provisional limits from three `make timing` runs". 1b's limits are budget rows | **Only 1c's limits, tighten only.** 1c gates medians over five passes in the serial `make timing` tier (review 2026-11-16), so per day = 2 × the largest measured `dayMedianMax` and per width = 2 × the largest measured `totalMedian`. 1b stays at 40/100/200 |
| OQ-1e-5 | Hitch-ratio denominator | **The main-thread `Choreographer#doFrame` window** of the measure block. Fallback (0e): frame count × 1000 / Hz |
| OQ-1e-6 | Baseline for the ±25 % trend and the CPU band | **The median of the last three `PASS` rows** of the same Class, Theme, Font and Page (fewer: their median; none: no trend and no band). Placeholders and GC, whose baseline is often 0, are flagged on rises of +2 and +1 (review 2026-11-16) |
| OQ-1e-7 | Freeze the clock in perf runs? iOS does not freeze, but its perf page always opens on day 0 (`TripPageDiagnosticRoot.swift:55`). Android's 1d page opens on the day containing now, and large60 ends 2026-12-08T11:00Z | **Freeze with `cf.freezeAt=2026-10-13T08:00:00Z`**: day 0 opens as on iOS, runs are comparable across dates, and the now line is drawn on the first screen. Applies to the benchmarks, the profile generator and acceptance 9 |
| OQ-1e-8 | Theme and font-scale coverage | **R1 light 1.0, R2 dark 1.0, R3 light 1.3**, all gated |
| OQ-1e-9 | Fallback scope inside 1e | **B′ only**, and only on replay or scroll evidence (the CPU band, a missed device scroll row, or replay > 1 ms on a low-end run); other misses go to the fix their evidence names (Task 9). C and D need a new brief |
| OQ-1e-10 | Day progress: iOS reads the `trip.dayMenu` value; UiAutomator has no state-description query | **The topmost visible `trip.day.<id>` resource name.** It is exact, and it is one query |
| OQ-1e-11 | JankStats in `benchmarkRelease` adds a frame-metrics listener to measured runs | **Keep it** (plan A1/A21: "debug and benchmark only"). If R1 differs by more than 5 % with it disabled in a local check, record that and raise it |
| OQ-1e-12 | Perf-run language | **en.** The script refuses an `nl` device locale; the AVD's en-US resolves to en |
| OQ-1e-13 | `timelineBuildOnMainCount` = 0 is not a row of plan.md "Performance budget"; the plan only reviews it ("builds on worker threads only") | **Gate it on every class.** A build on main breaks 1d rule 7 and A12 ("built off the main thread"); a zero count cannot flake. The orchestrator may add the row to plan.md "Performance budget" |
| OQ-1e-14 | Plan (`04 §11`): no `Recomposer:recompose` during steady flings. 15 flings over large60 cross several day boundaries, and each anchor change recomposes the day menu label (1d's ANCHOR positive control) | **Recompositions at anchor changes are expected;** any recomposition inside a day item, or one per frame, is a finding. Recorded, never gated |
| OQ-A22 | Reference and low-end devices | Pixel 8 (or 8a), and a Galaxy A15/A16 or Pixel 4a class phone. Device rows stay "open: device only" until they are attached |
