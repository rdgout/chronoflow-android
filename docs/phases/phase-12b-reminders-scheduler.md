# Phase 12b brief: Reminder scheduling and delivery

- **Branch:** `feat/phase-12b-reminders-scheduler`, stacked on the stack top once **12a**, **4e**, **5c** and **6d** are in it (review 2026-11-16). Through them the stack also carries 4a (account DB, `DevicePreferences`, reminder tables), 4d (repositories, `SyncEvent`, `LocalMutations`), 1a (`PlanDecoder`, `CopyCatalogProvider`, `CopyLineFormatter`), 0b (`ic_stat_moon`), 0c (`CopyIndex`), 0e (seeds, tiers, `resolveLanguage`), **5c** (`PendingLaunchIntents.capture(intent, restored)`, `takeLinks`), **6a** (`ShellNav`, `ShellViewModel` *[Refresh R3: there is no `ShellHolder`]*, `AppShell`), 6b (`trip.page`) and **6d** (`step.sheet`, the one-time `Step(blockId)` open). If 12a, 4e, 5c or 6d is missing, stop and report it.
- **Depends on:** 12a, 4e, 5c, 6d. **Wave:** 12. **Milestone:** M5.
- **Plan entry:** `plan.md` "### Phase 12b: Reminder scheduling and delivery" (l.3470–3510; *[Refresh R1]*); decisions A4 (l.91), A5 (l.92), A7 (l.94), A13 (l.100); spec "manifest and permissions" (l.450–464); OQ-A14, OQ-A15, OQ-A33 (l.616, 617, 635).
- **Brief written:** 2026-11-16 from iOS at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`), research `07` §7.2–§7.3 and `14` §6–§7, and the merged-phase briefs named in §2.
- **Checked 2026-11-16** (adversarial pass against iOS `f391a1fa` `Sources/Reminders`, `Tests/RemindersTests`, `App/AppTabs.swift`, `App/Session/*`, the boards, plan.md, AGENTS.md and the 4a/4d/4e/5c/6a/6b/6d/0b/0c/0e/1a/3a/12a briefs). Fixes: `AppTabs` range 370–385 and iOS `setLanguage`; `received_at` is ISO text; the service tests run the store on the test dispatcher; `sets` records only successful sets; the permission matrix is literal rows; due rows are cancelled once reminders turn off; the worker's catch list; the receiver test no longer expects unfinished work; `ApplicationWorkConfigurationTest` imports nothing from `androidx.work`; `AppReminderRuntime` takes injected pieces and starts `SessionGraphs` in a worker-only process; the cold-tap reducer test moved to `:app` (core cannot see `:feature:launch`); the journey reads the trip id; tags verified; `:core:database-testing`; OQ-12b-6 platform window detail; OQ-12b-20 (scheduler placement vs A4); Task 3 ticks nothing.
- **Tests:** follow AGENTS.md "Tests". Logic → JVM; Android adapters → Robolectric `@Logic` (`@Config(sdk = [36])` only); exactly **one** emulator journey (`@Smoke`).

## Refresh notes (2026-11-22, against 147aa187)

Checked against the code at `147aa187` (phases 0a–12a on `feat/phase-12b-reminders-scheduler`), iOS at `f391a1fa`, `plan.md` and AGENTS.md. **Where a note and the body below disagree, the note wins.** The worker repeats these checks first (`git grep` each name; read each cited line) and stops on any mismatch not listed here. Scope and deliverables are unchanged. Path aliases: `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/cdr/` = `core/data/src/main/kotlin/net/chronoflow/core/data/reminders/`; `…/cr/` = `core/reminders/src/main/kotlin/net/chronoflow/core/reminders/`; `…/nav/` = `core/navigation/src/main/kotlin/net/chronoflow/core/navigation/`.

**R1. Plan, OQ, research and iOS references.**
- `plan.md` "### Phase 12b" is **l.3470–3510** (not 2662–2702). Boxes: **Scheduler** l.3489, **Delivery** l.3492, **Upkeep** l.3495, **Taps** l.3499, **Teardown** l.3500, **Permission model** l.3501, **Tests** l.3502; acceptance l.3507–3510. Decisions: A4 l.91, A5 l.92, A7 l.94, A13 l.100. Spec: launch sequence l.321–346 (step 5 "After `first-commit`" l.337–342, "notification channel upkeep" l.341); `Route.TripPage` l.402; manifest and permissions **l.450–464** (allowlist l.453, `STARTUP_INITIALIZER` l.464); seeds l.466–478. OQ-A14 **l.616**, OQ-A15 **l.617**, OQ-A33 **l.635**. "Open questions for the user" starts **l.597**; its last row is **OQ-A566** (12a OQ-12a-10, l.1167).
- Earlier OQs as plan rows: OQ-4d-17 = **OQ-A171** (l.772), OQ-4e-3 = **OQ-A173** (l.774, "The reminder scheduler joins in 12b"), OQ-4e-10 = **OQ-A177** (l.778), 4a-6 = OQ-A118 (l.719), 12a OQ-12a-3/-5/-6/-7 = **OQ-A559/A561/A562/A563** (l.1160–1164). OQ-4e-14 has no plan row (it lives only in the 4e brief).
- **This phase's §9 rows map to OQ-A567 … OQ-A586, in order**: OQ-12b-1 → A567, -2 → A568, -3 → A569, -4 → A570, -5 → A571, -6 → A572, -7 → A573, -8 → A574, -9 → A575, -10 → A576, -11 → A577, -12 → A578, -13 → A579, -14 → A580, -15 → A581, -16 → A582, -17 → A583, -18 → A584, -19 → A585, -20 → A586. Task 10 appends them after l.1167 in the 12a format: `| OQ-A567 | (12b OQ-12b-1) <question> | <default> | 12b |` (not `| OQ-12b-<n> | …`). A new question takes **A587** and up. The body keeps citing the local numbers.
- **A4 wording (Task 10) has changed since the brief was written.** l.91 now reads "`SessionGraph` is built per session epoch: DB and repositories, with the shared `SessionSync` (which owns the per-epoch `SyncEngine`); the reminder scheduler joins in 12b (OQ-A173)." Replace only the clause after the semicolon with "the reminder scheduler is one process-wide service in `AppGraph`, attached per account DB (OQ-A586)". Make the same change to the 4e task text at l.2335 and add "(12b: see OQ-A586)" to OQ-A173's default (l.774).
- Research: `07` §7.1–§7.3 l.452–516 ✓; `14` §6–§7 l.297–370 ✓.
- iOS at `f391a1fa`: every cited file exists. Corrections: iOS `plan.md` "Phase 19" is **l.2212–2231** (19c l.2218 ✓); `App/AppTabs.swift` language `.task(id: locale)` is l.340 ✓ and the tap `.onChange(of: native?.reminderTaps.pending, initial: true)` block is **l.371–385** (l.370 closes the previous `.task`); `ReminderTap.swift` has 51 lines (tap + router l.1–31 ✓; l.33–50 is the DEBUG `MockReminderCenter`, which 12c ports). Hold: `ReminderService.swift` (158; inputs l.7–40, `language = .en` l.49, attach l.57–74, `setLanguage` l.79–82, flight/rerun l.83–100, `schedule` l.101–142, `detach` l.143–156), `SystemReminderCenter.swift` (76; mapping l.8–18, `add` l.45–57, trigger l.58–64, `removeDelivered` l.68–74), `ReminderSchedule.swift` l.17–36, `ReminderLedger.swift` (`elapsed` l.31–34; `read/write/clear` l.35–45, settings section `reminderLedger`), `ReminderTimeObservers.swift:8`, `NativeSessionController.swift` (245; detach closure l.50–54, `stopSync` l.100–103, `setActive` l.152–162 with `reminders.reschedule()` l.156, `observe` l.186–217: sync `Finished(idle)` l.189–193, attach l.206, stale detach l.207–210; `deleteAccount` l.114–118 signs out through the same teardown), `Docs/phase-19-reminders.md` ¶3–¶5 = l.7, 9, 11, `StepSheet.swift:80` (`step.sheet`) ✓.
- iOS tests and where they land: `ServiceTests.swift` (suite `ReminderServiceTests`, 7 tests l.18–203) → 12b Task 3; `SignOutTests.swift` (`ReminderSignOutTests.signOutWipesNotificationsLedgerAndAccountDirectory` l.16–57) → 12b Task 8; `TapTests.swift` (`ReminderTapTests.coldIntentPreservesEndingAndWarmRouterCanReopen` l.8–27, `capturedJSONAndTripStep` l.29–53) → 12b Task 9; `PermissionTests.swift` `absoluteTriggersSurviveDSTAndDeviceZoneChanges` l.25–37 → 12b Task 4, `nativeAuthorizationMatrix` l.38–43 → 12b Task 2, `mockAuthorizationChangesOnlyAfterAnExplicitRequest` l.9–16 → **12c** (12c brief l.278, 312 already takes it, with the DEBUG `MockReminderCenter`), `permissionMatrix` l.18–24 → already ported by 12a (`core/reminders/src/test/…/ReminderPermissionTests.kt`). 12a's brief (l.452) deferred all of these to 12b; this split stands. `ReminderTestSupport.swift` `FakeReminderCenter` l.55–98, `ReminderTestStore` l.100–131.

**R2. What 12a built (verify; do not rebuild or change).** All in `:core:reminders` (`…/cr/`, package `net.chronoflow.core.reminders`, pure JVM, `api(:core:model)`, `api(:core:plan)`):
- `ReminderSettings.kt`: `data class ReminderSettings(light, avoid, caffeine, winddown = true, leadMin = 30)` with `enabled`, `settingEnabled(Boolean)`, companion `isLeadOption`, `decode(json: String?): ReminderSettings?`, `enabledPatch(Boolean): JsonObject` (the four flags), `leadPatch(Int): JsonObject?` (`lead_min`); top-level `fun reminderTimeFormat(preferencesJson: String?): TimeFormat` (`Value12` only for the JSON **string** `"12"`); `enum class ReminderPermission { Undetermined, Granted, Blocked, Unsupported }` with `remindersActive(settings: ReminderSettings?)`; `enum class ReminderKind(raw, copyKey)` with `of(raw)`.
- `ReminderSelection.kt`: `ReminderMember`, `ReminderDraft`, `ReminderSelectionOptions`, `reminderLocalDay`, `reminderDrafts`.
- `ReminderLedger.kt`: `data class ReminderLedgerEntry(tripId, inputHash, sendAtMs: Double, memberKeys)`; `object ReminderLedger { decode(data: String); elapsed(rows, now) }`; the extension **`fun ReminderLedgerEntry.Companion.from(request: ReminderRequest)`** (not a member `fun from(`).
- `ReminderSchedule.kt`: `REMINDER_LOOKBACK_SECONDS` (129 600), `REMINDER_MARGIN_SECONDS` (5), `REMINDER_WINDOW_SECONDS` (172 800), `REMINDER_MAX_REQUESTS` (60); `data class ReminderPlan(card: TripCardView, detail: TripDetail, marks: List<CheckMark> = emptyList())`; `data class ReminderRequest(identifier, title, body, sendAt: Instant, tripID, blockID, inputHash, memberKeys, accountID: String? = null, expiresAt: Instant)`; `fun reminderRequests(plans, accountID, settings, ledger, now, language: Language, timeFormat: TimeFormat, formatter: CopyLineFormatter, strings: ReminderStrings): List<ReminderRequest>` (identifiers carry the `chronoflow.reminder.` prefix).
- `ReminderStrings.kt`: `fun interface ReminderStrings { fun text(key: String, language: Language, time: String): String }` (`Language` = `net.chronoflow.core.model.contracts.Language`, a generated `sealed interface` with `En`/`Nl`/`Unknown`: a `when` needs `else`).
- `:core:testing`: `net.chronoflow.core.testing.reminders.FakeReminderStrings` (`core/testing/src/main/kotlin/…/testing/reminders/FakeReminderStrings.kt`), pinned to `strings_ios.xml` by 12a's `ReminderStringsResourceTest`; `:core:testing` already has `implementation(project(":core:reminders"))`.
- **12a's test helpers are not reachable from `:core:data`.** `reminderNow` (`2026-10-14T00:00:00Z`), `reminderAccount` (`01900000-0000-7000-8000-000000000001`), `reminderBlock(...)`, `reminderPlan(...)` and `reminderFormatter()` are `internal` in `core/reminders/src/test/kotlin/…/ReminderTestSupport.kt` (l.43–147). Task 3 moves them, unchanged and public, to `core/testing/src/main/kotlin/net/chronoflow/core/testing/reminders/ReminderTestSupport.kt` and updates 12a's test imports **in the same commit** (12a's tests must stay green; Konsist `testingNotInMain` is unaffected: only tests import them).

**R3. What earlier phases left for 12b (real files).**
- **Tables** (4a): `core/database/src/main/sqldelight/net/chronoflow/core/database/ScheduledReminder.sq` and `ReminderLedger.sq` hold **only the DDL, no queries**; the DDL matches the body's "before" (no `expires_at`). No test spells `scheduled_reminder`'s columns (`SchemaParityTest.onlyTheDeclaredExtrasExist` l.62–90 and `SchemaV1Tests` l.91 check table and index names only), so Task 1 changes no schema test. `SqlSafetyTest.noSqlNewerThanApi29` asserts exactly **9** `.sq` files: put every new query in the two existing files. "Save" uses `queries.accountQueries.changes()` (`Account.sq` l.16), as `RecordMapping.kt` does. Generated accessors: `sql.queries.scheduledReminderQueries`, `sql.queries.reminderLedgerQueries` (`AccountSql.queries` is public). Seeded runs reset the mock root (`app/src/debug/…/MockSessionSeed.kt` l.33–40), so emulator tiers get the new DDL; a debug build installed over an older install keeps the old table (schema version stays 1, no migration) and needs its app data cleared.
- **Record mapping** lives in `:core:database` (`core/database/src/main/kotlin/net/chronoflow/core/database/store/RecordMapping.kt`), not 4d. Its row mappers (`Trips.record()`, `Plan_versions.record()`, `Check_marks.record()`) are **private**; the public readers are `AccountSql.fetchTrips()` (**ORDER BY departure_utc, id**), `fetchSettings()`, `fetchSetting(section)`, `fetchPlanVersion(tripId, inputHash)`, `fetchCheckMarks(tripId, inputHash)`. Task 2 default: `ReminderInputs.fetch(sql)` composes these in **one** `database.read { }` (settings filtered to `reminders`/`preferences`; trips sorted by `id` in Kotlin; for each trip with `keepOffline && deletedAt == null && !deletedLocally && currentInputHash != null`, `fetchPlanVersion` + `fetchCheckMarks` filtered to `done`/`skipped`). The four Task 1 input queries are then unnecessary; if the worker adds them anyway, their mappers go in `RecordMapping.kt` beside the private ones (`:core:data` cannot reach those).
- **Records** (`:core:model` `…model.store`): `TripRecord.card: String`, `PlanVersionRecord(tripId, inputHash, detail: ByteArray, receivedAt: String, detailVersion)`, `CheckMarkRecord(tripId, inputHash, type, localDate, ordinal, status, markedAt)`, `SettingsRecord(section, value)`. `CheckMark(key: CheckKeyView, status: CheckStatus, markedAt: String)` is in `TripPlan.generated.kt` l.1037. `PlanDecoder(worker, json = ApiJson.response)` with `suspend fun detail(record: PlanVersionRecord): TripDetail` is `core/plan/…/PlanDecoder.kt` l.18/81. **`AppGraph` has no `PlanDecoder`** (each `SessionGraph` builds its own, `…/app/SessionGraph.kt` l.66): the process-wide service gets `PlanDecoder(dispatchers.default)`. `AppGraph.copyCatalogs` exists (`AppGraph.kt` l.103–106): use it.
- **Repositories** (`:core:data` `…data.repository`): `TripRepository.upsert(trip: TripRecord)` l.72, `store(plan: PlanVersionRecord, checks: List<CheckMarkRecord>)` l.108, `trips()` l.42; `SettingsRepository.upsert(SettingsRecord)` l.32, `remove(section)` l.60; `OutboxRepository.enqueue(change, permit = null, now: Instant = Instant.now(), randomBytes)` l.45 (tests pass `now = reminderNow`; it requires the account row; `OutboxChange.CheckOff(CheckOffChange)` writes the mark through `StoreApplication.setMark`; `OutboxChange.Settings` camel-cases keys, so `lead_min` lands as `leadMin`). `CheckOffChange(tripID, planInputHash, type: BlockType, localDate, ordinal, status: CheckStatus?)` (`core/model/…/CheckOffChange.kt`).
- **`AccountDatabase`** (`core/database/…/AccountDatabase.kt`): `AccountDatabase(root: File, namespace, driverFactory, io)`, `open(accountID: String)`, `read { }` l.69, unguarded `write { }` l.74, `observe(vararg tables, fetch)` l.130 (throws `StoreFailure.Closed` when unopened; completes on close), `close()`, `@Volatile accountID: AccountID?` (`rawValue` is lower-cased by `AccountID.of`).
- **Test helpers:** `JdbcAccountDriverFactory()` and **`TestRoots.temporaryStoreRoot()`** (an `object`, not a top-level function) in `:core:database-testing`; `MemorySessionStore` and `sessionResult(id)` in `:core:sync` test fixtures (`core/sync/src/testFixtures/…/session/SessionFakes.kt` l.20, l.85), already on `:core:data`'s test classpath. Tier annotations `@Logic`/`@Smoke` are `net.chronoflow.core.testing.tiers.*`; `:core:data` already runs `@Logic` Robolectric tests with `@Config(sdk = [36])` (e.g. `…/data/session/DevicePreferencesWipeTests.kt`).
- **`:core:data`** (`core/data/build.gradle.kts`) does **not** depend on `:core:reminders` yet: add `api(project(":core:reminders"))`. It has `implementation(project(":core:datastore"))` (`DevicePreferences`, `WipeMarker`), `api(:core:sync)` (`SessionController`, `SessionTeardown`, `SyncEvent`), Robolectric, Turbine and coroutines-test. The catalog has `androidx-work-runtime` (`work = "2.12.0"`, `gradle/libs.versions.toml` l.14/73) but **no `androidx-work-testing`**: add it with `version.ref = "work"`. `core/data/src/main/AndroidManifest.xml` exists (only `ACCESS_NETWORK_STATE`). The package `net.chronoflow.core.data.reminders` already holds 9g's `FirstTripRemindersOffer.kt` (12c's).
- **Navigation** (4e): `ReminderTapExtras(tripId, blockId = null, accountId = null, inputHash = null)` and `PendingLaunchIntent(links, reminder)` with its `Buffer`, and `fun interface LaunchIntentHandler { suspend fun handle(intent: PendingLaunchIntent) }` are in `…/nav/PendingLaunchIntent.kt` (l.3–33). `Route.Reminders` (`Route.kt` l.48) is 12c's. `Route.TripPage(tripId, dayId, blockId, awaitingSync, notificationInputHash, @Transient notificationToken, sheet)` (l.60–69); `stateKey()` excludes both notification fields (l.133).
- **`PendingLaunchIntents`** (`…/app/PendingLaunchIntents.kt`): `capture(intent, restored)` already runs 5c's `AuthLinkIntentPolicy.linkToCapture` and **already skips restored and `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` intents** (l.17) before reading the four `"cf.reminder.*"` literals; `consume()` returns links **and** the reminder. **`AppGraph.launchIntentHandler` is a no-op** (`AppGraph.kt` l.322); `finishLaunch()` (l.477–487, called from `CfRoot.kt` l.101 once no overlay shows) and `handlePendingLaunchIntents()` (l.489–492, from `MainActivity.onNewIntent` l.112, only after the launch finished) both pass `consume()` to it. So the cold-after-ending and warm delivery the body asks Task 9 to build already exist: see R6.
- **Session** (4e): `SessionHooks { onForeground(); onSessionEnding() }` + `NoopSessionHooks` (`core/data/…/session/SessionHooks.kt`). **`AppGraph.hooks` = `GuestSessionHooks(guestWizardStore, NoopSessionHooks)`** (`AppGraph.kt` l.227–230; `GuestSessionHooks` is `internal` in `…/app/guest/`, recovers the guest draft then calls `next.onForeground()`, and delegates `onSessionEnding` to `next`). 12b passes its own `ReminderSessionHooks` as `next` in place of `NoopSessionHooks`; it never replaces or reorders the guest hook. `activation`'s foreground lambda (l.448–459) runs `settingsClipboard.foreground()` then `hooks.onForeground()`.
- **Teardown** (`SessionController`, `core/sync/…/session/SessionController.kt`, constructor `(credentials, accounts, teardown, io)`; `SessionTeardown` data class in the same package): `stopSync = { hooks.onSessionEnding(); sessionSync.stop() }` (`AppGraph.kt` l.382–385). `performSignOut` order (l.233–266): `beginWipe` → `publish(SignedOut)` → `stopSync()` (reminders detach while the DB is open) → logout → `teardown.detach()` → `accounts.close()` (runs `SessionAccountDatabase`'s `detach` closure, today `{ sessionGraphs.drop() }`, `AppGraph.kt` l.216) → `erase` → credentials delete → `finishWipe`. The no-captured-session branch (l.200–211) also calls `stopSync`, `accounts.close` and `teardown.detach`. Restore (l.347) and accept (l.400) call `stopSync()` and `teardown.prepare` (= `sessionAccounts.close(); wiper.recover()`), never `teardown.detach`. The teardown `detach` lambda today is `{ accountModel.detach(); withContext(dispatchers.main) { onSignedOut() } }` (l.375–378). 10d's account delete (`signOutAfterDeletingAccount`, l.496–509) is `controller.signOut(expected, discardPending = true)`, the same path (iOS `deleteAccount` does the same); the 401 forced sign-out (`unauthorized`) also goes through `performSignOut`. So the cancel-on-sign-out work covers sign-out, account delete, forced wipe and account switch with no extra call sites. `SessionAccountDatabase(storageRoot: suspend () -> File, namespace, driverFactory, io, detach)` and `AccountWiper(storageRoot: suspend () -> File, namespace, credentials, guestSecrets, io)` take a root **lambda** (for Task 8's test). `WipeMarker(File(storageRoot(), namespace.directoryName), io)` as `AccountWiper` l.37–39; `StoredSession.parse(bytes)`; `readStoredSession()` deletes a bad blob (`core/model/…/session/SessionStore.kt` l.13–19).
- **`SessionGraphs`** (`…/app/SessionGraphs.kt`): constructor `(controller, sessionAccounts, accountModel, sync, source, scope, io, copyCatalogs)`; collection starts in the named, idempotent `start()` (l.43–83), which `AppGraph`'s lazy calls (`graphsLazy` l.404–417, `.also { it.start() }`). So `startSessionGraphs = { sessionGraphs.start() }` (touching `sessionGraphs` also starts it). `drop()` l.85, `close()` l.92. It takes no context: pass the reminder attach/detach and `ensureRefill` as lambdas.
- **`SessionActivation`** (`…/app/SessionActivation.kt`): positional lambdas `(scope, evictIdleConnections, setEnabled, onForeground, retryUnauthorized, retryReadiness, activeSession, resume)`, a `Mutex`, `onStart`/`onStop` → `setActive`. There is no `startedOnce` yet. `SessionSync.enabled` starts **true** (`core/sync/…/SessionSync.kt` l.45), so `prepareBackground()` is needed. `SyncEvent.Finished(accountID, epoch, result: SyncResult)`, `SyncResult(state, pendingCount, …)`, `enum class SyncState { Idle, Offline, VerificationRequired, Unauthorized, Failed }` (`core/sync/…/SyncStatus.kt`); `SessionSync.events()` l.224.
- **Readiness:** the background pass must await `AppGraph.readiness.session()` (`StoredDataReadiness`, `core/sync/…/StoredDataReadiness.kt` l.53), **never `launchReadiness`** (`FirstCommitReadiness` awaits the first frame, which a worker process never draws).
- **Process lifecycle** (A7): `ChronoFlowApp.onCreate` (`…/app/ChronoFlowApp.kt`) installs debug StrictMode, builds the graph, adds `graph.activation` to `ProcessLifecycleOwner`, starts the `launch` trace. `ChronoFlowApp` is `open` with `createGraph()` and `lateinit var graph`.
- **Manifest** (`app/src/main/AndroidManifest.xml`): `InitializationProvider` is already declared with `tools:node="merge"` and two `tools:node="remove"` meta-data (`EmojiCompatInitializer`, `PlatformInitializer`). Task 6 adds `<meta-data android:name="androidx.work.WorkManagerInitializer" tools:node="remove" />` **inside that block** (the body's "if the whole provider is removed" case does not apply). Without it `verifyReleaseManifest` fails `STARTUP_INITIALIZER` (`build-logic/…/manifest/ManifestRules.kt` l.90) and WorkManager would initialize before `Application.onCreate`. The allowlist (`ManifestRules.kt` l.40–55) already admits `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` and the dynamic-receiver permission. No channel, alarm, receiver or notification permission exists in any manifest yet.
- **Device preferences** (`core/datastore/…/DevicePreferences.kt`): `notificationPermissionRequested: Flow<Boolean>` l.52, `setNotificationPermissionRequested` l.70 (12c's), `language: Flow<LanguageChoice>` l.42. Device preferences survive the account wipe (`DevicePreferencesWipeTests`), as on iOS.
- **Language** (OQ-12b-18): 10a's `AppLanguageApplier : LanguageDependencies` exists (`…/app/settings/AppLanguageApplier.kt`; `AppGraph.languageApplier` l.166). Delegate to it: `AppLanguageSource.current()` = `ConfigurationRules.resolveLanguage(languageApplier.current.first(), Locale.forLanguageTag(languageApplier.phoneLanguage()))` mapped to `Language` (`current` already does the API 33 `LocaleManager` / DataStore split, and refreshes `phoneLanguage()` on collection). `ConfigurationRules.resolveLanguage` and `AppLanguage`/`LanguageChoice` are in `core/model/…/config/`. Screens map with `if (locales[0].language == "nl") Language.Nl else Language.En` (`…/app/trip/TripPageRoute.kt` l.63).
- **Copy and icon:** `CopyIndex.find(CopyTable.Reminders, key): CopyEntry?` with `CopyEntry(resId, isPlural, args)` (`core/ui/…/l10n/generated/CopyIndex.generated.kt` l.9–12); the channel name is `R.string.reminders_reminders_on_this_phone` (`net.chronoflow.core.ui.R`; en `Reminders on this phone`, nl `Herinneringen op deze telefoon`; `names.lock.json` l.232). `app/src/main/res/drawable/ic_stat_moon.xml` exists; its `UnusedResources` ignore is **`lint.xml` l.18–19** (not a line of the 0b brief): remove both lines in Task 5.
- **Shell** (6a): there is **no `ShellHolder`**. The holder is `internal class ShellViewModel` (`…/app/shell/ShellViewModel.kt`, `openTrip` l.76, `wizardSaved` l.84), obtained in `AppShell` as `val holder = rememberShellViewModel(root)` (`…/app/shell/AppShell.kt` l.39). Read "`ShellHolder`" as `ShellViewModel` everywhere, and `shell/ShellHolder.kt` as `shell/ShellViewModel.kt`. `ShellNav`/`ShellNavState`/`ShellTab` are in `…/nav/shell/`; the existing test is `core/navigation/src/test/…/shell/ShellNavStateTest.kt`.
- **Trip page** (6b/6d): the tags exist (`trip.page`: `feature/trip/…/page/TripPageScreen.kt` l.315 and `state/TripPageStates.kt` l.46; `step.sheet`: `core/ui/…/plan/StepSheetContent.kt` l.68; `tab.today`: `…/app/shell/AppShellScaffold.kt` l.101). The one-shot open is `TripPageViewModel` l.342–352 (`NOTIFICATION_HANDLED` in `SavedStateHandle`), and the ViewModel key is `"trip:${route.tripId}"` (`TripPageRoute.kt` l.85), so OQ-12b-12 holds as written.
- **Launch:** `LaunchState` (3b, `:feature:launch`) has **no `pendingIntent` field** (`git grep pendingIntent -- feature/launch` is empty). Per Task 9's own rule, `ReminderTapLaunchTests.coldIntentPreservesEndingAndWarmRouterCanReopen` ports only the router half and says so in the commit body; the "ending preserved" half is structural (the reducer never sees the intent) and is covered by `PendingLaunchIntentsTest.viewAndReminderIntentsAreHeldUntilLaunchFinishes` (`app/src/test/…/PendingLaunchIntentsTest.kt` l.21).
- **Konsist** `noWorkManagerOutsideReminders` (`konsist/src/test/kotlin/net/chronoflow/konsist/KonsistRules.kt` l.424–437) is **package-based** (`net.chronoflow.core.data.reminders` and subpackages), over every file of the project scope, tests included. Fixtures: `violations/noWorkManagerOutsideReminders/core/sync/…/BackgroundFlush.kt`, `allowed/noWorkManagerOutsideReminders/core/data/…/reminders/RescheduleWorker.kt`; tests `KonsistRulesTest.kt` l.42–50. Task 6's exception matches the file path suffix `app/src/main/kotlin/net/chronoflow/app/ChronoFlowApp.kt`; add both the planted `app/…/Other.kt` violation and an `allowed/…/app/src/main/kotlin/net/chronoflow/app/ChronoFlowApp.kt` control. `timeHygieneModules` (l.291) does not include `core/data`.

**R4. What 12b adds, and what stays for 12c.** 12b adds: the reminder queries and the `expires_at` column (Task 1); `…/cdr/` `ReminderStore`, `ScheduledReminderRow`, `ReminderInputs`, `ReminderRequestCodes`, `reminderPermission`, `ReminderPorts`, `ReminderService`, `AndroidReminderAlarms`, `AndroidReminderNotices`, `ReminderPermissions`, `ReminderRuntime`/`ReminderHost`, `ReminderDelivery`, `ReminderAlarmReceiver`, `ReminderUpkeepReceiver`, `ReminderRescheduleWorker`, `ReminderWork`, `ReminderIntents`; the `:core:data` manifest entries; `:app` `…/app/reminders/` wiring (`AppReminderRuntime`, `AndroidReminderStrings`, `AppLanguageSource`, `ReminderSessionHooks`, the tap pieces), the `AppGraph`/`SessionGraphs`/`SessionActivation`/`ChronoFlowApp`/`RootContent`/`MainActivity`/`ShellViewModel`/`AppShell` edits; `:core:navigation` `ReminderTapExtras.parse`, `ReminderTapRouter`, `ShellNav.reminderTap`; the moved test helpers (R2); the catalog alias, Konsist exception and fixtures, `lint.xml` line removal; plan ticks, wording fixes and OQ-A567–A586. **12c** keeps: the Connectivity & reminders page (`Route.Reminders`, 10b's row `PreferencesScreen.kt`), the `POST_NOTIFICATIONS` request and every write of `notificationPermissionRequested`, the first-trip offer (`FirstTripRemindersOffer`), the DEBUG `MockReminderCenter` and `mockAuthorizationChangesOnlyAfterAnExplicitRequest`, every new string.

**R5. Corrections to the tasks.**
- §4 item 1: grep `Companion.from(` instead of `fun from(` (the ledger factory is an extension). §4 item 6 is a Gradle command; if the sandbox cannot run Gradle, `git grep -n "androidx.work" -- '*.kts'` (empty except the catalog) stands in, and the orchestrator runs the dependency check.
- Task 2 `ReminderInputs.fetch`: see R3 "Record mapping". `CheckMark(CheckKeyView(BlockType.of(type), localDate, ordinal), CheckStatus.of(status), markedAt)`.
- Task 3 test support: build on the moved 12a helpers (R2) and `TestRoots.temporaryStoreRoot()`. Every dispatcher in a test (the store's `io`, the service's `dispatcher`, `PlanDecoder`'s worker) is `StandardTestDispatcher(testScheduler)` of the one `runTest` scheduler.
- Task 5: the receiver's `clock = { Instant.now() }` contradicts §5.1 ("production `{ Instant.now() }` lives only in `:app`"). Add `val clock: () -> Instant` to `ReminderRuntime`; `AppReminderRuntime` supplies `{ Instant.now() }`; the receiver passes `runtime.clock`; tests set it on the fake runtime.
- Task 6: manifest per R3; `ApplicationWorkConfigurationTest` stays import-free of `androidx.work` (the Konsist rule covers test files).
- Task 7: `hooks` composition per R3 (`GuestSessionHooks(guestWizardStore, ReminderSessionHooks(...))`); `AppLanguageSource` delegates to `languageApplier` (R3); `decoder = PlanDecoder(dispatchers.default)`; the "3a post-`first-commit` call site" is **`AppGraph.onFirstCommit()`** (l.471–475, called once per process through `LaunchController`'s `onFirstCommit`, `…/app/LaunchDependency.kt` l.15); font warm-up is `RootFontWarmup.kt` l.25 (`warmFontsAfterFirstFrame`), not a `warmFonts()` call to sit next to. `SessionGraphs`, `SessionActivation` and their tests change signatures: update `SessionGraphsTest`/`SessionActivationTest` and every constructor call in the same commit.
- Task 8: the teardown `detach` lambda becomes `{ withContext(dispatchers.io) { ReminderWork.cancelAll(app) }; accountModel.detach(); withContext(dispatchers.main) { onSignedOut() } }` (keep the existing main hop). The body's step numbers (7, 9, 10) are the 4e brief's; the code order is R3 "Teardown". In `ReminderSignOutTests`, `SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io, detach = { service.detach() })`, `AccountWiper({ root }, StorageNamespace.Mock, credentials, io = io)`, `MemorySessionStore()`, `controller.accept(sessionResult(reminderAccount))`. iOS's test attaches no `stopSync`; the body's `controller.attach(stopSync = { service.detach() })` mirrors the production wiring and is kept (test name unchanged).
- Task 10 journey: `0199a3c0-0000-7000-8000-000000000000` is `MockScenarioValues.demoID` ✓; `…0001` is `twoFactorID` (the two-factor account), **not** a trip: read the trip id, hash and block from the DB as the body says. Launch through `ScreenTest.launch(...)` (`app/src/androidTest/…/testing/ScreenTest.kt` l.52; it adds `CF_AUTH_TEST_ID` and profile `screen`) as `SignedInColdLaunchSmokeTest` does. Plan updates per R1.

**R6. Taps: use the existing launch-intent seam (Task 9).** The cold path (`finishLaunch` after the ending is chosen) and the warm path (`onNewIntent` → `handlePendingLaunchIntents`) already hand `consume()` to `AppGraph.launchIntentHandler`. So: `ReminderTapDispatcher(router: ReminderTapRouter) : LaunchIntentHandler` with `handle(intent) { intent.reminder?.let(router::receive) }`, and `AppGraph.launchIntentHandler` (l.322) becomes it; there is **no** `takeReminder()`, `attach()`/`deliver()` or new `MainActivity` call (a `takeReminder()` beside `consume()` would split one buffer between two readers). `ReminderTapDispatcherTest` tests `handle` (reminder → `router.pending`; links only → nothing). `ReminderTapIntentPolicy` replaces `PendingLaunchIntents.capture`'s reminder part (l.17–28) and adds the `ACTION_OPEN` check. That check breaks two existing tests that capture reminder extras without the action: `PendingLaunchIntentsTest.viewAndReminderIntentsAreHeldUntilLaunchFinishes` (an `ACTION_VIEW` intent with `cf.reminder.tripId`) and `app/src/testDebug/…/WarmLaunchIntentTest.warmFreshActivityHandsItsReminderOver` (no action): update both in the Task 9 commit. Strip the reminder action only when it equals `ACTION_OPEN` (`setIntent(Intent(intent).setData(null))` is `MainActivity.kt` l.60; a launcher intent keeps `ACTION_MAIN`).

**R7. Android specifics and tiers.**
- **Exact vs inexact:** `setExactAndAllowWhileIdle(RTC_WAKEUP)` on API 29–30 (no permission exists there), `setAndAllowWhileIdle(RTC_WAKEUP)` on API 31+; never `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` (A13, OQ-A15, banned by the manifest spec l.454). `POST_NOTIFICATIONS` is declared for API 33+ and checked at delivery; 12b never requests it.
- **Receivers:** `BOOT_COMPLETED` (delivered after the first unlock; no `LOCKED_BOOT_COMPLETED`, no `directBootAware`), `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, `LOCALE_CHANGED`, all `exported="false"`. Doze: allow-while-idle alarms only. Process death and force-stop: alarms survive process death but not force-stop or reboot; OQ-12b-5's first-pass re-arm and the boot receiver recover them. The worker never starts a pull (A7, `prepareBackground`).
- **Tiers** (AGENTS "Tests" 1): the scheduler, inputs, request codes, the permission mapping, tap parsing, routing and `ShellNav` are JVM; alarms, channel, notices, permission reads, delivery, receivers, the worker and WorkManager wiring are Robolectric `@Logic` at `sdk = [36]` (the `sdkInt` constructor parameter drives the API 30/32 branches inside that one sandbox); the emulator gets exactly one `@Smoke` journey (system notification shade + tap → step sheet), because only that needs real system UI. Reboot/time-zone checks on `pixel` are the orchestrator's manual acceptance (§7 item 5), recorded in the PR body, never claimed as a test.

**R8. Launch path and AGENTS "Performance".** Main thread does no disk or network: every AlarmManager, NotificationManager, DataStore, Keystore, WorkManager and DB call runs on `io`. `Application.onCreate` gains no call (`Configuration.Provider` is a getter WorkManager reads on first `getInstance`). Release builds ship no seed or mock code: 12b adds nothing to `src/debug`/`src/perf`; its test apps and fakes are test-only. **Genuine gap, decided here:** on a cold launch `ProcessLifecycleOwner` ON_START fires **before** `first-commit`, so the body's `onForeground = { reminderPermissions.refresh(); reminderService.reschedule() }` would load DataStore before frame 0 (A11, launch step 5). Default: `ReminderSessionHooks.onForeground()` calls only `reminderService.reschedule()` (as iOS `setActive` l.156 does); the pass reads the permission through its `permission = { reminderPermissions.refresh() }` lambda, and an unattached service returns before any read (`schedule` step 1). Replace `foregroundRefreshesPermissionThenReschedules` with `foregroundReschedulesAfterTheGuestHook` (order: guest recover, then reschedule).

**R9. Perf (AGENTS "Performance", "Commands").** 12b touches the launch path: the merged manifest and App Startup (WorkManager initializer removed), `AppGraph.onFirstCommit` (channel upkeep after `first-commit`), the signed-in restore (`SessionGraphs` now attaches the scheduler and runs a full pass with plan decoding on `default` while Today composes its first content) and `MainActivity`'s intent capture. So **`make perf`** applies (numbers into `docs/perf-startup.md` with device, build type and command, as the 9c/10b/11b sections do) and **`make baseline-profile`** applies (regenerate and commit the profiles, as 11b's `8b5e51b1` did). **`make perf-timeline` does not apply**: no timeline code changes. All three are device tiers the orchestrator runs (device lock, headless `make emu`, never a windowed emulator). §7 item 7's "No perf number is required" is replaced by this note.

**R10. Lessons from 9f–12a the worker carries.**
- **One test scheduler.** Each test has one `TestCoroutineScheduler` (from `runTest`); every injected dispatcher, including Robolectric fake runtimes, is built on it. Cancel and join service/graph scopes before the test ends.
- **Never seed "now" with `Instant.EPOCH`** or the real clock: `reminderNow`, `request.sendAt + 1 s` and explicit `enqueue(now = …)`.
- **Tests must be able to fail for the real reason**: the DST/zone alarm test sets the request from an instant string and asserts epoch ms under two default zones (a zone conversion in the adapter would fail it); `reminderWritesDoNotWakeInputObservation` fails if a reminder table joins the observed keys; `dueRowsAreKeptAndExpiredRowsDropped` uses rows on both sides of `now`. If a check cannot fail for its reason under Robolectric, delete it and rely on the emulator journey.
- **Every commit leaves tests green**: a commit that breaks an existing test updates it (R2 helper move with 12a's imports; R6 launch-intent tests; `SessionGraphsTest`/`SessionActivationTest` signatures; Konsist fixtures with the rule; `lint.xml` with the first use of `ic_stat_moon`); Spotless and Konsist pass in the commit that adds the code; debug-only modules (`:core:data-mock`, `src/debug`) are imported only from `testDebug`/`androidTest`; generated outputs go with their generator (12b changes none).
- **Docs never claim more than a test proves** (PR body, `docs/perf-*.md`, plan ticks): Robolectric proves wiring, not delivery timing; emulator numbers are not device claims.
- **Emulator tests < 10 s**; if the journey is slow, make it cheaper (shorter `sendAt`, seeded state), never raise the budget.

**R11. Task 6 dependency manifest (2026-11-22).** The first merged release manifest adds two entries the body did not account for: exported `androidx.work.impl.background.systemjob.SystemJobService` (guarded by `BIND_JOB_SERVICE`, but the existing verifier permits only `DUMP`), and direct-boot-aware `androidx.room.MultiInstanceInvalidationService`. Keep the verifier and its allowlists unchanged. Override `SystemJobService` to `exported="false"`, retaining `BIND_JOB_SERVICE`; remove Room's unused multi-instance invalidation service with `tools:node="remove"` and `tools:ignore="MissingClass"` (WorkManager does not enable multi-instance invalidation). This affects only the new dependency. AOSP's [job validation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/apex/jobscheduler/service/java/com/android/server/job/JobSchedulerService.java) checks ownership and `BIND_JOB_SERVICE`, and [component permission checks](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/ActivityManager.java) grant the system access to unexported components; keeping system job delivery is inferred from these checks. The orchestrator's boot/refill acceptance still proves actual system binding. Recorded as OQ-A587. Fold this refresh into Task 6's commit, as R10 requires for later discoveries.

**R12. Task 7 language-effect owner.** `RootContent(state, onBack, content)` has no graph or configuration policy; it remains stateless. Put the locale-keyed reminder reschedule effect in its existing owner `CfRoot`, using `LocalResources.current` for the resolved locale. Its initial unattached pass performs no I/O. This changes no earlier routing or transitions. Task 7 also introduces the complete `ReminderTapRouter` needed by its detach closure; Task 9 adds parsing and delivery to that router. `AppReminderRuntime` accepts the tap component as a supplier so its JVM logic tests need no Android object construction; delivery evaluates the supplier on IO.

**R13. Task 10 expiry wording.** Task 10's parenthetical `setTimeoutAfter(expiresAt − now, ≤ 20 min)` incorrectly bounds the delivery timeout. OQ-12b-19 bounds expiry relative to the step start; an earlier reminder also includes its lead time. Keep the implemented `expiresAt − now`, and describe the 20-minute limit relative to step start in A13 and the phase entry. No scheduling or delivery behavior changes. The orchestrator's diagnosed Task 9 activity fixture fix uses `withLinkActivity` for scrubbed reminder intents, as for links; hang caps stay unchanged.

**R14. Warm-tap dispatch and native/Compose waits (2026-11-22).** Device diagnostics found no discarded tap: `onNewIntent`, capture, the finished launch handler, dispatch and router receipt all ran at 05:38:20.996; the composition's routing continuation resumed only at 05:38:26.066, during test teardown, then the matching account opened Trips. Run `ReminderTapRouting` on injected `AppDispatchers.main`, retaining the calling effect's cancellation. A JVM regression holds the caller dispatcher paused and checks that the injected main dispatcher opens the trip without advancing the caller. The emulator journey uses UiAutomator for the notification shade and Compose structural waits for the trip page and sheet, so those waits advance the Compose test clock. Temporary diagnostic logging is removed before commit.

**R15. Review: background storage failures.** An unreadable wipe marker or encrypted session means the account is unknown and the alarm is dropped. `AppReminderRuntime.storedAccountId` maps the specific I/O, cryptographic, provider and permission failures to null. The alarm receiver also contains those failures from channel setup and delivery bookkeeping, including SQLite/storage failures, before finishing its pending result. Cancellation propagates. The failure mapping logs no credentials and does not delete them. JVM tests cover account-read failures and the receiver's error policy; Robolectric verifies channel/ledger failures still finish the broadcast.

**R16. Review: delivery versus sign-out.** A second account read alone only narrows the race. One lazy process-wide `ReminderDeliveryGate` now serializes the final stored-account read and synchronous notification post with the sign-out `beginWipe` marker write. This gate deliberately holds its mutex across the account-read/marker-write suspension; a confined dispatcher alone would permit interleaving there. The scheduler actor is unchanged, and channel setup stays outside the gate. Explicit sign-out also removes delivered notices when the scheduler is unattached; ordinary background restore keeps the no-op detach policy. A JVM race test suspends the final read after capturing the account and verifies completed sign-out leaves no post. Robolectric covers sign-out during channel setup and normal/401 sign-out with an unattached scheduler.

Tags: **[F]** fact verified in source (path:line). **[R]** decision for Android made by this brief. **[P]** platform behaviour stated from memory; verify it where the task says so.

---

## 1. Goal

Turn 12a's pure `reminderRequests(...)` into real, reliable phone reminders, as iOS `ReminderService` + `SystemReminderCenter` do, in Android terms:

1. **Scheduler** (`ReminderService`, a port of iOS `ReminderService.swift`): observes the account DB, runs serialized replace-all passes (generation counter, one flight, one rerun), arms `AlarmManager` alarms (exact `setExactAndAllowWhileIdle(RTC_WAKEUP, …)` on API 29–30, inexact `setAndAllowWhileIdle(RTC_WAKEUP, …)` on API 31+; A13), mirrors them in `scheduled_reminder`, and keeps the ledger.
2. **Delivery** (`ReminderAlarmReceiver`): posts on channel `reminders` with `notify(tag = identifier, id = requestCode)`, `setTimeoutAfter(expiresAt − now)` and `ic_stat_moon`, after an account check.
3. **Upkeep**: boot, package-replaced, time, time-zone and locale broadcasts enqueue the expedited unique work `reminders-reschedule` (REPLACE); a 12 h periodic `reminders-refill` keeps the 48 h window full. WorkManager initializes on demand.
4. **Taps**: an explicit `PendingIntent` to `MainActivity` carries trip, block, plan hash and account; cold and warm taps route to `TripPage` after an account check (iOS `AppTabs.swift:371–385`).
5. **Teardown**: sign-out (and account switch) cancels every alarm, removes every posted reminder, deletes the rows and erases the ledger, through 4e's `SessionHooks.onSessionEnding` and the account-DB detach closure.
6. **Permission model**: `ReminderPermissions.state: StateFlow<ReminderPermission?>` for 12c, computed from `POST_NOTIFICATIONS`, `areNotificationsEnabled()`, the channel importance and `notificationPermissionRequested`.

No screen, no string, no permission request (12c).

---

## 2. Inputs to read

### 2.1 This repo (in this order)

| What | Path | Why |
|---|---|---|
| Agent rules | `AGENTS.md` (all; "Tests", "Performance", "Traps": `CancellationException`, StrictMode, no I/O on main) | How to work |
| Plan | `plan.md` l.100 (A13), l.92 (A5), l.94 (A7), l.321–346 (launch sequence, step 5 "notification channel upkeep" at l.341), l.402 (`Route.TripPage`), l.450–478 (manifest l.450–464, seeds l.466–478), l.3470–3510 (this phase) | Scope |
| Research | `docs/research/07-day-to-day-settings.md` §7.1–§7.3 (l.452–515); `docs/research/14-gaps.md` §6–§7 (l.297–370) | Rules, Android shape |
| 12a brief | `docs/phases/phase-12a-reminders-engine.md` §5.1 (constants), Task 2 (`ReminderSettings.decode`, `reminderTimeFormat`, `ReminderPermission`), Task 5 (`ReminderRequest` with `expiresAt`, `ReminderStrings`, `ReminderLedgerEntry.from`), §9 OQ-12a-3/-5/-6/-7, §10 | The API you call |
| 4a brief | `docs/phases/phase-4a-store.md` Task 3 (l.225–262, reminder DDL; "12b may edit this v1 DDL in place"), Task 5 (l.386–410, `write`, `observe`), l.510–527 (`DevicePreferences.notificationPermissionRequested`), l.156–163 (`StoredSession.parse`), l.528–532 (`WipeMarker`) | Tables, DB API |
| 4d brief | `docs/phases/phase-4d-sync.md` §5.3 (l.188–197, actor mapping), l.256 (`SyncEvent.Finished`, `SyncState.Idle`), l.274 (`RecordMapping`), l.308 (repository flows), Task 10 (l.446–449, `noWorkManagerOutsideReminders`), OQ-4d-17 (l.549) | Concurrency style, sync events, Konsist rule |
| 4e brief | `docs/phases/phase-4e-session.md` §5.3 (l.264–310, teardown order; step 7 = `SessionHooks.onSessionEnding`), §5.5 (l.383–391, `SessionActivation`), §5.6 (l.393–430, `AppGraph`, `SessionGraph(s)`), §5.8 (l.455–481, `PendingLaunchIntent`, `ReminderTapExtras`, `LaunchIntentHandler`, `finishLaunch`), T9 (l.618–635), OQ-4e-10, OQ-4e-14 | Hooks you fill |
| 5c brief | `docs/phases/phase-5c-password-reset-links.md` §5.6 (l.303–340, `capture(intent, restored)`, `AuthLinkIntentPolicy`, `AuthLinkDispatcher.attach/deliver`) | The pattern Task 9 copies |
| 6a brief | `docs/phases/phase-6a-shell-trips.md` §5.6 (l.184–210, `ShellNavState`, `ShellNav`), §5.7, l.382, l.509 (the shell holder, landed as `ShellViewModel`, and `AppShell`) | Tap routing target |
| 6d brief | `docs/phases/phase-6d-trip-interactions.md` l.91, l.136, l.157 (notification step-open once) | What the trip page does with your route |
| 0b brief | `docs/phases/phase-0b-tokens.md` l.84, l.112, l.375–376 (`ic_stat_moon`, its `UnusedResources` ignore to remove) | Icon |
| 0c brief | `docs/phases/phase-0c-strings.md` T4 (l.442–480, `CopyIndex.find(CopyTable.Reminders, key)`) | Copy lookup |
| 0e brief | `docs/phases/phase-0e-test-harness.md` l.137–175 (tiers), l.249 (`resolveLanguage`) | Tiers, language |
| 3a brief | `docs/phases/phase-3a-launch-overlay.md` l.330 (font warm-up after `first-commit`) | Where channel upkeep goes (*[Refresh R5]*: `AppGraph.onFirstCommit()`, l.471–475) |

### 2.2 iOS (read-only; `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

| File | Lines | What to mirror |
|---|---|---|
| `plan.md` | l.2212–2231 ("Phase 19: Local reminders", 19c l.2218) | "Integrate serialized scheduling after full sync, checks/undo, settings, foreground and significant time/zone changes, after overlay commit. Extend account teardown to cancel requests and erase the account ledger. Route captured taps after launch without changing splash ending; warm taps open the step." |
| `Docs/phase-19-reminders.md` | ¶3–¶5 | Ledger, teardown, triggers, tap rules |
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderService.swift` | 1–158 | `ReminderInputs` (7–40), attach (57–74), flight/rerun (83–100), `schedule` (101–142), `detach` (143–156) |
| `Packages/ChronoFlowKit/Sources/Reminders/SystemReminderCenter.swift` | 1–76 | Permission mapping (8–18), payload (45–57), UTC trigger (58–64), `removeDelivered` (68–74) |
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderSchedule.swift` | 17–36 | `ReminderRequest`, `ReminderNotificationCenter` protocol |
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderTap.swift` | 1–31 | Tap parsing, `ReminderTapRouter` |
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderLedger.swift` | 31–45 | `elapsed`, `read/write/clear` |
| `App/Session/ReminderTimeObservers.swift` | 1–15 | `significantTimeChangeNotification` + `NSSystemTimeZoneDidChange` → `reschedule()` |
| `App/Session/NativeSessionController.swift` | 44–54, 98–111, 152–160, 186–217 | Detach closure (taps, reminders, model), `stopSync` (reminders first), foreground order, attach on `signedIn`, sync `Finished(idle)` → reschedule |
| `App/AppTabs.swift` | 340, 371–385 | Language change → `reminders.setLanguage(language)` (stores the language in the service, then reschedules; `ReminderService.swift:49, 79–82`); tap routing. Plan.md cites l.370–383; the `.onChange` block runs 371–385 |
| Tests `Packages/ChronoFlowKit/Tests/RemindersTests/` | `ServiceTests.swift` 1–204, `SignOutTests.swift` 1–58, `TapTests.swift` 1–54, `PermissionTests.swift` 25–43, `ReminderTestSupport.swift` 55–131 | Ported tests (Appendix A) |

### 2.3 Web

None. Reminders are local only (`07` §7.1). No endpoint is called by this phase. (The reminders settings patch `PATCH /v1/account/reminders` is 12c's, through the outbox.)

### 2.4 Boards

**None.** There is no board for the notification, the channel or any reminder UI in this phase. The notification is system UI: title, body and small icon only, as on iOS. Do not open `SetReminders*` or `WizardReminders*` (12c). `LiveActivity.dc.html` draws a lock-screen Live Activity, which OQ-A33 excludes. [F] The one board line that touches this phase is `SplashSpec.dc.html` "Lands on. Today, or the screen a notification or link opened": a cold tap lands on the tapped trip after the splash, which Task 9's `finishLaunch` → `attach()` order gives.

---

## 3. Out of scope (owner)

- The Connectivity & reminders page, the master switch, lead time, Open phone settings, the `POST_NOTIFICATIONS` request flow and writing `notificationPermissionRequested` [12c]. iOS `PermissionTests.mockAuthorizationChangesOnlyAfterAnExplicitRequest` [12c].
- The first-trip offer sheet [12c].
- Live Updates or an ongoing notification (OQ-A33: no). Exact alarms on API 31+, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` (OQ-A15: exact only on API 29–30, where no permission exists). A battery-optimisation prompt (no board). A notification accent colour (no board, OQ-12b-11).
- Any new string. The channel name reuses the iOS key `Reminders on this phone` (already in 0c's `reminders` table).
- Background sync or flush (A7). The worker never starts a pull (§5.7).
- Changing 12a's engine, 6d's trip page, or 4d's sync files.

---

## 4. Pre-flight checks (stop and report if one fails)

Run from the repo root:

1. 12a: `grep -rn "fun reminderRequests\|class ReminderRequest\|val expiresAt\|fun interface ReminderStrings\|enum class ReminderPermission\|fun reminderTimeFormat\|REMINDER_MAX_REQUESTS\|Companion.from(" core/reminders/src/main` finds each (*[Refresh R5]*: the ledger factory is the extension `ReminderLedgerEntry.Companion.from`).
2. 4a: `core/database/src/main/sqldelight/net/chronoflow/core/database/ScheduledReminder.sq` and `ReminderLedger.sq` exist with the DDL of 4a Task 3; `grep -n "notificationPermissionRequested" core/datastore/src/main -r` finds the flow and its setter.
3. 4e: `grep -rn "interface SessionHooks\|fun onSessionEnding\|fun onForeground\|class SessionGraphs\|class SessionActivation\|class PendingLaunchIntents\|interface LaunchIntentHandler\|data class ReminderTapExtras" core app` finds each.
4. 4d: `grep -rn "data class Finished\|enum class SyncState\|fun events()" core/sync/src/main` finds each; `:konsist` has `noWorkManagerOutsideReminders`.
5. 0b: `app/src/main/res/drawable/ic_stat_moon.xml` exists. 0c: `grep -n '"reminders\\tReminders on this phone"' tools/l10n/names.lock.json` finds the resource name you will use for the channel.
6. `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep androidx.work` shows nothing yet (this phase adds it).

---

## 5. Shared rules and shapes

### 5.1 Modules and packages [R]

| Code | Module / package | Path |
|---|---|---|
| SQL queries, DDL edit | `:core:database` | `core/database/src/main/sqldelight/net/chronoflow/core/database/{ScheduledReminder,ReminderLedger}.sq` |
| Store, inputs, request codes, permission mapping, service, Android adapters, receivers, worker, `ReminderWork`, `ReminderRuntime`/`ReminderHost`, `ReminderIntents` | `:core:data` `net.chronoflow.core.data.reminders` | `core/data/src/main/kotlin/net/chronoflow/core/data/reminders/` |
| Receivers and permissions in the manifest | `:core:data` | `core/data/src/main/AndroidManifest.xml` |
| `ReminderTapExtras.parse`, `ReminderTapRouter` | `:core:navigation` `net.chronoflow.core.navigation` | next to 4e's `PendingLaunchIntent.kt` |
| `ShellNav.reminderTap` | `:core:navigation` `net.chronoflow.core.navigation.shell` | 6a's `ShellNav.kt` |
| Wiring: `AppReminderRuntime`, `AndroidReminderStrings`, `AppLanguageSource`, `ReminderTapIntentPolicy`, `ReminderTapDispatcher`, `ReminderTapRouting`, hooks | `:app` `net.chronoflow.app.reminders` | `app/src/main/kotlin/net/chronoflow/app/reminders/` |

- Dependencies: `:core:data` gains `implementation(libs.androidx.work.runtime)` (`androidx.work:work-runtime` **2.12.0**, A1; use the `-ktx`-free artifact, coroutines are in it since 2.9) and `api(project(":core:reminders"))` (*[Refresh R3]*: not present at `147aa187`; add it). Test: `testImplementation(libs.androidx.work.testing)` (`androidx.work:work-testing:2.12.0`; add the alias to the catalog with the same version ref). `:app` adds its own `implementation(libs.androidx.work.runtime)`, because `ChronoFlowApp.kt` imports `androidx.work.Configuration` and `:core:data`'s `implementation` dependency is not on `:app`'s compile classpath.
- Konsist: `noWorkManagerOutsideReminders` gains **one** named exception, `app/src/main/kotlin/net/chronoflow/app/ChronoFlowApp.kt` (OQ-4d-17 → OQ-12b-17). Add a planted violation proving another `:app` file still fails.
- Concurrency: 4d §5.3 exactly. `ReminderService` is a Swift `actor`: its state is touched only on a private `limitedParallelism(1)` view of the injected dispatcher (`dispatchers.default` in production); every public method is `withContext(confined)`; the flight is a `Job` in the service's own `CoroutineScope(SupervisorJob() + confined)`, never a child of the caller; never hold a `Mutex` across a suspension; `catch (e: CancellationException) { throw e }` before any mapping; never `runCatching`.
- No main-thread disk, Keystore, binder-heavy or DB work: every adapter call runs inside `withContext(io)`. Receivers do their work inside `goAsync()` on `io`.
- Inject the clock (`() -> Instant`) everywhere; production `{ Instant.now() }` lives only in `:app`.

### 5.2 Constants [R] (`ReminderIntents.kt`, public `const val`)

| Name | Value |
|---|---|
| `CHANNEL_ID` | `"reminders"` (A13) |
| `ACTION_ALARM` | `"net.chronoflow.reminder.ALARM"` |
| `ACTION_OPEN` | `"net.chronoflow.reminder.OPEN"` |
| `EXTRA_TRIP_ID`, `EXTRA_BLOCK_ID`, `EXTRA_ACCOUNT_ID`, `EXTRA_INPUT_HASH` | `"cf.reminder.tripId"`, `"cf.reminder.blockId"`, `"cf.reminder.accountId"`, `"cf.reminder.inputHash"` (4e OQ-4e-10; replace 4e's string literals in `PendingLaunchIntents` with these constants) |
| `EXTRA_IDENTIFIER`, `EXTRA_REQUEST_CODE`, `EXTRA_TITLE`, `EXTRA_BODY`, `EXTRA_EXPIRES_AT` | `"cf.reminder.identifier"`, `"cf.reminder.requestCode"` (Int), `"cf.reminder.title"`, `"cf.reminder.body"`, `"cf.reminder.expiresAt"` (Long epoch ms) |
| `WORK_RESCHEDULE` | `"reminders-reschedule"` (A13) |
| `WORK_REFILL` | `"reminders-refill"` |
| `REFILL_HOURS` | `12L` (OQ-A14) |
| `TAG_PREFIX` | `"chronoflow.reminder."` (= 12a's identifier prefix) |

Engine numbers (48 h, 60, 5 s, 36 h, 15 min, 6/day, 20 min) come only from 12a's `REMINDER_*` constants and `ReminderSelectionOptions`. Never repeat them.

### 5.3 Facts that shape the port [F]

- iOS reschedules from a DB observation of `settings` (sections `reminders`, `preferences`), `trips`, `plan_versions`, `check_marks`, skipping equal inputs (`ReminderService.swift:12–20, 62–73`). Check-off, undo and settings changes reach it because the outbox enqueue writes the local rows (`ServiceTests.swift:133–151`).
- iOS also reschedules on: sync `Finished` with `result.state == .idle` (`NativeSessionController.swift:189–193`), foreground (`:156`), significant time change and system time-zone change (`ReminderTimeObservers.swift:8`), language change (`AppTabs.swift:340`, `setLanguage`). iOS keeps the language as service state (default `.en`, `ReminderService.swift:49`); Android reads it through the `language` lambda instead [R], because a worker pass has no UI to set it.
- `schedule` (l.101–142): read pending; no database or account → remove all pending; `ledger = elapsed(read)`; read inputs, settings (`try?` decode), permission; generation check; compute `upcoming` only when `permission.remindersActive(settings)`; generation check; remove pending not desired; for each upcoming request: stop on generation change **or a requested rerun**; `add` only when not already pending with equal content (a failed add → `continue`, no ledger entry); append `ReminderLedgerEntry` for every kept request; generation check; write the ledger; `failure = nil`.
- iOS appends to the ledger at **schedule** time. `elapsed` keeps only entries with `now − 36 h ≤ sendAtMs ≤ now`, so a future entry is dropped at the next read and a passed one means "delivered" (`ReminderLedger.swift:31–34`). iOS has no delivery hook.
- `detach` (l.143–156): `generation += 1`, `rerun = false`, cancel and drop the observation, `database = nil`, await the flight and the observation, remove all pending, remove delivered, clear the ledger (when a database was attached).
- Permission: `.notDetermined → undetermined`, `.denied → blocked`, `.authorized/.provisional/.ephemeral → granted`, unknown → `unsupported` (`SystemReminderCenter.swift:8–18`). Scheduling never requests permission (`ServiceTests.swift:18–30`).
- Triggers are absolute UTC instants (`SystemReminderCenter.swift:58–64`, test `absoluteTriggersSurviveDSTAndDeviceZoneChanges`).
- Tap routing (`AppTabs.swift:371–385`): clear the router; then only if there is an active session and (`tap.accountID == nil` or equal to the session account): `selection = .trips`, `tripsPath = [TripPageRoute(tripID, blockID, notificationInputHash: inputHash, notificationToken: UUID())]`. The wizard cover is not touched.
- Teardown order (`NativeSessionController.swift:50–54, 100–103`): the account-DB detach closure runs `taps.clear()`, `reminders.detach()`, `model.detach()`; `stopSync` runs `reminders.detach()` before `sync.stop()`.

---

## 6. Tasks

Each task is one commit on `feat/phase-12b-reminders-scheduler`: Conventional Commits, lower-case imperative subject, **no trailer**. Tick the named plan.md box in the same commit. Run `make targeted ONLY=…` for what you touched; run `make quick` once before handing back.

### Task 1. Reminder tables: `expires_at` and queries

Commit: `feat(database): add reminder mirror and ledger queries`. Ticks nothing.

**Files:** `ScheduledReminder.sq`, `ReminderLedger.sq`; `core/data/…/reminders/ReminderStore.kt`, `ScheduledReminderRow.kt`; test `core/data/src/test/…/reminders/ReminderStoreTests.kt`.

**DDL [R] (OQ-12b-9; 4a allows in-place edits before the first release).** Replace the `scheduled_reminder` DDL with (column order exact; `reminder_ledger` and its index unchanged):
```sql
CREATE TABLE scheduled_reminder (
  identifier TEXT PRIMARY KEY NOT NULL,
  request_code INTEGER NOT NULL UNIQUE,
  send_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  trip_id TEXT NOT NULL,
  block_id TEXT NOT NULL,
  input_hash TEXT NOT NULL,
  member_keys TEXT NOT NULL,
  account_id TEXT
);
```
Times are epoch milliseconds. Update 4a's schema tests only where they spell this table's columns (*[Refresh R3]*: none do; keep every query in the two existing `.sq` files, `SqlSafetyTest` counts 9).

**Queries** (labels exact; 4a rule 6: no UPSERT, no `RETURNING`, no `INSERT OR REPLACE`; "save" = `UPDATE`, then `INSERT` when `changes()` is 0, in one transaction):
- `ScheduledReminder.sq`: `selectScheduledReminders` (`ORDER BY send_at, identifier`), `updateScheduledReminder` (every non-key column `WHERE identifier = ?`), `insertScheduledReminder`, `deleteScheduledReminder` (`WHERE identifier = ?`), `deleteScheduledReminders`, and the input queries of Task 2:
  ```sql
  selectReminderSettings: SELECT section, value FROM settings WHERE section IN ('preferences', 'reminders') ORDER BY section;
  selectReminderTrips: SELECT * FROM trips ORDER BY id;
  selectReminderPlanVersions: SELECT pv.* FROM plan_versions pv JOIN trips t ON t.id = pv.trip_id AND t.current_input_hash = pv.input_hash
    WHERE t.keep_offline = 1 AND t.deleted_at IS NULL AND t.deleted_locally = 0 ORDER BY pv.trip_id, pv.input_hash;
  selectReminderCheckMarks: SELECT c.* FROM check_marks c JOIN trips t ON t.id = c.trip_id AND t.current_input_hash = c.input_hash
    WHERE t.keep_offline = 1 AND t.deleted_at IS NULL AND t.deleted_locally = 0 AND c.status IN ('done', 'skipped')
    ORDER BY c.trip_id, c.input_hash, c.type, c.local_date, c.ordinal;
  ```
- `ReminderLedger.sq`: `selectReminderLedger` (`SELECT trip_id, input_hash, send_at_ms, member_keys FROM reminder_ledger ORDER BY id`), `insertReminderLedger(trip_id, input_hash, send_at_ms, member_keys)`, `deleteReminderLedger`.

**`ScheduledReminderRow`**: `data class ScheduledReminderRow(val identifier: String, val requestCode: Int, val sendAt: Instant, val expiresAt: Instant, val title: String, val body: String, val tripId: String, val blockId: String, val inputHash: String, val memberKeys: List<String>, val accountId: String?)`, with `fun toRequest(): ReminderRequest` and `companion fun of(request: ReminderRequest, requestCode: Int)`. `fun sameContent(request: ReminderRequest): Boolean` compares **every** `ReminderRequest` field (identifier, title, body, sendAt, tripID, blockID, inputHash, memberKeys, accountID, expiresAt; iOS `pending.contains(request)` uses synthesized `Equatable`). Instants are stored with `toEpochMilli()` and read with `Instant.ofEpochMilli`; 12a's `sendAt` is whole-minute plan data, so no precision is lost (assert it in a test).

**`ReminderStore`** (`object`, stateless; every function takes the `AccountDatabase`; reads through `database.read`, writes through the unguarded `database.write`; reminder tables are local-only, so no epoch or permit):
- `suspend fun rows(db): List<ScheduledReminderRow>`; `suspend fun save(db, row)` (update, then insert); `suspend fun delete(db, identifier)`; `suspend fun deleteAll(db)`.
- `suspend fun ledger(db): List<ReminderLedgerEntry>`: `sendAtMs = send_at_ms.toDouble()`; `member_keys` decoded as a JSON string array with `ApiJson.response`; a row that fails to decode (`SerializationException`, `IllegalArgumentException`) is skipped (12a's tolerant ledger decode).
- `suspend fun replaceLedger(db, entries)`: one write: `deleteReminderLedger`, then one insert per entry with `send_at_ms = floor(entry.sendAtMs).toLong()` (OQ-12a-5) and `member_keys` encoded as a JSON array (`ApiJson.request` or `response`, compact).
- `suspend fun clearLedger(db)`.
- `member_keys` uses the same encoding in both tables. Never log a row.

If SQLDelight generates a query-specific result type for the two `pv.*`/`c.*` join queries instead of the table type, call their mapper overload (`selectReminderPlanVersions { trip_id, input_hash, detail, received_at, detail_version -> … }`) and build the record exactly as 4d's `RecordMapping` does for the table row.

**Tests** `ReminderStoreTests` (plain JVM, `JdbcAccountDriverFactory` + `temporaryStoreRoot()` from `:core:database-testing`, which 4d Task 0 moved there and added to `:core:data` as `testImplementation`; `runTest(timeout = 60.seconds)`):
- `rowsRoundTripAndSaveUpdatesInPlace`: save a row, save the same identifier with a new body and the same request code → one row, new body; `rows()` order is `send_at, identifier`; a second identifier with the same `request_code` throws (UNIQUE).
- `ledgerRoundTripFloorsMilliseconds`: `sendAtMs = 1.7928e12 + 0.9` stores `floor`; read back as `Double`; `replaceLedger` replaces; `clearLedger` empties; a hand-inserted row with `member_keys = '[1]'` is skipped.
- `reminderWritesDoNotWakeInputObservation`: collect `db.observe("settings","trips","plan_versions","check_marks") { 0 }` with Turbine; after the initial item, `save` and `replaceLedger` produce no new item (the tables are not observed keys).

### Task 2. Inputs, request codes and the permission mapping

Commit: `feat(reminders): read reminder inputs and allocate stable request codes`. Ticks nothing.

**Files:** `ReminderInputs.kt`, `ReminderRequestCodes.kt`, `ReminderPermissionMapping.kt`; tests `ReminderInputsTests.kt`, `ReminderRequestCodesTest.kt`, `ReminderPermissionMappingTest.kt` (all JVM, `core/data/src/test/…/reminders/`).

**`ReminderInputs`** (port of `ReminderService.swift:7–40`):
- `data class ReminderInputs(val reminders: String?, val preferences: String?, val trips: List<TripRecord>, val versions: List<PlanVersionRecord>, val checks: List<CheckMarkRecord>)`. Equality is content equality (4d/1a records; `PlanVersionRecord` compares bytes).
- `companion fun fetch(sql: AccountSql): ReminderInputs` runs the four Task 1 input queries **in one read** and maps rows with 4d's `RecordMapping` helpers (*[Refresh R3]*: those mappers are private to `:core:database`; default is to compose the public `fetchTrips`/`fetchSettings`/`fetchPlanVersion`/`fetchCheckMarks` in one read, trips sorted by `id`). `[R]` iOS fetches every plan version and mark; Android fetches only those of eligible trips' current hash (the result of `plans` is identical; less I/O on large plans).
- `suspend fun plans(decoder: PlanDecoder): List<ReminderPlan>`, for each trip in `id` order, skip unless all hold: `keepOffline`, `deletedAt == null`, `!deletedLocally`, the card decodes as `TripCardView` with `ApiJson.response` (`SerializationException`/`IllegalArgumentException` → skip), `card.progress.status != TripStatus.Completed`, a version with `inputHash == currentInputHash` exists, and `decoder.detail(version)` succeeds (same two exceptions → skip). Marks: rows of that `(tripId, inputHash)` whose `CheckStatus.of(status)` is `Done` or `Skipped` → `CheckMark(CheckKeyView(BlockType.of(type), localDate, ordinal), status, markedAt)`. Use the 0d/1a spellings.

**`ReminderRequestCodes`** (`object`): `fun allocate(identifier: String, taken: Set<Int>): Int`: start at `identifier.hashCode() and 0x7FFF_FFFF` (Java `String.hashCode` is specified, so stable across processes); while the code is in `taken`, `code = (code + 1) and 0x7FFF_FFFF`. A code is reused for an identifier as long as its row exists (the service looks the row up first). Plan wording "a request code that stays stable per identifier" is met by that rule.

**`reminderPermission(...)`** [R] (pure; no `android.*` import; `07` §7.3):
```kotlin
fun reminderPermission(sdkInt: Int, managerAvailable: Boolean, runtimeGranted: Boolean, requestedBefore: Boolean,
                       notificationsEnabled: Boolean, channelBlocked: Boolean): ReminderPermission = when {
    !managerAvailable -> ReminderPermission.Unsupported
    !requestedBefore && (sdkInt < 33 || !runtimeGranted) -> ReminderPermission.Undetermined
    sdkInt >= 33 && !runtimeGranted -> ReminderPermission.Blocked
    !notificationsEnabled || channelBlocked -> ReminderPermission.Blocked
    else -> ReminderPermission.Granted
}
```
Below API 33 there is no runtime permission, so `requestedBefore` (`notificationPermissionRequested`) is the in-app opt-in: until the person taps **Turn on reminders** (12c), the state is `Undetermined` and nothing is scheduled, as on iOS (`ReminderSelection.swift:28`: reminders are active only when authorized; plan A13).

Plan names `ReminderPermissionState (granted, ask, blocked, unsupported)`; it is 12a's `ReminderPermission` with `Undetermined` = "ask". Do not add a second enum (OQ-12a-7, OQ-12b-10).

**Tests**
- `ReminderInputsTests` (JVM, Jdbc store; port of the filter half of `ServiceTests.currentOfflinePlansOnlyAndAccountSwitchClears`): one eligible trip → one plan with its two done/skipped marks (a `pending`-status row and a mark of another hash are ignored); each of `keepOffline = false`, `deletedAt` set, `deletedLocally = true`, a changed `currentInputHash`, a `Completed` card, an undecodable card, an undecodable detail → no plan; `fetch` returns `reminders`/`preferences` texts and nulls when absent.
- `ReminderRequestCodesTest`: deterministic for one identifier; non-negative for an identifier whose `hashCode()` is `Int.MIN_VALUE` (find one with a loop in the test or use a known string); probing skips taken codes; probing wraps from `Int.MAX_VALUE` to `0`.
- `ReminderPermissionMappingTest.nativeAuthorizationMatrix` (Swift name; the Android truth table). Assert literal rows only; do not re-implement the `when` in the test. Columns `(sdkInt, managerAvailable, runtimeGranted, requestedBefore, notificationsEnabled, channelBlocked) → expected`:
  1. `(36, false, true, false, true, false) → Unsupported`
  2. `(36, true, false, false, true, false) → Undetermined`
  3. `(36, true, false, true, true, false) → Blocked`
  4. `(36, true, true, true, true, false) → Granted`
  5. `(36, true, true, false, false, false) → Blocked`
  6. `(36, true, true, false, true, true) → Blocked`
  7. `(33, true, false, false, false, false) → Undetermined` (an unrequested runtime permission wins over the disabled switch)
  8. `(32, true, false, true, true, false) → Granted` (below 33 `runtimeGranted` is ignored; `requestedBefore` is the in-app opt-in)
  9. `(29, true, false, true, false, false) → Blocked`
  10. `(29, true, true, true, true, true) → Blocked`
  11. `(32, true, false, false, true, false) → Undetermined` (not opted in yet)
  12. `(29, true, false, false, false, false) → Undetermined` (an unrequested opt-in wins over the disabled switch, as in row 7)

### Task 3. The scheduler (`ReminderService`)

Commit: `feat(reminders): port the serialized reminder scheduler`. Ticks nothing (the "**Scheduler**" box is ticked in Task 7, once wired).

**Files:** `ReminderService.kt`, `ReminderPorts.kt` (interfaces); tests `core/data/src/test/…/reminders/{ReminderServiceTests,ReminderTestStore,FakeReminderAlarms,FakeReminderNotices}.kt`.

**Ports** (`ReminderPorts.kt`):
```kotlin
class ReminderAlarmRefused(cause: Throwable?) : Exception(null, cause)
interface ReminderAlarms {
    suspend fun set(request: ReminderRequest, requestCode: Int)      // throws ReminderAlarmRefused
    suspend fun cancel(identifier: String, requestCode: Int)
}
interface ReminderNotices {
    suspend fun ensureChannel(language: Language)
    suspend fun removeDelivered()
}
```

**Constructor**
```kotlin
class ReminderService(
    private val alarms: ReminderAlarms,
    private val notices: ReminderNotices,
    private val permission: suspend () -> ReminderPermission,
    private val language: suspend () -> Language,
    private val strings: ReminderStrings,
    private val formatter: suspend () -> CopyLineFormatter,
    private val decoder: PlanDecoder,
    private val clock: () -> Instant,
    dispatcher: CoroutineDispatcher,
)
```
Public API: `suspend fun attach(database: AccountDatabase)`, `suspend fun reschedule()`, `suspend fun detach()`, `suspend fun delivered(accountId: String, identifier: String)`, `val attachedAccount: StateFlow<String?>`, `val failure: Throwable?` (read on the confined context, test only).

**Behaviour** (port `ReminderService.swift:42–158` line by line; Android differences marked [R]):
- `attach(database)`: return if `this.database === database`; else `detach()`; set `database`; capture `gen = generation`; launch `observation` in the service scope: `database.observe("settings", "trips", "plan_versions", "check_marks") { ReminderInputs.fetch(it) }.distinctUntilChanged().collect { inputsChanged(gen) }` (`observe` throwing `StoreFailure.Closed` → end quietly; rethrow cancellation). Set `attachedAccount.value = database.accountID?.rawValue`. `inputsChanged(gen)`: `if (generation == gen) reschedule()`.
- `reschedule()`: `rerun = true`; if a `flight` exists, `join` it and return; else start `flight = scope.launch { run() }` and `join` it. `run()`: `do { rerun = false; val g = generation; try { schedule(g) } catch (e: CancellationException) { throw e } catch (e: Exception) { failure = e } } while (rerun)`; `flight = null`. (Swift catches every error into `failure`; this is the one place a broad catch is allowed, after the cancellation rethrow.)
- `schedule(g)` [port of l.101–142, with Android rows standing in for `center.pending()`]:
  1. `val db = database ?: return`; `val id = db.accountID ?: return` [R: iOS removes all pending here; Android rows live in the DB that is gone, OQ-12b-3].
  2. `now = clock()`; `rows = ReminderStore.rows(db)`; `ledger = ReminderLedger.elapsed(ReminderStore.ledger(db), now)`; `inputs = db.read { ReminderInputs.fetch(it) }`; `settings = ReminderSettings.decode(inputs.reminders)`; `permission = permission()`; `if (generation != g) return`.
  3. If `settings != null && permission.remindersActive(settings)`: `language = language()`, `timeFormat = reminderTimeFormat(inputs.preferences)`, `upcoming = reminderRequests(inputs.plans(decoder), id.rawValue, settings, ledger, now, language, timeFormat, formatter(), strings)`. Otherwise `upcoming = emptyList()`. `if (generation != g) return`.
  4. `if (upcoming.isNotEmpty()) notices.ensureChannel(language)` [R].
  5. **Remove** [R] (OQ-12b-4): let `active = settings != null && permission.remindersActive(settings)` (the step 3 condition). For each row whose identifier is not desired: if `row.expiresAt <= now` → `delete` only (the alarm already fired or is dropped by the receiver); else if `row.sendAt > now` **or** `!active` → `alarms.cancel(row.identifier, row.requestCode)` and `ReminderStore.delete`; else (due, `sendAt <= now < expiresAt`, reminders still on) keep it: an inexact alarm may still be on its way. Turning reminders off or losing permission therefore cancels due alarms too, so no reminder posts after the switch is off.
  6. **Add**: `taken = rows' request codes`. For each request in `upcoming` order: `if (generation != g || rerun) break`; `existing = rows[identifier]`; if `existing == null || !existing.sameContent(request) || !armedInThisProcess` [R, OQ-12b-5]: `code = existing?.requestCode ?: ReminderRequestCodes.allocate(identifier, taken)` (add it to `taken`); `try { alarms.set(request, code) } catch (e: ReminderAlarmRefused) { continue }`; `ReminderStore.save(db, ScheduledReminderRow.of(request, code))`. Then `ledger += ReminderLedgerEntry.from(request)`.
  7. `if (generation != g) return`; `ReminderStore.replaceLedger(db, ledger)`; if step 6 did not break, `armedInThisProcess = true`; `failure = null`.
- `detach()` [port of l.143–156]: `generation += 1`; `rerun = false`; cancel and null `observation`; `db = database`; `database = null`; `attachedAccount.value = null`; `flight?.join()`; join the old observation. **If `db == null` return** [R, OQ-12b-3: iOS also removes all pending and delivered here; Android does nothing for a service that was never attached, so a background restore never clears the user's posted reminders]. Otherwise: `try { ReminderStore.rows(db).forEach { alarms.cancel(it.identifier, it.requestCode) }; ReminderStore.deleteAll(db) } catch (e: StoreFailure.Closed) {}`; `notices.removeDelivered()`; `try { ReminderStore.clearLedger(db) } catch (e: StoreFailure.Closed) {}`; `armedInThisProcess = false`.
- `delivered(accountId, identifier)` [R, OQ-12b-2; plan A13 "deletes its row and appends to the ledger"]: on the confined context, if `database?.accountID?.rawValue == accountId`: find the row; if present, `delete` it and `replaceLedger(elapsed(ledger(db), clock()) + ReminderLedgerEntry.from(row.toRequest()))`. When not attached, do nothing: the schedule-time ledger already records the request (§5.3).

**Test support** (port `ReminderTestSupport.swift:55–131`):
- `FakeReminderAlarms : ReminderAlarms`: `active: Map<String, Pair<ReminderRequest, Int>>`, `sets: List<ReminderRequest>` (appended only after a successful `set`, as iOS `adds`: a rejected `set` is not recorded), `cancels: List<String>`, `reject(blockId)` (then `set` throws `ReminderAlarmRefused` for requests with that `blockID`), `holdNext()` + `release()` (a `CompletableDeferred` the next `set` awaits), `setting: Boolean`, `maximumConcurrentSets`.
- `FakeReminderNotices`: counts `ensureChannel` and `removeDelivered`.
- `ReminderTestStore(accountId = reminderAccount)`: a Jdbc `AccountDatabase` opened for the id, an account row, and the settings row `reminders` = `ReminderSettings()` encoded as `{"light":true,"avoid":true,"caffeine":true,"winddown":true,"leadMin":30}`; `store(plan: ReminderPlan)` writes the trip (`keepOffline = true`, `currentInputHash`, `departureUtc` from the card) and the plan version (detail = `ApiJson.response` encoding of `plan.detail`, `receivedAt` = the ISO-8601 string of `reminderNow`, e.g. `DateTimeFormatter.ISO_INSTANT.format(reminderNow)`, as iOS `reminderNow.ISO8601Format()`; `plan_versions.received_at` is `TEXT`) and marks, through 4d's `TripRepository.upsert` and `TripRepository.store(plan, checks)`; `close()`. The store's `AccountDatabase` takes `io` = the test's `StandardTestDispatcher(testScheduler)` (not `Dispatchers.IO`), so every DB hop runs under virtual time and `advanceUntilIdle()` really waits for it; the Jdbc driver then runs on the test thread, which its one-connection-per-thread rule allows.
- `service(store, clock = { reminderNow }, alarms, notices, permission = { Granted }, language = { Language.En })` builds `ReminderService` with `FakeReminderStrings()` (12a), a `CopyLineFormatter` from `CopyCatalog` (as 12a's `ReminderTestSupport`), `PlanDecoder(worker = dispatcher)`, and `StandardTestDispatcher(testScheduler)`. Use `runTest(timeout = 60.seconds)` and `advanceUntilIdle()` / `testScheduler` instead of iOS `eventually` polling; never sleep.
- `pending()` in the ports below = `ReminderStore.rows(db).map { it.toRequest() }`.

**Tests** `ReminderServiceTests` (Swift names; `ServiceTests.swift`):
- `schedulingNeverRequestsAuthorization`, parameterized over the four `ReminderPermission` values: attach, `reschedule()`, change the language lambda's value and `reschedule()` again; the permission lambda was read, and a counting "request" fake on the same object stays at 0 (Android has no request path in 12b; the test guards that none is added).
- `fakeCenterCapsAt60AndKeepsUnchangedRequests`: 11 plans `trip-0…trip-10`, 6 blocks each at hours `0*3+1 … 5*3+1` (ordinals 1–6) → after attach 60 rows; a second `reschedule()` adds nothing (`sets.size == 60`); ledger has 60 entries.
- `replaceRemovePermissionAndFailures`: blocks at 1, 2, 3 h; `reject(reminderBlock(2.0).id)` → 2 rows, `sets.size == 2` after a second pass; upsert settings `leadMin = 60` → 1 row with `sendAt == now + 2 h`, and `cancels` contains the 1 h request's identifier; permission `Blocked` → 0 rows; `Granted` → 1; delete the `reminders` settings row → 0.
- `overlappingRequestsRerunWithLatestSettings`: blocks at 2 h and 3 h; `holdNext()`; attach; wait until `setting`; upsert `leadMin = 60`; two concurrent `reschedule()`; `release()`; then one more `reschedule()` → `maximumConcurrentSets == 1` and sorted `sendAt` = `[now + 1 h, now + 2 h]`.
- `serializedRerunAndSignOutDrain`: blocks at 1 h and 2 h; `holdNext()`; attach; wait until `setting`; launch `reschedule()` and `detach()`; `release()`; join both → no row, empty ledger, `alarms.active` empty.
- `checkOffUndoDisabledAndRelaunchLedger`: blocks `reminderBlock()` and `reminderBlock(1.1, AvoidLight)` → 1 row with 2 member keys; enqueue the check-off `CheckOffChange(tripID "trip", planInputHash "hash", SeekLight, "2026-10-14", 1, Done)` through `OutboxRepository.enqueue` → the row has 1 member key; the same with `status = null` → 2; enqueue `OutboxChange.Settings(Reminders, ReminderSettings.enabledPatch(false))` → 0 rows. Then `detach()`, `replaceLedger([entry from the first request])`, restore default settings, a **new** service with `clock = { request.sendAt + 1 s }`, attach, `reschedule()` → 0 rows.
- `currentOfflinePlansOnlyAndAccountSwitchClears`: the iOS sequence (changed hash → 0; `keepOffline = false` → 0; `deletedAt` set → 0; restored → 1); then `attach(next.database)` (a second `ReminderTestStore`) → `alarms.active` empty, the first store's rows and ledger empty.
- Android-only `dueRowsAreKeptAndExpiredRowsDropped` (OQ-12b-4): with a row whose `sendAt = now − 1 min`, `expiresAt = now + 10 min` and one with `expiresAt = now − 1 s`, a pass keeps the first (no cancel) and deletes the second; then upsert the `reminders` settings with all four flags `false` and pass again → the due row is cancelled and deleted.
- Android-only `firstPassInAProcessRearmsUnchangedRows` (OQ-12b-5): a pass, then a **new** service on the same store with fresh `FakeReminderAlarms` → every row is `set` again; a second pass of that service sets nothing.
- Android-only `detachWithoutAttachTouchesNothing` (OQ-12b-3): `detach()` on a fresh service → `removeDelivered == 0`, `cancels` empty.
- Android-only `deliveredDeletesTheRowAndRecordsTheLedger`: after a pass, `delivered(reminderAccount, id)` deletes that row and the ledger contains its member keys; `delivered("other-account", id)` changes nothing.

### Task 4. Android adapters: alarms, channel, notices, permissions

Commit: `feat(reminders): add alarm, channel and permission adapters`. Ticks plan.md Phase 12b "**Permission model.**".

**Files:** `AndroidReminderAlarms.kt`, `AndroidReminderNotices.kt`, `ReminderPermissions.kt`; Robolectric tests `AndroidReminderAlarmsTest.kt`, `AndroidReminderNoticesTest.kt`, `ReminderPermissionsTest.kt` (`@Logic`, `@RunWith(AndroidJUnit4::class)`, `@Config(sdk = [36])`).

**`AndroidReminderAlarms(context: Context, io: CoroutineDispatcher, sdkInt: Int = Build.VERSION.SDK_INT) : ReminderAlarms`**
- `alarmIntent(request, code)`: `Intent(ACTION_ALARM).setComponent(ComponentName(context, ReminderAlarmReceiver::class.java)).setIdentifier(request.identifier)` (API 29; part of `filterEquals`, so each identifier has its own `PendingIntent`), extras `EXTRA_IDENTIFIER`, `EXTRA_REQUEST_CODE`, `EXTRA_TITLE`, `EXTRA_BODY`, `EXTRA_TRIP_ID`, `EXTRA_BLOCK_ID`, `EXTRA_INPUT_HASH`, `EXTRA_ACCOUNT_ID` (omit when null), `EXTRA_EXPIRES_AT`.
- `set`: on `io`, `PendingIntent.getBroadcast(context, code, alarmIntent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`; below API 31 `alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.sendAt.toEpochMilli(), pi)` (no permission exists there), from API 31 `alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.sendAt.toEpochMilli(), pi)` (inexact; A13, OQ-A15, OQ-12b-6). If Lint's `ScheduleExactAlarm` still flags the API 29–30 call, suppress it at that call with `@SuppressLint("ScheduleExactAlarm")` and a one-line why (no exact-alarm permission exists below API 31). `IllegalStateException` (the 500-alarm limit) and `SecurityException` → `throw ReminderAlarmRefused(e)`.
- `cancel`: rebuild the same intent (component, action, identifier) and `PendingIntent.getBroadcast(context, code, intent, FLAG_IMMUTABLE or FLAG_NO_CREATE)`; when non-null, `alarmManager.cancel(pi)` and `pi.cancel()`.

**`AndroidReminderNotices(context, io, channelName: (Language) -> String) : ReminderNotices`**
- `ensureChannel(language)`: `NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT).setName(channelName(language)).build())`. Re-creating with a new name renames it; the user's importance is kept [P: the platform keeps importance on re-create; the test proves it under Robolectric].
- `removeDelivered()`: for each `notificationManager.activeNotifications` whose `notification.channelId == CHANNEL_ID` or whose `tag` starts with `TAG_PREFIX`: `cancel(tag, id)` (iOS `removeDelivered`, `SystemReminderCenter.swift:68–74`).
- `fun post(...)` lives in Task 5's `ReminderDelivery`, not here.

**`ReminderPermissions(context, devicePreferences: DevicePreferences, io, sdkInt: Int = Build.VERSION.SDK_INT)`**
- `val state: StateFlow<ReminderPermission?>` (null until the first `refresh`).
- `suspend fun refresh(): ReminderPermission` on `io`: `managerAvailable = context.getSystemService(NotificationManager::class.java) != null`; `runtimeGranted = sdkInt < 33 || ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED`; `requestedBefore = devicePreferences.notificationPermissionRequested.first()`; `notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()`; `channelBlocked = notificationManager.getNotificationChannel(CHANNEL_ID)?.importance == IMPORTANCE_NONE` (no channel yet → false); map with Task 2's `reminderPermission`, publish, return.
- Read-only: it never requests and never writes `notificationPermissionRequested` (12c does).

**Tests**
- `AndroidReminderAlarmsTest`:
  - `setUsesRtcWakeupAllowWhileIdleAtTheInstant`: one `set` → `shadowOf(alarmManager).scheduledAlarms` has one alarm, type `RTC_WAKEUP`, trigger `sendAt.toEpochMilli()`, allow-while-idle, operation a broadcast whose saved intent has component `ReminderAlarmReceiver`, action `ACTION_ALARM`, identifier, and the extras above (use the Robolectric 4.18 accessors of `ShadowAlarmManager.ScheduledAlarm`; check their names in the 4.18 sources).
  - `exactBelowApi31`: `AndroidReminderAlarms(context, io, sdkInt = 30)` with `ShadowAlarmManager.setCanScheduleExactAlarms(true)` [verify the 4.18 API]: one `set` → one alarm, `RTC_WAKEUP`, exact and allow-while-idle, trigger `sendAt.toEpochMilli()` [verify the `ScheduledAlarm` accessor that tells exact from inexact]; with the default `sdkInt` (36) the same `set` is inexact.
  - `absoluteTriggersSurviveDSTAndDeviceZoneChanges` (Swift name, `PermissionTests.swift:25–37`): for `2027-03-28T01:30:00Z`, `2027-10-31T01:30:00Z`, `2027-11-07T06:30:00Z`, set under `TimeZone.setDefault(Pacific/Kiritimati)` and again under `America/Los_Angeles` (restore in `finally`): the trigger equals the instant's epoch ms both times.
  - `sameIdentifierReplacesAndCancelRemoves`: two `set`s of one identifier with new content → one alarm with the new extras; two identifiers → two alarms; `cancel` of one → one left; `cancel` of an unknown identifier is a no-op.
  - `refusalIsWrapped`: an `AlarmManager` that throws is out of reach in Robolectric; instead unit-test the mapping helper (`refusing { throw IllegalStateException() }` → `ReminderAlarmRefused`).
- `AndroidReminderNoticesTest`:
  - `channelIsCreatedAndRelocalized`: `ensureChannel(En)` → channel `reminders`, importance `IMPORTANCE_DEFAULT`, name = the `en` value; set its importance to `IMPORTANCE_NONE` through the shadow, then `ensureChannel(Nl)` → name = the `nl` value, importance still `NONE`. The test's `channelName` reads the generated resource (`Reminders on this phone` / `Herinneringen op deze telefoon`) through a localized context, not a literal.
  - `removeDeliveredRemovesOnlyReminders`: post two notifications on `reminders` (tags `chronoflow.reminder.a`, `…b`) and one on another channel with another tag → after `removeDelivered()` only the third remains.
- `ReminderPermissionsTest`: `sdkInt = 36`: not granted + not requested → `Undetermined`; not granted + requested → `Blocked`; granted (`shadowOf(app).grantPermissions(POST_NOTIFICATIONS)`) → `Granted`; granted + `ShadowNotificationManager.setNotificationsEnabled(false)` → `Blocked`; granted + channel `IMPORTANCE_NONE` → `Blocked`. `sdkInt = 32` (same sandbox; the parameter drives the mapping): not requested + enabled → `Undetermined`; requested + enabled → `Granted` (the runtime permission is ignored); requested + `setNotificationsEnabled(false)` → `Blocked`. `state` emits each value.

### Task 5. Delivery

Commit: `feat(reminders): post reminder notifications from the alarm receiver`. Ticks plan.md Phase 12b "**Delivery.**".

**Files:** `ReminderRuntime.kt` (interfaces), `ReminderDelivery.kt`, `ReminderAlarmReceiver.kt`, `core/data/src/main/AndroidManifest.xml`; Robolectric tests `ReminderDeliveryTest.kt`, `ReminderAlarmReceiverTest.kt`, test app `TestReminderApplication.kt` (in `core/data/src/test/…`).

**Runtime seam** [R] (receivers and the worker are created by the system and cannot see `AppGraph`):
```kotlin
interface ReminderHost { val reminderRuntime: ReminderRuntime }      // implemented by ChronoFlowApp (Task 7)
interface ReminderRuntime {
    val io: CoroutineDispatcher
    val scope: CoroutineScope                    // appScope
    val notificationIcon: Int                    // R.drawable.ic_stat_moon (lives in :app)
    val tapComponent: ComponentName              // MainActivity
    val clock: () -> Instant                     // { Instant.now() } in :app (Refresh R5)
    suspend fun storedAccountId(): String?       // see below
    suspend fun ensureChannel()                  // AndroidReminderNotices.ensureChannel(current language)
    suspend fun delivered(accountId: String, identifier: String)  // ReminderService.delivered
    suspend fun backgroundPass()                 // Task 6/7
}
```
`storedAccountId()` (implemented in Task 7): `null` when `WipeMarker.read() != null` (a wipe is pending; `CorruptWipeMarkerException` → null) or when `StoredSession.parse(sessionStore.read() ?: return null)` is null; else that session's `accountId` lower-cased. It never deletes anything (do **not** use `readStoredSession()`, which deletes a bad blob).

**`ReminderAlarmReceiver : BroadcastReceiver`**, manifest `exported="false"` (OQ-12b-8): if `intent.action != ACTION_ALARM` return; `val runtime = (context.applicationContext as? ReminderHost)?.reminderRuntime ?: return`; `val pending = goAsync()`; `runtime.scope.launch(runtime.io) { try { withTimeoutOrNull(8.seconds) { ReminderDelivery.deliver(context.applicationContext, intent, runtime, clock = runtime.clock /* Refresh R5 */) } } finally { pending.finish() } }`.

**`ReminderDelivery.deliver(context, intent, runtime, clock)`** — order is load-bearing:
1. Read the extras; any missing required extra (`identifier`, `requestCode`, `title`, `body`, `tripId`, `expiresAt`) → return.
2. `now = clock()`; `if (expiresAt <= now) return` [R, OQ-12b-4: a late inexact alarm never shows stale advice].
3. Account check: `if (accountId != runtime.storedAccountId()) return` (a missing `accountId` extra also fails: 12a always sets it). This drops alarms left behind by a crash during sign-out or an account switch.
4. Permission: on API 33+ `checkSelfPermission(POST_NOTIFICATIONS) != GRANTED` → return; `!NotificationManagerCompat.areNotificationsEnabled()` → return (Lint `MissingPermission` needs the explicit check).
5. `runtime.ensureChannel()`.
6. Build and post:
   - `NotificationCompat.Builder(context, CHANNEL_ID)`: `setSmallIcon(runtime.notificationIcon)`, `setContentTitle(title)`, `setContentText(body)`, `setStyle(NotificationCompat.BigTextStyle().bigText(body))`, `setCategory(NotificationCompat.CATEGORY_REMINDER)`, `setAutoCancel(true)`, `setTimeoutAfter(expiresAt − now in ms)` (A13 "20 min": the group's expiry is at most `start + 20 min`; OQ-12b-19), `setContentIntent(tap)`. Nothing else: no colour, no sound override, no actions, no public version (no board).
   - `tap` = `PendingIntent.getActivity(context, requestCode, Intent(ACTION_OPEN).setComponent(runtime.tapComponent).setIdentifier(identifier).addFlags(FLAG_ACTIVITY_NEW_TASK)` with extras `EXTRA_TRIP_ID`, `EXTRA_BLOCK_ID`, `EXTRA_INPUT_HASH`, `EXTRA_ACCOUNT_ID`, `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`. Explicit component, never a URL (A16).
   - `NotificationManagerCompat.from(context).notify(identifier, requestCode, notification)` (tag = identifier, id = requestCode, A13).
7. `runtime.delivered(accountId, identifier)` (best effort; it is a no-op when the service is not attached).

Catch only `SecurityException` around `notify` (a revoked permission between step 4 and 6) and return.

**Manifest** (`core/data/src/main/AndroidManifest.xml`):
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<application>
  <receiver android:name="net.chronoflow.core.data.reminders.ReminderAlarmReceiver" android:exported="false" />
</application>
```
Remove 0b's `UnusedResources` ignore for `ic_stat_moon` in this commit (`lint.xml` l.18–19, the comment and the `<ignore>`; *[Refresh R3]*).

**Tests**
- `ReminderDeliveryTest` (`@Config(sdk = [36], application = TestReminderApplication::class)`; `TestReminderApplication : Application(), ReminderHost` with a settable fake runtime: `storedAccountId`, `delivered` recorder, `ensureChannel` creating the real channel, icon `android.R.drawable.ic_dialog_info`, tap component a test activity declared in the test manifest or `ComponentName(context, "x.Main")`):
  - `postsWithTagIdChannelAndTimeout`: grant `POST_NOTIFICATIONS`; deliver an intent built by `AndroidReminderAlarms.alarmIntent(request, 7)` with `expiresAt = now + 20 min` → `shadowOf(nm).getNotification(identifier, 7)` exists; channel `reminders`; `EXTRA_TITLE` = title; `EXTRA_BIG_TEXT` = body; category `reminder`; `FLAG_AUTO_CANCEL` set; `timeoutAfter == 20 min`; the content intent's saved intent has component = tap component, action `ACTION_OPEN`, the four `cf.reminder.*` extras; `delivered(account, identifier)` recorded.
  - `expiredAlarmIsDropped`: `expiresAt = now` → no notification, nothing recorded.
  - `otherAccountOrSignedOutIsDropped`: `storedAccountId` returns another id, then null → no notification.
  - `missingPermissionIsDropped`: permission not granted → no notification; notifications disabled → none.
- `ReminderAlarmReceiverTest`: `context.sendBroadcast(alarmIntent)` + `shadowOf(Looper.getMainLooper()).idle()` + advancing the fake runtime's test dispatcher → the notification is posted and the `PendingResult` finished (`shadowOf(receiver)…` or a finished-flag on the fake scope); a wrong action → nothing.

### Task 6. Upkeep: receivers, worker, WorkManager on demand

Commit: `feat(reminders): reschedule reminders after boot, time, zone and locale changes`. Ticks plan.md Phase 12b "**Upkeep.**".

**Files:** `ReminderUpkeepReceiver.kt`, `ReminderRescheduleWorker.kt`, `ReminderWork.kt`; manifest edits (`:core:data` and `app/src/main/AndroidManifest.xml`); `app/src/main/kotlin/net/chronoflow/app/ChronoFlowApp.kt` (`Configuration.Provider`); `:konsist` rule exception; tests `ReminderUpkeepReceiverTest.kt`, `ReminderWorkTest.kt`, `ReminderRescheduleWorkerTest.kt` (Robolectric `@Logic`, sdk 36), Konsist planted case.

**`ReminderWork`** (`object`):
- `fun rescheduleRequest(sdkInt: Int = Build.VERSION.SDK_INT): OneTimeWorkRequest` = `OneTimeWorkRequestBuilder<ReminderRescheduleWorker>()`, and **only when `sdkInt >= 31`** `.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` [R, OQ-12b-7: below API 31 an expedited request runs as a foreground service, which needs `FOREGROUND_SERVICE` (banned) and a `getForegroundInfo`].
- `fun enqueueReschedule(context)`: `WorkManager.getInstance(context).enqueueUniqueWork(WORK_RESCHEDULE, ExistingWorkPolicy.REPLACE, rescheduleRequest())`.
- `fun ensureRefill(context)`: `enqueueUniquePeriodicWork(WORK_REFILL, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<ReminderRescheduleWorker>(REFILL_HOURS, TimeUnit.HOURS).build())`. No constraints (the pass needs no network).
- `fun cancelAll(context)`: `cancelUniqueWork(WORK_RESCHEDULE)` and `cancelUniqueWork(WORK_REFILL)`.
- Every caller runs these on `io` (WorkManager's first `getInstance` opens its database).

**`ReminderUpkeepReceiver : BroadcastReceiver`**, `exported="false"` (system broadcasts still reach it, OQ-12b-8), one `<intent-filter>` with the five actions `android.intent.action.BOOT_COMPLETED`, `android.intent.action.MY_PACKAGE_REPLACED`, `android.intent.action.TIMEZONE_CHANGED`, `android.intent.action.TIME_SET`, `android.intent.action.LOCALE_CHANGED`. `onReceive`: unknown action → return; else `goAsync()` and, on the runtime's `io` in its `scope`, `ReminderWork.enqueueReschedule(context)`, then `finish()`. No `LOCKED_BOOT_COMPLETED`, no `directBootAware` (14 §7). [P: all five are exempt from the API 26 implicit-broadcast limit.]

**`ReminderRescheduleWorker(context, params) : CoroutineWorker`**: `doWork()` = `(applicationContext as? ReminderHost)?.reminderRuntime?.backgroundPass()`; return `Result.success()` in every non-cancelled case (the next trigger or the refill retries; a `Result.retry()` loop could keep a signed-out process busy). Concretely: `try { …backgroundPass() } catch (e: CancellationException) { throw e } catch (e: StoreFailure) { } catch (e: IOException) { }`, then `Result.success()`; any other exception is a bug and may crash the worker. Never override `getForegroundInfo`.

**WorkManager on demand** (plan 12b "Upkeep", AGENTS "Performance"):
- `ChronoFlowApp : Application(), Configuration.Provider` with `override val workManagerConfiguration get() = Configuration.Builder().setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.INFO else Log.ERROR).build()`. Nothing else in `onCreate` changes.
- `app/src/main/AndroidManifest.xml`: remove the default initializer:
  ```xml
  <provider android:name="androidx.startup.InitializationProvider" android:authorities="${applicationId}.androidx-startup"
      android:exported="false" tools:node="merge">
    <meta-data android:name="androidx.work.WorkManagerInitializer" android:value="androidx.startup" tools:node="remove" />
  </provider>
  ```
  *[Refresh R3]*: the provider is already declared with `tools:node="merge"` and two removed initializers; add only the WorkManager `<meta-data … tools:node="remove" />` line inside that block.
- Remove `android.permission.FOREGROUND_SERVICE`, the one permission WorkManager merges outside the allowlist [P: work-runtime declares it], and nothing else: `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" tools:node="remove" />` in `app/src/main/AndroidManifest.xml`. Read the merged release manifest and list every `<uses-permission>` in the commit body. The release allowlist (spec l.453) is `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, plus androidx.core's `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which must stay (0a OQ-0a-20; `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)` needs it on API 29–32); `verifyReleaseManifest` must pass unchanged. WorkManager's own services and receivers stay as merged (not exported, or exported with system-only permissions, which 14 §6.3 allows).
- Konsist: `noWorkManagerOutsideReminders` exempts exactly `app/src/main/kotlin/net/chronoflow/app/ChronoFlowApp.kt`; plant `violations/noWorkManagerOutsideReminders/app/src/main/kotlin/net/chronoflow/app/Other.kt` importing `androidx.work.WorkManager` and keep it failing.

**Tests** (`WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())` in `@Before`):
- `ReminderUpkeepReceiverTest.everyUpkeepActionEnqueuesOneReplacingReschedule`, `Parameterized`-free: loop over the five actions inside one test (the receiver is the unit; each action is one assertion line): send the broadcast to the receiver, idle the main looper and the runtime dispatcher → `getWorkInfosForUniqueWork(WORK_RESCHEDULE).get()` has exactly one item after each (any state: the test driver runs unconstrained one-time work at once on the `SynchronousExecutor`, and REPLACE keeps one item per unique name), and the fake runtime's `backgroundPass` count went up by one. `unknownActionDoesNothing` (no work item, `backgroundPass` count 0).
- `ReminderWorkTest.expeditedOnlyFromApi31`: `rescheduleRequest(30).workSpec.expedited == false`, `rescheduleRequest(31).workSpec.expedited == true` (`@SuppressLint("RestrictedApi")` for `workSpec`); `ensureRefill` twice → one periodic item with `intervalDuration == 12 h`; `cancelAll` → both unique works cancelled.
- `ReminderRescheduleWorkerTest.runsTheBackgroundPass`: `TestListenableWorkerBuilder<ReminderRescheduleWorker>(context).build().doWork()` (in `runTest`) → `Result.success()` and the fake runtime's `backgroundPass` ran once.
- `:app` test `ApplicationWorkConfigurationTest` (plain JVM): `ChronoFlowApp::class.java.interfaces.map { it.name }` contains `"androidx.work.Configuration\$Provider"` (Task 7 adds `"net.chronoflow.core.data.reminders.ReminderHost"` to the same assertion when it makes `ChronoFlowApp` a `ReminderHost`). Compare by name so the test file imports nothing from `androidx.work` (the Konsist exception names exactly one file). The "no I/O in construction" half is 4e's existing `AppGraphConstructionTest`, which must still pass.

### Task 7. Wire the scheduler into the session

Commit: `feat(app): schedule reminders for the signed-in account`. Ticks plan.md Phase 12b "**Scheduler**".

**Files** (`:app`): `reminders/AppReminderRuntime.kt`, `reminders/AndroidReminderStrings.kt`, `reminders/AppLanguageSource.kt`, `reminders/AppSessionHooks.kt` (or 4e's hooks file, edited), and edits to `AppGraph.kt`, `SessionGraphs.kt`, `SessionActivation.kt`, `ChronoFlowApp.kt` (`ReminderHost`), `RootContent.kt`, and the 3a post-`first-commit` call site; tests listed below.

**AppGraph members** (all `by lazy`, no I/O in construction; AGENTS "Performance"):
- `reminderPermissions = ReminderPermissions(context, devicePreferences, dispatchers.io)`.
- `reminderStrings = AndroidReminderStrings(context)`: implements 12a's `ReminderStrings`: `CopyIndex.find(CopyTable.Reminders, key)` → `localized(language).getString(entry.resId, time)`; `null` → `key.replace("{{time}}", time)`. `fun channelName(language) = localized(language).getString(<resource of "Reminders on this phone" from names.lock.json>)`. `localized(language)` = `context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(if (language == Language.Nl) "nl" else "en")) })` (avoid the deprecated `Locale(String)` constructor), cached per `Language` in a 2-entry map **inside the instance** (no global state).
- `appLanguage = AppLanguageSource(context, devicePreferences, dispatchers.io)`: `suspend fun current(): Language` on `io`. API 33+: `LocaleManager.applicationLocales` → empty = `LanguageChoice.System`, else `nl` → `Nl`, otherwise `En`; system locale = `LocaleManager.systemLocales[0]`. API < 33: `devicePreferences.language.first()` and `Resources.getSystem().configuration.locales[0]`. Resolve with 0e's `ConfigurationRules.resolveLanguage(choice, systemLocale)` and map `AppLanguage` → `Language`. If 10a's `LanguageDependencies` implementation exists in the stack, delegate to it instead (OQ-12b-18).
- `reminderNotices = AndroidReminderNotices(context, dispatchers.io, reminderStrings::channelName)`.
- `reminderService = ReminderService(AndroidReminderAlarms(context, dispatchers.io), reminderNotices, permission = { reminderPermissions.refresh() }, language = { appLanguage.current() }, strings = reminderStrings, formatter = { CopyLineFormatter(copyCatalogs.catalog()) /* copyCatalogs = the AppGraph's existing 1a CopyCatalogProvider (grep AppGraph.kt for CopyCatalogProvider); if none exists yet, add `val copyCatalogs by lazy { CopyCatalogProvider(dispatchers.io) }` and reuse it everywhere */ }, decoder = <the graph's PlanDecoder, or PlanDecoder(worker = dispatchers.default)>, clock = { Instant.now() }, dispatcher = dispatchers.default)`.
- `reminderTaps = ReminderTapRouter()` (Task 9 fills it).
- `reminderRuntime = AppReminderRuntime(...)`, built from explicit parameters, not from the whole graph, so `AppReminderRuntimeTest` can pass JVM fakes: `io`, `scope`, `notificationIcon`, `tapComponent`, `sessionStore: SessionStore`, `wipeMarker: suspend () -> WipeMarker` (it needs the storage root, resolved on io), `prepareBackground: suspend () -> Unit`, `startSessionGraphs: () -> Unit`, `session: suspend () -> SessionPresence`, `attachedAccount: StateFlow<String?>`, `reschedule: suspend () -> Unit`, `delivered: suspend (String, String) -> Unit`, `ensureChannel: suspend () -> Unit`. `ChronoFlowApp` implements `ReminderHost` with `override val reminderRuntime get() = graph.reminderRuntime`.

**`AppReminderRuntime`** implements Task 5's interface: `io = dispatchers.io`, `scope = appScope`, `notificationIcon = R.drawable.ic_stat_moon`, `tapComponent = ComponentName(context, MainActivity::class.java)`, `storedAccountId()` per Task 5 (`sessionStore` + `WipeMarker(root/<namespace>, io)`), `ensureChannel() = reminderNotices.ensureChannel(appLanguage.current())`, `delivered = reminderService::delivered`, and:
```kotlin
override suspend fun backgroundPass() {            // OQ-12b-1
    prepareBackground()                            // AppGraph: activation.prepareBackground()
    startSessionGraphs()                           // see below: make sure its controller.updates collection runs in this process
    val presence = withTimeoutOrNull(20.seconds) { session() } ?: return   // AppGraph: readiness.session(), never launchReadiness (R3)
    val id = (presence as? SessionPresence.SignedIn)?.accountId?.lowercase() ?: return
    withTimeoutOrNull(10.seconds) { attachedAccount.first { it == id } } ?: return  // AppGraph: reminderService.attachedAccount
    reschedule()                                   // AppGraph: reminderService.reschedule()
}
```
(`attachedAccount` holds `AccountID.rawValue`, which is lower-case; the presence id is the stored value, so compare lower-cased, as `storedAccountId()` does.)
- **`SessionGraphs` in a process with no activity.** A worker can run in a process where no activity started, so nothing may have started `SessionGraphs`' `controller.updates` collection, and the attach the pass waits for would never happen (10 s timeout, no reschedule). Read how 4e starts that collection (`grep -n "updates" app/src/main/kotlin/net/chronoflow/app/SessionGraphs.kt` and its call sites). If it starts when the lazy member is first touched, AppGraph passes `startSessionGraphs = { sessionGraphs }`; if it starts from a named method (`start()`/`observe()`) called on the launch path, call that method (idempotent) instead of the bare access. *[Refresh R3]*: it is the idempotent `SessionGraphs.start()` (l.43), called by `AppGraph`'s lazy (l.416): pass `startSessionGraphs = { sessionGraphs.start() }`. `AppReminderRuntimeTest` covers the order (see Tests).
- **`SessionActivation.prepareBackground()`** (new, under the activation `Mutex`): if no `ON_START` has been seen in this process (`startedOnce == false`, set by the first ON_START), call `sessionSync.setEnabled(false)`, so the session restore that the worker triggers never starts a pull (A7). The first ON_START sets `enabled = true` and `resume`s, as 4e already does.

**Hooks and session wiring** (iOS `NativeSessionController.swift:44–54, 98–111, 152–160, 186–217`):
- `SessionHooks.onSessionEnding = { reminderService.detach() }` (4e calls it first in `stopSync`).
- `SessionHooks.onForeground`: `ReminderSessionHooks` is the `next` of 11a's `GuestSessionHooks` (`AppGraph.kt` l.227–230), so the guest recover runs first; its `onForeground()` is `reminderService.reschedule()` only (*[Refresh R8]*: no `refresh()` before it; the pass reads the permission through its lambda).
- `SessionAccountDatabase(detach = { reminderTaps.clear(); reminderService.detach(); sessionGraphs.drop() })` (iOS order: taps, reminders, model).
- `SessionGraphs`: on a qualifying `SignedIn` update, **before** `accountModel.attach`: `reminderService.attach(database)`; then re-check `controller.epoch == update.epoch && controller.presence == update.presence`; if stale, `reminderService.detach()` and continue (iOS l.206–210); else `withContext(io) { ReminderWork.ensureRefill(context) }`.
- Sync trigger: when `SessionGraphs` starts collecting, also launch on `appScope`: `sessionSync.events().filterIsInstance<SyncEvent.Finished>().filter { it.result.state == SyncState.Idle }.collect { reminderService.reschedule() }` (iOS l.189–193). Cancel it with the rest in 4e's stop path.
- Language change (iOS `AppTabs.swift:340`): in `RootContent`, `LaunchedEffect(language) { graph.reminderService.reschedule() }` where `language` is the resolved app language of the current configuration (the same resolver 6a uses). This also re-localizes the channel on the next pass.
- Channel upkeep (plan l.337–341, launch step 5 "After `first-commit` … notification channel upkeep"): in `AppGraph.onFirstCommit()` (l.471–475; *[Refresh R5]*), `appScope.launch(dispatchers.io) { reminderRuntime.ensureChannel() }`. No other launch-path change.
- Time and zone changes while the app runs reach the scheduler through Task 6's receivers (`TIME_SET`, `TIMEZONE_CHANGED` → work → `backgroundPass` → `reschedule`), replacing iOS `ReminderTimeObservers`. A plain midnight is not observed (OQ-12b-15).

**Tests**
- `app/src/test/…/reminders/AndroidReminderStringsTest` (Robolectric `@Logic`): for each `ReminderKind`, `text(copyKey, En, "01:00")` and `(…, Nl, "01:00")` equal `FakeReminderStrings` (12a) for the same arguments; an unknown key returns itself with `{{time}}` replaced; `channelName(En)` = `Reminders on this phone`, `channelName(Nl)` = `Herinneringen op deze telefoon`.
- `AppLanguageSourceTest` (Robolectric, sdk 36): `LocaleManager` application locales `[]` with system `nl-NL` → `Nl`; `[en]` → `En`; `[nl]` with system `en-US` → `Nl`.
- `SessionActivationTest` additions (JVM, 4e fakes): `prepareBackgroundDisablesSyncOnlyBeforeTheFirstStart` (before ON_START → `setEnabled(false)` recorded; after ON_START → nothing); `foregroundReschedulesAfterTheGuestHook` (hook order: guest recover, then reschedule; *[Refresh R8]*).
- `SessionGraphsTest` additions: a `SignedIn` update attaches the service before the model; a stale update detaches it; `drop()` runs taps.clear, reminders.detach, then the model detach (one ordered log).
- `ReminderSyncTriggerTest` (JVM): a `Finished(…, SyncResult(Idle, 0))` event → one `reschedule()`; `Offline` and `Started` → none.
- `ApplicationWorkConfigurationTest`: add the `ReminderHost` interface name to Task 6's assertion.
- `AppReminderRuntimeTest` (JVM with fakes): `backgroundPass` while signed out → no reschedule; signed in and attached → one reschedule; the `SessionGraphs` collection is started before `readiness.session()` is awaited (ordered fake log: `prepareBackground`, `sessionGraphs`, `session`); `storedAccountId` → null with a wipe marker, null with a corrupt blob (and the blob is **not** deleted), the account id otherwise.

### Task 8. Sign-out teardown

Commit: `feat(reminders): cancel reminders and erase the ledger on sign-out`. Ticks plan.md Phase 12b "**Teardown.**".

**Files:** edit 4e's teardown wiring in `AppGraph.kt`; tests `core/data/src/test/…/reminders/ReminderSignOutTests.kt` (JVM), `core/data/src/test/…/reminders/ReminderTeardownTest.kt` (Robolectric `@Logic`).

**Behaviour**
- The teardown runs reminders twice by construction, both safe: step 7 (`stopSync` → `onSessionEnding` → `detach()`, while the DB is still open) and step 10 (`accounts.close()` → the detach closure → `detach()`, a no-op the second time).
- 4e's teardown `detach` lambda (step 9, voluntary, discard and forced sign-out) becomes `{ withContext(dispatchers.io) { ReminderWork.cancelAll(context) }; accountModel.detach(); withContext(dispatchers.main) { onSignedOut() } }` (OQ-12b-16; `AppGraph.kt` l.375–378 keeps its main hop). The refill is **not** cancelled in `onSessionEnding`, which also runs on every restore and accept (4e §5.2), including the worker's own restore.
- Nothing else changes in 4e's order.

**Tests**
- `ReminderSignOutTests.signOutWipesNotificationsLedgerAndAccountDirectory` (port of `SignOutTests.swift:15–57`, JVM): a real 4e `SessionController` + `SessionAccountDatabase(root, Mock namespace, JdbcAccountDriverFactory(), io, detach = { service.detach() })` + `AccountWiper` + an in-memory `SessionStore`; `teardown` with `beginWipe/erase/finishWipe` from the wiper, `stopSync = { service.detach() }`, `sleep = { delay(it) }`. `accept(result)` for `reminderAccount`; copy the trip and plan version of `reminderPlan()` and the default `reminders` settings into the active DB; `service.attach(db)` → 1 row and a non-empty ledger. `signOut(expected, discardPending = true) == SignedOut` → `alarms.active` empty, `notices.removeDelivered >= 1`, `accounts.activeAccountDatabase() == null`, the account directory is gone, the credentials are null.
- `ReminderTeardownTest.signOutLeavesNoAlarmOrNotification` (Robolectric, real `AndroidReminderAlarms` + `AndroidReminderNotices`, `AndroidAccountDriverFactory`): attach → `ShadowAlarmManager` has 1 alarm; deliver it with Task 5's `ReminderDelivery` (fake runtime, same account) → 1 notification on `reminders`; `service.detach()` → no scheduled alarm, no active notification on `reminders`, no row, empty ledger.

### Task 9. Taps

Commit: `feat(app): route reminder taps to the trip page`. Ticks plan.md Phase 12b "**Taps.**".

**Files:** `:core:navigation`: `ReminderTapExtras.kt` (edit 4e's file), `ReminderTapRouter.kt`, `shell/ShellNav.kt` (edit); `:app`: `reminders/ReminderTapIntentPolicy.kt`, `reminders/ReminderTapDispatcher.kt`, `reminders/ReminderTapRouting.kt`, edits to `PendingLaunchIntents.kt`, `MainActivity.kt`, `AppGraph.kt` (`launchIntentHandler`), `shell/ShellViewModel.kt`, `shell/AppShell.kt`; tests below (*[Refresh R6]*: no new `finishLaunch`/`MainActivity` call; the existing seam delivers).

**Behaviour**
- `ReminderTapExtras.parse(tripId: String?, blockId: String?, accountId: String?, inputHash: String?): ReminderTapExtras?` (iOS `ReminderTap(intent:)`, `ReminderTap.swift:10–23`, translated from URL rules to extras [R]): `null` when `tripId` is null, empty, or contains `/`, `?` or `#` (iOS rejects `chronoflow://trip/`, `…/a/b`, `…/a?x=1`, `…/a#b`); other fields are kept as given (null when absent).
- `ReminderTapRouter` (iOS `ReminderTapRouter`, main-safe, no I/O): `val pending: StateFlow<ReminderTapExtras?>`, `fun receive(tap: ReminderTapExtras)` (the latest wins), `fun clear()`.
- `ReminderTapIntentPolicy.tapToCapture(action: String?, flags: Int, restored: Boolean, extra: (String) -> String?): ReminderTapExtras?` [R, OQ-12b-14; same guards as 5c's `AuthLinkIntentPolicy`]: `null` unless `!restored`, `action == ACTION_OPEN`, and `flags and 0x00100000 == 0` (`FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`); then `ReminderTapExtras.parse(extra(EXTRA_TRIP_ID), extra(EXTRA_BLOCK_ID), extra(EXTRA_ACCOUNT_ID), extra(EXTRA_INPUT_HASH))`.
- `PendingLaunchIntents.capture(intent, restored)` uses the policy for the reminder part (replacing 4e's direct extras read); `consume()` keeps returning the reminder (*[Refresh R6]*: no `takeReminder()`). After capture, `MainActivity` also strips the reminder extras, and the action when it is `ACTION_OPEN`, from its intent (extend 5c's `setIntent(Intent(intent).setData(null))`, `MainActivity.kt` l.60, with `removeExtra` of the four keys), so a recreation never routes again.
- `ReminderTapDispatcher(router) : LaunchIntentHandler` (*[Refresh R6]*): `handle(intent) { intent.reminder?.let(router::receive) }`; `AppGraph.launchIntentHandler` (l.322, a no-op today) becomes it. The existing `finishLaunch` (cold tap: after the launch ending is chosen, never changing it, iOS 19c) and `handlePendingLaunchIntents` from `onNewIntent` (warm tap) already call it.
- `ShellNav.reminderTap(state: ShellNavState, tap: ReminderTapExtras, token: String): ShellNavState`: `selected = Trips`; Trips stack = `[Route.Trips, Route.TripPage(tripId = tap.tripId, blockId = tap.blockId, notificationInputHash = tap.inputHash, notificationToken = token)]`; other stacks and `overlay` unchanged (iOS does not dismiss the wizard cover, OQ-12b-13). `ShellViewModel.openReminder(tap)` applies it with `token = UUID.randomUUID().toString()`.
- `ReminderTapRouting(router, activeSession: suspend () -> SessionSnapshot?, open: (ReminderTapExtras) -> Unit)`, `suspend fun run()` (port of `AppTabs.swift:371–385`): `router.pending.filterNotNull().collect { tap -> router.clear(); val current = activeSession() ?: return@collect; if (tap.accountId == null || tap.accountId == current.session.accountId) open(tap) }`. `AppShell` runs it in `LaunchedEffect(Unit)` with `open = holder::openReminder` (`holder` = `ShellViewModel`) (`initial: true` semantics: a tap pending before the shell appears is routed when it appears; while the gate shows terms or verify, the shell is absent and the tap waits).
- What the trip page does with the route (open `Step(blockId)` once when the plan hash matches) is 6d's, already built (6d l.157).

**Tests**
- `core/navigation/src/test/…/ReminderTapTests` (JVM, Swift names):
  - `capturedJSONAndTripStep`: `parse("trip-id", "light", reminderAccount, "hash")` → those fields; each of `null`, `""`, `"a/b"`, `"a?x=1"`, `"a#b"` as `tripId` → `null`; `parse("t", null, null, null)` → only `tripId`.
- `app/src/test/…/reminders/ReminderTapLaunchTests.coldIntentPreservesEndingAndWarmRouterCanReopen` (JVM; Swift name; it lives in `:app` because 3b's `LaunchState` reducer is in `:feature:launch`, which `:core:navigation` cannot depend on, and `:app` already has 3b's `:feature:launch` test fixtures): if 4e added `pendingIntent` to `LaunchState`, drive the reducer (`session(SignedIn)`, `storedData(true)`, `gate(App)`) with a `PendingLaunchIntent(links = emptySet(), reminder = ReminderTapExtras("trip-id", "light", null, "hash"))` → ending `App`, route `Today`, `pendingIntent` unchanged (if 4e kept the field out of `LaunchState`, skip this half and say so in the commit body); then (always) `router.receive(tap)` → `pending.value?.blockId == "light"`; `clear()` → null; `receive` again → `tripId == "trip-id"`.
- `ShellNavReminderTapTest` (JVM, 6a's style): from `initial(Today)` with Today stack `[Today, TripPage("x")]` and an overlay `Wizard(Create)` → selected `Trips`, Trips stack `[Trips, TripPage("trip-id", blockId = "light", notificationInputHash = "hash", notificationToken = "tok")]`, Today stack unchanged, overlay unchanged.
- `ReminderTapIntentPolicyTest` (JVM): valid → extras; wrong action, `restored = true`, the history flag, a bad `tripId` → `null`.
- `ReminderTapDispatcherTest` (JVM, *[Refresh R6]*): `handle` with a reminder → `router.pending` is it; links only → `router.pending` stays null. Update `PendingLaunchIntentsTest` and `WarmLaunchIntentTest` (testDebug) for the `ACTION_OPEN` rule in the same commit.
- `ReminderTapRoutingTest` (JVM, `runTest`): same account → opened once and cleared; `accountId == null` → opened; another account → cleared, not opened; no active session → cleared, not opened.
- 4e's `PendingLaunchIntentsTest` (Robolectric): update for the new policy; add `reminderExtrasNeedTheOpenActionAndAreNotRestored`.

### Task 10. One emulator journey, plan updates

Commit: `test(reminders): deliver one reminder and open its step on the emulator`. Ticks plan.md Phase 12b "**Tests.**".

**File:** `app/src/androidTest/kotlin/net/chronoflow/app/reminders/ReminderDeliveryTest.kt` (`@Smoke`).

**Journey** (< 10 s, seeded, AGENTS "Tests" 2–4):
1. `GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)`.
2. Launch seeded through `SeedReceiverActivity`: `CF_AUTH_TEST_ID=<unique>`, `CF_UI_TEST_PROFILE=screen`, `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1` (mock account `0199a3c0-0000-7000-8000-000000000000`, its AMS→HND trip; *[Refresh R5]*: `…0001` is the two-factor account, not a trip). Wait for `tab.today` with `device.wait(Until.hasObject(By.res("tab.today")), 5_000)`.
3. From the instrumentation (same process): read the AMS→HND trip's id, its current plan hash and its first `seek_light` block id from the active account DB through `(app as ChronoFlowApp).graph` (do not hard-code them); build a `ReminderRequest(identifier = "chronoflow.reminder.e2e", title = "Amsterdam → Tokyo", body = "Seek bright light from 01:00.", sendAt = now + 2 s, tripID = <trip id read from the DB>, blockID = <id>, inputHash = <hash>, memberKeys = listOf("seek_light|x|1"), accountID = demo id, expiresAt = now + 10 min)`; `AndroidReminderAlarms(context, Dispatchers.IO).set(request, 4242)`. [P: an alarm less than 10 s ahead gets no inexact window, so it fires on time on a non-idle device.]
4. `device.openNotification()`; `device.wait(Until.findObject(By.text("Amsterdam → Tokyo")), 6_000).click()`.
5. `device.wait(Until.hasObject(By.res("trip.page")), 5_000)` and then `By.res("step.sheet")` ([F] `trip.page` is 6b's tag, `phase-6b-trip-page.md` l.148; `step.sheet` is the iOS id, `StepSheet.swift:80`, tagged by 6d, `phase-6d-trip-interactions.md` l.343).
6. In `@After`: cancel the alarm and `NotificationManagerCompat.cancelAll()`.

**Plan.md updates (same commit):**
- Tick the box. Add rows `| OQ-A567 | (12b OQ-12b-1) <question> | <default> | 12b |` … `| OQ-A586 | (12b OQ-12b-20) … |` for every §9 question to "Open questions for the user" after l.1167 (*[Refresh R1]*).
- A13 wording: "then deletes its row and appends to the ledger inside `goAsync`" → "appends the request to the ledger when it is scheduled (iOS), and, when the scheduler is attached, deletes its row and appends again on delivery (OQ-12b-2)". Phase 12b "Permission model" line: `ReminderPermissionState (granted, ask, blocked, unsupported)` → `ReminderPermission (Granted, Undetermined = ask, Blocked, Unsupported)` (OQ-12b-10). Phase 12b "Delivery": `setTimeoutAfter(20 min)` → `setTimeoutAfter(expiresAt − now)` (expiry ≤ step start + 20 min). A4 (*[Refresh R1]*: l.91 now ends "…; the reminder scheduler joins in 12b (OQ-A173)."): replace that clause with "the reminder scheduler is one process-wide service in `AppGraph`, attached per account DB (OQ-A586)"; the same at l.2335, and point OQ-A173 (l.774) at OQ-A586.

---

## 7. Acceptance criteria

1. `make targeted ONLY="core:data:ReminderStoreTests core:data:ReminderInputsTests core:data:ReminderRequestCodesTest core:data:ReminderPermissionMappingTest core:data:ReminderServiceTests core:data:AndroidReminderAlarmsTest core:data:AndroidReminderNoticesTest core:data:ReminderPermissionsTest core:data:ReminderDeliveryTest core:data:ReminderAlarmReceiverTest core:data:ReminderUpkeepReceiverTest core:data:ReminderWorkTest core:data:ReminderRescheduleWorkerTest core:data:ReminderSignOutTests core:data:ReminderTeardownTest core:navigation:ReminderTapTests core:navigation:ShellNavReminderTapTest app:ReminderTapLaunchTests app:AndroidReminderStringsTest app:AppLanguageSourceTest app:SessionActivationTest app:SessionGraphsTest app:ReminderSyncTriggerTest app:AppReminderRuntimeTest app:ReminderTapIntentPolicyTest app:ReminderTapDispatcherTest app:ReminderTapRoutingTest app:PendingLaunchIntentsTest app:ApplicationWorkConfigurationTest"` passes. Every JVM method < 1 s and every Robolectric method < 5 s in `build/test-results/<tier>/timings.json`.
2. `make quick` passes (Spotless, Konsist including the new planted WorkManager case, every JVM and `@Logic` test).
3. `make check` passes with `pixel` booted (the orchestrator runs it if the sandbox cannot): Lint as errors (no `MissingPermission`, no `UnusedResources` for `ic_stat_moon`), `testReleaseUnitTest`, `verifyReleaseManifest`, the R8 analysis, and `ReminderDeliveryTest` among the `@Smoke` set (< 10 s).
4. **Manifest:** the merged release manifest's `<uses-permission>` set is exactly `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, plus only androidx.core's `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (0a OQ-0a-20; never removed) (`grep -o 'uses-permission[^/]*' $(find app/build/intermediates -path '*release*' -name AndroidManifest.xml | head -1)`); no `FOREGROUND_SERVICE*`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`; `ReminderAlarmReceiver` and `ReminderUpkeepReceiver` are `exported="false"`; no `directBootAware`; `androidx.work.WorkManagerInitializer` is absent.
5. **Reboot, time-zone and time changes reschedule:** proven by `ReminderUpkeepReceiverTest` → `ReminderRescheduleWorkerTest` → `AppReminderRuntimeTest` (the chain). Device check by the orchestrator, recorded in the PR body (not a test): with the demo account seeded and reminders scheduled, `adb shell dumpsys alarm | grep -c "net.chronoflow.app.debug"` > 0; `adb reboot`; unlock; within 60 s the count is back above 0. Change the zone in Settings → System → Date & time; `dumpsys alarm` still lists the alarms at the same UTC instants.
6. **Sign-out leaves no alarm or notification behind:** `ReminderSignOutTests` and `ReminderTeardownTest` pass.
7. **Startup unchanged:** `AppGraphConstructionTest` passes (no I/O in construction); `Application.onCreate` gains no call; a seeded cold launch is StrictMode-clean (logcat has no `StrictMode` violation from `net.chronoflow` during `ReminderDeliveryTest`). *[Refresh R9]*: `make perf` and `make baseline-profile` apply (launch path changes); `make perf-timeline` does not (no timeline change).
8. Ten atomic Conventional Commits, no trailers; plan.md Phase 12b boxes ticked by Tasks 4–10 as stated (Task 3 ticks nothing); §9 rows added to plan.md.

---

## 8. Review checklist (Opus reviewer)

- [ ] **Actor port:** `ReminderService` state is touched only on its `limitedParallelism(1)` view; one flight; `rerun` loop; generation checked exactly where Swift checks (after the reads, after computing, in the add loop with `|| rerun`, before the ledger write); `detach` bumps the generation, cancels the observation and **joins** the flight before cancelling alarms. No `Mutex` across a suspension; no `runCatching`; `CancellationException` rethrown before every broad catch.
- [ ] **Triggers are complete:** DB observation of exactly `settings`, `trips`, `plan_versions`, `check_marks` with `distinctUntilChanged`; sync `Finished` + `Idle`; ON_START (`refresh` then `reschedule`); language change in `RootContent`; the five broadcasts; the 12 h refill. Reminder-table writes do not wake the observation (no feedback loop).
- [ ] **Never requests permission** anywhere in 12b; `ReminderPermissions` only reads; `notificationPermissionRequested` is never written.
- [ ] **Window and caps come from 12a only:** no `48`, `60`, `5_000`, `129_600`, `20` minute literal in `core/data/…/reminders` (grep).
- [ ] **Alarms:** `setExactAndAllowWhileIdle(RTC_WAKEUP, sendAt.toEpochMilli(), …)` below API 31 and `setAndAllowWhileIdle(RTC_WAKEUP, sendAt.toEpochMilli(), …)` from API 31 (A13); no other `setExact*`, never `ELAPSED_REALTIME*`; `FLAG_IMMUTABLE` on every `PendingIntent`; `Intent.setIdentifier(identifier)` on both the alarm and the tap intent; cancel rebuilds an equal intent (same component, action, identifier, request code).
- [ ] **Request codes** are reused from the existing row, allocated with probing otherwise, and unique in the table.
- [ ] **Remove rules:** future undesired rows cancelled; due rows (`sendAt ≤ now < expiresAt`) kept only while reminders are active (cancelled when settings or permission turn them off); expired rows deleted; the first pass in a process re-arms every desired alarm, later passes only changed ones.
- [ ] **Ledger:** written at schedule time for every kept request (failed `set` → no entry); `replaceLedger` floors ms; `delivered` only when attached to the same account.
- [ ] **Delivery order:** expiry check, account check (`storedAccountId`, wipe marker, no blob deletion), permission check, channel, post with tag = identifier and id = request code, `setTimeoutAfter(expiresAt − now)`, then `delivered`. All inside `goAsync` on `io`, bounded at 8 s, `finish()` in `finally`.
- [ ] **Background pass** never starts a pull: `prepareBackground` disables sync only before the first ON_START; the worker always returns `success`; it waits for attachment with a timeout and never blocks a thread.
- [ ] **WorkManager:** no eager initializer; `Configuration.Provider` on `ChronoFlowApp`; expedited only on API 31+; no `FOREGROUND_SERVICE` in the merged manifest; Konsist exception names exactly one file.
- [ ] **Teardown:** `onSessionEnding = detach`; the detach closure order taps → reminders → model; `ReminderWork.cancelAll` only in the sign-out `detach` lambda, never in `onSessionEnding`; `detach` on a never-attached service does nothing.
- [ ] **Taps:** explicit component, `ACTION_OPEN`; capture ignores restored and from-history launches and strips the extras afterwards; the router clears before the async account check; `ShellNav.reminderTap` leaves the overlay and other stacks alone; a fresh UUID token per tap.
- [ ] **No main-thread I/O:** every AlarmManager, NotificationManager, Keystore, DataStore, WorkManager and DB call is inside `withContext(io)`; nothing new in `Application.onCreate` or before `first-commit`.
- [ ] **No new copy, no colour, no invented UI:** the channel name and bodies come from 0c resources through `CopyIndex`; `android-only.json` is unchanged.
- [ ] Typical mistakes: `PendingIntent` without `FLAG_IMMUTABLE` (crash on API 31+); `notify(id)` without the tag; using `readStoredSession()` in the receiver (deletes a blob); `ExistingWorkPolicy.KEEP` for `reminders-reschedule` (must be `REPLACE`); `setExpedited` unconditionally; cancelling the refill in `onSessionEnding` (kills the running worker); comparing rows without `expiresAt`/`accountID`; `Instant.toString()` used as a DB key; a Robolectric test with several SDK sandboxes in `@Logic`.

---

## 9. Open questions (proceed on the default; Task 10 copies them into plan.md)

| ID | Question | Default |
|---|---|---|
| OQ-12b-1 | A boot, refill or time-change worker runs in a cold process. How does it reach the account DB without racing sign-out or syncing in the background (A7)? | Through the normal session path: `SessionActivation.prepareBackground()` disables sync until the first ON_START, `readiness.session()` restores, `SessionGraphs` attaches the scheduler, then `reschedule()`. Teardown serialization stays the controller's. |
| OQ-12b-2 | Plan A13 appends to the ledger on delivery; iOS appends at schedule time (OQ-12a-6). | Both: schedule-time (iOS, always) plus delivery-time row delete and append only when the scheduler is attached to the same account. Correctness never depends on the delivery append. |
| OQ-12b-3 | iOS `detach` removes every pending and delivered notification even when nothing was attached, which happens on every launch's restore. | Android `detach` does nothing when never attached, so a background restore never clears posted reminders. Attached detach (sign-out, account switch) removes everything. |
| OQ-12b-4 | Inexact alarms can fire after `sendAt`. Should a pass cancel an undesired row whose `sendAt` has passed, and should a late alarm still post? | Keep due rows (`sendAt ≤ now < expiresAt`) while reminders stay active; cancel them when the settings or the permission turn reminders off; delete rows past `expiresAt`; the receiver never posts at or after `expiresAt`. |
| OQ-12b-5 | `AlarmManager` cannot list alarms; force-stop and reboot clear them while rows remain. | The first successful pass in each process re-arms every desired alarm; later passes only changed ones (iOS "unchanged requests are kept"). |
| OQ-12b-6 | [P] `setAndAllowWhileIdle` is inexact. AOSP `AlarmManagerService.maxTriggerTime` gives a non-repeating inexact alarm a delivery window of 75 % of its futurity (none under 10 s), capped at 1 h from API 31; on API 29–30 there is no cap (a 40 h-ahead alarm may come up to 30 h late). In practice batching with other alarms usually delivers near the trigger, but a window above 20 min means the receiver's expiry check can drop the reminder. A13 only accepted "~9 min Doze" delays. | Decided (review 2026-11-16, A13, OQ-A15): exact `setExactAndAllowWhileIdle` on API 29–30, where it needs no permission; inexact on API 31+, whose window is capped at 1 h, and the receiver's expiry check drops a reminder that arrives late. The orchestrator still records `dumpsys alarm` windows for a 2 h and a 40 h alarm on `pixel` (API 36) as a check, not as the decision. |
| OQ-12b-7 | Expedited work below API 31 runs as a foreground service. | Expedited only on API 31+; plain one-time work below; `FOREGROUND_SERVICE` removed from the merged manifest. |
| OQ-12b-8 | `exported` on the upkeep receiver (14 §6.3 allowed exported). | `exported="false"`: system broadcasts still reach it. |
| OQ-12b-9 | The 12a request has `expiresAt`; 4a's `scheduled_reminder` has no column for it. | Add `expires_at INTEGER NOT NULL` in place (4a allows edits before the first release). |
| OQ-12b-10 | Plan says `ReminderPermissionState (granted, ask, blocked, unsupported)`. | 12a's `ReminderPermission`; `Undetermined` = ask. Plan wording fixed in Task 10. |
| OQ-12b-11 | 0b deferred "the notification accent colour" to 12b; no board draws one. | No `setColor`. Ask for a board if wanted. |
| OQ-12b-12 | A second tap on a reminder of the trip already open: 6d's entry content key drops `notificationToken`, so the page and its once-flag are reused and the step may not reopen (iOS opens it again). | Accept for 12b (the journey covers the first tap); flag to the 6d owner. |
| OQ-12b-13 | A tap while the wizard overlay is open. | As iOS: route the Trips tab underneath; the overlay stays. |
| OQ-12b-14 | Which intents count as a reminder tap? | Only `ACTION_OPEN`, not restored, not launched from history; the extras are stripped after capture. |
| OQ-12b-15 | iOS also reschedules on `significantTimeChangeNotification` at midnight. | Not observed on Android (`ACTION_DATE_CHANGED` is not a manifest-exempt broadcast); ON_START, sync and the 12 h refill cover it. |
| OQ-12b-16 | When to cancel the refill work. | Only in the sign-out teardown's `detach` lambda; never in `onSessionEnding` (it runs on every restore, including the worker's). |
| OQ-12b-17 | `ChronoFlowApp` must import `androidx.work` for `Configuration.Provider` (OQ-4d-17). | One named Konsist exception for that file. |
| OQ-12b-18 | Source of the app language outside the UI (worker, receiver). | `AppLanguageSource` (LocaleManager on 33+, DataStore below, 0e resolver); delegate to 10a's implementation when present. |
| OQ-12b-19 | Plan says `setTimeoutAfter(20 min)`. | `expiresAt − now` (the group's expiry, at most 20 min after the step start; OQ-12a-3). |
| OQ-12b-20 | A4 lists the reminder scheduler as a member of the per-epoch `SessionGraph` (4e: "The reminder scheduler joins in 12b"). iOS keeps one `ReminderService` per `NativeSessionController` and attaches/detaches it per account database, and the receivers and the worker need it without a session graph. | One process-wide `ReminderService` in `AppGraph`, attached to the account DB by `SessionGraphs` and detached by the teardown hooks (iOS shape). `SessionGraph` gets no scheduler member. Record the A4 wording change in Task 10. |

None blocks the user. OQ-A14 (12 h refill, timeout: yes), OQ-A15 (exact alarms only on API 29–30) and OQ-A33 (Live Updates: no) are applied as decided.

---

## 10. Hand-off to 12c

- `ReminderPermissions.state` / `refresh()`; 12c writes `notificationPermissionRequested` when the person taps **Turn on reminders** (before the system dialog on API 33+; below API 33 that write is the in-app opt-in), then calls `refresh()` and `reminderService.reschedule()`.
- The settings patches go through the outbox (`ReminderSettings.enabledPatch`, `leadPatch`); the scheduler reacts through its DB observation, no call needed.
- Open phone settings: `Settings.ACTION_APP_NOTIFICATION_SETTINGS` (+ `EXTRA_APP_PACKAGE`), or `ACTION_CHANNEL_NOTIFICATION_SETTINGS` with `EXTRA_CHANNEL_ID = "reminders"` when only the channel is blocked (`07` §7.3).

---

## Appendix A. iOS → Kotlin test map

| iOS (`Tests/RemindersTests/`) | Kotlin | Task |
|---|---|---|
| `ServiceTests.swift` `schedulingNeverRequestsAuthorization`, `fakeCenterCapsAt60AndKeepsUnchangedRequests`, `replaceRemovePermissionAndFailures`, `overlappingRequestsRerunWithLatestSettings`, `serializedRerunAndSignOutDrain`, `checkOffUndoDisabledAndRelaunchLedger`, `currentOfflinePlansOnlyAndAccountSwitchClears` | `core/data` `ReminderServiceTests.<same>` | 3 |
| `PermissionTests.swift` `nativeAuthorizationMatrix` | `ReminderPermissionMappingTest.nativeAuthorizationMatrix` | 2 |
| `PermissionTests.swift` `absoluteTriggersSurviveDSTAndDeviceZoneChanges` | `AndroidReminderAlarmsTest.<same>` | 4 |
| `PermissionTests.swift` `mockAuthorizationChangesOnlyAfterAnExplicitRequest` | — | 12c |
| `SignOutTests.swift` `signOutWipesNotificationsLedgerAndAccountDirectory` | `ReminderSignOutTests.<same>` | 8 |
| `TapTests.swift` `capturedJSONAndTripStep` | `core/navigation` `ReminderTapTests.<same>` | 9 |
| `TapTests.swift` `coldIntentPreservesEndingAndWarmRouterCanReopen` | `app` `ReminderTapLaunchTests.<same>` (the reducer lives in `:feature:launch`) | 9 |
| `ReminderTestSupport.swift` `FakeReminderCenter`, `ReminderTestStore` | `FakeReminderAlarms`, `FakeReminderNotices`, `ReminderTestStore` | 3 |

Android-only: `ReminderStoreTests`, `ReminderInputsTests`, `ReminderRequestCodesTest`, the four Android-only `ReminderServiceTests` methods, `AndroidReminderNoticesTest`, `ReminderPermissionsTest`, `ReminderDeliveryTest`, `ReminderAlarmReceiverTest`, `ReminderUpkeepReceiverTest`, `ReminderWorkTest`, `ReminderRescheduleWorkerTest`, `ReminderTeardownTest`, `AndroidReminderStringsTest`, `AppLanguageSourceTest`, `ReminderSyncTriggerTest`, `AppReminderRuntimeTest`, `ShellNavReminderTapTest`, `ReminderTapIntentPolicyTest`, `ReminderTapDispatcherTest`, `ReminderTapRoutingTest`, `ApplicationWorkConfigurationTest`, and the emulator `ReminderDeliveryTest`.
