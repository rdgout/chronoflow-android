# Tooling: Phase 0a verified results

Mirrors iOS `Docs/tooling.md` "Phase 0 verified results". Every entry says who ran it. "Worker" means the Phase 0a worker sandbox, run 2026-11-16 on the host described in section 4.

## 1. Toolchain

| Item | Result | Who |
|---|---|---|
| Gradle | `./gradlew --version` prints `Gradle 9.9.1`, Kotlin 2.4.21 (embedded), launcher JVM `17.0.21.0.3 (Azul Systems)`. AGP 9.5.1 accepts 9.9.1, so the 9.6.0 fallback was not used (OQ-A35). | Worker |
| KGP on the build classpath | `./gradlew buildEnvironment`: `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 -> 2.4.21` | Worker |
| AGP | `com.android.tools.build:gradle:9.5.1` | Worker |
| Configuration cache | second `./gradlew help` prints `Reusing configuration cache.` | Worker |
| JDK 21 for tests | foojay provisioned `~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2` (Temurin 21.0.13) | Worker |
| `compileSdk` form | `compileSdk = 37` works with the installed `android-37.0` platform (OQ-0a-13); the `release(37)` fallback was not needed | Worker |
| `android.onlyEnableUnitTestForTheTestedBuildType=false` | accepted; `:app:testReleaseUnitTest` runs `ReleaseAppIdentityTest` | Worker |
| `aapt2 dump badging` (debug APK) | `package: name='net.chronoflow.app.native.debug' versionCode='10000' versionName='0.1.0'`, `minSdkVersion:'29'`, `targetSdkVersion:'36'` (aapt2 37 prints `minSdkVersion`, not `sdkVersion`) | Worker |

## 2. Sandbox capabilities

| Capability | Runs? | Evidence |
|---|---|---|
| Gradle daemon (`./gradlew help`) | yes | `BUILD SUCCESSFUL` |
| Dependency download | yes | AGP 9.5.1, KGP 2.4.21, Konsist, bundletool resolved |
| foojay JDK 21 download | yes | `~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2` |
| JVM tests (`./gradlew :konsist:test`) | yes | 23 tests, 0 failures |
| `python3 -I` scripts (`Scripts/test_scripts.sh`) | yes | `Ran 22 tests in 8.8s OK` |
| `adb devices` | yes | lists `emulator-5554 device` once `pixel` is booted |
| Emulator boot | yes | `pixel` booted headless with `-no-window -no-audio -no-boot-anim -gpu host`; the log reports `Hardware GPU requirements ... passed`, `androidboot.debug.hwui.renderer=skiagl` (OQ-0a-11) |
| `Scripts/emulator.sh ensure` | yes, skip path only | with `pixel` already booted it applied the functional prep (`make check`). The boot path of `ensure`, `down` and `wipe` was not exercised on a real emulator; their shell logic is covered by fake-`adb` tests |
| `:app:connectedDebugAndroidTest` | yes | `AppLaunchSmokeTest`, 1 test, 0.37 s |
| `bundletool` via Gradle (`make size`) | yes | needs `--aapt2` (below) |
| `git commit` (`.git/index.lock`) | yes | all commits were made by the worker |

## 3. Commands the orchestrator must run outside the sandbox

None were blocked in this run. Still the orchestrator's: the T2 AVD matrix (`make avd-create AVD=fold`, `make matrix`), `Scripts/emulator.sh wipe` (`make emu-clean`) and the cold boot path of `Scripts/emulator.sh ensure` (stop `pixel` with `make emu-down`, then `make emu`), because they were not run here.

## 4. Tier timings

Command: `Scripts/measure-tiers.sh` (one warm-up, then three measured runs per tier). Host Apple M4 Max, 36 GiB. Emulator `pixel` (Pixel 8, API 36), already booted. Commit `5d9aabe`, 2026-11-16, debug build, Gradle build cache and configuration cache warm.

| Tier | Runs (s) | Median (s) | Budget (s) |
|---|---|---|---|
| `make quick` | 12.11, 11.91, 11.94 | 11.94 | 60 |
| `make check` | 16.22, 16.56, 16.33 | 16.33 | 300 |

These are warm, mostly up-to-date runs. A cold `make check` after a source change takes longer (first `make check` in a clean clone via `Scripts/ci.sh`: 21 s with the Gradle caches warm). Phases that add real tests must re-measure.

## 5. Gates

| Gate | Result |
|---|---|
| Lint probe | `if (Build.VERSION.SDK_INT >= 21)` around `setContent`: `./gradlew :app:lintDebug` exit 1, `MainActivity.kt:13: Error: Unnecessary; SDK_INT is always >= 29 [ObsoleteSdkInt]`. Reverted. |
| `allWarningsAsErrors` probe | Kotlin 2.4.21 does not report an unused variable on the command line, so `val unused = 1` compiles. Probe used a redundant cast instead: `./gradlew --no-build-cache :app:compileDebugKotlin` exit 1, `e: warnings found and -Werror specified`, `MainActivity.kt:14:27 Cast is redundant.` Reverted. (Deviation from the brief's probe.) |
| Spotless probe | trailing spaces on a line of `MainActivity.kt`: `spotlessCheck` fails and names `app/src/main/kotlin/net/chronoflow/app/MainActivity.kt`. Reverted. |
| `lint.xml` ignores | `GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion` (versions are pinned by A1, not by Lint). |
| `tools:ignore` on `MainActivity` | `DiscouragedApi` and `LockedOrientationActivity`, because A10/D16 fixes `screenOrientation="portrait"`. |
| `:app:analyzeReleaseR8Config` | present; `make check` runs it. Output: `R8 Keep Rules analysis report generated: build/reports/r8/r8-config-analyzer-release.html`. |
| `verifyReleaseManifest` on the real app | `./gradlew :app:verifyReleaseManifest` passes; `build/reports/verifyReleaseManifest.txt` holds `OK`. |
| `make size` | `Bundle size: 521695 bytes (0.52 MB), arm64-v8a/xxhdpi/en: OK` |
| AAB entries | 86 files; 4 `.so` entries, all `libandroidx.graphics.path.so` (arm64-v8a, armeabi-v7a, x86, x86_64). See OQ-0a-22. |
| Release has no debug code | `grep -c DebugStrictMode app/build/outputs/mapping/release/mapping.txt` prints `0`. |
| Konsist | `./gradlew :konsist:test`: 19 rule tests and 4 module-graph tests pass, every planted violation is rejected with its rule name in the message, and every control passes. A planted `GlobalScope` import in a real module made `noGlobalScope` fail on the next run (the task inputs cover all `*.kt` and `*.kts`). Reverted. |
| `make tokens` | prints `make tokens: lands in Phase 0b (feat/phase-0b-tokens)`; the recipe exits 1, GNU make reports `Error 1` and exits 2 itself. `make generate` and `make generators-check` exit 0. |
| `Scripts/ci.sh` | clean clone of the branch: exit 0 (a first run failed because `__pycache__` files were committed; fixed by `chore: ignore python bytecode`). |

## 6. App

Runner: worker, `pixel` (emulator-5554), debug build.

```
./gradlew :app:installDebug                         -> Installed on 1 device.
adb -s emulator-5554 shell pm list packages | grep native.debug -> package:net.chronoflow.app.native.debug
adb -s emulator-5554 shell am start -W -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity
  -> Status: ok, LaunchState: COLD, TotalTime: 351, WaitTime: 353
adb -s emulator-5554 shell pidof net.chronoflow.app.native.debug -> 3875
```

The `native` segment installs, so OQ-A1 keeps `net.chronoflow.app.native`. `AppLaunchSmokeTest.launchesAsNativeDebugUnderStrictMode`: passed in 0.37 s (`build/test-results/smoke/timings.json`), under the launch-path `penaltyDeath` policy.

`adb logcat -d -s StrictMode` after the launch shows `DiskReadViolation` lines from other processes on the shared emulator (pid 1763, framework frames only, none with a `net.chronoflow.` frame: `grep -c net.chronoflow` prints `0`). They are logged by the policy and are not fatal (OQ-0a-2).

## 7. Merged manifest inventory (release, OQ-0a-10)

Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `net.chronoflow.app.native.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (androidx.core, OQ-0a-20).

`androidx.startup.InitializationProvider` meta-data entries: `androidx.emoji2.text.EmojiCompatInitializer`, `androidx.lifecycle.ProcessLifecycleInitializer`, `androidx.profileinstaller.ProfileInstallerInitializer`.

Other library components: `androidx.profileinstaller.ProfileInstallReceiver` (exported, guarded by `android.permission.DUMP`); `<uses-library>` entries `androidx.window.extensions` and `androidx.window.sidecar`. Phase 3a removes EmojiCompatInitializer through tools:node="remove"; only the lifecycle and profile installer entries remain. verifyReleaseManifest rejects any other androidx.startup initializer. Their measured costs are recorded in docs/perf-startup.md.

## 8. Deviations and open questions

- **OQ-0a-22 (new): `libandroidx.graphics.path.so`.** Compose UI pulls in `androidx.graphics:graphics-path`, which ships this library in the AAB. The brief and AGENTS.md say "no `.so`". Default applied: `MeasureBundleSizeTask` allows exactly that file name (`BundleSize.allowedNativeLibraries`) and fails on any other `.so`. The size gate still counts it (0.52 MB total). Raise with the user if a hard "no `.so`" is wanted (it would need a dependency exclusion, which risks a runtime failure in `Path` conic conversion on API < 34).
- **bundletool `--aapt2`.** bundletool 1.18.3 cannot extract its embedded aapt2 on this Mac (`Could not extract aapt2`). `measureBundleSize` passes the SDK's aapt2 from `androidComponents.sdkComponents.aapt2`.
- **`android:fullBackupContent`.** Lint (`DataExtractionRules`, an error with `minSdk 29`) requires it next to `dataExtractionRules`. Added `app/src/main/res/xml/backup_rules.xml` with the same exclusions. `allowBackup="false"` still holds.
- **`xmlReport` and `htmlReport`** are deprecated in AGP 9 (lint reports are always generated), so `LintSetup` does not set them.
- **Keep rules.** `optimization.keepRules.files` is deprecated in favour of the `src/main/keepRules` source folder, so `app/src/main/keepRules/app.keep` needs no Gradle line.
- **Compose test rule.** `createAndroidComposeRule` is deprecated (a warning, an error here); the smoke test uses `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`.
- **Compose rules artifact.** The catalog has one extra library entry, `compose-rules-ktlint` (`io.nlopez.compose.rules:ktlint`, version `composeRules`), so `SpotlessConventionPlugin` reads it from the catalog.
- **SQLDelight (OQ-0a-6).** `chronoflow.sqldelight` configured under AGP 9 without a fallback; `:core:database` applies it.
- **Spotless plugin** is registered and applied in the quality-gates commit (the brief lists it among the ten plugins of Task 4).
- **`adb shell pgrep`.** `emulator.sh busy` uses `pgrep -f 'instrumen[t]'` so the on-device `sh -c` command line does not match itself.
- **Probe for `allWarningsAsErrors`:** see section 5.
- **`make` exit status.** A failing recipe makes GNU make exit 2, not 1; the recipe's own status is 1.
- **Branch hygiene.** The Makefile/scripts commit also added `Scripts/**/__pycache__/*.pyc` by mistake; `chore: ignore python bytecode` untracks them and adds `__pycache__/` and `*.pyc` to `.gitignore`.
- OQ-0a-1 through OQ-0a-21 defaults were applied as written (OQ-0a-3 and OQ-0a-4 annotations are in `plan.md` "Locale wiring"; OQ-0a-19 and OQ-0a-20 edits are in `plan.md`).

## Phase 0b: stale-output proof

Worker, 2026-11-16. One hex edited by hand in `CfColors.generated.kt` (`Color(0xFF1F285A)` to `Color(0xFF1F285B)`), then `make generators-check`:

```
python3 -I Scripts/check-sha256-manifest.py tools/tokengen/ios-inputs.sha256 && ./gradlew --console=plain -q :tools:tokengen:verifyTokens :tools:tokengen:test
TokenGen: stale generated output: core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/generated/CfColors.generated.kt
...
Execution failed for task ':tools:tokengen:verifyTokens' (registered in build file 'tools/tokengen/build.gradle.kts').
> Process 'command '/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home/bin/java'' finished with non-zero exit value 1
make[1]: *** [tokens-check] Error 1
make: *** [generators-check] Error 2
```

After `git checkout` of the file, `make generators-check` exits 0. The check needs neither the network nor the iOS repo: it hashes the pinned iOS inputs, regenerates every output in memory and compares bytes.

## Pending: orchestrator

- Cold-boot path of `Scripts/emulator.sh ensure`, `down` and `wipe` against a real emulator (not exercised; `pixel` was already booted).
- `make du` output at the end of the session is in the hand-back.

## Phase 0e: test harness, device profiles and perf wiring

Worker, 2026-11-17, same host as section 4 (Apple M4 Max). Every command ran in the worker sandbox; the orchestrator ran none for this phase.

### Tiers (OQ-0e-2, OQ-0e-17)

- JUnit Platform 5.14.4 with the Vintage engine (`junit-bom` 5.14.4; never 6.x). The `PostDiscoveryFilter` and the timing listener in `:core:testing-junit` register through `META-INF/services` and work under the AGP unit-test tasks, so the fallback (`@Category`) was not needed.
- Proof: `./gradlew :core:ui:testDebugUnitTest -Pcf.tier=layout` runs no class (`core/ui/build/test-results/testDebugUnitTest/` has no XML, `core/ui/build/cf-timings/testDebugUnitTest/<pid>.json` has an empty `tests` list). `./gradlew :core:testing-compose:testDebugUnitTest -Pcf.tier=quick` runs the `@Logic` classes and no `@Layout` class, and writes `core/testing-compose/build/cf-timings/testDebugUnitTest/<pid>.json`.
- `Test.failOnNoDiscoveredTests` exists in Gradle 9.9.1 and is set to `false` in `TestLauncher.kt`; a tier with no classes in a module passes, and run-tier still fails on missing XML or timing files (exit 2).
- `core:ui` tests (`CopyIndexResolutionTest`, `DebugRouteResolutionTest`) are `@Logic`, so `make quick` and `make test` run them through the unqualified `testDebugUnitTest`.
- `make quick` runs `:build-logic:convention:test` as its own tier call (`build-logic`), because that included build runs outside the conventions and writes no timing file (OQ-0e-23).
- `make test` runs `$(JVM_MODULES:%=:%:test) testDebugUnitTest :app:testReleaseUnitTest -Pcf.tier=test` and passes (499 budgeted tests, 0 over budget). `ConfigurationPolicyTest` also runs in the release unit-test task there without a problem; a bare `./gradlew test` was not tried (it would add the perf variants' unit tests).
- Budget gate: `make quick` first full run had `overBudget: 0` with no `@PerfBudget` anywhere, so no Konsist class needed an exemption (OQ-0e-21). Slowest quick tests: `HarnessProbeSnapshotTest`-class sandbox start 2.0 s (first Robolectric test on SDK 36, `warmup`), `LauncherIconInflationTest` 1.5 s, then `KonsistRulesTest#routesAreSerializable` 0.40 s.
- Robolectric warm-up (first test per SDK in one JVM, `ConfigurationPolicyTest`): SDK 29 0.59 s, 32 0.41 s, 33 0.39 s, 36 1.05 s. The 20 s warm-up cap is far above what this host needs.
- Layout guards run light only (user decision 2026-11-16): `LayoutMatrix.phone` is `small`-nl, `largeFont`-nl, `pixel8`-en; `LayoutMatrix.wide` adds `foldInner`-en and `landscapeSideNav`-en (OQ-0e-28).

### Timings (single runs; the three-run `Scripts/measure-tiers.sh` was not run)

| Command | Result |
|---|---|
| `make quick` | 42.5 s wall (Gradle 26.6 s, scripts 12 s), `overBudget: 0`, 471 budget checks, 0 unbudgeted, after one source change |
| `make check` | 29.1 s wall, warm, `pixel` booted; smoke: 9 emulator tests, slowest `ConfigurationPolicyDeviceTest#nightModeChangeKeepsCapAndLocale` 0.60 s |
| `make test` | 10.4 s wall, warm |

### ConfigurationPolicy (OQ-A24)

`applyOverrideConfiguration` in `MainActivity.attachBaseContext` held on API 36: all four `ConfigurationPolicyDeviceTest` cases passed (app locale `ar-EG` gives `en`, LTR and Latin digits; `nl` gives `nl`; system font scale 2.0 gives 1.3; an in-place `cmd uimode night yes` kept font scale 1.3, locale `en` and LTR). The API < 33 path is proven on Robolectric at SDK 29 and 32 (`ConfigurationPolicyTest`) and through the injected `sdkInt` in `DeviceConfigStrictModeTest`; the `api29` AVD was not run. No AppCompat fallback was needed.

### Perf variants (OQ-A28)

- `androidx.baselineprofile` 1.5.0 adds `benchmarkRelease` and `nonMinifiedRelease` to `:app` itself; `onVariants(selector().withBuildType(...))` adds `src/perf/kotlin` and `BuildConfig.PERF_ENTRY = true` to both. No hand-written `benchmark` build type was needed.
- `./gradlew :app:compileNonMinifiedReleaseKotlin :app:compileBenchmarkReleaseKotlin :app:compileReleaseKotlin`, then `find app/build -name 'PerfEntry*.class'` lists only `.../nonMinifiedRelease/...` and `.../benchmarkRelease/...`.
- Both perf merged manifests have `<profileable android:shell="true">` (added by the plugin, so no `src/<variant>/AndroidManifest.xml`); the `release` merged manifest has none.
- `./gradlew :app:dependencies --configuration <c> | grep -c data-mock`: `releaseRuntimeClasspath` 0, `benchmarkReleaseRuntimeClasspath` 1, `nonMinifiedReleaseRuntimeClasspath` 1.
- `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -c core:testing`: `0` (2026-11-17). This is the proof that `LargeTimelineFixtures` ships in no release (OQ-1d-20).
- `"benchmarkReleaseImplementation"(project(":core:data-mock"))` fails (`Configuration with name 'benchmarkReleaseImplementation' not found`), so `app/build.gradle.kts` uses the `configurations.matching { … }.configureEach { project.dependencies.add(name, project(":core:data-mock")) }` fallback (OQ-0e-26). `ModuleGraphTest.dataMockOnlyInDebugAndPerf` accepts it and rejects a planted `implementation(...)`.
- `:app:testReleaseUnitTest :app:verifyReleaseManifest` pass; `ReleaseReadinessTest` proves no seed or perf class and `PERF_ENTRY == false` in release.
- `android.experimental.self-instrumenting` was not set and `StartupBenchmark` ran, so the baseline profile plugin covers it. The benchmark reads `targetAppId` from the instrumentation arguments and falls back to `net.chronoflow.app.native`, the perf variants' id.

### Timeouts (OQ-A29)

A temporary `HangCapProbeTest` (`@Smoke`, `CountDownLatch(1).await(70, SECONDS)`, deleted afterwards) run with `make targeted ONLY="app:HangCapProbeTest"` failed after 60.013 s with `org.junit.runners.model.TestTimedOutException: test timed out after 60000 milliseconds`, so the instrumentation runner honours `timeout_msec=60000` and `ScreenTest` needs no `Timeout` rule. Other caps: Gradle `Test.timeout` 10 min (`TestLauncher.kt`), `runTest(timeout = 60.seconds)` (Konsist `runTestHasTimeout`), run-tier 600 s idle and 3,600 s total, and 300 s only in `Scripts/perf-startup.sh`.

### Seeds

- Manual use:
  ```sh
  adb shell am start -n net.chronoflow.app.native.debug/net.chronoflow.debug.SeedReceiverActivity \
    --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_UI_TEST_PROFILE screen --es cf.settings.language nl
  adb logcat -d -s UITestTimeline   # CF_UI_TIMELINE ... event=seed-begin / seed-applied
  ```
  App locales persist across runs: reset with `--es cf.settings.language system` (when the receiver is not already on top) or by running `ScreenTestHarnessTest`, whose `@After` resets them.
- `seed-begin` to `seeded-screen-visible` (`ScreenTestHarnessTest`, `am instrument`, 5 runs): 0.258, 0.229, 0.234, 0.230, 0.215 s, median 0.23 s (iOS target 1 s, not asserted).
- The receiver declares `configChanges="locale|layoutDirection|uiMode|fontScale"`: without it the system recreated the activity when the app locale changed and the seed was applied twice (OQ-0e-29).

### Insets, benchmark and device matrix

- `docs/device-matrix.md` has the measured `pixel` insets (top 132 px, bottom 63 px, gesture navigation) and `PixelProfileInsetsTest` passes on `pixel`.
- `make perf` on `pixel` (emulator, `benchmarkRelease`, `Partial(UseIfAvailable)`, `-gpu host`): `Startup metric: startupCold timeToInitialDisplayMs median=88.7 ms REPORT` (min 79.9, max 97.1); the row is in `docs/perf-startup.md`. Emulator numbers are regression signals, not device claims. `build/emulator/gpu-mode` did not exist because the emulator was booted before this phase; it was written by hand after `ps` showed `-gpu host` (a later cold boot through `Scripts/emulator.sh ensure` writes it).

## Phase 2: primitive captures

`make captures ONLY=designsystem` writes 52 Robolectric PNGs under `build/captures/designsystem/` (13 sections × en/nl × light/dark). All native windows render on SDK 36; no device-only skips were needed. `make captures DEVICE=1 ONLY=designsystem` writes the matching 52 under `build/captures/device/designsystem/`. Each language/theme/section is a separate parameterized test, keeping the per-test budget visible.

`BoardCapture.capture(rule, name)` retains its signature. It selects the JVM path only when `Build.FINGERPRINT == "robolectric"`; the instrumentation registry also exists on Robolectric and cannot distinguish them. On pixel, AGP supplies `additionalTestOutputDir` and pulls the images to `core/ui/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/pixel(AVD) - 16/designsystem/`. The fallback external-files path was not needed. `Scripts/capture-device.py` runs the Gradle invocation and PNG copy under the host-wide device lock. A nonmatching prefix (`make captures DEVICE=1 ONLY=Nope`) passed with zero PNGs.

The androidTest dependency on `:core:testing-compose` excludes `org.robolectric` and `io.github.takahirom.roborazzi`; those JVM dependencies do not belong in the device APK. The device gallery sets and restores the target application's real locale before/after the Compose activity: a composition-local override alone leaves native dialog headings in the activity locale. The JVM gallery uses `ProfileRule` as before. `make captures ONLY=measurements/phase2` adds two light-only Dutch app-bar measurements on small/largeFont outside the 52-PNG gallery prefix.

Native-window guards use window-local geometry. Activity-inset assertions and rectangular overlap assertions do not describe separate windows or circular clock targets. Device captures verify native rendering; whole-display system-chrome placement review remains in Phase 14 (OQ-A42). Designed components retain the full guard set; the measured one-physical-pixel text/calendar rounding and native shared segment borders are documented in `docs/design-system.md`.

An optional host-process diagnostic could not run in the sandbox:

```text
$ ps -axo pid,etime,command | rg 'baseline|connectedNonMinified|GradleWrapperMain|run_tier'
zsh:1: operation not permitted: ps
```

Device logcat showed the profile iterations progressing; no required Gradle, adb or emulator command was blocked. Kotlin daemon marker creation under `~/Library/Application Support/kotlin/daemon/` also logs `Operation not permitted`; Kotlin's fallback compilation succeeds. No permission escalation or sibling-repo write was used.

Phase 2 gate close-out: the standalone quick run failed two rule budgets (1193/1436 ms); the rules were narrowed without changing limits and their six affected methods passed (267/2 ms for the project checks). The quick prerequisite in `make check` passed 876 budget checks with zero over budget. That single check invocation stopped on a modifier-factory lint issue, which was fixed directly; its remaining Makefile steps resumed once and passed (27 release tests, 197 layout tests, generators, 10 smoke tests). Exact logs are `build/phase2-{quick,rule-budget-fix1,check,check-resumed}.log`. A final log review found a pre-existing `filesDir` read in ScreenTestHarnessTest's onActivity callback; the test now reads on its worker, and its scoped rerun passed with clean fresh StrictMode output.

## Phase 3b process-death proof

`make perf` includes `net.chronoflow.benchmark.ProcessDeathLaunchTest` in the separate benchmark APK against the perf variant. The single-class command (under the device lock) is:

```sh
python3 -I Scripts/device_lock.py -- ./gradlew --console=plain :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.ProcessDeathLaunchTest -Pandroid.testInstrumentationRunnerArguments.timeout_msec=60000
```

The test launches the explicit `MainActivity` component with `am start -W`, kills the background process with `am kill`, waits for its PID to disappear through `UiDevice.wait`, then proves the relaunch has a different PID and shows `launch.opening`. It explicitly disables Reduce Motion so the splash remains observable with functional animator scales at 0. Both `benchmarkRelease` and `nonMinifiedRelease` target `net.chronoflow.app.native` (also the `targetAppId` fallback); `app.root` and `route.*` tags are exposed by the main-source root in release variants. `make baseline-profile` selects only `BaselineProfileGenerator`: benchmark rule filtering alone cannot exclude this plain JUnit test.

The functional hand-off/stall proofs use the debug `LaunchController.frameLog` ring buffer. The existing `compositionObserver` remains a composition counter. Read `CfLaunchProof` in logcat for ready/slow/stall hand-off end times and the sampled stall gap.
