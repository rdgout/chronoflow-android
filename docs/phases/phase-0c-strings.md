# Phase 0c brief: String pipeline (en + nl)

- **Branch:** `feat/phase-0c-strings`, from the stack top after 0a. **Wave:** 2, parallel with 0b, 0d and 0e, each in its own worktree (CLAUDE.md).
- **Depends on:** 0a (Gradle skeleton, `:core:ui`, `:core:plan`, `:konsist`, `lint.xml`, Makefile, `tools/ios-pin.txt`).
- **Plan entry:** `plan.md` "### Phase 0c: String pipeline (en + nl)" (l.825–869), Decision A17 (l.104), "Android-only copy awaiting approval" (l.481).
- **Boards:** none. This phase has no UI and draws no screen.
- **Tests:** follow AGENTS.md "Tests" for every test you write: cheapest layer, budgets (JVM < 1 s, Robolectric < 5 s), exact queries, and no `Thread.sleep`.

Tags: **[F]** is a fact verified at the iOS pin `f391a1fa` while writing this brief (2026-11-16; the prototype was re-run against the pin and every count, name and hash below was reproduced). **[R]** is a rule for Android that this brief adopts.

**Line numbers.** `plan.md` line numbers below are as of 2026-11-16. Parallel phases (0b, 0d, 0e) add rows to `plan.md` and shift them; always find a task box by its bold label, never by its line number.

---

## 1. Goal

One command, `make strings`, turns the iOS String Catalogs at the pinned commit into Android string resources in English and Dutch. A second command, `make plan-copy`, copies the iOS runtime plan-copy table byte for byte. Both have a `--check` mode inside `make generators-check`.

When the phase is done:
- every iOS UI string exists as a namespaced `<string>` or `<plurals>` in `core/ui`, in `values/` (en) and `values-nl/` (nl);
- Android-only copy comes from one overlay file and replaces named iOS keys;
- `CopyIndex` maps any iOS English key to its resource id at runtime;
- the app declares exactly en and nl through a generated locale config;
- Lint, a parity test, a resource-location test and a Konsist rule make an English string without its Dutch twin, or a literal in `Text(`, fail the build.

---

## 2. Inputs to read

### 2.1 In this repo
| Path | What to take from it |
|---|---|
| `plan.md` l.104 (A17), l.481–489, l.825–869 | The decision, the Android-only copy table and the phase tasks. Tick the six task boxes at l.847–864 in the commits below. |
| `AGENTS.md` "Working rules" (English and Dutch together), "Tests", "Commands", "Git" | Commit rules (no trailers) and test rules |
| `docs/research/10-copy-l10n.md` §2–§4, §9 (l.59–137, 262–400) | Inventory, naming, placeholders, escaping and plurals (quoted below where it matters) |
| `docs/research/10-copy-l10n.md` Appendix A (l.430–595) | **The prototype converter.** Start from this code. §6 below lists the bugs it has. |
| `docs/research/01-ios-plan-reuse.md` §6.8 (l.393–402) | "Keep the rule that generation fails on missing Dutch." |
| The Makefile, `lint.xml`, `build-logic/convention`, `konsist/` and `app/src/main/AndroidManifest.xml` from 0a | Where you wire targets, Lint issues and the Konsist rule |
| `docs/phases/phase-0a-scaffold.md` Task 6b (Konsist layout: `KonsistRules.kt`, `KonsistRulesTest.kt`, `violations/<rule>/`, `allowed/<rule>/`, `ModuleGraphTest` root lookup) and the Makefile table (`GENERATORS`, `GENERATOR_CHECKS`, `Scripts/test_scripts.sh`) | The structures you extend. Do not restructure them. |
| `docs/phases/phase-0b-tokens.md` "Makefile" bullets (`GENERATORS += tokens`, `GENERATOR_CHECKS += tokens-check`) | The wiring pattern to copy, one `+=` line each |

### 2.2 iOS (read-only, always at the pin)
Read iOS files only with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. Never build, write, fetch or check out in that repo.

| iOS path (at `f391a1fa`) | Why |
|---|---|
| `plan.md:296` "### Copy (String Catalog; English is the key)" | The convention: the English sentence is the key. |
| `plan.md:505` "### Auth copy (String Catalog; English sentence keys)" | OQ 28, the Dutch source order. It applies to the overlay Dutch. |
| `plan.md:1625` "**14a: Plan copy catalog and formatting**" | How `PlanCopy.json` is produced. Android copies it and never reruns the iOS generator. |
| `plan.md:2392` OQ 13, `plan.md:2407` OQ 28, `plan.md:2413` OQ 34 | Who owns copy, and that plan copy comes from web `afd80dc0` |
| `App/Localizable.xcstrings`, `App/InfoPlist.xcstrings`, `Packages/ChronoFlowKit/Sources/*/Resources/*.xcstrings` | The generator's input |
| `Packages/ChronoFlowKit/Sources/PlanCore/Resources/PlanCopy.json` | Copied verbatim |
| `Tools/PlanCopyGen/generate.py` | Read only, for context. Do not port or run it. |
| `Packages/ChronoFlowKit/Tests/AppCoreTests/CatalogTests.swift` (`everyKeyHasDutchText`) | Ported as a method of `StringParityTest` (T6) |
| `Packages/ChronoFlowKit/Tests/AuthTests/CatalogTests.swift` (`everyRowHasExactApprovedDutchAndNoFixtureOrPendingText`) | Its "no `PENDING` text" rule becomes a generator check (T1) |
| `Packages/ChronoFlowKit/Sources/Auth/AuthCopy.swift:14-37` | An example of runtime key lookup: the reason `CopyIndex` exists |

### 2.3 Quoted rules (from iOS `plan.md:128,507` and `10 §6`)
> "**English and Dutch together.** Each visible string lands with its Dutch in the commit that introduces it … A test fails on a missing `nl` entry from Phase 0 on. Dutch written by a person, never machine-translated."

> OQ 28: "use RN `src/i18n/locales/nl.json` when `en.json` has the exact same English; otherwise use backend `lang/nl.json` with the exact English key; otherwise write a natural Dutch translation now, matching RN's je/jij tone and terminology (`e-mailadres`, `wachtwoord`, `inloggen`) … Similar English wording is not an exact match."

### 2.4 No API endpoints are involved.

---

## 3. Facts verified at `f391a1fa` [F]

**Catalogs.** There are 14 `.xcstrings` files at the pin. The generator reads the **13** in this table. It does not read `PlanCore/Resources/PlanCopy.xcstrings`, because plan copy ships as `plan_copy.json` (T3). Paths below `Packages/ChronoFlowKit/Sources/` are shortened.

| # | Catalog path | Prefix | `CopyTable` | Keys | Catalog plurals |
|---|---|---|---|---:|---:|
| 1 | `App/Localizable.xcstrings` | `app` | `App` | 4 | 0 |
| 2 | `App/InfoPlist.xcstrings` | — (must stay empty) | — | 0 | 0 |
| 3 | `…/DesignSystem/Resources/Localizable.xcstrings` | — (must stay empty) | — | 0 | 0 |
| 4 | `…/Auth/Resources/Localizable.xcstrings` | `auth` | `Auth` | 100 | 0 |
| 5 | `…/Launch/Resources/Localizable.xcstrings` | `launch` | `Launch` | 2 | 0 |
| 6 | `…/Welcome/Resources/Localizable.xcstrings` | `welcome` | `Welcome` | 6 | 0 |
| 7 | `…/PlanUI/Resources/Localizable.xcstrings` | `plan` | `Plan` | 104 | 2 |
| 8 | `…/PlanUI/Resources/Screens.xcstrings` | `screens` | `Screens` | 121 | 6 |
| 9 | `…/PlanUI/Resources/Walkthrough.xcstrings` | `walkthrough` | `Walkthrough` | 30 | 0 |
| 10 | `…/Reminders/Resources/Reminders.xcstrings` | `reminders` | `Reminders` | 31 | 0 |
| 11 | `…/Settings/Resources/Settings.xcstrings` | `settings` | `Settings` | 277 | 11 |
| 12 | `…/Settings/Resources/Terms.xcstrings` | `terms` | `Terms` | 15 | 0 |
| 13 | `…/TripWizardUI/Resources/Localizable.xcstrings` | `wizard` | `Wizard` | 278 | 3 (+9 `_one` pairs) |

Note that the prefix `plan` means **PlanUI**/Localizable, not PlanCore.

- **The pin.** `tools/ios-pin.txt` (0a) holds the full SHA `f391a1fac33eb0f0ea9e3f565206b91118e793d0` plus a newline. Generated headers print its first 8 characters, `f391a1fa`.
- **The App table** has exactly 4 keys: `Debug route: %@` (en `Debug route: %@`, nl `Debugroute: %@`), `My trips` (`Mijn reizen`), `Settings` (`Instellingen`) and `Today` (`Vandaag`). `Debug route: %@` is debug-only on iOS (`10` OQ-L8); its locked name is `app_debug_route_x`.
- Every catalog has `sourceLanguage` `en` and version `1.0`. Every key has `en` and `nl`, and no locale other than these two occurs.
- The only variation type is `plural`, always with exactly `one` and `other`. No `device`, `width` or `substitutions` variations occur.
- Every `stringUnit` has state `translated` (1,980 units). No `extractionState: stale` occurs; 257 keys carry `extractionState: manual`, which the generator ignores.
- **One `shouldTranslate: false` key:** Auth `xxxx-xxxx` (comment: "Recovery code format example; identical in every language.").
- **Edge whitespace:** only `screens` ` · {{time}}` has it (a leading space, in en and nl). No value has a double space, a tab, a newline or a bare `%`.
- **The prototype run** (`python3 -I` on the Appendix A code against the pin, run again for this brief) gives:
  - 928 `<string>` and 31 `<plurals>` per language;
  - 959 lock entries: settings 277, wizard 269, screens 121, plan 104, auth 100, reminders 31, walkthrough 30, terms 15, welcome 6, app 4, launch 2.
- **Collisions.** Within a table, these keys map to the same slug. The key that sorts second gets the suffix sha1(`"<prefix>\t<key>"`)[:6]:

  | Table | Keys | Names |
  |---|---|---|
  | auth | `Check your inbox` / `Check your inbox.` | `auth_check_your_inbox` / `auth_check_your_inbox_78cde4` |
  | reminders | `Turn on reminders` / `Turn on reminders?` | `reminders_turn_on_reminders` / `reminders_turn_on_reminders_fc061c` |
  | wizard | `One way` / `one way` | `wizard_one_way` / `wizard_one_way_3bfc50` |
  | wizard | `Return` / `return` | `wizard_return` / `wizard_return_c57366` |

- **Truncation vector:** wizard `Adding a return flight costs 1 credit, because credits are per direction. Other changes are free.` → `wizard_adding_a_return_flight_costs_1_cre_474862`. That is 41 characters, then `_`, then sha1(key)[:6]. `10 §9.3` misprints this name as `…_1_cr_474862`; the 48-character rule gives `…_1_cre_474862`.
- **Nine keys share English but differ in Dutch per table.** 112 keys appear in more than one of the 14 catalogs (counting `PlanCopy.xcstrings`); 85 appear in more than one of the 13 generator tables, and 8 of those differ in Dutch. The ninth, `Seek bright light`, differs only against plan copy:

  | Key | Dutch per table |
  |---|---|
  | `Add at least one drink to save this answer.` | settings "…om dit antwoord op te slaan." · wizard "…om dit antwoord te bewaren." |
  | `Bedtime` | settings `Bedtijd` · wizard `Naar bed` |
  | `Blue-blocking glasses` | settings `Blauwlichtfilterbril` · wizard `Blauwfilterbril` |
  | `Early bird` | settings `Vroege vogel` · wizard `Ochtendmens` |
  | `Night owl` | settings `Nachtuil` · wizard `Avondmens` |
  | `Sleep` | plan, screens, walkthrough and settings `Slapen` · wizard `Slaap` |
  | `Usual bedtime` | settings `Gewone bedtijd` · wizard `Meestal naar bed` |
  | `When do you naturally feel most awake?` | settings "…het meest wakker?" · wizard "…het fitst?" |
  | `Seek bright light` | screens and walkthrough `Zoek fel licht op` · PlanCopy JSON `Zoek fel licht` (differs only against the JSON) |

- **A plural whose `one` form drops the number:** `screens_your_count_preparation_days_start_df4f1b`.
  - The `one` forms use only `%2$s`: en "Your preparation day starts on %2$s.", nl "Je voorbereidingsdag begint op %2$s."
  - The `other` forms use `%1$d` and `%2$s`.
- **Integer arguments in plain strings, which Lint may flag as `PluralsCandidate`:** `auth_use_at_least_n_characters` ("Use at least %1$d characters.") and `auth_use_no_more_than_n_characters`. iOS ships them as plain strings.
- **The keys the Android overlay replaces** (exact English, with the iOS Dutch):
  - settings `You need an authenticator app, like the one built into Passwords on iPhone.` (nl "Je hebt een authenticator-app nodig, zoals die in Wachtwoorden op de iPhone.")
  - settings `Sign in with Face ID, a fingerprint or your screen lock instead of your password.`
  - reminders `Open your phone's Settings.`, `Tap Notifications, then ChronoFlow.` and `Turn on Allow notifications.`
  - These also exist and are **not** replaced: reminders `Open phone settings` → `Telefooninstellingen openen`, `Come back here. Reminders switch on by themselves.`, `Reminders on this phone` (the A13 channel name) and settings `Recovery codes` → `Herstelcodes`.
- **Plan copy:** `Packages/ChronoFlowKit/Sources/PlanCore/Resources/PlanCopy.json`:
  - 141,652 bytes, git blob `05ea1be7d8e951fa4164931d405ca417fd27cb99`;
  - **SHA-256 `927568d02d8bcd215d80fa455c1c78dc616544f2d321fc8d7898df9bf1ce584f`**;
  - top-level `{"entries": [...]}` with 289 entries (272 `single`, 9 `plural`, 8 `coded`), each with `key`, `arguments` and `languages`.
- **Toolchain:** `python3` is 3.15.2 (`/opt/homebrew/bin/python3`). Build-tools 36.0.0 and 37.0.0 are installed under `/opt/homebrew/share/android-commandlinetools/build-tools/`.

---

## 4. Out of scope

- Any UI, screen or board. Nothing in this phase is drawn.
- `CopyCatalog`, `CopyLineFormatter`, the month and weekday tables, number formatting and loading or decoding `plan_copy.json`: Phase 1a.
- The language choice (`cf.settings.language`), `ConfigurationPolicy`, locale coercion and `LocaleManager`: Phase 0e (spike) and 10b.
- Helpers that select a quantity and format a string at a call site (wizard `_one` rule, `<terms>` link parsing): the feature phases. §11 records the rules they must follow.
- Rerunning iOS `Tools/PlanCopyGen` against a newer web commit (OQ-L1: reuse the iOS output verbatim).
- AAB language splits (OQ-A39) and the size gate: Phase 13. `localeFilters` is not in plan.md; this phase adds it only under the OQ-0c-5 default (acceptance 7).
- Porting feature-level iOS copy tests (`WizardCopyTests`, `WalkthroughCopyTests`, `TermsPresentationTests`, `GuestJourneyCatalogTests`): their feature phases.
- `core/plan/src/main/resources/page_copy.json` (the PlanUI table for JVM preparation): Phase 1b adds it to this generator (§5.9).
- Using `app_debug_route_x` from app code (the route placeholder): Phases 3b/4e.

---

## 5. Decisions this brief settles (plan wins over research where they differ)

1. **Output paths follow plan.md, not `10 §9.2`.**
   - Resources go to `core/ui/src/main/res/…`, not `app/…`.
   - The plan copy goes to `core/plan/src/main/resources/plan_copy.json` (a Java resource of the JVM module), not `assets/`.
   - The pin is `tools/ios-pin.txt`, not `tools/l10n/ios-pin.txt`.
2. **The lock is append-only and never pruned.** `10 §9.3.5` says to prune deleted keys; plan A17 says append-only.
   - A key deleted on iOS loses its resource, so Kotlin references fail to compile, but its lock entry stays.
   - Its name therefore stays reserved and is never handed to different text.
3. **Replaced iOS keys are not emitted** (new OQ-0c-1).
   - A key named in an overlay `replaces` keeps its lock entry but gets no resource in `strings_ios.xml`.
   - This makes "Android never shows iOS steps" a compile-time fact. §9 shows the expected counts and the arithmetic.
4. **`CopyIndex` covers every emitted main key**, not an allowlist (new OQ-0c-3). Ported JVM models carry English keys exactly as iOS does (e.g. `SettingsHomeState.swift:17`, `TripSummaryCard.swift:30`), and the UI resolves them at runtime.
5. **The two empty catalogs are read and must stay empty.**
   - If `InfoPlist` or `DesignSystem` ever gains a key, the generator fails with `"<path> is no longer empty: decide how Android uses it"`.
   - This keeps the "13 catalogs" claim exact.
6. **Generated Kotlin** goes to `core/ui/src/main/kotlin/net/chronoflow/core/ui/l10n/generated/CopyIndex.generated.kt`, package `net.chronoflow.core.ui.l10n.generated`. This matches the 0b naming (`…/generated/*.generated.kt`). The plan text calls it `CopyIndex.kt`.
7. **Debug-only keys go to the debug source set of `:core:ui`** (new OQ-0c-4).
   - The generator has one constant, `DEBUG_ONLY = {("app", "Debug route: %@")}`. Those keys are written to `core/ui/src/debug/res/values/strings_ios_debug.xml` and `core/ui/src/debug/res/values-nl/strings_ios_debug.xml` (same header and format as `strings_ios.xml`), not to `src/main`.
   - They keep their lock entry (`app_debug_route_x`) and are **not** in `CopyIndex` (which is main code and would not compile in release).
   - Why: release and the perf variants (which fall back to the library's `release`) then ship no debug copy, as Phase 4e requires (its OQ-4e-6), while every string still lives in a generated `:core:ui` file.
8. **Hand-written untranslatable names are reserved.** Later phases add `translatable="false"` strings to `core/ui/src/main/res/values/strings_untranslatable.xml` (e.g. `plan_day_title_date` in 1d, `plan_title_comma_date` in 6b, `ui_refresh_feedback_separator` in 6a). The generator reads every `name="…"` in that file and treats it as already used, so a future iOS key never gets the same name (it gets the sha1 suffix instead). If a **locked** name equals a hand-written one, the generator fails with `"<name>" is defined in strings_untranslatable.xml and locked for <lock key>`.
9. **One more output arrives in 1b.** Phase 1b extends this generator with `core/plan/src/main/resources/page_copy.json` (OQ-1b-1). Keep `build()` returning a `dict[relpath, bytes]` of every output so that adding a file is one function and one dict entry, and `--check` covers it automatically.

---

## 6. Prototype bugs to fix (Appendix A code)

1. **Comments.** The comment truncates the escaped key at 120 characters and can cut an entity in half (`&lt;/privacy&g`). Emit the **full** key instead, so a search for the iOS text finds it: XML-escape it, replace `--` with `- -`, and if it ends in `-`, append a space.
2. **Edge whitespace.** Only the first leading space becomes `\u0020`, and trailing spaces are ignored. Emit **every** leading and trailing space as the six characters `\u0020` (a backslash escape aapt2 keeps). Inside a value, every space after the first in a run becomes `\u0020`, because aapt2 collapses runs. Example: screens ` · {{time}}` is written `<string name="screens_time">\u0020· %1$s</string>`.
3. **Literal `%`.** A literal `%` in a value without placeholders becomes `%%` plus `formatted="false"`, which displays as `%%`. Rule: a resource with no placeholders keeps `%` literal and gets `formatted="false"` if it contains `%`. A resource with placeholders turns `%` into `%%`.
4. **Placeholder positions are never checked.** The prototype converts nl with the en slots but never compares them. Hard-fail when the set of positions (the union over plural items) differs between en and nl, or when a position's conversion (`s` or `d`) differs.
5. **Unknown names.** A `{{name}}` in nl that is absent from the key and from en raises a `KeyError`. Hard-fail with a clear message instead.
6. **Quantities.** They are not whitelisted. Hard-fail unless the quantities are exactly `{one, other}` in both languages.
7. **Empty values.** Missing `nl` is checked, but empty or whitespace-only values are not. Hard-fail on them in en or nl.
8. **`shouldTranslate: false`** is ignored. Emit `translatable="false"` in `values/` only and nothing in `values-nl/`.
9. **I/O.** `read_text()` and `write_text()` have no encoding and the output paths are hard-coded. Always use `encoding="utf-8"` and `newline="\n"`.
10. **Lock and `--check`.** The lock is always rewritten, and there is no `--check`. See T1.
11. **Index.** `copy-index.json` is replaced by `CopyIndex.generated.kt` (T4).

---

## 7. Tasks (each task is one commit; tick its box in `plan.md` in the same commit)

Commits are Conventional Commits, lower-case and imperative, with **no trailer of any kind**. Each commit includes the generated files its generator change produces. Run `make targeted ONLY=…` for what you touched, and run `make quick` once before handing back.

### T1: Generator (`build(l10n): generate en and nl strings from the ios catalogs`)
Ticks plan.md l.847, "**Generator** `tools/l10n/xcstrings2android.py` …".

**Files**
- `tools/l10n/xcstrings2android.py`: stdlib only, run as `python3 -I`, executable, with a `#!/usr/bin/env python3` shebang.
- `tools/l10n/names.lock.json`: generated and committed.
- `core/ui/src/main/res/values/strings_ios.xml` and `core/ui/src/main/res/values-nl/strings_ios.xml`: generated.
- `core/ui/src/debug/res/values/strings_ios_debug.xml` and `core/ui/src/debug/res/values-nl/strings_ios_debug.xml`: generated (§5.7).
- `tools/l10n/lint-ignores.json`, initially `{}`.
- `tools/l10n/test_xcstrings2android.py` and `tools/l10n/testdata/` (small synthetic catalogs).
- The Makefile: the `strings` target, plus `generators-check`, `generate` and the tooling-tests step of `quick`.
- The Spotless config, if needed (see below).

**CLI**
```
python3 -I tools/l10n/xcstrings2android.py [--ios <path>] [--pin <sha>] [--check]
```
- `--ios` defaults to `$IOS_REPO`, else `/Users/rickgout/Sites/chronoflow-ios`. Worktrees live in `../chronoflow-android-wt/`, so never use a relative default.
- `--pin` defaults to the stripped contents of `<repo>/tools/ios-pin.txt` (the full SHA `f391a1fac33eb0f0ea9e3f565206b91118e793d0`). Headers print `pin[:8]`. `<repo>` is resolved from `__file__` (`parents[2]`), never from the working directory.
- **Reading iOS.** First run `git -C <ios> cat-file -e <pin>^{commit}`; on failure, raise `PinNotFound`, which `main` turns into exit 2 with `iOS pin <pin> not found in <ios>`. Then run `git -C <ios> show <pin>:<path>` once per catalog. **No other git command** touches the iOS repo.
- **Without `--check`**, write every output and the lock. Exit 0 and print one summary line per output file, for example `core/ui/src/main/res/values/strings_ios.xml: 922 strings, 31 plurals`. After T1 alone (no overlay yet) the main files hold 927 en / 926 nl strings; T2 brings them to the §9 counts.
- **With `--check`**, regenerate in memory and compare bytes with the committed files. That covers both `strings_ios.xml` files, the lock and, from T2/T4 on, every other output. On any difference, exit 1, print the differing paths and the first 20 lines of a unified diff per file, and print `run make strings`. A key that needs a new lock entry is a difference too.
- **Structure, so tests need no git:**
  - `read_catalogs(ios, pin) -> dict[path, dict]` is the only function that runs git (raises `PinNotFound`);
  - `build(catalogs, lock, overlay, reserved) -> Result(files: dict[relpath, bytes], lock: dict)` is pure. `overlay` is the parsed `android-only.json` (`None` until T2); `reserved` is the set of `name="…"` values read from `strings_untranslatable.xml` (§5.8);
  - `main(argv=None, read=read_catalogs)` does argument parsing and file I/O and calls `read(ios, pin)`. Tests pass a fake `read` that returns in-memory catalogs (or raises `PinNotFound`), so no test runs git.

**Behaviour** (from `10 §9.3–§9.6`, with §6 above applied)
- **Tables.** Process them in the order of §3, rows 1 → 13. Within a table, take the keys in `sorted()` order: Python code-point order.
- **Skipped keys.** A key `K_one` is skipped when `K` exists in the same table, because it folds into `K` (wizard i18next pairs). Rows 2 and 3 must contain zero keys (§5.5).
- **The lock key** is `"<prefix>\t<English key>"`.
  - If the lock already has an entry, use its name.
  - Otherwise compute `slug(prefix, key)`. If that name is already in use (in the lock or in this run), use `name[:41].rstrip('_') + '_' + sha1(lock_key)[:6]`.
  - Every catalog key gets a lock entry, including replaced keys (T2), the debug-only key and the `shouldTranslate:false` key. Folded `_one` keys get none.
  - "In use" also covers every name in `strings_untranslatable.xml` (§5.8) and, from T2 on, every overlay name.
- **Debug-only keys** (`DEBUG_ONLY`, §5.7) are converted exactly like other keys but written to `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml`.
- **`slug`** (exactly as the prototype):
  - NFKD fold to ASCII, then lower-case;
  - `%(\d+\$)?l{0,2}d` → `n`, and `%@` → `x`;
  - `{{camelName}}` → `camel_name`;
  - each run of `[^a-z0-9]` → `_`, then strip `_`; an empty result becomes `x`;
  - the name is `prefix_slug`;
  - if it is longer than 48 characters: `name[:41].rstrip('_') + '_' + sha1(key)[:6]`. That hash is of the **key alone**, not the lock key.
- **Placeholders → positional Java arguments:**
  - Apple `%@` → `%N$s` and `%lld`/`%ld`/`%d` → `%N$d`, numbered in order of appearance;
  - `%N$@` → `%N$s` and `%N$lld` → `%N$d`;
  - `{{name}}` → `%K$s`, where K continues after the highest `%` position, in order of first appearance in the key, then in the English values;
  - **special case:** if a key says `{{count}}` and its values use `%lld`, `count` is position 1, `%1$d`. Example: `Your {{count}} preparation days start on {{date}}.` → `%1$d … %2$s`;
  - `{{count}}` in wizard and reminder values stays `%N$s`, because callers pass already formatted text.
- **Escaping, in this order:**
  1. XML-escape `&`, `<` and `>`;
  2. `\` → `\\`;
  3. newline → `\n`;
  4. `'` → `\'`;
  5. `"` → `\"`;
  6. a leading `@` or `?` gets a `\` prefix;
  7. edge whitespace and space runs as in §6.2.

  The typographic characters `’ “ ” … – · ½ −` stay literal (UTF-8). The Auth `<terms>…</terms>` and `<privacy>…</privacy>` tags are emitted escaped (`&lt;terms&gt;`), so `getString` returns them as literal text.
- **Plurals:**
  - a catalog `variations.plural.{one,other}` becomes `<plurals name="…"><item quantity="one">…</item><item quantity="other">…</item></plurals>`, always `one` before `other`;
  - for a wizard `K` + `K_one` pair, `K_one`'s value is `one` and `K`'s value is `other`, in both languages;
  - caller-selected pairs such as `Stop in {{airport}} · {{count}} complete night` and `…nights` are two separate `<string>`s, unchanged.
- **`shouldTranslate:false`** → `<string name="…" translatable="false">` in `values/` only.
- **Hard failures** (exit 1, naming the catalog and key):
  - missing `nl`;
  - an empty or whitespace-only value;
  - a variation other than `plural`, or quantities other than exactly `{one,other}`;
  - a position or conversion mismatch between en and nl;
  - an unknown `{{name}}`;
  - a duplicate resource name in the output;
  - a non-empty catalog in row 2 or 3;
  - any value containing `PENDING` or `rick@example.com` (ported from the iOS Auth `CatalogTests` line 50, applied to every table).
- **File format (byte-exact, LF, UTF-8, trailing newline):**
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- GENERATED by tools/l10n/xcstrings2android.py from chronoflow-ios@f391a1fa. Do not edit; run make strings. -->
  <resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources,Typos">
      <!-- settings: Early bird -->
      <string name="settings_early_bird">Early bird</string>
  ...
  </resources>
  ```
  Indentation is 4 spaces, and 8 for `<item>`. Each entry has a comment line `<!-- <prefix>: <full key> -->`.
- **`lint-ignores.json`** maps a resource name to `{"ids": ["PluralsCandidate"], "reason": "…"}`. The generator adds `tools:ignore="<ids>"` to that one element, the same in both languages.
  - Add an entry only when Lint flags a generated resource whose text iOS parity fixes (likely `PluralsCandidate` on the two `auth_use_…_n_characters` strings, and possibly `ImpliedQuantity` on the `df4f1b` plural, whose `one` items omit the number; `10 §4`, `10 §11`). Lint runs with `warningsAsErrors = true` (0a), so a warning counts. Run `make lint` to find out.
  - Never ignore `MissingTranslation`, `ExtraTranslation`, `StringFormatInvalid` or `StringFormatCount` for a whole file.
- **Lock file:** `json.dumps(dict(sorted(lock.items())), ensure_ascii=False, indent=1) + "\n"`.

**Makefile** (0a defines `GENERATORS :=` and `GENERATOR_CHECKS :=`, and `generate`/`generators-check` run them; 0b adds its lines the same way)
- `IOS_REPO ?= /Users/rickgout/Sites/chronoflow-ios`, on its own line. If 0b or 0d already landed it at the stack top, reuse theirs.
- `strings:` = `python3 -I tools/l10n/xcstrings2android.py --ios "$(IOS_REPO)"`. It replaces 0a's stub (`make strings: lands in Phase 0c …; exit 1`).
- `strings-check:` (new, `.PHONY`) = the same command with `--check`, then `python3 -I -m unittest discover -s tools/l10n -t tools/l10n -p 'test_*.py'` (AGENTS.md: `generators-check` also runs the generator tests).
- Directly under 0a's empty definitions, one line each: `GENERATORS += strings` and `GENERATOR_CHECKS += strings-check`.
- **Python tests.** `Scripts/test_scripts.sh` (0a; run by `make quick`) gains one line after its existing discover line: `python3 -I -m unittest discover -s "$ROOT/tools/l10n" -t "$ROOT/tools/l10n" -p 'test_*.py'`. `-t` puts `tools/l10n` on `sys.path`, so the tests `import xcstrings2android` and `import plan_copy` directly.
- 0b, 0d and 0e edit the same lines in parallel. Add yours on their own lines so the orchestrator's rebase conflicts stay trivial.

**Spotless.** 0a's `SpotlessConventionPlugin` already excludes `**/generated/**` and `**/*.generated.kt` from the Kotlin target. Verify it; change nothing unless it is missing. The generated Kotlin must still compile under `allWarningsAsErrors`.

**Tests:** `tools/l10n/test_xcstrings2android.py` (`unittest`, hermetic, in-memory catalogs, total under 1 s). Each test calls `build(...)` and asserts on the output bytes or errors.
- `test_unpositioned_args_become_positional`: `%@ and %lld` → `%1$s and %2$d`.
- `test_positioned_args_keep_order`: nl `%2$@ … %1$@` stays `%2$s … %1$s`.
- `test_named_args_follow_percent_args`: `%lld … {{name}}` → `%1$d … %2$s`; named order by first use in the key, then en.
- `test_count_key_with_lld_value_is_position_one`: the `Your {{count}} preparation days start on {{date}}.` fixture → `%1$d`/`%2$s`, and the `one` item keeps only `%2$s`.
- `test_plurals_one_and_other`: `<plurals>` with `one` before `other`.
- `test_wizard_one_pair_folds_into_plurals`: `{{count}} nights` + `{{count}} nights_one` → one `<plurals>`; no `_one` resource or lock entry. In the same test, an orphan `X_one` (no `X` in the table) becomes an ordinary `<string>` named `…_x_one` (§8).
- `test_unsupported_quantity_fails`: `few` fails; a `device` variation fails.
- `test_escaping`: `'`, `"`, `\`, `&`, `<terms>`, leading `@` and `?`, leading and trailing spaces (` · {{time}} ` → `\u0020· %1$s\u0020`), a double space (`a  b` → `a \u0020b`), a newline, `%` without arguments (`formatted="false"`, single `%`) and `%` with arguments (`%%`).
- `test_comment_has_full_key_and_no_double_dash`: a 200-character key with `--` and `&`.
- `test_collision_suffix_goes_to_later_sorted_key`: `Check your inbox` / `Check your inbox.` → `…_inbox` and `…_inbox_78cde4` (same algorithm as §3).
- `test_truncation_vector`: the §3 wizard key → `wizard_adding_a_return_flight_costs_1_cre_474862`.
- `test_lock_names_are_honoured`: a pre-seeded lock name wins over the slug.
- `test_removed_key_keeps_its_lock_entry`: the entry stays, and a new key with the same slug gets a suffix.
- `test_same_key_differs_per_table`: `Early bird` in settings and wizard → two names with different nl.
- `test_missing_or_empty_dutch_fails`, `test_position_mismatch_fails`, `test_conversion_mismatch_fails` (`%1$s` vs `%1$d`), `test_unknown_named_arg_fails`, `test_pending_text_fails` (also `rick@example.com`), `test_non_empty_infoplist_fails`.
- `test_missing_pin_exits_2`: `main([...], read=<fake raising PinNotFound>)` returns 2 and prints `iOS pin … not found in …`; nothing is written.
- `test_should_translate_false`: `translatable="false"` in en, absent in nl.
- `test_debug_only_key_goes_to_debug_file`: an `app` table with `Debug route: %@` and `Today` → `Debug route` only in `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml` (as `app_debug_route_x`, `%1$s`), `Today` only in `src/main`; both keys in the lock.
- `test_untranslatable_names_are_reserved`: with `strings_untranslatable.xml` defining `plan_sleep`, a new `plan` key `Sleep` gets `plan_sleep_<sha6>`; a lock entry equal to a hand-written name fails with the §5.8 message.
- `test_lint_ignores_applied_per_element`.
- `test_output_is_deterministic`: two builds give identical bytes.
- `test_check_mode_detects_drift`: `--check` against a temp dir with one changed byte exits 1. Use `tempfile` and call `main(["--root", tmp, "--pin", "0"*40, "--check"], read=fake_read)`; `--root` is an output-root override for tests only.

### T2: Android-only overlay (`feat(l10n): add android-only copy overlay`)
Ticks plan.md l.854, "**Android-only overlay.**".

**Files**
- `tools/l10n/android-only.json`
- `core/ui/src/main/res/values/strings_android.xml` and `values-nl/strings_android.xml`, generated by the same script
- `core/ui/src/main/res/values/strings_untranslatable.xml`, hand-written
- The generator and its tests
- `plan.md`: the overlay table and the new OQ rows

**`android-only.json` schema and content** (exact):
```json
{
  "entries": [
    {
      "name": "android_two_factor_intro_b1",
      "en": "You need an authenticator app.",
      "nl": "Je hebt een authenticator-app nodig.",
      "replaces": [{"table": "settings", "key": "You need an authenticator app, like the one built into Passwords on iPhone."}],
      "review": "needs review",
      "comment": "OQ-A12: 2FA intro b1 without the iPhone reference."
    },
    {
      "name": "android_passkeys_intro",
      "en": "Sign in with your fingerprint, face or screen lock instead of your password.",
      "nl": "Log in met je vingerafdruk, gezicht of schermvergrendeling in plaats van je wachtwoord.",
      "replaces": [{"table": "settings", "key": "Sign in with Face ID, a fingerprint or your screen lock instead of your password."}],
      "review": "needs review",
      "comment": "OQ-A12: passkeys intro without Face ID."
    },
    {
      "name": "android_notifications_blocked_step",
      "en": "Tap Open phone settings and turn on notifications for ChronoFlow.",
      "nl": "Tik op Telefooninstellingen openen en zet meldingen voor ChronoFlow aan.",
      "replaces": [
        {"table": "reminders", "key": "Open your phone's Settings."},
        {"table": "reminders", "key": "Tap Notifications, then ChronoFlow."},
        {"table": "reminders", "key": "Turn on Allow notifications."}
      ],
      "review": "needs review",
      "comment": "OQ-A12, OQ-0c-2: one Android step; the button is reminders 'Open phone settings'."
    },
    {
      "name": "android_recovery_codes_file_name",
      "en": "Recovery codes.txt",
      "nl": "Herstelcodes.txt",
      "replaces": [],
      "review": "needs review",
      "comment": "OQ-A12, A23: CreateDocument file name; nl from settings 'Recovery codes' = 'Herstelcodes'."
    }
  ]
}
```

**Validation** (hard fail; each one has a test):
- `name` matches `^android_[a-z0-9_]+$`, is at most 48 characters, and is unique, including against every generated iOS name;
- `en` and `nl` are non-empty;
- placeholders follow the T1 rules, and en and nl must have the same positions;
- every `replaces` entry names an existing `(prefix, key)` at the pin. A reworded iOS key must surface;
- `review` is `"needs review"` or `"approved"`;
- no key is replaced by two entries.

**Behaviour**
- `strings_android.xml` uses the T1 header, with the source line `from tools/l10n/android-only.json`. Every entry has a `<!-- android: <en> (review: needs review) -->` comment.
- Replaced iOS keys keep their lock entries but are **omitted** from `strings_ios.xml` (§5.3).
- **`strings_untranslatable.xml`** (hand-written; 0a already created it at this path, OQ-0a-4). Verify it reads exactly:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="app_name" translatable="false">ChronoFlow</string>
  </resources>
  ```
  If `app_name` is also defined anywhere else (e.g. `app/src/main/res/values/strings.xml`), delete that definition, and the file if it becomes empty. The manifest keeps `android:label="@string/app_name"`. Make sure `:app` has `implementation(project(":core:ui"))`. Later phases append their own `translatable="false"` strings to this file; the generator only reads it (§5.8).
- **`plan.md` edits in the same commit:**
  - In "Android-only copy awaiting approval", set the Recovery-codes nl cell to `Herstelcodes.txt`.
  - Set the "Replaces (iOS)" cells to the exact keys above.
  - Append the §10 rows OQ-0c-1 … OQ-0c-5 to "Open questions for the user" (its columns are `# | Question | Default | Affects`; copy the §10 Affects cells), keeping the `OQ-0c-n` ids (as 0b does with `OQ-0b-n`). Do **not** take the next `OQ-A` number: parallel and later phases (e.g. 3a) also claim numbers from OQ-A48 on (the 2026-11-16 review took OQ-A44–A47), and the orchestrator renumbers at restack.

**Tests** (in `test_xcstrings2android.py`):
- `test_overlay_written_in_both_languages`;
- `test_overlay_missing_dutch_fails`;
- `test_overlay_unknown_replaced_key_fails`;
- `test_overlay_name_clash_fails`;
- `test_replaced_keys_are_not_emitted_but_stay_locked`.

### T3: Plan copy (`build(l10n): copy the ios plan copy table with a sha-256 check`)
Ticks plan.md l.858, "**Plan copy.**".

**Files**
- `tools/l10n/plan_copy.py` (stdlib, `python3 -I`)
- `tools/l10n/test_plan_copy.py`
- `tools/l10n/plan_copy.sha256`
- `core/plan/src/main/resources/plan_copy.json`
- `core/plan/src/test/kotlin/net/chronoflow/core/plan/PlanCopyResourceTest.kt`
- Makefile: `plan-copy`, plus `generate` and `generators-check`

**Behaviour**
- Source: `git -C <ios> show <pin>:Packages/ChronoFlowKit/Sources/PlanCore/Resources/PlanCopy.json`. Read the bytes as they are, with no JSON round trip and no newline change.
- `plan_copy.sha256` holds one line in `shasum` format, so `shasum -a 256 -c tools/l10n/plan_copy.sha256` works from the repo root:
  ```
  927568d02d8bcd215d80fa455c1c78dc616544f2d321fc8d7898df9bf1ce584f  core/plan/src/main/resources/plan_copy.json
  ```
- **Without `--check`:** write the bytes and the hash file.
- **`--check`** exits 1 unless both hold:
  1. the committed file's SHA-256 equals the recorded hash;
  2. the bytes at the pin hash to the same value.

  It prints which one failed, and the committed size in bytes. Do not hard-code the size or the hash in the script: a pin bump then needs only `make plan-copy` (the pin-specific 141,652 bytes and 289 entries live in `PlanCopyResourceTest`, which the bump procedure in `docs/l10n.md` updates).
- `--ios`, `--pin` and `--root` work as in T1, and `main(argv=None, read=…)` takes a fake reader the same way, so `test_plan_copy.py` never runs git. Makefile: `plan-copy:` = `python3 -I tools/l10n/plan_copy.py --ios "$(IOS_REPO)"` (replacing the stub); `plan-copy-check:` (new, `.PHONY`) = the same with `--check`; `GENERATORS += plan-copy` and `GENERATOR_CHECKS += plan-copy-check`, one line each.

**Tests**
- `test_plan_copy.py`:
  - `test_copy_is_byte_identical`, with a fake reader returning bytes that contain `\r\n` and non-ASCII;
  - `test_check_fails_on_edited_copy`;
  - `test_check_fails_when_pin_hash_differs`.
- `PlanCopyResourceTest` (JVM, `:core:plan`, under 1 s):
  - `planCopyIsOnTheClasspathWithThePinnedHash` loads `/plan_copy.json` through `javaClass.getResourceAsStream`, and asserts the SHA-256 against the first field of `<repoRoot>/tools/l10n/plan_copy.sha256` and a size of 141,652. `<repoRoot>` is `System.getProperty("chronoflow.repoRoot")`, set in `core/plan/build.gradle.kts` as 9a does: `tasks.test { systemProperty("chronoflow.repoRoot", rootDir.absolutePath); inputs.file(rootProject.file("tools/l10n/plan_copy.sha256")).withPathSensitivity(PathSensitivity.RELATIVE) }`;
  - `planCopyHas289Entries` counts `"key":` occurrences at the entry level. Do not add a JSON dependency only for this test. If 0a already gave `:core:plan` kotlinx-serialization, parse with it instead.

### T4: CopyIndex (`feat(l10n): generate a copy index for keys resolved at runtime`)
Ticks plan.md l.859, "**CopyIndex.kt** …".

**Files**
- `core/ui/src/main/kotlin/net/chronoflow/core/ui/l10n/generated/CopyIndex.generated.kt`, written by `xcstrings2android.py` and covered by its `--check`
- Generator tests
- `core/ui/src/test/kotlin/net/chronoflow/core/ui/l10n/CopyIndexTest.kt` (JVM)
- `core/ui/src/test/kotlin/net/chronoflow/core/ui/l10n/CopyIndexResolutionTest.kt` (Robolectric)
- `core/ui/src/testDebug/kotlin/net/chronoflow/core/ui/l10n/DebugRouteResolutionTest.kt` (Robolectric, debug only)

**Generated API** (exact shape):
```kotlin
// GENERATED by tools/l10n/xcstrings2android.py from chronoflow-ios@f391a1fa. Do not edit; run make strings.
package net.chronoflow.core.ui.l10n.generated

import net.chronoflow.core.ui.R

enum class CopyTable { App, Auth, Launch, Welcome, Plan, Screens, Walkthrough, Reminders, Settings, Terms, Wizard, Android }

/** [args] names the positional arguments in order: `{{name}}` names, `count` for the count special case, `%N` for unnamed Apple args. */
class CopyEntry(val resId: Int, val isPlural: Boolean, val args: List<String>)

object CopyIndex {
    fun find(table: CopyTable, key: String): CopyEntry? =
        when (table) {
            CopyTable.App -> app(key)
            // … one branch per table …
            CopyTable.Android -> android(key)
        }

    private fun app(key: String): CopyEntry? =
        when (key) {
            "My trips" -> CopyEntry(R.string.app_my_trips, false, emptyList())
            // … ("Debug route: %@" is debug-only and absent, §5.7)
            else -> null
        }
    // … one private function per table, keys in the same order as the XML …
}
```

**Rules**
- **Keys.** Each table function holds every emitted key of its table: the English key exactly as in the catalog, including its Apple or `{{}}` placeholders. `Android` is keyed by the overlay `en` text.
- **Replaced iOS keys** follow two cases:
  - **Single-key replacement:** the overlay entry replaces exactly one key (b1, the passkeys intro). Then `find(<that table>, <that key>)` returns the overlay entry, so ported iOS model code that carries the iOS key gets Android copy.
  - **Multi-key replacement:** the three reminder steps. Then `find` returns `null`, because Phase 12c renders the single Android step explicitly.
- **Wizard pairs.** The folded `K_one` key is not indexed; `K` maps to the `R.plurals` id with `isPlural = true`.
- **Debug-only keys** (§5.7) are not indexed: `find(App, "Debug route: %@") == null`.
- **Literals.** Kotlin string literals escape `\` → `\\`, `"` → `\"`, `$` → `\$` and newline → `\n`, and every other control character becomes `\uXXXX`.
- **Arguments.** Use `emptyList()` when a key has none.
- **Real keys that need escaping:** screens `Couldn't save "{{step}}" as done, so we undid it.` and settings `Checked off "{{step}}"` contain `"`. No key contains `$` or `\` today.
- **Performance.** No collection is allocated at class load: no `mapOf`, no arrays in `init`. A `when` on a `String` compiles to a hash switch. Lookups are for error paths, settings and models, never inside the timeline draw.

**Tests**
- Generator tests (in `test_xcstrings2android.py`): `test_copy_index_escapes_kotlin_literals` (a synthetic key with `"`, `$`, `\` and a newline gives a valid escaped literal, e.g. `"a \"b\" \$c \\ d\n"`); `test_copy_index_redirects_single_replacement_only`; `test_copy_index_omits_debug_and_one_keys`.
- `CopyIndexTest` (JVM; compares ids only, no Robolectric):
  - `splitDutchKeysResolvePerTable`: `find(Settings,"Early bird")!!.resId != find(Wizard,"Early bird")!!.resId`, and the same for `Sleep` in `Plan` and in `Wizard`;
  - `catalogPluralHasCountArg`: `find(Settings,"%lld credits left")` is a plural with args `["%1"]`;
  - `countSpecialCaseNamesPositionOne`: `find(Screens,"Your {{count}} preparation days start on {{date}}.")!!.args == listOf("count","date")`;
  - `wizardPairIsOnePlural`: `find(Wizard,"{{count}} nights")!!.isPlural`, and `find(Wizard,"{{count}} nights_one") == null`;
  - `singleReplacementRedirectsToOverlay`: `find(Settings,"You need an authenticator app, like the one built into Passwords on iPhone.")!!.resId == R.string.android_two_factor_intro_b1`;
  - `multiReplacementIsNull`: `find(Reminders,"Open your phone's Settings.") == null`;
  - `debugOnlyKeyIsNotIndexed`: `find(App,"Debug route: %@") == null`, and `find(App,"Today")` is non-null;
  - `unknownKeyIsNull`.
- `CopyIndexResolutionTest` (Robolectric, one class, each method under 5 s):
  - It uses `RuntimeEnvironment.getApplication()` and method-level `@Config(qualifiers = "en")` or `"nl"`, with class-level `@RunWith(AndroidJUnit4::class) @Config(sdk = [36])`. It proves that aapt2 compiled the escaping correctly.
  - **Tier.** If 0e's `net.chronoflow.core.testing.tiers.Logic` exists at the stack top, annotate the class `@Logic`. If it does not (0e runs in parallel), leave it untagged and say so in the hand-back; the orchestrator adds `@Logic` at restack. Never use a JUnit4 `Timeout` rule.
  - **Dependencies.** If `chronoflow.android.library` does not already add them, add to `core/ui/build.gradle.kts`: `testImplementation(libs.robolectric)`, `testImplementation(libs.androidxTest)` (androidx.test:core), `testImplementation(libs.androidxTestExtJunit)`, `testImplementation(libs.assertk)`, `testImplementation(libs.junit4)` (0a catalog aliases; use the exact alias names 0a committed).
  - `earlyBirdDutchDiffersPerTable` (nl): `Vroege vogel` and `Ochtendmens`.
  - `dutchPluralPicksQuantity` (nl): `settings %lld credits left` with 1 → `Nog 1 tegoed over`, with 3 → `Nog 3 tegoeden over`.
  - `leadingSpaceSurvives` (en): `screens " · {{time}}"` with `"14:00"` → `" · 14:00"`.
  - `apostropheAndTermsTagsAreLiteral` (en): `reminders_allow_them_in_your_phone_s_sett_2852cd` → `Allow them in your phone's settings to get reminders.` (its `'` kept), and `auth_by_creating_an_account_you_agree_to_9e09cb` → `By creating an account you agree to the <terms>terms</terms> and <privacy>privacy policy</privacy>.` (tags literal). Both names are from the prototype lock; if your lock differs, use the lock's name.
  - `untranslatableFallsBackToDefault` (nl): `auth_xxxx_xxxx` → `xxxx-xxxx`.
  - `overlayDutch` (nl): `android_two_factor_intro_b1` → `Je hebt een authenticator-app nodig.`
- `DebugRouteResolutionTest` (Robolectric, `core/ui/src/testDebug/…`, same class annotations and tier rule as above), two methods: `debugRouteResolvesInDebug` (en): `getString(net.chronoflow.core.ui.R.string.app_debug_route_x, "today")` → `Debug route: today`; `debugRouteResolvesInDutch` (nl) → `Debugroute: today`. It must live in `src/testDebug`, not `src/test`: `testReleaseUnitTest` exists (`android.onlyEnableUnitTestForTheTestedBuildType=false`) and compiles `src/test` against the release `R`, which has no `app_debug_route_x`.

### T5: Locale wiring (`build(l10n): generate the locale config and make translation lint fatal`)
Ticks plan.md l.860, "**Locale wiring.**".

**Files**
- `app/build.gradle.kts`
- `app/src/main/res/resources.properties`
- `lint.xml`, or wherever 0a configured Lint
- The Konsist rule and its planted files
- `konsist/src/test/kotlin/…/StringResourceLocationTest.kt`

**Behaviour**
- In `:app`, set `android { androidResources { generateLocaleConfig = true } }`. Do not add `android:localeConfig` to the manifest by hand (AGP adds it).
- `app/src/main/res/resources.properties` contains exactly `unqualifiedResLocale=en` and a trailing newline.
- `lint.xml` gains:
  ```xml
  <issue id="MissingTranslation" severity="error" />
  <issue id="ExtraTranslation" severity="error" />
  <issue id="StringFormatInvalid" severity="error" />
  <issue id="StringFormatCount" severity="error" />
  <issue id="PluralsCandidate" severity="error" />
  ```
  No `<ignore path=…>` for these ids, and no baseline entry for them.
- **Konsist rule `noLiteralTextInText`** (0a Task 6b structure: one function in `KonsistRules.kt`, three tests in `KonsistRulesTest.kt`: `noLiteralTextInText()`, `noLiteralTextInTextRejectsPlantedViolation()`, `noLiteralTextInTextAllowsControl()`):
  - It applies to files whose `ProjectPaths.sourceSetOf(path)` is not a test source set (`isTestSourceSet` false): main, debug, release and perf.
  - It skips functions with an annotation whose name ends in `Preview`, such as `@Preview` or `@PreviewLightDark`.
  - It reports any function body matching `\b(Basic)?Text\(\s*(text\s*=\s*)?("|""")`, so it covers a positional literal, a named `text =` literal, a string template and a raw string.
  - `TextField(`, `CfText(` and `MyText(` do not match (the `\b` before `Text` and the optional `Basic` prefix see to that; test it).
  - Planted violation, module-shaped so `moduleOf`/`sourceSetOf` work: `konsist/src/test/resources/violations/noLiteralTextInText/feature/today/src/main/kotlin/net/chronoflow/feature/today/TextLiteral.kt` with three composables: `Text("Hello")`, `Text(text = "Hi $name")` and `BasicText("""raw""")`. Each one alone must fail the rule; put them in three functions and assert the failure message names all three.
  - Allowed control: `konsist/src/test/resources/allowed/noLiteralTextInText/feature/today/src/main/kotlin/net/chronoflow/feature/today/TextAllowed.kt` (`Text(stringResource(R.string.x))`, `Text(text = label)`, `TextField(value = "", onValueChange = {})` and a `@Preview` function with `Text("Preview")`), plus `…/feature/today/src/test/kotlin/net/chronoflow/feature/today/TextInTest.kt` with `Text("test")`.
  - Planted files are resources: parsed, never compiled, excluded from Spotless; they must still be valid Kotlin.
- **`StringResourceLocationTest`** (JVM, in `:konsist`, `konsist/src/test/kotlin/net/chronoflow/konsist/StringResourceLocationTest.kt`; finds the repo root as `ModuleGraphTest` does, then walks it, skipping `build/`, `.gradle/`, `_design/`, `.git/` and `konsist/src/test/resources/`):
  - It finds every `*/src/<sourceSet>/res/values*/*.xml` that contains `<string `, `<plurals ` or `<string-array `.
  - It asserts that each one is in the allowlist: `core/ui/src/main/res/values{,-nl}/strings_ios.xml`, `core/ui/src/main/res/values{,-nl}/strings_android.xml`, `core/ui/src/main/res/values/strings_untranslatable.xml` and `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml`.
  - It asserts that no `values-*` directory other than `values-nl` holds strings.
  - Method: `stringsLiveOnlyInTheGeneratedCoreUiFiles`.
  - **Inputs.** Extend 0a's `tasks.test { inputs.files(fileTree(rootDir) { … }) }` in `konsist/build.gradle.kts` with `include("**/src/*/res/values*/*.xml")`, or the test stays UP-TO-DATE when a string file is added.

### T6: StringParityTest and docs (`test(l10n): check en and nl string parity`)
Ticks plan.md l.864, "**StringParityTest** …".

**Files**
- `core/ui/src/test/kotlin/net/chronoflow/core/ui/l10n/StringParityTest.kt` (JVM; parses the XML with `javax.xml.parsers.DocumentBuilderFactory`; no Android API)
- `docs/l10n.md`

**`StringParityTest` methods.** Each test reads every `strings_*.xml` under `src/main/res/values{,-nl}` and `src/debug/res/values{,-nl}` of `:core:ui` (paths relative to the module directory, which is the test working directory). Main and debug are checked as two separate sets. Plain JVM test: no tier annotation, no Android API.

**Where it runs.** Before 0e lands, `make quick` runs only the JVM modules' `test` tasks, so `:core:ui` unit tests run only through `make test`, `make targeted` and the acceptance commands. Once 0e's Robolectric logic tier lands, `make quick` covers them; the reviewer checks that at restack (§12).
- `everyKeyHasDutchText`, ported from iOS `AppCoreTests/CatalogTests.everyKeyHasDutchText`: the set of en names whose `translatable` is not `false` equals the set of nl names, and no nl value is blank.
- `untranslatableStringsHaveNoDutch`: every `translatable="false"` name is absent from nl.
- `placeholdersMatchPerQuantity`: for each `<string>`, the multiset of `%N$[sd]` is the same in en and nl. For each `<plurals>`, en and nl have the same quantities, `{one, other}`, and each quantity has the same placeholder set in both languages.
- `positionsAreContiguous`: within a resource, the union of positions over all items is `1..N`, with no gap. The `df4f1b` plural passes, because `other` carries `%1$d`.
- `noUnpositionedArgs`: no bare `%s` or `%d` remains, and `%%` occurs only in resources that have placeholders.

**`docs/l10n.md`** (≤ 150 lines) must contain:
- the pipeline and commands (`make strings`, `make plan-copy`, `--check`, `IOS_REPO`);
- how to bump the pin: update `tools/ios-pin.txt`, run `make strings plan-copy`, review the XML diff, update the pin-specific numbers in `PlanCopyResourceTest` (size, entry count), fix the call sites that no longer compile, commit;
- how to add Android-only copy: an `android-only.json` entry with Dutch by the OQ 28 order, `review: "needs review"`, and a row in plan.md "Android-only copy awaiting approval";
- the measured counts, with the commands that produced them (§9);
- every difference from the prototype's 928/31: the 5 omitted replaced keys, the 1 debug-only key and the 1 untranslatable;
- each `lint-ignores.json` entry with its reason;
- the drift proofs and the planted-Lint proof from the acceptance, with the exact command output lines;
- the consumer rules of §11.

---

## 8. Edge cases checklist (each one is covered by a named test above)

- The wizard `_one` key exists but its base key does not: treat it as an ordinary key named `…_one`. None exist today; `test_wizard_one_pair_folds_into_plurals` covers both this and the normal case.
- A key whose English value differs from the key: PlanCopy `#case` rows are not read; wizard `_one` keys are folded; the one other case is wizard `wizard.room.from` (en `From`, a dotted id rather than a sentence), which is named `wizard_wizard_room_from`. Use the **values** for text and the **key** for naming and comments.
- A Dutch `one` form that omits the number: allowed; see `positionsAreContiguous`.
- A key containing `$`, `"` or `\`: valid Kotlin literals in `CopyIndex` (two real keys contain `"`, T4). Covered by `test_copy_index_escapes_kotlin_literals`.
- Two runs, or a run on another machine: byte-identical output, with no timestamps and no dict-order dependence.
- The iOS repo is missing or the pin is absent: exit 2 with a message (`test_missing_pin_exits_2`). Never fall back to the working tree.

---

## 9. Acceptance criteria

The standard acceptance of plan.md "Tasks" applies. In addition, run from the repo root with `export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`:

1. `python3 -I -m unittest discover -s tools/l10n -t tools/l10n -p 'test_*.py'` passes in under 2 s, and `Scripts/test_scripts.sh` runs it.
1a. `./gradlew :core:ui:testDebugUnitTest :core:ui:testReleaseUnitTest :core:plan:test :konsist:test` passes (`StringParityTest`, `CopyIndexTest`, `CopyIndexResolutionTest`, `DebugRouteResolutionTest` (debug only), `PlanCopyResourceTest`, the Konsist rule and `StringResourceLocationTest`; the release run proves `src/test` compiles without the debug string); each JVM method < 1 s, each Robolectric method < 5 s (`build/test-results`).
2. `make strings && make plan-copy && git status --porcelain` prints nothing: the committed output equals the regenerated output.
3. `python3 -I tools/l10n/xcstrings2android.py --check` and `python3 -I tools/l10n/plan_copy.py --check` exit 0. `shasum -a 256 -c tools/l10n/plan_copy.sha256` prints `OK`.
4. **Counts** (record them in `docs/l10n.md`):

   | File | `grep -c '<string '` | `grep -c '<plurals '` |
   |---|---:|---:|
   | `core/ui/src/main/res/values/strings_ios.xml` | 922 | 31 |
   | `core/ui/src/main/res/values-nl/strings_ios.xml` | 921 | 31 |
   | `core/ui/src/debug/res/values/strings_ios_debug.xml` | 1 | 0 |
   | `core/ui/src/debug/res/values-nl/strings_ios_debug.xml` | 1 | 0 |
   | `core/ui/src/main/res/values/strings_android.xml` | 4 | 0 |
   | `core/ui/src/main/res/values-nl/strings_android.xml` | 4 | 0 |

   - The arithmetic: the prototype's 928 − 5 replaced − 1 debug-only (`app_debug_route_x`) = 922 in en; nl has 1 fewer, because `auth_xxxx_xxxx` is `translatable="false"` = 921.
   - `python3 -c "import json;print(len(json.load(open('tools/l10n/names.lock.json'))))"` prints `959`.
   - If a count differs, explain it in `docs/l10n.md` before handing back.
5. **Drift proofs, once each, then revert.** Record the failing output lines in `docs/l10n.md`.
   - (a) Change one character in `values-nl/strings_ios.xml`: `make generators-check` fails, naming that file.
   - (b) Append a space to `plan_copy.json`: `make generators-check` fails, naming the SHA-256.
6. **Planted Lint proof, once, then remove it.** Add `core/ui/src/main/res/values/strings_planted.xml` with `<string name="planted_en_only">Planted</string>`. Then `./gradlew :app:lintDebug` fails with `MissingTranslation` for `planted_en_only`, and `make quick` fails in `StringResourceLocationTest`. Record both lines.
7. **Locale config.**
   - `./gradlew :app:assembleDebug`, then:
     ```
     $ANDROID_HOME/build-tools/36.0.0/aapt2 dump xmltree --file res/xml/_generated_res_locale_config.xml app/build/outputs/apk/debug/app-debug.apk
     ```
     It shows exactly two `locale` elements, `android:name="en"` and `android:name="nl"`.
   - If AGP names the file differently, find it with `aapt2 dump xmltree --file AndroidManifest.xml … | grep -i localeConfig` and `aapt2 dump resources … | grep -i locale`. Record the command you used.
   - If locales other than en and nl appear (from AAR dependencies), add `androidResources { localeFilters += listOf("en", "nl") }` to `app/build.gradle.kts` (OQ-0c-5 default), rebuild and dump again until only en and nl remain. Record both dumps in `docs/l10n.md` and the extra locales in OQ-0c-5.
   - Also confirm the release AAB/APK has no `app_debug_route_x`: `./gradlew :app:assembleRelease` (or the release variant 0a configured), then `aapt2 dump resources app/build/outputs/apk/release/app-release-unsigned.apk | grep -c app_debug_route_x` prints `0` (release has no `signingConfig` in 0a, so the APK is unsigned; use the path the build prints if it differs). If the release build cannot run in the sandbox, report the command for the orchestrator.
8. The Konsist `Text` rule rejects `violations/noLiteralTextInText/…/TextLiteral.kt` and accepts `allowed/noLiteralTextInText/…/TextAllowed.kt` and `…/TextInTest.kt` (T5).
9. `make quick` passes, warm, inside its 60 s budget; the new tests add under 3 s. `make check` passes with `pixel` booted; the orchestrator runs it if the sandbox cannot.
10. Six commits, one per task, each ticking its plan.md box. No trailers. `git log --format=%B` shows no `Co-Authored-By`.

No screenshots and no perf numbers apply to this phase (it has no UI and no runtime path).

---

## 10. Open questions (proceed on the default; T2 adds OQ-0c-1 … OQ-0c-5 to plan.md with these ids)

| # | Question | Default | Affects |
|---|---|---|---|
| OQ-0c-1 | Emit iOS keys that an Android overlay entry replaces, or omit them? | **Omit.** Their lock names stay reserved, and `CopyIndex` redirects single-key replacements. | 0c, 5b, 10c, 12c |
| OQ-0c-2 | Which iOS "notifications blocked" steps does the one Android step replace? | `Open your phone's Settings.`, `Tap Notifications, then ChronoFlow.` and `Turn on Allow notifications.` (the list in iOS `Settings/RemindersView.swift:116`). Keep `Come back here. Reminders switch on by themselves.` (Phase 12c re-checks the permission on resume, as iOS does). | 12c |
| OQ-0c-3 | Should `CopyIndex` cover every key, or only an allowlist of runtime keys? | **Every emitted main key.** Ported models carry English keys as on iOS; the cost is one generated `when` per table and no allocation at load. | 0c, 5a–5d, 6d, 7, 9c–9d |
| OQ-0c-4 | Where does the debug-only `Debug route: %@` live? The 4e brief (§5.8, OQ-4e-6, its release test) names it `app_debug_route` in `app/src/debug/res/values{,-nl}/`. | **`app_debug_route_x` in `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml`**, generated (the lock name; `10` OQ-L8). 4e uses `net.chronoflow.core.ui.R.string.app_debug_route_x` from `app/src/debug` and its release test asserts that `net.chronoflow.core.ui.R.string` has no field `app_debug_route_x`. The orchestrator corrects the 3b brief (its `RoutePlaceholder` lives in `app/src/main` and says to find the name in `strings_ios.xml`: the debug text must move to `app/src/debug`, because main code cannot reference a debug-only resource), the 4e brief, and 9c's reference to "the 4e `app_debug_route` pattern", before 3b starts. | 3b, 4e, 9c |
| OQ-0c-5 | Does AGP's generated locale config list only en and nl? | Expected yes. If AAR locales appear, set `androidResources.localeFilters` to `en`, `nl` in `:app` so the system per-app language list shows exactly en and nl (A17, `10 §9.9`) and Material components never show a third language; record the extra locales here and in `docs/l10n.md`. | 0c, 13 |
| (under OQ-A12) | Dutch file name for the recovery codes | `Herstelcodes.txt`, from settings `Recovery codes` → `Herstelcodes`. Marked "needs review". | 5b |
| (process) | Lint flags a generated string that iOS parity fixes (`PluralsCandidate` and similar) | A per-element `tools:ignore` through `lint-ignores.json`, with a reason, listed in `docs/l10n.md`. Never per file or global. | 0c |

---

## 11. Rules for later phases (record them in `docs/l10n.md`; do not implement them here)

- **Resources.** Read them with `stringResource(R.string.x, …)` and `pluralStringResource(R.plurals.x, count, …)` from `net.chronoflow.core.ui.R`. Use the generated name; never use `getIdentifier`.
- **Wizard `_one` plurals.** Pick the quantity as iOS does (`WizardStrings.swift:13-16`): `if (countText == "1") 1 else 2`, using `other`. Pass `countText` as `%1$s`. Never pass a truncated `Double`.
- **Auth `<terms>` and `<privacy>`** arrive as literal tags. A small parser builds an `AnnotatedString` with `LinkAnnotation.Url` (`10 §9.5`).
- **Unknown runtime keys.** `CopyIndex.find` returns `null`. Callers show the same generic fallback as iOS, never the raw English server sentence.
- **Background work** resolves the language explicitly (`10 §9.9`), never from the process locale.

---

## 12. Review checklist (for the Opus reviewer)

1. **Sources stay read-only.** The generator runs only `git -C <ios> cat-file -e` and `git -C <ios> show <pin>:<path>`; grep the script for `subprocess` calls. It uses `python3 -I` and the stdlib only (no `import yaml`, `lxml` or similar), opens every file with `encoding="utf-8"` and writes with `newline="\n"`.
2. **The pin comes from `tools/ios-pin.txt`** (full SHA; headers print `f391a1fa`). Nothing reads the iOS working tree directly (no `open('/Users/…/chronoflow-ios/…')`).
2a. **Debug-only key.** `app_debug_route_x` exists only in `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml`, is absent from `CopyIndex`, and the release resource dump has no `app_debug_route_x`. `DebugRouteResolutionTest` is in `src/testDebug`, not `src/test`. OQ-0c-4 is in plan.md so the orchestrator fixes the 3b, 4e and 9c briefs.
2b. **Reserved names.** The generator reads `strings_untranslatable.xml` names as used; `app_name` stays defined once.
3. **Each §6 prototype bug is fixed.** Look in particular at the full-key comments, all edge spaces as `\u0020`, the `%` with `formatted="false"` rule, and the en/nl position and conversion check.
4. **Names.**
   - The lock has 959 entries. Spot-check `auth_check_your_inbox_78cde4`, `wizard_return_c57366` and `wizard_adding_a_return_flight_costs_1_cre_474862`.
   - The lock was never pruned or rewritten out of order: it is sorted and has `indent=1`.
   - Replaced keys are in the lock but not in `strings_ios.xml`.
5. **The nine split keys** have two different names, with the Dutch from §3. `Sleep` is `Slaap` only under `wizard_sleep`.
6. **Plurals.** All 31 have `one` before `other` in both languages. The 9 wizard pairs are `<plurals>`, and no `_one` resource exists.
7. **Escaping.** `screens_time` is written `\u0020· %1$s` in the XML and resolves to ` · %1$s`. Apostrophes are `\'`. The Auth consent strings contain `&lt;terms&gt;`. No raw `&` remains (`Plan &amp; credits`).
8. **The overlay** matches §T2 exactly: the four entries, `review` set, and the plan.md table and OQ rows updated in the same commit.
9. **`plan_copy.json`** is byte-identical: `shasum -a 256` gives `927568d0…584f`, size 141,652. It sits under `core/plan/src/main/resources/`, not under `assets/` or `:app`.
10. **`CopyIndex`.**
    - It is generated, covered by `--check`, and has no `mapOf` or arrays at load.
    - It redirects single replacements and returns null for multi-key ones.
    - `$` is escaped in literals. The wizard `_one` keys are absent.
11. **Lint.**
    - The five ids are `severity="error"`.
    - The only file-level `tools:ignore` on generated XML is `UnusedResources,Typos`.
    - Every `lint-ignores.json` entry is justified in `docs/l10n.md`.
    - No `lint-baseline.xml` entry covers a translation id.
12. **Locale config.** The aapt2 dump shows only en and nl (with `localeFilters` only if OQ-0c-5 needed it, and the extra locales recorded). `resources.properties` is in `app/src/main/res/`. There is no hand-written `android:localeConfig`.
13. **`app_name`** is defined once, in `core/ui/…/strings_untranslatable.xml`, with `translatable="false"`. `StringResourceLocationTest` passes.
14. **The Konsist rule** catches named, template and raw literals, ignores `@Preview…` functions, and has both a planted violation and an allowed sample.
15. **Tests.**
    - Python tests are hermetic (no git, no iOS path).
    - JVM tests take < 1 s and the Robolectric tests < 5 s each, with no `Thread.sleep` and no JUnit4 `Timeout` rule.
    - Python tests inject a fake reader into `main(…, read=…)`; none runs git.
    - Test names match §7.
    - `make quick` runs the Python tests (through `Scripts/test_scripts.sh`).
    - At restack with 0e: `make quick` also runs `:core:ui` `StringParityTest`, `CopyIndexTest` and (as `@Logic`) `CopyIndexResolutionTest` and `DebugRouteResolutionTest`; read the result XML to confirm, and add `@Logic` if the worker could not.
    - The Konsist rule follows 0a's layout (`violations/noLiteralTextInText/<module path>`, `allowed/noLiteralTextInText/…`, three tests) and `konsist/build.gradle.kts` declares the `res/values*/*.xml` inputs.
    - Makefile changes are only `IOS_REPO`, the four `+=` lines, the `strings`/`strings-check`/`plan-copy`/`plan-copy-check` targets (`strings-check` also runs the `tools/l10n` unittests) and one line in `Scripts/test_scripts.sh`.
    - New open questions use `OQ-0c-n` ids, not `OQ-A` numbers.
16. **Git.** Six atomic commits; each ticks its plan.md box; generated files are committed with their generator change; no trailers.
17. **Typical mistakes to reject:**
    - output written to `app/` or `assets/` (research layout);
    - the 9 split keys merged into one name;
    - `getQuantityString` used with a parsed `Double`;
    - `String.format` or `MessageFormat` added to `CopyIndex`;
    - a global `tools:ignore="MissingTranslation"`;
    - a regenerated lock that renamed existing entries;
    - `PlanCopy.json` re-serialized (whitespace or key order changed);
    - the overlay Dutch machine-translated or altered from §T2.
