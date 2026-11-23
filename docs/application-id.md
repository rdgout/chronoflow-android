# Final application ID

User, 2026-11-23: RN never shipped on Play. The final applicationId is `net.chronoflow.app`; debug remains `net.chronoflow.app.debug`. Existing namespaces and build-type suffix rules are unchanged. A2, C6 and OQ-A1 record this decision. OQ-A629 preserves StorageNamespace's pinned iOS service/alias names and their existing tests.

## Changed areas

- `app/`: applicationId and debug/release/launch identity assertions.
- `benchmark/`: targetAppId defaults; LaunchComparisonBenchmark requires `cf.compareTarget=Native` or `Rn` and runs only that parameter.
- `build-logic/`: manifest-rule tests and every affected fixture package, permission and provider authority. Production rules already derive the dynamic-receiver permission from `variant.applicationId`; a regression test checks that a different applicationId rejects the fixture permission.
- `Scripts/` and `Makefile`: package references, fixtures and tests; separate comparison installs/instrumentation passes, reports, archives and native restoration. RN SHA verification is retained. See [the runner protocol](perf-rn.md).
- `plan.md`, `AGENTS.md` and `docs/`: decision, trap, commands, App Links association examples and comparison protocol. `CLAUDE.md` contains no literal old package and is unchanged. Research, design, StorageNamespace and its tests are unchanged.

## Validation

Local macOS host, JDK 17 Gradle / JDK 21 test toolchain, 2026-11-23. No emulator was connected or started.

| Command | Result |
|---|---|
| `git -C /Users/rickgout/Sites/chronoflow-android rev-parse --abbrev-ref HEAD` | `chore/application-id` |
| `python3 -I -m unittest discover -s Scripts/tests -p 'test_*.py'` | 359 pass, including separate-pass installation/signature simulation, failure restoration and missing metrics |
| `make targeted ONLY="app:DebugAppIdentityTest app:ReleaseAppIdentityTest build-logic:convention:VerifyReleaseManifestTest build-logic:convention:AppLinkManifestRulesTest app:AppLaunchSmokeTest"` | 33 JVM tests pass; emulator launch test builds but cannot run (output below); command exits 2 |
| `./gradlew --console=plain -Pkotlin.compiler.execution.strategy=in-process :app:verifyReleaseManifest :benchmark:assembleBenchmarkRelease` | Pass; manifest report `OK`; benchmark APK assembled |
| `make quick GRADLE='./gradlew --console=plain -Pkotlin.compiler.execution.strategy=in-process'` | Pass, run once: 4,457 main-tier tests, 65 build-logic tests, host tooling suites (359/26/49/11), timing tier with certified cached output |
| `bash -n Scripts/perf-compare-rn.sh Scripts/perf-archive.sh Scripts/perf-startup.sh Scripts/perf-timeline.sh Scripts/baseline-profile.sh` | Pass |
| `git diff --check` and `git grep -n "app\.native"` audit | No whitespace errors; remaining matches are research/design or intentional storage names/documentation |

The merged release manifest declares and uses `net.chronoflow.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Kotlin daemon startup in the targeted run printed a sandbox file-access error and compilation succeeded using its fallback; subsequent commands explicitly selected in-process compilation.

The unavailable emulator tier reported:

```text
Execution failed for task ':app:connectedDebugAndroidTest' (registered by plugin 'com.android.internal.application').
> com.android.builder.testing.api.DeviceException: No connected devices!
run-tier: emulator tier selected no test cases
make: *** [targeted] Error 1
```

Full command output is in `build/application-id-validation/{targeted,manifest-benchmark,scripts,quick}.log`; tier XML and timing summaries are in `build/test-results/`. No failed test command was retried.

Open with the orchestrator: `make check` on pixel (including AppLaunchSmokeTest), and a fresh `make perf-rn` to prove the new direct-instrumentation install/output protocol on the emulator. No server file was written or deployed; production/local-staging assetlinks publication remains server work. Historical benchmark numbers are unchanged and do not verify this new protocol.
