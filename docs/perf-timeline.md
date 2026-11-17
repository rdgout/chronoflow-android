# Timeline performance

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

Pre-composite versus a `saveLayerAlpha` reference (`PrecompositedAlphaTest`, 17 types x marked/optional): without text the largest channel difference is 3/255; with text, 16/255 on glyph edges (OQ-A52). 1e re-sets the limits to about twice the measured means.

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
- `cf.timeline.firstFrame`: one async slice, 818 ms (debug build on the emulator; it covers fixture read, decode, layout, prepare, typeface load and the first build). Pending 1e for any budget.

### Compose stability (`./gradlew :feature:trip:compileReleaseKotlin -PcomposeReports=true`)

```
restartable skippable ... fun ...timeline.DayTimeline(  stable day, layout, key, dayIndex, availableWidthDp, repository, resources, clock, onBlock, onFirstReplay, prefetch, modifier)
restartable skippable ... fun ...timeline.NowLine(  stable clock, dayId, widthDp, modifier)
restartable skippable ... fun ...timeline.DayHeaderLine(  stable day, colors, minHeight)
restartable skippable ... fun ...timeline.TimelineTargets(  stable drawing, dayId, onBlock, modifier)
```

### Invalidation guard

`TimelineInvalidationTest` (Robolectric, `@Logic`, in `make quick`): scrolling 120 deltas plus a current-day boundary 0 static compositions and draws; minute tick 1 clock composition (day 0 only); day boundary 2; check-off redraws only its day. Removing `graphicsLayer()` from the static layer fails the minute-tick and day-boundary tests (static draws of an untouched day 0 vs 1).
