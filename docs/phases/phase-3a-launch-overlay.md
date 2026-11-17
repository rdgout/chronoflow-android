# Phase 3a: System splash and launch overlay

- **Branch:** `feat/phase-3a-launch-overlay`, from the stack top once Phase 2 is in it. 0b (launch colors, `launch_moon` PNGs, fonts), 0c (strings), 0e (profiles, seeds, Robolectric, `:benchmark`) must also be in the stack. If one is missing, stop and report it.
- **Plan entry:** `plan.md` "Phase 3a: System splash and launch overlay" (l.1391–1466). Decisions **A3**, **A9**, **A10**, **A11**, **A19**, **A21**; "Android spec: process, activity and launch sequence" (l.321–346); "Performance budget" rows tagged 3a; Traps in `AGENTS.md`.
- **Tests:** every test follows `AGENTS.md` "Tests": cheapest tier, budgets, no `Thread.sleep`/`SystemClock.sleep`, hang caps. Read that section before writing a test.
- **Tags:** **[F]** fact verified in source (path given). **[R]** rule this brief sets for Android. `I/` = `/Users/rickgout/Sites/chronoflow-ios` read at `f391a1fa` (`git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`; never write there). `Kit/` = `I/Packages/ChronoFlowKit/Sources`. `DC/` = `_design/canvas/`.
- `…/launch/` below stands for `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/`; `…/app/` for `app/src/main/kotlin/net/chronoflow/app/`.

---

## 1. Goal

The first thing a person sees is the system splash (sky-top color + 288 dp moon). The app's first Compose frame is a launch overlay whose **frame 0 is pixel-identical** to that splash, so the hand-over is invisible. From `t0` (the overlay's first draw) the overlay plays the iOS **opening** (0–1500 ms: sky and band fade in, sun and halo rise, flight path draws with a light on it, moon lifts and shrinks, wordmark fades in) on a **wall clock**, drawn in **one Canvas**, with **no per-frame allocation**, **no blur, no `saveLayer`, no `RenderEffect`**, radial gradients for glows and a **vector wordmark**.

Done means:
- the pure Kotlin ports (`CubicBezier`, `LaunchClock`, `LaunchLayout`, `FlightPath`, `StatusLineLayout`, `LaunchTimeline` opening) pass the ported iOS test cases with the iOS reference values;
- `MainActivity` follows the exact launch order, the keep condition is released from layout (`onPlaced`), and frame 0 matches the system splash in light and dark on the `pixel` emulator;
- Reduce Motion shows the static launch frame; TalkBack sees one node "ChronoFlow is opening";
- traces `launch` and `first-commit` exist; `StartupBenchmark` (cold) and `LaunchJankBenchmark` produce their metrics, and the numbers are in `docs/perf-startup.md`.

**End state of this phase [R, OQ-3a-1]:** there is no ending yet (3b). The overlay plays the opening, then holds the 1500 ms frame, stops its frame loop and keeps the app below hidden from accessibility and touch. Use the seed profile `CF_UI_TEST_PROFILE=screen` (0e) to work on the app without the overlay.

## 2. Inputs to read (in this order)

1. `plan.md`: A3, A9, A10, A11, A19, A21; "The spec" table rows 188, 205, 277, 285, 289; "Android spec: process, activity and launch sequence"; "Android spec: seeds and test hooks"; "Performance budget"; Phase 3a (this), 3b and 3c (what consumes your API).
2. `AGENTS.md`: "Performance", "Tests", "Traps" (splash keep condition; animator scales).
3. Research:
   - `docs/research/05-launch-auth.md` §2.2 (board values iOS overrides), §3.1–§3.3, §3.6–§3.9, §15.1–§15.3, §15.10–§15.11;
   - `docs/research/12-built-apps.md` §4.1;
   - `docs/research/08-design-system.md` §6 (sky ramp, dithering), §11.4 (Android 12+ splash), §12 (Reduce Motion, no haptics).
4. Boards (read-only):
   - `DC/SplashSpec.dc.html`: key frames 0/700/1400 (ready mode) are this phase; "Timing", "Platforms and settings" ("Android 12+ … Keep it on screen until the first frame is drawn, then play the same animation in the app").
   - `DC/SplashScreen.dc.html`: geometry and keyframes (CSS in `<helmet>` and `renderVals`). **The board is older than the iOS decisions** — see §5.1.
   - `DC/AppIcon.dc.html`: no splash geometry; it only confirms the launcher icon is a different asset. The splash uses `launch_moon`, never `ic_launcher`.
5. iOS references (`I/` at `f391a1fa`):
   - `plan.md:685` "Phase 2: System launch screen (`feat/phase-2-launch-screen`)"
   - `plan.md:694` "Phase 3a: Pure timeline (`feat/phase-3a-timeline`)"
   - `plan.md:717` "Phase 3b: Core Animation scene (`feat/phase-3b-scene`)"
   - `plan.md:735` "Phase 3c: Splash snapshots (`feat/phase-3c-snapshots`)"
   - `plan.md:753` "Phase 3b review fixes"
   - `plan.md:922` "Phase 3c review fixes (after Phase 5)"
   - `plan.md:188` "Splash layer tree and geometry (any `w × h`)"
   - `plan.md:205` "Timeline: shared opening (all modes)"
   - `plan.md:277` "Reduce Motion", `:285` "Appearance", `:289` "Accessibility"
   - `plan.md:88–100` S1–S6 (board conflicts and their settled values)
   - `Kit/Launch/LaunchTimeline.swift`, `Kit/Launch/LayerID.swift`, `Kit/Launch/LaunchLayout.swift`, `Kit/Launch/FlightPath.swift`, `Kit/Launch/StatusLineLayout.swift`
   - `Kit/AppCore/CubicBezier.swift`, `Kit/AppCore/LaunchClock.swift`
   - Drawing reference (do not port the CA code, only its geometry and paint): `Kit/Launch/LaunchScene+Layers.swift`, `Kit/Launch/LaunchScene.swift` (moon pixel rounding l.62–67), `Kit/Launch/LaunchRadialLayer.swift`, `Kit/Launch/LaunchGlow.swift`
   - `Kit/DesignSystem/CfSky.generated.swift` l.379–424 (`LaunchPaint` halo stops and plane alphas) and its generator `I/Tools/TokenGen/Sources/TokenGenCore/Generator.swift` l.86–107 (board guard, halo and plane tables) and l.120–130 (`blurredDisc`)
   - `I/Tools/AssetGen/Sources/AssetGenCore/Raster.swift` l.9–48 (`WordmarkLayout`)
   - `Kit/Launch/Resources/Localizable.xcstrings`
   - Tests to port: `I/Packages/ChronoFlowKit/Tests/AppCoreTests/CubicBezierTests.swift`, `I/Packages/ChronoFlowKit/Tests/LaunchTests/{LaunchTimelineTests,FlightPathTests,LaunchLayoutTests,DeviceGeometryTests,LaunchGlowTests,LaunchRadialLayerTests,TimelineTestSupport,LaunchSnapshotGeometry}.swift`
6. What earlier phases give you (verify before Task 1; if something is missing, stop and report):
   - 0b: `app/src/main/res/values{,-night}/colors_launch.xml` (`launch_sky_top`), `drawable-xxxhdpi/launch_moon.png` and `drawable-night-xxxhdpi/launch_moon.png` (1152 px), `core/designsystem/src/main/res/font/fraunces_400_opsz32.ttf` (hash in `core/designsystem/fonts.sha256`), the generated `CfColors` (`skyTop`, `skyHorizon`, `skyTitle`, `dawn`; light and dark sets) and `SkyRamp.light`/`SkyRamp.dark` (`List<SkyStop>`) with `CfSky.brush(dark)`, **and `LaunchPaint`** (`white`, `haloPositions`, `haloAlphas`, `planeAlphas`, all `FloatArray`; OQ-0b-2) in `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/generated/CfSky.generated.kt`; `make assets` (`python3 -I tools/assets/build_assets.py icons moon`), `make assets-check`, the manifest `tools/assets/assets.sha256`, `docs/assets.md`. `lint.xml` carries `UnusedResources` ignores for `launch_moon.png` and `colors_launch.xml` that name this phase (OQ-0b-9).
   - 0c: `R.string.launch_chronoflow_is_opening` ("ChronoFlow is opening" / "ChronoFlow wordt geopend") and `R.string.launch_getting_your_plan_ready` ("Getting your plan ready" / "Je plan wordt klaargezet"), verified in `core/ui/src/main/res/values{,-nl}/strings_ios.xml`.
   - 0e: `core/testing/src/main/resources/profiles.json` (profiles `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`; the emulator is `pixel`) + `core/testing/.../DeviceProfiles.kt`, `ProfileRule`/`ProfileHost`/`BoardCapture` (`core/testing-compose`), tier annotations (`core/testing/.../tiers/`: `@Logic`, `@Layout`, `@Capture`, `@Smoke`, `@Perf`, `@PerfBudget`, `@TimingGate`), `app/src/debug/.../debug/{SeedReceiverActivity,TestSeed,TestSeeds,UITestTimeline}.kt` (`CF_UI_TEST_PROFILE`, `cf.reduceMotion`, `cf.freezeAt`, `cf.readyAt`, `cf.mainThreadStall` are already parsed), `ScreenTest` (`app/src/androidTest/kotlin/net/chronoflow/app/testing/ScreenTest.kt`: `launch(overrides)`, `waitFor(timeout) { }`), `:benchmark` with `StartupBenchmark` (one test, `startupCold`, `Partial(BaselineProfileMode.Require)`), `benchmark/budgets.json`, `Scripts/check-benchmarks.py`, `Scripts/perf-startup.sh`.
   - 2: `ChronoFlowTheme(darkTheme = isSystemInDarkTheme(), reduceMotion = LocalReduceMotion.current)`, `LocalReduceMotion`, `ReduceMotion.resolve(animatorsEnabled, override)` (`theme/CfGeometry.kt`), `LocalSafeInsets` (`core/designsystem/.../layout/`), `warmFonts()`/`warmFontsAfterFirstFrame()` (`theme/FontWarmup.kt`), `@BoardMetrics` (`layout/BoardMetrics.kt`), the Konsist rule set (`konsist/src/test/kotlin/net/chronoflow/konsist/`, including `noHapticsInLaunch`).
   - **The app today (verified at 2b7114f).** `feature/launch` is an empty module (convention plugin and namespace only, no sources and no test dependencies). `app` already depends on `:feature:launch`. The catalog entry `androidx-core-splashscreen` (1.2.0) exists but `app/build.gradle.kts` does not use it yet. `MainActivity` never calls `installSplashScreen()` or `enableEdgeToEdge()`, has no `CfRoot`, and composes `ChronoFlowTheme` → `Box(app.root, testTagsAsResourceId)` → `entry?.invoke()` inline, where `entry = TripPageEntry.content(intent, graph)` (debug and perf variants only). The manifest `android:theme` is `Theme.ChronoFlow` for application and activity; `values/themes.xml` and `values-night/themes.xml` each define that one style (parents `android:Theme.Material.Light.NoActionBar` / `android:Theme.Material.NoActionBar`).

**API endpoints:** none. This phase does no network or disk work.

## 3. Out of scope (deferred, with owner)

| Item | Owner |
|---|---|
| Endings `withReadyExit`, `withStatus`, the slow status line drawing and its dots, forced 8 s exit, `LaunchReadiness`, `LaunchCoordinator`, `StubReadiness`, warm-start 300 ms fade, process-death cold launch, `ready`/`exit` traces, `reportFullyDrawn()`, `LaunchStallTest`, `cf.mainThreadStall`, `cf.readyAt` | 3b |
| `withWelcomeExit`, `MoonTarget.fallback`, `MoonAnchorStore`, Welcome, intro, `intro` trace | 3c |
| Session restore, DataStore, `session` trace | 4a–4e |
| Boards `SplashSlow`, `SplashWelcome`, `Main` | 3b, 3c |
| Device-only baseline-profile runs | 13 (`make baseline-profile` is in the stack: 3a regenerates it on the emulator, Task 10) |

Port the **whole** `LaunchTimeline.swift` (including `append`, `replace`, `withoutExit`, repeat and replica support), because 3b builds on it; do not port `LaunchTimeline+Ready.swift` or `+Welcome.swift`.

## 4. Platform facts this phase relies on [F unless marked]

- **System splash (API 31+, backported to 29–30 by `androidx.core:core-splashscreen` 1.2.0):** background color + centred icon only. An icon *without* background is a 288 × 288 dp canvas, visible inside a 192 dp circle. Shown on cold and warm starts. Dark/light only through `-night` resources. Android 13+ shows the icon for non-launcher starts only with `android:windowSplashScreenBehavior="icon_preferred"` (API 33 attribute); Android 12/12L may show the background without the icon for link/notification starts (accepted, 05 OQ A3). (`05 §15.1`, `08 §11.4`)
- **Keep condition:** `setKeepOnScreenCondition` is checked from a pre-draw listener that cancels the app's draw while it returns true. A flag set while drawing never flips → the app hangs on the splash. Compose runs measure/layout (and `Modifier.onPlaced`) in `AndroidComposeView.onLayout`, before pre-draw, so a flag set in `onPlaced` releases the splash in the same traversal. (`AGENTS.md` Traps; plan A11 corrects `05 §15.1`'s `overlayFrameDrawn`.)
- **Edge-to-edge** is enforced for targetSdk 35+; `enableEdgeToEdge()` is required. (`05 §15.3`)
- **Compose animation APIs scale with the animator duration scale.** The overlay must not use `animate*AsState`, `Animatable`, `rememberInfiniteTransition` or `delay`; it samples `LaunchTimeline` from a wall clock. (`05 §15.2`)
- Functional emulator runs set the animator scales to 0 (`AGENTS.md` Traps), which the app treats as Reduce Motion (`plan.md` spec row 277, `05 §15.10`). Perf runs set them to 1 (`make perf` does it).

## 5. The launch spec

All geometry is in **dp** (iOS pt → dp). `w`, `h` = window size in dp; `cy = h / 2`. Every board `h − k` becomes `cy + (422 − k)` (user, 2026-11-06). Times are ms from `t0`.

### 5.1 Board vs settled values [F `I/plan.md:88–100`, `05 §2.2`]

| `SplashScreen` board says | Build this | Source |
|---|---|---|
| `pattern-dots` stars layer | **No stars layer** | S2, user 2026-11-06 |
| Moon top 340 → centre y 392 | Moon centred: `(w/2, cy)` (422 on 844) | S1 |
| Wordmark top 432, status top 488 | Wordmark box top `cy + 40` (462), status top `cy + 96` (518) | S1 (composition shifted with the moon) |
| Path `stroke-dasharray 560` | `strokeEnd` = the plane's arc-length fraction | S5 |
| CSS keyframes in % of 4200/5200/7000 ms | iOS keyframes in §5.3 | prose wins |

Captures will therefore differ from the board renders in exactly these places. Anything else that differs is a bug.

### 5.2 Layers, geometry and paint, in draw order [F `I/plan.md:188–203`, `Kit/Launch/LaunchLayout.swift`, `LaunchScene+Layers.swift`, `LaunchRadialLayer.swift`, `LaunchGlow.swift`, `CfSky.generated.swift`]

Colors come from the **captured appearance** (§5.6), from the generated tokens: `skyTop`, `skyHorizon`, `dawn`, `skyTitle` and the `CfSky` stops. Light / dark for reference only (never type these hex values): skyTop `#1F285A`/`#06091C`, skyHorizon `#EB9666`/`#AA5830`, dawn `#E78A45`/`#F2AD73`, skyTitle `#F4F9FF`/`#E5ECF6`.

| # | Layer (`LayerID`) | Geometry | Paint |
|---|---|---|---|
| 0 | container | full window, ignores insets, clips to bounds; scale origin `(0.5, 0.4)` | background `skyTop`; `opacity` and `transform.scale` applied as a `graphicsLayer` (both 1 in this phase) |
| 1 | sky | full window | `CfSky` vertical ramp (every stop 0b generated for the captured theme; `08 §6` computed 33 per theme, iOS has 17 — never hard-code the count), drawn with `alpha = sky.opacity` |
| 2 | halo | circle radius **250**, centre = sun centre + `(0, halo.translationY)` | radial gradient radius 250, color `dawn`, `LaunchPaint.haloPositions` / `LaunchPaint.haloAlphas` (83 stops, already scaled by 0.45; position = `r/250`; alpha 0 for `r < 90`) |
| 3 | sun | circle radius **90** (the 180 × 180 clip), centre `(w/2, cy + 228)` + `(0, sun.translationY)` | radial gradient radius **127.3**: `dawn` α1 at 0, α1 at `70/127.3`, α0 at `91.6/127.3`, α0 at 1. The α0 stops are `dawn` with alpha 0, **not** `Color.Transparent` |
| 4 | band | rect top `cy + 48`, to the window bottom, full width | vertical gradient over the band rect: `skyHorizon` α 0 / 0.22 / 0.70 / 1.0 at 0 / 0.40 / 0.75 / 1.0 |
| 5 | path | polyline through `FlightPath.positionValues` (128 points; see §5.4), stroked from its start to `strokeEnd × polylineLength` | white α **0.55** × `path.opacity`, width **1.5**, butt caps |
| 6 | plane | centre = `plane.position` (no rotation) | one radial gradient radius **24**: 49 stops at `r = i × 0.5` (i = 0…48): alpha **1** for `r ≤ 4` (the 8 dp dot), else `LaunchPaint.planeAlphas[i]` (already scaled by 0.8); color `LaunchPaint.white`; `alpha = plane.opacity`. One draw pre-composites dot over glow, so fading is a true group fade [R] |
| 7 | moon | `launch_moon` bitmap at its decoded size (288 dp), centred on `moon.position`, scaled by `moon.transform.scale` about its centre | `drawImage`, default filter quality. **Frame 0 placement is integer pixels:** `topLeftPx = IntOffset((wPx − bmpW) / 2, (hPx − bmpH) / 2)` (integer division, as `Gravity.CENTER`); later frames add `(moon.position − (w/2, cy)) × density` to it [R; iOS also pixel-aligns the moon, by rounding its centre to device pixels, `LaunchScene.swift:63–67`; Android uses the integer top-left instead because that is how the system splash places the icon] |
| 8 | wordmark | box `WIDTH_DP × 40`, centred at `(w/2, cy + 60)` + `(0, wordmark.translationY)` | generated vector path (Task 4), fill `skyTitle`, `alpha = wordmark.opacity` |
| 9 | status, statusDot | rect `x 0, y cy + 96, w × 20` | **not drawn in 3a** (opacity 0 in the opening; 3b draws it) |

Constants that belong to the board (250, 90, 127.3, 70, 91.6, 228, 48, 60, 96, 1.5, 0.55, 24, 4, the band stops) live in one `LaunchMetrics` object in the model package, annotated `@BoardMetrics("SplashScreen")` (`net.chronoflow.core.designsystem.layout.BoardMetrics`, SOURCE retention, no android import, so it is allowed in the pure model package; `boardMetricsNamesExist` checks that `_design/canvas/SplashScreen.dc.html` exists; AGENTS: "a screen `*Metrics` object that names its board").

**On 390 × 844** [F `LaunchLayoutTests.swift`]: moon centre (195, 422); sun centre (195, 650); band (0, 470, 390, 374); path start (−10, 640), c1 (90, 470), c2 (300, 470), end (400, 640); wordmark centre (195, 482); status (0, 518, 390, 20); path apex (195, 512.5); path length 505.8 ± 0.2.

**Radial gradients that move** [R]: build each radial `Brush` once per size with a **fixed local centre** `center = Offset(R, R)` and `radius = R` (px), then draw it as `translate(cx − R, cy − R) { drawCircle(brush, radius = r, center = Offset(R, R), alpha = …) }`. For the halo `R = r = 250 dp`; for the plane `R = r = 24 dp`; for the sun `R = 127.3 dp` and `r = 90 dp`. Do not use `Offset.Unspecified`: it resolves against the whole `DrawScope` size, not the circle. The shader lives in local coordinates, so `translate` moves it, and `ShaderBrush` caches its shader per draw size, so a moving centre allocates nothing.

### 5.3 The opening timeline [F `Kit/Launch/LaunchTimeline.swift:54–98`, `I/plan.md:205–219`]

`opening(size, reduceMotion)` first sets, for **every** `LayerID`, `opacity` = 1 for `container` and `moon`, else 0 (key time 0); plus `container transform.scale` 1, `moon position` `(w/2, cy)`, `moon transform.scale` 1, `wordmark transform.translation.y` 8, `path strokeEnd` 0, `sun`/`halo transform.translation.y` = `reduceMotion ? 0 : 110`. With Reduce Motion it stops there (18 tracks, all key times `[0]`). Otherwise it replaces these tracks (19 tracks in total):

| Layer | Key path | Key times (s) | Values | Curves per segment |
|---|---|---|---|---|
| sky, band | opacity | 0, 0.3, 1.1 | 0, 0, 1 | linear, easeOut |
| sun, halo | opacity | 0, 0.4, 1.3 | 0, 0, 1 | linear, easeOut |
| sun, halo | transform.translation.y | 0, 0.4, 1.3 | 110, 110, 0 | linear, easeOut |
| path | opacity | 0 | 1 | — |
| path | strokeEnd | 0, 0.5, 0.56, 1.4 | 0, 0, 0.03, 1 | linear ×3 |
| plane | position | `[0] + FlightPath.positionKeyTimes` (129) | `[start] + positionValues` | linear |
| plane | opacity | 0, 0.5, 0.56, 1.4, 1.5 | 0, 0, 1, 1, 0 | linear ×4 |
| moon | position | 0, 0.3, 1.1 | `(w/2,cy)`, `(w/2,cy)`, `(w/2,cy−28)` | linear, easeOut |
| moon | transform.scale | 0, 0.3, 1.1 | 1, 1, 0.86 | linear, easeOut |
| wordmark | opacity | 0, 1.0, 1.4 | 0, 0, 1 | linear, easeOut |
| wordmark | transform.translation.y | 0, 1.0, 1.4 | 8, 8, 0 | linear, easeOut |

Curves (`CubicBezier`): `easeOut (0, 0, 0.58, 1)`, `easeInOut (0.42, 0, 0.58, 1)`, `intro (0.2, 0.8, 0.2, 1)`, `linear (0, 0, 1, 1)`.

`Track.value(at seconds)`: with a repeat interval and `t > keyTimes[0]`, `t = keyTimes[0] + (t − keyTimes[0]) mod repeat`; `t ≤ keyTimes[0]` → first value; past the last key → last value; else interpolate between the bracketing keys with `curves[lower].value(progress)`. Points interpolate per component.

**Reference samples on 390 × 844** [F `LaunchTimelineTests.swift:58–64`], tolerance 1e-6:

| ms | moon y | moon scale | sky = band | wordmark opacity (translationY = 8 × (1 − o)) | plane opacity | sun = halo opacity (translationY = 110 × (1 − o)) |
|---|---|---|---|---|---|---|
| 0 | 422 | 1 | 0 | 0 | 0 | 0 |
| 300 | 422 | 1 | 0 | 0 | 0 | 0 |
| 700 | 402.829990752 | 0.9041499538 | 0.6846431874 | 0 | 1 | 0.4883490027 |
| 1100 | 394 | 0.86 | 1 | 0.3781381308 | 1 | 0.9245001910 |
| 1400 | 394 | 0.86 | 1 | 1 | 1 | 1 |
| 1500 | 394 | 0.86 | 1 | 1 | 0 | 1 |

Plane opacity at 530 ms and 1450 ms = 0.5 (± 1e-12). `sample(−10 ms) == sample(0)`; `sample(20 s) == sample(1500 ms)`. The first animated change is in `(300 ms, 300 ms + 1 frame]`.

### 5.4 Flight path [F `Kit/Launch/FlightPath.swift`, `FlightPathTests.swift`]

- Cubic from `LaunchLayout`: start `(−10, cy + 218)`, c1 `(90·w/390, cy + 48)`, c2 `(300·w/390, cy + 48)`, end `(w + 10, cy + 218)`.
- Arc-length table: 1024 points at `t = i/1023`, cumulative `hypot` distances; `length` = last entry. `point(atFraction f)`: clamp f to [0,1], binary-search the table, interpolate the parameter, evaluate the cubic.
- `fraction(at t seconds)`: `t ≤ 0.5 → 0`; `t < 0.56 → (t − 0.5)/0.06 × 0.03`; `t < 1.4 → 0.03 + (t − 0.56)/0.84 × 0.97`; else 1.
- `positionFractions` = `{i/126 | i = 0…126} ∪ {0.03}`, sorted (128 values). `positionKeyTimes`: `f ≤ 0.03 → 0.5 + f/0.03 × 0.06`, else `0.56 + (f − 0.03)/0.97 × 0.84` (first 0.5, last 1.4, contains 0.56). `positionValues` = `point(atFraction)` of each.
- The **drawn path is the polyline** through `positionValues` (iOS found cubic trimming drifts from arc length, `FlightPath.swift:36`). Android draws it with a `PathMeasure` over that polyline, `getSegment(0, strokeEnd × length, reusablePath, true)` into a path that is `rewind()`-ed each frame.

### 5.5 Reduce Motion [F `I/plan.md:277–283`; R `plan.md` spec row 277]

- Read once per process at the first `MainActivity.onCreate`: `ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), override)` (`CfGeometry.kt`; no binder call). The debug seed `cf.reduceMotion` is the override (today `RootOverrides.reduceMotion(intent)`). Store it in `LaunchController`; the overlay never re-reads it. `MainActivity` keeps its existing `onResume` re-resolve for `ChronoFlowTheme(reduceMotion)`; only the overlay uses the captured value.
- The opening is the static launch frame: skyTop + moon only, no lift, no wordmark. The frame loop never starts; one frame is drawn.
- Everything else in "### Reduce Motion" (300 ms fade, status) is 3b.

### 5.6 Appearance and system bars [F `I/plan.md:285–287`; R plan spec row 285, `05 §15.3`]

- Captured **once per process** in `MainActivity.attachBaseContext`, from `newBase.resources.configuration.uiMode and UI_MODE_NIGHT_MASK`. (`ConfigurationPolicy.overrideConfiguration(newBase)` caps the font scale, coerces the locale and sets the layout direction, and never touches `uiMode`; the activity applies it with `applyOverrideConfiguration` after `super.attachBaseContext`. `ConfigurationPolicy.wrap(context)` is for non-activity contexts.) On API 31+ this already includes an app night mode set through `UiModeManager`; on 29–30 it is the system mode (OQ-A23).
- The overlay uses the captured appearance for every color and for the moon bitmap, even if the system mode changes during the splash (`uiMode` is in `configChanges`, so the activity is not recreated). If the current night bit differs from the captured one when decoding, decode from `createConfigurationContext(config with the captured night bit)`.
- `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(TRANSPARENT), navigationBarStyle = if (night) SystemBarStyle.dark(TRANSPARENT) else SystemBarStyle.light(TRANSPARENT, TRANSPARENT))`, then `window.isNavigationBarContrastEnforced = false` (OQ-3a-3). While the overlay is shown the status-bar icons stay light (dark sky). Whenever no overlay is shown (profile `screen` now, the hand-off from 3b on) the icons of both bars follow the theme (OQ-A67, Task 7b); the styles come from the pure `SystemBarStyles` function of Task 7b, not from literals here.

### 5.7 Accessibility [F `I/plan.md:289–294`; R plan spec row 289, `05 §15.11`]

- The overlay is **one** semantics node: `contentDescription` and `paneTitle` = `stringResource(R.string.launch_chronoflow_is_opening)`, `role = Role.Image` (iOS `accessibilityTraits = .image`, `LaunchOverlayController.swift:93`; `05 §15.11` "image role"), test tag `launch.opening` (the iOS identifier, `LaunchOverlayController.swift:94`). Its children add no semantics.
- While the overlay is shown, the app content below carries `Modifier.semantics { hideFromAccessibility() }` (the non-deprecated name of plan's `invisibleToUser`) and receives no pointer input: the overlay consumes every pointer event.
- No haptics (Konsist rule from Phase 2), no `announceForAccessibility`.

### 5.8 Clock [R, plan A11 and spec step 4]

- `t0` = `nanoTime()` at the overlay's **first draw** (also the end of `first-commit`). `nanoTime` is injected (`System::nanoTime` in production).
- A frame loop `while (!settled) withFrameNanos { frameNanos -> elapsed.longValue = if (t0 == UNSET) 0 else max(0, frameNanos − t0) }` drives a `mutableLongStateOf`. The `UNSET` check is required: the first frame callback runs before the first draw, and `frameNanos − Long.MIN_VALUE` overflows. Choreographer frame times share `System.nanoTime`'s base. A stalled main thread therefore skips frames and never stretches the timeline.
- The draw lambda and the container `graphicsLayer` block are the **only** readers of `elapsed`. Composition never reads it.
- The loop stops when `elapsed ≥ settledAfter` (the largest last key time of the non-repeating tracks; infinite if a repeating track exists). With `cf.freezeAt` (debug), `elapsed` is fixed and no loop runs.

## 6. Kotlin shape [R]

Model package `net.chronoflow.feature.launch.model` (pure Kotlin; Konsist forbids `android.*` and `androidx.*` imports there). Doubles throughout, so values compare with iOS exactly. Keep the Swift type names.

```kotlin
class CubicBezier(val x1: Double, val y1: Double, val x2: Double, val y2: Double) {   // equals/hashCode on the 4 values
    fun value(progress: Double): Double   // iOS algorithm verbatim: endpoints, linear shortcut, 8 Newton steps, 48 bisection steps, 1e-13 / 1e-10
    companion object { val easeOut: CubicBezier; val easeInOut: CubicBezier; val intro: CubicBezier; val linear: CubicBezier }
}
data class LaunchPoint(val x: Double, val y: Double)
data class LaunchSize(val width: Double, val height: Double)
data class LaunchRect(val x: Double, val y: Double, val width: Double, val height: Double) { minX/maxX/minY/maxY, union, contains }
object MoonTarget { data class Insets(val top: Double, val left: Double, val bottom: Double, val right: Double) }   // 3c adds fallback()
enum class LayerID(val rawValue: String) { Container("container"), Sky("sky"), Halo("halo"), Sun("sun"), Band("band"), Path("path"), Plane("plane"), Moon("moon"), Wordmark("wordmark"), Status("status"), StatusDot("statusDot") }
object KeyPath { const val OPACITY = "opacity"; const val SCALE = "transform.scale"; const val TRANSLATION_Y = "transform.translation.y"; const val POSITION = "position"; const val STROKE_END = "strokeEnd" }
sealed interface TrackValue { data class Scalar(val value: Double); data class Point(val point: LaunchPoint) }
data class LayerValues(val properties: Map<String, TrackValue>, val replicaOpacities: List<Double>)

class LaunchTimeline private constructor(val tracks: List<Track>, val size: LaunchSize, val reduceMotion: Boolean) {
    class Track(layer, keyPath, keyTimes: DoubleArray, xs: DoubleArray, ys: DoubleArray? /* null = scalar */, curves: Array<CubicBezier>,
                repeatInterval: Double?, replicaCount: Int = 1, replicaDelay: Double = 0.0) {
        fun scalarAt(seconds: Double): Double; fun xAt(seconds: Double): Double; fun yAt(seconds: Double): Double   // allocation-free
        fun sample(at: Duration): TrackValue                                                                    // test path, allocates
        val values: List<TrackValue>                                                                            // test path
    }   // equals via contentEquals; preconditions as Swift (non-empty, counts, strictly increasing key times)
    fun sample(at: Duration): Map<LayerID, LayerValues>          // iOS-shaped, allocates; tests and 3b/3c logic only
    internal fun set(...) / replace(track) / append(layer, keyPath, start, end, value, curve) / withoutExit()
    companion object { fun opening(size: LaunchSize, reduceMotion: Boolean): LaunchTimeline }
}
class LaunchFrame { var containerOpacity; containerScale; skyOpacity; bandOpacity; haloOpacity; haloTranslationY; sunOpacity; sunTranslationY;
                    pathOpacity; strokeEnd; planeOpacity; planeX; planeY; moonOpacity; moonX; moonY; moonScale; wordmarkOpacity; wordmarkTranslationY;
                    statusOpacity; val statusDotOpacity = DoubleArray(3) }
class LaunchFrameSampler(timeline: LaunchTimeline) {       // resolves each LaunchFrame field to its Track once
    val settledAfterSeconds: Double
    fun sampleInto(seconds: Double, out: LaunchFrame)       // zero allocation; missing track → the opening default
    fun containerOpacity(seconds: Double): Double; fun containerScale(seconds: Double): Double
}
interface LaunchClock { val now: Duration; suspend fun sleepUntil(deadline: Duration) }
class MediaLaunchClock(val t0Nanos: Long, private val nanoTime: () -> Long = System::nanoTime) : LaunchClock  // now = max(0, nanoTime() − t0); sleepUntil = delay(remaining)
```

`ManualLaunchClock` (iOS `LaunchClock.swift:34–67`: `advance(to)`, `pendingSleeps`, cancellation removes the sleeper) goes in `feature/launch/src/test/kotlin/…/model/` for the 3a test and later phases.

## 7. Tasks

Each task is one Conventional Commit with no trailers. Tick the matching `plan.md` checkbox in the commit that completes it (mapping in brackets). Run only the tests you touched (`make targeted ONLY="…"`) per round; run `make quick` once before the last commit, and `make check` at the end (the orchestrator runs emulator tiers if your sandbox cannot; report the exact command and output).

### Task 1: Bezier and clock (`feat(launch): port cubic bezier and launch clock`)

- Files: `…/launch/model/CubicBezier.kt`, `…/launch/model/LaunchClock.kt`; test `ManualLaunchClock.kt`; `feature/launch/build.gradle.kts` (the module is empty: copy the dependency block of `feature/trip/build.gradle.kts` for what the launch tests need: `core:testing`, `core:testing-compose`, `junit4`, `robolectric`, `androidx.test.core`, `androidx.test.ext.junit`, `coroutines-test`, and `implementation(libs.kotlinx.coroutines.core)` for the clock).
- **Test tiers.** `:feature:launch` is not in the Makefile `JVM_MODULES`: its tests run under `testDebugUnitTest`, plain JVM test classes carry no tier annotation (Konsist `TierRules.plainJvmClassesHaveNoTier`), while Robolectric classes carry `@Logic` or `@Layout` and emulator classes `@Capture` or `@Smoke` (`TierRules` enforce both). "JVM" in this brief means a plain JVM test with no Compose, no Robolectric runner and no tier annotation. `make targeted ONLY="feature:launch:<Class>"` resolves a class by its file name under the module.
- Behaviour: §6. `CubicBezier` requires `x1, x2 ∈ [0,1]` and finite `y1, y2` (`require`). `value`: `require(progress.isFinite())`, then `p ≤ 0 → 0`, `p ≥ 1 → 1`, `x1 == y1 && x2 == y2 → p` (`CubicBezier.swift:22–48`).
- Konsist: add the rule "files in `net.chronoflow.feature.launch.model` import no `android.*`, `androidx.*`" as `…ImportNoAndroid…(scope)` in `KonsistRules.kt`, next to `jvmCoreModulesImportNoAndroidOrCompose` (which forbids `android.` and `androidx.compose.` in the JVM core modules), with the three tests every rule has in `KonsistRulesTest.kt` (the rule on `projectScope`, `…RejectsPlantedViolation`, `…AllowsControl`) and fixtures under `konsist/src/test/resources/{violations,allowed}/<rule>/`.
- Tests (JVM, `feature/launch/src/test/kotlin/net/chronoflow/feature/launch/model/`):
  - `CubicBezierTests` — port all five iOS tests with the same names (with their independent de Casteljau bisection reference, 60 steps, `CubicBezierTests.swift:26–40`): `presetsAgreeWithIndependentBisectionToOneTenThousandth`, `introNearEndpointAgreesWithIndependentBisection` (intro(0.95) = 0.9997243836889174 ± 1e-12), `cssReferenceValues` (easeOut at 0.1/0.25/0.5/0.75/0.9 = 0.1605721542, 0.3781381308, 0.6846431874, 0.9065353493, 0.9829733903; easeInOut = 0.0197224535, 0.1291619310, 0.5, 0.8708380690, 0.9802775465; intro = 0.3966935490, 0.7672843007, 0.9460794615, 0.9911081873, 0.9988302593; ± 1e-4), `flatDerivativesAndOvershootingY`, `presetControlsAndMonotonicity`.
  - `LaunchClockTests` — `MediaLaunchClock.now` with an injected `nanoTime` (before `t0` → 0; `t0 + 700 ms` → 700 ms); `ManualLaunchClock` resumes sleepers at `advance`, returns immediately for a past deadline, and a cancelled sleeper is removed (`pendingSleeps == 0`). `runTest(timeout = 60.seconds)`.

### Task 2: Layout, flight path, status layout (`feat(launch): port launch layout and flight path`)

- Files: `…/launch/model/{LaunchGeometry.kt (LaunchPoint/Size/Rect), MoonTarget.kt, LaunchMetrics.kt, LaunchLayout.kt, FlightPath.kt, StatusLineLayout.kt}`.
- `LaunchLayout(size, safeAreaInsets = Insets(0,0,0,0))` exactly as `LaunchLayout.swift` (fields `bounds, safeBounds, moonCentre, sunCentre, band, wordmarkCentre, status, pathStart, pathControl1, pathControl2, pathEnd`; `boardY(k) = cy + (422 − k)`, sun `k = 194`, band top `k = 374`, path `k = 204`; wordmark centre `cy + 60`; status `(0, cy + 96, w, 20)`). `require(w > 0 && h > 0 && finite)`.
- `FlightPath(size)`: §5.4.
- `StatusLineLayout(width, textWidth, ascent, descent, lineHeight, scale)`: `rounded = ceil(textWidth × scale)/scale`; `left = (width − rounded − 6 − 18)/2`; `text = (left, (lineHeight − ascent − descent)/2, rounded, lineHeight)`; `dots = (left + rounded + 6, 0, 18, lineHeight)`; `ink = text ∪ dots`.
- Tests (JVM):
  - `LaunchLayoutTests` — port `everyD1cSizeKeepsTheBoardOffsetsAndStretchesTheBand` (use the iOS sizes **and** every `profiles.json` profile, plus 280 × 653; insets zero and `(62, 19, 34, 31)`) and `board844GeometryIsExactlyTheOriginalComposition`.
  - `FlightPathTests` — port `boardGeometryAndArcLength`, `boardCompositionAndWidthScaling`, `fractionCurveAnd128PositionKeys` (the polyline check becomes: the vertex list used for drawing equals `positionValues`), `lookupAgreesWithIndependentDenseIntegrationEvery10Milliseconds` (32,768-step reference, < 0.04).
  - `StatusLineLayoutTests` — `statusTextAndDotsKeepTheirBoardSpacingAndPixelAlignedWidth` (on iOS it sits in `DeviceGeometryTests.swift:19–25`): width 390, textWidth 150.1, ascent 12, descent 3, lineHeight 20, scale 2 → text `(107.75, 2.5, 150.5, 20)`, gap 6, dots width 18, `ink.minX + ink.maxX == 390`.

### Task 3: Opening timeline and sampler (`feat(launch): port the launch timeline opening`) [ticks "Pure ports"]

- Files: `…/launch/model/{LayerID.kt, LaunchTimeline.kt, LaunchFrame.kt}`.
- Behaviour: §5.3, §6. Port `set`, `replace`, `append` (requires `start ≥ last key`, `end > start`; inserts a hold key when `start > last`), `withoutExit` (rebuilds the opening and keeps `status`/`statusDot` tracks) and replica sampling (`replicaOpacities[i] = value(t − i × replicaDelay)`) even though only 3b uses them.
- `LaunchFrameSampler.sampleInto` must not allocate: primitive arrays, index loops, no boxing, no `List`/`Map`/iterator/lambda, no `TrackValue`.
- Tests (JVM):
  - `LaunchTimelineTests` — port with the same names: `everyOpeningScalarKeyAndCurve` (every row of §5.3, 19 tracks, moon position keys `(195,422),(195,422),(195,394)`, plane track = `[0] + positionKeyTimes` / `[start] + positionValues`, no repeat interval, unique tracks), `samplesAtAllSpecifiedOpeningTimes` (the §5.3 table), `sampledPlaneFollowsStrokeEvery10Milliseconds` (sizes 390×844, 375×667, 590×900; < 0.02), `reduceMotionHoldsOnlyTheLaunchFrame`, and `pureValuesAreUsableOffTheMainActor` → `pureValuesAreUsableOnAnotherThread` (sample on `Dispatchers.Default` at 427 × 911, 700 ms; moon opacity 1).
  - `LaunchTimelineTrackTests` — `append` keeps the opening (sample at 700 ms unchanged after appending a container opacity key 1.5 → 1.9 s); `withoutExit` drops it; constructor preconditions throw on unsorted key times and count mismatch.
  - `LaunchFrameSamplerTests` — `sampleIntoAgreesWithSampleEvery10Milliseconds` (0–3200 ms, normal and Reduce Motion, 390×844 and `small`); `firstAnimatedChangeComesWithinOneFrameAfter300Milliseconds` (frame `i` at `i / 120.0` s — divide, do not multiply by `1/120.0`, so frame 36 is exactly 0.3: all equal frame 0 up to 300 ms, the next one differs); `settledAfterIs1500MillisecondsAndZeroWithReduceMotion`; `sampleIntoAllocatesNothing` (`@PerfBudget` not needed; warm up 1,000 calls, then 10,000 calls; `(ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean).getThreadAllocatedBytes(Thread.currentThread().id)` delta ≤ the delta of two back-to-back reads).

### Task 4: Wordmark vector (`build(launch): generate the wordmark path`)

- **LaunchPaint: nothing to generate.** 0b already ported it (OQ-0b-2, OQ-0b-10): `tools/tokengen/.../Sky.kt` (`launchPaint`, `blurredDisc`, `Erf.kt`, with the `SplashScreen.dc.html` guard of the three board strings), `LaunchPaintGoldenTest` against the iOS golden, and the committed `LaunchPaint` object in `CfSky.generated.kt` (`haloPositions`/`haloAlphas` with 83 stops, `planeAlphas` with 49 values). Use it as it is. Do not add a second generator or a `LaunchPaint.generated.kt`. If a value must change, change `Sky.kt` and run `make generate`.
- **Wordmark** (`tools/assets/wordmark.py`, `python3 -I`, stdlib only, called by `make assets`, i.e. a new mode of `tools/assets/build_assets.py` or a second command in the `assets` target): input `core/designsystem/src/main/res/font/fraunces_400_opsz32.ttf` (its hash must equal the entry in `core/designsystem/fonts.sha256`). Guard: `DC/SplashScreen.dc.html` `word:` style must still say `font-size: 32px`, `line-height: 40px`, `letter-spacing: -0.01em` (iOS `WordmarkLayout` does the same check).
  - Shape and outline with `/opt/homebrew/bin/hb-view --font-size=upem --margin=0 --output-format=svg <ttf> ChronoFlow` (HarfBuzz 15.1.0; it applies GPOS kerning). Parse the `<g id="glyph-…">` paths (absolute `M/L/C/Z`, y down from the baseline) and the `<use x y>` placements. Read `upem` (`head`), `ascender`/`descender` (`hhea`) with `struct`. Verified values: upem 2000, ascender 1956, descender −510, total advance 11,453 units.
  - Scale `s = 32/2000`. Tracking −0.32 dp is added after **every** glyph (CoreText `kCTKern` semantics): glyph `i` shifts by `i × (−0.32)`; `advance = 11453 × s + 10 × (−0.32) = 180.048` dp.
  - iOS box rules (`Raster.swift:38–46`): `overhang = max(0, −inkMinX, inkMaxX − advance)`; `WIDTH_DP = ceil(advance + 2 × overhang)`; `HEIGHT_DP = 40`; `originX = (WIDTH_DP − advance)/2`; baseline from the top `= (40 − ascent − descent)/2 + ascent = 31.568` dp (ascent 31.296, descent 8.16).
  - Output, committed: `…/launch/generated/LaunchWordmark.generated.kt` = `object LaunchWordmark { const val WIDTH_DP: Float; const val HEIGHT_DP = 40f; const val ADVANCE_DP: Float; const val BASELINE_DP: Float; const val INK_LEFT_DP/INK_TOP_DP/INK_RIGHT_DP/INK_BOTTOM_DP: Float; const val PATH_DATA: String }` — one SVG path string in dp, 3 decimals, box-relative. Record input and output hashes in `tools/assets/assets.sha256`; `generators-check` (through `assets-check`) checks the manifest and never re-runs `hb-view` (same rule as the moon PNGs). Note the method in `docs/assets.md`. The Konsist design rules skip `/generated/` paths.
- Tests:
  - `Scripts/tests/test_wordmark.py` (next to `test_build_assets.py`; `Scripts/test_scripts.sh`, which `make quick` runs, discovers only `Scripts/tests`, `tools/contracts-gen`, `tools/l10n` and `tools/icons`, so a test under `tools/assets/` would not run unless you add that directory to it): the guard fails on a changed board string; a run is byte-identical to the committed file.
  - `LaunchWordmarkTests` (JVM): `ADVANCE_DP = 180.048 ± 0.001`, `BASELINE_DP = 31.568 ± 0.001`, `WIDTH_DP = ceil(ADVANCE_DP + 2 × overhang)`, the ink box lies inside `WIDTH_DP × 40`, `PATH_DATA` has at least 10 `M` commands and only `M L C Z` and numbers.
  - `LaunchWordmarkShapeTest` (Robolectric, `@Layout`): parse `PATH_DATA` with `PathParser().parsePathString(…).toPath()`; compare its bounds with `Paint.getTextPath("ChronoFlow")` of the same font (`textSize = 32`, `letterSpacing = −0.01`), aligned on the baseline and centred on the advance: each edge within 0.75 dp.

### Task 5: Splash theme (`feat(app): add the system splash theme`) [ticks "Splash theme"]

- Files: `app/build.gradle.kts` (add `implementation(libs.androidx.core.splashscreen)`; the catalog entry exists), `app/src/main/res/values/themes.xml`, `values-night/themes.xml`, `values-v30/themes.xml`, `values-v33/themes.xml`, `AndroidManifest.xml` (MainActivity `android:theme="@style/Theme.ChronoFlow.Starting"`; the `<application>` theme stays `@style/Theme.ChronoFlow`), `lint.xml` (delete the `UnusedResources` ignores for `launch_moon.png` and `colors_launch.xml` that name this phase, OQ-0b-9).
- `Theme.ChronoFlow.Starting` (parent `Theme.SplashScreen`, core-splashscreen app attributes, no `android:` prefix): `windowSplashScreenBackground = @color/launch_sky_top`, `windowSplashScreenAnimatedIcon = @drawable/launch_moon`, `postSplashScreenTheme = @style/Theme.ChronoFlow`. No icon background color. (The plan calls the styles `Theme.App.Starting` and `Theme.App`; the tree already has `Theme.ChronoFlow`, so keep that name [R, OQ-3a-12].)
- `Theme.ChronoFlow` (existing): add `android:windowBackground = @color/launch_sky_top`, transparent status and navigation bars, `android:windowLayoutInDisplayCutoutMode = shortEdges`, so the overlay covers the cutout like the system splash. **A style defined in `values-night` replaces the one in `values` entirely**, and the night qualifier outranks the version qualifier, so repeat every item in `values-night/themes.xml`, and give `always` (API 30+) to both `values-v30` and `values-night-v30`; or set the cutout mode once in `MainActivity.onCreate` (`window.attributes.layoutInDisplayCutoutMode`, `ALWAYS` from API 30, else `SHORT_EDGES`) and keep it out of the XML. Record the choice.
- `values-v33`: `Theme.ChronoFlow.Starting` adds `<item name="android:windowSplashScreenBehavior">icon_preferred</item>`.
- `Theme.ChronoFlow.Starting` needs no `values-night` twin: night colors and the night moon come from `values-night/colors_launch.xml` and `drawable-night-xxxhdpi/launch_moon.png`.
- Test: `SplashThemeTest` (Robolectric, `@Layout`, `app/src/test`), at API 29, 31 and 36, light and night: the resolved `windowSplashScreenBackground` equals `R.color.launch_sky_top` (`#1F285A` / `#06091C` read from the resource, not typed), the icon is `R.drawable.launch_moon`, `postSplashScreenTheme` resolves `android:windowBackground` to the same color; at 36 `windowSplashScreenBehavior == icon_preferred`.

### Task 6: Controller and overlay drawing (`feat(launch): draw the launch overlay on a wall clock`) [ticks "LaunchOverlay"]

- Files: `…/launch/{LaunchController.kt, LaunchAppearance.kt, LaunchOverrides.kt, LaunchTrace.kt, LaunchOverlay.kt, LaunchScenePainter.kt}`.
- `LaunchController` (process-level; constructed lazily by `AppGraph`; no I/O in its constructor). Plain (non-snapshot) fields, main-thread only:
  - `captureAppearance(configuration)` (first call wins), `captureReduceMotion(system: Boolean, override: Boolean?)` (first call wins);
  - `claimOverlay(skip: Boolean): Boolean` — true exactly once per process unless `skip` (profile `screen`);
  - `overlayClaimedByCurrentActivity`, `overlayPlaced`, `splashHeld` (set from `overrides.holdSplash` in `claimOverlay`; a debug test clears it on the main thread, Task 9), `holdsSystemSplash(): Boolean = (overlayClaimedByCurrentActivity && !overlayPlaced) || splashHeld`;
  - `markOverlayPlaced()` — called **only** from `Modifier.onPlaced`;
  - `t0Nanos` (`UNSET = Long.MIN_VALUE`), `onFirstDraw(moonRectPx)` (sets `t0`, ends `first-commit`, stores the frame-0 moon rect), `openingDone`, `onOpeningSettled()` (ends `launch` in 3a; 3b moves it to removal), `systemIconBoundsPx` (set by the exit listener), `clock: LaunchClock` (a `MediaLaunchClock(t0)` once `t0` is set), `nanoTime: () -> Long`.
- `LaunchOverrides(reduceMotion: Boolean?, freezeAt: Duration?, holdSplash: Boolean, skipOverlay: Boolean)` with `LaunchOverrides.None` for release; the debug implementation in `app/src/debug` parses `TestSeed.fromIntent(activity.intent)` (`TestSeeds.forwardIntent` forwards every accepted seed to `MainActivity` as string extras) and maps `reduceMotion`, `freezeAtMs`, `profile == Screen` → `skipOverlay`, and the new `cf.holdSplash` (Task 9). Precedent to weigh: `RootOverrides` (`app/src/main`, `BuildConfig.DEBUG`-gated, parses only `cf.reduceMotion`) with `RootOverridesParityTest` (`testDebug`, against `TestSeed.parse`) and `RootOverridesReleaseTest` (`testRelease`). Either fold the new overrides into that pattern or use the debug source set as written; whichever you choose, keep one parity test against `TestSeed` and one release test, and keep `MainActivity`'s `reduceMotion` reading the same override.
- `LaunchTrace` (`android.os.Trace`, API 29+): `beginLaunch()`/`endLaunch()` → async section `launch`, cookie 1; `beginFirstCommit()`/`endFirstCommit()` → async section `first-commit`, cookie 2; sync section `cf.launch.moonDecode` around the bitmap decode. Each begin/end runs at most once per process.
- `LaunchOverlay(controller, modifier)`:
  - `Box(Modifier.fillMaxSize().onPlaced { controller.markOverlayPlaced() }.clearAndSetSemantics { contentDescription; paneTitle; role = Role.Image; testTag = "launch.opening" }.pointerInput(Unit) { consume all }.graphicsLayer { alpha/scale from sampler.container*(seconds); transformOrigin = TransformOrigin(0.5f, 0.4f) }.clipToBounds().drawWithCache { … })`.
  - `drawWithCache` cache block (runs on size or density change only): `LaunchSize` in dp, `LaunchTimeline.opening(size, reduceMotion)` + `LaunchFrameSampler`, `LaunchLayout`, the polyline `Path` in px + `PathMeasure` + a reusable segment `Path`, all brushes (§5.2), `Stroke(1.5 dp px, cap = Butt)`, the wordmark `Path` (`PathParser` once, then transformed by `density`), the moon top-left (§5.2). A size change mid-opening rebuilds these and keeps `t0`.
  - `onDrawBehind`: if `t0` is unset call `controller.onFirstDraw(...)`; `seconds = elapsed / 1e9`; `sampler.sampleInto(seconds, frame)`; draw §5.2 rows 0–8 in order, skipping a layer whose alpha is 0. **No allocation**: no `Path()`, `Brush.*`, `Stroke(...)`, `Paint()`, collections, string building or capturing lambdas inside `onDrawBehind`.
  - Moon bitmap: `ImageBitmap.imageResource(resources, R.drawable.launch_moon)` remembered per density and captured appearance, inside `cf.launch.moonDecode`; draw with `drawImage` (dst size = decoded size × scale).
  - Frame loop and freeze: §5.8, in a `LaunchedEffect(controller)`; Reduce Motion or freeze → no loop. When the loop stops, call `controller.onOpeningSettled()`.
  - `LaunchProbe` (`staticCompositionLocalOf<LaunchProbe?> { null }`) counts overlay compositions in a `SideEffect` (no-op when null).
- Tests (`feature/launch/src/test`, Robolectric `@GraphicsMode(NATIVE)`, `@Config(sdk = [36])`; build the controller with `nanoTime = { composeRule.mainClock.currentTime * 1_000_000 }` so test frame times share its base). `t0` is set only by a real draw: if, after the first `mainClock.advanceTimeByFrame()`, `controller.t0Nanos` is still `UNSET` (Robolectric did not draw), call `composeRule.onRoot().captureToImage()` once to force the first draw, and use the same helper in every test below; never set `t0` from the test:
  - `LaunchOverlayPlacementTest` (`@Layout`): after the first layout `overlayPlaced` is true and `holdsSystemSplash()` false; `t0` is set by the first draw; `claimOverlay` returns true once, then false; with `skip` it returns false and `holdsSystemSplash()` stays false (no deadlock on a second activity).
  - `LaunchOverlayRecompositionTest` (`@Layout`): with `mainClock.autoAdvance = false`, advance 1,600 ms in 8 ms steps; the probe counts exactly 1 composition; `openingDone` is true after 1,500 ms and no frame is requested afterwards (`mainClock` idles).
  - `LaunchOverlayRenderTest` (`@Layout`, light and dark, `pixel8`): with `freezeAt` 0, every pixel outside the moon's 288 dp square equals `skyTop` (± 1/255), and the pixel at moon centre + (0, −39 dp) (the top of the crescent stroke) is within 12/255 of `dawn`; at 1500 ms the bottom-centre pixel is within 3/255 of `skyHorizon`, and the stroke sample has moved to moon centre + (0, −28 − 39 × 0.86) dp; at 0/300/700/1000/1100/1400 ms the render completes without NaN geometry. Reduce Motion at 1500 ms equals the frame at 0.
  - `LaunchRadialPaintTest` (`@Layout`): port `LaunchRadialLayerTests` on the Android brushes at densities 2 and 3: halo alpha 0 at r 0 and 85, > 40/255 at r 100, smaller at r 200, horizontal = vertical ± 1; sun alpha 255 at r 60, 100–160 at r 80, < 40 at r 89.
  - `FlightPathStrokeTest` (`@Layout`): on the Android polyline path at density 3, `PathMeasure.getPosTan(strokeEnd × length)` lies within 0.02 dp of the sampled plane position every 10 ms from 500 to 1500 ms (port of `shapeLayerStrokeTipMatchesArcLengthEvery10Milliseconds`).
  - `LaunchOverlaySemanticsTest` (`@Layout`, en and nl): exactly one node with tag `launch.opening`, content description "ChronoFlow is opening" / "ChronoFlow wordt geopend" from resources, a pane title, `Role.Image`, no child semantics.

### Task 7: Activity order and root (`feat(app): wire the launch order and the overlay root`) [ticks "Activity order", "StrictMode"]

- Files: `…/app/{ChronoFlowApp.kt, AppGraph.kt, MainActivity.kt}` (existing) and `…/app/CfRoot.kt` (new: it takes over the content body now inline in `MainActivity.setContent`), `app/src/debug/kotlin/net/chronoflow/debug/DebugLaunchOverrides.kt`, release `LaunchOverrides.None` wiring.
- `ChronoFlowApp.onCreate` today: `if (BuildConfig.DEBUG) DebugStrictMode.install(this, launchPath)`, `super.onCreate()`, `graph = createGraph()` (`createGraph()` is `protected open`; `AppGraph(app, fontPreloader = ResourceFontPreloader)` has only `fontPreloader` and a lazy `planClock`). Change only: `LaunchTrace.beginLaunch()` and `AppGraph.launch` as a lazy property. **StrictMode is already done by 0a**: `DebugStrictMode` (`app/.../diagnostics/`) uses `detectAll().penaltyLog()` plus a `penaltyListener` that throws `LaunchPathViolation` for a disk or network violation with a `net.chronoflow.` frame while `LaunchPath.isActive` (`LaunchPathViolationPolicy`); `MainActivity` ends the launch path in its `registerFrameCommitCallback`, which stays as it is. Do not add `penaltyDeath`.
- **Test reset [R, OQ-3a-13]:** instrumented tests share one process (no Test Orchestrator, A19), so the once-per-process `claimOverlay` and the first-call-wins captures would make every launch after the first one overlay-free. `AppGraph` gets `@VisibleForTesting internal fun resetLaunchForTest()` that `check(BuildConfig.DEBUG)` and replaces `launch` with a fresh `LaunchController`; `LaunchTrace` guards are not re-armed (later launches emit no traces). Every `@Smoke` test that launches with profile `launch` calls it on the main thread (`InstrumentationRegistry.getInstrumentation().runOnMainSync { … }`) before `launch(...)`.
- `MainActivity.attachBaseContext` today: `super.attachBaseContext(newBase)`, then `applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(newBase))`. Add `launch.captureAppearance(newBase.resources.configuration)` (§5.6); `launch` comes from `(applicationContext as ChronoFlowApp).graph`, so check that `applicationContext` is usable at that point (use the `newBase.applicationContext` if not).
- `MainActivity.onCreate`, exactly in this order:
  1. `LaunchTrace.beginFirstCommit()` (first process launch only), `val splash = installSplashScreen()`, then `super.onCreate(savedInstanceState)`. The rest of today's `onCreate` stays in place around the new steps: the `ReduceMotion.resolve` read, `TripPageEntry.content(intent, graph)`, `PerfJank.tagger`/`PerfJank.install`, and the `registerFrameCommitCallback` that completes `firstCommit` and ends `launchPath`;
  2. `launch.onActivityCreated(restored = savedInstanceState != null)` — a stub that records the flag (3b implements A8);
  3. `launch.captureReduceMotion(...)`, `launch.claimOverlay(...)`;
  4. `enableEdgeToEdge(...)` and `window.isNavigationBarContrastEnforced = false` (§5.6);
  5. `setContent { CfRoot(graph) }`;
  6. `splash.setKeepOnScreenCondition { launch.holdsSystemSplash() }` (plan's `!launch.overlayPlaced`, extended [R] so an activity that did not claim the overlay — profile `screen`, a recreated or second activity — never holds the splash and never deadlocks);
  7. `splash.setOnExitAnimationListener { provider -> launch.systemIconBoundsPx = provider.iconView bounds on screen; provider.remove() }`.
- `CfRoot`: `ChronoFlowTheme` → the existing `CompositionLocalProvider` (`LocalSafeInsets`, `LayoutDirection.Ltr`, `LocalTimelineJankTagger`) → `Box(fillMaxSize)` that keeps `semantics { testTagsAsResourceId = true }` and `testTag("app.root")` on the **outer** box (`ScreenTest.launch` waits for exactly one `app.root` and the overlay's `launch.opening` tag is read as a resource id through it), with, in z-order, the app content and then `LaunchOverlay` while claimed. The content composes one frame later: `var contentReady by remember { mutableStateOf(!claimed) }; LaunchedEffect(Unit) { withFrameNanos { }; contentReady = true }`. While the overlay is present, the content wrapper carries `hideFromAccessibility()`.
- Fonts: Phase 2 already warms them: `setContent`'s `LaunchedEffect(resolver)` waits one frame and calls `warmFontsAfterFirstFrame(firstCommit, { graph.fontPreloader.preload(resolver, it) }, Dispatchers.IO)` (`FontWarmup.kt`, `FontPreloader.kt`). Move that effect into `CfRoot` unchanged; do not add a second call.
- Tests:
  - `CfRootTest` (Robolectric `@Layout`, `app/src/test`; `testDebug` is the source set that holds the app's debug-variant tests, e.g. `RootThemeTest`): on frame 1 only the overlay exists; on frame 2 the content exists, is hidden from accessibility (its semantics config has `HideFromAccessibility`), and a click on a content button under the overlay does not reach it; with `skipOverlay` the content composes on frame 1 and is visible to accessibility.
  - `LaunchStrictModeTest` (`@Smoke`, emulator, `app/src/androidTest`; `AppLaunchSmokeTest.launchesAsNativeDebugUnderStrictMode` already covers a seedless launch under the launch-path policy and must keep passing, now with the overlay in front. This test adds the seeded one; through `ScreenTest` with `CF_UI_TEST_PROFILE=launch`, after `resetLaunchForTest()`): a launch under the debug `penaltyDeath` policy reaches `openingDone` (the process is warm because instrumentation runs in it; the main-thread work from `attachBaseContext` to `openingDone` is the same as on a cold start) (wait with the 0e wait helper on `graph.launch.openingDone`, at most 5 s) and the process is alive; `launch.opening` exists. Under the functional scales 0 this runs in Reduce Motion, which is fine for this check.

### Task 7b: System-bar icons follow the theme (`feat(app): set the system bar icons from the theme`) [OQ-A67, owned by 3a]

- Plan OQ-A67: who sets system-bar icon appearance after the 3a hand-off? Default: **3a sets light theme → dark status-bar icons, dark theme → light icons, through `enableEdgeToEdge`; sky screens keep light icons** (`08` row 24). Today `MainActivity` never calls `enableEdgeToEdge` and `Theme.ChronoFlow` sets no light-bar flag, so the emulator shows white status-bar icons on the light background. Add a plan.md 3a checkbox "System-bar icons (OQ-A67)" in this commit and mark OQ-A67 resolved in Task 11.
- Files: `…/app/SystemBarStyles.kt` (pure, no `android.*`: `fun statusBar(night: Boolean, overlayShowing: Boolean, onSky: Boolean): IconAppearance` and `navigationBar(night)`, `IconAppearance` = light or dark icons, mapped to `SystemBarStyle.dark(TRANSPARENT)` (light icons) / `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` (dark icons) next to the `enableEdgeToEdge` call), `MainActivity.kt`, `CfRoot.kt`, `LaunchController.kt` (`overlayShowing`, true while the overlay is drawn; 3b flips it at the hand-off).
- Rules: while the overlay is showing or `onSky` is true, the status-bar icons are light (dark sky); otherwise they are dark in light theme and light in dark theme. The navigation bar follows OQ-3a-3 (light theme → dark icons, dark → light). `night` is the *current* theme (the same one `ChronoFlowTheme` uses), not the captured launch appearance. `uiMode` is in `configChanges`, so re-apply the styles from `onConfigurationChanged` and when `overlayShowing` changes (`enableEdgeToEdge` is idempotent); never from a draw lambda. Welcome and the auth screens (3c, 5a) pass `onSky = true`.
- Tests: `SystemBarStylesTest` (plain JVM, no tier annotation): the whole truth table (light/dark × overlay/none × onSky, status and navigation). One state test in the cheapest tier that can read it: `WindowCompat.getInsetsController(window, decorView).isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars` after launch with profile `screen` (so no overlay) in light and in dark: Robolectric `@Layout` through `CfRootTest` if it reads the flags, else a `ScreenTest` `@Smoke` method under the 10 s budget. The icon *colour* on screen can only be shown by pixels: add one `make captures DEVICE=1` frame of the status-bar region per theme and name it in the review notes (no pixel assertion).

### Task 8b: Konsist const gap (`test(konsist): catch numeric constants used as dp or sp in feature code`)

- Gap from the Phase 2 review: `featureDpLiteralsComeFromBoardMetrics` (`konsist/src/test/kotlin/net/chronoflow/konsist/DesignSystemRules.kt`, the `literal` regex at the top of the function) matches only `<number>.dp` / `<number>.sp`. `private const val X = 1f` followed by `X.dp` passes. Real case: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/timeline/TimelinePresentedMarker.kt` (`PRESENTED_MARKER_SIZE_DP = 1f` at the bottom, `PRESENTED_MARKER_SIZE_DP.dp` in the `Box`).
- Extend the rule, for the same files (`feature/*` and `core/ui`, main source set, no `/generated/`): collect every `val` / `const val` whose initializer is a numeric literal (`Int`, `Float`, `Double`, with an optional type, `f`/`F` suffix and `_`) in a file, in any scope (top level, `object`, `companion object`, private or not), outside `@BoardMetrics` objects, and report any use of that name as `NAME.dp` or `NAME.sp` (also `NAME.toFloat().dp`, `NAME.toInt().dp`). Names declared inside a `@BoardMetrics` object stay allowed (the existing code already blanks those objects out of the text first). A value of zero is allowed, as for literals.
- Tests, in the existing three-test shape: extend the planted-violation fixture `konsist/src/test/resources/violations/featureDpLiteralsComeFromBoardMetrics/feature/trip/src/main/kotlin/net/chronoflow/` with `private const val SIZE = 1f` and `SIZE.dp` (and a `companion object` variant), and the control `…/allowed/…/Example.kt` with the same constant inside an `@BoardMetrics` object. `…RejectsPlantedViolation` must fail for the new fixture alone (add it as its own file so the older literal fixture cannot satisfy the assertion).
- **Marker decision [R, OQ-3a-14]: rewrite it, with no exemption.** `TimelinePresentedMarker` is a benchmark hook with no board, so `@BoardMetrics` (which needs a real `_design/canvas/<Name>.dc.html`) is not available to it, and an exemption list would leave a hole in the rule. Draw the 1 dp node as 1 px computed in layout (`Modifier.layout { measurable, constraints -> val p = measurable.measure(constraints.copy(minWidth = 1, minHeight = 1, maxWidth = 1, maxHeight = 1)); layout(1, 1) { p.place(0, 0) } }`, no dp literal and no constant), keeping the tags `trip.timeline.opening` / `trip.timeline.presented`; UiAutomator only needs non-empty bounds. This touches the timeline, so run `make perf-timeline` once (device lock) and confirm `TimelineBenchmark` still finds the marker; record the run in `docs/perf-timeline.md`. **Fallback if the benchmark cannot see a 1 px node:** keep 1 dp and add one narrow, documented exemption to the rule (a constant `EXEMPT` set of exact file paths with a comment naming this marker and why, plus a test that every entry still exists), and record that in OQ-3a-14.
- Run `make targeted ONLY="konsist:KonsistRulesTest"`, then `make lint` once.

### Task 8: Geometry guards and captures (`test(launch): guard launch geometry on every profile`) [ticks "Guards"]

- `LaunchDeviceGeometryTests` (JVM, parameterized: one test case per profile, AGENTS "Tests" 5) — port `DeviceGeometryTests.sampledInkIsContainedAndCompositionKeepsBoardClearances` (geometry part, `DeviceGeometryTests.swift:38–87`) over every `profiles.json` profile (with its insets and with `(7, 19, 11, 31)`) plus 280 × 653, at 0, 300, 700, 1000, 1100, 1400 and 1500 ms (opening, and Reduce Motion at 0):
  - moon glyph rect (`104 × moonScale`, centred on `moon.position`, transformed by container scale about `(w/2, 0.4h)`) inside `safeBounds` ± 0.5;
  - wordmark box (`LaunchWordmark.WIDTH_DP × 40` at `wordmarkCentre + translationY`) inside `safeBounds` when its opacity > 0;
  - sun rect (180 × 180) satisfies `LaunchSnapshotGeometry.containsSun` (sides inside the horizontal safe bounds ± 0.5, top inside the window, ≥ 1/3 of its height visible) when its opacity > 0, **only for windows at least 667 dp tall** (the shortest iOS `SnapshotDevice`, `se` 375 × 667; every iOS device in `DeviceGeometryTests` is portrait). Shorter windows (`landscapeSideNav` 914 × 411, which on a portrait-locked phone only occurs in multi-window or freeform) keep the iOS geometry unchanged: the sun may sit mostly below the window there, and the test asserts the moon and wordmark rules plus finite values. Record this as an OQ-A row ("Launch sun on windows shorter than 667 dp"; default: keep iOS geometry, no sun-visibility rule below 667 dp; owner 3a).;
  - `status.minY − apex = 5.5 ± 0.001`, `status.maxY < sunCentre.y − 90`, `wordmarkCentre.y + 20 < apex`.
  - A robustness case (360 × 300 split screen) only asserts finite values and no exception.
- `LaunchStatusFitTest` (Robolectric `@Layout`, one test per profile; port of the status loop in `DeviceGeometryTests.swift:88–104`): Figtree 400 (`figtree_400.ttf`) at 14 sp × font scale 1.0 and 1.3 (iOS uses 1.0 and 1.5; Android caps at 1.3, OQ-3a-11), line height `20 × fontScale`, en and nl strings from resources, measured with `TextPaint` (`measureText`, `fontMetrics.ascent`/`descent`); `StatusLineLayout(width = w, textWidth, ascent, descent, lineHeight, scale = density).ink` offset by `(0, layout.status.minY)` lies inside `layout.safeBounds` (Typography `status` = Figtree 400, body 14/20, `I/…/DesignSystem/Typography.swift:98`, `TypeMetrics.generated.swift:7`; board `status:` 14px/20px). 3b draws the line; this proves the geometry now.
- `LaunchOverlayGuardTest` (Robolectric `@Layout`, parameterized: one test per profile × theme): render at 0, 300, 1000, 1500 ms; the frame-0 pixel rule of Task 6 holds at every profile; `largeFont` renders identically to `pixel8` (the overlay ignores font scale).
- `LaunchCaptureTest` (`@Capture`): `make captures ONLY=SplashScreen` writes `build/captures/SplashScreen_ready-<ms>_<profile>_en_<theme>.png` for ms 0000, 0300, 0700, 1000, 1100, 1400, 1500 and `SplashScreen_reduceMotion-0000_…`, at `pixel8` and `small`, light and dark, plus one `nl` frame at 0 to show the overlay has no text.

### Task 9: Frame-0 parity on the emulator (`test(app): prove launch frame zero matches the system splash`) [ticks "Frame-0 parity"]

- Debug hook (add it to plan.md "Android spec: seeds and test hooks" and OQ-3a-4): the seed extra `cf.holdSplash` (`"1"`/`"true"` → true, else absent; a `cf.*` key, so ungated). Add `holdSplash: Boolean?` to 0e's `TestSeed` (`app/src/debug/.../TestSeed.kt`; parse rule as `reduceMotion`) with a `TestSeedTest` case (`app/src/testDebug/kotlin/net/chronoflow/debug/TestSeedTest.kt`), and a `RootOverridesParityTest`-style case if the override lives in `RootOverrides`; `DebugLaunchOverrides` maps it to `holdSplash`. Release is in-process: the test sets `graph.launch.splashHeld = false` inside `runOnMainSync` (instrumented tests run in the app process, so no exported receiver is needed). `ReleaseReadinessTest` (`app/src/testRelease`) already proves `TestSeed` is absent from release.
- `LaunchFrameZeroTest` (`@Smoke`, emulator, extends 0e's `ScreenTest`, one method per appearance: `frameZeroMatchesSystemSplashLight`, `…Dark`):
  1. `resetLaunchForTest()` (Task 7), then `launch(mapOf("CF_UI_TEST_PROFILE" to "launch", "cf.settings.theme" to "light"|"dark", "cf.holdSplash" to "true", "cf.freezeAt" to "0"))`. The theme seed goes through `UiModeManager.setApplicationNightMode` (0e T11), which the system splash also follows on API 31+; the next `ScreenTest` launch resets it to `system`. This is a warm start of a new task (the instrumentation keeps the process alive), and Android shows the system splash on warm starts; the start is not from the launcher, so the icon depends on `icon_preferred` (Task 5).
  2. `waitFor(5_000) { graph.launch.overlayPlaced && graph.launch.holdsSystemSplash() }` (the overlay has laid out but cannot draw, so the system splash is what is on screen; window animations are at scale 0); screenshot A with `UiAutomation.takeScreenshot()`.
  3. `runOnMainSync { graph.launch.splashHeld = false }`; `device.wait(Until.hasObject(By.res("launch.opening")), 5_000)` and `waitFor { graph.launch.t0Nanos != UNSET && graph.launch.systemIconBoundsPx != null }`; screenshot B.
  4. Assert: `systemIconBoundsPx` and the stored frame-0 moon rect agree within 1 px on every edge; their centres equal `(displayW/2, displayH/2)` within 1 px; the centre pixel of A and B differs by ≤ 2/255 per channel; inside the 192 dp circle ≥ 99% of pixels differ by ≤ 4/255 and none by more than 16/255; a 24 dp band along the left and right edges at mid-height is `skyTop` ± 1 in both.
  - `ScreenTest.launch` waits up to 5 s for exactly one `app.root` through the compose rule, and `closeScenarioAndResetConfiguration` (`@After`) resets language and theme to `system`. If `launch` cannot reach `app.root` while the splash is held (draws are cancelled), or the exit listener never fires (no system splash was shown for this start), stop and report the observed behaviour; do not weaken the assertions.
  - Under the functional scale 0 the overlay is in Reduce Motion; its frame 0 is the same image, so the check stays valid.
  - If the system icon is not 288 dp (some API draws it at another size), stop and report the measured size; do not rescale to make the test pass.
- Each method < 10 s. API 29–30 (compat splash) runs only on the `api29` T2 AVD on request (OQ-3a-10).

### Task 10: Benchmarks and budgets (`perf(launch): measure first commit and launch jank`) [ticks "Traces and benchmarks"]

- `StartupBenchmark` (`benchmark/src/main/kotlin/net/chronoflow/benchmark/StartupBenchmark.kt`): the one test `startupCold`, 10 iterations, `CompilationMode.Partial(BaselineProfileMode.Require)`, `StartupMode.COLD`, `setupBlock = { pressHome() }`, package `net.chronoflow.app.native` unless `targetAppId` is passed. Add `TraceSectionMetric("first-commit", Mode.First)` to its metrics (today only `StartupTimingMetric()`). No seed extras (the perf variant has no seed receiver): the real signed-out launch. Compare the new cold TTID with the OQ-A66 reference (95.0 ms, Phase 2 95.8 ms, `Partial(Require)`, `docs/perf-startup.md` "Phase 2 comparison"): the overlay is expected to raise it; record the delta, do not gate on it.
- `LaunchJankBenchmark` (new, same package, `@Perf`): cold start, `FrameTimingMetric()`; inside `measureRepeated` after `startActivityAndWait()`, capture until `device.wait(Until.gone(By.res("launch.opening")), 3_500)` returns (in 3a the overlay stays, so this is the 3.5 s capture; from 3b it ends at the hand-off). 10 iterations.
- If `TraceSectionMetric` reports no value for the async `first-commit` slice, replace it with a `TraceMetric` subclass that queries the async slice on the app's process track, and record the switch in `docs/perf-startup.md`.
- `benchmark/budgets.json` rows (use the exact metric keys the JSON output shows):
  - cold `timeToInitialDisplayMs` median: reference hard 400; low-end 800 (proposal, OQ-A20); emulator report;
  - `first-commit` median: reference 50; low-end 100; emulator report;
  - `LaunchJankBenchmark` `frameOverrunMs` P99: reference 0; low-end 4; emulator report; jank % ≤ 5 (frame pacing ≥ 95%) report;
  - `LaunchJankBenchmark` `frameCount` **minimum** 75 per iteration on the emulator (functional: proves the overlay animated at scale 1). `Scripts/check-benchmarks.py` already judges a `min` gate (`judge`, used by `timelineBuildCount`), but only two suites exist (`CLASS_SUFFIX = {"startup": ".StartupBenchmark", "timeline": ".TimelineBenchmark"}`, `RAW_KEYS`, one history table per suite). Add the launch rows and the new class (a `launch` suite, or a second suffix for `startup`), and add a failing fixture under `Scripts/fixtures/benchmarks/` plus a case in `Scripts/tests/test_check_benchmarks.py`. `Scripts/perf-startup.sh` passes only `class=net.chronoflow.benchmark.StartupBenchmark` to Gradle and calls the checker with `--suite startup`: make `make perf` run both classes (comma-separated `class=`) and check both. `docs/perf-startup.md` has a fixed "Runs" header (TTID only): extend it or add a second table for the new metrics.
- `make baseline-profile` exists (`benchmark/src/main/kotlin/net/chronoflow/benchmark/BaselineProfileGenerator.kt`; Phase 2 ran it in about 8 minutes with `TIER_IDLE_TIMEOUT=1200`): add the cold launch + 1.6 s of overlay to the startup-profile journey and regenerate; commit the regenerated profiles.
- **App Startup initializers** (its own commit, `perf(app): keep only the lifecycle and profile installer initializers`; A21, plan.md OQ-A47, 0a OQ-0a-10): read 0a's inventory in `docs/tooling.md` and the merged release manifest. Keep `androidx.lifecycle.ProcessLifecycleInitializer` and `androidx.profileinstaller.ProfileInstallerInitializer`; remove every other `androidx.startup` `<meta-data>` entry (for example `androidx.emoji2.text.EmojiCompatInitializer`, merged by Compose text) with `<provider android:name="androidx.startup.InitializationProvider" android:authorities="${applicationId}.androidx-startup" tools:node="merge"><meta-data android:name="<class>" tools:node="remove"/></provider>` in `app/src/main/AndroidManifest.xml`. Add `STARTUP_INITIALIZER` to the `Rule` enum and `ManifestRules.check` in `build-logic/convention/src/main/kotlin/net/chronoflow/buildlogic/manifest/ManifestRules.kt` (a constant allowlist of those two class names; the `check(manifestXml, applicationId, allowedExportedComponents)` signature stays; tests in `VerifyReleaseManifestTest.kt`): it fails when a `<meta-data android:value="androidx.startup">` inside `androidx.startup.InitializationProvider` names another class. Add the planted `startup-initializer.xml` (an `EmojiCompatInitializer` entry) next to the other fixtures in `build-logic/convention/src/test/resources/manifests/` and the two allowed entries to `valid.xml`. `docs/tooling.md` §7 ("Merged manifest inventory") lists the three merged entries today. Record each kept initializer's cost from one `startupCold` trace in `docs/perf-startup.md` [verify the slice names App Startup emits].
- Run `make perf`; append rows to `docs/perf-startup.md` (date, commit, device `pixel` AVD API 36, refresh rate, build type, raw metrics). Emulator numbers are not device claims.

### Task 11: Hand-over evidence and docs (`docs(launch): record phase 3a launch results`)

- With the animator scales at 1, record a cold launch in light and in dark: `adb shell screenrecord --bit-rate 20000000 /sdcard/launch-<theme>.mp4`, pull to `build/launch/`, and if `ffmpeg` is available extract frames (`build/launch/<theme>/%04d.png`). Review the frames around the splash → overlay switch for any flash or jump. Do not commit the video; record the result and the frame numbers checked in `docs/perf-startup.md`.
- Add OQ-3a-1 … OQ-3a-14 below to plan.md "Open questions for the user" with the next free OQ-A numbers at the time of the commit (OQ-A44–A67 are taken at 2b7114f, so the next is OQ-A68; check the table first and never reuse a number); keep the defaults. Update the OQ-A67 row to say Task 7b resolved it. Add `cf.holdSplash` to "Android spec: seeds and test hooks" (intent extras list).

## 8. Acceptance criteria

- `make quick` passes (< 60 s warm), including `Scripts/tests/test_wordmark.py`, the Konsist purity rule for `feature.launch.model` and the extended `featureDpLiteralsComeFromBoardMetrics` with its new planted-violation test. `LaunchPaintGoldenTest` (0b) still passes.
- `make generators-check` passes; a hand edit to `LaunchWordmark.generated.kt` makes it fail (prove once, revert).
- `make targeted ONLY="feature:launch:CubicBezierTests feature:launch:LaunchClockTests feature:launch:LaunchLayoutTests feature:launch:FlightPathTests feature:launch:StatusLineLayoutTests feature:launch:LaunchTimelineTests feature:launch:LaunchTimelineTrackTests feature:launch:LaunchFrameSamplerTests feature:launch:LaunchWordmarkTests feature:launch:LaunchDeviceGeometryTests"` passes (use the module syntax the 0a Makefile expects).
- `make check` passes with `pixel` booted, including `LaunchFrameZeroTest` (light and dark) and `LaunchStrictModeTest` in `@Smoke`, and every Robolectric test above within 5 s each (`build/test-results/*/timings.json`).
- `make captures ONLY=SplashScreen` produced the PNGs of Task 8; the reviewer compared 0000/0700/1400 with `SplashSpec` key frames 0/700/1400, and every difference is either one of §5.1 or fixed.
- `make perf` produced `first-commit`, cold TTID, `frameOverrunMs` and `frameCount` for both benchmarks; `check-benchmarks.py` passes; the rows are in `docs/perf-startup.md`. Functional gates on the emulator: first animated change at 300 ms + ≤ 1 frame (JVM test), StrictMode launch survives, `frameCount` minimum met. Device gates stay "open: device only" (OQ-A22).
- The screen recording review is recorded and shows no flash.
- `verifyReleaseManifest` passes with `STARTUP_INITIALIZER`, and the merged release manifest lists only the two allowed initializers.
- Light theme shows dark status-bar and navigation-bar icons and dark theme light ones whenever no overlay is shown, and `SystemBarStylesTest` covers the table (OQ-A67, Task 7b).
- `TimelinePresentedMarker` no longer uses a numeric constant as dp, and the extended rule fails on a planted constant (Task 8b).
- Every `plan.md` 3a checkbox, including the two this brief adds (Tasks 7b and 8b), is ticked in its commit; `git log` shows atomic Conventional Commits without trailers.

## 9. Review checklist (Opus reviewer)

Ordering and deadlocks
- [ ] `installSplashScreen()` is called before `super.onCreate`; the steps in Task 7 are in that order.
- [ ] `overlayPlaced` is written only from `Modifier.onPlaced`; nothing in a draw lambda feeds the keep condition.
- [ ] `holdsSystemSplash()` is false when this activity did not claim the overlay (second activity, profile `screen`, Reduce Motion is irrelevant here). Try: rotate, change font size (recreates), relaunch from recents — the app never sticks on the splash.
- [ ] `setOnExitAnimationListener` always calls `remove()`.
- [ ] The frame loop treats an unset `t0` as elapsed 0 (no `frameNanos − Long.MIN_VALUE`).
- [ ] Launch-profile `@Smoke` tests call `resetLaunchForTest()` first; it is `check(BuildConfig.DEBUG)`-guarded; no exported debug receiver was added.

Frame 0 and fidelity
- [ ] The overlay draws the same `R.drawable.launch_moon` the theme uses, at its decoded size, at integer `(wPx − bmpW)/2`, `(hPx − bmpH)/2`; no `ContentScale`, no `painterResource` scaling.
- [ ] The overlay fills the whole window including the cutout (`windowLayoutInDisplayCutoutMode`) and ignores insets.
- [ ] Colors come from the captured appearance via generated tokens; no hex literals, no `MaterialTheme.colorScheme`, no `isSystemInDarkTheme()` in the overlay.
- [ ] No stars; moon centred at `cy`; wordmark at `cy + 40`; band at `cy + 48`; path control points use `90·w/390` and `300·w/390`; sun α0 stops use dawn α0.
- [ ] The drawn path is the 128-point polyline, trimmed with `PathMeasure.getSegment`, not the raw cubic and not a dash effect.
- [ ] Plane glow and dot are one pre-composited radial draw.

Performance
- [ ] `onDrawBehind` allocates nothing (look for `Path()`, `Brush.`, `Stroke(`, `Paint()`, `listOf`, `map {`, string templates, `Offset` inside collections, capturing lambdas); everything is built in the cache block.
- [ ] `elapsed` is read only in `onDrawBehind` and the `graphicsLayer {}` block; the recomposition test proves 1 composition over 1.6 s.
- [ ] No `animate*`, `Animatable`, `InfiniteTransition`, `delay` or `Thread.sleep` in launch code; the frame loop stops after 1.5 s and never runs with Reduce Motion or `freezeAt`.
- [ ] No disk, DataStore, Keystore or network on the main thread before `first-commit`; the only decode is the moon bitmap inside `cf.launch.moonDecode`; StrictMode `penaltyDeath` test passes.
- [ ] `sampleIntoAllocatesNothing` really measures (planted `listOf()` in `sampleInto` makes it fail — ask the worker for this proof).
- [ ] Trace sections are async with fixed cookies and each pair fires once per process.

Ports
- [ ] Swift names kept (`CubicBezier`, `LaunchClock`, `MediaLaunchClock`, `ManualLaunchClock`, `LaunchLayout`, `FlightPath`, `StatusLineLayout`, `LaunchTimeline`, `LayerID`, `TrackValue`, `LayerValues`, `MoonTarget.Insets`, `LaunchPaint`); ported tests keep the Swift test names.
- [ ] `CubicBezier.value` is the iOS algorithm (Newton then bisection, same iteration counts and epsilons), not Compose `CubicBezierEasing`.
- [ ] `FlightPath` uses 1024 table entries and 128 position keys including 0.03; `fraction(at:)` piecewise exactly.
- [ ] Reduce Motion opening: 18 tracks, all `[0]`, sun/halo translation 0.
- [ ] `LaunchPaint` is the 0b object, consumed unchanged (no second copy); the wordmark constants match §Task 4.

Accessibility and copy
- [ ] One node "ChronoFlow is opening" / "ChronoFlow wordt geopend" from `R.string.launch_chronoflow_is_opening`, with `paneTitle` and `Role.Image`; content hidden with `hideFromAccessibility()`; pointer events consumed; no haptics.
- [ ] No literal strings in UI code; no new copy (none is needed in 3a).

Tests and process
- [ ] Each test sits at the cheapest tier: model logic JVM, drawing and theme Robolectric, only splash parity and StrictMode on the emulator.
- [ ] Emulator tests launch seeded, use exact tags and bounded waits, < 10 s each; no `Thread.sleep`.
- [ ] Perf numbers are recorded with device and build type; no emulator number is presented as a device claim.
- [ ] Generated files have headers and manifest entries; `generators-check` never calls `hb-view` or `rsvg-convert`.

System bars and Konsist
- [ ] `SystemBarStyles` is pure and tested; the styles are re-applied on `uiMode` change and when `overlayShowing` flips, never from a draw lambda; icons are dark in light theme and light in dark theme once no overlay is shown.
- [ ] The new Konsist rule fails on a planted `const val X = 1f` + `X.dp`, passes inside `@BoardMetrics`, and no exemption list was added unless OQ-3a-14's fallback was needed.

Typical mistakes to look for: drawing the moon from a `Painter` (scales to the layout size); using `onGloballyPositioned`/`onSizeChanged` + state for sizes; `Brush.radialGradient(center = absolute)` rebuilt per frame; `Color.Transparent` gradient stops (dark fringe); `SystemBarStyle.auto` for the status bar; reading `LocalReduceMotion` (theme, re-read on resume) instead of the captured value; ending `first-commit` in `onPlaced` instead of the first draw; `t0` from `withFrameNanos` of the second frame instead of the first draw.

## 10. Open questions (proceed on the default; add each to plan.md with the next free OQ-A number, Task 11)

| # | Question | Default |
|---|---|---|
| OQ-3a-1 | What does the app show after the opening until 3b lands? | The overlay holds the 1500 ms frame, stops its frame loop, hides and shields the content. Work on the app with `CF_UI_TEST_PROFILE=screen`. |
| OQ-3a-2 | Activity recreated within the process during or after the splash (locale or font-scale change) in 3a. | No overlay and no keep condition on the new activity (D14 minimal); 3b adds the 300 ms fade and decides whether a recreated activity resumes a running overlay at its wall-clock position. |
| OQ-3a-3 | Navigation-bar icon color on the sky (`05` OQ A9). | From the sky's bottom per captured appearance: light → dark icons (`SystemBarStyle.light`), dark → light icons; contrast enforcement off; verify on the Task 11 recording. |
| OQ-3a-4 | Android-only debug hook `cf.holdSplash` for the parity screenshot. | Add it as a `TestSeed` field, debug only, listed in "Android spec: seeds and test hooks"; the in-process test releases it through `LaunchController.splashHeld`, so no exported receiver exists; release has no `TestSeed` (`ReleaseReadinessTest`). |
| OQ-3a-5 | Wordmark vector source. | Generated once from the Fraunces 400 opsz32 TTF with HarfBuzz `hb-view` 15.1.0 (kerning) plus CoreText-style tracking; committed with a hash manifest. Fallback if `hb-view` output proves unusable: build the path at runtime off the main thread with `Paint.getTextPath` before 1000 ms. |
| OQ-3a-6 | Parity tolerances. | Rect ±1 px; centre pixel ≤ 2/255; ≥ 99% of pixels in the 192 dp circle ≤ 4/255, none > 16/255. |
| OQ-3a-7 | `TraceSectionMetric` and async sections. | Async `first-commit`; fallback a `TraceMetric` query, recorded. |
| OQ-3a-8 | Moon decode on the main thread during the first composition. | Keep it in composition, traced as `cf.launch.moonDecode`. If it takes > 16 ms on the `pixel` emulator, start the decode on `Dispatchers.IO` at the top of `onCreate` and join it in the first composition; record the numbers. |
| OQ-3a-9 | Sky banding in dark. | Compose gradient draw; if the Task 11 frames show bands, draw the sky through `drawIntoCanvas` with a native `Paint` that has `isDither = true` and the same shader. |
| OQ-3a-10 | API 29–30 parity (compat splash). | Not in `make check`; run `LaunchFrameZeroTest` on the `api29` T2 AVD on request; accept ± a few px until then. |
| OQ-3a-11 | Does the status text scale with font scale (affects `LaunchStatusFitTest`)? | Yes, in sp up to the 1.3 cap, like iOS Dynamic Type up to xxLarge; 3b confirms when it draws the line. |
| OQ-3a-12 | Theme names and files: plan says `Theme.App.Starting` / `Theme.App` in `values{,-night}/themes.xml`. | The tree's `Theme.ChronoFlow` stays; add `Theme.ChronoFlow.Starting` in `values/themes.xml` (+ `-v33`); the existing `values-night/themes.xml` repeats the `Theme.ChronoFlow` items (a style in `values-night` replaces the one in `values`); night colors and the night moon come from the `-night` color and drawable resources. |
| OQ-3a-13 | Instrumented tests share one process, but the overlay is once per process. | Debug-checked `AppGraph.resetLaunchForTest()` gives each launch-profile `@Smoke` test a fresh `LaunchController`; traces are not re-armed. Later phases may reuse the hook. |
| OQ-3a-14 | `TimelinePresentedMarker` uses `PRESENTED_MARKER_SIZE_DP.dp`, which the extended dp rule (Task 8b) would flag. | Rewrite it as a 1 px layout node, no exemption; `make perf-timeline` once to prove the benchmark still finds it. Fallback: one documented file exemption in the rule. |
