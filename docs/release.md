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

The release configuration contains **57 library consumer rule file sections**. None keeps a whole package with all members; the only such package keep is our existing conditional Credential Manager provider rule above. Navigation 3 routes and sheets are `@Serializable`; the app owns its back stacks and uses `NavDisplay`. The requested `rememberNavBackStack|SavedStateConfiguration|serializer<|Json.encodeToString` search found no reflective route/sheet serializer lookup in app/feature production code. Wizard recovery uses generated kotlinx.serialization serializers; bundled companion `serializer()` consumer rules cover these. No route keep was added. The minified smoke is the runtime proof (host command `make release-smoke`, pending).

Initial sorted-directive diff found four extra entries in benchmarkRelease from `kotlinx-coroutines-test-jvm`, transitively through the fixture module: duplicate volatile-field keeps and dispatcher-factory name keeps. Perf code imports only `LargeTimelineFixtures`. Dependency insight also found a transitive fixture path through `:core:data-mock`; excluding only the direct edge left the diff unchanged. The final exclusion is on the two perf runtime classpaths, covering both paths and removing the unused dispatcher provider; debug/tests are unaffected. No app rule was changed. A first build-script attempt used Gradle's map overload (returns `Dependency`); the corrected string overload returns `ProjectDependency` and supports exclusions.

Final `:app:assembleBenchmarkRelease -Pkotlin.compiler.execution.strategy=in-process`: PASS, 28.96 s wall. The exact comparison below exits 0 with **no output**:

```sh
diff <(rg '^-' app/build/outputs/mapping/release/configuration.txt | sort) \
     <(rg '^-' app/build/outputs/mapping/benchmarkRelease/configuration.txt | sort)
```

The release/benchmarkRelease app keep and Log-stripping rules are identical. Only nonMinifiedRelease has the pinned profile exception. Runtime smoke and regenerated-profile proofs remain host commands.

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
