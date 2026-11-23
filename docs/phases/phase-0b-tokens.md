# Phase 0b brief: Token generator, fonts and icons

- **Branch:** `feat/phase-0b-tokens`, from the stack top once 0a is in the stack. It runs in parallel with 0c, 0d and 0e (wave 2) in its own worktree, so keep edits to shared files (`Makefile`, `lint.xml`, `plan.md`) small and local (see §9).
- **Depends on:** 0a (module skeletons, convention plugins, Makefile placeholders, `tools/ios-pin.txt`). **Wave:** 2. **Milestone:** M0.
- **Plan entry:** `plan.md` "### Phase 0b: Token generator, fonts and icons" (l.764–823). This brief expands that entry. Where they differ, `plan.md` wins: report the difference. The few places where this brief deliberately refines the entry are listed in §11 as open questions, each with its default.
- **Boards:** `AppIcon`, `SplashSpec` (`_design/canvas/<Name>.dc.html`). This phase adds **no UI, no screen and no copy**.
- **API endpoints:** none.
- **Tests rules:** AGENTS.md "Tests" applies to every test you write. Read it before task 1.
- **Sources are read-only.** Read iOS only with `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. Never write, build or check out anything in `chronoflow-ios`, `chronoflow-new` or `chronoflow-mobile`.

Tags: **[F]** is a fact verified in source for this brief (path given). **[R]** is a decision of the plan or of this brief.

---

## 1. Goal

Make every color, ramp, metric, font, icon and launch image that later phases need into a **generated, committed and checked** artifact, reproduced from the same inputs and with the same math as iOS. When this phase ends:

1. `:tools:tokengen` is a Kotlin JVM port of iOS `Tools/TokenGen` (plus the type-metric part of `Tools/generate_fonts.py`). It converts the OKLCH tokens to **sRGB** with the exact iOS math and writes the committed outputs in §5.
2. The 84 base hex values (42 tokens × light/dark) and the 16 derived hex values equal iOS `Tools/TokenGen/TokenGenReport.md` exactly. The sky ramp is within ±0.25/255 at 1,001 points. Every metric and type value equals the iOS generated Swift files at the pin. Output is byte-deterministic, and `verifyTokens` runs in `make generators-check`.
3. The ten Figtree/Fraunces TTFs sit in `core/designsystem/src/main/res/font/` byte-identical to iOS, with a hash manifest and a test.
4. The adaptive launcher icon (background, foreground, monochrome) and the notification icon `ic_stat_moon` are vector drawables converted unchanged from `_design/canvas/icon/`.
5. `launch_moon.png` (light and night, 1152 × 1152 px = 288 dp at xxxhdpi) is rasterized once from the iOS moon SVGs and pinned in a hash manifest.
6. `make tokens`, `make fonts`, `make assets` and their checks work, and `make generators-check` covers all three.

---

## 2. Facts verified for this brief [F]

These were checked on 2026-11-16 against the pin and this repo's `_design`. Your tests must reproduce them; if one fails, it is a defect in the port, not a reason to change the golden.

| # | Fact | How it was checked |
|---|---|---|
| F1 | The iOS algorithm (`ColorMath.swift`) with sRGB output reproduces **84/84** base hex values of the iOS report, 0 mismatches. | Independent Python port in the scratchpad |
| F2 | The report has **50 rows** = 42 base tokens + 6 auth/settings + 2 wizard derived tokens, i.e. 100 hex values. | `Tools/TokenGen/TokenGenReport.md` at the pin |
| F3 | The **sRGB** sky ramp (tolerance 0.25/255, 1,001 points) has **33 stops per theme**, divisions = 16; the exact lists are in §6.3. iOS's own ramp is in **P3** and has 17 stops: do not copy that count. | Same port; `08 §6` lists the same stops |
| F4 | `systemSelected` = OKLab mix(card, dawn, 0.16) = light `#FAECE3`, dark `#302D38`. | Same port |
| F5 | `tokens.json`, `theme.css`, `components/bundle.{js,css}` and every board the generator reads are **byte-identical** to iOS `_design` at the pin, except `PasswordPrompt`, `Set2FAOn` and `SetPreferences`. For those three, every regex the generator applies returns the **same values** as on iOS. | SHA-256 per file; regexes run on both copies |
| F6 | `theme.css`: first `scale(…)` = `0.97`, first `opacity: …; }` = `0.85`, `transition: transform 120ms ease`, both reduce-motion rules present, `--sky-mid-stop` = `55%`, `60%`. `SplashScreen.dc.html` contains the three iOS guard strings. | Regex on `_design/canvas` |
| F7 | **`rsvg-convert` 2.63.4 renders the iOS moon SVGs as a fully transparent PNG**, because it does not support `stroke="color(display-p3 …)"`. With the stroke replaced by the sRGB hex (`#E78A45` light, `#F2AD73` dark), the output is correct and deterministic: pixel (576, 420) = `E78A45FF` / `F2AD73FF`, corner (0,0) alpha 0, ink bbox x 327–820 / y 331–822, max ink distance from the centre (576, 576) ≈ 251.9 px (< 384 px, the 192 dp circle); ink covers ≈ 13.3 % of the pixels. Output SHA-256: light `a86464c3d31dd6fcca240f95dabbf1db6d9e6f645bf7892b70a0c44838d20ebf`, dark `b7f1dc110035026c97b8b8d66051178e1c55a27aa11f564d9903c441dfca8f50`. | Rendered in the scratchpad, twice |
| F8 | Font bytes at the pin match iOS `Fonts/manifest.json` (hashes in §7, task 6). | `shasum -a 256` over `git show` |
| F9 | Alpha bytes: `round(α·255)`: 0.9 → 230 (`E6`), 0.35 → 89 (`59`), 0.25 → 64 (`40`), 0.6 → 153 (`99`), 0.4 → 102 (`66`), 0.12 → 31 (`1F`). | Python `floor(x+0.5)` |

Where the research and the plan disagree, this brief follows the plan: package `net.chronoflow.core.designsystem.generated` (A2), not `net.chronoflow.designsystem.generated` (`08 §3.4`); font files `figtree_400.ttf` (plan), not `cf_figtree_400.ttf` (`08 §5.2`); color `launch_sky_top` (plan Phase 3a l.1324), not `launch_sky` (`08 §3.1`); `drawable-xxxhdpi/launch_moon.png` (plan), not `drawable-nodpi/splash_moon.png` (`08 §11.4`); `mipmap-anydpi` (plan, minSdk 29), not `mipmap-anydpi-v26`. The iOS report text says "±1/255", but iOS code (`ColorMath.swift:101`) uses **0.25/255**: follow the code.

---

## 3. Inputs to read

**This repo**
- `plan.md`: "What we reuse from iOS" rows R7–R9 (l.50–52); Decisions A2, A3, A9, A20, A21; OQ-A7, OQ-A9, OQ-A10, OQ-A43; the Phase 0b entry (l.764–823); Phase 2 (l.1227) and Phase 3a (l.1291) to see who consumes the outputs; the Phase 1c entry (l.1090), which reads `TimelinePalette`.
- `AGENTS.md`: "Tests", "Commands", "Git", "Traps". `CLAUDE.md`: orchestration and sandbox notes.
- `docs/research/08-design-system.md` §2–§6, §9, §11 (algorithm §3.2, token table §4.1, derived colors §4.2, fonts §5.1–5.3, sky §6, icon §11.2, notification icon §11.3, splash §11.4).
- `docs/research/01-ios-plan-reuse.md` §3 and Appendix A "Tokens" and "Typography".
- Boards `_design/canvas/AppIcon.dc.html` and `SplashSpec.dc.html`. The Android text they carry, verbatim:
  - AppIcon: "Android · adaptive icon: Foreground and background are separate 1024 layers on a 108 dp canvas; launchers show the middle 72 dp in their own shape. The moon is drawn smaller here so it looks the same size as on iOS. The monochrome layer is used for themed icons (Android 13+)."
  - SplashSpec: "Android 12+: The system splash shows the moon (as the splash icon) on the night-sky colour. Keep it on screen until the first frame is drawn, then play the same animation in the app." (The animation is Phase 3a.)
- Icon sources: `_design/canvas/icon/android/adaptive-{background,foreground,monochrome}.svg` (and the `.png` references), `_design/canvas/icon/colors.json`, `_design/canvas/icon/layers/moon-white.svg`.

**Design inputs the generator reads** (all under `_design/canvas/`): `ds/chronoflow/tokens.json`, `theme.css`, `ds/chronoflow/components/bundle.js`, `ds/chronoflow/components/bundle.css`, `Main.dc.html`, `SplashScreen.dc.html`, `Wizard.dc.html`, `Set2FASetup.dc.html`, `TodayRowStates.dc.html`, `TermsUpdate.dc.html`, `AuthSignIn.dc.html`, `SetAccount.dc.html`, `SetPreferences.dc.html`, `SetBodyClock.dc.html`, `SetLanguage.dc.html`, `SetPlanCredits.dc.html`, `SetProfile.dc.html`, `PasswordPrompt.dc.html`, `Set2FAOn.dc.html`. Never edit them.

**iOS at `f391a1fa`** (port behaviour from these; keep Swift type and function names):
- `plan.md:25` "Decisions" D10 (TokenGen, committed output, stale test) and D11 (static TTFs, registered off main).
- `plan.md:141` "Tokens (TokenGen output; P3 as `displayP3` r g b)" and `plan.md:173` "Typography".
- `plan.md:663` "Phase 1: Design tokens, fonts and launch assets (`feat/phase-1-design-system`)".
- `Tools/TokenGen/Sources/TokenGenCore/ColorMath.swift` (`RGB`, `OKLab`, `skyRamp`).
- `Tools/TokenGen/Sources/TokenGenCore/Source.swift` (`DesignSource`: names, theme.css cross-check, derived colors l.66–113, `midStop`, `ramp`, `captures`, `number`, `camelCase`, `writeOutputs`).
- `Tools/TokenGen/Sources/TokenGenCore/Generator.swift` (report format, `PressTokens`, `LaunchPaint`, `blurredDisc`).
- `Tools/TokenGen/Sources/TokenGenCore/{PlanTokens,PlanUITokens,AuthTokens,SettingsTokens,TermsTokens,WelcomeTokens}.swift`.
- `Tools/TokenGen/Tests/TokenGenTests/TokenGenTests.swift` (test names to keep, §7).
- `Tools/TokenGen/TokenGenReport.md`, `Tools/TokenGen/{Plan,PlanUI,Settings}.source.json`.
- `Tools/generate_fonts.py` (`generate_metrics` → `TypeMetrics.generated.swift`), `Tools/test_fonts.py`.
- `Packages/ChronoFlowKit/Sources/DesignSystem/Resources/Fonts/` (10 TTFs, 2 OFL texts, `manifest.json`, `SOURCE.md`).
- Goldens: `Packages/ChronoFlowKit/Sources/DesignSystem/{CfSky,PlanUITokens,AuthTokens,SettingsTokens,TermsTokens,WelcomeTokens,PlanTypeMetrics,TypeMetrics}.generated.swift`, `Packages/ChronoFlowKit/Sources/PlanCore/PlanMetrics.generated.swift`.
- `Packages/ChronoFlowKit/Sources/DesignSystem/Typography.swift` (to understand what the metrics feed; its hand-written styles are Phase 2).
- `Assets/moon/launch-moon-{light,dark}.svg`, `Docs/app-icon.md`.

---

## 4. Out of scope

- `ChronoFlowTheme`, `toM3()`, `CfType` `TextStyle`s, `FontFamily`s, font warm-up, `cfPress`, shadows, the sky `Brush`, primitives, lucide/Material Symbols vectors, contrast tests: **Phase 2**. This phase emits data only.
- The splash theme (`Theme.App.Starting`, `windowSplashScreenAnimatedIcon`), the launch overlay, the wordmark path: **Phase 3a**. This phase only provides `@color/launch_sky_top` and `@drawable/launch_moon`.
- The reminders channel and notification accent color: **Phase 12b**. This phase only provides `ic_stat_moon`.
- An open-source licences screen: **no board exists** (`08 §5.2` proposed one). Ship the OFL texts as assets; defer the screen (OQ-0b-8).
- Play Store 512 px listing icon: Phase 13.
- Display P3 rendering (OQ-A7: sRGB only; P3 appears in the report only).
- Regenerating fonts from Google Fonts (iOS `generate_fonts.py` download/instancing): copy the iOS bytes instead.
- iOS-only TokenGen outputs: `Colors.xcassets`, `plan-tokens.json`, `swift format`.

---

## 5. Output map

Every generated text file starts with a header line naming the generator and its inputs and ending in "Do not edit." (Kotlin: `// Generated by :tools:tokengen from <inputs>. Do not edit.`; XML: the same text in a `<!-- -->` comment after the prolog; Markdown: no header, as iOS). UTF-8, LF, final newline, no timestamps.

| Path (committed) | Contents | Consumers |
|---|---|---|
| `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/generated/CfColors.generated.kt` | `@Immutable class CfColors(val isDark: Boolean, …52 Color fields)`, `val CfLightColors`, `val CfDarkColors` | Phase 2 theme |
| `…/designsystem/generated/CfSky.generated.kt` | `class SkyStop(val location: Float, val color: Color)`, `object SkyRamp { val light: List<SkyStop>; val dark: List<SkyStop> }`, `object PressTokens`, `object LaunchPaint` | 2, 3a |
| `…/designsystem/generated/CfTypeScale.generated.kt` | `enum class CfFace`, `class CfTypeMetrics`, `class CfTypeSpec`, `object TypeMetrics`, `object PlanTypeMetrics`, `object CfTypeScale` | 2, 3a, 1c (via 2) |
| `…/designsystem/generated/CfDimens.generated.kt` | `object PlanUITokens`, `object AuthTokens`, `object SettingsTokens`, `object TermsTokens`, `object WelcomeTokens` | 2, 3c, 5a, 6x, 9x, 10x |
| `core/plan/src/main/kotlin/net/chronoflow/core/plan/generated/TimelinePalette.generated.kt` | `class TimelinePalette(…52 Int ARGB fields)` with `companion object { val Light; val Dark }` | 1c |
| `core/plan/…/plan/generated/PlanMetrics.generated.kt` | `object PlanMetrics` (Double, dp), the port of iOS `PlanCore/PlanMetrics.generated.swift` | 1b, 1c (OQ-0b-1) |
| `app/src/main/res/values/colors_launch.xml`, `app/src/main/res/values-night/colors_launch.xml` | `launch_sky_top` = `#FF1F285A` / `#FF06091C` | 3a |
| `tools/tokengen/TokenGenReport.md` | the iOS table format (§6.4) | review, task 9 |
| `tools/tokengen/ios/{Plan,PlanUI,Settings}.source.json` | iOS inputs copied at the pin | generator |
| `tools/tokengen/src/test/resources/ios/…` | iOS goldens copied at the pin (task 1) | tests |
| `tools/tokengen/ios-inputs.sha256` | pin, iOS path, local path, SHA-256 of each copied file | `generators-check` |
| `core/designsystem/src/main/res/font/*.ttf` (10), `core/designsystem/src/main/assets/licenses/{Figtree,Fraunces}-OFL.txt`, `core/designsystem/fonts.sha256` | fonts, licences, manifest | 2 |
| `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml`; `app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml` | adaptive icon | launcher |
| `app/src/main/res/drawable/ic_stat_moon.xml` | notification icon | 12b |
| `tools/assets/moon/launch-moon-{light,dark}.srgb.svg`, `app/src/main/res/drawable-xxxhdpi/launch_moon.png`, `app/src/main/res/drawable-night-xxxhdpi/launch_moon.png`, `tools/assets/assets.sha256` | launch moon | 3a |
| `docs/assets.md` | conversion method, moon substitution, screenshots | review |

---

## 6. Behaviour spec (what the port must do)

### 6.1 Color math (`ColorMath.kt`, port of `ColorMath.swift`)

Input `oklch(L C H [/ A])`; parse exactly like iOS: strip `oklch(` and `)`, replace `/` by a space, split on spaces, parse doubles; 3 or 4 numbers, else `GenerationError("Invalid OKLCH: …")`.

```
a = C·cos(H·π/180); b = C·sin(H·π/180); alpha = A or 1
l = (L + 0.3963377774a + 0.2158037573b)^3     // Math.pow(x, 3.0), negative bases allowed
m = (L − 0.1055613458a − 0.0638541728b)^3
s = (L − 0.0894841775a − 1.2914855480b)^3
R =  4.0767416621l − 3.3077115913m + 0.2309699292s
G = −1.2684380046l + 2.6097574011m − 0.3413193965s
B = −0.0041960863l − 0.7034186147m + 1.7076147010s
p3 = true: [0.8224619687R + 0.1775380313G, 0.0331941989R + 0.9668058011G, 0.0170826307R + 0.0723974407G + 0.9105199286B]
each channel: c = min(1, max(0, c)); enc = c ≤ 0.0031308 ? 12.92c : 1.055·c^(1/2.4) − 0.055
hex byte = Math.round(enc·255) (half up; NEVER kotlin.math.round, which rounds half to even)
OKLab.mixed(other, f) = per-component lerp of L, a, b, alpha; RGB.mixed likewise on encoded channels
```

Keep the type names `RGB` (with `hex`), `OKLab`, `RampStop`, `GenerationError` (a Kotlin `Exception`), and the function `skyRamp`.

### 6.2 Design source (`Source.kt`, port of `Source.swift`)

- `DesignSource.names`: the 42 names in iOS order: `sky-top, sky-mid, sky-horizon, sky-title, sky-copy, sky-copy-background, dawn, dawn-foreground, background, foreground, card, card-foreground, popover, popover-foreground, destructive-foreground, input, border, ring, surface-2, muted, secondary, secondary-foreground, now, cat-sleep, cat-sleep-fg, cat-light, cat-light-fg, cat-moderate, cat-moderate-fg, cat-dark, cat-dark-fg, cat-caffeine, cat-caffeine-fg, cat-travel, cat-travel-fg, muted-foreground, primary, primary-foreground, accent, accent-foreground, destructive, dawn-text`.
- `authDerivedNames` = `auth-error-background, auth-error-border, auth-shadow, settings-warning-background, settings-warning-border, settings-qr-background`; `wizardDerivedNames` = `wizard-notice-background, wizard-notice-border`. Rules exactly as `Source.swift:66-113` (OKLab mixes; alpha variants 0.35 and 0.4; QR from `Set2FASetup` `background: (oklch\(1 0 0\))`; wizard fraction from the `Wizard.dc.html` regexes; `auth-shadow` from the first `oklch(...)` in `shadow-soft`).
- **Android-only derived** (plan "Material mapping", not on iOS): `system-selected` = mix(card, dawn, 0.16). And `wizard-picker-fill` = the iOS `Color.wizardPickerFill` from `PlanUITokens.swift` (regex `const PILL = '[^']*background: rgba\(([\d., ]+)\)` on `Wizard.dc.html` → `rgba(118, 118, 128, 0.12)`), the same value in both themes. Do not add `pickerSelected` or any glass color (OQ-A6).
- Constructor checks, all fatal with a message naming the token: every name resolves without cycles in both themes (aliases `{name}`); each of the 42 names resolves in `theme.css` (`:root, [data-theme="light"] {…}` and `[data-theme="dark"] {…}` blocks, regex `--([a-z0-9-]+):\s*([^;]+);`, following `var(--x)`) to the **same string** as `tokens.json`; `midStop` = exactly 55 % / 60 % from `--sky-mid-stop:\s*([\d.]+)%;`.
- Regexes: port the iOS patterns **verbatim**. `captures(pattern, text)` returns group 1 of every match. In Kotlin raw strings, `$` followed by a letter or `{` starts a template: write `${'$'}`.

### 6.3 Sky ramp (sRGB)

Same loop as `skyRamp` (divisions start at 1 and double; segment 1 drops its last stop so the mid stop appears once; validity at x = i/1000, i = 0…1000, using `firstIndex { location >= x }`, `lo = max(0, hi − 1)`, `f = 0` when `hi == lo`), but every `rgb(p3 = true)` becomes **`rgb(p3 = false)`**, and the tolerance stays **0.25/255** per channel. Expected (F3):

- Light (mid 0.55): `#1F285A #222A5C #252B5F #282D61 #2A2F63 #2D3066 #303268 #33336A #36356C #39366F #3C3871 #3F3A73 #413B76 #443D78 #473E7A #4A407D #4D427F #56487F #5F4E7F #68537F #71597F #7B5E7E #84647D #8E697C #986E7B #A27379 #AC7878 #B67E76 #C18373 #CB8871 #D58D6E #E0926A #EB9666`
- Dark (mid 0.6): `#06091C #070A1E #090B20 #0B0C22 #0C0D24 #0E0E27 #100F29 #12102B #13112D #151330 #171432 #191534 #1B1637 #1C1739 #1E183B #20193E #221A40 #291F41 #312341 #392742 #402B42 #492F42 #513342 #593741 #623B41 #6A3F40 #73423F #7C463D #854A3C #8E4D3A #975137 #A05434 #AA5830`
- Locations: light `i·0.034375` for i = 0…16 (to 0.55), then `0.55 + j·0.028125` for j = 1…16; dark `i·0.0375` (to 0.6), then `0.6 + j·0.025`. Compute each exactly as the iOS `segment` does: `val f = i.toDouble() / divisions; start + (end - start) * f` (divide first; `(end - start) * i / divisions` can differ in the last bit), and emit as `Float`. Interpolate colors with the same `f`.
- iOS reference samples of the true OKLab gradient (iOS `plan.md:161-164`) at x = 0.0, 0.1, …, 1.0: light `#1F285A #272D61 #303167 #38366E #403B75 #493F7C #5D4C7F #7E607E #A1737A #C58572 #EB9666`; dark `#06091C #0A0C22 #0F0F27 #13112D #181434 #1D173A #221A40 #402B42 #623B41 #854A3C #AA5830`.

### 6.4 Report (`tools/tokengen/TokenGenReport.md`)

Same structure as iOS: title `# TokenGen report`; a source paragraph that states "OKLCH → OKLab → linear sRGB → clip → sRGB transfer curve; the P3 columns are linear Display P3 → clip → the same transfer curve, for reference only (OQ-A7)"; then `| Token | Light sRGB | Light P3 | Dark sRGB | Dark P3 |`. Rows in iOS order: the 42 names, then the 6 auth/settings names, then the 2 wizard names (camelCase via `camelCase`), then the Android-only rows `systemSelected` and `wizardPickerFill` (P3 cells `—`). Hex cell = `#RRGGBB`, plus ` α<alpha>` when alpha < 1, where `<alpha>` is Kotlin `Double.toString()` (`0.9`, `0.35`, `0.25`, `0.6`, `0.4`, `0.12`). P3 cell = three channels formatted with `String.format(Locale.ROOT, "%.10f", v)` and joined by spaces. After the table: `Light sky: 33 stops, including exact mid 0.55; encoded sRGB channel interpolation checked at 1001 points to ±0.25/255.` and the same line for `Dark sky` with `0.6`.

### 6.5 Kotlin color outputs

- `CfColors` field order and names = the report row order, camelCase (`skyTop … dawnText, authErrorBackground, authErrorBorder, authShadow, settingsWarningBackground, settingsWarningBorder, settingsQrBackground, wizardNoticeBackground, wizardNoticeBorder, systemSelected, wizardPickerFill`): 52 `Color` fields after `isDark`. Use a plain `class` with `@Immutable`, not a `data class` (`08 §3.4`). Values as `Color(0xAARRGGBB)` literals with the alpha byte from F9.
- `TimelinePalette` in `:core:plan` (pure Kotlin, no Android or Compose import): the same 52 names as `Int` ARGB (`0xFFF3F7FC.toInt()`), `Light` and `Dark` instances. The generator writes both files from one model, so they cannot drift.
- `colors_launch.xml`: one `<color name="launch_sky_top">` per file, from `sky-top`.

### 6.6 Press and launch paint (`CfSky.generated.kt`, port of the tail of `Generator.swift`)

- Fail unless `theme.css` gives scale `0.97`, opacity `0.85`, duration `120` and contains `.cf-press { transition: none; }` and `.cf-press:active { transform: none; }`. Emit `object PressTokens { const val scale = 0.97f; const val opacity = 0.85f; const val durationMillis = 120; val curve = floatArrayOf(0.25f, 0.1f, 0.25f, 1f) }` (iOS keeps seconds; Android uses milliseconds, the only unit change).
- Fail unless `SplashScreen.dc.html` contains `background: #fff;`, `box-shadow: 0 0 80px 20px` and `box-shadow: 0 0 10px 2px rgba(255,255,255,0.8)`.
- `blurredDisc(radius, disc, sigma)`: 256 strips, `step = disc/256`, `y = (i+0.5)·step`, `edge = √(disc² − y²)`, `horizontal = (erf((edge − r)/(√2σ)) − erf((−edge − r)/(√2σ)))/2`, sum of `horizontal·exp(−y²/(2σ²))/(√(2π)σ)·step·2`.
- `haloStops` = `(0,0)`, `(90/250, 0)`, then for r = 90.01 + 2k while r ≤ 250 (k = 0…79, computed as `90.01 + 2.0*k`, never by accumulation): `(r/250, blurredDisc(r, 110, 40)·0.45)`, then `(1, 0)`: 83 pairs. `planeAlphas` = for r = 0.5·k, k = 0…48: `blurredDisc(r, 6, 5)·0.8`: 49 values. `white` = opaque white.
- Kotlin has no `erf`. Add `Erf.kt`: a port of fdlibm `s_erf.c` `erf()` (keep its copyright notice; it is the licence condition). It must match `erf(0.5) = 0.5204998778130465`, `erf(1) = 0.8427007929497149`, `erf(2) = 0.9953222650189527`, `erf(−1) = −0.8427007929497149`, `erf(4) = 0.9999999845827421` within 1e-15. Abramowitz–Stegun approximations are too coarse (1.5e-7) and fail the golden.
- Emit `LaunchPaint.haloPositions: FloatArray`, `LaunchPaint.haloAlphas: FloatArray`, `LaunchPaint.planeAlphas: FloatArray`, `LaunchPaint.white: Color`.

### 6.7 Metrics and type (`CfDimens`, `CfTypeScale`, `PlanMetrics`)

Port each iOS generator function one to one, with the same source files, regexes, guards ("board differs from plan", "uniquely match", "sources disagree", "Settings metric changed") and ordering:

| Kotlin object (file) | Port of | Value type |
|---|---|---|
| `PlanUITokens` (CfDimens) | `PlanUITokens.swift`: `spacing` then `radius` tokens with a `px` value, in `tokens.json` order (`space1`…`space16`, `radiusSm`…`radiusFull` = 9999); `markedOpacity` = first `opacity: ([\d.]+);` in `TodayRowStates.dc.html` (= 0.45); every key of `PlanUI.source.json` except `source`, sorted | `const val … : Float` (dp, sp, opacity, ratio; `walkthroughDotDuration` stays in seconds) |
| `AuthTokens` (CfDimens) | `AuthTokens.swift` metrics, `disabledOpacity`, `light/darkShadow{Y,Radius,Spread}` | `const val Float` |
| `SettingsTokens` (CfDimens) | `SettingsTokens.swift` with `Settings.source.json`; `…Ratio` divided by 100; `skeletonRowWidths` | `const val Float`; `val skeletonRowWidths: List<Float>` |
| `TermsTokens`, `WelcomeTokens` (CfDimens) | `TermsTokens.swift`, `WelcomeTokens.swift` (Welcome fails unless every match equals the expected value) | `const val Float` |
| `PlanMetrics` (`:core:plan`) | `PlanTokens.swift` metrics: `spacingMultipliers × space-1` (sorted), `spacingTokens` (sorted), `scalars` (sorted), `laneLabelSize/LineHeight`, `<type>Size/LineHeight` (sorted), `pointsPerMinute = pointsPerHour / 60` | `const val Double` |
| `PlanTypeMetrics` (CfTypeScale) | `PlanTokens.swift` type part: `<name>: CfTypeMetrics`, `<name>Face: CfFace = Figtree<weight>` (port as is, including `planCardTitleFace = Figtree400`; Phase 2 assigns Fraunces as iOS `Typography.swift` does) | |
| `TypeMetrics` (CfTypeScale) | `generate_fonts.py` `generate_metrics`: `splashWordmark` (from the `word:` style in `SplashScreen.dc.html`), `body`, `sectionTitle`, `eyebrow`, `hero`, `bodyLg`, `button` (`lineHeight = null`) | |
| `CfTypeScale` (CfTypeScale) | the `Typography` extensions of `PlanUITokens.swift:27-124` (12 `screen*` styles from `tokens.json` `type.groups[].styles`, skipping `page-title-lg`; face rule: family `display` → always `Fraunces400Opsz<n>` whatever the token weight, with n = 48 for size ≥ 40, 32 for size ≥ 26, else 20; other families → `Figtree<fontWeight>`; size = `fontSize` without `px`; line height = `lineHeight` without `px` when it is a string, else the unitless number × size (`block-label` 11 × 1.25 = 13.75); tracking pt = `letterSpacing` em × size, else 0; plus the 20 composite styles), `AuthTokens.swift` (7 `auth*`), `SettingsTokens.swift` (5 parsed + 4 composite `settings*`), `TermsTokens.swift` (3 `terms*`) | `CfTypeSpec` |

Types (in `CfTypeScale.generated.kt`): `enum class CfFace { Figtree400, Figtree500, Figtree600, Figtree700, Fraunces400Opsz20, Fraunces400Opsz32, Fraunces400Opsz48, Fraunces500Opsz20, Fraunces500Opsz32, Fraunces500Opsz48 }`; `class CfTypeMetrics(val size: Float, val lineHeight: Float?, val letterSpacingEm: Float)`; `class CfTypeSpec(val face: CfFace, val size: Float, val lineHeight: Float?, val letterSpacingEm: Float)`. Sizes and line heights are sp. iOS stores tracking in points; convert with `letterSpacingEm = trackingPt / size` (`08 §5.3`: letter spacing in em). iOS `relativeTo` is dropped: Android scales every style by the capped font scale, and `splashWordmark`'s fixed size is Phase 3a's job.

Resolved composite styles (iOS writes them as expressions; tracking in pt):

| Style | Face | Size/line | Tracking pt |
|---|---|---|---|
| walkthroughBody | Figtree500 | 16/24 | 0 |
| wizardReadySummary | Fraunces400Opsz20 | 20/27 | 0 |
| wizardFailureTitle | Fraunces400Opsz20 | 22/28 | 0 |
| wizardFailureBody | Figtree400 | 15/22 | 0 |
| wizardSecondary | Figtree500 | 16/22 | 0 |
| wizardArc | Fraunces400Opsz32 | 26/32 | 0 |
| wizardNavTitle | Figtree500 | 15/20 | 0 |
| wizardSearchCode | Figtree700 | 15/20 | 0.6 |
| wizardSearchName | Figtree400 | 16/21 | 0 |
| wizardLongTitle | Fraunces400Opsz20 | 24/30 | −0.24 |
| wizardAirport / wizardAirportPlaceholder | Figtree600 / Figtree400 | 20/26 | 0 |
| wizardRow / wizardButton | Figtree400 / Figtree600 | 17/22 | 0 |
| wizardSubtitle | Figtree400 | 16/22 | 0 |
| wizardChronotype | Figtree600 | 16/22 | 0 |
| wizardFieldLabel | Figtree400 | 15/20 | 0 |
| wizardHint | Figtree400 | 13/18 | 0 |
| wizardChoice | Figtree600 | 18/24 | 0 |
| wizardRoute | Figtree600 | 15/20 | 0 |
| settingsRow / settingsStrong | Figtree500 / Figtree600 | 16/24 | 0 |
| settingsPill | Figtree500 | 14/20 | 0 |
| settingsBadge | Figtree500 | 12/16 | 0 |
| termsTitle | Fraunces400Opsz32 | 32/38 | −0.3 (screenPageTitle's, kept as iOS) |
| termsBody / termsStrong | Figtree400 / Figtree600 | 15/22 | 0 |

`screenDayTitle` stays 18/28 (tokens), exactly as iOS generates it. The day header uses `planDayTitle` 18/26 (OQ-A9, iOS `DayHeader.swift:62`), which Phase 2 maps. Record this in one line of the report under "Type notes".

### 6.8 Number formatting (determinism)

- Kotlin `Float` literal: `BigDecimal(v).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "f"` (`0f`, `-12f`, `0.97f`). `Double` literal: the same without `f`, with `.0` appended when there is no point.
- Report numbers: `String.format(Locale.ROOT, "%.10f", v)`. Never rely on the default locale: on `nl-NL`, `%.10f` writes a comma.
- Ordering: exactly as iOS (list order, or `sorted()` where iOS sorts keys). No map iteration over `HashMap`.

### 6.9 CLI and Gradle

- `Main.kt`: `--root <repo>` (required) and optional `--check`. Without `--check`, `writeOutputs(check = false)` writes each output atomically (temp file, then rename). With `--check`, `writeOutputs(check = true)` also writes each generated output to `tools/tokengen/build/verify/<same relative path>` (for diffing), compares its bytes with the committed file, collects **every** stale or missing path, and then throws one `GenerationError` whose message has one line `stale generated output: <path>` per path; `Main` prints it as below and exits 1. Success prints `TokenGen: <n> outputs verified` or `written`. Any `GenerationError` prints `TokenGen: <message>` to stderr and exits 1.
- `tools/tokengen/build.gradle.kts`: keep 0a's convention plugin (`chronoflow.jvm.library`). Add `kotlinx-serialization-json` (catalog, A1) and use only the `JsonElement` API (`Json.parseToJsonElement`), so no compiler plugin is needed. Register `generateTokens` and `verifyTokens` as `JavaExec` tasks on `sourceSets.main.runtimeClasspath`, passing `--root` from `rootProject.layout.projectDirectory` captured at configuration time. Declare their inputs (the design files in §3, `tools/tokengen/ios/`, the committed outputs for `verifyTokens`) and, for `verifyTokens`, a stamp output, so the configuration cache and up-to-date checks are correct. The `test` task gets `systemProperty("cf.repoRoot", <absolute root path string>)` and the same inputs.
- Spotless: exclude `**/*.generated.kt` (the generator owns their exact bytes; `verifyTokens` guards them). The generated code must still compile with `allWarningsAsErrors`.

---

## 7. Tasks

Each task is one Conventional Commit with no trailer. Generated files go in the same commit as the generator change that produced them. Tick the `plan.md` checkbox in the commit named in "Ticks". Test layer: everything here is **JVM** except one Robolectric inflation test; there is no emulator test. All JVM tests < 1 s each; Robolectric < 5 s.

### Task 1: pin the iOS inputs and goldens

Commit: `build(tokengen): pin ios token inputs and goldens`. Ticks: none.

- `tools/tokengen/copy-ios-inputs.sh` (POSIX `sh`, `set -eu`). It reads the pin from `tools/ios-pin.txt` and the repo from `${CF_IOS_REPO:-/Users/rickgout/Sites/chronoflow-ios}`, and uses only `git -C "$repo" show "$pin:<path>"`. It writes:
  - `Tools/TokenGen/{Plan,PlanUI,Settings}.source.json` → `tools/tokengen/ios/`;
  - into `tools/tokengen/src/test/resources/ios/`: `Tools/TokenGen/TokenGenReport.md`; `Packages/ChronoFlowKit/Sources/DesignSystem/{CfSky,PlanUITokens,AuthTokens,SettingsTokens,TermsTokens,WelcomeTokens,PlanTypeMetrics,TypeMetrics}.generated.swift`; `Packages/ChronoFlowKit/Sources/PlanCore/PlanMetrics.generated.swift`.
  - `tools/tokengen/ios-inputs.sha256`: a first line `# ios-pin <the full SHA in tools/ios-pin.txt>` (0a writes `f391a1fac33eb0f0ea9e3f565206b91118e793d0`, OQ-0a-9), then one line per file: `<sha256>  <ios path>  <local path>`, sorted by local path. **Manifest format (all three manifests of this phase):** three fields separated by two spaces; a field that does not apply is `-` (an iOS input that is not copied has local path `-`; a locally produced file has iOS path `-`); lines starting with `#` are comments.
  - It fails with a clear message when the repo or the pin is missing.
- `Scripts/check-sha256-manifest.py` (`python3 -I`, stdlib only): `check-sha256-manifest.py <manifest>` verifies that every local path (relative to the repo root) exists and hashes to its recorded value. It ignores `#` lines, skips entries whose local path is `-`, prints each mismatch, and exits 1 if there was any. Tasks 6 and 9 reuse it.
- Expected source hashes (verified at the pin): `Plan.source.json` `e1047f3b2cd78d42be89e39f5ac5993b6398cba07eac64f538d1767956d183a7`, `PlanUI.source.json` `9a405b0b17383071e373119a1d2320496286a41eca1c117cbeba4d3ff3b93bd2`, `Settings.source.json` `92d65d29c7a8a35410f062bd3cbdc2fbbaa69984dc042a0c07b4fe15fb804a39`, `TokenGenReport.md` `ebf4420b4f5c6878f38393163e2c1eb199c5b1bf1a158c40a5f523a0fef6a3a6`.
- Tests: `Scripts/tests/test_check_sha256_manifest.py` (unittest): a clean manifest passes; one flipped byte fails and names the path; a missing file fails; comment lines and entries with local path `-` are ignored. 0a's `Scripts/test_scripts.sh` discovers `Scripts/tests/test_*.py`, so it runs in `make quick` with no extra wiring.
- Note for zsh users: write `git show "$pin:$path"` with quotes and braces where needed; in zsh an unquoted `$P:P…` is parsed as a path modifier.
- Edge: run the copy twice; the second run must produce no diff.

### Task 2: port the color math and the design source

Commit: `feat(tokengen): port oklch color math and design source`. Ticks: none.

- Files (package `net.chronoflow.tools.tokengen`, under `tools/tokengen/src/main/kotlin/net/chronoflow/tools/tokengen/`): `ColorMath.kt` (§6.1, §6.3), `Source.kt` (§6.2: `DesignSource(root: Path)`, `color(name, appearance): OKLab`, `midStop`, `ramp`, `captures`, `number`, `camelCase`), `Erf.kt` (§6.6).
- Tests (`tools/tokengen/src/test/kotlin/…`); keep the iOS test names where they exist:
  - `ColorMathTest`
    - `specColorsAndAliases`: for each of the 42 names × 2 themes, `rgb(p3=false).hex` equals the iOS report cell, read by parsing `src/test/resources/ios/TokenGenReport.md` (84 assertions, one parameterized case per name). P3 channels are within 1e-9 of the report's values.
    - `derivedColorsMatchTheIOSReport`: the 8 derived names, hex plus alpha (`authErrorBorder` 0.35, `authShadow` light `#202955` α0.25 and dark `#000000` α0.6, `settingsWarningBorder` and `wizardNoticeBorder` 0.4).
    - `systemSelectedIsTheOKLabMix`: `#FAECE3` and `#302D38`.
    - `roundingIsHalfUp`: a channel of exactly x.5/255 rounds up.
  - `SkyRampTest`
    - `perSegmentRampMatchesTrueOKLab`: the stop lists of §6.3 exactly (33 per theme); the mid stop present exactly once at 0.55 / 0.6; at every i/1000 the interpolated stops are within 0.25/255 of true OKLab; the 11 iOS samples match.
  - `DesignSourceTest`
    - `conflictingDesignSourcesAreRejected`: copy `tokens.json` and `theme.css` into a temp root, change `--dawn: oklch(0.72 0.14 55)` to `0.73` in the CSS, and expect `GenerationError`.
    - `skyMidStopChangeIsRejected` (55 % → 50 %).
    - `aliasCycleIsRejected` (planted `{a}` → `{b}` → `{a}`).
    - `wizardExclusionSurfacesUseTheBoardsOKLabMixInBothAppearances` (iOS test, sRGB).
  - `ErfTest`: the five reference values within 1e-15.
- Edge: `pow` with a negative base (`cat-travel` light has a negative linear R, −0.0170, clipped to 0). Use `Math.pow(x, 3.0)`, not `cbrt` tricks.
- Edge: `camelCase` must reproduce Foundation `capitalized` (`Source.swift:147-150`): split on `-`, keep part 0, and for every later part lower-case it and upper-case its **first letter**, even after digits. iOS emits `radius-2xl` → `radius2Xl` and `radius-3xl` → `radius3Xl` (`PlanUITokens.generated.swift`); Kotlin `replaceFirstChar { it.uppercase() }` would give `radius2xl` and fail `MetricsGoldenTest`. Add `camelCaseMatchesFoundationCapitalized` to `DesignSourceTest`: `sky-copy-background` → `skyCopyBackground`, `surface-2` → `surface2`, `radius-2xl` → `radius2Xl`, `screen-body-lg` → `screenBodyLg`.

### Task 3: generate colors, sky, press and launch paint

Commit: `feat(tokengen): generate color, sky and launch tokens`. Ticks: none.

- `Generator.kt`: `tokenOutputs(root): Map<String, ByteArray>` (sorted keys), building one model and rendering `CfColors.generated.kt`, `CfSky.generated.kt`, `TimelinePalette.generated.kt`, both `colors_launch.xml` and `TokenGenReport.md` (§6.4–§6.6). `Outputs.kt`: `writeOutputs(outputs, root, check)` per §6.9. `Main.kt`.
- `tools/tokengen/build.gradle.kts`: the `generateTokens` and `verifyTokens` tasks (§6.9).
- `core/designsystem` build: if 0a's skeleton lacks them, add the Compose BOM `ui-graphics` and `runtime` dependencies (needed for `Color` and `@Immutable`).
- Run `./gradlew :tools:tokengen:generateTokens` and commit the outputs.
- Tests:
  - `ReportGoldenTest.reportMatchesTheIOSReportRowForRow`: for the 50 iOS rows, the Light and Dark sRGB cells are string-equal and the P3 cells within 1e-9; then exactly 2 Android-only rows; the two sky lines name 33 stops.
  - `LaunchPaintGoldenTest`: `haloStops` (83 pairs) and `planeAlphas` (49) are within 1e-9 of the values parsed from `ios/CfSky.generated.swift`. `PressTokens` are 0.97, 0.85, 120 and [0.25, 0.1, 0.25, 1].
  - `PaletteParityTest`: for all 52 names and both themes, the ARGB in `CfColors.generated.kt` equals the ARGB in `TimelinePalette.generated.kt` (parse both emitted texts); `launch_sky_top` = `#FF1F285A` / `#FF06091C`.
  - `DeterminismTest.twoRunsAreByteIdenticalInAnyLocale`: run `tokenOutputs` with `Locale.setDefault(Locale.forLanguageTag("nl-NL"))` and again with `en-US`, then compare the bytes. Restore the default in `finally`.
  - `OutputsTest.staleOutputIsRejected` (iOS name): in a temp dir, `writeOutputs(check = true)` against a modified file throws and names the path; against a missing file it also throws.
  - `GuardTest.pressAndSplashGuardsRejectBoardDrift`: a `theme.css` text with `scale(0.96)`, and a `SplashScreen.dc.html` text without `background: #fff;`, each throws `GenerationError`. Keep the guards as functions of the file text (`pressTokens(theme: String)`, `launchPaint(splash: String)`) so the test needs no temp repo.
  - Not ported, by design: iOS `generatedFilesAreCurrentAndLaunchSkyCopiesMatch` (replaced by `verifyTokens`; there is no xcassets copy) and `coreAnimationRampPixelsMatchTrueOKLab` (renders with Core Animation; Phase 2 owns the Android sky brush test).

### Task 4: generate the type scale and dimensions

Commit: `feat(tokengen): generate type scale and dimension tokens`. Ticks: `plan.md` "**TokenGen port**".

- Files: `PlanTokens.kt`, `PlanUITokens.kt`, `AuthTokens.kt`, `SettingsTokens.kt`, `TermsTokens.kt`, `WelcomeTokens.kt`, `FontMetrics.kt` (the `generate_metrics` port). Each returns an ordered model (`List<Metric(name, value, kind)>` and `List<TypeEntry(name, face, size, lineHeight, trackingPt)>`). The emitters render `CfDimens.generated.kt`, `CfTypeScale.generated.kt` and `PlanMetrics.generated.kt` (§6.7). Generate and commit.
- Tests (each parses the copied iOS Swift; first join each file's lines on whitespace so that wrapped `Style(` calls become one line):
  - `MetricsGoldenTest`: for each of `PlanUITokens`, `AuthTokens`, `SettingsTokens`, `TermsTokens`, `WelcomeTokens` and `PlanMetrics`, every `static var (\w+): (CGFloat|Double) \{ (-?[\d.]+) \}` in the iOS file appears in the Kotlin model with an equal value (|Δ| ≤ 1e-9). Expected match counts at the pin: `PlanMetrics` 99, `PlanUITokens` 115, `AuthTokens` 36, `SettingsTokens` 21, `TermsTokens` 8, `WelcomeTokens` 15. The Kotlin model has no extra names, except the two iOS entries that are not numeric literals: `PlanMetrics.pointsPerMinute` (= `pointsPerHour / 60` = 40/60) and `SettingsTokens.skeletonRowWidths` (= [150, 190, 130]), each asserted separately.
  - `TypeScaleGoldenTest`: every literal style, matched on the joined text with `Style\(\s*face: \.(\w+), size: (-?[\d.]+), lineHeight: (-?[\d.]+), tracking: (-?[\d.]+)` (composites whose size is an expression do not match and are covered by the table instead), equals the Kotlin entry (face mapped `figtree400` → `Figtree400`, `fraunces400Opsz20` → `Fraunces400Opsz20`; `letterSpacingEm × size` = c within 1e-9). The composite styles equal the §6.7 table. `PlanTypeMetrics` and `TypeMetrics` entries equal their iOS files (`button.lineHeight == null`).
  - `welcomeMetricsComeFromTheBoard` (iOS name): `topPadding` 64, `moonSize` 28, `cardRadius` 22, `buttonHeight` 52, and the emitted file contains them.
  - `authSourceChangesCannotHideBehindUnrelatedDeclarations` (iOS name): port the iOS mutation table verbatim (`TokenGenTests.swift:226-290`: 22 mutations = 17 board, 4 `bundle.js`, 1 `bundle.css`; plus the 2 family renames `'Figtree',` → `'OtherSans',` and `'Fraunces',` → `'OtherDisplay',` in `tokens.json`). Assert first that each `before` string occurs in its source. Each mutation either changes the auth output or throws `GenerationError`; each family rename must throw. This needs the iOS seam `authTokens(root, board, bundle, css, json)` (`AuthTokens.swift:16`): keep it as an `internal` overload.
  - `authMixUsesOKLabAndPreservesBothAppearances` (iOS name): the contrast pairs ≥ 4.5 and the accepted border pairs < 3, computed in sRGB.
  - `authGuestFooterExceptionStillMatchesTheBoardPairAndTypography` (iOS name).
  - `settingsMetricDriftIsRejected`: in a temp root, take `SetAccount.dc.html` (whose `rowVertical` pattern `min-height: \d+px; padding: (\d+)px 16px` matches `10` four times, F5) and change only the **second** `padding: 10px 16px` (the first row with `border-top`) to `padding: 11px 16px`. Generation must throw `GenerationError("Settings metric changed: rowVertical")`. Do not edit the first occurrence: it is also the only `rowHeight` match, and `rowHeight` sorts before `rowVertical`, so the run would fail on `rowHeight` instead (metrics are generated in sorted key order).
- Edges: `PlanUI.source.json` mixes ints and doubles. Read each as a `Double` and emit by the §6.8 rule; the iOS type split is by suffix (`Opacity`, `Ratio`, `Saturation`, `Duration` → Double; others CGFloat), and both become `Float` here. Make the `AuthTokens` regex uniqueness checks fatal, as on iOS.

### Task 5: wire `make tokens` and `verifyTokens`

Commit: `build(tokengen): wire make tokens and verify tokens`. Ticks: `plan.md` "**Golden tests**".

- Makefile (replace 0a's `tokens` placeholder recipe, touching only these lines):
  - `tokens`: `sh tools/tokengen/copy-ios-inputs.sh && $(GRADLE) -q :tools:tokengen:generateTokens`.
  - `tokens-check` (new, `.PHONY`): `python3 -I Scripts/check-sha256-manifest.py tools/tokengen/ios-inputs.sha256 && $(GRADLE) -q :tools:tokengen:verifyTokens :tools:tokengen:test`.
  - 0a's `generate` and `generators-check` run `$(GENERATORS)` / `$(GENERATOR_CHECKS)`. Add one line each, directly under 0a's empty definitions: `GENERATORS += tokens` and `GENERATOR_CHECKS += tokens-check`. One `+=` line per target keeps the parallel 0c/0d additions conflict-free. Tasks 6, 7 and 9 add `fonts-check`, `icons` and `assets-check` the same way.
  - `make tokens` reads the iOS repo at the pin, so `make generate` (and `Scripts/ci.sh`) needs it on this Mac. `generators-check` never does.
- `generators-check` must not need the network, `rsvg-convert` or the iOS repo.
- Proof (record the command and its exact output in `docs/tooling.md`, "Phase 0b: stale-output proof"): change one hex in `CfColors.generated.kt` by hand; `make generators-check` must fail and name that path; revert; it passes.

### Task 6: fonts

Commit: `feat(designsystem): add figtree and fraunces fonts`. Ticks: `plan.md` "**Fonts**".

- `tools/fonts/copy-fonts.sh` (same pin and repo rules as task 1) copies from `Packages/ChronoFlowKit/Sources/DesignSystem/Resources/Fonts/`:

  | iOS file | Android file | SHA-256 | Bytes |
  |---|---|---|---|
  | `CFFigtree-400.ttf` | `figtree_400.ttf` | `686c2512dfe40452a46d04008cd4969810f8a3ff3f4e5a8a627b3260aa053344` | 40088 |
  | `CFFigtree-500.ttf` | `figtree_500.ttf` | `1cafc3d5c8d2b81d2a4f4a88958445f9169e3bfe669122c1226ad4fec770e113` | 40080 |
  | `CFFigtree-600.ttf` | `figtree_600.ttf` | `8fb8e0dd3c66a26f0561b626f61aa88eba919da9e596b0b1e8959e86951d4555` | 40096 |
  | `CFFigtree-700.ttf` | `figtree_700.ttf` | `e3618108cf5b2ed4f5af6c0848e2a48bbda4eea33caa2cb34650c86701c2ab1c` | 40080 |
  | `CFFraunces-400-Opsz20.ttf` | `fraunces_400_opsz20.ttf` | `1d5e40673d093bb6047b60f0b4618fb9b93055ac4dad2f9dfc07a7d90535f3e5` | 72948 |
  | `CFFraunces-400-Opsz32.ttf` | `fraunces_400_opsz32.ttf` | `e5614ccbec229d798127395a494514bc5f458c3da283d0d25e60e4326da2bb1d` | 72916 |
  | `CFFraunces-400-Opsz48.ttf` | `fraunces_400_opsz48.ttf` | `c9bdb5b1d9e24187e830b50598ef07ee19d75da8a34ab43867340f4199b4b9f2` | 72888 |
  | `CFFraunces-500-Opsz20.ttf` | `fraunces_500_opsz20.ttf` | `fc7a3d5221232b1c5e35562776d6d6324b128e2268b43e9c2a8ea04e56c0f5b1` | 72912 |
  | `CFFraunces-500-Opsz32.ttf` | `fraunces_500_opsz32.ttf` | `3ddf293fb40258e762df17f88106667ceab83cb1944ede7e4e1dcd783862694f` | 72912 |
  | `CFFraunces-500-Opsz48.ttf` | `fraunces_500_opsz48.ttf` | `6c67072103e1b91abbebe36c057292ab195c403271cb87028c60c11eaa00379d` | 72892 |
  | `Figtree-OFL.txt` | `assets/licenses/Figtree-OFL.txt` | `140d37233e7f3ce7313798befa9600893bcceaf41a55fa0fa5ad52f7f657a268` | |
  | `Fraunces-OFL.txt` | `assets/licenses/Fraunces-OFL.txt` | `bdf4c22802eaf804f998195871c6b8938aac2ac14b2d78a8bd66a6f1eced833b` | |

  Fonts go to `core/designsystem/src/main/res/font/`; licences to `core/designsystem/src/main/assets/licenses/`. The script writes `core/designsystem/fonts.sha256` in the task 1 format: pin line, a `# source google/fonts 24ddc1f324cac5d9f5aedca126d298f56e3f1640, fonttools 4.60.1` line, then the 12 entries.
- Makefile: `fonts` runs the script; `fonts-check` (new, `.PHONY`) runs `python3 -I Scripts/check-sha256-manifest.py core/designsystem/fonts.sha256` and `$(GRADLE) -q :core:designsystem:testDebugUnitTest --tests '*FontHashTest'`; add `GENERATOR_CHECKS += fonts-check`. Fonts are copied, not generated, so `fonts` is not added to `GENERATORS`.
- Test `FontHashTest` (JVM, `core/designsystem/src/test/kotlin/net/chronoflow/core/designsystem/FontHashTest.kt`, files read relative to the module directory):
  - exactly these 10 TTFs exist in `res/font`, and each SHA-256 equals the table above, hard-coded in the test so it cannot pass against a rewritten manifest;
  - each file's table directory (offset table, then 16-byte records) has no `fvar` table;
  - `OS/2` `usWeightClass` (uint16 at table offset + 4) equals the weight in the file name;
  - both licences contain `SIL OPEN FONT LICENSE`.
- Edge: resource names must match `[a-z0-9_]`. Fonts are unused until Phase 2. If Lint `UnusedResources` flags them, add a path-scoped ignore in 0a's `lint.xml` with the comment `<!-- 0b: consumed in Phase 2 -->` (§9).

### Task 7: adaptive launcher icon

Commit: `feat(app): add adaptive launcher icon`. Ticks: `plan.md` "**Launcher icon**".

- `tools/assets/build_assets.py` (`python3 -I`, stdlib only, `xml.etree.ElementTree` plus regex). Subcommand `icons [--out <dir>]` writes, deterministically:
  - `app/src/main/res/drawable/ic_launcher_background.xml`: `<vector>` 108 dp × 108 dp, viewport 1024 × 1024, one path `M0,0h1024v1024h-1024z`, `fillColor` as `<aapt:attr>` `<gradient android:type="linear" startX=0 startY=0 endX=0 endY=1024>` with items `0 #FF06091C`, `0.6 #FF221A40`, `1 #FFAA5830`. These are taken from `adaptive-background.svg`; SVG `y2="1"` in bounding-box units is `endY=1024` here.
  - `ic_launcher_foreground.xml`: 108 dp, viewport 1024, `<group translateX=341.6 translateY=341.6 scaleX=14.2 scaleY=14.2>` containing `<path pathData="M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401" fillColor="#00000000" strokeColor="#FFF2AD73" strokeWidth="2.0" strokeLineCap="round" strokeLineJoin="round"/>`. The path is verbatim from the SVG. VectorDrawable applies scale and then translate, which is the same as SVG `translate(…) scale(…)`.
  - `ic_launcher_monochrome.xml`: the same, with `strokeColor="#FFFFFFFF"` and `strokeWidth="2.2"` (as supplied, OQ-A43).
  - `app/src/main/res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`: `<adaptive-icon>` with `<background>`, `<foreground>` and `<monochrome>` pointing at the three drawables.
  - The values are **parsed** from the SVGs, never retyped. The script fails unless the background stops equal `colors.json` `top` (`#06091c`), `mid` (`#221a40`) and `hor` (`#aa5830`) in that order, the foreground stroke equals `dawn` (`#f2ad73`), and the monochrome stroke is `#ffffff` (not in `colors.json`), compared case-insensitively.
- Subcommand `check`: run `icons --out build/assets-check/` and byte-compare against the committed files.
- Manifest: `<application android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round">`. 0a added no placeholder icon; it ignored `MissingApplicationIcon` in `lint.xml` until 0b (0a OQ-0a-5). **Remove that `lint.xml` line and its reason row in `docs/tooling.md` in this commit.**
- Makefile: add `GENERATORS += icons` (the `icons` target runs `python3 -I tools/assets/build_assets.py icons`).
- `docs/assets.md`: a "Launcher icon" section covering the source files, the conversion rules above, and the safe-zone numbers (outer moon radius 142 px of 1024 = 14.98 dp, inside the 33 dp safe radius; visible size 41.6 %, as iOS; `08 §11.2`).
- Tests:
  - `Scripts/tests/test_build_assets.py` (unittest, discovered by 0a's `Scripts/test_scripts.sh`; it imports `tools/assets/build_assets.py` by path): `icons` run twice gives identical bytes; a temp SVG with stop `#06091d` fails against `colors.json`; `check` fails when one committed byte is changed.
  - `AdaptiveIconTest` (JVM, `app/src/test/kotlin/net/chronoflow/app/AdaptiveIconTest.kt`, parsing the XML files): both mipmaps reference background, foreground and monochrome; every layer is 108 dp with a 1024 viewport; the monochrome layer uses exactly one color (`#FFFFFFFF`) and no fill other than transparent; the safe zone holds, meaning the transformed corners of the stroked 24-unit box [1, 23]² lie within 33 dp of the centre (the maximum is 23.3 dp).
  - `LauncherIconInflationTest` (Robolectric, `@Config(sdk = [36])`, same package): `R.mipmap.ic_launcher` inflates to an `AdaptiveIconDrawable` whose `background`, `foreground` and `monochrome` are non-null. This catches path-parse errors that XML checks cannot. If 0a did not put Robolectric on `:app`'s test classpath, add `testImplementation(libs.robolectric)`.

### Task 8: notification icon

Commit: `feat(app): add notification small icon`. Ticks: `plan.md` "**Notification icon**".

- `build_assets.py icons` also writes `app/src/main/res/drawable/ic_stat_moon.xml`: 24 dp × 24 dp, viewport 24 × 24, no group, the same moon path verbatim from `_design/canvas/icon/layers/moon-white.svg`, `strokeColor="#FFFFFFFF"`, `strokeWidth="2"`, round caps and joins, `fillColor="#00000000"`.
- Test `NotificationIconTest` (JVM, parses the XML): 24 dp, viewport 24, every color attribute is `#FFFFFFFF` or fully transparent, and the path equals the board path. Lint `IconColors` must pass in `make check`. `ic_stat_moon` is unused until 12b; scope an `UnusedResources` ignore as in task 6 (`<!-- 0b: consumed in Phase 12b -->`).

### Task 9: launch moon

Commit: `feat(app): rasterize launch moon from ios svgs`. Ticks: `plan.md` "**Launch moon**" and "**Make targets**".

- `build_assets.py moon` (not part of `make generate`; raster is excluded):
  1. Read `Assets/moon/launch-moon-{light,dark}.svg` at the pin through `git show` (expected SHA-256 light `5c6f92500ea5f03edb16a74a9f41eb148f34668e1ef2ef5550316dfbce6d0066`, dark `71909bcfffbf4283bc35604acad4cd7b431ec67e900687183ec73606aa033134`).
  2. Assert that each file has exactly one `stroke="color(display-p3 r g b)"`, and that `r g b` equals the `dawn` P3 cell of the iOS report for that theme (`0.8542512390 0.5599361895 0.3257756868` / `0.9082625891 0.6882337800 0.4860896207`).
  3. Replace that attribute with `stroke="#E78A45"` / `stroke="#F2AD73"`, the `dawn` sRGB cells read from `tools/tokengen/TokenGenReport.md`. Change nothing else. Write the result to `tools/assets/moon/launch-moon-{light,dark}.srgb.svg`.
  4. Run `/opt/homebrew/bin/rsvg-convert -w 1152 -h 1152 <svg> -o <png>` into `app/src/main/res/drawable-xxxhdpi/launch_moon.png` (light) and `app/src/main/res/drawable-night-xxxhdpi/launch_moon.png` (dark).
  5. Write `tools/assets/assets.sha256`: the pin line, a `# rsvg-convert <version>` line, then entries for the 2 iOS inputs (`<sha256>  Assets/moon/launch-moon-<theme>.svg  -`, verified at write time), the 2 `.srgb.svg` files and the 2 PNGs (`<sha256>  -  <local path>`).
- `build_assets.py check` (task 7) also runs `check-sha256-manifest.py tools/assets/assets.sha256` for the local entries. It never runs `rsvg-convert` and never reads the iOS repo.
- Makefile: `assets` = `python3 -I tools/assets/build_assets.py icons moon`; `assets-check` (new, `.PHONY`) = `python3 -I tools/assets/build_assets.py check`; add `GENERATOR_CHECKS += assets-check`. The Python tests already run in `make quick` through `Scripts/test_scripts.sh`.
- `docs/assets.md`, "Launch moon" section: why the stroke is substituted (F7: rsvg 2.63.4 renders `color(display-p3 …)` as nothing), the command, the sizes (288 dp = 1152 px at xxxhdpi; glyph 104 dp at offset 92; glow σ 9, α 0.45 baked; the ink fits the 192 dp circle), and the D10 rationale for sRGB (OQ-A7).
- Lint: `launch_moon` has only an xxxhdpi bitmap, which can raise `IconMissingDensityFolder`/`IconDensities`. Add path-scoped ignores (`<!-- 0b: one 1152 px master, plan A11 -->`) and an `UnusedResources` ignore for `launch_moon` and `launch_sky_top` (`<!-- 0b: consumed in Phase 3a -->`).
- Tests:
  - `LaunchMoonAssetTest` (JVM, `app/src/test/kotlin/net/chronoflow/app/LaunchMoonAssetTest.kt`, `javax.imageio.ImageIO`), one case per theme: 1152 × 1152 with alpha; pixel (0,0) alpha 0; pixel (576,420) within 2/255 of `E78A45FF` (light) / `F2AD73FF` (dark); no pixel with alpha > 0 farther than 384 px from (576,576); at least 1 % of pixels have alpha > 0 (this catches the blank-render trap).
  - In `test_build_assets.py`: substituting an SVG without a `display-p3` stroke fails; a P3 triple that differs from the report fails.

### Task 10: screenshots and phase record

Commit: `docs(assets): record launcher icon screenshots`. Ticks: none (the plan's acceptance item).

- The orchestrator runs this on the `pixel` emulator, because the worker sandbox may not reach `adb`. Install the debug build, then capture with `adb exec-out screencap -p`:
  - `docs/assets/launcher-icon.png`: the app drawer or home screen showing the icon;
  - `docs/assets/themed-icon.png`: the same with themed icons on;
  - `docs/assets/system-splash-default.png`: a cold start. Until 3a sets `Theme.App.Starting`, the system splash shows the **adaptive icon**, not `launch_moon` (OQ-0b-4).
- Link them from `docs/assets.md`. Add the new open questions (§11) to `plan.md` "Open questions for the user" as rows `OQ-0b-1` … `OQ-0b-9`. The orchestrator renumbers them at restack if needed.

---

## 8. Acceptance criteria

All commands run from the repo root with `ANDROID_HOME` set (AGENTS.md "Commands"). The worker runs what its sandbox allows and reports the exact output of anything it cannot run.

1. `make tokens && git diff --exit-code` passes: regeneration is byte-identical.
2. `make fonts && make assets && git diff --exit-code` passes on this Mac (rsvg-convert 2.63.4). If the PNG hashes differ from F7, stop and report; do not commit new PNGs.
3. `make generators-check` passes and, offline, needs neither the iOS repo nor `rsvg-convert`. A hand edit to any generated file makes it fail and name the file; the proof is recorded in `docs/tooling.md`.
4. `./gradlew :tools:tokengen:test` passes with 84/84 base hex, 16/16 derived, 33 + 33 ramp stops, every metric and type golden, and the determinism test. Each test is under 1 s (`build/test-results/*/timings.json` or the Gradle report).
5. `./gradlew :core:designsystem:testDebugUnitTest --tests '*FontHashTest'` and `./gradlew :app:testDebugUnitTest --tests '*AdaptiveIconTest' --tests '*LauncherIconInflationTest' --tests '*NotificationIconTest' --tests '*LaunchMoonAssetTest'` pass.
6. `make quick` passes (warm, < 60 s). `make check` passes with `pixel` booted: Lint is clean with warnings as errors, including `IconColors` and `MonochromeLauncherIcon`, and `generators-check` is included.
7. `./gradlew :app:assembleDebug` installs `net.chronoflow.app.debug`, which shows the ChronoFlow moon icon. The three screenshots of task 10 are in `docs/assets.md`.
8. `make size` still passes (≈ 0.6 MB of fonts and ≈ 0.2 MB of moon PNGs added).
9. `git -C /Users/rickgout/Sites/chronoflow-ios status --porcelain` and `git -C /Users/rickgout/Sites/chronoflow-ios rev-parse HEAD` are unchanged from before the phase (read-only proof).
10. There are 9 or 10 atomic Conventional Commits with no trailers, and the 7 `plan.md` checkboxes of Phase 0b are ticked in the commits named above.

There are no performance numbers to meet in this phase: nothing loads at startup, and the fonts stay unreferenced until Phase 2.

---

## 9. Shared-file etiquette (parallel wave 2)

- **Makefile:** add only the `tokens*`, `fonts*`, `icons`, `assets*` targets and one `GENERATORS +=` / `GENERATOR_CHECKS +=` line per target (task 5). Do not reformat other targets.
- **`lint.xml`:** add only `<ignore path="…"/>` entries under the issue ids named in tasks 6, 8 and 9, each with its phase comment. Never ignore an issue globally.
- **`plan.md`:** only the Phase 0b ticks and the OQ rows.

---

## 10. Review checklist (Opus reviewer)

Verify each point, citing a file and line or a failing test.

1. **Rounding:** hex bytes use `Math.round`/`roundToInt` (half up), never `kotlin.math.round` or `rint`. Alpha bytes follow F9 (`skyCopyBackground` = `0xE6F3F7FC`, not `0xE5…`).
2. **Ramp gamut:** the ramp is computed and checked in **sRGB** (33 stops), not copied from iOS's P3 path (17 stops). The mid stop is emitted exactly once.
3. **Clip before transfer, per channel.** No CSS gamut mapping (the RN culori `toGamut` is wrong here, `08 §3.2`).
4. **Locale:** no `String.format` or `toString()` path depends on the default locale. The determinism test sets `nl-NL`.
5. **Regex fidelity:** the patterns are verbatim from Swift. No `$` template accidents in raw strings. `captures` returns every match's group 1. The uniqueness and drift guards are fatal.
6. **Golden independence:** goldens come from the copied iOS files, never from Android output. `FontHashTest` hard-codes the hashes. No test "re-records" a golden.
7. **Names:** Swift type and function names are kept (`OKLab`, `RGB`, `DesignSource`, `skyRamp`, `tokenOutputs`, `writeOutputs`, `PlanUITokens`, `AuthTokens`, `SettingsTokens`, `TermsTokens`, `WelcomeTokens`, `PlanMetrics`, `PlanTypeMetrics`, `TypeMetrics`, `PressTokens`, `LaunchPaint`, `SkyRamp`, `SkyStop`), and so are the iOS test names listed in §7. The package is `net.chronoflow.core.designsystem.generated` / `net.chronoflow.core.plan.generated`. `native` appears in no package.
8. **Module rules:** `TimelinePalette` and `PlanMetrics` import nothing from `android.*` or Compose (Konsist). `CfColors` is a plain `@Immutable class`.
9. **erf precision:** `ErfTest` holds at 1e-15, and `LaunchPaint` matches iOS at 1e-9.
10. **Fonts:** byte-identical (hashes), lower-case names, 10 files including the Fraunces 500 cuts (OQ-A10), OFL texts present, no font referenced from code yet.
11. **Icons:** the vector values are parsed, not retyped: translate 341.6, scale 14.2, strokes 2.0 and 2.2, gradient offsets 0/0.6/1, colors from `colors.json`. The monochrome layer is single-color. The manifest sets `icon` and `roundIcon`. Inflation is tested.
12. **Launch moon:** the PNG is not blank (F7), is 1152², light and night differ, and the substituted SVGs are committed and hashed. `generators-check` never rasterizes.
13. **Read-only sources:** the scripts use only `git show`. Nothing writes under `chronoflow-ios`.
14. **Lint ignores** are path-scoped and name the consuming phase. There are no blanket suppressions. 0a's `MissingApplicationIcon` ignore and its `docs/tooling.md` row are gone (task 7).
15. **Makefile wiring:** only `+=` lines on `GENERATORS`/`GENERATOR_CHECKS` and the 0b targets changed; `generators-check` runs `tokens-check`, `fonts-check` and `assets-check`; the Python tests live in `Scripts/tests/` so `Scripts/test_scripts.sh` finds them.
16. **Tests rules:** cheapest layer (JVM; one Robolectric test), no `Thread.sleep`, budgets respected, parameterized cases instead of loops that hide which token failed.
17. **Scope:** no `TextStyle`, `FontFamily`, `MaterialTheme`, splash theme or UI was added (those belong to Phases 2 and 3a).

---

## 11. Open questions (proceed on the default; add them to plan.md in task 10)

| # | Question | Default | Why |
|---|---|---|---|
| OQ-0b-1 | Where does `PlanMetrics` live? `08 §3.1` lists it inside `CfDimens` (plan A9 does not name it), but its consumer `:core:plan` is a JVM module that cannot depend on `:core:designsystem`. | `core/plan/…/generated/PlanMetrics.generated.kt` (Double, dp), as iOS keeps it in `PlanCore`. | A3 module rules |
| OQ-0b-2 | Port all iOS TokenGen outputs now (Auth, Settings, Terms, Welcome, PlanUI, Press, LaunchPaint, TypeMetrics), or only colors, sky and type? | All of them now. | "Values only from generated tokens"; Phases 2, 3a, 3c, 5a, 9 and 10 need them, and the iOS files are exact goldens. |
| OQ-0b-3 | The iOS moon SVGs use `color(display-p3 …)`, which rsvg-convert 2.63.4 renders blank. | Substitute the sRGB `dawn` hex (`#E78A45` / `#F2AD73`) into a committed copy, then rasterize. | F7; sRGB-only per OQ-A7 |
| OQ-0b-4 | The plan's acceptance asks for the moon "on the system splash", but the splash theme that shows `launch_moon` is Phase 3a. | 0b records the launcher icon and the default system splash (adaptive icon); the `launch_moon` splash screenshot is 3a's acceptance. | No splash theme before 3a |
| OQ-0b-5 | Dimension values as raw `Float` or Compose `Dp`/`TextUnit`? | Raw `const val Float` in dp or sp as named. Phase 2 wraps them. | Several iOS `CGFloat` metrics are font sizes, so one Compose unit type would be wrong for some. |
| OQ-0b-6 | Generate the token `screenDayTitle` (18/28) next to `planDayTitle` (18/26)? | Yes, as iOS does. The day header uses `planDayTitle` (OQ-A9). | Parity of the generated set |
| OQ-0b-7 | Android-only derived colors. | Generate `systemSelected` (needed by `toM3()`) and `wizardPickerFill` (iOS emits it). Do not generate `pickerSelected`, `glass*` or `glassBorder`. | No glass (OQ-A6); no invention |
| OQ-0b-8 | Open-source licences screen for the OFL fonts. | Ship the OFL texts in `assets/licenses/`; no screen until a board exists. | No board |
| OQ-0b-9 | Unused-resource Lint findings for outputs consumed later (fonts, `launch_moon`, `launch_sky_top`, `ic_stat_moon`). | Path-scoped ignores, each naming its consuming phase, removed by that phase. | `warningsAsErrors` (A20) |
