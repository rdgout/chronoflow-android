# Phase 10a acceptance — 2026-11-21

Settings home, pending sign-out, Account, Profile and Language are implemented. The state and models port iOS `f391a1fa`; Phase 8 is merged and Terms uses the same pending-change sheet. The phase runs in the main checkout on `feat/phase-10a-settings-home`.

Home observes local account/settings/outbox values and session sync events. Only its sync-line leaf observes the clock. Sign-out captures the session, refuses a stale discard, flushes before confirmation, and delegates wiping and routing to the existing controller. The confirmation title counts displayed changes; the sync line counts outbox rows. Discard and Stay survive activity recreation through the entry ViewModel, without persisting the sheet across process death.

Profile saves the trimmed name and unchanged email through the permitted outbox write. Edited drafts survive incoming pulls, failures and recreation. The name limit counts code points. Language stays device-local: LocaleManager on API 33+, device-config below 33, with disk and binder work on injected IO. Retained ViewModels hold one-shot feedback until acknowledgement; the process feedback survives locale recreation. No new Android-only copy or shipping dependency was added. Release keeps the passkey capability disabled and hides unbuilt Settings rows.

## Brief corrections and implementation notes

- R7a adds the Python manifest-count assertion alongside the Kotlin glyph-count assertion (50 → 55).
- Task 9 records OQ-A483: Robolectric 4.18 stores application locales but does not project them into activity configuration or dispatch recreation. Its API 36 configuration case is ignored with `needs ShadowLocaleManager`; API 36 read/write/reconciliation and both SDKs' System choice remain tested. The pixel test proves actual locale recreation.
- The tier runner now excludes JUnit `<skipped/>` cases from required timing entries. Executed cases still require evidence; two runner regressions prove both rules. No budget changed.
- Accepted but unmapped settings seeds select Settings root, matching iOS's selection before its switch. Plan-route priority is unchanged.
- Terms captures live in `:app` to preserve the feature-import boundary. The two window captures combine the actual activity and sheet renders with native window dim/gravity. Feature guards measure Settings content with fixture insets; native shell chrome remains covered by the existing shell/native-control tests.
- Android's existing Sign-in tag is `sign-in.scroll`, rather than the brief's `auth.signIn`. No auth tag was renamed. The no-pending device test also checks the launch frame counter and t0 do not change.
- `SettingsTestSeedsTest` is a plain JVM test and therefore has no `@Logic` annotation; that annotation selects Robolectric classes in this harness. The brief's annotation shorthand was corrected after the single quick run identified it.
- Final parity review tightened Boolean parsing for the unlimited-credit flag and Unicode whitespace when deriving initials. Dedicated JVM cases cover both; valid board fixtures are unaffected.
- Typography without a convenience property uses `CfTheme.type.style(CfTypeScale.…)`. Generated metrics are reused rather than copied into board literals.

## Board captures and retained differences

`make captures ONLY="Set,DarkSetHome,SignOutPending"` produced ten PNGs in `build/captures/`. The run had 10 passed capture cases and 535 cases skipped by the requested prefix filter, with zero budget overruns. Captures were produced once. No Chromium or stored-reference recording was used. Home and the Terms overlay were inspected for nonblank, complete content; side-by-side board review remains the user's visual acceptance.

| Board / PNG stem | Recorded differences |
|---|---|
| `SetHome`, `SetHomeNL`, `DarkSetHome` | Native M3 system large title and bar/navigation chrome. Account subtitle omits data/policies; Security omits passkeys. Readiness badge is below the subtitle in both languages; English board's trailing badge is not reproduced. Sign-out is the 40 dp outline pill with a native minimum touch target. Bottom padding is 40 dp because the Android bar occupies layout space. Sync type is generated 13/18; the syncing glyph stays static. |
| `SignOutPendingSettings`, `SignOutPending` | Native M3 sheet, scrim and handle. Pending text uses generated 14/20; footer uses board 13/18. Title counts the two displayed entries. The Terms background uses the landed Phase 8 generic terms content. |
| `SetAccount` | Profile and Language only; data/policies and Delete account remain deferred. Native large app bar and navigation chrome. |
| `SetProfile` | Name-only editor, selectable read-only email and “Save changes”; the board's new-email/password/link flow is deferred. Native large app bar and navigation chrome. |
| `SetLanguage` | Native large app bar and navigation chrome; saved feedback uses the shared inverse M3 Snackbar. The static capture shows the System selection; the device test verifies saved presentation through its counter. |
| `SetStates` | Four sync lines plus the offline-disabled row and connection hint. Generic saved/failed toasts, refusal Details and health/readiness examples are outside 10a. |
| `SetStatesMore` | Loading and unavailable states only. Generated/iOS skeleton widths 150/190/130 and row height 60 replace board 140/180/120 and 64. Refusal feedback remains the existing shell's responsibility; no Details action. |

Only `SetHomeNL` supplies a separate Dutch board in this scope. All fifteen guarded states also run in Dutch and dark. Wide pages cap the content at 560 dp from 600 dp; app bars remain full width. `Set2FAWide` and `Set2FAManageWide` supply that rule and are not built as 10a pages.

OQ-A462–A482 record the brief's defaults in order. OQ-A483 records the Robolectric LocaleManager limit. OQ-A68 remains unused. No Android-only copy was added.

## Verification

Wall times below are command/build times unless explicitly labelled as case times. Failed development checks are retained below the table; no complete tier was repeatedly run to chase host timing noise.

| Check | Result | Wall time |
|---|---|---|
| Task 1 button/glyph tests | 4 passed | 7 s |
| Task 1 button-color guard, light/dark | 2 passed | 7 s |
| `designSystemPrimitivesHaveGuards` | passed | 12 s |
| State port and initial edge tests | 12 passed | 5 s |
| Home/sign-out model cases | 10 passed across the initial run and two corrected methods | 3 s initial + 3 s targeted methods; JDBC warm-up 391 ms |
| Scaffold/components/wide guards | 5 passed | 7 s initial; wide rerun 6 s; restored badge method 9 s |
| Home structure/invalidation | 5 passed; minute tick leaves home/rows unchanged | 9 s |
| Sheet state/structure/recreation and Terms slot | 5 passed across the initial run and corrected modal-back method | 7 s feature + 6 s Terms; corrected method 6 s |
| Stored states | 3 passed | 13 s |
| Profile model/failure cases | 16 passed | 6 s; max case 78 ms; JDBC warm-up 424 ms |
| Account retry, Account/Profile structure, draft recreation | 5 passed | 7 s; first cold UI case 2.135 s |
| Language ViewModel | 4 passed | 4 s |
| App language applier | 4 passed, 1 ignored | 6 s final; cold SDK-36 case 1.292 s, SDK-29 case 798 ms |
| Runner skipped/missing-entry regressions | 2 passed | 152 ms + 141 ms |
| Settings navigation, debug seeds and existing plan seeds | 26 passed | 1 s core + 8 s app |
| Pending seed/parser cases | 73 passed | 9 s; max case 77 ms; JDBC warm-up 456 ms |
| Settings guard matrix | 300 passed | 24 s corrected run; zero overruns |
| Minimum 280 dp width, Dutch, font scale 1.3 | 15 passed | 12 s build / 13.72 s runner wall; cold first case 2.522 s, warm cases at most 131 ms |
| Touched-module layout tier | 656 passed, 1 allowed API-36 case ignored | 25 s execution + 1 s scoped evidence collection; zero overruns |
| Board captures | 10 PNGs | 24 s |
| No-pending sign-out on pixel API 36 | passed | case 1.986 s |
| Pending Stay/Discard journey on pixel API 36 | passed | case 1.579 s; sign-out class build/run 11 s |
| Language/recreation/Back on pixel API 36 | passed; locale `nl`, saved counter 1 | case 3.996 s; class build/run 18 s |
| `:app:lintDebug :feature:settings:lintDebug` | passed | 34 s |
| Generators | passed, including 49 string and 11 vector generator cases | final log `/tmp/chronoflow-10a-generators-final.log` |
| Strict unlimited-flag regression | passed | 5 s |
| Unicode whitespace regression | passed | 8 s |
| `:app:testReleaseUnitTest :app:verifyReleaseManifest -Pcf.tier=quick` | 202 passed; release manifest passed | 7 s final; zero overruns |
| Single final `make quick` | 5,362 passed, one test-tier annotation failure; no budget failures or hangs | 40 s build / 41.31 s runner wall |
| `TierRegistryTest#plainJvmClassesHaveNoTier` after removing the annotation | passed | 5 s |
| Remaining quick build-logic stage | 37 passed, certified cached results | 2 s build / 2.92 s runner wall |
| Remaining quick tooling stage | 171 harness, 25 contract, 49 string and 11 vector cases passed across the initial run and corrected parity invocation | 19.857 s harness; 0.019 s initial contracts + 0.084 s corrected parity; 0.016 s strings; 0.080 s vectors |
| Final serial `make timing` | 7 passed, certified cached results for unchanged budget code | 2 s build / 3.10 s runner wall |

The badge guard was proved against a deliberate missing-gap mutation: it failed, then passed after restoration. The first wide-test initialization used a Robolectric parameter runner's outside-sandbox `BeforeClass`; native warm-up now runs once in the sandbox and is recorded separately. Two model tests were corrected to account for sign-out publication preceding best-effort logout and for explicit sync refresh versus debounce. The busy-sheet test now dispatches Back to the modal window. No product failure was hidden by those fixture corrections.

The initial generator check found the omitted Python glyph count. The initial guard matrix's 112 failures all came from fixture rail controls above the fake top inset; 188 cases already passed. The corrected fixture guards Settings content and all 300 pass. The first layout report used overly broad timing globs and saw three unrelated uncertified timing directories; the scoped report reused the already-passed selected task evidence. The API-36 ignored-case timing rejection led to the runner fix above.

`make quick` ran exactly once. Its sole failure was `TierRegistryTest#plainJvmClassesHaveNoTier`; the annotation was removed and that method passed. The stages after its early exit were completed separately: build-logic, tooling, then the serial timing gate. The first manual tooling invocation omitted the `IOS_REPO`/`WEB_REPO` variables normally exported by Make, so one contract parity test failed. That method passed with those variables supplied, and the remaining string/vector stages passed. The full quick command was not rerun. Timing results retain the original seven cases and their metrics; no new device performance claim is inferred from cached JVM evidence.

Both emulator classes ran once, under the device lock. No command was blocked by adb, the sandbox or signing. Gradle printed nonfatal Kotlin-daemon marker `Operation not permitted` diagnostics but compiled and ran successfully. No hang or three-minute Gradle stall occurred. The emulator is left running.

## Performance hand-off

No startup or timeline performance numbers are claimed. Per the user's instruction, do not run performance/profile generation here. AppGraph language services and the Terms slot touch startup/configuration code, so the user should run:

```sh
make baseline-profile
make perf
```

Record cold/warm TTID, TTFD and the startup trace rows, including the existing ±25% comparison, in `docs/perf-startup.md`. No timeline renderer, clock ownership, drawing cache, benchmark journey or wizard path changed, so no timeline benchmark/profile journey update was made. `make check`, other emulator tiers and T2 AVDs were not run as requested.

## Commits

Every commit is GPG-signed, has no trailer, and remains in its original linear history. Task 14 is the documentation commit containing this record.

| SHA | Subject |
|---|---|
| `3d250846` | `docs(phases): refresh the 10a brief against 9h` |
| `283e4b19` | `feat(designsystem): add settings glyphs and a ghost destructive button` |
| `2295bdcb` | `feat(settings): port the settings home state` |
| `735b40f7` | `feat(settings): add the home model and sign-out flow` |
| `51f1861a` | `feat(settings): add the settings page scaffold and components` |
| `86d79e24` | `feat(settings): add the settings home screen` |
| `191ecd33` | `feat(settings): add the sign out pending sheet` |
| `c19d55e3` | `feat(settings): add stored and loading states` |
| `46384508` | `feat(settings): add the account and profile pages` |
| `f174ac75` | `feat(settings): add the language page and applier` |
| `2a6856cf` | `feat(app): wire settings routes and seed paths` |
| `62c06426` | `feat(debug): seed settings pending changes` |
| `0fb5591f` | `test(settings): guard and capture the settings boards` |
| `2261725c` | `test(app): walk sign out and language on the device` |
| `d9756e9f` | `fix(settings): preserve strict state parsing parity` |
| `a0e0c1b2` | `test(settings): keep pending seed tests in the jvm tier` |
| `d692ceb4` | `test(settings): cover the minimum supported width` |
