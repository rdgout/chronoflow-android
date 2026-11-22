# Phase 12a brief: Reminder engine (JVM)

- **Branch:** `feat/phase-12a-reminders-engine`, stacked on the stack top once 1a is in the stack. *[Refresh: the branch exists at `8b3b592e`, phases 0a–11b.]*
- **Depends on:** 1a (`planDate`, `checkIdentity`, `buildCheckState`, `withoutOptionalSteps`, `CopyCatalog`, `CopyLineFormatter`, `CopyFormat`, `zoneLabel`), 0d (contracts, `ApiJson`, `Fixtures`, `copy_fixtures.py`), 0c (`strings_ios.xml`, `names.lock.json`), 0a (`:core:reminders` module, Konsist). **Wave:** 4. **Milestone:** M5.
- **Plan entry:** `plan.md` "### Phase 12a: Reminder engine (JVM)" (l.3428–3458; *[Refresh R1]*); decisions A3 (l.90), A5 (l.92), A13 (l.100).
- **Brief written:** 2026-11-16 from iOS read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`), RN at `e24872af` (the fixture's source commit; RN `HEAD` is the same commit), web at pin `e36368ec`.
- **Tests:** JVM only (JUnit 4.13.2, assertk 0.28.1). No Robolectric, no emulator, no tier annotation. Follow AGENTS.md "Tests".
- **Checked 2026-11-16** (adversarial pass against iOS `f391a1fa` `Sources/Reminders`, `Tests/RemindersTests`, the fixture bytes, web `e36368ec` routes/requests, plan.md, AGENTS.md and the 0a/0c/0d/1a/4a/4d briefs). Fixes: the parity decoder ignores `secondaryTz`/`secondaryText` and the timeline's `days`/`transferWarnings` (they are not null in the fixture); patch key order is an Android choice (iOS sorts keys); `ReminderSettings` companion members spelled out; `ReminderKind` gains `copyKey` in Task 5 explicitly; test-helper `endUtc` without fraction digits and `BlockParams` wrappers; `elapsedDailyCapAndDST` and `checksOptionalAndIfYouCan` construction made exact; `reminderPlan` values inlined; Task 6 repo root through `chronoflow.repoRoot` with declared inputs; the fixture copier's orphan rule covers `fixtures/reminders/`.

## Refresh notes (2026-11-22, against 8b3b592e)

Checked against the code at `8b3b592e` (phases 0a–11b; the branch `feat/phase-12a-reminders-engine` already exists and points there), iOS at `f391a1fa`, RN at `e24872af`, web at `e36368ec`, and `plan.md`. **Where a note and the body below disagree, the note wins.** The worker repeats these checks first (`git grep` each name; read each cited line) and stops on any mismatch not listed here. Scope and deliverables are unchanged. Path aliases: `…/cp/` = `core/plan/src/main/kotlin/net/chronoflow/core/plan/`; `…/cmc/` = `core/model/src/main/kotlin/net/chronoflow/core/model/contracts/`; `…/ct/` = `core/testing/src/main/kotlin/net/chronoflow/core/testing/`.

**R1. Plan, OQ and research references.**
- `plan.md` "### Phase 12a: Reminder engine (JVM)" is at **l.3428–3458** (not 2630–2660). Boxes: **Engine** l.3448, **Parity** l.3453, **Copy** l.3454; acceptance l.3456–3458. Decisions: A3 l.90, A5 l.92, A13 l.100. The time-hygiene sentence is **l.1637** (not 1028). Source rows R13 l.57 and R23 l.68. Related decided rows: OQ-A118 l.719 (4a-6: local `reminder_ledger`/`scheduled_reminder` tables, `send_at_ms` INTEGER; relevant to OQ-12a-5) and OQ-A177 l.778 (tap extras; 12b).
- The last plan OQ is **OQ-A556** (11b's OQ-11b-13, l.1157). This phase's §9 rows map to **OQ-A557 … OQ-A566**, in order: OQ-12a-1 → A557, -2 → A558, -3 → A559, -4 → A560, -5 → A561, -6 → A562, -7 → A563, -8 → A564, -9 → A565, -10 → A566. Task 6's commit appends them to "Open questions for the user" after l.1157, each row starting "(12a OQ-12a-n)" as the 11a/11b rows do, with the phase column `12a` (OQ-12a-3, -5, -6, -7: `12a, 12b`). A further question takes **A567** and up. The body keeps citing "OQ-12a-n"; the 12b brief cites the same local numbers.
- Research: `07-day-to-day-settings.md` §7.1 l.452–486 ✓, §7.2 **l.487–504**; `09-rn-android-lessons.md` §9 **l.374–386**. Briefs: 0d `copy_fixtures.py`/`Fixtures` text is **l.202–244** (shapes §5.4.2 at l.318); 0a module table l.122 ✓, its `JVM_MODULES` line is l.649 (the live `Makefile` line is **l.11** and also lists `core:testing-junit`); 0c lock-key rule l.230 ✓.
- The plan names the parity test `ReminderParityTest` (l.3453); OQ-12a-1 stands (Swift names). It names `ReminderPermissionState` in 12b (l.3491); OQ-12a-7 stands.

**R2. What already exists that 12a builds on (verify; do not rebuild).**
- **`:core:reminders` is an empty skeleton** (0a, `d82004ba`): only `core/reminders/build.gradle.kts` with `plugins { id("chronoflow.jvm.library") }`; no sources, no tests, no dependencies. Namespace/package to use: `net.chronoflow.core.reminders`. It is in `Makefile` `JVM_MODULES` (l.11, so `make quick` runs `:core:reminders:test`), in `Scripts/targeted.py`'s module list, and in Konsist `jvmCoreModules` (`konsist/…/KonsistRules.kt` l.9–20, rule `jvmCoreModulesImportNoAndroidOrCompose`) and `timeHygieneModules` (l.291, rule `noDefaultLocaleZoneOrClockInLogic` l.356, main source set only, skips `*.generated.kt`; it also flags `DateTimeFormatter.ofPattern(` without a locale argument and `"…".format(`). `testingNotInMain` (l.273) forbids `net.chronoflow.core.testing` imports in main.
- **`chronoflow.jvm.library`** (`build-logic/…/JvmLibraryConventionPlugin.kt`) applies Kotlin JVM + Android Lint, JDK 17 target with **`allWarningsAsErrors = true`** (`KotlinOptions.kt` l.15), a `timingTest` task, and adds JUnit 4 + assertk to `testImplementation`. It does **not** apply the serialization plugin: add `alias(libs.plugins.kotlin.serialization)` as `core/plan/build.gradle.kts` and `core/wizard/build.gradle.kts` do (Task 2).
- **`:core:plan` (1a), as merged**, all in `…/cp/`: `fun planDate(value: String): Instant?` (`PlanTime.kt` l.12; 10-char dates → noon UTC, 17-char `…THH:mmZ` accepted, fractions accepted); `fun checkIdentity(key: CheckKeyView): String` (`CheckState.kt` l.15); `fun buildCheckState(plan: PlanView, marks: List<CheckMark>): CheckState` (l.17); `fun withoutOptionalSteps(plan: PlanView): PlanView` (`PlanLogic.kt` l.28); `data class CopyFormat(language, timeFormat = Value24, displayZone: String? = null, annotateInstantZone = false, trip: TripDetailView? = null)` (`CopyLineFormatter.kt` l.19); `class CopyLineFormatter(val catalog: CopyCatalog)` (l.27) with **public** `formatInstant(value: String, options: CopyFormat, zone: String? = null): String` (l.139); the `Instant` overload (l.148) is `internal`, so call the `String` one as the body says; `fun zoneLabel(zone: String, trip: TripDetailView? = null)` (l.305); `class CopyCatalog(data: ByteArray)` (`CopyCatalog.kt` l.9; `CopyCatalog.load(io)` is `suspend`, so tests use the constructor as `PlanCoreTestSupport.kt` l.35 does: `CopyCatalog(checkNotNull(CopyCatalog::class.java.getResourceAsStream("/plan_copy.json")).use { it.readBytes() })`; `plan_copy.json` is `core/plan/src/main/resources/`, on `:core:reminders`' classpath through `api(project(":core:plan"))`). The zone cache `planZoneRules`/`ZoneRulesCache` (`PlanTime.kt` l.111–126) is `internal`: keep §6 Task 3's per-call local `HashMap`. `:core:plan` already depends on `api(project(":core:model"))`, serialization and coroutines.
- **`PlanClock`** (`…/cp/PlanClock.kt`, `now` + `values()`), `SystemPlanClock` (`:core:data` `…data.clock`), `FakePlanClock(start)` (`…/ct/FakePlanClock.kt`), `WizardTestClock` (`…/ct/wizard/WizardTestSupport.kt` l.21, starts at `Instant.ofEpochSecond(1_800_000_000)`) and `CheckingTestClock(scheduler)` (`…/ct/wizard/CheckingTestClock.kt`) exist. **12a uses none of them**: every engine function takes `now: Instant` as a parameter, there is no coroutine and no dispatcher in 12a (`AppDispatchers(main, io, default)` in `:app` and the injected `io` dispatcher pattern belong to 12b's caller). Tests use the ported constant `reminderNow` (`2026-10-14T00:00:00Z`) and vector `now`s, never `Instant.EPOCH`, `Instant.now()` or a default zone.
- **Contracts (0d)** in `…/cmc/TripPlan.generated.kt`: `BlockView` l.859, `CheckKeyView` l.1030, `TripDetail` l.428, `BlockReasonTagsView` l.750, `SleepParamsView` l.2572, `TimelinePresentationView` l.2894, `TripCardView` l.2987; `sealed interface BlockParams` with `Sleep(value)`, `SeekLight(value)`, `AvoidLight(value)`, `NoCaffeine(value)`, `Unknown(value: JsonElement)` and `companion fun decode(type, element, json)` in `…/cmc/PlanContracts.kt` l.8. Generated enums are `sealed interface X : RawEnum` with `data object` cases, `Unknown(raw)`, `.raw` and `X.of(raw)` (e.g. `TimeFormat` l.2757: `of("12")` → `Value12`, `of("24")` → `Value24`, anything else → `Unknown`), so a `when` over them needs an `else` branch. **Generated constructors have no defaults** (one `= null` in the whole file): see R4.
- **`ApiJson`** (`core/model/…/api/ApiJson.kt`) has both `response` (`ignoreUnknownKeys`, `explicitNulls = false`) and `request` (`explicitNulls = true`, `encodeDefaults = false`). Task 2's "if 0d's `ApiJson` has no `request`" fallback is moot: use `ApiJson.request`.
- **Settings as stored (4d, 10b).** `StoreApplication.snapshot` (`core/data/…/store/StoreApplication.kt` l.66–75) writes the server's `settings.reminders` `JsonObject` (`AccountTripContracts.kt` l.20, camelCase `{light, avoid, caffeine, winddown, leadMin}`) verbatim as settings section `"reminders"`, and `settings.preferences` as `"preferences"` (`timeFormat` is the string `"12"`/`"24"`; 10b's `SettingsPreferencesModel` writes `timeFormat` locally and enqueues `time_format`). Nothing in the tree decodes the reminders section yet. `SettingsSection.Reminders("reminders")` and `OutboxChange.Settings(section, patch: JsonObject)` are in `core/model/…/OutboxChange.kt` l.15–31; the allowed reminder patch keys `light, avoid, caffeine, winddown, lead_min` are already enforced by `SettingsPatchValidation` (l.19) and `OutboxPayload.safeSettings` (l.198), so Task 2's patches are accepted as built. Existing time-format readers differ from iOS's reminder rule and must **not** be reused for `reminderTimeFormat`: `planPreferences(section)` (`…/cp/trippage/PlanPreferences.kt`; a non-`"12"`/`"24"` string becomes `TimeFormat.Unknown`), and the `:app` readers in `SessionTripsDependencies.kt` l.55 / `SessionTripWizardDependencies.kt` l.58 (accept a JSON number `12`).
- **Copy (0c).** All four keys exist: `tools/l10n/names.lock.json` l.218 (`reminders_avoid_bright_light_from_time`), l.222 (`reminders_last_caffeine_before_time`), l.233 (`reminders_seek_bright_light_from_time`), l.240 (`reminders_wind_down_for_bed_sleep_from_time`); en and nl values in `core/ui/src/main/res/values{,-nl}/strings_ios.xml` use `%1$s` and equal the §6 Task 5 table exactly. `CopyIndex`/`CopyTable.Reminders` are in `core/ui/…/l10n/generated/CopyIndex.generated.kt` (12b's concern).
- **Fixtures (0d).** `Fixtures.text(path)`/`bytes(path)`/`manifest()` (`…/ct/Fixtures.kt`); `core/testing/src/main/resources/fixtures/{SOURCE.md,fixtures.sha256}` exist; there is no `fixtures/reminders/` yet. `tools/contracts-gen/copy_fixtures.py` `SOURCES` (l.19–22) has two 2-tuples, `(IOS, ["account", …, "wizard"])` and `("Packages/ChronoFlowKit/Tests/PlanCoreTests/Fixtures", ["intl-cases.json"])`; `collect()` (l.34–49) unpacks `for base, entries in SOURCES` (a 3-tuple breaks it: read the optional third element explicitly) and owned sub-directories go through `owned_dirs`/`is_owned` (l.64): add `"reminders"` there so `--check` catches an orphan. The foreign `mock-seed.json` manifest line (l.24, from `make mock-seed`) must survive. `FixtureManifestTest` asserts per-directory counts only for the six API dirs, so it needs no count change, but `everyFixtureFileIsInTheManifest` fails unless the generated manifest line lands in the same commit as the file. `make contracts`/`contracts-check` are `Makefile` l.144–149; `Scripts/test_scripts.sh` l.5 runs `tools/contracts-gen/test_*.py` in `make quick`.
- **`:core:testing`** (`core/testing/build.gradle.kts`) already has `api(project(":core:plan"))` and `implementation(project(":core:wizard"))`; adding `implementation(project(":core:reminders"))` (Task 5) mirrors `:core:plan`'s `testImplementation(project(":core:testing"))` and makes no cycle.
- **Neighbours 12a must not pre-empt.** `SessionHooks { onForeground(); onSessionEnding() }` and `NoopSessionHooks` (`core/data/…/session/SessionHooks.kt`) are composed in `AppGraph.hooks` as `GuestSessionHooks(guestWizardStore, NoopSessionHooks)` (`AppGraph.kt` l.227–230); 12b's scheduler and teardown plug into that chain. The tables `reminder_ledger` and `scheduled_reminder` exist (`core/database/src/main/sqldelight/…/ReminderLedger.sq`, `ScheduledReminder.sq`; no `expires_at` column, OQ-12b-9). `ReminderTapExtras` (`core/navigation/…/PendingLaunchIntent.kt` l.22), `Route.Reminders`, `DevicePreferences.notificationPermissionRequested` (`core/datastore/…/DevicePreferences.kt` l.52), the empty `:feature:reminders` skeleton, 10b's "Connectivity & reminders" row (`PreferencesScreen.kt` l.157) and `FirstTripRemindersOffer.queue(firstTrip)` (`core/data/…/reminders/FirstTripRemindersOffer.kt`, 9g: writes `"wizardReminders"` = `"pending"`) are 12b/12c territory. 12a adds nothing to `:core:data`, `:app`, `:core:navigation`, `:core:database` or any feature.
- **What 12a adds** (none of it exists): in `:core:reminders` main `ReminderSettings.kt` (`ReminderSettings`, `ReminderPermission`, `ReminderKind`, `reminderTimeFormat`), `ReminderSelection.kt` (`ReminderMember`, `ReminderDraft`, `ReminderSelectionOptions`, `reminderLocalDay`, `reminderDrafts`), `ReminderLedger.kt` (`ReminderLedgerEntry`, `ReminderLedger`, `ReminderLedgerEntry.from`), `ReminderSchedule.kt` (`REMINDER_*` constants, `ReminderPlan`, `ReminderRequest`, `reminderRequests`), `ReminderStrings.kt`; `FakeReminderStrings` in `…/ct/reminders/`; the Task 1 copier change and `fixtures/reminders/selection.json` + manifest line + `SOURCE.md` paragraph; the §6 tests; `core/reminders/build.gradle.kts` and `core/testing/build.gradle.kts` dependency lines; the plan ticks and OQ-A557–A566 rows.

**R3. iOS references.** Every cited path exists at `f391a1fa` and the cited ranges hold: `ReminderSelection.swift` (149 lines; l.5–24, 26–29, 31–36, 38–48, 50–57, 59–67, 69–74, 76–149 and the step lines of Task 3), `ReminderSchedule.swift` (123; l.6–15, 17–27, 29–36, 38–50, 52–123, constants l.65/71/74/82/122), `ReminderLedger.swift` (46; entry l.5–17, `decode` l.20–30, `elapsed` l.31–34 with the 36 h bound at l.33, `read`/`write`/`clear` l.35–45), `ReminderService.swift` (158; inputs/`plans` l.21–39, settings decode l.110–112, `timeFormat == "12"` l.117–120, ledger append l.134–140), `RemindersModel.swift` (90; `active` l.21, `setLead` l.62–72 with `[15, 30, 60]` l.63 and `lead_min` l.68, `write` l.73–84), `LocalStore/LocalMutations.swift` (`.sortedKeys` encoder l.10–14), `Reminders.xcstrings`, `Tests/RemindersTests/` `SelectionParityTests.swift` (81; suite `ReminderSelectionTests` l.29, `allRNSelectionVectors` l.30–56, `Options.Timeline` l.24, `ParityBlock` l.59–81), `ScheduleTests.swift` (148; suite `ReminderScheduleTests` l.19; `windowMarginAndCap` l.20, `elapsedDailyCapAndDST` l.36, `ledgerSuppressesMergedMembersAndScopesTripAndHash` l.55, `checksOptionalAndIfYouCan` l.71, `copyKindsZonesFormatsLanguagesAndStableIdentity` l.89, `ledgerValidationAndExpiry` l.131), `PermissionTests.swift` (45; suite `ReminderPermissionTests`, `permissionMatrix` l.18–24), `ReminderTestSupport.swift` (131; `reminderNow`/`reminderAccount` l.8–9, `reminderBlock` l.11–30, `reminderPlan` l.31–53), `Fixtures/selection.json` (1,046,865 B, SHA-256 `0ac14ed1…b2f` ✓; `source` `src/reminders/__tests__/selection.test.ts`, `commit` `e24872af…`, = RN `HEAD`), `Tools/ReminderParity/export-selection.cjs` (35), `Docs/phase-19-reminders.md` l.5/7/25, iOS `plan.md` l.2212 and 19b l.2217. The §6 fixture facts were recounted and hold (110 cases, 53 distinct names, 872 blocks, 423 check keys, 242 drafts, 280 members, 57 empty; option keys 13/9/4/2). The two golden identifiers in `identifierGolden` were recomputed and hold. Web: `ReminderSettingsView.php` l.11–15, `PreferenceRules.php` l.22–26, `routes/api.php` l.42/70 ✓. Ported Swift test names: `ReminderSelectionTests.allRNSelectionVectors`, `ReminderPermissionTests.permissionMatrix`, `ReminderScheduleTests.{windowMarginAndCap, elapsedDailyCapAndDST, ledgerSuppressesMergedMembersAndScopesTripAndHash, checksOptionalAndIfYouCan, copyKindsZonesFormatsLanguagesAndStableIdentity, ledgerValidationAndExpiry}`.

**R4. Test support, corrected (Task 5 `ReminderTestSupport.kt`; Task 3 parity decoder).** The generated constructors take every nullable parameter explicitly. Pass: `BlockView(…, secondaryTz = null, …, secondaryText = null, …)`; `TripCardView(…, flightNumber = null, …)`; `TripProgress(status = TripStatus.Active, nextStep = null)`; `PlanView(…, meta = PlanMeta(inputHash = hash, arrivalLandingUtc = null, assumedReturn = null), summary = null, …)`; `NoCaffeineParamsView(nextServingUtc = null, exception = null)`. Enum values exist as named (`Lane.Light`, `SleepSetting.Bed`, `SeekSource.Outdoor`, `AvoidSetting.Home`, `PlanStatus.Ok`, `CheckStatus.Done`/`.Skipped`, `TripStatus.Active`/`.Completed`). This is the "one more value" case §6 Task 5 anticipated; no report needed for these.

**R5. Lessons from 9f–11b the worker carries.**
- **Distinct iOS rules stay distinct.** Each iOS rule is its own step and its own assertion: lookback replay, ledger suppression, check-mark suppression, the if-you-can rule, the main-sleep rule, the margin, the window, the daily cap and the global 60 are separate code paths; never fold two into one predicate because the vectors happen to pass.
- **One clock, injected.** `now` is always a parameter; the tests' `now` is `reminderNow` or a vector value, never `Instant.EPOCH`/`Instant.now()`. There is no coroutine code in 12a, so no test scheduler (if a test ever needs one, use a single `TestCoroutineScheduler`; none should).
- **DST and zones.** Local days come from `ZoneRules.getOffset(instant)` per instant (`elapsedDailyCapAndDST` crosses the 2026-10-25 Amsterdam change; the edge test covers New York's 2026-11-01 change). Never derive a day from a UTC date or the device zone.
- **Tests can fail for the real reason.** `outputIgnoresDefaultLocaleAndZone` must use a zone far from UTC (Kiritimati, +14) so a default-zone bug moves a local day; use `Locale.forLanguageTag("ar")` (not the deprecated `Locale(String)` constructor: `allWarningsAsErrors` is on; precedent `core/plan/…/LocaleIndependenceTest.kt` l.21). The parity test compares whole draft lists and collects every mismatch.
- **Every commit leaves tests green.** A commit that breaks an existing test updates it in the same commit (Task 1: the copier change, `selection.json`, its manifest line and `test_copy_fixtures.py` together; Task 5: `:core:testing`'s new dependency with `FakeReminderStrings`). Spotless and Konsist pass in the same commit as the code they check. `:core:reminders` has no debug source set; nothing debug-only is imported anywhere.
- **Docs claim only what a test proves.** The 110-vector parity is the only parity claim; the `[D]` goldens are hand-derived, not iOS-run; 12a claims nothing about alarms, delivery or devices.
- **No copy assertions on screens** (there is no screen). The body strings in `ReminderScheduleTests`/`ReminderRequestTests` are engine output compared with `FakeReminderStrings`, and Task 6 pins that fake to the generated resources: this is the generator-parity layer AGENTS.md allows, not a screen assertion.

**R6. Perf.** 12a is JVM-only and touches neither the launch path nor the timeline: **`make perf`, `make baseline-profile` and `make perf-timeline` do not apply**; no `docs/perf-*.md` entry, no budget row (§7 item 8 holds). The only budget is AGENTS.md's < 1 s per JVM test (the parity test's decode stays in a companion `lazy`).

**R7. Genuine gaps the worker must handle (raise as OQ-A567+ only if the default does not work).**
- `"$hours"` in `reminderBlock`'s id: Kotlin `Double.toString` and Swift `\(Double)` agree for every value the ported tests use (shortest round-trip on the JDK 21 test runtime, no exponent between 1e-3 and 1e7); only `"seek_light-1.0"` is asserted (`requestCarriesTripBlockHashAccountAndExpiry`). If another id is asserted, build it by hand.
- iOS formats `member.blockStartUtc.ISO8601Format()` (seconds, no fraction) before `formatInstant`; Android passes `Instant.toString()`. Only `HH:mm` reaches the body, so the output is identical; do not add a formatter for it.
- The preflight grep in §4 item 2 no longer uses `\b` (macOS `grep` treats it inconsistently); it greps `class TripDetail(`.

Tags: **[F]** fact verified in source (path:line). **[R]** decision for Android made by this brief. **[D]** value derived by hand from the iOS algorithm (not run on iOS).

---

## 1. Goal

Port the platform-neutral half of iOS `Packages/ChronoFlowKit/Sources/Reminders` into the pure-JVM module `:core:reminders`, so that Phase 12b only has to turn a `List<ReminderRequest>` into alarms:

1. **Account-level switches:** `ReminderSettings` (four kind flags + lead time, the master switch `settingEnabled`), `ReminderPermission` and `remindersActive`, strict decoding of the stored `reminders` settings section, the account 12/24 h format, and the outbox patches the 12c page will enqueue.
2. **Selection** (`reminderDrafts`): kinds, eligibility, lead time, sleep deferral, 20 min expiry, 15 min bounded coalescing, core-first cap of 6 per local day. Byte-for-byte parity with the 110 RN vectors that iOS runs.
3. **Scheduling** (`reminderRequests`): 36 h lookback replay, ledger suppression, 5 s margin, 48 h window, earliest 60 requests, title, en/nl body, stable identifier.
4. **Ledger** (`ReminderLedger`): entry type, tolerant decode, 36 h `elapsed` filter.

No Android API, no database, no alarms, no UI. Everything is a pure function of its inputs; `now` is always a parameter.

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` l.3428–3458 (Phase 12a), A3 l.90 (module rules), A5 l.92 (reminder tables), A13 l.100 (reminder values), l.1637 (Konsist time rule covers `:core:reminders`; rule `noDefaultLocaleZoneOrClockInLogic`, `konsist/…/KonsistRules.kt` l.291/356) | Scope |
| Agent rules | `AGENTS.md` ("Working rules", "Tests", "Git", "Traps") | How to work |
| Research | `docs/research/07-day-to-day-settings.md` §7.1 (l.452–486), §7.2 (l.487–504) | Product rules, Android engine shape |
| Research | `docs/research/09-rn-android-lessons.md` §9 (l.374–386) | RN numbers and kinds |
| 1a brief | `docs/phases/phase-1a-plan-core.md` §4–§6 (Tasks 1, 2, 3, 5, 6) | Exact Kotlin spellings you call |
| 0d brief | `docs/phases/phase-0d-contracts.md` §5.4.2 (enum shape, l.318), l.202–244 (`copy_fixtures.py`, `Fixtures`) | Contracts, fixture copier |
| 0c brief | `docs/phases/phase-0c-strings.md` T1 (lock key `"<prefix>\t<English key>"`, l.230), T4 (`CopyIndex`) | Where the reminder copy lives |
| 4a brief | `docs/phases/phase-4a-store.md` l.241–262 (`scheduled_reminder`, `reminder_ledger` DDL; merged as `core/database/src/main/sqldelight/net/chronoflow/core/database/{ScheduledReminder,ReminderLedger}.sq`) | Shapes 12b will persist; do not touch here |

### 2.2 iOS (read-only; `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

| File | Lines | Port to |
|---|---|---|
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderSelection.swift` | 1–149 (`ReminderSettings` 5–24, `ReminderPermission` 26–29, `ReminderKind` 31–36, `ReminderMember` 38–48, `ReminderDraft` 50–57, `ReminderSelectionOptions` 59–67, `reminderLocalDay` 69–74, `reminderDrafts` 76–149) | `ReminderSettings.kt`, `ReminderSelection.kt` |
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderSchedule.swift` | 1–123 (`ReminderPlan` 6–15, `ReminderRequest` 17–27, `ReminderStrings` 38–50, `reminderRequests` 52–123). **Not** `ReminderNotificationCenter` (29–36): 12b | `ReminderSchedule.kt`, `ReminderStrings.kt` |
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderLedger.swift` | 1–34 (`read`/`write`/`clear` 35–45 are 12b) | `ReminderLedger.kt` |
| `Packages/ChronoFlowKit/Sources/Reminders/Resources/Reminders.xcstrings` | the four sentence keys | §5.5 |
| `Packages/ChronoFlowKit/Sources/Settings/RemindersModel.swift` | 21, 62–84 (patch shapes, lead options) | `ReminderSettings.kt` patches |
| `Packages/ChronoFlowKit/Sources/Reminders/ReminderService.swift` | 110–121 (settings decode, `timeFormat == "12"`) | `ReminderSettings.kt` helpers. The rest of the file is 12b. |
| `Packages/ChronoFlowKit/Tests/RemindersTests/SelectionParityTests.swift` | 1–81 | `ReminderSelectionTests.kt` |
| `Packages/ChronoFlowKit/Tests/RemindersTests/ScheduleTests.swift` | 1–148 | `ReminderScheduleTests.kt` |
| `Packages/ChronoFlowKit/Tests/RemindersTests/ReminderTestSupport.swift` | 1–53 (not `FakeReminderCenter`/`ReminderTestStore`, 55–131: 12b) | `ReminderTestSupport.kt` |
| `Packages/ChronoFlowKit/Tests/RemindersTests/PermissionTests.swift` | `permissionMatrix` 18–24 only | `ReminderPermissionTests.kt` |
| `Packages/ChronoFlowKit/Tests/RemindersTests/Fixtures/selection.json` | 110 cases | Task 1 |
| `Tools/ReminderParity/export-selection.cjs` | 1–35 | Re-pin note (Task 1) |
| `Docs/phase-19-reminders.md` | l.5, l.7, l.25 | Behaviour summary |
| `plan.md` | l.2212 "Phase 19: Local reminders (`feat/phase-19-reminders`)", 19b l.2217 | The decision: "keep 48 h / 60 requests, 15 min coalescing / 6 daily / 20 min expiry, 5 s margin, 36 h replay lookback" |

### 2.3 Web (pin `e36368ec`; read-only, for the settings shape only)

- `app/Data/Settings/ReminderSettingsView.php`: `{light: bool, avoid: bool, caffeine: bool, winddown: bool, leadMin: int}` **[F]**: this is the stored `settings.reminders` object (camelCase).
- `app/Http/Requests/Settings/PreferenceRules.php:22–26`: flags `boolean`, `reminder_lead_min` `integer` in `[15, 30, 60]` **[F]**.
- `routes/api.php:70` `PATCH v1/account/reminders` (prefix l.42), body `{light?, avoid?, caffeine?, winddown?, lead_min?}` → 200 account snapshot **[F]**. This phase builds only the patch objects; sending is 4d/12c.

### 2.4 Boards

None. This phase has no UI. `SetReminders`, `SetRemindersAsk`, `SetRemindersBlocked`, `DarkSetReminders`, `WizardReminders`, `WizardRemindersDarkNL` belong to 12c; do not read or build them here.

---

## 3. Out of scope (owner in brackets)

- `ReminderNotificationCenter`, `SystemReminderCenter`, `ReminderService` (serialization, generation counter, replace/remove diff, ledger read/write/clear), `ReminderInputs` (DB fetch and the trip filter `keepOffline`/`deletedAt`/`deletedLocally`/current version/done-skipped marks), AlarmManager, the channel, receivers, WorkManager, `scheduled_reminder`/`reminder_ledger` queries, request codes, taps (`ReminderTap`), teardown [12b].
- The Android implementation of `ReminderStrings` (resources through `CopyIndex`) [12b].
- `RemindersModel` (busy state, permission refresh, enqueueing), the page, the first-trip offer (`FirstTripRemindersOffer`), `notificationPermissionRequested` [12c].
- Any new string resource or `android-only.json` entry. This phase adds no copy.
- Editing the iOS repo or re-running the RN exporter (§6 Task 1 only records the command).

---

## 4. Pre-flight checks (stop and report if one fails)

Run from the repo root:

1. `grep -rn "fun planDate\|fun checkIdentity\|fun buildCheckState\|fun withoutOptionalSteps\|fun zoneLabel\|class CopyLineFormatter\|class CopyFormat\|fun formatInstant" core/plan/src/main` finds all eight (1a; `formatInstant` matches twice: use the public `String` overload, l.139; the `Instant` one is `internal`). Use the merged signatures; if one differs from this brief, keep the merged one and say so in your report.
2. `grep -rn "class BlockView\|class CheckKeyView\|class TripCardView\|class TripDetail(\|class TimelinePresentationView\|class BlockReasonTagsView\|sealed interface BlockParams\|class SleepParamsView" core/model/src/main` finds each (0d). Read `BlockView`'s constructor: the parity decoder builds it directly.
3. `core/reminders/build.gradle.kts` exists with plugin `chronoflow.jvm.library`, namespace/package `net.chronoflow.core.reminders` (0a §table l.122). `JVM_MODULES` in the `Makefile` (l.11) lists `core:reminders` (0a brief l.649).
4. `core/ui/src/main/res/values/strings_ios.xml`, `…/values-nl/strings_ios.xml` and `tools/l10n/names.lock.json` exist (0c).
5. `tools/contracts-gen/copy_fixtures.py` and `core/testing/src/main/resources/fixtures/fixtures.sha256` exist (0d).

---

## 5. Module, package and shared rules

- **Module** `:core:reminders`, package `net.chronoflow.core.reminders`. Main: `core/reminders/src/main/kotlin/net/chronoflow/core/reminders/`. Tests: `core/reminders/src/test/kotlin/net/chronoflow/core/reminders/`.
- **Dependencies** (`core/reminders/build.gradle.kts`): `api(project(":core:model"))`, `api(project(":core:plan"))` (public signatures expose `TripDetail`, `CopyLineFormatter`), `implementation(libs.kotlinx.serialization.json)`. Tests: `testImplementation(project(":core:testing"))`, JUnit 4, assertk. Apply the kotlinx-serialization compiler plugin to this module with `alias(libs.plugins.kotlin.serialization)`: the convention plugin does not (test-local `@Serializable` classes need it; *[Refresh R2]*).
- `:core:testing` gets `implementation(project(":core:reminders"))` for `FakeReminderStrings` (Task 5). No cycle: `:core:reminders` main does not depend on `:core:testing`.
- **Names [R]:** Swift types and free functions keep their names (`ReminderSettings`, `ReminderPermission`, `ReminderKind`, `ReminderMember`, `ReminderDraft`, `ReminderSelectionOptions`, `ReminderPlan`, `ReminderRequest`, `ReminderStrings`, `ReminderLedgerEntry`, `ReminderLedger`, `reminderLocalDay`, `reminderDrafts`, `reminderRequests`). Swift property names are kept too, including `tripID`, `blockID`, `accountID`, `sendAtMs`. Swift `Date` → `java.time.Instant`.
- **Enum spellings** are 0d's: `BlockType.SeekLight`, `.AvoidLight`, `.NoCaffeine`, `.Sleep`; `Priority.Core`, `.Recommended`, `.Optional`, `.Info`; `SleepKind.Main`; `TripStatus.Completed`; `Language.En`/`.Nl`; `TimeFormat.Value12`/`.Value24`; `.raw` for the wire string; `X.of(raw)`.
- **Time hygiene:** the 1a Konsist rule already covers `core/reminders` main (`Locale.getDefault`, `TimeZone.getDefault`, `ZoneId.systemDefault`, `System.currentTimeMillis`, `Instant.now(`, `String.format(`, …). Never read a clock, default zone or default locale. Never use `String.format`/`"…".format(` (pad digits by hand).
- **Android API level [R]:** this code runs on minSdk 29. Use only Java 8 `java.time`/`java.util` APIs: `ZoneId.of`, `ZoneRules.getOffset(Instant)`, `LocalDateTime.ofEpochSecond`, `Instant.plusSeconds`, `java.util.Base64`. **Not** `LocalDate.ofInstant` (Java 9, Android API 34) or other Java 9+ overloads.
- **No static state:** no top-level `val` that computes at class load, no `object` holding mutable state. `const val` and private constant tables are fine.
- **Exceptions:** catch by type (`DateTimeException`, `SerializationException`, `IllegalArgumentException`); never `catch (e: Exception)` or `runCatching`.

### 5.1 Constants (`ReminderSchedule.kt`, public `const val`) **[F]** `ReminderSchedule.swift:65,71,74,82,122`, `ReminderLedger.swift:33`, `ReminderSelection.swift:60–62`

| Name | Value | Source |
|---|---|---|
| `REMINDER_LOOKBACK_SECONDS` | `129_600L` (36 h) | `ReminderSchedule.swift:65`, `ReminderLedger.swift:33` |
| `REMINDER_MARGIN_SECONDS` | `5L` | `ReminderSchedule.swift:71,74,82` |
| `REMINDER_WINDOW_SECONDS` | `172_800L` (48 h) | `ReminderSchedule.swift:82` |
| `REMINDER_MAX_REQUESTS` | `60` | `ReminderSchedule.swift:122` |
| `ReminderSelectionOptions` defaults | `coalesceMinutes = 15`, `dailyCap = 6`, `expiryGraceMinutes = 20` | `ReminderSelection.swift:60–62` |
| Lead options | `15, 30, 60` (default `30`) | `RemindersModel.swift:63`, `ReminderSelection.swift:12` |

12b reads these constants; do not duplicate the numbers elsewhere.

---

## 6. Tasks

Each task is one commit on `feat/phase-12a-reminders-engine`, Conventional Commits, lower-case imperative subject, **no trailer**. Tick the plan.md checkbox named per task in the same commit. Run `make targeted ONLY=…` for what you touched; run `make quick` once before handing back.

### Task 1. Parity fixture

Commit: `test(reminders): copy the rn selection parity vectors from ios`. Ticks nothing (the "**Parity.**" box is ticked in Task 3).

**Files:** `tools/contracts-gen/copy_fixtures.py`, `tools/contracts-gen/test_copy_fixtures.py`, `core/testing/src/main/resources/fixtures/reminders/selection.json` (generated), `core/testing/src/main/resources/fixtures/fixtures.sha256` (generated line), `core/testing/src/main/resources/fixtures/SOURCE.md` (one paragraph appended).

**Behaviour**
- 0d's `SOURCES` rows are either a sub-directory (written to `fixtures/<sub>/`) or one `.json` file (written to the fixtures root). Neither writes `fixtures/reminders/selection.json` from `…/RemindersTests/Fixtures/selection.json`. **[R]** Add an optional third element to a row, the destination sub-directory: `("Packages/ChronoFlowKit/Tests/RemindersTests/Fixtures", ["selection.json"], "reminders")` writes `fixtures/reminders/selection.json`. Rows without the third element behave exactly as before. The manifest line's `ios-source` column is the iOS path `Packages/ChronoFlowKit/Tests/RemindersTests/Fixtures/selection.json`. A destination sub-directory counts as one of the script's own directories for 0d's `--check` orphan rule (a `*.json` under `fixtures/reminders/` that no row produces fails `--check`).
- Run `make contracts`. The output must be byte-identical to iOS at the pin: **[F]** 1,046,865 B, SHA-256 `0ac14ed1f2106fdf12ca7498716d4fb4c3b8c6d60b325792ed28c62c72ad3b2f`. Do not copy it by hand and never reformat it.
- `SOURCE.md` paragraph (re-pin record): the file is produced on the iOS side by `node Tools/ReminderParity/export-selection.cjs /Users/rickgout/Sites/chronoflow-mobile` run from the iOS repo root (it writes `Packages/ChronoFlowKit/Tests/RemindersTests/Fixtures/selection.json`; RN source `src/reminders/__tests__/selection.test.ts` at `e24872afd47ff671fbbab315665920f2805054df`, recorded in the file's `source`/`commit` keys). Android never runs the exporter: a re-pin moves `tools/ios-pin.txt` and reruns `make contracts`.

**Tests**
- `test_copy_fixtures.py`: add `test_row_with_destination_subdirectory` (a fake `read` returns bytes for `X/Fixtures/selection.json`; the row `("X/Fixtures", ["selection.json"], "reminders")` writes `fixtures/reminders/selection.json` with unchanged bytes and one manifest line; `--check` fails when that file changes by one byte). Existing row kinds keep passing.
- 0d's `FixtureManifestTest` covers the new manifest line; if it asserts per-directory counts, add `reminders/` = 1.

### Task 2. Settings, permission and kinds

Commit: `feat(reminders): port reminder settings, permission and kinds`. Ticks nothing yet.

**Files:** `core/reminders/…/ReminderSettings.kt`, `core/reminders/build.gradle.kts` (the §5 dependencies, first needed here); tests `ReminderPermissionTests.kt`, `ReminderSettingsTests.kt`.

**Behaviour** (port of `ReminderSelection.swift:5–36`, plus helpers from `ReminderService.swift:110–120` and `RemindersModel.swift:62–84`)
- `data class ReminderSettings(val light: Boolean = true, val avoid: Boolean = true, val caffeine: Boolean = true, val winddown: Boolean = true, val leadMin: Int = 30)`.
  - `val enabled: Boolean get() = light || avoid || caffeine || winddown`.
  - `fun settingEnabled(enabled: Boolean): ReminderSettings` = all four flags set to `enabled`, `leadMin` kept. This is the master switch.
  - In `companion object` (called as `ReminderSettings.isLeadOption(…)`, `ReminderSettings.decode(…)`, `ReminderSettings.enabledPatch(…)`, `ReminderSettings.leadPatch(…)`):
  - `fun isLeadOption(minutes: Int): Boolean = minutes == 15 || minutes == 30 || minutes == 60` **[R]** (a function, not a `List` built at class load).
  - `fun decode(json: String?): ReminderSettings?` **[R]** (Swift `try? $0.value.decode(ReminderSettings.self)`, synthesized `Decodable`, which ignores default values for missing keys): parse with `ApiJson.response.parseToJsonElement`; return `null` when `json` is `null`, malformed, not an object, any of the five keys is missing, a flag is not a JSON boolean (`JsonPrimitive`, `!isString`, content `true`/`false`), or `leadMin` is not a JSON integer that fits `Int` (`"30"` string → `null`; `30.5` → `null`; `30` → `30`). Extra keys are ignored. Any integer `leadMin` is accepted, including `45` (iOS parity; the server only stores 15/30/60).
  - Patches (the bodies 12c enqueues as `OutboxChange.Settings(SettingsSection.Reminders, patch)`), companion functions. Key order **[R]**: iOS builds a Swift dictionary and its outbox encodes with `.sortedKeys` (`LocalStore/LocalMutations.swift:10–14`), and 4d's `OutboxPayload` sorts keys too, so order never reaches the wire; build the objects in the order below so the test strings are deterministic:
    - `fun enabledPatch(enabled: Boolean): JsonObject` = `{"light":e,"avoid":e,"caffeine":e,"winddown":e}` (`RemindersModel.swift:76–80`).
    - `fun leadPatch(minutes: Int): JsonObject?` = `{"lead_min":minutes}` when `isLeadOption(minutes)`, else `null` (`RemindersModel.swift:63,68`).
- `fun reminderTimeFormat(preferencesJson: String?): TimeFormat` **[R]** (`ReminderService.swift:117–120`): `TimeFormat.Value12` only when the stored `preferences` section is a JSON object whose `timeFormat` is the JSON **string** `"12"`; everything else (missing, `null`, malformed, number `12`, `"24"`, anything) → `TimeFormat.Value24`.
- `enum class ReminderPermission { Undetermined, Granted, Blocked, Unsupported }` (Swift `undetermined, granted, blocked, unsupported`; case order kept).
  - `fun remindersActive(settings: ReminderSettings?): Boolean = this == Granted && settings?.enabled == true`.
  - This is the only permission enum. 12b maps Android state to it (OQ-12a-7).
- `enum class ReminderKind(val raw: String) { SeekLight("seek_light"), AvoidLight("avoid_light"), CaffeineCutoff("caffeine_cutoff"), WindDown("wind_down") }` with `companion object { fun of(raw: String): ReminderKind? }` (null for unknown; only tests decode it). Task 5 adds the second constructor parameter `copyKey`.

**Tests**
- `ReminderPermissionTests.permissionMatrix` (port of `PermissionTests.swift:18–24`): for every `ReminderPermission`: `remindersActive(ReminderSettings()) == (it == Granted)`; `!remindersActive(null)`; `!remindersActive(ReminderSettings().settingEnabled(false))`.
- `ReminderSettingsTests` (Android-only):
  - `defaultsAndMasterSwitch`: defaults are all true and 30; `settingEnabled(false)` has four false flags and `leadMin == 30`; `.enabled` is true when only `winddown` is true.
  - `decodeIsStrict`: `{"light":true,"avoid":false,"caffeine":true,"winddown":true,"leadMin":15}` → those values; with an extra key `"x":1` → same; missing `leadMin` → `null`; `"light":"true"` → `null`; `"leadMin":"30"` → `null`; `"leadMin":30.5` → `null`; `"leadMin":45` → `45`; `"{"`, `"[]"`, `null` → `null`.
  - `timeFormatFromPreferences`: `{"timeFormat":"12"}` → `Value12`; `{"timeFormat":"24"}`, `{"timeFormat":12}`, `{}`, `"{"`, `null` → `Value24`.
  - `patchesAreExact`: `ApiJson.request.encodeToString(JsonObject.serializer(), enabledPatch(true))` == `{"light":true,"avoid":true,"caffeine":true,"winddown":true}`; `leadPatch(60)` → `{"lead_min":60}`; `leadPatch(45)` and `leadPatch(0)` → `null`. (`ApiJson.request` exists; *[Refresh R2]*.)

### Task 3. Selection (`reminderDrafts`) and the 110 vectors

Commit: `feat(reminders): port reminder selection with rn parity vectors`. Ticks plan.md Phase 12a "**Parity.**".

**Files:** `core/reminders/…/ReminderSelection.kt`; tests `ReminderSelectionTests.kt`, `ReminderSelectionEdgeTests.kt`.

**Types**
- `data class ReminderMember(val kind: ReminderKind, val blockType: BlockType, val localDate: String, val ordinal: Int, val sendAt: Instant, val blockStartUtc: Instant, val displayTz: String, val expiresAt: Instant)` with `val identity: String get() = checkIdentity(CheckKeyView(type = blockType, localDate = localDate, ordinal = ordinal))` (`"seek_light|2026-10-14|1"`).
- `data class ReminderDraft(val members: List<ReminderMember>, val sendAt: Instant, val expiresAt: Instant, val blockStartUtc: Instant, val displayTz: String, val priority: Priority)`.
- `data class ReminderSelectionOptions(val coalesceMinutes: Int = 15, val dailyCap: Int = 6, val expiryGraceMinutes: Int = 20, val sentMemberKeys: Set<String> = emptySet(), val sentCountsByDay: Map<String, Int> = emptyMap(), val timeline: TimelinePresentationView? = null)`.

**`fun reminderLocalDay(date: Instant, zone: String): String`** (`ReminderSelection.swift:69–74`): `YYYY-MM-DD` of `date` in `zone`; an id that `ZoneId.of` rejects (`DateTimeException`) → UTC (Swift `?? .gmt`). Compute with `LocalDateTime.ofEpochSecond(date.epochSecond, 0, ZoneId.of(zone).rules.getOffset(date))`; pad year to 4 and month/day to 2 digits by hand.

**`fun reminderDrafts(blocks: List<BlockView>, settings: ReminderSettings, now: Instant, options: ReminderSelectionOptions = ReminderSelectionOptions()): List<ReminderDraft>`** — port `ReminderSelection.swift:76–149` step by step:

1. **Fallback keys** (l.79–86). For each block **with `actionable == true`**, in list order: `day = reminderLocalDay(start, block.displayTz)`; group `"${block.type.raw}|$day"`; increment that group's counter; `keys[block.id] = CheckKeyView(block.type, day, counter)`. Every actionable block counts, including ones later excluded (optional, info, other types, disabled kinds). A later block with the same `id` overwrites the key.
2. **Sleeps** (l.87): all blocks with `type == BlockType.Sleep` (any kind, priority or actionability), sorted by start instant (stable).
3. **If-you-can set** (l.88–89): ids of `options.timeline?.blockReasons` entries with any line whose `key == "If you can"` (exact string).
4. **Per block** (l.91–126), in list order. Skip unless `keys` has the block's id **and** `priority` is `Priority.Core` or `Priority.Recommended`. Kind:
   - `SeekLight` → `ReminderKind.SeekLight` if `settings.light`;
   - `AvoidLight` → `AvoidLight` if `settings.avoid` and the id is not in the if-you-can set;
   - `NoCaffeine` → `CaffeineCutoff` if `settings.caffeine`;
   - `Sleep` → `WindDown` if `params is BlockParams.Sleep` with `value.kind == SleepKind.Main` and `settings.winddown` (`InFlightMain`, `InFlightRecovery`, naps: none);
   - anything else (including `caffeine_serving`, `nap`, `rest`, `Unknown`) → skip.
   - `start = planDate(startUtc)`, `expires = min(planDate(endUtc), start + expiryGraceMinutes min)`. Skip if `expires <= now`.
   - `send = max(start − settings.leadMin min, now)`. Then for each sleep in sorted order: if `send >= sleep.start && send < sleep.end` then `send = sleep.end` (the condition uses the **updated** `send`, so adjacent sleeps chain; start-inclusive, end-exclusive).
   - Skip if `send >= expires`.
   - `key = block.checkKey ?: fallback`; skip if `checkIdentity(key) in options.sentMemberKeys`.
   - Draft with one member `ReminderMember(kind, key.type, key.localDate, key.ordinal, send, start, block.displayTz, expires)`; draft `sendAt = send`, `expiresAt = expires`, `blockStartUtc = start`, `displayTz = block.displayTz`, `priority = block.priority`.
   - Note the member's `blockType`/`localDate`/`ordinal` come from the **check key** (the server key may name another date than the start; vector "preserves check keys assigned before caffeine start adjustments").
5. **Sort** (l.127) by `sendAt`, ties by `blockStartUtc`; use a stable sort (`sortedWith`).
6. **Coalesce** (l.128–140), bounded: walk the sorted drafts; if a group exists and `draft.sendAt <= group.sendAt + coalesceMinutes min` (the group's **first** send, never moved), append the draft's members, set `expiresAt = max(group.expiresAt, draft.expiresAt)`, and set `priority = if (group.priority == Core) Core else draft.priority`. Group `sendAt`, `blockStartUtc`, `displayTz` stay the first draft's. Otherwise start a new group.
7. **Order for the cap** (l.141): stable sort: when priorities are equal, earlier `blockStartUtc` first; otherwise `Core` first. (Swift comparator `a.priority == b.priority ? a.start < b.start : a.priority == .core`.)
8. **Cap** (l.142–147): `counts` starts as a copy of `options.sentCountsByDay`; for each group, `day = reminderLocalDay(group.blockStartUtc, group.displayTz)`; drop it if `counts[day] ?: 0 >= dailyCap`, else increment and keep.
9. **Return** (l.148) the kept groups stably sorted by `sendAt`.

**Unparseable instants [R] (OQ-12a-4):** iOS turns them into NaN dates with undefined results. Android: a block whose `startUtc` or `endUtc` fails `planDate` is skipped in step 4 and gets no fallback key in step 1 (it does not advance the ordinal counter); a sleep with an unparseable bound is left out of step 2. Plan data never contains one.

**Performance [R]:** parse each block's two instants once per call (an internal array of `(block, start, end)`), and resolve each distinct zone id once per call (a local `HashMap<String, ZoneRules>`; no global cache). No regex, no `String.format`, no per-member `ZoneId.of`.

**Parity test `ReminderSelectionTests.allRNSelectionVectors`** (port of `SelectionParityTests.swift:29–56`; Swift suite and method names kept, OQ-12a-1):
- Load `Fixtures.text("reminders/selection.json")` once (companion `lazy`). Top level `{source, commit, cases}`; assert `cases.size == 110`.
- Decode each case as a `JsonObject`:
  - `blocks`: each through a test-local `ParityBlock` decoder (port of l.59–80): read `id`, `lane` (`Lane.of`), `type` (`BlockType.of`), `startUtc`, `endUtc`, `displayTz`, `priority` (`Priority.of`), `text` (`CopyLine` through `ApiJson.response`), `params` (raw `JsonElement`), `actionable`, `checkKey` (`CheckKeyView?`, `null` allowed; 423 blocks carry one). Do **not** read `secondaryTz`/`secondaryText`: iOS `ParityBlock` does not decode them (they stay `nil`), and the fixture has 54 non-null `secondaryTz` and 179 non-null `secondaryText` values that selection never reads; pass `null` for both. Build `BlockView` directly with `params = if (type == BlockType.Sleep) BlockParams.Sleep(ApiJson.response.decodeFromJsonElement(SleepParamsView.serializer(), params)) else BlockParams.Unknown(params)`. **Do not** call 0d's `BlockParams.decode`: RN's synthetic non-sleep params are incomplete and production decoding would throw (iOS l.72 comment).
  - Block `startUtc`/`endUtc` are 17 characters (`"2026-10-14T10:00Z"`); 1a's `planDate` accepts them.
  - `settings` → `ReminderSettings.decode(element.toString())!!` (all five keys present; `leadMin` is 15, 30 or 60).
  - `now` → `planDate(…)!!` (24 characters, `.000Z`).
  - `options`: `coalesceMinutes ?: 15`, `dailyCap ?: 6`, `expiryGraceMinutes ?: 20`; `sentMemberKeys` is a JSON **object** `{identity: Boolean}` → the set of keys whose value is `true`; `sentCountsByDay` `{day: Int}`; `timeline` → read only its `blockReasons` array (each a `BlockReasonTagsView` `{blockId, lines: [CopyLine]}` through `ApiJson.response`) → `TimelinePresentationView(days = emptyList(), blockReasons = …, transferWarnings = emptyList())` (use the generated constructor's parameter names). 6 of the 9 timelines also carry `days` and `transferWarnings`; ignore them, as iOS `Options.Timeline` decodes only `blockReasons` (`SelectionParityTests.swift:24`). Keys present in the fixture **[F]**: `sentCountsByDay` 13 cases, `timeline` 9, `sentMemberKeys` 4, `dailyCap` 2.
  - `expected`: a list of drafts; map each to `ReminderDraft` (`kind` → `ReminderKind.of(…)!!`, `blockType` → `BlockType.of`, dates → `planDate(…)!!`, `priority` → `Priority.of`).
- For each case: `reminderDrafts(blocks, settings, now, options) == expected` (data-class equality over every member, date and priority). Collect mismatches as `"<test>: expected <n> drafts, got <m>"` or the first differing field into one list and assert it is empty, so one run shows every failure. Case names repeat (53 distinct names over 110 cases); prefix each with its index.
- Fixture facts **[F]**: 872 blocks; 242 expected drafts with 280 members; 57 cases expect an empty list; zones `UTC`, `Europe/Amsterdam`, `Europe/London`, `America/New_York`, `America/Los_Angeles`, `Asia/Tokyo`, `Asia/Singapore`, `Asia/Dubai`, `Australia/Sydney`; `now` values include `2026-10-14T09:30:01.000Z`, `…10:19:59.000Z`, `…10:20:01.000Z` and `2020-01-01T00:00:00.000Z`.
- Budget: decode plus 110 evaluations under 1 s warm. If the first run is slower because of class loading, keep the decode in the companion `lazy`.

**`ReminderSelectionEdgeTests`** (Android-only):
- `localDayUsesTheZoneAndFallsBackToUtc`: `reminderLocalDay(planDate("2026-10-14T15:30Z")!!, "Asia/Tokyo") == "2026-10-15"`; `("2026-11-01T03:30Z", "America/New_York") == "2026-10-31"`; `("2026-10-25T22:30Z", "Europe/Amsterdam") == "2026-10-25"`; `("2026-10-14T23:30Z", "Not/AZone") == "2026-10-14"`; `("1969-12-31T23:30:00Z", "Asia/Tokyo") == "1970-01-01"`.
- `unparseableBlocksAreIgnored`: a core `seek_light` with `startUtc = "garbage"` yields no draft and does not shift the fallback ordinal of the next `seek_light` that day (its member ordinal is 1).
- `outputIgnoresDefaultLocaleAndZone`: run the first 10 parity vectors with `Locale.setDefault(Locale.forLanguageTag("ar"))` (*[Refresh R5]*) and `TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))`, restoring both in `finally`; results equal the expected drafts.

### Task 4. Ledger

Commit: `feat(reminders): port the reminder ledger`. Ticks nothing yet.

**Files:** `core/reminders/…/ReminderLedger.kt`; test `ReminderScheduleTests.kt` (created here with the one ledger method; Task 5 adds the rest).

**Behaviour** (port of `ReminderLedger.swift:5–34`)
- `data class ReminderLedgerEntry(val tripId: String, val inputHash: String, val sendAtMs: Double, val memberKeys: List<String>)`. `sendAtMs` stays a `Double` like Swift (OQ-12a-5).
- `object ReminderLedger` (stateless; functions only):
  - `fun decode(data: String): List<ReminderLedgerEntry>`: parse with `ApiJson.response.parseToJsonElement`; malformed JSON (`SerializationException`) or a non-array top level → `emptyList()`. Each element is kept only when it is an object with `tripId` (JSON string), `inputHash` (JSON string), `sendAtMs` (a JSON number: a non-string primitive that is not `true`/`false`/`null`, whose `doubleOrNull` is finite), and `memberKeys` (an array whose every element is a JSON string). Anything else drops that element only.
  - `fun elapsed(rows: List<ReminderLedgerEntry>, now: Instant): List<ReminderLedgerEntry>` keeps rows with `nowMs − 129_600_000 <= sendAtMs <= nowMs`, where `nowMs = now.epochSecond * 1000.0 + now.nano / 1_000_000.0` (both bounds inclusive; order kept).
  - `read`/`write`/`clear` are not ported (12b, tables).
- `ReminderLedgerEntry.Companion.from(request: ReminderRequest)` is added in Task 5.

**Test** `ReminderScheduleTests.ledgerValidationAndExpiry` (port of `ScheduleTests.swift:131–146`): `"{"`, `"{}"`, `"[null, {}, {\"memberKeys\": [1]}]"` decode to empty; a valid entry (`tripId "trip"`, `inputHash "hash"`, `sendAtMs = reminderNow` in ms, `memberKeys ["seek_light|2026-10-14|1"]`) written as JSON by hand decodes to itself; `elapsed([valid], reminderNow) == [valid]`; `elapsed` at `reminderNow + 36 h + 1 s` and at `reminderNow − 1 s` is empty; `sendAtMs: true` decodes to empty.

### Task 5. Scheduling (`reminderRequests`), copy seam and identifiers

Commit: `feat(reminders): port reminder scheduling, copy and stable identifiers`. Ticks plan.md Phase 12a "**Engine**".

**Files:** `core/reminders/…/ReminderSchedule.kt` (constants §5.1, `ReminderPlan`, `ReminderRequest`, `reminderRequests`), `core/reminders/…/ReminderStrings.kt`, `core/reminders/…/ReminderLedger.kt` (`from`), `core/testing/src/main/kotlin/net/chronoflow/core/testing/reminders/FakeReminderStrings.kt`, `core/testing/build.gradle.kts` (`implementation(project(":core:reminders"))`, §5); tests `ReminderTestSupport.kt`, `ReminderScheduleTests.kt`, `ReminderRequestTests.kt`.

**Types**
- `data class ReminderPlan(val card: TripCardView, val detail: TripDetail, val marks: List<CheckMark> = emptyList())`.
- `data class ReminderRequest(val identifier: String, val title: String, val body: String, val sendAt: Instant, val tripID: String, val blockID: String, val inputHash: String, val memberKeys: List<String>, val accountID: String? = null, val expiresAt: Instant)`. `expiresAt` is Android-only (the group's `expiresAt`), for 12b's `setTimeoutAfter` (OQ-12a-3); it is not part of the identifier.
- `fun ReminderLedgerEntry.Companion.from(request: ReminderRequest) = ReminderLedgerEntry(request.tripID, request.inputHash, sendAtMs = epoch ms of sendAt as Double, request.memberKeys)` (the entry iOS appends per request, `ReminderService.swift:134–137`). Give `ReminderLedgerEntry` an empty `companion object`.

**Copy seam [R]** (OQ-12a-2). `:core:reminders` cannot read Android resources. Port Swift `ReminderStrings` as:
```kotlin
fun interface ReminderStrings {
    /** The catalog value of [key] in [language] with `{{time}}` replaced by [time]; the key itself when missing. */
    fun text(key: String, language: Language, time: String): String
}
```
- The four keys are public constants on `ReminderKind`: change Task 2's enum to `enum class ReminderKind(val raw: String, val copyKey: String)`, exactly **[F]** `ReminderSchedule.swift:92–95`, `Reminders.xcstrings`:

| `ReminderKind` | `copyKey` (= en value) | nl value |
|---|---|---|
| `SeekLight` | `Seek bright light from {{time}}.` | `Zoek vanaf {{time}} fel licht.` |
| `AvoidLight` | `Avoid bright light from {{time}}.` | `Vermijd vanaf {{time}} fel licht.` |
| `CaffeineCutoff` | `Last caffeine before {{time}}.` | `Neem vóór {{time}} je laatste cafeïne.` |
| `WindDown` | `Wind down for bed: sleep from {{time}}.` | `Kom tot rust: slapen vanaf {{time}}.` |

- `FakeReminderStrings` (`:core:testing`, `class FakeReminderStrings : ReminderStrings`): the table above; `Language.Nl` → nl, every other language → en; unknown key → the key with `{{time}}` replaced. 12b's Android implementation (`CopyIndex.find(CopyTable.Reminders, key)` + `getString(id, time)` in a context for `language`) must return the same strings; Task 6 pins the fake to the generated resources.

**`fun reminderRequests(plans: List<ReminderPlan>, accountID: String, settings: ReminderSettings, ledger: List<ReminderLedgerEntry>, now: Instant, language: Language, timeFormat: TimeFormat, formatter: CopyLineFormatter, strings: ReminderStrings): List<ReminderRequest>`** — port `ReminderSchedule.swift:52–123`:

1. `elapsedLedger = ReminderLedger.elapsed(ledger, now)`.
2. For each plan in order, skip it when `card.progress.status == TripStatus.Completed`. Otherwise:
   - `plan = withoutOptionalSteps(detail.plan)`.
   - `options = ReminderSelectionOptions(timeline = detail.timeline, sentMemberKeys = buildCheckState(plan, marks).marks.map { checkIdentity(it.key) }.toSet())` (marks matching no current actionable keyed block drop out here).
   - **Lookback** (l.64–77): `elapsed = reminderDrafts(plan.blocks, settings, now − 36 h, ReminderSelectionOptions(timeline = detail.timeline)).filter { it.sendAt <= now + 5 s }` (default cap and coalescing; no sent keys). For each: `sentCountsByDay[reminderLocalDay(draft.blockStartUtc, draft.displayTz)] += 1`; for each member with `member.sendAt <= now + 5 s`, add `member.identity` to `sentMemberKeys`. A future member of an elapsed group stays selectable.
   - **Ledger** (l.78–80): for each `elapsedLedger` entry with `tripId == card.id && inputHash == plan.meta.inputHash`, add all its `memberKeys`.
   - **Upcoming** (l.81–82): `reminderDrafts(plan.blocks, settings, now, options).filter { now + 5 s < it.sendAt && it.sendAt <= now + 48 h }` (exclusive lower bound, inclusive upper bound).
   - **Body** (l.83–104): members sorted stably by `blockStartUtc`; keep only the first member of each `kind`. For each kept member:
     - `format = CopyFormat(language, timeFormat, displayZone = draft.displayTz, annotateInstantZone = true, trip = detail.trip)`;
     - `time = formatter.formatInstant(member.blockStartUtc.toString(), format, zone = member.displayTz)` (`Instant.toString()` prints `…Z` with 0, 3, 6 or 9 fraction digits; 1a's `planDate` accepts all);
     - if `member.displayTz != draft.displayTz`: `time += " (" + zoneLabel(member.displayTz, detail.trip) + ")"`;
     - `sentence = strings.text(member.kind.copyKey, language, time).trimEnd('.') + "."` (Swift strips `\.+$` and appends one dot, so nl 12 h `… 01:15 a.m.` ends with one dot, not two).
     - Body = sentences joined with `" "`.
   - **Identity** (l.105–107): `keys = draft.members.map { it.identity }` in **draft member order** (merge order, not start order); `identifier = "chronoflow.reminder." + Base64.getEncoder().encodeToString((listOf(accountID, card.id, plan.meta.inputHash) + keys).joinToString("\n").encodeToByteArray())` (standard alphabet with `=` padding, no line breaks, same as Foundation `base64EncodedString()`).
   - **Block id** (l.108–111): `first = draft.members[0]`; the first block of `plan.blocks` whose `checkKey` identity equals `first.identity`, else the first block with `type == first.blockType` and `planDate(startUtc) == first.blockStartUtc`, else `""`.
   - **Title** (l.115–116): `"${card.from.city ?: card.from.iata} → ${card.to.city ?: card.to.iata}"` (U+2192 with one space each side; an empty-string city is kept as empty, only `null` falls back).
   - Request: `ReminderRequest(identifier, title, body, draft.sendAt, card.id, blockID, plan.meta.inputHash, keys, accountID, draft.expiresAt)`.
3. **Return** (l.121–122) all requests stably sorted by `sendAt`, ties by `identifier` (`String.compareTo`), then the first 60.

**Test support** `ReminderTestSupport.kt` (port of `ReminderTestSupport.swift:8–53`):
- `reminderNow = planDate("2026-10-14T00:00:00Z")!!`, `reminderAccount = "01900000-0000-7000-8000-000000000001"`.
- `reminderBlock(hours: Double = 1.0, type: BlockType = BlockType.SeekLight, ordinal: Int = 1, zone: String = "UTC"): BlockView`: `start = reminderNow + Math.round(hours * 3_600_000.0)` ms (round to the millisecond; this makes the 4,999/5,000/5,001 ms margin rows exact); `id = "${type.raw}-$hours"`; `lane = Lane.Light`; `startUtc` = start with **three** fraction digits (`2026-10-14T01:00:00.000Z`, built by hand or with one test-file `DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)`); `endUtc` = `start + 1 h` **without** fraction digits, seconds truncated (`2026-10-14T02:00:00Z`; Swift `ISO8601Format()` at l.26; build it by hand or with a second test-file formatter `"uuuu-MM-dd'T'HH:mm:ss'Z'"`); `displayTz = zone`; `priority = Priority.Recommended`; `text = CopyLine("test", emptyList())`; `actionable = true`; `checkKey = CheckKeyView(type, reminderLocalDay(start, zone), ordinal)`; params: `Sleep` → `BlockParams.Sleep(SleepParamsView(kind = SleepKind.Main, setting = Bed, opportunityMin = 60))`, `SeekLight` → `BlockParams.SeekLight(SeekLightParamsView(source = Outdoor, coreMin = 30))`, `AvoidLight` → `BlockParams.AvoidLight(AvoidLightParamsView(setting = Home, reduced = false))`, `NoCaffeine` → `BlockParams.NoCaffeine(NoCaffeineParamsView())`, else `BlockParams.Unknown(JsonNull)` (use 0d's generated constructor and enum names).
- `reminderPlan(blocks = listOf(reminderBlock()), id = "trip", hash = "hash", marks = emptyList())` (port of l.31–53): `from = AirportRef(iata = "AMS", city = "Amsterdam", zone = "Europe/Amsterdam")`, `to = AirportRef(iata = "HND", city = "Tokyo", zone = "Asia/Tokyo")`;
  - `card = TripCardView(id = id, from = from, to = to, departLocal = "2026-10-14T00:00", arriveLocal = "2026-10-14T14:00", departureUtc = "2026-10-14T00:00:00Z", arrivalUtc = "2026-10-14T05:00:00Z", connections = emptyList(), offsetDifferenceMinutes = 540, progress = TripProgress(status = TripStatus.Active), canComplete = false, canReopen = false, canEdit = false, canDuplicate = false, canDelete = false)`;
  - `detail = TripDetail(trip = TripDetailView(id = id, from = from, to = to, departLocal = card.departLocal, departureUtc = card.departureUtc, endUtc = "2026-10-17T00:00:00Z", offsetDifferenceMin = 540, legs = emptyList()), plan = PlanView(status = PlanStatus.Ok, reasons = emptyList(), prompts = emptyList(), meta = PlanMeta(inputHash = hash), days = emptyList(), blocks = blocks, warnings = emptyList()), notices = PlanNoticesView(days = emptyList(), unassignedWarningIndexes = emptyList()), timeline = TimelinePresentationView(days = emptyList(), blockReasons = emptyList(), transferWarnings = emptyList()), checkState = CheckState(planInputHash = hash, marks = emptyList(), days = emptyList()), detailVersion = "1")`;
  - `ReminderPlan(card, detail, marks)`. Optional generated parameters not named here keep their defaults; if a generated constructor requires one more value, pass its empty/`null` form and say so in the report. *[Refresh R4: the generated constructors have no defaults; pass `flightNumber = null`, `TripProgress(…, nextStep = null)`, `PlanMeta(…, arrivalLandingUtc = null, assumedReturn = null)`, `summary = null`, `secondaryTz = null`, `secondaryText = null`, `NoCaffeineParamsView(nextServingUtc = null, exception = null)`.]*
- `scheduled(plans, now = reminderNow, ledger = emptyList(), settings = ReminderSettings(), language = Language.En, timeFormat = TimeFormat.Value24)`: calls `reminderRequests` with `reminderAccount`, a `CopyLineFormatter` built once per test class from `CopyCatalog(CopyCatalog::class.java.getResourceAsStream("/plan_copy.json")!!.readBytes())` (or 1a's merged loader), and `FakeReminderStrings()`.

**Tests** `ReminderScheduleTests` (ports of `ScheduleTests.swift:20–129`, Swift method names):
- `windowMarginAndCap`: blocks at hours `-1, 0, 24, 48.5, 48.5001` (ordinals 1–5) → send dates `[now + 23.5 h, now + 48 h]`; for ms in `-1, 0, 1, 4999, 5000, 5001`, one block at `0.5 + ms / 3_600_000.0` h → `count == (if (ms > 5000) 1 else 0)`; 65 plans `trip-0…trip-64` with one block at `1 + i / 100.0` h → exactly 60 requests sorted by `sendAt`.
- `elapsedDailyCapAndDST`: for `(UTC, 2026-10-14T00:00:00Z)`, `(Pacific/Kiritimati, 2026-10-13T10:00:00Z)`, `(Pacific/Pago_Pago, 2026-10-14T11:00:00Z)`, `(Europe/Amsterdam, 2026-10-24T22:00:00Z)`: 8 blocks `i = 0…7`, each `reminderBlock((3*i + 1).toDouble(), ordinal = i + 1, zone = zone).copy(startUtc = midnight + (3i+1) h, endUtc = midnight + (3i+2) h)` with both instants written **without** fraction digits (`id` and `checkKey` keep the `reminderNow`-based values `reminderBlock` computed, exactly as iOS l.43–46 mutates only the two instants) → 6 requests at `now = midnight`; at `now = midnight + 8 h` the send dates equal the first run's minus its first 3.
- `ledgerSuppressesMergedMembersAndScopesTripAndHash`: blocks `[reminderBlock(), reminderBlock(1.1, AvoidLight)]`; `first` = the first request; ledger `[ReminderLedgerEntry("trip", "hash", first.sendAt ms, first.memberKeys)]`; at `now = first.sendAt + 1 s`: same plan → empty; plan id `"other"` → 1; hash `"other"` → 1; no ledger → first request's `memberKeys == ["avoid_light|2026-10-14|1"]`.
- `checksOptionalAndIfYouCan`: blocks `reminderBlock()`, `reminderBlock(2.0, ordinal = 2)`, `reminderBlock(3.0, ordinal = 3)`; two separate runs, one with `CheckMark(key = blocks[1].checkKey!!, status = CheckStatus.Done, markedAt = "2026-10-14T00:00:00Z")` and one with the same mark as `CheckStatus.Skipped` → each run's flattened member keys `["seek_light|2026-10-14|1", "seek_light|2026-10-14|3"]`; `reminderPlan([reminderBlock(1.0, AvoidLight), reminderBlock(2.0)])` with `detail.timeline.blockReasons = [BlockReasonTagsView(blockId = blocks[0].id, lines = [CopyLine("If you can", emptyList())])]` and the second block's `priority = Priority.Optional` → empty; `reminderPlan()` with `card.progress.status = TripStatus.Completed` → empty.
- `copyKindsZonesFormatsLanguagesAndStableIdentity`: blocks `reminderBlock()`, `(1.1, AvoidLight)`, `(1.2, NoCaffeine)`, `(1.25, Sleep, zone = "Europe/Amsterdam")`:
  - en 24 h body `Seek bright light from 01:00. Avoid bright light from 01:06. Last caffeine before 01:12. Wind down for bed: sleep from 03:15 (Amsterdam).`
  - nl 12 h body `Zoek vanaf 01:00 a.m. fel licht. Vermijd vanaf 01:06 a.m. fel licht. Neem vóór 01:12 a.m. je laatste cafeïne. Kom tot rust: slapen vanaf 03:15 a.m. (Amsterdam).`
  - the reversed block list gives the same en body; `en.identifier == nl.identifier`; title `Amsterdam → Tokyo`; the four city/IATA combinations give `Amsterdam|AMS → Tokyo|HND`;
  - `[reminderBlock(1.1, ordinal = 2), reminderBlock()]` → body `Seek bright light from 01:00.` (one sentence per kind);
  - a Tokyo block at 4.5 h: 12 h → `Seek bright light from 01:30 PM.`, 24 h → `Seek bright light from 13:30.`;
  - a UTC seek-light at 1 h with an avoid-light at 1.1 h in `Europe/Amsterdam` → `… Avoid bright light from 03:06 (Amsterdam).`; in `America/Los_Angeles` → `… from 18:06 (Los Angeles).`

`ReminderRequestTests` (Android-only):
- `identifierGolden` **[D]**: the first request of the four-kind case above has `identifier == "chronoflow.reminder.MDE5MDAwMDAtMDAwMC03MDAwLTgwMDAtMDAwMDAwMDAwMDAxCnRyaXAKaGFzaApzZWVrX2xpZ2h0fDIwMjYtMTAtMTR8MQphdm9pZF9saWdodHwyMDI2LTEwLTE0fDEKbm9fY2FmZmVpbmV8MjAyNi0xMC0xNHwxCnNsZWVwfDIwMjYtMTAtMTR8MQ=="` and `memberKeys == ["seek_light|2026-10-14|1", "avoid_light|2026-10-14|1", "no_caffeine|2026-10-14|1", "sleep|2026-10-14|1"]`; the default single block gives `"chronoflow.reminder.MDE5MDAwMDAtMDAwMC03MDAwLTgwMDAtMDAwMDAwMDAwMDAxCnRyaXAKaGFzaApzZWVrX2xpZ2h0fDIwMjYtMTAtMTR8MQ=="`.
- `dutchTwelveHourSentenceEndsWithOneDot`: one `Sleep` block at 1.25 h in UTC, nl, 12 h → body `Kom tot rust: slapen vanaf 01:15 a.m.` (one trailing dot).
- `requestCarriesTripBlockHashAccountAndExpiry`: default plan → `tripID "trip"`, `blockID "seek_light-1.0"`, `inputHash "hash"`, `accountID == reminderAccount`, `sendAt = now + 30 min`, `expiresAt = now + 1 h 20 min`.
- `blockIdFallsBackToTypeAndStart`: the same block with `checkKey = null` still yields its id; a block whose id is not findable by key or by `(type, start)` cannot occur from selection, so assert only these two paths.
- `ledgerEntryFromRequest`: `ReminderLedgerEntry.from(request)` has the request's trip, hash, member keys and `sendAtMs == sendAt` epoch ms.
- `missingCopyFallsBackToTheKey`: with a `ReminderStrings` that returns `key.replace("{{time}}", time)` for every language, the nl body equals the en body.

### Task 6. Copy pinned to the generated resources

Commit: `test(reminders): pin notification copy to the generated strings`. Ticks plan.md Phase 12a "**Copy.**" and appends §9 to plan.md "Open questions for the user" as OQ-A557–A566 (*[Refresh R1]*).

**Files:** `core/reminders/src/test/kotlin/net/chronoflow/core/reminders/ReminderStringsResourceTest.kt`, `core/reminders/build.gradle.kts`.

**Behaviour** (JVM; no Android API; parse with `javax.xml.parsers.DocumentBuilderFactory` and `ApiJson.response`): the repo root is `System.getProperty("chronoflow.repoRoot")`, set the way 0c (`plan_copy.sha256`) and 9a do: in `core/reminders/build.gradle.kts`, `tasks.test { systemProperty("chronoflow.repoRoot", rootDir.absolutePath); inputs.files(rootProject.file("tools/l10n/names.lock.json"), rootProject.file("core/ui/src/main/res/values/strings_ios.xml"), rootProject.file("core/ui/src/main/res/values-nl/strings_ios.xml")).withPathSensitivity(PathSensitivity.RELATIVE) }` (without the inputs the test stays UP-TO-DATE after `make strings`). For each `ReminderKind`:
- `name = names.lock.json["reminders\t" + kind.copyKey]` (0c lock key format **[F]** 0c brief l.230);
- the `<string name="$name">` in `core/ui/src/main/res/values/strings_ios.xml` and in `values-nl/strings_ios.xml` exist;
- after replacing `%1$s` with `{{time}}` and un-escaping Android resource escapes (`\'` → `'`, `\"` → `"`), they equal `FakeReminderStrings().text(kind.copyKey, Language.En|Nl, "{{time}}")`.

One method, `fakeCopyMatchesGeneratedResources`, collecting every mismatch.

---

## 7. Acceptance criteria

1. `make targeted ONLY="core:reminders:ReminderSelectionTests core:reminders:ReminderSelectionEdgeTests core:reminders:ReminderScheduleTests core:reminders:ReminderRequestTests core:reminders:ReminderSettingsTests core:reminders:ReminderPermissionTests core:reminders:ReminderStringsResourceTest"` passes.
2. `ReminderSelectionTests.allRNSelectionVectors` evaluates **110 of 110** vectors with 0 mismatches.
3. `make quick` passes (Spotless, Konsist, every JVM test of `:core:*` and `:tools:*`, the tooling tests including `test_copy_fixtures.py`). Every new test method is under 1 s in `build/test-results/<tier>/timings.json`.
4. `make generators-check` passes: `fixtures/reminders/selection.json` is byte-identical to iOS at the pin (SHA-256 `0ac14ed1…b2f`) and listed in `fixtures.sha256`.
5. `./gradlew :core:reminders:dependencies --configuration runtimeClasspath` shows no `androidx.*`, `com.android.*` or Compose artifact; the A3 Konsist rule and the 1a time rule pass for `core/reminders`.
6. `make check` passes on the branch (the orchestrator runs the emulator part; this phase adds no emulator test).
7. Six atomic Conventional Commits, no trailers; plan.md Phase 12a boxes "Engine" (l.3448), "Parity" (l.3453) and "Copy" (l.3454) ticked by Tasks 5, 3 and 6; Task 6 also appends §9 as OQ-A557–A566 (*[Refresh R1]*).
8. No screenshots and no perf numbers (no UI, no timeline change).

---

## 8. Review checklist (Opus reviewer)

- [ ] **Fixture untouched:** `shasum -a 256 core/testing/src/main/resources/fixtures/reminders/selection.json` = `0ac14ed1f2106fdf12ca7498716d4fb4c3b8c6d60b325792ed28c62c72ad3b2f`; produced by `copy_fixtures.py`, not by hand; old `SOURCES` rows unchanged.
- [ ] **Parity test is real:** it asserts `cases.size == 110`, compares whole `ReminderDraft` lists (all members, all three dates, priority, zone), and fails on any one mismatch. No vector is skipped, filtered or "known-failing". `sentMemberKeys` honours the boolean values (only `true` counts).
- [ ] **Coalescing is bounded** by the group's first `sendAt` (not chained from the last member), keeps the first draft's `blockStartUtc`/`displayTz`, takes the max `expiresAt`, and keeps `Core` once any member is core.
- [ ] **Cap day** is the group's `blockStartUtc` in its `displayTz`, not the send date or the check key's date.
- [ ] **Sleep deferral** loops over sleeps sorted by start with the updated `send`, start-inclusive and end-exclusive, and includes non-actionable sleeps of any kind.
- [ ] **Fallback ordinals** count every actionable block in plan order (including ineligible ones); a server `checkKey` wins over the fallback.
- [ ] **Window bounds:** `now + 5 s < sendAt <= now + 48 h`; lookback uses `now − 36 h` and `sendAt <= now + 5 s` for both groups and members; ledger `elapsed` bounds are inclusive at both ends; 60 is applied after the global sort by `(sendAt, identifier)`.
- [ ] **Body:** one sentence per kind in start order; zone label only when the member zone differs from the group zone; `trimEnd('.') + "."`; sentences joined by one space.
- [ ] **Identifier:** member keys in draft order; `\n` separators; standard Base64 with padding; prefix `chronoflow.reminder.`; independent of language and time format.
- [ ] **No clock or locale reads:** `grep -rnE "Instant\.now|System\.currentTimeMillis|ZoneId\.systemDefault|Locale\.getDefault|TimeZone\.getDefault|String\.format|\.format\(" core/reminders/src/main` is empty.
- [ ] **API level (minSdk 29):** no Java 9+ API in main: `grep -rn "ofInstant\|\.stream()\|Stream\.\|Math\.floorDiv" core/reminders/src/main` is empty or only uses Java 8 overloads.
- [ ] **Strict settings decode:** missing key → `null`, string booleans → `null`, `reminderTimeFormat` only `"12"` (string) is 12 h.
- [ ] **Scope:** no `ReminderNotificationCenter`, no DB, no WorkManager/AlarmManager, no resources or `android-only.json` entries, no UI; `ReminderPermission` is the only permission enum.
- [ ] **Swift names kept** for types, functions and test methods (§2.2 table; Appendix A).
- [ ] **Exceptions** caught by type only; no `runCatching`; nothing swallows `CancellationException` (there is no suspending code in main).
- [ ] Typical mistakes: a hand-written sort that is not stable (Kotlin `sortedWith`/`sortedBy` are stable; use them); comparing `Priority` with `==` against a new `Unknown("core")` instead of `Priority.of`; recomputing the cap counts from `sentCountsByDay` by reference (mutating the caller's map); `Instant.toString()` used for `startUtc` in the test helper (drops `.000`); `Base64.getMimeEncoder()` (inserts line breaks); `"%02d".format(…)`.

---

## 9. Open questions (proceed on the default)

| ID | Question | Default |
|---|---|---|
| OQ-12a-1 | plan.md names the parity test `ReminderParityTest`; AGENTS.md says ported tests keep Swift names (`ReminderSelectionTests.allRNSelectionVectors`). | Use the Swift names. The plan task is met by that test. |
| OQ-12a-2 | `:core:reminders` is JVM; the reminder copy lives in Android resources (0c). | `fun interface ReminderStrings` seam; 12b implements it with `CopyIndex` and a localized context; `FakeReminderStrings` in `:core:testing`, pinned to the generated XML by Task 6. No JVM copy table in main code. |
| OQ-12a-3 | iOS `ReminderRequest` has no expiry; Android wants `setTimeoutAfter(expiresAt − now)` (research `07` OQ-14, A13 "20 min setTimeoutAfter"). | Add `expiresAt` (the group's) to `ReminderRequest`; it is not in the identifier. 12b chooses how to use it. |
| OQ-12a-4 | Unparseable plan instants are NaN on iOS (undefined results). | Skip such blocks and sleeps; they take no fallback ordinal. |
| OQ-12a-5 | iOS ledger `sendAtMs` is a `Double`; 4a's `reminder_ledger.send_at_ms` is `INTEGER`. | Keep `Double` in the engine for parity; 12b stores `floor(sendAtMs)` and reads it back as `Double`. |
| OQ-12a-6 | iOS appends a ledger entry for every request at **schedule** time (`ReminderService.swift:134–140`); plan A13/12b append on **delivery**. | Not decided here (12b). Recommended for 12b: follow iOS (persist `elapsed(previous) + from(request)` for each request added or already pending) and keep the delivery append; duplicate entries are harmless because member keys are unioned. |
| OQ-12a-7 | plan 12b names `ReminderPermissionState (granted, ask, blocked, unsupported)`; iOS has `ReminderPermission (undetermined, granted, blocked, unsupported)`. | One enum, `ReminderPermission`, from this phase; 12b maps "ask" to `Undetermined` and does not add a second enum. |
| OQ-12a-8 | Destination of the fixture: 0d's copier writes files only to the fixtures root or a same-named sub-directory. | Add the optional destination element (Task 1); the file lands at `fixtures/reminders/selection.json` as plan.md asks. |
| OQ-12a-9 | `ZoneId.of` and Foundation `TimeZone(identifier:)` differ on non-IANA ids (abbreviations, `GMT+2`). | Plan data uses IANA ids only; invalid → UTC. No test beyond `Not/AZone`. |
| OQ-12a-10 | A stored `leadMin` outside 15/30/60 (server forbids it). | Accept it as iOS does; only `leadPatch` validates. |

None blocks the user; the orchestrator copies OQ-12a-3, -5, -6 and -7 into the 12b brief. Plan numbers (*[Refresh R1]*): OQ-12a-1 … -10 = OQ-A557 … OQ-A566, appended by Task 6. OQ-12a-5 sits beside the decided OQ-A118 (4a tables, `send_at_ms` INTEGER).

---

## 10. Hand-off to 12b and 12c

- **12b** calls `reminderRequests` on IO after reading `ReminderSettings.decode(remindersSection)`, `reminderTimeFormat(preferencesSection)`, the app `Language`, the plans (`keepOffline`, not deleted, current version, done/skipped marks; port `ReminderInputs.plans`, `ReminderService.swift:21–39`) and the ledger rows; it only schedules when `permission.remindersActive(settings)`. It uses `REMINDER_*` constants, `ReminderRequest.expiresAt`, `ReminderLedgerEntry.from` and implements `ReminderStrings`.
- **12c** uses `ReminderSettings.enabled`, `settingEnabled`, `enabledPatch`, `leadPatch`, `isLeadOption` and `ReminderPermission`.

---

## Appendix A. iOS → Kotlin test map

| iOS (`Tests/RemindersTests/`) | Kotlin (`core/reminders/src/test/…`) | Task |
|---|---|---|
| `SelectionParityTests.swift` `ReminderSelectionTests.allRNSelectionVectors` | `ReminderSelectionTests.allRNSelectionVectors` | 3 |
| `PermissionTests.swift` `permissionMatrix` | `ReminderPermissionTests.permissionMatrix` | 2 |
| `ScheduleTests.swift` `ledgerValidationAndExpiry` | `ReminderScheduleTests.ledgerValidationAndExpiry` | 4 |
| `ScheduleTests.swift` `windowMarginAndCap`, `elapsedDailyCapAndDST`, `ledgerSuppressesMergedMembersAndScopesTripAndHash`, `checksOptionalAndIfYouCan`, `copyKindsZonesFormatsLanguagesAndStableIdentity` | `ReminderScheduleTests.<same>` | 5 |
| `PermissionTests.swift` `mockAuthorizationChangesOnlyAfterAnExplicitRequest`, `absoluteTriggersSurviveDSTAndDeviceZoneChanges`, `nativeAuthorizationMatrix`; `ServiceTests.swift`, `SignOutTests.swift`, `TapTests.swift` | — | 12b |
| `FirstTripRemindersOfferTests.swift` | — | 12c |

Android-only: `ReminderSelectionEdgeTests`, `ReminderSettingsTests`, `ReminderRequestTests`, `ReminderStringsResourceTest`, `test_copy_fixtures.py::test_row_with_destination_subdirectory`.
