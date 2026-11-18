# Phase 6a: Tab shell, My trips and pull to refresh

- **Branch:** `feat/phase-6a-shell-trips`, from the stack top once 4e and 2 are in it. This phase also uses 1a (`PlanDecoder`, `CopyLineFormatter`, `tripDisplayStatus`, `buildCheckState`, `withoutOptionalSteps`, `checkIdentity`, `planDate`, `PlanClock`), 1c (`TimelineCategoryStyle`, `TimelineGlyph`), 1d (`SystemPlanClock`, `AppGraph.planClock`), 4c (mock demo trips), 4d (`TripRepository`, `AccountRepository`, `SettingsRepository`, `SyncResult`, `SyncChanges`) and 0c/0e (strings, `ProfileRule`, `Guards`, tiers). All of them are earlier waves. If one is missing, stop and report it.
- **Stack check (2026-11-18, against 7ab9823).** Single lane: 4e, 2 and everything up to 5d are in the stack. Every upstream name this brief cites resolves (`RoutePlaceholder`, `LaunchOverrides`, `MockSources`, `TripRepository`, `PlanDecoder`, `CopyLineFormatter`, `tripDisplayStatus`, `buildCheckState`, `TimelineCategoryStyle`, `TimelineGlyph`, `SystemPlanClock`, `AppGraph.planClock`, `CfPullToRefresh`, `Route.Trips/Today/Settings`). Deltas since the brief was written: root route entries are registered in `CfRoot.kt`'s `when (displayedRoute)`. `RootContent` decorates entries with the saveable-state and ViewModel-store decorators (5d), so ViewModels are entry-scoped and cleared when their root route leaves, and the content key is the secret-free `Route.stateKey()` (`core/navigation/…/Route.kt`). Extend `stateKey()` for any route that gains fields. A Robolectric fixture that renders `RootContent` outside `CfRoot` must provide `LocalSafeInsets` and the other root locals that `CfRoot` provides. Robolectric link launches use `Robolectric.buildActivity`, not `ActivityScenario`. OQ rows end at OQ-A231. `make quick`'s timing gate requires a passed stamp to reuse test outputs (5c).
- **Plan entry:** `plan.md` "Phase 6a: Tab shell, My trips and pull to refresh" (l.1856–1896). Decisions **A3** (l.90), **A4** (l.91), **A8** (l.95), **A10** (l.97). "Android spec: window policy" (l.357–369), "Android spec: Material mapping" (l.414–432), OQ-A3/A4/A5 (l.578–580), "Standard acceptance for every phase" (l.636–643).
- **Revised 2026-11-16** (single pass, no subagents) against the final 2, 3a, 3b, 4d and 4e briefs and iOS `f391a1fa`. Changes: 4e has no `RouteSeam` and no `VariantHooks`. Seams are 3b/4e's `RoutePlaceholder(route, label)`, and debug-only values come through **the variant hook that already carries 3a's `LaunchOverrides`** (4e put `MockSources` there too) (§5.7, T10, T11). Other changes: Phase 2 line numbers; `softShadow(shape, dark)`; `LocalSafeInsets` now lives in `…designsystem.layout`; `core/ui` dp literals need `@BoardMetrics`; the empty-state glyph colour is `foreground`; `CopyCatalogProvider` is owned by `AppGraph`; `detailValues` uses the shape from the 6b brief; added OQ-6a-9…11.
- **Checked 2026-11-16** against iOS `f391a1fa`, the boards, web `e36368ec` and the 1a/1c/2/3b/4d/4e/6b/6d/7 briefs. Corrected: line references (plan l.433, 1c §5.1 l.190, `RefreshFeedback.swift:58–60`), the rail container default, out-of-scope owners (trip actions 6d, refused changes 6d, wizard save 9g), safe-area insets for the list and capsule (iOS `planPageRail`), token names for the loading skeleton, the empty-state button size, `cfClickable` focus shape, `TripsMetrics` visibility, the `AppLanguage` → `Language` mapping, the T3 file list, the `CategorySwatch` package (6d path), and added OQ-6a-12…14.
- **Tests:** follow `AGENTS.md` "Tests": cheapest tier first, the budgets (JVM < 1 s, Robolectric < 5 s, emulator < 10 s), no `Thread.sleep`/`SystemClock.sleep`, `runTest(timeout = 60.seconds)`, exact tags, at most one emulator journey for this feature. Ported tests keep the Swift names. Read that section before you write a test.
- **Tags:**
  - **[F]** is a fact verified in the source at the path given. **[R]** is a rule this brief sets for Android.
  - `I/` means `/Users/rickgout/Sites/chronoflow-ios`, read at `f391a1fa` with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. `Kit/` = `I/Packages/ChronoFlowKit/Sources`, `KitT/` = `I/Packages/ChronoFlowKit/Tests`. `B/` = `/Users/rickgout/Sites/chronoflow-android/_design/canvas`.
  - **All source repos and `_design` are read-only.** Never write, build, run prebuild or change git state in them.
- **Path shorthands:**
  - `…/plan/` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/`
  - `…/sync/` = `core/sync/src/main/kotlin/net/chronoflow/core/sync/`
  - `…/data/` = `core/data/src/main/kotlin/net/chronoflow/core/data/`
  - `…/model/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`
  - `…/nav/` = `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/`
  - `…/ds/` = `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/`
  - `…/ui/` = `core/ui/src/main/kotlin/net/chronoflow/core/ui/`
  - `…/trips/` = `feature/trips/src/main/kotlin/net/chronoflow/feature/trips/`
  - `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`, `…/debug/` = `app/src/debug/kotlin/net/chronoflow/debug/`
- **Secrets rule (AGENTS, A23):** no token or link secret goes into a `Bundle`, `SavedStateHandle`, `rememberSaveable`, the DB, the outbox, a log or a Nav3 key. This phase adds routes with trip ids only.
- **Commits:** one per task, Conventional Commits, lower-case imperative subject, **no `Co-Authored-By` and no other trailer**, whatever a tool suggests.

---

## 1. Goal

After 4e, a signed-in launch ends on `Route.Today` rendered as a debug `RoutePlaceholder`. After this phase, the signed-in app is a three-tab shell, a port of iOS `App/AppTabs.swift` (iOS 15a/15b):

- `NavigationSuiteScaffold` shows a bar below 600 dp and a rail from 600 dp. It never hides on scroll.
- The tabs are Today, My trips and Settings, each with its own Nav3 back stack.
  - Reselecting a tab pops it to its root; at the root it scrolls to the top.
  - System back on a non-Today tab root selects Today. Back on the Today root exits the app.
  - Predictive back works within a stack.
- **My trips** is real: sections Active, Upcoming and Completed, trip cards with an optional "Next" row, an empty state and a loading skeleton.
  - "+" and "Plan a trip" open a wizard seam (until 9c).
  - A card opens the `TripPage(tripId)` seam (until 6b).
- **Pull to refresh** calls `SessionSync.refreshNow()`. The designed capsule shows the outcome below the top app bar for 2 s, as one polite live region tagged `refresh.feedback`. A debug probe counts accepted user refreshes and records what was presented.
- Today and Settings stay debug route seams until Phases 7 and 10a.

## 2. Inputs to read (in this order)

| Source | What to take from it |
|---|---|
| `AGENTS.md`, `CLAUDE.md` | Rules, `make` targets, tests, Git |
| `plan.md` l.1856–1896, l.90–97, l.357–369, l.414–432, l.578–580, l.636–643 | Phase entry, decisions, window policy, Material mapping, OQ-A3/A4/A5, standard acceptance |
| `docs/research/07-day-to-day-settings.md` §2 (l.66–87), §4 (l.127–151), §10 (l.600–618), §11 (l.620+) | Shell, My trips, refresh, endpoints |
| `docs/research/08-design-system.md` §10 (l.582+) | M3 mapping, designed surfaces |
| `docs/research/04-timeline.md` §9.14 (l.1249) | Pull to refresh on Android |
| `docs/phases/phase-2-design-system.md` | `ChronoFlowTheme`, `CfTheme`, `CfTypeScale`, `PlanUITokens`, `AuthTokens`, `CfBadge` (l.436), `CfCard`, `CfSkeleton` (l.465), `CfLargeTopAppBar` (l.479, insets from `LocalSafeInsets` Top+Horizontal), `CfPullToRefreshBox` (l.487), `softShadow(shape, dark)` (l.294), `cfClickable` (l.283), `@BoardMetrics` (l.524), Konsist rules `featureDpLiteralsComeFromBoardMetrics`/`boardMetricsNamesExist` (l.517–518, they cover `core/ui/src/main` too), `MaterialSymbols` (l.340: `space_dashboard`, `flight`, `settings` each with `-fill`, `add`), `LucideGlyphs` + `LucideGlyph.imageVector()` (l.330–336: `plane`, `check`, `chevron-right` exist), Task 0 (`LocalSafeInsets` in `net.chronoflow.core.designsystem.layout`) |
| `docs/phases/phase-4e-session.md` §5.4–§5.8 (l.395–470), T10, T11 (l.662–720) | `Route` (l.436–452), `RootState`, `RootNavigator`, `RootContent` (l.456), `RoutePlaceholder(route, label)` (l.464), `RouteProbe` (l.467), `AppGraph` (l.395), `SessionGraph(… sync: SessionSync …)` (l.426), `SessionGraphs.current`, `AppGraphKey` (l.431), `DebugSeedRouting.current` (l.687), `PlanRouteSeed` (l.679), `TestSeeds.apply` |
| `docs/phases/phase-3a-launch-overlay.md` l.300, `phase-3b-launch-readiness.md` l.7, l.258–263 | The variant hook that carries `LaunchOverrides` (debug: `DebugLaunchOverrides`; release: `None`), the `…/perf/` and `…/rel/` twin source sets, `DebugRouteLabel` wiring. **Do not add a second variant mechanism.** |
| `docs/phases/phase-6b-trip-page.md` Task 1 (l.154–170) | The `detailValues` shape that 6b expects. 6a builds it exactly that way (§5.3). |
| `docs/phases/phase-4d-sync.md` | `SyncState`, `SyncResult`, `SyncChanges`, `TripRepository`, `AccountRepository.values()`, `SettingsRepository.values()`, `TripRecord` (l.121–122) |
| `docs/phases/phase-1a-plan-core.md` l.263–301, l.420–421 | `PlanDecoder`, `TripPlanSnapshot`, OQ-1a-5 (the `trip` field is added here) |
| `docs/phases/phase-1c-timeline-drawing.md` §5.1 (l.190–211) | `TimelineCategoryStyle` table |
| `docs/phases/phase-0c-strings.md` l.205–245 | Resource naming (`<prefix>_<slug>`), `names.lock.json`, plurals |
| `I/App/AppTabs.swift` (l.85–98 debug options incl. `CF_REFRESH_TEST_COUNTER`, l.99–105 iOS 26 `tabBarMinimizeBehavior(.onScrollDown)`, l.137–182 tabs and wizard, l.260–280 seeds, l.350–385 continuation and reminder routes, l.466–490 `trips`/`refreshTrips`/`tripsContent`) | Shell behaviour |
| `Kit/PlanUI/Trips/TripsScreen.swift`, `TripSections.swift`, `TripsLoadingView.swift` | My trips |
| `Kit/PlanUI/Components/TripSummaryCard.swift`, `PlanComponents.swift` (`CategorySwatch` l.7–99, `PlanBadge` l.102+, `planEmptyCard`/`planCard` l.180–203) | Card, swatch, badge |
| `Kit/PlanUI/Components/RefreshFeedback.swift`, `PlanRefreshFeedback.swift` | Refresh model and capsule |
| `Kit/PlanUI/Models/TripsModel.swift`, `PlanDecoder.swift` (l.1–63, 117+ `adaptation`) | List model and decoder |
| `Kit/PlanCore/Today.swift` | `TodayPlan`, `todayView`, `calendarDaysUntil` |
| `Kit/LocalStore/TripRepository+PlanObservation.swift` | `detailValues` |
| `KitT/PlanUITests/TripsTests.swift`, `DecodeRecoveryTests.swift:64`, `ModelTests.swift:115`, `RefreshFeedbackTests.swift`, `TodayTests.swift:28,45` | Tests to port |
| Boards `B/Trips.dc.html` (states `default`/`empty`/`loading`, light/dark), `B/TripsEmpty.dc.html`, `B/TripsLoading.dc.html` (wrappers that import Trips), `B/ds/chronoflow/components/bundle.js` (`Badge`, `MetaChip`, `CategorySwatch`) | Visual spec |

**[F] There is no shell board.** `B/Main.dc.html` is the **Welcome** board (title "Welcome"). The shell reference is the `<nav aria-label="Main">` tab bar drawn on Trips, Today and SetHome. Canvas notes `dirNative` and `reqSystemChrome` say that the tab bar is the system's, that we set only its icons, labels and selected colour, and that "Android Material not drawn yet". Build the shell per the Material mapping. Do not invent a board (OQ-6a-1).

**Endpoints** (no new client code; `refreshNow` runs the 4d full pull) [F 07 §11, 4d]: first the outbox flush, then `GET users/me`, `GET account`, then `GET trips` → `{trips:[TripListItem{card, planInputHash, planStartUtc, planStartZone, planEndUtc, planEndDate, preparationDays, adaptation?, keepOffline, detailVersion?, checkState?}]}`, then `GET trips/{uuid}` → `{trip, plan, notices, timeline, checkState}` for trips with `keepOffline`.

## 3. Out of scope (owner in brackets)

- The trip page, its timeline and list, `awaitingSync` handling, `CF_PLAN_TEST_BLOCK` and the `trip:<id>:list` mode [6b–6e]. The route is a seam here.
- Today content, `selectTodayTrip`, Today pull to refresh and its capsule [7].
- Settings pages and `CF_SETTINGS_ROUTE` [10a].
- The wizard, `CF_WIZARD_AUTO_OPEN`, the guest continuation and the "saved" hand-off UI [9c, 9g, 11b]. The reducer action `wizardSaved` is built and tested here, but nothing calls it yet.
- Notification taps routing into the shell [12b]. Only the root-route mapping in T10 exists.
- Trip actions (complete, reopen, duplicate, delete) [6d, on the trip page]. iOS `TripSummaryCard` has no actions [F], so My trips cards get none.
- `RefusedChangeFeedback` / toasts (OQ-A5) [6d builds it in `:core:ui`; 10a uses it]. iOS overlays it on each tab stack (`AppTabs.swift:142,147,165`); the shell adds it when 6d lands.
- `showPlanStart` ("Plan starts on …"): iOS passes `showPlanStart: false` on My trips [F `TripsScreen.swift`]. Not built here [7].
- An Android tab-bar board, a logo header item and a Fraunces large title (OQ-6a-1, OQ-6a-6).

## 4. Platform facts this phase relies on (verify, then record the result in the T13 doc commit)

- [R, verify in 1.4.0 sources] `NavigationSuiteScaffold` has an overload that takes an explicit layout type (`layoutType: NavigationSuiteType` or `navigationSuiteType`). Pass `NavigationSuiteType.NavigationBar` or `NavigationSuiteType.NavigationRail`, never `NavigationDrawer` and never the expressive short or wide variants. If the names differ, use the 1.4.0 equivalent and note it in the KDoc.
- [R, verify in adaptive 1.3.0] `currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(600)` decides rail versus bar. The decision uses **width only**, either orientation.
- [R, verify in lifecycle 2.11.0] `rememberViewModelStoreNavEntryDecorator(…)` takes a lambda that decides whether an entry's store is cleared when the entry leaves the back stack (`removeViewModelStoreOnPop` in 1.0). T10 relies on it returning `false` when a whole tab's `NavDisplay` leaves composition because another tab was selected.
- [F 4e §4] `MainActivity.defaultViewModelCreationExtras` carries `AppGraphKey`.
- [F plan l.433 / phase 2 l.184] `toM3()` maps `surfaceContainer` to `popover` and `surface` to `background`. [R, verify in material3/navigation-suite 1.4.0 sources] The M3 default `NavigationBar` container is `surfaceContainer` (→ `popover`) and the default `NavigationRail` container is `surface` (→ `background`). Record the verified defaults in T13.

## 5. Spec

### 5.1 Trip list values and sections [F `PlanDecoder.swift:6–63`, `TripSections.swift`]

- `data class TripListValue(card: TripCardView, planStartUtc: String?, planEndUtc: String?, status: TripStatus, planStartZone: String? = null, planEndDate: String? = null, preparationDays: Int? = null)` in `net.chronoflow.core.plan`.
- `PlanDecoder.trips(records: List<TripRecord>, now: Instant): TripsDecodeResult(values, error: Throwable?)`:
  - runs on the decoder's worker, like 1a's `detail`;
  - skips records with `deletedAt != null` or `deletedLocally`;
  - decodes `record.card` (raw JSON) as `TripCardView` through `ApiJson`;
  - `status = tripDisplayStatus(card, record.planStartUtc, record.planEndUtc, now)`;
  - on a decode error, skips that record and keeps **the first** error. A `CancellationException` is rethrown, never kept.
- `PlanDecoder.card(record)` and `PlanDecoder.adaptation(record): AdaptationView?` (`record.adaptation?.decode`) [F l.117+]. These are ported for parity; 6a uses only `trips`.
- `tripSections(trips: List<TripListValue>): List<TripSection(status, trips)>`:
  - order `[Active, Upcoming, Completed]`; any other status (for example `Unknown`) is dropped [F];
  - within a section, sort by `planDate(card.departureUtc)` **descending**; ties by `card.id` **descending** (string compare);
  - empty sections are omitted.

### 5.2 Today logic [F `Kit/PlanCore/Today.swift`]

Port the whole file into `…/plan/Today.kt` (`net.chronoflow.core.plan`), which Phase 7 extends:
- `TodayPlan(currentDay, currentSteps, upcomingSteps, marks, dayProgress, allCategoriesHidden, importantHidden, nextStep, minutesUntilNext, beforePlan, afterPlan)`.
- `todayView(detail, marks, now, hidden = emptyList())`. The rules are line for line from Swift:
  - `plan = withoutOptionalSteps(detail.plan)`;
  - excluded = `hidden + [LightOk, LightNeutral]`;
  - `nextStep` = the first `plan.blocks` element that is `actionable`, whose `checkKey` is non-null and not in the marked identities, with `planDate(endUtc) > now`. **`hidden` does not affect `nextStep`**;
  - `upcomingSteps` takes at most 4;
  - `minutesUntilNext = max(0, ceil((start − now)/60 s))`.
- `calendarDaysUntil(at: String, now: Instant, zone: String): Int`: Gregorian start-of-day difference in `zone`, falling back to UTC for an unknown zone; never negative.

### 5.3 Plan snapshot observation [F `TripRepository+PlanObservation.swift:12–30`]

- Add `val trip: TripRecord? = null` as the **first** property of `TripPlanSnapshot` (closes OQ-1a-5). Update every existing constructor call with named arguments.
- **Shape [R]:** use the code shape from `phase-6b-trip-page.md` Task 1 (l.160–170): `tripValue(tripId).flatMapLatest { trip -> … combine(planValues, checkValues, pendingCheckOffs) … }.distinctUntilChanged()`, with a new named query `tripById` (`asFlow().mapToOneOrNull(io)`) unless 4d already observes a single trip. 6a owns this code, and 6b's Task 1 then only verifies it (OQ-6a-11).
- `TripRepository.detailValues(tripId: String): Flow<TripPlanSnapshot>` in `:core:data`, built on the 4d SQLDelight query flows (`asFlow().mapToList(io)`). It re-emits when any of these change:
  - the trip row;
  - the `plan_versions` row where `trip_id = tripId AND input_hash = trip.currentInputHash` (`plan = null` when the hash is null);
  - `check_marks` where `trip_id = tripId AND input_hash = currentInputHash ?: ""`, `ORDER BY marked_at`;
  - pending outbox rows that decode to a check-off change for `tripId`, in outbox order.
- Apply `distinctUntilChanged()`. Never query on main.

### 5.4 Refresh feedback [F `RefreshFeedback.swift`, `PlanRefreshFeedback.swift`]

**`RefreshFeedback`** (`…/sync/refresh/RefreshFeedback.kt`, package `net.chronoflow.core.sync.refresh`):
- `data class RefreshFeedback(val key: RefreshFeedbackKey, val count: Int? = null)`.
- `enum class RefreshFeedbackKey { UpToDate, TripsAdded, TripsUpdated, TripsRemoved, NeedsConnection, SomethingWentWrong }`.
- `fun outcome(result: SyncResult, userInitiated: Boolean): List<RefreshFeedback>`:

| Condition | Result |
|---|---|
| `!userInitiated` | `[]` |
| `Offline` | `[NeedsConnection]` |
| `Failed`, `Unauthorized`, `VerificationRequired` | `[SomethingWentWrong]` |
| `Idle` with `retryAfter != null` or `pendingCount != 0` or refusals non-empty | `[SomethingWentWrong]` |
| `Idle` otherwise | rows `TripsAdded(tripsAdded)`, `TripsUpdated(tripsUpdated)`, `TripsRemoved(tripsRemoved)`, in that order, each only if > 0; if none, `[UpToDate]` |

**`RefreshFeedbackModel(scope: CoroutineScope, probe: RefreshDebugProbe = RefreshDebugProbe.None)`** (`…/sync/refresh/RefreshFeedbackModel.kt`):
- `val state: StateFlow<RefreshFeedbackState>`, with `data class RefreshFeedbackState(refreshing: Boolean = false, feedback: List<RefreshFeedback> = emptyList(), presentationId: Long = 0)`.
- `fun refresh(userInitiated: Boolean = true, action: suspend () -> SyncResult?): Job?`:
  1. If `refreshing`, return `null` and record nothing.
  2. Set `refreshing = true`, `presentationId += 1`, `feedback = []`. If `userInitiated`, call `probe.accepted()`.
  3. Launch the work in **`scope`**, not in the caller. Cancelling a caller that `join`s the returned job does not cancel the sync. This is the Android form of the iOS detached `Task` [F `RefreshFeedback.swift:58–60`].
  4. Result `null` → no feedback; `probe.outcome("nil")`.
  5. Result non-null → `feedback = outcome(result, userInitiated)`; `probe.outcome("result:<state>:<n>")`.
  6. `action` throws `CancellationException` → no feedback; `probe.outcome("cancelled")`; rethrow.
  7. `action` throws any other `Exception` (catch `CancellationException` first, step 6): `probe.outcome("error:<simple class name>")`; then `feedback = outcome(SyncResult(state, pendingCount = 0), userInitiated)` with `state = Offline` when the exception is 4b's `DataFailure.Offline`, else `Failed` [F `RefreshFeedback.swift:75–81`].
  8. `finally`: `refreshing = false`.
- `fun dismiss(presentationId: Long)`: clears `feedback` only if the id equals the current `presentationId`.
- `interface RefreshDebugProbe { fun accepted(); fun outcome(value: String); fun presented(text: String); companion object { val None } }`. The debug implementation is in T11.

### 5.5 Refresh capsule [F `PlanRefreshFeedback.swift`; R motion]

`RefreshFeedbackCapsule(state: RefreshFeedbackState, onDismiss: (Long) -> Unit, probe: RefreshDebugProbe, modifier)` in `…/ui/refresh/`:
- **Text:** each entry resolved in the UI, joined with the untranslatable separator `" · "` (`core/ui/src/main/res/values/strings_untranslatable.xml`, name `ui_refresh_feedback_separator`, value `" · "`).

  | Key | Resource |
  |---|---|
  | `UpToDate` | "Up to date" / "Alles is bijgewerkt" |
  | `TripsAdded` | plurals "%d trip(s) added" / "… toegevoegd", via `pluralStringResource(name, count, count)` |
  | `TripsUpdated` | plurals "… updated" / "… bijgewerkt" |
  | `TripsRemoved` | plurals "… removed" / "… verwijderd" |
  | `NeedsConnection` | "Needs a connection" / "Verbinding nodig" |
  | `SomethingWentWrong` | "Something went wrong" / "Er ging iets mis" |

  Find the exact resource names in `tools/l10n/names.lock.json` by iOS key (`"screens\t%lld trips added"` and so on). Do not guess them.
- **Look:** `Text` `screenBody`, colour `foreground`, centred, wraps. Inner padding horizontal `space4` (16), vertical `space2` (8). Fill `card`, shape pill (`CircleShape`/`RoundedCornerShape(50%)`), border `AuthTokens.borderWidth` (1) `border`. Outer padding horizontal 16, vertical 8.
- **Placement:** `Alignment.TopCenter` of the list area, i.e. directly below the top app bar, above the list. Apply `windowInsetsPadding(LocalSafeInsets.current.only(Horizontal))` before the outer padding, so the capsule stays inside the horizontal safe area (iOS `.planPageRail()`, `PlanPageSafeArea.swift`: horizontal safe-area padding only).
- **Semantics:** `Modifier.clearAndSetSemantics { contentDescription = text; liveRegion = LiveRegionMode.Polite; testTag = "refresh.feedback" }`. The tag goes inside the block.
- **Lifetime:** shown while `feedback` is non-empty. `LaunchedEffect(state.presentationId)` (only when shown) calls `probe.presented(text)`, then `delay(2000)`, then `onDismiss(presentationId)`.
- **Motion [R]:** `AnimatedVisibility`. Enter is `slideInVertically { -it } + fadeIn`, exit is `slideOutVertically { -it } + fadeOut`, both `tween(RefreshCapsuleMetrics.animationMillis = 350)`. When `CfTheme.reduceMotion` is on, use `fadeIn`/`fadeOut` only, with the same duration. (iOS uses `.animation(.default)` with move plus opacity [F l.27–30]; the plan asks for a fade under Reduce Motion.)

### 5.6 Shell navigation state [R; behaviour F `AppTabs.swift`]

In `…/nav/shell/` (package `net.chronoflow.core.navigation.shell`, pure Kotlin, no Compose):
```kotlin
enum class ShellTab { Today, Trips, Settings }   // root routes Route.Today, Route.Trips, Route.Settings
data class ShellNavState(
    val selected: ShellTab,
    val stacks: Map<ShellTab, List<Route>>,      // each list starts with its root route, never empty
    val overlay: Route? = null,                  // full-screen layer above the shell (Wizard)
    val scrollToTop: Map<ShellTab, Int> = emptyMap(),
)
```
Pure functions (`ShellNav.kt`):

| Function | Result |
|---|---|
| `initial(tab)` | Every stack is `[root]`; `selected = tab` |
| `select(tab)` with `tab != selected` | `selected = tab`; stacks unchanged |
| `select(tab)` with `tab == selected` and depth > 1 | That stack becomes `[root]` (pop to root; no scroll signal) |
| `select(tab)` with `tab == selected` and depth == 1 | `scrollToTop[tab] += 1` |
| `push(route)` | `Route.Wizard` → `overlay = route`; otherwise append to the selected stack |
| `openTrip(tripId)` | Append `Route.TripPage(tripId)` to the **selected** stack |
| `back()` → `Pair<ShellNavState, Boolean>` (handled) | If `overlay != null`, clear it. Else if depth > 1, pop. Else if `selected != Today`, select Today. Else unhandled (`false`: the activity finishes) |
| `wizardSaved(tripId)` | `overlay = null`, `selected = Trips`, Trips stack = `[Trips, TripPage(tripId, awaitingSync = true)]` [F l.170–176] |
| `fromRoot(route)` | `Today`/`Trips`/`Settings` → `initial(tab)`. `TripPage` → `initial(Trips)` with Trips stack `[Trips, route]` [F l.375–381]. `Wizard` → `initial(Trips)` with `overlay = route`. Anything else throws `IllegalArgumentException` |
| `seed(start: ShellStart)` | `ShellStart.Tab(t)` → `select(t)`. `ShellStart.Trip(id)` → Today stack `[Today, TripPage(id)]`, `selected = Today` [F l.266–277: iOS pushes onto the Today path] |

`sealed interface ShellStart { data class Tab(val tab: ShellTab); data class Trip(val tripId: String) }` also lives here.

### 5.7 The shell UI [R]

- **`CfNavigationSuiteScaffold(layout: CfNavLayout, items: List<CfNavItem>, selected: Int, onSelect: (Int) -> Unit, modifier, content)`** in `…/ds/system/` (`enum class CfNavLayout { Bar, Rail }`; `data class CfNavItem(label: String, icon: ImageVector, selectedIcon: ImageVector, tag: String)`):
  - item colours for bar **and** rail: selected icon and label `CfTheme.colors.primary` (the token, **not** M3 `primary`, which is `dawnText`), indicator `muted`, unselected icon and label `mutedForeground` (OQ-A3);
  - container: the M3 defaults through `toM3()` (bar `popover`, rail `background`; §4). Do not override them: there is no Android tab-bar board (OQ-6a-1);
  - labels always shown (`alwaysShowLabel = true`);
  - icon = `selectedIcon` when selected, else `icon`;
  - each item gets `Modifier.testTag(tag)`; the suite root is tagged `app.tabs`;
  - it never calls any hide or `NavigationSuiteScaffoldState` API.
- **Tabs** [F `AppTabs.swift`; OQ-A4]:

  | Tab | Label (en / nl) | Icon / selected | Tag |
  |---|---|---|---|
  | Today | Today / Vandaag | `MaterialSymbols.SpaceDashboard` / `SpaceDashboardFill` | `tab.today` |
  | My trips | My trips / Mijn reizen | `MaterialSymbols.Flight` / `FlightFill` | `tab.trips` |
  | Settings | Settings / Instellingen | `MaterialSymbols.Settings` / `SettingsFill` | `tab.settings` |

  Find the resource names in `names.lock.json` (`screens` or `app` table, key "Today", "My trips", "Settings"). Prefer the `screens_*` names; record the choice in the KDoc.
- **Layout:** `CfNavLayout.Rail` if `isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)`, else `Bar`. `WindowPolicy` is `object WindowPolicy { const val wideBreakpointDp = 600; const val listCardMaxWidthDp = 640 }` in `…/ds/layout/`. Reuse an existing constant if Phase 2 added one.
- **Insets:** `AppShell` re-provides `LocalSafeInsets` (`net.chronoflow.core.designsystem.layout`, moved by Phase 2 Task 0) to its content: `only(Top + Horizontal)` with the bar; `only(Top + End + Bottom)` with the rail. The bar and rail consume their own system insets.
- **Per-tab state:**
  - one `SaveableStateHolder`, with `SaveableStateProvider(tab)` around each tab's `NavDisplay`;
  - one `TabViewModelStoreOwner` per tab, held by the shell holder. It implements `ViewModelStoreOwner` and `HasDefaultViewModelProviderFactory`, forwarding the activity's `defaultViewModelCreationExtras` and factory. It is provided as `LocalViewModelStoreOwner` around that tab's `NavDisplay`;
  - each `NavDisplay` uses `rememberSaveableStateHolderNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator(removeViewModelStoreOnPop = { holder.state.selected == tab })`. A pop inside the selected tab clears that entry's ViewModel; switching tabs keeps it;
  - `DisposableEffect(Unit) { onDispose { holder.clear() } }` clears all three tab stores when the shell leaves composition (sign-out or a new root revision).
- **Only the selected tab's `NavDisplay` is composed.**
- **Back:**
  - the `NavDisplay` of the selected tab gets `onBack = { holder.back() }`;
  - plus `BackHandler(enabled = overlay == null && depth == 1 && selected != Today) { holder.back() }`;
  - plus, with the overlay shown, `BackHandler(enabled = true) { holder.back() }`, composed last so it wins;
  - nothing intercepts at the Today root;
  - predictive back is already on (4e manifest).
- **Scroll to top:** `LocalTabReselect` (`compositionLocalOf { 0 }`, `…/ui/shell/TabReselect.kt`) carries `scrollToTop[tab] ?: 0` for that tab. A screen reacts only when the value differs from the last one it handled. Keep that last value in `rememberSaveable`, so re-entering a tab never scrolls.
- **Overlay:** with `overlay != null`, a full-screen `Box(Modifier.fillMaxSize().background(background))` above the scaffold shows `placeholder(overlay)`, which gives the tag `route.<debugName>`, e.g. `route.wizard`. The tab bar is covered, as with the iOS `fullScreenCover`.
- **Seams** [F 4e §5.8 l.464]: a seam is 3b/4e's `RoutePlaceholder(route, label)`, unchanged. Its tag is `route.<route.debugName()>`, and it shows text only through the variant `label` hook (`DebugRouteLabel` in debug, `null` in perf and release). `AppShell` takes a parameter `placeholder: @Composable (Route) -> Unit`. `RootContent` passes the same lambda it already uses for its own placeholder entries, `{ RoutePlaceholder(it, label) }`. Do not add a new composable, string or tag scheme.
  - the Today root renders `placeholder(Route.Today)`; the Settings root renders `placeholder(Route.Settings)`;
  - any `TripPage` entry renders `placeholder(route)`, giving `route.tripPage`;
  - the tab bar stays visible on pushed pages (iOS keeps the TabView).
- **Route probe:** 4e's debug `RouteProbe` (`@Volatile var state: RootState`) keeps tracking the **root** route only. It reports `Today`, `Trips` or `TripPage` as the shell's root. Do not extend it: tests assert the `route.*` and `tab.*` tags.
- **Never hidden [F/R]:** on iOS 26, `AppTabs.swift:101` minimizes the tab bar on scroll down (`tabBarMinimizeBehavior(.onScrollDown)`). The Android plan overrides this: the bar is never hidden (plan 6a "Delivers", OQ-6a-9).

### 5.8 My trips

**Dependencies [R] (OQ-6a-3).** `:feature:trips` must not see `:app`.

```kotlin
interface TripsDependencies {
    fun tripRecords(): Flow<List<TripRecord>>               // TripRepository.values()
    fun planSnapshots(tripId: String): Flow<TripPlanSnapshot>  // TripRepository.detailValues
    fun synced(): Flow<Boolean>                              // AccountRepository.values().map { it?.syncedAt != null }
    fun preferences(): Flow<TripsPreferences>                // from SettingsRepository.values()
    val clock: PlanClock
    val decoder: PlanDecoder
    suspend fun formatter(): CopyLineFormatter              // catalog loaded off main, cached
    suspend fun refreshNow(): SyncResult?                    // sessionGraph.sync.refreshNow() (4e l.426; iOS `native?.refreshNow()`)
    val refreshProbe: RefreshDebugProbe
    val worker: CoroutineDispatcher                          // Dispatchers.Default in production
}
object TripsDependenciesKey : CreationExtras.Key<TripsDependencies>
data class TripsPreferences(val hidden: List<BlockType> = emptyList(), val timeFormat: TimeFormat = TimeFormat.Value24)
```
- `preferences` [F `PlanHomeModel`]: `hidden` from `preferences.hiddenCategories`, `timeFormat` from `preferences.timeFormat`, default `"24"`.
- `TripsViewModel.Factory = viewModelFactory { initializer { TripsViewModel(this[TripsDependenciesKey]!!) } }`.
- `:app` calls `viewModel(factory = TripsViewModel.Factory, extras = MutableCreationExtras(owner.defaultViewModelCreationExtras).apply { set(TripsDependenciesKey, deps) })`. `deps = SessionTripsDependencies(appGraph, sessionGraph)` is captured from `graph.sessionGraphs.current.value` at creation, so a screen never outlives its epoch (4e).

**UI model** (`…/trips/TripsUiState.kt`), with every class `@Immutable`:
```kotlin
sealed interface TripsUiState { data object Loading; data object Empty; data class Content(val sections: List<TripSectionUi>) }
data class TripSectionUi(val status: TripStatus, val trips: List<TripCardUi>)
data class TripCardUi(val id: String, val status: TripStatus, val flightNumber: String?, val offset: String,
    val from: AirportUi, val to: AirportUi, val times: String, val next: NextStepUi?)
data class AirportUi(val iata: String, val city: String?)
data class NextStepUi(val type: BlockType, val label: NextLabel, val text: String)
sealed interface NextLabel { data object Now; data class In(val duration: String) }
```

**`TripsViewModel`**:
- **Inputs:** `deps`, plus `fun setLanguage(language: Language)` backed by a `MutableStateFlow<Language?>(null)`. The screen calls it from `LaunchedEffect(language)`, where `language` comes from the 0e resolver (`resolveLanguage(LanguageChoice.System, LocalConfiguration.current.locales[0])`, or the helper 0e/2 already expose). `resolveLanguage` returns 0e's `AppLanguage`; map it to the plan `Language` (`En` → `En`, `Nl` → `Nl`) with the mapping 0e/1a already provide, or a one-line `when` in `:feature:trips` if none exists. Until a language is set, the state is `Loading`.
- **Pipeline** (all on `deps.worker`, via `flowOn`):
  1. `values = tripRecords().map { decoder.trips(it, now) }`. Decoding runs only on record changes. A tick re-derives each value's `status` with `tripDisplayStatus`, without decoding [F `TripsModel.updateNow`].
  2. `activeIds` = distinct ids with status `Active`, then `flatMapLatest`, then `combine(ids.map { planSnapshots(it).map { s -> it to decoder.detail(s) } })`. No active trips gives an empty map (`flowOf(emptyMap())`). A failed `detail` gives `null` for that trip (no Next row).
  3. `combine(values, details, clock.values(), synced(), preferences(), language)` → `TripsUiState`:
     - `!synced || !loaded` → `Loading`, where `loaded` = the first `values` emission arrived [F `AppTabs` loading branch];
     - no sections → `Empty`; else `Content(tripSections(…).map { … })`.
  4. `distinctUntilChanged()`, then `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)`.
- **Card strings** [F `TripSummaryCard.swift`], with `CopyFormat(language, timeFormat)`:
  - `offset = formatter.formatOffset(card.offsetDifferenceMinutes.toDouble(), language)`;
  - `times` = `formatDayMonth(departureUtc, from.zone) + " " + formatInstant(departureUtc, from.zone) + " → " + formatDayMonth(arrivalUtc, to.zone) + " " + formatInstant(arrivalUtc, to.zone)`;
  - `next` only when `status == Active` and a detail exists: `block = todayView(detail, detail.checkState.marks, now, hidden).nextStep`; `label = if (planDate(block.startUtc) <= now) Now else In(formatter.formatMinutes(max(0, ceil(minutes)), language, duration = true))`; `text = formatter.format(block.text, CopyFormat(language, timeFormat, displayZone = block.displayTz))`.
- **Refresh:**
  - `val refresh = RefreshFeedbackModel(viewModelScope, deps.refreshProbe)`, exposed as `refreshState: StateFlow<RefreshFeedbackState>`;
  - `fun refresh() { refresh.refresh { deps.refreshNow() } }`;
  - `fun dismissFeedback(id: Long) = refresh.dismiss(id)`.
- **Errors:** the decoder's first error is not shown (iOS keeps `error` but `TripsScreen` never shows it). Do not log card JSON.

**Screen** (`TripsScreen(viewModel, onOpenTrip: (String) -> Unit, onNewTrip: () -> Unit)` → stateless `TripsContent(state, refreshState, language, onRefresh, onDismissFeedback, onOpenTrip, onNewTrip, refreshProbe)`):
- **Root:** `Scaffold(topBar, contentWindowInsets = WindowInsets(0))` with `Modifier.testTag("trips.screen").nestedScroll(scrollBehavior.nestedScrollConnection)`, where `scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`.
- **Top bar:** `CfLargeTopAppBar(title = "My trips"/"Mijn reizen", scrollBehavior, actions = { IconButton(onNewTrip, Modifier.testTag("trips.create")) { Icon(MaterialSymbols.Add, contentDescription = "New trip"/"Nieuwe reis") } })`. No logo item (OQ-6a-6).
- **Body:**
  - `CfPullToRefreshBox(isRefreshing = refreshState.refreshing, onRefresh)`, wrapping **every** state, so a pull works on Loading and Empty too [F 07 §10.1];
  - inside it, one `LazyColumn(state = listState, Modifier.testTag("trips.list").fillMaxSize(), horizontalAlignment = CenterHorizontally, verticalArrangement = spacedBy(space3 = 12), contentPadding = PaddingValues(start = space4, end = space4, top = space1, bottom = space4) + LocalSafeInsets.current.only(Horizontal + Bottom).asPaddingValues())` (top `space1` = board `main` padding `4px 16px`; add the two `PaddingValues` component-wise in a small private helper). The safe-inset part keeps cards out of a landscape cutout and, with the rail, above the gesture bar (iOS `.planPageRail()`/`.planPageSafeArea()`, `TripsScreen.swift:90–92`);
  - every item gets `Modifier.widthIn(max = WindowPolicy.listCardMaxWidthDp.dp).fillMaxWidth()`;
  - the capsule overlay (§5.5) sits in the same `Box`, after the list.
- **Loading** (one item, key `"loading"`) [F `TripsLoadingView.swift`; B loading]:
  - `Column(spacedBy(space4 = 16))` holding `CfSkeleton(Modifier.fillMaxWidth(PlanUITokens.loadingHeadingWidthRatio /* 0.3 */).height(PlanUITokens.loadingHeadingHeight.dp /* 18 */), CircleShape)`, then `CfSkeleton(height PlanUITokens.tripsLoadingFirstHeight = 260, RoundedCornerShape(PlanUITokens.radius3Xl = 26))`, then the heading again, then `CfSkeleton(height PlanUITokens.tripsLoadingSecondHeight = 180, radius 26)` [F `PlanUITokens.generated.swift:26,27,44,45`; all are tokens, so none goes into `TripsMetrics`];
  - one node: `semantics(mergeDescendants = false) { contentDescription = "Loading your trips"/"Je reizen laden" }` and `testTag("trips.loading")`;
  - static (OQ-2-6).
- **Empty** (one item, key `"empty"`, tag `trips.empty`) [F `TripsScreen.swift`; B empty]:
  - `Column(horizontalAlignment = CenterHorizontally, spacedBy(space3 = 12))` with top margin `space6` (24), padding horizontal `space5` (20) and vertical `space8` (32);
  - dashed border `AuthTokens.focusWidth` (2) in `border`, dash `space1` (4) on and 4 off, radius 26. Draw it with `drawBehind` and a `PathEffect.dashPathEffect`; there is no fill;
  - contents:
    - a 56 dp (`space14`) `muted` circle with lucide `plane` at 24 dp (`space6`) tinted `foreground`. [F] The board's svg uses `stroke="currentColor"`, which inherits `var(--foreground)` from the page root, and iOS uses the default foreground style. The glyph is decorative (`contentDescription = null`);
    - "No trips yet" (`screenCardTitle`, `foreground`), with the heading semantics;
    - body "Your upcoming, active and past trips show up here." (`screenBodyLg`, `mutedForeground`, centred);
    - `CfButton("Plan a trip"/"Plan een reis", onNewTrip, style = CfButtonStyle.Primary, size = CfButtonSize.Lg, fillWidth = true)` (board `h-10 px-6 text-sm font-medium rounded-full bg-primary text-primary-foreground`, `align-self: stretch`; label `primaryForeground` [F 2026-11-09 contrast fix]), tag `trips.create-empty`.
- **Sections:**
  - header item: key `"section:<status>"`, `contentType = "header"`, `Text(statusLabel.uppercase(locale of language))`, `screenLabel`, `mutedForeground`, `semantics { heading() }`, tag `trips.section.<active|upcoming|completed>`;
  - every header after the first gets an extra `padding(top = space3)`, so sections sit 24 apart and items inside a section 12 apart [F iOS spacing 24/12; B gap 24/12];
  - cards: key `trip.id`, `contentType = "trip"`.
- **Trip card** (`TripCard(card: TripCardUi, onClick)`, `…/trips/TripCard.kt`) [F `TripSummaryCard`, `planCard`; B Trips]:
  - **Container:** `Column(spacedBy(space4 = 16))`, padding `space5` (20), shape radius 26, background `card`, content colour `cardForeground`. Border: `Active` → 2 dp `dawn`; else 1 dp `border`. `softShadow(shape, dark = CfTheme.colors.isDark)` unless `Completed` [F board: the Completed card has no `box-shadow`]. `graphicsLayer { alpha = completedCardOpacity (0.9) }` when `Completed`. Then `cfClickable(onClick, Role.Button, focusShape = shape)` (it already carries the `CfPressIndication`, board `cf-press`), `semantics(mergeDescendants = true)` and `testTag("trip.<id>")`.
  - **Header** (`FlowRow(spacedBy(space2 = 8))`):
    - `CfBadge(statusText, variant = Active → Dawn, Upcoming → Outline, Completed → Secondary with icon LucideGlyphs check 12 dp)`;
    - `MetaChip(flightNumber)` when non-null;
    - offset `Text` (`screenMeta`, `mutedForeground`, `fontFeatureSettings = "tnum"`) with `Modifier.weight(1f)` and `TextAlign.End`.
  - **Airports:** `Row(spacedBy(space3 = 12), verticalAlignment = CenterVertically)`:
    - from: `Column(weight 1, start)`: IATA `screenPageTitle`, city `screenBody` `mutedForeground` when non-null;
    - connector: `Row(spacedBy(space1 = 4))` of a 24 dp dashed 1 dp line, lucide `plane` 16 dp and another dashed line, all `mutedForeground`, dash 4 on 4 off, decorative;
    - to: `Column(weight 1, end-aligned)`.
  - **Times:** `Row`: `Text(times)` (`screenBody`, `mutedForeground`, tnum, weight 1). When `next == null`, add lucide `chevron-right` 20 dp (`space5`) `mutedForeground`, decorative.
  - **Next row** (when `next != null`):
    - `Row(spacedBy(12), CenterVertically)`, `heightIn(min = 56)`, padding vertical 10 / horizontal 12 (`@BoardMetrics("Trips")`), background `surface2`, shape `radius2Xl` (22);
    - contents: `CategorySwatch(type, CategorySwatchSize.Small)`; then a `Column(spacedBy(space1))` of the eyebrow (`screenEyebrow`, `dawnText`, uppercased per the board, OQ-6a-5: `"Next · now"` or `"Next · in %1$s"` with the duration) and the title (`screenBodyLg`, `foreground`); then a chevron 20 dp `mutedForeground`.
  - **Status labels:** "Active"/"Actief", "Upcoming"/"Binnenkort", "Completed"/"Afgerond".
- **Reselect:** when `LocalTabReselect` changes (per §5.7), call `listState.animateScrollToItem(0)`, or `scrollToItem(0)` under Reduce Motion. Then set `scrollBehavior.state.heightOffset = 0f` and `contentOffset = 0f`.
- **Card/board divergences** are listed in OQ-6a-5; the default is **board look, iOS behaviour**.
- **Every dp literal** above that is not a token goes in `internal object TripsMetrics` (its own file `TripsMetrics.kt`, so `internal`, not `private`) annotated `@BoardMetrics("Trips")` (Konsist `featureDpLiteralsComeFromBoardMetrics`).

### 5.9 Shared pieces in `:core:ui` [R; B bundle.js, F `PlanComponents.swift`]

- **`MetaChip(text: String, modifier)`** (`…/ui/trip/MetaChip.kt`) [B `rounded-full border border-current/20 px-2 py-0.5 text-xs tnum`]:
  - pill; padding horizontal 8, vertical 2;
  - border 1 dp in `LocalContentColor.current.copy(alpha = 0.2f)`;
  - text `screenMeta` (12/16), tnum, one line, `ellipsizable()`.
- **`CategorySwatch(type: BlockType, size: CategorySwatchSize = Regular, modifier)`** (`…/ui/plan/CategorySwatch.kt`, package `net.chronoflow.core.ui.plan`, the path the 6d brief expects; OQ-6a-12):
  - size: `Small` = 28 dp (iOS `space6 + space1`; B sm 28); `Regular` = 32 dp (iOS `space8`);
  - shape radius `radiusLg` (14);
  - fill, foreground, pattern and dashed come from `TimelineCategoryStyle(type)` (1c §5.1);
  - glyph: the lucide `TimelineGlyph` of that style, 14 dp at `Small` (B) and 16 dp at `Regular`, tinted `foreground`;
  - patterns:
    - dots: `PlanUITokens.patternDotSpacing`/`patternDotDiameter` at `patternDotOpacity`;
    - hatch: "/" stripes at `patternHatchSpacing`/`patternHatchWidth`/`patternHatchOpacity`, clipped to the shape;
  - dashed: 1 dp `foreground` stroke, dash 4/4;
  - decorative: `clearAndSetSemantics {}`;
  - **check:** compare 1c §5.1 with iOS `CategorySwatch.fill/foreground` (`PlanComponents.swift:58–80`) and the dashed set `[lightOk, noCaffeine, note]` (l.25). Where they differ, follow the **iOS swatch mapping** in a private `swatchStyle(type)`, and list each difference in the KDoc.
- **Metrics [R]:** Konsist `featureDpLiteralsComeFromBoardMetrics` also covers `core/ui/src/main`. Every dp literal above that is not a token goes into `private object TripChipMetrics` / `CategorySwatchMetrics` / `RefreshCapsuleMetrics`, each annotated `@BoardMetrics("Trips")`. The chip uses vertical 2 and the swatch glyphs 14/16. The capsule's `animationMillis` is a plain `Int` and needs no annotation. Prefer a token wherever one exists: 8 = `space2`, 28 = `space6 + space1`, 32 = `space8`, 4 = `space1`.
- **`CfBadge`** (`:core:designsystem`) gets an additive parameter `icon: ImageVector? = null`. When set, a 12 dp icon in the text colour sits before the text with a gap of 4 (`space1`). Existing callers are unchanged.

## 6. Kotlin shape [R]

| Type / file | Module, package |
|---|---|
| `TripListValue`, `TripsDecodeResult`, `PlanDecoder.trips/card/adaptation`, `TripSection`, `tripSections`, `TodayPlan`, `todayView`, `calendarDaysUntil` | `:core:plan` `net.chronoflow.core.plan` |
| `TripPlanSnapshot.trip` | `:core:model` `…model.store` |
| `TripRepository.detailValues` | `:core:data` (the 4d repository package) |
| `RefreshFeedback`, `RefreshFeedbackKey`, `outcome`, `RefreshFeedbackModel`, `RefreshFeedbackState`, `RefreshDebugProbe` | `:core:sync` `net.chronoflow.core.sync.refresh` |
| `ShellTab`, `ShellNavState`, `ShellNav` functions, `ShellStart` | `:core:navigation` `…navigation.shell` |
| `CfNavigationSuiteScaffold`, `CfNavLayout`, `CfNavItem`, `WindowPolicy`, `CfBadge(icon)` | `:core:designsystem` `…designsystem.system`, `…designsystem.layout` |
| `MetaChip`, `CategorySwatch`, `RefreshFeedbackCapsule`, `LocalTabReselect` | `:core:ui` `…ui.trip` (`MetaChip`), `…ui.plan` (`CategorySwatch`), `…ui.refresh`, `…ui.shell` |
| `TripsDependencies`, `TripsDependenciesKey`, `TripsPreferences`, `TripsViewModel`, `TripsUiState`, `TripsScreen`, `TripsContent`, `TripCard`, `TripsEmpty`, `TripsLoading`, `TripsMetrics` | `:feature:trips` `net.chronoflow.feature.trips` |
| `AppShell`, `AppShellScaffold` (slots, testable), `ShellHolder`, `TabViewModelStoreOwner`, `SessionTripsDependencies` | `:app` `net.chronoflow.app.shell` |
| `shellStart: () -> ShellStart?` and `refreshProbe: RefreshDebugProbe`, two members added to **the existing variant hook that carries `LaunchOverrides`** (find its real name with `grep -rn "LaunchOverrides.None\|DebugLaunchOverrides\|MockSources" app/src`). Release twin: `{ null }` and `RefreshDebugProbe.None`. The perf twin, if the hook has one, gives the same values as release. | `:app` main interface; implementations in the hook's existing source sets |
| `DebugRefreshProbe`, `DebugShellStart` (the seed mapping) | `app/src/debug` `net.chronoflow.debug` |

`:feature:trips` depends on `:core:plan`, `:core:model`, `:core:sync`, `:core:ui` and `:core:designsystem` only. It does not depend on `:core:data`, another feature or `:app`.

## 7. Tasks

### T1. Trip list values and sections (`feat(plan): port trip list values and sections`)
- Files: `…/plan/TripListValue.kt`, `…/plan/TripSections.kt`, and `PlanDecoder.kt` (add `trips`, `card`, `adaptation`) per §5.1.
- **Tests** (JVM, `core/plan/src/test/…`):
  - `TripsTests.tripsSectionsKeepRNNewestFirstAndStableTies`: port of `KitT/PlanUITests/TripsTests.swift:45`. Ids come out `["new", "b", "a"]` in one section.
  - `TripSectionsTest` (Android-only):
    - an `Unknown` status is dropped;
    - empty sections are omitted;
    - the order is Active, Upcoming, Completed.
  - `PlanDecoderTripsTest` (Android-only):
    - a tombstoned record (`deletedAt`) and a `deletedLocally` record are skipped;
    - a bad card JSON is skipped, `error` is the first error, and the other cards are returned;
    - decoding runs on the injected worker (the 1a dispatch-count technique).

### T2. Today logic (`feat(plan): port the today view`)
- File: `…/plan/Today.kt` per §5.2.
- **Tests** (JVM, `TodayTests`):
  - `todaySeparatesCurrentAndFourUpcomingAndDoesNotHideTheNextImportantStep` and `calendarDaysUseThePlanZoneAcrossDaylightSaving`: ports of `KitT/PlanUITests/TodayTests.swift:28,45`, with the same fixtures (use the 1a test fixture loader);
  - Android-only `nextStepIgnoresHiddenCategories`: the same `nextStep` with `hidden = all categories`.

### T3. Plan snapshot observation (`feat(data): observe a trip's plan snapshot`)
- Files: `core/model/…/store/TripPlanSnapshot.kt` (the `trip` field), the 4a `.sq` file that holds the `trips` queries in `:core:database` (the named query `tripById`, unless 4d already observes a single trip), and `core/data/…/repository/TripRepository.kt` (`detailValues`, §5.3). Update 1a's `PlanDecoder.detail(snapshot)` callers and tests only for the constructor change.
- **Tests** (JVM, `core/data/src/test/…`, on the 4d file-backed `SyncStore`), class `DetailValuesTest`:
  - an unknown trip emits `TripPlanSnapshot(trip = null, plan = null)`;
  - inserting the plan version at `currentInputHash` emits it with `plan != null`;
  - a check mark on another `input_hash` is excluded;
  - an enqueued check-off for this trip appears in `pendingChecks`, and one for another trip does not;
  - an unrelated write (another trip's row) does not emit, because of `distinctUntilChanged`.

### T4. Refresh feedback (`feat(sync): port refresh feedback`)
- Files: `…/sync/refresh/RefreshFeedback.kt` and `RefreshFeedbackModel.kt` per §5.4.
- **Tests** (JVM, `RefreshFeedbackTests`, names from `KitT/PlanUITests/RefreshFeedbackTests.swift`; the model gets a `TestScope`; there is no real delay):
  - `statesUseExistingCopy`: each state gives the key from §5.4. The en/nl text half is in T7;
  - `addedUpdatedRemovedUseCatalogPlurals` (counts 1 and 3): the keys and counts, in order;
  - `partialSyncDoesNotClaimUpToDate`: `pendingCount = 1` gives `SomethingWentWrong`, and so do `retryAfter` and refusals;
  - `automaticSyncNeverProducesFeedback`: every state with `userInitiated = false` gives `[]`;
  - `refreshWaitsForSyncAndDismissesItsOutcome`: `refreshing` is true until a `CompletableDeferred` completes, then the feedback is set; `dismiss(id)` clears it;
  - `anOlderPresentationCannotDismissANewerRefresh`;
  - `debugCounterRecordsOnlyAcceptedUserRefreshes`: a fake probe; a second `refresh` while refreshing is not counted; `userInitiated = false` is not counted;
  - `thrownFailuresAndCancellation`: `DataFailure.Offline` gives `NeedsConnection`; `IllegalStateException` gives `SomethingWentWrong`; `CancellationException` gives no feedback;
  - `finishedSyncIsReportedWhenTheGestureTaskIsCancelled`: a caller coroutine `join`s the returned job and is cancelled; the sync completes; the feedback is set;
  - `syncKeepsRunningWhenTheGestureTaskIsCancelledMidFlight`: the caller is cancelled while the action is suspended; the action still runs to completion (a counter after its suspension point equals 1).

### T5. Shell navigation state (`feat(navigation): add the tab shell state`)
- Files: `…/nav/shell/ShellNavState.kt` and `ShellNav.kt` per §5.6.
- **Tests** (JVM, `ShellNavStateTest`), one test per table row of §5.6, plus:
  - `selectingAnotherTabKeepsEveryStack`;
  - `backFromNonTodayRootSelectsTodayThenIsUnhandled`;
  - `reselectAtRootIncrementsOnlyThatTabsSignal`;
  - `fromRootRejectsAuthRoutes` (`Route.SignIn` throws);
  - `routesHoldNoSecret`: `TripPage(id).toString()` contains only the id and defaults.

### T6. Navigation suite wrapper and badge icon (`feat(designsystem): add the tab suite and a badge icon`)
- Files: `…/ds/system/CfNavigationSuiteScaffold.kt`, `…/ds/layout/WindowPolicy.kt`, and `CfBadge` (`icon`) per §5.7 and §5.9.
- **Tests** (Robolectric, `core/ui/src/test/…` next to the Phase 2 guards):
  - `CfNavigationSuiteScaffoldTest` (`@Logic`):
    - `Bar` gives a node with role tab per item and no rail node;
    - `Rail` the reverse (assert by the tags `app.tabs` plus a `cf.nav.bar`/`cf.nav.rail` test tag on the bar and rail);
    - the selected item's icon colour equals `CfTheme.colors.primary` (read the tint with a `LocalContentColor` probe inside the icon slot);
    - unselected equals `mutedForeground`.
  - `CfNavigationSuiteGuardsTest` (`@Layout`, profiles `small`, `pixel8`, `foldInner`, `largeFont` × en/nl × light/dark): the three tab labels fit (`assertTextFits`) and the touch targets are ≥ 48 dp.
  - Extend `CfBadgeGuardsTest` with "Completed"/"Afgerond" plus the check icon.

### T7. Meta chip, category swatch and refresh capsule (`feat(ui): add meta chip, category swatch and refresh capsule`)
- Files: `…/ui/trip/MetaChip.kt`, `…/ui/plan/CategorySwatch.kt`, `…/ui/refresh/RefreshFeedbackCapsule.kt`, `…/ui/refresh/RefreshFeedbackText.kt` (`@Composable fun refreshFeedbackText(list): String`), `…/ui/shell/TabReselect.kt`, and the separator resource, per §5.5, §5.7 and §5.9.
- **Tests** (Robolectric):
  - `RefreshFeedbackTextTest` (`@Logic`), the text half of `statesUseExistingCopy`:
    - en: "Up to date", "Needs a connection", "Something went wrong", "1 trip added", "3 trips updated";
    - nl: "Alles is bijgewerkt", "Verbinding nodig", "Er ging iets mis", "1 reis toegevoegd", "3 reizen bijgewerkt" (take the exact nl from the generated XML);
    - two entries are joined with `" · "`.
  - `RefreshFeedbackCapsuleTest` (`@Logic`, `mainClock.autoAdvance = false`):
    - with feedback, exactly one `refresh.feedback` node with that content description and live region `Polite`;
    - `probe.presented` is called once;
    - after `advanceTimeBy(1999)`, `onDismiss` has not been called; after 1 more ms it is called once with the current id;
    - a new `presentationId` restarts the 2 s;
    - motion: 100 ms after appearing, the node's top equals its final top with Reduce Motion on and is above it with Reduce Motion off.
  - `CategorySwatchTest` (`@Logic`): `Small` is 28 × 28 dp and `Regular` 32 × 32; the node has no semantics.
  - `MetaChipGuardsTest`, `RefreshCapsuleGuardsTest` (`@Layout`, 8 cases each per Phase 2 l.112):
    - chip "KL 861";
    - capsule "3 trips added · 1 trip removed" and the nl equivalent: fits and wraps without clipping at `small` and `largeFont`.

### T8. Trips view model (`feat(trips): add the trips view model`)
- Files: `…/trips/TripsDependencies.kt`, `TripsUiState.kt`, `TripsViewModel.kt` per §5.8. Plus `feature/trips/build.gradle.kts` dependencies per §6.
- **Tests** (JVM, `feature/trips/src/test/…`, `FakeTripsDependencies` with `MutableStateFlow`s, a `FakePlanClock`, `Dispatchers.setMain(StandardTestDispatcher)`, `runTest(timeout = 60.seconds)`):
  - `TripsTests.fixedClockFinishesWithoutBlockingInitialLoadOrLaterRepositoryUpdates` (`KitT/…/TripsTests.swift:10`): with a `FixedPlanClock`, the first records give `Content`, and a later repository emission updates it;
  - `TripsTests.tombstonedTripsDisappearAndEmptyFirstLoadFinishes` (`:63`): an empty first load gives `Empty`, not stuck `Loading`; a tombstoned record vanishes;
  - `DecodeRecoveryTests.badCardIsSkippedWhileOtherTripsKeepUpdating` (`DecodeRecoveryTests.swift:64`);
  - `ModelTests.stopReleasesModelAndRestartObserves` (`ModelTests.swift:115`), adapted:
    - after the last subscriber leaves and 5 s of virtual time pass, the fake `tripRecords` flow has `subscriptionCount == 0`;
    - a new subscriber collects again and gets fresh records;
  - `TripsViewModelTest` (Android-only):
    - `loadingUntilSynced`: `synced = false` gives `Loading` even with records;
    - `statusMovesWithTheClock`: an upcoming trip becomes `Active` after the clock passes `planStartUtc`, without a new `decoder.trips` call (count calls);
    - `nextOnlyForActiveTrips`: active with a plan gives `next`; upcoming with a plan gives none;
    - `nextLabelNowAndIn`: start ≤ now gives `Now`; start = now + 29 min 10 s gives `In("30 min")` (assert whatever 1a `formatMinutes` returns for 30, en);
    - `checkedStepIsSkipped`: marking the next step through a pending check-off moves `next` to the following actionable block;
    - `minuteTickWithoutChangesDoesNotEmit`: with only completed and upcoming trips (no labels change), advancing the clock by one minute gives no new emission;
    - `languageChangeReformats`: `setLanguage(Nl)` changes `offset`/`times` text;
    - `refreshDelegatesAndReportsFeedback`: `refresh()` calls `refreshNow` once; the state ends with `UpToDate`; a second `refresh()` while in flight is ignored;
    - `formatterIsLoadedOffMain`: `formatter()` is invoked on the worker.

### T9. My trips screen (`feat(trips): build the my trips screen`)
- Files: `…/trips/TripsScreen.kt` (`TripsScreen`, `TripsContent`), `TripCard.kt`, `TripsEmpty.kt`, `TripsLoading.kt`, `TripsMetrics.kt`, `TripsStatusText.kt`, per §5.8.
- **Board fixture** (`feature/trips/src/test/…/TripsBoardFixture.kt`): `TripsUiState.Content` built from the `B/Trips.dc.html` default state:
  - **Active** AMS (Amsterdam) → NRT (Tokyo), KL 861, offset "+7 h", times "12 Oct 14:40 → 13 Oct 09:15", next row with the board's eyebrow "Next · in 30 min" and the board's title text;
  - **Upcoming** NRT → AMS, KL 862, "−7 h", "21 Oct 10:25 → 21 Oct 15:30";
  - **Completed** AMS → JFK (New York), KL 641, "−6 h", "3 Jun 10:10 → 3 Jun 12:20";
  - nl variants use the nl strings.
- **Tests:**
  - `TripCardFormattingTest` (JVM): the three board cards built as `TripCardView`s through the real formatter give exactly the board's `times` and `offset` strings in en. If 1a's formatter output differs from a board string, the formatter wins; record the difference in the T13 doc.
  - `TripsGuardsTest` (`@Layout`): states Content, Empty and Loading × profiles `small`, `pixel8`, `foldInner`, `largeFont` × en/nl × light/dark (48 cases). Each calls `assertTextFits`, `assertTouchTargets`, `assertInteractiveInSafeArea`, `assertNoInteractiveOverlap`, `assertInk`. At `foldInner` the card width is ≤ 640 dp and centred (±1 dp).
  - `TripsContentTest` (`@Logic`):
    - tags `trips.screen`, `trips.create`, `trips.section.active|upcoming|completed`, `trip.<id>`, `trips.empty`, `trips.create-empty`, `trips.loading` exist in their states;
    - a card click calls `onOpenTrip(id)`; "+" and "Plan a trip" call `onNewTrip`;
    - each card is one merged node with `Role.Button`;
    - `performTouchInput { swipeDown() }` on `trips.list` calls `onRefresh` in the Loading, Empty and Content states;
    - a reselect signal change scrolls a 30-card list back to index 0; re-providing the **same** signal after recomposition does not scroll.
  - Captures: `make captures ONLY=Trips,TripsEmpty,TripsLoading` (add the three to the capture registry, light and dark, `pixel8`).

### T10. Three-tab shell (`feat(app): add the three-tab shell`) [ticks "Shell" and "Seams"]
- Files: `…/app/shell/AppShell.kt` (`AppShell(root: Route)` wiring the graph), `AppShellScaffold.kt` (stateless slots: `today`, `trips`, `settings`, `page: @Composable (Route) -> Unit`, `overlay`), `ShellHolder.kt`, `TabViewModelStoreOwner.kt`, and the `RootContent` change: `Today`, `Trips`, `Settings`, `TripPage` and `Wizard` render `AppShell(root = route)` inside the existing `key(revision)`, with the initial state `fromRoot(route)` (§5.6).
- `AppShell(root: Route, placeholder: @Composable (Route) -> Unit)`. `RootContent` passes its existing `{ RoutePlaceholder(it, label) }` (§5.7 "Seams").
- In this task the Trips tab root renders `placeholder(Route.Trips)` (`route.trips`). T11 replaces it.
- **Tests** (Robolectric `@Logic`, `AppShellTest`, on `AppShellScaffold` with fake slot content: a `LazyColumn` of 50 items tagged `test.list.<tab>` plus a button that calls `openTrip("t1")`):
  - `barBelow600`: at `pixel8` the bar tag exists and the rail does not;
  - `railFrom600`: at `foldInner` and `landscapeSideNav` the rail exists;
  - `barNeverHidesOnScroll`: after `performScrollToIndex(49)` the bar's bounds are unchanged and it is displayed;
  - `backFromTripsRootSelectsTodayThenFinishes`: `onBackPressedDispatcher.onBackPressed()` from the Trips root shows the Today content; a second back makes `activity.isFinishing` true;
  - `backPopsPushedPageFirst`: `openTrip` → `route.tripPage`; back → the tab root;
  - `predictiveBackCancelKeepsThePage`: `dispatchOnBackStarted(BackEventCompat(…))`, then progressed, then cancelled; the page is still shown;
  - `reselectPopsToRootThenScrollsToTop`;
  - `tabSwitchKeepsScrollAndViewModel`: in Trips scroll to 30; read a `viewModel<CounterViewModel>()` instance id; switch to Settings and back; the first visible index is 30 and the instance is the same;
  - `popClearsThePagesViewModel`: a VM in the page entry gets `onCleared` after back;
  - `wizardOverlayCoversTabsAndBackCloses`: `push(Route.Wizard(Create))` shows `route.wizard` and no tab node is displayed; back removes it;
  - `insetsDifferForBarAndRail`: the slot reads `LocalSafeInsets` and gets bottom 0 with the bar and the system bottom inset with the rail.
- Update the 4e smoke and launch tests only if they assert a node that the shell now wraps. `route.today` must still exist after a signed-in launch.

### T11. Host My trips, seeds and the refresh probe (`feat(app): host my trips in the shell`) [ticks "My trips" (New trip seam, `TripPage` seam) and "Pull to refresh"]
- `SessionTripsDependencies(appGraph, sessionGraph)` in `…/app/shell/`, per §5.8. `formatter()` = `CopyLineFormatter(appGraph.copyCatalogs.catalog())`, computed once per dependencies instance under a `Mutex`. `copyCatalogs` is 1a's `CopyCatalogProvider(dispatchers.io)`, which loads once and off main. If `AppGraph` has no such member yet (1a: "the app graph will own one provider (later phase)"), add `val copyCatalogs: CopyCatalogProvider by lazy { CopyCatalogProvider(dispatchers.io) }` in this commit. `refreshNow()` = `sessionGraph.sync.refreshNow()`, using the `SessionGraph` captured at creation.
- The Trips root entry: `TripsScreen(viewModel(factory = TripsViewModel.Factory, extras = …), onOpenTrip = holder::openTrip, onNewTrip = { holder.push(Route.Wizard(WizardMode.Create)) })`.
- Wrap the Trips tab content in `CompositionLocalProvider(LocalTabReselect provides holder.state.scrollToTop[Trips] ?: 0)`.
- **Variant hook members** (§6). Add them to the existing hook that carries `LaunchOverrides`, next to 4e's `MockSources`. Never create a `VariantHooks` class or any second mechanism:
  - **`shellStart: () -> ShellStart?`**:
    - release (and perf) returns `null`;
    - debug (`DebugShellStart` in `…/debug/`) maps `DebugSeedRouting.current?.planRoute` (4e `PlanRouteSeed`): `trips` → `Tab(Trips)`, `trip:<uuid>[:list]` → `Trip(uuid)`. It returns the value **once per seed apply**, using an `AtomicBoolean consumed` that `TestSeeds.apply` resets;
    - `AppShell` applies it with `seed()` on its first composition after launch.
  - **`refreshProbe: RefreshDebugProbe`**:
    - release (and perf) returns `RefreshDebugProbe.None`;
    - debug returns `DebugRefreshProbe`, an `object` in `…/debug/` with `AtomicInteger accepted`, `@Volatile var outcome = "none"`, `@Volatile var presented = "none"`, and `reset()`, which `TestSeeds.apply` calls. These three fields are iOS `debugRefreshCount`, `debugOutcome` and `debugPresented` (`RefreshFeedback.swift:39–41`, initial values `0`/`"none"`/`"none"`). Android records them in process for the instrumented test and has no hidden `refresh.debug.*` nodes (OQ-6a-10).
- **Tests:**
  - `ShellSeedTest` (`@Logic`, debug):
    - `CF_PLAN_TEST_ROUTE=trips` selects My trips;
    - `trip:<uuid>` shows `route.tripPage` with the **Today** tab selected;
    - an invalid value is ignored (Today);
    - a second composition does not re-apply the seed.
  - `TripsTabTest` (`@Logic`, debug, `MockAppSeed` signed-in demo through 4e's Robolectric seed path):
    - My trips shows `trips.screen` and at least one `trip.` card with the mock's active trip id (use the 4c fixture constant);
    - clicking it shows `route.tripPage`;
    - "+" shows `route.wizard`.
  - `ReleaseReadinessTest` (extend 4e's, `app/src/testRelease`):
    - the release hook's `shellStart()` is `null` and its `refreshProbe` is `RefreshDebugProbe.None`;
    - `Class.forName` throws `ClassNotFoundException` for `net.chronoflow.debug.DebugRefreshProbe` and `net.chronoflow.debug.DebugShellStart`.

### T12. Emulator journey (`test(app): prove the shell journey on the emulator`) [ticks "Tests"]
- `app/src/androidTest/…/ShellJourneySmokeTest` (`@Smoke`, one test, `pixel`, launched seeded: signed-in `demo`, `CF_PLAN_TEST_ROUTE=trips`, mock delay 0):
  1. `trips.screen` is shown and `tab.trips` is selected;
  2. `swipeDown` on `trips.list`; wait (`waitUntil`, ≤ 3 s) for `DebugRefreshProbe.accepted.get() == 1` and `presented != "none"`, and assert the `refresh.feedback` node exists;
  3. tap the active trip card, giving `route.tripPage`, with the tab bar still displayed;
  4. system back (`UiDevice.pressBack()`) returns to `trips.screen`;
  5. open the card again, then tap `tab.trips` (reselect), giving `trips.screen`;
  6. tap `tab.today`, giving `route.today`; tap `tab.trips`, giving `trips.screen` (stack kept at root);
  7. back gives `route.today`; back finishes the activity (`ActivityScenario` state `DESTROYED`).
- The whole test stays under 10 s; check the tier timing listener.
- Predictive back is covered by `AppShellTest` in T10, not here (OQ-6a-14).

### T13. Docs and plan (`docs(trips): record phase 6a results`)
- Check that the five 6a checkboxes are ticked (T10: Shell, Seams; T11: My trips, Pull to refresh; T12: Tests). Tick nothing else here.
- Add OQ-6a-1…OQ-6a-14 to `plan.md` "Open questions for the user" with their defaults.
- Record in `docs/phases/phase-6a-shell-trips.md` (an appended "Results" section):
  - the verified 1.4.0 / 1.3.0 / 2.11.0 API names (§4);
  - the capture comparison per board, with every remaining difference and its board name;
  - the formatter-versus-board string differences from T9;
  - the swatch mapping differences from T7.

## 8. Acceptance criteria

- `make quick` and `make check` pass. `./gradlew :konsist:test :app:testReleaseUnitTest` passes.
- Targeted runs pass:
  - `make targeted ONLY="net.chronoflow.core.plan.TripsTests net.chronoflow.core.plan.TripSectionsTest net.chronoflow.core.plan.PlanDecoderTripsTest net.chronoflow.core.plan.TodayTests"`
  - `make targeted ONLY=net.chronoflow.core.sync.refresh.*`
  - `make targeted ONLY=net.chronoflow.core.navigation.shell.*`
  - `make targeted ONLY=net.chronoflow.feature.trips.*`
  - `make targeted ONLY=net.chronoflow.app.shell.*`
- Every ported Swift test name in T1–T4 and T8 exists with the same words.
- Guards: `TripsGuardsTest` (48 cases) and `CfNavigationSuiteGuardsTest` pass. The bar is used at `pixel8` and the rail at `foldInner` (`AppShellTest`).
- Screenshots: `make captures ONLY=Trips,TripsEmpty,TripsLoading` produced light and dark captures. The Opus reviewer compared them with `B/Trips.dc.html` (default, empty, loading). Each difference is fixed or recorded with the board name.
- The emulator test `ShellJourneySmokeTest` passes on `pixel` in under 10 s. It is the only new emulator test.
- Performance (functional, no benchmark row):
  - `minuteTickWithoutChangesDoesNotEmit` passes;
  - all decoding and `todayView` run on the worker (tests);
  - list items use stable keys and `contentType`;
  - every UI model is `@Immutable`;
  - debug StrictMode is clean while scrolling My trips and pulling to refresh on the emulator run (no disk or network on main).
- `rg -n "MaterialTheme.colorScheme" feature core/ui app/src/main` finds nothing new. Konsist `featureDpLiteralsComeFromBoardMetrics` passes.
- `rg -n "Log\.|println" feature/trips/src/main core/sync/src/main/kotlin/net/chronoflow/core/sync/refresh` finds nothing.

## 9. Review checklist (Opus reviewer)

1. **Sections and sort.** Active, Upcoming, Completed. Departure descending, ties by id descending. `Unknown` is dropped. Empty sections are omitted. Tombstones are skipped before decoding.
2. **Next step.** It appears only on Active cards and comes only from the local plan (no server fallback). `hidden` does not change it. A pending check-off counts as marked. Decoding and `todayView` run off main.
3. **Loading rule.** `Loading` while `!synced || !loaded`. An empty first load ends in `Empty`.
4. **Refresh model.**
   - The work runs in the VM scope, not in the gesture.
   - A second pull while refreshing is ignored and not counted.
   - `CancellationException` is rethrown and gives no feedback.
   - An older presentation cannot dismiss a newer one.
   - `retryAfter`, `pendingCount` or refusals never give "Up to date".
5. **Capsule.**
   - One node: tag `refresh.feedback`, live region Polite.
   - Dismissed after 2000 ms keyed by presentation id.
   - Fade only under Reduce Motion.
   - Placed below the top app bar.
   - Plurals come from `<plurals>`; the separator comes from the untranslatable resource.
6. **Shell.**
   - Bar below 600 dp and rail from 600 dp, by width only; never a drawer.
   - No hide-on-scroll.
   - Selected tint is `CfTheme.colors.primary` (not M3 `primary`); indicator `muted`; unselected `mutedForeground`.
   - Filled icon when selected.
7. **Back.**
   - Within a stack, pop.
   - On a non-Today root, Today.
   - On the Today root, nothing intercepts.
   - The overlay is closed first.
   - Predictive back cancel leaves state intact.
8. **Reselect.** Depth > 1 pops to root without scrolling. At the root it scrolls to the top and expands the large title. A tab re-entering composition never scrolls.
9. **State lifetime.**
   - Switching tabs keeps scroll and ViewModels; a pop clears the page's ViewModel.
   - Shell disposal clears all three tab stores.
   - No `rememberNavBackStack`, `SavedStateHandle` or saved route stack (A8).
   - Route keys hold ids only.
10. **Module rules.**
    - `:feature:trips` does not depend on `:core:data`, `:app` or another feature.
    - `:core:sync` and `:core:plan` import no `android.*`.
    - `:core:navigation` has no Compose.
11. **Tokens.**
    - No raw colour or dp outside `@BoardMetrics("Trips")` metrics.
    - `MaterialTheme.colorScheme` is not used outside the design system.
    - Card values: border 2 `dawn` for active, 1 `border` otherwise; radius 26; padding 20; opacity 0.9 and no shadow for completed.
12. **Board versus iOS.** The divergences listed in OQ-6a-5 follow the defaults, and the capture review records the rest.
13. **Debug-only and seams.**
    - `DebugRefreshProbe` and `DebugShellStart` live in `src/debug`, and `ReleaseReadinessTest` names both.
    - `shellStart` and `refreshProbe` hang off the **existing** `LaunchOverrides` variant hook. A new `VariantHooks`, `ServiceLoader` or reflection mechanism is a defect.
    - Seams are 3b/4e's unchanged `RoutePlaceholder` (main code, text only in debug). `src/main` never names `DebugRouteLabel` or `app_debug_route_x`.
    - `RouteProbe` is not extended.
14. **Tests.** Cheapest tier. Exact tags. No sleeps. One emulator journey. Budgets respected.
15. **Commits.** One per task, Conventional, lower-case, with no `Co-Authored-By` and no other trailer.

## 10. Open questions (proceed on the default; T13 adds them to plan.md)

| ID | Question | Default |
|---|---|---|
| OQ-6a-1 | The plan says "the shell matches `Main`", but `B/Main.dc.html` is the Welcome board, and no Android shell board exists (`reqSystemChrome`). | Build per the Material mapping (plan l.418, OQ-A3/A4). Accept by `AppShellTest` and the captures of the Trips screen. Ask the designer for an Android tab-bar board (OQ-A42) |
| OQ-6a-2 | The plan's dependency list for 6a names only 4e and 2, but 6a needs 1a, 1c, 1d, 4c and 4d. | These are all earlier waves. Treat them as preconditions; stop if one is missing |
| OQ-6a-3 | How a feature ViewModel gets session services without seeing `AppGraph` in `:app` (A4 says "read the graph from `CreationExtras`"). | A feature-owned `TripsDependencies` interface and a `CreationExtras.Key`. `:app` puts the implementation into the extras when it calls `viewModel()`. If 5a already established a different shared convention in code, follow it and note it in the KDoc |
| OQ-6a-4 | Reselect on iOS (UIKit) pops to root **or**, at root, scrolls to the top. The plan says "pops it to the root and scrolls to the top". | System rule: depth > 1 pops (no scroll); at the root it scrolls to the top and expands the title |
| OQ-6a-5 | Card look where the board and the iOS app differ. | **Board:** Upcoming badge `Outline` (iOS `Secondary`); a check icon on the Completed badge; `softShadow` on non-completed cards; `MetaChip` around the flight number (iOS plain text); a dashed-line plane connector (iOS SF airplane); uppercase eyebrow (iOS screenshot not uppercased); next-row padding 10/12 (iOS 12). **iOS:** title style `screenBodyLg` with no extra weight, sizes from tokens. Record any other difference in the capture review |
| OQ-6a-6 | The board draws a 44 dp glass logo at the top left and a Fraunces large title. | iOS parity: no logo, and a system-font `CfLargeTopAppBar` (07 §2.2, user 2026-11-10) |
| OQ-6a-7 | `trip:<uuid>` seeds push onto the **Today** stack (iOS), while a card tap pushes onto My trips. | Keep iOS parity for seeds. Notification taps (12b) and the wizard save use the Trips stack (`fromRoot`, `wizardSaved`) |
| OQ-6a-8 | Capsule enter/exit timing: iOS uses `.animation(.default)` (system spring). | `tween(350)` slide plus fade; fade only under Reduce Motion, 350 ms |
| OQ-6a-9 | iOS 26 minimizes the tab bar on scroll down (`AppTabs.swift:101`, `tabBarMinimizeBehavior(.onScrollDown)`), but the Android plan says "never hidden on scroll". | The bar and rail are never hidden (plan 6a, Material guidance for `NavigationSuiteScaffold`). `AppShellTest.barNeverHidesOnScroll` guards it |
| OQ-6a-10 | iOS exposes the refresh counter as hidden `refresh.debug.count/outcome/presented` nodes, gated by the seed `CF_REFRESH_TEST_COUNTER=1` (`PlanRefreshFeedback.swift:64–85`). The Android seed table (plan "seeds and test hooks", 4e T11) has no such key. | An in-process `DebugRefreshProbe` object, always on in debug and absent in release. The `@Smoke` test reads it in process (AGENTS Tests 8). No hidden nodes, so the accessibility tree stays clean. Add the seed only if a later out-of-process (UiAutomator-only) test needs it |
| OQ-6a-11 | The 6b brief (Task 1) also adds `TripPlanSnapshot.trip` and `TripRepository.detailValues`. 6a needs them first, for the Next row. | 6a owns both and builds them in the 6b shape (§5.3). 6b's Task 1 finds them and only verifies them. The 6a T13 doc commit adds one line to the 6a "Results" saying so; it does not edit the 6b brief |
| OQ-6a-12 | The 6d brief also plans `core/ui/…/plan/CategorySwatch.kt` (`net.chronoflow.core.ui.plan`, signature `small: Boolean = false`), but 6a needs the swatch first, for the Next row. | 6a owns it at that path and package, with the iOS shape `size: CategorySwatchSize` (iOS `CategorySwatch.Size`; ported code keeps Swift names). 6d finds it and reuses it (its own brief says to use earlier names). T13 adds one line to the 6a "Results"; it does not edit the 6d brief |
| OQ-6a-13 | 1a assigns `Today.swift` to Phase 7, and the Phase 7 brief also ports `todayView`, `calendarDaysUntil` and the two `TodayTests` cases, but 6a needs `todayView(...).nextStep` for the Next row (`TripsScreen.swift:77–81`). | 6a ports the whole file and the two tests (§5.2, T2). Phase 7 finds `Today.kt` and extends it (`selectTodayTrip`, `lastEndedTrip`) instead of porting it again. T13 records this in the 6a "Results" |
| OQ-6a-14 | The plan's 6a "Tests" item puts predictive back on the emulator, while AGENTS "Tests" asks for the cheapest tier. | Predictive back start/progress/cancel is proven in Robolectric (`AppShellTest.predictiveBackCancelKeepsThePage`, `dispatchOnBackStarted`); the one emulator journey (T12) covers system back across tabs, reselect and pull to refresh. Add an emulator predictive-back gesture only if the Robolectric check turns out unable to fail for the real reason (AGENTS Tests 6) |
