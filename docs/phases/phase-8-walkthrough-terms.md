# Phase 8: Walkthrough and terms

- **Branch:** `feat/phase-8-walkthrough-terms`, from the stack top once **6a** (tab shell) is in it. 6a brings 4e (session controller, gate router, routes, `RootNavigator`, seeds), 4d (outbox, repositories, sync), 4c (mock), 2 (primitives), 0c (strings), 0e (profiles, guards, captures) and 1c (`TimelineCategoryStyle`, `TimelineGlyph`). If any of them is missing, stop and report it.
- **Plan entry:** `plan.md` "Phase 8: Walkthrough and terms" (l.2545–2582). Also read A7 (sync, the walkthrough 401 exception), A8 (navigation: "walkthrough and terms replace the shell"), A10, A23 ("No WebView: policy pages open in a Custom Tab"), "Android spec: window policy" (l.357–370: Walkthrough and Terms use a column of at most 560 dp, centred, from a 600 dp window), "Android spec: navigation and routes" (l.400–404: gate order verify → terms → walkthrough → app; missing policies keep the gate loading), "Android spec: seeds and test hooks" (l.466–476), OQ-A32 (l.619), the inherited OQ 38 and 39 row (l.159), and "Standard acceptance for every phase" (l.1007–1018, #5 trims the guard matrix).
- **Tests:** every test follows `AGENTS.md` "Tests": the cheapest tier, the budgets, exact queries, no `Thread.sleep`, the hang caps. Read it before you write a test. Use the 0e tier annotations (`core/testing/…/testing/tiers/`): Robolectric `@Logic`/`@Layout`/`@Capture`, and emulator `@Smoke`. The current rules (test diet, 2026-11-20) override every budget and test shape written below; see "Stack check (2026-11-20)".
- **Tags:** **[F]** is a fact read in source (path given). **[R]** is a rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at `f391a1fa` with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there. `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`. `W/` = `/Users/rickgout/Sites/chronoflow-new` at `e36368ec`.
- **Paths:** `…/onboarding/` = `feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/`; `…/ui/` = `core/ui/src/main/kotlin/net/chronoflow/core/ui/`; `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`.
- **Names from earlier phases.** This brief uses the plan's names (`SessionController.activeSession()`, `mutationPermit(snapshot)`, `signOut(expected, discardPending)`, `OutboxRepository.enqueue(change, permit)`, `SettingsRepository.values()`, `OutboxChange.Walkthrough`, `OutboxChange.Policies(version)`, `AccountPolicies`, `StaleSessionException`, `SessionGraph`, `RootNavigator`, `RouteProbe`, `CfButton`, `CfBanner`, `CfLinearProgress`, `Modifier.softShadow(shape, dark)`, `cfClickable(onClick, role)`, `LocalSafeInsets`, `LucideGlyphs`, `CfTheme.colors/type`, `PlanUITokens`, `AuthTokens`, `TermsTokens`, `@BoardMetrics`, `WindowPolicy`, `CategorySwatch`/`CategorySwatchSize` (6a), `ShellHolder.push/back` (landed as `ShellViewModel.push/back`), `CfNavLayout` (6a), `Guards`, `BoardCapture`). Before you code, open the merged code and use **its exact names**. Where a name differs, keep the merged one and say so in your report. Do not rename earlier code.
- **Checked 2026-11-16** against iOS `f391a1fa`, the boards, web `e36368ec`, `plan.md`, `AGENTS.md` and the 2/4a/4c/4d/4e/5a/5d/6a/6d briefs. Corrected: 4d line references; `CategorySwatch` is 6a's (`CategorySwatchSize`, OQ-6a-12), so the old Task 3 is dropped; `softShadow(shape, dark)`; `SettingsRecord.value` is a JSON string; the 4a `StaleSessionException` is a `CancellationException`, so both models treat it as a session change; the replay hides the shell through 6a's `CfNavLayout` and `holder.push/back`, with full insets while hidden; `RootContent` needs entry-scoped ViewModels (5d OQ-5d-14); 5d's Custom Tab opener lives in `:feature:auth` and cannot be reused, and the Custom Tab takes no toolbar colour (5d parity); the seed `mutationsOffline` uses `CF_AUTH_TEST_TERMS_PENDING` (iOS `AppServices.swift:63`), not `…_TERMS_CHANGES_PENDING`; the 600 dp breakpoint is 6a's `WindowPolicy`; the terms gap between summary and actions is 40 dp; the guards add `landscapeSideNav` (AGENTS); Robolectric `:app` tests pass `SoftwareSessionCipher`.
- **Stack check (2026-11-20, against `391e1102`: 0a–7 plus the test-diet branch).** Every dependency (6a, 4e, 4d, 4c, 2, 0c, 0e, 1c) is in the stack, and Phase 7 too. Nothing of Phase 8 exists yet: `:feature:onboarding` is registered with only its `build.gradle.kts` (namespace `net.chronoflow.feature.onboarding`, no sources), and there is no `OnboardingSession`, `PolicyLinks`, `Walkthrough*` or `Terms*` code. Deltas:
  - **Paths.** `…/ds/` below means `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/`. `WindowPolicy` (`wideBreakpointDp = 600`) is `…/ds/layout/WindowPolicy.kt`; `BoardMetrics` and `LocalSafeInsets` are in the same package. `PlanUITokens`, `AuthTokens`, `TermsTokens` are in `…/ds/generated/CfDimens.generated.kt`; every token named in this brief exists with the value given. `CfButton`/`CfBanner` are in `…/ds/component/`, `cfClickable(onClick, role, enabled, onClickLabel, interactionSource, focusShape)` in `…/ds/effect/CfPress.kt:115`, `softShadow(shape, dark)` in `…/ds/effect/SoftShadow.kt:32`, `CfLinearProgress(progress: () -> Float, modifier)` in `…/ds/system/CfControls.kt:159`, `CfNavLayout { Bar, Rail }` in `…/ds/system/CfNavigationSuiteScaffold.kt:17`, `LocalReduceMotion` in `…/ds/theme/ChronoFlowTheme.kt:29`. `LocalWindowSize` (6e) is `core/ui/…/ui/window/LocalWindowSize.kt`.
  - **Feature module.** The `chronoflow.android.feature` plugin already adds `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:model`, lifecycle-runtime/viewmodel-compose and navigation3-runtime. Task 4 adds only `:core:sync` and `:core:data`. For tests, the JDBC factory is `:core:database-testing` `JdbcAccountDriverFactory` (`core/database-testing/…/database/testing/`); `SessionAccountDatabase` is `core/data/…/data/session/SessionAccountDatabase.kt`. Follow `feature/today/build.gradle.kts` for the test deps (`:core:data-mock`, `:core:testing`, `:core:testing-compose`, coroutines-test, turbine).
  - **Session APIs (all match the brief).** `SessionController` (`core/sync/…/sync/session/SessionController.kt`): `updates: StateFlow<SessionUpdate?>` l.77, `suspend activeSession()` l.88, `suspend mutationPermit(snapshot): SessionMutationPermit?` l.95, `suspend signOut(expected, discardPending): SignOutResult` l.136. `SignOutResult.{SignedOut, NeedsConfirmation(pendingCount)}` and `SessionSnapshot(session, epoch)` are in `core/model/…/model/session/SessionValues.kt`. `StaleSessionException : CancellationException("stale session")` is `core/model/…/model/session/StaleSessionException.kt`. `OutboxRepository.enqueue(change, permit = null, now, randomBytes): OutboxRecord` (`core/data/…/repository/OutboxRepository.kt:45`); `SettingsRepository.values(): Flow<List<SettingsRecord>>` (l.24); `SettingsRecord(section, value: String)`; `OutboxChange.Walkthrough` and `OutboxChange.Policies(version)` (`core/model/…/model/OutboxChange.kt`); `AccountPolicies` (`core/model/…/model/api/SessionContracts.kt:18`). `AccountReadinessRepository.values(accountId)` is `suspend` and returns `Flow<StoredAccountReadiness?>`.
  - **Fixture database access (worker preflight).** `SessionAccountDatabase.activeAccountDatabase()` returns the SQL `AccountDatabase`; `activeDatabase()` returns the sync interface. Use `activeAccountDatabase()` in `OnboardingFixture` and wrap it in the existing `core/data/…/data/sync/SqlSyncDatabase` for `SyncEngine`, `FullPull` and `OutboxFlusher`. Supply their existing dispatcher/scheduler dependencies. This adapts test wiring only and changes no landed behaviour.
  - **No app `Json`.** `AppGraph` holds no `Json`. The stored `policies` section is decoded everywhere with `ApiJson.response` (`core/model/…/model/api/ApiJson.kt`; see `AccountReadinessRepository.kt:26–28`). §5.2's `json` parameter is `ApiJson.response`. Drop the parameter if nothing else needs it.
  - **`:app` seam.** `SessionGraph` (`app/…/app/SessionGraph.kt`) carries `epoch`, `snapshot`, `settings` and `outbox`. `AppGraph.controller` and `AppGraph.sessionGraphs.current` (a `StateFlow<SessionGraph?>`) exist. A feature ViewModel receives its dependencies the way Trips and Today do: the feature declares the dependency interface plus `object XKey : CreationExtras.Key<X>` (`TripsDependenciesKey` in `feature/trips/…/TripsDependencies.kt:37`, `TodayDependenciesKey` in `feature/today/…/TodayViewModel.kt:251`), and `:app` sets it into `MutableCreationExtras(owner.defaultViewModelCreationExtras)` (see `app/…/app/shell/AppShell.kt`). `AppGraphKey` is `:app`-only and cannot reach a feature. Declare `OnboardingSessionsKey` in `:feature:onboarding`.
  - **Root routing.** `RootContent(state, onBack, content: @Composable (Route) -> Unit)` (`app/…/app/RootContent.kt`) already has both entry decorators (`rememberSaveableStateHolderNavEntryDecorator`, `rememberViewModelStoreNavEntryDecorator`). It already keys `Terms`, `Walkthrough` and `GuestWizard` entries by navigation revision, so each navigation builds a fresh entry and ViewModel. **The §5.8 conditional decorator work and the Task 6 `RootContentTest` "ViewModel is cleared" case are therefore moot: do not add them.** The per-route `when (route)` that §5.8 calls "`RootContent` entries" lives in `app/…/app/CfRoot.kt` (l.137–230). `Terms` and `Walkthrough` currently fall into its `else -> RoutePlaceholder(...)` branch. Follow the `SignIn`/`SignUp` branches there for `viewModel(factory = …, extras = …)`. `Gate.toRoute()` (`core/navigation/…/RootNavigator.kt`) maps `Gate.Walkthrough` to `Route.Walkthrough()` and `Gate.App` to `Route.Today`.
  - **Shell.** The 6a "holder" is `ShellViewModel` (`app/…/app/shell/ShellViewModel.kt`: `state`, `push(route)`, `back(): Boolean`, `select(tab)`), built by `rememberShellViewModel(root)`. The tab entry provider is `AppShell`'s `page = { route -> … }` lambda (`app/…/app/shell/AppShell.kt`). Non-`TripPage` routes currently render `RoutePlaceholder`; add the `Route.Walkthrough` branch there. The Bar/Rail choice (`currentWindowAdaptiveInfoV2()…isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)`), the `contentInsets` rule and the `LocalSafeInsets provides contentInsets` re-provision are in **`AppShellScaffold.kt`** (l.57–70, l.120), not in `AppShell`. `ShellNavState` already has an `overlay` field (used only for `Route.Wizard`), and `ShellNav.push` sends every non-`Wizard` route to the selected stack. `CfNavigationSuiteScaffold` also picks its test tag from the layout (`cf.nav.bar`/`cf.nav.rail`), so `None` needs a tag too.
  - **Today is real.** Phase 7 replaced the 6a Today seam. First run ends on `TodayScreen` (tag `today.screen`), still reached through `Route.Today`.
  - **CategorySwatch.** The signature is `CategorySwatch(type: BlockType, modifier: Modifier = Modifier, size: CategorySwatchSize = CategorySwatchSize.Regular)` (`core/ui/…/ui/plan/CategorySwatch.kt:70`); call it with named arguments. `CategorySwatchMetrics` is `private`, so nothing outside that file can use it.
  - **Custom Tab.** The catalog already has `androidx-browser` (`browser = "1.10.0"`, `libs.androidx.browser`; A1/OQ-A219 recorded by 5d). Task 1 only adds `implementation(libs.androidx.browser)` to `core/ui/build.gradle.kts`. `:core:ui` has no `CustomTabsIntent` helper, so Task 1 runs. 5d's opener is `CustomTabPolicyOpener(context, dark, start)` in `feature/auth/…/auth/signup/PolicyOpener.kt`, with its test `feature/auth/src/test/…/signup/PolicyOpenerTest.kt`. Mirror both.
  - **Strings.** All 45 resource names in §5.1 exist in `tools/l10n/names.lock.json` and in `core/ui/src/main/res/values{,-nl}/strings_ios.xml`, with the placeholders and Dutch values given.
  - **Lucide.** `tools/icons/lucide.json` board entries use `{"source": "board", "board": …, "first": …}` (e.g. `chevron-right`, l.94). `file-text` is absent, and the board has its five paths. `LucideGlyphsTest` (`core/designsystem/src/test/…/LucideGlyphsTest.kt:19`) asserts **43**, so Task 2 makes it 44.
  - **Seeds.** `TestSeed.parse` (`app/src/debug/…/debug/TestSeed.kt`) reads flags with `flag("CF_…")` (l.104–116). `CF_WALKTHROUGH_TEST_PENDING` and `CF_AUTH_TEST_TERMS_CHANGES_PENDING` are not parsed yet. `MockSeedFlags(unverified, walkthroughSeen, termsPending)` and `GraphOverrides` are in `app/…/app/GraphOverrides.kt` (no `mutationsOffline` yet). The mock is built in `PerfMockSources.dataSource` (`app/src/perf/kotlin/net/chronoflow/app/perf/PerfMockSources.kt:52`; debug compiles `src/perf`), and `MockDataSource(mutationsOffline = …)` exists (`core/data-mock/…/MockDataSource.kt:86`). The signed-in seed runs in `SeedRunner.reseed` → `MockSessionSeed.run` (`app/src/debug/…/debug/`); seed the extra settings row there. `TestSeedSessionKeysTest` exists in `app/src/testDebug/…/debug/`.
  - **Test-diet rules (AGENTS.md "Tests", `docs/test-diet.md`).**
    1. Budgets: a JVM or warm Robolectric case < 1 s (not 5 s); per fork, 20 s for the first Robolectric case per SDK, then 3 s for the first case per class/SDK; 60 s hang watchdog (`TestTimingListener`). Every "under 5 s" below means < 1 s warm.
    2. Konsist `unitTestsKeepAppBootsAtApprovedBoundaries` (`konsist/…/konsist/AppTestBoundaryRules.kt`, allowlist `keptAppTestBoundaries`) rejects any unit test that calls `AppGraph(`, `SessionGraph(s)(`, `RootContent(`, `CfRoot(`, `MainActivity(`, `ActivityScenario.launch` or `buildActivity<MainActivity>`, unless it is allowlisted with a reason in `docs/test-diet.md`.
    3. Never assert copy on a screen. Screens are stateless (`XScreen(state, onEvent)`, as `TodayScreen`), and logic is tested on the ViewModel's UI state on the JVM.
  - **`LaunchGateRoutingTest` no longer exists.** The test diet deleted it (`ed18cf1b`). Gate routing is covered on the JVM by `LaunchSessionBridgeTest` (`termsMapsToTerms`, `walkthroughMapsToFirstRunWalkthrough`, …), `LaunchConnectionTests` (`SessionGateRouter`), `MockSessionSeedGateTests` and `core/data` `AccountReadinessTests`. Only one remaining unit test reaches the real `Route.Terms` dispatch: `RootContentTest.recreationKeepsTheProcessRouteWithoutAnotherNavigation` (asserts `route.terms` through `MainActivity`). Switch it to a route that stays a seam (`Route.GuestWizard`, until 11a). `RootContentTest`'s other cases, `RootContentConstructionTest` and `RootRouteTransitionTest` pass their own `RoutePlaceholder` lambda and are unaffected. The emulator `SignUpVerifyJourneyTest` waits for `Route.Walkthrough()` through `RouteProbe` and still works.
  - **Emulator harness.** `ScreenTest.launch(overrides)` (`app/src/androidTest/…/app/testing/ScreenTest.kt`) applies `TestSeeds.apply` and forwards the intent itself (`CF_AUTH_TEST_ID`, profile `screen`, language/theme `system` by default). Pass the seed keys as `overrides`. `waitFor(timeout) {}` wraps `compose.waitUntil`. `UiDevice.predictiveBack()` is in `…/testing/BackGestures.kt`.
  - **Captures.** `BoardCapture.capture(rule, name)` filters by name prefix (`cf.captureOnly`). The `pixel8` profile is 411 × 914; Phase 7 captured at the board's 390 × 844 through a test-only copy of `pixel8` (`feature/today/src/test/…/TodayBoardCaptureTest.kt`). The boards here are 375 × 812; follow the same approach.
  - **Selectors.** `make targeted` selectors have no leading colon (`feature:onboarding:WalkthroughModelTests`, `app:…`).
  - **OQ numbering.** `plan.md` OQ rows end at **OQ-A336**. Task 12 adds OQ-8-1 … OQ-8-11 as **OQ-A337 … OQ-A347**, each tagged "(8 OQ-8-n)" as Phase 7 did. `plan.md` l.2880 is 10a's "Sign out with pending changes" task, and l.2917 is 10b's "Replay walkthrough" line.
  - **Flagged for the worker (decide and report; do not silently pick):**
    1. **Progress bar height.** Phase 7 settled the same iOS `DayProgressRow` at **80 × 6 dp** (OQ-A334, `TodayMetrics.progressHeight`, board `h-1.5`). §4 here says 4 dp (iOS `ProgressView` default). `CfLinearProgress` sets its height inside to `wizardProgressHeight` (4), and Today overrides it through the modifier. Pick one height for both screens and record it in the OQ row.
    2. **Screen shape.** §5.6/§5.7 pass the ViewModel into `WalkthroughScreen`/`TermsScreen`. AGENTS requires stateless screens. Split each into a thin route composable in `:feature:onboarding` (collects the ViewModel, owns `BackHandler`/`LaunchedEffect(completed)`) and a stateless `XScreen(state, onEvent)`.
    3. **Screen tests that test logic or copy.** Move these to JVM ViewModel tests on `WalkthroughUiState`/`TermsUiState`: `busyDisablesSkipAndDoneAndCompletesOnce`, `acceptDisabledUntilPoliciesLoad…`, `signOutBusyDisablesBothButtons`, `signOutFailureShowsMessage`, `needsConfirmation…`, the counter progression, and the Replay `onFinish` count. Keep Robolectric only for structure: tags, enabled flags read from semantics, headings, live region, cleared semantics, the reduce-motion dot width, touch sizes. Drop text assertions (`"1 OF 3"`, `"Try again"`, `"Sleep"`). Assert the page index, the `failed` flag and cleared semantics on the preview tag instead.
    4. **`WalkthroughReplayRoutingTest`** as written (real `AppGraph`, `TestSeeds`, `GraphOverrides`) breaks the Konsist boundary rule. Host `AppShellScaffold` with fake content and `rememberShellViewModel`, as `app/src/testDebug/…/shell/AppShellTest.kt` does, and assert the chrome hiding plus a zero-call `enqueue`.
    5. **Tasks 6 and 9 `LaunchGateRoutingTest` cases.** They cannot be added back without an allowlist entry. Default: prove skip → gate `App` and accept → gate `Walkthrough` in the onboarding JVM tests, which already assert `gate`, and leave the screen-to-route hop to `OnboardingJourneyTest`.
    6. **Guard matrix.** Standard acceptance #5 replaces the 60- and 40-case matrices: light only, `small` + `largeFont` in nl, `pixel8` in en, plus `foldInner` and `landscapeSideNav`, because both screens have a wide 560 dp layout. That is 15 walkthrough cases (3 pages × 5) and 10 terms cases (2 states × 5). Captures keep both themes and both languages.

---

## 1. Goal

When a signed-in account has not accepted the current terms version, the **Terms update** screen (`TermsUpdate`) appears before anything else except email verification. **Accept** commits locally and moves the gate at once, even offline. If the local save fails, the screen shows `TermsFailed`. **Read the full terms and privacy policy** opens `https://chronoflow.net/terms` in a Custom Tab, and **Sign out** goes through the session controller. Next, an account that has not seen the walkthrough gets three **walkthrough pages** (`Walkthrough`, `Walkthrough2`, `Walkthrough3`), driven only by **Next / Done** and **Skip**. Skip and Done write `walkthroughSeen` plus one `walkthrough` outbox row in a single local transaction. The gate then opens **Today** (Phase 7's `TodayScreen`, tag `today.screen`) without waiting for the network. The same pages open in **replay** mode (`WalkthroughReplay`) from a tab back stack; there Skip, Done and system back return to the caller without writing anything.

Done means:
- the gate order is terms, then walkthrough, then the app, proven by the ported JVM tests and one emulator journey;
- the ported iOS 17f and 17g cases pass on the JVM, including a walkthrough POST 401 that keeps its row and leaves the session alive;
- Robolectric guards cover every board state per plan standard acceptance #5 (light; `small` and `largeFont` in nl, `pixel8` in en, plus `foldInner` and `landscapeSideNav` for the 560 dp column); captures cover en/nl × light/dark;
- `make captures` produces the six board captures, and the reviewer has compared them with the boards;
- replay returns to its caller with zero writes;
- no main-thread disk work: StrictMode stays clean during the journey.

## 2. Inputs to read (in this order)

1. **This repo:** `plan.md` (the sections listed above); `AGENTS.md` ("Working rules", "Performance", "Tests", "Traps"); `docs/phases/phase-4d-sync.md` Task 3 (`OutboxRepository.enqueue` rules for `Policies` and `Walkthrough`, l.283–291) and the walkthrough-401 rules (Task 5 l.351–353, Task 6 `runOnce` l.386, checklist l.512); `core/model/…/model/session/StaleSessionException.kt` (`StaleSessionException : CancellationException`); `docs/phases/phase-4e-session.md` §5.4 (readiness, gate, router), §5.7–5.8 (routes, `RootContent`, `RoutePlaceholder`, `RouteProbe`), T11 (seeds) and T13 (`LaunchGateRoutingTest`); `docs/phases/phase-2-design-system.md` tasks 7 (`cfClickable`), 9 (`softShadow`), 10 (icons), 11 (`CfButton`), 14 (`CfBanner`) and 15 (`CfLinearProgress`); `docs/phases/phase-0c-strings.md` (resource names: **the lock file `tools/l10n/names.lock.json` is the authority**); `docs/phases/phase-0e-test-harness.md` T6–T9; the merged 6a brief and code (§5.6 `ShellNavState`/`ShellNav.push`/`back`, §5.7 `CfNavigationSuiteScaffold`/`CfNavLayout`/`WindowPolicy`/`LocalSafeInsets` re-provision, `ShellHolder`, the Today seam, and `CategorySwatch` l.356–367, OQ-6a-12); `docs/phases/phase-5d-sign-up-verify.md` §5.3 (`PolicyOpener`, `androidx.browser` version rule, l.144–145) and OQ-5d-14 (`RootContent` entry decorators, l.188); `docs/phases/phase-5a-sign-in.md` l.95/l.109 (the 560 dp column pattern in an `@BoardMetrics` object).
2. **Research:** `docs/research/05-launch-auth.md` §5 (routing), §6 (Walkthrough), §7 (Terms, PolicyReader, SignOutPending) and §17 OQ A6, A7, A19; `docs/research/07-day-to-day-settings.md` §1 (scope) and §11 (endpoints, outbox kinds).
3. **Boards** (read-only; open the HTML in a browser to render them):
   - `DC/Walkthrough.dc.html` is the main board, with props `theme` (light/dark), `step` (1–3) and `entry` (first/replay). The frame is 375 × 812 with `padding: 8px 16px 32px`.
   - `DC/Walkthrough2.dc.html` and `DC/Walkthrough3.dc.html` are wrappers with `step="2"` and `step="3"`.
   - `DC/WalkthroughReplay.dc.html` is a wrapper with `entry="replay"`. It looks the same as page 1; only the exit differs (`exitHref` = `SetPreferences.dc.html` instead of `Today.dc.html`).
   - `DC/TermsUpdate.dc.html` is the main board, with props `theme` and `state` (ready/failed). The frame is 375 × 812 with `padding: 24px 16px 32px` and `gap: 20px`.
   - `DC/TermsFailed.dc.html` is a wrapper with `state="failed"`.
   - `DC/PolicyReader.dc.html` holds placeholder legal text only. **Not built** (§3).
   - `DC/ds/chronoflow/components/bundle.js`: `TimelineBlock` (`function qe`; titles and times are `max-sm:hidden`, so below a 640 px viewport only the centred icon shows), `CategorySwatch` (`function ve`), `DayProgress` (`function Le`), `TodayRow` (`function ie`) and the category styles (`le=`).
   - The canvas note `canvas.json` l.3698 ("Done 1 Nov") decides that the walkthrough ends on Today after the first run and returns to Preferences on replay. The effective date and change summary stay placeholders until legal writes them.
4. **iOS references** (`I/` at `f391a1fa`). Port the behaviour from these and keep the type and test names:
   - `plan.md:1967` "17f: Walkthrough"; `plan.md:1980` "17g: Terms update"; `plan.md:2376` "Open questions for the user": OQ 39 is at l.2418 and OQ 31 at l.2410.
   - `Docs/phase-17f-walkthrough.md`, `Docs/phase-17g-terms.md`.
   - `Kit/PlanUI/Walkthrough/WalkthroughModel.swift` (66 lines), `WalkthroughGeometry.swift` (21), `WalkthroughView.swift` (112), `WalkthroughPreview.swift` (131) and `WalkthroughCopy.swift` (15).
   - `Kit/Settings/TermsUpdateModel.swift` (42), `TermsUpdateView.swift` (142), `TermsGeometry.swift` (21), `TermsStrings.swift`; `Kit/DesignSystem/TermsTokens.generated.swift`.
   - `I/App/WalkthroughDestination.swift` (33) and `I/App/TermsDestination.swift` (55), which show how the app waits for session, database and permit, and how the save closures check the session.
   - `I/App/Debug/TermsUITestSeed.swift`; `Kit/DataAccess/AuthUITestConfiguration.swift:28` (`CF_WALKTHROUGH_TEST_PENDING`); `I/App/Session/AppServices.swift:63` (`mutationsOffline` from the pending seeds).
   - `Kit/PlanUI/Components/PlanComponents.swift:7–98` (`CategorySwatch`; ported by 6a, reused here), `Kit/PlanUI/Today/TodayPanelComponents.swift:39–70` (`DayProgressRow`), `Kit/PlanUI/TripPage/PlanAppearance.swift` (`PlanCategoryStyle`, `PlanPattern`).
   - `Kit/Settings/SettingsHomeModel.swift:123–160` (`signOut`, `stay`: the Terms sign-out reuses it on iOS).
   - Tests: `Packages/ChronoFlowKit/Tests/PlanUITests/WalkthroughTests.swift`, `WalkthroughCopyTests.swift`; `Tests/SettingsTests/TermsAcceptanceTests.swift`, `TermsPresentationTests.swift`; `I/AppUITests/WalkthroughTests.swift`, `TermsTests.swift`.
   - iOS screenshot: `docs/research/screens/ios/23-walkthrough.png` (page 1, light: compact icon-only blocks; the caffeine pill is dotted).
5. **API** (`W/` at `e36368ec`, reached only through 4b/4d; never call it in tests):
   - `POST /v1/terms/acceptance` (`routes/api.php:65`, `auth:sanctum`, `throttle:api`, `verified`) takes the body `{"accepted": true, "version": "<termsVersion>"}` and answers **204**. If `version` is not `PolicyVersions::CURRENT_TERMS`, it answers **422** with "The terms have changed. Read the current version and agree again." (`app/Http/Requests/Api/V1/AcceptTermsRequest.php`). 4d treats the 422 as a final refusal (`isFinal`, kind `settings`): it records `settings.syncFailure` and deletes the row; with no pending row left, the next full pull stores the server's policies again, and the gate asks again.
   - `POST /v1/walkthrough` (`routes/api.php:66`, same middleware) has no body and answers **204**. 4d **never refuses** a walkthrough row. A 401 on it returns `deferredWalkthroughAuthorization`, keeps the row and retries with backoff; it never wipes the session.
   - Policies reach the app in `GET users/me` and `GET account` as `policies {termsVersion, privacyVersion, acceptedTermsVersion?, acceptedTermsAt?}`, which 4d stores as settings section `"policies"`.

## 3. Out of scope (deferred, with owner)

| Item | Owner / reason |
|---|---|
| The Today screen itself | 7 (landed). The first run ends on `Route.Today`, now Phase 7's `TodayScreen`. |
| Preferences and its "Replay walkthrough" row | 10b. It pushes `Route.Walkthrough(replay = true)` on the Settings stack, using the hook built here. |
| The `SignOutPending` sheet (Stay / Sign out and discard) | 10a. This phase exposes the `NeedsConfirmation` state, `stay()`, `signOut(discardPending = true)` and a composable slot (built in Task 8, wired in Task 9). |
| An in-app policy reader (`PolicyReader`) | Not built: it holds placeholder legal text only, and iOS did not build it (05 OQ A6). The policy row opens a Custom Tab. |
| A separate privacy link on Terms | None: the board and iOS have one combined row pointing to `/terms`. `/privacy` is used by sign-up (5d). |
| Per-version effective date and "what changed" summary | No server fields exist. Ship the iOS copy (§4). |
| Swipe or page-by-page back navigation in the walkthrough | Never: the boards have buttons only (iOS 17f). |
| Turning reminders on from page 3 | 12c. The page-3 body copy stays as drawn, with no action. |
| Any visible error for a failed walkthrough save | No board. As on iOS, the buttons simply re-enable (OQ-8-6). |
| Sync-failure or refusal presentation | 6d, after a board exists (iOS OQ 30). |

## 4. Settled values that override the raw boards [F]

Conflict order (plan "How conflicts between sources are settled"): a user decision, then iOS at the pin, then the boards. Build the right-hand column.

| Board says | Build | Source |
|---|---|---|
| Terms body "The new terms and privacy policy apply from **[effective date]**…" | "Please take a moment to read what changed. You'll need to agree before carrying on." | `TermsUpdateView.swift:71`; 17g quick win (2026-11-10); 05 OQ A7; web `terms-update-modal.tsx:83` |
| Bullets `[Change one]` … as a bold title plus a muted body | Three single-style bullets in `termsStrong` (600 15/22), `foreground`: "ChronoFlow helps you plan sleep, light and caffeine around your trip." / "ChronoFlow gives general timing guidance, not medical advice. Check with a doctor if you have a health condition, are pregnant or take prescription medicine." / "Your account stores your trips, profile and preferences." | `TermsUpdateView.swift:84–96` |
| Footnote "Shown after sign-in, before the walkthrough. Your trips stay saved if you sign out." | "Your trips stay saved if you sign out." only (the first sentence is a designer annotation) | `TermsUpdateView.swift:136`; 17g doc |
| "What changed" at +0.05 em tracking | `screenLabel` (600 14/20, 0 em), uppercased | `TermsUpdateView.swift:80` |
| Policy row label weight 500 | `termsBody` (Figtree 400 15/22) | `TermsUpdateView.swift:102` |
| Policy row `href="PolicyReader.dc.html"` | Custom Tab to `https://chronoflow.net/terms` | iOS opens the system browser (`:98`); A23; OQ-A32 |
| Accept `href="Walkthrough.dc.html"` | No navigation from the screen. The gate router navigates to Walkthrough or Today once the local commit lands. | `TermsDestination.swift`, 4e router |
| Walkthrough Skip/Done `href` to Today | No navigation from the screen in first-run mode: the gate opens Today. In replay mode the caller's return action runs. | `WalkthroughModel.swift`, 17f |
| `CategorySwatch size="md"` = 36 dp (bundle `size-9`), icon 16 | 6a's `CategorySwatch(type)` = 32 dp (`PlanUITokens.space8`); small 28 dp (`space6 + space1`) | `PlanComponents.swift:15–17`; 6a l.356, OQ-6a-12 (OQ-8-1) |
| `DayProgress` bar `h-1.5` (6 px) | `CfLinearProgress` (4 dp) × `PlanUITokens.progressTrackWidth` (80) | iOS `ProgressView` default (`TodayPanelComponents.swift:52`) |
| Dot width `transition 150ms ease` | 150 ms (`PlanUITokens.walkthroughDotDuration`), `EaseInOut`; no animation under Reduce Motion | `WalkthroughView.swift:89–92` (OQ-8-7) |
| Brand image `/_blob/…` 28 × 28 | `R.drawable.terms_brand`, converted unchanged from `_design/icon/ios/AppIcon-default.svg` (square; iOS AssetGen copies the PNG rendering of the same master, `_design/icon/ios/AppIcon-default.png`, into `TermsBrand.png`) | `I/Tools/AssetGen/Sources/AssetGenCore/Generator.swift:54–61` |

## 5. Spec

### 5.1 Copy (0c resources, `net.chronoflow.core.ui.R`) [F catalogs `Kit/PlanUI/Resources/Walkthrough.xcstrings`, `Kit/Settings/Resources/Terms.xcstrings`]

These names were computed with the 0c slug rule. **Check every one in `tools/l10n/names.lock.json`; where they differ, the lock wins.** Placeholders follow the 0c rule: `{{a}} … {{b}}` becomes `%1$s … %2$s` in order of first appearance. Never write a literal; `Text` receives `stringResource(...)` only. Uppercase with `text.uppercase(LocalConfiguration.current.locales[0])`.

| Resource | en | nl |
|---|---|---|
| `walkthrough_current_of_total` | `%1$s of %2$s` | `%1$s van %2$s` |
| `walkthrough_skip` / `walkthrough_next` / `walkthrough_done` | Skip / Next / Done | Overslaan / Volgende / Klaar |
| `walkthrough_your_plan_on_one_timeline` | Your plan on one timeline | Je plan op één tijdlijn |
| `walkthrough_sleep_light_caffeine_and_your_06bee8` | Sleep, light, caffeine and your flights side by side, each at the right local time. | Slaap, licht, cafeïne en je vluchten naast elkaar, steeds op de juiste lokale tijd. |
| `walkthrough_know_each_step_at_a_glance` | Know each step at a glance | Herken elke stap in één oogopslag |
| `walkthrough_every_kind_of_step_has_its_ow_14503c` | Every kind of step has its own icon and pattern, so you never need colour to tell them apart. | Elke soort stap heeft een eigen pictogram en patroon, zodat je ze ook zonder kleur uit elkaar kunt houden. |
| `walkthrough_tick_off_what_you_did` | Tick off what you did | Vink af wat je hebt gedaan |
| `walkthrough_checking_off_changes_your_pro_a36d57` | Checking off changes your progress, never your plan. Turn on reminders to get a nudge before each step. | Afvinken verandert je voortgang, nooit je plan. Zet herinneringen aan om vóór elke stap een seintje te krijgen. |
| Preview: `walkthrough_sleep`, `_seek_bright_light`, `_avoid_light`, `_caffeine`, `_flight`, `_1_cup_of_coffee`, `_seek_bright_light_outdoors`, `_1_cup_of_filter_coffee`, `_in_15_min`, `_tokyo`, `_kl_861`, `_23_0007_00`, `_08_0010_00`, `_09_30`, `_14_4009_15`, `_11_00`, `_10_45` | as the key | Slapen, Zoek fel licht op, Vermijd licht, Cafeïne, Vlucht, 1 kop koffie, Zoek buiten fel licht op, 1 kop filterkoffie, Over 15 min, Tokio; times and `KL 861` unchanged |
| `walkthrough_done_total_done` | `%1$s/%2$s done` | `%1$s/%2$s gedaan` |
| `walkthrough_time_zone_relative` | `%1$s · %2$s · %3$s` | same |
| `walkthrough_title_time` | `%1$s · %2$s` | same |
| `terms_before_you_continue` | Before you continue | Voordat je verdergaat |
| `terms_we_ve_updated_our_terms` | We've updated our terms | We hebben onze voorwaarden bijgewerkt |
| `terms_please_take_a_moment_to_read_what_c_52b3c0` | Please take a moment to read what changed. You'll need to agree before carrying on. | Lees even wat er is veranderd. Je moet akkoord gaan om verder te gaan. |
| `terms_what_changed` | What changed | Wat is er veranderd |
| `terms_chronoflow_helps_you_plan_sleep_lig_30819d` | ChronoFlow helps you plan sleep, light and caffeine around your trip. | ChronoFlow helpt je slaap, licht en cafeïne rond je reis te plannen. |
| `terms_chronoflow_gives_general_timing_gui_de4deb` | ChronoFlow gives general timing guidance, not medical advice. Check with a doctor if you have a health condition, are pregnant or take prescription medicine. | ChronoFlow geeft algemene timingadviezen, geen medisch advies. Overleg met een arts als je een aandoening hebt, zwanger bent of medicijnen op recept gebruikt. |
| `terms_your_account_stores_your_trips_prof_64f9bd` | Your account stores your trips, profile and preferences. | Je account bewaart je reizen, profiel en voorkeuren. |
| `terms_read_the_full_terms_and_privacy_policy` | Read the full terms and privacy policy | Lees de volledige voorwaarden en het privacybeleid |
| `terms_couldn_t_save_your_answer` | Couldn't save your answer. | Je antwoord kon niet worden opgeslagen. |
| `terms_check_your_connection_and_try_again_14d498` | Check your connection and try again. You can keep reading the policy meanwhile. | Controleer je verbinding en probeer het opnieuw. Je kunt de voorwaarden intussen blijven lezen. |
| `terms_accept_and_continue` / `terms_try_again` | Accept and continue / Try again | Akkoord en verder / Opnieuw proberen |
| `terms_sign_out` / `terms_something_went_wrong` | Sign out / Something went wrong | Uitloggen / Er ging iets mis |
| `terms_your_trips_stay_saved_if_you_sign_out` | Your trips stay saved if you sign out. | Je reizen blijven bewaard als je uitlogt. |

No new copy is added. If a key is missing from the generated XML, stop: that is a 0c defect, not something to patch here.

### 5.2 Session seam (`…/onboarding/OnboardingSession.kt`) [R, from F `WalkthroughDestination.swift`, `TermsDestination.swift`]

`:feature:onboarding` cannot see `:app` (`AppGraph`, `SessionGraph`). It declares what it needs, and `:app` supplies it:

```kotlin
class SessionChangedException : Exception()          // NOT a CancellationException

class OnboardingSession(
    val snapshot: SessionSnapshot,
    val enqueue: suspend (OutboxChange) -> Unit,
    val policies: Flow<AccountPolicies?>,
    val signOut: suspend (discardPending: Boolean) -> SignOutResult,
) {
    companion object {
        fun from(controller: SessionController, snapshot: SessionSnapshot, permit: SessionMutationPermit,
                 outbox: OutboxRepository, settings: SettingsRepository, json: Json): OnboardingSession
    }
}

fun interface OnboardingSessions { fun current(): Flow<OnboardingSession?> }
```

- `from(...).enqueue(change)` does two steps:
  1. If `controller.activeSession() != snapshot`, throw `SessionChangedException()`.
  2. Call `outbox.enqueue(change, permit)`. The repository already runs on its IO writer; never wrap it in `runBlocking`. An invalidated permit makes it throw 4a's `StaleSessionException`, which **is a `CancellationException`** (4a l.165, iOS `CancellationError`); §5.3 and §5.4 say how the models treat it.
- `policies` = `settings.values()` mapped to the record with `section == "policies"`. `SettingsRecord.value` is JSON **text** (4a l.215), so decode with `ApiJson.response.decodeFromString(AccountPolicies.serializer(), record.value)` (the decoder `AccountReadinessRepository` uses; there is no app `Json`). A missing record gives `null`. Apply `distinctUntilChanged()`. Collect it on the repository's flow as is (4a observes on IO).
- `signOut(discard)` = `controller.signOut(expected = snapshot, discardPending = discard)`.
- **`:app` implementation** (`…/app/onboarding/AppOnboardingSessions.kt`): `combine(controller.updates, graph.sessionGraphs.current) { _, g -> g }.mapLatest { g -> resolve(g) }`. `resolve` does four things:
  1. `val s = controller.activeSession() ?: return null`;
  2. require `g != null && g.epoch == s.epoch`, else `null`;
  3. `val p = controller.mutationPermit(s) ?: return null`;
  4. return `OnboardingSession.from(controller, s, p, g.<outbox>, g.<settings>, json)`.

  There is **no polling** (iOS polls every 100 ms; Android reacts to both flows). Register it in `AppGraph` and expose it to ViewModel factories the way Trips and Today do: a feature-declared `object OnboardingSessionsKey : CreationExtras.Key<OnboardingSessions>`, set by `:app` into `MutableCreationExtras(owner.defaultViewModelCreationExtras)` (see `TripsDependenciesKey` and `AppShell.kt`). Use the same mechanism; do not invent a second one. `SessionGraph` already carries `snapshot`, `outbox` and `settings`.

### 5.3 `WalkthroughModel` (`…/onboarding/walkthrough/WalkthroughModel.kt`) [F `WalkthroughModel.swift`]

```kotlin
class WalkthroughModel(
    val mode: Mode = Mode.FirstRun,
    private val complete: suspend () -> Unit,
    private val onFinish: () -> Unit = {},
) {
    enum class Mode { FirstRun, Replay }
    enum class Page { Timeline, Categories, Progress }          // ordinal 0, 1, 2
    data class State(val page: Page = Page.Timeline, val busy: Boolean = false,
                     val completed: Boolean = false, val failure: Throwable? = null)
    val state: StateFlow<State>
    suspend fun advance()
    suspend fun skip()
}
```

- `advance()`: if `busy || completed`, return. If a next page exists, go to it. Otherwise call `skip()`.
- `skip()`: if `busy || completed`, return. Set `busy = true`; a `finally` block always resets `busy = false`.
  - First run: `complete()`. Replay never calls it.
  - Success sets `failure = null` and `completed = true`, then calls `onFinish()`.
  - Catch order: `catch (e: StaleSessionException) { failure = e }` **first** (a revoked permit is a session change, not the caller's cancellation; iOS catches its `CancellationError` into `failure`), then `catch (e: CancellationException) { throw e }`, then `catch (e: Exception) { failure = e }` (this includes `SessionChangedException`).
- It is called on the main thread only (from `viewModelScope`). It is pure Kotlin with no Android import, so it is JVM-testable.

### 5.4 `TermsUpdateModel` (`…/onboarding/terms/TermsUpdateModel.kt`) [F `TermsUpdateModel.swift`]

```kotlin
class TermsUpdateModel(policies: AccountPolicies? = null, private val save: suspend (version: String) -> Unit) {
    data class State(val policies: AccountPolicies?, val busy: Boolean = false,
                     val completed: Boolean = false, val failed: Boolean = false) {
        val canAccept: Boolean get() = policies != null && !busy && !completed &&
            policies.acceptedTermsVersion != policies.termsVersion
    }
    val state: StateFlow<State>
    fun update(policies: AccountPolicies?)   // if termsVersion changed: completed = false, failed = false; then store
    suspend fun accept()
}
```

`accept()` runs in this order:
1. If `!canAccept`, return. Capture `p = policies`. Set `busy = true` and `failed = false`; a `finally` resets `busy = false`.
2. Call `save(p.termsVersion)`. On success, set `completed = (state.policies?.termsVersion == p.termsVersion)`.
3. In this order: `catch (SessionChangedException)` and `catch (StaleSessionException)`: nothing (iOS `catch is CancellationError {}`; the iOS fixture saves straight through the permit, so `revokedPermitCannotAcceptForAnotherSession` reaches this through `StaleSessionException`). `catch (CancellationException)`: rethrow. `catch (Exception)`: `failed = true`.

### 5.5 Geometry (pure, `:feature:onboarding`) [F `WalkthroughGeometry.swift`, `TermsGeometry.swift`; R the 560 dp column]

Both are `data class …Geometry(contentLeft, contentTop, contentWidth, contentHeight, top, bottom, leading, trailing)` in dp floats, built from `(widthDp, heightDp, insets: left/top/right/bottom dp)`:

- **Walkthrough:**
  - `top = insets.top + space2 (8)`;
  - `bottom = max(insets.bottom, space8 (32))`;
  - `leading = insets.left + space4 (16)`;
  - `trailing = insets.right + space4 (16)`.
- **Terms:**
  - `top = insets.top + space6 (24)`;
  - `bottom = insets.bottom + space8 (32)`;
  - `leading` and `trailing` as for the walkthrough.
- **Both, [R]:** `available = max(0, width − leading − trailing)`.
  - If `width >= WindowPolicy.wideBreakpointDp` (600, 6a `…designsystem.layout`): `contentWidth = min(available, OnboardingMetrics.wideColumn)` (560), centred inside `[leading, width − trailing]`.
  - Otherwise: `contentWidth = available`.
  - `contentHeight = max(0, height − top − bottom)`. There is no `contentBottom` field; tests use `contentTop + contentHeight`.
- Constants come from `PlanUITokens` (`space2/4/6/8`). The 560 goes into `@BoardMetrics("Walkthrough", "TermsUpdate") internal object OnboardingMetrics` (`…/onboarding/OnboardingMetrics.kt`, `const val wideColumn = 560f`, KDoc citing plan "Android spec: window policy" l.357–371), the same pattern as 5a's `AuthMetrics` (5a l.109; `AuthMetrics` is `internal` to `:feature:auth`, so it cannot be reused). Every other board-only dp of this phase also lives there (Konsist `featureDpLiteralsComeFromBoardMetrics`).
- In compose, the screens compute this from `BoxWithConstraints` (`maxWidth`, `maxHeight`) and `LocalSafeInsets.current`. **Never** from `onSizeChanged`, and never from `WindowInsets.*` directly (Konsist `insetsOnlyAtRoot`).

### 5.6 Walkthrough screen (`…/onboarding/walkthrough/WalkthroughScreen.kt`, `WalkthroughPreview.kt`) [F `WalkthroughView.swift`, `WalkthroughPreview.swift`, `DC/Walkthrough.dc.html`]

The layout is a full-size `Box` with `background(colors.background)`. Inside it sits a `Column` placed by the geometry, testTag `walkthrough.screen`, with semantics that keep its children. The column has three parts.

**Header** (fixed): a `Row`, `heightIn(min = PlanUITokens.actionMinHeight)` (48), vertically centred.
- **Counter:** `stringResource(walkthrough_current_of_total, page + 1, 3)` uppercased; `type.screenEyebrow` (600 12/16, +0.1 em) with tabular numbers; `colors.dawnText`; `padding(start = space1)` (4); tag `walkthrough.counter`.
- **Spacer:** `Modifier.weight(1f)`, with a minimum of `space2`.
- **Skip:** `CfButton(text = Skip, style = Ghost, size = Sm, minHeight = AuthTokens.touch.dp (44))`. The label is `authLabel`, the horizontal padding 12, and the touch target ≥ 48 dp. Tag `walkthrough.skip`. It is disabled while `busy || completed`.

**Middle** (`weight(1f)`, scrolls): a `BoxWithConstraints` around a `Column(Modifier.verticalScroll(key(page) { rememberScrollState() }))`, so the scroll resets per page as with iOS `.id(model.page)`. Inside it, a `Column(Modifier.fillMaxWidth().heightIn(min = viewportHeight), verticalArrangement = spacedBy(walkthroughContentGap = 28, Alignment.CenterVertically))` holds the preview card and the text:

- **Preview card** (`WalkthroughPreview`):
  - Shape and surface: padding `space5` (20); `RoundedCornerShape(radius3Xl = 26)`; modifier order `softShadow(shape, CfTheme.colors.isDark)` → `background(colors.card, shape)` → `border(AuthTokens.borderWidth.dp (1), colors.border, shape)` (Phase 2 l.294: `softShadow` takes `dark` explicitly).
  - Content: a vertical stack with gap `walkthroughCopyGap` (10).
  - Semantics: **`clearAndSetSemantics {}`** and no click handling (decorative, iOS `accessibilityHidden(true).allowsHitTesting(false)`).
  - **Page 1, timeline:** four blocks (type, title, time; `WalkthroughPreview.swift:41–44`):
    - `sleep`, "Sleep", `23:00–07:00`;
    - `seek_light`, "Seek bright light", `08:00–10:00`;
    - `caffeine_serving`, "1 cup of coffee", `09:30`;
    - `flight`, "KL 861" (the title is the flight number, not "Flight"), `14:40–09:15`.

    Each block is a `Box`:
    - `fillMaxWidth().heightIn(min = walkthroughBlockHeight (64))`, padding 6 (`space1 + space1/2`);
    - fill = the category fill;
    - clip `RoundedCornerShape(radiusLg = 14)`, or `radiusFull` for caffeine.

    Decoration:
    - every block except caffeine gets a 4 dp (`space1`) bar on the left edge, in `colors.foreground` at alpha `priorityCoreOpacity` (0.7);
    - caffeine gets the dot pattern over the whole block: category foreground at `patternDotOpacity` (0.16), dots of diameter `patternDotDiameter` (2) on a `patternDotSpacing` (7) grid, drawn with `drawWithCache` (no allocation per frame).

    Content: when the block's available width is `< walkthroughCompactWidth` (640), show the **compact** form: only the category glyph, 14 dp (`space3 + space1/2`), centred, in the category foreground. Otherwise show the full form: glyph plus title (`screenBlockLabel` 600 11/13.75; caffeine shows `walkthrough_title_time`), then the time on a second line except for caffeine. With the 560 dp column, every Android width is compact (OQ-8-4); implement both, as iOS does.
  - **Page 2, categories:** five rows for `sleep`, `seek_light`, `avoid_light`, `caffeine_serving` and `flight`, labelled Sleep, Seek bright light, Avoid light, Caffeine and Flight. Each row is a `Row(spacedBy(space3 = 12), heightIn(min = AuthTokens.touch = 44), centred)` holding 6a's `CategorySwatch(type)` (`CategorySwatchSize.Regular`, 32 dp) and the label in `type.walkthroughBody` (500 16/24), `foreground`.
  - **Page 3, progress:**
    - A progress row with `padding(top = space1)`, flowing with gap `space2` (8) (a `FlowRow`, iOS `PlanFlowRow`): `CfLinearProgress(progress = { 3f / 7f }, Modifier.width(progressTrackWidth = 80))` (Phase 2: dawn on muted, 4 dp), then a `Row(spacedBy(space2))` holding the lucide `check` at 12 dp (`space3`) and `walkthrough_done_total_done("3", "7")`, all in `screenMeta` (12/16), `mutedForeground`, with tabular numbers. (iOS `DayProgressRow` reads the same English key from the plan table; the walkthrough table has the identical key and Dutch value, so use `walkthrough_done_total_done`.)
    - Two rows, each `Row(spacedBy(space3), heightIn(min = space14 = 56), padding(space2 = 8))` with `CategorySwatch` and a `Column` of the title (`walkthroughBody`, `foreground`) and a meta line `walkthrough_time_zone_relative(time, Tokyo, relative)` (`screenBody` 14/20, `mutedForeground`, tabular numbers):
      - seek_light "Seek bright light outdoors", `11:00`, "In 15 min";
      - caffeine_serving "1 cup of filter coffee", `10:45`, relative = `walkthrough_done` ("Done" / "Klaar").
- **Text** (`padding(horizontal = space1)`, gap `walkthroughCopyGap` (10)):
  - title in `type.screenPageTitle` (Fraunces opsz 32, 30/36, −0.3), `foreground`, `semantics { heading(); liveRegion = LiveRegionMode.Polite }`, tag `walkthrough.title` (the iOS focus move; AGENTS "Announcements");
  - body in `type.screenBodyLg` (16/24), `mutedForeground`.

**Footer** (fixed): a `Column(spacedBy(space5 = 20))` with the dots and the button.
- **Dots:** `Row(spacedBy(space2, CenterHorizontally))`, `clearAndSetSemantics {}`. Each dot is 8 dp high (`space2`) with a pill shape. The current dot is 24 dp wide (`space6`) in `dawn`; the others are 8 dp in `border`. The width animates over `(walkthroughDotDuration * 1000).toInt()` ms with `EaseInOut`. Under `LocalReduceMotion.current` it snaps (`snap()`). Read the animated width in the layout phase (`Modifier.layout` or `width { }`), not in composition.
- **Button:** `CfButton(text = Next | Done, style = Primary, size = Lg (40 visual = walkthroughNextHeight/DoneHeight), fillWidth = true)`, with a ≥ 48 dp target and tag `walkthrough.next` (pages 1–2) or `walkthrough.done` (page 3). It is disabled while `busy || completed`.

**Behaviour:**
- Next calls `model.advance()`, Skip calls `model.skip()`, Done calls `model.advance()`. Each runs in `viewModelScope`.
- **First run:** the screen never navigates. The 4e `SessionGateRouter` sees the gate turn `App` (the pending walkthrough row counts as seen) and calls `rootNavigator.navigate(Route.Today)`.
- **Replay:** when `completed` turns true, the screen calls its `onFinish()` parameter exactly once (`LaunchedEffect(completed)`). `BackHandler(enabled = mode == Replay)` calls `model.skip()`, so system and predictive back behave like Skip with no write. (It is composed inside the tab's `NavDisplay` entry, so it wins over 6a's `NavDisplay(onBack = holder::back)`.)
- **First-run back:** no `BackHandler`. The root back stack finishes or backgrounds the activity (4e `RootContent`); the gate is unchanged on the next start (OQ-8-5).
- **Loading:** until `OnboardingSessions.current()` emits a non-null value (first run only), render only the background. There is no spinner and no board for it (iOS parity).

`WalkthroughViewModel(replay: Boolean, sessions: OnboardingSessions)`:
- **Replay:** creates `WalkthroughModel(Mode.Replay, complete = {})` at once.
- **First run:** collects `sessions.current().filterNotNull().first()`, then creates `WalkthroughModel(FirstRun, complete = { session.enqueue(OutboxChange.Walkthrough) })`.
- It exposes `state: StateFlow<WalkthroughUiState?>` (`null` = loading). The page survives rotation, fold and locale/font recreation (ViewModel). After process death the app cold-launches and starts again on page 1, as on iOS.

### 5.7 Terms screen (`…/onboarding/terms/TermsScreen.kt`) [F `TermsUpdateView.swift`, `DC/TermsUpdate.dc.html`, `TermsTokens`]

A full-size `Box` with `background(colors.background)`, tag `terms.screen` (semantics keep children). Inside it, `BoxWithConstraints` and a `Column(Modifier.verticalScroll(rememberScrollState()))` padded by `TermsGeometry`, wrapping `Column(Modifier.heightIn(min = contentHeight), verticalArrangement = Arrangement.SpaceBetween)` with **two groups**. Never use `weight` inside the scroll.

**Top group** (`spacedBy(space5 = 20)`):
1. **Header:** `Row(spacedBy(AuthTokens.bannerGap = 10), heightIn(min = AuthTokens.touch = 44), centred)`.
   - `Image(painterResource(R.drawable.terms_brand), null)` at `TermsTokens.brandSize` (28) square.
   - The eyebrow `terms_before_you_continue` uppercased, in `screenEyebrow`, `dawnText`.
2. **Heading:** `Column(spacedBy(space2 = 8))`.
   - Title `terms_we_ve_updated_our_terms` in `type.termsTitle` (Fraunces opsz 32, 32/38), `foreground`, `heading()`, tag `terms.title`.
   - Body `terms_please_take…` in `screenBodyLg`, `mutedForeground`.
3. **Summary:** `Column(spacedBy(space3 = 12))`, padding `TermsTokens.summaryPadding` (18), background `colors.surface2` in `RoundedCornerShape(radius2Xl = 22)`.
   - Heading `terms_what_changed` uppercased, in `screenLabel`, `mutedForeground`, `heading()`.
   - Three bullets, each a `Row(spacedBy(space3), verticalAlignment = Top)`: a dot of `TermsTokens.bulletSize` (6) in `colors.dawn` with `padding(top = space2)` and no semantics, then the text in `type.termsStrong` (600 15/22), `foreground`.
   - **Policy row:** `padding(top = space1)`.
     - Shape and size: `Row(spacedBy(space3), centred)`, padding horizontal `AuthTokens.bannerHorizontal` (14) and vertical `space2` (8), `heightIn(min = TermsTokens.policyHeight = 52)`, background `colors.card` in `RoundedCornerShape(space4 = 16)`.
     - Content: the lucide `file-text` at 20 dp (`space5`) in `foreground`; the label `terms_read_the_full…` in `type.termsBody`, `foreground`, `weight(1f)`; the lucide `chevron-right` at 18 dp (`OnboardingMetrics.policyChevron = 18`, board `TermsUpdate` svg `width="18"`; no token) in `mutedForeground`. Both icons have no semantics.
     - Interaction: `cfClickable(onClick = …, role = Role.Button, focusShape = RoundedCornerShape(space4))`, tag `terms.policy`. A tap calls `PolicyLinks.open(context, PolicyLinks.TERMS, CfTheme.colors.isDark)` (§5.9), with `context = LocalContext.current` (the activity). It stays enabled in every state ("You can keep reading the policy meanwhile").

**Bottom group** (`spacedBy(space5)`, with `padding(top = space5 + space5)`). `SpaceBetween` adds nothing at minimum height, so this padding alone keeps the summary and the banner/actions at least 40 dp apart, as iOS (`VStack(spacing: 20)` around a `Spacer(minLength: 0)`, `TermsUpdateView.swift:29–33`) and the board (`gap: 20px` around a `flex: 1` div) both do:
1. **Failure banner**, only when `failed`: `CfBanner(title = terms_couldn_t_save_your_answer, message = terms_check_your_connection…, testTag = "terms.failure")`. `CfBanner` already sets a polite live region.
2. **Actions:** `Column(spacedBy(space2 = 8))`.
   - When the sign-out failed: `terms_something_went_wrong` in `screenBody`, `colors.destructive`, tag `terms.signOutFailure`.
   - Accept: `CfButton(text = if (failed) Try again else Accept and continue, style = Primary, size = Lg, fillWidth = true, minHeight = PlanUITokens.actionMinHeight.dp (48))`, tag `terms.accept`, `enabled = canAccept && !signOutBusy`.
   - Sign out: `CfButton(text = Sign out, style = Ghost, size = Lg, fillWidth = true, minHeight = 48.dp from actionMinHeight)`, tag `terms.signOut`, `enabled = !busy && !completed && !signOutBusy`.
   - Footnote `terms_your_trips_stay_saved…` in `screenMeta`, `mutedForeground`, `TextAlign.Center`, `fillMaxWidth`, `padding(top = space1)`.
3. **Sign-out confirmation slot:** `signOutConfirmation: @Composable (count: Int, onStay: () -> Unit, onDiscard: () -> Unit) -> Unit` is a parameter of `TermsScreen`. It is invoked only while `pendingSignOutCount != null`; §5.8 says who fills it.

`TermsViewModel(sessions: OnboardingSessions)`:
- It collects the first non-null session. It builds `TermsUpdateModel(save = { v -> session.enqueue(OutboxChange.Policies(v)) })` and then collects `session.policies` into `model.update(...)`.
- It exposes `state: StateFlow<TermsUiState?>` (`null` = loading, background only). `TermsUiState(policies, busy, completed, failed, canAccept, signOutBusy, signOutFailed, pendingSignOutCount: Int?)`.
- `accept()` calls `model.accept()`. There is no navigation: the gate router moves to `Walkthrough` or `Today`.
- `signOut(discardPending: Boolean = false)` is a port of `SettingsHomeModel.signOut` (`:123–160`):
  - Return if `signOutBusy`. Also return when `discardPending && pendingSignOutCount == null`.
  - Set `signOutBusy = true` and `signOutFailed = false`, then call `session.signOut(discardPending)`.
  - `SignedOut`: nothing; the 4e router's `onSignedOut` navigates to `SignIn`.
  - `NeedsConfirmation(n)`: `pendingSignOutCount = n`.
  - Any non-cancellation exception: `signOutFailed = true`.
  - A `finally` resets `signOutBusy = false`.
- `stay()` sets `pendingSignOutCount = null`; it does nothing while `signOutBusy` (iOS `stay()` guards `!busy`).
- Back: no `BackHandler`. It is a root route, so back finishes or backgrounds the activity, and the gate asks again on the next start.

### 5.8 Routing (`:app`, `:core:navigation`) [F 4e §5.4/§5.8; R]

- `RootContent` entries:
  - `Route.Terms` → `TermsScreen(viewModel, signOutConfirmation = …)`;
  - `Route.Walkthrough` with `replay == false` → `WalkthroughScreen(viewModel(replay = false), onFinish = {})`;
  - a root `Walkthrough(replay = true)` is not produced by any code path. Treat it like replay with `onFinish = { rootNavigator.navigate(Route.Settings) }` and add a debug `Log.w`.

  The `RoutePlaceholder` seam stays for every other unbuilt route.
- **Entry-scoped ViewModels at the root.** *(Stack 2026-11-20: already done. `RootContent` holds both decorators and keys `Terms`/`Walkthrough` by revision; the route `when` is in `CfRoot.kt` l.137–230. Skip this bullet.)* 4e's `RootContent` has no ViewModel entry decorator, so a plain `viewModel()` there is activity-scoped (OQ-5a-5) and a second sign-in would reuse the first account's `TermsViewModel`/`WalkthroughViewModel`. If the merged `RootContent` `NavDisplay` does not yet hold `entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator())` (5d adds them, OQ-5d-14; 5d runs in the same wave), add exactly those two in Task 6 and run 5a's `SignIn*` test classes; they must pass unchanged.
- **Replay** lives on a **tab back stack** (6a, iOS `navigationDestination` from Preferences). Add a `Route.Walkthrough` entry to the shell's tab entry provider (`AppShell`'s `page` lambda; `holder` is `ShellViewModel`): `WalkthroughScreen(viewModel(replay = true), onFinish = { holder.back() })`. It is pushed with 6a's `holder.push(Route.Walkthrough(replay = true))` (`ShellNav.push` appends any non-`Wizard` route to the selected stack), so 6a's `rememberViewModelStoreNavEntryDecorator` clears its ViewModel on pop and each replay starts fresh.
- **Hide the shell chrome while replay is on top** (iOS `.toolbar(.hidden, for: .navigationBar, .tabBar)`, `SettingsPreferencesView.swift:144`; A8 "walkthrough and terms replace the shell"):
  - add `fun Route.hidesShellChrome(): Boolean` in `:core:navigation` (true for `Walkthrough`, false otherwise; later phases extend it);
  - add `None` to 6a's `enum class CfNavLayout { Bar, Rail }` in `CfNavigationSuiteScaffold`, mapped to the scaffold's explicit layout type `NavigationSuiteType.None` (a layout choice, not the hide/show state API 6a forbids; use the 1.4.0 name 6a verified);
  - in `AppShellScaffold` (l.57–62), pick `CfNavLayout.None` while `holder.state.stacks.getValue(selected).last().hidesShellChrome()`, else 6a's Bar/Rail rule;
  - with `None` there is no bar or rail to consume the bottom and side insets, so `AppShellScaffold` (`contentInsets`, l.64–70) re-provides the **full** `LocalSafeInsets` (all four sides) to the content instead of 6a's `only(Top + Horizontal)` / `only(Top + End + Bottom)`.
- **Sign-out confirmation slot.** If 10a's `SignOutPendingSheet` (or equivalent) is already in the stack, pass it from `:app`. Otherwise pass `{ _, _, _ -> }` and leave a `// 10a wires SignOutPending here` comment in `:app` only (OQ-8-2). Never build a substitute sheet.
- **Update the 4e tests that asserted seams:** `LaunchGateRoutingTest` was deleted by the test diet (stack check). Only `RootContentTest.recreationKeepsTheProcessRouteWithoutAnotherNavigation` reaches the real `Terms` dispatch: switch it to `Route.GuestWizard`. Re-check with `git grep -n 'route.terms\|route.walkthrough' app core feature`.
- **Robolectric `:app` tests** must not build a real `AppGraph` or launch `MainActivity` (Konsist `unitTestsKeepAppBootsAtApprovedBoundaries`); if an allowlisted test does, it passes `GraphOverrides(sessionCipher = SoftwareSessionCipher())` (4e OQ-4e-17; Robolectric has no AndroidKeyStore).

### 5.9 Policy links (`…/ui/links/PolicyLinks.kt`) [F A23, OQ-A32; R]

5d (landed) built `PolicyOpener`/`CustomTabPolicyOpener` in **`:feature:auth`** (`feature/auth/…/auth/signup/PolicyOpener.kt`). `:feature:onboarding` cannot depend on a feature (Konsist), so Phase 8 builds this `:core:ui` helper whether or not 5d is in the stack, with the **same** behaviour as 5d's opener, and does not touch 5d's code (OQ-8-10). If a `:core:ui` helper with this job already exists (`grep -rn CustomTabsIntent core`), reuse it instead and skip Task 1.

```kotlin
object PolicyLinks {
    const val TERMS = "https://chronoflow.net/terms"
    const val PRIVACY = "https://chronoflow.net/privacy"
    fun open(context: Context, url: String, dark: Boolean, start: (Intent) -> Unit = context::startActivity)
}
```

- **Dependency:** the catalog already has `androidx-browser` 1.10.0 (5d): only add `implementation(libs.androidx.browser)` to `:core:ui`. (The fallback rule, now unused: add it with 5d's rule (l.145, OQ-5d-6): the newest **stable** `androidx.browser:browser` on Google Maven, ≥ 1.8.0, released at least two weeks ago (no alpha, beta or RC); record the version in plan A1's AndroidX list in the same commit.)
- `CustomTabsIntent.Builder().setShowTitle(true).setColorScheme(if (dark) COLOR_SCHEME_DARK else COLOR_SCHEME_LIGHT).build()`; set `customTabs.intent.data = url.toUri()` and call `start(customTabs.intent)` (equivalent to `launchUrl`, but testable; 5d does the same). No toolbar colour: iOS opens the system browser and 5d sets none, so there is no source for one. The caller passes `CfTheme.colors.isDark`.
- `catch (ActivityNotFoundException)`: retry with `start(Intent(ACTION_VIEW, uri))`. If that also throws `ActivityNotFoundException`, do nothing (no board). Catch only that exception type.
- No `<queries>` and no warm-up/`bindCustomTabsService`. Confirm that the merged release manifest gains **no permission** (`:app:verifyReleaseManifest`).
- The URL is the same for nl (iOS parity; OQ-8-3).

### 5.10 `CategorySwatch` (6a's, reused) [F `PlanComponents.swift:7–98`; 6a l.356–367, OQ-6a-12]

6a, which this phase depends on, already ports the swatch as `CategorySwatch(type: BlockType, modifier: Modifier = Modifier, size: CategorySwatchSize = CategorySwatchSize.Regular)` at `core/ui/…/ui/plan/CategorySwatch.kt:70` (package `net.chronoflow.core.ui.plan`), with Regular 32 dp / Small 28 dp, the iOS fill, foreground, dot/hatch and dashed mapping, and a `private` `CategorySwatchMetrics` (`@BoardMetrics("Trips")`). 6d reuses it too (6d l.111, l.325). **Phase 8 builds no swatch.** Use it as merged (`grep -rn "fun CategorySwatch\|enum class CategorySwatchSize" core/ui`); if its name or signature differs, use the merged one and say so in the report. Do not add a second swatch anywhere.

### 5.11 Seeds (debug only, `app/src/debug/…`) [F `AuthUITestConfiguration.swift:28`, `AppServices.swift:63`, `TermsUITestSeed.swift`]

- **Parse two new keys**, gated by `CF_AUTH_TEST_ID` like the others:
  - `CF_WALKTHROUGH_TEST_PENDING` (`== "1"`);
  - `CF_AUTH_TEST_TERMS_CHANGES_PENDING` (`== "1"`).
- **Hold sends offline:** pass `mutationsOffline = walkthroughPending || termsPending` to the mock (4c's `MockDataSource(mutationsOffline = …)`), where `walkthroughPending` is the new `CF_WALKTHROUGH_TEST_PENDING` and `termsPending` is the **existing 4e gate seed `CF_AUTH_TEST_TERMS_PENDING`** (`MockSeedFlags.termsPending`). This is exactly iOS `AppServices.swift:63` (`settingsPending || walkthroughPending || termsPending`, with `termsPending = environment["CF_AUTH_TEST_TERMS_PENDING"]`, `AuthUITestConfiguration.swift:29`). `CF_AUTH_TEST_TERMS_CHANGES_PENDING` does **not** feed this flag. If 10a later adds `CF_SETTINGS_TEST_PENDING`, it ORs into the same flag. 4e's `GraphOverrides` (`app/…/app/GraphOverrides.kt`) has no such field: add `val mutationsOffline: Boolean = false` to it and pass it where the mock is built (`PerfMockSources.dataSource`, `app/src/perf/…/perf/PerfMockSources.kt:52`).
- **Seed one pending change for sign-out tests:** with `CF_AUTH_TEST_TERMS_CHANGES_PENDING=1`, after the signed-in seed and only when the outbox is empty, enqueue `Settings(Preferences, {"time_format":"12"})` through the seeded session's permit (in `SeedRunner.reseed`, after `MockSessionSeed.run`). iOS does this in its Terms destination once the permit exists (`TermsUITestSeed.swift`, called from `TermsDestination.swift`); Android does it in the seed because `:feature:onboarding` must not carry debug code [R]. This is for 10a's sign-out journey; this phase only tests the parse and the single row.

## 6. Tasks

Each task is one atomic Conventional Commit with no trailer. Tick the `plan.md` boxes in the commits named.

### Task 1: `feat(ui): add policy links that open in a custom tab`
- **Files:** `core/ui/build.gradle.kts` (`implementation(libs.androidx.browser)`; the catalog entry exists), `…/ui/links/PolicyLinks.kt` (§5.9).
- **Test:** `core/ui/src/test/…/links/PolicyLinksTest.kt` (`@Logic`, Robolectric, a `ComponentActivity` from `Robolectric.buildActivity` as the context, a recording `start` seam; Robolectric does not throw for unresolvable intents by default, so failures are injected through the seam, as in 5d's `PolicyOpenerTest`):
  - `opensTermsInACustomTab`: the recorded intent has `ACTION_VIEW`, data `https://chronoflow.net/terms`, and the extra key `CustomTabsIntent.EXTRA_SESSION` (`android.support.customtabs.extra.SESSION`).
  - `colorSchemeFollowsTheTheme`: with `dark = true` the intent's `CustomTabsIntent.EXTRA_COLOR_SCHEME` is `COLOR_SCHEME_DARK`; with `false`, `COLOR_SCHEME_LIGHT`.
  - `fallsBackToAPlainViewIntent`: a seam that throws `ActivityNotFoundException` on the first call records a second `ACTION_VIEW` intent with the same data and no session extra.
  - `fallsBackWithoutCrashingWhenNothingHandlesIt`: a seam that always throws `ActivityNotFoundException`; `open` returns normally.
- Skip this task (and say so) only if a `:core:ui` helper already exists (§5.9). 5d's `:feature:auth` opener does not count.

### Task 2: `feat(tools): add the file-text lucide glyph`
- Add `"file-text": {"source": "board", "board": "TermsUpdate", "first": "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"}` under `"glyphs"` in `tools/icons/lucide.json` (Phase 2 l.329 format; the board svg has five paths), run `make generate`, and commit the regenerated `LucideGlyphs.generated.kt`.
- Update the glyph counts in `LucideGlyphsTest` and `tools/icons/test_generate_icons.py` (43 → 44) and assert `LucideGlyphs.FileText.paths.size == 5`.
- `make generators-check` passes.

### Task 3: removed
6a already ships `CategorySwatch` (§5.10). There is no swatch commit in this phase; task numbers below are kept so the cross-references stay valid.

### Task 4: `feat(onboarding): port the walkthrough model, geometry and session seam`
- **Files:** `…/onboarding/OnboardingSession.kt` (§5.2, everything except the `:app` part), `walkthrough/WalkthroughModel.kt` (§5.3), `walkthrough/WalkthroughGeometry.kt` and `OnboardingMetrics.kt` (§5.5).
- **Module deps** (`feature/onboarding/build.gradle.kts`):
  - `implementation`: `:core:sync`, `:core:data` (the feature plugin already adds `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:model`);
  - `testImplementation`: `:core:testing`, `:core:testing-compose`, `:core:data-mock`, `:core:database-testing` (`JdbcAccountDriverFactory`), coroutines-test, Turbine.

  No `:feature:*` dependency (Konsist).
- **Fixture:** `feature/onboarding/src/test/…/support/OnboardingFixture.kt`, a port of `WalkthroughFixture`/`TermsFixture`:
  - a temp root, an in-memory credentials store, the 4a `SessionAccountDatabase` over the JDBC driver (reuse `:core:database-testing`'s `JdbcAccountDriverFactory`), `SessionController` with an injectable `SessionTeardown` (`flush = { throw DataFailure.Offline }`, wipe through the 4e `AccountWiper`);
  - `MockDataSource(seed = MockAppSeed.make(now, walkthroughSeen, termsPending), sleep = {})`;
  - `signIn(demo@example.com / password, deviceId "019a562b-8000-7000-8000-000000000099")`, then `accept`, `snapshot`, `database`, `permit`;
  - `session()` = `OnboardingSession.from(...)`;
  - `walkthroughModel()` = `WalkthroughModel(complete = { outbox.enqueue(OutboxChange.Walkthrough, permit) })` and `termsModel()` = `TermsUpdateModel(account.policies) { v -> outbox.enqueue(OutboxChange.Policies(v), permit) }`: straight through the permit, exactly as the iOS fixtures' `model()` (`WalkthroughTests.swift:52–54`, `TermsAcceptanceTests.swift:61–65`), so a revoked permit reaches the models as `StaleSessionException`;
  - `gate()` = the first `AccountReadinessRepository.values(accountId)` value's `gate`;
  - `close()`.

  Use `runTest(timeout = 60.seconds)`.
- **Tests** (`feature/onboarding/src/test/…/walkthrough/`, JVM; keep the Swift names):
  - `WalkthroughModelTests`:
    - `nextNextDoneCommitsOnceAndGateMovesWithoutAnyNetworkRequest`: the source is offline; the pages go Timeline → Categories → Progress; no row before Done; after Done `completed && !busy && failure == null`, `gate == App`, exactly 1 pending row of kind `walkthrough` that decodes to `OutboxChange.Walkthrough`, and `account.walkthroughSeen`. Then `skip()`, `advance()` and a direct `enqueue(Walkthrough)` leave the same row ids.
    - `skipFromEveryPage`: parameterized over the 3 pages; `completed` and `gate == App`.
    - `failedFlushKeepsCompletionAndLaterSyncs`: parameterized over `Offline`, `Server(503)`, `Unauthorized(401)`, `Server(422)`. After `skip`, `failNext(send, f)`, then `SyncEngine.refreshNow()`:
      - state is not unauthorized, `pendingCount == 1`, `gate == App`, the session is still active, the row has `attempts == 1`;
      - a second `refreshNow()` gives `pendingCount == 0`, `fetchIdentity().walkthroughSeen`, and `gate == App`.
    - `relaunchPendingOverlaySurvivesSessionUpsertAndStaleFullPull`.
    - `replayReturnsWithoutWritingOrRoutingToToday`: writes 0, returns 1.
    - `invalidatedSessionDoesNotCompleteAndStorageFailureCanRetry`: re-`accept` invalidates the permit, so `skip()` gives `!completed && failure != null`, `gate == Walkthrough`, no rows. A `complete` that throws `StoreFailure` once and then enqueues fails on the first skip and succeeds on the second.
    - `pendingLegacyRowOverridesAStaleFlagButKeepsVerificationAndTermsFirst`.
    - Android-only: `cancellationIsRethrownAndBusyResets` (a `complete` that throws a plain `CancellationException` propagates it, and `busy` is false afterwards); `stalePermitIsASessionChangeNotACancellation` (a `complete` that throws `StaleSessionException` sets `failure`, does not propagate, `!completed`).
  - `WalkthroughGeometryTests.deviceTableUsesEveryInsetAndKeepsRoomForFixedControls`: every profile in `profiles.json` × {its insets, (7, 19, 11, 31)}. Assert:
    - `contentTop == top + 8`;
    - `contentTop + contentHeight == height − max(bottom, 32)`;
    - below 600 dp: `contentLeft == left + 16` and `contentLeft + contentWidth == width − right − 16` (iOS); from 600 dp: the §5.5 560 dp column;
    - `contentHeight > 48 + 8 + 20 + 44`.

    Android-only: `wideWindowCentresA560Column` (`foldInner` 841 and `landscapeSideNav` 914 with its right inset 48: content width 560, equal margins inside `[leading, width − trailing]`).
  - `OnboardingSessionTests`: `enqueueRejectsAStaleSnapshotWithSessionChanged` (after `signOut(discardPending = true)`, `enqueue` throws `SessionChangedException` and no row is written); `policiesFlowDecodesTheStoredSection` (Turbine: `null` before the pull, the `AccountPolicies` after).

### Task 5: `feat(onboarding): build the walkthrough screen`
- **Files:** `walkthrough/WalkthroughViewModel.kt`, `WalkthroughScreen.kt`, `WalkthroughPreview.kt`, `WalkthroughTags.kt` (the constants `walkthrough.screen|counter|skip|title|next|done`) (§5.6).
- *Stack 2026-11-20:* apply stack check flags 2 and 3 to the tests below: logic and counter/busy cases move to JVM `WalkthroughViewModel` tests, and no case asserts on-screen text.
- **Tests** (`feature/onboarding/src/test/…/walkthrough/`):
  - `WalkthroughCopyTests.everyBoardStringHasPairedRNDutch` (`@Logic`, `@Config(qualifiers = "nl")`):
    - `walkthrough_skip` = "Overslaan", `walkthrough_next` = "Volgende", `walkthrough_done` = "Klaar";
    - `getString(walkthrough_current_of_total, "2", "3")` = "2 van 3";
    - the three titles' Dutch values;
    - every `walkthrough_*` id in `R.string` resolves to non-empty text under en and nl.
  - `WalkthroughScreenTest` (`@Logic`, `pixel8`, fake `OnboardingSessions` whose `enqueue` suspends on a `CompletableDeferred`):
    - `counterAndButtonFollowThePage`: counter "1 OF 3" → Next → "2 OF 3" → Next → "3 OF 3"; `walkthrough.done` exists and `walkthrough.next` does not.
    - `busyDisablesSkipAndDoneAndCompletesOnce`: tap Skip, assert both disabled, tap again, then complete the deferred; `enqueue` was called once.
    - `titleIsAHeadingAndAPoliteLiveRegion`.
    - `previewIsHiddenFromAccessibility`: no node with text "Sleep" in the unmerged tree on page 2.
    - `reduceMotionSnapsTheDots`: `LocalReduceMotion provides true`, `mainClock.autoAdvance = false`; after Next, dot 2 is 24 dp wide with no clock advance.
    - `loadingShowsOnlyTheBackground`: `sessions` never emits, so no `walkthrough.skip`.
  - `WalkthroughReplayTest` (`@Logic`): replay mode with an `enqueue` that fails the test if called. Skip, Done (after two Nexts) and `Espresso.pressBack()` (or `activity.onBackPressedDispatcher.onBackPressed()`) each call `onFinish` exactly once on fresh screens.
  - `WalkthroughScreenGuardsTest` (`@Layout`, `ParameterizedRobolectricTestRunner`, one case per page {1, 2, 3} × {`small` nl, `largeFont` nl, `pixel8` en, `foldInner` en, `landscapeSideNav` en}, light only = 15 cases (standard acceptance #5); each < 1 s warm):
    - `Guards.assertAll`;
    - without scrolling, `walkthrough.skip` and `walkthrough.next|done` lie fully inside the window minus `LocalSafeInsets`;
    - `walkthrough.title` becomes displayed after `performScrollTo()`;
    - every clickable is ≥ 48 dp;
    - on `foldInner` and `landscapeSideNav` the content width is 560 dp ± 0.5, centred inside the insets.
- The plan box is not ticked yet (Task 6).

### Task 6: `feat(app): route the walkthrough gate and the replay`
- **Files:** `…/app/onboarding/AppOnboardingSessions.kt` (§5.2 `:app` part, wired into `AppGraph` and the ViewModel creation extras); the `Route.Walkthrough` branch in `CfRoot.kt`'s route `when` (decorators already present, §5.8); the shell tab entry; `Route.hidesShellChrome()`; `CfNavLayout.None` in 6a's `CfNavigationSuiteScaffold`; the `AppShell` layout and insets rule (§5.8).
- Update the 4e seam tests (§5.8).
- **Tests:**
  - `HidesShellChromeTest` (JVM, `:core:navigation`): only `Walkthrough` is true.
  - `WalkthroughReplayRoutingTest` (`@Logic`, Robolectric, `app/src/testDebug`). *Stack 2026-11-20: host `AppShellScaffold` with fake content as `AppShellTest` does, with no `AppGraph`/`TestSeeds` (Konsist boundary rule), and assert tags and a zero-call fake `enqueue`, not counter text; see stack check flag 4.* Original text: seed signed in with `CF_AUTH_TEST_WALKTHROUGH_SEEN=1` (profile `screen`); click `tab.settings`; call `holder.push(Route.Walkthrough(replay = true))` on the shell holder (reach it the way 6a's `AppShellTest` does). Assert that `walkthrough.screen` is shown and `tab.settings` does not exist (no bar or rail). Tap `walkthrough.skip`: `route.settings` is back, `tab.settings` exists, and the outbox has 0 rows. Repeat the push once and assert the counter reads "1 OF 3" (a fresh ViewModel).
  - ~~`LaunchGateRoutingTest` (updated)~~: the class was deleted by the test diet. Skip → gate `App` is proven by `WalkthroughModelTests` (JVM), and the route hop by `OnboardingJourneyTest` (stack check flag 5).
  - ~~`RootContentTest` ViewModel-cleared case~~: moot, because the decorators and revision keys already exist.
  - Switch `RootContentTest.recreationKeepsTheProcessRouteWithoutAnotherNavigation` from `Route.Terms`/`route.terms` to `Route.GuestWizard`/`route.guestWizard` (in Task 9 if Terms lands there).
- **Tick** `plan.md` Phase 8 "**Walkthrough**".

### Task 7: `feat(onboarding): port the terms update model`
- **Files:** `terms/TermsUpdateModel.kt` (§5.4), `terms/TermsGeometry.kt` (§5.5).
- **Tests** (JVM, `feature/onboarding/src/test/…/terms/`):
  - `TermsAcceptanceTests` (the fixture with `termsPending = true`):
    - `offlineAcceptanceOpensNextGateAndCanSyncLater`, parameterized `seen` false/true:
      - `gate == Terms`;
      - offline accept gives `completed && !failed && !busy`;
      - `gate == (seen ? App : Walkthrough)`;
      - one row of kind `settings` decoding to `Policies(termsVersion)`;
      - a repeated `accept()` and a direct `enqueue` keep the same ids;
      - `OutboxFlusher.run(epoch).offline`, the gate unchanged; online, the flush empties the outbox and `fetchIdentity().policies.acceptedTermsVersion == termsVersion`;
      - when not seen, enqueue `Walkthrough` and `gate == App`.
    - `relaunchSessionUpsertAndStaleFullPullKeepPendingAcceptance`.
    - `failedOutboxInsertRollsBackAcceptanceAndShowsRetry`: execute `CREATE TRIGGER fail_terms BEFORE INSERT ON outbox BEGIN SELECT RAISE(ABORT, 'fixture'); END` through the JDBC driver. Assert `failed && !completed && canAccept`, `gate == Terms`, no rows. Drop the trigger; accept → completed, `gate == Walkthrough`.
    - `newerServerVersionStillRequiresAcceptanceAndRejectsStaleScreen`.
    - `revokedPermitCannotAcceptForAnotherSession`.
  - `TermsUpdateModelTests` (Android-only, pure fakes):
    - `canAcceptRequiresLoadedUnacceptedPoliciesAndIdle` (null → false; accepted == current → false; busy → false);
    - `newVersionResetsCompletedAndFailed`;
    - `sessionChangedIsSilent` (no `failed`);
    - `cancellationIsRethrownAndBusyResets`;
    - `stalePermitIsSilent` (`save` throws `StaleSessionException`: no `failed`, `!completed`, nothing propagates).
  - `TermsPresentationTests.deviceTableIncludesAllInsetsAndScrollableContentBounds` (JVM): every profile × {its insets, (7, 19, 11, 31)}; `contentTop == top + 24`, `contentTop + contentHeight == height − bottom − 32`, left/right per §5.5 (iOS values below 600 dp, the 560 column from 600 dp), positive sizes.

### Task 8: `feat(onboarding): build the terms update screen`
- **Files:** `terms/TermsViewModel.kt`, `TermsScreen.kt`, `TermsTags.kt` (`terms.screen|title|policy|failure|accept|signOut|signOutFailure`) (§5.7), and `feature/onboarding/src/main/res/drawable/terms_brand.xml`.
  - The drawable is a `<vector>` 28 dp × 28 dp, viewport 1024 × 1024.
  - Path 1 is `M0,0h1024v1024h-1024z`, filled with an `aapt:attr` linear gradient from `startY=0` to `endY=1024`, stops `0 #FF06091C`, `0.6 #FF221A40`, `1 #FFAA5830`.
  - Then a `<group translateX="256.4" translateY="256.4" scaleX="21.3" scaleY="21.3">` holds the moon path `M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401`: no fill, `strokeColor #FFF2AD73`, `strokeWidth 2.0`, round cap and join.

  All of it is copied unchanged from `_design/icon/ios/AppIcon-default.svg`.
- *Stack 2026-11-20:* apply stack check flags 2 and 3: the `TermsScreenTest` logic and copy cases (`failedSave…` "Try again" text, busy/sign-out states) become JVM `TermsViewModel` tests; Robolectric keeps tags and structure only.
- **Tests:**
  - `TermsBrandAssetTest` (JVM, parsing both files): the gradient stops, translate, scale, path `d` and stroke equal the SVG's; the size is 28 dp.
  - `TermsCopyTests.catalogPairsEveryBoardStringAndRetainsRNDutch` (`@Logic`, nl): `terms_accept_and_continue` = "Akkoord en verder"; `terms_we_ve_updated_our_terms` = "We hebben onze voorwaarden bijgewerkt"; `terms_try_again` = "Opnieuw proberen"; every `terms_*` id is non-empty in en and nl.
  - `TermsScreenTest` (`@Logic`, fake sessions):
    - `acceptDisabledUntilPoliciesLoadAndWhenAlreadyAccepted`;
    - `failedSaveShowsBannerAndTryAgain` (the fake `enqueue` throws `IllegalStateException`): `terms.failure` exists and the `terms.accept` text is "Try again";
    - `policyRowOpensTheTermsCustomTab`: click `terms.policy`; the next started activity's data is `https://chronoflow.net/terms`;
    - `policyRowStaysEnabledWhileFailed`;
    - `signOutBusyDisablesBothButtons`;
    - `signOutFailureShowsMessage`;
    - `needsConfirmationInvokesTheSlotWithTheCount`: the slot records `count == 1`; `onStay` closes it.
  - `TermsViewModelTests` (JVM, fixture): `signOutFromTermsUsesExistingPendingConfirmationStayAndDiscard`, ported. Enqueue `Settings(Preferences, {"time_format":"12"})`; `signOut()` gives `pendingSignOutCount == 1` with the session still active; `stay()` gives null; `signOut()` then `signOut(discardPending = true)` gives `activeSession() == null`.
  - `TermsScreenGuardsTest` (`@Layout`, parameterized): state {ready, failed} × {`small` nl, `largeFont` nl, `pixel8` en, `foldInner` en, `landscapeSideNav` en}, light only = 10 cases (standard acceptance #5). Assert `Guards.assertAll`; `terms.accept` and `terms.signOut` can be scrolled into view and are ≥ 48 dp; on `pixel8` en, the ready state shows `terms.accept` without scrolling; on `foldInner` and `landscapeSideNav` the content width is 560 dp ± 0.5.

### Task 9: `feat(app): route the terms gate`
- The `Route.Terms` branch in `CfRoot.kt`'s route `when`, with the sign-out slot (§5.8). Switch `RootContentTest.recreationKeepsTheProcessRouteWithoutAnotherNavigation` off `Route.Terms` (it hits the real dispatch).
- **Test:** ~~`LaunchGateRoutingTest.termsPendingAcceptOpensWalkthrough`~~ (the class was deleted; a real-graph Robolectric test breaks the Konsist boundary rule). Accept → gate `Walkthrough` is proven by `TermsAcceptanceTests.offlineAcceptanceOpensNextGateAndCanSyncLater` (JVM), and the screen hop by `OnboardingJourneyTest` (stack check flag 5).
- **Tick** `plan.md` Phase 8 "**Terms update gate**".

### Task 10: `test(debug): seed pending walkthrough and terms changes`
- §5.11.
- **Tests:**
  - extend `TestSeedSessionKeysTest` with both keys (valid `"1"`, invalid `"yes"`, ungated);
  - `mutationsOfflineFollowsPendingSeeds` (JVM, parameterized): `CF_WALKTHROUGH_TEST_PENDING=1` → `mutationsOffline == true`; `CF_AUTH_TEST_TERMS_PENDING=1` → `true`; `CF_AUTH_TEST_TERMS_CHANGES_PENDING=1` alone → `false`; no key → `false`;
  - `termsChangesPendingSeedsOneRowOnce` (JVM, `SeedRunner` seam): one `settings` row after two applies.

### Task 11: `test(onboarding): add the onboarding journey and board captures`
- **`OnboardingJourneyTest`** (`@Smoke`, `app/src/androidTest`, extends `ScreenTest`).
  - Seed with `ScreenTest.launch(overrides)` (it applies `TestSeeds` and supplies `CF_AUTH_TEST_ID`/`CF_UI_TEST_PROFILE=screen`):  `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_TERMS_PENDING=1`, `CF_WALKTHROUGH_TEST_PENDING=1`, no `WALKTHROUGH_SEEN`, language en.
  - Steps:
    1. `waitUntilExactlyOneExists(hasTestTag("terms.screen"), 10_000)`;
    2. click `terms.accept`;
    3. wait for `walkthrough.screen`;
    4. click `walkthrough.next` twice; after the second, wait until exactly one `walkthrough.done` exists (no counter text: AGENTS "never assert copy");
    5. click `walkthrough.done`;
    6. `waitUntil(10_000) { RouteProbe.state.route == Route.Today }`;
    7. assert `walkthrough.screen` does not exist.
  - The test stays under 10 s. It is the **only** emulator test of this phase.
- **`OnboardingBoardCaptureTest`** (`@Capture`, Robolectric, a test-only board-sized copy of `pixel8` as `TodayBoardCaptureTest` does, fake sessions): `BoardCapture.capture` for `Walkthrough`, `Walkthrough2`, `Walkthrough3`, `WalkthroughReplay`, `TermsUpdate` and `TermsFailed`, each as `<Board>_pixel8_<en|nl>_<light|dark>` (24 PNGs).
- **Tick** `plan.md` Phase 8 "**Tests**".

### Task 12: `docs(onboarding): record phase 8 results and open questions`
- Append OQ-8-1 … OQ-8-11 (§9) to `plan.md` "Open questions for the user" as OQ-A337 … OQ-A347 ("(8 OQ-8-n)" in the question), each with its default; any new question continues from OQ-A348.
- Add a line under Phase 10b's "Replay walkthrough" task (plan l.2917): "call `holder.push(Route.Walkthrough(replay = true))` from Preferences (Phase 8 tab entry, `hidesShellChrome`, `CfNavLayout.None`)". Add a line under Phase 10a's "Sign out with pending changes" task (plan l.2880): "also fill the Terms `signOutConfirmation` slot (Phase 8)".
- Record in `docs/tooling.md` the seeded `adb` command for the terms journey.

## 7. Edge cases (each is covered by a test named above)

- **Double taps:** Skip, Done and Accept ignore taps while busy or completed. A walkthrough or policies enqueue with a pending row returns the existing row (4d), so there are never two rows.
- **Offline:** completion and acceptance are local. The gate moves at once. Sync sends later (4d lifecycle observes the outbox; no producer calls `requestSync`).
- **Walkthrough POST 401:** the row is kept, the flight is deferred, the session survives, and a retry succeeds (`failedFlushKeepsCompletionAndLaterSyncs`).
- **Terms POST 422 (stale version):** 4d refuses the row (final, `settings.syncFailure`) and deletes it; the next pull stores the server's policies and the gate asks again. A stale screen cannot accept a changed version: `enqueue` throws `InvalidPayload`, so the model shows `failed`.
- **New terms version while on Today:** the gate turns `Terms` and the router navigates there (4e). On the Terms screen, `update()` resets completed and failed.
- **Session change mid-save:** `SessionChangedException` (snapshot moved) or `StaleSessionException` (permit revoked, a `CancellationException` subclass) leaves the walkthrough with `failure` set and the terms screen silent. Neither writes, and neither propagates as a cancellation.
- **Policies missing:** 4e keeps the gate loading. On the screen, Accept stays disabled.
- **Rotation, fold, split, locale or font-scale recreation:** page and state live in the ViewModel. **Process death:** a cold launch recomputes the gate and starts the walkthrough on page 1.
- **Very short windows (`small` at `largeFont`):** the walkthrough header and footer stay fixed and the middle scrolls. Terms scrolls as a whole.
- **No browser installed:** the policy row does nothing and never crashes.

## 8. Acceptance

1. `make quick` passes, including every new JVM test and the `@Logic` Robolectric tests.
2. `make targeted ONLY="feature:onboarding:WalkthroughModelTests feature:onboarding:TermsAcceptanceTests feature:onboarding:TermsViewModelTests app:RootContentTest app:WalkthroughReplayRoutingTest"` passes, every JVM and warm Robolectric case under 1 s (first Robolectric case per SDK ≤ 20 s, first per class ≤ 3 s; `build/test-results/*/timings.json`).
3. `make generators-check` passes (the lucide manifest change).
4. `make captures ONLY=Walkthrough,TermsUpdate,TermsFailed` (prefix match also selects `Walkthrough2`, `Walkthrough3` and `WalkthroughReplay`) writes 24 PNGs to `build/captures/`. The Opus reviewer compares them with the boards; each difference is fixed or listed with its board name, and the §4 differences are expected.
5. `make check` passes with `pixel` booted. It includes `OnboardingJourneyTest`, Lint (`MissingTranslation`), Konsist (no `:feature` → `:feature` edge, `insetsOnlyAtRoot`, no `MaterialTheme.colorScheme` in feature code), `testReleaseUnitTest` and `:app:verifyReleaseManifest` (no new permission from `androidx.browser`).
6. During the journey, `adb logcat -d -s StrictMode` shows no violation from `net.chronoflow` frames. Paste the output into the report.
7. No perf gate applies to this phase. `make size` is reported before and after; `androidx.browser` must add under 100 KB.
8. All three Phase 8 boxes are ticked, the commits are atomic Conventional Commits with no trailers, and the OQ rows are added.

## 9. Open questions (proceed on the default; Task 12 copies them to plan.md)

| # | Question | Default |
|---|---|---|
| OQ-8-1 | `CategorySwatch` size: board `md` is 36 dp with a 16 dp icon; iOS draws 32 (`space8`). | Settled by 6a (OQ-6a-12): iOS, 32 dp; Small 28. Phase 8 reuses 6a's swatch and records no new question here beyond this note. |
| OQ-8-2 | Sign-out from Terms with pending changes needs the `SignOutPending` sheet, which is 10a. | 8 builds the state, `stay()`, discard and a slot. Until 10a fills the slot, `NeedsConfirmation` shows nothing more and the user stays on Terms (Accept still works). |
| OQ-8-3 | Policy URL in Dutch: the web serves `/nl/terms`; iOS always opens `/terms`. | `https://chronoflow.net/terms` for both languages (iOS parity; 05 OQ A19: confirm the host before release). |
| OQ-8-4 | Walkthrough timeline preview: iOS shows titles only at ≥ 640 pt; the Android column is capped at 560 dp. | Keep the iOS rule, so every Android width shows the compact icon-only blocks, matching the board at phone width and `23-walkthrough.png`. |
| OQ-8-5 | System back on the first-run walkthrough and on Terms. | Root behaviour (the activity finishes or goes to the background); no page-back. In replay, back = Skip (returns, no write). |
| OQ-8-6 | A failed local walkthrough save has no board. | As iOS: no message; the buttons re-enable so the user can retry. |
| OQ-8-7 | Dot animation easing: board CSS `ease`, iOS `easeInOut`. | `EaseInOut`, 150 ms; snap under Reduce Motion. |
| OQ-8-8 | Terms copy: board placeholders (effective date, three changes) or the iOS/web generic summary. | iOS code (§4); the placeholders wait for server per-version fields. |
| OQ-8-9 | Where `CategorySwatch` lives. | Settled by 6a: `:core:ui` `net.chronoflow.core.ui.plan` (OQ-6a-12); 6d, 7, 8 and 10b reuse it and never redefine it. |
| OQ-8-10 | 5d's Custom Tab opener (`PolicyOpener`) lives in `:feature:auth`, which `:feature:onboarding` cannot use. | Phase 8 adds `PolicyLinks` in `:core:ui` with the same behaviour (no toolbar colour) and leaves 5d's code alone; a later cleanup may make 5d's opener delegate to it. |
| OQ-8-11 | Hiding the tab bar under the replay needs a "no bar" layout that 6a's `CfNavLayout { Bar, Rail }` lacks. | Add `CfNavLayout.None` (`NavigationSuiteType.None`), chosen while the top entry `hidesShellChrome()`, with full insets for the content; no hide/show animation API (6a OQ-6a-9 still holds). Alternative not taken: 6a's full-screen `overlay` layer, which has no per-entry ViewModel store. |

## 10. Review checklist (Opus reviewer)

- **Gate order:** Terms is shown before Walkthrough, which comes before Today. The screens never call `rootNavigator.navigate` in first-run mode; only the 4e router does. Look for a stray `navigate(Route.Today)` in the walkthrough or terms code, which would cause a double navigation.
- **Local first:** neither the model nor the ViewModel awaits sync. No `refreshNow`/`requestSync` in onboarding code. `enqueue` goes through the 4d writer (IO), never on main and never under `runBlocking`.
- **Walkthrough 401:** `failedFlushKeepsCompletionAndLaterSyncs` includes `Unauthorized(401)` and asserts that the session is still active.
- **Replay:** zero writes (the test's `enqueue` must fail if called), `onFinish` fires exactly once on Skip, Done and system back, and the shell bar/rail is hidden while replay is on top and back afterwards.
- **Exceptions:** `CancellationException` is rethrown everywhere, except 4a's `StaleSessionException`, which both models catch first as a session change. `SessionChangedException` does not extend `CancellationException`. There is no `runCatching` around suspend calls.
- **Copy:** every `Text` uses `stringResource` with lock-file names. Terms ships the iOS body and bullets, not the board placeholders and not "Shown after sign-in…". The counter is uppercased with the configuration locale ("1 VAN 3" in nl).
- **Tokens:** no literal dp, sp or colour in feature code. Values come from `PlanUITokens`, `AuthTokens`, `TermsTokens`, `CfTheme`, 6a's `WindowPolicy` and the `@BoardMetrics` `OnboardingMetrics` (560, chevron 18). The brand drawable is the only place with hex colours, and `TermsBrandAssetTest` guards it. The preview colours are `cat*` tokens; the swatch is 6a's.
- **Accessibility:** the preview and the dots are cleared from semantics. The title is a heading and a polite live region. The Terms headings are headings, and the failure banner is announced. Touch targets are ≥ 48 dp (the Skip pill is 44 visual).
- **Layout:**
  - geometry is computed from `BoxWithConstraints` and `LocalSafeInsets`, never `onSizeChanged` and never `WindowInsets.*`;
  - no `weight` inside a `verticalScroll`;
  - the walkthrough header and footer stay fixed at `largeFont` on `small`;
  - the 560 dp column holds on `foldInner` and `landscapeSideNav`;
  - with the shell chrome hidden (`CfNavLayout.None`), the replay gets the full `LocalSafeInsets`, so its footer clears the gesture area;
  - root `Terms`/`Walkthrough` ViewModels are entry-scoped (`RootContent` decorators), and each replay gets a fresh ViewModel.
- **Custom Tab:** `ActivityNotFoundException` is caught and nothing else is swallowed. No WebView. No `<queries>`, no toolbar colour (5d parity), one `androidx-browser` catalog entry, and no permission in the release manifest.
- **Tests:** they follow AGENTS "Tests": the cheapest tier, one emulator journey only, exact tags, `waitUntil` instead of sleeps, parameterized cases instead of loops, and the timings inside budget. The 4e seam assertions (`route.terms`, `route.walkthrough`) are updated, not deleted.
- **Typical mistakes:**
  - building a placeholder `SignOutPending` sheet;
  - adding swipe navigation (`HorizontalPager`);
  - storing the page in `rememberSaveable` instead of the ViewModel;
  - calling `complete()` in replay;
  - opening `/privacy` from Terms;
  - building `PolicyReader`;
  - polling with `delay(100)` instead of reacting to `controller.updates` and `sessionGraphs.current`;
  - `animateDpAsState` read in composition for the dots;
  - a second `CategorySwatch` (in `:feature:onboarding` or `:core:ui`) instead of 6a's;
  - importing 5d's `PolicyOpener` from `:feature:auth` (a feature → feature edge);
  - using `NavigationSuiteScaffoldState` hide/show instead of the `CfNavLayout.None` layout type;
  - deriving `mutationsOffline` from `CF_AUTH_TEST_TERMS_CHANGES_PENDING`.
