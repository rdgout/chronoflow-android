# Phase 9a brief: Wizard model (JVM)

- **Branch:** `feat/phase-9a-wizard-model`, stacked on the stack top once 0c and 0d are in the stack.
- **Depends on:** 0c (string lock `tools/l10n/names.lock.json`, plan copy `core/plan/src/main/resources/plan_copy.json`), 0d (generated contracts, `ApiJson`, `ApiTripReviewInput`, `DataFailure`, `FieldMessage`, `ProfilePreferences`, fixtures in `:core:testing`). **Wave:** 3.
- **Plan entry:** `plan.md` "### Phase 9a: Wizard model (JVM)" (l.2595–2629). Next phase: 9b (l.2631–2667).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`).
- **Tests:** follow AGENTS.md "Tests" for every test. This phase is JVM only: JUnit 4.13.2, assertk 0.28.1, kotlinx-coroutines-test 1.11.0, Turbine 1.2.1. No Robolectric, no emulator. Plain JVM test classes carry **no** tier annotation (0e rule 4). Every `runTest` passes `timeout = 60.seconds`. Each test runs in under 1 s. The current rules (test diet, 2026-11-20) override every budget written below; see "Stack check (2026-11-20)".
- **Stack check (2026-11-20, against `f8ad327a`: 0a–8 plus the test-diet branch).** 0c and 0d (and 0e, 1a, 4c) are in the stack. Nothing of 9a exists yet: `core/wizard/` holds only `build.gradle.kts` (`plugins { id("chronoflow.jvm.library") }`, no sources, no dependencies), and `:feature:wizard` likewise holds only its `build.gradle.kts` (namespace `net.chronoflow.feature.wizard`). No earlier phase wrote wizard model code; build on these skeletons. Deltas:
  - **plan.md lines.** Phase 9a is l.2595–2629, 9b l.2631–2667. Ticks: "**Model**" l.2615, "**No-health-data rules.**" l.2623, "**Discrepancies.**" l.2624, "**Ported tests.**" l.2625. OQ-A18 is l.605, OQ-A36 l.623, "Standard acceptance" l.1019; A3 (l.90) and "No health data" (l.142) are unchanged. All `06-wizard.md` section ranges in §2.1 hold.
  - **Contracts (all present).** `ApiTripReviewInput(input, identity, profile)` (encode-only) and `ApiTripSaveInput.from` are in `core/model/…/model/api/TripWizardContracts.kt` (l.22, l.62). Every type of §4 check 2 is in `core/model/…/model/contracts/TripPlan.generated.kt` (also `ApiReviewItinerary`, `ApiTripInputBusy`, `AirportRef(iata, city, zone)`); `ApiReviewIdentity.guestSecret: String?` exists. Generated enums are `sealed interface … : RawEnum` with `data object` cases plus `Unknown(raw)`. The review status enum is **`PlanStatus`** (`Ok`, `Modified`, `NeedsAnswers`, `NoPlan`), the prompt target enum is **`ReviewPromptTarget`** (`Profile`, `Trip`), and `PromptKind` also has `ReturnLeg`. `ApiReviewPrompt` has a private constructor; build it with `ApiReviewPrompt(...)` (`operator fun invoke`, which filters `answer` through `ProfilePreferences.promptAnswers`); `answer` is `Map<String, JsonPrimitive>`. `Language` is in `contracts/TripPlan.generated.kt` (`En`, `Nl`, `Unknown`). `ProfilePreferences` (`contracts/ProfilePreferences.kt`) has every member §4 check 4 lists. `ApiJson.request`/`ApiJson.response` are in `api/ApiJson.kt`.
  - **`DataFailure` and `FieldMessage`** are in package `net.chronoflow.core.model` (`core/model/…/model/DataFailure.kt`), not `contracts`. `DataFailure` extends `Exception`, so §5.1 rule 6 holds (throw it). It also has `SaveCapacity`, `Unauthorized`, `EmailUnverified`, `Conflict`, `Unsupported`; `Throttled(retryAfterSeconds: Double, status: Int)`, `Server(status: Int?)`. `FieldMessage(key, arguments: Map<String, JsonPrimitive>)` has a one-argument `FieldMessage(key)` constructor.
  - **Fixtures.** All 16 `wizard/*.json` exist; `input.json`/`review.json` hash `c6b6852b…`/`cc96738a…`; `snapshot.json` lists only `AMS` in `airports`. The loader is `net.chronoflow.core.testing.Fixtures.text("wizard/input.json")` (or `.bytes`).
  - **Build.** `chronoflow.jvm.library` already adds `junit4` and `assertk` to `testImplementation`, and the test-runtime entries (JUnit BOM, vintage engine, platform launcher, `:core:testing-junit`) to `testRuntimeOnly`. `:core:model` exports `kotlinx-serialization-json` and `kotlinx-coroutines-core` as `api`. So `core/wizard/build.gradle.kts` needs only `alias(libs.plugins.kotlin.serialization)`, `api(project(":core:model"))`, `testImplementation(project(":core:testing"))`, `libs.kotlinx.coroutines.test` and `libs.turbine` (follow `core/plan/build.gradle.kts`, which also shows the `chronoflow.repoRoot` system property for Task 9). Catalog versions match (JUnit 4.13.2, assertk 0.28.1, coroutines 1.11.0, Turbine 1.2.1).
  - **Konsist.** `core/wizard` is already in the JVM-module rule (`konsist/…/KonsistRules.kt:15`) and in `timeHygieneModules` (l.287), so Task 1's Konsist step is a no-op. `Makefile` `JVM_MODULES` and `Scripts/targeted.py` already list `core:wizard`.
  - **Selectors.** `make targeted` selectors have no leading colon and no `--tests`: `make targeted ONLY="core:wizard:<Class>[#method] …"`.
  - **Test diet (AGENTS "Tests" 5).** A plain JVM case has a hard 1,000 ms budget with **no** first-case allowance (`TestTimingListener.kt`, `JVM_BUDGET_MS`), and `TestHangWatchdog` stops any case at 60 s. Keep the first case of each class cheap (fixture decode and `ApiJson` warm-up count). No case here is a wall-clock budget test, so none is `@TimingGate` (if one is added, it needs `core:wizard` in `TIMING_MODULES`). Plain JVM classes still carry no tier annotation (Konsist `plainJvmClassesHaveNoTier`).
  - **Copy files.** `names.lock.json` keeps `{{name}}` placeholders in wizard keys (e.g. `"wizard\tConnection in {{airport}} · {{duration}}"`), and `"{{flight}} · via {{airports}}"` is absent from it and from `android-only.json` (as OQ-9a-8 expects). `plan_copy.json` is `{"entries": [{"key": …}]}`; match against `entries[].key`.
  - **OQ numbering.** `plan.md` OQ rows end at **OQ-A348**. Task 10 records OQ-9a-n as **OQ-A(348+n)**: OQ-9a-1 … OQ-9a-14 → **OQ-A349 … OQ-A362**, each tagged "(9a OQ-9a-n)" as Phase 8 did. Record OQ-9a-1 too (A349), so the numbers have no gap.
  - **Flagged for the worker (keep the brief's behaviour; report, do not silently change):**
    1. **`WizardMode` name clash.** `:core:navigation` already has `@Serializable sealed interface WizardMode { Create, Edit(tripId), Duplicate(tripId) }` (`core/navigation/…/navigation/Route.kt:89`, used by `Route.Wizard(mode)`, `AppShell`, `TodayTab`, `TripPage`). This brief's `enum class WizardMode { member, guest }` (`net.chronoflow.core.wizard.model`) means something else. Both compile (different packages, and `:core:wizard` cannot see `:core:navigation`), but 9b's `:feature:wizard` will import both. Keep the Swift name; name the clash in the PR for 9b. `TripWizardAction { edit, duplicate }` likewise mirrors the navigation `Edit`/`Duplicate`.
    2. **Existing error-path port.** `core/model/…/model/api/TripWizardErrors.kt` (`map`, `journeyPath(path, outboundCount)`, `camel`) ports the iOS **DataAccess** `TripWizardErrors.swift`. It is not `WizardJourney.errorPath`: it offsets return legs by `outboundCount` rather than `returnBoundary`, and its top-level map lacks `return_from_leg` and the `override_*`/`use_profile_override` keys. Port `WizardJourney.errorPath`/`camel`/`topFields` in `:core:wizard` as Task 4 says, as iOS keeps both; do not call or edit the `:core:model` one. `ProfilePreferences.camel` also differs (drops empty parts), so do not reuse it either.
    3. **`MockWizardPrompts` exists.** 4c landed `object MockWizardPrompts { fun make(input, profile): List<ApiReviewPrompt> }` in `:core:data-mock` (JVM, no dependency on `:core:wizard`, so `testImplementation(project(":core:data-mock"))` would not cycle). The brief's reasons for hand-building prompts in `sheetValidationAcceptsCommaRatingAndRejectsMissingDrinks` and for leaving `guestFineTuneUsesOnlyTheDrawnRowsAndLeavesMemberRowsAvailable` to 9b ("needs `MockWizardPrompts` from 4c") are stale. Default: keep the brief's split and hand-built prompts; say in the PR that the guest Fine-tune test is now portable.
    4. **Calendar tables.** `:core:plan` already has `internal` en/nl weekday/month tables (`core/plan/…/plan/PlanCalendarText.kt`, `DateTemplate`). They are internal and `:core:wizard` does not depend on `:core:plan`, so Task 2 writes its own tables in the same style.

Tags: **[F]** fact verified in source (path and line). **[R]** decision for Android adopted by this brief. **[A]** Apple output measured on this Mac on 2026-11-16 with `swift` (Foundation `DateFormatter`), used as goldens.

---

## 1. Goal

Port the iOS trip-wizard model (`Packages/ChronoFlowKit/Sources/TripWizard/Model/`, minus the stores and the airport search) into the pure-JVM module `:core:wizard`, so that 9b can host it in a ViewModel and 9c–9h can draw it. Concretely:

1. **Wall-clock model.** `WizardDateTime`: strict `yyyy-MM-ddTHH:mm` and `HH:mm` values, airport-zone instants that reject DST gaps, fixed en/nl date text.
2. **Form and route.** `WizardForm` and its parts, the route-editing rules verbatim (stops, return, home route, the 16-flight limit), and the mapping form ↔ `ApiTripReviewInput` / `ApiTripInput` / `ApiTripInputSnapshot`.
3. **Validation and picker bounds.** `WizardValidation`, `WizardFlightTiming`, `WizardOutboundRules`, `WizardFlightRules`, `WizardJourney` (error step and server-path mapping).
4. **State and recovery record.** `MemberWizardState`, `MemberWizardDraft`, `WizardDraftStore` (interface only), a pure recovery-record codec with the iOS migrations and the 30-minute expiry rule, the initial-draft factories, and the Edit/Duplicate loaders.
5. **Checking.** `WizardChecking.run` with an injected clock: ticks at 400/800 ms, minimum 1200 ms, 300 ms pause, slow at 10 s, failure at 30 s.
6. **The model.** `MemberWizardModel`: modes (member, guest) and entries (create, edit, duplicate, guest, seeded), the step graph (route first, then times), transitions, questions, Fine-tune, Ready, save hand-off, retry countdowns, the 250 ms choice delay, serialized persistence, and the progress values of the boards.
7. **No health data.** Only `age_band` and `shift_work` block. `exclude_caffeine` and `flight_movement_breaks` are neutral and default to false. Retired keys never survive.
8. **Ported tests.** 60 iOS test functions under their Swift names (§10 maps each one), plus Android tests for each board deviation.

No UI, no Android API, no Compose, no database, no network. Nothing in this phase draws, stores to disk or calls the server.

---

## 2. Inputs to read

### 2.1 This repo

| Path | Why |
|---|---|
| `AGENTS.md` | Working rules, "Tests", "Commands", "Git" (no commit trailers), "Traps" |
| `plan.md` l.2595–2629 (Phase 9a), l.2631–2667 (9b, to see what you must **not** do), A3 (module graph, l.90), "No health data (2026-11-10)" (l.142), OQ-A18 (l.605), OQ-A36 (l.623), "Standard acceptance" (l.1019) | Scope and decisions |
| `docs/research/06-wizard.md` §3 (l.185–300), §4 (l.304–527), §5 (l.529–606), §6.1 (l.610–643), §11 (l.1074–1087), §12 (l.1089–1108) | Behaviour spec, state machine, request shape, wall-clock model, validation table, discrepancies, OQ defaults |
| `docs/phases/phase-0d-contracts.md` §5.4 (ApiJson, `RawEnum`), Task 7 (`ApiTripReviewInput`, `ApiTripSaveInput`) | What the DTOs look like in Kotlin |
| `docs/phases/phase-0c-strings.md` T1 (lock format: key `"<prefix>\t<English key>"`) | For `WizardCopyKeysTest` |
| `docs/phases/phase-1a-plan-core.md` Task 1 and Task 8 | The fixed calendar-table style and the time-hygiene Konsist rule you must stay inside |
| `core/testing/src/main/resources/fixtures/wizard/*.json` (16 files) | Test fixtures (0d) |

### 2.2 iOS (read-only; read with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

Prefix `Packages/ChronoFlowKit/Sources/TripWizard/Model/` = `M/`. Kotlin paths are under `core/wizard/src/main/kotlin/net/chronoflow/core/wizard/` = `W/`.

| iOS file (lines) | Kotlin port | Task |
|---|---|---|
| `M/WizardDateTime.swift` (114) | `W/model/WizardDateTime.kt`, `W/model/WizardCalendarText.kt` | 2 |
| `M/WizardForm.swift` (199) | `W/model/WizardForm.kt` | 3 |
| `M/WizardRoute.swift` (47) | `W/model/WizardRoute.kt` | 3 |
| `M/WizardJourney.swift` l.4–109 (form ↔ input) | `W/model/WizardJourney.kt` | 3 |
| `M/WizardJourney.swift` l.111–169 (`errorStep`, `errors`, `errorPath`, `camel`, `topFields`) | same file | 4 |
| `M/WizardValidation.swift` (138) | `W/model/WizardValidation.kt` | 4 |
| `M/WizardFlightTiming.swift` l.1–74 | `W/model/WizardFlightTiming.kt` | 4 |
| `M/WizardOutboundRules.swift` (137; `WizardOutboundRules` + `WizardFlightRules`) | `W/model/WizardOutboundRules.kt` | 4 |
| `M/MemberWizardStore.swift` l.6–112 (types, state, draft, store protocol), l.134–156 (`recover` migrations), l.164–216 (`start(seed:)`, `startTrip`, `initialDraft`) | `W/model/MemberWizardState.kt`, `W/model/MemberWizardRecord.kt`, `W/model/MemberWizardDrafts.kt` | 5 |
| `M/TripWizardActions.swift` (113) | `W/model/TripWizardActions.kt` (l.84–112 `acceptSourceReload` goes with the model in Task 7) | 5, 7 |
| `M/WizardPresentation.swift` l.4–20 (`SourceChange` type only) | `W/model/WizardPresentation.kt` | 5 |
| `M/MemberWizardCompletion.swift` l.4–20 (`WizardSaveFailure`, `WizardSaveOutcome`) | `W/model/WizardSaveFailure.kt` | 5 |
| `Generation/TripGenerationService.swift` l.21–59, `Generation/TripGenerationReview.swift` l.4–19 (value types only) | `W/generation/TripGenerationTypes.kt` | 5 |
| `M/WizardChecking.swift` (76) | `W/model/WizardChecking.kt` | 6 |
| `M/MemberWizardModel.swift` l.5–28 (`MemberWizardGeneration` protocol) | `W/generation/MemberWizardGeneration.kt` | 6 |
| `Generation/TripGenerationAnswers.swift` l.13–70 (`applyingAnswers`, `set`) | `W/generation/TripGenerationAnswers.kt` | 6 |
| `M/MemberWizardModel.swift` l.37–271 | `W/model/MemberWizardModel.kt` | 7 |
| `M/MemberWizardNavigation.swift` (69) | `W/model/MemberWizardNavigation.kt` | 7, 8 |
| `M/MemberWizardReview.swift` (342) | `W/model/MemberWizardReview.kt` | 7, 8 |
| `M/MemberWizardRetry.swift` (50) | `W/model/MemberWizardRetry.kt` | 7 |
| `M/MemberWizardCompletion.swift` l.43–300 | `W/model/MemberWizardCompletion.kt` | 7 |
| `M/WizardMode.swift` (91) | `W/model/WizardMode.kt` | 7, 8 |
| `M/WizardFlightTiming.swift` l.76–84 (model extension) | in `MemberWizardModel.kt` | 7 |
| `M/MemberWizardCompletion.swift` l.22–41 (`WizardCredits`) | `W/model/WizardCredits.kt` | 9 |
| `M/WizardOptionalAnswer.swift` (34) | `W/model/WizardOptionalAnswer.kt` | 9 |
| `M/WizardPresentation.swift` l.21–272 | `W/model/WizardPresentation.kt` | 9 |
| `M/GuestWizardStore.swift` l.25–29 (guest initial values), l.114–124 (record scrubbing) | `MemberWizardDrafts.guest()`, `MemberWizardRecord` | 5 |

Tests: `Packages/ChronoFlowKit/Tests/TripWizardTests/` (`WizardPortTests.swift` holds `WizardDateTimeTests`, `WizardValidationTests`, `WizardFormTests` and the `wizardAirport` helper at l.350; `WizardSearchPresentationTests.swift` holds `WizardPresentationTests` at l.77; `WizardCheckingTests.swift` l.100–150 holds `CheckingTestClock` and `eventually`; `WizardRecoveryTests.swift` l.112–117 holds `WizardTestClock`; `TripGenerationTests.swift` l.303–315 holds `generationInput()`/`generationReview()`).

iOS docs: `plan.md` l.1787–1856 (16, 16a, 16b, fidelity follow-up, 16b.2), `Docs/wizard-16a.md`, `Docs/wizard-16b2.md`, `Docs/wizard-fidelity.md`.

### 2.3 Research quotes this brief relies on

- `06 §4.3`: "Accepted answers: `age_band` ∈ shape options (`under_18`, `18_64`, `65_plus`); `shift_work` ∈ {true, false}."
- `06 §5.3`: "`LocalDateTime.atZone` silently shifts gap times; do not use it for validation." "Never use `java.util.Date`/`Calendar` with the device zone."
- `06 §11.1`: the iOS guest weights are "shifted one step against its own stated source"; default is the boards.
- `06 §12` OQ 1: "**Board `pct` values** … with the never-backwards floor." OQ 2: "**Board**: no back chevron and no system-back pop on these first screens."
- `plan.md` l.142: "Only `age_band` and `shift_work` block; `exclude_caffeine`/`flight_movement_breaks` default false."

### 2.4 Boards

`_design/canvas/WizardSpec.dc.html` (behaviour text, quoted in `06 §3`), `_design/canvas/WizardCopy.dc.html` (copy rules), and `_design/canvas/Wizard.dc.html` for two facts only:
- **[F]** `Wizard.dc.html:258`: `firstScreen = (screen === 'where' && v !== 'home' && v !== 'homestop') || (screen === 'outbound' && v === 'dup') || (screen === 'ready' && isEdit)`; l.260: `back: !firstScreen && screen !== 'checking' && screen !== 'calc' && !(screen === 'fail' && isEdit)`.
- **[F]** the `pct` prop of each board (table in §6 Task 8).

This phase draws nothing; it invents no screen. Every state it exposes has a board or an iOS twin; the two without a board (16-flight limit, preference toggles) are named in §8 and come from iOS.

### 2.5 API endpoints

None are called. The shapes the model builds or reads are 0d DTOs: `ApiTripReviewInput` (`POST /v1/trips/review` body, `06 §5.1`), `ApiTripReview`, `ApiTripInputSnapshot` (`GET /v1/trips/{id}/input`), `ApiAirportSuggestion`. Calls belong to 9b.

---

## 3. Out of scope (owner in brackets)

- The database recovery row, its writer, `DraftRepository` (9b). You write the pure codec and the `WizardDraftStore` interface only.
- `TripGenerationService` (review/save/edit calls, in-flight map, 90 s timeout), `TripGenerationEffects` (9b). You write the value types, the `MemberWizardGeneration` interface and `applyingAnswers`.
- `AirportSearchService` and its states (9b). The Kotlin model has **no** `search` property (iOS stores it on the model only to hand it to views).
- `TripWizardViewModel`, `viewModelScope` hosting, `SavedStateHandle` (9b).
- `GuestWizardStore`, `GuestRecoveryGate`, `GuestSessionAcceptance`, claim (11a/11b). You only provide `MemberWizardDrafts.guest()` with the guest initial values.
- Any screen, sheet, picker or haptic (9c–9h). `formatLocal` (the iOS combined pill) is not ported (OQ-9a-7).
- The `#if DEBUG` board hooks of `MemberWizardModel` (`boardAnswerError`, `boardDiscardConfirmation`, `boardFrozen`): seeds phase.
- Ported iOS suites that need the generation service, the database or `MockWizardPrompts` (§10.2 lists them with their owner).

---

## 4. What 0c and 0d hand you (check before starting)

Run these; if one fails, stop and report rather than adding the type yourself.

1. `grep -rn "class ApiTripReviewInput\|fun from(" core/model/src/main/kotlin/net/chronoflow/core/model/api/` finds `ApiTripReviewInput(input, identity, profile)` (encode-only) and `ApiTripSaveInput.from`.
2. `grep -rln "class ApiTripInput\b\|class ApiTripReview\b\|class ApiReviewPrompt\b\|class ApiReviewIdentity\b\|class ApiTripInputSnapshot\b\|class ApiAirportSuggestion\b\|class ApiStayEnd\b\|class ApiReviewSummary\b\|class ApiReviewPrice\b" core/model/src/main/kotlin/net/chronoflow/core/model/contracts/` finds each type. `ApiReviewIdentity` has `guestSecret: String?` (iOS `TripPlan.swift:242`).
3. `PromptKind` has `AgeBand`, `Screening`, `Cups`, `BlueBlockers`, `FreeDayTimes`, `Room`, `LatestWake`, `SeatSide`, `LightDevice`, `LateCheckOut`, `PreDepartureBusyHours` (and `ReturnLeg`), `Unknown(raw)`; the review status enum (`PlanStatus`) has `NeedsAnswers` and `NoPlan`; the prompt target enum (`ReviewPromptTarget`) has `Profile`, `Trip`, `Unknown`.
4. `object ProfilePreferences` has `requestFields`, `camel`, `bodyClock`, `request`, `promptAnswers`, `reviewPrompts`, `isSupported`.
5. `sealed class DataFailure` (`core/model/…/model/DataFailure.kt`, package `net.chronoflow.core.model`) has `ReviewExpired`, `NotEnoughCredits`, `ProfileInvalid(fields)`, `Offline`, `Validation(fields, status)`, `Throttled(retryAfterSeconds: Double, status)`, `Server(status)`; `FieldMessage(key, arguments)` exists.
6. 0d's wire enum `Language` (with `Unknown(raw)`) exists in `:core:model` contracts; this brief uses it for date text, as 1a does. Do not add another `Language` type.
7. `ApiJson.response` encodes and decodes every contract type you store (`ApiTripReview`, `ApiReviewPrompt`, `ApiAirportSuggestion`, `ApiStayEnd`, `ApiReviewPrice`). Prove it once in Task 5's round-trip test.
8. The fixture loader 0d put in `:core:testing` (`Fixtures.text(path)` in `core/testing/…/testing/Fixtures.kt`) reads `wizard/input.json`, `wizard/review.json`, `wizard/review-request.json`, `wizard/snapshot.json`.
9. `tools/l10n/names.lock.json` has keys of the form `"wizard\t<English key>"`.

---

## 5. Module and package layout

| Module | Plugin | Packages |
|---|---|---|
| `:core:wizard` (0a skeleton, `core/wizard/build.gradle.kts` only) | `chronoflow.jvm.library` | `net.chronoflow.core.wizard.model`, `net.chronoflow.core.wizard.generation` |

- Dependencies: `api(project(":core:model"))`, kotlinx-serialization-json (plugin applied the way 0d applied it to `:core:model`), kotlinx-coroutines-core. Tests: `testImplementation(project(":core:testing"))`, junit4, assertk, kotlinx-coroutines-test, turbine, and the 0e test-runtime entries 0e lists for every module with tests. (Stack check: the convention plugin already adds junit4, assertk and the test-runtime entries, and `:core:model` exports serialization-json and coroutines-core as `api`.)
- No `android.*`, `androidx.*` or Compose import (Konsist rule 2 already covers `core/wizard`). No `Locale.getDefault`, `TimeZone.getDefault`, `ZoneId.systemDefault`, `System.currentTimeMillis`, `Instant.now`, `Clock.system*`, `DateTimeFormatter`, `String.format` in main code (1a's time-hygiene rule; `core/wizard` is already in its module list, `KonsistRules.kt:287`).
- Test path shorthand: `T/` = `core/wizard/src/test/kotlin/net/chronoflow/core/wizard/`.

### 5.1 Porting rules [R]

1. **Names.** Swift type, function and property names stay (`WizardForm`, `routeComplete`, `returnBoundary`, `MemberWizardStep.whereTo`). Swift enum cases stay lower camel (`MemberWizardStep.whereTo`, `WizardMode.member`) because they are persisted by name; `@SerialName` is the raw value.
2. **Value types are immutable.** Every Swift `struct` becomes a `@Serializable data class` with `val` properties. A Swift `mutating func` becomes a function that returns the new value (`fun WizardForm.insertStop(airport, beforeDestinationOf: Int): WizardForm`). Swift functions that mutate **and** return something return a small result class: `takeFlight` → `TakenFlight(form, removed: RemovedFlight)?`; `removeReturn` → `ReturnRemoval(form, retained: RetainedReturn)`. `updateLeg(index) { … }` becomes `updateLeg(index: Int, update: (WizardLeg) -> WizardLeg): WizardForm`.
3. **JSON.** iOS `JSONValue`/`JSONObject` → kotlinx `JsonElement`/`JsonObject`. `JSONValue.null` → `JsonNull`; an absent key is an absent key (they differ: absent = unchanged, null = clear, `06 §5.1`).
4. **Times.** iOS `Date` holding wall components → `LocalDateTime`; iOS `Date` holding an instant → `java.time.Instant`; the checking clock is `Long` milliseconds (Task 6). The model's wall clock is `now: () -> Instant`, injected.
5. **Fingerprints.** iOS `Data` fingerprints (`checkedInput`, `revisit`, `originalInput`) → `String` (canonical JSON, Task 5) so data-class equality works.
6. **Failures.** iOS `throw DataFailure.server(status: nil)` → `throw DataFailure.Server(null)` (0d's type is a `Throwable`; check with `grep -n "sealed class DataFailure" -A2`). If it is not throwable, return `Result`-free nullable values and say so in the PR; do not add a second failure type.
7. **Coroutines.** iOS `Task {}` → `scope.launch {}` on the injected scope. Never catch `CancellationException` without rethrowing, except where Task 6 says so explicitly. No `GlobalScope`, no `runBlocking`.

---

## 6. Tasks

Each task is one commit. Commit messages are Conventional Commits with a lower-case imperative subject and **no trailer of any kind** (AGENTS "Git"). Run `make targeted ONLY="core:wizard:<Class>[#method]"` for what you touched; run `make quick` once before handing back.

### Task 1. Module wiring and test support

Commit: `build(wizard): wire the wizard module and its test support`. Ticks nothing.

**Files**
- `core/wizard/build.gradle.kts` (dependencies of §5).
- ~~If 1a's time-hygiene Konsist rule lists modules explicitly and `core/wizard` is missing, add it~~ — already listed (`konsist/…/KonsistRules.kt:287`); nothing to do.
- `T/WizardTestSupport.kt`:
  - `fun wizardAirport(iata: String, city: String, zone: String): ApiAirportSuggestion` = `ApiAirportSuggestion(iata = iata, name = city, city = city, country = "", timezone = zone)` (iOS `WizardPortTests.swift:350`).
  - `fun generationInput(): ApiTripInput` and `fun generationReview(): ApiTripReview`: decode `wizard/input.json` and `wizard/review.json` with `ApiJson.response` (iOS `TripWizardTests/Fixtures/{input,review}.json` are byte-identical to these, sha256 `c6b6852b…` and `cc96738a…`).
  - `class WizardTestClock { var now: Instant = Instant.ofEpochSecond(1_800_000_000); fun advance(seconds: Double) }` (iOS `WizardRecoveryTests.swift:112`). `advance` adds `(seconds * 1000).roundToLong()` ms, so `advance(1799.999)` is exact.

**Tests**
- `T/WizardTestSupportTest`: `fixturesDecode` — `generationInput().outbound.legs[0].fromIata == "AMS"` and `generationReview().identity.token` is not blank; `testClockAdvancesInMilliseconds` — `advance(1799.999)` moves `now` by exactly 1_799_999 ms.

### Task 2. Wall-clock date and time model

Commit: `feat(wizard): add the strict wall-clock date and time model`. Ticks nothing yet.

**Files** `W/model/WizardDateTime.kt` (`object WizardDateTime`), `W/model/WizardCalendarText.kt` (internal, Android-only).

**Behaviour** (port of `WizardDateTime.swift`; every function is pure and allocation-light)
1. `fun isTime(value: String?): Boolean` — regex `^([01]\d|2[0-3]):[0-5]\d$`.
2. `fun date(value: String): LocalDateTime?` — regex `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$`; parse the five integers; year must be > 0; build with `LocalDateTime.of(y, m, d, h, min)` inside `try { } catch (e: DateTimeException) { null }`; then require `string(result) == value` (round trip). Rejects `2026-02-30T12:00`, `2026-11-01T24:00`, `2026-11-01T12:60`, `2026-11-01`, `""`, `invalid`, `0000-01-01T12:00`.
3. `fun string(date: LocalDateTime): String` — `yyyy-MM-ddTHH:mm`, zero-padded, built by hand (no `String.format`, no `DateTimeFormatter`).
4. `fun shiftDate(value: String, days: Int): String?` — `date(value.take(10) + "T12:00")?.plusDays(days)` → first 10 chars. `("2028-02-28", 1)` → `"2028-02-29"`; `("2026-12-31", 1)` → `"2027-01-01"`.
5. `fun pickerValue(value: String?, fallbackDate: String, dateTime: Boolean = true): String` — date-time: `value` if `date(value) != null`, else `fallbackDate + "T12:00"`; time: `value` if `isTime(value)`, else `"12:00"`.
6. `fun pickerDate(value: String, dateTime: Boolean = true): LocalDateTime?` — `date(if (dateTime) value else "2001-01-15T$value")`.
7. `fun pickedValue(date: LocalDateTime, dateTime: Boolean = true): String` — `string(date)` or its last 5 chars.
8. Android-only, for 9e's Material pickers (which work in UTC millis): `fun pickerMillis(value: LocalDateTime): Long` = `value.toEpochSecond(ZoneOffset.UTC) * 1000` and `fun fromPickerMillis(millis: Long): LocalDateTime` (UTC, truncated to minutes). The phone's zone is never involved.
9. `fun instant(value: String, zone: String): Instant?` — `null` when `date(value)` is null, when `zone` is not a valid IANA id (`ZoneId.of` throws `DateTimeException`; catch that type only), or when `zoneId.rules.getValidOffsets(ldt).isEmpty()` (DST gap). In an overlap use the **earlier** offset: `ZonedDateTime.ofLocal(ldt, zoneId, null).toInstant()`. Never `atZone` for validation. **[F]** iOS `instant` rejects gaps by round-tripping the components (`WizardDateTime.swift:58-67`).
10. `fun localDate(instant: Instant, zone: String): LocalDateTime?` — wall time of `instant` in `zone`, truncated to minutes; `null` on an invalid zone.
11. `fun parseInstant(value: String): Instant?` — accepts `yyyy-MM-ddTHH:mm:ss` with optional `.` + 1–9 digits, then `Z` or `±HH:MM`; additionally the 17-character `yyyy-MM-ddTHH:mmZ` form (append `:00`, iOS `WizardDateTime.swift:109-111`). Anything else → `null`. Hand-parse: match `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,9})?(Z|[+-]\d{2}:\d{2})$`, then `OffsetDateTime.parse(value).toInstant()` inside `try { } catch (e: DateTimeParseException) { null }`. No `DateTimeFormatter` reference (acceptance 5 greps for it); never throws.
12. `fun formatWeekdayTime(value: String?, language: Language, twelveHour: Boolean = false): String?` — `null` → `null`; invalid → `value` unchanged. 24 h: `"<EEE> HH:mm"`. 12 h English: `"<EEE> h:mm AM|PM"`; 12 h Dutch: `"<EEE> hh:mm a.m.|p.m."`. Hour 0 → 12. **[F]** goldens `WizardPortTests.swift:59-84`.
13. `fun format(date: LocalDateTime, template: WizardDateTemplate, language: Language): String` with `enum class WizardDateTemplate { EEE_D_MMM, EEE, EEE_D_MMM_Y }`, and an `Instant` overload that takes a `zone: String` and converts first. **[A]** outputs (macOS 27.2, `setLocalizedDateFormatFromTemplate`):

    | Template | en_GB | nl_NL |
    |---|---|---|
    | `EEE d MMM` | `Mon 12 Oct` | `ma 12 okt` |
    | `EEE` | `Mon` | `ma` |
    | `EEE d MMM y` | `Mon, 12 Oct 2026` (**comma**) | `ma 12 okt 2026` |

    Tables (in `WizardCalendarText.kt`, ISO Monday = 1): en weekdays `Mon Tue Wed Thu Fri Sat Sun`, months `Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec`; nl weekdays `ma di wo do vr za zo`, months `jan feb mrt apr mei jun jul aug sep okt nov dec`. Day without padding. `Language.Unknown` → en.
14. `formatLocal` is **not** ported (OQ-9a-7).

**Edge cases** — a wall time inside the *phone's* DST gap (`2027-03-28T02:30`) is valid for `date()` and for another airport (`Asia/Tokyo`) and invalid only for `instant(…, "Europe/Amsterdam")`; adding a day to a picker value keeps `02:30`.

**Tests**
- `T/model/WizardDateTimeTests` (Swift names; 5 of 6, `combinedPillUsesAirportWallComponentsAndTheLocalesClock` is not ported, OQ-9a-7):
  - `leapDaysAndMonths` — the two `shiftDate` cases (the `formatLocal` assertion is dropped with a one-line comment naming OQ-9a-7);
  - `rejectsInvalid` — JUnit `Parameterized` over the 7 values above (one case each);
  - `pickerPreservesMinutesAcrossDeviceDSTChanges` — parameterized over `2026-11-01T10:07`, `2027-01-01T00:00`, `2027-03-28T02:30`, `2026-10-25T02:30`: `pickedValue(pickerDate(v)!!) == v` and `pickedValue(fromPickerMillis(pickerMillis(pickerDate(v)!!))) == v`, each repeated with `TimeZone.setDefault` set to `Europe/Amsterdam`, `America/New_York`, `Asia/Tokyo` (restore the default in `finally`);
  - `actualPhoneGapIsValidAtAnotherAirport` — the assertions of `WizardPortTests.swift:42-57` with `pickerDate(v)!!.plusDays(1)` for the calendar step;
  - `weekdayAndTimeRespectLocaleAndClock` — all 11 table rows of `WizardPortTests.swift:70-80` with en_GB → `Language.En`, nl_NL → `Language.Nl` (0d names enum cases in PascalCase, 0d §4), plus `formatWeekdayTime("invalid", Language.En) == "invalid"`.
- `T/model/WizardDateTimeFormatTest` (Android): the [A] table above for 2026-03-02, 2026-05-04, 2026-09-07, 2026-10-12 in en and nl; `instant("2026-10-25T02:30", "Europe/Amsterdam")` equals `2026-10-25T00:30:00Z` (earlier offset); `instant(…, "Not/AZone") == null`; `parseInstant` accepts `2026-11-07T22:00Z`, `2026-11-07T22:00:00Z`, `2026-11-07T22:00:00.123+01:00` and rejects `2026-11-07T22:00+01:00`, `2026-11-07`, `""`; with `Locale.setDefault(Locale.forLanguageTag("ar-EG"))` and `TimeZone.setDefault("Pacific/Kiritimati")` (restored after) two of the table outputs are unchanged.

### Task 3. Form, route editing and the input mapping

Commit: `feat(wizard): port the wizard form, route rules and input mapping`. Ticks nothing yet.

**Files** `W/model/WizardForm.kt`, `W/model/WizardRoute.kt`, `W/model/WizardJourney.kt` (mapping half).

**Behaviour**
1. Types (`WizardForm.swift:4-84`), all `@Serializable data class`, defaults exactly as iOS:
   - `WizardLeg(id: String = UUID.randomUUID().toString(), fromIata = "", fromCity: String? = null, toIata = "", toCity: String? = null, departLocal = "", arriveLocal = "", flightNumber: String? = null, seatLieFlat = false, seatWindow: String? = null, leaveForAirportLocal: String? = null, airportTransferMin: Int? = null)`.
   - `WizardBusy(id, startLocal, endLocal)` (+ `val input: ApiTripInputBusy`).
   - `WizardStay(bedAvailableMode: String? = "arrival", bedAvailableFromLocal, checkOutLocal, lateCheckOutOrDayRoom = false, arrivalTransferMin, transferMode, busyIntervals = emptyList())` — check every default against l.35–45.
   - `WizardEnd(kind = "one_way", nights: Int? = null, checkOutDate: String? = null, returnFromLeg: Int? = null)`.
   - `WizardSleepOverride(enabled = false, habitualBed, habitualWake, chronotype)`; `WizardPreparation(days = 3, fastDelayOptIn = false, latestWake, feasibleNightBed, feasibleNightWake, busyAnswered = false, busyIntervals = emptyList())`.
   - `WizardForm(legs = listOf(WizardLeg()), stays = listOf(WizardStay()), end, override, preparation, identity: ApiReviewIdentity? = null, itinerary: ApiReviewItinerary? = null)` with `companion { const val maxFlights = 16; const val maxPreparationDays = 4 }` and `val returnBoundary = if (end.kind == "return_legs") end.returnFromLeg ?: legs.size else legs.size`.
2. Operations, verbatim from `WizardForm.swift:86-198` and `WizardRoute.swift` (06 §5.2): `updateLeg` (editing the last outbound leg's To also moves the first home leg's From; **any** update clears `itinerary`), `addFlight(home)`, `takeFlight`/`restoreFlight`, `addReturn(retained)`/`removeReturn()` (restores `nights`/`checkOutDate` when present), `insertStop(airport, beforeDestinationOf)` (leg i's To becomes the stop and its `arriveLocal` is cleared; a new empty-time leg stop→old To; a new stay at i; `returnFromLeg += 1` when the leg is outbound), `removeStop(at)` (i > 0 and not the return boundary; leg i−1 takes leg i's To and `arriveLocal`; leg i and **stay i−1** are removed, `WizardForm.swift:187-198`), `route(home)`, `routeComplete(home)` (≥ 2 airports, none empty, first ≠ last, no adjacent equal), `setRouteAirport(airport, at, home)`, `makeDirect(home)` (removes stop 1 until direct). The 16-flight limit: `addFlight`, `insertStop` and `restoreFlight` return the form unchanged at 16 legs; `addReturn` returns it unchanged when `legs.size + (retained?.legs?.size ?: 1) > 16`.
3. Mapping (`WizardJourney.swift:4-109`):
   - `fun WizardForm.reviewInput(identity: ApiReviewIdentity?, profile: JsonObject? = null): ApiTripReviewInput` — legs before `returnBoundary` → `outbound`, the rest → `return` (null when none); each leg's `stay` is the stay at the same index; the fields of `06 §5.1`. Encoding with `ApiJson.request` gives exactly `wizard/review-request.json` for the fixture input.
   - `WizardForm.from(input: ApiTripInput, airports: Map<String, ApiAirportSuggestion> = emptyMap(), retained: WizardForm? = null): WizardForm` — throws `DataFailure.Server(null)` when `tripEndKind ∉ {return_legs, nights, check_out, one_way}`, `override.chronotype ∉ {early, intermediate, late}`, `seatWindow ∉ {left, right}`, `bedAvailableMode ∉ {arrival, declared}`, `transferMode ∉ {passenger, self_driven, unknown}` (null always allowed). Ids: retained id or `leg-N` / `busy-N`. Cities: `airports[iata]?.city ?: retained city`. `identity`/`itinerary` come from `retained`.
   - `WizardForm.from(reviewInput: ApiTripReviewInput, retained: WizardForm? = null)` (sets `identity = reviewInput.identity`) and `WizardForm.from(snapshot: ApiTripInputSnapshot)` (uses `snapshot.airports` for cities).

**Edge cases** — `removeStop(0)` and removing at the boundary are no-ops; `insertStop` on a home leg does not move `returnFromLeg`; `restoreFlight` after a later edit keeps that edit (`WizardFormTests.removeRestoreKeepsStayBoundaryAndLaterChanges`).

**Tests**
- `T/model/WizardFormTests` (Swift names, all 5): `independentDefaults`, `removeRestoreKeepsStayBoundaryAndLaterChanges`, `returnRetentionLimitsAndOutboundPropagation`, `stopSplitsMiddleTimesAndRemovalJoinsEndpoints`, `roundTripsAllFieldsWithoutLeakingLocalMetadata`.
- `T/model/WizardRouteTests` (pure 3 of 8; the class grows in Tasks 4, 5 and 7): `splitsLastLegAndJoinsOnlyTheRemovedStopsFlights`, `returnStopsKeepBoundaryAndDirectDoesNotTouchTheOtherRoute`, `editingAStopDerivesBothAdjacentEndpointsAndHonorsTheTotalLimit`.
- `T/model/WizardInputMappingTest` (Android): `reviewInputMatchesTheRequestFixture` — `WizardForm.from(generationInput()).reviewInput(null, review-request.json["profile"])` encoded with `ApiJson.request` equals `wizard/review-request.json` as a `JsonObject`; `snapshotFormHasCities` — `from(snapshot.json).legs[0].fromCity == "Amsterdam"`; `unknownEnumValueIsAServerFailure` — `tripEndKind = "forever"` throws `DataFailure.Server`.

### Task 4. Validation, picker bounds and flight rules

Commit: `feat(wizard): port client validation, picker bounds and flight rules`. Ticks nothing yet.

**Files** `W/model/WizardValidation.kt`, `W/model/WizardFlightTiming.kt`, `W/model/WizardOutboundRules.kt` (`object WizardOutboundRules`, `object WizardFlightRules`), `W/model/WizardJourney.kt` (error half).

**Behaviour**
1. `WizardValidation.validate(form, airports = emptyMap()): Map<String, FieldMessage>` — every row of `06 §6.1` (l.612–638), keys verbatim (`"The planner cannot use this value."`, `"Choose the airport you leave from."`, …). Paths are camelCase form paths (`legs.0.departLocal`, `stays.1.bedAvailableFromLocal`, `end.nights`, `preparation.latestWake`). `validationErrors(fields, form)` and `errorSection(path, form)` (section labels of `06 §6.1` last paragraph) as `WizardValidation.swift:104-138`.
2. `WizardFlightTiming(form, airports)` with `sealed`/data `Field(index: Int, isArrival: Boolean)` (`departure(i)`, `arrival(i)`), `position = index * 2 + (if (isArrival) 1 else 0)`; `minimumLocal(field): LocalDateTime?` = the immediately preceding field's instant converted to this field's airport zone (null if the predecessor is unset, its zone is missing, or it sits in a DST gap); `initialLocal(field, now: Instant): LocalDateTime` = stored value, else the nearest earlier set field converted, else airport-local `now` truncated to the minute and clamped to the minimum (when the field's zone is missing, `now` as a UTC wall time, untruncated, `WizardFlightTiming.swift:54`); `instant(field): Instant?`. No upper bound. Stored values below a new bound are kept (06 §5.4).
3. `WizardOutboundRules.errors(form, airports, now: Instant, checkPast: Boolean)`, `connection(form, index, airports, duration: (Int) -> String): FieldMessage?` (itinerary facts first; else local facts: complete 23:00–07:00 nights in the connection airport's zone; keys, verbatim from `WizardOutboundRules.swift:30-109`: `"Connection in {{airport}} · {{duration}}"` (nights 0; `duration` = `duration(minutes)`), `"Stop in {{airport}} · {{count}} complete night"` when nights == 1, `"Stop in {{airport}} · {{count}} complete nights"` otherwise (the model picks the singular key itself; `count` is a JSON number), and `"Stop in {{airport}}"` when no fact can be computed), `flightMinutes(form, index, airports): Int?`.
4. `WizardFlightRules.errors(form, airports, now, checkPast, mode: WizardMode = WizardMode.member)` (`WizardOutboundRules.swift:111-137`): validation + chronology over all legs; with `checkPast`, a last arrival not after `now` gives `review` = `"A new plan needs an upcoming arrival. Check your flight dates or add your return flight."` for a one-way member, and `"A new plan needs an upcoming arrival. Check your flight dates."` for a guest or with a return.
5. `WizardJourney.errorStep(errors, form): MemberWizardStep`, `errors(fields, form)` (server snake paths → form paths via `topFields`, `camel`, `errorPath`; moves `"Your flight home must leave from the airport you landed at."` to `review`), exactly `WizardJourney.swift:111-169`. `MemberWizardStep` and `WizardMode` are declared here in their final forms (Task 5 moves nothing): `enum class WizardMode { member, guest }` in `W/model/WizardMode.kt`; `enum class MemberWizardStep { whereTo, outbound, returnChoice, returnRoute, returnFlight, sleep, checking, question, excluded, continuation, fineTune, ready, calculating }` in `W/model/MemberWizardState.kt`.

**Edge cases** — airports with different wall times never establish chronology through wall strings (always instants); a leave-for-airport time is not compared with an invalid departure; a missing zone yields no chronology error (it cannot be computed), not a false one.

**Tests**
- `T/model/WizardValidationTests` (all 6): `blankErrorsMatchRN`, `airportCountsSleepBusyAndLimits`, `differentAirportWallTimesDoNotEstablishChronology`, `leaveTimeDoesNotCompareAgainstInvalidDeparture`, `leaveAndConnectionOrderAndContinuity`, `errorMappingAndSections`.
- `T/model/WizardFlightTimingTests` (8 of 9; the ninth lands in Task 7): `arrivalBoundsAndEmptyStartConvertInstants` (parameterized as iOS), `boundsChainThroughOutboundAndEveryHomeFlight`, `unsetPredecessorHasNoBoundButStartFindsNearestEarlierSetField`, `noEarlierValuesUsesAirportLocalNowToTheMinuteAndIgnoresLaterValues`, `destinationDSTOffsetComesFromThePredecessorInstant` (parameterized), `sourceDSTResolutionMatchesExistingValidation` (parameterized), `nonexistentDSTSourceOrMissingZoneCannotEstablishABound`, `connectionValidationComparesInstantsThroughEachAirportZone`.
- `WizardRouteTests` + `mixedServerErrorsReturnToTheEarliestScreenInPathOrder`, `laterCardOwnsChronologyErrorAndServerFieldsReturnToTheirEditors`.
- `T/model/WizardReviewFlowTests` (1 of 19 here; the rest are 9b's): `returnPastUsesFinalArrivalAndAirportZones`.

### Task 5. State, recovery record, entries and generation value types

Commit: `feat(wizard): add wizard state, the recovery record and trip entries`. Ticks nothing yet.

**Files** `W/model/MemberWizardState.kt`, `W/model/WizardFingerprint.kt`, `W/model/MemberWizardRecord.kt`, `W/model/MemberWizardDrafts.kt`, `W/model/TripWizardActions.kt`, `W/model/WizardPresentation.kt` (the `SourceChange` type only), `W/model/WizardSaveFailure.kt`, `W/generation/TripGenerationTypes.kt`.

**Behaviour**
1. In `MemberWizardState.kt` (`MemberWizardStore.swift:6-112`):
   - `WizardMode` was declared in Task 4 so its flight-rule default compiles in that commit; `WizardAirportSheet(index: Int, side: Side, replacingStop: Boolean? = null)` with `enum class Side { from, to, home, stop }` and `val id = "$index-${side.name}-${replacingStop == true}"`.
   - `MemberWizardState` with every field of l.36–68 under the same names and defaults (`instance = UUID.randomUUID().toString()`, `revision = 0`, `step = whereTo`, `entered = false`, `fromLastTrip = false`, `firstTrip = true`, `sleepNeeded = true`, `homeChanged = false`, `profile = JsonObject(emptyMap())`, `airports = emptyMap()`, the rest `null`). Types per §5.1: `checkedInput`, `revisit`, `originalInput`: `String?`; `reviewedAt: Instant?` (serialized as epoch millis via an internal `InstantMillisSerializer`); `progressFloor: Double?`. `mode` is serialized under `"mode"`; a missing key decodes as `member` (default value).
   - `fun discardRetiredValues(): MemberWizardState` — `profile = ProfilePreferences.request(profile)`, `savedProfile = savedProfile?.let(ProfilePreferences::bodyClock)`, `questions`/`review.prompts` through `ProfilePreferences.reviewPrompts`.
   - `MemberWizardDraft(form, state, fieldErrors: Map<String, FieldMessage> = emptyMap())` with `val airports get() = state.airports`.
   - `interface WizardDraftStore { suspend fun persist(draft: MemberWizardDraft); suspend fun discard(instance: String); suspend fun didSave(instance: String, firstTrip: Boolean) }`.
2. `WizardSaveFailure(kind: Kind, retryAt: Instant? = null)` with `enum class Kind { offline, server, throttle }` and `fun seconds(at: Instant): Int` = ceil of the remaining seconds, 0 when none or ≤ 0 (`MemberWizardCompletion.swift:4-19`); `enum class WizardSaveOutcome { signUpRequested }`.
3. `WizardPresentation.SourceChange` (`WizardPresentation.swift:4-20`): `@Serializable`, fields and `Field` enum (`route, flight, returnFlight, sleep, extras`) as iOS.
4. Generation value types (§2.2): `TripEditContext(tripID, sourceRevision)` (`@Serializable`), `TripGenerationReviewResult(input, review, edit = null, sourceSnapshot = null)`, `TripGenerationState(instance, input, edit = null, review = null, savedTripId = null)` (its factory sets `input.identity = review.identity` when `review != null`, as the Swift init), `TripGenerationSaveResult(state, outcome)` with `sealed interface Outcome { Saved(tripId), ReReviewNeeded(reason), Failed(failure) }` and `enum class Reason { reviewExpired, notEnoughCredits }`.
5. `WizardFingerprint.canonical(input: ApiTripReviewInput): String` — encode with `ApiJson.request` to a `JsonElement`, sort object keys recursively, print compactly. `MemberWizardDraft.inputFingerprint` = canonical of `form.reviewInput(identity = null, profile = state.profile.takeIf { it.isNotEmpty() })`; `hasChanges` = `if (state.edit != null) state.originalInput != inputFingerprint else state.entered` (`TripWizardActions.swift:29-40`).
6. `TripWizardActions.kt`: `enum class TripWizardAction { edit, duplicate }`; `WizardForm.clearDuplicateDates()` (l.7–27); `MemberWizardDraft.loadTrip(snapshot, id, action)` (l.42–73) with **one Android change**: edit sets `progressFloor = 1.0` instead of 0.95 (OQ-9a-3).
7. `object MemberWizardDrafts` (pure halves of the iOS stores):
   - `memberInitial(hasTrip: Boolean, latestOrigin: AirportRef?, savedProfile: JsonObject?, sleepAnswered: Boolean?): MemberWizardDraft` — `MemberWizardStore.swift:190-216`: `firstTrip = !hasTrip` (iOS: no undeleted trip exists; a trip whose card fails to decode still makes `firstTrip = false` with no origin, l.193–194); with an origin (the latest trip card's `from`), leg 0 From = its iata/city, `airports[iata] = ApiAirportSuggestion(iata, name = city ?: iata, city, country = "", timezone = zone)`, `fromLastTrip = true`; `savedProfile` stored; `sleepNeeded = sleepAnswered != true`; when `sleepNeeded`, copy non-null `habitualBed`/`habitualWake`/`chronotype` from `savedProfile` into `profile` as `habitual_bed`/`habitual_wake`/`chronotype`.
   - `seeded(seed: MemberWizardDraft, initial: MemberWizardDraft): MemberWizardDraft` — l.164–177 (`form = seed.form`, `airports = seed.airports`, identity cleared, override default, `entered`, `fromLastTrip = false`, `step = checking`, `returnToReady = true`, empty profile).
   - `guest(): MemberWizardDraft` — `GuestWizardStore.swift:25-29`: `mode = guest`, `lastInputStep = sleep`, `firstTrip = false`, `profile = {"habitual_bed":"23:00","habitual_wake":"07:00"}`.
8. `object MemberWizardRecord` (the codec 9b's DB row and 11a's guest file use):
   - `const val ID = "member-wizard"`; `const val EXPIRY_MILLIS = 1_800_000L`; `fun isExpired(updatedAtMillis: Long, nowMillis: Long) = nowMillis - updatedAtMillis >= EXPIRY_MILLIS`.
   - `fun encode(draft): Payload(form: String, state: String, fieldErrors: String)` with `ApiJson.response`. Before encoding: `discardRetiredValues()`, and **strip `guestSecret`** from `form.identity` and `state.review?.identity` in every mode (secrets never reach the DB; iOS does this for guests at `GuestWizardStore.swift:117-118`).
   - `fun decode(payload): MemberWizardDraft?` — `null` on any `SerializationException`/`IllegalArgumentException` or when legs ∉ 1…16, `stays.size != legs.size`, or `returnBoundary ∉ 1…legs.size` (the caller deletes the row). Then `discardRetiredValues()` and the migrations of `MemberWizardStore.swift:143-146`: `continuation` → `fineTune` when `state.review` has any non-blocking prompt, else `ready`; `returnFlight` with `returnRouteConfirmed != true` → `returnRoute`.

**Edge cases** — a record whose `profile` holds `pregnancy` decodes without it; an unknown prompt kind in a stored review survives as `Unknown(raw)` and is filtered out by `reviewPrompts`; `JsonNull` profile values (cleared answers) survive the round trip.

**Tests**
- `T/model/GuestWizardModeTests` (1 of 10 here): `legacyStateDecodesAsMemberAndModeRoundTrips` — encode a default state, remove `"mode"` from the `JsonObject`, decode → `member`; a guest state round-trips equal.
- `T/model/TripWizardActionsTests` (1 of 5 here): `duplicateClearsEveryDateBoundAnswerAndRetainsRouteAndOtherAnswers`, parameterized over `return_legs`, `nights`, `check_out`.
- `WizardRouteTests` + `recoveryDecodesOldReturnFlightAndPreservesConfirmedTimes` (parameterized `false`/`true`), adapted: build the draft with `MemberWizardDrafts.memberInitial(false, null, null, null)`, round-trip it through `MemberWizardRecord.encode`/`decode` instead of the store; assert the step migration and `form` equality.
- `T/model/MemberWizardRecordTests` (Android):
  - `fullStateRoundTrips` — a draft holding `generationReview()`, airports, a `SourceChange`, a `WizardSaveFailure(throttle, retryAt)`, field errors and `reviewedAt` decodes equal;
  - `expiresAtExactlyThirtyMinutes` — parameterized ages 0, 1_799_000, 1_799_999 → not expired; 1_800_000, 1_801_000 → expired;
  - `invalidShapesDecodeAsNull` — 0 legs, 17 legs, stays ≠ legs, boundary 0, malformed JSON;
  - `continuationBecomesFineTuneOrReady` — with and without a non-blocking prompt;
  - `retiredValuesAndSecretsNeverReachThePayload` — `profile.pregnancy`, `savedProfile.bipolar`, `form.identity.guestSecret`, `review.identity.guestSecret` are absent from the encoded strings.
- `T/model/MemberWizardDraftsTest` (Android): `memberInitialPrefillsOriginAndSleep`, `sleepAnsweredSkipsPrefill`, `guestDefaults`, `seededStartsCheckingWithoutIdentity`, `editLoadStartsCheckingWithFullProgress` (`loadTrip(snapshot, "trip", edit)`: step `checking`, `progressFloor == 1.0`, `returnToReady == true`, `originalInput == inputFingerprint`, `hasChanges == false`), `duplicateLoadStartsOnTimes` (step `outbound`, `entered`, `copiedRoute == "Amsterdam → HND"`: `wizard/snapshot.json` lists only `AMS` in `airports`, so the destination falls back to its IATA code, `TripWizardActions.swift:52-55`).

### Task 6. Checking, the generation interface and prompt answers

Commit: `feat(wizard): add the checking run and the generation interface`. Ticks nothing yet.

**Files** `W/model/WizardChecking.kt`, `W/generation/MemberWizardGeneration.kt`, `W/generation/TripGenerationAnswers.kt`, `T/model/CheckingTestClock.kt`.

**Behaviour**
1. `class WizardCheckingClock(val nowMillis: () -> Long, val sleepUntilMillis: suspend (Long) -> Unit)` with `companion object { val continuous: WizardCheckingClock }` built on one `TimeSource.Monotonic.markNow()` origin (`nowMillis` = elapsed ms; `sleepUntilMillis(d)` = `delay((d - nowMillis()).coerceAtLeast(0))`). [R] Long milliseconds instead of iOS `TimeInterval` seconds (OQ-9a-12).
2. `data class WizardCheckingProgress(ticks = 0, slow = false, failure: DataFailure? = null, retryAt: Long? = null)` with `fun retrySeconds(atMillis: Long): Int` = `ceil(max(0, (retryAt ?: at) - at) / 1000.0)`.
3. `object WizardChecking` with `sealed interface Event { Tick(count), Slow, Complete(result), Failed(failure) }` and `suspend fun run(clock, request: suspend () -> TripGenerationReviewResult, receive: (Event) -> Unit)`. Constants (`internal const val`): `TICK_1_MS = 400`, `TICK_2_MS = 800`, `MINIMUM_MS = 1200`, `PAUSE_MS = 300`, `SLOW_MS = 10_000`, `TIMEOUT_MS = 30_000`. Port of `WizardChecking.swift:40-75`:
   - `val start = clock.nowMillis()`; inside `coroutineScope`, launch two children and a `CompletableDeferred<Unit>` completed from each child's `invokeOnCompletion`; await it, then `coroutineContext.cancelChildren()`.
   - Response child: `request()`; `sleepUntil(start + 1200)`; `ensureActive()`; `receive(Tick(3))`; `sleepUntil(now + 300)`; `ensureActive()`; `receive(Complete(response))`. On a `DataFailure` → `receive(Failed(it))`. On `CancellationException`: call `currentCoroutineContext().ensureActive()` (rethrows when **this** run is cancelled) and otherwise `receive(Failed(DataFailure.Server(null)))` — a request that cancels itself while the run is live is a server failure (iOS test `requestCancellationWhileCheckingIsActiveShowsServerFailure`). Any other exception → `Failed(Server(null))`.
   - Timer child: for `(400, Tick(1)), (800, Tick(2)), (10_000, Slow)`: `sleepUntil(start + t)`, `ensureActive()`, `receive(event)`; then `sleepUntil(start + 30_000)`, `ensureActive()`, `receive(Failed(Server(null)))`.
   - After the run returns, no further `receive` call happens and no sleeper is left.
4. `interface MemberWizardGeneration` (`MemberWizardModel.swift:5-28`): `suspend fun review(instance: String, input: ApiTripReviewInput, edit: TripEditContext? = null): TripGenerationReviewResult`, `suspend fun save(state: TripGenerationState, didCreate: suspend (String) -> Unit): TripGenerationSaveResult`, `suspend fun cancelReviews(instance: String)`, `suspend fun cancel()`.
5. `object TripGenerationAnswers { fun applyingAnswers(input: ApiTripReviewInput, review: ApiTripReview, answers: Map<String, Map<String, JsonElement>>): ApiTripReviewInput }` — `TripGenerationAnswers.swift:13-70`: unknown prompt id, unsupported prompt, a field outside `prompt.fields`, or target `Unknown` → `DataFailure.Validation(mapOf("review" to FieldMessage("Choose one of the available options.")), 422)`; values go through `ProfilePreferences.promptAnswers`; `Profile` target → `profile[field]`; `Trip` target → JSON path `field.split(".")`, prefixed with `[journey, "legs", index]` when the scope has a leg or stay index and a journey; a path through a missing array index → `DataFailure.ReviewExpired`. Work on `ApiJson.request.encodeToJsonElement(input.input)` and decode back with `ApiJson.response`. Finally `profile = profile?.let(ProfilePreferences::request)`, `identity = review.identity`. [R] iOS hangs this off `TripGenerationService`; 9b's service calls this object.

**Tests**
- `T/model/CheckingTestClock.kt` (test support, port of `WizardCheckingTests.swift:100-141`): wraps the `TestCoroutineScheduler`; `clock` (`nowMillis = { scheduler.currentTime }`, `sleepUntilMillis` records the deadline in a list, `delay`s until it, removes it in `finally`); `val now`, `val waiterCount`, `val deadlines`; `fun advance(toMillis: Long)` = `advanceTimeBy(to - currentTime)` then `runCurrent()`. iOS `eventually {}` becomes `runCurrent()` followed by the assertion.
- `T/model/WizardCheckingTests` (all 4, times in ms): `ticksMinimumAndFinalPauseUseInjectedClock` (399 → no tick; 400 → [1]; 800 → [1,2]; 1199 → not complete; 1200 → [1,2,3] and a deadline at 1500; 1500 → complete; `waiterCount == 0`), `slowAtTenSecondsAndTimeoutIsServerFailureAtThirty`, `cancellationDropsLateResponseAndTimers`, `requestCancellationWhileCheckingIsActiveShowsServerFailure`.
- `T/generation/TripGenerationAnswersTest` (Android): profile answer lands in `profile` and passes `ProfilePreferences.request`; trip answer on a leg-scoped prompt writes `outbound.legs.0.stay.<field>`; foreign field → `Validation` with key `review`; missing leg index → `ReviewExpired`; identity is the review's.

### Task 7. The member wizard model

Commit: `feat(wizard): port the member wizard model`. Ticks plan.md "**Model**" (l.2615) and "**No-health-data rules.**" (l.2623).

**Files** `W/model/MemberWizardModel.kt` (class), and extension-function files mirroring the Swift extensions: `MemberWizardNavigation.kt`, `MemberWizardReview.kt`, `MemberWizardRetry.kt`, `MemberWizardCompletion.kt`, `WizardMode.kt`, plus `acceptSourceReload` and `checkingTitle` in `TripWizardActions.kt`. Members the extensions touch are `internal`.

**Behaviour**
1. Constructor: `MemberWizardModel(draft: MemberWizardDraft, store: WizardDraftStore, scope: CoroutineScope, now: () -> Instant, generation: MemberWizardGeneration? = null, checkingClock: WizardCheckingClock = WizardCheckingClock.continuous)`. Init: `draft.state.discardRetiredValues()`, `enforceGuestMode()`, `restartRetryCountdown()` (`MemberWizardModel.swift:68-84`).
2. Observable state: one `MutableStateFlow<MemberWizardSnapshot>` with `data class MemberWizardSnapshot(draft, closed, persistenceFailed, checking, retrySeconds, choicePending, reviewing, createdTripID: String?, saveOutcome: WizardSaveOutcome?, guestAuthenticationPresented)`, exposed as `val snapshots: StateFlow<MemberWizardSnapshot>`. Each property also has a Kotlin property (`var draft: MemberWizardDraft` with `internal set`, and so on) that reads and `update`s the flow, so tests can write `model.draft = model.draft.copy(…)` as the Swift tests write `model.draft.state.x = …`.
3. Threading: the class is single-threaded like the iOS `@MainActor` class. Every public function is called on `scope`'s dispatcher (Main in 9b, the test dispatcher in tests). Jobs: `checkJob`, `reviewJob`, `saveJob`, `choiceJob`, `retryJob` (iOS `retryTask`), `pending` (persistence chain); ids `checkID`, `retryID` as `String` UUIDs; `checkInstance = "${state.instance}/$id"`.
4. Port every function of `MemberWizardModel.swift` l.86–271, `MemberWizardNavigation.swift`, `MemberWizardReview.swift`, `MemberWizardRetry.swift`, `MemberWizardCompletion.swift` l.43–300 and `WizardMode.swift`, keeping guards, ordering and names (`go`, `openAirport`, `chooseAirport`, `airportIsAllowed`, `swapAirports`, `setDateTime`, `removeStop`, `makeDirect`, `finishReturnRoute`, `finishOutbound`, `flush`, `setGuestAuthenticationPresented`, `setGuestSigningIn`, `discard`, `changed`, `persist`, `navigationPath`, `popNavigation`, `lastInputStep`, `returnComplete`, `sleepComplete`, `currentQuestion`, `progress`, `chooseReturn`, `finishReturn`, `setSleep`, `finishSleep`, `finishFlightInput`, `beginChecking`, `resumeChecking`, `cancelCheck`, `applyReview`, `applyFailure`, `answerQuestion`, `navigateToQuestion`, `backFromQuestion`, `exclusionWasSaved`, `changeExcludedAnswers`, `updateRetrySeconds`, `restartRetryCountdown`, `reviewInput`, `inputFingerprint`, `optionalPrompts`, `needsReadyReview`, `canSave`, `preference`, `setPreference`, `editForm`, `openPrompt`, `answerOptional`, `revisit`, `finishRoute`, `finishFineTune`, `readyTick`, `resumeCompletion`, `cancelInlineReview`, `savePlan`, `guestAnsweredQuestionCount`, `revisitGuestQuestions`, `guestScreeningQuestions`, `mode`, `showsReturnRow`, `allowsSleepOverride`, `guestReviewExpired`, `finishOutboundStep`, `recheckGuestReady`, `minimumLocal(field)`, `initialLocal(field)`). Swift `editForm(_ update: (inout WizardForm) -> Void)` becomes `editForm(update: (WizardForm) -> WizardForm)`.
5. Key values (all [F] at the cited lines):
   - choice delay **250 ms** on `checkingClock` (`MemberWizardReview.swift:329-341`); taps while `choicePending` are ignored;
   - sleep duration `(wake − bed + 1440) % 1440` ∈ **180…840** min, else `fieldErrors["profile.habitual_bed"] = FieldMessage("Check your bedtime and wake-up time.")` (l.90–104);
   - Ready expiry **1800 s** on `now()` (`MemberWizardCompletion.swift:63-72`), i.e. `Duration.between(reviewedAt, now()) >= 1_800_000 ms`;
   - throttle: checking `retryAt = checkingClock.nowMillis() + (retryAfterSeconds * 1000).roundToLong()` when finite and > 0; save `retryAt = now() + that`; countdown ticks every `min(1000, remaining)` ms on `checkingClock` (`MemberWizardRetry.swift`);
   - `persist()` increments `state.revision`, then chains `pending = scope.launch { previous?.join(); store.persist(value) }`; a non-cancellation exception sets `persistenceFailed = true` (rethrow `CancellationException`).
6. **No health data.** `answerQuestion` accepts only a single-field profile prompt that is `Screening` + `shift_work` + boolean shape + `true`/`false`, or `AgeBand` + `age_band` + a value in `shape.options`; anything else is ignored. `questions` = blocking prompts that pass `ProfilePreferences.isSupported`; `needs_answers` with none → `applyFailure(Server(null))`. `preference`/`setPreference` accept only `exclude_caffeine` and `flight_movement_breaks` (default false from `profile` or `savedProfile` camel keys); any other field is ignored. Retired keys are dropped by `discardRetiredValues` on init and by `ProfilePreferences.request` on every review.
7. Progress in this task is the iOS table (`MemberWizardReview.swift:28-43`, `WizardMode.swift:72-84`); Task 8 replaces it with the board values. `navigationPath`/`popNavigation` are the iOS versions; Task 8 adds the board's first screens.

**Edge cases** — a late review response after `cancelCheck`, `go(...)` or `discard` is dropped (`checkID` guard); `discard` during `calculating` is a no-op; a save never retries without a user action; `savePlan(recovering = true)` re-runs with the stored `savedTripId`; a stale `popNavigation` (presented ≠ current path) is ignored; `beginChecking` while a check runs is ignored.

**Tests** (fixtures: `InMemoryWizardDraftStore` in `T/model/InMemoryWizardDraftStore.kt` — records every persisted draft, `last`, `discarded` instances, `didSave` calls; `FakeWizardGeneration` in `T/model/FakeWizardGeneration.kt` — queued `Result<TripGenerationReviewResult>` and save outcomes, counts calls. iOS `store.recover()` assertions become `store.last` (or `MemberWizardRecord.decode(MemberWizardRecord.encode(store.last))` where iOS relies on the migration). Every test calls `model.flush()` before reading the store and `model.discard()` at the end. Use `runTest(timeout = 60.seconds)`, `scope = backgroundScope`, `CheckingTestClock(testScheduler)`, `WizardTestClock`.)
- `WizardRouteTests` + `returnChoiceStartsWithADirectRouteAndEmptyTimes`, `routeEditingLocksAndFollowsHomeAndBackKeepsStops`.
- `T/model/WizardHomeRouteTests`: `homeStopChoicesUseTheReturnBoundaryAndDirectPreservesOutbound`.
- `T/model/WizardAirportChoiceTests`: `searchAndSelectionRejectAdjacentDuplicatesInBothRoutes` (parameterized `false`/`true`), `directRouteStillAllowsTheDesignedSameAirportError`.
- `WizardFlightTimingTests` + `changedEarlierValuesKeepStoredTimesAndValidationFlagsThem`.
- `GuestWizardModeTests` + `questionsRowReopensSupportedAnswersAndReturnsToReady`, `readyWaitsForTheSignupSheetAndDismissalAllowsAnotherRequest`, `guestPathBackTargetsAndSleepAreMandatory`, `readyRechecksEveryInvalidation` (parameterized over the 7 reasons), `liveReadyRequestsSignUpWithoutSaving`, `exclusionResetKeepsSleepAndPreferences`, `guestPastArrivalCopyAndMemberNavigationStayDistinct`. The guest fixture is `MemberWizardDrafts.guest()` with `form = WizardForm.from(generationInput())`, `sleepCompleted = true` and, for `ready`, the review/identity/`reviewedAt`/step of `GuestWizardModeTests.swift:207-221`, plus `checkedInput = model.inputFingerprint` set on the model after construction (l.220).
- `WizardReviewFlowTests` + `neutralPreferencesPersistAndReachReviewWithoutMedicalKeys`, adapted: the fixture is a member draft at `fineTune` holding `generationReview()`; `setPreference("pregnancy", true)` changes nothing; the last persisted profile is exactly `{"exclude_caffeine":true,"flight_movement_breaks":true}`; `reviewInput.profile` equals it; `needsReadyReview` is true.
- `T/model/MemberWizardReviewTest` (Android, `FakeWizardGeneration`):
  - `reviewWithBlockingPromptsAsksThem` (`needs-answers.json` → step `question`, index 0);
  - `needsAnswersWithOnlyRetiredPromptsIsAServerFailure` (Android name; the iOS test `needsAnswersWithoutSupportedPromptsIsAServerFailure` is ported by 9b);
  - `noPlanGoesToExcluded`;
  - `answerAdvancesAfterTwoHundredFiftyMilliseconds` (249 ms → same step and `choicePending`; 250 ms → next);
  - `unsupportedAnswersAreIgnored` (`age_band = "42"`, `shift_work = "yes"`, a third prompt kind);
  - `throttledFailureCountsDownWithCeiling` (`Throttled(41.2)` → `retrySeconds == 42`; `beginChecking` ignored until 0);
  - `validationErrorsRouteToTheirStep` — `Validation` with **server** paths, as `applyFailure` maps them through `WizardJourney.errors` (`MemberWizardReview.swift:237-251`): `outbound.legs.0.stay.check_out_local` → `fineTune` with `fieldErrors` key `stays.0.checkOutLocal`; `profile.habitual_bed` → `sleep`; `outbound.legs.0.to_iata` → `whereTo` with key `legs.0.toIata`;
  - `saveIsNeverRetriedAutomatically` (`Failed(Offline)` → `ready` with `saveFailure.kind == offline`, `FakeWizardGeneration.saves == 1` after advancing 60 s).

### Task 8. Board progress values and first screens

Commit: `feat(wizard): use the board progress values and first screens`. Ticks plan.md "**Discrepancies.**" (l.2624).

**Files** `W/model/MemberWizardReview.kt`, `W/model/WizardMode.kt`, `W/model/MemberWizardNavigation.kt`.

**Behaviour**
1. Member `stepProgress` (board `pct`, OQ-A36 / `06` OQ 1; [F] board values: Where 15, WhereEmpty 8, Stop/Stops 15, Outbound 30, OutboundEmpty 22, Duplicate 22, Return 45, HomeRoute 50, ReturnFlight 55, Checking 60, Again 70, Excluded 60, FineTune 80, FineTuneNone 95 (via the floor), Ready 95, Calc 100):

   | Step | Value |
   |---|---|
   | `whereTo` | 0.15 when `whereComplete`, else 0.08 |
   | `outbound` | 0.30 when `outboundComplete`, else 0.22 |
   | `returnChoice` | 0.45 |
   | `returnRoute` | 0.50 |
   | `returnFlight` | 0.55 |
   | `sleep` | 0.50 (plan OQ-A36 / `06` OQ 1 default "sleep 50"; no member sleep board). After a home flight (0.55) this is lower; 9c's bar animates only upward (`06 §9`, "never downward"), so the drawn bar holds at 55. OQ-9a-2 |
   | `checking`, `excluded` | 0.70 when `checkingAnswers == true`, else 0.60 |
   | `question` | `0.60 + 0.10 * index / max(1, questions.size)` |
   | `continuation`, `fineTune` | 0.80 |
   | `ready` | 0.95 |
   | `calculating` | 1.0 |

2. **Edit:** while `state.edit != null`, `progress` is **1.0** on every step [R] (every edit board draws 100; iOS floors at 0.95; OQ-9a-3).
3. Guest `guestStepProgress` (board: Where 9, Outbound 18, Sleep 30, Checking 42, QAge 54, QShift 64, Excluded 60 (`WizardExcluded.dc.html` renders `guest: true, pct: 60`), FineTune 86, Ready 95): `whereTo` 0.09 (complete or not); `outbound` 0.18; `sleep` 0.30; `checking` 0.42, or 0.64 when `checkingAnswers == true` [R] (no board; the value of the last question so the bar holds, OQ-9a-4); `excluded` 0.60 (board), or 0.64 when `checkingAnswers == true` (OQ-9a-4); `question` 0.54 at index 0, 0.64 after; `fineTune` 0.86; `ready` 0.95; `calculating` 1.0; return steps (unreachable for guests) 0.18.
4. The floor rule is unchanged: `progress = max(progressFloor ?: 0, stepProgress)`; `go()` sets the floor to the current `progress` on entering `checking, question, excluded, continuation, fineTune, ready, calculating` and clears it otherwise.
5. First screens (`Wizard.dc.html:258-260`, `06` OQ 2) — Android additions, no Swift twin:
   - `val firstScreen: Boolean` = `step == whereTo` || (`copiedRoute != null && edit == null && step == outbound && revisit == null`) || (`edit != null && step == ready`) || (`edit != null && step == checking && checking.failure != null`).
   - `val showsBack: Boolean` = `!firstScreen && step != calculating && !(step == checking && checking.failure == null)`. The board hides back on `checking` and `calc` only; a member (non-edit) failure screen (`fail`, e.g. `WizardCheckOffline`) **shows** back (`Wizard.dc.html:260`), and the edit failure is already a first screen.
   - `popNavigation` also returns without effect when `firstScreen` (9c maps system back to × there).
   - `navigationPath` is unchanged (iOS).

**Tests**
- `GuestWizardModeTests` + `guestWeightsAndForwardProgressFloor`, adapted to the board values: route empty 0.09; route complete 0.09; then `outbound` 0.18, `sleep` 0.30, `checking` 0.42, `question` 0.54; index 1 → 0.64; `checkingAnswers` + `checking` → 0.64; `fineTune` 0.86; `ready` 0.95; `checking` again → 0.95 (floor). A comment names `06 §11.1`.
- `T/model/WizardProgressTest` (Android): `memberValuesMatchTheBoards` (each row above, including the question interpolation with 2 questions: 0.60, 0.65), `sleepIsFiftyWithOrWithoutAReturn`, `guestExcludedUsesTheBoardValue` (0.60; 0.64 with `checkingAnswers`), `fineTuneOpenedFromReadyStaysAtNinetyFive`, `editIsAlwaysFull` (edit draft on `outbound`, `checking`, `ready` → 1.0).
- `T/model/WizardNavigationTest` (Android): `whereIsAFirstScreen`, `duplicateTimesIsAFirstScreenAndCannotPop`, `revisitedTimesIsNotAFirstScreen`, `editReadyAndEditFailureAreFirstScreens`, `checkingAndCalculatingHideBack`, `memberCheckingFailureShowsBack`.

### Task 9. Credits, optional answers and presentation

Commit: `feat(wizard): port credits, optional answers and wizard presentation`. Ticks plan.md "**Ported tests.**" (l.2625).

**Files** `W/model/WizardCredits.kt`, `W/model/WizardOptionalAnswer.kt`, `W/model/WizardPresentation.kt` (functions), `val MemberWizardModel.credits` in `MemberWizardCompletion.kt`.

**Behaviour**
1. `enum class WizardCredits { unlimited, free, oneFree, credits, unavailable }` + `companion fun of(review: ApiTripReview)` exactly as `MemberWizardCompletion.swift:22-41`. Never a hard-coded credit count (OQ-A18).
2. `object WizardOptionalAnswer { fun error(prompt, values: Map<String, JsonElement>): String? }` (`WizardOptionalAnswer.swift`): `drinkFields = listOf("cups_filter_coffee", "cups_tea", "cups_cola", "cups_energy_drink")`; a `Cups` prompt whose `caffeine_habit` is not `"non_user"` needs at least one drink field holding a JSON number > 0 (`"Add at least one drink to save this answer."`); a `BlueBlockers` prompt with `blue_blockers == "rated"` needs `blue_blocker_mdfd` (number or string) that parses, with `,` or `.`, to a finite 0.1…9.9 (`"Enter a rating between 0.1 and 9.9."`); a `FreeDayTimes` prompt with `free_day_status == "declared"` needs `free_day_bed` and `free_day_wake` both `HH:mm` (`"Check your bedtime and wake-up time."`); otherwise `null`. Parse the number with `toDoubleOrNull()` after replacing `,` by `.` (never a locale-aware parser).
3. `WizardPresentation` (`WizardPresentation.swift:21-272`), with `typealias Translate = (key: String, values: Map<String, String>) -> String` and `language: Language` in place of `Locale`: `sourceChangeSummary`, `promptKey`, `promptAnswers`, `promptAnswerText`, `flightSummary`, `extrasSummary`, `returnSummary`, `planDateSummary` (the year template `EEE_D_MMM_Y` when the plan's start year in `planStartZone` differs from the end date's year; en output carries the comma, [A]), `planDayCount`, `promptFieldError`. Numbers (cups, ratings) are formatted without `String.format`.
4. Copy keys stay the iOS English keys; the model never resolves a string. Plural keys are passed with `count` and resolved by the UI from their `_one` pairs (`{{count}} credits`, `{{count}} time ranges`, `{{count}} a day`, `{{count}} days prep`, `{{count}} answered`, `{{count}} nights`; the iOS wizard catalog's `_one` keys). The connection night keys are the exception: the model itself picks `Stop in {{airport}} · {{count}} complete night` (1) or `… complete nights` (Task 4), as iOS does.

**Tests**
- `T/model/WizardCreditsTests` (both): `noPriceIsNeverFree`, `pricedReviews`.
- `T/model/WizardPresentationTests` (all 4): `planDatesCountAndSourceChangesUseThePlanStartZone`, `localFactsBeforeCheckingUseZonesAndCompleteNights`, `errorsPastConnectionsAndOvernightUseAirportClock`, `summariesAndPromptAnswersRetainScopeAndFallback` (en_GB → `Language.En`).
- `T/model/WizardCompletionTests` (2 of 12 here): `creditsCoverEveryReadyVariant`, `sheetValidationAcceptsCommaRatingAndRejectsMissingDrinks` (build the cups prompt with fields `caffeine_habit`, `cups_filter_coffee`, `cups_tea`, `cups_cola`, `cups_energy_drink` and the blue prompt with `blue_blockers`, `blue_blocker_mdfd` by hand; `MockWizardPrompts` is 4c's).
- `T/model/WizardCopyKeysTest` (Android): `everyModelKeyIsListed` — a regex scan of `core/wizard/src/main/kotlin` for `FieldMessage("…"`/`FieldMessage(key = "…"` and `t("…"`/`translate("…"` literals equals the hand-written set in `T/model/WizardModelCopyKeys.kt`; `everyListedKeyResolves` — each key is `"wizard\t<key>"` in `tools/l10n/names.lock.json`, or an `entries[].key` in `core/plan/src/main/resources/plan_copy.json` (the two server question texts), except `"{{flight}} · via {{airports}}"`, which must be **absent** (OQ-9a-8; the test fails when iOS or 9g adds it, so the exception is removed then). Pass both paths with `systemProperty("chronoflow.repoRoot", rootDir.absolutePath)` in `core/wizard/build.gradle.kts` and declare the two files as task inputs.

### Task 10. Record the open questions

Commit: `docs(plan): record the phase 9a wizard decisions`. Ticks nothing.

- Add OQ-9a-1 … OQ-9a-14 (§9) to `plan.md` "Open questions for the user" as **OQ-A349 … OQ-A362** (OQ-9a-n → OQ-A(348+n), tagged "(9a OQ-9a-n)"), each with its default and owner, in the existing table format after OQ-A348.
- Under Phase 9b, add one line to "Delivers": "9a already ships `MemberWizardModel`, `WizardChecking.run` and the 250 ms choice delay; 9b hosts the model, wires the store and service, and ports the remaining suites (9a brief §10.2)." Do not edit the 9b brief.

---

## 7. Acceptance criteria

1. `make targeted ONLY="core:wizard:<Class> …"` (every class of §10.1 and §10.3) passes; every class of §10.1 runs. `build/test-results/<tier>/timings.json` shows each test under 1 s.
2. `make quick` passes (Spotless, Konsist including rule 2 and the time-hygiene rule over `core/wizard`, every JVM test).
3. `make check` passes.
4. `./gradlew :core:wizard:dependencies --configuration runtimeClasspath` shows no `androidx.*`, `com.android.*` or Compose artifact.
5. `grep -rnE "Locale\.getDefault|TimeZone\.getDefault|ZoneId\.systemDefault|System\.currentTimeMillis|Instant\.now|DateTimeFormatter|String\.format|\.atZone\(" core/wizard/src/main` prints nothing.
6. `grep -rn "guestSecret" core/wizard/src/main` shows only the scrubbing in `MemberWizardRecord.kt` (and the reads that pass identity through, never a log or a copy into state other than the form/review identity).
7. Counting: `grep -c "@Test" ` over the 14 ported classes plus parameterized cases accounts for the 60 iOS functions of §10.1 (list them in the PR description with their Kotlin class).
8. `git log --format=%B feat/phase-9a-wizard-model` contains no `Co-Authored-By` or other trailer.

---

## 8. Review checklist (Opus reviewer)

- [ ] **Scope:** no DB, no HTTP, no airport search, no ViewModel, no Compose; `MemberWizardModel` has no `search`.
- [ ] **Names:** Swift type, function and test names kept; parameterized iOS tests are one case per argument.
- [ ] **Wall clock:** strict regexes plus round trip; year > 0; `instant` uses `getValidOffsets` (gap → null) and the earlier offset in overlaps; no `atZone`, `Date`, `Calendar`, default zone or default locale anywhere in main.
- [ ] **Route rules** verbatim: `insertStop` clears only the split leg's arrival; `removeStop` keeps the first departure and last arrival; the home To follows the outbound origin until `homeChanged`; any leg update clears `itinerary`; 16-flight limit.
- [ ] **Validation keys** byte-equal to `06 §6.1`; past-arrival copy differs for one-way member vs guest or return; the open-jaw server message moves to `review`.
- [ ] **Checking:** 400/800/1200/300/10 000/30 000 ms; a self-cancelled request is a server failure; cancelling the run rethrows and leaves no sleeper; no event after return.
- [ ] **Progress:** the board table of Task 8; edit 1.0; floor rule; guest values never use the shifted iOS table.
- [ ] **First screens:** Where, Duplicate's first Times, edit Ready, edit Checking failure; `popNavigation` refuses there. Back is hidden on Checking (no failure) and Calculating; a member Checking failure shows back.
- [ ] **No health data:** only `age_band` (`under_18`, `18_64`, `65_plus`) and `shift_work` (`true`/`false`) are accepted; preference toggles limited to the two neutral keys; retired keys never in state, record or request.
- [ ] **Secrets:** `guestSecret` stripped from both identities before any record payload; nothing logs a draft.
- [ ] **Recovery codec:** 30 min expiry inclusive at 1_800_000 ms; shape checks; `continuation` and `returnFlight` migrations; `mode` default member.
- [ ] **Concurrency:** single-threaded model on the injected scope; every `CancellationException` rethrown except the documented request case; persistence serialized and `revision` incremented per write; no save retry without a user action.
- [ ] **Copy:** the model emits iOS English keys only; `WizardCopyKeysTest` passes; no hard-coded credit counts.
- [ ] **Commits:** 10 atomic commits, Conventional, no trailers, plan boxes ticked in the commits named above.

---

## 9. Open questions (proceed on the default)

| # | Question | Default |
|---|---|---|
| OQ-9a-1 | The plan gives 9b "checking timers / 250 ms" and the ViewModel. Where does the iOS model class (`MemberWizardModel`) live? | **9a** ports it as a pure class on an injected scope; 9b hosts it in `TripWizardViewModel` and owns the DB store, service and search. 9b's timer items become wiring and integration tests. |
| OQ-9a-2 | Sleep after a return flight: no board value; the model value (0.50) is below ReturnFlight 55 | 0.50 on every sleep step, as plan OQ-A36 / `06` OQ 1 ("sleep 50"); 9c's bar never animates downward, so it holds at 55 |
| OQ-9a-3 | Edit progress: iOS floor 0.95; every edit board draws 100 | 1.0 on every edit step |
| OQ-9a-4 | Guest "checking your answers": no board value | 0.64 on `checking` and `excluded` while checking answers (bar holds at the last question); first check 0.42 (`WizardCheckingGuest`); No plan otherwise 0.60 (`WizardExcluded`, guest) |
| OQ-9a-5 | Back on Duplicate's Times and edit Checking failure (`06` OQ 2) | Board: `firstScreen`, no pop; system back acts as × (9c) |
| OQ-9a-6 | Date text: Apple templates vs fixed tables | Fixed en/nl tables with 0d's `Language`; en year form `EEE, d MMM y` keeps Apple's comma [A] |
| OQ-9a-7 | iOS combined date-time pill (`formatLocal`, its test) | Not ported: Android uses separate date and time pills (`06` OQ 4) |
| OQ-9a-8 | `"{{flight}} · via {{airports}}"` is used by `flightSummary` but missing from the iOS wizard catalog | Model emits the key; 9g adds an Android-only string (en and nl) and removes the test exception; report the gap to the iOS session |
| OQ-9a-9 | iOS fingerprints are `Data` | Canonical sorted-key JSON `String` |
| OQ-9a-10 | iOS strips `guestSecret` only from the guest record | Strip it from every record payload |
| OQ-9a-11 | Swift `mutating` structs | Immutable data classes with copy-returning functions |
| OQ-9a-12 | Checking clock unit | `Long` milliseconds (no `Double` rounding at 1199/1200) |
| OQ-9a-13 | iOS `#if DEBUG` board hooks on the model | Not ported; the seeds phase adds what it needs |
| OQ-9a-14 | `neutralPreferencesPersistAndReachReviewWithoutMedicalKeys` needs the iOS DB | Ported in 9a against the in-memory store, because it proves the no-health-data rule; 9b does not port it again |

---

## 10. Test map

### 10.1 Ported in 9a (60 iOS functions)

| Kotlin class | iOS functions | Task |
|---|---|---|
| `WizardDateTimeTests` | 5 of 6 (not `combinedPillUsesAirportWallComponentsAndTheLocalesClock`) | 2 |
| `WizardFormTests` | 5 of 5 | 3 |
| `WizardRouteTests` | 8 of 8 (3 in Task 3, 2 in Task 4, 1 in Task 5, 2 in Task 7) | 3, 4, 5, 7 |
| `WizardValidationTests` | 6 of 6 | 4 |
| `WizardFlightTimingTests` | 9 of 9 (8 in Task 4, 1 in Task 7) | 4, 7 |
| `WizardReviewFlowTests` | 2 of 19: `returnPastUsesFinalArrivalAndAirportZones`, `neutralPreferencesPersistAndReachReviewWithoutMedicalKeys` | 4, 7 |
| `TripWizardActionsTests` | 1 of 5: `duplicateClearsEveryDateBoundAnswerAndRetainsRouteAndOtherAnswers` | 5 |
| `GuestWizardModeTests` | 9 of 10 (not `guestFineTuneUsesOnlyTheDrawnRowsAndLeavesMemberRowsAvailable`) | 5, 7, 8 |
| `WizardCheckingTests` | 4 of 4 | 6 |
| `WizardHomeRouteTests` | 1 of 1 | 7 |
| `WizardAirportChoiceTests` | 2 of 2 | 7 |
| `WizardCreditsTests` | 2 of 2 | 9 |
| `WizardPresentationTests` | 4 of 4 | 9 |
| `WizardCompletionTests` | 2 of 12: `creditsCoverEveryReadyVariant`, `sheetValidationAcceptsCommaRatingAndRejectsMissingDrinks` | 9 |

Total 5 + 5 + 8 + 6 + 9 + 2 + 1 + 9 + 4 + 1 + 2 + 2 + 4 + 2 = **60**.

### 10.2 Not ported here (owner)

- **9b:** `WizardReviewFlowTests` (17), `WizardCompletionTests` (10), `WizardRetryTests` (6), `TripWizardActionsTests` (4), `WizardRecoveryTests` (4, including `recoveryExpiresAtExactlyThirtyMinutes` and `oldContinuationDraftDecodesAndBecomesReadyOrFineTune`, whose logic 9a's codec already carries), `WizardSaveRecoveryTests` (7), `WizardSearchTests` (3), `TripGenerationTests` (10), `TripGenerationRecoveryTests` (9), `WizardGenerationSessionTests` (1), and `GuestWizardModeTests.guestFineTuneUsesOnlyTheDrawnRowsAndLeavesMemberRowsAvailable` (needs `MockWizardPrompts` from 4c).
- **11a/11b:** `GuestWizardStoreTests`, `GuestRecoveryGateTests`, `GuestWizardGenerationTests`, `GuestSessionAcceptanceTests`, `GuestClaimStoreTests`, `GuestWizardClaimIntegrationTests`, `GuestJourneyCatalogTests`.
- **9c–9g:** `WizardCopyTests` (8).

### 10.3 Android-only tests added

`WizardTestSupportTest`, `WizardDateTimeFormatTest`, `WizardInputMappingTest`, `MemberWizardRecordTests`, `MemberWizardDraftsTest`, `TripGenerationAnswersTest`, `MemberWizardReviewTest`, `WizardProgressTest`, `WizardNavigationTest`, `WizardCopyKeysTest`.

### 10.4 Discrepancies (`06 §11`) and where each is named in a test

| # | Discrepancy | Resolution | Test |
|---|---|---|---|
| 1 | Guest progress shifted on iOS | Board values | `guestWeightsAndForwardProgressFloor`, `WizardProgressTest` |
| 2 | Back on Duplicate's Times and edit failure | Board: first screens | `WizardNavigationTest` |
| 3 | No-credits action | Close only (UI, 9g); model exposes `WizardCredits.unavailable` | `creditsCoverEveryReadyVariant` |
| 4 | Combined pill vs separate pills | Separate (not ported) | `WizardDateTimeTests` comment, OQ-9a-7 |
| 5 | Question wording | Server text wins; guest local questions use the server keys | `questionsRowReopensSupportedAnswersAndReturnsToReady`, `WizardCopyKeysTest` |
| 6 | Preference toggles without a board | Mirrored from iOS, default false, neutral only | `neutralPreferencesPersistAndReachReviewWithoutMedicalKeys` |
| 7 | Airport search states | 9b/9d | — (search is out of scope) |
| 8 | 16-flight limit state | From copy | `editingAStopDerivesBothAdjacentEndpointsAndHonorsTheTotalLimit` |
| 9 | Overnight sample times | Computed from facts | `localFactsBeforeCheckingUseZonesAndCompleteNights` |
| 10 | CheckPractice | Not built | — |

---

## 11. Hand-off to 9b

9b receives: `MemberWizardModel` (construct with the DB store, the service as `MemberWizardGeneration`, `viewModelScope`, the app clock and `WizardCheckingClock.continuous`), `MemberWizardSnapshot` as the ViewModel's state, `MemberWizardRecord` for the drafts row (`ID`, `isExpired`, `encode`/`decode`), `MemberWizardDrafts` for `start`/`start(seed)`/`startTrip`, `TripGenerationAnswers.applyingAnswers`, and `firstScreen`/`showsBack` for 9c. Never put a `MemberWizardDraft` in a `SavedStateHandle`: it may hold a review identity with a guest secret; persist only through `MemberWizardRecord`.
