# Phase 5c: Password reset and App Links

- **Branch:** `feat/phase-5c-password-reset-links`, from the stack top once **5a** is in it (5a brings 4e, 3c, 2, 0c–0e, 4a–4d). 5b and 5d run in the same wave (9) in their own worktrees. §5.9 says which shared pieces you create or reuse. If 5a is not in the stack, stop and report it.
- **Stack check (2026-11-18, against 41300fa).** Single lane: 5a and 5b are in the stack, 5d has not started, so 5c creates every §5.9 shared piece (`AuthHeadingGlyph`, `AuthPasswordPolicy`, `EmailAppLauncher`, `VerifyLinkReceiver` are all absent). Landed names: `Route.Reset` (data class, `core/navigation/…/Route.kt:52`), `PendingLaunchIntents` (`app/…/app/PendingLaunchIntents.kt`), `LaunchIntentHandler` (`core/navigation/…/PendingLaunchIntent.kt`), `DataSource.requestPasswordReset`/`resetPassword`, `ResetPasswordInput` (`core/model/…/model/api/AuthContracts.kt`). 4e's warm-activity hand-off (16189a6) consumes pending intents on every activity mount. 5b added OQ rows up to OQ-A204; build cache no longer replays test timings (d169a99); Konsist shares a pre-built scope (0848ad4).
- **Implementation preflight (2026-11-18).** Repeated §2 item 6 and §5.9 against the 5b stack. All upstream types and generated copy exist; the four shared pieces remain absent. Mechanical mappings: `DataFailure.Validation(fields, status)`, `Throttled(retryAfterSeconds: Double, status)` and `Unauthorized(status)` require explicit statuses; `AuthServices` already receives the IO-backed `LazyPublicDataSource`, so new models reuse that injected source rather than calling the suspending `dataSources.public()` in a constructor. `:core:testing` now reaches `:core:model` transitively through `:core:plan`; the URL helper still adds no dependency. Task 9 uses the source-taking `MockSources.latestResetLink(source, email)` signature defined in §5.5. Keep 4e's fresh-launch guard on reminder extras when moving the link guard into `capture`. Screens expose stateless state/event content with composition-owned adapters for requests, fields and focus, following AGENTS.md; structural tests inspect tags, while model and resource tests cover state and copy. Route cases live in `CfRoot.kt` (the generic `RootContent` owns only NavDisplay). Real-graph `ForgotRecreationTest` and `ResetRecreationTest` live in `app/src/testDebug`, so `:feature:auth` gains no dependency on `:app`. The landed session teardown and gate observer can both report sign-out after a pending reset opens; preserve an active new Reset route in `AppGraph.onSignedOut`, and dispatch teardown navigation onto the injected main dispatcher. Earlier routes keep their existing behavior; the real signed-in link/sign-out test covers this iOS-required hand-over.
- **Harness follow-up (2026-11-18, orchestrator).** `ActivityScenario` discovers a launched activity through `Intent.filterEquals`; the intended production data scrub changes that identity for VIEW links. Link launches and recreation use `Robolectric.buildActivity`/`ActivityController` in `AuthResetActivityFixture`, preserving every scrub, route and replay assertion. Non-link launches retain ActivityScenario. The production scrub is unchanged.
- **Plan entry:** `plan.md` "Phase 5c: Password reset and App Links" (l.1786–1819). Decisions **A2**, **A6**, **A8**, **A16**, **A23**, and the A25 rows for OQ 21, 22, 25 and 27. Spec sections: "Android spec: navigation and routes", "forms and IME", "manifest and permissions", "seeds and test hooks" and "window policy". Risk 3 (server item S1). Also read AGENTS.md "Working rules", "Tests" and "Traps".
- **Checked 2026-11-16** against iOS at `f391a1fa`, web at `e36368ec` and the final 4e, 5a and 5d briefs. This version replaces the 02:46 draft. Names now follow 4e (`Route.Reset(email)`, `PendingLaunchIntents`, `LaunchIntentHandler`), 5a (`AuthScreen`, `AuthServices`, `SignInRouter`, single-entry root routes, `SecretField`) and 5d (5d owns `VerificationLinkInbox`).
- **Tests:** every test follows AGENTS.md "Tests": the cheapest tier, the budgets, exact tag queries, no `Thread.sleep` or `SystemClock.sleep`, and the hang caps. Tier annotations come from 0e: Robolectric `@Logic`, `@Layout` and `@Capture`, and emulator `@Smoke`. Ported tests keep the Swift test names. A Swift `@Test(arguments:)` with two rows becomes two methods named `<swiftName><Row>` (the 4c rule).
- **Tags:** **[F]** is a fact read in source (path given). **[R]** is a rule this brief sets for Android.
- **Path abbreviations:**
  - `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. Never write there.
  - `Kit/` = `I/Packages/ChronoFlowKit/Sources` and `KitT/` = `I/Packages/ChronoFlowKit/Tests`.
  - `W/` = `/Users/rickgout/Sites/chronoflow-new` at `e36368ec`. `DC/` = `_design/canvas/`.
  - `…/auth/` = `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/`.
  - `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`.
  - `…/model/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`.
- **Names from earlier phases:** before Task 1, `grep` every name in §2 item 6 in the merged code. If a merged name differs, use the merged one and list the mapping in the phase report. Never rename earlier code.

---

## 1. Goal

A signed-out person taps "Forgot your password?" on Sign in and asks for a reset link. The emailed link `https://app.chronoflow.net/reset-password/{token}?email=…` opens the app on **Choose a new password**. Saving shows **Password saved** with the honest copy, and **Continue** goes back to **Sign in**. The 204 creates **no session** (OQ 22 and 27). An invalid, used or expired link shows **This link has expired**, where the person sends a new link from the same screen. Verify links (`/email/verify/…`) are parsed by the same parser and handed to 5d's verification entry.

Done means:
- `AuthLink` is an exact Kotlin port of `Kit/AppCore/AuthLink.swift` and passes the two ported Swift tests plus the Android-only cases in Task 1.
- `MainActivity` declares one `autoVerify` App Link filter for the two path prefixes. 4e's `PendingLaunchIntents` captures links on a cold start and in `onNewIntent`, but never again after an activity recreation or a start from Recents. Auth links are handed over at the iOS point (§5.6).
- A reset link waits while someone is signed in, or while presence is still unknown, and opens once on the next sign-out (`GuestResetLinkRouter` port).
- The four boards are built with the settled copy, in en and nl, light and dark, and pass the guards on every profile.
- `docs/app-links.md` tells the server exactly what to publish (S1).
- One emulator journey runs forgot → mock link → `am start` → reset → done → Sign in.

## 2. Inputs to read (in this order)

1. **This repo:**
   - `AGENTS.md` (all of it) and the `plan.md` sections in the header.
   - `docs/phases/phase-4e-session.md`: §5.1 (`Route.Reset(email)`, `RootNavigator`), §5.8 (`RootContent`, `PendingLaunchIntent`, `PendingLaunchIntents`, `LaunchIntentHandler`, `finishLaunch`), T10 (`LaunchSessionBridge`, `ScreenSessionFixture`), T11 (seeds) and OQ-4e-5, OQ-4e-11 and OQ-4e-13.
   - `docs/phases/phase-5a-sign-in.md`: §5.1 (`AuthScreen`), §5.4 (`AuthBannerKind` with the `Forgot` and `Reset` contexts), §5.6–§5.9 (`AuthFormModel`, `SignInModel`, `AuthNavigation`, `SignInRouter`, `AuthServices`, the auth-exit observer), Task 9 (the guard and capture pattern) and the Appendix.
   - `docs/phases/phase-5d-sign-up-verify.md` §5.10 (the shared pieces).
   - `docs/phases/phase-2-design-system.md` Task 14 (`CfNotice`).
2. **Research:**
   - `docs/research/05-launch-auth.md`: §2.2 (the board values iOS overrides), §8.1–§8.4, §9.4 (Forgot), §9.5 (Reset, expired, done), §9.9 (copy), §11 (links), §15.4–§15.6 (IME, back, App Links), §15.8 (autofill) and §15.11 (TalkBack).
   - `docs/research/03-api-contract.md`: §3 (error envelope), §4 (limits), §5.1 rows 6–7 and §8 (links).
3. **Boards** (read-only). Each one is a 375 × 812 wrapper around `DC/AuthTwoFactor.dc.html`, whose props are `theme` (`light`/`dark`), `screen` and `error` (`none`/`attempts`/`server`). "Sent" is internal click state, not a prop.
   - `DC/AuthForgot.dc.html` → `screen="forgot"`.
   - `DC/AuthReset.dc.html` → `screen="reset"`.
   - `DC/AuthResetDone.dc.html` → `screen="resetDone"`.
   - `DC/AuthResetExpired.dc.html` → `screen="resetExpired"`.
   - `DC/AuthStates.dc.html` is the banner catalogue; 5a built the banner.

   The boards still draw values from before the decisions. §5.1 lists what wins.
4. **iOS** (`I/` at `f391a1fa`):
   - `plan.md:1232` "Phase 11a: Forgot password (`feat/phase-11-account`)" and `plan.md:1255` "Phase 11b: Reset, expired and done (`feat/phase-11b-reset`)".
   - `plan.md:494–503` "Form behavior, routing and accessibility": l.497 has the keyboard and return-chain rules, l.500 the Forgot and Reset rules, l.502 the universal links.
   - `plan.md:2376` "Open questions for the user": OQ 22, 25 and 27, plus 21 (password hint) and 23 (offline banner).
   - `Kit/AppCore/AuthLink.swift`: port it exactly.
   - `Kit/AppCore/PendingLaunchIntent.swift`: read it. Its `urls` set carries debug URLs, not auth links (§5.6).
   - `Kit/Auth/`: `ForgotPasswordModel.swift`, `ForgotPasswordView.swift`, `ResetPasswordModel.swift`, `ResetPasswordView.swift` (including `ResetLinkExpiredView`), `GuestResetLinkRouter.swift`, `AuthNavigation.swift` (`forgotEmail`, `forgot(email:)`), `SignInModel.swift:61–91` (`makeForgotPasswordModel`, `mailAction`, `openReset`, `makeResetPasswordModel`) and `AuthFormModel.swift` (the `.forgot` and `.reset` contexts).
   - `Kit/DesignSystem/AuthInputTraits.swift` (`AuthPasswordPolicy.hint`), `AuthControls.swift:24–33` (`AuthGlyph`, whose `.problem` and `.check` cases are the heading glyphs) and `:265–310` (`AuthNotice`), `AuthTokens.generated.swift`, `Resources/Auth.xcassets/{AuthProblem,AuthCheck}.imageset/*.svg` and `Kit/Auth/AuthScreen.swift:43–70` (the heading glyph).
   - `I/App/SceneDelegate.swift`: l.18–20 and l.51–74 (capture), l.508–532 (debug seeds, `CF_AUTH_TEST_LINK`, consumption at root mount) and l.663–695 (`captureAuthActivities`, `scene(_:continue:)`, `consumePendingAuthLink`).
   - `I/App/Session/AuthSessionServices.swift`: l.116–131 (router and mock mail wiring) and l.251–281 (`prepareUITestRoute`: `forgot`, `reset`).
   - `Kit/DataAccess/Mock/MockDataSource.swift:122–176, 358–393` (mock forgot and reset semantics).
   - Tests to port:
     - `KitT/AppCoreTests/AuthLinkTests.swift`;
     - `KitT/AuthTests/ForgotPasswordModelTests.swift` (9 tests);
     - `KitT/AuthTests/ResetPasswordModelTests.swift` (3);
     - `KitT/AuthTests/GuestResetLinkRouterTests.swift` (3; in Swift they are an `extension ResetPasswordModelTests`, ported as the Kotlin class `GuestResetLinkRouterTests` after the file name; record this in the port map);
     - `I/AppTests/AuthUniversalLinkTests.swift` (2).
5. **Web** (`W/` at `e36368ec`, read-only): `routes/api.php:45–46` (forgot and reset routes, throttles), `app/Providers/FortifyServiceProvider.php:78–91` (link URLs) and `app/Actions/Fortify/ResetUserPassword.php:23–38` (it revokes every token).
6. **Names you build on.** Grep each one.
   - **0d/4b:**
     - `DataSource.requestPasswordReset(email)`, `DataSource.resetPassword(ResetPasswordInput)` and `ResetPasswordInput(token, email, password, passwordConfirmation)`.
     - `SignedVerificationInput(id, hash, expires: Long, signature)`.
     - `DataFailure` (`Offline`, `Throttled(retryAfterSeconds, status)`, `Validation(fields, status)`, `Server(status)`, `Unauthorized`) and `FieldMessage(key, arguments)`.
   - **4c:** `MockDataSource` (`inbox(): List<MockMail>`, `failNext`, `setOffline`, and the `delay`/`sleep` constructor seams), `MockMail.PasswordReset(id, to, sentAt, input: ResetPasswordInput)` and `MemoryMockStateStore`.
   - **4a/4e:**
     - `SessionPresence` (`SignedOut`, `SignedIn(accountId)`, in `net.chronoflow.core.model.session`), `SessionController.presence`, `SessionController.updates: StateFlow<SessionUpdate?>` and `SessionUpdate(presence, epoch)`.
     - `Route.{SignIn, Forgot, Reset(email), Verify}`, `RootNavigator.navigate`, `RouteProbe` and `RootContent`.
     - `PendingLaunchIntent`, `PendingLaunchIntents` (`capture`, `consume`), `LaunchIntentHandler`, `LaunchSessionBridge`, `finishLaunch`, `ScreenSessionFixture`, `MockSources` and `PerfMockSources`.
     - `AppGraph` (`dispatchers`, `appScope`, `controller`, `rootNavigator`, `pendingLaunchIntents`, `selection: DataSourceFactory`, `dataSources.public()`, and the `MockSources?` it obtains from the variant hook) and `TestSeed`/`TestSeeds`.
     - `MockSources` is a **`fun interface`** with the single abstract method `dataSource(root, overrides, bearer): DataSource` (4e §5.6). Do not add a second abstract method (§5.5).
   - **5a:**
     - `AuthScreen(scrollTag, title, description, onBack, showsBack, compact, enabled, announcementRevision, takeAnnouncement, card, footer)`, `AuthBanner`, `AuthAnnouncer`, `AuthFailureFeedback`, `AuthKeyboardReveal`, `UserEditReporter`, `SecretField` and `TextFieldSecret`.
     - `AuthFormModel` (`begin`, `edit`, `receive`, `finish`, `clear`, `takeAnnouncement`, `state`), `AuthFormContext.{Forgot, Reset}`, `AuthFormModel.Action.{SendResetLink, Reset}`, `AuthBannerKind.title(context)`/`body(context)` and `AuthFieldCopy`.
     - `AuthNavigation` (`challengeID`, `reset()`; 5a left `forgotEmail` to 5c), `SignInModel` (`navigation`, `clearSecrets()`), `SignInRouter.forgot()` and `AuthServices` (owner of `signIn`, the router and the auth-exit observer).
     - The test fakes in `feature/auth/src/test/…/testing/`.
   - **2:** `CfButton` (`loadingDescription`), `CfTextField`, `CfSecureTextField`, `CfCard`, `CfNotice(title, message, modifier, tone = Info)`, `LucideGlyphs`, `AuthTokens` and `CfTypeScale.auth*`.
   - **0c:** every string below is in the generated `auth` table. Find each resource name in `tools/l10n/names.lock.json` or with `CopyIndex.find(CopyTable.Auth, "<English key>")`. Never guess a slug. If a key is missing, stop and report it.

**API endpoints** [F `03 §5.1` rows 6–7]. The base is `https://api.chronoflow.net/v1`. Both endpoints are public (no bearer) and use the 15 s timeout.

| Call | Request | Success | Errors |
|---|---|---|---|
| `POST auth/forgot-password` | `{"email": "<as typed>"}` | **204 for any address** (no enumeration) | 422 `errors.email`: `Please fill this in.` or `Enter a valid email address, like name@example.com.`. 429 `throttled` (`api-password-email`: 1/min per email + IP, 5/min per IP). |
| `POST auth/reset-password` | `{"token","email","password","password_confirmation"}`, from the link and the two fields | **204 with no session.** The server revokes every token and web session of that user. | 422 `errors.token` = `This password reset link is invalid or has expired.` → the expired view. 422 password-policy keys on `password`, and `The passwords don't match yet.` on `password_confirmation`. 429 (`api-password-reset`: 5/min per IP). |

## 3. Out of scope

| Item | Owner |
|---|---|
| `VerificationLinkInbox`, `VerificationCoordinator.receiveVerificationLink`, the Verify screens and `POST email/verify` | 5d. 5c hands verify links to a `VerifyLinkReceiver` seam (§5.6). |
| Sign-up and its policy hint | 5d. 5c creates `AuthPasswordPolicy` only if 5d has not (§5.9). |
| Cancelling a two-factor model in `openReset` | 5b. If 5b is in the stack, `SignInModel.prepareReset()` also cancels it, as iOS `openReset` does. |
| Forgot password while signed in (`PasswordPromptForgot`) | 10c |
| Notification taps (`PendingLaunchIntent.reminder`) and reminder extras across recreation | 12b |
| Server `assetlinks.json` (S1) | Server. 5c documents it in `docs/app-links.md`. |
| A "paste a link" test screen (`03 §8` [R]) | Not built: it has no board. |
| `AuthEmailChanged` | Out of scope (A24). |
| A real auth back stack with predictive cross-screen back | Later (OQ-5a-7). 5c uses explicit `navigate(Route.SignIn)`. |

## 4. Platform facts [F unless marked]

- **App Link verification.**
  - `<intent-filter android:autoVerify="true">` with `VIEW`, `DEFAULT`, `BROWSABLE` and `https` data is verified against `https://<host>/.well-known/assetlinks.json`.
  - On Android 12 and later, an unverified domain does not open the app from an implicit browsable intent; the browser opens it instead.
  - An intent with an explicit package still resolves to the app's matching filter. That is how the tests deliver links.
  - The server serves no `assetlinks.json` today (`03 §8`), so real emails open the web reset page, which works.
- **`singleTask`**, already set on `MainActivity`. A link arriving while the activity exists comes in through `onNewIntent`. A link on a cold start is the launch intent in `onCreate`.
- **Re-delivery traps.**
  - A configuration recreation (`locale`, `fontScale` and `layoutDirection` still recreate, A8) runs `onCreate` again with the same intent and `savedInstanceState != null`.
  - A start from Recents after process death delivers the original intent with `Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` (`0x00100000`).
  - Neither case may replay a link.
- **Query decoding.** `android.net.Uri.getQueryParameter` and `java.net.URLDecoder` both decode `+` as a space. Foundation's `URLComponents.queryItems` does not. That is why the parser lives in `:core:model` (JVM) and works on `java.net.URI` raw components with its own percent decoder.
- **UUIDs.** `java.util.UUID.fromString` is lenient: it accepts `1-1-1-1-1`. Swift `UUID(uuidString:)` accepts only the 8-4-4-4-12 hex form.
- **Shell commands.** `UiAutomation.executeShellCommand` runs no shell: it splits on spaces and does not strip quotes. Pass the URL unquoted there, and quote it when typing `adb shell` on the host.
- **Nav3 saved state** (4e §4). A route key's `toString()` goes into the saved-state `Bundle`. `Route.Reset` therefore carries only `email`, and the token lives in memory (OQ-4e-5).
- **Activity-scoped ViewModels** (OQ-5a-5). 4e's `RootContent` has no ViewModel entry decorator, so a `viewModel()` would outlive the route and keep a stale model. That is why §5.5 uses no ViewModel for these screens.

## 5. Spec

### 5.1 Board values vs settled values [F `05 §2.2`, `I/plan.md:500`, OQ 21, 22 and 27]

| Board (`DC/AuthTwoFactor.dc.html`) | Build this |
|---|---|
| `pattern-dots` at opacity 0.2 | No dots: 5a's plain sky. |
| Forgot email prefilled `rick@example.com` | Forgot starts **empty**. Expired is prefilled with the **link's** email. |
| Forgot sent notice "If rick@example.com has an account, …" | The **submitted** email (`sentEmail`), never a sample. |
| Board buttons `h-10` | 5a's `CfButton` (a 40 dp visual with a 48 dp target). Expired "Send a new link" and Done "Continue" use `AuthTokens.footerHeight` (48) as their minimum height. |
| Forgot banner `attempts`: "Too many attempts." / "Wait a few minutes before you try again." | Forgot context: **"Too many requests."** / **"Wait a few minutes before you ask for another link."** (5a `AuthBannerKind`). |
| Reset sub: "You'll be signed in on this phone once it's saved." | **"Sign in with your new password once it's saved."** |
| Reset "New password" hint "At least 10 characters." | **"Use at least 12 characters, with uppercase and lowercase letters, numbers and symbols. Choose a password that has not appeared in a data leak."** (OQ 21 supersedes the board hint; `AuthPasswordPolicy.hint`) |
| Reset button "Save and sign in" → Today | **"Save new password"**, with no session. |
| Done sub: "You're signed in on this phone. Use your new password next time, here and on the web." | **"Password updated. Sign in with your new password."** |
| Done muted line: "Other devices stay signed in. …" | **"Other devices are signed out. If you didn't make this change, reset your password again and contact us."** |
| Done "Continue" → Today | **Continue → Sign in.** |
| Expired "Send a new link" → the `AuthForgot` board | It sends **directly** from the expired screen, then shows the Forgot sent notice in place (iOS `ResetLinkExpiredView` = `ForgotPasswordView(expired: true)`). |
| Board back chevron, `backHref ''` on reset and resetDone | 5a's `auth.back`. Reset form and Done use `showsBack = false`: an empty 48 dp slot, with the brand still centred. |

### 5.2 Screens, test tags and layout [F `ForgotPasswordView.swift`, `ResetPasswordView.swift`, `AuthScreen.swift`]

All four screens use 5a's `AuthScreen`. Shell tags from 5a: `auth.back`, `auth.brand`, `auth.heading`, `auth.subtitle` and `auth.banner`.

| Screen / state | `scrollTag` | Back | Glyph | Title | Subtitle | Card, top to bottom |
|---|---|---|---|---|---|---|
| Forgot, form | `forgot.scroll` | shown → `backToSignIn()` | — | Reset your password | Enter your email and we'll send you a link to choose a new one. | Banner if any. `CfTextField` "Email" `forgot.email`. Primary "Send reset link" `forgot.submit`. |
| Forgot, sent | `forgot.scroll` | shown | — | Reset your password | (same) | Notice `forgot.notice`: "Check your inbox." + "If %1$s has an account, a link to choose a new password is on its way." with the **submitted** email. Primary "Open email app" `forgot.open-email`. Ghost "Back to sign in" `forgot.back`. |
| Expired, form | `forgot.scroll` | shown | `Problem` | This link has expired | Reset links work once and expire after an hour. We can send you a new one. | Banner if any. "Email" `forgot.email` (the link's email, editable). Primary "Send a new link" `forgot.submit` at min height 48. Ghost "Back to sign in" `forgot.back`. |
| Expired, sent | `forgot.scroll` | shown | — | Reset your password | Enter your email and we'll send you a link to choose a new one. | Same as Forgot, sent. |
| Reset, form | `reset.scroll` | `showsBack = false` | — | Choose a new password | Sign in with your new password once it's saved. | Banner if any. `CfSecureTextField` "New password" `reset.password` with the policy hint below it. `CfSecureTextField` "Confirm new password" `reset.confirmation`. Primary "Save new password" `reset.submit`. |
| Reset done | `reset.scroll` | `showsBack = false` | `Check` | Password saved | Password updated. Sign in with your new password. | Muted text (`CfTypeScale.authHint`, `mutedForeground`): "Other devices are signed out. If you didn't make this change, reset your password again and contact us." Primary "Continue" `reset.continue` at min height 48. |

The title, subtitle and glyph rules come from `ForgotPasswordView.swift:31–36`:
- title = `expired && sentEmail == null` ? "This link has expired" : "Reset your password";
- glyph = `Problem` only in that same case.

There is no footer on any of the four (`footer = null`).

**Heading glyph** [F `AuthScreen.swift:49–59`, `AuthControls.swift:24–33`, `DC/AuthTwoFactor.dc.html` `isProblem`/`isDoneIcon` spans, `AuthTokens`]. Add `headingGlyph: AuthHeadingGlyph? = null` to 5a's `AuthScreen`, unless 5d has already added it (§5.9). `enum class AuthHeadingGlyph { Problem, Check }`: the two heading cases of Swift `AuthGlyph` (`.problem`, `.check`), keeping the Swift case names. If 5d already landed it as `Done`, keep 5d's name and record the mapping.
- The glyph is a `headingBadge` (48 dp) circle with a `headingGlyph` (24 dp) lucide glyph centred inside. It is the **first child of 5a's heading block** (so the block's top padding of 32 is above it), **leading-aligned** (its start edge is the title's start edge; iOS `VStack(alignment: .leading)`, board flex column with a fixed 48 px width). Below it: an extra bottom padding of `fieldGap` (8 dp), then the heading block's own gap of `fieldGap` (8 dp). So the title's top = circle bottom + 16 dp.
- It is hidden from accessibility (`clearAndSetSemantics {}`).
- `Problem`: circle `card`, lucide `triangle-alert` (iOS `AuthProblem.svg`), stroke 2, `destructive`.
- `Check`: circle `dawn`, lucide `check` (`M20 6 9 17l-5-5`, iOS `AuthCheck.svg`), stroke 2.5, `dawnForeground`.
- The values come from `AuthTokens` and `CfColors` only. If a token is missing, stop and report it (0b missed it).

**Notice:** use Phase 2's `CfNotice(title = "Check your inbox.", message = <resetSent>, tone = Info)`, with `Modifier.testTag("forgot.notice")`. Do not build a second notice.

### 5.3 Copy [F `Kit/Auth/Resources/Localizable.xcstrings` at the pin, generated by 0c]

**This phase adds no copy** and leaves `tools/l10n/android-only.json` unchanged. Labels use `stringResource`. Banner and field keys go through 5a's `AuthBannerKind` and `AuthFieldCopy` with the context `Forgot` or `Reset`.

| en key | nl |
|---|---|
| Reset your password | Stel je wachtwoord opnieuw in |
| Enter your email and we'll send you a link to choose a new one. | Vul je e-mailadres in. We sturen je een link om een nieuw wachtwoord te kiezen. |
| Email | E-mailadres |
| Send reset link | Verstuur resetlink |
| Check your inbox. (**with** the period; 5d's verify title "Check your inbox" has none and is a different resource) | Bekijk je inbox. |
| If %@ has an account, a link to choose a new password is on its way. (generated as `%1$s`) | Als er een account bestaat voor %@, is er een link onderweg om een nieuw wachtwoord te kiezen. |
| Open email app | Open je e-mailapp |
| Back to sign in | Terug naar inloggen |
| Choose a new password | Kies een nieuw wachtwoord |
| Sign in with your new password once it's saved. | Log in met je nieuwe wachtwoord zodra het is opgeslagen. |
| New password / Confirm new password | Nieuw wachtwoord / Bevestig je nieuwe wachtwoord |
| `AuthPasswordPolicy.hint` (the OQ 21 sentence in §5.1) | its catalog nl |
| Save new password | Nieuw wachtwoord opslaan |
| This link has expired | Deze link is verlopen |
| Reset links work once and expire after an hour. We can send you a new one. | Een herstellink werkt één keer en verloopt na een uur. We kunnen je een nieuwe sturen. |
| Send a new link | Nieuwe link sturen |
| Password saved | Wachtwoord opgeslagen |
| Password updated. Sign in with your new password. | Je wachtwoord is gewijzigd. Log in met je nieuwe wachtwoord. |
| Other devices are signed out. If you didn't make this change, reset your password again and contact us. | Andere apparaten zijn uitgelogd. Heb je dit niet zelf gedaan? Herstel je wachtwoord dan opnieuw en neem contact met ons op. |
| Continue | Doorgaan |

The banners come from 5a's table:
- offline: "Needs a connection", title only;
- throttled: Forgot "Too many requests." / "Wait a few minutes before you ask for another link.", Reset "Too many attempts." / "Wait a few minutes before you try again.";
- server: "Something went wrong on our side." + Forgot "Try again in a moment." / Reset "Your new password wasn't saved. Try again in a moment.".

### 5.4 Models [F `ForgotPasswordModel.swift`, `ResetPasswordModel.swift`, `SignInModel.swift:61–91`]

Both models are plain Kotlin in `…/auth/model/`, main-confined like 5a's `SignInModel`, and expose `StateFlow`s. They hold **no password**: passwords stay in the screen's `remember { TextFieldState() }` and reach the model as 5a `SecretField`s.

**`ForgotPasswordModel(source: DataSource, navigation: AuthNavigation, toSignIn: () -> Unit, email: String = "", mail: (suspend (String) -> Boolean)? = null)`**
- `form = AuthFormModel(displayedFields = setOf("email"), context = AuthFormContext.Forgot)`. It exposes `email: StateFlow<String>`, `sentEmail: StateFlow<String?>` and `isActive`.
- `editEmail(value)`: ignored when inactive, busy or `value == email.value`. Otherwise set it and call `form.edit("email")`.
- `suspend fun submit()`:
  1. Return when inactive, when `sentEmail.value != null`, or when `!form.begin(Action.SendResetLink)`.
  2. Set `submitted = email.value` **before** the request.
  3. `try`: `ensureActive()`; `source.requestPasswordReset(submitted)`; `ensureActive()`; `if (!active) return`; `sentEmail.value = submitted`.
  4. `catch (e: CancellationException)` → rethrow. `catch (e: DataFailure)` → `form.receive(e)` if still active. Any other `Exception` → `form.receive(DataFailure.Server(null))` if still active. List each type; never put a bare `catch (e: Exception)` before the cancellation case.
  5. `finally`: `form.finish()`.
  6. There is **no client validation**: blank and malformed emails go to the server.
- `suspend fun openEmail(open: () -> Boolean)`: only when active and `sentEmail.value != null`. With a `mail` seam, call `mail(sentEmail.value!!)` and never `open`. Otherwise call `open()` once.
- `backToSignIn()` = `cancel()`; `navigation.reset()`; `toSignIn()`.
- `cancel()` = `active = false`; `form.clear()`.

**`ResetPasswordModel(link: ResetPasswordLink, source: DataSource, navigation: AuthNavigation, toSignIn: () -> Unit, mail: (suspend (String) -> Boolean)? = null)`**
- `form = AuthFormModel(displayedFields = setOf("password", "password_confirmation"), context = AuthFormContext.Reset)`.
- `recovery = ForgotPasswordModel(source, navigation, toSignIn, email = link.email, mail)`.
- `state: StateFlow<State>` with `enum class State { Form, Expired, Done }`. The initial value is `if (link.isValid) Form else Expired`.
- `editPassword()` and `editConfirmation()`: ignored when inactive or busy, else `form.edit("password")` or `form.edit("password_confirmation")`. The "changed" check is 5a's `UserEditReporter`, as for Sign in.
- `suspend fun submit(password: SecretField, confirmation: SecretField)`:
  1. Return when inactive, when `state != Form`, or when `!form.begin(Action.Reset)`.
  2. `try`: `ensureActive()`; `source.resetPassword(ResetPasswordInput(link.token, link.email, password.read(), confirmation.read()))`; `ensureActive()`; `if (!active) return`; `state.value = Done`.
  3. A `DataFailure.Validation` whose `fields.values` contains a `FieldMessage` with key `This password reset link is invalid or has expired.` → `state.value = Expired`. The recovery model keeps `link.email`.
  4. Any other `DataFailure` → `form.receive(e)`. Any other non-cancellation `Exception` → `form.receive(Server(null))`. Both only when still active.
  5. `finally`: `password.clear()`; `confirmation.clear()`; `form.finish()`. Both passwords are cleared after **every** attempt.
- It **never** touches `SessionController`, autofill commit, storage or `RootNavigator` beyond `toSignIn`.
- `backToSignIn()` = `cancel()`; `navigation.reset()`; `toSignIn()`.
- `cancel()` = `active = false`; `form.clear()`; `recovery.cancel()`.

**`AuthNavigation.forgotEmail`** [F `AuthNavigation.swift:13–18, 33–37`; 5a §3 left it to 5c]. Add to 5a's `AuthNavigation`: `var forgotEmail: String = ""` (private set), `fun forgot(email: String = "") { forgotEmail = email }` (the iOS `path.append(.forgot)` is replaced by the root route, so it only stores the email), and make `reset()` also set `forgotEmail = ""`. Production always calls `forgot()` with no argument (iOS `SignInView.swift:104`, `AuthSessionServices.swift:271`); the argument exists for the ported tests. Extend 5a's `AuthNavigationTests` with one case: `forgot("a@b.c")` stores it and `reset()` clears it.

**`SignInModel.prepareReset()`**, new in 5a's model: `navigation.reset()`. If 5b is in the stack, also cancel and drop its two-factor model. Port of the first half of iOS `openReset`. The sign-in password is the Sign in screen's `TextFieldState`, which is disposed when the route changes.

### 5.5 Ownership, routes and screens [R; F `SignInModel.swift:82–91`, `AuthSessionServices.swift:116–131`]

**Ownership.** `AuthServices` (5a) owns the two models process-wide, as it owns `SignInModel`. This is the in-memory "`AuthLinkHolder`" that OQ-4e-5 names; there is no separate class.
- `var forgotModel: ForgotPasswordModel?` (private set) and `fun startForgot(): ForgotPasswordModel`. `startForgot` cancels the old model and builds `ForgotPasswordModel(source = dataSources.public(), navigation = signIn.navigation, toSignIn = ::toSignIn, email = signIn.navigation.forgotEmail, mail = mockMail)` (iOS `makeForgotPasswordModel`, `SignInModel.swift:61–63`; the value is `""` in production).
- `var resetModel: ResetPasswordModel?` (private set) and `fun openReset(link: ResetPasswordLink)`, which runs in this order:
  1. `signIn.prepareReset()`;
  2. `forgotModel?.cancel(); forgotModel = null`;
  3. `resetModel?.cancel()`;
  4. `resetModel = ResetPasswordModel(link, dataSources.public(), signIn.navigation, ::toSignIn, mockMail)`;
  5. `rootNavigator.navigate(Route.Reset(link.email))`.
- `private fun toSignIn() = rootNavigator.navigate(Route.SignIn)`.
- `SignInRouter.forgot()`, from 5a, becomes `signIn.navigation.forgot()`, then `startForgot()`, then `navigate(Route.Forgot)`.
- **Auth-exit observer** (5a §5.9), extended:
  - When a collected state arrives and the **current** route (`rootNavigator.state.value.route`, read at that moment, not the collected value) is not `Forgot`, call `forgotModel?.cancel()` and set it to null.
  - When the current route is not `Reset`, do the same for `resetModel`.
  - Read the current value because the collector runs after the navigation that triggered it: the link `open` navigates `SignIn` and then `openReset` builds the new `resetModel` and navigates `Reset` in the same main-thread turn. Acting on a stale `SignIn` emission would cancel and null the **new** model, and the `Reset` entry would then fall back to Sign in. `openReset` and `startForgot` already cancel the model they replace.
  - This is the iOS `onDisappear { model.cancel() }`.
  - An activity recreation does not change the route, so the models and their state survive recreation.
- **`mockMail`** (debug, mock data source only; OQ-5c-5).
  - 4e's `MockSources` is a `fun interface`, so add **no** abstract method. Add a member with a default body: `suspend fun latestResetLink(source: DataSource, email: String): ResetPasswordLink? = null`.
  - `PerfMockSources` overrides it: `(source as? MockDataSource)?.inbox()`, the **last** `MockMail.PasswordReset` whose `to.lowercase() == email.trim().lowercase()`, mapped to `ResetPasswordLink(input.token, input.email)`; else `null`. (`to` is the account's stored email, `MockDataSource.swift:368`.)
  - `AppGraph` keeps the `MockSources?` it already gets from the variant hook as `val mockSources: MockSources?` (grep `MockSources` in `AppGraph.kt`; if 4e already holds it under another name, use that name). Release has no `MockSources`, so it is `null`.
  - `mockMail` is non-null only when `selection.source == DataSourceFactory.Source.Mock` and `mockSources != null` (use 4e's exact `selection` names). It is `{ email -> val link = mockSources.latestResetLink(dataSources.public(), email) ?: return@… false; openReset(link); true }`.
  - The release twin stays `null`. This mirrors `AuthSessionServices.swift:120–131`.

**RootContent entries** (`…/app/RootContent.kt`, next to 5a's `SignIn` entry):
- `Route.Forgot` → `ForgotPasswordScreen(model = remember { auth.forgotModel ?: auth.startForgot() }, expired = false, openEmailApp = EmailAppLauncher(context)::open)`. The `startForgot()` fallback serves the 4e `forgot` audit seed, which navigates straight to `Forgot`.
- `Route.Reset` → `ResetPasswordScreen(model, openEmailApp)` with `model = auth.resetModel`. If it is `null` (only possible if code navigates to `Reset` without `openReset`), draw the plain sky only and run `LaunchedEffect(Unit) { rootNavigator.navigate(Route.SignIn) }`. Do not invent a screen.
- **No ViewModels** for these two screens (§4). Each screen ports the iOS `@State request`:
  - `val scope = rememberCoroutineScope()` and `var request by remember { mutableStateOf<Job?>(null) }`;
  - `submit()` does `if (request != null || form.busy) return`, then `request = scope.launch { try { model.submit(…) } finally { request = null } }`;
  - leaving composition cancels `scope`, and with it the request.
  - A recreation therefore cancels an in-flight request (the model drops the late result) and keeps everything else.

**`ForgotPasswordScreen(model, expired: Boolean, openEmailApp: () -> Boolean)`** in `…/auth/forgot/` is stateless apart from the request holder, `remember { TextFieldState(model.email.value) }` and focus.
- The email field: `KeyboardType.Email`, `ImeAction.Send` → submit, autocorrect off, `KeyboardCapitalization.None`, `ContentType.EmailAddress`. Edits go through `UserEditReporter` → `model.editEmail(text)`.
- Busy: the whole screen is disabled through `AuthScreen(enabled = !busy)` and the card. Only the pending button shows the spinner, and its label and size stay the same. Tap and IME together submit once.
- Back: `auth.back` and system back (`BackHandler`, always enabled) cancel the request, clear focus and call `model.backToSignIn()`. `forgot.back` does the same.
- `forgot.open-email` → `scope.launch { model.openEmail(openEmailApp) }`.
- Announcements: failures go through 5a's `AuthAnnouncer` + `form.takeAnnouncement` (the `AuthScreen` `announcementRevision`/`takeAnnouncement` parameters). When `sentEmail` becomes non-null, clear focus (`focusManager.clearFocus()`, iOS l.90) and announce "Check your inbox." + " " + the sent message once, through the same `AuthAnnouncer` (iOS l.92).
- Failure focus: 5a's `AuthFailureFeedback`. The first invalid field is `forgot.email`. A banner-only failure brings the banner into view.
- The keyboard reveal is 5a's `AuthKeyboardReveal` for `forgot.email` + `forgot.submit`.

**`ResetPasswordScreen(model, openEmailApp)`** in `…/auth/reset/`.
- `state == Expired` → `ForgotPasswordScreen(model.recovery, expired = true, openEmailApp)`.
- Otherwise it shows the Reset form or Done, per §5.2.
- Passwords: `remember { TextFieldState() }` ×2 on `CfSecureTextField`, with `ContentType.NewPassword`, `KeyboardType.Password`, `ImeAction.Next` on the first (focus → `reset.confirmation`) and `ImeAction.Go` on the second (submit). Pass them as `TextFieldSecret(state)`. **Never** `rememberTextFieldState()` or `rememberSaveable`.
- The hint below `reset.password` is the policy hint (`AuthPasswordPolicy`, §5.9). Field errors come from `form.fields["password"]` and `["password_confirmation"]` through `AuthFieldCopy`. The first invalid field is `reset.password`, then `reset.confirmation`.
- Back has no drawn control. System back (`BackHandler`) on the form and on Done cancels the request, clears focus and calls `model.backToSignIn()`. `reset.continue` does the same (Continue = back, iOS `back()`).
- On every `state` change: `focusManager.clearFocus()`, and the title becomes the screen's `paneTitle`. On the change to `Done`, announce "Password updated. Sign in with your new password." once through `AuthAnnouncer`.

**`EmailAppLauncher(context).open(): Boolean`** (§5.9) runs `startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(FLAG_ACTIVITY_NEW_TASK))`. It returns `true` on success and `false` on `ActivityNotFoundException`. With no email app nothing visible happens: the notice stays, with no toast and no error.

**Secrets.**
- The token and the link email live only in `ResetPasswordModel.link`, held by `AuthServices`.
- They never go into a `Bundle`, `SavedStateHandle`, `rememberSaveable`, a route key other than `Reset(email)`, the database, the outbox or a log.
- `ResetPasswordLink.toString()` prints `ResetPasswordLink(token=<redacted>, email=<redacted>)`. If 0d's `SignedVerificationInput` has no redacting `toString()`, add one in Task 1 (OQ-5c-7).
- Process death means a cold launch, and a pending link is lost. iOS keeps it only in memory too.

### 5.6 Link capture, hand-over and routing [F `SceneDelegate.swift:508–532, 663–695`; `GuestResetLinkRouter.swift`; R where marked]

On iOS, auth links are separate from `PendingLaunchIntent.urls`. `SceneDelegate.pendingAuthLink` captures the latest parsed link, cold or warm. It is consumed at **root mount**: after the launch route is applied and before the overlay finishes (l.532). After that, it is consumed at once on every new activity (l.674–677). Android maps this onto 4e's pieces as follows.

**1. Capture (edit 4e's `PendingLaunchIntents`, `…/app/PendingLaunchIntents.kt`).**
- Change the signature to `capture(intent: Intent, restored: Boolean)`. `MainActivity.onCreate` passes `savedInstanceState != null`, and `onNewIntent` passes `false`. Update 4e's existing call sites and its `PendingLaunchIntentsTest` to `capture(intent, restored = false)`; change none of its assertions.
- Add **`fun captureLink(url: String)`**, the Android-free part: it keeps `url` only when it starts with `https:` and appends it in capture order (below). `capture(intent, restored)` calls it with the policy result. The `CF_AUTH_TEST_LINK` seed (Task 8) and the JVM tests of Task 8 use it directly, so they need no `Intent`.
- The link part now goes through the pure **`AuthLinkIntentPolicy.linkToCapture(action: String?, dataString: String?, flags: Int, restored: Boolean): String?`** (`…/app/links/AuthLinkIntentPolicy.kt`). It returns `dataString` only when all of these hold:
  - `!restored`;
  - `action == "android.intent.action.VIEW"`;
  - `flags and 0x00100000 == 0` (`FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`);
  - `dataString != null`;
  - the data starts with `https:`, as 4e already requires.
- The reminder extras stay as 4e wrote them (12b).
- After cold capture, `MainActivity.onCreate` replaces its launch intent with `setIntent(Intent(intent).setData(null))`. `onNewIntent` captures only its parameter and preserves the original activity intent; Android does not store that parameter unless `setIntent` is called (orchestrator follow-up, 2026-11-18).
- Links are kept in **capture order**: a `LinkedHashSet`, where an equal string captured again is removed and re-added at the end.
- Add `fun takeLinks(matching: (String) -> Boolean): List<String>`. It removes and returns the matching links in capture order. 4e's `consume()` still returns what is left (the reminder part and non-auth links) at `finishLaunch`.

**2. `AuthLinkDispatcher`** (`…/app/links/AuthLinkDispatcher.kt`, a lazy `AppGraph` member `authLinks`, main thread only, no I/O).
```kotlin
class AuthLinkDispatcher(private val pending: PendingLaunchIntents, private val handle: (AuthLink) -> Unit) {
    fun attach()   // idempotent: attached = true, then deliver()
    fun deliver()  // if (!attached) return
                   // val links = pending.takeLinks { AuthLink.parse(it) != null }
                   // links.lastOrNull()?.let { handle(AuthLink.parse(it)!!) }   — latest wins
}
```
- **`attach()` is called at the iOS root-mount point:**
  - by `LaunchSessionBridge` right after its **first** successful `rootNavigator.navigate(…)` of a launch route;
  - and again, idempotently, at the start of `finishLaunch` as a backstop (the `screen` profile, a warm activity, or a launch that never navigated).
- `MainActivity` calls `graph.authLinks.deliver()` right after `capture(…)` in `onCreate` and `onNewIntent`. Before attach this does nothing. After attach, it is the warm path.
- `AppGraph` builds it as `AuthLinkDispatcher(pendingLaunchIntents) { link -> authLinkRouting(link) }`, where `authLinkRouting` is a separate lazy member. Building `authLinks` in `onCreate` must therefore touch nothing but `pendingLaunchIntents`: no `controller`, `auth` or router (AGENTS "Performance"; §7 item 10).
- `attach` lasts for the process. There is no detach: `RootNavigator` is process-wide, so a link that arrives in a new activity in the same process routes at once.
- A link never changes the launch ending. 4e's revision guard makes the launch route a no-op once a link has navigated (iOS `launchDoesNotOverwriteSessionNavigation`).
- `takeLinks` removes only links that parse as an `AuthLink`. Other `https` links stay for 4e's `LaunchIntentHandler`, which keeps 4e's `PendingLaunchIntentsTest` (`…/x`) valid.

**3. `AuthLinkRouting`** (`…/app/links/AuthLinkRouting.kt`), `class AuthLinkRouting(private val scope: CoroutineScope /* appScope + dispatchers.main */, private val resetRouter: GuestResetLinkRouter, private val verifyLinks: VerifyLinkReceiver) : (AuthLink) -> Unit`, is the production `handle` (`AppGraph` member `authLinkRouting`, lazy):
- `AuthLink.Reset(link)` → `scope.launch { resetRouter.receive(link) }`;
- `AuthLink.Verify(input)` → `scope.launch { verifyLinks.receiveVerificationLink(input) }`.
- **`VerifyLinkReceiver`** is a `fun interface` in `…/auth/links/`: `suspend fun receiveVerificationLink(input: SignedVerificationInput?)`.
  - If 5d's `VerificationCoordinator` is in the stack, `AppGraph` passes `verificationCoordinator::receiveVerificationLink`.
  - Otherwise it passes `VerifyLinkReceiver { }`, which drops the link (there is no Verify screen yet), and 5d replaces it (5d §5.10).
  - Nothing in 5c calls `POST email/verify`.

**4. `GuestResetLinkRouter`** (`…/auth/links/GuestResetLinkRouter.kt`) is an exact port.
```kotlin
class GuestResetLinkRouter(
    private val presence: suspend () -> SessionPresence?,   // controller.presence (null = not yet restored)
    updates: Flow<SessionUpdate?>,                          // controller.updates
    scope: CoroutineScope,                                  // appScope + dispatchers.main
    private val open: (ResetPasswordLink) -> Unit,
) {
    var pending: ResetPasswordLink? ; private set
    // init: scope.launch { updates.collect { if (it?.presence == SessionPresence.SignedOut) consume() } }
    suspend fun receive(link: ResetPasswordLink) { pending = link; consume() }
    private suspend fun consume() { if (presence() != SessionPresence.SignedOut) return; val l = pending ?: return; pending = null; open(l) }
}
```
- The router is main-confined. Reading and clearing `pending` after `presence()` returns is therefore atomic, and two consumes never both open.
- An unknown presence (`null`) and `SignedIn` both keep the link pending. The latest link wins.
- `open`, wired in `AppGraph`, is `{ rootNavigator.navigate(Route.SignIn); auth.openReset(it) }`. That is the iOS order (`AuthSessionServices.swift:116–119`), and it ends on `Route.Reset(email)`.

### 5.7 `AuthLink` parser [F `Kit/AppCore/AuthLink.swift`, `KitT/AppCoreTests/AuthLinkTests.swift`]

The parser goes in `…/model/auth/AuthLink.kt`, package `net.chronoflow.core.model.auth` (beside 5a's `LockoutWait`). It is JVM code with no `android.*` imports.
```kotlin
data class ResetPasswordLink(val token: String, val email: String) {
    val isValid: Boolean get() = token.isNotBlank() && email.isNotBlank()   // Swift trims .whitespacesAndNewlines
    override fun toString() = "ResetPasswordLink(token=<redacted>, email=<redacted>)"
}
sealed interface AuthLink {
    data class Reset(val link: ResetPasswordLink) : AuthLink
    data class Verify(val input: SignedVerificationInput?) : AuthLink   // null = malformed → 5d's failed state
    companion object { fun parse(url: String): AuthLink? }
}
```
`parse` mirrors the Swift exactly. `uri = java.net.URI(url)`, and a `URISyntaxException` → `null`.
1. **Origin.** Return `null` unless all of these hold:
   - `uri.scheme == "https"` and `uri.host == "app.chronoflow.net"`;
   - `uri.port == -1`;
   - `uri.rawUserInfo == null`;
   - `uri.rawFragment == null`. An empty fragment (`…#`) is not null, so it returns `null`.

   Compare case-sensitively, as Swift `==` does.
2. **Path.** `path = (uri.rawPath ?: "").split("/")`. Kotlin's `split` keeps empty segments: `/a/b` → `["", "a", "b"]`. Never use `java.lang.String.split(regex)`, which drops trailing empty segments. The segments stay percent-encoded until step 4 or 5.
3. **Query items** from `uri.rawQuery`.
   - `null` or `""` means no items. Otherwise split on `&`, keeping empty segments.
   - A segment's name is the text before its first `=`. Its value is the text after that `=`, or `null` when there is no `=`.
   - Decode both with `percentDecodeOrNull`. A `null` decoded value counts as missing (OQ-5c-8). An item whose **name** decodes to `null` still counts in `items.size` but matches no name (`email`, `expires`, `signature`).
4. **Verify**, when `path.size >= 3 && path[1] == "email" && path[2] == "verify"`. The link is valid only if all of these hold:
   - `path.size == 5`;
   - `id = decode(path[3])` matches `^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$` (never `UUID.fromString`);
   - `hash = decode(path[4])` is exactly 40 ASCII hex characters;
   - `items.size == 2`, with exactly one item named `expires` and exactly one named `signature`;
   - the `expires` value is non-null and non-empty, contains only ASCII digits, and `toLongOrNull() != null`;
   - the `signature` value is exactly 64 ASCII hex characters.

   Valid → `Verify(SignedVerificationInput(id, hash, expires.toLong(), signature))`. Anything else under `/email/verify` → `Verify(null)`.
5. **Reset**, otherwise, and only when `path.size >= 2 && path[1] == "reset-password"` (else `null`).
   - `emails` = the items named `email`.
   - `valid = path.size == 3 && path[2].isNotEmpty() && emails.size == 1 && items.size == 1`.
   - `token = if (valid) decode(path[2]) ?: "" else ""`.
   - `email = if (emails.size == 1) emails[0].value ?: "" else ""`.
   - Return `Reset(ResetPasswordLink(token, email))`. The link may be invalid; that gives the expired view.

**`percentDecodeOrNull(s)`** (`…/model/auth/PercentDecoding.kt`, `internal`):
- `%XX` (two hex digits) becomes that byte, and every other character becomes its UTF-8 bytes.
- Decode the bytes as UTF-8 with `CodingErrorAction.REPORT`.
- A malformed `%` or invalid UTF-8 → `null`, as Swift `removingPercentEncoding` returns nil.
- `+` stays `+`.
- Never use `URLDecoder` or `android.net.Uri` for any of this.

### 5.8 Manifest [R, A16, `05 §15.6`]

Add this inside 4e's `MainActivity` element (`singleTask`, exported):
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" />
    <data android:host="app.chronoflow.net" />
    <data android:pathPrefix="/reset-password/" />
    <data android:pathPrefix="/email/verify/" />
</intent-filter>
```
Add no `http`, no other host, no custom scheme and no `android:port`. The prefixes end in `/` (OQ-5c-3).

### 5.9 Pieces shared with 5d (same wave) [R, 5d §5.10]

`grep -rn` the stack top first. **If a piece exists, reuse it and keep its name. If it does not, create it exactly here**; 5d reuses it after rebasing.

| Piece | Place | Notes |
|---|---|---|
| `AuthScreen(headingGlyph: AuthHeadingGlyph? = null)`, with `AuthHeadingGlyph.{Problem, Check}` | `…/auth/shell/AuthScreen.kt`, `AuthHeadingGlyph.kt` | §5.2. A default parameter, so 5a's callers do not change. |
| `AuthPasswordPolicy` (`HINT_KEY` = the OQ 21 English sentence, plus a `@Composable fun hint(): String` resolved through `CopyIndex`) | `…/auth/form/AuthPasswordPolicy.kt` | iOS `AuthPasswordPolicy.hint` |
| `EmailAppLauncher(context)` with `open(): Boolean` | `…/auth/shell/EmailAppLauncher.kt` | §5.5 |
| `VerifyLinkReceiver` | `…/auth/links/VerifyLinkReceiver.kt` | §5.6. 5d's `VerificationCoordinator` implements it, or is adapted to it. |

### 5.10 Mock behaviour the tests rely on [F `MockDataSource.swift:122–176, 358–393`; 4c brief l.308]

- **`requestPasswordReset`**:
  - the `forgot` limiter allows 5 per 60 s;
  - a blank email (after trimming) → field `email`, `Please fill this in.`;
  - an email without `@` → field `email`, `Enter a valid email address, like name@example.com.`;
  - an unknown account → success (204) with **no** mail;
  - a known account (matched on the trimmed, lower-cased email) → `resets[accountId] = (token = id(), expiresAt = now + 3600)`, plus the mail `PasswordReset(id, to = the account's stored email, sentAt, ResetPasswordInput(token, <the account's stored email>, "", ""))`.
- **`resetPassword`**:
  - the `reset` limiter;
  - the password policy is checked first (`password_confirmation` `The passwords don't match yet.`; `password` `Use at least :min characters.` with `{min: 12}`, then the mixed-case, number and symbol keys);
  - then the token is checked against `resets` and its expiry, else field `token`, `This password reset link is invalid or has expired.`;
  - success sets the password, removes the link (so reuse fails) and revokes every session and two-factor challenge of that account.
- **Values:**
  - App seed (journey, audit seeds): the demo account is `demo@example.com` / `password` (`MockScenarioValues.password`); the new password typed in the journey is `NewPassword12!` (4c `MockScenarios.newPassword`).
  - Ported JVM model tests: the source is 5a's port of Swift `authTestSource` (`SignInModelTests` source: the 0d fixture account `demo@example.com` with password `Password123!`, delay 0); the new password is the Swift value `NewPassword123!`. Keep the Swift values verbatim.
- If 4c's mock differs from this, stop and report it. Do not patch the mock in this phase.

## 6. Tasks

Each task is one commit with a Conventional Commits message: a lower-case imperative subject and **no trailer** of any kind. Tick the plan.md checkbox named in brackets in the same commit. Run each task's tests with `make targeted ONLY="…"` (module paths without a leading colon). Run `make quick` after Tasks 4 and 8, and `make check` once before hand-off.

### Task 1: AuthLink parser (`feat(model): port the auth link parser`)
- **Create:**
  - `…/model/auth/AuthLink.kt` and `…/model/auth/PercentDecoding.kt` (§5.7);
  - if needed, a redacting `toString()` on `SignedVerificationInput` (OQ-5c-7);
  - `core/testing/src/main/kotlin/net/chronoflow/core/testing/AuthLinkUrls.kt`, with `reset(token: String, email: String)` and `verify(id: String, hash: String, expires: Long, signature: String)` building URLs through an RFC 3986 encoder: keep `A–Z a–z 0–9 - . _ ~` and encode every other UTF-8 byte as upper-case `%XX`. It takes **plain strings only**: `:core:testing` has no direct dependency on `:core:model`; its existing `:core:plan` dependency supplies the transitive model types, and this phase adds no dependency.
- **JVM tests**, `core/model/src/test/kotlin/net/chronoflow/core/model/auth/AuthLinkTests.kt`:
  - `exactSignedVerificationShapeAndMalformedInputs`, the Swift case verbatim:
    - id `0199a3c0-0000-7000-8000-000000000001`, hash `"a" × 40`, signature `"b" × 64`, `?signature=…&expires=1800000000` → `Verify(SignedVerificationInput(id, hash, 1_800_000_000, signature))`;
    - `/email/verify`, `/email/verify/bad/hash?expires=1&signature=x`, a duplicate `expires` and `expires=-1` → `Verify(null)`.
  - `exactResetURLDecodesOnceAndKeepsPlusPercentAndUnicode`:
    - `/reset-password/a%2Fb%252F?email=rick%2B100%25%40example.com` → `ResetPasswordLink("a/b%2F", "rick+100%@example.com")`;
    - `/reset-password`, `/reset-password/`, `/reset-password/token`, `/reset-password/token?email=a&email=b` and `/reset-password/a/b?email=a` → `Reset` with `!isValid`;
    - `http://…`, `https://evil.test/…`, `:443`, `user@`, `#c`, `chronoflow://reset/a` and `https://app.chronoflow.net/other` → `null`.
  - **Android-only cases:**
    - `plusInQueryStaysPlus`: `?email=a+b@example.com` → `a+b@example.com`.
    - `lenientUuidFormIsRejected`: `/email/verify/1-1-1-1-1/<hash>?…` → `Verify(null)`.
    - `upperCaseHexAndUuidAreAccepted`.
    - `expiresOverflowIsMalformed`: `expires=99999999999999999999` → `Verify(null)`.
    - `trailingSlashVerifyIsMalformed`.
    - `emptyFragmentIsRejected`: `…#` → `null`.
    - `emptyQueryMeansNoItems`: `/reset-password/t?` → an invalid reset.
    - `malformedPercentEncodingIsMissing`: `/reset-password/t?email=%E0%A4` → `ResetPasswordLink("t", "")`, which is `!isValid`.
    - `unparseableUrlIsNotALink`: `https://app.chronoflow.net/reset-password/a b` → `null`.
    - `redactedToString`: neither the token nor the email appears in `toString()`.
- **JVM test** `PercentDecodingTest`: `%2B` → `+`; `+` stays `+`; `%252F` → `%2F`; `%C3%A9` → `é`; `%`, `%4`, `%ZZ` and `%FF` → `null`.
- **JVM test** `AuthLinkUrlsTest`: `AuthLink.parse(AuthLinkUrls.reset("a/b+", "x+1@example.com"))` round-trips exactly.

### Task 2: Shared auth pieces (`feat(auth): add the heading glyph, password hint and email app launcher`)
- Create whatever of §5.9 does not exist yet: `AuthHeadingGlyph` plus the `AuthScreen` parameter, `AuthPasswordPolicy`, `EmailAppLauncher` and `VerifyLinkReceiver`. Skip the pieces 5d already landed, and say so in the commit body.
- **Robolectric tests** (`feature/auth/src/test/kotlin/net/chronoflow/feature/auth/shell/`):
  - `AuthHeadingGlyphTest` (`@Layout`, `pixel8`). Give the glyph box a test-only tag through a `Modifier.testTag("auth.heading-glyph")` that sits **outside** its `clearAndSetSemantics {}` only in the test host, or measure it from `auth.heading` (title top − 16 − 48); do not add a semantics node to production:
    - the circle is 48 ± 0.5 dp square, and its left edge equals the `auth.heading` left edge ± 0.5 dp (leading-aligned, not centred);
    - `auth.heading` top = circle bottom + 16 dp ± 0.5 (8 bottom padding + 8 heading-block gap);
    - no semantics node with a content description or text exists for the glyph;
    - pixel checks at circle-local (4 dp, 24 dp), which is inside the circle and outside the 24 dp glyph: `Problem` = `card`, `Check` = `dawn`, in light and dark.
  - `EmailAppLauncherTest` (`@Logic`): the started intent is `ACTION_MAIN` with a selector that has `CATEGORY_APP_EMAIL`, plus `FLAG_ACTIVITY_NEW_TASK`. With no handler, `open()` returns `false`. If 5d already landed an `EmailAppLauncherTest`, keep 5d's and do not add a second one.
  - `AuthPasswordPolicyTest` (`@Logic`): the hint resolves to the OQ 21 sentence in en and to the catalog value in nl.

### Task 3: Forgot model (`feat(auth): port the forgot password model`)
- **Create:**
  - `…/auth/model/ForgotPasswordModel.kt` (§5.4);
  - `startForgot()` and `forgotModel` in `…/app/auth/AuthServices.kt`, plus `SignInRouter.forgot()` → `startForgot()` + `navigate(Route.Forgot)` (§5.5);
  - `latestResetLink` on `MockSources` and `PerfMockSources`, and `mockMail` (§5.5).
- **JVM tests**, `feature/auth/src/test/kotlin/net/chronoflow/feature/auth/model/ForgotPasswordModelTests.kt`. They use 5a's test fakes, a `MockDataSource` built the way 5a's `SignInModelTests` build it, and a `GatedDataSource` helper that suspends one named method on a `CompletableDeferred` and counts entries (reuse 5a's if it has one). Swift names:
  - `mockMailDeliveryUsesTheSubmittedRecipientEvenAfterLaterEdits`.
  - `existingAndUnknownAddressesHaveIdenticalSuccessWithoutASession`: both succeed the same way; the inbox has exactly 1 mail; `fetchIdentity` throws `Unauthorized`.
  - `fieldServerThrottleOfflineAndHiddenFailuresKeepTheFormAndEmail`:
    - the six Swift failures, with the same expected banner kinds and `retryAfterSeconds == 73`;
    - the announcement is the Forgot locked title + body **keys**;
    - an unchanged edit keeps the feedback, and a changed edit clears it.
  - `serverValidatesBlankAndMalformedEmailWithoutClientValidation`: `""` and `"   "` → `Please fill this in.`; `invalid` → the email-format key; no banner.
  - `busyLockIgnoresEditsAndDuplicateTapOrReturnAndCapturesSubmittedEmail`: the gate is entered exactly once.
  - `cancellationAndBackIgnoreLateCompletionCancel` and `cancellationAndBackIgnoreLateCompletionBack`.
  - `emailAppMissingHandlerPreservesTheSentNoticeAndBackReturnsToSignIn`:
    - `open` is not called before sending, and is called once after;
    - when it returns `false`, the notice stays;
    - after back, it is not called again, and `toSignIn` has been called once.
  - `expiredResetCanPrefillANewFormWithoutReusingSentState`.
- **Robolectric test** `ForgotPasswordCopyTest` (`@Logic`), Swift name `noticeUsesTypedInterpolationInEnglishAndDutch`:
  - `person+100%@example.com` appears unchanged in the en and nl sent message;
  - the nl throttle announcement is "Te veel verzoeken. Wacht een paar minuten voordat je een nieuwe link aanvraagt.".
- **JVM test** `app/src/test/kotlin/net/chronoflow/app/auth/ForgotRoutingTest.kt`:
  - `SignInRouter.forgot()` creates a fresh model (a different instance than before) and navigates `Forgot`;
  - leaving `Forgot` for `SignIn` cancels the model and nulls it;
  - `mockMail` is `null` when the data source is `api`.

### Task 4: Forgot screen (`feat(auth): build the forgot password screen`) [ticks "Forgot"]
- Create `…/auth/forgot/ForgotPasswordScreen.kt` (§5.2, §5.5) and the `Route.Forgot` entry in `RootContent`.
- **`ForgotPasswordScreenTest`** (`@Logic`, `pixel8`, en), with tag queries only:
  - IME Send submits once, and tap + Send during busy submits once;
  - every control is disabled while busy;
  - system back and `forgot.back` call `backToSignIn()`, and the route becomes `SignIn`;
  - the notice shows the submitted email after a later edit;
  - `forgot.email` carries `ContentType.EmailAddress`;
  - `expired = true` shows the `Problem` glyph and "This link has expired" until sent, then "Reset your password" with no glyph.
- **`ForgotRecreationTest`** (`@Logic`): after `scenario.recreate()` the email text, `sentEmail` and the state are kept (the model lives in `AuthServices`), and the route is still `Forgot`.
- **`ForgotGuardsTest`** (`@Layout`, parameterised, one test per case), using 0e `Guards.assertAll(rule, profile, headingTags = setOf("auth.heading"))`, light theme:
  - `expired` with a `Server` banner (the tallest card) × `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en, nl: 10 cases;
  - `sent`, nl, on `small` and `largeFont`: 2;
  - `form` with the email-format field error, nl, on `small`: 1;
  - `busy`, en, on `pixel8`: 1.

  That is 14 cases, each under 5 s. Record the class run time.

### Task 5: Reset model and opening a reset (`feat(auth): port the reset password model`)
- **Create:**
  - `…/auth/model/ResetPasswordModel.kt` (§5.4);
  - `SignInModel.prepareReset()`;
  - `AuthServices.openReset(link)` and `resetModel`, plus the auth-exit observer extension (§5.5).
- **JVM tests**, `feature/auth/src/test/kotlin/net/chronoflow/feature/auth/model/ResetPasswordModelTests.kt`. Swift names:
  - `mockMailResetClearsSecretsRevokesSessionsAndCannotBeReused`:
    1. sign in the demo account and request a reset;
    2. take the token from the mock inbox;
    3. the first submit → `Done`, and both `SecretField`s are cleared;
    4. a reuse → `Expired`;
    5. `fetchIdentity` with the old token → `Unauthorized`;
    6. `backToSignIn` → `toSignIn` is called once.
  - `missingInvalidAndServerFieldMessagesUseDesignedStates`:
    - `ResetPasswordLink("", "")` → `Expired`;
    - `short`/`short` → a `password` field error, and a mismatch → a `password_confirmation` field error;
    - `Offline`, `Server(500)` and `Throttled(60)` → the form with that failure;
    - then token `bad` → `Expired` with `recovery.email == "demo@example.com"`;
    - a recovery edit plus submit → `recovery.sentEmail == "replacement@example.com"`;
    - the secrets are cleared after every attempt.
  - `cancelAndDuplicateSubmissionIgnoreLateCompletion`.
  - **Android-only:**
    - `resetNeverTouchesTheSession`: a recording session seam sees zero calls through a whole success-and-failure run.
    - `prepareResetDropsTheChallenge`: after a two-factor result, `prepareReset()` makes `navigation.challengeID == null`.
- **JVM test** `app/src/test/kotlin/net/chronoflow/app/auth/OpenResetTest.kt`:
  - `openReset(link)` navigates `Reset(link.email)`, cancels the previous forgot and reset models, and the new model carries the link;
  - `Route.Reset(...).toString()` does not contain the token;
  - leaving `Reset` for `SignIn` cancels and nulls `resetModel`.

### Task 6: Reset screens (`feat(auth): build the reset, expired and done screens`) [ticks "Reset … Reset done … Reset expired"]
- Create `…/auth/reset/ResetPasswordScreen.kt` (§5.2, §5.5) and the `Route.Reset` entry in `RootContent`.
- Extend 4e's `ScreenSessionFixture` audit routes, porting `AuthSessionServices.swift:263–281`:
  - `reset`, with `CF_AUTH_TEST_AUDIT_STATE` absent or not `expired` → `requestPasswordReset("demo@example.com")` on the mock, take `latestResetLink`, then `navigate(SignIn)` and `auth.openReset(link)`;
  - `reset` + `expired` → nothing here. iOS reaches the expired view through `CF_AUTH_TEST_LINK=https://app.chronoflow.net/reset-password` (Task 8).
- **`ResetPasswordScreenTest`** (`@Logic`):
  - IME Next on `reset.password` focuses `reset.confirmation`, and Go submits once;
  - both fields carry `ContentType.NewPassword`;
  - the hint is the OQ 21 sentence;
  - `auth.back` does not exist on the form or Done, and the brand centre x equals the window centre x ± 0.5 dp;
  - system back on Done equals Continue (route `SignIn`);
  - the Done announcement fires once.
- **`ResetRecreationTest`** (`@Logic`): after a locale `scenario.recreate()` both password fields are empty, and the state and the link are kept. Walk the activity's saved `Bundle` recursively: no string contains the token, and no key or value contains a typed password.
- **`ResetGuardsTest`** (`@Layout`, parameterised, one test per case, light theme):
  - `form` with both field errors ("Use at least 12 characters." and "The passwords don't match yet.") plus a `Server` banner × the 5 profiles × en, nl: 10 cases;
  - `done`, nl, on `small` and `largeFont`: 2;
  - `form`, en, on `pixel8`: 1.

  Expired is covered by `ForgotGuardsTest` (same composable).
- **`ResetBoardCaptureTest`** (`@Capture`), in the 5a `BoardCapture` setup (375 × 812 dp, density 3, zero insets, en):
  - board captures: `AuthForgot_en_{light,dark}`, `AuthReset_en_{light,dark}`, `AuthResetDone_en_{light,dark}` and `AuthResetExpired_en_{light,dark}`;
  - non-board captures under `auth/`: `auth/ForgotSent_en_light`, `auth/ExpiredSent_en_light` and `auth/ResetFieldErrors_en_light`.

### Task 7: Reset link router (`feat(auth): port the guest reset link router`)
- Create `…/auth/links/GuestResetLinkRouter.kt` (§5.6 item 4).
- **JVM tests**, `feature/auth/src/test/kotlin/net/chronoflow/feature/auth/links/GuestResetLinkRouterTests.kt`. Use a fake presence plus a `MutableStateFlow<SessionUpdate?>`, `runTest(timeout = 60.seconds)` and `StandardTestDispatcher`. Swift names:
  - `unresolvedLaunchWaitsForSignedOutRestore`;
  - `guestOpensImmediatelyAndDoesNotReplayOnLaterSessionUpdates`;
  - `activeSessionDefersLatestLinkUntilSignOutUnverified` and `activeSessionDefersLatestLinkUntilSignOutVerified`: the latest link wins, the session is untouched until sign-out, and `open` runs once.

### Task 8: App Links and dispatch (`feat(app): add verified app link filters and auth link dispatch`) [ticks "App Links"]
- **Manifest:** add the §5.8 filter to `app/src/main/AndroidManifest.xml`. Extend `:app:verifyReleaseManifest` so that it fails when:
  - the App Link filter is missing, or is not `autoVerify="true"`;
  - it has a scheme other than `https`, a host other than `app.chronoflow.net`, or a path other than the two prefixes;
  - it declares a port.
- **Create:**
  - `…/app/links/AuthLinkIntentPolicy.kt`, `AuthLinkDispatcher.kt` and `AuthLinkRouting.kt`;
  - the `PendingLaunchIntents` edits (`capture(intent, restored)`, ordered links, `takeLinks`) and the `MainActivity` hooks (§5.6);
  - `attach()` calls in `LaunchSessionBridge` and `finishLaunch`;
  - `AppGraph` members `authLinks`, `resetRouter` and `verifyLinks`, all lazy, with nothing built in `Application.onCreate`.
- **Debug seed** `CF_AUTH_TEST_LINK` (port of `SceneDelegate.swift:522–529`). In `TestSeeds.apply`, when `CF_AUTH_TEST_ID` is set and the value starts with `https:`, add it to `pendingLaunchIntents` as a captured link before `MainActivity` starts. Add the name to 4e's `TestSeed` parsing and to plan.md "Android spec: seeds and test hooks".
- **JVM tests** (`app/src/test/kotlin/net/chronoflow/app/links/`):
  - **`AuthLinkIntentPolicyTest`:**
    - restored → `null`;
    - flag `0x00100000` → `null`;
    - action `android.intent.action.MAIN` → `null`;
    - an `http:` data string → `null`;
    - a VIEW with `https:` data → that data string.
  - **`AuthUniversalLinkTests`**, the ported Swift names:
    - `testSignedColdAndWarmBrowsingActivitiesReachTheSameRouter`;
    - `testColdCaptureAndWarmActivityPreserveResetInputWithoutAScheme`: it expects `ResetPasswordLink("token%2F", "a+b@example.com")`, then a warm `…/reset-password?email=expired%40example.com` → `("", "expired@example.com")`.
  - **`AuthLinkDispatcherTest`:**
    - a capture before `attach` does nothing, and `attach` delivers it;
    - two captured links → only the latest is handled;
    - re-capturing an equal string moves it to the end;
    - `attach` twice delivers once;
    - a non-auth `https://app.chronoflow.net/x` stays in `PendingLaunchIntents` for `consume()`.
  - **`AuthLinkRoutingTest`**, with the real router, a recording navigator and recording `AuthServices`/`VerifyLinkReceiver` seams:
    - cold signed out: nothing before attach; after attach, the navigations are `[SignIn, Reset(email)]`;
    - cold signed in: pending and no navigation; after a `SignedOut` update, opened once;
    - warm signed out: opened at once;
    - warm signed in: pending;
    - two links while signed in: the latest opens;
    - presence `null` (before restore): pending until `SignedOut`;
    - a verify link (valid or malformed): `receiveVerificationLink` is called once with the parsed input (or `null`), and the reset router is never touched.
- **Robolectric tests:**
  - **`AppLinkIntentFilterTest`** (`@Logic`): `packageManager.queryIntentActivities(Intent(ACTION_VIEW, uri).addCategory(BROWSABLE).setPackage(packageName), 0)`.
    - It resolves `MainActivity` for `https://app.chronoflow.net/reset-password/t?email=a%40b.c` and for `…/email/verify/<uuid>/<40hex>?expires=1&signature=<64hex>`.
    - It resolves nothing for `http://app.chronoflow.net/reset-password/t`, `https://evil.test/reset-password/t`, `https://app.chronoflow.net/other` and `https://app.chronoflow.net/reset-password` (no slash).
    - If Robolectric does not match data paths, build `IntentFilter`s from the merged manifest and use `IntentFilter.match` instead, and record that in the commit body.
  - **`LinkRecreationTest`** (`@Logic`, Robolectric, real `AppGraph` with `GraphOverrides(sessionCipher = SoftwareSessionCipher())`, signed out, `screen` profile):
    - launch with a VIEW reset link → route `Reset`;
    - navigate to `SignIn`, then recreate through the activity controller → the route stays `SignIn` and the link is not replayed;
    - a relaunch intent with `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` replays nothing.

### Task 9: Emulator journey (`test(app): drive the password reset link journey on the emulator`) [ticks "Tests"]
`app/src/androidTest/kotlin/net/chronoflow/app/auth/PasswordResetLinkJourneyTest.kt`, extending 0e's `ScreenTest`, class-level `@Smoke`, with one method `forgotLinkResetDoneReturnsToSignIn` that runs in under 10 s:
1. `launch(mapOf("CF_AUTH_TEST_AUDIT_SCREEN" to "forgot", "CF_MOCK_DELAY" to "0"))` (the `screen` profile, the mock source and a fresh `CF_AUTH_TEST_ID`, as 0e's `launch` does).
2. Type `demo@example.com` into `forgot.email`, click `forgot.submit`, and wait for `forgot.notice`.
3. Get the link from `appGraph.mockSources!!.latestResetLink(appGraph.dataSources.public(), "demo@example.com")` in-process and build `url = AuthLinkUrls.reset(link.token, link.email)`. Run `InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d $url ${context.packageName}")`, with no quotes, and read its stream to the end.
4. `waitUntilExactlyOneExists(hasTestTag("reset.password"), 5_000)`. Type `NewPassword12!` into `reset.password` and `reset.confirmation`, click `reset.submit`, and wait for `reset.continue`.
5. Click `reset.continue` and wait for `sign-in.email`. `RouteProbe.state.route == Route.SignIn`; `controller.presence == SignedOut`; no `session.bin` exists under the mock namespace.

Rules for the journey:
- Use no `Thread.sleep`, only `waitUntil`/`waitUntilExactlyOneExists`.
- The cold path, the signed-in path and the recreation path are covered by Task 8's JVM and Robolectric tests. That keeps it to one journey per feature.
- If your sandbox cannot run the emulator, report the exact commands (`make emu`, then `make targeted ONLY="app:PasswordResetLinkJourneyTest"`) and their output. The orchestrator runs them.

### Task 10: Server doc, auth doc and plan (`docs(app): document app links and the reset flow`) [ticks "docs/app-links.md"]
- **`docs/app-links.md`:**
  - **What the app declares:** the §5.8 filter, and package `net.chronoflow.app`. The debug package `net.chronoflow.app.debug` is listed for local/staging with its matching certificate (user, 2026-11-23; see `docs/app-links.md`).
  - **What the server must publish (S1).** `GET https://app.chronoflow.net/.well-known/assetlinks.json` must answer HTTP 200 with `Content-Type: application/json`, no redirect, no cookies and no auth, and must work outside maintenance mode. Serve it like the AASA route (`05 §15.6`):
    ```json
    [{
      "relation": ["delegate_permission/common.handle_all_urls",
                   "delegate_permission/common.get_login_creds"],
      "target": {"namespace": "android_app",
                 "package_name": "net.chronoflow.app",
                 "sha256_cert_fingerprints": ["<Play app-signing certificate SHA-256, AA:BB:… form>"]}
    }]
    ```
    List the release fingerprint only, never the committed debug key (OQ-A38). `get_login_creds` is for passkeys (A15) and is harmless for links. Redo the file if the applicationId changes (OQ-A1) or the signing key changes (Risk 3).
  - **Until S1 ships:** links open the web reset and verify pages, which work.
  - **How to test** (debug build):
    - `adb shell pm get-app-links <pkg>`;
    - `adb shell pm set-app-links --package <pkg> 0 all`;
    - `adb shell pm verify-app-links --re-verify <pkg>`;
    - `adb shell pm set-app-links-user-selection --user cur --package <pkg> true app.chronoflow.net`;
    - `adb shell "am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'https://app.chronoflow.net/reset-password/<token>?email=<encoded>' <pkg>"`.

    Record the output you actually got on `pixel`, or the exact error.
- **`docs/auth.md`** (5a's file): add the Forgot and Reset state tables, the §5.1 differences, the §5.6 hand-off point, the port map (Swift test → Kotlin test) and the reviewer's board comparison. Record every difference as fixed, or with its board name.
- **`plan.md`:**
  - append OQ-5c-1 … OQ-5c-9 to "Open questions for the user" as the next free `OQ-A` numbers, with their defaults;
  - add `CF_AUTH_TEST_LINK` to "Android spec: seeds and test hooks" (unless Task 8 already did);
  - the routes line already reads `Reset(email)` (4e).

## 7. Acceptance criteria

1. `make quick` passes (under 60 s warm).
2. This command is green, every JVM test takes under 1 s and every Robolectric test under 5 s (see `build/test-results/*/timings.json`), and every ported Swift method exists under its Swift name:
   ```
   make targeted ONLY="core:model:AuthLinkTests core:model:PercentDecodingTest core:model:AuthLinkUrlsTest feature:auth:AuthHeadingGlyphTest feature:auth:EmailAppLauncherTest feature:auth:AuthPasswordPolicyTest feature:auth:ForgotPasswordModelTests feature:auth:ForgotPasswordCopyTest feature:auth:ForgotPasswordScreenTest app:ForgotRecreationTest feature:auth:ForgotGuardsTest feature:auth:ResetPasswordModelTests feature:auth:ResetPasswordScreenTest app:ResetRecreationTest feature:auth:ResetGuardsTest feature:auth:GuestResetLinkRouterTests app:ForgotRoutingTest app:OpenResetTest app:AuthLinkIntentPolicyTest app:AuthUniversalLinkTests app:AuthLinkDispatcherTest app:AuthLinkRoutingTest app:AppLinkIntentFilterTest app:LinkRecreationTest"
   ```
3. 4e's `PendingLaunchIntentTest`, `PendingLaunchIntentsTest`, `LaunchConnectionTests`, `ReseedEqualsColdSeedTest` and `ReleaseReadinessTest` still pass. So do 5a's `AuthServicesTest` and `SignInGuardsTest`.
4. `make check` passes with `pixel` booted. That includes `PasswordResetLinkJourneyTest` in under 10 s, `:app:verifyReleaseManifest` with the new App Link rules, `testReleaseUnitTest` (no mock or seed class in release) and Lint with no `MissingTranslation`.
5. `make captures ONLY=AuthForgot,AuthReset,AuthResetDone,AuthResetExpired` wrote the PNGs. The reviewer compared them with the boards, and every difference is either in §5.1, fixed, or recorded in `docs/auth.md` with its board name.
6. `docs/app-links.md` exists with the JSON, the requirements and the test commands, and says what was verified on `pixel`.
7. Debug runs are StrictMode-clean through the journey: no main-thread disk or network.
8. There is no new string, and `tools/l10n/android-only.json` is unchanged.
9. The plan.md 5c boxes are ticked in the commits named in §6. Commits are Conventional, with no trailers.
10. **Performance:** this phase has no budget of its own. Startup must not regress: the only addition on the launch path is a string check in `onCreate`. Run `make perf` only if `StartupBenchmark` already exists, and record the result in `docs/perf-startup.md`.

## 8. Review checklist (Opus reviewer)

**Parser**
- [ ] `AuthLink` is in `:core:model` and uses `java.net.URI` raw components and its own decoder. There is no `android.net.Uri`, `getQueryParameter`, `URLDecoder` or `UUID.fromString` anywhere in link code.
- [ ] Verify is checked before reset. The checks are `path.size == 5`, `items.size == 2`, exactly one `expires` and one `signature`, length-exact ASCII-only hex, and an `expires` of digits that fits a `Long`.
- [ ] Reset decodes the token **once**, and only when the link is valid. The email is taken whenever there is exactly one `email` item, even for an invalid link.
- [ ] Kotlin `split("/")` is used, never the regex split.
- [ ] Both Swift parser tests are ported case by case, not summarised.

**Capture and routing**
- [ ] `capture` ignores restored activities, `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` and non-VIEW intents. Cold capture clears the launch intent's data; warm capture leaves the original activity intent unchanged. To check by hand: open a link, rotate, change the font size (it recreates), then relaunch from Recents. The reset opens once.
- [ ] Auth links are handed over at the first launch navigation (with `finishLaunch` as the backstop), never in `onCreate`. A link never changes the launch ending.
- [ ] The latest link wins, both in `takeLinks` order and in the router. A non-auth link is left for `LaunchIntentHandler`.
- [ ] A reset link while signed in, or with unknown presence, stays pending and opens once on the next sign-out.
- [ ] Verify links go only to `VerifyLinkReceiver`. 5c never calls `POST email/verify` and never duplicates `VerificationLinkInbox`.
- [ ] The manifest has exactly one `autoVerify` filter: `https` only, host `app.chronoflow.net`, prefixes `/reset-password/` and `/email/verify/`, and no port. `verifyReleaseManifest` enforces it.

**Forgot and Reset behaviour**
- [ ] The 204 never reaches `SessionController`. After a reset there is no session blob, no account directory and no autofill commit.
- [ ] Both `SecretField`s are cleared after **every** reset attempt: success, field error, banner and cancel.
- [ ] The invalid-token key selects Expired. Other 422 keys on fields that are not shown give the server banner, never raw server English.
- [ ] Forgot shows the **submitted** email in the notice, and a later edit does not change it. A submit after sent does nothing.
- [ ] Forgot throttled shows "Too many requests." (the context copy), not "Too many attempts.".
- [ ] Expired sends from the same screen, and the title, subtitle and glyph switch to the Forgot sent state.
- [ ] "Open email app" with no email app does nothing visible and does not crash. With the mock seam it never calls the system launcher.
- [ ] Models live in `AuthServices` and are cancelled when their route is left. A recreation keeps them. There is no activity-scoped ViewModel holding a stale model.
- [ ] `CancellationException` is rethrown, never swallowed by `runCatching` or `catch (e: Exception)`.

**Secrets**
- [ ] No token or password is in a `Bundle`, `SavedStateHandle`, `rememberSaveable`, a route key, the database, the outbox or a log. The only route is `Reset(email)`.
- [ ] `ResetPasswordLink` and `SignedVerificationInput` redact their `toString()`, and `ResetRecreationTest` walks the saved bundle.
- [ ] Password fields use `remember { TextFieldState() }`, never `rememberTextFieldState()`.

**Fidelity**
- [ ] Every §5.1 override is applied. No board sample value appears: `rick@example.com`, "Save and sign in", "Other devices stay signed in" or "You'll be signed in on this phone".
- [ ] The glyph circle is 48 dp with a 24 dp glyph and 8 dp below it. The notice is Phase 2's `CfNotice`. Colors come only from generated tokens.
- [ ] Reset form and Done have no drawn back, the brand stays centred, and system back behaves per §5.5.
- [ ] The Forgot notice uses "Check your inbox." **with** the period, and its resource name comes from `names.lock.json`.

**Tests and process**
- [ ] Logic is tested on the JVM, layout in Robolectric, and only the journey runs on the emulator. The journey launches seeded and uses exact tags and bounded waits.
- [ ] Each guard case is its own test under 5 s. The emulator test runs under 10 s.
- [ ] Shared pieces exist once (5d overlap). Commits are atomic and tick the plan in the right commit.

**Typical mistakes:**
- parsing with `Uri.getQueryParameter`, which turns `+` into a space, so `a+b@example.com` resets the wrong address;
- `UUID.fromString` accepting short forms;
- capturing the link again after a recreation;
- consuming the link in `onCreate` before the session is known;
- putting the token into `Route.Reset` or a `rememberSaveable`;
- navigating to Today or creating a session after the 204;
- using the board's "Too many attempts." on Forgot;
- quoting the URL inside `executeShellCommand`;
- an activity-scoped `viewModel()` for Forgot or Reset that keeps a cancelled model;
- one guard test looping over every profile instead of one test per case.

## 9. Open questions (proceed on the default; Task 10 adds each to plan.md as the next `OQ-A` number)

| # | Question | Default |
|---|---|---|
| OQ-5c-1 | Who owns the reset token in memory? OQ-4e-5 names an `AuthLinkHolder`. | `AuthServices.resetModel`, the `ResetPasswordModel` that holds the link, is that holder. There is no separate class, and the route is `Reset(email)`. |
| OQ-5c-2 | When is a cold link handed over? iOS does it at root mount, under the overlay (`SceneDelegate.swift:532`). `05 §11.3` and 4e's `finishLaunch` say after the overlay. | iOS: right after `LaunchSessionBridge`'s first launch navigation, with `finishLaunch` as an idempotent backstop. The ending stays committed. With the Welcome ending, the moon uses 3c's fallback target. Alternative: hand over only at `finishLaunch`, which shows Welcome for a moment first. |
| OQ-5c-3 | The prefixes `/reset-password/` and `/email/verify/` (with a trailing slash) leave `/reset-password?email=…` (no token) to the web, while the parser would show the expired view for it. | Keep the trailing-slash prefixes. Server links always have the full shape. |
| OQ-5c-4 | Fingerprints in `assetlinks.json`. | The Play app-signing certificate only. Add the upload key only if the user wants locally signed release builds to verify. Never the debug key (OQ-A38). |
| OQ-5c-5 | Port the iOS mock "Open email app", which opens the latest mock reset link in the app? | Yes, in debug builds with the mock data source only. Release has no mock code. |
| OQ-5c-6 | How does "Open email app" open mail on Android, and what happens with no email app? | `makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_EMAIL)`. With no app nothing happens and the notice stays, as on iOS, where a missing handler keeps the designed state. |
| OQ-5c-7 | Redact `ResetPasswordLink` and `SignedVerificationInput` in `toString()`? | Yes, both values, so an accidental log line or test failure message never leaks a link secret. |
| OQ-5c-8 | Malformed percent-encoding in a query value. Foundation's exact behaviour is untested on iOS. | The value counts as missing: a reset link gets an empty email (the expired view), and a verify link becomes `Verify(null)`. |
| OQ-5c-9 | iOS uses a `ViewModel`-free view with `@State request`. 5a used an activity-scoped `SignInViewModel`. | No ViewModel for Forgot or Reset: models live in `AuthServices`, and requests run in `rememberCoroutineScope` (§5.5). An entry-scoped ViewModel decorator stays a later refactor (OQ-5a-5). |
