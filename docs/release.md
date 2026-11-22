# Release build

## Upload key

Enrol in Play App Signing; Google holds the app signing key. Create an upload key outside the repository:

```sh
keytool -genkeypair -v -storetype PKCS12 -keystore ~/keys/chronoflow-upload.p12 -alias upload \
  -keyalg RSA -keysize 4096 -validity 9125 -dname "CN=ChronoFlow Upload"
```

Put these in `~/.gradle/gradle.properties` (or environment variables with the same names). Never commit values:

```properties
CF_UPLOAD_STORE_FILE=/absolute/path/to/keys/chronoflow-upload.p12
CF_UPLOAD_STORE_PASSWORD=<store password>
CF_UPLOAD_KEY_ALIAS=upload
CF_UPLOAD_KEY_PASSWORD=<key password>
```

Gradle properties take precedence over environment variables. No values means unsigned release; partial values fail naming missing keys. The canonical store path must be absolute and outside the repository, including through symlinks; `~/` expands against the user's home. Only release takes this signing config; debug and perf retain debug signing. The signing value object deliberately has no generated `toString` so credentials cannot appear through diagnostic interpolation.

Phase 13 review corrected 5b4cd017: Baseline Profile 1.5.0 clones release in its finalizeDsl callback, copying the eagerly assigned upload config into both perf build types. The convention plugin now resets nonMinifiedRelease and benchmarkRelease to the debug config in a later finalizeDsl callback. An independent onVariants invariant checks their finalized config names on every app configuration, after all DSL callbacks; this catches an ordering regression without reading or printing signing credentials.

Regression evidence (2026-11-22, worker/JDK 17, no device): with a temporary external fixture file and complete synthetic CF_UPLOAD_* environment values, `./gradlew help --no-configuration-cache -Pkotlin.compiler.execution.strategy=in-process` fails before the reset with `nonMinifiedRelease must use debug signing` (5.82 s), then passes with it (7.57 s). The temporary file is empty: this proves the real plugin lifecycle/configuration, not keystore validity or a signed bundle. Five PerfSigningTest cases and nine existing UploadSigningTest cases pass in 3.60 s command wall. The host independently confirmed `./gradlew :app:signingReport` with a throwaway key after fbd51e30: debug → debug, release → upload, nonMinifiedRelease → debug and benchmarkRelease → debug. This is a host-reported signing check; no signing credentials or private key are archived. Existing signed/unsigned bundle evidence below remains valid.

Print the upload certificate's SHA-256:

```sh
keytool -list -v -keystore ~/keys/chronoflow-upload.p12 -alias upload
```

Use the Play app-signing certificate for production asset links and passkey origins. Never the debug certificate.

## Verification

Worker, 2026-11-22, local macOS host, JDK 17 / JDK 21 test launcher. No device was used. Task 1 checks:

| Command | Result | Wall time |
|---|---|---:|
| `./gradlew :build-logic:convention:test --tests '*UploadSigningTest'` | 9 cases, zero failures; XML suite 0.009 s | 16.65 s |
| Same tests after formatting, with `-Pkotlin.compiler.execution.strategy=in-process` | PASS | 3.64 s |
| `python3 -I -m unittest discover -s Scripts/tests -p test_repo_keys.py` | 4 cases PASS, including planted key and property violations | 0.07 s |
| `make format` | PASS after replacing the new test's wildcard import | 55.06 s |
| Signed `:app:bundleRelease` below | `BUILD SUCCESSFUL` | 97.01 s |

The throwaway key used `CN=ChronoFlow Test Upload`, PKCS12 / RSA 4096, outside the repository at `$TMPDIR/cf-upload-test/upload.p12`. Password arguments are redacted here; no production key was created.

```sh
./gradlew :app:bundleRelease \
  -PCF_UPLOAD_STORE_FILE="$TMPDIR/cf-upload-test/upload.p12" \
  -PCF_UPLOAD_STORE_PASSWORD=<throwaway-password> \
  -PCF_UPLOAD_KEY_ALIAS=upload -PCF_UPLOAD_KEY_PASSWORD=<throwaway-password>
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

Verifier output includes `X.509, CN=ChronoFlow Test Upload`. The build output contains no throwaway password. Raw local logs: `/tmp/cf13-task1-signed.log`, `/tmp/cf13-task1-jarsigner-signed.log`.

Unsigned proof: `./gradlew :app:bundleRelease -Pkotlin.compiler.execution.strategy=in-process` without signing properties completed successfully in **43.63 s wall**. `jarsigner -verify app/build/outputs/bundle/release/app-release.aab` printed:

```text
no manifest.

jar is unsigned.
```

The first unsigned attempt stopped after three minutes without R8 output (386.80 s total), using the required one `./gradlew --stop` (`2 Daemons stopped`, 0.30 s). The one retry above passed. The verifier run after the stopped attempt inspected the previous signed artifact and is not unsigned evidence. Logs: `/tmp/cf13-task1-unsigned.log`, `/tmp/cf13-task1-stop.log`, `/tmp/cf13-task1-unsigned-retry.log`, `/tmp/cf13-task1-jarsigner-unsigned-retry.log`. The throwaway key and its directory have been deleted.

Tooling: the Kotlin daemon cannot write its timestamp marker under the sandbox's read-only `Library/Application Support/kotlin/daemon`; Gradle's compiler fallback completed the builds/tests. In-process compilation avoids that daemon path for the final resolver test run.

## Versioning

`version.properties` holds `versionMajor`, `versionMinor`, `versionPatch`, `versionBuild`, in that order. `versionName = major.minor.patch`; `versionCode = major*1_000_000 + minor*10_000 + patch*100 + build`. Major is 0–2099; the other parts are 0–99. Every upload bumps at least build. The first upload remains 0.1.0 (10000).

| `make version-bump PART=…` | Change | At the limit |
|---|---|---|
| `build` | Increment build | `versionBuild is 99: bump patch` |
| `patch` | Increment patch; reset build | `versionPatch is 99: bump minor` |
| `minor` | Increment minor; reset patch/build | `versionMinor is 99: bump major` |
| `major` | Increment major; reset minor/patch/build | `versionMajor is 2099` |

Preview with `python3 -I Scripts/bump-version.py build --dry-run`. Commit uploads as `chore(release): bump version to <name> (<code>)`. This phase does not bump the version.

Version script verification: `python3 -I -m unittest discover -s Scripts/tests -p test_bump_version.py`, 16 cases PASS, 0.52 s wall on the local host (2026-11-22). Temporary files only; `version.properties` is unchanged.

## R8 keep rules

App-owned rules, reviewed 2026-11-22:

| File | Rule | Why |
|---|---|---|
| `app/src/main/keepRules/app.keep` | `-assumenosideeffects class android.util.Log` for `v(...)`, `d(...)`, `i(...)` only | Strip low-priority app logging. Warnings/errors retain their side effects. |
| `app/src/main/keepRules/credentials.keep` | `-if class androidx.credentials.CredentialManager` / `-keep class androidx.credentials.playservices.** { *; }` | Credential Manager discovers its Play services provider by reflection (5b); conditional and restricted to that provider package. |
| `app/src/nonMinifiedRelease/keepRules/profile.keep` | Exactly `-dontobfuscate` and `-dontoptimize` | Profile collection needs real names and uninlined methods. This exception belongs only to nonMinifiedRelease (OQ-A595). |

`KeepRulesTest` scans main and every build-type keep directory except the profile exception, pins the known files, rejects optimization escapes and broad app keeps, and rejects three planted violations. Its keep files are declared Konsist task inputs, so a keep-only change invalidates the test. `make targeted ONLY='konsist:KeepRulesTest'`: 8 cases PASS, 6.67 s wall.

`./gradlew :app:assembleRelease :app:assembleBenchmarkRelease :app:analyzeReleaseR8Config -Pkotlin.compiler.execution.strategy=in-process`: PASS, 42.81 s wall. AGP 9.5.1 writes `configuration.txt`, `mapping.txt`, `seeds.txt`, `usage.txt` under `app/build/outputs/mapping/<variant>/`. No `-printconfiguration` addition is needed. Analyzer output: `R8 Keep Rules analysis report generated: build/reports/r8/r8-config-analyzer-release.html` (module-relative path).

The release configuration contains **57 library consumer rule file sections**. None keeps a whole package with all members; the only such package keep is our existing conditional Credential Manager provider rule above. Navigation 3 routes and sheets are `@Serializable`; the app owns its back stacks and uses `NavDisplay`. The requested `rememberNavBackStack|SavedStateConfiguration|serializer<|Json.encodeToString` search found no reflective route/sheet serializer lookup in app/feature production code. Wizard recovery uses generated kotlinx.serialization serializers; bundled companion `serializer()` consumer rules cover these. No route keep was added. The minified smoke is the runtime proof: host `make release-smoke` passes all three cases after 40ba540f (results below).

Initial sorted-directive diff found four extra entries in benchmarkRelease from `kotlinx-coroutines-test-jvm`, transitively through the fixture module: duplicate volatile-field keeps and dispatcher-factory name keeps. Perf code imports only `LargeTimelineFixtures`. Dependency insight also found a transitive fixture path through `:core:data-mock`; excluding only the direct edge left the diff unchanged. The final exclusion is on the two perf runtime classpaths, covering both paths and removing the unused dispatcher provider; debug/tests are unaffected. No app rule was changed. A first build-script attempt used Gradle's map overload (returns `Dependency`); the corrected string overload returns `ProjectDependency` and supports exclusions.

Final `:app:assembleBenchmarkRelease -Pkotlin.compiler.execution.strategy=in-process`: PASS, 28.96 s wall. The exact comparison below exits 0 with **no output**:

```sh
diff <(rg '^-' app/build/outputs/mapping/release/configuration.txt | sort) \
     <(rg '^-' app/build/outputs/mapping/benchmarkRelease/configuration.txt | sort)
```

The release/benchmarkRelease app keep and Log-stripping rules are identical. Only nonMinifiedRelease has the pinned profile exception. The host runtime smoke and c8e7cfb3 regenerated-profile proofs pass. The subsequent clock fix 89f3dd7f requires the launch/profile refresh listed below.

## Release manifest

`./gradlew :build-logic:convention:test --tests '*VerifyReleaseManifestTest' --tests '*AppLinkManifestRulesTest' :app:verifyReleaseManifest -Pkotlin.compiler.execution.strategy=in-process`: PASS, **13.20 s wall**, local host 2026-11-22. All 21 verifier cases and nine unchanged App Link cases pass. The report is `app/build/reports/verifyReleaseManifest.txt` (`OK`). Each planted mutation retains MainActivity's verified HTTPS auth-link filter.

AGP 9.5.1 includes SDK and version fields in `app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`: minSdk 29, targetSdk 36, versionCode 10000, versionName 0.1.0. The verifier's cache inputs carry all four values and the required-permission set. No APK badging fallback is needed.

Merged permission lines:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="net.chronoflow.app.native.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
```

Every exported component:

```text
activity: net.chronoflow.app.MainActivity
receiver: androidx.work.impl.diagnostics.DiagnosticsReceiver (permission android.permission.DUMP)
receiver: androidx.profileinstaller.ProfileInstallReceiver (permission android.permission.DUMP)
```

The named `EXPORTED` set contains MainActivity; the additional exported-component set stays empty. The only guard permission is `DUMP`. `SystemJobService` and both reminder receivers remain unexported; the planted exported job service fails `EXPORTED_COMPONENT`.

InitializationProvider's complete metadata:

```text
androidx.lifecycle.ProcessLifecycleInitializer
androidx.profileinstaller.ProfileInstallerInitializer
```

The app manifest removes permissions `FOREGROUND_SERVICE`, `USE_BIOMETRIC`, `USE_FINGERPRINT`; services `com.google.android.gms.auth.api.signin.RevocationBoundService` and `androidx.room.MultiInstanceInvalidationService`; initializers `androidx.work.WorkManagerInitializer`, `androidx.emoji2.text.EmojiCompatInitializer`, `okhttp3.internal.platform.PlatformInitializer`. Missing optional classes are removed with `tools:ignore="MissingClass"`. WorkManager initializes on demand through ChronoFlowApp's Configuration.Provider.

## Size

Task 5 adds a language-split check after bundletool builds the device-specific APK set. It records every entry at `app/build/outputs/size/apks-entries.txt`, then rejects `splits/base-<language>[_<REGION>].apk`. The existing native-library allow-list and 8/12 MB verdicts are unchanged. The previous recorded size (5b) was **2,445,635 B**; Phase 13's measurement follows below.

`./gradlew :build-logic:convention:test --tests '*BundleSizeTest' -Pkotlin.compiler.execution.strategy=in-process`: **11 cases PASS**, 10.71 s wall, local host 2026-11-22. English, Dutch and pt_BR splits are rejected; ABI, density and master APKs are controls.

Host negative proof completed (R17): temporarily setting `bundle.language.enableSplit = true` makes `make size` fail with `language split present: splits/base-en.apk`. The convention plugin was restored to false, and normal `make size` passes at **4,156,988 B (4.16 MB)**. The same size passes in host release-check. Logs: `build/phase13-host-evidence/t13-split.log`, `t13-size.log` and `t13-release-check.log`. The normal gate finds no new native library or language split; the Release build plan box is complete.

Earlier worker result, local host, 2026-11-22, release bundle, bundletool device spec arm64-v8a/xxhdpi/en/API 36:

```text
Bundle size: 4147965 bytes (4.15 MB), arm64-v8a/xxhdpi/en: OK
```

Command: `make size GRADLE='./gradlew --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process'`, **40.72 s wall**, PASS. This is a bundletool download-size estimate, not an emulator or physical-device performance claim. No unapproved `.so` and no language split were found. The first size build reached the three-minute no-output R8 limit and was stopped once (`1 Daemon stopped`); the one retry above passed with a fresh single-use daemon. The later host proof above closes the split-enabled requirement without changing the checker or budget.

## Release content

`make release-check GRADLE='./gradlew --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process'`: **PASS, 42.36 s wall**, worker/local host, 2026-11-22. Release unit XML contains **291 cases, zero failures/errors**. Manifest verification passes. The APK checker reports:

```text
INFO billing: absent
release apk: OK (2 dex files, 7747 classes checked)
Bundle size: 4147966 bytes (4.15 MB), arm64-v8a/xxhdpi/en: OK
```

`Scripts/check-release-apk.py` uses apkanalyzer's defined classes with R8 mappings, then scans every dex disassembly for forbidden original class prefixes, seed strings and app-owned Log.v/d/i or exit-history calls. It permits only perf stub declarations read from the releaseNoPerf directory, not the whole package. A forbidden string reports its owning class without echoing its value. Billing is informational. The Make target uses AGP's current output metadata to select the signed or unsigned APK, avoiding a stale artifact left by another signing mode. This gate is separate from `make check`.

Fixture command `python3 -I -m unittest discover -s Scripts/tests -p test_check_release_apk.py`: **20 PASS**, 5.08 s wall initially; **2.18 s wall** after making all string findings omit their values. Each fixture case remains below 1 s. Controls cover preview annotations, the declared stub, library Log.d and app Log.w; planted failures cover both debug-package forms, new CF_ seeds, undeclared perf code, forbidden calls, missing dex evidence and a missing tool.

Tooling: installed apkanalyzer accepts `dex packages --defined-only --proguard-mappings`; installed dexdump emits modified UTF-8 (an overlong NUL in this real APK raised UnicodeDecodeError in the initial inspection). The checker streams with byte-preserving backslash replacement, retaining ASCII forbidden markers; a planted modified-UTF-8 fixture proves the markers still fail. No release content finding required a source move or keep-rule change.

## Debug exit history

The debug-only `ExitInfoLogger` reads the previous `ApplicationExitInfo` on API 30+ after first commit, on AppGraph's injected IO dispatcher. It logs the specified metadata only, never trace input or process state summaries. The existing first-commit guard keeps the hook once per process. Release and perf use the no-op LaunchPlatform hook; Application.onCreate is unchanged.

`env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make targeted ONLY='app:ExitInfoLoggerTest app:ReleaseReadinessTest'`: **PASS, 15.75 s wall**, local host 2026-11-22. Three Robolectric cases: previous crash 0.745 s (first SDK 30 case), no exit 0.011 s, SDK 29 no-service guard 0.413 s (first SDK 29 case). All 15 release-readiness cases pass (0.080 s suite); the logger class is absent. No graph, activity or app root is booted by the new test.

Robolectric 4.18 has `ShadowActivityManager.addApplicationExitInfo(Object)` and nested `ApplicationExitInfoBuilder`. The four-argument helper is deprecated and failed the first compile under warnings-as-errors; the builder overload fixes it without suppression. Logs: `/tmp/cf13-task7-tests.log` (compile failure), `/tmp/cf13-task7-tests2.log` (pass). Startup/profile host proofs remain `make baseline-profile` and `make perf`.

## Perf journey entries

`cf.perf-account=demo` seeds only an absent perf mock state; explicit GraphOverrides remain authoritative. `cf.perf-wizard=WizardSaveJourney` installs the real-store, real-clock 9g journey through the existing WizardStartOverride seam, once per process on shell entry. Debug wizard seeds remain authoritative. MainActivity adds only the two intent-hook calls (and their import); all implementation lives in perf sources. Release has a fifth no-op declaration, PerfJourneyHook, following the existing four-stub pattern. The APK gate derives that exact declaration set; no perf package is allow-listed.

The initial spike found one additional brief mismatch: each 9g capture creates a random `WizardLeg.id`, so separately generated drafts cannot be raw-equal. The perf copy preserves this behavior and the equality guard aligns only that identity, verifies UUID validity, then compares the full draft. Route, all airports, UTC times, profile, revision, recovery and step fields compare exactly. The correction is recorded in the brief; no 9g source or test changed.

`env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make targeted ONLY='app:PerfJourneyExtrasTest app:PerfWizardJourneyTest app:PerfMockSourcesSeedTest feature:wizard:WizardSaveJourneySeedTest app:ReleaseReadinessTest'`: **PASS, 16.88 s wall**, local host 2026-11-22. The nine new JVM cases total 0.283 s (extras 0.007 s; draft 0.008 s; mock source 0.268 s). The mock-seed class warms its fixture once via measureClassWarmup. All 16 release-readiness cases and the unchanged 9g journey seed test pass.

Runner fixture commands (fake adb/Gradle/checker only):

- `python3 -I -m unittest discover -s Scripts/tests -p test_perf_startup.py`: 11 PASS, **2.34 s wall**.
- `python3 -I -m unittest discover -s Scripts/tests -p test_perf_timeline.py`: 19 PASS, **4.54 s wall**.

All three runners clear the native perf package exactly once after successful perf preparation and before Gradle. The scripts skip their clear on physical devices without PERF_CLEAR=1 and report `state: as found`. Dry runs print the clear without executing it. No device/perf run was performed by the worker.

## Minified smoke

Host command: `make release-smoke` (device lock, pixel emulator/API 36, minified benchmarkRelease). The rerun after 40ba540f passes all three cases, **80.00 s tier wall**, with same-process and crash-buffer assertions intact. Archived evidence: `build/phase13-host-evidence/t13-release-smoke.log` and `build/phase13-host-evidence/release-smoke/` (timings and connected benchmarkRelease XML). The earlier failed run is retained as `build/phase13-host-evidence/t13-smoke.log`; it is superseded by this rerun, not treated as a pass.

The class signs in through the real mock-backed form once in BeforeClass, then starts a fresh process per method while preserving the account/session file. This makes the once-per-process wizard hook deterministic without another sign-in journey. The sheet case waits for an app window event from the font-scale recreation, checks the restored `step.sheet` and unchanged pid, dismisses the sheet, waits for it to disappear, then checks `trip.page` and the pid again. It restores the old setting in finally. Every method compares pids and checks the crash buffer. The first-trip reminder guard does not wait and runs only after the wizard save here. All queries use structural tags; no sleeps or copy queries.

| Host measurement | Budget | Result |
|---|---:|---|
| signInOnce setup | ≤ 20 s | Setup completed; not separately timed in XML |
| todayTripsAndSettingsOpen | < 10 s | Host rerun PASS, 3.381 s |
| tripSheetSurvivesRecreation | < 10 s | Host rerun PASS, 8.606 s |
| wizardSavesToTripPage | < 10 s | Host rerun PASS, 6.778 s |

Review correction belongs to 0feca5ee. `TripPageSheets` uses `CfModalBottomSheet`, whose Material modal owns a separate window. The old test found the restored sheet but queried the hidden activity window next; it failed before reaching its pid assertion. No crash was recorded, and that failure alone did not prove the underlying page was lost. The shared test-only `verifyRecreatedSheet` flow now asserts the process both while the sheet is visible and after revealing the page. Its JVM active-window fixture fails with the old ordering, then all three cases pass with the fix (0.013 s suite); missing-page and process-death controls still fail as required. `:benchmark:assembleBenchmarkRelease` plus root Spotless passes in 59.38 s command wall. No product source, timeout or budget changes; the passing rerun above supplies the emulator proof. Physical-device evidence remains separate.

The runner refuses physical-device clearing without `PERF_CLEAR=1`; shared in-test clearing independently requires an emulator or the runner's explicit `cf.perfClear=1` argument. It uses functional preparation, timeout_msec=60000 and no benchmark rule/error suppression. It is separate from `make check`.

Worker validation, local host 2026-11-22:

- `python3 -I -m unittest discover -s Scripts/tests -p test_release_smoke.py`: **4 PASS, 0.74 s wall** (dry-run class/cap selection, Gradle status, physical refusal and opt-in).
- `python3 -I -m unittest discover -s Scripts/tests -p test_makefile.py`: **2 PASS, 0.06 s wall**.
- `./gradlew :benchmark:assembleBenchmarkRelease -Pkotlin.compiler.execution.strategy=in-process`: **PASS, 10.10 s wall**.
- `./gradlew --no-daemon :app:assembleBenchmarkRelease :benchmark:assembleBenchmarkRelease -Pkotlin.compiler.execution.strategy=in-process`: **PASS, 56.30 s wall**. Both target and test APKs compile. The later passing host smoke above provides the runtime keep-rule proof.

## Completed profile proof

Host `make baseline-profile` passed in 10 min 34 s (bp-13b.log); c8e7cfb3 commits both genuine generated profiles and the strict coverage guard. The subsequent native startup/timeline runs at c8e7cfb3 use Partial(Require) successfully. This completes the original collection; the Profiles box is reopened for the required refresh after launch-clock fix 89f3dd7f. See [host profile evidence](perf-startup.md#phase-13-profile-preparation). Performance budget remains open for the unmeasured/failed host gates, without a waiver.

## RN comparison tooling

`make perf-rn` takes the host-wide device lock, checks the prebuilt APK hash before installation, uses shared device/archive helpers, checks animator scales, and restores functional preparation even on a Gradle/checker failure. The checker still runs after a failed Gradle command so missing evidence is explicit. Physical clearing has both runner and instrumentation guards. The optional NOT-PROFILEABLE suppression and am-start fallback require named opt-ins and are recorded in the [comparison protocol](perf-startup.md#rn-comparison-protocol). The host normal-harness RN comparison now passes; all emulator values remain REPORT, as recorded in perf-startup.md.

## 16 KB alignment

Worker, local host, 2026-11-22: `/opt/homebrew/share/android-commandlinetools/build-tools/36.0.0/zipalign -c -P 16 -v 4 app/build/outputs/apk/release/app-release-unsigned.apk` exits 0 and prints `Verification successful`. This checks the existing release APK's native-library packaging, including the sole allowed graphics-path library. Raw local log: `/tmp/cf13-task13-zipalign.log`. Recheck the final APK after any packaging/native dependency change.

Final version-parser review found Python int() accepts underscores and Unicode digits that VersionProperties.parse rejects. The script now accepts only the same signed ASCII integer syntax before applying the existing ranges. `python3 -I -m unittest discover -s Scripts/tests -p test_bump_version.py`: **18 PASS, 0.49 s wall** (2026-11-22), including both planted mismatches. This corrects Task 2 commit 6cca81c7; valid versions and version.properties are unchanged.

## Tooling notes

Earlier worker source: 17527208 plus documentation, 2026-11-22, local macOS host. Gradle uses JDK 17; JVM/Robolectric tests use JDK 21. No device/emulator/performance tier was run. The throwaway signing key is deleted; no production key or Play upload was created.

| Exact command | Result | Wall time |
|---|---|---:|
| `make release-check GRADLE='./gradlew --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process'` | 293 release cases, manifest, every-dex scan and size PASS | 60.19 s |
| `./gradlew :app:lintDebug -Pkotlin.compiler.execution.strategy=in-process` | PASS, including dependency lint | 66.25 s |
| `Scripts/run-tier.sh layout-app --results 'app/build/test-results/testDebugUnitTest/*.xml' --timings 'app/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :app:testDebugUnitTest -Pcf.tier=layout -Pkotlin.compiler.execution.strategy=in-process` | 76 cases PASS, zero test-budget overruns | 13.21 s wrapper / 13.16 s tier |
| `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make quick` | All test stages PASS; duration above the 60 s warm target | 102.34 s |
| `make format` | PASS; no tracked source changes | 232.05 s |

Final release-check output:

```text
INFO billing: absent
release apk: OK (2 dex files, 7747 classes checked)
Bundle size: 4147961 bytes (4.15 MB), arm64-v8a/xxhdpi/en: OK
```

The normal APK set still has no language split or new native library. Final `zipalign -c -P 16 -v 4` against `app-release-unsigned.apk` exits 0 with `Verification successful` (`/tmp/cf13-final-zipalign.log`). Sorted release and benchmarkRelease R8 directives still have an empty diff. The merged-manifest report is `OK`; its ten original rules, verified auth-link filter and unexported SystemJobService remain enforced. Provider-based signing and unsigned builds are proven above. Running `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' ./gradlew help --configuration-cache` twice prints `Reusing configuration cache.` on the second run.

Quick's 6,304 JVM/Robolectric cases, 58 build-logic cases, seven serial timing cases and 342 Python cases all pass. No individual test-budget violation or hang occurred. The 102.34 s command duration exceeds the warm target; no second quick run was made and R9 remains open. Slowest warm cases and separate cold allowances are in [tooling](tooling.md#phase-13). Logs: `/tmp/cf13-final-release-check.log`, `/tmp/cf13-final-lint.log`, `/tmp/cf13-final-layout.log`, `/tmp/cf13-final-format.log`, `/tmp/cf13-final-quick.log`.

Host follow-up, 2026-11-22: release-check passes with `release apk: OK (2 dex files, 7768 classes checked)` and `Bundle size: 4156988 bytes (4.16 MB), arm64-v8a/xxhdpi/en: OK`. The corrected minified smoke passes as above; signingReport and the real split-enabled failure proof are complete. A standalone make check passes. The subsequent `Scripts/measure-tiers.sh` run after 4df62c45–27693223 takes **22 min 35 s**: warm-up quick/check pass; quick runs **81.86 / 44.33 / 43.83 s**, median **44.33 s ≤ 60**; check runs **245.89 / 284.79 s** pass, but the third check fails in LaunchHandOffTest. There is **no three-run check median**. The script exits before writing `build/tier-timings.json`, which remains the **2026-11-16** artifact; it has not been edited by hand. Archived evidence: `build/phase13-host-evidence/t13-tiers4.log`, `prior-tier-quick.times`, `prior-tier-check.times`, `prior-tier-timings.json` and `smoke/` (the failed third check's XML).

The failure is a product clock deadline defect, corrected in 89f3dd7f. `MediaLaunchClock.sleepUntil` now rechecks monotonic time after Android's millisecond timer wakes; the assertion window and animation duration are unchanged. The JVM early-wake test fails before the fix and passes afterwards, alongside cancellation and the coordinator/timeline cases. Host verification of that fix remains open; no device/perf command was run by this worker.

Remaining commands under R17, serially with pixel running (the launch path changed):

```sh
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
make targeted ONLY="app:LaunchHandOffTest#slowShowsStatusAndHandsOffAfterReady"
Scripts/measure-tiers.sh
make check
make baseline-profile
make perf
make perf-timeline
THEME=dark make perf-timeline
FONT_SCALE=1.3 make perf-timeline
```

The profile/performance results recorded at c8e7cfb3 remain historical evidence; regenerate/commit profiles after 89f3dd7f and record the new Require results. Do not reapply the old saved guard patch: it is already committed. Release-check and minified smoke passed before the clock fix; repeat `make release-check` and `make release-smoke` for the final upload artifact. No board captures are required (no UI/boards changed).

Physical reference and low-end runs remain open (OQ-A22), and block production under OQ-A45. Only when the host selects/prepares the corresponding device:

```sh
PERF_CLEAR=1 DEVICE_CLASS=reference make perf
PERF_CLEAR=1 DEVICE_CLASS=reference make perf-timeline
PERF_CLEAR=1 DEVICE_CLASS=reference make perf-rn
PERF_CLEAR=1 DEVICE_CLASS=lowEnd make perf
PERF_CLEAR=1 DEVICE_CLASS=lowEnd make perf-timeline
PERF_CLEAR=1 DEVICE_CLASS=lowEnd make perf-rn
```

Actual device metadata, raw metrics and trace paths go into the pending performance tables. Use the explicitly documented RN fallbacks only if their stated tooling condition occurs. No missing metric, repeated hard miss or above-target tier median closes a gate.
