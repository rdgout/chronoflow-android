# Startup performance

Cold start and opening animation of the native app. Emulator numbers are regression signals, never device claims (OQ-A22).

## How to run

`make perf` boots nothing: it needs the `pixel` emulator (`make emu`). It runs `StartupBenchmark` and `LaunchJankBenchmark` on the
`benchmarkRelease` variant through `Scripts/perf-startup.sh`, checks the results with `Scripts/check-benchmarks.py` and
appends TTID and launch-metric rows to the tables below. Animator scales are set to 1 for the run and restored afterwards.

## Budgets

| Metric | Reference device | Low-end device | Emulator |
|---|---|---|---|
| Cold start, time to initial display (median, `StartupTimingMetric`) | 400 ms hard | 800 ms hard | report only |
| Activity onCreate → first overlay draw (`first-commitFirstMs`, median) | 50 ms hard | 100 ms hard | report only |
| Opening frame overrun P99 (`frameOverrunMs`) | 0 ms hard | 4 ms hard | report only |
| Opening jank (`frameOverrunMs` > 0) | ≤ 5% | ≤ 5% | report only |
| Animated frames per launch iteration (`frameCount`) | report | report | minimum 75 |

The reference and low-end gates stay open until a device exists (OQ-A22).

## Runs

| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Build | TTID ms med/min/max | Result |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-17 | adeeb8e-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(UseIfAvailable) | 88.7/79.9/97.1 | REPORT |
| 2026-11-17 | 0342ec9-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 95.0/87.5/108.5 | REPORT |
| 2026-11-17 | bfa817f-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 95.8/91.5/108.2 | REPORT |
| 2026-11-17 | 8b5db6a-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 121.2/113.7/165.2 | REPORT |
| 2026-11-17 | 072aff0-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 96.4/90.9/116.3 | REPORT |
| 2026-11-17 | d47ae14-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 100.5/93.5/107.3 | PASS |

Device rows: open: device only (OQ-A22).

## Phase 2 comparison

Worker, 2026-11-17, Apple M4 Max host; `make perf`, pixel / emulator-5554, Android 16 API 36, host GPU, 60 Hz, English, light, font 1.0, `benchmarkRelease`, `Partial(Require)`. One invocation, exit 0, 44.41 s wall (Gradle 41 s), 10 cold iterations. Median/min/max **95.8 / 91.5 / 108.2 ms**. Archive: `build/perf/startup/20261117T075109Z-bfa817f-dirty-light-1.0.ZHHU2d/`; JSON, all ten traces and XML are retained. Source was committed at bfa817f; `-dirty` includes pending documentation, requested snapshot references and the unrelated untracked phase briefs.

The historical comparison against 0e is **+8.0%** over 88.7 ms. In Phase 2 review round 1 the user selected M1's **95.0 ms Partial(Require)** as the like-for-like reference: Phase 2's **95.8 ms is +0.8% and passes the 5% limit** (99.75 ms). The intervening M1 result was already 95.0 ms with `Partial(Require)`; Phase 2 is **+0.8%** against that immediate predecessor. The 0e row used `Partial(UseIfAvailable)`. These are distinct historical conditions, not evidence that the theme caused or repaired the difference. The M1 reference is recorded in OQ-A66; no repeated benchmark was used to seek a passing sample. These are emulator regression signals, not physical-device claims.

`make baseline-profile` passed once in 467.87 s wall (Gradle 7m45s); both startup and large60 paths completed, including the existing bounded-swipe-v3 progress assertions. Regenerated baseline/startup outputs are committed in bfa817f. The current StartupBenchmark contains only startupCold (10 iterations); the older command description's warm-start case is absent from the landed harness and was not invented in this phase.

## Phase 3a results

**Allocation guard resolved by the orchestrator-approved minimum-round rule (7a14c67).** The normal in-process test warms 20,000 calls, then measures five rounds of 10,000 samples with ThreadMXBean. Each round subtracts its own back-to-back-read empty baseline; the minimum must be zero beyond that baseline. The constant 72-byte one-time VM/Kotlin cost therefore does not masquerade as a per-call sampler allocation. The unused subprocess probe and the two added plan checkboxes are removed. Production sampler code is unchanged.

The local, uncommitted planted `System.identityHashCode(listOf(seconds))` failed with **[480000, 480000, 480000, 480000, 480000] bytes beyond empty baseline**: minimum 480,000, **6.47 s wall / 0.066 s test**. The source was restored in a finally block. The exact restored targeted method then passed in **1.87 s wall / 0.064 s test**. Logs/XML: `build/phase3a-validation/allocation-minimum-{planted,targeted}.{log,xml}`. No planted allocation or diagnostic helper remains in the committed tree.

The requested single `make quick` invocation **passed in 29.28 s wall**, and the single `make check` invocation **passed in 68.23 s wall**, including its quick prerequisite, lint/release checks, layout guards, positive generators-check and pixel smoke tests. Logs: `build/phase3a-validation/allocation-minimum-{quick,check}.log`. Every resulting timing tier reports zero over-budget cases. Both code and documentation commits sign normally after the orchestrator restarted the agent; no unsigned fallback was used. It did not repeat `make perf`, `make perf-timeline` or `make baseline-profile`, as instructed while the host is busy. Quiet-host performance scheduling remains with the orchestrator.

Previously completed implementation, captures, startup measurement, regenerated profiles and the earlier `make check` pass are recorded below. **The timeline timing comparison is INVALID for judgement**, as confirmed by the orchestrator: other builds and simulators were running on this Mac. Its functional cases and 1 px marker passed. Budgets were retained. See `docs/perf-timeline.md` "Phase 3a: 1 px presented marker". Quiet-host timeline validation is pending with the orchestrator; no timeline-performance conclusion is drawn from the busy-host measurements.

| Validation | Result | Wall time | Evidence |
|---|---|---|---|
| Remaining launch device targets | 8/8 passed, every method < 10 s | 12.52 s | `device-final.log` |
| SplashScreen JVM captures | 33 PNGs, viewed | 15.34 s | `captures-jvm.log` |
| SplashScreen device captures | 19 PNGs, viewed | 22.50 s | `captures-device.log` |
| Startup + launch jank | passed emulator gates; timing report-only | 104.55 s | `perf-startup-fix1.log` |
| Timeline | functional cases passed; timing judgement INVALID (busy host) | 640.93 s | `perf-timeline.log` |
| Baseline + startup profiles | passed, generated files committed | 410.53 s | `baseline-profile.log` |
| Standalone lint | initial source-manifest diagnostic corrected; scoped lint passed | 53.94 s + 7.71 s | `lint.log`, `lint-fix1.log` |
| Pre-pause standalone quick | initial fixture-loop timing violation; scoped fix passed 9 cases | 19.30 s + 3.73 s | `quick.log`, `fixture-split-fix1.log` |
| Pre-pause complete check, including its quick prerequisite | passed | 89.17 s | `check.log` |

Logs and local proof artifacts live under `build/phase3a-validation/`; generated build output, PNGs and recordings are not committed. During the first validation window, the standalone quick invocation was not repeated. Its initial eight-fixture decoder loop took 1,239 ms against the 1,000 ms unit budget; 71a7111 splits the fixture cases and retains the separate large60 guard once. The scoped cases took 0.002–0.147 s. Check then passed its entire quick prerequisite (960 tests, 9.76 s), 26 build-logic tests (1.89 s), 185 script/generator tests, six serial timing cases (2.39 s), 30 release checks (6.86 s), 289 layout cases (25.34 s), positive generators-check and 12 pixel smoke tests (23.09 s). All timing tiers report zero over-budget cases. The initial lint diagnostic was the source Startup provider declaration; 74967fb makes `exported=false` explicit, matching the already-private merged provider. No lint suppression or budget exception was added.

Every mandated validation command was executed; there is no unrun tier to substitute with an assumed pass. The busy-host timeline command produced the exact output: `make: *** [perf-timeline] Error 4`. Its two measured P90/P99 pairs are 9.1/57.8 and 11.5/113.1 ms against the retained 8.1/11.0 ms limits. The orchestrator will repeat the timeline comparison on a quiet host. Device performance claims remain open under OQ-A22.

| Task | Implementation commits / status |
|---|---|
| 1: Bezier and clock | 8447437, done |
| 2: layout and flight path | be5c1bc, done |
| 3: opening timeline | 1c4e36e, done |
| 4: wordmark | 8addaef, done |
| 5: system theme | 20a6d34, done |
| 6: overlay | c1b0617; wall-clock correction 90d9ea7, done |
| 7: activity/root | 75d88ef; root ordering 6886226, done |
| 7b: system-bar icons | 941d92a, done |
| 8b: numeric dp/sp gap and marker | 8284e75; rule and 1 px marker pass; quiet-host timing validation pending |
| 8: geometry guards/captures | 10f77e1, 590a7b2; guards and capture review done |
| 9: frame-zero parity | 79d0bcd, ff7a573, 337b0e1; both appearances pass |
| 10: benchmarks/initializers/profiles | e21192e, 8b5fbe6, 8b5db6a, 392319f; done |
| 11: recordings, OQs and evidence | this final documentation change; done, including the invalid busy-host timing record |

## Launch metrics

| Date | Commit | Class | Device | Hz | Build | First commit median ms | Overrun P99 ms | Frame count min | Jank % mean/max | Result |
|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-17 | 337b0e1-dirty | emulator | unknown | 60 | benchmarkRelease, Partial(Require) | missing | missing | missing | missing | FAIL |
| 2026-11-17 | 8b5db6a-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 49.6 | 26.1 | 89.0 | 67.8/96.8 | PASS |
| 2026-11-17 | 072aff0-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 37.6 | 13.4 | 127.0 | 60.0/72.4 | PASS |
| 2026-11-17 | d47ae14-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 38.5 | 9.3 | 191.0 | 93.5/100.0 | PASS |

## Phase 3a: startup measurements

The first `make perf` stopped before tests at `StartupBenchmark.kt:28`, `This Metric API is experimental.` (11.26 s wall); the opt-in is fixed in 8b5db6a. The missing-metrics FAIL row records that build failure, not a measured run. The scoped correction ran `make perf` once and **passed in 104.55 s wall** (Gradle 1m41s), pixel / emulator-5554, API 36, host GPU, 60 Hz, font 1.0, English/light, benchmarkRelease, Partial(Require), scales 1 with functional settings restored afterwards. Archive: `build/perf/startup/20261117T103220Z-8b5db6a-dirty-light-1.0.R1FUmi/`; raw JSON, XML and all 20 traces are retained. `-dirty` includes the prepared profile generator and documentation/untracked phase briefs.

Cold TTID median/min/max **121.2 / 113.7 / 165.2 ms**, **+27.6% against OQ-A66's 95.0 ms** reference (+26.5% against Phase 2's 95.8 ms). This observed shared-host delta is reported, not a 5% gate or a causal estimate of overlay cost. `first-commitFirstMs` median **49.6 ms**, from TraceSectionMetric directly; no fallback query metric was needed. Launch frame overrun P99 **26.1 ms**; frame counts **92,89,92,93,93,91,91,91,91,92**, every iteration above 75. Jank mean/max **67.8% / 96.8%**, report-only on the emulator. These are emulator regression signals and do not establish physical-device smoothness; the device budgets remain open under OQ-A22.

The first startupCold trace (iteration 000, selected by index) contains App Startup total **1.8073 ms**, `ProcessLifecycleInitializer` **0.4700 ms**, `ProfileInstallerInitializer` **1.0727 ms**, async `first-commit` **79.7297 ms**, and `cf.launch.moonDecode` **15.8166 ms**. Query evidence is `build/phase3a-validation/startup-slices.txt`. Moon decoding in this trace is below the brief's 16 ms IO-switch threshold. The merged manifest guard separately restricts the initializer allowlist to those two classes.

`make baseline-profile` passed once in **410.53 s wall** (Gradle 6m44s), regenerating both profiles with the startup opening journey and large60 timeline journey. The startup collection took **57.281 s**, and large60 **327.456 s**. The committed outputs contain **19,784 baseline lines** and **9,590 startup lines**; each contains 113 entries matching CfRoot/LaunchOverlay/LaunchScenePainter. The generator waits for launch.opening, then captures another bounded 1.6 s, covering the complete opening. Log: `build/phase3a-validation/baseline-profile.log`.

## Phase 3a: measured rim policy and passing parity

Orchestrator brief e2ce2ba replaces the unmeasured all-pixel tolerance with the user-approved rim policy. LaunchFrameZeroTest now reads two padded pixel buffers, classifies a pixel as edge if any RGB channel spans > 32 across its 3 × 3 neighbourhood in either image, and applies ≥ 99.5% within 4/255 and none above 16/255 only to flat pixels. It separately checks distance-weighted ink centroids (distance > 64/255 from skyTop) within 0.5 px on each axis. No per-pixel collections or diagnostic strings are allocated in that scan; the two buffers are bounded by the 192 dp circle. OQ-A74 mirrors this rule. Rendering, scale and placement remain unchanged.

The requested local, uncommitted **1 px shift proof failed**, as required: sampling the overlay one pixel to the left shifts its comparison ink right. The x centroid changed from **−10.0656132263 to −9.0884692390 px** relative to the display centre, a **0.977144 px** error against the 0.5 px limit. Command `make targeted ONLY="app:LaunchFrameZeroTest#frameZeroMatchesSystemSplashDark"`, **20.82 s wall / 1.408 s test**, expected failure `AssertionError: moon ink centroid x`. Log/XML: `build/phase3a-validation/parity-shift-proof.{log,xml}`. The offset was restored before the passing run and is absent from the committed test.

`make targeted ONLY="app:LaunchFrameZeroTest app:LaunchStrictModeTest app:AppLaunchSmokeTest app:ConfigurationPolicyDeviceTest"` then **passed all eight tests in 12.52 s wall**: dark parity **1.360 s**, light parity **0.992 s**, StrictMode **0.362 s**, seedless native launch **0.232 s**, and all four configuration tests. Every method was below 10 s. Log: `build/phase3a-validation/device-final.log`. The earlier icon-size/rim stop reports above are historical and resolved by the orchestrator's corrected measurements and this passing result. Task 9 is now ticked.

## Phase 3a: capture comparison

`make captures ONLY=SplashScreen` passed once, **15.34 s wall**, producing **33 PNGs** at `build/captures/SplashScreen_{ready-0000,0300,0700,1000,1100,1400,1500,reduceMotion-0000}_{pixel8,small}_en_{light,dark}.png` plus `SplashScreen_ready-0000_pixel8_nl_light.png`. `make captures ONLY=SplashScreen DEVICE=1` passed once, **22.50 s wall**, producing **19 PNGs** in `build/captures/device/`: the same eight states per theme on pixel, one Dutch frame zero, and `SplashScreen_system-bars_pixel_en_{light,dark}.png`. Logs are `build/phase3a-validation/captures-{jvm,device}.log`.

Every PNG was viewed in `capture-jvm-review.png`, `capture-device-review.png`, `capture-remaining-review.png` and `system-bars-review.png` under `build/phase3a-validation/`, and compared with the SplashScreen/SplashSpec board definitions. At 0/300 ms and with Reduce Motion only the moon and flat sky show; 700 ms shows the rising sun and partial flight path; 1000/1100 ms starts the wordmark; 1400/1500 ms holds the complete opening. No clipping or font-scale dependence was observed in the overlay. The light screen-profile status-bar crop has dark icons; the dark crop has light icons. Overlay status icons remain light. These are independent JVM/device renders, not cross-renderer golden comparisons.

Intentional board differences follow §5.1 and the pinned iOS rules: no stars; the moon is centred at cy (30 dp below the old 390×844 board centre), and the wordmark top is cy+40 (30 dp below its old board top). The band stays at cy+48 (470 dp at 844 dp tall). Geometry adapts to each window; the path follows arc-length samples rather than the CSS dash length; glows use the specified cached radial gradients rather than CSS blur/shadow. The opening stops at 1500 ms and holds, per 3a's scope. No additional unexplained composition difference was found in this source-to-PNG board review.

## Phase 3a: handover recordings

Recorded cold launches on pixel/API 36 with all animator scales at 1, using the measured benchmarkRelease APK (reinstalled explicitly because Gradle removes test APKs when its connected tier completes). Videos: `build/launch/launch-light.mp4` and `launch-dark.mp4`, 20,000,000 bit/s request, 4 s recording cap. ffprobe reports video r_frame_rate **60/1**, with **111 light** and **107 dark** decoded frames; screenrecord emits variable timestamps and does not repeat the final static frame. Frames are retained at `build/launch/{light,dark}/%04d.png`.

Reviewed full-window transition frames light **0015–0045**, dark **0015–0041**, including enlarged moon crops light **0025–0045** and dark **0021–0041**, and later opening frames 0066 and the last encoded frame. The normal Android launcher window expansion is visible before full-screen; no additional flash, moon jump or obvious sky banding was observed at the system splash → overlay switch. The slightly softer system rim is covered by the measured edge policy; drawing geometry is unchanged. The existing Compose gradient remains; no dither fallback was indicated by this review. Contact sheets: `build/phase3a-validation/record-{light,dark}-{handover,moon-review}.png`.

Recording preparation initially found the perf APK uninstalled (`Error type 3: Activity class ... does not exist`); installing the existing APK resolved it. ffmpeg 9 rejects the removed `-vsync` option; extraction used `-fps_mode passthrough`. The successful light video was reused rather than re-recorded for that tool-option correction. Raw logs: `record-launch.log`, `record-launch-fix1.log` and `record-launch-dark.log` in `build/phase3a-validation/`. Global theme and animator settings were restored to the functional defaults afterwards.

## Phase 3a: planted-failure proofs

The local 1 px shift proof is recorded above (centroid error 0.977144 px, expected failure), and absent from the committed test. Earlier isolated allocation measurements picked up one-time VM/Kotlin strings and byte arrays; a constant 72 bytes remained even with longer warmup. The user approved the five-round minimum measurement to distinguish this from allocations on every call. The resulting guard and the 480,000-byte-per-round planted proof are recorded at the start of this section's Phase 3a results. It retains the zero-byte steady-state criterion and runs in-process; the obsolete subprocess probe is deleted.

A local comment hand-edit to LaunchWordmark.generated.kt made `make generators-check` fail in **5.26 s** with the precise message `tools/assets/assets.sha256: hash mismatch: feature/launch/src/main/kotlin/net/chronoflow/feature/launch/generated/LaunchWordmark.generated.kt`. The generator output was restored in a finally block. Log: `build/phase3a-validation/generator-stale-proof.log`. Positive generator verification passed in the final check run. Both planted source edits are absent from git diff.

## Resolved integration issues and decisions

The earlier RootThemeTest hang was a genuine mixed-clock bug: elapsed subtracted a System.nanoTime origin from a virtual Compose frame timestamp. 90d9ea7 awaits first draw before requesting frames, then uses frames only for pacing and injected nanoTime for elapsed. LaunchFrameLoopTests proves settlement at 1500 ms and no subsequent requests with clock bases that differ by trillions of nanoseconds. Non-overlay activity tests use profile screen. CfRootTest now observes explicit frame boundaries. b3b0797 also fixes DrawScope.size shadowing the scene's dp size, which had translated the moon offscreen. The original scoped logs remain under the evidence directory.

The held-splash dump (`parity-thread-dump.txt`) showed the test thread waiting in Instrumentation.startActivitySync → ActivityScenario.launch → ScreenTest.launch, before its controller wait, while main was in MessageQueue.nativePollOnce. 79d0bcd uses the same seeds through plain startActivity, awaits a placement signal without idling and finishes the activity explicitly. Idle-based waits resume only after splashHeld is false. The icon view is the platform's 192 dp visible circle; the overlay's decoded resource stays a 288 dp canvas. The measured softer system rim is covered by the orchestrator-approved policy, not by rescaling or changing the renderer.

The short-window sun guard follows user-approved OQ-A83: keep iOS geometry, apply containsSun only at heights ≥ 667 dp; shorter windows keep moon/wordmark containment and finite-value checks. Layout guards use the light-only LayoutMatrix; captures and paint checks cover both appearances. Moon decoding is constructor-supplied because the asset belongs to the app module; reset support is a debug-source AppGraph extension. The unused subprocess allocation probe is deleted; the replacement in-process guard uses the user-approved five-round minimum rule. Other behavior and geometry follow the pinned iOS/board rules described in the brief. OQ-A67 is resolved; OQ-A69–A83 keep their recorded defaults, A74 carries the measured parity policy, and A68 remains absent. No new OQ was added by final validation.

## Phase 3a review round 1: root background

Finding 1 is corrected by painting `CfTheme.colors.background` on the full `app.root` behind its children. This is the generated `CfLightColors.background` / `CfDarkColors.background` token (`core/designsystem/.../generated/CfColors.generated.kt`: light #F3F7FC, dark #0A0E1F), sourced from `theme.css`'s `--background` and used by `Trips.dc.html:24`. The window background remains launch_sky_top; the opaque overlay still covers the root while shown. OQ-A67 records this surface/icon pairing.

`make targeted ONLY="app:CfRootTest app:LaunchFrameZeroTest"` passed in **15.04 s wall**: six Robolectric root tests (including tagged root pixel checks in light and dark), plus both pixel-emulator frame-zero parity tests. `make captures ONLY=SplashScreen_system-bars DEVICE=1` passed in **17.28 s**, writing two PNGs at `build/captures/device/SplashScreen_system-bars_pixel_en_{light,dark}.png`. Both were viewed: dark icons now appear on the light app background, and light icons on the dark app background. Logs: `build/phase3a-review-r1/{root-targeted,system-bars-captures}.log`.

## Phase 3a review round 1: trace lifecycle

Finding 2 is corrected by pairing skipped-overlay spans at MainActivity's first frame commit and closing any remaining spans on activity destruction, including abandonment before first draw. The activity retains its own LaunchController reference for teardown. `LaunchTrace` keeps one process-wide guarded session, with an injectable `LaunchTracing` constructor seam and a pure `LaunchTraceSession`; no fake or reset hook was added to production sources. A first draw and normal opening still end their spans at the original points. Duplicate callbacks, later activity starts and destruction after settlement cannot emit another end.

`make targeted ONLY="feature:launch:LaunchTraceTests feature:launch:LaunchControllerTests app:RootThemeTest"` passed in **9.31 s wall**. Six new plain JVM fake-tracer cases cover skipped launch, recreation, abandonment before and after first draw, normal settlement/destruction, and skipped destruction before commit. Existing controller and seeded root-theme checks also pass. Evidence: `build/phase3a-review-r1/trace-targeted.log`. No perf/profile tier was repeated while the host is busy; quiet-host profiling remains with the orchestrator.

## Phase 3a review round 1: StrictMode assertion

Finding 3 removes the always-true positive-PID assertion from LaunchStrictModeTest. The seeded test still waits for openingDone and asserts the launch.opening node, so a failed launch or missing overlay can fail it for a real reason. `make targeted ONLY="app:LaunchStrictModeTest#seededLaunchSurvivesStrictModeThroughTheOpening"` passed in **5.25 s wall**. Log: `build/phase3a-review-r1/strict-mode-targeted.log`.

Review round 1 final validation: the single `make quick` invocation passed in **22.92 s wall**, including Spotless/Konsist, JVM/logic tests, build-logic and script tests, and the serial timing tier. Evidence: `build/phase3a-review-r1/quick.log`. All requested targeted checks and the two system-bar captures ran successfully; no sandbox block or test retry was needed. Performance/profile reruns remain deferred to the orchestrator's quiet-host schedule.

## Phase 3b: functional emulator validation

2026-11-17, Phase 3b Task 9 (`6775c73` plus the Task 9 changes committed as `4ad6f65`) on `pixel` / API 36 / 60 Hz, debug build, functional animator scales 0 with `cf.reduceMotion=false`. These are functional emulator measurements, not physical-device performance claims.

| Run / command | Hand-off end from t0 (ms) | Stall sample gap / sampled-vs-wall error (ms) | Result |
|---|---|---|---|
| `make targeted ONLY="app:LaunchHandOffTest app:LaunchStallTest app:LaunchWarmStartTest app:LaunchStrictModeTest app:LaunchFrameZeroTest"` (23.52 s wall) | ready 1903.234460; slow 3920.192752; stall 1905.408792 | 530.886125 / 1.879708 | Six passed; slow missed its 3918.666666 ms upper bound by 1.526086 ms |
| Orchestrator: `make targeted ONLY="app:LaunchHandOffTest"` (10.63 s wall) | ready 1911.187626; slow 3916.087794 | — | Both passed; `BUILD SUCCESSFUL` |

The user classified the one-off 3920.19 ms slow result as one dropped emulator frame on a busy host (load average 3.5), not a timing bug. The passing orchestrator rerun is recorded without changing the one-frame timing bound, the clock, the readiness rules or the implementation. The initial ready/stall results were 3–5 ms after their 1900 ms targets. Every instrumented method stayed below 10 s. Warm start, slow-status StrictMode and both light/dark frame-zero proofs passed in the initial run.

The stall proof reads the debug `LaunchController.frameLog` ring buffers; `compositionObserver` remains the composition counter. The separate-process `ProcessDeathLaunchTest` compiles and is wired into `make perf`; execution is pending with the quiet-host orchestrator. No perf/profile tier was run by this phase worker. Warm TTID, cold TTFD, ready/exit durations, benchmark gates and regenerated profiles remain pending.

Evidence: `build/phase3b-validation/task9-emulator/`, `task9-timings.json`, and `task9-orchestrator-rerun/`. The preserved initial failure remains evidence; it was not overwritten by a worker retry.

## Phase 3b: ending captures and status guards

`make targeted ONLY="feature:launch:LaunchStatusGuardTest app:LaunchModeTimelineTests"` passed in **6.32 s wall**. The ten status guards cover small, pixel8, foldInner, landscapeSideNav and largeFont in English and Dutch at the configured font cap. They prepare the actual status raster on IO, sample the slow hand-off at 2600 ms, and check its text/dot bounds against the horizontal safe bounds and the wordmark box. Diagnostic timelines now use the same `LaunchHandOffTimeline` builder as readiness-driven endings; the existing mode-equivalence test passes.

`make captures ONLY=SplashScreen,SplashSlow` passed in **13.42 s wall** and wrote **58 PNGs** under `build/captures/`: the 33 existing opening captures and all **25** new ending frames. `LaunchEndingCaptureTest` produced 25 results, no skips or failures (3.462 s combined test time). The new frames are ready 1700/1900, slow 2000/2600/4900 and slow Reduce Motion 2600, for pixel8/small in light/dark, plus slow 2600 pixel8/nl/light. Each slow frame waits for its status raster before capture. An opaque generated app-background token sits below the overlay, matching the real root during its fade.

The ready 1700/1900 and slow 2600/4900 light frames, Dutch slow 2600, and small dark/Reduce Motion frames were inspected. At native ready 1900 the overlay is fully transparent and the app background is visible. SplashSpec's captioned 1900 frame embeds board time 1700, so it still shows the older board hold; the native 1500–1900 exit follows S3. Slow 2600 retains the status and three dots; slow 4900 fades the entire scene into the app background. The centred moon, shifted wordmark/status composition, no stars and arc-length path retain the settled S1–S5 rules. Reduce Motion shows the static moon/status with constant dots. No new designed destination was added.

Phase 3b records its 17 defaults as OQ-A84–A100 and marks OQ-A69/A70 answered. OQ-A68 remains unused. The plan now documents the exact diagnostic keys, units, defaults, variants and process/recreation order.

Final validation invoked `make quick` once (**14.04 s wall**). Its JVM/logic tier reported 1,092 cases and stopped on two module-graph checks: an explicit dependency on the launch module's own test fixtures was redundant because AGP exposes those fixtures automatically. Removing that dependency preserved the architecture rules; the two failed methods plus `LaunchClockTests` passed through `make targeted` in **3.79 s**. The remaining quick steps then passed separately: build-logic **1.20 s**, script tooling **12.92 s** (104 script cases and 24 contract cases passed; one parity case initially lacked Make's exported environment and passed with the exports restored in **0.13 s**), l10n **0.08 s**, icons **0.07 s**, and serial timing **2.17 s**. The complete quick sequence, including the scoped corrections, totals **34.43 s**; the original command's failure is retained rather than reported as a passing invocation. No per-test budget overrun was reported.

`./gradlew --console=plain :app:lintDebug :app:testReleaseUnitTest :app:verifyReleaseManifest :app:analyzeReleaseR8Config -Pcf.tier=quick` took **29.57 s**: release tests, manifest verification and R8 analysis passed; lint requested the equivalent KTX `createBitmap` call in the status raster. After that equivalent API substitution, `make targeted ONLY="feature:launch:LaunchStatusRasterTest"` passed in **3.92 s** and `./gradlew --console=plain :app:lintDebug` passed in **6.13 s**. `make format` also passed. Full commands, outputs and timings are retained under `build/phase3b-validation/`; the combined `make check` target was not repeated.

Quiet-host work remains with the orchestrator: `make baseline-profile` to regenerate both committed profiles through the hand-off, then `make perf` on the final profiled build (warm TTID; cold TTFD, `readyFirstMs`, `exitFirstMs`; existing startup/launch gates; `ProcessDeathLaunchTest`). Physical-device gates remain open under OQ-A22. No perf/profile tier or physical-device claim was made by this worker.

## Phase 3b review: process-death launch and warm preparation

The orchestrator's quiet-host run of `3039799` exposed two harness errors. `ProcessDeathLaunchTest` used `monkey`, which returned without launching ChronoFlow in the saved log. It now starts the explicit `MainActivity` component through `am start -W`, checks the launch result, waits for the app to leave the foreground, and uses `UiDevice.wait` for PID disappearance after `am kill`. Reduce Motion is explicitly false so the relaunch overlay is observable with functional animator scales at 0. Both perf variants' APK metadata identifies `net.chronoflow.app.native`; the shared main-source root exposes `app.root` and `route.*` resource ids. No release tag or routing change was needed.

Profile generation previously filtered only by `androidx.benchmark.enabledRules=BaselineProfile`. That skips other benchmark rules but cannot skip a plain JUnit class such as `ProcessDeathLaunchTest`. `Scripts/baseline-profile.sh` now also selects `BaselineProfileGenerator` explicitly, leaving the process-death proof in `make perf`. A fake-device script test checks the actual generated command without collecting profiles.

The warm failure was Macrobenchmark 1.5.0's unmeasured cold preparation, rather than a measured warm launch: `macrobenchmarkWithStartupMode` sets `iteration=null` and invokes the measure block before iteration 0. The saved log shows a new PID and only one activity start before the 2 s route wait failed, earlier than the normal signed-out 2550 ms hand-off. That preparation now uses the existing 5 s cold-completion wait; measured warm iterations retain the 2 s route wait and additionally require the overlay to be absent. A JVM controller regression proves that a warm activity arriving before the cold session resolves picks Welcome with a headless Reduce Motion coordinator, releases the splash and cancels pending clock sleeps. The app's routing logic remains unchanged.

The orchestrator reported cold TTID median/min/max **101.4 / 95.1 / 116.0 ms**, first-commit **38.2 ms**, TTFD **2673.1 ms**, ready **47.6 ms**, exit **958.8 ms**, and launch jank **38.6–61.2%**, P99 overrun **16.0 ms**, frame counts **152–156** (launch gate PASS), pixel/API 36/60 Hz, benchmarkRelease. These describe that incomplete run; final benchmark rows await the orchestrator's rerun. **TTFD does not break the plan's hand-off + two-frame allowance.** TTFD starts at the activity launch, while the 2550 ms interim Welcome ending starts at overlay `t0`. Read-only analysis of all ten saved cold traces, joining `android_startups` with `android_startup_time_to_display` and the `exit`/`first-commit` slices, puts full display **15.585750–19.190458 ms after hand-off**, below **33.333333 ms** at 60 Hz. SQL and per-iteration evidence: `build/phase3b-review-r2/ttfd-audit.{sql,csv}`. Emulator TTFD is also report-only in the plan and budgets; no budget was changed.

Review validation used only the single-class commands in `docs/tooling.md`, each under the device lock. `ProcessDeathLaunchTest` passed on its first corrected execution (**7.06 s wall / 2.789 s test**). `StartupBenchmark` passed on its first corrected execution with animator scales 1 (**80.81 s wall**; cold **41.963 s**, warm **31.035 s**); all ten measured warm iterations retain the no-overlay check. An initial stdin-based command wrapper ran no Gradle tests because adb consumed the remaining shell input (2.14 s); the documented argument-based wrapper fixes that and restores functional settings under the lock. `make targeted ONLY="feature:launch:LaunchControllerLifecycleTest"` passed all five cases in **4.30 s wall**; the fake-device profile filter test passed in **0.45 s**. The single final `make quick` passed in **22.15 s**, including 105 script, 25 contract, 49 l10n and nine icon cases. `make format` and `git diff --check` passed. Logs, original failures and result XML are retained under `build/phase3b-review-r2/`. No `make perf` or `make baseline-profile` run, benchmark-history append, profile regeneration, bound change or timeout increase was performed by this worker; the orchestrator owns those final reruns.


## Phase 3c: Welcome measurement setup (2026-11-18)

Quiet-host measurements and profile regeneration are **pending with the orchestrator**, as requested; this worker did not run `make perf`, `make perf-timeline` or `make baseline-profile`. Run `make baseline-profile` and then `make perf` on the final phase commit. Record pixel AVD / API 36 / benchmarkRelease / Partial(Require), commit and refresh rate, cold TTID median/min/max, warm TTID, TTFD, `welcomeInteractiveMs`, `introLastItemMs` and launch `frameOverrunMs` P50/P90/P99. Compare cold TTID with the last comparable row and flag changes beyond ±25%. Physical-device rows remain open under OQ-A22.

The display configuration was read with `python3 -I Scripts/device_lock.py -- /opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 shell dumpsys display`: active/default mode 1, supported/current refresh **60.000004 Hz**. One frame is 16.66666556 ms. The emulator functional gates check every iteration: interactive **2549…2567.66666556 ms**; last item **3199…3217.66666556 ms**. Missing and non-finite measurements fail. These are configured timing bounds, not benchmark results.

`WelcomeMomentMetric` joins the app process's async `first-commit`, `exit` and `intro` slices. Interactive is `exit.end − first-commit.end`; the last item is `intro.end − first-commit.end`. Cold startup, launch jank and startup profile collection wait through removal and the intro. Waiting for Sign in to exist first prevents the following bounded 900 ms `Until.gone` wait from returning early while its intro alpha is zero; the control then persists, so the wait expires by design. The capture covers at least the final 3200 ms item (roughly 3.45–3.65 s from t0 depending on visibility timing). The existing launch frame-overrun/frame-count gates are unchanged.


## Phase 4e: session restore measurement (2026-11-18)

`StartupBenchmark.startupCold` now collects `TraceSectionMetric("session", TraceSectionMetric.Mode.First)`. The async session slice begins when `first-commit` ends and resolves at the first known session. Every variant defaults to production `StoredDataReadiness`; explicit diagnostic extras alone select the stub. Restore, disk and Keystore work run on the injected IO dispatcher after first commit.

| Measurement | Device / build / setup | Command | Reference budget | Low-end budget | Result |
|---|---|---|---|---|---|
| Session known, signed out | Reference and low-end devices; pixel emulator separately as a regression signal; benchmarkRelease, Partial(Require), fresh install, no seed or stub extras | `make perf` | ≤50 ms after first-commit | ≤100 ms after first-commit | Pending with user; report only |

No startup, timeline or profile measurement was run during Phase 4e implementation, as requested. Record the signed-out cold median, build commit, device/API/refresh rate and trace evidence when running the command. Baseline/startup profile regeneration is also delegated (`make baseline-profile`); the changed startup path has not yet been reprofiled. No performance number or device claim is inferred from unit tests. See [Phase 4e results](phase-4e-results.md) for functional validation and the unavailable emulator output.
