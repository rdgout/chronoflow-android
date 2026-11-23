# Phase 1a brief: PlanCore port: decode, copy and check state

- **Branch:** `feat/phase-1a-plan-core`, stacked on the stack top once 0c and 0d are in the stack.
- **Depends on:** 0c (plan copy at `core/plan/src/main/resources/plan_copy.json`), 0d (generated contracts, `ApiJson`, fixtures in `:core:testing`, `tools/contracts-gen/copy_fixtures.py`). Owns `CheckOffChange`, which 4b uses and never redeclares (4b depends on 1a since the 2026-11-16 review; Task 4). **Wave:** 3. **Milestone:** M1.
- **Plan entry:** `plan.md` "### Phase 1a: PlanCore port: decode, copy and check state" (l.993–1033).
- **Brief written:** 2026-11-16 by the orchestrator, from iOS read at `f391a1fa` (verified: `git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` = `f391a1fac33eb0f0ea9e3f565206b91118e793d0`).
- **Checked:** 2026-11-16 by an adversarial checker against iOS at the pin, web `routes/api.php` and `app/Data/Api/ApiTripDetail.php`, and the 0a/0c/0d/0e/4b/4d briefs. Fixes: 0d spellings (`ApiJson.response`, `.raw`/`X.of`, `CopyArgValue.Text/Numeric`, `PlanMeta`), `intl-cases.json` through `copy_fixtures.py`, `CheckOffChange` shared with 4b (not `@Serializable`), Konsist layout, `planDate` leniency (A.3), fixture walk over `plan` only, tick placement, `targeted` syntax.
- **Tests:** follow AGENTS.md "Tests" for every test you write. Plain JVM test classes carry no tier annotation (0e §5). This phase is JVM only: JUnit 4.13.2, assertk 0.28.1, kotlinx-coroutines-test 1.11.0, Turbine 1.2.1. No Robolectric, no emulator.

Tags: **[F]** fact verified in source (path and line). **[R]** decision for Android adopted by this brief. **[A]** Apple output measured on this Mac on 2026-11-16 with `swift` (Foundation `DateFormatter`/`NumberFormatter`), used as goldens.

---

## 1. Goal

Port the non-geometry half of iOS `PlanCore` (and the decode and mark-merge part of iOS `PlanUI/Models/PlanDecoder.swift`) into the pure-JVM module `:core:plan`, so that later phases can turn stored plan bytes into a typed plan, its check state and its localized text, identically to iOS:

1. **Decode** plan-detail bytes into the 0d contracts with tolerant enums, cached per `(tripId, inputHash, detailVersion)` with a raw-byte guard. The raw bytes are kept untouched (they are what `plan_versions.detail` stores).
2. **CopyCatalog** over `plan_copy.json`, resolved with an explicit `Language`, with the single/plural/coded forms and the unknown-key fallback.
3. **CopyLineFormatter**: every argument kind, Drinks caffeine amounts, fixed month and weekday tables, the 12/24 h rule from the account `timeFormat`, the U+2212 minus, durations, numbers. Exactly the iOS strings, on every JVM and Android API level, independent of the default locale and time zone.
4. **CheckState**: `checkIdentity`, `buildCheckState`, and merging stored marks with pending outbox check-offs.
5. **PlanLogic**: optional and hidden-step logic, notices, zone labels, summary intervals, label keys, trip display status. Plus the pure label functions of `TimelineLabels.swift` except `nowLabel` (see OQ-1a-1).
6. **PlanClock** interface, `FixedPlanClock`, and `FakePlanClock` in `:core:testing`.
7. **Time hygiene**: a `ZoneRules` cache, no `DateTimeFormatter`/`String.format` in formatting paths, and a Konsist rule that bans default-locale, default-zone and system-clock reads in the logic modules.
8. **Ported tests**: every non-geometry case of iOS `Tests/PlanCoreTests` (44 intl goldens, caffeine tables, plural and fallback, labels, notices, status), plus the two PlanDecoder/check-state cases of `Tests/PlanUITests`, under their Swift names.

No UI, no Android API, no Compose. Nothing in this phase draws, lays out or measures.

---

## 2. Inputs to read

### 2.1 This repo

| What | Path | Why |
|---|---|---|
| Plan | `plan.md` l.993–1033 (Phase 1a), A3 (module rules), A17 (date/number rules), "Android spec: configuration and appearance" (Language is explicit) | Scope and decisions |
| Agent rules | `AGENTS.md` ("Working rules", "Tests", "Git", "Traps": rethrow `CancellationException`) | How to work |
| Research | `docs/research/04-timeline.md` §2.1–§2.4, §2.14, §9.2–§9.3 | Contract shapes, check identity, formatting rules |
| Research | `docs/research/10-copy-l10n.md` §5–§8 (especially §8.3 "Parity trap") and §9.7 | Plan copy, rendering rules, Apple vs CLDR |
| Research | `docs/research/09-rn-android-lessons.md` §3 row 22 | Keep RN's golden en/nl/DST cases as JVM tests. Row 22 also suggests `DateTimeFormatter.ofPattern` per locale; that part is **superseded** by `10 §8.3` and A17 (fixed tables, hand assembly), because CLDR prints "Sept"/"sep." |
| 0c output | `core/plan/src/main/resources/plan_copy.json`, `tools/l10n/plan_copy.sha256` | The table you load |
| 0d output | `core/model/src/main/kotlin/net/chronoflow/core/model/contracts/TripPlan.generated.kt`, `…/contracts/{RawEnum,CopyLine,PlanContracts,TripContracts}.kt`, `…/model/api/ApiJson.kt`, `core/testing/src/main/kotlin/net/chronoflow/core/testing/Fixtures.kt`, `core/testing/src/main/resources/fixtures/**`, `fixtures/fixtures.sha256`, `tools/contracts-gen/copy_fixtures.py` | Types, fixtures and the fixture copier you build on |
| 0d brief | `docs/phases/phase-0d-contracts.md` §5.4.2 (enum shape: `X.of(raw)`, `.raw`, `Unknown(raw)`, PascalCase cases), §5.4.3 (`CopyArgValue.Text`/`Numeric(BigDecimal)`), §9 "Hand-off" to 1a | Exact Kotlin spellings used below |
| 4b brief | `docs/phases/phase-4b-api-client.md` Task 1 (`OutboxChange.CheckOff` wraps `CheckOffChange`) | How 4b uses the `CheckOffChange` you own |
| 0a output | `konsist/src/test/kotlin/**/KonsistRulesTest.kt`, `konsist/src/test/resources/violations/` | Where the new rule and its planted violations go |

### 2.2 iOS (read-only; read at the pin with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

Mirror behaviour from these files. Line numbers are at `f391a1fa`.

| File | Lines | Port to |
|---|---|---|
| `Packages/ChronoFlowKit/Sources/PlanCore/CopyCatalog.swift` | 1–61 | `CopyCatalog.kt` |
| `Packages/ChronoFlowKit/Sources/PlanCore/CopyLineFormatter.swift` | 1–206 (`CopyFormat` 4–21, `format` 27–42, `formatArgument` 44–61, numbers 63–69, dates 71–99, `formatInstant` 101–110, minutes 112–127, offset 129–142, drinks 144–178, `CopyArgValue.text/number` 185–193, `planDate` 195–200, `zoneLabel` 202–206) | `CopyLineFormatter.kt`, `PlanTime.kt` |
| `Packages/ChronoFlowKit/Sources/PlanCore/CheckState.swift` | 1–27 | `CheckState.kt` |
| `Packages/ChronoFlowKit/Sources/PlanCore/PlanLogic.swift` | 1–128 | `PlanLogic.kt` |
| `Packages/ChronoFlowKit/Sources/PlanCore/TimelineLabels.swift` | 1–44, **except `nowLabel` (14–19)** | `TimelineLabels.kt` |
| `Packages/ChronoFlowKit/Sources/PlanCore/PlanClock.swift`, `FixedPlanClock.swift` | 1–5, 1–14 | `PlanClock.kt`, `FixedPlanClock.kt` |
| `Packages/ChronoFlowKit/Sources/PlanUI/Models/PlanDecoder.swift` | `detail(_ record:)` 65–74, `detail(_ snapshot:)` 76–96, `evict` 111–115 | `PlanDecoder.kt` |
| `Packages/ChronoFlowKit/Sources/AppCore/Contracts/CopyLine.swift` | 1–46 | Semantics of `CopyArgValue` and `CopyArgument` (0d types) |
| `Packages/ChronoFlowKit/Sources/AppCore/OutboxChange.swift` | 3–29 | `CheckOffChange` |
| `Packages/ChronoFlowKit/Sources/LocalStore/Records/{PlanVersionRecord,CheckMarkRecord}.swift`, `LocalStore/TripRepository+PlanObservation.swift` 4–9 | all | Plain value types for the decoder input |
| `Packages/ChronoFlowKit/Sources/PlanCore/Resources/SOURCE.md` | all | Provenance of the copy table |
| `Packages/ChronoFlowKit/Tests/PlanCoreTests/CopyTests.swift` | 1–111 | `CopyTests.kt` |
| `Packages/ChronoFlowKit/Tests/PlanCoreTests/LabelTests.swift` | 1–179 | `LabelTests.kt` (non-layout parts) |
| `Packages/ChronoFlowKit/Tests/PlanCoreTests/TimelineTests.swift` | helpers 7–37; copy half of `everyFixturePreservesLayoutAndResolvesCopy` 199–270 | `PlanCoreTestSupport.kt`, `TimelineTests.kt` |
| `Packages/ChronoFlowKit/Tests/PlanCoreTests/Fixtures/intl-cases.json` | 44 cases | copied to `:core:testing` |
| `Packages/ChronoFlowKit/Tests/PlanUITests/ModelTests.swift` | `versionAndRawPayloadInvalidateCache` 134–153 | `ModelTests.kt` |
| `Packages/ChronoFlowKit/Tests/PlanUITests/StepCheckTests.swift` | `localProgressCountsStartsAndKeepsSkippedSeparate` 80–90 | `ModelTests.kt` |
| `plan.md` | l.1623 "Phase 14: Plan copy and timeline logic (`feat/phase-14-plan-logic`), no UI"; l.1625 "14a: Plan copy catalog and formatting"; l.1639 "14b: Timeline logic and observation models" | The iOS decisions this phase inherits |

iOS 14a/14b decisions that bind this port (quoted from iOS `plan.md` l.1630–1648):
- "A key missing at runtime shows the English catalog value with args. A key missing from the catalog entirely is caught by the generator check, never invented."
- "Tests: RN `intl-cases.ts` as golden cases in en and nl; every key in S1–S6 and the review fixtures resolves in en and nl; plural forms; Dutch half/one/2–4/5+ cups; DST instants (`Europe/Amsterdam` on 2026-10-25, `America/New_York` on 2026-11-01); unknown-key fallback."
- "optional filtering is enforced by `layoutDay` and `summaryIntervals` as well as the plan helper, so callers cannot accidentally reveal optional steps. Unknown types/lanes are skipped only in layout; the stored DTOs stay unchanged."
- "Native ISO8601 value parsing avoids repeated formatter allocation."

### 2.3 Research quotes this brief relies on

- `04 §2.1`: "Instant parsing (`planDate`, `PC/CopyLineFormatter.swift`): a 10-character date gets `T12:00:00Z` appended. A 17-character `…THH:MMZ` gets `:00` inserted before `Z`." and "Enums must be tolerant: an unknown value becomes `Unknown(raw)` and never throws. Unknown types are skipped only at render time."
- `04 §2.14`: "`checkIdentity = "type|localDate|ordinal"`. `buildCheckState` counts per-day done, skipped and total by the day in which the block **starts**."
- `04 §9.3`: "`HH:mm` is built by hand from `ZoneRules.getOffset(instant)` and epoch-second arithmetic … Never use `String.format` or `DateTimeFormatter` per block." and "Cache `ZoneRules` per tz id."
- `10 §8.3`: OpenJDK/CLDR gives "15 **Sept** 2026" (en-GB) and "15 **sep.** 2026" (nl-NL) where Apple gives "15 Sep 2026" / "15 sep 2026". "**[R]** Use fixed tables for plan copy … Build `d MMM yyyy`-style strings by hand."
- `10 §9.7`: "Replace `%N$@` with a hand-written substituter (`%%` → `%`), not `String.format`." "Language is passed **explicitly** (`Language.EN/NL`), never from the process locale." (0d spells these `Language.En`/`Language.Nl`.)

### 2.4 Boards

None. This phase has no UI. (`SetPreferences` shows the 12/24 h choice; that screen is Phase 10b. Do not build anything from a board here.)

### 2.5 API endpoints

None are called. The decoded shape is `GET /v1/trips/{trip}` → `ApiTripDetail` (web `app/Data/Api/ApiTripDetail.php`, iOS name `TripDetail`):

```text
{ trip: TripDetailView, plan: PlanView, notices: PlanNoticesView,
  timeline: TimelinePresentationView, checkState: CheckStateView (iOS/0d `CheckState`), detailVersion: string }
```

(Web route `routes/api.php:111` `GET trips/{trip}` under the `v1` prefix of l.46; check-off route l.101.)

The check-off body `PUT /v1/trips/{trip}/checks` `{plan_input_hash, type, local_date, ordinal, status: "done"|"skipped"|null}` is Phase 4b/6d; this phase only defines `CheckOffChange` as the value the outbox will carry.

---

## 3. Out of scope (owner in brackets)

- `Timeline.swift` (bucketing, `layoutDay`, `packLane`, ticks, zones, `nowLineOffset`, `layoutPlan`, `indexTimeline`), `TimelineGeometry`, `TimelineAccessibilityGeometry`, `nowLabel`, `PlanDecoder.layouts/prepare/nowTexts`, `PlanPreparer`, `PreparedPlan`, large30/60 fixtures, budget tests [1b].
- `PlanMetrics` values and `TimelinePalette` [0b token generator; 1b uses them].
- `Today.swift` [7]. `PlanDecoder.trips/adaptation/card`, `TripListValue` [6a]. `SystemPlanClock` (Android receivers, minute ticks, `cf.freezeAt`) [1d].
- Any database, SQLDelight row, outbox persistence format or API call [4a, 4b, 4d]. `CheckOffChange` JSON shape on disk [4d].
- `CopyIndex.kt` and the UI string resources [0c]. Any `Text` or resource lookup.
- Fetching or re-generating `plan_copy.json` [0c owns it; never edit it].

---

## 4. What 0c and 0d hand you (check before starting)

Run these checks first. If one fails, stop and report: the phase cannot start.

1. `core/plan/src/main/resources/plan_copy.json` exists and its SHA-256 is `927568d02d8bcd215d80fa455c1c78dc616544f2d321fc8d7898df9bf1ce584f` (141,652 B, 289 entries; iOS `PlanCopy.json` at `f391a1fa`).
2. The 0d contracts compile in `:core:model` and include, under the iOS names (iOS `AppCore/Contracts/Generated/TripPlan.swift`): `TripDetail`, `TripDetailView`, `TripDetailLegView`, `AirportRef`, `PlanView`, `PlanMeta` (holds `inputHash`), `DayView`, `DayZoneView`, `BlockView`, `CheckKeyView`, `CheckMark`, `CheckState`, `CheckStatus`, `DayCheckProgressView`, `PlanNoticesView`, `NoteView`, `CopyLine`, `CopyArg`, `CaffeineCopyArg`, the hand-written `CopyArgument` (`Standard(value: CopyArg)` / `Caffeine(value: CaffeineCopyArg)`, split on the presence of the `drinks` key) and `CopyArgValue` (`Text(value: String)` / `Numeric(value: BigDecimal)`, built from the raw JSON number text) in `contracts/CopyLine.kt`, `CopyArgKind`, `Drink`, `DrinkAmountView`, `CaffeineAmountView`, `BlockType`, `Lane`, `Priority`, `LabelPhase`, `DayStatus`, `SleepKind`, `BlockParams` (hand-written, `contracts/PlanContracts.kt`; sleep params are `BlockParams.Sleep(value)`), `TripCardView`, `TripStatus`, `Language`, `TimeFormat`, `TimelinePresentationView`.
   - Enum spelling **[F]** (0d §5.4.2): `sealed interface X : RawEnum` with PascalCase `data object` cases named after the Swift case (`BlockType.SeekLight`, `BlockType.CaffeineOK`, `BlockType.LightOK`, `Language.En`, `Language.Nl`, `TimeFormat.Value12`, `TimeFormat.Value24`, `CheckStatus.Done`, `Priority.Optional`, `SleepKind.InFlightMain`, `TripStatus.Completed`), plus `data class Unknown(override val raw: String)`. The wire string is `.raw`; the factory is `X.of(raw)`. This brief uses those spellings; Swift `rawValue`/`X(rawValue:)` map to `.raw`/`X.of(...)`. If 0d as built differs, use 0d's spelling. Do not rename or edit generated files.
3. `object ApiJson` exists in `net.chronoflow.core.model.api` with `response` (`Json { ignoreUnknownKeys = true; explicitNulls = false }`, used for every decode here) and `request`. Use `ApiJson.response`; never create another `Json { }` in main code (0d rule).
4. `core/testing/src/main/resources/fixtures/plans/{S1,S2,S3,S4,S5,S6,S6-RETURN-10N}.json`, `fixtures/trips/detail.json` and `fixtures/wizard/{review,needs-answers}.json` exist with their hashes in `fixtures.sha256`. If 0d used another directory layout, use 0d's loader and layout.
   - Reference hashes: `S1.json` `cb2f1fa5e47997e1397985a142344139f359dceaf60043413ed00bca0c1947eb`, `S6-RETURN-10N.json` `8bb0af18c3e4099bcb8359026b50ffa16472ea3bd77f4198c8c17ec4a6f558c7`. The `PlanCoreTests/Fixtures/S*.json` files on iOS are byte-identical to the `DataAccessTests/Fixtures/plans/` ones, so 0d's copies serve both.
   - `Fixtures` (0d, `net.chronoflow.core.testing`) loads them through `getResourceAsStream("/fixtures/$path")`.
   - `CheckOffChange`: 1a owns it (4b depends on 1a since the 2026-11-16 review), so `grep -rn "class CheckOffChange" core/model/src/main` finds nothing and Task 4 creates it. If the grep finds a declaration, stop and report it.
   - Note **[F]**: `S*.json` hold only `{plan, notices, timeline}` (no `trip`, `checkState` or `detailVersion`). `trips/detail.json` is the full envelope (S1 parts, `detailVersion` `"v1"`, `inputHash` `"aaaa…a"` ×64, empty `checkState`).

---

## 5. Module and package layout

- Module `:core:plan` (plugin `chronoflow.jvm.library`, namespace/package `net.chronoflow.core.plan`). Add the kotlinx-serialization compiler plugin to this module if the convention plugin does not apply it.
- Dependencies: `api(project(":core:model"))`, `implementation(libs.kotlinx.serialization.json)`, `implementation(libs.kotlinx.coroutines.core)`. Tests: `testImplementation(project(":core:testing"))`, JUnit 4, assertk, coroutines-test, Turbine.
- `:core:testing` gets `implementation(project(":core:plan"))` for `FakePlanClock` (no cycle: `:core:plan` main does not depend on `:core:testing`).
- Main sources: `core/plan/src/main/kotlin/net/chronoflow/core/plan/`. Tests: `core/plan/src/test/kotlin/net/chronoflow/core/plan/`.
- Ported code keeps Swift names: Swift free functions become Kotlin top-level functions with the same names; Swift `extension CopyLineFormatter` functions become Kotlin extension functions; Swift structs become Kotlin classes with the same names.
- No `object` holding state, no top-level `val` that does work at class-load time (D26: no static initialisers, no eager singletons). Constant tables (`arrayOf("Jan", …)`) inside a `private object` or `companion` are fine. The one allowed shared value is the zone-rules memo in Task 1 (`internal val planZoneRules = ZoneRulesCache()`): constructing it only allocates an empty map, and the Swift top-level functions `localDate` and `utcOffset` need it without an instance.

---

## 6. Tasks

Each task is one commit on `feat/phase-1a-plan-core`. Tick the matching checkbox in `plan.md` (Phase 1a "Tasks") in the commit that completes it; the mapping is given per task. Commit subjects are given; no trailers.

### Task 1. Time and number primitives

Commit: `feat(plan): add plan time parsing, zone cache and fixed calendar tables`. Ticks nothing yet.

**Files**
- `core/plan/…/PlanTime.kt`
- `core/plan/…/PlanCalendarText.kt` (Android-only helper; no iOS twin)
- `core/plan/…/PlanNumberText.kt` (Android-only helper; no iOS twin)

**Behaviour**

1. `fun planDate(value: String): Instant?` — port of `CopyLineFormatter.swift:195–200`. Returns `null` where iOS returns the NaN date.
   - If `value.length == 10`, append `T12:00:00Z`.
   - Else if `value.length == 17` and it ends with `Z`, insert `:00` before the `Z` (`2026-10-14T23:45Z` → `2026-10-14T23:45:00Z`).
   - Then accept exactly `YYYY-MM-DD` + `T` (upper case only) + `HH:MM:SS`, optionally `.` + 1–9 digits, then one of `Z`, `z`, `±HH`, `±HHMM`, `±HH:MM`. Anything else is `null` (Appendix A.3). **[A]** Apple rejects `2026-01-15T00:05+02:00` (no seconds with an offset), a lower-case `t`, a missing zone, `.` without digits and 10 fraction digits; `DateTimeFormatter.ISO_OFFSET_DATE_TIME` accepts the first, so do not use that formatter. Hand-parse (no allocation beyond the result).
   - Field ranges: month 1–12 (else `null`; Apple rejects `2026-13-01`). Day 1–31, hour 0–24, minute 0–59 and second 0–60 are combined **arithmetically**, so overflow rolls over as on Apple (**[A]** `2026-02-30T00:00:00Z` → 2 Mar, `T24:00:00Z` → next day 00:00, `:60` → next minute): `epochSecond = (LocalDate.of(y, m, 1).toEpochDay() + d - 1) × 86400 + h × 3600 + min × 60 + sec − offsetSeconds`. Out-of-range fields → `null`. Plan data never contains these; the rule only keeps the parser total.
   - Never throws.
2. `internal class ZoneRulesCache`: `fun rules(tz: String?): ZoneRules`, backed by a `ConcurrentHashMap<String, ZoneRules>` (`computeIfAbsent`). `null`, unknown or invalid ids resolve to `ZoneOffset.UTC.rules` and are cached as such. `ZoneId.of` exceptions are caught by type (`DateTimeException`, which covers `ZoneRulesException`), never `Exception`. One process-wide instance, `internal val planZoneRules = ZoneRulesCache()` in `PlanTime.kt`, is used by `CopyLineFormatter`, `localDate` and `utcOffset` (§5). Do not put `ZoneRulesCache` in a public signature (Kotlin rejects an internal type in a public constructor).
3. `internal class LocalFields(year, month, day, hour, minute, dayOfWeek)` computed from `(Instant, ZoneRules)` with `rules.getOffset(instant)` and epoch-second arithmetic (`Math.floorDiv`/`floorMod`). No `ZonedDateTime` allocation is required; `LocalDateTime.ofEpochSecond` is acceptable.
4. Fixed tables (A17, `10 §8.3`), indexed 1–12 and ISO Monday=1…Sunday=7:
   - en months `Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec`; nl months `jan feb mrt apr mei jun jul aug sep okt nov dec`.
   - en weekdays `Mon Tue Wed Thu Fri Sat Sun`; nl weekdays `ma di wo do vr za zo`.
   - `Language.Unknown(_)` uses en.
5. `internal fun dateText(instant: Instant?, language: Language, rules: ZoneRules, template: DateTemplate): String` with `enum class DateTemplate { D_MMM_YYYY, D_MMM, EEE_D_MMM, EEE_D, EEE, MMM_YYYY }` producing `"29 Mar 2026"`, `"29 Mar"`, `"Sun 29 Mar"`, `"Sun 29"`, `"Sun"`, `"Mar 2026"` (day without padding, year as 4 digits, single spaces). Null instant → `""` (OQ-1a-4). iOS strips every `.` from the output; the tables contain none, so the output never has a dot.
6. `internal fun formatPlanNumber(value: Double, language: Language): String` — port of `NumberFormatter` en_GB / nl_NL, `.decimal`, `maximumFractionDigits = 3`:
   - Round with `BigDecimal.valueOf(abs(value)).setScale(3, RoundingMode.HALF_EVEN).stripTrailingZeros()`, then read the digits with `toPlainString()` (never `toString()`, which gives `1E+3`). `BigDecimal.valueOf` uses the shortest decimal string of the double, which is what Apple rounds (**[A]** `1234567.8915` → `…892`, `0.0015` → `0.002`, `0.1245` → `0.124`); `new BigDecimal(double)` or `DecimalFormat` round the binary value and give `…891`.
   - Group the integer part by 3 with `,` (en) or `.` (nl); the decimal separator is `.` (en) or `,` (nl).
   - Prefix `-` (U+002D) when the value is negative **or negative zero** (`value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)`); **[A]** Apple prints `-0` for `-0.0` and `-0.0004`.
   - `NaN`/infinite never come from plan JSON; return `"NaN"`/`"∞"`/`"-∞"` without crashing (no golden; OQ-1a-4).
   - No `NumberFormat`, `DecimalFormat` or `String.format`.
7. Two-digit padding helper (`internal fun StringBuilder.append2(n: Int)`) used by every `HH:MM`, `YYYY-MM-DD` and `GMT±hh:mm` producer. ASCII digits only.

**Edge cases:** DST gaps and folds come out of `ZoneRules.getOffset(instant)` for free (the instant is fixed; only the offset varies). Dates before 1970 must work (`floorDiv`). A `tz` of `null` means UTC.

**Tests** (`core/plan/src/test/…`)
- `PlanDateTest`: `"2026-10-15"` → `2026-10-15T12:00:00Z`; `"2026-10-14T23:45Z"` → `23:45:00Z`; `"2026-01-01T00:00:00.000Z"`; `"2026-01-15T00:05:00.123Z"`; `"2026-01-15T00:05:00+02:00"` → `2026-01-14T22:05:00Z`; `"2026-01-15T00:05+02:00"` → `null`; `""`, `"garbage"`, `"2026-13-01"` → `null`; and every other row of Appendix A.3 (accepted rows to the listed instant, rejected rows to `null`).
- `DateTextTest`: the internal `dateText` for all six templates on `2026-09-15T12:00:00Z` and `2026-05-03T12:00:00Z`, en and nl (expected values in Appendix A.1), plus a pre-1970 instant (`1969-12-31T23:30:00Z` in `Asia/Tokyo` → `1 Jan 1970`).
- `NumberTextTest`: the **[A]** table in Appendix A.2 through the internal `formatPlanNumber`, both languages.
- (`CalendarTablesTest`, through the public formatter API, lands in Task 3.)

### Task 2. CopyCatalog

Commit: `feat(plan): port copy catalog over plan_copy.json`. Ticks nothing yet: the plan's "**CopyCatalog.kt.**" checkbox also covers the `CopyArg`/`CaffeineCopyArg` arguments and the caffeine tables, which land in Task 3.

**Files:** `core/plan/…/CopyCatalog.kt`, `core/plan/…/CopyCatalogProvider.kt`.

**Behaviour** (port of `CopyCatalog.swift:1–61`)

- Table shape **[F]** (`plan_copy.json`): `{"entries":[{"key": String, "arguments": [String], "languages": {"en": T, "nl": T}}]}` with `T = {"kind": "single"|"plural"|"coded", "formats"?: [String], "cases"?: [{"bound": Number, "exact": Boolean, "format": String}]}`. Counts at the pin: 289 entries; 272 single, 9 plural, 8 coded per language; every format uses only `%1$@`, `%2$@`, `%3$@` (238/48/10 occurrences); no `%%`.
- `class CopyCatalog(data: ByteArray)`: decode with kotlinx.serialization (private `@Serializable` `Table`, `Entry`, `Translation`, `Case`) into a read-only `HashMap<String, Entry>`. Throw `IllegalArgumentException` on a duplicate key (Swift `uniqueKeysWithValues` traps) and on an entry without `en` (Swift force-unwraps at lookup; Android fails at load instead).
- At load, compile every format once into a small template (literal segments + argument indexes) so lookups never scan for `%`. Substitution rules: `%N$@` → value N (1-based); `%@` → the next sequential value; `%%` → `%`; any other `%` sequence is copied literally; an index beyond the values is copied literally.
- `companion object { suspend fun load(io: CoroutineDispatcher): CopyCatalog }` reads `/plan_copy.json` through `CopyCatalog::class.java.getResourceAsStream` inside `withContext(io)` (Swift `load()` is `Task.detached`). It is never called at class-load time.
- `class CopyCatalogProvider(private val io: CoroutineDispatcher)` with `suspend fun catalog(): CopyCatalog`: loads once (guarded by a `Mutex`) and returns the same instance afterwards. This is "loads once, lazily and off the main thread" (plan.md). The app graph will own one provider (later phase).
- `fun contains(key: String): Boolean`.
- `fun text(key: String, language: Language, arguments: Map<String, String> = emptyMap(), count: Double = 1.0): String`:
  1. Unknown key → `interpolate(key, arguments)`: replace each `:name` matching `:([A-Za-z][A-Za-z0-9_]*)` whose name is in `arguments`; leave others untouched (`"Unknown :place and :place2"` with `place=Tokyo` → `"Unknown Tokyo and :place2"`). Use a precompiled `Regex` held by the instance, or a hand scan.
  2. Translation = `languages[language.raw] ?: languages["en"]` (so `Language.Unknown` and a missing `nl` fall back to en).
  3. Values = for each name in `entry.arguments`: `arguments[name]`, else `count.toString()` if the name is `count` (Swift `String(Double)`: `1.0` → `"1.0"`; Kotlin's `Double.toString()` matches for normal values), else `":" + name`.
  4. Form selection (`Translation.format(count:)`, Swift l.21–25): if `cases` is non-null → the first case where `exact ? count == bound : count >= bound`, else `""`; otherwise `formats[0]` when `count == 1.0`, else `formats[min(1, formats.size - 1)]`; empty `formats` → `""`. The `kind` string is not consulted.
  5. Substitute the values into the selected template.
- Thread-safe after construction (immutable).

**Edge cases:** coded case order matters (first match wins); `count` 0 or 0.5 on a coded table can yield `""` (iOS does the same); `count` is a `Double`, compare exactly.

**Tests:** `CopyCatalogTableTests` (new, Android-only):
- all 289 entries load; every entry has `en` and `nl`;
- every `%N$@` index is ≤ `arguments.size`; plural entries have exactly 2 formats; coded entries have ≥ 1 case;
- a duplicate key and a missing `en` each throw;
- `text` on an inline table `{"entries":[{"key":"Hello :name","arguments":["name"],"languages":{"en":{"kind":"single","formats":["Hello %1$@"]}}}]}` with `Language.Nl` and `arguments = mapOf("name" to "Ada")` → `"Hello Ada"` (en fallback);
- `%%` and `%@` substitution on an inline table;
- `CopyCatalogProvider` (`runTest(timeout = 60.seconds)`): with `io` = the test-local `CountingDispatcher` of Task 4 wrapping `StandardTestDispatcher(testScheduler)`, two concurrent `async { provider.catalog() }` calls return the same instance (`isSameInstanceAs`), a third sequential call returns it too, and `dispatches > 0` (the load ran on `io`). Define `CountingDispatcher` (shape in Task 4) in `core/plan/src/test/kotlin/net/chronoflow/core/plan/CountingDispatcher.kt` in this task; Task 4 reuses it.

### Task 3. CopyLineFormatter and TimelineLabels (without `nowLabel`)

Commit: `feat(plan): port copy line formatter with fixed tables and 12/24 h rule`. Ticks `plan.md` Phase 1a "**CopyCatalog.kt.**" and "**CopyLineFormatter.kt.**".

**Files:** `core/plan/…/CopyLineFormatter.kt` (`CopyFormat`, `CopyLineFormatter`, `CopyArgValue.text`/`.number` extensions, `zoneLabel`), `core/plan/…/TimelineLabels.kt` (`dayDateLabel`, `blockInterval`, `localDate`, `utcOffset`). `planDate` stays in `PlanTime.kt` (Task 1). Test support: create `core/plan/src/test/…/PlanCoreTestSupport.kt` here (contents in Task 9 "Test support"; `LabelTests.datesZonesAndIntervals` needs `sampleDay` and `block`). Fixture copy: `tools/contracts-gen/copy_fixtures.py` and the generated `core/testing/src/main/resources/fixtures/intl-cases.json` plus its manifest line (see "Fixture copy" below).

**Behaviour** (port of `CopyLineFormatter.swift`; exact strings)

- `data class CopyFormat(val language: Language, val timeFormat: TimeFormat = TimeFormat.Value24, val displayZone: String? = null, val annotateInstantZone: Boolean = false, val trip: TripDetailView? = null)`.
- `class CopyLineFormatter(val catalog: CopyCatalog)`. It reads zone rules through `planZoneRules` (Task 1). Stateless apart from that cache, so thread-safe.
- `val CopyArgValue.text: String`: `is CopyArgValue.Text` → `value`; `is CopyArgValue.Numeric` → `if (value.signum() == 0) "0" else value.stripTrailingZeros().toPlainString()` (**[A]** Swift `NSDecimalNumber.stringValue`: `2` → `"2"`, `1.50` → `"1.5"`, `1e3` → `"1000"`, `0.000001` → `"0.000001"`, `0` → `"0"`). Never go through `Double`. `val CopyArgValue.number: Double` = `text.toDoubleOrNull() ?: 0.0`.
- `format(line: CopyLine, options: CopyFormat): String` (l.27–42): build `arguments` in arg order (a later arg with the same name wins). Standard arg → `formatArgument(arg, options)`; when `kind == count && name == "count"`, `count = arg.value.number`. Caffeine arg (`CopyArgument.Caffeine(value)`) → `usualDrinks(value.usualDrinks, …)` if `usualDrinks != null`, else `caffeineAmount(CaffeineAmountView(mg = it.value, drinks = it.drinks), …)`. Return `catalog.text(line.key, options.language, arguments, count)`.
- `formatArgument(arg: CopyArg, options): String` (l.44–61):
  - `date` → `formatDate(value.text, language)` in **UTC**; `arg.tz` is ignored (iOS parity).
  - `instant` → `zone = arg.tz ?: options.displayZone ?: "UTC"`; `time = formatInstant(text, options, zone)`; if `options.annotateInstantZone && zone != options.displayZone` → `"$time (${zoneLabel(zone, options.trip)})"`, else `time`. (With `displayZone == null` and `tz == null`, zone is `"UTC"` and it is annotated `"(UTC)"`; iOS does the same.)
  - `offset` → `formatOffset(number)`; `minutes` → `formatMinutes(number)`; `count` → `formatNumber(number)`; `milligrams` → `text(":amount mg", ["amount" to formatNumber(number)])`; `place`, `text`, unknown → `value.text`.
- `formatNumber(value, language)` → `formatPlanNumber` (Task 1).
- Dates (l.71–99), all with `zone: String = "UTC"`: `formatDate` (`d MMM yyyy`), `formatDayMonth` (`d MMM`), `formatWeekdayDate(value, language, zone, month = true)` (`EEE d MMM`, or `EEE d` when `month = false`), `formatWeekday(value, language)` (`EEE`, always UTC), `formatMonthYear` (`MMM yyyy`). Input goes through `planDate`; output through the fixed tables.
- `formatInstant(value: String, options: CopyFormat, zone: String? = null): String` (l.101–110): zone = `zone ?: options.displayZone ?: "UTC"`. 24 h (any `timeFormat` that is not `Value12`, including `Unknown`): `HH:MM` zero-padded. 12 h: hour `h % 12 == 0 ? 12 : h % 12`, **zero-padded** (`03:00`, `12:05`), then `" "` and `text("AM")` if `h < 12` else `text("PM")` (en `AM`/`PM`, nl `a.m.`/`p.m.`, from the catalog). Invalid value → `""`.
- `formatMinutes(value, language, duration = false)` (l.112–127): `hours = floor(value / 60)`, `minutes = value % 60` (Kotlin `%` on `Double` = Swift `truncatingRemainder`). Not duration → `text(":amount min", amount = formatNumber(value))`. Duration → hours > 0 and minutes > 0: `":hours hr :minutes min"`; else `":amount hr"` with hours if hours > 0, else `":amount min"` with minutes.
- `formatOffset(value, language)` (l.129–142): on `abs(value)`: `hours = floor(abs / 60)`, `minutes = abs % 60`; minutes ≠ 0 → `":hours h :minutes min"`, else `":amount h"`; prefix `"+"` if value > 0, `"−"` (**U+2212**) if value < 0, nothing for 0.
- Drinks (l.144–178):
  - `drink(amount: DrinkAmountView, language, chip = false)`: key by drink — `filter_coffee`/`espresso`: chip `":amount coffee|:amount coffees"`, else `"half a cup of coffee|:amount cups of coffee"`; `tea`: `":amount tea|:amount teas"` / `"half a cup of tea|:amount cups of tea"`; `cola`: `":amount soft drink|:amount soft drinks"` / `"half a can of soft drink|:amount cans of soft drink"`; `energy_drink`: `":amount energy drink|:amount energy drinks"` / `"half a can of energy drink|:amount cans of energy drink"`; unknown drink → `""`. `whole = if (halves >= 2) formatNumber((halves / 2).toDouble()) else ""` (integer division); `quantity = whole + if (halves % 2 == 1) "½" else ""`; `catalog.text(key, language, mapOf("amount" to quantity), count = halves.toDouble())`.
  - `usualDrinks(count: Int, language)` → `catalog.text(":count drink|:count drinks", language, mapOf("count" to formatNumber(count)), count.toDouble())`.
  - `caffeineAmount(amount: CaffeineAmountView, language)`: `mg = text(":n mg", n = formatNumber(mg))`; drinks = each `drink(…)` (not chip); none → `mg`; description = exactly 2 drinks → `text(":first or :second", …)`, otherwise the first only (a 3rd drink is dropped, as on iOS); result `text(":drinks (:mg)", drinks = description, mg = mg)`.
- `zoneLabel(zone: String, trip: TripDetailView? = null): String` (l.202–206): airports in order `trip.from, trip.to, then for each leg: leg.from, leg.to`; the first with `zone == zone` and a non-null `city` gives the city; otherwise the last non-empty `/`-separated segment of `zone` with `_` → space, or `zone` itself when there is none (`"UTC"` → `"UTC"`, `"America/Argentina/Buenos_Aires"` → `"Buenos Aires"`).
- `TimelineLabels.kt` (TimelineLabels.swift l.5–13, 21–44):
  - `CopyLineFormatter.dayDateLabel(day: DayView, language): String`: no `labelRange`, or `labelRange == labelDate` → `formatWeekdayDate(labelDate)`. Otherwise `formatWeekdayDate(labelDate, month = labelDate.take(7) != range.take(7)) + " – " + formatWeekdayDate(range)` (en dash U+2013, spaces around it).
  - `CopyLineFormatter.blockInterval(block: BlockView, options: CopyFormat, separator: String = " – "): String`: zone = `block.displayTz`; `dates = localDate(start, zone) != localDate(end, zone)`; `offsets = utcOffset(start, zone) != utcOffset(end, zone)`; each endpoint = `(if dates) formatDate(at, language, zone) + " "` + `formatInstant(at, options, zone)` + `(if offsets) " (" + utcOffset(at, zone) + ")"`.
  - `localDate(at: String, zone: String): String` → `YYYY-MM-DD` in the zone (invalid zone → UTC; invalid instant → `""`).
  - `utcOffset(at: String, zone: String): String` → `"GMT"` for offset 0, else `"GMT"` + `"-"` (U+002D, **not** U+2212) or `"+"` + `hh:mm` zero-padded.
- No `String.format`, `"…".format(…)`, `DateTimeFormatter.ofPattern/ofLocalized*`, `SimpleDateFormat`, `NumberFormat` or `Locale` anywhere in these files.

**Edge cases:** invalid zone → UTC everywhere (iOS falls back to the device zone inside `dateText`; Android must not read the device zone, OQ-1a-3). `annotateInstantZone` compares zone ids as strings. Plan date args (`"2026-10-21"`) become noon UTC, so the zone never moves the day.

**Tests**
- `CopyTests` (port of `CopyTests.swift`, same method names):
  - `intlGoldens`: decode `fixtures/intl-cases.json` (through 0d's `Fixtures`; Task 3 adds it, see below) into a test-local `@Serializable data class Golden(val id: String, val kind: String, val locale: String, val expected: String, val value: JsonPrimitive, val zone: String? = null, val timeFormat: String? = null, val style: String? = null)` (extra keys such as `label` are ignored by `ApiJson.response`); assert 44 cases; for each: `lang = Language.of(locale)`; `instant` → `formatInstant(value, CopyFormat(lang, TimeFormat.of(timeFormat ?: "24")), zone)`; `date` → `formatDate(value, lang, zone!!)`; `minutes` → `formatMinutes(number, lang, duration = style == "duration")`; else `formatOffset(number, lang)`. Collect all mismatches as `"<id>: <actual>"` and assert the list is empty, so one run shows every failure.
  - `caffeineCases`: halves `[1,2,3,4,5,8]` → en `half a cup of coffee, one cup of coffee, 1½ cups of coffee, two cups of coffee, 2½ cups of coffee, 4 cups of coffee`; nl `een half kopje koffie, één kopje koffie, 1½ kopje koffie, twee kopjes koffie, 2½ kopjes koffie, 4 kopjes koffie`; chips en `½ coffee, 1 coffee, 1½ coffees, 2 coffees, 2½ coffees, 4 coffees`; nl chips end with `" koffie"`. For tea, cola, energy drink, espresso × those halves × en/nl: non-empty, no `|`, no `:amount`, no `{`. `caffeineAmount(125 mg, [filter_coffee 3, tea 5])` → en `1½ cups of coffee or 2½ cups of tea (125 mg)`, nl `1½ kopje koffie of 2½ kopjes thee (125 mg)`; `caffeineAmount(125, [])` nl → `125 mg`; `usualDrinks(1, nl)` → `1 drankje`; `usualDrinks(2, en)` → `2 drinks`.
  - `argumentsPluralAndFallback`: `":count booked nights"` with count 2 → `2 booked nights` / `2 geboekte nachten`, count 1 → `1 booked night` / `1 geboekte nacht`; unknown key `"Unknown :place and :place2"` → `Unknown Tokyo and :place2`; caffeine arg `{name: mg, kind: milligrams, value: 125, drinks: [], usualDrinks: 2}` in `"Coffee nap: have about :mg, then nap."` contains `2 drinks` / `2 drankjes`; the inline-table en fallback; `formatArgument(instant "2026-01-15T00:00Z", tz Asia/Tokyo, CopyFormat(en, displayZone = "UTC", annotateInstantZone = true))` → `09:00 (Tokyo)`; `text` kind `"verbatim"` → `verbatim`; milligrams 125 → `125 mg`.
- `LabelTests.datesZonesAndIntervals` (port, all of it): day labels `Thu 15 Oct`, `do 15 okt`, `(2026-10-15, 2026-10-15)` → `Thu 15 Oct`, `(2026-10-15, 2026-10-16)` → `Thu 15 – Fri 16 Oct`, `(2026-10-31, 2026-11-01)` → `Sat 31 Oct – Sun 1 Nov` / `za 31 okt – zo 1 nov`; `formatDayMonth("2026-03-29", en)` → `29 Mar`; `formatMonthYear("2026-03-29", nl)` → `mrt 2026`; `localDate("2026-10-14T23:45Z", "Asia/Tokyo")` → `2026-10-15`, `America/New_York` → `2026-10-14`; block Tokyo `14:30Z–22:30Z` → `14 Oct 2026 23:30 – 15 Oct 2026 07:30`; block Amsterdam `2026-10-25T00:30Z–01:30Z` → `02:30 (GMT+02:00) – 02:30 (GMT+01:00)`; `utcOffset("2026-01-01T00:00Z", "America/New_York")` → `GMT-05:00`; the `zoneLabel` cases (`New York`, `Boston` after `from.city = null`, `Narita` from a leg, `Buenos Aires`, `UTC`); annotated 12 h instant → `09:00 AM (Narita)`; the 12 h table `00:05→12:05 AM/12:05 a.m.`, `03:00→03:00 AM/03:00 a.m.`, `11:59→11:59 AM/11:59 a.m.`, `12:00→12:00 PM/12:00 p.m.`, `15:00→03:00 PM/03:00 p.m.`, `23:59→11:59 PM/11:59 p.m.`.
- `LocaleIndependenceTest` (new): with `Locale.setDefault(Locale.forLanguageTag("ar-EG"))` and `TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))` in `@Before` (restored in `@After`), re-run a representative subset (4 intl goldens, `formatNumber(1500.0, nl)`, a 12 h instant, `dayDateLabel`) and get identical strings with ASCII digits.
- `CalendarTablesTest` (new): for each month of 2026 (`2026-MM-15`) `formatDayMonth` gives `15 <month>` in en and nl; for 16–22 Nov 2026 (Mon–Sun) `formatWeekday` gives the weekday tables; plus every row of Appendix A.1 (includes September: `15 Sep 2026`, `Tue 15 Sep`, `15 sep 2026`, `di 15 sep`).

**Fixture copy** (0d §9 hand-off; do not copy by hand): append the row `("Packages/ChronoFlowKit/Tests/PlanCoreTests/Fixtures", ["intl-cases.json"])` to `SOURCES` in `tools/contracts-gen/copy_fixtures.py` (0d already supports a row that names one `.json` file, written to the fixtures root), add a case for it to that script's Python test if 0d's test lists rows, and run `make contracts`. Result: `core/testing/src/main/resources/fixtures/intl-cases.json`, byte-identical to iOS at the pin (**[F]** 9,992 B, SHA-256 `c42a3a551343391245e976a7b9261639b26b9b82608de9cac1af70e577f4ceef`), with its line in `fixtures/fixtures.sha256`, so `FixtureManifestTest` and `make generators-check` cover it. Commit the script change and its output together. **[F]** The 44 cases are 28 `instant`, 6 `date`, 6 `minutes`, 4 `offset`; `value` is a string in 34 and an integer in 10, so decode it as `JsonPrimitive` and wrap it as `CopyArgValue.Text(content)` when `isString`, else `CopyArgValue.Numeric(BigDecimal(content))`.

### Task 4. Plan decoding

Commit: `feat(plan): decode plan detail bytes with a per-trip cache`. Ticks `plan.md` Phase 1a "**Plan decoding.**".

**Files**
- `core/model/src/main/kotlin/net/chronoflow/core/model/store/PlanVersionRecord.kt`, `CheckMarkRecord.kt`, `TripPlanSnapshot.kt` (package `net.chronoflow.core.model.store`; 4a and 4d expect them here and do not create them).
- `core/model/src/main/kotlin/net/chronoflow/core/model/CheckOffChange.kt`, always: 1a owns the type, and 4b (which depends on 1a) uses it instead of declaring it. It is not `@Serializable`: 4d adds the storage codec.
- `core/plan/…/PlanDecoder.kt`.

**Value types** (plain Kotlin; no database code; Phase 4a maps its rows into them)
- `class PlanVersionRecord(val tripId: String, val inputHash: String, val detail: ByteArray, val receivedAt: String, val detailVersion: String? = null)` with `equals`/`hashCode` using `detail.contentEquals`/`contentHashCode` (a `data class` with a `ByteArray` compares by reference).
- `data class CheckMarkRecord(val tripId: String, val inputHash: String, val type: String, val localDate: String, val ordinal: Int, val status: String, val markedAt: String)`.
- `data class CheckOffChange(val tripID: String, val planInputHash: String, val type: BlockType, val localDate: String, val ordinal: Int, val status: CheckStatus?)` (iOS `OutboxChange.swift:3–29`), in package `net.chronoflow.core.model`, **not** `@Serializable`: this is exactly 4b's declaration, and 4d owns the storage codec (4b checklist; 4d OQ-4d-15). The check-off wire body is 0d's `ApiCheckOffInput`.
- `data class TripPlanSnapshot(val plan: PlanVersionRecord?, val checks: List<CheckMarkRecord> = emptyList(), val pendingChecks: List<CheckOffChange> = emptyList())`. The iOS `trip: TripRecord?` field is added by the phase that ports `TripRecord` (6a/6b), see OQ-1a-5.

**PlanDecoder** (port of `PlanDecoder.swift:65–96, 111–115`; the iOS type is an actor)
- `class PlanDecoder(private val worker: CoroutineDispatcher, private val json: Json = ApiJson.response)` (0d hand-off). Decode with `json` directly, **not** through `TripDetailResponse.decode`: iOS `PlanVersionRecord.decodedDetail()` is a plain `JSONDecoder` decode of the stored bytes (`PlanVersionRecord.swift:23–25`, `JSONDocument.swift`); legacy cleaning already happened when the bytes were fetched (`TripDetailResponse`, 0d). All state is guarded by one `Mutex`; all decoding runs inside `withContext(worker)` (production: `Dispatchers.Default`; the caller never decodes on the main thread).
- `suspend fun detail(record: PlanVersionRecord): TripDetail`: key = `(tripId, inputHash, detailVersion)`. A cached entry with the same key **and** `contentEquals` bytes is returned as is. Otherwise decode `json.decodeFromString(TripDetail.serializer(), record.detail.decodeToString())` (or a byte-stream decode), increment `decodeCount`, drop every cache entry of the same trip, store the new one. Decode errors (`SerializationException`, `IllegalArgumentException`) propagate unchanged; the caller decides (iOS "Fix round 1", Phase 6b). Never catch `Exception`.
- `suspend fun detail(snapshot: TripPlanSnapshot): TripDetail?` is added in **Task 5** (it needs `mergedCheckState`; adding it here would break this commit's build).
- `suspend fun evict(tripID: String)` drops that trip's entries.
- `@Volatile var decodeCount: Int = 0; private set` (written under the mutex, readable in tests without suspending; iOS exposes it the same way).
- The bytes are never re-encoded or normalized: `PlanVersionRecord.detail` is what the server sent.
- Tolerant enums come from 0d: an unknown `type`, `lane`, `priority`, `labelPhase` or `status` decodes to its `Unknown(raw)` case; unknown fields are ignored.

**Tests**
- `PlanDecoderTests` (new): `S1`–`S6` and `S6-RETURN-10N` decode as `PlanFixture` (`@Serializable data class PlanFixture(val plan: PlanView, val timeline: TimelinePresentationView, val notices: PlanNoticesView)` in `PlanCoreTestSupport.kt` from Task 3, the iOS `TimelineTests.swift:7–11` type) through `ApiJson.response`; `trips/detail.json` decodes through `PlanDecoder.detail` as a full `TripDetail` with `detailVersion == "v1"`; a copy of `detail.json` with one block's `"type"` set to `"future_type"` and `"lane"` to `"future_lane"` decodes, and that block's type is `Unknown("future_type")`; decoding runs on the injected worker: pass `CountingDispatcher` (Task 2; `class CountingDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() { @Volatile var dispatches = 0; override fun dispatch(context: CoroutineContext, block: Runnable) { dispatches++; delegate.dispatch(context, block) } }`) wrapping `StandardTestDispatcher(testScheduler)` and assert `dispatches > 0` after one `detail(record)` call (no production test hook); `evict` forces the next call to decode again.
- `ModelTests.versionAndRawPayloadInvalidateCache` (port of `ModelTests.swift:134–153`; `value` = `trips/detail.json` decoded; `record = PlanVersionRecord(value.trip.id, value.plan.meta.inputHash, ApiJson.response.encodeToString(TripDetail.serializer(), value).encodeToByteArray(), "now", value.detailVersion)`, iOS `.encoding(value)`): two calls with the same record → `decodeCount == 1`; new `detailVersion` (`"new-version"`, re-encoded bytes) → result has it and `decodeCount == 2`; same key, bytes with `plan.blocks` emptied → result has no blocks and `decodeCount == 3`. (The iOS `decodedOnMainThread` assertion has no JVM equivalent; the worker assertion above replaces it.)

All coroutine tests use `runTest(timeout = 60.seconds)`.

### Task 5. CheckState and mark merging

Commit: `feat(plan): port check identity, check state and pending mark merging`. Ticks nothing (the combined checkbox is ticked in Task 7).

**Files:** `core/plan/…/CheckState.kt`; `core/plan/…/PlanDecoder.kt` (add `suspend fun detail(snapshot: TripPlanSnapshot): TripDetail?`: `null` when `snapshot.plan == null`; else `detail(snapshot.plan)` and return `decoded.copy(checkState = mergedCheckState(decoded.plan, snapshot.plan.inputHash, snapshot.checks, snapshot.pendingChecks))`, never mutating the cached instance; port of `PlanDecoder.swift:76–96`).

**Behaviour**
- `fun checkIdentity(key: CheckKeyView): String = "${key.type.raw}|${key.localDate}|${key.ordinal}"` (`CheckState.swift:4–6`). `raw` is the wire string (`seek_light`, or the raw text of an `Unknown`).
- `fun buildCheckState(plan: PlanView, marks: List<CheckMark>): CheckState` (`CheckState.swift:8–27`):
  - `actionable = plan.blocks.filter { it.actionable }`.
  - `current`: identity → block for actionable blocks with a `checkKey`; on duplicate identities the **last** block wins.
  - `matching = marks.filter { checkIdentity(it.key) in current }`, order kept.
  - For each `day` in `plan.days` (order kept): `startsOnDay(b) = dayStart <= bStart && bStart < dayEnd` on `planDate` instants; any `null` (unparseable) instant makes it `false` (NaN semantics). `dayMarks = matching.filter { current[identity]?.let(startsOnDay) == true }`; `done` = count of `CheckStatus.Done`, `skipped` = count of `CheckStatus.Skipped` (an `Unknown` status counts in neither); `total = actionable.count(startsOnDay)` — every actionable block that starts on the day, with or without a `checkKey`, optional or not (iOS counts the plan it is given).
  - Result `CheckState(planInputHash = plan.meta.inputHash, marks = matching, days = …DayCheckProgressView(dayId = day.id, done, skipped, total))`.
- `fun mergedCheckState(plan: PlanView, inputHash: String, checks: List<CheckMarkRecord>, pendingChecks: List<CheckOffChange>): CheckState` (port of `PlanDecoder.swift:79–94`):
  1. A `LinkedHashMap<String, CheckMark>`. For each stored row **whose `inputHash` equals the record's `inputHash`**, in the given order (the store sorts by `marked_at`): key = `CheckKeyView(type = BlockType.of(row.type), localDate = row.localDate, ordinal = row.ordinal)`, mark = `CheckMark(key = key, status = CheckStatus.of(row.status), markedAt = row.markedAt)`; put by identity (later rows overwrite).
  2. Then for each pending change in order (outbox FIFO): status non-null → put `CheckMark(key, status, markedAt = "")`; status null → remove the identity.
  3. **Pending changes are not filtered by `planInputHash`** (iOS parity: a check made before a re-plan still shows on the new plan while it is unsent; `StepCheckTests.swift:47–54` relies on this: after the re-plan to `inputHash = "replacement"` the pending `done` still shows).
  4. `buildCheckState(plan, marks.values.sortedBy { checkIdentity(it.key) })` (plain `String` ordering).

**Tests**
- `ModelTests.localProgressCountsStartsAndKeepsSkippedSeparate` (port of `StepCheckTests.swift:80–90`, uses `trips/detail.json`): first two actionable blocks with a `checkKey` → marks `done`, `skipped` → `state.marks.size == 2`, Σ`done` == 1, Σ`skipped` == 1.
- `PlanDecoderTests.snapshotMergesMarks` (new, added to Task 4's class): `detail(TripPlanSnapshot(plan = null))` is `null`; a snapshot with one stored `done` row for the first actionable block of `trips/detail.json` returns a detail whose `checkState.marks.size == 1`, while a second `detail(record)` call still returns the cached instance with empty marks (no mutation) and `decodeCount == 1`.
- `CheckMergeTests` (new): stored `skipped` + pending `done` for the same identity → `done` with `markedAt == ""`; pending `null` removes a stored mark; a stored row with another `inputHash` is ignored; a pending change with another `planInputHash` still applies; a mark whose identity matches no actionable block is dropped from `marks` and from the counts; result marks are sorted by identity; an `Unknown("maybe")` stored status is kept in `marks` but counted in neither `done` nor `skipped`; a block starting exactly at `day.endUtc` counts on the next day, not this one; a block whose `startUtc` does not parse counts on no day.

### Task 6. PlanLogic

Commit: `feat(plan): port optional steps, notices, summaries and label keys`. Ticks nothing yet.

**File:** `core/plan/…/PlanLogic.kt` (port of `PlanLogic.swift:1–128`).

**Behaviour**
- `fun isOptionalStep(block: BlockView): Boolean`: `avoid_light`/`seek_light` → `priority == optional`; `caffeine_ok` → always `true`; `light_ok`/`light_neutral` → `priority == info`; anything else (including `Unknown`) → `false`.
- `fun withoutOptionalSteps(plan: PlanView): PlanView` = `plan.copy(blocks = plan.blocks.filterNot(::isOptionalStep))`. The input is never mutated; idempotent.
- `fun tripDisplayStatus(card: TripCardView, planStartUtc: String?, planEndUtc: String?, now: Instant): TripStatus`: if `card.progress.status == completed` or either bound is `null` → `card.progress.status`. Else `now >= end` → `completed`; `start <= now` → `active`; else `upcoming`. An unparseable bound behaves like NaN: an unparseable end is never reached, an unparseable start is never passed (→ `upcoming` unless completed by end).
- `data class NoticeEntry(val note: NoteView, val warning: Boolean)`; `fun noticeItems(plan, notices: PlanNoticesView, day: DayView): List<NoticeEntry>`: warning indexes from the first `notices.days` entry with `dayId == day.id` (none → empty); in-range indexes map to `NoticeEntry(plan.warnings[i], warning = true)`, out-of-range ones are dropped; then `day.notes` as `NoticeEntry(note, warning = note.role == warning)`; de-duplicate by `note` equality keeping the first position and OR-ing `warning`.
- `fun dayZoneLabel(day: DayView, trip: TripDetailView? = null): String`: unique `tz` values of `day.zones` in first-seen order, each through `zoneLabel`, joined with `" → "` (U+2192).
- `data class DisplayInterval(val start: Instant, val end: Instant, val tz: String)`; `fun summaryIntervals(day: DayView, blocks: List<BlockView>, type: BlockType): List<DisplayInterval>`: for blocks of that `type` that are not optional; for `BlockType.Sleep` only blocks whose `params` is `BlockParams.Sleep` with `value.kind` `SleepKind.Main` or `SleepKind.InFlightMain`; clip to `[max(dayStart, blockStart), min(dayEnd, blockEnd))`, skip if empty; split by each `day.zones` entry (clip again, skip empty); sort by `start` (stable); merge neighbours with the same `tz` when `last.end >= next.start` (end = max). Unparseable instants → the block is skipped.
- `fun stepLabelKey(type: BlockType): String?` — `sleep "Sleep"`, `nap "Nap"`, `rest "Rest"`, `seek_light "Seek bright light"`, `light_ok "Normal light is fine"`, `light_neutral "Indoor light"`, `avoid_light "Avoid bright light"`, `caffeine_serving "Caffeine serving"`, `caffeine_routine "Caffeine routine"`, `caffeine_ok "Caffeine window"`, `no_caffeine "Avoid caffeine"`, `flight "Flight"`, `layover "Layover"`, `transfer "Airport transfer"`, `movement_break "Movement break"`, `zone_change "Time-zone change"`, `note "Note"`, unknown → `null`.
- `fun dayLabelKey(phase: LabelPhase): String?` — `pre_departure "Before departure"`, `travel "Travel day"`, `travel_arrival_night "Travel · arrival night"` (U+00B7), `arrival_day_0 "Arrival day"`, `day "Day :count"`, unknown → `null`.
- `fun dayStatusLabelKey(status: DayStatus): String?` — `adapting "Adjusting"`, `partially_adapted "Partly adapted"`, `adapted "Expected adapted"`, `adapted_slower_clock "Protect a slower clock"`, unknown → `null`.
- The keys are plan-copy keys: they resolve through `CopyCatalog`, not through Android string resources.
- Optional steps are **always** hidden: `withoutOptionalSteps` takes no flag, and nothing in this phase reads the settings `showOptionalItems` or sends `show_optional_items` (plan.md Phase 1a task and A24: the optional-items switch is omitted, as on iOS). Do not add a "show optional" parameter anywhere.
- Hidden categories (settings `hiddenCategories`) are applied by layout in 1b (`layoutDay(…, hidden:)`); this phase adds no separate hidden filter beyond what `PlanLogic.swift` has.

**Tests**
- `LabelTests.optionalRulesAndUnknowns` (port, **isOptionalStep half only**): the full 17 types × 4 priorities matrix against `{avoid_light: [optional], seek_light: [optional], caffeine_ok: [core, recommended, optional, info], light_ok: [info], light_neutral: [info]}`. The two `layoutDay` assertions (l.25–36) are added to this same method by Phase 1b.
- `LabelTests.summaryClippingSplittingAndMerging` (port, all but the final `nowLabel` assertion l.140–143, which 1b adds): day `2026-10-14T22:00Z–2026-10-15T22:00Z` with zones Amsterdam/Tokyo/Amsterdam; one long main sleep → 3 intervals with tz `[Europe/Amsterdam, Asia/Tokyo, Europe/Amsterdam]`, first start = day start, last end = day end; four sleeps → 3 intervals, the middle one `00:00Z–05:00Z`; an `inFlightRecovery` sleep → empty; sleeps entirely before/after the day → empty; `dayZoneLabel(day) == "Amsterdam → Tokyo"`.
- `LabelTests.noticesStatusAndLabelCatalog` (port, all): every block type in S1 has a step-label key that the catalog contains; every known `LabelPhase` and `DayStatus` has a key the catalog contains; duplicate notes and duplicate warning indexes collapse to `[NoticeEntry(note, warning = true)]`; `tripDisplayStatus` at minutes −1/0/59/60 → `upcoming/active/active/completed`, missing start → `upcoming`, a completed card stays completed.
- `TimelineTests.emittedLanesPresentationAndEmptyPlan` stays in 1b, including its S1 `seekLight` interval assertion.

### Task 7. PlanClock

Commit: `feat(plan): add plan clock interface and test clocks`. Ticks `plan.md` Phase 1a "**CheckState, PlanLogic and PlanClock.**".

**Files:** `core/plan/…/PlanClock.kt`, `core/plan/…/FixedPlanClock.kt`, `core/testing/src/main/kotlin/net/chronoflow/core/testing/FakePlanClock.kt`.

**Behaviour**
- `interface PlanClock { fun values(): Flow<Instant> }` (Swift `values() async -> AsyncStream<Date>`). Contract for implementations: emit the current instant on collection, then each time the plan time should be re-read (minute boundary, foreground, time or zone change; the Android implementation is Phase 1d's `SystemPlanClock`).
- `class FixedPlanClock(val now: Instant) : PlanClock` → `flowOf(now)` (emits once and completes, as the Swift one finishes).
- `class FakePlanClock(start: Instant) : PlanClock` (port of the iOS test `TestPlanClock`, `ModelTests.swift:9–26`): each collector gets the current instant first, then every `advance(to: Instant)`, including repeats of the same instant; buffering keeps only the newest (`MutableSharedFlow(replay = 1, onBufferOverflow = DROP_OLDEST)`). `val now: Instant` reads the current value.

**Tests:** `PlanClockTests` (Turbine, `runTest(timeout = 60.seconds)`): `FixedPlanClock` emits once and completes; `FakePlanClock` gives the start to a late collector, then `advance` values in order, and a repeated instant is emitted again.

### Task 8. Time hygiene rule

Commit: `test(konsist): forbid default locale, zone and clock reads in logic modules`. Ticks `plan.md` Phase 1a "**Time hygiene.**" (the `ZoneRules` cache landed in Task 1; "cached `DateTimeFormatter`s" is met by using none, OQ-1a-9).

**Files** (0a Task 6b layout; do not restructure it):
- `konsist/src/test/kotlin/net/chronoflow/konsist/KonsistRules.kt`: `fun noDefaultLocaleZoneOrClockInLogic(scope: KoScope)`.
- `konsist/src/test/kotlin/net/chronoflow/konsist/KonsistRulesTest.kt`: `noDefaultLocaleZoneOrClockInLogic()` (project scope, must pass), `noDefaultLocaleZoneOrClockInLogicRejectsPlantedViolation()` (must throw), `noDefaultLocaleZoneOrClockInLogicAllowsControl()` (must pass).
- Planted violations, module-shaped so 0a's `moduleOf`/`sourceSetOf` resolve them: `konsist/src/test/resources/violations/noDefaultLocaleZoneOrClockInLogic/core/plan/src/main/kotlin/net/chronoflow/core/plan/<Pattern>.kt`, one file per forbidden pattern below (for example `UsesLocaleGetDefault.kt`, `UsesInstantNow.kt`, `UsesStringFormat.kt`, `UsesLiteralFormat.kt`), plus one under `…/core/sync/src/main/…` to prove the module list.
- Allowed controls under `konsist/src/test/resources/allowed/noDefaultLocaleZoneOrClockInLogic/`: `core/plan/src/test/kotlin/…/TestUsesNow.kt` (a test source set may call `Instant.now()`), `core/plan/src/main/kotlin/…/generated/Gen.generated.kt` (generated files are exempt), and `core/ui/src/main/kotlin/…/UiUsesLocale.kt` (modules outside the list are not checked).

**Rule:** in files with `moduleOf(path)` in `core/plan`, `core/sync`, `core/reminders`, `core/wizard` and `sourceSetOf(path) == "main"`, excluding `*.generated.kt` and paths containing `/generated/`, no file may contain:
- from `plan.md`: `Locale.getDefault`, `TimeZone.getDefault`, `ZoneId.systemDefault`, `System.currentTimeMillis`;
- added by this brief (OQ-1a-2): `Instant.now(`, `LocalDate.now(`, `LocalDateTime.now(`, `ZonedDateTime.now(`, `OffsetDateTime.now(`, `Clock.systemDefaultZone`, `Clock.systemUTC`, `SimpleDateFormat`, `Calendar.getInstance`, `DateTimeFormatter.ofLocalized`, `String.format(` and a string literal followed by `.format(` (regex `"\s*\.format\(`), because they read the default locale or the system clock.
- Match the file text (`KoFileDeclaration.text`) with plain `contains` for the literal patterns and one precompiled `Regex` for the literal-`.format(` pattern. The failure message lists every offending `path: pattern`.

**Tests:** the three tests above. `…RejectsPlantedViolation()` additionally asserts that the failure message names **every** planted file (0c's convention for multi-pattern rules), so a pattern the rule misses fails the test. The real modules pass.

### Task 9. Fixture-wide ported tests and the porting map

Commit: `test(plan): port fixture-wide copy resolution and record the test map`. Ticks `plan.md` Phase 1a "**Ported tests.**".

**Files:** `core/plan/src/test/…/TimelineTests.kt` (`PlanCoreTestSupport.kt` already exists from Task 3). Do not add a porting document: the map is Appendix B of this brief, and the commit body lists the ported methods.

**Test support** (`PlanCoreTestSupport.kt`, port of `TimelineTests.swift:7–37`, reused by 1b): `fixture(name = "S1"): PlanFixture` (loads `fixtures/plans/<name>.json` once per name and caches it in the test process); `at(minute: Double): String` = ISO-8601 **always with three fraction digits** of `1_767_225_600 s + minute × 60 s` (`at(0) == "2026-01-01T00:00:00.000Z"`, `at(-1) == "2025-12-31T23:59:00.000Z"`; Swift `ISO8601Format(includingFractionalSeconds: true)`). Do not use `Instant.toString()`, which drops `.000`; build the string by hand or with one `DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)` in the test file (tests are outside the Konsist rule); `sampleDay(hours = 24.0)`: S1 day 0 with `startUtc = at(0)`, `endUtc = at(hours × 60)`, one Amsterdam zone over the whole day; `block(id, start, end)`: the first S1 block of type `BlockType.Sleep`, copied with `id = id`, `startUtc = at(start)`, `endUtc = at(end)`; `PlanFixture` (Task 4 uses it); `fixture()` decodes with `ApiJson.response` through 0d's `Fixtures`. Load the `CopyCatalog` once per test class (`@BeforeClass` or a lazy companion), not per method, to keep each test under 1 s.

**Tests**
- `TimelineTests.everyFixturePreservesLayoutAndResolvesCopy` (port of the **copy half** of `TimelineTests.swift:199–270`; 1b adds the layout half to the same method): for `S1, S2, S3, S4, S5, S6, S6-RETURN-10N`: decoding twice yields equal plans (no mutation); `withoutOptionalSteps(withoutOptionalSteps(p)) == withoutOptionalSteps(p)` and the original is unchanged; walk the fixture's **`plan`** subtree as a `JsonElement` (iOS walks `JSONEncoder().encode(f.plan)`, l.251, so `timeline` and `notices` are not walked) and, for every object that has a string `key` and an array `args` and decodes as `CopyLine` (`ApiJson.response.decodeFromJsonElement`, catching only `SerializationException`/`IllegalArgumentException` to skip non-lines), assert `catalog.contains(key)` and that `format(line, CopyFormat(lang))` is non-empty for `Language.En` and `Language.Nl`. Collect failures as `"<fixture>: <key>"` into one list and assert it is empty.
- `CopyTests.reviewFixturesResolve` (new, from iOS 14a's test list "every key in S1–S6 and the review fixtures resolves in en and nl"): the same walk over `fixtures/wizard/review.json` and `fixtures/wizard/needs-answers.json`. **[F]** at the pin all 1 + 2 keys of these two fixtures resolve, as do all 1,630 `CopyLine` keys in the `plan` subtrees of the seven plan fixtures (1,701 when `timeline` and `notices` are included) and the 240 of `trips/detail.json` (checked against `PlanCopy.json` at the pin on 2026-11-16).

---

## 7. Acceptance criteria

1. `make quick` passes (Spotless, Konsist including the new rule, every JVM test of `:core:*` and `:tools:*`). Each new test class appears in `build/test-results/<tier>/timings.json` under 1 s per test.
2. `make targeted ONLY="core:plan:CopyTests core:plan:LabelTests core:plan:TimelineTests core:plan:ModelTests core:plan:PlanDecoderTests core:plan:CheckMergeTests core:plan:CopyCatalogTableTests core:plan:PlanDateTest core:plan:DateTextTest core:plan:CalendarTablesTest core:plan:NumberTextTest core:plan:LocaleIndependenceTest core:plan:PlanClockTests konsist:KonsistRulesTest"` passes (module paths without the leading colon, 0a `Scripts/targeted.py`).
3. `./gradlew :core:plan:dependencies --configuration runtimeClasspath` shows no `androidx.*`, `com.android.*` or Compose artifact; the A3 Konsist rule ("JVM core modules import no `android.*` or Compose") passes for `:core:plan`.
4. `make generators-check` passes: `plan_copy.json` (0c) and `intl-cases.json` (through `copy_fixtures.py`) are byte-identical to the iOS pin and listed in their manifests.
5. All 44 intl goldens pass (`CopyTests.intlGoldens` reports `44` cases and 0 mismatches).
6. `make check` passes on the branch (the orchestrator runs the emulator part; this phase adds no emulator test).
7. Every Phase 1a checkbox in `plan.md` is ticked by the commit that completes it; nine atomic Conventional Commits, no trailers.
8. No screenshots and no perf numbers are required in this phase (no UI; the plan-preparation budgets are 1b's).

---

## 8. Review checklist (Opus reviewer)

Verify each item by reading the diff and, where noted, running one targeted test.

- [ ] **Strings are exact.** Spot-check against Appendix A and the iOS tests: en dash `–` (U+2013) in day ranges and intervals, arrow `→` (U+2192) in zone labels, minus `−` (U+2212) only in `formatOffset`, hyphen `-` in `utcOffset` and negative numbers, `½` (U+00BD), `·` (U+00B7) in "Travel · arrival night", nl `a.m.`/`p.m.` with dots, no dot after Dutch months.
- [ ] **No CLDR leakage.** `grep -rnE 'DateTimeFormatter\.ofPattern|ofLocalized|SimpleDateFormat|NumberFormat|DecimalFormat|String\.format|\.format\(|Locale\.' core/plan/src/main` returns nothing (a strict ISO parse formatter, if used, is the only allowed `DateTimeFormatter`). Run `LocaleIndependenceTest`.
- [ ] **12 h hours are zero-padded** (`03:00 PM`, not `3:00 PM`) and midnight/noon are `12:xx AM`/`12:xx PM`; `TimeFormat.Unknown` behaves as 24 h.
- [ ] **Number rounding** uses `BigDecimal.valueOf` (shortest repr) + `HALF_EVEN`; `1234567.8915` → `1,234,567.892`; negative zero prints `-0`.
- [ ] **`CopyArgValue.text`** for numbers drops trailing zeros and never shows an exponent; `count` is taken only from a `count`-kind arg named `count`.
- [ ] **Coded and plural selection** follows Swift l.21–25 exactly: cases first (exact vs `>=`), plural `count == 1.0` → form 0, otherwise form `min(1, size-1)`.
- [ ] **Unknown key fallback** interpolates only the provided names and leaves `:place2` alone; missing language falls back to `en`.
- [ ] **`date` args are formatted in UTC** even when `tz` is set (iOS parity), and `instant` annotation compares the resolved zone with `displayZone`.
- [ ] **Merge semantics:** stored rows filtered by `inputHash`, pending changes not filtered, null pending status removes, pending `markedAt == ""`, sorted by identity before `buildCheckState`. `total` counts every actionable block starting on the day.
- [ ] **Decoder cache:** keyed by `(tripId, inputHash, detailVersion)` plus `contentEquals` bytes; one entry per trip; `evict` works; decode runs on the injected dispatcher; decode errors are not swallowed; no `catch (e: Exception)` and no `runCatching` around suspending work (AGENTS "Traps": rethrow `CancellationException`).
- [ ] **Shared types:** `CheckOffChange` exists exactly once in `:core:model`, is not `@Serializable`, and matches 4b's shape; `PlanVersionRecord`/`CheckMarkRecord`/`TripPlanSnapshot` are in `net.chronoflow.core.model.store`. Nothing reads `showOptionalItems` or mentions `show_optional_items`.
- [ ] **No static state:** no top-level `val` that loads or computes at class init (the empty `planZoneRules` memo is the one allowed shared value); `CopyCatalog` loads only through `load`/`CopyCatalogProvider`.
- [ ] **Swift names kept:** types `CopyCatalog`, `CopyFormat`, `CopyLineFormatter`, `NoticeEntry`, `DisplayInterval`, `PlanDecoder`, `PlanClock`, `FixedPlanClock`; functions `checkIdentity`, `buildCheckState`, `isOptionalStep`, `withoutOptionalSteps`, `tripDisplayStatus`, `noticeItems`, `dayZoneLabel`, `summaryIntervals`, `stepLabelKey`, `dayLabelKey`, `dayStatusLabelKey`, `planDate`, `zoneLabel`, `localDate`, `utcOffset`, `dayDateLabel`, `blockInterval`; test methods as in Appendix B.
- [ ] **Scope:** nothing from `Timeline.swift`, geometry, `nowLabel`, `Today.swift`, Android APIs or UI slipped in; no generated file or `plan_copy.json` was edited.
- [ ] **Tests follow AGENTS.md "Tests":** JVM only, no tier annotation, each < 1 s, `runTest(timeout = 60.seconds)`, no `Thread.sleep`, loops collect all failures into one assertion, catalog loaded once per class.
- [ ] **Konsist rule** rejects each planted file and does not flag `:core:plan`'s own (clean) sources.
- [ ] Typical mistakes to look for: `"%02d".format(h)` (default locale, Arabic digits); `ZoneId.of(tz)` per call without the cache; `java.time` `DayOfWeek.getDisplayName`; `String.lowercase()` without `Locale.ROOT` (should not be needed at all); using `DateTimeFormatter.ISO_OFFSET_DATE_TIME` unmodified (accepts `HH:mm+02:00`); `Math.floorMod` vs `%` for pre-1970 instants; `data class` with `ByteArray` equality.

---

## 9. Open questions (proceed on the default)

| # | Question | Default (build this) |
|---|---|---|
| OQ-1a-1 | `plan.md` puts `TimelineLabels.kt` in Phase 1b, but iOS `LabelTests.datesZonesAndIntervals` needs `dayDateLabel`, `blockInterval`, `localDate` and `utcOffset`, which are pure formatting. | Port those four in 1a; 1b adds only `nowLabel` (it needs `DayLayout`). 1b's brief must say so. |
| OQ-1a-2 | Extend the plan's Konsist list (`Locale.getDefault`, `TimeZone.getDefault`, `ZoneId.systemDefault`, `System.currentTimeMillis`) with `Instant.now`, `*.now(`, `Clock.system*`, `SimpleDateFormat`, `Calendar.getInstance`, `DateTimeFormatter.ofLocalized*` and `String.format`? | Yes. They are implicit default-locale or system-clock reads, which A17 and the plan clock exist to remove. |
| OQ-1a-3 | iOS `dateText` falls back to the **device** zone for an unknown tz id (`formatter.timeZone = nil`), while `formatInstant`, `localDate` and `utcOffset` fall back to GMT. | Android uses UTC everywhere (the device zone is banned in logic). Plan data always carries IANA ids, so no visible difference is expected. |
| OQ-1a-4 | iOS crashes (`parts.hour!`) on an unparseable instant in `formatInstant` and prints NaN dates unpredictably. | Return `""` for an unparseable instant or date; never crash. |
| OQ-1a-5 | `TripPlanSnapshot.trip` (iOS `TripRecord?`) has no Android type yet. | Omit it now; the phase that ports `TripRecord` (6a/6b) adds it. |
| OQ-1a-6 | Where do the store value types (`PlanVersionRecord`, `CheckMarkRecord`, `TripPlanSnapshot`) and `CheckOffChange` live? | `:core:model` (`net.chronoflow.core.model.store` and `net.chronoflow.core.model`), so `:core:database` (4a) and `:core:sync` (4d) can produce them without depending on `:core:plan`. |
| OQ-1a-7 | `plan_copy.json` is read with `Class.getResourceAsStream`, which on Android opens the APK's Java resources (tens of ms on a first call). | Acceptable: it is read once, off the main thread, through `CopyCatalogProvider`. If 1e or 6b traces show it on the critical path, move the file to `assets/` behind the same `CopyCatalog(data)` constructor. |
| OQ-1a-8 | iOS `formatNumber` handles `NaN`/infinity through `NumberFormatter` (never seen in plan data). | Print `NaN`, `∞`, `-∞`; no golden. |
| OQ-1a-9 | `plan.md` Phase 1a "Time hygiene" asks for "cached `DateTimeFormatter`s", but A17, `10 §8.3` and `04 §9.3` forbid `DateTimeFormatter` in formatting paths (CLDR "Sept"/"sep.", per-block cost). | Use none in main code: hand-built strings over fixed tables, and a hand-written `planDate` parser. The checkbox is met by the `ZoneRules` cache and the absence of formatters. |
| OQ-1a-10 | 0d §4.2 prefers one test case per row (no loops inside a `@Test`); the iOS tests this phase ports (`intlGoldens`, `caffeineCases`, the `isOptionalStep` matrix, the fixture walk) loop inside one Swift-named method. | Keep the Swift names and the loops (AGENTS: "ported tests keep the Swift test names"); each row is a sub-millisecond pure call, and every loop collects all mismatches into one assertion so one run shows every failure. New Android-only table tests (`DateTextTest`, `NumberTextTest`, `CalendarTablesTest`) may use plain loops the same way. |
| OQ-1a-11 | Apple's ISO parser is lenient beyond the formats plan data uses (`z`, `±HH`, `±HHMM`, day/hour/second overflow rolls over). | Match the measured behaviour (Task 1, Appendix A.3); no plan fixture depends on it. |

None of these needs the user before 1a merges; they are recorded here and the orchestrator copies OQ-1a-1 to the 1b brief.

---

## 10. Hand-off to Phase 1b

- 1b adds to `LabelTests.optionalRulesAndUnknowns` the two `layoutDay` assertions (`LabelTests.swift:25–36`) and to `LabelTests.summaryClippingSplittingAndMerging` the `nowLabel` assertion (l.140–143, expects `10:00`).
- 1b adds the layout half of `TimelineTests.everyFixturePreservesLayoutAndResolvesCopy` and ports `dimensionsPackingAndTinyTiles`, `boundariesShortDaysAndLongDays`, `markersZonesAndTickThinning`, `emittedLanesPresentationAndEmptyPlan` and `TimelineGeometryTests`.
- 1b builds `PlanPreparer` on `PlanDecoder` (adding `layouts`, `prepare`, `nowTexts`) and on `CopyCatalogProvider`.

---

## Appendix A. Apple goldens measured on 2026-11-16 [A]

Produced on this Mac with `swift` (Foundation), `DateFormatter` with `locale = en_GB / nl_NL`, `timeZone = UTC`, the iOS templates, and `NumberFormatter(.decimal, maximumFractionDigits = 3)`. These match the iOS `CopyLineFormatter` code paths and are the Android expectations.

### A.1 Dates (input → en / nl)

| Call | en | nl |
|---|---|---|
| `formatDayMonth("2026-MM-15")`, MM = 01…12 | `15 Jan` … `15 Dec` (table: `Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec`) | `15 jan` … `15 dec` (table: `jan feb mrt apr mei jun jul aug sep okt nov dec`) |
| `formatWeekday("2026-11-16")` … `("2026-11-22")` (Mon…Sun) | `Mon Tue Wed Thu Fri Sat Sun` | `ma di wo do vr za zo` |
| `formatDate("2026-09-15")` | `15 Sep 2026` | `15 sep 2026` |
| `formatWeekdayDate("2026-09-15")` | `Tue 15 Sep` | `di 15 sep` |
| `formatMonthYear("2026-05-03")` | `May 2026` | `mei 2026` |
| `formatWeekdayDate("2026-05-03", month = false)` | `Sun 3` | `zo 3` |
| `formatDayMonth("2026-05-03")` | `3 May` | `3 mei` |

### A.2 Numbers (`formatNumber(value)` → en / nl)

| value | en | nl |
|---|---|---|
| `0.0` | `0` | `0` |
| `1.0` | `1` | `1` |
| `1.5` | `1.5` | `1,5` |
| `1500.0` | `1,500` | `1.500` |
| `-1500.0` | `-1,500` | `-1.500` |
| `1234567.8915` | `1,234,567.892` | `1.234.567,892` |
| `0.0005` | `0` | `0` |
| `0.0015` | `0.002` | `0,002` |
| `2.5` | `2.5` | `2,5` |
| `1.23456` | `1.235` | `1,235` |
| `-0.5` | `-0.5` | `-0,5` |
| `12.0` | `12` | `12` |
| `999.9995` | `1,000` | `1.000` |
| `1.0005` | `1` | `1` |
| `0.1235` | `0.124` | `0,124` |
| `0.1245` | `0.124` | `0,124` |
| `-0.0` | `-0` | `-0` |
| `-0.0004` | `-0` | `-0` |
| `1.0E15` | `1,000,000,000,000,000` | `1.000.000.000.000.000` |
| `12345.0` | `12,345` | `12.345` |

The minus sign above is U+002D (bytes `2d`), the grouping and decimal separators are ASCII.

### A.3 Parsing (`planDate`)

| Input | Apple result |
|---|---|
| `2026-01-15T00:05:00Z` | 2026-01-15 00:05:00 UTC |
| `2026-01-15T00:05:00.123Z` | 2026-01-15 00:05:00.123 UTC |
| `2026-01-15T00:05:00+02:00` | 2026-01-14 22:05:00 UTC |
| `2026-01-15T00:05+02:00` | rejected (`nil`) |
| `2026-01-15T00:05:00.1Z` / `.123456789Z` | accepted (1 and 9 fraction digits) |
| `2026-01-15T00:05:00.1234567891Z` | rejected (10 digits) |
| `2026-01-15T00:05:00.Z` | rejected |
| `2026-01-15T00:05:00+0200` / `+02` | 2026-01-14 22:05:00 UTC (both accepted) |
| `2026-01-15T00:05:00-05:00` | 2026-01-15 05:05:00 UTC |
| `2026-01-15T00:05:00z` | accepted (lower-case `z`) |
| `2026-01-15t00:05:00Z`, `2026-01-15T00:05:00` (no zone), `2026-01-15T00:05:00 Z`, `20260115T000500Z`, `2026-1-15` | rejected |
| `2026-13-01` (10 chars, gets `T12:00:00Z`) | rejected |
| `2026-02-30T00:00:00Z` | 2026-03-02 00:00:00 UTC (rolls over) |
| `2026-01-15T24:00:00Z` | 2026-01-16 00:00:00 UTC |
| `2026-01-15T00:05:60Z` | 2026-01-15 00:06:00 UTC |

Measured again by the checker on 2026-11-16 with `swift` on this Mac, using the exact iOS `planDate` body.

## Appendix B. iOS → Kotlin test map

| iOS test (file:line) | Kotlin class.method | 1a covers |
|---|---|---|
| `CopyTests.intlGoldens` (CopyTests.swift:19) | `CopyTests.intlGoldens` | all |
| `CopyTests.caffeineCases` (:40) | `CopyTests.caffeineCases` | all |
| `CopyTests.argumentsPluralAndFallback` (:77) | `CopyTests.argumentsPluralAndFallback` | all |
| `LabelTests.optionalRulesAndUnknowns` (LabelTests.swift:8) | `LabelTests.optionalRulesAndUnknowns` | `isOptionalStep` matrix (1b adds layout) |
| `LabelTests.datesZonesAndIntervals` (:39) | `LabelTests.datesZonesAndIntervals` | all |
| `LabelTests.summaryClippingSplittingAndMerging` (:102) | `LabelTests.summaryClippingSplittingAndMerging` | all but `nowLabel` |
| `LabelTests.noticesStatusAndLabelCatalog` (:146) | `LabelTests.noticesStatusAndLabelCatalog` | all |
| `TimelineTests.everyFixturePreservesLayoutAndResolvesCopy` (TimelineTests.swift:199) | `TimelineTests.everyFixturePreservesLayoutAndResolvesCopy` | copy and immutability half |
| `TimelineTests.{dimensionsPackingAndTinyTiles, boundariesShortDaysAndLongDays, markersZonesAndTickThinning, emittedLanesPresentationAndEmptyPlan}`, `TimelineGeometryTests` | — | 1b |
| `ModelTests.versionAndRawPayloadInvalidateCache` (PlanUITests/ModelTests.swift:134) | `ModelTests.versionAndRawPayloadInvalidateCache` | all except `decodedOnMainThread` |
| `ModelTests.localProgressCountsStartsAndKeepsSkippedSeparate` (PlanUITests/StepCheckTests.swift:80) | `ModelTests.localProgressCountsStartsAndKeepsSkippedSeparate` | all |
| `ModelTests.localCheckUndoAndReplanAreObservedThroughTheOutbox` (StepCheckTests.swift:10) | merge rules in `CheckMergeTests` | semantics only; the DB/outbox journey is 6d |
| `ModelTests.systemClockRefreshesForTimeZoneNotification` | — | 1d (`SystemPlanClock`) |
| — (Android-only) | `PlanDateTest`, `DateTextTest`, `CalendarTablesTest`, `NumberTextTest`, `LocaleIndependenceTest`, `CopyCatalogTableTests`, `PlanDecoderTests`, `CheckMergeTests`, `PlanClockTests`, `CopyTests.reviewFixturesResolve` | new |
