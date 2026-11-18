# Auth shell and Sign in

Phase 5a, 2026-11-18. The implementation follows the pinned iOS auth models and the AuthSignIn/AuthStates boards. Auth routes replace the root without transitions. Two-factor, Forgot, Sign up and the accepted-session destinations retain their existing route placeholders until their phases.

Implementation and the focused JVM/Robolectric checks are complete. The orchestrator-authorized keyboard follow-up passes with the full keyboard after correcting functional device prep. Acceptance remains **open**: the original `make quick` invocation failed; its failures pass focused checks after correction, and the authorized full rerun is next. The sandbox cannot launch a browser for board comparison, and manual autofill/TalkBack checks need configured services. No performance benchmark, baseline-profile, `make check` or other emulator tier ran.

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
| Request job and presentation state | Activity-scoped `SignInViewModel` | `leave()` cancels its request on disposal |
| Layout coordinates | Plain remembered `AuthCoordinates` holder | Layout only; never snapshot state |

`SignInContent` takes state, caller-owned field states and events. The ViewModel adapts the model and owns the request lock. Its presentation state contains the form and passkey visibility: email keystrokes with unchanged feedback do not emit a new card state. A JVM emission test covers this.

The secret adapter reads the password once for the request and clears it in `finally`, then unlocks the form. The successful order is session accept → autofill commit → password clear → stored-gate routing → final cleanup. A `NonCancellable` block covers acceptance through routing, preserving a committed session when its screen request is cancelled. Two-factor stores only its in-memory challenge and navigates without route arguments; it never accepts a session.

The app supplies `LazyPublicDataSource`, resolving the suspend `SessionDataSources.public()` accessor inside each request. Construction performs no IO. Device identity and the memoized device name run on injected IO; the existing source/controller dispatch their network, storage and Keystore work to IO. Production acceptance delegates directly to `StoredDataReadiness.routeAcceptedSession`, which owns its routing task and rechecks the current epoch. No extra app-scope acceptance task or duplicate gate logic was added. The 20 routing cases exercise password/passkey, every gate, cancellation after commit, and sign-out during suspended gate reads.

`AppGraphKey` is read by the factory in `:app`; `CfRoot` supplies creation extras at the composition boundary. `RootContent` retains its transition-free host and activity scoping. Debug Welcome keeps only its guest seam; every variant routes Sign in normally.

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
| `make targeted ONLY="app:SignInKeyboardTest"` | **Failed**, no full software keyboard; test **4.112 s**; no reveal result | 8.81 s |
| `make quick` (one invocation) | **Failed**: 2,525 passed / one failed; old forced-sign-out placeholder tag and a 2,689 ms Konsist route guard (1,000 ms budget) | 22.64 s |
| `make targeted ONLY="app:LaunchGateRoutingTest#currentUnauthorizedRoutesToSignInAndErasesAccountFiles"` | Passed after updating the structural tag; test 2.647 s | 5.80 s |
| `make targeted ONLY="konsist:KonsistRulesTest#routesAreSerializable"` (diagnostic, before optimization) | JUnit passed; tier failed its budget at 1.263 s | 3.22 s |
| Five focused `KonsistRulesTest` route methods | Passed after memoizing ancestry; project check 542 ms; four positive/negative fixture controls all under 1 s | 3.95 s |
| `Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew --console=plain :build-logic:convention:test` | 26 passed | 1.86 s |
| `IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new Scripts/test_scripts.sh` | 107 script, 25 contract, 49 localization tests passed; icon suite eight passed / one failed (stale 32-glyph count) | 12.37 s |
| `python3 -I -m unittest discover -s tools/icons -t tools/icons -p test_generate_icons.py -k test_manifest_names_all_generate` | Passed after updating count to 34; test 11 ms | 0.057 s |
| `make timing` | Six passed, zero over-budget; serial JVM preparation/drawing budgets | 3.02 s tier / 3.07 s process |
| `make targeted ONLY="feature:auth:AuthAnnouncerTest#persistentLiveRegionRepeatsOnlyForANewFailure"` | Passed after keeping the screen assertion structural; test 2.356 s | 5.62 s |

Initial compilation fixes were explicit parameter array types, fixture construction through the existing validated account constructors, correct Compose action return types/geometry accessors, and parenthesized factory delegation. Layout/semantics fixes were confined to the test harness; no assertion was weakened. Lint was fixed with `make format`.

Final verification found one additional stale screen tag in the earlier forced-sign-out test, updated to `sign-in.email` without changing its route/epoch/erase assertions. The Konsist route guard repeatedly expanded shared parent declarations; an identity-memoized walk now uses the same direct-parent matching API, including indirect ancestry. New fixture controls prove an unannotated indirect route is rejected and an annotated indirect route is accepted. No budget or exemption changed. The generator assertion now includes T4's two new glyphs; generation/content checks remain intact.

The brief's requested literal announcement assertion was replaced with a nonempty content-description check to follow the user's stronger structure-only screen-test instruction. The persistent live region, identical-repeat content change, unchanged message across repeats, and absence of a second banner live region remain checked; model/resource tests cover the copy.

The first standalone `Scripts/test_scripts.sh` invocation omitted Make's exported `IOS_REPO`/`WEB_REPO` values and stopped at the contract parity precondition (14.13 s): `AssertionError: set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)`. The corrected invocation is recorded above. An initial isolated icon invocation using `-I -m unittest test_generate_icons...` could not import the local module; the discovery-based command above ran the actual method. These invocation errors changed no production behaviour.

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
```

The final acceptance documentation commit is `docs(auth): record sign-in decisions and acceptance`.
