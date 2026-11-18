# Phase 4e: Session controller, app graph, gate and routing

- **Branch:** `feat/phase-4e-session`, from the stack top once 4d and 3b are in it. 4a (store, Keystore session, wipe marker), 4b (DataSource), 4c (mock and `MockAppSeed`) and 0e (seeds, `ScreenTest`, `ReleaseReadinessTest`) must also be there. If one is missing, stop and report it.
- **Refreshed 2026-11-18** against the landed 0e, 3b–3c and 4a–4d code (4d OQ-4d-14). Earlier phases are already in the stack. Work in the main checkout, one task at a time; waves and worktrees do not apply. §2.5 records the existing APIs. **Do not re-create anything listed there.**
- **Checked 2026-11-16** against iOS at `f391a1fa`, web at `e36368ec` and the upstream briefs: the controller follows 4d's actor mapping (no `Mutex`, §5.2), the missing Swift test cases are now listed, `LaunchRoute.toRoute()` moved to `:app`, the auth-test seed reaches the mock as `seed`, and Robolectric tests use a software cipher (OQ-4e-15…17).
- **Plan entry:** `plan.md` "Phase 4e: Session controller, app graph, gate and routing" (heading; lines 1873–1938 at refresh). Decisions **A3** (l.90), **A4** (l.91), **A5** (l.92), **A7** (l.94), **A8** (l.95), **A19** (l.106), **A21** (l.108), **A23** (l.110). Spec sections "process, activity and launch sequence" (l.321–346), "storage layout" (l.385–398), "navigation and routes" (l.400–405) and "seeds and test hooks" (heading; lines 466–476 at refresh). The "Performance budget" row "Session known, signed out" (l.516).
- **Tests:** follow `AGENTS.md` "Tests": cheapest tier first, the budgets, no `Thread.sleep`/`SystemClock.sleep`, `runTest(timeout = 60.seconds)`, and ported tests keep the Swift names (camelCase, same words). Read that section before you write a test.
- **Tags:**
  - **[F]** is a fact verified in the source at the path given. **[R]** is a rule this brief sets for Android.
  - `I/` means `/Users/rickgout/Sites/chronoflow-ios`, read at `f391a1fa` with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. **Never write, build or run anything in it.**
  - `Kit/` means `I/Packages/ChronoFlowKit/Sources`, and `KitT/` means `I/Packages/ChronoFlowKit/Tests`.
- **Path shorthands:**
  - `…/sync/` = `core/sync/src/main/kotlin/net/chronoflow/core/sync/`
  - `…/data/` = `core/data/src/main/kotlin/net/chronoflow/core/data/`
  - `…/model/` = `core/model/src/main/kotlin/net/chronoflow/core/model/`
  - `…/nav/` = `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/`
  - `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`
  - `…/debug/` = `app/src/debug/kotlin/net/chronoflow/debug/`
- **Secrets rule (AGENTS, A23):** passwords, tokens, two-factor codes and link secrets live only in memory and in the Keystore-encrypted `session.bin`. They never go into a `Bundle`, `SavedStateHandle`, `rememberSaveable`, the database, the outbox, a log or a **Nav3 key** (see T8).

---

## 1. Goal

Today, launch ends on a stub (3b), and nothing owns the signed-in session. After this phase, the app has a real session lifecycle, ported from iOS Phase 9d.

The `SessionController` restores, accepts and signs out with the iOS order and generation rules:
- A stale accept can never write credentials.
- An old 401 can never wipe a newer session (D23).
- A voluntary sign-out with pending changes returns `NeedsConfirmation(n)`.
- A forced 401 wipes in the exact iOS teardown order. Logout is bounded at 3 s, and the wipe marker is retried at the next start.

The other pieces:
- `AppGraph` (process-wide, lazy) builds one `SessionGraph` per epoch, and wires 4d's `SessionSync` (adding `requestStart`) and 4d's `AccountModel`.
- `ProcessLifecycleOwner` starts sync on ON_START and only disables it on ON_STOP (iOS `setActive(false)`: no flush, no source call).
- `StoredDataReadiness` replaces 3b's default `SignedOutReadiness` in debug, perf and release (OQ-4e-2; 3b's `StubReadiness` still wins when a `cf.session|gate|stored|readyDelay` extra is present), and emits the `session` trace.
- `SessionGateRouter` keeps routing verify → terms → walkthrough → app after launch.
- `RootNavigator` exposes `StateFlow<RootState>`, and `NavDisplay` (inside 3b's `CfRoot`) renders the root routes. Routes without a screen show 3b's `RoutePlaceholder`, which has text only in debug.
- `PendingLaunchIntent` holds App Link and notification intents until launch ends.
- `TestSeeds.apply` can seed a signed-in account through the real restore path. A reseed equals a cold seed.
- Release ships no seed or mock code.

**Done means:**
- The ported iOS session, sign-out, wiper, readiness and gate cases pass on the JVM.
- A Robolectric launch test per gate state lands on the right route.
- One emulator smoke test, a seeded signed-in cold launch, reaches `route.today`.
- A cold launch with no session lands on Welcome.
- `testReleaseUnitTest` and `verifyReleaseManifest` prove that no seed or mock code ships.
- The `session` trace number is in `docs/perf-startup.md`.

## 2. Inputs to read (in this order)

### 2.1 This repo
1. `AGENTS.md`: "Tests", "Traps" (rethrow `CancellationException`; `native` is a Java keyword; release ships no seed or mock code; config cache reads through `providers`).
2. `plan.md`: the lines in the header, plus Phases 3b (heading; lines 1583–1634 at refresh), 4a–4d (headings; lines 1690–1872 at refresh), 5a (sign-in calls `accept`), 6a (tab shell), 8 (walkthrough POST 401), 10a (sign-out sheet) and 12b (the reminder hook). Those later phases consume this one's API.
3. `docs/phases/phase-0e-test-harness.md` T11 (`TestSeed`, `TestSeeds`, `SeedReceiverActivity`, `ScreenTest`), T13 (`ReleaseReadinessTest`), T15–T16 (perf variants, `StartupBenchmark`).
4. `docs/phases/phase-3a-launch-overlay.md` (`LaunchTrace`, `LaunchOverrides`) and `phase-3b-launch-readiness.md` §5.1 (seams), §5.2 (`LaunchState.route`, `LaunchRoute`), §5.7 (`LaunchController`, `readinessFactory`), §5.8 (`CfRoot`, `RoutePlaceholder`, `DebugRouteLabel`), §5.9 (stub selection), the trace cookie line ("Cookies 5 (`session`, 4e)") and OQ-3b-8, -9, -13. Then the merged code: `grep -rn "StubReadiness\|LaunchReadiness\|LaunchController\|RoutePlaceholder\|DebugRouteLabel" --include=*.kt`.
5. The upstream hand-offs (binding names, summarised in §2.5): `phase-4a-store.md` §4.2 and §9 "Hand-off" (4e bullet), Tasks 1, 4, 7, 8; `phase-4b-api-client.md` §9 "Hand-off" (4e bullet), `DataSourceFactory`, OQ-4b-9; `phase-4c-mock-data-source.md` `MockDataSource` constructor, `MockAppSeed.make`, OQ-4c-4, OQ-4c-13; `phase-4d-sync.md` §4, §5.1, §5.2 (`ActiveSyncDatabase`), Tasks 3b (`AccountModel`), 7 (`SyncLifecycle`, `SessionSync`), 8 (`LoginUpsert`), 9 (`ConnectivitySyncPathMonitor`), OQ-4d-12, -13, -14.
6. Research:
   - `docs/research/02-ios-architecture.md` §4.3–§4.6, §4.11, §8–§11;
   - `docs/research/14-gaps.md` §3 (process death, survival table);
   - `docs/research/13-testing-tooling.md` §3 and §5 (§5.2 steps 1–7).

### 2.2 iOS sources to mirror (`I/` at `f391a1fa`, read-only)
| What | Path | Key lines |
|---|---|---|
| Controller, value types | `Kit/AppCore/SessionController.swift` | types 3–103; actor state 105–137; `activeSession` 138; `updates` 149; `restore` 160; `accept` 177; `confirmEmailVerification` 194; `signOut` 222/226; `unauthorized` 232; `retryUnauthorized` 240; `enqueueSignOut` 249; `performSignOut` 275–325; `bestEffortLogout` 327–335; `check` 337; `performRestore` 342–388; `performAccept` 390–423; `publish` 425–437 |
| Sign-out result, teardown seam | `Kit/AppCore/SignOutResult.swift` | whole file (17 lines) |
| Gate router | `Kit/AppCore/SessionGateRouter.swift` | whole file (70 lines) |
| Readiness | `Kit/AppCore/StoredDataReadiness.swift` | whole file (218 lines); `App/Readiness/StoredDataReadiness.swift` is only a typealias |
| Launch readiness protocol, `Gate.route` | `Kit/AppCore/LaunchReadiness.swift` | 9–19 |
| Pending intent | `Kit/AppCore/PendingLaunchIntent.swift` | whole file |
| Routes | `Kit/AppCore/Route.swift`; `App/RootView.swift` (`RootNavigator`, routed switch) | |
| Wiper | `Kit/LocalStore/AccountWiper.swift` (63 lines); `App/Session/AccountWiper.swift` is only a typealias | |
| Account DB seam | `Kit/LocalStore/SessionAccountDatabase.swift` | |
| Gate from DB | `Kit/LocalStore/AccountReadinessRepository.swift` | |
| App wiring | `App/Session/NativeSessionController.swift` (245 lines); `App/Session/AppServices.swift`; `App/SceneDelegate.swift`; `App/AppDelegate.swift` (`verificationRequired`) | |
| Session sync | `Kit/Sync/SessionSync.swift` **l.41–44 only** (`requestStart`); the rest of the file, `SyncLifecycle.swift` and the path monitor are already ported by 4d | |
| Seeds | `Kit/DataAccess/AuthUITestConfiguration.swift`; `Kit/Sync/MockSessionSeed.swift`; `App/Debug/UITestSessionFixture.swift`; `App/Debug/RoutePlaceholder.swift`; `App/AppTabs.swift` (seed consumption) | |
| Stub | `App/Readiness/StubReadiness.swift`; `App/LaunchArguments.swift` | |
| Reminder tap shape | `Kit/Reminders/ReminderTap.swift` | |
| iOS plan | `I/plan.md:1134` (Phase 9d), `:311` (Core seams), `:25` (D17, D21, D23, D25, D26), `:2376` (OQ 7, 8, 16, 29, 30) | |

Tests to port (keep the names):
- `KitT/AppCoreTests/SessionControllerTests.swift`, `SignOutTests.swift` and `StoredDataReadinessTests.swift`;
- `KitT/LocalStoreTests/AccountWiperTests.swift`, `AccountReadinessTests.swift` and `SessionAccountDatabaseTests.swift`;
- `KitT/SyncTests/MockSessionSeedTests.swift`, `SessionSyncReadinessTests.swift` and `SessionWipeTests.swift` (4d OQ-4d-6 moved them here; `SyncTriggerTests` is 4d's);
- `I/AppTests/SessionLifecycleTests.swift` and `LaunchConnectionTests.swift`;
- `I/ReleaseReadinessTests/ReleaseReadinessTests.swift`.

`KitT/RemindersTests/SignOutTests.swift` belongs to 12b.

### 2.3 Facts verified while writing this brief [F]
- Plan 4e cites `App/Session/AccountWiper.swift` and `App/Readiness/StoredDataReadiness.swift`. Both are one-line typealiases. The real code is `Kit/LocalStore/AccountWiper.swift` and `Kit/AppCore/StoredDataReadiness.swift`.
- The plan now has ten teardown steps under Phase 4e. The complete authoritative order is §5.3, from `SessionController.swift:275–325`; the plan preserves its relative order.
- On iOS, `prepare` runs on every restore and every accept: it closes the DB, then runs `wiper.recover()` (`NativeSessionController.swift:79–82`). That is how the wipe marker is retried at the next start.
- The rest of the iOS teardown wiring (`NativeSessionController.swift:50–110`):
  - `pendingCount`: `accounts.activeDatabase()`, or throw `StoreFailure.closed`; then `OutboxRepository(database).pending().count` (l.83–86);
  - `flush`: `sync?.refreshNow()`; `logout`: `source(session).signOut()` only when a source exists (l.87–93);
  - `detach`: `model.detach()` (the `AccountModel`), then the `signedOut` callback, which navigates (l.94–97);
  - `stopSync`: `reminders.detach()`, then `sync?.stop()`; `requestSync(epoch)`: `sync?.requestStart(controller, epoch)` (l.100–109);
  - `SessionAccountDatabase` gets a detach closure: clear reminder taps, `reminders.detach()`, `model.detach()` (l.50–54);
  - `SessionSync.isCurrent` compares both the session and the epoch with `controller.activeSession()`; its `unauthorized` callback calls `controller.unauthorized(epoch)` and swallows errors (`try?`) (l.61–71).
- `setActive(true)` order (l.152–160): `sync.setEnabled(true)`, `guestWizardStore.recover()`, `reminders.reschedule()`, `controller.retryUnauthorized()` (an error stops here), `readiness.retry()`, `sync.resume(current)`.
- `observe()` (l.186–218; the update loop is l.196–217): for each `SessionUpdate`, if `controller.epoch == update.epoch && controller.presence == update.presence` and the update is `signedIn(id)` with an open database whose id matches, attach reminders and then `model.attach(AccountRepository(database), epoch, ifCurrent: token)`; otherwise detach the model if the token is unchanged.
- `MockAppSeed.make(now:unverified:walkthroughSeen:termsPending:)` (`Kit/DataAccess/Mock/MockAppSeed.swift:16–17`) applies the gate seeds:
  - `unverified` sets `hasVerifiedEmail = false`;
  - `termsPending` makes the accepted terms version older;
  - `walkthroughSeen` sets the server flag.

  `AppServices.swift:120–123` feeds it from `CF_AUTH_TEST_UNVERIFIED`, `CF_AUTH_TEST_WALKTHROUGH_SEEN` and `CF_AUTH_TEST_TERMS_PENDING`, and the result is passed to the mock as its **`seed:`** argument (`MockDataSource(store:, seed:, …)`; `MockSessionSeedTests.swift:21–26` does the same). It is not written to `state.json` up front: the mock uses `restored ?? seed` on first load (4c `load()`).
- iOS `SceneDelegate.runReadiness` (l.426–451) captures `navigationRevision = navigator.revision` once, then **on every coordinator callback whose `state.route` is non-nil**: `if navigator.revision == navigationRevision { navigator.navigate(to: route); navigationRevision = navigator.revision }`. The launch therefore follows route changes (for example a gate that resolves later) until someone else navigates, and then never navigates again.
- iOS begins the `session` signpost with `launch`, `first-commit` and `ready` at scene connect (`SceneDelegate.swift:103–107`) and ends it in the coordinator callback the first time `state.session != nil` (l.437–438).
- iOS Perf builds set `CHRONOFLOW_READINESS = signed-out` and so use `SignedOutReadiness` (`AppServices.swift:96–100`; `ReleaseReadinessTests.testReleaseIgnoresArgumentsRestoresNativeSessionAndKeepsPerfIsolated`). Android departs from this on purpose (OQ-4e-2).
- `SessionController` is a Swift **actor**: it is re-entrant at every `await`. Callbacks it awaits while a `tail` task is running (`stopSync`, `requestSync` → `SessionSync.requestStart` → `controller.activeSession()`; `SessionSync.isCurrent`) call back into it, and the tests read `signOutFlight`, `generation` and `pendingUnauthorizedEpoch` while an `accept` is suspended inside its credential write (`SignOutTests.explicitSignOutUpgradesQueuedEpochScopedFlight`). A lock held across a suspension deadlocks all of these.
- `SessionGateRouter.start()` returns at once if it is already running (`guard sessionTask == nil`, l.22). Inside the gate task, a value whose `activeSession() != snapshot` **ends** that gate task (`return`, l.43–45); it does not skip one value.
- `StoredDataReadiness.routeAcceptedSession` runs its body in an unstructured `Task` so that cancelling the request does not stop the routing (l.71–81: "The committed session owns routing, including verification reads, after its request is canceled.").
- `AccountReadinessRepository.values(accountID:)` first validates the id and throws `StoreFailure.invalidAccountID` when the open database belongs to another account (l.9–10).
- Four `StoredDataReadinessTests` drive a real `LaunchCoordinator` with a `ManualLaunchClock`: `freshDataKeepsTwoAndEightSecondRules`, `verificationIsReadyWithoutMarkerAndSignedOutDoesNotReadDatabase`, `unknownSessionUsesExistingEightSecondFallbackWithoutDeletingData` and `verificationDiscoveredDuringFirstPullReleasesLaunchWithoutMarker`. On Android those types live in `:feature:launch` (3b), which `:core:sync` cannot see.
- `MockSessionSeed.run` skips when `<root>/signed-in-seed.done` exists. The code comment: "A completed fixture must never resurrect a session after sign-out on relaunch" (`MockSessionSeed.swift`).
- The logout endpoint is `POST auth/logout` with a 3 s timeout (`Kit/DataAccess/API/APIDataSource.swift:60`). The controller adds its own 3 s race on top of that.
- There is no 4e board. The related boards belong to later phases: `SignOutPending` and `SignOutPendingSettings` (10a), `TermsUpdate` and `TermsFailed` (8), `AuthVerify` (5d), `TodayLoading` (7) and `TripsLoading` (6a).

### 2.4 API endpoints this phase calls (all through the 4b `DataSource`)
| Call | Endpoint | Where |
|---|---|---|
| `signOut()` | `POST auth/logout`, 204, 3 s timeout | Teardown step "logout" (voluntary only) |
| `signIn(input)` | `POST auth/login` → `SignInResult.Session` or `.TwoFactor(challengeId)` | Debug `MockSessionSeed` only |
| `completeTwoFactor(input)` | `POST auth/two-factor`, body `challengeId` + `code` | Debug `MockSessionSeed` only |
| Full pull | `GET users/me` → `GET account` → `GET trips` → details (4d `FullPull`) | Seed step 4; `SessionSync` start |

`SessionResult` JSON (`SessionController.swift:41–70`): `{"kind":"session","token":"…","account":{…},"tripId":"…"?}`. 0d already decodes it with a custom serializer (`kind == "session"`, non-blank token, `AccountID.of(account.id)`), and 4b maps a decode failure on a 200 to `DataFailure.Server(200)`. 4e adds no decoding.

### 2.5 Names this phase builds on (from the final upstream briefs; `grep` each before T1)
| Name | Owner, package | Shape |
|---|---|---|
| `AccountID` | 0d, `net.chronoflow.core.model` | `value class`, `AccountID.of(s)` (throws `AccountID.InvalidUUID`), `rawValue` lower-case |
| `SessionAccount` (`typealias AccountIdentity = SessionAccount`), `AccountPolicies`, `SessionResult`, `SignInResult` | 0d, `net.chronoflow.core.model.api` (`SessionContracts.kt`, `AuthContracts.kt`) | `SessionResult.toString()` redacts `token`; `StoredSession.toString()` uses `<redacted>` |
| `DataSource` (25 methods incl. `signOut()`, `signIn`, `completeTwoFactor`), `DataFailure` | 0d/4b, `net.chronoflow.core.model` | |
| `StorageNamespace(directoryName, keychainService)` (`Api("ChronoFlow", …)`, `Mock("ChronoFlowMock", …)`, `sessionKeyAlias`), `StoredSession` (+ `parse`), `SessionStore` (+ `readStoredSession()`, `loadSession()`), `SessionPresence`, `SyncEpoch(accountID, generation)`, `SessionMutationPermit` (public `invalidate()`), `StaleSessionException` | 4a, `net.chronoflow.core.model.session` | |
| `StoreFailure` (`Closed`, `AlreadyOpen`, `InvalidAccountID`, `Unavailable(cause?)`, `Migration(cause?)`, `StorageAttributes(cause?)`) | 4d Task 0 moved it to `net.chronoflow.core.model.store` | |
| `AccountDatabase(root, namespace, driverFactory: AccountDriverFactory, io)` with `accountID`, `open(accountID: String)`, `close()`, `read`, `write`, `observe(vararg tables) { }`; `AndroidAccountDriverFactory(context)` | 4a, `net.chronoflow.core.database` | |
| `KeystoreSessionStore(file, alias, cipher, io)`, `KeystoreSessionStore.forNamespace(root, ns, cipher, io)`, `AndroidKeystoreSessionCipher`, `WipeMarker(namespaceRoot, io)` (`file`, `write(id)`, `read(): AccountID?`, `clear()`, `CorruptWipeMarkerException`), `DevicePreferences.shared` | 4a, `net.chronoflow.core.datastore` | |
| `JdbcAccountDriverFactory`, `TestRoots` | 4d Task 0, `:core:database-testing` (test-only) | |
| `ApiClient(baseUrl, bearer, io)`, `ApiDataSource(client)`, `DataSourceFactory(setting, baseUrl, release, testing)` (`.source`, `.baseUrl`) | 4b, `net.chronoflow.core.network` | `:app` main never imports OkHttp |
| `MockDataSource(store: MockStateStore, seed: MockServerState? = null, …, delay: () -> Double /* seconds */, sleep, …, bearer, …, debugAppSeed: Boolean = true, zone, io)`, `MemoryMockStateStore`, `FileMockStateStore(root)` (`<root>/ChronoFlowMock/Server/state.json`), `MockAppSeed.make(now, unverified, walkthroughSeen, termsPending, includeTimelinePerformanceTrips, zone): MockServerState`, `MockScenarioValues` | 4c, `net.chronoflow.core.datamock` (debug + perf only) | |
| `DataSourceFactory.Source` (`Mock`, `Api`), `DataSourceFactory.make(mock, api)` | 4b, `net.chronoflow.core.network` | nested enum: write `DataSourceFactory.Source.Api` |
| `AccountModel(scope: CoroutineScope)` (`state: StateFlow<LoadState>`, `LoadState.Unknown/Loaded/Failed`) | 4d, `net.chronoflow.core.data.repository` | production scope dispatcher `Dispatchers.Main.immediate` (4d) |
| `SoftwareSessionCipher` (AES-GCM fake; Robolectric has no AndroidKeyStore provider) | 4a, `core/datastore/src/test` (not visible to other modules) | 4e copies it, see T9 |
| `SyncDatabase`, `fun interface ActiveSyncDatabase { suspend fun activeDatabase(): SyncDatabase? }`, `SyncScheduler(now, sleep)`, `SyncLifecycle`, `SyncRetryState`, `SessionSync(accounts, scheduler, makePath, source, isCurrent, unauthorized, verification, dispatcher)` (`start`, `resume`, `setEnabled`, `requestSync`, `refreshNow`, `stop`, `events()`), `SyncResult`, `SyncEvent`, `FullPull`, `SyncPathMonitoring` | 4d, `net.chronoflow.core.sync` | |
| `SqlSyncDatabase(database)`, `ConnectivitySyncPathMonitor(context)`, `SyncScheduler.Companion.system()` | 4d, `net.chronoflow.core.data.sync` | `system()` is an extension; import it explicitly |
| `AccountRepository`, `TripRepository`, `SettingsRepository`, `OutboxRepository` (`pending()`), `AccountModel` (`attach(repository, epoch, ifCurrent)`, `detach()`, `attachmentToken`) | 4d, `net.chronoflow.core.data.repository` | `detach()` is suspend and non-cancellable |
| `AccountDatabase.upsertSessionAccount(account)` | 4d, `net.chronoflow.core.data.store`, file `LoginUpsert.kt` | extension; preserves pending edits |
| top-level `syncAccount`, `syncOther`, `syncDate`, `identity`, `snapshot` (file `SyncFixtures.kt`), `ScriptedSource`, `TestGate`, `ManualSyncScheduler`, `eventually` | 4d, `:core:testing` `net.chronoflow.core.testing.sync` | reuse in 4e tests |
| `LaunchReadiness`, `SignedOutReadiness`, `Gate(rawValue)`, `LaunchEnding` | 3b, `:core:model` `net.chronoflow.core.model.launch` | `SessionPresence` also exists inside `LaunchReadiness.kt` (see T1); `SignedOutReadiness` is a separate file |
| `LaunchState` (`route: LaunchRoute?`), `LaunchRoute`, `Gate.launchRoute()`, `reduce`, `LaunchCoordinator`; test fixture `ManualLaunchClock` | 3b, **`:feature:launch`** `net.chronoflow.feature.launch.model` (3b §5.2, OQ-3b-8; `ManualLaunchClock` in `:feature:launch` test fixtures, 3b "Shared test clock") | `:core:*` cannot see these; only `:app` can |
| `LaunchController` (`readinessFactory: (LaunchClock) -> LaunchReadiness`; snapshot state `route`, `handOffComplete`, `overlayActive`; private `onLaunchChange(state, effects)`), `CfRoot` (`app.root`), `RoutePlaceholder(route: LaunchRoute, label)`, `LaunchDiagnostics`/`StubConfig`, `net.chronoflow.app.perf.StubReadiness(…, nativeReadiness)`, `LaunchTrace` (cookie 5 reserved for `session`) | 3a/3b, `:app` and `:feature:launch` | |
| `DebugRouteLabel(rawName)` (debug), string `net.chronoflow.core.ui.R.string.app_debug_route_x` (`core/ui/src/debug/res`, en "Debug route: %1$s", nl "Debugroute: %1$s") | 3b / 0c OQ-0c-4 | never named from `src/main` |
| `TestSeed` (`testId`, `profile`, `raw`, `ignoredKeys`, …), `TestSeeds.apply(context, seed, io)`, `TestSeeds.forwardIntent`, `SeedReceiverActivity`, `UITestTimeline`, `ScreenTest.launch(overrides: Map<String, String>)` | 0e, `app/src/debug` `net.chronoflow.debug`; `ScreenTest` in `app/src/androidTest` `net.chronoflow.app.testing` | `TestSeeds.apply(context, seed, io, sdkInt = …)` already has the SDK seam; append the cipher parameter after it |

### 2.6 Landed integration details (refresh 2026-11-18)
- `AppGraph(app, fontPreloader = ResourceFontPreloader, launchProvider = null)` already serves launch and Welcome tests. Keep those named dependency seams while adding `GraphOverrides`. `launchScope` is lazy; `processToken` is memory-only. `ChronoFlowApp.graph` currently has a private setter; T11 changes it to internal as specified.
- `LaunchPlatform` lives in `app/src/debug` and `app/src/release`; release is also compiled into perf variants. Put release-only twins in `app/src/releaseNoPerf`, shared debug/perf implementations in `app/src/perf`. Existing parser overrides capture `StubReadiness` with its signed-out default: T10 must pass the graph's native readiness when selecting the stub, retaining the existing diagnostics semantics.
- `LaunchController.onLaunchChange` is private. T10 adds a production observer callback with a no-op default for the app bridge and session trace; do not expose a test hook in main. Extend `LaunchTracing`/`LaunchTraceSession` with cookie 5 session methods, preserving existing traces and no-op compatibility for test implementations.
- `SessionSync.stop()` and `setEnabled(false)` already enter `NonCancellable + confined`. `AccountModel.detach()` already enters `NonCancellable + context`. Preserve these implementations. New database/graph teardown paths must likewise complete when the caller is cancelled. SessionSync has no `close()` API; graph shutdown stops it before cancelling graph-owned scopes, without changing earlier-phase sync behavior.
- `ApiClient.evictIdleConnections()` is suspend and dispatches to its injected IO dispatcher; never replace it with direct OkHttp access. `ApiClient.baseUrl` is `ApiBaseUrl`, supplied by `DataSourceFactory.baseUrl`.
- `DevicePreferences.shared(context)` is lazy about its DataStore file; it uses the device-wide store outside account namespaces. `DeviceIdentityStore(persistence)` accepts it. `SessionCipher` methods are synchronous and must be called only from injected IO paths; `SoftwareSessionCipher` remains test-only.
- `MockDataSource` controls are suspend (`setOffline`, `failNext`, etc.); its default latency is random 0.3–0.8 seconds, while the gated test delay defaults to zero. `MockStateStore` and its memory/file implementations live in `MockServerState.kt`; their synchronous file calls are made by the mock on IO.
- Gradle already gives app dependencies on network/data/sync/navigation and debug/perf dependencies on data-mock. Add only missing dependencies. Existing aliases: `libs.plugins.kotlin.serialization`, `libs.kotlinx.coroutines.test`, `libs.androidx.lifecycle.process`, `libs.androidx.navigation3.runtime`/`ui`. `:core:data` still needs its datastore dependency for the wiper. `:core:sync` still needs `java-test-fixtures` and fixture dependencies.
- Konsist already enforces `routesAreSerializable`; reuse it. Shared JVM fixture imports are test-only. Test rule details are in Appendix B. File-based tests resolve the repository with `projectRoot()`, rather than assuming the test process working directory.

## 3. Out of scope (owner in brackets)
- The sign-out confirmation sheet: Stay or Discard [10a]. 4e returns `SignOutResult.NeedsConfirmation(n)` and does nothing more (iOS OQ 29).
- The tab shell, the per-tab back stacks, reselect and the back rules across tabs [6a]. 4e has root routes only, and the `Today`, `Trips` and `Settings` keys exist.
- The screens:
  - Welcome [3c];
  - SignIn [5a];
  - TwoFactor [5b];
  - Forgot and Reset [5c];
  - Verify [5d];
  - Terms and Walkthrough [8];
  - Today [7];
  - Trips [6a];
  - TripPage [6b];
  - Wizard [9c];
  - settings pages [10a–10c];
  - GuestWizard [11a].

  Each shows the debug `RoutePlaceholder` until then.
- App Link intent filters and the `AuthLink` parser [5c]. Reminder scheduling, the notification tap payload and its routing [12b]. 4e captures and holds a `PendingLaunchIntent` and hands it to a no-op `LaunchIntentHandler`.
- Consuming `CF_PLAN_TEST_ROUTE`, `CF_PLAN_TEST_POSITION`, `CF_PLAN_TEST_SHEET`, `CF_SETTINGS_ROUTE`, `CF_WIZARD_AUTO_OPEN` and `CF_WIZARD_BOARD_CAPTURE` [6a, 6b, 9c, 10a]. 4e parses and validates them and exposes them through `DebugSeedRouting` (T11).
- The `CF_AUTH_TEST_AUDIT_SCREEN` values `sign-up`, `reset` and `two-factor` [5a, 5c, 5b]. 4e routes `sign-in` and `forgot` only.
- Store or sync failure UI (iOS OQ 30). Pull to refresh [6a].
- `confirmEmailVerification` callers [5d]. 4e ports the method and its test only.
- `SyncLifecycle`, `SessionSync` (all but `requestStart`), `ConnectivitySyncPathMonitor`, the login upsert body, `AccountModel` and `SyncTriggerTests` [4d]. 4e wires and attaches them only.
- The guest secret store, `guest-wizard.json` and its `recover()` [11a/11b]. 4e calls them through no-op seams (`GuestSecrets`, `SessionHooks.onForeground`).
- Any designed UI. There is **no 4e board** (§2.3); no loading, error or sign-out screen is built here.

## 4. Platform facts this phase relies on
- [F, lifecycle 2.11.0] `ProcessLifecycleOwner.get().lifecycle` dispatches ON_START when the first activity starts, and ON_STOP about 700 ms after the last one stops. Events arrive on the main thread.
- [F 4d Task 9] `ConnectivitySyncPathMonitor` already wraps `registerDefaultNetworkCallback` (current state first, online = `INTERNET && VALIDATED`). It needs `ACCESS_NETWORK_STATE`; check the merged main manifest and add the permission in T9 if 0a/4d did not.
- [F, navigation3 1.2.1] `NavDisplay(backStack, onBack, entryDecorators, entryProvider)`. The default decorator `rememberSaveableStateHolderNavEntryDecorator` keys saved state by the entry's `contentKey`, which defaults to `key.toString()`. **A data-class key's `toString()` therefore goes into the saved-state `Bundle`.** This is why no secret may sit in a route key (T8).
- [F, lifecycle 2.11.0] `viewModelFactory { initializer { … } }` reads `CreationExtras`. To add a key, override `ComponentActivity.defaultViewModelCreationExtras` and return `MutableCreationExtras(super.defaultViewModelCreationExtras).apply { set(AppGraphKey, graph) }`.
- [F, AGENTS Traps] `runCatching` swallows `CancellationException`. Use explicit `try/catch` and rethrow it. The bounded logout racers in §5.3 also rethrow cancellation; their own cancellation is handled by the structured race, preserving the Swift deadline behavior without violating AGENTS.
- [R] A job in the controller's own `CoroutineScope` (not a child of the caller) is the Kotlin form of the iOS comment "Once requested, cleanup must complete even if its UI caller disappears" (`SessionController.swift:271`, `enqueueSignOut`). `withContext(NonCancellable)` protects the accept commit block (§5.2) and teardown paths that must complete; preserve the landed 4d cleanup guarantees.
- [F, Robolectric 4.18 / 4a] Robolectric has no AndroidKeyStore provider (4a uses a `SoftwareSessionCipher` in its Robolectric tests). Any Robolectric test here that builds a real `AppGraph` or runs `TestSeeds.apply` passes `GraphOverrides(sessionCipher = SoftwareSessionCipher())` (OQ-4e-17).

## 5. Spec

### 5.1 Types [R, from F `SessionController.swift:3–103`, `SignOutResult.swift`]
**Reuse, never redefine** (§2.5): `SessionPresence`, `StoredSession`, `SessionStore`, `SyncEpoch`, `SessionMutationPermit`, `StorageNamespace` (4a); `Gate`, `LaunchReadiness`, `SignedOutReadiness`, `LaunchRoute` (3b); `SessionAccount`, `AccountPolicies`, `SessionResult`, `AccountID` (0d); `StoreFailure` (4d).

New in 4e, in `:core:model`, package `net.chronoflow.core.model.session`:
```kotlin
sealed class SessionFailure : Exception() {           // message null: never carries a token
    data object CredentialsUnavailable : SessionFailure(); data object DatabaseUnavailable : SessionFailure()
    data object CredentialWrite : SessionFailure(); data object InvalidResult : SessionFailure(); data object Superseded : SessionFailure()
}
sealed interface SignOutResult { data object SignedOut : SignOutResult; data class NeedsConfirmation(val pendingCount: Int) : SignOutResult }
data class SessionSnapshot(val session: StoredSession, val epoch: Long)      // StoredSession.toString() already redacts
data class SessionUpdate(val presence: SessionPresence, val epoch: Long)
data class StoredAccountReadiness(val accountId: String, val gate: Gate?, val hasStoredData: Boolean, val firstLoadGate: Gate = Gate.App)
```
- `SessionPresence` currently exists twice: 3b defines it inside `LaunchReadiness.kt` and 4a in `SessionPresence.kt` (`…model.launch` and `…model.session`). T1 keeps the **4a** one (`net.chronoflow.core.model.session`), removes the declaration from `LaunchReadiness.kt` and fixes the imports (including same-package uses in `SignedOutReadiness.kt`), with the same name and cases (OQ-4e-1).
- `Gate` and `LaunchReadiness` stay in `net.chronoflow.core.model.launch` (3b put them in a core module so `:core:sync` can implement them). If 3b fell back to `:core:sync` (its §5.1 last line), leave them there.

### 5.2 SessionController state and rules [F `SessionController.swift`; R Kotlin form]
- **Actor mapping: use 4d §5.3 exactly, never a `Mutex`.** State is touched only on `confined = io.limitedParallelism(1)`; every public method is `withContext(confined) { … }`. That keeps Swift actor **re-entrancy**: state may change at every suspension point, so re-check exactly where Swift re-checks. **Never hold a lock across a suspension**: `stopSync`, `requestSync` (→ `SessionSync.requestStart` → `activeSession()`) and `SessionSync.isCurrent` call back into the controller while a `tail` job is suspended, and the ported tests read state while an `accept` waits inside its credential write (§2.3).
- **Long work** runs as a chained `tail` job in an owned `CoroutineScope(SupervisorJob() + confined)` (never a child of the caller):
  ```kotlin
  val previous = tail; val job = ownScope.async { previous?.join(); work() }; tail = job
  ```
  - `restore` and `accept` await `job` and, if the **caller** is cancelled, cancel `job` (Swift `withTaskCancellationHandler { … } onCancel: { task.cancel() }`; 4d §5.3 form). `cancelingAnAcceptBeforeOpenCompletesPreventsPublication` depends on it.
  - Sign-out never cancels its job (below).
- **Readable state.** `presence`, `epoch`, `storedSession`, `generation`, `pendingUnauthorizedEpoch` and `signOutFlight` are readable from outside (`suspend` getters through `confined`, or `@Volatile` fields written only on `confined`); the ported tests read them. Only `presence`, `epoch` and `pendingUnauthorizedEpoch` are public; the rest are `internal`.
- **State:**
  - `presence: SessionPresence?` (null until the first restore or accept) and `epoch: Long` (starts at 0);
  - `storedSession: StoredSession?`;
  - `generation: Long`, `transitioning: Boolean` and `mutationPermit: SessionMutationPermit?` (4a's class; only this controller calls `invalidate()`, 4a OQ-4a-15);
  - `signOutFlight: Deferred<SignOutResult>?` and `unconditionalSignOut: UnconditionalSignOut?` (a private `data class UnconditionalSignOut(val discardPending: Boolean, val unauthorized: Boolean)`; Swift keeps the tuple `(discardPending, unauthorized)?`, l.111);
  - `discardRequested`, `unauthorizedRequested` and `pendingUnauthorizedEpoch: Long?`;
  - the callbacks `stopSync: suspend () -> Unit` and `requestSync: suspend (epoch: Long) -> Unit`, both no-ops by default (Swift init defaults, l.128–129). `AppGraph` sets both once with `attach(stopSync, requestSync)` before the first restore; tests that need them call `attach` too.
- **`activeSession(): SessionSnapshot?`.** Returns null if `transitioning`, or if `presence != SignedIn(storedSession.accountId)`. Otherwise it returns `SessionSnapshot(storedSession, epoch)`.
- **`mutationPermit(snapshot)`.** Returns the permit only when `activeSession() == snapshot`.
- **`updates: StateFlow<SessionUpdate?>`.** A `MutableStateFlow`, so the newest value replaces older ones (iOS `bufferingNewest(1)`). It replays the current value only once presence is known (non-null).
- **`publish(p)`, the only writer of presence.** In order:
  1. invalidate the old permit;
  2. create a new permit only when `p` is `SignedIn`;
  3. `epoch += 1`;
  4. set `presence = p`;
  5. emit `SessionUpdate(p, epoch)`.
- **`check(gen)`.** Calls `currentCoroutineContext().ensureActive()`. If `gen != generation`, it throws `SessionFailure.Superseded`.
- **`restore(): SessionPresence`** [F l.342–388]. `generation += 1`, captured as `gen`, then chained on `tail`. Every store call rethrows `CancellationException` unchanged and maps any other exception as stated:
  1. `check(gen)`; `transitioning = true`; `stopSync()`; `teardown.prepare()`; `check(gen)`.
  2. Read the credentials (`readStoredSession`). Any non-cancellation exception throws `CredentialsUnavailable` and leaves the file untouched. `check(gen)`.
  3. If there is no session: `accounts.close()` (an error throws `DatabaseUnavailable`); `check(gen)`; `storedSession = null`; `publish(SignedOut)`; return `SignedOut`.
  4. If the `accountId` is not a valid UUID (`AccountID.of` throws `AccountID.InvalidUUID`, 0d): `credentials.delete()` (an error throws `CredentialsUnavailable`, and the bad blob is kept); `check(gen)`; `accounts.close()` (an error throws `DatabaseUnavailable`); `check(gen)`; `storedSession = null`; `publish(SignedOut)`; return `SignedOut`.
  5. `accounts.open(id)` with the `AccountID` (an error throws `DatabaseUnavailable`); `check(gen)`.
  6. `storedSession = StoredSession(token, id.rawValue)` (lower-case); `publish(SignedIn(id.rawValue))`; `transitioning = false`.
  7. `requestSync(epoch)`; `check(gen)`; return `SignedIn(id.rawValue)`.
  8. In `finally`: `transitioning = false`, always (Swift `defer`).
- **`accept(result)`** [F l.390–423]. `generation += 1`, then chained:
  1. `check(gen)`. Then validate a non-blank token (after trimming) and `AccountID.of(account.id)`; otherwise throw `InvalidResult` and touch no storage. (0d's serializer already rejects these on the wire; this guards hand-built results.)
  2. `transitioning = true` (reset in `finally`, always); `stopSync()`; `prepare()`; `check(gen)`.
  3. If `presence` is `SignedIn` to a **different** account: `storedSession = null` and `publish(SignedOut)`.
  4. `accounts.open(id)`; `check(gen)`; `accounts.upsert(account)` (4d's `upsertSessionAccount`, which keeps pending local edits). A `SessionFailure` is rethrown as is; any other non-cancellation error maps to `DatabaseUnavailable`. Then `check(gen)`.
  5. In **one** `withContext(NonCancellable) { … }` block: write the credentials JSON `{"token","accountId"}` (4a's `StoredSession` encoding; an error throws `CredentialWrite`), then `storedSession = …`, `publish(SignedIn(id))`, `transitioning = false`, `requestSync(epoch)`. iOS comment: "A successful credential write commits the session, even if superseded or canceled during the write." **There is no `check(gen)` after the write.** Keep the publish inside the block: `withContext` has a prompt-cancellation guarantee on return, so a cancelled caller could otherwise lose the publish after a successful write.

  The order is **DB upsert → secure write → publish → sync** (A5).
- **`confirmEmailVerification(expected, email): Boolean`** [F l.194–220]. Chained, with **no** generation bump; `gen` is the current generation, captured at the call.
  1. `ensureActive()`; if `generation != gen` or `activeSession() != expected`, return false.
  2. `updated = accounts.markEmailVerified(AccountID.of(expected.session.accountId), email)`; `ensureActive()`.
  3. If not `updated`, or the generation or session changed, return false.
  4. `requestSync(expected.epoch)`; return `generation == gen && activeSession() == expected`.

### 5.3 Sign-out and wipe [F `SessionController.swift:222–335`, `SignOutResult.swift`]
- **`SessionTeardown`**, a data class of suspend lambdas. Every one is injectable; the defaults are in brackets:
  - `prepare` [no-op]
  - `pendingCount: suspend () -> Int` [0]
  - `flush` [no-op]
  - `beginWipe(accountId)`, `erase(accountId)` and `finishWipe()`
  - `logout(StoredSession)`
  - `detach` [no-op]
  - `sleep: suspend (Duration) -> Unit` [**no default**: `:core:sync` has no clock defaults (4d §5.3, 1a's Konsist clock rule). `AppGraph` passes `{ delay(it) }`; tests pass a virtual-time or gated sleep.]
- **Entry points:**
  - `signOut(discardPending: Boolean, unauthorized: Boolean = false): SignOutResult` is unconditional (`expectedEpoch = null`).
  - `signOut(expected: SessionSnapshot, discardPending)` returns `SignedOut` at once if `activeSession() != expected`. Otherwise it enqueues with `expectedEpoch = expected.epoch`.
  - `unauthorized(epoch)`:
    1. If `this.epoch != epoch`, return `SignedOut` and do nothing. **This is D23: an old 401 never wipes a new session.**
    2. Otherwise set `pendingUnauthorizedEpoch = epoch` and enqueue with `discard = true, unauthorized = true, expectedEpoch = epoch`.
    3. On success, clear `pendingUnauthorizedEpoch`.
  - `retryUnauthorized()`, called on ON_START. If `pendingUnauthorizedEpoch != null`: if the epoch has since moved on, clear it; otherwise run `unauthorized(it)` again.
- **`enqueueSignOut(expectedEpoch, discard, unauthorized)`:**
  1. If `expectedEpoch != null && expectedEpoch != epoch`, return `SignedOut`.
  2. `discardRequested ||= discard` and `unauthorizedRequested ||= unauthorized`.
  3. If `expectedEpoch == null`: `generation += 1` and `unconditionalSignOut = UnconditionalSignOut((unconditionalSignOut?.discardPending ?: false) || discard, (unconditionalSignOut?.unauthorized ?: false) || unauthorized)` (merged, l.255–261).
  4. If `signOutFlight` exists, await and return it (join).
  5. Otherwise start `performSignOut` as the next `tail` job in the controller's own scope (not a child of the caller), set `signOutFlight` to it, and await it. Cancelling the caller only stops the caller's wait; the job runs to the end (iOS: "Once requested, cleanup must complete even if its UI caller disappears.").
- **`performSignOut(expectedEpoch)`. The order is load-bearing:**
  0. `finally`: reset `signOutFlight`, `unconditionalSignOut`, `discardRequested` and `unauthorizedRequested`.
  1. If `expectedEpoch != null && expectedEpoch != epoch`: return `SignedOut` when `unconditionalSignOut == null`. Otherwise **replace** (not OR) `discardRequested = unconditionalSignOut.discardPending` and `unauthorizedRequested = unconditionalSignOut.unauthorized`, and go on (l.282–286). `staleForcedFlightPreservesReplacementPendingRowsWithoutExplicitDiscard` depends on the replacement.
  2. `captured = storedSession`. If it is null: `stopSync()`, `accounts.close()`, `prepare()`, then read the credentials again.
  3. Still no session: `credentials.delete()`, `publish(SignedOut)`, `detach()`, return `SignedOut`.
  4. **Voluntary only** (neither unauthorized nor discard):
     - `n = pendingCount()`;
     - if `n > 0`: `try { flush() } catch (non-cancellation) {}`, then `n = pendingCount()`;
     - if still `n > 0` **and** neither `discardRequested` nor `unauthorizedRequested` was set meanwhile (a request merged during the flush upgrades this flight, l.306), return `NeedsConfirmation(n)`. Nothing has been touched: no marker, no logout, and the credentials stay.
  5. `id = AccountID.of(captured.accountId)`; `beginWipe(id)` writes the marker.
  6. `storedSession = null`; `publish(SignedOut)`. iOS: "Publication invalidates session consumers before awaiting any old work." The permit is revoked here.
  7. `stopSync()`. On Android this is the reminder hook first (`SessionHooks.onSessionEnding`, a no-op until 12b), then `SessionSync.stop()`.
  8. **Voluntary or discard only (not unauthorized):** `bestEffortLogout(captured)` [F l.327–335]. In a `coroutineScope`, start two `async` racers: `try { logout(captured) } catch (_: Exception) {}` and `try { sleep(3.seconds) } catch (_: Exception) {}`. Each racer ignores non-cancellation failures and rethrows `CancellationException` (AGENTS takes precedence over Swift `try?`; explicit cancellation of the losing child is joined by `coroutineScope`). Wait for the first to finish (`select { a.onAwait {}; b.onAwait {} }`), then cancel both. Do not use `withTimeoutOrNull`: the deadline must come from the injected `sleep`. The whole sign-out runs in the controller's own scope, so no outer cancellation reaches this point.
  9. `detach()`. This detaches the session UI and navigates to `SignIn` when the epoch still matches (see `SessionGateRouter`).
  10. `accounts.close()`.
  11. `erase(id)`, then `credentials.delete()`. If either fails, remember the first error, try the other, then rethrow. The marker stays, so the next `prepare()` retries.
  12. `finishWipe()` removes the marker.
  13. Return `SignedOut`.

  Plan mapping: plan.md Phase 4e "SessionController" teardown steps 1–10 are this list's steps 4, 5, 6, 7, 8, 9, 10, 11 (`erase`), 11 (`credentials.delete`) and 12 (review 2026-11-16).
- **AccountWiper** [F `Kit/LocalStore/AccountWiper.swift`]. `AccountWiper(storageRoot: File, namespace: StorageNamespace, credentials: SessionStore, guestSecrets: GuestSecrets = NoGuestSecrets, io, remove: (File) -> Unit = { if (it.exists() && !it.deleteRecursively()) throw StoreFailure.Unavailable() })`. The `remove` seam is the Swift `remove:` parameter (l.9, 14–16); `pendingWipeRetriesGuestRecoveryDeletion` and `markerSurvivesPartialFailure…` inject a remover that fails once. Every file removal in `erase` goes through `remove`, on `io`.
  - **Root.** `root = File(storageRoot, namespace.directoryName)`; the marker is 4a's `WipeMarker(root, io)` (`root/pending-wipe.json`).
  - **`begin(id)`.** `marker.write(id)`: 4a creates the root and writes the **JSON string** `"<uuid>"` (with the quotes) atomically.
  - **`recover()`.** `marker.read()`: null → nothing. Otherwise `erase(id)` → `credentials.delete()` → `finish()`.
    - `CorruptWipeMarkerException` (not a JSON string, or not a UUID) → throw `SessionFailure.DatabaseUnavailable` [R, 4a hand-off; iOS throws whatever the decoder throws and its test only expects "an error"]. It touches nothing and never reaches another namespace.
  - **`erase(id)`.** Deletes `root/Accounts/<id>/` recursively (4a's path: `…/ChronoFlow/Accounts/<id>/account-<id>.db`), then `guestSecrets.deleteAll()`, then `root/guest-wizard.json`. `GuestSecrets` is a `:core:data` interface (`suspend fun deleteAll()`) with `NoGuestSecrets` as the default until 11a.
  - **`finish()`.** `marker.clear()`.
  - **What survives:** device preferences (`device_prefs`), `device-config` and the device id.

### 5.4 Readiness and gate [F `StoredDataReadiness.swift`, `AccountReadinessRepository.swift`, `SessionGateRouter.swift`]
- **`AccountReadinessRepository(database: AccountDatabase).values(accountId: String): Flow<StoredAccountReadiness?>`**. First `AccountID.of(accountId)` (throws `InvalidUUID`), and if `database.accountID != id` throw `StoreFailure.InvalidAccountID` [F l.9–10]. Then one 4a `database.observe("account", "settings", "outbox") { sql -> … }` over the account row, the settings row `policies` and the pending outbox rows of kind `walkthrough`, so all three are read in one fetch and observed together. (Not `asFlow().mapToList`: 4a OQ-4a-4.)
  - No account row → `null`.
  - `walkthroughSeen = account.walkthroughSeen || a pending outbox row of kind "walkthrough" exists`.
  - Gate precedence:
    1. `!hasVerifiedEmail` → `Verify`;
    2. `policies == null` → `null` (the gate keeps loading);
    3. `acceptedTermsVersion != termsVersion` → `Terms`;
    4. `!walkthroughSeen` → `Walkthrough`;
    5. otherwise `App`.
  - `hasStoredData = syncedAt != null`, and `firstLoadGate = if (walkthroughSeen) App else Walkthrough`.
- **`StoredDataReadiness`** implements 3b's `LaunchReadiness` (`session`, `gate`, `gateUpdates`, `hasStoredData`, `storedDataUpdates`, `allowFirstLoad`), plus `retry()` and `routeAcceptedSession`. `:core:sync` cannot see `AccountDatabase`, so it reads only through `fun interface AccountReadinessSource { suspend fun values(accountId: String): Flow<StoredAccountReadiness?> }` (Swift `values: (String) async throws -> Values`). `:core:data` implements it as `{ id -> AccountReadinessRepository(sessionAccounts.activeAccountDatabase() ?: throw StoreFailure.Closed).values(id) }`, so each call (each epoch) builds a repository over the currently open database. Constructor: `StoredDataReadiness(controller, values: AccountReadinessSource, io)`; it owns a confined scope like the controller (§5.2).
  - **`session()`.** Uses one shared restoration `Deferred`, which loops `controller.restore()`. On an error it sets `failure` (the `SessionFailure`, else `DatabaseUnavailable`) and suspends until `retry()` bumps the revision. Cancellation returns `SignedOut` without deleting anything.
  - **`gate()`** is the first `gateUpdates()` value, else `App`. **`hasStoredData()`** is the first value's `hasStoredData`, else `false`.
  - **`gateUpdates(): Flow<Gate>`.** When signed out, it emits nothing.
    - A value with a gate is emitted, deduplicated per collector.
    - A value with no gate records `fallback = value?.firstLoadGate ?: App` (a `null` value, no account row yet, falls back to `App`). The fallback is emitted only if `firstLoadEpoch == epoch`.
    - An `accountId` mismatch throws `DatabaseUnavailable`. On an error with an unchanged epoch, it sets `failure` and waits for `retry()`. It stops when the epoch changes.
  - **`storedDataUpdates()`** emits `hasStoredData ?: false`.
  - **`allowFirstLoad()`.** Only when signed in: sets `firstLoadEpoch = epoch` and emits the pending fallbacks.
  - **`routeAcceptedSession(navigate, verification)`.** This is for 5a after `accept`. The body runs as a job in the readiness's own scope and the caller only awaits it, so cancelling the caller does not stop the routing [F l.71–81; `committedVerificationRoutingSurvivesRequestCancellationAndRestore`].
    1. Require `activeSession()`.
    2. Read `gate()`.
    3. Check the session again.
    4. `Verify` → `verification(epoch)`; anything else → `navigate(gate.toRoute())`.
  - **`SignedOutReadiness`** already exists (3b `object`, session `SignedOut`, gate `App`, `hasStoredData = true`). Reuse it.
  - **`UnavailableReadiness`** (new, `:core:sync`): the session never resolves (`awaitCancellation()`), so 3b's 8 s fallback applies (iOS OQ 7: an unknown session at 8 s is treated as signed out).
- **`SessionGateRouter`** (`:core:sync`, JVM). It takes callbacks, not a navigator:
  ```kotlin
  SessionGateRouter(controller, readiness, scope,
      onSignedOut: () -> Unit, onGate: (Gate) -> Unit, onVerify: suspend (epoch: Long) -> Unit)
  ```
  - `start()` returns at once if already started. Otherwise it collects `controller.updates` (skipping the initial `null`) with a local `sawSignedIn = false`. Every update cancels the running `gateJob`.
  - **`SignedOut`:** if `sawSignedIn && controller.epoch == update.epoch`, call `onSignedOut()` (`:app` maps it to `SignIn`). Then continue. An initially signed-out app keeps Welcome.
  - **`SignedIn`:**
    1. `sawSignedIn = true`.
    2. Require `activeSession()?.epoch == update.epoch`; otherwise skip.
    3. `gateJob` collects `readiness.gateUpdates()`. For each value it re-checks `activeSession() == snapshot`; if the snapshot changed, the gate job **ends** (Swift `return`, l.43–45).
    4. It dedupes against the previous gate. `Verify` → `onVerify(epoch)`; any other gate → `onGate(gate)`.
  - `stop()` cancels both jobs.
- **`Gate.toRoute()`** (`:core:navigation`) [F `LaunchReadiness.swift:9–19`]: `Verify` → `Route.Verify`, `Terms` → `Route.Terms`, `Walkthrough` → `Route.Walkthrough(replay = false)`, `App` → `Route.Today`.
- **Launch route** [F `LaunchReadiness.swift` `LaunchState.route`]. 3b already computes `LaunchState.route: LaunchRoute?` (signed out → `Welcome`; signed in → `(gate ?: if (didForceReady) App else null)?.launchRoute()`). 4e adds `LaunchRoute.toRoute()` **in `:app`** (`…/app/LaunchSessionBridge.kt`), because `LaunchRoute` lives in `:feature:launch` and `:core:navigation` cannot depend on a feature (OQ-4e-15): `Welcome` → `Route.Welcome`, `Today` → `Route.Today`, `Verify` → `Route.Verify`, `Terms` → `Route.Terms`, `Walkthrough` → `Route.Walkthrough(replay = false)` (OQ-3b-8).

  3b's reducer owns the timing, and 4e only maps the route. **Revision guard** [F `SceneDelegate.swift:430, 448–451`, §2.3]: `LaunchSessionBridge` captures `launchRevision = rootNavigator.state.value.revision` when the launch starts. For every published launch route (3b's `LaunchController.route` snapshot state, non-null), it navigates only if `rootNavigator.state.value.revision == launchRevision`, and then sets `launchRevision` to the new revision. Once anyone else navigates (sign-out, verification), the launch never navigates again (`LaunchConnectionTests.testLaunchDoesNotOverwriteSessionNavigation`).

### 5.5 Sync lifecycle [F `SessionSync.swift:41–44`, `NativeSessionController.swift:61–71, 152–160`; plan A7]
- **Ownership** (4d OQ-4d-14). 4d already ships `SyncLifecycle` (backoff `min(2^n, 300)` s, server deadline, retry state across pauses), `SessionSync` (everything but `requestStart`) and `ConnectivitySyncPathMonitor`, with `SyncTriggerTests`. **Do not port or change them.** If one is missing, stop and report it.
- **`SessionSync.requestStart(controller: SessionController, epoch: Long)`** (new, in 4d's `SessionSync.kt`, the only edit to that file): `val current = controller.activeSession(); if (current == null || current.epoch != epoch) return`; then launch `start(current.session, epoch)` on the sync's own scope (iOS `Task { … }`: the caller does not wait for the start).
- **Wiring** (`AppGraph`, T9): `SessionSync(`
  - `accounts = sessionAccounts` (the `SessionAccountDatabase`, which implements 4d's `ActiveSyncDatabase` by returning `SqlSyncDatabase(open database)` or null),
  - `scheduler = SyncScheduler.system()`,
  - `makePath = { ConnectivitySyncPathMonitor(appContext) }`,
  - `source = { session -> dataSources.forSession(session) }`,
  - `isCurrent = { s, e -> controller.activeSession()?.let { it.session == s && it.epoch == e } ?: false }`,
  - `unauthorized = { e -> try { controller.unauthorized(e) } catch (c: CancellationException) { throw c } catch (_: Exception) {} }`,
  - `verification = { e -> onVerify(e) }` (§5.4 / T10),
  - `dispatcher = dispatchers.io)`.
  The `unauthorized` lambda passes **the epoch the lifecycle was built with**, never `controller.epoch` (D23).
- **Foreground triggers** (`SessionActivation`, `:app`). One `Mutex` serializes the steps, like iOS `setActive` on `activityTask`.
  - **ON_START** (iOS order, `NativeSessionController.swift:152–160`):
    0. `sessionDataSources.evictIdleConnections()` — a **suspend** call that runs on `io` (4b OQ-A150: `ApiClient.evictIdleConnections()` suspends and closes idle pooled connections on `io`, because closing a TLS socket sends close_notify, which is network I/O and throws `NetworkOnMainThreadException` on main). `SessionDataSources` keeps every `ApiClient` it builds (session and public) and exposes `suspend fun evictIdleConnections()` that evicts each; for the mock source it is a no-op. Inject it into `SessionActivation` as a `suspend () -> Unit` so `SessionActivationTest` can record the call. It drops connections that may have died during sleep or a network change **while backgrounded**; a network change while foregrounded is not covered (4d may also evict from its default-network callback). Android-only, no iOS equivalent;
    1. `sessionSync.setEnabled(true)`;
    2. `hooks.onForeground()` (guest-wizard recover for 11a, then reminder reschedule for 12b; both no-ops now);
    3. `controller.retryUnauthorized()`; if it throws a non-cancellation exception, stop here;
    4. `readiness.retry()`;
    5. if `controller.activeSession()` is non-null, `sessionSync.resume(it.session, it.epoch)`.
  - **ON_STOP** (iOS `NativeSessionController.setActive(false)`, `SessionSync.swift:99–102`): under the same `Mutex`, `sessionSync.setEnabled(false)`. It cancels the running flight and keeps the retry state; it makes no source call and starts no job.
  - There is no WorkManager and no background flush (A7).

### 5.6 Graphs (`:app`) [R, A4]
- **`AppGraph`** (`…/app/AppGraph.kt`). Built in `ChronoFlowApp.onCreate` with **no I/O**. Every member is `by lazy`.
  - Members: `dispatchers: AppDispatchers` (`main`, `io`, `default`; 4b leaves it to 4e), `appScope`, `storageRoot`, `selection: DataSourceFactory`, `namespace`, `sessionStore`, `wiper`, `sessionAccounts: SessionAccountDatabase`, `dataSources: SessionDataSources`, `accountModel: AccountModel` (4d), `sessionGraphs`, `sessionSync`, `controller`, `readiness`, `rootNavigator`, `gateRouter`, `pendingLaunchIntents`, `hooks: SessionHooks`, `activation`, plus the 3b `launchController` and the members already there.
  - **Storage root.** `storageRoot: suspend () -> File` resolves `context.noBackupFilesDir` on `dispatchers.io` once and caches it (resolving it may create a directory; 4a hand-off). No member resolves it in a constructor.
  - **Selection and namespace** [F 4b hand-off]: `selection = DataSourceFactory(BuildConfig.DATA_SOURCE, BuildConfig.API_BASE_URL, release = !BuildConfig.DEBUG, testing = false)`; `namespace = if (selection.source == DataSourceFactory.Source.Api) StorageNamespace.Api else StorageNamespace.Mock`.
  - **Production stores:** `KeystoreSessionStore.forNamespace(root, namespace, cipher, io)` with `cipher = overrides.sessionCipher ?: AndroidKeystoreSessionCipher()`; `AccountDatabase(root, namespace, AndroidAccountDriverFactory(context), io)` inside `SessionAccountDatabase`; `DevicePreferences.shared(context)` (4a).
  - **`accountModel`** = 4d's `AccountModel(scope = CoroutineScope(SupervisorJob() + dispatchers.main))` (`AppDispatchers.main` defaults to `Dispatchers.Main.immediate`; its injectable type is `CoroutineDispatcher`, so tests can pass `StandardTestDispatcher`).
  - **`SessionDataSources`** (`…/app/SessionDataSources.kt`): `forSession(session: StoredSession): DataSource` and `public(): DataSource`.
    - `Api`: `ApiDataSource(ApiClient(baseUrl = selection.baseUrl, bearer = { session.token }, io = dispatchers.io))`, built lazily, without `baseClient` (`:app` main never imports OkHttp; 4b). `public()` uses `bearer = { null }`. `:app` already depends on `:core:network` (since 0a); add it only if missing.
    - `Mock`: main code cannot name `:core:data-mock` (debug and perf only). It asks the **variant hook that already carries 3a's `LaunchOverrides`** for a `MockSources` (`fun interface MockSources { fun dataSource(root: File, overrides: GraphOverrides, bearer: suspend () -> String?): DataSource }`). `PerfMockSources` (in `app/src/perf`, which debug and perf share) returns:
      ```kotlin
      MockDataSource(
          store = FileMockStateStore(root),
          seed = overrides.mockSeed?.let { MockAppSeed.make(Instant.now(), it.unverified, it.walkthroughSeen, it.termsPending) },
          delay = overrides.mockDelay?.let { d -> { d.toDouble(DurationUnit.SECONDS) } } ?: <4c default>,
          bearer = bearer,
          debugAppSeed = BuildConfig.DEBUG,
      )
      ```
      Debug seeds the demo accounts, perf does not (4c OQ-4c-4). The auth-test seed goes in as the mock's `seed` argument, exactly as iOS (§2.3), never as a pre-written `state.json`. Release returns null, and `Mock` cannot be selected in release (4b fixes release to `api`). One `MockDataSource` per graph, shared by every session (the mock server is process state); its `bearer` reads the current `sessionStore.readStoredSession()?.token`.
  - **`GraphOverrides`** (main, `…/app/GraphOverrides.kt`), the only knob debug uses (T11):
    ```kotlin
    data class MockSeedFlags(val unverified: Boolean, val walkthroughSeen: Boolean, val termsPending: Boolean)
    data class GraphOverrides(
        val storageDir: File? = null,          // replaces noBackupFilesDir as the storage root
        val mockDelay: Duration? = null,       // null = the 4c default delay
        val sessionAlias: String? = null,      // Keystore alias override
        val mockSeed: MockSeedFlags? = null,   // CF_AUTH_TEST_* gate seeds, passed to MockAppSeed.make
        val sessionCipher: SessionCipher? = null, // Robolectric tests pass a software cipher (no AndroidKeyStore there)
    ) { companion object { val None = GraphOverrides() } }
    ```
  - `ChronoFlowApp.onCreate` registers `activation` on `ProcessLifecycleOwner`. Registering is not I/O.
- **`SessionGraph`** (`…/app/SessionGraph.kt`), one per epoch (A4):
  - `SessionGraph(epoch: Long, snapshot: SessionSnapshot, database: AccountDatabase, accounts: AccountRepository, trips: TripRepository, settings: SettingsRepository, outbox: OutboxRepository, sync: SessionSync, dataSource: DataSource, scope: CoroutineScope)`.
  - It holds **no `SyncEngine` of its own**: 4d's `SessionSync` owns one `SyncLifecycle`/`SyncEngine` per epoch and keeps the retry state, so the graph exposes `sync` (OQ-4e-3). The reminder scheduler joins in 12b.
  - **`SessionGraphs`** (`…/app/SessionGraphs.kt`) ports iOS `NativeSessionController.observe()` (§2.3): it collects `controller.updates` on `appScope`. For each update, if `controller.epoch == update.epoch && controller.presence == update.presence` and the update is `SignedIn(id)` with `sessionAccounts.activeAccountDatabase()?.accountID?.rawValue == id`, it builds the graph, publishes it, and calls `accountModel.attach(graph.accounts, update.epoch, ifCurrent = token)` (token read before the checks). Otherwise it publishes null and detaches the model if the token is unchanged.
  - A new epoch cancels the old graph's `scope` and drops it. A signed-out presence leaves `current = null`. `drop()` (the `SessionAccountDatabase` detach closure) cancels the scope, publishes null and calls `accountModel.detach()`.
  - Exposed as `val current: StateFlow<SessionGraph?>`.
- **`AppGraphKey : CreationExtras.Key<AppGraph>`** (`…/app/AppGraphKey.kt`). `MainActivity` overrides `defaultViewModelCreationExtras` as in §4.
  - A ViewModel factory reads `this[AppGraphKey]!!` and captures `graph.sessionGraphs.current.value` at creation. A screen in `key(revision)` never outlives its epoch.

### 5.7 Routes and navigator (`:core:navigation`) [R from plan l.400–405, A8; F `Route.swift`, `RootView.swift`]
```kotlin
@Serializable sealed interface Route : NavKey {
  @Serializable data object Welcome; SignIn; TwoFactor; Forgot; SignUp; Verify; GuestWizard; Terms; Today; Trips; Settings
  @Serializable data class Reset(val email: String) : Route                      // token NOT in the key (OQ-4e-5)
  @Serializable data class Walkthrough(val replay: Boolean = false) : Route
  @Serializable data object Profile; Account; Language; Security; Password; TwoFactorSettings; Passkeys; BodyClock; Preferences; Reminders; Plan
  @Serializable data class TripPage(val tripId: String, val dayId: String? = null, val blockId: String? = null,
      val awaitingSync: Boolean = false, val notificationInputHash: String? = null,
      val notificationToken: String? = null, val sheet: TripSheet? = null) : Route
  @Serializable data class Wizard(val mode: WizardMode) : Route
}
@Serializable sealed interface TripSheet { @Serializable data class Step(val blockId: String); @Serializable data object Legend; @Serializable data object TripActions }
@Serializable sealed interface WizardMode { @Serializable data object Create; @Serializable data class Edit(val tripId: String); @Serializable data class Duplicate(val tripId: String) }
data class RootState(val route: Route?, val revision: Long)
class RootNavigator { val state: StateFlow<RootState>; fun navigate(to: Route) /* revision += 1, always, even for the same route */ }
fun Route.debugName(): String   // "welcome", "signIn", "twoFactor", …, "tripPage", "wizard": the lowerCamel class name
```
- `Route.debugName()` feeds the test tag `route.<debugName>` [F `App/Debug/RoutePlaceholder.swift`: identifier `route.<routeName>`].
- `TripSheet` lists only the three sheets the spec names. Later phases add cases with their boards.

### 5.8 Root content, placeholder, probe, pending intents (`:app`) [R; F D17, `RootView.swift`, `PendingLaunchIntent.swift`]
- **`RootContent`** (`…/app/RootContent.kt`), placed by 3b's `CfRoot` where it now draws `RoutePlaceholder(route, label)` (inside the `app.root` box, after the hand-off rules 3b set up):
  - It collects `rootNavigator.state`.
  - It keeps `val stack = remember { mutableStateListOf<NavKey>() }` and replaces its content with `[route]` whenever `revision` changes.
  - It calls `NavDisplay(backStack = stack, onBack = { /* root: system back finishes the activity */ }, entryProvider = …)`.
  - Each entry's content is wrapped in `key(revision)`.
  - The stack is **never** a `rememberNavBackStack` and never saved. It is derived from the process-level `RootNavigator`, so an activity recreation in the same process (locale, font scale, layout direction; A8) shows the same route at once, and a size or uiMode change does not recreate at all. Process death is a cold launch (A8, iOS OQ 16).
- **Entries:**
  - routes with a screen keep it (the 3c Welcome, if merged);
  - every other route renders 3b's `RoutePlaceholder`, generalised in T10 from `route: LaunchRoute` to `route: Route` with `rawName = route.debugName()`. Its surface stays `CfColors.background` with tag `route.<rawName>`, as in landed 3c; the real Welcome screen owns its sky, and its text still comes only from the variant `label` hook (`DebugRouteLabel` in debug, null in perf and release).
  - The 3b `LaunchRoute` placeholders are replaced by `Route` values through `LaunchRoute.toRoute()`.
- **No new string.** The label stays 3b's `DebugRouteLabel` with `app_debug_route_x` from `core/ui/src/debug/res` (0c OQ-0c-4). Do not add `app_debug_route` or any other resource.
- **Route probe** (debug, `…/debug/RouteProbe.kt`): `object RouteProbe { @Volatile var state: RootState }`, updated by a debug-only collector. UI tests assert on it or on the `route.*` tag.
- **`PendingLaunchIntent`** (`:core:navigation`, JVM data, no Android types):
  ```kotlin
  data class PendingLaunchIntent(val links: Set<String>, val reminder: ReminderTapExtras?) // toString() redacts links
  data class ReminderTapExtras(val tripId: String, val blockId: String?, val accountId: String?, val inputHash: String?)
  ```
  - `MainActivity.onCreate` and `onNewIntent` call `graph.pendingLaunchIntents.capture(intent)`, which keeps:
    - an `ACTION_VIEW` `https` data URI as a string;
    - the extras `cf.reminder.tripId`, `cf.reminder.blockId`, `cf.reminder.accountId` and `cf.reminder.inputHash` (String). The names are [R]; 12b will use them.
  - The data is held **in memory only**, is never logged, and is never put into saved state.
  - `consume()` returns the merged value once and clears it.
  - `finishLaunch` (after the overlay is gone) does three things: start `gateRouter`, set `RouteProbe`, then pass the consumed intent to `LaunchIntentHandler`. `LaunchIntentHandler` is an interface in `:core:navigation`; the default in AppGraph is a no-op until 5c and 12b.
  - A pending intent never changes the launch ending [F `SceneDelegate` `finishLaunch`].
  - iOS keeps the intent in `LaunchState.pendingIntent` (`LaunchReadiness.swift:72–76`). Add `val pendingIntent: PendingLaunchIntent = PendingLaunchIntent()` to 3b's `LaunchState` and the assertion 3b left for 4e in `endingIsChosenOnceAndIntentNeverRoutes`: the intent passed in is unchanged in the final state (OQ-3b-9). This needs `:feature:launch` → `:core:navigation`; if Konsist forbids it, keep the field out of `LaunchState` and hold it only in `PendingLaunchIntents` (say so in the commit body).
  - [F] iOS shape: `urls: Set<URL>` and `notification: Notification(requestIdentifier, actionIdentifier, userInfoJSON, userText)?`. The Android shape above is the [R] form for intents.

## 6. Kotlin shape [R]
| Class | Module / package | Notes |
|---|---|---|
| New value types §5.1 (`SessionFailure`, `SignOutResult`, `SessionSnapshot`, `SessionUpdate`, `StoredAccountReadiness`) | `:core:model` `net.chronoflow.core.model.session` | JVM |
| `SessionController(credentials, accounts, teardown, io)` (+ `attach(stopSync, requestSync)`), `SessionTeardown`, `SessionAccountStore` (interface: `open(AccountID)`, `upsert(SessionAccount)`, `close()`, `markEmailVerified(AccountID, email): Boolean`, default body throws `SessionFailure.DatabaseUnavailable` as Swift l.83–87) | `:core:sync` `net.chronoflow.core.sync.session` | JVM; inject the `io` dispatcher; the controller owns its confined scope (§5.2) |
| Session test fakes: `MemorySessionStore` (with `reads`, `deletes`, `readFails`, `writeFails`, `writeGate`), `RecordingAccountStore` (`ControllerAccountStore` port, logs `open:<id>`, `upsert:<id>`, `close`), `SessionEventLog` | `:core:sync` **test fixtures** (`java-test-fixtures` on the JVM module; `core/sync/src/testFixtures/kotlin/net/chronoflow/core/sync/session/`) | shared by `:core:sync`, `:core:data` and `:app` tests through `testImplementation(testFixtures(project(":core:sync")))` (OQ-4e-16) |
| `SessionSync.requestStart` (one method added to 4d's file) | `:core:sync` `net.chronoflow.core.sync` | JVM |
| `StoredDataReadiness`, `UnavailableReadiness`, `AccountReadinessSource`, `SessionGateRouter` | `:core:sync` `net.chronoflow.core.sync.session` | JVM |
| `AccountWiper`, `GuestSecrets` (+ `NoGuestSecrets`), `SessionAccountDatabase` (implements `SessionAccountStore` and 4d's `ActiveSyncDatabase`; adds `activeAccountDatabase(): AccountDatabase?`), `AccountReadinessRepository` (implements `AccountReadinessSource`), `SessionHooks` (interface + `NoopSessionHooks`) | `:core:data` `net.chronoflow.core.data.session` | Android lib; JVM tests through `:core:database-testing` |
| `Route`, `TripSheet`, `WizardMode`, `RootState`, `RootNavigator`, `Gate.toRoute()`, `PendingLaunchIntent`, `ReminderTapExtras`, `LaunchIntentHandler` | `:core:navigation` `net.chronoflow.core.navigation` | `@Serializable` routes |
| `AppGraph`, `AppDispatchers`, `GraphOverrides`, `MockSeedFlags`, `SessionDataSources`, `MockSources` (interface), `SessionGraph`, `SessionGraphs`, `AppGraphKey`, `SessionActivation`, `RootContent`, `PendingLaunchIntents`, `LaunchSessionBridge` (+ `LaunchRoute.toRoute()`, OQ-4e-15); 3b's `RoutePlaceholder` (edited) | `:app` `net.chronoflow.app` | |
| `RouteProbe`, `MockSessionSeed`, `DebugAppHooks`, `DebugSeedRouting`, `ScreenSessionFixture` | `app/src/debug` `net.chronoflow.debug` | debug only |
| `PerfMockSources` (implements `MockSources`) | `app/src/perf` `net.chronoflow.app.perf` (3b's source set shared by debug and perf) | `debugAppSeed = BuildConfig.DEBUG` |

## 7. Tasks
One commit per task, in order. Use Conventional Commits, lower-case and imperative, with **no trailer of any kind** (AGENTS "Commits"). Per task, run that task's tests with `make targeted ONLY="<module>:<Class> …"` (module paths without a leading colon). User override (2026-11-18): run `make quick` once at the end. Do not run `make check`, `make perf`, `make perf-timeline`, `make baseline-profile` or other emulator tiers. Run only T13's named emulator smoke once; report exact blocked commands and output. If a run hangs or the same failure survives two fixes, stop and report. Format/lint fixes do not count as failures. Apply Appendix B to every table.

### T1. Session value types and one `SessionPresence` (`refactor(model): add session value types and unify session presence`)
- Add the five new §5.1 types to `net.chronoflow.core.model.session`.
- `grep -rn "SessionPresence" --include=*.kt`. If both `…model.launch.SessionPresence` (3b) and `…model.session.SessionPresence` (4a) exist, delete the 3b file and point every import at the 4a one. Same name, same cases, so this is a pure import change (OQ-4e-1). If 3b's `LaunchReadiness`, `Gate` or `SignedOutReadiness` sits in `:feature:launch` or `:app`, move it to `net.chronoflow.core.model.launch` with the same names.
- Do **not** add `SessionResult`, `SessionAccount`, `AccountPolicies`, `StoredSession` or `SessionStore`, or any decoding: 0d and 4a own them, with tests (`SessionStoreTests`, 0d's contract and `SecretRedactionTest`).
- **Tests** (JVM, `core/model/src/test/kotlin/net/chronoflow/core/model/session/SessionValueTypesTest.kt`):
  - `sessionFailureCarriesNoMessage`: every case has `message == null`;
  - `snapshotToStringRedactsTheToken`: `SessionSnapshot(StoredSession("s3cr3t-value", A), 1).toString()` does not contain `s3cr3t-value`;
  - `needsConfirmationKeepsTheCount`: `NeedsConfirmation(2) == NeedsConfirmation(2)` and `!= NeedsConfirmation(3)`.
- **Edge:** `:konsist` must still pass, so no `android.*` in `:core:model`. 3b's tests (`SignedOutReadinessTest`, `LaunchReadinessTests`) must still pass unchanged apart from imports.

### T2. SessionController: restore, accept, generation (`feat(sync): port the session controller`) [ticks the first half of "SessionController"]
- `…/sync/session/SessionController.kt` per §5.2, with `SessionTeardown` (§5.3) and the `SessionAccountStore` interface.
- Add the session test fakes to `:core:sync` test fixtures (§6 row; apply `java-test-fixtures` in `core/sync/build.gradle.kts`). They port the Swift `ControllerMemoryStore`, `ControllerAccountStore` and `SessionEventLog` from `KitT/AppCoreTests/SessionControllerTests.swift:1–123`. 4a's `MemorySessionStore` lives in `core/model/src/test` and is not visible, so this is the shared copy. If the convention plugin rejects `java-test-fixtures`, copy the three fakes into each test source set that needs them and say so in the commit body.
- **Tests:** `core/sync/src/test/…/session/SessionControllerTests.kt`. Plain JVM with `runTest(timeout = 60.seconds)`, the controller's `io` = a `StandardTestDispatcher` from the test scheduler, `MemorySessionStore` and `RecordingAccountStore` logging into one `SessionEventLog` (`"open:<id>"`, `"upsert:<id>"`, `"close"`, `"write"`, `"delete"`, `"sync"`). Ids are the top-level `syncAccount` / `syncOther` from `:core:testing` (the Swift `sessionAccountA/B`). Gate suspensions with 4d's `TestGate`; polls with 4d's `eventually`. Port every case by name (13 Swift cases at the pin):
  - `acceptOrdersDatabaseCredentialsPublicationAndSyncWithoutFakingMarker`: log == `["open:A","upsert:A","write","sync"]`, epoch == 1.
  - `failuresCannotPublishASessionOrSync`, parameterized over `open`, `upsert` and `keychain`: no publish and no sync.
  - `restoreRetriesUnavailableCredentialsWithoutDeletionAndOpensTheMatchingDatabase`: an upper-case id is restored lower-cased.
  - `absentAndMalformedCredentialsSignOut`, over `null`, `"invalid"` and `{"token":"x","accountId":"../outside"}`.
  - `malformedCredentialsAreRetainedWhenDeletionIsTemporarilyUnavailable`.
  - `invalidSessionResultDoesNotTouchStorage`.
  - `newestConcurrentAcceptWinsAndStaleCompletionCannotWriteCredentials`.
  - `cancelingAnAcceptBeforeOpenCompletesPreventsPublication`.
  - `committedWritePublishesBeforeANewerInvalidAccept`.
  - `cancellationDuringSuccessfulWriteStillCommitsAndPublishes`.
  - `supersedingDuringSyncDoesNotRejectAnAlreadyCommittedAccept`.
  - `failedSameAccountRefreshRetainsThePublishedSession`, parameterized over `open`, `upsert` and `keychain`: after a successful accept, a failing re-accept of the same account keeps `presence`, `epoch`, `storedSession` and the stored blob (Swift l.309–340).
  - `failedAccountSwitchPublishesSignedOut`, parameterized over `open`, `upsert` and `keychain`: a failing accept of `syncOther` leaves `storedSession == null`, keeps the old blob, publishes `SignedOut` at `epoch + 1` (Swift l.342–365).
  - Android-only: `updatesReplayCurrentValueOnlyAfterPresenceKnown`.
- Gate fakes with `CompletableDeferred`, never with delays.

### T3. Sign-out, forced wipe, D23 (`feat(sync): port sign-out and the forced wipe`) [ticks "SessionController"]
- Add §5.3 to `SessionController`.
- **Tests:** `core/sync/src/test/…/session/SignOutTests.kt`. The teardown fakes append to one ordered `log`, with `pendingCount`, `flush` and `logout` scripted. `sleep` is never real time: where Swift writes "Block the deadline until canceled" (`discardAndForcedOrderAndConcurrentRequests`), use `sleep = { awaitCancellation() }`; where it gates the deadline (`logoutDeadlineCancelsRequestAndStillErases`), use a `TestGate` and assert the requested duration is `3.seconds`. Port (10 Swift cases at the pin):
  - `offlinePendingReturnsConfirmationAndPreservesCredentials`: pending 2; flush throws offline → `NeedsConfirmation(2)`, flush called once, no marker, no logout, credentials kept.
  - `discardAndForcedOrderAndConcurrentRequests`, over `forced` = false and true:
    - exactly one marker;
    - order `marker < stopSync < detach < close < erase < delete` (Swift records `stopSync` as `"drain"`; search each later event after the previous one, as Swift `dropFirst` does);
    - logout only when not forced; no flush;
    - two concurrent calls run one flight.
  - `oldUnauthorizedCannotWipeReplacementAndCurrentUnauthorizedWipesOnce` (**D23**).
  - `failedWipeRetriesBeforeAnotherAccountCanOpen`: the next `accept` runs `prepare` (recover) before `open`.
  - `logoutDeadlineCancelsRequestAndStillErases`: `sleep` called with 3 s; logout cancelled; erase and delete still run.
  - `activationCannotCaptureCredentialsDuringReplacement`.
  - `emptyOutboxSkipsFlushBeforeLogoutAndWipe`.
  - `explicitSignOutUpgradesQueuedEpochScopedFlight` (reads `signOutFlight` and `generation` while the replacement `accept` waits in its credential write; poll with `eventually`, never a busy loop on real time).
  - `failedForcedMarkerIsRetriedOnActivation`: `retryUnauthorized`.
  - `staleForcedFlightPreservesReplacementPendingRowsWithoutExplicitDiscard`.
  - Android-only: `cancelingTheCallerDoesNotCancelCleanup`. Cancel the calling job after `beginWipe`; `finishWipe` still runs.

### T4. Wiper and account database seam (`feat(data): add the account wiper and session account database`)
- `…/data/session/AccountWiper.kt` and `GuestSecrets.kt` (`interface GuestSecrets { suspend fun deleteAll() }`, `object NoGuestSecrets`) per §5.3, built on 4a's `WipeMarker` and `SessionStore`.
- `…/data/session/SessionAccountDatabase.kt` [F `Kit/LocalStore/SessionAccountDatabase.swift`]: `class SessionAccountDatabase(storageRoot: suspend () -> File, namespace, driverFactory: AccountDriverFactory, io, detach: suspend () -> Unit) : SessionAccountStore, ActiveSyncDatabase`, wrapping one 4a `AccountDatabase(root, namespace, driverFactory, io)` built on first `open`:
  - `open(id)` reuses the database when the account is the same; otherwise it closes first (4a's `AlreadyOpen` must never surface).
  - `activeAccountDatabase(): AccountDatabase?` returns null while closing; `activeDatabase(): SyncDatabase?` (4d's `ActiveSyncDatabase`) returns `SqlSyncDatabase(it)` for the same database, or null.
  - `upsert(account)`: with no open database, or an open database for another account, throw `StoreFailure.InvalidAccountID` [F `SessionAccountDatabase.swift:30–32`]; otherwise delegate to 4d's `AccountDatabase.upsertSessionAccount(account)` (OQ-A40). Do not re-implement the protections.
  - `markEmailVerified(id, email)` requires the email to match and the id to be the open account.
  - `close()` runs the `detach` callback (`SessionGraphs.drop()`), then `AccountDatabase.close()`. A late attachment resolved before detach is refused.
- **Tests:** JVM, `JdbcAccountDriverFactory` and `TestRoots` from `:core:database-testing` (`testImplementation`), temp dirs.
  - `core/data/src/test/…/session/AccountWiperTests.kt` (4 Swift cases; a test-local `MemoryGuestSecrets : GuestSecrets` with `set(key, value)`/`get(key)` stands in for Swift `MemoryGuestSecretStore`):
    - `memberSignOutClearsGuestSecretsAndRecovery`, parameterized over `unauthorized` = false and true: a real `SessionController` + `SessionAccountDatabase` + `AccountWiper` wired as `prepare = wiper::recover`, `beginWipe`, `erase`, `finishWipe`; `signOut(discardPending = false, unauthorized)` returns `SignedOut`; both guest secrets and `ChronoFlowMock/guest-wizard.json` are gone; a `device-preferences.json` file in the storage root survives; the credentials are gone (Swift l.30–59);
    - `pendingWipeRetriesGuestRecoveryDeletion` (inject a `remove` that fails once for the guest-wizard file);
    - `markerSurvivesPartialFailureAndRestartRemovesDirectoryAndCredentials`;
    - `corruptMarkerBlocksRecoveryWithoutTouchingAnotherNamespace` (asserts `SessionFailure.DatabaseUnavailable`; 4a's `WipeMarkerTests` already proves the byte format, so do not repeat it).
  - `SessionAccountDatabaseTests.kt`:
    - `verificationPreservesStoredFieldsAndRejectsWrongEmailOrSupersededSession`;
    - `closeRejectsAnAttachmentResolvedBeforeDetachAndHidesTheClosingDatabase`;
    - `failedAcceptanceAfterSignInMatchesThePublishedStateAndDatabase`, parameterized over the next id = `syncAccount` and `syncOther` (Swift l.121–161);
    - `acceptancePreservesSyncedMarkerAndAccountAndSourceIsolation`;
    - Android-only `activeSyncDatabaseFollowsTheOpenAccount`: null before `open`, non-null after, null after `close`.
  - `core/data/src/test/…/session/DevicePreferencesWipeTests.kt` (Robolectric `@Logic`, `@Config(sdk = [36])`, because 4a's `DevicePreferences` needs a `Context`; 4a tests DataStore the same way): `devicePreferencesSurviveDeletingAllAccountFiles` (Swift `SessionAccountDatabaseTests` l.196–213): set language `nl` and theme `dark` through `DevicePreferences.shared(context)`, read `DeviceIdentityStore(...).identity()`, open and close an account DB under `noBackupFilesDir`, delete its directory; identity, language and theme are unchanged.

### T5. Session sync start (`feat(sync): start session sync for the current epoch`)
- Add `requestStart(controller, epoch)` to 4d's `SessionSync.kt` per §5.5. Change nothing else in 4d's sync files.
- **Tests** (`core/data/src/test/…/session/`, because they need the real database and 4d's `SyncStore` support; JDBC, `ScriptedSource`, `ManualSyncScheduler`):
  - `SessionSyncReadinessTests.kt`: the two Swift cases, `routedTripUsesTheSessionStoreAndKeepsItsPlanAcrossSyncRestarts` and `storedSessionResolvesWhileProductionSyncSourceStartupIsSuspended`.
  - `SessionWipeTests.kt`: `forcedWipeDrainsLateResponseBeforeCloseAndCannotRecreateFiles`.
  - Android-only `SessionSyncStartTest`: `requestStart` for a stale epoch makes no source call; for the current epoch it makes exactly one debounced cycle (`ScriptedSource.calls` starts with `identity`).
- 4d's `SyncTriggerTests` must still pass unchanged.

### T6. Stored-data readiness (`feat(sync): port stored-data readiness`)
- `…/data/session/AccountReadinessRepository.kt` and `…/sync/session/StoredDataReadiness.kt` per §5.4, plus `UnavailableReadiness`. Reuse 3b's `SignedOutReadiness`.
- **Tests:**
  - `core/data/src/test/…/AccountReadinessTests.kt` (JDBC): `sqliteMarkerAndGatePrecedenceAreObservedTogether`. Also Android-only `pendingWalkthroughRowCountsAsSeen` and `missingPoliciesYieldNullGate`.
  - `core/sync/src/test/…/session/StoredDataReadinessTests.kt` (session test fixtures, a scripted `AccountReadinessSource`, Turbine). Port by name:
    - `cachedOfflineAndFreshMarkerStreamNeverFakesSyncedAt`
    - `protectedCredentialFailureStaysUnknownAndRetriesOnActivation`
    - `cancellationOfUnknownSessionPreservesCredentialsAndStartsNoObservation`
    - `unavailableObservationWaitsForRetryAndEmptyReadIsFalse`
    - `committedVerificationRoutingSurvivesRequestCancellationAndRestore`
    - `missingPoliciesWaitUntilLoadedOrExplicitFirstLoadAllowance`
  - `app/src/test/kotlin/net/chronoflow/app/StoredDataReadinessLaunchTests.kt` (plain JVM): the four Swift cases that drive a real `LaunchCoordinator` with a `ManualLaunchClock` (§2.3), which only `:app` can see (3b's `:feature:launch` and its test fixtures, plus `:core:sync` test fixtures). Same names, same assertions on `coordinator.state` (OQ-4e-16):
    - `freshDataKeepsTwoAndEightSecondRules`
    - `verificationIsReadyWithoutMarkerAndSignedOutDoesNotReadDatabase`
    - `unknownSessionUsesExistingEightSecondFallbackWithoutDeletingData`
    - `verificationDiscoveredDuringFirstPullReleasesLaunchWithoutMarker`

### T7. Gate router (`feat(sync): port the session gate router`) [ticks the first item of "Gate and routes"]
- `…/sync/session/SessionGateRouter.kt` per §5.4.
- **Tests:** `core/sync/src/test/…/session/SessionGateRouterTests.kt`. Ported from `StoredDataReadinessTests`:
  - `gatesContinueAfterAcceptanceAndCancelAtSignOut`:
    - verify → `onVerify`, terms → `Terms`, walkthrough → `Walkthrough`, app → `App`;
    - sign-out → `onSignedOut` once;
    - nothing after that.
  - `signedOutRoutesAndRevokesMutationsBeforeLogoutFinishes`: the permit is null and `onSignedOut` has fired while `logout` is still suspended.
  - `initiallySignedOutKeepsWelcomeRoute`: no callback at all.
  - Android-only: `duplicateGateValuesNavigateOnce`, `staleSnapshotEndsTheGateJob` (after the snapshot changes, later gate values reach no callback) and `startTwiceCollectsOnce`.

### T8. Routes, navigator, pending intent (`feat(navigation): add routes and the root navigator`) [ticks "Gate and routes": routes and `RootNavigator`]
- Add §5.7 and the `PendingLaunchIntent`/`LaunchIntentHandler` part of §5.8 to `:core:navigation`. Make sure `kotlinx-serialization` is applied to the module.
- **Tests** (JVM, `core/navigation/src/test/…`):
  - `RouteSerializationTest`: every route, including `TripPage` with a sheet and every `WizardMode`, round-trips through `Json`. `Reset` has no `token` property (`Route.Reset.serializer().descriptor.elementNames == ["email"]`).
  - `RouteDebugNameTest`: the exact names `welcome`, `signIn`, `twoFactor`, `forgot`, `reset`, `signUp`, `verify`, `guestWizard`, `terms`, `walkthrough`, `today`, `trips`, `settings`, `tripPage`, `wizard`, plus the settings pages.
  - `RootNavigatorTest`: the revision starts at 0 with `route = null`; each `navigate` increments it, including a repeat of the same route.
  - `GateRouteTest`: the four `Gate` mappings. (The five `LaunchRoute` mappings are tested in T10's `LaunchSessionBridgeTest`, because `LaunchRoute.toRoute()` lives in `:app`, OQ-4e-15.)
  - `PendingLaunchIntentTest`: merging two captures unions the links; `consume()` returns the value once and then null; `toString()` contains no link text.
- Add `LaunchState.pendingIntent` and the `endingIsChosenOnceAndIntentNeverRoutes` assertion per §5.8 (OQ-3b-9), in this commit.
- **Konsist:** add `routesAreSerializable` if 0a's rule does not already cover `Route` subclasses.

### T9. App graph, session graph, foreground triggers (`feat(app): wire the app graph and per-epoch session graph`) [ticks "Graphs"]
- `AppGraph`, `AppDispatchers`, `GraphOverrides`, `SessionDataSources`, `MockSources` (+ `PerfMockSources` in `app/src/perf` and the null release twin in 3b's release source set, `app/src/releaseNoPerf/kotlin/`), `SessionGraph`, `SessionGraphs`, `AppGraphKey`, `SessionActivation` and `SessionHooks` wiring per §5.5–§5.6. Add `implementation(project(":core:network"))`, `:core:data`, `:core:sync` and `:core:navigation` to `:app` if missing, and `ACCESS_NETWORK_STATE` if the merged manifest lacks it.
- `controller.attach` (iOS l.98–109):
  - `stopSync = { hooks.onSessionEnding(); sessionSync.stop() }` (reminders detach first, 12b);
  - `requestSync = { sessionSync.requestStart(controller, it) }`.
- The teardown is wired as on iOS (`NativeSessionController.swift:79–97`):
  - `prepare = { sessionAccounts.close(); wiper.recover() }`;
  - `pendingCount = { val db = sessionAccounts.activeAccountDatabase() ?: throw StoreFailure.Closed; OutboxRepository(db).pending().size }`;
  - `flush = { sessionSync.refreshNow() }`;
  - `beginWipe`, `erase` and `finishWipe` go to the wiper;
  - `logout = { dataSources.forSession(it).signOut() }`;
  - `detach = { accountModel.detach(); onSignedOut() }`, where `onSignedOut` is the gate router's sign-out navigation (T10) — iOS `signedOut`;
  - `sleep = { delay(it) }`.
- `SessionAccountDatabase(detach = { sessionGraphs.drop() })` (iOS l.50–54: taps, reminders and model detach; taps and reminders join in 12b).
- Test dependencies of `:app`: `testImplementation(project(":core:database-testing"))`, `testImplementation(testFixtures(project(":core:sync")))`, `testImplementation(project(":core:testing"))`, and 3b's `:feature:launch` test fixtures (already added by 3b Task 5).
- **Tests:**
  - `app/src/test/…/SessionActivationTest` (JVM, fakes): ON_START runs the steps in the §5.5 order; a throwing `retryUnauthorized` stops before `readiness.retry`; ON_STOP calls `setEnabled(false)` and the fake source records no call; a flight running at ON_STOP is cancelled; a following ON_START re-enables and resumes in the §5.5 order.
  - `SessionGraphsTest` (JVM): one graph per epoch; a new epoch cancels the old scope; signed out → `current == null`; an update whose epoch is already stale attaches nothing; `AccountModel.state` becomes `Loaded` after a `SignedIn` update and `Unknown` after `drop()`.
  - `DataSourceSelectionTests` (JVM, `app/src/test`):
    - `defaultsAndIsolation`, the namespace half 4b left to 4e: `Mock` source → `StorageNamespace.Mock`, `Api` → `Api`; the two `directoryName`s and `keychainService`s differ (Swift l.8–16), so the session files and account roots differ;
    - `apiFactoryNeverConstructsMockSeed` (Swift `MockAppSeedTests.swift:210–219`; 4c left it to 4b/4e and 4b did not port it): with `DataSourceFactory("api", …, release = true, testing = false)`, `SessionDataSources.forSession(…)` returns an `ApiDataSource` and the `MockSources` fake is never called.
  - `AppGraphConstructionTest` (`@Logic`, Robolectric): constructing `AppGraph` touches no file. Assert that `StrictMode` with `penaltyDeath` on disk reads does not fire, and that `noBackupFilesDir` gets no new children.
  - `ViewModelExtrasTest` (`@Logic`, Robolectric): a `viewModelFactory` initializer receives the graph through `MainActivity.defaultViewModelCreationExtras`.
- **Port** `I/AppTests/SessionLifecycleTests.swift` (2 cases) as `app/src/test/kotlin/net/chronoflow/app/SessionLifecycleTests.kt` (plain JVM; JDBC driver factory from `:core:database-testing`, `MemorySessionStore`, a temp root). The UIKit window, overlay and signpost assertions are 3a/3b's and are dropped; name them in the commit body.
  - `nativeFirstFlushAndSuspendedOpenUseProductionStoredDataReadiness`: three rows `(verified, cached) → (gate, stored)`: `(true, true) → (App, true)`, `(true, false) → (App, false)`, `(false, false) → (Verify, false)`. Seed the account through `SessionAccountDatabase.open` + `upsert` (policies `"1"/"1"`, accepted `"1"`, `walkthroughSeen = true`; `cached` sets `syncedAt = "2026-11-06T00:00:00Z"`), close it, then build the production `StoredDataReadiness` over a `SessionController` whose `SessionAccountStore` suspends `open` on a `TestGate`. `session()` does not complete while the gate holds; after release, `session() == SignedIn(id)`, `gate()` and `hasStoredData()` match the row.
  - `nativeCompositionObservesAndDetachesAccountsUsingOneKeychainStore`: one `MemorySessionStore`, `SessionGraphs` + `AccountModel` (test dispatcher scope). `accountModel.state` starts `Unknown`; for `syncAccount`, then `syncOther`: `accept` → the model becomes `Loaded` for that id and `sessionGraphs.current.value?.epoch` is the new epoch; after `signOut(discardPending = true)` the model is `Unknown` and `current` is null.
- **Robolectric keystore.** Copy 4a's `SoftwareSessionCipher` (`core/datastore/src/test/…`) into `app/src/test/kotlin/net/chronoflow/app/SoftwareSessionCipher.kt` unchanged (`src/test` serves both `testDebugUnitTest` and `testReleaseUnitTest`) (4a's copy is not visible across modules). Every Robolectric test of T9–T13 that builds a real `AppGraph` passes `GraphOverrides(sessionCipher = SoftwareSessionCipher())` (OQ-4e-17).

### T10. Launch on real readiness, root content, placeholder, probe (`feat(app): launch on stored-data readiness and render root routes`) [ticks "StoredDataReadiness", and the `NavDisplay`, `PendingLaunchIntent` and probe item]
- **Readiness selection** (3b's `readinessFactory`, §5.9 of 3b):
  - a `cf.session|gate|stored|readyDelay` extra present (debug and perf only) → 3b's `StubReadiness(…, nativeReadiness = graph.readiness)`, so `cf.session=native` runs the real session;
  - otherwise **debug, perf and release** use `graph.readiness` (`StoredDataReadiness`), replacing 3b's `SignedOutReadiness` default (OQ-4e-2);
  - if building the session services throws, use `UnavailableReadiness`.
- **`session` trace:** `LaunchTrace` cookie **5** (reserved by 3b): begin in the same callback that ends `first-commit` [R: the plan budget is "≤ 50 ms after `first-commit`"; iOS begins it at scene connect, §2.3]; end in 3b's `onLaunchChange` the first time `state.session != null` (iOS l.437–438). Restore runs on `dispatchers.io` and never on main.
- **Hand-off:** `LaunchSessionBridge` follows 3b's `LaunchController.route` (every non-null value, not only the ending) with the revision guard of §5.4: navigate `route.toRoute()` only while `rootNavigator` revision equals the captured `launchRevision`, then re-capture it. When 3b sets `handOffComplete` (claimed launch) or at once when there is no overlay (Warm activity, `screen` profile), call `finishLaunch` (§5.8) once per process.
- **Verification callback:** `onVerify(epoch)` navigates to `Verify` when `controller.activeSession()?.epoch == epoch` and the current route is not already `Verify`. 5d may tighten this.
- **Gate router callbacks:**
  - `onSignedOut = { rootNavigator.navigate(Route.SignIn) }`;
  - `onGate = { rootNavigator.navigate(it.toRoute()) }`.
- **`screen` profile** (`ScreenSessionFixture`, debug) [F `UITestSessionFixture.route`]. Skip the overlay as 3a/3b already do, then:
  1. `presence = readiness.session()`;
  2. if signed in: `allowFirstLoad()`, then navigate `gate().toRoute()`; otherwise `Welcome`;
  3. `audit = sign-in` → `SignIn`, `forgot` → `Forgot`;
  4. then `finishLaunch`.
- **`RootContent`** in 3b's `CfRoot`, 3b's `RoutePlaceholder` generalised to `Route`, `RouteProbe`, and `PendingLaunchIntents` capture in `MainActivity.onCreate`/`onNewIntent`, all per §5.8. No new string resource.
- **Tests:**
  - `app/src/test/…/LaunchConnectionTests.kt` (JVM): `testLaunchDoesNotOverwriteSessionNavigation` → `launchDoesNotOverwriteSessionNavigation`. A sign-out navigation during the launch makes the launch hand-off a no-op.
  - `LaunchSessionBridgeTest` (JVM): signed out → `Welcome`; signed in with `Terms` → `Terms`; signed in, gate null, forced → `Today`; gate null, not forced → no navigation; a launch route that changes from `Terms` to `Today` with no other navigation in between navigates twice; the five `LaunchRoute.toRoute()` mappings.
  - `RootContentTest` (`@Layout`, Robolectric, `app/src/testDebug`): `navigate(Terms)` shows exactly one `route.terms` (structure only; no copy assertion, per AGENTS); navigating to the same route again recomposes under a new revision (an inner `remember` counter resets); `scenario.recreate()` keeps `route.terms` without a new `navigate`.
  - 3b's `CfRootHandOffTest` and `RoutePlaceholderGuardTest` still pass (the tags `route.welcome`, `route.today`, `route.terms` are unchanged).
  - `PendingLaunchIntentsTest` (`@Logic`, Robolectric): an `ACTION_VIEW https://app.chronoflow.net/x` intent plus a `cf.reminder.tripId` extra are captured; `consume` happens only after `finishLaunch`.
  - `RootContentTest` and any other Robolectric test here that builds a real `AppGraph` passes `GraphOverrides(sessionCipher = SoftwareSessionCipher())` (T9).

### T11. Seeds: session, account and route keys (`feat(debug): seed signed-in sessions and route seeds`) [ticks "Seeds"]
- **`TestSeed` parse additions** (`…/debug/TestSeed.kt`). All of them are gated by `CF_AUTH_TEST_ID` [F `AuthUITestConfiguration.swift`]:
  | Key | Grammar | Field |
  |---|---|---|
  | `CF_AUTH_TEST_SIGNED_IN` | `demo` \| `twofactor`; else null | `signedIn: SeedAccount?` |
  | `CF_AUTH_TEST_WALKTHROUGH_SEEN`, `CF_AUTH_TEST_TERMS_PENDING`, `CF_AUTH_TEST_UNVERIFIED` | `== "1"` | `Boolean` (OQ-4e-4) |
  | `CF_AUTH_TEST_AUDIT_SCREEN`, `CF_AUTH_TEST_AUDIT_STATE` | raw string | `String?` |
  | `CF_MOCK_DELAY` | seconds, finite, 0…60, else 0 | `mockDelay: Duration` |
  | `CF_PLAN_TEST_ROUTE` | `trips` \| `trip:<uuid>` \| `trip:<uuid>:list` | `planRoute: PlanRouteSeed?` |
  | `CF_PLAN_TEST_POSITION` | `summary` | `Boolean` |
  | `CF_PLAN_TEST_SHEET` | `legend` \| `step:<blockId>` | `TripSheet?` |
  | `CF_SETTINGS_ROUTE` | `profile`, `language`, `delete`, `password`, `twoFactor`, `twoFactorSetup`, or a settings route raw name | `settingsRoute: String?` |
  | `CF_WIZARD_AUTO_OPEN` | `== "1"` | `Boolean` |
  | `CF_WIZARD_BOARD_CAPTURE` | raw board name | `String?` |

  An invalid value is null **and** goes into `ignoredKeys`.
- **`DebugSeedRouting`** (`…/debug/`): `@Volatile var current: TestSeed?`, set by `apply`. 6a, 6b, 9c and 10a read it. 4e only consumes the auth audit keys.
- **`MockSessionSeed`** (`…/debug/MockSessionSeed.kt`) [F `Kit/Sync/MockSessionSeed.swift`]:
  - `reset(root, credentials)`: delete the credentials, then delete `root` recursively.
  - `run(root, ns, credentials, source, input, twoFactorCode, driverFactory, io)`:
    1. Skip if `root/signed-in-seed.done` exists.
    2. `signIn(input)`; on `TwoFactor(challengeId)` → `completeTwoFactor` with `challengeId` and `code = twoFactorCode` (callers pass `MockScenarioValues.twoFactorCode`, `"123456"`).
    3. Build a local `SessionAccountDatabase({ root }, ns, driverFactory, io)` and a local `SessionController(credentials, accounts, teardown with sleep = { delay(it) }, io)`, then `accept(result)`.
    4. Require `accounts.activeAccountDatabase()` (the concrete database, rather than the `SyncDatabase` adapter) and `controller.activeSession()` (else `SessionFailure.DatabaseUnavailable`); `FullPull(SqlSyncDatabase(db), source, now = { Instant.now() }).run(SyncEpoch(AccountID.of(result.account.id), session.epoch))` (4d).
    5. `accounts.close()`.
    6. Write the empty marker file atomically. On any error in 3–6: `accounts.close()` (errors ignored, cancellation rethrown) and rethrow.
- **`TestSeeds.apply`** follows `13 §5.2` steps 1–7, plus 0e's language and theme steps. It runs on `io`:
  1. On the current graph: `gateRouter.stop()`, `sessionSync.stop()`, then `sessionGraphs.drop()` (cancels the current `SessionGraph` scope).
  2. `graph.sessionAccounts.close()`.
  3. Delete and recreate `filesDir/uitests/<id>/` (0e).
  4. Delete the test session alias `ChronoFlowMock.uitest.<id>` and its blob. Clear the test DataStore if 0e/4a created one.
  5. `DebugAppHooks.resetGraph(app, GraphOverrides(storageDir = filesDir/uitests/<id>, mockDelay = seed.mockDelay, sessionAlias = "ChronoFlowMock.uitest.<id>", mockSeed = MockSeedFlags(unverified, walkthroughSeen, termsPending), sessionCipher = <the cipher passed to apply, default null>))`. The graph's `MockDataSource` uses `FileMockStateStore(storageDir)` (`<storageDir>/ChronoFlowMock/Server/state.json`) and receives `MockAppSeed.make(now, unverified, walkthroughSeen, termsPending)` as its **`seed`** argument (§5.6, iOS §2.3); the directory is fresh, so the first load takes the seed. The session file is `KeystoreSessionStore(File(storageDir, "ChronoFlowMock/session.bin"), "ChronoFlowMock.uitest.<id>", cipher, io)` (4a: file and alias are overridable).
  6. If `signedIn != null`: `MockSessionSeed.run(root = storageDir, ns = StorageNamespace.Mock, credentials = graph.sessionStore, source = graph.dataSources.public(), input = PasswordSignInInput(email = MockScenarioValues.demoEmail | twoFactorEmail, password = MockScenarioValues.password, deviceId = testId, deviceName = "UI test"), twoFactorCode = MockScenarioValues.twoFactorCode, driverFactory = AndroidAccountDriverFactory(context), io)`.
  7. The stored gate is now in the DB (the full pull wrote the account, policies and walkthrough). Log `CF_UI_TIMELINE … event=seed-applied signedIn=<demo|twofactor|none>`.

  Without `CF_AUTH_TEST_ID`, none of this runs. Seeded state lives for the process only (OQ-4e-7).
- **Graph reset.**
  - Main code defines the `GraphOverrides` data class of §5.6 with `GraphOverrides.None`. `AppGraph(app, overrides = GraphOverrides.None, …)` is the production constructor. Preserve the existing `fontPreloader` and `launchProvider` constructor parameters used by landed launch tests; these inject dependencies and perform no I/O. Add overrides without breaking those named call sites.
  - `TestSeeds.apply` gains a trailing parameter `sessionCipher: SessionCipher? = null`, forwarded into `GraphOverrides`; Robolectric tests pass `SoftwareSessionCipher()`.
  - `ChronoFlowApp.graph` (the 0a `lateinit var`) gets an `internal set`.
  - Only `app/src/debug/…/DebugAppHooks.kt` (`fun resetGraph(app, overrides)`) calls the setter: it calls the old graph's `close()` (cancels `appScope` and the scopes owned by its controller, readiness and session sync), then assigns `AppGraph(app, overrides)`. Add `AppGraph.close()` (main, used only by this hook) in T9.
  - Release has no caller, so there is no reset path.
- **Tests:**
  - `app/src/testDebug/…/TestSeedSessionKeysTest` (JVM): every row of the table, with valid, invalid and ungated input. `CF_MOCK_DELAY` cases: `0`, `60`, `60.1`, `-1`, `NaN`, `abc`.
  - `MockSessionSeedTests` (JVM, JDBC, temp root, in-memory credentials). Port:
    - `signedInSeedUsesRestorableCredentialsAndAccountGates`, parameterized over `demo|twofactor × app|verify|terms|walkthrough`: `gate` as expected; `hasStoredData == (gate != Verify)`; re-running after deleting the credentials does not resurrect the session.
    - `reseedRemovesPreviousAccountQueueAndCompletionMarkerAndMatchesColdSeed`.
  - `ReseedEqualsColdSeedTest` (JVM, `app/src/testDebug`). Extract steps 2–6 of `apply` into `internal class SeedRunner(storageDir: File, credentials: SessionStore, driverFactory: AccountDriverFactory, source: DataSource, io)` with `suspend fun reseed(signedIn: SeedAccount?, testId: String)` (reset = `MockSessionSeed.reset(storageDir, credentials)`, then `MockSessionSeed.run` when signed in). `TestSeeds.apply` builds it from the production instances; the test builds it with `JdbcAccountDriverFactory()`, a `MemorySessionStore` and `MockDataSource(MemoryMockStateStore(), seed = MockAppSeed.make(Instant.ofEpochSecond(1_800_000_000), walkthroughSeen = true), delay = { 0.0 }, sleep = {}, bearer = { credentials.readStoredSession()?.token })` (Swift `MockSessionSeedTests` l.65–71):
    1. cold-seed `twofactor` → `SeedSummary(id, email, walkthrough, verified, trips.sorted(), pending, synced)`;
    2. seed `demo` and enqueue one settings change;
    3. reseed `twofactor` with the same test id.

    Assert summary == cold, the `Accounts/0199a3c0-0000-7000-8000-000000000000` directory is gone, and the marker was recreated.

### T12. Release readiness (`test(app): prove release ships no session seed or mock code`) [ticks "Release readiness"]
- Extend `app/src/testRelease/kotlin/net/chronoflow/app/ReleaseReadinessTest.kt` with `sessionSeedCodeIsAbsent`. `Class.forName` throws `ClassNotFoundException` for each of:
  - `net.chronoflow.debug.MockSessionSeed`
  - `net.chronoflow.debug.RouteProbe`
  - `net.chronoflow.debug.DebugSeedRouting`
  - `net.chronoflow.debug.ScreenSessionFixture`
  - `net.chronoflow.debug.DebugAppHooks`
  - `net.chronoflow.app.perf.PerfMockSources`
- 4c's `mockDataSourceIsAbsent` and 3b's `DebugRouteLabel`/`StubReadiness` lines already cover the mock and the label; do not repeat them (OQ-4c-13). `RoutePlaceholder` is main code and does ship (no text in release).
- Assert that `net.chronoflow.core.ui.R.string::class.java.fields` has no `app_debug_route_x`.
- `./gradlew :app:verifyReleaseManifest` still finds no exported debug component.
- Record `apkanalyzer dex packages app/build/outputs/apk/release/*.apk | grep -c datamock` == 0 in `docs/tooling.md`.
- **Port:** `I/ReleaseReadinessTests/ReleaseReadinessTests.swift` (the 4e parts; `testReleaseDefaultMockDoesNotSeedAccounts` is covered by 4c's class-absence test, and `testReleaseKeepsOnlyReadyPlaybackAndStallDiagnostics` by 3b):
  - `testReleaseIgnoresUITestSeedsAndHasNoTestURLRegistration` becomes `releaseIgnoresSeedExtras`: a Robolectric release unit test (`app/src/testRelease`, `@Config(sdk = [36])`) launches `MainActivity` with `CF_AUTH_TEST_ID` plus `CF_AUTH_TEST_SIGNED_IN=demo` and asserts that `noBackupFilesDir/ChronoFlowMock` and `filesDir/uitests` do not exist. Release has no `GraphOverrides` caller, so the real restore fails on Robolectric's missing AndroidKeyStore; that is expected and must not crash the activity (it stays `CredentialsUnavailable` until retry).
  - The 4e half of `testReleaseIgnoresArgumentsRestoresNativeSessionAndKeepsPerfIsolated` becomes `releaseRestoresNativeSession` (plain JVM in `testRelease`): a `StoredDataReadiness` over a `SessionController` with a `MemorySessionStore` holding `StoredSession("t", syncAccount)` and an accepting fake account store returns `SignedIn(syncAccount)` from `session()` with exactly **1** credential read; `SignedOutReadiness.session()` then reads nothing more (Swift l.101–115).

### T13. Launch per gate state and the emulator smoke (`test(app): prove cold launches route by session and gate`) [ticks "Tests"]
- **`LaunchGateRoutingTest`** (`@Logic`, Robolectric `@Config(sdk=[36])`, `app/src/testDebug`). One test per state:
  - no seed → `route.welcome`. Name this case `nativeSignedOutLaunchAndForegroundWithoutAnIntentDoNotRequestPermissionOrChangeNavigation` (port of `LaunchConnectionTests` l.122–163): after the launch, drive `SessionActivation` ON_START and hand an empty `PendingLaunchIntent` to the launch-intent path; the route stays `Welcome` and the `RootNavigator` revision is unchanged. The permission assertions of the Swift test join in 12b;
  - `demo` + `WALKTHROUGH_SEEN=1` → `route.today` (the mock's `walkthroughSeen` defaults to false, so `demo` alone lands on the walkthrough);
  - `demo` + `UNVERIFIED=1` → `route.verify`;
  - `demo` + `TERMS_PENDING=1` → `route.terms`;
  - `demo` with `WALKTHROUGH_SEEN` absent → `route.walkthrough`;
  - `demo` + `WALKTHROUGH_SEEN=1`, then a forced 401 through `controller.unauthorized(epoch)` → `route.signIn`, and `Accounts/` is empty.

  Use `TestSeeds.apply(…, sessionCipher = SoftwareSessionCipher())` in-process with profile `screen` and the `io` dispatcher. Each test stays under 5 s.
- **`SignedInColdLaunchSmokeTest`** (`@Smoke`, emulator, extends `ScreenTest`). Seed `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1`, `CF_UI_TEST_PROFILE=launch`. Assert:
  - `waitUntilExactlyOneExists(hasTestTag("route.today"), 10_000)`;
  - `RouteProbe.state.route == Route.Today`;
  - the test takes less than 10 s.

  This is the only emulator test in this phase.

### T14. Perf number and docs (`docs(session): record the session trace and phase 4e results`)
- Add `TraceSectionMetric("session", Mode.First)` to `StartupBenchmark.startupCold` (signed out: a fresh install, no seed). The user will run `make perf`; do not run it in this phase implementation session. Record the pending command and budget, never an invented measurement. Append the row to `docs/perf-startup.md` (budget ≤ 50 ms after `first-commit`, ≤ 100 ms low-end; **report only**).
- Tick the 4e boxes. Append to plan.md "Open questions for the user" (heading; starts at line 580 at refresh; highest existing id OQ-A169, so start at OQ-A170), as the next free `OQ-A` numbers with their defaults, the questions that change plan text or bind another phase: OQ-4e-2, -3, -4, -5, -7, -10, -13, -15, -16 and -17 (cite the `OQ-4e-n` id in each row, as 4d did). Apply their plan-text consequences in the same commit:
  - A4 (l.91): "`SessionGraph` is built per session epoch: DB, repositories, SyncEngine, reminder scheduler" → "DB and repositories, with the shared `SessionSync` (which owns the per-epoch `SyncEngine`); the reminder scheduler joins in 12b" (OQ-4e-3). Same change in the Phase 4e "Graphs" task line.
  - Phase 4e task "StoredDataReadiness replaces `StubReadiness` in debug and release; the stub stays in perf" → "…replaces the default `SignedOutReadiness` in every variant; `StubReadiness` runs only when a stub extra is present" (OQ-4e-2).
  - "Android spec: seeds and test hooks" (l.467): add `CF_AUTH_TEST_UNVERIFIED` and `CF_AUTH_TEST_TERMS_PENDING` (OQ-4e-4).
  - "Android spec: navigation and routes" (l.402): `Reset(email, token)` → `Reset(email)`, the token held in memory (OQ-4e-5).
- Note in `docs/tooling.md` the seeded signed-in `adb` command:
  ```sh
  adb shell am start -n net.chronoflow.app.native.debug/net.chronoflow.debug.SeedReceiverActivity \
    --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1
  ```

## 8. Acceptance criteria
- `make quick` passes (once, at the end); `make check` is deferred to the user, and `./gradlew :konsist:test :app:testReleaseUnitTest :app:verifyReleaseManifest` passes.
- This targeted run passes (0a `Scripts/targeted.py` syntax, module paths without a leading colon):
  ```sh
  make targeted ONLY="core:model:SessionValueTypesTest core:sync:SessionControllerTests core:sync:SignOutTests core:sync:StoredDataReadinessTests core:sync:SessionGateRouterTests core:data:AccountWiperTests core:data:SessionAccountDatabaseTests core:data:DevicePreferencesWipeTests core:data:AccountReadinessTests core:data:SessionSyncReadinessTests core:data:SessionWipeTests core:data:SessionSyncStartTest core:navigation:RouteSerializationTest core:navigation:RouteDebugNameTest core:navigation:RootNavigatorTest core:navigation:GateRouteTest core:navigation:PendingLaunchIntentTest app:StoredDataReadinessLaunchTests app:SessionLifecycleTests app:SessionActivationTest app:SessionGraphsTest app:DataSourceSelectionTests app:AppGraphConstructionTest app:ViewModelExtrasTest app:LaunchConnectionTests app:LaunchSessionBridgeTest app:RootContentTest app:PendingLaunchIntentsTest app:TestSeedSessionKeysTest app:MockSessionSeedTests app:ReseedEqualsColdSeedTest app:LaunchGateRoutingTest"
  ```
- 4d's `SyncTriggerTests` and `LoginUpsertTests`, and 3b's `LaunchReadinessTests`, `CfRootHandOffTest` and `StubReadinessTests`, still pass unchanged.
- The teardown order and D23 tests pass: `SignOutTests.discardAndForcedOrderAndConcurrentRequests` and `SignOutTests.oldUnauthorizedCannotWipeReplacementAndCurrentUnauthorizedWipesOnce`.
- Cold launches route correctly:
  - Robolectric `LaunchGateRoutingTest`: all six states;
  - emulator `SignedInColdLaunchSmokeTest` on `pixel`, under 10 s;
  - a manual cold launch with no seed (`am force-stop`, then a launcher start) ends on Welcome.
- No new emulator test except the smoke test. JVM tests stay under 1 s each and Robolectric tests under 5 s each (the tier timing listener reports no violation).
- `docs/perf-startup.md` lists the pending `session` measurement for the user: cold-start median from `StartupBenchmark` (report only; ≤ 50 ms reference / ≤ 100 ms low-end after first-commit).
- `apkanalyzer` shows 0 `datamock` packages in release.
- No screenshots: this phase adds no designed UI. `RoutePlaceholder` shows text only in debug.
- `grep -rn "class SessionResult\|class SessionAccount\|class StoredSession\|interface SessionStore\|class SyncLifecycle\|class AccountModel\|class ConnectivitySyncPathMonitor" --include=*.kt` lists one definition each, all from 0d, 4a or 4d (none added by this branch).

## 9. Review checklist (Opus reviewer)
1. **Accept order.** In `performAccept`, the steps run DB `open`/`upsert`, then the credentials write, then `publish`, then `requestSync`. There is no `check(gen)` between a successful write and publish.
2. **Teardown order.** It matches §5.3 steps 4–12 exactly. Logout is skipped only when unauthorized. The marker is written before `publish(SignedOut)`. `publish(SignedOut)` comes before `stopSync`.
3. **D23.** `unauthorized(epoch)` compares epochs before it enqueues anything. `SessionSync`'s `unauthorized` callback passes the epoch it was built with, never the current one.
4. **Cleanup cannot be cancelled.** `performSignOut` runs as a job in the controller's own scope, never a child of the caller. The credentials write in `accept` and the publish after it share one `NonCancellable` block. No `Mutex` is held across a suspension anywhere in the controller (4d §5.3).
5. **Cancellation.** No `runCatching` sits around suspend calls, and every `catch (e: Exception)` rethrows `CancellationException`.
6. **3 s bound.** The race uses the injected `sleep`. The test proves the loser is cancelled. There is no real `delay(3000)` in tests.
7. **Wipe marker retry.** `prepare` = `close` + `recover` runs on every `restore` and `accept`. A corrupt marker blocks without deleting anything.
8. **Secrets:**
   - no token or link secret in a route key, `toString()`, log, `Bundle` or `SavedStateHandle`;
   - `Route.Reset` has no token;
   - `PendingLaunchIntent.toString()` is redacted;
   - `grep -rn "Log\.\|println" core/sync core/data/src/main/kotlin/net/chronoflow/core/data/session` shows nothing that prints a session.
9. **No I/O before the first frame.** `AppGraph` construction is lazy (test). Restore starts after `first-commit` on `io`.
10. **Back stack never saved.** There is no `rememberNavBackStack`, `rememberSaveable` or `SavedStateHandle` for routes. Process death becomes a cold launch (A8).
11. **Launch versus session navigation.** The revision guard is present and tested.
12. **Module rules.** `:core:sync` and `:core:model` import no `android.*`. Features import no features. `:core:navigation` holds no Compose UI beyond what Nav3 keys need.
13. **Debug-only.** `RouteProbe`, `MockSessionSeed`, `DebugSeedRouting`, `ScreenSessionFixture` and `DebugAppHooks.resetGraph` are in `src/debug`; `PerfMockSources` is in `src/perf`. The release readiness test covers each. No new string resource was added; `app_debug_route_x` is still named only from `app/src/debug`.
14. **Foreground triggers.** ON_START and ON_STOP are serialized. ON_STOP only disables sync: no flush, no source call, no stop job. There is no WorkManager.
15. **Ported test names** match iOS one-to-one. The Android-only extras are marked as such in KDoc.
16. **Commits.** One per task, Conventional, lower-case, with no `Co-Authored-By` or any other trailer.
17. **No duplicates of upstream types.** `SessionPresence` exists once (4a's package). Nothing from §2.5 was redefined. `SessionSync.kt` gained only `requestStart`. `SessionAccountDatabase.upsert` delegates to `upsertSessionAccount`.
18. **iOS wiring parity.** `pendingCount` throws `StoreFailure.Closed` with no open database (so a voluntary sign-out with a closed DB fails rather than silently signing out); `detach` detaches `AccountModel` before navigating; `isCurrent` compares session **and** epoch; the `unauthorized` callback swallows non-cancellation errors.
19. **Variant boundaries.** `:app` main imports nothing from `net.chronoflow.core.datamock` or `okhttp3`; the mock comes only through `MockSources`; `selection`/`namespace` follow `DataSourceFactory`.

## 10. Open questions (proceed on the default; T14 adds the plan-relevant ones to plan.md)
| ID | Question | Default |
|---|---|---|
| OQ-4e-1 | 3b (§5.1) and 4a (Task 1) each define `SessionPresence`, in `…model.launch` and `…model.session` | Keep 4a's (`net.chronoflow.core.model.session`), delete 3b's copy in T1; `Gate`/`LaunchReadiness`/`SignedOutReadiness` stay in `…model.launch` |
| OQ-4e-2 | Plan says "the stub stays in perf", but the `session` budget is measured with `StartupBenchmark` on a perf build | Perf uses 3b's `StubReadiness` only when a stub extra is present. Otherwise every variant runs `StoredDataReadiness`, so the `session` trace measures the real signed-out restore |
| OQ-4e-3 | Plan A4 says the `SessionGraph` holds the `SyncEngine`, but 4d's `SessionSync` owns one lifecycle and engine per epoch (with the retry state kept across pauses) | `SessionGraph` holds DB and repositories and exposes the shared `SessionSync`; it builds no engine. Fix the A4 wording in T14 |
| OQ-4e-4 | The plan's seed list lacks `CF_AUTH_TEST_TERMS_PENDING` and `CF_AUTH_TEST_UNVERIFIED`, which the gate-state tests need | Add both, with the iOS names and the `== "1"` rule (`AuthUITestConfiguration.swift:29–31`) |
| OQ-4e-5 | The spec route `Reset(email, token)` would put the link secret into the saved-state `Bundle` through Nav3's `contentKey = key.toString()` | `Reset(email)` only. 5c keeps the token in an in-memory `AuthLinkHolder` in `AppGraph` |
| OQ-4e-6 | Where the debug placeholder copy lives | Reuse 3b's `DebugRouteLabel` and 0c's `app_debug_route_x` (`core/ui/src/debug/res`); add no string |
| OQ-4e-7 | Seeded storage roots after process death: iOS re-reads the environment on every launch, but Android gets the seed only through the forwarded intent | Seeded state lives for the process only. After process death the app uses the normal mock root. A `launch`-profile test that kills the process must reseed |
| OQ-4e-8 | iOS `SessionGateRouter` takes a navigator; `:core:sync` cannot see `:core:navigation` | Callbacks `onSignedOut`, `onGate` and `onVerify`; `:app` maps them to routes |
| OQ-4e-9 | Withdrawn (review 2026-11-16): ON_STOP makes no flush, as on iOS, so there is no flush duration to bound | — |
| OQ-4e-10 | Notification tap extras names before 12b exists | `cf.reminder.tripId`, `cf.reminder.blockId`, `cf.reminder.accountId`, `cf.reminder.inputHash` (String extras). 12b may rename them in the same commit as its sender |
| OQ-4e-11 | Audit screens `sign-up`, `reset` and `two-factor` need flows that do not exist yet | 4e routes `sign-in` and `forgot` only. 5a, 5c and 5b add their cases to `ScreenSessionFixture` |
| OQ-4e-12 | Verify routing on iOS also checks "the verify model is active" (`AppDelegate.verificationRequired`) | 4e checks the epoch and dedupes against the current route. 5d adds the model check |
| OQ-4e-13 | How `:app` main gets the mock without depending on `:core:data-mock` (debug and perf only) | A `MockSources` interface in main, supplied through the variant hook that carries 3a's `LaunchOverrides`; `PerfMockSources` in `app/src/perf` (debug + perf), null in release |
| OQ-4e-14 | iOS `setActive` also recovers the guest wizard store and reschedules reminders | `SessionHooks.onForeground()` no-op now; 11a and 12b fill it in |
| OQ-4e-15 | 3b put `LaunchRoute`/`LaunchState` in `:feature:launch` (OQ-3b-8), so `:core:navigation` cannot map them | `LaunchRoute.toRoute()` lives in `:app` (`LaunchSessionBridge.kt`) and is tested there; `Gate.toRoute()` stays in `:core:navigation` |
| OQ-4e-16 | Four `StoredDataReadinessTests` need 3b's `LaunchCoordinator` and `ManualLaunchClock`, which `:core:sync` cannot see; the session fakes are needed in three modules | Those four run in `app/src/test` (`StoredDataReadinessLaunchTests`, same names); the fakes live in `:core:sync` `java-test-fixtures` (local copies only if the convention plugin refuses) |
| OQ-4e-17 | Robolectric has no AndroidKeyStore provider, but the gate-routing, root-content and seed tests build a real `AppGraph` | `GraphOverrides.sessionCipher`; Robolectric tests pass a copy of 4a's `SoftwareSessionCipher`. The real Keystore path is covered by 4a's instrumented `KeystoreSessionStoreTest` and this phase's emulator smoke test |

## Appendix B. Table-driven test rule

Follow 0d §4.2 and landed 4d Appendix B throughout T1–T14: up to five rows become independent `<swiftName><Row>` methods calling a private helper; longer tables get their own JUnit4 `Parameterized` class holding only that test, with the Swift method name and readable `@Parameters(name = "{0}")` row names. Never loop over test cases inside a test. Keep every test class listed in this brief, including Android-only classes. Coroutine tests use `runTest(timeout = 60.seconds)` and `backgroundScope` for long-lived collectors.
