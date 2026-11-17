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

Device rows: open: device only (OQ-A22).

## Phase 2 comparison

Worker, 2026-11-17, Apple M4 Max host; `make perf`, pixel / emulator-5554, Android 16 API 36, host GPU, 60 Hz, English, light, font 1.0, `benchmarkRelease`, `Partial(Require)`. One invocation, exit 0, 44.41 s wall (Gradle 41 s), 10 cold iterations. Median/min/max **95.8 / 91.5 / 108.2 ms**. Archive: `build/perf/startup/20261117T075109Z-bfa817f-dirty-light-1.0.ZHHU2d/`; JSON, all ten traces and XML are retained. Source was committed at bfa817f; `-dirty` includes pending documentation, requested snapshot references and the unrelated untracked phase briefs.

The historical comparison against 0e is **+8.0%** over 88.7 ms. In Phase 2 review round 1 the user selected M1's **95.0 ms Partial(Require)** as the like-for-like reference: Phase 2's **95.8 ms is +0.8% and passes the 5% limit** (99.75 ms). The intervening M1 result was already 95.0 ms with `Partial(Require)`; Phase 2 is **+0.8%** against that immediate predecessor. The 0e row used `Partial(UseIfAvailable)`. These are distinct historical conditions, not evidence that the theme caused or repaired the difference. The M1 reference is recorded in OQ-A66; no repeated benchmark was used to seek a passing sample. These are emulator regression signals, not physical-device claims.

`make baseline-profile` passed once in 467.87 s wall (Gradle 7m45s); both startup and large60 paths completed, including the existing bounded-swipe-v3 progress assertions. Regenerated baseline/startup outputs are committed in bfa817f. The current StartupBenchmark contains only startupCold (10 iterations); the older command description's warm-start case is absent from the landed harness and was not invented in this phase.

## Phase 3a implementation and validation

Worker, 2026-11-17. No Phase 3a startup or timeline benchmark result is available yet. Task 10 adds StartupBenchmark's async `first-commitFirstMs` measurement and LaunchJankBenchmark, and checks ten launch iterations for frame count ≥ 75, overrun P99 and derived jank. Emulator timing remains report-only; physical-device gates stay open. The cold TTID comparison must use OQ-A66's 95.0 ms Partial(Require) reference when measured.

The initial feature group ran 54 cases (47 passed, seven failed; 18.69 s wall). The orchestrator resolved the short-window sun guard in brief commit 1b2c8fa. OQ-A83 keeps the iOS geometry and applies containsSun only at heights ≥ 667 dp; shorter windows retain moon/wordmark and finite-value guards. The corrected guard and the old parameterized-runner, Reduce Motion fixture and wordmark-reference failures passed in the first scoped run: 51 cases, 43 passed; eight newly executing render checks failed (12.34 s wall).

Those pixel failures exposed a production bug: inside DrawScope, `size` shadowed the scene's dp size, translating the moon off-screen. Renaming it `sceneSize` fixed all 36 affected render/guard cases on the first correction (9.69 s wall). A diagnostic run before the fix took 9.20 s. The unhinted wordmark reference uses subpixel and linear text metrics, preserving the ±0.75 dp bounds comparison. Robolectric parameterized classes now use the runner's own Parameters annotation. Reduce Motion compares the absent plane position with the sampler's defined zero default. The constructor-supplied frame callback lets the settling test count real pending requests; no test-only field or fake lives in src/main.

The next targeted group stopped on a confirmed RootThemeTest hang. Command: `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools make targeted ONLY="app:CfRootTest app:SystemBarStylesTest app:SplashThemeTest app:LaunchOverridesParityTest app:TestSeedTest app:RootThemeTest app:RootOverridesParityTest app:LaunchOverridesReleaseTest app:RootOverridesReleaseTest app:ReleaseReadinessTest konsist:KonsistRulesTest konsist:TierRegistryTest build-logic:convention:VerifyReleaseManifestTest tools:tokengen:LaunchPaintGoldenTest app:LaunchFrameZeroTest app:LaunchStrictModeTest app:AppLaunchSmokeTest"`. It produced 21 app cases: 18 passed, three failed, 70.94 s command wall time. Exact failures:

```text
CfRootTest > overlayComposesFirstAndShieldsTheContentOnTheNextFrame[36] FAILED
java.lang.AssertionError: Failed: assertExists.
Reason: Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'root.fixture')

RootThemeTest > activityProvidesReduceMotionAndPreloadsOnlyAfterItsFrameCommit[36] FAILED
androidx.test.espresso.AppNotIdleException: Compose did not get idle after 109081 attempts in 60 SECONDS.
run-tier: over budget: robolectric net.chronoflow.app.RootThemeTest#activityProvidesReduceMotionAndPreloadsOnlyAfterItsFrameCommit 60068 ms > 5000 ms

SplashThemeTest > initializationError FAILED
java.lang.IllegalStateException: Couldn't create lock file /Users/rickgout/.m2/repository/.robolectric-download-lock
Caused by: java.io.FileNotFoundException: /Users/rickgout/.m2/repository/.robolectric-download-lock (Operation not permitted)
```

No hang retry was attempted. CfRootTest's two system-bar flag cases passed (light and dark). The full output is `build/phase3a-validation/remaining-targeted.log`; result XML is preserved under `app-results/` and `pixel-results/` there. The earlier feature run and compiler corrections remain in that directory's logs and `feature-results/` archive. Kotlin daemon marker writes outside the sandbox used the existing successful compiler fallback; the SplashThemeTest Maven-cache write was a separate blocker.

Remaining targeted groups, `make captures ONLY=SplashScreen`, `make captures DEVICE=1 ONLY=SplashScreen`, `make perf`, `make perf-timeline`, `make baseline-profile`, `make quick`, `make check` and `make lint` were not run after the required hang stop (no output exists for these commands). Neither the generator stale-output proof nor the planted-allocation proof ran. There are no official phase capture PNGs, screen recordings, reviewed hand-over frames, board-difference results or new performance numbers. Diagnostic images are in `build/phase3a-validation/render-*.png`; they are not board captures or reference snapshots. BaselineProfileGenerator's prepared 1.6 s opening journey remains uncommitted until its generated profiles can be included with it.

Implementation choices: moon decoding is constructor-supplied because the asset belongs to the app module. Reset support is a debug-source AppGraph extension. Cutout mode is set in MainActivity (ALWAYS on API 30+, SHORT_EDGES on 29). Layout guards use the light-only LayoutMatrix; render/capture cases cover both themes. The scene retains the specified no-stars, centred moon, shifted wordmark/band and arc-length path differences from the older boards; a board comparison remains required. OQ-A69–A82 carry the brief's defaults, A83 records the user-approved short-window rule, and A68 stays absent.

## Phase 3a: one-clock correction and held-splash stop

Worker, 2026-11-17. User diagnosis resolved the RootThemeTest hang: frame timestamps and t0 could have different bases. Commit 90d9ea7 paces on frame callbacks but computes elapsed from the injected wall clock, and suspends on a first-draw CompletableDeferred before requesting frames. A plain JVM regression supplies a near-zero frame clock and a 9,000,000,000,000 ns wall-clock base; it proves no pre-draw frame requests, no settlement at 1499 ms, settlement at 1500 ms, and no later requests. It and the existing overlay recomposition test passed. RootThemeTest now uses the seeded screen profile and passed. All six SplashThemeTest cases (API 29/31/36 × light/dark) passed with the newly writable Maven cache. That combined invocation took 29.57 s (feature group succeeded, then app group had only the independent CfRootTest ordering failure).

CfRootTest now observes two explicit clock frames; its exact failing method passed on the first correction, 6.10 s wall / 2.504 s test. The configuration device tests also receive the screen profile. Four scoped design-boundary checks passed in 2.81 s after recognizing CfRoot as an inset-reading root and using Compose's allowed transparent value for system bars. The initial boundary group was 74 tests, 72 passed (13.63 s including the preceding passing release group). Build-logic's manifest guard and LaunchPaintGoldenTest passed; the targeted harness now matches the documented included-build XML-only result protocol, with seven script tests passing in 0.176 s. The two remaining emulator compilation errors used nonexistent UiDevice.displayDensity; both tests now read resource displayMetrics.density.

The next emulator invocation hung on its first test. Exact command:

```sh
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools make targeted ONLY="app:LaunchFrameZeroTest app:LaunchStrictModeTest app:AppLaunchSmokeTest app:ConfigurationPolicyDeviceTest"
```

```text
11-17 11:12:38.431 24113 24129 I TestRunner: run started: 8 tests
11-17 11:12:38.432 24113 24129 I TestRunner: started: frameZeroMatchesSystemSplashDark(net.chronoflow.app.LaunchFrameZeroTest)
```

There was no test result after more than 90 s. MainActivity was the resumed activity; a diagnostic screenshot showed the dark sky and centred moon still held on screen. This does not establish where ScreenTest.launch or the test method was blocked. Per the user/brief hang stop rule, no retry or assertion change was made. The debug application process was force-stopped to release the run; the emulator stayed running. Final command output:

```text
BUILD FAILED in 2m 12s
run-tier: over budget: emulator net.chronoflow.app.LaunchFrameZeroTest#frameZeroMatchesSystemSplashDark 127449 ms > 10000 ms
make: *** [targeted] Error 1
real 133.10
```

Evidence: `build/phase3a-validation/device-compile-fix1.log`, `held-splash-logcat.log`, `held-splash-hang.png`, and `device-hang-results/`. Clock/root-order XML and scoped logs are retained in the same directory. The screenshot was viewed and is diagnostic only; it is not the required SplashScreen capture matrix or proof of frame-0 parity. Official captures, board comparison, screen recordings, startup/timeline benchmarks, baseline generation, standalone quick/check/lint and planted-failure proofs remain unrun after this new required stop; they have no new output or performance numbers. The prepared BaselineProfileGenerator journey remains uncommitted so it can be committed with regenerated profiles. No new OQ was added: the one-clock correction is a direct user decision, and the new held-splash stall needs diagnosis.

## Phase 3a: held-splash diagnosis and icon-size blocker

Worker, 2026-11-17. Reproduced exactly once with `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools make targeted ONLY="app:LaunchFrameZeroTest#frameZeroMatchesSystemSplashDark"`. While PID 24446 was blocked, `adb -s emulator-5554 shell su 0 debuggerd -j 24446` captured `build/phase3a-validation/parity-thread-dump.txt`. The test thread was waiting in `Object.wait` → `Instrumentation.startActivitySync:625` → `ActivityScenario.launchInternal:367` → `ScreenTest.launch:47` → `LaunchFrameZeroTest.verify:29`. It had not reached the controller wait. The main thread was in `MessageQueue.nativePollOnce`, not an application frame loop. The observed synchronous launch block confirms the held-splash harness diagnosis; the dump does not literally show `waitForIdleSync` or Compose `waitUntil`. The app was force-stopped after the dump. The command ended in 53.97 s wall, reporting the interrupted test at 42.528 s (over its 10 s budget).

The correction uses the same seed construction through a plain `startActivity` Intent with the existing NEW_TASK/CLEAR_TASK flags. A placement CompletableDeferred suspends the test without waiting for Compose or instrumentation idle; `runOnMainSync` verifies the hold and releases it. Idle-based waits resume only after release. A test lifecycle callback and bounded destruction latch explicitly finish the plain-launched activity in `@After`. No assertion tolerance, icon size, theme or painter geometry changed.

`make format` passed (Gradle 7 s). The first scoped correction ran the same exact targeted command and completed in **9.40 s wall / 1.117 s test**, without a hang. It reached the existing icon-size assertion and failed:

```text
java.lang.AssertionError: system icon must be 288 dp; measured 192.0 dp expected:<288.0> but was:<192.0>
BUILD FAILED in 9s
make: *** [targeted] Error 1
```

This measures the exit listener's `provider.iconView` width on pixel / emulator-5554, API 36, dark appearance, functional animator scales 0. Task 9 explicitly requires stopping if that size is not 288 dp, without rescaling. No inference about drawable canvas versus framework view bounds was substituted for the measured result. The rectangle, circle-pixel and sky-band assertions follow this assertion, so their parity is still unproven. Logs: `build/phase3a-validation/parity-reproduce.log`, `parity-async-format.log`, `parity-async-fix1.log`; result XML: `app/build/outputs/androidTest-results/connected/debug/`.

Stopped at this new, explicit brief condition. The other device tests, `make captures ONLY=SplashScreen`, `make captures DEVICE=1 ONLY=SplashScreen`, `make perf`, `make perf-timeline`, `make baseline-profile`, `make quick`, `make check`, standalone `make lint`, screen recordings, and planted generator/allocation proofs were not run in this continuation; there is no output for those unrun commands. No official capture paths, board review, startup/timeline numbers or regenerated profiles are claimed. BaselineProfileGenerator's prepared journey stays uncommitted until it can accompany regenerated profiles. The remaining Task 10/11 evidence is partial. No new OQ was added: this is a measured mismatch covered by Task 9's explicit stop rule, rather than a new product decision. Existing OQ-A69–A83 retain their defaults; OQ-A68 remains absent.
