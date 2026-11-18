# Phase 5a: Auth shell and sign-in

- **Branch:** `feat/phase-5a-sign-in`, from the stack top once **4e** (session controller, `RootNavigator`, `RootContent`, seeds, `StoredDataReadiness.routeAcceptedSession`) and **3c** (Welcome) are in it. It also builds on 0c (strings), 0d (contracts, `DataFailure`, `FieldMessage`), 0e (profiles, guards, captures, `ScreenTest`), 2 (primitives), 4a (`DeviceIdentityStore`, `androidDeviceName`), 4b (`DataSource`), 4c (`MockDataSource`). If one is missing, stop and report it.
- **Plan entry:** `plan.md` "Phase 5a: Auth shell and sign-in" (l.1702–1748). Also A4 (DI), A8 (navigation), A10 (window), A15 (passkeys, autofill), A19 (tests), A23 (secrets); "Android spec: window policy" (auth row), "state survival" (auth forms row), "navigation and routes", "forms and IME", "seeds and test hooks"; inherited D27 and iOS OQ 23 (A25 table).
- **Tests:** every test follows `AGENTS.md` "Tests" (cheapest tier, budgets, exact tag queries, no `Thread.sleep`, hang caps). Tier annotations are 0e's (`net.chronoflow.core.testing.tiers`): Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`. Plain JVM tests carry no annotation.
- **Tags:** **[F]** fact read in source (path given). **[R]** rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>` (never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `KitT/` = `I/Packages/ChronoFlowKit/Tests`. `DC/` = `_design/canvas/`.
- **Paths:** `…/auth/` = `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/model/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`.
- **Names from earlier phases.** This brief uses the names of the final 0c, 0e, 2, 3c, 4a–4e briefs. Before Task 1, `grep` each name in §2 item 7 in the merged code. Where the merged name differs, use the merged one and list the mapping in the PR. Never rename earlier code.

- **Stack check (2026-11-18, against 93b4cb9).** Every name in §2 item 7 resolves in the merged code. Deltas: `Route.Walkthrough` is `data class Walkthrough(val replay: Boolean = false)`, not an object; `RouteProbe` is debug-only (`app/src/debug/kotlin/net/chronoflow/debug/RouteProbe.kt`, absent from release per `ReleaseReadinessTest`); `PasswordSignInInput` declares `deviceID`/`deviceName`/`draftToken`/`guestSecret` (`core/model/…/model/api/AuthContracts.kt:14`); `SessionController.accept(result: SessionResult)` is in `core/sync/…/sync/session/SessionController.kt:105`; `StoredDataReadiness.routeAcceptedSession(navigate: (Gate) -> Unit, verification: suspend (Long) -> Unit)` at `core/sync/…/sync/session/StoredDataReadiness.kt:169`; `AppGraph.dataSources.public()` is `SessionDataSources.public()` (`app/…/app/SessionDataSources.kt:51`); `AuthTokens` lives in `core/designsystem/…/designsystem/generated/CfDimens.generated.kt`; `CfTypeScale.authBrand/authHeading/authBody/authLabel` exist; `CopyTable.Auth` exists.

- **Implementation stack check (2026-11-18).** Repeated §2 item 7 and §6 against the current checkout. All prerequisite symbols exist; §6's auth files are new. Further mechanical mappings: `RootContent` is a generic, transition-free route host and the route-to-screen `when` lives in `CfRoot.kt`, so Task 8 adds the Sign in entry there. `AppGraphKey` is owned by `:app`; the `viewModelFactory { initializer { SignInViewModel(this[AppGraphKey]!!.auth.signIn) } }` therefore lives in `app/auth/SignInViewModelFactory.kt`, not in the feature (which cannot import `:app`). Task 7 keeps the constructor-injected ViewModel in the feature. `:app` already depends on `:feature:auth`. `LaunchPlatform.welcomeDestinations` is currently a variant property with no graph; Task 8 makes it a navigator-taking provider, retaining the debug guest seam. These resolutions preserve landed routes and Welcome behaviour except the Sign in seam explicitly replaced by this phase.

---

## 1. Goal

A signed-out person opens **Sign in** from Welcome (or lands on it after sign-out or a forced 401), types email and password, and is signed in. The session goes through 4e's gate (verify → terms → walkthrough → app). Everything the later auth screens (5b–5d) reuse is built here once: the **auth shell** (`AuthScreen`) on the plain sky, the **form model** (busy lock, failure → banner or field, one announcement per failed submit), the field, banner and link presentation, and the **keyboard reveal**.

Done means:
- The shell matches `AuthSignIn` geometry at any window size: reference frame 375 × 812 dp; insets top `safeTop + 16`, sides `safe + 16`, bottom `safeBottom + 24`; card padding 20, radius 26, gap 14; a 560 dp centred column from a 600 dp window.
- Sign in behaves exactly like iOS `SignInModel` + `AuthFormModel`: it starts empty; there is no client validation; the password is cleared after **every** attempt; email and feedback are kept; a double-submit lock; five banners (credentials, locked, server, offline, email-taken for 5d); field messages under their field.
- Success calls `SessionController.accept` once, then routes through `StoredDataReadiness.routeAcceptedSession`. A two-factor result keeps the challenge in memory and navigates to `Route.TwoFactor` (5b builds the screen).
- The passkey button and the "or with email" divider are **not composed** while `PasskeyCapability.Shipped.enabled` is false.
- No password or challenge id ever reaches a `Bundle`, `SavedStateHandle`, `rememberSaveable`, a route key, the DB, the outbox or a log. The password is gone after activity recreation.
- The ported iOS cases pass on the JVM; lean Robolectric guards and captures cover the board states; on the emulator, one journey signs in the demo account and one seeded check proves that the focused field and submit stay above a real software keyboard.

## 2. Inputs to read (in this order)

1. **This repo:** `AGENTS.md` (all of it); `plan.md` sections in the header; `docs/phases/phase-2-design-system.md` Tasks 2 (`CfTheme.type`), 7 (`cfClickable`), 8 (`skyBackground`, `softShadow`), 10 (lucide manifest), 11 (`CfButton`), 12 (`CfTextField`, `CfSecureTextField`), 13 (`CfCard`), 14 (`CfBanner`), 17 (Konsist, `@BoardMetrics`); `docs/phases/phase-4e-session.md` §5.2 (`accept`), §5.4 (`routeAcceptedSession`, `SessionGateRouter`), §5.7 (`Route`, `RootNavigator`), §5.8 (`RootContent`, `ScreenSessionFixture`, `RouteProbe`), T11 (seeds); `docs/phases/phase-0e-test-harness.md` T6–T9 and T12 (`LocalSafeInsets`, `ProfileRule`, `ProfileHost`, `Guards`, `BoardCapture`, `ScreenTest`); `docs/phases/phase-3c-welcome.md` §6 (`WelcomeDestinations`).
2. **Research:** `docs/research/05-launch-auth.md` §5 (routing), §8.1–§8.4 (shell, type, form behaviour, failure mapping), §9.1 (Sign in), §9.9 (copy), §10 (API), §15.3–§15.9 (bars, IME, back, Credential Manager, autofill, font cap), §15.13 (secrets, process death).
3. **Boards** (read-only; open the HTML in a browser to render):
   - `DC/AuthSignIn.dc.html`: props `theme` (`light`/`dark`) and `error` (`none` | `wrong` | `locked` | `server`). Frame 375 × 812, `padding: 16px 16px 24px`, card `padding: 20px`, banner `padding: 12px 14px`, links `min-height: 44px; padding: 0 8px`, footer `min-height: 48px`. Inline glyphs: back `m15 18-6-6 6-6` (22), moon `M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z` (20, `--dawn`), circle-alert (18, `--destructive`, `margin-top: 1px`), key-round `M2.586 17.414A2 …-4-4z` + `<circle cx="16.5" cy="7.5" r=".5" fill="currentColor">` (16).
   - `DC/AuthSignInError.dc.html`: a wrapper, `<dc-import name="AuthSignIn" error="wrong">`.
   - `DC/AuthStates.dc.html`: the banner catalogue for every auth screen and the placement rule (banner first inside the card; it clears when the user edits a field). Its section headings are documentation, not app copy.
   - `DC/ds/chronoflow/components/bundle.js`: `Button` (`lg` = `h-10 px-6`, `outline` = `border-input bg-background`) and `Field` (label, input, error row).
4. **iOS references** (`I/` at `f391a1fa`). Behaviour is ported from these; type and test names are kept:
   - `plan.md:1176` "Phase 10a: Native Auth shell, controls and Sign in (`feat/phase-10-sign-in`)"; `plan.md:1192` "Phase 10a fixes and acceptance (2026-11-07)" (OQ 32 keyboard lesson); `plan.md:471` "Auth geometry, tokens and typography"; `plan.md:494` "Form behavior, routing and accessibility"; `plan.md:505` "Auth copy (String Catalog; English sentence keys)"; `plan.md:2234` "Auth submit feedback fix (2026-11-08, `fix/auth-error-banners`)"; `plan.md:25` "Decisions" (D27); `plan.md:2376` "Open questions for the user" (OQ 23; also OQ 30, 32, 33).
   - `Kit/Auth/SignInModel.swift`, `AuthFormModel.swift`, `AuthCopy.swift`, `AuthNavigation.swift`, `AuthLayout.swift`, `AuthScreen.swift`, `AuthBrand.swift`, `SignInView.swift`, `AuthFailureFeedback.swift`, `AuthKeyboardReveal.swift`, `PasskeySignIn.swift:4–6` (`PasskeyFailure` only).
   - `Kit/DesignSystem/AuthControls.swift` (`AuthButton`, `AuthField`, `AuthBanner`), `Kit/DesignSystem/AuthTokens.generated.swift`.
   - `Kit/AppCore/LockoutWait.swift`, `Kit/AppCore/GuestSessionContinuation.swift:1` (`AuthenticationMethod`), `Kit/AppCore/StoredDataReadiness.swift:68–84` (`routeAcceptedSession`).
   - `I/App/AuthRootView.swift` (`onDisappear { model.clearSecrets() }`), `I/App/Session/AuthSessionServices.swift:93–115` (wiring), `I/App/Session/NativeSessionController.swift:127–149`.
   - Tests: `KitT/AuthTests/{SignInModelTests,AuthFormModelTests,AuthFailureMappingTests,AuthLayoutTests}.swift`, `KitT/AppCoreTests/LockoutWaitTests.swift`. Reference only: `I/AppUITests/{SignInTests,SignInKeyboardTests}.swift`.
   - `Docs/auth-gate.md` (what iOS proved and how).
5. **iOS screenshot:** `docs/research/screens/ios/13-sign-in.png` (light, empty form; iOS shows the passkey button, Android hides it).
6. **API** (web `e36368ec`, through 4b's `DataSource.signIn`; never call the network in tests): `POST {apiBase}/v1/auth/login` (`routes/api.php:47–48`, throttles `api-login`, `api-login-ip`, `api-login-email`), public, no bearer, 15 s timeout (A6).
   - Body `PasswordSignInInput {email, password, device_id, device_name, draft_token?, guest_secret?}`; without draft and guest values it encodes to exactly `{email, password, device_id, device_name}` (0d). Kotlin property names keep the Swift ones (0d "Ported code keeps the Swift type and property names (`deviceID`, …)"): `email`, `password`, `deviceID`, `deviceName`, `draftToken`, `guestSecret`. 4e's seed code writes `deviceId`; use whatever the merged class declares.
   - The server lower-cases and trims the email (`LoginRequest::prepareForValidation`), and requires `device_id` to be a UUID (`'device_id' => 'required|uuid'`).
   - 200 → `SignInResult.Session(SessionResult {kind:"session", token, account, tripId})` or `SignInResult.TwoFactor(challengeId)` (`{kind:"two-factor", challengeId}`).
   - 422 `Validation`: wrong credentials → `errors.email = "These sign-in details don't match our records."` (`app/Actions/Api/VerifyLoginCredentials.php:28`); blank → `"Please fill this in."`; bad email → `"Enter a valid email address, like name@example.com."`. 429 → `Throttled(retryAfterSeconds, 429)`. Timeout or I/O → `Offline`.
7. **Names you build on** (grep each): `DataSource.signIn(PasswordSignInInput): SignInResult`, `DataFailure` (`Offline`, `Throttled(retryAfterSeconds, status)`, `Validation(fields, status)`, `Server(status)`, …), `FieldMessage(key, arguments: Map<String, JsonPrimitive>)`, `SessionResult`, `SessionAccount`, `SessionFailure` (`CredentialsUnavailable`, `DatabaseUnavailable`, `CredentialWrite`, `InvalidResult`, `Superseded`), `SessionController.accept`, `StoredDataReadiness.routeAcceptedSession(navigate, verification)`, `Gate.toRoute()`, `Route.{Welcome, SignIn, TwoFactor, Forgot, SignUp, GuestWizard, Verify, Terms, Walkthrough, Today}`, `RootNavigator.navigate`, `RouteProbe`, `AppGraph` (`dispatchers`, `appScope`, `dataSources.public()`, `controller`, `readiness`, `rootNavigator`, `gateRouter`), `AppGraphKey`, `DeviceIdentityStore.identity()`, `androidDeviceName(context, io)`, `MockDataSource`, `MockServerState`, `MemoryMockStateStore`, `WelcomeDestinations`, `CfButton` (`loadingDescription`), `CfTextField`, `CfSecureTextField`, `CfCard(border, spacing)`, `CfBanner(announce, testTag)`, `Modifier.skyBackground(dark)`, `Modifier.softShadow(shape, dark)`, `Modifier.cfClickable`, `LucideGlyphs`, `AuthTokens`, `CfTypeScale.auth*`, `LocalSafeInsets`, `@BoardMetrics`, `CopyIndex.find(CopyTable.Auth, key)`.

## 3. Out of scope (owner in brackets)

| Item | Owner |
|---|---|
| Two-factor screen, code and recovery entry, `TwoFactorModel` | 5b. 5a keeps the challenge in `AuthNavigation` and navigates to `Route.TwoFactor` (4e's placeholder until 5b). |
| Passkey sign-in (`PasskeySignIn` class, Credential Manager, `Base64URL`, the `cf.passkeyCapability` debug seed) | 5b. 5a adds `PasskeyCapability`, `PasskeyFailure` and a passkey seam that throws `Unavailable`. |
| Forgot, Reset, App Links, `forgotEmail`, `openReset` | 5c. The Sign in link only navigates to `Route.Forgot`. |
| Sign up, Verify | 5d. The link only navigates to `Route.SignUp`; an unverified session is routed to `Verify` by 4e. |
| Guest claim on login (`draft_token`, `guest_secret`), `GuestSessionAcceptance` | 11b. Send both as null. |
| `LockoutWait` **strings and UI** (wizard throttle, settings password prompt) | 9f, 10c. 5a ports the pure type and its test (§5.6). |
| `AuthNotice` (info tone), the heading glyph disc (`headingBadge` 48) | 5c (Forgot sent, Reset done/expired). The plan's 5a line "`auth.banner` for error, info and offline" is met as follows: error and offline are `AuthBanner` (§5.4); "info" is Phase 2's `CfNotice(Info)`, which no Sign in state uses, so 5c places it (5c §5 "Notice … new if 5a has none"). |
| Interactive keyboard dismissal (iOS `.scrollDismissesKeyboard(.interactively)`) | Not built: system back closes the IME (05 OQ A11). |
| An offline pre-check or a submit disabled while offline or throttled | Never (iOS: "No offline-disabled submit"). |
| A screen for any unbuilt route (TwoFactor, Forgot, SignUp, GuestWizard, Verify, Terms, Walkthrough, Today) | Their phases. 4e's `RoutePlaceholder` shows them. **Do not build a placeholder screen.** |
| `AuthEmailChanged`, `PolicyReader` boards | Out of scope (D27, A24). |
| Haptics | None: iOS auth has none. |
| A ViewModel entry decorator in `RootContent` | Not in 5a (OQ-5a-5). |

## 4. Settled values that override the raw boards [F]

The canvas is older than the iOS decisions in places (`05 §2.2`). Build the right-hand column.

| Board says | Build | Source |
|---|---|---|
| `pattern-dots` over the sky | Plain sky only (`Modifier.skyBackground(dark)`), no dots | user 2026-11-06, `I/plan.md:471–493` ("No PatternDots") |
| Fields prefilled `rick@example.com` / `password12` | Both fields start **empty** | `I/plan.md:498` |
| Back `href="Main.dc.html"` | Back → Welcome, with no intro and no splash | `05 §5`, `05 §15.5` |
| Footer `href="GuestFlight.dc.html"` | Footer → `Route.GuestWizard` | iOS OQ 12, `I/plan.md:499` |
| Passkey button and divider always drawn | Composed only while `PasskeyCapability.Shipped.enabled` (false until server S1–S3) | A15, OQ-A21 |
| Input `h-9` (36), button `h-10` (40), links `min-height: 44` | Field box ≥ 48 dp (Phase 2 `CfTextField`); buttons keep the 40 dp visual height (`CfButtonSize.Lg`) with a 48 dp target; link rows 48 dp | AGENTS (48 dp targets), OQ-5a-4 |
| No offline state on the board | Offline banner, title only: "Needs a connection" / "Verbinding nodig", on the same banner surface | `I/plan.md:2240–2242` (user authorization superseding OQ 23's default) |
| Link underline offset 0.2 em | The default underline (Compose has no offset): an accepted adaptation | [R] |

## 5. The spec

### 5.1 Shell geometry: `AuthScreen` [F `Kit/Auth/AuthScreen.swift`, `AuthLayout.swift`, `AuthTokens.generated.swift`, `I/plan.md:471–479`; R where marked]

`AuthScreen(scrollTag: String, title: String, description: String, onBack: () -> Unit, showsBack: Boolean = true, compact: Boolean = false, enabled: Boolean = true, announcementRevision: Int = 0, takeAnnouncement: () -> String? = { null }, card: @Composable ColumnScope.() -> Unit, footer: (@Composable () -> Unit)? = null)`. The two announcement parameters feed the `AuthAnnouncer` in the brand row's end slot (§5.4).

Structure, back to front:
1. A full-window `Box` with `Modifier.fillMaxSize().skyBackground(CfTheme.colors.isDark)`. The sky is full-bleed under every inset.
2. Inside it, `BoxWithConstraints(Modifier.fillMaxSize())`. Its `maxWidth` is the window width (the activity is edge to edge).
3. The scroll column, with this exact modifier order: `Column(Modifier.fillMaxSize().imePadding().onPlaced { viewport = it }.verticalScroll(rememberScrollState()).testTag(scrollTag).windowInsetsPadding(LocalSafeInsets.current).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp))`. Values: `AuthTokens.gutter` (16) and `AuthTokens.bottom` (24).
   - `imePadding()` before `verticalScroll` makes the scroll viewport end at the IME top **and** consumes the IME inset for the rest of the chain, so `windowInsetsPadding(LocalSafeInsets.current)` (which includes the IME in production, 0e T6) does not add it again. Do not also call `exclude(WindowInsets.ime)`.
   - `onPlaced` before `verticalScroll` reports the viewport's coordinates into a plain holder for the keyboard reveal (§5.5). It is not snapshot state.
   - `verticalScroll` measures its content with an infinite max height but keeps the incoming min height. After `fillMaxSize()` that min height is the viewport, so `Spacer(Modifier.weight(1f))` works (Compose's `Column` uses `mainAxisMin` as the target when the max is infinite). Prove it with the `pixel8` footer assertion in Task 6. If it does not hold, add `Modifier.heightIn(min = this@BoxWithConstraints.maxHeight)` after `verticalScroll` instead; never write a measured size into state (AGENTS Traps).
4. The content column: `Modifier.align(CenterHorizontally)` plus, **only when `maxWidth >= 600.dp`**, `Modifier.widthIn(max = 560.dp)`; otherwise `fillMaxWidth()`. Both numbers live in `AuthMetrics` with a KDoc that points at plan "Android spec: window policy" [R].

Content order, top to bottom:
1. **Brand row** (`AuthBrand`): min height 48 dp [R; iOS 44]. Start: the back control, a 48 × 48 dp target with the lucide `chevron-left` glyph at 22 dp (`AuthTokens.backGlyph`), stroke 2, `skyTitle`; tag `auth.back`, `Role.Button`, content description Auth "Back"; `cfClickable`, enabled only when `enabled`. Centre: a merged node tagged `auth.brand` with the moon (`LucideGlyphs.Moon`, 20 dp = `AuthTokens.moon`, `dawn`, no semantics) + gap 6 (`AuthTokens.brandGap`) + Auth "ChronoFlow" in `CfTypeScale.authBrand` (Fraunces 400 opsz 20, 18/24) `skyTitle`. End: a 48 dp box so the brand stays centred; it is empty to the eye and hosts the failure announcer (§5.4). With `showsBack = false` (5c Reset), the start slot is an empty 48 dp box too.
2. **Heading block**: top padding 32 (`AuthTokens.headingMargin`; `compact = true` → 24 = `AuthTokens.spacer`, used by 5d), vertical gap 8 (`AuthTokens.fieldGap`). Title in `CfTypeScale.authHeading` (Fraunces 400 opsz 32, 36/40, letter spacing −0.01 em) `skyTitle`, `semantics { heading() }`, tag `auth.heading`. Description in `authBody` (Figtree 400, 16/24) `skyTitle`, tag `auth.subtitle`.
3. **Flexible spacer:** `Spacer(Modifier.height(24.dp))` (`AuthTokens.spacer`) then `Spacer(Modifier.weight(1f))`.
4. **Card:** `CfCard(border = false, spacing = AuthTokens.cardGap.dp)`. Phase 2 gives it padding 20, radius 26, fill `card`, content `cardForeground` and `softShadow`. The board card has no border; `AuthStates`' bordered card is documentation. Tag `auth.card`.
5. **Footer** (optional): top padding 12 (`AuthTokens.footerGap`), then the footer content.

Rules:
- At the zero-inset reference frame 375 × 812 this gives the content rect x 16, y 16, width 343, height 772 (port of `AuthLayout.content`: height = `h − top − bottom − 16 − 24`).
- At 280 dp (the minimum width) nothing clips: text wraps.
- Font scale: the root cap (1.3, `ConfigurationPolicy`) is used as is; no per-screen cap, no smaller text.
- The screen root sets `semantics { paneTitle = title }` (replaces iOS `.screenChanged`).
- Sizes come only from `AuthTokens`, `CfTypeScale` and an `@BoardMetrics("AuthSignIn", "AuthStates") internal object AuthMetrics` holding the board-only or Android numbers: `touch = 48`, `wideBreakpoint = 600`, `wideColumn = 560`. The link gap and the keyboard-reveal inset are **not** metrics: iOS uses `AuthTokens.fieldGap` (8) for both (`SignInView.swift:70`, `AuthKeyboardReveal.swift:65`). Divider and border widths use `AuthTokens.borderWidth` (1). No other dp literal (Konsist `featureDpLiteralsComeFromBoardMetrics`).

### 5.2 Sign in card content [F `DC/AuthSignIn.dc.html`, `Kit/Auth/SignInView.swift`]

In this order, gap 14 between each:

| # | Element | Spec | Test tag |
|---|---|---|---|
| 1 | Banner (only when `form.banner != null`) | §5.4. Always **first** in the card | `auth.banner` |
| 2 | Passkey button (only when `state.passkeyVisible`, §5.8) | `CfButton(style = Primary, size = Lg, fillWidth = true, leadingIcon = LucideGlyphs.KeyRound.imageVector())`, label Auth "Sign in with a passkey"; `loading = form.pending == Passkey`, `loadingDescription` = the same label | `sign-in.passkey` |
| 3 | Divider (same condition as 2) | `Row(horizontalArrangement = spacedBy(12))` (`AuthTokens.inset`), vertically centred: a line `AuthTokens.borderWidth` (1) high in `border` (weight 1), Auth "or with email" in `CfTypeScale.authMeta` (Figtree 400, 12/16) `mutedForeground`, the same line (weight 1). `clearAndSetSemantics {}` (iOS `.accessibilityHidden(true)`) | `sign-in.divider` |
| 4 | Email field | `CfTextField`, label Auth "Email"; §5.5 | `sign-in.email` |
| 5 | Password field | `CfSecureTextField`, label Auth "Password"; no visibility control (the board and iOS have none); §5.5 | `sign-in.password` |
| 6 | Submit | `CfButton(style = Outline, size = Lg, fillWidth = true)`, label Auth "Sign in"; `loading = form.pending == Password`, `loadingDescription` = the same label | `sign-in.submit` |
| 7 | Links row | §5.3 | `sign-in.forgot`, `sign-in.signup` |

Footer (outside the card): `CfButton(style = OnSky, fillWidth = true, minHeight = AuthTokens.footerHeight.dp)`, label Auth "New here? Plan a trip first", `textStyle = CfTheme.type.authLabel`; tag `sign-in.guest`. The `(sign-in.guest, contrast)` pair is the one accepted contrast exception (iOS OQ 33); do not change its tokens.

Screen texts: title Auth "Welcome back"; description Auth "Sign in to see your trips and plans."; scroll tag `sign-in.scroll`.

### 5.3 Links and the busy state [F `SignInView.swift:67–121`, `AuthControls.swift`]

- **Links:** "Forgot your password?" in `authLabel` (500, 14/20) `foreground`; "Create an account" in `authStrong` (600, 14/20) **`dawnText`**. Both underlined (`TextDecoration.Underline`). Each link: `cfClickable(role = Role.Button)`, min height 48 dp, horizontal padding 8 (`AuthTokens.fieldGap`), label start-aligned and vertically centred.
- **Links row layout** (port of the iOS `ViewThatFits`): one row (top-aligned, iOS `HStack(alignment: .top)`) with forgot at the start and sign-up at the end and at least `AuthTokens.fieldGap` (8) between them (iOS `Spacer(minLength: AuthTokens.fieldGap)`), when both fit at their intrinsic widths; otherwise a column, forgot first, both start-aligned. Implement as `AuthLinkRow`, a custom `Layout` that decides from `maxIntrinsicWidth` against `constraints.maxWidth` inside the measure block. No state.
- **Busy** (iOS `.disabled(model.form.busy)` on the whole screen): while a request runs, every drawn control is disabled (alpha 0.5 = `AuthTokens.disabledOpacity`, not clickable, `disabled()`): back, the passkey button, both fields, submit, both links and the footer. `AuthScreen(enabled = !busy)` passes it to the back control; the card passes it to its children. Only the pending button shows the spinner; its label and width do not change. System back stays enabled (§5.9).
- `loadingDescription` [R, OQ-5a-6]: `CfButton` requires one when `loading`; pass the button's own label. No new copy.

### 5.4 Banner, announcement and failure presentation [F `AuthFormModel.swift:5–39`, `AuthControls.swift` `AuthBanner`, `AuthFailureFeedback.swift`, `I/plan.md:2236–2243`]

- **Surface:** `AuthBanner(kind, context)` = `CfBanner(title, message, announce = false, testTag = "auth.banner")`. Phase 2 draws it exactly as `AuthStates`: row gap 10, padding 12/14, radius 18, fill `authErrorBackground`, 1 dp `authErrorBorder`, 18 dp `circle-alert` in `destructive` with a 1 dp top offset; title `authStrong`, message `authHint`, both `cardForeground`; `message = null` draws the title only (offline).
- **Kinds** (port `AuthBannerKind` exactly; `title(context)` and `body(context)`):

| Kind | Title key | Body key (`SignIn`) | Other contexts |
|---|---|---|---|
| `Credentials` | "That email and password don't match." | "Check both and try again, or reset your password." | — |
| `Locked` | "Too many attempts." (`Forgot`: "Too many requests.") | "Wait a few minutes before you try again, or reset your password now." | `TwoFactor`, `Reset`, `SignUp`, `Verify`: "Wait a few minutes before you try again."; `Forgot`: "Wait a few minutes before you ask for another link." |
| `Server` | "Something went wrong on our side." | "Try again in a moment." | `SignUp`: "Your details are kept. Try again in a moment."; `Reset`: "Your new password wasn't saved. Try again in a moment."; `Verify`: "We couldn't send the link. Try again in a moment." |
| `EmailTaken` | "There's already an account with this email." | "Sign in instead, or reset your password." | — |
| `Offline` | "Needs a connection" | `""` (draw no message) | — |

- The banner clears on any real user edit of any field (§5.5) and at the start of every submit (`begin` → `clear`).
- **Text resolution** (`fun AuthText.resolve(resources: Resources): String` in `shell/AuthBanner.kt`): `val entry = checkNotNull(CopyIndex.find(CopyTable.Auth, key))`; with `count == null` → `resources.getString(entry.resId)`; with a count → `if (entry.isPlural) resources.getQuantityString(entry.resId, count.toInt(), count) else resources.getString(entry.resId, count)`. Composables get `resources` from `LocalContext.current.resources` (it follows the app language). Banner title and body keys are passed as `AuthText(key)`.
- **Announcement** [R form of `takeAnnouncement` + `UIAccessibility.post(.announcement)`; OQ-5a-10]: `AuthAnnouncer(revision, take: () -> String?)` is the modifier of the brand row's **end** 48 dp box (§5.1), which exists on every auth screen. It keeps `var text by remember { mutableStateOf<String?>(null) }` and `var odd by remember { mutableStateOf(false) }` (**not** keyed by revision) and runs `LaunchedEffect(revision) { val t = take() ?: return@LaunchedEffect; odd = !odd; text = t }`, where `take = { form.takeAnnouncement { it.resolve(resources) } }`. The box carries `Modifier.testTag("auth.announcement").semantics { liveRegion = LiveRegionMode.Polite; text?.let { contentDescription = if (odd) it + "​" else it } }`.
  - Why this shape: Compose reports live-region changes only for a node that already existed in the previous accessibility snapshot, and it drops a node with empty bounds from the accessibility tree. So the node must be persistent and non-zero; an identical repeat is made a real content change by the zero-width space (U+200B, silent in TalkBack). A zero-size or `key(revision)`-recreated node passes Robolectric but announces nothing on a device.
  - `CfBanner` gets `announce = false`, so nothing is announced twice. Never `announceForAccessibility` (AGENTS Traps).
- **After a failure** (`AuthFailureFeedback`, port of the iOS modifier):
  - When `busy` becomes true: clear focus (`LocalFocusManager.clearFocus()`; the keyboard hides).
  - When `busy` becomes false, or `announcementRevision` changes, and `failure != null`:
    - with a field message: request focus on the first invalid field, in the order `email`, `password` (`FocusRequester`), and do not scroll to the banner;
    - banner only: `LocalSoftwareKeyboardController.current?.hide()`, keep focus cleared, then after one frame (`withFrameNanos {}`, so the inserted banner is laid out) `bannerBringIntoView.bringIntoView()` with the banner's full rect (`BringIntoViewRequester` on the banner).
  - iOS also moves VoiceOver focus to the banner. Compose has no public API to move TalkBack focus; the live-region announcement replaces it [R, OQ-5a-3].

### 5.5 Fields, IME, autofill and state ownership [F `AuthControls.swift` `AuthField`, `SignInView.swift:46–64`, `I/plan.md:494–503`; R `05 §15.4`, `§15.8`, plan "forms and IME" and "state survival"]

| Field | `keyboardOptions` | IME action | `contentType` | State |
|---|---|---|---|---|
| Email | `KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Next)` | `onKeyboardAction` → focus password | `ContentType.Username + ContentType.EmailAddress` | `remember { TextFieldState(model.email.value) }` |
| Password | `KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go)` | `onKeyboardAction` → `submit(Password)` | `ContentType.Password` | `remember { TextFieldState() }`, obfuscation `RevealLastTyped` (Phase 2 default, 05 OQ A17) |

- Field messages: `error = form.fields["email"]?.let { AuthFieldCopy.field(it) }` resolved to text. Phase 2's field draws the error row (16 dp `circle-alert`, gap 6, `authHint` `destructive`), turns the border `destructive` and sets `error(message)`.
- **Real edits only** [F fix 2026-11-08, `SignInModel.editEmail/editPassword`: `guard !form.busy, value != current`]: `class UserEditReporter(private val onEdit: (String) -> Unit) : InputTransformation`. In `TextFieldBuffer.transformInput()`: `val new = asCharSequence().toString(); if (originalText.toString() != new) onEdit(new)`. It never changes the buffer. An `InputTransformation` runs only for user input (IME, paste, autofill), never for `TextFieldState.clearText()` or `setTextAndPlaceCursorAtEnd`, so the code's own clearing never clears feedback. Pass it as the field's `inputTransformation` (Task 4 adds that parameter to `CfTextField`/`CfSecureTextField` if Phase 2 lacks it).
  - Email: `UserEditReporter { vm.editEmail(it) }` → `SignInModel.editEmail(value)`, which returns at once when busy or unchanged.
  - Password: `UserEditReporter { vm.editPassword() }` → `SignInModel.editPassword()`, which returns at once when busy, else `form.edit("password")`.
  - If the system autofill bypasses the transformation, feedback stays until the next keystroke. Record what the emulator shows in `docs/auth.md` (OQ-5a-8).
- **Busy:** both fields `enabled = false`.
- **Autofill commit** [A15, 05 OQ A14]: after a successful sign-in only, `LocalAutofillManager.current?.commit()`, passed into the model as `commitAutofill`. Never on failure.
- **State ownership:**
  - The email lives in the process-scoped `SignInModel` (§5.7) as `email: StateFlow<String>`. That mirrors iOS, where `SignInModel` lives in the app-lifetime `AuthSessionServices`: the email survives leaving Sign in and activity recreation, and process death gives an empty form. The email is not a secret.
  - The password lives **only** in the screen's `remember { TextFieldState() }`. At submit the screen passes it as a `SecretField` (`interface SecretField { fun read(): String; fun clear() }`, adapter `TextFieldSecret(state: TextFieldState)` in `shell/`; `clear()` = `state.clearText()`). The model reads it once and clears it in `finally`; it never stores it in a property. Activity recreation (locale, font scale) drops the `remember`ed state, so the password is gone.
  - Never `rememberSaveable`, `rememberTextFieldState()` (saveable), `SavedStateHandle` or a route argument for a password or the challenge id (Konsist rule from Phase 2 Task 17).
- **Keyboard reveal** (`AuthKeyboardReveal`, port of `AuthLayout.reveal` + `AuthKeyboardReveal.swift`):
  - Pure function in `AuthLayout`: `reveal(field: Rect, submit: Rect, viewport: Rect): Reveal(offset: Float, fits: Boolean)`. `pair = field ∪ submit`; `fits = pair.height <= viewport.height`; `offset = max(pair.bottom − viewport.bottom, min(0f, pair.top − viewport.top))`; when `!fits`, `offset = field.top − viewport.top`.
  - Trigger: a field is focused **and** the IME has settled. In a `LaunchedEffect(focusedField)`, `snapshotFlow { imeBottom == imeTargetBottom }` with both read from `WindowInsets.ime` and `WindowInsets.imeAnimationTarget` through `getBottom(density)`, `distinctUntilChanged().filter { it }`. Also trigger once after a field-error failure.
  - Action: `Modifier.onPlaced` stores the `LayoutCoordinates` of the focused field (the whole `CfTextField`, including its error row), the submit button and the content column (placed after `verticalScroll`) in a plain non-snapshot holder; the viewport coordinates come from §5.1 item 3. Compute `field = content.localBoundingBoxOf(fieldCoords)`, `submit = content.localBoundingBoxOf(submitCoords)`, and the viewport as `content.localBoundingBoxOf(viewportCoords)` deflated by `AuthTokens.fieldGap` (8) at top and bottom (iOS `viewport.insetBy(dx: 0, dy: AuthTokens.fieldGap)`, `AuthKeyboardReveal.swift:65`). Call `AuthLayout.reveal(field, submit, viewport)`. Then `contentRequester.bringIntoView(rect)` on a `BringIntoViewRequester` attached to the content column (`g = AuthTokens.fieldGap` in px):
    - `fits`: `rect = (field ∪ submit)` inflated by `g` vertically. The default bring-into-view scrolls by the minimal offset, which equals `reveal`'s `offset`.
    - `!fits`: iOS puts the **field top** at the deflated viewport top (`offset = field.minY − viewport.minY`). A minimal scroll to the field rect would not do that when the field is below the viewport, so request `rect = Rect(field.left, field.top − g, field.right, field.top − g + undeflatedViewportHeight)`: a viewport-tall rect starting `g` above the field, which any minimal scroll aligns to the viewport top.
  - Never feed `imeAnimationTarget` or a measured size into layout state (plan "forms and IME").
  - If the pair cannot fit on a small window at the capped font scale, do not shrink text and do not pin a second button: record it in `docs/auth.md` (iOS OQ 32 policy).

### 5.6 Exact ports: form model, failure mapping, copy, lockout [F]

**`AuthFailureMapping(failure: DataFailure, displayedFields: Set<String>, context: AuthFormContext = SignIn)`** (`AuthFormModel.swift:43–90`) → `banner: AuthBannerKind?`, `fields: Map<String, FieldMessage>`, `retryAfterSeconds: Double?`:
1. `Offline` → banner `Offline`.
2. `Throttled(seconds, _)` → banner `Locked`, `retryAfterSeconds = seconds`.
3. `Validation(messages, _)`:
   1. `context == SignUp` and `messages["email"]?.key == "This email address is already in use."` → banner `EmailTaken`.
   2. `context == SignIn` and **any** message key equals `"These sign-in details don't match our records."` → banner `Credentials`, **no fields**, return immediately.
   3. `proofFields = {"code", "recovery_code"}`; `activeProofHasError = context == TwoFactor && (displayedFields ∩ proofFields) ∩ messages.keys ≠ ∅`.
   4. For each `(name, message)`: skip when `banner == EmailTaken && name == "email"`; skip when `activeProofHasError && name in proofFields && name !in displayedFields`; if `name in displayedFields && AuthFieldCopy.supports(message)` → `fields[name] = message`, except in `TwoFactor` with `name == "code"` and key `"The authentication code was not accepted. Try again."` → `FieldMessage("That code didn't work. Codes change every 30 seconds, so try the newest one.")`; otherwise, if `banner == null` → `Server`.
4. Any other failure (`EmailUnverified`, `Unauthorized`, `Conflict`, `Server`, `Unsupported`, `ReviewExpired`, `NotEnoughCredits`, `SaveCapacity`, `ProfileInvalid`) → `Server`.
5. Finally: `banner == null && fields.isEmpty()` → `Server` (an empty validation is never silent).

**`AuthFieldCopy`** (`AuthCopy.swift:14–42`; Android shape: pure, no resources):
- `fun field(message: FieldMessage): AuthText?` with `data class AuthText(val key: String, val count: Long? = null)`; `fun supports(message) = field(message) != null`.
- Supported as is (key returned unchanged): "Please fill this in.", "Enter a valid email address, like name@example.com.", "The passwords don't match yet.", "Use both uppercase and lowercase letters.", "Use at least one letter.", "Use at least one number.", "Use at least one symbol.", "This password has appeared in a data leak. Choose a different password.", "This email verification link is invalid or has expired.", "Enter the 6-digit code from your authenticator app.", "Enter one of the recovery codes you saved when you set up two-factor authentication.", "The authentication code was not accepted. Try again.", "That code didn't work. Codes change every 30 seconds, so try the newest one.", "This recovery code was not accepted. Try another saved code.".
- Counted: keys "Use no more than :max characters." and "Use no more than {{max}} characters." → `AuthText("Use no more than %lld characters.", n)`; "Use at least :min characters." and "Use at least {{min}} characters." → `AuthText("Use at least %lld characters.", n)`. Argument name `max` when the key contains "max", else `min`. Supported **only** when `arguments[name]` is a non-string JSON number that is an integer in `0..Long.MAX_VALUE` (no fraction, not negative, not a string, present). Otherwise → null (server banner).
- Everything else → null, including the obsolete "These credentials do not match our records.".
- Compose resolves `AuthText` with `CopyIndex.find(CopyTable.Auth, key)`, and formats `count` with the current locale.

**`AuthFormModel(displayedFields: Set<String> = setOf("email", "password"), context: AuthFormContext = SignIn)`** (`AuthFormModel.swift:92–157`). Plain Kotlin, main-thread confined, state as one `StateFlow<AuthFormState>`:
- `AuthFormState(pending: Action?, banner: AuthBannerKind?, fields: Map<String, FieldMessage>, failure: DataFailure?, retryAfterSeconds: Double?, announcementRevision: Int)`, `busy = pending != null`. `enum class Action { Password, Passkey, Verify, SendResetLink, Reset, Register, CheckVerification, ResendVerification, SignOut }`.
- `begin(action): Boolean`: false when busy; else `pending = action`, `clear()`, true. One lock covers tap, IME Go and passkey.
- `finish()`: `pending = null`.
- `edit(field)`: ignored while busy; `banner = null`, `failure = null`, remove `fields[field]`.
- `clear()`: `banner = null`, `fields = {}`, `failure = null`, `retryAfterSeconds = null`.
- `receive(failure)`: store `failure`, apply `AuthFailureMapping(failure, displayedFields, context)`, `announcementRevision += 1`.
- `takeAnnouncement(resolve: (AuthText) -> String): String?`: null when `announcedRevision == announcementRevision`. Else messages = `resolve(AuthText(banner.title(context)))`, then `resolve(AuthText(banner.body(context)))` when the body is not empty, then `resolve(AuthFieldCopy.field(m))` for each `m` in `fields.keys.sorted()` whose `field(m)` is non-null. Empty → return null **without** marking it taken. Otherwise mark `announcedRevision = announcementRevision` and return the messages joined by one space.
- `showFields(set)`: ignored while busy; replace `displayedFields`, then `clear()`.

**`AuthFormContext`**: `SignIn, TwoFactor, Forgot, Reset, SignUp, Verify`.

**`LockoutWait`** → `…/model/auth/LockoutWait.kt` (`:core:model`, package `net.chronoflow.core.model.auth`, because 9f and 10c consume it and feature modules must not depend on each other; OQ-5a-2): `enum Unit { Seconds, Minutes, Hours }`; `s = max(0, seconds)`; `≥ 3600` → Hours/3600, `≥ 60` → Minutes/60, else Seconds/1; `count = s / divisor + (if (s % divisor > 0) 1 else 0)`; `localizationKey` = "Too many attempts. Try again in %lld seconds." / "… minutes." / "… hours.". **iOS Sign in does not use it** [F: `git grep LockoutWait` finds only `SettingsStrings.swift` and `WizardStrings.swift`]: the Sign in locked state is the static `Locked` banner, with no countdown, and submit stays enabled (OQ-5a-1).

**`AuthenticationMethod`** → `…/model/auth/AuthenticationMethod.kt`: `SignUp, Password, Passkey, TwoFactor` (`GuestSessionContinuation.swift:1`).

### 5.7 `SignInModel`: exact port with the Android password delta [F `Kit/Auth/SignInModel.swift`]

```kotlin
class SignInModel(
    private val source: DataSource,                       // AppGraph.dataSources.public()
    private val session: SessionAcceptor,                 // fun interface { suspend fun accept(result: SessionResult) }; :app adapts controller.accept
    private val deviceIdentity: suspend () -> String,     // DeviceIdentityStore.identity()
    private val deviceName: suspend () -> String,         // androidDeviceName(context, io), memoised by AuthServices
    val passkeyCapability: PasskeyCapability = PasskeyCapability.Shipped,      // public: SignInViewModel reads it
    private val passkey: suspend () -> SessionResult = { throw PasskeyFailure.Unavailable },  // 5b passes PasskeySignIn(...)::signIn
    private val accepted: suspend (SessionResult, AuthenticationMethod) -> Unit,             // SignInRouter::acceptedSession
    private val onTwoFactor: () -> Unit,                                                     // SignInRouter::twoFactor
)
```
- `val email: StateFlow<String>` (starts `""`), `val form = AuthFormModel()`, `val navigation = AuthNavigation()`, `var sessionFailure: SessionFailure?` (read-only outside).
- `editEmail(value)`: `if (form.busy || value == email.value) return`; `email.value = value`; `form.edit("email")`.
- `editPassword()`: `if (form.busy) return`; `form.edit("password")`. (The "changed" check is `UserEditReporter`.)
- `suspend fun submitPassword(password: SecretField, commitAutofill: () -> Unit)`:
  1. `if (!form.begin(Password)) return`. `sessionFailure = null`.
  2. `try`: `id = deviceIdentity()`; `ensureActive()`; `result = source.signIn(PasswordSignInInput(email = email.value, password = password.read(), deviceID = id, deviceName = deviceName(), draftToken = null, guestSecret = null))` (0d's property names, §2 item 6); `ensureActive()`.
  3. `Session(r)` → `accept(r.session, Password, commitAutofill, password)`.
  4. `TwoFactor(challengeId)` → `if (navigation.twoFactor(challengeId)) onTwoFactor()`. No session is accepted. A blank id leaves you on Sign in with no banner (iOS `cancelTwoFactor`).
  5. `catch (e: CancellationException)` → rethrow. `catch (e: PasskeyFailure.Canceled)` → nothing. `catch (e: SessionFailure)` → `sessionFailure = e; form.receive(DataFailure.Server(null))`. `catch (e: DataFailure)` → `form.receive(e)`. `catch (e: PasskeyFailure)` → `form.receive(Server(null))`. Any other `Exception` → `form.receive(Server(null))`. Name each type; never a bare `catch (e: Exception)` before the cancellation case (AGENTS Traps).
  6. `finally`: `password.clear()`; `form.finish()`.
- `suspend fun submitPasskey(password: SecretField)`: return at once when `!passkeyCapability.enabled` [R, 5b]; `if (!form.begin(Passkey)) return`; `sessionFailure = null`; `accept(passkey(), Passkey, commitAutofill = {}, password = password)`; same catches; `finally { password.clear(); form.finish() }` (iOS `submitPasskey` also clears the password in its `defer`, `SignInModel.swift:127–130`). In 5a the default `passkey` throws `PasskeyFailure.Unavailable` → `Server` banner (iOS mock parity: "the app mock reports designed server failure through its unavailable assertion provider", `AuthSessionServices.swift:96–97`).
- `private suspend fun accept(result, method, commitAutofill, password: SecretField?)`: `ensureActive()`; then **`withContext(NonCancellable) { session.accept(result); commitAutofill(); password?.clear(); accepted(result, method) }`**. `commitAutofill()` runs **before** clearing, so the password provider still sees the typed value.
  - Why `NonCancellable`: in Swift, cancelling the request while `session.accept` is suspended still lets `accept` return and `accepted` route (`committedSessionUsesStoredGateWithAndWithoutCancellation` asserts `Task.isCancelled == cancelAfterCommit` inside the route callback and still expects the route). In Kotlin, a cancelled caller resumes from `session.accept` (a `withContext`/`await`) with a `CancellationException`, so without `NonCancellable` a committed session would never route. A `SessionFailure` thrown inside still reaches the catches.
- `fun clearPassword()` is not needed (the screen owns the password). `fun clearSecrets()`: `navigation.reset()`; `form.clear()`; keep `email` (iOS `clearSecrets`; 5b adds `twoFactor?.cancel()`).
- Never trim or lower-case the email (the server does). No client validation: a blank form is sent and the server or mock answers "Please fill this in.".

**`AuthNavigation`** (port of `AuthNavigation.swift`, memory only; the iOS `path` is replaced by root routes, §5.9):
- `var challengeID: String?` (private set).
- `fun twoFactor(challengeID: String): Boolean`: if `challengeID.isBlank()` → `cancelTwoFactor()`, return false; else store it, return true.
- `fun discardChallenge()` and `fun cancelTwoFactor()`: `challengeID = null`. `fun reset()`: `challengeID = null`.
- `companion fun route(account: SessionAccount): Route`: `!hasVerifiedEmail` → `Verify`; `policies.acceptedTermsVersion != policies.termsVersion` → `Terms`; `!walkthroughSeen` → `Walkthrough(replay = false)`; else `Today`. It is pure and tested; production routing goes through 4e's stored gate (§5.9).
- Its `toString()` must not print the challenge (override it).

**Passkey seam** (`…/auth/model/passkey/`, the package 5b uses): `@JvmInline value class PasskeyCapability(val enabled: Boolean) { companion object { const val SERVER_READY = false; val Shipped = PasskeyCapability(SERVER_READY) } }` and `sealed class PasskeyFailure : Exception(null, null, false, false) { data object Canceled; data object InvalidEncoding; data object Unavailable; data object Busy; data object AlreadyRegistered }` (`PasskeySignIn.swift:4–6`).

**`SignInRouter`** (interface in `…/auth/signin/`, implemented in `:app`): `fun back()`, `fun forgot()`, `fun signUp()`, `fun guest()`, `fun twoFactor()`, `suspend fun acceptedSession(result: SessionResult, method: AuthenticationMethod)`. `AuthServices` builds the model with `accepted = router::acceptedSession` and `onTwoFactor = router::twoFactor`; the screen calls `back`, `forgot`, `signUp` and `guest` directly.

### 5.8 `SignInViewModel`, `AuthServices` and the screen [R, A4]

- **`AuthServices`** (`…/app/auth/AuthServices.kt`), a lazy `AppGraph` member `auth`. Members: `val signIn: SignInModel` (process-scoped, built lazily on first use, on main; no I/O in construction), `val router: SignInRouter` (the §5.9 implementation), and `deviceName` memoised on `dispatchers.io`. Mirrors iOS `AuthSessionServices.model`.
- **`SignInViewModel(model: SignInModel)`** (`…/auth/signin/`), created in `:app` with `viewModelFactory { initializer { SignInViewModel(this[AppGraphKey]!!.auth.signIn) } }` (A4). It adapts the model for Compose:
  - `uiState: StateFlow<SignInUiState>` = `combine(model.email, model.form.state) { … }` plus `passkeyVisible = model.passkeyCapability.enabled` (so a test builds the model with `PasskeyCapability(true)` to compose the passkey controls), `stateIn(viewModelScope, Eagerly, …)`. `SignInUiState` holds no password and no challenge.
  - `submitPassword(secret: SecretField, commitAutofill: () -> Unit)` and `submitPasskey(secret: SecretField)`: `if (request != null || uiState.value.form.busy) return`; `request = viewModelScope.launch { try { model.submit…() } finally { request = null } }` (iOS `SignInView.submit`).
  - `leave()`: `request?.cancel()`; `request = null` (iOS `SignInView.onDisappear`). The screen calls it from `DisposableEffect(Unit) { onDispose { vm.leave() } }`.
  - `editEmail(value)`, `editPassword()` forward to the model.
  - 4e's `RootContent` has no ViewModel entry decorator, so this ViewModel is activity-scoped. That is fine because all state is in `SignInModel` and `leave()` cancels the request whenever the screen leaves (OQ-5a-5).
- **`SignInScreen(vm, router)`** → stateless `SignInContent(state, emailState, passwordState, callbacks)` per §5.2–§5.5 and §5.10. It reads `uiState` with `collectAsStateWithLifecycle()`. `BackHandler { router.back() }` (always enabled, also while busy).

### 5.9 Navigation, back and acceptance [F `05 §5`, `§15.5`, `StoredDataReadiness.swift:68–84`, `AuthRootView.swift`; R A8 and 4e's root routes]

4e's `RootContent` shows a single-entry stack `[route]` and finishes the activity on back. Auth screens are root routes, so the auth "stack" is expressed with explicit navigation:

| From Sign in | `SignInRouter` | Effect |
|---|---|---|
| `auth.back` or system back | `back()` | `rootNavigator.navigate(Route.Welcome)`. Welcome shows its final state, with no intro and no splash (3c: the process-level intro already played). Also when Sign in became the screen after sign-out. |
| `sign-in.forgot` | `forgot()` | `navigate(Route.Forgot)` (5c builds it) |
| `sign-in.signup` | `signUp()` | `navigate(Route.SignUp)` (5d) |
| `sign-in.guest` | `guest()` | `navigate(Route.GuestWizard)` (11a) |
| Two-factor result | `twoFactor()` | `navigate(Route.TwoFactor)`, no arguments (5b) |
| Session accepted | `acceptedSession(result, method)` | see below |

- **`acceptedSession`** calls `readiness.routeAcceptedSession(navigate = { rootNavigator.navigate(it) }, verification = { epoch -> onVerify(epoch) })` and returns when it returns (use 4e's exact parameter types; `onVerify` is 4e's T10 verification callback). 4e already runs the body in the readiness's own scope, so the caller only awaits it (4e §5.4); do not wrap it in another `appScope.launch`. Together with `SignInModel.accept`'s `NonCancellable` block (§5.7), a cancel of the Sign in request after the session committed still routes (iOS: "The committed session owns routing … after its request is canceled"), and the form stays busy until routing returns, as on iOS. `routeAcceptedSession` itself re-checks the active session after reading the gate, so a sign-out in between routes nothing. Do not duplicate that logic. 4e's running `SessionGateRouter` may navigate to the same route too; that is iOS behaviour (iOS calls both), so keep it.
- **Leaving auth clears secrets** (port of `AuthRootView.onDisappear { model.clearSecrets() }`): `AuthServices` collects `rootNavigator.state` on `appScope` with `dispatchers.main`, starting when `auth` is first built (Sign in is the first auth screen, so it is always running before anyone leaves auth). When the route changes from an auth route (`SignIn`, `TwoFactor`, `Forgot`, `SignUp`, `Reset`) to any other route, it calls `signIn.clearSecrets()` (form and challenge cleared, email kept). Moving between auth routes clears nothing.
- **Welcome:** `WelcomeDestinations.signIn()` now calls `rootNavigator.navigate(Route.SignIn)` in every build type. Remove the debug `signIn` seam from `DebugWelcomeDestinations` and keep its `guestWizard` seam until 11a. 3c made the release/perf wiring the stateless `object InertWelcomeDestinations` (3c §6), which cannot reach the navigator: replace it with `class RoutedWelcomeDestinations(private val navigator: RootNavigator) : WelcomeDestinations` (`signIn()` navigates, `planMyTrip()` stays a no-op, `seam` stays null), built from `graph.rootNavigator`, and let the debug class take the navigator for `signIn()` too. Grep the merged 3c code first (`grep -rn "WelcomeDestinations" app/`) and adapt to its real shape.
- **3c tests that this changes** (update them in Task 8, keep their names): `WelcomeSeamTest` (Sign in now shows `sign-in.email`, not `route.signIn`; back from Sign in still shows `welcome.wordmark` at its final bounds with no intro); `WelcomeDestinationsReleaseTest` (release wiring is `RoutedWelcomeDestinations`; `signIn()` makes the navigator's route `SignIn`; `seam` stays null); `WelcomeJourneyTest` (its last wait becomes `waitUntilExactlyOneExists(hasTestTag("sign-in.email"), 3_000)`).
- 4e already routes `onSignedOut` and the `CF_AUTH_TEST_AUDIT_SCREEN=sign-in` seed to `Route.SignIn`; add nothing there.
- Predictive back: Sign in uses `BackHandler`, so the system shows no cross-screen preview into Welcome (single-entry root stack). Accepted for 5a (OQ-5a-7).

### 5.10 Accessibility [F `I/plan.md:503`; R `05 §15.11`]

- Reading order: back, brand (moon hidden), heading (`heading()`), description, banner, passkey (when composed), email, password, submit, forgot, sign up, footer. The layout order already gives this; set no `traversalIndex`. After a failure, the announcer box (§5.4) is also a stop between brand and heading that reads the last announcement (OQ-5a-10).
- Fields: the label is the content description; errors through `error()`; the password field exposes no text.
- Targets ≥ 48 dp everywhere; color is never the only signal (the error row has an icon and text).
- Contrast: board values are accepted (iOS OQ 33); only `(sign-in.guest, contrast)` is exempt. Do not change tokens to pass a check.

### 5.11 Copy [F `Kit/Auth/Resources/Localizable.xcstrings` at the pin, generated by 0c]

All strings already exist in en and nl in 0c's generated `auth` table. **Add no string in this phase.** Find each resource name in `tools/l10n/names.lock.json` (table `auth`); never guess a slug. Static labels use `stringResource(R.string.<name>)`; banner and field keys resolve through `CopyIndex.find(CopyTable.Auth, key)`. If a key below is missing from the lock, stop and report it (it means 0c missed an iOS key); do not hand-add it.

| English key | nl |
|---|---|
| Back | Terug |
| ChronoFlow | ChronoFlow |
| Welcome back | Welkom terug |
| Sign in to see your trips and plans. | Log in om je reizen en plannen te bekijken. |
| Sign in with a passkey | Inloggen met een passkey |
| or with email | of met e-mail |
| Email | E-mailadres |
| Password | Wachtwoord |
| Sign in | Inloggen |
| Forgot your password? | Wachtwoord vergeten? |
| Create an account | Een account aanmaken |
| New here? Plan a trip first | Nieuw hier? Plan eerst een reis |
| That email and password don't match. | Dit e-mailadres en dit wachtwoord horen niet bij elkaar. |
| Check both and try again, or reset your password. | Controleer ze allebei en probeer het opnieuw, of herstel je wachtwoord. |
| Too many attempts. | Te veel pogingen. |
| Wait a few minutes before you try again, or reset your password now. | Wacht een paar minuten voordat je het opnieuw probeert, of herstel nu je wachtwoord. |
| Something went wrong on our side. | Er ging bij ons iets mis. |
| Try again in a moment. | Probeer het zo meteen opnieuw. |
| Needs a connection | Verbinding nodig |
| Please fill this in. | Vul dit veld in. |
| Enter a valid email address, like name@example.com. | Vul een geldig e-mailadres in, zoals naam@voorbeeld.nl. |
| Use no more than %lld characters. | Gebruik niet meer dan %lld tekens. |
| Use at least %lld characters. | Gebruik minstens %lld tekens. |

The other contexts' banner keys of §5.4 also exist (for example "Too many requests." / "Te veel verzoeken.", "Sign in instead, or reset your password." / "Log in of herstel je wachtwoord.").

## 6. Kotlin shape [R]

```
core/model/…/model/auth/LockoutWait.kt
core/model/…/model/auth/AuthenticationMethod.kt
feature/auth/…/auth/model/AuthBannerKind.kt         enum + title(context) / body(context)
feature/auth/…/auth/model/AuthFormContext.kt
feature/auth/…/auth/model/AuthFailureMapping.kt
feature/auth/…/auth/model/AuthFieldCopy.kt          AuthText, field(), supports()
feature/auth/…/auth/model/AuthFormModel.kt          AuthFormState, Action
feature/auth/…/auth/model/AuthNavigation.kt
feature/auth/…/auth/model/SecretField.kt            interface only (pure)
feature/auth/…/auth/model/SessionAcceptor.kt
feature/auth/…/auth/model/SignInModel.kt
feature/auth/…/auth/model/passkey/PasskeyCapability.kt
feature/auth/…/auth/model/passkey/PasskeyFailure.kt
feature/auth/…/auth/shell/AuthLayout.kt             content(size, insets), reveal(...)  (pure)
feature/auth/…/auth/shell/AuthMetrics.kt            @BoardMetrics("AuthSignIn", "AuthStates")
feature/auth/…/auth/shell/AuthScreen.kt
feature/auth/…/auth/shell/AuthBrand.kt
feature/auth/…/auth/shell/AuthLinkRow.kt
feature/auth/…/auth/shell/AuthBanner.kt             AuthBanner + AuthAnnouncer
feature/auth/…/auth/shell/AuthFailureFeedback.kt
feature/auth/…/auth/shell/AuthKeyboardReveal.kt
feature/auth/…/auth/shell/UserEditReporter.kt
feature/auth/…/auth/shell/TextFieldSecret.kt
feature/auth/…/auth/signin/SignInRouter.kt
feature/auth/…/auth/signin/SignInViewModel.kt       + SignInUiState, factory
feature/auth/…/auth/signin/SignInScreen.kt          SignInScreen → SignInContent (stateless)
app/…/app/auth/AuthServices.kt                      SignInModel owner, SignInRouter impl, auth-exit observer
app/…/app/CfRoot.kt                                 the SignIn entry inside RootContent (edit)
app/…/app/auth/SignInViewModelFactory.kt             factory reading AppGraphKey
```
`model/` stays free of Android and Compose imports (JVM-testable). `AuthLayout` uses `androidx.compose.ui.geometry.Rect`/`Size` (plain Kotlin classes). Add `:feature:auth` to `:app` and `testImplementation(project(":core:data-mock"))` plus `:core:testing`/`:core:testing-compose` to `:feature:auth` if missing.

## 7. Tasks

Each task is one commit: Conventional Commits, lower-case imperative subject, **no trailer**. Tick the plan box named in brackets in the same commit. Kotlin test classes keep the Swift suite names; methods keep the Swift test names.

### Task 1: LockoutWait and AuthenticationMethod (`feat(model): port lockout wait and authentication method`)
- Files: `…/model/auth/LockoutWait.kt`, `…/model/auth/AuthenticationMethod.kt` (§5.6).
- **JVM** `core/model/src/test/…/auth/LockoutWaitTests.roundsUpAtUnitBoundaries`: (0, Seconds, 0), (1, Seconds, 1), (45, Seconds, 45), (59, Seconds, 59), (60, Minutes, 1), (61, Minutes, 2), (2818, Minutes, 47), (3599, Minutes, 60), (3600, Hours, 1), (3601, Hours, 2). Android addition `negativeSecondsClampToZero`: −5 → (Seconds, 0); and the three `localizationKey` strings exactly.

### Task 2: Form model and failure mapping (`feat(auth): port the auth form model and failure mapping`)
- Files: `model/AuthBannerKind.kt`, `AuthFormContext.kt`, `AuthFailureMapping.kt`, `AuthFieldCopy.kt`, `AuthFormModel.kt` (§5.4 table, §5.6).
- **JVM** (`feature/auth/src/test/…/auth/model/`):
  - `AuthFailureMappingTests`: `rejectedCodeUsesBoardCopyOnlyForTheTwoFactorCodeField`, `inactiveProofIsIgnoredOnlyAlongsideAnActiveProofError` (parameterised over `code`, `recovery_code`), `onlyApprovedMessagesReachDisplayedFields`, `offlineUsesTheRNBannerAndThrottleRetainsDeadline` (`Throttled(73.0, 429)` → `Locked`, `retryAfterSeconds == 73.0`), `obsoleteCredentialsKeyUsesTheServerFallback`, `fieldArgumentsAreWhitelistedAndNeverExposeUnknownEnglish` (`AuthText` keys and counts: `{max: 255}` → count 255; `{max: "255"}`, `{max: 2.5}`, `{max: -1}`, missing → null), `backendFieldMessagesMapAndLocalizeInBothLanguages` (mapping half; the en/nl half is in `AuthCopyResourcesTest`).
  - `AuthFormModelTests`: `oneLockCoversTapReturnAndPasskeyAndEditsClearFeedback`, `announcementsAreConsumedOncePerFailureAndNeverRepeatOnEdit`, `everyFailureHasVisibleLocalizedFeedbackAndOneAnnouncement` (one parameterised case per failure in the Swift list, `AuthFormModelTests.swift:37–58`, 22 failures; each runs over the **seven** Swift `(context, displayedFields)` pairs: `SignIn {email, password}`, `SignUp {name, email, password, password_confirmation}`, `Forgot {email}`, `Reset {password, password_confirmation}`, `TwoFactor {code}`, `TwoFactor {recovery_code}`, `Verify {}`; a fake resolver returning the key; after `begin(Password)`, `receive`, `finish()`: `failure == it`, not busy, fields ⊆ displayed, every field resolves through `AuthFieldCopy.field`, a banner or a field exists, the announcement is non-null and contains every field text and the banner title, an immediate second `takeAnnouncement` is null, a second identical `receive` announces the same text again, and after `clear()` `takeAnnouncement` is null), `fieldOnlyAnnouncementsClearOnEditAndUseTheLatestFailure`.
  - `AuthBannerKindTests` (Android): every `(kind, context)` title and body equals the §5.4 table.
- **Robolectric** `AuthCopyResourcesTest` (`@Logic`): every key that `AuthBannerKind` and `AuthFieldCopy` can return resolves through `CopyIndex.find(CopyTable.Auth, key)` to a non-empty string in en and nl. nl spot checks: "Er ging bij ons iets mis. Probeer het zo meteen opnieuw." (server announcement), "Verbinding nodig", `AuthText("Use no more than %lld characters.", 255)` → "Gebruik niet meer dan 255 tekens.", "Use both uppercase and lowercase letters." → "Gebruik hoofdletters en kleine letters.".

### Task 3: Navigation and layout math (`feat(auth): port auth navigation and layout math`)
- Files: `model/AuthNavigation.kt`, `model/SecretField.kt`, `shell/AuthLayout.kt` (§5.5, §5.7).
- **JVM**:
  - `AuthLayoutTests.zeroInsetBoardAndEveryDeviceRespectFourInsets`: 375 × 812 with zero insets → `(16, 16, 343, 772)`; every profile of `DeviceProfiles.all`, plus an asymmetric fixture (left 24, top 0, right 62, bottom 20): `minX = left + 16`, `maxX = w − right − 16`, `minY = top + 16`, `maxY = h − bottom − 24`.
  - `AuthLayoutTests.measuredKeyboardViewportRevealsPairOrReportsOverflow`: field `(20, 300, 300 × 100)`, submit `(20, 500, 300 × 44)`; a viewport 330 high fits and both rects are inside after the offset; a viewport 150 high does not fit and the offset puts the field top at the viewport top.
  - `AuthNavigationTests` (Android): blank and whitespace ids → `twoFactor` returns false and `challengeID == null`; a valid id is stored; `discardChallenge`, `cancelTwoFactor` and `reset` clear it; `toString()` does not contain the id.

### Task 4: Sign-in glyphs and the field input hook (`feat(designsystem): add sign-in glyphs and a field input transformation`)
- Add two board entries to `tools/icons/lucide.json`: `chevron-left` (board `AuthSignIn`, first `m15 18-6-6 6-6`) and `key-round` (board `AuthSignIn`, first `M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z`, including its filled circle). Regenerate with `make vectors`; commit the generated file.
- Update `LucideGlyphsTest`: the glyph count grows by exactly 2 from the merged value (Phase 2 wrote 32; 1c or a parallel phase may already have changed it, so read the merged assertion and add 2); `ChevronLeft.paths.single().d == "m15 18-6-6 6-6"`; `KeyRound` has 2 paths and the second is `filled`.
- If Phase 2's `CfTextField`/`CfSecureTextField` lack it, add `inputTransformation: InputTransformation? = null`, passed to the underlying `BasicTextField`/`BasicSecureTextField`. Add a `CfTextFieldTest` case: a transformation that records calls sees typed input, not `state.clearText()`.

### Task 5: SignInModel (`feat(auth): port the sign-in model`)
- Files: `model/SessionAcceptor.kt`, `model/SignInModel.kt`, `model/passkey/PasskeyCapability.kt`, `model/passkey/PasskeyFailure.kt`, `signin/SignInRouter.kt` (§5.7).
- **JVM** `SignInModelTests` (`runTest(timeout = 60.seconds)`). Source: `MockDataSource(store = MemoryMockStateStore(), seed = MockServerState(accounts = listOf(MockServerAccount(snapshot = 0d fixture account/snapshot.json with hasVerifiedEmail per case and policies terms "1.0" / privacy "1.0" / accepted "1.0", password = "Password123!", twoFactorCode = null or "123456", walkthroughSeen = true))), delay = { 0.0 }, sleep = {}, debugAppSeed = false, io = <the test dispatcher>)` (port of `authTestSource`; use 4c's exact names). Fakes in `feature/auth/src/test/…/testing/`: `FakeSessionController : SessionAcceptor` (records accepts, can throw a `SessionFailure`), `FakeSecretField` (value + `cleared` flag), `RecordingRoutes` (`accepted`, `twoFactor` calls). Device id `"0199a3c0-0000-7000-8000-000000000001"`, device name `"test"`. Ported:
  - `verifiedAndUnverifiedAcceptOnceAndClearSecrets` (both values): starts with empty email; one accept; `accepted` called once; `AuthNavigation.route(account)` = `Today` / `Verify`; secret cleared; email kept; not busy.
  - `twoFactorNeverAcceptsSessionAndBackDiscardsChallenge`: `onTwoFactor` once, `navigation.challengeID != null`, no accept, secret cleared; `discardChallenge()` → null.
  - `blankCredentialsThrottleServerAndOfflinePreserveEmailAndClearPassword`: blank → `fields["email"].key == "Please fill this in."`; email `retained@example.com` + password `wrong` → `Credentials`, no fields, secret cleared; then for `Server(500)`, `Throttled(40.0, 429)`, `Offline` via `failNext(MockMethod.SignIn, …)`: `editPassword()` clears the banner first; after the submit `form.failure == it`, secret cleared, email kept; it ends with the `Offline` banner.
  - `unchangedEmailPreservesServerFeedbackUntilTheValueChanges` (the Swift case uses the password; its Android twin is `SignInScreenTest.unchangedPasswordPreservesServerFeedbackUntilTheValueChanges` in Task 7): after `receive(Validation({"password": fill, "device_id": fill}, 422))`, `editEmail(sameValue)` keeps banner `Server`, the password message and `failure`; a different value clears banner and failure.
  - `concurrentAttemptsAndEditingWhileBusyAreIgnored`: device identity suspends on a `CompletableDeferred`; a second `submitPassword` and a `submitPasskey` (capability on) return at once; `editEmail("changed")` and `editPassword()` are ignored; after completing the deferred: exactly one accept, email `demo@example.com`, secret cleared.
  - `storeFailuresShowAndAnnounceTheServerBannerWithoutRouting` (one case per `SessionFailure`: `CredentialsUnavailable`, `DatabaseUnavailable`, `CredentialWrite`, `InvalidResult`, `Superseded`): no `accepted`, banner `Server`, secret cleared, `sessionFailure == it`, `takeAnnouncement` non-null; then the same through `submitPasskey(secret)` with capability on and a fake `passkey` returning a valid session.
  - `accountGatePrecedenceUsesAvailableAccountRecord`: unverified → `Verify`; accepted "1" vs terms "2" → `Terms`; accepted "2", not seen → `Walkthrough(false)`; seen → `Today`.
  - Android additions: `autofillIsCommittedBeforeThePasswordIsCleared` (order on success: accept → commitAutofill → clear → accepted; on failure commitAutofill is never called); `passkeyCancelIsSilent` (`PasskeyFailure.Canceled` → no banner, no announcement, not busy, secret cleared); `passkeyIsANoOpWhileTheCapabilityIsOff` (form never busy, `passkey` never called, secret not cleared); `unavailablePasskeyShowsTheServerBanner` (and the secret is cleared, iOS `submitPasskey` `defer`); `clearSecretsKeepsTheEmail`; `cancellationRethrowsAndUnlocksTheForm` (cancel during `deviceIdentity` → not busy, secret cleared, no banner).
- `committedSessionUsesStoredGateWithAndWithoutCancellation` and `signOutDuringStoredGateReadPreventsStaleRouting` need the real controller and readiness: they are Task 8.

### Task 6: Auth shell (`feat(auth): add the auth shell`) [ticks "Auth shell"]
- Files: `shell/AuthMetrics.kt`, `AuthScreen.kt`, `AuthBrand.kt`, `AuthLinkRow.kt`, `AuthBanner.kt` (with `AuthAnnouncer`), `AuthFailureFeedback.kt`, `AuthKeyboardReveal.kt`, `UserEditReporter.kt`, `TextFieldSecret.kt` (§5.1, §5.3–§5.5).
- **Robolectric** (`feature/auth/src/test/…/auth/shell/`, `ProfileRule` + `ProfileHost`, a fixed sample card with two `CfTextField`s and a `CfButton`, and a footer button):
  - `AuthScreenGeometryTest` (`@Layout`), one test per profile (`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`):
    - brand row top = safe top + 16; content start = safe start + 16; below 600 dp, content end = width − safe end − 16;
    - on `foldInner` and `landscapeSideNav` (≥ 600 dp): column width 560 ± 0.5 dp and centred in the window minus insets (± 0.5 dp);
    - heading top = brand row bottom + 32; card top ≥ subtitle bottom + 24; footer top = card bottom + 12; footer height ≥ 48;
    - on `pixel8` (content shorter than the viewport): footer bottom = window height − safe bottom − 24 (± 0.5 dp);
    - on `small` and `largeFont`: `performScrollTo()` reaches the footer.
  - `AuthLinkRowTest` (`@Layout`): en on `pixel8` → one row (equal tops, forgot start = row start, sign-up end = row end); nl on `DeviceProfiles.small.copy(fontScale = 2f)` (`ProfileRule` caps it to 1.3) → stacked, forgot above sign-up, both start-aligned; both ≥ 48 dp high.
  - `AuthAnnouncerTest` (`@Logic`): the `auth.announcement` node exists before any failure, has `LiveRegionMode.Polite`, no content description, and non-zero bounds (48 × 48 dp); after `receive(Server(null))` its content description with U+200B removed is "Something went wrong on our side. Try again in a moment."; a second identical `receive` changes the raw content description (the U+200B suffix toggles) while the text without it is the same; an `edit` changes nothing; the `auth.banner` node has no live region.
  - `UserEditReporterTest` (`@Logic`): typing calls `onEdit` once per change; `performTextReplacement` with the same text does not call it; `state.clearText()` from code does not call it.

### Task 7: Sign-in screen and ViewModel (`feat(auth): add the sign-in screen`) [ticks "Fields", "SignInViewModel", "Passkey entry"]
- Files: `signin/SignInViewModel.kt` (+ `SignInUiState`, factory), `signin/SignInScreen.kt` (§5.2–§5.5, §5.8, §5.10).
- **Robolectric** (`feature/auth/src/test/…/auth/signin/`, `@Logic`, `pixel8`, en unless stated; a real `SignInModel` on a `MockDataSource` as in Task 5, a recording router; queries by tag only):
  - `SignInScreenTest`:
    - `unchangedPasswordPreservesServerFeedbackUntilTheValueChanges` (Swift name): with a `Server` banner and a password field message shown, `performTextReplacement` with the same password keeps both; a different value clears the banner and the password message; the model's own clearing of the password after a submit does not clear new feedback.
    - `imeChainMovesEmailToPasswordToSubmit`: `performImeAction()` on email focuses password; on password it submits once.
    - `busyDisablesEveryControlAndIgnoresASecondGo`: with the mock blocked (`CompletableDeferred` device identity), every tagged control is disabled and a second Go does nothing.
    - `fieldFailureFocusesTheFirstInvalidField` and `bannerFailureLeavesNoFocusAndShowsTheBanner` (banner fully inside the scroll viewport).
    - `autofillContentTypes`: `SemanticsProperties.ContentType` of `sign-in.email` is `Username + EmailAddress`; of `sign-in.password` is `Password`.
    - `passkeyControlsAreAbsentWhileTheCapabilityIsOff`: `sign-in.passkey` and `sign-in.divider` do not exist; with a `SignInModel` built with `passkeyCapability = PasskeyCapability(true)` both exist in the order banner, passkey, divider, email; tapping passkey shows the `Server` banner and opens no system UI.
    - `traversalFollowsTheBoardOrder`: tops increase in the §5.10 order (one `onNodeWithTag` per tag); `auth.heading` has `Heading`; the divider has no semantics.
  - `SignInSecretsTest`:
    - `recreationKeepsTheEmailAndDropsThePassword`: `StateRestorationTester`; type `demo@example.com` and `Secret123!`; `emulateSavedInstanceStateRestore()` → email `demo@example.com`, password empty.
    - `nothingSavedContainsASecret`: host the screen under a recording `SaveableStateRegistry`; after typing `Secret123!` and a two-factor submit, the flattened `performSave()` map contains neither `Secret123!` nor the challenge id.

### Task 8: App wiring and accepted-session routing (`feat(app): route sign-in and hand over to the session gate`)
- Files: `…/app/auth/AuthServices.kt` (§5.8, §5.9), `AppGraph` member `auth`, `CfRoot` entry for `Route.SignIn` inside `RootContent` (`SignInScreen(viewModel(factory = SignInViewModelFactory), graph.auth.router)`), with `app/auth/SignInViewModelFactory.kt` reading `AppGraphKey`, `WelcomeDestinations.signIn()` → `navigate(Route.SignIn)` with `RoutedWelcomeDestinations` replacing `InertWelcomeDestinations` (§5.9), the removal of the debug `signIn` seam, and the three 3c test updates listed in §5.9 (`WelcomeSeamTest`, `WelcomeDestinationsReleaseTest`, `WelcomeJourneyTest`).
- **JVM** (`app/src/test/…/auth/`):
  - `SignInModelRoutingTests` (the app half of the Swift suite `SignInModelTests`; Swift method names; password and passkey methods; the two-factor variant moves to 5b): `committedSessionUsesStoredGateWithAndWithoutCancellation` (parameterised over every `Gate` × cancel-after-commit) and `signOutDuringStoredGateReadPreventsStaleRouting` (`Verify`, `Terms` × cancel). Use 4e's real `SessionController` and `StoredDataReadiness` with the in-memory credential and account fakes 4e's own tests use. If those fakes are private to 4e's test source set, move them to `:core:testing` in this commit. Assert the routes `RootNavigator` received, the verification epochs, a cleared secret, not busy and no banner.
  - `AuthServicesTest`: `back`, `forgot`, `signUp`, `guest` and `twoFactor` navigate to `Welcome`, `Forgot`, `SignUp`, `GuestWizard` and `TwoFactor`; no route value contains a password or the challenge; leaving auth (`SignIn` → `Today`) clears the form and the challenge and keeps the email; `SignIn` → `Forgot` clears nothing.
- `ReleaseReadinessTest` stays green (nothing new in `src/debug`).

### Task 9: Guards and board captures (`test(auth): guard sign-in states and capture the boards`)
- **`SignInGuardsTest`** (`@Layout`, parameterised, each case its own test) runs 0e `Guards.assertAll(rule, profile, headingTags = setOf("auth.heading"))` on `SignInContent`, light theme:
  - state `locked` with the passkey capability on (the tallest card) × profiles `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en, nl (10 cases);
  - `fieldError` (both fields "Please fill this in.") on `small` and `largeFont`, nl (2); `busy` on `pixel8`, en (1); `none` with the capability off on `pixel8`, en (1);
  - `wrong` (the `AuthSignInError` board: `Credentials` banner) on `small` × en, nl (2), because plan.md 5a asks for guards on `AuthSignInError` too;
  - `authStates` (the `AuthStatesSample` composable of `AuthStatesCaptureTest`, moved to a shared test helper in `feature/auth/src/test/…/auth/shell/`) on `small`, nl (1): it has no heading and no control, so this case calls `Guards.assertTextFits(rule, profile)` and `Guards.assertInk(rule, profile)` instead of `assertAll`.
  - 17 cases, each < 5 s. Record the class run time in the PR.
- **`SignInBoardCaptureTest`** (`@Capture`): 375 × 812 dp, density 3, zero insets, en, capability **on** (as drawn), empty fields, through `BoardCapture.capture(rule, name)`: `AuthSignIn_none_en_light`, `AuthSignIn_none_en_dark`, `AuthSignIn_locked_en_light`, `AuthSignIn_locked_en_dark`, `AuthSignIn_server_en_light`, `AuthSignIn_server_en_dark`, `AuthSignInError_en_light`, `AuthSignInError_en_dark`. Non-board captures under `auth/`: `auth/SignInOffline_en_light`, `auth/SignInOffline_en_dark`, `auth/SignInPasskeyOff_en_light` (what ships), `auth/SignInFieldError_en_light`.
- **`AuthStatesCaptureTest`** (`@Capture`): the test-only composable `AuthStatesSample` (shared with `SignInGuardsTest`) on `background`, 375 dp wide, that stacks `AuthBanner`s in the board's order: Sign in `Credentials`, `Locked`, `Server`; Create an account `EmailTaken` (SignUp), `Server` (SignUp); Forgot `Locked` (Forgot), `Server` (Forgot); Choose a new password `Server` (Reset); Two-factor `Locked` (TwoFactor), `Server` (TwoFactor); Verify `Server` (Verify). Captures `AuthStates_en_light`, `AuthStates_en_dark`. No section headings (they are board documentation).
- No stored Roborazzi references (opt-in only).

### Task 10: Emulator journey and keyboard check (`test(app): sign in the demo account on the emulator`) [ticks "Tests"]
Both in `app/src/androidTest/kotlin/net/chronoflow/app/auth/`, extending 0e's `ScreenTest`, class-level `@Smoke`, each < 10 s, waits through `waitFor`/`waitUntilExactlyOneExists`, no sleeps.
- **`SignInJourneyTest.typedDemoSignInReachesTheAppRoot`** (the one journey): `launch(mapOf("CF_AUTH_TEST_AUDIT_SCREEN" to "sign-in", "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1", "CF_MOCK_DELAY" to "0"))`. Wait for `sign-in.email`; `performTextInput("demo@example.com")`; `performImeAction()`; assert `sign-in.password` is focused; `performTextInput("password")` (4c `MockScenarios` password); `performImeAction()`; `waitFor(5_000) { RouteProbe.state.route == Route.Today }`; assert `sign-in.email` no longer exists. Record the duration in the PR.
- **`SignInKeyboardTest.fieldAndSubmitStayAboveTheSoftwareKeyboard`** (one seeded single-screen check): same seed. Click `sign-in.email`. Wait until the IME is really visible: `ViewCompat.getRootWindowInsets(decorView)!!.isVisible(WindowInsetsCompat.Type.ime())` and its bottom inset ≥ 100 dp; otherwise fail with "software keyboard not visible — check show_ime_with_hard_keyboard" (iOS OQ 32: minimized keyboards gave false passes; 0a's `prep-functional` sets `show_ime_with_hard_keyboard 1`). Wait for the reveal to settle (`waitFor { bounds stable for two consecutive polls }`). Assert in window coordinates: `sign-in.email` bottom ≤ IME top and `sign-in.submit` bottom ≤ IME top; both tops ≥ status-bar bottom. `performImeAction()` → password focused → the same assertions for `sign-in.password` and `sign-in.submit`.
- If your sandbox cannot run the emulator, report the exact commands (`make emu`, `make targeted ONLY="app:SignInJourneyTest app:SignInKeyboardTest"`) and their output; the orchestrator runs them.

### Task 11: Docs (`docs(auth): record sign-in decisions and acceptance`)
- `docs/auth.md` (new): the shell metrics table; the settled-vs-board table of §4; the failure mapping table; the announcement and reveal mechanisms; the state-ownership rule (§5.5, §5.8); the port map (Swift test → Kotlin test); the capture list and the reviewer's board comparison (each difference fixed or recorded with its board name); what the emulator shows for autofill and feedback (OQ-5a-8); and the open questions below with their defaults.
- `plan.md`: in Phase 5a replace "The `LockoutWait` countdown." with "`LockoutWait` ported to `:core:model` (Sign in has no countdown; iOS shows the static locked banner)". Append OQ-5a-1 to OQ-5a-10 to "Open questions for the user" with the next free `OQ-A` numbers.

## 8. Acceptance criteria

1. `make quick` passes (< 60 s warm). `make check` passes with `pixel` booted (the orchestrator runs it if your sandbox cannot).
2. `make targeted ONLY="core:model:LockoutWaitTests feature:auth:AuthFailureMappingTests feature:auth:AuthFormModelTests feature:auth:AuthBannerKindTests feature:auth:AuthLayoutTests feature:auth:AuthNavigationTests feature:auth:SignInModelTests app:SignInModelRoutingTests app:AuthServicesTest"` is green; every JVM test < 1 s (`build/test-results/*/timings.json`).
3. Every Robolectric class of Tasks 2, 6, 7 and 9 passes, each test < 5 s; `SignInGuardsTest` has exactly 17 cases.
4. `make captures ONLY=AuthSignIn,AuthSignInError,AuthStates,auth/` writes the 14 PNGs of Task 9 into `build/captures/`. The Opus reviewer compares them side by side with the boards rendered at 375 × 812 (both themes, every `error` prop); every difference is fixed or recorded in `docs/auth.md`. Expected accepted differences only: no dots, empty fields, 48 dp targets (link and footer rows a few dp taller), the underline offset, and wrapping one word earlier.
5. On `pixel`: `SignInJourneyTest` and `SignInKeyboardTest` pass, each < 10 s.
6. A debug run of the journey is StrictMode-clean: no disk or network on main (device identity, device name and the session write run on IO).
7. `rg -n "rememberSaveable|rememberTextFieldState|SavedStateHandle" feature/auth` finds nothing that holds an email, a password or a challenge; Konsist passes; `rg -n "Log\.|println" feature/auth/src/main` finds nothing.
8. `git -C /Users/rickgout/Sites/chronoflow-ios status --short`, and the same for `chronoflow-new` and `chronoflow-mobile`, print nothing. Every commit follows Conventional Commits with no trailer. The plan boxes "Auth shell", "Fields", "SignInViewModel", "Passkey entry" and "Tests" are ticked, and `plan.md` carries the corrected LockoutWait line and the new OQs.

Performance: no budget row applies to Sign in. The screen must not recompose per IME animation frame (insets only through modifiers, §5.1; the reveal reads insets only inside `snapshotFlow`), and a keystroke must not recompose the card beyond the edited field.

## 9. Review checklist (Opus reviewer)

- [ ] **Ports are exact:** `AuthFailureMapping` (credentials early return, email-taken skip, proof-field rule, final server fallback), the `AuthFieldCopy` whitelist (obsolete key, non-integer, negative and string arguments), `AuthFormModel.begin/edit/receive/takeAnnouncement` (sorted fields; an empty result is not marked taken), the `SignInModel.submitPassword` order, `AuthNavigation.route`, `AuthLayout.content/reveal` and `LockoutWait` match the Swift line for line, with Swift test names kept.
- [ ] **No client validation**, no offline pre-check, submit never disabled for offline or throttled, no countdown on Sign in.
- [ ] **Password cleared after every attempt** (success, failure, cancel, session failure); email and banner kept; autofill committed **before** the clear and only on success.
- [ ] **Real-edit rule:** the model's own `clearText()` never clears feedback; re-entering the same text never clears it; edits while busy are impossible (fields disabled) and ignored by the model.
- [ ] **Secrets:** no password or challenge in `rememberSaveable`, `SavedStateHandle`, a route, a `Bundle`, a log, an exception message or a `toString()`. `SignInUiState` holds no password. The password `TextFieldState` is `remember`ed in the screen.
- [ ] **State ownership:** `SignInModel` is process-scoped in `AuthServices`; the ViewModel only adapts it and holds the request job; `leave()` runs on dispose; leaving auth clears form and challenge and keeps the email.
- [ ] **Routing:** `acceptedSession` runs in `appScope`, is joined, and uses 4e's `routeAcceptedSession` (no duplicated gate logic); two-factor never accepts a session; back goes to Welcome with no intro; no placeholder screen was invented.
- [ ] **Passkey:** button and divider are not composed while the capability is off (absent from the tree, not hidden); `submitPasskey` is a no-op then; cancel is silent; no system UI opens.
- [ ] **Geometry** comes only from `AuthTokens`, `CfTypeScale`, `AuthMetrics` (`@BoardMetrics`) and `LocalSafeInsets`; no `WindowInsets.safeDrawing` in the feature; IME through `imePadding` with consumption; the 560 dp column only from a 600 dp window; no size written to state from `onGloballyPositioned`/`onSizeChanged`.
- [ ] **Card** has no border, radius 26, padding 20, gap 14, soft shadow; the banner is first in the card; offline is title-only with the alert glyph.
- [ ] **Announcements:** exactly one live-region announcement per failed submit, repeated for an identical failure (U+200B toggle), never doubled by `CfBanner`; the announcer node is persistent and 48 × 48 dp (never zero-size, never recreated per revision); no `announceForAccessibility`.
- [ ] **Committed session routes after a cancel:** `SignInModel.accept` wraps `session.accept` … `accepted` in `withContext(NonCancellable)`; `committedSessionUsesStoredGateWithAndWithoutCancellation` passes with `cancelAfterCommit = true`.
- [ ] **Keyboard test proves a real software keyboard** (IME inset ≥ 100 dp), not a minimized one.
- [ ] **Strings:** only generated resources; no literal reaches `Text`; no new string; names from `names.lock.json`.
- [ ] **Tests at the cheapest tier:** model logic on the JVM, layout in Robolectric, only the journey and the IME check on the emulator; no `Thread.sleep`, no tree traversals, exact tag queries; the guard set is 17 cases, not a full matrix.
- [ ] **Typical mistakes:** `catch (e: Exception)` swallowing `CancellationException`; clearing feedback from `snapshotFlow { state.text }` (fires on programmatic clears); `rememberTextFieldState()` for the password; `weight` under `verticalScroll` without a min height; `MaterialTheme.colorScheme` or `MaterialTheme.typography` in the feature; an M3 `OutlinedTextField` instead of the designed field; the `CfBanner` live region left on next to the announcer; a `traversalIndex` hack; a submit pinned above the keyboard; `exclude(ime)` combined with `imePadding` consumption (double subtraction).

## 10. Open questions (proceed on the default; append each to `plan.md` with the next free `OQ-A` number)

| # | Question | Default |
|---|---|---|
| OQ-5a-1 | The plan task says "The `LockoutWait` countdown" for Sign in, but iOS Sign in shows only the static "Too many attempts." banner; `LockoutWait` is used by Settings and the wizard. | iOS parity: port `LockoutWait` with its test; no countdown and no disabled submit on Sign in; keep `retryAfterSeconds` in the form state. Fix the plan wording. |
| OQ-5a-2 | Plan R20 puts `LockoutWait` in `:feature:auth`, but 9f (`:feature:wizard`) and 10c (`:feature:settings`) need it, and features must not depend on each other. | `:core:model`, package `net.chronoflow.core.model.auth`, beside `AuthenticationMethod`. |
| OQ-5a-3 | iOS moves VoiceOver focus to a banner-only failure; Compose has no public API to move TalkBack focus. | The live-region announcement (title + body) replaces it; the banner is brought into view. |
| OQ-5a-4 | Visible control heights: the board's 40 dp buttons and 36 dp inputs vs iOS 44 pt. | Buttons keep the board's 40 dp visual (`Lg`) with a 48 dp target; fields use Phase 2's ≥ 48 dp box; link rows 48 dp. |
| OQ-5a-5 | 4e's `RootContent` has no ViewModel entry decorator, so every `viewModel()` in a route is activity-scoped. 5c's brief expects `onCleared()` on leaving a route. | 5a keeps all state in the process-scoped `SignInModel` and cancels through `leave()`, so it does not depend on scoping. Recommend that 5c (or 6a) add `rememberViewModelStoreNavEntryDecorator()` to `RootContent` when it first needs entry-scoped ViewModels. |
| OQ-5a-6 | `CfButton` requires a `loadingDescription` while loading; iOS has no loading copy. | Pass the button's own label; add no copy. |
| OQ-5a-7 | Auth screens are single-entry root routes (4e), so back from Sign in to Welcome uses `BackHandler` and shows no predictive cross-screen preview. 5d's brief assumes a `[Welcome, SignIn, SignUp]` stack. | Accept for 5a. 5b–5d use the same explicit `SignInRouter` navigation (back from Forgot, Sign up and Two-factor → `navigate(Route.SignIn)`). A real auth back stack with predictive back is a later refactor with its own OQ. |
| OQ-5a-8 | Autofill may bypass `InputTransformation`, so feedback may not clear when a provider fills a field. | Accept; feedback clears on the next keystroke. Record what the emulator shows in `docs/auth.md`. |
| OQ-5a-9 | No board for the offline banner or for field errors on Sign in. | Offline: the user-approved RN copy on the AuthStates surface, title only. Field errors: the bundle `Field` error row (AuthStates: "Field problems stay under the field"). Both are captured under `auth/` and labelled as adaptations. |
| OQ-5a-10 | iOS posts a one-shot VoiceOver announcement that is not a focus stop. Compose has no equivalent: a live region announces only on a content change of a persistent, non-empty node, so the announcer is a real node TalkBack can also reach. | The brand row's end 48 dp box carries the live region; after a failure it is a TalkBack stop between brand and heading that reads the last announcement. Record in `docs/auth.md` what TalkBack does on the `pixel` emulator (manual check, not a test). |

## Appendix: hand-over names for 5b–5d

Later briefs were written before this one. Map their names to these:

| Later brief says | 5a builds |
|---|---|
| `AuthScaffold` (5d), "the `AuthScreen` port" (5b, 5c) | `AuthScreen` |
| `AuthChallengeHolder` (5d) | `AuthNavigation.challengeID` |
| `SignInModel.password = ""` (5b) | `SecretField.clear()` in `finally`; the model holds no password |
| `PasskeyCapability.Shipped`, `SERVER_READY`, `PasskeyFailure` (5b) | built here in `…/auth/model/passkey/`; 5b adds `PasskeySignIn` and passes it as `passkey` |
| `loadingLabel` (5b) | `CfButton.loadingDescription` = the button's own label |
| "stack `[Welcome, SignIn]`" (5d) | single-entry root routes; `SignInRouter` navigation (OQ-5a-7) |
| `FakeSessionController`, `FakeSecretField` (5d) | `feature/auth/src/test/…/testing/` |
