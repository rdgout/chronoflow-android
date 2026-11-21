# Phase 10d acceptance

Implemented on `feat/phase-10d-settings-account-plan`. Passkey management is built behind the existing capability, account deletion wipes local state and ends on Welcome, and Plan & credits is read-only. All four phase groups in `plan.md` are complete. No push was performed.

## Capability, secrets and ownership

`PasskeyCapability.Shipped` remains off (`SERVER_READY = false`); the Security row requires both capability and a server count. A disabled route composes nothing, pops once and sends no request. There is no passkeys seed mapping or release override. Mock registration uses the deterministic provider; API registration uses the existing Credential Manager client, with the shared pending-call guard.

Creation options are validated before the ceremony. Registration rebuilds the attestation from parsed fields, normalizes binary fields, checks id/rawId equality, filters transports and omits an empty list. Challenge, creation-option, user-id, excluded-id and attestation descriptions redact their payloads. The model never holds the password; cancellation of the native ceremony leaves the screen-owned field ready for retry. Other failures follow the pinned Swift branches.

Passwords live in `remember`-owned `TextFieldState` inputs, passed through `SecretField`. They are absent from UI state, saved state, routes, storage, outbox, logs and exception descriptions. The recreation test preserves the Delete stage and proves the recreated password field is empty. No secure-window policy was widened. Disk, Keystore and network work continue through injected IO dispatchers.

Passkey mutations remain session-owned. DELETE and the following sign-out are app-owned, so disposing a page or cancelling the session cannot interrupt the local wipe after server deletion. Foreign session cancellation maps to `StaleSessionException`; real caller cancellation propagates. The model generation drops late UI results without cancelling submitted work. The real graph integration test leaves the gate router running and proves Welcome remains after pending observer work; the pure routing policy additionally pins Welcome preservation.

Unverified accounts still cannot delete in-app until S4. Risk 4 and OQ-A525 record the gap. OQ-A519–OQ-A532 append the brief's fourteen defaults, in order; no additional OQ was needed. The window-policy table now says 540 dp for the existing wide password prompt, matching OQ-A502.

## Brief corrections and implementation names

The signed preflight refresh records R11: `android_passkeys_intro` belongs to `android-only.json` and its generated outputs rather than the iOS name lock; registration DTO descriptions needed redaction. No copy or generated string changed.

Task 7 records that `SettingsEntry` hosts the delete route beside Account, because forwarding a second ViewModel through `AccountRoute` violates the existing Compose rule. Integration obtains the real graph through the already-approved `AppSessionFixture` boundary; the Konsist allowlist is unchanged. Its fixture enables SessionSync before deletion. `SettingsDeleteRoutingTest` tests the extracted routing policy. Seed coverage is `SettingsDeleteSeedTest`, alongside the existing `ShellNavSettingsSeedTest`.

The 10c fallbacks were applied in Task 4: `run(work)`, `PasskeyFailure`, passkey success handling, the passkey branch before clearing a password, destructive removal, and clearing the removal target. The store already merged `passkeyCount`; a new JVM test pins preservation of the other security fields. Existing constructor call sites use the named `now` argument. Task 7 adds optional prompt tags while preserving the 10c defaults. The Account delete row is private because the standard row's icon disc differs from the board's direct 20 dp trash glyph.

## Boards and captures

The capture command ran once and wrote these eleven files under `build/captures/`:

- `SetPasskeys_pixel8_en_light.png`
- `SetPasskeysEmpty_pixel8_en_light.png`
- `SetDelete_pixel8_en_light.png`
- `PasswordPromptDelete_pixel8_en_light.png`
- `PasswordPromptLocked_pixel8_en_light.png`
- `SetPlan_pixel8_en_light.png`
- `SetPlanCredits_pixel8_en_light.png`
- `SetPlanLoading_pixel8_en_light.png`
- `SetPlanNoLink_pixel8_en_light.png`
- `SetPlanOffline_pixel8_en_light.png`
- `DarkSetPlan_pixel8_en_dark.png`

`PasswordPromptLocked` replaces 10c's Disable stand-in with Delete and a 2,820-second lockout. Fixtures use the two named devices, board dates, 31 Oct “today”, two credits and yearly renewal on 14 Nov 2026. The host still needs to compare captures with the boards; Chromium was not launched.

| Board | Deliberate difference |
|---|---|
| `SetPasskeys` | Generated Android fingerprint/face/screen-lock intro replaces Face ID copy. |
| `SetPlanCredits` | Pending-checkout notice, credits hero and NoLink paragraph; no board purchase button, caption or Billing section. |
| `SetPlan`, `DarkSetPlan` | Website-free payment-failed sentence; no date in that notice, Billing rows or Action needed badge. |
| `SetPlanOffline` | Cancelled hero and ends line, with the offline paragraph below; no disabled Billing rows. |
| `PasswordPromptDelete`, `PasswordPromptLocked`, `SetDelete` | Shared native sheet/dialog chrome, safe insets and the existing 540 dp wide prompt. |
| All pages | Native app bar and navigation chrome follow A9. Loading rows already match the board's 150/190/130 widths and 60 dp rows. |

## Validation

JDK 17 runs Gradle; test JVMs use JDK 21. Commands use `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`. Make commands used `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process'` after the sandbox denied Kotlin's marker file under `Library/Application Support/kotlin/daemon`. The compiler fallback succeeded on the earlier model/data calls. Logs are archived under `build/test-results/10d-validation-logs/`.

Times below are command/build wall times unless identified as an individual case. Targeted selectors use `make targeted ONLY="…"`.

| Check | Result / wall time |
|---|---|
| Trash generation and generator guard | Generated successfully. Initial manifest-count assertion expected 59; updated to 60 and its focused test passed in 0.026 s. |
| `core:model:PasskeyRegistrationJsonTest core:model:SystemPasskeyRegistrationProviderTest core:model:DeterministicRegistrationProviderTest` | Passed; 12 s. |
| `core:data:CredentialManagerClientCreateMappingTest` | Nine mappings passed; 8 s. |
| Passkey model, parameterized failures, plural fallback, changed 10c model tests and `core:data:AccountSecurityStoreTests` | Passed; 11 s + 3 s. |
| Passkey screen, row, capability guard, 10c Security screen and shell seed mapping | Passed; 7 s + 5 s. Extracted row's focused structure check passed; 6 s. |
| `feature:settings:SettingsDeleteTests feature:settings:SettingsDeleteFailureTests` | Passed; 4 s. |
| Delete dependency integration and seed checks | Seed/stale cases passed initially. Enabled sync in the integration fixture, then both failed request methods passed; 5 s. |
| Delete sheet, Account ViewModel and existing password-prompt tests | Passed; 8 s. Pure delete routing policy passed; 5 s. |
| `feature:settings:SettingsPlanTests feature:settings:SettingsPlanTextTests` | Passed; 2 s after making parameter array types explicit. |
| Plan screen, no-billing source guard and home visibility | All other cases passed in the 8 s run; the focused pull test passed in 6 s after starting the gesture below the app bar and awaiting Compose idle. Shell seed mapping passed; 3 s. |
| Layout tier: Settings, app, design system and data tasks | 1,307 measured cases; 65.73 s; no budget overrun or hang. Four small-profile Delete confirmation cases exposed a missing bottom safe inset. |
| `feature:settings:AccountPlanGuardsTest#screenFitsTheProfile` after the safe-inset fix | All 360 cases passed; 26.72 s; no budget overrun. Covers 18 states × five profiles × en/nl × light/dark. |
| Requested `make captures ONLY=…` | Eleven PNGs; 21.59 s; no budget overrun. |
| `app:SettingsDeleteDeviceTest` | Passed once on pixel AVD / Android 16, debug. Journey 2.786 s; command 12.46 s. Welcome, removed account directory/session blob, zero ChronoFlow StrictMode disk/network violations. |
| `./gradlew :app:lintDebug :feature:settings:lintDebug :core:data:lintDebug :core:designsystem:lintDebug :app:verifyReleaseManifest` | Passed; 56 s. |
| `make size` | Passed; 105 s. 3,955,581 bytes (3.96 MB), arm64-v8a/xxhdpi/en; +30,719 bytes versus 10c's 3,924,862. No added dependency or native library; only the existing allowed graphics-path library. |
| `app:ReleaseReadinessTest` | Passed; 4 s, including shipped-off passkeys and absence of debug/mock entry points. |
| `make quick` (one invocation) | 3,608 measured cases; 38.24 s; no budget overrun or hang. Failed only the old 59-glyph expectation in `LucideGlyphsTest`. |
| Focused `core:designsystem:LucideGlyphsTest#everyBoardPathParsesAndVectorsAreCached` | Passed after updating the expected count to 60; 1 s. |
| Unfinished feature quick tasks, run directly with `-Pcf.tier=quick` | 1,946 measured cases; 22.23 s; no budget overrun or hang. Found the old Account test expecting Delete to stay hidden. |
| Focused `feature:settings:AccountProfileScreenTest#accountHasProfileAndLanguageRows` | Passed; 7 s. Now checks the visible row dispatches `AccountEvent.Delete`. |
| Remaining build-logic stage | 37 passed; 3.01 s. |
| Remaining tooling stage | All 256 unique tests passed: script tooling 171 in 19.870 s; contracts 25 (one environment recovery); localization 49 in 0.017 s; icons 11 in 0.087 s. |
| `make timing` (last test stage) | Seven passed; 3.76 s; no budget overrun or hang. |
| Final `make format` | Passed; 17 s. |

The layout selector could not match parameter display names. These attempts produced no matching tests (not a layout result):

```text
make targeted ONLY='feature:settings:AccountPlanGuardsTest#screenFitsTheProfile*DeleteConfirmation*small*'
No tests found for given includes: [*.AccountPlanGuardsTest.screenFitsTheProfile*DeleteConfirmation*small*](--tests filter)

./gradlew :feature:settings:testDebugUnitTest -Pcf.tier=targeted --tests '*AccountPlanGuardsTest*DeleteConfirmation*small*'
No tests found for given includes: [*AccountPlanGuardsTest*DeleteConfirmation*small*](--tests filter)
```

The supported method selector then ran all 360 parameters successfully. The initial layout evidence remains under `build/test-results/layout-10d/`; passing account/plan, capture and device evidence are archived under `10d-account-plan-layout`, `10d-board-captures` and `10d-delete-device` respectively. Near-budget cases were only first-class warm-ups (2.352 s account/plan; 2.306 s root hand-off; 2.142 s button); no budget was loosened.

The single `make quick` invocation remains recorded as failed; it was not repeated. The two stale expectations were corrected and their methods rerun; before review, those fixes were folded into the implementation commits that introduced the changes. All other assertions in those runs passed. The remaining build-logic, tooling and serial timing stages passed separately, as recorded above.

Other resolved build/fix rounds were compiler unused-Unit warnings, explicit parameter-array types, a nullable Java system-property warning, the Account route argument migration, and formatting. The direct tooling-script invocation initially lacked Make's exports:

```text
Scripts/test_scripts.sh
AssertionError: set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)
```

Only the failed parity method was rerun with `IOS_REPO=/Users/rickgout/Sites/chronoflow-ios` and `WEB_REPO=/Users/rickgout/Sites/chronoflow-new`; it passed in 0.090 s. The two remaining localization/icon batches then passed. An optional process diagnostic was unavailable:

```text
ps -axo pid,etime,command | rg '(Gradle Test Executor|GradleDaemon)' | head -8
zsh:1: operation not permitted: ps
```

It blocked no validation. No hung run or signing failure occurred. No permission escalation or destructive emulator action was needed.

## Host follow-up

`make check`, `make perf`, `make perf-timeline` and `make baseline-profile` were not run, as instructed. Because app graph/settings entry wiring changed, run `make perf` and `make baseline-profile` on the host and record startup TTID/TTFD in `docs/perf-startup.md`. No production timeline path changed, so this phase has no timeline benchmark result or new timeline measurement request. Review the eleven board captures. Real-server Credential Manager verification and unverified deletion remain S1–S4 follow-ups; the shipped capability remains off.

The deletion test types a password only in the credential-check prompt under test. No authentication journey is used to reach Settings. The pixel emulator is left running. No blocked adb command remains.

## Signed implementation commits

| Commit | Subject |
|---|---|
| `b7fc55ca` | docs(phases): refresh the 10d brief against 10c |
| `25048e18` | feat(designsystem): add the trash glyph |
| `82e9d1e9` | feat(model): add passkey registration json and providers |
| `98a37e1a` | feat(data): create passkeys through credential manager |
| `32bfc6f3` | feat(settings): port the passkey model |
| `de4c1a37` | feat(settings): add the passkeys page and security row |
| `cdfd32df` | feat(settings): port account deletion |
| `8ce025d4` | feat(settings): add the delete account sheet and prompt |
| `2b29ac82` | feat(settings): port plan and credits state |
| `774aef4c` | feat(settings): add the plan and credits page |
| `5dee2a21` | test(settings): guard and capture the account and plan boards |
| `87a6abe5` | test(app): delete the account on the device |
| `31063321` | docs(settings): record passkeys, delete and plan acceptance |

`31063321` records this report and the OQ rows. All implementation commits have a GPG signature.

## Review round 1

All five findings are fixed in separate signed commits: `37f07002` adds JVM coverage for the three ViewModels and leaves the Plan swipe testing event wiring; `aec206f3` directs delete sign-out to Welcome; `02f857f0` restores the pending checkout in `SetPlanCredits`; `faf13d41` updates the Account capture/documentation; `d25e5b9d` updates passkey metadata when the injected clock's local date changes. Ordinary minute ticks do not reformat metadata. Resource lookup lives in the route adapter, so the ViewModel clock test runs on the JVM with one scheduler.

The running gate-router regression test records every route: before the routing fix it failed with `[Today, SignIn, Welcome]`; afterwards it passes with `[Today, Welcome]`. Normal sign-out tests remain unchanged and pass. `SetPlanCredits_pixel8_en_light.png` now shows Payment processing and differs from NoLink; `SetAccount_pixel8_en_light.png` shows Delete account. Both regenerated PNGs are in `build/captures/`. The pending Plan state also joins the profile/language/theme layout matrix.

| Validation | Result / wall time |
|---|---|
| Plan, Passkeys and Delete ViewModels, delete failure matrix, Plan screen | 36 passed; 11.93 s. Strengthened formatter guards: two passed, 4 s; two added reset cases passed, 4 s. |
| Delete dependency, routing and normal sign-out wiring | Ten passed; 9.43 s. The expected pre-fix regression failed once, 6 s. |
| Pending-checkout fixture and notice structure | Three passed; 6.96 s. |
| Account row structure/event | One passed; 5.57 s. |
| Passkeys ViewModel, screen, capability and model | 32 passed; 8.88 s. Final midnight/capability methods: two passed, 4.39 s. |
| `make captures ONLY=SetPlanCredits` / `make captures ONLY=SetAccount` | One PNG each; 20.94 s / 20.75 s. |
| `./gradlew :app:lintDebug :feature:settings:lintDebug` | Passed; 24 s. |
| App/settings layout tier | 1,316 passed, one skipped; 68.07 s; no budget overrun or hang. |
| `make quick` (one invocation this review) | All assertions passed (3,921 measured cases); 43.45 s. Timing gate failed only unchanged `TermsScreenTest#policyRowStaysEnabledWhileFailed`: 3,056 ms > 3,000 ms class warm-up allowance. Not retried. |
| Remaining quick stages, run once after the gate stopped Make | Build logic: 37 passed, 2.89 s (up-to-date). Tooling: 256 passed, batches 19.853 / 0.109 / 0.017 / 0.090 s. Serial timing: seven valid up-to-date results, 2.78 s. |

The failing command was `env 'ORG_GRADLE_PROJECT_kotlin.compiler.execution.strategy=in-process' make quick`; its final output was:

```text
BUILD SUCCESSFUL in 42s
run-tier: over budget: robolectric net.chronoflow.feature.onboarding.terms.TermsScreenTest#policyRowStaysEnabledWhileFailed 3056 ms > 3000 ms
make: *** [quick] Error 3
```

Logs and focused timing evidence are under `build/test-results/10d-r1-*`. No new OQ or copy was needed. No perf tier, `make check`, push, history rewrite or emulator shutdown was performed. No sandbox blocker remains; the unrelated quick timing overrun above remains recorded as a failure.
