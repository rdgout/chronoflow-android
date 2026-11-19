# Phase 6b brief: Trip page, data pipeline and timeline mode

- **Branch:** `feat/phase-6b-trip-page`, from the stack top once 6a and 1e are in the stack.
- **Stack check (2026-11-19, against e575992).** Single lane: 6a and 1e are in the stack, along with everything through 6a. I ran the §2 checks P1–P10 and every item exists. P10's glob misses with zsh; the icons are in `core/designsystem/…/icon/{LucideGlyphs,MaterialSymbols}.generated.kt`. `CfPullToRefreshBox` is in `core/designsystem/…/system/CfControls.kt:184`.
  Deltas from 6a and earlier phases:
  - 6a already owns `TripPlanSnapshot.trip` (`core/model/…/store/TripPlanSnapshot.kt`) and `TripRepository.detailValues` in the 6b shape. Task 1 verifies and reuses them; it must not redefine them. Since 6a review L2, `detailValues` returns checks and pending check-offs when the input hash is null.
  - The 6a shell keeps a Nav3 back stack per tab in the retained `ShellViewModel` (`app/…/shell/`). Tab entries get their ViewModels from per-tab stores, built from the current parent's factory and extras. The trip page renders through the shell's `page(route)` slot (`AppShellScaffold.kt:102`), which `AppShell` fills.
  - Root and tab entry content keys come from the secret-free `Route.stateKey()`, which already covers `TripPage`. Never add `notificationInputHash` or `notificationToken` to a key, a Bundle or a SavedStateHandle.
  - Shell back handlers fall through to the root finish (A8) when nothing is handled.
  - Emulator tests: the `ScreenTest` base hides the IME before launch and before close; `waitForImeToSettle` exists.
  - Repeated preflight: `TimelineInvalidationTest` already has five methods, including `scrollingWithinThresholdDoesNotRecomposeDayMenu`; retain all five and add the summary-clock case (six total). The capture matrix totals 16 PNGs, not 14.
  - `PlanDecoder.detail(snapshot)` returns `TripDetail?`; the non-null-plan branch checks its result. `PlanPreparer.evict(tripID)` evicts both preparation and decode caches. `PlanMetrics` are numeric generated values converted with `.dp`/`.sp` at the UI boundary.
  - Task 3 references types assigned to Tasks 4 and 5. Land the index and clock value/state declarations with Task 3 so its commit compiles; Tasks 4 and 5 complete their algorithms, UI and dedicated tests. No earlier-phase behaviour changes.
  - Assembly adjustments: the shared exported `RefreshFeedbackCapsule` needs the full `SyncResult?`, so the new source's `refresh` returns that result instead of discarding it (OQ-A266). Remove the legacy host in Task 13 after both its test and app/perf callers have moved; Tasks 11–12 introduce and verify the new page first, keeping each intermediate commit buildable (OQ-A267).
  - The generated header has 12 dp item top padding plus 8 dp vertical padding. Opening-position guards compare the outer `trip.headerItem.<id>` to the viewport; the inner `trip.dayHeader.<id>` remains the heading (OQ-A265). `DayTimeline` gains an optional headings slot so its existing unique `trip.day.<id>` root tag includes the lanes while preserving earlier primitive callers (OQ-A268).
  - OQ rows end at OQ-A247. Signed-out cold TTID is now 125–131 ms (OQ-A247). 6b is a timeline change, so the orchestrator runs `make perf-timeline` and `make baseline-profile`.
- **Depends on:** 6a (tab shell, My trips, `CfPullToRefreshBox`, the `TripPage` placeholder) and 1e (benchmarks, budgets, baseline profile). Through them: 0a–0e, 1a–1d, 2, 4a–4e. **Wave:** 9. **Milestone:** M3.
- **Plan entry:** `plan.md` "### Phase 6b: Trip page: data pipeline and timeline mode" (l.1898–1941). This brief expands that entry. Where they differ, `plan.md` wins; report the difference.
- **Boards:** `Plan`, `PlanDark`, `PlanScrolled`, `PlanScrolledDark`, `PlanTimelineTop`, `PlanDayMenu`, `PlanEnd`, `PlanEndNoText` (all in `_design/canvas/`; the last seven are wrappers that pass props to `Plan.dc.html`).
- **API endpoints:** no new client code, and no detail fetch in 6b. The page reads the local store. The on-open detail fetch (`SessionSync.fetchDetail(tripId, session, epoch)` → `GET /v1/trips/{trip}` → `ApiTripDetail {trip, plan, notices, timeline, checkState, detailVersion}`), the fetch states, `awaitingSync` and `syncCompleted` are **6c** (6c Tasks 1 and 3 port `TripPageModel.retry` as `TripPageFetchModel`; OQ-6b-15). Pull to refresh goes through `session.sync.refreshNow()` (4d `SessionSync`; the full pull). `PUT /v1/trips/{trip}/checks` is 6d.
- **Tests rules:** AGENTS.md "Tests" applies to every test you write: tiers `@Logic` / `@Layout` / `@Smoke`, JVM < 1 s, Robolectric < 5 s, emulator < 10 s, `runTest(timeout = 60.seconds)`, no `Thread.sleep`, no JUnit `Timeout` rule. Read it before Task 1.

- **Checked:** 2026-11-16 by an adversarial checker against iOS `f391a1fa`, the boards, web `e36368ec`, plan.md and the 4d/4e/6c briefs. Fixed: `SessionGraph`/`SessionSync` member names (4e has no `syncEngine`/`repositories`), the fetch/`awaitingSync`/`syncCompleted` overlap with 6c (moved to 6c, OQ-6b-15), `noPlan` source (decoded `plan.status`, not the card), eviction rule (`snapshot.plan == null`), preferences parsing (`TimeFormat.of`/`BlockType.of` keep `Unknown`; real `BlockType` raws in tests), the `select` unknown-day guard, the SavedStateHandle wording vs A8, fixture S1 (AMS→HND) instead of S3 (AMS→SYD) for the top bar and board tests, iOS paths (`PlanUI/Models/TripPlanModel.swift`, `PlanUI/TripPage/PlanAppearance.swift`) and line references.

Tags used below:
- **[F]** is a fact checked in source (path and line given).
- **[R]** is a decision of this brief or the plan.
- **[verify]** marks a name or library detail you must confirm on the first build. When it is different, use what you find, keep the behaviour, and note it in the PR body.

Paths are shortened as follows:
- `feature/trip/…/X.kt` = `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/X.kt`.
- `core/plan/…` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/…`.
- iOS paths are relative to `chronoflow-ios` at `f391a1fa`. `PlanUI/…` = `Packages/ChronoFlowKit/Sources/PlanUI/…`.

---

## 1. Goal

Replace the Phase 1d host (`TimelinePage` + `TimelineHostViewModel` + `TimelineSource`) with the real trip page in timeline mode, fed by the account database.

When this phase is done:
1. `TripPageViewModel` (port of iOS `TripPageModel` + `TripPlanModel`) combines the trip, plan, marks and settings flows, decodes and prepares off the main thread behind a revision guard, holds a `PlanLease` while it lives, and keeps the anchor `(dayId, offsetInDay)` in `SavedStateHandle`.
2. The page draws the top bar, summary card, day pills, designed day headers, the compact header line, lane headings, the 1d timeline with its `NowLine`, the end card and the hidden-steps notice. The day menu and the pills jump between days.
3. It opens on `openingDay`, or at the summary under the `CF_PLAN_TEST_POSITION=summary` seed.
4. `TripPage(tripId)` in the app opens this page instead of 6a's placeholder.
5. The perf entry `cf.trip-page=<fixture>` opens the same page from fixtures. `make perf-timeline` passes every emulator gate of plan.md "Where gates apply"; its rows (Page = `trip`) start the baseline that 6c–6e are compared with. The baseline profile is regenerated.

---

## 2. Before you start: check what earlier phases left

Run these checks first. If an item is missing, **stop and report it to the orchestrator**. Do not rebuild an earlier phase.

| # | Must exist | From | How to check |
|---|---|---|---|
| P1 | `TimelinePage`, `TimelineHostViewModel`, `TimelineSource`, `TimelineListIndex`, `FirstTimelineFrameTrace`, `DayTimeline`, `NowLine`, `TimelineTargets`, `TimelineDrawingRepository`, `DayClocks`, `TimelineProbe` (+ `LocalTimelineProbe`) in `:feature:trip` | 1d | `grep -rn "class TimelineHostViewModel\|fun TimelinePage\|enum class Kind" feature/trip/src/main` |
| P2 | `TimelineTestHost`, `TimelineInvalidationTest` (5 methods), `CountingTimelineProbe`, `TimelinePageTest`, `TimelinePageGuardTest`, `TimelinePerfMarkersTest` | 1d, 1e | `grep -rln "TimelineTestHost" feature/trip/src/test` |
| P3 | Perf entry `app/src/perf/kotlin/net/chronoflow/app/perf/{FixtureTimelineSource,TripPageEntry}.kt`, the release stub, `PerfClockTest`, `FixtureTimelineSourceTest` | 1d | `ls app/src/perf/kotlin/net/chronoflow/app/perf/` |
| P4 | `TimelineBenchmark` (`timelineFirstFrame`, `timelineScroll`, `timelineLastDay`), `make perf-timeline`, `make baseline-profile`, `docs/perf-timeline.md` with a "Phase 1e" result row | 1e | `grep -n "Phase 1e" docs/perf-timeline.md` |
| P5 | `PlanPreparer`, `PreparedPlan`, `PreparedDay`, `TripPageState`, `TripPageFetch`, `TripPageAnchor`, `openingDay`, `replacementAnchor`, `TripPageLayout`, `LargeTimelineFixtures` | 1b | `grep -rn "fun openingDay\|fun replacementAnchor" core/plan` |
| P6 | `PlanDecoder.detail(snapshot)`, `TripPlanSnapshot(plan, checks, pendingChecks)`, `PlanVersionRecord`, `CheckMarkRecord`, `CheckOffChange` | 1a | `grep -rn "data class TripPlanSnapshot" core/model` |
| P7 | `TripRecord` (`net.chronoflow.core.model.store`), `TripRepository` (`planValues`, `checkValues`), `SettingsRepository`, `OutboxRepository`, `SessionSync` (`refreshNow(): SyncResult?`, `retention`), `PlanRetention.hold → PlanLease.release()` (idempotent) | 4d | `grep -rn "fun hold\|fun refreshNow" core/sync core/data` |
| P8 | `AppGraph.planClock`, `AppGraphKey`, `SessionGraph(epoch: Long, snapshot, database, accounts, trips, settings, outbox, sync: SessionSync, dataSource, scope)` (4e §5.6; no `SyncEngine` of its own), `Route.TripPage(...)`, `TripSheet`, `TestSeed` (`planRoute`, `planPosition` or similar, `planSheet`), `DebugSeedRouting.current` | 1d, 4e | `grep -rn "CF_PLAN_TEST_POSITION" app/src` |
| P9 | The 6a shell: `NavigationSuiteScaffold`, one Nav3 back stack per tab, a per-entry `ViewModelStore` (`rememberViewModelStoreNavEntryDecorator` or equivalent), the `TripPage` placeholder, `CfPullToRefreshBox` and the refresh capsule (`refresh.feedback`) | 6a | `grep -rn "TripPage" app/src/main` |
| P10 | `CfButton`, `CfIconButton`, `CfBackButton`, `LucideGlyphs` (incl. `ChevronLeft` from 5a, `ChevronRight`, `ChevronDown`, `Check`, `Info`, `Moon`, `TriangleAlert`), `LucideGlyph.imageVector()`, `MaterialSymbols.MoreVert`, `CfTheme.type.*` incl. `plan*`, `eyebrow`, `welcomeWordmark`, `authStrong`, `authMeta`, `authBody`, `settingsBadge` | 0b, 2, 5a | `grep -n "MoreVert\|ChevronLeft" core/designsystem/src/main/kotlin/**/icon/*.kt` |

Write down the real names you find (package of `TimelinePage`, the `TestSeed` field for the position seed, the 6a placeholder's file, the refresh capsule composable). Use them as they are. The paths below assume the plan defaults.

---

## 3. Inputs to read

**This repo (read in this order)**
1. `AGENTS.md`: all of it. Especially "Working rules", "Performance", "Tests" 1–9, "Commands", "Git" (no commit trailers), "Traps".
2. `plan.md`:
   - Phase 6b (l.1898–1941), 6c and 6d (to know what is **not** yours);
   - "Performance budget", A8 (configuration changes in place), A10 (window policy, font cap 1.3, 48 dp targets), A12 (timeline design B);
   - "Android spec: window policy" (trip page: phone layout below 600 dp);
   - OQ-A11 (`more_vert`).
3. `docs/research/04-timeline.md`:
   - §3.1–§3.6 (page, top bar, summary, pills, day header, compact header, lane headings), §3.10 (now line), §3.15 (end card), §3.20 (board vs iOS and OQ-T defaults);
   - §4.1–§4.3 (opening and replacement anchors, current day, jumps);
   - §9.3 (pipeline), §9.5 (page composition), §9.8 (where scroll values may be read), §9.10 (menu);
   - row J of the decision table (l.1315: "LazyColumn `stickyHeader` for day headers: Rejected");
   - Appendix A (copy keys).
4. Earlier briefs, for names you reuse: `docs/phases/phase-1d-timeline-host.md` (Tasks 3, 5–8), `phase-1e-timeline-perf.md` (§2, Tasks 1, 4–6), `phase-1b-timeline-layout.md` (PreparedPlan, PlanPreparer, TripPageState), `phase-1a-plan-core.md` (TripPlanSnapshot, OQ-1a-5), `phase-4d-sync.md` (§5.2, Task 6, PlanRetention), `phase-4e-session.md` (T11 seeds, §5.5–§5.8), `phase-2-design-system.md` (CfButton, CfCard, icons), `phase-0e-test-harness.md` (T9 captures, guards).

**Boards (`_design/canvas/`, read-only)**
- `Plan.dc.html` (the base; every wrapper below passes props to it). Look at:
  - the summary `<section aria-label="Plan summary">`;
  - the day strip, the day header (`headerStyle`), the compact line (`compactStyle`), the lanes row;
  - the now line (`nowStyle`), the end card `<section aria-labelledby="end-title">`;
  - the menu `<div role="menu">`;
  - the `dayData`/`menuDays` script at the end of the file.
- Wrappers and their props:
  - `PlanDark`: `theme=dark`.
  - `PlanScrolled` / `PlanScrolledDark`: `scrolled=true`, `scroll-hour=9`.
  - `PlanTimelineTop`: `from-top=true`, `open-day=0`, height 2600.
  - `PlanDayMenu`: scrolled, `scroll-hour=9`, `day-menu=true`.
  - `PlanEnd`: `view=list`, `day=6`, height 1500.
  - `PlanEndNoText`: as `PlanEnd`, plus `end-text=false`.

**iOS (`chronoflow-ios` at `f391a1fa`, read-only)**

| File | What to port |
|---|---|
| `PlanUI/TripPage/TripPageModel.swift` (190 lines) | State fields, `start` (lease, settings observation l.90–106), `acceptPreparedPlan` (l.140–151), `select` (l.159–164), `stop`. `retry`/`online`/`fetch` are 6c's. |
| `PlanUI/Models/TripPlanModel.swift` (218 lines; **not** under `TripPage/`) | Observation and eviction (l.77–122), decode, `rebuildLayouts` with the `layoutRevision` guard (l.180–211), clock task |
| `PlanUI/TripPage/TripPageView.swift` (562 lines) | `toolbar` (l.224–259), `titleItem` (l.297–328), `dayMenuLabel` and `subtitle` (l.334–349), compact header (l.351–382), `jump` (l.394–398), `summary` (l.405–436), page body (l.119–159), `onAppear` and `onChange(prepared)` (l.171–220), current day (l.187–193) |
| `PlanUI/TripPage/DayHeader.swift` | Whole file (quoted in Appendix B) |
| `PlanUI/TripPage/TripPageComponents.swift` | `DualClocks`, `EndOfPlanCard` (l.45–70), `DayNavigation` (l.72–116) |
| `PlanUI/TripPage/TripDayRow.swift` | Row = header + `LaneHeadings` + timeline, tag `trip.day.<id>` |
| `PlanUI/TripPage/DayTimelineView.swift` | `LaneHeadings` |
| `PlanUI/TripPage/TripScrollTracker.swift:126–134` | Compact header opacity (Appendix B) |
| `PlanUI/TripPage/TripHeaderText.swift`, `PageCopy.swift` | Already ported in 1b; read for names |
| `PlanUI/TripPage/PlanAppearance.swift` | `PlanCard` (l.59–68): padding 16, radius 26, 1 dp border, no shadow |
| `LocalStore/TripRepository+PlanObservation.swift` | `detailValues(tripID)` |
| `App/TripPageDestination.swift` | Route, retention, `fetchDetail`, freeze-clock seed, sync events, `onSettings`, `onActions` |
| iOS `plan.md:1692–1763` | 15c, 15d, 15 fix round 1, 15 integration fixes |

**iOS screenshots:** `docs/research/screens/ios/04-trip-timeline-top.png`, `06-trip-summary.png`.

---

## 4. Out of scope (do not build)

| Item | Owner |
|---|---|
| The detail fetch on open (`retry`, `TripPageFetch` states, `online`, `reconnectPending`), `awaitingSync` on the route, `syncCompleted` from `SessionSync.events()` (6c Tasks 1 and 3) | 6c |
| List mode, the view toggle (`trip.viewToggle`), `meta` line, every non-ready state (`PlanLoading`, remote summary, offline, not found, no plan, server error), `TripPagePathMonitor` (online/offline input) | 6c |
| Step sheet and block selection UI, legend sheet, trip actions sheet, a11y custom actions on blocks, keyboard navigation, focus after a jump | 6d |
| Wide layout (≥ 600 dp: side list, `TripWideListPosition`) | 6e |
| Notification opener (`notificationInputHash`, `notificationToken`), first-trip reminders offer | later phases |
| Check-off writes | 6d |
| Any screen without a board (a hidden-steps board, a "refused change" sheet) | not built; the hidden-steps button is built from iOS only, see Task 10 |

Below 600 dp the page is the phone layout. At ≥ 600 dp, 6b still renders the phone layout full width until 6e [R].

---

## 5. Rules fixed by this brief [R]

1. **Conflict order:** a user decision, then iOS at the pin, then boards, then web, then RN.
   - Where iOS sets explicit metrics, iOS wins, and the board delta goes into Appendix C.
   - Where iOS uses a system-styled control (default SwiftUI `Button`, `Label`, `Menu`), the look comes from the board, or else from the M3 mapping.
2. **Day headers are list items, not sticky headers.**
   - Research 04 row J rejects `stickyHeader`. iOS fades a separate compact line (`04 §3.5`, `TripScrollTracker.swift:126–134`).
   - The designed `DayHeader` scrolls away with the content. The **compact header** (lane headings of the current day, opaque `card`) is an overlay that fades in over the last 24 dp of the header's travel. Together they are what plan.md calls the "designed sticky DayHeader". See OQ-6b-1.
3. **Values come only from tokens:** `CfColors`, `CfTheme.type`, `PlanMetrics`, or `TripPageMetrics` (new, Task 5; its KDoc names the boards `Plan` and `PlanDayMenu`, which satisfies the Konsist rule). The 48 dp minimum target comes from `Modifier.minimumInteractiveComponentSize()`, never from a literal.
4. **Copy:**
   - `Text` gets only string resources, or prepared strings from `PreparedPlan`/`PreparedDay` (data).
   - Find each iOS key's resource by its `<!-- plan: KEY -->` comment in `core/ui/src/main/res/values/strings_ios.xml` (Appendix A lists every key with en and nl). Plurals use `pluralStringResource`.
   - A missing key means 0c is incomplete: stop and report it. Do not add the string by hand.
5. **Scroll values are read only where `04 §9.8` allows:**
   - in `graphicsLayer { }` / draw lambdas;
   - in `derivedStateOf` that flips only at boundaries;
   - in effects.
   No item composable reads `listState`. No `onSizeChanged` feeds state.
6. **Off main:** decode, layout, prepare, clock projection and summary clock texts run on the compute dispatcher. Drawings build on `Dispatchers.Default.limitedParallelism(2)`, created once per ViewModel.
7. Rethrow `CancellationException` everywhere you catch.
8. **Test tags kept from 1d/1e:** `trip.page`, `trip.scroll`, `trip.timeline.preparing|ready`, `trip.timeline.presented`, `trip.day.<dayId>`, `trip.block.<id>`, `trip.dayMenu`, `trip.dayMenu.day.<n>` (0-based), `trip.plan.end`. 1e's benchmarks depend on them.
9. **Reduce Motion:** read it from the existing 0b/2 composition local (`LocalReduceMotion` [verify]). It switches every animated jump and pill scroll to an instant one, and the compact header to a 0/1 alpha.

---

## 6. Tasks

Each task is one commit, in this order. Tick the named plan.md checkbox in the same commit when the last task that serves it lands (named per task).

### Task 1: Observe the trip plan snapshot

**Commit:** `feat(data): observe trip plan snapshots`. **Ticks:** none.

**Files**
- `core/model/…/store/TripPlanSnapshot.kt`: add `val trip: TripRecord? = null` as the **first** field (OQ-1a-5). Keep the defaults so existing callers compile.
- `core/data/…/repository/TripRepository.kt`: add `fun detailValues(tripId: String): Flow<TripPlanSnapshot>`, a port of `TripRepository+PlanObservation.swift`:
  ```kotlin
  fun detailValues(tripId: String): Flow<TripPlanSnapshot> =
      tripValue(tripId).flatMapLatest { trip ->
          val hash = trip?.currentInputHash
          if (trip == null || hash == null) flowOf(TripPlanSnapshot(trip, plan = null))
          else combine(planValues(tripId, hash), checkValues(tripId, hash), pendingCheckOffs(tripId)) { p, c, o ->
              TripPlanSnapshot(trip, p, c, o)
          }
      }.distinctUntilChanged()
  ```
  - `tripValue(id)`: a new named query `tripById` in the 4a `.sq` file, observed with SQLDelight `asFlow().mapToOneOrNull(io)`. If 4d already has an observed single-trip query, reuse it [verify].
  - `checkValues` ordered by `marked_at` [F iOS].
  - `pendingCheckOffs(tripId)`: `OutboxRepository.values()` (or `outboxValues()`) filtered to `OutboxChange.CheckOff` whose `value.tripId == tripId`, in outbox order.
  - All flows run on the repository's IO dispatcher. The `flatMapLatest` needs `@OptIn(ExperimentalCoroutinesApi::class)` at the function only.
- `core/plan/…/trippage/PlanPreferences.kt` (pure, JVM):
  ```kotlin
  data class PlanPreferences(val timeFormat: TimeFormat = TimeFormat.Value24, val hiddenCategories: List<BlockType> = emptyList())
  fun planPreferences(section: JsonObject?): PlanPreferences
  ```
  - `section` is the stored settings section `preferences` with camelCase keys [F 4d Task 5: `time_format` → `timeFormat`].
  - Port of `TripPageModel.swift:93–103` [F]:
  - `timeFormat`: when the value is a JSON **string**, `TimeFormat.of(it)` (`"12"` → `Value12`, `"24"` → `Value24`, anything else → `Unknown(raw)`, as Swift `TimeFormat(rawValue:)`); a non-string or missing value → `Value24` (the iOS default).
  - `hiddenCategories`: a JSON array; each **string** element mapped through `BlockType.of(raw)` (non-strings dropped, as Swift `compactMap { $0.string }`). Unknown raws stay as `BlockType.Unknown(raw)` and order and duplicates are kept (iOS does not filter). A non-array → empty. The wire values are `BlockType` raws such as `caffeine_serving`, `seek_light` (web `PreferenceRules.php:19–20`), not lane names.
  - `TimeFormat`, `BlockType` are the 0d contracts in `:core:model` (1a §2: `X.of(raw)`, `Unknown(raw)`); `:core:plan` sees them.
- `core/data/…/repository/SettingsRepository.kt`: add `fun planPreferences(): Flow<PlanPreferences>` = the observed `preferences` section mapped through `planPreferences`, then `distinctUntilChanged()`.

**Edge cases**
- `currentInputHash` changes (new plan): `flatMapLatest` drops the old plan's flows.
- The trip row is deleted: one emission with `trip = null`.
- A tombstoned trip (`deletedAt != null` or `deletedLocally`) is still emitted. The ViewModel decides.

**Tests**
- `PlanPreferencesTest` (JVM, `core/plan/src/test`):
  - `null` → defaults;
  - `{"timeFormat":"12"}` → `Value12`;
  - `{"timeFormat":"24"}` and `{"timeFormat":12}` (a number) → `Value24`; `{"timeFormat":"x"}` → `Unknown("x")`;
  - `{"hiddenCategories":["caffeine_serving",3,"seek_light","bogus"]}` → `[CaffeineServing, SeekLight, Unknown("bogus")]`;
  - `{"hiddenCategories":"caffeine_serving"}` → empty.
- `TripPlanObservationTest` (JVM on JDBC, `core/data/src/test`, the 4d DB test setup). Each case collects with Turbine or `take(n).toList()`:
  1. Unknown trip → one snapshot `(null, null, [], [])`.
  2. Trip without `currentInputHash` → `(trip, null)`.
  3. Trip + plan for its hash + 2 marks inserted out of order → marks ordered by `marked_at`.
  4. A plan stored for **another** hash is ignored.
  5. Updating `currentInputHash` to a hash with a stored plan emits that plan.
  6. Enqueuing a check-off for this trip adds it to `pendingChecks`; one for another trip does not.
  7. Writing an unrelated trip emits nothing new (`distinctUntilChanged`).

### Task 2: The trip page source seam

**Commit:** `feat(trip): add the trip page source seam`. **Ticks:** none.

**Files**
- `feature/trip/…/page/TripPageSource.kt`:
  ```kotlin
  interface TripPageSource {
      fun snapshots(tripId: String): Flow<TripPlanSnapshot>
      fun preferences(): Flow<PlanPreferences>
      fun hold(tripId: String): PlanLease
      suspend fun refresh()
  }
  ```
  6c adds `fetchDetail` and the sync-completion input to this seam (6c Task 3); do not add them here.
- `app/src/main/kotlin/net/chronoflow/app/trip/DbTripPageSource.kt` [R: it binds session members, so it lives in `:app`]:
  ```kotlin
  class DbTripPageSource(session: SessionGraph) : TripPageSource
  ```
  - `snapshots` = `session.trips.detailValues`.
  - `preferences` = `session.settings.planPreferences()`.
  - `hold` = `session.sync.retention.hold` [F iOS `TripPageDestination.swift:81`: `retention: sync.retention`].
  - `refresh` = `session.sync.refreshNow()` (the result is ignored here; 6a's capsule shows it, see Task 11). If 6a's My trips refresh calls a different entry point (an `AppGraph`-level `refreshNow`, iOS `native.refreshNow()`), use that one [verify 6a].
- `feature/trip/src/test/kotlin/net/chronoflow/feature/trip/page/FakeTripPageSource.kt` (test helper):
  - `MutableStateFlow<TripPlanSnapshot>` and `MutableStateFlow<PlanPreferences>`;
  - counters `holds`, `releases`, `refreshes`.
  - `companion fun fixture(name: String)` builds a snapshot from `LargeTimelineFixtures.detailBytes(name)`. See the fixture snapshot rule below.

**Fixture snapshot rule** [R], shared by the test fake and the perf source (Task 13):
- `TripRecord(id = "fixture-$name", keepOffline = true, currentInputHash = "fixture-$name", …)`: other fields null, defaults, or `departureUtc` from the fixture, whichever the constructor requires.
- `PlanVersionRecord(tripId = "fixture-$name", inputHash = "fixture-$name", detail = bytes, receivedAt = "2026-01-01T00:00:00Z")`.
- `checks = []`, `pendingChecks = []`.
- Do not parse the bytes to fill these fields. [verify] that `PlanDecoder.detail` does not compare the record's ids with the decoded `trip.id`/`inputHash`. If it does, read both values with one `Json.decodeFromString<IdsOnly>` on the worker and note it.

**Tests:** none of its own (the seam is exercised by Tasks 3, 11 and 13; the fetch mapping is 6c's `TripPageFetchTests.notFoundAndServerMapping`).

### Task 3: TripPageViewModel

**Commit:** `feat(trip): port the trip page model`. **Ticks:** "TripPageViewModel".

**Files** (`feature/trip/…/page/`)
- `TripPageArgs.kt`: `data class TripPageArgs(val tripId: String, val dayId: String? = null, val blockId: String? = null, val startAtSummary: Boolean = false)`. (6c adds `awaitingSync`.)
- `TripPlanPipeline.kt`. This is the seam that makes the revision guard testable:
  ```kotlin
  data class PipelineInput(val snapshot: TripPlanSnapshot, val preferences: PlanPreferences, val language: Language)
  class ReadyPlan(val detail: TripDetail, val prepared: PreparedPlan, val layouts: Map<String, DayLayout>,
                  val index: TripListIndex, val timeFormat: TimeFormat, val language: Language, val hasHiddenSteps: Boolean)
  data class PipelineOutput(val trip: TripRecord?, val title: TripTitleParts?, val noPlan: Boolean, val plan: ReadyPlan?)
  interface TripPlanPipeline { suspend fun build(input: PipelineInput, previous: ReadyPlan?): PipelineOutput }
  class DefaultTripPlanPipeline(decoder: PlanDecoder, preparer: PlanPreparer) : TripPlanPipeline
  ```
  `DefaultTripPlanPipeline.build` is a port of `TripPlanModel.swift:77–122` and `rebuildLayouts` (l.180–211):
  - If `trip == null`, `trip.deletedAt != null`, `trip.deletedLocally`, or `snapshot.plan == null`: call the decoder/preparer eviction for `tripId` (iOS `decoder.evict(tripID:)`, l.99; use the 1a/1b name) and return `plan = null`, `noPlan = false`.
  - Otherwise `detail = decoder.detail(snapshot)`.
  - `noPlan = detail.plan.status == PlanStatus.NoPlan` (wire `"no_plan"`) [F `TripPageModel.swift:71`: `plan.detail?.plan.status == .noPlan`; it comes from the decoded detail, not from the card]. A no-plan detail is still prepared, as iOS does.
  - `hidden = preferences.hiddenCategories`; `layouts = preparer.layouts(detail, hidden)`; `prepared = preparer.prepare(detail, hidden, language, timeFormat, marks = snapshot.checks mapped to CheckMark, pendingChecks = snapshot.pendingChecks)`.
  - `index = TripListIndex(prepared.days.map { it.id }, hasHidden = hidden.isNotEmpty())` (Task 4).
  - `title = TripTitleParts` from `detail.trip` (from/to IATA, city), else from the decoded `trip.card` (4b card type [verify]), else `null`.
  - A decode or prepare failure (not cancellation) logs with `Log.w("CfTrip", …)` and returns `previous` as `plan` (keep the last good plan) [R].
- `TripPageViewModel.kt`:
  ```kotlin
  class TripPageViewModel(
      private val args: TripPageArgs,
      private val source: TripPageSource,
      private val clock: PlanClock,
      private val pipeline: TripPlanPipeline,
      private val savedState: SavedStateHandle,
      language: Language,
      private val compute: CoroutineDispatcher,          // Dispatchers.Default in production
      buildDispatcher: CoroutineDispatcher,              // Dispatchers.Default.limitedParallelism(2), created once by the factory
      metrics: suspend () -> TextMetrics,
      cache: DayDrawingCache = DayDrawingCache(),
      private val formatter: suspend () -> CopyLineFormatter, // 1a/1b formatter, created on compute
  ) : ViewModel()
  ```
  **`init`, in this order:**
  1. `Trace.beginAsyncSection("cf.timeline.firstFrame", 0)` is the **first** statement (1d/1e contract).
  2. `lease = source.hold(args.tripId)`.
  3. Read the saved anchor (keys below).
  4. Launch the pipeline. (6c adds its fetch model's `start()` after this, iOS `start()` order.)

  The pipeline keeps 1e's first-frame breakdown (1e Task 1): the async sections `cf.timeline.decode`, `cf.timeline.prepare` and `cf.timeline.typeface` (cookies 1–3, first input only) wrap the decode, the layout and prepare, and the first metrics load, so `timelineFirstFrame` keeps reporting `timelineDecodeMs`, `timelinePrepareMs` and `timelineTypefaceMs`.

  **State:**
  ```kotlin
  data class TripPageUi(val state: TripPageState, val title: TripTitleParts?, val plan: ReadyPlan?)
  val ui: StateFlow<TripPageUi>                 // initial: TripPageUi(Loading, null, null)
  val anchor: StateFlow<TripPageAnchor>
  val selectedStepId: StateFlow<String?>        // set by selectBlock; the sheet is 6d
  val scrollCommands: Flow<ScrollCommand>       // Channel(BUFFERED).receiveAsFlow()
  val clocks: DayClocks                         // 1d
  val summaryClock: SummaryClockState           // Task 5
  val repository: TimelineDrawingRepository     // 1d, built with viewModelScope, buildDispatcher, metrics, cache
  fun setLanguage(language: Language)
  ```

  **Pipeline** (`04 §9.3`):
  ```
  combine(source.snapshots(id), source.preferences(), languageFlow) { s, p, l -> PipelineInput(s, p, l) }
    .onEach { seq = inputSeq.incrementAndGet() }  // seq travels with the input
    .mapLatest { (input, seq) -> withContext(compute) { pipeline.build(input, lastPlan) } to seq }
    .filter { (_, seq) -> seq == inputSeq.get() } // revision guard: a late result never lands
    .collect { (out, _) -> apply(out) }          // on Main
  ```
  - `mapLatest` cancels stale work. The `seq` check also drops a result whose block finished without reaching a cancellation point (iOS `layoutRevision` guard, `TripPlanModel.swift`).
  - `apply(out)`:
    - set `hasLoaded = true`;
    - store `out.plan`;
    - on each new `plan`, call `clocks.retain(dayIds)`;
    - call `acceptPreparedPlan(old, new)`;
    - call `recompute()`.

  **State resolution, `recompute()`** (port of `TripPageModel.state`, l.68–75, with 6b's fixed inputs):
  ```kotlin
  TripPageState.resolve(keepOffline = trip?.keepOffline ?: true, stored = plan != null, noPlan = out.noPlan,
      online = true, fetch = TripPageFetch.Idle, missingAfterSync = false,
      tombstoned = trip?.deletedAt != null || trip?.deletedLocally == true, awaitingSync = false)
  ```
  - 6c replaces `online`, `fetch`, `missingAfterSync` and `awaitingSync` with its `TripPageFetchModel`, `syncCompleted` and route inputs (6c Task 3). Keep `recompute()` a single private function so 6c changes one call.

  **Lease:** `override fun onCleared() { lease.release(); super.onCleared() }`. `release()` is idempotent (4d) [F OQ-4d-8].

  **Anchor** (port of `acceptPreparedPlan` and `select`):
  - First plan (`old == null`):
    - `day = openingDay(days, blocks, requestedDay = args.dayId, blockID = args.blockId, keptDay = saved?.dayId, now = clock.values().first())`;
    - `anchor = TripPageAnchor(day.id, day.labelDate, if (day.id == saved?.dayId) saved.offset else 0.0)`.
    - No scroll command: the UI creates its `LazyListState` from `openingScroll()` (Task 11).
  - Later plans: `next = replacementAnchor(anchor.value, days)`. Send `ScrollCommand(next.dayID, next.offset, animated = false)` **only** when the day-id list changed (`old.days.map { id } != new.days.map { id }`) or `anchor.dayID` is no longer in the plan [R; on Android, list keys keep the position otherwise]. Then set `anchor = next`.
  - `fun select(dayId: String, offsetDp: Double)`:
    - store `offsetDp` in a plain field (not observed);
    - change `anchor` only when `dayId != anchor.value.dayID` **and** the day is in the current plan (else return) [F `TripPageModel.swift:159–164`];
    - write the saved state on a day change.
  - `fun saveAnchor()`: writes `trip.anchor.day`, `trip.anchor.labelDate`, `trip.anchor.offset` (Double, dp) into `savedState`. The UI calls it when a scroll stops.
  - `fun jump(dayId: String)`: `select(dayId, 0.0)`, then sends `ScrollCommand(dayId, 0.0, animated = true)`.
  - `fun openingScroll(): TripPageAnchor?` = the current anchor (null before the first plan).
  - `fun selectBlock(id: String)` sets `selectedStepId`. It is called from `onBlock`.

  **Clock** (`suspend fun runClock()`, called under `repeatOnLifecycle(STARTED)` as in 1d):
  - `combine(planFlow.filterNotNull(), clock.values())`.
  - On `compute`: `projectDayClocks(layouts, now, formatter, timeFormat)` plus `dualClockValues(...)` (Task 5).
  - Then on Main: `clocks.apply(v)` and `summaryClock.apply(values)`.
- `TripTitleParts.kt` (pure): `data class TripTitleParts(val fromIata: String, val toIata: String, val fromName: String, val toName: String)`, where `name = city ?: iata` [F `TripPageView.swift` `dayMenuLabel`].

**SavedStateHandle rule** [R]: the anchor is written only to `SavedStateHandle`, never to disk or the database. plan.md 6b: "for configuration changes only". Under A8, size and orientation changes are handled in place (same ViewModel), and only `locale`, `fontScale` and `layoutDirection` recreate the activity; those are what the saved anchor restores. Process death is always a cold launch that ignores the saved stack (A8), so it never restores a trip page. A newly opened page (new ViewModel, no saved state) always uses `openingDay`.

**Edge cases**
- A plan with zero days: `openingDay` returns null, the anchor is `(null, null, 0)`, and there are no scroll commands.
- `args.dayId` not in the plan: the order falls through to the next rule [F 1b].
- The language changes: a new prepare runs (same day ids), so there is no scroll command.
- A check-off arrives (new marks, same days): one prepare, and no scroll command.
- A pipeline result that lands after `onCleared`: the scope is cancelled, so no state write happens.
- `select` with a day id not in the plan: nothing changes (iOS guard).

**Tests** (`TripPageViewModelTest`, JVM, `feature/trip/src/test`; `StandardTestDispatcher` for `compute` and Main via `Dispatchers.setMain`; `FakeTripPageSource`; `FakePlanClock`; a gated fake `TripPlanPipeline` unless stated; each < 1 s):

| Test | Asserts |
|---|---|
| `staleResultIsDroppedByTheRevisionGuard` | Emit input A (gate held), then input B. Release B, then A. `ui.value.plan` is B's, A is never applied (count `apply` calls via the fake's outputs = 1), and `pipeline.builds == 2`. |
| `decodeFailureKeepsTheLastPlan` | The real `DefaultTripPlanPipeline` with S3. Then a snapshot with corrupt bytes → `plan` is still S3's instance. |
| `leaseIsHeldWhileAliveAndReleasedOnClear` | After creation `holds == 1`, `releases == 0`. Create it through `ViewModelProvider(store, factory)[…]`, then `store.clear()` → `releases == 1`. A second `clear()` keeps 1. |
| `openingAnchorFollowsOpeningDay` | Real pipeline, S3, clock at day 2 start + 1 h → `anchor.dayID == day2.id`, offset 0, no scroll command. |
| `requestedDayWins` | `args.dayId = day4` → anchor day 4. |
| `savedAnchorRestores` | `SavedStateHandle(mapOf("trip.anchor.day" to day3, "trip.anchor.labelDate" to day3.labelDate, "trip.anchor.offset" to 180.0))` → anchor `(day3, …, 180.0)`. |
| `selectChangesAnchorOnlyOnDayChange` | `select(day2, 10.0)` then `select(day2, 50.0)` → one `anchor` emission. `saveAnchor()` writes offset 50.0. |
| `replacementScrollsOnlyWhenDaysChange` | A new snapshot with a mark (same days) → no command. A snapshot whose plan renames the anchor day id (same labelDate) → one `ScrollCommand(newId, offset kept, animated = false)`. |
| `jumpSelectsAndScrolls` | `jump(day5)` → `anchor.dayID == day5`, command `(day5, 0.0, animated = true)`. |
| `stateReadyWhenStored` | S3 snapshot → `Ready`. |
| `noPlanStatusResolvesNoPlan` | Real pipeline, a stored detail whose `plan.status` is `no_plan` → `NoPlan`. |
| `missingPlanEvicts` | Snapshot with a trip and `plan = null` → `ui.plan == null`, the pipeline's eviction called once. |
| `tombstonedIsNotFound` | `deletedLocally = true` → `NotFound`. |
| `selectIgnoresUnknownDay` | `select("nope", 10.0)` → `anchor` unchanged. |
| `firstFrameTraceBeginsInInit` | Uses a `TraceRecorder` seam if 1d added one; otherwise skip and cover it in Task 13 (Perfetto). |

### Task 4: List index and scroll math

**Commit:** `feat(trip): add trip list index and scroll math`. **Ticks:** none.

**Files** (`feature/trip/…/page/`, pure Kotlin, no Compose import except `LazyListItemInfo`)
- `TripListIndex.kt` replaces 1d's `TimelineListIndex` (delete it in Task 11):
  ```kotlin
  class TripListIndex(dayIds: List<String>, hasHidden: Boolean) {
      val summary = 0; val days = 1
      fun headerIndex(dayId: String): Int      // 2 + 2i
      fun timelineIndex(dayId: String): Int    // 3 + 2i
      val end: Int                             // 2 + 2n
      val hidden: Int?                         // 3 + 2n when hasHidden, else null
      fun dayAt(index: Int): String?           // the day of a header or timeline index, else null
      val itemCount: Int
  }
  ```
  An unknown day id throws `IllegalArgumentException`. Lookups go through a `HashMap`; nothing searches.
- `TripScrollMath.kt`. Every function takes `visible: List<LazyListItemInfo>` plus `firstVisibleIndex: Int` and returns plain values:
  - `currentDay(visible, index, thresholdPx): String?`
    - The day of the **last** visible item whose `contentType` is `"dayHeader"` or `"dayTimeline"` and whose `offset ≤ thresholdPx`.
    - `null` when none qualifies (summary or pills at the top). The caller then keeps the anchor.
    - Port of `TripPageView.swift:187–193` ("last day whose row minY ≤ compactHeight"); a row = header + timeline item.
  - `headerBottomPx(visible, firstVisibleIndex, headerIndex): Float`:
    - header visible → `offset + size`;
    - `headerIndex < firstVisibleIndex` → `Float.NEGATIVE_INFINITY`;
    - otherwise → `Float.POSITIVE_INFINITY`.
  - `compactAlpha(bottomPx, fadePx, reduceMotion): Float`:
    - `reduceMotion` → `if (bottomPx <= 0f) 1f else 0f`;
    - otherwise → `(1f - bottomPx / fadePx).coerceIn(0f, 1f)` [F `TripScrollTracker.swift:128`].
    - `fadePx = PlanMetrics.compactFadeDistance` (24) dp → px.
  - `headerScrolled(visible, firstVisibleIndex, headerIndex): Boolean`: header visible with `offset < 0`, or `headerIndex < firstVisibleIndex` [F `TripPageView.swift`: "the anchor row's minY < 0"].
  - `offsetInDayPx(visible, timelineIndex): Int`: the timeline item visible with `offset < 0` → `-offset`; otherwise 0 [R].
  - `restoreTarget(index, anchor, offsetPx): Pair<Int, Int>`: `offsetPx > 0` → `(timelineIndex, offsetPx)`, otherwise `(headerIndex, 0)`.
- **Threshold:** `thresholdPx = with(density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }` (36, scaled with the font like iOS `@ScaledMetric`; the font scale is already capped at 1.3; OQ-1d-14).

**Tests**
- `TripListIndexTest` (JVM): 3 days, no hidden → headers 2/4/6, timelines 3/5/7, end 8, hidden null, count 9. With hidden → hidden 9, count 10. `dayAt(5) == day1`, `dayAt(0) == null`. An unknown id throws.
- `TripScrollMathTest` (JVM; fake `LazyListItemInfo` with index, offset, size, contentType, threshold 36):
  - header of day 1 at offset 36 → current day 1; at 37 → day 0 when day 0's timeline is at offset −200;
  - only the summary and pills visible → null;
  - `headerBottomPx`: visible (offset −10, size 90) → 80; scrolled past → −∞; below → +∞;
  - `compactAlpha(24, 24, false) == 0`, `(12, 24) == 0.5`, `(0, 24) == 1`, `(−5, 24) == 1`; `(1, 24, true) == 0`, `(0, 24, true) == 1`;
  - `headerScrolled`: offset −1 → true, offset 0 → false;
  - `offsetInDayPx` (timeline at −360 → 360);
  - `restoreTarget`: offset 0 → header index; offset 120 → (timeline index, 120).

### Task 5: Page tokens, plan card, summary card and dual clocks

**Commit:** `feat(trip): add the plan summary card`. **Ticks:** none.

**Files**
- `feature/trip/…/page/TripPageMetrics.kt`:
  ```kotlin
  /** Trip page metrics that the boards `Plan` and `PlanDayMenu` draw and no token covers. */
  object TripPageMetrics {
      val chipHeight = 28.dp          // Plan: zone, warning and notes chips
      val chipPaddingH = 10.dp        // Plan: chips
      val moreHeight = 28.dp          // Plan: "Show more"
      val legendIcon = 18.dp          // Plan: legend info glyph
      val endIcon = 22.dp             // Plan: end-card moon glyph
      val menuWidth = 250.dp          // PlanDayMenu
      val menuItemMinHeight = 52.dp   // PlanDayMenu
      val menuCheckColumn = 20.dp     // PlanDayMenu
      val menuCheckIcon = 18.dp       // PlanDayMenu (stroke 2.6)
      val menuItemGap = 10.dp         // PlanDayMenu
      val menuItemPaddingStart = 12.dp // PlanDayMenu
      val chevron = 20.dp             // DayNavigation glyphs (Phase 2 row chevron size)
  }
  ```
  Use `PlanMetrics` (dp) for everything else:
  - `cardPadding` 16, `cardRadius` 26, `border` 1, `emphasisBorder` 2;
  - `rowRadius` 22, `rowGap` 8, `sectionGap` 12, `smallGap` 4, `laneGap` 4;
  - `gutter` 16, `gutterWidth` 44, `dot` 6, `icon` 16, `touch` 44 (visual only);
  - `compactHeaderHeight` 36, `compactFadeDistance` 24;
  - `warningOpacity` 0.1, `tagOpacity` 0.4.
- `feature/trip/…/page/PlanCard.kt`: `@Composable fun PlanCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`. Port of iOS `PlanCard`:
  - padding `cardPadding`, fill max width, start-aligned;
  - fill `card` with shape `RoundedCornerShape(cardRadius)`;
  - 1 dp `border` stroke;
  - **no shadow** [F `PlanAppearance.swift:59–68`; board has `shadow-soft`, delta C1].
  - Do not reuse `CfCard`: its padding is 20 and its shadow is on.
- `core/plan/…/trippage/DualClockValues.kt` (pure):
  ```kotlin
  enum class ClockLabel { Home, There, YoureHere, HomeTime, DestinationTime }
  data class DualClockValue(val label: ClockLabel, val place: String, val time: String, val active: Boolean)
  fun activeZone(days: List<DayView>, now: Instant): String?       // first zone with start <= now < end, across days.flatMap { zones } → tz
  fun dualClockValues(from: AirportRef, to: AirportRef, now: Instant, activeZone: String?, remote: Boolean,
                      formatter: CopyLineFormatter, options: CopyFormat): List<DualClockValue>
  ```
  Port of `DualClocks` [F `TripPageComponents.swift` l.7–40; `TripPageView.swift:429–431`]:
  - places `[from, to]`;
  - `active = place.zone == activeZone && (index == 0 || from.zone != to.zone)`;
  - label:
    - `remote` → `Home` (index 0) or `There`;
    - else `active` → `YoureHere`;
    - else index 0 → `HomeTime`;
    - else `DestinationTime`;
  - `place = city ?: iata`;
  - `time = formatter.formatInstant(now, options, place.zone)`.
  6b always passes `remote = false` (the remote summary is 6c).
- `feature/trip/…/page/SummaryClockState.kt`: `class SummaryClockState { var values by mutableStateOf<List<DualClockValue>>(emptyList()); private set; fun apply(v) }` (main thread only). Only `DualClocks` reads it.
- `feature/trip/…/page/SummaryCard.kt`:
  ```kotlin
  @Composable fun SummaryCard(prepared: PreparedPlan, clocks: SummaryClockState, onLegend: (() -> Unit)?, modifier: Modifier = Modifier)
  ```
  Port of `TripPageView.summary` (l.415–435):
  - `PlanCard` → `Column(verticalArrangement = spacedBy(sectionGap))`:
    1. `Row(verticalAlignment = Top)`:
       - `Column(spacedBy(smallGap))`:
         - `Text(<"Your plan">, style = eyebrow, color = dawnText)`, **not** uppercased [F iOS, screenshot `06-trip-summary.png`; board uppercases, delta C2];
         - `prepared.shift?.let { Text(it, style = welcomeWordmark, color = foreground) }`;
       - `Spacer(Modifier.weight(1f))`;
       - **legend button:**
         - a 44 dp (`PlanMetrics.touch`) circle, fill `surface2`;
         - lucide `Info` at `legendIcon` 18 dp, tint `foreground` [verify the board colour at `Plan.dc.html` "Legend"];
         - `minimumInteractiveComponentSize()`, `Role.Button`;
         - content description <"Legend: what the colours and patterns mean">;
         - tag `trip.legend`;
         - enabled only when `onLegend != null` (null until 6d; disabled alpha as `CfButton`) [F iOS 15 fix: "Unavailable callbacks disable their designed controls"].
    2. `Text(prepared.summary, style = planListLine, color = mutedForeground)`.
    3. `DualClocks(clocks)`.
  - Card semantics: `Modifier.semantics { contentDescription = <"Plan summary"> }.testTag("trip.summary")`, children not merged (iOS `.contain`).
- `DualClocks(clocks: SummaryClockState)`:
  - `SideEffect { probe?.composed(SUMMARY_CLOCK, "summary") }` is the first statement.
  - `Row(spacedBy(rowGap))` of 2 cells, each `Modifier.weight(1f)`:
    - `Column(spacedBy(smallGap))`, padding `sectionGap`;
    - label: `stringResource(<key for label>, place)` in `planSubtitle`, colour `dawnText` if active else `mutedForeground`;
    - time `Text(value.time)` in `planTitle` with `fontFeatureSettings = "tnum"`;
    - background `card` if active else `surface2`;
    - shape `RoundedCornerShape(rowRadius)`;
    - border `emphasisBorder` in `dawn` if active else `surface2`.
- Add `SUMMARY_CLOCK`, `PILLS` and `COMPACT` to `TimelineProbe.Kind` (1d).

**Copy:** `Your plan`; `Legend: what the colours and patterns mean`; `Plan summary`; `Home · %@`; `There · %@`; `You’re here · %@` (the key has a typographic apostrophe); `Home time · %@`; `Destination time · %@` (Appendix A).

**Edge cases**
- `prepared.shift == null`: the eyebrow is alone.
- Both airports in one zone: only index 0 can be active.
- `now` outside every zone: no cell is active, and both cells use `surface2`.

**Tests**
- `DualClockValuesTest` (JVM, S3 fixture, en): now in the destination zone → index 1 is `YoureHere` and active, index 0 is `HomeTime`. Now in the origin zone → index 0 is `YoureHere`. Same-zone trip with the active zone → only index 0 active. `remote = true` → `Home`/`There`, none active. `activeZone` before the first zone → null.
- `SummaryCardTest` (Robolectric `@Logic`, `pixel8`, en):
  - `trip.summary` has content description "Plan summary";
  - `trip.legend` is disabled with `onLegend = null` and enabled with a lambda, and a click calls it once;
  - the eyebrow text is exactly "Your plan" (not uppercased);
  - with `CountingTimelineProbe` provided, `summaryClock.apply(new values)` gives a `SUMMARY_CLOCK` composition delta of exactly 1, and the new time text is displayed.

### Task 6: Day navigation pills

**Commit:** `feat(trip): add day navigation pills`. **Ticks:** none.

**File:** `feature/trip/…/page/DayNavigation.kt`:
```kotlin
@Composable fun DayNavigation(days: List<PreparedDay>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier)
```
Port of `TripPageComponents.swift:72–116`:
- `SideEffect { probe?.composed(PILLS, selectedId ?: "") }` is the first statement.
- `Row(horizontalArrangement = spacedBy(smallGap), verticalAlignment = CenterVertically)`:
  1. **Previous:**
     - `CfIconButton(LucideGlyphs.ChevronLeft.imageVector(), <"Previous day">, style = Ghost)` with the glyph at `TripPageMetrics.chevron`;
     - enabled when the selected index > 0;
     - on click `onSelect(days[i - 1].id)`;
     - tag `trip.days.previous`.
  2. **Pills:** `LazyRow(state = rowState, horizontalArrangement = spacedBy(rowGap), modifier = Modifier.weight(1f).semantics { contentDescription = <"Plan days"> }.testTag("trip.days"))`, with `items(days, key = { it.id })`. Each pill:
     - `Column(horizontalAlignment = Start)`: `day.title` in `authStrong`, `day.date` in `planSubtitle`;
     - padding horizontal `gutter` (16), `heightIn(min = 48.dp via minimumInteractiveComponentSize)`, content vertically centred;
     - selected: fill `dawn`, content `dawnForeground`; otherwise fill `card`, content `foreground`;
     - `CircleShape`-style full pill shape, 1 dp `border` stroke always;
     - `clickable(role = Role.Tab) { onSelect(day.id) }`;
     - `semantics(mergeDescendants = true) { contentDescription = stringResource(plan_title_comma_date, title, date); selected = (day.id == selectedId) }`;
     - tag `trip.days.pill.<i>`.
  3. **Next:** as Previous with `ChevronRight`, <"Next day">, tag `trip.days.next`.
- **Auto-centre** [F iOS `.onChange(of: selected) { scrollTo(id, anchor: .center) }`]: `LaunchedEffect(selectedIndex)`:
  - if the item is visible, the delta is `itemCenter − viewportCenter` from `rowState.layoutInfo`;
  - else first `rowState.scrollToItem(i)` and recompute;
  - then `animateScrollBy(delta)`, or `scrollBy(delta)` under Reduce Motion.
  - The values are read only in the effect (rule 5).
- Add the untranslatable `plan_title_comma_date` = `%1$s, %2$s` to `core/ui/src/main/res/values/strings_untranslatable.xml` (`translatable="false"`; iOS builds `day.title + ", " + day.date` inline, `TripPageComponents.swift:99`).

**Copy:** `Previous day`, `Next day`, `Plan days`.

**Edge cases**
- One day: both chevrons are disabled.
- `selectedId == null`: no pill is selected, and Previous/Next act from index 0 (iOS `?? 0`).

**Tests:** `DayNavigationTest` (Robolectric `@Logic`, `pixel8`, 7 days):
- with day 0 selected, `trip.days.previous` is disabled and `trip.days.next` is enabled;
- clicking next calls `onSelect(day1)`;
- the selected pill has `selected = true` and the description "<title>, <date>";
- after the selection changes to day 6 (Reduce Motion on), `trip.days.pill.6` is fully displayed and its centre is within 1 px of the row centre, or the row is scrolled to its end.

### Task 7: Day header and lane headings

**Commit:** `feat(trip): add the designed day header`. **Ticks:** none.

**Files**
- `feature/trip/…/page/DayHeader.kt`: `@Composable fun DayHeader(day: PreparedDay, modifier: Modifier = Modifier)`. Port of `DayHeader.swift` (Appendix B):
  - `SideEffect { probe?.composed(HEADER, day.id) }` is the first statement (replaces 1d's `DayHeaderLine`).
  - `var expanded by rememberSaveable(day.id) { mutableStateOf(false) }` and `var notesOpen by rememberSaveable(day.id) { mutableStateOf(false) }`.
  - Root `Column(verticalArrangement = spacedBy(smallGap))`, vertical padding `rowGap` (8), background `background`, content colour `foreground`.
  - **Heading + zone:** a custom `HeadingZoneLayout` (`Layout` with two measurables):
    - Row form when `heading.maxIntrinsicWidth + smallGap + zone.width ≤ maxWidth`: place the heading at the start and the zone at the end, aligned on `FirstBaseline` (iOS `HStack(alignment: .firstTextBaseline)` + `Spacer(minLength: 4)`).
    - Otherwise the column form: heading, then the zone below with `smallGap`.
    - This ports `ViewThatFits`.
  - **Heading:** one `Text` of `AnnotatedString`:
    - built from `stringResource(plan_day_title_date, day.title, day.date)` (1d, `%1$s · %2$s`);
    - span `[0, title.length)` styled `planDayTitle` / `foreground`;
    - the rest styled `planListLine` / `mutedForeground` with `tnum`;
    - `Modifier.semantics { heading() }.testTag("trip.dayHeader.${day.id}")`.
  - **Zone chip** (iOS explicit metrics win over the board chip, delta C4):
    - `Text(day.zone, style = planSubtitle)`, padding `smallGap` (4);
    - background `muted` with a full pill shape.
  - **Warnings** (`day.warningCount > 0`), built with the board chip look (rule 1; iOS uses a system `Button` + `Label`):
    - `Row(spacedBy(smallGap), CenterVertically)`, height `chipHeight` 28, padding horizontal `chipPaddingH` 10;
    - full pill, fill `dawn` @ `warningOpacity`, 1 dp border `dawn` @ `tagOpacity`;
    - lucide `TriangleAlert` 14 dp (`PlanMetrics.blockIcon`) tinted `dawnText`;
    - text `pluralStringResource(<"%lld warnings">, n, n)` in `settingsBadge` (12/16 500), colour `foreground`;
    - `minimumInteractiveComponentSize()`, `Role.Button`, toggles `expanded`;
    - tag `trip.dayHeader.warnings.${day.id}`.
  - **When `expanded`** [F iOS order]:
    - `day.status?` in `authStrong` / `dawnText`;
    - `day.summary` in `planSubtitle` with `tnum`;
    - `day.progress` in `planSubtitle`;
    - `day.headline?` in `planSubtitle` / `dawnText`;
    - the **notes chip** if `warningCount == 0 && notices.isNotEmpty()`:
      - board look: as the warnings chip but fill `card`, border `border`, lucide `Info` 14 dp in `foreground`;
      - text `pluralStringResource(<"%lld notes">, notices.size, …)`;
      - toggles `notesOpen`;
    - then `day.shownNotices(expanded, notesOpen).forEach { DayNotice(it) }`.
  - **`DayNotice`:**
    - `Row(spacedBy(sectionGap), verticalAlignment = Top)`, padding `sectionGap`;
    - lucide `TriangleAlert` (warning, `dawnText`) or `Info` (`mutedForeground`) at `PlanMetrics.icon` 16, hidden from accessibility;
    - `Text(notice.text, planListLine, Modifier.weight(1f))`;
    - background `dawn` @ `warningOpacity` or `card`, shape `rowRadius`;
    - 1 dp border `dawn` @ `tagOpacity` or `border`;
    - `semantics(mergeDescendants = true)`.
    - [verify the 1b field names `PreparedNotice.warning`/`text`.]
  - **Show more / Show less** (board look, delta C5):
    - full-width `Box(contentAlignment = Center)` with `heightIn(min = moreHeight)` + `minimumInteractiveComponentSize()`;
    - `Text(<expanded ? "Show less" : "Show more">, style = planSubtitle, color = mutedForeground)`, no chevron (the board has none);
    - `clickable(role = Role.Button) { expanded = !expanded }`;
    - tag `trip.dayHeader.more.${day.id}`.
- `feature/trip/…/page/LaneHeadings.kt`: `@Composable fun LaneHeadings(day: PreparedDay?, modifier: Modifier = Modifier)`. Port of `DayTimelineView.swift` `LaneHeadings`:
  - `Row(horizontalArrangement = spacedBy(laneGap), verticalAlignment = CenterVertically)`.
  - **Gutter cell**, width `gutterWidth` 44, end-aligned:
    - with a day: `Column(horizontalAlignment = End, Modifier.padding(end = smallGap))` of `day.weekday` in `planCompactWeekday` / `foreground` and `day.dayMonth` in `planCompactDate` with `tnum`;
    - without a day: an empty `Spacer`.
  - **Four cells**, each `weight(1f)`, `Row(spacedBy(smallGap), CenterVertically, horizontalArrangement = Center)`:
    - a `dot` (6 dp) circle in `catSleep` / `catLight` / `catCaffeine` / `catTravel` [verify the `CfColors` names];
    - the label <"Sleep"> / <"Light"> / <"Caffeine"> / <"Travel"> in `planLane`, `maxLines = 1`, ellipsis, colour `mutedForeground`.
    - Board dots are 8 px; iOS `PlanMetrics.dot` 6 wins (delta C6).
  - **Semantics:**
    - with a day: `clearAndSetSemantics { contentDescription = stringResource(plan_title_comma_date, day.title, day.date) }`;
    - without a day: `clearAndSetSemantics { }` (hidden).

**Copy:** `%lld warnings` (plural), `%lld notes` (plural), `Show more`, `Show less`, `Sleep`, `Light`, `Caffeine`, `Travel`.

**Edge cases**
- A long nl day title with a zone at `largeFont` uses the column form.
- `warningCount == 0` with no notices: only the heading and Show more.
- `shownNotices(expanded = false, …)` is empty, so notices never show collapsed [F 1b; the board shows warnings collapsed, delta C7].

**Tests**
- `DayHeaderTest` (Robolectric `@Logic`, `pixel8`, S3 days with and without warnings):
  - collapsed: no status text, and the warnings chip text is "2 warnings" when `warningCount == 2` (plural "1 warning" for one);
  - clicking Show more shows status, summary, progress and the warning notices, and the label becomes "Show less";
  - a day with 0 warnings and 2 notes, expanded: the notes chip "2 notes", notices hidden until it is clicked, then 2 notices;
  - the heading node has `heading()`;
  - `expanded` survives `StateRestorationTester` recreation.
- `HeadingZoneLayoutTest` (Robolectric `@Logic`):
  - width 360 dp with "Day 3 · Wed 14 Oct" and "Tokyo" → the zone top equals its row-form position (same baseline);
  - width 200 dp → the zone top ≥ the heading bottom.
- `LaneHeadingsTest` (Robolectric `@Logic`): with a day, one node with description "<title>, <date>"; without a day, no semantics node with the lane labels.

### Task 8: Top bar and day menu

**Commit:** `feat(trip): add the trip top bar and day menu`. **Ticks:** none.

**File:** `feature/trip/…/page/TripTopBar.kt`:
```kotlin
@Composable fun TripTopBar(
    title: TripTitleParts?, prepared: PreparedPlan?, ready: Boolean,
    anchorDay: PreparedDay?, headerScrolled: Boolean,
    onBack: () -> Unit, onPickDay: (Int) -> Unit, onActions: (() -> Unit)?,
)
```
Port of `TripPageView.toolbar`, `titleItem`, `dayMenuLabel` and `subtitle`:
- **Container:** `CenterAlignedTopAppBar(colors = topAppBarColors(containerColor = background, scrolledContainerColor = background, titleContentColor = foreground))`, pinned, no scroll behaviour.
- **Navigation:** `CfBackButton(contentDescription = <App "My trips">, onClick = onBack)` [R: the board's back control is labelled "My trips"; iOS uses the system back with the previous title. M3 mapping: `arrow_back`].
- **Title slot**, `DayMenuAnchor`:
  - `SideEffect { probe?.composed(ANCHOR, anchorDay?.id ?: "") }` is the first statement.
  - `Column(horizontalAlignment = CenterHorizontally)`:
    - `Text(code, style = planTitle, maxLines = 1)`, where `code = stringResource(plan_x_x, fromIata, toIata)` (`%@ → %@`) or <"Loading your plan"> when `title == null`;
    - when ready, a `Row(spacedBy(smallGap), CenterVertically)`:
      - `Text(subtitle, style = planSubtitle, color = mutedForeground, maxLines = 1, overflow = Ellipsis)` with the Phase 2 `ellipsizable()` marker;
      - lucide `ChevronDown` 12 dp in `mutedForeground`, hidden from accessibility.
      - `subtitle = if (headerScrolled) stringResource(plan_day_title_date, anchorDay.title, anchorDay.date) else prepared.subtitle`.
  - **Not ready:** no click, tag `trip.title`, no subtitle.
  - **Ready:** tag `trip.dayMenu` and `clickable(role = Role.Button, onClickLabel = <"Go to a day">) { expanded = true }`.
    - `contentDescription` (iOS `dayMenuLabel`, 1d):
      - `headerScrolled` → `"%@ to %@, %@, %@. Go to a day"` (fromName, toName, day title, day date);
      - else → `"%@ to %@, %@. Go to a day"` (fromName, toName, `prepared.spokenDates`).
    - `stateDescription` = `"<title> · <date>"` of `anchorDay`.
- **Day menu:** M3 `DropdownMenu(expanded, onDismissRequest, containerColor = popover, shape = RoundedCornerShape(cardRadius), scrollState = menuScroll, modifier = Modifier.width(menuWidth))` [F board note: "the system menu (… Compose DropdownMenu on Android)"].
  - **Title row**, not focusable as an action:
    - `Text(<"Go to a day">, style = planListLine, color = mutedForeground, textAlign = Center)`;
    - padding top 8 (`rowGap`), horizontal 16 (`gutter`), bottom 4 (`smallGap`).
    - iOS copy wins over the board's "Go to day" (`04 §3.20`).
  - **One `DropdownMenuItem` per day**, `i` 0-based:
    - `contentPadding = PaddingValues(start = menuItemPaddingStart, end = gutter)`;
    - `modifier = Modifier.heightIn(min = menuItemMinHeight).testTag("trip.dayMenu.day.$i").semantics { selected = day.id == anchorDay?.id }`;
    - `leadingIcon`: a `Box(Modifier.width(menuCheckColumn))` holding lucide `Check` (`imageVector(strokeWidth = 2.6f)`) at `menuCheckIcon`, `foreground`, only for the current day;
    - `text`: `Column`:
      - `Text(day.title, style = planTitle.copy(fontWeight = FontWeight.Normal))` (board 17/22 400);
      - `Text(day.date, style = planListLine, color = mutedForeground)` with `tnum`;
    - `onClick = { expanded = false; onPickDay(i) }`;
    - the gap between the check column and the text is `menuItemGap`.
  - **Pre-scroll on open** (`04 §9.10`): `LaunchedEffect(expanded) { if (expanded) menuScroll.scrollTo(i_current × menuItemMinHeight.roundToPx()) }`.
- **Actions:**
  - `CfIconButton(MaterialSymbols.MoreVert, <"Trip actions">, onClick = { onActions?.invoke() }, enabled = onActions != null)`, tag `trip.actions.open` (OQ-A11; iOS ellipsis). It is `null` in 6b (6d).
  - The view toggle is 6c: do not reserve space.

**Copy:** `%@ → %@`, `Loading your plan`, `Go to a day`, `%@ to %@, %@. Go to a day`, `%@ to %@, %@, %@. Go to a day`, `Trip actions`, App `My trips`.

**Tests:** `TripTopBarTest` (Robolectric `@Logic`, `pixel8`, en, **S1** prepared: S1 is AMS→HND with 13 days; S3 is AMS→SYD, 1b Task 2):
- not ready → `trip.title` has the text "Loading your plan", has no click action, and `trip.dayMenu` does not exist;
- ready, not scrolled → `trip.dayMenu` has the description "Amsterdam to Tokyo, <spokenDates>. Go to a day" (fixture values [verify]) and the subtitle `prepared.subtitle`;
- `headerScrolled = true` → the subtitle "<title> · <date>" and the 4-argument description;
- click → 13 `trip.dayMenu.day.<i>` nodes (one per S1 day);
- the current one has `selected = true` and the check icon;
- clicking `day.4` calls `onPickDay(4)` and closes the menu;
- the actions button is disabled.
- `DayMenuScrollTest` (Robolectric `@Logic`, large60): with anchor day 50, opening the menu shows `trip.dayMenu.day.50` without a further scroll (`assertIsDisplayed`).

### Task 9: Compact header

**Commit:** `feat(trip): fade in the compact day header`. **Ticks:** none.

**File:** `feature/trip/…/page/CompactHeader.kt`:
```kotlin
@Composable fun CompactHeader(day: PreparedDay?, alpha: () -> Float, visible: Boolean, horizontalPadding: PaddingValues, modifier: Modifier = Modifier)
```
- `SideEffect { probe?.composed(COMPACT, day?.id ?: "") }` is the first statement.
- `Box(modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha() }.background(card).padding(horizontalPadding).heightIn(min = thresholdDp).testTag("trip.compactHeader"))`:
  - `alpha` is a lambda read in the layer block, so it is never read in composition;
  - `thresholdDp` = `compactHeaderHeight.sp` to dp, the same value as Task 4;
  - content: `LaneHeadings(day)` vertically centred;
  - opaque `card` [F `04` OQ-T13, iOS wins over the board's glass and its 0.5 hairline, delta C8].
- **Semantics:** when `!visible`, apply `Modifier.clearAndSetSemantics { }` (iOS `accessibilityElementsHidden = opacity < 1`). `visible` comes from the page as a `derivedStateOf { alphaNow.value() >= 1f }` over `rememberUpdatedState(alpha)` (Task 11 step 8), so it flips only at boundaries and follows the current anchor.
- The overlay has no pointer input, so touches reach the list underneath.

**Tests:** `CompactHeaderTest` (Robolectric `@Logic`):
- `alpha = { 0.5f }, visible = false` → the node `trip.compactHeader` exists and has no child semantics;
- `visible = true` → one node with "<title>, <date>";
- a click at the overlay's centre reaches a `clickable` placed underneath it.

### Task 10: End card and hidden-steps notice

**Commit:** `feat(trip): add the end of plan card`. **Ticks:** "End of plan".

**Files**
- `feature/trip/…/page/EndOfPlanCard.kt`: `@Composable fun EndOfPlanCard(prepared: PreparedPlan, onReturnPlan: (String) -> Unit)`. Port of `TripPageComponents.swift:45–70`:
  - `PlanCard(Modifier.testTag("trip.plan.end"))` → `Column(spacedBy(sectionGap), horizontalAlignment = CenterHorizontally, Modifier.fillMaxWidth())`, text centred.
  - The moon: a `Box` of `PlanMetrics.touch` (44, visual) circle in `dawn`, with lucide `Moon` at `endIcon` 22 in `dawnForeground`, hidden from accessibility. The board circle is 48 dp; iOS wins (delta C9).
  - `Text(<"End of this plan">, style = planCardTitle, Modifier.semantics { heading() }.testTag("trip.plan.end.title"))`. iOS copy wins over the board's "That's the end of this plan" (`04 §3.20`).
  - `Text(prepared.endText, style = authBody, color = mutedForeground)`.
  - `prepared.assumedEnd?.let { Text(it, style = planSubtitle) }`.
  - `prepared.returnDayID?.let { id -> CfButton(<"See your return plan">, onClick = { onReturnPlan(id) }, style = Primary, size = Lg, fillWidth = true) }`. This is the board look (`bg-primary h-10 px-6`, full width); iOS uses the system `.borderedProminent`.
  - The card keeps the iOS padding 16 (board 20, delta C9).
- `feature/trip/…/page/HiddenStepsButton.kt`:
  - `CfButton(<"Some steps are hidden in your preferences.">, onClick = { onSettings?.invoke() }, enabled = onSettings != null, style = Ghost, size = Touch, fillWidth = true, textStyle = planListLine)`;
  - tag `trip.hiddenSteps`;
  - shown only when `ReadyPlan.hasHiddenSteps` [F `TripPageView.swift:143–145`].
  - **No board draws it**: it is built from iOS with the Phase 2 primitive, and nothing else is designed around it (OQ-6b-6).

**Copy:** `End of this plan`, `See your return plan`, `Some steps are hidden in your preferences.`, plus the strings that arrive prepared (`Assumed trip end`, `Based on your check-out date`, `Based on %@ nights`, `There are no more steps in this plan.`) come from 1b. Do not add them.

**Tests**
- `EndOfPlanCardTest` (Robolectric `@Logic`):
  - S3 → `trip.plan.end.title` is a heading with "End of this plan";
  - with `returnDayID` the button is displayed and a click calls `onReturnPlan(id)`; without it there is no button;
  - `assumedEnd` null → no extra text.
- `HiddenStepsButtonTest` (`@Logic`): `onSettings = null` → disabled; with a lambda, a click calls it once.

### Task 11: Assemble the trip page

**Commit:** `feat(trip): assemble the trip page`. **Ticks:** "Page chrome", "Opening position".

**Files**
- `feature/trip/…/page/TripPage.kt`:
  ```kotlin
  @Composable fun TripPage(viewModel: TripPageViewModel, onBack: () -> Unit, onSettings: (() -> Unit)?,
                           onLegend: (() -> Unit)? = null, onActions: (() -> Unit)? = null, onRefreshed: (SyncResult?) -> Unit = {})
  ```
  Structure (`04 §9.5`):
  1. **Clock gate:** as 1d: `LaunchedEffect(viewModel, owner) { owner.repeatOnLifecycle(STARTED) { viewModel.runClock() } }`.
  2. **Language:** `val lang = LocalConfiguration.current.locales[0]` mapped as 1d → `LaunchedEffect(lang) { viewModel.setLanguage(lang) }`.
  3. `val ui by viewModel.ui.collectAsStateWithLifecycle()`.
  4. **Scaffold:** `Scaffold(topBar = { TripTopBar(...) }, containerColor = background)`, with `contentWindowInsets` matching 6a's screens (do not double the bottom inset [verify 6a]).
  5. **Body:**
     - When `ui.state != Ready || ui.plan == null`: the 6c seam. A `Box(Modifier.fillMaxSize().padding(inner).testTag("trip.page"))` with `Modifier.semantics { contentDescription = <"Loading your plan"> }.testTag("trip.timeline.preparing")`. Nothing else (6c builds the states).
     - **Ready:**
       - `CfPullToRefreshBox(isRefreshing, onRefresh = { scope.launch { refreshing = true; viewModel.refresh(); refreshing = false } })` → `Box(Modifier.fillMaxSize().padding(inner).testTag("trip.page"))` holding the list and the compact header;
       - wire 6a's refresh capsule exactly as My trips does, if 6a exposes it as a composable; otherwise leave it out and report it (OQ-6b-5).
       - `fun refresh()` on the ViewModel calls `source.refresh()`.
  6. **List state:**
     - `val listState = remember(viewModel) { val a = viewModel.openingScroll(); val (i, o) = if (startAtSummary && firstOpen) 0 to 0 else restoreTarget(index, a, a.offset.dp.roundToPx()); LazyListState(i, o) }`;
     - created **inside** the Ready branch, so the first frame composes only the opening day and its neighbours [F `04 §9.5`, RN lesson 4];
     - `firstOpen` is a ViewModel flag that is true until the first Ready composition consumes it.
  7. **LazyColumn:**
     - `LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("trip.scroll"), contentPadding = PaddingValues(start = gutter + safeStart, end = gutter + safeEnd, bottom = PlanMetrics.gutter + unconsumedBottomInset))`;
     - `gutter` from `TripPageLayout.resolve(windowWidthDp).gutter` (16 here);
     - `verticalArrangement = spacedBy(0.dp)`.
     - Items, in order (`04 §9.5`). The 12 dp `sectionGap` is the item's top padding, so the timeline item has no seam.

     | Key | contentType | Content | Top padding |
     |---|---|---|---|
     | `summary` | `summary` | `SummaryCard(prepared, vm.summaryClock, onLegend)` | 0 |
     | `days` | `dayNav` | `DayNavigation(days, anchor.dayID, onSelect = ::jumpTo)` | 12 |
     | `h:<dayId>` | `dayHeader` | `DayHeader(day)` | 12 |
     | `t:<dayId>` | `dayTimeline` | `Column(Modifier.testTag("trip.day.$id")) { LaneHeadings(null); DayTimeline(...) }`. `DayTimeline` keeps 1d's `DayDrawingKey`, `NowLine`, `TimelineTargets` and `onBlock = vm::selectBlock`. The `NowLine` stays inside `DayTimeline`, so it is positioned against the timeline, not the item. | 0 |
     | `end` | `end` | `EndOfPlanCard(prepared, onReturnPlan = ::jumpTo)` | 12 |
     | `hidden` | `hidden` | `HiddenStepsButton(onSettings)` (only when `index.hidden != null`) | 12 |

     Drop 1d's per-day bottom padding: the next header's top padding replaces it.
  8. **Compact header**, the second child of the Box, aligned top:
     - `currentAnchorHeader = remember(anchor) { index.headerIndex(anchor.dayID) }`;
     - `alpha = { compactAlpha(headerBottomPx(listState.layoutInfo.visibleItemsInfo, listState.firstVisibleItemIndex, currentAnchorHeader), fadePx, reduceMotion) }`;
     - `val alphaNow = rememberUpdatedState(alpha)` and `visible by remember { derivedStateOf { alphaNow.value() >= 1f } }`. A bare `alpha()` inside `remember { derivedStateOf { … } }` keeps the first composition's `currentAnchorHeader` and `reduceMotion` forever, so after an anchor change TalkBack would reach an invisible header;
     - `CompactHeader(anchorDay, alpha, visible, PaddingValues(horizontal = gutter + safe))`.
  9. **Current day and offset** (`04 §4.2`):
     - `val current by remember(index) { derivedStateOf { currentDay(visible, index, thresholdPx) } }`;
     - `LaunchedEffect(current) { current?.let { viewModel.select(it, offsetDp(it)) } }`, where `offsetDp` reads `offsetInDayPx` in the effect;
     - `LaunchedEffect(listState) { snapshotFlow { listState.isScrollInProgress }.filter { !it }.collect { viewModel.select(anchorId, offsetDp(anchorId)); viewModel.saveAnchor() } }`.
  10. **headerScrolled:** `derivedStateOf { headerScrolled(…, index.headerIndex(anchor.dayID)) }`, passed to the top bar. It flips only at boundaries.
  11. **Jumps and commands** (`04 §4.3`, §9.10, 1d):
      - `LaunchedEffect(viewModel) { viewModel.scrollCommands.collect { cmd -> ... } }`;
      - wait for the target drawing for at most 50 ms (`withTimeoutOrNull(50) { repository.drawing(key(cmd.dayId)) }`);
      - then compute `(i, o) = restoreTarget(index, cmd, cmd.offset.dp.roundToPx())`;
      - then `if (cmd.animated && !reduceMotion) animateScrollToItem(i, o) else scrollToItem(i, o)`.
      - `jumpTo(dayId)` = `viewModel.jump(dayId)` (menu, pills, prev/next, return plan).
      - Menu pick `i` → `jumpTo(days[i].id)`.
  12. **Perf markers (1e):** keep `FirstTimelineFrameTrace.onReplay()` on the first real replay and the zero-size `trip.timeline.presented` Box exactly as 1e placed them, moved from `TimelinePage`. Keep the zero-size `trip.timeline.ready` marker once Ready is composed.
- **Delete** the 1d host:
  - `TimelinePage.kt`, `TimelineHostViewModel.kt`, `TimelineSource.kt`, `TimelineListIndex.kt`;
  - 1d's `DayMenu` and `DayHeaderLine`;
  - their tests `TimelinePageTest`, `TimelinePageGuardTest`, `TimelineListIndexTest` (replaced below and in Task 15);
  - `stickyHeader` `@OptIn`s.
- **Update** `TimelinePerfMarkersTest` to host `TripPage` through `TimelineTestHost` (Task 12).

**Edge cases**
- The plan is replaced while the menu is open: the menu closes when `days` changes (`remember(days)` on `expanded`).
- A jump to the current day still scrolls to its header top.
- `startAtSummary`: the first Ready shows index 0, and the first plan sends no command (iOS `old == nil && startAtSummary`).
- A rotation (handled in place by A8): the same ViewModel and `listState`, new widths, a new drawing key. The keys keep the position.
- An activity recreation (locale or font scale, A8): `openingScroll()` gives the saved anchor (Task 3). Process death is a cold launch (A8) and never restores the page.

**Tests:** `TripPageTest` (Robolectric `@Logic`, `pixel8`, `FakeTripPageSource.fixture("large60")` unless stated, `FakePlanClock`, `FakeTextMetrics`, dispatchers `Unconfined`, each < 5 s):

| Test | Asserts |
|---|---|
| `opensOnTheDayContainingNow` | Clock at day 33 start + 2 h → `trip.dayHeader.<day33>` top == `trip.scroll` top (±1 px). `trip.day.<day32>` does not exist. |
| `pastTripOpensOnLastDay` | S3 with the clock after its end → the last day's header at the top. |
| `summarySeedOpensAtTheSummary` | `startAtSummary = true` → `trip.summary` top == `trip.scroll` top. The compact header has no semantics children. |
| `menuJumpReachesTheEnd` | Reduce Motion on. Pick `trip.dayMenu.day.59` → `trip.dayHeader.<day59>` at the top, then `performScrollToNode(hasTestTag("trip.plan.end"))` succeeds. |
| `pillJumpSelectsDay` | From the summary position, click `trip.days.next` → `vm.anchor.dayID == day1` and day 1's header at the top. |
| `compactHeaderFadesIn` | Scroll until day 33's header bottom is 12 dp below the top → the layer alpha is 0.5 (read with `fetchSemanticsNode().layoutInfo` / the `graphicsLayer` test helper [verify]) and there are no semantics. Then 1 dp past 0 → visible, with "<day33 title>, <date>". |
| `compactHeaderSemanticsFollowTheAnchor` | Open on day 0, then pick `trip.dayMenu.day.2` and wait until `vm.anchor.dayID` is day 2's id, with day 2's header at the top of `trip.scroll` → `trip.compactHeader` has no semantics. Then scroll, as in `compactHeaderFadesIn`, until day 2's header bottom is 1 dp above the top → visible, with "<day2 title>, <date>". |
| `anchorFollowsTheCurrentDay` | Scroll until day 34's header offset ≤ `thresholdPx` → `vm.anchor.dayID == day34`, and `trip.dayMenu` `stateDescription` "<day34 title> · <date>". |
| `subtitleSwitchesWhenTheHeaderScrolls` | At the header top → `prepared.subtitle`. 1 px into the header → "<title> · <date>". |
| `endCardAndHiddenSteps` | Preferences with hidden `[BlockType.CaffeineServing]` → `trip.hiddenSteps` exists after the end card; without → it does not exist. |
| `replacementKeepsTheAnchor` | Emit a snapshot whose plan renames the anchor day id → the renamed day's header is still at the top. |

### Task 12: Invalidation on the real page

**Commit:** `test(trip): rerun the invalidation guard on the trip page`. **Ticks:** none (the "Tests" tick is in Task 15).

**Files**
- `feature/trip/src/test/…/timeline/TimelineTestHost.kt`: retarget it to `TripPage(vm, onBack = {}, onSettings = null)`, with `TripPageViewModel` built on `FakeTripPageSource.fixture("large60")`. Keep:
  - the clock `FakePlanClock(day0.startUtc)`;
  - the `Unconfined` dispatchers;
  - the pre-warmed 64-entry `DayDrawingCache` (all 60 drawings for the test keys);
  - `LocalTimelineProbe provides probe`;
  - the snapshot/delta helpers.
  - The opening position is day 0's header (index 2), so `summary` and `days` are not composed.
- `TimelineInvalidationTest`: keep the five method names and the 1d assertions. Then extend each:

| Method | Added assertions |
|---|---|
| `…WhileScrolling` (120 × 1 dp, then one day boundary) | During the 120 deltas: `HEADER`, `PILLS`, `COMPACT` composition deltas = 0, and `ANCHOR` ≤ 1 (the single `headerScrolled` flip). After the boundary: `COMPACT` delta = 1 (new day), `ANCHOR` ≥ 1, `vm.anchor.dayID == day1`. |
| `…OnMinuteTick` | `HEADER`, `COMPACT`, `ANCHOR` = 0. |
| `…OnDayBoundary` | `HEADER`, `COMPACT`, `ANCHOR` = 0 (the clock does not move the anchor). |
| `checkOffRedrawsOnlyThatDay` | The new input is `source.snapshot.value.copy(checks = + CheckMarkRecord(done, first actionable block of day k))`. `HEADER` delta for day k ≤ 1 (progress text changes), 0 for every other day. |

- New method `minuteTickRecomposesOnlyTheSummaryClock`:
  - host with `startAtSummary = true`;
  - `clock.advance(60 s)`;
  - assert `SUMMARY_CLOCK` delta = 1, and `PILLS`, `HEADER`, `ANCHOR`, `STATIC` (compositions and draws) = 0 for every key, and `CLOCK` = 1 only for the day containing now.
- Each method stays < 5 s (1d fallback: `advanceTimeByFrame` with `autoAdvance = false`, never fewer steps).

### Task 13: Open the real page in the app and the perf entry

**Commit:** `feat(app): open the trip page from the route and the perf entry`. **Ticks:** "Opening position" (seed part) if not ticked in Task 11.

**Files**
- `app/src/main/kotlin/net/chronoflow/app/trip/TripPageRoute.kt` replaces 6a's placeholder for `Route.TripPage`:
  ```kotlin
  @Composable fun TripPageRoute(route: Route.TripPage, onBack: () -> Unit, onSettings: () -> Unit)
  ```
  - `viewModel(key = "trip:${route.tripId}", factory = tripPageViewModelFactory(route, startAtSummary, freezeClock))`.
  - The factory reads `this[AppGraphKey]!!`, captures `graph.sessionGraphs.current.value!!` at creation (4e rule: a screen in `key(revision)` never outlives its epoch), and calls `createSavedStateHandle()`.
  - It builds `DbTripPageSource(session)`, `DefaultTripPlanPipeline(session-scoped decoder, preparer)` (the 1a/1b instances the session graph holds; create them in `SessionGraph` if 4e did not [verify]), clock, `Dispatchers.Default`, `Dispatchers.Default.limitedParallelism(2)` and `metrics = { TextPaintMetrics(TimelineTypefaces.load(appContext)) }`.
  - The initial `language` comes from `appContext.resources.configuration.locales[0]`.
  - Clock: `graph.planClock`, or `FixedPlanClock(Instant.now())` when the seed `CF_PLAN_TEST_FREEZE_CLOCK=1` is set [F iOS `TripPageDestination.swift`; OQ-1d-2].
  - `onSettings` selects the Settings tab through the 6a shell API (iOS `AppTabs: selection = .settings`).
  - `onLegend` and `onActions` are `null` (6d).
  - The ViewModel is scoped to the Nav3 entry (P9), so popping the entry clears it and releases the lease.
- `app/src/debug/…/TestSeed.kt`:
  - add `CF_PLAN_TEST_FREEZE_CLOCK` (`== "1"` → `freezePlanClock: Boolean`), gated like the other plan keys, with an invalid value going into `ignoredKeys` (4e T11 pattern), if 4e did not add it.
  - `CF_PLAN_TEST_POSITION=summary` → `startAtSummary`.
  - `CF_PLAN_TEST_ROUTE=trip:<id>` already navigates (6a). `:list` is parsed and ignored until 6c.
  - Release builds pass `false` for both.
- **Perf entry:**
  - `app/src/perf/kotlin/net/chronoflow/app/perf/FixtureTripPageSource.kt` replaces `FixtureTimelineSource`:
    - snapshots use the fixture snapshot rule (Task 2), built inside `flow { emit(...) }.flowOn(Dispatchers.Default)`;
    - `preferences` = `flowOf(PlanPreferences())`;
    - `hold` returns a lease from a private `PlanRetention()`;
    - `refresh` is a no-op.
  - `TripPageEntry.content(intent, graph)` builds `TripPageViewModel` with `TripPageArgs(tripId = "fixture-$name")`, `FixtureTripPageSource(name)`, the 1d clock rule (`cf.freezeAt`), `DefaultTripPlanPipeline(PlanDecoder(...), PlanPreparer(...))` as 1d built them, and an empty `SavedStateHandle()`. It renders `TripPage(vm, onBack = {}, onSettings = null)`. The rest of the 1d entry (fixture names, `Log.w` on unknown names, `MainActivity` hook, splash release from `onPlaced`) is unchanged.
  - Rename `FixtureTimelineSourceTest` → `FixtureTripPageSourceTest` (all eight names decode; large60 has 60 days).
  - In `ReleaseReadinessTest`, the class name becomes `net.chronoflow.app.perf.FixtureTripPageSource`.
  - In this commit, set `"timelinePage": "trip"` in `benchmark/budgets.json` (1e Task 3): from here on the history rows carry Page `trip`, and the band and the trend compare only trip-page rows.

**Tests**
- `TripPageViewModelFactoryTest` (Robolectric `@Logic`, debug, the 4e `ScreenSessionFixture` session): creating the ViewModel through the factory with a seeded mock session holds one lease (`retention.withHeldPlans { it == setOf(tripId) }`). After `ViewModelStore.clear()`, it is empty.
- `TripPageSummarySeedTest` (emulator, `app/src/androidTest`, `@Smoke`, < 10 s):
  - launch with the 4e seeds: `CF_AUTH_TEST_ID`, `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_PLAN_TEST_ROUTE=trip:<the active mock trip id from 4c MockAppSeed, ending 0001>`, `CF_PLAN_TEST_POSITION=summary`, `CF_PLAN_TEST_FREEZE_CLOCK=1`, passed the way 4e's seeded tests pass them [verify];
  - wait for `trip.summary` (5 s);
  - assert its top is within 1 dp of the `trip.scroll` top;
  - assert `trip.dayMenu` exists with a description ending "Go to a day";
  - assert `trip.legend` exists and is disabled;
  - press back → My trips is shown (6a tag).

### Task 14: Re-run the timeline benchmarks through the real page

**Commit:** `perf(trip): rerun timeline benchmarks on the trip page`. **Ticks:** "Perf".

**Steps** (the orchestrator runs them if the worker sandbox cannot):
1. `make emu`, then `make perf-timeline` (1e). It runs `TimelineBenchmark` against `cf.trip-page=large60`, unchanged:
   - `trip.page` is still the fling target;
   - `trip.dayMenu` → `trip.dayMenu.day.59`;
   - `topDay()` (min-top `trip.day.*`) ends `-tour-9`;
   - `trip.plan.end` within 6 flings.
   The opening position is the opening day's header, so `topDay()` reads timeline items as before.
2. Gates (plan.md "Performance budget", `benchmark/budgets.json`, 1e):
   - first large60 frame ≤ 800 ms on the emulator;
   - peak RSS anon ≤ 150 MiB;
   - builds on main = 0 and at most 6 placeholders per iteration;
   - the CPU band (P90/P99) against the last three `PASS` rows with Page `trip` and the same Class, Theme and Font: this is the first trip-page run, so there is none yet, and this run starts the baseline;
   - every other metric is reported; list each `FLAGGED` line in the commit body with a one-line cause (it does not fail);
   - `check-benchmarks.py --suite timeline` passes.
3. If a gate fails (the script has already re-run a band miss once):
   - capture a Perfetto trace (1d command, `cf.trip-page=large60`, 15 swipes) and find the cause;
   - likely suspects: a recomposition per frame from a scroll read outside rule 5, or `DayHeader` intrinsic measurement;
   - fix it in this task's commit;
   - **never** widen a budget (AGENTS "Performance").
4. `make baseline-profile`. Commit the regenerated `baseline-prof.txt` and the startup profile; the profile must include `TripPage`, `DayHeader`, `SummaryCard` (`grep -c "feature/trip/page" app/src/main/baseline-prof.txt` > 0 [verify the path]).
5. Add a "Phase 6b" section to `docs/perf-timeline.md`:
   - the metrics table next to the 1e row, with deltas in %;
   - the emulator and image;
   - the trace review: `cf.timeline.build` only on `DefaultDispatch*`, one `cf.timeline.firstFrame` async slice, `cf.timeline.replay` on main;
   - the StrictMode count (0) for the 8 fixtures (1d loop).

**Tests:** `TimelinePerfMarkersTest` (retargeted in Task 11) passes: no `trip.timeline.presented` before the first real replay, and exactly one after two frames.

### Task 15: Board guards and captures

**Commit:** `test(trip): guard the trip page against its boards`. **Ticks:** "Tests"; then confirm every 6b box in plan.md is ticked.

**Files**
- `feature/trip/src/test/…/page/TripPageBoardFixture.kt` (test helper). It hosts `TripPage` with `FakeTripPageSource.fixture("S1")` (S1 is the AMS→HND template, 13 days; the boards draw Amsterdam→Tokyo with 7 days; S3 is AMS→SYD, 1b Task 2), `FakePlanClock` frozen at **day 1 start + 9 h** (board `day: 1` default, `scroll-hour=9`) and Reduce Motion **off**. It prepares one board state:

  | State | Setup |
  |---|---|
  | `Plan` | the opening position (day 1 header at the top) |
  | `PlanScrolled` | `scrollToItem(timelineIndex(day1), (9 × pointsPerHour).dp.roundToPx())`, so the compact header is opaque |
  | `PlanTimelineTop` | clock at day 0 start + 9 h, `startAtSummary = true` |
  | `PlanDayMenu` | `PlanScrolled`, then click `trip.dayMenu` |
  | `PlanEnd` | `scrollToItem(index.end)` (timeline mode; the board's list mode is 6c) |
  | `PlanEndNoText` | `PlanEnd` with the plan's end note removed from the fixture before encoding, so `prepared.endText` is the fallback "There are no more steps in this plan." [verify which 1b input feeds `endText`] |

  The dark boards (`PlanDark`, `PlanScrolledDark`) are `Plan` and `PlanScrolled` with `dark = true`.
- `TripPageBoardGuardTest` (`@Layout`, parameterised, `ProfileRule(profile, language, dark)`):
  - **cases:**
    - {Plan, PlanScrolled, PlanTimelineTop, PlanDayMenu, PlanEnd} × {`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`} × {en, nl}, light;
    - plus {Plan, PlanScrolled} × {`pixel8`, `small`} × {en, nl}, dark;
    - plus PlanEndNoText × {`pixel8`, `small`} × {en, nl}.
  - **Each case** runs `Guards.assertAll(rule, profile, headingTags, exemptTags = blockTags)`:
    - `headingTags` = the `trip.dayHeader.<id>` and `trip.plan.end.title` tags visible in that state;
    - `blockTags` = every `trip.block.<id>` of the fixture (built from the fixture; the 1d rule for invisible overlapping targets);
    - if `assertAll` applies `exemptTags` to touch-target or safe-area rules, run those two separately without exemptions (OQ-1d-5).
  - **Plus designed checks**, cheap, from semantics:
    - `Plan`: `trip.summary` does not exist; `trip.dayHeader.<day1>` top == list top.
    - `PlanScrolled`: the compact header shows "<day1 title>, <date>"; the top bar subtitle is "<day1 title> · <date>".
    - `PlanTimelineTop`: `trip.summary` and `trip.days` are displayed; `trip.days.pill.0` is `selected`.
    - `PlanDayMenu`: one `trip.dayMenu.day.*` item per S1 day (13; the board's 7 is its sample trip), `day.1` is `selected`, and each item height ≥ 52 dp.
    - `PlanEnd`: `trip.plan.end.title` text is "End of this plan" / "Einde van dit plan"; the return button exists when the fixture has `returnDayID`.
    - `PlanEndNoText`: the end text equals the en/nl fallback.
  - Each case < 5 s.
- `TripPageBoardCaptureTest` (`@Capture`, same states, `pixel8`, en and nl, light and dark for the two dark boards): `BoardCapture.capture(rule, "<Board>_pixel8_<lang>_<theme>")`. Examples: `Plan_pixel8_en_light`, `PlanScrolledDark` → `PlanScrolled_pixel8_en_dark`, `PlanDayMenu_pixel8_nl_light`. No stored references (0e T9).

**Run:** `make captures ONLY=Plan,PlanScrolled,PlanTimelineTop,PlanDayMenu,PlanEnd,PlanEndNoText`. Put the PNGs next to the board screenshots for the review (orchestrator).

---

## 7. Acceptance criteria

1. `make quick` passes in < 60 s warm and runs:
   - the JVM tests `PlanPreferencesTest`, `TripPlanObservationTest`, `TripPageViewModelTest`, `TripListIndexTest`, `TripScrollMathTest`, `DualClockValuesTest`, `FixtureTripPageSourceTest`, `PerfClockTest`;
   - the `@Logic` Robolectric tests `SummaryCardTest`, `DayNavigationTest`, `DayHeaderTest`, `HeadingZoneLayoutTest`, `LaneHeadingsTest`, `TripTopBarTest`, `DayMenuScrollTest`, `CompactHeaderTest`, `EndOfPlanCardTest`, `HiddenStepsButtonTest`, `TripPageTest`, `TimelineInvalidationTest` (6 methods), `TimelinePerfMarkersTest`, `TripPageViewModelFactoryTest`.
   - Each class is visible in `build/test-results/quick/…/timings.json`.
2. `make targeted ONLY=":feature:trip:TimelineInvalidationTest"` passes, each method < 5 s.
3. `make check` passes with `pixel` booted. That includes Lint (warnings as errors), Konsist (no raw dp outside tokens and `TripPageMetrics`; `:feature:trip` depends on no feature module), `testReleaseUnitTest` (no fixture class in release), `TripPageBoardGuardTest` (all cases) and `TripPageSummarySeedTest`.
4. `grep -rn "TimelineHostViewModel\|TimelineSource\b\|fun TimelinePage\|stickyHeader" feature app` prints nothing.
5. `make perf-timeline`:
   - first frame ≤ 800 ms;
   - RSS anon ≤ 150 MiB;
   - builds on main = 0, at most 6 placeholders per iteration, and the CPU band (none yet: this run starts the Page `trip` baseline); `FLAGGED` lines are listed in the commit body;
   - results are in `docs/perf-timeline.md` "Phase 6b".
6. `make baseline-profile` is regenerated and committed.
7. `make captures ONLY=…` writes 16 PNGs: `Plan_pixel8_{en,nl}_{light,dark}`, `PlanScrolled_pixel8_{en,nl}_{light,dark}`, and `PlanTimelineTop`, `PlanDayMenu`, `PlanEnd`, `PlanEndNoText` `_pixel8_{en,nl}_light`. They match the boards except for the deltas listed in Appendix C.
8. On `pixel`:
   - a debug build with the seeds of `TripPageSummarySeedTest` opens at the summary;
   - from My trips, opening the active mock trip lands on the opening day's header;
   - back returns to My trips;
   - `adb logcat -d | grep -c StrictMode` prints 0.

---

## 8. Review checklist (Opus reviewer)

**Pipeline and lifecycle**
- [ ] `cf.timeline.firstFrame` begins as the first statement of `TripPageViewModel.init`.
- [ ] The pipeline keeps 1e's `cf.timeline.decode`, `cf.timeline.prepare` and `cf.timeline.typeface` async sections (cookies 1–3, first input only).
- [ ] Decode, layout, prepare, clock projection and dual-clock texts run on `compute`, never on Main. `limitedParallelism(2)` is created once, in the factory.
- [ ] The revision guard has both `mapLatest` and the `seq` check, and the test releases results out of order.
- [ ] `lease.release()` is called in `onCleared`. The ViewModel is scoped to the Nav3 entry, so popping releases it.
- [ ] The anchor goes only to `SavedStateHandle` (three keys). Nothing is written to disk.
- [ ] `select` changes `anchor` only on a day change. Offsets are not observed.
- [ ] Scroll commands are sent only for jumps and for day-id changes or a missing anchor day, not for every prepare.
- [ ] Every `catch` rethrows `CancellationException`.

**Composition and performance**
- [ ] No item composable reads `listState`. The compact alpha is read inside `graphicsLayer {}`. `visible`, `headerScrolled` and `current` are `derivedStateOf` values that flip only at boundaries.
- [ ] `DualClocks` is the only reader of `SummaryClockState`, and `NowLine` the only reader of `DayClock`.
- [ ] `LazyListState` is created inside the Ready branch with the opening index, and there is no scroll animation on open.
- [ ] Item keys are `summary`, `days`, `h:<id>`, `t:<id>`, `end`, `hidden`, and contentTypes as in Task 11. No `stickyHeader`.
- [ ] The 1d/1e tags are unchanged. `trip.day.<id>` is on the timeline item.
- [ ] The Phase 6b perf table is present, no budget is widened, and the baseline profile is regenerated.

**Design fidelity**
- [ ] Every dp value comes from `PlanMetrics`, a theme token or `TripPageMetrics` (whose KDoc names `Plan`/`PlanDayMenu`). Targets use `minimumInteractiveComponentSize`.
- [ ] Rule 1 is applied:
  - iOS explicit metrics: PlanCard 16/26 without shadow, zone chip, end card, dual clocks, pills;
  - board looks for system controls: warnings and notes chips, Show more, legend circle, menu items, return button.
  - Every delta is in Appendix C.
- [ ] iOS copy is used: "End of this plan", "Go to a day", the eyebrow "Your plan" not uppercased.
- [ ] Unavailable callbacks disable their controls: legend, trip actions, hidden steps.
- [ ] No screen without a board was invented: there is no hidden-steps design beyond the primitive, and no non-ready state UI.

**Copy and accessibility**
- [ ] Every string is a resource found by its `<!-- plan: … -->` comment. Plurals use `pluralStringResource`. The only new untranslatable resource is `plan_title_comma_date`.
- [ ] Headings: day headers and the end title. Selected: pills and menu items. The compact header is hidden unless fully opaque. Lane headings inside items are hidden.
- [ ] The title's description switches between the 3- and 4-argument forms. Its `stateDescription` is "title · date".

**Tests**
- [ ] Each test is at the cheapest tier, has a name as listed, and stays within its time budget. There is no `Thread.sleep`.
- [ ] The invalidation test has 6 methods with the added kinds. The guards cover every listed board state × profile × language.
- [ ] The removed 1d tests are replaced, not just deleted.
- [ ] Commits follow Conventional Commits with lower-case imperative subjects and no trailers. There is one commit per task.

---

## 9. Open questions (proceed on the default; the orchestrator adds them to plan.md "Open questions for the user")

| # | Question | Default |
|---|---|---|
| OQ-6b-1 | plan.md says "designed sticky DayHeader". The board pins the full glass header (`position: sticky; top: 54px`), while iOS scrolls it away and fades in the compact lane line, and research 04 row J rejects `stickyHeader`. | iOS: header as a list item, compact overlay fading over 24 dp. |
| OQ-6b-2 | Summary card shadow: board `shadow-soft`, iOS `PlanCard` none. | None (iOS explicit). |
| OQ-6b-3 | "Show more": board 28 px text-only; iOS a full-width touch-height button with a chevron. | Board look (text only, muted, 12 px) with a 48 dp target. |
| OQ-6b-4 | Board shows warnings while collapsed; iOS `shownNotices` shows notices only when expanded. | iOS. |
| OQ-6b-5 | The pull-to-refresh capsule on the trip page: 04 §9.5 puts `PullToRefreshBox` here, the 6b plan entry does not mention it. | Include `CfPullToRefreshBox` calling `refreshNow`. Reuse 6a's capsule only if it is an exported composable; otherwise leave it out and report it. |
| OQ-6b-6 | The hidden-steps notice has no board. | Built from iOS as a ghost `CfButton`; it opens the Settings tab. |
| OQ-6b-7 | Back control: board chevron in a glass circle labelled "My trips"; M3 `arrow_back`. | `CfBackButton` (`arrow_back`) with the description App "My trips". |
| OQ-6b-8 | Anchor offset granularity: iOS keeps the offset within the whole row (header + timeline); Android stores it within the timeline item. | Timeline item. A partly scrolled header restores to the header top. |
| OQ-6b-9 | Replacement scrolling: iOS scrolls on every revision change; Android relies on stable keys. | Scroll only when the day-id list changes or the anchor day is gone. |
| OQ-6b-10 | Focus after a jump (04 OQ-T24 says yes). | Deferred to 6d (a11y and keyboard). |
| OQ-6b-11 | Wide windows (≥ 600 dp) before 6e. | The phone layout, full width. |
| OQ-6b-12 | Board captures for `PlanEnd`/`PlanEndNoText` are list mode (`view=list`). | Capture timeline mode in 6b; 6c adds the list-mode captures. |
| OQ-6b-13 | Day menu title: board "Go to day", iOS "Go to a day". | iOS copy, board style. |
| OQ-6b-14 | Prev/next day buttons: board DayStrip has none; iOS has them. | Keep them (04 §3.20). |
| OQ-6b-15 | Who owns the on-open detail fetch, `awaitingSync` and `syncCompleted`: an earlier draft of this brief built them in 6b, while the 6c brief ports them as `TripPageFetchModel` and wires them into 6b's ViewModel (6c Tasks 1, 3), and plan.md lists the remote/fetch states under 6c. | 6c. 6b resolves the state with `online = true`, `fetch = Idle`, no `awaitingSync`/`missingAfterSync`; a trip whose plan is not stored stays on the 6c placeholder until 6c lands. |
| OQ-6b-16 | plan.md 6b says the anchor goes into `SavedStateHandle` "for configuration changes only"; A8 handles size changes in place and makes process death a cold launch. | `SavedStateHandle` only; it matters for locale and font-scale recreation. No disk. |
| OQ-6b-17 | Title text: board "Amsterdam → Tokyo" (cities); iOS at the pin shows airport codes `AMS → HND` on iOS 26 (`codeTitle`, TripPageView l.285–291, screenshot `06-trip-summary.png`) and `prepared.title` before iOS 26. | iOS 26 codes through the catalog key `%@ → %@`; delta C14. |

---

## Appendix A: Copy (iOS keys; find each resource by its `<!-- plan: KEY -->` comment)

| iOS key | en | nl | Task |
|---|---|---|---|
| `Your plan` | Your plan | Je plan | 5 |
| `Legend: what the colours and patterns mean` | Legend: what the colours and patterns mean | Legenda: wat de kleuren en patronen betekenen | 5 |
| `Plan summary` | Plan summary | Plansamenvatting | 5 |
| `Home · %@` | Home · %1$s | Thuis · %1$s | 5 |
| `There · %@` | There · %1$s | Daar · %1$s | 5 |
| `You’re here · %@` | You’re here · %1$s | Je bent hier · %1$s | 5 |
| `Home time · %@` | Home time · %1$s | Thuistijd · %1$s | 5 |
| `Destination time · %@` | Destination time · %1$s | Tijd op bestemming · %1$s | 5 |
| `Previous day` | Previous day | Vorige dag | 6 |
| `Next day` | Next day | Volgende dag | 6 |
| `Plan days` | Plan days | Plandagen | 6 |
| `%lld warnings` (plural) | one: %d warning · other: %d warnings | one: %d waarschuwing · other: %d waarschuwingen | 7 |
| `%lld notes` (plural) | one: %d note · other: %d notes | one: %d notitie · other: %d notities | 7 |
| `Show more` / `Show less` | Show more / Show less | Meer tonen / Minder tonen | 7 |
| `Sleep` / `Light` / `Caffeine` / `Travel` | Sleep / Light / Caffeine / Travel | Slapen / Licht / Cafeïne / Reizen | 7 |
| `%@ → %@` | %1$s → %2$s | %1$s → %2$s | 8 |
| `Loading your plan` | Loading your plan | Je plan laden | 8, 11 |
| `Go to a day` | Go to a day | Ga naar een dag | 8 |
| `%@ to %@, %@. Go to a day` | %1$s to %2$s, %3$s. Go to a day | %1$s naar %2$s, %3$s. Ga naar een dag | 8 |
| `%@ to %@, %@, %@. Go to a day` | %1$s to %2$s, %3$s, %4$s. Go to a day | %1$s naar %2$s, %3$s, %4$s. Ga naar een dag | 8 |
| `Trip actions` | Trip actions | Reisacties | 8 |
| App `My trips` | My trips | Mijn reizen | 8 |
| `End of this plan` | End of this plan | Einde van dit plan | 10 |
| `See your return plan` | See your return plan | Bekijk je terugreisplan | 10 |
| `Some steps are hidden in your preferences.` | Some steps are hidden in your preferences. | Sommige stappen zijn verborgen in je voorkeuren. | 10 |
| (untranslatable, 1d) `plan_day_title_date` | %1$s · %2$s | (same) | 7, 8 |
| (untranslatable, new) `plan_title_comma_date` | %1$s, %2$s | (same) | 6, 7 |

Not used in 6b (6c/6d): `Show as list`, `Show as timeline`, `Legend`, `Done`, `Steps`, `Plan timeline`. `Now, %@` is already used by 1d's `NowLine`.

## Appendix B: iOS quoted (at `f391a1fa`)

`PlanUI/TripPage/TripScrollTracker.swift:126–134`:
```swift
func updateChrome(dayID: String, reduceMotion: Bool) {
    let remaining = frames["header." + dayID]?.maxY ?? .infinity
    opacity = reduceMotion ? (remaining <= 0 ? 1 : 0) : min(1, max(0, 1 - remaining / PlanMetrics.compactFadeDistance))
    chrome?.alpha = opacity
    chrome?.accessibilityElementsHidden = opacity < 1
}
```

`PlanUI/TripPage/TripPageView.swift:187–193` (current day):
```swift
if let current = model.plan.prepared?.days.last(where: {
    (value[$0.id]?.minY ?? .infinity) <= compactHeight
}), let frame = value[current.id] {
    model.select(dayID: current.id, offset: max(0, -frame.minY))
}
let id = model.anchor.dayID ?? ""
let scrolled = (value[id]?.minY ?? .infinity) < 0
```

`PlanUI/TripPage/DayHeader.swift` (structure):
```swift
VStack(alignment: .leading, spacing: PlanMetrics.smallGap) {
    ViewThatFits(in: .horizontal) {
        HStack(alignment: .firstTextBaseline) { heading; Spacer(minLength: PlanMetrics.smallGap); zone }
        VStack(alignment: .leading, spacing: PlanMetrics.smallGap) { heading; zone }
    }
    if day.warningCount > 0 { Button { expanded.toggle() } label: { Label(copy.count("%lld warnings", day.warningCount), systemImage: "exclamationmark.triangle") }.tint(ColorToken.dawnText.color) }
    if expanded {
        if let status = day.status { Text(verbatim: status).font(Typography.authStrong.font).foregroundStyle(ColorToken.dawnText.color) }
        Text(verbatim: day.summary).font(Typography.planSubtitle.font).monospacedDigit()
        Text(verbatim: day.progress).font(Typography.planSubtitle.font)
        if let headline = day.headline { Text(verbatim: headline).font(Typography.planSubtitle.font).foregroundStyle(ColorToken.dawnText.color) }
        if day.warningCount == 0 && !day.notices.isEmpty { Button(copy.count("%lld notes", day.notices.count)) { notesExpanded.toggle() }.font(Typography.authMeta.font) }
        ForEach(day.shownNotices(expanded: expanded, notesOpen: notesExpanded)) { DayNotice(notice: $0) }
    }
    Button(copy(expanded ? "Show less" : "Show more"), systemImage: expanded ? "chevron.up" : "chevron.down") { expanded.toggle() }
        .font(Typography.planSubtitle.font).frame(maxWidth: .infinity, minHeight: PlanMetrics.touch)
}.foregroundStyle(ColorToken.foreground.color).padding(.vertical, PlanMetrics.rowGap).background(ColorToken.background.color)
```

`PlanUI/TripPage/TripPageView.swift:143–145`:
```swift
if !model.plan.hiddenCategories.isEmpty {
    Button(copy("Some steps are hidden in your preferences.")) { onSettings?() }.disabled(onSettings == nil)
}
```

## Appendix C: Board vs iOS deltas (record in the PR body; the reviewer checks them against the captures)

| # | Element | Board | Android (source) |
|---|---|---|---|
| C1 | Summary card | padding 16, radius 26, `shadow-soft` | padding 16, radius 26, border, **no shadow** (iOS `PlanCard`) |
| C2 | Eyebrow "Your plan" | uppercase, 600, 0.1em | `eyebrow` token, not uppercased (iOS) |
| C3 | Clock cells | radius 18, padding 10×12 | radius 22, padding 12, active border 2 dp dawn (iOS) |
| C4 | Zone chip | 28 high, padding 0 10, 12/16 500, muted-foreground | padding 4, `planSubtitle`, `muted` fill (iOS) |
| C5 | Show more | 28 high, text only | board look with a 48 dp target (system control → board) |
| C6 | Lane dots | 8 px, lanes row inside the sticky header | 6 dp (`PlanMetrics.dot`); lanes above each timeline and in the compact header (iOS) |
| C7 | Warnings while collapsed | notices visible | hidden until expanded (iOS `shownNotices`) |
| C8 | Compact line | glass, 0.5 hairline, sticky | opaque `card`, no hairline, overlay fading over 24 dp (iOS, 04 OQ-T13) |
| C9 | End card | padding 20, 48 circle, "That's the end of this plan", Fraunces 24/32, text 14/20 | padding 16, 44 circle, "End of this plan" in `planCardTitle`, text `authBody` (iOS) |
| C10 | Day header | sticky glass, StatusPill, progress bar | list item on `background`; status, summary, progress as text (iOS) |
| C11 | Day strip | pills only | prev/next chevrons + pills (iOS, 04 §3.20) |
| C12 | Tab bar | minimises on scroll | never hides (plan) |
| C13 | Menu title | "Go to day" | "Go to a day" (iOS copy), board style |
| C14 | Title | "Amsterdam → Tokyo" | "AMS → HND" (iOS 26 `codeTitle`, OQ-6b-17) |
| C15 | Menu title padding | 8 / 16 / 6 | 8 / 16 / 4 (`smallGap`; 6 has no token) |
