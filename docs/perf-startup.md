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
| 2026-11-18 | 88cad27-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 106.3/99.6/119.5 | FAIL |
| 2026-11-18 | 88cad27-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 107.3/98.9/114.5 | PASS |
| 2026-11-19 | c3192ed-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 125.0/112.3/145.4 | PASS |
| 2026-11-19 | c3192ed-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 131.0/116.0/143.7 | PASS |
| 2026-11-19 | 8edb8455-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 129.4/119.2/146.3 | PASS |
| 2026-11-20 | ed0c402f-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 127.5/116.1/136.5 | FAIL |
| 2026-11-20 | ed0c402f-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 123.9/119.2/149.3 | PASS |
| 2026-11-20 | 5cb57682-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 132.9/123.0/144.0 | FAIL |
| 2026-11-21 | 683eadcf-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 132.1/121.8/152.5 | PASS |
| 2026-11-22 | 069adafd-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 122.6/112.2/141.0 | PASS |
| 2026-11-22 | fe9268ac-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 128.3/117.1/144.9 | PASS |
| 2026-11-22 | d9f8a8e9-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 131.8/124.5/147.5 | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | host | 1.0 | light | benchmarkRelease, Partial(Require) | 133.9/120.7/146.0 | PASS |

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
| 2026-11-18 | 88cad27-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 40.4 | 8.9 | 191.0 | 97.1/100.0 | PASS |
| 2026-11-18 | 88cad27-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 40.3 | 12.3 | 191.0 | 99.8/100.0 | PASS |
| 2026-11-19 | c3192ed-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 47.5 | 10.8 | 191.0 | 98.2/100.0 | PASS |
| 2026-11-19 | c3192ed-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 51.4 | 16.1 | 190.0 | 98.4/100.0 | PASS |
| 2026-11-19 | 8edb8455-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 51.1 | 36.5 | 173.0 | 99.1/100.0 | PASS |
| 2026-11-20 | ed0c402f-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 50.3 | 13.3 | 192.0 | 99.6/100.0 | PASS |
| 2026-11-20 | ed0c402f-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 49.5 | 22.3 | 191.0 | 98.0/100.0 | PASS |
| 2026-11-20 | 5cb57682-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 52.0 | 12.0 | 191.0 | 98.4/100.0 | PASS |
| 2026-11-21 | 683eadcf-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 50.6 | 12.5 | 190.0 | 99.8/100.0 | PASS |
| 2026-11-22 | 069adafd-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 48.0 | 12.9 | 190.0 | 98.1/100.0 | PASS |
| 2026-11-22 | fe9268ac-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 48.1 | 12.6 | 188.0 | 99.9/100.0 | PASS |
| 2026-11-22 | d9f8a8e9-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 49.6 | 8.5 | 188.0 | 99.8/100.0 | PASS |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 60 | benchmarkRelease, Partial(Require) | 51.6 | 8.5 | 189.0 | 88.5/100.0 | PASS |

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
| Session known, signed out | Reference and low-end devices; pixel emulator separately as a regression signal; benchmarkRelease, Partial(Require), fresh install, no seed or stub extras | `make perf` | ≤50 ms after first-commit | ≤100 ms after first-commit | pixel emulator: 13.2 ms (run 1), 15.4 ms (run 2) cold median; reference and low-end devices pending (OQ-A22); report only |

Orchestrator, 2026-11-18, Apple M4 Max host (indexers idle); `make perf`, pixel / emulator-5554, Android 16 API 36, host GPU, 60 Hz, English, light, font 1.0, `benchmarkRelease`, `Partial(Require)`, 10 cold iterations, build 88cad27 (profiles regenerated by `make baseline-profile` in that commit; `-dirty` is the untracked phase briefs). Emulator numbers are a regression signal, not a device claim.

| Metric (ms) | Run 1 median / min / max | Run 2 median / min / max |
|---|---|---|
| `sessionFirstMs` (session known after first-commit) | 13.2 / 11.7 / 18.4 | 15.4 / 11.9 / 23.5 |
| Cold TTID | 106.3 / 99.6 / 119.5 | 107.3 / 98.9 / 114.5 |
| Warm TTID | 30.5 / 20.4 / 33.3 | 31.5 / 22.5 / 37.2 |
| Cold TTFD | 2689.4 / 2680.4 / 2697.2 | 2694.9 / 2680.9 / 2703.4 |
| `first-commitFirstMs` | 40.4 / 38.6 / 44.8 | 40.3 / 35.8 / 43.6 |
| `welcomeInteractiveMs` (hard 2567.7) | 2555.3 / 2550.2 / **2568.5** | 2561.6 / 2552.8 / 2567.6 |
| `introLastItemMs` (hard 3217.7) | 3201.2 / 3199.6 / 3202.2 | 3201.0 / 3200.7 / 3202.3 |
| Launch `frameOverrunMs` P99 | 8.9 | 12.3 |

Run 1 (`build/perf/startup/20261118T114418Z-88cad27-…`) exited 1: one `welcomeInteractiveMs` iteration was 0.8 ms over its hard gate (one late frame; the other nine were 2550–2565 ms). Run 2 (`build/perf/startup/20261118T114735Z-88cad27-…`) exited 0, with its maximum 0.07 ms under the gate. The gate (2550 ms designed hand-off plus one 60 Hz frame plus 1 ms) leaves no room for a single late emulator frame; this is recorded as a follow-up rather than relaxed here. Cold TTID moved from 95.8 ms (Phase 3a row) to 106–107 ms, +11%, inside the ±25% flag threshold. See [Phase 4e results](phase-4e-results.md) for functional validation.


## Phase 6a session (2026-11-19)

Emulator `pixel` (API 36, 4 cores / 2 GB), `benchmarkRelease`, `Partial(Require)`, host load 2–3. Run with `make perf` on `c3192ed`, before and after `make baseline-profile`.

| Metric | Before regeneration | After regeneration | 4e (`88cad27`, run 2) |
|---|---|---|---|
| Cold TTID median/min/max ms | 125.0 / 112.3 / 145.4 | 131.0 / 116.0 / 143.7 | 107.3 / 98.9 / 114.5 |
| `first-commit` median ms | 47.5 | 51.4 | 40.3 |
| Warm TTID median ms | 27.5 | 30.6 | — |
| Launch `frameOverrunMs` P99 | 10.8 | 16.1 | 12.3 |

Every hard gate passed in both runs. Cold TTID is report-only and far inside the plan target (median ≤ 400 ms). Still, signed-out cold start has drifted +17–22% since 4e, and regenerating the profiles did not recover it. `first-commit` grew by about 11 ms, so the extra cost is before the first composition commit, on the signed-out welcome path. The 6a shell is not on that path. The 5c launch-intent capture and the 5d eager `auth.verification` collector are. Follow-up: OQ-A247.

## Release size

Phase 5b Task 3, 2026-11-18; `make size`, release AAB, bundletool arm64-v8a / xxhdpi / en device specification (size selection only, no attached-device timing claim). These measurements bracket adding Credential Manager dependencies/client/keep rules; they are not a later measurement of the final wired phase tree.

| Point | Tree | Exact command output | Gradle duration |
|---|---|---|---|
| Before dependencies | `dddb27e` | `Bundle size: 2309016 bytes (2.31 MB), arm64-v8a/xxhdpi/en: OK` | 52 s |
| After dependencies | Task 3 working tree committed as `4e9dda3` | `Bundle size: 2445635 bytes (2.45 MB), arm64-v8a/xxhdpi/en: OK` | 37 s |

Delta: 136,619 bytes. Both runs stay below the 8 MB warning and pass the native-library gate. Neither Credential Manager AAR contains a `.so`; no native-library exclusion or allow-list change was added. The Play-services consumer keep rule and the app's broader guard appear in the merged release R8 configuration. Original logs are retained in `build/phase5b-validation/size-before.log` and `size-after.log`. No startup/perf/profile run was performed in Phase 5b.

## Phase 7 — Today (2026-11-19)

`TodayRoute` adds a stable `ReportDrawnWhen` predicate: first non-Loading content **and** 3b's unchanged root hand-off predicate must both finish. `CfRoot` composes `RootContent` below the overlay, and `LaunchController` publishes its route at exit commit, before overlay teardown. The two ordering cases pass with a real FullyDrawnReporter.

`StartupBenchmark.startupColdSignedIn` uses ten cold iterations, `CompilationMode.Partial(BaselineProfileMode.Require)` and `StartupTimingMetric`. It launches with `cf.session=in cf.stored=yes cf.gate=app` and waits on structural Today tags. With no real session, only the explicitly signed-in **perf** app stub selects `FixtureTodaySource`; it prepares MockAppSeed at device time on the injected worker. Debug native sessions and release builds cannot select it. The startup profile journey now reaches the first Today content.

Results on `pixel` AVD, API 36, `benchmarkRelease`, host on AC power (`make baseline-profile` → 96736893, then `make perf`): signed-in cold TTID median **128.1 ms**, TTFD median **2062.9 ms** (FixtureTodaySource). Signed-out cold TTID **129.4 ms**, TTFD **2711.3 ms**, warm TTID **39.5 ms**; every welcome/intro hard bound passes. A run in Low Power Mode on battery missed `welcomeInteractiveMs` once by 0.7 ms and was discarded. Emulator numbers are reports, never device claims.

## Phase 8 — walkthrough and terms gates (2026-11-20)

The walkthrough and terms gates sit on the launch path, so the profiles were regenerated (`make baseline-profile`) and `make perf` ran on `pixel` AVD, API 36, `benchmarkRelease`, Partial(Require), host on AC power and quiet. Signed-out cold TTID median **123.9 ms**, TTFD **2708.6 ms**, warm TTID **34.3 ms**; signed-in cold TTID **127.5 ms**, TTFD **2061.1 ms** (Phase 7: 128.1 / 2062.9). Every hard bound passes.

Near-miss to watch: `welcomeInteractiveMs` (hard 2567.7 ms, min 2549, both from 3c) put its first iteration at 2569.1 ms in the run before (one of ten, 1.4 ms over; the other nine 2554–2567) and at 2567.1 ms in this one. Phase 7 saw the same 0.7 ms first-iteration overshoot. The signed-out welcome path is unchanged since 3c, so this is first-iteration jitter against a one-frame window, not a Phase 8 regression. Emulator numbers are reports, never device claims.

## Signed-in Today startup

| Date | Commit | Class | Device | API | Hz | Font | Theme | Build | TTID median ms | TTFD median ms | Today source | Result |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-19 | 8edb8455-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 128.1 | 2062.9 | FixtureTodaySource | REPORT |
| 2026-11-20 | ed0c402f-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 125.4 | 2053.5 | FixtureTodaySource | FAIL |
| 2026-11-20 | ed0c402f-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 127.5 | 2061.1 | FixtureTodaySource | REPORT |
| 2026-11-20 | 5cb57682-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 124.4 | 2061.8 | FixtureTodaySource | FAIL |
| 2026-11-21 | 683eadcf-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 127.6 | 2060.4 | FixtureTodaySource | REPORT |
| 2026-11-22 | 069adafd-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 119.1 | 2050.0 | FixtureTodaySource | REPORT |
| 2026-11-22 | fe9268ac-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 121.8 | 2048.8 | FixtureTodaySource | REPORT |
| 2026-11-22 | d9f8a8e9-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 131.9 | 2062.8 | FixtureTodaySource | REPORT |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 60 | 1.0 | light | benchmarkRelease, Partial(Require) | 130.9 | 2060.2 | FixtureTodaySource | REPORT |

## Phase 9c — wizard shell and Where (2026-11-20)

The wizard open path changed, so the profiles were regenerated (`make baseline-profile`) before `make perf` on 5cb57682. Results on `pixel` AVD, API 36, `benchmarkRelease`, `Partial(Require)`, 60 Hz (`build/perf/startup/20261120T123721Z-5cb57682-…`):

| Metric | Phase 8 (AC) | Phase 9c |
|---|---|---|
| Signed-out cold TTID median | 123.9 ms | 132.9 ms (+7%) |
| Signed-out cold TTFD median | 2708.6 ms | 2714.4 ms |
| Warm TTID median | 34.3 ms | 35.2 ms |
| Signed-in cold TTID / TTFD median | 127.5 / 2061.1 ms | 124.4 / 2061.8 ms |
| `welcomeInteractiveMs` (each) | 2554–2569.1 | 2550.3–2568.8 |
| `introLastItemMs` (each) | — | 3200.1–3203.0 |

The host was on battery but not in Low Power Mode (`powermode 2`), and every metric sits within 7% of the Phase 8 AC run, so the run stands. The `FAIL` rows above for 5cb57682 are this run under the old one-frame `welcomeInteractiveMs` gate: its first iteration was 2568.8 ms, 1.1 ms over 2567.7, the same first-iteration miss seen in 4e, 7 and 8 with the welcome path unchanged since 3c. OQ-A382 widens that emulator gate alone to two frames (2584.3 ms); re-checking the same results with `Scripts/check-benchmarks.py --suite startup` and `--suite launch` (emulator, 60 Hz) exits 0. Emulator numbers are reports, never device claims.

## Phase 10b: theme launch-path handoff (2026-11-21)

The root now resolves this device's theme before `RootThemeContent`. DataStore collection begins after `withFrameNanos`; below API 31 the initial choice uses the existing ≤64 B device-config read in `ConfigurationPolicy`. `Application.onCreate` is unchanged. A counting-flow guard fails when the frame barrier is removed and passes with it restored. The native theme cases pass on pixel API 36, including a separately seeded trip-screen toggle with zero debug IO violations. A manual dark cold-start capture has centre-top pixel `#06091C`; its `am start -W` output is an appearance check and is not a benchmark result.

The user will measure startup and regenerate profiles on the host. Neither command was run by the worker:

```sh
make baseline-profile
make perf
```

Record cold/warm TTID, TTFD, frame CPU/jank and RSS from the normal benchmark output here, including build/device and command, and commit the regenerated baseline/startup profiles. No new startup performance numbers are claimed for 10b. No benchmark journey or timeline/plan/trip-page source changed; `make perf-timeline` is not required by the phase's R10.

Review round 1 moves overlay observation and the launch-finish effect into the root theme content lambda. `make targeted ONLY="app:RootThemeTest app:CfRootTest app:CfRootHandOffTest app:RootThemeChoiceTest"` passes 21 cases in 8.56 s tier wall time on the macOS host, JDK 21, debug Robolectric tests. `endingTheOverlayDoesNotRecomposeCfRoot` observes the actual overlay disappearing and **0** root compositions; restoring the original reads produces **1** and fails the guard. Explicit snapshot notification keeps the paused-frame fixture from reporting a false zero. API 29/30 tests also prove the stored choice reaches `CfRoot` and is reapplied to system bars after the real activity configuration callback, even when the resolved Dark choice stays unchanged. These are JVM wiring/scope checks, not startup performance measurements. The host will run the two commands above after this round; no performance/profile tier was run by the worker.

## Phase 13 release size

2026-11-22, worker, local macOS host, release bundle, bundletool arm64-v8a/xxhdpi/en/API 36 device spec; no device used. `make size GRADLE='./gradlew --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process'`: **4,147,965 B (4.15 MB), OK**, 40.72 s wall. Previous 5b result: 2,445,635 B. Native-library and language-split checks pass. The later host split-enabled proof fails as expected, and the restored normal size is 4,156,988 B (4.16 MB); see release evidence. See [release evidence](release.md#size).

## Phase 13 profile preparation

The generator retains the original stubbed Today startup and wide journey, and adds signed-out Welcome, a real signed-in Today, the active-trip journey and wizard-to-Ready recovery. Large60 appends the shared day-59 jump. Collection remains three max/two stable iterations, 120 s per journey; the runner remains timeout_msec=900000 and TIER_IDLE_TIMEOUT=1200.

The first host run failed the wizard journey; its repair and JVM evidence are retained below. The second host `make baseline-profile` passed on pixel, AC power, in **10 min 34 s** reported command wall (**10 min 31 s** Gradle wall), source log `build/phase13-host-evidence/bp-13b.log`. Commit **c8e7cfb3** contains the genuinely regenerated files and strict BaselineProfileFilesTest guard: **45,338 baseline rules** and **35,110 startup rules**. Today, Trips, step sheet, Ready, Welcome and SignIn coverage is now guarded. No profile was hand-edited. The saved guard patch has already been applied; do not apply it again.

The subsequent `make perf` and three `make perf-timeline` runs use Partial(Require) successfully (R1–R4 below), completing the profile proof. `make targeted ONLY="app:BaselineProfileFilesTest"` is included in the worker's final targeted validation. This completes the original profile task at c8e7cfb3. The later launch-clock fix ccedfbe0 requires a fresh host `make baseline-profile` and performance verification; the plan records that refresh as open. Earlier preparation checks: four planted post-generation coverage cases passed; the existing thirteen runner fixture cases pass after the Bash 3 array correction. Physical generation still requires PERF_CLEAR=1.

## RN comparison protocol

Native `net.chronoflow.app.native` and RN `net.chronoflow.app` use the same parameterized LaunchComparisonBenchmark harness: signed out, data cleared before every iteration, StartupMode.COLD, ten measured iterations, StartupTimingMetric / FrameTimingMetric / MemoryUsageMetric(Max), a fixed 0–3.5 s capture, and Partial(UseIfAvailable) for each APK's embedded profile. Native budget suites retain Require. Clearing RN data removes the research demo sign-in. This is the OQ-13-16 default: StartupBenchmark stays native-only because its native trace metrics cannot exist in RN.

The existing prebuilt RN release APK was verified read-only by the worker on 2026-11-22: SHA-256 `4ee240d3754a7bba12761ddcb4b8317b15d7131f3c55f93f314d64e89df3b1f5` (125,310,093 B; applicationId net.chronoflow.app, version 1.0.0 / 1, research 12 §1). `make perf-rn` reads and installs only this existing APK after the hash check, or a user-supplied RN_APK/RN_APK_SHA pair. A missing/mismatched APK refuses before installation with OQ-A591. No RN build is part of the recipe.

TTID median and memory median must be below RN on reference/low-end devices; emulator comparisons report. Overrun P99 and TTID P90 report. P90 is the linearly interpolated 90th percentile of the ten raw TTID samples. Every missing/non-finite metric fails; signed frame overrun remains valid. History rows have the exact twelve-column schema below and a twelve-character verified APK hash.

Host commands (the first is the normal run; use fallbacks only for the stated tooling condition):

```sh
make perf-rn
# If Macrobenchmark rejects the known non-profileable RN APK:
RN_NOT_PROFILEABLE=1 make perf-rn
# If RN StartupTimingMetric is still missing after that:
RN_NOT_PROFILEABLE=1 RN_AM_START_FALLBACK=1 make perf-rn
```

The last command additionally records ten RN-only `am start -W -S` TotalTime samples as `rnAmStartTotalTimeMs`, report only, with raw JSONL in the archive. Those values are a different harness and never become a below-RN TTID verdict; missing comparison metrics keep the command failing/open. No fallback has been exercised on a device yet. Physical runs require `PERF_CLEAR=1`; no device/perf tier was run by the worker.

Worker validation (local host, 2026-11-22): `python3 -I -m unittest discover -s Scripts/tests -p test_check_benchmarks.py`: **64 PASS, 2.56 s wall**; after isolating the session fixture's other reference values, its targeted case passed in **0.07 s**. `test_perf_compare_rn.py`: **8 PASS, 2.01 s wall**, including the fallback with fake adb only. `:benchmark:assembleBenchmarkRelease -Pkotlin.compiler.execution.strategy=in-process`: **PASS, 1.71 s wall**. The new sessionFirstMs budget is exactly plan.md's 50/100 ms (emulator report); existing budget values are unchanged.

## RN comparison

| Date | Commit | Class | Device | API | RN APK sha256 | Metric | Stat | Native | RN | Δ % | Verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 4ee240d3754a | timeToInitialDisplayMs | median | 125.7 | 471.3 | -73.3% | REPORT |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 4ee240d3754a | memoryRssAnonMaxKb | median | 47930.0 | 197084.0 | -75.7% | REPORT |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 4ee240d3754a | frameOverrunMs | P99 | 21.8 | 35.8 | -39.1% | REPORT |
| 2026-11-22 | c8e7cfb3-dirty | emulator | sdk_gphone64_arm64 | 36 | 4ee240d3754a | timeToInitialDisplayMs | P90 | 130.3 | 487.1 | -73.3% | REPORT |

Host comparison results above come from R5 (`make perf-rn`, `t13-rn.log`) with the normal Macrobenchmark harness and verified RN APK. No non-profileable suppression or am-start fallback was needed. Research's am-start TotalTime values remain context only. All four emulator comparison values report; below-RN device gates remain open.

## Phase 13

Status: profiles and R1–R5 host emulator runs are recorded below, including the scripts' existing auto-recorded history rows, which are retained unchanged. These are emulator regression measurements, never device claims. Release-check, corrected minified smoke, signingReport, the split-enabled failure proof, restored normal size and a standalone make check have now passed. R9 proves the quick median; its third timed check failed the early launch-clock deadline, fixed in ccedfbe0. The complete check median and post-fix launch/profile verification remain open, along with all physical rows.

Host evidence: 2026-11-22, `c8e7cfb3-dirty` as recorded by the scripts, pixel / sdk_gphone64_arm64, Android 16 / API 36, 60 Hz, GPU host, animator scales 1, host on AC power. Font 1.0/light unless the command states otherwise. Startup/timeline use benchmarkRelease, Partial(Require); profile collection uses nonMinifiedRelease. RN comparison uses Partial(UseIfAvailable) for both apps. Source logs are archived under `build/phase13-host-evidence/`; benchmark JSON, XML and traces retain the script archives below. These measurements predate the launch-clock fix ccedfbe0.

| Evidence | Log | Raw JSON and traces under build/ |
|---|---|---|
| Profiles | `bp-13b.log` | Committed profiles/guards in c8e7cfb3; source collection output also retained in R1's previous-output/ |
| R1 startup | `t13-perf.log` | `build/perf/startup/20261122T092852Z-c8e7cfb3-dirty-light-1.0.brcLZ9/` |
| R5 comparison | `t13-rn.log` | `build/perf/compare/20261122T101118Z-c8e7cfb3-dirty-light-1.0.G6HiJ9/` |

The archive's `androidTest-results/` and `connected_android_test_additional_output/` retain the XML, benchmark JSON and Perfetto traces. R1 took **241.10 s** command wall; R5 **162.93 s**. The logs show CF_UPLOAD_* absent, so the reported perf APKs use debug signing; the inherited-upload defect did not affect these runs.

| Run | Exact host command | Evidence/current result |
|---|---|---|
| R1 | `make perf` | PASS functional emulator checks; startup/launch values below, t13-perf.log |
| R5 | `make perf-rn` | PASS comparison execution; all four emulator values REPORT, t13-rn.log; RN SHA verified by recipe |
| R7 | `make release-check` | Host PASS: release tests, manifest, 7,768-class APK scan and 4,156,988 B size; `build/phase13-host-evidence/t13-release-check.log` |
| R8 | `make release-smoke` | Host PASS after 40ba540f: tabs 3.381 s, sheet 8.606 s, wizard 6.778 s; all < 10 s, unchanged pids and crash checks pass; `build/phase13-host-evidence/release-smoke/` |
| R9 | `Scripts/measure-tiers.sh` | Host after a05a1def, 22 min 35 s: warm-up quick/check PASS; quick 81.86 / 44.33 / 43.83 s, median 44.33 s PASS; check 245.89 / 284.79 s PASS, third check FAIL in LaunchHandOffTest. No three-run check median; build/tier-timings.json not regenerated (still 2026-11-16). `build/phase13-host-evidence/t13-tiers4.log` and archived tier times |
| R10 | `PERF_CLEAR=1 DEVICE_CLASS=reference make perf` and `PERF_CLEAR=1 DEVICE_CLASS=reference make perf-rn` | Open: device only (OQ-A22); repeat with DEVICE_CLASS=lowEnd for the selected low-end device |

Startup row-to-verdict inventory (plan.md, including R14's complete mapping):

| Budget row | Source / command | Phase 13 value | Verdict |
|---|---|---|---|
| Cold TTID, signed out | startupCold / R1; comparison median / R5 | R1 median 133.9 ms (min/max 120.7/146.0); R5 native/RN 125.7/471.3 ms | REPORT, emulator; device hard rows open OQ-A22 |
| Warm TTID | startupWarm / R1 | 38.6 ms median | REPORT, emulator |
| onCreate → overlay frame 0 | first-commitFirstMs / R1 | 51.6 ms median | REPORT, emulator; reference 50 ms remains a device gate |
| Overlay frame pacing | launchCold frameCount + display Hz / R1 | 189–194 frames per iteration, display 60 Hz; capture extends through intro | REPORT, emulator; do not infer physical 95% pacing from this capture |
| First animated change | LaunchTimelineTests / final quick | 5 cases PASS, 0.007 s suite | PASS, JVM functional guard |
| Launch jank, frame count | launchCold frameOverrunMs P99, frameCount minimum / R1 | Overrun P99 8.5 ms; jank 1.0–100.0%/iteration (mean 88.5%, max 100%); frameCount min 189 | Jank REPORT; functional minimum 75 frames PASS |
| Main thread before hand-off | debug StrictMode AppLaunchSmokeTest / `make check` | Host standalone check and R9 warm-up PASS | PASS at a05a1def; post-fix check pending |
| Stall behavior | LaunchStallTest / `make check` | Host standalone check and R9 warm-up PASS | PASS at a05a1def; post-fix check pending |
| Session known, signed out | startupCold sessionFirstMs median / R1 | 17.5 ms | REPORT, emulator; reference/low-end 50/100 ms remain device gates |
| Hand-off end | LaunchHandOffTimelineTests / quick; LaunchHandOffTest / `make check` | Earlier standalone check PASS; R9 third check FAIL: exitStart 3499.573168 ms, below 3500 ms. ccedfbe0 rechecks the monotonic deadline; 29 targeted JVM cases PASS | Post-fix emulator verification pending; existing window unchanged |
| Welcome interactive | welcomeInteractiveMs each / R1 | 2550.3–2566.7 ms, ten samples | PASS emulator bound 2584.333 ms; device-only one-frame rule remains open |
| Last intro item | introLastItemMs each / R1 | 3200.5–3203.7 ms, ten samples | PASS emulator bound 3217.667 ms |
| TTFD, signed out/in | startupCold / startupColdSignedIn / R1 | 2710.8 / 2060.2 ms median; signed-in FixtureTodaySource | REPORT, emulator; both finite |
| Peak memory to Welcome | LaunchComparisonBenchmark memoryRssAnonMaxKb median / R5 | Native 47,930 KiB; RN 197,084 KiB; native −75.7% | REPORT, emulator; below-RN device gate open |
| RN TTID median/P90 and overrun P99 | LaunchComparisonBenchmark / R5 | TTID median 125.7/471.3 ms; P90 130.3/487.1 ms; overrun P99 21.8/35.8 ms (native/RN) | REPORT, emulator; all required metrics present |
| Download size | release-check → `make size`, arm64/xxhdpi/en | Host 4,156,988 B (4.16 MB), allowed native lib only, no language split; enableSplit=true rejected splits/base-en.apk, then restored false | PASS normal gate and real negative proof |
| make quick < 60 s warm | R9 archived .tier-quick.times / t13-tiers4.log | 81.86 / 44.33 / 43.83 s; median 44.33 s | PASS, three host runs; old tier-timings.json is not this evidence |
| make check < 300 s warm | R9, including reminders-permission tier | Two passing runs 245.89 / 284.79 s; third FAIL at LaunchHandOffTest | Incomplete: no three-run median; rerun `Scripts/measure-tiers.sh` after the fix |

Device startup rows remain **open: device only (OQ-A22)** until R10 supplies physical evidence. Every host row must retain its raw result directory and metric. A missed hard gate creates a fix task, followed by the permitted one re-run; no budget is loosened or waived. Record a ±25% trend flag with its observed cause rather than editing the prior row. This R1 cold TTID is +1.6% against the preceding 131.8 ms history row; no startup/timeline ±25% flag appears in the supplied logs. Launch jank remains reported at the high values above, without a device verdict.


Earlier worker check (2026-11-22, source 17527208 plus documentation, local macOS host, JDK 21 tests): the single `make quick` passed its test tiers in **102.34 s wall**, above the 60 s warm target. No repeat or budget change was made. All individual test budget checks passed. The subsequent host quick median above supersedes this single-run evidence; R9's complete check series is still required. The final `make release-check` passed in **60.19 s**, reporting **4,147,961 B (4.15 MB)**; this supersedes the earlier size samples for that worker source; the later host result above is 4,156,988 B. See [tooling notes](release.md#tooling-notes) for exact commands and outputs. These are host JVM/build results, not device performance evidence.

## Phase 13 wizard profile repair (2026-11-22)

The host ran `make baseline-profile` on pixel, AC power and a quiet host, against 62cb4ccb. It failed (reported command wall 10 min 15 s). The connected nonMinifiedRelease XML records startup 42.353 s, startupSignedIn 57.499 s, startupSignedOut 57.386 s, tripJourney 123.838 s, timelineLarge60 183.272 s and timelineWide 88.216 s passing; wizardToReady failed at 16.635 s with `wizard.fine-tune.continue never became enabled`. This is a failed profile run, not verified generated-profile coverage. At that failed run the strict guard patch remained unapplied; c8e7cfb3 subsequently committed it with successfully generated profiles.

The journey opened its wizard while fresh sign-in's first account pull was still pending. `SessionController.performAccept` publishes SignedIn and then requests sync. `MemberWizardStore.initialDraft` reads local settings; before FullPull commits them, `sleepAnswered` and the saved profile are absent. `PerfWizardJourney.apply` deliberately preserves that base's `sleepNeeded=true` and empty profile. Choosing One way therefore opens **Sleep**, with bedtime, wake time and chronotype unanswered and Continue disabled. Fine-tune is not reached. Its actual enable condition is no active review, no closing and no persistence failure; optional prompts do not disable it. The flight dates remain thirty days ahead of the injected clock.

The repair belongs to 45793880. Wizard profile setup now signs in without the wizard extra, then waits structurally for `today.trip`: FullPull stores the account settings and trip cards in the same pull transaction. Setup happens once before collection so later iterations can retain their quiet wizard recovery. Each collected iteration still kills the process, launches with the real perf wizard extra, taps One way, taps enabled fine-tune Continue and reaches Ready. It uses the existing 60 s setup and 120 s journey budgets, with bounded waits and no sleeps. No product code, seed field, validation rule or step is bypassed or changed.

`PerfWizardViewModelTest` uses the same perf draft, a fake clock, in-memory persistence, real mock review source and a single test scheduler. The initial regression probe expecting fine-tune before settings sync fails with `expected:<fineTune> but was:<sleep>` (3.40 s command wall). The retained cases prove that the pre-snapshot draft requires Sleep with all three answers missing and Continue disabled, while the post-snapshot draft reaches enabled fine-tune and continues to enabled Ready. The existing full draft equality test against 9g remains unchanged.

Worker command `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make targeted ONLY='app:PerfWizardViewModelTest app:PerfWizardJourneyTest'`: **3 cases PASS, 3.74 s wall**. New ViewModel cases take 0.106 / 0.007 s, with seed initialization warmed once and recorded separately. No graph, activity or composable is used. Local fixture compile/setup mistakes were corrected before the regression probe; their logs remain `/tmp/cf13-wizard-repro.log`, `/tmp/cf13-wizard-repro2.log` and `/tmp/cf13-wizard-targeted.log`.

At the repair hand-off, the worker left device generation to the host. The subsequent successful `make baseline-profile` run and c8e7cfb3 resolved that profile/guard gap, as recorded above. The remaining performance/functional gates stay open.

Final local validation for the repair (2026-11-22, macOS worker, JDK 17 Gradle / JDK 21 tests): `./gradlew spotlessApply :benchmark:assembleNonMinifiedRelease -Pkotlin.compiler.execution.strategy=in-process` **PASS, 17.29 s wall**; this builds the profile test APK without running a device. Spotless is root-scoped: the first attempt with `:app:spotlessKotlinApply` failed with `Cannot locate tasks that match ':app:spotlessKotlinApply' as task 'spotlessKotlinApply' not found in project ':app'.` (2.98 s wall), then the corrected root task above passed. The earlier `make format` passed in 21.94 s.

`env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make quick` ran **once**, **PASS for all test stages, 66.66 s command wall**. Its duration exceeds the 60 s warm target, which remains open under R9; no repeat or budget change was made. The tier reports have zero test-budget overruns, errors or hangs and certify reused unchanged results (including build-logic and serial timing). In this full app run the two new ViewModel methods took 0.018 / 0.003 s, with seed initialization recorded separately. Logs: `/tmp/cf13-wizard-targeted2.log`, `/tmp/cf13-wizard-build2.log`, `/tmp/cf13-wizard-quick.log`. No device, emulator or perf tier was run by the worker.


Host follow-up after c8e7cfb3: the successful profile generation and strict guard commit resolve the earlier missing-coverage/patch instructions in this document. No new profile generation is required by the signing/checker/smoke-test review corrections: they change build signing and validation code, not the startup, wizard or timeline product paths. The smoke/window correction belongs to 0feca5ee and is verified by the passing host rerun after 40ba540f. Signing, normal/split-enabled size, release-check and a standalone make check are also verified. R9 quick median is 44.33 s; its check series remains incomplete as recorded above. The subsequent clock fix requires the launch/profile commands listed in release.md before closing the remaining gate.


## Phase 13 monotonic launch deadline repair (2026-11-22)

The third timed host check after a05a1def failed `LaunchHandOffTest.slowShowsStatusAndHandsOffAfterReady`: exitStart **3499.573168 ms**, below its **3500 ms** readiness seed. This is a clock deadline defect, not a frame-timestamp comparison. `LaunchScreenTest.exitStartMillis` reads `LaunchState.exitStart`, set from `LaunchCoordinator`'s `clock.now`; `MediaLaunchClock` derives that from continuous `System.nanoTime`. The test reads no vsync timestamp for this value. The frame interval supplies only the completion/dispatch upper allowance.

The contract stays unchanged. Plan.md “Performance budget” requires an early-ready hand-off end at **1900 ms ± one frame from t0**; Phase 3b brief §5 defines late readiness R as exit **R → R + 400 ms**. At iOS pin f391a1fa, `Packages/ChronoFlowKit/Sources/AppCore/LaunchReadiness.swift:165–172` sets exitStart to max(1500 ms, clock.now), `Sources/Launch/LaunchTimeline+Ready.swift:5–13` adds the 400 ms exit, and `Sources/AppCore/LaunchClock.swift` sleeps until the monotonic deadline. Pinned iOS plan.md “Performance budget” has the same 1900 ms ± one-frame row.

The resolved coroutines 1.11.0 bytecode rounds Duration delays up to whole milliseconds, then Android Handler.postDelayed schedules against whole SystemClock.uptimeMillis. That timer's clock quantization can still resume before the nanosecond deadline. The old MediaLaunchClock read the remaining duration only once. ccedfbe0 (correcting 8447437a's clock, used by Phase 3b's 4ad6f65f checks) rechecks remaining time after each wake and preserves cancellation. No test window, target or animation duration was widened.

A JVM test injects the reported early wake and fails before the fix: sleepUntil(3500 ms) has already returned at 3499.573168 ms. After the fix it stays suspended, then returns at 3500.573168 ms. A second test cancels between those wakes and proves no return. `make targeted ONLY='feature:launch:LaunchClockTests feature:launch:LaunchCoordinatorTests feature:launch:LaunchHandOffTimelineTests'` passes **29 cases, 14.01 s tier wall**, no budget overruns; the new regression itself takes **1 ms**. Launch layout passes **100 cases, 15.32 s tier wall**; app/launch lint passes (**23 s Gradle wall**). Red/green XML, logs and timings are archived under `build/phase13-host-evidence/launch-clock/`.

Post-fix host evidence remains pending: `make targeted ONLY="app:LaunchHandOffTest#slowShowsStatusAndHandsOffAfterReady"`, `Scripts/measure-tiers.sh`, `make check`, `make baseline-profile`, `make perf` and `make perf-timeline` (also THEME=dark and FONT_SCALE=1.3). The prior host measurements above stay attributed to their measured source. No emulator/perf command was run by this worker.


Post-clock-fix worker validation (2026-11-22, ccedfbe0, macOS host; JDK 17 Gradle / JDK 21 tests): the single `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make quick` invocation passes in **92.51 s command wall**. Its quick tier takes **54.28 s**, reports 6,312 cases (4,402 freshly measured; the remaining unchanged outputs certified), with zero budget overruns/errors/hangs. Build-logic's 64 cases and the serial timing tier's seven cases pass using certified unchanged results. This single command duration exceeds the warm target; it does not replace the host's three-run quick median (44.33 s) or complete R9's failed check series. No second quick run was made. Exact command, log, XML and timings: `build/phase13-host-evidence/launch-clock/quick-command.json`, `quick.log` and its `quick/`, `build-logic/`, `timing/` directories. The old build/tier-timings.json remains byte-for-byte unchanged.
