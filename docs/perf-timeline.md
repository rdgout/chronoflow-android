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

Required activity: `timelineBuildCount` and `timelineReplayCount` must each be ≥ 1 in all three scroll iterations. Empty frame-overrun iterations fail.

Report-only, each required: `timelineScrollWindowMs`, `timelinePlaceholderToReplayMaxMs`, `timelinePlaceholderUnreplayedCount`, `timelineRecomposeCount`, `timelineGcCount`, `timelineReplayMaxMs`, `frameCount`, `memoryHeapSizeMaxKb`, `timeToInitialDisplayMs`, `timelineDecodeMs`, `timelinePrepareMs`, `timelineTypefaceMs`.

## 5. How to run and read

```
make emu                                   # the pixel AVD, -gpu host
make perf-timeline                         # light, font scale 1.0 (R1)
THEME=dark make perf-timeline              # R2
FONT_SCALE=1.3 make perf-timeline          # R3
DEVICE_CLASS=reference make perf-timeline  # physical device only
make baseline-profile                      # regenerates the committed profiles
```

`perf-timeline` runs `TimelineBenchmark` on the `benchmarkRelease` variant with `Partial(BaselineProfileMode.Require)`, then the checker, which prints `Timeline metric:` lines, `Timeline trend:` (+-25 % against the median of the last three PASS rows with the same Class, Theme, Font, Page and workload) and `Timeline band:` lines, and appends one row to section 6. Each attempt archives JSON, Perfetto traces and XML results under `build/perf/timeline/<UTC-date>-<sha>-<theme>-<font>-bounded-swipe-v3.<unique>/`; prior Gradle output is preserved under `previous-output/`. These files are not committed. Startup runs use `build/perf/startup/<UTC-date>-<sha>-<config>.<unique>/`. `PERF_NO_APPEND=1` keeps a run out of the table; `PERF_GRADLE_ARGS` adds Gradle arguments.

## 6. Runs

| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Page | Build | First frame ms med/max | CPU P50/P90/P99 ms | Main/Render P99 ms | Overrun P99 ms | Jank % mean/max | Hitch ms/s mean/max | Placeholder max | Replay max ms | RSS anon MiB max | RSS growth MiB | Recompose max | GC max | Scroll | Last day | Result |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-17 | ee26f55-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require) | 92.4/96.9 | 3.1/3.9/5.2 | 2.0/4.5 | -7.2 | 0.0/0.0 | 0.0/0.0 | 0 | 0.7 | 103.5 | 0.4 | 1426 | 10 | ok | ok | PASS |
| 2026-11-17 | ee26f55-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | dark | host | benchmarkRelease, Partial(Require) | 80.2/107.8 | 3.2/3.9/5.1 | 1.9/4.5 | -7.2 | 0.0/0.0 | 0.0/0.0 | 0 | 1.4 | 103.5 | -1.7 | 1405 | 7 | ok | ok | PASS |
| 2026-11-17 | ee26f55-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.3 | light | host | benchmarkRelease, Partial(Require) | 84.3/102.5 | 3.2/4.0/5.1 | 1.9/4.6 | -7.4 | 0.0/0.0 | 0.0/0.0 | 0 | 0.4 | 103.5 | -0.2 | 1404 | 9 | ok | ok | PASS |
| 2026-11-17 | ce5cb8d | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require), bounded-swipe-v2 | 123.9/135.8 | 6.3/7.8/10.6 | 3.5/9.6 | -3.5 | 0.2/0.5 | 0.5/1.5 | 0 | 0.7 | 103.0 | 1.1 | 9 | 1 | ok | ok | SUPERSEDED (too short: day 9) |
| 2026-11-17 | 2164c68 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 99.4/108.8 | 4.3/7.2/10.2 | 4.3/9.3 | -3.8 | 0.3/0.6 | 0.9/1.4 | 0 | 0.8 | 101.8 | 44.5 | 56 | 3 | ok | ok | PASS |
| 2026-11-17 | bfa817f-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 88.8/97.5 | 3.3/6.5/8.8 | 3.7/7.5 | -5.6 | 0.1/0.2 | 0.1/0.2 | 0 | 2.9 | 103.2 | -1.6 | 56 | 3 | ok | ok | PASS |
| 2026-11-17 | bf61469-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 85.6/108.2 | 3.4/4.4/6.0 | 2.2/4.9 | -5.6 | 0.0/0.0 | 0.0/0.0 | 0 | 0.6 | 103.5 | -1.5 | 56 | 3 | ok | ok | PASS |
| 2026-11-17 | 8b5db6a-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 66.8/84.0 | 2.2/9.1/57.8 | 27.9/81.9 | 65.2 | 9.8/28.4 | 209.1/604.3 | 0 | 4.3 | 102.9 | -1.7 | 56 | 3 | ok | ok | INVALID (busy host; CPU BAND) |
| 2026-11-17 | 8b5db6a-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 156.4/226.1 | 2.4/11.5/113.1 | 48.0/165.3 | 150.8 | 12.0/22.6 | 372.7/970.8 | 0 | 1.4 | 102.7 | -1.7 | 56 | 3 | ok | ok | INVALID (busy host; CPU BAND) |
| 2026-11-17 | 3a3a442 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | host | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 67.6/85.0 | 3.3/4.3/5.7 | 2.2/4.8 | -8.1 | 0.0/0.0 | 0.0/0.0 | 0 | 0.8 | 105.6 | -0.2 | 55 | 3 | ok | ok | PASS |
| 2026-11-19 | 984736e | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 99.3/105.3 | 3.8/4.7/6.1 | 2.7/4.3 | -5.3 | 0.0/0.0 | 0.0/0.0 | 0 | 0.5 | 107.2 | -0.1 | 6346 | 4 | ok | ok | PASS |
| 2026-11-19 | 9c60131 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 82.7/100.0 | 3.8/5.1/6.7 | 3.0/4.7 | -4.6 | 0.0/0.0 | 0.0/0.0 | 0 | 0.4 | 106.9 | 0.2 | 571 | 3 | ok | ok | PASS |
| 2026-11-19 | d61c3ac | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 98.9/100.2 | 3.7/4.8/6.6 | 2.8/4.6 | -5.5 | 0.0/0.0 | 0.0/0.0 | 0 | 0.5 | 108.1 | 0.1 | 571 | 3 | ok | ok | PASS |
| 2026-11-19 | b869739 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 83.3/84.1 | 3.7/4.8/6.7 | 3.0/4.7 | -4.6 | 0.1/0.1 | 0.1/0.3 | 0 | 0.5 | 108.2 | 0.1 | 571 | 3 | ok | ok | PASS |
| 2026-11-19 | e61c4550 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 99.5/100.2 | 3.6/4.9/6.6 | 2.9/4.8 | -5.0 | 0.0/0.0 | 0.0/0.0 | 0 | 0.6 | 108.1 | -0.0 | 571 | 3 | ok | ok | PASS |
| 2026-11-19 | e61c4550 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip-wide | benchmarkRelease, Partial(Require), bounded-swipe-v3 | n/a/n/a | 3.6/4.5/6.4 | 3.4/5.1 | -5.8 | 0.0/0.0 | 0.0/0.0 | 0 | 0.7 | 107.7 | -0.1 | 513 | 5 | ok | — | PASS |
| 2026-11-19 | 8edb8455 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 98.1/99.8 | 3.5/4.6/6.5 | 2.8/4.7 | -5.1 | 0.1/0.2 | 0.1/0.4 | 0 | 0.7 | 109.1 | 0.4 | 571 | 3 | ok | ok | PASS |
| 2026-11-19 | 8edb8455 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip-wide | benchmarkRelease, Partial(Require), bounded-swipe-v3 | n/a/n/a | 3.5/4.4/7.2 | 4.5/12.6 | -5.5 | 0.5/1.4 | 2.2/6.5 | 0 | 0.3 | 108.3 | 0.9 | 527 | 5 | ok | — | PASS |
| 2026-11-20 | 62472d29 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 99.5/100.6 | 3.8/7.2/11.8 | 5.8/8.8 | 1.8 | 1.3/2.1 | 5.1/8.9 | 0 | 1.1 | 109.1 | 0.0 | 571 | 3 | ok | ok | BAND |
| 2026-11-20 | 62472d29 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip-wide | benchmarkRelease, Partial(Require), bounded-swipe-v3 | n/a/n/a | 3.3/4.2/6.2 | 3.5/4.9 | -7.4 | 0.0/0.0 | 0.0/0.0 | 0 | 0.3 | 109.0 | -0.5 | 526 | 5 | ok | — | BAND |
| 2026-11-20 | 62472d29 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 98.9/99.9 | 3.6/4.6/6.4 | 3.0/4.7 | -5.4 | 0.0/0.0 | 0.0/0.0 | 0 | 0.7 | 109.2 | 0.3 | 571 | 3 | ok | ok | PASS |
| 2026-11-20 | 62472d29 | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip-wide | benchmarkRelease, Partial(Require), bounded-swipe-v3 | n/a/n/a | 3.4/4.3/6.3 | 3.3/5.1 | -6.1 | 0.0/0.0 | 0.0/0.0 | 0 | 0.4 | 108.5 | -0.0 | 526 | 5 | ok | — | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 99.2/100.9 | 3.6/4.6/6.4 | 2.9/4.5 | -4.9 | 0.0/0.0 | 0.0/0.1 | 0 | 0.6 | 109.4 | 0.0 | 571 | 3 | ok | ok | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | trip-wide | benchmarkRelease, Partial(Require), bounded-swipe-v3 | n/a/n/a | 3.4/4.3/6.2 | 3.3/4.9 | -5.8 | 0.0/0.0 | 0.0/0.0 | 0 | 0.4 | 109.5 | 0.2 | 526 | 5 | ok | — | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | dark | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 82.3/97.8 | 3.6/4.8/6.3 | 2.9/4.6 | -4.8 | 0.0/0.0 | 0.1/0.3 | 0 | 0.7 | 110.9 | 0.1 | 571 | 3 | ok | ok | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | dark | trip-wide | benchmarkRelease, Partial(Require), bounded-swipe-v3 | n/a/n/a | 3.4/4.4/6.6 | 3.4/5.7 | -5.7 | 0.2/0.7 | 0.4/1.1 | 0 | 0.5 | 109.6 | 0.2 | 525 | 5 | ok | — | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.3 | light | trip | benchmarkRelease, Partial(Require), bounded-swipe-v3 | 83.0/97.3 | 3.6/4.8/6.5 | 3.0/4.7 | -5.0 | 0.0/0.0 | 0.0/0.0 | 0 | 0.6 | 110.7 | 0.1 | 570 | 3 | ok | ok | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.3 | light | trip-wide | benchmarkRelease, Partial(Require), bounded-swipe-v3 | n/a/n/a | 3.4/4.2/6.2 | 3.2/4.7 | -6.0 | 0.0/0.0 | 0.0/0.0 | 0 | 0.3 | 110.2 | -0.2 | 513 | 5 | ok | — | PASS |

`-dirty` marks a run made with uncommitted changes (here the regenerated baseline profile, committed right after).

### Phase 2 review round 1: purpose-specific marker metric

2026-11-17, `make perf-timeline`, one invocation, exit 0; **307.53 s wall**. pixel / emulator-5554, API 36, 60 Hz, host GPU, en/light/font 1.0; benchmarkRelease, Partial(Require), large60, bounded-swipe-v3. Measured source bf61469 with the pending gallery/documentation changes and untracked later briefs (`-dirty`). The marker remains exactly 1 dp; its metric now belongs to TimelinePresentedMarkerMetrics instead of AuthTokens. No timeline algorithm or workload changed.

First-frame median/max **85.6/108.2 ms**; CPU P50/P90/P99 **3.4/4.4/6.0 ms**; main/render P99 **2.2/4.9 ms**; zero placeholders and builds on main; RSS peak **103.5 MiB**, growth **-1.5 MiB**; 44 worker builds/replays per scroll iteration; replay max **0.6 ms**; recomposition/GC maxima **56/3**; scroll and last-day gates passed. The CPU band passed. Report-only trend flags are improvements over the prior matching medians, not physical-device claims or causal attribution to this constant-name change. The appended run row and local archive retain JSON/XML/traces; exact console output is `build/phase2-r1-perf-timeline.log`.

### Phase 2 theme validation

2026-11-17, `make perf-timeline`, one invocation and no retry, exit 0; **289.96 s wall**. pixel / emulator-5554, API 36, host GPU, 60 Hz, en, light, font 1.0, benchmarkRelease, Partial(Require), large60, bounded-swipe-v3. Measured source bfa817f; dirty documentation/references/untracked briefs were present. Archive: `build/perf/timeline/20261117T075222Z-bfa817f-dirty-light-1.0-bounded-swipe-v3.Qea4LS/` (row, log, timing JSON, result XML, benchmark JSON and traces). Emulator regression evidence only.

| Metric | Phase 2 | Prior matching v3 run | Result |
|---|---|---|---|
| First frame median / max | 88.8 / 97.5 ms | 99.4 / 108.8 | all ten <= 800 ms |
| CPU P50 / P90 / P99 | 3.3 / 6.5 / 8.8 ms | 4.3 / 7.2 / 10.2 | P90/P99 band passed |
| Peak RSS anon | 103.2 MiB | 101.8 | <= 150 MiB |
| RSS growth | -1.6 MiB | +44.5 | below warning |
| Main builds / placeholders | 0 / 0 | 0 / 0 | passed each iteration |
| Worker builds / replays | 44 / 44 each | 44 / 44 each | passed activity gate |
| Replay maximum | 2.9 ms | 0.8 | **+262% flagged**, report-only emulator trend |
| Recompositions / GC maxima | 56 / 3 | 56 / 3 | report-only |
| Scroll / last day | ok / ok | ok / ok | passed |

The root now provides ChronoFlowTheme, Reduce Motion and post-first-frame font warm-up. The only timeline source edit replaces the presented marker's literal 1 dp with the generated `AuthTokens.borderWidth` (also 1 dp), to satisfy the new boundary rule. There is no builder/replay/workload algorithm change. The replay trend is retained without a causal claim; the brief's >1 ms B' trigger belongs to low-end physical-device results, which this is not. The checker consumes the actual traces; no extra full-composition trace or dark/large-font benchmark was run in Phase 2.

### Clean R1, forty-day workload (v3)

2026-11-17, clean measured HEAD `2164c68b7ccd35c9abd6c80fff298b459aaa6532` on `feat/phase-1e-timeline-perf`. The tracked tree was clean before launch; only the unrelated untracked phase briefs were excluded by the runner. Command: `export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools; THEME=light FONT_SCALE=1.0 make perf-timeline`. One invocation, no retry, exit 0; tier wall time 306.58 s (Gradle 5m 3s). All three benchmark tests passed. Device: `pixel` / `emulator-5554`, `sdk_gphone64_arm64`, Android 16 / API 36, 60 Hz, host GPU, English, light, font 1.0; `benchmarkRelease`, `Partial(Require)`, Page `host`, large60, workload **`bounded-swipe-v3`**. Emulator results are regression signals, not device claims.

Archive: `build/perf/timeline/20261117T063816Z-2164c68-light-1.0-bounded-swipe-v3.ZMy7gw/`. It retains `row.md`, `run.log`, `timings.json`, benchmark JSON and all 14 traces under `connected_android_test_additional_output/benchmarkRelease/connected/pixel(AVD) - 16/`, XML and per-test logcat under `androidTest-results/`, the SQL review and outputs, profile-generation evidence, and `swipe-diagnosis/`. `previous-output/` is older evidence and is excluded from this result.

The temporary probe measured **550.5 dp for one v2 swipe**, which remains on day index 0. The page is 2,100 px high at density 2.625; the 85% -> 15% path is only 1,470 px = 560 dp. Day 0's grid bottom moved from its unscrolled y=2,915 to y=1,470: 1,445 px / 2.625 = 550.476 dp. UiAutomator 2.4.1's cached `InteractionController` bytecode shows VSync pacing after touch-down and each move, so 26 steps take about 433 ms at 60 Hz; the old 2,800 dp claim assumed an inertial tail that the measurement does not show. v3 groups **five fixed 26-step swipes**, each with `waitForIdle`, into one gesture, with no scroll-end-event wait or UI query inside the workload. The probe travelled **41,343.6 dp over 15 gestures = 2,756.2 dp per gesture on average**, ending at day index 43 with the plan end absent. This exact total uses the final unclipped grid bottom and fixture heights, 95 px headers and 32 px section gaps; clipped bounds in earlier samples are not exact offsets. Both temporary probes were removed before `2f0da30`.

| Metric | Clean R1 (v3) | Emulator gate / interpretation |
|---|---|---|
| First frame median / max | 99.4 / 108.8 ms | PASS: all 10 <= 800 ms |
| Peak RSS anon per iteration | 58,644 / 104,284 / 104,256 KiB; max 101.8 MiB | PASS: each <= 153,600 KiB (150 MiB) |
| RSS growth, first -> last iteration | 45,612 KiB = 44.5 MiB | **WARN: > 40,960 KiB (40 MiB)**; not an emulator failure |
| Builds on main | 0 / 0 / 0 | PASS: each = 0 |
| Worker builds / replays | 44 / 44 in each iteration | PASS: each >= 1; SQL independently confirms worker-only builds |
| Placeholder draw episodes | 0 / 0 / 0 | PASS: each <= 6 |
| Placeholder-to-replay max / unfinished | 0 ms / 0 in each iteration; no intervals | Report only; no placeholder wait was observed |
| Replay max per iteration | 0.542459 / 0.781083 / 0.721209 ms | Report only; > 1 ms triggers B' review on low-end devices only |
| Actual steady-scroll recompositions under Recomposer | 56 / 55 / 54 | Report only; no per-frame pattern; individual sources unattributed (section 7) |
| Top-level GC | 2 / 3 / 3 | Report only; aim 0 |
| Jank mean / max | 0.3 / 0.6 % | Report only |
| Hitch mean / max | 0.9 / 1.4 ms/s | Report only; active-frame denominator |
| CPU P50 / P90 / P99 | 4.3 / 7.2 / 10.2 ms | P50 report; P90/P99 establish the new v3 CPU band baseline; no prior matching row |
| Overrun P99 | -3.8 ms | Report only |
| Main-thread frame P50 per iteration | 0.566375 / 0.615625 / 0.802375 ms | Report only |
| Main / RenderThread P99, max | 4.254375 / 9.258791 ms | Report only |
| Targets max per iteration | 0.4695 / 0.212333 / 0.3015 ms | Report only; lower bound, pausable prefetch bodies excluded |
| Frame counts | 2,125 / 2,067 / 2,086 | Report only; every overrun iteration is nonempty |
| Elapsed scroll windows | 67,618.5 / 66,316.6 / 66,468.5 ms | Report only; not the hitch denominator |
| Heap max per iteration | 19,267 / 58,461 / 58,465 KiB | Report only |
| TTID mean / max | 109.7 / 120.5 ms | Report only |
| Decode / prepare / typeface means | 8.0 / 11.4 / 2.0 ms | Report only; respective maxima 15.0 / 14.9 / 3.1 ms |
| Scroll progress after 15 gestures | Day index 0 -> **43 / 43 / 43** | PASS: each in 35..50 (including >= 35), and `trip.plan.end` absent |
| Separate last-day journey | `trip.day.day-29871120-tour-9` and `trip.plan.end` reached; 537 frames | PASS: within 12 menu scrolls / 6 page gestures |

The CPU band **restarts with `bounded-swipe-v3`**; v2 and earlier rows are excluded by their Build metadata. The old v2 R1 below is superseded because day index 9 after 15 gestures did not exercise the intended forty-day workload, despite passing its former progress assertion. R2/R3, startup perf, full composition tracing and physical-device runs were not rerun. The RSS-growth warning is retained without a causal claim; the peak gate passes.

Profiles were regenerated once through `make baseline-profile` (379.09 s wall, exit 0; startup 47.351 s, timeline 322.988 s) and both changed outputs committed as `2164c68`. The generator uses the same gesture and asserts day 0 -> 35..50 with no plan end. The 48 targeted checker/perf-script tests passed, including exclusion of v2 history from the v3 CPU band.

### Superseded R1 after review fixes (v2: too short)

**Superseded: too short.** This run reached day index 9 rather than the intended 35..50. Its former gate results and archived trace review are retained as historical evidence only.

2026-11-17, clean measured HEAD `ce5cb8db46f8150bd0c6d996e20dc421236b8cbb` on `feat/phase-1e-timeline-perf`; tracked tree clean before launch, with only the unrelated untracked phase briefs excluded by the runner. Command: `export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools; THEME=light FONT_SCALE=1.0 make perf-timeline`. One invocation, no retry, exit 0; tier wall time 153.71 s (Gradle 2m 30s). Device: `pixel` / `emulator-5554`, `sdk_gphone64_arm64`, Android 16 / API 36, 60 Hz, host GPU, English, light, font 1.0; `benchmarkRelease`, `Partial(Require)`, Page `host`, large60, workload **`bounded-swipe-v2`**. Emulator results are regression signals, not device claims.

Archive: `build/perf/timeline/20261117T062016Z-ce5cb8d-light-1.0-bounded-swipe-v2.1PWiyl/`. It retains `row.md`, `run.log`, `timings.json`, JSON and all 14 traces under `connected_android_test_additional_output/benchmarkRelease/connected/pixel(AVD) - 16/`, plus XML and per-test logcat under `androidTest-results/`. `previous-output/` is older evidence and is excluded from this result.

| Metric | Clean R1 | Emulator gate / interpretation |
|---|---|---|
| First frame median / max | 123.9 / 135.8 ms | PASS: all 10 <= 800 ms |
| Peak RSS anon, max | 105,428 KiB = 103.0 MiB | PASS: each <= 153,600 KiB (150 MiB) |
| Builds on main | 0 / 0 / 0 | PASS: each = 0 |
| Worker builds / replays | 9 / 9 in each iteration | PASS: each >= 1 |
| Placeholder draw episodes | 0 / 0 / 0 | PASS: each <= 6 |
| Placeholder-to-replay max / unfinished | 0 ms / 0 in each iteration; no intervals | Report only; no placeholder wait was observed |
| Replay max per iteration | 0.596417 / 0.536209 / 0.727208 ms | Report only; > 1 ms triggers B' review on low-end devices only |
| Actual steady-scroll recompositions | 9 / 9 / 9 | Report only; `Compose:recompose` directly under `Recomposer:recompose`, not a per-frame pattern (section 7) |
| Top-level GC | 1 / 1 / 1 | Report only; aim 0 |
| Jank mean / max | 0.2 / 0.5 % | Report only |
| Hitch mean / max | 0.5 / 1.5 ms/s | Report only; active-frame denominator |
| CPU P50 / P90 / P99 | 6.3 / 7.8 / 10.6 ms | P50 report; P90/P99 start the new CPU band baseline |
| Overrun P99 | -3.5 ms | Report only |
| Main / RenderThread P99, max | 3.5 / 9.6 ms | Report only |
| RSS growth | 1,112 KiB = 1.1 MiB | PASS: below 40,960 KiB warning |
| Scroll progress after 15 gestures | `trip.day.day-29864460-tour-0` -> `trip.day.day-29868150-tour-1` (zero-based day 9), all three iterations | PASS: day advanced and `trip.plan.end` remained absent |
| Separate last-day journey | `trip.day.day-29871120-tour-9` and `trip.plan.end` reached | PASS: within 12 menu scrolls / 6 page gestures |

The CPU band baseline **restarts with `bounded-swipe-v2`**. This is the first matching PASS row, so no prior band applies; historical R1–R3 rows have different Build metadata and are excluded. This run establishes the light / 1.0 / host baseline only. Decode / prepare / typeface means: 11.9 / 15.2 / 2.7 ms; TTID mean / max: 146.9 / 168.6 ms; `timelineTargetsMaxMs` max: 0.4515 ms (lower bound); frame counts: 419 / 422 / 423; elapsed scroll windows: 13,250.0 / 13,317.1 / 13,317.3 ms; heap max: 58,465 KiB. These are all report-only.

## 7. Perfetto review

Clean R1 archive: section 6. Historical R1–R3 evidence was overwritten before archiving was added. Every new attempt preserves both prior output and its own traces, including band retries.

**Forty-day R1 (`bounded-swipe-v3`) trace-processor review completed** on all three scroll traces in `build/perf/timeline/20261117T063816Z-2164c68-light-1.0-bounded-swipe-v3.ZMy7gw/` with `/Users/rickgout/.local/share/perfetto/prebuilts/trace_processor_shell-8488edbefadfc4be -q <file.sql> <trace>`. The archived `review-r1.sql` is the same query set used for v2. Each trace has a matching `*-sql.txt` output and `*-sql.stderr` in the archive root; all three commands exited 0. No graphical Perfetto UI session or extra benchmark run was used.

| Trace filename | Top-level main frames | Frames with one / six actual recompositions | Frames without recomposition, input / no input | Compositions in no-input frames | Top-level GC | Placeholder intervals / max ms / unfinished |
|---|---|---|---|---|---|---|
| `TimelineBenchmark_timelineScroll_iter000_2026-11-17-06-40-03.perfetto-trace` | 4,044 | 44 / 2 | 3,709 / 289 | 1 | 2 | 0 / 0 / 0 |
| `TimelineBenchmark_timelineScroll_iter001_2026-11-17-06-41-22.perfetto-trace` | 3,976 | 43 / 2 | 3,709 / 222 | 1 | 3 | 0 / 0 / 0 |
| `TimelineBenchmark_timelineScroll_iter002_2026-11-17-06-42-38.perfetto-trace` | 3,984 | 42 / 2 | 3,711 / 229 | 2 | 3 | 0 / 0 / 0 |

A recomposition is a main-thread `Compose:recompose` directly under `Recomposer:recompose`: **56 / 55 / 54** over the measured steady-scroll traces. There is no per-frame composition pattern. Unlike v2, these counts exceed the 43 day advances, and 1 / 1 / 2 compositions fall in frames without input-delivery slices. Every iteration also has two input frames with six compositions each. These traces contain no named DayMenu, DayTimeline or TimelineTargets slices, and no source-named composition descendants, so the individual sources remain unattributed; anchor changes cannot explain the complete counts without further evidence. R4 full composition tracing was not run.

SQL confirms **44 builds on DefaultDispatch, zero builds on main and 44 main-thread replays per iteration**, with replay maxima matching section 6. The depth-0, case-sensitive `name GLOB '*GC*'` predicate finds **2 / 3 / 3** `Background concurrent mark compact GC` slices on HeapTaskDaemon and excludes the TrimSpaces / TrimIndirectReferenceTables slices. Process-track queries find **no** `cf.timeline.placeholderToReplay` intervals: zero duration and unfinished count mean no placeholder episode, not a measured zero-duration wait.

### Historical v2 review (superseded: too short)

**Historical v2 R1 trace-processor review completed**, using `/Users/rickgout/.local/share/perfetto/prebuilts/trace_processor_shell-8488edbefadfc4be -q <file.sql> <trace>` on all three archived scroll traces. The archive's `review-r1.sql` combines `build/review-fixes/review-r1.sql` with build/replay, composition-descendant/source-name and placeholder-to-replay queries. Each trace has its own matching `*-sql.txt` output in the archive root. No graphical Perfetto UI session or additional benchmark run was used.

| Trace (under the archive's current output directory) | Top-level main frames | Frames with one actual recomposition | Frames without a recomposition, input / no input | Top-level GC | Placeholder intervals / max ms / unfinished |
|---|---|---|---|---|---|
| `TimelineBenchmark_timelineScroll_iter000_2026-11-17-06-21-27.perfetto-trace` | 795 | 9, all input frames | 741 / 45 | 1 | 0 / 0 / 0 |
| `TimelineBenchmark_timelineScroll_iter001_2026-11-17-06-21-47.perfetto-trace` | 800 | 9, all input frames | 742 / 49 | 1 | 0 / 0 / 0 |
| `TimelineBenchmark_timelineScroll_iter002_2026-11-17-06-22-07.perfetto-trace` | 800 | 9, all input frames | 741 / 50 | 1 | 0 / 0 / 0 |

Recomposition means a main-thread `Compose:recompose` whose direct parent is `Recomposer:recompose`. There are nine per complete steady-scroll trace, none on the frames without input-delivery slices, and no per-frame composition pattern. The nine compositions match the nine day advances; DayMenu's anchor updates are a plausible source from the source code and the existing invalidation guard (OQ-1e-15), **an inference, not trace-proven attribution**. These R1 traces contain no named `DayMenu`, `DayTimeline` or `TimelineTargets` slices and no source-named descendants of `Compose:recompose`; attributing individual compositions or excluding day-item sources requires full composition tracing (R4, not run here).

Each trace contains exactly one depth-0 `Background concurrent mark compact GC` on `HeapTaskDaemon`. The old case-insensitive `LIKE '%GC%'` also matches `TrimSpaces` and `TrimIndirectReferenceTables`; both are excluded by `depth = 0 AND name GLOB '*GC*'`. SQL independently confirms nine synchronous builds on `DefaultDispatch`, zero builds on main and nine main-thread replays per iteration, with replay maxima matching section 6. Process-track queries find no `cf.timeline.placeholderToReplay` intervals: 0 ms and 0 unfinished denotes no placeholder episode, rather than a measured zero-duration wait.

Historical R1–R3 ran `TimelineTraceMetric`, a Perfetto SQL pass over each iteration's trace (numbers below, per iteration over the three scroll iterations of the historical R1 run; the historical trace files were overwritten by later runs; these rows have no retained trace evidence):

- `timelineBuildOnMainCount`: 0 in every iteration of R1, R2 and R3. `timelineBuildCount`: 57 per iteration (all on worker threads).
- `timelineReplayCount` 59 and `timelineReplayMaxMs` 0.7 (R1), 1.4 (R2), 0.4 (R3), max over iterations. Emulator only; the B' trigger (> 1 ms on a low-end device, OQ-T27) is not evaluated here.
- `timelineGcCount`: 10 (R1), 7 (R2), 9 (R3) max per iteration; the aim is 0. The slice filter is `HeapTaskDaemon` with `LIKE '%GC%'`, which was not checked against one trace by eye.
- `timelineRecomposeCount`: 1,426 (R1), 1,405 (R2), 1,404 (R3) max per iteration, in a 73 s window with about 1,510 frames: roughly one `Recomposer:recompose` per rendered frame. These are Recomposer frame counts, not actual recompositions. The verified per-frame composition source was DayMenu's composition-read `derivedStateOf` over `layoutInfo`; the corrected observer and child-slice metric are described in OQ-1e-15. R4 was not run.
- `timelineTargetsMaxMs` 0.5 (R1), 0.5 (R2), 1.5 (R3).

Historical GC cross-check: the corrected predicate was also checked against the retained Phase 1d trace (`build/review-fixes/gc-reference/phase-1d.pftrace`): the old case-insensitive filter returns 3 slices (one `Background concurrent mark compact GC`, `TrimSpaces`, `TrimIndirectReferenceTables`); `depth = 0 AND name GLOB '*GC*'` returns exactly 1 GC. Query and output: `build/review-fixes/gc-review.sql`, `gc-review-phase1d.txt`. Historical R1–R3 counts above use the old predicates and are not current validation; the archived clean R1 review above supersedes them for light / 1.0.

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

**Design B stands (emulator, 2026-11-17, clean `2164c68b7ccd35c9abd6c80fff298b459aaa6532`).** The forty-day R1 (light, 1.0, `bounded-swipe-v3`) passes every applicable emulator gate and functional row: first frame median / max 99.4 / 108.8 ms <= 800; peak RSS 101.8 MiB <= 150; builds on main 0; placeholder episodes 0 <= 6; day index 43 in all three iterations, with the plan end absent; Scroll and Last day both ok. **RSS growth is 44.5 MiB, above the 40 MiB warning threshold**, while all peak samples pass. CPU P50 / P90 / P99 is 4.3 / 7.2 / 10.2 ms and starts a fresh v3 workload baseline, with no prior matching CPU band. Replay max is 0.781083 ms; every iteration builds and replays 44 days. Section 6 records every metric and the archive; section 7 records the SQL review. Emulator numbers are not device claims.

R2 (dark, 1.0) and R3 (light, 1.3) remain historical evidence and were not rerun with v3. The v2 R1 is superseded: 15 gestures reached only day index 9. The compiler lines in section 8 remain skippable. The v3 run's 56 / 55 / 54 actual compositions and 2 / 3 / 3 top-level GCs are report-only; composition-source attribution remains limited as described in section 7. Device rows remain open: device only (OQ-A22, OQ-A45); B' is re-evaluated on the first low-end run if replay max > 1 ms (OQ-T27). The historical startup `make perf` result with `Require` is median TTID 95.0 ms, REPORT; startup was not rerun here.

## 13. Tooling notes

- `TraceMetric` in benchmark 1.5.0: `getMeasurements(captureInfo: Metric.CaptureInfo, traceSession: TraceProcessor.Session): List<Metric.Measurement>`; `Session.query(sql): Sequence<Row>`. The subclass worked and no fallback was needed.
- Raw keys: every key matched its metric id (`timelineFirstFrameFirstMs`, `timelinePlaceholderCount`, `timelineReplayMaxMs`, `timelineReplayCount`, `timelineTargetsMaxMs`, the `TimelineTraceMetric` names); `frameDurationCpuMs` and `frameOverrunMs` are in `sampledMetrics` with per-iteration `runs`.
- Async slice: `TraceSectionMetric("cf.timeline.firstFrame", Mode.First)` read the async slice on its process track (values 66-156 ms, never 0), so the `TimelineTraceMetric` fallback was not needed. `cf.timeline.decode`, `prepare` and `typeface` are read from process tracks by SQL.
- Presented marker: `By.res("trip.timeline.presented")` was found after launch in all 30 first-frame iterations, so the 1 dp node is visible to UiAutomator (the `opening` state was not queried on its own).
- Robolectric: `TimelinePerfMarkersTest` drives two main-looper frames with `shadowOf(Looper.getMainLooper()).idleFor(16, MILLISECONDS)` after `mainClock.advanceTimeByFrame()`; `advanceTimeByFrame()` alone was not tried.
- Day menu: the `DropdownMenu` popup is its own window, so its `trip.dayMenu.day.<n>` entries had no resource ids until the menu got its own `testTagsAsResourceId` (the first `timelineLastDay` run failed with "the day menu did not open"). After the fix `timelineLastDay` passed within the 12-scroll and 6-fling bounds; which of the scroll or swipe branch ran, and whether off-screen entries are reported, was not recorded.
- Scroll window: historical `fling` calls added about 5 s of idle accessibility-event wait each. The old hitch denominator included those waits and understated hitches (about 2.9×). The bounded workloads use explicit steps and `waitForIdle`; hitch now divides summed positive overrun by active frame time (overrun frame sample count × 1000 / Hz). `timelineScrollWindowMs` remains elapsed-time evidence only. Jank uses nonempty per-iteration samples; CPU percentiles cover produced frames.
- `-gpu host` held headless: `build/emulator/gpu-mode` read `host` for every run; the script refuses anything else.
- Hz: 60 on the emulator (`hw.lcd.vsync`). The device parse (`dumpsys display`, `renderFrameRate`) was not exercised.
- Baseline profile on AGP 9: the profile plugin's `nonMinifiedRelease` stayed minified (AGP 9 ignores its legacy keep-all rules), so the first profiles held R8-renamed classes and no timeline classes. `app/src/nonMinifiedRelease/keepRules/profile.keep` (`-dontobfuscate`, `-dontoptimize`) fixes it; adding `-dontshrink` crashed the app at start with `Resources$NotFoundException` (resource shrinking without code shrinking) and was dropped. The committed profile holds `TimelinePageKt` (22 lines), `DayDrawingBuilder` (5) and `DayDrawingReplayKt` (5).
- Historical run: `make baseline-profile` ran 592 s, just under `run-tier`'s 600 s idle cap, which stopped an earlier attempt at 605 s; `Scripts/baseline-profile.sh` was run directly for the committed profile. `timelineLarge60` needed more than the 300 s test cap, so the script passes `timeout_msec=900000` (the perf script keeps 300,000: each `TimelineBenchmark` test stayed under it).
- R4 (full tracing for the recomposition source) was not run; see OQ-1e-15.

`cf.timeline.targets` encloses the call to `TimelineTargets`. Pausable prefetch composition can execute its bodies after that section ends, so `timelineTargetsMaxMs` is a lower bound, not a complete composition cost. A value below 1 ms cannot establish the low-end aim; inspect full composition traces before changing accessibility behavior (OQ-A46).

The baseline-profile Makefile target now sets `TIER_IDLE_TIMEOUT=1200` for that tier only. Its generator uses the bounded scroll workload and runs through `make baseline-profile`; other tiers retain the 600 s idle cap.

Workload `bounded-swipe-v3` uses 15 gestures, each containing five fixed 26-step swipes (85% to 15% of the page height, x=8%) with `waitForIdle`. Measured travel averages 2,756.2 dp per gesture on pixel (section 6), versus 550.5 dp for one v2 swipe. The benchmark logs the final zero-based day index and asserts 35..50, including >= 35, with `trip.plan.end` absent. No UI query or scroll-end-event wait occurs inside the loop. The CPU band restarts for v3; old rows are excluded by the Build metadata. The baseline generator uses the same gestures and bounds.

Review correction: `timelineRecomposeCount` now counts actual `Compose:recompose` children of `Recomposer:recompose`. The old rows counted Recomposer frames. A real per-frame source was DayMenu's composition-read `derivedStateOf` over never-equal `layoutInfo`; its threshold boolean now comes from `TimelinePage`'s `snapshotFlow`. The probe guard asserts zero DayMenu recompositions while scrolling within the threshold; anchor changes remain expected (OQ-1e-15).

Placeholder counts are draw episodes, not frames (corrects OQ-1e-3). The cached layer need not redraw while it scrolls. `cf.timeline.placeholderToReplay` is an async slice from a day lifetime's first placeholder through its first replay; `timelinePlaceholderToReplayMaxMs` reports the maximum completed interval and `timelinePlaceholderUnreplayedCount` reports intervals without a replay. No placeholders gives 0 ms and 0 unfinished; the duration is report-only and the existing count gate remains an episode gate.

## 14. Historical verified review fixes (2026-11-17)

Review fixes A–L and N, regenerated profiles and validation docs are committed through `ce5cb8db46f8150bd0c6d996e20dc421236b8cbb`. The earlier Git-write blocker is resolved. Per-fix patches and commit subjects remain in `build/review-fixes/commits.tsv` as local evidence; they must not be reapplied to this committed tree.

Validation so far (host Mac arm64; emulator numbers are not device claims):

- `make targeted ONLY='feature:trip:TimelineInvalidationTest feature:trip:TimelinePerfMarkersTest feature:trip:PlaceholderReplayTraceTest core:plan:DayDrawingBudgetTest'`: all 12 tests passed.
- `make quick`: passed (832 tests in the quick tier, 178 script/generator tests, plus build-logic and the serial timing tier). `DayDrawingBudgetTest` per-width medians: 1.4 / 0.7 / 1.5 ms at 360 / 411 / 841 dp; largest per-day median 0.07 ms, within the 5 ms / 1 ms limits.
- `make check`: passed, including lint, release readiness, manifest verification, R8 analysis, layout guards, generators and pixel smoke tests.
- `make baseline-profile`: passed through make, 187.13 s wall time including preparation/restoration (Gradle: 3m 4s). Both generated profiles were committed in `0f7c414` (`perf(app): regenerate baseline profiles`).

Historical v2 validation of fix M at `ce5cb8db46f8150bd0c6d996e20dc421236b8cbb` (superseded: workload too short): one `make perf-timeline`, exit 0, no retry; all three benchmark tests passed, with archived JSON, traces, XML, logcat and the trace-processor queries/results. Section 6 records every metric, the final scroll day and the archive path; section 7 records the recomposition, GC and placeholder-to-replay checks; section 12 now cites the replacement forty-day v3 run. The historical v2 CPU band is superseded by `bounded-swipe-v3`.

The earlier `make check` ran after profile generation while clean R1 was blocked; it was not repeated for this documentation-only recording. No failing test was retried. Kotlin daemon marker writes outside the sandbox again reported `Operation not permitted` during R1, but compilation and the benchmark completed successfully; the archive's `run.log` retains the exact diagnostics. OQ-1e-19 records the placeholder episode/duration distinction and its default.

## Phase 3a: 1 px presented marker — invalid timing judgement

**INVALID for performance judgement.** The orchestrator confirmed that the host was busy: the user runs iOS builds and simulators on this same Mac in parallel. The CPU band check failed both measurements. The orchestrator will re-run on a quiet host; these results establish no conclusion about timeline performance, a regression, or the effect of the marker change.

2026-11-17, `make perf-timeline`, one invocation including the script's existing single CPU-band recheck, **640.93 s wall**, exit 4. Pixel / emulator-5554, API 36, host GPU, 60 Hz, en/light/font 1.0, benchmarkRelease, Partial(Require), large60, bounded-swipe-v3, source 8b5db6a with pending profile-generator/docs changes. Both runs completed all three functional benchmark cases: last-day navigation, three 15-gesture scroll iterations, and ten cold first-frame iterations. Every scroll ended at day index 43, with the plan end absent. UiAutomator found the rewritten **1 physical pixel** `trip.timeline.presented` node; the brief's 1 dp/exemption fallback is unnecessary. Functional marker visibility is established independently of the invalid timing comparison.

Observed values, retained only as the invalid run's evidence: initial first-frame median/max **66.8/84.0 ms**, frame CPU P50/P90/P99 **2.2/9.1/57.8 ms**, main/RenderThread P99 **27.9/81.9 ms**, frame-overrun P99 **65.2 ms**, jank mean/max **9.8/28.4%**, hitch mean/max **209.1/604.3 ms/s**, replay max **4.3 ms**. Automatic recheck: first frame **156.4/226.1 ms**, CPU **2.4/11.5/113.1 ms**, main/RenderThread **48.0/165.3 ms**, overrun **150.8 ms**, jank **12.0/22.6%**, hitch **372.7/970.8 ms/s**, replay max **1.4 ms**. Both show zero placeholders, 56 recompositions and three GCs. The checker reported P90/P99 above its retained **8.1/11.0 ms** limits; the busy-host result is neither a pass nor a valid regression judgement.

The first scroll trace includes a **1673.533 ms PathStencilCoverOp**, **1728.106 ms flush commands** and **1681.548 ms main-thread postAndWait**. These are observations from the invalid run, not causal evidence against the timeline. Query output: `build/phase3a-validation/timeline-band-slices.txt`. No timing budget, workload or marker fallback was changed. Raw archives: `build/perf/timeline/20261117T103423Z-8b5db6a-dirty-light-1.0-bounded-swipe-v3.0uwwyu/` and `build/perf/timeline/20261117T103934Z-8b5db6a-dirty-light-1.0-bounded-swipe-v3.8atRZd/`. Full log: `build/phase3a-validation/perf-timeline.log`.

The resumed worker did not re-run `make perf`, `make perf-timeline` or `make baseline-profile`, per the orchestrator's instruction. Quiet-host scheduling belongs to the orchestrator.


## Phase 6b: real trip page

2026-11-19, `feat/phase-6b-trip-page`. The perf entry now uses `TripPageViewModel` and the same `TripPageScreen` as the database-backed route. `benchmark/budgets.json` selects Page `trip`; the first passing trip row starts a separate CPU baseline. Historical host rows are not trip-page evidence. No budget changed.

Measured by the orchestrator on 2026-11-19 (`make perf-timeline`, then `make baseline-profile`). Pixel / emulator-5554, sdk_gphone64_arm64, API 36, host GPU, 60 Hz, en / light / font 1.0, benchmarkRelease, Partial(Require), large60, bounded-swipe-v3. These are emulator gates and reports, never physical-device claims.

The first run, at 984736e (archive `build/perf/timeline/20261119T022453Z-984736e-light-1.0-bounded-swipe-v3.mTbytA`), passed every gate but reported `timelineRecomposeCount` 6346 / 6201 / 6296 per scroll iteration, about three compositions per frame, against 55 on the 1e host page. Its cause was trip-page chrome that read `LazyListState` in composition (the same pattern as OQ-1e-15), fixed in 1f7d790 and 9c60131. `TimelineInvalidationTest#scrollingWithinOneDayDoesNotRecomposePageChrome` now guards it. The run at 9c60131 (archive `build/perf/timeline/20261119T030901Z-9c60131-light-1.0-bounded-swipe-v3.MA0uVc`) is the trip-page baseline:

| Metric | Gate or comparison | Phase 6b, 9c60131 |
|---|---|---|
| First timeline frame mean / max | max ≤ 800 ms | 84.2 / 100.0 ms, PASS |
| TTID mean | report | 102.1 ms |
| Decode / prepare / typeface mean | report | 10.0 / 11.1 / 0.9 ms |
| Frame CPU P50 / P90 / P99 | first `trip` row: starts the band | 3.8 / 5.1 / 6.7 ms |
| Main / RenderThread P99 | report | 3.0 / 4.7 ms |
| Frame overrun P99, jank %, hitch ms/s | report | -4.6 ms, 0.0 %, 0.0 ms/s |
| Peak RSS anon / growth | peak ≤ 150 MiB; growth > 40 MiB warns | 106.9 MiB / 0.2 MiB, PASS |
| Builds on main | 0 | 0, PASS |
| Placeholder episodes per iteration | ≤ 6 | 0, PASS |
| Builds / replays per iteration, replay max | report | 40 / 40, 0.4 ms |
| Recompositions / GC per iteration | report | 571 / 3 (was 6346 / 4 at 984736e) |
| Last day / end reachability | last day selected, end reached | PASS |
| Baseline / startup profiles | non-empty; TripPage, DayHeader, SummaryCard | regenerated; `BaselineProfileFilesTest` passes |

The 571 compositions are the same in all three iterations, about 13 for each of the 43 day advances, and the guard proves zero while scrolling within one day. Attributing them to the day-change chrome (top bar title, pills, compact header, day menu) is an inference from the code, not trace-proven. No trend flags apply, because this is the first `trip` row. The StrictMode fixture sweep and a manual trace review were not run for this phase.

Host evidence: the retargeted six-case `TimelineInvalidationTest` passes (suite 3.628 s; individual maximum 2.804 s). Its sustained 120 × 1 dp scroll case takes 0.307 s, the summary minute case 0.090 s, and the perf presented-marker case 0.058 s. These are Robolectric test wall times, not timeline runtime metrics. The static drawing layer, off-main builder, clock isolation and all budget thresholds remain intact.

`make timing` passes all six serial JVM gates in **11.62 s wall** (Gradle 11 s), MacBook-Pro-van-Rick.local / Apple silicon, JDK 21 test toolchain, `:core:plan:timingTest`, one worker. This is host evidence, with no emulator or device build. Decode/layout/preparation samples and drawing samples remain below their existing gates:

| Host measurement | Mean / maximum or median | Unit |
|---|---|---|
| S3 decode/layout/preparation | 3.2 / 4.8 | ms |
| large30 decode/layout/preparation | 5.0 / 5.5 | ms |
| large60 decode/layout/preparation | 8.4 / 10.3 | ms |
| Drawing total median, 360 / 411 / 841 dp | 1.5 / 0.8 / 1.6 | ms |
| Largest day median, 360 / 411 / 841 dp | 0.07 / 0.05 / 0.05 | ms |
| Drawing bytes maximum, 360 / 411 / 841 dp | 11795 / 11795 / 14089 | bytes |

The synthetic benchmark-checker tests now use Page `trip` for their matching-history fixtures and Page `host` for the isolation case. All 38 checker tests pass; no runtime threshold or history row was changed.

## Phase 6c: list mode

Measured by the orchestrator on 2026-11-19 with `make perf-timeline` and `make baseline-profile`. Pixel / emulator-5554, sdk_gphone64_arm64, API 36, host GPU, 60 Hz, en / light / font 1.0, benchmarkRelease, Partial(Require), large60, bounded-swipe-v3. These are emulator values, never device claims.

**Timeline CPU band: missed, and the A/B attributes the miss to the environment, not to 6c.** Every 6c run missed the trip band (baseline P90/P99 4.9/6.4 ms):

| Commit | timelineScroll CPU P50/P90/P99 ms | Main/Render P99 ms | Archive |
|---|---|---|---|
| ea8f3ef | 5.4/8.1/11.1 | 6.0/7.7 | `20261119T093129Z-ea8f3ef-…fwNugI` (the reviewer was running Gradle at the same time) |
| 4b036c7 | 6.8/8.7/15.6, recheck 6.8/8.7/16.3 | 6.0/9.1, 6.1/9.4 | `20261119T095549Z-…pR7qS5`, `20261119T100443Z-…tnloBZ` |
| e418883 (after an emulator restart) | 6.6/8.6/15.1, recheck 6.4/8.9/16.8 | 6.2/12.3, 5.8/9.6 | `20261119T111928Z-…zVACJX`, `20261119T112813Z-…tDhIvV` |

Frames (~2,100), recompositions (571), builds and replays (40/40), placeholders (0) and GC (3) are unchanged from 6b. The trace analysis in `docs/perf-6c-review.md` finds the extra cost on the app's main thread and RenderThread, and also in the launcher and SurfaceFlinger. A matched A/B ran `TimelineBenchmark#timelineScroll` alone, back to back, between 12:06 and 12:18:

| Run | CPU P50/P90/P99 ms |
|---|---|
| 6b (87e4d7a) | 6.4/8.9/16.6 |
| 6c (e418883) | 6.6/8.7/11.9 |
| 6b again | 6.4/8.6/13.4 |

6b's own code, which measured 3.8/5.1/6.7 at 04:09, now reproduces the 6c numbers. The miss is therefore an environment change and not a 6c regression. The suspected cause is host GPU contention: the emulator uses `-gpu host`, and during these runs WindowServer and a Chrome GPU helper were using 30–45% CPU. That cause is an inference and has not been proven. No band row is recorded as PASS, and OQ-A291 holds the re-measure.

**Other gates, at e418883:**
- First frame: max 99.4 ms (gate 800).
- RSS peak: 108.0 MiB.
- Builds on main: 0.
- Placeholders: 0.
- Last day and end: reached.
- The `listScroll` row (its first; it sets no band): CPU P50/P90/P99 5.8/8.2/11.4 ms; recheck 5.8/8.2/12.3; jank max 1.1 %; hitch max 2.8 ms/s; RSS peak 107.2 MiB (gate 150). These numbers were taken in the same slow environment, so they are not a list-mode baseline.

**Profiles:** regenerated at 02c8db7 and committed in 3e3d449, with DayListRow coverage. `BaselineProfileFilesTest` passes.

**Not run:** the separate StrictMode sweep (remote open, list, pull) and an independent capture review. The ready-list pixel smoke test logged zero IO violations.



## Phase 6d — interactions (2026-11-19)

Host evidence: MacBook-Pro-van-Rick.local, macOS 27.2.1, JDK 17 Gradle / JDK 21 test toolchain, JVM budget build and debug Robolectric native graphics. Command: `make timing`, serial after the remaining quick checks. Six gates pass, 2.30 s tier wall (2.35 s shell wall). These are host measurements, not emulator or device claims.

| Preparation fixture | Mean ms | Maximum ms of ten samples | Gate ms per sample |
|---|---:|---:|---:|
| S3 | 2.8 | 4.3 | 40 |
| large30 | 4.1 | 4.7 | 100 |
| large60 | 6.5 | 7.9 | 200 |

| large60 width dp | Largest day median ms | Largest day maximum ms (report only) | Total median ms | Largest drawing bytes |
|---|---:|---:|---:|---:|
| 360 | 0.07 | 0.10 | 0.8 | 11,795 |
| 411 | 0.04 | 0.05 | 0.6 | 11,795 |
| 841 | 0.05 | 0.14 | 1.3 | 14,089 |

Drawing gates remain median ≤1 ms per day, total median ≤5 ms per width, ≤512 KiB per drawing. XML and raw sample output: `build/phase6d-review/timing/` and `core/plan/build/test-results/timingTest/`.

The button and custom-action check-off guards both pass. Only day k rebuilds its static layer and targets; the unchanged visible days have zero static/target/clock/header changes. Day k has one static composition, 1–2 draws and 1–2 target compositions: check actions can arrive before the asynchronous replacement drawing. The clock stays unchanged. The list guard proves a scroll moves within one day with zero row/chrome compositions and zero composition passes; its header-crossing positive control observes composition. The original timeline scroll guard stays green.

`./gradlew :feature:trip:compileReleaseKotlin -PcomposeReports=true` passes (4 s Gradle wall). Release report excerpts, with every listed parameter marked `stable`:

```text
restartable skippable ... fun net.chronoflow.feature.trip.timeline.DayTimeline(
  stable day: PreparedDay
  stable layout: DayLayout
  stable key: DayDrawingKey
  stable actions: DayCheckActions?
  stable onCheck: Function2<String, CheckStatus?, Unit>?
  ... all other parameters stable
)
restartable skippable ... fun net.chronoflow.feature.trip.timeline.TimelineTargets(
  stable drawing: State<DayDrawing?>
  stable dayId: String
  stable onBlock: Function1<String, Unit>
  stable modifier: Modifier?
  stable actions: DayCheckActions?
  stable onCheck: Function2<String, CheckStatus?, Unit>?
)
```

The baseline journey now opens a checkable block, expands the native sheet, checks it, undoes it and closes it using bounded structural-tag waits. The perf-only source feeds pending checks through the real decoder. Its JVM test passes. The timeline/list measurement workloads and checker thresholds are unchanged.

**Emulator runs (orchestrator, 2026-11-19, `make perf-timeline`, quiet host):**

| Commit | Profiles | First frame mean/max ms | Timeline CPU P50/P90/P99 ms | List CPU P50/P90/P99 ms | Main/RT P99 ms | Jank % max | Result |
|---|---|---:|---:|---:|---:|---:|---|
| d61c3ac | 6c profiles | 98.9/100.2 | 3.7/4.8/6.6 | 3.4/4.4/5.9 | 2.8/4.6 | 0.0 | PASS |
| b869739 | regenerated with the step-sheet journey | 83.1/84.1 | 3.7/4.8/6.7 | 3.5/4.5/6.0 | 3.0/4.7 | 0.1 | PASS |

Both runs sit inside the trip CPU band, next to the 9c60131 row (3.8/5.1/6.7), with zero placeholders, zero main-thread builds and RSS growth under 0.2 MiB. No ±25% FLAGGED line. These are the first in-band `listScroll` numbers. They close OQ-A291: on a quiet host the 6c-and-later stack measures like 6b, so the 6c misses were environmental.

**Profiles:** `make baseline-profile` first failed at "native sheet missing", because the sheet window did not export its test tags. After 6028332 the profiles regenerate in about 4 min; they are committed in b869739.

## Phase 6e — wide and foldable page (2026-11-19)

Implementation adds `TimelineBenchmark.timelineScrollWide`: 15 bounded flings × 3 iterations on large60 at `wm size 1840x2208` (about 701×841 dp at 420 dpi), using the same frame/memory/trace metrics as phone timeline scroll. Readiness checks use structural resource ids with bounded waits. The wide profile journey covers list selection, the step sheet, scrolling and a resize back to phone. Finally/@After cleanup, runner cleanup and the Makefile's trap reset the window; runner cleanup restores functional animator scales. The Makefile trap runs within the device lock.

No emulator performance run or profile generation was performed, per user instruction. No Phase 6e timing, RSS, refresh-rate or jank result is claimed. Existing profile files remain from the prior measured stack until the following user-run regeneration. JVM invalidation proof: 120 within-day deltas schedule zero composition passes; the deliberate composition-read mutation schedules 240 and fails. Source/replay architecture stays A12 (no saveLayer, blur or TextMeasurer; NowLine owns the timeline clock).

User-run commands, with `pixel` already running (`-gpu host`, API 36):

```sh
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
make baseline-profile
make perf-timeline
```

Record the measured commit, actual refresh rate, font scale 1.0, theme and `benchmarkRelease` build. The checker retains phone Page `trip` and appends wide Page `trip-wide`; the wide page cannot enter phone CPU baselines. Wide emulator RSS is gated at 150 MiB; other wide values report, while missing metrics or iterations fail. Physical-device rows keep the phone values and remain open: device only (OQ-A22).

Required evidence for timelineFirstFrame, timelineScroll, timelineLastDay, listScroll and timelineScrollWide: first-frame duration where applicable; raw frameDurationCpuMs P50/P90/P99, frameOverrunMs P99, jank %, hitch ratio, placeholder episodes where applicable, builds on main, replay/target times, recomposition/GC counts and peak anonymous RSS. Raw JSON and Perfetto traces are archived by the existing runner. Record each ±25% phone trend flag against the last three matching PASS rows with its cause; the flag itself is not a gate. Phone first-frame, RSS, placeholder, build-on-main and CPU-band gates still apply.

**Emulator runs (orchestrator, 2026-11-19, `make perf-timeline`, pixel, `-gpu host`):**

| Commit | Run | First frame mean/max ms | CPU P50/P90/P99 ms | Main/RT P99 ms | RSS max MiB | Result |
|---|---|---:|---:|---:|---:|---|
| 4dfa6642 | trip (phone) | 99.5/100.9 | 3.7/4.8/6.5 | 3.0/4.7 | 108.1 | PASS |
| 4dfa6642 | trip-wide (`wm size 1840x2208`, 701 dp) | n/a | 3.6/4.5/6.4 | 3.4/4.9 | 108.0 | PASS |
| e61c4550 | trip (phone) | 99.5/100.2 | 3.6/4.9/6.6 | 2.9/4.8 | 108.1 | PASS |
| e61c4550 | trip-wide | n/a | 3.6/4.5/6.4 | 3.4/5.1 | 107.7 | PASS |

`listScroll` CPU P99 was 6.0 ms in both runs. The 4dfa6642 run flagged `timelineReplayMaxMs 0.5 -> 0.9 (+80%)` (a trend flag, not a gate). The review found no code cause (the per-day replay path is unchanged), and the e61c4550 run measured 0.6 ms, so it was emulator noise. Zero placeholders, zero main-thread builds.

**Profiles:** regenerated with the wide journey and committed in 4dfa6642. The round-1 fixes after it change no startup or journey path.

**Pin lag (OQ-A311):** the wide invalidation guard now advances exactly one frame after each scroll delta (fad7add9). It passes, and a deliberately frame-delayed offset fails it by 3 px. The Perfetto and screen-recording inspection was not done; the one-frame guard stands in for it.

**StrictMode smoke** (debug build, the large60 trip, two swipes; the commands below): 0 StrictMode lines.

```sh
adb -s emulator-5554 logcat -c
adb -s emulator-5554 shell am start -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity --es cf.trip-page large60 --es cf.freezeAt 2026-10-13T08:00:00Z
adb -s emulator-5554 logcat -d | grep -c StrictMode
```

T2 fold driving is on request only; see `docs/device-matrix.md`.


## Phase 7 — shared card and check-off writer (2026-11-19)

Task 1 moves the trip card to `:core:ui` and delegates the trip page's enqueue path to `CheckOffWriter`; busy, refusal and cancellation handling stay in the trip ViewModel. The targeted run passes all 12 `TimelineInvalidationTest` and 124 `ListInvalidationTest` cases. Drawing, replay, `NowLine` and per-frame code are unchanged.

Results (`make perf-timeline`, `pixel` AVD, API 36, 60 Hz, animator scales 1, `benchmarkRelease`, Partial(Require), host on AC power and quiet, 8edb8455 + regenerated profiles 96736893): frame CPU P50/P99 **3.5/6.5 ms** timeline (6e e61c4550: 3.6/6.6), **3.4/6.0 ms** list (6.0), **3.5/7.2 ms** wide (6.4). All PASS, within ±25% of the 6e baselines. Trend flags are reports: replay max 0.5→0.7→0.3 ms (noise around a sub-ms value), main-frame P99 3.4→4.5 ms and render-thread P99 5.1→12.6 ms on one scenario; frame CPU P99 stays in budget, so this is recorded to watch in the next timeline phase, not a regression claim.

Invalid runs, discarded: earlier the same evening the host ran on battery in Low Power Mode (`pmset powermode 1`). Every scenario doubled (timeline P99 13–16 ms), and the unchanged 6e top e61c4550 measured 13.3 ms on the same host, so those rows were not recorded. Emulator numbers are reports, never device claims.


Phase 7 Task 2 JVM measurement (`make targeted ONLY="core:plan:TodayTests core:plan:TodayEdgeTests core:plan:TodayViewBudgetTest"`, local MacBook Pro, debug test compilation): large60 `todayView`, one warm-up and ten samples, min **0.7365 ms**, max **0.8724 ms**; every sample passes the **20 ms** budget. These are JVM numbers only. The final serial `make quick` timing tier also includes this guard.


Phase 7 Task 6 invalidation proof (`make targeted ONLY="feature:today:TodayInvalidationTest"`, Robolectric API 36, debug tests, pixel8 width/test-only tall viewport): quiet minute = **0** SCREEN/HERO/PANEL/ROW recompositions, **1** per changed clock leaf (six leaves). Marked rows have no leaf. The plain-Instant row negative control records **1** ROW recomposition. Both cases pass; this is a JVM scope-count proof, not a device frame-time claim.


## Phase 9h review round 1: marker publication (2026-11-20)

The parallel logic suite reproduced `TimelinePerfMarkersTest` failing while both Choreographer callbacks had completed. Temporary diagnostics recorded replay at uptime 310 ms, callback 1 at 325 ms, callback 2 at 340 ms, and `presented == true` at the assertion at 372 ms; semantics still exposed `trip.timeline.opening`. The production marker read its flag in composition, so tag publication could lag the two-frame trace endpoint. Reading it in the semantics lambda invalidates the marker directly. The trace’s callbacks, first real replay, drawing pipeline, `NowLine` and benchmark journeys are unchanged. All diagnostics were removed; the existing marker test and frame count are unchanged.

Worker evidence: Robolectric API 36, pixel8 profile, debug unit tests on the macOS host. The unchanged marker class passed five consecutive executions alongside the app, wizard, plan and network logic suites in parallel Gradle test processes. Test outputs were forced to execute; the temporary init script allows four trip forks and eight Gradle workers. Command wall times: **20.438 / 21.262 / 18.513 / 26.438 / 26.013 s**. The marker cases took **2.714 / 3.340 / 3.191 / 2.865 / 2.749 s**, each the fork’s first Robolectric SDK case (20 s allowance). These are test-harness cold costs, not app first-frame measurements. XML and timing evidence: `/private/tmp/9h-review-proof/review-loaded-1` through `review-loaded-5`; diagnostic XML: `/private/tmp/9h-review-marker-diagnostic.xml`.

The page now observes the boolean availability of actions; the full busy state is collected inside the open sheet. `TimelineInvalidationTest.actionBusyChangesDoNotRecomposePageChrome` renders the real page and toggles the real preparer. Its initial mutation proof failed with 16 composition passes before these fixes; the final guard records **zero** passes and zero chrome compositions. The targeted class passes all **13** cases in **3.945 s** (command **7.923 s**); the new case is **0.091 s**. No drawing, main-thread build, clock, blur, layer or per-frame allocation path was added.

No emulator performance or profile tier was run, per the orchestrator. Host follow-up commands remain `make perf-timeline` and `make baseline-profile`; record their normal first-frame, CPU/jank, RSS, placeholder, main-thread build, replay, recomposition and GC metrics here. No startup path changed, so no new startup measurement is claimed.

## Phase 10a review round 1: settings clock seed (2026-11-21)

`PlanClock` exposes its synchronous current instant so Settings can seed the sync age before the first flow emission. The timeline collector, renderer, drawing cache and clock ownership are unchanged. The single review `make quick` ran the serial `:core:plan:timingTest --max-workers=1` stage on the macOS arm64 host, JDK 21 JVM test build, in 2.36 s wall time. All seven budget cases passed; these are host JVM measurements, not Android frame-time or device claims.

| Preparation fixture | Mean ms | Max ms |
|---|---:|---:|
| S3 | 2.9 | 4.0 |
| large30 | 4.2 | 4.7 |
| large60 | 6.5 | 8.9 |

| DayDrawing width dp | Worst day median ms | Worst day max ms | Total median ms | Max bytes |
|---|---:|---:|---:|---:|
| 360 | 0.08 | 0.16 | 1.5 | 11,795 |
| 411 | 0.05 | 0.07 | 0.6 | 11,795 |
| 841 | 0.07 | 0.10 | 1.2 | 14,089 |

The ten large60 `todayView` samples range from 0.49925 to 0.548583 ms (20 ms budget). Evidence: `core/plan/build/test-results/timingTest/TEST-*.xml`, `build/test-results/timing/timings.json`, `/tmp/chronoflow-10a-r1-quick.log`. No emulator performance run or profile regeneration is required for this Settings diff: the host review confirmed unchanged `Application.onCreate` and no new pre-frame reads.

## Phase 10b: serial JVM budget check (2026-11-21)

The single final quick run stopped at a stale glyph-count assertion. After the corrected method passed, the remaining quick stages ran separately, ending with `make timing` (`:core:plan:timingTest --max-workers=1`). All seven cases passed in 3.75 s tier wall time. The timing task executed freshly on the local macOS host using the JDK 21 JVM test toolchain. These are host JVM metrics, not Android frame-time or device claims. No timeline, trip-page, drawing-cache or plan source changed in 10b.

| Preparation fixture | Mean ms | Max ms |
|---|---:|---:|
| S3 | 2.8 | 4.2 |
| large30 | 4.1 | 5.2 |
| large60 | 6.4 | 8.0 |

| DayDrawing width dp | Worst day median ms | Worst day max ms | Total median ms | Max bytes |
|---|---:|---:|---:|---:|
| 360 | 0.06 | 0.08 | 0.9 | 11,795 |
| 411 | 0.04 | 0.08 | 0.5 | 11,795 |
| 841 | 0.05 | 0.08 | 1.4 | 14,089 |

The ten large60 `todayView` samples range from 0.580625 to 0.641 ms (20 ms budget). Quick also passed the 13 `TimelineInvalidationTest` and 124 `ListInvalidationTest` cases unchanged. Evidence: `core/plan/build/test-results/timingTest/TEST-*.xml`, `build/test-results/timing/timings.json`, `build/phase-10b/final/quick-timings.json`, `/private/tmp/chronoflow-10b-timing.log`. No emulator performance/profile command was run. R10 does not require `make perf-timeline` for this diff; the root theme startup/profile handoff is recorded in `docs/perf-startup.md`.

## Phase 13

The worker changed only benchmark/profile journeys and their scripts, not the timeline renderer or page. R17 reserves all device/perf tiers to the host. The new shared last-day helper retains the twelve menu-scroll / six bounded-gesture limits; its default swipe path retains bounded-swipe-v3 and adds reversal only for the trip profile's return scroll. Existing static/clock invalidation and timing gates passed the worker quick checks. The host has now completed R2–R4 below; the six auto-recorded history rows are retained unchanged.

| Run | Exact host command | Evidence/current result |
|---|---|---|
| R2 | `make perf-timeline` | PASS emulator gates; light/font 1.0; t13-tl.log, 767.65 s command wall |
| R3 | `THEME=dark make perf-timeline` | PASS emulator gates; dark/font 1.0; t13-tldark.log, 769.83 s command wall |
| R4 | `FONT_SCALE=1.3 make perf-timeline` | PASS emulator gates; light/font 1.3; t13-tlfont.log, 765.58 s command wall |
| R6 | `make size` | Host PASS, 4,156,988 B (4.16 MB); enableSplit=true failed as expected with language split present: splits/base-en.apk; plugin restored and normal gate PASS. Logs: build/phase13-host-evidence/t13-split.log and t13-size.log |
| R10 | `PERF_CLEAR=1 DEVICE_CLASS=reference make perf-timeline` | Open: reference 120 Hz device only; repeat with DEVICE_CLASS=lowEnd on the selected low-end device |

Host metadata: 2026-11-22, c8e7cfb3-dirty, pixel / sdk_gphone64_arm64, Android 16 / API 36, 60 Hz, GPU host, animator scales 1, host on AC power; benchmarkRelease, Partial(Require), bounded-swipe-v3. R2 is light/font 1.0, R3 dark/font 1.0, R4 light/font 1.3. Values below are in R2/R3/R4 order when grouped; emulator rows are never device claims. The history's first-frame field is median/max, while the budget table explicitly labels mean/max.

Source logs: `build/phase13-host-evidence/t13-tl.log`, `t13-tldark.log` and `t13-tlfont.log`. Raw JSON, XML and Perfetto traces:

| Run | Evidence archive |
|---|---|
| R2 | `build/perf/timeline/20261122T093253Z-c8e7cfb3-dirty-light-1.0-bounded-swipe-v3.vOw4FM/` |
| R3 | `build/perf/timeline/20261122T094541Z-c8e7cfb3-dirty-dark-1.0-bounded-swipe-v3.oYNNNv/` |
| R4 | `build/perf/timeline/20261122T095831Z-c8e7cfb3-dirty-light-1.3-bounded-swipe-v3.hdK2eS/` |

Each archive retains `androidTest-results/`, `connected_android_test_additional_output/` and its `row.md`. All five TimelineBenchmark methods pass in each run; no retry/failure or ±25% flag appears in the supplied logs.

| Budget row | Source | Phase 13 value | Verdict |
|---|---|---|---|
| Today view, JVM | TodayViewBudgetTest, serial make timing | Ten large60 samples 0.503375–0.5565 ms | PASS, every sample ≤ 20 ms |
| Plan preparation, JVM | TimelinePreparationBudgetTest, make timing | S3/large30/large60 max 4.2/5.0/8.1 ms | PASS, limits 40/100/200 ms |
| DayDrawing build | DayDrawingBudgetTest, make timing; real TextPaint guard | Fake metrics below; real TextPaint 2 cases PASS | PASS, JVM/Robolectric guards |
| Drawing cache | DayDrawingCacheTest / quick | 8 cases PASS, 0.118 s suite | PASS |
| Invalidation | TimelineInvalidationTest + ListInvalidationTest / quick | 13 + 124 cases PASS, 0.763 + 7.233 s suite | PASS, including both within-day scroll guards |
| First large60 frame | timelineFirstFrameMs each | R2/R3/R4 maxima 100.9 / 97.8 / 97.3 ms; means 93.2 / 83.7 / 84.6 ms | PASS, every sample ≤ 800 ms |
| CPU frame P50/P90/P99 | timelineScroll frameDurationCpuMs | R2 3.6/4.6/6.4; R3 3.6/4.8/6.3; R4 3.6/4.8/6.5 ms | PASS emulator CPU-band checker; P50 REPORT |
| Overrun P99 | timelineScroll frameOverrunMs | R2/R3/R4 −4.9 / −4.8 / −5.0 ms | REPORT, emulator |
| Jank percentage | positive-overrun frames / frames | Mean/max 0.0/0.0% in R2, R3 and R4 | REPORT, emulator |
| Hitch ratio | positive overrun / active frame time | R2/R3/R4 maxima 0.1 / 0.3 / 0.0 ms/s | REPORT, emulator |
| Placeholder episodes | timelinePlaceholderCount maximum | 0 in R2, R3 and R4 | PASS, ≤ 6 per emulator iteration |
| Timeline builds on main | timelineBuildOnMainCount | 0 in R2, R3 and R4 | PASS, hard 0 |
| Main-thread frame P50/P99 | timelineMainFrameMsP50 / timelineMainFrameMsP99 | R2/R3/R4 iteration maxima 0.8/2.9, 0.8/2.9, 0.8/3.0 ms | REPORT, emulator |
| RenderThread frame P99 | timelineRenderThreadMsP99 | R2/R3/R4 iteration maxima 4.5 / 4.6 / 4.7 ms | REPORT, emulator |
| TimelineTargets composition/day | timelineTargetsMaxMs | R2/R3/R4 maxima 0.4 / 0.2 / 0.5 ms; lower bounds excluding pausable-prefetch bodies | REPORT, emulator |
| First-frame breakdown | timelineDecodeMs / timelinePrepareMs / timelineTypefaceMs | R2 maxima 9.0/17.1/1.0; R3 9.8/16.5/1.0; R4 13.3/15.1/1.0 ms | REPORT, emulator |
| Jump to last day | timelineLastDay | PASS in all three runs; helper enforces ≤ 6 bounded gestures, actual gesture count is not emitted | PASS emulator functional guard |
| Peak memory, large60 | memoryRssAnonMaxKb maximum + growth | R2/R3/R4 maxima 112,024 / 113,548 / 113,364 KiB; growth 8 / 72 / 152 KiB | PASS, ≤ 153,600 KiB; growth below 40 MiB warning |
| List and wide scroll | listScroll / timelineScrollWide | R2/R3/R4 list CPU 3.4/4.4/5.8, 3.4/4.4/5.9, 3.4/4.4/6.0 ms; wide CPU 3.4/4.3/6.2, 3.4/4.4/6.6, 3.4/4.2/6.2 ms | PASS execution/required metrics and wide emulator CPU checker; CPU is P50/P90/P99 |

Layer 4 device review: **open: device only (OQ-A22)**. Before the first production upload, one passing reference 120 Hz run and one passing low-end run are required, or the user's recorded OQ-A45 waiver. Prepare airplane mode, battery saver off, charge above 50%, cooled device, animator scales 1; record the actual refresh rate. The host handles device selection and leaves pixel available afterwards.

A hard miss receives one immediate repeat of the same configuration, with both rows retained; a repeated miss becomes a fix task. The checker already handles the emulator CPU-band retry. No missing metric, over-budget result or absent physical evidence is a PASS. The original profile collection is verified by c8e7cfb3 and the successful Require runs; the launch-clock fix 89f3dd7f requires a refresh. Performance budget remains unchecked. The host standalone make check and corrected minified smoke pass, and R9 quick median is 44.33 s. The third timed check failed the early launch-clock deadline, so the check series has only two passing times (245.89 / 284.79 s), no three-run median, and no regenerated tier-timings.json. Post-89f3dd7f launch/profile verification and physical device rows remain open; see release.md for exact commands.


Earlier JVM evidence (2026-11-22, source 17527208 plus documentation, local macOS host, JDK 21, debug JVM/Robolectric): `make quick` ran the serial timing tier last (7 cases, 4.09 s tier wall). The whole command took 102.34 s; this exceeded the warm target; the later host three-run quick median is 44.33 s (PASS), while the incomplete check series keeps R9 open. Raw timing XML: `core/plan/build/test-results/timingTest/`; timing budget report: `build/test-results/timing/timings.json`. No individual test budget overrun occurred.

| DayDrawing width dp | Slowest day median ms | Day maximum ms (report) | Total median ms | Maximum drawing bytes |
|---:|---:|---:|---:|---:|
| 360 | 0.07 | 0.10 | 0.9 | 11,795 |
| 411 | 0.04 | 0.06 | 0.6 | 11,795 |
| 841 | 0.05 | 0.05 | 1.3 | 14,089 |

All widths pass ≤ 1 ms median/day, ≤ 5 ms total/width and ≤ 512 KiB/drawing. `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make targeted ONLY='feature:trip:TextPaintDayDrawingBudgetTest'` separately passed its two real TextPaint cases in **5.51 s tier wall** (1.688 s suite including class initialization). Reported real-metric day maxima: 0.57 ms at 841 dp and 0.08 ms at 411 dp; total pass times 14.9/2.0 ms; drawing bytes 14,155/11,795. Fake and real metric results are different harnesses. No renderer, clock scope or drawing allocation path changed in Phase 13, and no device/perf tier was run by the worker.


Additional R2/R3/R4 values: list peak RSS maxima **112,896 / 112,816 / 112,772 KiB**; wide peak RSS maxima **112,176 / 112,196 / 112,856 KiB**. Wide dark jank mean/max is **0.2/0.7%**, hitch **0.4/1.1 ms/s**; light and large-font wide jank/hitch maxima are zero. List overrun P99 is **−5.4 / −5.3 / −5.3 ms**. All are emulator reports; peak RSS passes the shared 153,600 KiB limit. First-frame TTID mean/max is **132.5/143.3, 118.2/129.5, 119.9/140.5 ms**. No new physical-device performance claim is made.
