# Phase 10c: Password, password prompt and two-factor

- **Branch:** `feat/phase-10c-settings-security`. Start it from the top of the stack once **10a** is in it. 10a brings `SettingsPage`, `SettingsDependencies` + `SettingsDependenciesKey`, `SettingsStoredState`, `SettingsDestination.isBuilt`, `SessionSettingsDependencies`, the `Route.*` settings entries in `SettingsEntry` (*[Refresh R8]*), `ShellStart.Settings` and `SettingsSeedRoute.path`. Through 10a the stack also carries 6a (shell), 4e (`SessionController`, `SessionGraph`, `Route.Security`/`Password`/`TwoFactorSettings`/`Passkeys`, `DebugSeedRouting`, the `CF_AUTH_TEST_SIGNED_IN=demo|twofactor` seed), 4d (`SessionSync.accountSecurity`, `SettingsRepository.upsertSecurity`, `StoreApplication`), 4c (`MockDataSource` two-factor rules, `MockScenarioValues`; *[Refresh R6.8]*), 4b (`AccountSecurityChange`/`Result`, `DataSource.accountSecurity`), 5a (`LockoutWait` in `:core:model`, the `SecretField` pattern), 2 (`CfButton`, `CfSecureTextField`, `CfTextField`, `CfCard`, `CfRow`, `CfBadge`, `CfBanner`, `CfNotice`, `CfModalBottomSheet`, `CfAlertDialog`, `CfSnackbarHost`, `CfSpinner`, `@BoardMetrics`), 0c (`CopyIndex`, `CopyTable.Settings`, `android-only.json` with `android_two_factor_intro_b1` and `android_recovery_codes_file_name`), 0d (`DataFailure`, `FieldMessage`, `ApiTwoFactorSetup`, `ApiRecoveryCodes`) and 0e (guards, `BoardCapture`, `ScreenTest`, profiles). If one is missing, stop and report it.
- **Depends on:** 10a. **Wave:** 10. **Plan entry:** `plan.md` "### Phase 10c" (l.3179–3219). Also read in `plan.md`: A23 (l.110, security and privacy), OQ 21 (l.150), "Android-only copy awaiting approval" (l.487–500), OQ-A12, OQ-A13 (l.609–610), OQ-A5 (l.602, Snackbar), OQ-A37 (l.634, 560 dp column), OQ-A462 (l.1058). *[Refresh R1]*
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa`, web at `e36368ec` and the boards at canvas version `1794774555-3fd6`. Before you code, open the merged code of 2, 4b, 4d, 5a and 10a and use **their** names. Where a name in this brief differs, keep theirs and note it in the PR. Never rename earlier code.
- **Tests:** every test follows `AGENTS.md` "Tests": cheapest tier, budgets (JVM and warm Robolectric < 1 s with the AGENTS first-case allowances, emulator < 10 s; *[Refresh R9]*), exact tag queries, `waitUntil`, never `Thread.sleep`, `runTest(timeout = 60.seconds)`. Tier annotations come from 0e (`net.chronoflow.core.testing.tiers`): Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`. Transient UI (snackbars) is asserted through a debug "presented" counter.
- **Tags:** **[F]** fact read in source (path given). **[R]** a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` read with `git -C I show f391a1fa:<path>`. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `W/` = `/Users/rickgout/Sites/chronoflow-new` at `e36368ec`. `DC/` = `_design/canvas/`. Board CSS px map 1:1 to dp.
- **Paths:** `…/fs/` = `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/`; `…/fst/` = `feature/settings/src/test/kotlin/net/chronoflow/feature/settings/`; `…/ds/` = `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/`; `…/ui/` = `core/ui/src/main/kotlin/net/chronoflow/core/ui/`; `…/data/` = `core/data/src/main/kotlin/net/chronoflow/core/data/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/appd/` = `app/src/debug/kotlin/net/chronoflow/debug/`; `…/appt/` = `app/src/test/kotlin/net/chronoflow/app/`; `…/appat/` = `app/src/androidTest/kotlin/net/chronoflow/app/`.
- **Read-only sources:** never write to, build in, run prebuild in, or change the git state of `chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile` or `_design/canvas`.

## Refresh notes (2026-11-21, against a980747f)

Checked against the code at `a980747f` (phases 0a–10b), iOS at `f391a1fa`, `plan.md` and the boards. **Where a note and the body below disagree, the note wins.** The worker repeats these checks first (`git grep` each name) and stops on any mismatch not listed here. Scope and deliverables are unchanged.

**R1. Plan and OQ references.**
- `plan.md` "### Phase 10c" is at **l.3179–3219** (not 2468–2508). Its boxes: Security + password change (l.3197), Password prompt (l.3198), Two-factor (l.3201), Secrets (l.3210), Tests (l.3211). The prompt line "on wide windows a 560 dp dialog" is l.3199.
- Unchanged: A23 l.110, OQ 21 l.150. Moved: "Android-only copy awaiting approval (OQ-A12)" is **l.487–500**; OQ-A5 **l.602**, OQ-A12 **l.609**, OQ-A13 **l.610**, OQ-A37 **l.634**; OQ-A462 (10a's Security-subtitle default) l.1058, OQ-A482 (row disabled alpha 0.5) l.1078.
- The last plan OQ is **OQ-A501** (10b). Task 13 appends §9 as **OQ-A502 … OQ-A518**, in order: OQ-10c-1 → A502, -2 → A503, -3 → A504, -4 → A505, -5 → A506, -6 → A507, -7 → A508, -8 → A509, -9 → A510, -10 → A511, -11 → A512, -12 → A513, -13 → A514, -14 → A515, -15 → A516, -16 → A517, -17 → A518. Each row starts "(10c OQ-10c-n)", as 10a/10b rows do. A new question takes **A519** and up. (This replaces Task 13's "the orchestrator renumbers at restack".)
- OQ-A462 says "10c switches to `settings_password_passkeys_two_factor`". This brief's OQ-10c-12 (→ A513) keeps "Password, two-factor" because passkeys are still debug-only until 10d, and the 10d brief (OQ-10d-12) keeps it too. Keep the brief's default; the A513 row says it supersedes A462's "10c switches" clause. Flag it in the PR.

**R2. What 10a/10b built (reuse it; do not rebuild).** Paths under `…/fs/` unless marked.
- `SettingsDependencies.kt`: `SettingsDependencies` + `SettingsDependenciesKey` + **`SettingsMainKey`** (`CreationExtras.Key<CoroutineDispatcher>`). Members today: `snapshot`, `account()`, `settings()`, `outbox()`, `pendingOutbox()`, `syncEvents()`, `sessionUpdates()`, `isCurrent()`, `signOut(discardPending)`, `enqueueProfile(name, email)`, `requestSync()`, `clock: PlanClock`, `formatter()`, `zone: ZoneId`, `enqueueSettings(section, patch)` (10b). Implementers that must all gain the 10c members: `app/…/settings/SessionSettingsDependencies.kt`, `feature/settings/src/test/…/support/FakeSettingsDependencies.kt`, and the anonymous object in `feature/settings/src/test/…/support/SettingsFixture.kt` (`deps`, l.136).
- `SettingsPagesModel.kt`: `SettingsPagesModel(deps, scope)` with `ui: StateFlow<SettingsPagesState(account, settings, loaded, settingsLoaded, failed)>`, `start()`, `retry()`, `stop()`, and `internal fun SettingsPagesState.section(name): JsonObject?`. `SecurityViewModel` uses it for the settings list, the account email, loaded/failed and Retry (do not hand-roll a `deps.settings()` collector, §5.9). `SecuritySummary.from` may take `pages.section("security")`. Nothing in `:feature:settings` parses the `security` section yet.
- `SettingsAttempt.kt`: `internal suspend fun settingsAttempt(failed, cancelled = failed, block)`. It rethrows a `CancellationException` only when the *calling* coroutine is cancelled; a foreign one (such as `StaleSessionException`) goes to `cancelled`. Commit `b11b3531` passes `cancelled = {}` in `SettingsProfileModel.submit` so a stale session is silent (no error, not busy), tested by `ProfileSubmitFailureTests` ("stale-session") and `SettingsProfileTests.notCurrentSessionDoesNotSaveOrShowAnError`. `SettingsSecurityModel.run` keeps its own catch order (§5.3 step 3; it must not catch `RuntimeException` broadly as `settingsAttempt` does); the ViewModel may use `settingsAttempt` around non-model work (the Download write, the seeded prompt).
- ViewModel pattern (`profile/ProfileViewModel.kt`, `language/LanguageSettingsViewModel.kt`): `class XViewModel(deps, …, main: CoroutineDispatcher) : ViewModel(CoroutineScope(SupervisorJob() + main))`; `Factory = viewModelFactory { initializer { X(checkNotNull(this[SettingsDependenciesKey]), this[SettingsMainKey] ?: Dispatchers.Main.immediate) } }`; `pages.start()` in `init`, `pages.stop()` in `onCleared()`. Screens are a stateless `XScreen(state, onEvent)` plus an `XRoute(viewModel, onBack, …)` wrapper.
- `components/SettingsPage.kt`: `SettingsPage(title: Int /* string res */, onBack: (() -> Unit)?, testTag: String, modifier, content: ColumnScope.() -> Unit)` and the lazy twin `SettingsLazyPage(title, onBack, testTag, listState, modifier, content: LazyListScope.() -> Unit)`. Gap is fixed at `space6` (24): **there is no `spacing` parameter**, so Task 8 adds `spacing: Dp = PlanUITokens.space6.dp` to `SettingsPage` (Hand-in 6). Wide windows use `currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)` and `SettingsWideMetrics.COLUMN` (560), applied **inside** the 16 dp side padding. `SettingsWideMetrics` already carries `@BoardMetrics("Set2FAWide", "Set2FAManageWide")`; reuse it, do not redeclare the 560.
- `components/SettingsStoredState.kt`: `SettingsStoredState(loaded, failed, onRetry, modifier)` (tags `settings.loading`, `settings.missing`, `settings.stored.retry`).
- `components/SettingsFields.kt` (commit `d838ffbf`): `SettingsOfflineCard(modifier, content)` draws a rows `CfCard` (tag `settings.offline.card`) and the private `SettingsNeedsConnection()` line ("Needs a connection.", tag `settings.needsConnection`) with the 12 dp (`space3`) gap the card+hint pair owns; `SettingsOfflineHint(text)` is the cloud-off hint. 10c shows no offline-disabled card (§3, iOS disables none here); its "Needs a connection." is a model `message`. If any 10c surface does need the offline card+hint, call `SettingsOfflineCard`; never re-implement the pair or its gap.
- `components/SettingsPreferenceComponents.kt:43`: `internal fun settingsText(key): String` (CopyIndex Settings lookup, raw-key fallback, 10b OQ-10b-12 → OQ-A495). `securityText` (§5.4) builds on it: add the args overload next to it rather than a second lookup.
- `home/SettingsHomeScreen.kt`: `enum class SettingsDestination(route, glyph, title, subtitle, tag, isBuilt = false)`. `Security` (l.90–96) has no `isBuilt` argument; flip it by passing `true`, as `Account`/`BodyClock`/`Preferences` do. Update `home/SettingsHomeScreenTest.kt`'s release-visibility expectation.
- Debug counter: `SettingsDebugCounters` already exists in `feature/settings/src/debug/…` (an `AtomicInteger languageSaved` + `presented()`) with a release no-op twin in `src/release/…`. Add a security counter to both (e.g. `securityFeedback` + `securityPresented()`) instead of a new `SecurityDebug` object.
- Test support (`feature/settings/src/test/…`): `support/SettingsScreenTest` (Robolectric base with `ProfileRule`, `show { }`, warm-up), `support/FakeSettingsDependencies`, `support/SettingsFixture` (real `SessionAccountDatabase` on `JdbcAccountDriverFactory`, `SessionController`, `SessionSync`, `MockDataSource`, `permit`, `FakePlanClock(settingsNow)`), `SettingsBoardFixture`, `SettingsCaptureTest` (uses `BoardCapture.capture(compose, name)` and, for sheets/dialogs, `BoardCapture.captureWindow(compose, name, tag)` from `core/testing-compose/…/BoardWindowCapture.kt`), `SettingsGuardsTest`.

**R3. One-shot events (how 10a/10b do it; §5.10 is rewritten to match).** No `Channel`/`SharedFlow` consumed before handling, and no `LaunchedEffect` keyed on a changing value to fire a one-shot. The ViewModel holds the event in its UI state with an id until the screen acknowledges it: `LanguageUiState.notice: LanguageNotice(id, choice)?` + `presented(id)`, `recreate: Long?` + `recreated(id)` (`language/LanguageSettingsViewModel.kt`); `BodyClockUiState.scroll` + `BodyClockEvent.ScrollHandled(id)` (10b). The route collects in a `LaunchedEffect` keyed on stable objects (`viewModel`, the host state, `resources`): `viewModel.ui.map { it.notice }.filterNotNull().distinctUntilChanged().collect { … }`, and `CfSnackbarHost(onPresented = { SettingsDebugCounters.presented(); viewModel.presented(id) })` acknowledges (`language/LanguageScreen.kt:100–145`). Acknowledging clears the event only when the id still matches.

**R4. Secrets and stale sessions.**
- `StaleSessionException` is `core/model/…/session/StaleSessionException.kt`: `class StaleSessionException : CancellationException("stale session")` (Hand-in 3 confirmed). `SessionMutationPermit.whileValid` throws it for a revoked permit (so `revokedPermitWritesNothing` expects `StaleSessionException`). `SessionSync.accountSecurity` throws a plain `CancellationException()` when the session/epoch is not current, before and after the call; Task 10 maps that to `StaleSessionException` exactly as the body says.
- Secrets never go into a `Bundle`, `SavedStateHandle`, `rememberSaveable`, a route key, the DB, the outbox, a log or a `toString()`. The `AccountSecurityChange` secret cases already redact (`Password(***)`, `EnableTwoFactor(***)`, …; `RequestPasswordReset(email)` does not, email is not a secret); `ApiTwoFactorSetup` and `ApiRecoveryCodes` redact too (`core/model/…/api/AccountSecurityContracts.kt`, tested by `core/model/src/test/…/api/SecretRedactionTest.kt`). `SecurityState.toString()` and `QrMatrix.toString()` must redact as well.
- Konsist `secureFieldStateIsNeverSaved` (`konsist/src/test/…/DesignSystemRules.kt:145`) only flags files containing `CfSecureTextField(` that also contain `rememberTextFieldState(`, `rememberSaveable` or `TextFieldState.Saver`. It does not see `BasicSecureTextField(` (the prompt), the `CfTextField` code field, or `SavedStateHandle`; review those by hand (§7's Konsist line is limited to what the rule checks).

**R5. Security API (all exist; 10c adds no network call).**
- `net.chronoflow.core.model.api.AccountSecurityChange` cases: `RequestPasswordReset(email)`, `Password(current, password, confirmation)`, `EnableTwoFactor(current)`, `ConfirmTwoFactor(code)`, `DisableTwoFactor(current)`, `RevealRecoveryCodes(current)`, `RegenerateRecoveryCodes(current)`, `ListPasskeys`, `PasskeyCreationOptions(current)`, `AddPasskey(input)`, `RemovePasskey(id, current)`. `AccountSecurityResult`: `Changed`, `Setup(setup: ApiTwoFactorSetup)`, `Codes(codes: ApiRecoveryCodes)` (so `codes.recoveryCodes`), `Snapshot(snapshot: AccountSnapshot)`, `Passkeys(passkeys)`, `PasskeyCreation(challenge)`, `PasskeyAdded(passkey)`.
- `DataSource.accountSecurity(change)` (`core/model/…/DataSource.kt:52`), `SyncEngine.accountSecurity(change)` and `SessionSync.accountSecurity(change, session: StoredSession, epoch: Long)` (`core/sync/…/SessionSync.kt:179`). `SessionSettingsDependencies` reaches it as `session.sync.accountSecurity(change, snapshot.session, snapshot.epoch)` (`SessionGraph.sync`, `SessionSnapshot(session, epoch)`), the permit as `app.controller.mutationPermit(snapshot)`, and does its work on `app.dispatchers.io`, as `enqueueSettings` does.
- `DataFailure` (`core/model/…/DataFailure.kt`): `Validation(fields: Map<String, FieldMessage>, status)`, `Throttled(retryAfterSeconds: Double, status)`, `Offline`, `Server(status: Int?)`, `Conflict(status)`, `Unauthorized`, …; `FieldMessage(key, arguments: Map<String, JsonPrimitive>)`. `core/network/…/ApiErrorMapper.kt:89–94` maps the three 409 sentences to `Validation(mapOf("form" to FieldMessage(message)), 409)`, other 409s to `Conflict`.
- `SettingsRepository.upsertSecurity(value: JsonObject, permit: SessionMutationPermit)` exists (`core/data/…/repository/SettingsRepository.kt:53`). There is **no public way** to apply an `AccountSnapshot` under a permit: `StoreApplication` is `internal object` (`core/data/…/store/StoreApplication.kt:35`, `snapshot(value, accountID, sql)` l.56) and only `SqlSyncDatabase.applyPull` calls it. So Task 4's `AccountSecurityStore` is needed. `SessionMutationPermit` carries no account id: take it from `AccountDatabase.accountID` (or pass it in), and write with `database.write(permit) { … }` (`core/database/…/AccountDatabase.kt:82`).
- **10c adds** (none exist yet): `SettingsDependencies.performSecurity(change)`, `passwordPromptSeed`, `clipboard` (§5.14 injects `SensitiveClipboard` through it; §5.9's list omits it), `AccountSecurityStore`, `SecretField`/`TextFieldSecret` in `:feature:settings`, `SecureWindow`, `SensitiveClipboard`, `QrMatrix`, the ZXing and Espresso-Intents dependencies.

**R6. Other hand-ins (results of §4).**
1. Confirmed (R5). 2. Confirmed; Task 4 stays (R5). 3. Confirmed (R4). 4. Confirmed: `LockoutWait(seconds)` is `core/model/…/auth/LockoutWait.kt` (`unit`, `count` rounded up, `localizationKey` = the `%lld seconds|minutes|hours` iOS keys); 5a's `SecretField` is `feature/auth/…/model/SecretField.kt` and `TextFieldSecret` is `feature/auth/…/shell/TextFieldSecret.kt`. 5. Confirmed: `CopyIndex.find(table, key): CopyEntry?` with `CopyEntry(resId, isPlural, args: List<String>)` (`core/ui/…/l10n/generated/CopyIndex.generated.kt`). Every `settings_*`/`android_*` name in §5.12 exists in `core/ui/src/main/res/values/`. The three lockout keys map to `R.plurals.settings_too_many_attempts_try_again_in_n_086dac` (seconds), `…_e0c4d8` (minutes), `…_n_hours` with `args = ["%1"]`; `"Use at least {{min}} characters."` → `settings_use_at_least_min_characters`, `args = ["min"]`. **0c maps replaced keys:** `CopyIndex.find(CopyTable.Settings, "You need an authenticator app, like the one built into Passwords on iPhone.")` returns `R.string.android_two_factor_intro_b1`, so look b1 up through `CopyIndex` (§5.4's fallback is not needed). Strings are `net.chronoflow.core.ui.R`. 6. `SettingsPage` has no `spacing` (R2). 7. `LucideGlyphs` has `Lock`, `KeyRound`, `Copy`, `CircleAlert`, `Check`, `ChevronRight`; **only `Smartphone` is missing**. The glyph source is `tools/icons/lucide.json`, regenerated with **`make vectors`** (`tools/icons/generate_icons.py`; `make icons` builds launcher assets). 8. `MockDataSource` has the two-factor rules (`core/data-mock/…/MockDataSource.kt` ~l.1610–1700: secret `JBSWY3DPEHPK3PXP`, code `123456`); `AccountSecurityAPITests.mockRunsSetupCodesManageAndPasswordAndRejectsOffline` is `core/data-mock/src/test/…/AccountSecurityAPITests.kt:16`. Seed values are `MockScenarioValues` (`core/data-mock/src/main/…/MockScenarioValues.kt`: `password = "password"`, `twoFactorCode = "123456"`, `demoEmail`, `twoFactorEmail`); there is **no `MockScenarios.fixturePassword`**. Seed accounts (`MockAppSeed.kt`): demo `twoFactorEnabled false`, `twoFactorConfirmedAt null`, `passkeyCount 2`, `passwordRules "minlength: 10;"`; twofactor `twoFactorEnabled true`, `twoFactorConfirmedAt = now`, `passkeyCount 0`.

**R7. Design-system facts (names differ from the body).**
- `CfButton` **already has** `disabledAlpha: Float = AuthTokens.disabledOpacity` (and `loading`, `loadingDescription` (required when loading), `leadingIcon`, `fillWidth`, `minHeight`, `textStyle`, `size: CfButtonSize`); `CfButtonStyle` has `Primary, Secondary, Outline, Ghost, GhostDestructive, Destructive, Dawn, OnSky`. Task 2 drops the `disabledAlpha` part and its test; it only adds `smartphone`.
- `CfRow(title, modifier, subtitle, icon: ImageVector?, badge: String?, badgeDawn: Boolean, chevron = true, lock = false, destructive = false, enabled = true, onClick, testTag)` is the iOS `SettingsRow` port: a 36 dp `muted` disc when `icon` is set, a **trailing** `CfBadge` (`Dawn` when `badgeDawn`, else `Secondary`), the `lock` glyph (tag `cf.row.lock`), the chevron (tag `cf.row.chevron`). Use it for all three Security rows (`icon = LucideGlyphs.Lock/KeyRound/Smartphone.imageVector()`, two-factor `badge = On|Off`, `badgeDawn = enabled`) and for the 2FA action rows. Do **not** use 10a's `SettingsDestinationRow` here: its badge sits under the subtitle and is Dawn-only.
- `CfBadge(text, modifier, variant: CfBadgeVariant = Secondary, icon)`; variants `Default, Secondary, Outline, Dawn`. `CfCard(modifier, rows = false, shadow = true, border = true, spacing: Dp = space4, content)`; `CfDivider()`. `CfBanner(title, message, modifier, actionLabel, onAction, announce = true, testTag, actionTestTag)`. `CfNotice(title, message, modifier, tone: CfNoticeTone = Info)` with `CfNoticeTone.Warning`. `CfTextField`/`CfSecureTextField(state, label, modifier, testTag, …, error, enabled, keyboardOptions, onKeyboardAction, contentType, inputTransformation)` take the tag as a **`testTag` parameter**; `CfSecureTextField` has no `placeholder`.
- `CfModalBottomSheet(onDismissRequest, modifier, sheetState, content)` (`core/designsystem/…/system/CfDialogs.kt:37`): shape is already `CfTheme.shapes.sheetTop` (= `SettingsTokens.sheetRadius` 38), container `card`, M3 drag handle, `sheetMaxWidth` 640. It has **no `properties` parameter**, so `ModalBottomSheetProperties(shouldDismissOnBackPress = !busy)` cannot be passed: Task 7 adds an optional `properties: ModalBottomSheetProperties = ModalBottomSheetProperties()` parameter (additive, existing callers unchanged), or guards back with the `sheetState` `confirmValueChange` and a no-op `onDismissRequest` while busy; state which in the PR.
- `CfAlertDialog(title, text, confirm: CfDialogAction, onDismissRequest, dismiss: CfDialogAction? = null)` has **no modifier or tag parameter**; its buttons are tagged `cf.dialog.confirm` / `cf.dialog.dismiss`. Either add an optional `testTag` (additive) for `"$identifier.forgotAlert"`, or have the tests use `cf.dialog.confirm`/`cf.dialog.dismiss`. `CfDialogAction(label, onClick, destructive = false)`.
- `CfSnackbarHost(hostState, modifier, onPresented: (SnackbarData) -> Unit = {})`.
- Tokens: `SettingsTokens.dimmedFieldOpacity` 0.55, `disabledOpacity` 0.45, `promptButtonHeight` 50, `promptFieldHeight` 52, `qrSize` 168, `sheetRadius` 38, `selectionWidth` 2, `pillGap` 6, `pageBottom` 40, `rowHeight` 64; `AuthTokens.disabledOpacity` 0.5; `PlanUITokens.radiusMd` 12, `radiusLg` 14, `actionMinHeight` 48, `walkthroughDoneHeight` 40, `space1…space8`. Colors `surface2`, `settingsQrBackground` (white in both), `dawnText`; `CfLightColors` is a top-level `val` in `generated/CfColors.generated.kt`. Type: `CfTheme.type` exposes `settingsRow`, `screenBody`, `screenLabel`, `authLabel`, …; reach `settingsStrong`, `settingsPromptBody`, `settingsCodes`, `settingsHint`, `settingsEyebrow`, `screenBodyLg`, `screenSectionTitle` through `CfTheme.type.style(CfTypeScale.<name>)` (as `SettingsFields.kt` does).

**R8. App wiring (§5.15 and Task 10).**
- Settings routes are rendered by `app/…/settings/SettingsEntry.kt` (`SettingsEntry(graph, session, route, onBack, onNavigate, modifier)`), not directly by `AppShell`. Add `Route.Security`, `Route.Password`, `Route.TwoFactorSettings` branches to its `when` (and titles to its `session == null` loading page), and add the three routes to `AppShell`'s `page` condition (`app/…/shell/AppShell.kt` ~l.110, the `route == Route.Account || …` list). `Route.Passkeys` keeps falling through to `render.value(route)` (`RoutePlaceholder`).
- The debug label hook is `LaunchPlatform.routeLabel` (a per-build-type object: `app/src/debug/…/app/LaunchPlatform.kt`, `app/src/release/…/app/LaunchPlatform.kt`); `SettingsEntry` passes `LaunchPlatform.routeLabel != null` to `SettingsHomeRoute` as "show unbuilt". Pass the same flag to the Security page for the Passkeys row (there is no `PasskeysRow.isBuilt`; a constant in 10c is fine).
- `passwordPromptSeed` goes through the same per-build-type seam: add it to both `LaunchPlatform` objects (debug: `MockScenarioValues.password` when `DebugSeedRouting.current?.settingsRoute == "twoFactorSetup"`, else null; release: null); `SessionSettingsDependencies` reads it. `:app` has `debugImplementation(project(":core:data-mock"))`. `StaticSecret` lives in `src/debug` only if nothing in `main` names it; otherwise make it a `main` `SecretField` in `:feature:settings` that holds no state beyond the call.
- `SettingsSeedRoute.path` (`app/…/settings/SettingsSeedRoute.kt`) maps `password` → `[Security, Password]` and `twoFactor`/`twoFactorSetup` → `[Security, TwoFactorSettings]` (confirmed). `TestSeed` also accepts `twoFactorSettings`, which `path` does not map (returns null); leave it.
- **Task 10's JVM port location.** `SessionSettingsDependencies` is `internal` to `:app` and needs a whole `AppGraph` + `SessionGraph`; 10a's "real parts" test pattern lives in `:feature:settings` (`support/SettingsFixture`, used by `home/SettingsHomeModelTests`, `profile/SettingsProfileTests`). Put `disableSnapshotClearsSecretsAndObservedEnabledStateForIntroduction` and `staleSessionThrowsStaleSessionException` in `feature/settings/src/test/…/security/SessionSettingsSecurityTests.kt`, with `SettingsFixture.deps.performSecurity` mirroring `SessionSettingsDependencies.performSecurity` line for line (as its `enqueueSettings` mirrors the app's), and `:feature:settings` already depends on `:core:data` for `AccountSecurityStore`. Keep the names; update §7's `make targeted` line to `feature:settings:SessionSettingsSecurityTests`. `ShellNavSettingsSeedTest` is `app/src/test/…/settings/ShellNavSettingsSeedTest.kt` (exists). Seed/debug tests live in `app/src/testDebug/…` (e.g. `debug/SettingsShellStartTest.kt`); a debug-only seed assertion goes there.
- `CopyLineFormatter.formatDate(value: String, language: Language, zone: String = "UTC")` takes the zone **id string**: pass `deps.zone.id`; `deps.formatter()` is `suspend`.
- **Espresso-Intents is not in the catalog** (`gradle/libs.versions.toml`, `app/build.gradle.kts` androidTest deps). Task 12 adds `androidx.test.espresso:espresso-intents` (a stable release at least two weeks old) and `androidTestImplementation(libs…)` in its commit.
- Third-party notices: `docs/third-party.md` exists; Task 1 adds "ZXing core, Apache-2.0" there.
- `:feature:settings/build.gradle.kts` depends on `:core:plan`, `:core:sync`, `:core:data`, `:core:wizard`, `androidx.activity.compose`; tests on `:core:testing`, `:core:testing-junit`, `:core:testing-compose`, `:core:data-mock`, `:core:database-testing`, coroutines-test, turbine.

**R9. Tests and budgets.** AGENTS "Tests" 5 governs: a JVM or warm Robolectric case < 1 s (first Robolectric case per SDK 20 s, first per class/SDK 3 s, recorded separately), emulator < 10 s. Robolectric screen tests extend `support/SettingsScreenTest`. Tier annotations are `net.chronoflow.core.testing.tiers.{Logic, Layout, Capture, Smoke, TimingGate}`; `BoardCapture`/`Guards` are `net.chronoflow.core.testing.compose`; the emulator base is `app/src/androidTest/…/app/testing/ScreenTest.kt`.

**R10. iOS at the pin.** Every iOS path in §2.3 exists at `f391a1fa` (`SettingsSecurityModel.swift` 248 lines, `SettingsSecurityView.swift` 390, `SettingsPasswordPrompt.swift` 112, `SettingsBoardComponents.swift` 361, `SettingsRecoveryDocument.swift` 14, `SettingsPagesModel.swift` 144, `SettingsStrings.swift` 21, `AppCore/LockoutWait.swift` 29, `SettingsTokens.generated.swift` 64; `SettingsSecurityTests.swift` has the 9 tests at l.10, 29, 66, 120, 140, 158, 201, 213, 229; `SettingsSecurityPagesTests` is `AppUITests/SettingsPagesTests.swift` l.146–184; `I/plan.md:1993` and `:2040` are the cited headings; `AppTabs.swift` l.155–162 and l.245–262 are the cited seams; `SettingsHomeView.swift:22` is "Password, two-factor"). iOS `SettingsRow` puts the badge trailing (matches `CfRow`). All 20 boards exist in `_design/canvas/`.

Precedence when sources disagree: **iOS behaviour** first, **board looks** second, this brief's [R] rules third. Never invent a screen, row or copy line that has no board and no iOS string. Every visible string in this phase already exists in the generated catalog (§5.12). If one is missing, stop and report it; do not add copy.

---

## 1. Goal

Settings → **Security** becomes a real page: Password, Passkeys (only when the server reports `passkeyCount`, and only in debug until 10d) and Two-factor authentication with an On/Off badge.

**Password** changes the password through `PUT /v1/account/password`, with the iOS client checks (empty, mismatch), the server policy errors mapped to the iOS copy, the warning notice, a throttle countdown and the "Password updated" feedback.

**Two-factor authentication** shows the intro when off. **Turn on two-factor** asks for the password in the shared **Confirm it's you** prompt, then shows setup: a QR code drawn on the device from `otpauthUrl` on a white tile, the setup key with Copy, and the 6-digit code field. Confirming shows the 8 recovery codes with Copy and Download and the toast "Two-factor is on. Save your recovery codes now.". When on, the page shows the On badge with "Since {date}", Show recovery codes, Make new codes and Turn off two-factor, each behind the password prompt.

The **password prompt** is one reusable component (10d reuses it for delete account and passkeys). It is a bottom sheet on phones and a centred dialog on windows ≥ 600 dp, and it covers every state: empty, checking, wrong password, locked with a live countdown, Forgot password? (alert → reset link), offline and activation failure.

Secrets never leave memory: passwords and the 6-digit code live in `remember { TextFieldState() }` and are read once and cleared; the setup key, QR and recovery codes live only in the ViewModel and are cleared when the page leaves composition. The setup and codes states set `FLAG_SECURE`. Copy puts a sensitive clip on the clipboard and clears it after 60 s. Download saves "Recovery codes.txt" through the system document picker.

Done means:
- `SettingsSecurityModel` is ported with all 9 iOS `SettingsSecurityTests` on the JVM, names kept (7 model tests in Task 5, `secretsStayOutOfSQLiteAndPendingBodyClockSurvivesSecuritySnapshot` in Task 4, `disableSnapshotClearsSecretsAndObservedEnabledStateForIntroduction` in Task 10), plus the Android edge tests;
- all 20 boards are guarded on Robolectric (5 profiles × en/nl × light/dark) and captured;
- a Robolectric recreation test proves no secret is in the saved `Bundle`;
- `FLAG_SECURE` is proven on Robolectric and on the `pixel` emulator;
- one emulator journey runs seeded setup → confirm → recovery codes → Download.

---

## 2. Inputs to read (in this order)

### 2.1 This repo

1. `AGENTS.md` (all of it, especially "Secrets", "Tests", "Traps").
2. `plan.md` sections in the header.
3. `docs/research/07-day-to-day-settings.md` §6.4 (Security, Password, Two-factor, shared password prompt, recovery codes Copy/Download), §11 (API table), §14 OQ-7, OQ-11, OQ-12.
4. `docs/research/05-launch-auth.md` §13 (prompt summary), §15.13 (secrets, process death).
5. `docs/research/03-api-contract.md` §3 (error envelope, the three 409 two-factor sentences), §4 (`api-credential` 6/min + 20/h shared budget; `api-two-factor-confirm` 5/min + 10/h), §5.2 rows 19–24, §7 (two-factor; draw the QR from `otpauthUrl`).
6. `docs/research/14-gaps.md` §1.2 (`PasswordPromptWide`, `Set2FAWide`, `Set2FAManageWide`), §1.3 (recovery codes Download), §3.2–§3.4 (state survival: prompt reopens empty after recreation; setup and codes kept in the ViewModel; gone after process death).
7. Earlier briefs and their merged code: `phase-10a-settings-home.md` (§5.3 `SettingsDependencies`, §5.6 `SettingsStoredState`, §5.7 `SettingsPage`, §5.10 `SettingsSeedRoute`), `phase-5a-sign-in.md` §5.5 and l.177 (`SecretField`, `TextFieldSecret`), l.220 (`LockoutWait`), `phase-4b-api-client.md` l.163–187 and rows 13a–13g, `phase-4c-mock-data-source.md` l.318 (mock two-factor rules) and l.341, `phase-4d-sync.md` l.293 (`upsertSecurity`), l.275 (`StoreApplication.snapshot`), l.408 (`SessionSync.accountSecurity`), `phase-2-design-system.md` Tasks 11–13 and l.495–497 (sheet, dialog, snackbar), `phase-0c-strings.md` (slug rule, `CopyIndex`, `android-only.json`), `phase-0e-test-harness.md` (profiles, `Guards`, `BoardCapture`, `ScreenTest`).

### 2.2 Boards (`DC/`, read-only; `*.dc.html`)

| Board | Size | What it fixes |
|---|---|---|
| `SetSecurity` | 375 × 812 | Page title "Security"; one rows card: Password (`lock`, "Change your password"), Passkeys (`key-round`, "2 passkeys"), Two-factor authentication (`smartphone`, "A code from an app when you sign in", Secondary badge "Off"). Rows 64 min, padding 10/16, gap 12, 36 disc, chevron 20. |
| `SetPassword` | 375 × 812 | Warning notice "Check the highlighted field." / "Your new password isn't saved yet."; card with Current / New / Confirm new password (error "The passwords don't match." on Confirm); full-width "Change password". Main padding 4/16/40, gap 24. |
| `PasswordPrompt` | 375 × 812 | The prompt. Props `action = email|delete|twofaOn|twofaOff|newCodes|showCodes` (default `showCodes`), `state = empty|checking|wrong|locked`, `lang`, `forgot`. Sheet: radius 38 top, padding 10/20/32, gap 14, grabber 36 × 5; header row min 44: Cancel (start, `dawn-text`) + centred title 17/22 600; description 15/22 muted; field 52 high, radius 12, padding 0 16, gap 10, trailing 18 dp `lock`; wrong → 2 dp inset `destructive` ring; checking/locked → field opacity 0.55; message row (16 dp `circle-alert`, gap 6, 14/20 `destructive`); "Forgot password?" link min 40, 14/20 500 underlined; button 50 high, 16/600, primary (or destructive for `delete`/`twofaOff`), opacity 0.45 when empty/checking/locked, spinner 16 + "Checking…" while checking. Scrim `rgba(0,0,0,.25)`. Copy table embedded in the board script. |
| `PasswordPrompt2FAOn` | wrapper | `action=twofaOn` (Continue) over `Set2FA` |
| `PasswordPromptChecking` | wrapper | `action=email state=checking` (see OQ-10c-3) |
| `PasswordPromptForgot` | wrapper | `action=email forgot` → alert "Reset your password?" / body / Cancel, Send link (OQ-10c-3) |
| `PasswordPromptLocked` | wrapper | `action=delete state=locked` → "Too many attempts. Try again in 47 minutes." (OQ-10c-3) |
| `PasswordPromptNewCodes` | wrapper | `action=newCodes` (Make new codes) over `Set2FAManage` |
| `PasswordPromptNL` | wrapper | `action=newCodes state=wrong lang=nl` |
| `PasswordPromptWide` | 669 × 951 | Same prompt as a centred dialog: `width: 540px; border-radius: 38px`, no grabber, centred both ways (OQ-10c-1) |
| `PasswordPromptWrong` | wrapper | `action=twofaOff state=wrong` (destructive "Turn off") over `Set2FAManage` |
| `Set2FA` | 375 × 812 | Off intro: title + Secondary badge "Off" (gap 8); card (gap 14): body 16/24, 3 bullets 14/20 muted (gap 6, list indent 20), primary button 48 "Turn on two-factor", centred lock hint 13/18 "You'll be asked for your password first." Main gap 20. Copy table for every 2FA board is in this file's script. |
| `Set2FANL` | wrapper | `Set2FA lang=nl` |
| `Set2FASetup` | 375 × 812 | Intro 16/24 muted; card 1 (gap 16): step "1 · Scan this code…" 14/20 600 with the digit in `dawn-text`, QR tile 168 × 168, radius 14, 1 dp `border`, white fill, centred; hint 14/20 muted; key row (`surface-2`, radius 14, padding 6 6 6 14, gap 8): monospace 15/20 key + Outline "Copy" with a 16 dp `copy` glyph; card 2: step 2 + field "6-digit code", numeric, placeholder 123456, maxlength 6; primary "Turn on two-factor". |
| `Set2FAOn` | 375 × 812 | `state=fresh|shown`. Title + Dawn badge "On" + "Since 30 Oct 2026" (14/20 muted, tabular). Codes card (gap 16): "Recovery codes" Fraunces 20/28, body 14/20 muted (gap 4); grid on `surface-2`, radius 14, padding 12, 2 columns, gap 8 × 12, monospace 15/22 centred, 8 codes; two Outline lg buttons Copy / Download (gap 8). Rows card: Make new codes, Turn off two-factor (destructive title), each with a 16 dp `lock` and an 18 dp chevron; lock hint "Each of these asks for your password first." 13/18 muted, start-aligned, margin 0 4. `fresh` adds the toast (popover, radius 22, min 52, dawn 24 dp check disc) "Two-factor is on. Save your recovery codes now." |
| `Set2FACodes` | wrapper | `Set2FAOn state=shown` (no toast) |
| `Set2FAManage` | 375 × 812 | On, no codes: badge row; section (gap 8): eyebrow "RECOVERY CODES" 14/20 600 tracking 0.05em uppercase muted (margin 0 4) + card with "Show recovery codes" / "Each code works once and is then replaced"; then the Make new codes / Turn off card and the lock hint. |
| `Set2FAManageNL` | wrapper | `Set2FAManage lang=nl` |
| `Set2FAManageWide` | 669 × 951 | `Set2FAManage` with `main { max-width: 560px; margin: 0 auto }` |
| `Set2FAWide` | 669 × 951 | `Set2FA` with the same 560 column |

`Set2FA`, `Set2FAOn` and `Set2FAManage` draw `main` padding `4 16 120` (`Set2FASetup` draws `4 16 40`); the 120 is the overlaid iOS tab bar and is **not** reproduced (10a OQ-10a-5: 40).

### 2.3 iOS (`f391a1fa`): behaviour to mirror

- `Kit/Settings/SettingsSecurityModel.swift` (whole file, 248 lines): `SettingsSecurityPrompt` (cases `enable, disable, reveal, regenerate, addPasskey, removePasskey`, `detail`, `button`, `change(password:)`), `changePassword()`, `showPrompt`, `cancelPrompt`, `submitPrompt`, `confirm()`, `run(_:)` with its error mapping (l.139–218), `failureMessage` (l.221–227), `sendReset(email:)`, `clearSecrets()`, `generation`, `retryAt`, `locked`, `remaining(at:)`, `refreshLock()`.
- `Kit/Settings/SettingsSecurityView.swift`: `SettingsSecurityView` (rows, l.15–51), `SettingsPasswordView` (l.54–96), `SettingsTwoFactorView` (intro l.138–158, setup l.165–205, manage/codes l.212–280, `promptButton` l.281–287, `copy` l.288–292), `SettingsSecurityFeedback` (l.295–310, which messages are "success"), `SettingsSecurityLock` (l.311–324), `SettingsAuthenticatorQR` (l.325–355), `refreshLock` (l.356–361), `SettingsSecurityUITestPage` (DEBUG, l.363–389).
- `Kit/Settings/SettingsPasswordPrompt.swift` (whole file): layout, `alert = remaining > 0 ? lockout : error`, disabled rules, feedback vs `AuthBanner` for "Couldn't turn on two-factor.", Forgot alert.
- `Kit/Settings/SettingsBoardComponents.swift`: `SettingsCompactSheet` (l.256–296: inline title, Cancel disabled while busy, `interactiveDismissDisabled(busy)`, padding 20/16/32, gap 16), `SettingsLockHint`, `SettingsError`, `SettingsCodeText`, `SettingsBadge`, `SettingsEyebrow`.
- `Kit/Settings/SettingsRecoveryDocument.swift` (UTF-8 text, codes joined by `\n`, name `t("Recovery codes") + ".txt"`).
- `Kit/Settings/SettingsPagesModel.swift` l.112–135 (`performSecurity`: currency checks, snapshot write, `upsertSecurity` with `twoFactorEnabled = true` and `twoFactorConfirmedAt = null` after a confirm, `requestSync()`).
- `Kit/Settings/SettingsStrings.swift` (`lockout(seconds:)`), `Kit/AppCore/LockoutWait.swift`.
- `Kit/DesignSystem/SettingsTokens.generated.swift` (`dimmedFieldOpacity 0.55`, `disabledOpacity 0.45`, `promptButtonHeight 50`, `promptFieldHeight 52`, `qrSize 168`, `sheetRadius 38`, `selectionWidth 2`, `pillGap 6`; styles `settingsPromptBody 15/22`, `settingsCodes 15/22`, `settingsHint 13/18`, `settingsEyebrow 14/20 600 tracking 0.7`).
- `I/App/AppTabs.swift` l.155–162, l.245–262 (`CF_SETTINGS_ROUTE` `password`/`twoFactor`/`twoFactorSetup`).
- Tests: `Packages/ChronoFlowKit/Tests/SettingsTests/SettingsSecurityTests.swift` (9 tests: 7 on `SettingsSecurityModel` → Task 5; l.229–250 → Task 4; l.66–118 → Task 10), `Tests/AppCoreTests/LockoutWaitTests.swift` (5a ported it), UI tests `I/AppUITests/SettingsPagesTests.swift` l.145–184 (`SettingsSecurityPagesTests`), `I/AppUITests/SettingsBoardCaptureTests.swift` l.139–185.
- `I/plan.md:1993` "17h: Settings pages" (Security bullet; "Fix Security re-entry (2026-11-10)": request generations), `I/plan.md:2040` "Settings board fidelity (2026-11-09)" (Security bullet: shared sensitive-action sheet, QR white in both appearances, memory-only codes).

### 2.4 API (web `e36368ec`; all through 4b's `DataSource.accountSecurity`, never a new client)

All rows except `RequestPasswordReset` are bearer + `verified` + `throttle:api` (`forgot-password` is a guest route with only `throttle:api-password-email`, `W/routes/api.php:45`); `api-credential` = 6/min + 20/h (the hourly cap counts 422s only), one budget shared with `PATCH users/me`, `DELETE account` and passkey management [F `W/app/Providers/AppServiceProvider.php:105-228`, `03 §4`].

| `AccountSecurityChange` | Method + path | Body | Success → `AccountSecurityResult` | Errors |
|---|---|---|---|---|
| `Password(current, password, confirmation)` | `PUT /v1/account/password` | `{current_password, password, password_confirmation}` | 204 → `Changed`. Keeps the calling token, revokes the others and web sessions. | 422 `current_password` "The provided password was incorrect."; policy keys on `password` |
| `EnableTwoFactor(current)` | `POST /v1/account/two-factor` | `{current_password}` | 200 `{secretKey, otpauthUrl}` (`no-store`) → `Setup` | 422 `current_password`; 409 "Two-factor authentication is already on." |
| `ConfirmTwoFactor(code)` | `POST /v1/account/two-factor/confirmation` | `{code}` (6 digits) | 200 `{recoveryCodes: string[]}` → `Codes`. Revokes the other tokens. | 422 `code` "The authentication code was not accepted. Try again."; 409 "Start two-factor setup again."; 409 "Two-factor authentication is already on." (`W/app/Actions/Api/ConfirmMobileTwoFactor.php:24`); `api-two-factor-confirm` 5/min + 10/h |
| `DisableTwoFactor(current)` | `DELETE /v1/account/two-factor` | `{current_password}` | 200 `ApiAccountSnapshot` → `Snapshot` | 422 `current_password` |
| `RevealRecoveryCodes(current)` | `POST /v1/account/two-factor/recovery-codes/reveal` | `{current_password}` | 200 → `Codes` (current set) | 422 `current_password`; 409 "Two-factor authentication is off." |
| `RegenerateRecoveryCodes(current)` | `POST /v1/account/two-factor/recovery-codes` | `{current_password}` | 200 → `Codes` (new set) | 422 `current_password`; 409 off |
| `RequestPasswordReset(email)` | `POST /v1/auth/forgot-password` | `{email}` | 204 → `Changed` (non-enumerating) | `api-password-email` 1/min + 5/min |

- 4b maps the three 409 sentences to `DataFailure.Validation(fields = {"form": FieldMessage(<sentence>)}, 409)` (iOS `APIClient.mapError`, `03 §3`); 429 → `Throttled(retryAfterSeconds, 429)`; no network → `Offline`.
- Policy: production `Password::defaults()` = min 12, mixed case, numbers, symbols, uncompromised [F `W/app/Providers/AppServiceProvider.php:242-248`]. `security.passwordRules` is the rules string; iOS shows no policy hint on the Password page and no board does (OQ-10c-8).
- Mock (4c, l.318): wrong password → 422 `current_password`; secret `JBSWY3DPEHPK3PXP`, URL `otpauth://totp/ChronoFlow?secret=JBSWY3DPEHPK3PXP&issuer=ChronoFlow`; code `123456`; 8 codes; demo account has `twoFactorEnabled false`, `passkeyCount 2`, `passwordRules "minlength: 10;"`; the `twofactor` account has `twoFactorEnabled true`, `twoFactorConfirmedAt = now`.

---

## 3. Out of scope (owner)

- Passkeys page, add/remove, `SetPasskeys*`, `PasswordPromptDelete`, Delete account → **10d**. 10c only shows the Passkeys row (debug only, §5.6) and routes it to `Route.Passkeys` (still `RoutePlaceholder`). `SettingsSecurityPrompt.AddPasskey`/`RemovePasskey` exist as enum cases (port) but no 10c UI uses them; `change()` returns null for them and `submitPrompt` returns without a request until 10d adds `submitPasskeyPrompt`.
- `PasswordPromptEmail` and the email-change prompt → deferred (10a OQ-5). The board's `email` action copy is not built.
- Password policy hint on the Password page: no board, no iOS → not built (OQ-10c-8).
- A "Copied" confirmation after Copy: no board, no iOS string → not built (OQ-10c-10).
- Opening the `otpauth://` URL in an authenticator app (`03 §7` [R]): no board, no iOS → not built (OQ-10c-11).
- Offline-disabled Security rows: iOS disables none here; the `SetStates` offline row belongs to 10d (Add a passkey).
- Settings home Security **subtitle** stays `settings_password_two_factor` ("Password, two-factor"), as iOS `SettingsHomeView.swift:22` at the pin (OQ-10c-12).

---

## 4. Hand-ins to verify before Task 1 (no commit)

Run each check; if one fails, stop and report, unless the item names a fallback.

**Hand-ins verified at a980747f** (results in Refresh R4–R7): 1 yes; 2 yes, no public snapshot path, so Task 4 stays; 3 yes; 4 yes; 5 yes, and 0c maps the replaced b1 key; 6 no `spacing`, Task 8 adds it; 7 only `smartphone` is missing (`make vectors`); 8 yes, seed values are `MockScenarioValues`.

1. `DataSource.accountSecurity(change)` and `SessionSync.accountSecurity(change, session, epoch)` exist with the 4b/4d shapes; `AccountSecurityChange` secret-bearing cases redact `toString()`.
2. `SettingsRepository.upsertSecurity(value: JsonObject, permit)` exists (4d). Find the public way to apply an `AccountSnapshot` inside `database.write(permit)`; `StoreApplication` is `internal` to `:core:data`. Task 4 adds `AccountSecurityStore` inside `:core:data` for this; if 4d already has a public equivalent, call it and skip that part of Task 4.
3. 4e has a `StaleSessionException` (4a, `net.chronoflow.core.model.session`). Note: 4a declares `class StaleSessionException : CancellationException("stale session")`, so it **is** a `CancellationException`. Task 10 throws it to tell the model "session no longer current" (iOS `CancellationError`); the model must catch it **before** the generic `CancellationException` clause (§5.3 step 3).
4. 5a's `LockoutWait` is in `:core:model` (`net.chronoflow.core.model.auth`). 5a's `SecretField` is in `:feature:auth` (`model/SecretField.kt`); `:feature:settings` must not depend on `:feature:auth`, so Task 5 defines its own (OQ-10c-6).
5. `CopyIndex.find(CopyTable.Settings, key)` returns the entry for an iOS English key, with `isPlural` (0c). Every name in §5.12 is in `tools/l10n/names.lock.json`; `android_two_factor_intro_b1` and `android_recovery_codes_file_name` are in `tools/l10n/android-only.json`.
6. 10a's `SettingsPage(title: Int, onBack, testTag, modifier, content)`; it has no `spacing` parameter. Task 8 adds `spacing: Dp = PlanUITokens.space6.dp`.
7. `LucideGlyphs` contains `lock`, `key-round`, `copy`, `circle-alert`, `check`, `chevron-right`; `smartphone` is missing (Task 2 adds it).
8. 4c `MockDataSource`: the seeded `demo` account enables, confirms (`123456`) and reveals; the `twofactor` account disables. 4c's `AccountSecurityAPITests.mockRunsSetupCodesManageAndPasswordAndRejectsOffline` is green.

---

## 5. Spec

### 5.1 Security summary (`…/fs/security/SecuritySummary.kt`) [F `SettingsSecurityView.swift:16-43`]

```kotlin
data class SecuritySummary(val twoFactorEnabled: Boolean, val twoFactorConfirmedAt: String?, val passkeyCount: Int?) {
    companion object { fun from(settings: List<SettingsRecord>): SecuritySummary? }
}
```
- `null` when there is no `security` row (the page then shows 10a's `SettingsStoredState`).
- `twoFactorEnabled` = the JSON value is exactly `true`; anything else is false.
- `twoFactorConfirmedAt` = a JSON string, else null. `passkeyCount` = a JSON number with an integral value ≥ 0, else null (Phase 20 servers omit it).

### 5.2 Secret inputs (`…/fs/security/SecretField.kt`) [R, 5a l.177]

`interface SecretField { fun read(): String; fun clear() }` and `class TextFieldSecret(private val state: TextFieldState) : SecretField` (`read() = state.text.toString()`, `clear() = state.clearText()`). Screens own every password and code as `remember { TextFieldState() }` (never `rememberTextFieldState`, `rememberSaveable` or `SavedStateHandle`). The model reads a secret once and clears it; it never stores it in a property or in its state flow. An activity recreation drops the `remember`ed states, so the prompt reopens empty (`14 §3.4`).

### 5.3 The model: `SettingsSecurityModel` (`…/fs/security/SettingsSecurityModel.kt`) [F `SettingsSecurityModel.swift`]

A plain class (JVM-testable), wrapped by `SecurityViewModel` (§5.9) that passes `viewModelScope`.

```kotlin
enum class SettingsSecurityPrompt(val detail: String, val button: String) {   // iOS English keys
    Enable("Enter your password to turn on two-factor authentication.", "Continue"),
    Disable("Enter your password to turn off two-factor authentication.", "Turn off"),
    Reveal("Enter your password to show your recovery codes.", "Show codes"),
    Regenerate("Enter your password to make new recovery codes. Your current codes stop working.", "Make new codes"),
    AddPasskey("Enter your password to add a passkey on this phone.", "Add a passkey"),
    RemovePasskey("Are you sure you want to remove the “{{name}}” passkey? You will no longer be able to use it to sign in.", "Remove passkey");
    val destructive: Boolean get() = this == Disable
    fun change(password: String): AccountSecurityChange? // Enable/Disable/Reveal/Regenerate → the 4b case; passkeys → null
}

data class SecurityState(
    val prompt: SettingsSecurityPrompt? = null,
    val setup: ApiTwoFactorSetup? = null,
    val codes: List<String> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,                     // iOS English key
    val errors: Map<String, String> = emptyMap(),     // field → iOS English key
    val errorArguments: Map<String, Map<String, String>> = emptyMap(),
    val retryAt: Instant? = null,
    val offline: Boolean = false,
    val feedbackRevision: Long = 0,                   // +1 every time `message` is set to non-null
) {
    val twoFactorActivationFailed: Boolean get() = message == "Couldn't turn on two-factor."
    override fun toString(): String                  // redacts setup.secretKey, setup.otpauthUrl and codes
}

class SettingsSecurityModel(
    private val perform: suspend (AccountSecurityChange) -> AccountSecurityResult,
    private val now: () -> Instant,
) {
    val state: StateFlow<SecurityState>
    val locked: Boolean                               // retryAt?.isAfter(now()) == true
    fun remaining(at: Instant): Int                   // max(0, ceil(seconds(retryAt - at)))
    fun refreshLock()                                 // retryAt <= now() → retryAt = null
    suspend fun changePassword(current: SecretField, new: SecretField, confirmation: SecretField)
    fun showPrompt(action: SettingsSecurityPrompt)
    fun cancelPrompt()
    suspend fun submitPrompt(password: SecretField)
    suspend fun confirm(code: SecretField)
    suspend fun sendReset(email: String)
    fun clearSecrets()
}
```

Behaviour (line-for-line port; Swift line numbers in brackets):

- **`changePassword`** [l.85–102]: return if `busy || locked`. `errors = {}`. Read the three values. If current or new is empty → `errors["password"] = "Enter your password."`, return (fields **kept**). If `new != confirmation` → `errors["password_confirmation"] = "The passwords don't match."`, return (fields kept). Otherwise clear all three secrets, then `run(Password(current, new, confirmation))`; on success `message = "Password updated"`.
- **`showPrompt(action)`** [l.103–110]: return if busy. `errors = {}`, `message = null`, `prompt = action`. (The screen clears its prompt `TextFieldState` when the prompt opens and closes.)
- **`cancelPrompt()`** [l.111–118]: return if busy. `prompt = null`, `errors = {}`, `message = null`.
- **`submitPrompt(password)`** [l.119–128]: `val action = prompt`; read the password; return (without clearing) if `action == null`, the value is empty, `busy` or `locked`. Clear the secret **before** the request. If `action.change(value)` is non-null: `if (run(change)) prompt = null`. Passkey actions: return (10d).
- **`confirm(code)`** [l.129–138]: return if busy or locked. If the value is not exactly 6 ASCII digits → `errors["code"] = "Enter the 6-digit code from your authenticator app."`, return (code kept). Else clear the code, `run(ConfirmTwoFactor(value))`; on success `message = "Two-factor is on. Save your recovery codes now."`.
- **`sendReset(email)`** [l.228–231]: return if busy or locked. `if (run(RequestPasswordReset(email))) message = "Check your inbox."`.
- **`run(change): Boolean`** [l.139–218]:
  1. `val started = generation`; `busy = true`, `errors = {}`, `message = null`.
  2. `try { result = perform(change) }`. After it returns: `currentCoroutineContext().ensureActive()` (iOS `!Task.isCancelled`; a cancelled caller gets the `CancellationException`, not a silent `false`); if `generation != started` return false. Set `retryAt = null`, `offline = false`. Apply: `Changed` → nothing; `Setup(v)` → `setup = v`, `codes = []`; `Codes(v)` → `codes = v.recoveryCodes`, `setup = null`; `Snapshot` → `setup = null`, `codes = []`; others → nothing. Return true.
  3. **Order matters**, because `StaleSessionException` extends `CancellationException` (4a): first `catch (e: StaleSessionException) { currentCoroutineContext().ensureActive(); return false }` (iOS `CancellationError` from a stale session; no message; a truly cancelled caller still gets its cancellation), then `catch (e: CancellationException)` → rethrow (AGENTS Traps).
  4. `catch (e: DataFailure)` → if `generation != started` return false; then:
     - `Offline` → `offline = true`, `message = "Needs a connection."`.
     - `Throttled(s, _)` → `retryAt = now() + max(0, s)` seconds (fractional, `Duration`), `message = "Too many attempts."`.
     - `Validation(fields, _)` → for each `(field, value)` in the map's iteration order:
       - `field == "credential" || field == "name"` → `message = "We couldn't register that passkey. Please try again."`, continue; `field == "challengeId"` → `message = "The passkey registration request expired. Please try again."`, continue (10d's paths; ported now).
       - `value.key`: `"The provided password was incorrect."` → `errors[field] = key`; `"Use at least :min characters."` → `errors[field] = "Use at least {{min}} characters."` and, when `arguments["min"]` is a JSON number, `errorArguments[field] = {"min": <its integral text, e.g. "12">}`; `"Use both uppercase and lowercase letters."`, `"Use at least one number."`, `"Use at least one symbol."` → `errors[field] = key`; `"The passwords don't match yet."` → `errors[field] = "The passwords don't match."`; the three 409 sentences → `message = key`, `setup = null`, `codes = []`; `"The authentication code was not accepted. Try again."` → `errors[field] = "That code didn't work. Codes change every 30 seconds; try the newest one."`; anything else → `errors[field] = "Check the highlighted field."`.
       - If `fields` is empty → `message = failureMessage(change)`.
     - any other `DataFailure` (`Server`, `Unauthorized`, `Conflict`, …) → `message = failureMessage(change)`.
  5. Other `Exception` types are not caught (they are bugs).
  6. `finally { if (generation == started) busy = false }`.
  - `failureMessage(change)`: `EnableTwoFactor`, `ConfirmTwoFactor` → `"Couldn't turn on two-factor."`; `PasskeyCreationOptions`, `AddPasskey` → the registration sentence; else `"Something went wrong"`.
  - Every non-null `message` assignment increments `feedbackRevision`.
- **`clearSecrets()`** [l.232–247]: `generation++`, `busy = false`, `message = null`, `errors = {}`, `errorArguments = {}`, `setup = null`, `codes = []`, `prompt = null`. **`retryAt` is kept** (server throttling survives leaving, `I/plan.md` "Fix Security re-entry").
- State updates go through `MutableStateFlow.update {}` on the caller's dispatcher; the model does no I/O itself.

### 5.4 Copy resolution (`…/fs/security/SecurityCopy.kt`) [F `SettingsStrings.swift`]

- `@Composable fun securityText(key: String, args: Map<String, String> = emptyMap()): String`: `CopyIndex.find(CopyTable.Settings, key)` → `stringResource(entry.resId, *positional(args))`. `{{min}}` → `%1$s`, `{{email}}` → `%1$s`, `{{date}}` → `%1$s` (0c rule: `{{name}}` → `%K$s`). A key not in the index is a bug: `checkNotNull` with the key in the message (tests catch it).
- `@Composable fun lockoutText(seconds: Int): String`: `val wait = LockoutWait(seconds)`; plural resource from `CopyIndex.find(CopyTable.Settings, wait.localizationKey)`; `pluralStringResource(id, wait.count, wait.count)`. Results: 2820 s → "Too many attempts. Try again in 47 minutes." / "Te veel pogingen. Probeer het over 47 minuten opnieuw."; 1 s → "Too many attempts. Try again in 1 second." in en (the catalog has `one`/`other` for all three keys, `Settings.xcstrings`) / "Te veel pogingen. Probeer het over 1 seconde opnieuw." in nl; 59 s → "… 59 seconds."; 3600 s → "… 1 hour." / "… 1 uur opnieuw.".
- The 2FA intro bullet b1 resolves to the overlay `android_two_factor_intro_b1` ("You need an authenticator app." / "Je hebt een authenticator-app nodig."), because 0c's overlay replaces the iOS key. 0c maps replaced keys (verified): look it up through `CopyIndex.find(CopyTable.Settings, "You need an authenticator app, like the one built into Passwords on iPhone.")`.
- *[Refresh]* Order positional arguments by `CopyEntry.args` (e.g. `["min"]`, `["email"]`, `["date"]`, `["%1"]` for plurals) instead of hard-coding the mapping, and add `securityText` next to 10b's `settingsText` (`components/SettingsPreferenceComponents.kt:43`).

### 5.5 Password prompt (`…/fs/security/prompt/`) [F `SettingsPasswordPrompt.swift`, `SettingsCompactSheet`; boards `PasswordPrompt*`]

**Content** `SettingsPasswordPromptContent(...)` (shared by both hosts; 10d calls it for delete and passkeys):

```kotlin
@Composable internal fun SettingsPasswordPrompt(
    detail: String, button: String, destructive: Boolean,       // iOS English keys
    detailArgs: Map<String, String> = emptyMap(),
    password: TextFieldState, busy: Boolean, retryAt: Instant?, now: () -> Instant,
    error: String?, errorArgs: Map<String, String> = emptyMap(), // field error key for "current_password"
    message: String?, email: String,
    identifier: String = "settings.security.prompt",
    onCancel: () -> Unit, onSubmit: () -> Unit, onReset: () -> Unit,
)
```

Layout, top to bottom (`PasswordPromptMetrics` `@BoardMetrics("PasswordPrompt")` holds every board number that has no token):

1. **Header** row, min height 48 [R; board 44 + Android target]: `TextButton` "Cancel" (`settings_cancel`, color `dawnText`, start), title "Confirm it's you" (`settings_confirm_it_s_you`, `settingsStrong` 16/24, centred in the row, `heading()`), an empty end slot of the Cancel width. Cancel tag `settings.security.prompt.cancel` [R]; disabled while busy.
2. Gap 14 (`radiusLg`) between all body children [F iOS spacing `radiusLg`; board gap 14].
3. **Description**: `securityText(detail, detailArgs)`, `settingsPromptBody` 15/22, `mutedForeground`.
4. **Field group** (gap 6, `pillGap`):
   - Field: `BasicSecureTextField(state = password)` inside a box: min height 52 (`promptFieldHeight`), radius 12 (`radiusMd`), fill `surface2` [F iOS; board's `rgba(118,118,128,.12)` is the iOS system fill], padding horizontal 16 (`space4`), row gap 10, trailing 18 dp `lock` in `mutedForeground` [board], text `screenBodyLg` 16/24 `cardForeground`, placeholder "Password" (`settings_password`, `mutedForeground`) when empty, `textObfuscationMode = RevealLastTyped`. Semantics: `contentDescription = "Password"`, `ContentType.Password`, `error(alertText)` when an alert shows, `disabled()` when not enabled. Keyboard `KeyboardType.Password`, `ImeAction.Done`; IME Done calls `onSubmit` only when the button is enabled [R]. Tag `"$identifier.password"`. No autofocus (iOS sets none).
   - `val remaining = retryAt?.let { max(0, ceil((it - now()).seconds)) } ?: 0`, read in the leaf `PromptAlertLine` only (§5.11). `alert = if (remaining > 0) lockoutText(remaining) else error?.let { securityText(it, errorArgs) }`.
   - When `alert != null`: 2 dp `destructive` inner border on the field (`selectionWidth`), and a row (gap 6) of 16 dp `circle-alert` (top offset 2) + `alert` in `screenBody` `destructive`, tag `"$identifier.error"`, `liveRegion = Polite`.
   - Field enabled only when `!busy && remaining == 0`; disabled → alpha 0.55 (`dimmedFieldOpacity`).
   - **Forgot password?** (`settings_forgot_password`): text button, `screenBody` 14/20 weight 500, underlined, `foreground`, start-aligned, min height 40 (`walkthroughDoneHeight`) + `minimumInteractiveComponentSize()`. Enabled when `!busy && remaining == 0` [F `SettingsPasswordPrompt.swift:71`; a signed-in account always has an email, so no extra email rule]. Tag `"$identifier.forgot"`. Opens the alert (6).
5. **Feedback** (only when `message != null && remaining == 0`), tag `"$identifier.feedback"`:
   - `message == "Couldn't turn on two-factor."` → `CfBanner(title = securityText(message), message = securityText("Nothing changed on your account. Try again in a moment."))`.
   - else → `Text(securityText(message), screenBody, mutedForeground)` ("Check your inbox.", "Needs a connection.", "Something went wrong", the 409 sentences).
6. **Button**: `CfButton(text = if (busy) "Checking…" else button, style = if (destructive) Destructive else Primary, fillWidth = true, minHeight = 50 /* promptButtonHeight */, textStyle = settingsStrong, loading = busy, loadingDescription = "Checking…", disabledAlpha = 0.45 /* SettingsTokens.disabledOpacity */, enabled = !busy && remaining == 0 && password.text.isNotEmpty())`, tag `"$identifier.submit"`. Clicking hides the keyboard, then `onSubmit()`.
7. **Forgot alert**: `CfAlertDialog(title = "Reset your password?", text = "We'll email a reset link to {{email}}. You stay signed in on this phone." with email, confirm = CfDialogAction("Send link") { onReset() }, dismiss = CfDialogAction("Cancel") {})`, tag `"$identifier.forgotAlert"` [R]. Its visibility is `remember { mutableStateOf(false) }` (not saveable; it closes on recreation).
- Root semantics: `testTag(identifier)`, `paneTitle = "Confirm it's you"`.

**Hosts** (`SettingsPasswordPromptHost(visible, busy, onDismiss, content)`):

- **Phone** (`!isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)`): `CfModalBottomSheet` (radius 38 and `card` built in, M3 drag handle). `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !busy })`; `onDismissRequest = onDismiss` (ignored while busy; back press too: `ModalBottomSheetProperties(shouldDismissOnBackPress = !busy)`). Content padding horizontal 20 (`space5`), top 0 (the drag handle supplies the board's 10), bottom 32 (`space8`) plus `navigationBars` and `ime` insets; content in `verticalScroll` so the button stays reachable above the keyboard.
- **Wide** (≥ 600 dp): `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = !busy))` containing a `Surface(shape = RoundedCornerShape(38), color = card, contentColor = cardForeground)` of width **540 dp** (`PasswordPromptWide`, OQ-10c-1) capped at `windowWidth − 32`, `heightIn(max = 0.9 × windowHeight)`, padding 10/20/32, content scrolls inside. No drag handle. The dim is the platform dialog dim.
- Window size comes from `currentWindowAdaptiveInfoV2()` (what 10a's `SettingsPage` uses) / `LocalWindowInfo.current.containerSize`; never from `onSizeChanged` (Traps).
- *[Refresh R7]* `CfModalBottomSheet` already uses the 38 dp `sheetTop` shape and `card`, but has no `properties` parameter (see R7 for the back-press choice). `CfAlertDialog` has no tag parameter (see R7).

### 5.6 Security page (`…/fs/security/SecurityScreen.kt`, tag `settings.security.screen`, `SecurityMetrics` `@BoardMetrics("SetSecurity")`)

`SettingsPage(title = settings_security, onBack)`. When `SecuritySummary.from(settings)` is null → `SettingsStoredState(loaded, failed, onRetry)` (10a). Else one `CfCard(rows = true)`:

| Row | Glyph | Title | Subtitle | Trailing | Route | Tag | Shown |
|---|---|---|---|---|---|---|---|
| Password | `lock` | `settings_password` | `settings_change_your_password` | chevron | `Route.Password` | `settings.security.password` | always |
| Passkeys | `key-round` | `settings_passkeys` | plural `settings_n_passkeys` (count) | chevron | `Route.Passkeys` | `settings.security.passkeys` | `passkeyCount != null` **and** (`PasskeysRow.isBuilt` or the debug label hook `LaunchPlatform.routeLabel` is non-null) — `isBuilt = false` until 10d (a 10c constant; Refresh R8) |
| Two-factor | `smartphone` | `settings_two_factor_authentication` | `settings_a_code_from_an_app_when_you_sign_in` | `CfBadge` "On" (`settings_on`, `Dawn`) or "Off" (`settings_off`, `Secondary`), then chevron | `Route.TwoFactorSettings` | `settings.security.twoFactor` | always |

Rows are `CfRow` (64 min, 10/16, 36 disc), separated by `CfDivider`. "2 passkeys" uses tabular figures (board). *[Refresh R7]* Map the columns onto `CfRow(title, subtitle = …, icon = LucideGlyphs.<Lock|KeyRound|Smartphone>.imageVector(), badge = On|Off text, badgeDawn = twoFactorEnabled, onClick, testTag)`; `CfRow` draws the badge trailing before the chevron. The Passkeys visibility hook is `LaunchPlatform.routeLabel != null`, passed in from `SettingsEntry` (R8); there is no `PasskeysRow.isBuilt` yet (a 10c constant is fine).

### 5.7 Password page (`…/fs/security/PasswordScreen.kt`, tag `settings.password.screen`, `PasswordMetrics` `@BoardMetrics("SetPassword")`) [F `SettingsSecurityView.swift:54-96`]

- `SettingsPage(title = settings_password, onBack)`, gap 24.
- Screen state: `val current = remember { TextFieldState() }`, `newPassword`, `confirmation` likewise (plain `remember`, §5.2).
- When `state.errors.isNotEmpty()`: `CfNotice(title = "Check the highlighted field.", message = "Your new password isn't saved yet.", tone = Warning)`.
- `CfCard` (spacing 16) with three `CfSecureTextField`s, disabled while busy:
  - "Current password" (`settings_current_password`), tag `settings.password.current`, `ContentType.Password`, `ImeAction.Next`, error `errors["current_password"]`.
  - "New password" (`settings_new_password`), tag `settings.password.new`, `ContentType.NewPassword`, `ImeAction.Next`, error `errors["password"]` (with `errorArguments["password"]`).
  - "Confirm new password" (`settings_confirm_new_password`), tag `settings.password.confirmation`, `ContentType.NewPassword`, `ImeAction.Done` (hides the keyboard; does not submit, as iOS), error `errors["password_confirmation"]`.
- `CfButton("Change password", Primary, fillWidth, minHeight = 48 /* actionMinHeight */, loading = busy, loadingDescription = "Checking…", enabled = !busy && !locked)`, tag `settings.password.save` → hide keyboard, `changePassword(TextFieldSecret(current), …)`.
- `SettingsSecurityLock` (§5.11) at the end.
- Feedback snackbar (§5.10).

### 5.8 Two-factor page (`…/fs/security/TwoFactorScreen.kt`, tag `settings.twoFactor.screen`, `TwoFactorMetrics` `@BoardMetrics("Set2FA", "Set2FASetup", "Set2FAOn", "Set2FAManage")`) [F `SettingsSecurityView.swift:98-293`]

`SettingsPage(title = settings_two_factor_authentication, onBack, spacing = 20 /* space5 */)`. On ≥ 600 dp the 10a column applies (`Set2FAWide`, `Set2FAManageWide`: `max-width: 560px` including the 16 dp side padding). Content by state, first match wins: `setup != null` → **Setup**; `enabled` (`summary.twoFactorEnabled || codes.isNotEmpty()`) → **Manage**; else **Intro**. Then `SettingsSecurityLock`. A `null` summary → `SettingsStoredState`.

**Intro** (`Set2FA`):
- `CfBadge("Off", Secondary)` as the first item (the large title belongs to the app bar).
- `CfCard(spacing = 14 /* radiusLg */)`: body `settings_ask_for_a_code_from_an_authentic_822326` (`screenBodyLg`, `cardForeground`); bullets column (gap 6), each a row of "•" (hidden from accessibility, gap 6) + text in `screenBody` `mutedForeground`: b1 = `android_two_factor_intro_b1`, b2 `settings_you_get_recovery_codes_for_when_505e13`, b3 `settings_signing_in_with_a_passkey_doesn_34471b`; `CfButton("Turn on two-factor", Primary, fillWidth, minHeight = 48)`, tag `settings.twoFactor.enable` → `showPrompt(Enable)`; lock hint (§5.11, centred) "You'll be asked for your password first.".

**Setup** (`Set2FASetup`), wrapped in `SecureWindow` (§5.13):
- Intro `settings_when_it_s_on_you_sign_in_with_yo_bb939f`, `screenBodyLg` `mutedForeground`.
- `CfCard` (spacing 16):
  - Step label "1 · Scan this code with your authenticator app": `screenLabel` 14/20 600, the leading digit `1` in `dawnText` with tabular figures (`AnnotatedString`; find the digit with `indexOf('1')`, as iOS `range(of:)`).
  - **QR tile** (`SettingsAuthenticatorQr`, tag `settings.twoFactor.qr`): 168 × 168 (`qrSize`), radius 14 (`radiusLg`), fill `settingsQrBackground` (white in both themes), 1 dp `border` stroke, centred; the modules inset 8 (`space2`) and drawn in the **light** `foreground` color (`CfLightColors.foreground`) in both themes. `semantics { contentDescription = "QR code for your authenticator app"; role = Role.Image }`.
  - Hint `settings_using_this_phone_copy_the_setup_f4a45f` (`screenBody` muted).
  - Key row: `surface2`, radius 14, padding start 14 / others 6, gap 8. Key text = `setup.secretKey` exactly as sent (no grouping, OQ-10c-9), `settingsCodes` 15/22 with `FontFamily.Monospace`, `SelectionContainer`, fills the width, wraps anywhere; tag `settings.twoFactor.secret`. `CfButton("Copy", Outline, minHeight = 40 /* walkthroughDoneHeight */, leadingIcon = copy)` tag `settings.twoFactor.copySecret` [R] → `SensitiveClipboard.copy(secretKey)`.
- `CfCard` (spacing 16): step "2 · Enter the 6-digit code it shows" (digit `2` in `dawnText`); `CfTextField(state = code /* remember { TextFieldState() } */, label = "6-digit code", placeholder = "123456", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done), inputTransformation = InputTransformation.maxLength(6) [board maxlength 6; no digit filter: iOS accepts any text and the model rejects non-ASCII-digit codes with "Enter the 6-digit code from your authenticator app."], contentType = ContentType.SmsOtpCode [iOS `.oneTimeCode`; same as 5b OQ-5b-2], error = errors["code"])`, tag `settings.twoFactor.code`. IME Done hides the keyboard.
- When `twoFactorActivationFailed`: `CfBanner(title = "Couldn't turn on two-factor.", message = "Nothing changed on your account. Try again in a moment.")`, tag `settings.twoFactor.failure`. The QR and key stay.
- `CfButton(if (failed) "Try again" else "Turn on two-factor", Primary, fillWidth, minHeight = 48, loading = busy, loadingDescription = "Checking…", enabled = !busy && !locked)`, tag `settings.twoFactor.confirm` → `confirm(TextFieldSecret(code))`.

**Manage** (`Set2FAManage`, `Set2FAOn`):
- Row (gap 8): `CfBadge("On", Dawn)` + "Since {{date}}" (`settings_since_date`, `screenBody` muted, tabular) when `twoFactorConfirmedAt` parses as an ISO-8601 instant (with or without fractional seconds); the date is 1a's `CopyLineFormatter.formatDate(…, language, zone = deps.zone)` → "30 Oct 2026" / "30 okt 2026". Unparsable or null → the line is omitted.
- If `codes.isEmpty()`: section (gap 8 [board `Set2FAManage`]; iOS uses `space3` 12, recorded in OQ-10c-15) with eyebrow "Recovery codes" (`settingsEyebrow`, uppercase, `mutedForeground`, horizontal padding 4, `heading()`) and `CfCard(rows = true)` with `CfRow("Show recovery codes", subtitle = "Each code works once and is then replaced", lock = true)`, tag `settings.twoFactor.reveal` → `showPrompt(Reveal)`.
- Else (codes shown; `Set2FACodes`), wrapped in `SecureWindow`: `CfCard` (spacing 16):
  - header column (gap 4): "Recovery codes" `screenSectionTitle` (Fraunces 20/28, −0.2 tracking), `heading()`; body `settings_if_you_lose_your_phone_sign_in_w_e16307` `screenBody` muted.
  - codes grid: `surface2`, radius 14, padding 12, 2 equal columns, row gap 8, column gap 12; each code `settingsCodes` + `FontFamily.Monospace`, centred, tag `settings.twoFactor.recoveryCode`; the grid is a plain `Column` of `Row`s (8 items: no lazy layout) inside one `SelectionContainer`; the grid node has `contentDescription = "Recovery codes"` semantics as a list (`collectionInfo(rows = n/2, columns = 2)`).
  - buttons row (gap 8, equal weights): `CfButton("Copy", Outline, minHeight = 48)` tag `settings.twoFactor.copyCodes` → copy `codes.joinToString("\n")`; `CfButton("Download", Outline, minHeight = 48)` tag `settings.twoFactor.download` → §5.14.
- `CfCard(rows = true)`: `CfRow("Make new codes", subtitle = "Your current codes stop working", lock = true)` tag `settings.twoFactor.regenerate` → `showPrompt(Regenerate)`; `CfDivider`; `CfRow("Turn off two-factor", subtitle = "Sign in with only your password or passkey", lock = true, destructive = true)` tag `settings.twoFactor.disable` → `showPrompt(Disable)`.
- Lock hint "Each of these asks for your password first.", start-aligned, horizontal padding 4 (board).

**Prompt host** on this page: `SettingsPasswordPromptHost(visible = state.prompt != null, …)` with `detail = prompt.detail`, `button = prompt.button`, `destructive = prompt.destructive`, `password = promptPassword /* remember { TextFieldState() } */`, `error = errors["current_password"]`, `message = state.message`, `email = account?.email.orEmpty()`, `onCancel = cancelPrompt`, `onSubmit = { submitPrompt(TextFieldSecret(promptPassword)) }`, `onReset = { sendReset(email) }`. `LaunchedEffect(state.prompt) { promptPassword.clearText() }` (iOS `onDismiss` clears it).

### 5.9 ViewModels and dependencies

- Add to 10a's `SettingsDependencies`: `suspend fun performSecurity(change: AccountSecurityChange): AccountSecurityResult` (throws `StaleSessionException` when the session is not current, `DataFailure` from the server), `val passwordPromptSeed: String?` [debug seam, §5.15] and `val clipboard: SensitiveClipboard` (§5.14). `SessionSettingsDependencies` implements them in Task 10; `FakeSettingsDependencies` and `SettingsFixture.deps` gain them in the task that adds them (R2).
- `SecurityViewModel(deps, main)` (one class for Password and Two-factor; each route gets its own instance through the Navigation 3 entry's `ViewModelStore`; constructor, scope and `Factory` follow 10a's pattern, R2): owns `SettingsSecurityModel(perform = deps::performSecurity, now = { Instant.now() })` and a `SettingsPagesModel(deps, viewModelScope)` (started in `init`, stopped in `onCleared`), exposes `state`, `summary: StateFlow<SecuritySummary?>` (from `pages.ui` settings, mapped off the main thread), loaded/failed/Retry from `pages.ui`, `email: StateFlow<String>` (from `pages.ui.account?.email`), `qr: StateFlow<QrMatrix?>` (§5.13), and launches each action in `viewModelScope` with a single `request: Job?` guard (ignore taps while a request runs). `onCleared()` → `model.clearSecrets()`.
- Leaving: each screen has `DisposableEffect(Unit) { onDispose { if (activity?.isChangingConfigurations != true) vm.clearSecrets() } }` (`activity` = `LocalActivity.current`, as `LanguageScreen` uses) — iOS clears on `onDisappear` (back, tab switch). A configuration recreation keeps setup and codes (`14 §3.4`).
- Nothing in these ViewModels uses `SavedStateHandle`.

### 5.10 Feedback snackbar [F `SettingsSecurityFeedback`, OQ-A5]

On the Password and Two-factor pages, when `message != null && retryAt == null && !twoFactorActivationFailed` (and on the Two-factor page also `prompt == null`), the ViewModel exposes a pending `feedback: SecurityFeedback(id = feedbackRevision, key = message)?` and holds it until the screen acknowledges it (10a's pattern, Refresh R3; **not** a `LaunchedEffect(state.feedbackRevision)` and not a channel). The route collects it in `LaunchedEffect(viewModel, snackbar, resources) { viewModel.ui.map { it.feedback }.filterNotNull().distinctUntilChanged().collect { hostState.showSnackbar(resources.getString(<resolved resId>, …), duration = SnackbarDuration.Long) } }` in a page-level `CfSnackbarHost`, tag `settings.security.feedback`, `onPresented = { SettingsDebugCounters.securityPresented(); viewModel.feedbackPresented(id) }` (a new counter on 10a's `SettingsDebugCounters`, debug `AtomicInteger`, release no-op; R2). `feedbackPresented(id)` clears it only when the id still matches. Only the latest revision shows; a recomposition or recreation never re-shows an acknowledged one. Messages that appear here: "Password updated", "Two-factor is on. Save your recovery codes now.", "Needs a connection.", "Something went wrong", "Two-factor authentication is already on.", "Two-factor authentication is off.", "Start two-factor setup again.".

### 5.11 Countdown leaves [F `SettingsSecurityLock`, `refreshLock`]

- `SettingsSecurityLock(retryAt, now)`: shown while `retryAt != null`; a row with 16 dp `circle-alert` + `lockoutText(remaining)` in `screenBody` `destructive`, tag `settings.security.lock` [R], `liveRegion = Polite`. `remaining` comes from `produceState(key1 = retryAt) { while (true) { value = remaining(now()); if (value == 0) break; delay(1_000) } }` inside this leaf only.
- The ViewModel runs `LaunchedEffect`-free: `viewModelScope.launch` keyed on `retryAt` changes that calls `model.refreshLock()` once per second until it is null (iOS `task(id: retryAt)`). Tests use virtual time.
- The prompt's `PromptAlertLine` reads the same ticker in its own leaf; nothing above it recomposes per second.

### 5.12 Copy (resources only; names from the slug rule, verify each in `tools/l10n/names.lock.json`)

All in the Settings table (`settings_*`), en / nl from the catalog at the pin:

| Name | en | nl |
|---|---|---|
| `settings_security` | Security | Beveiliging |
| `settings_password` | Password | Wachtwoord |
| `settings_change_your_password` | Change your password | Wijzig je wachtwoord |
| `settings_passkeys` | Passkeys | Passkeys |
| `settings_n_passkeys` (plural) | %d passkeys (one/other) | %d passkey / %d passkeys |
| `settings_two_factor_authentication` | Two-factor authentication | Tweestapsverificatie |
| `settings_a_code_from_an_app_when_you_sign_in` | A code from an app when you sign in | Een code uit een app bij het inloggen |
| `settings_on` / `settings_off` | On / Off | Aan / Uit |
| `settings_check_the_highlighted_field` | Check the highlighted field. | Controleer het gemarkeerde veld. |
| `settings_your_new_password_isn_t_saved_yet` | Your new password isn't saved yet. | Je nieuwe wachtwoord is nog niet opgeslagen. |
| `settings_current_password`, `settings_new_password`, `settings_confirm_new_password` | Current password / New password / Confirm new password | Huidig wachtwoord / Nieuw wachtwoord / Bevestig je nieuwe wachtwoord |
| `settings_change_password` | Change password | Wachtwoord wijzigen |
| `settings_enter_your_password` | Enter your password. | Vul je wachtwoord in. |
| `settings_the_passwords_don_t_match` | The passwords don't match. | De wachtwoorden komen niet overeen. |
| `settings_use_at_least_min_characters` | Use at least %1$s characters. | Gebruik minstens %1$s tekens. |
| `settings_use_both_uppercase_and_lowercase_03dd17`, `settings_use_at_least_one_number`, `settings_use_at_least_one_symbol` | policy lines | |
| `settings_the_provided_password_was_incorrect` | The provided password was incorrect. | Het opgegeven wachtwoord is onjuist. |
| `settings_password_updated` | Password updated | Wachtwoord bijgewerkt |
| `settings_needs_a_connection`, `settings_something_went_wrong`, `settings_too_many_attempts` | | |
| `settings_too_many_attempts_try_again_in_n_086dac` / `…_e0c4d8` / `settings_too_many_attempts_try_again_in_n_hours` (plurals) | seconds / minutes / hours | seconde(n) / minuut/minuten / uur |
| `settings_confirm_it_s_you` | Confirm it's you | Bevestig dat jij het bent |
| `settings_cancel` | Cancel | Annuleren |
| `settings_enter_your_password_to_turn_on_t_7b94fb`, `…_turn_off_79a148`, `…_show_your_acfe2d`, `…_make_new_8ee53b` | prompt details | |
| `settings_continue`, `settings_turn_off`, `settings_show_codes`, `settings_make_new_codes` | Continue / Turn off / Show codes / Make new codes | Doorgaan / Uitzetten / Codes tonen / Nieuwe codes maken |
| `settings_checking` | Checking… | Controleren… |
| `settings_forgot_password` | Forgot password? | Wachtwoord vergeten? |
| `settings_reset_your_password` | Reset your password? | Je wachtwoord opnieuw instellen? |
| `settings_we_ll_email_a_reset_link_to_emai_7de5ee` | We'll email a reset link to %1$s. You stay signed in on this phone. | We sturen een link naar %1$s. Je blijft op deze telefoon aangemeld. |
| `settings_send_link`, `settings_check_your_inbox` | Send link / Check your inbox. | Link sturen / Bekijk je inbox. |
| `settings_couldn_t_turn_on_two_factor`, `settings_nothing_changed_on_your_account_fafd2d` | banner | |
| `settings_ask_for_a_code_from_an_authentic_822326`, `android_two_factor_intro_b1`, `settings_you_get_recovery_codes_for_when_505e13`, `settings_signing_in_with_a_passkey_doesn_34471b` | intro + bullets | |
| `settings_turn_on_two_factor`, `settings_you_ll_be_asked_for_your_password_first` | | |
| `settings_when_it_s_on_you_sign_in_with_yo_bb939f`, `settings_1_scan_this_code_with_your_authe_ba80d4`, `settings_qr_code_for_your_authenticator_app`, `settings_using_this_phone_copy_the_setup_f4a45f`, `settings_copy`, `settings_2_enter_the_6_digit_code_it_shows`, `settings_6_digit_code`, `settings_123456`, `settings_enter_the_6_digit_code_from_your_ee11fc`, `settings_that_code_didn_t_work_codes_chan_8d392d`, `settings_try_again` | setup | |
| `settings_since_date`, `settings_recovery_codes`, `settings_show_recovery_codes`, `settings_each_code_works_once_and_is_then_ae177d`, `settings_if_you_lose_your_phone_sign_in_w_e16307`, `settings_download`, `settings_make_new_codes`, `settings_your_current_codes_stop_working`, `settings_turn_off_two_factor`, `settings_sign_in_with_only_your_password_892dad`, `settings_each_of_these_asks_for_your_pass_42afec`, `settings_two_factor_is_on_save_your_recov_856eb0` | manage / codes | |
| `settings_two_factor_authentication_is_already_on`, `settings_two_factor_authentication_is_off`, `settings_start_two_factor_setup_again` | 409 messages | |
| `android_recovery_codes_file_name` | Recovery codes.txt | Herstelcodes.txt |

Key Dutch checks: "Bevestig dat jij het bent", "Te veel pogingen. Probeer het over 47 minuten opnieuw.", "Het opgegeven wachtwoord is onjuist.", "Je hebt een authenticator-app nodig.", "Tweestapsverificatie staat aan. Bewaar nu je herstelcodes.", "Sinds 30 okt 2026". Where a board's Dutch differs from the catalog (board "Wachtwoord opnieuw instellen?", "…ingelogd.", "Vraag bij elke keer inloggen…"), the catalog wins (OQ-10c-13).

### 5.13 QR and `FLAG_SECURE`

- **Dependency:** `com.google.zxing:core` (Apache-2.0) in `gradle/libs.versions.toml` as `zxing = "3.5.3"` / `zxing-core = { module = "com.google.zxing:core", version.ref = "zxing" }`. Use 3.5.3 unless Maven Central has a newer **stable** release published at least two weeks before the commit; write the chosen version and its publish date in the commit body. `:feature:settings` `implementation(libs.zxing.core)`. Core only: no `zxing-android-embedded`, no camera.
- `QrMatrix` (`…/fs/security/QrMatrix.kt`): `class QrMatrix(val size: Int, private val bits: BooleanArray) { operator fun get(x: Int, y: Int): Boolean }` with `companion fun encode(text: String): QrMatrix` = `QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.CHARACTER_SET to "UTF-8"))` (iOS `CIQRCodeGenerator` default correction is M). `toString()` prints only the size.
- The ViewModel encodes on `Dispatchers.Default` whenever `setup.otpauthUrl` changes; `qr = null` when `setup == null`. The composable draws in `Modifier.drawWithCache { val path = Path().apply { /* one rect per dark module, scaled to the inner square, snapped to whole pixels */ }; onDrawBehind { drawPath(path, color) } }`: no bitmap, no per-frame allocation, crisp at every density. While `qr == null` the tile shows only its white fill.
- `SecureWindow(enabled: Boolean)` (`…/ui/secure/SecureWindow.kt`, `:core:ui`): `DisposableEffect(window, enabled)` that, when enabled, increments a per-window counter (`object SecureWindowCounter` with a `WeakHashMap<Window, Int>`, main thread only) and calls `window.addFlags(FLAG_SECURE)` on 0 → 1; on dispose decrements and calls `clearFlags(FLAG_SECURE)` on 1 → 0. The window is `LocalActivity.current?.window`. Used by the Setup and the codes-shown states only (Intro and Manage-without-codes stay capturable).

### 5.14 Clipboard and Download

- **`SensitiveClipboard`** (`…/fs/security/SensitiveClipboard.kt`; a process-level instance from `:app`, injected through `SettingsDependencies.clipboard`):
  - Port interface (for JVM tests): `interface ClipboardPort { fun set(label: String, text: String, sensitive: Boolean): Long /* returns ClipDescription.timestamp */; fun currentTimestamp(): Long? /* null when unreadable */; fun clear() }`. Android adapter `AndroidClipboardPort(clipboardManager)`: `ClipData.newPlainText(label, text)`; on API 33+ `clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }`; `currentTimestamp()` = `primaryClipDescription?.timestamp` (null when the app has no focus, Android 10+); `clear()` = `clearPrimaryClip()` (API 28+, min SDK 29). The label is `"ChronoFlow"` [R, not shown to users: a fixed `translatable="false"` string `clipboard_label` appended to `strings_untranslatable.xml`].
  - `copy(text)`: `val stamp = port.set(…, sensitive = true)`; remember `(stamp)`; cancel the previous timer; start a timer on `appScope` (`delay(60.seconds)`, iOS `.expirationDate: +60 s`). At expiry: `when (port.currentTimestamp()) { stamp → port.clear(); null → pending = true; else → forget }`. On the next process `ON_START` (`ProcessLifecycleOwner`) with `pending`: same check (`stamp` → clear; other → forget; null → stay pending).
  - Never log the text. No in-app "Copied" message (OQ-10c-10); Android 13+ shows its own clipboard confirmation.
- **Download** (`RecoveryCodesDownload` in `…/fs/security/`):
  - `RecoveryCodesDocument.bytes(codes: List<String>): ByteArray = codes.joinToString("\n").toByteArray(Charsets.UTF_8)` (no BOM, LF only, no trailing newline, iOS `joined(separator: "\n")`).
  - `rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain"))`; `launch(stringResource(android_recovery_codes_file_name))`. Launching inside `try`; on `ActivityNotFoundException` → fallback `startActivity(Intent.createChooser(Intent(ACTION_SEND).setType("text/plain").putExtra(EXTRA_TEXT, codes.joinToString("\n")), null))`.
  - Result `uri != null`: the ViewModel writes on `Dispatchers.IO` with `contentResolver.openOutputStream(uri, "wt")`. If the codes are gone by then (cleared, or a new process), write nothing and best-effort `DocumentsContract.deleteDocument(resolver, uri)` (catch `Exception` subclasses other than `CancellationException`). On `IOException`/`SecurityException` → `message = "Something went wrong"` (feedback snackbar; OQ-10c-14). Success shows nothing (iOS share sheet shows nothing).
  - Never write the codes to `cacheDir`/`filesDir` or a `FileProvider` (`14 §1.3`).

### 5.15 Navigation, home flag and seeds

- `SettingsEntry` branches (plus `AppShell`'s `page` route list, Refresh R8): `Route.Security` → `SecurityScreen`, `Route.Password` → `PasswordScreen`, `Route.TwoFactorSettings` → `TwoFactorScreen`; `Route.Passkeys` stays `RoutePlaceholder` (10d).
- 10a `SettingsDestination.Security`: pass `isBuilt = true` (constructor argument; the home row now shows in release).
- 10a's `SettingsSeedRoute.path` already maps `password` → [Security, Password], `twoFactor` and `twoFactorSetup` → [Security, TwoFactorSettings]. Add: when `DebugSeedRouting.current?.settingsRoute == "twoFactorSetup"`, `SettingsDependencies.passwordPromptSeed` returns `MockScenarioValues.password` ("password", `:core:data-mock`, a debug-only dependency of `:app`) through the debug `LaunchPlatform` (Refresh R8); release and every other route return null. On the Two-factor page's first start with a non-null seed, the ViewModel runs `showPrompt(Enable)` then `submitPrompt(StaticSecret(seed))` once (iOS `SettingsSecurityUITestPage`, l.379–385). `StaticSecret` lives in `src/debug` too.
- `CF_AUTH_TEST_SIGNED_IN=twofactor` + `CF_SETTINGS_ROUTE=twoFactor` opens Manage (iOS `account: .twofactor`).

---

## 6. Tasks (one commit each, in order; Conventional Commits, lower-case imperative, **no trailer**)

### Task 1: `build(deps): add zxing core to the catalog`
- `gradle/libs.versions.toml` (§5.13); `feature/settings/build.gradle.kts` `implementation(libs.zxing.core)`. Add "ZXing core, Apache-2.0" to `docs/third-party.md`.
- Tests: none new; `make quick` and `./gradlew :feature:settings:dependencies --configuration releaseRuntimeClasspath` shows `com.google.zxing:core:<version>`; `make size` still passes (no `.so`).

### Task 2: `feat(designsystem): add security glyphs and a button disabled alpha`
- Add `smartphone` to `tools/icons/lucide.json` (the only missing glyph at a980747f); regenerate `LucideGlyphs` with `make vectors`.
- *[Refresh R7]* `CfButton` already has `disabledAlpha: Float = AuthTokens.disabledOpacity`; nothing to add, no `CfButtonTest` change. The commit subject becomes `feat(designsystem): add the smartphone glyph`.
- Tests: `make generators-check` (`vectors-check`) passes.

### Task 3: `feat(ui): add the secure window effect`
- `…/ui/secure/SecureWindow.kt`, `SecureWindowCounter` (§5.13).
- Tests (`@Logic`, `core/ui/src/test/…/secure/SecureWindowTest.kt`): `flagIsSetWhileShownAndClearedAfter`; `nestedUsesKeepTheFlagUntilTheLastLeaves` (two `SecureWindow(true)`, remove one → still set, remove both → cleared); `disabledDoesNothing`; `aFlagSetElsewhereIsNotTouchedWhenUnused` (enabled = false never clears a pre-set flag). Assert on `activity.window.attributes.flags and FLAG_SECURE`.

### Task 4: `feat(data): apply account security results to the store`
- `…/data/security/AccountSecurityStore.kt` (`:core:data`, public): `class AccountSecurityStore(database, settings: SettingsRepository)` with `suspend fun apply(change: AccountSecurityChange, result: AccountSecurityResult, permit: SessionMutationPermit)` porting `SettingsPagesModel.swift:117-129`: `Snapshot(v)` → `database.write(permit) { StoreApplication.snapshot(v, accountID, it) }`; `ConfirmTwoFactor` + `Codes` → read the current `security` object (or `{}`), set `twoFactorEnabled = true`, `twoFactorConfirmedAt = null`, `settings.upsertSecurity(value, permit)`; `Passkeys(list)` → set `passkeyCount = list.size` and upsert (10d uses it); everything else → nothing. Never write the codes, the code, the setup key or a password anywhere.
- Tests (JVM, JDBC driver like 4d's DB tests, `core/data/src/test/…/security/AccountSecurityStoreTests.kt`): `secretsStayOutOfSQLiteAndPendingBodyClockSurvivesSecuritySnapshot` (port, Swift l.229–250: enqueue a body-clock `Settings` patch `{"age_band": "65_plus"}`; apply `ConfirmTwoFactor("123456")` → `Codes(["secret-code"])`; no settings row's raw JSON contains `secret-code` or `123456`; `security.twoFactorEnabled == true`; outbox kinds `["settings"]`); (the Swift test's tail, `performSecurity` after `stop()` throwing, is ported in Task 10 as `staleSessionThrowsStaleSessionException`); Android extras `disableSnapshotWritesTwoFactorOffAndKeepsPendingBodyClock`, `revokedPermitWritesNothing` (throws `StaleSessionException` from `SessionMutationPermit.whileValid`, store unchanged).

### Task 5: `feat(settings): port the settings security model`
- `SecretField.kt`, `SecuritySummary.kt`, `SettingsSecurityModel.kt` (with `SettingsSecurityPrompt`, `SecurityState`) (§5.1–§5.3).
- Fakes in `…/fst/testing/`: `FakeSecretField(value)` (`read()` returns the value, `clear()` empties it and sets `cleared = true`), `ScriptedSecurity` (a queue of results/throws, records changes; can suspend on a `CompletableDeferred`).
- Tests (JVM, `…/fst/security/SettingsSecurityTests.kt`, Swift names kept, `runTest(timeout = 60.seconds)`):
  - `promptWorksAgainAfterLeavingAndReturning`: reveal → codes `["one"]`; `clearSecrets()` → codes empty, prompt null; reveal again works; 2 calls.
  - `lateResultCannotRestoreSecretsOrFinishANewRequest` as `…StartNewRequestFalse` / `…StartNewRequestTrue` (or Parameterized): a suspended reveal; `clearSecrets()` → not busy, no codes, no prompt; optionally a second suspended reveal; completing the first with `["late"]` leaves codes empty and `busy == startNewRequest`; completing the second with `["current"]` → codes `["current"]`, not busy, prompt null.
  - `passwordSuccessAndCancelPromptClearSecrets`: changes `[Password("old", "New password1!", "New password1!")]`, message "Password updated", the three fakes cleared; `showPrompt(Disable)` then `cancelPrompt()` → prompt null; still one change.
  - `passwordValidationAndSubmissionClearCredentialsAndReportOffline`: mismatch → no call, `errors["password_confirmation"]`, fakes **not** cleared; then matching → one call, fakes cleared, `message == "Needs a connection."`, `offline`, not busy.
  - `wrongPasswordKeepsPromptOpenAndSetupSurvivesConfirmationFailure`: wrong → prompt stays `Enable`, prompt secret cleared, `errors["current_password"]`; correct → prompt null, setup key `secret`; code `"1"` → `errors["code"]`, code not cleared; `"123456"` with `Server(503)` → setup kept, no codes, code cleared, `twoFactorActivationFailed`, errors empty, not busy; then success → setup null, codes `["one", "two"]`, message "Two-factor is on. Save your recovery codes now."; `clearSecrets()` → empty.
  - `failedTwoFactorSetupExplainsThatTheAccountIsUnchanged`: `Server(503)` on enable → failed flag, prompt still `Enable`, no setup, secret cleared; `cancelPrompt()` → flag false.
  - `throttlingBlocksFurtherRequestsUntilDeadline`: `Throttled(60.0, 429)`, `now = 100 s` → `locked`, `remaining(130 s) == 30`; a second submit makes no call.
- Android extras (`SettingsSecurityEdgeTests`): `policyKeysMapToTheIosCopy` (one case per key in §5.3 including `:min` with `{"min": 12}` → args `{"min": "12"}`, "don't match yet" → "don't match", `This password has appeared in a data leak…` → "Check the highlighted field."); `conflictSentencesClearSetupAndCodesAndBecomeMessages` (all three); `offlineInPromptKeepsPromptOpen`; `sendResetSetsCheckYourInbox`; `codeMustBeSixAsciiDigits` (`"12345"`, `"12345a"`, `"١٢٣٤٥٦"` Arabic-Indic → error); `staleSessionIsSilent` (`perform` throws `StaleSessionException` → returns quietly: no message, not busy, no rethrow); `cancellationIsRethrownAndBusyResets` (cancel the calling job while `perform` suspends → the job ends cancelled and `busy == false`); `clearSecretsKeepsRetryAt`; `feedbackRevisionIncrementsPerMessage`; `stateToStringRedactsSecrets`; `emptyPromptPasswordMakesNoCall`. `SecuritySummaryTests`: missing row → null; `twoFactorEnabled` `"true"` string → false; `passkeyCount` absent/`2`/`2.5`/`-1` → null/2/null/null.
- If `TextFieldState` is not needed here (it is not: the model takes `SecretField`), the class stays free of Compose imports; Konsist's feature rules stay green.

### Task 6: `feat(settings): add the sensitive clipboard and recovery codes document`
- `SensitiveClipboard`, `ClipboardPort`, `AndroidClipboardPort`, `RecoveryCodesDocument` (§5.14); `clipboard_label` in `strings_untranslatable.xml`.
- Tests: JVM `SensitiveClipboardTests` (virtual time): `clearsItsOwnClipAfterSixtySeconds` (59 s → not cleared, 60 s → cleared); `leavesAForeignClipAlone` (timestamp changed); `unreadableAtExpiryClearsOnNextForeground`; `aSecondCopyRestartsTheTimer`; `textIsNeverInToString`. JVM `RecoveryCodesDocumentTests.bytesAreUtf8LfWithoutBomOrTrailingNewline` (`["8f3k-29dm","q7zp-4wxr"]` → exactly `8f3k-29dm\nq7zp-4wxr` bytes; first byte is not `0xEF`; no `\r`). Robolectric `@Logic` `AndroidClipboardPortTest` with `@Config(sdk = [33, 36])`: the primary clip's description extras hold `EXTRA_IS_SENSITIVE == true`, label `ChronoFlow`; `clear()` empties it.

### Task 7: `feat(settings): add the shared password prompt`
- `…/fs/security/prompt/SettingsPasswordPrompt.kt`, `SettingsPasswordPromptHost.kt`, `PromptAlertLine`, `PasswordPromptMetrics` (§5.5); `SecurityCopy.kt` (§5.4).
- Tests (`@Logic`, `…/fst/security/prompt/SettingsPasswordPromptTest.kt`, hosting the content directly): `emptyPasswordDisablesSubmit`; `checkingShowsLabelSpinnerAndDisablesFieldForgotAndCancel` (submit text "Checking…", field `disabled()`, cancel `disabled()`); `wrongShowsErrorUnderTheField` (tag `.error` text "The provided password was incorrect.", field `SemanticsProperties.Error` set); `lockedCountsDownEverySecond` (retryAt = now + 2820 s → "…47 minutes."; advance the fake clock and `mainClock` by 2 761 s → "Too many attempts. Try again in 59 seconds."; at 0 the line is gone and the field is enabled); `forgotOpensTheAlertAndSendLinkCallsReset` (email interpolated); `forgotIsDisabledWhileBusyOrLocked`; `imeDoneSubmitsOnlyWhenEnabled`; `destructiveUsesDestructiveStyle` (`Disable`); `activationFailureShowsTheBanner`; `otherMessagesShowAsMutedText`; `dutchCopy` (nl: "Bevestig dat jij het bent", "Annuleren", "Wachtwoord vergeten?"). If `CfModalBottomSheet`/`CfAlertDialog` gain an optional parameter (Refresh R7), it is additive and lands in this commit. Host tests: `SettingsPasswordPromptHostTest` (`@Logic`): at 411 dp a sheet is shown and back press while busy keeps it; at 841 × 701 a dialog is shown (`@Layout`: width 540 dp ±1, centred, height ≤ 631 dp at `largeFont`), and an outside click while busy keeps it.

### Task 8: `feat(settings): add the security and password pages`
- `SecurityScreen`, `PasswordScreen`, `SecurityViewModel` (password part), `SettingsSecurityLock`, feedback snackbar, the security counter on `SettingsDebugCounters` (debug + release twins), `SecurityMetrics`, `PasswordMetrics`; `SettingsPage(spacing)` (Hand-in 6: needed); extend `SettingsDependencies` (§5.9) and its fakes.
- Tests (`@Logic`, `…/fst/security/SecurityScreenTest.kt`, `PasswordScreenTest.kt`, with a fake `SettingsDependencies`): Security: rows and badge "Off"/"On" from the summary; Passkeys row hidden with `passkeyCount = null`, hidden in a release-like run (label hook null) and shown in debug with "2 passkeys"; missing security row → `settings.missing`; taps push `Route.Password` / `Route.TwoFactorSettings`. Password: `passwordFieldsSubmitAndShowFeedback` (port of the iOS UI test, fake deps whose `performSecurity` is a `ScriptedSecurity` returning `Changed`: type `password`, `New password1!`, `New password1!` into the three tags, click `settings.password.save`, `waitUntil` the debug counter is 1 and `settings.security.feedback` shows "Password updated"); errors show the notice and each field's error under the right field; save disabled while locked and the lock line counts down; success → snackbar counter 1 with "Password updated"; offline → "Needs a connection."; the three fields are empty after a submitted attempt.

### Task 9: `feat(settings): add the two-factor page`
- `TwoFactorScreen` (Intro, Setup, Manage, codes), `QrMatrix`, `SettingsAuthenticatorQr`, `RecoveryCodesDownload`, `SecurityViewModel` (two-factor part), `TwoFactorMetrics` (§5.8, §5.13, §5.14).
- Tests:
  - JVM `QrMatrixTests`: `encodesTheMockOtpauthUrl` (size is `21 + 4k`; the three finder patterns are dark 7 × 7 rings at (0,0), (size−7,0), (0,size−7); encoding twice gives equal bits); `toStringHidesContent`.
  - `@Logic` `TwoFactorScreenTest`: intro → enable opens the prompt with "Continue"; setup shows `settings.twoFactor.secret` with the key and the QR node with its description; failed confirm shows `settings.twoFactor.failure` and "Try again"; codes state shows 8 `settings.twoFactor.recoveryCode` nodes, `settings.twoFactor.copyCodes` calls the clipboard port with the joined codes and `sensitive = true`; manage shows reveal/regenerate/disable and "Since 30 Oct 2026" for `twoFactorConfirmedAt = "2026-10-30T08:00:00.000000Z"`, no Since line for `null` or `"garbage"`; the toast counter is 1 after a successful confirm; the prompt `TextFieldState` is empty after the prompt closes; `enabledTwoFactorCanCancelThenDisableAndReturnToIntroduction` (port of the iOS UI test; fake deps: settings flow starts with `security.twoFactorEnabled = true`; `performSecurity(DisableTwoFactor("password"))` returns `Snapshot` and sets the fake flow's `twoFactorEnabled` to false, as Task 4's store would: click `settings.twoFactor.disable` → `settings.security.prompt.password` exists; click `settings.security.prompt.cancel` → the prompt is gone and `settings.twoFactor.disable` exists; disable again, type `password` into the prompt field, click `settings.security.prompt.submit` → `waitUntil` `settings.twoFactor.enable` exists and `settings.twoFactor.disable` does not).
  - `@Logic` `RecoveryCodesDownloadTest`: Download fires `ACTION_CREATE_DOCUMENT` with type `text/plain` and `EXTRA_TITLE` "Recovery codes.txt" (nl "Herstelcodes.txt"); with no handler (Robolectric default) the fallback chooser carries `ACTION_SEND` with the joined codes; a result Uri for a temp file receives exactly `RecoveryCodesDocument.bytes(codes)`; a result after `clearSecrets()` writes nothing.
  - `@Logic` `TwoFactorSecureWindowTest`: FLAG_SECURE is set in Setup and codes-shown, clear in Intro and Manage-without-codes, and cleared after leaving.

### Task 10: `feat(app): wire the security pages, store and seeds`
- `SessionSettingsDependencies.performSecurity`: `if (!isCurrent()) throw StaleSessionException()`; `try { session.sync.accountSecurity(change, snapshot.session, snapshot.epoch) } catch (e: CancellationException) { currentCoroutineContext().ensureActive(); throw StaleSessionException() }`; re-check `isCurrent()` (else `StaleSessionException`); `AccountSecurityStore.apply(change, result, permit)` with `app.controller.mutationPermit(snapshot)` (null → `StaleSessionException`), on `app.dispatchers.io`; `session.sync.requestSync()`; return the result. Plus `clipboard` (process-level, on `app.appScope`), `passwordPromptSeed` (§5.15, via `LaunchPlatform`), the `SettingsEntry`/`AppShell` routes, the `isBuilt` flip (Refresh R8).
- Tests:
  - JVM `…/fst/security/SessionSettingsSecurityTests.kt` (*[Refresh R8]*: in `:feature:settings` on `support/SettingsFixture`, whose `deps.performSecurity` mirrors `SessionSettingsDependencies.performSecurity`; not in `:app`): `disableSnapshotClearsSecretsAndObservedEnabledStateForIntroduction` (port, Swift l.66–118, with 4c's `MockDataSource` signed in as demo and the real 4d/4e parts as in 10a's `SettingsHomeModelTests`: for `retainSetup` true/false run enable → (confirm) → disable through a real `SettingsSecurityModel(perform = deps::performSecurity)`; the last change is `DisableTwoFactor("password")`; prompt null, not busy, no setup, no codes, no message, no errors; the observed `security.twoFactorEnabled` becomes false); `staleSessionThrowsStaleSessionException` (signed out between the call and the response).
  - JVM `ShellNavSettingsSeedTest` (`app/src/test/…/settings/`): extend with `twoFactorSetup`; the "seed password non-null only in debug" check goes in `app/src/testDebug/…` (debug `LaunchPlatform`).
  - Robolectric `@Logic` `…/appt/settings/SecuritySecretsNotSavedTest.kt`: seeded two-factor setup with codes shown and `"hunter2"` typed in the prompt field; `scenario.onActivity { val b = Bundle(); instrumentation.callActivityOnSaveInstanceState(it, b); val p = Parcel.obtain(); b.writeToParcel(p, 0); … }` then assert the marshalled bytes contain none of `JBSWY3DPEHPK3PXP`, any recovery code, `hunter2`, `123456` (search UTF-16LE and UTF-8 encodings); after `scenario.recreate()` setup and codes are still shown and the prompt field is empty (`14 §3.4`).

### Task 11: `test(settings): guard and capture the security boards`
- `…/fst/security/SettingsSecurityGuardsTest.kt` (`@Layout`): Security, Password (idle, errors + notice, locked), prompt (empty, checking, wrong, locked, forgot alert, activation failure) as sheet and as dialog, Two-factor (intro, setup, setup failed, manage, codes) × `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en/nl × light/dark; `Guards.assertAll`. One test per profile (Parameterized).
- `…/fst/security/SettingsSecurityCaptureTest.kt` (`@Capture`), fake `perform` scripted per state, fixed clock, account email `rick@example.com`, `twoFactorConfirmedAt = "2026-10-30T08:00:00Z"`, codes `8f3k-29dm q7zp-4wxr m2vt-8ncb h6ye-1lsa c9rk-5jtd w4bn-7ugq t1xf-3hzo p5ma-6evk` (board), setup key `JBSWY3DPEHPK3PXP`. Names (`BoardCapture.capture(rule, …)`), all `_pixel8_en_light` unless stated:
  `SetSecurity` (passkeyCount 2, off), `SetPassword` (confirm mismatch → notice + error), `PasswordPrompt` (Reveal, empty), `PasswordPrompt2FAOn` (Enable, empty), `PasswordPromptChecking` (Enable, checking; OQ-10c-3), `PasswordPromptForgot` (Enable + alert; OQ-10c-3), `PasswordPromptLocked` (Disable, locked 2820 s; OQ-10c-3), `PasswordPromptNewCodes` (Regenerate, empty), `PasswordPromptNL_pixel8_nl_light` (Regenerate, wrong), `PasswordPromptWide_foldInner_en_light` (Reveal, dialog), `PasswordPromptWrong` (Disable, wrong), `Set2FA`, `Set2FANL_pixel8_nl_light`, `Set2FASetup`, `Set2FAOn` (codes + the snackbar presented; OQ-10c-7), `Set2FACodes` (codes, no snackbar), `Set2FAManage`, `Set2FAManageNL_pixel8_nl_light`, `Set2FAManageWide_foldInner_en_light`, `Set2FAWide_foldInner_en_light`; plus `_pixel8_en_dark` twins of `SetSecurity`, `PasswordPrompt`, `Set2FASetup` (the QR tile stays white) and `Set2FACodes`.

### Task 12: `test(app): walk two-factor setup on the device`
- `…/appat/settings/SettingsSecurityDeviceTest.kt` (`@Smoke`, launched seeded via `ScreenTest`, each < 10 s):
  - *[Refresh R8]* Add `androidx.test.espresso:espresso-intents` to the catalog and `app` `androidTestImplementation` in this commit (it is not there yet).
  - `seededTwoFactorSetupConfirmsAndShowsRecoveryCodes` (the one journey; `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_SETTINGS_ROUTE=twoFactorSetup`): wait for `settings.twoFactor.code` and `settings.twoFactor.secret`; assert `FLAG_SECURE` on the activity window (`scenario.onActivity`); type `123456`; tap `settings.twoFactor.confirm`; wait for the first `settings.twoFactor.recoveryCode`; `FLAG_SECURE` still set; stub `ACTION_CREATE_DOCUMENT` with Espresso-Intents to return a `file://` Uri in the test app's cache dir; tap `settings.twoFactor.download`; the file holds the 8 codes joined by `\n`, no BOM; delete the file.
  - `promptKeepsSubmitAboveTheKeyboard` (seeded screen check, not a journey; `CF_AUTH_TEST_SIGNED_IN=twofactor`, `CF_SETTINGS_ROUTE=twoFactor`): `FLAG_SECURE` is **not** set on Manage-without-codes; tap `settings.twoFactor.disable`; wait for `settings.security.prompt.password`; click it so the IME shows (`WindowInsetsCompat.isVisible(ime())` via `scenario.onActivity`); `settings.security.prompt.submit` is displayed and its bounds end above the IME top. (IME placement is emulator-only, AGENTS "Tests" 1 and 6.)
  - The two iOS UI tests `testPasswordFieldsSubmitAndShowFeedback` and `testEnabledTwoFactorCanCancelThenDisableAndReturnToIntroduction` are **not** emulator tests here: they check logic and navigation inside one screen, so they are ported on Robolectric in Task 8 and Task 9 under their Swift names (AGENTS "Tests" 1–2: one emulator journey per feature).

### Task 13: `docs(settings): record security acceptance`
- Tick the 10c boxes in `plan.md`; change the Phase 10c prompt line "on wide windows a 560 dp dialog" to the width chosen in OQ-10c-1; append the §9 rows to "Open questions for the user" as **OQ-A502 … OQ-A518** in §9 order, each starting "(10c OQ-10c-n)" (Refresh R1); new questions take A519 and up; note any name differences from this brief and the ZXing version.

---

## 7. Acceptance criteria

- `make quick` and `make check` (with `pixel` booted) pass; `make generators-check` passes; `make size` passes.
- `make targeted ONLY="feature:settings:SettingsSecurityTests feature:settings:SettingsSecurityEdgeTests core:data:AccountSecurityStoreTests feature:settings:SessionSettingsSecurityTests app:SecuritySecretsNotSavedTest"` is green within budgets (`build/test-results/*/timings.json`).
- `make targeted ONLY="app:SettingsSecurityDeviceTest"` passes on `pixel` (2 tests, each < 10 s).
- `make captures ONLY=SetSecurity,SetPassword,PasswordPrompt,Set2FA` writes every capture in Task 11 to `build/captures/`; the reviewer compares each side by side with its board.
- No secret in the saved `Bundle` (Task 10 test); `FLAG_SECURE` set on Setup and codes, absent elsewhere (Tasks 3, 9, 12).
- All 20 boards match, except the recorded OQs.
- Konsist green: no dp literal outside a `@BoardMetrics` object, no raw colour, `Text` gets resources only, `secureFieldStateIsNeverSaved` (files with `CfSecureTextField(`). The reviewer greps the prompt (`BasicSecureTextField`) and two-factor files for `rememberSaveable`/`rememberTextFieldState`/`SavedStateHandle` by hand (Refresh R4).
- No timeline code changes, so `make perf-timeline` is not required. The two-factor page does no main-thread I/O: StrictMode (debug) logs no violation during the Task 12 journey (check `adb logcat -s StrictMode` output in the run log).

---

## 8. Review checklist (Opus reviewer)

1. `run` matches Swift l.139–218 line by line: `generation` captured before the call; late results ignored; `retryAt`/`offline` reset only on success; `busy` reset only when the generation still matches; `CancellationException` rethrown; stale session silent.
2. `submitPrompt` clears the password **before** the request; a wrong password keeps the prompt open with an empty field; success closes it.
3. `changePassword` keeps the fields on a local validation error and clears all three once validation passes, success or failure.
4. `clearSecrets` keeps `retryAt`; leaving (back, tab switch) clears setup and codes; a configuration recreation keeps them; the prompt reopens empty.
5. No password, code, setup key or recovery code in a `Bundle`, `SavedStateHandle`, `rememberSaveable`, the DB, the outbox, `toString()` or a log (grep for `Log.`, `println`, `Timber`). `SecurityState.toString()` and `QrMatrix.toString()` redact.
6. `AccountSecurityStore` writes only `twoFactorEnabled`/`twoFactorConfirmedAt`/`passkeyCount` and snapshots; the pending body-clock row survives.
7. 409 sentences arrive as `Validation({"form": …}, 409)` and become `message`, clearing setup and codes.
8. The lock countdown is read only in the two leaves; nothing else recomposes per second.
9. Prompt: Cancel, Forgot, field, sheet swipe, back and outside click are all inert while busy; the button text is "Checking…" with a spinner; disabled alpha 0.45; destructive only for Disable.
10. Wide prompt is a `Dialog` with `usePlatformDefaultWidth = false`, width per OQ-10c-1, height ≤ 90 %; phone is `CfModalBottomSheet`; the switch uses window width, not `onSizeChanged`.
11. QR: generated off the main thread; drawn as a cached path; tile white and modules dark in both themes; content description set.
12. `FLAG_SECURE` is reference-counted and removed when the secret state ends, not just when the page leaves.
13. Clipboard: `EXTRA_IS_SENSITIVE` on 33+; the 60 s clear never clears a foreign clip; the timer survives leaving the page (process scope).
14. Download: `CreateDocument("text/plain")`, name from `android_recovery_codes_file_name`, bytes UTF-8 without BOM, LF, no trailing newline; written on IO; fallback `ACTION_SEND`; nothing written to app storage.
15. All copy from resources through `CopyIndex`; b1 is the Android overlay string; no new copy except the untranslatable `clipboard_label`.
16. Tags exactly as in §5; tests query by tag; ported tests keep the Swift names.
17. Passkeys row hidden in release until 10d; Security home row now built.
18. Commits: one per task, Conventional Commits, no trailer.

Typical mistakes: storing the prompt password in `SecurityState`; using `rememberTextFieldState()` (saveable); `runCatching` around `perform` (swallows cancellation); `Instant.plusSeconds(Long)` truncating a fractional `retryAfterSeconds`; reading the ticker in the page root; FLAG_SECURE added in `LaunchedEffect` without removal; the dialog sized from `LocalConfiguration.screenWidthDp` (ignores multi-window); `ClipDescription.EXTRA_IS_SENSITIVE` referenced on API < 33 without a guard; `openOutputStream(uri)` without `"wt"` leaving old bytes; `joinToString(System.lineSeparator())`.

---

## 9. Open questions (proceed on the default; append each to `plan.md` as OQ-A502 … OQ-A518, Refresh R1)

| id | Question | Default |
|---|---|---|
| OQ-10c-1 | Wide prompt width: `PasswordPromptWide` draws 540 dp; plan.md and `14 §1.2` say 560 dp. | **540 dp** (the board; looks come from boards), max height 90 %. Task 13 updates the plan line. |
| OQ-10c-2 | Prompt header: the board draws Cancel + title at 17/22 (iOS nav bar). | `settingsStrong` 16/24 title, `TextButton` Cancel in `dawnText`, row min 48. |
| OQ-10c-3 | `PasswordPromptChecking` and `PasswordPromptForgot` use the deferred `email` action; `PasswordPromptLocked` uses `delete` (10d). | Capture the same state with an in-scope action: Checking and Forgot with `Enable` (primary), Locked with `Disable` (destructive, as `delete`). 10d recaptures Locked with Delete. |
| OQ-10c-4 | Prompt field fill: board `rgba(118,118,128,.12)` vs iOS `surface2`; board trailing lock glyph vs iOS none. | `surface2` (iOS) with the board's 18 dp lock glyph. |
| OQ-10c-5 | Buttons: boards draw some at 40 (bundle `size="lg"` and `h-10`: Change password, setup Turn on, codes Copy/Download) and the setup-key Copy at 36 (default `h-9`) where iOS uses 48 (`actionMinHeight`) and 40 (`walkthroughDoneHeight`). The intro Turn on is 48 on both. | iOS heights: 48 for Change password, setup Turn on, codes Copy/Download; 40 for the setup-key Copy. |
| OQ-10c-6 | 5a's `SecretField` lives in `:feature:auth`; features cannot depend on each other. | A same-shape `SecretField` in `:feature:settings`; consolidating into `:core:model` is a later refactor. |
| OQ-10c-7 | The board toast (popover, dawn check) vs a Snackbar (OQ-A5). | `CfSnackbarHost`, `Long`, as 10a OQ-10a-10; `Set2FAOn` is captured with the snackbar shown. |
| OQ-10c-8 | Show `security.passwordRules` as a hint on the Password page? | No: no board, iOS shows none. Server policy errors are mapped (OQ 21). |
| OQ-10c-9 | Setup key grouping: the board shows `JBSW Y3DP EHPK 3PXP`; iOS shows the raw key. | Raw key (iOS); Copy copies the raw key. |
| OQ-10c-10 | "Copied" feedback after Copy. | None: no iOS string; Android 13+ shows its own clipboard confirmation. |
| OQ-10c-11 | Offer to open `otpauth://` in an authenticator app (`03 §7`). | Not built: no board or iOS. |
| OQ-10c-12 | Settings home Security subtitle: 10a OQ-10a-1 (plan OQ-A462) expected 10c to switch to "Password, passkeys, two-factor". | Keep "Password, two-factor" (iOS at the pin uses it even with passkeys built; 10d OQ-10d-12 agrees). The A513 row notes it supersedes A462's "10c switches" clause. |
| OQ-10c-13 | Board Dutch differs from the catalog ("Wachtwoord opnieuw instellen?", "ingelogd", "inloggen" in the intro). | The catalog wins (generated copy); flag for the user. |
| OQ-10c-14 | Download write failure has no designed copy. | Show "Something went wrong" (existing Settings key) in the feedback snackbar. |
| OQ-10c-15 | iOS 2FA action rows are `SettingsRow` (64, subtitle 14/20); the boards draw 60 min, title 16/22, subtitle 13/18, no disc. | `CfRow(lock = true)` as iOS; record the 4 dp difference. The "Recovery codes" eyebrow-to-card gap follows the board (8) rather than iOS `space3` (12); record it. |
| OQ-10c-16 | The Download emulator test stubs the document picker with Espresso-Intents instead of driving DocumentsUI (another app's UI, flaky). | Stub; the real picker is checked by hand once on `pixel` and noted in the PR. |
| OQ-10c-17 | Clipboard clear when the app is in the background at 60 s (Android 10+ hides the clip from background apps). | Clear on the next foreground if the clip is still ours; a process death leaves it (the system clears clips after an hour on 13+). |
