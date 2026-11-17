# Timeline performance

## 1. Architecture under test

Design B (plan.md A12): a `LazyColumn` with one item per day. Each day's `DayDrawing` op list is built off the main thread (`cf.timeline.build`), then replayed from `drawWithCache` inside its own graphics layer (`cf.timeline.replay`); a placeholder draws until it arrives (`cf.timeline.placeholder`). `NowLine` is the only clock reader. Phase 1e measures it on the `pixel` emulator; emulator numbers are regression signals, never device claims (AGENTS.md "Performance").

## 2. Fixtures and direct launch

`cf.trip-page=S1...S6|large30|large60` opens the timeline page from a fixture (debug and the perf variants only). `cf.freezeAt=<ISO instant>` freezes the plan clock; every benchmark uses `2026-10-13T08:00:00Z`, so large60 opens on day 0 as the iOS perf page does (OQ-1e-7).

```
adb shell am start -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity --es cf.trip-page large60 --es cf.freezeAt 2026-10-13T08:00:00Z
```

## 3. Four guarantee layers

1. JVM budgets (1b, 1c): `TimelinePreparationBudgetTest`, `DayDrawingBudgetTest`, run by `make timing` and in quick (section 9).
2. Invalidation (1d): `TimelineInvalidationTest` and `TimelineJankTaggerTest`, in quick (section 10).
3. Macrobenchmarks: `make perf-timeline` (`TimelineBenchmark`), required for every timeline change.
4. The device review (`04 section 10.4`), before release: open (section 11).

## 4. Budgets

Enforced by `Scripts/check-benchmarks.py --suite timeline` from `benchmark/budgets.json`. On the emulator only the gates fail; every other value prints `REPORT`.

| Metric | Test | Reference hard / aim | Low-end hard / aim | Emulator |
|---|---|---|---|---|
| `timelineFirstFrameMs` (each of 10) | firstFrame | 800 / 450 | 1500 / 800 | **800 gate** |
| `frameDurationCpuMs` P50 / P90 / P99 | scroll | aim 3 / 5 / 8 | aim 6 / 10 / 16 | P50 report; P90, P99 CPU band (max(1.25 x baseline, baseline + 1 ms); exit 4, one re-run) |
| `frameOverrunMs` P99 | scroll | 2 / 0 | 4 / 0 | report |
| `jankPercent`, `hitchRatioMsPerS` (each) | scroll | 3 / 1 %, 5 / 1 ms/s | 5 / 3 %, 5 / 2 ms/s | report |
| `timelinePlaceholderCount` (each) | scroll | 2 / 0 | 6 / 2 | **6 gate** |
| `timelineMainFrameMsP50` / `P99`, `timelineRenderThreadMsP99` | scroll | aim 2 / 4, 4 | same | report |
| `timelineTargetsMaxMs` (lower bound) | scroll | report | aim 1 | report |
| `memoryRssAnonMaxKb` (each) | scroll | 153,600 / 81,920 | 153,600 / 102,400 | **153,600 gate** |
| `memoryRssAnonGrowthKb` | scroll | warn > 40,960 | same | warn |
| `timelineBuildOnMainCount` (each) | scroll | **0** | **0** | **0 gate** (OQ-1e-13) |
| Scroll and Last day present | scroll, lastDay | functional | functional | functional |

Report-only, each required: `timelineScrollWindowMs`, `timelineBuildCount`, `timelineRecomposeCount`, `timelineGcCount`, `timelineReplayMaxMs`, `timelineReplayCount`, `frameCount`, `memoryHeapSizeMaxKb`, `timeToInitialDisplayMs`, `timelineDecodeMs`, `timelinePrepareMs`, `timelineTypefaceMs`.

## 5. How to run and read

```
make emu                                   # the pixel AVD, -gpu host
make perf-timeline                         # light, font scale 1.0 (R1)
THEME=dark make perf-timeline              # R2
FONT_SCALE=1.3 make perf-timeline          # R3
DEVICE_CLASS=reference make perf-timeline  # physical device only
make baseline-profile                      # regenerates the committed profiles
```

`perf-timeline` runs `TimelineBenchmark` on the `benchmarkRelease` variant with `Partial(BaselineProfileMode.Require)`, then the checker, which prints `Timeline metric:` lines, `Timeline trend:` (+-25 % against the median of the last three PASS rows with the same Class, Theme, Font, Page and workload) and `Timeline band:` lines, and appends one row to section 6. Results JSON and Perfetto traces land in `benchmark/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/<device>/` and are not committed. `PERF_NO_APPEND=1` keeps a run out of the table; `PERF_GRADLE_ARGS` adds Gradle arguments.

## 6. Runs

| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Page | Build | First frame ms med/max | CPU P50/P90/P99 ms | Main/Render P99 ms | Overrun P99 ms | Jank % mean/max | Hitch ms/s mean/max | Placeholder max | Replay max ms | RSS anon MiB max | RSS growth MiB | Recompose max | GC max | Scroll | Last day | Result |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-17 | ee26f55-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require) | 92.4/96.9 | 3.1/3.9/5.2 | 2.0/4.5 | -7.2 | 0.0/0.0 | 0.0/0.0 | 0 | 0.7 | 103.5 | 0.4 | 1426 | 10 | ok | ok | PASS |
| 2026-11-17 | ee26f55-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | dark | host | benchmarkRelease, Partial(Require) | 80.2/107.8 | 3.2/3.9/5.1 | 1.9/4.5 | -7.2 | 0.0/0.0 | 0.0/0.0 | 0 | 1.4 | 103.5 | -1.7 | 1405 | 7 | ok | ok | PASS |
| 2026-11-17 | ee26f55-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.3 | light | host | benchmarkRelease, Partial(Require) | 84.3/102.5 | 3.2/4.0/5.1 | 1.9/4.6 | -7.4 | 0.0/0.0 | 0.0/0.0 | 0 | 0.4 | 103.5 | -0.2 | 1404 | 9 | ok | ok | PASS |

`-dirty` marks a run made with uncommitted changes (here the regenerated baseline profile, committed right after).

## 7. Perfetto review

R1-R3 run `TimelineTraceMetric`, a Perfetto SQL pass over each iteration's trace (numbers below, per iteration over the three scroll iterations of the R1 run; the trace files stay under `build/`):

- `timelineBuildOnMainCount`: 0 in every iteration of R1, R2 and R3. `timelineBuildCount`: 57 per iteration (all on worker threads).
- `timelineReplayCount` 59 and `timelineReplayMaxMs` 0.7 (R1), 1.4 (R2), 0.4 (R3), max over iterations. Emulator only; the B' trigger (> 1 ms on a low-end device, OQ-T27) is not evaluated here.
- `timelineGcCount`: 10 (R1), 7 (R2), 9 (R3) max per iteration; the aim is 0. The slice filter is `HeapTaskDaemon` with `LIKE '%GC%'`, which was not checked against one trace by eye.
- `timelineRecomposeCount`: 1,426 (R1), 1,405 (R2), 1,404 (R3) max per iteration, in a 73 s window with about 1,510 frames: roughly one `Recomposer:recompose` per rendered frame. Its source is not named: R4 (`runtime-tracing`, full tracing) was not run, so no recomposition is attributed to a day item or to the anchor change. See OQ-1e-15.
- `timelineTargetsMaxMs` 0.5 (R1), 0.5 (R2), 1.5 (R3).

Perfetto UI review: pending (reviewer)

## 8. Compiler reports

`./gradlew :feature:trip:compileReleaseKotlin -PcomposeReports=true --rerun-tasks`, 2026-11-17 (`feature/trip/build/compose_compiler/*-composables.txt`):

```
restartable skippable fun ...timeline.DayTimeline(  stable day, layout, key, dayIndex, availableWidthDp, repository, resources, clock, onBlock, onFirstReplay, prefetch, modifier)
restartable skippable fun ...timeline.NowLine(  stable clock, dayId, widthDp, modifier)
restartable skippable fun ...timeline.DayHeaderLine(  stable day, colors, minHeight)
restartable skippable fun ...timeline.TimelineTargets(  stable drawing: State<DayDrawing?>, dayId, onBlock, modifier)
restartable skippable fun ...timeline.TimelinePresentedMarker(  stable presented: State<Boolean>, modifier)
```

Every parameter is `stable`; no finding.

## JVM budgets (1b, 1c), in quick

Decode + `layoutPlan` + `preparePlan`, cold (no cache), after three warm-ups; the limit applies to every one of 10 samples. Command: `make timing` (`./gradlew :core:plan:timingTest --max-workers=1`), test class `TimelinePreparationBudgetTest` (`@TimingGate`). Host process on a Mac, not a device claim. Same limits for every build type.

Environment: macOS arm64 (`uname -m` = `arm64`), Gradle launcher Zulu 17.0.21, test JVM Eclipse Adoptium 21.0.13 from the JDK 21 toolchain (`~/.gradle/jdks`, foojay). Run on 2026-11-17 on the Phase 1b branch (`04c9e62`), three consecutive `--rerun` runs on a machine that also ran an idle emulator.

| Fixture | Budget (every sample) | Run 1 mean / max (ms) | Run 2 mean / max (ms) | Run 3 mean / max (ms) | iOS mean (ms) |
|---|---|---|---|---|---|
| S3 | 40 | 2.9 / 3.8 | 3.0 / 3.8 | 2.9 / 4.1 | 19 |
| large30 | 100 | 4.5 / 5.2 | 4.6 / 5.2 | 4.4 / 5.2 | 50 |
| large60 | 200 | 7.2 / 8.2 | 7.5 / 8.4 | 7.0 / 8.8 | 92 |

### DayDrawing build (1c)

Run on 2026-11-17 at `9dbcd07` (Phase 1c branch). Host process on a Mac, not a device claim. `uname -m` = `arm64`; test JVM Eclipse Adoptium 21 from the JDK 21 toolchain (Gradle launcher `openjdk version "17.0.21.0.3"`). Command: `make timing` (`./gradlew :core:plan:timingTest --max-workers=1`), class `DayDrawingBudgetTest` (`@TimingGate`, `FakeTextMetrics`, density 2.625, textScale 1.0, light, 3 warm-ups, 5 measured passes). Gates: day median max ≤ 5 ms, per-width total median ≤ 150 ms, drawing ≤ 512 KiB.

```
DayDrawing metric: W=360 dayMedianMax=0.06 dayMax=0.10 totalMedian=0.9 bytesMax=11795
DayDrawing metric: W=411 dayMedianMax=0.04 dayMax=0.08 totalMedian=0.6 bytesMax=11795
DayDrawing metric: W=841 dayMedianMax=0.04 dayMax=0.06 totalMedian=1.3 bytesMax=14089
```

Cache and sizes (`TimelineRenderingTests`, large60, density 3, `FakeTextMetrics`; the cache stayed within 4 MiB and 8 entries after every put):

```
DayDrawing metric: largest 11795 bytes   (content 358)
DayDrawing metric: largest 13995 bytes   (content 637)
```

Real `TextPaint` on Robolectric (`TextPaintDayDrawingBudgetTest`, large60; bytes gated at 411, times reported only, window 841 report-only; 2026-11-17, `make targeted ONLY="feature:trip:TextPaintDayDrawingBudgetTest"`, host Mac, not a device claim):

```
DayDrawing metric: robolectric W=411 dayMax=0.07 total=1.2 bytesMax=11795
DayDrawing metric: robolectric W=841 dayMax=0.26 total=5.5 bytesMax=14155
```

Wide windows draw the title, interval, tag and reason runs that the icon-only 411 tiles skip, so the 841 build costs about 4x more, still far under the 5 ms and 150 ms gates.

Pre-composite versus a `saveLayerAlpha` reference (`PrecompositedAlphaTest`, 17 types x marked/optional): without text the largest channel difference is 3/255; with text, 16/255 on glyph edges (OQ-A52). 1e re-set the limits to about twice the measured numbers (below).

### Re-set limits (1e)

Three `./gradlew :core:plan:timingTest --max-workers=1 --rerun` runs on 2026-11-17 (`make timing` reuses a cached result, so `--rerun` forces a fresh measurement), Gradle launcher `openjdk version "17.0.21.0.3"`, `uname -m` = `arm64`, test JVM Eclipse Adoptium 21 from the JDK 21 toolchain. Host process, not a device claim.

| Fixture | Budget (every sample) | Run 1 mean / max (ms) | Run 2 mean / max (ms) | Run 3 mean / max (ms) | iOS mean (ms) |
|---|---|---|---|---|---|
| S3 | 40 | 2.8 / 4.0 | 3.1 / 4.4 | 2.9 / 4.3 | 19 |
| large30 | 100 | 5.1 / 6.1 | 5.1 / 6.5 | 5.1 / 5.9 | 50 |
| large60 | 200 | 8.1 / 9.6 | 8.5 / 9.9 | 8.1 / 9.8 | 92 |

`DayDrawingBudgetTest` (`FakeTextMetrics`, density 2.625, five measured passes):

| Width | Run 1 dayMedianMax / dayMax / totalMedian (ms) | Run 2 | Run 3 |
|---|---|---|---|
| 360 | 0.06 / 0.13 / 1.5 | 0.05 / 0.11 / 1.4 | 0.06 / 0.15 / 1.5 |
| 411 | 0.04 / 0.05 / 0.8 | 0.04 / 0.07 / 0.8 | 0.04 / 0.07 / 0.8 |
| 841 | 0.04 / 0.07 / 1.4 | 0.04 / 0.08 / 1.5 | 0.05 / 0.06 / 1.5 |

New limits (OQ-1e-4, tightened only): per day `max(1, ceil(2 x 0.06))` = 1 ms (was 5); per width `max(5, ceil(2 x 1.5))` = 5 ms (was 150; review adds a 5 ms floor to avoid host scheduling flakes). Three more `--rerun` runs at these limits all passed. Unchanged: 512 KiB per drawing, 4 MiB and 8 entries for the cache, and 1b's 40, 100 and 200 ms preparation limits.

## Board differences (Phase 1c)

Reviewed against the `Plan` and `PlanDark` boards. iOS at `f391a1fa` wins over the boards for behaviour (plan.md "How conflicts between sources are settled"), so the timeline draws these as iOS does:

- **Tile shadow.** The boards' `shadow-sm` is not drawn. A12 forbids blur, and iOS draws no shadow.
- **Continuation chevrons.** Drawn as the iOS 8 x 4 dp line pair (`TimelineRenderer.swift`, two 1 dp lines of `smallGap` reach), not the boards' Lucide chevron-up and chevron-down at 12 px.
- **Zone line.** Drawn as the iOS dash pattern (2 on, 4 off, round caps, `emphasisBorder` wide), not the boards' 2 px dotted border.
- **Top hour label.** A day's first hour label at y = 0 is drawn cut off at the top, as iOS draws it. The boards hide it.

## Phase 1d: timeline host

Worker, 2026-11-17. No performance claim is made here; 1e measures. Numbers are `pending` until then.

### Harness outcome (Task 3, OQ-1d-3)

`TimelineProbeHarnessTest` (Robolectric, `@GraphicsMode(NATIVE)`, SDK 36), `make targeted ONLY="feature:trip:TimelineProbeHarnessTest"`: all three controls pass, so draw counts are asserted in Robolectric and OQ-1d-3's instrumented fallback is not needed.

- Draws only run when the test forces one: a Compose draw lambda does not run on `waitForIdle()` alone under Robolectric. Every test settles with `waitForIdle()` followed by `onRoot().captureToImage()`, which draws the window.
- Control 1: a state read in `drawWithCache { onDrawBehind { } }` redraws exactly once per write.
- Control 2: a child with `graphicsLayer()` keeps its draw count when its parent's `drawBehind` state changes; the same child without a layer is redrawn (negative control).
- Control 3: moving a layer with `Modifier.offset { }` from state does not redraw it.

### Perf entry runs (Task 8)

Worker, 2026-11-17, emulator `pixel` (API 36), debug build, animator scales 0, `--es cf.trip-page <name> --es cf.freezeAt 2026-11-15T08:00:00Z`. Emulator numbers are regression signals, not device claims.

| Fixture | `trip.timeline.ready` nodes (uiautomator dump) | `grep -c StrictMode` | crashes |
|---|---|---|---|
| S1, S2, S3, S4, S5, S6, large30, large60 | 1 each | 0 each | 0 |

Screenshots: `build/captures/phase-1d/<name>.png` (evidence that each fixture renders, not board captures).

Perfetto (`adb shell perfetto -t 15s -a net.chronoflow.app.native.debug gfx view sched dalvik`, large60, 15 swipes; trace `build/phase-1d.pftrace`, queried with the Python trace processor):

- `cf.timeline.build`: 17 slices, all on `DefaultDispatch*` threads, none on the main thread.
- `cf.timeline.replay`: 13 slices, all on the main thread. `cf.timeline.placeholder`: 1 slice.
- `cf.timeline.firstFrame`: one async slice, 818 ms before the fix (debug build on the emulator, large60; it included fixture generation). It now starts after the fixture is generated, as in iOS `TripPageDiagnosticRoot`, and covers decode, layout, prepare, typeface load and the first build. Pending 1e for any budget.
- `cf.timeline.firstFrame` after generating the fixture before the section starts (2026-11-17, `pixel`, debug build, cold start, large60, Perfetto `-t 8s -a net.chronoflow.app.native.debug gfx view sched dalvik`, three runs, `build/phase-1d-after-{1,2,3}.pftrace`): 212 ms, 225 ms, 214 ms. The old 818 ms included the roughly 579 ms large60 generator.

### Compose stability (`./gradlew :feature:trip:compileReleaseKotlin -PcomposeReports=true`)

```
restartable skippable ... fun ...timeline.DayTimeline(  stable day, layout, key, dayIndex, availableWidthDp, repository, resources, clock, onBlock, onFirstReplay, prefetch, modifier)
restartable skippable ... fun ...timeline.NowLine(  stable clock, dayId, widthDp, modifier)
restartable skippable ... fun ...timeline.DayHeaderLine(  stable day, colors, minHeight)
restartable skippable ... fun ...timeline.TimelineTargets(  stable drawing, dayId, onBlock, modifier)
```

### Invalidation guard

`TimelineInvalidationTest` (Robolectric, `@Logic`, in `make quick`): scrolling 120 deltas plus a current-day boundary 0 static compositions and draws; minute tick 1 clock composition (day 0 only); day boundary 2; check-off redraws only its day. Removing `graphicsLayer()` from the static layer fails the minute-tick and day-boundary tests (static draws of an untouched day 0 vs 1).

## 11. Device rows

Reference (Pixel 8 class) and low-end (Pixel 4a or Galaxy A15/A16 class) rows: open: device only (OQ-A22). The first production upload needs one reference and one low-end run, or the user's waiver (OQ-A45). R5 did not run: no physical device was attached.

## 12. Decision

**Design B stands (emulator, 2026-11-17, `061f921`).** R1 (light, 1.0), R2 (dark, 1.0) and R3 (light, font scale 1.3) passed every emulator gate and functional row of plan.md "Where gates apply"; the compiler lines of section 8 are skippable. Device: `pixel` (`sdk_gphone64_arm64`, API 36, `-gpu host`, 60 Hz), `benchmarkRelease`, `Partial(Require)`, `make perf-timeline`. Numbers from the section 6 rows and the run logs (emulator numbers, not device claims):

| Value | R1 light 1.0 | R2 dark 1.0 | R3 light 1.3 | Gate |
|---|---|---|---|---|
| First frame median / max (ms) | 92.4 / 96.9 | 80.2 / 107.8 | 84.3 / 102.5 | each <= 800 |
| Decode / prepare / typeface mean (ms) | 8.5 / 9.5 / 1.7 | 7.5 / 7.8 / 1.7 | 7.9 / 8.2 / 1.6 | report |
| Peak RSS anon max (MiB) | 103.5 | 103.5 | 103.5 | <= 150 |
| Builds on main | 0 | 0 | 0 | 0 |
| Placeholder max per iteration | 0 | 0 | 0 | <= 6 |
| Replay max (ms) | 0.7 | 1.4 | 0.4 | report (B' trigger > 1 ms on low-end only) |
| Main P99 / RenderThread P99 max (ms) | 2.0 / 4.5 | 1.9 / 4.5 | 1.9 / 4.6 | report |
| CPU P50 / P90 / P99 (ms) | 3.1 / 3.9 / 5.2 | 3.2 / 3.9 / 5.1 | 3.2 / 4.0 / 5.1 | P90, P99: band (these rows start the baseline) |
| Jank % / hitch ms/s max | 0.0 / 0.0 | 0.0 / 0.0 | 0.0 / 0.0 | report |
| Scroll, Last day | ok, ok | ok, ok | ok, ok | functional |

Device rows open: device only (OQ-A22, OQ-A45); B' is re-evaluated on the first low-end run if `timelineReplayMaxMs` > 1 ms (OQ-T27). R2's replay max of 1.4 ms is an emulator number and is only recorded. GC per iteration (7 to 10) and the recomposition count (OQ-1e-15) are open review items, not gates. `make perf` (startup) still passes with `Require`: median TTID 95.0 ms, REPORT.

## 13. Tooling notes

- `TraceMetric` in benchmark 1.5.0: `getMeasurements(captureInfo: Metric.CaptureInfo, traceSession: TraceProcessor.Session): List<Metric.Measurement>`; `Session.query(sql): Sequence<Row>`. The subclass worked and no fallback was needed.
- Raw keys: every key matched its metric id (`timelineFirstFrameFirstMs`, `timelinePlaceholderCount`, `timelineReplayMaxMs`, `timelineReplayCount`, `timelineTargetsMaxMs`, the `TimelineTraceMetric` names); `frameDurationCpuMs` and `frameOverrunMs` are in `sampledMetrics` with per-iteration `runs`.
- Async slice: `TraceSectionMetric("cf.timeline.firstFrame", Mode.First)` read the async slice on its process track (values 66-156 ms, never 0), so the `TimelineTraceMetric` fallback was not needed. `cf.timeline.decode`, `prepare` and `typeface` are read from process tracks by SQL.
- Presented marker: `By.res("trip.timeline.presented")` was found after launch in all 30 first-frame iterations, so the 1 dp node is visible to UiAutomator (the `opening` state was not queried on its own).
- Robolectric: `TimelinePerfMarkersTest` drives two main-looper frames with `shadowOf(Looper.getMainLooper()).idleFor(16, MILLISECONDS)` after `mainClock.advanceTimeByFrame()`; `advanceTimeByFrame()` alone was not tried.
- Day menu: the `DropdownMenu` popup is its own window, so its `trip.dayMenu.day.<n>` entries had no resource ids until the menu got its own `testTagsAsResourceId` (the first `timelineLastDay` run failed with "the day menu did not open"). After the fix `timelineLastDay` passed within the 12-scroll and 6-fling bounds; which of the scroll or swipe branch ran, and whether off-screen entries are reported, was not recorded.
- Scroll window: historical `fling` calls added about 5 s of idle accessibility-event wait each. The old hitch denominator included those waits and understated hitches (about 2.9×). `bounded-swipe-v2` uses explicit steps and `waitForIdle`; hitch now divides summed positive overrun by active frame time (overrun frame sample count × 1000 / Hz). `timelineScrollWindowMs` remains elapsed-time evidence only. Jank uses nonempty per-iteration samples; CPU percentiles cover produced frames.
- `-gpu host` held headless: `build/emulator/gpu-mode` read `host` for every run; the script refuses anything else.
- Hz: 60 on the emulator (`hw.lcd.vsync`). The device parse (`dumpsys display`, `renderFrameRate`) was not exercised.
- Baseline profile on AGP 9: the profile plugin's `nonMinifiedRelease` stayed minified (AGP 9 ignores its legacy keep-all rules), so the first profiles held R8-renamed classes and no timeline classes. `app/src/nonMinifiedRelease/keepRules/profile.keep` (`-dontobfuscate`, `-dontoptimize`) fixes it; adding `-dontshrink` crashed the app at start with `Resources$NotFoundException` (resource shrinking without code shrinking) and was dropped. The committed profile holds `TimelinePageKt` (22 lines), `DayDrawingBuilder` (5) and `DayDrawingReplayKt` (5).
- Historical run: `make baseline-profile` ran 592 s, just under `run-tier`'s 600 s idle cap, which stopped an earlier attempt at 605 s; `Scripts/baseline-profile.sh` was run directly for the committed profile. `timelineLarge60` needed more than the 300 s test cap, so the script passes `timeout_msec=900000` (the perf script keeps 300,000: each `TimelineBenchmark` test stayed under it).
- R4 (full tracing for the recomposition source) was not run; see OQ-1e-15.

`cf.timeline.targets` encloses the call to `TimelineTargets`. Pausable prefetch composition can execute its bodies after that section ends, so `timelineTargetsMaxMs` is a lower bound, not a complete composition cost. A value below 1 ms cannot establish the low-end aim; inspect full composition traces before changing accessibility behavior (OQ-A46).

The baseline-profile Makefile target now sets `TIER_IDLE_TIMEOUT=1200` for that tier only. Its generator uses the bounded scroll workload and runs through `make baseline-profile`; other tiers retain the 600 s idle cap.

Workload `bounded-swipe-v2` uses 15 explicit 26-step swipes (85% to 15% of the page height, x=8%) with `waitForIdle`. Target travel is roughly 2,800 dp per gesture on pixel; the final day is logged and `trip.plan.end` must remain absent. The CPU band restarts for this workload; old rows are excluded by the Build metadata. The baseline generator uses the same gestures.

Review correction: `timelineRecomposeCount` now counts actual `Compose:recompose` children of `Recomposer:recompose`. The old rows counted Recomposer frames. A real per-frame source was DayMenu's composition-read `derivedStateOf` over never-equal `layoutInfo`; its threshold boolean now comes from `TimelinePage`'s `snapshotFlow`. The probe guard asserts zero DayMenu recompositions while scrolling within the threshold; anchor changes remain expected (OQ-1e-15).
