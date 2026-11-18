# Authentication

The implementation follows the pinned iOS auth models and the auth boards. Auth routes replace the root without transitions. Sign in (5a), two-factor (5b), password reset/App Links (5c), and Sign up/Verify (5d) are implemented. Accepted-session destinations retain their placeholders until their phases. The sections below retain each phase's verification record.

Phase 5a, 2026-11-18: implementation and the focused JVM/Robolectric checks are complete. Both orchestrator-authorized follow-up checks pass: the unchanged keyboard test with the full keyboard after correcting functional device prep, and the complete `make quick` command in 31.88 s. Acceptance remains **open** for browser board comparison, manual autofill/TalkBack checks, and `make check`. No performance benchmark, baseline-profile, `make check` or other emulator tier ran.

## Shell and tokens

All feature geometry comes from generated `AuthTokens`, `CfTypeScale`, safe insets and the window. The Android-only window/target constants live in `@BoardMetrics("AuthSignIn", "AuthStates") AuthMetrics`.

| Surface | Value | Source |
|---|---|---|
| Reference capture | 375 × 812 dp, density 3, zero insets | AuthSignIn |
| Content padding | safe top/start/end + 16; safe bottom + 24 | `AuthTokens.gutter/bottom` |
| Back and opposite announcement slot | 48 × 48 dp; back glyph 22 dp | `AuthMetrics.TOUCH`; `AuthTokens.backGlyph` |
| Brand | moon 20 dp, gap 6 dp; Fraunces 400 opsz20, 18/24 | generated tokens/type |
| Heading | margin 32 dp (compact 24); Fraunces 400 opsz32, 36/40 | generated tokens/type |
| Heading to description | 8 dp; description Figtree 400, 16/24 | generated tokens/type |
| Spacer before card | at least 24 dp, expands with available height | `AuthTokens.spacer` |
| Card | padding 20; radius 26; gap 14; no border; soft shadow | `CfCard`, generated tokens |
| Footer | gap 12; visual minimum 48 dp | generated tokens |
| Wide window | from 600 dp; sections max 560 dp, centred within safe content | plan window policy, `AuthMetrics` |
| Field box / buttons / links | field ≥ 48; Lg button visual 40 with 48 target; links ≥ 48 | existing `Cf*` controls, `AuthMetrics` |
| Banner | padding 12/14; gap 10; radius 18; 1 dp border; 18 dp glyph, 1 dp down | `CfBanner`, generated tokens |

The scroll column owns the viewport and flexible spacer. Capped section widths keep the brand, heading, card and footer aligned. The `pixel8` geometry test proves the footer ends at safe bottom − 24 without adding a measured height to state. Small and large-font tests reach the footer by scrolling; fold and side-navigation tests prove the 560 dp width and asymmetric-inset centring.

## Settled board differences

| Raw board | Shipped behaviour / appearance |
|---|---|
| Dotted sky | Plain generated sky, as settled on iOS |
| Sample email/password | Empty fields |
| Back link to Main | Welcome in its final state, without replaying the intro |
| Guest footer link | `Route.GuestWizard` |
| Visible passkey and divider | Absent from composition while `PasskeyCapability.Shipped` is false |
| 36 dp input / 40 dp button / 44 dp links | Native field/target sizing; generated button visual retained |
| No offline example | Approved title-only offline banner on the AuthStates surface |
| Underline offset 0.2 em | Compose's standard underline |

No strings were added. Every label uses generated en/nl resources; dynamic banner/field keys resolve through `CopyIndex` in `CopyTable.Auth`. The Android-only copy approval table is unchanged.

## Ownership and requests

| State | Owner | Lifetime |
|---|---|---|
| Email, form, challenge | Process-scoped `AuthServices.signIn` / `AuthNavigation` | Kept between auth routes; leaving auth clears form/challenge and keeps email |
| Password | `remember { TextFieldState() }` in `SignInScreen` | Dropped on recreation/disposal; never saved |
| Request job and presentation state | Entry-scoped `SignInViewModel` (since 5d) | `leave()` cancels its request on disposal; removing the entry clears the ViewModel |
| Layout coordinates | Plain remembered `AuthCoordinates` holder | Layout only; never snapshot state |

`SignInContent` takes state, caller-owned field states and events. The ViewModel adapts the model and owns the request lock. Its presentation state contains the form and passkey visibility: email keystrokes with unchanged feedback do not emit a new card state. A JVM emission test covers this.

The secret adapter reads the password once for the request and clears it in `finally`, then unlocks the form. The successful order is session accept → autofill commit → password clear → stored-gate routing → final cleanup. A `NonCancellable` block covers acceptance through routing, preserving a committed session when its screen request is cancelled. Two-factor stores only its in-memory challenge and navigates without route arguments; it never accepts a session.

The app supplies `LazyPublicDataSource`, resolving the suspend `SessionDataSources.public()` accessor inside each request. Construction performs no IO. Device identity and the memoized device name run on injected IO; the existing source/controller dispatch their network, storage and Keystore work to IO. Production acceptance delegates directly to `StoredDataReadiness.routeAcceptedSession`, which owns its routing task and rechecks the current epoch. No extra app-scope acceptance task or duplicate gate logic was added. The 20 routing cases exercise password/passkey, every gate, cancellation after commit, and sign-out during suspended gate reads.

`AppGraphKey` is read by the factory in `:app`; `CfRoot` supplies creation extras at the composition boundary. `RootContent` retains its transition-free host. Since 5d, its entries have saveable-state and ViewModel-store decorators, in that order. Debug Welcome keeps only its guest seam; every variant routes Sign in normally.

## Failure mapping

| Failure | Presentation |
|---|---|
| Offline | `Offline`: “Needs a connection”, no body |
| Throttled | `Locked`; retain retry seconds, no countdown or disabled submit after completion |
| Sign-in credential classification key | `Credentials`, no field errors; early return |
| Sign-up email already in use | `EmailTaken`; suppress that email message, preserve other supported displayed fields |
| Supported message for a displayed field | Generated localized field error |
| Active two-factor proof error | Ignore errors for the inactive proof; code rejection uses the board's code-specific copy |
| Unknown/hidden/empty validation | `Server`, with any supported displayed fields retained |
| Other data, identity, storage or session failure | `Server`; session failures also remain available as their typed diagnostic |
| Passkey cancel | Silent |
| Passkey unavailable (capability explicitly on) | `Server`; no system UI in 5a |

`AuthFieldCopy` is an exact whitelist. Count arguments must be non-string JSON integers in 0…Long.MAX_VALUE; malformed, negative, fractional, missing and obsolete keys fall back to a banner. There is no client validation, email normalization, offline precheck or throttle-disabled submit.

`AuthFormModel.begin` clears feedback under one lock. A real user edit clears the banner and only that field's message; busy and unchanged edits do nothing. `UserEditReporter` uses `InputTransformation`, so programmatic password cleanup cannot erase new feedback. The form announces the current failure once per revision, resolves fields in sorted order, and does not mark an empty announcement taken.

## Announcement and reveal

The brand's opposite 48 dp slot is a persistent polite live region. A failure consumes title/body and supported field messages; alternating a silent U+200B suffix makes identical failures a new accessibility content change. The node is neither zero-sized nor recreated. `CfBanner(announce = false)` prevents duplicate announcements. There is no deprecated accessibility announcement call.

Submitting clears focus. A completed field failure focuses the first invalid field; a banner-only failure hides the keyboard and brings the banner into view after one frame. The live region replaces iOS's programmatic VoiceOver focus move. It is also a TalkBack stop; manual service verification remains open.

`imePadding` precedes scrolling and consumes IME insets before `LocalSafeInsets` padding. Coordinates are recorded with `onPlaced`; nothing writes measured sizes into snapshot state. The reveal observes IME/animation-target equality inside `snapshotFlow`, then requests the field/submit union with an 8 dp margin. If the pair cannot fit, it aligns the field top using a viewport-tall request. No text shrink, duplicate/pinned action or per-animation-frame composition is introduced.

## Test port map

| Swift suite / case | Kotlin coverage |
|---|---|
| `LockoutWaitTests.roundsUpAtUnitBoundaries` | `core:model:LockoutWaitTests`, parameterized boundaries; Android negative/key additions |
| `AuthFailureMappingTests` | Same suite/method names; mapping on JVM, resource resolution in `AuthCopyResourcesTest` |
| `AuthFormModelTests` | Same names; 24 pinned failure cases × seven contexts, each an independent case |
| `AuthLayoutTests` | Same names; board/all Android profiles/asymmetric insets and keyboard math on JVM |
| `SignInModelTests` success, two-factor, feedback, lock, session-failure and gate cases | Same suite/method names on JVM; email edit twin named `unchangedEmailPreservesServerFeedbackUntilTheValueChanges` |
| `unchangedPasswordPreservesServerFeedbackUntilTheValueChanges` | `SignInScreenTest`, actual input transformation and programmatic clear |
| `committedSessionUsesStoredGateWithAndWithoutCancellation` | `app:SignInModelRoutingTests`, 16 independent password/passkey gate/cancel cases |
| `signOutDuringStoredGateReadPreventsStaleRouting` | Same app suite/name, four gate/cancel cases |
| Native sign-in / keyboard intent | `SignInJourneyTest` and `SignInKeyboardTest`, seeded and tag-only |

Additional tests cover ViewModel request cancellation/emission, secret recreation/save registry, announcements, resource content hints, IME actions, focus, disabled controls, passkey absence, geometry and service ownership. The autofill semantics test compares actual platform content hints: combined Compose `ContentType` objects have identity equality. Reflection is test-only and reads Compose's existing internal hint accessor.

A cheap negative control replaced `isBlank()` with `isEmpty()` temporarily: `AuthNavigationTests#blankAndWhitespaceChallengesCancelTwoFactor` failed at the whitespace assertion. The original code was restored before its signed commit. The native keyboard test also proved its real-keyboard precondition can fail, rather than passing against an accessory strip.

## Verification record

Targeted times below include Gradle/harness overhead unless identified as test duration. Failing methods alone were rerun after fixes. No test hung. In the original phase run, `make quick` ran once after T10, during final verification. Its remaining substeps ran separately in the same order, with serial timing last. The orchestrator subsequently authorized the keyboard investigation below and one full quick rerun.

| Command / selection | Result | Wall time |
|---|---|---|
| `make targeted ONLY=core:model:LockoutWaitTests` | 20 passed, boundary/negative tests all under 1 s | 3.22 s |
| `make targeted ONLY="feature:auth:AuthFailureMappingTests feature:auth:AuthFormModelTests feature:auth:AuthBannerKindTests feature:auth:AuthCopyResourcesTest"` | 227 passed; resources en/nl | 3.60 s |
| `AuthLayoutTests AuthNavigationTests` | 14 unique cases pass; fixed JUnit inline-Size fixture, reran only geometry method | initial 2.13 s; geometry rerun 2.12 s |
| `core:designsystem:LucideGlyphsTest core:ui:CfTextFieldTest` | Both passed; five field cases, two new board glyphs | Gradle 2 s + 6 s; retained second-tier wall 6.29 s |
| `feature:auth:SignInModelTests` | 23 passed; slowest 80 ms | 17.23 s, including concurrent formatter/build lock |
| `AuthScreenGeometryTest AuthLinkRowTest AuthAnnouncerTest UserEditReporterTest` | Nine unique cases pass; fixed sample footer to the specified 48 dp visual minimum | initial 10.68 s; geometry rerun 7.37 s |
| `SignInViewModelTests SignInScreenTest SignInSecretsTest` | 13 unique cases pass; the two harness corrections were rerun alone | initial 9.04 s; corrected methods 6.13 s |
| `app:SignInModelRoutingTests app:AuthServicesTest app:WelcomeSeamTest` | 29 passed, 3.327 s summed test durations | Gradle 9 s |
| `app:WelcomeDestinationsReleaseTest app:ReleaseReadinessTest` | 10 passed | 6.97 s |
| `feature:auth:SignInGuardsTest` | Exactly 17 passed; summed 5.793 s, maximum 3.221 s per case | 10.18 s |
| `make captures ONLY=AuthSignIn,AuthSignInError` | Eight board PNGs, one invocation | 17.15 s |
| `make targeted ONLY="feature:auth:SignInAdaptationCaptureTest feature:auth:AuthStatesCaptureTest"` | Six complementary PNGs; preserves the eight board PNGs | 7.97 s |
| `./gradlew :app:compileDebugAndroidTestKotlin --console=plain` | Passed, includes updated Welcome journey | Gradle 2 s |
| `make targeted ONLY="app:SignInJourneyTest"` | Passed on pixel/API 36; test **2.279 s**; StrictMode IO counter **0** | 10.24 s |
| `make targeted ONLY="app:SignInKeyboardTest"` (original run) | **Failed**, no full software keyboard; test **4.112 s**; no reveal result | 8.81 s |
| `make quick` (original run) | **Failed**: 2,525 passed / one failed; old forced-sign-out placeholder tag and a 2,689 ms Konsist route guard (1,000 ms budget) | 22.64 s |
| `make targeted ONLY="app:LaunchGateRoutingTest#currentUnauthorizedRoutesToSignInAndErasesAccountFiles"` | Passed after updating the structural tag; test 2.647 s | 5.80 s |
| `make targeted ONLY="konsist:KonsistRulesTest#routesAreSerializable"` (diagnostic, before optimization) | JUnit passed; tier failed its budget at 1.263 s | 3.22 s |
| Five focused `KonsistRulesTest` route methods | Passed after memoizing ancestry; project check 542 ms; four positive/negative fixture controls all under 1 s | 3.95 s |
| `Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew --console=plain :build-logic:convention:test` | 26 passed | 1.86 s |
| `IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new Scripts/test_scripts.sh` | 107 script, 25 contract, 49 localization tests passed; icon suite eight passed / one failed (stale 32-glyph count) | 12.37 s |
| `python3 -I -m unittest discover -s tools/icons -t tools/icons -p test_generate_icons.py -k test_manifest_names_all_generate` | Passed after updating count to 34; test 11 ms | 0.057 s |
| `make timing` | Six passed, zero over-budget; serial JVM preparation/drawing budgets | 3.02 s tier / 3.07 s process |
| `make targeted ONLY="feature:auth:AuthAnnouncerTest#persistentLiveRegionRepeatsOnlyForANewFailure"` | Passed after keeping the screen assertion structural; test 2.356 s | 5.62 s |
| `make targeted ONLY="app:SignInKeyboardTest"` (follow-up, after functional prep) | **Passed**, real keyboard and both field/submit geometry checks; test **1.441 s** | 5.49 s tier / 6.74 s prep plus process |
| `make quick` (one authorized follow-up invocation) | **Passed**, 2,528 quick results / zero budget violations; all remaining substeps passed | **31.88 s** complete command |

Initial compilation fixes were explicit parameter array types, fixture construction through the existing validated account constructors, correct Compose action return types/geometry accessors, and parenthesized factory delegation. Layout/semantics fixes were confined to the test harness; no assertion was weakened. Lint was fixed with `make format`.

Final verification found one additional stale screen tag in the earlier forced-sign-out test, updated to `sign-in.email` without changing its route/epoch/erase assertions. The Konsist route guard repeatedly expanded shared parent declarations; an identity-memoized walk now uses the same direct-parent matching API, including indirect ancestry. New fixture controls prove an unannotated indirect route is rejected and an annotated indirect route is accepted. No budget or exemption changed. The generator assertion now includes T4's two new glyphs; generation/content checks remain intact.

The brief's requested literal announcement assertion was replaced with a nonempty content-description check to follow the user's stronger structure-only screen-test instruction. The persistent live region, identical-repeat content change, unchanged message across repeats, and absence of a second banner live region remain checked; model/resource tests cover the copy.

The first standalone `Scripts/test_scripts.sh` invocation omitted Make's exported `IOS_REPO`/`WEB_REPO` values and stopped at the contract parity precondition (14.13 s): `AssertionError: set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)`. The corrected invocation is recorded above. An initial isolated icon invocation using `-I -m unittest test_generate_icons...` could not import the local module; the discovery-based command above ran the actual method. These invocation errors changed no production behaviour.

The complete follow-up `make quick` ran once after commit `5410b29`: quick Gradle tier 15.37 s, including `routesAreSerializable` at 782 ms; build-logic 1.26 s / 26 results; Python tooling 107 + 25 + 49 + 9 passing tests (13.703 + 0.114 + 0.017 + 0.038 s test-runner times); serial timing tier 0.83 s / six results. Build-logic and timing Gradle test tasks were up to date; their result files were collected. The full 31.88 s process is below the 60 s warm budget. No additional quick or emulator tier was run.

The debug APK contains only the existing allow-listed `libandroidx.graphics.path.so` for its packaged ABIs. No Credential Manager or other new native dependency was added. Release readiness passes; `make size` was not run. There is no Sign in performance-budget metric to claim. Startup/timeline benchmarks and profile regeneration remain unmeasured under the user's run limits.

### Native keyboard diagnosis and follow-up

The original run failed with:

```sh
make targeted ONLY="app:SignInKeyboardTest"
```

```text
java.lang.AssertionError: software keyboard not visible — check show_ime_with_hard_keyboard
    at net.chronoflow.app.auth.SignInKeyboardTest.requireSoftwareKeyboard(SignInKeyboardTest.kt:43)
    at net.chronoflow.app.auth.SignInKeyboardTest.fieldAndSubmitStayAboveTheSoftwareKeyboard(SignInKeyboardTest.kt:31)
```

The original failure was at the first email focus. `show_ime_with_hard_keyboard` returned `1`; Gboard logged `keyboardType=accessory` with no keyboard body. Both emulator classes ran once in that initial phase run.

The orchestrator authorized at most two harness fix attempts. The follow-up used `pixel`/API 36, debug, under the device lock:

1. Read-only settings probe: `settings get secure stylus_handwriting_enabled` → `null`; `settings get secure show_ime_with_hard_keyboard` → `1`. Neither `getprop` nor the global/secure settings lists contained a stylus simulation override. Android's [setting definition](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r2/core/java/android/provider/Settings.java) defaults handwriting to enabled.
2. First fix attempt: replace Compose's view-dispatched tap with a system-injected `MotionEvent` explicitly using `TOOL_TYPE_FINGER` / `SOURCE_TOUCHSCREEN`. An activity window callback confirmed `focus tap: source=4098 toolType=1`. `make targeted ONLY="app:SignInKeyboardTest"` still failed with the same real-keyboard visibility assertion: test 4.216 s, tier 8.58 s (process 8.66 s). The prototype was reverted completely.
3. Manual seeded debug probe: `adb shell input touchscreen tap 540 1437`; `dumpsys input_method` showed `initialToolType=1` for the current Android editor and `mInputShown=true`, but Gboard's `AccessoryInputModeNotification` reported `Current active accessory input mode: STYLUS`. Its own editor dump showed `initialToolType=2`. A screenshot confirmed the narrow handwriting toolbar. Thus a finger reached Android correctly; the remaining obstruction was Gboard's stylus accessory mode. The first launch probe found the debug app had been uninstalled; reinstalling the existing debug APK made that probe runnable.
4. Second fix attempt: add `settings put secure stylus_handwriting_enabled 0` to `Scripts/emulator.sh` `prep_functional`, alongside `show_ime_with_hard_keyboard 1`. Run `Scripts/emulator.sh prep-functional emulator-5554`, then `make targeted ONLY="app:SignInKeyboardTest"`. **Passed**: test 1.441 s, tier 5.49 s, prep plus test process 6.74 s. Gboard logged `keyboardType=prime`, a full `BODY` view of `1080 × 641` px, for both focused fields. No Gboard force-stop was used.

The final change touches device prep and its existing script guard only. App code and `SignInKeyboardTest` are unchanged. The ≥100 dp IME precondition, both fields/submit above the real keyboard top, below the status bar, and settled-bounds checks remain intact. `test_emulator.EmulatorTest.test_ensure_skips_boot_when_booted` passes with the added prep expectation (0.307 s test / 0.359 s process). No AVD hardware configuration changed.

Read-only device observations through `Scripts/device_lock.py`: `settings get secure autofill_service` → `null`; `settings get secure enabled_accessibility_services` → an empty line. Thus no provider autofill or spoken TalkBack result is claimed. Autofill-bypassed edit feedback remains on the default (clear on the next keystroke); TalkBack's persistent announcement stop and identical-repeat speech remain manual checks.

## Captures and comparison

All 14 PNGs are under `build/captures/`; no stored Roborazzi references were added:

- `AuthSignIn_{none,locked,server}_en_{light,dark}.png` (six).
- `AuthSignInError_en_{light,dark}.png` (two).
- `AuthStates_en_{light,dark}.png` (two, banner catalogue without documentation headings).
- `auth/SignInOffline_en_{light,dark}.png` (two).
- `auth/SignInPasskeyOff_en_light.png` (one, shipping capability).
- `auth/SignInFieldError_en_light.png` (one).

Native captures were inspected against the HTML geometry/type/surface declarations. Side-by-side browser comparison remains **pending**, rather than claimed as accepted: the local browser failed before opening a page. At 375 dp the native intrinsic link widths stack Forgot/Create account; locked/credentials states need vertical scrolling to reach the full footer. That follows the ported fit/scroll policy; do not shrink text or pin an action. Record this alongside the target-size, empty-field, plain-sky, underline and font-wrap adaptations when reviewing `AuthSignIn` and `AuthSignInError`. `AuthStates` intentionally omits its explanatory headings and placement placeholder; its actual banner order, copy and surfaces are captured.

Browser probe command:

```sh
PLAYWRIGHT_BROWSERS_PATH=/private/tmp/chronoflow-5a-browsers node - <<'JS'
const { chromium } = require('/private/tmp/chronoflow-5a-browser/node_modules/playwright');
(async () => { const browser = await chromium.launch({headless:true}); const page = await browser.newPage({viewport:{width:375,height:812}}); await page.setContent('<html><body>probe</body></html>'); console.log(await page.title()); await browser.close(); })().catch(e=>{console.error(e.message);process.exit(1)});
JS
```

Fatal browser output:

```text
browserType.launch: Target page, context or browser has been closed
[pid=67544][err] [1118/132034.702794:FATAL:base/apple/mach_port_rendezvous_mac.cc:159] Check failed: kr == KERN_SUCCESS. bootstrap_check_in org.chromium.Chromium.MachPortRendezvousServer.67544: Permission denied (1100)
```

## Brief integration patches

- Resolve the real route-to-screen switch in `CfRoot`, leaving `RootContent` unchanged.
- Keep the `AppGraphKey` factory in `:app`, supply creation extras at that boundary, and retain feature-to-core dependency direction.
- Replace the variant Welcome property with a navigator-taking provider, owned lazily per graph; keep the debug guest seam.
- Acknowledge the existing app-to-auth dependency and defer the suspend public source through `LazyPublicDataSource` without changing the earlier source or model APIs.
- Port all **24** failures from the pinned form suite (the original brief said 22).
- Update the forced-sign-out structural tag in `LaunchGateRoutingTest.currentUnauthorizedRoutesToSignInAndErasesAccountFiles` after replacing the Sign in placeholder; retain its earlier routing/erase assertions.

`CfTypeScale.authBrand/authHeading` exist; the screen uses `CfTheme.type.style(...)` because they are not named `CfType` convenience properties. Existing SessionController/readiness fixtures were already exported by `:core:sync` test fixtures, so no fake move was needed.

## Open questions and defaults

The ten brief questions are recorded as **OQ-A182…OQ-A191** in `plan.md`, in the same order as OQ-5a-1…10. They retain the brief's defaults:

1. Static locked banner, no Sign in countdown; retain retry seconds.
2. Shared `LockoutWait` in `:core:model`.
3. Live region plus bringing the banner into view replaces an inaccessible TalkBack focus API.
4. Native targets/fields with the board's button visual.
5. Activity-scoped adapter for now; entry-scoped stores can be added by 5c/6a when needed.
6. Loading description reuses the button label.
7. Explicit auth root routing, with no predictive cross-screen preview for now.
8. Accept provider-bypassed feedback until the next keystroke; provider check open on this emulator.
9. Offline and field errors reuse approved surfaces and are captured as adaptations.
10. Persistent announcement slot is a TalkBack stop; spoken check open on this emulator.

No additional product question or Android-only copy row was introduced.

## Signed implementation commits

```text
fbc9b37 docs(phases): refresh the 5a brief against 4e
3ed489b feat(model): port lockout wait and authentication method
aaff40c feat(auth): port the auth form model and failure mapping
63d03e3 feat(auth): port auth navigation and layout math
90b6c12 feat(designsystem): add sign-in glyphs and a field input transformation
2481971 feat(auth): port the sign-in model
ebb5f9c feat(auth): add the auth shell
29c6592 feat(auth): add the sign-in screen
7649138 docs(phases): refresh the 5a brief against 4e
ff6f9c0 feat(app): route sign-in and hand over to the session gate
5209109 test(auth): guard sign-in states and capture the boards
5bc9d0f test(app): sign in the demo account on the emulator
1772484 docs(phases): refresh the 5a brief against 4e
6e7031a test(app): assert sign-in fields after forced sign-out
68a4798 perf(konsist): memoize route ancestry checks
9353fb5 test(icons): include the sign-in glyphs in the manifest count
7845c2b test(auth): keep announcement checks structural
fe8389e docs(auth): record sign-in decisions and acceptance
5410b29 build(scripts): use the full keyboard in functional tests
```

The follow-up results are recorded by `docs(auth): record the passing follow-up checks`.


## Two-factor (Phase 5b, 2026-11-18)

Each password challenge creates a fresh process-memory `TwoFactorModel` owned by `SignInModel`. Its code is ASCII-filtered before truncation to six digits; recovery input is sent as typed. There is no auto-focus or six-digit auto-submit. Toggle clears proof and feedback. Every attempt clears the proof; explicit back cancels the request and clears the challenge. Switching apps and activity recreation keep the challenge/code in memory, while process death loses them. Nothing secret is a route argument or saveable field.

Successful proof acceptance calls the same private `SignInModel.accept` as password/passkey sign-in, with `AuthenticationMethod.TwoFactor`. Acceptance and challenge disposal complete in the protected block if the caller is cancelled after the session commits. The app gate remains verify → terms → walkthrough → Today. There is no password autofill commit on this path because the password has already been cleared.

The root entry and ViewModel factory live in `:app`, in `CfRoot` and `auth/TwoFactorViewModelFactory`. The activity-scoped ViewModel follows the current model rather than retaining visit state. Field initialization and synchronization read the authoritative in-memory model: the combined UI flow can lag behind an edit during recreation. The real activity recreation regression caught this race and passes after that correction. Root transitions remain disabled.

### Board adaptations

| Board (`DC/AuthTwoFactor.dc.html`) | Build instead | Why |
|---|---|---|
| A `pattern-dots` layer on the sky | Plain sky, no dots | iOS "No PatternDots" (`I/plan.md:479`), already in 5a's `AuthScreen` |
| The code field prefilled with `482913`, and the error always drawn (l.55) | The field starts empty and is cleared after every attempt. A field error therefore shows an **empty** field with the error. | `TwoFactorModel.swift:56–59` |
| "That code didn't work. Codes change every 30 seconds…" as the code error | Only for the `code` field when the server key is "The authentication code was not accepted. Try again."; other keys show their own copy | fix round 4, `AuthFormModel.swift:74–79`, 5a's `AuthFailureMapping` |
| Verify is an `<a>` 40 px high (`h-10`); field 36 px; back 44 px | `CfButton` Primary `Lg` (40 dp visual, ≥ 48 dp target), field box ≥ 48 dp, back target 48 dp | 5a OQ-5a-4 |
| Recovery placeholder `xxxx-xxxx` (l.58) | A placeholder only, never a value, from the `xxxx-xxxx` resource (not translated) | fix round 4, `I/plan.md:628` |
| No footer | No footer | `TwoFactorView.swift:60–62` (`EmptyView()`) |

The card orders banner, proof field, Primary Lg Verify, Ghost Lg toggle. Both modes reuse generated light/dark tokens, catalog en/nl copy, the existing failure-feedback/reveal code and one polite announcement per failure. Field errors are structural semantics, and hints disappear while an error is shown. There is no new Android-only string: b1 belongs to Set2FA in Phase 10c; `android_two_factor_intro_b1` is already generated in both languages and remains unused here.

### Autofill contract

Both hints shipped as `TwoFactorFieldSpec.CodeContentType = ContentType.SmsOtpCode + ContentType("2faAppOTPCode")`, using the public constructor and `plus` (OQ-A194, orchestrator follow-up). Recovery exposes no content type. This uses the public [Compose ContentType API](https://developer.android.com/reference/kotlin/androidx/compose/ui/autofill/package-summary).

The single probe on the resolved Compose version returned `equality=false`; public descriptions were opaque `AndroidContentType@…` strings without hints. The final tests therefore check the shared production value's identity/wiring, keyboard traits and recovery omission. They use no internal accessor, visibility suppression or reflection; the reason for omitting a hint-level assertion is in `TwoFactorFieldSpecTest`'s KDoc. The temporary probe is removed; its output is retained in `build/phase5b-validation/autofill-probe.xml`.

### Swift test port map

All references below are read from iOS at `f391a1fa`. Coroutine/model behavior stays on the JVM. Parameter rows become independent named methods/cases. In this table `{…}` expands the suffixes on the original Swift method name.

| Swift suite / method | Kotlin suite / method |
|---|---|
| `TwoFactorModelTests.emptyProofShowsOnlyTheActiveFieldError` | `feature:auth:TwoFactorModelTests#emptyProofShowsOnlyTheActiveFieldError{Code,Recovery}` |
| `TwoFactorModelTests.latestCodeAcceptsThroughPasswordSessionPath` | `feature:auth:TwoFactorModelTests#latestCodeAcceptsThroughPasswordSessionPath{Verified,Unverified}` |
| `TwoFactorModelTests.numericInputFiltersPastesAndAutoFillWithoutSubmitting` | `feature:auth:TwoFactorModelTests#numericInputFiltersPastesAndAutoFillWithoutSubmitting` |
| `TwoFactorModelTests.recoveryIsSingleUseAndModeToggleClearsFeedback` | `feature:auth:TwoFactorModelTests#recoveryIsSingleUseAndModeToggleClearsFeedback` |
| `TwoFactorModelTests.expiryAndFiveFailuresKeepTheReturnedFieldState` | `feature:auth:TwoFactorModelTests#expiryAndFiveFailuresKeepTheReturnedFieldState{Expired,Exhausted}` |
| `TwoFactorModelTests.throttleServerOfflineAndHiddenFieldsUseKeptDefaults` | `feature:auth:TwoFactorModelTests#throttleServerOfflineAndHiddenFieldsUseKeptDefaults{Throttle,Server,Offline,Challenge,HiddenProof}` |
| `TwoFactorModelTests.doubleSubmitBusyEditsToggleAndBackDiscardLateSuccess` | `feature:auth:TwoFactorModelTests#doubleSubmitBusyEditsToggleAndBackDiscardLateSuccess` |
| `TwoFactorModelTests.taskCancellationClearsCodeWithoutPresentationOrAcceptance` | `feature:auth:TwoFactorModelTests#taskCancellationClearsCodeWithoutPresentationOrAcceptance` |
| `TwoFactorModelTests.missingChallengeReturnsToSignInAndNewNavigationCannotRestoreSecrets` | `feature:auth:TwoFactorModelTests#missingChallengeReturnsToSignInAndNewNavigationCannotRestoreSecrets` |
| `TwoFactorModelTests.sessionStorageFailureRemainsTypedAndShowsTheServerBanner` | `feature:auth:TwoFactorModelTests#sessionStorageFailureRemainsTypedAndShowsTheServerBanner{CredentialsUnavailable,DatabaseUnavailable,CredentialWrite,InvalidResult,Superseded}` |
| `TwoFactorModelTests.backendMessagesLocalizeOnlyUnderTheDisplayedModeAndAnnouncementsAreConsumedOnce` | `feature:auth:TwoFactorModelTests#backendMessagesLocalizeOnlyUnderTheDisplayedModeAndAnnouncementsAreConsumedOnce{CodeMissing,CodeRejected,RecoveryMissing,RecoveryRejected}` |
| `TwoFactorModelTests.onlySessionAcceptancePersistsATokenAndNeverTheChallengeOrProof` | `app:TwoFactorModelRoutingTests#onlySessionAcceptancePersistsATokenAndNeverTheChallengeOrProof` (debug JVM test, real controller, capturing stores) |
| `SignInModelTests.committedSessionUsesStoredGateWithAndWithoutCancellation` two-factor iteration | Same method in `app:SignInModelRoutingTests`; 24 gate × cancellation × authentication-method cases total |
| `PasskeySignInTests.mockPasskeyAcceptsVerifiedUnverifiedAndTwoFactorAccountsDirectly` | `feature:auth:PasskeySignInTests#mockPasskeyAcceptsVerifiedUnverifiedAndTwoFactorAccountsDirectly{Verified,Unverified,TwoFactor}` |
| `PasskeySignInTests.canceledPromptIsSilentAndRetryAlwaysGetsFreshOptions` | `feature:auth:PasskeySignInTests#canceledPromptIsSilentAndRetryAlwaysGetsFreshOptions` |
| `PasskeySignInTests.consumedExpiredCredentialThrottleServerAndOfflineFailuresRetryWithoutSession` | `feature:auth:PasskeySignInTests#consumedExpiredCredentialThrottleServerAndOfflineFailuresRetryWithoutSession` |
| `PasskeySignInTests.failedCredentialConsumesChallengeAndRetryFetchesAnother` | `feature:auth:PasskeySignInTests#failedCredentialConsumesChallengeAndRetryFetchesAnother` |
| `PasskeySignInTests.taskCancellationAfterAssertionNeverAcceptsOrCallsLogin` | `feature:auth:PasskeySignInTests#taskCancellationAfterAssertionNeverAcceptsOrCallsLogin` |
| `PasskeyEncodingTests.binaryBytesRoundTripWithoutUTF8OrPaddingAndAllDTOFieldsUseBase64URL` | Same Kotlin suite/method; invalid-encoding inputs additionally split into `invalidEncoding…` methods |
| `PasskeyEncodingTests.passkeyFieldFailuresUseOnlyDesignedBannerAndNeverLeakServerCopy` | Same name with `{Credential,Challenge}` suffixes |
| `TwoFactorControlTests.testDisplayedCodeRejectsOverflowAndNonDigitsEvenWhenTheModelDoesNotChange` | `feature:auth:TwoFactorControlTests#displayedCodeRejectsOverflowAndNonDigitsEvenWhenTheModelDoesNotChange` (Robolectric, structure/input values only) |
| `TwoFactorControlTests.testNativeInputTraitsAndModeToggleDiscardTheOldKeyboardConfiguration` | `feature:auth:TwoFactorControlTests#nativeInputTraitsAndModeToggleDiscardTheOldKeyboardConfiguration` (Robolectric, structure/input values only) |
| `TwoFactorControlTests.testRecoveryPlaceholderSurvivesErrorsAndClearsWhenReturningToCode` | `feature:auth:TwoFactorControlTests#recoveryPlaceholderSurvivesErrorsAndClearsWhenReturningToCode` (Robolectric, structure/input values only) |
| `TwoFactorControlTests.testFailedAttemptsRefocusTheEnabledFieldWithoutEditingAwayFeedback` | `feature:auth:TwoFactorControlTests#failedAttemptsRefocusTheEnabledFieldWithoutEditingAwayFeedback{Code,Recovery}` (Robolectric, structure/input values only) |
| `TwoFactorControlTests.testBothDisplayedProofsFollowModelEditsAndAttemptClearingWithoutErasingErrors` | `feature:auth:TwoFactorControlTests#bothDisplayedProofsFollowModelEditsAndAttemptClearingWithoutErasingErrors{Code,Recovery}` (Robolectric, structure/input values only) |
| `TwoFactorControlTests.testVerifyKeepsItsLabelAndSizeWhileBusyInBothLanguages` | `feature:auth:TwoFactorControlTests#verifyKeepsItsLabelAndSizeWhileBusyInBothLanguages{En,Nl}` (Robolectric, structure/input values only) |
| `TwoFactorTests` number-pad journey | `app:TwoFactorJourneyTest#numberPadDoesNotAutoSubmitAndVerifyCompletesTheMockSession` |

Backend en/nl assertions are in `TwoFactorCopyTest`, with one language/message per parameter case. Android additions cover the current-model ViewModel behavior, unchanged-proof feedback/redaction, code/recovery field specs, saved-state scanning/restoration, root routing/back/reentry/recreation, Credential Manager exception mapping/anchor lifetime, parsed request/response JSON and the shipped capability gate. Mock-backed persistence tests are in `app/src/testDebug`; the common gate/cancellation tests keep their existing `ScriptedSource` seam so release tests compile without a mock dependency.

`testBothModesKeepInteractiveKeyboardDismissalAndVerticalBouncing` is not ported: Android uses system back for IME dismissal (research 05 OQ A11). `testRootCapMakesAX5EqualXXLarge` is covered through the existing `ConfigurationPolicyTest.fontScaleTwoIsCappedAtOnePointThree` root policy and the capped `largeFont` profile guards, rather than a duplicated UIKit pixel-equality test. The iOS `-cf.twoFactorServerFailure` flag is not ported; JVM mapping and Robolectric banner tests cover that state. An expired-challenge screen has no board; the returned field state is retained. Native provider autofill and spoken TalkBack checks remain manual; none is inferred from semantics or the keyboard checks.

### Captures and comparison

The original single `make captures ONLY=AuthTwoFactor` invocation wrote these 18 PNGs under `build/captures/` at 375 × 812 dp, density 3 and zero insets:

- `AuthTwoFactor_code-attempts_en_dark.png`
- `AuthTwoFactor_code-attempts_en_light.png`
- `AuthTwoFactor_code-fieldError_en_dark.png`
- `AuthTwoFactor_code-fieldError_en_light.png`
- `AuthTwoFactor_code-fieldError_nl_light.png`
- `AuthTwoFactor_code-none_en_dark.png`
- `AuthTwoFactor_code-none_en_light.png`
- `AuthTwoFactor_code-server_en_dark.png`
- `AuthTwoFactor_code-server_en_light.png`
- `AuthTwoFactor_recovery-attempts_en_dark.png`
- `AuthTwoFactor_recovery-attempts_en_light.png`
- `AuthTwoFactor_recovery-fieldError_en_dark.png`
- `AuthTwoFactor_recovery-fieldError_en_light.png`
- `AuthTwoFactor_recovery-none_en_dark.png`
- `AuthTwoFactor_recovery-none_en_light.png`
- `AuthTwoFactor_recovery-none_nl_light.png`
- `AuthTwoFactor_recovery-server_en_dark.png`
- `AuthTwoFactor_recovery-server_en_light.png`

The 38 independent layout cases are eight states × small/nl, pixel8/en, foldInner/en and largeFont/nl in light; code/recovery attempts on landscapeSideNav/en/light; and four pixel8/en/dark states. They guard content, text, ink, targets, headings, placeholders and wide card centering/width. All pass with no timing misses.

Native captures were inspected against `AuthTwoFactor`'s source anatomy and the Two-factor banner declarations in `AuthStates`: empty proof fields, placeholder-only recovery, banner-first placement, code/recovery errors, Primary/Ghost actions, plain sky, both themes and Dutch wrapping follow the settled table above. The larger native targets and font wrapping remain the documented adaptations. No stored screenshot references were added. A browser-rendered side-by-side board comparison remains open: the copied canvas references an absent `support.js`, and the earlier browser probe could not launch in the sandbox. No new board render or final reviewer acceptance is claimed.

### Verification record

The three requested emulator classes each ran once in this authorized follow-up, under `Scripts/run-tier.sh`'s device lock on pixel / API 36 / debug with functional animator settings. The seed performs the password challenge; the two-factor tests do not type credentials. The Sign-in keyboard test uses the extracted `AuthKeyboardAssertions` with its existing ≥100 dp IME precondition, stable-bounds check and status-bar/keyboard geometry assertions unchanged.

| Selection | Result | Wall time |
|---|---|---|
| Public `ContentType` equality/description probe (temporary method) | Identity equality; descriptions omit hints; probe removed | 4.22 s |
| `make targeted ONLY="feature:auth:TwoFactorFieldSpecTest feature:auth:TwoFactorControlTests#nativeInputTraitsAndModeToggleDiscardTheOldKeyboardConfiguration"` | Seven passed using public shared-value wiring | 12.79 s |
| `make targeted ONLY="app:SignInKeyboardTest"` | Passed; test 1.450 s | 10.45 s |
| `make targeted ONLY="app:TwoFactorJourneyTest"` | Passed; test 1.776 s; no auto-submit; session gate reached; StrictMode IO counter 0 | 6.89 s |
| `make targeted ONLY="app:TwoFactorKeyboardTest"` | Both passed; code 1.432 s, recovery 0.885 s | 7.31 s |

Follow-up logs, XML probe evidence and per-run timings are retained under `build/phase5b-validation/`. The final `make quick` invocation follows the docs commit; its full output is `build/phase5b-validation/final-quick.log`, with the normal tier timing/result files under `build/test-results/`. The orchestrator's end report records its exit status and wall time.

Earlier implementation validation: 60 model/copy/sign-in cases passed in 16.29 s; field/control/screen/saved-state cases in 16.87 s; ViewModel cases in 7.97 s; 38 layout guards in 16.57 s; 18 captures in 13.13 s; passkey flow/client cases in 12.84 s; graph/seed/service/hidden-capability checks in 27.97 s; and the release capability/routing checks in 7.06 s. The first route/storage selection passed 43/44 cases in 20.45 s, exposing the recreation race; its corrected method passed in 5.36 s, with control/saved-state regressions in 6.52 s. The saved-state guard was proven with a temporary `rememberSaveable` mutation (expected failure in 6.69 s); restoring memory-only state passed in 4.52 s.

The initial Task 10 stop after two autofill assertion fixes is retained in the history. No production hint was removed. The orchestrator authorized the shared-value resolution: internal `contentHints` access is removed from the two-factor tests, and no reflection or visibility bypass was introduced. The phase adds no new copy. OQ-5b-1…11 are recorded as OQ-A193…OQ-A203; OQ-A194 records that both OTP hints shipped. Passkey server/device prerequisites and the capability flip procedure are in [Passkey sign-in](passkeys.md). `SERVER_READY` remains false.

`make check`, perf/timeline measurements and profile regeneration were not run, as instructed. No Phase 5b performance-budget measurement is requested; only the release dependency size delta is recorded in [Startup performance](perf-startup.md#release-size).

## Password reset and App Links (Phase 5c, 2026-11-18)

Forgot, Reset, Expired and Done port the pinned iOS models and views. A successful reset returns HTTP 204 and creates no session. Continue returns to Sign in. The [App Links setup](app-links.md) documents the release manifest, server association and actual emulator evidence. Verify links reach a `VerifyLinkReceiver` seam; Phase 5d supplies the verification coordinator. Phase 5c does not call the verification endpoint.

### State and request tables

| Forgot / Expired state | Presentation and next action |
|---|---|
| Forgot, unsent | Empty editable email; Send reset link; back to Sign in |
| Expired, unsent | Problem heading glyph; link email prefilled and editable; Send a new link sends directly |
| Busy | Whole screen disabled, one pending-button spinner; tap and IME share one request lock |
| Displayed field failure | Remain on form, localized email field error; first invalid field receives focus |
| Offline / throttle / other failure | Remain on form with the context-specific banner; no client validation or countdown |
| Sent, including unknown recipient | Identical notice interpolating the submitted email; Open email app and Back to sign in; further submit is ignored |
| Expired after resend | Switch in place to the Forgot sent heading/subtitle and remove the problem glyph |
| Cancel / back | Cancel the request, ignore late completion; back resets auth navigation and returns to Sign in |

Editing after success cannot alter `sentEmail`. Mock mail looks up that submitted recipient, normalized only for inbox matching, and opens the latest mock reset link in the app. It is available only with debug mock sources. A real email launch uses the native `ACTION_MAIN` / `CATEGORY_APP_EMAIL` selector; a missing handler leaves the sent notice unchanged.

| Reset state / event | Result |
|---|---|
| Valid in-memory link | Form with New password, full policy hint, confirmation and Save new password |
| Missing/invalid link | Expired recovery form carrying the link email |
| Submit | Snapshot both secrets into the public request; duplicate submit ignored |
| HTTP 204 | Done, check glyph, honest signed-out copy and Continue → Sign in |
| Invalid/expired-token validation key | Expired recovery, regardless of the field containing that key |
| Other supported displayed-field validation | Remain on Form with localized field feedback |
| Hidden/unknown validation or other failure | Designed banner, with supported displayed-field errors retained |
| Every completed, failed or cancelled attempt | Clear both `SecretField`s in `finally` and release the busy lock |
| System back / Continue | Cancel the current model and return to Sign in; form and Done have no drawn back |

Submit paths rethrow cancellation before mapping `SessionFailure`, `DataFailure`, `DeviceIdentityFailure`, `StoreFailure`, `IOException`, `GeneralSecurityException` and `RuntimeException` to the designed feedback. Typed server keys resolve through the generated resources; no raw server message reaches the screen.

### Ownership, secrets and launch hand-off

| Value / work | Owner and lifetime |
|---|---|
| Forgot model and submitted email | `AuthServices.forgotModel`, survives activity recreation, cancelled and released on route exit |
| Reset token, link and recovery model | `AuthServices.resetModel`; this is the in-memory holder from OQ-A175, with no separate holder class |
| Route | `Reset(email)` only; never the token |
| Password and confirmation | Composition-owned `remember { TextFieldState() }`; discarded on recreation and never saved |
| Request | Composition-owned coroutine scope/job; disposal cancels it and the model ignores late completion |
| Screen | Stateless `XScreen(state, onEvent)`; route adapters collect model state and own fields/focus |
| Network, mock inbox, disk and Keystore | Existing source/store abstractions with injected IO dispatchers; no launch-time IO added |

Opening another reset cancels the previous reset and Forgot models. The route observer reads the navigator's current route so a queued earlier Sign in emission cannot cancel a newly opened Reset model. There is no activity-scoped Forgot/Reset ViewModel retaining a cancelled model. `ResetPasswordLink` and `SignedVerificationInput` redact secrets in `toString()`. Saved-bundle tests inspect the actual activity registry and prove passwords/token are absent; a recreation retains the model but loses both typed passwords.

`PendingLaunchIntents` captures fresh VIEW links in order, ignoring restored activities and history launches. Repeated equal URLs move to the end; the latest auth link wins. Cold `onCreate` captures then clears launch-intent data. Warm `onNewIntent` captures the incoming parameter and does not call `setIntent`: the original stored launch intent stays unchanged. Non-auth links remain with the existing `LaunchIntentHandler`, and reminder guards retain their earlier behaviour.

The dispatcher attaches immediately after `LaunchSessionBridge` first navigates the launch route, matching iOS root mount beneath the overlay. `finishLaunch` is an idempotent backstop. The ending remains committed; Welcome uses the existing moon fallback target if the link replaces it. Dispatcher construction touches only the in-memory pending buffer. It resolves auth services only when delivering after attachment.

`GuestResetLinkRouter` opens immediately only when signed out. Unknown or signed-in presence retains the latest pending reset until sign-out, then opens once. Both the session gate observer and teardown can report the same sign-out; `AppGraph.onSignedOut` preserves the newly opened Reset route, and teardown navigation runs on injected main. The real-session test covers this order without changing earlier route behaviour. Root swaps retain no transition.

### Board comparison and settled adaptations

The one capture invocation was:

```sh
make captures ONLY=AuthForgot,AuthReset,AuthResetDone,AuthResetExpired,auth/ForgotSent,auth/ExpiredSent,auth/ResetFieldErrors
```

It wrote **11 PNGs in 11.83 s**: eight English light/dark board states plus the sent, expired-sent and field-error states under `build/captures/`. Captures were visually reviewed against the local HTML's geometry, type and surface declarations and the settled iOS overrides. Forgot sent and Expired sent are byte-identical, as intended. Browser-rendered side-by-side review remains open; no browser comparison is claimed.

| Board | Recorded difference / disposition |
|---|---|
| All four | Settled: plain generated sky, no dots; native 48 dp targets around the board's 40 dp primary-button visual; generated colors, spacing and typography in both themes |
| `AuthForgot` | Settled: starts empty rather than `rick@example.com`; notice uses the submitted recipient and the catalog's “Check your inbox.” including its period; throttled banner uses “Too many requests.” and the request-specific body |
| `AuthReset` | Settled: honest “Sign in with your new password once it's saved.” subtitle; “Save new password”; full 12-character/upper/lower/number/symbol/data-leak policy replaces the old 10-character hint; no drawn back |
| `AuthReset` | Native adaptation: long policy wraps and increases card height; fields remain at least 48 dp and scroll rather than shrinking type |
| `AuthResetDone` | Settled: “Password updated. Sign in with your new password.”; other devices are signed out; Continue → Sign in, no session; check glyph; no drawn back |
| `AuthResetExpired` | Settled: editable link email; resend happens directly and switches to Forgot sent in place, including its heading/subtitle and no glyph |
| `AuthResetExpired`, `AuthResetDone` | Settled: 48 dp minimum recovery/Continue buttons, 48 dp heading circle with 24 dp glyph and 8 dp below |
| All four | Native adaptation: 48 dp opposite header slots replace 44 dp board slots, keeping the brand centred; intrinsic Android font wrapping changes line breaks/card height; standard underline is inherited from the auth shell |

The guards cover `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` and Dutch with the root font cap. No copy or approval row was added; generated English/Dutch resources and `tools/l10n/android-only.json` are unchanged.

### Swift test port map

All Swift sources below are read at `f391a1fa`. Same-named Kotlin cases preserve the pinned assertions; Android-only cases add failure, secret, recreation and routing guards.

| Swift suite / method | Kotlin selection |
|---|---|
| `AppCoreTests/AuthLinkTests`: `exactSignedVerificationShapeAndMalformedInputs`, `exactResetURLDecodesOnceAndKeepsPlusPercentAndUnicode` | `core:model:AuthLinkTests`, same methods; plus `PercentDecodingTest` and `AuthLinkUrlsTest` |
| `ForgotPasswordModelTests`: `mockMailDeliveryUsesTheSubmittedRecipientEvenAfterLaterEdits`, `existingAndUnknownAddressesHaveIdenticalSuccessWithoutASession` | `feature:auth:ForgotPasswordModelTests`, same methods |
| `fieldServerThrottleOfflineAndHiddenFailuresKeepTheFormAndEmail`, `serverValidatesBlankAndMalformedEmailWithoutClientValidation` | Same suite/names, one uniquely named parameter case per failure/input |
| `busyLockIgnoresEditsAndDuplicateTapOrReturnAndCapturesSubmittedEmail` | Same suite/name, gated request |
| `cancellationAndBackIgnoreLateCompletion` argument rows | Same suite, `cancellationAndBackIgnoreLateCompletionCancel` and `cancellationAndBackIgnoreLateCompletionBack` |
| `emailAppMissingHandlerPreservesTheSentNoticeAndBackReturnsToSignIn`, `expiredResetCanPrefillANewFormWithoutReusingSentState` | Same suite/names |
| `noticeUsesTypedInterpolationInEnglishAndDutch` | `feature:auth:ForgotPasswordCopyTest`, same method; resource-only assertions |
| `ResetPasswordModelTests`: `mockMailResetClearsSecretsRevokesSessionsAndCannotBeReused`, `missingInvalidAndServerFieldMessagesUseDesignedStates`, `cancelAndDuplicateSubmissionIgnoreLateCompletion` | `feature:auth:ResetPasswordModelTests`, same methods |
| `GuestResetLinkRouterTests.swift` extension of `ResetPasswordModelTests`: `unresolvedLaunchWaitsForSignedOutRestore`, `guestOpensImmediatelyAndDoesNotReplayOnLaterSessionUpdates` | File-based Kotlin suite `feature:auth:GuestResetLinkRouterTests`, same methods |
| `activeSessionDefersLatestLinkUntilSignOut` argument rows | Same router suite, `activeSessionDefersLatestLinkUntilSignOutUnverified` / `…Verified`; real teardown additionally covered in `app:LinkRecreationTest` |
| `AuthUniversalLinkTests`: `testSignedColdAndWarmBrowsingActivitiesReachTheSameRouter`, `testColdCaptureAndWarmActivityPreserveResetInputWithoutAScheme` | `app:AuthUniversalLinkTests`, same methods |

Structural Robolectric suites cover shared glyph/email launcher/policy, both screens, guards and the manifest filter. Real-graph `ForgotRecreationTest` and `ResetRecreationTest` live in `:app`, preserving the feature-to-core dependency direction. JVM `ForgotRoutingTest`, `OpenResetTest`, intent policy/dispatcher/routing and seed tests cover behaviour without Compose copy assertions. A temporary plus-to-space decoder mutation failed the literal-plus parser assertion, then passed after restoration.

### Integration patches and verification

The preflight refresh is commit `a1bbafd`. It records the landed `DataFailure` status parameters, existing lazy public source, transitive test-model dependency, source-taking mock lookup, current `CfRoot` route switch and app-owned recreation fixtures. Screens follow the stronger stateless-screen rule. Reminder fresh-launch behaviour stays intact. The sign-out hand-off correction and the subsequent harness/intent follow-ups are recorded in the brief.

The first Task 8 run hung because `ActivityScenario` matches activities with `Intent.filterEquals`, while cold-link scrubbing deliberately changes the URI. The orchestrator diagnosed this; link-launched Robolectric tests now use `Robolectric.buildActivity`/`ActivityController`, retaining all assertions. Non-link launches still use ActivityScenario. A later emulator teardown exposed the warm-intent variant of the same matching issue; the production fix preserves the original launch intent in `onNewIntent` (commit `ecfc7dd`). The added test asserts intent identity, no data, exactly one capture/navigation and no replay after recreation. Neither fix weakens the cold scrub.

| Focused selection / command | Result | Wall time |
|---|---|---|
| Parser / percent decoding / URL helper | 24 pass; restored negative-control method passes | 6.61 s; restored 1.67 s |
| Heading glyph / email launcher / password policy | 12 pass; generated token golden passes | 5.81 s; golden Gradle 2 s |
| Forgot model/copy/navigation + app routing/ownership | 38 pass; busy-lock method also rerun after formatting | approximately 10.77 s; busy method tier 2.71 s |
| Forgot screen/guards + activity recreation/back | 21 pass; 14 guard cases, each below 5 s | approximately 14.12 s |
| Reset model + app routing/ownership | 28 pass | approximately 7.24 s |
| Reset screen/policy/guards + corrected recreation fixture | 19 feature cases and three recreation methods pass; each guard below 5 s | feature Gradle 8 s; corrected app tier 7.25 s |
| `GuestResetLinkRouterTests` | Five pass | 2.91 s |
| Manifest rule suites | 22 pass, including malformed-filter controls | Gradle 14 s |
| `LinkRecreationTest` affected cold / history / real-sign-out methods | Pass, test durations 2.524 / 2.328 / 2.666 s | tiers 9.33 / 4.45 / 4.76 s |
| Task 8 app selection + Forgot/Reset recreation regressions | 54 pass, zero budget misses | 8.16 s |
| `LinkRecreationTest#warmViewKeepsTheLaunchIntentAndCapturesTheLinkOnce` | Pass, test 3.819 s | 10.98 s |
| `PasswordResetLinkJourneyTest` | Pass on pixel/API 36/debug; test 3.435 s, StrictMode IO counter zero, signed out with no session blob | 9.43 s |
| `PendingLaunchIntentTest`, app pending/launch/reseed/auth ownership, release readiness | All pass | per-group Gradle 4 / 6 / 17 s |
| `SignInGuardsTest` | 17 pass, slowest 3.362 s; zero budget misses | 14.22 s |
| `./gradlew --console=plain :app:lintDebug :app:lintRelease :app:testReleaseUnitTest -Pcf.tier=quick :app:verifyReleaseManifest :app:analyzeReleaseR8Config` | Pass; 141 release unit tests, no failures; both Lint variants clean; exact manifest and R8 analysis pass | 96.05 s |
| `make generators-check` | Pass; committed generated outputs match, en/nl parity and generator tests pass | 15.39 s |

Logs, XML and available per-run timing JSON are retained under `build/phase5c-validation/`. The earlier failed journey took 47.631 s in teardown (tier 68.38 s); the authorized production fix and single retry above supersede it. No additional emulator journey was run. OQ-5c-1…9 are recorded as OQ-A205…OQ-A213. `CF_AUTH_TEST_LINK` is listed in the seed spec and is debug/test-ID gated, memory-only, and absent from release.

The single final `make quick` follows the documentation commit; its exact command, exit status and wall time are retained in the validation directory and the final report. `make check`, performance tiers and profile regeneration are reserved for the orchestrator as instructed. Startup cold/warm TTID, launch first-frame/TTFD and launch-jank numbers remain unmeasured for this phase; no device performance claim is made. No dependency or native library was added.

## Sign up and email verification (Phase 5d, 2026-11-18)

`SignUpModel`, `VerifyEmailModel` and `VerificationLinkInbox` port iOS at `f391a1fa`. The coordinator ports the verification portion of `AuthSessionServices`; it implements the `VerifyLinkReceiver` handed over by 5c. Existing parser, dispatcher, routing, heading glyph, password policy and email launcher are reused. No duplicate shared-piece tests were added.

### Sign-up behaviour and ownership

| Event / state | Result |
|---|---|
| Enter Sign up | Empty name, email, password and confirmation; shared Show password switch off |
| Edit / submit | Server alone validates; four displayed fields; captured device ID, device name and selected en/nl language; no guest claim fields |
| Busy | One request; fields, switch, consent links, drawn back and footer disabled; system back still cancels |
| Email taken | Designed banner; other supported field messages remain; first invalid field receives focus |
| Other failure | Existing field/banner mapping; name and email retained; both passwords cleared |
| Successful register | Accept session, commit autofill, clear passwords and route through stored readiness; an unverified account reaches Verify |
| Drawn back / footer / system back | Cancel model/request and navigate once to Sign in |
| Policy link | Two native annotated spans; only the terms/privacy allow-listed HTTPS URLs open a Custom Tab, then ACTION_VIEW fallback; no handler leaves the screen unchanged |

The full submit catch list maps `SessionFailure`, `DataFailure`, `DeviceIdentityFailure`, `StoreFailure`, `IOException`, `GeneralSecurityException` and `RuntimeException` to feedback, after rethrowing cancellation. `NonCancellable` covers committed-session acceptance through routing and secret cleanup. Secrets are also cleared and the form unlocked in `finally`.

| State / work | Owner and lifetime |
|---|---|
| Name, email, visibility and form | Fresh `SignUpModel` in entry-scoped `SignUpViewModel`; retained on activity recreation; cleared with a removed entry |
| Password and confirmation | Route composition's plain remembered `TextFieldState`s; recreation loses both; never saved |
| Sign-up request | ViewModel child scope; disposal/back cancels; late completion cannot accept an uncommitted session |
| Verify model and signed input | Process coordinator's current-account model; child scope cancelled on disposal/replacement; no saved deadline or link input |
| Pending signed link | In-memory `VerificationLinkInbox`; consumed once for the matching account and epoch |
| IO | Device identity, source resolution, mock inbox, database, session storage and Keystore use injected IO dispatchers |

`RootContent` applies the saveable-state decorator followed by the entry ViewModel-store decorator through `rememberDecoratedNavEntries(entries, decorators)`. Saveable content keys retain route/revision identity, without secrets. Root swaps have no transition. The unchanged Sign-in tests still pass with entry scoping. SignUp and Verify both use the launch sky, including its system-bar appearance.

Standalone Robolectric activities rendering auth content outside `CfRoot` must provide `LocalSafeInsets`, LTR layout direction and the nullable timeline jank tagger. `SignUpEntryScopeTest` supplies zero insets and test dispatchers. Its earlier setup stall came from the missing inset provider; the corrected method passed in **2.389 s** (6.14 s tier wall), below the 5 s layout budget.

### Verify state machine and account isolation

Waiting has Open email app, Resend and Use a different email. Failed has the problem glyph, Try again and Send a new link, with both buttons visually at least 48 dp. A failed check changes Waiting to Failed; a failed resend retains its state. A malformed pending input selects Failed without sending a request. An invalid submitted link is dropped before the failure is presented, so Retry checks server status.

Activation refreshes status whenever the lifecycle enters STARTED. Ticks run only while STARTED; the wall-clock deadline includes background time. Remaining seconds round up and never go below zero. Only the countdown reads the changing seconds; ten test ticks recompose that node ten times and the button group zero times. The countdown has no live region. A failure announces once per attempt; repeating a failure creates a new announcement.

Resend requires a status (or Failed), no busy request and zero remaining seconds. The real API's first status returns zero, so Resend is initially enabled; the server's 429 starts the countdown. It is not pre-emptively disabled after registration. The generated countdown uses ASCII `m:ss` with `Locale.ROOT` in both languages. Verify uses the catalog's heading without a period and the catalog's Dutch interpolation.

Every asynchronous boundary rechecks the active account, exact email, epoch and request revision. Verified status is insufficient on its own: fetch identity must match and be verified; sync stops before the session controller confirms verification. Persistence must succeed before routing. Completion routes only while still on Verify, allowing the existing gate observer to win without a second navigation. `onVerify` likewise avoids recreating an already displayed Verify entry.

The inbox retains a guest link only across the first subsequent sign-in epoch. A foreign account, verified account, replacement epoch, sign-out or skipped/coalesced epoch clears it; malformed input cannot replace a matching pending link. Taking a link clears it. Synchronization rechecks presence/epoch after the account read. Preparing a model rechecks the session after suspending source resolution and reuses an active model for the same context.

Both drawn back and Use a different email explicitly discard/sign out, erase account files, invalidate the old mock token and reach Sign in. Disposal caused by SignedOut publication cannot interrupt teardown: discard completes in `NonCancellable`. A stale model cannot discard a replacement account. System back leaves the app with the session intact.

`AppGraph.auth.verification` is lazy and first touched at launch hand-off, a delivered link or a gate callback. It is not constructed in `Application.onCreate` or `MainActivity.onCreate`. A null Verify model draws only the plain sky. Release/API builds use the existing native email selector. Debug mock builds consume the last matching verification mail for the exact email and account ID; there is no inbox UI. Release readiness proves the mock mail implementation and composition counter are absent.

### Board captures and review status

The single capture invocation was:

```sh
make captures ONLY=AuthSignUp,AuthVerify,AuthVerifyFailed,auth/SignUpOffline,auth/VerifyWaiting,auth/VerifySignOutError
```

It passed in **15.19 s** and produced these **14 PNGs** in `build/captures/`:

| Surface | Capture names (all `.png`) |
|---|---|
| Sign up | `AuthSignUp_none_en_light`, `AuthSignUp_none_en_dark`, `AuthSignUp_taken_en_light`, `AuthSignUp_taken_en_dark`, `AuthSignUp_server_en_light`, `AuthSignUp_server_en_dark` |
| Verify | `AuthVerify_en_light`, `AuthVerify_en_dark`, `AuthVerify_server_en_light` |
| Verify failed | `AuthVerifyFailed_en_light`, `AuthVerifyFailed_en_dark` |
| Adaptations | `auth/SignUpOffline_en_light`, `auth/VerifyWaiting_en_light`, `auth/VerifySignOutError_en_light` |

Native renders were inspected against the board source declarations and settled iOS overrides. **Browser-rendered side-by-side comparison and final reviewer acceptance remain open.** No Opus comparison is claimed. These are the recorded differences to assess when that render is available:

| Board | Difference / disposition |
|---|---|
| All three | Settled: plain sky without dots; generated light/dark colors and typography; 48 dp header/action targets; standard native underline offset |
| `AuthSignUp` | Settled: real form starts empty; capture alone uses board samples; full 12-character policy replaces the short 10-character hint; two policy links and the native switch row replace the board's single link and eye button |
| `AuthSignUp` | Native adaptation: 48 dp fields and wrapped policy increase card height. At 375 × 812 the consent/footer and error-state submit are reached by scrolling; no type or control shrinks. Guards prove reachability, including Dutch/large font |
| `AuthVerify` | Settled: real account email/deadline; capture uses 42 seconds and disabled Resend. Normal waiting has no countdown at zero. The catalog's Dutch sentence takes precedence over board wording |
| `AuthVerifyFailed` | Settled: 48 dp problem disc, generated destructive glyph and 8 dp below it; both actions have a 48 dp visual minimum; no different-email action/countdown |
| Verify error states | Settled: check/resend share iOS Verify banner copy; sign-out error is the designed destructive line; offline is title-only; no invented loading screen |

The layout matrix has 20 Sign-up cases and 21 Verify cases, each independently timed, across `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`, English/Dutch and the specified dark states. Native inline links keep the paragraph's text-sized layout. Their guard checks Compose's expanded 48 dp touch bounds and annotated character regions for wrapped-link overlap; ordinary controls still require 48 dp layout. Positive and negative harness controls reject a reduced native target, ordinary 40 dp control and overlapping link ranges. Safe-area edges alone allow one physical pixel for fractional dp-to-pixel window rounding. Existing negative inset controls still fail for the real reason. Two additional board-viewport tests pin the brand row and both heading positions.

### Swift test port map

All Swift files are read at `f391a1fa`. Parameter rows have distinct Kotlin names; model logic uses fake clocks/repositories and `runTest(timeout = 60.seconds)`. Screen assertions use tags and structure; copy checks read resources directly.

| Swift suite / cases | Kotlin coverage |
|---|---|
| `SignUpModelTests.registrationUsesDeviceLanguageAndTheExistingSessionController`, `serverAloneValidatesAllFieldsAndTakenEmailUsesTheBanner` | Same suite/method names; password-policy arguments independently parameterized |
| `uiTestSeedReturnsTheExpectedRegistrationBanner` | Same prefix with `Online` / `Offline` methods |
| `failuresPreserveNameEmailAndClearPasswordsIncludingKeychainFailure`, `createdAccountAfterSessionFailureCanRecoverThroughSignIn` | Same prefixes with failure suffixes; all five session failures |
| `emailTakenPreservesOtherFieldErrorsInTheSameResponse`, `emailTakenKeepsBannerWhenOtherMessagesNeedTheFallback` | Same prefixes; three mixed-message methods and fallback case |
| `unchangedFieldCallbacksPreserveMixedFailureUntilAnEdit` | Name/email in model suite; password/confirmation in `SignUpFieldEditTest`; same prefix plus field suffix |
| `unchangedConfirmationPreservesServerFeedbackUntilTheValueChanges`, `oneToggleControlsBothPasswordsAndBusyCancellationStopsAcceptance` | Same names in field-edit/model suites; native toggle additionally checked by `SignUpVisibilityTest` |
| `VerifyEmailModelTests.signedMockMailPersistsOnceAndRoutesOnlyForMatchingAccountEmailEpoch`, `resendDeadlineSurvivesIdentityRefreshActivationAndBackgroundTime`, `activationAfterWebVerificationAndEmailAppReturnObserveTheServer` | Same suite/method names |
| `invalidExpiredForeignAndTamperedLinksUseFailedAndRetryChecksStatus`, `staleSessionOrEmailAndCanceledResponsesCannotPersistOrRoute`, `serverOfflineUnverifiedFailureDoesNotLoopAndBackExplicitlyDiscards` | Same prefixes with independent input/context/failure suffixes |
| `responseArrivingAfterEmailChangeCannotWriteAndStaleBackCannotDiscard`, `offlineResendShowsAndAnnouncesTheConnectionBanner`, `repeatedSignOutFailuresEachShowAndAnnounceTheConnectionBanner` | Same suite/method names |
| `signOutFailureIsAnnouncedOncePerAttemptAndReplacedByTheNextFailure`, `sessionFailuresAreVisibleOnCheckActivationResendAndSignOut` | Same prefixes; Check/Resend methods and uniquely parameterized session-failure cases |
| All seven `VerificationLinkInboxTests` methods | Same names; replacement argument split into SignedOut/SameAccount/OtherAccount methods |
| `SignUpControlTests.testSharedVisibilityPreservesValuesAutoFillAndNativeNameTraits`, `SignUpLayoutTests.testMixedFailureFocusesAndRevealsTheConfirmationMessage` | Same methods in `SignUpVisibilityTest` / `SignUpFailureFocusTest` |
| `SignUpSnapshotTests` ten states and `testAllStatesKeepConsentToggleAndFooterReachableAtXXLarge` | `SignUpGuardsTest`, same method name; board samples in `SignUpVerifyBoardCaptureTest` |
| `VerifyEmailControlTests.testWaitingCountdownAndFailedHaveNoInputsAndKeepLargeActions`, `VerifyEmailLayoutTests.testWaitingFailedAndErrorActionsFitTheLeanDeviceProfiles` | Same methods in `VerifyEmailControlTest` / `VerifyGuardsTest`; all ten snapshot states |
| `AccountRecoveryTests.secondAccountConsumesItsOwnMailAfterTheFirstAccountIsWiped`, `VerifyEmailTests.unverifiedDifferentEmailReturnsToSignIn`, `tamperedLinkOpensFailedScreen` | Same names in JVM `VerificationCoordinatorTest`; real graph teardown in `VerifyRoutingTest` |
| Auth native sign-up/verification journey | `SignUpVerifyJourneyTest.signUpVerifiesThroughTheMockLinkAndOpensTheGate`, the only emulator journey |

The three guest cases remain deferred to 11b: `guestSignupRequiresALiveClaimBeforeCreatingAnAccount`, `guestSignupDistinguishesARequestFailureFromAnAccountAwaitingAcceptance`, and `expiredGuestClaimShowsReviewAndClearsPasswords`.

Android additions cover committed-session cancellation, all checked submit failures, a newer link during a suspended check, coordinator epoch/dedup races, IME chaining, disabled controls, policy annotations/opener fallbacks, secret registry/recreation, entry scoping, lifecycle/countdown accessibility, and native-link harness negatives. A temporary mutation dropping the matching first guest link failed `guestLinkIsConsumedOnceByTheMatchingFirstSignIn`; restoring it passed before commit.

### Verification and outstanding acceptance

| Selection / command | Result | Wall time |
|---|---|---|
| `AuthConsentTextTest PolicyOpenerTest` | 10 passed | 35.84 s (first compilation) |
| `SignUpModelTests SignInModelTests` | 54 passed | 4.20 s |
| `VerifyEmailModelTests VerificationLinkInboxTests` | 65 passed | 4.06 s |
| Sign-up screen/ViewModel and unchanged Sign-in selection | 56 unique cases: initial 52 pass / four fixture assertions fail; four corrected cases pass | initial 7.93 s; corrected Gradle 6 s |
| `SignUpEntryScopeTest#removedEntryIsClearedAndARecreatedActivityKeepsItsCurrentEntry` | Pass after the user-diagnosed missing-provider fix; test 2.389 s | 6.14 s |
| `core:designsystem:LucideGlyphsTest` | Pass; generated eye and 35-glyph manifest | Gradle 3 s |
| Verify controls/copy/lifecycle/countdown/accessibility | Six passed, copy in en/nl | 5.82 s |
| `VerificationCoordinatorTest` | Nine passed | Gradle 2 s |
| Verify routing and unchanged 5c dispatcher/routing | Initial 14/15 pass; drawn-back method passes after waiting for activation and completed teardown; test 3.059 s | initial 14.27 s; corrected 5.35 s |
| `app:ReleaseReadinessTest` | 10 passed, including mock/counter absence | 8.15 s |
| `./gradlew --console=plain :app:verifyReleaseManifest` | Pass, allow-lists unchanged | Gradle 1 s |
| `SignUpGuardsTest` | 20 passed after native-inline-link/window-rounding harness corrections | Gradle 7 s |
| `NativeInlineLinkTargetTest` plus existing negative inset guard | Six passed | 5.18 s |
| `VerifyGuardsTest` | 21 passed with final shared guard | 6.73 s |
| `VerifyBoardGeometryTest` | Two passed; corrected the assertion to measure the brand row rather than its centred text | 6.65 s |
| `make captures ONLY=AuthSignUp,AuthVerify,AuthVerifyFailed,auth/SignUpOffline,auth/VerifyWaiting,auth/VerifySignOutError` | 14 captures, one invocation | 15.19 s |
| `make targeted ONLY="app:SignUpVerifyJourneyTest"` | Pixel/API 36/debug: pass; test **3.168 s**, Walkthrough reached, StrictMode IO counter **0** | **26.53 s**, including APK rebuild/install |
| `make quick` (initial invocation) | 2,935 passed / one stale Verify placeholder-tag assertion failed; zero budget violations; timing outputs left uncertified | 29.04 s |
| `app:LaunchGateRoutingTest#unverifiedLaunchesVerify app:RootContentTest app:RootContentConstructionTest app:RootRouteTransitionTest` | Seven passed; real Verify control/status replaces the placeholder assertion; all standalone root fixtures provide root locals | 6.83 s |
| `make quick` (one allowed stamp rerun) | All 2,936 assertions pass; **fails timing gate** on the three methods below | 43.64 s |
| Remaining quick substeps: build-logic / tooling / serial timing | 37 / 211 / six passed; run once after the failed quick gate | 4.12 / 18.34 / 2.76 s |

Every successful tier reports zero budget violations. The original entry-scope setup stalled before the user's diagnosed fix; no further test run hung. The real-graph back fixture now waits for initial status before tapping an enabled back control, then waits for teardown completion before checking disk removal. Formatting ran through `make format`. Kotlin's sandboxed daemon marker warning fell back successfully; it did not prevent compilation.

Validation logs and saved per-run timing JSON are in `build/phase5d-validation/`. The initial quick failure left its outputs without matching passed stamps. After the focused fix, the user's one allowed rerun passed every assertion but failed three existing methods' timing budgets. No further quick or method retry was run. The remaining quick substeps passed separately, with serial timing last. **Quick acceptance remains open**; thresholds were not changed. The failing command was `make quick` (exit 2 from Make, tier exit 3), with this exact gate output:

```text
run-tier: over budget: jvm net.chronoflow.konsist.HapticsRulesTest#noHapticsInLaunchRejectsPlantedViolation 1261 ms > 1000 ms
run-tier: over budget: jvm net.chronoflow.core.datamock.MockSeedRefreshTests#restoresTenDayOldSeedWithoutLosingUserStateCurrentAnchor 1402 ms > 1000 ms
run-tier: over budget: robolectric net.chronoflow.app.LaunchGateRoutingTest#pendingTermsLaunchesTerms 5147 ms > 5000 ms
make: *** [quick] Error 3
```

No `make check`, other emulator tier, perf/timeline benchmark or profile regeneration ran, as instructed. No performance-budget row applies to 5d; no performance number is claimed. The debug APK still packages only the existing allow-listed `libandroidx.graphics.path.so`; Browser added no native library. `make size` was not run.

The preflight refresh is `e1f605a`: `MockScenarioValues`, `RegisterInput.deviceID`, suspending source resolution, core `LaunchRoute` adapter, injected feature dispatchers, debug/perf source seam, main interface/debug implementation split, stateless screen adapters and existing shared geometry/resource names. The Task 4 brief records the landed navigation decorator/provider adaptation; Task 7 records native-link guard semantics and one-pixel safe-area rounding. OQ-5d-1…18 are **OQ-A214…OQ-A231**. A1 includes stable Browser 1.10.0. No visible copy was added; the Android-only approval table is unchanged. Shipped passkey capability remains disabled.

The fully qualified selector requested for the entry-scope fix is unsupported by `Scripts/targeted.py`:

```sh
make targeted ONLY="app:net.chronoflow.app.auth.SignUpEntryScopeTest#removedEntryIsClearedAndARecreatedActivityKeepsItsCurrentEntry"
```

```text
targeted: class 'net.chronoflow.app.auth.SignUpEntryScopeTest' not found in app
make: *** [targeted] Error 2
```

The supported short-class selector shown in the verification table ran that exact method once and passed.

Browser comparison could not run. This command exited **134** with **empty stdout/stderr** and produced no screenshot:

```sh
'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' --headless --no-sandbox --disable-gpu --disable-crash-reporter --no-first-run --no-default-browser-check --user-data-dir=/private/tmp/cf-phase5d-browser --screenshot=/private/tmp/cf-phase5d-browser-smoke.png --window-size=375,812 about:blank
```

The board HTML also references `_design/canvas/support.js`, which is absent: `head: _design/canvas/support.js: No such file or directory`. Browser review is the outstanding visual acceptance item; the captures and structural guards are available for that review.
