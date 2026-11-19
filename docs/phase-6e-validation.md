# Phase 6e validation

Branch preflight: `feat/phase-6e-trip-wide`. Upstream names and all P1 metrics are present. The brief refresh records the day-tag owner and the allocation-free px pin adaptation. OQ-A305–OQ-A323 record the nineteen brief defaults; no new copy.

## Task 1

- `make targeted ONLY="core:testing-compose:WindowSizeFakeHostTest core:testing-compose:ProfileHostTest konsist:KonsistRulesTest#featureSizesComeFromTheWindow konsist:KonsistRulesTest#featureSizesComeFromTheWindowRejectsPlantedViolation"`: first run caught two fixture expectations (inherited top inset and fractional-density pixel rounding).
- Focused rerun of both affected methods and the two Konsist methods: PASS (4 helper cases, 2 Konsist methods; Gradle 4 s + 5 s). Helper cases 0.027–1.757 s, Konsist 0.007–0.066 s. Evidence copied to `build/test-results/phase-6e/task1/`.
- Window size uses public Compose `LocalWindowInfo.containerSize` at `CfRoot`, beside the existing inset provider. It shares the shell's window bounds source; `WindowPolicy.wideBreakpointDp` remains the shell policy. The profile helper supplies the same local. No feature configuration-size reads existed.
- Kotlin daemon cannot create its timestamp in the sandbox (`java.nio.file.FileSystemException: /Users/rickgout/Library/Application Support/kotlin/daemon/kotlin-daemon-client-tsmarker….tmp: Operation not permitted`); Gradle's in-process fallback succeeds.
