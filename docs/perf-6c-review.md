# Phase 6c performance investigation (2026-11-19)

Current outcome: P1 is closed by the matched A/B below. P2 profile generation now completes successfully; see "Successful profile generation" at the end of this report. Earlier failures are retained as history.

## Matched A/B (orchestrator)

On pixel at 12:06–12:18 on 2026-11-19, the orchestrator ran 6b (`87e4d7a`), 6c (`e418883`), then 6b again. CPU P50/P90/P99 was **6.4/8.9/16.6 ms**, **6.6/8.7/11.9 ms**, and **6.4/8.6/13.4 ms**, respectively. Unchanged 6b reproduces the elevated costs, confirming an environment change consistent with the launcher/SurfaceFlinger evidence. **P1 is closed as environmental; no app code change is needed.** The earlier investigation below is retained as history; its open P1 status is superseded by this matched comparison.

## P1: original frame-cost comparison (historical)

These are analyses of the orchestrator's existing pixel traces, not new performance measurements or a gate pass. The CPU band remains unchanged. No app-level cause has been attributed; this investigation does not establish that the 6c diff is responsible for all of the observed increase.

Sources under `build/perf/timeline/`, each `TimelineBenchmark_timelineScroll_iter000*.perfetto-trace`:

- `20261119T030901Z-9c60131-light-1.0-bounded-swipe-v3.MA0uVc` (6b).
- `20261119T095549Z-4b036c7-light-1.0-bounded-swipe-v3.pR7qS5` (6c, first).
- `20261119T100443Z-4b036c7-light-1.0-bounded-swipe-v3.tnloBZ` (6c, recheck).

Parsed with Perfetto Python 0.59.1 and its SHA-verified mac-arm64 v59.1 trace processor, installed only in `build/phase6c-perf-review/`. Reproduction scripts, exact SQL, trace paths and JSON results are there (`inspect_traces.py`, `deeper.py`, `control_window.py`). App rows join `slice` → `thread_track` → `thread` → `process`, restricted to `process.name = 'net.chronoflow.app'`; frame rows select main-thread `Choreographer#doFrame%` and RenderThread `DrawFrame%`. Child rows join `child.parent_id = frame.id`. Durations are nanoseconds divided by 1e6; percentiles use Perfetto SQL `percentile(dur, 99)`.

The reusable frame/child/control queries are committed in [phase-6c-frame-breakdown.sql](perf/phase-6c-frame-breakdown.sql).

| Trace slice | 6b | 6c first | 6c recheck |
|---|---:|---:|---:|
| Main doFrame P99, ms | 2.971 | 5.973 | 6.036 |
| RenderThread DrawFrame P99, ms | 4.668 | 9.052 | 8.920 |
| Main traversal mean, ms | 0.543 | 0.886 | 0.898 |
| Main input mean, ms | 0.243 | 0.394 | 0.394 |
| Main animation mean, ms | 0.305 | 0.621 | 0.643 |
| Main Record View draw mean, ms | 0.105 | 0.208 | 0.216 |
| AndroidOwner measure/layout mean, ms | 0.160 | 0.309 | 0.322 |
| RT flush commands mean, ms | 1.312 | 1.940 | 1.961 |
| RT swap buffers mean, ms | 0.909 | 1.908 | 1.854 |
| RT prepareTree mean, ms | 0.027 | 0.059 | 0.059 |

The change is not localized to an added Compose operation. Lazy prefetch composition/measurement each remain **81 calls**; text node measurement remains **211 calls** and text layout **252 calls**. RT prepare/execute operation batches stay close to two per drawn frame. AAConvexPath operations are 73,370 / 72,128 / 72,972 over 2,123 / 2,101 / 2,110 drawing slices. FillRect operations rise from about 13.9 to 15.9 per drawing slice; that modest count increase cannot by itself attribute the much larger duration changes across unrelated operations. JIT slices total only 6.06 ms versus 14.41 ms in the first 6c trace; GC slices total 50.90 versus 66.72 ms across the whole trace.

The same traces also show higher costs in processes outside the app:

| System control, mean ms | 6b | 6c first | 6c recheck |
|---|---:|---:|---:|
| Launcher input | 0.1019 | 0.1717 | 0.1728 |
| Launcher gesture monitor | 0.0954 | 0.1607 | 0.1598 |
| Launcher taskbar drawing | 1.9394 | 3.2565 | 3.0032 |
| SurfaceFlinger prepareFrame | 0.0842 | 0.1499 | 0.1497 |
| SurfaceFlinger postComposition | 0.0646 | 0.0951 | 0.0953 |

Scheduler Running time also increases: app main 2,864 → 5,045 ms, app RenderThread 6,220 → 10,344 ms, SurfaceFlinger main 1,595 → 2,675 ms (6b → first 6c). This is evidence of a broad cost increase, not proof of a particular host condition. This analysis does not establish a host-frequency cause. The two 6c runs agree, and their failed band is not dismissed or reset.

Code audit of `d5a4436..4b036c7`: timeline items still use `dayTimeline`, the same DayTimeline arguments, off-main builder and cached drawing layer. The list-row branch is conditional on list mode and is not emitted into the timeline DSL. No list swatch, priority tag or zone-card draw path runs in timeline mode. The compact header's timeline branch still draws LaneHeadings. Fetch/path state is not a per-frame flow in the perf fixture, and the existing invalidation tests cover scroll-time composition. No speculative runtime optimization or unrelated renderer fallback was made to hide an unattributed band miss.

## P2: bounded generation and interrupted input

The original profile XML timed out at 900000 ms inside synchronous injection from the **timeline** loop (`BaselineProfileGenerator.kt:60`), before list mode. That stack alone does not prove an infinite Binder call. Device logcat also shows repeated activity lifetimes around 98 seconds and an interrupted VSync wait when the cap fired.

`BoundedProfileInput` bounds each named action to 10 s, a journey to 120 s, and the complete collector to 600 s. A daemon worker keeps an uninterruptible Binder call from holding the test thread. Four JVM tests pass, including an interrupt-resistant blocked worker (maximum 0.109 s; targeted tier 4.21 s).

The single permitted `make baseline-profile` attempt failed in 1m 43s. Startup failed the initially introduced strict five-attempt policy (79.822 s); the timeline case then failed fast at `summary swipe rejected` (4.198 s). That stricter collection policy was a mistake and has been restored to the existing maximum 15 / three-stable / non-strict policy.

The timeline rejection has explicit device evidence: InputDispatcher logged `Invalid DOWN event - pointers already down` for injected device -1, pointer 0. An interrupted gesture can outlive its instrumentation process. The revised setup sends an asynchronous, flagged ACTION_CANCEL for that pointer before injecting new gestures; rejection when no pointer is held is harmless. The event format follows the [AOSP input verifier](https://android.googlesource.com/platform/frameworks/native/+/4f463a6b1de9198963dc6aff74154a504ba3f8f6/libs/input/rust/input_verifier.rs).

The generator now uses three bounded timeline gestures, asserts it covered the first six-day tour, then uses three list gestures and toggles back. Each gesture still uses five fixed 26-step drags. A JVM fixture guard passes and verifies that this first tour contains every block-type/priority style used by large60. The generation script requires DayListRowKt as well as the existing page classes. The measured timelineScroll journey retains all 15 gestures and its day-index assertions.

The profile command is not repeated under the user's one-invocation limit. No regenerated profile is committed from the failed run. Final device and host validation results follow in the phase brief; P1 is not marked fixed without evidence.

## Single authorized timeline diagnostic at 6eca046

One `TimelineBenchmark#timelineScroll` invocation, under `Scripts/device_lock.py`, with perf preparation and a functional-preparation exit trap. Device: pixel/emulator-5554, API 36, host GPU, en/light/font 1.0, animator scales 1, benchmarkRelease, `Partial(Require)`. Command:

```sh
./gradlew --console=plain :benchmark:connectedBenchmarkReleaseAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.TimelineBenchmark#timelineScroll' \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR \
  -Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000
```

Functional result: **PASS**, 235.583 s test time, Gradle 4m 21s. All three iterations report day index 0 → 39 after the existing 15 gestures. CPU P50/P90/P99 is **6.400 / 8.917 / 14.013 ms**, above the requested P90 ≤ 6.1 / P99 ≤ 8.0 band. Main-thread P99 runs are **5.635 / 6.219 / 6.434 ms**; RenderThread P99 **7.963 / 9.314 / 8.612 ms**. This diagnostic is not a full-suite gate pass and is not appended to the performance history. **P1 remains blocking.** No app runtime change was made in this review, so no code-level performance fix is claimed.

Its iter000 system controls remain elevated: launcher input mean 0.2104 ms, gesture monitor 0.1936 ms, taskbar drawing 2.7326 ms; SurfaceFlinger prepareFrame 0.1559 ms and postComposition 0.0936 ms. CPU-frequency counter values span 1–2 in all four analyzed traces; this is not evidence of a physical host-frequency change. Raw results, all three traces, command and logs are in `build/phase6c-perf-review/timeline-run/`; control queries/results are in `current-controls.json` and `*-frequency.json`.

No additional device run is made. Attribution needs a matched comparison of 6b and 6c under the current emulator/host conditions; the current one-invocation authorization does not cover that comparison. No band threshold or baseline is changed. The corrected profile generator also requires a future orchestrator run: its one authorized invocation has already been used.

App/trip lint passes (29 s). The attempted command `./gradlew --console=plain :benchmark:lint :feature:trip:lintDebug :app:lintDebug` fails during task resolution because AGP's test module exposes no such lint task: `Cannot locate tasks that match ':benchmark:lint' as task 'lint' not found in project ':benchmark'.` The app/trip pair is then run once successfully; benchmark Kotlin compilation already passes. Logs are retained in `build/phase6c-perf-review/`.

The single final `make quick` passes: 3,321 JVM/Robolectric tests in 35.64 s tier wall, 37 build-logic tests in 1.17 s, tooling suites 138 / 25 / 49 / 9 tests in 18.148 / 0.125 / 0.018 / 0.046 s, and six serial timing gates in 1.84 s. No test budget overrun is reported. Both timeline and list invalidation guards pass. Final evidence is archived in `build/phase6c-perf-review/final/`. Profiles and performance history remain unchanged, and pixel remains running.

## P2: collection budget correction

The orchestrator's next `make baseline-profile` at e418883 completed startup in **96.089 s**, then failed timeline collection at **599.898 s**, for **11m 41s** overall. Logcat shows six startup launches at intervals **16.725–17.118 s** (median **16.877 s**) and fourteen timeline launches at intervals **44.234–45.651 s** (median **45.013 s**). Timeline was making progress; a fixed 600 s collector could not accommodate its configured fifteen attempts plus ART profile flushing and compilation.

`ProfileCollectionBudget` now caps both collectors at **three attempts**, stopping early after **two stable iterations**. AndroidX Benchmark 1.5.0 supports this policy; its existing non-strict behavior is preserved. The journey remains three timeline gestures plus three list gestures, with the existing first-tour style coverage guard and generated-profile descriptor checks. Each journey remains capped at **120 s**, each input at **10 s**, and the collector deadline is derived as **60 s setup + 3 × (120 s journey + 30 s ART/teardown) = 510 s**. The 900 s instrumentation cap stays as a later backstop. A fake-clock JVM test proves the last allowed journey and ART flush can run after earlier iterations consume their full budgets, including a fifteen-iteration configuration that would fail with the previous fixed deadline.

`CfProfile` logcat entries now record begin/end and elapsed milliseconds for every startup and timeline iteration. Original XML/logcat, downloaded AndroidX source and new validation results are retained under `build/phase6c-profile-final/`. Required descriptors (TripPage, DayHeader, SummaryCard and DayListRow) remain mandatory; lowering the attempt cap does not bypass those checks.

## Successful profile generation

At source commit **02c8db7**, the first authorized `make baseline-profile` attempt **passes**, exit 0, **194.80 s tier wall / Gradle 3m 12s**. Device: pixel/emulator-5554, API 36, host GPU, nonMinifiedRelease collection. Both baseline and startup profiles are regenerated. The script verifies every required page/list descriptor; no second attempt is used. This is profile-generation evidence, not a new timeline performance judgement.

| Case | Iterations | Journey times, s | Complete test time, s |
|---|---:|---|---:|
| Startup | 3 | 4.688 / 4.191 / 5.097 | 45.001 |
| Timeline + list | 3 | 33.718 / 32.400 / 32.885 | 131.445 |

The complete test times include ART export/compilation and collection overhead. Every journey stays within its 120 s cap and both collectors finish within their derived 510 s deadline. The regenerated baseline has **10 DayListRow rules**, versus **0** in the prior committed profile. `BaselineProfileFilesTest` now also guards the list-mode descriptor; the existing startup/timeline descriptor guards remain. All **three tests pass** in **3.61 s tier wall** (0.044 s total test time). The old profile's absent descriptor provides the cheap stale-profile negative control.

Final validation: the single `make quick` **passes** 3,324 JVM/Robolectric cases (34.58 s tier), 37 build-logic cases (1.07 s), all 221 tooling cases (138 / 25 / 49 / 9 in 17.804 / 0.123 / 0.019 / 0.045 s), and all six serial timing gates (1.82 s), with zero budget overruns. App/trip lint passes once (15 s); benchmark Kotlin compilation passes (1 s). The two collection-budget JVM guards pass (2.46 s tier, 0.002 s each).

Raw profile output, XML/logcat, targeted results and final host checks are archived under `build/phase6c-profile-final/`. No `make perf-timeline` or other device test tier is run in this repair. No app runtime change is made for P1, and no CPU-band baseline or threshold changes. Pixel remains running.
