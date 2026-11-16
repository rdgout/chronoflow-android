# 10 — Copy and localisation (en + nl)

Research for the native Android ChronoFlow app (Kotlin + Jetpack Compose). Topic: every user-visible string, how iOS stores and renders it, how server copy keys turn into text, date/time/number formatting parity, and a pipeline that turns the iOS String Catalogs into Android resources.

**Legend.** **[F]** = fact verified in source (path:line). **[R]** = recommendation for Android. **[OQ]** = open question with a default the plan uses until the user answers.

Pins used for this research:
- iOS `chronoflow-ios` HEAD `f391a1fa` (2026-11-12, "feat(settings): manage passkeys from security"), clean working tree.
- Web `chronoflow-new` HEAD `1f4a2684` on `feat/mobile-api-passkey-management` (2026-11-12). The iOS plan-copy generator is pinned to the older web commit `afd80dc0`.
- RN `chronoflow-mobile`, read in place.

---

## 0. Summary

1. **[F]** iOS ships **13 String Catalogs** with **1,281 keys** and **2,624 string units** in total. Every key has `en` and `nl`, no key is missing Dutch, and every unit has `state: translated`. There are **no device or width variations** (0 found); the only variation type is `plural` (`one`/`other`), used by **31 keys**. Two catalogs are empty: `App/InfoPlist.xcstrings` and `DesignSystem/Resources/Localizable.xcstrings`.
2. **[F]** Keys are **English sentences**, the same convention as the web (`lang/*.json`) and RN (i18next with `keySeparator: false`). Four placeholder dialects coexist, depending on which source a catalog was copied from:
   - `%@` / `%lld` / `%1$@`: Apple format specifiers.
   - `{{name}}`: i18next style, replaced by string substitution after the lookup.
   - `:name`: Laravel style. It appears in plan-copy keys only; the values are already converted to `%N$@`.
   - `_one` key suffixes: i18next plurals, in the wizard catalog only.
3. **[F]** Server copy (plan, review, prompts, reasons, 422 validation messages) **always arrives in English** as `CopyLine{key,args}` or as English sentence keys. The API ignores `Accept-Language` (web `.ai/rules/api.md` "API plans carry no rendered language"). The phone translates. iOS does this through a **generated runtime table**, `PlanCore/Resources/PlanCopy.json` (289 entries), produced by `Tools/PlanCopyGen/generate.py` from web `lang/{en,nl}.json` at pin `afd80dc0`. It is looked up by English key at runtime, not through Apple's localisation APIs.
4. **[R]** The Android app should **keep generating from the iOS catalogs, without forking.** iOS is the behavioural reference, its Dutch is reviewed by a person, and it is still changing (the passkey copy landed 2026-11-12). The plan:
   - A Python script (`python3 -I`) reads the catalogs from a **pinned iOS commit via `git show`** and emits `res/values/strings_ios.xml` and `res/values-nl/strings_ios.xml`.
   - A committed **name lock file** keeps resource names stable.
   - **`PlanCopy.json` is copied verbatim** into `assets/`.
   - A small **Android-only overlay catalog** holds the strings iOS does not have.
   - A prototype converter was written and run against all catalogs. It emits 928 `<string>` + 31 `<plurals>` per language, with 0 placeholder mismatches between en and nl and 0 positional gaps (Appendix A).
5. **[F] → [R] Parity trap found.** JDK/CLDR formats `en-GB` September as **"Sept"** and Dutch months with a trailing dot ("sep.", "mrt."). Apple prints **"Sep"** and "sep"/"mrt" (iOS also strips dots). See §8.3 for the commands that showed this. **[R]** Android must not rely on locale month and weekday names for plan copy. Ship fixed en/nl month and weekday abbreviation tables, or post-process, and add a September golden test. The 44 iOS goldens contain no September date, so they would not catch it.
6. **[F]** Rules to carry over:
   - Copy promises a clock shift, never symptom relief (iOS `AGENTS.md:24`, `plan.md:129`, `plan.md:1533`).
   - English and Dutch land together, and a test fails on a missing `nl` (`plan.md:126`, `AppCoreTests/CatalogTests.swift`).
   - Dutch is written by a person, never machine-translated (`plan.md:126`).
   - Dutch source order is OQ 28 (`plan.md:507`).
   - Board Dutch terms come from `WizardCopy.dc.html`; "Where a board differs from the app's existing Dutch, the app wins."

---

## 1. What can be reused from the iOS research and plan

This answers the user's question directly: *does the iOS research/plan contain things the Android app can already reuse?* Yes, a lot. All paths below are in `/Users/rickgout/Sites/chronoflow-ios`.

| Reusable asset | Path | How Android uses it |
|---|---|---|
| All UI copy, en+nl, reviewed Dutch | 11 non-empty `*.xcstrings` (§2) | Input to the generator (§9). No retranslation needed. |
| Plan/review/validation copy runtime table | `Packages/ChronoFlowKit/Sources/PlanCore/Resources/PlanCopy.json` (141,652 bytes, 289 entries) | Copy **verbatim** into `app/src/main/assets/plan_copy.json`; port `CopyCatalog` (61 lines) to Kotlin. |
| Plan-copy generator logic | `Tools/PlanCopyGen/generate.py` (141 lines) + `format-units.json`, `support-keys.json`, `no-health-data.json` | Not rerun by Android. Its output is reused instead (OQ-L1). Read it to understand `coded` cases and the no-health overlay. |
| CopyLine formatter | `Sources/PlanCore/CopyLineFormatter.swift` (206 lines), `TimelineLabels.swift` (44) | Port 1:1 to Kotlin (`dayDateLabel`, `nowLabel`, `blockInterval`, `utcOffset`, `localDate`, caffeine and drinks). |
| Golden tests | `Tests/PlanCoreTests/Fixtures/intl-cases.json` (44 cases), `CopyTests.swift` (caffeine en/nl tables, fallback) | Copy them as JVM unit-test fixtures, then add September and weekday goldens (§8.3). |
| Plan fixtures S1–S6, S6-RETURN-10N | `Tests/PlanCoreTests/Fixtures/*.json` (byte-identical to DataAccess fixtures) | A test asserts that every `CopyLine.key` in them resolves in en and nl. |
| Catalog completeness test | `Tests/AppCoreTests/CatalogTests.swift` | Same rule in the generator plus a Gradle check. |
| Language-choice model | `Sources/Settings/SettingsDevicePreferences.swift` | Same three choices (system/en/nl) and fallback rule. |
| Lockout wording keys | `Sources/AppCore/LockoutWait.swift:22-28` | Same keys, so they map to the same plurals. |
| Copy rules and Dutch source order | `AGENTS.md:23-24`, `plan.md:126-129`, `plan.md:507`, `plan.md:1528-1533` | Copy into the Android AGENTS/CLAUDE rules. |
| Board copy rules incl. **Android** section | `_design/canvas/WizardCopy.dc.html` ("Android" block and "Dutch terms") | Pickers follow app language and the **phone 12/24-hour setting**; Material date/time dialogs. |

---

## 2. Inventory of string catalogs [F]

Counted with a Python script over the JSON (`sourceLanguage` is `en` everywhere; version `1.0`).

| Catalog (under `Packages/ChronoFlowKit/Sources/` unless noted) | Table | Keys | Plural keys | Placeholder style | Notes |
|---|---|---:|---:|---|---|
| `App/Localizable.xcstrings` (app target) | Localizable | 4 | 0 | `%@` | Tab titles `Today`, `My trips`, `Settings`; `Debug route: %@` (debug only) |
| `App/InfoPlist.xcstrings` | InfoPlist | 0 | 0 | — | Empty; no usage-description strings localised |
| `DesignSystem/Resources/Localizable.xcstrings` | — | 0 | 0 | — | Empty |
| `Auth/Resources/Localizable.xcstrings` | Localizable | 100 | 0 | `%@`, `%lld` | `<terms>…</terms>`/`<privacy>…</privacy>` inline tags in 2 keys; 1 `shouldTranslate:false` |
| `Launch/Resources/Localizable.xcstrings` | Localizable | 2 | 0 | — | `ChronoFlow is opening`, `Getting your plan ready` |
| `Welcome/Resources/Localizable.xcstrings` | Localizable | 6 | 0 | — | |
| `PlanCore/Resources/PlanCopy.xcstrings` | PlanCopy | 313 | 9 | `%1$@`, `%1$lld` from `:arg` | **Generated** from web; 289 logical keys + 24 `#case.N` coded-range rows; all `extractionState: manual`, `generatesSymbol: false` |
| `PlanUI/Resources/Localizable.xcstrings` | Localizable | 104 | 2 | `%@` (31), `%lld` | `%lld notes`, `%lld warnings` |
| `PlanUI/Resources/Screens.xcstrings` | Screens | 121 | 6 | `{{name}}` + `%lld` | Key says `{{count}}`, plural value says `%lld` |
| `PlanUI/Resources/Walkthrough.xcstrings` | Walkthrough | 30 | 0 | `{{name}}` | Every entry commented with its Dutch source ("Dutch: exact RN nl.json match.") |
| `Reminders/Resources/Reminders.xcstrings` | Reminders | 31 | 0 | `{{time}}`, `{{count}}` | Notification bodies + settings page |
| `Settings/Resources/Settings.xcstrings` | Settings | 277 | 11 | `%lld`, `{{name}}` | 255 entries commented "RN exact English key: …" |
| `Settings/Resources/Terms.xcstrings` | Terms | 15 | 0 | — | |
| `TripWizardUI/Resources/Localizable.xcstrings` | Localizable | 278 | 3 (+9 `_one` pairs) | `{{name}}`, `%lld` | i18next `_one` keys |
| **Total** | | **1,281** | **31** | | |

Placeholder occurrences across all units: `%@` 70, `%lld` 90, `%1$@` 226, `%2$@` 32, `%3$@` 10, `%1$lld` 20, `%2$lld` 16, `{{name}}` 256.

Characters that need escaping on Android (unit counts): `@` 282 (inside text, e.g. emails; none at start), `?` 92 (none at start), ASCII `'` 76, `’` 11, `"` 4, `&` 3 (`Plan & credits`, `Connectivity & reminders`), `<`/`>` 4 (the Auth link tags), `…` 10, `–` 12, `·` 64, leading whitespace 2 (`' · {{time}}'` in PlanUI). There are no newlines and no literal `%` outside format specifiers.

**Duplicate keys across tables [F].** 112 keys appear in more than one catalog, and **9 have different Dutch**, on purpose, per context. So Android resource names **must be namespaced per table**:

| English key | Dutch per table |
|---|---|
| `Sleep` | `Slapen` (PlanCore, PlanUI, Screens, Walkthrough, Settings) · `Slaap` (TripWizardUI) |
| `Seek bright light` | `Zoek fel licht` (PlanCopy) · `Zoek fel licht op` (Screens, Walkthrough) |
| `Early bird` | `Vroege vogel` (Settings) · `Ochtendmens` (TripWizardUI) |
| `Usual bedtime` | `Gewone bedtijd` (Settings) · `Meestal naar bed` (TripWizardUI) |
| `Bedtime` | `Bedtijd` (Settings) · `Naar bed` (TripWizardUI) |
| `Blue-blocking glasses` | `Blauwlichtfilterbril` (Settings) · `Blauwfilterbril` (TripWizardUI) |
| `When do you naturally feel most awake?` | `…het meest wakker?` (Settings) · `…het fitst?` (TripWizardUI) |
| `Add at least one drink to save this answer.` | `…op te slaan.` (Settings) · `…te bewaren.` (TripWizardUI) |

The ninth key is `Night owl`, which also differs between Settings and TripWizardUI.

---

## 3. Key conventions [F]

- **English sentence = key.** On iOS, every key's `en` value equals the key, except in two places. One is the PlanCopy `#case.N` rows and pipe plurals (119 entries). The other is the wizard `_one` keys (10 entries), whose English value is the singular sentence. Lookup helpers fall back to the key itself when a key is missing: `bundle.localizedString(forKey:value:key,table:)` in `SettingsStrings.swift:16`, `PlanStrings.swift:11`, `ReminderSchedule.swift:43` and `WizardStrings.swift:16`.
- **Explicit language, explicit bundle.** iOS does not rely on the process locale. Each helper opens `Bundle.module/<lang>.lproj` for the resolved `Language` (`en`/`nl`). Examples: `SettingsStrings.text(_:language:_:count:)`, `PlanStrings.text`, `PageCopy(language:)`, `AuthCopy.string(_:locale:)`, `WizardStrings.text(_:locale:arguments:)`, `ReminderStrings.text(_:language:time:count:)`.
- **Two-step formatting.** The helpers first apply `String(format:locale:, Int64(count))` for `%lld`. They then do `replacingOccurrences(of: "{{name}}", with: value)` for named args (`SettingsStrings.swift:17-18`, `PlanStrings.swift:12-13`).
- **Wizard plurals (i18next style).** `WizardStrings.swift:13-16` looks up `key + "_one"` and uses it **only when `arguments["count"] == "1"`**, a string compare. Otherwise it uses the base key. Some singular/plural pairs are selected in code instead, e.g. `WizardOutboundRules.swift:98-99` picks `"Stop in {{airport}} · {{count}} complete night"` or `"…nights"`.
- **Plan copy keys** use Laravel syntax: `:name` args, `a|b` plurals and `{n} …|[n,*] …` numbered ranges. Example: `:count day until :city.|:count days until :city.`.
- **Server error keys** arrive as English sentences. `AuthCopy.field(_:locale:)` (`Auth/AuthCopy.swift:14-37`) holds a closed list of accepted server keys. It maps both `Use no more than :max characters.` and `Use no more than {{max}} characters.` onto the catalog key `Use no more than %lld characters.`, and returns `nil` for unknown keys, so the caller shows generic copy.
- **Comments** record the Dutch provenance (Settings, Walkthrough).

---

## 4. Plurals [F]

- **Catalog plurals:** 31 keys, all with exactly `one` + `other` in both en and nl. That matches CLDR for both languages (`one` = integer 1 with no visible fraction).
  - Settings (11): `%lld changes waiting to sync`, `%lld credits left`, `%lld passkeys`, `%lld to answer`, `Sign out and lose %lld changes?`, `Synced %lld days ago`, `Synced %lld hours ago`, `Synced %lld min ago`, and the three `Too many attempts. Try again in %lld seconds|minutes|hours.` keys.
  - TripWizardUI (3): the same three lockout keys.
  - PlanUI (2): `%lld notes`, `%lld warnings`.
  - Screens (6): `%lld trips added|removed|updated`, `In {{count}} days`, `Your {{count}} preparation days start on {{date}}.`, `{{count}} skipped`.
  - PlanCopy (9): `:count booked nights`, `:count counted nights`, `:count day until :city.|…`, `:count drink|:count drinks`, `:count nights counted by the plan`, and four `We shift your clock :shift earlier|later, starting :count day(s) before you leave.[ Your trip home is planned too.]` keys.
- **Dutch "one" forms sometimes drop the number.** Example (Screens): `Je voorbereidingsdag begint op %2$s.` vs `Je %1$d voorbereidingsdagen …`. Android `<plurals>` allows this; lint may warn (`ImpliedQuantity`), which is acceptable.
- **Wizard `_one` pairs (9):** `{{count}} a day`, `{{count}} answered`, `{{count}} credits`, `{{count}} days prep`, `{{count}} nights`, `{{count}} time ranges`, `{{count}} days of steps`, `You have {{count}} credits`, `Stop in {{airport}} · {{count}} complete night`.
- **Coded numbered ranges (PlanCopy, 8 keys).** These are caffeine amounts in half-units:
  - The four chip keys: `:amount coffee|:amount coffees`, `:amount tea|:amount teas`, `:amount soft drink|:amount soft drinks`, `:amount energy drink|:amount energy drinks`.
  - The four sentence keys: `half a cup of coffee|:amount cups of coffee`, `half a cup of tea|:amount cups of tea`, `half a can of soft drink|:amount cans of soft drink`, `half a can of energy drink|:amount cans of energy drink`.
  - The backend Dutch is, for example, `{1} een half kopje koffie|{2} één kopje koffie|{3} :amount kopje koffie|{4} twee kopjes koffie|[5,*] :amount kopjes koffie` (`plan.md:1380`). `PlanCopy.json` stores it as `{"kind":"coded","cases":[{"bound":1,"exact":true,"format":"…"},…]}`. `CopyCatalog.Translation.format(count:)` picks the first case where `exact ? count == bound : count >= bound`; count is in **halves**.
  - Expected outputs (`CopyTests.swift`), for halves 1, 2, 3, 4, 5 and 8:
    - en: "half a cup of coffee", "one cup of coffee", "1½ cups of coffee", "two cups of coffee", "2½ cups of coffee", "4 cups of coffee".
    - nl: "een half kopje koffie", "één kopje koffie", "1½ kopje koffie", "twee kopjes koffie", "2½ kopjes koffie", "4 kopjes koffie".

**[R]** Android plural rules:
- Catalog plurals become `<plurals>`, read with `pluralStringResource(id, count, …)`.
- For wizard `_one` pairs, **do not pass a truncated Double into `getQuantityString`**. Mirror iOS exactly: quantity = `if (countText == "1") 1 else 2`, using `other`. Some counts are decimals (`{{count}} a day` gets `NSDecimalNumber.stringValue`, `WizardPresentation.swift:118`), and `1.5.toInt() == 1` would select `one`.
- Coded ranges stay in the JSON runtime table and are not turned into Android resources.

---

## 5. Server-provided copy: shapes and rendering [F]

### 5.1 Contract

- **`CopyLine{key: String, args: [CopyArg]}`.** A standard `CopyArg` is `{name, kind, value, tz}` with `kind ∈ date | instant | minutes | offset | count | milligrams | text | place` (unknown values tolerated). See iOS generated contract `AppCore/Contracts/Generated/TripPlan.swift:1534-1543`; RN `Plan/index.ts:98-107`. Caffeine args are a separate variant with `drinks` / `usualDrinks`. Example from `S1.json`:
  ```json
  {"key": "Required bright-light sessions end around :lastCoreSeekDate; optional ones continue until :lastSeekDate.",
   "args": [{"name":"lastCoreSeekDate","kind":"date","value":"2026-10-21","tz":null},
            {"name":"lastSeekDate","kind":"date","value":"2026-10-24","tz":null}]}
  ```
- **The API is language-free.** Trip reads present with fixed `Language::EN` in Drinks style and ignore `Accept-Language`. `SetLocale` never runs on the API host, and "error messages stay English sentence keys" (web `.ai/rules/api.md`, section "API plans carry no rendered language"; iOS `plan.md:1380`). This covers 422 `errors.*`, review prompts and reasons, `summary.line` and `endCopy`.
- **The error envelope** is `ApiError{code,message,errors?,args?,retryAfterSeconds?}` (`plan.md:1389`). `errors` values are English keys, e.g. `"This plan has changed. Refresh it before marking this step."`, `"Not enough credits"`, `"Try saving again shortly."`.
- **The account's stored `language` (`en`/`nl`)** is used only for mail. Registration sends an explicit `language` (`POST auth/register {…, language, …}`, `plan.md:447`). The phone's display language is a **device-local** choice and is never PATCHed. `ApiPreferences` exposes `timeFormat` (`"12"`/`"24"`, default `"24"`, web `app/Models/UserPreferences.php:52`) and the stored `language`.

### 5.2 How the web renders it [F]

- `lang/en.json` / `lang/nl.json` (1,555 keys each at HEAD, 0 missing Dutch, 31 pipe values in nl) are the single source of truth.
- `php artisan i18n:transform` (`app/Console/Commands/I18n/TransformTranslationsCommand.php`) derives the react-i18next bundle. `:name` becomes `{{name}}`, `a|b` becomes `_one`/`_other`, and English entries whose value equals the key are omitted. Vite runs it on build start (`vite.config.ts:12-28`), so the committed web locale is regenerated and not authoritative.
- Front-end rendering goes through `resources/js/lib/plan-copy-format.ts` (`sentenceKey` turns `:x` into `{{x}}`, `:13-15`).

### 5.3 How iOS renders it [F]

- `Tools/PlanCopyGen/generate.py` runs `git show afd80dc0:lang/{en,nl}.json` and selects keys:
  - PHP string literals in plan/review/trip scopes (`SCOPES`, `generate.py:13-17`);
  - required keys from `CopyKeyMap.php`, `PresenterLine.php` and `ReviewSummaryLine.php`;
  - `support-keys.json`;
  - six duration templates from `format-units.json` (RN values, since they are absent from the backend);
  - every CopyLine in the plan and wizard fixtures;
  - the `no-health-data.json` overlay, which removes 6 retired medical prompts and adds `Answer the questions to get your plan.` / `Beantwoord de vragen om je plan te krijgen.`
- It fails on missing en/nl, unknown Dutch args and unsupported plural forms. It emits `PlanCopy.xcstrings` (Xcode visibility) **and** `PlanCopy.json` (the runtime table), with `--check` for freshness (`make plan-copy`, `generators-check`).
- `CopyCatalog.load()` decodes `PlanCopy.json` on a detached task (no static initialiser). `text(key, language, arguments, count)` resolves the entry, picks the form, maps named args to positional `%N$@`, then calls `String(format:)`. **An unknown key falls back to interpolating the English key itself** (`CopyCatalog.swift:41-60`).
- `CopyLineFormatter.format` formats each arg by kind (see §8) and passes `count` for plural selection.

### 5.4 Drift since the iOS pin [F]

Web `lang/*.json` changed between `afd80dc0` and HEAD (`8e63dbc9 feat(profile): stop collecting health answers (#108)`): 23 keys added, 40 removed, 0 changed values. 7 of the 289 PlanCopy keys no longer exist at web HEAD; these are the retired health prompts, which the iOS overlay already removes from the active set. **[R]** Android takes the iOS `PlanCopy.json` as-is, so both apps show identical text, and re-syncs when iOS bumps its pin (OQ-L1).

---

## 6. Copy rules [F]

- "**Copy** promises a clock shift, never symptom relief; modest scientific claims." (`plan.md:129`; `AGENTS.md:24`). "Copy must promise a clock shift. The backend copy already does; never add symptom wording. The screening and exclusion copy gets its own review (OQ 45)." (`plan.md:1533`).
- "**English and Dutch together.** Each visible string lands with its Dutch in the commit that introduces it, in a String Catalog with English sentences as keys. A test fails on a missing `nl` entry from Phase 0 on. Dutch written by a person, never machine-translated." (`plan.md:126`; `AGENTS.md:23`: "Never hard-code customer-facing text.")
- **Dutch source order (OQ 28, user 2026-11-06, `plan.md:507`):**
  1. If RN `en.json` has the exact same English, use RN `nl.json`.
  2. Otherwise, use backend `lang/nl.json` with the exact English key.
  3. Otherwise, write a natural Dutch translation matching RN's je/jij tone and terminology (`e-mailadres`, `wachtwoord`, `inloggen`).
  4. Similar English wording is not an exact match.
- **Board Dutch terms** (`_design/canvas/WizardCopy.dc.html`, "Dutch terms"): `luchthaven` (not vliegveld), `lichtbox` (not lichtbak), `credit / credits`, `Tijd in {city}` (not "Amsterdamse tijd"), `Lokale tijd` when the city is unknown. "Where a board differs from the app's existing Dutch, the app wins."
- **Board Android rules** (`WizardCopy.dc.html`, "Android" block):
  - "× on the right, as on iOS"
  - "Date and time pills have no compact form: a tap opens the Material date or time dialog."
  - "Preparation days: drawn − and + buttons (48 dp)"
  - "Progress: the native determinate linear progress indicator"
  - "**Pickers follow the app's language and the phone's 12/24-hour setting.**"
- **No health data (Phase 19):** retired medical prompts are removed from copy (`Docs/no-health-data.md`, `Tools/PlanCopyGen/no-health-data.json`).

---

## 7. Language selection and lookup on iOS [F]

- `SettingsDevicePreferences.LanguageChoice { system, en, nl }`, persisted in `UserDefaults` under `cf.settings.language` (default `system`) (`Settings/SettingsDevicePreferences.swift:6-15`).
- `resolvedLocale(phone:)`: `system` gives `nl` if the phone's language code is `nl`, otherwise `en`. The hour cycle is **copied from the phone locale** (`components.hourCycle = phone.hourCycle`, `:17-27`). The root view injects it with `.environment(\.locale, …)` (`App/RootView.swift:60`).
- Screens derive `Language` from that locale (`App/AppTabs.swift:49`: `locale.language.languageCode == "nl" ? .nl : .en`).
- Settings copy: "ChronoFlow follows your phone's language, and uses English if your phone is set to a language we don't offer yet. Pick a language here only if you want something different." (`SettingsPreferencesView.swift:181`). Subtitles: `Phone language ({{language}})`, `Now {{language}} · changes with your phone`.
- RN had the same model: `resolveLanguage(override, locales)` with storage key `language` (`chronoflow-mobile/src/i18n/index.ts`).

---

## 8. Dates, times, numbers and time zones

### 8.1 iOS behaviour [F]

| What | Rule | Source |
|---|---|---|
| Plan instants (`instant` args, block intervals, now label, reminders) | Manual `%02d:%02d`. In 12 h mode: `hour%12==0 ? 12 : hour%12`, **zero-padded**, then `" "` + catalog `AM`/`PM`. en gives `AM`/`PM`; nl gives `a.m.`/`p.m.`. Goldens: `03:00 AM`, `03:00 p.m.` | `CopyLineFormatter.swift:100-109`; `intl-cases.json` |
| 12/24 source for plan copy | Account preference `timeFormat` (`"12"`/`"24"`), default `.value24`; **not** the phone setting | `TripPlanModel.swift:15`, `TripPageModel.swift:98-99`, `CopyFormat` init |
| Plan dates | `DateFormatter` with fixed locale `en_GB` / `nl_NL`, zone per arg (default UTC); templates `d MMM yyyy`, `d MMM`, `EEE d MMM`, `EEE d`, `EEE`, `MMM yyyy`; **all `.` removed** | `CopyLineFormatter.swift:64-98` |
| Day label range | `first – last` with an en dash; month omitted on the first date when both are in the same month | `TimelineLabels.swift:5-13` |
| Block interval | Each endpoint gets a date prefix if the local dates differ and a `(GMT+hh:mm)` suffix if the offsets differ; separator `" – "` | `TimelineLabels.swift:22-33` |
| UTC offset | `GMT` for 0, else `GMT±hh:mm` | `TimelineLabels.swift:39-43` |
| Durations / offsets / mg | Catalog templates (`:amount min`, `:hours hr :minutes min`, `:amount h`, `:hours h :minutes min`, `:amount mg`), sign `+` / `−` (U+2212) | `CopyLineFormatter.swift:111-142` |
| Numbers | `NumberFormatter` `en_GB` / `nl_NL`, decimal, max 3 fraction digits: `1,500 min` / `1.500 min` | `CopyLineFormatter.swift:57-63` |
| Zone label | City of the matching airport in the trip, else the last IANA path segment with `_` → space | `CopyLineFormatter.swift:198-205` |
| Trip header dates | Fixed `en_GB`/`nl_NL`, UTC, `d MMM [yyyy]`, `%@–%@` or `%@ to %@` | `PlanUI/TripPage/TripHeaderText.swift` |
| Wizard flight date/time | App locale **with the phone hour cycle**, airport-fixed calendar, `EEE d MMM` + ` · ` + template `jmm` | `TripWizard/Model/WizardDateTime.swift:75-79` |
| Settings dates (passkeys, sync, plan) | `.dateTime.day().month(.abbreviated)[.year()].locale(locale)`, a skeleton on the resolved language-only locale | `SettingsPasskeyText.swift:19-28`, `SettingsHomeRows.swift:65` |
| Clock refresh | `PlanClock` ticks on the minute boundary, on foreground, on `significantTimeChange` and on time-zone change | `plan.md:1526`; `PlanCore/PlanClock.swift` |
| DST goldens | `Europe/Amsterdam` 2026-03-29 / 2026-10-25, `America/New_York` 2026-11-01, Tokyo | `intl-cases.json`; `plan.md:1641` |

### 8.2 Recommendations for Android [R]

- **Port the formatter by hand rather than using `DateTimeFormatter.ofLocalizedTime`.** Instants: `java.time.Instant` → `ZonedDateTime` in `ZoneId.of(tz)` → hour/minute → the same zero-padded `%02d:%02d` + `AM`/`PM` from `plan_copy.json`. This is also the fastest path for the timeline: no `Formatter`, no `DateTimeFormatter` allocation per label.
- **Plan 12/24 comes from the account `timeFormat`**, default `"24"`, as on iOS. **Wizard pickers and wizard date-time labels use `android.text.format.DateFormat.is24HourFormat(context)`** together with the app language. This follows the board rule and iOS `resolvedLocale` (phone hour cycle). In Compose M3: `TimePicker(state = rememberTimePickerState(is24Hour = DateFormat.is24HourFormat(ctx)))`. Labels: `DateFormat.getBestDateTimePattern(appLocale, if (is24) "Hm" else "hm")`.
- **Settings-style dates:** `DateFormat.getBestDateTimePattern(Locale(lang), "dMMM[y]")` on the language-only locale, matching iOS's skeleton on `en`/`nl` (iOS gives US-style order for plain `en`). Verify on device and record it as a golden (OQ-L6).
- **Zones:** `java.time` with IANA ids. Android updates tzdata through the Time Zone Data mainline module. Port the DST goldens unchanged.
- **Clock:** a minute-aligned coroutine ticker in the plan ViewModel, plus a dynamic `BroadcastReceiver` for `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` and `ACTION_DATE_CHANGED` while started, plus a refresh `onStart`. Inject a `Clock` for tests.
- **Numbers:** `java.text.NumberFormat.getNumberInstance(Locale("nl","NL"))` gives `1.234,5` (checked locally, see §8.3). Cache one instance per language per thread (not thread-safe) or use `android.icu.text.NumberFormat`. Add an integer fast path for counts.

### 8.3 Parity trap: month and weekday names differ between Apple and CLDR/Java [F]

Both outputs below were produced on this Mac on 2026-11-15:

- **Apple Foundation** (`swift`):
  - `en_GB d MMM yyyy` gives "15 Sep 2026" and "29 Mar 2026"; `EEE d MMM` gives "Tue 15 Sep".
  - `nl_NL` gives "15 sep 2026", "29 mrt 2026" and "di 15 sep" (no dots in the format context).
- **OpenJDK 17.0.21 (CLDR):**
  - `en-GB d MMM yyyy` gives "**15 Sept 2026**"; `EEE d MMM` gives "Tue 15 **Sept**".
  - `nl-NL` gives "15 **sep.** 2026", "29 **mrt.** 2026" and "di 15 sep.".
  - `h:mm a` gives "2:05 pm" / "2:05 p.m.".

Android's ICU/CLDR data (API level dependent) behaves like CLDR. The dots are harmless because iOS strips `.` and Android must too. **"Sept" is a real divergence**, and the iOS goldens do not cover September.

**[R]** Use fixed tables for plan copy:
- en months `Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec`
- nl months `jan feb mrt apr mei jun jul aug sep okt nov dec`
- en weekdays `Mon Tue Wed Thu Fri Sat Sun`
- nl weekdays `ma di wo do vr za zo`

Build `d MMM yyyy`-style strings by hand. This is deterministic across API 26–37 and faster. Add goldens for 2026-09-15 and for every weekday in both languages. Generate the tables once from Apple output; do not type them by hand without a test.

---

## 9. Android pipeline design [R]

### 9.1 Source-of-truth decision

**Recommendation: keep generating from the iOS catalogs (no fork), pinned and checked.** iOS stays the single place where copy and Dutch are authored. Android adds only an overlay for strings that exist on Android alone.

Why:
1. The goal is "behaves exactly like the iOS app". iOS copy is still moving: the Settings and passkey copy landed 2026-11-12, after most of this research input. A one-time fork would drift within weeks.
2. The Dutch is person-reviewed with recorded provenance (OQ 28). Forking doubles the review burden and risks two different Dutch texts for one product.
3. The iOS conventions are machine-convertible. The prototype converted all 959 logical strings with no manual fixes (Appendix A).
4. It is the same pattern iOS uses against the web (`PlanCopyGen` reads a **pinned** commit via `git show`, with a `--check` freshness gate). Reading via `git show <pin>` also respects the read-only rule and never picks up uncommitted iOS work.

Rejected alternatives:
- **Generate once, then Android owns its copy.** Simple, but it drifts and creates a second Dutch review stream.
- **Generate both apps from web/RN JSON.** iOS has already deviated (no-health overlay, iOS-authored Dutch), and OQ 13 deferred this.

### 9.2 Layout

```
tools/l10n/xcstrings2android.py      # generator (python3 -I), stdlib only
tools/l10n/ios-pin.txt               # e.g. f391a1fa  (bump = regenerate + review diff)
tools/l10n/names.lock.json           # "<table>\t<English key>" -> resource name (committed, append-only)
tools/l10n/android-only.json         # overlay: {key: {en, nl, table, comment}} for Android-only copy
app/src/main/res/values/strings_ios.xml      # GENERATED (en, default locale)
app/src/main/res/values-nl/strings_ios.xml   # GENERATED (nl)
app/src/main/res/values/strings_android.xml  # GENERATED from overlay (or hand-written, en)
app/src/main/res/values-nl/strings_android.xml
app/src/main/res/values/strings_untranslatable.xml  # app_name "ChronoFlow" translatable="false"
app/src/main/assets/plan_copy.json   # verbatim copy of iOS PlanCore/Resources/PlanCopy.json @ pin (+ sha256 check)
app/src/main/res/resources.properties        # unqualifiedResLocale=en  (for generateLocaleConfig)
core/l10n/.../CopyIndex.kt           # GENERATED: English key -> R id, only for keys looked up dynamically
```

Commands: `python3 -I tools/l10n/xcstrings2android.py --ios ../chronoflow-ios --pin $(cat tools/l10n/ios-pin.txt) …` writes the files. `--check` exits non-zero if any generated file differs; it is wired into `./gradlew check` via an `Exec` task, or run first in `make check`.

### 9.3 Resource names from English keys

1. Prefix by table: `app`, `auth`, `launch`, `welcome`, `plan` (PlanUI/Localizable), `screens`, `walkthrough`, `reminders`, `settings`, `terms`, `wizard`. This prefix is needed because of the 9 same-key/different-Dutch pairs (§2).
2. Build the slug:
   - NFKD fold to ASCII, then lowercase.
   - `%lld` / `%N$lld` → `n`; `%@` → `x`.
   - `{{camelName}}` → `camel_name`.
   - Each run of non-`[a-z0-9]` → `_`; strip `_`.
3. Cap at **48 characters**. Longer names are truncated to 41 characters + `_` + the first 6 hex characters of `sha1(key)`. In the prototype, 254 of 959 names were truncated this way, e.g. `wizard_adding_a_return_flight_costs_1_cr_474862`.
4. **Collisions.** These pairs map to the same slug: `auth`: `Check your inbox.` / `Check your inbox`; `reminders`: `Turn on reminders` / `Turn on reminders?`; `wizard`: `One way` / `one way` and `Return` / `return`. The second key to arrive gets the hash suffix of `"<table>\t<key>"`.
5. **Stability via `names.lock.json`.** A name, once assigned, never changes, even if a later key would collide. The generator reads the lock first and only appends. When a key is deleted on iOS, its resource disappears and the lock entry is pruned. Compose call sites then fail to compile, which is intended because it surfaces removed copy.
6. Every generated entry carries an XML comment `<!-- table: original English key -->`, so a search for the iOS English text finds the resource.

### 9.4 Placeholders → Java format args

- Every argument becomes **positional** (`%1$s`, `%2$d`). aapt rejects multiple unpositioned args, and Dutch may reorder.
- Apple `%@` → `%N$s`; `%lld`/`%ld`/`%d` → `%N$d`; `%N$@` → `%N$s`; `%N$lld` → `%N$d`. Unpositioned `%` args are numbered in order of appearance, matching `String(format:)`.
- `{{name}}` → `%K$s`. K comes after the highest `%` position, numbered by first appearance in the key, then in the English value.
- **Special case [F]:** in Screens and Settings, keys say `{{count}}` while plural values use `%lld`. Example: `Your {{count}} preparation days start on {{date}}.` has the value `Your %lld preparation days start on {{date}}.`, which becomes `%1$d … %2$s`. The generator maps `count` onto `%1$d` in that case.
- `{{count}}` in wizard and reminder strings stays `%N$s`. The caller passes the already-formatted number, as iOS does with `count.formatted(.number.locale(…))`.
- A literal `%` becomes `%%`. A string with `%` but no args gets `formatted="false"` (none today).
- The generator fails if the set of positions differs between en and nl. Prototype result: 0 mismatches, 0 positional gaps.
- **Kotlin call sites** keep the iOS argument names readable through a tiny generated helper per resource with named parameters. Alternatively, keep the order documented in the comment. Default: comment only (OQ-L4).

### 9.5 Escaping for `strings.xml`

Applied in this order:
1. XML-escape `&`, `<`, `>`.
2. `\` → `\\`.
3. Newline → `\n`.
4. `'` → `\'`.
5. `"` → `\"`.
6. A leading `@` or `?` gets a `\` prefix.
7. Leading or trailing spaces become ` `. Example: `' · {{time}}'` → ` · %1$s`.

Typographic `’ “ … – · ½ −` stay literal (UTF-8 file). The `<terms>…</terms>` and `<privacy>…</privacy>` tags in the 2 Auth sign-up strings are emitted **escaped**, so `getString` returns them as literal text. A small Kotlin parser then builds an `AnnotatedString` with `LinkAnnotation.Url` to the web `/terms` and `/privacy` URLs, matching iOS. This is preferable to Android `<annotation>` spans, which `stringResource` drops.

### 9.6 Plurals

- Catalog `variations.plural.{one,other}` → `<plurals><item quantity="one|other">`.
- Wizard `X` + `X_one` → one `<plurals name=…X…>` with one/other. The `_one` key gets no separate resource. The call-site rule from §4 applies (string compare to "1").
- Caller-selected pairs (`…complete night` / `…complete nights`) stay as two resources, called exactly as iOS calls them.
- `zero`/`two`/`few`/`many` never occur. The generator fails on any other quantity, and on any `device`/`width`/`substitutions` variation (none exist today), so a future iOS change cannot slip through silently.

### 9.7 Plan copy (server keys): JSON table, not resources

- Server keys are arbitrary English sentences looked up at runtime. Android resources cannot be looked up by English text (`getIdentifier` is slow and discouraged). **[R]** Ship iOS `PlanCopy.json` verbatim as `assets/plan_copy.json`; the generator copies it and records its sha256. Port `CopyCatalog` to Kotlin:
  - Load once on `Dispatchers.IO` at app start or first plan open: about 142 KB, 289 entries, a `HashMap<String, Entry>`.
  - Use kotlinx.serialization.
  - Replace `%N$@` with a hand-written substituter (`%%` → `%`), not `String.format`. This avoids Formatter cost on the timeline's hot path.
  - Unknown key → interpolate `:name` in the English key, like iOS.
- Language is passed **explicitly** (`Language.EN/NL`), never from the process locale, as on iOS. This also makes it usable from WorkManager (reminders) and host JVM tests.
- **Validation and error keys** that map onto UI catalogs (Auth `AuthCopy.field`, wizard and settings validation) use a generated `CopyIndex.kt`: `mapOf("<English key>" to R.string.x)` limited to the keys the iOS code accepts dynamically. Unknown keys get the same generic fallback as iOS.

### 9.8 Android-only copy (overlay) [F→R]

The iOS catalogs contain iOS-specific wording that Android needs to replace (Reminders catalog):
- `Open your phone's Settings.`
- `Tap Notifications, then ChronoFlow.`
- `Turn on Allow notifications.`
- `Come back here. Reminders switch on by themselves.`

These are iOS Settings steps. On Android, `Settings.ACTION_APP_NOTIFICATION_SETTINGS` opens the right page directly. Android also needs strings with no iOS counterpart:
- the notification **channel name and description** (default: reuse `Reminders` / `Herinneringen`);
- `POST_NOTIFICATIONS` denial and rationale wording;
- any exact-alarm wording if reminders use `AlarmManager` exact alarms;
- the Credential Manager/passkey provider wording, if it differs from iOS;
- the `app_name` label.

These go in `android-only.json` with both en and nl, following the OQ 28 source order. The generator applies the same nl-completeness check to the overlay. The iOS keys they replace are listed in an `android-only.json → replaces` array, so the Android UI provably never shows iOS steps (OQ-L3).

### 9.9 Runtime language on Android

- **Per-app language:** `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("nl"|"en"))`. An empty list means `system`. Set `autoStoreLocales = true` (service metadata) for API < 33. Use AGP `androidResources { generateLocaleConfig = true }` with `resources.properties` `unqualifiedResLocale=en`, so the Android 13+ system per-app language setting lists exactly en and nl and stays in sync with the in-app picker.
- **Resolving `Language`:** `system` gives `nl` if the first system locale's language is `nl` (so `nl-BE` counts as nl), otherwise `en`. This is identical to iOS and RN. Resource fallback agrees automatically: `values/` is English, so a `de-DE` phone gets English.
- **Mirroring iOS's three-way choice.** AppCompat stores only the locale list, so it cannot tell "system" from an explicit choice once that matches the phone. Keep the iOS model as the source: store `LanguageChoice` in DataStore under `cf.settings.language`, values `system|en|nl`, and apply it with `setApplicationLocales` on change and at startup (OQ-L5).
- **Background work** (reminder notifications built in WorkManager or a receiver): resolve `Language` from DataStore and use `context.createConfigurationContext(config.apply { setLocale(locale) })` for resources, plus the explicit-language `CopyCatalog`. Do not rely on the process default locale.
- **Wizard Android board:** Material date and time dialogs follow the app language (AppCompat locale) and the phone's 12/24 setting.

### 9.10 Checks and tests

- **Generator (hard fail):**
  - missing `nl` or empty value;
  - en/nl placeholder-position mismatch;
  - unsupported variation or quantity;
  - duplicate resource name;
  - `plan_copy.json` sha256 ≠ the pinned iOS file;
  - overlay without nl.
- **Gradle:** Android Lint with `MissingTranslation`, `StringFormatInvalid`, `StringFormatCount` and `PluralsCandidate` as errors. Do not blanket-ignore them in generated files; the prototype's `tools:ignore` should list only `UnusedResources` and `Typos`.
- **JVM unit tests:**
  - port `CopyTests.swift`: 44 `intl-cases.json` goldens, caffeine en/nl tables, argument, plural and fallback cases;
  - every CopyLine key in S1–S6 and the wizard fixtures resolves in en and nl;
  - new September and weekday goldens (§8.3);
  - DST instants.
- **Screenshot tests** (Roborazzi/Paparazzi) in en and nl at default and largest font scale. Dutch is often longer (e.g. `Plan de verschuiving van je biologische klok voor je volgende reis`).
- **Lint rule or detekt:** no string literals in Compose `Text(...)`, matching iOS "Never hard-code customer-facing text".

### 9.11 Performance notes for the timeline

- Format all labels when the layout is built (off the main thread, in the same pass that builds the `DayLayout`), not during composition. iOS does the same: `TripPlanModel.rebuildLayouts()` runs when `timeFormat` or language changes (`TripPlanModel.swift:15`).
- Formatting cost is minimal: one `HashMap` lookup + manual substitution + `ZonedDateTime` hour/minute + fixed month tables. No `DateTimeFormatter.ofPattern` per call. If one is needed, cache it per (language, pattern).
- The "now" label is recomputed once per minute tick only.

---

## 10. Open questions (defaults the plan proceeds on)

| # | Question | Default |
|---|---|---|
| OQ-L1 | Android plan copy: reuse iOS `PlanCopy.json` verbatim, or rerun `PlanCopyGen` against a newer web commit? | **Reuse iOS verbatim at the iOS pin**, so text matches iOS exactly. Bump when iOS bumps. |
| OQ-L2 | Keep generating from iOS, or fork once? | **Keep generating** from a pinned iOS commit with a `--check` gate (§9.1). |
| OQ-L3 | Android-only copy (notification settings steps, channel name, permission rationale): who writes the Dutch? | Follow the OQ 28 order: RN `nl.json` exact match, then backend `lang/nl.json`, then a person-quality translation in je/jij tone. Mark the rows "translated – needs review" in `android-only.json`. Channel name reuses `Reminders`/`Herinneringen`. |
| OQ-L4 | Generate typed Kotlin wrappers with named args, or rely on comments? | **Comments only** in v1. Revisit if arg-order bugs appear. |
| OQ-L5 | In-app language picker vs Android 13 system per-app language: which wins? | Both write through `AppCompatDelegate`. The DataStore `LanguageChoice` keeps the `system` distinction. On start, if the AppCompat locales differ from DataStore (the user changed the system setting), **adopt the system setting** and update DataStore. |
| OQ-L6 | Settings-style dates on a language-only `en` locale give US order ("Sep 15, 2026") on iOS: intended? | **Match iOS output** (verify on the iOS simulator and record a golden). Do not "fix" it on Android alone. |
| OQ-L7 | Month abbreviation "Sep" vs CLDR "Sept" (en-GB) | **Fixed tables matching Apple** ("Sep"), with goldens. |
| OQ-L8 | iOS `Debug route: %@` (App catalog): ship on Android? | Generate it, but reference it only from debug builds. R8 removes it from release if unused. |
| OQ-L9 | `minSdk`, which affects `java.time` and per-app locales | Assume **minSdk 26**: `java.time` is native, and per-app locales work via AppCompat back-compat. Owned by the architecture research doc. |

---

## 11. Risks

- **Silent copy drift** if the iOS pin is not bumped. Mitigation: `ios-pin.txt` and a release checklist item ("bump pin, regenerate, review diff").
- **Resource churn when iOS rewords a key.** A changed English sentence is a new key, so it gets a new resource name and the Kotlin references break. This is intended (it surfaces copy changes) but costs a small edit per rewording.
- **CLDR divergences** beyond "Sept": weekday abbreviations, am/pm and grouping may change between Android API levels. Mitigation: fixed tables for plan copy, and goldens run on the oldest and newest emulator images (API 26 needs an image; only android-36 is installed locally).
- **Wizard plural semantics.** Using `getQuantityString` with a truncated decimal count would pick the wrong form. Mitigation: the string-compare rule in §4.
- **Dutch "one" forms that omit the number** may trigger lint `ImpliedQuantity` warnings. Suppress them per resource, not globally.
- **Rich-text tags in Auth strings** need the custom parser. If the tags are forgotten, users would see literal `<terms>`.
- **Server error keys outside `CopyIndex`** fall back to generic copy, which must match iOS's generic copy, not a raw English server sentence.

---

## Appendix A — Prototype converter (tested 2026-11-15 against iOS `f391a1fa`)

Run:
```
python3 -I xcstrings2android.py --ios /Users/rickgout/Sites/chronoflow-ios --pin f391a1fa --out out/res --lock out/names.lock.json
```

Results:
- `values/strings_ios.xml` and `values-nl/strings_ios.xml`, each with 928 `<string>` + 31 `<plurals>` (959 names, all unique). Both parse as XML.
- 0 en/nl argument mismatches, 0 positional gaps.
- `PlanCopy.xcstrings` is excluded on purpose (§9.7).

Sample output:
```xml
<!-- screens: Your {{count}} preparation days start on {{date}}. -->
<plurals name="screens_your_count_preparation_days_start_df4f1b">
    <item quantity="one">Je voorbereidingsdag begint op %2$s.</item>
    <item quantity="other">Je %1$d voorbereidingsdagen beginnen op %2$s.</item>
</plurals>
<!-- settings: %lld credits left -->
<plurals name="settings_n_credits_left">
    <item quantity="one">Nog %1$d tegoed over</item>
    <item quantity="other">Nog %1$d tegoeden over</item>
</plurals>
<string name="screens_time"> · %1$s</string>
```

Source (stdlib only; production version adds `--check`, the overlay, the `plan_copy.json` copy + sha256, `CopyIndex.kt`, and hard checks on unsupported quantities):

```python
#!/usr/bin/env python3
"""Prototype: iOS String Catalogs -> Android res/values{,-nl}/strings.xml (+ names lock, + key index)."""
import argparse, hashlib, json, pathlib, re, subprocess, sys, unicodedata
from xml.sax.saxutils import escape as xml_escape

TABLES = {  # catalog path in the iOS repo -> resource prefix
    'App/Localizable.xcstrings': 'app',
    'Packages/ChronoFlowKit/Sources/Auth/Resources/Localizable.xcstrings': 'auth',
    'Packages/ChronoFlowKit/Sources/Launch/Resources/Localizable.xcstrings': 'launch',
    'Packages/ChronoFlowKit/Sources/Welcome/Resources/Localizable.xcstrings': 'welcome',
    'Packages/ChronoFlowKit/Sources/PlanUI/Resources/Localizable.xcstrings': 'plan',
    'Packages/ChronoFlowKit/Sources/PlanUI/Resources/Screens.xcstrings': 'screens',
    'Packages/ChronoFlowKit/Sources/PlanUI/Resources/Walkthrough.xcstrings': 'walkthrough',
    'Packages/ChronoFlowKit/Sources/Reminders/Resources/Reminders.xcstrings': 'reminders',
    'Packages/ChronoFlowKit/Sources/Settings/Resources/Settings.xcstrings': 'settings',
    'Packages/ChronoFlowKit/Sources/Settings/Resources/Terms.xcstrings': 'terms',
    'Packages/ChronoFlowKit/Sources/TripWizardUI/Resources/Localizable.xcstrings': 'wizard',
}
PH = re.compile(r'\{\{(\w+)\}\}|%(\d+\$)?(lld|ld|d|@)')
MAXLEN = 48

def slug(prefix, key):
    s = unicodedata.normalize('NFKD', key).encode('ascii', 'ignore').decode().lower()
    s = re.sub(r'%(\d+\$)?l{0,2}d', 'n', s).replace('%@', 'x')
    s = re.sub(r'\{\{(\w+)\}\}', lambda m: re.sub(r'(?<!^)(?=[A-Z])', '_', m[1]).lower(), s)
    s = re.sub(r'[^a-z0-9]+', '_', s).strip('_') or 'x'
    name = f'{prefix}_{s}'
    if len(name) > MAXLEN:
        name = name[:MAXLEN - 7].rstrip('_') + '_' + hashlib.sha1(key.encode()).hexdigest()[:6]
    return name

def android_escape(v):
    v = xml_escape(v)                      # & < >
    v = v.replace('\\', '\\\\').replace('\n', '\\n').replace("'", "\\'").replace('"', '\\"')
    if v[:1] in ('@', '?'): v = '\\' + v
    if v != v.strip(): v = v.replace(' ', '\\u0020', 1) if v.startswith(' ') else v  # keep edge whitespace
    return v

def arg_slots(key, en_values):
    """iOS %-args keep their String(format:) positions; {{name}} args follow, by first use in the key, then English."""
    pct = 0
    for v in en_values:
        auto = 0
        for m in PH.finditer(v):
            if m[3]: auto += 0 if m[2] else 1; pct = max(pct, int(m[2][:-1]) if m[2] else auto)
    names = []
    for v in [key] + en_values:
        for m in PH.finditer(v):
            if m[1] and m[1] not in names: names.append(m[1])
    # A key that says {{count}} while its values say %lld (Screens/Settings convention) is the same argument.
    if pct and 'count' in names: names.remove('count'); slots = {'count': 1}
    else: slots = {}
    slots.update({n: pct + i + 1 for i, n in enumerate(names)})
    return pct, slots

def convert(value, slots):
    """Named and unpositioned placeholders -> positional Java format args; a literal % -> %%."""
    pct, named = slots
    out, auto, i = [], 0, 0
    for m in re.finditer(r'%%|\{\{(\w+)\}\}|%(\d+)\$(lld|ld|d|@)|%(lld|ld|d|@)|%', value):
        out.append(value[i:m.start()]); i = m.end(); tok = m[0]
        if tok in ('%%', '%'): out.append('%%'); continue
        if m[1]: idx, conv = named[m[1]], 's'
        elif m[2]: idx, conv = int(m[2]), 's' if m[3] == '@' else 'd'
        else: auto += 1; idx, conv = auto, 's' if m[4] == '@' else 'd'
        out.append(f'%{idx}${conv}')
    out.append(value[i:])
    return ''.join(out)

def units(loc):
    if 'stringUnit' in loc: return ('single', loc['stringUnit']['value'])
    plural = loc.get('variations', {}).get('plural')
    if plural: return ('plural', {q: plural[q]['stringUnit']['value'] for q in plural})
    other = set(loc.get('variations', {})) - {'plural'}
    if other: raise ValueError(f'unsupported variation {other}')
    raise ValueError('no value')

def load(ios, pin, path):
    return json.loads(subprocess.check_output(['git', '-C', str(ios), 'show', f'{pin}:{path}']))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--ios', required=True); ap.add_argument('--pin', required=True)
    ap.add_argument('--out', required=True); ap.add_argument('--lock', required=True)
    a = ap.parse_args()
    lock_path = pathlib.Path(a.lock)
    lock = json.loads(lock_path.read_text()) if lock_path.exists() else {}
    used = set(lock.values()); res = {'en': [], 'nl': []}; index = {}; errors = []
    for path, prefix in TABLES.items():
        strings = load(a.ios, a.pin, path)['strings']
        for key in sorted(strings):
            if key.endswith('_one') and key[:-4] in strings: continue   # folded into the base key below
            entry = strings[key]; loc = entry.get('localizations', {})
            if 'nl' not in loc: errors.append(f'{path}: missing nl: {key!r}'); continue
            lid = f'{prefix}\t{key}'
            name = lock.get(lid)
            if not name:
                name = slug(prefix, key)
                if name in used: name = name[:MAXLEN - 7].rstrip('_') + '_' + hashlib.sha1(lid.encode()).hexdigest()[:6]
                lock[lid] = name; used.add(name)
            index.setdefault(prefix, {})[key] = name
            forms = {l: units(loc[l]) for l in ('en', 'nl')}
            one = strings.get(key + '_one')                              # i18next-style wizard plural
            if one:
                for l in ('en', 'nl'):
                    forms[l] = ('plural', {'one': units(one['localizations'][l])[1], 'other': forms[l][1]})
            en_vals = [forms['en'][1]] if forms['en'][0] == 'single' else list(forms['en'][1].values())
            order = arg_slots(key, en_vals)
            for l in ('en', 'nl'):
                kind, v = forms[l]
                note = f'    <!-- {prefix}: {xml_escape(key)[:120].replace("--", "- -")} -->\n'
                if kind == 'single':
                    fmt = '' if PH.search(v) or '%' not in v else ' formatted="false"'
                    res[l].append(f'{note}    <string name="{name}"{fmt}>{android_escape(convert(v, order))}</string>')
                else:
                    items = ''.join(f'        <item quantity="{q}">{android_escape(convert(t, order))}</item>\n' for q, t in v.items())
                    res[l].append(f'{note}    <plurals name="{name}">\n{items}    </plurals>')
    if errors: sys.exit('\n'.join(errors))
    for l, folder in (('en', 'values'), ('nl', 'values-nl')):
        d = pathlib.Path(a.out) / folder; d.mkdir(parents=True, exist_ok=True)
        (d / 'strings_ios.xml').write_text(
            f'<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERATED from chronoflow-ios@{a.pin}. Do not edit. -->\n'
            '<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources,Typos">\n'
            + '\n'.join(res[l]) + '\n</resources>\n')
    lock_path.write_text(json.dumps(dict(sorted(lock.items())), ensure_ascii=False, indent=1) + '\n')
    (pathlib.Path(a.out) / 'copy-index.json').write_text(json.dumps(index, ensure_ascii=False, indent=1) + '\n')

if __name__ == '__main__':
    main()
```

Known prototype limits, to fix in the production version:
- Trailing whitespace is not escaped; it does not occur today.
- `tools:ignore` is already narrowed in this listing.
- The plural quantity whitelist is not enforced yet.
- `CopyIndex.kt` is not emitted yet; the prototype writes only `copy-index.json`.
