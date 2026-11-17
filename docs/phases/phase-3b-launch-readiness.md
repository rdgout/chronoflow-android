# Phase 3b: Readiness reducer, endings and process policy

- **Branch:** `feat/phase-3b-launch-readiness`, from the stack top once Phase 3a is in it (3a brings 0b, 0c, 0e and 2 with it). If 3a's `LaunchController`, `LaunchOverlay`, `LaunchTimeline` (with `set`/`replace`/`append`/`withoutExit` and replica sampling), `LaunchFrameSampler`, `ManualLaunchClock`, `LaunchTrace` or `LaunchOverrides` is missing, stop and report it.
- **Plan entry:** `plan.md` "Phase 3b: Readiness reducer, endings and process policy" (l.1491–1539). Decisions **A4**, **A8** (process death), **A11**, **A19**, **A21**; inherited **D5**, **D12**, **D14**, **OQ 7**, **OQ 16** (table "Inherited iOS decisions"); "Android spec: process, activity and launch sequence" (l.321–346), steps 4–7; "Android spec: seeds and test hooks"; "Performance budget" rows tagged 3b; `AGENTS.md` Traps.
- **Tests:** every test follows `AGENTS.md` "Tests": cheapest tier, budgets, no `Thread.sleep`/`SystemClock.sleep` in tests, hang caps (`runTest(timeout = 60.seconds)`). Read that section before writing a test.
- **Tags:** **[F]** fact verified in source (path given). **[R]** rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`.
- `…/launch/` stands for `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/`; `…/model/` for `…/launch/model/` (pure Kotlin, Konsist forbids `android.*`/`androidx.*` there); `…/app/` for `app/src/main/kotlin/net/chronoflow/app/`; `…/perf/` for `app/src/perf/kotlin/net/chronoflow/app/perf/` (compiled into `debug`, `benchmarkRelease` and `nonMinifiedRelease`, exactly as 1d did for `TripPageEntry`; follow `docs/tooling.md`); `…/rel/` for `app/src/releaseNoPerf/kotlin/net/chronoflow/app/perf/` (the release-only twins; `app/build.gradle.kts` adds this directory only to `release`, because `src/release` also compiles into the perf variants).

---

## 1. Goal

After 3a the overlay plays the opening and then holds the 1500 ms frame forever. This phase makes it **end**, exactly like iOS:

- a pure **readiness reducer** (port of `Kit/AppCore/LaunchReadiness.swift`) owns the rules 1500 / 1600 / 2000 / 8000 ms and "never cut short", driven by a **coordinator** (port of `LaunchCoordinator.swift`) on the injectable `LaunchClock`;
- the **endings** (port of `LaunchTimeline+Ready.swift`): the ready exit (fade + 8% scale-up, ending at **1900 ms** when ready comes before 1500), the slow **status line with three pulsing dots** at 2000 ms, and the **forced exit at 8 s** (unknown session → signed out);
- a **stub readiness seam** (port of `App/Readiness/StubReadiness.swift`), driven by debug and perf intent extras, until 4e brings the real session;
- the **process policy**: the overlay plays once per process (D14), a warm start gets a **300 ms** fade of the system splash, an activity recreated mid-overlay resumes it at its wall-clock position, and a **process-death restore is a cold launch** (A8, iOS OQ 16);
- traces `ready` and `exit`, `reportFullyDrawn()` at the hand-off, `StartupBenchmark` WARM, and a **main-thread-stall test** proving the overlay follows the wall clock.

Done means: all ported iOS cases pass on the JVM; on the `pixel` emulator a ready launch hands off at 1900 ms ± 1 frame, a slow launch shows the status and hands off at 3900 ms ± 1 frame, a 500 ms stall skips frames without stretching, a warm start shows no overlay, and a killed process relaunches with the splash; warm TTID is recorded.

## 2. Inputs to read (in this order)

1. `plan.md`: A8, A11, A21; "Inherited iOS decisions" (D5, D12, D14, OQ 7, OQ 16); "Android spec: process, activity and launch sequence"; "Android spec: seeds and test hooks"; "Performance budget"; Phase 3a (what you build on), 3b (this), 3c and 4e (what consumes your API).
2. `AGENTS.md`: "Performance", "Tests", "Traps" (splash keep condition; animator scales; release ships no seed code).
3. `docs/phases/phase-3a-launch-overlay.md` **in full** (the shapes you extend: §5.2 layer 9 status, §5.8 clock, §6 Kotlin shape, Task 6 `LaunchController`, Task 7 activity order) and what 3a actually landed (`git log --oneline`, the files under `…/launch/`, `docs/perf-startup.md`, plan.md OQ-A48+; the 2026-11-16 review took OQ-A44–A47).
4. Research:
   - `docs/research/05-launch-auth.md` §3.4 (endings), §3.5 (readiness rules), §3.6 (Reduce Motion), §3.7, §3.8 (copy), §15.1 (warm start item 5), §15.2;
   - `docs/research/14-gaps.md` §3.1–§3.4 (process death vs config change, survival table);
   - `docs/research/02-ios-architecture.md` §4.5 (coordinator and readiness), §5.5 (launch arguments).
5. Boards (read-only):
   - `DC/SplashSlow.dc.html`: a wrapper `<dc-import name="SplashScreen" mode="slow">`.
   - `DC/SplashScreen.dc.html`: mode `slow` (`dur 7000`: status `28.57%→32.86%` = 2000→2300 ms; out `65.71%→71.43%` = 4600→5000 ms), the `status` style (`top: 488px` → superseded by S1, see §5.5; `font-size: 14px; line-height: 20px; color: var(--sky-title)`), dots (`width: 4px; height: 4px; border-radius: 9999px; background: currentColor`, `gap: 3px; margin-left: 6px`, `cfsp-dot 1200ms ease-in-out` with delays 0/200/400), and mode `ready` out keyframes (board 1700–2100, which S3 maps to native 1500–1900).
   - `DC/SplashSpec.dc.html` (context only): key frames "1900 ms · Hand-off", "2600 ms · Still loading", "4900 ms · Ready"; "Never cut the animation short"; "Hard limit. At 8 s the app opens anyway"; "Reduce Motion … cross-fades into the app in 300 ms. The status line still appears on a slow first load".
6. iOS references (`I/` at `f391a1fa`):
   - `plan.md:767` "Phase 4: Readiness and hand-off (`feat/phase-4-readiness`)" — its "Tests:" line is the case list you port.
   - `plan.md:909` "Phase 4 review fixes (after Phase 5)" — `stop()` cancels timers before any advance; stall and `-cf.mode ready` kept for the device proof.
   - `plan.md:221` "Endings", `plan.md:267` "Readiness rules (RN Phase 21, with the RN bug fixed)", `plan.md:277` "Reduce Motion", `plan.md:289` "Accessibility".
   - `plan.md:25` "Decisions": D5, D12, D14 (and D3/D4 for the clock and one-track-per-key-path).
   - `plan.md:2376` "Open questions for the user": OQ 6 (Reduce Motion dots hold at 1.0), OQ 7 (late signed out starts at resolution; unknown at 8 s → signed out), OQ 8 (debug-only route-name placeholders), OQ 16 (cold start after a kill always splashes).
   - `Kit/AppCore/LaunchReadiness.swift` (whole file, 187 lines), `Kit/AppCore/LaunchCoordinator.swift` (92 lines), `Kit/Launch/LaunchTimeline+Ready.swift` (32 lines), `I/App/Readiness/StubReadiness.swift` (74 lines), `I/App/LaunchArguments.swift` (parsing rules), `Kit/AppCore/PendingLaunchIntent.swift` l.38–48 (`LaunchProcessState.claimOverlay`), `Kit/AppCore/LaunchSignposts.swift`, `Kit/AppCore/StoredDataReadiness.swift` l.200 (`SignedOutReadiness`).
   - Wiring reference (do not port UIKit, only the behaviour): `I/App/SceneDelegate.swift` l.78–175 (`connect`: signposts begin, stall at 300 ms), l.326–474 (`startReadiness`, `runReadiness`: `session`/`ready`/`exit` signposts, route, `controller.update`), l.622–648 (`finishLaunch`), l.716–732 (`sceneDidDisconnect`); `Kit/Launch/LaunchOverlayController.swift` (rebuild the timeline from `opening` on every commit effect; status announced once); `Kit/Launch/StatusLineLayer.swift` (dot 4 × 4, pitch 7, centred on the line); `I/App/Debug/RoutePlaceholder.swift` (`route.<name>` id, text "Debug route: %@").
   - Tests to port: `I/Packages/ChronoFlowKit/Tests/AppCoreTests/LaunchReadinessTests.swift`, `…/AppCoreTests/LaunchCoordinatorTests.swift`, `…/AppCoreTests/LaunchLifecycleTests.swift` (`firstConnectionIsProcessScoped` only), `…/LaunchTests/ReadyTimelineTests.swift`, `I/AppTests/StubReadinessTests.swift`, `I/AppTests/LaunchArgumentsTests.swift` (`testDefaultsAndInvalidValues`, `testModeTimelinesAndAsymmetricSafeAreaFallback` without the welcome case).
7. What earlier phases give you (verify before Task 1):
   - 0c: `R.string.launch_getting_your_plan_ready` ("Getting your plan ready" / "Je plan wordt klaargezet"), `R.string.launch_chronoflow_is_opening`, and the App-table key `"Debug route: %@"` (en "Debug route: %@", nl "Debugroute: %@"). It is **debug-only**: generated as `net.chronoflow.core.ui.R.string.app_debug_route_x` (`%1$s`) in `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml`, absent from release and from `CopyIndex` [F `phase-0c-strings.md` l.90, l.173, OQ-0c-4]. Only code in `app/src/debug` (or `core/ui/src/debug`) may reference it; `src/main` code cannot (§5.8).
   - 0e: `TestSeed` (`cf.reduceMotion` accepts `1/true/0/false`; `cf.freezeAt`, `cf.readyAt`, `cf.mainThreadStall` as ms `0.0..60000.0`; `raw` holds every accepted key), `TestSeeds`, `ScreenTest` (`launch(overrides: Map<String, String>)`, `waitFor`, `launchWithoutIdle`), `UITestTimeline`, `ReleaseReadinessTest`, `StartupBenchmark`, `benchmark/budgets.json`, `Scripts/check-benchmarks.py`. Check that `TestSeed.raw`/`TestSeeds.forwardIntent` forward the new `cf.session`, `cf.gate`, `cf.stored`, `cf.readyDelay`, `cf.mode` keys to `MainActivity`; if 0e drops unknown `cf.*` keys, add them to its accepted list with a `TestSeedTest` case in Task 5.
   - 2: `ChronoFlowTheme`, `CfColors` (`skyTop`, `skyTitle`, `background`), `CfSky`, the `status` type style (`CfType.status` = Figtree 400 with `TypeMetrics.body`, 14 / 20, iOS `Typography.status`), `R.font.figtree_400`, `warmFonts()`.
   - 3a: everything in its §6 and Tasks 6–7, with the landed names/signatures: `claimOverlay(overrides: LaunchOverrides)`, `t0Nanos`, `markOverlayPlaced()`, `onActivityDestroyed()`, and `overlayShowing`. `AppGraph.launch` delegates to the variant `LaunchPlatform.controller`; `AppGraph.resetLaunchForTest()` is a debug-only extension in `app/src/debug/kotlin/net/chronoflow/app/LaunchPlatform.kt`; construction is in `app/src/main/kotlin/net/chronoflow/app/LaunchDependency.kt`. The composition counter is the injected `LaunchController.compositionObserver` callback (no `LaunchProbe` type). `LaunchTrace` delegates to the pure `LaunchTraceSession` through `LaunchTracing` in `LaunchTracing.kt`; extend this existing seam.

**API endpoints:** none. This phase does no network and no disk I/O (the stub reads nothing; the only file-system access is the status-font load on `Dispatchers.IO`, §5.5).

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| `withWelcomeExit` (band, sun/halo, path, wordmark, moon flight), `MoonTarget.fallback`, `MoonAnchorStore`, `LaunchEvent.MoonAnchor` producers, Welcome, intro, `intro` trace, TalkBack focus to Welcome after teardown, `reportFullyDrawn()` at "Welcome interactive" | 3c |
| `StoredDataReadiness`, `SessionStore`, `session` trace, `Route`/`RootNavigator`/Nav3, `PendingLaunchIntent` (and its field in `LaunchState`), `CF_AUTH_TEST_SIGNED_IN` and other account seeds, `SessionGateRouter` | 4e |
| `reportFullyDrawn()` after the first Today content frame | 7 |
| Physical-device stall and hand-off proof, perf gates on devices | 13 |
| A `RenderThread` moon that keeps animating through a stall (iOS no-hitch-under-stall) | not required (OQ-A19) |

There is **no board** for the route placeholders, so they render no designed UI (§5.8). Do not build Today, Welcome or any loading/skeleton screen.

## 4. Platform facts this phase relies on [F unless marked]

- **Splash on warm starts.** The Android 12+ system splash shows on cold **and** warm starts (activity created while the process lives), not on hot starts and not on configuration recreation. `setOnExitAnimationListener` is called only when a splash was shown; `SplashScreenViewProvider.view` can be animated before `remove()`. (`05 §15.1`)
- **Process death restore:** the new process creates the activity with the old `savedInstanceState`. `ComponentActivity.onCreate(bundle)` restores the `SavedStateRegistry` (Compose `rememberSaveable`, Nav3 back stacks) from that bundle; `super.onCreate(null)` restores nothing. (`14 §3.1–3.2`)
- **Frame clock:** Compose frames only **pace** the loop. Elapsed time is always `controller.nanoTime() − t0`, on the same injected clock that set `t0` (3a as landed: `LaunchFrameLoop.kt` `runLaunchOpening`; 3a commit 90d9ea7 fixed a hang caused by subtracting a `withFrameNanos` timestamp from a `nanoTime()` `t0`, and `LaunchFrameLoopTests` uses different bases for the two clocks). A stalled main thread produces no frames; the next frame reads the wall clock, so the stalled part is skipped. Frames stop while the activity is stopped. **Never subtract a frame timestamp from `t0`.**
- **Animator scale:** functional emulator runs set the scales to 0, which the app reads as Reduce Motion (3a §5.5). Timing tests that need the animated path seed `cf.reduceMotion=false`. `ViewPropertyAnimator` durations follow the scale (0 → the end action runs at once).
- **`ReportDrawnWhen`** (activity-compose 1.13) calls `reportFullyDrawn()` on the frame after its predicate turns true; `StartupTimingMetric` then reports `timeToFullDisplayMs`.
- **Instrumentation runs in the app process**, so a test that kills the app process must live in another APK (`:benchmark`, `com.android.test`).

## 5. The spec

All times are ms from `t0` (3a: the overlay's first draw). `rm` = the captured Reduce Motion value (3a §5.5).

### 5.1 Seams [F `Kit/AppCore/LaunchReadiness.swift:1–55`; R module split]

`:core:model`, package `net.chronoflow.core.model.launch` (4e's `StoredDataReadiness` in `:core:sync` implements it, so it cannot live in a feature module; 4e T1 may move `SessionPresence`/`Gate` into `net.chronoflow.core.model.session`, keeping names and signatures):

```kotlin
enum class LaunchEnding { Welcome, App }
sealed interface SessionPresence { data class SignedIn(val accountId: String) : SessionPresence; data object SignedOut : SessionPresence }
enum class Gate(val rawValue: String) { Verify("verify"), Terms("terms"), Walkthrough("walkthrough"), App("app") }
interface LaunchReadiness {
    suspend fun session(): SessionPresence
    suspend fun gate(): Gate
    fun gateUpdates(): Flow<Gate> = flow { emit(gate()) }            // iOS default: one value, then finish
    suspend fun hasStoredData(): Boolean
    fun storedDataUpdates(): Flow<Boolean> = flow { emit(hasStoredData()) }
    suspend fun allowFirstLoad()
}
object SignedOutReadiness : LaunchReadiness { /* session = SignedOut, gate = App, hasStoredData = true, allowFirstLoad no-op */ }
```

[F `Kit/AppCore/StoredDataReadiness.swift:200–206`: iOS `SignedOutReadiness.hasStoredData()` returns **true**; 4e's brief (§5, "`SignedOutReadiness` (perf)") repeats it.]

`kotlinx-coroutines-core` is allowed in `:core:model` only if 0a's Konsist rules allow it; otherwise put these five types in `:core:sync` (plan spec row "Core seams") and record the choice.

### 5.2 Reducer [F `LaunchReadiness.swift:57–187`, port line for line]

`…/model/LaunchReadinessReducer.kt`. Immutable state, functional step [R] (iOS mutates `inout`; the logic must stay identical):

```kotlin
enum class LaunchRoute { Welcome, Today, Verify, Terms, Walkthrough }   // the iOS Route cases LaunchState.route can yield; 4e maps them onto Route
fun Gate.launchRoute(): LaunchRoute                                     // verify→Verify, terms→Terms, walkthrough→Walkthrough, app→Today

data class LaunchState(
    val ending: LaunchEnding? = null, val session: SessionPresence? = null, val gate: Gate? = null,
    val hasStoredData: Boolean? = null, val isReady: Boolean = false, val showsStatus: Boolean = false,
    val statusStart: Duration? = null, val exitStart: Duration? = null, val moonTarget: LaunchRect? = null,
    val now: Duration = Duration.ZERO, val didForceReady: Boolean = false, val didCommitExit: Boolean = false,
    val isComplete: Boolean = false, val fallbackTarget: LaunchRect = LaunchRect(0.0, 0.0, 0.0, 0.0),
) {
    val route: LaunchRoute?            // SignedOut → Welcome; SignedIn → (gate ?: if (didForceReady) Gate.App else null)?.launchRoute(); null → null
    val welcomeDeadline: Duration?     // ending == Welcome ? exitStart?.let { maxOf(ZERO, it − 50.ms) } : null
}
sealed interface LaunchEvent { data class Session(val presence: SessionPresence) : LaunchEvent; data class GateKnown(val gate: Gate) : LaunchEvent;
    data class StoredData(val stored: Boolean) : LaunchEvent; data class MoonAnchor(val rect: LaunchRect) : LaunchEvent;
    data class Reached(val time: Duration) : LaunchEvent; data object ExitCompleted : LaunchEvent }
sealed interface LaunchEffect { data class CommitStatus(val at: Duration) : LaunchEffect; data object CancelStatus : LaunchEffect;
    data class CommitExit(val ending: LaunchEnding, val at: Duration, val moonTarget: LaunchRect?) : LaunchEffect; data object ForceReady : LaunchEffect }
data class LaunchStep(val state: LaunchState, val effects: List<LaunchEffect>)
fun reduce(state: LaunchState, event: LaunchEvent, reduceMotion: Boolean): LaunchStep
```

Event names follow the Swift cases (`session`, `storedData`, `moonAnchor`, `reached`, `exitCompleted`) except `GateKnown` [R]: a nested `LaunchEvent.Gate` would shadow the `Gate` enum.

Rules, in the iOS order (each line is one iOS statement):
1. `isComplete` → no change, no effects.
2. `Session(p)`: only if `session == null`: set `session = p`, `ending = (p == SignedOut) ? Welcome : App`; if signed out, `exitStart = rm ? now : max(1600 ms, now)`.
3. `GateKnown(g)`: `gate = g` unless `didForceReady`. `StoredData(s)`: `hasStoredData = s` unless `didForceReady`. `Reached(t)`: `now = max(now, t)`.
4. `MoonAnchor(r)`: accepted only if `r.width > 0 && r.height > 0`, all four values finite, and (`exitStart == null || now < exitStart`); `anchorChanged = moonTarget != r`; `moonTarget = r`. (iOS also checks `!rect.isNull && !rect.isInfinite` [F l.127]; `CGRect.null` has an infinite origin, so the finiteness check covers it; `CGRect.infinite` is a CoreGraphics sentinel with no `LaunchRect` equivalent and is dropped [R].)
5. `ExitCompleted`: `isComplete = true`, return no effects.
6. `if (statusStart != null && now >= statusStart) showsStatus = true`.
7. `if (now >= 8 s && !isReady)`: `didForceReady = true`; if `session == null` → `session = SignedOut`, `ending = Welcome`, `exitStart = rm ? now : max(1600 ms, now)`; `isReady = true`; if signed in → effect `ForceReady`.
8. `isReady = isReady || session == SignedOut || (session != null && gate != null && (gate != App || hasStoredData == true))`.
9. `if (isReady && !showsStatus && statusStart != null)` → `statusStart = null`, effect `CancelStatus`.
10. `if (!isReady && hasStoredData == false && session != null && statusStart == null)` → `start = max(2 s, now)`, `statusStart = start`, `showsStatus = now >= start`, effect `CommitStatus(start)`.
11. If `isReady` and `ending != null`:
    - `App`: if `!didCommitExit` → `start = rm ? now : max(1500 ms, now)`; `exitStart = start`; `didCommitExit = true`; effect `CommitExit(App, start, null)`.
    - `Welcome`: `start = exitStart!!`; `fallbackDue = now >= max(0, start − 50 ms)`; if `!didCommitExit || (anchorChanged && now < start)`, and `rm || moonTarget != null || fallbackDue` → `didCommitExit = true`, effect `CommitExit(Welcome, start, moonTarget ?: fallbackTarget)`.

Effects are returned in the order they are appended. 3b never sends `MoonAnchor` (3c does) and keeps `fallbackTarget` at the zero rect (3c sets `MoonTarget.fallback`).

**No `Failure` event** [F `LaunchReadiness.swift:92–99`: the iOS `LaunchEvent` has exactly six cases, none a failure; R OQ-3b-15]. plan.md's task line lists "failure" among the events; do not add one. A readiness source that fails simply never answers (or answers `SignedOut`), and the 8 s rule (step 7) ends the launch: unknown session → signed out, signed in → `ForceReady`.

### 5.3 Coordinator [F `LaunchCoordinator.swift`; R Kotlin form]

`…/model/LaunchCoordinator.kt`:

```kotlin
class LaunchCoordinator(
    val readiness: LaunchReadiness, val clock: LaunchClock, private val reduceMotion: Boolean,
    initialState: LaunchState = LaunchState(), private val scope: CoroutineScope,   // Main.immediate in production
    private val onChange: (LaunchState, List<LaunchEffect>) -> Unit,
) { var state: LaunchState; private set
    fun start(); fun send(event: LaunchEvent); fun stop() }
```

- `start()` (idempotent; no-op after `stop()`): launch the **session job**: `val s = readiness.session()`; return if cancelled, stopped or `state.didForceReady`; `send(Session(s))`; if signed in, launch two child jobs collecting `readiness.gateUpdates()` → `send(GateKnown)` and `readiness.storedDataUpdates()` → `send(StoredData)`. Launch one job per deadline **2 s and 8 s**: `clock.sleepUntil(d); send(Reached(clock.now))`.
- `send(e)`: return if stopped; `previous = state`; reduce `Reached(clock.now)` first, then `e` (concatenate effects); call `onChange(state, effects)` if `previous != state || effects.isNotEmpty()`; on `ForceReady` launch `readiness.allowFirstLoad()`; if `welcomeDeadline` changed, is non-null and `!didCommitExit` → cancel the old deadline job and launch `clock.sleepUntil(deadline); send(Reached(clock.now))`.
- `stop()`: set stopped, cancel every job (deadline job included). Rethrow `CancellationException`; never `runCatching` around a suspend call.
- Signed out therefore registers **three** sleeps (2 s, 8 s, welcome deadline 1550 ms); signed in **two**.

### 5.4 Endings [F `LaunchTimeline+Ready.swift`, `I/plan.md:221–241`]

`…/model/LaunchTimelineReady.kt` (extension functions; they use 3a's internal `set`/`replace`/`append`/`withoutExit`):

- `fun LaunchTimeline.withReadyExit(start: Duration): LaunchTimeline` — `t = withoutExit()`; `exit = max(rm ? 0.0 : 1.5, start seconds)`; append container `opacity` → 0 from `exit` to `exit + (rm ? 0.3 : 0.4)`, `easeOut`; unless rm, append container `transform.scale` → 1.08 from `exit` to `exit + 0.4`, `easeOut`. Scale origin is 3a's `TransformOrigin(0.5f, 0.4f)`.
- `fun LaunchTimeline.withStatus(start: Duration): LaunchTimeline` — `time = max(0, start seconds)`; `set(status, opacity, times = time == 0 ? [0, 0.3] : [0, time, time + 0.3], scalars = time == 0 ? [0, 1] : [0, 0, 1], curves = time == 0 ? [easeOut] : [linear, easeOut])`; `replace(Track(statusDot, opacity, keyTimes = rm ? [0] : [0, 0.48, 0.96, 1.2], values = rm ? [1.0] : [0.25, 1, 0.25, 0.25], curves = rm ? [] : [easeInOut, easeInOut, linear], repeatInterval = rm ? null : 1.2, replicaCount = 3, replicaDelay = rm ? 0 : 0.2))`. The dots run from **mount** (t = 0), as on the board; only the status opacity waits.
- **Interim welcome ending [R, OQ-3b-1]** `…/model/LaunchTimelineWelcomeInterim.kt`: `internal fun LaunchTimeline.withWelcomeHandOff(start: Duration): LaunchTimeline` — if rm → `withReadyExit(start)` (as iOS `withWelcomeExit` does under rm); else `t = withoutExit()`; `exit = max(1.6, start seconds)`; append container `opacity` → 0 from `exit + 0.7` to `exit + 0.95`, `easeOut`. This is exactly the container row of iOS `withWelcomeExit`; 3c deletes this file and adds the other rows.
- `…/model/LaunchHandOffTimeline.kt` (pure; mirrors `LaunchOverlayController.update`):
  ```kotlin
  object LaunchHandOffTimeline {
      fun build(opening: LaunchTimeline, state: LaunchState): LaunchTimeline   // opening → withStatus(statusStart) if set → exit if didCommitExit
      fun exitEndSeconds(t: LaunchTimeline): Double?                          // last key time of container opacity when its last value is 0, else null
  }
  ```
  `build`: start from `opening` every time (D4: one track per key path, rebuilt, never stacked); `statusStart?.let { withStatus(it) }`; if `didCommitExit && exitStart != null && ending != null` → `App` → `withReadyExit(exitStart)`, `Welcome` → `withWelcomeHandOff(exitStart)` (3c: `withWelcomeExit(exitStart, size, moonTarget ?: fallbackTarget)`).
- Hand-off end times that follow (ms): ready known ≤ 1500 → exit 1500–**1900**; ready at R > 1500 → R → R + 400; rm → R → R + 300; signed out (interim) → W = max(1600, known) → removal **W + 950** (2550 when known early); forced at 8 s: signed in → 8000–8400; unknown session → welcome W = 8000 → 8950 (rm 8300).

### 5.5 Slow status line [F `I/plan.md:229`, `StatusLineLayer.swift`, `DC/SplashScreen.dc.html` `status`/`dot`; R drawing]

- Shown only when the reducer commits `CommitStatus(at)` (signed in, `hasStoredData == false`, not ready); opacity 0 → 1 over `at … at + 300` ms, latched until the hand-off ends; cancelled (`CancelStatus`) only if ready arrives before `at`.
- **Geometry** (dp): the status rect from 3a `LaunchLayout.status` = `(0, cy + 96, w, 20)` (S1 moved the board's `top: 488` with the moon). Line height `L = L₀ × f` (`L₀` = 20, see Text) where `f` is the configuration font scale (already capped at 1.3 by `ConfigurationPolicy`; OQ-3a-11). Text box and dots from 3a `StatusLineLayout(width = w, textWidth, ascent, descent, lineHeight = L, scale = density)`: `text = (left, (L − ascent − descent)/2, rounded, L)`, `dots = (left + rounded + 6, 0, 18, L)`, `left = (w − rounded − 6 − 18)/2`; all relative to the status rect top.
- **Dots:** three circles, radius **2 dp**, centres `x = dots.minX + 2 + i × 7` (i = 0, 1, 2), `y = statusTop + L/2`, color `skyTitle`, alpha `statusOpacity × replicaOpacity[i]` (3a `LaunchFrame.statusDotOpacity`).
- **Text:** `R.string.launch_getting_your_plan_ready`, Figtree 400 (`R.font.figtree_400`), size `S × f` dp where `S`/`L₀` are the size and line height of Phase 2's `status` style (`TypeMetrics.body`: 14 / 20; read them from the generated object, never as literals, AGENTS "Values only from generated tokens"), so `L = L₀ × f`; color `skyTitle`, alpha `statusOpacity`. No `TextMeasurer`, no text layout and no font load in the draw path [R]:
  - `…/launch/LaunchStatusRaster.kt`: `suspend fun prepareStatusRaster(context: Context, text: String, density: Float, fontScale: Float, color: Int, io: CoroutineDispatcher): StatusRaster`. On `io`: `ResourcesCompat.getFont(context, R.font.figtree_400)`; a `TextPaint(ANTI_ALIAS_FLAG)` with that typeface, `textSize = S × f × density`, `color`; `textWidth = measureText(text)/density`; `ascent = −fontMetrics.ascent/density`, `descent = fontMetrics.descent/density`; `rounded = ceil(textWidth × density)/density`; a `Bitmap` of `ceil(rounded × density) × ceil(L × density)` px with the text drawn at `x = 0`, baseline `((L − ascent − descent)/2 + ascent) × density`. Return `StatusRaster(image: ImageBitmap, textWidthDp, ascentDp, descentDp, lineHeightDp)`.
  - The controller starts this job (once per process) when the first `CommitStatus` arrives (or when `cf.mode=slow` is active), reading the string on the main thread from the activity's resources (so it is en or nl per `ConfigurationPolicy`). The result is published to a snapshot state the overlay's status draw reads. Until it arrives **nothing** of the status line draws, dots included: the dot positions depend on the measured text width (iOS places the dots only in `prepareText`, `StatusLineLayer.swift:65–69`, after font registration). If a Recreated activity (§5.6) has a different locale or font scale than the raster, prepare it again for the new configuration (OQ-3b-5).
  - Per frame: `drawImage(raster.image, topLeft = Offset(text.x, statusTop + text.y) × density, alpha = statusOpacity)` and the three `drawCircle` calls. Zero allocation.
- **Accessibility [F `I/plan.md:289–294`; R plan spec row 289]:** the overlay's single node gains `liveRegion = LiveRegionMode.Polite`; when `showsStatus` first becomes true, its `stateDescription` becomes the status string (announced once by TalkBack). Read `showsStatus` in composition, so the overlay recomposes exactly once for it; never per frame. Debug counter `LaunchController.statusAnnouncementCount` (AGENTS Tests rule 8) increments once.
- **Reduce Motion:** status fades in over 300 ms; dots hold at 1.0 (OQ 6).

### 5.6 Process policy [F `I/plan.md:2395` OQ 16, D14, `14 §3`; R mechanism]

`…/model/LaunchProcessPolicy.kt` (pure):

```kotlin
enum class LaunchKind { Cold, ProcessDeathRestore, Recreated, Warm }
object LaunchProcessPolicy {
    fun classify(hasSavedState: Boolean, savedProcessToken: String?, processToken: String, firstActivityOfProcess: Boolean): LaunchKind
    // hasSavedState && savedProcessToken == processToken → Recreated
    // hasSavedState (token missing or different)         → ProcessDeathRestore
    // !hasSavedState && firstActivityOfProcess           → Cold
    // !hasSavedState                                     → Warm
    fun claimsOverlay(kind: LaunchKind): Boolean          // Cold, ProcessDeathRestore → true (still subject to claimOverlay once per process and skip)
}
enum class SplashExit { RemoveNow, Fade300 }
object SplashExitPolicy { fun forActivity(kind: LaunchKind, overlayClaimed: Boolean): SplashExit }  // overlayClaimed → RemoveNow; Warm → Fade300; else RemoveNow
```

- **Process token** [R]: `AppGraph` passes `processToken = "${Process.myPid()}:${Process.getStartElapsedRealtime()}"` (no I/O, no `SecureRandom`) to `LaunchController`. `MainActivity.onSaveInstanceState` writes it under `KEY_PROCESS_TOKEN = "cf.launch.processToken"` (not a secret).
- **Cold / ProcessDeathRestore:** the system splash, the overlay (if `claimOverlay` grants it), readiness. For **ProcessDeathRestore**, `MainActivity` calls `super.onCreate(null)` so no saved Compose/Nav state is restored (A8: "ignores the saved stack"). Only DB-backed records will resume later phases' work.
- **Recreated** (locale or font-scale change, same process): no system splash, no keep condition. If the overlay is still running, the new activity composes it again with the **same `t0`, timeline and coordinator**, so it continues at its wall-clock position (OQ-3b-5); readiness is never restarted. If the hand-off is done, the content composes at once.
- **Warm** (new activity, same process, e.g. after finishing the activity): no overlay; the system splash fades out over **300 ms** (`provider.view.animate().alpha(0f).setDuration(300).setInterpolator(PathInterpolatorCompat.create(0f, 0f, 0.58f, 1f)).withEndAction { provider.remove() }`), then the content shows the known route. Debug counter `warmFadeCount`.
- **Activity finished mid-launch** (`onDestroy` with `isFinishing && !isChangingConfigurations` while the hand-off is not complete): complete the launch at once (iOS `sceneDidDisconnect` → `finishLaunch`): `send(ExitCompleted)`, `stop()`, end traces. A later Warm start in that process: if `state.route != null` show it; else start a **headless** coordinator (no overlay, `reduceMotion = true`, as iOS `reduceMotion: overlayController?… ?? true` [F `SceneDelegate.swift:433`]) and show the route when it is known (OQ-3b-6). A headless coordinator's clock is `clockFactory(nanoTime())` taken when it starts (iOS `MediaLaunchClock(t0: overlayController?.launchScene.t0 ?? CACurrentMediaTime())`, l.333), and it is stopped as soon as `state.route != null` (iOS l.467–469: no overlay → `coordinator.stop()` once routed). It begins no `ready`/`exit` trace.
- **`CF_UI_TEST_PROFILE=screen`** (3a: `skipOverlay`): no overlay; a headless coordinator picks the route (OQ-3b-12). `skipOverlay` must be evaluated **per activity** from that activity's intent (if 3a made it process-wide, change it and say so in the commit body).

### 5.7 LaunchController additions [R; extends 3a Task 6]

`…/launch/LaunchController.kt` stays process-level (built lazily by `AppGraph`, no I/O in its constructor). Add, main-thread only:

- Constructor inputs: `processToken: String`, `scope: CoroutineScope` (`SupervisorJob() + Dispatchers.Main.immediate`, owned by `AppGraph`), `clockFactory: (t0Nanos: Long) -> LaunchClock = { MediaLaunchClock(it, nanoTime) }`, `readinessFactory: (LaunchClock) -> LaunchReadiness` (from §5.9).
- `fun onActivityCreated(savedInstanceState: Bundle?, skipOverlay: Boolean): LaunchKind` — called in `MainActivity.onCreate` **before** `super.onCreate`; classifies (§5.6), counts activities, calls 3a's `claimOverlay(overrides)` only when `claimsOverlay(kind)`, records `lastLaunchKind` (public, read by tests).
- `fun onActivityDestroyed(isFinishing: Boolean, isChangingConfigurations: Boolean)`.
- `fun onSystemSplashExit(provider: SplashScreenViewProvider, kind: LaunchKind)` — applies `SplashExitPolicy` (claimed overlay: 3a's `systemIconBoundsPx` + `remove()`).
- Readiness start: when 3a's `onFirstDraw` sets `t0`, schedule (never synchronously inside the draw) `scope.launch(Dispatchers.Main) { startReadiness() }`: unless diagnostic (§5.9) build `clock = clockFactory(t0)`, `coordinator = LaunchCoordinator(readinessFactory(clock), clock, reduceMotion, LaunchState(), scope, ::onLaunchChange)` and `start()`. Also schedule the debug/perf main-thread stall here (§5.9).
- `onLaunchChange(state, effects)`: end `ready` trace when `state.isReady` first holds; publish `route` (snapshot state) when `state.route != null`; publish `showsStatus`; on any `CommitExit`/`CommitStatus`/`CancelStatus` effect rebuild `handOff = HandOffPlan(sampler = LaunchFrameSampler(LaunchHandOffTimeline.build(opening, state)), exitEndSeconds, revision + 1)` (snapshot state); on the first `CommitStatus` start the status raster job; when `didCommitExit` and `exitStart` is set, launch once `clock.sleepUntil(exitStart); LaunchTrace.beginExit()`.
- `fun onExitFrame(elapsedNanos: Long)` — called by the overlay's frame loop on the first frame whose elapsed time (`nanoTime() − t0`, §4 Frame clock) ≥ `exitEndSeconds`: record `handOffEndElapsedNanos = elapsedNanos`; `coordinator.send(ExitCompleted)`; `coordinator.stop()`; cancel the exit-trace job; `LaunchTrace.endExit()`, `endLaunch()` (moved here from 3a's `onOpeningSettled`); set `handOffComplete = true` (snapshot state). Removal is driven by the frame clock, never by a timer (iOS: "on the animation's completion, never `asyncAfter`").
- `frameLog: LaunchFrameLog?` (a constructor parameter, default null; `AppGraph` passes one only when `BuildConfig.DEBUG`): two preallocated `LongArray(512)` ring buffers `sampledNanos` / `wallNanos`, written from the draw without allocation. This is what plan.md's Stall test calls "read through a debug `LaunchProbe`": keep 3a's `compositionObserver` callback (the composition counter) unchanged and read the positions from `controller.frameLog`; say so in the Task 9 commit body (OQ-3b-16).
- `overlayPlaced`, `claimOverlay` and `holdsSystemSplash()` stay exactly as 3a built them (plan task "It owns `overlayPlaced`" is already satisfied); 3b only moves the `claimOverlay` call into `onActivityCreated` (§5.10).
- Snapshot state the UI reads: `handOff`, `route`, `showsStatus`, `statusRaster`, `handOffComplete`, `overlayActive` (= claimed by this process && !handOffComplete).

### 5.8 Overlay and root changes [R; extends 3a Tasks 6–7]

- **Overlay frame loop** (replaces 3a §5.8's "stop when settled"):
  ```kotlin
  LaunchedEffect(controller) {
      if (frozen) return@LaunchedEffect                       // cf.freezeAt: one frame, never removed
      while (!controller.handOffComplete) {
          val plan = controller.handOff
          val needsFrames = elapsedSeconds < plan.sampler.settledAfterSeconds || plan.exitEndSeconds != null
          if (needsFrames) { nextFrame()                              // pacing only; its timestamp is discarded
              val e = max(0, controller.nanoTime() − t0)              // t0 from the awaited first draw, as 3a's runLaunchOpening
              elapsed.longValue = e; plan.exitEndSeconds?.let { if (e ≥ it s) controller.onExitFrame(e) } }
          else snapshotFlow { controller.handOff.revision }.first { it != plan.revision }
      }
  }
  ```
  Extend 3a's `runLaunchOpening` (await the first-draw deferred for `t0` before any frame, injected `nanoTime`, injected `nextFrameNanos`) rather than writing a second loop; keep `LaunchFrameLoopTests` passing and add the exit/revision cases there (JVM, different clock bases).
  `settledAfterSeconds` is infinite while the dots repeat. With rm the opening settles at 0, and the loop wakes for the 300 ms fade or the status.
- `onDrawBehind` reads `controller.handOff.sampler` (not the cache block), so a new timeline never rebuilds paths or brushes. The status line is a **second** `drawWithCache` after the scene's, inside the same container `graphicsLayer` (so container alpha/scale apply); its cache block reads `statusRaster`, size and density only.
- Debug `frameLog` write in the scene draw: `sampled = elapsed`, `wall = controller.nanoTime() − t0`.
- **`CfRoot`** (`…/app/CfRoot.kt`):
  - shows `LaunchOverlay` while `controller.overlayActive` (also on a Recreated activity, §5.6);
  - the content keeps `hideFromAccessibility()` and gets no pointer input while `controller.overlayActive` (as 3a: "while the overlay is present"). Key it on `overlayActive`, never on `handOffComplete`: a Warm activity or a `screen`-profile process has no overlay and never sets `handOffComplete`, and its content must be accessible at once;
  - content = `Box(Modifier.fillMaxSize().testTag("app.root"))` containing `RoutePlaceholder(route, label)` once `controller.route != null`, nothing before;
  - `ReportDrawnWhen { !controller.overlayActive && controller.route != null }` (spec step 6; after the hand-off on a claimed launch, at once on a Warm/screen activity; signed in "follows the first app frame until Today lands in 7"; signed out moves to "Welcome interactive" in 3c);
  - after the hand-off, system bars per OQ-3b-11.
- **`RoutePlaceholder`** (`…/app/RoutePlaceholder.kt`, iOS OQ 8, no board): `@Composable fun RoutePlaceholder(route: LaunchRoute, label: (@Composable (rawName: String) -> Unit)?)` = `Box(Modifier.fillMaxSize().background(…).testTag("route.<rawName>"))` with raw names `welcome`, `today`, `verify`, `terms`, `walkthrough` (iOS `Route.rawValue`), and `label?.invoke(rawName)` centred inside. Background: `Welcome` → the `CfSky` gradient (the Welcome background per `I/plan.md:260`); others → `CfColors.background`.
  - **Debug label** [F OQ-0c-4]: `app/src/debug/kotlin/net/chronoflow/debug/DebugRouteLabel.kt`, `@Composable fun DebugRouteLabel(rawName: String)` = `Text(stringResource(net.chronoflow.core.ui.R.string.app_debug_route_x, rawName))` (iOS `RoutePlaceholder.swift:20`, the whole iOS file is `#if DEBUG`). `src/main` never names `app_debug_route_x` (it does not exist in release `R`).
  - **Wiring:** `CfRoot` gets `label` from the same variant hook that supplies 3a's `LaunchOverrides` (debug: `DebugLaunchOverrides` side in `app/src/debug` → `::DebugRouteLabel`; perf and release → `null`, so they show no text). Do not add a second variant mechanism; if 3a's hook cannot carry a composable, add a `routeLabel` property next to it and say so in the commit body.
  - 4e replaces this with the Nav3 root.

### 5.9 Diagnostics extras and the stub [F `I/App/LaunchArguments.swift`, `I/App/Readiness/StubReadiness.swift`; R names as intent extras]

All are **String** intent extras on `MainActivity` (forwarded by 0e's `SeedReceiverActivity`; Macrobenchmark passes them in `startActivityAndWait { it.putExtra(…) }`). Types and parser [R]:
- `LaunchMode { Ready, Slow }` goes next to `LaunchOverrides` in `…/launch/LaunchOverrides.kt` (3a's `LaunchOverrides` gains a `mode` field, below).
- `app/src/main/kotlin/net/chronoflow/app/LaunchDiagnostics.kt`: plain data only, `data class LaunchDiagnostics(mode: LaunchMode?, readyAt: Duration = 4600.ms, mainThreadStall: Duration?, reduceMotion: Boolean?, stub: StubConfig?)` with `companion object { val None }`, and `data class StubConfig(session: StubSession, gate: Gate, stored: StubStored, readyDelay: Duration)` (`enum StubSession { Native, SignedIn, SignedOut, Never }`, `enum StubStored { Yes, No, Never }`, iOS `StubReadiness.Configuration`). No behaviour, so it may ship.
- `…/perf/LaunchDiagnosticsParser.kt`: `object LaunchDiagnosticsParser { fun parse(extras: Map<String, String>): LaunchDiagnostics }`, so the extras work in debug and perf; `…/rel/LaunchDiagnosticsParser.kt` returns `LaunchDiagnostics.None` for any input. `stub` is non-null exactly when `usesInjectedReadiness` (below).
- In debug, `cf.readyAt`, `cf.mainThreadStall`, `cf.mode` and the stub keys come from `LaunchDiagnosticsParser` (one parser for debug and perf); 0e's `TestSeed` keeps supplying `reduceMotion`, `freezeAt`, the profile and `holdSplash`. 0e's own `readyAtMs`/`mainThreadStallMs` fields stay as they are (same rule, unused by 3b).

| Extra | Values (anything else → ignored/default) | Effect |
|---|---|---|
| `cf.session` | `in`/`signedIn` → SignedIn("debug"); `out`/`signedOut` → SignedOut; `never` → Never; default Native | stub session |
| `cf.gate` | `verify`/`terms`/`walkthrough`/`app`; default `app` | stub gate |
| `cf.stored` | `yes`/`no`/`never`; default `yes` | stub stored data |
| `cf.readyDelay` | seconds, finite, `0…60`; default 0 | stub: stored data turns true at this absolute launch time |
| `cf.mode` | `ready`/`slow` (`welcome` parses but is treated as absent until 3c, with a `Log.w`) | diagnostic fixed ending, no coordinator |
| `cf.readyAt` | ms, finite, `0…60000`; default **4600** | ready time of `cf.mode=slow` |
| `cf.mainThreadStall` | ms, finite, `0…60000` | block the main thread for this long, starting 300 ms after `t0` |
| `cf.reduceMotion` | as 0e `TestSeed` (`1/true/0/false`) | perf variant only; debug keeps 0e's parse |

- **`usesInjectedReadiness`** = any of `cf.session`, `cf.gate`, `cf.stored`, `cf.readyDelay` present (iOS checks presence of a value, `LaunchArguments.swift:39–41`). Then `readinessFactory = { clock -> StubReadiness(diagnostics.stub!!, clock, nativeReadiness = SignedOutReadiness) }`; otherwise the variant default: **`SignedOutReadiness` in debug, perf and release** (iOS release wiring before its Phase 9d and the Perf build: `SignedOutReadiness`). 4e replaces the debug/release default.
- **`…/perf/StubReadiness.kt`** (port; `Native` delegates to `nativeReadiness`):
  - `session()`: Native → `nativeReadiness.session()`; SignedIn → `SignedIn("debug")`; SignedOut; Never → `delay(60.seconds)`, then SignedOut.
  - `gate()` = config gate. `hasStoredData()`: Never → `delay(60.seconds)`; returns `stored == Yes`.
  - `storedDataUpdates()`: `flow { if (stored == Never) awaitCancellation(); emit(stored == Yes); if (stored == No) { clock.sleepUntil(readyDelay); emit(true) } }`.
  - `allowFirstLoad()`: increments `firstLoadCount` once (`firstLoadCount` stays ≤ 1).
- **Diagnostic mode** (iOS `diagnostic = mode != null || freezeAt != null`, `SceneDelegate.swift:91`): no coordinator, no `ready` trace. Timeline: `ready` → `opening.withReadyExit(1500 ms)`; `slow` → `opening.withStatus(2 s).withReadyExit(readyAt)` (start the status raster at once). The route stays `null`: iOS never navigates in diagnostic `ready`/`slow` mode (only `welcome` does, l.161–171), so the content stays empty after removal and `ReportDrawnWhen` does not fire. With `cf.freezeAt` too, it is frozen at that time and never removed.
- **Stall** (`…/perf/MainThreadStall.kt`, release twin a no-op): `Handler(Looper.getMainLooper()).postDelayed({ SystemClock.sleep(ms) }, max(0, 300 − elapsedSinceT0Ms))`, scheduled once from `startReadiness`. This is app code, deliberately blocking; it never ships in release.
- **`LaunchOverrides`** (3a): add `mode: LaunchMode?`, `readyAt: Duration = 4600.ms`, `mainThreadStall: Duration?`, `readiness: ((LaunchClock) -> LaunchReadiness)?`. Debug = 0e `TestSeed` (`reduceMotion`, `freezeAt`, profile, `holdSplash`) + `LaunchDiagnosticsParser` (the rest); perf = `LaunchDiagnosticsParser` (including `cf.reduceMotion`); release = `LaunchOverrides.None`. Keep 3a's variant wiring; do not add a second mechanism.

### 5.10 Activity order (3a Task 7, updated) [R]

`MainActivity.onCreate`:
1. `val kind = launch.onActivityCreated(savedInstanceState, skipOverlay)` (`skipOverlay` from this activity's `intent`, readable before `super.onCreate`); if this claims the overlay: `LaunchTrace.beginFirstCommit()` and `LaunchTrace.beginReady()` (unless diagnostic). This replaces 3a's step-1 `beginFirstCommit()` and 3a's step-2 stub `onActivityCreated(restored = …)` (delete that stub signature).
2. `val splash = installSplashScreen()`; `super.onCreate(if (kind == ProcessDeathRestore) null else savedInstanceState)`.
3. 3a steps 3–5 **minus** the `launch.claimOverlay(...)` call, which now runs inside `onActivityCreated` (reduce motion capture, edge-to-edge, `setContent { CfRoot(graph) }`).
4. `splash.setKeepOnScreenCondition { launch.holdsSystemSplash() }` (unchanged: only the claiming activity holds).
5. `splash.setOnExitAnimationListener { launch.onSystemSplashExit(it, kind) }`.

`onSaveInstanceState(out)`: `super`, then `out.putString(KEY_PROCESS_TOKEN, launch.processToken)`. `onDestroy`: `launch.onActivityDestroyed(isFinishing, isChangingConfigurations)`. After `warmFonts()` returns (3a starts it after `first-commit`) nothing else is needed: the status raster loads its own typeface on IO.

### 5.11 Traces [F `LaunchSignposts.swift`, `SceneDelegate.swift:104–107,438–466,646–648`; R cookies]

`LaunchTrace` async sections (each begin/end at most once per process): `launch` 1 and `first-commit` 2 (3a), **`ready` 3**: begins with `first-commit` (claimed overlay, not diagnostic), ends in `onLaunchChange` when `state.isReady`; **`exit` 4**: begins when the launch clock reaches `exitStart`, ends in `onExitFrame`; `launch` now ends in `onExitFrame`. Cookies 5 (`session`, 4e) and 6 (`intro`, 3c) are reserved; write that in a KDoc on `LaunchTrace`.

## 6. Tasks

Each task is one Conventional Commit with no trailers. Tick the matching `plan.md` checkbox in the commit that completes it (mapping in brackets). Per fix round run only what you touched (`make targeted ONLY="…"`); `make quick` once before the last commit; `make check` at the end (the orchestrator runs emulator tiers if your sandbox cannot: report the exact command and its output).

### Task 1: Seams and reducer (`feat(launch): port the launch readiness reducer`)

- Files: §5.1 in `core/model/src/main/kotlin/net/chronoflow/core/model/launch/{LaunchReadiness.kt, SignedOutReadiness.kt}`; `…/model/LaunchReadinessReducer.kt` (§5.2).
- Tests (JVM, `feature/launch/src/test/kotlin/net/chronoflow/feature/launch/model/LaunchReadinessTests.kt`), same names as Swift (`I/Packages/ChronoFlowKit/Tests/AppCoreTests/LaunchReadinessTests.swift`), target rect `LaunchRect(38, 64, 28, 28)`; a test helper `fun LaunchState.step(e, rm = false)` returns the effects and replaces a `var state`. Session ids are `"a"` unless stated:
  - `endingIsChosenOnceAndIntentNeverRoutes` — signed out gives no effects; a later `SignedIn("later")` changes nothing: ending Welcome, route Welcome, `isReady`. (The pending-intent assertion is added in 4e.)
  - `appWaitsForBothGateAndStoredDataThenCommitsAhead` — SignedIn, StoredData(true) → not ready; GateKnown(App) → `[CommitExit(App, 1500 ms, null)]`, route Today; `Reached` 1400/1500/2000/8000/9000 → no effects.
  - `nonAppGatesAreReadyWithoutStoredData` — parameterized (one test case each) over Terms, Verify, Walkthrough: ready, route = gate route, `[CommitExit(App, 1500 ms, null)]`.
  - `lateReadyStartsAtReadinessAndStatusIsLatched` — SignedIn, GateKnown(App); StoredData(false) → `[CommitStatus(2 s)]`, `showsStatus` false; Reached(2 s) → true; Reached(3500) then StoredData(true) → `[CommitExit(App, 3500 ms, null)]`; status still shown; `statusStart == 2 s`.
  - `statusRequiresExplicitFalseAndCancelsBeforeItsStart` — SignedIn; Reached(1900) → none, `statusStart` null; StoredData(false); GateKnown(Terms) → `[CancelStatus, CommitExit(App, 1900 ms, null)]`; Reached(3 s) → not shown, `statusStart` null.
  - `lateStoredFalseStartsStatusNowAndNeverForUnknown` — SignedIn; Reached(4 s) → none, not shown; StoredData(false) → `[CommitStatus(4 s)]`, shown.
  - `timeoutForcesCurrentSessionOnceAndIgnoresLateFacts` — SignedIn("a"); Reached(8 s) → `[ForceReady, CommitExit(App, 8 s, null)]`; route Today; Reached(9 s), GateKnown(Verify), Session(SignedIn("b")) → none; session stays "a".
  - `unresolvedSessionDefaultsSignedOutAtEightSeconds` — Reached(8 s) → `[CommitExit(Welcome, 8 s, target)]` (fallback = target), SignedOut, ready, route Welcome.
  - `welcomeCommitsOnAnchorAndRebuildsOnlyBeforeFlight`, `welcomeFallbackIsOnlyCommittedAtDeadlineAndCanBeReplaced` (Reached(1549) → none; Reached(1550) → `[CommitExit(Welcome, 1600, target)]`; anchor `(61,103,28,28)` → re-commit; after ExitCompleted nothing), `invalidAnchorsAreIgnored` (Swift set mapped: zero rect, `CGRect.null` → `LaunchRect(+∞, +∞, 0, 0)`, NaN x, infinite y; `CGRect.infinite` has no equivalent and is omitted, §5.2 rule 4; after each, no effects and `moonTarget == null`), `lateSignedOutStartsAtResolution` (Reached(3 s), SignedOut → `[CommitExit(Welcome, 3 s, target)]`), `reduceMotionSkipsWaitAndAnchor` (parameterized: signed out rm → `[CommitExit(Welcome, 0, zeroRect)]`; signed in + Terms rm → `[CommitExit(App, 0, null)]`). Values exactly as `LaunchReadinessTests.swift`.
  - `gateRoutesMapLikeIos` (new): each `Gate.launchRoute()`.
- `core/model` test `SignedOutReadinessTest`: session SignedOut, `gateUpdates().toList() == [App]`, `storedDataUpdates().toList() == [true]` (iOS `hasStoredData() → true`).
- **Shared test clock** [R] (done in Task 5, the first `:app` user; listed here so Tasks 1–4 keep using 3a's copy unchanged): `:app` tests (Tasks 5–7) cannot see `feature/launch/src/test`. Task 5 moves 3a's `ManualLaunchClock` to Android test fixtures of `:feature:launch` (`android { testFixtures { enable = true } }`, `feature/launch/src/testFixtures/kotlin/net/chronoflow/feature/launch/model/ManualLaunchClock.kt`, `testImplementation(testFixtures(project(":feature:launch")))` in `:feature:launch` and `:app`). If AGP 9 rejects Kotlin test fixtures, keep 3a's copy and add a test-only `app/src/testDebug/kotlin/net/chronoflow/app/perf/ObservedLaunchClock.kt` (iOS `StubReadinessTests.swift:118–138` shape: own manual sleepers, `pendingSleeps`, recorded `deadlines`) and record the fallback in the commit body.

### Task 2: Coordinator (`feat(launch): port the launch coordinator`) [ticks "LaunchReadiness.kt (pure)"]

- File: `…/model/LaunchCoordinator.kt` (§5.3).
- Tests (JVM, `feature/launch/src/test/kotlin/net/chronoflow/feature/launch/model/LaunchCoordinatorTests.kt`; `runTest(timeout = 60.seconds)`; the coordinator scope = **`backgroundScope`** (runTest's `StandardTestDispatcher`), never `this`: `runTest` waits for its own children at the end, so a pending deadline or a suspended `session()` would hang the test until the 60 s cap; still call `stop()` where Swift does; the 3a `ManualLaunchClock`; replace iOS yield loops with `runCurrent()`; never `advanceUntilIdle()` while a `Never` stub is pending): port `manualClockResumesAtDeadlinesAndCancelsSleep`, `coordinatorForcesOnlyItsSourceOnce` (start twice; stored false; advance to 8 s → `forced == 1`, exactly 1 `CommitExit`; `send(Reached(9 s))` keeps 1; after `stop()` `pendingSleeps == 0`), `stopCancelsPendingTimersBeforeAnyDeadline` (parameterized signedOut true/false: 3 vs 2 sleeps registered, `now == 0`; stop → 0 sleeps; advance to 8 s → no `onChange`, state unchanged), `stopCancelsAnUnresolvedSessionTask` (a source whose `session()` suspends until cancelled records `cancelled == true`; no forced readiness, 0 changes), `signedOutUsesFallbackAtDeadlineAndFinishesOnce` (fallback `(55,76,28,28)`; ready with no effects; advance to 1550 → `[CommitExit(Welcome, 1600, target)]`; after ExitCompleted + stop a later Session leaves the state equal), `storedDataStreamHandsOffAtReadinessBeforeTimeout` (stream false, then true at 3500 → effects `[CommitStatus(2 s), CommitExit(App, 3500 ms, null)]`, `showsStatus` after 2 s, `exitStart == 3500`, no force), `mediaClockUsesPassedCommitEpoch` (`MediaLaunchClock(t0 = n − 500 ms, nanoTime = { n })` → `now == 500 ms`).

### Task 3: Ending timelines (`feat(launch): port the ready exit and the slow status timeline`)

- Files: `…/model/{LaunchTimelineReady.kt, LaunchTimelineWelcomeInterim.kt, LaunchHandOffTimeline.kt}` (§5.4).
- Tests (JVM):
  - `ReadyTimelineTests.kt` — port all five with the Swift names and values: `readyExitKeysAndNeverCutShort` (ready 0/50/1499/1500/4600/8000 → `start = max(1.5, r)`; container opacity and scale key times `[0, start, start + 0.4]`, values `[1, 1, 0]` / `[1, 1, 1.08]`, curves `[linear, easeOut]`; at `start + 0.2` opacity `1 − 0.6846431874` and scale `1 + 0.08 × 0.6846431874` within 1e-9; opacity 0 at `start + 1`; unique tracks), `mergingReadyPreservesEveryOpeningKeyAndSample` (every 10 ms 0–1500 equals the opening; idempotent; `withReadyExit(4600)` on a merged timeline equals the opening's), `slowStatusFadeAndLatchThroughExit` (keys `[0, 2, 2.3]`; 0 at 1999; 0.6846431874 at 2150; 1 at 2300/2600/4600/4900/5000/9000; order-independent; idempotent), `dotsRepeatFromMountWithDelayedReplicas` (8 cycles × 3 replicas × offsets 0/0.24/0.48/0.72/0.96/1.1 → 0.25/0.625/1/0.625/0.25/0.25; `withStatus(1 s)` dots equal `withStatus(2 s)` dots at 2600), `reduceMotionFadesImmediatelyWithConstantDots` (375 × 667 rm, ready 50 → keys `[0, 0.05, 0.35]`, 0 at 350, scale 1 at 4 s, dots `[1, 1, 1]`, moon `(187.5, 333.5)`, no repeat; `withReadyExit(0)` keys `[0, 0.3]`; `withStatus(0)` keys `[0, 0.3]`).
  - `LaunchHandOffTimelineTests.kt` — `buildStartsFromTheOpeningEveryTime` (status then exit in either order gives equal timelines); `exitEndIsNineteenHundredWhenReadyBefore1500`; `exitEndForLateReady` (R = 3500 → 3.9); `exitEndWithReduceMotion` (R = 50 ms → 0.35); `interimWelcomeEndsAtW950` (W 1600 → 2.55; W 8000 → 8.95; rm W 8000 → 8.3); `interimWelcomeKeepsTheOpeningUntilW700` (samples 0–2300 every 10 ms equal the opening); `noExitWhileNotCommitted` (`exitEndSeconds == null`); `cancelledStatusRemovesTheStatusTrack` (after `CancelStatus` the rebuilt timeline has no status keys beyond `[0]`).
  - `LaunchFrameSamplerTests` (3a) gains `settledAfterIsInfiniteWhileDotsRepeat` and `sampleIntoStatusAllocatesNothing` (same allocation method as 3a, sampling 2000–5000 ms of a slow timeline).

### Task 4: Overlay endings and the status line (`feat(launch): play the endings and the slow status`) [ticks "Endings"]

- Files: `…/launch/{LaunchController.kt, LaunchOverlay.kt, LaunchScenePainter.kt, LaunchStatusRaster.kt, LaunchFrameLog.kt, LaunchTrace.kt, LaunchTracing.kt, LaunchFrameLoop.kt}`.
- Behaviour: §5.5, §5.7 (`handOff`, `onExitFrame`, readiness start, `onLaunchChange`), §5.8 frame loop and draw split, §5.11.
- Tests (Robolectric `@Layout`, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`, `feature/launch/src/test`; controller built with `nanoTime = { mainClock.currentTime × 1e6 }`, `clockFactory = { manualClock }`, a fake `LaunchReadiness`; a helper `advanceTo(ms)` moves `manualClock` and `mainClock` together in 8 ms steps with `autoAdvance = false`):
  - `LaunchReadyExitRenderTest` — ready at 0: at 1700 ms the container `graphicsLayer` alpha is `1 − 0.6846431874` ± 0.01 and scale `1 + 0.08 × 0.6846431874` ± 0.001 (read through the 3a sampler hooks, not pixels); `handOffComplete` becomes true on the first frame ≥ 1900 and `handOffEndElapsedNanos` is in `[1900, 1908] ms`; the overlay leaves the tree on the next frame; recompositions over 0–1900 ms ≤ 2 (3a probe).
  - `LaunchSlowStatusRenderTest` — stored false, ready at 3500, en and nl, light and dark, at `pixel8` (one test case each); before asserting pixels, wait (bounded `waitUntil`) for `controller.statusRaster != null` (it loads on the injected IO dispatcher): no status pixels at 1990; at 2600 the status rect contains `skyTitle`-ish pixels (≥ 1 pixel within 24/255 of `skyTitle` inside the text box) and three dot discs at `x = dots.minX + 2 + i × 7`; at 2600 ms replica i samples the dot track at `(2.6 − 0.2 i) mod 1.2` = 0.2 / 0.0 / 1.0 s, so dot 0 is brighter than dots 1 and 2 (assert that order, not exact pixel alpha); `statusAnnouncementCount == 1` and the overlay node's `stateDescription` equals the resource string; hand-off at 3900 ± 8 ms.
  - `LaunchForcedExitTest` (`@Layout`: it draws frames, so it stays out of `make quick`; to stay < 5 s, move both clocks to 7,900 ms in one step, as a stall would, then 8 ms steps) — session never resolves: `handOffComplete` at 8950 ± 8 ms with route Welcome; signed in with stored never: `allowFirstLoad` called once, hand-off at 8400 ± 8 ms, route Today.
  - `LaunchReduceMotionEndingTest` — rm, ready at 50 (fade 50 → 350 ms, ease-out): container alpha `1 − 0.6846431874` ± 0.01 at 200 ms (progress 0.5), removed at 350 ± 8 ms, scale 1 throughout; rm slow: status opacity 1 at 2300 and dots alpha 1.
  - `LaunchStatusRasterTest` (`@Layout`) — `prepareStatusRaster` with `fontScale` 1.0 and 1.3: image height `ceil(20 × f × density)`, width `ceil(rounded × density)`, `StatusLineLayout.ink` inside the window; runs on the test's IO dispatcher, never on main (assert with a dispatcher that records its thread).

### Task 5: Diagnostics and the stub (`feat(app): add launch diagnostics and the stub readiness`) [ticks "StubReadiness"]

- Files: `app/src/main/kotlin/net/chronoflow/app/LaunchDiagnostics.kt`, `…/perf/{LaunchDiagnosticsParser.kt, StubReadiness.kt, MainThreadStall.kt}`, `…/rel/{LaunchDiagnosticsParser.kt, MainThreadStall.kt}` (the release-only `src/releaseNoPerf` directory defined above), `LaunchMode` and the `LaunchOverrides` additions (3a), debug and perf override wiring (§5.9), the shared test clock (Task 1, "Shared test clock"), and 0e `TestSeed` forwarding of the new `cf.*` keys if it drops them (§2 item 7).
- Tests (all in `app/src/testDebug/kotlin/net/chronoflow/app/perf/`; `src/testDebug` because the perf sources compile into `debug` but not into `release`, and `testReleaseUnitTest` compiles `src/test`):
  - `LaunchDiagnosticsTests.kt` (JVM) — port `testReadinessArgumentsHaveExplicitUnitsAndRejectInvalidValues` (it sits in iOS `StubReadinessTests.swift:98–115`: `cf.session=in cf.gate=terms cf.stored=no cf.readyDelay=3.5` → SignedIn/Terms/No/3500 ms; `readyDelay` `-1, nan, inf, 61, wrong` → 0; `never`/`never`/`out`/`bad` → Never/Never/SignedOut/Native) and `testDefaultsAndInvalidValues` (iOS `LaunchArgumentsTests.swift:55–73`; no extras, `cf.freezeAt` absent/`nan`/`-1`/`inf`/`60001`, `cf.mode=unknown` → mode null, `readyAt == 4600 ms`; `cf.mode=slow cf.readyAt=3500 cf.mainThreadStall=500 cf.reduceMotion=1` → Slow, 3500, 500, reduceMotion true). The `freezeAt` range itself is 0e `TestSeed`'s job (`TestSeedTest`), not the parser's: assert only that those inputs leave `mode`/`readyAt` at their defaults. Add `usesInjectedReadinessOnlyWithAStubKey` and `welcomeModeIsDeferred` (`cf.mode=welcome` → null).
  - `StubReadinessTests.kt` (JVM, `runTest(timeout = 60.seconds)`, collectors in `backgroundScope`, the shared `ManualLaunchClock` wrapped to record deadlines as iOS `ObservedLaunchClock`) — port `testFalseBecomesTrueAtTheSharedLaunchDeadline` and `testProtocolDispatchUsesTheSameInjectedDeadline` (clock at 2 s, stored No, delay 3500 → one sleep registered for 3500; still pending at 3499; at 3500 values `[false, true]`, sleeps 0; `allowFirstLoad` twice → count 1; the first test calls `storedDataUpdates()` on the `StubReadiness` reference, the second on a `LaunchReadiness`-typed reference, as iOS `checkDelayedStream(throughProtocol:)`), `testPassedDeadlineEmitsReadinessWithoutAnotherDelay` (clock at 4 s → `[false, true]`, no sleep), `testCancellingTheStreamCancelsItsClockSleep` (delay 8 s; cancel the collector → values `[false]`, sleeps 0); add `neverStoredEmitsNothingUntilCancelled` and `nativeSessionDelegates`.
  - `LaunchModeTimelineTests.kt` — port `testModeTimelinesAndAsymmetricSafeAreaFallback` for `ready` and `slow` only (570 × 730): equals `opening.withReadyExit(1500)` / `opening.withStatus(2 s).withReadyExit(4600)`.
  - `ReleaseReadinessTest` (`app/src/testRelease`) gains: `Class.forName("net.chronoflow.app.perf.StubReadiness")` and `Class.forName("net.chronoflow.debug.DebugRouteLabel")` throw (the latter after Task 7; add it there); release `LaunchDiagnosticsParser.parse(mapOf("cf.session" to "in", "cf.mode" to "ready", "cf.mainThreadStall" to "500"))` is `LaunchDiagnostics.None`.

### Task 6: Process policy and controller lifecycle (`feat(launch): add the process launch policy`) [ticks "LaunchController"]

- Files: `…/model/LaunchProcessPolicy.kt`, `…/launch/LaunchController.kt` (§5.6, §5.7), `…/app/{AppGraph.kt, LaunchDependency.kt, MainActivity.kt}` and variant `LaunchPlatform.kt` (§5.10).
- Tests:
  - `LaunchProcessPolicyTests` (JVM) — every `classify` row; `claimsOverlay`; `SplashExitPolicy` (claimed → RemoveNow; Warm → Fade300; Recreated/Cold-with-skip → RemoveNow); port `firstConnectionIsProcessScoped` against 3a's `claimOverlay` on two controller instances (true, false, false; a new instance → true).
  - `LaunchControllerLifecycleTest` (Robolectric `@Logic`, `feature/launch/src/test`: `onActivityCreated` takes a `Bundle`, which is a throwing stub on the plain JVM; fakes for readiness and clock): a Recreated activity mid-overlay keeps `t0`, coordinator and `handOff` (`session()` called once); finishing mid-launch completes the launch (`ExitCompleted` sent, coordinator stopped, `handOffComplete`); a Warm activity after that with a known route shows it, with an unknown route starts a headless coordinator whose `reduceMotion` is true and which stops once the route is known; `screen` profile → no overlay, headless coordinator, `overlayActive` false.
  - `ProcessDeathRestoreTest` (Robolectric `@Logic`, `app/src/testDebug/kotlin/net/chronoflow/app/`: it needs 0e's debug `TestSeed` to read the profile): build `MainActivity` A through `Robolectric.buildActivity(…, intent with CF_AUTH_TEST_ID + CF_UI_TEST_PROFILE=screen)`; register `savedStateRegistry.registerSavedStateProvider("cf.test") { bundleOf("v" to 1) }`; `saveInstanceState(bundle)`; destroy. (a) Replace `cf.launch.processToken` with `"other"`, call `graph.resetLaunchForTest()` (3a, OQ-3a-13) so the once-per-process claim is fresh, create B (profile `launch`) from it: `lastLaunchKind == ProcessDeathRestore`, `B.savedStateRegistry.consumeRestoredStateForKey("cf.test") == null`, the overlay is claimed. (b) Untampered bundle: `Recreated`, the key is restored, no claim, `holdsSystemSplash()` false. If Robolectric cannot create `MainActivity` (splash or Compose host), extract the bundle decision into `LaunchActivityDelegate.bundleForSuper(saved): Bundle?` used by `MainActivity`, test it with a minimal `ComponentActivity` in test sources, and record the fallback in the commit body.

### Task 7: Hand-off targets (`feat(app): hand off to route placeholders`) [ticks "Hand-off targets"]

- Files: `…/app/{CfRoot.kt, RoutePlaceholder.kt}` (§5.8), system-bar update (OQ-3b-11).
- Files also: `app/src/debug/kotlin/net/chronoflow/debug/DebugRouteLabel.kt` and its wiring (§5.8); the `ReleaseReadinessTest` line for it (Task 5).
- Tests (Robolectric `@Layout`, `app/src/testDebug/kotlin/net/chronoflow/app/`: they assert the debug-only label, which release `R` does not have):
  - `CfRootHandOffTest` — fake readiness signed in + App + stored: before the hand-off `app.root` exists, is hidden from accessibility and `route.today` is absent or hidden; after `handOffComplete` `route.today` exists, the content has no `HideFromAccessibility`, the overlay node (`launch.opening`, which consumes pointers) is gone; signed out → `route.welcome`; gate Terms → `route.terms`; a `screen`-profile controller (no overlay) shows `route.welcome` at once with the content visible to accessibility. The placeholder text is "Debug route: today" (en) / "Debugroute: today" (nl) from resources.
  - `RoutePlaceholderGuardTest` — reference-free guards for `route.welcome` and `route.today` at `small`, `pixel8`, `foldInner`, `largeFont`, light and dark (no text overflow; the debug text stays inside the window).

### Task 8: Traces, reportFullyDrawn and warm start benchmark (`perf(launch): trace the hand-off and measure warm start`) [ticks "Traces"]

- `LaunchTrace` `ready`/`exit` (§5.11); `ReportDrawnWhen` (§5.8).
- `LaunchTraceTest` (JVM with 3a's injectable `LaunchTracing` / `LaunchTraceSession` seam, or Robolectric `ShadowTrace`): each section begins and ends once per process; `exit` begins at `exitStart` on the launch clock and ends with `launch` in `onExitFrame`; diagnostic mode never begins `ready`.
- `:benchmark`:
  - `StartupBenchmark` gains `startupWarm` (`StartupMode.WARM`, 10 iterations, perf variant, `CompilationMode` as 0e): `StartupTimingMetric()`.
  - The cold `StartupBenchmark` adds `TraceSectionMetric("ready", Mode.First)` and `TraceSectionMetric("exit", Mode.First)` (use 3a's async-slice fallback if it was needed) and now reports `timeToFullDisplayMs`.
  - `budgets.json`: warm `timeToInitialDisplayMs` median — reference 200, low-end 400 (proposals, OQ-A20), emulator report; `ready`, `exit`, `timeToFullDisplayMs` — report everywhere. Use the exact metric keys the JSON output shows.
- Run `make perf`; append warm TTID, TTFD, `ready`, `exit` rows to `docs/perf-startup.md` (date, commit, `pixel` AVD API 36, 60 Hz, build type). If `make baseline-profile` exists in the stack, regenerate (the startup journey now runs through the hand-off); otherwise write "pending".

### Task 9: Emulator proofs (`test(app): prove hand-off timing, stall and warm start`) [ticks "Stall test"]

All `@Smoke`, `app/src/androidTest`, through `ScreenTest.launch(overrides)` with `CF_UI_TEST_PROFILE=launch` and `cf.reduceMotion=false` (the scales are 0 in functional runs). Each test first calls `graph.resetLaunchForTest()` on the main thread (3a OQ-3a-13); `LaunchWarmStartTest` calls it only before its first launch (a reset between its two launches would make the second one Cold). Read values from `(application as ChronoFlowApp).graph.launch` with the 0e wait helper (in-process, bounded). `frame` = `1000 / display.refreshRate` ms. Each method < 10 s.

- `LaunchHandOffTest`:
  - `readyHandsOffAt1900` — `cf.session=in cf.stored=yes cf.gate=app`: wait ≤ 5 s for `handOffComplete`; `handOffEndElapsedNanos` in `[1900, 1900 + frame + 2] ms`; `route.today` exists; `launch.opening` gone.
  - `slowShowsStatusAndHandsOffAfterReady` — `cf.session=in cf.stored=no cf.readyDelay=3.5`: `statusAnnouncementCount == 1`; end in `[3900, 3900 + frame + 2] ms`.
- `LaunchStallTest.stallSkipsFramesAndNeverStretches` — `cf.session=in cf.stored=yes cf.mainThreadStall=500`: from `frameLog`, the first frame with `wall ≥ 800 ms` has `|sampled − wall| ≤ 2 × frame + 2 ms`; the sampled gap across the stall is ≥ 450 ms (frames skipped, not stretched); the hand-off still ends in `[1900, 1900 + frame + 2] ms`.
- `LaunchWarmStartTest.warmStartShowsNoOverlay` — launch with `cf.session=in cf.stored=yes`, wait for the hand-off; `scenario.onActivity { it.finish() }`, wait until destroyed; `ActivityScenario.launch(MainActivity)` again: `lastLaunchKind == Warm`, `launch.opening` never appears (`waitUntilDoesNotExist` plus `waitUntilExactlyOneExists(route.today, 2_000)`), the app is not held on the splash. If the exit listener ran, `warmFadeCount == 1`.
- `:benchmark` `ProcessDeathLaunchTest.killedProcessColdLaunchesWithTheSplash` (plain JUnit4 + UiAutomator in the `:benchmark` module, perf variant, run by `make perf`; document the exact Gradle command in `docs/tooling.md`): start the app (`monkey -p <pkg> -c android.intent.category.LAUNCHER 1`), wait for `By.res("app.root")`; record `pidof <pkg>`; `pressHome()`; `am kill <pkg>`; poll `pidof` with one bounded shell command (`sh -c 'for i in $(seq 20); do pidof <pkg> >/dev/null || exit 0; sleep 0.25; done; exit 1'`, the sleep is in the shell, not the test); relaunch with the same `monkey` command; assert `device.wait(Until.hasObject(By.res("launch.opening")), 3_000)` and that the new pid differs. If `make perf` is not the right home in 0e's `Makefile`, add the class to whatever target runs `:benchmark` connected tests and record it.

### Task 10: Captures, guards and docs (`docs(launch): record phase 3b launch results`)

- `LaunchEndingCaptureTest` (`@Capture`): `make captures ONLY=SplashScreen,SplashSlow` writes `build/captures/SplashScreen_ready-1700_<profile>_en_<theme>.png`, `…_ready-1900_…`, `SplashSlow_slow-2000_…`, `…_slow-2600_…`, `…_slow-4900_…`, `…_slowReduceMotion-2600_…`, at `pixel8` and `small`, light and dark, plus `SplashSlow_slow-2600_pixel8_nl_light`. Drive them with `cf.mode`-equivalent timelines through `LaunchHandOffTimeline` at a frozen time; the status raster is prepared before the capture.
- `LaunchStatusGuardTest` (`@Layout`, one test per profile × language): at 2600 ms of the slow timeline the drawn status (text box + dots) lies inside the horizontal safe bounds at `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` (font scale 1.3), and never overlaps the wordmark box.
- `plan.md`: add `cf.session`, `cf.gate`, `cf.stored`, `cf.readyDelay`, `cf.mode` to "Android spec: seeds and test hooks" (keep `cf.readyAt`, `cf.mainThreadStall`); add OQ-3b-1 … OQ-3b-17 as the next free OQ-A numbers with their defaults; mark OQ-3a-1 and OQ-3a-2 answered by 3b.
- `docs/perf-startup.md`: Task 8 rows; the Task 9 measured `handOffEndElapsedNanos` values (ready, slow, stall) and the stall frame gap; the process-death result.

## 7. Acceptance criteria

- `make quick` passes (< 60 s warm), including every JVM test of Tasks 1–3, 5, 6 and the Konsist purity rule for `…/model/` and `core/model/…/launch/`.
- `make targeted ONLY="core:model:SignedOutReadinessTest feature:launch:LaunchReadinessTests feature:launch:LaunchCoordinatorTests feature:launch:ReadyTimelineTests feature:launch:LaunchHandOffTimelineTests feature:launch:LaunchFrameSamplerTests feature:launch:LaunchProcessPolicyTests app:LaunchDiagnosticsTests app:StubReadinessTests app:LaunchModeTimelineTests"` passes (use the module syntax 0a's Makefile expects).
- `./gradlew :app:testReleaseUnitTest` passes with the new `ReleaseReadinessTest` assertions; `:app:verifyReleaseManifest` passes.
- `make check` passes with `pixel` booted, including `LaunchHandOffTest`, `LaunchStallTest`, `LaunchWarmStartTest` and 3a's `LaunchFrameZeroTest`/`LaunchStrictModeTest` (StrictMode `penaltyDeath` still survives: the status font loads on IO). Every Robolectric test < 5 s (`build/test-results/*/timings.json`).
- Functional numbers on the emulator: ready hand-off end 1900 ms + ≤ 1 frame; slow 3900 ms + ≤ 1 frame; with a 500 ms stall the post-stall frame is at the wall-clock position (≤ 2 frames) and the hand-off end is unchanged. JVM: forced exit at 8 s (signed in 8000–8400; unknown → welcome 8000–8950).
- `ProcessDeathLaunchTest` passes; the warm start shows no overlay.
- `make perf` produced warm `timeToInitialDisplayMs`, cold `timeToFullDisplayMs`, `ready` and `exit`; `check-benchmarks.py` passes; rows are in `docs/perf-startup.md`. Device gates stay "open: device only" (OQ-A22).
- `make captures ONLY=SplashScreen,SplashSlow` produced the Task 10 PNGs; the reviewer compared `ready-1900` with `SplashSpec` "1900 ms · Hand-off" (which shows board 1700, S3) and `slow-2600`/`slow-4900` with `SplashSpec`/`SplashSlow`; every difference is S1–S5 (3a §5.1) or fixed.
- Every `plan.md` 3b checkbox is ticked in its commit; `git log` shows atomic Conventional Commits without trailers.

## 8. Review checklist (Opus reviewer)

Reducer and coordinator
- [ ] `reduce` follows §5.2 statement by statement (diff it against `LaunchReadiness.swift:108–186`): `Reached(clock.now)` is reduced **before** every event in `send`; `GateKnown`/`StoredData` ignored after `didForceReady`; status needs `hasStoredData == false`, never `null`; `CancelStatus` only before the status start; app exit `max(1500, now)`, welcome `max(1600, now)`, rm → `now`.
- [ ] 8 s forces once, emits `ForceReady` only for a signed-in session, and an unknown session becomes signed out (OQ 7). `allowFirstLoad` is called exactly once.
- [ ] `stop()` cancels the session job, both deadline jobs and the welcome-deadline job; nothing calls `onChange` after `stop()`. No `GlobalScope`, no `runBlocking`, no swallowed `CancellationException`.
- [ ] All ported tests keep the Swift names and values; none was loosened (tolerances 1e-9 where Swift has them).

Endings and drawing
- [ ] The timeline is rebuilt from `opening` on each commit (D4); exit keys never stack; ready exit = 1500–1900 when ready is early (S3), not the board's 1700–2100.
- [ ] Removal happens on the first frame ≥ `exitEndSeconds`, via the frame clock, not `delay`/`postDelayed`; `launch` and `exit` end there.
- [ ] Draw path: no `TextMeasurer`, no `Typeface`/font load, no `Path()`/`Brush`/`Paint()` allocation; the status is a pre-rendered `ImageBitmap` plus three `drawCircle`s; the cache block does not read the timeline (no cache rebuild per commit).
- [ ] Dots run from mount (dot cycle anchored at t = 0, replica delay 0.2 s), status opacity 2000–2300 ms; rm: dots 1.0, 300 ms fades, no scale.
- [ ] The overlay recomposes at most once for the status (semantics) and never per frame.
- [ ] Interim welcome ending = container row only; nothing else of the welcome exit was invented.

Process policy
- [ ] `onActivityCreated` runs before `super.onCreate`; ProcessDeathRestore passes `null` to `super.onCreate`; Recreated passes the bundle.
- [ ] Only the claiming activity holds the system splash; Recreated/Warm/screen never hold it (try a locale change, a font-size change and a relaunch from recents during and after the splash: never stuck).
- [ ] A recreated activity mid-overlay resumes at the same `t0` (no replay, no restart of readiness); a finished activity mid-launch completes the launch.
- [ ] Warm fade: 300 ms, ease-out `(0, 0, 0.58, 1)`, then `remove()`; claimed overlay: `remove()` at once.
- [ ] The process token holds no secret and is the only thing `MainActivity` adds to the saved state.
- [ ] Content accessibility and pointer shielding follow `overlayActive`, so a Warm or `screen` activity (no overlay) is usable at once; a headless coordinator stops once the route is known.

Seams, variants and release
- [ ] `LaunchReadiness`, `SessionPresence`, `Gate`, `LaunchEnding` live in a core module (4e can implement them); the reducer and coordinator are pure (Konsist).
- [ ] Extra names and units match iOS (`cf.readyDelay` seconds, `cf.readyAt`/`cf.mainThreadStall` ms, ranges 0–60 / 0–60000, invalid → default).
- [ ] `StubReadiness`, `LaunchDiagnostics` parsing and `MainThreadStall` exist only in `src/perf` (debug + perf); release twins are no-ops; `ReleaseReadinessTest` proves it.
- [ ] Route placeholders show text only in debug, from `app_debug_route_x` referenced only in `app/src/debug`; no other copy was added; no board-less screen was designed.
- [ ] `SignedOutReadiness.hasStoredData()` is `true` (iOS); stub keys parse in one place (`LaunchDiagnosticsParser`) for debug and perf.

Tests and evidence
- [ ] Emulator tests seed `cf.reduceMotion=false` when they assert animated timing; no `Thread.sleep`/`SystemClock.sleep` in test code; waits are bounded; each < 10 s.
- [ ] The stall test really measures (planting a `delay`-based clock that stretches time must make it fail; ask the worker for the proof).
- [ ] Perf rows name device, build type and command; no emulator number is presented as a device claim.

Typical mistakes: starting the coordinator synchronously inside `onFirstDraw` (a draw); sampling from `withFrameNanos` of the second frame; ending `launch` at 1500 ms as in 3a; letting the frame loop stop for good after 1500 ms so a late ready never animates; reading `controller.handOff` in the `drawWithCache` cache block; restarting `LaunchCoordinator` on recreation; using `Animatable`/`animate*AsState` for the fade (scaled by the animator scale and not wall-clock); loading the Figtree font on the main thread; keeping 3a's `skipOverlay` process-wide; a placeholder with literal text in release; referencing `app_debug_route_x` from `src/main` (release does not compile); a coordinator test on `runTest`'s own scope (hangs to the 60 s cap); computing elapsed from a `withFrameNanos` timestamp instead of `nanoTime() − t0`, or requesting a frame before the first draw has set `t0`; drawing the dots before the status raster exists.

## 9. Open questions (proceed on the default; add each to plan.md as the next OQ-A numbers)

| # | Question | Default |
|---|---|---|
| OQ-3b-1 | Signed-out ending before Welcome (3c) exists. | Interim: the welcome exit's container row only (opacity 1 → 0 over W+700 → W+950, ease-out; rm → the ready exit), then the Welcome placeholder. 3c replaces it with `withWelcomeExit`. |
| OQ-3b-2 | Plan says the stub is "driven by `cf.readyAt` and a stub outcome extra". iOS drives the stub with `-cf.session/-cf.gate/-cf.stored/-cf.readyDelay` (seconds) and uses `-cf.readyAt` (ms) only for `-cf.mode slow`. | Port iOS exactly: the four stub extras plus `cf.mode`/`cf.readyAt` with iOS meanings and units. |
| OQ-3b-3 | Port `cf.mode` (fixed endings without readiness)? | Yes, `ready` and `slow`, debug and perf only; `welcome` waits for 3c. iOS keeps `-cf.mode ready` in Release for the device proof; Android uses the perf variant for that. |
| OQ-3b-4 | `cf.mainThreadStall` in which variants? | Debug and perf; never release. |
| OQ-3b-5 | Activity recreated (locale, font scale) while the overlay runs (OQ-3a-2). | Resume at the wall-clock position with the same `t0`, timeline and coordinator; no keep condition; no restart. If the status raster was prepared for another locale or font scale, prepare it again (the line is hidden until it is ready). |
| OQ-3b-6 | Activity finished during the splash. | Complete the launch at once (iOS `sceneDidDisconnect`); a later warm start shows the known route, or runs a headless coordinator (rm semantics) until the route is known. |
| OQ-3b-7 | How to detect a process-death restore. | A per-process token (`pid:startElapsedRealtime`) in the saved state; a mismatch or absence with a non-null bundle is a restore; `super.onCreate(null)`. |
| OQ-3b-8 | Route type before 4e. | `LaunchRoute` (Welcome, Today, Verify, Terms, Walkthrough) in `:feature:launch`; 4e maps it onto `Route`. |
| OQ-3b-9 | `PendingLaunchIntent` in `LaunchState`. | Omitted until 4e, which adds the field and the assertion in `endingIsChosenOnceAndIntentNeverRoutes`. |
| OQ-3b-10 | Placeholder look (no board). | Debug: the catalog text "Debug route: %@" (`app_debug_route_x`, drawn by `app/src/debug` `DebugRouteLabel`, OQ-0c-4) with id `route.<name>` (iOS OQ 8); release/perf: no text. Background: CfSky for Welcome, `background` token otherwise. |
| OQ-3b-11 | System bars after the hand-off (the spec only says "light until the hand-off ends"). | Welcome route: light icons (sky). Other routes: icons follow the resolved app theme (`SystemBarStyle.light` in light, `dark` in dark), applied once through `enableEdgeToEdge` at `handOffComplete`. |
| OQ-3b-12 | `CF_UI_TEST_PROFILE=screen` "skips the splash and readiness", but something must pick the route before 4e. | No overlay; a headless coordinator (rm semantics) picks the route from the stub or signed-out default; 4e's session seeds take over. |
| OQ-3b-13 | Who emits the `session` trace? | 4e (with `StoredDataReadiness`); 3b reserves cookie 5. |
| OQ-3b-14 | Process-death proof needs a second process. | `ProcessDeathLaunchTest` in `:benchmark` on the perf variant (UiAutomator, `am kill`, `monkey` relaunch), run with `make perf`; the restore branch itself is proven by `ProcessDeathRestoreTest` on Robolectric. |
| OQ-3b-15 | plan.md lists a "failure" readiness event; iOS has none. | No `Failure` event; failures resolve through the 8 s rule (unknown → signed out, signed in → forced). 4e may revisit if its session source needs one. |
| OQ-3b-16 | plan.md's stall test reads positions "through a debug `LaunchProbe`"; 3a's `compositionObserver` callback only counts compositions. | Read from the debug `controller.frameLog` ring buffers; leave `compositionObserver` unchanged. |
| OQ-3b-17 | Diagnostic `cf.mode` `ready`/`slow`: which route after removal? | None, as iOS (`SceneDelegate.swift:161–171` navigates only for `welcome`): the content stays empty and `reportFullyDrawn()` is not called. |
