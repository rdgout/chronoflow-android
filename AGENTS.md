# ChronoFlow Android

ChronoFlow is a jet-lag planner: it plans sleep, light and caffeine for a trip. This repo is the native Android app (Kotlin, Jetpack Compose, Material 3). It behaves exactly like the native iOS app, follows the boards, and must outperform the React Native app. The trip timeline is the critical screen.

`plan.md` is the plan. Work one phase at a time from its brief, `docs/phases/phase-<id>-<slug>.md`, and tick each task in the commit that completes it. Follow the plan's Decisions (A1–A25), spec, Performance budget and Open questions. Every open question has a default: build on the default, and add any new question to "Open questions for the user" with its own default.

## Sources (read-only)

ALL READ-ONLY — never write to, build in, run prebuild in, or git-modify these repos.

- **iOS**, `/Users/rickgout/Sites/chronoflow-ios`, pinned at `f391a1fa` (`tools/ios-pin.txt`). This is the reference app. You **port** its `plan.md`, `Docs/`, Swift sources, String Catalogs, fixtures and tests. Read files at the pin: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`.
- **Web**, `/Users/rickgout/Sites/chronoflow-new`, pinned at `e36368ec` (`tools/web-pin.txt`). The web app and API, and the source of truth for the API contracts.
- **RN**, `/Users/rickgout/Sites/chronoflow-mobile`. The React Native app. Read it for history only; its Android lessons are in `docs/research/09-rn-android-lessons.md`.
- **Boards**, `_design/canvas/`: `<Name>.dc.html`, `canvas.json`, `ds/chronoflow/tokens.json`, `theme.css` and `icon/`. This is a local copy of a canvas the user maintains. `_design/icon/` holds the icon masters.
- **Research**, `docs/research/01–14`, with the iOS reference screenshots in `docs/research/screens/ios/`.

When the sources disagree, the order is: a user decision, then iOS at the pin, then the current boards, then the web app, then RN. See plan.md "How conflicts between sources are settled".

## Working rules

- **No design, no invention.** Behavior comes from iOS at the pin; looks come from the boards. If a screen or state has neither, stop, say so and defer it. Ported code keeps the Swift type names, and ported tests keep the Swift test names.
- **Values only from generated tokens and resources**, in light and dark. Colors, type, spacing and shapes come from `:core:designsystem` and generated files. Geometry comes from the window size and insets. Konsist and Lint enforce this.
- **Every phone, foldables and large font.**
  - The breakpoint is 600 dp and the minimum width is 280 dp.
  - `ConfigurationPolicy` caps the font scale at 1.3 at the root. Screens use that cap as it is.
  - Prove that a screen fits with the JVM device profiles and the Robolectric guards (`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`), plus the `pixel` emulator in `make check`.
  - The T2 AVDs (`fold`, `small`, `api29`) run only on request.
- **Native components.** Use Material 3 system components (app bars, navigation bar and rail, switches, sheets, dialogs, pickers, snackbars), tinted through `toM3()` and the `Cf*` wrappers. Draw designed surfaces exactly as the boards draw them.
- **English and Dutch together.** Every visible string goes into the string resources with its `nl` value, in the same commit. `Text` receives only resources.
  - Copy is generated from the iOS String Catalogs (`make strings`).
  - Android-only copy goes into `tools/l10n/android-only.json` and into plan.md "Android-only copy awaiting approval".
- **Copy** promises a clock shift, never symptom relief. The app collects no health data: age band and shift work are the only questions.
- **Secrets** (passwords, tokens, two-factor codes, link secrets) live in memory and in the Keystore-encrypted session only. They never go into a `Bundle`, `SavedStateHandle`, `rememberSaveable`, the database, the outbox or a log.
- **Comments** record only a *why* that the code cannot show.
- **Sandbox limits.** Your sandbox may not be able to run the Gradle daemon, `adb` or the emulator; Phase 0a records what works. If a command cannot run, report the command and its exact output. The orchestrator runs it.

## Performance

- **The main thread does no disk or network work.**
  - The database, DataStore, files and the Keystore run on IO dispatchers.
  - The only read before the first frame is the `device-config` file of at most 64 B, and only below API 33.
- **StrictMode is on in debug builds:** `detectAll().penaltyLog()`, plus `penaltyDeath` for disk reads on the launch path. A violation is a bug to fix.
- **`Application.onCreate` stays minimal.** It installs StrictMode in debug builds, builds the lazy `AppGraph`, registers the `ProcessLifecycleOwner` observer (A7) and starts the `launch` trace, and nothing else. App Startup keeps only `ProcessLifecycleInitializer` (A7's sync triggers) and `ProfileInstallerInitializer` (the profiles), whose cost Phase 3a measures in a startup trace. Every other initializer is removed with `tools:node="remove"`, and `verifyReleaseManifest` fails on any other (plan.md OQ-A47). WorkManager initializes on demand through `Configuration.Provider`.
- **Baseline and startup profiles** are generated by `:benchmark` and committed. Regenerate them (`make baseline-profile`) when a startup, timeline or wizard path changes.
- **Compose:**
  - Read fast-changing state (the clock, scroll offsets) in the narrowest scope, preferably inside draw or layout lambdas.
  - Lazy lists have stable keys and a `contentType`.
  - Draw code allocates nothing per frame.
- **Timeline** (plan.md A12):
  - `NowLine` is the only clock reader.
  - Day drawings are built off the main thread and replayed from `drawWithCache` in their own graphics layer.
  - It uses no `saveLayer`, no blur and no `TextMeasurer`.
  - From Phase 1e on, every timeline change runs `make perf-timeline` against plan.md "Performance budget". Phases 1b–1d predate the target: they run their JVM budgets and record their trace checks in `docs/perf-timeline.md`.
- **Record numbers, not impressions.** Each result goes into `docs/perf-startup.md` or `docs/perf-timeline.md` with its device, build type and command. Emulator numbers are never device claims.

## Tests

Write every new or changed test this way. These rules translate the iOS Tests rules (user, 2026-11-10). iOS wizard tests that ignored them cost a morning of 3–10 minute hangs and five fix rounds aimed at a harness artifact.

1. **Cheapest layer that can fail.**
   - Logic → JVM unit tests.
   - Layout and geometry → Robolectric Compose tests.
   - Emulator → only for native controls, navigation, system UI, IME, permissions and system appearance/locale.
   - Never test logic through a screen.
   - **ViewModels carry the logic; screens stay thin** (user, 2026-11-16; iOS does the same with its `*Model` tests). A screen is a stateless `XScreen(state, onEvent)`; its ViewModel takes repositories, clock and dispatchers through its constructor. Test behaviour as JVM tests on the ViewModel's UI state (`runTest`, fake clock, fake repositories), never through Compose.
   - **Never assert copy on a screen.** No test checks that a string appears on screen (no `onNodeWithText`, `hasText`, `assertTextEquals`). Copy is covered by the string generator tests (en/nl parity) and the board captures. Screen tests find nodes by test tag and check structure only.
2. **One emulator journey per feature, at most.** Every other emulator test seeds a state and checks one screen.
3. **Emulator tests launch seeded** (`docs/research/13-testing-tooling.md` §5). Never sign in through the form or click through earlier steps. Only the auth tests type credentials.
4. **Exact queries and waits.**
   - Use `onNodeWithTag("sign-in.email")` (plus `useUnmergedTree = true` only when needed) or `onNode(hasTestTag(..) and hasClickAction())`.
   - No `onAllNodes(isRoot())`/`printToLog` traversals, no loops over `fetchSemanticsNodes()` to find something, and no text-prefix matching unless the content is genuinely variable.
   - Wait with `composeTestRule.waitUntil(timeoutMillis) {…}` / `waitUntilExactlyOneExists(…)`. For UiAutomator, use `device.wait(Until.hasObject(By.res(..)), t)`.
   - **Never `Thread.sleep` / `SystemClock.sleep`.**
5. **Budgets.**
   - An emulator test < 10 s; a Robolectric test < 5 s; a JVM unit test < 1 s (except the declared perf-budget tests).
   - Hang caps (below): 60 s per test.
   - Split profile × language × size loops into one test per profile (JUnit `Parameterized` or `TestParameterInjector` is fine: each parameter is its own test case).
   - Needing more time is a design smell; fix the test.
6. **Know what the harness can show.**
   - Robolectric does not lay out real system bars, cutouts, IME or hinge events. Insets come only from the fixture provider (`profiles.json`), and IME behaviour belongs on the emulator.
   - When a check cannot fail for the real reason, delete it and capture one seeded emulator screenshot instead.
7. **Running tests.**
   - Fixing a failure → run only that method (`make targeted ONLY=…#method`).
   - Each fix round runs only what it touched.
   - Run `make quick` once before pushing, not every round.
   - Read the suspect diff before bisecting.
   - **After one fix round that changes nothing, or a hang, stop and rethink.**
8. **Transient UI** (snackbars, the 2 s "Up to date" capsule): assert through a Debug-only "was presented" counter (iOS I13), not by racing the animation.
9. **Missing evidence is failure.** A tier that produced no result file, or a benchmark without its metric, fails.

How the harness works:
- **Seeds** use the iOS `CF_*` names, sent as intent extras to the debug-only `SeedReceiverActivity` (plan.md "Android spec: seeds and test hooks"). Profile `screen` skips the splash and readiness; profile `launch` runs them for real.
- **Test tags** are the iOS dotted ids, such as `sign-in.email`, exposed as resource ids at the root.
- **Hang caps:** `:core:testing-junit` TestTimingListener enforces 60 s per JVM/Robolectric test (`cf.testHangCapSeconds`), with 3× for the first Robolectric test of each SDK in a fork (180 s by default) for cold sandbox resolution and disabled under JDWP; stacks and the unique id go to stderr and `build/cf-timings/<task>/hang-*.txt` before the worker halts; the timing tier keeps its own budgets.
  - coroutine tests use `runTest(timeout = 60.seconds)`;
  - instrumented tests run with `timeout_msec=60000`;
  - `Scripts/run-tier.sh` stops a tier after 600 s without output, or after 3,600 s in total.
- **Snapshots** are reference-free guards by default. Stored references are opt-in (`SNAPSHOT_RECORD=1 make snapshots`). A JVM render is only ever compared with another JVM render.
- **Results** land in `build/test-results/<tier>/`, with a `timings.json` per run.

## Commands

- Set `export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`.
- Gradle runs on JDK 17 (Zulu 17.0.21). Test tasks use a JDK 21 toolchain, which the foojay resolver provisions.
- Always use `./gradlew`.
- A target whose phase has not landed yet prints the phase that adds it and exits with status 1.

| Command | What it does |
|---|---|
| `make generate` | Regenerates every committed generated output: tokens, strings, plan copy, contracts and icons. Raster assets are excluded. |
| `make quick` | The inner loop: Spotless, Konsist, every JVM test of `:core:*` and `:tools:*`, the Robolectric logic tier (including the timeline invalidation guard), the tooling tests, then the serial timing tier (`make timing`). Under 60 s warm. |
| `make timing` | The wall-clock budget tests (`@TimingGate`: `TimelinePreparationBudgetTest`, `DayDrawingBudgetTest`), in one test JVM on one Gradle worker, after every other test task. `make quick` and `make test` run it last. |
| `make check` | The commit gate: `quick`, Lint with warnings as errors, `testReleaseUnitTest`, `verifyReleaseManifest`, the R8 configuration analysis, `generators-check`, the Robolectric layout tier, and the `@Smoke` emulator set on `pixel`. Under 5 min warm, with `pixel` booted. |
| `make format` / `make lint` | `spotlessApply` / Spotless, Lint and Konsist. |
| `make test` | Every JVM and Robolectric test, without the emulator. |
| `make targeted ONLY="<module>:<Class>[#method] …" [REUSE_BUILD=1]` | Runs only these tests: JVM and Robolectric tests through `--tests`, emulator tests through the runner's `class` argument. `REUSE_BUILD=1` runs `am instrument` against the installed APKs. |
| `make snapshots` / `SNAPSHOT_RECORD=1 make snapshots` | Compares or records the opt-in stored Roborazzi references. |
| `make captures [ONLY=<screens>] [DEVICE=1]` | Board captures into `build/captures/`, one PNG per board state, named after the board. Runs on Robolectric, or on the emulator with `DEVICE=1`. |
| `make perf` | `StartupBenchmark` (cold and warm, `CompilationMode.Partial`), then `Scripts/check-benchmarks.py`. Sets the animator scales to 1 and restores them afterwards. |
| `make perf-timeline` | `TimelineBenchmark` (first frame; 15 flings × 3 iterations on large60; last day), then `check-benchmarks.py`. Required for every timeline change. |
| `make baseline-profile` | Regenerates the committed baseline and startup profiles. |
| `make tokens` / `make fonts` / `make assets` | Generates the tokens / copies the fonts and checks their hash manifest / rasterizes the launch moon and builds the icons. |
| `make strings` / `make plan-copy` / `make contracts` | Generates the en and nl strings from the iOS catalogs / copies `plan_copy.json` and checks its hash / generates the Kotlin contracts. |
| `make generators-check` | Regenerates into `build/`, diffs against every committed output, and runs the generator tests. |
| `make size` | Release bundle size for arm64, xxhdpi and en. Warns above 8 MB; fails above 12 MB or on any `.so` file except the OQ-0a-22 allow-list (plan.md). |
| `make emu` / `make emu-down` | Boots the headless `pixel` emulator with the functional device prep / stops every running emulator. |
| `make emu-clean` | Wipes the `pixel` data. Refuses while tests run. |
| `make avd-create AVD=fold\|small\|api29` | Creates a T2 AVD. On request only. |
| `make matrix` | On request only: the `@Smoke` set on `fold`, `small` and `api29`. |
| `make du` | Prints `du -sh ~/.gradle/caches ~/.android/avd build` and the size of each worktree. |
| `Scripts/ci.sh` | Runs `make generate check`, then fails if any generated file changed (`git diff --exit-code`). |

## Git

- **One branch per phase:** `feat/phase-<id>-<slug>`. Branches are stacked:
  - A phase starts from the stack top once every phase it depends on is in the stack.
  - Parallel phases run in their own worktrees. They are rebased onto the stack top in the order they finish (`git rebase --onto`).
  - History stays linear, with no merge commits.
- `main` holds only the first docs commit. Merge, push or force-push only when the user asks; there is no remote yet.
- **Commits** are atomic and follow Conventional Commits: `type(scope): subject`. The type is one of build, chore, ci, docs, feat, fix, perf, refactor, revert, style or test, and the subject is lower-case and imperative. A message ends with its subject or body: no `Co-Authored-By` and no other trailer, whatever a tool suggests.
- Commit generated files together with the generator change that produced them.

## Traps

- **Splash keep condition.** Release it from the launch overlay's `onPlaced` callback (`overlayPlaced`). The system splash holds back drawing until the condition is false, so a flag set while drawing never flips, and the app hangs on the splash.
- **`native` is a Java keyword.** It appears only in the `applicationId`, `net.chronoflow.app.native`. Namespaces and packages are `net.chronoflow.app`, `net.chronoflow.core.<module>` and `net.chronoflow.feature.<module>`. Each module's namespace must be distinct, because `android.uniquePackageNames` is on.
- **AGP 9 defaults stay on.** AGP's built-in Kotlin compiles Kotlin:
  - never apply `org.jetbrains.kotlin.android`;
  - never set `android.builtInKotlin=false` or `android.newDsl=false` (the RN app's opt-outs);
  - use `androidComponents { onVariants { } }` for variant work.
- **Set `targetSdk = 36` explicitly** in every convention plugin. Left unset, AGP 9 sets it to compileSdk, which is 37.
- **`testReleaseUnitTest` exists** only because `gradle.properties` sets `android.onlyEnableUnitTestForTheTestedBuildType=false`. Release-readiness tests run in that task.
- **Robolectric** needs the JDK 21 test toolchain. A JUnit4 `Timeout` rule moves the test off Robolectric's main looper and breaks it; rely on the hang caps instead.
- **Announcements** use `semantics { liveRegion = LiveRegionMode.Polite }` or a pane title. `announceForAccessibility` is deprecated from API 36.
- **`toM3()` maps `primary` to `dawnText`** and `tertiary` to `dawn`. A system control that shows the dawn fill (switch track, progress bar, pull to refresh) takes its colors from its `Cf*` wrapper.
- **Rethrow `CancellationException`.** `runCatching` and `catch (e: Exception)` swallow it; catch the specific exception types instead.
- **Measured sizes stay in layout.** Compute from constraints in a layout modifier. A size written into state from `onSizeChanged` or `onGloballyPositioned` costs an extra frame and can loop.
- **`_design/` is tracked** from the first docs commit, so every worktree has the boards.
- **Animator scales.** Functional emulator tests run with the scales at 0 (`make emu`). Perf runs need them at 1; `make perf` and `make perf-timeline` set them and restore them. A perf run with the scales at 0 measures nothing.
- **The `pixel` AVD** is wiped only through `make emu-clean`, which refuses while tests run.
- **Release builds ship no seed or mock code.** Debug code lives in `src/debug`, and perf-only code in `src/perf`. `testReleaseUnitTest` proves both are absent.
- **The configuration cache fails on problems.** Read environment variables and Gradle properties through `providers`.
