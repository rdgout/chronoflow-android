# Phase 5b: Two-factor and passkey sign-in

- **Branch:** `feat/phase-5b-two-factor-passkeys`. Start it from the stack top once **5a** is in it; 5a brings 4e, 4c, 4b, 3c, 2, 0c, 0d and 0e. If 5a is missing, stop and report it.
- **Parallel work:** 5c and 5d run in their own worktrees (wave 9). Do not touch their files: `AuthLink`, Forgot, Reset, Sign up, Verify and `docs/app-links.md`. Both also edit `AuthServices`, `RootContent` and `ScreenSessionFixture`; keep your edits to those files small and self-contained so the rebase stays trivial.
- **Stack check (2026-11-18, against 5f756ad).** Phases now run one at a time in a single lane: no worktree, and 5c/5d have not started, so the parallel-work rule above only means "keep `AuthServices`, `RootContent` and `ScreenSessionFixture` edits small". 5a's hand-over names (5a brief, Appendix) are all landed: `AuthScreen`, `AuthNavigation.challengeID`, `SecretField`, `PasskeyCapability` and `PasskeyFailure` under `feature/auth/…/auth/model/passkey/`, `SignInRouter`, `AuthServices` (`app/…/app/auth/AuthServices.kt`), and the test fakes `FakeSessionController`/`FakeSecretField` in `feature/auth/src/test/…/auth/testing/AuthTestFixtures.kt`. The 0d contracts (`TwoFactorInput`, `ApiPasskey*` in `core/model/…/model/api/{AuthContracts,PasskeyContracts}.kt`), 4b's `completeTwoFactor`/`passkeyOptions`/`passkeyLogin`, the `auth/passkey-options.json` fixture and `android_two_factor_intro_b1` (en + nl) resolve. Mind 5a's OQ-A182–OQ-A192 (notably OQ-A186: no per-route ViewModel scope; OQ-A188: single-entry root routes with `BackHandler`).
- **Plan entry:** `plan.md` "### Phase 5b: Two-factor and passkey sign-in" (l.1750–1784).
  - Decisions **A4** (manual DI), **A8** (navigation, process death), **A15** (passkeys), **A23** (secrets).
  - Spec sections: "Android spec: navigation and routes" (l.400), "forms and IME" (l.407), "seeds and test hooks" (l.465), "Android-only copy awaiting approval" (l.481).
  - Risks 3 (l.544), OQ-A12 and OQ-A21.
- **Tests:** every test follows `AGENTS.md` "Tests". Read that section before you write a test. Tier annotations are 0e's (`net.chronoflow.core.testing.tiers`): Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`. Plain JVM tests carry no annotation.
- **Tags:**
  - **[F]** is a fact verified in source; the path is given.
  - **[R]** is a rule this brief sets for Android.
- **Path shorthands:**

  | Shorthand | Means |
  |---|---|
  | `I/` | `/Users/rickgout/Sites/chronoflow-ios`, read at `f391a1fa` with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. Never write there. |
  | `Kit/` | `I/Packages/ChronoFlowKit/Sources` |
  | `KitT/` | `I/Packages/ChronoFlowKit/Tests` |
  | `W/` | `/Users/rickgout/Sites/chronoflow-new`, read at `e36368ec` with `git -C … show e36368ec:<path>` |
  | `DC/` | `_design/canvas/` |
  | `…/auth/` | `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/` |
  | `…/authT/` | `feature/auth/src/test/kotlin/net/chronoflow/feature/auth/` |
  | `…/model/` | `core/model/src/main/kotlin/net/chronoflow/core/model/` |
  | `…/data/` | `core/data/src/main/kotlin/net/chronoflow/core/data/` |
  | `…/app/` | `app/src/main/kotlin/net/chronoflow/app/` |
  | `…/appT/` | `app/src/test/kotlin/net/chronoflow/app/` |
  | `…/debug/` | `app/src/debug/kotlin/net/chronoflow/debug/` |

- **Where code and brief differ:** the code that 5a, 4e and 4c actually landed wins over the names below. `grep` every name of §2.1 in the merged code before Task 1. Use the real names, never rename earlier code, and list the mapping in the body of your first commit.

- **Implementation stack check (2026-11-18).** Repeated §2.1 and §6 against the checkout; all prerequisite symbols exist. Mechanical mappings: `RootContent` is a generic, transition-free route host; screen entries belong in `app/…/app/CfRoot.kt`. `AppGraphKey` belongs to `:app`, so Task 6 adds `app/…/app/auth/TwoFactorViewModelFactory.kt`, mirroring `SignInViewModelFactory`, rather than a graph-reading factory in the feature. `RootContent` keys only GuestWizard, Terms and Walkthrough by revision; auth retains route identity. `AuthScreen` takes `announcementRevision` and `takeAnnouncement`; its brand row hosts `AuthAnnouncer`, so Two-factor supplies those callbacks rather than composing another announcer. `AuthFailureFeedback` already accepts the ordered field/requester list. `RecordingRoutes.acceptedSession` is the acceptance callback; two-factor calls are recorded as `actions.count { it == "twoFactor" }`, not a numeric property. `ApiJson.request` and `ApiJson.response` are the serializer instances. `ApiPasskeyLoginInput` uses `deviceID`; `DataSource`, `DataFailure` and `FieldMessage` are in `net.chronoflow.core.model`, and 4b's implementation is `core/network/…/network/ApiDataSource.kt`. The shared session fakes live in `core/sync/src/testFixtures/…/sync/session/SessionFakes.kt`. All §6 files other than the existing capability/failure types are new. Preserve these landed names and behaviour.

---

## 1. Goal

**Two-factor.** When `POST auth/login` answers `{kind:"two-factor", challengeId}`, 5a's `SignInModel.submitPassword` stores the challenge in `navigation` (`AuthNavigation.challengeID`) and calls `onTwoFactor()`, which navigates to `Route.TwoFactor`.
- This phase builds the screen that route shows, and the iOS `TwoFactorModel` behind it. Like every auth screen it is a **single-entry root route** (4e `RootContent`, 5a §5.9, OQ-5a-7): there is no back stack; back navigates to `Route.SignIn`.
- On it, the person:
  - types the 6-digit authenticator code (number keyboard, one-time-code autofill), or
  - switches to a recovery code (ASCII keyboard).
- `POST auth/two-factor` returns a session. The session goes through **5a's own `SignInModel.accept`** (method `TwoFactor`), exactly as iOS calls `SignInModel.accept(result, method: .twoFactor)`, and then to the session gate (verify → terms → walkthrough → app).
- Errors and throttling follow the iOS mapping exactly (5a's `AuthFailureMapping`, context `TwoFactor`).
- The challenge ID and the code live only in memory, in the process-scoped `SignInModel`.

**Passkeys.** Passkey sign-in is built completely: options → Credential Manager assertion → `POST auth/passkey/login` → 5a's `accept` (method `Passkey`).
- `PasskeyCapability.SERVER_READY` stays **false** in every shipped build until server items S1–S3 are live. Until then no passkey control is composed (5a already does this).
- A user cancel is silent. Every other failure shows the existing designed Server banner.

**Done means:**
- These iOS suites pass as JVM tests under their Swift names (rows per the 4c rule, §7 "Test naming"):
  - `TwoFactorModelTests`: 11 of the 12 Swift tests in `:feature:auth`, and `onlySessionAcceptancePersistsATokenAndNeverTheChallengeOrProof` in `:app` (it needs 4e's real `SessionController`), the same split 5a used for `SignInModelTests`;
  - `PasskeySignInTests` (5 Swift tests; the 3-row one becomes 3 methods);
  - `PasskeyEncodingTests` (2);
  - the `twoFactor` iteration of `committedSessionUsesStoredGateWithAndWithoutCancellation`, which 5a deferred to this phase, added to 5a's `SignInModelRoutingTests`.
- The Two-factor screen matches the `AuthTwoFactor` board and the "Two-factor code" banners of `AuthStates`:
  - board: screen `twofactor`, code and recovery modes, errors none/attempts/server;
  - in light and dark, and in en and nl;
  - guarded at `small`, `pixel8`, `foldInner`, `landscapeSideNav` and `largeFont`.
- On `pixel`, three emulator tests pass: one journey (seeded Two-factor screen → code → app root) and two seeded keyboard checks (code variant and recovery variant).
- The passkey request JSON, the response → `ApiPasskeyAssertion` conversion and the Credential Manager error mapping are proven on the JVM, using a fake `CredentialClient` and the `auth/passkey-options.json` fixture.
- With the capability false:
  - `sign-in.passkey` and `sign-in.divider` do not exist in the semantics tree (5a's test stays green);
  - `submitPasskey` is a no-op (5a's test stays green);
  - a release unit test proves the shipped capability is false.

## 2. Inputs to read (in this order)

1. **`plan.md`.** Read:
   - A4, A8, A15 (passkeys) and A23 (secrets);
   - "Android spec: forms and IME", "navigation and routes", "seeds and test hooks" and "Android-only copy awaiting approval";
   - Risks 3, OQ-A12 and OQ-A21;
   - Phase 5a (what you build on), 5b (this phase), and 10c/10d (who reuses your passkey types).
2. **`AGENTS.md`:** "Working rules" (secrets), "Tests" and "Traps" (`CancellationException`, announcements, measured sizes).
3. **The 5a brief** `docs/phases/phase-5a-sign-in.md`, read in full, especially §5.1 (`AuthScreen`), §5.4 (banner, announcer, failure feedback), §5.5 (fields, `UserEditReporter`, keyboard reveal), §5.6 (`AuthFailureMapping`, `AuthFormModel`), §5.7 (`SignInModel`, `AuthNavigation`, passkey seam, `SignInRouter`), §5.8 (`AuthServices`, `SignInViewModel`), §5.9 (root-route navigation, the auth-exit observer), §6, Tasks 5–10, OQ-5a-5 and OQ-5a-7. Then read the code 5a landed under `…/auth/` and `…/app/auth/`. This phase extends it.
4. **Research:**
   - `docs/research/05-launch-auth.md`:
     - §8.4 (failure → banner/field table);
     - §9.2 (passkey sign-in) and §9.3 (two-factor);
     - §9.9 (copy, en/nl);
     - §15.5 (back table: "Two-factor → Sign in; discard challenge and code"; "While a request runs: back still works; the in-flight request is cancelled");
     - §15.7 (Credential Manager) and §15.8 (keyboards l.866–870, autofill table);
     - §17, OQs A12–A15.
   - `docs/research/03-api-contract.md`:
     - §3 (error envelope) and §4 (limits);
     - §7 (two-factor);
     - §9 (passkeys), including §9.4 "Android-specific gaps".
   - `docs/research/14-gaps.md` §12, S1–S3.
5. **Boards** (read-only):
   - **`DC/AuthTwoFactor.dc.html`.** Props: `screen` (default `twofactor`), `error=none|attempts|server`, `theme=light|dark`, plus its `toggleRecovery` handler (code ⇄ recovery). Only `screen=twofactor` is this phase; the other `screen` values belong to 5c and 5d.
   - **`DC/AuthStates.dc.html`.** Read "Placement" (the banner comes first inside the card and clears on edit) and the "Two-factor code" section (l.57: the attempts and server banners).
   - **`DC/AuthSignIn.dc.html`.** Only to recall the passkey button and the "or with email" divider. 5a built both, not composed while the capability is off.
6. **iOS references** (`I/` at `f391a1fa`):
   - **Plan sections:**
     - `plan.md:1210` "Phase 10b: Two-factor code/recovery and auth acceptance (`feat/phase-10b-two-factor`)", including fix round 4 (the rejected-code copy and the catalogued `xxxx-xxxx` placeholder);
     - `plan.md:496–497` (busy lock, no auto-submit at six digits, recovery code → go) and `plan.md:628` (the `xxxx-xxxx` placeholder is never a prefilled value);
     - `plan.md:1176` "Phase 10a" (its third task: native passkey sign-in; implementation note l.1188);
     - `plan.md:444–445` and `453` (the passkey rows and "Passkey contract source");
     - `plan.md:2376` "Open questions for the user" (OQ 16, 20, 30 and 32);
     - `plan.md:2055` "Passkey management addendum (2026-11-12, user request: build it now though it cannot be verified yet)", which explains why 10d reuses your types.
   - **Port line by line:**
     - `Kit/Auth/TwoFactorModel.swift` (86 lines);
     - `Kit/Auth/PasskeySignIn.swift` (`Base64URL`, `PasskeyAssertionProviding`, `DeterministicAssertionProvider`, `PasskeySignIn`; `PasskeyFailure` is already 5a's);
     - `Kit/Auth/SignInModel.swift:115–120` (the two-factor branch that creates the `TwoFactorModel`) and `:150–156` (`clearSecrets` cancels it).
   - **Behaviour twins:**
     - `Kit/Auth/TwoFactorView.swift` (UI rules);
     - `Kit/Auth/AuthNavigation.swift` (`twoFactor`, `discardChallenge`, `cancelTwoFactor`);
     - `Kit/DesignSystem/AuthControls.swift` (`AuthButton` default kind `.primary`; `AuthField` shows the hint only without an error);
     - `Kit/Auth/SystemPasskeyAssertionProvider.swift` (busy guard, cancel → `canceled`, everything else → `unavailable`).
   - **Tests to port:**
     - `KitT/AuthTests/TwoFactorModelTests.swift` (12 tests, l.8–289; helpers `challenged`, `twoFactorSource`, `TwoFactorClock`, `TwoFactorGate`, `TwoFactorCredentials`, `TwoFactorAccounts`, l.292–367);
     - `KitT/AuthTests/PasskeySignInTests.swift` (suites `PasskeyEncodingTests` and `PasskeySignInTests`, helpers `testAssertion` and `passkeySource`);
     - `KitT/AuthTests/SignInModelTests.swift:164–229` (the `"twoFactor"` iteration of `committedSessionUsesStoredGateWithAndWithoutCancellation`);
     - `I/AppTests/TwoFactorControlTests.swift` (the screen-level behaviours §7 Task 5 ports under their Swift names).
   - **Seeds and wiring:**
     - `I/App/Debug/AuthUITestConfiguration.swift:27–32` (`prepareChallenge`: types the two-factor email and password into the real `SignInModel` and submits);
     - `I/App/SceneDelegate.swift:508–515` (audit screen `two-factor` → navigate to Sign in, then `prepareChallenge`);
     - `I/App/Session/AuthSessionServices.swift:94–98` (with the mock source, the app uses a `DeterministicAssertionProvider` that throws `unavailable`).
7. **Web** (`W/` at `e36368ec`), only to confirm server behaviour:
   - `routes/api.php:49–51`;
   - `app/Http/Requests/Api/V1/TwoFactorRequest.php`;
   - `app/Actions/Api/MobileTwoFactorChallenges.php`;
   - `app/Http/Requests/Api/V1/LoginWithPasskeyRequest.php`;
   - `app/Providers/AppServiceProvider.php:128–146` (limiters: passkey l.128–139, two-factor l.140–146).

### 2.1 What earlier phases give you — verify each before Task 1

If an item is missing, stop and report it.

- **0c.** The `auth` string table in en and nl, and `CopyIndex.find(CopyTable.Auth, key)`. Resource names are in `tools/l10n/names.lock.json`; never guess a slug. `xxxx-xxxx` is `shouldTranslate: false` and is emitted in `values/` only (0c: `auth_xxxx_xxxx` falls back to `xxxx-xxxx` in nl). `android_two_factor_intro_b1` is in `strings_android.xml`.
- **0d** (`net.chronoflow.core.model.api`):
  - `TwoFactorInput(challengeId, proof: Proof)` with `Proof.Code(value)` and `Proof.RecoveryCode(value)`;
  - `SignInResult.TwoFactor(challengeId)` and `SessionResult`;
  - `ApiPasskeyChallenge(challengeId, options)` and `ApiPasskeyRequestOptions(challenge, rpId, timeout: Int, userVerification: UserVerification, allowCredentials: List<EmptyCredential>)`;
  - `ApiPasskeyLoginInput` (wire keys `challengeId`, `credential`, `device_id`, `device_name`, `draft_token?`, `guest_secret?`);
  - `ApiPasskeyAssertion(id, rawId, type: CredentialType, response)` and `ApiPasskeyAssertionResponse(clientDataJSON, authenticatorData, signature, userHandle: String? = null)`;
  - `UserVerification.Required` and `CredentialType.PublicKey` (strict enums);
  - `ApiJson`, `DataFailure` and `FieldMessage`;
  - `toString()` redaction of `challengeId` and of the proof value.
  - The fixture `auth/passkey-options.json` is in `core/testing/src/main/resources/fixtures/auth/` (challenge = 43 × `A`, `challengeId` = 64 × `a`).
- **4b.** `DataSource.completeTwoFactor(TwoFactorInput): SessionResult`, `passkeyOptions(): ApiPasskeyChallenge` and `passkeyLogin(ApiPasskeyLoginInput): SessionResult`.
  - `passkeyOptions()` checks: `challengeId.length == 64`, `rpId == "app.chronoflow.net"`, `timeout == 60_000`, a 43-character `[A-Za-z0-9_-]` challenge that decodes to 32 bytes. Anything else is `DataFailure.Server(200)`.
  - These are public routes: no bearer, 15 s timeout.
- **4c.** `:core:data-mock`, package `net.chronoflow.core.datamock`:
  - the constructor `MockDataSource(store, seed, offline, mutationsOffline, remoteTripUpdate, now: () -> Instant, delay: () -> Double, sleep: suspend (Double) -> Unit, id, bearer, assertionProvider: AssertionProvider, debugAppSeed, zone, io)`;
  - `typealias AssertionProvider = (ApiPasskeyAssertion, ApiPasskeyChallenge) -> String?`;
  - `failNext(MockMethod, DataFailure)` and `MockMethod.CompleteTwoFactor` / `PasskeyLogin`;
  - `MockServerState`, `MemoryMockStateStore` and `MockServerAccount(snapshot, password, twoFactorCode: String? = null, recoveryCodes: Set<String> = emptySet(), walkthroughSeen = false, …)`;
  - `MockScenarioValues` (`twoFactorEmail`, `password`, `twoFactorCode`, `recoveryCode`; debug and perf only);
  - two-factor rules: 5-minute TTL; the challenge is deleted on the 5th failure; blank proof → both field messages.
  - Feature test sources depend on it through `testImplementation(project(":core:data-mock"))` (5a already added it). `MockClock` stays in data-mock test sources (OQ-4c-14), so do not import it; Task 4 gives a local clock.
- **4e:**
  - `SessionController.accept(result)`, `SessionFailure` (`CredentialsUnavailable`, `DatabaseUnavailable`, `CredentialWrite`, `InvalidResult`, `Superseded`) and the `:core:sync` test fixtures `MemorySessionStore` and `RecordingAccountStore` (`testImplementation(testFixtures(project(":core:sync")))`);
  - `Route.TwoFactor`, a `data object` with no arguments;
  - `RootNavigator` (`state: StateFlow<RootState(route, revision)>`, `navigate(to)`), `RootContent` (a single-entry `NavDisplay` stack `[route]`, each entry in `key(revision)`, **no ViewModel entry decorator**), and `RouteProbe` (debug);
  - `TestSeed`, `TestSeeds.apply`, `DebugSeedRouting` and `ScreenSessionFixture`. `CF_AUTH_TEST_AUDIT_SCREEN` is parsed; 4e routes only `sign-in` and `forgot` and leaves `two-factor` to you (4e §3 and OQ-4e-11);
  - `StoredDataReadiness.routeAcceptedSession`.
- **5a** (package layout `…/auth/model/`, `…/auth/shell/`, `…/auth/signin/`; fakes in `…/authT/testing/`):
  - `model/`: `AuthFormModel` (`state: StateFlow<AuthFormState>`, `Action.Verify`, `Action.Passkey`, `begin`, `finish`, `edit`, `clear`, `receive`, `takeAnnouncement(resolve)`, `showFields`), `AuthFormContext.TwoFactor`, `AuthFailureMapping` (both two-factor rules, tested in `AuthFailureMappingTests`), `AuthBannerKind`, `AuthFieldCopy` and `AuthText`;
  - `model/AuthNavigation` (`challengeID`, `twoFactor(id): Boolean` that refuses a blank id, `discardChallenge()`, `cancelTwoFactor()`, `reset()`, `companion fun route(account)`, a redacting `toString()`);
  - `model/SessionAcceptor` and `model/SignInModel(source, session, deviceIdentity, deviceName: suspend () -> String, val passkeyCapability, passkey: suspend () -> SessionResult, accepted: suspend (SessionResult, AuthenticationMethod) -> Unit, onTwoFactor)` with `email`, `form`, `navigation`, `sessionFailure`, `submitPassword(SecretField, commitAutofill)`, `submitPasskey(SecretField)`, `clearSecrets()` and a private `accept(result, method, commitAutofill, password)` whose commit-and-route part runs in `NonCancellable`;
  - `model/passkey/PasskeyCapability` (`@JvmInline value class PasskeyCapability(val enabled: Boolean)`, `SERVER_READY = false`, `Shipped`) and `model/passkey/PasskeyFailure` (`Canceled`, `InvalidEncoding`, `Unavailable`, `Busy`, `AlreadyRegistered`, already all five);
  - `:core:model` `AuthenticationMethod` (`SignUp`, `Password`, `Passkey`, `TwoFactor`);
  - `shell/`: `AuthScreen(scrollTag, title, description, onBack, showsBack = true, compact = false, enabled = true, card, footer = null)` (sets the pane title), `AuthBanner(kind, context)`, `AuthAnnouncer` (tag `auth.announcement`), `AuthFailureFeedback`, `AuthKeyboardReveal`, `UserEditReporter`, `SecretField`/`TextFieldSecret`;
  - `signin/`: `SignInRouter` (`back`, `forgot`, `signUp`, `guest`, `twoFactor`, `suspend acceptedSession(result, method)`), `SignInViewModel` (reads `model.passkeyCapability.enabled`), `SignInScreen`; the hidden `sign-in.passkey` button and `sign-in.divider`;
  - `:app` `AuthServices` (lazy `AppGraph.auth`: `signIn: SignInModel`, `router: SignInRouter`, memoised `deviceName`, and the auth-exit observer that calls `signIn.clearSecrets()` when the route leaves the auth routes);
  - test fakes `FakeSessionController : SessionAcceptor`, `FakeSecretField` and `RecordingRoutes` (`accepted`, `twoFactor`), and the 5a test source `authTestSource(twoFactor)`;
  - `app/src/test/…/auth/SignInModelRoutingTests` and `AuthServicesTest`;
  - the IME-visible wait used by `SignInKeyboardTest`.

### 2.2 API endpoints

All three are public: no bearer, no cookies, a 15 s timeout. 4b built them; this phase only calls them. [F `03 §5.1`, `§7`, `§9`; `W/routes/api.php:49–51`]

**`POST /v1/auth/two-factor`** (throttle `api-two-factor`)
- **Request:** `{"challengeId":"<64 chars>","code":"123456"}` **or** `{"challengeId":"…","recovery_code":"…"}`, never both (`required_without` + `prohibits`, `W/…/TwoFactorRequest.php:15–17`).
- **Success:** 200 `ApiSession {kind:"session", token, account, tripId}`.
- **422 on `code`:**
  - "Enter the 6-digit code from your authenticator app." when the code is missing, not a string or does not match `^[0-9]{6}$`;
  - "The authentication code was not accepted. Try again." when the code is wrong, or the challenge has expired or is exhausted.
- **422 on `recovery_code`:**
  - "Enter one of the recovery codes you saved when you set up two-factor authentication.";
  - "This recovery code was not accepted. Try another saved code."
- An empty proof gives **both** `code` and `recovery_code` messages (`required_without` on each).
- **422 on `challengeId`:** missing, or longer than 255.
- **429 `throttled`:** 5 per minute plus 10 per hour, keyed by the challenge's user (or the IP when the challenge is unknown) (`AppServiceProvider.php:140–146`).
- **Other failures:** 5xx and offline.
- **Challenge rules** (`MobileTwoFactorChallenges.php`): 5-minute TTL; deleted on the 5th failure or on success; a password, email or two-factor change in between makes it read as expired (fingerprint, l.61).

**`POST /v1/auth/passkey/options`** (throttle `api-passkey-options`)
- **Request:** no body.
- **Success:** 200 `{"challengeId":"<64 [A-Za-z0-9]>","options":{"challenge":"<43 chars = 32 bytes, unpadded base64url>","rpId":"app.chronoflow.net","timeout":60000,"userVerification":"required","allowCredentials":[]}}`.
- **Challenge rules:** single use, valid for 120 s.
- **Failures:**
  - 429 (10 per minute per IP);
  - 5xx and offline;
  - a shape that fails the 4b checks → `Server(200)`.

**`POST /v1/auth/passkey/login`** (throttle `api-passkey-login`)
- **Request:** `{"challengeId":"…","credential":{"id":"<b64url>","rawId":"<b64url>","type":"public-key","response":{"clientDataJSON":"…","authenticatorData":"…","signature":"…","userHandle":"…"?}},"device_id":"<uuid>","device_name":"…","draft_token"?,"guest_secret"?}`.
- **Success:** 200 `ApiSession`. There is never a two-factor step, not even for 2FA accounts; unverified accounts also get a session.
- **Failures:**
  - 422 `credential`: "We couldn't sign you in with that passkey. Please try again.";
  - 422 `challengeId`: "The passkey request expired. Please try again." The challenge is consumed even on failure.
  - 429: 5 per minute per credential + IP, 20 per minute per IP, and 20 per hour per credential counting only 422 responses (`AppServiceProvider.php:129–139`);
  - 5xx and offline.
- **Field rules:** `challengeId` ≤ 64; every credential string ≤ 8,192 characters; `type` must be `public-key`; `id` and `clientDataJSON` must be unpadded base64url; `rawId` must decode to the same bytes as `id` (`W/…/LoginWithPasskeyRequest.php:27–40`, `:111`).

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| Settings two-factor setup, management and recovery codes; `FLAG_SECURE`; clipboard; boards `Set2FA*` and `PasswordPrompt2FAOn`; the Android-only intro b1 (`android_two_factor_intro_b1`) | 10c |
| Passkey management and registration (`CreatePublicKeyCredentialRequest`), boards `SetPasskeys*` | 10d |
| Guest claim fields (`draft_token`, `guest_secret`) on passkey login: pass `null`, as 5a does for password login | 11b |
| Forgot, Reset, App Links, `assetlinks.json` for links. 5c's reset entry must also cancel the two-factor model (iOS `openReset`, `SignInModel.swift:82–88`); `clearSecrets()` (Task 4) does that for them. | 5c |
| Sign up, Verify | 5d |
| A real auth back stack with predictive cross-screen back, and a ViewModel entry decorator in `RootContent` | Later refactor (OQ-5a-5, OQ-5a-7). This phase stays on 5a's root-route navigation. |
| Server items S1 (`assetlinks.json` with `get_login_creds`), S2 (`android:apk-key-hash` in the allowed origins) and S3 (management merged) | Server repo. You only document them (Task 11). |
| Real passkey verification on a device. The `pixel` AVD is a `google_apis` image without Play Store. | T3 device, "open: device only" |
| An expired-challenge screen | None: there is no board. The returned field message is shown, as on iOS 10b. |
| The iOS launch flag `-cf.twoFactorServerFailure` | Not ported. JVM and Robolectric tests cover the server banner. |
| iOS interactive keyboard dismissal (`TwoFactorControlTests.testBothModesKeepInteractiveKeyboardDismissalAndVerticalBouncing`) | Not ported (`05` OQ A11: system back closes the IME). |
| Any new copy | None (§5.6) |

## 4. Platform facts this phase relies on

- **[F] `rememberTextFieldState()` is saveable.** It uses `rememberSaveable`, so it writes the text into the saved-instance `Bundle`. The code field must use `remember { TextFieldState(…) }` (AGENTS secrets, `05 §15.8`).
- **[F] `Char.isDigit()` is Unicode-aware.** It accepts Arabic-Indic `٣` and full-width `１`. The iOS filter is ASCII only (`$0.isASCII && $0.isNumber`, `TwoFactorModel.swift:28`). Use `c in '0'..'9'`.
- **[F] Code keyboard (`05` l.866–869).** `KeyboardType.NumberPassword` is wrong for a visible code; use `KeyboardType.Number`. `ImeAction.Go` submits on an explicit press only. The recovery field uses `KeyboardType.Ascii` with `ImeAction.Go`.
- **[F] Root routes (4e §5.8, 5a §5.8–§5.9).** `RootContent` shows `[route]` only, each entry inside `key(revision)`, with no ViewModel entry decorator. A `viewModel()` inside an entry is therefore **activity-scoped**: it survives navigation and recreation and is cleared only when the activity finishes. Per-visit state must not live in a ViewModel; it lives in the process-scoped `SignInModel` (iOS `AuthSessionServices.model`).
- **[F] Credential Manager** (`androidx.credentials:credentials` 1.6.0 and `credentials-play-services-auth` 1.6.0; the 0a catalog has `credentials = "1.6.0"` and both libraries — use 0a's aliases):
  - `CredentialManager.create(context)`.
  - `suspend fun getCredential(context: Context, request: GetCredentialRequest): GetCredentialResponse`. The context must be an **Activity** to show the system UI.
  - The option is `GetPublicKeyCredentialOption(requestJson)`. The result's `credential` is a `PublicKeyCredential` with `authenticationResponseJson`.
  - Failures are subclasses of `GetCredentialException`: `GetCredentialCancellationException`, `NoCredentialException`, `GetCredentialInterruptedException`, `GetCredentialProviderConfigurationException`, `GetCredentialUnsupportedException`, `GetCredentialUnknownException` and `GetPublicKeyCredentialDomException` (built with a `DomError` from `androidx.credentials.exceptions.domerrors`, for example `NotAllowedError()`).
  - The documented R8 rule for the Play-services provider:
    ```
    -if class androidx.credentials.CredentialManager
    -keep class androidx.credentials.playservices.** { *; }
    ```
  - [verify] If the AAR's consumer rules already cover it, keep the file anyway as a guard and note that in the commit.
- **[F] The Android assertion origin** is `android:apk-key-hash:<base64url(sha256(signing cert))>`. The server does not accept it yet (`03 §9.4`). This is why the capability is false.
- **[R] Process death** while the person is in the authenticator app cold-launches to the splash (A8, iOS OQ 16). The challenge is gone and they sign in again. Leaving the app (`onStop`) and recreation (locale, font scale) must **not** cancel the two-factor screen or lose the typed code.

## 5. The spec

### 5.1 Board vs settled values

[F `I/plan.md:479, 496–497, 628, 1210–1232`; `TwoFactorModel.swift:56–59`; `AuthFormModel.swift:74–79`]

| Board (`DC/AuthTwoFactor.dc.html`) | Build instead | Why |
|---|---|---|
| A `pattern-dots` layer on the sky | Plain sky, no dots | iOS "No PatternDots" (`I/plan.md:479`), already in 5a's `AuthScreen` |
| The code field prefilled with `482913`, and the error always drawn (l.55) | The field starts empty and is cleared after every attempt. A field error therefore shows an **empty** field with the error. | `TwoFactorModel.swift:56–59` |
| "That code didn't work. Codes change every 30 seconds…" as the code error | Only for the `code` field when the server key is "The authentication code was not accepted. Try again."; other keys show their own copy | fix round 4, `AuthFormModel.swift:74–79`, 5a's `AuthFailureMapping` |
| Verify is an `<a>` 40 px high (`h-10`); field 36 px; back 44 px | `CfButton` Primary `Lg` (40 dp visual, ≥ 48 dp target), field box ≥ 48 dp, back target 48 dp | 5a OQ-5a-4 |
| Recovery placeholder `xxxx-xxxx` (l.58) | A placeholder only, never a value, from the `xxxx-xxxx` resource (not translated) | fix round 4, `I/plan.md:628` |
| No footer | No footer | `TwoFactorView.swift:60–62` (`EmptyView()`) |

### 5.2 Screen anatomy

On 5a's `AuthScreen` [F `DC/AuthTwoFactor.dc.html` l.50–62, `TwoFactorView.swift:25–87`]:
- `scrollTag = "two-factor.scroll"`, `onBack = vm::back`, `showsBack = true`, `enabled = true` (the back control stays enabled while busy, because iOS disables only the card group, `TwoFactorView.swift:59`), `footer = null`;
- title "Enter your code", description "Open your authenticator app and type the 6-digit code for ChronoFlow.". `AuthScreen` already sets `paneTitle = title` (replaces the iOS `.screenChanged` post) and tags the title `auth.heading` with `heading()`.

The card children, in order (gap 14 from `AuthScreen`'s card):

| # | Element | Test tag | Values |
|---|---|---|---|
| 1 | Banner | `auth.banner` | Only when `form.banner != null`. 5a's `AuthBanner(kind, context = AuthFormContext.TwoFactor)` (§5.4). |
| 2a | Code field (mode `Code`) | `two-factor.code` | `CfTextField`, label "6-digit code"; error under the field; no hint, no placeholder |
| 2b | Recovery field (mode `Recovery`) | `two-factor.recovery_code` | `CfTextField`, label "Recovery code". Hint "Each recovery code works once." (drawn only when there is no error; `CfTextField` already does this). Placeholder `xxxx-xxxx`. |
| 3 | Verify | `two-factor.submit` | "Verify" / "Bevestigen". `CfButton(style = Primary, size = Lg, fillWidth = true)`. `loading = form.pending == Verify`; `loadingDescription` = the same label (5a OQ-5a-6). Spinner, label and width kept. |
| 4 | Mode toggle | `two-factor.toggle` | `CfButton(style = Ghost, size = Lg, fillWidth = true)`. "Use a recovery code instead" in Code mode; "Use your authenticator app" in Recovery mode. |

- **Field identity.** Wrap the field in `key(mode)` so that switching mode builds a new field (iOS `.id(model.mode)`). Its test tag is `"two-factor." + model.field`.
- **Busy.** While `form.busy`, the field (`enabled = false`), Verify and the toggle are disabled (`TwoFactorView.swift:59` `.disabled(model.form.busy)`). `auth.back` and system back stay enabled: back cancels.
- **Announcer.** Place 5a's `AuthAnnouncer(form, resources)` exactly where 5a's Sign in places it.
- **No auto-focus** on appear, as on iOS (OQ-5b-5).
- **`vm.validateChallenge()`** runs once from `LaunchedEffect(Unit)` (iOS `onAppear`), never during composition or in a ViewModel `init`.
- **Back.** `BackHandler { vm.back() }`, always enabled, also while busy (5a §5.8 does the same for Sign in).

### 5.3 Fields, keyboards and autofill [R]

`TwoFactorFieldSpec.forMode(mode)` is pure and tested. It returns:

| | Code | Recovery |
|---|---|---|
| `keyboardType` | `KeyboardType.Number` | `KeyboardType.Ascii` |
| `imeAction` | `ImeAction.Go` → `vm.submit()` (ignored while busy) | `ImeAction.Go` → `vm.submit()` |
| Capitalization / autocorrect | `None` / `false` | `None` / `false` |
| `contentType` | `ContentType.SmsOtpCode` (OQ-5b-2) | none (the board has `autocomplete="off"`, iOS `nil`) |
| `inputTransformation` | `TwoFactorCodeTransformation` | none |
| Label / hint / placeholder | "6-digit code" / none / none | "Recovery code" / "Each recovery code works once." / `xxxx-xxxx` |
| Test tag | `two-factor.code` | `two-factor.recovery_code` |

[F `TwoFactorView.swift:37–45`, `I/AppTests/TwoFactorControlTests.swift:174–197`: code `.numberPad`, `.oneTimeCode`, no capitalisation, no autocorrect; recovery `.default` keyboard, no content type, return key `.go`.]

**`TwoFactorCodeTransformation`** replaces the buffer with `TwoFactorModel.filterCode(buffer)`: keep only `'0'..'9'`, **then** take the first 6. Never put `maxLength(6)` before the filter: a paste of `"a12 ٣b345678"` must give `123456`, and typing `7` into a full `123456` field leaves `123456` (`TwoFactorControlTests.swift:128–144`). 5a Task 4 gave `CfTextField` an `inputTransformation` parameter; pass it there.

**Never auto-submit** when the sixth digit arrives (`I/plan.md:497`).

**Text state** is the Compose form of `TwoFactorView.swift:67–72` (`text` follows `model.code` both ways):
- The screen holds `val field = remember(model, state.mode) { TextFieldState(initialText = state.code) }`. It lives in memory and is never saveable. After recreation the field is rebuilt from the model's code, which is still in memory.
- `LaunchedEffect(field) { snapshotFlow { field.text.toString() }.collect(vm::onCodeEdited) }`.
- `LaunchedEffect(field, state.code) { if (field.text.toString() != state.code) field.setTextAndPlaceCursorAtEnd(state.code) }`. This covers both the clear after every attempt and any other model change (`testBothDisplayedProofsFollowModelEditsAndAttemptClearingWithoutErasingErrors`).
- The model's "value changed" guard (`nextCode != code`) makes the echo of a programmatic change a no-op, so the model's own clear never clears feedback (5a's "typical mistake" about `snapshotFlow` does not apply here because of that guard; prove it in Task 5).

**Keyboard reveal.** Reuse 5a's `AuthKeyboardReveal` with the focused field and `two-factor.submit` (iOS OQ 32). Never feed a measured size into state.

**Toggle.** `focusManager.clearFocus()` (this hides the IME), then `vm.toggleMode()` (`TwoFactorView.swift:56–57`).

**Failure feedback.** Use 5a's `AuthFailureFeedback` with the single field `model.field` and its `FocusRequester`. If 5a hard-coded the order `email`, `password`, generalise it to an ordered list of `(name, FocusRequester)` in the Task 5 commit, keeping Sign in's behaviour and tests unchanged.

### 5.4 Failure mapping in the two-factor context

[F `AuthFormModel.swift:43–90`, `05 §8.4`, `TwoFactorModelTests.swift`]

5a already implements this in `AuthFailureMapping`; this phase only uses it.
- `TwoFactorModel.form = AuthFormModel(displayedFields = setOf("code"), context = AuthFormContext.TwoFactor)` (`TwoFactorModel.swift:10`).
- `toggleMode()` calls `form.showFields(setOf(field))`.

| Failure | Result |
|---|---|
| `Offline` | Banner Offline: "Needs a connection", no body |
| `Throttled(s, _)` | Banner Locked: "Too many attempts." / "Wait a few minutes before you try again." (the two-factor body, **not** the sign-in body). `retryAfterSeconds = s`. **No countdown; Verify stays enabled.** |
| `Validation`: the displayed field with a supported key | Under the field. `code` + "The authentication code was not accepted. Try again." → "That code didn't work. Codes change every 30 seconds, so try the newest one.". Other supported keys are shown verbatim. |
| `Validation` with errors on **both** `code` and `recovery_code` | Only the active field's message. The inactive one is ignored **only** when the active one also has an error. |
| `Validation` on `challengeId`, on the inactive proof alone, with an unknown key or unknown English | Banner Server |
| `SessionFailure` thrown by acceptance | `sessionFailure = it`; banner Server |
| Anything else (`Server`, `Conflict`, any other exception) | Banner Server: "Something went wrong on our side." / "Try again in a moment." |

- An edit clears the banner and that field's error **only when the value changed**.
- After a failure, 5a's `AuthFailureFeedback` presents it:
  - a banner-only failure hides the IME and brings the banner into view;
  - a field failure focuses `two-factor.<field>` (`TwoFactorControlTests.swift:72–105`);
  - exactly one announcement per failed submit (`form.takeAnnouncement`, through `AuthAnnouncer`).

### 5.5 Model, flow, back and the gate

[F `TwoFactorModel.swift`, `SignInModel.swift:115–120, 150–156`, `AuthNavigation.swift:20–32`; R where marked]

**`TwoFactorModel`** (`…/auth/model/TwoFactorModel.kt`) is a line-by-line port of the Swift class. It is pure Kotlin (no `android.*`/`androidx.*` imports), main-thread confined, like 5a's `SignInModel`.

```kotlin
class TwoFactorModel(
    private val source: DataSource,
    private val navigation: AuthNavigation,                  // 5a's, owned by SignInModel (iOS: the same AuthNavigation)
    private val accept: suspend (SessionResult) -> Unit,     // SignInModel's private accept(…, TwoFactor, …); may throw SessionFailure
    private val leave: () -> Unit,                           // [R] the pop that iOS cancelTwoFactor() does on the path; SignInRouter::backToSignIn
)
```

- **State.** `mode` (`Code`), `code` (`""`), `sessionFailure` (`null`), `form`, `field` (`"code"` or `"recovery_code"`), and a private `active = true`.
  - Expose `val state: StateFlow<TwoFactorState>` with `data class TwoFactorState(val mode: Mode, val code: String, val sessionFailure: SessionFailure?)`.
  - `TwoFactorState.toString()` prints `code=***` when the code is not empty.
- **`editCode(value)`.**
  - Return when `!active || form.busy`.
  - `next = if (mode == Code) filterCode(value) else value`.
  - Return when `next == code`.
  - Otherwise `code = next; form.edit(field)`.
- **`toggleMode()`.**
  - Return when `!active || form.busy`.
  - Flip the mode; `code = ""`; `sessionFailure = null`; `form.showFields(setOf(field))`.
- **`validateChallenge()`.** If `navigation.challengeID` is null or blank → `cancel()`.
- **`suspend fun submit()`:**
  1. `validateChallenge()`.
  2. `val challenge = navigation.challengeID`. Return when `!active || challenge == null || !form.begin(Action.Verify)`.
  3. `sessionFailure = null`.
  4. `proof = if (mode == Code) Proof.Code(code) else Proof.RecoveryCode(code)`. The recovery code is sent as typed: no trim, no case change (OQ-5b-8).
  5. `try`:
     - `currentCoroutineContext().ensureActive()`;
     - `result = source.completeTwoFactor(TwoFactorInput(challenge, proof))`;
     - `ensureActive()`;
     - return when `!active || navigation.challengeID != challenge`;
     - `code = ""`; `accept(result)`; `navigation.discardChallenge()`; `active = false`.
  6. `catch (e: CancellationException) { throw e }`.
  7. `catch (e: SessionFailure)` → `if (active) { sessionFailure = e; form.receive(DataFailure.Server(null)) }`.
  8. `catch (e: DataFailure)` → `if (active) form.receive(e)`.
  9. Any other `Exception` → `if (active) form.receive(DataFailure.Server(null))` (iOS `error as? DataFailure ?? .server(status: nil)`). Same catch order as 5a's `SignInModel`: never a broad catch before the cancellation rethrow; no `runCatching`.
  10. `finally { code = ""; form.finish() }`.
  - Use 0d's real `DataFailure.Server` constructor.
  - 5a's `accept` runs commit-and-route in `NonCancellable`, so a cancel after the commit still routes and still reaches `discardChallenge()` and `active = false`.
- **`cancel()`.**
  - Return when `!active`.
  - `active = false`; `code = ""`; `sessionFailure = null`; `form.clear()`; `navigation.cancelTwoFactor()`; `leave()`.
- **`companion fun filterCode(value: CharSequence): String`** = `value.filter { it in '0'..'9' }.take(6)`.

**`SignInModel` changes (5a's file) [F `SignInModel.swift:10, 115–120, 150–156`]:**
- New `val twoFactor: StateFlow<TwoFactorModel?>` (starts `null`; iOS `public private(set) var twoFactor: TwoFactorModel?`).
- New constructor parameter `private val onLeaveTwoFactor: () -> Unit = {}`, last in the list so 5a's call sites compile unchanged.
- `submitPassword`, step 4 becomes: `TwoFactor(challengeId)` → `if (navigation.twoFactor(challengeId)) { twoFactor.value = TwoFactorModel(source, navigation, accept = { accept(it, AuthenticationMethod.TwoFactor, commitAutofill = {}, password = null) }, leave = onLeaveTwoFactor); onTwoFactor() }`. Build the model **before** `onTwoFactor()` so the screen finds it. Each challenge gets a fresh model. `commitAutofill = {}`: the password field was cleared when the challenge arrived (OQ-5b-11).
- `clearSecrets()` starts with `twoFactor.value?.cancel(); twoFactor.value = null`, then 5a's body (iOS order).
- Never copy `accept`; the two-factor path calls the same private function, as iOS does.

**Gate.** 5a's `SignInRouter.acceptedSession(result, method)` routes through 4e's `routeAcceptedSession`. The expected routes equal `AuthNavigation.route(account)`:
- unverified → `Verify`;
- terms version differs → `Terms`;
- walkthrough unseen → `Walkthrough(replay = false)`;
- otherwise `Today`.

**Navigation [R] (5a §5.9, OQ-5a-7):**
- `SignInRouter.twoFactor()` (5a) = `rootNavigator.navigate(Route.TwoFactor)`. Unchanged.
- **New `SignInRouter.backToSignIn()`**, implemented in `AuthServices`: `if (rootNavigator.state.value.route == Route.TwoFactor) rootNavigator.navigate(Route.SignIn)`. The guard makes it idempotent and makes a late `cancel()` harmless after the route has already moved on (for example after success, or when the auth-exit observer runs `clearSecrets()`). `AuthServices` passes `onLeaveTwoFactor = router::backToSignIn` to `SignInModel`.
- Sign in's email lives in `SignInModel`, so back shows the kept email and an empty password (5a's password `TextFieldState` is `remember`ed in the Sign in screen, which is rebuilt).
- **`TwoFactorViewModel(signIn: SignInModel, router: SignInRouter)`** (`…/auth/twofactor/`), activity-scoped like `SignInViewModel` (OQ-5a-5). It holds **no per-visit state** except the running `request: Job?`; it always reads the current model from `signIn.twoFactor.value`.
  - `uiState: StateFlow<TwoFactorUiState?>` = `signIn.twoFactor.flatMapLatest { m -> m?.let { combine(it.state, it.form.state) { s, f -> TwoFactorUiState(s.mode, s.code, f, it.field) } } ?: flowOf(null) }.stateIn(viewModelScope, Eagerly, …)`. `TwoFactorUiState` holds no challenge id, and its `toString()` redacts the code.
  - `onCodeEdited(text)`, `toggleMode()` forward to the current model.
  - `validateChallenge()`: no model → `router.backToSignIn()`; else `model.validateChallenge()`.
  - `submit()`: `val m = current ?: return`; return when `request != null || m.form.state.value.busy`; `request = viewModelScope.launch { try { m.submit() } finally { request = null } }` (iOS `TwoFactorView.submit`, `request == nil`).
  - `back()`: `request?.cancel(); request = null; signIn.twoFactor.value?.cancel(); router.backToSignIn()` (iOS `TwoFactorView.cancel`). The second call is a no-op when `cancel()` already left.
- **Every way off the screen clears the challenge:** the drawn back and system back go through `back()`; success ends with `discardChallenge()`; leaving auth for any other route runs 5a's auth-exit observer → `clearSecrets()` → `twoFactor.cancel()`.
- **Never cancel from `DisposableEffect.onDispose`, `ON_STOP` or `onCleared`.** Recreation disposes the composition, and the person switches to the authenticator app (iOS cancels in `onDisappear`, which has no recreation; Android does not copy that).
- An in-flight success after a cancel sees `!active` or `navigation.challengeID != challenge` and is discarded.
- `Route.TwoFactor` carries no arguments. The challenge lives only in `AuthNavigation`.

### 5.6 Copy

[F `Kit/Auth/Resources/Localizable.xcstrings` at the pin, generated by 0c; en/nl in `05 §9.9`]

Every string exists from 0c (table `auth`). **Add no string.**

Static labels use `stringResource(R.string.<name>)` with the name from `tools/l10n/names.lock.json`. Banner and field keys resolve through `CopyIndex.find(CopyTable.Auth, key)`, as in 5a (`AuthText.resolve`).

| English key | nl |
|---|---|
| Enter your code | Voer je code in |
| Open your authenticator app and type the 6-digit code for ChronoFlow. | Open je authenticator-app en voer de 6-cijferige code voor ChronoFlow in. |
| 6-digit code | 6-cijferige code |
| Recovery code | Herstelcode |
| xxxx-xxxx | xxxx-xxxx (`shouldTranslate: false`) |
| Each recovery code works once. | Elke herstelcode kun je één keer gebruiken. |
| Verify | Bevestigen |
| Use a recovery code instead | Gebruik een herstelcode |
| Use your authenticator app | Gebruik je authenticator-app |
| That code didn't work. Codes change every 30 seconds, so try the newest one. | Die code werkte niet. Codes veranderen elke 30 seconden, dus probeer de nieuwste. |
| Enter the 6-digit code from your authenticator app. | Vul de 6-cijferige code uit je authenticator-app in. |
| Enter one of the recovery codes you saved when you set up two-factor authentication. | Vul een van de herstelcodes in die je hebt bewaard bij het instellen van tweefactorauthenticatie. |
| This recovery code was not accepted. Try another saved code. | Deze herstelcode is niet geaccepteerd. Probeer een andere bewaarde code. |
| Too many attempts. / Wait a few minutes before you try again. | Te veel pogingen. / Wacht een paar minuten voordat je het opnieuw probeert. |
| Banner Server, Offline, Back | as 5a |

- The two passkey 422 sentences are never shown; they become the Server banner (iOS OQ 30).
- The plan's 5b "Copy" item, the Android-only 2FA intro b1, replaces an iOS **Settings** string shown on `Set2FA` (10c, plan l.2491), not on this screen; plan l.76 also lists "the two-factor intro b1" as iOS-only copy that is not reused (OQ-5b-1). Check that `android_two_factor_intro_b1` exists and leave it unused.

### 5.7 Passkey sign-in

[F `PasskeySignIn.swift`, `SignInModel.swift:124–135`, `SystemPasskeyAssertionProvider.swift`, `03 §9`; R where marked]

**`PasskeySignIn`** (`…/auth/model/passkey/PasskeySignIn.kt`) ports the Swift class under its Swift name. 5a's `SignInModel` takes `passkey: suspend () -> SessionResult`, so `AuthServices` passes `PasskeySignIn(…)::signIn` (5a appendix: "5b adds `PasskeySignIn` and passes it as `passkey`").

```kotlin
class PasskeySignIn(
    private val source: DataSource,
    private val provider: PasskeyAssertionProviding,
    private val identity: suspend () -> String,     // DeviceIdentityStore.identity(), as 5a's deviceIdentity
    private val deviceName: suspend () -> String,   // 5a's memoised deviceName
)
```

`signIn()` runs these steps:
1. `deviceId = identity()`; `ensureActive()`.
2. `challenge = source.passkeyOptions()`.
3. `Base64URL.decode(challenge.options.challenge)`, which throws `InvalidEncoding`.
4. `rpId` empty → throw `PasskeyFailure.Unavailable`.
5. `ensureActive()`.
6. `assertion = provider.assertion(challenge)`.
7. `ensureActive()`.
8. `return source.passkeyLogin(ApiPasskeyLoginInput(challenge.challengeId, assertion, deviceId, deviceName(), draftToken = null, guestSecret = null))` (0d's property names; 11b adds the claim, iOS `claim()` at step 7).

Every attempt fetches fresh options.

**`SignInModel.submitPasskey(password)`** is 5a's and does not change: the capability guard, the busy lock, `accept(passkey(), Passkey, …)` and the catches (`CancellationException` → rethrow; `PasskeyFailure.Canceled` → silent; `SessionFailure` → kept, Server; `DataFailure` → `form.receive`; any other `PasskeyFailure` or exception → Server). With `displayedFields = {email, password}`:
- a 422 on `credential` or `challengeId` → Server banner;
- 429 → Locked with the sign-in body "Wait a few minutes before you try again, or reset your password now.";
- offline → Offline.

**Request JSON [R].** `PasskeyJson.requestJson(options) = ApiJson.encodeToString(ApiPasskeyRequestOptions.serializer(), options)`:
- exactly the 5 keys, in declared order;
- use whichever `ApiJson` instance 0d uses for request bodies.

For the fixture the result is exactly:

```
{"challenge":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","rpId":"app.chronoflow.net","timeout":60000,"userVerification":"required","allowCredentials":[]}
```

**Credential request [R].**
- `GetCredentialRequest(credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson)), preferImmediatelyAvailableCredentials = false)`;
- called with the Activity held by `PasskeyAnchor`.

**Response JSON → `ApiPasskeyAssertion` [R]** (`PasskeyJson.assertion(responseJson)`):
1. Parse as a `JsonObject`. Malformed JSON → `PasskeyFailure.Unavailable`.
2. These must be non-empty strings, else `Unavailable`:
   - `type`, which must equal `"public-key"`;
   - `rawId` (fall back to `id`);
   - `response.clientDataJSON`, `response.authenticatorData` and `response.signature`.
3. Decode each with `Base64URL.decodeLenient`: map `+`→`-` and `/`→`_`, strip trailing `=`, then strict `decode`. A failure → `InvalidEncoding`.
4. When `id` is present, it must decode to the same bytes as `rawId`, else `InvalidEncoding`.
5. A `userHandle` that is absent, `null` or `""` becomes `null` (iOS encodes `assertion.userID`; an empty one is dropped here so the body never carries `"userHandle":""`).
6. Return `Base64URL.assertion(credentialID, clientDataJSON, authenticatorData, signature, userHandle)`:
   - `id == rawId`;
   - every field is unpadded base64url;
   - every other member (`authenticatorAttachment`, `clientExtensionResults`, …) is dropped (`05` OQ A12).

**`SystemPasskeyAssertionProvider(client: CredentialClient)`** (twin of the iOS provider):
- `ensureActive()`;
- `Base64URL.decode(challenge.options.challenge)`;
- `json = client.getPasskey(PasskeyJson.requestJson(challenge.options))`;
- `return PasskeyJson.assertion(json)`.

**`CredentialManagerClient`** (`:core:data`) implements `CredentialClient`. Exception mapping [R]:

| Thrown or situation | `PasskeyFailure` | UI |
|---|---|---|
| `GetCredentialCancellationException` | `Canceled` | silent |
| `NoCredentialException` | `Canceled` | silent (`05` OQ A13, OQ-5b-3) |
| `GetPublicKeyCredentialDomException`, `GetCredentialInterruptedException`, `…ProviderConfigurationException`, `…UnsupportedException`, `…UnknownException`, any other `GetCredentialException` | `Unavailable` | Server banner (OQ-5b-4) |
| The result's credential is not a `PublicKeyCredential` | `Unavailable` | Server banner |
| No attached Activity | `Unavailable` | Server banner |
| A second call while one is pending (`AtomicBoolean`) | `Busy` | Server banner. The form lock normally prevents this. |
| `CancellationException` | rethrown | — |

**Capability [R]:**
- `PasskeyCapability.SERVER_READY = false` (5a). `AppGraph.passkeyCapability` is `PasskeyCapability.Shipped`, or `PasskeyCapability(true)` when the debug override applies; `AuthServices` passes it to `SignInModel.passkeyCapability`, which 5a's `SignInViewModel` already reads.
- While it is false, Sign in composes **neither** `sign-in.passkey` **nor** `sign-in.divider`, and `submitPasskey` is a no-op (5a, unchanged).
- Debug builds may turn it on with the seed extra `cf.passkeyCapability=1` (`"1"`/`"true"` → on; `"0"`/`"false"` → off; anything else → no override), for manual device checks once the server is ready.
- Release has no override path.

**Provider pairing [R, iOS `AuthSessionServices.swift:94–98`]:**
- With 4e's data-source selection `mock`, `AuthServices` uses `DeterministicAssertionProvider { throw PasskeyFailure.Unavailable }`. The passkey button then shows the Server banner, as the iOS mock app does (and as 5a's default lambda does today).
- With `api`, it uses `SystemPasskeyAssertionProvider(graph.credentialClient)`.

## 6. Kotlin shape [R]

```kotlin
// :core:model — net.chronoflow.core.model.passkey  (pure JVM; 10d's :feature:settings reuses these, so not in a feature)
@JvmInline value class PasskeyCapability(val enabled: Boolean) {          // moved unchanged from 5a's feature/auth/…/model/passkey/
    companion object { const val SERVER_READY = false; val Shipped = PasskeyCapability(SERVER_READY) }
}
sealed class PasskeyFailure : Exception(null, null, false, false) {      // moved unchanged from 5a (all five cases exist)
    data object Canceled : PasskeyFailure(); data object InvalidEncoding : PasskeyFailure()
    data object Unavailable : PasskeyFailure(); data object Busy : PasskeyFailure()
    data object AlreadyRegistered : PasskeyFailure()                       // used by 10d
}
object Base64URL {
    fun encode(bytes: ByteArray): String           // java.util.Base64.getUrlEncoder().withoutPadding()
    fun decode(value: String): ByteArray           // iOS rules (PasskeySignIn.swift:13–28): non-empty; only [A-Za-z0-9_-];
                                                   // length % 4 != 1; decodes and re-encodes to the same string; else throw InvalidEncoding
    fun decodeLenient(value: String): ByteArray    // + → -, / → _, strip trailing '=', then decode
    fun assertion(credentialID: ByteArray, clientDataJSON: ByteArray, authenticatorData: ByteArray,
                  signature: ByteArray, userHandle: ByteArray?): ApiPasskeyAssertion   // id == rawId, type PublicKey
}
object PasskeyJson {
    fun requestJson(options: ApiPasskeyRequestOptions): String
    fun assertion(responseJson: String): ApiPasskeyAssertion        // §5.7
}
interface CredentialClient { suspend fun getPasskey(requestJson: String): String }  // authenticationResponseJson; throws PasskeyFailure

// :core:data — net.chronoflow.core.data.passkey  (Android; the only module with androidx.credentials)
class PasskeyAnchor { fun attach(activity: Activity); fun detach(activity: Activity); fun current(): Activity? } // WeakReference
class CredentialManagerClient(appContext: Context, anchor: PasskeyAnchor) : CredentialClient  // CredentialManager by lazy
internal fun GetCredentialException.toPasskeyFailure(): PasskeyFailure

// :feature:auth — net.chronoflow.feature.auth.model.passkey  (5a's package for the passkey seam)
fun interface PasskeyAssertionProviding { suspend fun assertion(challenge: ApiPasskeyChallenge): ApiPasskeyAssertion }
class DeterministicAssertionProvider(
    private val provide: suspend (ApiPasskeyChallenge) -> ApiPasskeyAssertion = { throw PasskeyFailure.Canceled },
) : PasskeyAssertionProviding
class SystemPasskeyAssertionProvider(client: CredentialClient) : PasskeyAssertionProviding
class PasskeySignIn(…) { suspend fun signIn(): SessionResult }      // §5.7

// :feature:auth — net.chronoflow.feature.auth.model  (pure, like 5a's SignInModel)
class TwoFactorModel(…) { enum class Mode { Code, Recovery } … }  // §5.5
data class TwoFactorState(val mode: TwoFactorModel.Mode, val code: String, val sessionFailure: SessionFailure?)  // toString redacts code

// :feature:auth — net.chronoflow.feature.auth.twofactor
object TwoFactorFieldSpec { fun forMode(mode: TwoFactorModel.Mode): FieldSpec }  // §5.3 table, string res ids
class TwoFactorCodeTransformation : InputTransformation
data class TwoFactorUiState(val mode: TwoFactorModel.Mode, val code: String, val form: AuthFormState, val field: String) // toString redacts code
class TwoFactorViewModel(signIn: SignInModel, router: SignInRouter) : ViewModel() {
    val uiState: StateFlow<TwoFactorUiState?>
    fun onCodeEdited(text: String); fun toggleMode(); fun validateChallenge()
    fun submit()                                   // viewModelScope.launch, ignored while a job runs (iOS `request == nil`)
    fun back()                                     // job?.cancel(); model?.cancel(); router.backToSignIn()
    companion object { val Factory: ViewModelProvider.Factory }   // viewModelFactory { initializer { … this[AppGraphKey]!!.auth … } } (A4), like 5a
}
@Composable fun TwoFactorScreen(viewModel: TwoFactorViewModel)
@Composable internal fun TwoFactorContent(state: TwoFactorUiState, field: TextFieldState,
    onSubmit: () -> Unit, onToggle: () -> Unit, onBack: () -> Unit)   // stateless; guards and captures render this
```

- Keep the Swift names `TwoFactorModel`, `PasskeyFailure`, `Base64URL`, `PasskeyAssertionProviding`, `DeterministicAssertionProvider`, `SystemPasskeyAssertionProvider` and `PasskeySignIn`.
- `model/` stays free of Android and Compose imports (5a's rule). `TwoFactorFieldSpec` uses Compose types, so it lives in `twofactor/`.
- If 5a placed `model/`, `shell/` or `signin/` elsewhere, mirror its layout.

## 7. Tasks

**Rules for every task:**
- Each task is one Conventional Commit with no trailer. Tick the `plan.md` 5b box named in brackets in the same commit.
- Each fix round runs only what it touched: `make targeted ONLY="…"`, module paths without a leading colon, e.g. `feature:auth:TwoFactorModelTests`.
- Run `make quick` once before the last commit, and `make check` at the end.
- If your sandbox cannot run Gradle, `adb` or the emulator, write the code and tests, then hand the orchestrator the exact commands and their output.

**Test naming:**
- A ported Swift suite keeps its suite name as the Kotlin class (`TwoFactorModelTests`) and its method names (drop the iOS `test` prefix of XCTest methods).
- A Swift `@Test(arguments:)` with ≤ 5 rows becomes one plain method per row, named `<swiftName><Row>` (4c's rule), e.g. `emptyProofShowsOnlyTheActiveFieldErrorRecovery`.
- Android-only classes end in `Test`.
- Every coroutine test uses `runTest(timeout = 60.seconds)`, and every JVM test takes < 1 s.

### Task 1: Move the passkey seam (`refactor(model): move the passkey seam into core model`)

- Move 5a's `PasskeyCapability` and `PasskeyFailure` from `…/auth/model/passkey/` to `…/model/passkey/` (package `net.chronoflow.core.model.passkey`), **unchanged in shape** (§6).
- Update every import (5a's `SignInModel`, `SignInViewModel`, `SignInScreen`, tests). No behaviour change: 5a's tests stay green (`make targeted ONLY="feature:auth:SignInModelTests feature:auth:SignInScreenTest"`).
- Test `PasskeyCapabilityTest` (JVM, `core/model/src/test/kotlin/net/chronoflow/core/model/passkey/`):
  - `Shipped.enabled == false`;
  - `SERVER_READY == false`;
  - every `PasskeyFailure` case has `message == null` and no stack trace (`stackTrace.isEmpty()`).

### Task 2: Base64URL and passkey JSON (`feat(model): add base64url and passkey json`)

- Files: `…/model/passkey/Base64URL.kt`, `PasskeyJson.kt` and `CredentialClient.kt` (§6, §5.7).
- **`…/authT/model/passkey/PasskeyEncodingTests.kt`** (JVM; both Swift tests, kept together because the second needs 5a's `AuthFailureMapping`):
  - **`binaryBytesRoundTripWithoutUTF8OrPaddingAndAllDTOFieldsUseBase64URL`:**
    - bytes `0..255` encode without `=`, `+` or `/`, and round-trip;
    - each of `""`, `"a"`, `"AQ=="`, `"AQ+"`, `"AQ/"`, `"AR"`, `"é"` and `"a b"` throws `PasskeyFailure.InvalidEncoding`;
    - `assertion(bytes…, userHandle = bytes)` has `id == rawId == encoded`, `type == CredentialType.PublicKey`, and all four response fields `== encoded`;
    - `userHandle = null` stays `null`;
    - an encode/decode round trip through 0d's `ApiJson` gives an equal value.
  - **`passkeyFieldFailuresUseOnlyDesignedBannerAndNeverLeakServerCopy`:** `AuthFailureMapping(Validation(mapOf(field to FieldMessage(key)), 422), displayedFields = setOf("email", "password"))` gives banner `Server` and no fields, for both pairs:
    - (`credential`, "We couldn't sign you in with that passkey. Please try again.");
    - (`challengeId`, "The passkey request expired. Please try again.").
- **`core/model/src/test/kotlin/net/chronoflow/core/model/passkey/PasskeyJsonTest.kt`** (JVM; read the fixture the way 0d's contract tests do):
  - **`requestJsonHasExactlyTheFiveOptionKeys`:** the fixture `auth/passkey-options.json` → `requestJson(fixture.options)` equals the §5.7 string byte for byte.
  - **`credentialManagerResponseBecomesTheLoginAssertion`.** Input:
    ```
    {"id":"AQ","rawId":"AQ","type":"public-key","authenticatorAttachment":"platform","clientExtensionResults":{},"response":{"clientDataJSON":"eyJ0eXBlIjoid2ViYXV0aG4uZ2V0In0","authenticatorData":"Ag==","signature":"Aw","userHandle":""}}
    ```
    - The result equals `ApiPasskeyAssertion(id = "AQ", rawId = "AQ", type = PublicKey, response = ApiPasskeyAssertionResponse("eyJ0eXBlIjoid2ViYXV0aG4uZ2V0In0", "Ag", "Aw", null))`.
    - Its encoded JSON has exactly the top-level keys `id, rawId, type, response`, and response keys `clientDataJSON, authenticatorData, signature`.
  - **`standardAlphabetIsNormalized`:** `"+/8"` in `signature` → `"-_8"`.
  - **`mismatchedIdAndRawIdIsInvalidEncoding`:** `id = "Ag"` with `rawId = "AQ"`.
  - **`wrongTypeMissingFieldsOrMalformedJsonAreUnavailable`:** `type: "password"`; a missing `signature`; `"{"`.

### Task 3: Credential Manager client (`feat(data): wrap credential manager for passkey assertions`)

- **Before you add the dependencies, run `make size` and keep the line.**
- Add `androidx.credentials:credentials` and `credentials-play-services-auth` (0a catalog aliases) to `:core:data` only.
- Files:
  - `…/data/passkey/PasskeyAnchor.kt` and `CredentialManagerClient.kt` (§6, §5.7 table);
  - `app/src/main/keepRules/credentials.keep` with the §4 rule, next to 0a's `app.keep` and registered the same way.
- **`getPasskey`:**
  1. `val activity = anchor.current() ?: throw PasskeyFailure.Unavailable`.
  2. If `pending.compareAndSet(false, true)` fails, throw `PasskeyFailure.Busy`.
  3. `try`: `credentialManager.getCredential(activity, request)` → cast `response.credential` to `PublicKeyCredential` or throw `Unavailable` → return `authenticationResponseJson`.
  4. `catch (e: GetCredentialException) { throw e.toPasskeyFailure() }`.
  5. `finally { pending.set(false) }`.
- `CredentialManager.create(appContext)` happens inside the first call (`by lazy`), never at construction.
- **Tests** (`core/data/src/test/kotlin/net/chronoflow/core/data/passkey/`):
  - **`CredentialManagerClientMappingTest`** (JVM): one method per exception row of the §5.7 table, calling `toPasskeyFailure()` on an instance built directly. Example: `GetPublicKeyCredentialDomException(NotAllowedError())` → `Unavailable`; `NoCredentialException()` → `Canceled`. If constructing them fails on the JVM ("Method … not mocked"), make the class Robolectric `@Logic`.
  - **`PasskeyAnchorTest`** (Robolectric `@Logic`, `Robolectric.buildActivity(ComponentActivity::class.java)`):
    - attach A, then detach B → `current() == A`;
    - detach A → `null`.
- **After the change, run `make size` again.** Put both lines in the commit body; Task 11 records them.

### Task 4: Two-factor model (`feat(auth): port the two-factor model`)

- Files: `…/auth/model/TwoFactorModel.kt` (a line-by-line port, §5.5); the §5.5 `SignInModel` changes; `SignInRouter.backToSignIn()` (interface only; `AuthServices` implements it in Task 6); `RecordingRoutes` gains a `backToSignIn` counter.
- **Test support** in `…/authT/model/TwoFactorTestSupport.kt` (port of Swift l.292–353):
  - **`twoFactorSource(verified = true, code = "123456", now: () -> Instant = { Instant.now() }, sleep: suspend (Double) -> Unit = {})`** builds `MockDataSource(store = MemoryMockStateStore(), seed = …, now = now, delay = { 0.0 }, sleep = sleep, debugAppSeed = false, io = <the test dispatcher>)` with:
    - `seed = MockServerState(accounts = listOf(MockServerAccount(snapshot, password = "password", twoFactorCode = code, recoveryCodes = setOf("AbCdEf1234-GhIjKl5678"), walkthroughSeen = true)))`;
    - the snapshot is 0d's `account/snapshot.json`, with `email = "twofactor@example.com"`, `hasVerifiedEmail = verified` and policies `("1", "1", "1")`.
  - **`TestClock`:** `var instant = Instant.ofEpochSecond(1_800_000_000)`, `fun now()`, and `fun advance(seconds: Long)`.
  - **`TestGate`:** `armed: Boolean`, `entered: CompletableDeferred<Unit>`, `released: CompletableDeferred<Unit>`. `suspend fun wait()` returns at once unless armed; otherwise it completes `entered` and awaits `released`. Pass `sleep = { gate.wait() }`.
  - **`challenged(source, session = FakeSessionController(), routes = RecordingRoutes()): SignInModel`:**
    1. Build 5a's `SignInModel(source, session, deviceIdentity = { "0199a3c0-0000-7000-8000-000000000001" }, deviceName = { "test" }, accepted = routes::accepted, onTwoFactor = routes::twoFactor, onLeaveTwoFactor = routes::backToSignIn)` (adapt to 5a's real fake API).
    2. `editEmail("twofactor@example.com")`, then `submitPassword(FakeSecretField("password"), commitAutofill = {})`.
    3. Assert `routes.twoFactor == 1`, `navigation.challengeID != null` and `twoFactor.value != null`.
    4. Return the `SignInModel`; each test reads `model = signIn.twoFactor.value!!`.
- **`…/authT/model/TwoFactorModelTests.kt`** (JVM; Swift names and cases; read the Swift bodies at `KitT/AuthTests/TwoFactorModelTests.swift:8–289`). "Accept" means `FakeSessionController` recorded an accept; "route" means `routes.accepted` with `AuthNavigation.route(result.account)`:
  1. **`emptyProofShowsOnlyTheActiveFieldErrorCode` / `…Recovery`:**
     - `form.failure` carries both field messages, but `form.fields` holds only the active one;
     - no banner, an empty code, not busy;
     - the announcement (fake resolver returning the key) is the active key once, then `null`.
  2. **`latestCodeAcceptsThroughPasswordSessionPathVerified` / `…Unverified`** (source code `654321`):
     - no accept and no route before the proof;
     - an empty submit gives "Enter the 6-digit code from your authenticator app.";
     - `123456` gives the 30-second copy, an empty code, no banner, not busy;
     - `654321` submitted twice → exactly one accept;
     - exactly one route, `Today` / `Verify`, with method `AuthenticationMethod.TwoFactor`;
     - an empty code and `navigation.challengeID == null`.
  3. **`numericInputFiltersPastesAndAutoFillWithoutSubmitting`:**
     - `"a12 ٣b345678"` → `"123456"`, with no accept and not busy;
     - after a toggle, `"AbCdEf1234-GhIjKl5678"` is kept as typed.
  4. **`recoveryIsSingleUseAndModeToggleClearsFeedback`:** as Swift; the second `challenged` reuses the same source and session, and its recovery submit gives "This recovery code was not accepted. Try another saved code.".
  5. **`expiryAndFiveFailuresKeepTheReturnedFieldState`:** both Swift loop iterations in one method:
     - expired case: `clock.advance(300)`;
     - exhausted case: five `000000` submits, then `advance(60)`;
     - then `123456` → the 30-second copy, no banner, empty code, not busy, no accept.
  6. **`throttleServerOfflineAndHiddenFieldsUseKeptDefaults`:**
     - before each submit `editCode("123456")`, then `failNext(MockMethod.CompleteTwoFactor, …)` with `Throttled(73.0, 429)`, `Server(503)`, `Offline`, `Validation({challengeId: "Please fill this in."}, 422)` and `Validation({recovery_code: "Please fill this in."}, 422)`;
     - `form.failure == it`, empty code, not busy; banners Locked / Server / Offline / Server / Server;
     - Locked keeps `retryAfterSeconds == 73.0` (its nl announcement text is asserted in `TwoFactorCopyTest`);
     - `editCode("1")` clears the banner.
  7. **`doubleSubmitBusyEditsToggleAndBackDiscardLateSuccess`:**
     - `editCode("123456")`, arm the gate, launch `submit()` and await `entered`;
     - a second `submit()`, `editCode("654321")` and `toggleMode()` are all ignored (code still `123456`, mode Code, busy);
     - `model.cancel()` → empty code, `navigation.challengeID == null`, `routes.backToSignIn == 1`;
     - release the gate and join → no accept, not busy, `form.failure == null`;
     - `signIn.navigation.twoFactor("new-challenge"); model.cancel()` → `challengeID == "new-challenge"` and `backToSignIn` is still 1.
  8. **`taskCancellationClearsCodeWithoutPresentationOrAcceptance`:** cancel the launched job while it waits in the gate, release, join → empty code, no failure, not busy, no accept.
  9. **`missingChallengeReturnsToSignInAndNewNavigationCannotRestoreSecrets`:**
     - `editCode("123456")`, `signIn.navigation.discardChallenge()`, then `submit()` → `routes.backToSignIn == 1`, empty code;
     - `fresh = AuthNavigation()`; `missing = TwoFactorModel(twoFactorSource(), fresh, accept = { fail("Missing challenge must not accept") }, leave = { freshLeaves++ })`; `missing.validateChallenge()` → `freshLeaves == 1`, `fresh.challengeID == null`, empty code;
     - `fresh.twoFactor(" \n")` returns `false` and leaves `challengeID == null`;
     - no accept.
  10. **`sessionStorageFailureRemainsTypedAndShowsTheServerBanner<Case>`:** 5 methods, one per `SessionFailure` case, with `FakeSessionController` throwing it. After `editCode("123456")` and `submit()`: `sessionFailure == case`, banner Server, empty code, a non-null announcement, no route.
  11. *(`onlySessionAcceptancePersistsATokenAndNeverTheChallengeOrProof` is in `:app`, Task 6.)*
  12. **`backendMessagesLocalizeOnlyUnderTheDisplayedModeAndAnnouncementsAreConsumedOnce`:**
      - the four `(recovery, key, displayed)` rows of Swift l.246–266: toggle to the row's mode, `form.receive(Validation({field: key}, 422))` → `form.failure` equal, no banner, and `fields[field].key == displayed`;
      - then twice (the Swift en and nl iterations): `receive(Throttled(40.0, 429))` → `takeAnnouncement` non-null once, then `null`; `toggleMode()` → `takeAnnouncement == null` and no fields;
      - the en/nl text assertions (the Swift `dutch` column and the throttle sentences) are in `TwoFactorCopyTest`.
- **`…/authT/model/TwoFactorCopyTest.kt`** (Robolectric `@Logic`; resolve text the way 5a's `AuthCopyResourcesTest` does):
  - for the four rows of test 12, `AuthFieldCopy.field(FieldMessage(displayed))` resolved in en equals `displayed`, and in nl equals the Swift `dutch` column (§5.6);
  - after `receive(Throttled(…))` in a `TwoFactor` form, `takeAnnouncement` resolved in en is "Too many attempts. Wait a few minutes before you try again." and in nl is "Te veel pogingen. Wacht een paar minuten voordat je het opnieuw probeert.".
- **`…/authT/model/SignInModelTests.kt`** (5a's class) gains one Android test, **`clearSecretsCancelsTheTwoFactorModel`:** after `challenged`, `clearSecrets()` → the old model is inactive (a later `editCode("1")` leaves its code empty), `twoFactor.value == null`, `challengeID == null`, email kept.

### Task 5: Two-factor screen (`feat(auth): add the two-factor screen`)

- Files in `…/auth/twofactor/`:
  - `TwoFactorFieldSpec.kt`, `TwoFactorCodeTransformation.kt`;
  - `TwoFactorViewModel.kt` (+ `TwoFactorUiState`, factory; §5.5, §6);
  - `TwoFactorScreen.kt`: `TwoFactorScreen` plus the stateless `TwoFactorContent` (§5.2–§5.4).
  - The `AuthFailureFeedback` generalisation of §5.3, if 5a needs it.
- **Tests:**
  - **`…/authT/twofactor/TwoFactorFieldSpecTest`** (JVM; move it to Robolectric `@Logic` only if `ContentType` needs Android):
    - the §5.3 table for both modes;
    - `filterCode`: `"a12 ٣b345678"` → `"123456"`; `"１２３"` → `""`; `"9876543"` → `"987654"`; `""` → `""`.
  - **`…/authT/twofactor/TwoFactorControlTests`** (Robolectric `@Logic`, `ProfileRule(pixel8)` + `ProfileHost`, en; a real `TwoFactorViewModel` over a `SignInModel` from Task 4's `challenged(twoFactorSource())` and a `RecordingRoutes` router; Swift names from `I/AppTests/TwoFactorControlTests.swift`):
    - **`displayedCodeRejectsOverflowAndNonDigitsEvenWhenTheModelDoesNotChange`:** with `two-factor.code` focused, `performTextInput` of `"a٣"`, `"12b٣345678"`, `"7"`, `"x"` in turn → field text and model code `""`, `"123456"`, `"123456"`, `"123456"`; still focused; not busy.
    - **`nativeInputTraitsAndModeToggleDiscardTheOldKeyboardConfiguration`:** `two-factor.code` has `SemanticsProperties.ContentType` = the §5.3 value and the IME action `Go`; type `123`, click `two-factor.toggle` → `two-factor.recovery_code` exists and is empty, `two-factor.code` does not exist, the toggle text is "Use your authenticator app", and the recovery field has no content type and IME action `Go`.
    - **`recoveryPlaceholderSurvivesErrorsAndClearsWhenReturningToCode`:** in Recovery with a field error, the placeholder node `xxxx-xxxx` is shown and the text is empty; toggle → the code field has no placeholder and empty text.
    - **`failedAttemptsRefocusTheEnabledFieldWithoutEditingAwayFeedback`** (both modes): `Server(503)` → the field is enabled, not focused, banner Server; `Validation({field: "Please fill this in."}, 422)` → the field is enabled and focused, the error stays.
    - **`bothDisplayedProofsFollowModelEditsAndAttemptClearingWithoutErasingErrors`** (both modes): type the proof → field and model equal; `model.editCode(other)` → the field shows it; submit (wrong proof) → field and model empty, the field error stays (`SemanticsProperties.Error` holds the mapped copy), no banner.
    - **`verifyKeepsItsLabelAndSizeWhileBusyInBothLanguages`** (en and nl as two methods, `…En` / `…Nl`): gate the mock; while pending, `two-factor.submit` keeps its idle size (± 0.5 dp) and label, the field, submit and toggle are not enabled, and `auth.back` is enabled.
  - **`…/authT/twofactor/TwoFactorScreenTest`** (Robolectric `@Logic`, same setup; Android-only):
    - **`bannerIsFirstInCardAndAnnouncedOnce`:** `failNext(CompleteTwoFactor, Throttled(60.0, 429))` → the top of `auth.banner` is above the field's top, and exactly one `auth.announcement` node holds "Too many attempts. Wait a few minutes before you try again.".
    - **`headingAndPaneTitle`:** `auth.heading` has `Heading`, and a node with `PaneTitle == "Enter your code"` exists.
    - **`imeGoSubmitsOnceAndNeverAutoSubmits`:** typing `123456` makes no data-source call (no `form.pending`); `performImeAction()` makes exactly one.
    - **`missingModelReturnsToSignIn`:** with `signIn.twoFactor.value == null`, composing the screen calls `router.backToSignIn()` once.
  - **`…/authT/twofactor/TwoFactorSavedStateTest`** (Robolectric `@Logic`): use the same `SaveableStateRegistry` technique as 5a's `SignInSecretsTest.nothingSavedContainsASecret`:
    - type `123456` and run `performSave()`;
    - the flattened values contain neither `123456` nor the challenge;
    - after `StateRestorationTester.emulateSavedInstanceStateRestore()` with the same ViewModel, the field shows `123456` (from memory) and `navigation.challengeID` is unchanged.

### Task 6: Route and hand-over (`feat(app): route two-factor and hand over to the session gate`) [ticks "TwoFactorScreen and TwoFactorViewModel"]

- **`:app` wiring:**
  - register the `Route.TwoFactor` entry in `RootContent`: `TwoFactorScreen(viewModel(factory = TwoFactorViewModel.Factory))`, the factory reading `this[AppGraphKey]!!.auth` (A4);
  - `AuthServices`: implement `backToSignIn()` (§5.5) and pass `onLeaveTwoFactor = router::backToSignIn` to `SignInModel`.
- **Debug seed** (`…/debug/ScreenSessionFixture`, port of iOS `prepareChallenge` and `SceneDelegate.swift:512–514`): `CF_AUTH_TEST_AUDIT_SCREEN=two-factor` (signed out):
  1. `navigate(Route.SignIn)`;
  2. on main: `auth.signIn.editEmail(MockScenarioValues.twoFactorEmail)`, then `auth.signIn.submitPassword(<a debug fixed-value SecretField holding MockScenarioValues.password>, commitAutofill = {})`, and wait for it. The real model navigates to `Route.TwoFactor` and keeps the email.
  3. then `finishLaunch` as for the other audit screens.
  - Document the value in `plan.md` "seeds and test hooks". If you add a debug class, add it to `ReleaseReadinessTest`.
- **`…/appT/auth/TwoFactorRoutingTest`** (Robolectric `@Logic`, the real `RootContent`, `RootNavigator` and `AuthServices`, mock source with zero delay, `GraphOverrides(sessionCipher = SoftwareSessionCipher())` as 4e requires):
  - **`twoFactorReplacesSignIn`:** after a two-factor sign-in the route is `TwoFactor`, and no route value's `toString()` contains the challenge.
  - **`drawnBackDiscardsChallengeAndKeepsTheEmail`:** type an email on Sign in, reach Two-factor, click `auth.back` → the route is `SignIn`, `sign-in.email` shows the email, `navigation.challengeID == null`, and the old model is inactive.
  - **`systemBackDiscardsChallenge`:** `activity.onBackPressedDispatcher.onBackPressed()` → the same.
  - **`reenteringGetsAFreshModel`:** back, then submit the password again → `signIn.twoFactor.value` is a new instance with an empty code.
  - **`recreationKeepsTheScreenTheChallengeAndTheCode`:** type `123` and `scenario.recreate()` → the route is still `TwoFactor`, the field shows `123`, and the challenge is unchanged.
  - **`acceptedCodeRoutesThroughTheGate`:** code `123456` → `RootNavigator` shows `AuthNavigation.route(account)` for the twofactor account (`Today` with the 4c seed; if 4c's twofactor account has terms pending, `Terms`), and `navigation.challengeID == null`.
- **`…/appT/auth/TwoFactorModelRoutingTests`** (JVM; the app half of the Swift suite `TwoFactorModelTests`):
  - **`onlySessionAcceptancePersistsATokenAndNeverTheChallengeOrProof`:** 4e's real `SessionController` with `MemorySessionStore` and a local capturing `SessionAccountStore` that keeps `ApiJson.encodeToString(account)` of each upsert (Swift `TwoFactorAccounts`), adapted to 5a's `SessionAcceptor`; `challenged(twoFactorSource(), session)` (copy the two helpers into `…/appT/auth/` if test sources are not shared);
    - after `000000` nothing is stored and no account record exists;
    - after a toggle and typing `AbCdEf1234-GhIjKl5678`, still nothing is stored;
    - the submit stores a blob that is a JSON object with exactly the keys `token` and `accountId`, and a non-empty token;
    - neither the blob nor the account records contain the challenge or `AbCdEf1234-GhIjKl5678`;
    - empty code, `challengeID == null`.
- **`…/appT/auth/SignInModelRoutingTests`** (5a's class): add the `twoFactor` method to `committedSessionUsesStoredGateWithAndWithoutCancellation`, exactly as Swift l.199–211: the source is 5a's `authTestSource(twoFactor = true)`; after `submitPassword`, `twoFactor.value != null`, no active session and no stored credentials; `editCode("123456")` and `submit()` on it inside the same request; the same gate, cancel and assertions as the other methods, plus `code` empty and `sessionFailure == null` on the two-factor model. Keep 5a's parameterisation; the method becomes one more loop value or parameter, whichever 5a used.
- **`AuthServicesTest`** (5a's class) gains `backToSignInOnlyLeavesTwoFactor`: from `TwoFactor` → `SignIn`; from `Today` or `SignIn` → no navigation (revision unchanged).

### Task 7: Guards and captures (`test(auth): guard and capture the two-factor states`)

- **States.** Render `TwoFactorContent` with a fixed `TwoFactorUiState` and an empty field for each:
  - `code-none`;
  - `code-fieldError` (the 30-second copy);
  - `code-attempts` (Locked);
  - `code-server`;
  - `recovery-none`;
  - `recovery-fieldError` ("This recovery code was not accepted. Try another saved code.");
  - `recovery-attempts`;
  - `recovery-server`.
  These are the iOS reference states (`I/AppTests/__Snapshots__/Auth/TwoFactor/*-{code,recovery}-{none,fieldError,attempts,server}.png`).
- **`…/authT/twofactor/TwoFactorGuardsTest`** (`@Layout`, `ParameterizedRobolectricTestRunner`, one case per row; name `{state}_{profile}_{lang}_{theme}`):
  - **The matrix (38 cases).** The worst profiles are paired with nl:
    - 8 states × {`small`+nl, `pixel8`+en, `foldInner`+en, `largeFont`+nl}, light = 32;
    - `code-attempts` and `recovery-attempts` on `landscapeSideNav`, en, light = 2;
    - `code-none`, `code-server`, `recovery-none` and `recovery-fieldError` on `pixel8`, en, dark = 4.
  - Each case runs `Guards.assertAll(rule, profile, headingTags = setOf("auth.heading"))`.
  - Each case also checks:
    - `two-factor.submit` and `two-factor.toggle` exist exactly once;
    - on `foldInner` and `landscapeSideNav` the card is 560 ± 0.5 dp wide and centred (5a's column);
    - the `recovery-none` cases show the `xxxx-xxxx` placeholder node.
  - Each case < 5 s. Record the class's run time in the commit body.
- **`…/authT/twofactor/AuthTwoFactorCaptureTest`** (`@Capture`; the same setup as 5a's `SignInBoardCaptureTest`: 375 × 812 dp, density 3, zero insets):
  - `BoardCapture.capture(rule, "AuthTwoFactor_<state>_en_<theme>")` for the 8 states × light/dark = 16;
  - plus `AuthTwoFactor_code-fieldError_nl_light` and `AuthTwoFactor_recovery-none_nl_light`;
  - 18 PNGs in total.
- The Sign in passkey-on layout is already guarded and captured by 5a (`SignInGuardsTest`, `AuthSignIn_*` with the capability on). Do not duplicate it. 5a's `AuthStatesCaptureTest` already captures the two "Two-factor code" banners.

### Task 8: Passkey sign-in (`feat(auth): port passkey sign-in`)

- Files in `…/auth/model/passkey/`:
  - `PasskeyAssertionProviding.kt`;
  - `DeterministicAssertionProvider.kt`;
  - `SystemPasskeyAssertionProvider.kt`;
  - `PasskeySignIn.kt` (§5.7).
- **Test support** (`…/authT/model/passkey/PasskeyTestSupport.kt`, port of Swift l.161–180):
  - **`passkeySource(verified = true, twoFactor = false)`:**
    - 0d's snapshot with `hasVerifiedEmail = verified` and policies `("1.0", "1.0", "1.0")`;
    - `MockServerAccount(snapshot, password = "Password123!", twoFactorCode = if (twoFactor) "123456" else null, walkthroughSeen = true)`;
    - `delay = { 0.0 }`, `sleep = {}`, `debugAppSeed = false`, `io` = the test dispatcher;
    - `assertionProvider = { credential, _ -> if (credential.id == "AQ") snapshot.id else null }`.
  - **`testAssertion()`** = `Base64URL.assertion(byteArrayOf(1), "{\"type\":\"webauthn.get\"}".toByteArray(), byteArrayOf(2), byteArrayOf(3), null)`.
  - **`passkeyModel(source, provider, session = FakeSessionController(), routes = RecordingRoutes())`** = 5a's `SignInModel` built with:
    - `passkeyCapability = PasskeyCapability(true)`;
    - `passkey = PasskeySignIn(source, provider, identity = { "0199a3c0-0000-7000-8000-000000000001" }, deviceName = { "test" })::signIn`;
    - the same device identity and name, `accepted = routes::accepted`, `onTwoFactor = routes::twoFactor`.
  - Call `submitPasskey(FakeSecretField(""))` (5a's signature).
- **`…/authT/model/passkey/PasskeySignInTests`** (JVM; Swift names; `KitT/AuthTests/PasskeySignInTests.swift:41–158`):
  - **`mockPasskeyAcceptsVerifiedUnverifiedAndTwoFactorAccountsDirectlyVerified` / `…Unverified` / `…TwoFactor`:**
    - the provider sees `rpId == "app.chronoflow.net"`, `userVerification == Required` and a challenge that decodes to 32 bytes;
    - one accept;
    - one route: `Today` / `Verify` / `Today`;
    - `routes.twoFactor == 0`, and `navigation.challengeID == null`.
  - **`canceledPromptIsSilentAndRetryAlwaysGetsFreshOptions`:** the first assertion throws `Canceled` → not busy, no banner, no failure, no accept; the second attempt gets a different `challengeId` and is accepted once.
  - **`consumedExpiredCredentialThrottleServerAndOfflineFailuresRetryWithoutSession`:**
    - `failNext(MockMethod.PasskeyLogin, …)` with the five Swift failures in order: `Validation({challengeId: "The passkey request expired. Please try again."}, 422)`, `Validation({credential: "We couldn't sign you in with that passkey. Please try again."}, 422)`, `Throttled(87.0, 429)`, `Server(500)`, `Offline`;
    - each time `form.failure == it`, not busy, no accept;
    - every `challengeId` seen by the provider is new;
    - then one more attempt → one accept.
  - **`failedCredentialConsumesChallengeAndRetryFetchesAnother`:**
    - the first assertion has `id = rawId = "Ag"` → Server banner;
    - calling `source.passkeyLogin` directly with the used challenge and `testAssertion()` throws `Validation({challengeId: "The passkey request expired. Please try again."}, 422)`;
    - the retry gets a different challenge and is accepted once.
  - **`taskCancellationAfterAssertionNeverAcceptsOrCallsLogin`:**
    - the provider calls `currentCoroutineContext().cancel()` and returns `testAssertion()`;
    - run `launch { model.submitPasskey(…) }.join()`;
    - no accept, no route, no banner, not busy.
- **`…/authT/model/passkey/SystemPasskeyAssertionProviderTest`** (JVM, a fake `CredentialClient` that records its argument):
  - **`requestIsTheOptionsJsonAndResponseIsParsed`:** with the fixture challenge, the recorded request equals `PasskeyJson.requestJson(fixture.options)`, and the result equals Task 2's expected assertion.
  - **`cancelIsSilentThroughSignIn`:** the client throws `Canceled` → after `submitPasskey` there is no banner, no failure, not busy, and no announcement.
  - **`otherFailuresShowTheServerBanner`:** `Unavailable`, `InvalidEncoding` and `Busy` each give banner Server.

### Task 9: Passkey wiring (`feat(app): sign in with a passkey through credential manager`) [ticks "Passkey sign-in"]

- **`AppGraph`:**
  - lazy `passkeyAnchor`;
  - lazy `credentialClient = CredentialManagerClient(appContext, passkeyAnchor)`;
  - `passkeyCapability` per §5.7 "Capability".
- **`AuthServices`:**
  - `assertionProvider` per §5.7 "Provider pairing", using the same `mock`/`api` selection as 4e's data-source factory;
  - `SignInModel(…, passkeyCapability = graph.passkeyCapability, passkey = PasskeySignIn(source, assertionProvider, identity = deviceIdentity, deviceName = deviceName)::signIn, …)`, replacing 5a's default lambda.
- `MainActivity.onCreate` calls `passkeyAnchor.attach(this)`, and `onDestroy` calls `detach(this)`. Nothing else is created eagerly. `Application.onCreate` is untouched (AGENTS Performance).
- **Debug override:**
  - `TestSeed` gains `passkeyCapability: Boolean?`, parsed from the intent extra `cf.passkeyCapability` like `cf.reduceMotion`;
  - `AppGraph.passkeyCapability` asks the debug variant hook that carries 3a's `LaunchOverrides` (and 4e's `MockSources`) for the override, which reads `DebugSeedRouting.current?.passkeyCapability`; release has no hook, so it is always `Shipped`;
  - [verify] the value must be read when `AuthServices` builds `SignInModel`. If `auth` was built before the seed was applied, make sure 4e's seed reset rebuilds it (grep `DebugAppHooks`); say in the commit body how it is guaranteed;
  - add the extra to `plan.md` "seeds and test hooks";
  - extend `TestSeedTest` with `"1"` → true, `"true"` → true, `"0"` → false, `"x"` → null.
- **`ReleaseReadinessTest`** (`app/src/testRelease/`) gains `passkeyCapabilityIsShippedOff`:
  - `PasskeyCapability.SERVER_READY == false`;
  - `PasskeyCapability.Shipped.enabled == false`;
  - and, if the release graph can be built in that test as 4e does, its `passkeyCapability.enabled` is false.
  - Failure message: "Flip only with S1–S3 evidence (docs/passkeys.md)".
- **Tests:**
  - 5a's `SignInScreenTest.passkeyControlsAreAbsentWhileTheCapabilityIsOff` and `SignInModelTests.passkeyIsANoOpWhileTheCapabilityIsOff` still pass, now through the moved `PasskeyCapability`.
  - **`app/src/testDebug/…/auth/PasskeyGraphTest`** (Robolectric `@Logic`, debug):
    - with the `mock` source and the override on, tapping `sign-in.passkey` shows the Server banner, and `shadowOf(activity).nextStartedActivity` is `null` (no system UI);
    - the anchor holds the activity after `onCreate` and `null` after `onDestroy`.

### Task 10: Emulator checks (`test(app): check the two-factor journey and keyboards on the emulator`) [ticks "Tests"]

All three tests:
- live in `app/src/androidTest/kotlin/net/chronoflow/app/auth/`, are class-level `@Smoke`, and extend 0e's `ScreenTest`;
- are launched with `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_AUDIT_SCREEN=two-factor`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1` and `CF_MOCK_DELAY=0`;
- take < 10 s each, use bounded waits only, and never sleep.

The seed performs the mock password step (Task 6); no test types credentials.

The tests:
- **`TwoFactorJourneyTest.numberPadDoesNotAutoSubmitAndVerifyCompletesTheMockSession`** (the one journey; Swift name from `I/AppUITests/TwoFactorTests.swift:4`):
  1. `waitUntilExactlyOneExists(hasTestTag("two-factor.code"), 5_000)`.
  2. `performTextInput("1234567")` → the text is `123456`, and `RouteProbe.state.route` is still `Route.TwoFactor` (no auto-submit).
  3. Click `two-factor.submit`.
  4. Within 5 s, `RouteProbe` reports the gate route for the twofactor account (as in Task 6), and `two-factor.code` no longer exists.
- **`TwoFactorKeyboardTest.codeVariantKeepsFieldAndVerifyAboveTheKeyboard`:**
  1. Click `two-factor.code`.
  2. Wait (≤ 5 s) with the same IME-visible check as 5a's `SignInKeyboardTest` until the IME is really visible (bottom inset ≥ 100 dp). If 5a inlined it, extract it into a shared helper in this package in this commit.
  3. Wait for the reveal to settle (bounds stable for two consecutive polls), then assert, in window coordinates: the bottoms of `two-factor.code` and `two-factor.submit` are ≤ the IME top, and both tops are ≥ the status-bar bottom.
- **`TwoFactorKeyboardTest.recoveryVariantKeepsFieldAndVerifyAboveTheKeyboard`:** click `two-factor.toggle`, then `two-factor.recovery_code`, and make the same assertions. The toggle is an interaction on the seeded screen, not an earlier step.

### Task 11: Docs and plan (`docs(auth): record passkey server items and phase 5b results`) [ticks "Copy"]

- **`docs/passkeys.md`:**
  - Why the capability is false.
  - **S1:** `assetlinks.json` with `delegate_permission/common.get_login_creds` for `net.chronoflow.app.native` and the release signing SHA-256 only, never the debug key (OQ-A38).
  - **S2:** `android:apk-key-hash:<base64url(sha256(cert DER))>` in the server's passkey allowed origins, computed with:
    ```
    keytool -exportcert -alias <alias> -keystore <ks> | openssl sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '='
    ```
  - **S3:** management merged.
  - How to verify on a Play-enabled device with `cf.passkeyCapability=1` and the `api` data source.
  - The flip procedure: set `SERVER_READY = true` and update `ReleaseReadinessTest.passkeyCapabilityIsShippedOff` in one commit.
  - Do not edit `docs/app-links.md`; 5c owns it.
- **`docs/auth.md`** (5a's file): add a "Two-factor" section with:
  - the §5.1 settled-vs-board table;
  - the ported test list (Swift name → Kotlin name), including the iOS tests not ported and why (§3);
  - which content type shipped for the code field (OQ-5b-2);
  - the 18 capture names and the reviewer's board comparison.
- **`plan.md`:**
  - tick "Copy" with the note "b1 belongs to Set2FA (10c); the sign-in two-factor screen uses only catalog copy";
  - add the §10 questions to "Open questions for the user" with the next free OQ-A numbers;
  - add the `two-factor` seed value and `cf.passkeyCapability` to "seeds and test hooks".
- **`docs/perf-startup.md`, "Release size":** add the `make size` before and after from Task 3.

## 8. Acceptance criteria

1. `make quick` passes (< 60 s warm). It includes every JVM and `@Logic` test above.
2. This targeted run is green, with every JVM test < 1 s and every Robolectric test < 5 s (`build/test-results/*/timings.json`):
   ```
   make targeted ONLY="core:model:PasskeyCapabilityTest core:model:PasskeyJsonTest core:data:CredentialManagerClientMappingTest core:data:PasskeyAnchorTest feature:auth:PasskeyEncodingTests feature:auth:TwoFactorModelTests feature:auth:TwoFactorCopyTest feature:auth:SignInModelTests feature:auth:TwoFactorFieldSpecTest feature:auth:TwoFactorControlTests feature:auth:TwoFactorScreenTest feature:auth:TwoFactorSavedStateTest feature:auth:PasskeySignInTests feature:auth:SystemPasskeyAssertionProviderTest feature:auth:SignInScreenTest app:TwoFactorRoutingTest app:TwoFactorModelRoutingTests app:SignInModelRoutingTests app:AuthServicesTest app:PasskeyGraphTest app:TestSeedTest"
   ```
3. `make check` passes with `pixel` booted. It covers:
   - Lint with warnings as errors;
   - `testReleaseUnitTest`, including `passkeyCapabilityIsShippedOff`;
   - `verifyReleaseManifest`;
   - `:app:analyzeReleaseR8Config` (if 0a found it) with the credentials keep rule and no missing-class warning for `androidx.credentials.playservices`;
   - the layout tier with `TwoFactorGuardsTest` (38 cases);
   - the `@Smoke` set with `TwoFactorJourneyTest` and both `TwoFactorKeyboardTest` methods, each < 10 s.
4. `make captures ONLY=AuthTwoFactor` writes 18 PNGs. The Opus reviewer compares them with `AuthTwoFactor` (`screen=twofactor`, error none/attempts/server, light/dark, recovery through `toggleRecovery`) and with the "Two-factor code" section of `AuthStates`. Every difference is listed in §5.1 or fixed.
5. `make size` stays below the 8 MB warning (it fails above 12 MB), and both numbers are recorded.
6. No perf gate applies. `Application.onCreate` and the eager part of `AppGraph` are unchanged; `CredentialManager` is created only on the first passkey call. A debug run of the journey is StrictMode-clean.
7. `rg -n "rememberSaveable|rememberTextFieldState|SavedStateHandle" feature/auth/src/main` finds nothing that holds a code or a challenge. `rg -n "Log\.|println" feature/auth/src/main core/data/src/main/kotlin/net/chronoflow/core/data/passkey` finds nothing.
8. `git -C /Users/rickgout/Sites/chronoflow-ios status --short`, and the same for `chronoflow-new` and `chronoflow-mobile`, print nothing.
9. Every `plan.md` 5b box is ticked in its commit. The commits are atomic Conventional Commits with no trailers.

## 9. Review checklist (Opus reviewer)

**Secrets**
- [ ] The code, the recovery code and the challenge ID never reach `rememberSaveable`, `rememberTextFieldState()`, `SavedStateHandle`, a route key, a `Bundle`, the DB, DataStore or a log.
- [ ] `TwoFactorState.toString()` and `TwoFactorUiState.toString()` redact the code; `AuthNavigation.toString()` still redacts the challenge.
- [ ] `TwoFactorSavedStateTest` really scans the saved values. Ask for proof that it fails when a `rememberSaveable` is planted.
- [ ] `Route.TwoFactor` has no arguments, and the challenge lives only in `AuthNavigation`.

**Two-factor behaviour**
- [ ] `TwoFactorModel` matches `TwoFactorModel.swift` line by line:
  - the `active` guards;
  - the `nextCode != code` guard;
  - the post-call checks `navigation.challengeID == challenge` and `active`;
  - the code is cleared in `finally`;
  - `discardChallenge()` and `active = false` only after a successful accept;
  - `cancel()` clears the challenge, then leaves.
- [ ] `SignInModel` creates a fresh `TwoFactorModel` per challenge, accepts through its own `accept(…, TwoFactor, …)` (not a copy), and `clearSecrets()` cancels the model first.
- [ ] The filter is ASCII `'0'..'9'` then `take(6)`. There is no `isDigit()`, no `maxLength(6)` before filtering, and no auto-submit at six digits. The recovery code is sent untrimmed.
- [ ] The rejected-code copy appears only for `code` in the two-factor context. The inactive proof is ignored only alongside an active proof error. `challengeId` errors give the Server banner.
- [ ] Throttling shows "Too many attempts." with "Wait a few minutes before you try again." (the two-factor body). `retryAfterSeconds` is kept, there is no countdown, and Verify stays enabled.
- [ ] Back by the drawn button or system back cancels the request, clears the challenge and navigates to `SignIn`, also while busy.
  - `backToSignIn()` navigates only from `TwoFactor`, so a late `cancel()` never moves the person off `Today` or Sign in.
  - There is no `onDispose`, `ON_STOP` or `onCleared` cancel.
  - Recreation keeps the screen and the typed code.
- [ ] `TwoFactorViewModel` holds no per-visit state (it is activity-scoped, OQ-5a-5); a second visit uses the new `signIn.twoFactor` model.
- [ ] Success goes through 5a's `accept` and `SignInRouter.acceptedSession`; no session exists before the proof.
- [ ] `CancellationException` is rethrown everywhere. There is no `runCatching`, and no broad catch before the cancellation rethrow.

**Passkeys**
- [ ] With `SERVER_READY = false`, neither `sign-in.passkey` nor `sign-in.divider` exists in the semantics tree, and `submitPasskey` is a no-op. Release has no override path.
- [ ] `requestJson` is built from the parsed options (5 keys).
- [ ] The login body is built from parsed response fields:
  - unpadded base64url;
  - `id == rawId`, with mismatched input rejected;
  - extra members dropped;
  - an empty `userHandle` becomes absent.
- [ ] Cancel and `NoCredentialException` are silent. Every other failure gives the Server banner. A 422 on `credential` or `challengeId` never shows server English. Each attempt fetches new options.
- [ ] Credential Manager is created lazily and called with the Activity from the weak `PasskeyAnchor`, never with the application context. The anchor is cleared in `onDestroy`.
- [ ] The shared passkey types are in `:core:model` and `:core:data`; only the sign-in classes are in `:feature:auth`. `:core:model` stays free of `android.*`.
- [ ] `androidx.credentials` is a dependency of `:core:data` only. The keep rule exists, `analyzeReleaseR8Config` is clean, and the `make size` delta is recorded.
- [ ] With the mock source, the provider is `DeterministicAssertionProvider { throw Unavailable }`; with `api`, it is `SystemPasskeyAssertionProvider`.

**UI and copy**
- [ ] The anatomy and order follow §5.2:
  - plain sky, no dots;
  - Primary `Lg` Verify, Ghost `Lg` toggle, no footer;
  - `key(mode)` around the field;
  - the placeholder from resources, never a value.
- [ ] Keyboard options and content types follow §5.3. The IME keeps the field and Verify visible (emulator), and no size is fed back into state.
- [ ] No literal reaches `Text`, and no new string was added. Colors come only from tokens. `auth.heading` is a heading, the pane title is set, there is one polite announcement per failure, and the field error uses `semantics { error() }`.

**Tests and process**
- [ ] Ported tests keep the Swift suite and method names and all their cases; parameter rows follow the `<name><Row>` rule.
- [ ] Logic is on the JVM, layout in Robolectric, and only the IME checks and the journey on the emulator. Queries use exact tags, waits use bounded `waitUntil…`, and there are no sleeps.
- [ ] Emulator tests launch seeded; only the seed performs the mock password step.

**Typical mistakes:**
- `rememberTextFieldState()` for the code;
- `KeyboardType.NumberPassword`;
- cancelling in `onDispose` or `onCleared`;
- a `backToSignIn()` without the "only from `TwoFactor`" guard;
- per-visit state (the model, the code) stored in the activity-scoped `TwoFactorViewModel`;
- a second copy of the accept-and-route logic for two-factor;
- mapping every 422 to the 30-second copy;
- forwarding `authenticationResponseJson` raw;
- padded base64;
- constructing `CredentialManager` in the `AppGraph` constructor;
- hiding the passkey button with `alpha(0f)` instead of not composing it;
- a guard matrix that renders through the ViewModel instead of `TwoFactorContent`;
- `Thread.sleep` gates instead of `CompletableDeferred`.

## 10. Open questions (proceed on the default)

Add each one to `plan.md` with the next free OQ-A number (after the numbers 5a, 5c and 5d took).

| # | Question | Default |
|---|---|---|
| OQ-5b-1 | The plan's 5b "Copy" item says the Android-only 2FA intro b1 comes from `android-only.json`. But b1 replaces a **Settings** string shown on `Set2FA` (plan l.2491, 10c), plan l.76 lists it as iOS-only copy, and neither the `AuthTwoFactor` board nor iOS `TwoFactorView` shows an intro. | No new copy in 5b. Check that `android_two_factor_intro_b1` exists and leave its use to 10c. |
| OQ-5b-2 | Autofill hint for an authenticator code: the plan says `SmsOtpCode`, while `05` OQ A15 prefers `AUTOFILL_HINT_2FA_APP_OTP`. iOS uses `.oneTimeCode`, which covers both. | `ContentType.SmsOtpCode`. If Compose UI exposes a public way to add the `"2faAppOTPCode"` hint, combine both with `+`. Record which shipped in `docs/auth.md`. |
| OQ-5b-3 | `NoCredentialException` (no passkey for this RP on the device): silent or a banner? | Silent, like a cancel (`05` OQ A13). |
| OQ-5b-4 | `GetPublicKeyCredentialDomException` (for example `NotAllowedError` on a timeout or RP mismatch): silent or a banner? | Server banner. iOS maps every non-cancel error to `unavailable`. Revisit on the first device test. |
| OQ-5b-5 | Should the code field be auto-focused, opening the keyboard, when the screen appears? | No, for iOS parity: `TwoFactorView` sets no initial focus. |
| OQ-5b-6 | Where the shared passkey types live: the plan says `feature/auth/…/passkey/`. | `:core:model` (capability, failure, Base64URL, JSON, client interface) and `:core:data` (Credential Manager). 10d (`:feature:settings`) reuses them, and features never depend on features. The sign-in classes stay in `:feature:auth`. |
| OQ-5b-7 | iOS pushes Two-factor onto an auth navigation path; 4e/5a use single-entry root routes with no ViewModel entry decorator (OQ-5a-5, OQ-5a-7). | Follow 5a: `navigate(Route.TwoFactor)` and back → `navigate(Route.SignIn)` with `BackHandler` (no predictive cross-screen preview); the two-factor state lives in `SignInModel.twoFactor`, so the activity-scoped ViewModel is safe. |
| OQ-5b-8 | Trim or normalise recovery codes before sending them? | No. Send them as typed, as iOS does; the server decides. |
| OQ-5b-9 | Process death while the person reads the code in the authenticator app. | Cold launch; the challenge is lost and they sign in again (iOS OQ 16, A8). |
| OQ-5b-10 | A runtime passkey availability probe (provider present, RP association) on top of the constant. | None. The constant stays false until S1–S3, then failures map per §5.7. |
| OQ-5b-11 | Password autofill commit for two-factor accounts: 5a commits only after a successful password sign-in, but a 2FA account's password field is cleared when the challenge arrives, before any session exists. | No commit on the two-factor path (`commitAutofill = {}`), so the provider gets no save offer for 2FA accounts. Revisit on a device; a fix would need the password kept in memory until the two-factor success, which A23 discourages. |
