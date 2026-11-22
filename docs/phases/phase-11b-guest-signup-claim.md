# Phase 11b: Guest sign-up and claim

- **Branch:** `feat/phase-11b-guest-signup-claim`. Start it from the top of the stack once **11a** (guest wizard) and **5d** (sign-up and verify) are in it. Through them the stack also carries 9a–9g (wizard), 6a/6c (shell, `ShellNav.wizardSaved(state, tripId)` / `ShellViewModel.wizardSaved(tripId)`, `Route.TripPage(…, awaitingSync)`; *[Refresh R3]*), 9c (`WizardLauncher`, `WizardRecoveryCheck`), 8 (terms, walkthrough), 5a (`SignInModel`, `AuthServices`, `SignInRouter`), 5c (`AuthPasswordPolicy`), 4e (`SessionController`, `StoredDataReadiness`, `SessionAccountDatabase`, `AccountWiper`), 4c (mock with the registration claim **already ported**), 4b (`GuestClaimWireTests` **already ported**), 4a (`AccountDatabase`, `SettingsRecord`), 0c (strings), 0e (harness). **5b** (`PasskeySignIn`) is in the stack (*[Refresh R1]*), so the passkey half of Task 7 applies. If any other phase above is missing, stop and report it.
- **Depends on:** 11a, 5d. **Wave:** 15. **Plan entry:** `plan.md` "### Phase 11b: Guest sign-up and claim" (l.3358–3393). Also read in `plan.md`: OQ 44 (l.164), OQ-A18 (l.620, credits come only from the server), "Android spec: window policy" (l.357–371, the guest sign-up row l.362: column max 520 dp), "Android spec: seeds and test hooks" (l.466–478), "How conflicts between sources are settled" (l.167–175). *[Refresh R1]*
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa`, web at `e36368ec` and the boards at canvas version `1794774555-3fd6`. This is the first version of this brief; no earlier 11b brief existed. **Checked 2026-11-16** against the same pins: line citations, the runner's second guard (`continue`, not `return`), the guest banner rule for field-only validation, `TripWizardErrors.map` without a count, the passkey claim wiring through `AuthServices`, 11a's real counter name, the journey's `trip.scroll` wait and the keyboard test's seeds were corrected. Before you code, open the merged code of 11a, 5a–5d, 9a–9c, 6a and 4a–4e and use **their** names. Where a name here differs from the merged code, keep the merged name and list the mapping in the PR. Never rename earlier code except where a task says so.
- **Tests:** every test follows `AGENTS.md` "Tests": cheapest tier; budgets (JVM and warm Robolectric < 1 s with the AGENTS first-case allowances, emulator < 10 s; *[Refresh R6]*); no copy asserted on screen (AGENTS "Tests" 1; *[Refresh R6]*); exact tag queries; `waitUntil`; never `Thread.sleep`; hang caps (`runTest(timeout = 60.seconds)`, `timeout_msec=60000`). Tier annotations are 0e's: Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`. Ported tests keep the Swift test names. A Swift test with `arguments:` becomes a JUnit `Parameterized` class (one case per argument) whose method keeps the Swift name (the 4c precedent).
- **Tags:** **[F]** fact read in source (path and line given). **[R]** a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `KT/` = `I/Packages/ChronoFlowKit/Tests`. `W/` = `/Users/rickgout/Sites/chronoflow-new` at `e36368ec`. `DC/` = `_design/canvas/`.
- **Paths:** `…/cm/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`; `…/cw/` = `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/`; `…/cwt/` = `core/wizard/src/test/kotlin/net/chronoflow/core/wizard/`; `…/cd/` = `core/data/src/main/kotlin/net/chronoflow/core/data/`; `…/cdt/` = `core/data/src/test/kotlin/net/chronoflow/core/data/`; `…/nav/` = `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/`; `…/fa/` = `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/`; `…/fat/` = `feature/auth/src/test/kotlin/net/chronoflow/feature/auth/`; `…/fw/` = `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/appd/` = `app/src/debug/kotlin/net/chronoflow/debug/`; `…/appt/` = `app/src/test/kotlin/net/chronoflow/app/`; `…/apptd/` = `app/src/testDebug/kotlin/net/chronoflow/app/`; `…/appat/` = `app/src/androidTest/kotlin/net/chronoflow/app/`.
- **Read-only sources:** never write to, build in, run prebuild in, or change the git state of `chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile` or `_design/canvas`.

## Refresh notes (2026-11-22, against fcf08348)

Checked against the code at `fcf08348` (phases 0a–11a), iOS at `f391a1fa`, web at `e36368ec`, `plan.md` and the boards. **Where a note and the body below disagree, the note wins.** The worker repeats these checks first (`git grep` each name) and stops on any mismatch not listed here. Scope and deliverables are unchanged. Extra path aliases: `…/fwt/` = `feature/wizard/src/test/kotlin/net/chronoflow/feature/wizard/`; `…/fwdt/` = `feature/wizard/src/testDebug/kotlin/net/chronoflow/feature/wizard/`; `…/fws/` = `feature/wizard/src/debug/kotlin/net/chronoflow/feature/wizard/debug/`; `…/fat/` as above (note `…/fat/signup/`, `…/fat/testing/`).

**R1. Plan, OQ, board and research references.**
- `plan.md` "### Phase 11b" is at **l.3358–3393** (not 2593–2627). Boxes: Guest sign-up sheet l.3380, Claim l.3381, Credits l.3385, Tests l.3386. Other lines: OQ 44 l.164 ✓; **OQ-A18 l.620** (not 593); OQ-A165 l.766 (mock claim ported in 4c); "How conflicts between sources are settled" l.167–175; "Android spec: window policy" l.357–371 (guest sign-up row l.362: "Column max 560 dp centred (guest sign-up 520)"); "Android spec: seeds and test hooks" l.466–478 (the guest seed roles are l.469); "Open questions for the user" starts l.597.
- The last plan OQ is **OQ-A543** (11a's OQ-11a-11, l.1144). Task 15 appends §10 as **OQ-A544 … OQ-A556**, in order: OQ-11b-1 → A544, -2 → A545, -3 → A546, -4 → A547, -5 → A548, -6 → A549, -7 → A550, -8 → A551, -9 → A552, -10 → A553, -11 → A554, -12 → A555, -13 → A556. Each row starts "(11b OQ-11b-n)", as the 11a rows do. A further question (R10's gaps, only if a default does not work) takes **A557** and up. The body keeps citing "OQ-11b-n" by its local number; the plan rows carry both numbers.
- Boards: `_design/canvas/canvas.json` reads `"version": "1793387116-7ce3"`; the boards have not changed since `363dca19`. `GuestSignup`, `GuestSignupFailed`, `GuestSignupRetry`, `GuestSignupReview`, `GuestExcluded`, `WizardReadyGuest*` exist; the §2.2 geometry was re-read and holds. The "Review my trip" link is `href="WizardReadyChecking.dc.html"`.
- Research: `05-launch-auth.md` §12 is **l.728–766** (not 770); its §2.1 GuestExcluded row is l.90 ✓; `03-api-contract.md` §10 l.503–539; `06-wizard.md` §10 l.1053–1073.
- 5b is in the stack (`PasskeySignIn`, `…/fat/model/passkey/PasskeySignInTests.kt`), so the passkey half of Task 7 applies. The shipped `PasskeyCapability.Shipped` is disabled (`SERVER_READY = false`, `core/model/…/passkey/PasskeyCapability.kt`), so the passkey claim is proven on the JVM only.

**R2. What 11a built that 11b builds on (reuse these names).**
- **Guest draft store.** `class GuestWizardStore(file: suspend () -> File, io: CoroutineDispatcher, now: () -> Instant) : WizardDraftStore` (`…/cw/model/GuestWizardStore.kt`): `start()`, `recover()`, `recoveryDecision(handoffFinished, introFinished)`, `persist(draft)` (instance mismatch or missing file → `RecoveryRejected`; an older revision is ignored), `discard(instance)`, `didSave(…)`. One `Mutex`, every file access inside `withContext(io)`; the private `recoverRecord()`/`record()`/`write()` normalise the draft (guest mode, sliding 30-minute expiry, one-way only). `claim`/`reviewExpired` do **not** exist: Task 2 adds them with the same mutex/io discipline (iOS reads `recover()` twice around the secret read; do that through the public `recover()`, never holding the mutex across the secret read). File: `GuestRecoveryLocation.file(noBackupRoot, namespace)` → `<root>/<ns>/guest-wizard.json` (`:core:model` `…model.guest`); `AppGraph.guestWizardStore` resolves it from `storageRoot()`.
- **9a helpers (verify only).** `internal fun MemberWizardDraft.returnToReadyAfterExpiredClaim()` is already `internal` in `:core:wizard` (`…/cw/model/WizardMode.kt` l.17), so Task 2's "make it internal" is a no-op. `guestReviewExpired()` (l.93), `revisitGuestQuestions()` (l.48) and `readyTick(force = false)` (`MemberWizardCompletion.kt` l.154) are **extension functions** on `MemberWizardModel` (so `grep "fun guestReviewExpired"` does not match; grep `guestReviewExpired()`); `flush()`, `setGuestAuthenticationPresented`, `setGuestSigningIn`, `closed`, `persistenceFailed`, `guestAuthenticationPresented` are members (`MemberWizardModel.kt` l.24–297).
- **Secret store.** `interface GuestSecretStore { set(secret, token); get(token); delete(token); rotate(oldToken, newToken, secret); deleteAll() }` and `MemoryGuestSecretStore` (redacted `toString()`) in `:core:model` `net.chronoflow.core.model.guest`; `KeystoreGuestSecretStore.forNamespace(app, namespace, cipher, io)` in `:core:datastore`; `AppGraph.guestSecrets: GuestSecretStore`. `AccountWiper` takes the 4e `GuestSecrets { deleteAll() }` adapter, not a `GuestSecretStore` (`AppGraph` l.189–200 builds the adapter); Task 5's fixture builds the same adapter.
- **Guest generation.** `GuestWizardGeneration(source, secrets, deviceIdentity, parentContext)` (`…/cw/generation/GuestWizardGeneration.kt`), created by `GuestWizardDependencies.newGeneration()`.
- **Host (`…/app/guest/`).** `GuestWizardRoute(graph, revision)` builds, inside a `viewModelFactory`, `GuestWizardDependencies(store, LazyPublicDataSource { … }, secrets, deviceIdentity, presence, dispatchers, writerScope, now, signUpLauncher, catalogs, seed, uiOverride)` (`override val guest = true`) and `internal class GuestWizardHolder(dependencies, exits, scope = graph.appScope, presentation) : ViewModel()`, keyed `"guestWizardHolder:$revision"` in `RootContent`'s entry store (OQ-A227/A538; Konsist has an exact-file naming exception for it). The `TripWizardViewModel` is keyed `"guestWizard:$revision"` with `TripWizardViewModel.factory(WizardMode.Create)`. Holder: `close(model)`, `observeForeground(model, process)`, `onCleared()` (closes the foreground observer). `GuestWizardExits(navigator, revision, presence, store, secrets, dispatchers)`: `close(draft, discard)`, `onForeground(draft, discard)`, guarded by route `== Route.GuestWizard` **and** the composed revision **and** `SignedOut`; the file-level `quietly { }` helper. `GuestWizardForeground` (process-lifecycle observer). `AppGraph.hooks = GuestSessionHooks(guestWizardStore, NoopSessionHooks)`. `GuestRecoveryScheduler` routes `GuestRecoveryGate.Decision.signIn` → `Route.SignIn` (l.53); `GuestWizardDependencies.start()` resets `signingIn` to false on the next start.
- **Sign-up hand-off today.** `fun interface GuestSignUpLauncher { fun request(onDismissed: () -> Unit) }` (`…/fw/guest/GuestSignUpLauncher.kt`); `TripWizardDependencies.signUpLauncher` (default `null`) and `guest` (default `false`). The VM has **no** `requestSignUp()`: inside its `model.snapshots.collect` (`TripWizardViewModel.kt` l.420–434) it reacts to `saveOutcome == signUpRequested && !guestAuthenticationPresented` with `setGuestAuthenticationPresented(true)`, `flush()`, a bail-out on `closed || persistenceFailed`, then `signUpLauncher.request { if (viewModelScope active) { setGuestAuthenticationPresented(false); if (!closed) readyTick() } }`. There is **no route check** anywhere in that path. The binding is inline in `GuestWizardRoute` (`GuestSignUpLauncher { onDismissed -> LaunchPlatform.guestSignUpRequested(); onDismissed() }`); `LaunchPlatform.guestSignUpRequested` is `GuestWizardDebugCounters::requested` in debug and `{}` in release (`app/src/release/…/LaunchPlatform.kt` l.55; there is no perf `LaunchPlatform`). `ReleaseReadinessTest` l.36 asserts the counter class is missing and l.41 calls `LaunchPlatform.guestSignUpRequested()`.
- **Seeds.** `TestSeed` fields `guestWizard` (`CF_GUEST_WIZARD`), `guestWizardJourney` (`CF_GUEST_WIZARD_JOURNEY`), `guestRecoveryBoard` (`…/appd/TestSeed.kt` l.215–217). Debug `LaunchPlatform`: `guestWizardSeedEnabled` (either flag + a board), `guestWizardBoard`, `guestWizardInstant` (`null`, i.e. the real clock, for `WizardGuestJourney`), `guestWizardUiOverride`, `guestWizardPresentation`, `seedGuestWizard(draft, locale, now)`; `routeScreen` navigates to `Route.GuestWizard` only for `CF_GUEST_WIZARD=1` under the `screen` profile (`CF_GUEST_WIZARD_JOURNEY` alone stays on Welcome). `GuestWizardBoardSeed` (public object, `…/fws/GuestWizardBoardSeed.kt`): `names`, `draft(draft, board, locale, now)`, `presentation(board)`, `uiOverride(board)`; `GuestSignup*` → base `WizardReady` (l.45, a **frozen** draft with no server identity); `WizardGuestJourney` → base `WizardSaveJourney`, real clock, no UI override. Seed roles after 11a's review (plan l.469, `docs/wizard.md` "Phase 11a" and its review rounds 1–2): `CF_GUEST_WIZARD_JOURNEY=1` + `CF_WIZARD_BOARD_CAPTURE=WizardReadyGuest` keeps Welcome and opens **frozen** Ready after Plan my trip; `CF_GUEST_WIZARD=1` + `CF_WIZARD_BOARD_CAPTURE=WizardGuestJourney` opens **live** Sleep (future AMS → HND, shift work false) and runs the real first review and secret mint; `CF_GUEST_RECOVERY_BOARD=WizardSleep` tests recovery. Debug probe `GuestWizardDebugSecrets.hasReviewSecret(drafts, secrets, io)` (`…/appd/GuestWizardDebugSecrets.kt`, absent in release).
- **Tests 11a left.** `GuestWizardJourneyTest.testWelcomeThroughReadyRequestsSignUp` (`…/appat/guest/`: frozen `WizardReadyGuest`, Plan my trip → Ready continue → counter == 1, 5.693 s); `GuestWizardReviewIoTest.firstGuestReviewFromSleepDoesNoMainThreadIo` (live `WizardGuestJourney` → Sleep → review → age question → secret stored, 2.663 s before the probe); `GuestWizardScreenTest` is in **`…/fwdt/`** on the stateless `WizardScreen(state, onEvent)` (its `testReadyGuest` asserts `WizardEvent.Continue(WizardStepKey.Ready)`, not the counter); `TripWizardViewModelGuestSignUpTest` (`…/fwt/`); `GuestWizardHostTest`, `GuestWizardIoTest` (`…/apptd/guest/`); `GuestFixture` (`…/appt/guest/`).
- **What 11b adds** (none of it exists): `GuestClaim`, `GuestSessionContinuation`; `GuestWizardStore.claim`/`reviewExpired`; `MemberWizardStore.start(seed)`; `GuestContinuationRepository`, `GuestSessionAcceptance`, `GuestAcceptanceSuperseded` (`:core:data` `…data.guest`, a new package); `GuestSignupState`, the `SignUpModel`/`SignInModel`/`PasskeySignIn` claim parameters; `GuestSignupContent`/`GuestSignupSheet` (`…/fa/guest/`, new package); `GuestSignUpHost` and the new `GuestSignUpLauncher.request(host)`; `ShellNav.guestTrips` + `ShellViewModel.guestTrips()`; `GuestContinuationRunner`, `GuestSignupCoordinator`, `AppGraph.guestAcceptance`; the `AuthServices` parameter changes (R3); debug `GuestSignupSeed`/`GuestSignupPrefill`. 11b deletes `GuestWizardDebugCounters`, `LaunchPlatform.guestSignUpRequested` (debug and release) and their `ReleaseReadinessTest` lines.

**R3. Auth, session and claim plumbing as merged (5a–5d, 4a–4e, 10d).**
- **Contracts and API: nothing to add.** `RegisterInput` (`draftToken`, `inputRevision`, `reviewRevision`, `guestSecret`, all default `null`), `PasswordSignInInput` (`draftToken`, `guestSecret`) in `…/cm/api/AuthContracts.kt`; `ApiPasskeyLoginInput` (`draftToken`, `guestSecret`) in `PasskeyContracts.kt`; all redact in `toString()`. `TwoFactorInput(challengeId, proof)` has no claim fields. `SessionResult(token, account: SessionAccount, tripId: String? = null)` (`SessionContracts.kt`). `TripWizardErrors.map(failure, outboundCount = null)` (`…/cm/api/TripWizardErrors.kt`: `"review_expired" in fields` → `DataFailure.ReviewExpired`). `ApiDataSource` (4b) and `MockDataSource` (4c, `failNext(method, error)`, `inbox()`) already carry the claim; no `core/network` or `core/data-mock` change.
- **`SignUpModel(source, session, deviceIdentity, deviceName, accepted, onBack)`** (`…/fa/model/SignUpModel.kt`): `submit(password, confirmation, language, commitAutofill)` returns `Unit`; `RegisterInput` is built positionally with no claim arguments; the catch list is `SessionFailure`, `DataFailure`, `DeviceIdentityFailure`, `StoreFailure`, `IOException`, `GeneralSecurityException`, `RuntimeException` (each after `if (!active) return`). §5.6's "other `Exception`" means that list: never add `catch (e: Exception)`. It also has `showsPassword`/`togglePassword` (5d's member screen only).
- **`SignInModel(source, session, deviceIdentity, deviceName, passkeyCapability = Shipped, passkey = { throw … }, accepted, onTwoFactor, onLeaveTwoFactor = {})`**: `submitPassword` passes `draftToken = null, guestSecret = null` explicitly; `submitPasskey` calls `passkey()`; two-factor accepts with `AuthenticationMethod.TwoFactor`; `makeSignUpModel(onBack)` l.159; `clearSecrets()`, `editEmail()`. Add the new parameters with defaults; call sites (tests, `AuthServices`) already pass `accepted`/`onTwoFactor` by name. **`PasskeySignIn(source, provider, identity, deviceName)`** passes `draftToken = null, guestSecret = null` explicitly.
- **`AuthServices` (`…/app/auth/AuthServices.kt`) has no `graph`.** Constructor: `(navigator, appScope, dispatchers, source, session, deviceIdentity, deviceName, routeAcceptedSession: suspend () -> Unit, passkeyCapability, assertionProvider, mockMail, verificationFactory)`. Its `router.acceptedSession(result, method)` **ignores both arguments** and calls `routeAcceptedSession()`; `AppGraph.auth` (l.251–289) passes `{ readiness.routeAcceptedSession(navigate = { rootNavigator.navigate(it.toRoute()) }, verification = ::onVerify) }`. So Task 8 changes that parameter to `suspend (SessionResult, AuthenticationMethod) -> Unit` (the router forwards both), adds `claim: suspend () -> GuestClaim? = { null }` and `guestReviewExpired: suspend (GuestClaim) -> Unit = {}` parameters that `AuthServices` hands to `SignInModel` and to `PasskeySignIn` (one shared instance), and makes the `AppGraph` lambda call `guestAcceptance.routeAcceptedSession(result, method, controller, sessionAccounts, readiness, appScope, navigate = { rootNavigator.navigate(it.toRoute()) }, verification = ::onVerify)`. Update `AuthServicesTest` (`…/appt/auth/`) and `SignInModelRoutingTests` (`…/appt/auth/`) in the same commit. `VerificationCoordinator`'s `routeAccepted` (post-verification gate routing) keeps calling `readiness.routeAcceptedSession` directly: it is not an acceptance.
- **Readiness and session.** `StoredDataReadiness.routeAcceptedSession(navigate: (Gate) -> Unit, verification: suspend (Long) -> Unit)` (`core/sync/…/session/StoredDataReadiness.kt` l.169) already runs in its own `scope.async`; `Verify` → `verification(epoch)`, else `navigate(gate)` (**the caller maps `Gate.toRoute()`**). `enum class Gate { Verify, Terms, Walkthrough, App }` (`…/cm/launch/LaunchReadiness.kt`). `SessionController.activeSession(): SessionSnapshot?` and `mutationPermit(snapshot): SessionMutationPermit?` (`core/sync/…/session/SessionController.kt` l.88–96); `SessionSnapshot(session: StoredSession, epoch)`; **`StoredSession.accountId` is a `String`** (compare `AccountID.of(snapshot.session.accountId) == AccountID.of(result.account.id)`); `SessionAccountDatabase.activeAccountDatabase()` (`…/cd/session/SessionAccountDatabase.kt` l.33); `AccountDatabase.accountID: AccountID?`, `write(permit, body)`, `observe(vararg tables, fetch)` (table `"settings"`); `AccountSql.fetchSetting/saveSetting/deleteSetting` (`core/database/…/store/RecordMapping.kt` l.143–169); `SettingsRecord(section, value)` (`…/cm/store/`). `SessionGateRouter` (`core/sync/…/session/`) is unchanged.
- **§5.4's [R] check is resolved.** The 4d snapshot applier is `internal object StoreApplication` (`…/cd/store/StoreApplication.kt` l.56, used by `SqlSyncDatabase` l.100): it writes only its eight known sections plus `readiness` and never deletes other sections, so `guestContinuation` (and `wizardReminders`) survive sync. `snapshotApplicationKeepsTheRow` calls it from `:core:data` tests (same module, `TripGenerationTests` l.74 precedent).
- **Member store.** `MemberWizardStore(database, permit = null, now, io = Dispatchers.IO)` (`…/cd/wizard/MemberWizardStore.kt`); `initialDraft()` and `record(draft)` are private (reachable from `start(seed)`); `DraftRepository.replaceRecovery(record, permit)`. Run `start(seed)` inside `withContext(io)` like `start()`.
- **Sign-out.** `AppGraph.onSignedOut()` (l.492) only routes. `signOutAfterDeletingAccount(expected)` (10d, l.477) calls `controller.signOut(expected, discardPending = true)`, which wipes the account directory, so the continuation goes with it. Nothing to add.
- **Shell.** There is **no `ShellHolder`**: the retained shell owner is `ShellViewModel` (`…/app/shell/ShellViewModel.kt`) with `wizardSaved(tripId)` (l.86, delegating to the pure `ShellNav.wizardSaved(state, tripId)`, `…/nav/shell/ShellNav.kt` l.71), `wizardLauncher(deps, io)` and `closeWizard(exit)`. `WizardLauncher.open()` (`…/app/wizard/WizardLauncher.kt` l.35). `AppShell`'s `LaunchedEffect(sessionGraph?.epoch)` (`…/app/shell/AppShell.kt` l.41–45) runs `LaunchPlatform.prepareWizard(…)` then `graph.wizardRecoveryCheck.shouldOpen(epoch)`; `SessionGraph` already carries `snapshot`, `database` and `permit`. The trip page has `trip.scroll` (`TripPageScreen.kt` l.388) and handles `awaitingSync` (`TripPageViewModel.kt` l.161, l.274).
- **Design system and auth shell.** `CfModalBottomSheet(onDismissRequest, modifier, sheetState, properties, content)` (`core/designsystem/…/system/CfDialogs.kt` l.38) already applies `sheetMaxWidth = 640.dp`, `CfTheme.shapes.sheetTop` (38), `card`, the M3 drag handle and `testTagsAsResourceId`; pass the busy guard through `rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = …)` and `ModalBottomSheetProperties(shouldDismissOnBackPress = …)`. `CfBanner(title, message, modifier, actionLabel, onAction, announce, testTag, actionTestTag)` matches §5.11. `CfButton(text, onClick, …, style, size, enabled, loading, loadingDescription, …, fillWidth)` **requires `loadingDescription` when `loading`**; `CfButtonStyle.Ghost` and `CfButtonSize.Lg` exist. `CfTextField`/`CfSecureTextField(state, label, …, testTag, hint, error, enabled, keyboardOptions, onKeyboardAction, contentType)`. The column token is `PlanUITokens.wizardColumnWidth = 520f` (`CfDimens.generated.kt`). `AuthPasswordPolicy` is **`…/fa/form/AuthPasswordPolicy.kt`** (composable `hint()`). `AuthAnnouncer(revision, take)` (`…/fa/shell/AuthBanner.kt` l.43), `AuthFailureFeedback`, `AuthKeyboardReveal` are `internal` in `…/fa/shell/` (fine: the sheet is in `:feature:auth`); `UserEditReporter` is `…/fa/shell/UserEditReporter.kt`. **`AuthConsent(policyOpener, enabled, modifier)` hard-codes 5d's `auth_by_creating_an_account_you_agree_to_9e09cb`**: Task 11 adds a text-resource parameter defaulting to it (5d unchanged) and passes `R.string.auth_by_creating_an_account_you_accept_th_b18f33`. All §5.13 strings exist in `core/ui/src/main/res/values{,-nl}/strings_ios.xml` (`auth_save_your_plan`, `auth_create_an_account_to_save_it_and_ope_a14c8c`, `auth_try_again_in_a_moment`, `auth_your_account_is_ready`, `auth_try_saving_again_shortly`, `auth_your_trip_needs_a_new_check`, `auth_review_your_trip_again_before_creati_f30abe`, `auth_review_my_trip`, `auth_confirm_password`, `auth_create_account_and_save`, `auth_i_already_have_an_account`, `auth_we_couldn_t_create_your_account`).

**R4. The sign-up seam and coordinator, corrected (§5.10, §5.12, Tasks 10 and 12).**
- **Route guard.** 11a has no route check to reuse (R2). iOS resets "presented" first, then returns unless the route is still the guest wizard, then expires the review and ticks Ready. On Android the VM cannot see the root route, so the coordinator (which has `rootNavigator` and the holder's revision) evaluates `route == Route.GuestWizard && revision == <holder revision>` and passes it: `fun signUpDismissed(needsReview: Boolean, onGuestRoute: Boolean)`. The VM always resets `guestAuthenticationPresented` (while its scope is active) and, only when `onGuestRoute`, calls `guestReviewExpired()` when `needsReview` and then `readyTick()`. After "I already have an account" the entry leaves and the VM is cleared; a late `signUpDismissed` must then be a no-op (keep 11a's `viewModelScope` guard).
- **No `requestSignUp()`; `internal` is invisible to `:app`.** Task 10 extracts the inline block (l.420–434) into one private VM function called from the collector. For the `GuestSignup*` auto-present, add a one-shot flag to `WizardInitialPresentation` (already seeded per board through `GuestWizardBoardSeed.presentation(board)` → `holder.presentation`), for example `presentSignUp: Boolean = false`, which the guest VM consumes once after start by running that same function (guard, presented, flush, `request(host)`); never `savePlan()`. A public VM method is acceptable instead if it is a no-op for members.
- **Ownership and scope.** `GuestWizardRoute`'s factory builds the dependencies before the holder: build the `GuestSignupCoordinator` first (it needs `graph.auth`, `graph.rootNavigator`, the revision and the debug seed/prefill from `LaunchPlatform`), pass it as `signUpLauncher` **and** to `GuestWizardHolder`, and call `coordinator.clear()` from `GuestWizardHolder.onCleared()`. The coordinator's submit job runs in the holder's `viewModelScope` (cancelled when the entry leaves, as iOS cancels `request` on disappear); a committed session still routes (5d's `NonCancellable` acceptance + §5.5 in `appScope`). Its `presentation: StateFlow` is the one-shot: it is held in the retained holder until `dismiss`/`review`/`signInInstead` acknowledge it, so recreation redraws the sheet.
- **Deletions.** Delete `GuestWizardDebugCounters`, `LaunchPlatform.guestSignUpRequested` in **debug and release**, the inline 11a binding, and `ReleaseReadinessTest` l.36 and l.41; add the `GuestSignupSeed`/`GuestSignupPrefill` absence checks there.
- **Prefill binding.** `CF_GUEST_WIZARD_JOURNEY=1` binds `GuestSignupPrefill` whatever the board (iOS), so the claim journey (R5) and the keyboard test get it while opening through `CF_GUEST_WIZARD=1`.

**R5. Emulator tests, corrected (Task 15). This replaces the stale journey text.**
- **Why.** The body's journey ("same seeds … `CF_WIZARD_BOARD_CAPTURE=WizardGuestJourney`", "11a's steps up to `wizard.ready.continue`", "keep its '2 answered' assertion") describes 11a's pre-review journey. After 11a's review rounds 1–2 (`docs/wizard.md`), `GuestWizardJourneyTest` seeds the **frozen** `WizardReadyGuest` capture draft behind Welcome (no server-minted identity, no secret) and only taps Plan my trip → Ready continue; the live `WizardGuestJourney` seed (with `CF_GUEST_WIZARD=1`) now opens Sleep and is used by `GuestWizardReviewIoTest`. A frozen draft can never claim: `claim()` returns `null`, so the sheet goes to `Review` and sends nothing. There is also no on-screen copy assertion any more (AGENTS Tests 1).
- **`GuestWizardJourneyTest.testWelcomeThroughSignupOpensClaimedTrip`** (rename `testWelcomeThroughReadyRequestsSignUp` to the Swift name; `@Smoke`; `ScreenTest.launch` supplies `CF_AUTH_TEST_ID` and the `screen` profile). Seeds: `CF_MOCK_DELAY=0`, `cf.settings.language`/`cf.settings.theme` = `null`, `CF_GUEST_WIZARD=1`, `CF_GUEST_WIZARD_JOURNEY=1` (prefill), `CF_WIZARD_BOARD_CAPTURE=WizardGuestJourney` (live Sleep, real clock, real first review and secret mint). Where and Outbound are skipped: they are covered at cheaper tiers (`docs/wizard.md` 11a round-2 coverage table). Steps, one structural wait per action (`waitUntil` on tag + enabled/displayed): `wizard.sleep.continue` → `wizard.choice.question.18_64` (5 s) → `wizard.fine-tune.continue` (5 s) → `wizard.ready.continue` → `guest-signup.name` (2 s); then, off main through `runBlocking` on the graph's IO dispatcher, read `token = graph.guestWizardStore.recover()?.form?.identity?.token` (non-null) and require `GuestWizardDebugSecrets.hasReviewSecret(…)`; `performScrollTo()` + click `guest-signup.submit` → `verify.open-email` (5 s) → `walkthrough.skip` (5 s) → `trip.scroll` (3 s). Assert: `wizard.ready.continue` does not exist; `GuestRecoveryLocation.file(graph.storageRoot(), graph.namespace)` does not exist (under a test id the root is `filesDir/uitests/<id>/`, **not** `noBackupFilesDir`, so the body's `File(targetContext.noBackupFilesDir, "ChronoFlowMock/guest-wizard.json")` is wrong); `graph.guestSecrets.get(token) == null`; `ioViolationsForTest == 0` (11a's hook, reset before launch); the internal `< 10 s` assertion. Delete the counter assertion and the frozen `WizardReadyGuest` launch.
- **Budget.** 11a's seeded half reached the age question in 2.663 s and the old hand-off journey took 5.693 s. If the tier reports more than 10 s (wall time including setup), make the test cheaper (fewer waits, no duplicate checks); never relax the budget and never retry for timing: one run, then stop and report.
- **`GuestWizardReviewIoTest`** stays unchanged (a seeded single-screen IO check, not a second journey).
- **`GuestSignupKeyboardTest.testConfirmationStaysAboveTheKeyboard`**: its `GuestSignup` seed is the frozen Ready draft, so the IME Go submit yields `Review` and **never reaches `verify.open-email`**. Keep the seeds and the inset assertions; replace the last step with: pressing the IME action submits → wait until `auth.banner.action` exists (3 s), which only the Review state draws. < 10 s.
- **StrictMode.** Use the `ioViolationsForTest` hook in the journey; `adb logcat -d -s StrictMode` filtered for the five classes is an extra host check, not a test.
- **Plan l.469** (seed roles) is updated in Task 15: the frozen `CF_GUEST_WIZARD_JOURNEY` + `WizardReadyGuest` hand-off role is gone; `CF_GUEST_WIZARD_JOURNEY=1` binds the sign-up prefill; `CF_GUEST_WIZARD=1` + `WizardGuestJourney` opens live Sleep (journey and IO test); `CF_GUEST_WIZARD=1` + `GuestSignup*` presents the sheet over frozen Ready.

**R6. Other tests, corrected (Tasks 6, 7, 11, 12, 13, 14; §8).**
- **No copy on screen** (AGENTS Tests 1). Task 11: `failedAndRetryShowTheirBanners`, `passwordHintIsTheServerPolicy`, `consentUsesTheGuestKey` and the "no node with text Show password" check drop their text assertions: assert structure by tag (banner present/absent, `auth.banner.action` present only for Review, four field tags, no reveal control tag) and assert keys/resource ids on the JVM (`GuestSignupState.titleKey`/`messageKey`; the hint key `AuthPasswordPolicy` uses; the consent resource passed to `AuthConsent`). `GuestSignupStateTest.keysResolveInEnAndNl` is a resource lookup (not a screen) and stays. Task 12: `testGuestSignupRetry`/`Failed` assert `auth.banner` exists, not its text.
- **`GuestWizardScreenTest.testReadyGuest` is not updated**: it lives in `…/fwdt/` on the stateless `WizardScreen` and cannot see the `:app` sheet. Ready → sheet is proven by a JVM case in `GuestSignupCoordinatorTest` (a guest VM over fakes whose `savePlan` request reaches `request(host)` and publishes one presentation) plus the journey.
- **Budgets.** JVM and warm Robolectric < 1 s each (AGENTS first-case allowances 20 s / 3 s, recorded separately); emulator < 10 s. Replace every "Robolectric < 5 s" (header, Task 11, acceptance 2 and 4). `GuestWizardClaimIntegrationTests` keeps its declared < 3 s.
- **Test homes.** `SignUpModelTests` is `@RunWith(Enclosed::class)` with nested `Behavior`, `WeakPasswords`, `UnexpectedFailures` and a private `fixture()`: add the guest Swift-named tests as a nested `class Guest`. 5d has **no `SignUpScreenTest`**: its tests are `…/fat/signup/SignUpViewModelTests`, `SignUpGuardsTest`, `SignUpConsentTest`, plus `…/fat/model/VerifyEmailModelTests`. 6a's ShellNav tests are `core/navigation/src/test/…/shell/ShellNavStateTest` and `ShellNavSettingsTest`; 9c's `WizardRecoveryCheckTest` is `…/appt/wizard/`; 5b's are `PasskeySignInTests`, `TwoFactorModelTests`. Helpers: `WizardTestClock` and `CheckingTestClock` are `:core:testing` (`net.chronoflow.core.testing.wizard`), not 9a; auth fixtures are `…/fat/testing/AuthTestFixtures.kt` (`authTestSource(dispatcher)`, `FakeSessionController`, `FakeSecretField`, `RecordingRoutes`, `AuthTestFixture`); session-test precedents are `core/data/src/test/…/session/SessionSyncReadinessTests.kt` and `…/appt/auth/SignInModelRoutingTests.kt`. Robolectric graph tests use `GraphOverrides(mockDelay = Duration.ZERO, sessionCipher = SoftwareSessionCipher())` (`CF_MOCK_DELAY` is an intent seed; `SoftwareSessionCipher` is `app/src/test/…`, visible to `testDebug`). `:app` tests already see `:core:database-testing` (Task 14's JDBC database).
- **iOS screen tests.** There is no `GuestSignupScreenTests.swift` at the pin: the five cases are in `AppUITests/GuestWizardCompletionScreenTests.swift` (`testGuestSignupReady` l.46, `Failed` l.54, `Retry` l.62, `Review` l.70, `testSignInInstead` l.79).
- **Captures.** `BoardCapture.capture(rule, name)` (`core/testing-compose/…/BoardCapture.kt` l.24) captures the `isDialog()` node when one exists, i.e. the sheet window **without** the Ready backdrop. Robolectric reaches `CfModalBottomSheet` (`CfModalBottomSheetTest`, `CfModalBottomSheetGuardsTest`). Record whether the four PNGs are sheet-only (Robolectric) or full-screen (`DEVICE=1`); `make captures ONLY=` takes name prefixes.

**R7. Lessons from 9f–11a the worker carries.**
- Distinct iOS rules get distinct events or branches: sheet dismiss ≠ "Review my trip" ≠ "I already have an account"; the two `continue`/`return` guards of the runner stay distinct (§5.9).
- One-shot events live in the ViewModel (or the retained holder) until acknowledged: the VM's "presented" flag, the coordinator's `presentation`, the continuation row (consumed once, in one transaction).
- Retained objects live in a retained owner (`GuestWizardHolder`, `ShellViewModel`), never in `remember` or an unkeyed singleton. The continuation runner may run inside `AppShell`'s epoch-keyed `LaunchedEffect` because a restart is harmless (`consume` is transactional); keep no runner state in composition.
- One test scheduler per test (`StandardTestDispatcher(testScheduler)` for every dispatcher); cancel and join scopes before resetting Main. Never seed "now" with `Instant.EPOCH`: use `WizardTestClock` (1_800_000_000 s) or the seed's fixed instant.
- Spacing and sizes come from production components and tokens (`PlanUITokens`, `AuthTokens`, `CfDialogs`), never re-declared in a test host; the guards frame reuses the production values.
- Every commit leaves `make targeted` for what it touched green: a test the change breaks (`AuthServicesTest`, `TripWizardViewModelGuestSignUpTest`, `GuestWizardJourneyTest`, `ReleaseReadinessTest`) is updated in the same commit.
- Secrets: the guest secret, passwords and verification codes never go into a `Bundle`, `SavedStateHandle`, `rememberSaveable`, a route key, the database, the outbox, a log or a `toString()`; `GuestClaim.toString()` redacts; the debug prefill password lives only in `src/debug`.
- Tests must be able to fail for the real reason (the frozen-seed keyboard test above could not); emulator budgets are met by making tests cheaper; never leave the emulator windowed during device tiers (run `make emu` headless).

**R8. Perf (§8 item 10 replaced).** 11b changes the signed-in startup path (`AppShell`'s session `LaunchedEffect` gains the continuation runner; `AppGraph.guestAcceptance` and the `AuthServices` wiring) and the guest wizard path. Run **`make baseline-profile`** and commit the regenerated profiles (the generator's startup journey goes through `AppShell` → Today; 11a did the same in `a8196262`), and **`make perf`**, recording cold/warm in `docs/perf-startup.md` with device, build type and command, and checking that nothing new reads disk on main before the first frame (new `AppGraph` members `by lazy`; the runner's reads are on IO). **`make perf-timeline` does not apply** (no timeline change; `awaitingSync` exists). No new budget row.

**R9. iOS references.** Every cited path exists at `f391a1fa` and the cited ranges hold: `GuestWizardStore.swift` l.92–112, `WizardMode.swift` l.5–10/61–66, `MemberWizardStore.swift` l.164–177, `AppTabs.swift` l.341–370 (`.trips` sets `tripsPath = []`, as `ShellNav.guestTrips` does), `AuthSessionServices.swift` l.105–112/259–262/295, `NativeSessionController.swift` l.127–135, `GuestWizardDestination.swift` l.35–54/75–81/90–128 (prefill l.100–105), `GuestSignupView.swift` l.77–79/82–99/116–117, `SignUpModel.swift` l.65–121 (guest state l.109), `SignInModel.swift` l.34–37/97, `PasskeySignIn.swift` l.82–88, `GuestWizardJourneyTests.swift` l.26, the Swift test files and counts of §2.3, and `DataAccessTests/Fixtures/wizard/expired.json`.

**R10. Genuine gaps the worker must handle (raise as OQ-A557+ only if the default does not work).**
1. The route guard for `signUpDismissed` (R4): default as R4.
2. The `GuestSignup*` auto-present entry point (R4): default `WizardInitialPresentation.presentSignUp`.
3. The `AuthServices` acceptance parameter carries no result or method today (R3).
4. `AuthConsent` cannot take the guest consent key today (R3).
5. The claim journey needs a live reviewed draft and must stay < 10 s (R5).
6. The keyboard test's submit cannot reach Verify from a frozen seed (R5).
7. Robolectric captures show the sheet without its backdrop (R6).
8. `CfButton(loading = …)` requires a localized `loadingDescription`: pass the button's own label ("Create account and save"), as 5d's `SignUpScreen` passes its label (l.299–308); no new copy.

**R11. Additional mechanical mapping (worker preflight).** Android `MemberWizardDraft.airports` is a derived getter over `MemberWizardState.airports` (`MemberWizardState.kt`); it is not a constructor/copy property. Task 3 copies `seed.airports` through `state.copy(airports = seed.airports)`, preserving the existing model representation and the iOS seed behavior.

This phase is mostly **ports of small iOS types, one sheet and wiring**. The mock registration claim (iOS 18d) is **4c's and already merged** (`MockGuestRegistrationClaimTests`, `MockStaleClaimTests`, `MockClaimReplayTests`). The HTTP half of `GuestClaimAuthTests` is **4b's** (`GuestClaimWireTests`). The guest wizard model (`guestReviewExpired`, `setGuestSigningIn`, `setGuestAuthenticationPresented`) is **9a's**; the guest store, secret store and the sign-up seam are **11a's**. Do not port them again. If a rule below is missing from those phases, port it into that module with its iOS test in a separate `fix(<scope>): …` commit that names the Swift line, and report it.

---

## 1. Goal

On guest Ready, "Make my plan" opens the **GuestSignup sheet** (Name, Email, Password, Confirm password, "Create account and save", "I already have an account", consent). Registration sends the guest claim (`draft_token`, `input_revision`, `review_revision`, `guest_secret`). The server creates the account, the sign-up credit and the trip in one transaction and returns `tripId` = the draft token. The app stores a per-account **guest continuation** `Trip(id)`, deletes the guest draft and its secret, and the normal gates run: Verify → (Terms) → (Walkthrough) → app. When the app gate is reached, the shell consumes the continuation once and opens the claimed trip on My trips with `awaitingSync = true`.

The sheet has four states: **ready**, **failed** (the request failed), **retry** (the account was created but the local session could not be accepted) and **review** (the claim expired; "Review my trip" goes back to Ready, which re-checks). "I already have an account" goes to Sign in with the claim: password and passkey sign-in send `draft_token` + `guest_secret`. With no `tripId` back, a password or passkey sign-in seeds the member wizard at Checking (`returnToReady`), and a two-factor sign-in opens My trips.

Credits are never computed or hard-coded by the app: the price on Ready is the review's `price` (11a), and after the claim the account's balance comes from the server snapshot.

Done means:
- the claimed trip opens after verification (one emulator journey: guest wizard → sign up → verify through the mock inbox → walkthrough skip → the claimed trip page);
- the guest secret and the guest draft are gone after the claim (JVM integration test, plus a file check in the journey);
- the ported iOS cases of 18d (verify only), 18i, 18j and 18n pass at the cheapest tier;
- the four GuestSignup boards are guarded on Robolectric and captured.

**`GuestExcluded` is not built** (see §4 and OQ-11b-1): iOS left it unimported, it is the retired health-answers screen, and guests already use `WizardExcluded` (9e, routed by 11a).

---

## 2. Inputs to read (in this order)

### 2.1 This repo

1. `AGENTS.md` (all of it).
2. `plan.md` sections listed in the header.
3. Research: `docs/research/05-launch-auth.md` §12 (l.728–766, *[Refresh R1]*; the GuestSignup states and the claim outcomes) and §2.1 row "GuestExcluded … do not build" (l.90); `docs/research/03-api-contract.md` §10 (claim on register, login and passkey); `docs/research/06-wizard.md` §10 (guest specifics, claim outcomes).
4. Earlier briefs and their merged code: `phase-11a-guest-wizard.md` (§5.4 secret store, §5.5 `GuestWizardStore`, §5.8 `GuestSignUpLauncher` seam, §5.9 seeds incl. `GuestSignup*` → `WizardReady`, §11 hand-off); `phase-5d-sign-up-verify.md` (§3 out of scope "guest claim … 11b", §5.1–§5.4 sign-up layout, fields, consent, `SignUpModel`, §5.6 `VerifyEmailModel`, OQ-5d-10); `phase-5a-sign-in.md` (§5.4 banner and announcer, §5.7 `SignInModel`, §5.8 `AuthServices`, §5.9 `acceptedSession`); `phase-5b-two-factor-passkeys.md` (§`PasskeySignIn`, the `TwoFactorModel` accept with `AuthenticationMethod.TwoFactor`); `phase-4e-session.md` (§5.4 `StoredDataReadiness.routeAcceptedSession`, `Gate`, `SessionGateRouter`; `SessionGraph`; `mutationPermit(snapshot)`); `phase-4a-store.md` (`AccountDatabase.write/observe`, `RecordMapping` `fetchSetting`/`saveSetting`/`deleteSetting`); `phase-9b-wizard-data.md` §6.5 (`MemberWizardStore`, `initialDraft`, `DraftRepository.replaceRecovery`); `phase-9c-wizard-shell-where.md` §5.8 (`WizardLauncher`, `WizardRecoveryCheck`, the `AppShell` `LaunchedEffect`); `phase-6a-shell-trips.md` §5.6 (`ShellNav.wizardSaved`); `phase-2-design-system.md` Task 14 (`CfBanner` with action) and Task 16 (`CfModalBottomSheet`).

### 2.2 Boards (`DC/`, read-only)

| Board | What it is | State drawn |
|---|---|---|
| `GuestSignup` (big board, 375 × 812, prop `state` ∈ `ready`/`failed`/`retry`/`review`, prop `theme`) | A bottom sheet over a (superseded) `GuestReview` backdrop with a scrim | `ready`: no banner |
| `GuestSignupFailed` | wrapper `<dc-import name="GuestSignup" state="failed">` | banner "We couldn't create your account." / "Try again in a moment." |
| `GuestSignupRetry` | wrapper, `state="retry"` | banner "Your account is ready." / "Try saving again shortly." |
| `GuestSignupReview` | wrapper, `state="review"` | banner "Your trip needs a new check." / "Review your trip again before creating your account." + underlined link "Review my trip" |
| `GuestExcluded` | the retired health-answers screen ("Step 3 of 4 · Health & age", "Based on your health answers…") | **do not build** (OQ-11b-1) |

**[F] `DC/GuestSignup.dc.html`** sheet geometry: `section` `padding: 12px 20px 32px`, `gap: 16px`, `border-radius: 38px 38px 0 0`, `background: var(--card)`; grabber 36 × 5; title `font-display` 400 24/32, letter-spacing −0.01em, "Save your plan"; sub 16/24 `muted-foreground` "Create an account to save it and open it on any device."; banner (only when `state != ready`): `gap 10px; padding 12px 14px; border-radius 18px`, destructive mix fill and border, 18 px `circle-alert` in `destructive`, title 600 + body 14/20, link "Review my trip" (600, underlined, `margin-top: 6px`) only for `review`; fields `gap: 12px`: Name (`autocomplete=name`), Email (`type=email`), Password (hint "At least 10 characters.", `new-password`), Confirm password (`new-password`); buttons `display: grid; gap: 8px`: primary "Create account and save", ghost "I already have an account"; consent 12/16 `muted-foreground` centred with underlined "terms" / "privacy policy" in `foreground`. There is **no** show-password control.

Further board facts: email field `type=email autocomplete=email`; banner fill `color-mix(in oklab, var(--destructive) 10%, var(--card))`, border `1px solid color-mix(in oklab, var(--destructive) 35%, transparent)`, icon `margin-top: 1px`; scrim `oklch(0.15 0.03 272 / 0.5)`; sheet `box-shadow: var(--shadow-soft)`; the board's buttons are the web `h-10 text-sm` pills (iOS draws `AuthButton` instead, so Android uses `CfButton … Lg` as below).

**Board values iOS overrides** (iOS `plan.md:2209` OQ 47, follow iOS):
- Backdrop: the superseded `GuestReview` is not built; the sheet sits over guest Ready (`WizardReadyGuest` / `WizardReadyGuestFree`).
- Password hint: the 12-character server policy hint (`AuthPasswordPolicy.hint()`, 5c), not "At least 10 characters." (OQ-11b-2).
- "Review my trip" (board `href="WizardReadyChecking"`): dismisses the sheet, marks the guest review expired and returns to Ready; "Make my plan" then re-runs Checking.

Boards that do **not** exist (do not invent): a "Verified, no trip claimed" line (iOS 18 "Not built", WizardCopy :34); any landing screen after the claim other than the existing trip page and My trips; a dark or Dutch GuestSignup wrapper (guard dark and nl through the shared tokens; capture only the 4 light en boards).

### 2.3 iOS (`f391a1fa`) — behaviour to mirror

| Source | What | Android target |
|---|---|---|
| `I/plan.md:2062–2093` | Phase 18 decisions: claim outcomes, two-factor → My trips, "Not built" | §1, §5 |
| `I/plan.md:2115–2123` (18d) | mock registration claim | **4c; verify only** (§3) |
| `I/plan.md:2158–2164` (18i) | claim on sign-up, sign-in and passkey | Tasks 6–8 |
| `I/plan.md:2165–2171` (18j) | accepting a session with or without a trip | Tasks 3–5, 8–9 |
| `I/plan.md:2192–2199` (18n) | integration and acceptance (journey; Valet claim gate is user-run) | Tasks 14–15 |
| `I/plan.md:2201–2210` | "Open (no board)" and OQ 47 (GuestSignup resolved, GuestExcluded unimported, 12-char hint) | §2.2, OQ-11b-1/2 |
| `Kit/AppCore/GuestClaim.swift` (13 lines) | `GuestClaim(draftToken, inputRevision, reviewRevision, guestSecret)` | Task 1 |
| `Kit/AppCore/GuestSessionContinuation.swift` (7 lines) | `AuthenticationMethod`; `GuestSessionContinuation { trip(String), memberWizard, trips }` | Task 1 (the enum; `AuthenticationMethod` exists from 5a) |
| `Kit/TripWizard/Model/GuestWizardStore.swift:92–112` | `claim(secrets:)`, `reviewExpired(claim:)` | Task 2 |
| `Kit/TripWizard/Model/WizardMode.swift:5–10, 61–66` | `returnToReadyAfterExpiredClaim()`, `guestReviewExpired()` | 9a (verify), used in Task 2 |
| `Kit/TripWizard/Model/MemberWizardStore.swift:164–177` | `start(seed:)` | Task 3 |
| `Kit/LocalStore/GuestContinuationRepository.swift` (35 lines) | settings section `guestContinuation`; `store`, `values`, `consume(gate:)` | Task 4 |
| `Kit/TripWizard/Model/GuestSessionAcceptance.swift` (54 lines) | `routeAcceptedSession`, `accept` | Task 5 |
| `Kit/Auth/SignUpModel.swift:6–132`, `Kit/Auth/GuestSignupState.swift` (21 lines) | guest claim, `Outcome`, `guestState`, `requiresGuestClaim` | Task 6 |
| `Kit/Auth/SignInModel.swift:15–54, 72–81, 97–123`, `Kit/Auth/PasskeySignIn.swift:58–90` | `claim` / `guestReviewExpired` providers; password and passkey claim fields | Task 7 |
| `I/App/Session/AuthSessionServices.swift:105–112`, `I/App/Session/NativeSessionController.swift:127–135` | wiring the claim provider and the acceptance | Task 8 |
| `I/App/AppTabs.swift:341–370` (the `.task(id: wizardStore != nil)` block; the plan cites l.341–369) | consuming the continuation at the app gate | Task 9 |
| `I/App/GuestWizardDestination.swift:35–54, 75–81, 90–128` | sheet presentation, sign-in instead, review, dismissal, debug seeds and prefill | Tasks 10, 12 |
| `Kit/Auth/GuestSignupView.swift` (126 lines) | sheet content, tags, announcements, busy rules | Task 11 |
| `KT/TripWizardTests/GuestClaimStoreTests.swift` (2), `GuestSessionAcceptanceTests.swift` (7 tests, 3 with arguments), `GuestWizardClaimIntegrationTests.swift` (1 with en/nl); `KT/AuthTests/SignUpModelTests.swift:8–57` (3 guest tests); `KT/DataAccessTests/GuestClaimAuthTests.swift` (2 with arguments, model half) | tests to port | Tasks 2–7, 14 |
| `I/AppUITests/GuestWizardCompletionScreenTests.swift:13–23, 46–87` (`testReadyGuest`; the five `testGuestSignup*`/`testSignInInstead` cases, l.46–87; there is no `GuestSignupScreenTests.swift`, *[Refresh R6]*), `I/AppUITests/GuestWizardJourneyTests.swift` | screen and journey tests | Tasks 12, 15 |

### 2.4 API (web `e36368ec`; client already in 4b, mock in 4c; never called in tests)

| Call (`DataSource`) | Endpoint | Auth, timeout | Claim fields | Success | Claim failures |
|---|---|---|---|---|---|
| `signUp(RegisterInput)` | `POST /v1/auth/register` | public, **90 s** | `draft_token`, `input_revision` (Int), `review_revision` (Int), `guest_secret` — all four, or none (absent, not null) | 200 `SessionResult {kind:"session", token, account (hasVerifiedEmail false), tripId}`; `tripId` = the draft token when claimed | 422 `review_expired` (alone, or inside `validation_failed` as field `review_expired`: "Review your trip again before creating your account."). The claim is checked before anything is created; an expired claim creates nothing. A retry with the same email, password and claim within 7 days returns the same account and `tripId` (receipt `api_guest_claims`). |
| `signIn(PasswordSignInInput)` | `POST /v1/auth/login` | public, 15 s | `draft_token`, `guest_secret` | `tripId` only when a live receipt from an earlier registration matches **this** user | none (a mismatch just returns `tripId: null`) |
| `passkeyLogin(ApiPasskeyLoginInput)` | `POST /v1/auth/passkey/login` | public, 15 s | `draft_token`, `guest_secret` | as login | as login |
| two-factor completion | `POST /v1/auth/two-factor` | public, 15 s | **no** claim fields | `tripId` always null | — |

Sources: `docs/research/03-api-contract.md` §10; 4b brief table rows 2, 3, 5; `W/app/Actions/…/RegisterMobile.php:27–55`, `ResolveMobileGuestClaim.php:12–17` (cited in iOS 18). Trip reads are `verified`, so the claimed trip can be fetched only after email verification. Credits: `W/app/Planning/Credits/CreditLedger.php:16` `SIGNUP_CREDITS = 1` (the mock grants 3, iOS 18c); the app never computes a balance.

---

## 3. Already delivered by earlier phases (verify; do not rebuild)

Run these greps first. Each must find the named thing in the merged code. If one fails, stop and report it.

| Thing | Owner | Grep |
|---|---|---|
| `GuestSecretStore`, `MemoryGuestSecretStore` (`net.chronoflow.core.model.guest`) | 11a | `grep -rn "interface GuestSecretStore" core/model/src/main` |
| `GuestWizardStore` (`start`, `recover`, `persist`, `discard`) in `:core:wizard` | 11a | `grep -rn "class GuestWizardStore" core/` |
| `GuestSignUpLauncher` (`request(onDismissed)`), `TripWizardDependencies.signUpLauncher`, the VM's inline `signUpRequested` handling (`TripWizardViewModel.kt` l.420–434; no `requestSignUp()`, *[Refresh R2]*) | 11a | `grep -rn "GuestSignUpLauncher" feature/wizard/src/main app/src/main` |
| `GuestWizardHolder`, `GuestWizardRoute`, `GuestWizardBoardSeed` (`GuestSignup*` → `WizardReady`) | 11a | `grep -rn "class GuestWizardHolder\|GuestSignup" app/src feature/wizard/src/debug` |
| `guestReviewExpired()`, `readyTick` (extensions), `setGuestSigningIn`, `setGuestAuthenticationPresented`, `flush` (members), and the `internal` draft helper `returnToReadyAfterExpiredClaim` (*[Refresh R2]*) | 9a | `grep -rn "guestReviewExpired()\|fun setGuestSigningIn\|returnToReadyAfterExpiredClaim" core/wizard/src/main` |
| `RegisterInput`, `PasswordSignInInput`, `ApiPasskeyLoginInput` with the optional claim fields | 0d | `grep -rn "draftToken" core/model/src/main` |
| `GuestClaimWireTests.requestUsesOnlyTheClaimFieldsForItsMethod` (6 cases) | 4b | `grep -rln "GuestClaimWireTests" core/network/src/test` |
| Mock registration claim + `MockGuestRegistrationClaimTests`, `MockStaleClaimTests` (10), `MockClaimReplayTests` (6) | 4c | `grep -rln "MockStaleClaimTests\|MockClaimReplayTests" core/data-mock/src/test` |
| `SignUpModel`, `SignUpViewModel`, `SignInModel.makeSignUpModel(onBack)`, `AuthConsent`, `PolicyOpener`, `VerifyEmailModel` (mock mail on `verify.open-email`) | 5d | `grep -rn "fun makeSignUpModel" feature/auth/src/main` |
| `SignInModel(…, accepted, onTwoFactor)`, `AuthenticationMethod`, `AuthServices`, `SignInRouter.acceptedSession` | 5a | `grep -rn "enum class AuthenticationMethod" core/model/src/main` |
| `PasskeySignIn` | 5b | `grep -rn "class PasskeySignIn" feature/auth/src/main` |
| `AuthPasswordPolicy.hint()` (`…/fa/form/`, *[Refresh R3]*) | 5c/5d | `grep -rn "object AuthPasswordPolicy" feature/auth/src/main` |
| `StoredDataReadiness.routeAcceptedSession`, `gate()`, `Gate`, `SessionController.mutationPermit(snapshot)`, `SessionAccountDatabase.activeAccountDatabase()` | 4e | `grep -rn "fun routeAcceptedSession\|fun mutationPermit\|fun activeAccountDatabase" core/` |
| `MemberWizardStore(database, permit, now)`, `initialDraft()`, `DraftRepository.replaceRecovery` | 9b | `grep -rn "class MemberWizardStore" core/data/src/main` |
| `ShellNav.wizardSaved(state, tripId)`, `ShellViewModel` (there is no `ShellHolder`, *[Refresh R3]*), `WizardLauncher.open()`, `WizardRecoveryCheck` | 6a, 9c | `grep -rn "fun wizardSaved\|class WizardLauncher" core/navigation app/src/main` |
| `CfBanner(… actionLabel, onAction, actionTestTag)`, `CfModalBottomSheet`, `CfTextField`, `CfSecureTextField`, `CfButton` | 2 | `grep -rn "fun CfBanner\|fun CfModalBottomSheet" core/` |

Then run, unchanged and green: `make targeted ONLY="core:data-mock:MockGuestRegistrationClaimTests core:data-mock:MockStaleClaimTests core:data-mock:MockClaimReplayTests core:network:GuestClaimWireTests"`. This is the proof for plan 18d; tick nothing for it.

---

## 4. Out of scope

| Item | Owner / reason |
|---|---|
| `GuestExcluded` | **Not built** (iOS OQ 47, `05` l.90): retired by "no health data". Guests reach `WizardExcluded` (9e, exits routed in 11a). Record it in plan.md (Task 15). |
| The mock registration claim and the HTTP wire test | 4c, 4b (§3) |
| Guest wizard screens, guest Ready rows and price lines, the secret store, the guest draft file, quiet recovery | 11a |
| A "Verified, no trip claimed" line, any landing screen beyond the trip page and My trips | no board (iOS 18 "Not built") |
| A show-password control in the guest sheet | none on the board or in iOS `GuestSignupView` (OQ-11b-9) |
| Running the iOS "Valet claim gate" against a local backend | user-run (iOS 18n "Open: user"); record it as open (OQ-11b-12) |
| Billing, a purchase path, any client-side credit arithmetic | never (A14, OQ-A18) |

---

## 5. Behaviour spec

### 5.1 Types (`:core:model`)

- `data class GuestClaim(val draftToken: String, val inputRevision: Int, val reviewRevision: Int, val guestSecret: String)` in `net.chronoflow.core.model.guest` (`…/cm/guest/GuestClaim.kt`) [F `GuestClaim.swift`]. `toString()` prints `GuestClaim(draftToken=<token>, inputRevision=<n>, reviewRevision=<n>, guestSecret=***)`.
- `sealed interface GuestSessionContinuation { data class Trip(val tripId: String); data object MemberWizard; data object Trips }` in `net.chronoflow.core.model.auth`, next to 5a's `AuthenticationMethod` [F `GuestSessionContinuation.swift:3–7`].
- **Stored form** [R; OQ-11b-7]: a JSON object written as the `value` of the settings row `section = "guestContinuation"`: `{"kind":"trip","tripId":"<id>"}`, `{"kind":"memberWizard"}`, `{"kind":"trips"}`. `GuestSessionContinuation.encode(): String` and `companion fun decode(json: String): GuestSessionContinuation?` (unknown `kind`, a missing or blank `tripId`, or invalid JSON → `null`). Use `ApiJson` (no second `Json` configuration).

### 5.2 Claim from the guest draft (`GuestWizardStore`, port `GuestWizardStore.swift:92–112`)

Add to 11a's `…/cw/model/GuestWizardStore.kt`:

```kotlin
suspend fun claim(secrets: GuestSecretStore): GuestClaim?
suspend fun reviewExpired(claim: GuestClaim)
```

- **`claim`** returns `null` (never throws, except `CancellationException`) unless all hold, in this order:
  1. `draft = recover()` is non-null (any non-cancellation exception → `null`);
  2. `identity = draft.form.identity` non-null, `identity.token` non-empty, `identity.reviewRevision` non-null;
  3. `secret = secrets.get(identity.token)` non-null and non-empty (exception → `null`);
  4. `current = recover()` non-null, `current.state.instance == draft.state.instance` and `current.form.identity == identity` (re-read after the suspending secret read, so a discard or a new review in between yields `null`).
  Then `GuestClaim(identity.token, identity.inputRevision, identity.reviewRevision, secret)`.
- **`reviewExpired(claim)`**: `draft = recover() ?: return`; return unless `draft.form.identity?.token == claim.draftToken`, `inputRevision == claim.inputRevision` and `reviewRevision == claim.reviewRevision`. Then apply 9a's `returnToReadyAfterExpiredClaim()` (`returnToReady = true`, `checkingAnswers = null`, `reviewedAt = null`, `step = ready`), `revision + 1`, `persist(draft)`. It throws what `persist` throws.
- `recover()` keeps its sliding-expiry rule (11a): a claim on a draft ≥ 30 min old is `null`.

### 5.3 Member wizard from a guest seed (`MemberWizardStore.start(seed)`, port `MemberWizardStore.swift:164–177`)

Add to 9b's `…/cd/wizard/MemberWizardStore.kt`:

```kotlin
suspend fun start(seed: MemberWizardDraft): MemberWizardDraft
```
`draft = initialDraft()` (9b, so the member's `savedProfile`, `sleepNeeded`, `firstTrip` come from the account), then a copy with: `form = seed.form` with `identity = null` and `override = WizardSleepOverride()` (the default); `state.airports = seed.airports` (Android derived draft getter; R11); `state.entered = true`; `state.fromLastTrip = false`; `state.step = checking`; `state.returnToReady = true`; `state.profile = JsonObject(emptyMap())`. `mode` stays `member` (from `initialDraft`). `drafts.replaceRecovery(record(draft), permit)`, return `draft`. It replaces any existing member recovery row (a stale writer of the old row then gets `RecoveryRejected`).

### 5.4 Guest continuation repository (port `GuestContinuationRepository.swift`)

`…/cd/guest/GuestContinuationRepository.kt` (package `net.chronoflow.core.data.guest`):

```kotlin
class GuestContinuationRepository(private val database: AccountDatabase, private val permit: SessionMutationPermit? = null) {
    suspend fun store(destination: GuestSessionContinuation)                 // write(permit) { saveSetting(SettingsRecord("guestContinuation", destination.encode())) }
    fun values(): Flow<GuestSessionContinuation?>                           // database.observe("settings") { fetchSetting("guestContinuation")?.value?.let(GuestSessionContinuation::decode) }
    suspend fun consume(gate: Gate): GuestSessionContinuation?              // gate != App → null (row kept); else in ONE write(permit): read, deleteSetting, return the decoded value
}
```
- `consume` deletes the row even when it decodes to `null` (a corrupt row is dropped once, not retried forever) [R].
- The row lives in the account database, so 4e's sign-out with `discardPending = true` (which wipes the account directory) erases it, and a plain close and reopen keeps it.
- [R] Verify that 4d's account-snapshot application does not delete settings sections it does not know (`guestContinuation`, `wizardReminders`). If it does, stop and report: the continuation would be lost on the first sync. *[Refresh R3: verified; `StoreApplication.snapshot` writes only its known sections and deletes only `readiness`.]*

### 5.5 Session acceptance (port `GuestSessionAcceptance.swift`)

`…/cd/guest/GuestSessionAcceptance.kt`:

```kotlin
class GuestSessionAcceptance(
    private val guest: GuestWizardStore,
    private val secrets: GuestSecretStore,
    private val now: () -> Instant,                     // for MemberWizardStore
) {
    suspend fun accept(result: SessionResult, method: AuthenticationMethod, database: AccountDatabase, permit: SessionMutationPermit? = null)
    suspend fun routeAcceptedSession(
        result: SessionResult, method: AuthenticationMethod,
        controller: SessionController, accounts: SessionAccountDatabase, readiness: StoredDataReadiness,
        scope: CoroutineScope,                           // AppGraph.appScope
        navigate: (Gate) -> Unit,                         // 4e's type; the caller maps Gate.toRoute() [Refresh R3]
        verification: suspend (Long) -> Unit,
    )
}
class GuestAcceptanceSuperseded : CancellationException("guest acceptance superseded")
```

**`accept`** [F l.29–53], in this order:
1. `database.accountID` must equal `AccountID.of(result.account.id)` (0d's case-insensitive id), else throw `GuestAcceptanceSuperseded` (iOS `CancellationError`; OQ-11b-6). Nothing is read or written for another account.
2. `draft = guest.recover()`.
3. `continuation = GuestContinuationRepository(database, permit)`.
4. If `result.tripId != null`: `continuation.store(Trip(tripId))`, then `secrets.delete(tripId)` (the trip id **is** the draft token).
   Else if `draft != null`, by `method`:
   - `Password`, `Passkey` → `MemberWizardStore(database, permit, now).start(seed = draft)`, then `continuation.store(MemberWizard)`;
   - `TwoFactor` → `continuation.store(Trips)` (no seeding: the unregistered draft is dropped, iOS user decision 2026-11-10);
   - `SignUp` → **return** at once (draft and secret are kept; a sign-up without a claimed trip never consumes the guest).
5. If `draft != null`: `draft.form.identity?.token?.let { secrets.delete(it) }`, then `guest.discard(draft.state.instance)`.

The continuation is always written **before** the guest data is deleted, so a crash in between leaves a recoverable state.

**`routeAcceptedSession`** [F l.13–27]: runs its body as `scope.async { … }.await()`, so a cancelled caller does not stop it (iOS runs it in an unstructured `Task`; test `committedSessionFinishesContinuationAfterRequestCancellation`). Body:
1. `snapshot = controller.activeSession()`; require `snapshot != null`, the snapshot's account id equal to `result.account.id` (`StoredSession.accountId` is a `String`: compare `AccountID.of(…)` on both sides; iOS compares the raw strings; *[Refresh R3]*) and `snapshot.session.token == result.token`; `database = accounts.activeAccountDatabase()` non-null; `permit = controller.mutationPermit(snapshot)` non-null. Any failure → throw `GuestAcceptanceSuperseded`.
2. `accept(result, method, database, permit)`.
3. If `controller.activeSession() != snapshot` → return.
4. `readiness.routeAcceptedSession(navigate, verification)` (4e: `Verify` → `verification(epoch)`, else `navigate(gate)`; the `AppGraph` lambda maps `toRoute()`).

### 5.6 Sign-up with the claim (`SignUpModel`, port `SignUpModel.swift:65–121`; refactor of 5d)

`…/fa/model/GuestSignupState.kt` [F `GuestSignupState.swift`]:

| State | Title key (Auth) | Message key (Auth) | nl title / message |
|---|---|---|---|
| `Ready` | — | — | — |
| `Failed` | "We couldn't create your account." | "Try again in a moment." | "We konden je account niet aanmaken." / "Probeer het zo meteen opnieuw." |
| `Retry` | "Your account is ready." | "Try saving again shortly." | "Je account is klaar." / "Probeer het opslaan zo opnieuw." |
| `Review` | "Your trip needs a new check." | "Review your trip again before creating your account." | "Je reis moet opnieuw worden gecontroleerd." / "Bekijk je reis opnieuw voordat je je account maakt." |

`enum class GuestSignupState { Ready, Failed, Retry, Review; val titleKey: String?; val messageKey: String? }` with the keys above (resolved through 5a's `AuthText`/`CopyIndex.find(CopyTable.Auth, key)`).

Changes to 5d's `SignUpModel` (constructor parameters with defaults, so 5d's call sites compile unchanged):
- `claim: suspend () -> GuestClaim? = { null }`, `guestReviewExpired: suspend (GuestClaim) -> Unit = {}`, `initialGuestState: GuestSignupState = Ready` (OQ-11b-10).
- `val guestState: StateFlow<GuestSignupState>`; `enum class Outcome { Accepted, GuestReviewExpired }`.
- `suspend fun submit(password, confirmation, language, commitAutofill, requiresGuestClaim: Boolean = false): Outcome?`:
  1. `if (!active || !form.begin(Register)) return null`. `sessionFailure = null`; `guestState = Ready`.
  2. `try`: `id = deviceIdentity()`; `claim = claim()`; if `requiresGuestClaim && claim == null` → `guestState = Review`, return `GuestReviewExpired` (no request is sent, no account is created). `attemptedClaim = claim`. `ensureActive()`; `if (!active) return null`.
  3. `result = source.signUp(RegisterInput(…5d fields…, draftToken = claim?.draftToken, inputRevision = claim?.inputRevision, reviewRevision = claim?.reviewRevision, guestSecret = claim?.guestSecret))`. With `claim == null` the four keys are absent (0d encoder; 5d's assertion stays true). `ensureActive()`; `if (!active) return null`.
  4. `accountCreated = true` (set **before** acceptance).
  5. 5d's `withContext(NonCancellable) { session.accept(result); commitAutofill(); password.clear(); confirmation.clear(); accepted(result, SignUp) }`; return `Accepted`.
  6. `catch (e: CancellationException)` → rethrow. Every other catch first does `if (!active) return null`. Then:
     - `attemptedClaim != null && e is DataFailure && TripWizardErrors.map(e) == DataFailure.ReviewExpired` (4b's `map(failure, outboundCount: Int? = null)` in `:core:model` `…model.api`, called without a count exactly as iOS `TripWizardErrors.map(failure)`) → `guestReviewExpired(attemptedClaim)`, `guestState = Review`, return `GuestReviewExpired`. No banner (`form.banner` stays `null`).
     - else 5d's handling (`SessionFailure` → `sessionFailure = e; form.receive(Server(null))`; `DataFailure` → `form.receive(e)`; other `Exception` → `form.receive(Server(null))`), then `guestState = if (accountCreated) Retry else if (form.state.value.banner == null) Ready else Failed`; return `null`.
  7. `finally`: clear both secrets; `form.finish()`.
- `SignInModel.makeSignUpModel(onBack, initialGuestState = Ready)` passes SignInModel's `claim` and `guestReviewExpired` (Task 7) into every sign-up model, as iOS does: the member Sign up screen (5d) also sends a live claim (OQ-11b-5).
- 5d's `SignUpViewModel` returns nothing new; it ignores the outcome.

### 5.7 Claim on password and passkey sign-in (port `SignInModel.swift:97–123`, `PasskeySignIn.swift:79–90`)

- `SignInModel` gains `claim: suspend () -> GuestClaim? = { null }` and `guestReviewExpired: suspend (GuestClaim) -> Unit = {}` (5a constructor, defaults keep 5a/5b tests compiling).
- `submitPassword` step 2: `id = deviceIdentity()`; `claim = claim()`; `ensureActive()`; `PasswordSignInInput(…, draftToken = claim?.draftToken, guestSecret = claim?.guestSecret)`. Nothing else changes: a login never fails on a claim.
- `PasskeySignIn` gains `claim: suspend () -> GuestClaim? = { null }` (last constructor parameter). 5b's steps 7–8 (`ensureActive()`, then `passkeyLogin`) become: `claim = claim()` **after** the assertion; `ensureActive()`; then `ApiPasskeyLoginInput(…, draftToken = claim?.draftToken, guestSecret = claim?.guestSecret)` [F `PasskeySignIn.swift:82–88`]. On Android `SignInModel` does not build `PasskeySignIn` (5b: `AuthServices` passes `PasskeySignIn(…)::signIn` as `passkey`), so `AuthServices` passes the **same** provider instance to both `SignInModel(claim = …)` and `PasskeySignIn(claim = …)` (iOS builds `PasskeySignIn` inside `SignInModel.init` with SignInModel's `claim`, `SignInModel.swift:36–37`).
- `TwoFactorModel` and its input are unchanged (no claim fields).

### 5.8 Wiring (`:app`, port `AuthSessionServices.swift:105–112`, `NativeSessionController.swift:127–135`)

- `AppGraph.guestAcceptance: GuestSessionAcceptance` (lazy; `guestWizardStore`, `guestSecrets`, the app clock).
- *[Refresh R3: `AuthServices` has no `graph`; the providers and the acceptance arrive as new `AuthServices` constructor parameters built by `AppGraph.auth`, and the `routeAcceptedSession` parameter becomes `suspend (SessionResult, AuthenticationMethod) -> Unit`.]* `AuthServices` builds one `val claim: suspend () -> GuestClaim? = { graph.guestWizardStore.claim(graph.guestSecrets) }` and passes it to `SignInModel(…, claim = claim, guestReviewExpired = { c -> try { graph.guestWizardStore.reviewExpired(c) } catch (e: CancellationException) { throw e } catch (e: IOException) {} catch (e: SerializationException) {} catch (e: RecoveryRejected) {} }, passkey = PasskeySignIn(source, assertionProvider, identity = deviceIdentity, deviceName = deviceName, claim = claim)::signIn)` (iOS `try?`; 5b's `passkey` argument keeps its other parameters).
- `SignInRouter.acceptedSession(result, method)` (5a; today it ignores both and calls the `routeAcceptedSession` lambda, *[Refresh R3]*) now calls `graph.guestAcceptance.routeAcceptedSession(result, method, controller, sessionAccounts, readiness, scope = appScope, navigate = …, verification = …)` with exactly the `navigate` and `verification` arguments 5a passes today, instead of calling `readiness.routeAcceptedSession` directly. Sign-up (5d), password, passkey and two-factor all reach it through `accepted`.
- `claim()` and every store call run on `dispatchers.io` inside the stores (11a rule); nothing here touches disk on main.

### 5.9 Consuming the continuation in the shell (port `AppTabs.swift:341–370`)

`GuestContinuationRunner` (`…/app/guest/GuestContinuationRunner.kt`, JVM-testable):

```kotlin
class GuestContinuationRunner(
    private val repository: GuestContinuationRepository,
    private val gate: suspend () -> Gate,                 // readiness.gate()
    private val isCurrent: suspend () -> Boolean,         // controller.activeSession() == the shell's snapshot
    private val onTrip: (String) -> Unit,                 // ShellViewModel.wizardSaved(id) [Refresh R3]
    private val onTrips: () -> Unit,                      // ShellViewModel.guestTrips() → ShellNav.guestTrips(state)
    private val onMemberWizard: () -> Unit,               // WizardLauncher.open()
) { suspend fun run() }
```
- `run()` collects `repository.values()`: for each value, **return** if `!isCurrent()`; skip `null`; `g = gate()`; **skip this value** (not return) if `!isCurrent()`; `destination = repository.consume(g) ?: continue` [F `AppTabs.swift:349–354`: the first guard is `else { return }`, the second `else { continue }`]; then `Trip(id)` → `onTrip(id)`; `Trips` → `onTrips()`; `MemberWizard` → `onMemberWizard()`. `CancellationException` rethrows; any other exception ends `run()` silently (iOS `assertionFailure`).
- `ShellNav.guestTrips(state)` (new pure reducer in `…/nav/shell/ShellNav.kt`, plus `ShellViewModel.guestTrips()`; *[Refresh R3]*): `overlay = null`, `selected = Trips`, Trips stack `[Trips]`. `Trip(id)` reuses `wizardSaved(id)` (`overlay = null`, `selected = Trips`, Trips stack `[Trips, TripPage(id, awaitingSync = true)]`), which is exactly iOS `wizard = nil; selection = .trips; tripsPath = [TripPageRoute(tripID: id, awaitingSync: true)]`.
- **Start:** in `AppShell`'s `LaunchedEffect(sessionGraph?.epoch)` (`…/app/shell/AppShell.kt` l.41–45), after the existing `if (graph.wizardRecoveryCheck.shouldOpen(epoch)) launcher.open()`, build the runner from the current `SessionGraph` (`database`, `permit`, `snapshot`; *[Refresh R3]*) and call `run()` (iOS keys it on `.task(id: wizardStore != nil)`, i.e. it starts once the shell has its wizard store, `AppTabs.swift:296–300`; on Android that is after 9c's recovery check). `AppShell` exists only on app routes, so `gate()` is `App` there in practice; keep the check anyway.
- The trip page (6c) handles `awaitingSync = true`: Loading until sync delivers the trip, no NotFound.

### 5.10 The sheet: launcher, coordinator and dismissal (port `GuestWizardDestination.swift:35–54, 90–128`)

**Seam change** (refactor of 11a §5.8) in `…/fw/guest/GuestSignUpLauncher.kt`:
```kotlin
interface GuestSignUpHost {
    fun signUpDismissed(needsReview: Boolean)  // setGuestAuthenticationPresented(false); if the route is still the guest wizard: if (needsReview) guestReviewExpired(); readyTick()
    suspend fun prepareSignIn()                 // setGuestSigningIn(true); flush()
    fun reviewExpired()                         // guestReviewExpired()
}
fun interface GuestSignUpLauncher { fun request(host: GuestSignUpHost) }
```
*[Refresh R4: 11a makes no route check; the coordinator evaluates it and passes `signUpDismissed(needsReview, onGuestRoute)`.]* The guest `TripWizardViewModel` implements `GuestSignUpHost` (route check: the same "route is still `Route.GuestWizard`" check 11a's `onDismissed` already makes; keep its merged name and shape). The VM's guard and flush before `request` stay as 11a built them (`!guestAuthenticationPresented`, `setGuestAuthenticationPresented(true)`, `flush()`, not closed, not `persistenceFailed`).

**`GuestSignupCoordinator`** (`…/app/guest/GuestSignupCoordinator.kt`, owned by 11a's `GuestWizardHolder`, cleared with it; implements `GuestSignUpLauncher`; built before the dependencies in `GuestWizardRoute`'s factory and run in the holder's `viewModelScope`, *[Refresh R4]*):
- State: `val presentation: StateFlow<GuestSignupPresentation?>`, `data class GuestSignupPresentation(val id: Long, val model: SignUpModel, val host: GuestSignUpHost, val prefill: GuestSignupPrefill?)`; private `request: Job?`.
- `request(host)`: return if `presentation.value != null`. `model = auth.signIn.makeSignUpModel(onBack = {}, initialGuestState = seed?.state ?: Ready)`. If a debug prefill is bound: `model.editName(prefill.name)`, `model.editEmail(prefill.email)`. Publish the presentation (`id` = a counter).
- `submit(password, confirmation, language, commitAutofill)`: return if `request != null` or busy or `model.guestState.value == Review`; `request = scope.launch { try { model.submit(…, requiresGuestClaim = true) } finally { request = null } }` (iOS `GuestSignupView.submit`).
- `dismiss()` (sheet `onDismissRequest`, system back): ignored while `model.form.busy` (iOS `interactiveDismissDisabled(busy)`). Else: `needsReview = model.guestState.value == Review`; `request?.cancel()`; `model.cancel()`; `presentation = null`; `host.signUpDismissed(needsReview)`.
- `review()` (banner action): `host.reviewExpired()`, then `dismiss()` (iOS `wizard?.guestReviewExpired(); signup = nil` → `onDisappear` → `signupDismissed`).
- `signInInstead()` (`guest-signup.sign-in`), in `scope` on main: `host.prepareSignIn()` (awaited); `email = model.email.value`; `model.cancel()`; `auth.signIn.clearSecrets()`; `auth.signIn.editEmail(email)`; `presentation = null`; `rootNavigator.navigate(Route.SignIn)`; then `host.signUpDismissed(needsReview = model.guestState.value == Review)`, which finds the route already changed and only resets `guestAuthenticationPresented` (iOS order: `signup = nil` and `navigate` run before `onDisappear`). 11a's holder then clears the guest ViewModel; the draft keeps `signingIn = true`, so a cold launch recovers to Sign in (11a).
- `clear()` (holder cleared): `request?.cancel()`; `presentation.value?.model?.cancel()`; `presentation = null`. A committed session still routes: 5d's acceptance runs in `NonCancellable` and 5.5's `routeAcceptedSession` runs in `appScope`.
- Release binds the coordinator with `seed = null`, `prefill = null`. 11a's launcher binding (inline in `GuestWizardRoute`: `LaunchPlatform.guestSignUpRequested()` followed by an immediate `onDismissed()`) and its debug counter `GuestWizardDebugCounters.signUpRequested` (`app/src/debug/kotlin/net/chronoflow/debug/GuestWizardDebugCounters.kt`) are deleted, together with `LaunchPlatform.guestSignUpRequested` in debug **and** release and `ReleaseReadinessTest` l.36 and l.41 (*[Refresh R4]*).

**`GuestWizardRoute`** (11a) shows `GuestSignupSheet(presentation, coordinator)` above `WizardRoot` while `presentation != null`, keyed by `presentation.id`.

### 5.11 The sheet: layout and behaviour (port `GuestSignupView.swift`; board `GuestSignup`)

`…/fa/guest/GuestSignupSheet.kt` (the `CfModalBottomSheet` host) and `…/fa/guest/GuestSignupContent.kt` (stateless content, used directly by guards):

- **Sheet:** `CfModalBottomSheet(onDismissRequest = coordinator::dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !busy }))`. Container `card`, top radius 38 (`CfTheme.shapes.sheetTop`), M3 drag handle, `sheetMaxWidth = 640.dp` (Phase 2 defaults; iOS `presentationDetents([.large])`, `presentationCornerRadius(wizardSheetRadius = 38)`, `presentationBackground(card)`).
- **Column** (tag `guest-signup.scroll` on the scroll container, `verticalScroll`, `imePadding()`): modifier order `fillMaxWidth().wrapContentWidth().widthIn(max = PlanUITokens.wizardColumnWidth.dp /* 520 */).padding(…)`, so the 520 dp **includes** the 20 dp side padding and the content (fields) is at most 480 dp wide [F `GuestSignupView.swift:77–79`: `.padding(.horizontal, space5).padding(.top, space5).padding(.bottom, space8).frame(maxWidth: wizardColumnWidth).frame(maxWidth: .infinity)`]. Padding: horizontal **20** (`space5`), top **0** below the M3 drag handle (iOS pads `space5` = 20 under an overlaid system grabber; OQ-11b-11), bottom **32** (`space8`) plus the navigation-bar inset. Vertical gap **16** (`space4`). Whole content `enabled = !busy` (iOS `.disabled(model.form.busy)`).

| # | Element | Spec | Tag |
|---|---|---|---|
| 1 | Title block, gap **8** | Title Auth "Save your plan", `screenCardTitle` (Fraunces 400 24/32), `foreground`, `heading()`. Description Auth "Create an account to save it and open it on any device.", `screenBodyLg` (16/24), `mutedForeground` | `guest-signup.title` |
| 2 | Banner, only when `guestState != Ready` | `CfBanner(title, message, actionLabel = if (Review) Auth "Review my trip" else null, onAction = coordinator::review, announce = false, testTag = "auth.banner", actionTestTag = "auth.banner.action")` | `auth.banner` |
| 3 | Fields, gap **12** | Name `CfTextField` (Auth "Name"); Email `CfTextField` (Auth "Email"); Password `CfSecureTextField(revealed = false)` (Auth "Password", `hint = AuthPasswordPolicy.hint()`); Confirm password `CfSecureTextField(revealed = false)` (Auth "Confirm password"). Field errors under each field from `form.fields` through 5a's `AuthFieldCopy` | `guest-signup.name`, `guest-signup.email`, `guest-signup.password`, `guest-signup.confirmation` |
| 4 | Buttons, gap **8** | `CfButton(Primary, Lg, fillWidth, loading = pending == Register, enabled = guestState != Review)`, Auth "Create account and save" → `submit`. `CfButton(Ghost, Lg, fillWidth)` (iOS `AuthButton(kind: .ghost)`; if Phase 2 has no `Ghost`, use the style the merged code maps that iOS kind to), Auth "I already have an account" → `signInInstead` | `guest-signup.submit`, `guest-signup.sign-in` |
| 5 | Consent | 5d's `AuthConsent` (gains a text-resource parameter defaulting to 5d's key, *[Refresh R3]*) with Auth "By creating an account you accept the <terms>terms</terms> and <privacy>privacy policy</privacy>." (0c name `auth_by_creating_an_account_you_accept_th_b18f33`, **not** 5d's `…agree_to_9e09cb`), same `PolicyOpener` and URLs | `guest-signup.consent` |

- **Fields, IME, autofill:** as 5d §5.2 (name `Words` capitalisation, `PersonFullName`; email `Email`, no autocorrect, `EmailAddress + NewUsername`; both passwords `Password`, `NewPassword`). IME: Next → Next → Next → **Go** = submit. Name and email states are `remember { TextFieldState(model.name.value) }` (and email); password states are `remember { TextFieldState(prefill?.password ?: "") }`. **Never** `rememberSaveable` or `rememberTextFieldState` (saveable): a rotation drops typed passwords (OQ-11b-13). Edits go through 5a's `UserEditReporter` (`editName`, `editEmail`, `editPassword`, `editConfirmation`). `commitAutofill` = `LocalAutofillManager.current?.commit()`. Language at submit = the resolved app language (5d rule).
- **Submit** (button and IME Go): clear focus first (iOS `focus = nil`, l.117), then `coordinator.submit(…)`.
- **Feedback** (iOS l.82–99): when `guestState` changes to a non-`Ready` value: clear focus and hide the IME, bring the banner into view, announce `title + " " + message` through 5a's `AuthAnnouncer`. The sheet has no brand row, so host the announcer on a 0-size `Box` that is the first child of the column (tag `auth.announcement`, as 5a); feed it a revision that increments on each non-`Ready` change and on each `form` announcement revision. While `guestState == Ready`, announce `form.takeAnnouncement` (field errors). 5a's `AuthFailureFeedback` focuses the first invalid field in the order name, email, password, password_confirmation (tags above). 5a's `AuthKeyboardReveal` keeps the focused field plus `guest-signup.submit` visible.
- **Guest banner rule** [F `SignUpModel.swift:109`, `AuthFormModel.swift:48–89`]: `Failed` exactly when the request failed **and** 5a's mapping set a form banner: offline, throttled, server/5xx, any non-validation failure, "This email address is already in use." (`EmailTaken`, which has no field message; the sheet never draws 5a's form banner, so the guest `Failed` banner stands in, iOS parity, OQ-11b-4), or a validation message on an undisplayed field or with unsupported copy. A validation failure whose messages are all supported messages on displayed fields (for example "Please fill this in." on `name`) sets **no** form banner, so `guestState` stays `Ready`: no guest banner, the messages show under their fields and are announced through `form.takeAnnouncement`.
- **System back** while the sheet shows = `dismiss()` (ignored while busy). The guest wizard's own back handling is unchanged once the sheet is gone.

### 5.12 Seeds (debug only; iOS names)

| Seed | Effect |
|---|---|
| `CF_GUEST_WIZARD=1` + `CF_WIZARD_BOARD_CAPTURE=GuestSignup` \| `GuestSignupFailed` \| `GuestSignupRetry` \| `GuestSignupReview` | 11a's seed opens guest Ready (`WizardReady` draft). After the VM starts, the VM runs, once, the block 11a runs when `saveOutcome` becomes `signUpRequested` (guard on `guestAuthenticationPresented`, `setGuestAuthenticationPresented(true)`, `flush()`, `signUpLauncher.request(host)`). *[Refresh R4: 11a inlined that block and `internal` is invisible to `:app`; extract it and trigger it through a one-shot `WizardInitialPresentation.presentSignUp` set by `GuestWizardBoardSeed.presentation(board)`.]* Never call `savePlan()` for this: iOS calls `presentSignup(wizard)` directly (`GuestWizardDestination.swift:75–81`), without the Ready gate. The coordinator uses `seed.state` = `Ready` / `Failed` / `Retry` / `Review` as `initialGuestState`. |
| `CF_GUEST_WIZARD_JOURNEY=1` (alone, or together with `CF_GUEST_WIZARD=1`, which then opens the wizard as iOS `prepareUITestRoute` does, `AuthSessionServices.swift:259–262`; the claim journey uses it with `CF_GUEST_WIZARD=1` + `WizardGuestJourney`, *[Refresh R4/R5]*) | Binds `GuestSignupPrefill(name = "Traveller", email = "guest-journey@example.com", password = "NewPassword123!")` (iOS l.101–106): name and email through the model, both password states start with the password. |

`GuestSignupSeed` and `GuestSignupPrefill` live in `app/src/debug` (`…/appd/guest/`); `:app` main sees only nullable interfaces bound by debug. `ReleaseReadinessTest` asserts neither class exists in release.

### 5.13 Copy (every string en + nl, from resources)

All strings come from the iOS Auth catalog (0c). Find each name with `grep -rn "<en text>" core/ui/src/main/res/values/`: "Save your plan" / "Sla je plan op"; "Create an account to save it and open it on any device." / "Maak een account om het te bewaren en op elk apparaat te openen."; the six banner strings of §5.6; "Review my trip" / "Mijn reis bekijken"; "Name" / "Naam"; "Email" / "E-mailadres"; "Password" / "Wachtwoord"; "Confirm password" / "Bevestig je wachtwoord"; the 12-character policy hint (5c); "Create account and save" / "Account aanmaken en opslaan"; "I already have an account" / "Ik heb al een account"; the consent string / "Door een account aan te maken, ga je akkoord met de <terms>voorwaarden</terms> en het <privacy>privacybeleid</privacy>.". **No Android-only copy** in this phase; `tools/l10n/android-only.json` is unchanged.

### 5.14 Edge cases (each has a test in §7)

1. "Create account and save" with no live claim (draft expired, secret missing, identity changed) → `Review`, no request, no account, passwords cleared.
2. Server `review_expired` (plain or inside `validation_failed`) → `guestReviewExpired(claim)` once, `Review`, no banner text from the form, passwords cleared; the stored draft is now Ready + `returnToReady`, without `checkingAnswers`/`reviewedAt`, revision + 1.
3. Offline / 5xx / 429 / email taken → `Failed` banner, name and email kept, passwords cleared. A validation failure with only displayed-field messages → `Ready`, no guest banner, field messages shown.
4. Account created but the local acceptance throws (`SessionFailure.CredentialWrite`) → `Retry`; a second tap replays the server receipt (same `tripId`).
5. A cancelled request after the session was committed still stores the continuation and routes (`committedSessionFinishesContinuationAfterRequestCancellation`).
6. Another account's database → `GuestAcceptanceSuperseded`; the guest draft, its secret and the other account's continuation are untouched.
7. Sign-up without a claimed trip (`tripId == null`) → the guest draft and secret are kept.
8. Password/passkey login with a live draft and no receipt → member wizard seeded at Checking, continuation `MemberWizard`, draft and secret deleted, account settings unchanged.
9. Two-factor login with a live draft → continuation `Trips`, no member seed, draft and secret deleted.
10. No guest draft → no continuation, the member recovery row is unchanged (all four methods).
11. Continuation present while the gate is Verify, Terms or Walkthrough → not consumed; consumed exactly once at App.
12. Kill after the claim but before verification → the continuation survives (account DB); sign-out with discard erases it.
13. Busy request → the sheet cannot be dismissed by swipe, scrim tap or back.
14. "I already have an account" while the request is idle → Sign in with the email filled in, `signingIn = true` persisted, the sheet gone.
15. "Review my trip" → sheet gone, Ready shown, "Make my plan" re-runs Checking (needs-review gate).
16. Rotation with the sheet open → the sheet stays (holder scope), name and email kept, passwords empty.

---

## 6. Kotlin shape [R]

| Class / change | Module, package | Notes |
|---|---|---|
| `GuestClaim` | `:core:model` `…model.guest` | JVM |
| `GuestSessionContinuation` (+ `encode`/`decode`) | `:core:model` `…model.auth` | JVM |
| `GuestWizardStore.claim`, `.reviewExpired` | `:core:wizard` `…wizard.model` (11a file) | JVM |
| `MemberWizardStore.start(seed)` | `:core:data` `…data.wizard` (9b file) | JDBC tests |
| `GuestContinuationRepository`, `GuestSessionAcceptance`, `GuestAcceptanceSuperseded` | `:core:data` `net.chronoflow.core.data.guest` | JDBC tests |
| `GuestSignupState`; `SignUpModel` guest additions; `SignInModel`/`PasskeySignIn` claim | `:feature:auth` `…auth.model`, `…auth.model.passkey` | JVM |
| `GuestSignupSheet`, `GuestSignupContent` | `:feature:auth` `…auth.guest` | Compose |
| `GuestSignUpHost`, `GuestSignUpLauncher(host)` | `:feature:wizard` `…wizard.guest` (11a file) | |
| `ShellNav.guestTrips()` | `:core:navigation` `…navigation.shell` | pure |
| `GuestContinuationRunner`, `GuestSignupCoordinator`, `AppGraph.guestAcceptance`, `AuthServices`/`SignInRouter` changes, `AppShell` start, `GuestWizardRoute` sheet slot | `:app` `net.chronoflow.app.guest` (+ edits) | |
| `GuestSignupSeed`, `GuestSignupPrefill` | `app/src/debug` `net.chronoflow.debug.guest` | debug only |

Module edges: `:core:data` → `:core:wizard`, `:core:model`, `:core:sync`, `:core:database` (all exist; check with `grep` in `core/data/build.gradle.kts`). `:feature:auth` must **not** depend on `:core:wizard` or `:core:data` (the claim enters as a lambda). Do not add `:core:wizard` → `:core:data`.

---

## 7. Tasks (one commit each, in order)

Run `make targeted ONLY=…` for each task's tests, and `make quick` once before the last commit.

### Task 1. `feat(model): add the guest claim and session continuation`
- Files: `…/cm/guest/GuestClaim.kt`; `…/cm/auth/GuestSessionContinuation.kt` (§5.1).
- Tests (JVM, `core/model/src/test/…`): `GuestClaimTest.toStringHidesTheSecret` (no `"secret"` value in `toString()`); `GuestSessionContinuationCodecTest`: `roundTripsEveryCase` (exact strings `{"kind":"trip","tripId":"t1"}`, `{"kind":"memberWizard"}`, `{"kind":"trips"}`), `unknownOrMalformedDecodesToNull` (`{"kind":"x"}`, `{"kind":"trip"}`, `{"kind":"trip","tripId":""}`, `"{bad"`).

### Task 2. `feat(wizard): read and expire the guest claim`
- Files: `…/cw/model/GuestWizardStore.kt` (§5.2). `returnToReadyAfterExpiredClaim` is already `internal` in `:core:wizard` (*[Refresh R2]*).
- Tests (JVM `…/cwt/model/GuestClaimStoreTests.kt`, Swift names, `GuestWizardStoreTests`' temp-dir pattern and `:core:testing`'s `WizardTestClock`, *[Refresh R6]*):
  - `providerNeedsALiveDraftAndItsKeychainSecret`: no file → `null`; started draft without identity → `null`; identity `("guest-token", 4, 7)` without a secret → `null`; with secret `"secret"` → `GuestClaim("guest-token", 4, 7, "secret")`; clock + 1800 s → `null`.
  - `expiredClaimReturnsToReadyAndInvalidatesTheReview`: identity `("token", 1, 2)`, step `checking`, `checkingAnswers = true`, `reviewedAt` set, `age_band = "18_64"` → after `reviewExpired(GuestClaim("token", 1, 2, "secret"))`: step `ready`, `returnToReady == true`, `checkingAnswers == null`, `reviewedAt == null`, `profile` and `form` equal, revision + 1.
  - Android: `mismatchedClaimLeavesTheDraftUnchanged` (another `reviewRevision` → file bytes unchanged); `claimIsNullWhenTheDraftChangesDuringTheSecretRead` (a `GuestSecretStore` fake whose `get` persists a new identity before returning → `null`).

### Task 3. `feat(data): start the member wizard from a guest seed`
- Files: `…/cd/wizard/MemberWizardStore.kt` (§5.3).
- Tests (JVM, `…/cdt/wizard/MemberWizardStoreSeedTest.kt`, 9b's JDBC fixture): `seedStartsAtCheckingWithReturnToReady` (every assertion of the Swift `loginWithoutTripSeedsOnlyJourneyIntoTheAccountWizard` seed block: `form == guest.form` with `identity = null` and the default override; `airports` equal; `mode == member`; `step == checking`; `returnToReady == true`; `entered`; `!fromLastTrip`; `profile` empty; `review`, `questions`, `checkedInput`, `reviewedAt`, `checkingAnswers` null; `fieldErrors` empty; `savedProfile ==` the stored `bodyClock`; `!sleepNeeded` with `sleepAnswered = true`; a new `instance`); `seedReplacesTheExistingRowAndRejectsItsWriter` (`persist(replaced)` → `RecoveryRejected`; one recovery row).

### Task 4. `feat(data): keep the guest continuation in the account store`
- Files: `…/cd/guest/GuestContinuationRepository.kt` (§5.4).
- Tests (JVM, `…/cdt/guest/GuestContinuationRepositoryTest.kt`, 4a JDBC `AccountDatabase`): `consumesOnlyAtTheAppGate` (Verify/Terms/Walkthrough → `null`, row kept; App → value, row gone; App again → `null`); `valuesEmitTheStoredDestination` (store `Trip("t")` → `values().first { it != null } == Trip("t")`); `survivesCloseAndReopen`; `corruptRowIsDroppedOnConsume`; `otherSettingsAreUntouched` (a `bodyClock` row is unchanged); `snapshotApplicationKeepsTheRow` (apply a 4d account snapshot through the merged applier; the row remains — see §5.4 [R]).

### Task 5. `feat(data): accept a session with or without a claimed trip`
- Files: `…/cd/guest/GuestSessionAcceptance.kt` (§5.5).
- Tests (JVM, `…/cdt/guest/`, port the Swift fixture `GuestAcceptanceFixture` l.168–233 with 4e's `SessionController`, `SessionAccountDatabase`, `AccountWiper(guestSecrets = …)`, `StoredDataReadiness`, `MemoryGuestSecretStore`, 11a's `GuestWizardStore` on a temp dir; `result(tripID, verified, accepted, seen)` exactly as Swift l.196–206 (token `"session-token"`, account id `019a562b-8000-7000-8000-000000000001`, terms/privacy version `"2"`); `seedGuest()` exactly as Swift l.208–227: AMS→HND 2027-03-28T12:00 → 2027-03-29T09:00, preparation 2, identity `("guest-token", 4, 7)`, override enabled with bed 22:00, `age_band 65_plus`, `shift_work false`, step `ready`, `checkedInput` set (Swift `Data([1])`; use the merged type's non-null value), `reviewedAt` = now, airport `AMS` (Amsterdam, `Europe/Amsterdam`), then `persist`, secret `"secret"`; the "other account" id is `019a562b-8000-7000-8000-000000000002`):
  - `GuestSessionAcceptanceClaimedTripTests` (Parameterized: `SignUp`, `Password`, `Passkey`) `claimedTripSurvivesEveryGateAndIsConsumedOnce`: routes `[Verify]`; guest draft and secret gone; for gates Verify, Terms, Walkthrough, App (upserting the account as Swift does) the route is the gate's route and `consume(gate)` is `Trip("guest-token")` only at App; a second App consume → `null`.
  - `GuestSessionAcceptanceLoginTests` (Parameterized: `Password`, `Passkey`) `loginWithoutTripSeedsOnlyJourneyIntoTheAccountWizard`: every Swift expectation (l.41–77), incl. `consume(App) == MemberWizard`, no listed drafts, settings unchanged.
  - `GuestSessionAcceptanceTests`: `twoFactorDropsTheGuestAndContinuesToMyTripsWithoutSeeding`, `continuationPersistsOnReopenAndSignOutErasesIt`, `anotherAccountCannotTakeTheGuestOrItsContinuation` (`GuestAcceptanceSuperseded`), `committedSessionFinishesContinuationAfterRequestCancellation` (cancel the caller before `routeAcceptedSession`; routes `[Today]`, continuation `Trip`, draft gone).
  - `GuestSessionAcceptanceOrdinaryTests` (Parameterized: all four methods) `ordinaryAuthenticationKeepsTheMemberWizard`.
  - Android: `signUpWithoutTripKeepsTheGuest` (`tripId = null`, method `SignUp`, live draft → draft and secret kept, no continuation).

### Task 6. `refactor(auth): add the guest claim to sign-up`
- Files: `…/fa/model/GuestSignupState.kt`; `…/fa/model/SignUpModel.kt` (§5.6); `SignInModel.makeSignUpModel(onBack, initialGuestState)` passes the providers (they default to `{ null }` until Task 7).
- Tests (JVM, add to 5d's `…/fat/model/SignUpModelTests.kt` as a nested `class Guest` (the file is `@RunWith(Enclosed::class)`, *[Refresh R6]*), Swift names, 5d's mock-source fixture):
  - `guestSignupRequiresALiveClaimBeforeCreatingAnAccount`: `requiresGuestClaim = true`, no claim → `GuestReviewExpired`, `guestState == Review`, both secrets cleared, `accepted` never called, `mock.inbox()` empty.
  - `guestSignupDistinguishesARequestFailureFromAnAccountAwaitingAcceptance`: starts `Ready`; `failNext(SignUp, Server(503))` → `Failed`; second submit with `session.accept` throwing `SessionFailure.CredentialWrite` → `Retry`, `sessionFailure == CredentialWrite`, secrets cleared.
  - `expiredGuestClaimShowsReviewAndClearsPasswords`: claim `("trip", 1, 1, "secret")`, `failNext(SignUp, ReviewExpired)` → `GuestReviewExpired`, `Review`, secrets cleared.
  - Android: `claimFieldsAreSentOnlyWithAClaim` (recording source: with a claim the `RegisterInput` has all four values; without, all four `null`); `submitResetsTheGuestStateFirst` (`initialGuestState = Review`, then a model-level `submit` with a live claim resets to `Ready` before the request, as iOS l.68; blocking submit in `Review` is the coordinator's job, Task 12); `fieldOnlyValidationKeepsReady` (`failNext(SignUp, Validation(mapOf("name" to FieldMessage("Please fill this in.")), 422))` → outcome `null`, `guestState == Ready`, `form.banner == null`, `form.fields["name"]` set); `GuestSignupStateTest.keysResolveInEnAndNl` (Robolectric `@Logic`, the six keys of §5.6 resolve to the exact en/nl strings).
- 5d's `SignUpModelTests`, `SignUpViewModelTests`, `SignUpGuardsTest` and `SignUpConsentTest` stay green unchanged (there is no `SignUpScreenTest`, *[Refresh R6]*).

### Task 7. `feat(auth): send the guest claim on password and passkey sign-in`
- Files: `…/fa/model/SignInModel.kt`, `…/fa/model/passkey/PasskeySignIn.kt` (§5.7).
- Tests (JVM, `…/fat/model/GuestClaimAuthTests.kt`, the model half of the Swift class; a recording fake `DataSource` — reuse 5a/5b's if one exists):
  - `GuestClaimAuthRequestTests` (Parameterized: `register`, `password`, `passkey` × live `true`/`false`) `requestUsesOnlyTheClaimFieldsForItsMethod`: the recorded input has `draftToken == "guest-token"` and `guestSecret == "secret"` only when live; `inputRevision == 4` and `reviewRevision == 7` only for `register` when live; `deviceID == "device"`, `deviceName == "phone"`; `accepted` gets `tripId == "guest-token"` when live and the method `SignUp`/`Password`/`Passkey`; the session accepted once; secrets cleared after the call; passkey makes 2 source calls (options + login), others 1. Use `PasskeyCapability(true)` and a deterministic assertion provider for the passkey rows.
  - `GuestClaimRegistrationFailureTests` (Parameterized: `expired` → `DataFailure.ReviewExpired`, `validation-expired` → `Validation(mapOf("review_expired" to FieldMessage("This review has expired. Update your plan before saving.")), 422)` (the Swift fixture `DataAccessTests/Fixtures/wizard/expired.json`; only the key matters to `TripWizardErrors.map`), `offline` → `Offline`, `server` → `Server(503)`) `registrationFailureClearsBothPasswordsAndReturnsExpiry`: outcome `GuestReviewExpired` only for the two expiry rows; `guestReviewExpired` called once for them, else 0; secrets cleared; not busy; name "Traveller" and email `new@example.com` kept; `form.banner == null` for expiry, non-null otherwise.
  - `GuestClaimTwoFactorTest.twoFactorInputHasNoClaimFields` (Android, its own plain JVM class so it does not run once per parameter row): a live claim, two-factor completion → the recorded two-factor input has no claim property (assert the input class's declared properties contain no `draftToken`/`guestSecret`).
- 5a's `SignInModelTests` and 5b's passkey tests stay green unchanged.

### Task 8. `feat(app): route accepted sessions through the guest acceptance`
- Files: `AppGraph.guestAcceptance`; `…/app/auth/AuthServices.kt` (providers, `acceptedSession`) (§5.8); `AuthServicesTest`, `SignInModelRoutingTests` updated in the same commit (*[Refresh R3]*).
- Tests (Robolectric `@Logic`, `…/apptd/guest/GuestAcceptanceWiringTest.kt`, real `AppGraph` with `GraphOverrides(mockDelay = Duration.ZERO, sessionCipher = SoftwareSessionCipher())`, mock source (*[Refresh R6]*); seed the guest draft and secret through a real guest review on the mock, as `GuestGenerationMockTest` (11a) does):
  - `signUpWithALiveClaimStoresTheTripAndClearsTheGuest`: `makeSignUpModel(…).submit(…, requiresGuestClaim = true)` → route `Verify`; continuation `Trip(<token>)`; `guest-wizard.json` absent; `guestSecrets.get(token) == null`.
  - `passwordSignInWithALiveDraftSeedsTheMemberWizard`: a seeded verified mock account, a live guest draft without a receipt → `tripId == null`, member recovery row at `checking`, continuation `MemberWizard`, guest file gone.
  - `signInWithoutAGuestLeavesEverythingAsBefore`.

### Task 9. `feat(app): open the claimed trip from the guest continuation`
- Files: `…/app/guest/GuestContinuationRunner.kt`; `ShellNav.guestTrips()`; the `AppShell` `LaunchedEffect` (§5.9).
- Tests:
  - JVM `…/appt/guest/GuestContinuationRunnerTest.kt` (fake flow, `runTest(timeout = 60.seconds)`): `tripOpensOnceAtTheAppGate` (`onTrip("t1")` once; a second emission of the same row after consume does nothing); `tripsSelectsMyTrips`; `memberWizardOpensTheWizard`; `nonAppGateConsumesNothing` (gate `Verify` → no callback, row kept); `stopsWhenTheSessionChanges` (`isCurrent` false before the gate read → no callback, `run()` returns); `staleAfterTheGateSkipsOnlyThatValue` (`isCurrent` true, then false after `gate()`, then true again for the next emission → the first value is not consumed, the second is); `storeErrorEndsQuietly`.
  - JVM `ShellNavGuestTest` (6a test style): `guestTripsClearsTheOverlayAndSelectsMyTrips` (from an overlay on Today → `overlay == null`, `selected == Trips`, Trips stack `[Trips]`); `wizardSavedStillOpensTheTrip` (unchanged).

### Task 10. `refactor(wizard): hand a sign-up host to the guest launcher`
- Files: `…/fw/guest/GuestSignUpLauncher.kt`; the guest VM implements `GuestSignUpHost` (§5.10 seam). Keep 11a's binding compiling (temporarily: `request(host)` → `host.signUpDismissed(needsReview = false, onGuestRoute = true)`; *[Refresh R4]*).
- Tests: update 11a's `TripWizardViewModelGuestSignUpTest` to the new seam (same assertions); add `dismissalWithReviewMarksTheReviewExpired` (`signUpDismissed(true)` on the guest route → step `ready`, `returnToReady`, `saveOutcome == null`, then `readyTick`), `dismissalAfterLeavingTheRouteOnlyResetsPresented`, `prepareSignInPersistsSigningIn` (the guest file has `signingIn == true` after `prepareSignIn()` returns).

### Task 11. `feat(auth): draw the guest sign-up sheet`
- Files: `…/fa/guest/GuestSignupContent.kt`, `GuestSignupSheet.kt` (§5.11).
- Tests:
  - *[Refresh R6: no copy on screen; replace the text assertions below with tag structure plus JVM key/resource checks.]* Robolectric `@Logic` `…/fat/guest/GuestSignupContentTest.kt` (content only, fake callbacks): `readyHasNoBannerAndFourFields` (tags of §5.11 exist; `auth.banner` absent; no node with text Auth "Show password"); `failedAndRetryShowTheirBanners` (exact en text of title and message); `reviewShowsTheActionAndDisablesSubmit` (`auth.banner.action` click → `onReview` once; `guest-signup.submit` `assertIsNotEnabled()`); `passwordHintIsTheServerPolicy` (the 5c hint text, not "At least 10 characters."); `consentUsesTheGuestKey` (nl text "Door een account aan te maken, ga je akkoord met …"; two link spans); `busyDisablesEverything`; `goOnConfirmationSubmits`.
  - Robolectric `@Layout` `GuestSignupGuardsTest` (0e `Guards`: no truncation, 48 dp targets, inside `safeDrawing`, no overlap, headings) on a test host `GuestSignupSheetFrame` that draws the content as the sheet lays it out (width `min(window, 640 dp)`, centred, `card` fill, top radius 38, a 48 dp drag-handle slot), one case per parameter set: states `Ready`, `Failed`, `Retry`, `Review` × profiles `small`, `pixel8`, `foldInner`, `largeFont`, `landscapeSideNav` × en, nl × light, dark = **80** cases. Extra assertions (on the `guest-signup.name` field, whose width is the column width minus the 2 × 20 dp padding): at `foldInner` it is exactly 480 dp wide and horizontally centred in the frame; at `pixel8` it is the frame width − 40 dp.

### Task 12. `feat(app): present the guest sign-up sheet over guest ready`
- Files: `…/app/guest/GuestSignupCoordinator.kt`; `GuestWizardRoute` sheet slot; `GuestWizardHolder` owns and clears the coordinator; the release/debug bindings; `…/appd/guest/GuestSignupSeed.kt`, `GuestSignupPrefill.kt`; the `GuestSignup*` auto-present (§5.10, §5.12). Delete 11a's launcher binding and `GuestWizardDebugCounters.signUpRequested` (§5.10).
- Tests:
  - JVM `…/appt/guest/GuestSignupCoordinatorTest.kt` (fake host, a `SignUpModel` over the mock source): `secondRequestWhilePresentedIsIgnored`; `dismissIsIgnoredWhileBusy`; `dismissInReviewMarksTheReviewExpired` (`signUpDismissed(true)` once); `reviewDismissesAndExpires` (`reviewExpired` once, `signUpDismissed(true)` once, presentation `null`); `signInInsteadCarriesTheEmail` (order recorded: `prepareSignIn` → `clearSecrets` → `editEmail("new@example.com")` → navigate `SignIn` → `signUpDismissed`); `submitInReviewSendsNothing`; `clearCancelsTheRequest`.
  - Robolectric `@Logic` `…/apptd/guest/GuestSignupScreenTest.kt` (port of `GuestSignupScreenTests`, signed-out `AppGraph`, `CF_GUEST_WIZARD=1` + the board seed; Swift names): `testGuestSignupReady` (`guest-signup.name`, `.password`, `.confirmation` exist; `auth.banner` absent), `testGuestSignupFailed`, `testGuestSignupRetry` (banner text contains title and message), `testGuestSignupReview` (`auth.banner.action` → `wizard.ready.continue` enabled, `guest-signup.name` gone), `testSignInInstead` (`performScrollTo()` on `guest-signup.sign-in`, click → `sign-in.email` exists (iOS assertion); Android addition: the guest file has `signingIn == true`. The email carry is covered by the coordinator JVM test, since the board seed has no email). If Robolectric cannot reach the `ModalBottomSheet` window, move this class to the emulator (`@Smoke`, same names, < 10 s each) and record it (AGENTS Tests 6).
  - ~~Update 11a's `GuestWizardScreenTest.testReadyGuest`~~ *[Refresh R6: it is a stateless `WizardScreen` test in `…/fwdt/` and stays unchanged; add the Ready → presentation JVM case to `GuestSignupCoordinatorTest` instead.]*
  - Extend `ReleaseReadinessTest`: no `GuestSignupSeed`/`GuestSignupPrefill` class; the release coordinator has `seed == null` and `prefill == null`; remove 11a's counter line (§5.10).

### Task 13. `test(app): guard and capture the guest sign-up boards`
- `GuestSignupCaptureTest` (`@Capture`, 0e `BoardCapture.capture(rule, name)`, 375 × 812 dp as the board frame, density 3, zero insets, light, en): exactly `GuestSignup`, `GuestSignupFailed`, `GuestSignupRetry`, `GuestSignupReview`, each the real route with its seed (sheet over guest Ready). If 0e's capture cannot include the sheet window on Robolectric, produce the four with `make captures ONLY=GuestSignup* DEVICE=1` instead and record which path was used. No stored references.
- `docs/wizard.md` (guest section from 11a): add the board → seed rows for the four captures and the recorded visual differences (backdrop = guest Ready, the 12-character hint, the M3 drag handle, field style from Phase 2).

### Task 14. `test(app): claim a reviewed guest trip through the account gates`
- No production code. JVM `…/appt/guest/GuestWizardClaimIntegrationTests.kt` (Parameterized: `En`, `Nl`) `guestJourneyClaimsTheReviewedTripAndWaitsForTheAccountGates`, a port of the Swift test l.10–92: `MockDataSource` (zero delay, `now` fixed at epoch second 1_800_000_000), `MemoryGuestSecretStore`, `DeviceIdentityStore` over a memory preference, `GuestWizardStore` on a temp dir, `MemberWizardModel` with `GuestWizardGeneration` and 9a's `CheckingTestClock`; drive AMS→HND 2030-10-12T14:40 → 2030-10-13T09:15, `chronotype intermediate`, `shift_work false`; Checking; answer `age_band 18_64`; Checking again; Fine-tune (no optional prompts); Ready; `savePlan()` → `signUpRequested`, `createdTripID == null`; `flush()`. Then a `SignUpModel` with `claim = { store.claim(secrets) }` and `accepted` = open a JDBC `AccountDatabase` for the account + `GuestSessionAcceptance.accept(result, SignUp, database)`; name "Traveller", `guest-journey@example.com`, `NewPassword123!` ×2 → `Accepted`. Assert: `tripId == token`; `AuthNavigation.route(account) == Verify`; secrets cleared; `store.recover() == null`; `secrets.get(token) == null`; `consume(Verify | Terms | Walkthrough) == null`; open the last mock verification mail and `verifyEmail` it; `fetchIdentity().hasVerifiedEmail`; `consume(App) == Trip(token)`; local `TripRepository.trips()` empty; `consume(App)` again `null`; `source.fetchTripDetail(token).trip.id == token`. Budget: each case < 1 s is not required (declared integration test, like iOS); keep it < 3 s and record the time in the PR.

### Task 15. `test(app): walk a guest from welcome to the claimed trip`
- *[Refresh R5 replaces this bullet: the live `CF_GUEST_WIZARD=1` + `CF_GUEST_WIZARD_JOURNEY=1` + `WizardGuestJourney` seed from Sleep, a minted token, the storage-root file check, no copy assertion.]* **`GuestWizardJourneyTest.testWelcomeThroughSignupOpensClaimedTrip`** (`…/appat/guest/`, `@Smoke`, rename 11a's `testWelcomeThroughReadyRequestsSignUp` back to the Swift name, same seeds: `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, `CF_MOCK_DELAY=0`, `CF_GUEST_WIZARD_JOURNEY=1`, `CF_WIZARD_BOARD_CAPTURE=WizardGuestJourney`). After 11a's steps up to `wizard.ready.continue` (delete 11a's `GuestWizardDebugCounters.signUpRequested` assertion; keep its "2 answered" assertion before the tap): wait `guest-signup.name` (2 s); `performScrollTo()` + click `guest-signup.submit`; wait and click `verify.open-email` (5 s); wait and click `walkthrough.skip` (5 s); wait until `trip.scroll` exists (3 s; the iOS id, `GuestWizardJourneyTests.swift:26`); assert `wizard.ready.continue` does not exist; assert `File(targetContext.noBackupFilesDir, "ChronoFlowMock/guest-wizard.json")` does not exist. < 10 s, `timeout_msec=60000`, `waitUntil` only.
- **`GuestSignupKeyboardTest.testConfirmationStaysAboveTheKeyboard`** (Android-only, `@Smoke`; seeds `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, `CF_MOCK_DELAY=0`, `CF_GUEST_WIZARD=1`, `CF_GUEST_WIZARD_JOURNEY=1` (the prefill), `CF_WIZARD_BOARD_CAPTURE=GuestSignup`): wait `guest-signup.confirmation` (3 s), click it; wait until the IME inset of the root window insets is > 0 (2 s); the confirmation field's bottom ≤ window height − IME inset; pressing the IME action submits: ~~wait until `verify.open-email` exists (5 s)~~ wait until `auth.banner.action` exists (3 s): the frozen seed has no claim, so submit yields Review (*[Refresh R5]*).
- StrictMode: during the journey, `adb logcat -d -s StrictMode` shows no violation from `GuestSignupCoordinator`, `GuestSessionAcceptance`, `GuestContinuationRepository`, `GuestContinuationRunner` or `GuestWizardStore`.
- plan.md: tick all four 11b boxes (sheet, Claim, Credits, Tests) in this commit; next to the sheet box add "`GuestExcluded` not built (iOS OQ 47; retired by no health data; guests use `WizardExcluded`)"; add OQ-11b-1…13 to "Open questions for the user" as OQ-A544…OQ-A556 (*[Refresh R1]*); update the guest seed roles at plan l.469 (*[Refresh R5]*); add `CF_WIZARD_BOARD_CAPTURE=GuestSignup*` and the journey prefill to "Android spec: seeds and test hooks"; record the Valet claim gate as open for the user.

---

## 8. Acceptance criteria

1. All greps of §3 succeed, and the four 4b/4c test classes of §3 pass unchanged.
2. `make targeted ONLY="core:model:GuestClaimTest core:model:GuestSessionContinuationCodecTest core:wizard:GuestClaimStoreTests core:data:MemberWizardStoreSeedTest core:data:GuestContinuationRepositoryTest core:data:GuestSessionAcceptanceTests core:data:GuestSessionAcceptanceClaimedTripTests core:data:GuestSessionAcceptanceLoginTests core:data:GuestSessionAcceptanceOrdinaryTests feature:auth:SignUpModelTests feature:auth:GuestSignupStateTest feature:auth:GuestClaimAuthRequestTests feature:auth:GuestClaimRegistrationFailureTests feature:auth:GuestClaimTwoFactorTest feature:auth:GuestSignupContentTest feature:wizard:TripWizardViewModelGuestSignUpTest core:navigation:ShellNavGuestTest app:GuestAcceptanceWiringTest app:GuestContinuationRunnerTest app:GuestSignupCoordinatorTest app:GuestSignupScreenTest app:GuestWizardScreenTest app:GuestWizardClaimIntegrationTests"` is green; every JVM and warm Robolectric test < 1 s except `GuestWizardClaimIntegrationTests` (< 3 s), with AGENTS' first-case allowances recorded separately (`build/test-results/*/timings.json`; *[Refresh R6]*). Drop `app:GuestWizardScreenTest` from this list (unchanged, `feature:wizard`).
3. Unchanged and green: 5a `SignInModelTests`, 5b `PasskeySignInTests`/`TwoFactorModelTests`, 5d `SignUpViewModelTests`/`SignUpGuardsTest`/`SignUpConsentTest`/`VerifyEmailModelTests`, 9b `MemberWizardStore` tests, 9c `WizardRecoveryCheckTest`, 6a `ShellNavStateTest`/`ShellNavSettingsTest`, 11a `GuestWizardReviewIoTest`, `GuestWizardScreenTest` (*[Refresh R6]*), 11a `GuestWizardStoreTests`, `GuestRecoveryGateTests`, `GuestWizardGenerationTests`.
4. `make targeted ONLY="feature:auth:GuestSignupGuardsTest"` — 80 cases green, each < 1 s warm (*[Refresh R6]*).
5. `make captures ONLY=GuestSignup,GuestSignupFailed,GuestSignupRetry,GuestSignupReview` (or `DEVICE=1`, recorded) — exactly 4 PNGs in `build/captures/`, named after the boards; the reviewer compares each with its board.
6. `make quick` and `make check` (with `pixel` booted) pass. `make check` runs `GuestWizardJourneyTest.testWelcomeThroughSignupOpensClaimedTrip` and `GuestSignupKeyboardTest` (`@Smoke`), each < 10 s.
7. `testReleaseUnitTest` proves no `GuestSignupSeed`, `GuestSignupPrefill` or `CF_GUEST_*` handling ships in release.
8. `grep -rnE "SIGNUP_CREDITS|credits you get when you sign up\"|\b3 credits\b" --include=*.kt app/src/main feature/*/src/main core/*/src/main` prints nothing (credits come only from the server; the guest price strings are resources).
9. `grep -rn "guestSecret\|guest_secret" --include=*.kt app/src/main feature/auth/src/main` shows only request building and the claim provider: never a log, `Bundle`, `SavedStateHandle`, route argument or `toString()`.
10. *[Refresh R8 replaces this item]* StrictMode clean during the journey (Task 15). No new perf budget row. Run `make baseline-profile` (commit the profiles) and `make perf` (record in `docs/perf-startup.md`); `make perf-timeline` is not applicable.

---

## 9. Review checklist (for the Opus reviewer)

- [ ] **No re-port.** The mock claim and the wire test are 4c/4b code; `guestReviewExpired`, `setGuestSigningIn`, `setGuestAuthenticationPresented` are 9a's; the guest store and secret store are 11a's. No second `MockGuestRegistrationClaimTests`, no second copy of `returnToReadyAfterExpiredClaim`.
- [ ] **Claim provider.** `claim()` re-reads the draft after the secret read and compares instance **and** identity; it never throws (except cancellation); an expired draft gives `null`.
- [ ] **Sign-up order.** `requiresGuestClaim` with no claim returns `Review` **before** any request; `accountCreated` is set before acceptance, so an acceptance failure is `Retry`, not `Failed`; expiry calls `guestReviewExpired(claim)` once and leaves `form.banner == null`; passwords are cleared on every path.
- [ ] **Claim fields.** Register sends all four or none; password and passkey send only `draft_token` + `guest_secret`; two-factor sends none. The member Sign up screen also sends a live claim (iOS parity).
- [ ] **Acceptance order.** Continuation written before the guest draft and secret are deleted; `SignUp` without `tripId` keeps the guest; `TwoFactor` stores `Trips` without seeding; another account's database throws `GuestAcceptanceSuperseded` and touches nothing; `routeAcceptedSession` runs in `appScope` (survives caller cancellation).
- [ ] **Continuation.** Consumed only at the App gate, exactly once, in one transaction; `Trip` → `wizardSaved(id)` (Trips tab, `awaitingSync = true`); `Trips` → My trips root; `MemberWizard` → `WizardLauncher.open()`; the runner starts after the 9c recovery check; sign-out with discard erases it; a sync snapshot does not.
- [ ] **Sheet behaviour.** Not dismissible while busy (swipe, scrim, back); "Review my trip" → Ready and a re-check; "I already have an account" flushes `signingIn = true` **before** navigating and carries the email; one presentation at a time; the holder clears the coordinator.
- [ ] **Sheet look.** Column max 520 dp centred, 640 dp sheet, radius 38, `card`; gaps 16 / 8 / 12 / 8; padding 20 / 32; title `screenCardTitle`; the 12-character hint; no show-password control; the guest consent key; the Failed banner exactly when the form mapping sets a banner (including email taken), `Ready` with field messages for a field-only validation failure.
- [ ] **Secrets.** Passwords only in `remember { TextFieldState() }` and `SecretField` (never saveable, never in the coordinator or model); the claim's secret only inside one request; the debug prefill password exists only in `src/debug`.
- [ ] **Credits.** No client arithmetic or literal; Ready's price line is 11a's resource pair with the review's values.
- [ ] **Copy.** Every string from resources, en and nl in the same commit; no new Android-only copy.
- [ ] **Tests.** Swift names kept; parameterised cases are `Parameterized` classes; JVM for logic, Robolectric for layout and route changes, emulator only for the journey and the IME; exact tags; no `Thread.sleep`.
- [ ] **Not built:** `GuestExcluded`, a "Verified, no trip claimed" line, any post-claim screen without a board.
- [ ] Typical mistakes: deleting the guest draft before writing the continuation; consuming the continuation on Verify/Terms; calling `readiness.routeAcceptedSession` directly from `SignInRouter` (skipping acceptance); a `CancellationException` swallowed by `runCatching`; using `rememberTextFieldState()` for passwords; dismissing the sheet while the request runs; reading the guest file or DataStore on main; forgetting to delete 11a's counter binding; `:feature:auth` importing `:core:wizard`.

---

## 10. Open questions (proceed on the default; add them to plan.md)

| # | Question | Default |
|---|---|---|
| OQ-11b-1 | plan.md lists `GuestExcluded` for 11b, but the board is the retired health-answers screen; iOS left it unimported (iOS OQ 47) and `05` l.90 says "do not build". | Do not build it. Guests use `WizardExcluded` (9e; exits in 11a). Note it next to the plan box. |
| OQ-11b-2 | The board's password hint says "At least 10 characters."; the server policy is 12+. | Use `AuthPasswordPolicy.hint()` (the 12-character hint), as iOS. |
| OQ-11b-3 | The board's backdrop is the superseded `GuestReview`. | Present the sheet over guest Ready (`WizardReadyGuest`/`Free`), as iOS. |
| OQ-11b-4 | The guest sheet draws only the guest banner, so "email already in use" shows the generic Failed banner, and a field-only validation failure shows no banner at all (`guestState` stays `Ready`). | iOS parity; field messages still show. |
| OQ-11b-5 | The member Sign up screen (5d) also sends a live guest claim; on expiry it shows no banner (iOS `form.banner == nil`). | iOS parity; no new copy. |
| OQ-11b-6 | iOS throws `CancellationError` when acceptance targets another account or a stale session. | `GuestAcceptanceSuperseded : CancellationException` (4a `SyncEpoch.check()` precedent): the caller ends silently. |
| OQ-11b-7 | Stored form of the continuation (iOS uses Swift `Codable`). | `{"kind":"trip","tripId":…}` / `{"kind":"memberWizard"}` / `{"kind":"trips"}` in settings section `guestContinuation`; a corrupt row is dropped on consume. |
| OQ-11b-8 | Two-factor sign-in drops an unregistered guest draft (no `tripId` from two-factor). | iOS user decision 2026-11-10: My trips; sync surfaces a claimed trip. |
| OQ-11b-9 | Member Sign up has a show-password row; the guest sheet has none. | None, as the board and iOS. |
| OQ-11b-10 | iOS seeds the guest sheet state through a `#if DEBUG` method. | A constructor default `initialGuestState = Ready`; only the debug seed passes another value. |
| OQ-11b-11 | The M3 drag handle slot (48 dp) differs from the board's 5 px grabber + 12 px top padding and from iOS, whose system grabber overlays the content and whose column starts 20 pt (`space5`) below the sheet top. | Phase 2's M3 handle, content top padding 0 below it (title at 48 dp vs 20 pt on iOS); record in `docs/wizard.md`. |
| OQ-11b-12 | iOS 18n's "Valet claim gate" (register → verify → claimed trip in en/nl; password → member seed; two-factor → My trips) needs the user's local backend. | Not run; recorded as open for the user. Mock coverage stands in. |
| OQ-11b-13 | A rotation while the sheet is open loses typed passwords. | Accepted (secrets never saveable); name and email survive. |

---

## 11. Hand-off

- **13/14 (release and acceptance):** the guest journey (`testWelcomeThroughSignupOpensClaimedTrip`) is part of `@Smoke`; the Valet claim gate (OQ-11b-12) waits for the user.
- **Later phases** that add a sign-in method must route through `SignInRouter.acceptedSession` so `GuestSessionAcceptance` runs.
