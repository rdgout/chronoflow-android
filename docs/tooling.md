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
| `lint.xml` ignores | `GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion` (versions are pinned by A1, not by Lint); `MissingApplicationIcon` (0b adds the icon and removes the line, OQ-0a-5). |
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

Other library components: `androidx.profileinstaller.ProfileInstallReceiver` (exported, guarded by `android.permission.DUMP`); `<uses-library>` entries `androidx.window.extensions` and `androidx.window.sidecar`. Nothing was removed (Phase 3a keeps the lifecycle and profile installer entries and drops the rest).

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
