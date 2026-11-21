# Phase 10c security acceptance — 2026-11-21

Implemented on `feat/phase-10c-settings-security`, from the Phase 10b stack. Security, password change, the shared password prompt, authenticator setup, management and recovery-code export are wired to the session store. The pinned Swift model and tests are ported; secrets remain in memory. No push was performed.

## Decisions and integration

The preflight printed the required branch. The upstream-name audit matched the brief's refresh notes R1–R10; no further brief patch or refresh commit was needed. OQ-A502–A518 record all 17 defaults from §9. No additional question was needed. OQ-A513 explicitly supersedes OQ-A462's planned subtitle switch: Security still says “Password, two-factor”. The release passkey capability remains disabled, and its row is debug-only.

Reused `SettingsPagesModel`, `SettingsMainKey`, `settingsAttempt`, `settingsText`, `SettingsDebugCounters`, `SettingsWideMetrics.COLUMN` and `CfRow`. All three `SettingsDependencies` implementers have the security, prompt-seed and clipboard members. Optional IO/compute creation-extra keys inject the dispatchers. The production security/clipboard dependency seams landed with Task 8 so its intermediate app compiled; route and seed wiring landed in Task 10. The existing `twoFactorSetup` shell mapping and its test were retained.

`CfModalBottomSheet.properties` and `CfAlertDialog.testTag` are optional, additive parameters; existing callers retain their defaults. `BoardCapture` gained a separate window-stack capture helper. Prompt feedback remains in the ViewModel until the snackbar presentation callback acknowledges its revision. Session responses update snapshots under a mutation permit, preserving pending settings; stale-session cancellation is silent.

The model uses an anonymous `SecretField` adapter rather than an extra named `StaticSecret`. Screen metrics reuse generated token values rather than adding empty metric objects. The two ported UI tests retain their actual Swift `test…` method names. No test hook was added to production code. A pre-existing trip test's deprecated Activity cast was changed to `LocalActivity` for lint, with a test-only dependency; its assertions and production trip code are unchanged.

ZXing Core is pinned to **3.5.4**, published **2025-11-11**, with its Apache-2.0 notice in `docs/third-party.md`. Espresso-Intents is catalogued at **3.7.0** (2025-07-30). Neither adds a native library. The new smartphone glyph is generated from `SetSecurity`'s SVG. The only added label is untranslatable `clipboard_label`; visible English/Dutch copy uses generated resources.

## Recorded board differences

These follow the brief defaults and are not new design decisions:

- `PasswordPromptWide`: 540 dp dialog, height at most 90%; the Phase 10c plan entry now agrees. Other settings columns remain unchanged.
- All `PasswordPrompt*`: token title 16/24 with native Cancel; `surface2` field plus the board's 18 dp lock. `PasswordPromptChecking` and `PasswordPromptForgot` use Enable; `PasswordPromptLocked` uses destructive Disable until Delete is implemented in 10d.
- `SetPassword`, `Set2FASetup`, `Set2FAOn`, `Set2FACodes`: iOS action heights (48 dp; setup-key Copy 40 dp) instead of the smaller board buttons. Setup displays/copies the raw key.
- `Set2FAOn`: the shared long snackbar replaces the board toast. There is no added “Copied” message, password-policy hint or authenticator-app link.
- `Set2FAManage` and its wide/NL variants: `CfRow` follows iOS's 64 dp minimum and 14/20 subtitle rather than the board's 60 dp and 13/18. The recovery-code eyebrow gap follows the board's 8 dp rather than iOS's 12 dp.
- `PasswordPromptNL`, `PasswordPromptForgot` and `Set2FANL`: generated Dutch catalog wording wins over the board's reset/sign-in wording. Security's home subtitle remains “Password, two-factor”; release omits Passkeys.

The 24 requested captures exist under `build/captures/`. Host side-by-side comparison with the boards remains a reviewer check; Chromium was not launched in the sandbox.

## Secret and threading audit

Manually inspected every new secret-holding production file, including `SettingsSecurityModel`, `SecurityViewModel`, `PasswordInputs`, `TwoFactorInputs`, `TextFieldSecret`, the prompt's `BasicSecureTextField`, setup/code controls, QR generation, clipboard and document export. This includes files beyond Konsist's `CfSecureTextField` detection. None uses `rememberSaveable`, `rememberTextFieldState`, `SavedStateHandle`, a secret route key, logging or secret-bearing exception text. State/event/input `toString()` implementations redact; `QrMatrix` reports only its size. Setup and recovery codes are retained in the entry ViewModel across configuration, while typed passwords and verification input are not saved.

The recreation tests inspect parcelled saved state for UTF-8 and UTF-16 secret needles and verify empty password input after recreation. `SecureWindow` is reference counted and tested; device checks prove the flag on setup/codes and its absence on management without codes. Clipboard tests cover sensitive metadata, ownership, the 60-second clear and deferred foreground clear. The process timer retains only the ownership stamp. Disk/Keystore/network work uses injected IO; QR preparation uses compute. Download bytes are UTF-8, LF, without BOM or trailing newline. No secret is written to app storage by the production export path. Debug seed passwords remain debug-only.

## Validation

Commands used JDK 17 and `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`. Gradle execution used `-Pkotlin.compiler.execution.strategy=in-process` (or `ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process` for Make) because the sandbox cannot create Kotlin daemon marker files outside its writable roots. Test toolchains remain JDK 21. Full command/output logs are retained locally in `build/test-results/10c-validation-logs/`; timing JSON and JUnit evidence are under `build/test-results/`.

Targeted checks were run for each task. The following lists final results; scoped fix reruns are included in the logs.

| Check / command | Result and wall time |
|---|---|
| Dependency resolution; `make vectors-check` | ZXing 3.5.4 resolved (21 s); 11 generator tests passed (0.087 s). |
| `make targeted ONLY="core:ui:SecureWindowTest"` | 4 passed; Gradle 28 s, first cold case 2.273 s, remaining 24–31 ms. |
| `make targeted ONLY="core:data:AccountSecurityStoreTests"` | 3 passed; 5 s, warm-up 232 ms and cases 10–23 ms. |
| `make targeted ONLY="feature:settings:SettingsSecurityTests feature:settings:SettingsSecurityEdgeTests feature:settings:SecurityValidationTests feature:settings:SecurityConflictTests feature:settings:SecurityCodeTests feature:settings:SecuritySummaryTests"` | 41 cases passed across the initial run and scoped validation rerun. Also passed in final quick's JVM stage. |
| `make targeted ONLY="feature:settings:SensitiveClipboardTests feature:settings:RecoveryCodesDocumentTests feature:settings:AndroidClipboardPortTest"` | 8 passed, 7 s; service stamp cases rerun after adapter fix. |
| `make targeted ONLY="feature:settings:SettingsPasswordPromptTest feature:settings:SettingsPasswordPromptHostTest"` | 17 passed; includes busy back/outside dismissal, sheet/dialog switching, countdown, destructive style and large font. |
| `make targeted ONLY="feature:settings:SecurityScreenTest feature:settings:PasswordScreenTest feature:settings:SecurityViewModelTests"` | 13 passed, including submit failures, foreign cancellation, feedback acknowledgement and the Swift password UI case. |
| Two-factor/QR/date/document/secure-window targeted classes | 22 passed, 7 s; includes the Swift disable/cancel UI case. |
| Session store integration, release home, shell routes, debug seed, recreation checks | 3 + 1 + 16 + 2 + 2 passed. Integration lives on `SettingsFixture` in `:feature:settings`; recreation uses a standalone activity, with no AppGraph/root test allowlist addition. |
| `:feature:settings:testDebugUnitTest -Pcf.tier=layout` through `Scripts/run-tier.sh` | 861 passed, 49.34 s; zero budget overruns/hangs. Includes all 456 security profile/language/theme cases. |
| Layout tier for `:app`, `:core:ui`, `:core:data`, `:core:designsystem`, `:core:testing-compose`, `:feature:trip` | 657 passed, execution 38.84 s; zero overruns. Initial collector glob included unrelated modules; corrected collector reused certified outputs in 1.95 s. XML and measurements are retained in `layout-other/`. |
| Final security-guard rerun after fidelity fix | 456 passed, 35.75 s; `10c-security-guards-final/`. |
| `make captures ONLY=SetSecurity,SetPassword,PasswordPrompt,Set2FA` | 24 PNGs, 25 s. Full command ran once; affected Set2FA captures were refreshed with the scoped capture class after final fidelity fixes (9 s). |
| `make targeted ONLY="app:SettingsSecurityDeviceTest"` | Setup/confirm/download journey passed in 1.944 s; class invocation 32 s. Keyboard assertions passed, but the initial shared cleanup failed while the prompt still owned focus. |
| `make targeted ONLY="app:SettingsSecurityDeviceTest#promptKeepsSubmitAboveTheKeyboard"` | Passed in 2.400 s, invocation 9 s, after explicitly cancelling the prompt before shared cleanup. Only the failed method was rerun. |
| `adb -s emulator-5554 logcat -d -s StrictMode` | Exit 0, zero lines; no violation in the journey logs. |
| `:app:testReleaseUnitTest :app:verifyReleaseManifest :app:analyzeReleaseR8Config` | Passed in the combined 1 min 15 s lint/release invocation; release XML archived in `10c-release/`. |
| `./gradlew :app:lintDebug :feature:settings:lintDebug :feature:trip:lintDebug` | Passed, 17 s, after fixing the pre-existing trip test cast. No allowlist widened. |
| `make generators-check` | Passed, including contract/string/token/icon generation checks. |
| `make size` | Passed, 1 min 23 s; **3,924,862 bytes (3.92 MB)** for arm64-v8a/xxhdpi/en. No new `.so`; existing allowed graphics-path library only. |
| `make quick` — exactly one invocation | JVM/logic stage measured 3,846 tests in 45.45 s; zero overruns/hangs. Exit 1 on two stale count assertions described below; the full command was not rerun. |
| `make targeted ONLY="core:designsystem:LucideGlyphsTest#everyBoardPathParsesAndVectorsAreCached core:ui:StringParityTest#countsAreRecorded"` | Both passed, 3 s per module, after updating count fixtures. |
| Remaining quick stages, run separately | Build-logic 37 passed (3.62 s); script tests 171 passed (19.850 s); contracts 24 passed and one environment-dependent parity case corrected below; l10n 49 passed (0.019 s); icons 11 passed (0.087 s). |
| `make timing` | 7 passed, 3.75 s; zero overruns/hangs. |
| `./gradlew spotlessCheck` | Passed, 39 s. Final NonCancellable teardown strengthening: 20 affected cases passed (7 s), then scoped Spotless apply passed (13 s). |

The single quick run correctly caught the added smartphone's stale Kotlin glyph count (58 → 59), and the string parity counter treating the untranslatable clipboard label as iOS catalog copy. Both assertions were repaired and only those methods rerun. Every other quick JVM/logic result, including Konsist and timeline invalidation, passed. New security cases measured in that run stayed below 148 ms warm; class warm-ups are recorded separately. The full quick command therefore has a recorded failure, followed by passing targeted corrections and passing remaining stages, rather than a fabricated final green invocation.

The manually invoked tooling script initially lacked Make's `IOS_REPO`/`WEB_REPO` exports: 24 contract tests passed and the parity case raised an environment error. With the normal exports restored, only that case was rerun and passed in 0.090 s. `make generators-check` independently passed with the normal environment. No test hung and no Gradle invocation needed the three-minute stall recovery.

## Remaining host checks

Per the user's execution constraints, `make check`, `make perf`, `make perf-timeline` and `make baseline-profile` were not run. No command was blocked by adb or emulator access. The pixel emulator remains running.

The app graph/settings route integration touches the launch entry path. The host should run **`make perf`** and **`make baseline-profile`**, recording cold/warm startup and first-frame/TTFD metrics in `docs/perf-startup.md`. No production timeline path changed, so `make perf-timeline` is not required for this phase. These are pending measurements, not emulator/device performance claims.

The host reviewer should compare all 24 captures with their named boards and check one real DocumentsUI save on `pixel`. The automated document-picker test uses Espresso-Intents as specified by OQ-A517; it verifies the written file bytes rather than driving another app's UI. No manual real-picker check is claimed here. The full `make check` commit gate remains for the host.

## Review round 1

The host's `make check` passed on `a870672a` (reported by the reviewer). All five verified findings are fixed in separate signed commits:

- `b738a605`: missing format arguments preserve iOS's literal `{{name}}` placeholder. Both string and absent `min` values are covered by JVM model/helper tests; the tests first reproduced `NoSuchElementException` with the old implementation.
- `0d257b88`: `clearSecrets` no longer cancels a submitted mutation. A delayed confirmation test on `SettingsFixture` proves the real store update and `performSecurity` completion after clearing, while the generation guard keeps UI secrets/feedback cleared.
- `27809289`: the ViewModel derives the date in its UI state from the security summary and language on the injected compute dispatcher. Formatter failure silently omits the line. JVM tests cover dispatcher use, failure, summary/language changes and unrelated account updates.
- `ca190463`: only the lockout text reads the per-second count; controls observe a derived lock boolean. A composition-trace guard verifies countdown updates without prompt/submit recomposition and verifies controls update at expiry (37 ms in quick).
- `a29a5393`: Cancel/Forgot have zero horizontal content padding and token-based 48 dp targets. The native minimum-width override and start-aligned label width prevent centering short labels away from the field edge. A production geometry guard checks both alignment and height without asserting copy.

Validation used the same JDK/SDK and in-process compiler settings recorded above. Final results:

| Command / scope | Result and wall time |
|---|---|
| `make targeted ONLY="feature:settings:SecurityTextArgumentsTests#nonNumericOrMissingMinimumLeavesTheIosPlaceholderIntact"` | 2 passed; Gradle 4 s. |
| `make targeted ONLY="feature:settings:SessionSettingsSecurityTests feature:settings:SecurityViewModelTests"` | 10 passed; Gradle 5 s. |
| `make targeted ONLY="feature:settings:SecurityConfirmedDateTests feature:settings:SecurityConfirmedDateStateTests feature:settings:SecurityViewModelTests feature:settings:TwoFactorScreenTest"` | 19 passed; Gradle 10 s. |
| `make targeted ONLY="feature:settings:SettingsPasswordPromptRecompositionTest feature:settings:SettingsPasswordPromptTest feature:settings:SettingsPasswordPromptHostTest feature:settings:SecurityViewModelTests"` | 24 passed; tier 8.32 s. |
| `make targeted ONLY="feature:settings:SettingsPasswordPromptTest feature:settings:SettingsPasswordPromptHostTest feature:settings:SettingsPasswordPromptRecompositionTest"` | 19 passed; tier 9.06 s. Alignment case 23 ms. |
| `Scripts/run-tier.sh 10c-r1-layout --results 'feature/settings/build/test-results/testDebugUnitTest/*.xml' --timings 'feature/settings/build/cf-timings/testDebugUnitTest/*.json' -- ./gradlew --console=plain :feature:settings:testDebugUnitTest -Pcf.tier=layout -Pkotlin.compiler.execution.strategy=in-process` | 861 passed; 49.89 s, zero overruns/hangs. |
| `make captures ONLY=PasswordPrompt` | 10 prompt PNGs refreshed; 21.51 s. Other existing PNGs preserved. |
| `./gradlew --console=plain :app:lintDebug :feature:settings:lintDebug -Pkotlin.compiler.execution.strategy=in-process` | Passed; 25.44 s. |
| `make quick` — exactly once this round | **Passed**, full wall time 69.35 s. JVM/logic stage: 3,778 measured tests, 42.93 s, zero overruns/hangs. Build-logic's 37 and timing's 7 cases reused certified passing results (2.90 s / 2.83 s). Tooling: 171 + 25 + 49 + 11 tests passed (19.953 / 0.112 / 0.017 / 0.092 s). |

One initial date compilation needed an explicit `MutableStateFlow<Language>` type because `Language.En` is a singleton type; the corrected targeted run passed. An attempted module-scoped formatting command could not run:

```text
./gradlew :feature:settings:spotlessApply -Pkotlin.compiler.execution.strategy=in-process
Cannot locate tasks that match ':feature:settings:spotlessApply' as task 'spotlessApply' not found in project ':feature:settings'.
```

The root `make format` succeeded instead. Spotless's state-parameter rule also prompted using a count-provider lambda for the lockout leaf; formatting and tests then passed. No unresolved failure, hang or environment block remains. No perf command, `make check` or emulator test tier ran this round; pixel was left running. Captures still require the host's visual comparison. Logs and archived tier evidence are under `build/test-results/10c-r1-*`.

## Review round 2

All four findings are fixed in separate signed commits:

- `89205e6f`: `SettingsDependencies.performSecurity` starts an async request in its session-owned `securityScope` and awaits it. Production supplies `SessionGraph.scope`; `requestSecurity` keeps the server call, permit-protected store apply and sync on injected IO. All three main/test implementations and the additional saved-state fixture implement the contract. Long-lived work captures dependencies and the request, with no ViewModel, Activity or entry. JVM tests dispose the actual ViewModel store during confirmation and prove store apply plus sync still happen; cancelling the session instead prevents them.
- `5fd0debf`: submit gates on model busy state. A delayed overlap test proves a fresh submission starts after `clearSecrets`, and the old response neither restores secrets nor clears the new request's busy state.
- `e5e7b1e1`: language starts unknown. JVM coverage proves zero formatter calls before language selection and Dutch as the first formatted date. Failure coverage still proves the formatter actually ran off main and remained silent.
- `657c34b3`: the copy tests call the real composable `settingsText` in a headless composition using real English/Dutch resources and the model's missing/string `min` arguments. No UI nodes or screen-copy queries are involved. Temporarily bypassing argument formatting caused all four cases to fail; restoring production made them pass.

| Validation | Final result and wall time |
|---|---|
| `make targeted ONLY="feature:settings:SecurityViewModelTests feature:settings:SessionSettingsSecurityTests"` | 13 passed, 5.24 s. Includes both entry/session cancellation cases and overlapping submissions. |
| Scoped rerun of the three failure-mapping methods; `make targeted ONLY="app:SecuritySecretsNotSavedTest"` | 3 + 2 passed; Gradle 4 s / 8 s. |
| `make targeted ONLY="feature:settings:SecurityConfirmedDateStateTests feature:settings:SecurityConfirmedDateTests feature:settings:TwoFactorScreenTest"` | 14 passed, 8.51 s. |
| `make targeted ONLY="feature:settings:SecurityTextArgumentsTests#nonNumericOrMissingMinimumLeavesTheIosPlaceholderIntact"` | 4 passed, 3.75 s after restoring the intentional mutation. |
| App/settings layout tier (`:feature:settings:testDebugUnitTest :app:testDebugUnitTest -Pcf.tier=layout`) | 937 measured cases passed, 48.92 s; zero overruns/hangs. |
| `./gradlew --console=plain :app:lintDebug :feature:settings:lintDebug -Pkotlin.compiler.execution.strategy=in-process` | Passed, 21.77 s. |
| `make quick` — exactly once | Assertions passed; **timing gate failed**, 42.21 s overall. JVM/logic measured 3,784 tests in 42.16 s, one overrun, no hangs. The untouched Terms case below was not retried or changed. |

```text
make quick
run-tier: over budget: robolectric net.chronoflow.feature.onboarding.terms.TermsScreenTest#policyRowStaysEnabledWhileFailed 3290 ms > 3000 ms
make: *** [quick] Error 3
```

The unreached quick stages were completed separately: build-logic's 37 certified cached cases passed (2.80 s tier), tooling's 171 + 25 + 49 + 11 cases passed (19.831 / 0.105 / 0.017 / 0.088 s), and timing's 7 certified cached cases passed (2.92 s tier). Every new regression test passed in quick; the entry-disposal test took 50 ms.

The first ownership run exposed three existing failure-mapping tests whose fake used a non-supervisor background scope. The fake now creates a supervisor child owned by the test scope, matching production; only the three failed methods were rerun before proceeding. A shell setup attempt, `export 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process'`, was rejected with `zsh:export:5: not valid in this context: ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy`; the commands then ran with `env` successfully. No command remains blocked. No perf, `make check` or emulator tier ran; pixel was left running. Logs and tier evidence are archived under `build/test-results/10c-r2-*`.
