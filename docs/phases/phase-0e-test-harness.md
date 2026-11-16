# Phase 0e brief: Test harness, device profiles and perf wiring

- **Branch:** `feat/phase-0e-test-harness`, from the stack top once 0a is in it. **Wave:** 2, parallel with 0b, 0c and 0d, each in its own worktree (CLAUDE.md).
- **Depends on:** 0a. You reuse what 0a landed: the convention plugins, every A3 module as a skeleton, `:konsist` with planted violations, the `Makefile`, `Scripts/run-tier.sh` → `Scripts/run_tier.py`, `Scripts/emulator.sh`, `Scripts/targeted.py`, `Scripts/measure-tiers.sh`, `Scripts/test_scripts.sh` (discovers `Scripts/tests/test_*.py`), `verifyReleaseManifest`, `@Smoke` in `net.chronoflow.core.testing.tiers`, `AppLaunchSmokeTest` and `docs/tooling.md`. Read them before you start (§2.1).
- **Plan entry:** `plan.md` "### Phase 0e: Test harness, device profiles and perf wiring" (l.921–991). Also A3, A10, A11, A19, A21 (l.86–112), "Android spec: build" (l.282–319), "process, activity and launch sequence" (l.321–346), "configuration and appearance" (l.348–355), "storage layout" (l.385–398), "seeds and test hooks" (l.465–471), "Performance budget" (l.491–538), OQ-A22, A24, A27, A28, A29 (l.570–618).
- **Boards:** none. This phase draws no screen and adds no visible string. Harness probes render literal test text inside tests only.
- **API endpoints:** none. No network code.
- **Check 2026-11-16 (adversarial pass against iOS `f391a1fa`, the AVD config, research 11/13/14, plan.md, AGENTS.md and the 0a/0d/1e/2 briefs):** iOS `ui-test-tiers.md` line references corrected; `hw.mainKeys=no` no longer read as gesture navigation (Step 0 measures it); `:app` must not implement the new modules; run-tier timing globs are task-specific; `make test` avoids release-variant Robolectric; `kotlinx-serialization-json` added to `:core:testing`; `ProfileQualifiersTest` is plain JVM; `assertTouchTargets` uses layout size; planted guard layouts are "good + one defect"; the harness capture moved under `harness/`; OQ-0e-21 (Konsist vs the 1 s budget).
- **Check 2026-11-16, second pass (against 0a's brief, iOS `f391a1fa`, the AVD config, research 13/14, plan.md, AGENTS.md, the 1d/1e/2/3a briefs):** `:core:testing-junit`'s own tests keep Vintage; planted tier files get source-set paths and per-file scopes, and the benchmark rule names `benchmark/src/main/` itself (0a's `moduleOf` skips it); `testingNotInMain` exempts `:benchmark`; `:app` gets `ui-test-manifest` in T5 (its smoke test needs `ComponentActivity`); `:core:testing-compose` exposes `:core:model`; `ProfileHostTest`/`LargeFontEqualsCapTest` are parameterised (a class rule cannot switch profiles per method); `check` reads only `testReleaseUnitTest` XML; `make captures` reruns the test tasks and clears `build/captures`; the theme seed case uses `sdkInt = 30`; `:app` coroutine deps; `:benchmark` dependencies, self-instrumentation and `targetAppId`; a fallback for the perf dependency configurations; the T5-late path; stale OQ notes.
- **Tests:** every test follows AGENTS.md "Tests": the cheapest layer; budgets JVM < 1 s, Robolectric < 5 s, emulator < 10 s; exact queries; `waitUntil…`, never `Thread.sleep`/`SystemClock.sleep`; seeded launches; one fix round that changes nothing means stop and rethink.

Tags: **[F]** a fact verified in source while writing this brief (2026-11-16). **[R]** a rule this brief adopts. **[verify]** something you confirm during the phase and record in `docs/tooling.md`.

---

## 1. Goal

Before any product code exists, every test tier works and is cheap, bounded and honest:

1. **Device profiles.** `profiles.json` holds five profiles. The `pixel` AVD insets are measured, never guessed. Production reads insets through `LocalSafeInsets`; tests inject the profile insets through the same local.
2. **Robolectric + Roborazzi** run on the JDK 21 test launcher. `ProfileRule` sets size, density, capped font scale, locale and night mode. Reference-free **guards** assert fit, touch targets, safe area, overlap, headings and ink. Stored references stay opt-in. `make captures` works on Robolectric (the emulator path is Phase 2's).
3. **Tier registry.** Every Robolectric and emulator test class carries exactly one tier annotation. A Gradle property selects the tier per run. The tier runner records each test's duration and fails a run with an over-budget test.
4. **Debug seed harness.** `SeedReceiverActivity`, `TestSeed`/`TestSeeds` with the iOS seed names, the `ScreenTest` emulator base, and a release-readiness test that proves none of it ships.
5. **Perf wiring.** `:benchmark` with the perf variants, a cold-start `StartupBenchmark` on the empty app, `check-benchmarks.py` with budgets and failing fixtures, and `make perf` appending to `docs/perf-startup.md`.
6. **Recorded spikes.** `ConfigurationPolicy` (font cap 1.3, en/nl coercion, LTR, the `device-config` read below API 33; OQ-A24), perf-variant sources (OQ-A28), and the instrumentation timeout (OQ-A29).

`make quick` stays under 60 s warm.

---

## 2. Inputs to read

### 2.1 This repo (read first)

| Path | Take from it |
|---|---|
| `AGENTS.md` "Tests", "Commands", "Traps", "Performance" | The test rules, the target names, the AGP 9, Robolectric and configuration-cache traps |
| `CLAUDE.md` | The orchestrator runs what your sandbox cannot (Gradle daemon, `adb`, emulator). Report the command and its exact output |
| `plan.md` l.921–991 | The phase entry and its nine checkboxes (§7) |
| `docs/phases/phase-0a-scaffold.md` Task 4 (convention plugins), Task 5 (`Smoke.kt`, `MainActivity`), Task 6b (Konsist `ProjectPaths`, `KonsistRulesTest` pattern), Task 8 (`run_tier.py`, `emulator.sh`, `Scripts/tests/`), Task 9 (Makefile, `targeted.py`) | Names and mechanisms you must extend, not duplicate |
| What 0a actually landed: `git log --oneline`, `Makefile`, `build-logic/`, `Scripts/`, `konsist/`, `docs/tooling.md` | Where 0a deviated from its brief. Follow the code, and note any deviation |
| `docs/research/13-testing-tooling.md` §3–§7 | iOS harness facts and the Android translation |
| `docs/research/11-android-stack.md` §7, §9–§10 | Perf tooling, the Roborazzi choice, AVD facts |
| `docs/research/14-gaps.md` §5, §14 | Locale/RTL policy, device and test gaps |
| `docs/research/12-built-apps.md` §2 | How iOS seeds were driven (names and values) |
| `docs/phases/phase-1e-timeline-perf.md` Task 3, Task 4 (`targetAppId`), Task 5 | 1e extends your checker, budgets and scripts. Match its shapes (T14–T16) |
| `docs/phases/phase-1d-timeline-host.md` OQ-1d-2, OQ-1d-4, §"perf entry" | `cf.freezeAt` with an ISO instant; the `@Logic` tier; `src/perf` |
| `docs/phases/phase-3a-launch-overlay.md` "LaunchOverrides" | 3a reads your `TestSeed` (`cf.reduceMotion`, `cf.freezeAt`, `CF_UI_TEST_PROFILE`) and your `ScreenTest` wait helper |
| `docs/phases/phase-2-design-system.md` §5 item 5, Task 18, OQ-2-21 | Phase 2 consumes `ProfileRule`, `ProfileHost`, `Guards`, `BoardCapture`, `LocalSafeInsets`, `Ellipsizable`, and **owns `make captures DEVICE=1`** (you leave a stub, T9) |
| `docs/phases/phase-4e-session.md` "releaseIgnoresSeedExtras" | 4e adds the plan's "the mock never seeds" release check once the mock exists |

### 2.2 iOS (read-only, at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

| Path | What to mirror |
|---|---|
| `plan.md:68` "Test tiers (user decision 2026-11-06)" | quick < 1 min, targeted, check < 5 min; never a device matrix by default |
| `plan.md:31` D1c (in "Decisions", `plan.md:25`) | Geometry from window size and all four insets; device fit through a committed device table plus pure tests, not a matrix |
| `plan.md:974` "Tooling round 3: test tiers" | Fixture insets go straight into production geometry, never simulated outside the view; reference comparisons opt-in |
| `AGENTS.md` "## Tests" | Already translated into this repo's AGENTS.md "Tests" |
| `Packages/ChronoFlowKit/Tests/SnapshotSupport/profiles.json` | Shape: named devices, size, scale, four insets, a profile name per device |
| `Docs/ui-test-tiers.md` (l.102, 121, 123, 166) | `CF_UI_TIMELINE` lines (l.102); the `screen` profile (l.121) and the `launch` profile (l.123); reset-to-first-screen-element target "under roughly 1 s" (l.166) |
| `Docs/perf-timeline.md` (l.222–234) | Missing, non-finite or unknown-unit metrics fail; an injected 0.81 s sample must fail the 0.8 s gate; raw values logged as `Timeline metric:`; no baseline file |
| `App/LaunchArguments.swift` | `-cf.freezeAt`, `-cf.readyAt`, `-cf.mainThreadStall` parse as ms: finite `Double` in `0...60_000`, else ignored; `-cf.reduceMotion` is a presence flag; `readyAt` defaults to 4600 ms |
| `App/Debug/UITestTimeline.swift`, `UITestRuntime.swift` | `CF_UI_TIMELINE id=<id\|ungated> pid=<pid> event=<event> wall=<s> uptime=<s> <details>`; recorded only when gated unless `allowUngated` |
| `Packages/ChronoFlowKit/Sources/DataAccess/AuthUITestConfiguration.swift` (the `App/Debug/` file of the same name only adds form prefills, 5a) | Seeds honoured only when `CF_AUTH_TEST_ID` parses as a UUID; id lower-cased; `CF_UI_TEST_PROFILE == "screen"` → fast launch; `CF_MOCK_DELAY` finite `0...60` s, default 0; isolated directory `AuthUITests/<id>` |
| `Packages/ChronoFlowKit/Sources/Settings/SettingsDevicePreferences.swift` | `cf.settings.theme` = `system\|light\|dark`; `cf.settings.language` = `system\|en\|nl`; with `system`, `nl` → Dutch, anything else → English |

### 2.3 Facts verified while writing this brief [F]

- **iOS `profiles.json`** has 7 devices keyed by model id, each `{alias, profile: full|compact, width, height, scale, insets{top,left,bottom,right}}`; e.g. `"iPhone18,5": alias "17e", 390 × 844, scale 3, insets 47/0/34/0`; `"iPhone14,6": alias "se", 375 × 667, scale 2, insets 20/0/0/0`.
- **iOS `AuthUITestConfiguration.init?`**: `guard let value = environment["CF_AUTH_TEST_ID"], let uuid = UUID(uuidString: value) else { return nil }`; `id = uuid.uuidString.lowercased()`; `fastLaunch = environment["CF_UI_TEST_PROFILE"] == "screen"`.
- **iOS `LaunchArguments.milliseconds(key)`**: `Double(raw)`, `isFinite`, `(0...60_000).contains`; `reduceMotion = arguments.contains("-cf.reduceMotion")`; `-cf.settings.*` live in the UserDefaults argument domain (not persisted).
- **iOS `resolvedLocale`** uses the bare language code (`"nl"`/`"en"`) plus the phone's hour cycle; it drops the region.
- **iOS `UITestTimeline`**: `wall` is `Date().timeIntervalSince1970` and `uptime` is `systemUptime`, both in seconds.
- **The `pixel` AVD** (`~/.android/avd/pixel.avd/config.ini`): `hw.device.name=pixel_8`, `hw.lcd.width=1080`, `hw.lcd.height=2400`, `hw.lcd.density=420`, `hw.lcd.vsync=60`, `hw.mainKeys=no` (no hardware keys, so a software navigation bar; this does **not** say gesture or three-button, which Step 0 reads with `settings get secure navigation_mode`), `hw.gpu.mode=host`, image `system-images/android-36/google_apis/arm64-v8a/`. The window is 411.43 × 914.29 dp at density 2.625, so the profile is **411 × 914 dp**.
- **Host:** only JDK 17 (Zulu 17.0.21) is installed; platforms `android-36`, `android-37.0`; one system image (`android-36`).
- **0a** puts `@Smoke` in `core/testing/src/main/kotlin/net/chronoflow/core/testing/tiers/Smoke.kt` (`@Target(CLASS, FUNCTION)`), and the Makefile's `SMOKE := net.chronoflow.core.testing.tiers.Smoke`. `run_tier.py` already writes `build/test-results/<tier>/timings.json` from JUnit XML `<testcase>` times. `Scripts/test_scripts.sh` runs `python3 -I -m unittest discover -s Scripts/tests -p 'test_*.py'`. The JVM convention uses `useJUnit()`. Konsist rule 2 lists the JVM modules `core/{model,plan,sync,reminders,wizard,network,data-mock,testing}`.
- **0a (brief as of 2026-11-16 07:34)** also: `TestLauncher.kt` sets the JDK 21 launcher and `maxParallelForks = cores / 2` (you override Android unit-test tasks to 1, §5); `MainActivity.onCreate` registers a one-shot `registerFrameCommitCallback` that calls `ChronoFlowApp.launchPath.end()` (keep it, T6); `:core:data-mock` is `debugImplementation` only and "0e adds the perf configurations" (T15); `ReleaseAppIdentityTest` already lives in `app/src/testRelease` (leave it; `ReleaseReadinessTest` is a separate class); `chronoflow.roborazzi` exists but is applied nowhere (T7).
- **0d** generates a wire enum `Language` (with `Unknown(raw)`) in `:core:model` contracts, which 1a uses. Do not add a second type named `Language`.
- **No board** among the 338 is a test, debug or device board.

### 2.4 Research quotes this brief relies on

- `13 §4.4`: "Production never reads this file. Production reads `WindowInsets.safeDrawing` once at the root into an app `LocalSafeInsets`, and Robolectric tests provide the fixture insets through that same local. … It avoids the iOS I9 trap of insets simulated outside the view."
- `13 §4.6`: "A JUnit4 `Timeout` rule runs the test on another thread, which breaks Robolectric's main looper, so **don't** use it in Robolectric tests."
- `13 §5.2.3`: "`createEmptyComposeRule()`, so no Activity launches before seeding (`createAndroidComposeRule<MainActivity>()` would launch first)."
- `13 §6`: "Default tier = reference-free render guards … Reference comparisons are opt-in … File names come from `<Screen>_<state>_<profile>_<lang>_<theme>`, **never** from the executing device. … Never compare a Roborazzi reference with an emulator screenshot."
- `14 §5.2`: "Coerce the configuration locale to the resolved app language (`en` or `nl`, keeping the phone's region when it is one of ours) … Then `stringResource`, plurals, `NumberFormat` and Material date/time pickers all format in en or nl."
- `14 §5.3`: "apply the stored choice through this policy on every API level, and use `LocaleManager` only on 33+, which removes the AppCompat dependency. … If the override misbehaves, fall back to capping font scale through the Compose root `LocalDensity`."
- `14 §3.3`: `configChanges` holds `orientation|screenSize|screenLayout|smallestScreenSize|density|keyboard|keyboardHidden|navigation|uiMode`; "**Not listed: `locale` and `fontScale`.** These recreate the activity, so `attachBaseContext` reapplies the configuration policy."
- `11 §7`: "Macrobenchmark refuses emulators unless `androidx.benchmark.suppressErrors=EMULATOR`; emulator numbers are regression signals, not device claims."

---

## 3. Out of scope (owner in brackets)

- Account, gate and route seeds (`CF_AUTH_TEST_SIGNED_IN`, `CF_PLAN_TEST_ROUTE`, …), `CF_MOCK_DELAY`, steps 1, 2 and 4–7 of `13 §5.2`, `ReseedEqualsColdSeedTest` [4c, 4e]. The mock data source and the MockWebServer harness [4b, 4c].
- Consumers of the seeds: what `CF_UI_TEST_PROFILE=screen` skips, `cf.reduceMotion` → `LocalReduceMotion`, `cf.freezeAt`/`cf.readyAt`/`cf.mainThreadStall` [2, 3a, 3b]; `cf.trip-page`, `cf.timeline-fixtures` and the plan-clock freeze [1d]. This phase parses, validates and exposes them only.
- Theme in the UI and DataStore preferences [2, 4a, 10b]; the language settings screen [10a]; splash appearance capture, `enableEdgeToEdge`, `installSplashScreen` [3a].
- `TimelineProbe`, `TimelineBenchmark`, baseline profiles, JankStats [1d, 1e]. Warm-start and launch-jank benchmarks [3a, 3b, 13]. `make perf-timeline` and `make baseline-profile` stay 0a stubs [1e].
- T2 AVDs and `make matrix` (on request only); accessibility-check audits; token-pair contrast tests [2].
- `make captures DEVICE=1` and every emulator capture [2, Task 18]. You leave a stub (T9).
- The plan's "Release readiness: … the mock never seeds" assertion: no mock exists yet [4e, `releaseIgnoresSeedExtras`]. You prove only that `:core:data-mock` is off the release classpath (T15).
- Any screen. If you feel a screen is needed to prove the harness, use the harness probe composables in test sources instead.

---

## 4. Modules and packages

| Module | Kind (one convention plugin) | Package | This phase adds |
|---|---|---|---|
| `:core:testing` (exists) | JVM, `chronoflow.jvm.library` | `net.chronoflow.core.testing`, `…testing.tiers` | Tier annotations next to `Smoke.kt`; `DeviceProfiles`; `profiles.json` |
| `:core:testing-junit` (**new**, OQ-0e-1) | JVM, `chronoflow.jvm.library` | `net.chronoflow.core.testing.junit` | JUnit Platform tier filter and timing listener, registered through `META-INF/services` |
| `:core:testing-compose` (**new**, OQ-0e-1) | Android, `chronoflow.android.library.compose`; namespace `net.chronoflow.core.testing.compose` | same | `ProfileRule`, `ProfileHost`, `Guards`, `BoardCapture` |
| `:core:model` (exists) | JVM | `net.chronoflow.core.model.config` (new package) | `AppLanguage`, `LanguageChoice`, `ThemeChoice`, `ConfigurationRules`, `DeviceConfig`. Do not touch `core/model/build.gradle.kts` or the contracts packages (0d edits them in parallel) |
| `:core:ui` (exists) | Android | `net.chronoflow.core.ui` | `LocalSafeInsets`, the `Ellipsizable` semantics key |
| `:app` | Android app | main `net.chronoflow.app`, `…app.config`; debug `net.chronoflow.debug`; perf `net.chronoflow.app.perf` | `ConfigurationPolicy`, `DeviceConfigFile`, root wiring; seeds in `src/debug`; a perf marker in `src/perf` |
| `:benchmark` (exists) | `chronoflow.android.test` | `net.chronoflow.benchmark` | `StartupBenchmark` |

Path shorthand: `core/testing/…/X.kt` means `core/testing/src/main/kotlin/net/chronoflow/core/testing/X.kt`.

- Add both new modules to `settings.gradle.kts`, each on its own line (0b–0d edit the same file).
- Add `core:testing-junit` to the Makefile's `JVM_MODULES` and to Konsist rule 2's JVM module list (no `android.`/`androidx.compose.` imports), in the commit that creates it.
- Every module namespace is distinct (`android.uniquePackageNames`); `native` appears in no namespace or package.
- 0a's `app/build.gradle.kts` implements "every `:core:*` module except `:core:data-mock` and `:core:testing`". Do **not** add the two new modules to that list: like `:core:testing`, they are never a main dependency. `:core:testing-compose` is only ever `testImplementation`, and `:core:testing-junit` only `testRuntimeOnly` (from the convention, §5).

---

## 5. How tiers work [R]

All JVM and Robolectric tests run on the **JUnit Platform** with the **Vintage** engine (JUnit 4 tests unchanged). One `PostDiscoveryFilter` selects classes by their tier annotation, and one `TestExecutionListener` records timings with each test's kind and `@PerfBudget` (OQ-0e-2).

**Tier property.** `-Pcf.tier=<name>`, read with `providers.gradleProperty("cf.tier").orElse("quick")` and passed to the test JVM through a `CommandLineArgumentProvider` whose `@get:Input` is that provider (keeps the configuration cache lazy).

| `cf.tier` | Runs | Used by |
|---|---|---|
| `quick` (default) | plain JVM tests (no tier annotation) and `@Logic` | `make quick`, `:app:testReleaseUnitTest` |
| `layout` | `@Layout` only | `make check` |
| `test` | `quick` + `layout` | `make test` |
| `snapshot` | `@Snapshot` only | `make snapshots` |
| `capture` | `@Capture` only | `make captures` |
| `timing` | `@TimingGate` only (plain JVM wall-clock budget tests) | `make timing` (the serial `timingTest` tasks; the last step of `make quick` and `make test`) |
| `targeted` | everything; `--tests` narrows it | `make targeted` |

**Convention changes** (in the shared `TestLauncher.kt` helper that 0a applies to every module with tests; not in module build files):
- `tasks.withType<Test>().configureEach`:
  - `useJUnitPlatform()` (replaces 0a's `useJUnit()`);
  - `failOnNoDiscoveredTests.set(false)` [verify the property name in Gradle 9.9.1]: Gradle 9 fails a test task with test sources but no discovered tests, which every tier filter produces somewhere (OQ-0e-17);
  - system properties through the argument provider: `cf.tier`, `cf.timingsDir` = `<module>/build/cf-timings/<taskName>`, `cf.capturesDir` = `<root>/build/captures`, `cf.captureOnly` (`providers.gradleProperty("cf.captureOnly").orElse("")`), `robolectric.graphicsMode=NATIVE`, `robolectric.pixelCopyRenderMode=hardware`, `robolectric.alwaysIncludeVariantMarkersInTestName=true` (method names end in `[<sdk>]`);
  - `outputs.dir(<module>/build/cf-timings/<taskName>)` and a `doFirst` that deletes that directory (capture the `Provider<Directory>`, never `project`), so a cached or up-to-date task keeps its matching timings;
  - `timeout.set(Duration.ofMinutes(10))`;
  - JUnit XML stays at Gradle's default location (`<module>/build/test-results/<taskName>/`), which 0a's run-tier globs read.
- Android unit-test tasks (`test*UnitTest`): `maxParallelForks = 1` (one Robolectric JVM per module; modules still run in parallel).
- JVM modules (`kotlin("jvm")`) also get a `timingTest` Test task (OQ-0e-22): the `test` source set's classes and runtime classpath, the same launcher configuration, `cf.tier` fixed to `timing` whatever `-Pcf.tier` says, `maxParallelForks = 1`, and `mustRunAfter(tasks.withType<Test>().matching { it.name != "timingTest" })`. Its XML lands in `build/test-results/timingTest/` and its timings in `build/cf-timings/timingTest/`. It is wired to neither `test` nor `check`: `make timing` runs it, and `make targeted` still reaches these classes through `test` with `cf.tier=targeted`.
- Dependencies for every module that has tests: `testRuntimeOnly(project(":core:testing-junit"))` (skipped in `:core:testing-junit` itself, which must not depend on itself), `testRuntimeOnly(org.junit.vintage:junit-vintage-engine)` and `testRuntimeOnly(org.junit.platform:junit-platform-launcher)` (in every module **including** `:core:testing-junit`: without Vintage its own JUnit 4 tests are not discovered, and `failOnNoDiscoveredTests = false` would hide that), all from `platform(org.junit:junit-bom)`. Pin the newest **5.14.x** patch on Maven Central that is at least two weeks old; never 6.x (it deprecates Vintage). Add `junitBom`, `junit-vintage-engine`, `junit-platform-launcher` to `gradle/libs.versions.toml`.
- `testImplementation(project(":core:testing"))` where tier annotations are used.
- The application convention already sets `testInstrumentationRunnerArguments["timeout_msec"] = "60000"` (0a); keep it.

**Fallback** if ServiceLoader registration of the filter or listener does not work under AGP unit-test tasks [verify]: JUnit 4 `@Category(<TierAnnotationClass>::class)` on every tiered class, Gradle `useJUnitPlatform { includeTags/excludeTags(<fqcn>) }` (Vintage maps categories to tags), and the listener kept for timings only. Record the outcome as OQ-0e-2. Do not invent a third mechanism.

**Budgets** (AGENTS "Tests" 5): JVM 1,000 ms; Robolectric 5,000 ms, except the **first test per Robolectric SDK in a JVM** (sandbox start), which gets 20,000 ms (OQ-0e-3); emulator 10,000 ms; `@PerfBudget` and `@TimingGate` exempt.

---

## 6. Tasks

**Step 0 (no commit): measure the `pixel` insets first**, because T5 needs them and T7 needs T5. Run `make emu`, install and launch the 0a debug app (`adb shell am start -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity`), then `adb shell dumpsys window | grep -i -E "insetssource|statusBars|navigationBars|displayCutout|mandatorySystemGestures"` and `adb shell wm size; adb shell wm density; adb shell settings get secure navigation_mode` (`0` three-button, `1` two-button, `2` gesture; pixel8's `"navigation"` value in T5 follows this output, not an assumption). Keep the verbatim output for T5. If your sandbox cannot run `adb`, ask the orchestrator for exactly these commands' output and continue with T1–T4 meanwhile.

Each task below is one commit: Conventional Commits, lower-case imperative subject, **no trailer**. Tick plan.md boxes only as §7 says. Never commit a measured value you did not measure.

### T1. Tier annotations and the tier registry
**Commit:** `test(konsist): add tier annotations and the tier registry`

- In `core/testing/…/tiers/`, next to 0a's `Smoke.kt` (leave it as it is), one file each: `Logic.kt`, `Layout.kt`, `Snapshot.kt`, `Capture.kt`, `Perf.kt`, `PerfBudget.kt`, `TimingGate.kt`. All `@Retention(AnnotationRetention.RUNTIME) @MustBeDocumented`. The five tiers are `@Target(AnnotationTarget.CLASS)`. `@PerfBudget` is `@Target(CLASS, FUNCTION)`: not a tier, it exempts a test from the duration budget. `@TimingGate` is also `@Target(AnnotationTarget.CLASS)`: a class tier for plain JVM wall-clock budget tests (1b, 1c), which run only in the serial `timing` tier and are exempt from the duration budget (OQ-0e-22). KDoc one line each, naming the tier from §5.
- `konsist/src/test/kotlin/net/chronoflow/konsist/TierRegistryTest.kt`, in 0a's `KonsistRulesTest` pattern (a passing run on the project scope, plus a `…RejectsPlantedViolation` run per rule):
  1. **Robolectric class:** a class in a `src/test*/` source set annotated with `@RunWith(AndroidJUnit4::class)`, `@RunWith(RobolectricTestRunner::class)`, `@RunWith(ParameterizedRobolectricTestRunner::class)`, `@Config(…)` or `@GraphicsMode(…)`. It has exactly one of `@Logic`, `@Layout`, `@Snapshot`, `@Capture`, and no `@TimingGate`.
  2. **Emulator class:** a class with a `@Test` function in `src/androidTest*/`. It has exactly one of `@Smoke`, `@Capture` at class level.
  3. **Benchmark class:** a class with a `@Test` function in `benchmark/src/main/`. It has `@Perf` and no other tier. 0a's `ProjectPaths.moduleOf` matches only `app`, `feature/*`, `core/*` and `tools/*`, so it returns `null` for `benchmark/…` and 0a's "only files with a module" filter would skip this rule silently: select these files by the path segment `/benchmark/src/main/` instead (the planted file's path ends the same way).
  4. **Plain JVM test class** (in `src/test*/`, not Robolectric): no tier annotation except `@TimingGate`. `@PerfBudget` is allowed anywhere tests are.
  5. Every `runTest(` call in test sources passes `timeout = 60.seconds`.
  6. No test source imports `org.junit.rules.Timeout` except under `src/androidTest*/` (AGENTS Traps).
- Planted violations under `konsist/src/test/resources/violations/tier/` (excluded from compilation, Spotless and the project scope, as 0a does). The rules key on source sets, so each file sits under a module path that `ProjectPaths` resolves, exactly as 0a's planted files do:
  - rule 1: `app/src/test/kotlin/net/chronoflow/app/UntaggedRobolectricTest.kt`, `app/src/test/kotlin/net/chronoflow/app/TwoTierRobolectricTest.kt`;
  - rule 2: `app/src/androidTest/kotlin/net/chronoflow/app/UntaggedEmulatorTest.kt`;
  - rule 3: `benchmark/src/main/kotlin/net/chronoflow/benchmark/UntaggedBenchmark.kt`;
  - rule 4: `core/plan/src/test/kotlin/net/chronoflow/core/plan/LayoutOnJvmTest.kt`;
  - rule 5: `core/sync/src/test/kotlin/net/chronoflow/core/sync/RunTestWithoutTimeoutTest.kt`;
  - rule 6: `app/src/test/kotlin/net/chronoflow/app/TimeoutRuleInRobolectricTest.kt`.

  Each `…RejectsPlantedViolation` test runs its rule on **that one file** (`Konsist.scopeFromFile(<path>)`), so every file proves its own rule (rule 1 has two such tests). Each planted file breaks only its own rule.

Edge cases: abstract bases without `@Test` (e.g. `ScreenTest`) need no annotation. `@Smoke` may also sit on a function (0a's target); the registry counts class-level annotations only.

Tests: `TierRegistryTest` passes on the tree (including 0a's `AppLaunchSmokeTest`) and each planted file fails its rule. JVM, < 1 s each.

### T2. Tier selection and the timing listener
**Commit:** `build(testing): select test tiers on the junit platform`

New module `:core:testing-junit`, package `net.chronoflow.core.testing.junit`, depending only on `compileOnly(junit-platform-launcher)` (+ the BOM):
- `TierName.kt`: `enum class TierName { Quick, Layout, Test, Snapshot, Capture, Timing, Targeted }` with `companion fun parse(raw: String?): TierName` (case-insensitive; null or unknown → `Quick`, and print `cf.tier=<raw> unknown; using quick` to stderr), and `fun includes(classTier: String?): Boolean` where `classTier` is the simple name of the class's tier annotation (`"Logic"`, `"Layout"`, `"Snapshot"`, `"Capture"`, `"Smoke"`, `"Perf"`, `"TimingGate"`, or `null`) per the §5 table. `Smoke` and `Perf` classes never run in a JVM tier except `targeted`. `TimingGate` classes run only in `timing` and `targeted`, and `timing` runs nothing else.
- `TierDiscoveryFilter : PostDiscoveryFilter`: for each descriptor, walk up to the nearest `ClassSource`, load the class with `Class.forName(name, false, Thread.currentThread().contextClassLoader)`, find its tier annotation by **fully qualified annotation name** in `net.chronoflow.core.testing.tiers` (no compile dependency on `:core:testing`), and return `included()` or `excluded("cf.tier=<tier> excludes @<Tier>")`. Descriptors without a class source are included.
- `TestTimingListener : TestExecutionListener`, per test (`isTest`), timed with `System.nanoTime()`:
  - `kind`: `robolectric` if the class has a Robolectric tier annotation (`Logic`, `Layout`, `Snapshot`, `Capture`) or a `@RunWith` whose runner class name contains `Robolectric` or equals `androidx.test.ext.junit.runners.AndroidJUnit4`, or `@Config`/`@GraphicsMode` (matched by annotation class name); else `jvm`;
  - `sdk`: the `[NN]` suffix of the method's display name, or `"default"`;
  - `budgetMs`: `jvm` 1,000; `robolectric` 5,000, or 20,000 with `status = "warmup"` for the first robolectric test of each `sdk` in this JVM; `null` with `status = "exempt"` when `@PerfBudget` is on the method or class, or `@TimingGate` is on the class;
  - `status`: `ok`, `over`, `warmup` (within 20 s) or `exempt`; a warm-up test over 20 s is `over`;
  - on `testPlanExecutionFinished`, write `<cf.timingsDir>/<pid>.json` (`ProcessHandle.current().pid()`), creating the directory: `{"tier": "<tier>", "tests": [{"class", "method", "kind", "sdk", "ms", "budgetMs", "status"}]}`. No `cf.timingsDir` → write nothing. Catch and print `IOException`; the listener never throws.
- Keep the classification pure: `internal fun classify(kind: String, sdk: String, ms: Long, perfBudget: Boolean, firstForSdk: Boolean): Pair<Long?, String>`.
- `src/main/resources/META-INF/services/org.junit.platform.launcher.PostDiscoveryFilter` and `…/org.junit.platform.launcher.TestExecutionListener`.
- The §5 convention changes in build-logic, including the JUnit dependencies and catalog entries.

Tests (`core/testing-junit/src/test/…`, JVM; plain JUnit 4 under Vintage):
- `TierNameTest`: the full matrix of 7 tier names × 8 class tiers (the 7 annotations + `null`), plus `parse(null)`, `parse("LAYOUT")`, `parse("bogus")`.
- `TimingClassifyTest`: jvm 1,200 ms → `(1000, over)`; robolectric 4,900 ms → `(5000, ok)`; first robolectric on sdk `36` at 12,000 → `(20000, warmup)`; second on `36` at 6,000 → `over`; first on `29` at 9,000 → `warmup`; `@PerfBudget` 6,000 → `(null, exempt)`; warm-up at 21,000 → `over`.

Verify once [verify] and record under OQ-0e-2 in `docs/tooling.md`: after T7 lands, `./gradlew :core:testing-compose:testDebugUnitTest -Pcf.tier=quick` runs no `@Layout` class (read the XML), and `core/testing-compose/build/cf-timings/testDebugUnitTest/*.json` exists.

### T3. Budget enforcement in the tier runner, and tiered Make targets
**Commit:** `build: enforce per-test budgets in the tier runner`

Extend 0a's `Scripts/run_tier.py`; do not write a second `timings.json`.
- New repeatable option `--timings <glob>` (listener files). Globs always name the Gradle task directory the run executes, for example `'*/build/cf-timings/testDebugUnitTest/*.json'` and `'*/*/build/cf-timings/testDebugUnitTest/*.json'`, never `cf-timings/**`. A `**` glob would also pick up stale files from other tiers' tasks (`test`, `testReleaseUnitTest`), double-count them, and hide a task that wrote no timing file. New option `--emulator` (tests from the XML are emulator tests with a 10,000 ms budget).
- `timings.json` keeps 0a's fields and adds `"budgets": {"jvm": 1000, "robolectric": 5000, "robolectricWarmup": 20000, "emulator": 10000}`, `"budgetChecks": [ …listener entries…, or for `--emulator` one entry per XML testcase with kind `emulator` ]`, `"overBudget": <n>` and `"slowest": [top 10 entries by ms]`.
- After collecting results: print `run-tier: over budget: <kind> <class>#<method> <ms> ms > <budget> ms` per over entry. If the command succeeded and `overBudget > 0`, exit **3**. Keep 0a's codes: the command's own code, `2` for no result files, `124` for a cap. With `--timings` given and no listener file found while XML exists, print `run-tier: <tier> wrote no timing files` and exit 2 (missing evidence).
- `Makefile` (your edits on their own lines; 0b–0d edit the file in parallel):
  - `quick`: add `testDebugUnitTest -Pcf.tier=quick` to the existing Gradle call, plus `--results '*/build/test-results/testDebugUnitTest/*.xml' --results '*/*/build/test-results/testDebugUnitTest/*.xml'` and four `--timings` globs: `'*/build/cf-timings/test/*.json'`, `'*/*/build/cf-timings/test/*.json'`, `'*/build/cf-timings/testDebugUnitTest/*.json'`, `'*/*/build/cf-timings/testDebugUnitTest/*.json'`.
  - `check`: the 0a `check` run keeps `:app:lintDebug :app:testReleaseUnitTest :app:verifyReleaseManifest [:app:analyzeReleaseR8Config]` (tier `quick`; drop 0a's `:app:testDebugUnitTest`, which quick now covers). Replace 0a's `--results 'app/build/test-results/**/*.xml'` with `--results 'app/build/test-results/testReleaseUnitTest/*.xml'` (the `**` glob would also collect the `testDebugUnitTest` XML that quick just wrote and list those classes as unbudgeted), and give it `--timings 'app/build/cf-timings/testReleaseUnitTest/*.json'`. Add a separate `$(RUN_TIER) layout … -- $(GRADLE) testDebugUnitTest -Pcf.tier=layout` with the two `testDebugUnitTest` result globs and the two `testDebugUnitTest` timing globs. The smoke run gains `--emulator` (it keeps 0a's `--device`).
  - `test`: `$(GRADLE) $(JVM_MODULES:%=:%:test) testDebugUnitTest :app:testReleaseUnitTest -Pcf.tier=test`, not a bare `test`. With `android.onlyEnableUnitTestForTheTestedBuildType=false`, a bare `test` also runs every library's `testReleaseUnitTest` and the perf variants' unit tests. There Robolectric Compose tests have no `ui-test-manifest`, which is `debugImplementation` only, so `createComposeRule()` cannot resolve its activity [verify once; record in `docs/tooling.md`]. This keeps AGENTS "every JVM and Robolectric test".
  - `targeted`: `Scripts/targeted.py` appends `-Pcf.tier=targeted` to Gradle calls and passes `--timings` for the task it runs (`<module dir>/build/cf-timings/<test|testDebugUnitTest>/*.json`, JVM/Robolectric) or `--emulator` (instrumented) to run-tier. Update `Scripts/tests/test_targeted.py` expectations.
  - `timing` (new target, OQ-0e-22): the header gains `TIMING_MODULES :=` (empty; 1b adds `core:plan`). With modules, it runs `$(RUN_TIER) timing --results '*/build/test-results/timingTest/*.xml' --results '*/*/build/test-results/timingTest/*.xml' --timings '*/build/cf-timings/timingTest/*.json' --timings '*/*/build/cf-timings/timingTest/*.json' -- $(GRADLE) $(TIMING_MODULES:%=:%:timingTest) --max-workers=1`; with none, it prints `timing: no timing tests yet (1b adds them)` and exits 0. `quick` and `test` run `$(MAKE) timing` as their last step.
- `Scripts/tests/test_run_tier.py`, new cases (fixtures written into the temp root per test):
  - `test_listener_over_budget_fails`: XML passes, a listener file has a jvm test at 1,200 ms → exit 3, line names the test;
  - `test_exempt_and_warmup_pass` → exit 0, `overBudget == 0`;
  - `test_emulator_xml_over_budget_fails`: `--emulator`, `time="10.5"` → exit 3;
  - `test_missing_timing_files_fail`: `--timings` given, XML present, no listener file → exit 2;
  - `test_xml_without_listener_entry_is_not_budgeted`: XML for a class with no listener entry (0a's `:build-logic:convention:test` runs outside the conventions, so it writes XML only) → not in `budgetChecks`, exit 0, and `timings.json` lists it under `"unbudgeted": ["<class>#<method>"]` so the gap stays visible;
  - `test_slowest_lists_top_ten`.
- **First enforced run** (OQ-0e-21): run `make quick` once after this task and read `overBudget`. 0a's `:konsist` classes (`KonsistRulesTest`, `ModuleGraphTest`, and T1's `TierRegistryTest`) parse the whole project once per class (`by lazy`), and that first test may pass 1 s. If, and only if, a `:konsist` test is over budget, put class-level `@PerfBudget` on that Konsist class (`:konsist` gets `testImplementation(project(":core:testing"))`) and record the measured ms in `docs/tooling.md`. Any other over-budget test is a test to fix, not to exempt.

**Plan tick:** "Tier registry".

### T4. Configuration rules and the device-config codec (JVM)
**Commit:** `feat(model): add app language, theme and configuration rules`

Files in `core/model/src/main/kotlin/net/chronoflow/core/model/config/` (pure Kotlin; `java.util.Locale` allowed):
- `AppLanguage.kt`: `enum class AppLanguage(val tag: String) { En("en"), Nl("nl") }`. It is the resolved UI language; 0d's generated `Language` stays the wire enum (OQ-0e-10).
- `LanguageChoice.kt`: `enum class LanguageChoice(val raw: String) { System("system"), En("en"), Nl("nl"); companion object { fun fromRaw(raw: String?): LanguageChoice? } }` (iOS `SettingsDevicePreferences.LanguageChoice`). Exact lower-case match; anything else → `null`.
- `ThemeChoice.kt`: `System("system"), Light("light"), Dark("dark")`, same `fromRaw` (iOS `SettingsDevicePreferences.Theme`).
- `ConfigurationRules.kt` (`object`):
  - `const val MAX_FONT_SCALE = 1.3f`;
  - `fun capFontScale(system: Float): Float` = `min(system, 1.3f)`; non-finite or ≤ 0 → `1.0f`;
  - `fun resolveLanguage(choice: LanguageChoice, systemLocale: Locale): AppLanguage`: `En`/`Nl` as chosen; `System` → `Nl` when `systemLocale.language == "nl"`, else `En`;
  - `fun resolveLocale(choice: LanguageChoice, systemLocale: Locale): Locale`: the language from `resolveLanguage`; keep the phone's region only when the phone's language equals the resolved one; drop script, variant and extensions (OQ-0e-4).
- `DeviceConfig.kt`: `data class DeviceConfig(val language: LanguageChoice = LanguageChoice.System, val theme: ThemeChoice = ThemeChoice.System)`, `fun encode(): ByteArray`, `companion fun decode(bytes: ByteArray?): DeviceConfig` (OQ-0e-5):
  - format: UTF-8, LF-separated `key=value`; `encode()` writes exactly `language=<raw>\ntheme=<raw>\n` (≤ 30 bytes);
  - `decode`: `null`, more than 64 bytes, or malformed UTF-8 (strict `CharsetDecoder` with `REPORT`) → defaults; unknown keys, blank or malformed lines and invalid values are skipped; a trailing `\r` is trimmed; the last valid value of a key wins; it never throws.

Tests (`core/model/src/test/kotlin/net/chronoflow/core/model/config/`, JVM):
- `ConfigurationRulesTest`:
  - `capFontScale`: 0.85 → 0.85; 1.0 → 1.0; 1.3 → 1.3; 2.0 → 1.3; `NaN` → 1.0; `0f` → 1.0; `-1f` → 1.0;
  - `resolveLocale(System, …)`: `ar-EG` → `en`; `de-DE` → `en`; `nl-BE` → `nl-BE`; `nl-NL` → `nl-NL`; `en-GB` → `en-GB`; `en-US-u-nu-arab` → `en-US`; `nl` → `nl`; `Locale.ROOT` → `en`;
  - explicit: `(Nl, en-US)` → `nl`; `(En, nl-NL)` → `en`; `(Nl, nl-BE)` → `nl-BE`;
  - Latin digits: `String.format(resolveLocale(System, Locale.forLanguageTag("ar-EG")), "%,d", 1234567) == "1,234,567"`.
- `DeviceConfigTest`: round trip of all 9 combinations; 64 bytes decode, 65 bytes → defaults; `"language=nl\nlanguage=xx\n"` → `Nl`; `"theme=dark"` without LF → `Dark`; `"language=nl\r\n"` → `Nl`; unknown key ignored; bytes `0xC3 0x28` → defaults; `null` → defaults.

### T5. Device profiles
**Commit:** `test(testing): add device profiles with measured pixel insets`

1. **Record Step 0** in `docs/device-matrix.md` (new): date, AVD facts (§2.3), navigation mode, the verbatim `dumpsys`/`wm` output, and per side the px value and its dp (px ÷ 2.625, two decimals). Add `systemGestures` left/right for reference.
2. `core/testing/src/main/resources/profiles.json`:
   ```json
   {
     "schema": 1,
     "profiles": {
       "small":            {"widthDp": 360, "heightDp": 640, "density": 2.0,   "fontScale": 1.0, "navigation": "threeButton", "insetsDp": {"top": 24, "left": 0, "bottom": 48, "right": 0}, "source": "research 13 §4.4", "purpose": "worst-case height (iOS se)"},
       "pixel8":           {"widthDp": 411, "heightDp": 914, "density": 2.625, "fontScale": 1.0, "navigation": "gesture", "insetsPx": {"top": <measured>, "left": 0, "bottom": <measured>, "right": 0}, "source": "measured pixel AVD <yyyy-mm-dd>", "purpose": "reference phone (iOS 17e)"},
       "foldInner":        {"widthDp": 841, "heightDp": 701, "density": 2.625, "fontScale": 1.0, "navigation": "gesture", "insetsFrom": "pixel8", "source": "derived from pixel8, not measured", "purpose": ">= 600 dp layout (iOS duo/wide)"},
       "landscapeSideNav": {"widthDp": 914, "heightDp": 411, "density": 2.625, "fontScale": 1.0, "navigation": "threeButton", "insetsDp": {"top": 24, "left": 0, "bottom": 0, "right": 48}, "source": "research 13 §4.4", "purpose": "asymmetric side inset (iOS duo rail)"},
       "largeFont":        {"basedOn": "pixel8", "fontScale": 2.0, "purpose": "root font-scale cap (iOS AX5 vs xxLarge)"}
     }
   }
   ```
   A left/right measured value that is not 0 is committed as measured. `insetsPx` converts to dp as px ÷ density (unrounded floats in code). `largeFont` inherits every pixel8 field except `fontScale` and `purpose`; `foldInner` copies pixel8's dp insets.
3. `core/testing/…/DeviceProfiles.kt`:
   ```kotlin
   data class SafeInsetsDp(val top: Float, val left: Float, val bottom: Float, val right: Float)
   data class DeviceProfile(
       val name: String, val widthDp: Int, val heightDp: Int, val density: Float, val fontScale: Float,
       val navigation: String, val insets: SafeInsetsDp, val source: String,
   ) {
       val densityDpi: Int get() = (density * 160).roundToInt()
       val isLandscape: Boolean get() = widthDp > heightDp
   }
   object DeviceProfiles {
       val all: List<DeviceProfile>                 // file order
       operator fun get(name: String): DeviceProfile // unknown → IllegalArgumentException naming it
       val small: DeviceProfile; val pixel8: DeviceProfile; val foldInner: DeviceProfile
       val landscapeSideNav: DeviceProfile; val largeFont: DeviceProfile
   }
   ```
   Loaded lazily from the classpath resource `/profiles.json` with `kotlinx.serialization.json.Json.parseToJsonElement` (the runtime library only; no serialization plugin). Add `implementation(libs.kotlinx.serialization.json)` to `core/testing/build.gradle.kts` on its own line, unless the stack top already has it (0d edits this file in parallel; 0a's catalog already holds the library). A missing field throws naming the profile and field.

Tests:
- `DeviceProfilesTest` (JVM, `core/testing/src/test/…`): exactly the five names in order; pixel8 is 411 × 914 at 2.625 (`densityDpi == 420`); largeFont equals pixel8 except `fontScale == 2.0f`; small insets 24/0/48/0; landscapeSideNav right 48; every width ≥ 280; `foldInner.widthDp >= 600` and `pixel8.widthDp < 600`; pixel8 `source` starts with `measured`; foldInner insets equal pixel8's.
- `PixelProfileInsetsTest` (`@Smoke`, `app/src/androidTest/kotlin/net/chronoflow/app/PixelProfileInsetsTest.kt`): `createAndroidComposeRule<ComponentActivity>()`; content reads `WindowInsets.safeDrawing` (`getTop(density)`, `getBottom(density)`, `getLeft(density, LayoutDirection.Ltr)`, `getRight(…)`) into a captured value in `SideEffect` and logs `Log.i("CfInsets", "top=… left=… bottom=… right=…")`. After `compose.waitForIdle()`, assert the four px values equal `ViewCompat.getRootWindowInsets(decorView)!!.getInsets(Type.systemBars() or Type.displayCutout())` and equal pixel8's `insetsPx`. `assumeTrue` that `Build.MODEL` starts with `sdk_gphone` and the real display is 1080 × 2400 (other devices skip). This file reads `WindowInsets.safeDrawing` and is allowed to (T6 rule exempts `src/androidTest`). `createAndroidComposeRule<ComponentActivity>()` launches `ComponentActivity` in the **app's** package, so it must be declared in the app's debug manifest: add `debugImplementation(<0a's alias for androidx.compose.ui:ui-test-manifest>)` to `app/build.gradle.kts` on its own line in this commit (0a's `:app` has no `ui-test-manifest`; the BOM from `implementation(platform(…))` versions it). T7's convention adds the same dependency to every Compose module; remove this line in T7.

### T6. `LocalSafeInsets`, the root and the insets rule
**Commit:** `feat(ui): provide safe insets and test tags at the root`

- `core/ui/…/LocalSafeInsets.kt`: `val LocalSafeInsets = staticCompositionLocalOf<WindowInsets> { error("LocalSafeInsets is provided at the root and by ProfileHost") }`.
- `core/ui/…/semantics/Ellipsizable.kt`: `val Ellipsizable = SemanticsPropertyKey<Unit>("cf.ellipsizable")` and `fun Modifier.ellipsizable(): Modifier = semantics { this[Ellipsizable] = Unit }`. Guards use it to allow an intended ellipsis.
- `MainActivity.setContent` (keep 0a's frame-commit callback):
  ```kotlin
  setContent {
      CompositionLocalProvider(
          LocalSafeInsets provides WindowInsets.safeDrawing,
          LocalLayoutDirection provides LayoutDirection.Ltr,
      ) {
          Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.testTag("app.root"))
      }
  }
  ```
  `app.root` is a harness tag, not an iOS id; later phases replace the `Box` with `CfRoot` and keep the tag and both providers. If `testTagsAsResourceId` is still opt-in in Compose 1.12.1, opt in at this call site only. No system-bar styling (3a).
- Konsist rule `insetsOnlyAtRoot` with a planted violation: `WindowInsets.safeDrawing`, `.safeContent`, `.systemBars`, `.statusBars`, `.navigationBars` and `.displayCutout` appear only in `app/src/main/…/MainActivity.kt`, `core/testing-compose/`, and `src/androidTest*/`. Screens read `LocalSafeInsets` (and pass it to Material components' `windowInsets` parameters). `WindowInsets.ime` and `.systemGestures` stay allowed.
- Konsist rule `testingNotInMain` with a planted violation: no file under `src/main/` outside `core/testing*/` and `benchmark/` imports `net.chronoflow.core.testing`. `app/src/debug/` and `app/src/perf/` may (1d uses fixtures there). `benchmark/src/main/` may (it is the benchmark module's test code and imports `@Perf`, T16); 0a's `moduleOf` returns `null` there anyway, so say it explicitly in the rule rather than relying on that. Planted file: `konsist/src/test/resources/violations/testingNotInMain/core/plan/src/main/kotlin/net/chronoflow/core/plan/UsesFixtures.kt` importing `net.chronoflow.core.testing.DeviceProfiles`.
- If 0a's Konsist package rule rejects `net.chronoflow.debug`, allow that package only under `app/src/debug/`, `app/src/testDebug/` and `app/src/androidTest/`.

Tests: the two Konsist rules and their planted files. The root is exercised by T12's smoke tests.

**Plan tick:** "Profiles".

### T7. `:core:testing-compose`: `ProfileRule` and `ProfileHost`
**Commit:** `test(testing): add the robolectric profile harness`

- New module `:core:testing-compose` (`chronoflow.android.library.compose`, namespace `net.chronoflow.core.testing.compose`).
  - 0a defined `chronoflow.roborazzi` but applies it nowhere. Apply it **from inside** `chronoflow.android.library.compose`, `chronoflow.android.application.compose` and `chronoflow.android.feature` (via `pluginManager.apply`), so each module still applies exactly one plugin in its build file. Extend it to add, as `testImplementation`: Robolectric 4.18, Roborazzi 1.79.0 (`roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule`), Compose `ui-test-junit4` (BOM 2026.09.00), `androidx.test.ext:junit` 1.3.0; `debugImplementation(ui-test-manifest)`; and `roborazzi { outputDir.set(layout.projectDirectory.dir("src/test/snapshots")) }`.
  - Remove T5's `debugImplementation(ui-test-manifest)` line from `app/build.gradle.kts`; the convention now adds it.
  - `:core:testing-compose` exposes those libraries plus `project(":core:testing")`, `project(":core:ui")` and `project(":core:model")` as `api` from `src/main`, because its main code is test helpers. `:core:model` is needed for `AppLanguage` and `ConfigurationRules` (`ProfileRule`); 0a's `:core:ui` skeleton has no dependencies, so it does not bring `:core:model`. Consumers add `testImplementation(project(":core:testing-compose"))`. If Lint flags test APIs in its main source, disable only those checks in this module's `lint.xml` with a one-line reason.
- `ProfileRule.kt`:
  ```kotlin
  class ProfileRule(
      val profile: DeviceProfile,
      val language: AppLanguage = AppLanguage.En,
      val dark: Boolean = false,
  ) : TestRule
  ```
  Before the inner statement:
  - `RuntimeEnvironment.setQualifiers(qualifiers(profile, language, dark))`, with `"<lang>-r<REGION>-w<W>dp-h<H>dp-<port|land>-<night|notnight>-<dpi>dpi"` (en → `en-rUS`, nl → `nl-rNL`), e.g. `nl-rNL-w360dp-h640dp-port-notnight-320dpi`. Write it as an `internal` top-level function `profileQualifiers(profile, language, dark): String` in `ProfileRule.kt`. It is pure Kotlin, with no Android or Robolectric types, so its test runs on the plain JVM.
  - `RuntimeEnvironment.setFontScale(ConfigurationRules.capFontScale(profile.fontScale))`: the production cap, so `largeFont` renders at 1.3.
  - KDoc: use `@get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(…)).around(composeRule)`; the profile must apply before the compose rule launches its activity.
- `ProfileHost.kt`: `@Composable fun ProfileHost(profile: DeviceProfile, content: @Composable () -> Unit)` provides `LocalSafeInsets provides remember(profile) { WindowInsets(left = l.dp, top = t.dp, right = r.dp, bottom = b.dp) }` and `LocalLayoutDirection provides LayoutDirection.Ltr`.
- Never a JUnit 4 `Timeout` rule in Robolectric tests (T1 rule 6).

Tests (`core/testing-compose/src/test/…`):
- `ProfileQualifiersTest`, a **plain JVM** test (no runner, no tier annotation; AGENTS "Tests" 1, cheapest layer): `profileQualifiers` for all five profiles × en/nl × light/dark gives exactly the format above. Assert at least `small`/nl/light = `nl-rNL-w360dp-h640dp-port-notnight-320dpi`, `pixel8`/en/dark = `en-rUS-w411dp-h914dp-port-night-420dpi` and `landscapeSideNav`/en/light = `en-rUS-w914dp-h411dp-land-notnight-420dpi`.

Each of the next classes is `@RunWith(ParameterizedRobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(sdk = [36]) @Logic`. The profile is a constructor parameter and the rule a field (`@get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles[name])).around(composeRule)`), because a class-level rule cannot change the profile between test methods, and `RuntimeEnvironment.setQualifiers` after the compose rule has launched its activity does not re-lay it out. Each parameter set is its own test case (AGENTS "Tests" 5):
- `ProfileHostTest`, cases `small` and `landscapeSideNav`. Content inside `ProfileHost(profile)` reads `LocalSafeInsets.current` and `LocalDensity.current` into captured values. Expected px (top/left/bottom/right): `small` 48/0/96/0 (24 dp and 48 dp at 2.0); `landscapeSideNav` 63/0/0/126 (24 dp and 48 dp at 2.625, `getLeft`/`getRight` with `LayoutDirection.Ltr`). The root node's size is within ± 1 px of `widthDp × density` by `heightDp × density` (`small`: 720 × 1280).
- `LargeFontEqualsCapTest`, cases `largeFont` (expected scale 1.3, other 1.0) and `pixel8` (expected 1.0, other 1.3). One composition holds three `Text("Aa Bb 12:00", fontSize = 16.sp)` tagged `cap.subject` (ambient density), `cap.expected` (inside `CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, expectedScale))`) and `cap.other` (same with the other scale). Assert `LocalDensity.current.fontScale == expectedScale`, `cap.subject` height equals `cap.expected` height, and differs from `cap.other` (positive control). This proves the harness renders `largeFont` at the 1.3 cap; the production cap itself is `ConfigurationPolicyTest` (T10).

### T8. Reference-free guards
**Commit:** `test(testing): add reference-free layout guards`

`core/testing-compose/…/Guards.kt`, `object Guards`. Each function takes `rule: SemanticsNodeInteractionsProvider` and throws `AssertionError` naming the node's test tag (or text) and both rectangles. Tolerance 0.5 px. Guards enumerate nodes with specific matchers (`onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()`); that is their job, and screen tests never do it (AGENTS "Tests" 4). Never `printToLog`.

| Function | Asserts |
|---|---|
| `assertTextFits(rule, profile)` | Every node with `SemanticsProperties.Text` lies inside the window `[0, W] × [0, H]` px horizontally; vertically after `performScrollTo()` when an ancestor has `VerticalScrollAxisRange`. Through `SemanticsActions.GetTextLayoutResult`: `hasVisualOverflow` is false and no line `isLineEllipsized`, unless the node or an ancestor has `Ellipsizable`. |
| `assertTouchTargets(rule, minSize = 48.dp, exemptTags = emptySet())` | Every node (merged tree) with `OnClick`, `ToggleableState` or `SetText` has a **layout size** (`node.size`, in px, compared with `minSize` in px) of at least 48 × 48 dp. Never use `touchBoundsInRoot`: Compose may extend it to the minimum touch target for any small pointer-input node, so the planted 40 dp box would pass and the guard could not fail. Material components meet 48 dp in layout through `minimumInteractiveComponentSize()`. |
| `assertInteractiveInSafeArea(rule, profile, exemptTags = emptySet())` | Those nodes' `boundsInRoot` lie inside the window minus the profile insets (after `performScrollTo()` when scrollable). |
| `assertNoInteractiveOverlap(rule, exemptTags = emptySet())` | No two interactive nodes (merged tree) intersect by more than 0.5 px in both axes, unless one contains the other in the semantics tree. A node is exempt when it or an ancestor carries an exempt tag. |
| `assertHeadings(rule, vararg headingTags: String)` | Each tag exists exactly once and has `SemanticsProperties.Heading`. At least one tag is required. |
| `assertInk(rule, profile, minInkFraction = 0.005f)` | `onRoot().captureToImage()` is `round(W × density) × round(H × density)` px (± 1). Background = the most frequent ARGB value. The share of pixels with any channel more than 16 away from it is ≥ `minInkFraction`. |
| `assertAll(rule, profile, headingTags: Set<String>, exemptTags: Set<String> = emptySet())` | All of the above, in this order. |

KDoc states: lazy lists expose only composed items, so guards see composed nodes only; text in a horizontally scrolling row is checked after `performScrollTo()`.

Tests: `GuardsSelfTest` (`@Logic`, Robolectric, `small` through `ProfileRule` + `ProfileHost`; literals are allowed in tests). One good layout (a heading tagged `guard.good.title`, a 48 dp button inside the safe area, a short text, on a white background, laid out inside `LocalSafeInsets`) passes `assertAll(rule, small, headingTags = setOf("guard.good.title"))`. Each planted layout is **the good layout plus exactly one defect**. It fails **only** its guard (`assertFailsWith<AssertionError>`) and passes each other guard called on its own (with `headingTags = {"guard.good.title"}`):
1. `Text(softWrap = false)` with 600 dp of content → `assertTextFits`;
2. `Text(maxLines = 1, overflow = TextOverflow.Ellipsis)` too long → fails; the same with `Modifier.ellipsizable()` → passes;
3. a 40 × 40 dp `Box(Modifier.clickable {})` without `minimumInteractiveComponentSize()` → `assertTouchTargets`;
4. a 48 dp button at y = 0, under the 24 dp top inset → `assertInteractiveInSafeArea`;
5. two 48 dp buttons offset by 10 dp → `assertNoInteractiveOverlap`; with the container's tag in `exemptTags` → passes;
6. a second title tagged `guard.bad.title` without `heading()` → `assertHeadings(rule, "guard.good.title", "guard.bad.title")` fails;
7. the good layout wrapped in `Box(Modifier.alpha(0f))` on the white background (semantics and bounds unchanged, no ink) → `assertInk`.

Each test < 5 s after the warm-up test.

**Plan tick:** "Reference-free guards".

### T9. Opt-in references and board captures (Robolectric and emulator)
**Commit:** `test(testing): wire opt-in snapshots and board captures`

- **Stored references.** `make snapshots` → `$(RUN_TIER) snapshots --results '*/build/test-results/testDebugUnitTest/*.xml' --results '*/*/build/test-results/testDebugUnitTest/*.xml' --timings '*/build/cf-timings/testDebugUnitTest/*.json' --timings '*/*/build/cf-timings/testDebugUnitTest/*.json' -- $(GRADLE) verifyRoborazziDebug -Pcf.tier=snapshot` (the Roborazzi tasks run `testDebugUnitTest`, so results land in its directories); `SNAPSHOT_RECORD=1 make snapshots` → the same with `recordRoborazziDebug`. Names `<Screen>_<state>_<profile>_<lang>_<theme>.png` in `<module>/src/test/snapshots/`. Default tiers never record or compare (Roborazzi captures nothing unless one of its tasks runs [verify]).
- `HarnessProbeSnapshotTest` (`@Snapshot`, `core/testing-compose/src/test`): a fixed probe (a 120 × 48 dp outlined box with two short texts) at pixel8, en, light, captured with `captureRoboImage("src/test/snapshots/HarnessProbe_default_pixel8_en_light.png")`. Record once and commit it: a harness reference, not a screen (OQ-0e-9).
- **Board captures.** `core/testing-compose/…/BoardCapture.kt`:
  - `fun capture(rule: SemanticsNodeInteractionsProvider, name: String)` writes `onRoot().captureToImage()` as PNG to `<cf.capturesDir>/<name>.png`. `name` may contain `/` subdirectories and must match `^[A-Za-z0-9][A-Za-z0-9._/-]*$`.
  - Naming convention (KDoc): `<Board>[_<state>][_<profile>]_<lang>_<theme>`, after the board file in `_design/canvas/`; non-board captures go under a subdirectory (`timeline/…`, `designsystem/…`).
  - `cf.captureOnly` is a comma list; a capture runs when any token is a prefix of its `name`, else it is skipped with `assumeTrue`. Empty = all.
  - Robolectric only in 0e: no `cf.capturesDir` system property → `IllegalStateException("cf.capturesDir is not set; run make captures")`. Phase 2 Task 18 adds the emulator path; keep the function signature so it can.
- `make captures [ONLY=a,b]`:
  - default: first `rm -rf build/captures`, then `$(RUN_TIER) captures` with the two `testDebugUnitTest` `--results` globs and the two `testDebugUnitTest` `--timings` globs of `snapshots` above, `-- $(GRADLE) testDebugUnitTest --rerun -Pcf.tier=capture -Pcf.captureOnly=$(ONLY)`, then print `captures: <n> PNGs in build/captures` (the count of `build/captures/**/*.png`). `--rerun` is required: the PNGs are not declared task outputs, so an up-to-date or build-cache hit of `testDebugUnitTest` (same inputs as the previous `make captures`) would write nothing and the second run would fail with 0 PNGs. 0 PNGs with an empty `ONLY` → exit 1 (missing evidence); 0 PNGs with a non-empty `ONLY` → exit 0 (every capture skipped by the filter).
  - `DEVICE=1`: print `make captures DEVICE=1 arrives in Phase 2 (feat/phase-2-design-system)` and exit 1, in 0a's not-landed style (Phase 2 Task 18 owns it, OQ-0e-18).
- `HarnessProbeCaptureTest` (`@Capture`, Robolectric, `core/testing-compose/src/test`) calls `BoardCapture.capture(rule, "harness/HarnessProbe_pixel8_en_light")` → `build/captures/harness/HarnessProbe_pixel8_en_light.png` (not a board, so it goes under a subdirectory, per the naming rule above and as Phase 2 does with `designsystem/…`).

Tests: `make snapshots` passes with the committed probe; `make captures` (run it twice in a row; the second run also writes the PNG) and `make captures ONLY=harness` write the Robolectric probe; `make captures ONLY=Nope` writes nothing and exits 0; `make captures DEVICE=1` prints the Phase 2 line and exits 1; `make quick` and `make check` write no PNG and no snapshot.

**Plan tick:** "Robolectric and Roborazzi".

### T10. `ConfigurationPolicy` spike
**Commit:** `feat(app): cap font scale and coerce locale at the root`

- `app/src/main/kotlin/net/chronoflow/app/config/DeviceConfigFile.kt` (`object`):
  - `const val FILE_NAME = "device-config"`.
  - `fun readForLaunch(context: Context, sdkInt: Int = Build.VERSION.SDK_INT): DeviceConfig`: `sdkInt >= 33` → `DeviceConfig()` **without touching disk**. Otherwise:
    ```kotlin
    val old = StrictMode.allowThreadDiskWrites()   // also allows reads; noBackupFilesDir may mkdir
    try { /* read at most 65 bytes of File(context.noBackupFilesDir, FILE_NAME), then DeviceConfig.decode */ }
    finally { StrictMode.setThreadPolicy(old) }
    ```
    A missing file or an `IOException` → defaults. Catch `IOException` only (never a bare `Exception`).
  - `suspend fun write(context: Context, config: DeviceConfig, io: CoroutineDispatcher)`: `AtomicFile` on `io`.
- `app/src/main/kotlin/net/chronoflow/app/ConfigurationPolicy.kt` (stateless `object`):
  - `fun overrideConfiguration(base: Context, sdkInt: Int = Build.VERSION.SDK_INT): Configuration` returns `Configuration()` (every field undefined) with only:
    - `fontScale = ConfigurationRules.capFontScale(base.resources.configuration.fontScale)`;
    - `setLocales(LocaleList(ConfigurationRules.resolveLocale(choice, systemLocale)))`, where API 33+ uses `choice = LanguageChoice.System` and `systemLocale = base.resources.configuration.locales[0]` (`LocaleManager` already put an explicit app language there), and API < 33 uses `choice = DeviceConfigFile.readForLaunch(base, sdkInt).language` with the same `systemLocale`;
    - then `setLayoutDirection(Locale.ENGLISH)` (after `setLocales`, which also sets a direction).
  - `fun wrap(context: Context): Context = context.createConfigurationContext(overrideConfiguration(context))` for non-activity contexts in later phases (`createConfigurationContext` applies only the defined fields of the override, as `applyOverrideConfiguration` does).
- `MainActivity.attachBaseContext(newBase)`: `super.attachBaseContext(newBase); applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(newBase))` (OQ-0e-8). No AppCompat dependency. `locale` and `fontScale` are not in `configChanges`, so a change recreates the activity and re-applies the policy (`14 §3.3`).

Tests:
- `ConfigurationPolicyTest` (Robolectric, `@Layout`: four SDK sandboxes are too slow for quick; `@Config(sdk = [29, 32, 33, 36])`, `app/src/test/kotlin/net/chronoflow/app/`). Build a `ContextThemeWrapper(base, 0)`, call `applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(base))`, read `wrapper.resources.configuration`. Do not launch MainActivity.
  - `@Config(qualifiers = "ar-rEG")` → locale `en`, `layoutDirection == View.LAYOUT_DIRECTION_LTR`;
  - `de-rDE` → `en`; `nl-rBE` → `nl-BE`;
  - `RuntimeEnvironment.setFontScale(2.0f)` → `fontScale == 1.3f`; `1.0f` → `1.0f`;
  - with a `device-config` of `language=nl\n` on an `en-rUS` phone: `nl` at API 29 and 32; `en-US` at 33 and 36 (file ignored);
  - a 65-byte `device-config` → `en-US`.
- `ConfigurationPolicyDeviceTest` (`@Smoke`, emulator, `app/src/androidTest/…/ConfigurationPolicyDeviceTest.kt`; each test restores every system setting in `finally`; launch with `ActivityScenario.launch(MainActivity::class.java)` after changing settings; wait with `compose.waitUntil(5_000)`):
  1. `appLocaleArabicIsEnglishLtr`: `LocaleManager.applicationLocales = LocaleList.forLanguageTags("ar-EG")` → locale `en`, decor view `LAYOUT_DIRECTION_LTR`, `"%,d".format(locale, 1234567) == "1,234,567"`. Reset to the empty list.
  2. `appLocaleDutchIsDutch`: `nl` → locale `nl`.
  3. `fontScaleTwoIsCapped`: `uiAutomation.executeShellCommand("settings put system font_scale 2.0")` → `resources.configuration.fontScale == 1.3f`. Restore `1.0`.
  4. `nightModeChangeKeepsCapAndLocale`: font scale 2.0 and app locale `ar-EG`, launch, then `cmd uimode night yes` and `waitUntil(5_000)` for night in `resources.configuration.uiMode`: font scale still 1.3, locale still `en`, direction still LTR. Restore `night no`, font scale 1.0 and app locales.
- `DeviceConfigStrictModeTest` (`@Smoke`): on the main thread (`instrumentation.runOnMainSync`), install `ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyDeath().build()`, call `DeviceConfigFile.readForLaunch(context, sdkInt = 29)` with a file holding `language=nl\n`, restore the policy. The process survives and the result is `Nl`.
- **Record in `docs/tooling.md` (OQ-A24):** whether the override held on API 36 (all four device tests), and whether the in-place `uiMode` change kept it. The API < 33 path is proven on Robolectric and through the injected `sdkInt`; the `api29` AVD runs only on request. If the override fails, apply the OQ-A24 fallback (AppCompat `setApplicationLocales`, and the font cap through the root `LocalDensity`) and record it.

**Plan tick:** "ConfigurationPolicy spike".

### T11. Seeds: `TestSeed`, `TestSeeds`, `UITestTimeline`, `SeedReceiverActivity`
**Commit:** `feat(debug): add the seed receiver and test seeds`

All files in `app/src/debug/kotlin/net/chronoflow/debug/`. Nothing in `src/main` references them.

Dependencies (`app/build.gradle.kts`, each on its own line, only if the stack top lacks them; 0a's `:app` declares only `activity-compose`, `ui`, `foundation`, `core-ktx`): `implementation(<kotlinx-coroutines-android>)` (`withContext`, `Dispatchers.IO`; T10's `DeviceConfigFile.write` uses it too, so add it in T10 if T10 lands first), `implementation(<lifecycle-runtime-compose>)` if `lifecycleScope` does not resolve through `activity-compose`, and `testImplementation(<kotlinx-coroutines-test>)` (`runTest`). All aliases are in 0a's catalog.

- `TestSeed.kt` (ports iOS `AuthUITestConfiguration.init?` and `LaunchArguments` parsing):
  ```kotlin
  data class TestSeed(
      val testId: String?,               // CF_AUTH_TEST_ID: canonical UUID, lower-cased; null → every CF_* key is dropped
      val profile: UiTestProfile?,       // CF_UI_TEST_PROFILE: exactly "screen" | "launch"; else null
      val theme: ThemeChoice?,           // cf.settings.theme
      val language: LanguageChoice?,     // cf.settings.language
      val reduceMotion: Boolean?,        // cf.reduceMotion: "1"/"true" → true, "0"/"false" → false, else null
      val freezeAtMs: Double?,           // cf.freezeAt as ms: finite, 0.0..60000.0 inclusive; else null
      val freezeAtRaw: String?,          // cf.freezeAt verbatim (1d reads an ISO-8601 instant from it, OQ-0e-12)
      val readyAtMs: Double?,            // cf.readyAt, same rule; null when absent (3b applies the 4600 default)
      val mainThreadStallMs: Double?,    // cf.mainThreadStall, same rule
      val raw: Map<String, String>,      // every accepted key → value, for later phases (4e routes, 1d trip page)
      val ignoredKeys: Set<String>,      // CF_* keys dropped for a missing/invalid id, and non-string extras
  ) {
      companion object {
          fun parse(extras: Map<String, String>, nonStringKeys: Set<String> = emptySet()): TestSeed
          fun fromIntent(intent: Intent?): TestSeed   // string extras only; other extras → ignoredKeys
      }
  }
  enum class UiTestProfile(val raw: String) { Screen("screen"), Launch("launch") }
  ```
  - **UUID:** `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`, then lower-case. `UUID.fromString` alone is too lenient (`1-1-1-1-1` parses).
  - **Gating (OQ-0e-6):** `CF_*` keys need a valid `CF_AUTH_TEST_ID`, as on iOS. `cf.*` keys (iOS launch arguments) apply without it.
  - **Extras:** `fromIntent` uses `extras.keySet()` and `extras.getString(k)`; a key whose `getString` is null is non-string. Never the deprecated `Bundle.get`.
- `UITestTimeline.kt`: `object UITestTimeline { fun record(event: String, seed: TestSeed, details: String = "", allowUngated: Boolean = false) }` → `Log.i("UITestTimeline", "CF_UI_TIMELINE id=<testId|ungated> pid=<Process.myPid()> event=<event> wall=<currentTimeMillis/1000.0, 3 decimals> uptime=<SystemClock.uptimeMillis/1000.0, 3 decimals> <details>")`. Without a test id and without `allowUngated`, it logs nothing (iOS).
- `TestSeeds.kt`, `object TestSeeds` (stateless):
  - `suspend fun apply(context: Context, seed: TestSeed, io: CoroutineDispatcher, sdkInt: Int = Build.VERSION.SDK_INT)`, everything inside `withContext(io)`:
    1. `testId != null`: delete and recreate `filesDir/uitests/<testId>/` (`13 §5.2` step 3);
    2. `language` non-null: `sdkInt >= 33` → `LocaleManager.applicationLocales = LocaleList.forLanguageTags(tag)` (`System` → `LocaleList.getEmptyLocaleList()`); below 33, read-modify-write the `language` field of `device-config` through `DeviceConfigFile`;
    3. `theme` non-null: `sdkInt >= 31` → `UiModeManager.setApplicationNightMode(MODE_NIGHT_AUTO | MODE_NIGHT_NO | MODE_NIGHT_YES)` for system/light/dark (the production mirror, plan "configuration and appearance"); below 31 → the `theme` field of `device-config`. DataStore arrives in 4a/10b;
    4. absent keys leave stored values alone;
    5. log `seed-begin`, then `seed-applied` (details `keys=<sorted raw keys>`), plus `seed-ignored keys=<…>` when any.
  - `fun forwardIntent(context: Context, seed: TestSeed): Intent`: explicit `Intent(context, MainActivity::class.java)` carrying `seed.raw` as string extras, with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`, so `attachBaseContext` runs again.
- `SeedReceiverActivity.kt`: `ComponentActivity`, no UI. `onCreate`: `val seed = TestSeed.fromIntent(intent)`, then `lifecycleScope.launch { TestSeeds.apply(applicationContext, seed, Dispatchers.IO); startActivity(TestSeeds.forwardIntent(this@SeedReceiverActivity, seed)); finish() }` (pass `Dispatchers.IO` here if 0a's Konsist allows it in debug code; else inject it from a debug `object`).
- `app/src/debug/AndroidManifest.xml` (merge with 0a's): `<activity android:name="net.chronoflow.debug.SeedReceiverActivity" android:exported="true" android:excludeFromRecents="true" android:noHistory="true" android:theme="@android:style/Theme.Translucent.NoTitleBar" />`. If Lint warns about the exported activity, suppress with `tools:ignore` on this element only.
- Document manual use in `docs/tooling.md`:
  ```sh
  adb shell am start -n net.chronoflow.app.native.debug/net.chronoflow.debug.SeedReceiverActivity \
    --es CF_AUTH_TEST_ID "$(uuidgen | tr A-Z a-z)" --es CF_UI_TEST_PROFILE screen --es cf.settings.language nl
  ```

Tests:
- `TestSeedTest` (plain JVM, `app/src/testDebug/kotlin/net/chronoflow/debug/`): canonical lower-case UUID kept; upper-case lower-cased; `1-1-1-1-1`, `""` and a 37-char string rejected, dropping every `CF_*` key into `ignoredKeys`; `cf.settings.language=nl` applies without a UUID; profile `screen`, `launch`, `Screen` → null, `fast` → null; themes and languages valid and invalid; reduceMotion `1`, `true`, `0`, `false`, `yes` → null; freezeAt `0`, `60000`, `1500.5` accepted; `60000.1`, `-1`, `NaN`, `Infinity`, `abc` → `freezeAtMs` null; `2026-10-13T08:00:00Z` → `freezeAtMs` null and `freezeAtRaw` kept; `nonStringKeys = {"cf.reduceMotion"}` → in `ignoredKeys`; unknown `CF_FOO` with a valid id lands in `raw`.
- `TestSeedsApplyTest` (`@Logic`, Robolectric, `@Config(sdk = [36])`, `app/src/testDebug/…`, `runTest(timeout = 60.seconds)` with a `StandardTestDispatcher` as `io`): `apply` creates `filesDir/uitests/<id>/` and removes a stale file in it; `sdkInt = 32` + language `nl` writes `device-config` = `language=nl\ntheme=system\n`; then `sdkInt = 30` + theme `dark` (below 31, so the theme goes to the file; at 32 it would go to `UiModeManager` and leave the file untouched) gives exactly `language=nl\ntheme=dark\n`; `forwardIntent` carries every `raw` extra and both flags.

### T12. `ScreenTest` and the seeded smoke tests
**Commit:** `test(app): add the seeded screen test base`

- `app/src/androidTest/kotlin/net/chronoflow/app/testing/ScreenTest.kt` (abstract, no tier annotation):
  - `@get:Rule val compose = createEmptyComposeRule()`;
  - `protected fun launch(seed: Map<String, String> = emptyMap()): ActivityScenario<MainActivity>`: fills defaults unless given: `CF_AUTH_TEST_ID` = a fresh lower-case UUID, `CF_UI_TEST_PROFILE=screen`, `cf.settings.language=system`, `cf.settings.theme=system` (language and theme persist across tests, so every launch resets them). Then `runBlocking { TestSeeds.apply(targetContext, parsed, Dispatchers.IO) }` (the instrumentation thread, not main), `ActivityScenario.launch<MainActivity>(TestSeeds.forwardIntent(targetContext, parsed))`, `compose.waitUntilExactlyOneExists(hasTestTag("app.root"), 5_000)`, and `UITestTimeline.record("seeded-screen-visible", parsed)`;
  - `protected fun waitFor(timeoutMillis: Long = 5_000, condition: () -> Boolean)` = `compose.waitUntil(timeoutMillis, condition)` (3a uses it);
  - `@After`: close the scenario.
  - It calls the same `TestSeeds.apply` in-process instead of going through the receiver (OQ-0e-7).
- `ScreenTestHarnessTest` (`@Smoke`, extends `ScreenTest`): `launch(mapOf("cf.settings.language" to "nl"))`; `app.root` exists exactly once; the resumed activity's `resources.configuration.locales[0].language == "nl"`; `filesDir/uitests/<id>/` exists. `@After` launches nothing; the next test's defaults reset language (and this class's own `@After` calls `TestSeeds.apply` with `language=system`).
- `SeedReceiverActivityTest` (`@Smoke`, `createEmptyComposeRule()`): start `SeedReceiverActivity` with an explicit intent and string extras through `ActivityScenario.launch`; `compose.waitUntilExactlyOneExists(hasTestTag("app.root"), 5_000)`; assert through `ActivityLifecycleMonitorRegistry` (on the main thread) that a `MainActivity` is `RESUMED` and no `SeedReceiverActivity` is alive. Finish `MainActivity` in `@After`.
- Record the seed-begin → `seeded-screen-visible` time (median of 5 runs, from `adb logcat -d -s UITestTimeline`) in `docs/tooling.md`. Target ≤ 1 s (iOS), not asserted.

### T13. Release readiness
**Commit:** `test(app): prove release ships no seed code`

`app/src/testRelease/kotlin/net/chronoflow/app/ReleaseReadinessTest.kt` (plain JVM, runs in `testReleaseUnitTest`):
- `Class.forName(name)` throws `ClassNotFoundException` for `net.chronoflow.debug.SeedReceiverActivity`, `…TestSeeds`, `…TestSeed`, `…UITestTimeline`;
- `BuildConfig.DEBUG == false`.

T15 adds the perf assertions. Also run `./gradlew :app:verifyReleaseManifest`: the release merged manifest has no `SeedReceiverActivity`.

**Plan tick:** "Seed harness".

### T14. Results checker and budgets
**Commit:** `build(perf): check benchmark results against budgets`

1e Task 3 extends this checker; keep its shapes.

- `benchmark/budgets.json`:
  ```json
  {
    "schema": 1,
    "labels": {"startup": "Startup", "timeline": "Timeline"},
    "startup": [
      {"test": "startupCold", "metric": "timeToInitialDisplayMs", "stat": "median", "unit": "ms",
       "reference": {"hard": 400}, "lowEnd": {"hard": 800}, "emulator": {"report": true}}
    ],
    "timeline": [
      {"test": "timelineFirstFrame", "metric": "timelineFirstFrameMs", "stat": "each", "unit": "ms",
       "emulator": {"hard": 800}, "reference": {"hard": 800, "aim": 450}, "lowEnd": {"hard": 1500, "aim": 800}}
    ]
  }
  ```
  Values from plan.md "Performance budget". The 400 ms reference and 800 ms low-end gates stay "open: device only" until a device exists (OQ-A22).
- `Scripts/check-benchmarks.py` (stdlib, `python3 -I`):
  - **Arguments:** `--suite startup|timeline --device-class emulator|reference|lowEnd|unknown --results-dir <dir> [--budgets benchmark/budgets.json] [--history <md>] [--theme light|dark] [--font-scale <f>] [--refresh-hz <n|unknown>] [--gpu <mode|unknown>] [--commit <sha[-dirty]>] [--append]`.
  - **Input:** every `*-benchmarkData.json` under `--results-dir`, recursively. Match benchmarks by `name` (the method) and `className` ending `.StartupBenchmark` / `.TimelineBenchmark`. Expected shape [verify on the first real run]: `benchmarks[].metrics.<key>.{minimum,maximum,median,runs[]}`, `benchmarks[].sampledMetrics.<key>.{P50,P90,P95,P99,runs[][]}`, `context.build.{model,version.sdk}`.
  - **`RAW_KEYS`**, one table: `timeToInitialDisplayMs → timeToInitialDisplayMs`, `timelineFirstFrameFirstMs → timelineFirstFrameMs` (1e confirms the second).
  - **Stats:** `median|min|max` read `metrics`; `each` checks every value of `runs`; `P50|P90|P95|P99` read `sampledMetrics`; `present` means the entry exists.
  - **Derived helpers** (1e uses them): `jank_percent(runs_i) = 100 × count(v > 0) / len`; `hitch_ms_per_s(runs_i, window_ms) = Σ max(0, v) / (window_ms / 1000)`; with no window, `window_ms = len(runs_i) × 1000 / refresh_hz`.
  - **Exit 1** on: no result file (`no benchmark results in <dir>`); a test of the suite absent; a budgeted metric missing; a non-finite value; a negative duration; a value over the selected class's `hard`. `unknown` class and `"report": true` only print. Usage errors exit 2.
  - **Output**, one line per value: `<Label> metric: <test> <metric> <stat>=<value> <unit> [hard=<h> aim=<a>] <PASS|FAIL|AIM-MISSED|REPORT>`, e.g. `Startup metric: startupCold timeToInitialDisplayMs median=512.3 ms REPORT`.
  - **History (`--suite startup`):** the `## Runs` table of `--history`, header exactly `| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Build | TTID ms med/min/max | Result |`. Build is `benchmarkRelease, Partial(UseIfAvailable)` (OQ-0e-11). Result: `FAIL` if any value failed, else `PASS` if a hard gate was checked, else `REPORT`. `--append` adds the row as the table's last line, including a FAIL row. **Trend:** `Startup trend: timeToInitialDisplayMs <old> -> <new> (<±x%>) FLAGGED` when |Δ| > 25 % against the last `PASS` or `REPORT` row with the same Class, Theme and Font. A trend never fails.
- `docs/perf-startup.md`: "How to run" (`make perf`), "Budgets" (the plan's startup rows), "## Runs" (header only), "Device rows: open: device only (OQ-A22)".
- `Scripts/tests/test_check_benchmarks.py` (unittest; `make quick` runs it through `Scripts/test_scripts.sh`; load the script with `importlib.util.spec_from_file_location` because of the hyphen). Fixtures in `Scripts/fixtures/benchmarks/` (the path 1e uses):
  - `startup/pass/` (TTID runs of 10 values) with `--device-class emulator` → exit 0, a `Startup metric:` line ending `REPORT`;
  - `startup/missing-ttid/` → exit 1, names `timeToInitialDisplayMs`;
  - `startup/nan/` (`NaN` in `runs`) → exit 1;
  - `startup/median-450/` with `--device-class reference` → exit 1;
  - `timeline/first-frame-810/` (runs `[610, 640, 700, 810, 655, 630, 620, 645, 660, 635]`) with `--suite timeline --device-class emulator` → **exit 1** naming `timelineFirstFrameMs` (the iOS 0.81 s expected failure);
  - an empty directory → exit 1, `no benchmark results`;
  - `test_derived_frame_math`: runs `[[-1, 2, 0, 5], [0, 0, 0, 0], [3, -4, 1, 0]]` with windows `[1000, 2000, 500]` → jank `[50.0, 0.0, 50.0]`, hitch `[7.0, 0.0, 8.0]`; without windows at 60 Hz, `[[-2, 1, 3, -1], [0, -5]]` → hitch `[60.0, 0.0]` (4 / 0.0667 s);
  - `test_append_and_trend`: append to a copy of a history fixture → exactly one new row of 12 cells; previous REPORT row 400 ms, new median 520 ms → `FLAGGED`, exit 0.

**Plan tick:** "Results checker".

### T15. Perf variants
**Commit:** `build(app): add perf variants with perf-only sources`

- The `chronoflow.android.application` convention applies `androidx.baselineprofile` (1.5.0) to `:app`; `chronoflow.android.test` applies it to `:benchmark` (`targetProjectPath = ":app"` from 0a).
- `:app`:
  ```kotlin
  androidComponents {
      onVariants(selector().withBuildType("benchmarkRelease")) { addPerfSources(it) }
      onVariants(selector().withBuildType("nonMinifiedRelease")) { addPerfSources(it) }
  }
  // addPerfSources(v): v.sources.kotlin?.addStaticSourceDirectory("src/perf/kotlin")
  //                    v.buildConfigFields?.put("PERF_ENTRY", BuildConfigField("boolean", "true", null))
  ```
  plus `defaultConfig { buildConfigField("boolean", "PERF_ENTRY", "false") }`. BuildConfig stays enabled in `:app` only.
- `app/src/perf/kotlin/net/chronoflow/app/perf/PerfEntry.kt`: `internal object PerfEntry { const val ENABLED = true }` (a marker; 1d adds the `cf.trip-page` entry here, OQ-0e-19).
- **`:core:data-mock` in the perf variants** (plan "Android spec: build", "Perf variants"; 0a left this to you). In `app/build.gradle.kts`, next to 0a's `debugImplementation(project(":core:data-mock"))`, add `"benchmarkReleaseImplementation"(project(":core:data-mock"))` and `"nonMinifiedReleaseImplementation"(project(":core:data-mock"))` (string notation: these configurations exist only once the plugin adds the build types). [verify] If Gradle fails with `Configuration with name 'benchmarkReleaseImplementation' not found` (the plugin may add its build types only in `finalizeDsl`, after the build script ran), replace both lines with exactly
  ```kotlin
  configurations.matching { it.name == "benchmarkReleaseImplementation" || it.name == "nonMinifiedReleaseImplementation" }
      .configureEach { project.dependencies.add(name, project(":core:data-mock")) }
  ```
  and record it in `docs/tooling.md`. With the OQ-A28 fallback, the single line is `"benchmarkImplementation"(…)`. Never `implementation` or `releaseImplementation`.
- **`ModuleGraphTest.dataMockOnlyInDebugAndPerf`** (extend 0a's `konsist/…/ModuleGraphTest.kt`, same file-text style): in `app/build.gradle.kts`, every line containing `":core:data-mock"` starts (after whitespace) with `debugImplementation(`, `"benchmarkReleaseImplementation"(`, `"nonMinifiedReleaseImplementation"(` or `"benchmarkImplementation"(`, or is the fallback's `.configureEach { project.dependencies.add(name, project(":core:data-mock")) }` line directly below a `configurations.matching` line that names only those two configurations; no other `build.gradle.kts` outside `core/data-mock/` names `:core:data-mock` except as `testImplementation`/`androidTestImplementation`. Planted violation `konsist/src/test/resources/violations/moduleGraph/dataMock/app/build.gradle.kts` with `implementation(project(":core:data-mock"))` → `…RejectsPlantedViolation` throws.
- `<profileable android:shell="true"/>`: check the merged manifests of `benchmarkRelease` and `nonMinifiedRelease` first; the plugin may add it [verify]. Only if absent, add `app/src/benchmarkRelease/AndroidManifest.xml` and `app/src/nonMinifiedRelease/AndroidManifest.xml` with `<application><profileable android:shell="true" tools:targetApi="29"/></application>`.
- `ReleaseReadinessTest` gains: `Class.forName("net.chronoflow.app.perf.PerfEntry")` throws; `BuildConfig.PERF_ENTRY == false`.
- **Verify (OQ-A28)** and record commands and outputs in `docs/tooling.md`:
  - `./gradlew :app:compileNonMinifiedReleaseKotlin :app:compileReleaseKotlin`, then `find app/build -name 'PerfEntry*.class'` lists only `nonMinifiedRelease` paths;
  - the `benchmarkRelease` merged manifest has `profileable shell="true"`; the `release` one has no `profileable`;
  - `./gradlew :app:testReleaseUnitTest :app:verifyReleaseManifest` passes.
  - `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -c data-mock` prints `0`; the same for `benchmarkReleaseRuntimeClasspath` and `nonMinifiedReleaseRuntimeClasspath` prints ≥ 1 (run without the configuration cache if the task refuses it, and say so).
  - If the plugin's build types reject extra sources: a hand-written `benchmark` build type (`initWith(release)`, debug signing, `matchingFallbacks += "release"`, source set `src/perf`) and record it.

### T16. `StartupBenchmark` and `make perf`
**Commit:** `test(benchmark): measure cold start on the empty app`

- `benchmark/src/main/kotlin/net/chronoflow/benchmark/StartupBenchmark.kt`:
  ```kotlin
  @Perf
  @RunWith(AndroidJUnit4::class)
  class StartupBenchmark {
      @get:Rule val rule = MacrobenchmarkRule()
      private val packageName: String =
          InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app.native"

      @Test fun startupCold() = rule.measureRepeated(
          packageName = packageName,
          metrics = listOf(StartupTimingMetric()),
          iterations = 10,
          startupMode = StartupMode.COLD,
          compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
          setupBlock = { pressHome() },
      ) { startActivityAndWait() }
  }
  ```
  `UseIfAvailable` until 1e commits the profiles (OQ-0e-11). `:app` needs `androidx.profileinstaller` 1.4.1 (A1); add it if 0a did not.
- `benchmark/build.gradle.kts` (0a left it as a skeleton with only the convention plugin; each addition on its own line):
  - dependencies: `implementation(<benchmark-macro-junit4>)`, `implementation(<androidx.test.ext:junit>)`, `implementation(project(":core:testing"))` (for `@Perf`; T6's `testingNotInMain` exempts `benchmark/`);
  - `android { experimentalProperties["android.experimental.self-instrumenting"] = true }`, so the test APK instruments itself and `MacrobenchmarkRule` can kill and cold-start the target process [verify it is still required under AGP 9.5.1 and `androidx.baselineprofile` 1.5.0; record];
  - `targetAppId`: if the baselineprofile plugin does not already pass it [verify in the instrumentation arguments of the first run], add
    ```kotlin
    androidComponents {
        onVariants { v ->
            val loader = v.artifacts.getBuiltArtifactsLoader()
            v.instrumentationRunnerArguments.put("targetAppId", v.testedApks.map { loader.load(it)?.applicationId })
        }
    }
    ```
    The `?: "net.chronoflow.app.native"` fallback keeps the benchmark correct either way (the perf variants have no suffix).
- `Scripts/emulator.sh ensure` writes the GPU mode it booted with to `build/emulator/gpu-mode` (`host` for 0a's flags); 1e reads it. Extend 0a's `Scripts/tests/test_emulator.py`: `ensure` from a cold (fake) boot writes `host` to that file.
- `Scripts/perf-startup.sh`, run by `make perf` as `$(RUN_TIER) perf --device --results 'benchmark/build/outputs/androidTest-results/connected/**/*.xml' -- Scripts/perf-startup.sh` (`--device` takes the host-wide emulator lock, 0a OQ-0a-21). No `--emulator` and no `--timings`: the 10 s emulator budget does not apply to benchmarks, whose gates are the checker's. The script mirrors 1e's `perf-timeline.sh`:
  1. require exactly one device (`adb get-state`);
  2. `IS_EMU=1` when `getprop ro.boot.qemu` or `ro.kernel.qemu` prints `1`; `CLASS=emulator` then, else `${DEVICE_CLASS:-reference}`;
  3. on an emulator, read `build/emulator/gpu-mode` (`unknown` if absent) and refuse unless it is `host`;
  4. `rm -rf benchmark/build/outputs/connected_android_test_additional_output`;
  5. `trap 'Scripts/emulator.sh prep-functional' EXIT`, then `Scripts/emulator.sh prep-perf`; check `settings get global animator_duration_scale` is `1` or `1.0`, else stop;
  6. `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.StartupBenchmark -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark -Pandroid.testInstrumentationRunnerArguments.timeout_msec=300000`, plus `-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR` only when `IS_EMU=1` (10 cold starts exceed 60 s; OQ-1e-2);
  7. always run `python3 -I Scripts/check-benchmarks.py --suite startup --device-class $CLASS --results-dir benchmark/build/outputs/connected_android_test_additional_output --history docs/perf-startup.md --theme light --font-scale "$(adb shell settings get system font_scale | tr -d '\r')" --refresh-hz <60 on the emulator per hw.lcd.vsync, else unknown> --gpu "$GPU" --commit "$(git rev-parse --short HEAD)[-dirty]" --append` (`-dirty` when `git status --porcelain` lists anything other than `docs/perf-startup.md`);
  8. exit non-zero if Gradle or the checker failed.
- `Scripts/tests/test_perf_startup.py` drives the script with `PERF_STARTUP_DRY_RUN=1` (prints commands instead of running them) and stub `adb`/`gradlew`/`emulator.sh` on `PATH`: `suppressErrors=EMULATOR` appears only when the stub `getprop` returns `1`; the checker runs after a stubbed Gradle failure; the trap restores the scales after that failure; a non-`host` GPU on an emulator refuses.
- Run `make perf` on `pixel` once (or ask the orchestrator). The row lands in `docs/perf-startup.md`. Emulator numbers are never device claims.

**Plan tick:** "Benchmark module".

### T17. Hang caps verified, tiers measured, decisions recorded
**Commit:** `docs(tooling): record phase 0e decisions and tier timings`

- **`timeout_msec` (OQ-A29).** Write a temporary, **uncommitted** `app/src/androidTest/kotlin/net/chronoflow/app/HangCapProbeTest.kt` (`@Smoke`) whose test calls `CountDownLatch(1).await(70, TimeUnit.SECONDS)`. Run `make targeted ONLY="app:HangCapProbeTest"`. It must fail with a timeout after about 60 s. Record the elapsed time and the failure text, then delete the file. If it is not honoured: add a JUnit 4 `Timeout.seconds(60)` rule to `ScreenTest` (instrumented only, never Robolectric) and rely on the run-tier caps; record that.
- **Other caps:** Gradle `Test.timeout` 10 min (T2/§5) and `runTest(timeout = 60.seconds)` (T1 rule 5). Record them.
- **Measure:** `Scripts/measure-tiers.sh` (three warm runs each, `pixel` booted). Record the medians and the slowest 10 tests from `build/test-results/quick/timings.json`. `make quick` must be < 60 s. If it is over:
  1. confirm that quick runs only plain JVM and `@Logic` classes;
  2. if the Robolectric warm-up dominates, move `GuardsSelfTest` (then `LargeFontEqualsCapTest`) to `@Layout` and record it;
  3. never raise a budget.
- **`docs/tooling.md`**, a "Phase 0e" section: OQ-A24 (T10), OQ-A28 (T15), OQ-A29 (above), OQ-0e-2 (T2) and OQ-0e-17 outcomes; the JUnit 5.x version; the measured Robolectric warm-up per SDK; the seed-to-screen median (T11/T12); the T15 classpath outputs; the commands the orchestrator ran for you.
- Add the OQ-0e rows of §10 to plan.md "Open questions for the user" (one row each: question, default, `0e`).
- In plan.md's Phase 0e "Results checker" bullet, change `Scripts/test_check_benchmarks.py` to `Scripts/tests/test_check_benchmarks.py` (OQ-0e-16).

**Plan tick:** "Hang caps".

---

## 7. Plan boxes and commits

| plan.md Phase 0e box | Ticked in |
|---|---|
| Tier registry | T3 |
| Profiles | T6 |
| Robolectric and Roborazzi | T9 |
| Reference-free guards | T8 |
| ConfigurationPolicy spike | T10 |
| Seed harness | T13 |
| Results checker | T14 |
| Benchmark module | T16 |
| Hang caps | T17 |

If the Step 0 output is still missing when you reach T5, do not commit a placeholder. T6–T9 need `DeviceProfiles` (T5), so continue with the tasks that do not: T10, T11, T13, T14, T15 (in that order), then T5–T9, T12, T16 and T17 once the orchestrator has sent the output. The ticks stay in the commits this table names.

---

## 8. Acceptance criteria

All on the phase branch. The orchestrator runs the emulator ones if your sandbox cannot.

1. `make quick` passes in **< 60 s warm** (three runs in `docs/tooling.md`), and `build/test-results/quick/timings.json` has `"overBudget": 0` and non-empty `budgetChecks`.
2. `make check` passes with `pixel` booted (< 5 min warm, recorded). The smoke set includes `AppLaunchSmokeTest`, `PixelProfileInsetsTest`, `ScreenTestHarnessTest`, `SeedReceiverActivityTest`, `ConfigurationPolicyDeviceTest` (4 tests) and `DeviceConfigStrictModeTest`, each < 10 s in `build/test-results/smoke/timings.json`.
3. `./gradlew :konsist:test`: the registry and both T6 rules pass on the tree and reject all 7 planted tier files and both T6 planted files.
4. `make targeted ONLY="core:testing-compose:GuardsSelfTest"`: each planted layout fails only its guard; the good layout passes.
5. `Scripts/test_scripts.sh` passes, including `first-frame-810` → exit 1, `missing-ttid` → exit 1, the new `test_run_tier.py` budget cases and `test_perf_startup.py`.
6. `make snapshots` passes; `SNAPSHOT_RECORD=1 make snapshots` then `git diff --exit-code core/testing-compose/src/test/snapshots` is clean; `make captures` writes `build/captures/harness/HarnessProbe_pixel8_en_light.png`; `make captures ONLY=Nope` exits 0 with no PNG; `make captures DEVICE=1` prints the Phase 2 line and exits 1.
7. `./gradlew :app:testReleaseUnitTest :app:verifyReleaseManifest` passes; `:core:data-mock` is on the two perf runtime classpaths and not on `releaseRuntimeClasspath` (T15), and `ModuleGraphTest.dataMockOnlyInDebugAndPerf` rejects its planted file.
8. `make perf` on `pixel` exits 0, prints `Startup metric: startupCold timeToInitialDisplayMs median=… ms REPORT`, and appends one row to `docs/perf-startup.md`.
9. The T11 `adb shell am start … SeedReceiverActivity …` command brings up `MainActivity` in Dutch (`adb shell dumpsys activity activities | grep -i locale`, or `ScreenTestHarnessTest`), and logcat shows `CF_UI_TIMELINE … event=seed-applied`.
10. `docs/tooling.md` records the OQ-A24, OQ-A28, OQ-A29, OQ-0e-2 and OQ-0e-17 outcomes; `docs/device-matrix.md` records the measured insets.
11. `make quick`, then `make check`, then `make quick` again: the last run prints `Reusing configuration cache.` for its Gradle call.
12. Each §7 box is ticked in its commit; commits are atomic Conventional Commits without trailers.

---

## 9. Review checklist (Opus reviewer)

- **Measured, not guessed:** pixel8 `insetsPx` match `docs/device-matrix.md` and `PixelProfileInsetsTest` passes on `pixel`. foldInner says "derived".
- **0a reused, not duplicated:** tier annotations live in `net.chronoflow.core.testing.tiers` next to `Smoke.kt`; `timings.json` is still written only by `run_tier.py`; Python tests are in `Scripts/tests/`; JUnit XML stays at Gradle's default paths, so 0a's globs still match.
- **Tier hygiene:** one tier per Robolectric class; emulator classes `@Smoke` or `@Capture`; `StartupBenchmark` `@Perf`. A `cf.tier=quick` run's XML has no `@Layout` class. `failOnNoDiscoveredTests` is off, so a tier with no tests in a module passes.
- **Budgets:** listener warm-up is per SDK, not per class; `@PerfBudget` exempts; an over-budget test makes run-tier exit 3; a missing timing file exits 2.
- **Hang caps:** no `Timeout` rule in Robolectric tests; every `runTest` has `timeout = 60.seconds`; `timeout_msec=60000` in the app runner args; Gradle `Test.timeout` 10 min; 300 s only in `perf-startup.sh`.
- **No sleeps:** no `Thread.sleep`, `SystemClock.sleep` or `delay` waits in tests; `CountDownLatch.await` only in the uncommitted probe.
- **ProfileRule order:** `RuleChain.outerRule(profileRule).around(composeRule)`; qualifiers in Android order; font scale through `ConfigurationRules.capFontScale`.
- **`LocalSafeInsets`:** no default (it errors); provided at the root and by `ProfileHost`; the Konsist rule blocks other `safeDrawing` reads.
- **Seeds:** all in `app/src/debug`; nothing in `src/main` references `net.chronoflow.debug`; string extras only; the canonical-UUID regex; `CF_*` gated, `cf.*` not; ms range `0…60000` inclusive; `freezeAtRaw` kept for 1d; `apply` runs on the injected IO dispatcher; `ScreenTest` resets language and theme to `system` by default; `UITestTimeline` logs nothing ungated.
- **ConfigurationPolicy:** `applyOverrideConfiguration` after `super.attachBaseContext`; only font scale, locales and layout direction are set, on a fresh `Configuration()`; the `device-config` read happens only when `sdkInt < 33`, reads at most 65 bytes, sits inside `allowThreadDiskWrites` with the policy restored in `finally`; `LocaleManager` behind `>= 33`, `setApplicationNightMode` behind `>= 31`; no AppCompat; only `IOException` caught.
- **Types:** no second `Language` type in `:core:model` (0d owns it); `AppLanguage` is used.
- **Checker:** fails on a missing file, missing test, missing metric, NaN, negative duration and over-hard values; `report` and trends never fail; `suppressErrors=EMULATOR` never reaches a physical device; the results dir is deleted before a run; a trap restores the animator scales; `targetAppId` drives the package name.
- **Release:** `testReleaseUnitTest` proves no seed or perf class; the release merged manifest has no `SeedReceiverActivity` and no `profileable`; `:core:data-mock` reaches only `debug` and the two perf variants (never `implementation`/`releaseImplementation`).
- **Scope held:** no emulator capture path, no `HarnessProbeDeviceCaptureTest`; `make captures DEVICE=1` is the Phase 2 stub. No screen, no visible string, no `res/values*/strings.xml` change.
- **Config cache:** properties and environment through `providers`; tier passed through an argument provider; no `project` access at execution time.
- **Parallel-phase hygiene:** Makefile and `settings.gradle.kts` edits on their own lines; `core/model/build.gradle.kts` and the contracts untouched.
- **Typical mistakes:** guessing insets; `createAndroidComposeRule<MainActivity>()` in `ScreenTest` (it launches before seeding); `UUID.fromString` without the regex; `Bundle.get`; `CompilationMode.Partial()` (defaults to `Require`) before profiles exist; benchmarking the `.debug` package; committing the hang probe; multi-SDK Robolectric tests in quick; a guard that cannot fail (e.g. `touchBoundsInRoot` in `assertTouchTargets`); `cf-timings/**` globs that mix other tasks' stale timing files into a tier; a bare `make test` `test` task that runs Robolectric in release unit tests; a pure-function test under Robolectric (`ProfileQualifiersTest` is plain JVM); `runCatching` around coroutine code (swallows `CancellationException`); a second writer of `build/test-results/<tier>/timings.json`; Vintage missing from `:core:testing-junit`'s own tests (zero tests discovered, silently green); a Konsist tier rule that relies on `moduleOf` for `benchmark/` (skipped silently); planted tier files without a source-set path; a class-level `ProfileRule` with per-method profiles; `make captures` without `--rerun` (an up-to-date test task writes no PNG).

---

## 10. Open questions (proceed on the default; T17 adds them to plan.md)

| # | Question | Default |
|---|---|---|
| OQ-0e-1 | A3 makes `:core:testing` JVM (Konsist rule 2 forbids Android imports there), but guards need Compose UI test and Robolectric, and the tier filter needs the JUnit Platform API. Phase 2's brief says `core/testing/…/Guards.kt` | Add `:core:testing-compose` (Android test helpers: `ProfileRule`, `ProfileHost`, `Guards`, `BoardCapture`) and `:core:testing-junit` (JVM: filter and listener). `:core:testing` keeps annotations, profiles and fixtures. Later briefs read "Guards.kt" as `:core:testing-compose` |
| OQ-0e-2 | Plan: "annotations … and a Gradle test listener". JUnit 4 categories cannot read custom annotations, and a Gradle listener cannot see annotations | JUnit Platform + Vintage with a `PostDiscoveryFilter` and a `TestExecutionListener` registered by `ServiceLoader`; `run_tier.py` merges the listener files. Fallback: `@Category` mapped to tags. Adds `@Logic` for Robolectric tests in quick (1d OQ-1d-4) |
| OQ-0e-3 | The first Robolectric test on each SDK in a JVM pays the sandbox start | 20 s `warmup` cap for that test; 5 s for all others. Record the measured warm-up |
| OQ-0e-4 | Region after coercion: iOS drops it, the plan keeps it | Keep the phone's region only when its language is the resolved one (`nl-BE` → `nl-BE`, `de-DE` → `en`), so picker week starts follow the phone |
| OQ-0e-5 | `device-config` format is unspecified | UTF-8 `language=<system\|en\|nl>\ntheme=<system\|light\|dark>\n`, ≤ 64 B, defaults on anything invalid |
| OQ-0e-6 | Do `cf.*` extras need `CF_AUTH_TEST_ID`? | No. Only `CF_*` keys are gated, as iOS gates the environment but not launch arguments. Release reads none |
| OQ-0e-7 | Plan: `ScreenTest` "launches through the receiver" | `ScreenTest` calls the same `TestSeeds.apply` in-process, then `ActivityScenario<MainActivity>` (`13 §5.2.3`), which gives `scenario.close()`. `SeedReceiverActivityTest` covers the receiver |
| OQ-0e-8 | Plan says `ConfigurationPolicy.wrap(base)`; an override survives in-place `uiMode`/size changes more reliably | `applyOverrideConfiguration` in `MainActivity`; `wrap` kept for non-activity contexts. T10's device test decides |
| OQ-0e-9 | Committing a stored reference before approval | Only the harness probe `HarnessProbe_default_pixel8_en_light.png`; no screen reference until a person approves it |
| OQ-0e-10 | 0d generates a wire enum `Language` in `:core:model`; the plan names the UI language `Language` too | The resolved UI language is `AppLanguage { En, Nl }` in `…model.config`; a mapping to the wire enum is added when a phase needs it |
| OQ-0e-11 | `CompilationMode.Partial()` defaults to `BaselineProfileMode.Require`, but no profile exists until 1e | `UseIfAvailable` in 0e; 1e switches `StartupBenchmark` to `Require` when it commits the profiles |
| OQ-0e-12 | `cf.freezeAt` means splash ms on iOS; 1d (OQ-1d-2) also reads an ISO instant from it for the plan clock | `TestSeed` parses ms (0…60000) into `freezeAtMs` and keeps `freezeAtRaw`; 1d parses the instant itself |
| OQ-0e-13 | No profile covers 280 dp or split screen (`14 §14`: `split-narrow` 320 × 700, `landscape-short` 800 × 360) | Keep the plan's five; add them when a screen phase needs them |
| OQ-0e-14 | AGENTS says `make perf` runs "cold and warm" | Cold only in 0e, per the phase entry; warm arrives with 3b |
| OQ-0e-15 | Low-end device models for `budgets.json` | The script passes the class (`DEVICE_CLASS`); no model list |
| OQ-0e-16 | 1e's brief names `Scripts/test_check_benchmarks.py`, but 0a discovers only `Scripts/tests/test_*.py` | The test lives at `Scripts/tests/test_check_benchmarks.py`; fixtures at `Scripts/fixtures/benchmarks/` as 1e says. The 1e brief already uses this path (its revision of 2026-11-16); plan.md's Phase 0e "Results checker" bullet still says `Scripts/test_check_benchmarks.py`, so T17 corrects that path in plan.md |
| OQ-0e-17 | Gradle 9 fails a test task that has test sources but discovers no tests, which tier filtering causes | `failOnNoDiscoveredTests = false` in the convention; missing evidence is still caught by run-tier (no XML or no timing file → exit 2) |
| OQ-0e-18 | Who builds `make captures DEVICE=1`? | Phase 2 Task 18 (its brief, OQ-2-21, cites this question). 0e ships the Robolectric path and a stub that exits 1 naming Phase 2 |
| OQ-0e-19 | 1d also compiles `src/perf` into `debug`; the plan says perf variants only | 0e wires the two perf variants only; 1d adds `debug` if it still needs to |
| OQ-0e-20 | Earlier versions of later briefs drifted from 0e's names: 1d (P8, item 10) and Phase 2 (§5 item 5, Task 18) said package `net.chronoflow.core.testing.tier` / one `Tiers.kt`; Phase 2 passed `Language.En` to `ProfileRule` | 0a fixed `net.chronoflow.core.testing.tiers` (`Smoke.kt`, Makefile `SMOKE`), so 0e keeps `tiers` with one file per annotation, and `ProfileRule` takes `AppLanguage` (OQ-0e-10). Both briefs are already corrected (1d P8 and item 10 name `…testing.tiers`; Phase 2's revision of 2026-11-16 uses `tiers` and `AppLanguage`); do not add aliases |
| OQ-0e-21 | The 1 s JVM budget now fails `make quick` (T3), but 0a's Konsist classes parse the whole project in their first test, which may take more than 1 s | Exempt only a measured over-budget `:konsist` class with class-level `@PerfBudget` (a structure check over the tree, not a unit test), and record the ms. Every other JVM test keeps 1 s |
| OQ-0e-22 | Wall-clock budget tests (1b, 1c) flake under the parallel test JVMs of `make quick` (review 2026-11-16) | `@TimingGate` plus a serial `timingTest` task, run by `make timing` as the last step of `make quick` and `make test`; `@PerfBudget` stays a duration-budget exemption only |
