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
