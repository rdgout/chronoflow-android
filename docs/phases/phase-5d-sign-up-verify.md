# Phase 5d: Sign-up and email verification

- **Branch:** `feat/phase-5d-sign-up-verify`, from the stack top once **5a** (auth shell, form model, sign-in) is in it. 5a brings 4e (session controller, gate router, routes, seeds), 4c (mock with mail inbox), 4b (`DataSource`), 4a (device identity), 2 (primitives), 0c (strings), 0e (profiles, guards, captures). If one is missing, stop and report it.
- **Stack check (2026-11-18, against 9972e7c).** Single lane: 5a, 5b and 5c are all in the stack, so §5.10's "5c is in the stack" path applies. 5c created `AuthHeadingGlyph` (`…/auth/shell/AuthHeadingGlyph.kt`, `AuthScreen(headingGlyph)` at `AuthScreen.kt:46`), `AuthPasswordPolicy` (`…/auth/form/AuthPasswordPolicy.kt`, `HINT_KEY` + `hint()`), `EmailAppLauncher` (`…/auth/shell/EmailAppLauncher.kt`, already used by `CfRoot.kt:155,167`) and `VerifyLinkReceiver` (`…/auth/links/VerifyLinkReceiver.kt`), along with their tests: reuse them, do not recreate them. `AuthLinkRouting` and `AuthLinkDispatcher` live in `:app` (`app/…/app/links/`), not `feature/auth`. The placeholder is `AppGraph.verifyLinks` (`AppGraph.kt:186`, `VerifyLinkReceiver {}`), which `authLinkRouting` (l.187) receives; Task 6 replaces that member with `auth.verification`. `SignedVerificationInput.toString()` already redacts `hash` and `signature` (`core/model/…/api/AuthContracts.kt:70`), so skip §5.12's fallback. Route entries are registered in `CfRoot.kt`'s `when (displayedRoute)`, not `RootContent.kt`: add the SignUp and Verify entries there. Add `Route.SignUp` and `Route.Verify` to the launch-sky condition at `CfRoot.kt:94-95` if the boards draw them on the launch sky. `AuthServices.kt:55` already navigates to `Route.SignUp`, and l.143 lists it. `VerificationLinkInbox`, `VerificationCoordinator`, `SignUpModel` and `makeSignUpModel` do not exist yet. 5c's OQ rows end at OQ-A213. 5c review fixes made `Scripts/run_tier.py` require a passed stamp before reusing test outputs (9972e7c). Link launches in Robolectric tests go through `Robolectric.buildActivity`, never `ActivityScenario` (MainActivity scrubs intent data, so `filterEquals` never matches), and `onNewIntent` must not call `setIntent`.
- **Runs in parallel with 5b and 5c** (all wave 9, all depend on 5a only). 5c owns App Links, the `AuthLink` parser, `AuthLinkDispatcher` and `AuthLinkRouting`, and hands verify links to its `VerifyLinkReceiver` seam. The pieces shared with 5c are listed in 5c §5.9: `AuthHeadingGlyph` + the `AuthScreen(headingGlyph)` parameter, `AuthPasswordPolicy`, `EmailAppLauncher` and `VerifyLinkReceiver`; whichever phase lands first creates them at 5c's paths. **This phase owns** `VerificationLinkInbox` (5c §3 says so; 5c never creates it), the Verify screen, `VerifyEmailModel` and the `VerificationCoordinator` (which implements `VerifyLinkReceiver`). §5.10 says exactly what to do when 5c is or is not in the stack.
- **Implementation preflight (2026-11-18).** Reuse the landed names without renaming upstream code: `MockScenarioValues` is the public constants object; `MockScenarios.kt` contains module-private test helpers, not an exported `MockScenarios` type. Registration uses `RegisterInput.deviceID`, not `deviceId`. `SessionDataSources.forSession` and `.public()` both suspend on IO, so `VerificationSessionPort.source` must be suspending. `StoredDataReadiness.routeAcceptedSession` supplies a core-model `LaunchRoute`; the app adapter maps it with the existing `toRoute()` before invoking the coordinator's `Route` callback. `AppDispatchers` belongs to `:app`; inject its `main` and `io` dispatcher values into the feature's JVM-testable coordinator/model rather than introducing a feature-to-app dependency. `MockSources` lives in `SessionDataSources.kt` and its implementation is `PerfMockSources` in the debug-shared `src/perf` set; extend that variant seam for verification mail, keeping `MockVerificationMail` and mock imports out of release. The `VerificationMail` interface itself stays in main beside the coordinator; only its mock implementation is debug-only. Shared `AuthHeadingGlyph` geometry already exists, so do not add duplicate problem-disc metrics. Screens follow the landed stateless `XScreen(state, onEvent)` pattern with route adapters for model, field and lifecycle ownership. Resource reads use `LocalResources.current`. Tests split argument rows into separate methods or uniquely named parameterized cases, with no loops over test actions. These are mechanical adaptations and preserve earlier-phase behavior.
- **Plan entry:** `plan.md` "Phase 5d: Sign-up and email verification" (l.1821–1854). Also A6 (90 s register timeout), A8 (routes, back), A16 (verify links → per-account inbox), A23 (no WebView, Custom Tab), OQ-A32 (policy URLs), "Android spec: forms and IME", "Android spec: state survival" (auth forms row), "Android spec: window policy" (auth row), inherited OQ 21, 23, 25, 29, D27.
- **Tests** follow `AGENTS.md` "Tests" (cheapest tier, budgets, exact tag queries, no `Thread.sleep`, hang caps). Tier annotations are the 0e ones: Robolectric `@Logic`/`@Layout`/`@Capture`; emulator `@Smoke`.
- **Tags:** **[F]** fact read in source (path given). **[R]** rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` at `f391a1fa` (read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`.
- **Paths:** `…/auth/` = `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/model/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`.
- **Names from earlier phases.** This brief uses the names of the final 5a, 4e, 4c, 5c and 2 briefs:
  - 5a (`feature/auth`): `AuthScreen` (shell), `AuthBanner`, `AuthAnnouncer`, `AuthFailureFeedback`, `AuthKeyboardReveal`, `AuthMetrics`, `UserEditReporter`, `TextFieldSecret` (all in `…/auth/shell/`); `AuthFormModel` + `AuthFormState` + `AuthFormModel.Action`, `AuthFailureMapping`, `AuthFormContext`, `AuthBannerKind`, `AuthFieldCopy`/`AuthText`, `AuthNavigation` (with `challengeID` and `companion fun route(account)`), `SecretField`, `SessionAcceptor`, `SignInModel` (all in `…/auth/model/`); `SignInRouter`, `SignInViewModel` (`…/auth/signin/`); `AuthServices` (`…/app/auth/AuthServices.kt`, `AppGraph.auth`); `RootContent` entries; `AuthenticationMethod.SignUp` (`:core:model` `…model.auth`); test fakes `FakeSessionController`, `FakeSecretField`, `RecordingRoutes` (`feature/auth/src/test/…/testing/`).
  - 4e: `SessionController` (`presence`, `epoch`, `activeSession()`, `updates`, `confirmEmailVerification(expected, email)`, `signOut(expected, discardPending)`), `SessionSnapshot`, `SessionUpdate`, `SessionPresence` (4a package), `SessionFailure` (`CredentialsUnavailable`, `DatabaseUnavailable`, `CredentialWrite`, `InvalidResult`, `Superseded`), `StoredDataReadiness.routeAcceptedSession(navigate, verification)`, `SessionGateRouter(onVerify)`, `RootNavigator` (`state: StateFlow<RootState(route, revision)>`, `navigate(to)`), `Route.SignIn/SignUp/Verify/Walkthrough(replay)`, `AppGraph.dataSources.forSession(session)`/`.public()`, `AppGraphKey`, `ScreenSessionFixture`, `TestSeed`, `RouteProbe`.
  - 4c (`net.chronoflow.core.datamock`): `MockDataSource` (`inbox()`, `failNext(MockMethod, DataFailure)`, `setOffline(Boolean)`), `MockMethod.SignUp/VerificationStatus/ResendVerificationEmail/VerifyEmail`, `MockMail.Verification(id, to, sentAt, input)`, `MockServerState`, `MemoryMockStateStore`, `MockScenarios`.
  - 5c (final brief, 5c §5.6 and §5.9): `AuthLinkDispatcher`, `AuthLinkRouting(scope, resetRouter, verifyLinks: VerifyLinkReceiver)`, `AuthLink.Verify(input?)`, `fun interface VerifyLinkReceiver { suspend fun receiveVerificationLink(input: SignedVerificationInput?) }` (`…/auth/links/VerifyLinkReceiver.kt`), `AuthHeadingGlyph` (`Problem`, `Check`) with `AuthScreen(headingGlyph: AuthHeadingGlyph? = null)`, `AuthPasswordPolicy` (`…/auth/form/AuthPasswordPolicy.kt`: `HINT_KEY` + `@Composable fun hint(): String`), `class EmailAppLauncher(context)` with `fun open(): Boolean` (a class, no interface).
  - 2: `CfButton(loadingDescription)`, `CfTextField`, `CfSecureTextField`, `CfSwitch`, `cfToggleable`, `CfBanner`; 0c `CopyIndex.find(CopyTable.Auth, key)`.

  Before coding, `grep` each name in the merged code and use **its exact name**. Where a name differs, keep the merged one and list the mapping in the PR. Never rename earlier code.

---

## 1. Goal

A signed-out person taps "Create an account" on Sign in, enters name, email, password and confirmation, and gets an **unverified** session. The 4e gate sends them to **Verify**: "Check your inbox", with Open email app, a rate-limited Resend link with a `m:ss` countdown, and Use a different email. A verify link (an App Link from 5c, or the mock inbox in debug) or a verification done in the browser moves them through the gate (terms → walkthrough → app). A link that fails shows the **failed** state (Try again, Send a new link). The drawn back arrow and "Use a different email" **sign out and wipe** the unverified account.

Done means:
- `SignUpModel` is an exact port of iOS `SignUpModel` (without the guest claim, see §3), hosted by an entry-scoped `SignUpViewModel`: no client validation, passwords cleared after **every** attempt, name and email kept, one shared show/hide control for both password fields, server field errors under their fields, the email-taken banner, register with `language` and the device identity, then the gate.
- The two consent links open `https://chronoflow.net/terms` and `https://chronoflow.net/privacy` in a Custom Tab.
- `VerifyEmailModel` and `VerificationLinkInbox` are exact ports. Every result is checked against account + email + session epoch before `has_verified_email` is stored. The resend deadline uses wall-clock time, keeps the larger deadline, survives backgrounding, and a 429 extends it. The model re-checks on every return to the foreground.
- Verify back and "Use a different email" call `SessionController.signOut(expected, discardPending = true)`: the user lands on Sign in, signed out, with the account directory wiped.
- The ported iOS cases pass on the JVM. Robolectric guards cover every state of `AuthSignUp`, `AuthVerify` and `AuthVerifyFailed`. **One** emulator journey goes sign-up → Verify → mock inbox link → Walkthrough.

## 2. Inputs to read (in this order)

1. **This repo:** `plan.md` sections above; `AGENTS.md` ("Working rules", "Performance", "Tests", "Traps"); `docs/phases/phase-5a-sign-in.md` (all of §5 and §6, OQ-5a-5 and OQ-5a-7, and the Appendix "hand-over names for 5b–5d": you reuse its shell, form model, feedback, field rules, model/ViewModel split and router pattern); `docs/phases/phase-5c-password-reset-links.md` §3 (5d owns the inbox), §5.2 (heading glyph geometry), §5.6 items 2–3 (`AuthLinkDispatcher`, `AuthLinkRouting`, `VerifyLinkReceiver`), §5.9 (shared pieces), Task 2 (the shared-piece tests), OQ-5c-7 (redacting `toString()`), OQ-5c-8; `docs/phases/phase-5b-two-factor-passkeys.md` "[F] Root routes" (5b and 5c deliberately use **no** ViewModel entry decorator; 5d is the first phase that adds one, as OQ-5a-5 recommends); `docs/phases/phase-4e-session.md` §5.1–§5.4, §5.7–§5.8, T10 (controller, sign-out, readiness, gate router, routes, `RootContent`) and the line "Verification callback: `onVerify(epoch)` … 5d may tighten this", OQ-4e-11, OQ-4e-12; `docs/phases/phase-4c-mock-data-source.md` (Verification row of the rules table, `MockMail`, `failNext`, `setOffline`, `inbox()`, `MockScenarios`); `docs/phases/phase-2-design-system.md` Tasks 11–12 and the `CfSwitch` paragraph; `docs/phases/phase-0c-strings.md` (resource names, `CopyIndex`, the literal `<terms>` tags).
2. **Research:** `docs/research/05-launch-auth.md` §2.2 (board values the iOS decisions override), §8.1–§8.4, §9.6 (Sign up), §9.7 (Verify), §9.9 (copy), §10 (API), §11.3 (link routing), §15.4–§15.8, §15.13; `docs/research/03-api-contract.md` §3 (error envelope), §4 (limits), §5.1 rows 1, 9, 11, 12, §6 (unverified tokens), §8.
3. **Boards** (read-only; open the HTML in a browser to render them):
   - `DC/AuthSignUp.dc.html`: props `theme` (light/dark), `error` (`none` | `taken` | `server`). Frame 375 × 812, `padding: 16px 16px 24px`, heading `margin-top: 24px`.
   - `DC/AuthVerify.dc.html`: wrapper, `<dc-import name="AuthTwoFactor" screen="verify">`.
   - `DC/AuthVerifyFailed.dc.html`: wrapper, `<dc-import name="AuthTwoFactor" screen="verifyFailed">`.
   - `DC/AuthTwoFactor.dc.html`: the big board. Read the `isVerify` and `isVerifyFailed` sections and the `verify`/`verifyFailed` rows of the `copy` table. Its `error` prop (`attempts`, `server`) draws the banner on these screens too.
   - `DC/AuthStates.dc.html`: banner catalogue ("Create an account": EmailTaken, Server; "Verify": Server).
4. **iOS references** (`I/` at `f391a1fa`). Port behaviour from these, and keep their type and test names:
   - `plan.md:1271` "Phase 11c: Sign up (`feat/phase-11c-sign-up`)" and `plan.md:1287` "Phase 11d: Verify email waiting/failed and recovery acceptance (`feat/phase-11d-verify`)", including all fix rounds and the "11c/11d implementation" paragraphs.
   - `plan.md:494` "Form behavior, routing and accessibility" (the Sign up and Verify bullets); `plan.md:505` "Auth copy"; `plan.md:2376` "Open questions for the user" (OQ 21 at l.2400, 23, 25, 29).
   - `Kit/Auth/SignUpModel.swift` (137 lines, including `AuthPolicy`), `SignUpView.swift` (131), `AuthConsent.swift` (42), `VerifyEmailModel.swift` (195), `VerifyEmailView.swift` (120), `VerificationLinkInbox.swift` (53), `AuthCopy.swift:44-52` (`verificationSent`, `resendCountdown`), `AuthFormModel.swift` (already ported in 5a), `SignInModel.swift:72-81` (`makeSignUpModel`).
   - `I/App/Session/AuthSessionServices.swift:141-248` (`verificationContext`, `receiveVerificationLink`, `prepareVerification`, `synchronizeVerificationSession`), `I/App/AppDelegate.swift:44-56` (`verificationRequired`), `I/App/RootView.swift:102-103`, `I/App/SceneDelegate.swift:531,686-692`, `I/App/Debug/AuthUITestConfiguration.swift:8-18`.
   - `Kit/AppCore/Contracts/AuthContracts.swift:32-123` (`RegisterInput`, `SignedVerificationInput`, `EmailVerificationStatus`, `AuthLanguage`); `Kit/DataAccess/API/APIDataSource.swift:38-57`; `Kit/DataAccess/Mock/MockDataSource.swift:122-170, 276-345, 395-446`.
   - Tests: `Packages/ChronoFlowKit/Tests/AuthTests/{SignUpModelTests,VerifyEmailModelTests,VerificationLinkInboxTests}.swift`; `I/AppUITests/{SignUpTests,VerifyEmailTests,AccountRecoveryTests}.swift`; `I/AppTests/{SignUpControlTests,SignUpLayoutTests,SignUpSnapshotTests,VerifyEmailControlTests,VerifyEmailLayoutTests,VerifyEmailSnapshotTests}.swift` (the snapshot files list the 10 + 10 states you guard).
5. **iOS reference screenshot:** `docs/research/screens/ios/14-sign-up.png` (light, empty form, show-password row with a switch at the trailing end).
6. **API** (web at `e36368ec`, through 4b; never called in tests):

| Call (`DataSource`) | Endpoint | Auth, timeout | Request | Success | Errors and limits |
|---|---|---|---|---|---|
| `signUp(RegisterInput)` | `POST /v1/auth/register` | public, no bearer, **90 s** | `{name, email, password, password_confirmation, device_id (UUIDv7), device_name (≤ 255), language: "en"\|"nl"}`. 5d sends **no** `draft_token`, `input_revision`, `review_revision` or `guest_secret` (absent, not null). | 200 `ApiSession {kind:"session", token, account: ApiAccount, tripId: null}`. The account has `hasVerifiedEmail: false`, and the server sends a verification mail. | 422 `validation_failed` with `AuthMessageKeys` (§5.9), `email`: `"This email address is already in use."`; 429 `throttled` (`api-registration` 5/min/IP) |
| `verificationStatus()` | `GET /v1/users/me` | bearer (not `verified`), 15 s | — | `ApiAccount {id, name, email, hasVerifiedEmail, policies, walkthroughSeen}`, mapped by 4b to `EmailVerificationStatus(email, hasVerifiedEmail, resendAfterSeconds = 0)` [F `APIDataSource.swift:47-51`] | 401 |
| `resendVerificationEmail()` | `POST /v1/email/verification-notification` | bearer, 15 s | none | 200 `{email, hasVerifiedEmail, resendAfterSeconds}`; already verified → `true, 0`, no mail | 429 (`api-verification-send` 1/min per user) with `retryAfterSeconds` |
| `verifyEmail(SignedVerificationInput)` | `POST /v1/email/verify` | bearer, 15 s | `{id (uuid), hash (40 hex), expires (int), signature (64 hex)}` exactly as parsed from the link; never re-signed | 200 `{email, hasVerifiedEmail, resendAfterSeconds}` | 422 on every field: `"This email verification link is invalid or has expired."`; 429 (`api-verification` 6/min user+IP) |
| `fetchIdentity()` | `GET /v1/users/me` | bearer | — | `AccountIdentity` | 401 |
| sign-out | `POST /v1/auth/logout` | bearer, 3 s | — | 204 | best effort, inside 4e teardown |

Link shape (5c parses it): `https://app.chronoflow.net/email/verify/{id}/{hash}?expires={unix}&signature={64hex}` [F `03 §8`].

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| App Link intent filters, the `AuthLink` parser, `AuthLinkDispatcher`, `AuthLinkRouting`, `PendingLaunchIntents` link capture | 5c. 5c's `AuthLinkRouting` sends `AuthLink.Verify(input)` to its `VerifyLinkReceiver.receiveVerificationLink(input)`; in 5d that receiver is `VerificationCoordinator` (§5.10). |
| Guest claim in registration (`claim`, `guestReviewExpired`, `GuestSignupState`, `Outcome.GuestReviewExpired`, `requiresGuestClaim`) and the three iOS guest tests (`guestSignupRequiresALiveClaimBeforeCreatingAnAccount`, `guestSignupDistinguishesARequestFailureFromAnAccountAwaitingAcceptance`, `expiredGuestClaimShowsReviewAndClearsPasswords`) | 11b. It adds them to `SignUpViewModel` with a refactor commit. |
| The "Verified, no trip claimed" line and the guest continuation after verification | 11b |
| `AuthEmailChanged` board (screen `changed`) | Out of scope (iOS D27, A24) |
| `PolicyReader` board | Not built (05 OQ A6, OQ-A32); the consent links open a Custom Tab |
| Debug seeds `CF_AUTH_TEST_SIGNUP_FAILURE`, `CF_AUTH_TEST_VERIFY_STATE`, `CF_AUTH_TEST_OFFLINE`, sign-up prefill by `CF_AUTH_TEST_AUDIT_STATE` | Not built here: the states are guarded in Robolectric, which builds them directly. Add them only when a device audit is requested (record in `docs/auth.md`). |
| A toast, an inbox screen, an offline pre-check, a disabled-while-offline submit | Never (iOS: "no mock inbox, toast or email-changed UI", "No offline-disabled submit") |
| Two-factor, passkeys | 5b |
| Haptics | None (iOS auth has none). `CfSwitch` is used with `onCheckedChange = null` under `cfToggleable`. Phase 2's `cfToggleable` **does** fire `ToggleOn`/`ToggleOff` (Phase 2 l.484, system-control parity); accept it and add no other haptic. |

## 4. Settled values that override the raw boards [F]

| Board says | Build | Source |
|---|---|---|
| `pattern-dots` at 0.2 over the sky | Plain sky only | User 2026-11-06, `I/plan.md:901-906` |
| Sign up fields prefilled `Rick` / `rick@example.com` / `sunrise-in-tokyo` | All fields start **empty** | `Kit/Auth/SignUpModel.swift:8-11` (`name`, `email`, `password`, `confirmation` all `""`) |
| Password hint "At least 10 characters." | Auth "Use at least 12 characters, with uppercase and lowercase letters, numbers and symbols. Choose a password that has not appeared in a data leak." | OQ 21, `I/plan.md:555,2400` |
| One link "terms and privacy policy" → `PolicyReader` | Two links: `<terms>` → `https://chronoflow.net/terms`, `<privacy>` → `https://chronoflow.net/privacy`, in a Custom Tab | `Kit/Auth/SignUpModel.swift:134-137`, `AuthConsent.swift`, OQ-A32 |
| Show/Hide: a borderless eye button (`role=switch`) | A row: eye glyph 18 + label "Show password"/"Hide password" + a trailing `CfSwitch` (iOS native `Toggle`, see `14-sign-up.png`) | `SignUpView.swift:60-70`, `I/plan.md:477` (OQ-5d-1) |
| "Create account" `href="AuthVerify.dc.html"` | Register, then the 4e gate (unverified → Verify) | `I/plan.md:501` |
| Verify back `href="GuestSignup.dc.html"`; "Use a different email" → `GuestSignup` | Both = explicit sign-out with discard → Sign in | `I/plan.md:501`, OQ 29 |
| Verify sub with `rick@example.com`, countdown "0:42" | The account's real email; the countdown from the server deadline (hidden at 0) | `I/plan.md:501` ("0:42 is sample only") |
| Field heights 36, buttons 40, link `min-height: 44` | Phase 2 field box ≥ 48 dp; `Lg` buttons keep a 40 dp visual with a 48 dp target; failed-state buttons 48 dp visual; links 48 dp min height | AGENTS (48 dp targets), 5a OQ-5a-4 |
| No offline banner on the boards | Offline banner, title only: "Needs a connection" / "Verbinding nodig" | iOS OQ 23 |

## 5. The spec

### 5.1 Sign up layout [F `DC/AuthSignUp.dc.html`, `Kit/Auth/SignUpView.swift`]

5a's `AuthScreen(scrollTag = "sign-up.scroll", title = Auth "Create an account", description = Auth "Save your trips and get reminders on this phone.", onBack = vm::backToSignIn, showsBack = true, compact = true /* heading margin 24 */, enabled = !state.form.busy, announcementRevision = state.form.announcementRevision, takeAnnouncement = { vm.takeAnnouncement(resources) }, card = …, footer = …)`.

Card content, in this order. The card gap is 14 dp, except row 7:

| # | Element | Spec | Test tag |
|---|---|---|---|
| 1 | Banner (only when present) | 5a `AuthBanner(kind, AuthFormContext.SignUp)`; always first | `auth.banner` |
| 2 | Name | `CfTextField`, label Auth "Name" | `sign-up.name` |
| 3 | Email | `CfTextField`, label Auth "Email" | `sign-up.email` |
| 4 | Password | `CfSecureTextField(revealed = showsPassword)`, label Auth "Password", `hint = AuthPasswordPolicy.hint()` (the policy hint, §4, §5.10), which stays visible under the field when no error is shown | `sign-up.password` |
| 5 | Confirm password | `CfSecureTextField(revealed = showsPassword)`, label Auth "Confirm password", no hint | `sign-up.confirmation` |
| 6 | Show/hide row | Row, min height 48: lucide `eye` 18 dp (board path `M2.062 12.348a1 1 0 0 1 0-.696 …` + `circle cx12 cy12 r3`), `foreground`, hidden from accessibility; gap 8; label `authLabel` (Figtree 500 14/20) `foreground`, Auth "Show password" when hidden, "Hide password" when shown; flexible space; `CfSwitch(checked = showsPassword, onCheckedChange = null)`. The whole row is `Modifier.cfToggleable(value = showsPassword, onValueChange = { vm.togglePassword() }, enabled = !busy)` (Phase 2 l.484: it is `toggleable(role = Role.Switch)` plus the haptics; it takes no `role` parameter), merged, with `contentDescription` fixed to Auth "Show password" (iOS `.accessibilityLabel("Show password")`) and the toggle state from `toggleable`. | `sign-up.visibility` |
| 7 | (spacing) | The gap between rows 5 and 6 is **4 dp** (board: inner gap 14 with `margin-top: -10px` on the toggle). OQ-5d-2. | — |
| 8 | Submit | `CfButton(style = Primary, size = Lg, fillWidth = true, loading = pending == Register, loadingDescription = <its label>)`, Auth "Create account" | `sign-up.submit` |
| 9 | Consent | §5.3 | `sign-up.consent` |

Footer: `CfButton(style = OnSky, fillWidth = true, minHeight = 48.dp)`, Auth "Already have an account? Sign in", tag `sign-up.back`, → `vm.backToSignIn()`.

- **Busy:** the whole screen is disabled while a request runs (fields, toggle row, links, drawn back, footer), as on iOS `.disabled(model.form.busy)`. System back still works: `BackHandler { vm.backToSignIn() }`, always enabled, which cancels the request (§5.4).
- **Screen root:** `paneTitle` comes from `AuthScreen`.
- **Failure presentation** (5a `AuthFailureFeedback`): a banner-only failure hides the IME, clears focus and brings the banner into view. A field failure focuses the first invalid field in the order `name`, `email`, `password`, `password_confirmation` (tags `sign-up.name`, `sign-up.email`, `sign-up.password`, `sign-up.confirmation`) [F `SignUpView.swift:99-103`].
- **Keyboard reveal** (5a `AuthKeyboardReveal`): the pair is the focused field (with its hint or error) plus `sign-up.submit`.

### 5.2 Sign up fields, IME and autofill [F `SignUpView.swift:38-59`; R `05 §15.4`, `§15.8`]

| Field | Keyboard | IME action | Autofill `contentType` | State owner |
|---|---|---|---|---|
| Name | `KeyboardType.Text`, `KeyboardCapitalization.Words` | `Next` → email | `ContentType.PersonFullName` | `SignUpModel.name: StateFlow<String>`; the screen's field is `remember { TextFieldState(model.name.value) }` (survives recreation through the entry-scoped ViewModel; never saveable) |
| Email | `KeyboardType.Email`, `autoCorrectEnabled = false`, `KeyboardCapitalization.None` | `Next` → password | `ContentType.EmailAddress + ContentType.NewUsername` | `SignUpModel.email`, same pattern |
| Password | `KeyboardType.Password` | `Next` → confirmation | `ContentType.NewPassword` | `remember { TextFieldState() }` in the screen |
| Confirm password | `KeyboardType.Password` | `Go` → `submit` | `ContentType.NewPassword` | `remember { TextFieldState() }` in the screen |

- Both password states are passed through the ViewModel to `SignUpModel.submit` as `SecretField`s (5a `TextFieldSecret`), read once and cleared in `finally`. Neither the ViewModel nor the model stores a password in a property. Never `rememberSaveable`, `rememberTextFieldState()`, `SavedStateHandle` or a nav argument for any of the four fields (the Konsist rule from Phase 2 Task 17).
- **Reveal:** toggling `showsPassword` switches both fields between obfuscated and visible **on the same `TextFieldState`** (`CfSecureTextField(revealed)`). It never swaps the composable, so values, focus and autofill stay (port of `testSharedVisibilityPreservesValuesAutoFillAndNativeNameTraits`).
- **Real edits only** (5a rule): every field gets a `UserEditReporter` as its `inputTransformation`. Name and email report `UserEditReporter { vm.editName(it) }` / `{ vm.editEmail(it) }`, and the model compares with its stored value. The password fields report `UserEditReporter { vm.editPassword() }` / `{ vm.editConfirmation() }` (`form.edit("password")` / `form.edit("password_confirmation")`). The model's own `clear()` of a secret never clears feedback.
- **No client validation:** a blank form is sent as is. The server or the mock returns every field message.
- **Language** sent: `AuthLanguage.Nl` when the resolved app language is Dutch, else `En`. Read it from the `Language` that 0e `ConfigurationPolicy` resolves (en/nl only), at submit time [F `SignUpView.swift:120`].
- **Autofill commit:** after a successful `accept`, call `commitAutofill()` (`LocalAutofillManager.current?.commit()`) **before** the secrets are cleared, so the password manager can offer to save the new password (5a order: accept → commitAutofill → clear → acceptedSession). Never on failure.

### 5.3 Consent text and policy links [F `AuthConsent.swift`, `SignUpModel.swift:134-137`; R A23]

- Copy: Auth "By creating an account you agree to the <terms>terms</terms> and <privacy>privacy policy</privacy>." 0c emits the tags literally (`getString` returns `<terms>…</terms>`).
- **`AuthConsentText.parse(text): List<Segment>`** (pure JVM, `…/auth/model/AuthConsentText.kt`, with `AuthPolicy`), an exact port of `AuthConsent.attributedText`:
  - repeat: find the next `<`, then the next `>` after it; `tag` = the text between; if `tag` is not `terms` or `privacy`, or `</tag>` is not found in the remaining text, **stop the loop**;
  - otherwise append the text before `<` as plain, the inner text as `Link(policy)`, and continue after `</tag>`;
  - finally append the remaining text (from the current position, so it still holds the text before the unknown `<`) as plain. An unknown tag leaves the rest, tags included, as plain text.
  - [F] iOS appends the text before `<` **before** its guard (`AuthConsent.swift:26-29`), so an unknown tag after plain text would print that plain text twice (`"a <foo>x</foo>"` → `"a a <foo>x</foo>"`). The shipped copy never hits this. Android appends it only after the guard passes (no duplicate), OQ-5d-16.
- `AuthPolicy` enum `Terms`, `Privacy`; `url = "https://chronoflow.net/" + name.lowercase()`. Only these two URLs are ever opened (iOS discards other URLs).
- Rendering: `Text(AnnotatedString)` in `authMeta` (Figtree 400 12/16), `mutedForeground`, centred, full width, wraps. Each link span is a `LinkAnnotation.Url(url, styles = TextLinkStyles(SpanStyle(color = foreground, textDecoration = Underline)), linkInteractionListener = { policyOpener.open(url) })`. Board underline offset 0.2 em → default underline (5a adaptation).
- **`PolicyOpener`** (`…/auth/signup/PolicyOpener.kt`): `interface PolicyOpener { fun open(url: String) }` (tests pass a fake); `class CustomTabPolicyOpener(private val context: Context, private val dark: Boolean, private val start: (Intent) -> Unit = context::startActivity) : PolicyOpener` with the activity context from `LocalContext` and `dark` from the resolved theme: build `CustomTabsIntent.Builder().setShowTitle(true).setColorScheme(if (dark) COLOR_SCHEME_DARK else COLOR_SCHEME_LIGHT).build()`, set `intent.data = uri` on its `intent` and call `start(customTabs.intent)` (equivalent to `launchUrl`, but testable). On `ActivityNotFoundException`, fall back to `start(Intent(ACTION_VIEW, uri))`. If that throws too, do nothing (OQ-5d-7). Use no WebView and no `resolveActivity` (that needs `<queries>`).
- Dependency: `androidx.browser:browser`, the newest **stable** version on Google Maven (≥ 1.8.0, released at least 2 weeks before use, no alpha, beta or RC). Add it to `gradle/libs.versions.toml` and to plan A1's AndroidX list in the same commit (OQ-5d-6).

### 5.4 Sign-up behaviour: `SignUpModel` (exact port, without the guest claim) and `SignUpViewModel` [F `SignUpModel.swift:6-132`, `SignUpView.swift:93-130`, `SignInModel.swift:72-81`, `I/App/AuthRootView.swift:26,52-58`; R 5a §5.7–§5.8]

iOS builds a **new** `SignUpModel` each time Sign up is shown (`AuthRootView.signUpModel()` → `SignInModel.makeSignUpModel()`), and the view's `onDisappear` cancels the request and calls `model.cancel()`. Android keeps that lifetime with 5a's model/ViewModel split:

**`SignUpModel`** (`…/auth/model/SignUpModel.kt`, plain Kotlin, no Android or Compose import, main-confined):

```kotlin
class SignUpModel(
    private val source: DataSource,                         // the public source, as SignInModel's
    private val session: SessionAcceptor,                   // 5a
    private val deviceIdentity: suspend () -> String,
    private val deviceName: suspend () -> String,
    private val accepted: suspend (SessionResult, AuthenticationMethod) -> Unit,   // SignInRouter::acceptedSession
    private val onBack: () -> Unit,                         // :app → rootNavigator.navigate(Route.SignIn)
)
```
Built only by **`SignInModel.makeSignUpModel(onBack: () -> Unit): SignUpModel`** (new, port of `SignInModel.swift:72-81`), which passes SignInModel's own `source`, `session`, `deviceIdentity`, `deviceName` and `accepted`.

- State: `name: StateFlow<String>`, `email: StateFlow<String>` (both start `""`), `showsPassword: StateFlow<Boolean>`, `form = AuthFormModel(displayedFields = setOf("name", "email", "password", "password_confirmation"), context = AuthFormContext.SignUp)`, `sessionFailure: SessionFailure?` (read-only outside), private `active = true`.
- `editName(v)`: `if (!active || form.busy || v == name.value) return; name.value = v; form.edit("name")`. `editEmail` is the same with `"email"`. Email is never trimmed or lower-cased by the app.
- `editPassword()` / `editConfirmation()`: `if (!active || form.busy) return; form.edit("password")` / `form.edit("password_confirmation")` (the "changed" check is `UserEditReporter`).
- `togglePassword()`: `if (!active || form.busy) return; showsPassword.value = !showsPassword.value`.
- `suspend fun submit(password: SecretField, confirmation: SecretField, language: AuthLanguage, commitAutofill: () -> Unit)`:
  1. `if (!active || !form.begin(Action.Register)) return` (one lock across tap and IME Go).
  2. `sessionFailure = null`.
  3. `try`: `id = deviceIdentity()`; `ensureActive()`; `if (!active) return`. `result = source.signUp(RegisterInput(name = name.value, email = email.value, password = password.read(), passwordConfirmation = confirmation.read(), deviceId = id, deviceName = deviceName(), language = language))` with every guest field absent/null (0d property names; use the merged ones); `ensureActive()`; `if (!active) return`.
  4. `withContext(NonCancellable) { session.accept(result); commitAutofill(); password.clear(); confirmation.clear(); accepted(result, AuthenticationMethod.SignUp) }` (5a §5.7 `accept`, same reason: a committed session must still route after a cancel). `accepted` runs 4e's `routeAcceptedSession`: an unverified account → `onVerify(epoch)` → `Verify`.
  5. `catch (e: CancellationException) { throw e }`. For every other catch, first `if (!active) return` (iOS `guard active, !Task.isCancelled … else { return nil }`: a cancelled screen gets no feedback). Then `SessionFailure` → `sessionFailure = e; form.receive(DataFailure.Server(null))`; `DataFailure` → `form.receive(e)`; any other `Exception` → `form.receive(DataFailure.Server(null))`. Name each type; never a bare `catch (e: Exception)` before the cancellation case.
  6. `finally`: `password.clear(); confirmation.clear(); form.finish()`.
- `backToSignIn()`: `cancel()`, then `onBack()`.
- `cancel()`: `active = false`; `showsPassword.value = false`; `form.clear()`. Idempotent. The screen's password states die with the screen.
- `takeAnnouncement(resolve)` forwards to `form.takeAnnouncement`.
- **Failure mapping** is 5a `AuthFailureMapping` with context `SignUp`. The facts it must already give (assert them in tests): an email `"This email address is already in use."` → banner `EmailTaken` with **no** email field message, and the **other** field messages of the same response are still shown; an unsupported key on a displayed field (for example `password: "unknown English"`) or any undisplayed field (`device_id`) → no field message, and the banner stays `EmailTaken` when it is already set, else `Server`. Server body for `SignUp`: "Your details are kept. Try again in a moment."; locked body: "Wait a few minutes before you try again.".

**`SignUpViewModel(model: SignUpModel)`** (`…/auth/signup/SignUpViewModel.kt`), the 5a `SignInViewModel` pattern:
- `uiState: StateFlow<SignUpUiState(name, email, showsPassword, form: AuthFormState)>` = `combine(model.name, model.email, model.showsPassword, model.form.state)`, `stateIn(viewModelScope, Eagerly, …)`. No password in it.
- `submit(password, confirmation, language, commitAutofill)`: `if (request != null || uiState.value.form.busy) return; request = viewModelScope.launch { try { model.submit(…) } finally { request = null } }` (iOS `SignUpView.submit`).
- `leave()`: `request?.cancel(); request = null`. The screen calls it from `DisposableEffect(Unit) { onDispose { vm.leave() } }`.
- `backToSignIn()`: `leave()`, then `model.backToSignIn()` (iOS `SignUpView.back`).
- `editName`, `editEmail`, `editPassword`, `editConfirmation`, `togglePassword`, `takeAnnouncement` forward to the model.
- `onCleared()`: `leave(); model.cancel()` (iOS `onDisappear`).
- **Entry scope [R, OQ-5d-14]:** the ViewModel must be scoped to the `SignUp` entry so a later visit gets a fresh model. `RootContent`'s `NavDisplay` `entryDecorators` must hold `rememberSaveableStateHolderNavEntryDecorator()` then `rememberViewModelStoreNavEntryDecorator()` (5b and 5c deliberately add no decorator and use no ViewModel for their screens; 5d adds both decorators in Task 4 unless the merged code already has them, as OQ-5a-5 recommends). This also makes 5a's `SignInViewModel` entry-scoped: it is cleared when Sign in is left and rebuilt on return over the same process-scoped `SignInModel`. Run 5a's `SignIn*` test classes after the change; they must pass unchanged. 4e replaces the stack with `[route]` on every navigate, so leaving `SignUp` removes its entry and clears the ViewModel; an activity recreation keeps it.
- Factory (in the `:app` entry, A4): `viewModelFactory { initializer { val g = this[AppGraphKey]!!; SignUpViewModel(g.auth.signIn.makeSignUpModel(onBack = { g.rootNavigator.navigate(Route.SignIn) })) } }`.

**Navigation** (5a §5.9, OQ-5a-7: auth screens are single-entry root routes, no stack, no predictive cross-screen preview):
- 5a's `SignInRouter.signUp()` already does `rootNavigator.navigate(Route.SignUp)`; 5d replaces the `SignUp` placeholder entry with the screen.
- Back from Sign up (drawn `auth.back`, footer `sign-up.back`, system back through `BackHandler`) → `vm.backToSignIn()` → `navigate(Route.SignIn)`. Sign in keeps its email (process-scoped `SignInModel`).
- 5a's "leaving auth clears secrets" observer already treats `SignUp` as an auth route; leaving it for `Verify` clears Sign in's form. Add nothing there.

### 5.5 Verify layout [F `DC/AuthTwoFactor.dc.html` `isVerify`/`isVerifyFailed`, `Kit/Auth/VerifyEmailView.swift`]

5a's `AuthScreen(scrollTag = "verify.scroll", title, description, onBack = { run(model::signOut) }, showsBack = true, compact = false /* margin 32 */, enabled = !form.busy, announcementRevision, takeAnnouncement, card = …, footer = null, headingGlyph = if (failed) AuthHeadingGlyph.Problem else null)` (5c's parameter name; see below).

- **Heading glyph `Problem`** [F `AuthScreen.swift:48-59`, `DC/AuthTwoFactor.dc.html` `isProblem` span]: a 48 dp disc (`AuthTokens.headingBadge`), fill `card`, lucide `triangle-alert` 24 dp (`AuthTokens.headingGlyph`) stroke 2 in `destructive`, leading-aligned, the first child of the heading block; below it an extra bottom padding of 8 dp (`AuthTokens.fieldGap`) **plus** the heading block's own gap of 8 dp, so the title's top = disc bottom + **16 dp** (iOS `.padding(.bottom, fieldGap)` inside a `VStack(spacing: fieldGap)`; board `margin-bottom: 8px` inside `gap: 8px`). Hidden from accessibility (`clearAndSetSemantics {}`). If 5c is in the stack, use its `AuthHeadingGlyph` and `AuthScreen(headingGlyph)` as merged. If not, create them exactly as 5c §5.2 and §5.9 specify (`enum class AuthHeadingGlyph { Problem, Check }` in `…/auth/shell/AuthHeadingGlyph.kt`; `Check`: disc `dawn`, lucide `check` `M20 6 9 17l-5-5` stroke 2.5 in `dawnForeground`; a default parameter so 5a's callers do not change), so 5c reuses them.
- **Waiting:** title Auth "Check your inbox"; description `stringResource(auth_we_sent_a_link_to_x…, context.email)` = "We sent a link to %1$s. Open it on this phone to finish.". The email is the account's stored email and wraps when long.
- **Failed:** title Auth "We couldn't check your email"; description Auth "The link may have expired, or we couldn't reach the server.".

Card, **waiting**, in order (card gap 14):

| # | Element | Spec | Tag |
|---|---|---|---|
| 1 | Sign-out error **or** banner | If `signOutFailure != null`: text `authBody` (16/24) `destructive`: Auth "Needs a connection" when it is `Offline`, else Auth "Something went wrong". Otherwise the 5a banner (context `Verify`). | `verify.sign-out-error` / `auth.banner` |
| 2 | Button group, gap **8** | "Open email app": `CfButton(Primary, Lg, fillWidth)`, no loading state. "Resend link": `CfButton(Outline, Lg, fillWidth, enabled = canResend, loading = pending == ResendVerification)` | `verify.open-email`, `verify.resend` |
| 3 | Countdown, only while `remaining > 0` | `authHint` (400 14/20) `mutedForeground`, centred, full width, tabular figures (`fontFeatureSettings = "tnum"`): `stringResource(auth_you_can_resend_in_x, "%d:%02d".format(Locale.ROOT, r / 60, r % 60))` | `verify.countdown` |
| 4 | "Use a different email" | Text link: `authLabel` (500 14/20) `foreground`, underlined, centred, min height 48, horizontal padding 12, `Role.Button` → `run(model::signOut)` | `verify.different-email` |

Card, **failed**:

| # | Element | Spec | Tag |
|---|---|---|---|
| 1 | Sign-out error or banner | as above | as above |
| 2 | "Try again" | `CfButton(Primary, Lg, fillWidth, minHeight = 48.dp, loading = pending == CheckVerification)` → `run(model::check)` | `verify.retry` |
| 3 | "Send a new link" | `CfButton(Outline, Lg, fillWidth, minHeight = 48.dp, enabled = canResend, loading = pending == ResendVerification)` → `run(model::resend)` | `verify.resend` |

No countdown and no "Use a different email" in the failed state (iOS and board). The drawn back is present in both states.

- **Busy:** the whole screen is disabled while `form.busy` (this includes the silent activation check). Only the pending button shows a spinner.
- **`run(action)`** (port of the view's `request` guard): ignore it if a request job is active or `form.busy`; else launch it in the model's scope and keep the job.
- **Lifecycle** [R, port of `.task` + `scenePhase`]: in the Verify entry, `LaunchedEffect(model) { lifecycle.repeatOnLifecycle(STARTED) { model.activate(); while (true) { delay(1.seconds); model.tick() } } }`. This runs `activate()` on first show and on **every** return to the foreground (email app, browser), and ticks every second. `DisposableEffect(model) { onDispose { requestJob?.cancel(); model.cancel() } }` (iOS `onDisappear`).
- **Recomposition:** only the countdown composable reads `remaining`. The buttons read `canResend` through `derivedStateOf`, so a tick recomposes the countdown text only. The countdown has **no** live region (it would speak every second).
- **Accessibility:** root `paneTitle` = the current title, so the switch from waiting to failed is announced (iOS posts `.screenChanged`). Announcements use the 5a announcer with `takeAnnouncement`; when `signOutFailure` is set, the announced text is the sign-out error text (iOS `authAnnounce(signOutError ?? message)`).
- **System back** on Verify: no override. Verify is a single-entry root stack, so back leaves the app **without** signing out (05 §15.5, OQ A4). Only the drawn back and "Use a different email" sign out.

### 5.6 Verify behaviour: exact port of `VerifyEmailModel` [F `VerifyEmailModel.swift`]

`…/auth/model/VerifyEmailModel.kt` (with `VerificationContext`, `VerifyState`, `VerifyUiState`; no Android or Compose import). It is a plain class (Swift name kept), **not** an androidx ViewModel: the coordinator owns it across navigation, as iOS `AuthSessionServices.verifyModel` does. It has its own `scope` (a child `SupervisorJob` of the app scope, cancelled by `cancel()`) and is confined to the injected main dispatcher.

```kotlin
data class VerificationContext(val accountId: String, val email: String, val epoch: Long)
enum class VerifyState { Waiting, Failed }
class VerifyEmailModel(
  val context: VerificationContext, source: DataSource,
  nowSeconds: () -> Double = { System.currentTimeMillis() / 1000.0 },   // wall clock, as iOS
  current: suspend () -> VerificationContext?,
  persist: suspend (VerificationContext) -> Boolean,
  route: suspend () -> Unit,
  discard: suspend () -> Unit,
  mail: (suspend () -> SignedVerificationInput?)?,                     // non-null only on the mock
  scope: CoroutineScope, dispatchers: AppDispatchers)
```

`val form = AuthFormModel(displayedFields = emptySet(), context = AuthFormContext.Verify)` [F `VerifyEmailModel.swift:20`]: no field message is ever shown on Verify; a 422 on the link (`signature`, …) becomes the `Server` banner through 5a's mapping. `state` starts `Waiting`; `now` starts at `nowSeconds()`.

State (one `StateFlow<VerifyUiState>`): `state`, `form: AuthFormState`, `signOutFailure: DataFailure?`, `sessionFailure: SessionFailure?`, `hasStatus`, `deadline: Double?`, `now: Double`. Derived: `remaining = max(0, ceil((deadline ?: now) − now)).toInt()`; `canResend = !form.busy && remaining == 0 && (hasStatus || state == Failed)`; `isActive`.

Private: `input: SignedVerificationInput?`, `invalidLink`, `active = true`, `revision: Long = 0`, `linkPending`, `completed`.

- `receiveLink(input?)`: if `!active` return; `revision += 1`; store `input`; `invalidLink = input == null`; `linkPending = true`.
- `tick()`: `now = nowSeconds()`.
- `isCurrent(rev)`: `active && revision == rev && coroutineContext.isActive`, then `current()`, then the same three checks again plus `value == context`.
- `activate()`: `tick()`; `check(silent = input == null && !invalidLink)`.
- `check(silent = false)`:
  1. `if (!active || completed || !form.begin(CheckVerification)) return`; clear `signOutFailure` and `sessionFailure`; capture `rev = revision`, `submitted = input`, `malformed = invalidLink`; `linkPending = false`.
  2. `try`: `if (!isCurrent(rev)) return`. If `malformed`: `invalidLink = false`, `state = Failed`, return. `status = if (submitted != null) source.verifyEmail(submitted) else source.verificationStatus()`. When `verifyEmail` throws `DataFailure.Validation` **and** `isCurrent(rev)`, set `input = null` first, then rethrow. Then `if (!isCurrent(rev) || status.email != context.email) return`; `input = null`; `accept(status, rev)`; `if (!isCurrent(rev)) return`; `state = Waiting`.
  3. `catch`: rethrow `CancellationException`; `if (!isCurrent(rev)) return`; a `SessionFailure` sets `sessionFailure`; if `!silent || sessionFailure != null`, `form.receive(failure as? DataFailure ?: Server(null))`; if `!silent`, `state = Failed`.
  4. `finally`: `form.finish()`; if `active && linkPending`, `scope.launch { check() }`.
- `resend()`: `tick()`; `if (!active || completed || !canResend || !form.begin(ResendVerification)) return`; clear both failures; `rev = revision`. `try`: `isCurrent`; `status = source.resendVerificationEmail()`; `isCurrent && status.email == context.email` else return; `accept(status, rev)`; `isCurrent`; `state = Waiting`. `catch`: rethrow cancellation; `isCurrent`; a `SessionFailure` sets `sessionFailure`; `failure = e as? DataFailure ?: Server(null)`; on `Throttled(seconds)`: `tick()`, `deadline = max(deadline ?: now, now + max(0.0, seconds))`; `form.receive(failure)`. `finally` `form.finish()`. (A failed resend does **not** switch to `Failed`.)
- `accept(status, rev)` (private): `isCurrent && status.email == context.email` else return; `hasStatus = true`; `tick()`. If `!status.hasVerifiedEmail`: `deadline = max(deadline ?: now, now + max(0, status.resendAfterSeconds))`; return. Else `identity = source.fetchIdentity()`; require `isCurrent && identity.id == context.accountId && identity.email == context.email && identity.hasVerifiedEmail`; require `persist(context) && isCurrent(rev)`; `completed = true`; `route()`.
- `openEmail(open: suspend () -> Boolean)`: `if (!active || form.busy) return`. If `mail != null`: `mail()?.let { receiveLink(it); check() }`. Else if `open()`: `check(silent = true)`.
- `signOut()`: require `active && !form.busy && current() == context && active && form.begin(SignOut)`; clear both failures; `revision += 1`. `try { discard(); cancel() }`; catch: rethrow cancellation; if `!active` return; a `SessionFailure` sets `sessionFailure`; `failure = e as? DataFailure ?: Server(null)`; `signOutFailure = failure`; `form.receive(failure)`. `finally` `form.finish()`.
- `cancel()`: `active = false`; `revision += 1`; `input = null`; `invalidLink = false`; `form.clear()`; cancel `scope`.

Facts these rules produce, which the tests pin:
- With the real API, the first `activate()` gets `resendAfterSeconds = 0` from `users/me`, so **Resend is enabled right after sign-up**. The first tap returns 429 from the server (1/min), which shows the Locked banner ("Too many attempts." / "Wait a few minutes before you try again.") and a 60 s countdown. This is iOS behaviour; do not "fix" it (OQ-5d-5).
- A failed **check** shows the server body "We couldn't send the link. Try again in a moment." (context `Verify`); iOS parity.
- Background time counts, because the deadline is wall-clock based (OQ-5d-11).

### 5.7 `VerificationLinkInbox`: exact port [F `VerificationLinkInbox.swift`]

`…/auth/links/VerificationLinkInbox.kt`. **5d creates it** (5c §3 assigns it to 5d; 5c never creates or duplicates it, 5c §8 checklist). Main-confined. State: `pending: SignedVerificationInput?`, `presence: SessionPresence?`, `epoch: Long?`, `verified`.

- `sessionChanged(presence, epoch, hasVerifiedEmail = false)`:
  - if a previous epoch exists: if `epoch < previous`, return (ignore stale). If `epoch != previous`: `firstGuestSignIn = presence is SignedIn && this.presence == SignedOut && epoch == previous + 1`; unless `firstGuestSignIn`, `pending = null`.
  - store `presence`, `epoch`, `verified = hasVerifiedEmail`; if `verified`, `pending = null`; if `presence is SignedIn` and `pending` exists but does not match its account, `pending = null`.
- `receive(input?): Boolean`: false if `input == null` or `verified`; false if signed in and `input.id` does not match the account; else `pending = input`, true.
- `take(context): SignedVerificationInput?`: requires `!verified`, `context.epoch == epoch`, `presence == SignedIn(context.accountId)`, `pending` matching `context.accountId`; returns it and clears it.
- `matches(a, b)`: both parse as account ids (0d `AccountID`, case-insensitive UUID) and are equal. A non-UUID never matches.

### 5.8 `VerificationCoordinator`: port of the `AuthSessionServices` verification parts [F `AuthSessionServices.swift:141-248`, `AppDelegate.swift:44-56`]

`…/auth/verify/VerificationCoordinator.kt` (JVM-testable; it gets seams, not Android types). It is created once, lazily, as `AppGraph.auth.verification` (inside 5a's `AuthServices`, process scope). `:app` supplies the adapter `VerificationSessionPort` (`…/app/auth/VerificationSessionAdapter.kt`):

```kotlin
data class StoredVerificationAccount(val id: String, val email: String, val hasVerifiedEmail: Boolean)
interface VerificationSessionPort {
  suspend fun epoch(): Long                                  // controller.epoch
  suspend fun presence(): SessionPresence?                   // controller.presence
  suspend fun activeSession(): SessionSnapshot?              // controller.activeSession()
  suspend fun storedAccount(): StoredVerificationAccount?    // the active SessionGraph's account row (4d/4e read; grep `hasVerifiedEmail`), on io, null on any error
  fun source(snapshot: SessionSnapshot): DataSource          // AppGraph.dataSources.forSession(snapshot.session)
  suspend fun confirm(expected: SessionSnapshot, email: String): Boolean   // AppGraph.sessionSync.stop(), then controller.confirmEmailVerification(expected, email) [F AuthSessionServices.swift:190-196]; 4e's confirm restarts sync through requestSync
  suspend fun signOutDiscarding(expected: SessionSnapshot)                  // controller.signOut(expected, discardPending = true)
  fun updates(): Flow<SessionUpdate>                         // controller.updates.filterNotNull()
}

class VerificationCoordinator(
  port: VerificationSessionPort, inbox: VerificationLinkInbox, navigator: RootNavigator,
  routeAccepted: suspend (navigate: (Route) -> Unit, verification: suspend (Long) -> Unit) -> Unit,  // readiness::routeAcceptedSession
  clearSignInSecrets: () -> Unit,                            // auth.signIn.clearSecrets() (5a)
  verificationMail: VerificationMail?,                       // debug + mock only
  nowSeconds: () -> Double, scope: CoroutineScope, dispatchers: AppDispatchers,
) : VerifyLinkReceiver                                       // 5c §5.6/§5.9; create the fun interface at 5c's path if 5c is not in the stack
```

Coordinator members (`model: StateFlow<VerifyEmailModel?>`):
- `verificationContext(requireUnverified = false)`: `snapshot = activeSession()`; `account = storedAccount()`; require `account.id == snapshot.session.accountId`, `!requireUnverified || !account.hasVerifiedEmail`, and `activeSession() == snapshot` again → `VerificationContext(account.id, account.email, snapshot.epoch)`; else null.
- `synchronizeVerificationSession(): Boolean`: `epoch = epoch()`; `presence = presence() ?: return false`; when signed in, require a snapshot with that epoch and a stored account with that id, and read `verified = account.hasVerifiedEmail`. Re-check that epoch and presence are unchanged, then `inbox.sessionChanged(presence, epoch, verified)`. If the current model's `context.epoch != epoch`: `model.cancel()`, set it to null. Return true. It is called on every `updates()` emission (collected in the app scope from creation).
- `prepareVerification()`: `synchronize` must succeed; `context = verificationContext(requireUnverified = true) ?: return`. If there is no model, or its context differs, or it is inactive: cancel the old one and build a new `VerifyEmailModel` with:
  - `current = { verificationContext() }`;
  - `persist = { ctx -> snapshot = activeSession(); require snapshot.epoch == ctx.epoch && snapshot.session.accountId == ctx.accountId; confirm(snapshot, ctx.email) }`;
  - `source = port.source(snapshot)` with the snapshot read in `verificationContext`;
  - `route = { routeAccepted({ r -> if (navigator.state.value.route == Route.Verify) navigator.navigate(r) }, { onVerify(it) }) }` [R: the 4e `SessionGateRouter` may already have moved off Verify when the DB flag flipped, so route only while still on Verify, and never navigate twice];
  - `discard = { snapshot = activeSession(); require epoch, account and verificationContext() all equal the model's context, else return; signOutDiscarding(snapshot); if presence() == SignedOut: clear the model if it is this one, and navigate to SignIn if the route is still Verify }`;
  - `mail = verificationMail?.let { m -> { m.latest(context) } }`.

  Then `inbox.take(context)?.let { model.receiveLink(it) }`.
- `override suspend fun receiveVerificationLink(input: SignedVerificationInput?)`: **the entry 5c's `AuthLinkRouting` calls** through `VerifyLinkReceiver` for a parsed verify link (null = malformed). `synchronize` must succeed and `inbox.receive(input)` must be true, else return. If signed out: `clearSignInSecrets()` and navigate to `SignIn` (the link waits in the inbox for the first sign-in). Else `prepareVerification()`; require the model to be active and `verificationContext(requireUnverified = true) == model.context`; navigate to `Verify`; `model.activate()`.
- `onVerify(epoch)`: **replaces 4e's default `onVerify`** (port of `AppDelegate.verificationRequired`). Require `epoch() == epoch` and a stored, unverified account, re-checking the epoch; `prepareVerification()`; require `epoch() == epoch`, `verificationContext(true)?.epoch == epoch` and an active model; navigate to `Verify` **only if the current route is not already `Verify`** [R: iOS navigates unconditionally (`AppDelegate.swift:55`) but keys the view by model identity (`RootView.swift:102-103`), so a repeat is harmless there; on Android a second navigate bumps the revision, disposes the screen and cancels the model; 4e's default `onVerify` already dedupes this way (4e §5.8)].
- **`VerificationMail`** (`fun interface VerificationMail { suspend fun latest(context: VerificationContext): SignedVerificationInput? }`). Debug builds on the mock data source provide `MockVerificationMail` (`app/src/debug/…/MockVerificationMail.kt`): it takes the **last** `MockMail.Verification` in `mock.inbox()` with `to == context.email && input.id == context.accountId`. Release and the api data source pass `null`, so "Open email app" launches the real email app (OQ-5d-12).

**Verify entry** (in `…/app/RootContent.kt`, next to 5a's SignIn entry, replacing the `Verify` placeholder): `val model by graph.auth.verification.model.collectAsStateWithLifecycle()`; `LaunchedEffect(Unit) { coordinator.prepareVerification() }`. While `model == null`, draw the plain sky only, with no content (iOS shows no Verify view without a model; do not invent a loading state). Otherwise `val launcher = remember(context) { EmailAppLauncher(context) }; VerifyEmailScreen(model, openEmailApp = launcher::open)` (`openEmailApp: () -> Boolean`, so screen tests pass a lambda).

**Wiring** (`AppGraph`/`AuthServices`, Task 6): replace 4e's default `onVerify` (T10) with `{ epoch -> auth.verification.onVerify(epoch) }` for both the gate router and 5a's `acceptedSession` path. In 5c's `AppGraph.authLinkRouting`, replace the placeholder `VerifyLinkReceiver { }` with `auth.verification` (5c §5.6 item 3). The coordinator is a lazy `AuthServices` member whose `init` launches the `port.updates()` → `synchronizeVerificationSession()` collection in its scope. It is first touched by 4e's `finishLaunch` (call `auth.verification` next to `gateRouter.start()`), or earlier by `authLinkRouting` or `onVerify`, whichever comes first, so a link or a sign-in before Verify is seen (iOS builds `AuthSessionServices` at launch). Never touch it in `Application.onCreate` or `MainActivity.onCreate` (AGENTS "Performance"; 5c §5.6 item 2 keeps `authLinks` construction free of `auth`).

**`EmailAppLauncher`** (`…/auth/shell/EmailAppLauncher.kt`, 5c §5.9: `class EmailAppLauncher(private val context: Context) { fun open(): Boolean }`; reuse it if 5c created it): `open()` = `context.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(FLAG_ACTIVITY_NEW_TASK))`; it returns true on success and false on `ActivityNotFoundException`. With no email app, nothing visible happens and the designed state stays (iOS: a missing handler keeps the state; OQ-5d-8).

### 5.9 Copy [F `Kit/Auth/Resources/Localizable.xcstrings`, generated by 0c into `strings_ios.xml`, table `auth`]

**No new string in this phase.** Every key exists in the iOS catalog. The names below were computed with the 0c slug rule; **take the real name from `tools/l10n/names.lock.json`** and, if a name differs, use the lock's. If a key is missing from the lock, stop and report it; never hand-add it.

| English key | nl | Expected resource name |
|---|---|---|
| Create an account | Een account aanmaken | `auth_create_an_account` |
| Save your trips and get reminders on this phone. | Bewaar je reizen en ontvang herinneringen op deze telefoon. | `auth_save_your_trips_and_get_reminders_on_a0f4f9` |
| Name | Naam | `auth_name` |
| Email | E-mailadres | `auth_email` |
| Password | Wachtwoord | `auth_password` |
| Use at least 12 characters, with uppercase and lowercase letters, numbers and symbols. Choose a password that has not appeared in a data leak. | Gebruik minstens 12 tekens, met hoofdletters, kleine letters, cijfers en symbolen. Kies een wachtwoord dat niet in een datalek voorkomt. | `auth_use_at_least_12_characters_with_uppe_46e32e` |
| Confirm password | Bevestig je wachtwoord | `auth_confirm_password` |
| Show password / Hide password | Wachtwoord tonen / Wachtwoord verbergen | `auth_show_password` / `auth_hide_password` |
| Create account | Account aanmaken | `auth_create_account` |
| By creating an account you agree to the \<terms>terms\</terms> and \<privacy>privacy policy\</privacy>. | Door een account aan te maken ga je akkoord met de \<terms>voorwaarden\</terms> en het \<privacy>privacybeleid\</privacy>. | `auth_by_creating_an_account_you_agree_to_9e09cb` (**not** `…_you_accept_th_b18f33`, which is the guest sheet's) |
| Already have an account? Sign in | Heb je al een account? Log in | `auth_already_have_an_account_sign_in` |
| There's already an account with this email. / Sign in instead, or reset your password. | Er is al een account met dit e-mailadres. / Log in of herstel je wachtwoord. | banner keys (5a) |
| Your details are kept. Try again in a moment. | Je gegevens blijven bewaard. Probeer het zo meteen opnieuw. | `auth_your_details_are_kept_try_again_in_a_moment` |
| Check your inbox | Bekijk je inbox | `auth_check_your_inbox` (**not** `…_78cde4`, which is "Check your inbox." with a period, used by Forgot) |
| We sent a link to %@. Open it on this phone to finish. | We hebben een link naar %@ gestuurd. Open die op deze telefoon om het af te ronden. | `auth_we_sent_a_link_to_x_open_it_on_this_694644` (`%1$s`) |
| Open email app | Open je e-mailapp | `auth_open_email_app` |
| Resend link | Link opnieuw versturen | `auth_resend_link` |
| You can resend in %@. | Je kunt de link over %@ opnieuw versturen. | `auth_you_can_resend_in_x` (`%1$s` = `m:ss`) |
| Use a different email | Gebruik een ander e-mailadres | `auth_use_a_different_email` |
| We couldn't check your email | We konden je e-mailadres niet controleren | `auth_we_couldn_t_check_your_email` |
| The link may have expired, or we couldn't reach the server. | De link is misschien verlopen, of we konden de server niet bereiken. | `auth_the_link_may_have_expired_or_we_coul_7bd9a7` |
| Try again | Opnieuw proberen | `auth_try_again` |
| Send a new link | Nieuwe link sturen | `auth_send_a_new_link` |
| We couldn't send the link. Try again in a moment. | We konden de link niet versturen. Probeer het zo meteen opnieuw. | `auth_we_couldn_t_send_the_link_try_again_4a39e9` |
| Needs a connection | Verbinding nodig | `auth_needs_a_connection` |
| Something went wrong | Er ging iets mis | `auth_something_went_wrong` (sign-out error only) |

Server field keys shown under sign-up fields (5a `AuthFieldCopy` whitelist): "Please fill this in.", "Enter a valid email address, like name@example.com.", "Use no more than :max characters." (name ≤ 255), "Use at least :min characters." (password, min 12), "The passwords don't match yet." (`password_confirmation`), "Use both uppercase and lowercase letters.", "Use at least one letter.", "Use at least one number.", "Use at least one symbol.", "This password has appeared in a data leak. Choose a different password.". Classification only, never displayed: "This email address is already in use." → `EmailTaken`.

The Dutch verify sentence comes from the iOS catalog, **not** from the `WizardCopy` board table ("We hebben een link gestuurd naar {email}. Open hem …"), OQ-5d-3.

### 5.10 Shared pieces with 5c (parallel phase) [R]

5c §5.9 lists the pieces shared with 5d; whichever of 5c and 5d is rebased second reuses the other's types and deletes its own duplicate in the rebase commit. Before writing each piece, `grep -rn` the stack top. **If it exists, reuse it with its merged name. If it does not, create it here at 5c's path, with 5c's name and shape**, so 5c finds it.

| Piece | Path and name (5c §5.9) | 5d notes |
|---|---|---|
| `AuthHeadingGlyph { Problem, Check }` and `AuthScreen(headingGlyph: AuthHeadingGlyph? = null)` | `…/auth/shell/AuthHeadingGlyph.kt`, `…/auth/shell/AuthScreen.kt` | §5.5 |
| `AuthPasswordPolicy` (`HINT_KEY` = the OQ 21 English sentence, `@Composable fun hint(): String` resolved through `CopyIndex`) | `…/auth/form/AuthPasswordPolicy.kt` | §4, §5.1 row 4 (`hint = AuthPasswordPolicy.hint()`) |
| `class EmailAppLauncher(context)` with `fun open(): Boolean` | `…/auth/shell/EmailAppLauncher.kt` | §5.8 |
| `fun interface VerifyLinkReceiver { suspend fun receiveVerificationLink(input: SignedVerificationInput?) }` | `…/auth/links/VerifyLinkReceiver.kt` | `VerificationCoordinator` implements it (§5.8) |
| `VerificationLinkInbox`, `VerificationContext`, `VerificationCoordinator` | `…/auth/links/VerificationLinkInbox.kt`, `…/auth/model/VerifyEmailModel.kt`, `…/auth/verify/VerificationCoordinator.kt` | **5d only.** 5c never creates them. If 5c is in the stack, its `AppGraph.authLinkRouting` passes the placeholder `VerifyLinkReceiver { }`; Task 6 replaces it with `auth.verification`. 5c's routing tests (`AuthLinkRouting`, a verify link calls `receiveVerificationLink` once) must still pass unchanged. If 5c is not in the stack, 5c wires `auth.verification` after rebasing (5c §5.6 item 3). |

If this phase creates a 5c piece, it also adds that piece's test from 5c Task 2, with 5c's name and assertions: `AuthHeadingGlyphTest` (`@Layout`, `pixel8`), `EmailAppLauncherTest` (`@Logic`), `AuthPasswordPolicyTest` (`@Logic`), in `feature/auth/src/test/kotlin/net/chronoflow/feature/auth/shell/`.

### 5.11 Navigation summary [F `05 §5`, `§15.5`; R A8]

| From | Action | Goes to |
|---|---|---|
| Sign in | "Create an account" | `navigate(Route.SignUp)` (single-entry root route, OQ-5a-7) |
| Sign up | `auth.back`, `sign-up.back`, system back (`BackHandler`) | `navigate(Route.SignIn)`; the entry is removed, `onCleared` → `model.cancel()`; Sign in keeps its email |
| Sign up | success | 4e `routeAcceptedSession` via `accepted(result, SignUp)`: unverified → `onVerify(epoch)` → `Verify` |
| Verify | `auth.back`, `verify.different-email` | sign out with discard → `SignIn` (back from there → `Welcome`, 5a) |
| Verify | system back | 4e root `onBack` finishes the activity; still signed in, unverified |
| Verify | verified (link, mock mail, web) | gate route: `Terms`, `Walkthrough` or `Today` (the mock's new account → `Walkthrough`, because mock registration accepts the current terms [F `I/plan.md` 11d fix round 3]) |
| Cold start, signed in, unverified | — | `Verify` (4e launch route), model prepared by the entry |
| 403 `email_unverified` from sync | — | `onVerify(epoch)` → `Verify` if not already; never a wipe |

### 5.12 State survival [R plan "state survival"]

- Sign up, rotation or fold: everything is kept (no recreation, A8).
- Sign up, locale or font-scale recreation: name, email and `showsPassword` are kept (the entry-scoped `SignUpViewModel` and its `SignUpModel` survive); both passwords are empty (the screen's `remember` is gone); an in-flight request is cancelled by `leave()` (5a parity).
- Sign up, process death: an empty form (cold launch).
- Verify, recreation: the screen is disposed → `model.cancel()`. The entry calls `prepareVerification()`, which builds a fresh model (waiting, deadline unknown). The next server 429 restores the countdown (OQ-5d-4).
- Nothing in 5d writes to a `Bundle`, `SavedStateHandle` or `rememberSaveable`. The inbox and the link input live in memory only. `SignedVerificationInput.toString()` must redact `hash` and `signature`: 5c Task 1 adds the override if 0d did not (OQ-5c-7); if neither 0d nor 5c has it in the stack, add it here in Task 3 with a JVM assertion that `toString()` contains neither value.

## 6. Kotlin shape [R]

Paths follow 5a §6 (`…/auth/` = `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/`). `model/`, `links/` and `verify/VerificationCoordinator.kt` stay free of Android and Compose imports (`links/VerifyLinkReceiver.kt` too).

```
feature/auth/…/auth/model/SignUpModel.kt             port; built by SignInModel.makeSignUpModel(onBack)
feature/auth/…/auth/model/SignInModel.kt             + makeSignUpModel(onBack)  (edit)
feature/auth/…/auth/model/AuthConsentText.kt         parse(): List<Segment>; AuthPolicy
feature/auth/…/auth/form/AuthPasswordPolicy.kt       5c's, or created here (§5.10)
feature/auth/…/auth/model/VerifyEmailModel.kt        + VerificationContext, VerifyState, VerifyUiState
feature/auth/…/auth/links/VerificationLinkInbox.kt   5d only (§5.7)
feature/auth/…/auth/links/VerifyLinkReceiver.kt      5c's, or created here (§5.10)
feature/auth/…/auth/shell/AuthScreen.kt              + headingGlyph parameter (only if 5c has not added it)
feature/auth/…/auth/shell/AuthHeadingGlyph.kt        5c's, or created here
feature/auth/…/auth/shell/EmailAppLauncher.kt        5c's, or created here
feature/auth/…/auth/signup/AuthConsent.kt            composable + LinkAnnotation
feature/auth/…/auth/signup/PolicyOpener.kt           interface PolicyOpener { fun open(url: String) } + CustomTabPolicyOpener(context)
feature/auth/…/auth/signup/SignUpViewModel.kt        + SignUpUiState
feature/auth/…/auth/signup/SignUpScreen.kt           SignUpScreen(vm, policyOpener) → SignUpContent(state, nameState, emailState, passwordState, confirmationState, callbacks) (stateless)
feature/auth/…/auth/verify/VerificationCoordinator.kt : VerifyLinkReceiver; + VerificationSessionPort, StoredVerificationAccount, VerificationMail
feature/auth/…/auth/verify/VerifyEmailScreen.kt      VerifyEmailScreen(model, openEmailApp) → VerifyEmailContent(state, callbacks) (stateless)
app/…/app/auth/AuthServices.kt                       + verification coordinator member (edit)
app/…/app/auth/VerificationSessionAdapter.kt
app/…/app/RootContent.kt                             SignUp and Verify entries, entry decorators (edit)
app/…/app/AppGraph.kt                                onVerify wiring; authLinkRouting gets auth.verification (edit)
app/src/debug/kotlin/net/chronoflow/debug/MockVerificationMail.kt
app/src/debug/kotlin/net/chronoflow/debug/ScreenSessionFixture.kt   + audit `sign-up` (edit)
```

`AuthMetrics` gains only what is new, from the boards: `toggleGlyph 18`, `toggleGap 8`, `confirmationToToggle 4`, `verifyButtonGap 8`, `differentEmailHorizontal 12`, `problemDisc 48`, `problemGlyph 24`, `problemGap 8`, `failedButtonMinHeight 48`. Add `"AuthSignUp", "AuthTwoFactor"` to its `@BoardMetrics`.

## 7. Tasks

Each task is one commit (Conventional Commits, lower-case imperative, **no trailer**). The plan has three boxes for 5d ("SignUpScreen and ViewModel", "Verify gate", "Tests"); tick each box in the commit that completes its last bullet, as named in brackets. Run `make quick` once before pushing, and the touched test classes with `make targeted` before each commit.

### Task 1: Consent parser, policy opener, shared auth pieces (`feat(auth): add consent links and policy opener`)
- `AuthConsentText`, `AuthPolicy`, `PolicyOpener` + `CustomTabPolicyOpener` (§5.3); the §5.10 5c pieces that do not exist in the stack yet (`AuthPasswordPolicy`, `AuthHeadingGlyph` + `AuthScreen(headingGlyph)`, `EmailAppLauncher`, `VerifyLinkReceiver`), each with its 5c Task 2 test (§5.10); say in the commit body which ones 5c had already landed; `androidx.browser` in the catalog and in plan A1.
- **JVM `AuthConsentTextTest`:** the en and nl strings give `[Plain, Link(Terms, "terms"), Plain, Link(Privacy, "privacy policy"), Plain(".")]` and the nl inner texts `voorwaarden`, `privacybeleid`; an unknown tag stops parsing and keeps the rest as plain text with its tags, with no duplicated prefix: `"a <foo>x</foo>"` → `[Plain("a <foo>x</foo>")]` and `"a <terms>t</terms> b <foo>x</foo>"` → `[Plain("a "), Link(Terms, "t"), Plain(" b <foo>x</foo>")]` (OQ-5d-16); a missing closing tag stops the same way; no tags → one plain segment; `AuthPolicy.Terms.url == "https://chronoflow.net/terms"` and `Privacy.url == "https://chronoflow.net/privacy"`.
- **Robolectric `PolicyOpenerTest` (`@Logic`):** `CustomTabPolicyOpener.open(Terms.url)` starts an `ACTION_VIEW` intent whose data is the URL and which carries the Custom Tabs session extra `android.support.customtabs.extra.SESSION`; with a `start` seam that throws `ActivityNotFoundException` on the first call, the fallback `ACTION_VIEW` intent (same data, no session extra) is started; when both calls throw, no exception escapes. (Robolectric does not throw for an unresolvable intent by default, and both intents resolve the same way, so the failure is injected through the seam, not the package manager.)
- **5c's shared-piece tests** (`AuthHeadingGlyphTest`, `EmailAppLauncherTest`, `AuthPasswordPolicyTest`), only for the pieces this phase created, exactly as 5c Task 2 specifies them. If 5c already landed a piece, do not add a second test for it.

### Task 2: Sign-up model (`feat(auth): port the sign-up model`)
- `SignUpModel` and `SignInModel.makeSignUpModel(onBack)` per §5.4 (no ViewModel yet).
- **JVM `SignUpModelTests`** (Swift suite name; `feature/auth/src/test`, `runTest(timeout = 60.seconds)`; the source is 5a's port of `authTestSource` (`MockDataSource(store = MemoryMockStateStore(), seed = …, delay = { 0.0 }, sleep = {}, debugAppSeed = false, io = <test dispatcher>)`, seeded with the 0d `account/snapshot.json` account, email `demo@example.com`); 5a's `FakeSessionController : SessionAcceptor`, `FakeSecretField`, `RecordingRoutes` (extend it with `back` calls, or add `RecordingSignUpRoutes` beside it); device id `0199a3c0-0000-7000-8000-000000000001`, device name `"test"` unless stated). Swift method names kept; a Swift `@Test(arguments:)` with ≤ 5 rows becomes one plain method per row named `<swiftName><Row>` (4c rule):
  - `registrationUsesDeviceLanguageAndTheExistingSessionController`: name "Nieuw", `new@example.com`, `NewPassword123!` ×2, language `Nl`, device name "Test phone" → one accept, `accepted(result, SignUp)` once, and `AuthNavigation.route(result.account)` is `Verify`; name and email kept, both secrets cleared; `mock.inbox()` has 1 mail; the encoded `RegisterInput` JSON has `"language":"nl"`, `"device_name":"Test phone"`, `"password_confirmation"`, and **no** `draft_token`, `input_revision`, `review_revision` or `guest_secret` key.
  - `serverAloneValidatesAllFieldsAndTakenEmailUsesTheBanner`: blank → `name` field "Please fill this in."; email "bad" → an `email` field message; `demo@example.com` → banner `EmailTaken`; then each of `short`, `lowercase123456!`, `NoNumbersInThis!`, `NoSymbols123456` (fresh model each) → a `password` field message and both secrets cleared.
  - `uiTestSeedReturnsTheExpectedRegistrationBannerOnline` / `…Offline` (source seeded with 4c `MockAppSeed.make(now)`): offline + `new@example.com` → `Offline`; online + `demo@example.com` → `EmailTaken`; no field messages; name and email kept; secrets cleared; not busy.
  - `failuresPreserveNameEmailAndClearPasswordsIncludingKeychainFailure`: `failNext(MockMethod.SignUp, …)` with `Offline`, `Server(500)`, `Throttled(60.0, 429)`, `Validation(password_confirmation: "The passwords don't match yet.")` → each time `form.failure` equals it, secrets cleared, name and email kept; then an accept throwing `SessionFailure.CredentialWrite` → `sessionFailure` set, banner `Server`, a non-null nl announcement.
  - `createdAccountAfterSessionFailureCanRecoverThroughSignIn<Failure>` (one method per 4e `SessionFailure`: `CredentialsUnavailable`, `DatabaseUnavailable`, `CredentialWrite`, `InvalidResult`, `Superseded`): the first submit fails in accept → `Server`, no `accepted` call; the second submit → `EmailTaken` (the account exists), `sessionFailure` null, one mail in the inbox; `backToSignIn()` → `onBack` called once; then a 5a `SignInModel` on the same source with the same email and password → one accept, and `AuthNavigation.route` of the result is `Verify`.
  - `emailTakenPreservesOtherFieldErrorsInTheSameResponse<Row>` (one method each for "Use at least one symbol.", "The passwords don't match yet.", "This password has appeared in a data leak. Choose a different password."): banner `EmailTaken`, `fields == {name: "Please fill this in.", <password or password_confirmation>: <key>}`, secrets cleared, email kept.
  - `unchangedFieldCallbacksPreserveMixedFailureUntilAnEditName` / `…Email` (the password and confirmation rows are in Task 4, because on Android the "changed" check for secrets is `UserEditReporter`): after `form.receive(Validation(email taken + field "Please fill this in."))`, an edit with the same value keeps failure, banner and field; a changed value clears all three.
  - `emailTakenKeepsBannerWhenOtherMessagesNeedTheFallback` (pure mapping): email taken + `password: "unknown English"` + `device_id: "Please fill this in."` + `name: "Please fill this in."` → banner `EmailTaken`, fields `{name}` only.
  - `oneToggleControlsBothPasswordsAndBusyCancellationStopsAcceptance`: device identity suspended on a `CompletableDeferred`; toggle → `showsPassword`; a second submit while busy is ignored (one entry); edits and toggle while busy are ignored; `cancel()` + cancelling the job → no accept, **no banner** (the inactive catch returns early); secrets cleared; `showsPassword == false`.
  - Android-only `committedSessionRoutesAfterCancel`: `accepted` suspended on a `CompletableDeferred` inside the `NonCancellable` block; cancel the submit job; complete the deferred → `accepted` returned, secrets cleared, not busy (5a parity).

### Task 3: Inbox and verify model (`feat(auth): port the email verification model and link inbox`)
- `VerificationContext`, `VerifyEmailModel` per §5.6; `VerificationLinkInbox` per §5.7 (5d creates it).
- **JVM `VerificationLinkInboxTests`** (Swift suite name, created here), all 7 Swift cases by name: `guestLinkIsConsumedOnceByTheMatchingFirstSignIn` (upper-case id still matches), `foreignFirstSignInDropsTheLinkAndCannotApplyItLater`, `verifiedSignInAndRepeatedOldLinksHaveNoVerificationDestination`, `signOutAndSessionReplacementClearPendingInput`, `coalescedUpdatesCannotCarryGuestInputAcrossAMissedSignOut`, `liveForeignAndMalformedInputsDoNotReplaceAMatchingPendingLink`, `foreignGuestLinkLeavesTheNewAccountsFirstVerifyScreenWaiting` (an unverified mock sign-in, then a foreign link → the model activates `Waiting` with `hasStatus`, the account stays unverified, persist and route are never called).
- **JVM `VerifyEmailModelTests`** (a `registered(now)` helper, port of `VerifyEmailModelTests.swift:15-29`: `MockDataSource(store = MemoryMockStateStore(), seed = MockServerState(), now = now, delay = { 0.0 }, sleep = {}, debugAppSeed = false, io = …)`; `signUp(RegisterInput("Name", "new@example.com", "NewPassword123!", "NewPassword123!", deviceId = "0199a3c0-0000-7000-8000-000000000001", deviceName = "test", language = Nl))`; take the last `MockMail.Verification` from `inbox()`). A `VerificationClock` fake starts at `1_800_000_000.0` s and drives **both** the mock's `now` (4c's type is `() -> Instant`: pass `{ Instant.ofEpochMilli((clock.seconds * 1000).toLong()) }`) and the model's `nowSeconds` (`{ clock.seconds }`), as iOS does, so `advance(42)` moves the server's resend window and the countdown together. All 11 Swift cases by name; parameterised ones become `<swiftName><Row>` methods:
  - `signedMockMailPersistsOnceAndRoutesOnlyForMatchingAccountEmailEpoch`: persist once, route once, `Waiting`, the mock now reports verified; after `cancel()`, `activate()` changes nothing.
  - `invalidExpiredForeignAndTamperedLinksUseFailedAndRetryChecksStatus`: for `null`, tampered signature (64 × "0"), `expires = 1`, foreign id → `activate()` gives `Failed`; `check()` gives `Waiting` with `hasStatus`.
  - `resendDeadlineSurvivesIdentityRefreshActivationAndBackgroundTime`: after activate, `canResend`; resend → `Locked` banner, `remaining == 60`; activate → still 60; +42 s, then activate → `remaining == 18`, `!canResend`; resend ignored (inbox still 1); +18 s and `tick()` → `canResend`; resend → `remaining == 60`, no failure, inbox 2. (The nl countdown copy assertion moves to Task 5's `VerifyCopyTest`.)
  - `activationAfterWebVerificationAndEmailAppReturnObserveTheServer`: no route on the first activate; `verifyEmail` on the mock directly, then activate → route once; `openEmail` with `mail = null` calls `open` once (returns false) and does not route again.
  - `staleSessionOrEmailAndCanceledResponsesCannotPersistOrRoute`: `current` returning null / a foreign id / a changed email / epoch + 1 → `!hasStatus`; an in-flight check cancelled through a gated mock sleep → `!hasStatus`, no failure.
  - `responseArrivingAfterEmailChangeCannotWriteAndStaleBackCannotDiscard`: the email changes while the verify request is in flight → no write; `signOut()` afterwards does not call `discard` and the model stays active.
  - `serverOfflineUnverifiedFailureDoesNotLoopAndBackExplicitlyDiscards`: `failNext(MockMethod.VerificationStatus, …)` with `Offline`, `Server(503)`, `EmailUnverified(403)` → `Failed`, banner `Offline` / `Server` / `Server`, one nl announcement each; `signOut()` → discard once, inactive; `fetchIdentity` then throws `Unauthorized(401)`.
  - `signOutFailureIsAnnouncedOncePerAttemptAndReplacedByTheNextFailureCheck` and `…Resend`: discard throws `Offline` → `signOutFailure == Offline`, still active, the announcement is "Needs a connection" exactly once per attempt (twice); then a `Server(503)` check or resend clears `signOutFailure`, banner `Server`, announced.
  - `sessionFailuresAreVisibleOnCheckActivationResendAndSignOut<Failure>` (5 failures, one method each, looping the 4 actions): persist (or discard) throws → `sessionFailure`, banner `Server`, no route, announced; then persist succeeds and `check()` → no failure, route once.
  - `offlineResendShowsAndAnnouncesTheConnectionBanner`: `setOffline(true)` → resend → `Offline`, not busy, `canResend`, the nl announcement "Verbinding nodig".
  - `repeatedSignOutFailuresEachShowAndAnnounceTheConnectionBanner`.

### Task 4: Sign-up screen (`feat(auth): add the sign-up screen`) [ticks "SignUpScreen and ViewModel"]
- `SignUpViewModel`, `SignUpScreen` / `SignUpContent`, `AuthConsent` per §5.1–§5.4. `:app` replaces the `SignUp` placeholder entry in `RootContent` with the screen and the §5.4 factory, and makes sure `entryDecorators` hold `rememberSaveableStateHolderNavEntryDecorator()` then `rememberViewModelStoreNavEntryDecorator()` (add them if 5b/5c have not; OQ-5d-14). 5a's `sign-in.signup` link now opens it. Add the audit case `sign-up` → `navigate(Route.SignUp)` to 4e's `ScreenSessionFixture` step 3 (OQ-4e-11; iOS `AuthSessionServices.swift:266-268`).
- **Robolectric (`@Logic`), exact tag queries:**
  - `SignUpFieldEditTest` (ports `unchangedFieldCallbacksPreserveMixedFailureUntilAnEdit` for password and confirmation, and `unchangedConfirmationPreservesServerFeedbackUntilTheValueChanges`): with `EmailTaken` + a confirmation message shown, `performTextReplacement` of the same confirmation keeps banner, field message and failure; a new value clears them. The model's own clear of the secrets does not clear feedback.
  - `SignUpImeChainTest`: Next moves name → email → password → confirmation; Go on confirmation calls submit once; a second Go while busy is ignored; all fields, the toggle row, the consent and the footer are disabled while busy.
  - `SignUpFailureFocusTest` (port of `testMixedFailureFocusesAndRevealsTheConfirmationMessage`): `Validation(password_confirmation)` + `EmailTaken` → `sign-up.confirmation` is focused and its error row lies inside the scroll viewport; a banner-only failure → no field focused, the banner inside the viewport. Run it once at `pixel8` and once at `largeFont`.
  - `SignUpVisibilityTest` (port of `testSharedVisibilityPreservesValuesAutoFillAndNativeNameTraits`): type `NewPassword123!` into both; toggle → both fields reveal the text, values unchanged, the same nodes (the tags exist once each); toggle back → obscured; `sign-up.visibility` has `Role.Switch` and toggleable state on/off, and its content description is "Show password" in both states; content types: name `PersonFullName`, email `EmailAddress` + `NewUsername`, both passwords `NewPassword`.
  - `SignUpConsentTest`: `sign-up.consent`'s text has exactly two `LinkAnnotation.Url` ranges with the two URLs, over "terms" and "privacy policy" (nl: "voorwaarden", "privacybeleid"); invoking each annotation's listener calls a fake `PolicyOpener` with that URL.
  - `SignUpRecreationTest`: `StateRestorationTester`, type all four, `emulateSavedInstanceStateRestore()` with the same ViewModel → name and email kept, both passwords empty, `showsPassword` kept.
  - `SignUpSavedStateTest`: under a custom `SaveableStateRegistry`, after typing `Secret123!` twice and submitting, `performSave()` flattened to strings contains no `Secret123!`.
  - `SignUpRoutingTest` (real 4e `RootNavigator`, a `SignUpModel` with `onBack = { navigator.navigate(Route.SignIn) }`): `auth.back`, `sign-up.back` and system back (`Espresso.pressBack()` or the `OnBackPressedDispatcher`) each give exactly one `navigate(Route.SignIn)`; no route key contains a password (`Route.SignUp` is a `data object`).
  - `SignUpEntryScopeTest` (`app/src/testDebug`, `@Logic`, `RootContent` with the debug graph): `navigate(SignUp)`, type a name → `navigate(SignIn)` → the `SignUpViewModel` was cleared (`model.cancel()` ran; observe through a spy factory or a debug counter) → `navigate(SignUp)` again shows an empty name; `scenario.recreate()` while on `SignUp` keeps the name and the same ViewModel instance.

### Task 5: Verify screen (`feat(auth): add the verify email screen`)
- `VerifyEmailScreen` / `VerifyEmailContent` per §5.5, with the lifecycle effect, `run` guard, countdown and announcer.
- **Robolectric (`@Logic`):**
  - `VerifyEmailControlTest` (port of `testWaitingCountdownAndFailedHaveNoInputsAndKeepLargeActions`): waiting has `verify.open-email`, `verify.resend`, `verify.different-email`, `auth.back` and no text input; countdown present only when `remaining > 0`; failed has `verify.retry` and `verify.resend` (both ≥ 48 dp visual height), no `verify.different-email`, no `verify.countdown`; Resend is disabled while counting.
  - `VerifyCopyTest`: the countdown for 42 s reads "You can resend in 0:42." (en) and "Je kunt de link over 0:42 opnieuw versturen." (nl); for 600 s, "10:00"; the description interpolates the email in both languages; the sign-out error shows "Needs a connection" for `Offline` and "Something went wrong" for `Server`.
  - `VerifyLifecycleTest`: with a fake model counter, moving the test lifecycle `CREATED → STARTED → CREATED → STARTED` calls `activate()` twice; the tick runs only while started (advance the `TestCoroutineScheduler` by 3 s → 3 ticks); disposing the screen calls `cancel()`.
  - `VerifyCountdownRecompositionTest`: ten ticks recompose the countdown node ten times and the button group zero times (use a recomposition counter `Modifier` in the debug test source).
  - `VerifyAccessibilityTest`: the root `paneTitle` changes from "Check your inbox" to "We couldn't check your email" on failure; `verify.countdown` has no live region; exactly one live-region announcement per failed action.

### Task 6: Coordinator and app wiring (`feat(app): route email verification through the per-account inbox`) [ticks "Verify gate"]
- `VerificationCoordinator`, `VerificationSessionPort` + the `:app` adapter, `VerificationMail` + debug `MockVerificationMail` (wired only when `DataSourceFactory.source == Mock`, through 4e's debug `MockSources` path; release has no reference), `AuthServices.verification`, `AppGraph` wiring; replace 4e's default `onVerify`; the Verify entry in `RootContent`; the 5c hand-off per §5.10 (if 5c is in the stack, `authLinkRouting` gets `auth.verification` in place of `VerifyLinkReceiver { }`, and 5c's `AuthLinkRouting`/`AuthLinkDispatcher` tests still pass unchanged).
- **JVM `VerificationCoordinatorTest`** (fake port with scripted epoch, presence, snapshot, account and an updates `MutableSharedFlow`; recording navigator; real `VerificationLinkInbox`; 4c mock). Names port the iOS app-level cases:
  - `secondAccountConsumesItsOwnMailAfterTheFirstAccountIsWiped` (iOS `AccountRecoveryTests`): account A's model is built; A signs out (epoch + 1) → model cancelled and null; account B signs up → the new model's `mail` returns only B's link; verifying routes B.
  - `unverifiedDifferentEmailReturnsToSignIn`: `signOut()` on the model → `signOutDiscarding(expected)` with the model's snapshot, presence becomes SignedOut → navigates `SignIn`, model null.
  - `tamperedLinkOpensFailedScreen`: `receiveVerificationLink(tampered)` while signed in as that account → navigates `Verify`, model `Failed`.
  - `malformedLinkIsIgnoredAcrossAuthentication`: `receiveVerificationLink(null)` → nothing pending, no navigation (iOS 11d fix round 2).
  - `signedOutLinkWaitsForTheFirstMatchingSignIn`: signed out → `clearSignInSecrets` called once and `SignIn`; then sign-in as the same account (epoch + 1) → `prepareVerification` hands the link to the new model.
  - `verificationRequiredNavigatesOnlyForTheCurrentUnverifiedEpoch`: `onVerify(old epoch)` → nothing; a verified account → nothing; current unverified → `Verify` once; a second call while on `Verify` → no navigation (the revision does not change).
  - `completionRoutesOnceEvenWhenTheGateRouterMovedFirst`: persist true; the route seam sees the navigator already on `Walkthrough` → no navigate; when still on `Verify` → navigate once.
  - `staleBackCannotDiscardAReplacementAccount`: the model's context is epoch 3, the port is at epoch 4 → `signOutDiscarding` never called.
- **Robolectric `VerifyRoutingTest` (`@Logic`, `app/src/testDebug`)** with a real debug `AppGraph` on the mock (`GraphOverrides(sessionCipher = SoftwareSessionCipher())`, 4e T9) seeded through 4e's `TestSeeds.apply` with `CF_AUTH_TEST_ID`, `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_SIGNED_IN=demo` and `CF_AUTH_TEST_UNVERIFIED=1` → route `verify` (`RouteProbe`); if the seeding cannot run under Robolectric, say so in the PR and keep the JVM test plus the emulator journey; tap `auth.back` → route `signIn`, `controller.presence == SignedOut`, the account directory `noBackupFilesDir/ChronoFlowMock/Accounts/0199a3c0-0000-7000-8000-000000000000/` does not exist, and `MockDataSource.fetchIdentity()` with the old token throws `Unauthorized`. System back on Verify does not sign out.

### Task 7: Guards and board captures (`test(auth): guard sign-up and verify states and capture the boards`)
Follow 5a Task 9's lean matrix (the tallest state on every profile, the rest where they are most likely to break), not a full cross product.
- **`SignUpGuardsTest`** (`@Layout`, parameterised, each case its own test) runs 0e `Guards.assertAll(rule, profile, headingTags = setOf("auth.heading"))` on `SignUpContent`, light theme unless stated. States are the 10 iOS ones (`SignUpSnapshotTests.swift:6-57`): `form` (Rick / rick@example.com), `empty` (four "Please fill this in."), `email`, `taken`, `password` ("Use at least 12 characters."), `confirmation`, `visible` (both passwords shown), `server`, `throttle`, `offline`.
  - `empty` (the tallest card) × `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en, nl (10);
  - `form`, `taken`, `server`, `throttle`, `offline`, `visible` on `small`, nl (6);
  - `email`, `password`, `confirmation` on `largeFont`, nl (3);
  - `form` on `pixel8`, en, dark (1).
  - In every `largeFont` case also assert that `sign-up.visibility`, `sign-up.consent` and `sign-up.back` can be scrolled into view (`performScrollTo()`), port of `testAllStatesKeepConsentToggleAndFooterReachableAtXXLarge`.
  - 20 cases.
- **`VerifyGuardsTest`** (`@Layout`), the 10 iOS states (`VerifyEmailSnapshotTests.swift:6-47`): `waiting`, `countdown`, `failed`, `server`, `throttle`, `offline`, `failedServer`, `failedThrottle`, `failedOffline`, `signOutError`, built through the model with a fixed clock as the iOS helper does, email `rick@example.com`:
  - `throttle` (banner + countdown, the tallest) × the five profiles × en, nl (10);
  - the other nine states on `small`, nl (9);
  - `waiting` with the email `a.very.long.address.for.testing.purposes@example-domain.com` on `largeFont`, nl (the description wraps, nothing clips) (1);
  - `failed` on `pixel8`, en, dark (1).
  - 21 cases. Port of `testWaitingFailedAndErrorActionsFitTheLeanDeviceProfiles`.
- Each case < 5 s; record the class run times in the PR.
- **`SignUpVerifyBoardCaptureTest`** (`@Capture`, Robolectric) at 375 × 812 dp, density 3, zero insets, en, via `BoardCapture.capture(rule, name)`, names in 5a's pattern. Sign-up captures fill name "Rick", email "rick@example.com" and password `sunrise-in-tokyo` ×2 in the test's own `TextFieldState`s, as the board does:
  - `AuthSignUp_none_en_light`, `AuthSignUp_none_en_dark`, `AuthSignUp_taken_en_light`, `AuthSignUp_taken_en_dark`, `AuthSignUp_server_en_light`, `AuthSignUp_server_en_dark`;
  - `AuthVerify_en_light`, `AuthVerify_en_dark` (the board's state: the countdown at 42 s, Resend disabled), `AuthVerify_server_en_light` (board `error="server"`);
  - `AuthVerifyFailed_en_light`, `AuthVerifyFailed_en_dark`;
  - adaptations with no board, under `auth/`: `auth/SignUpOffline_en_light`, `auth/VerifyWaiting_en_light` (no countdown), `auth/VerifySignOutError_en_light`.
- No stored Roborazzi references (opt-in only).

### Task 8: Emulator journey (`test(app): sign up and verify through the mock inbox on the emulator`) [ticks "Tests"]
- **`SignUpVerifyJourneyTest.signUpVerifiesThroughTheMockLinkAndOpensTheGate`** (`app/src/androidTest/kotlin/net/chronoflow/app/auth/`, extends 0e `ScreenTest`, `@Smoke`, < 10 s, `timeout_msec=60000`, waits with `waitUntil`/`waitUntilExactlyOneExists`, no sleeps). This is the only emulator test of the phase.
  - Seed `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_AUDIT_SCREEN=sign-up`, `CF_MOCK_DELAY=0`.
  - Wait for `sign-up.name`; type `Traveller`, IME Next; `new@example.com`, Next; `NewPassword123!`, Next; `NewPassword123!`, Go.
  - Wait until the route probe reports `verify` and `verify.open-email` exists (≤ 5 s). Click it.
  - Wait until the route probe reports `walkthrough` (≤ 5 s); assert `verify.open-email` no longer exists.
  - Record the duration in the PR.
- If the worker sandbox cannot run the emulator, report the exact commands (`make emu`, `make targeted ONLY="app:SignUpVerifyJourneyTest"`) and their output; the orchestrator runs them.

### Task 9: Docs (`docs(auth): record sign-up and verify acceptance`)
- Extend `docs/auth.md`: the sign-up and verify tables of §4, the Verify state machine (§5.6 in prose), the coordinator and inbox rules, the 5c hand-off point, which iOS tests were ported (Swift name → Kotlin name) and which were deferred to 11b, the capture list, and the reviewer's board comparison (every difference fixed or recorded with its board name).
- Append OQ-5d-1 … OQ-5d-16 to `plan.md` "Open questions for the user" with the next free `OQ-A` numbers. Add `androidx.browser` to A1 if Task 1 did not.

## 8. Acceptance criteria

1. `make quick` passes (< 60 s warm). `make check` passes on the phase branch with `pixel` booted (the orchestrator runs it if the sandbox cannot).
2. `make targeted ONLY="feature:auth:AuthConsentTextTest feature:auth:SignUpModelTests feature:auth:VerificationLinkInboxTests feature:auth:VerifyEmailModelTests feature:auth:VerificationCoordinatorTest"` is green, every JVM test < 1 s (`build/test-results/*/timings.json`).
3. All Robolectric classes of Tasks 1, 4, 5, 6 and 7 pass, each test < 5 s; the guard counts are 20 (sign-up) and 21 (verify).
4. `make captures ONLY=AuthSignUp,AuthVerify,AuthVerifyFailed,auth/` writes the 14 PNGs of Task 7 to `build/captures/`. The Opus reviewer compared them side by side with the boards rendered at 375 × 812 (both themes, every `error` prop), and every difference is fixed or recorded in `docs/auth.md`. Expected accepted differences only: no dots, the 12-character hint, two consent links, the show/hide switch row, 48 dp targets, underline offset.
5. On `pixel`: `SignUpVerifyJourneyTest` passes in < 10 s, ending on `walkthrough`. A debug run of it is StrictMode-clean (register, mock inbox read and the DB write run off the main thread).
6. After Verify back (Task 6 `VerifyRoutingTest`): signed out, the account directory is gone, the route is `signIn`; back from there goes to `welcome` (5a).
7. `rg -n "rememberSaveable|rememberTextFieldState|SavedStateHandle" feature/auth` finds nothing that holds a name, email, password or link; `rg -n "Log\.|println" feature/auth/src/main` finds no secret; Konsist passes; no `WebView` anywhere (`rg -n WebView app feature core` is empty).
8. `git -C /Users/rickgout/Sites/chronoflow-ios status`, `…/chronoflow-new` and `…/chronoflow-mobile` are clean. Every commit is Conventional Commits with no trailer; the plan boxes "SignUpScreen and ViewModel", "Verify gate" and "Tests" are ticked; the new OQs are in `plan.md`.

Perf: no budget row applies. The countdown recomposes only its own text once per second (Task 5 test); no screen recomposes per IME animation frame (5a rules); Verify does no disk or network work on the main thread.

## 9. Review checklist (Opus reviewer)

- [ ] **Ports are exact.** `SignUpModel.submit` order (lock → identity → register (secrets read here) → `NonCancellable { accept → autofill → clear → accepted(SignUp) }`), the `if (!active) return` at the top of every non-cancellation catch, `cancel`, `togglePassword`; every branch of `VerifyEmailModel` (`isCurrent` checked before and after each await, `input = null` on a validation failure, the email equality checks, `fetchIdentity` before `persist`, `completed`, the deferred `linkPending` re-check, `deadline = max(…)`, `canResend` formula, a failed resend that does not switch to Failed); `VerificationLinkInbox.sessionChanged` with the `firstGuestSignIn` rule; Swift test names kept; deferred guest tests listed in `docs/auth.md`.
- [ ] **No client validation**, no offline pre-check, Create account and Resend never disabled for being offline; Resend disabled only by `canResend`.
- [ ] **Secrets:** both passwords live only in screen `remember { TextFieldState() }` and are cleared after every attempt (success, each failure, session failure, cancel); no password, link `hash`/`signature` or token in a route key, `Bundle`, saved state, log, exception message or data-class `toString()`.
- [ ] **One visibility control** drives both fields on the same `TextFieldState`s (no field swap); the row is `Role.Switch` with a fixed "Show password" label; it uses `CfSwitch`, not a hand-drawn switch.
- [ ] **Consent:** the sign-up key (`…agree_to_9e09cb`), not the guest key; two `LinkAnnotation.Url` spans; Custom Tab with the fallback; only the two policy URLs; no WebView, no `PolicyReader`.
- [ ] **Verify routing:** the drawn back and "Use a different email" call `signOut(expected, discardPending = true)` scoped to the model's snapshot (a stale screen cannot wipe a replacement account); system back does **not** sign out; `onVerify` never re-navigates to Verify while already there; completion routes once and not after the gate router has moved; an unverified 403 never wipes.
- [ ] **Re-check on foreground** uses `repeatOnLifecycle(STARTED)` (not `ON_RESUME` polling or a timer that runs in the background); the tick stops when stopped; the deadline is wall-clock (`System.currentTimeMillis`), not `elapsedRealtime`, as on iOS.
- [ ] **Mock vs real email app:** `VerificationMail` exists only in `src/debug` and only for the mock; release has no reference to `MockDataSource` (`testReleaseUnitTest`); the real intent uses `makeMainSelectorActivity` without `resolveActivity`.
- [ ] **5c overlap:** nothing duplicated; `VerificationLinkInbox` lives once, in `…/auth/links/`, created by 5d; `AuthHeadingGlyph` (+ `AuthScreen(headingGlyph)`), `AuthPasswordPolicy` (`…/auth/form/`), `EmailAppLauncher` and `VerifyLinkReceiver` exist once, with 5c §5.9's names and shapes; `VerificationCoordinator` implements `VerifyLinkReceiver` and is the one entry for verify links; 5c's `authLinkRouting` passes it (no placeholder `VerifyLinkReceiver { }` left); 5c's tests still pass.
- [ ] **Model lifetime:** `SignUpViewModel` is entry-scoped (both entry decorators present, saveable first); leaving `SignUp` runs `onCleared` → `model.cancel()`; a second visit starts empty; recreation keeps name and email; the `SignUpModel` is built only through `SignInModel.makeSignUpModel`. `VerifyEmailModel` is owned by the coordinator, never by a ViewModel.
- [ ] **5a names used, not this brief's old ones:** no `AuthScaffold`, `AuthChallengeHolder`, `AuthFeedback`, `SignUpRouter` or `[Welcome, SignIn, SignUp]` stack anywhere; navigation is `rootNavigator.navigate(...)` on single-entry routes.
- [ ] **Geometry** only from tokens, `AuthMetrics` and `LocalSafeInsets`; heading margin 24 on sign-up and 32 on verify; 4 dp between confirmation and the toggle row; verify buttons gap 8; failed buttons 48 dp; the 560 dp column from 600 dp (inherited); no size written to state.
- [ ] **Strings:** only generated resources, names from `names.lock.json`; `auth_check_your_inbox` (no period) on Verify; the countdown argument is ASCII `m:ss` (`Locale.ROOT`); the Dutch verify sentence is the iOS one.
- [ ] **Tests** at the cheapest tier: logic on the JVM, layout and controls in Robolectric, one emulator journey; parameterised cases split; no `Thread.sleep`, no tree traversal, exact tags.
- [ ] Typical mistakes: `catch (e: Exception)` before a `CancellationException` rethrow; a `SignUpModel` held by an activity-scoped ViewModel (reused, inactive, on the next visit); `persist` without `sessionSync.stop()` first; separate clocks for the mock and the model in `VerifyEmailModelTests`; a `LaunchedEffect` tick loop that keeps running in the background; reading `remaining` high in the tree; `rememberSaveable` for the name or email "for convenience"; a live region on the countdown; navigating to Verify twice and cancelling the model; calling `confirmEmailVerification` without stopping sync first; mapping `This email address is already in use.` to a field message; a placeholder or loading screen invented for a null Verify model.

## 10. Open questions (proceed on the default; append each to `plan.md` with the next free `OQ-A` number)

| # | Question | Default |
|---|---|---|
| OQ-5d-1 | Show/hide control: the board draws an eye button with `role=switch`; iOS ships a row with a native switch (`14-sign-up.png`). | iOS: the eye + label row with a trailing `CfSwitch` (dawn track per the Material mapping; iOS tinted it `primary`). |
| OQ-5d-2 | Gap between Confirm password and the toggle row: board 4 dp (`margin-top: -10px`), iOS 14 dp (card gap). | Board, 4 dp (looks come from the boards; the row's 48 dp height already adds space). |
| OQ-5d-3 | Dutch verify sentence: iOS catalog "We hebben een link naar {email} gestuurd. Open die op deze telefoon om het af te ronden." vs the `WizardCopy` board table "We hebben een link gestuurd naar {email}. Open hem op deze telefoon om af te ronden." | iOS catalog (generated by 0c). Ask the designer to align the board. |
| OQ-5d-4 | Activity recreation (locale or font change) on Verify disposes the model, so the countdown is lost until the next 429. | Accept (iOS rebuilds the model the same way); no saved deadline. |
| OQ-5d-5 | Resend is enabled right after sign-up, although the server just sent a mail (`users/me` has no `resendAfterSeconds`). | iOS parity: the first tap gets the server's 429 and starts the countdown. |
| OQ-5d-6 | `androidx.browser` is not in plan A1. | Add the newest stable version (≥ 1.8.0, older than two weeks) to the catalog and A1. |
| OQ-5d-7 | No browser supports Custom Tabs or none is installed. | Fall back to `ACTION_VIEW`; if that fails, do nothing (no invented error UI). |
| OQ-5d-8 | No email app on the device. | "Open email app" does nothing visible and the waiting state stays (iOS parity). |
| OQ-5d-9 | 5c and 5d both need the heading glyph, `AuthPasswordPolicy`, `EmailAppLauncher` and the verify-link entry. | 5c §5.9's paths, names and shapes; whichever lands second reuses and deletes its duplicate (§5.10). 5d alone owns `VerificationLinkInbox` and the full `receiveVerificationLink`/`synchronizeVerificationSession` port in the coordinator, behind 5c's `VerifyLinkReceiver`. |
| OQ-5d-10 | The guest claim parts of `SignUpModel`. | Deferred to 11b with the three guest tests. |
| OQ-5d-11 | Resend deadline: wall clock (iOS) or monotonic clock. | Wall clock, as iOS ("survives backgrounding"). |
| OQ-5d-12 | Debug builds on the mock: "Open email app" reads the mock inbox instead of launching a mail app. | iOS parity ("Mock Open email consumes the fake server's mail data; no inbox screen"); the real intent runs with `-Pcf.dataSource=api` and in release. |
| OQ-5d-13 | System back on Verify. | Leaves the app, still signed in (05 OQ A4); only the drawn controls sign out. |
| OQ-5d-14 | iOS makes a fresh `SignUpModel` per presentation and cancels it on disappear; 4e's `RootContent` has no ViewModel entry decorator, so a plain `viewModel()` would be activity-scoped and reuse a cancelled model (OQ-5a-5). | Add `rememberSaveableStateHolderNavEntryDecorator()` + `rememberViewModelStoreNavEntryDecorator()` to `RootContent` (5b and 5c chose no ViewModel and no decorator; OQ-5a-5 recommends the first phase that needs entry scope adds it); `SignUpViewModel.onCleared()` cancels the model. 5a's `SignInViewModel` becomes entry-scoped too; 5a's tests must still pass. |
| OQ-5d-15 | Plan 5d task text says the Verify gate "consumes links from the per-account inbox"; 5c drops verify links until 5d lands (`VerifyLinkReceiver { }`). | One implementation: the coordinator's full iOS port (`AuthSessionServices.swift:141-248`), passed to 5c's `AuthLinkRouting` as its `VerifyLinkReceiver`. |
| OQ-5d-16 | iOS `AuthConsent.attributedText` appends the plain prefix before checking the tag, so an unknown tag after plain text duplicates that prefix (`AuthConsent.swift:26-29`). The shipped copy has only `<terms>` and `<privacy>`. | Do not port the duplication: append the prefix only once the tag is known (§5.3). No visible difference with the real copy. |
