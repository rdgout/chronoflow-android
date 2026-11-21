# Phase 10b: Preferences and body clock

- **Branch:** `feat/phase-10b-settings-preferences-body-clock`. Start it from the stack top once **10a** (Settings home, shared settings states, Account/Profile/Language), **8** (walkthrough replay on a tab stack) and **9e** (`CfTimePickerSheet`, `WizardClockRounding` and the `android_wizard_time_sheet_steps` string; review 2026-11-16) are all in it. Through them you get 6a (shell, `ShellHolder`, `CategorySwatch`), 6b (`PlanPreferences` reads `preferences`), 4e (routes, seeds, `RouteProbe`), 4d (outbox, `SettingsRepository`, pending protection), 4a (`DevicePreferences`, `DeviceConfigFile`), 2 (primitives, system wrappers), 0c (strings, `CopyIndex`) and 0e (tiers, guards, captures). If 10a, 8 or 9e is missing, stop and report it.
- **Wave:** 12, in parallel with 6e, 7, 9f and 12b. 10c (wave 10) also edits `:feature:settings`; if it has not landed, keep your edits to shared files (the settings entry provider, `strings_android.xml`, `plan.md`) small and additive. **9e** is in the stack before you and owns the time picker sheet, the 5-minute rounding and the `android_wizard_time_sheet_steps` string; reuse them (§5.7).
- **Plan entry:** `plan.md` "Phase 10b: Preferences and body clock" (l.3109–3141; refresh R1). Also read: A5 (device preferences in DataStore), A7 (outbox, final refusal), A8 (tab back stacks), A9 (system components vs designed surfaces), A10, A11 (no DataStore before the overlay's first frame), A17 (the 12/24 h rules), the "No health data (2026-11-10)" row (l.142), "Android spec: configuration and appearance" (l.348–355), the spec table row `### Appearance` (l.262), "Android spec: window policy" (l.357), OQ-A5 (l.602), OQ-A8 (l.605) and OQ-A37 (l.634).
- **Tests:** follow `AGENTS.md` "Tests": the cheapest tier, the budgets, exact tags, no `Thread.sleep`, and the hang caps. Use the 0e tier annotations: Robolectric `@Logic`/`@Layout`/`@Capture`, and emulator `@Smoke`.
- **Tags:** **[F]** is a fact read in source (path given). **[R]** is a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`. `W/` = `/Users/rickgout/Sites/chronoflow-new` at `e36368ec`.
- **Checked 2026-11-16** against iOS `f391a1fa`, web `e36368ec`, the boards, `plan.md` and the 4a/4d/8/9e/10a briefs. Corrected: the 10a names (10a builds `SettingsDependencies`, not a `SettingsPagesModel` port; its `SettingsPage` takes a string resource; its `SettingsRadioCards` are reused; it already calls `reconcileDeviceConfig()`); the missing `enqueueSettings` seam and `ThemeDependencies`; the `isBuilt` flips for both home rows; iOS file line counts and test counts (6 pure body-clock cases; 7 preferences functions); the `replaySkipAndDone…` port, which Phase 8 does not have; the theme half of `localLanguageAndThemePersist…`; the validate-then-return rule; and the Kotlin "is a JSON number" check for cups. Copy (en/nl), tokens, board values, endpoint shapes and the iOS line references were verified unchanged.
- **Paths:** `…/settings/` = `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/ds/` = `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/`; `…/datastore/` = `core/datastore/src/main/kotlin/net/chronoflow/core/datastore/`.
- **Names from 10a** (checked against `docs/phases/phase-10a-settings-home.md`, 2026-11-16; **superseded by refresh R2–R3**: 10a *did* port `SettingsPagesModel`). It builds:
  - `SettingsDependencies` + `SettingsDependenciesKey` (`…/settings/SettingsDependencies.kt`, 10a §5.3: `snapshot`, `settings(): Flow<List<SettingsRecord>>`, `isCurrent()`, `enqueueProfile(…)`, `requestSync()`, …), implemented in `:app` by `SessionSettingsDependencies`. ViewModels get it with `viewModelFactory { initializer { X(this[SettingsDependenciesKey]!!) } }`;
  - `SettingsPage(title: Int /* string res */, onBack, testTag, content)` (10a §5.7: large title, padding 16/4/40, gap 24, the 560 dp column from 600 dp);
  - `SettingsStoredState(loaded, failed, onRetry)` (10a §5.6; tags `settings.loading` / `settings.missing` / `settings.stored.retry`);
  - `SettingsRadioCards` / `SettingsChoiceCard` (10a §5.9, the Language cards);
  - `SettingsSeedRoute.path(raw)` (10a §5.10, the `CF_SETTINGS_ROUTE` mapping) and the home rows' `isBuilt` flags (10a §5.4: "10b–10d flip their flags").

  10a does **not** build `SettingsEyebrow`, `SettingsError`, a `CF_SETTINGS_BOARD_STATE` seed, or a theme seam; this phase builds the ones it needs (§5.6, §5.8, §5.10). (10a **does** have a `SettingsPageFixture` port, named `SettingsFixture`; see the refresh notes.) Other names come from the briefs of the phases already merged: `CfCard`, `CfDivider`, `CfRow`, `CfBadge`, `CfPill`/`CfPillGroup`, `CfSwitch`/`cfToggleable`, `CfTextField`, `CfButton`, `CfModalBottomSheet`, `CfTimePickerDialog`, `CfLargeTopAppBar`, `CategorySwatch(type, CategorySwatchSize.Small)`, `ShellHolder.push/back`, `Route.Preferences`, `Route.BodyClock`, `Route.Reminders`, `Route.Walkthrough(replay)`, `DevicePreferences`, `ThemeChoice`, `DeviceConfig`, `OutboxChange.Settings(section, patch)`, `SettingsSection`, `ProfilePreferences`, `CopyIndex.find(CopyTable.Settings, key)`, `LucideGlyphs`, `PlanUITokens`, `SettingsTokens`, `AuthTokens`, `CfTheme.colors/type`, `Guards`, `BoardCapture`. **Before you code, grep the merged code and use its exact names.** If a name differs, keep the merged one and say so in your report. Never rename earlier code. If a piece listed above does not exist, build the smallest port of the iOS type under the iOS name in `…/settings/components/`, and say so in your report.

## Refresh notes (2026-11-21, against d838ffbf)

These notes were checked against the code at `d838ffbf` (top of 10a; 8 and 9e are in the stack). **Where a note and the body below disagree, the note wins.** The worker repeats these checks first (`git grep` each name) and stops on any mismatch.

**R1. Plan and OQ references.**
- `plan.md` "Phase 10b: Preferences and body clock" is at **l.3109–3141** (not 2431–2466). Its three boxes are "**Preferences**" (l.3127), "**Body clock**" (l.3133) and "**Tests.**" (l.3138).
- `plan.md` line refs that moved: OQ-A5 is at **l.602**, OQ-A8 at **l.605** and OQ-A37 at **l.634**. These are unchanged: "No health data (2026-11-10)" l.142, `### Appearance` l.262, "Android spec: configuration and appearance" l.348 (theme rule l.354), "Android spec: window policy" l.357.
- The next free plan OQ is **OQ-A484** (10a used up to OQ-A483). Task 13 appends the §9 rows as **OQ-A484 … OQ-A498**, in order: OQ-10b-1 → A484, -2 → A485, -3 → A486, -4 → A487, -5 → A488, -6 → A489, -7 → A490, -8 → A491, -9 → A492, -10 → A493, -11 → A494, -12 → A495, -13 → A496, -14 → A497, -15 → A498. Each plan row starts "(10b OQ-10b-n)", as 10a's rows do. New questions take A499 and up.

**R2. What 10a built (exists; reuse it, do not rebuild).** All paths are under `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/` unless marked otherwise.
- `SettingsDependencies` + `SettingsDependenciesKey` + **`SettingsMainKey`** (`SettingsDependencies.kt`). The real members are `snapshot`, `account()`, `settings(): Flow<List<SettingsRecord>>`, `outbox()`, `pendingOutbox()`, `syncEvents()`, `sessionUpdates()`, `isCurrent()`, `signOut(discardPending)`, `enqueueProfile(name, email)`, `requestSync()`, `clock: PlanClock`, `formatter()` and `zone`. There is **no `enqueueSettings`**. The implementers are `app/…/settings/SessionSettingsDependencies.kt`, the test fakes `feature/settings/src/test/…/support/FakeSettingsDependencies.kt` and the `SettingsFixture` dependencies in `support/SettingsFixture.kt`. Adding a member means updating all three.
- **`SettingsPagesModel` is ported** (`SettingsPagesModel.kt`; the body's "10a does not port `SettingsPagesModel`" is stale). `SettingsPagesState(account, settings: List<SettingsRecord>, loaded, settingsLoaded, failed)`, `start()`, `retry()` (cancels, re-collects, then `requestSync()`, as iOS `retry()`), `saveProfile(name)` (`active && isCurrent()` else `StaleSessionException`) and `stop()`. Use it for the section map, the loaded/failed state and Retry. Do not hand-roll a `deps.settings()` collector (§5.8 "Dependencies").
- `settingsAttempt(failed, cancelled = failed, block)` (`SettingsAttempt.kt`, internal). It rethrows a `CancellationException` only when the *calling* coroutine is cancelled. A foreign cancellation (such as `StaleSessionException`) goes to `cancelled`. It catches the typed failures (`SessionFailure`, `DataFailure`, `StoreFailure`, `LocalMutationFailure`, IO/SQL/security, `RuntimeException`), never `Exception` broadly.
- ViewModel pattern (`profile/ProfileViewModel.kt`): `class XViewModel(deps: SettingsDependencies, …, main: CoroutineDispatcher) : ViewModel(CoroutineScope(SupervisorJob() + main))`, with `pages = SettingsPagesModel(deps, viewModelScope)` started in `init` and stopped in `onCleared()`. The model is created on the first emission and later emissions call `receive`. The ViewModel exposes `val ui: StateFlow<…>` from a `MutableStateFlow`. The factory is `viewModelFactory { initializer { X(checkNotNull(this[SettingsDependenciesKey]), …, this[SettingsMainKey] ?: Dispatchers.Main.immediate) } }`.
- Screen pattern: a stateless `XScreen(state, onEvent)` plus an `XRoute(viewModel, onBack, …)` wrapper (`language/LanguageScreen.kt`: `LanguageScreen` + `LanguageEvent` + `LanguageRoute`; `home/SettingsHomeRoute.kt`; `profile/ProfileScreen.kt`).
- `components/SettingsPage.kt`: `SettingsPage(title: Int, onBack: (() -> Unit)?, testTag: String, modifier, content: @Composable ColumnScope.() -> Unit)`. It is a `Scaffold` + `CfLargeTopAppBar` + `verticalScroll` `Column`, with padding top `space1`, horizontal `space4` and bottom `SettingsTokens.pageBottom`, gap `space6`, and `SettingsWideMetrics.COLUMN` (560) from `WindowPolicy.wideBreakpointDp`. The content tag is `"$testTag.content"`. **There is no `LazyListScope` variant** (see R6).
- `components/SettingsStoredState.kt`: `SettingsStoredState(loaded, failed, onRetry, modifier)` (tags `settings.loading`, `settings.missing`, `settings.missing.title`, `settings.stored.retry`) and `SettingsLoadingRows()`.
- `components/SettingsChoices.kt`: `SettingsOption(id, title, hint)`, `SettingsRadioCards(options, selection, testTag, onSelect, modifier)` (option tags `"$testTag.$id"`) and `SettingsChoiceCard(title, hint, selected, testTag, onClick, modifier)`. It has **no title parameter**. The unselected radio stroke uses `AuthTokens.disabledOpacity` (0.5), **not** `PlanUITokens.statusBorderOpacity`. Keep 10a's value. The radius is `CfTheme.shapes.x2l` and the stroke is `SettingsTokens.selectionWidth`.
- `components/SettingsDestinationRow.kt`: `SettingsDestinationRow(title, glyph: LucideGlyph, testTag, onClick, modifier, subtitle, badge, badgeTag, enabled, chevron = enabled, singleLineSubtitle)`. This is iOS `SettingsRow`: `rowHeight` 64, a `rowDisc` 36 `muted` disc and `rowVertical` 10. **Use it for the Preferences link rows** (reminders: `chevron = true`; replay: `chevron = false`) instead of `CfRow`. iOS uses `SettingsRow` for both.
- `components/SettingsFields.kt`: `SettingsReadOnlyField`, `SettingsOfflineHint`, `SettingsOfflineCard` (the offline card + hint pair; not used by 10b).
- `home/SettingsHomeScreen.kt`: `enum class SettingsDestination(route, glyph, title, subtitle, tag, isBuilt = false)`. `BodyClock` (l.~96) and `Preferences` (l.~103) have no `isBuilt` argument, so they are false. Flip each by passing `true`, as `Account` does. The release-visibility expectation is `home/SettingsHomeScreenTest.kt` (`state(showUnbuilt = false)`, l.~53).
- Test support (`feature/settings/src/test/…`): `support/SettingsScreenTest` (a Robolectric base hosting a plain `ComponentActivity`, with `ProfileRule`, `show { }` and the native-runtime warm-up through `measureClassWarmup`), `support/FakeSettingsDependencies`, and `support/SettingsFixture` + `SettingsFixtureTest` (real `SessionAccountDatabase` over `JdbcAccountDriverFactory`, `OutboxRepository`, `SettingsRepository` and `MockDataSource`, warmed once in `@BeforeClass`). **`SettingsFixture` is 10a's port of iOS `SettingsPageFixture`** (Task 9 said "10a has none"): extend it with a `bodyClock()`/`preferences()` ViewModel factory, as it has `profile()`. Also `SettingsBoardFixture.kt` (a composable board fixture keyed by state) and `SettingsCaptureTest.kt` (`@Capture`, parameterized `board, state, language, dark`).
- `:app`: `app/…/settings/SettingsEntry.kt` (`SettingsEntry(graph, session, route, onBack, onNavigate, modifier)` builds `extras` with `SettingsDependenciesKey` + `SettingsMainKey`, and `languageExtras` with `LanguageDependenciesKey`; Settings home already calls `graph.devicePreferences.reconcileDeviceConfig()` in a `LaunchedEffect`), `SettingsSeedRoute.kt` (in **`:app`**, not the feature; it already maps `"bodyClock"` → `[Route.BodyClock]` and `"preferences"` → `[Route.Preferences]` explicitly) and `AppLanguageApplier.kt` (`graph.languageApplier`).
- `app/src/test/…/settings/ShellNavSettingsSeedTest.kt` **already has** the `bodyClock` and `preferences` cases (l.31–32). §5.9's "add the two cases" is done.
- `TestSeed.settingsRoutes` (`app/src/debug/…/debug/TestSeed.kt:65`) already accepts `bodyClock` and `preferences`.

**R3. Names that differ from the body.**
- `ShellHolder` does not exist. The shell state holder is `ShellViewModel` (`app/…/shell/ShellViewModel.kt`, internal to `:app`). Feature screens never see it. They get `onBack` and `onNavigate: (Route) -> Unit` from `SettingsEntry`, and `onNavigate` is `holder::push` in `AppShell`. So Replay is `onNavigate(Route.Walkthrough(replay = true))` and Reminders is `onNavigate(Route.Reminders)`, both emitted as `PreferencesEvent`s and wired in the route wrapper.
- The 9e string is **`android_wizard_time_sheet_steps`** (Android-only copy, `tools/l10n/android-only.json`), not `wizard_time_sheet_steps` as the body first said (the body is patched). It exists in en and nl.
- `CfTimePickerSheet(title, subtitle, footnote, initialHour, initialMinute, is24Hour, confirmLabel, dismissLabel, onConfirm: (hour, minute, touched) -> Unit, onDismiss, modifier, testTag = "time-sheet", confirmTag, dismissTag, state)` is in `core/designsystem/…/system/CfTimePickerSheet.kt`. `WizardClockRounding.roundToStep(hour, minute): String?` (nullable) is in `core/wizard/…/model/WizardClockRounding.kt`. `:feature:settings` does **not** depend on `:core:wizard` yet: add `implementation(project(":core:wizard"))` (a JVM library) to `feature/settings/build.gradle.kts`.
- `CategorySwatch(type: BlockType, modifier, size)` (`core/ui/…/plan/CategorySwatch.kt`) takes a `BlockType`, not a raw string. Use `BlockType.Sleep`, `BlockType.SeekLight`, `BlockType.CaffeineServing` and `BlockType.Flight` (iOS `swatch(_:)`, `SettingsPreferencesView.swift`). `SettingsTimelineGroup.types` stay raw strings.
- "Connectivity & reminders" exists in both tables (`reminders_connectivity_reminders`, `settings_connectivity_reminders`). Use **`reminders_connectivity_reminders`**: iOS reads it through `ReminderStrings`. Every other §5.1 key was verified in `tools/l10n/names.lock.json` (`settings_…`), including the two separate "We need …" keys (`settings_we_need_1_more_answer_before_we_c7ff63`, `settings_we_need_count_more_answers_befor_63734e`).
- Lucide glyphs: `Check`, `CircleAlert`, `TriangleAlert`, `Plus` and `ChevronRight` exist. **`Bell`, `RotateCcw` and `Minus` are missing.** Add them to `tools/icons/lucide.json` and regenerate with `make vectors` (`tools/icons/generate_icons.py`), in the commit that first uses them.
- `ThemeChoice` (`core/model/…/config/ThemeChoice.kt`) has `raw` and `fromRaw`, but **no `resolve`**. Add it (§5.5). `DevicePreferences.setTheme(choice, sdkInt)` writes `device-config` below **31**. `theme: Flow<ThemeChoice>` and `reconcileDeviceConfig(sdkInt)` exist (`core/datastore/…/DevicePreferences.kt`). `DeviceAppearance`, `NightModeSink`, `UiModeNightModeSink` and `ThemeDependencies` do not exist yet.
- 4a/4d names exist as cited: `StaleSessionException : CancellationException` (`core/model/…/session/`), `StoreFailure.Closed`, `DataFailure.Offline`, `OutboxChange.Settings(section, patch: JsonObject)` and `SettingsSection.BodyClock|Preferences|Reminders` (`core/model/…/OutboxChange.kt`), `SettingsRecord(section, value: String)`, `ProfilePreferences.bodyClock/readiness/requestFields`, and `internal object StoreApplication.setting(section, value, sql)` (`core/data/…/store/`). `SyncStore` is at `core/data/src/test/…/sync/support/SyncStore.kt`, and `ScriptedSource` is in **`:core:testing`** (`core/testing/…/sync/ScriptedSource.kt`), not 4d's own test code. Neither `offlineQueueAndPullsPreservePendingValuesInOtherGroups` nor `offlineChangesRemainPendingDuringPull` exists yet, so Task 4 stands.
- There is no shared "debug recomposition counter from 5d/9f". Count with a test-local `SideEffect` counter, as `feature/wizard/src/test/…/WizardRecompositionTest.kt` does.

**R4. Dependencies seam (replaces §5.8 "Dependencies").**
- Add `suspend fun enqueueSettings(section: SettingsSection, patch: JsonObject)` to `SettingsDependencies`, at the end, additively. Implement it in `SessionSettingsDependencies` in the shape of the real `enqueueProfile`: `withContext(app.dispatchers.io) { val permit = app.controller.mutationPermit(snapshot) ?: throw StaleSessionException(); session.outbox.enqueue(OutboxChange.Settings(section, patch), permit) }`.
- **Unlike `enqueueProfile`, do not call `session.sync.requestSync()`.** `SyncLifecycle.start()` already requests a sync for every new outbox id (`core/sync/…/SyncLifecycle.kt:72–87`).
- Also add `suspend fun enqueueSettings(section, patch)` to `SettingsPagesModel`, mirroring `saveProfile`'s guard (`if (!active || !deps.isCurrent()) throw StaleSessionException()`, then `ensureActive()`; iOS `SettingsPagesModel.enqueue`). The models' save closures call `pages.enqueueSettings(…)`.
- Update `FakeSettingsDependencies` and `SettingsFixture` to record or enqueue the change.

**R5. Exceptions and one-shot events (override §5.2, §5.4 and §10 "Exceptions").**
- iOS has `catch is CancellationError {}` in both `SettingsBodyClockModel.change` and `SettingsPreferencesModel.change`. A foreign cancellation is **silent**: it does not rethrow and sets no `failure`. Wrap each save in `settingsAttempt(failed = { failure = true }, cancelled = {}) { save(body); … }`, as `SettingsProfileModel.submit` does. Own-scope cancellation still propagates (inside `settingsAttempt`).
- Delete the "`catch (e: CancellationException) { throw e }` … `catch (e: Exception)`" steps. The Android-only test `cancellationIsSilentAndRethrown` becomes `foreignCancellationIsSilent`: a save that throws `StaleSessionException` leaves `failure == false` and no "Saved", and the caller's `change` returns normally. Keep FIFO serialization (a fair `Mutex` or a tail-`Job` chain, as iOS does).
- **Scroll requests are one-shot events held in the ViewModel until acknowledged.** "Go to the first one" and "a group newly saved" (NH4.1) set `BodyClockUiState.scroll: BodyClockScroll(id: Long, group, fieldKey?)`. The screen performs the scroll and then sends `BodyClockEvent.ScrollHandled(id)`, which clears it. **No `LaunchedEffect(state.saved)` diff** (replaces §5.8 "Scrolling" bullet 2) and no channel.
- **The time sheet is VM-held** (replaces §5.7's `rememberSaveable { mutableStateOf<String?>(null) }`): `BodyClockUiState.timeSheet: BodyClockTimeSheet(field, initialHour, initialMinute)?`, opened by `OpenTime(field)` and closed by `ConfirmTime(field, h, m)` or `DismissTime`, as 9e does (`feature/wizard/…/sleep/WizardSleepTimeSheet.kt`, `WizardTimeSheetUi`). On confirm, `WizardClockRounding.roundToStep(h, m) ?: return`, then always write (OQ-10b-3; ignore `touched`).
- The mDFD field: do not seed a `rememberTextFieldState`. Bind it from the ViewModel state the way 10a's Profile name field is bound (`profile/ProfileScreen.kt`, `ProfileNameBindingTest`).

**R6. Screens.**
- `PreferencesScreen(state, onEvent)` + `PreferencesRoute(viewModel, onBack, onNavigate)` and `BodyClockScreen(state, onEvent)` + `BodyClockRoute(viewModel, onBack)`, with `PreferencesEvent` and `BodyClockEvent` sealed interfaces. ViewModels: `PreferencesViewModel(deps, theme: ThemeDependencies, main)` and `BodyClockViewModel(deps, main)`, with `ui: StateFlow` per R2 (not `stateIn(WhileSubscribed)`).
- **`SettingsPage` has no lazy variant.** Body clock's keyed `LazyColumn` (§5.8) needs one. Add an additive overload in `components/SettingsPage.kt` (for example `SettingsLazyPage(title, onBack, testTag, listState, modifier, content: LazyListScope.() -> Unit)`) that shares the scaffold, app bar, insets, padding and 560 dp column. Do not change the `ColumnScope` version that 10a's pages use.
- Routing (§5.9): in `app/…/shell/AppShell.kt`, the `page` branch sends only `Account`, `Profile` and `Language` to `SettingsEntry` (l.~108). Add `Route.Preferences` and `Route.BodyClock` there. In `SettingsEntry`, add the two `when` branches, and add their titles to the `session == null` loading `SettingsPage`. `Route.Reminders` keeps falling through to `render.value(route)` (`RoutePlaceholder`). `Route.Walkthrough` is already rendered there by `WalkthroughEntry(graph, replay = true) { holder.back() }`.
- Add `ThemeDependenciesKey` to `SettingsEntry`'s session `extras`, next to `SettingsDependenciesKey`.

**R7. Theme root wiring (overrides §5.5 "Root wiring" and "System bars").**
- The root composable is **`CfRoot`** (`app/…/CfRoot.kt`), not `MainActivity`'s `setContent`. Today it does `val night = isSystemInDarkTheme()` (l.88), then `RootThemeContent(night, graph.rootThemeInputs(…))` (l.89; `RootThemeContent.kt` calls `ChronoFlowTheme(darkTheme, reduceMotion)`). Replace that `night` with `theme.resolve(isSystemInDarkTheme())`, where `theme` is collected from `graph.deviceAppearance.theme` only after `withFrameNanos { }`.
- **System bars already follow `night`.** `CfRoot` calls `SideEffect { onSystemBars(night, overlayShowing, onSky || launchSky) }`, which reaches `MainActivity.applySystemBars` → `enableEdgeToEdge`. Passing the resolved value is enough. Do **not** add a second `enableEdgeToEdge` `LaunchedEffect`. `MainActivity.onCreate` and `onConfigurationChanged` still call `applySystemBars(isNight(configuration), …)` from `Configuration.uiMode`. That is right on 31+ (app night mode is in `uiMode`). On 29–30, make sure the `SideEffect` re-applies the resolved value after a configuration change, and cover it in `RootThemeTest`.
- Initial theme below 31: `ConfigurationPolicy.overrideConfiguration` (`app/…/ConfigurationPolicy.kt:20`) already calls `DeviceConfigFile.readForLaunch(base, sdkInt)`, but keeps only `.language`. Keep that **single** read and expose its `.theme` in memory (for example, return the `DeviceConfig` or stash it in `MainActivity.attachBaseContext`) for `CfRoot`'s initial value when `sdkInt < 31`. Never read the file a second time. On 31–32 the file's theme is stale (`reconcileDeviceConfig` keeps `old.theme`), so use `ThemeChoice.System` there.
- `reconcileDeviceConfig()` is already called by `SettingsEntry` on `Route.Settings`, so do not add another call.
- `AppGraph` gains `val deviceAppearance by lazy { DeviceAppearance(devicePreferences, UiModeNightModeSink(app), dispatchers.io) }` next to `devicePreferences` (l.150) and `languageApplier` (l.155).

**R8. Tests (override Tasks 6, 8, 9 and 11 where they differ).**
- Logic runs as JVM tests on the ViewModels and models, using one `TestCoroutineScheduler`: pass `StandardTestDispatcher(testScheduler)` as `main` and use `FakeSettingsDependencies`. Screen tests extend `SettingsScreenTest`, which hosts a plain `ComponentActivity`. They find nodes by tag and **assert no copy**. Drop every text-content assertion from the body: in Task 7, the "Fewer coffee" / "Meer koffie" labels; in Task 9, "We need 2 more answers…", "0/2 done", "Add at least one drink…", "Your plan is adjusted…", and the "no node contains pregnan / 18–29" check. Assert state on the ViewModel instead (`readiness == NeedsAnswers(2, 2, 0)`, `errors["cupsFilterCoffee"] == "Add at least one drink to save this answer."` as a key, the `contentDescription` key resolved through `CopyIndex` in a JVM test). The no-health-data check stays as the §10 source grep.
- Every test runs in under 1 s warm. A one-off cold cost is warmed in `@BeforeClass` and recorded through `measureClassWarmup` (`core/testing-junit/…/ClassWarmup.kt`), as `SettingsScreenTest` and `SettingsFixtureTest` do. No Konsist allowlist additions.
- **Task 6 `RootThemeTest` already exists** (`app/src/testDebug/kotlin/net/chronoflow/app/RootThemeTest.kt`, 4 cases from 3b). Add the theme cases to it, or name a new class `RootThemeChoiceTest`. Host `RootThemeContent` (or the extracted theme-collecting composable) in a plain `ComponentActivity` with a fake `Flow<ThemeChoice>`; do not boot `MainActivity`/`AppGraph`. `darkChoiceRendersDarkTokens…` asserts the dark background pixel. "Without recreate" is proven on the emulator (Task 12). `noDataStoreReadBeforeFirstFrame` counts collections of the fake flow before the first `withFrameNanos`.
- **Task 8 `PreferencesReplayTest` (`:app`, booting the shell) is replaced:**
  - `PreferencesWalkthroughReplayTests` goes in **`feature/onboarding/src/test/…/walkthrough/`** (JVM). Konsist forbids `:feature:settings` → `:feature:onboarding` imports (`konsist/…/KonsistRules.kt:53`). It ports `replaySkipAndDoneReturnToPreferencesWithoutAnotherCompletionWrite` (parameterized `skip`: Skip, or `advance()` ×3; then `onFinish` called, `completed`, 0 writes) and the walkthrough half of `replayTwiceStartsFreshAfterSkipOrDone` (a second `WalkthroughModel(Mode.Replay, …)` starts at `Page.Timeline`, `!completed`, `!busy`, `failure == null`). `WalkthroughModel` is public in `feature/onboarding/…/walkthrough/WalkthroughModel.kt`.
  - The preferences half (set 12, then exactly one preferences row and no walkthrough row) is a `PreferencesViewModelTest` case. Phase 8's `replayReturnsWithoutWritingOrRoutingToToday` stays as it is.
- Task 9: `SettingsBodyClockStoreTests` uses 10a's `SettingsFixture` (R2), not a new `SettingsPageFixture`. `BodyClockRecompositionTest` uses a test-local `SideEffect` counter (R3).
- Task 11 captures: extend 10a's `SettingsBoardFixture` (new states) and add the rows to `SettingsCaptureTest.cases()`, or add a sibling `@Capture` class beside it in `feature/settings/src/test`. Do not add an `:app` capture.
- Task 12: `ScreenTest.launch(overrides: Map<String, String>)` (`app/src/androidTest/…/testing/ScreenTest.kt:51`) always sets `"cf.settings.theme" to "system"` and has no way to omit it. The planned extension is needed as written.

**R9. Seeds (§5.10, Task 10).** There is **no `CF_SETTINGS_BOARD_STATE`** in the code yet (only `docs/research/13-testing-tooling.md:291` names it). Add it to `TestSeed.parse` (`app/src/debug/…/debug/TestSeed.kt`) and to `TestSeedSessionKeysTest`'s row table (`app/src/testDebug/…/debug/TestSeedSessionKeysTest.kt`), gated by `CF_AUTH_TEST_ID`. Apply it in `app/src/debug/…/debug/` next to `SettingsTestSeeds.kt`. The demo-account readiness can come from `MockSettings.readiness(body)` (`core/data-mock/…/MockSettings.kt:273`) or `ProfilePreferences.readiness(value, bodyClock)`.

**R10. Body clock and the timeline.** Neither page changes timeline, plan or trip-page code. Preferences writes the stored `preferences` section, which `PlanPreferences` (`core/plan/…/trippage/PlanPreferences.kt`) already feeds to the trip page, Today and Trips (6b). Body clock writes only `bodyClock` and has no local plan effect. So no `make perf-timeline` is required. **If the worker touches anything under `core/plan`, `feature/trip` or the timeline**, it runs `make perf-timeline` and records the result in `docs/perf-timeline.md`. The theme change recomposes the root (`CfRoot`), so record one StrictMode-clean theme toggle on the trip page as part of acceptance item 7.

**R12. Repeated preflight (2026-11-21).**
- Lucide glyph generation is `make vectors` (`tools/icons/generate_icons.py`); `make icons` only generates launcher assets. Use `make vectors` in Task 7 and commit the manifest with its generated output.
- `components/SettingsComponentsTest.kt` already exists as a `@Layout` class with 10a geometry checks. Extend it and keep those checks; additional logic-only cases may live in a sibling `@Logic` class. Do not duplicate or replace the existing class.
- `BodyClockChoice.patch` must fall back from explicit JSON null free-day times as well as absent keys; iOS does both. The abbreviated §5.3 expression omitted the explicit-null fallback.

**R11. Copy.** This phase still adds no copy. If any Android-only string becomes necessary, it goes in `tools/l10n/android-only.json` with `{{name}}` placeholders and `"replaces": []`, and into plan.md "Android-only copy awaiting approval" (l.487).

---

## 1. Goal

Settings › **Preferences** (`SetPreferences`, `SetPreferencesNL`) lets the user pick:
- a **theme** (System / Light / Dark). The theme belongs to this phone: it is stored in DataStore, applied at once without recreating the activity, and on API 31+ mirrored to `UiModeManager`, so the next cold start's system splash matches;
- a **time format** (24-hour / 12-hour) and which **timeline categories** to show (Sleep, Light, Caffeine, Travel). Both are account settings, saved locally and through the outbox to `PATCH /v1/account/preferences`.

The page also holds two link rows. **Connectivity & reminders** pushes `Route.Reminders`, which stays a seam until 12c. **Replay walkthrough** pushes Phase 8's replay, which returns to Preferences.

Settings › **Body clock** (`SetBodyClock`, `DarkSetBodyClock`) shows:
- the **readiness card** in its four variants: needs-answers, excluded, ready and adjusted;
- five sections: About you, Sleep schedule, Caffeine, Light equipment and Safety checks.

Each section saves itself on every valid change, through the outbox to `PATCH /v1/account/body-clock`, and shows "Saved" by its heading. Invalid input stays a local draft with its field error and never reaches the outbox. A pending local value is never overwritten by a pull. A missing required answer gets the **"Needs an answer"** badge and the dawn box. Only age band and shift work are required, and the app asks no medical question (no-health-data).

Done means:
- the ported iOS `SettingsBodyClockTests` and `SettingsPreferencesTests` cases pass on the JVM;
- Robolectric guards cover both pages at `small`, `pixel8`, `foldInner`, `landscapeSideNav` and `largeFont`, in en and nl, light and dark;
- `make captures` produces the board captures, and the reviewer has compared them with the boards;
- on `pixel`, a theme change applies without a restart and persists in `UiModeManager`;
- StrictMode stays clean.

## 2. Inputs to read (in this order)

1. **This repo:**
   - `plan.md` (the sections above);
   - `AGENTS.md`;
   - `docs/phases/phase-4a-store.md` Task 8 (`DevicePreferences.setTheme(choice, sdkInt)`, `reconcileDeviceConfig()`, l.505–520, and the hand-off at l.697: "10a/10b … mirror to … `UiModeManager` (31+)");
   - `docs/phases/phase-4d-sync.md` Task 5 (l.276–293: `StoreApplication.setting` skips a section while a `settings:<section>` row is pending; `snapshot` deletes `readiness` while a bodyClock row is pending; `enqueue` merges snake keys into the stored camelCase section, clears `invalidDrafts`, sets `sleepAnswered` and deletes `readiness`);
   - `docs/phases/phase-4b-api-client.md` l.142–146 (`SettingsSection`, `OutboxChange.Settings`) and Task 2 (`SettingsPatchValidation`);
   - `docs/phases/phase-0d-contracts.md` l.381 (`ProfilePreferences`: `requestFields`, `bodyClock`, `readiness`);
   - `docs/phases/phase-0c-strings.md` T4 (`CopyIndex`). **The lock file `tools/l10n/names.lock.json` is the authority for resource names**;
   - `docs/phases/phase-0e-test-harness.md` T4 (`ThemeChoice`, `DeviceConfig`), T10–T11 (`DeviceConfigFile`, `TestSeeds`, `ScreenTest.launch` defaults);
   - `docs/phases/phase-2-design-system.md` Tasks 13–16 (the components in §5.6) and the `MainActivity` root at l.236;
   - `docs/phases/phase-6a-shell-trips.md` (`ShellHolder`, `CategorySwatch` l.356–367);
   - `docs/phases/phase-6b-trip-page.md` l.180–199 (`PlanPreferences` reads `timeFormat`/`hiddenCategories` from the stored `preferences` section; this phase changes nothing there);
   - `docs/phases/phase-8-walkthrough-terms.md` §5.8 (the replay entry, `holder.push(Route.Walkthrough(replay = true))`, `hidesShellChrome`);
   - `docs/phases/phase-9e-wizard-sleep-questions.md` §5.1 (`WizardClockRounding`) and §5.2 (`CfTimePickerSheet`);
   - `docs/phases/phase-10a-settings-home.md` §5.3 (`SettingsDependencies`), §5.4 (`isBuilt`), §5.6–§5.7 (`SettingsStoredState`, `SettingsPage`), §5.9 (`SettingsRadioCards`, `reconcileDeviceConfig()` call) and §5.10 (`SettingsSeedRoute`);
   - the merged 10a code under `feature/settings/` and `app/…/settings/` (grep for `SettingsDependencies`, `SessionSettingsDependencies`, `SettingsPage`, `SettingsStoredState`, `SettingsRadioCards`, `SettingsSeedRoute`, `isBuilt`, `CF_SETTINGS_BOARD_STATE`).
2. **Research:**
   - `docs/research/07-day-to-day-settings.md` §6.5 (Body clock), §6.6 (Preferences), §6.2 (shared states), §11 (endpoints, outbox kinds) and §13 (performance);
   - `docs/research/03-api-contract.md` §5.2 rows 14–15, §13.1 (`ApiAccountSnapshot`) and §13.2 (body-clock PATCH fields);
   - the iOS screenshots `docs/research/screens/ios/11-settings-bodyclock.png` and `12-settings-preferences.png`.
3. **Boards** (read-only; open the HTML in a browser to render them):
   - `DC/SetPreferences.dc.html`: 375 × 812, props `theme` and `lang`, main `padding: 4px 16px 40px; gap: 24px`. The Appearance card has radius 26, padding 20, gap 16 and a soft shadow. Pills have `min-height: 44px`, `padding: 0 16px`, a 2 px border (dawn when selected, else `border`), weight 600/500, and a 16 px check in `dawn-text`. The categories hint is 14/20 muted. The rows card has radius 22 and rows with `min-height: 60px; padding: 8px 16px; gap: 12px`, a `CategorySwatch size="sm"` and a switch. The links card has rows with `min-height: 64px; padding: 10px 16px`, a 36 px `muted` disc with a 16 px icon (lucide `bell`, `rotate-ccw`), a 16/24 500 title, a 14/20 muted subtitle, and a chevron on the reminders row only. The board notes that "Show optional items" was removed.
   - `DC/SetPreferencesNL.dc.html`: a wrapper with `lang="nl"`.
   - `DC/SetBodyClock.dc.html`: 375 × 3700, prop `theme`. Readiness has three states: blocked, incomplete and complete. Section cards have radius 26 and padding 20, with Fraunces 20/28 headings. A missing-question box has `padding: 12px; margin: 0 -12px; border-radius: 22px`, a dawn-mix border at 40 % and fill at 10 %. Time tiles have `min-height: 84px; padding: 14px 16px; radius 22; background surface-2`, a 14/20 muted label and a 24/32 600 tabular value. Drink rows have `min-height: 60px; padding: 8px 8px 8px 16px; gap 4`, with outline touch buttons and a 64 px count. The "Saved" status is 12/16 muted with a 14 px dawn-text check.
   - `DC/DarkSetBodyClock.dc.html`: a wrapper with `theme="dark"`.
   - `DC/PickerTime.dc.html`: the time sheet that the bedtime tiles link to. Its title is Fraunces 24/32, its subtitle 14/20 muted, and its footnote "Steps of 5 minutes.".
   - **The body-clock board's questions are partly superseded** (§4).
4. **iOS references** (`I/` at `f391a1fa`). Port the behaviour from these files and keep the Swift type and test names.
   - `plan.md:1993` "17h: Settings pages" (Body clock and Preferences bullets; the replay follow-ups of 2026-11-09; the save-feedback follow-up).
   - `plan.md:2040` "Settings board fidelity (2026-11-09)" (the Body clock and Preferences bullets: backend age bands, a single `light_device`, boolean sensitivity, readiness = required answers plus invalid sections; Preferences has no extra saved row).
   - `plan.md:2477` "No health data — server Phase 19 (user, 2026-11-10)", which includes the **explicit design exception**: the two preference switches and the notices are built from the existing components without a board.
   - `Docs/no-health-data.md` (the copy provenance table; caffeine exclusion goes in Caffeine and movement breaks in Safety checks; notices are text, and no answer is stored for them).
   - `Kit/Settings/SettingsPreferencesView.swift` (221 lines; `SettingsLanguageView` from l.168 is 10a's), `SettingsPreferencesModel.swift` (75), `SettingsDevicePreferences.swift` (28).
   - `Kit/Settings/SettingsBodyClockView.swift` (475), `SettingsBodyClockModel.swift` (186), `SettingsClockPicker.swift` (41), `SettingsPagesModel.swift` (144), `SettingsBoardComponents.swift` (`SettingsBadge` l.86, `SettingsEyebrow` l.97, `SettingsError` l.118, `SettingsRadioGroup` l.134).
   - `Kit/AppCore/Contracts/ProfilePreferences.swift` (`bodyClock` l.18, `readiness` l.60).
   - `I/App/Debug/SettingsBoardCaptureFixture.swift` (the `bodyMissing`, `bodyAdjusted` and `bodyExcluded` states) and `I/App/AppTabs.swift:246–262` (`CF_SETTINGS_ROUTE`).
   - Tests: `Packages/ChronoFlowKit/Tests/SettingsTests/SettingsBodyClockTests.swift` (9 tests: 6 in Task 1, 1 in Task 4, 2 in Task 9), `SettingsPreferencesTests.swift` (7 functions: `groupsPreserve…` and `localFailure…` in Task 3, `offlineChangesRemainPendingDuringPull` in Task 4, `replaySkipAndDone…` and `replayTwice…` in Task 8; `localLanguageAndThemePersist…` is 10a's language port, whose theme half Task 6 covers; `appLanguagePreservesThePhonesHourOverride…` is not portable, OQ-10a-9) and `SettingsProfileTests.swift` (where `SettingsPageFixture` is defined, l.10); `I/AppUITests/BodyClockPrivacyScreenTests.swift`.
   - Copy: `Kit/Settings/Resources/Settings.xcstrings` and `Kit/Reminders/Resources/Reminders.xcstrings` ("Connectivity & reminders").
5. **API** (`W/` at `e36368ec`). Reach it only through 4b/4d, and never call it in tests.
   - `PATCH /v1/account/preferences` takes `{time_format?: "12"|"24", hidden_categories?: string[] (the complete list), show_optional_items?}` and returns 200 with `ApiAccountSnapshot`. A bad value gets 422 "Choose one of the available options.". **Never send `show_optional_items`** (A24).
   - `PATCH /v1/account/body-clock` takes a snake_case partial diff of the 20 `ProfilePreferences.requestFields` (`W app/Http/Requests/Settings/BodyClockRules.php:18-41`); `null` clears a field. It returns 200 with the snapshot. **The server validates the merged section**, so iOS always sends the whole section (every field of the group, with `null` for an unset one). Legacy health keys are ignored by PATCH; never send them. `blueBlockerMdfd` is a **string** in `BodyClockAnswersView` (`W resources/js/types/App/Data/Settings/index.ts:25`).
   - `GET /v1/account` → `settings.preferences {hiddenCategories, showOptionalItems, timeFormat, language}`, `settings.bodyClock`, `settings.sleepAnswered` and `readiness {variant: needs-answers|excluded|adjusted|ready, missing: [{question}], exclusions: CopyLine[], adjustments: CopyLine[], invalidSections: string[]}`. 4d stores these as settings sections `preferences`, `bodyClock`, `sleepAnswered` and `readiness`, with camelCase keys.

## 3. Out of scope (deferred, with owner)

| Item | Owner / reason |
|---|---|
| The Settings home rows, their subtitles, and the Body clock "{{n}} to answer" home badge | 10a (`SettingsHomeState`). This phase builds the question-level "Needs an answer" badges only. |
| Language page and its toast | 10a. |
| The refused-change toast ("Couldn't save your sleep times, so we undid the change.") and the saved/failed snackbars | 10a's shared states (OQ-A5). Preferences and Body clock show no snackbar of their own; they show the iOS `SettingsError` line (§5.4, §5.5). |
| The Connectivity & reminders page, permission handling, and the row subtitle "On, {{minutes}} min ahead · works offline" / "Off · works offline" | 12c (it needs 12b's permission state). In 10b the row has **no subtitle**, as on iOS whenever permission or reminders are unknown (`SettingsPreferencesView.swift:155–163`), and it pushes `Route.Reminders`, which stays a seam. |
| The walkthrough screens and the replay entry | 8 (built). This phase only pushes the replay. |
| Hiding categories on the timeline and the 12/24 h rendering of plans | 6b/6c/7 already read `PlanPreferences` from the stored section, so a change here shows up there through the settings flow. |
| The "Show optional items" switch and `show_optional_items` | Never: removed on the board and by A24. |
| The board's medical questions (pregnancy, bipolar, eyes, blood clots, oral contraception, "told to avoid caffeine"), its age bands 18–29/30–44/45–59/60+, the separate light box / visor switches, and the third sensitivity answer "Not sure" | Never: superseded by no-health-data and the backend schema (§4). |
| Reminder flags (`light`, `avoid`, `caffeine`, `winddown`, `lead_min`) | 12c. |
| Live Activity, practice mode, data download | Never (A24). |
| A wide-window variant beyond 10a's 560 dp column | No board (OQ-A37). |

## 4. Settled values that override the raw boards [F]

The conflict order is a user decision, then iOS at the pin, then the boards. Build the right-hand column.

| Board says | Build | Source |
|---|---|---|
| Large title in Fraunces (`ios-largetitle`) | 10a's `SettingsPage`: `CfLargeTopAppBar`, system font bold, collapsing | plan l.137 (user, 2026-11-10) |
| Age pills 18–29 / 30–44 / 45–59 / 60 or over | **Under 18 / 18–64 / 65 or older** → `age_band` `under_18`, `18_64`, `65_plus` | fidelity 2026-11-09; `BodyClockRules` |
| Light: separate "Light box" and "Visor or light glasses" yes/no questions | One choice, "Do you have a light box, light visor or light glasses?", with None / Light box or visor / Light glasses → `light_device` `none`, `box`, `glasses`; then the eye-caution caption | `SettingsBodyClockView.swift:173–185` |
| Caffeine sensitivity Yes / No / Not sure | Yes / No (bool `caffeine_sensitive`) | fidelity 2026-11-09 |
| "Have you been told to avoid caffeine?" and "Do you take oral contraception?" | Removed. Add the switch "Leave caffeine out of my plan" (hint "Your plan won't suggest any caffeine.") → `exclude_caffeine` | no-health-data, `SettingsBodyClockView.swift:119` |
| Section "Health checks" with the caption "Required. Bright light and caffeine timing aren't safe for everyone…" and five medical questions | **"Safety checks"** (server section `health`): the shift-work yes/no, the switch "Add movement breaks during long-flight sleep" with its long hint, then two captions (audience notice, bipolar notice) | `SettingsBodyClockView.swift:203–222`; `Docs/no-health-data.md` |
| Readiness "blocked" with "Based on your health answers…" | variant `excluded`: "No plan available" plus each server `exclusions[].key` line | `SettingsBodyClockView.swift:388–407` |
| Readiness "complete" subtitle "You can build a plan for any trip." | "Your age range and shift-work answer are saved. You can add optional details below."; the `adjusted` variant has its own title and the `adjustments[].key` lines | `:442–471` |
| Progress over 9 required answers | `total = 2 + invalidSections.size`; `missing = missing.size + invalidSections.size`; `done = max(0, total − missing)` | `:411–412` |
| "Saved" shown on Caffeine only | Shown by the heading of every section whose latest edit saved | `:237–252`; 17h save-feedback follow-up |
| Category name nl "Slaap" | "Slapen" (the catalog value) | `Settings.xcstrings` |
| Category switch track `var(--primary)` | `CfSwitch` (dawn track; plan "Material mapping") | A9; OQ-10b-6 |
| Bedtime picker: an iOS wheel (`PickerTime`) | `CfTimePickerSheet` (M3 `TimePicker` dial in a sheet), rounded to 5 min on Done, with the footnote "Steps of 5 minutes." | OQ-A8; §5.7 |
| Readiness `aria-live` | `liveRegion = Polite` on the readiness card | board |

## 5. Spec

### 5.1 Copy [F `Settings.xcstrings`, `Reminders.xcstrings`]

Every string below is already in the iOS catalogs, so `make strings` emits it. Look up each resource name in `tools/l10n/names.lock.json` (table `settings_…`, except "Connectivity & reminders", which is `reminders_…`). **This phase adds no new copy**, except `android_wizard_time_sheet_steps` if 9e has not landed yet (§5.7). Check the nl values against this table:

| en (key) | nl |
|---|---|
| Preferences / Body clock | Voorkeuren / Biologische klok |
| Appearance / Theme / System / Light / Dark | Weergave / Thema / Systeem / Licht / Donker |
| Time format / 24-hour (14:30) / 12-hour (2:30 PM) | Tijdnotatie / 24-uursnotatie (14:30) / 12-uursnotatie (2:30 PM) |
| Timeline categories / Hide a category to keep your timeline calmer. Its steps stay in your plan. | Tijdlijncategorieën / Verberg een categorie om je tijdlijn rustiger te maken. De stappen blijven gewoon in je plan. |
| Sleep / Light / Caffeine / Travel | Slapen / Licht / Cafeïne / Reizen |
| Connectivity & reminders (Reminders table) | Verbinding en herinneringen |
| Replay walkthrough / The short tour from your first trip | Rondleiding opnieuw bekijken / De korte uitleg die je bij je eerste reis zag |
| We couldn't save your change. Please try again. | Je wijziging kon niet worden opgeslagen. Probeer het opnieuw. |
| Every plan is built from these answers. Changes save automatically. | Elk plan is gebaseerd op deze antwoorden. Wijzigingen worden automatisch opgeslagen. |
| About you / Sleep schedule / Caffeine / Light equipment / Safety checks / Saved | Over jou / Slaapritme / Cafeïne / Lichtmiddelen / Veiligheidsvragen / Opgeslagen |
| When do you naturally feel most awake? | Wanneer voel je je van nature het meest wakker? |
| Early bird · You wake early without an alarm and fade in the evening. | Vroege vogel · Je wordt vroeg wakker zonder wekker en bent in de avond moe. |
| In between · Your energy follows a normal working day. | Ertussenin · Je energie volgt een gewone werkdag. |
| Night owl · You come alive late and find early starts hard. | Nachtuil · Je komt laat op gang en vindt vroeg opstaan lastig. |
| Not sure · We use your sleep times instead. | Weet ik niet · We gebruiken dan je slaaptijden. |
| Your age / Under 18 / 18–64 / 65 or older | Je leeftijd / Jonger dan 18 / 18–64 / 65 of ouder |
| On a normal work or school day. / Usual bedtime / Usual wake time / Not set | Op een normale werk- of schooldag. / Gewone bedtijd / Gewone opstaantijd / Niet ingesteld |
| Do your free days follow the same schedule? / Yes / No / On free days / Bedtime / Wake time | Volg je op vrije dagen hetzelfde schema? / Ja / Nee / Op vrije dagen / Bedtijd / Tijd van opstaan |
| Free-day bedtime / Free-day wake time (sheet titles) | Bedtijd op vrije dagen / Wektijd op vrije dagen |
| Do you usually drink caffeine? / What do you drink on a normal day? | Drink je normaal cafeïne? / Wat drink je op een gewone dag? |
| Coffee / Tea / Soft drinks / Energy drinks / {{count}} a day | Koffie / Thee / Frisdrank / Energiedrank / {{count}} per dag |
| More {{drink}} / Fewer {{drink}} (accessibility labels) | Meer {{drink}} / Minder {{drink}} |
| Does caffeine affect you strongly? · For example, one cup keeps you awake or makes you jittery. | Heeft cafeïne een sterk effect op je? · Bijvoorbeeld als één kop je wakker houdt of onrustig maakt. |
| Leave caffeine out of my plan · Your plan won't suggest any caffeine. | Laat cafeïne weg uit mijn plan · Je plan stelt geen cafeïne voor. |
| Optional. If you have any of these, your plan can use them. | Optioneel. Als je deze hulpmiddelen hebt, kan je plan ze gebruiken. |
| Do you have a light box, light visor or light glasses? / None / Light box or visor / Light glasses | Heb je een lichtbox, lichtvizier of lichtbril? / Geen / Lichtbox of lichtvizier / Lichtbril |
| Blue-blocking glasses / Yes, rating unknown / Yes, I know the rating | Blauwlichtfilterbril / Ja, waarde onbekend / Ja, ik ken de beoordeling |
| Rating (mDFD) · Melanopic daylight filtering density. It's on the box or the maker's website. | Waarde (mDFD) · Melanopische daglichtfilterdichtheid. Je vindt dit op de verpakking of de website van de fabrikant. |
| In the past 14 days, have you worked night or rotating shifts, or had a very irregular sleep schedule? | Heb je de afgelopen 14 dagen in nacht- of wisseldiensten gewerkt, of een heel onregelmatig slaapritme gehad? |
| Add movement breaks during long-flight sleep (+ hint, eye caution, audience and bipolar notices: exact keys in `Docs/no-health-data.md`) | from the catalog |
| Needs an answer / No plan available / Almost ready / Go to the first one | Nog te beantwoorden / Geen plan beschikbaar / Bijna klaar / Ga naar de eerste |
| We need 1 more answer before we can build a plan / We need {{count}} more answers before we can build a plan (**two keys, not a plural**) | We hebben nog 1 antwoord nodig… / We hebben nog {{count}} antwoorden nodig… |
| {{done}}/{{total}} done | {{done}}/{{total}} gedaan |
| Required answers complete / Your plan is adjusted for your preferences and age range / Your age range and shift-work answer are saved. You can add optional details below. | from the catalog |
| Choose one of the available options. / Check your bedtime and wake-up time. / Enter a rating between 0.1 and 9.9. / Add at least one drink to save this answer. | Kies een van de beschikbare opties. / Controleer je bedtijd en opstatijd. / Vul een waarde tussen 0,1 en 9,9 in. / Voeg minstens één drankje toe om dit antwoord op te slaan. |
| Cancel / Done | Annuleren / Klaar |

- The research's "Enter a valid time." is **not** in the catalog, and iOS never shows it. Do not add it.
- Validation errors and readiness lines are **English keys at runtime**. Resolve them with `CopyIndex.find(CopyTable.Settings, key)`. An unknown key renders the raw key, which is the iOS fallback `value: key` (OQ-10b-12).
- `{{drink}}` is the localized drink title, lower-cased with the configuration locale (iOS `t(title).lowercased()`).

### 5.2 `BodyClockGroup` and `SettingsBodyClockModel` (`…/settings/bodyclock/SettingsBodyClockModel.kt`, pure Kotlin) [F `SettingsBodyClockModel.swift`]

```kotlin
enum class BodyClockGroup(val raw: String) {
    About("about"), Sleep("sleep"), Caffeine("caffeine"), Light("light"), Health("health");
    val title: String            // English key: "About you", "Sleep schedule", "Caffeine", "Light equipment", "Safety checks"
    val fields: List<String>     // camelCase, exactly:
    // About:    chronotype, ageBand
    // Sleep:    habitualBed, habitualWake, freeDayStatus, freeDayBed, freeDayWake
    // Caffeine: caffeineHabit, cupsFilterCoffee, cupsTea, cupsCola, cupsEnergyDrink, caffeineSensitive, caffeineNonUserOptIn, excludeCaffeine
    // Light:    lightDevice, blueBlockers, blueBlockerMdfd
    // Health:   shiftWork, flightMovementBreaks
    companion object { fun requestKey(field: String): String }   // each upper-case letter → "_" + lower-case: cupsFilterCoffee → cups_filter_coffee
}

class SettingsBodyClockModel(answers: JsonObject, private val save: suspend (JsonObject) -> Unit) {
    data class State(val answers: JsonObject, val errors: Map<String, String>,
                     val saved: Set<BodyClockGroup>, val failure: Boolean)
    val state: StateFlow<State>
    fun receive(value: JsonObject)
    suspend fun change(group: BodyClockGroup, patch: Map<String, JsonElement>)
    companion object {
        fun validate(group: BodyClockGroup, answers: JsonObject): Map<String, String>
        fun sleepMinutes(bed: String?, wake: String?): Int?
    }
}
```

- **Init:** `answers = ProfilePreferences.bodyClock(answers)` (0d). This sets `excludeCaffeine` and `flightMovementBreaks` to `false` when absent.
- **`receive(value)`:** for each `(key, entry)` in `edits`, drop the entry if `value[key] == entry`. Then `answers = ProfilePreferences.bodyClock(value)`, and lay the remaining `edits` over it. Compare JSON with `JsonElement` equality: an absent key (`null`) is **not** equal to `JsonNull`, as in Swift.
- **`change(group, patch)`:**
  1. If `patch.keys ⊄ group.fields`, return with no state change (this drops `pregnancy` and the other legacy keys).
  2. Write each value into `answers` and `edits`, then `normalize(group)`.
  3. `revision[group] += 1`; remove `group` from `saved`; drop the errors whose keys are in `group.fields`; `invalid = validate(group, answers)`; merge `invalid` into `errors`. If `invalid` is non-empty, publish the state and **return** (iOS `guard invalid.isEmpty`, l.70–72): nothing is saved, and the draft stays.
  4. Body = a `JsonObject` with **every** field of the group: `requestKey(field) to (answers[field] ?: JsonNull)`.
  5. Publish the state, then save under a **FIFO `Mutex`** (`kotlinx.coroutines.sync.Mutex` is fair): `mutex.withLock { save(body) }`.
     - Success: add `group` to `saved` only if `revision[group]` still equals this call's revision; set `failure = false`.
     - Wrap the save in `settingsAttempt(failed = { failure = true }, cancelled = {})` (refresh R5). A foreign cancellation, including 4a's `StaleSessionException`, is silent and sets no failure, as iOS `catch is CancellationError {}`.
     - Never use `runCatching`.
- **`normalize(group)`** sets each value in both `answers` and `edits`:
  - About: if `ageBand == "unanswered"`, set it to `null`.
  - Caffeine: if `caffeineHabit == "unknown"`, set it to `null`. Then, for each of the 4 cups keys, if `caffeineHabit != "cups"` or the value is the number 0, set it to `null`.
  - Sleep: if `freeDayStatus != "declared"`, set `freeDayBed` and `freeDayWake` to `null`.
  - Light: if `blueBlockers != "rated"`, set `blueBlockerMdfd` to `null`.
  - Every group: in `answers["invalidDrafts"]` (an object; absent means `{}`), remove `requestKey(key)` for every group field with an edit, and store the object back.
- **`validate`** follows `SettingsBodyClockModel.swift:115–172` line by line:
  - Choices: `chronotype ∈ {early, intermediate, late}`, `ageBand ∈ {under_18, 18_64, 65_plus}`, `freeDayStatus ∈ {same_as_workdays, declared, unconfirmed}`, `caffeineHabit ∈ {cups, non_user}`, `lightDevice ∈ {none, box, glasses}`, `blueBlockers ∈ {no, unrated, rated}`. A non-null value outside its set gets "Choose one of the available options.".
  - Sleep: the pair `(habitualBed, habitualWake)`, plus `(freeDayBed, freeDayWake)` when `freeDayStatus == "declared"`. If `sleepMinutes` is `null` or outside `180..840`, both keys get "Check your bedtime and wake-up time.".
  - Caffeine: every cups value is absent, `JsonNull`, or a JSON **number** that is a whole number in `0..20`. In Kotlin a number is a `JsonPrimitive` with `!isString` **and** `doubleOrNull != null` (a boolean `true` is also `!isString`, so `!isString` alone is wrong); whole = `d == floor(d)`. Otherwise `cupsFilterCoffee` gets "Choose one of the available options.". If they are valid but `caffeineHabit == "cups"` and no count is above 0, `cupsFilterCoffee` gets "Add at least one drink to save this answer.".
  - Light: when `blueBlockers == "rated"`, the string `blueBlockerMdfd` must match `^\d+(?:\.\d)?$` and lie in `0.1..9.9`. Otherwise it gets "Enter a rating between 0.1 and 9.9.".
  - Every group field whose `requestKey` is in `invalidDrafts` gets "Check your bedtime and wake-up time." in Sleep, else "Choose one of the available options.".
- **`sleepMinutes`:** each value must match `^(?:[01]\d|2[0-3]):[0-5]\d$`. The result is `(wake − bed + 1440) % 1440`.
- Values are read with `as? JsonPrimitive` and `takeIf { it.isString }`, never `.jsonPrimitive`, which throws (0d rule).

### 5.3 Readiness and choice helpers (`…/settings/bodyclock/BodyClockReadiness.kt`, `BodyClockChoice.kt`, pure) [F `SettingsBodyClockView.swift:263–278, 360–377, 380–474`]

```kotlin
sealed interface BodyClockReadiness {
    data class NeedsAnswers(val missingCount: Int, val total: Int, val done: Int) : BodyClockReadiness
    data class Excluded(val lineKeys: List<String>) : BodyClockReadiness
    data class Ready(val adjusted: Boolean, val adjustmentKeys: List<String>) : BodyClockReadiness
    companion object {
        fun of(readiness: JsonObject?): BodyClockReadiness?
        fun missingFields(readiness: JsonObject?): List<String>
        fun firstMissing(readiness: JsonObject?): String
    }
}
object BodyClockChoice { fun patch(answers: JsonObject, key: String, id: String): Map<String, JsonElement> }
```

- `of`:
  - `null` (no stored `readiness`, for example while a bodyClock row is pending) → `null`, and no card is shown.
  - `variant == "excluded"` → `Excluded`, keeping the `key` of each `exclusions` item that is an object with a string `key`.
  - `"needs-answers"` → `missingCount = missing.size + invalidSections.size`, `total = 2 + invalidSections.size`, `done = max(0, total − missingCount)`.
  - `"ready"` → `Ready(false, [])`.
  - `"adjusted"` → `Ready(true, adjustments keys)`.
  - Any other variant → `null`.
- `missingFields`: for each item of `missing`, take `question` (a missing or null value counts as `"age_band"`), then lower-camel it (`shift_work` → `shiftWork`).
- `firstMissing`: the first of `missingFields`. Otherwise, the first field of the group named by `invalidSections[0]`. Otherwise `"ageBand"`.
- `BodyClockChoice.patch`:
  - `id == ""` → `{key: null}` ("Not sure").
  - Otherwise `{key: id}`.
  - When `key == "freeDayStatus"` and `id == "declared"`, also add `freeDayBed` from the existing free-day value, falling back to `habitualBed` when absent **or JSON null**, then to `JsonNull`; do the same for `freeDayWake` (iOS l.268–275 explicitly repeats the fallback for `.null`).
- Yes/no questions map `"yes"`/`"no"` to `JsonPrimitive(true/false)`. The selected id is `"yes"`/`"no"` for a boolean, else `""`.

### 5.4 `SettingsTimelineGroup` and `SettingsPreferencesModel` (`…/settings/preferences/SettingsPreferencesModel.kt`, pure) [F `SettingsPreferencesModel.swift`]

- `SettingsTimelineGroup(title, types)`, with `all` in this order:
  - Sleep `[sleep, nap, rest]`;
  - Light `[seek_light, avoid_light, light_ok, light_neutral]`;
  - Caffeine `[caffeine_serving, caffeine_routine, caffeine_ok, no_caffeine]`;
  - Travel `[flight, layover, transfer, movement_break, zone_change]`.

  Types are **raw strings**: unknown future categories in `hiddenCategories` must survive.
- `SettingsPreferencesModel(value: JsonObject, save: suspend (JsonObject) -> Unit)`, with `state: StateFlow<State(value, saved, failure)>`, `receive(value)` (the same edit overlay as §5.2), `hidden: List<String>`, `shown(group) = !group.types.all { it in hidden }`, `suspend fun setTimeFormat(format: String)` and `suspend fun setShown(shown: Boolean, group)`.
- `setTimeFormat`: a value outside `{"12", "24"}` sets `failure = true` and saves nothing. Otherwise it calls `change("timeFormat", "12"|"24", request = "time_format")`.
- `setShown`: `next = hidden − group.types`, plus `group.types` appended when `!shown`. Then `change("hiddenCategories", JsonArray(next), "hidden_categories")`. Order: the existing entries in their order, then the group's types.
- `change(key, entry, request)`: set `value[key]` and `edits[key]`; `saved = false`; then, FIFO under a `Mutex`, `save(JsonObject(mapOf(request to entry)))`. **Only the changed key** is sent. Success sets `saved = true, failure = false`. The save runs in `settingsAttempt(failed = { failure = true }, cancelled = {})`, so a foreign cancellation is silent (refresh R5).
- The selected time format defaults to `"24"` when absent (iOS `?? "24"`).
- **No walkthrough model here:** on Android, Phase 8's nav entry gives every replay a fresh `WalkthroughViewModel`. This replaces iOS `startWalkthroughReplay()` (§5.8).

### 5.5 Theme (`…/datastore/DeviceAppearance.kt`, `:core:model` `ThemeChoice`, `:app` root) [F `SettingsDevicePreferences.swift`; plan l.354; R]

- `fun ThemeChoice.resolve(systemDark: Boolean): Boolean` in `:core:model` `config`: `System` → `systemDark`, `Light` → `false`, `Dark` → `true`. Add it only if 0e did not.
- `fun interface NightModeSink { fun set(mode: Int) }`. The production implementation `UiModeNightModeSink(context)` calls `context.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)`, annotated `@RequiresApi(31)`.
- `class DeviceAppearance(prefs: DevicePreferences, sink: NightModeSink, io: CoroutineDispatcher, sdkInt: Int = Build.VERSION.SDK_INT)`:
  - `suspend fun setTheme(choice: ThemeChoice)`:
    1. `prefs.setTheme(choice, sdkInt)`, which writes DataStore and, below 31, `device-config` (4a);
    2. then, if `sdkInt >= 31`, `withContext(io) { sink.set(mode) }` with `System` → `MODE_NIGHT_AUTO`, `Light` → `MODE_NIGHT_NO` and `Dark` → `MODE_NIGHT_YES`.
  - `val theme: Flow<ThemeChoice> = prefs.theme`.
  - 10a's language applier (`AppLanguageApplier` in `:app`, behind `LanguageDependencies`) is language-only; leave it alone and keep `DeviceAppearance` separate.
- **Feature seam** (mirrors 10a's `LanguageDependencies`, because the theme is device-level and works without a session): `interface ThemeDependencies { val theme: Flow<ThemeChoice>; suspend fun setTheme(choice: ThemeChoice) }` plus `object ThemeDependenciesKey : CreationExtras.Key<ThemeDependencies>` in `…/settings/preferences/`. `:app` implements it with `DeviceAppearance` from `AppGraph` and passes it in the ViewModel creation extras the way 10a passes `SettingsDependenciesKey`.
- **Root wiring** (`MainActivity`'s root composable, Phase 2 l.236):
  - Replace `ChronoFlowTheme(reduceMotion = …)` with `ChronoFlowTheme(darkTheme = theme.resolve(isSystemInDarkTheme()), reduceMotion = …)`.
  - `theme` is a `mutableStateOf(initialTheme)`:
    - `initialTheme` = the `theme` of the `DeviceConfig` that `ConfigurationPolicy` read in `attachBaseContext` when `sdkInt < 31`. Expose that config from the activity as an in-memory value; do not read the file again.
    - Otherwise `initialTheme = ThemeChoice.System`. On 31+ the app night mode is already in `Configuration.uiMode`, so `isSystemInDarkTheme()` is right from frame 0.
  - Start collecting `DeviceAppearance.theme` only after the first frame: `LaunchedEffect(Unit) { withFrameNanos { }; appearance.theme.collect { theme = it } }`. Before the overlay's first frame there is no DataStore read (A11; iOS initializes theme "only when the root mounts after the splash commit").
  - Do **not** call `reconcileDeviceConfig()` here: 10a already calls it once when the Settings root first composes (10a §5.9, Task 10). Grep it; only if the merged code has no call, add one to this effect before collecting (4a l.516).
- **System bars** follow the resolved theme once the launch hand-off has ended. Find where the merged `MainActivity` calls `enableEdgeToEdge` (3b/3c own the launch and Welcome bar styles; keep their rules). After the hand-off, call `enableEdgeToEdge(SystemBarStyle.auto(…, detectDarkMode = { resolvedDark }))` again whenever `resolvedDark` changes, from a `LaunchedEffect(resolvedDark)` in the activity. Never set the bar appearance from feature code.
- `uiMode` is in `configChanges` (A8), so on 31+ the `UiModeManager` call does **not** recreate the activity. Compose sees the new `LocalConfiguration`.
- The system splash on API 29–30 keeps the system appearance (plan `### Appearance`). Content and system bars follow the choice (OQ-10b-13).

### 5.6 Components (`…/settings/components/`; designed surfaces, values from tokens only) [F `SettingsBoardComponents.swift`, `SettingsBodyClockView.swift`]

Reuse the Phase 2 primitives. Build only what is missing:

- **Pill groups:** `CfPillGroup` with a title `Text` above it (`settingsRow`, 16/24 500, gap `space2` = 8). Option tags are `"$testTag.$id"`.
- **Choice cards: reuse 10a's `SettingsRadioCards` / `SettingsChoiceCard`** (10a §5.9, built for Language; iOS `SettingsRadioGroup(cards: true)`, `SettingsBoardComponents.swift:134–207`). Do not build a second one. The chronotype group needs a title `Text` above the cards (`settingsRow`, gap 8), a hint per card, and option tags `"$testTag.$id"`. If 10a's API lacks any of these, add it as an optional parameter with a default that keeps Language unchanged. For reference, the look 10a ports:
  - a `Column(selectableGroup)` with gap 8. Each card is a `Row(top)` with gap `space3` (12), padding 16/16, min height `pillHeight` (44) and a ≥ 48 dp target, radius `radius2Xl` (22), fill `card`, and a 2 dp border (`dawn` when selected, else `border`);
  - the title in `settingsStrong` and the hint in `screenBody` `mutedForeground`, with gap 2;
  - a trailing 20 dp (`space5`) radio with top padding 4 and a 2 dp stroke. Selected: a `dawn` fill with a 12 dp `check` in `dawnForeground`. Unselected: the stroke is `mutedForeground` at α `AuthTokens.disabledOpacity` (0.5; 10a's merged value);
  - semantics `Role.RadioButton` and `selected`.
- **`SettingsQuestion(title, missing, hint?, error?, anchorKey, content)`** [F `:300–322`]:
  - A column with gap 8: the title (`settingsRow`), then `CfBadge("Needs an answer", Dawn)` when missing, then the hint (`screenBody` muted), the content, and the error.
  - When missing, draw a box with radius `radius2Xl`, fill `settingsWarningBackground` and a 1 dp `settingsWarningBorder` border, with inner padding `space3` (12). The box is **12 dp wider on each side** than the column, so the content stays aligned with its neighbours. Do this in a `Modifier.layout` that measures with `maxWidth + 24 dp` and places at `x = −12 dp` (measured sizes stay in layout; AGENTS trap).
- **`SettingsEyebrow(text, dawn = false)`** (10a has none) [F `SettingsBoardComponents.swift:97–105`]: the text upper-cased with the configuration locale, `settingsEyebrow` in `mutedForeground`, or `screenEyebrow` in `dawnText` when `dawn`; `heading()`.
- **`SettingsError(text)`** (10a has none) [F `:118–124`]: a `Row` with a 16 dp lucide `circle-alert` and the text in `screenBody` `destructive`, wrapping.
- **`SettingsSavedLabel`**: a 14 dp `check` in `dawnText`, gap 4, then "Saved" in `screenMeta` `mutedForeground`. One merged node. Tag `settings.bodyClock.saved.<group.raw>`.
- **`SettingsTimeTile(label, value, onClick, testTag)`** [F `:323–359`]:
  - Layout: a column with gap 4; padding horizontal 16 and vertical `timeVertical` (14); min height `timeHeight` (84); fill `surface2`; radius 22; 1 dp `border`.
  - Text: the label in `screenBody` muted; the value in `settingsTime` (Figtree 600 24/32) with `FontFeature tnum`. The value is **the raw stored `HH:mm`**, whatever the time format, as on iOS; an unset value shows "Not set".
  - Semantics: `Role.Button`, merged.
  - **The tile pair** is a `Layout` with spacing 12. It uses 2 columns when `(maxWidth − 12) / 2 ≥ 128 dp` (`space16 × 2`), and stacks otherwise (iOS adaptive grid, min 128). Each tile's error sits under that tile.
- **`SettingsDrinkCounter`** [F `:124–172`]:
  - Layout: a column with gap 8, a title, then a list with radius 22 and a 1 dp `border`, rows split by `CfDivider`.
  - Each row: min height `counterHeight` (60); padding start 16, end 8, vertical 8; gap 4. It holds the label (weight 1), a minus button, the count "{{count}} a day" (`screenBody`, tabular, min width `space16` = 64, centred, `liveRegion = Polite`), and a plus button.
  - Buttons: 44 dp (`AuthTokens.touch`) circles with a 1 dp `border` stroke and a 16 dp lucide `minus`/`plus`, inside a ≥ 48 dp target. Minus is disabled at 0, plus at ≥ 20. `contentDescription` is "Fewer {{drink}}" / "More {{drink}}".
  - Tags: the row `settings.bodyClock.<key>`; the buttons `.fewer` and `.more`.
- **`SettingsPreferenceSwitch(title, hint, checked, onChange, testTag)`**: a `Row` with a `Column(title settingsRow, hint screenBody muted, gap 4)` and a `CfSwitch`, made `cfToggleable` as one node (`Role.Switch`).
- **Readiness cards** (§5.8).
- Glyphs: `bell`, `rotate-ccw`, `check`, `circle-alert`, `triangle-alert`, `minus`, `plus` and `chevron-right` must be in `LucideGlyphs`. Add any missing ones with the Phase 2 icon generator, in the commit that first uses them (`make generators-check`).

### 5.7 Time picker (`…/ds/system/CfTimePickerSheet.kt`; 9e's) [F `SettingsClockPicker.swift`; R OQ-A8]

- **9e owns `CfTimePickerSheet`, `WizardClockRounding` and `android_wizard_time_sheet_steps`** and is in the stack (10b depends on 9e since the 2026-11-16 review): `grep -rn "fun CfTimePickerSheet\|object WizardClockRounding" core` finds both; use them.
- If the grep finds either one missing, stop and report it; never create them here. `:feature:settings` may depend on `:core:wizard` (a JVM core module; A3).
- **Body clock use:**
  - Title: "Usual bedtime" / "Usual wake time" for the habitual keys, "Free-day bedtime" / "Free-day wake time" for the free-day keys.
  - Subtitle: "On a normal work or school day." for the habitual keys, "On free days" for the free-day keys (OQ-10b-2).
  - Footnote: "Steps of 5 minutes."; confirm "Done"; dismiss "Cancel".
  - Initial value: the stored value, else 12:00 (iOS `value ?? "12:00"`).
  - `is24Hour = DateFormat.is24HourFormat(context)`: the phone's hour cycle, as iOS `DatePicker` (A17).
  - Confirm: `rounded = WizardClockRounding.roundToStep(h, m)`, then `change(Sleep, {key: rounded})`. It **always** writes on Done, touched or not (iOS; OQ-10b-3). Dismiss, back and scrim write nothing.
  - The open sheet is VM-held (`BodyClockUiState.timeSheet`), not `rememberSaveable` (refresh R5).

### 5.8 Screens (`…/settings/preferences/PreferencesScreen.kt`, `PreferencesViewModel.kt`; `…/settings/bodyclock/BodyClockScreen.kt`, `BodyClockViewModel.kt`) [F views above; R]

**Dependencies** (extend 10a's seam additively; 10c also edits this file, so add, never reorder):
- Add `suspend fun enqueueSettings(section: SettingsSection, patch: JsonObject)` to 10a's `SettingsDependencies`, and implement it in `:app`'s `SessionSettingsDependencies` exactly like 10a's `enqueueProfile`: if `!isCurrent()` throw 4a's `StaleSessionException` (a `CancellationException`; iOS `SettingsPagesModel.enqueue` throws `CancellationError`, `SettingsPagesModel.swift:96–100`), else `outbox.enqueue(OutboxChange.Settings(section, patch), permit = controller.mutationPermit(snapshot))` on the 4d writer.
- **No `requestSync()` after the enqueue** (iOS calls it; on Android 4d's `SyncLifecycle` observes the outbox and requests a sync for each new row id, 4d §5 "`start()`" and "No producer calls `requestSync`").
- The section map is `deps.settings().map { rows -> rows.associate { it.section to Json.parseToJsonElement(it.value).jsonObject } }` (`SettingsRecord.value` is a JSON string; skip a row that does not parse to an object, as iOS `try?` does). `settingsLoaded` = the first emission arrived; `failed` = the flow threw. Retry (`SettingsStoredState.onRetry`) re-collects the flow and calls `deps.requestSync()` (iOS `retry()`, `SettingsPagesModel.swift:83–90`; 10a's Account does the same).

**ViewModels** (A4: `viewModelFactory { initializer { … } }` with `this[SettingsDependenciesKey]!!`, as 10a):
- `PreferencesViewModel(deps: SettingsDependencies, theme: ThemeDependencies)` and `BodyClockViewModel(deps: SettingsDependencies)`.
- **Model lifetime:** the model is created on the first emission that has the section (`preferences` / `bodyClock`). Every later emission calls `model.receive(section)`. Until the section exists, the page shows 10a's `SettingsStoredState(loaded, failed, onRetry)` (loading skeletons, or "Your account isn't on this phone yet" with Try again).
- **Save closures:**
  - Preferences: `{ patch -> deps.enqueueSettings(SettingsSection.Preferences, patch) }`;
  - Body clock: `{ patch -> deps.enqueueSettings(SettingsSection.BodyClock, patch) }`.
- **State:** expose one immutable UI state per screen (`@Immutable` data classes; lists as `ImmutableList` or `List` with a stable wrapper) through `stateIn(viewModelScope, WhileSubscribed(5_000), …)`. Model calls run in `viewModelScope.launch` on `Dispatchers.Main.immediate`, so the local state updates in the same frame.
- **Theme:** `PreferencesViewModel.theme` = `theme.theme`. `setTheme(id)` = `ThemeChoice.fromRaw(id)?.let { viewModelScope.launch { theme.setTheme(it) } }`.

**Preferences screen** (`SetPreferences`; `SettingsPage(title = R.string.settings_preferences, onBack = holder::back, testTag = "settings.preferences.screen")`, column content, gap 24):
1. `CfCard` (spacing 16):
   - `SettingsEyebrow("Appearance")`;
   - the pill group "Theme": `system`/`light`/`dark` → "System"/"Light"/"Dark", tag `settings.preferences.theme`;
   - the pill group "Time format": `24`/`12` → "24-hour (14:30)"/"12-hour (2:30 PM)", tag `settings.preferences.timeFormat`.
2. A column with gap 12:
   - a column with gap 4: `SettingsEyebrow("Timeline categories")` and the hint (`screenBody` muted);
   - `CfCard(rows = true)` with the 4 groups split by `CfDivider`. Each row: `cfToggleable` (`Role.Switch`), min height `counterHeight` (60), padding 16/8, gap 12, then `CategorySwatch(type, Small)` with Sleep → `sleep`, Light → `seek_light`, Caffeine → `caffeine_serving` and Travel → `flight`; the title (`screenBodyLg`, weight 1); and a `CfSwitch(checked = shown, onCheckedChange = null)`. Tags `settings.preferences.Sleep|Light|Caffeine|Travel` (the iOS ids use the English title).
3. `CfCard(rows = true)`:
   - `CfRow("Connectivity & reminders", subtitle = null, icon = bell, chevron = true, tag = settings.preferences.reminders)` → `holder.push(Route.Reminders)`;
   - `CfDivider`;
   - `CfRow("Replay walkthrough", "The short tour from your first trip", icon = rotate-ccw, chevron = false, tag = settings.preferences.replay)` → `holder.push(Route.Walkthrough(replay = true))`. Phase 8's entry hides the shell, and Skip, Done and back return to Preferences with no write.
4. When `failure`: `SettingsError("We couldn't save your change. Please try again.")`.

There is no saved indicator on this page (fidelity 2026-11-09: the selected values show the result).

**Body clock screen** (`SetBodyClock`; `SettingsPage(title = R.string.settings_body_clock, onBack = holder::back, testTag = "settings.bodyClock.screen")` with its **`LazyListScope`** content, because the page is about 3,700 dp tall; research 07 §13):
- **Items, keyed and with a `contentType`:**
  - `"intro"` (`screenBodyLg` muted);
  - `"readiness"` (only when non-null);
  - `"about"`, `"sleep"`, `"caffeine"`, `"light"`, `"health"`;
  - `"failure"` (only when `failure`).

  Spacing is 24; content padding is 10a's page padding (4/16/40).
- **Section card:** `CfCard` (padding 20, gap 16). The header is a `Row(firstBaseline)` with the title in `screenSectionTitle` (`heading()`), a flexible spacer of at least 8, and `SettingsSavedLabel` when the group is in `saved`.
- **About:**
  - 10a's `SettingsRadioCards` with the title "When do you naturally feel most awake?" with `early` "Early bird", `intermediate` "In between", `late` "Night owl" and `""` "Not sure", each with its hint. Tag `settings.bodyClock.chronotype`; the "Not sure" card's tag is `settings.bodyClock.chronotype.`, with a trailing dot (iOS id; OQ-10b-10).
  - The question "Your age": pills `under_18`, `18_64`, `65_plus`, tag `settings.bodyClock.ageBand`.
- **Sleep:**
  - the caption "On a normal work or school day.";
  - the tile pair `habitualBed` "Usual bedtime" / `habitualWake` "Usual wake time";
  - the question "Do your free days follow the same schedule?" with `same_as_workdays` "Yes" and `declared` "No" (tag `settings.bodyClock.freeDayStatus`);
  - when `freeDayStatus == "declared"`: a column with gap 8, the caption "On free days" and the tile pair `freeDayBed` "Bedtime" / `freeDayWake` "Wake time".
- **Caffeine:**
  - the question "Do you usually drink caffeine?" with `cups` "Yes" and `non_user` "No";
  - when `caffeineHabit == "cups"`: `SettingsDrinkCounter` "What do you drink on a normal day?" with `cupsFilterCoffee` Coffee, `cupsTea` Tea, `cupsCola` Soft drinks and `cupsEnergyDrink` Energy drinks. A tap sends `change(Caffeine, {key: count ± 1})` as a JSON integer. The error `cupsFilterCoffee` sits under the list;
  - the yes/no question "Does caffeine affect you strongly?" with its hint (`caffeineSensitive`);
  - `SettingsPreferenceSwitch` "Leave caffeine out of my plan" / "Your plan won't suggest any caffeine." (`excludeCaffeine`).
- **Light:**
  - a header column with gap 4 and the caption "Optional. If you have any of these, your plan can use them.";
  - the question for `lightDevice`;
  - the eye-caution caption (tag `settings.bodyClock.lightNotice`);
  - the question "Blue-blocking glasses" (`no`, `unrated`, `rated`);
  - when `rated`: `CfTextField` "Rating (mDFD)" with its hint, `KeyboardType.Decimal`, `ImeAction.Done` (clears focus), error `blueBlockerMdfd`, and tag `settings.bodyClock.blueBlockerMdfd`. **Every text change** calls `change(Light, {blueBlockerMdfd: text})` (iOS binding; OQ-10b-14), after replacing `,` with `.` (OQ-10b-4). The field's `TextFieldState` is seeded once from the stored string. It is not a secret, so `rememberTextFieldState` is allowed.
- **Health ("Safety checks"):**
  - the yes/no question for `shiftWork`;
  - `SettingsPreferenceSwitch` "Add movement breaks during long-flight sleep" plus its hint (`flightMovementBreaks`);
  - the audience caption;
  - the bipolar caption (tag `settings.bodyClock.audienceNotice`, as on iOS, which puts the tag on the second caption).
- **Missing badges:** a question shows `missing = key in BodyClockReadiness.missingFields(readiness)`. Only the questions built with `SettingsQuestion` can show it: ageBand, freeDayStatus, caffeineHabit, caffeineSensitive, lightDevice, blueBlockers and shiftWork.
- **Readiness card** (live region polite; tag `settings.bodyClock.readiness`):
  - `NeedsAnswers`: `CfCard` with spacing 12, holding:
    - `SettingsEyebrow("Almost ready", dawn = true)` (`screenEyebrow`, `dawnText`);
    - the title in `screenSectionTitle`: "We need 1 more answer…" when `missingCount == 1`, else "We need {{count}} more answers…";
    - a row with gap 8: a drawn capsule `progressTrackWidth` (80) × `pillGap` (6), `muted` track, `dawn` fill of width `80 × done / total`, hidden from accessibility; then a 12 dp `check` plus "{{done}}/{{total}} done" in `screenMeta` muted, tabular;
    - `CfButton(style = Secondary)` "Go to the first one", in the size whose height is `walkthroughDoneHeight` (40), with a ≥ 48 dp target. Tag `settings.bodyClock.firstMissing`.
  - `Excluded`: a box with padding 20, radius `radius3Xl` (26), fill `settingsWarningBackground` and a 1 dp `settingsWarningBorder` border. Inside, a column with gap 8: a row with an 18 dp `triangle-alert` in `dawnText` and "No plan available" in `settingsStrong`; then each line in `screenBody`.
  - `Ready`: `CfCard`, then a row with gap 12: a 36 dp (`rowDisc`) `dawn` circle holding an 18 dp `check` in `dawnForeground`; the title "Required answers complete" (or the adjusted title) in `settingsStrong`, and the saved sentence in `screenBody` muted. When adjusted, add each adjustment line in `screenBody`.
- **Scrolling:**
  - "Go to the first one" calls `listState.animateScrollToItem(indexOf(group of firstMissing))`, then `bringIntoView()` on the question's `BringIntoViewRequester` (registered per field key).
  - When a group newly enters `saved` (diff against the previous set in a `LaunchedEffect(state.saved)`), call `animateScrollToItem(indexOf(group))`. iOS scrolls the saved heading to the top (NH4.1; OQ-10b-11).
  - Under Reduce Motion, use `scrollToItem` in both cases.
- **Time picker:** see §5.7.
- **Recomposition:** each section item reads only its own slice of the state (a derived `BodyClockSectionState(group, answers subset, errors subset, saved, missing subset)`), so a chronotype tap does not recompose the Caffeine section.

### 5.9 Routing (`:app` / the shell entry provider)

- Replace the seams for `Route.Preferences` and `Route.BodyClock` in `AppShell`'s Settings entries with the two screens. Settings home rows (10a) already push them. `Route.Reminders` stays a `RoutePlaceholder` seam.
- **Flip 10a's `isBuilt` flags** for the `BodyClock` and `Preferences` home rows to `true` (10a §5.4: release shows only built rows; "10b–10d flip their flags"), in the commit of the page that becomes built (Tasks 8 and 9), and update 10a's release-visibility test expectation in the same commit.
- `CF_SETTINGS_ROUTE=preferences` and `CF_SETTINGS_ROUTE=bodyClock` must open the pages. 10a's `SettingsSeedRoute.path` maps "any other → the destination whose `debugName()` matches" (iOS `AppTabs.swift:257–261`), so both should already work; add the two cases to 10a's `ShellNavSettingsSeedTest` and fix the mapping only if they fail.

### 5.10 Seeds (debug only, `app/src/debug/…`) [F `SettingsBoardCaptureFixture.swift`]

`CF_SETTINGS_BOARD_STATE` (gated by `CF_AUTH_TEST_ID`) gains three body states. If 10a added the key, extend its fixture; otherwise add `settingsBoardState: String?` to the 4e seed parse and a `SettingsBoardCaptureFixture.seed(state)` applied to the mock server state of the `demo@example.com` account before sign-in:
- `bodyMissing`: `bodyClock.ageBand = null`, `shiftWork = null`; `readiness = {variant: "needs-answers", missing: [{question: null}, {question: "shift_work"}], invalidSections: []}`.
- `bodyAdjusted`: `ageBand = "65_plus"`, `excludeCaffeine = true`, `flightMovementBreaks = true`; `readiness = ProfilePreferences.readiness({}, bodyClock)`.
- `bodyExcluded`: `shiftWork = true`; `readiness = {variant: "excluded", missing: [], invalidSections: []}`.
- `bodyReady`, or no state: the default seed.

Release builds contain none of this (`testReleaseUnitTest` proves it).

## 6. Tasks

Each task is one commit. Tick the plan box named in a task in that same commit.

### Task 1: `feat(settings): port the body clock model`
- Files: `…/settings/bodyclock/SettingsBodyClockModel.kt` (§5.2).
- Test `feature/settings/src/test/…/bodyclock/SettingsBodyClockTests.kt` (JVM, `runTest(timeout = 60.seconds)`; each case < 1 s). It ports the 6 pure Swift cases **with the same names and values** (the other 3 of the 9 are in Tasks 4 and 9):
  - `invalidSleepStaysInDraftAndDoesNotReachOutboxUntilBothTimesAreValid`;
  - `normalizesDependentValuesAndValidatesCupsAndRating`;
  - `failedLocalSaveRetainsAnswerAndAllowsRetry` (the failing save throws 4a's `StoreFailure.Closed` or the merged equivalent);
  - `completionOfAnOlderSaveDoesNotMarkANewerInvalidDraftAsSaved` (gate the save with two `CompletableDeferred`s, never a delay);
  - `preferencesSaveNonNullableBooleansWithoutMedicalFields` (every body's keys ⊆ `ProfilePreferences.requestFields`; a `pregnancy` patch writes nothing);
  - `sleepDurationChecksClockBoundsAndCrossesMidnight`;
  - plus Android-only `foreignCancellationIsSilent`: a save that throws `StaleSessionException` leaves `failure == false` and no "Saved", and `change` returns normally (refresh R5).

### Task 2: `feat(settings): add body clock readiness and choice rules`
- Files: `BodyClockReadiness.kt`, `BodyClockChoice.kt` (§5.3).
- Test `BodyClockReadinessTests` (JVM):
  - the four variants, plus an unknown variant and `null`;
  - `needs-answers` with 2 missing and 1 invalid section → `missingCount 3, total 3, done 0`;
  - with 1 missing → the single-answer key;
  - `missingFields([{question: null}, {question: "shift_work"}]) == ["ageBand", "shiftWork"]`;
  - `firstMissing` falling back to `invalidSections[0]`'s first field, then to `"ageBand"`;
  - `declaredFreeDaysCopyHabitualTimes`;
  - `notSureClearsChronotype`;
  - yes/no ↔ boolean.

### Task 3: `feat(settings): port the preferences model`
- Files: `…/settings/preferences/SettingsPreferencesModel.kt` (§5.4).
- Test `SettingsPreferencesTests` (JVM). It ports `groupsPreserveUnrelatedAndFutureCategoriesAndOnlySendChangedSetting` and `localFailureKeepsDraftThroughIncomingValuesAndCanRetry` verbatim, and adds `missingTimeFormatReadsAs24`.

### Task 4: `test(data): pin pending protection for settings sections`
- File `core/data/src/test/kotlin/net/chronoflow/core/data/settings/SettingsPendingProtectionTests.kt`, in the same module as 4d's internal `StoreApplication`, using 4d's `SyncStore`.
- It ports `offlineQueueAndPullsPreservePendingValuesInOtherGroups` and `offlineChangesRemainPendingDuringPull`:
  - Enqueue the exact bodies the models emit. The about body is `{chronotype: null, age_band: "65_plus"}`. The sleep body is `{habitual_bed: "23:15", habitual_wake: "07:45", free_day_status: null, free_day_bed: null, free_day_wake: null}`. The preferences body is `{time_format: "12"}`.
  - Flush with an offline source: 4d's `ScriptedSource` throwing `DataFailure.Offline` on `send`.
  - Apply `StoreApplication.setting("bodyClock" | "preferences", <server value>)` inside `database.write`.
  - Assert the stored values, `sleepAnswered == true`, and 2 (or 1) pending rows.
- If 4d already has these exact cases, skip this task and say so.

### Task 5: removed (review 2026-11-16)
- 9e owns the time picker sheet, the rounding and the string (§5.7). This task has no commit; keep the later task numbers.

### Task 6: `feat(settings): apply the theme per device`
- Files: §5.5 (`ThemeChoice.resolve`, `NightModeSink`, `UiModeNightModeSink`, `DeviceAppearance`, `ThemeDependencies` + key, its `:app` implementation and creation-extras wiring, `MainActivity` root and system bars, `AppGraph` registration).
- Tests:
  - `ThemeChoiceResolveTest` (JVM, 6 cases: 3 choices × `systemDark` true/false, one parameterized test).
  - `DeviceAppearanceTest` (Robolectric `@Logic`, `runTest(timeout = 60.seconds)`):
    - `sdk36WritesDataStoreAndMirrorsNightMode`: a recording sink gets `MODE_NIGHT_YES` for Dark, `NO` for Light and `AUTO` for System;
    - `sdk30WritesDeviceConfigAndNeverCallsTheSink` (`device-config` = `language=system\ntheme=dark\n`);
    - `unknownStoredValueReadsAsSystem`;
    - `themePersistsAcrossInstances`: set Dark, build a second `DeviceAppearance` over the same DataStore file, `theme.first() == Dark` (the theme half of iOS `localLanguageAndThemePersistAndUnsupportedPhoneLanguageFallsBackToEnglish`; 10a ported the language half).
  - `RootThemeTest` (Robolectric `@Logic`, `:app`, `GraphOverrides(sessionCipher = SoftwareSessionCipher())`):
    - `darkChoiceRendersDarkTokensWithoutRecreate`: set Dark through `DeviceAppearance`, wait until a root pixel equals `CfDarkColors.background` ±2, and the activity instance is unchanged;
    - `noDataStoreReadBeforeFirstFrame`: a counting DataStore wrapper records 0 reads until the first `withFrameNanos`.

### Task 7: `feat(settings): add body clock and preference components`
- Files: §5.6 (only the missing ones) and any lucide glyphs.
- Tests:
  - `SettingsComponentsTest` (`@Logic`):
    - a choice card's semantics are `Role.RadioButton` + `selected`, and its tag is `<group>.<id>`, including `settings.bodyClock.chronotype.`;
    - drink minus is disabled at 0 and plus at 20, and the labels are "Fewer coffee" / "Meer koffie";
    - a preference switch row toggles as one `Role.Switch` node;
    - the saved label merges into one node.
  - `SettingsComponentsLayoutTest` (`@Layout`, pixels):
    - the missing box fill and border pixels (`settingsWarning*`), with the content's left edge equal to a non-missing sibling's;
    - the time tile pair is 2 columns at `pixel8` and 1 column at `small` + `largeFont`;
    - light and dark.

### Task 8: `feat(settings): build preferences`
- Files: `SettingsDependencies.enqueueSettings` + its `:app` implementation (§5.8 "Dependencies"; first used here, Task 9 reuses it), `SettingsEyebrow`/`SettingsError` if Task 7 did not add them, `PreferencesViewModel.kt`, `PreferencesScreen.kt`, the shell entry and the `Preferences` `isBuilt = true` flip with 10a's visibility test updated (§5.8–§5.9). Tick plan "**Preferences**".
- Tests:
  - `PreferencesViewModelTest` (JVM or `@Logic`):
    - stored `{timeFormat: "24", hiddenCategories: []}`: tap Travel off → one outbox change, `Settings(Preferences, {hidden_categories: [flight, layover, transfer, movement_break, zone_change]})`;
    - choose 12 → `{time_format: "12"}`;
    - `setTheme("dark")` calls `DeviceAppearance.setTheme(Dark)` and enqueues **nothing**;
    - a pull that emits the old value during a pending edit keeps the edit;
    - the save that throws shows `failure`.
  - `PreferencesScreenTest` (`@Logic`):
    - the tags exist: `settings.preferences.screen`, `.theme.system|light|dark`, `.timeFormat.24|12`, `.Sleep|Light|Caffeine|Travel`, `.reminders`, `.replay`;
    - replay calls `push(Route.Walkthrough(replay = true))`, and reminders calls `push(Route.Reminders)`;
    - the reminders row has no subtitle node;
    - with `failure`, the error line shows;
    - no section → `SettingsStoredState`.
  - `PreferencesReplayTest` (Robolectric `@Logic`, `:app`): port `replayTwiceStartsFreshAfterSkipOrDone` (parameterized `skip`):
    - from Preferences, set 12-hour, then open the replay;
    - Skip, or Next ×2 then Done; you are back on `settings.preferences.screen`;
    - open the replay again: a **new** `WalkthroughViewModel` instance at page Timeline;
    - finish; the outbox holds exactly one preferences row and **no** walkthrough row.

  - `PreferencesWalkthroughReplayTests` (JVM, `feature/settings/src/test`, or `feature/onboarding/src/test` if Phase 8's walkthrough model is `internal` there): port `replaySkipAndDoneReturnToPreferencesWithoutAnotherCompletionWrite` against Phase 8's replay-mode walkthrough model (parameterized `skip`): Skip, or Next ×3, → `onFinish` called, `completed`, 0 writes. Phase 8 has a similar case under another name (`replayReturnsWithoutWritingOrRoutingToToday`), not this one; port it anyway so the Swift name exists.
  - `PreferencesViewModelTest` also covers `enqueueSettings` when `isCurrent()` is false: nothing is enqueued and `failure` stays false.

### Task 9: `feat(settings): build body clock`
- Files: `BodyClockViewModel.kt`, `BodyClockScreen.kt`, the shell entry, and the `BodyClock` `isBuilt = true` flip with 10a's visibility test updated (§5.9). Tick plan "**Body clock**".
- Tests:
  - `SettingsBodyClockStoreTests` (JVM, `feature/settings/src/test`, with a port of iOS `SettingsPageFixture` (`SettingsProfileTests.swift:10`; 10a has none) built from 10a's real test parts: `SessionAccountDatabase` with `JdbcAccountDriverFactory`, a permit, and 4d's `OutboxRepository`). It ports `reopenedEditorUsesChronotypeObservedFromLocalSave` and `chronotypeSelectionIsSavedLocallyAndQueuedBeforeShowingSaved` with the same assertions (stored section, one `settings` row whose payload has `section == "bodyClock"` and `patch.chronotype == "late"`).
  - `BodyClockScreenTest` (`@Logic`):
    - with `bodyMissing` data, the badges appear on `ageBand` and `shiftWork` only, and the readiness card says "We need 2 more answers…" with "0/2 done";
    - "Go to the first one" brings `settings.bodyClock.ageBand` into view;
    - choosing `declared` shows the free-day tiles prefilled with the habitual times;
    - `caffeineHabit = cups` with all counts 0 shows "Add at least one drink…" and enqueues nothing;
    - typing "2,1" in mDFD enqueues `blue_blocker_mdfd: "2.1"`;
    - confirming the time sheet at 23:13 writes `"23:15"`, and dismissing writes nothing;
    - after a successful save, `settings.bodyClock.saved.about` exists;
    - the `excluded` and `adjusted` variants render their lines (`CopyIndex` lookup) and "Your plan is adjusted for your preferences and age range";
    - the notices exist by tag (`lightNotice`, `audienceNotice`);
    - **no node contains** "pregnan", "bipolar disorder?" as a question, "Light box" as its own question, or "18–29".
  - `BodyClockRecompositionTest` (`@Logic`, with the debug recomposition counter from 5d/9f test support): a chronotype tap recomposes the About item and leaves the Caffeine and Light items at 0.

### Task 10: `test(debug): seed body clock board states`
- Files: §5.10.
- Test `SettingsBoardStateSeedTest` (Robolectric `@Logic`, `:app` `testDebug`): each state gives the stored `bodyClock`/`readiness` listed; with no `CF_AUTH_TEST_ID`, nothing changes.

### Task 11: `test(settings): add guards and board captures`
- `PreferencesGuardsTest` and `BodyClockGuardsTest` (`@Layout`, one case per profile × language × theme; `Guards` from 0e): no clipped or ellipsized text, every touch target ≥ 48 dp, no horizontal overflow, and the 560 dp column on `foldInner` and `landscapeSideNav`.
  - Body clock runs with `bodyMissing` (badges and boxes are the widest content), Dutch at `small` + `largeFont`.
  - Scroll the `LazyColumn` through every item in each case.
- `SettingsPreferencesBodyClockCaptureTest` (`@Capture`, `pixel8`):
  - `SetPreferences_pixel8_en_{light,dark}`, `SetPreferencesNL_pixel8_nl_{light,dark}`;
  - body clock per state and section (the board is 3,700 tall; OQ-10b-9): `SetBodyClock_<bodyMissing|bodyReady|bodyAdjusted|bodyExcluded>-<intro|about|sleep|caffeine|light|health>_pixel8_en_light`, and the same set as `DarkSetBodyClock_…_dark` for `bodyMissing` and `bodyReady`.

### Task 12: `test(settings): check the theme on the emulator`
- `PreferencesThemeDeviceTest` (`@Smoke`, `app/src/androidTest/kotlin/net/chronoflow/app/settings/`, extends 0e `ScreenTest`; each test < 10 s, `timeout_msec=60000`, `waitUntil`, no sleeps).
- Seeds: `CF_AUTH_TEST_ID=<uuid>`, `CF_UI_TEST_PROFILE=screen`, the signed-in demo seed, walkthrough seen, `CF_SETTINGS_ROUTE=preferences`, `cf.settings.theme=light`.
- `themeChangeAppliesWithoutRestart`:
  1. wait for `settings.preferences.screen`;
  2. note the activity instance in `onActivity`;
  3. click `settings.preferences.theme.dark`;
  4. `waitUntil` the node's `captureToImage()` background pixel equals `CfDarkColors.background` ±2;
  5. assert the same activity instance, `getSystemService(UiModeManager).applicationNightMode == MODE_NIGHT_YES`, and DataStore theme `Dark`.
- `themeSurvivesRecreateAndRelaunch`:
  1. set Dark;
  2. `scenario.recreate()`, then the pixel is still dark;
  3. close the scenario and launch again **without** the seed's theme default (extend `ScreenTest.launch` so that passing `null` omits a default key, if 0e has no such option);
  4. it is still dark, and `applicationNightMode == MODE_NIGHT_YES`.
- The system splash itself is proven by 3a's splash theme test, which seeds the same `UiModeManager` state. A true cold start kills the instrumentation process, so record this manual check in the report (§8 item 6). Tick plan "**Tests**".

### Task 13: `docs(settings): record phase 10b results and open questions`
- Append OQ-10b-1 … OQ-10b-15 (§9) to `plan.md` "Open questions for the user" as **OQ-A484 … OQ-A498** (refresh R1).
- Add one line under Phase 12c "Connectivity & reminders page": "fill the Preferences reminders-row subtitle (`SettingsPreferencesView.swift:155–163`); 10b leaves it empty".
- In `docs/tooling.md`, record the seeded `adb` commands for `CF_SETTINGS_ROUTE=bodyClock` with each `CF_SETTINGS_BOARD_STATE`.

## 7. Edge cases (each one is covered by a test named above)

- A pull arrives while an edit is pending. The section is skipped by 4d, and the model's edit overlay keeps the value (Tasks 3, 4, 8).
- An older save completes after a newer invalid draft. "Saved" is not shown (Task 1).
- An invalid sleep pair. Both tiles show the error, and no outbox row is written (Task 1).
- `freeDayStatus` moves from `declared` back to `same_as_workdays`. The free-day times are sent as `null` (Task 1, normalize).
- `caffeineHabit` moves to `non_user`. All four counts are sent as `null` (Task 1).
- `blueBlockers` moves away from `rated`. `blue_blocker_mdfd` is sent as `null` (Task 1).
- An unknown hidden category from the server survives a toggle (Task 3).
- `readiness` is missing because a bodyClock row is pending. No readiness card is shown, and the page still works (Task 9).
- A readiness `missing` item has a `null` question. It is read as `ageBand` (Task 2).
- An unknown exclusion key. The raw key is rendered (Task 9).
- The time sheet is confirmed untouched on an unset tile. It writes `"12:00"` (iOS; Task 9).
- A theme change on API 30. `device-config` is written, `UiModeManager` is never called, and the content turns dark (Task 6).
- The session ends mid-save (`StaleSessionException`). No failure line, no crash (Task 1).
- A second replay starts fresh (Task 8).

## 8. Acceptance

1. `make quick` passes, including every new JVM test and the `@Logic` Robolectric tests.
2. `make targeted ONLY="feature:settings:SettingsBodyClockTests feature:settings:BodyClockReadinessTests feature:settings:SettingsPreferencesTests core:data:SettingsPendingProtectionTests feature:settings:SettingsBodyClockStoreTests feature:settings:PreferencesViewModelTest feature:settings:BodyClockScreenTest feature:settings:BodyClockRecompositionTest feature:settings:PreferencesWalkthroughReplayTests app:PreferencesReplayTest app:RootThemeTest core:datastore:DeviceAppearanceTest"` passes. Every JVM case is under 1 s and every Robolectric case under 5 s (`build/test-results/*/timings.json`).
3. `make generators-check` passes (glyphs).
4. `make captures ONLY=SetPreferences,SetPreferencesNL,SetBodyClock,DarkSetBodyClock` writes the Task 11 PNGs to `build/captures/`. The Opus reviewer compares them with the boards, and fixes or lists each difference by board name. The §4 differences are expected.
5. `make check` passes with `pixel` booted. It includes `PreferencesThemeDeviceTest`, Lint (`MissingTranslation`, no literal strings), Konsist (no `:feature` → `:feature` edge, no `MaterialTheme.colorScheme` or `isSystemInDarkTheme()` in feature code), `testReleaseUnitTest` (no seed code in release) and `:app:verifyReleaseManifest` (no new permission).
6. Manual cold-start check on `pixel`, with output pasted into the report:
   - set Dark in the app;
   - `adb shell am force-stop net.chronoflow.app.native.debug`;
   - `adb shell am start -W -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity`;
   - `adb exec-out screencap -p > splash.png` within the splash;
   - the centre-top pixel is `#06091C` (dark `skyTop`).

   If the sandbox cannot run `adb`, list the commands; the orchestrator runs them.
7. During `PreferencesThemeDeviceTest`, `adb logcat -d -s StrictMode` shows no violation from `net.chronoflow` frames.
8. No perf gate applies. A debug run through Settings → Body clock → change every section stays StrictMode-clean. Report `make size` before and after.
9. All three plan boxes of Phase 10b are ticked. Commits are atomic Conventional Commits with no trailers, and the OQ rows are added.

## 9. Open questions (proceed on the default; Task 13 copies them to plan.md as OQ-A484–OQ-A498, refresh R1)

| # | Question | Default |
|---|---|---|
| OQ-A484 (OQ-10b-1) | Time precision for the settings bedtime tiles: iOS uses a minute wheel; OQ-A8 says 5-minute steps for 9e and 10b. | OQ-A8: round to 5 min on Done, with the footnote "Steps of 5 minutes.". |
| OQ-A485 (OQ-10b-2) | Sheet subtitle: `PickerTime` shows "On a normal work or school day."; iOS has none. | Habitual times: "On a normal work or school day."; free-day times: "On free days" (both catalog keys). |
| OQ-A486 (OQ-10b-3) | Done on an untouched sheet: iOS writes (12:00 when unset); 9e writes nothing. | iOS: Done always writes the rounded value. |
| OQ-A487 (OQ-10b-4) | A Dutch keyboard types "2,1" for mDFD; iOS rejects it. | Replace `,` with `.` before validation and saving. |
| OQ-A488 (OQ-10b-5) | After a refused body-clock or preferences change, the model's edit overlay keeps the refused value until the page closes (iOS behaviour). | Port iOS as is; 10a's refused toast explains it. Revisit if the user reports it. |
| OQ-A489 (OQ-10b-6) | Category switch colour: the board uses `primary`, iOS the system default. | `CfSwitch` (dawn), as everywhere on Android (A9). |
| OQ-A490 (OQ-10b-7) | The reminders-row subtitle before 12b/12c. | None (iOS shows none while permission is unknown); 12c fills it. |
| OQ-A491 (OQ-10b-8) | `CfTimePickerSheet`/`WizardClockRounding` belong to 9e. | 10b depends on 9e (wave 12) and reuses them; it never creates them (review 2026-11-16). |
| OQ-A492 (OQ-10b-9) | `SetBodyClock` is 375 × 3,700, larger than one capture. | One capture per state and section item. |
| OQ-A493 (OQ-10b-10) | The "Not sure" chronotype id is empty, so its iOS tag ends in a dot. | Keep the iOS id `settings.bodyClock.chronotype.`. |
| OQ-A494 (OQ-10b-11) | Every save scrolls its section heading into view (iOS NH4.1). | Port it; use `scrollToItem` (no animation) under Reduce Motion. |
| OQ-A495 (OQ-10b-12) | A readiness line key missing from the catalog. | Render the raw key (iOS `value: key` fallback). |
| OQ-A496 (OQ-10b-13) | On API 29–30, the splash follows the system appearance while the app follows the choice. | Accept (plan `### Appearance`). Content and system bars follow the choice after the hand-off. |
| OQ-A497 (OQ-10b-14) | Each valid mDFD keystroke enqueues an outbox row (iOS binding). | Port it; rows coalesce in the `settings:bodyClock` chain and the payloads are tiny. |
| OQ-A498 (OQ-10b-15) | The board's NL category "Slaap" vs the catalog's "Slapen". | The catalog. |

## 10. Review checklist (Opus reviewer)

- **No health data:**
  - only `ageBand` and `shiftWork` can be required;
  - no medical question, legacy key or board age band appears anywhere (grep `pregnan|bipolar|eye_disease|vte|oral_contraception|caffeine_ruled_out|18_29` in `feature/settings`);
  - `exclude_caffeine` and `flight_movement_breaks` are never sent as `null`;
  - the notices are text, with no stored answer.
- **Whole-section bodies:** every body-clock save sends every field of its group (nulls included) and nothing else. Preferences sends only the changed key. `show_optional_items` never appears.
- **Local first:** the models never await sync. This phase's code calls no `refreshNow` or network, and calls `deps.requestSync()` only from the stored-state retry (never after `enqueueSettings`; 10a's own calls are not this phase's). `enqueueSettings` runs on the 4d writer, never on main and never under `runBlocking`.
- **Pending protection:** the edit overlay clears an entry only on an equal incoming value. Task 4 asserts the 4d section skip with real SQL.
- **Exceptions:** saves go through `settingsAttempt(…, cancelled = {})`. A foreign `CancellationException` (including `StaleSessionException`) is silent and never sets `failure`, and own-scope cancellation propagates. There is no `runCatching` around suspend calls. Saves are serialized FIFO.
- **Theme:**
  - DataStore is not read before the first frame;
  - `UiModeManager` is called only on 31+ and off the main thread;
  - there is no `recreate()`;
  - the theme is never in the outbox or the account DB;
  - feature code never calls `isSystemInDarkTheme()`;
  - the system-bar change happens in the activity only and respects the 3b/3c launch rules.
- **Board fidelity:** compare the captures with the boards using §4. Check:
  - pill border 2 dp, dawn vs border;
  - card radii 26/22;
  - row heights 64/60;
  - tile 84 dp with `surface2`;
  - missing box aligned to the content edge;
  - "Saved" by the heading;
  - readiness progress 80 × 6.
- **Copy:** every `Text` uses `stringResource` with lock-file names, and runtime keys go through `CopyIndex`. The two "We need …" sentences are separate keys. `{{drink}}` is lower-cased in the configuration locale. NL wraps at `small` + `largeFont`.
- **Accessibility:** headings on section titles; radio semantics on pills and cards; one `Role.Switch` node per switch row; drink buttons labelled; the readiness card and the counts are polite live regions; targets ≥ 48 dp.
- **Performance:**
  - Body clock is a keyed `LazyColumn` with a `contentType`;
  - sections read only their slices (Task 9 recomposition test);
  - no `onSizeChanged` sizing (the tile pair and missing box use layout);
  - no allocation in draw for the progress capsule.
- **Tests:** they follow AGENTS "Tests": the cheapest tier, exact tags, `waitUntil` instead of sleeps, parameterized cases instead of loops, and timings inside budget. Logic is tested on the JVM, not through the screens.
- **Typical mistakes:**
  - building the reminders page or its subtitle (12c);
  - a second `CategorySwatch`, `CfSwitch`, pill group or choice-card component (reuse 10a's `SettingsRadioCards`);
  - forgetting to flip 10a's `isBuilt` for Preferences and Body clock (release would hide both rows);
  - a duplicate `CfTimePickerSheet` beside 9e's;
  - sending only the changed body-clock field (the server validates the merged section);
  - formatting the tile times with the account 12-hour format (iOS shows raw `HH:mm`);
  - `rememberSaveable` holding answers instead of the ViewModel;
  - a snackbar for saves (10a owns toasts; this page uses the error line);
  - re-creating the activity for a theme change;
  - reading DataStore in `attachBaseContext`;
  - using `Route.Walkthrough` on the root navigator instead of `holder.push`.
