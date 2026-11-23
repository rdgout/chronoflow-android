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
| `aapt2 dump badging` (debug APK) | `package: name='net.chronoflow.app.debug' versionCode='10000' versionName='0.1.0'`, `minSdkVersion:'29'`, `targetSdkVersion:'36'` (aapt2 37 prints `minSdkVersion`, not `sdkVersion`) | Worker |

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
adb -s emulator-5554 shell pm list packages | grep native.debug -> package:net.chronoflow.app.debug
adb -s emulator-5554 shell am start -W -n net.chronoflow.app.debug/net.chronoflow.app.MainActivity
  -> Status: ok, LaunchState: COLD, TotalTime: 351, WaitTime: 353
adb -s emulator-5554 shell pidof net.chronoflow.app.debug -> 3875
```

The `native` segment installs, so OQ-A1 keeps `net.chronoflow.app`. `AppLaunchSmokeTest.launchesAsNativeDebugUnderStrictMode`: passed in 0.37 s (`build/test-results/smoke/timings.json`), under the launch-path `penaltyDeath` policy.

`adb logcat -d -s StrictMode` after the launch shows `DiskReadViolation` lines from other processes on the shared emulator (pid 1763, framework frames only, none with a `net.chronoflow.` frame: `grep -c net.chronoflow` prints `0`). They are logged by the policy and are not fatal (OQ-0a-2).

## 7. Merged manifest inventory (release, OQ-0a-10)

Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `net.chronoflow.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (androidx.core, OQ-0a-20).

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
- Parameterized row names must be unique. The navigation tables repeat route names across variants; use `@Parameterized.Parameters(name = "{index}: {0}")`, following the indexed cases in database/datastore tests. With duplicate names, Vintage cannot satisfy every exclusion and leaves synthetic `Gradle Test Run` cases without method timing sources. The navigation module already receives the filter/listener through the Android-library convention; adding a tier annotation or changing its module type is unnecessary.
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
- `android.experimental.self-instrumenting` was not set and `StartupBenchmark` ran, so the baseline profile plugin covers it. The benchmark reads `targetAppId` from the instrumentation arguments and falls back to `net.chronoflow.app`, the perf variants' id.

### Timeouts (OQ-A29)

A temporary `HangCapProbeTest` (`@Smoke`, `CountDownLatch(1).await(70, SECONDS)`, deleted afterwards) run with `make targeted ONLY="app:HangCapProbeTest"` failed after 60.013 s with `org.junit.runners.model.TestTimedOutException: test timed out after 60000 milliseconds`, so the instrumentation runner honours `timeout_msec=60000` and `ScreenTest` needs no `Timeout` rule. Other caps: Gradle `Test.timeout` 10 min (`TestLauncher.kt`), `runTest(timeout = 60.seconds)` (Konsist `runTestHasTimeout`), run-tier 600 s idle and 3,600 s total, and 300 s only in `Scripts/perf-startup.sh`.

### Seeds

- Phase 10b Body clock board states (debug only; install `app/build/outputs/apk/debug/app-debug.apk` first if the connected-test runner removed it). Each command supplies a fresh test id, applies the fixture before demo sign-in, and opens the stateless Body clock page:

  ```sh
  adb shell am start -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
    --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_UI_TEST_PROFILE screen \
    --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
    --es CF_SETTINGS_ROUTE bodyClock --es CF_SETTINGS_BOARD_STATE bodyMissing

  adb shell am start -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
    --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_UI_TEST_PROFILE screen \
    --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
    --es CF_SETTINGS_ROUTE bodyClock --es CF_SETTINGS_BOARD_STATE bodyReady

  adb shell am start -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
    --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_UI_TEST_PROFILE screen \
    --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
    --es CF_SETTINGS_ROUTE bodyClock --es CF_SETTINGS_BOARD_STATE bodyAdjusted

  adb shell am start -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
    --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_UI_TEST_PROFILE screen \
    --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
    --es CF_SETTINGS_ROUTE bodyClock --es CF_SETTINGS_BOARD_STATE bodyExcluded
  ```

  Append `--es cf.settings.theme light` or `dark` for a fixed appearance. Omit it to preserve this device's choice. All four states are ignored without a valid `CF_AUTH_TEST_ID`. `bodyReady` is the default fixture. The capture command is `make captures ONLY=SetPreferences,SetPreferencesNL,SetBodyClock,DarkSetBodyClock`; state/section PNG names and verification results are recorded in [settings preferences and body clock](settings-preferences-body-clock.md).

- Manual use:
  ```sh
  adb shell am start -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
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

The test launches the explicit `MainActivity` component with `am start -W`, kills the background process with `am kill`, waits for its PID to disappear through `UiDevice.wait`, then proves the relaunch has a different PID and shows `launch.opening`. It explicitly disables Reduce Motion so the splash remains observable with functional animator scales at 0. Both `benchmarkRelease` and `nonMinifiedRelease` target `net.chronoflow.app` (also the `targetAppId` fallback); `app.root` and `route.*` tags are exposed by the main-source root in release variants. `make baseline-profile` selects only `BaselineProfileGenerator`: benchmark rule filtering alone cannot exclude this plain JUnit test.

To diagnose only startup on the pixel emulator, under the same device lock (no profile generation or history append):

```sh
python3 -I Scripts/device_lock.py -- bash -c '
set -e
restore() { Scripts/emulator.sh prep-functional emulator-5554; }
trap restore EXIT
Scripts/emulator.sh prep-perf emulator-5554
./gradlew --console=plain :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.StartupBenchmark -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR -Pandroid.testInstrumentationRunnerArguments.timeout_msec=60000
'
```

Macrobenchmark calls the WARM measure block once cold with `iteration == null` before iteration 0. That unmeasured preparation waits for the normal cold hand-off; each measured warm iteration still requires no overlay and a route within the original 2 s bound. The diagnostic command uses scales 1 so it exercises the complete cold preparation, and restores functional settings under the same lock. Use `make perf` on a quiet host for recorded performance results.

The functional hand-off/stall proofs use the debug `LaunchController.frameLog` ring buffer. The existing `compositionObserver` remains a composition counter. Read `CfLaunchProof` in logcat for ready/slow/stall hand-off end times and the sampled stall gap.


## Phase 4a

SQLite reports **3.44.3** in Robolectric's native framework driver (`AccountStorageLifecycleTests.frameworkSqliteIsAtLeastTheApi29Floor`) and **3.44.3** on the API 36 `pixel` emulator (`StoreThreadingSmokeTest`, `StoreThreadingSmoke` logcat tag). These checks identify the harness engines; no API 29 emulator was requested. SQLDelight retains its sqlite-3-18 dialect and the SQL safety scanner rejects newer syntax.

The single warm `make quick` invocation took **30.97 s** overall (log creation to final output). Its quick tier took 14.60 s: 1,243 cases, zero failures and zero budget overruns; database contributed 42 cases and datastore 32. Build-logic took 1.06 s (26 cases), the four Python suites passed 190 cases, and the final serial timing tier took 1.93 s (6 cases, zero overruns).

Device tests ran once per class through `make targeted`, under the device lock. `KeystoreSessionStoreTest` passed five cases in 0.122 s of reported case time (10.43 s including Gradle/build/install); `StoreThreadingSmokeTest` passed its one case in 0.103 s (5.52 s including Gradle/build/install). Both used isolated roots and `test.<uuid>` key aliases; their combined case time was 0.225 s. The strict main-thread disk policy survived every store operation. These are functional emulator timings, not device performance claims.

Commands, fix history, brief-refresh patches and remaining broad gates are recorded in [phase-4a-validation.md](phase-4a-validation.md). Exact local run logs and tier JSON snapshots are under `build/phase4a/`.


## Phase 4c: mock source, seeds and release boundary

2026-11-18, macOS host, Zulu JDK 17 build / provisioned JDK 21 tests. This phase ran JVM checks, the release build and static APK probes. It produced no emulator, capture, timeline or device performance result.

All targeted commands use `make targeted ONLY="core:data-mock:<Class> …"` (the brief lists each class). From Task 6 onward the command environment sets `ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process`. Before that override, Kotlin's daemon could not create its client marker under `~/Library/Application Support/kotlin/daemon/` (`java.nio.file.FileSystemException: …: Operation not permitted`); compilation fell back successfully. The override avoids that sandbox path without changing repository configuration.

| Task | Targeted coverage | Cases | Wall time | Result |
|---|---|---:|---:|---|
| 1 | time, crypto, limiter, latency; fixture import boundary | 19 + 4 | Gradle 1 s + 2.25 s | pass; 0 over budget |
| 2 | state codec, atomic stores, legacy claim | 9 | 1.83 s | pass; 0 over budget |
| 3 | settings readiness and codec | 6 | 1.81 s | pass; 0 over budget |
| 4 | seed/rebase suite | 11 | 1.81 s | pass; 0 over budget |
| 5 | generation helpers | 3 | 7.60 s | pass; 0 over budget |
| 6 | auth and confirmation | 26 | 8.80 s | pass; 0 over budget |
| 7 | account, persistence, passkeys, security/delete | 28 | 2.13 s | pass; 0 over budget |
| 8 | app seed and refresh through source | 18 | 3.00 s | pass; 0 over budget |
| 9 | wizard, guest and claims | 59 | 3.43 s | pass; 0 over budget |
| 10 | API parity and all method controls | 81 | 2.38 s | pass; 0 over budget |
| 11 | selected Konsist guards / release mock absence | 12 / 1 | 2.80 s / 2.90 s | pass; 0 over budget |

Task 4 also reran its seven timestamp cases in 2.13 s after making their expected local times and offsets independent of production helpers. Task 7's failed-save case reaches the profile write after a successful session refresh; it proves the unsaved candidate does not become visible. Parameter rows are separate JUnit cases: method controls 75, passkey add faults 7, password confirmations 6, guest possession 6, stale claims 10 and claim replay 6. The method-coverage table matches all 25 `MockMethod` entries.

### Dependency and APK proof

With `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home` and `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`:

```sh
./gradlew :app:dependencies --configuration <configuration>
```

Each report exited 0 with the configuration cache enabled. Counting the report using `grep -c "project ':core:data-mock'"` and `grep -c "project ':core:testing'"` gives:

| configuration | data-mock | testing | command wall time |
|---|---:|---:|---:|
| `releaseRuntimeClasspath` | 0 | 0 | 1.44 s |
| `debugRuntimeClasspath` | 1 | 2 | 1.28 s |
| `benchmarkReleaseRuntimeClasspath` | 1 | 2 | 1.27 s |
| `nonMinifiedReleaseRuntimeClasspath` | 1 | 2 | 1.25 s |

The old brief's unquoted `project :core:…` pattern returned 0 for every report; Gradle 9 quotes project paths. The corrected debug/perf positive controls prove the release zeros. No dependency line in `app/build.gradle.kts` changed.

```sh
./gradlew :app:assembleRelease :app:testReleaseUnitTest :app:verifyReleaseManifest
```

Exit 0, `BUILD SUCCESSFUL in 4s`; all 40 release JVM cases passed. `ReleaseReadinessTest.mockDataSourceIsAbsent` checks four classes. OQ-A166 gives these lookups to 4c; 4e keeps its other release assertions.

```sh
$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer dex packages app/build/outputs/apk/release/app-release-unsigned.apk
```

Exit 0, 0.57 s. `grep -c "net.chronoflow.core.datamock"` on that output prints `0`. APK ZIP inspection finds only `libandroidx.graphics.path.so` under the four ABI directories, all already allowed by OQ-0a-22; no new native library.

`onlyDebugAndPerfImportTheMock` passes on the project, rejects the two planted main-source imports and accepts its debug, perf and test controls. The existing JVM/import/sleep/OkHttp/module-graph guards also pass. `testingNotInMain` keeps its prior boundary and adds only the named mock fixture imports (OQ-A151).

### Final inner-loop result

`env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make quick` ran once, exited 0, and took **26.95 s wall time** including tooling tests and the serial timing tier (under 60 s). Its primary tier recorded 1,734 cases in 10.54 s; build-logic recorded 26 in 1.41 s; timing recorded 6 in 1.39 s. All tier reports have zero over-budget cases. The mock module's final XML contains **249 cases in all 31 required classes**; every mock case is under 1 s, with no PerfBudget annotation. The first Task 1 helper duration above is Gradle's reported build duration; its separate wrapper wall time was not retained. Its cases are all included in this final result.

Five slowest mock cases from the final JVM timing report:

| Class | Method | Time |
|---|---|---:|
| `MockAppSeedTests` | `defaultAppMockReseedsIncompatibleFilesEmptyObject` | 562 ms |
| `DataSourceParityTests` | `invalidSettingsReturnEveryFieldReminders` | 496 ms |
| `MockAccountTripsTests` | `unsupportedAndSensitivePatchRefusal` | 387 ms |
| `PlanDecodingTests` | `mockTripsRebaseEveryTimestampAndDate20000229` | 368 ms |
| `MockAuthTests` | `newPasswordPolicyAndDeviceValidation` | 308 ms |

Results: `build/test-results/quick/`, `build/test-results/build-logic/`, `build/test-results/timing/`, `core/data-mock/build/test-results/test/` and `core/data-mock/build/cf-timings/test/`. The release XML is under `app/build/test-results/testReleaseUnitTest/`.

The phase intentionally leaves `make check` (including the pixel emulator tier) to the user. No `make perf`, `make perf-timeline`, `make baseline-profile`, emulator tier or T2 AVD was run. The APK and JVM probes completed in the sandbox; no requested command remains blocked.

## Phase 4e release inspection (2026-11-18)

`make targeted ONLY="app:ReleaseReadinessTest"` passed (6 s reported Gradle time), including session-seed class absence and native session restoration. The isolated `app:ReleaseSeedExtrasTest` passed (4 s). `./gradlew :app:verifyReleaseManifest :app:assembleRelease` passed (31 s).

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
  "$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer" dex packages \
  app/build/outputs/apk/release/app-release-unsigned.apk | grep -c datamock
```

Output: `0` (grep exits 1 for no matches). The APK's only native library basename is the existing OQ-0a-22 allow-list entry `libandroidx.graphics.path.so`, across arm64-v8a, armeabi-v7a, x86 and x86_64. No new `.so` was introduced. No device or performance tier was run for this inspection.


## Phase 4e signed-in seed and validation

Start a fresh process-isolated debug seed with the iOS account and gate keys:

```sh
adb shell am start -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1
```

Seeded roots are selected for the current process only. Reseed after process death. Add `--es CF_UI_TEST_PROFILE launch` for the real splash/readiness path; `screen` is the default seeded profile. Unverified and pending-terms gates use `CF_AUTH_TEST_UNVERIFIED=1` and `CF_AUTH_TEST_TERMS_PENDING=1`.

The only Phase 4e device test is run through the targeted runner's device lock:

```sh
Scripts/emulator.sh prep-functional emulator-5554
make targeted ONLY="app:SignedInColdLaunchSmokeTest"
```

The initial sandbox attempts could not see the emulator: preparation returned `adb: device 'emulator-5554' not found`, and the compiled smoke run failed with `No connected devices!` (7.42 s wall). The user subsequently reported a passing `make check`, including `SignedInColdLaunchSmokeTest.signedInColdLaunchReachesToday` in **3.4 s** on **pixel** (review round 1, 2026-11-18). This supersedes the pending smoke status. Full validation, diagnostic history and delegated performance measurements are recorded in [Phase 4e results](phase-4e-results.md).


## Phase 8 terms and walkthrough journey

Start the debug-only seeded journey on pixel:

```sh
adb -s emulator-5554 shell am start \
  -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" \
  --es CF_UI_TEST_PROFILE screen \
  --es CF_AUTH_TEST_SIGNED_IN demo \
  --es CF_AUTH_TEST_TERMS_PENDING 1 \
  --es CF_WALKTHROUGH_TEST_PENDING 1 \
  --es cf.settings.language en
```

Accept terms, then Next / Next / Done reaches Today while mutation sends remain offline. The only automated native journey is `make targeted ONLY="app:OnboardingJourneyTest"` (the targeted runner owns the device lock). `CF_AUTH_TEST_TERMS_CHANGES_PENDING=1` adds one preference change for Phase 10a's Terms sign-out confirmation; it does not hold sends offline by itself.

Validation and the 24 board captures are recorded in [Phase 8 results](phase-8-results.md).

## Reminder seeds (Phase 12c)

These three keys are ignored without `CF_AUTH_TEST_ID`. Authorization overrides are debug only and govern the scheduler, reminders page and Preferences row together. Omit authorization to use the real system state. Invalid authorization means undetermined. The requested flag records the one request; it does not grant the OS permission. The first-trip seed queues `wizardReminders = "pending"` through the real store; normal consumption writes `"seen"` before presentation.

Authorization (blocked page; `undetermined`, `granted` and `unsupported` are also supported):

```sh
python3 -I Scripts/device_lock.py -- "$ANDROID_HOME/platform-tools/adb" -s emulator-5554 shell am start -W \
  -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID 01900000-0000-7000-8000-0000000012c1 --es CF_UI_TEST_PROFILE screen \
  --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
  --es CF_SETTINGS_ROUTE reminders --es cf.notificationAuthorization blocked
```

Request bookkeeping (real permission source):

```sh
python3 -I Scripts/device_lock.py -- "$ANDROID_HOME/platform-tools/adb" -s emulator-5554 shell am start -W \
  -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID 01900000-0000-7000-8000-0000000012c2 --es CF_UI_TEST_PROFILE screen \
  --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
  --es CF_SETTINGS_ROUTE reminders --es cf.notificationPermissionRequested 1
```

First-trip sheet over a seeded trip:

```sh
python3 -I Scripts/device_lock.py -- "$ANDROID_HOME/platform-tools/adb" -s emulator-5554 shell am start -W \
  -n net.chronoflow.app.debug/net.chronoflow.debug.SeedReceiverActivity \
  --es CF_AUTH_TEST_ID 01900000-0000-7000-8000-0000000012c3 --es CF_UI_TEST_PROFILE screen \
  --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
  --es cf.notificationAuthorization undetermined --es cf.firstTripReminders 1 \
  --es CF_PLAN_TEST_ROUTE trip:0199a3c0-0000-7000-8000-000000000001
```

Run the real dialog journey alone with `make targeted ONLY="app:RemindersPermissionDeviceTest"`. It installs the APKs, revokes/clears flags outside instrumentation under the device lock, runs exactly that class, and writes `build/reminders-permission/result.xml` plus the tier timing report. `REUSE_BUILD=1` skips installation. In `make check`, the main smoke run excludes this class and keeps the APKs installed, then the same isolated runner executes it. A missing APK, missing metric or instrumentation failure fails the tier. Permission is never revoked inside a test process.

## Phase 13

Worker commands use ANDROID_HOME `/opt/homebrew/share/android-commandlinetools`, JDK 17 for Gradle and the provisioned JDK 21 test launcher. Detailed per-task commands, outcomes and wall times are in [release evidence](release.md) and the two performance documents. No device/emulator/perf tier was run by the worker; no Play key was created. The pre-existing pixel emulator is left running.

Completed host runs: baseline profiles, startup, light/dark/font-1.3 timeline, RN comparison, release-check, corrected minified release-smoke after 40ba540f, throwaway-key signingReport after fbd51e30, the real enableSplit=true failure proof, restored normal size and a standalone make check. Commands/results and build archives are in the performance/release documents. After 4df62c45–27693223, R9 quick meets its median target; the check series stops at the third run's early launch-clock deadline failure, corrected in 89f3dd7f. The post-fix emulator check, complete R9 check median and launch/profile refresh remain open. Physical variants require PERF_CLEAR=1 when clearing data; reference/low-end rows remain open: device only.

| R9 measurement | Runs / median | Budget | Status |
|---|---|---:|---|
| warm make quick, three runs | 81.86 / 44.33 / 43.83 s; median 44.33 s | < 60 s median | PASS, host after 4df62c45–27693223 |
| warm make check, three runs | 245.89 / 284.79 s PASS; third FAIL at LaunchHandOffTest | < 300 s median | Incomplete; no three-run median |
| slowest ten quick cases | Saved last host quick report under build/phase13-host-evidence/host-last-quick/timings.json; table below | warm cases < 1 s | PASS; cold/class allowances recorded separately |

The R9 invocation took 22 min 35 s; warm-up quick and check passed. It exited before rewriting build/tier-timings.json, which still dates to 2026-11-16. Do not use that old JSON as Phase 13 evidence or hand-write a replacement. The original logs, two tier time files and unchanged old JSON are archived under build/phase13-host-evidence/. The earlier worker run below is historical and does not replace the host series.

Verified tooling outcomes:

- Gradle provider-based upload signing builds both signed and unsigned bundles; the throwaway key is deleted. Configuration remains cache-compatible.
- AGP writes SDK/version fields in the merged manifest and writes all four R8 diagnostic files; the analyzer exists. The final release/benchmarkRelease directive comparison is empty after removing unused coroutine-test runtime dependencies from perf.
- Bundletool's normal APK-set entries are toc.pb, base-arm64_v8a, base-master and base-xxhdpi; there is no language split. The real split-enabled proof rejects `splits/base-en.apk`; the plugin is restored and normal size passes at 4,156,988 B.
- Apkanalyzer's mapped defined-class output works. Dexdump emits modified UTF-8, handled with byte-preserving decoding; planted checks reject seed markers despite those bytes.
- Robolectric 4.18's supported exit-info builder is nested in ShadowActivityManager; the old four-argument helper is deprecated. Three logger cases pass without booting the app graph.
- Bash 3 under nounset rejects an empty optional array expansion; the baseline runner now expands a non-empty full argument array. Profile-specific fixtures pass.
- The Kotlin daemon cannot write its timestamp marker outside the sandbox's writable roots. In-process compilation avoids the path. Two release builds reached three minutes without R8 output in a reused daemon; each was stopped once and its one retry passed. Fresh single-use release builds completed without the stall. The exact logs and outputs are in release.md; no hang was hidden by a larger cap.
- The host normal-harness RN comparison and profile collection pass. The optional non-profileable and am-start fallbacks remain unexercised on a real device; named fallbacks preserve missing-evidence failures.


Earlier worker validation, 2026-11-22, source 17527208 plus documentation, local macOS host:

| Check | Result | Wall time |
|---|---|---:|
| Single `make quick` | All test stages PASS; whole-command duration exceeds the 60 s warm target | 102.34 s |
| quick JVM/Robolectric tier | 6,304 cases; zero test-budget overruns/hangs | 61.75 s |
| build-logic tier | 58 cases; zero test-budget overruns | 7.08 s |
| Serial timing tier | 7 cases; zero test-budget overruns | 4.09 s |
| Python tooling | Scripts 256, contracts 26, l10n 49, icons 11; all PASS | 28.636 / 0.115 / 0.017 / 0.090 s unittest times |
| `:app:lintDebug` | PASS, including dependency-module lint | 66.25 s |
| App layout tier | 76 cases; zero test-budget overruns | 13.21 s wrapper / 13.16 s tier |
| Final `make release-check` | 293 release cases + manifest/APK/size PASS | 60.19 s |
| Final `make format` | PASS; no tracked source changes | 232.05 s |

Quick was run exactly once. Compilation and cold workers were present in this run; only the host's measured warm medians can establish R9. The run is retained as above-target evidence, without rerunning, loosening a budget or claiming a warm pass. Raw reports remain under `build/test-results/`; command log `/tmp/cf13-final-quick.log`. Formatting printed ten existing unresolved-auto-fix warnings and completed successfully. Two `./gradlew help --configuration-cache` runs confirmed `Reusing configuration cache.` on the second run.

Earlier worker slowest ten **warm** cases from `build/test-results/quick/timings.json` (all ≤ 1,000 ms):

| Class.method | ms |
|---|---:|
| `TestHangWatchdogTest.aDebuggerLeavesTheWatchdogDisarmed` | 869 |
| `MockSeedRefreshTests.restoresTenDayOldSeedWithoutLosingUserStateCurrentAnchor` | 672 |
| `TestHangWatchdogTest.eachRobolectricSdkGetsItsOwnColdAllowance` | 640 |
| `DecodeRecoveryTests.badCardIsSkippedWhileOtherTripsKeepUpdating` | 585 |
| `LargeTimelineFixtureTests.templatesAreUnchanged` | 576 |
| `DataSourceParityTests.invalidSettingsReturnEveryFieldReminders` | 565 |
| `DeleteAccountSheetTest.sheetSurvivesRecreationWithoutThePassword` | 551 |
| `AccountStorageLifecycleTests.failedCreationLeavesNoPartialTable` | 537 |
| `AccountStorageLifecycleTests.corruptFileIsKeptAndReportedUnavailable` | 536 |
| `HapticsRulesTest.noHapticsInLaunch` | 528 |

Cold Robolectric initialization is recorded separately by the timing harness. The ten largest first-SDK cases in the same run were 6,713 / 6,547 / 6,454 / 6,360 / 6,039 / 5,993 / 5,750 / 5,673 / 5,417 / 4,863 ms (SDK 36, each below its 20,000 ms cold allowance). These are not warm-case measurements. Per-class explicit warm-ups are in the same report's `warmups` array; no allowance or watchdog cap changed.


Host evidence after 4df62c45–27693223 (2026-11-22): build-logic limits concurrent test tasks to four worker slots on this 14-logical-core host and one fork per task. The listener still counts the entire test lifecycle; warm-up labels deduct no time and the regression guard rejects slow bodies even when a warm-up is recorded. Terms/CfBanner use the lighter direct-activity fixture and explicit recorded native-text warm-up. GuestContinuationRepositoryTest uses the smallest settings snapshot that proves the write. No budget, allowance or tier was changed. Local tuning evidence: `build/phase13-test-tuning/evidence.json`. Host R9 evidence: `build/phase13-host-evidence/t13-tiers4.log` and the archived tier time files.

With a quiet host, warm daemon and the quick JVM/Robolectric test tasks forced to rerun, default heaps measured **45.5 / 45.0 / 44.3 / 44.5 s**, versus **44.7 s** on the first run after the configuration change and **42.5 / 42.3 s** with **3 GB per Test task**, now applied including `timingTest`. TermsScreenTest's first case improved from **1029–1171 to 1009–1055 ms**, and CfBannerTest's from **900–965 to 885–928 ms**, with no test outcome change; forks, workers, budgets and hang caps remain unchanged.

Slowest ten warm cases in the saved last host quick report, before this task (cold SDK/class allowances excluded; reused unchanged results retain their original measured provenance):

| Class.method | ms |
|---|---:|
| `AccountStorageLifecycleTests.failedCreationLeavesNoPartialTable` | 533 |
| `AccountStorageLifecycleTests.corruptFileIsKeptAndReportedUnavailable` | 526 |
| `ResetRecreationTest.localeRecreationDropsPasswordsKeepsTheLinkAndSavesNoSecrets` | 359 |
| `WizardAnswerSheetTest.busyPickingUsesTheFourDialogsAndWritesOnlyAtTheEnd` | 353 |
| `TripPageTest.menuJumpReachesTheEnd` | 350 |
| `DeleteAccountSheetTest.sheetSurvivesRecreationWithoutThePassword` | 338 |
| `BodyClockScreenTest.openTimeSheetUsesTheSharedPickerWindow` | 263 |
| `TwoFactorRoutingTest.systemBackDiscardsChallenge` | 236 |
| `ForgotRecreationTest.recreationKeepsTheFormEmailAndProcessModel` | 229 |
| `SessionActivationTest.backgroundCancelsAFlightAndForegroundReenablesAndResumes` | 217 |

The check failure is distinct from the earlier worker-contention failures: LaunchHandOffTest measures a continuous `System.nanoTime` duration, and the seeded readiness clock's single Android Handler delay could resume before its nanosecond deadline. 89f3dd7f rechecks that deadline without changing the assertion window. Its JVM regression reproduces 3499.573168 ms before the fix and passes afterwards; the complete host check series must still be rerun. Exact commands are in release.md.


Post-clock-fix worker validation (2026-11-22, 89f3dd7f, macOS host; JDK 17 Gradle / JDK 21 tests): the single `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make quick` invocation passes in **92.51 s command wall**. Its quick tier takes **54.28 s**, reports 6,312 cases (4,402 freshly measured; the remaining unchanged outputs certified), with zero budget overruns/errors/hangs. Build-logic's 64 cases and the serial timing tier's seven cases pass using certified unchanged results. This single command duration exceeds the warm target; it does not replace the host's three-run quick median (44.33 s) or complete R9's failed check series. No second quick run was made. Exact command, log, XML and timings: `build/phase13-host-evidence/launch-clock/quick-command.json`, `quick.log` and its `quick/`, `build-logic/`, `timing/` directories. The old build/tier-timings.json remains byte-for-byte unchanged.
