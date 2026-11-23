# RN launch comparison

`make perf-rn` compares the native benchmarkRelease APK with the existing SHA-verified RN release APK. Since the final applicationId decision (user, 2026-11-23), both apps use `net.chronoflow.app` and have different signing keys. They require separate installation passes.

The runner assembles native and benchmark APKs once, installs the benchmark APK, uninstalls the shared app package, installs native, and instruments with `cf.compareTarget=Native`. It then uninstalls native, installs the verified RN APK, and instruments with `cf.compareTarget=Rn`. The benchmark requires exactly one of these argument values; it never runs both labels against one installation. Direct instrumentation keeps Gradle from reinstalling native during the RN pass.

Each pass retains the existing cold, signed-out, ten-iteration protocol, metrics, compilation mode and 3.5 s capture window described in [startup performance](perf-startup.md#rn-comparison-protocol). SHA verification happens before any build, uninstall or install. No RN build runs. Physical devices require `PERF_CLEAR=1`; the passes uninstall apps and reset their data.

On exit, including a failed pass, the runner uninstalls the current shared package, reinstalls native benchmarkRelease with empty app data, and restores functional animator scales. A restoration failure fails the command and explicitly asks for native reinstallation by the next native tier. A build failure leaves the installed app untouched.

Pass XML reports land in `build/perf-rn/{Native,Rn}.xml` for the tier collector. The archive under `build/perf/compare/` contains those reports, instrumentation output, and separate `connected_android_test_additional_output/benchmarkRelease/{Native,Rn}/` JSON/traces. Missing test success, timing, JSON or required comparison metrics fails. The checker reads both pass directories together and keeps `coldSignedOut[Native]` / `coldSignedOut[Rn]` labels. Existing history rows predate this package change; a fresh host run is required to verify the new installation protocol on the emulator.

`RN_NOT_PROFILEABLE=1` keeps the explicit RN tooling override. `RN_AM_START_FALLBACK=1` gathers RN-only report samples before native is restored; those samples never satisfy the Macrobenchmark comparison gate.
