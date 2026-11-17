# Phase 3c: Welcome

- **Branch:** `feat/phase-3c-welcome`, from the stack top once Phase **3b** is in it (3b brings 3a, 2, 0b, 0c, 0e). If 3b is missing, stop and report it. 4e may run in parallel in its own worktree; keep `:app` edits small (Tasks 5 and 7) because the orchestrator rebases one onto the other.
- **Plan entry:** `plan.md` "Phase 3c: Welcome" (l.1416–1461). Also: Decisions **A8**, **A11**, **A19**, **A21**; inherited **D6** (l.122), **D14** (l.129), "Plain sky, composition (2026-11-06)" (l.141), **OQ 10** (l.146), **OQ 11** (l.147); iOS **OQ 12** (the stale `GuestFlight` link; it has no row in plan.md); "Android spec: process, activity and launch sequence" step 6 (l.345); "Android spec: configuration and appearance" (Reduce Motion, l.355); "Android spec: window policy" (l.357–371); "Android spec: seeds and test hooks" (l.465); "Performance budget" rows tagged 3c (l.518–519); `AGENTS.md` "Performance", "Tests" and "Traps".
- **Revision 2026-11-16:** re-checked against the revised briefs for 0e, 2, 3a and 3b. The names used here (`LaunchHandOffTimeline`, `withWelcomeHandOff`, `HandOffPlan`, `onExitFrame`, `handOffComplete`, `RoutePlaceholder`, `LaunchDiagnostics`, `CfTheme.type.welcomeWordmark`, `CfButton(contentPadding)`, `Guards.assertAll`) are the ones those briefs define. If what landed differs, use what landed and say so in the commit body.
- **Checked 2026-11-16 (adversarial pass):** facts re-verified against iOS `f391a1fa`, the boards, plan.md, AGENTS.md and the 0b/0c/0e/2/3a/3b briefs. Changes: plan/iOS line numbers, `app_debug_route_x`, `LaunchRect.midX/midY`, the non-linear font-scale row height, the headless fallback, the size-change rebuild (`setFallbackTarget`), the interim-ending swap moved from Task 2 to Task 5 (commit atomicity), Task 5 no-op callbacks until Task 7, 3b's real `ReportDrawnWhen` predicate, `LaunchMode`/parser locations, the journey's `resetLaunchForTest()` and overlay wait, and the contrast record.
- **Tests:** every test follows `AGENTS.md` "Tests": cheapest tier, budgets, exact queries, no `Thread.sleep`/`SystemClock.sleep`, hang caps. Read that section before writing a test.
- **Tags:** **[F]** = fact verified in source (path given). **[R]** = rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios`, read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `KitT/` = `I/Packages/ChronoFlowKit/Tests`. `DC/` = `_design/canvas/`.
- **Path shorthands:** `…/launch/` = `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/`; `…/model/` = `…/launch/model/` (pure Kotlin: Konsist forbids `android.*`/`androidx.*`); `…/app/` = `app/src/main/kotlin/net/chronoflow/app/`; `…/perf/` and `…/rel/` as in the 3b brief (header).

---

## 1. Goal

When the user is signed out, the launch overlay flies its moon onto the moon glyph that Welcome measured, dissolves, and leaves Welcome behind. Welcome is the plain sky with the `Main` board composition at every window size, and the copy card is capped at 560 dp. On the first Welcome of the process only, a staggered intro plays, driven by the shared `LaunchClock`.

Done means:
- `withWelcomeExit`, `MoonTarget.fallback`/`contentBounds`, `MoonAnchorStore`, `LaunchIntro` and `WelcomeGeometry` are pure Kotlin ports that pass the ported iOS tests with the iOS values. 3b's interim welcome ending is gone.
- `WelcomeScreen` matches `Main` (light/dark, en/nl) on every profile. It passes the reference-free guards, has the iOS test ids and reading order, and scrolls when height is short.
- With W = 1600 ms, the overlay is removed (Welcome becomes interactive) at **2550 ms**, and the last intro item lands at **3200 ms**, both ± 1 frame. With Reduce Motion, the final Welcome shows after the 300 ms fade with no intro.
- In debug builds, Plan my trip and Sign in open route seams (`route.guestWizard`, `route.signIn`). In release and perf builds they do nothing, until 11a and 5a build their destinations.
- `cf.mode=welcome` (the iOS diagnostic welcome ending) works in debug and perf.

## 2. Inputs to read (in this order)

1. `plan.md`: the sections in the header; the Phase 3a and 3b entries (what you build on); the 4e, 5a and 11a entries (they replace your seams).
2. `AGENTS.md`: "Performance", "Tests", "Traps" (measured sizes stay in layout; animator scales; never a JUnit `Timeout` rule in Robolectric).
3. Earlier briefs, **as built** (read the code that landed; the briefs explain it):
   - `docs/phases/phase-3a-launch-overlay.md` §5.2 (layer table), §5.3 (opening), §5.5 (Reduce Motion), §5.8 (frame loop, `cf.freezeAt`), §6 (types: `LaunchTimeline` with `append`/`withoutExit`, `LaunchFrameSampler`, `LaunchClock`/`MediaLaunchClock`/`ManualLaunchClock`, `MoonTarget.Insets`, `LaunchRect`, `LaunchSize`, `LaunchController`, `LaunchTrace`, `LaunchOverrides`, `LaunchProbe`), Task 6 (`LaunchOverlay(controller, modifier)`, `onPlaced`, cache block).
   - `docs/phases/phase-3b-launch-readiness.md` §5.2 (reducer: `MoonAnchor`, `fallbackTarget`, `welcomeDeadline`, `CommitExit(Welcome, at, target)`), §5.3 (coordinator), §5.4 (`LaunchHandOffTimeline.build`, interim `withWelcomeHandOff`), §5.7 (`LaunchController.startReadiness`, `onLaunchChange`, `handOff`/`HandOffPlan`, `onExitFrame`, `handOffComplete`, `route`), §5.8 (`CfRoot`, `RoutePlaceholder`, `ReportDrawnWhen`), §5.9 (`LaunchDiagnostics`, `cf.mode`, `cf.session`), §5.11 (trace cookies: `intro` = **6**, reserved), OQ-3b-1, OQ-3b-11.
   - `docs/phases/phase-2-design-system.md` Task 5 (`warmFonts`), Task 8 (`CfSky.brush(dark)`), Task 11 (`CfButton` signature at l.358–365, Welcome usage at l.386), `CfType` (l.165: `welcomeWordmark`, `eyebrow`, `headline`, `copy`, `primaryButton`, `secondaryButton`).
   - `docs/phases/phase-0e-test-harness.md` T6 (`LocalSafeInsets`, `ProfileRule(profile, language, dark)`, `ProfileHost`), T8 (`Guards`), `BoardCapture` (l.379–384: `cf.captureOnly` is already a prefix match).
4. Research: `docs/research/05-launch-auth.md` §3.4 (the welcome ending), §4.1–§4.5 (Welcome), §15.2, §15.12; `docs/research/12-built-apps.md` §4.1. Reference screenshots: `docs/research/screens/ios/01-launch-t04000ms.png` (moon landed top-left), `01-launch-t06000ms.png` (Dutch Welcome), `15-welcome-en.png` (English Welcome).
5. Boards (read-only):
   - `DC/Main.dc.html`: Welcome. Props `theme` (light/dark) and `intro` (boolean; a 5200 ms loop. Item keyframes `cfwi-w/-e/-h/-c/-b`: for example, `cfwi-w` holds at 0 until 44.23 % (= 2300 ms) and lands at 52.88 % (= 2750 ms)). **Ignore** its `pattern-dots` layer (user, 2026-11-06) and its `href="GuestFlight.dc.html"` (stale, iOS OQ 12).
   - `DC/SplashWelcome.dc.html`: a wrapper around `<dc-import name="SplashScreen" mode="welcome" hint-size="390px,844px">`. Read the `-w` keyframes in `DC/SplashScreen.dc.html`: `cfsp-icon-w` lifts to −28 at 21.15 % and flies to `translate(-157px,-314px) scale(0.27)` at 44.23 % (2300 ms); `cfsp-out-w` fades the container 1 → 0 from 44.23 % to 49.04 % (2300 → 2550 ms). The board stars (`cfsp-stars-w`) are removed (S2).
   - `DC/theme.css`: l.47–53 (light sky tokens), l.97–103 (dark), l.157–160 (`.cf-sky` sets `--dawn-text: var(--dawn)`), l.134–146 (`cf-press`).
6. iOS references (`I/` at `f391a1fa`):
   - `plan.md:792` "Phase 5: Welcome, intro and moon hand-over (`feat/phase-5-welcome`)"
   - `plan.md:819` "Phase 5 fix round 1" (eyebrow clipping; the five contrast pairs l.845–849)
   - `plan.md:852` "Phase 5 user decisions and fix round 2 (2026-11-06)" (font-size cap)
   - `plan.md:872` "Phase 5 fix round 3 (2026-11-06)" (a frozen intro installs no live animation; claim on first appearance)
   - `plan.md:892` "User decision: board composition on every height (2026-11-06)"
   - `plan.md:901` "User decision: plain sky without a dot pattern (2026-11-06)"
   - `plan.md:930` "Phase 5 review fixes (after Phase 5)" (exact 48/48 headline line pitch; reconnect and Reduce Motion regressions)
   - `plan.md:221` "Endings", `plan.md:243` "Welcome intro (first launch of the process only)", `plan.md:258` "Welcome layout (on `cf-sky`)", `plan.md:277` "Reduce Motion", `plan.md:289` "Accessibility"
   - `Kit/Welcome/{WelcomeView,WelcomeLayout,WelcomeBrand,IntroItem,WelcomeNavigator,MoonAnchorEnvironment}.swift`, `Kit/Welcome/Resources/Localizable.xcstrings`
   - `Kit/AppCore/{LaunchIntro,MoonAnchorStore,MoonTarget,Route}.swift`, `Kit/AppCore/LaunchReadiness.swift` l.57–190
   - `Kit/Launch/LaunchTimeline+Welcome.swift`
   - `Kit/DesignSystem/{WelcomeTokens.generated,WelcomeText,Buttons}.swift`, `Typography.swift` l.95–106
   - Wiring (behaviour only, not UIKit): `I/App/SceneDelegate.swift` l.90–175 (`allowsIntro`, the anchor `onChange`, the diagnostic welcome mode with intro start 2300), l.425–470 (`runReadiness`: `fallbackTarget`, intro created at `exitStart + 700`), l.555–566 (`scheduleIntroSignpost`), l.598–610 (`updateDiagnosticMoon`); `I/App/LaunchArguments.swift` l.60–80 (`fallbackRowHeight`, `timeline(opening:safeAreaInsets:rowHeight:)`); `I/App/Debug/RoutePlaceholder.swift` (`route.<rawName>`, "Debug route: %@").
   - Tests to port: `KitT/AppCoreTests/{LaunchIntroTests,MoonAnchorStoreTests,MoonTargetTests}.swift`, `KitT/LaunchTests/WelcomeTimelineTests.swift`, `KitT/WelcomeTests/WelcomeGeometryTests.swift`, `I/AppTests/LaunchArgumentsTests.swift` (`testWelcomeFallbackUsesTheCurrentTextSizeAndAllSafeInsets` and the `welcome` case of `testModeTimelinesAndAsymmetricSafeAreaFallback`). Behaviour references (not ported 1:1): `I/AppTests/WelcomeHandOverTests.swift`, `I/RenderTests/WelcomeRenderingTests.swift`, `I/AppUITests/{WelcomeTests,LaunchToWelcomeTests}.swift`.
7. **Check what earlier phases delivered before Task 1.** If an item is missing, stop and report it.
   - 0b: `WelcomeTokens` in `CfDimens.generated.kt` (`const val Float`: `topPadding 64, gutter 24, bottomPadding 40, groupGap 16, actionGap 12, brandGap 8, moonSize 28, moonStroke 2, moonViewBox 24, eyebrowMargin 56, cardVerticalPadding 16, cardHorizontalPadding 20, cardRadius 22, buttonHeight 52, introRise 14`) and `TypeMetrics.sectionTitle` in `CfTypeScale.generated.kt` (`CfTypeMetrics(size 20, lineHeight 28, letterSpacingEm −0.01)`; `lineHeight` is `Float?`).
   - 0c: the six Welcome strings (§5.6) in `strings_ios.xml`, en and nl; the debug-only App string "Debug route: %@" as `net.chronoflow.core.ui.R.string.app_debug_route_x` in `core/ui/src/debug/res/values{,-nl}/strings_ios_debug.xml` (OQ-0c-4; 3b's `DebugRouteLabel` uses it; release `R` has no such field).
   - 0e: `profiles.json`/`DeviceProfiles`, `ProfileRule`, `ProfileHost`, `LocalSafeInsets`, `Guards`, `BoardCapture`, tier annotations in `net.chronoflow.core.testing.tiers`, `ScreenTest`, `StartupBenchmark`, `check-benchmarks.py`.
   - 2: `ChronoFlowTheme`, `CfTheme.colors/type`, `LocalReduceMotion`, `CfSky.brush(dark)`, `CfButton(…, style = Dawn|OnSky, fillWidth, minHeight, textStyle, contentPadding)`, `cfPress`, `LucideGlyphs.Moon`, `warmFonts()`.
   - 3a: the model types above, `LaunchOverlay`, `CfRoot`, `LaunchTrace`, `LaunchProbe`, `cf.freezeAt`, `cf.reduceMotion`.
   - 3b: the reducer with `LaunchEvent.MoonAnchor`, `LaunchState.fallbackTarget` (the zero rect until now), `welcomeDeadline`; `LaunchCoordinator`; `withReadyExit`, `withStatus`; `LaunchHandOffTimeline` with the interim `withWelcomeHandOff` (`LaunchTimelineWelcomeInterim.kt`); `LaunchController.startReadiness/onLaunchChange/onExitFrame`, `handOff`, `route`, `handOffComplete`; `RoutePlaceholder`; `LaunchDiagnostics` (`cf.mode=welcome` is parsed but deferred; test `welcomeModeIsDeferred`); `StubReadiness` with `cf.session=out`.

**API endpoints:** none. This phase does no network or disk work.

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| The real Sign in screen; `Route`, `RootNavigator`, the Nav3 `NavDisplay` | 5a; 4e |
| The guest wizard behind Plan my trip; the quiet guest-draft recovery after the intro (iOS `SceneDelegate.swift:568–590`) | 11a |
| The sign-out landing (it goes straight to Sign in, never to Welcome, with no intro) | 4e / 5a |
| Real session readiness and the `session` trace (cookie 5) | 4a–4e |
| A wide Welcome layout. **No board exists**; only the 560 dp copy-card cap from the window policy applies | — |
| Baseline/startup profile regeneration, if `make baseline-profile` is not in the stack yet | 1e / 13 (write "pending" in `docs/perf-startup.md`) |
| Contrast fixes for the five Welcome pairs (there is no corrective design) | design question, OQ-3c-8 |

Add no copy: every visible string already exists (§5.6). Add no state without a board (loading, error) to Welcome.

## 4. Platform facts this phase relies on [F unless marked]

- Compose `animate*AsState`, `Animatable`, `AnimatedVisibility` and transitions follow the animator duration scale. Functional emulator runs set that scale to 0 (`AGENTS.md` Traps). So the intro samples the `LaunchClock`, as the overlay does (3a §5.8), and never uses those APIs.
- `Modifier.verticalScroll` measures its child with `maxHeight = Infinity` and keeps the incoming `minHeight`. After `fillMaxSize()`, the child gets `minHeight` = the viewport height [R: prove it in `WelcomeLayoutTest`; the fallback is in Task 3].
- `LayoutCoordinates.positionInWindow()` and the semantics `boundsInRoot` include `graphicsLayer` translations. `boundsInWindow()` is clipped to the visible bounds, so do not use it for the anchor.
- A `graphicsLayer` does not clip by default, so glyphs that overflow a 48 sp line box (Fraunces, `lineHeight = 48.sp`, `Trim.None`) still draw.
- The overlay and Welcome share one edge-to-edge window, and the overlay fills it from `(0, 0)`. So window coordinates in dp are the overlay's `LaunchLayout` coordinates (`05 §15.2`).
- `Modifier.onPlaced` runs in the layout pass, before the first draw of that frame (3a relies on this for `overlayPlaced`).
- `with(density) { x.sp.toDp() }` applies the font scale, including the non-linear scaling on API 34+. The root has already capped that scale at 1.3.

## 5. The Welcome spec

All geometry is in **dp** (iOS pt → dp). `w × h` is the window size, and insets come from `LocalSafeInsets`. Times are in ms from `t0` (3a). The curves are 3a's `CubicBezier` constants: `easeOut (0,0,0.58,1)`, `easeInOut (0.42,0,0.58,1)`, `intro (0.2,0.8,0.2,1)` and `linear`.

### 5.1 Where the build differs from the boards [F `I/plan.md:88–100`, `05 §2.2`]

| Board says | Build this | Source |
|---|---|---|
| `Main`: `pattern-dots` at opacity 0.2 | **No dots**; one `CfSky` gradient | user, 2026-11-06 |
| `SplashScreen` `-w`: a stars track | No stars | S2 |
| Moon flight ease-out (`cfsp-icon-w`) | **easeInOut**, one straight line, one progress for both position and scale | S4 |
| Flight target `translate(-157px,-314px) scale(0.27)` (fixed) | Welcome's **measured** glyph rect, else `MoonTarget.fallback` | D1c, `I/plan.md:265` |
| Stagger "about 100 ms" | 2300/2400/2450/2600/2700/2750 | S6 |
| Plan my trip → `GuestFlight` | Guest wizard (11a); a seam until then | iOS OQ 12 |
| Fixed padding 64/24/40 | `top = max(safeTop + 16, 64)`, sides `24 + inset`, `bottom = max(safeBottom, 40)` | iOS OQ 10 (RN rule) |
| Fixed 390 width | Copy card at most 560 dp wide, centred (plan window policy) | plan l.361, OQ-3c-1 |

### 5.2 Welcome ending of the overlay [F `Kit/Launch/LaunchTimeline+Welcome.swift`, `I/plan.md:229–241`]

`fun LaunchTimeline.withWelcomeExit(start: Duration, size: LaunchSize, moonTarget: LaunchRect): LaunchTimeline` (file `…/model/LaunchTimelineWelcome.kt`):
1. `require(size == this.size)`; `require(moonTarget.width > 0 && moonTarget.width, midX and midY are finite)`. **These checks run before the Reduce Motion branch**, as in iOS. 3a's `LaunchRect` has `minX/maxX/minY/maxY` but no `midX`/`midY`: add `val midX get() = x + width / 2` and `val midY get() = y + height / 2` to `LaunchRect` in this task (pure, no other change).
2. If `reduceMotion`: return `withReadyExit(start)` (3b: a 300 ms container fade, ease-out, no scale).
3. Else: take `withoutExit()`, set `exit = max(1.6, start in seconds)`, then `append` these rows in this order (columns: key path, keys `(start, end)`, target value, curve):

| Layer | Key path | Start → end (s) | To | Curve |
|---|---|---|---|---|
| band | opacity | exit → exit + 0.7 | 0 | easeOut |
| sun, then halo | opacity | exit → exit + 0.6 | 0 | easeOut |
| sun, then halo | transform.translation.y | exit → exit + 0.6 | 120 | easeOut |
| path | opacity | exit → exit + 0.4 | 0 | linear |
| wordmark | opacity | exit → exit + 0.3 | 0 | easeOut |
| wordmark | transform.translation.y | exit → exit + 0.3 | −6 | easeOut |
| moon | position | exit → exit + 0.7 | `(target.midX, target.midY)` | easeInOut |
| moon | transform.scale | exit → exit + 0.7 | `target.width / 104` | easeInOut |
| container | opacity | exit + 0.7 → exit + 0.95 | 0 | easeOut |

(The iOS loop appends sun opacity, sun translation, halo opacity, halo translation, in that order.) The container scale stays `[1]`. 3b's `onExitFrame` removes the overlay at `exit + 0.95`, on the frame clock. **That is "Welcome interactive".** With W = 1600: the flight runs 1600 → 2300, the dissolve 2300 → 2550, and removal is at 2550. On 390 × 844 with the target `(24, 64, 28, 28)`, the moon goes from `(195, 394)` to `(38, 78)` and its scale from `0.86` to `28/104`. The container opacity at 2425 ms is `1 − 0.6846431874`.

**Replace the interim ending** (Task 5, not Task 2: until Task 5 sets a non-zero `fallbackTarget`, a signed-out launch would fail `withWelcomeExit`'s `width > 0` check and break 3b's signed-out tests). Delete `…/model/LaunchTimelineWelcomeInterim.kt` (`withWelcomeHandOff`). In `LaunchHandOffTimeline.build`, the `Welcome` branch becomes `opening.withWelcomeExit(exitStart, opening.size, state.moonTarget ?: state.fallbackTarget)`, as the KDoc 3b left there says.

### 5.3 Moon target: measured, else fallback [F `Kit/AppCore/MoonTarget.swift`, `MoonAnchorStore.swift`, `I/App/SceneDelegate.swift:90–102,336–344,425–435`]

- **`MoonAnchorStore`** (port, `…/model/MoonAnchorStore.kt`; plain fields, **not** snapshot state; main thread only) has `var frame: LaunchRect?` (private set) and `var onChange: ((LaunchRect) -> Unit)?`. `update(r)` ignores a rect with any non-finite value, `width ≤ 0`, `height ≤ 0`, or a rect equal to `frame`. Otherwise it stores the rect and calls `onChange`.
- **Measured anchor:** Welcome's moon glyph reports its **unclipped** window rect in dp to `store.update`. The rect is `positionInWindow()` plus `size`, divided by `density`, read in `Modifier.onGloballyPositioned`.
- **`MoonTarget.fallback(size, safeAreaInsets, rowHeight)`** (add it to 3a's `…/model/MoonTarget.kt`): `require(rowHeight finite && > 0)`; `content = contentBounds(size, insets)`; return `LaunchRect(content.minX, content.minY + max(28, rowHeight)/2 − 14, 28, 28)`.
- **`MoonTarget.contentBounds(size, insets)`**: `require(w, h finite && > 0)`; `top = max(insets.top + 16, 64)`; `bottom = max(insets.bottom, 40)`; `LaunchRect(insets.left + 24, top, max(0, w − left − right − 48), max(0, h − top − bottom))`. This is exactly iOS, with **no 560 cap** (the cap applies to the card only, §5.4).
- **`rowHeight`** [F `LaunchArguments.fallbackRowHeight`; R Android form]: `WelcomeMetrics.fallbackRowHeight(density: Density): Double = max(28.0, with(density) { TypeMetrics.sectionTitle.lineHeight!!.sp.toDp().value.toDouble() })` (`lineHeight` is `Float?` in 0b; `sectionTitle` always has one). This is the wordmark line height at the capped font scale: 28 at scale 1. At 1.3 it is whatever the density's font-scale converter gives: 36.4 with a linear `Density(d, 1.3f)` (plain JVM), and less on API 34+ devices and Robolectric `sdk = 36`, where `LocalDensity` applies Android's non-linear converter to 28 sp. Never hard-code the 1.3 value; tests that need a fixed row pass it explicitly. Put it in `…/launch/welcome/WelcomeMetrics.kt`, using the generated name for that metric. Write 28 as `WelcomeTokens.moonSize`, not as a literal.
- **Where the fallback comes from [R]:** `LaunchOverlay` reports its geometry from the same `Modifier.onPlaced` callback that already calls 3a's `controller.markOverlayPlaced()`: `controller.onOverlayGeometry(sizeDp, insets, rowHeight)`. `insets` is `LocalSafeInsets` converted to `MoonTarget.Insets` in dp, and `rowHeight` uses `LocalDensity`. Both are read in composition and captured by the lambda. `onOverlayGeometry` writes **plain fields only**. `startReadiness` (3b) then builds the coordinator with `LaunchState(fallbackTarget = MoonTarget.fallback(size, insets, rowHeight))`. When this process claimed the overlay, use `check(geometry != null)`: `onPlaced` always runs before the first draw, and the first draw starts readiness.
- **Headless coordinator** (3b: a Warm activity with an unknown route, or the `screen` profile; no overlay, so no geometry): seed `fallbackTarget = LaunchRect(0.0, 0.0, WelcomeTokens.moonSize.toDouble(), WelcomeTokens.moonSize.toDouble())` with a KDoc saying nothing draws it. As 3b landed, `startHeadless()` (`LaunchController.kt`) uses its own `onChange` that publishes the route and stops the coordinator; it ignores effects and never rebuilds `handOff`, so no Welcome exit timeline is built headlessly (iOS likewise has no overlay, hence no exit, on that path). Keep that 3b behaviour unchanged. The seed exists only to keep the invariant that `fallbackTarget` is never the zero rect on any path (§10 checklist), so a future caller of `withWelcomeExit` cannot trip its `width > 0` precondition.
- **The fallback is never the zero rect** once 3c lands, because `withWelcomeExit` requires `width > 0`, even under Reduce Motion.
- **Wiring:** `store.onChange = { coordinator?.send(LaunchEvent.MoonAnchor(it)) }`. The reducer (3b) accepts an anchor only while `now < exitStart`. It re-emits `CommitExit(Welcome, W, target)` when the anchor changes before W, and it commits with `fallbackTarget` at `W − 50 ms` if no anchor arrived. After `ExitCompleted` the coordinator is stopped, so later updates do nothing.
- **Window size change before removal** (a fold or resize) [R]: if `onOverlayGeometry` sees a new size while a coordinator exists, it posts (`scope.launch`, never inside layout) a rebuild. 3b does not keep an opening per size, so the rebuild: (1) replaces the controller's `opening` with `LaunchTimeline.opening(newSize, reduceMotion)`; (2) calls a new `LaunchCoordinator.setFallbackTarget(rect)` (3c adds it: main thread, returns if stopped, `state = state.copy(fallbackTarget = rect)`, no effects, no `onChange`) with the fallback for the new size, so a later `CommitExit` uses it; (3) rebuilds `handOff = HandOffPlan(LaunchFrameSampler(LaunchHandOffTimeline.build(opening, coordinator.state)), …, revision + 1)` exactly as 3b's `onLaunchChange` does. Welcome re-measures and sends a new anchor, which is accepted only before W. `t0` is kept (3a rule).

### 5.4 Layout [F `DC/Main.dc.html`, `Kit/Welcome/WelcomeView.swift`, `WelcomeLayout.swift`, `WelcomeBrand.swift`, `WelcomeTokens.generated.swift`; R the 560 dp card cap]

**`WelcomeGeometry(size: LaunchSize, insets: MoonTarget.Insets)`** (port, `…/model/WelcomeGeometry.kt`, Doubles; values from `WelcomeTokens`):
- `top = max(insets.top + groupGap, topPadding)`; `bottom = max(insets.bottom, bottomPadding)`; `leading = insets.left + gutter`; `trailing = insets.right + gutter`;
- `content = LaunchRect(leading, top, max(0, w − leading − trailing), max(0, h − top − bottom))`; this equals `MoonTarget.contentBounds`;
- `groups(headingHeight, actionsHeight)` returns `WelcomeGroups(heading, actions)`: `heading = (content.minX, content.minY, content.width, headingHeight)`; `actions = (content.minX, max(heading.maxY + gutter, content.maxY − actionsHeight), content.width, actionsHeight)`.
- On 390 × 844 with zero insets: `content = (24, 64, 342, 740)`; `groups(276, 296)` gives a heading top of 64 and an actions bottom of 804.

**Tree.** One custom `Layout` stands in for the iOS `VStack` + `Spacer(minLength: 24)`:
```
Box(fillMaxSize)                                        // sky: drawBehind { drawRect(CfSky.brush(dark)) }; full window, ignores insets, no semantics, does not scroll
 └ WelcomeColumn(Modifier.fillMaxSize().verticalScroll(rememberScrollState()))   // custom Layout; no scrollbar
     ├ heading: Column(spacedBy 16)
     │   ├ brand Row(spacedBy 8, CenterVertically): moon glyph 28×28 (anchor) + wordmark   [introItem(0) on the wordmark only]
     │   ├ eyebrow (padding top 56)                                                         [introItem(1)]
     │   └ headline                                                                         [introItem(2)]
     └ actions: Column(spacedBy 12)
         ├ copy card (12 dp below it, outside the card)                                     [introItem(3)]
         ├ Plan my trip                                                                     [introItem(4)]
         └ Sign in                                                                          [introItem(5)]
```
- **MeasurePolicy** (all in layout; it writes no state):
  1. `viewport = (constraints.maxWidth, constraints.minHeight)`, px → dp.
  2. Read the insets from `LocalSafeInsets` (captured in composition; convert with `getTop(density)` and the others inside the measure block).
  3. `g = WelcomeGeometry(viewport, insets)`.
  4. Measure both groups with `Constraints.fixedWidth(g.content.width px)`.
  5. Place the heading at `(g.leading, g.top)` and the actions at `g.groups(…).actions.minY`.
  6. Layout height = `max(viewport.h, g.top + headingH + gutter + actionsH + g.bottom)`.
- **Moon glyph:** `Image(rememberVectorPainter(LucideGlyphs.Moon.imageVector()), contentDescription = null, colorFilter = ColorFilter.tint(CfTheme.colors.dawn), modifier = Modifier.size(moonSize).reportMoonAnchor(store))`. Stroke 2 in the 24 viewport (= 2.33 dp), round caps and joins. No glow, never animated, no semantics.
- **Wordmark:** `R.string.welcome_chronoflow`, `CfTheme.type.welcomeWordmark` (Fraunces opsz20, 20/28, −0.01 em), color `skyTitle`. Tag `welcome.wordmark`.
- **Eyebrow:** `stringResource(R.string.welcome_jet_lag_planner).uppercase(<the configuration's first locale>)`, which gives "JET-LAG PLANNER" / "JETLAGPLANNER". `CfTheme.type.eyebrow` (Figtree 600, 12/16, +0.1 em), color `dawn` (inside `cf-sky`, `--dawn-text` = `--dawn`). No `maxLines`, never ellipsized (iOS fix round 1 clipped it). Tag `welcome.eyebrow`.
- **Headline:** `R.string.welcome_plan_your_clock_shift_for_your_next_trip`, `CfTheme.type.headline` (Fraunces opsz48, 48/48, −0.01 em), color `skyTitle`, `Modifier.semantics { heading() }`. The line pitch is exactly the scaled line height (48 sp at the capped scale). Glyphs may overflow the line boxes and must not be clipped. Tag `welcome.headline`.
- **Copy card:** `R.string.welcome_tell_us_your_flights_and_how_you_3cbe87`, `CfTheme.type.copy` (Figtree 400, 16/24), color `skyCopy`. Padding 16 vertical / 20 horizontal. Background `skyCopyBackground` (α 0.9) in `RoundedCornerShape(cardRadius 22)`. Text start-aligned. Tag `welcome.copy`. **Width:** `Modifier.widthIn(max = 560.dp).fillMaxWidth()`, centred horizontally in the actions column, so it is the full content width below 600 dp and at most 560 dp, centred, above [R plan window policy; the 560 and the 600 live in a `WelcomeWindowPolicy` object with a KDoc pointing at plan "Android spec: window policy"]. The intro layer wraps the card (background and text together).
- **Plan my trip:** `CfButton(text = stringResource(R.string.welcome_plan_my_trip), onClick = onPlanMyTrip, style = CfButtonStyle.Dawn, fillWidth = true, minHeight = WelcomeTokens.buttonHeight.dp, textStyle = CfTheme.type.primaryButton, contentPadding = PaddingValues(horizontal = WelcomeTokens.cardHorizontalPadding.dp, vertical = WelcomeTokens.brandGap.dp))` (Phase 2 l.378). Pill shape, `cfPress`, `Role.Button`. Tag `welcome.plan`.
- **Sign in:** the same with `style = CfButtonStyle.OnSky`, `textStyle = CfTheme.type.secondaryButton` (Figtree 500), label `R.string.welcome_sign_in`. Tag `welcome.signIn`.
- **Board numbers on 390 × 844, zero insets, en, scale 1** [F `I/plan.md:263`]: moon row y 64–92 (moon `(24, 64, 28, 28)`); eyebrow 164–180; headline top 196 (3 lines × 48 = 144); Plan my trip 688–740; Sign in 752–804; x 24, width 342.
- **Wide (`w ≥ 600`):** only the copy card changes (capped and centred). The heading and buttons keep the full content width, as on iOS. There is no wide board; build nothing else.
- **Short heights:** the column scrolls, and the actions follow the heading at +24 (`landscapeSideNav`; `small` × `largeFont` × nl).
- **Font scale:** the root cap (1.3) applies; `largeFont` must render exactly like `pixel8` at 1.3.

### 5.5 Intro [F `Kit/AppCore/LaunchIntro.swift`, `Kit/Welcome/IntroItem.swift`, `Kit/Welcome/WelcomeView.swift`, `I/plan.md:243–256`, `I/App/SceneDelegate.swift:92,162–170,447–451,558–566`]

- **`LaunchIntro(start: Duration, clock: LaunchClock, frozenTime: Duration? = null)`** (port, `…/model/LaunchIntro.kt`, main thread):
  - `hasPlayed` (private set); `claim(): Boolean` returns true once, then false.
  - `companion fun offset(index: Int): Duration` = `[0, 100, 150, 300, 400, 450]` ms, with `require(index in 0..5)`.
  - `progress(index, time) = CubicBezier.intro.value(clamp((time − start − offset(index)) / 450 ms, 0, 1))`.
  - `delay(index): Double = (start + offset(index) − clock.now)` in seconds.
  - Nothing in it allocates per call.
- **`allowsIntro`** [R, iOS `SceneDelegate.swift:92`] = this activity claimed the overlay (first activity of the process: Cold or ProcessDeathRestore, not the `screen` profile) **and** `LaunchController`'s captured Reduce Motion is false.
- **Who creates it:** `LaunchController.onLaunchChange`, once per process. It creates the intro when `state.route == Welcome`, `allowsIntro` holds, `launchIntro == null` and `state.exitStart != null`: `LaunchIntro(start = exitStart + 700 ms, clock = coordinator clock)` (no frozen time on this path). Create it **before** publishing `route`, so Welcome's first composition sees it. At the same moment, schedule the `intro` trace (below). The diagnostic welcome mode (§5.9) creates it differently.
- **Who claims it:** `WelcomeScreen`, on its first composition:
  ```kotlin
  val reduceMotion = LocalReduceMotion.current
  val presented = remember(intro) { intro?.takeIf { !it.hasPlayed && !reduceMotion } }
  SideEffect { presented?.claim() }                         // claim after the frame commits; never decide again
  ```
  A later Welcome (back from a seam, a recreated activity, the `screen` profile) sees `hasPlayed` and shows the final screen at once. That matches iOS `onDisappear { activeIntro = nil }`.
- **Item `i`:** `p = presented.progress(i, now)`; alpha = `p`; translationY = `introRise (14 dp) × (1 − p)`. Both are applied in `Modifier.graphicsLayer { … }`, and that block is the **only** reader of `now`. Indices: 0 wordmark (not the moon), 1 eyebrow, 2 headline, 3 copy card, 4 Plan my trip, 5 Sign in. When `presented == null`, add no layer at all.
- **Clock:** `now` is a `mutableLongStateOf(clock.now in nanoseconds)` remembered with `presented`. Then:
  ```kotlin
  LaunchedEffect(presented) {
      val intro = presented ?: return@LaunchedEffect
      if (intro.frozenTime != null) return@LaunchedEffect   // frozen: now = frozenTime, no loop
      intro.clock.sleepUntil(intro.start)                  // nothing moves before the start
      while (true) {
          withFrameNanos { now.longValue = intro.clock.now.inWholeNanoseconds }
          if (now.longValue >= (intro.start + 900.milliseconds).inWholeNanoseconds) break
      }
  }
  ```
  Nominal landings: 2750, 2850, 2900, 3050, 3150, **3200**. If `start` has already passed when Welcome composes, each item shows its current progress.
- **Frozen** (`frozenTime != null`, only through the diagnostic mode with `cf.freezeAt`): `now = frozenTime`, no loop, no frame requests (iOS fix round 3).
- **Interactive** = the overlay is removed (W + 950). The buttons are clickable from then on, even while they still fade in (iOS parity, OQ-3c-6).
- **`intro` trace** (async section, cookie **6**, reserved by 3b): `LaunchController` launches it in its own process-level scope (`clock.sleepUntil(start)`, `beginIntro()`, `clock.sleepUntil(start + 900 ms)`, `endIntro()`, with `endIntro()` also in a `finally`). It never runs from Welcome, so it still ends when Welcome leaves composition. It begins and ends at most once per process. It is not scheduled in diagnostic mode (iOS parity).
- **Reduce Motion:** no `LaunchIntro`. The overlay plays `withReadyExit` at `exitStart = session time` (a 300 ms fade), and Welcome is final from its first frame. If `LocalReduceMotion` becomes true later (Welcome composes after Reduce Motion was turned on mid-launch), Welcome does not claim the intro and shows the final screen.

### 5.6 Copy [F `Kit/Welcome/Resources/Localizable.xcstrings`; names from the 0c slug rule. Confirm each in `strings_ios.xml`/`names.lock.json` and use the generated name if it differs]

| Resource | en | nl |
|---|---|---|
| `welcome_chronoflow` | ChronoFlow | ChronoFlow |
| `welcome_jet_lag_planner` | Jet-lag planner | Jetlagplanner |
| `welcome_plan_your_clock_shift_for_your_next_trip` | Plan your clock shift for your next trip | Plan de verschuiving van je biologische klok voor je volgende reis |
| `welcome_tell_us_your_flights_and_how_you_3cbe87` | Tell us your flights and how you sleep. You get a day-by-day plan for sleep, light and caffeine that helps your body clock shift. | Vertel ons welke vluchten je neemt en hoe je slaapt. Je krijgt een dagplan voor slaap, licht en cafeïne dat je biologische klok helpt verschuiven. |
| `welcome_plan_my_trip` | Plan my trip | Plan mijn reis |
| `welcome_sign_in` | Sign in | Inloggen |
| `app_debug_route_x` (App, debug only, `core/ui/src/debug`; 3b's `DebugRouteLabel`) | Debug route: %1$s | Debugroute: %1$s |

### 5.7 Accessibility, appearance, haptics [F `I/plan.md:285–294`; R the system bars]

- Reading order (TalkBack follows geometry): wordmark, eyebrow, headline, copy, Plan my trip, Sign in. The moon glyph and the sky have no semantics. Only the headline is a heading. Both buttons have `Role.Button` and a click action.
- While the overlay shows, `CfRoot` keeps the content under `hideFromAccessibility()` (3b). After removal the flag is gone, and TalkBack should land on the wordmark (OQ-3c-3).
- Colors come from `CfTheme.colors`; Welcome follows `cf.settings.theme`, while the overlay keeps its captured appearance (OQ-3c-11). Welcome code never uses `isSystemInDarkTheme()`, a hex literal or `MaterialTheme.colorScheme`.
- **System bars on Welcome** (this amends OQ-3b-11 for the navigation bar only): the status-bar icons stay light, because the sky top is dark in both themes. The navigation-bar icons follow the sky bottom (`skyHorizon` L 0.75 in light, 0.55 in dark): **dark icons in the light theme, light icons in the dark theme**. Apply this through 3b's post-hand-off bar mechanism, not a second one (OQ-3c-9).
- No haptics (Phase 2's Konsist rule, which 3a relies on, covers `:feature:launch`).

### 5.8 Timing summary (W = 1600) [F]

| Moment | ms | Source |
|---|---|---|
| Moon flight | 1600 → 2300 | §5.2 |
| Intro start (`intro` begins) | 2300 | W + 700 |
| Dissolve | 2300 → 2550 | §5.2 |
| **Welcome interactive** (overlay removed, `exit` ends, `reportFullyDrawn()`) | **2550** | W + 950 |
| **Last item lands** (`intro` ends) | **3200** | W + 1600 |
| Late session at 4000 | W = 4000: interactive 4950, last item 5600 | `W = max(1600, session known)` |
| Unknown session, forced at 8 s | interactive 8950, last item 9600 | 3b step 7 |
| Reduce Motion, session at s | interactive s + 300, no intro | §5.5 |
| Diagnostic `cf.mode=welcome` | interactive 2550, intro start 2300 (fixed) | §5.9 |

### 5.9 Diagnostic welcome mode [F `I/App/LaunchArguments.swift:67–80`, `I/App/SceneDelegate.swift:91–101,161–170,598–610`]

3b parses `cf.mode=welcome` but treats it as absent (with a `Log.w`). 3c enables it in debug and perf; release keeps `LaunchDiagnostics.None` because only `…/perf/LaunchDiagnosticsParser.kt` (debug and perf) parses it, and `…/rel/LaunchDiagnosticsParser.kt` stays unchanged. Where 3b put things [F 3b §5.9]: `enum LaunchMode { Ready, Slow }` is in `…/launch/LaunchOverrides.kt` (`:feature:launch`); the plain-data `LaunchDiagnostics` is in `app/src/main/kotlin/net/chronoflow/app/LaunchDiagnostics.kt` (`:app`, unreachable from `:feature:launch`).
- **Mode:** add `Welcome` to `LaunchMode`; in the perf parser map `welcome` → `LaunchMode.Welcome` and drop the `Log.w`.
- **Timeline** (port of `LaunchArguments.timeline`, `I/App/LaunchArguments.swift:67–80`): find the function 3b's `LaunchModeTimelineTests` calls for the `ready`/`slow` timelines and add the `welcome` branch there, with the iOS parameters `(opening, safeAreaInsets: MoonTarget.Insets, rowHeight: Double = 28.0)`. `ready` and `slow` stay as 3b built them. `welcome` → `opening.withWelcomeExit(1600 ms, opening.size, MoonTarget.fallback(opening.size, safeAreaInsets, rowHeight))`; no mode → `opening`. It must be callable from `:feature:launch` (the controller and Task 8's capture test): if 3b put it in `:app`, move it to `…/launch/LaunchOverrides.kt` as `fun LaunchMode?.timeline(opening: LaunchTimeline, readyAt: Duration, safeAreaInsets: MoonTarget.Insets, rowHeight: Double = 28.0): LaunchTimeline` and say so in the commit body. Production passes `WelcomeMetrics.fallbackRowHeight(density)` for `welcome` (iOS passes the measured row only in that mode, `SceneDelegate.swift:130`).
- **Route:** Welcome is published at once (iOS navigates before the overlay ends), so Welcome composes under the overlay and reports its anchor. After removal, the route stays Welcome.
- **Anchor:** in diagnostic welcome mode, `store.onChange` rebuilds `handOff` with `opening.withWelcomeExit(1600 ms, size, frame)` at any time (there is no coordinator). Keep `cf.freezeAt` frozen (iOS `updateDiagnosticMoon`).
- **Intro:** if `allowsIntro`, create `LaunchIntro(start = 2300 ms, clock = <the controller's 3a `clock`, a `MediaLaunchClock(t0)`>, frozenTime = cf.freezeAt)`. No `intro` trace.

## 6. Kotlin shape [R]

```kotlin
// …/model/ (pure)
class LaunchIntro(val start: Duration, val clock: LaunchClock, val frozenTime: Duration? = null)
class MoonAnchorStore { var frame: LaunchRect? private set; var onChange: ((LaunchRect) -> Unit)?; fun update(frame: LaunchRect) }
object MoonTarget { data class Insets(…) /* 3a */; fun fallback(size: LaunchSize, safeAreaInsets: Insets, rowHeight: Double): LaunchRect; fun contentBounds(size: LaunchSize, safeAreaInsets: Insets): LaunchRect }
data class WelcomeGroups(val heading: LaunchRect, val actions: LaunchRect)
class WelcomeGeometry(size: LaunchSize, insets: MoonTarget.Insets) { val content: LaunchRect; val top: Double; val bottom: Double; val leading: Double; val trailing: Double
    fun groups(headingHeight: Double, actionsHeight: Double): WelcomeGroups }          // equals/hashCode over the five fields
fun LaunchTimeline.withWelcomeExit(start: Duration, size: LaunchSize, moonTarget: LaunchRect): LaunchTimeline

// …/launch/welcome/
@Composable fun WelcomeScreen(intro: LaunchIntro?, moonAnchorStore: MoonAnchorStore?, onPlanMyTrip: () -> Unit, onSignIn: () -> Unit, modifier: Modifier = Modifier)
internal fun Modifier.introItem(index: Int, intro: LaunchIntro?, now: LongState): Modifier
internal fun Modifier.reportMoonAnchor(store: MoonAnchorStore?): Modifier
object WelcomeTags { const val WORDMARK = "welcome.wordmark"; const val EYEBROW = "welcome.eyebrow"; const val HEADLINE = "welcome.headline"
                     const val COPY = "welcome.copy"; const val PLAN = "welcome.plan"; const val SIGN_IN = "welcome.signIn" }
object WelcomeMetrics { fun fallbackRowHeight(density: Density): Double }
object WelcomeWindowPolicy { const val WIDE_BREAKPOINT_DP = 600; const val COPY_CARD_MAX_DP = 560 }
val LocalWelcomeProbe = staticCompositionLocalOf<WelcomeProbe?> { null }   // counts WelcomeScreen compositions in a SideEffect, like 3a's LaunchProbe

// …/model/LaunchRect (3a type; 3c adds)
val midX: Double; val midY: Double

// …/launch/LaunchController.kt (3a/3b class; 3c adds)
val moonAnchorStore: MoonAnchorStore                       // one per process
var launchIntro: LaunchIntro? ; private set                // plain field, set before `route` is published
fun onOverlayGeometry(size: LaunchSize, insets: MoonTarget.Insets, rowHeight: Double)
val fontsReady: Boolean                                    // snapshot state, §Task 5

// …/model/LaunchCoordinator.kt (3b class; 3c adds, §5.3 size change)
fun setFallbackTarget(rect: LaunchRect)

// …/launch/LaunchOverrides.kt (3b enum; 3c adds Welcome, §5.9)
enum class LaunchMode { Ready, Slow, Welcome }

// :app
interface WelcomeDestinations { fun planMyTrip(); fun signIn(); val seam: String? get() = null; fun closeSeam() {} }
object InertWelcomeDestinations : WelcomeDestinations      // release and perf build types: no-ops
// app/src/debug/kotlin/net/chronoflow/debug/DebugWelcomeDestinations.kt: seam = "guestWizard" / "signIn" (snapshot state); closeSeam() → null
```

## 7. Tasks

Each task is one Conventional Commit with no trailers. Tick the matching `plan.md` checkbox in the commit that completes it (the mapping is in brackets). In each round, run only what you touched (`make targeted ONLY="feature:launch:<Class>"`). Run `make quick` once before the last commit and `make check` at the end. If your sandbox cannot run the emulator tiers, the orchestrator runs them; report the exact command and its output.

### Task 1: Intro and anchor store (`feat(launch): port the launch intro and moon anchor store`)

- Files: `…/model/LaunchIntro.kt`, `…/model/MoonAnchorStore.kt`.
- Behaviour: §5.3 (store), §5.5 (intro). Durations are `kotlin.time.Duration`.
- Tests (JVM, `feature/launch/src/test/kotlin/net/chronoflow/feature/launch/model/`; use `runTest(timeout = 60.seconds)` wherever coroutines run):
  - `LaunchIntroTests`, a port that keeps the Swift names:
    - `offsetsProgressAndAbsoluteClockDelays`: the offsets are `[0,100,150,300,400,450]`. For every index, progress is 0 at `start − 1 ms` and at `start`, `CubicBezier.intro.value(0.5)` at `+225 ms`, and 1 at `+450 ms`. With `ManualLaunchClock` at 2400 ms (start 2300), `delay(0) = −0.1` and `delay(5) = 0.35` (± 1e-12). Every index is at 1 at 3200 ms.
    - `introIsClaimedOnceAndFreezingKeepsTheSharedEpoch`: `LaunchIntro(2300 ms, clock, frozenTime = 2450 ms)`; `claim()` returns true, then false; `hasPlayed` is true; `frozenTime == 2450 ms`. With the clock advanced to 10 s, `progress(2, frozenTime) == 0` (the epoch is `start`, not the clock). `LaunchIntro(8700 ms)` gives `progress(5, 9600 ms) == 1`.
    - Use the exact values in `KitT/AppCoreTests/LaunchIntroTests.swift`.
  - `MoonAnchorStoreTests`, a port of both Swift tests:
    - `publishesValidChangedFramesAndRejectsInvalidGeometry`: Swift feeds `.zero`, `.null`, `.infinite` and a NaN-x rect. `LaunchRect` has no null/infinite sentinels, so feed the zero rect, `LaunchRect(Double.POSITIVE_INFINITY, 0.0, 28.0, 28.0)` (Swift's `.null` has an infinite origin), `LaunchRect(0.0, 0.0, Double.POSITIVE_INFINITY, 28.0)`, `LaunchRect(Double.NaN, 0.0, 28.0, 28.0)` and `LaunchRect(0.0, 0.0, -28.0, 28.0)`: `frame` stays null. Then `first = (24,64,28,28)` twice and `first` offset by `(31, 16)`: the published list is exactly `[first, first + (31,16)]` and `frame` equals its last element.
    - `measuredAnchorRebuildsTheReducerBeforeFlightOnly`: uses 3b's reducer (`Session(SignedOut)` first, reduce motion false). Two anchors produce exactly 2 effects; `state.moonTarget` is the second rect; after `Reached(1600 ms)` a further `update(first)` adds no effect and the target stays the second rect.

### Task 2: Welcome exit, moon target and geometry (`feat(launch): port the welcome exit and moon target`)

- Files:
  - `…/model/LaunchTimelineWelcome.kt`;
  - `…/model/MoonTarget.kt` (add `fallback` and `contentBounds`);
  - `…/model/LaunchRect` (add `midX`/`midY`, §5.2);
  - `…/model/WelcomeGeometry.kt`.
  - Do **not** touch `LaunchHandOffTimeline.kt` or the interim file here; Task 5 swaps them (§5.2), so every commit keeps 3b's signed-out tests green.
- In `MoonTarget` and `WelcomeGeometry`, use `WelcomeTokens` values (`toDouble()`): 16 `groupGap`, 64 `topPadding`, 40 `bottomPadding`, 24 `gutter`, 48 = `2 × gutter`, and for the fallback moon 28 = `moonSize` and 14 = `moonSize / 2` (iOS writes 28 and 14 as literals; add a one-line comment saying so). No numeric literals remain there. `withWelcomeExit` keeps the iOS timing and distance literals (1.6, 0.7, 0.6, 0.4, 0.3, 0.95, 120, −6) and 104 (the overlay moon's size, as iOS) as named `private const val`s.
- Tests (JVM):
  - `WelcomeTimelineTests`: port all five, with the Swift names and values.
    - `everyWelcomeEndingKey`: start 50 ms → exit 1.6; for each row of §5.2, its last two key times, last value and last curve; container scale `[1]`; unique tracks.
    - `flightUsesOneStraightLineAndProgressForPositionAndScale`: every 10 ms from 1600 to 2300, the moon is within 0.01 dp of the easeInOut line `(195,394)→(38,78)`, and its scale within 1e-10. Container opacity is 1 at 2300, `1 − 0.6846431874` ± 1e-9 at 2425, and < 1e-12 at 2550.
    - `targetCanChangeBeforeFlightWithoutDamagingTheOpening`: the opening prefixes are intact, and samples 0–1600 ms equal the opening. Rebuilding with `(71,109,42,42)` equals a fresh build and is idempotent.
    - `lateResolutionShiftsTheEntireEndingAndRetainsStatus`: with `withStatus(2 s)` and start 4 s, the moon is at `(195,394)` at 4 s and `(38,78)` ± 0.01 at 4.7 s; the container is 0 at 4.95 s; the status is 1 at 4.9 s.
    - `reduceMotionUsesOnlyTheImmediateFade`: equals `withReadyExit(50 ms)`; at 350 ms the moon is at `(195,422)` with scale 1.
  - Also in `WelcomeTimelineTests`: `welcomeSettlesAtExitPlus950`. `LaunchFrameSampler(opening.withWelcomeExit(1600 ms, …)).settledAfterSeconds == 2.55` ± 1e-9; with start 4 s it is 4.95. Also `LaunchHandOffTimeline.exitEndSeconds(opening.withWelcomeExit(1600 ms, …)) == 2.55` (3b's static reader; it needs no `build` change).
  - `MoonTargetTests`: port the four Swift tests with their names and values.
    - `boardTargetAndPaddingRule`: 390 × 844, insets top 47 / bottom 34, row 28 → `(24,64,28,28)`, content `(24,64,342,740)`.
    - `largerRowMovesTheCentreAndSmallRowsKeepTheMoonSize`: rows 20/28/40/56/96 → midY 78/78/84/92/112.
    - `allFourSafeInsetsDefineTheContent`.
    - `shortAndWideWindowsAndInsetsRemainInputs`.
  - `WelcomeGeometryTests`: port both Swift tests.
    - `everyDeviceUsesAllFourInsetsAndScrollsWithoutCompressingContent`: run it over every `profiles.json` profile, with the profile's insets and with `(7,19,11,31)`.
    - `zeroInsetBoardRetainsItsComposition`: actions bottom 804, Sign in top 752, Plan my trip top 688.
  - `WelcomeCompositionTest` (JVM, `@RunWith(Parameterized::class)`, one case per profile plus a 280 × 653 case): in each case, run heights from 568 to 1400 dp in steps of 1 at the profile width. Check:
    - `WelcomeGeometry.content == MoonTarget.contentBounds`;
    - top, bottom, leading and trailing per §5.4;
    - the fallback lies inside `content` and inside the window;
    - the straight flight segment from the opening's moon centre at 1600 ms to the fallback centre lies inside the window;
    - `opening(size).withWelcomeExit(1600 ms, size, fallback)` samples finite values at 1600, 1950, 2300 and 2550 ms, and container opacity is 0 at 2550.

    Each case must take < 1 s.

### Task 3: Welcome screen (`feat(launch): build the welcome screen`) [ticks "WelcomeScreen"]

- Files: `…/launch/welcome/{WelcomeScreen.kt, WelcomeColumn.kt, WelcomeBrand.kt, IntroItem.kt (static path only: no layer when the intro is null), MoonAnchorModifier.kt, WelcomeTags.kt, WelcomeMetrics.kt, WelcomeWindowPolicy.kt}`.
- Behaviour: §5.4, §5.6, §5.7. `WelcomeScreen(intro = null, …)` is the final screen. `reportMoonAnchor` converts with the `LocalDensity` captured in composition, inside the `onGloballyPositioned` callback. No `mutableStateOf` and no `onSizeChanged`.
- Fallback: if `WelcomeLayoutTest` shows that `minHeight` does not survive `verticalScroll`, wrap the column in `BoxWithConstraints`, pass `maxHeight` as the viewport, and record that in the commit body.
- Edge cases:
  - The Dutch headline at `small` × `largeFont` wraps to many lines, and the screen scrolls.
  - At 280 dp a long Dutch word may break mid-word (Compose default). Do not shrink the type.
  - The `landscapeSideNav` right inset keeps the column inside the safe area.
  - On `foldInner` the card is 560 dp, centred, while the buttons stay full content width.
- Tests: `feature/launch/src/test/kotlin/net/chronoflow/feature/launch/welcome/`, Robolectric, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`, `RuleChain.outerRule(ProfileRule(…)).around(composeRule)`, content inside `ProfileHost` + `ChronoFlowTheme`.
  - `WelcomeLayoutTest` (`@Layout`):
    - Use an ad-hoc `DeviceProfile(name = "board390", widthDp = 390, heightDp = 844, density = 3.0f, fontScale = 1.0f, navigation = "gesture", insets = SafeInsetsDp(0f, 0f, 0f, 0f), source = "Main board")` (0e T5 fields), en, light. Node bounds in dp ± 0.5: `MoonAnchorStore.frame == (24,64,28,28)`; wordmark row 64–92; eyebrow 164–180; headline top 196 and height `3 × 48`; Plan my trip 688–740; Sign in 752–804; x 24, width 342. If the headline is not 3 lines, stop and report the line breaks; do not tweak.
    - At `foldInner`: the copy card is 560 wide and centred in the content ± 0.5; Plan my trip width == content width.
    - At `landscapeSideNav`: Plan my trip top == heading bottom + 24 + card height + 12 + 12, and `performScrollTo()` reaches Sign in.
  - `WelcomeAccessibilityTest` (`@Layout`, `pixel8`, en and nl):
    - the six tags exist exactly once;
    - only `welcome.headline` has `Heading`;
    - `welcome.plan` and `welcome.signIn` have `Role.Button`, `OnClick`, and the texts "Plan my trip"/"Plan mijn reis" and "Sign in"/"Inloggen", read from resources;
    - the eyebrow text is `JET-LAG PLANNER` / `JETLAGPLANNER`;
    - no node has a content description (the moon is decorative);
    - tops strictly increase: wordmark < eyebrow < headline < copy < plan < signIn;
    - each click calls its callback exactly once.
  - `WelcomeGuardsTest` (`@Layout`, `ParameterizedRobolectricTestRunner`, 5 profiles × en/nl × light/dark = 20 cases): `Guards.assertAll(composeRule, profile, headingTags = setOf(WelcomeTags.HEADLINE))`. Each case must take < 5 s.

### Task 4: Intro on the launch clock (`feat(launch): play the welcome intro on the launch clock`)

- Files: `IntroItem.kt` (the live and frozen paths) and `WelcomeScreen.kt` (claim, frame loop, `LocalWelcomeProbe`).
- Behaviour: §5.5. Use no `animate*`, `Animatable`, `AnimatedVisibility`, `delay` or transitions. Read `now` only inside `graphicsLayer {}` blocks. The loop starts at `start` and stops at `start + 900 ms`. A frozen intro runs no loop.
- Tests (Robolectric `@Layout`, `pixel8`; build `LaunchIntro` on a `ManualLaunchClock` and advance it together with `composeRule.mainClock` (`autoAdvance = false`)):
  - `WelcomeIntroRenderTest`:
    - `frozenIntroPositionsFollowTheIntroCurve`: frozen at 2299, 2400, 2450, 2700, 3200 and 3300. Each tagged node's `boundsInRoot.top` equals its final top + `14 dp × (1 − progress(i, t))` ± 0.5 px.
    - `frozenIntroPixelsStayFixedAcrossClockAndRenderPasses` (iOS name): two captures at the same frozen time, with `mainClock` advanced 1 s between them, are pixel-identical, and no frame is pending.
    - `laterAppearanceAndReduceMotionShowTheFinalScreen` (iOS name): an already-claimed intro and a `LocalReduceMotion provides true` render are both pixel-identical to `intro = null` (a JVM render compared with a JVM render only). The Reduce Motion case leaves `hasPlayed == false`.
  - `WelcomeRecompositionTest`: a live intro with start 2300. Step 8 ms at a time from 2200 to 3400. The probe counts exactly 1 composition. `hasPlayed` is true after the first frame. After 3200 ms, `mainClock` has no pending frame (the loop stopped), and every item's `boundsInRoot` equals its final position.

### Task 5: Hand-over wiring (`feat(launch): hand the launch over to welcome`) [ticks "Intro", "Moon hand-over", "Teardown"]

- Files:
  - `LaunchController.kt`: `moonAnchorStore`, `onOverlayGeometry`, the fallback in `startReadiness` (claimed and headless, §5.3), intro creation, the intro trace job, `fontsReady`;
  - `…/model/LaunchCoordinator.kt`: `setFallbackTarget` (§5.3 size change);
  - `…/model/LaunchHandOffTimeline.kt`: the Welcome branch (§5.2); delete `…/model/LaunchTimelineWelcomeInterim.kt`;
  - `LaunchOverlay.kt`: the geometry report in the existing `onPlaced`;
  - `LaunchTrace.kt`: `beginIntro`/`endIntro`, cookie 6;
  - `…/app/CfRoot.kt`: the route switch, the font gate, system bars;
  - `…/app/RoutePlaceholder.kt`: drop the Welcome sky background;
  - `AppGraph.kt`: set `fontsReady` after `warmFonts()` returns.
- Behaviour:
  1. Implement §5.3 (store wiring, fallback, size change) and §5.5 (intro creation, `intro` trace). Swap the Welcome ending (§5.2 "Replace the interim ending") in this commit, together with the fallback, so no commit has a signed-out path with a zero fallback.
  2. **Route switch in `CfRoot`:** `LaunchRoute.Welcome` → `WelcomeScreen(intro = controller.launchIntro, moonAnchorStore = controller.moonAnchorStore, onPlanMyTrip = {}, onSignIn = {})`. The callbacks stay no-ops in this commit; Task 7 replaces them with `WelcomeDestinations` and adds the seam branch. Every other route keeps `RoutePlaceholder`. `CfRoot` never passes `LaunchRoute.Welcome` to `RoutePlaceholder` any more; in `RoutePlaceholder`, remove the Welcome → `CfSky` background branch (every placeholder uses `background`) and keep the raw-name mapping as it is.
  3. **Font gate** (OQ-3c-4): while `controller.overlayActive`, compose `WelcomeScreen` only once `controller.fontsReady` is true, **or** once the launch clock passes 1500 ms, whichever comes first. Set `fontsReady` in a `finally` after 3a's `warmFonts()` call returns. For the 1500 ms bound, use a `clock.sleepUntil(1500 ms)` job in the controller scope that sets the same flag. With a 1500 ms bound, the anchor still arrives before the 1550 ms deadline. Until then, `app.root` stays empty. With no overlay (Warm, a Recreated activity after removal, the `screen` profile: no `t0`, no launch clock) the gate is open at once; the process has already warmed the fonts, or (`screen`) the launch-path StrictMode policy is not active.
  4. **Teardown:** 3b's `onExitFrame` ends `exit`, sets `handOffComplete` (so `overlayActive` turns false), and that lifts `hideFromAccessibility`. Check that 3b's `ReportDrawnWhen { !controller.overlayActive && controller.route != null }` (3b §5.8) fires exactly once, at the removal frame, on the Welcome route; it should need no change. Do not move accessibility focus in code unless the OQ-3c-3 check fails.
  5. **System bars** per §5.7.
  6. **Update 3b's tests:**
     - `LaunchHandOffTimelineTests`: rename `interimWelcomeEndsAtW950` → `welcomeEndsAtW950` (same values: W 1600 → 2.55, W 8000 → 8.95, rm W 8000 → 8.3; give every `LaunchState` a non-zero `fallbackTarget`, `(24,64,28,28)`, because Reduce Motion now passes through `withWelcomeExit`'s checks); replace `interimWelcomeKeepsTheOpeningUntilW700` (0–2300 ms) with `welcomeKeepsTheOpeningUntilW` (samples every 10 ms from 0 to 1600 ms equal the opening's samples; the moon moves from 1600).
     - `CfRootHandOffTest`: the signed-out case and the `screen`-profile case now assert `welcome.wordmark` instead of `route.welcome`.
     - `RoutePlaceholderGuardTest`: drop the `route.welcome` cases.
- Edge cases:
  - Signed in: the store has no effect and no intro is created.
  - Activity recreated during the intro (3b resumes the overlay if it still runs): a final Welcome, no second claim.
  - `screen` profile: no overlay, so no intro; a final Welcome.
  - An anchor update after `exitStart` is ignored by the reducer.
  - A size change before removal rebuilds the hand-off per §5.3.
- Tests:
  - `WelcomeHandOverTests` (next to 3b's `LaunchController` tests in `feature/launch/src/test/…`; 3b's `ManualLaunchClock` and `StandardTestDispatcher`; plain JVM if the controller can be driven without `Bundle`, otherwise Robolectric `@Logic` as 3b's `LaunchControllerLifecycleTest`):
    - `fallbackComesFromTheOverlayGeometry`: 390 × 844, insets top 47 / bottom 34, row 28 → `state.fallbackTarget == (24,64,28,28)`; with row 36.4 → y 68.2.
    - `introIsCreatedOnceAtExitStartPlus700BeforeTheRoute`: signed out at 10 ms → `launchIntro.start == 2300 ms`, set before `route` is published; a second `onLaunchChange` keeps the same instance.
    - `noIntroWithReduceMotionOrWithoutTheOverlay`: both cases.
    - `introTraceSpansStartToStartPlus900`: a recording `LaunchTrace` fake sees begin at 2300 and end at 3200 on the manual clock; cancelling the scope at 2500 still ends it once.
    - `sizeChangeBeforeFlightUsesTheNewFallback`: geometry changes at 1200 ms → the rebuilt `handOff` lands on the new size's fallback, and the coordinator's `state.fallbackTarget` is the new fallback.
    - `headlessCoordinatorUsesANonZeroFallback`: a `screen`-profile (no overlay) signed-out controller publishes `route == Welcome` without crashing, with `fallbackTarget.width == 28`.
  - `WelcomeIntroTimingTest` (JVM; reducer + `LaunchHandOffTimeline` + `LaunchIntro` on `ManualLaunchClock`; frames quantized at 60 Hz and at 120 Hz; frame `i` at `i / hz` s):
    - signed out at 10 ms → W = 1600. The first frame at which `exitEndSeconds` is reached is in `[2550, 2550 + 1 frame]`, and the first frame with `progress(5) == 1` is in `[3200, 3200 + 1 frame]`.
    - Per item, the first frame at progress 1 is ≥ 2750/2850/2900/3050/3150/3200.
    - A session at 4000 → 4950 and 5600.
    - Reduce Motion with the session at 120 ms → exit at 120, interactive at 420, no intro.
    - An anchor at 1400 versus no anchor (fallback committed at 1550): both commit, and both land at 2300.
  - `WelcomeHandOffTest` (Robolectric `@Layout`, `app/src/testDebug/…`; the real `CfRoot` with `StubReadiness` signed out at 10 ms, `cf.reduceMotion=false`, `nanoTime` bound to `mainClock`; one profile per test; each < 5 s):
    - `realWelcomePublishesAnAnchorToTheCoordinator` (iOS name): `state.moonTarget == store.frame`, and the overlay root `positionInWindow == Offset.Zero`.
    - `measuredWelcomeLandingAtDefaultAndLargeFont` (`pixel8` and `largeFont`): at 2300 ms the sampled moon centre == the anchor centre ± 0.5 dp, and scale == `anchor.width / 104` ± 1e-6.
    - `teardownMakesWelcomeAccessibleAndReportsFullyDrawnOnce`: before 2550 the content has `HideFromAccessibility`; after removal it does not, and a `reportFullyDrawn` counter reads 1.
    - `fontGateHoldsWelcomeUntilFontsAreReady`: with `fontsReady` false, `welcome.wordmark` is absent at 1000 ms and present at 1500 ms.

### Task 6: Diagnostic welcome mode (`feat(launch): add the diagnostic welcome mode`)

- Files: `…/launch/LaunchOverrides.kt` (`LaunchMode.Welcome`), `…/perf/LaunchDiagnosticsParser.kt` (`welcome` → `LaunchMode.Welcome`), the diagnostic timeline function with its `welcome` branch (§5.9: wherever 3b put it, moved into `:feature:launch` if needed), the diagnostic branch in `LaunchController` (§5.9), and plan.md "Android spec: seeds and test hooks" (add `cf.mode=ready|slow|welcome`).
- Tests (`app/src/testDebug/…`, JVM):
  - In `LaunchDiagnosticsTests`, replace `welcomeModeIsDeferred` with `welcomeModeParses`.
  - Port `welcomeFallbackUsesTheCurrentTextSizeAndAllSafeInsets` (iOS `I/AppTests/LaunchArgumentsTests.swift:10–29`): `fallbackRowHeight(Density(3f, 1f)) == 28`, and `fallbackRowHeight(Density(3f, 1.3f)) > 28` (do not assert 36.4, §5.3). Then 570 × 730, insets `(top 31, left 37, bottom 27, right 59)`, the larger row, mode `welcome`: the moon position sampled at 2300 ms is `x = 37 + 24 + 14`, `y = max(31 + 16, 64) + larger / 2` (± 1e-9).
  - In `LaunchModeTimelineTests`, add the `welcome` case of `testModeTimelinesAndAsymmetricSafeAreaFallback` (570 × 730, insets `(23, 31, 29, 47)` as top/left/bottom/right): it equals `opening.withWelcomeExit(1600 ms, size, MoonTarget.fallback(size, safe, 28))`.
  - `DiagnosticWelcomeAnchorTest` (JVM on the controller): an anchor in welcome mode rebuilds `handOff` with that target, both at 1000 ms and at 2000 ms.

### Task 7: Destinations, seams and the journey (`feat(app): route welcome actions to debug seams`) [ticks "Tests"]

- Files:
  - `…/app/WelcomeDestinations.kt` (the interface and `InertWelcomeDestinations`);
  - `app/src/debug/kotlin/net/chronoflow/debug/DebugWelcomeDestinations.kt`;
  - the per-variant wiring, using the same mechanism 3a/3b use for `LaunchOverrides` (the debug implementation in `src/debug`, the no-op for release and the perf build types). Add no second mechanism;
  - `…/app/CfRoot.kt` (callbacks, the seam branch, its `BackHandler`) and `…/app/RoutePlaceholder.kt` (the `rawName` overload).
- **Wire the callbacks:** replace Task 5's no-op `onPlanMyTrip`/`onSignIn` in `CfRoot` with `destinations::planMyTrip` / `destinations::signIn`.
- **The seam** (iOS OQ 8, `I/App/Debug/RoutePlaceholder.swift`): `CfRoot` shows `RoutePlaceholder(rawName = seam, label)` in place of Welcome while `destinations.seam != null`. Extend 3b's `RoutePlaceholder` with a `rawName: String` overload (the `LaunchRoute` version delegates to it). This gives tag `route.signIn` / `route.guestWizard`, the `background` token, and, through the `label` 3b wires from the debug variant (`DebugRouteLabel`), the text "Debug route: signIn" / "Debugroute: signIn" from `app_debug_route_x`. Perf and release pass no label and never have a seam.
  - Back: a `BackHandler` that calls `destinations.closeSeam()`, which returns to Welcome. Welcome re-enters composition and shows the final screen (`hasPlayed`).
  - Never in release.
- `ReleaseReadinessTest` gains: `Class.forName("net.chronoflow.debug.DebugWelcomeDestinations")` throws.
- Tests:
  - `WelcomeSeamTest` (Robolectric `@Layout`, `app/src/testDebug`): clicking Sign in shows `route.signIn`; back shows `welcome.wordmark` at its final bounds (no intro layer); clicking Plan my trip shows `route.guestWizard`.
  - `WelcomeDestinationsReleaseTest` (`app/src/testRelease`, JVM): the release wiring is `InertWelcomeDestinations`, and `seam` stays null after both calls.
  - `WelcomeJourneyTest` (`@Smoke`, emulator, `app/src/androidTest`, extends `ScreenTest`), the feature's only emulator journey:
    - first `InstrumentationRegistry.getInstrumentation().runOnMainSync { graph.resetLaunchForTest() }` (3a OQ-3a-13: instrumented tests share one process, so without it the launch would be overlay-free);
    - `launch(mapOf("CF_UI_TEST_PROFILE" to "launch", "cf.session" to "out"))` (the functional scales are 0 and no `cf.reduceMotion` is passed, so this runs as Reduce Motion: a 300 ms fade, no intro);
    - `compose.waitUntilDoesNotExist(hasTestTag("launch.opening"), 5_000)` first: `hideFromAccessibility()` does not remove Welcome's nodes from the test tree, so Welcome can already be found under the overlay;
    - then `compose.waitUntilExactlyOneExists(hasTestTag("welcome.signIn") and hasClickAction(), 3_000)`;
    - click, then `waitUntilExactlyOneExists(hasTestTag("route.signIn"), 3_000)`.

    The debug StrictMode `penaltyDeath` launch policy is active, so a disk read in Welcome kills the test. Must take < 10 s.

### Task 8: Board captures (`test(launch): capture the welcome boards`)

- `WelcomeCaptureTest` (`@Capture`, Robolectric, `feature/launch/src/test/…/welcome/`). `make captures ONLY=Main,SplashWelcome` writes:
  - `build/captures/Main_final_<profile>_<lang>_<theme>.png` (pixel8, foldInner, small; en and nl; light and dark);
  - `Main_intro-<ms>_pixel8_en_<theme>.png` for 2400, 2450, 2700, 3200 and 3300 (frozen intro);
  - `SplashWelcome_welcome-<ms>_<profile>_en_<theme>.png` for 1500, 1950, 2300, 2400 and 3300 at pixel8 and foldInner.
- The SplashWelcome frames compose `WelcomeScreen` (intro `LaunchIntro(2300 ms, clock, frozenTime = t)`) under `LaunchOverlay` frozen at the same `t`. They use the diagnostic timeline (Task 6's function, mode `welcome`, `(opening, insets, rowHeight)`), rebuilt with `store.frame` after the first layout (§5.9). Drive it through a test `LaunchController` in diagnostic welcome mode; if 3a's controller cannot be built in a feature test, record how you drove it.
- The reviewer compares:
  - `Main_final_pixel8_en_light` with `Main` (`theme=light`, `intro=false`);
  - `Main_intro-*` with `Main` `intro=true` at the same loop times;
  - `SplashWelcome_welcome-1950/2300` with `SplashWelcome`.

  The only expected differences are those in §5.1.

### Task 9: Measure the moments (`perf(launch): measure welcome interactive and the last intro item`)

- `LaunchJankBenchmark` (signed-out cold start, perf variant): after `device.wait(Until.gone(By.res("launch.opening")), 3_500)`, wait for the intro to end with `device.wait(Until.gone(By.res("welcome.signIn")), 900)`. That is a bounded wait that times out by design; comment why. `FrameTimingMetric` then covers 0 to about 3.45 s.
- `WelcomeMomentMetric` (a `TraceMetric` subclass, `@OptIn(ExperimentalMetricApi::class)`): query the app process's async slices `first-commit`, `exit` and `intro`. Report `welcomeInteractiveMs = exit.end − first-commit.end` and `introLastItemMs = intro.end − first-commit.end`. Add it to the cold signed-out `StartupBenchmark`.
- `benchmark/budgets.json` (a functional gate on the emulator; tolerance one frame at the measured refresh rate, which you record): `welcomeInteractiveMs` min 2549, max 2550 + frame + 1; `introLastItemMs` min 3199, max 3200 + frame + 1. A missing value fails (`check-benchmarks.py`). The `LaunchJankBenchmark` frame-overrun budget rows from 3a/3b must still pass over the longer capture.
- Run `make perf`. Append rows to `docs/perf-startup.md`: date, commit, `pixel` AVD API 36, refresh rate, build type, cold TTID, TTFD, both moments, `frameOverrunMs` P50/P90/P99. Flag a cold-TTID change of more than ±25 % against the last row. Regenerate the profiles if `make baseline-profile` exists; otherwise write "pending".

### Task 10: Docs (`docs(launch): record phase 3c decisions`)

- Add OQ-3c-1 … OQ-3c-12 (§10) to plan.md "Open questions for the user", using the next free OQ-A numbers and keeping the defaults. Note in OQ-3b-11 that 3c amends the navigation-bar icons on Welcome.
- Add the `route.signIn`/`route.guestWizard` seams and the font gate to "Android spec: seeds and test hooks" (`cf.mode=welcome` was added in Task 6).
- Record the five Welcome contrast pairs in `docs/design-system.md` as accepted board exceptions (iOS `plan.md:845–849`; research `05 §4.5`): eyebrow `dawn` on sky, headline `skyTitle` on sky, copy `skyCopy` on `skyCopyBackground`, Sign in `skyCopy` on `skyCopyBackground`, Plan my trip (`dawnForeground` on `dawn`, and the `dawn` pill against the sky; iOS measured 6.76:1 and 1.40:1 on SE nl). Phase 2's `ContrastTest` has no rows for these pairs (it only covers `skyTitle` on `skyTop`/`skyMid`, which passes, and `dawnForeground` on `dawn`, 6.76/9.54); cite those two values and write "not measured on Android" for the rest. Add no test and change no token.
- Record the OQ-3c-3 TalkBack check (§9).

## 8. Acceptance criteria

- `make quick` passes (< 60 s warm), including every JVM test from Tasks 1, 2, 5 and 6.
- `make targeted ONLY="feature:launch:LaunchIntroTests feature:launch:MoonAnchorStoreTests feature:launch:WelcomeTimelineTests feature:launch:MoonTargetTests feature:launch:WelcomeGeometryTests feature:launch:WelcomeCompositionTest feature:launch:WelcomeIntroTimingTest feature:launch:WelcomeHandOverTests feature:launch:LaunchHandOffTimelineTests"` passes (use the module syntax the Makefile expects).
- `make check` passes with `pixel` booted. That includes `WelcomeLayoutTest`, `WelcomeAccessibilityTest`, all 20 `WelcomeGuardsTest` cases, `WelcomeIntroRenderTest`, `WelcomeRecompositionTest`, `WelcomeHandOffTest`, `WelcomeSeamTest` (each < 5 s in `timings.json`), `testReleaseUnitTest` with `ReleaseReadinessTest`/`WelcomeDestinationsReleaseTest`, and `WelcomeJourneyTest` in `@Smoke` (< 10 s).
- `make perf`: `welcomeInteractiveMs` = 2550 ms and `introLastItemMs` = 3200 ms, each within + 1 frame (functional gate); `check-benchmarks.py` passes; the rows are in `docs/perf-startup.md`. Device gates stay "open: device only" (OQ-A22).
- `make captures ONLY=Main,SplashWelcome` produced the Task 8 PNGs. The composition matches `Main` and `SplashWelcome` at `pixel8` and `foldInner`, apart from §5.1. Any other difference is either fixed or recorded with its board name.
- Debug runs are StrictMode-clean through the intro: the journey survives `penaltyDeath`.
- `git grep -n "withWelcomeHandOff\|LaunchTimelineWelcomeInterim" -- '*.kt' '*.kts'` returns nothing (the docs still name them).
- Every plan.md 3c checkbox is ticked in its commit, and `git log` shows atomic Conventional Commits without trailers.

## 9. Review checklist (Opus reviewer)

Hand-over
- [ ] The anchor is the moon glyph's unclipped window rect in dp (`positionInWindow` + `size`). It is not `boundsInWindow`, not the brand row and not the wordmark.
- [ ] `MoonAnchorStore` and the controller's geometry fields are plain fields. There is no `mutableStateOf`, no `onSizeChanged` + state, and no recomposition caused by the anchor. `onOverlayGeometry` writes no snapshot state from `onPlaced`.
- [ ] `fallbackTarget` is non-zero, comes from `MoonTarget.fallback` with the overlay's insets and `fallbackRowHeight` at the capped font scale, and has no 560 cap.
- [ ] `fallbackTarget` is non-zero on every path, including the headless coordinator (Warm, `screen`).
- [ ] `withWelcomeExit` runs its checks before the Reduce Motion branch and appends onto `withoutExit()`. `exit = max(1.6, start)`; the moon is easeInOut with one progress; the container fades from W+700 to W+950 with no scale. Reduce Motion returns `withReadyExit`. The interim file is deleted.
- [ ] The overlay is removed on the frame clock at W+950 (3b `onExitFrame`, never a timer); `exit` ends there, and `reportFullyDrawn()` fires once there.
- [ ] On the emulator, fold or resize during the opening: the moon lands on the new glyph position and never on a stale one.

Intro
- [ ] `LaunchIntro` start = `exitStart + 700 ms`, created once per process before `route` is published, only when the overlay was claimed and the captured Reduce Motion is false. Welcome claims it once, in a `SideEffect`. A recreated activity, a back from the seam and the `screen` profile all show the final screen.
- [ ] `now` is read only in `graphicsLayer` blocks; there is 1 composition across the intro; the loop starts at `start` and stops at start + 900; a frozen intro runs no loop.
- [ ] Welcome code has no `animate*`, `Animatable`, `AnimatedVisibility`, `updateTransition` or `delay`. The `intro` trace runs from the controller scope, not from Welcome, and has a `finally`.
- [ ] The moon glyph is not inside an intro layer; index 0 is the wordmark only.

Layout and fidelity
- [ ] No dots; one sky brush behind everything, not scrolling, covering the full window including the system-bar areas.
- [ ] Padding is `max(safeTop+16, 64)`, `24 + inset` at the sides and `max(safeBottom, 40)`. The groups are placed by one custom `Layout` from constraints (no `BoxWithConstraints` unless the recorded fallback was needed; no `Spacer(Modifier.weight(1f))` in the scroll).
- [ ] Copy card: radius 22, padding 16/20, `skyCopyBackground`, at most 560 dp, centred. Buttons: 52 high, pill, full content width, Dawn/`primaryButton` and OnSky/`secondaryButton`, `contentPadding` 20/8. Eyebrow: uppercase with the app locale, `dawn`, never clipped. Headline: 48/48 with unclipped glyphs.
- [ ] Every value comes from `WelcomeTokens`, `CfTheme.type`, `CfTheme.colors` or `WelcomeWindowPolicy`. Production code has no hex, no `MaterialTheme.colorScheme`, no `isSystemInDarkTheme()` and no 390/844 constants.

Accessibility and copy
- [ ] Reading order: wordmark → eyebrow → headline → copy → plan → signIn. The heading is on the headline only; the moon is decorative; the buttons have `Role.Button`. The iOS tags are used exactly.
- [ ] Strings come only from the six generated resources (plus 3b's "Debug route: %@" in debug), in en and nl. No new copy.
- [ ] The status-bar icons are light; the navigation-bar icons are dark in light and light in dark, through 3b's mechanism. No haptics.

Performance and process
- [ ] While the overlay is active, Welcome composes only after the font gate (fonts warm or 1500 ms); without an overlay it composes at once. There is no disk read on the main thread (the journey runs under `penaltyDeath`).
- [ ] Tests are at the cheapest tier; the only emulator test is the journey; no `Thread.sleep`; tag queries are exact; every Robolectric case is < 5 s.
- [ ] The seams are debug only (`ReleaseReadinessTest`); release buttons do nothing; `cf.mode=welcome` is absent from release (`LaunchDiagnostics.None`).
- [ ] Ported tests keep the Swift names, and the Swift type names are kept (`LaunchIntro`, `MoonAnchorStore`, `MoonTarget`, `WelcomeGeometry`, `withWelcomeExit`).

Typical mistakes:
- driving the intro with `animateFloatAsState`/`AnimatedVisibility` (it dies at animator scale 0 and is not on the launch clock);
- deciding the claim inside `remember {}` with a side effect, or deciding again on recomposition;
- putting the intro layer on the brand row, so the moon fades;
- measuring the anchor with `boundsInWindow`;
- `onGloballyPositioned` writing a `mutableStateOf`;
- a zero fallback crashing `withWelcomeExit` under Reduce Motion;
- ending `intro` from Welcome's `LaunchedEffect`, so it never ends after navigating away;
- reading `WindowInsets.safeDrawing` directly (Konsist);
- capping the whole column or the moon fallback at 560 dp;
- `uppercase()` without the app locale;
- leaving the old `RoutePlaceholder` Welcome case alive under the new screen.

TalkBack check (manual, once, recorded in Task 10): on the emulator with TalkBack on, cold launch signed out with `cf.reduceMotion=false`. After the dissolve, the first focused item must be "ChronoFlow" (the wordmark).

## 10. Open questions (proceed on the default; Task 10 adds them to plan.md)

| # | Question | Default |
|---|---|---|
| OQ-3c-1 | The plan says "Welcome copy card max 560 dp, centred" on windows ≥ 600 dp. Only the card, or the whole column? | **Only the card**, as the plan writes it. The heading, the buttons, `contentBounds` and the moon fallback keep the iOS formula unchanged (iOS has no cap). No wide board exists. If the user wants a capped column, `WelcomeGeometry` gains the cap in one place. |
| OQ-3c-2 | What do Plan my trip and Sign in do before 11a and 5a? | Debug: the iOS route seam (`route.guestWizard`, `route.signIn`, "Debug route: %@"); back returns to Welcome. Release and perf: they do nothing (iOS OQ 8). |
| OQ-3c-3 | How does TalkBack focus reach Welcome's first node after teardown (iOS `.screenChanged`)? Compose has no public way to request accessibility focus. | Rely on the overlay pane disappearing and `hideFromAccessibility` lifting, and check once by hand. If focus does not land on the wordmark, add a `FocusRequester` on the wordmark used only while touch exploration is on, and record it. |
| OQ-3c-4 | If Welcome composes before the fonts are warm, it loads them on the main thread under the overlay (StrictMode `penaltyDeath`). | Compose Welcome after the Phase 2 warm-up completes, or at 1500 ms, whichever comes first. The measured anchor still arrives before W − 50 in normal runs; otherwise the fallback is used. |
| OQ-3c-5 | The plan reads both moments "through the `intro` trace", but `intro` spans 2300–3200, and interactive (2550) is the end of `exit`. | `welcomeInteractiveMs = exit.end − first-commit.end`; `introLastItemMs = intro.end − first-commit.end`, through one `TraceMetric`. Emulator tolerance: + 1 frame at the measured refresh rate. |
| OQ-3c-6 | The buttons are clickable from 2550, while they still fade in until 3150/3200. | Yes (iOS parity: interactive = the overlay is gone). |
| OQ-3c-7 | The intro's time source: the frame time or `clock.now`? | `clock.now`, sampled in each `withFrameNanos` callback (the same `LaunchClock` as the overlay; they differ by less than 1 frame). |
| OQ-3c-8 | Five Welcome contrast pairs fail audits at the board values (iOS `plan.md:845–849`). | Keep the board colors and record them as accepted exceptions (research `05` OQ A10, l.1041; not plan.md's OQ-A10, which is about Fraunces 500). |
| OQ-3c-9 | Navigation-bar icon color on Welcome (OQ-3b-11 said light icons for Welcome). | Status bar: light icons. Navigation bar: from the sky bottom, so dark icons in the light theme (`skyHorizon` L 0.75) and light icons in the dark theme. This amends OQ-3b-11. |
| OQ-3c-10 | The plan's task text says the overlay "reads `MoonAnchorStore` at draw time". iOS instead sends the anchor to the reducer, which rebuilds the timeline before W. | Follow iOS: store → `LaunchEvent.MoonAnchor` → `CommitExit` → a rebuilt `handOff` sampler, which the overlay reads at draw time. This keeps the W − 50 fallback deadline and the "no changes after W" rule. |
| OQ-3c-11 | The overlay uses the captured system appearance, and Welcome uses `cf.settings.theme`. If they differ, the dissolve crosses themes. | Accept it (it only happens when the app theme overrides the system); add no extra transition. |
| OQ-3c-12 | Is the diagnostic `cf.mode=welcome` needed on Android? | Yes, in debug and perf: the board captures and the frozen-frame checks use it, as iOS does. Release keeps `LaunchDiagnostics.None`. |
