# Phase 10a: Settings home, sign-out and account pages

- **Branch:** `feat/phase-10a-settings-home`. Start it from the top of the stack once **6a** (shell, `ShellNav`, `ShellViewModel` *[Refresh: not `ShellHolder`, R3]*, `RoutePlaceholder`, `CategorySwatch` *[Refresh: in `:core:ui`]*) and **4e** (`SessionController`, `SessionGraph`, `SessionGraphs`, `Route.*` settings routes, `DebugSeedRouting.settingsRoute`) are in it. Through them (and the earlier waves 1–7, which finish before wave 8) the stack carries 4a (`DevicePreferences`, `LanguageChoice`, records, repositories), 4c (`MockDataSource`), 4d (`OutboxPayload`, `OutboxFlusher`, `FullPull`), 2 (primitives), 1a (`CopyLineFormatter`, `stepLabelKey`, `PlanClock`), 1d (`SystemPlanClock`), 0b (tokens), 0c (strings, `CopyIndex`), 0e (`ConfigurationPolicy`, `ConfigurationRules`, guards, captures, `ScreenTest`, seeds). If one of these is missing, stop and report it.
- *[Refresh R1: 6d, 7 and 8 are all merged at c7818ff0; the fallbacks below no longer apply and every Phase 8 item is required.]* **Not guaranteed in the stack** (plan.md waves: Phase 8 runs in parallel in wave 9, 6d is wave 11, Phase 7 is wave 12). Do **not** stop when they are missing; use the fallbacks in §4: Phase 8 (Terms `signOutConfirmation` slot, `GraphOverrides.mutationsOffline`, `CF_AUTH_TEST_TERMS_CHANGES_PENDING`), 6d (`RefusedChangeModel` and the shell refusal Snackbar host; 10a uses neither), Phase 7 (`MinuteClock`; 10a does not need it).
- **Depends on:** 6a, 4e. **Wave:** 9. **Plan entry:** `plan.md` "### Phase 10a" (l.3027–3067 *[Refresh; was l.2390–2429]*). Also read in `plan.md`: R22, OQ 29, OQ-A5 (toasts are `CfSnackbarHost`), OQ-A24, OQ-A25 (with `system`, follow later phone-language changes), OQ-A37 (560 dp column from 600 dp), "Android spec: configuration and appearance" (l.348–356), "window policy" (l.357–371), "state survival" (l.373–383).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS at `f391a1fa`, web at `e36368ec` and the boards at canvas version `1794774555-3fd6`. **Checked** 2026-11-16 against those pins, `plan.md` and `AGENTS.md`; corrected: phase dependencies (8/6d/7 optional), `stepLabelKey` is 1a's, the clock comes from 1a/1d, the `SettingsHomeState` settings-row parsing and `syncedAt` source, the profile `receive` rule, the stored-state flags, the test zone (UTC), exact Lucide manifest entries, the iOS doc paths. Before you code, open the merged code of 1a, 1d, 4a, 4d, 4e, 6a (and 6d and 8 if merged) and use **their** names. Where a name in this brief differs, keep theirs and note it in the PR. Never rename earlier code.
- **Tests:** every test follows `AGENTS.md` "Tests": cheapest tier, budgets (JVM and warm Robolectric < 1 s, cold costs warmed in `@BeforeClass` via `measureClassWarmup` *[Refresh; was Robolectric < 5 s]*, emulator < 10 s), no copy assertions on screen (R9), exact tag queries, waits with `waitUntil`, never `Thread.sleep`, hang caps (`runTest(timeout = 60.seconds)`). Tier annotations are 0e's (`net.chronoflow.core.testing.tiers`): Robolectric `@Logic` / `@Layout` / `@Capture`; emulator `@Smoke`. Transient UI (snackbars) is asserted through a debug "presented" counter, never by catching it on screen.
- **Tags:** **[F]** fact read in source (path given). **[R]** a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` read with `git -C I show f391a1fa:<path>`. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `W/` = `/Users/rickgout/Sites/chronoflow-new` at `e36368ec`. `DC/` = `_design/canvas/`. Board CSS px map 1:1 to dp.
- **Paths:** `…/fs/` = `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/`; `…/fst/` = `feature/settings/src/test/kotlin/net/chronoflow/feature/settings/`; `…/ds/` = `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/appd/` = `app/src/debug/kotlin/net/chronoflow/debug/`; `…/appt/` = `app/src/test/kotlin/net/chronoflow/app/`; `…/appat/` = `app/src/androidTest/kotlin/net/chronoflow/app/`.
- **Read-only sources:** never write to, build in, run prebuild in, or change the git state of `chronoflow-ios`, `chronoflow-new`, `chronoflow-mobile` or `_design/canvas`.

## Refresh notes (2026-11-21, against c7818ff0)

Checked with `git grep` and file reads at HEAD `c7818ff0` (stack top = 9h). **These notes override the body wherever they differ.** Inline edits below are marked *[Refresh]*. Repeat the spot checks in R1–R5 before Task 1 and stop on any mismatch.

**R1. Stack state.** Every dependency is merged, including the three the header calls "not guaranteed": 6d (`RefusedChangeModel`; `LocalRefusedChanges` is provided in `AppShell`), 7 (`MinuteClock`, `core/ui/src/main/kotlin/net/chronoflow/core/ui/clock/MinuteClock.kt`; 10a still does not use it) and 8 (Terms slot, `GraphOverrides.mutationsOffline`, `CF_AUTH_TEST_TERMS_CHANGES_PENDING`). Every "only if Phase 8 is merged" item is therefore **required**: §5.5 Terms slot, the Task 6 Terms test, the Task 12 `SignOutPending` capture. Record "Phase 8 merged" in the PR. Ignore the §4 fallbacks.

**R2. `:feature:settings`** is registered (`settings.gradle.kts:66`; `app/build.gradle.kts:105` has `implementation(project(":feature:settings"))`) but holds only `build.gradle.kts` (plugin `chronoflow.android.feature`, namespace `net.chronoflow.feature.settings`): no sources, no tests, no `dependencies {}` block. 4a–9h put no placeholder route, screen or tab code there. 10a adds the dependencies. `feature/onboarding/build.gradle.kts` is the closest precedent, since its Terms tests drive a real `SessionController` against JDBC and the mock: `:core:sync`, `:core:data`, `androidx.activity.compose`; tests `:core:testing`, `:core:testing-junit`, `:core:testing-compose`, `:core:data-mock`, `:core:database-testing`, coroutines-test and turbine. Add `:core:plan` for `stepLabelKey` and `PlanClock`. The `chronoflow.android.feature` convention plugin already adds `:core:designsystem`, `:core:ui`, `:core:navigation` and `:core:model` (`AndroidFeatureConventionPlugin.kt:10`). `feature/onboarding/src/test/kotlin/net/chronoflow/feature/onboarding/support/OnboardingFixture.kt` shows the JVM session fixture. Features may not import other features (Konsist `featuresDoNotImportOtherFeatures`).

**R3. Shell (6a): names differ, and Hand-in 6 already passes.**
- There is no `ShellHolder` and no `DisposableEffect { holder.clear() }`. Shell state is `ShellViewModel` (`app/src/main/kotlin/net/chronoflow/app/shell/ShellViewModel.kt`, created by `rememberShellViewModel(root) = viewModel { ShellViewModel(ShellNav.fromRoot(root)) }`), with `push(route)`, `back()`, `select(tab)` and `seed(ShellStart)`. `ShellRecreationTest` (`app/src/testDebug/kotlin/net/chronoflow/app/shell/ShellRecreationTest.kt`) already proves the selected tab, the pushed page and the tab ViewModels survive `recreate()`. So skip the throwaway test, add no `isChangingConfigurations` guard, and drop "shell keeps `route.account` across `recreate()`" from Task 10.
- `ShellNav`, `ShellNavState`, `ShellStart` and `ShellTab` live in `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/shell/`. `ShellStart` (in `ShellNavState.kt`) has `Tab(tab)` and `Trip(tripId)`. Add `ShellStart.Settings(path: List<Route>)` there with its `ShellNav.seed` branch, tested on the JVM in `core/navigation/src/test/kotlin/net/chronoflow/core/navigation/shell/`.
- `AppShell` (`app/src/main/kotlin/net/chronoflow/app/shell/AppShell.kt`) draws the Settings tab root as `settings = { render.value(Route.Settings) }` and every other pushed route through `page = { route -> … else render.value(route) }`. `render` is the placeholder lambda that `CfRoot.kt` passes (`AppShell(route, onRootBack) { RoutePlaceholder(it, LaunchPlatform.routeLabel) }`). 10a replaces those branches for `Settings`, `Account`, `Profile` and `Language`; the other settings routes keep `render.value(route)`.
- `RoutePlaceholder(route, label)` is `app/src/main/kotlin/net/chronoflow/app/RoutePlaceholder.kt` (tag `route.<debugName>`). The "6a label hook" is `LaunchPlatform.routeLabel`: null in release, `DebugRouteLabel` in debug.
- Every settings route already exists in `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/Route.kt`, with `debugName`: `Settings`, `Profile`, `Account`, `Language`, `Security`, `Password`, `TwoFactorSettings`, `Passkeys`, `BodyClock`, `Preferences`, `Reminders`, `Plan`. Add none.

**R4. Seeds (4e, 8).**
- `CF_SETTINGS_ROUTE` is already parsed (Hand-in 4 passes): `TestSeed.settingsRoute: String?` (`app/src/debug/kotlin/net/chronoflow/debug/TestSeed.kt:46`, parsed at `:155`). It is filtered to `profile, account, language, security, password, twoFactor, twoFactorSetup, twoFactorSettings, passkeys, bodyClock, preferences, reminders, plan, delete` (`:64–80`) and kept only with a valid `CF_AUTH_TEST_ID`, like every `CF_*` key. `DebugSeedRouting.current` is that `TestSeed`. Nothing consumes `settingsRoute` yet.
- The seed-to-shell hook is `DebugShellStart.take()` (`app/src/debug/kotlin/net/chronoflow/debug/DebugShellStart.kt`), exposed as `LaunchPlatform.shellStart` (release returns null) and called once from `AppShell` (`LaunchedEffect(holder) { LaunchPlatform.shellStart()?.let(holder::seed) }`). Today it maps only `planRoute`. 10a adds: when `planRoute` is null, `settingsRoute` → `SettingsSeedRoute.path(raw)?.let { ShellStart.Settings(it) }`. `twoFactorSettings` and `passkeys` pass the parser but are not iOS cases, so they map to null (Settings root only), like the iOS `default:`.
- `GraphOverrides.mutationsOffline` exists (`app/src/main/kotlin/net/chronoflow/app/GraphOverrides.kt:20`), so Hand-in 8 passes; do not re-add it. Seeds feed it through the computed `TestSeed.mutationsOffline` (`TestSeed.kt:54`, `walkthroughPending || termsPending`), passed in `TestSeeds.kt:57`. `CF_SETTINGS_TEST_PENDING` becomes a new `settingsPending = flag("CF_SETTINGS_TEST_PENDING")` field on `TestSeed`, OR-ed into that getter. This matches iOS `App/Session/AppServices.swift:63`: `authTest.settingsPending || authTest.walkthroughPending || authTest.termsPending`. Add rows for the key to `TestSeedSessionKeysTest` and to an `OnboardingSeedFlagsTest`-style flag test (`app/src/testDebug/kotlin/net/chronoflow/debug/`).
- The pending row: Phase 8's precedent is `seedTermsChangesPending(database, permit)` (`app/src/debug/kotlin/net/chronoflow/debug/SeedRunner.kt:63`). `MockSessionSeed.run(onSessionReady = …)` calls it **after** a `FullPull` of the seeded account (`MockSessionSeed.kt:73–77`). Add `seedSettingsPending(database, permit)` to the same hook, passing `seed.settingsPending` through `SeedRunner.reseed`. Do not port iOS's `refreshNow()` wait loop (up to 100 tries): it only waits for that first pull. Keep the rest of §5.10:
  - demo account only (`MockScenarioValues.demoEmail`);
  - the first trip with `deletedAt == null && !deletedLocally`, a `currentInputHash` and a cached plan for that hash, and in it the first block with `actionable && checkKey != null`;
  - enqueue `OutboxChange.CheckOff(CheckOffChange(…, status = CheckStatus.Done))` with the permit;
  - require the row in `outbox.pending()`;
  - the same exception messages.

  If the FullPull leaves no cached plan, stop and report; do not add a sync loop.

**R5. Session, data and plan APIs (4a, 4d, 4e, 1a, 1d): the exact shapes.**
- **Session controller** (Hand-in 1 passes). `SessionController.signOut(expected: SessionSnapshot, discardPending: Boolean): SignOutResult` is at `core/sync/src/main/kotlin/net/chronoflow/core/sync/session/SessionController.kt:136`. It also has `activeSession(): SessionSnapshot?`, `mutationPermit(snapshot): SessionMutationPermit?` and `updates: StateFlow<SessionUpdate?>`, where `SessionUpdate(presence, epoch)` is 4e's type. `SignOutResult.SignedOut` and `NeedsConfirmation(pendingCount)` are in `core/model/src/main/kotlin/net/chronoflow/core/model/session/SessionValues.kt`. The controller is `AppGraph.controller`; `SessionGraph` does not carry it.
- **Session graph** (Hand-in 2 passes). `SessionGraph` (`app/src/main/kotlin/net/chronoflow/app/SessionGraph.kt`) is a data class with `epoch`, `snapshot`, `database`, `accounts: AccountRepository`, `trips`, `settings: SettingsRepository`, `outbox: OutboxRepository`, `sync: SessionSync`, `dataSource`, `scope` and `permit: SessionMutationPermit?`. `SessionSync.events()` replays the latest event into a CONFLATED channel (`SessionSync.kt:224`). The live graph is `appGraph.sessionGraphs.current: StateFlow<SessionGraph?>`. Follow `SessionTripsDependencies(appGraph, sessionGraph)` (`app/src/main/kotlin/net/chronoflow/app/shell/SessionTripsDependencies.kt`) for `SessionSettingsDependencies`.
- **Repositories.**
  - `AccountRepository.values(): Flow<AccountRecord?>`
  - `SettingsRepository.values(): Flow<List<SettingsRecord>>`
  - `OutboxRepository`: `values()`, `pending()` and `enqueue(change, permit)`
- **Sync events.** `SyncEvent.Started(accountID: AccountID, epoch)` and `Finished(accountID, epoch, result: SyncResult)`. `SyncResult.state` is a `SyncState`: `Idle`, `Offline`, `VerificationRequired`, `Unauthorized` or `Failed` (`core/sync/src/main/kotlin/net/chronoflow/core/sync/SyncStatus.kt`). `SessionSync.requestSync()` and `refreshNow()` are `suspend`, so `SettingsDependencies.requestSync()` is `suspend` too.
- **Records.**
  - `AccountRecord(id, name, email, hasVerifiedEmail, syncedAt: String?, walkthroughSeen)`
  - `SettingsRecord(section, value: String)`: the JSON is in `value`
  - `OutboxRecord(id, kind, payload, createdAt, attempts, lastError)`
- **Outbox changes.**
  - `OutboxPayload.change(record)` **throws** `LocalMutationFailure.InvalidPayload` on a bad payload. To "fall through", catch `LocalMutationFailure`, not `Throwable` and not `runCatching` (AGENTS "Rethrow `CancellationException`").
  - The types, all in `core/model/src/main/kotlin/net/chronoflow/core/model/`: `OutboxChange.CheckOff(change: CheckOffChange)`, `CheckOffChange(tripID, planInputHash, type: BlockType, localDate, ordinal, status: CheckStatus?)` and `OutboxChange.Profile(ProfileChange(name, email))`.
  - `BlockType` is a generated sealed `RawEnum`, and its wire name is `type.raw`. The identity is therefore `"check:${tripID}:${localDate}:${type.raw}:${ordinal}"`.
  - For `BlockType.Unknown(raw)`, `stepLabelKey` returns null, so the "null label → `Generic`" edge test applies.
- **Device preferences** (Hand-in 3 passes). `DevicePreferences` (`core/datastore/src/main/kotlin/net/chronoflow/core/datastore/DevicePreferences.kt`) has `language: Flow<LanguageChoice>`, `suspend setLanguage(choice, sdkInt)` (writes DataStore, plus `DeviceConfigFile` below 33, on `io`) and `suspend reconcileDeviceConfig(sdkInt)`. The instance is `AppGraph.devicePreferences`. Nothing in `main` calls `reconcileDeviceConfig()` yet. `LanguageChoice` (`core/model/src/main/kotlin/net/chronoflow/core/model/config/LanguageChoice.kt`) has `System`, `En` and `Nl`, with `raw` and `fromRaw`.
- **Clock** (Hand-in 7 passes). `PlanClock` (`core/plan/src/main/kotlin/net/chronoflow/core/plan/PlanClock.kt`, `values(): Flow<Instant>`) and `SystemPlanClock(context)` (`core/data/src/main/kotlin/net/chronoflow/core/data/clock/SystemPlanClock.kt`) exist. Use the shared `AppGraph.planClock`. As in `TripsDependencies`, declare `val clock: PlanClock` (not `Flow<Instant>`) and collect `clock.values()` in the leaf. Tests use `FakePlanClock` from `core/testing`.
- **Date formatter.** `CopyLineFormatter` is a **class**, `CopyLineFormatter(catalog)`; build it from `appGraph.copyCatalogs.catalog()` as `SessionTripsDependencies.formatter()` does. Its `formatDayMonth(value: String, language: Language, zone: String = "UTC")` takes the contracts `Language` and a zone-id **string**. Expose `suspend fun formatter(): CopyLineFormatter` on `SettingsDependencies` and pass `zone.id`.
- **Step labels.** `stepLabelKey(type)` is at `core/plan/src/main/kotlin/net/chronoflow/core/plan/PlanLogic.kt:127`. Resolve it with `planScreenText(key)` (`core/ui/src/main/kotlin/net/chronoflow/core/ui/plan/PlanText.kt`, which calls `CopyIndex.find(CopyTable.Screens, key)`), as `RefusedChangeText.kt` does.
- **Category swatch.** `CategorySwatch(type: BlockType, modifier, size: CategorySwatchSize = Regular)` lives in **`:core:ui`** (`net.chronoflow.core.ui.plan`), with sizes `Small` and `Regular`. The "sleep" category is `BlockType.Sleep`, so `PendingChange.category` is `BlockType?` and there is no separate `PendingCategory`.
- **Phase 8 Terms slot** (Hand-in 5: merged). It is `TermsRoute(…, signOutConfirmation: @Composable (Int, () -> Unit, () -> Unit) -> Unit)` (`feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/terms/TermsScreen.kt:60`), shown while `TermsUiState.pendingSignOutCount != null` (`:199`). The placeholder is at `app/src/main/kotlin/net/chronoflow/app/CfRoot.kt:220`: `{ _, _, _ -> // Phase 10a supplies the pending-change confirmation sheet. }`, not "10a wires SignOutPending here". The slot carries no busy flag, and `TermsViewModel.stay()` already ignores Stay while busy, so the Terms host passes `busy = false`. In the `Route.Terms` branch, read the outbox from `graph.sessionGraphs.current`.

**R6. Design system (Phase 2).**
- **Paths.** There is no `…/ds/button/` folder. `CfButton`, `CfButtonStyle` and `CfButtonSize` are all in `…/ds/component/CfButton.kt`:
  - `CfButtonStyle`: `Primary`, `Secondary`, `Outline`, `Ghost`, `Destructive`, `Dawn`, `OnSky`
  - `CfButtonSize`: `Sm`, `Default`, `Lg`, `Icon`, `Touch`

  `GhostDestructive` does not exist yet; Task 1 adds it in that file. Its test is `core/designsystem/src/test/kotlin/net/chronoflow/core/designsystem/CfButtonTest.kt`. Adding a style adds no new `Cf*` function, so Konsist `designSystemPrimitivesHaveGuards` is unaffected; the gallery is `core/testing-compose/src/main/kotlin/net/chronoflow/core/testing/compose/designsystem/DesignSystemGallery.kt`.
- **Button size and loading.** `Lg40` does not exist; `CfButtonSize.Lg` is 40 dp (`ButtonMetrics.LG_HEIGHT`). `CfButton(loading = true)` requires `loadingDescription`; pass the button's own label, as the auth screens do.
- **Existing components, used as they are.**
  - `CfCard(modifier, rows, shadow, border, spacing, content)`, `CfDivider`, `CfRow`, `CfBadge(text, variant = CfBadgeVariant.Dawn)`
  - `CfSkeleton(modifier, shape)`, in `component/CfBanner.kt`
  - `CfTextField(state: TextFieldState, label, testTag, error, keyboardOptions, onKeyboardAction, contentType, …)`
  - In `system/CfBars.kt`: `CfLargeTopAppBar(title: String, scrollBehavior, modifier, navigationIcon, actions)` and `CfBackButton(contentDescription, onClick)`
  - `CfModalBottomSheet(onDismissRequest, modifier, sheetState, content)`. It already uses `shapes.sheetTop` (`SettingsTokens.sheetRadius`, 38), `card`, the M3 drag handle and the M3 scrim; pass `rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !busy })`.
  - `CfSnackbarHost(hostState, modifier, onPresented)`: use `onPresented` for the debug counter.
  - `WindowPolicy.wideBreakpointDp = 600` (`layout/WindowPolicy.kt`) and `@BoardMetrics` (`layout/BoardMetrics.kt`)
- **Tokens.** The generated `SettingsTokens` (`generated/CfDimens.generated.kt:162`) already holds most settings metrics; use them instead of new `@BoardMetrics` literals:
  - sizes: `rowHeight` 64, `rowDisc` 36, `rowVertical` 10, `pageBottom` 40, `sheetRadius` 38, `selectionWidth` 2, `pillHeight` 44, `pillGap` 6, `toastHeight` 52, `counterHeight` 60
  - `disabledOpacity` 0.45 and `skeletonRowWidths` [150, 190, 130]
  - elsewhere: `cardGap` 14 is `AuthTokens`; `space4`, `space6`, `radius2Xl` 22 and `radiusXl` 18 are `PlanUITokens`
  - every type style the brief names exists (`settingsStrong`, `settingsHint`, `settingsRow`, `screenBody`, `screenBodyLg`, `screenCardTitle`, `screenMeta`), and so does every colour (`surface2`, `popover`, `dawnForeground`, `mutedForeground`, `primaryForeground`, `muted`, `destructive`)
- **Two token conflicts, recorded as new OQs (§9).**
  - Loading rows: the `SetStatesMore` board draws bars 140/180/120 in 64 dp rows. iOS `SettingsLoadingRows` (`Kit/Settings/SettingsBoardComponents.swift:298–311`) uses `SettingsTokens.skeletonRowWidths` 150/190/130 in `counterHeight` 60 rows. → OQ-A481
  - Disabled row: the board's `opacity: 0.5` (= `AuthTokens.disabledOpacity`) differs from `SettingsTokens.disabledOpacity` 0.45. → OQ-A482
- **Missing iOS citation.** §2.3 omits `Kit/Settings/SettingsBoardComponents.swift` (`SettingsCard`, `SettingsRow`, `SettingsBadge`, `SettingsReadOnlyField`, `SettingsRadioGroup`, `SettingsLoadingRows`, `SettingsCompactSheet`). Read it with the others. Every `I/…/Settings*.swift` and `SignOutPendingSheet.swift` path in §2.3 is under `Kit/Settings/`; `AppTabs.swift` is `I/App/AppTabs.swift`.

**R7. Glyphs (Task 1).** `tools/icons/lucide.json` has 50 glyphs. These already exist, so skip them: `check`, `globe`, `loader-circle` and `moon` (bundle); `chevron-right` (board SetHome); **`cloud-upload`** (board PlanRemote) and **`cloud-off`** (board Wizard). Add only `cloud-check`, `user`, `shield`, `sliders-horizontal` and `credit-card`; each `first` string occurs once in `SetHome.dc.html`. `LucideGlyphsTest` (`core/designsystem/src/test/kotlin/net/chronoflow/core/designsystem/LucideGlyphsTest.kt:19`) goes from 50 to **55**. `make vectors` and `make vectors-check` exist.

**R7a. Repeated check (2026-11-21).** `tools/icons/test_generate_icons.py:test_manifest_names_all_generate` also asserts the manifest count (50). Task 1 must raise that Python assertion to 55 alongside `LucideGlyphsTest`; otherwise `make generators-check` fails after adding the five glyphs. This changes only the generator guard.

**R8. Strings.** Every name in §5.11 is in `tools/l10n/names.lock.json`, and so are `settings_password_passkeys_two_factor` and `auth_back`. No Android-only copy is expected. If one turns out to be needed, it goes into `tools/l10n/android-only.json` (`entries`, `{{name}}` placeholders, `replaces: []`) and into plan.md "Android-only copy awaiting approval". Composables read copy with `stringResource`/`pluralStringResource` or `LocalResources.current`; `Text` receives resources only.

**R9. Conventions that override the body.**
- **Screens.** Each screen is stateless and sits behind a thin `XRoute(viewModel)` wrapper:
  - `SettingsHomeScreen(state, onEvent)`
  - `AccountScreen(state, onEvent)`
  - `ProfileScreen(state, onEvent)`
  - `LanguageScreen(state, onEvent)`
  - the sheet content, `SignOutPendingContent(…)`
- **ViewModels.** Each takes `SettingsDependencies` (or `LanguageDependencies`) and a `main: CoroutineDispatcher` through its constructor, and passes `CoroutineScope(SupervisorJob() + main)` to `ViewModel(...)`, as `TermsViewModel` does. Factories read the CreationExtras key like `TripsViewModel.Factory`. A ported plain model class held inside the ViewModel (`SettingsHomeModel`, `SettingsProfileModel`) is fine; Terms does the same with `TermsUpdateModel`. The tests drive the ViewModel's UI state on the JVM (`runTest(timeout = 60.seconds)`, a fake clock, fake or JDBC repositories), never through Compose.
- **Test budgets.** Every test runs in < 1 s warm, JVM and Robolectric alike. Warm any one-off cold cost (the Robolectric SDK, the JDBC driver) in `@BeforeClass`, recorded through `measureClassWarmup(...)` (`core/testing-junit/src/main/kotlin/net/chronoflow/core/testing/junit/ClassWarmup.kt`). The tier annotations `@Logic`, `@Layout`, `@Capture` and `@Smoke` are in `net.chronoflow.core.testing.tiers` (`:core:testing`).
- **No copy assertions on screen:** no `onNodeWithText`, `hasText` or `assertTextEquals`, and no "reads Taal". The string generators and the captures cover copy. Every copy check in Tasks 2, 5, 6, 8, 9 and 13 becomes one of:
  - a JVM assertion on ViewModel or state values (`PendingText.CheckedOff(BlockType.AvoidLight)`, `SyncLine.Minutes(2)`, `missingAnswers == 3`, `credits == 2`, `pending.size == 2`, the account name and email behind the subtitle);
  - a tag or structure check (the node exists, is absent, or is enabled);
  - a configuration check (`resources.configuration.locales[0].language == "nl"`).
- **No app boots in unit tests.** Konsist `unitTestsKeepAppBootsAtApprovedBoundaries` (`konsist/src/test/kotlin/net/chronoflow/konsist/AppTestBoundaryRules.kt`) rejects unit tests that call `AppGraph(`, `SessionGraph(`, `SessionGraphs(`, `RootContent(`, `CfRoot(`, `MainActivity(` or `ActivityScenario.launch` outside its allow-list. 10a adds no allow-list entry. The language-applier and recreation tests host a plain `ComponentActivity` through Robolectric `buildActivity(...).setup()` with fake dependencies, as `ShellRecreationTest` hosts `ShellRecreationActivity`.
- **Emulator tests.** At most one emulator journey per feature; the other emulator tests seed a state and check one screen. All of them launch seeded: `ScreenTest.launch(overrides)` adds `CF_AUTH_TEST_ID` and `CF_UI_TEST_PROFILE=screen`.
- **Captures** go through `SettingsCaptureTest`: `BoardCapture.capture(compose, name)` under `ProfileRule(profile, language, dark)`. The pattern is `feature/trip/src/test/kotlin/net/chronoflow/feature/trip/actions/TripActionsCaptureTest.kt`.
- **Debug counters** follow `TripActionsDebugCounters`: an object in the `debug` source set and a same-named no-op in `release`, both in `:feature:settings`.
- **Invalidation probe.** There is no shared "0e recomposition counter". The Task 5 probe counts with a local `SideEffect` counter, as `feature/launch/src/test/kotlin/net/chronoflow/feature/launch/welcome/WelcomeRecompositionTest.kt` does.
- **Secrets.** 10a handles none. The draft name is not secret, so `SavedStateHandle` is allowed for it.

**R10. The language applier and the main thread.** The mechanism is already in place (0e and 4a, OQ-A24):
- `ConfigurationPolicy.overrideConfiguration` (`app/src/main/kotlin/net/chronoflow/app/ConfigurationPolicy.kt`) runs in `MainActivity.attachBaseContext` (`MainActivity.kt:44–47`). Below 33 it reads the ≤ 64 B `device-config` through `DeviceConfigFile.readForLaunch`, the only read before the first frame. From 33 on it trusts `LocaleManager`.
- `android:configChanges` (`AndroidManifest.xml:30`) does not list `locale`, so a locale change recreates the activity.
- The debug seed already applies `cf.settings.language` this way (`TestSeeds.kt`: `setLanguage`, then `LocaleManager.applicationLocales` on 33+).

The applier therefore:
- calls `devicePreferences.setLanguage(...)`, which already runs `withContext(io)`;
- reads and writes `LocaleManager.applicationLocales` and `systemLocales` from a coroutine off the main thread (binder IPC);
- never reads DataStore or `device-config` synchronously on main;
- below 33, calls `recreate()` on main after the write completes.

`reconcileDeviceConfig()` stays a `LaunchedEffect` call; it is `withContext(io)`. `ScreenTest.seededIntent` (`app/src/androidTest/kotlin/net/chronoflow/app/testing/ScreenTest.kt`) already seeds `cf.settings.language=system` and `cf.settings.theme=system` on every launch (`TestSeed.kt:161`), so the next seeded launch resets the language. Do not drive the UI back to System in `@After`.

**R11. plan.md.**
- **Lines.** "### Phase 10a" is at l.3027–3067; its five task boxes (Home, Sign out with pending changes, Shared states, Account/Profile/Language, Tests) are at l.3048–3062. The cited lines for "configuration and appearance" (348), "window policy" (357) and "state survival" (373) are current.
- **References.** R22 (l.67), OQ 29 (l.156), OQ-A5 (l.602), OQ-A24 (l.621), OQ-A25 (l.622) and OQ-A37 (l.634) exist. Also relevant: OQ-A296 (6d: Settings adds no refusal UI) and OQ-A338 (8: the Terms slot waits for 10a).
- **Shared states box.** The plan's "Shared states" task lists saved and failed messages and the refused change, which this brief scopes out (OQ-10a-7 and -8). Tick it with a pointer to those OQs.
- **OQ numbers.** The highest number in plan.md is OQ-A461 (9h), so the 10a OQs take OQ-A462 onward, in order: OQ-10a-1 → A462 … OQ-10a-19 → A480. The two new ones from this refresh are A481 and A482. Rows follow 9h's format: `| OQ-A462 | (10a OQ-10a-1) … | <default> | 10a |`.

---

Precedence when sources disagree: **iOS behaviour** first, **board looks** second, this brief's [R] rules third. Never invent a screen, row or copy line that has no board and no iOS string.

---

## 1. Goal

The Settings tab stops being a placeholder. A signed-in member sees **Settings home**: a profile card (initials, name, email), a sync line ("Synced 2 min ago", "3 changes waiting to sync", "Syncing…", "Saved on this phone · syncs when you're online"), the destination rows (Account, Security, Body clock with its "3 to answer" badge, Preferences, Plan & credits with "2 credits left") and an outline **Sign out** pill.

**Sign out** with an empty outbox signs out at once and lands on Sign in without replaying the launch. With pending changes it opens the **SignOutPending** sheet listing the waiting changes in plain words; **Stay signed in** keeps everything, **Sign out and discard** wipes them and signs out. If Phase 8 is already in the stack, the same sheet fills the Terms screen's `signOutConfirmation` slot; otherwise Phase 8 wires it when it lands (its brief §5.8: "If 10a's `SignOutPendingSheet` … is already in the stack, pass it from `:app`").

**Account** lists Profile and Language (Delete account is hidden until 10d). **Profile** edits the name (email read-only, OQ-5) and saves offline-first through the outbox. **Language** offers Phone language / English / Nederlands; the choice applies at once on API 29 and 36 and survives relaunch.

Done means:
- `SettingsHomeState`, the home model and the profile model are ported with their iOS tests on the JVM, test names kept;
- every 10a board is guarded on Robolectric (5 profiles × en/nl × light/dark) and captured;
- the language switch is proven on Robolectric at `sdk = [29, 36]` and once on the pixel emulator;
- one emulator journey runs sign-out pending → Stay → Sign out → Discard → Sign in.

---

## 2. Inputs to read (in this order)

### 2.1 This repo

1. `AGENTS.md` (all of it: "Working rules", "Tests", "Traps", "Commands").
2. `plan.md` sections in the header.
3. `docs/research/07-day-to-day-settings.md` §6.1–§6.3 (home, sync wording table, sign-out), §11 OQ-3 (no data row; Account subtitle "Profile, language"), OQ-5 (email change deferred; button "Save changes"), OQ-11 (560 column), OQ-18 (language is device-local; no endpoint), §14.
4. `docs/research/05-launch-auth.md` §7 (SignOutPending sheet; reached from Terms and Settings).
5. Earlier briefs and their merged code: `phase-4a-*` (`DevicePreferences`, `LanguageChoice`, `AccountRecord`, `SettingsRecord`, `OutboxRecord`, repositories), `phase-4d-*` (`OutboxPayload.change`, `OutboxChange`, its hand-off "the `sessionUpsertPreservesPendingProfileAcrossRelaunch` feature test belongs to 10a"), `phase-4e-*` (`SessionController`, `SignOutResult`, `SessionGraph`, `SessionGraphs`, `AppGraphKey`, `Route`, `DebugSeedRouting`), `phase-6a-shell-trips.md` (`ShellNav`, `ShellStart`, `ShellViewModel` *[Refresh: not `ShellHolder`]*, `RoutePlaceholder`, `TripsViewModel` factory pattern), `phase-1a-plan-core.md` l.334 (`stepLabelKey(type): String?`, resolved with 0c's `CopyIndex.find(CopyTable.Screens, key)` as 6d does) and l.354 (`PlanClock`), `phase-1d-timeline-host.md` (`SystemPlanClock`), `phase-6d-*` OQ-6d-5 only (10a adds no refusal UI), `phase-8-*` l.338 and l.369–370 (Terms slot, `GraphOverrides.mutationsOffline`, `CF_AUTH_TEST_TERMS_CHANGES_PENDING`; *[Refresh]* merged), `phase-2-*` (`CfButton`, `CfCard`, `CfRow`, `CfLargeTopAppBar`, `CfModalBottomSheet`, `CfSnackbarHost`, `CfTextField`, `CfSkeleton`), `phase-0c-strings.md` (slug rule, `tools/l10n/names.lock.json`), `phase-0e-test-harness.md` (`ConfigurationPolicy`, `ConfigurationRules.resolveLanguage/resolveLocale`, `Guards`, `ProfileRule`, `BoardCapture`, `ScreenTest`, seeds).
6. `tools/icons/lucide.json` (glyph manifest) and `tools/l10n/names.lock.json`.

### 2.2 Boards (`DC/`, read-only)

| Board | What it fixes |
|---|---|
| `SetHome` (375 × 812) | Home layout; profile card; sync line; rows; badges; Sign out pill |
| `SetHomeNL` | Dutch copy, badge **below** subtitle (`margin-top: 6px`) |
| `DarkSetHome` | Dark theme of SetHome |
| `SignOutPending` / `SignOutPendingSettings` (`from="settings"`) | Pending sheet |
| `SetAccount` | Account page |
| `SetProfile` | Profile page (Android builds iOS name-only, see §5.8) |
| `SetLanguage` | Language page and saved toast |
| `SetStates` | Four sync lines; offline-disabled row with "Needs a connection." |
| `SetStatesMore` | Loading skeleton; "Your account isn't on this phone yet" |
| `Set2FAWide`, `Set2FAManageWide` | Only the `max-width: 560px` wide column rule |

### 2.3 iOS (`f391a1fa`): behaviour to mirror

- `Kit/Settings/SettingsHomeState.swift` (whole file: initials, `apply(settings)`, `apply(outbox)`, `syncCopy`).
- `Kit/Settings/SettingsHomeModel.swift` (start, receive, stay, signOut, stop).
- `Kit/Settings/SettingsHomeView.swift`, `SettingsHomeRows.swift` (destinations, subtitles, tags, release visibility). *[Refresh: all `I/…/Settings*.swift` files in this list are under `Kit/Settings/`; also read `Kit/Settings/SettingsBoardComponents.swift`, R6.]*
- `I/…/SignOutPendingSheet.swift`.
- `I/…/SettingsPagesView.swift` (SettingsPage scaffold, Account, `SettingsStoredState`, Profile editor, `languageSubtitle`).
- `Kit/Settings/SettingsProfileModel.swift`, `SettingsPagesModel.swift` (`saveProfile`, `retry`).
- `I/…/SettingsPreferencesView.swift` l.168+ (`SettingsLanguageView`), `Kit/Settings/SettingsDevicePreferences.swift`.
- `I/App/AppTabs.swift:246–262` (`CF_SETTINGS_ROUTE` mapping).
- Tests: `SettingsHomeStateTests`, `SettingsHomeModelTests`, `SettingsProfileTests`, `SettingsPreferencesTests`, UI tests `SettingsTests`, `SettingsPagesTests`.
- Docs: `I/Docs/phase-17e-settings.md`, `I/Docs/phase-17h-settings-pages.md`, `I/Docs/settings-board-fidelity.md` (no failure UI on sign-out; title counts displayed changes; name-only profile; large titles in system font).
- Debug seed: `I/App/Debug/SettingsUITestSeed.swift` (whole file), `I/App/Session/AppServices.swift:63`.

### 2.4 API (web `e36368ec`; 10a calls nothing directly)

- `PATCH /v1/users/me` (`W/routes/api.php:67`, `auth:sanctum`, `throttle:api` + `throttle:api-credential`, no `verified`). Body `{name, email}`; `W/app/Concerns/ProfileValidationRules.php`: `name` required, string, max 255; `email` required, email, max 255, unique ignoring self. `current_password` required only when the email changes [F]. 10a enqueues `OutboxChange.Profile(ProfileChange(name, email = account.email))`; 4d's flusher sends it. Because the email is unchanged, no password is needed.
- `POST /v1/auth/logout` (l.52): 4e's 3 s best-effort call inside `signOut`.
- No endpoint sets the account language (`PatchPreferencesRequest` takes only `time_format`, `hidden_categories`, `show_optional_items`) [F]. Language is device-local.

---

## 3. Out of scope (owner)

- Security, Password, Two-factor, Passkeys pages → 10c. Body clock → 10b. Preferences, Appearance, Reminders → 10b/12b. Plan & credits → 10d. Delete account sheet → 10d.
- Email change ("Save and send link", `SetProfile` email flow) → deferred (OQ-5).
- "Your data & policies" row → omitted (OQ-3).
- Generic "Saved" and "Couldn't save. Check your connection." toasts on `SetStates` → no iOS use; not built (OQ-10a-7).
- Refused-change toast: shown by 6d's shell Snackbar host on every tab, Settings included (OQ-6d-5). 6d is merged (R1); 10a adds nothing and does not port iOS `SettingsHomeModel.refusedChanges`. The "Details" action on `SetStatesMore` is not built (iOS has none).
- Sign-out failure UI: none (iOS 17e). `failure` is kept for tests only.
- Hour-cycle override (`appLanguagePreservesThePhonesHourOverrideForPillsAndPickers`): not portable (OQ-10a-9).
- Notification channel re-localisation after a locale change → 12b.

---

## 4. Hand-ins to verify before Task 1 (no commit)

Run each check; if one fails, stop and report, except items 5, 6, 7 and 8, which have fallbacks.

*[Refresh: at c7818ff0 all eight pass. 1, 2, 3, 4, 7: R4/R5. 5: Phase 8 merged; the placeholder is `CfRoot.kt:220` with a different comment (R5). 6: passed with no test to write (`ShellViewModel`, `ShellRecreationTest`; R3). 8: `GraphOverrides.mutationsOffline` exists; feed it through `TestSeed.mutationsOffline` (R4).]*

1. `SessionController.signOut(expected: SessionSnapshot, discardPending: Boolean): SignOutResult` with `SignedOut` / `NeedsConfirmation(pendingCount)` exists (4e).
2. `SessionGraph` exposes `accounts`, `settings`, `outbox`, `sync: SessionSync` and `SessionSync.events()` is conflated and replays the latest event (4e/4d).
3. `DevicePreferences` has `language: Flow<LanguageChoice>`, `setLanguage(choice, sdkInt)` and `reconcileDeviceConfig()` (4a).
4. `DebugSeedRouting.current.settingsRoute` is parsed from `CF_SETTINGS_ROUTE` (4e).
5. **If Phase 8 is merged:** Terms has the `signOutConfirmation: @Composable (count: Int, onStay: () -> Unit, onDiscard: () -> Unit) -> Unit` slot, filled in `:app` with `{ _, _, _ -> }` and the comment `// 10a wires SignOutPending here`. Record "merged / not merged" in the PR. Not merged → skip every Terms-slot item in this brief (§5.5 last paragraph, Task 6 Terms test, Task 12 `SignOutPending` capture); Phase 8 wires it later.
6. **Shell retention on recreation.** *[Refresh: passed; there is no `ShellHolder`/`holder.clear()`. `ShellViewModel` and `ShellRecreationTest` cover it (R3). Skip this item.]* 6a's `ShellHolder` is cleared by a `DisposableEffect { onDispose { holder.clear() } }`. Write a throwaway Robolectric test: push `Route.Account`, `scenario.recreate()`, assert `route.account` is still shown. If it fails, Task 10 includes the fix (guard the clear with `!activity.isChangingConfigurations`).
7. 1a's `interface PlanClock { fun values(): Flow<Instant> }` and 1d's `SystemPlanClock` exist. `SettingsDependencies.clock` is `SystemPlanClock(…).values()` (use 1d's constructor as merged). Phase 7's `MinuteClock` is **not** used (Phase 7 is wave 12).
8. 4e's `GraphOverrides` has `mutationsOffline: Boolean` (Phase 8 adds it, phase-8 brief l.369). If it is missing, add `val mutationsOffline: Boolean = false` to `GraphOverrides` and pass it where 4e's `MockSources` builds `MockDataSource(mutationsOffline = …)`, exactly as the Phase 8 brief describes, in Task 11; note it in the PR so Phase 8 reuses it on rebase.

---

## 5. Spec

### 5.1 Pure state: `SettingsHomeState` (`…/fs/home/SettingsHomeState.kt`) [F `Kit/Settings/SettingsHomeState.swift`]

```kotlin
data class SettingsHomeState(
    val account: AccountRecord? = null,
    val missingAnswers: Int = 0,
    val credits: Int? = null,
    val pendingCount: Int = 0,
    val pending: List<PendingChange> = emptyList(),
    val outboxLoaded: Boolean = false,
)   // no separate syncedAt: iOS reads `account?.syncedAt` (SettingsHomeState.swift:118)
data class PendingChange(val id: String, val category: BlockType?, val text: PendingText) // [Refresh] sleep = BlockType.Sleep
sealed interface PendingText { data object Generic; data class CheckedOff(val type: BlockType); data object Sleep }
sealed interface SyncLine { Syncing; Pending(n); Offline; JustNow; Minutes(n); Hours(n); Yesterday; Days(n); On(raw: String) }
```

- **initials(name):** split on whitespace (drop empties). Take the first grapheme (`BreakIterator.getCharacterInstance`) of the first word, plus the first grapheme of the last word if ≥ 2 words; `uppercase()`. "  Rick van Gout  " → "RG"; "éva" → "É"; "🧑🏽‍💻 de Vries" → "🧑🏽‍💻V"; blank → "".
- **apply(settings: List<SettingsRecord>):** section `readiness` JSON: if `variant == "needs-answers"`, `missingAnswers = missing.size + invalidSections.size`, else 0. Section `planCredits`: `unlimited == true` → `credits = null`; else `credits` a finite number ≥ 0 → `toInt()`; else null. Malformed JSON → those fields reset (0 / null), never throw.
- **apply(outbox: List<OutboxRecord>):** `pendingCount = outbox.size`; `outboxLoaded = true`. Per row via `OutboxPayload.change(record)`:
  - default: identity `row.id`, text `Generic`, category null, replace false.
  - `OutboxPayload.change(record)` returns `CheckOff(c)` (decoding failures fall through to the next rule, never throw; *[Refresh]* it throws `LocalMutationFailure`, catch that type): identity `"check:${c.tripID}:${c.localDate}:${c.type.raw}:${c.ordinal}"` where `raw` is the API raw value (`avoid_light`, as Swift `rawValue`), no `planInputHash`, so the latest check across hashes wins; category `c.type`, replace = true; text `CheckedOff(c.type)` only when `c.status == done` **and** 1a's `stepLabelKey(c.type) != null`, else `Generic`.
  - Else, when `record.kind == "settings"` and `record.payload` parses as a JSON object with a string `section` (read the raw JSON, not `OutboxPayload.change`; Swift l.88–90): `subsection = payload["bodyClockSection"]` (string or null); identity `"settings:$section:${subsection ?: ""}"`. When `section == "bodyClock"` and (`subsection == "sleep"` or `payload["patch"]` is a non-empty object whose keys are a subset of `{habitual_bed, habitual_wake}`): category `sleep`, text `Sleep`.
  - dedupe in order: a repeated identity replaces the earlier entry (in its position) only when replace is true; otherwise the first stays.
  - Note: the sheet title uses `pending.size` (displayed count), the home sync line uses `pendingCount` (rows).
- **syncLine(now: Instant, syncing, offline, zone): SyncLine?**, first match wins:
  1. syncing → `Syncing`; 2. `pendingCount > 0` → `Pending(pendingCount)`; 3. offline → `Offline`;
  4. parse `account?.syncedAt` as ISO-8601 with or without fractional seconds (`Instant.parse`, fallback `OffsetDateTime.parse`); invalid or null → null;
  5. `minutes = max(0, floor((now - t).seconds / 60))`: < 1 → `JustNow`; < 60 → `Minutes`; < 1440 → `Hours(minutes / 60)`;
  6. else `days = max(1, daysBetween(t.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate()))`: 1 → `Yesterday`; < 7 → `Days`; else `On(syncedAt)`.
- `On(raw)` is formatted at render time with 1a's `CopyLineFormatter.formatDayMonth(raw, language, zone.id)` ("24 Oct" / "24 okt"). *[Refresh: an instance method on `CopyLineFormatter(catalog)`; zone is an id string; R5.]*

### 5.2 Home model (`…/fs/home/SettingsHomeModel.kt`, `SettingsHomeViewModel.kt`) [F `SettingsHomeModel.swift`]

Plain class with an injected `CoroutineScope` (testable on the JVM), wrapped by an `androidx.lifecycle.ViewModel` that passes `viewModelScope`. *[Refresh R9: `SettingsHomeViewModel(deps, main)` takes its dependencies and `main` dispatcher through the constructor; tests assert the ViewModel's `ui` state on the JVM; the screen is `SettingsHomeScreen(state, onEvent)`.]* Exposes `ui: StateFlow<SettingsHomeUi>` with `state`, `syncing`, `offline`, `busy`, `showsPending`, `confirmationCount`, plus `failure` (internal; tests only).

- `start()`: once per session generation. Collect `accounts.values()` (first or null), `settings.values()`, `outbox.values()` (→ `receive`), `sync.events()` (`Started` matching `accountID`/`epoch` → `syncing = true`; `Finished` → `syncing = false`, `offline = result.state == Offline`), and `controller.updates`: when the epoch or presence no longer matches the captured snapshot → `stop()`.
- `receive(outbox)`: `state.apply(outbox)`; `confirmationCount = state.pending.size`; if outbox empty and not busy → `showsPending = false`.
- `stay()`: if busy return; `showsPending = false`. Nothing is deleted.
- `signOut(discardPending: Boolean)`: guard `active && !busy && (!discardPending || showsPending)`. Set busy, capture generation, `try/finally { busy = false; if (outboxLoaded && pendingCount == 0) showsPending = false }`. Check `controller.activeSession() == captured`, else return. Call `controller.signOut(expected = captured, discardPending)`:
  - `SignedOut` → `stop()` (4e routes the app to Sign in; this screen does nothing more).
  - `NeedsConfirmation(count)`: re-check session; `confirmationCount = count`; `receive(outbox.pending())`; re-check; `showsPending = confirmationCount > 0`.
  - `CancellationException` rethrown; any other `Throwable` → `failure = it`, no UI.
- `stop()`: clear state, `generation++`, cancel child jobs, `active = false`.
- A replacement session (new epoch) stops the old model; its pending Discard can never act (`replacementSessionRevokesOldDiscardAndClearsValues`).
- Clock: the model does **not** hold `now` (iOS does; [R] Android moves it to the leaf so a minute tick recomposes one line). The UI collects `SettingsDependencies.clock.values()` (`AppGraph.planClock`, 1d's `SystemPlanClock`) inside the sync-line leaf only.

### 5.3 Dependencies seam [R]

`…/fs/SettingsDependencies.kt`:

```kotlin
interface SettingsDependencies {
    val snapshot: SessionSnapshot
    fun account(): Flow<AccountRecord?>
    fun settings(): Flow<List<SettingsRecord>>
    fun outbox(): Flow<List<OutboxRecord>>
    suspend fun pendingOutbox(): List<OutboxRecord>
    fun syncEvents(): Flow<SyncEvent>
    fun sessionUpdates(): Flow<SessionUpdate>   // 4e's type, keep its name
    suspend fun isCurrent(): Boolean
    suspend fun signOut(discardPending: Boolean): SignOutResult
    suspend fun enqueueProfile(name: String, email: String) // throws when the permit is revoked
    suspend fun requestSync()                  // [Refresh] SessionSync.requestSync() is suspend
    val clock: PlanClock                       // [Refresh] as TripsDependencies; AppGraph.planClock
    suspend fun formatter(): CopyLineFormatter // [Refresh] for SyncLine.On, as TripsDependencies
    val zone: ZoneId
}
object SettingsDependenciesKey : CreationExtras.Key<SettingsDependencies>
```

`:app` implements it as `SessionSettingsDependencies(appGraph, sessionGraph)` (`…/app/settings/`), passing `controller.mutationPermit(snapshot)` to `outbox.enqueue` (*[Refresh]* `appGraph.controller`; pattern `app/…/shell/SessionTripsDependencies.kt`). ViewModels use `viewModelFactory { initializer { X(this[SettingsDependenciesKey]!!) } }` like 6a's `TripsViewModel`. Language uses a separate `LanguageDependencies` (§5.9) because it is device-level and works without a session.

### 5.4 Home layout (`…/fs/home/SettingsHomeScreen.kt`, `SettingsHomeMetrics` `@BoardMetrics("SetHome")`)

Scaffold: `SettingsPage(title = R.string.settings_settings)` (§5.7). Content column gap `space6` 24; board title margin-bottom −8 is **not** reproduced (M3 large app bar spacing). Root tag `settings.screen`.

1. **Profile card** (only when `account != null`), tag `settings.profile`, pushes `Route.Profile`: `CfCard` look (radius 22 `radius2Xl`, 1 dp `border`, `card` fill, `softShadow`), min height 72, padding 12 vertical / 16 horizontal, gap 12. Initials disc 44 dp, `primary` fill, `primaryForeground` text 16 sp / 600. Name `settingsStrong` 16/24; email `screenBody` 14/20 `mutedForeground`, `maxLines = 1`, ellipsis. Trailing `chevron-right` 20 dp `mutedForeground`. Content description: name, email.
2. **Sync line**, tag `settings.sync` (a leaf composable `SettingsSyncLine(state, syncing, offline, clock)`): row with padding horizontal 4, gap 6; glyph 14 dp; text `settingsHint` 13/18 `mutedForeground`, tabular numbers (`fontFeatureSettings = "tnum"`). Glyph: Syncing → `loader-circle` (static); Pending → `cloud-upload`; Offline → `cloud-off`; else `cloud-check`. Hidden when `syncLine` is null. Collects `clock` (`collectAsStateWithLifecycle`) inside this leaf only. iOS shows `checkmark.icloud` while syncing; the board's `loader-circle` wins (looks).
3. **Rows card**, `CfCard(rows = true)`, rows separated by `CfDivider`. Row component `SettingsDestinationRow` (feature row because the badge sits **below** the subtitle, not trailing as in `CfRow`): min height 64 (`SettingsTokens.rowHeight`), padding 10 (`SettingsTokens.rowVertical`)/16, gap 12, 36 dp (`SettingsTokens.rowDisc`) `muted` disc with a 16 dp glyph `foreground`, title `settingsRow` 16/24 500, subtitle `screenBody` 14/20 `mutedForeground`, optional `CfBadge(variant = Dawn)` 6 dp under the subtitle (iOS `SettingsHomeRows.swift` VStack and `SetHomeNL`; the English `SetHome` board draws it trailing, OQ-10a-18), trailing `chevron-right` 20 dp. Disabled alpha 0.5 (*[Refresh]* `AuthTokens.disabledOpacity`; OQ-A482).

| Destination (`SettingsDestination`) | Glyph | Title | Subtitle | Badge | Route | Tag | `isBuilt` |
|---|---|---|---|---|---|---|---|
| Account | `user` | `settings_account` | `settings_profile_language` | – | `Route.Account` | `settings.account` | true |
| Security | `shield` | `settings_security` | `settings_password_two_factor` | – | `Route.Security` | `settings.security` | false |
| BodyClock | `moon` | `settings_body_clock` | `settings_the_answers_every_plan_is_built_from` | `settings_n_to_answer` when `missingAnswers > 0`, tag `settings.readiness` | `Route.BodyClock` | `settings.bodyClock` | false |
| Preferences | `sliders-horizontal` | `settings_preferences` | `settings_appearance_timeline_reminders` | – | `Route.Preferences` | `settings.preferences` | false |
| Plan | `credit-card` | `settings_plan_credits` | `settings_n_credits_left` when `credits != null`, else none | – | `Route.Plan` | `settings.plan` | false |

   Release shows only rows with `isBuilt`; a debug build (the 6a label hook `LaunchPlatform.routeLabel` non-null) shows all and unbuilt routes render `RoutePlaceholder`. 10b–10d flip their flags.
4. **Sign out**: `CfButton(text = settings_sign_out, style = Outline, size = Lg /* [Refresh] 40 dp; no Lg40 */, fillWidth = true, loadingDescription = <label>)`, tag `settings.signOut`, `enabled = !busy && account != null`, `loading = busy`. Tapping calls `signOut(discardPending = false)`.

Bottom padding is `pageBottom` 40 (board's 120 assumes an overlaid iOS tab bar; OQ-10a-5).

### 5.5 SignOutPending sheet (`…/fs/signout/SignOutPendingSheet.kt`, `SignOutPendingMetrics` `@BoardMetrics("SignOutPending")`)

Stateless: `SignOutPendingContent(items: List<PendingChange>, busy, onStay, onDiscard)` inside `CfModalBottomSheet` (radius 38 top, `card`, M3 drag handle, M3 scrim). Root tag `settings.pending`. Padding horizontal 20, top 12 (below the handle), bottom 32; column gap `cardGap` 14.

- Title `settings_sign_out_and_lose_n_changes` (plural, count = `items.size`), `screenCardTitle` (Fraunces 24/32), tag `settings.pending.title`, heading semantics.
- Body `settings_these_haven_t_reached_your_accou_c0e36b`, `screenBodyLg` 16/24 `mutedForeground`.
- Items box, tag `settings.pending.items`: `surface2` fill, radius 18 (`radiusXl`), padding 12 vertical / 16 horizontal, gap 8. Each row: `CategorySwatch(type, Small)` (or nothing when category null), gap 10, text `screenBody` (OQ-10a-2). Text: `Generic` → `settings_a_change_waiting_to_sync`; `CheckedOff(t)` → `settings_checked_off_step` with `step` = 1a's `stepLabelKey(t)` resolved through `CopyIndex.find(CopyTable.Screens, key)` (the iOS `PlanStrings.text(step)` call); `Sleep` → `settings_changed_your_usual_sleep_times`.
- Buttons, gap 6: `CfButton(settings_stay_signed_in, Primary, minHeight = 48, fillWidth)`, tag `settings.pending.stay`; `CfButton(settings_sign_out_and_discard, GhostDestructive, minHeight = 48, fillWidth)`, tag `settings.pending.discard`. Both disabled while busy; Discard shows `loading`.
- Footer `settings_changes_sync_by_themselves_when_1009db`, `settingsHint` 13/18 (board; iOS uses `screenMeta`, OQ-10a-17), centred, `mutedForeground`.
- Dismiss (swipe, scrim, back) = `onStay`, ignored while busy (`sheetState` `confirmValueChange = { !busy }`, `BackHandler(enabled = busy) {}`).
- Survives rotation and locale recreation (state lives in the ViewModel); gone after process death [F plan "state survival"].

Terms slot (in `:app`, **required: Phase 8 is merged** *[Refresh]*; replaces the `{ _, _, _ -> }` placeholder at `CfRoot.kt:220`; the slot has no busy flag, pass `busy = false`): `signOutConfirmation = { count, onStay, onDiscard -> PendingSheetFromOutbox(count, onStay, onDiscard) }` collects the session outbox, maps it through `SettingsHomeState.apply`, shows the same content and calls `onStay` when the list becomes empty.

### 5.6 Shared states (`…/fs/components/`)

- `SettingsStoredState(loaded: Boolean, failed: Boolean, onRetry)`: not loaded and not failed → `SettingsLoadingRows` (card with three skeleton rows: 36 disc + bars 140/180/120 wide from the board, row 64 *[Refresh: iOS uses `SettingsTokens.skeletonRowWidths` 150/190/130 and `counterHeight` 60; OQ-A481]*, content description `settings_loading`); else title `settings_your_account_isn_t_on_this_phone_yet` (`screenCardTitle`), body `settings_we_need_a_connection_once_to_loa_a9f557` (`screenBodyLg` muted), `CfButton(settings_try_again, Outline, minHeight = 48)`, tag `settings.stored.retry`. Root tags `settings.loading` / `settings.missing`.
- `SettingsOfflineHint(text)`: `cloud-off` 14 dp + `settingsHint` muted (used by Profile; 10c/10d reuse it). The board's "Needs a connection." under a disabled row is built here as `SettingsNeedsConnection()` (`settings_needs_a_connection`, `screenBody` 14/20 muted, 12 dp under the card: the board's section gap) for 10c; 10a only guards it in a `SetStates` capture.

### 5.7 Page scaffold and wide windows (`…/fs/components/SettingsPage.kt`)

`SettingsPage(title: Int, onBack: (() -> Unit)?, testTag, content: LazyListScope / ColumnScope)`: `Scaffold` with `CfLargeTopAppBar` (`exitUntilCollapsedScrollBehavior`, nav icon `CfBackButton` with Auth "Back" when `onBack != null`), a vertically scrolling column with padding horizontal 16 (`space4`), top 4, bottom 40, gap 24. When `isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)` (600) the column is `widthIn(max = SettingsWideMetrics.column /* 560 */)` and centred; the app bar stays full width (OQ-10a-11). `SettingsWideMetrics` is `@BoardMetrics("Set2FAWide")`. *[Refresh: `isWidthAtLeastBreakpoint` is the androidx.window `WindowSizeClass` method, used as `currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(...)` in `AppShellScaffold.kt:75`.]*

### 5.8 Account and Profile

- **Account** (`…/fs/account/AccountScreen.kt`, tag `settings.account.screen`, title `settings_account`, back): when the account is null → `SettingsStoredState` [F `SettingsPagesModel.swift:44–94`]: `loaded` = the account flow has emitted at least once (even null); `failed` = the account or settings flow threw; retry = cancel and restart both collections with `failed = false`, then `requestSync()`. So: not loaded and not failed → loading rows; loaded with a null account, or failed → "isn't on this phone yet". Else `CfCard(rows)`: Profile row (`user`, title `settings_profile`, subtitle "name · email" one line ellipsis, tag `settings.account.profile`, → `Route.Profile`), `CfDivider`, Language row (`globe`, title `settings_language`, subtitle `languageSubtitle`, tag `settings.account.language`, → `Route.Language`). Delete account card hidden until 10d (OQ-10a-13).
- `languageSubtitle(choice, phone)`: `System` → `settings_phone_language_language` with "English" or "Nederlands" from the phone language (nl → Nederlands, else English); `En` → `settings_english`; `Nl` → `settings_nederlands`.
- **Profile** (`…/fs/profile/ProfileScreen.kt`, `SettingsProfileModel`, tag `settings.profile.screen`, title `settings_profile`): `CfTextField` Name (`settings_name`, tag `settings.profile.name`, `KeyboardType.Text`, `ContentType.PersonFullName`, IME Done → submit); read-only Email field `SettingsReadOnlyField(label = settings_email, value)` (tag `settings.profile.email`, not focusable for editing, still readable by TalkBack); `CfButton(settings_save_changes, Primary, minHeight = 48, fillWidth, loading = busy)`, tag `settings.profile.save`; when `saved`, `SettingsOfflineHint(settings_saved_on_this_phone_syncs_when_y_4fb8b8)` tag `settings.profile.saved`. Error under the field via `CfTextField(error = …)`.
- `SettingsProfileModel` [F `SettingsProfileModel.swift`, whole file]: `receive(account)`, in this order: if `account.name != storedName` → `saved = false`; if the draft `name == storedName` (not edited) → `name = account.name` (an edited draft is kept); then `storedName = account.name`, `email = account.email`. `submit()`: if busy return; `value = name.trim()`; empty or longer than 255 → `error = settings_enter_your_name`, `saved = false`, return. Else `busy = true`, `saved = false`; `try { deps.enqueueProfile(value, email); name = value; storedName = value; error = null; saved = true } finally { busy = false }`. `CancellationException` → silent (no error, rethrow after `busy = false`); any other exception → `error = settings_we_couldn_t_save_your_change_ple_354da2`, draft kept. `enqueueProfile` [F `SettingsPagesModel.swift:96–105`] throws `CancellationException` unless the model is active and `isCurrent()` is true, then enqueues `OutboxChange.Profile(ProfileChange(name, email = account.email))` with the permit and calls `requestSync()`; the account missing → `LocalMutationFailure.MissingAccount`. Length [R]: count code points (`value.codePointCount(0, value.length)`), which is what Laravel `max:255` measures (`mb_strlen`); iOS counts graphemes, which is never more (OQ-10a-19).
- The draft name survives rotation through `SavedStateHandle` (non-secret).

### 5.9 Language (`…/fs/language/LanguageScreen.kt`, `LanguageSettingsViewModel`, `SettingsLanguageMetrics` `@BoardMetrics("SetLanguage")`; applier in `…/app/settings/AppLanguageApplier.kt`)

- Page title `settings_language`, tag `settings.language.screen`. Intro `settings_chronoflow_follows_your_phone_s_ca480e`, `screenBodyLg` muted. Then `SettingsRadioCards` (gap 8, `selectableGroup()`) of three `SettingsChoiceCard`s: padding 16, radius 22, 2 dp border (`dawn` selected, else `border`), title `settingsStrong`, hint `screenBody` muted 2 dp below, gap 12, trailing 20 dp circle with 2 dp border (selected: `dawn` fill + 12 dp `check` in `dawnForeground`; unselected: `mutedForeground` at alpha 0.5). `Modifier.selectable(role = Role.RadioButton)`.

| id | Title | Hint | Tag |
|---|---|---|---|
| system | `settings_phone_language` | `settings_now_language_changes_with_your_phone` (phone language name) | `settings.language.choice.system` |
| en | `settings_english` | `settings_always_english` | `settings.language.choice.en` |
| nl | `settings_nederlands` | `settings_always_dutch` | `settings.language.choice.nl` |

- `LanguageDependencies` (in `:feature:settings`, implemented in `:app`): `current: Flow<LanguageChoice>`, `phoneLanguage(): String` (`"nl"`/`"en"` after `resolveLanguage`), `suspend fun apply(choice): Boolean /* true = caller must recreate */`, `feedback: MutableStateFlow<LanguageChoice?>`.
- `AppLanguageApplier` [R]:
  - **API 33+:** `devicePreferences.setLanguage(choice, sdk)` then `LocaleManager.applicationLocales = if System LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(raw)`; the system recreates the activity; returns false. `current` on 33+ reads `applicationLocales` (empty → `System`) and reconciles DataStore when they differ.
  - **API < 33:** `devicePreferences.setLanguage(choice, sdk)` (DataStore + `device-config`), returns true; the screen calls `LocalActivity.current?.recreate()`. `ConfigurationPolicy` applies the locale in `attachBaseContext`.
  - Phone language: 33+ `LocaleManager.systemLocales[0]`, else `Resources.getSystem().configuration.locales[0]`; unsupported → English.
  - `reconcileDeviceConfig()` is called once when the Settings root first composes (`LaunchedEffect(Unit)` in `:app`). It has no caller today.
  - *[Refresh R10]* No disk or binder work on the main thread: `setLanguage` is already `withContext(io)`; read and set `LocaleManager` locales off main; `recreate()` on main after the write.
- Selecting the current choice does nothing (no write, no toast, no recreate).
- Saved feedback: before applying, set `feedback.value = choice` (process-level `MutableStateFlow` in AppGraph, survives recreation). After recreation the Language page consumes it once (`getAndSet(null)`) and shows a `CfSnackbarHost` Short snackbar: System → `settings_saved_chronoflow_follows_your_phone`, En → `settings_saved_the_app_is_in_english`, Nl → `settings_saved_the_app_is_in_dutch`. Tag `settings.language.saved`; debug presented counter `SettingsDebugCounters.languageSaved` (debug/release pair like `TripActionsDebugCounters`, fed by `CfSnackbarHost(onPresented = …)`). Board look (popover fill, radius 22, 52 min height, dawn check 24) is honoured through `CfSnackbarHost` tokens (OQ-10a-10).
- Navigation after a change: stay on Language (iOS test name "…ReturnsToAccount" checks Back reaches Account with the new subtitle; the back stack survives recreation: `ShellViewModel`, R3).
- OQ-A25: with `System`, a later phone-language change recreates the activity and `ConfigurationPolicy` / empty `applicationLocales` follow it. No code beyond not storing a fixed locale.

### 5.10 Navigation and seeds

- `AppShell` entries: `Route.Settings` → `SettingsHomeScreen`; `Route.Account`, `Route.Profile`, `Route.Language` → their screens; other settings routes stay `RoutePlaceholder`.
- `ShellStart.Settings(path: List<Route>)` (pure; add the case to `ShellStart` in `core/navigation/…/shell/ShellNavState.kt` and the branch to `ShellNav.seed`): selects the Settings tab and pushes `path` in order. *[Refresh]* Wire it in `DebugShellStart.take()` from `TestSeed.settingsRoute` (R4).
- `SettingsSeedRoute.path(raw: String?): List<Route>?` (`…/app/settings/`) [F `AppTabs.swift`]: `profile` → [Account, Profile]; `language` → [Account, Language]; `delete` → [Account] (sheet is 10d); `password` → [Security, Password]; `twoFactor` → [Security, TwoFactorSettings]; `twoFactorSetup` → [Security, TwoFactorSettings] (10c adds the setup state); any other raw value that names a `SettingsDestination` rawValue (`account`, `security`, `bodyClock`, `preferences`, `plan`, `reminders`; iOS `SettingsDestination(rawValue:)`) → [that route] (`Route.Account`, `Security`, `BodyClock`, `Preferences`, `Plan`, `Reminders`); unknown → null (Settings root only). Like iOS, it applies only in an auth-test launch (4e's `DebugSeedRouting.current` is set only then). The parser also accepts `twoFactorSettings` and `passkeys`; both map to null.
- `CF_SETTINGS_TEST_PENDING=1` (debug only; ignored unless `CF_AUTH_TEST_ID` is set). *[Refresh R4: a `settingsPending` flag on `TestSeed`, OR-ed into `TestSeed.mutationsOffline` (TestSeed.kt:54), which already feeds `GraphOverrides.mutationsOffline`; the row is added by `seedSettingsPending(database, permit)` in `SeedRunner.kt`'s `onSessionReady` hook after `MockSessionSeed`'s FullPull, next to `seedTermsChangesPending`. The iOS wait loop below is not ported; the account, trip and block rules and the error messages are.]* Port of `SettingsUITestSeed.swift`: after the signed-in seed, return silently unless the stored account email is `demo@example.com`; loop up to 100 times: return silently if `controller.activeSession() != snapshot`; `refreshNow()`; break when the result state is `Idle`; else `delay(50)` (`delay` is fine in the seed; tests never sleep). Not ready → throw `IllegalStateException("settings pending seed failed: initial sync unavailable")`. Then, for each trip not deleted (`deletedAt == null && !deletedLocally`) with a `currentInputHash` and a cached plan for that hash, take the first block with `actionable && checkKey != null`, enqueue `CheckOff(tripID, planInputHash = hash, type, localDate, ordinal, status = done)` with the permit, and require the row in `outbox.pending()` (else throw `"…: missing pending change"`); return. No such block in any trip → throw `"…: no cached actionable step"`.

### 5.11 Copy (resources only; verify every name in `tools/l10n/names.lock.json`)

All `settings_*` names in the summary table of the research (§6) exist in the generated catalog: `settings_settings`, `settings_account`, `settings_security`, `settings_body_clock`, `settings_preferences`, `settings_plan_credits`, `settings_profile`, `settings_profile_language`, `settings_password_two_factor`, `settings_the_answers_every_plan_is_built_from`, `settings_appearance_timeline_reminders`, plurals `settings_n_credits_left`, `settings_n_to_answer`, `settings_n_changes_waiting_to_sync`, `settings_synced_n_min_ago`, `settings_synced_n_hours_ago`, `settings_synced_n_days_ago`, `settings_sign_out_and_lose_n_changes`; strings `settings_sign_out`, `settings_syncing`, `settings_saved_on_this_phone_syncs_when_y_4fb8b8`, `settings_synced_just_now`, `settings_synced_yesterday`, `settings_synced_on_date`, `settings_these_haven_t_reached_your_accou_c0e36b`, `settings_stay_signed_in`, `settings_sign_out_and_discard`, `settings_changes_sync_by_themselves_when_1009db`, `settings_a_change_waiting_to_sync`, `settings_checked_off_step`, `settings_changed_your_usual_sleep_times`, `settings_loading`, `settings_your_account_isn_t_on_this_phone_yet`, `settings_we_need_a_connection_once_to_loa_a9f557`, `settings_try_again`, `settings_language`, `settings_phone_language_language`, `settings_nederlands`, `settings_english`, `settings_name`, `settings_email`, `settings_save_changes`, `settings_enter_your_name`, `settings_we_couldn_t_save_your_change_ple_354da2`, `settings_chronoflow_follows_your_phone_s_ca480e`, `settings_phone_language`, `settings_now_language_changes_with_your_phone`, `settings_always_english`, `settings_always_dutch`, `settings_saved_chronoflow_follows_your_phone`, `settings_saved_the_app_is_in_english`, `settings_saved_the_app_is_in_dutch`, `settings_needs_a_connection`, `settings_delete_account` (unused until 10d). "Back" comes from the Auth table. If a name differs in the lock file, use the lock file's name. Never add copy; if a line is missing, stop and report.

Key Dutch checks: "Instellingen", "2 min geleden gesynchroniseerd", "Nog 3 vragen te beantwoorden", "Nog 2 tegoeden over", "Uitloggen en 2 wijzigingen kwijtraken?", `"Vermijd fel licht" afgevinkt`, "Je gebruikelijke slaaptijden aangepast", "Gesynchroniseerd op 24 okt".

---

## 6. Tasks (one commit each, in order; Conventional Commits, lower-case imperative, **no trailer**)

### Task 1: `feat(designsystem): add settings glyphs and a ghost destructive button`
- Add these board entries under `"glyphs"` in `tools/icons/lucide.json`, in Phase 2's manifest form (Phase 2 brief Task 10; verified against the boards at the canvas pin). Skip any name an earlier phase already added; `check`, `globe`, `loader-circle`, `moon` and `chevron-right` already exist (Phase 2), and *[Refresh]* so do `cloud-upload` and `cloud-off` (boards PlanRemote and Wizard): skip the two entries below.
  - `"cloud-check": {"source": "board", "board": "SetHome", "first": "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"}` (2 children; the first SetHome svg with this first path is the sync line)
  - `"cloud-upload": {"source": "board", "board": "SetStates", "first": "M12 13v8"}` (3 children)
  - `"cloud-off": {"source": "board", "board": "SetStates", "first": "m2 2 20 20"}` (3 children)
  - `"user": {"source": "board", "board": "SetHome", "first": "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"}` (2 children)
  - `"shield": {"source": "board", "board": "SetHome", "first": "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"}`
  - `"sliders-horizontal": {"source": "board", "board": "SetHome", "first": "<line x1=\"21\" x2=\"14\" y1=\"4\" y2=\"4\">"}` (9 children)
  - `"credit-card": {"source": "board", "board": "SetHome", "first": "<rect width=\"20\" height=\"14\" x=\"2\" y=\"5\" rx=\"2\">"}` (2 children)
- Run `make vectors`, commit the regenerated `LucideGlyphs.generated.kt`, and raise the glyph count in `LucideGlyphsTest` and `tools/icons/test_generate_icons.py:test_manifest_names_all_generate` by the number added (read the merged assertions first; *[Refresh]* 50 → 55; R7a). `make generators-check` passes.
- `…/ds/component/CfButton.kt` *[Refresh; there is no `button/CfButtonStyle.kt`]*: add `GhostDestructive` to `CfButtonStyle` (transparent container, `destructive` content, pressed overlay as `Ghost`). Update the colour mapping and its guard test.
- Tests: extend `core/designsystem/src/test/kotlin/net/chronoflow/core/designsystem/CfButtonTest.kt` with GhostDestructive in light and dark; `designSystemPrimitivesHaveGuards` stays green.

### Task 2: `feat(settings): port the settings home state`
- `…/fs/home/SettingsHomeState.kt`, `PendingChange`, `SyncLine` (§5.1).
- `…/fst/home/SettingsHomeStateTests.kt` (JVM), names kept: `syncedLineAgeAndPriorityMatchRN` (now = `Instant.ofEpochSecond(1_794_132_000)`, zone `ZoneOffset.UTC` (Swift `.gmt`); account name `"  Rick van Gout  "`, `syncedAt` = `now − age` as ISO-8601; ages 0, −60, 59 → JustNow; 60 → Minutes(1); 3599 → Minutes(59); 3600 → Hours(1); 86399 → Hours(23); 86400 → Yesterday; 172800 → Days(2); 604800 → On; offline → Offline; pending 2 + offline → Pending(2); syncing beats both; invalid date → null; empty state → null), `initialsReadinessAndCreditsFollowLocalValues` (cases in §5.1; readiness missing 2 + invalid 1 → 3; credits 2; unlimited → null with missing 0; credits 0 → 0; empty → null), `pendingChangesGroupSettingsAndUseLatestCheckAcrossHashes` (rows: check 1 done hash old, settings 2 bodyClock `{"habitual_bed":"23:00"}`, settings 3 bodyClock, check 4 done hash new, settings 5 preferences → `pendingCount` 5, ids [4, 2, 5]; item 0 `PendingText.CheckedOff(BlockType.AvoidLight)` *[Refresh: assert the value only; no en/nl copy assertion, R9]*; item 1 category sleep; item 2 `Generic`; check 1 done + check 2 status null → one item, `Generic`). Android extras in `SettingsHomeStateEdgeTests`: fractional-second `syncedAt`; malformed readiness JSON; negative credits → null; dedupe keeps position; a check-off whose `stepLabelKey` is null → `Generic` (`BlockType.Unknown("x")`); `bodyClockSection = "sleep"` with a non-sleep patch → `Sleep`.

### Task 3: `feat(settings): add the home model and sign-out flow`
- `SettingsDependencies` + key (§5.3), `SettingsHomeModel`, `SettingsHomeViewModel` (§5.2). *[Refresh: the tests assert the ViewModel's UI state; JVM session fixture as `feature/onboarding/…/support/OnboardingFixture.kt`; warm JDBC in `@BeforeClass` with `measureClassWarmup`.]*
- `…/fst/home/SettingsHomeModelTests.kt` (JVM, real 4e/4d parts: `SessionController`, `SessionAccountDatabase` with `JdbcAccountDriverFactory`, `MockDataSource`, `OutboxFlusher`, `FullPull`; a `TestScope` with `runTest(timeout = 60.seconds)`): `stayPreservesQueueDiscardWipesAndNextAccountHasNoOldRows`, `realSeededCheckOffRequiresConfirmationOnlyWhileMutationsStayOffline` (two cases, `mutationsOffline` false/true), `observationUpdatesBadgesAndEmptiedQueueDismissesSheet`, `emptyQueueAndLogoutTimeoutStillFinishSignOut` (virtual time; the 3 s logout cap), `forced401DuringFlushNeverPresentsStaleConfirmation`, `syncEventsDriveOfflineCopyAndReplayToALateSettingsSubscriber`, `replacementSessionRevokesOldDiscardAndClearsValues`. Plus `discardWithoutSheetIsIgnored` and `doubleTapSignOutRunsOnce`.

### Task 4: `feat(settings): add the settings page scaffold and components`
- `SettingsPage` (§5.7), `SettingsWideMetrics`, `SettingsDestinationRow`, `SettingsChoiceCard`/`SettingsRadioCards`, `SettingsReadOnlyField`, `SettingsOfflineHint`, `SettingsNeedsConnection`. Each `@BoardMetrics` object holds the dp literals.
- Tests (`@Layout`): wide test at 840 dp width asserts the content column is 560 dp and centred, and at 599 dp it fills minus 32; `SettingsChoiceCard` exposes `Role.RadioButton` and `selected`; `SettingsDestinationRow` badge is below the subtitle (bounds check); guards at `largeFont`.

### Task 5: `feat(settings): add the settings home screen`
- `SettingsHomeScreen` (§5.4), `SettingsSyncLine` leaf, `SettingsHomeMetrics`.
- Tests (`@Logic`, `…/fst/home/SettingsHomeScreenTest.kt`): rows and badges for missing 3 and credits 2 (*[Refresh]* by tag: `settings.readiness` present, `settings.plan` present; no copy assertions); release-visibility (label hook null → only Account); sign-out disabled when busy or no account; profile card absent without account; sync-line invalidation probe: advance the clock one minute and assert only `SettingsSyncLine` recomposed (*[Refresh]* a local `SideEffect` counter as `WelcomeRecompositionTest`; no shared 0e counter exists).

### Task 6: `feat(settings): add the sign out pending sheet`
- `SignOutPendingContent`, sheet host in `SettingsHomeScreen` driven by `showsPending`, `SignOutPendingMetrics`; Terms slot wiring in `:app` (§5.5), *[Refresh]* required.
- Tests: *[Refresh R9: no copy assertions]* JVM: the title count is `pending.size` (3 rows, 2 distinct → 2) and the item texts are `PendingText` values; `@Logic`: swipe/back ignored while busy; Stay closes and keeps rows; the Terms slot shows the sheet (tag `settings.pending`) with one item node per pending entry and calls `onStay` when emptied; the sheet survives `recreate()` in a plain `ComponentActivity` host (moved here from Task 13).

### Task 7: `feat(settings): add stored and loading states`
- `SettingsStoredState`, `SettingsLoadingRows` (§5.6).
- Tests (`@Logic`): loading vs missing vs retry calls `requestSync`.

### Task 8: `feat(settings): add the account and profile pages`
- `AccountScreen`, `ProfileScreen`, `SettingsProfileModel`, `ProfileViewModel`, `languageSubtitle` (§5.8).
- Tests: JVM `SettingsProfileTests`: `validatesTrimsAndPreservesEditedDraftDuringPulls`, `sessionUpsertPreservesPendingProfileAcrossRelaunch` (4d hand-off: enqueue offline, reopen the database, pull, the pending name wins), `storageFailureKeepsDraftForRetry`, `offlineProfileIsDurablePendingPullCannotOverwriteAndRevokedPermitCannotWrite`; plus `nameOf256CharsIsRejected`. `@Logic`: Account profile row present (*[Refresh]* the subtitle's name and email are asserted on the ViewModel state, not as text); email field is read-only; saved hint appears after save; draft survives `recreate()`.

### Task 9: `feat(settings): add the language page and applier`
- `LanguageScreen`, `LanguageSettingsViewModel`, `LanguageDependencies`; `:app` `AppLanguageApplier`, `LanguageFeedback` in AppGraph (§5.9).
- Tests: JVM `LanguageSettingsTests`: `localLanguageAndThemePersistAndUnsupportedPhoneLanguageFallsBackToEnglish` (port; theme half asserts only that the theme key is untouched), `selectingCurrentChoiceDoesNothing`, `feedbackIsConsumedOnce`. Robolectric `@Layout` `AppLanguageApplierTest` with `@Config(sdk = [29, 36])`: choose Nl → after recreation the activity's `resources.configuration.locales[0].language == "nl"` *[Refresh; no copy assertion]* and `device-config` (29) / `applicationLocales` (36) hold `nl`; host a plain `ComponentActivity` that applies `ConfigurationPolicy` (no `MainActivity`, R9); choose System → follows a changed system locale (OQ-A25). If Robolectric 36 lacks `ShadowLocaleManager` support, mark the 36 case `@Ignore("needs ShadowLocaleManager")` with an OQ and rely on the Task 13 emulator test.

### Task 10: `feat(app): wire settings routes and seed paths`
- `AppShell` entries (§5.10), `SessionSettingsDependencies`, `ShellStart.Settings`, `SettingsSeedRoute.path`, `DebugShellStart` mapping, `reconcileDeviceConfig()` call. *[Refresh: no `holder.clear()` guard; Hand-in 6 passed, R3.]*
- Tests: JVM `ShellNavSettingsSeedTest` (every mapping in §5.10, unknown → null) plus `ShellNav.seed(ShellStart.Settings)` and `ShellNav.back` from Profile → Account → Settings (pure JVM in `core/navigation`). *[Refresh: dropped the `recreate()` check, which `ShellRecreationTest` already covers, and any test that composes `AppShell`/`CfRoot` (Konsist boundary rule).]*

### Task 11: `feat(debug): seed settings pending changes`
- `CF_SETTINGS_TEST_PENDING` (§5.10, R4). *[Refresh: `GraphOverrides.mutationsOffline` exists.]*
- Tests: *[Refresh: extend `TestSeedSessionKeysTest` and the seed-flag test for the key (`TestSeed.mutationsOffline` true)]*; `@Logic` `SettingsTestSeedsTest`: seed on → outbox has one `CheckOff` row; without `CF_AUTH_TEST_ID` → ignored; another account email → no row; no actionable step → throws with the message.

### Task 12: `test(settings): guard and capture the settings boards`
- `…/fst/SettingsGuardsTest.kt` (`@Layout`): Home (full, no account, pending, syncing, offline), sheet, Account, Account missing, Loading, Profile (idle, error, saved), Language (each choice) × `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` × en/nl × light/dark; `Guards.assertAll`.
- `…/fst/SettingsCaptureTest.kt` (`@Capture`): `BoardCapture.capture(rule, "SetHome_pixel8_en_light")`, `SetHomeNL_pixel8_nl_light`, `DarkSetHome_pixel8_en_dark`, `SignOutPendingSettings_pixel8_en_light`, `SetAccount_pixel8_en_light`, `SetProfile_pixel8_en_light` (name-only; note in the PR), `SetLanguage_pixel8_en_light`, `SetStates_pixel8_en_light` (four sync lines stacked), `SetStatesMore_pixel8_en_light` (loading + missing), plus NL twins where a board exists; `SignOutPending_pixel8_en_light` (sheet over Terms) *[Refresh: required; Phase 8 is merged]*.

### Task 13: `test(app): walk sign out and language on the device`
- `…/appat/settings/SettingsSignOutDeviceTest.kt` (`@Smoke`, launched seeded via `ScreenTest`): `signOutWithNoPendingOpensSignInWithoutReplayingLaunch` (tap `settings.signOut` → `auth.signIn` visible, launch screen never shown); `pendingSignOutStayAndDiscard` (`CF_SETTINGS_TEST_PENDING=1`; tap Sign out → `settings.pending` → Stay → sheet gone, `settings.sync` present → Sign out → Discard → Sign in) is the feature's one journey; the no-pending test seeds and checks one screen. *[Refresh: `pendingSheetSurvivesRecreation` moved to Robolectric in Task 6 (at most one journey per feature).]*
- `…/appat/settings/SettingsLanguageDeviceTest.kt` (`@Smoke`): `languageChoiceUpdatesAppAndReturnsToAccount` (`CF_SETTINGS_ROUTE=language`; choose Nederlands → activity locale `nl` *[Refresh; no copy assertion]*, `settings.language.saved` counter = 1 → Back → `settings.account.screen` shown; the next `ScreenTest` launch reseeds `cf.settings.language=system`, so no UI reset in `@After`).

### Task 14: `docs(settings): record settings home acceptance`
- Tick the 10a boxes in `plan.md`; append `OQ-10a-n` entries with the next free `OQ-A` numbers (*[Refresh]* highest used is OQ-A461: OQ-10a-1…19 → OQ-A462…A480, then OQ-A481 and OQ-A482 from R6; format `| OQ-A462 | (10a OQ-10a-1) … | default | 10a |`); note any name differences from this brief.

---

## 7. Acceptance criteria

- `make quick` and `make check` (pixel emulator booted) pass; `make generators-check` passes.
- `make targeted ONLY=net.chronoflow.feature.settings` and `make targeted ONLY=net.chronoflow.app.settings` pass within budgets.
- `make captures ONLY=Set` and `ONLY=SignOutPending` produce every capture in Task 12; reviewer compares each side by side with the board.
- Home, sheet, Account, Profile and Language match their boards in en/nl and light/dark, except the recorded OQs.
- Sign-out with no pending lands on Sign in without the launch screen; with pending, Stay keeps all rows and Discard removes them and signs out.
- Language switch works on API 29 (Robolectric + `device-config`) and API 36 (Robolectric where supported + pixel emulator), survives relaunch, and `System` follows the phone.
- Terms' `signOutConfirmation` shows the same sheet (*[Refresh]* Phase 8 is merged).
- No dp literal outside a `@BoardMetrics` object; no raw colour; `Text` gets resources only (Konsist green).
- Every ported iOS test exists with its Swift name.

---

## 8. Review checklist (Opus reviewer)

1. `SettingsHomeState` matches the Swift file line by line: identity strings, `replace` only for check-offs, dedupe keeps position, sleep subset rule, days computed by start-of-day in the zone.
2. Sync priority is syncing > pending > offline > age; title uses `pending.size`, line uses `pendingCount`.
3. `signOut` guards: Discard ignored unless the sheet is shown; session re-checked before and after `NeedsConfirmation`; busy reset in `finally`; `CancellationException` rethrown.
4. A replacement session cannot run an old Discard.
5. No failure UI was added.
6. Sheet cannot be dismissed while busy; survives `recreate()`; not persisted across process death.
7. Badge sits under the subtitle; release hides unbuilt rows; debug shows them with `RoutePlaceholder`.
8. Clock read only in `SettingsSyncLine`; invalidation probe present.
9. Profile: trim, 255 limit, draft kept during pulls and errors, email unchanged in the payload, permit checked.
10. Language: 33+ uses `LocaleManager`, < 33 uses `device-config` + `recreate()`; same-choice no-op; feedback consumed once; no server call.
11. Shell back stack survives recreation (`ShellViewModel`; `ShellRecreationTest`).
12. All copy from resources; names match `names.lock.json`; no new copy.
13. Tags exactly as in §5; tests query by tag.
14. Captures exist for every board in §2.2 except `Set2FAWide*` (rule only).
15. Commits: one per task, Conventional Commits, no trailer.

---

## 9. Open questions (proceed on the default; append each to `plan.md` as `OQ-10a-n` with the next free `OQ-A` number: *[Refresh]* OQ-10a-n → OQ-A(461+n), shown in the id column)

| id | Question | Default |
|---|---|---|
| OQ-10a-1 → OQ-A462 | Security subtitle: board says "Password, passkeys, two-factor", iOS "Password, two-factor". | iOS until passkeys ship (10c switches to `settings_password_passkeys_two_factor`). |
| OQ-10a-2 → OQ-A463 | Pending item text is 15/22 on the board; no token. | `screenBody` 14/20. |
| OQ-10a-3 → OQ-A464 | Sync line 13/18 (board) vs iOS 12/16. | Board: `settingsHint` 13/18. |
| OQ-10a-4 → OQ-A465 | Sign-out pill 40 dp (board `h-10`) vs iOS 44/48. | Board 40 (`CfButtonSize.Lg` *[Refresh]*); touch target still 48 via M3 minimum interactive size. |
| OQ-10a-5 → OQ-A466 | Home bottom padding 120 (board) vs 40. | 40; Android's bar is not overlaid. |
| OQ-10a-6 → OQ-A467 | "Syncing…" glyph: board shows `loader-circle`; animate? | Static glyph (reduced motion safe). |
| OQ-10a-7 → OQ-A468 | Generic "Saved" / "Couldn't save…" toasts on `SetStates`. | Not built; no iOS use. |
| OQ-10a-8 → OQ-A469 | Refused-change "Details" action. | Not built; iOS has none. |
| OQ-10a-9 → OQ-A470 | iOS hour-cycle override test. | Not ported; Android has no locale hour-cycle extension. |
| OQ-10a-10 → OQ-A471 | Language saved toast style. | `CfSnackbarHost` (OQ-A5) with board tokens where the host allows. |
| OQ-10a-11 → OQ-A472 | App bar on wide windows. | Full width; only content is 560. |
| OQ-10a-12 → OQ-A473 | Notification channel names after an in-app locale change. | 12b re-localises them. |
| OQ-10a-13 → OQ-A474 | Delete account row before 10d. | Hidden. |
| OQ-10a-14 → OQ-A475 | "Your data & policies" row. | Omitted (OQ-3). |
| OQ-10a-15 → OQ-A476 | Email change on Profile. | Deferred (OQ-5); email read-only, "Save changes". |
| OQ-10a-16 → OQ-A477 | `SetProfile` board shows the email flow. | Capture name-only as `SetProfile` and note the difference. |
| OQ-10a-17 → OQ-A478 | Footer type: iOS `screenMeta` 12/16, board 13/18. | Board 13/18 (`settingsHint`). |
| OQ-10a-18 → OQ-A479 | Body clock badge: English `SetHome` draws it trailing (before the chevron); `SetHomeNL` and iOS put it below the subtitle. | Below the subtitle in both languages (iOS behaviour, and the NL board). |
| OQ-10a-19 → OQ-A480 | Name length: iOS counts graphemes (`String.count`), Laravel `max:255` counts code points. | Count code points, so the server never refuses a name the app accepted. |
| OQ-A481 *(refresh)* | Loading rows: the `SetStatesMore` board draws bars 140/180/120 in 64 dp rows; iOS `SettingsLoadingRows` uses the generated `SettingsTokens.skeletonRowWidths` 150/190/130 in `counterHeight` 60 rows. | iOS and the generated tokens (sources order; values only from generated tokens); record the board difference. |
| OQ-A482 *(refresh)* | Disabled row alpha: board 0.5 (= `AuthTokens.disabledOpacity`) vs `SettingsTokens.disabledOpacity` 0.45 (iOS password prompt). | `AuthTokens.disabledOpacity` 0.5 for rows (board; also `CfButton`'s default). |

### Task 9 harness note (2026-11-21)

Robolectric 4.18 has `ShadowLocaleManager`, but its setter only updates a package-to-`LocaleList` map; it neither projects the list into Activity resources nor dispatches recreation. The API 36 activity-locale case is therefore ignored with `needs ShadowLocaleManager` (OQ-A483). API 36 read/write and preference reconciliation remain tested; the pixel test proves the actual configuration/recreation path. API 29 keeps the full device-config/recreation test.

The timing collector initially counted the ignored JUnit case as executed and rejected its absent listener entry. Task 9 also excludes XML `<skipped/>` cases from budget matching, while preserving every missing-entry check for executed cases; a runner regression covers a measured case beside an ignored one. No timing budget changes.

### Task 10 seed mapping note (2026-11-21)

`SettingsSeedRoute.path` still returns null for the accepted-but-unmapped `twoFactorSettings` and `passkeys` strings. `DebugShellStart` must select `ShellStart.Settings(emptyList())` for these accepted settings seeds, rather than return no start: pinned iOS selects Settings before its switch, so the default case stays at Settings root. A null/dropped settings seed still supplies no start, and plan-route seeds keep their existing priority. This corrects R4's optional `?.let` shorthand without changing any earlier plan-route behavior.

### Task 12 capture and guard boundaries (2026-11-21)

`SignOutPending` over Terms is captured by `:app`'s `SettingsTermsCaptureTest`; the remaining boards use `:feature:settings`'s `SettingsCaptureTest`. This keeps the prohibition on feature-to-feature imports. The native sheet and activity renders are composited with their window dim/gravity for the two overlay boards. Feature guards host Settings content with all fixture insets; captures include the native phone shell. Robolectric does not project the fixture's top inset into a native navigation rail, so the existing shell/native-control tests retain that responsibility.

### Task 13 tag note (2026-11-21)

The landed Android Sign-in screen exposes `sign-in.scroll`, not `auth.signIn`. The device tests query that existing structural tag and leave the auth feature unchanged. The no-pending test also checks the existing launch frame counter and t0 remain unchanged, so a transient replay cannot pass through a final absence assertion.
