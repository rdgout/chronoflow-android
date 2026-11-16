# 08 · Design system → Android (tokens, type, theming, system chrome, icon)

Research for the native Android app (Kotlin + Jetpack Compose). Written 2026-11-15.

Legend: **[F]** = fact verified in a source file (path given). **[R]** = recommendation for Android. **[OQ n]** = open question, with the default the plan should use (section 13).

All sources were read only. Nothing was built, generated or modified in `chronoflow-ios`, `chronoflow-new` or `chronoflow-mobile`. The only executables run were throwaway Python scripts in the session scratchpad (OKLCH maths, contrast, font inspection through the iOS repo's existing fonttools venv with `-I -B`, and `javap` on cached Compose AARs in `~/.gradle`).

---

## 0. Short answer: what the iOS work gives us for free

The iOS repo has already solved most of this topic. These items can be reused directly or ported one to one:

| # | iOS asset | Path | How Android reuses it |
|---|---|---|---|
| 1 | **OKLCH → sRGB/P3 algorithm** (CSS Color 4 matrices, clip, sRGB transfer) | `chronoflow-ios/Tools/TokenGen/Sources/TokenGenCore/ColorMath.swift` | Port to Kotlin (section 3.2). I re-implemented it independently and it reproduces **all 84 light/dark hex values** in the iOS report with 0 mismatches. |
| 2 | **Golden values** for every colour token (sRGB hex + P3 floats, light and dark) | `chronoflow-ios/Tools/TokenGen/TokenGenReport.md` | Copy it as a test fixture: the Android generator's output must equal it. `tokens.json` and `theme.css` are byte-identical between the iOS and Android `_design` copies (checked with `diff`). |
| 3 | **Derived colours** (OKLab mixes and alpha variants taken from boards: auth error banner, settings warning, wizard notice, QR white, shadow colour) | `Tools/TokenGen/Sources/TokenGenCore/Source.swift:66-113` | Port the same derivation list. Values are in section 4.2. |
| 4 | **Sky ramp algorithm**: per-segment OKLab sampling with an exact stop at the mid point, plus stops added until linear interpolation is within ±0.25/255 | `ColorMath.swift:83-106`, `CfSky.swift` | Same algorithm with sRGB output: 33 stops per theme (section 6). |
| 5 | **Fonts**: 10 static TTFs cut from pinned Google Fonts variable sources (Figtree 400/500/600/700; Fraunces 400/500 × opsz 20/32/48, SOFT 0, WONK 1), with SHA-256 manifest and OFL licences | `Packages/ChronoFlowKit/Sources/DesignSystem/Resources/Fonts/`, `Tools/generate_fonts.py`, `Tools/fonts.sh` | Copy the TTFs into `res/font/` under lower-case names, or port the script with an Android output directory. The bytes and hashes stay the same (section 5.1). |
| 6 | **Opsz selection rule**: display size ≥ 40 → opsz 48, ≥ 26 → opsz 32, otherwise opsz 20 | `Tools/TokenGen/Sources/TokenGenCore/PlanUITokens.swift:31-33` | Same rule in the Kotlin type generator. |
| 7 | **Hand-curated board values** as JSON: plan type scale and metrics, wizard/plan UI scalars, settings scalars | `Tools/TokenGen/Plan.source.json`, `PlanUI.source.json`, `Settings.source.json`, `plan-tokens.json` | Copy as generator inputs. Board measurements and design-note decisions are already settled there. |
| 8 | **Press feedback values** from `theme.css` (`cf-press`): scale 0.97, opacity 0.85, 120 ms, `cubic-bezier(0.25,0.1,0.25,1)`. Reduce Motion: opacity only. The generator asserts these against `theme.css`. | `Generator.swift:83-95`, `PressStyle.swift` | Same constants; Compose `Modifier.Node` implementation (section 9.2). |
| 9 | **Policy decisions already taken with the user**: native components over look-alikes and designed surfaces stay as drawn (D1b); Dynamic Type capped at xxLarge (OQ 10, 2026-11-06); plain sky with no dot pattern (2026-11-06); system-font tab and settings titles (2026-11-10); opaque compact line on the trip page; colour is never the only signal | `chronoflow-ios/plan.md` D1b, l.59-66, l.186, l.901-907; `AGENTS.md` | Carry them over as Android rules (sections 7-10). |
| 10 | **Which native control gets which tint** (switch → dawn, pickers → dawnText, tabs → primary, progress → dawn) | `grep .tint(` across `App/` and `Packages/ChronoFlowKit/Sources` | Feeds the system-component table (section 10). One mismatch with the boards is listed as OQ 3. |
| 11 | **Launch colours and moon**: LaunchSky = `sky-top` (light #1F285A, dark #06091C); 288 pt moon canvas with a 104 pt glyph and glow σ 9 α 0.45 | `plan.md` D7, Phase 1; `Assets/moon/launch-moon-{light,dark}.svg` | Android 12 splash: 288 dp icon canvas, 192 dp visible circle (section 11.4). |

What iOS does **not** cover: the Material 3 mapping, Android glass/blur limits, the adaptive and themed icon, the font-scale cap mechanics, ripple versus press, and Android haptics. Those are in sections 7-11.

---

## 1. Sources read

- `_design/canvas/ds/chronoflow/tokens.json` (colour, type, spacing, radius, shadow), `README.md` (principles, colour, type, shape, shell, timeline, motion, a11y), `components/bundle.css` and `bundle.js` (Tailwind output and React components).
- `_design/canvas/theme.css` (CSS variables, `cf-glass`, `cf-glass-bar`, `cf-press`, `cf-sky`, `.ios-*` system chrome).
- `_design/canvas/canvas.json` notes: `dirNative`, `reqSystemChrome`, `planCompactNote`, `planCompactQuestions`, `reqWideTrip`, `wizTodo1/3/7`, `wiz4Rick`. Boards: `AppIcon`, `SplashSpec`, `WizardCopy`, `WizardSpec`, `Today`, `Plan`, `PickerDateTime`, `PickerTime`, `SheetStates`, `TripDelete`, `PlanDayMenu`.
- `_design/canvas/icon/` (`colors.json`, `layers/*.svg`, `android/adaptive-{background,foreground,monochrome}.{svg,png}`).
- iOS: `AGENTS.md`, `CLAUDE.md`, `plan.md` (Decisions, Navigation titles, Tokens, Typography, Reduce Motion, Accessibility, Auth geometry, Phase 1, plain-sky decision), `Tools/TokenGen/**`, `Tools/generate_fonts.py`, `Tools/test_fonts.py`, `Tools/fonts.sh`, `DesignSystem/{Typography,ContentSizeCap,PressStyle,CfSky,DesignSystem,FontRegistry}.swift`, `App/AppTabs.swift`, `PlanUI/TripPage/{PlanAppearance,TripPageView,DayHeader}.swift`, `Docs/settings-board-fidelity.md`.
- RN: `chronoflow-mobile/plan.md` Phases 15/16/21/25 (native tabs, controls, splash, compact page), `scripts/build-theme.ts`, `src/theme/fonts.ts`, `android/app/src/main/res/**`.
- Toolchain: cached Compose artifacts on this Mac, `~/.gradle/caches/modules-2/files-2.1/androidx.compose.ui/*/1.12.1` and `androidx.compose.material3/*/1.5.0-beta01`, inspected with `javap` to confirm the APIs named below exist.

---

## 2. Design-system facts

### 2.1 Principles that constrain Android choices [F `README.md`]

- "Calm over clever", "`shadow-soft` is the only elevation. No hard or stacked shadows."
- "Colour is never the only signal": every plan category has an icon plus a pattern (hatch or dots) or a dashed border.
- "Times are the content": tabular numerals (`tnum`) for every time, duration, date and count that lines up.
- "Dawn means 'you' and 'now'". `now` is used only for the timeline now line; `destructive` only for errors and delete. "Nothing else is red."
- Both themes are first class; dark is "night sky", not inverted light.
- Motion: "Subtle colour and opacity transitions, sheet and dialog slide/fade, a spinner. No parallax, no springs. `prefers-reduced-motion` turns everything off except the spinner."
- A11y: WCAG AA in both themes, visible focus (2 px `ring`), touch targets ≥ 44 px. Known gaps kept on purpose: `input` borders are under 3:1; `cat-dark` is close to `background` in dark; `cat-light` and `cat-moderate` are close to `background` in light.
- Icons: lucide, stroke only. 16 px in UI, 20 px in the tab bar, 14 px in dense blocks.
- Logo: "Use the file as supplied; never redraw or recolour it, and keep it off `bg-sky`."

### 2.2 Native-components direction [F `canvas.json` notes `dirNative`, `reqSystemChrome` (2 Nov)]

- "The app now uses the platform's own components wherever iOS **and Android** provide one … The tab bar is the system's: … **Material on Android**. We set its icons, labels and the amber selected colour; its shape, size, position and blur are the OS's."
- "Switches, segmented controls, the date and time picker and confirmation alerts are the system's, tinted amber where the OS allows it." Sheets are native, with the OS's corners, grabber and detents.
- Segmented control: "On iOS the selected segment stays system grey … **on Android it takes our colours**."
- "What stays ours: anything designed as content, such as cards, the timeline, the trip actions sheet, the Preferences pills, answer lists, type, colour and icons."
- `reqSystemChrome` ends with "Done 2 Nov (iOS drawn; **Android Material not drawn yet**)". **No board shows Android chrome**, so the mapping in section 10 is derived, not drawn (OQ 1).

### 2.3 Android-specific rules already on the canvas [F]

- `WizardCopy.dc.html` → "Android":
  - "× on the right, as on iOS: Rick's rule is one place for Close."
  - "Date and time pills have no compact form: a tap opens the Material date or time dialog."
  - "Preparation days: drawn − and + buttons (48 dp), no native stepper."
  - "Progress: the native determinate linear progress indicator, styled like the 4 pt bar."
  - "Pickers follow the app's language and the phone's 12/24-hour setting."
  - Layout: "Android tablets stay portrait for now."
- `wiz4Rick` (Done 4 Nov): "Fine-tune and Android are fine" (× on the right confirmed).
- `planCompactQuestions` #5: the day menu is "the system menu (SwiftUI on iOS, Compose on Android) … Android: Compose DropdownMenu with the same two-line items." The menu title is "Go to day" / "Ga naar dag", with a checkmark in front of the current day.
- `SplashSpec.dc.html` → "Android 12+: The system splash shows the moon (as the splash icon) on the night-sky colour. Keep it on screen until the first frame is drawn, then play the same animation in the app."
- `AppIcon.dc.html` → "Android · adaptive icon: Foreground and background are separate 1024 layers on a 108 dp canvas; launchers show the middle 72 dp in their own shape. The moon is drawn smaller here so it looks the same size as on iOS. The monochrome layer is used for themed icons (Android 13+)."
- RN plan l.1460 (settled by Rick, 5 Nov): tab-bar minimise is iOS 26 only; "**Android keeps its full Material bar**."

### 2.4 Token inventory [F `tokens.json`, `theme.css`]

- **Colour**: 42 named colour tokens plus 2 chart and 8 sidebar aliases. Every value is `oklch(L C H [/ A])` with fractional L, light and dark. Aliases use `{name}`. The full table with sRGB values is in section 4.1.
- **Sky gradient**: `sky-top` 0 %, `sky-mid` at **55 % (light) / 60 % (dark)** (`theme.css` `--sky-mid-stop`, `bundle.css .bg-sky`), `sky-horizon` 100 %. CSS interpolates in OKLab because the stops are not legacy colours (iOS plan.md l.159).
- **Type** (section 5.3): Display group in Fraunces (hero 48/1, page-title 30/36, page-title-lg 36/40, card-title 24/32, section-title 20/28, day-title 18/28; all 400 at −0.01em). Text group in Figtree (body-lg 16/24, body 14/20, label 14/20 600, meta 12/16, eyebrow 12/16 600 +0.1em uppercase, block-label 11/1.25 600, priority-tag 10/14 500).
- **Spacing**: 4 px grid. `space-1` 4, `-2` 8, `-3` 12, `-4` 16 (page gutter), `-5` 20 (card padding on phones), `-6` 24, `-8` 32, `-14` 56 (header height, Today row min height), `-16` 64 (tab bar).
- **Radius**: base 14. `sm` 10, `md` 12, `lg` 14, `xl` 18, `2xl` 22, `3xl` 26, `full` 9999. "Corners never go square."
- **Shadow**: `shadow-soft` light `0 10px 30px -12px oklch(0.3 0.08 272 / 0.25)`, dark `0 12px 40px -14px oklch(0 0 0 / 0.6)`.
- **Glass** (`theme.css`):
  - `.cf-glass-bar` = `background` 72 % mixed with transparent in OKLab, `backdrop-filter: blur(20px) saturate(1.8)`, bottom border `border` 70 %.
  - `.cf-glass` = `card` 72 %, `blur(24px) saturate(1.8)`, border `border` 70 %, `shadow-soft`.
  - The boards use the same 20 px blur recipe inline in TripReview, TripEditor, TripAirportSearch, the Guest* boards and LiveActivity (24 px).
- **Press** (`.cf-press`): `:active { transform: scale(0.97); opacity: 0.85 }`, `transition: transform 120ms ease, opacity 120ms ease`. Reduced motion: no transition and no transform.
- **Patterns** (`bundle.css`):
  - `pattern-hatch` = `repeating-linear-gradient(135deg, currentColor 12 % 0 2px, transparent 2px 8px)`.
  - `pattern-dots` = `radial-gradient(currentColor 16 % 1px, transparent 1.5px)` on a 7 px grid.
  - `outline-dashed-cat` = 2 px dashed.
  - The user removed **sky** dots on 2026-11-06 (iOS plan l.901). **Category** patterns remain (iOS `PlanAppearance.swift:10-31` still draws dots and hatch).

### 2.5 System chrome drawn on the boards [F `theme.css` "System (iOS 26) components, drawn as the OS shows them. Not ours to restyle."; class counts across `*.dc.html`]

| Class | Count | Boards |
|---|---|---|
| `ios-glass` | 55 | most shell, settings, plan, alert boards |
| `ios-navbar` (54 px, 3-column grid) | 27 | Plan, PlanRemote(+Wide), PlanWide, PolicyReader, all Set* pages, Today, Trips, Wizard |
| `ios-largetitle` (Fraunces 34/41) | 22 | Today, Trips, SetHome(+NL), all Set* pages, PolicyReader |
| `ios-back` (glass pill with previous title) | 20 | PlanRemote(+Wide), PolicyReader, Set* subpages |
| `ios-tab` / `ios-tabbar` (floating glass, 62 px, 3 columns, inset 20 px, 22 px from bottom) | 18 / 6 | Today, Trips, SetHome(+NL), Plan, PlanRemote |
| `ios-alert-btn` / `ios-alert` (300 px wide, 34 radius) | 14 / 7 | PasswordPrompt(+Wide), SheetStates, TripDelete, TripDiscard, Wizard |
| `ios-navbtn` (44 × 44 glass circle) | 10 | Plan, PlanWide, Wizard … |
| `ios-switch` (51 × 31, amber `dawn` track when on) | 7 | SetReminders, SetPreferences, TripEditor, Wizard |
| `ios-navtitle` | 5 | |
| `ios-seg` (36 px, grey track, white selected) | 2 | CalendarExport, Wizard |
| `ios-rail` | 0 | defined only |

Other system UI drawn without an `ios-` class:
- Sheet grabbers (`width: 36px; height: 5px`): CalendarExport, GuestSignup, GuestStates, PasswordPrompt, ItemDetail, PickerTime, PickerDateTime, ReviewAnswer, ReviewStates, SetPaywall.
- The calendar grid and time field: PickerDateTime.
- Wheels: PickerTime ("Steps of 5 minutes").
- The day menu: PlanDayMenu.
- Lock screen and Dynamic Island: LiveActivity.

### 2.6 What iOS actually ships for system controls [F]

- Tabs: SwiftUI `TabView`, SF Symbols `square.grid.2x2` / `airplane` / `gearshape`, **`.tint(ColorToken.primary)`** (`App/AppTabs.swift:143-168`). `tabBarMinimizeBehavior(.onScrollDown)` on iOS 26 only.
- Titles: plain system large titles in system font, bold, standard size (plan.md l.59-66, user 2026-11-10). This **supersedes** the boards' Fraunces `ios-largetitle`.
- Switches: native `Toggle` tinted `dawn` (e.g. `SettingsBodyClockView.swift:235`, `MemberWizardFineTuneView.swift:122`).
- Segmented: `Picker(.segmented)` (wizard stops) in system appearance.
- Date pickers: native `DatePicker` tinted `dawnText` (`WizardFlightDateTimeRow.swift:113`). The bedtime picker uses `.wheel` (`SettingsClockPicker.swift:26`).
- Day progress bar: `ProgressView(.linear).tint(dawn)` (`TodayPanelComponents.swift:54`).
- Alerts: `.alert` (`SettingsPasswordPrompt.swift`, `MemberWizardView.swift`). Sheets: `presentationDetents` (11 files).
- Glass: only one explicit `glassEffect`, on the wizard swap button behind `#available(iOS 26)`. Designed surfaces stay opaque: the compact day line on the trip page is `.background(ColorToken.card.color)` (`TripPageView.swift:367-368`), and DayHeader uses `background`.
- Haptics: `sensoryFeedback` in 7 wizard files, following WizardSpec "Haptics".
- Tabular numerals: `monospacedDigit()` in 22 files.

---

## 3. Token generator for Android

### 3.1 Shape of the tool [R]

- A JVM-only Gradle module `:tools:tokengen` (Kotlin, `application` plugin, no Android dependency). It reads `_design/canvas/ds/chronoflow/tokens.json`, `_design/canvas/theme.css`, the few boards iOS already parses (`Wizard.dc.html`, `Set2FASetup.dc.html`, `TodayRowStates.dc.html`, `Main.dc.html`, `SplashScreen.dc.html`) and the copied iOS inputs `tools/tokengen/{Plan,PlanUI,Settings}.source.json`.
- Tasks:
  - `./gradlew :tools:tokengen:run` writes the outputs.
  - `./gradlew :tools:tokengen:verifyTokens` regenerates in memory and fails if a committed output is stale (the iOS `--check` equivalent). Wire it into `check`.
- **Outputs are committed**, as on iOS (D10), so app builds never depend on the generator:
  - `core/designsystem/src/main/kotlin/<pkg>/designsystem/generated/CfColors.generated.kt`: `CfColors` class, `CfLightColors`, `CfDarkColors`.
  - `…/generated/CfSky.generated.kt`: sky ramps.
  - `…/generated/CfTypeScale.generated.kt`: text-style metrics (sizes, line heights, tracking in em, face).
  - `…/generated/CfDimens.generated.kt`: spacing, radii, `PlanMetrics`, `PlanUiTokens`, `PressTokens`, shadows.
  - `app/src/main/res/values/colors_launch.xml` and `values-night/colors_launch.xml`: `launch_sky` (= `sky-top`) for the splash theme and the adaptive-icon fallback.
  - `tools/tokengen/TokenGenReport.md`: the same table format as iOS, so the two reports can be diffed.
- Validation rules carried over from iOS `Source.swift`:
  1. Every token resolves, without cycles, in both themes.
  2. `tokens.json` and the `theme.css` `:root` / `[data-theme="dark"]` blocks agree token by token (regex `--([a-z0-9-]+):\s*([^;]+);`, following `var(--x)`).
  3. `--sky-mid-stop` is exactly 55 % / 60 %.
  4. `cf-press` is exactly scale 0.97, opacity 0.85, 120 ms, and its Reduce Motion rules are present.
  5. Any board regex miss fails the run ("board differs from plan").
- Unit tests:
  1. Every generated sRGB hex equals the iOS `TokenGenReport.md` value (copy the report to `tools/tokengen/src/test/resources/ios-TokenGenReport.md`).
  2. The ramp matches true OKLab within ±0.25/255 at 1001 points for both themes.
  3. Derived mixes match the iOS rows (`authErrorBackground`, `settingsWarningBackground` …).
  4. Generation is deterministic (stable ordering, fixed number formatting).

### 3.2 Exact conversion algorithm [F iOS `ColorMath.swift`, re-verified; R for Kotlin]

Input `oklch(L C H [/ A])`. L is a fraction 0–1; the tokens never use `%`.

```
a = C · cos(H · π/180)
b = C · sin(H · π/180)
l = (L + 0.3963377774·a + 0.2158037573·b)³
m = (L − 0.1055613458·a − 0.0638541728·b)³
s = (L − 0.0894841775·a − 1.2914855480·b)³
R_lin =  4.0767416621·l − 3.3077115913·m + 0.2309699292·s
G_lin = −1.2684380046·l + 2.6097574011·m − 0.3413193965·s
B_lin = −0.0041960863·l − 0.7034186147·m + 1.7076147010·s          // linear sRGB

sRGB output:   c = clamp(c_lin, 0, 1)
               enc(c) = c ≤ 0.0031308 ? 12.92·c : 1.055·c^(1/2.4) − 0.055
               byte = round(enc(c) · 255)                            // ARGB hex; alpha = round(A · 255)

Display P3:    P_lin = [[0.8224619687, 0.1775380313, 0],
                        [0.0331941989, 0.9668058011, 0],
                        [0.0170826307, 0.0723974407, 0.9105199286]] · (R_lin, G_lin, B_lin)
               then the same clamp and the same transfer curve (P3 uses the sRGB curve)
```

- The gamut strategy is **per-channel clip**, the same as iOS. The RN app used culori `toGamut('rgb','oklch')`, which reduces chroma (CSS gamut mapping) (`chronoflow-mobile/scripts/build-theme.ts:31`). Do **not** copy RN here; clip keeps parity with iOS.
- Out-of-sRGB tokens (clipped; all tiny):

  | Token | Theme | Linear sRGB overshoot | In P3? |
  |---|---|---|---|
  | `card`, `popover` | light | B 1.0097 | no |
  | `primary-foreground`, `sky-title` | light | B 1.0013 | yes |
  | `cat-sleep-fg` | both | B 1.0043 | yes |
  | `cat-light-fg` | light, dark | B −0.0010 / −0.0006 | yes |
  | `cat-travel` | light | **R −0.0170** (largest) | yes |
  | `cat-travel-fg` | dark | R −0.0005 | yes |

- `color-mix(in oklab, X p%, transparent)` equals X with alpha p, because CSS mixing is premultiplied. Emit it as `X.copy(alpha = p)`.
- `color-mix(in oklab, A p%, B)` with an opaque B is a **true OKLab interpolation**: `lab = A·p + B·(1−p)` per component, then convert. It must be **precomputed by the generator**. Runtime alpha blending in sRGB gives a different colour.

### 3.3 sRGB or Display P3 at runtime [R]

- **Ship sRGB** (`Color(0xAARRGGBB)`, packed 8-bit, cheapest path).
- Android only shows P3 if the window sets `colorMode = COLOR_MODE_WIDE_COLOR_GAMUT`. Android documents that this raises GPU memory and bandwidth use, and the timeline is the performance-critical screen.
- The palette's sRGB/P3 difference is limited to the clipped channels in 3.2, so in practice it cannot be seen.
- Also emit the P3 floats in the report, and behind a generator flag as `Color(r, g, b, a, ColorSpaces.DisplayP3)`, for a later experiment. **[OQ 6]**

### 3.4 Generated Kotlin shape [R]

```kotlin
// Generated by :tools:tokengen from _design/canvas/ds/chronoflow/tokens.json. Do not edit.
package net.chronoflow.designsystem.generated

@Immutable
class CfColors(
    val isDark: Boolean,
    val background: Color, val foreground: Color,
    val card: Color, val cardForeground: Color,
    val popover: Color, val popoverForeground: Color,
    val surface2: Color,
    val primary: Color, val primaryForeground: Color,
    val secondary: Color, val secondaryForeground: Color,
    val muted: Color, val mutedForeground: Color,
    val accent: Color, val accentForeground: Color,
    val destructive: Color, val destructiveForeground: Color,
    val border: Color, val input: Color, val ring: Color,
    val dawn: Color, val dawnText: Color, val dawnForeground: Color, val now: Color,
    val catSleep: Color, val catSleepFg: Color, val catLight: Color, val catLightFg: Color,
    val catModerate: Color, val catModerateFg: Color, val catDark: Color, val catDarkFg: Color,
    val catCaffeine: Color, val catCaffeineFg: Color, val catTravel: Color, val catTravelFg: Color,
    val skyTop: Color, val skyMid: Color, val skyHorizon: Color,
    val skyTitle: Color, val skyCopy: Color, val skyCopyBackground: Color,
    // derived (section 4.2)
    val authErrorBackground: Color, val authErrorBorder: Color,
    val warningBackground: Color, val warningBorder: Color,
    val qrBackground: Color, val shadowSoft: Color,
    val systemSelected: Color,          // mix(card, dawn, 0.16), Android only, see 9.1
    val glassBar: Color, val glass: Color, val glassBorder: Color,
)

val CfLightColors = CfColors(isDark = false, background = Color(0xFFF3F7FC), /* … */ skyCopyBackground = Color(0xE6F3F7FC), /* … */)
val CfDarkColors  = CfColors(isDark = true,  background = Color(0xFF0A0E1F), /* … */)
```

Use a plain `class` with `@Immutable`, not a `data class`, so a large `equals` isn't generated on recomposition checks. Theme switching swaps the whole instance.

---

## 4. Colour values

### 4.1 Token table (sRGB = output of 3.2; identical to iOS `TokenGenReport.md`) [F, computed and cross-checked]

| Token (json) | Kotlin | Light OKLCH | Light sRGB | Dark OKLCH | Dark sRGB |
|---|---|---|---|---|---|
| `background` | `background` | (0.975 0.008 250) | `#F3F7FC` | (0.17 0.035 272) | `#0A0E1F` |
| `foreground` | `foreground` | (0.22 0.05 270) | `#121932` | (0.94 0.015 260) | `#E5ECF6` |
| `card` | `card` | (0.995 0.004 250) | `#FBFEFF` | (0.21 0.04 272) | `#12172B` |
| `card-foreground` | `cardForeground` | (0.22 0.05 270) | `#121932` | (0.94 0.015 260) | `#E5ECF6` |
| `popover` | `popover` | (0.995 0.004 250) | `#FBFEFF` | (0.23 0.045 272) | `#161B32` |
| `popover-foreground` | `popoverForeground` | (0.22 0.05 270) | `#121932` | (0.94 0.015 260) | `#E5ECF6` |
| `surface-2` | `surface2` | (0.955 0.012 255) | `#EBF1F8` | (0.19 0.038 272) | `#0E1225` |
| `primary` | `primary` | (0.32 0.09 272) | `#242E60` | (0.8 0.11 60) | `#F2AD73` |
| `primary-foreground` | `primaryForeground` | (0.98 0.01 250) | `#F4F9FF` | (0.2 0.04 272) | `#101428` |
| `secondary` | `secondary` | (0.93 0.018 255) | `#E0E9F4` | (0.27 0.045 272) | `#1F253C` |
| `secondary-foreground` | `secondaryForeground` | (0.26 0.06 270) | `#192241` | (0.92 0.015 260) | `#DFE5EF` |
| `muted` | `muted` | (0.94 0.012 255) | `#E6ECF3` | (0.25 0.04 272) | `#1B2035` |
| `muted-foreground` | `mutedForeground` | (0.34 0.04 265) | `#2E384D` | (0.74 0.03 262) | `#A1ABBE` |
| `accent` | `accent` | (0.92 0.03 60) | `#F4E0D1` | (0.3 0.05 280) | `#292B46` |
| `accent-foreground` | `accentForeground` | (0.3 0.06 45) | `#462312` | (0.94 0.015 260) | `#E5ECF6` |
| `destructive` | `destructive` | (0.55 0.2 27) | `#CC2827` | (0.68 0.17 25) | `#EF6661` |
| `destructive-foreground` | `destructiveForeground` | (0.98 0 0) | `#F8F8F8` | (0.15 0.03 272) | `#070A18` |
| `border` | `border` | (0.88 0.02 260) | `#D0D8E5` | (0.32 0.04 272) | `#2C3247` |
| `input` | `input` | (0.86 0.02 260) | `#C9D2DE` | (0.34 0.04 272) | `#31374D` |
| `ring` | `ring` | (0.55 0.12 272) | `#596BB7` | (0.8 0.11 60) | `#F2AD73` |
| `dawn` | `dawn` | (0.72 0.14 55) | `#E78A45` | (0.8 0.11 60) | `#F2AD73` |
| `dawn-text` | `dawnText` | (0.53 0.13 55) | `#A35303` | (0.8 0.11 60) | `#F2AD73` |
| `dawn-foreground` | `dawnForeground` | (0.22 0.05 45) | `#2D1205` | (0.2 0.04 272) | `#101428` |
| `now` | `now` | (0.6 0.19 30) | `#DA4433` | (0.74 0.16 35) | `#FF8162` |
| `cat-sleep` | `catSleep` | (0.42 0.12 275) | `#3C448D` | (0.5 0.13 278) | `#5459AB` |
| `cat-sleep-fg` | `catSleepFg` | (0.98 0.01 270) | `#F6F8FF` | (0.98 0.01 270) | `#F6F8FF` |
| `cat-light` | `catLight` | (0.85 0.15 85) | `#FAC547` | (0.86 0.14 88) | `#F7CB58` |
| `cat-light-fg` | `catLightFg` | (0.28 0.07 60) | `#411F00` | (0.24 0.06 60) | `#331700` |
| `cat-moderate` | `catModerate` | (0.93 0.06 90) | `#F7E7BB` | (0.72 0.07 90) | `#B6A372` |
| `cat-moderate-fg` | `catModerateFg` | (0.32 0.06 70) | `#462D0B` | (0.2 0.04 70) | `#211201` |
| `cat-dark` | `catDark` | (0.36 0.03 260) | `#343E4D` | (0.3 0.025 265) | `#282E3B` |
| `cat-dark-fg` | `catDarkFg` | (0.97 0.01 260) | `#F1F5FC` | (0.95 0.01 260) | `#EBEFF5` |
| `cat-caffeine` | `catCaffeine` | (0.5 0.08 50) | `#885538` | (0.6 0.08 55) | `#A67351` |
| `cat-caffeine-fg` | `catCaffeineFg` | (0.98 0.01 60) | `#FEF7F2` | (0.15 0.03 50) | `#150702` |
| `cat-travel` | `catTravel` | (0.52 0.1 200) | `#007980` | (0.66 0.1 200) | `#30A4AA` |
| `cat-travel-fg` | `catTravelFg` | (0.99 0.01 200) | `#F4FEFE` | (0.15 0.03 200) | `#000F10` |
| `sky-top` | `skyTop` | (0.3 0.09 272) | `#1F285A` | (0.15 0.04 272) | `#06091C` |
| `sky-mid` | `skyMid` | (0.42 0.1 290) | `#4D427F` | (0.25 0.07 290) | `#221A40` |
| `sky-horizon` | `skyHorizon` | (0.75 0.12 50) | `#EB9666` | (0.55 0.12 45) | `#AA5830` |
| `sky-title` | `skyTitle` | = primary-foreground | `#F4F9FF` | = foreground | `#E5ECF6` |
| `sky-copy` | `skyCopy` | = foreground | `#121932` | = foreground | `#E5ECF6` |
| `sky-copy-background` | `skyCopyBackground` | (0.975 0.008 250 / 0.9) | `#F3F7FC` α0.9 | (0.17 0.035 272 / 0.9) | `#0A0E1F` α0.9 |

The `chart-1..3` and `sidebar-*` aliases are not generated. No sidebar or chart ships.

Inside `cf-sky`, CSS redefines `--dawn-text: var(--dawn)`. iOS deliberately keeps base `dawnText` for card text inside the sky (plan.md l.155-156). Android does the same: `CfSkyScope` provides `dawnText = dawn` only to text drawn directly on the sky (eyebrow, CTA), not to cards placed on it.

### 4.2 Derived colours [F iOS `Source.swift`; values computed]

| Name | Rule | Light | Dark | Used by |
|---|---|---|---|---|
| `authErrorBackground` | mix(card, destructive, 0.10) | `#FAEAEA` | `#262031` | auth banner, M3 `errorContainer` |
| `authErrorBorder` | destructive α0.35 | `#CC2827` α.35 | `#EF6661` α.35 | auth banner border |
| `warningBackground` | mix(card, dawn, 0.10) (= `wizard-notice-background`, read from `Wizard.dc.html`) | `#FBF2EE` | `#242433` | settings and wizard notices |
| `warningBorder` | dawn α0.4 | `#E78A45` α.4 | `#F2AD73` α.4 | same |
| `qrBackground` | `oklch(1 0 0)` from `Set2FASetup.dc.html` | `#FFFFFF` | `#FFFFFF` | 2FA QR tile |
| `shadowSoft` | colour from `shadow-soft` | `#202955` α.25 | `#000000` α.6 | section 7 |
| `systemSelected` **(new)** | mix(card, dawn, 0.16) | `#FAECE3` | `#302D38` | M3 selected containers (9.1) |
| `pickerSelected` **(new)** | mix(card, dawn, 0.22) (PickerDateTime: `color-mix(dawn 22%, transparent)` over the card) | `#F9E5D9` | `#3C353D` | date picker today/selected tint if needed |
| `glassBar` | background α0.72 | `#F3F7FC` α.72 | `#0A0E1F` α.72 | not used by default (section 8) |
| `glass` | card α0.72 | `#FBFEFF` α.72 | `#12172B` α.72 | not used by default |
| `glassBorder` | border α0.70 | `#D0D8E5` α.7 | `#2C3247` α.7 | hairlines under bars |

### 4.3 Contrast facts used in the mapping (computed, WCAG 2 with 8-bit rounding)

- `dawnText` on `background` / `card` / `popover`: light 5.14 / 5.46 / 5.46; dark 10.04 / 9.30 / 8.89.
- `dawnText` on `systemSelected`: light 4.79, dark 7.06. `foreground` on `systemSelected`: 15.01 / 11.33.
- `primaryForeground` on `dawnText` (light): 5.23. `dawnForeground` on `dawn`: 6.76 / 9.54.
- `dawn` on `background` (light): **2.40**. Never use it as text or as the only marker on light; it is a fill.
- `card` (white) thumb on `dawn` track (light): **2.55**, under the 3:1 non-text floor. This is why section 10.1 row 9 recommends a `dawnForeground` thumb.
- `mutedForeground` on `card`: 11.58 / 7.68. `destructive` on `popover`: 5.31 / 5.45.
- `input` and `border` on `background`: 1.42 / 1.33 light, 1.63 / 1.51 dark. These are known and accepted gaps (README). M3 controls that need a 3:1 outline get `mutedForeground` (9.1).

---

## 5. Typography

### 5.1 Font files [F iOS `Tools/generate_fonts.py`, `Fonts/manifest.json`, `SOURCE.md`]

- Sources: `google/fonts` at commit `24ddc1f324cac5d9f5aedca126d298f56e3f1640`.
  - `ofl/figtree/Figtree[wght].ttf` (sha256 `26ad3db9…6fb714`).
  - `ofl/fraunces/Fraunces[SOFT,WONK,opsz,wght].ttf` (sha256 `177ff6c0…958abbb`).
  - OFL licences are hash-pinned.
- Fraunces variable defaults are **opsz 9, wght 900, SOFT 0, WONK 1**. A variable Fraunces with no explicit axes renders black at opsz 9.
- Cuts are made with fonttools 4.60.1 `instantiateVariableFont`, `recalcTimestamp=False`, so output is deterministic:
  - Figtree 400/500/600/700.
  - Fraunces 400/500 × opsz 20/32/48 with SOFT=0 and WONK=1.
  - Names are CF-prefixed (`CFFraunces-400-Opsz32`) and unique, so optical cuts never substitute for each other. No `fvar` remains.
- Full upstream character sets are kept. They are not latin-subsetted, despite the README's "latin subsets".
- Sizes: Figtree about 40 KB per face; Fraunces about 73 KB per cut; about 600 KB in total.
- Inspected with fontTools:
  - `CFFigtree-400`: upm 1000, hhea 950/−250/0, `USE_TYPO_METRICS` set. GSUB features include **`tnum`**, `pnum`, `case`, `frac`, `ss01`, `ss02`.
  - `CFFraunces-400-Opsz20`: upm 2000, hhea 1956/−510/0. Features `case liga rvrn ss01`, **no `tnum`**.
- Glyph coverage:
  - Both have `− – · … ’ “ ” € ×`.
  - **Fraunces has no `→` (U+2192)**; Figtree has it. Fraunces strings containing "→" fall back to the system font for that glyph.
  - Neither has `✓`.
- Weights used by iOS code: figtree400 (27×), figtree600 (22×), figtree500 (13×), figtree700 (2×), fraunces400 opsz20/32/48 (12/6/3×). Fraunces **500 cuts are registered but never used**.

### 5.2 Android font recommendation [R]

- **Static cuts, not variable.** Reuse the iOS TTFs unchanged: same bytes, same hashes, guaranteed parity, and no risk from the opsz 9 / wght 900 defaults.
- Compose can drive `opsz` through `FontVariation.Settings` on API 26+. Each distinct setting still creates its own `Typeface`, and the variable file is larger. Revisit only if text must scale continuously.
- `res/font/` names must be lower-case `[a-z0-9_]`:
  - `cf_figtree_400.ttf`, `cf_figtree_500.ttf`, `cf_figtree_600.ttf`, `cf_figtree_700.ttf`
  - `cf_fraunces_400_opsz20.ttf`, `cf_fraunces_400_opsz32.ttf`, `cf_fraunces_400_opsz48.ttf`
  - and the 500 cuts **[OQ 9: ship them? default yes, 3 × 73 KB, for parity]**.
- Pipeline: port `generate_fonts.py` and `fonts.sh` into `tools/fonts/`, changing only the output directory and file names. Keep `manifest.json` with the sha256 values. A JVM unit test checks the hashes, weights, the absence of `fvar`, and coverage of `ChronoFlowëïéèÉáóúü` (iOS `test_fonts.py`).
- Put the OFL texts in `app/src/main/assets/licenses/` and list them on an open-source licences screen.
- One `FontFamily` per optical size, so weight matching never crosses opsz:

```kotlin
val Figtree = FontFamily(
    Font(R.font.cf_figtree_400, FontWeight.W400), Font(R.font.cf_figtree_500, FontWeight.W500),
    Font(R.font.cf_figtree_600, FontWeight.W600), Font(R.font.cf_figtree_700, FontWeight.W700),
)
val FrauncesOpsz20 = FontFamily(Font(R.font.cf_fraunces_400_opsz20, FontWeight.W400), Font(R.font.cf_fraunces_500_opsz20, FontWeight.W500))
val FrauncesOpsz32 = FontFamily(Font(R.font.cf_fraunces_400_opsz32, FontWeight.W400), Font(R.font.cf_fraunces_500_opsz32, FontWeight.W500))
val FrauncesOpsz48 = FontFamily(Font(R.font.cf_fraunces_400_opsz48, FontWeight.W400), Font(R.font.cf_fraunces_500_opsz48, FontWeight.W500))
```

- Display faces are never bold. The generator rejects a display style with a weight above 500.
- **Startup**: resource fonts load synchronously on first text layout. Mirror iOS D11/D26 (fonts registered off the main thread after the splash commit): warm `ResourcesCompat.getFont(ctx, id)` for figtree 400/600 and fraunces 400 opsz20/32/48 on `Dispatchers.IO` from `Application.onCreate`, after the splash is posted. Confirm with a macrobenchmark that it removes the first-frame font stall. **[verify]**

### 5.3 Type scale [F tokens.json, iOS Plan.source.json, plan.md typography and auth tables; R Kotlin]

Common `TextStyle` defaults:
- `lineHeightStyle = LineHeightStyle(Alignment.Center, Trim.None)`. This reproduces CSS half-leading; do not rely on the platform default.
- `platformStyle = PlatformTextStyle(includeFontPadding = false)`.
- `letterSpacing` in **em**, as CSS defines it: −0.01em → `(-0.01).em`. Do not convert to sp.
- `tnum` versions via `fontFeatureSettings = "tnum"` (Figtree only).

| Style | Face | Size / line (sp) | Tracking | Source |
|---|---|---|---|---|
| `hero` | Fraunces 400 **opsz 48** | 48 / 48 | −0.01em | tokens (landing; Welcome headline) |
| `pageTitle` | Fraunces 400 opsz 32 | 30 / 36 | −0.01em | tokens (also trip-card IATA codes) |
| `pageTitleLg` | Fraunces 400 opsz 32 | 36 / 40 | −0.01em | tokens (auth heading 36/40 uses this) |
| `cardTitle` | Fraunces 400 opsz 20 | 24 / 32 | −0.01em | tokens (card and dialog titles, zone-change card) |
| `sectionTitle` | Fraunces 400 opsz 20 | 20 / 28 | −0.01em | tokens (Welcome wordmark 20/28) |
| `dayTitle` | Fraunces 400 opsz 20 | 18 / 28 (tokens) vs **18 / 26 (iOS planDayTitle)** | −0.01em | **[OQ 8]** default 18/26 as iOS (from the Plan board) |
| `planRemoteTitle` | Fraunces 400 opsz 20 | 22 / 28 | 0 | Plan.source.json |
| `splashWordmark` | Fraunces 400 opsz 32 | 32 / 40, fixed (not scaled) | −0.01em | SplashScreen board, D8 |
| `bodyLg` | Figtree 400 | 16 / 24 | 0 | tokens |
| `body` | Figtree 400 | 14 / 20 | 0 | tokens |
| `label` | Figtree 600 | 14 / 20 | 0 | tokens |
| `meta` | Figtree 400 | 12 / 16 | 0 | tokens |
| `eyebrow` | Figtree 600, uppercase | 12 / 16 | +0.1em | tokens (colour dawnText) |
| `blockLabel` | Figtree 600 | 11 / 14 (tokens say lineHeight 1.25 → 13.75; iOS rounds to 14) | 0 | tokens / Plan.source.json |
| `priorityTag` | Figtree 500 | 10 / 14 | 0 | tokens |
| `button` | Figtree 600 (primary), 500 (secondary) | 16 / normal | 0 | plan.md Welcome |
| `planTitle` | Figtree 600 | 17 / 22 | 0 | planCompactNote (trip-page title, **not** system font) |
| `planSubtitle` | Figtree 400 | 12 / 15 | 0 | planCompactNote (muted) |
| `planLane` | Figtree 500 | 11 / 14 | 0 | planCompactNote |
| `planCompactWeekday` / `planCompactDate` | Figtree 600 / 400 | 10 / 12 | 0 | planCompactNote |
| `planListLine` / `planListTitle` | Figtree 400 / 600 | 13 / 18 | 0 | planCompactNote |
| `planNow` | Figtree 600 | 10 / 16 | 0 | Plan.source.json |
| `planPriority` | Figtree 500 | 10 / 14 | 0 | Plan.source.json |
| wizard set (`wizardAirport` 20/26 600, `wizardRow` 17/22, `wizardHint` 13/18, `wizardRoute` 15/20, `wizardChoice` 18/24 600, `wizardLongTitle` Fraunces 24/30, `wizardSearchCode` 15 700 +0.6 …) | | | | `PlanUI.source.json` + `PlanUITokens.swift:46-136` (port as is) |

- Opsz rule (iOS `PlanUITokens.swift:31-33`): `size >= 40 → 48; size >= 26 → 32; else 20`.
- Fixed-size text (splash wordmark, and the launch frame if text is ever drawn there): wrap it in `CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1f))`.

### 5.4 Font scaling cap [F iOS; R Android]

- iOS fact: "Dynamic Type is capped at xxLarge at the root; never add per-view caps or bypass the cap" (`AGENTS.md`, plan.md l.186, `ContentSizeCap.swift`). On iOS that is body 17 → 21 pt (+23.5 %).
- Android:
  - Cap `fontScale` at **1.3**, the highest pre-Android-14 preset and the nearest preset above iOS xxLarge. Android 14+ users at 1.5 / 1.8 / 2.0 get 1.3. **[OQ 5]**
  - Apply it once at the root by overriding the `Configuration` in `MainActivity.attachBaseContext`. Then Android 14's **non-linear** `FontScaleConverter` still applies at 1.3, and Compose dialogs and sheets inherit it.
  - Do not use a `Density(density, fontScale)` override in Compose: that makes scaling linear.

```kotlin
override fun attachBaseContext(newBase: Context) {
    val config = Configuration(newBase.resources.configuration)
    super.attachBaseContext(
        if (config.fontScale > MAX_FONT_SCALE) newBase.createConfigurationContext(config.apply { fontScale = MAX_FONT_SCALE })
        else newBase
    )
}
private const val MAX_FONT_SCALE = 1.3f
```

- No per-screen caps or "smaller font" overrides (iOS rule). Layouts must reflow at 1.3 together with Dutch strings, which run about 20 % longer.
- WizardCopy layout rules (AX1 wrap points, the 92 pt Ready label column, the Sleep 2×2 grid becoming one column under 360 wide) map to fontScale ≥ 1.3 and width < 360 dp.

---

## 6. Sky gradient (`cf-sky` / `bg-sky`)

**[F]**
- Vertical, top → bottom. `sky-top` 0 %, `sky-mid` at 55 % (light) or 60 % (dark), `sky-horizon` 100 %, interpolated in OKLab.
- iOS samples each segment in OKLab, always emits the exact mid stop, and doubles the divisions until linear interpolation of the encoded channels is within ±0.25/255 of true OKLab at 1001 points (`ColorMath.swift:83-106`). In P3 that took 17 stops.
- Reference samples for tests are in iOS plan.md l.161-164 (light 0.0 → 1.0: `#1F285A #272D61 #303167 #38366E #403B75 #493F7C #5D4C7F #7E607E #A1737A #C58572 #EB9666`).

**[R]** Same algorithm with sRGB encoding. Computed here: **33 stops per theme**.
- Light: 0.0000 `#1F285A` … 0.5500 `#4D427F` … 1.0000 `#EB9666`. 16 equal steps of 0.034375 to the mid point, then 16 steps of 0.028125.
- Dark: 0.0000 `#06091C` … 0.6000 `#221A40` … 1.0000 `#AA5830`. Steps of 0.0375, then 0.025.
- Full stop lists for the test fixture:
  - Light: `#1F285A #222A5C #252B5F #282D61 #2A2F63 #2D3066 #303268 #33336A #36356C #39366F #3C3871 #3F3A73 #413B76 #443D78 #473E7A #4A407D #4D427F | #56487F #5F4E7F #68537F #71597F #7B5E7E #84647D #8E697C #986E7B #A27379 #AC7878 #B67E76 #C18373 #CB8871 #D58D6E #E0926A #EB9666`
  - Dark: `#06091C #070A1E #090B20 #0B0C22 #0C0D24 #0E0E27 #100F29 #12102B #13112D #151330 #171432 #191534 #1B1637 #1C1739 #1E183B #20193E #221A40 | #291F41 #312341 #392742 #402B42 #492F42 #513342 #593741 #623B41 #6A3F40 #73423F #7C463D #854A3C #8E4D3A #975137 #A05434 #AA5830`
- Drawing:
  - `Brush.verticalGradient(*CfSky.light)`, built once per theme. `Modifier.drawBehind { drawRect(brush) }` on a full-bleed `Box`, with `WindowInsets` ignored, decorative and hidden from accessibility.
  - Compose caches the `LinearGradient` shader per size.
- **Banding**: the dark ramp is very dark and low-contrast in 8 bits. If a device screenshot shows bands, draw through `drawIntoCanvas` with a native `Paint` that has `isDither = true` and the same shader. **[verify on the Pixel 8 AVD and a real OLED]**
- The sky is plain: no starfield or dots (user decision 2026-11-06). The README's "Layer `pattern-dots` at 20 %" is superseded.
- On the sky: titles in `skyTitle`; long copy on a `skyCopyBackground` plate in `skyCopy`; the CTA is a `dawn` pill with `dawnForeground`, **never `primary`** (README). Status-bar icons are light on sky screens.

---

## 7. Shadows and elevation

**[F]** `shadow-soft` is the only elevation: feature, trip and summary cards, sticky day headers, the zone-change card and the auth card. iOS applies it only to the auth/settings cards, toast, walkthrough and wizard checking card, never in the splash tree. iOS DayHeader and the compact line have **no** shadow.

**[R]**
- Use Compose UI 1.9+ `Modifier.dropShadow(shape, Shadow(...))`. Confirmed present in `ui-graphics-android:1.12.1` (`androidx.compose.ui.graphics.shadow.Shadow(radius: Dp, color: Color, spread: Dp, offset: DpOffset, alpha: Float, blendMode)`) and `ui-android:1.12.1` (`Modifier.dropShadow(Shape, Shadow)`). It models CSS `box-shadow` (blur radius, spread, offset), which `Modifier.shadow(elevation)`'s light-source model cannot.

```kotlin
val ShadowSoftLight = Shadow(radius = 30.dp, color = Color(0xFF202955), spread = (-12).dp, offset = DpOffset(0.dp, 10.dp), alpha = 0.25f)
val ShadowSoftDark  = Shadow(radius = 40.dp, color = Color.Black,       spread = (-14).dp, offset = DpOffset(0.dp, 12.dp), alpha = 0.60f)
fun Modifier.shadowSoft(shape: Shape) = composed { dropShadow(shape, if (CfTheme.colors.isDark) ShadowSoftDark else ShadowSoftLight) }
```

Write the final version as a `Modifier.Node` rather than `composed`.

- Two things to verify against a board render: whether Compose's `radius` equals the CSS blur radius (CSS σ = radius/2), and whether **negative spread** is honoured. If spread is not honoured, inset the shadow shape by 12/14 dp instead. **[verify]**
- Performance: no `dropShadow` on anything inside the timeline's scrolling lanes or on per-frame sticky elements. Follow iOS: day headers and the compact line are opaque with no shadow. Use shadows on cards in Today, My trips, Settings, the zone-change card and auth.
- M3 elevation:
  - Set `ColorScheme.surfaceTint = Color.Transparent` so M3 never tints surfaces by tonal elevation.
  - System components (dialogs, menus, sheets) keep their own platform shadows; they belong to the OS.

---

## 8. Glass and blur

**[F]** The DS defines glass for the sticky header and the floating tab bar (`cf-glass-bar`, `cf-glass`). Both are now **system** chrome (2.2). The remaining designed blur surfaces on boards: footers and headers of TripReview, TripEditor(+Errors), TripAirportSearch, the Guest* boards (20 px), and LiveActivity (iOS only). iOS ships the trip-page compact line opaque (`card`). RN flagged that Android `BlurView` without a target "may only tint" (RN plan OQ 46).

**[R]**
- **No backdrop blur in v1.** Glass surfaces become **opaque `background`** (bars and footers) or **opaque `card`** (floating elements), with the `glassBorder` hairline where the board draws a border.
  - This is what iOS chose for the compact line.
  - It avoids reading 72 % alpha with no blur, which hurts legibility over the timeline.
  - It costs nothing per frame.
- Why not `RenderEffect`: `RenderEffect.createBlurEffect` (API 31+) blurs a node's **own** content, not what is behind it. A real backdrop blur needs the background re-recorded into a separate `RenderNode` every frame (that is how libraries like Haze work: real blur on API 31+, a scrim below). That is extra work on exactly the scrolling screens that must stay smooth.
- If the user wants glass later: add Haze behind a flag on API 31+ for **non-timeline** surfaces only (wizard and guest footers), measure frame times with Macrobenchmark `FrameTimingMetric`, and keep the opaque fallback for API < 31 and for Remove Animations / battery saver. **[OQ 7]**
- The tab bar, top app bar, sheets and dialogs are M3 and opaque by design. That is Android's equivalent of "the OS's blur". Do not imitate Liquid Glass (iOS rule D1b: "never a hand-drawn imitation").

---

## 9. Theming architecture

### 9.1 Recommendation: custom tokens through CompositionLocal, mapped onto MaterialTheme for system components [R]

```kotlin
@Composable
fun ChronoFlowTheme(darkTheme: Boolean = rememberDarkThemePreference(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) CfDarkColors else CfLightColors
    CompositionLocalProvider(
        LocalCfColors provides colors,              // staticCompositionLocalOf: theme flips are rare, no read tracking
        LocalCfType provides CfTypeScale,           // Figtree/Fraunces styles (5.3)
        LocalCfShapes provides CfShapes,            // radii (9.3)
        LocalCfSpacing provides CfSpacing,
        LocalRippleConfiguration provides RippleConfiguration(color = colors.foreground),
        LocalTextSelectionColors provides TextSelectionColors(handleColor = colors.dawnText, backgroundColor = colors.dawn.copy(alpha = 0.4f)),
    ) {
        MaterialTheme(colorScheme = colors.toM3(), /* typography = M3 default, shapes = M3 default */ content = content)
    }
}
object CfTheme { val colors: CfColors @Composable @ReadOnlyComposable get() = LocalCfColors.current /* … */ }
```

- Designed surfaces read `CfTheme.*` only: tokens by their DS names, exactly as the README and the boards name them.
- System components read `MaterialTheme` and need only colours. **M3 typography and shapes stay at their defaults** (system font, M3 corner radii). This mirrors iOS, where system components keep the system font and shapes: alerts, menus, pickers, and since 2026-11-10 even the tab and settings titles. The day menu "uses the system font because it is the system menu" (planCompactNote).
- Use `MaterialTheme`, **not `MaterialExpressiveTheme`**. Expressive motion schemes are spring-based and the README says "no springs". `MaterialExpressiveTheme` exists in the cached `material3:1.5.0-beta01` but stays unused.
- **Disable dynamic colour** (Material You). The brand palette is the identity; Android's dynamic tonal palette is never used.
- Theme preference (Settings → Preferences pills: System / Light / Dark):
  - On API 31+, call `UiModeManager.setApplicationNightMode(MODE_NIGHT_AUTO|YES|NO)` so the **system splash and launch frame** match the user's choice across cold starts.
  - Compose reads the effective mode from `LocalConfiguration.uiMode` (or the stored preference below API 31).

`CfColors.toM3()`: the M3 role mapping. It is a **system-tint** mapping, so M3 `primary` is not the token `primary`:

| M3 role | Token | Why |
|---|---|---|
| `primary` | **`dawnText`** | "System components tinted amber" (dirNative). `dawnText` passes AA as text on every surface (5.14+), unlike `dawn` (2.4). The designed indigo buttons read `CfTheme.colors.primary`, not M3. |
| `onPrimary` | light `primaryForeground` (5.23), dark `dawnForeground` | |
| `primaryContainer` / `onPrimaryContainer` | `systemSelected` / `dawnText` | Time-picker selected field, FAB (unused) |
| `secondary` / `onSecondary` | `dawnText` / same as `onPrimary` | |
| `secondaryContainer` / `onSecondaryContainer` | **`systemSelected`** (mix(card, dawn, .16)) / `dawnText` | Navigation-bar indicator, active segmented button, selected chips. README: "the active tab is `text-dawn` with a 16 % dawn tint behind it". |
| `tertiary` / `onTertiary` / `tertiaryContainer` / `onTertiaryContainer` | `dawn` / `dawnForeground` / `systemSelected` / `dawnText` | Time-picker AM/PM selector |
| `background` / `onBackground` | `background` / `foreground` | Scaffold |
| `surface` / `onSurface` | `background` / `foreground` | Top app bar at rest blends with the page, like iOS's transparent nav bar |
| `surfaceVariant` / `onSurfaceVariant` | `muted` / `mutedForeground` | |
| `surfaceContainerLowest` / `Low` | `card` / `card` | Bottom sheets ("cards, sheets … on `card`") |
| `surfaceContainer` | `popover` | Navigation bar, menus ("menus on `popover`"), scrolled top app bar |
| `surfaceContainerHigh` / `Highest` | `popover` / `muted` | Dialogs and date picker / switch track, filled fields |
| `surfaceBright` / `surfaceDim` | `popover` / `surface2` | |
| `surfaceTint` | `Color.Transparent` | No tonal-elevation tint |
| `inverseSurface` / `inverseOnSurface` / `inversePrimary` | `foreground` / `background` / `dawn` | Snackbar |
| `error` / `onError` / `errorContainer` / `onErrorContainer` | `destructive` / `destructiveForeground` / `authErrorBackground` / `cardForeground` | |
| `outline` | `mutedForeground` | M3 uses it for control borders (unchecked switch, radio, outlined field). `input` and `border` fall short of 3:1. |
| `outlineVariant` | `border` | Dividers and hairlines |
| `scrim` | `Color.Black` (components apply their own alpha, about 32 %) | |
| `primaryFixed` etc. (M3 1.4 fixed roles) | same as the non-fixed counterparts | |

### 9.2 Ripple versus press [R]

- **System (M3) components keep the ripple**: Android's native press feedback, tinted through `LocalRippleConfiguration`.
- **Designed pressables** (cards, rows, pills, CfButton, chips, day pills, timeline blocks) implement `cf-press` exactly, as iOS `PressStyle` does: the boards and the DS say "everything pressable gives feedback on press" with these values. **[OQ 4]**
- Implement it as a `Modifier.Node` (`LayoutModifierNode`) that collects `PressInteraction` from its `MutableInteractionSource`, animates a progress `Animatable` (120 ms, `CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)`), and applies it inside `placeWithLayer { scaleX = scaleY = 1 − 0.03p; alpha = 1 − 0.15p }`. This updates layer properties only, with no recomposition and no relayout.
- Combine it with `Modifier.clickable(interactionSource, indication = null, role = …)` and a focus indication: a 2 dp `ring` outline when keyboard-focused (README "Focus is visible").
- Reduce Motion: pressed = alpha 0.85, scale 1, no animation (`theme.css`, iOS `PressStyle.values`).

### 9.3 Shapes and spacing [R]

- `CfShapes`: `sm = RoundedCornerShape(10.dp)`, `md` 12, `lg` 14, `xl` 18, `x2l` 22, `x3l` 26, `full = CircleShape` (`RoundedCornerShape(50)`). Usage as in the README: 3xl for feature/trip/summary cards and the zone-change card; 2xl for tiles, day headers, list rows and choice cards; lg/xl for swatches and small cards; full for buttons, chips, badges, tags and progress bars.
- `CfSpacing`: `s1 = 4.dp … s16 = 64.dp` from tokens.
- Port `PlanMetrics` from `plan-tokens.json` as `multiplier × 4.dp`. Examples: `pointsPerHour = 40.dp` (×10), `gutterWidth = 44.dp` (×11), `minLaneWidth = 44.dp`, `touch = 44.dp` → **raise to 48.dp on Android**, `compactHeaderHeight = 36.dp`, `wideBreakpoint = 600.dp` (×150), `wideListWidth = 280.dp`, `wideLargeBreakpoint = 900.dp`, `wideLargeListWidth = 320.dp`. Scalars: `hatchOpacity` 0.12, `dotOpacity` 0.16, `coreBorderOpacity` 0.7, `optionalOpacity` 0.75, `markedOpacity` 0.6 …
- The timeline renderer should take these as px `Float`s computed once per density and pass them to `DrawScope`. Avoid `Dp` arithmetic per block per frame.
- Category patterns (designed content): one cached `ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))` per (pattern, colour, density), so each block's pattern is a single `drawRect`.
  - Dots: 7 px grid, 2 px diameter, fg α0.16.
  - Hatch: 135°, 2 px line, 8 px period (diagonal tile edge = 8·√2 px, rounded to whole px), fg α0.12.
  - Dashed border: `Stroke(2.dp, pathEffect = dashPathEffect)`.
  - This is the timeline research topic; listed here because the values are design tokens.

---

## 10. iOS system component → Material 3 mapping

Rule (iOS AGENTS/D1b, canvas dirNative): **native component, tinted with tokens where the platform exposes colour; shape, size, motion and font stay the platform's. Designed content inside stays as drawn.** No Android chrome boards exist yet (OQ 1).

### 10.1 Mapping table

| # | Board element (class · boards) | iOS ships | Android / Compose M3 | Token tinting | Notes / copy |
|---|---|---|---|---|---|
| 1 | Floating tab bar `ios-tabbar`/`ios-tab` · Today, Trips, SetHome, Plan, PlanRemote | `TabView` + SF Symbols, `.tint(primary)`, minimise on scroll (iOS 26) | **`NavigationSuiteScaffold`** (`material3-adaptive-navigation-suite`): `NavigationBar` + `NavigationBarItem` under 600 dp; `NavigationRail` from 600 dp (iOS: "a side rail on iPhone Duo"; canvas wide breakpoint 600). | container `popover` (M3 `surfaceContainer`); `NavigationBarItemDefaults.colors(selectedIconColor = dawnText, selectedTextColor = dawnText, indicatorColor = systemSelected, unselectedIconColor = mutedForeground, unselectedTextColor = mutedForeground)` | Labels "Today / My trips / Settings" = "Vandaag / Mijn reizen / Instellingen". Icons: Material Symbols Rounded `space_dashboard` (board: lucide layout-dashboard; iOS `square.grid.2x2`), `flight`, `settings`; outlined unselected, filled selected. Always show labels. **No minimise on Android** (RN l.1460). Hidden on settings sub-pages (RN 2026-10-31). Selected colour: **OQ 3**. |
| 2 | `ios-rail` (defined, unused) | Duo side rail by OS | `NavigationRail` through the same scaffold | as 1 | Portrait only for now (canvas, WizardCopy). |
| 3 | Large title header `ios-navbar` + `ios-largetitle` · Today, Trips, SetHome, all Set* pages, PolicyReader | System large title, **system font bold** (decision 2026-11-10, supersedes Fraunces) | **`LargeTopAppBar`** (or `MediumTopAppBar` for long Dutch titles) with `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`; collapses on scroll as on iOS | `largeTopAppBarColors(containerColor = background, scrolledContainerColor = popover, titleContentColor = foreground, navigationIconContentColor = foreground, actionIconContentColor = foreground)`; title in **M3 default typography** (system font) | Logo as a header item: Today and My trips use a decorative `navigationIcon` image (32 dp logo); Settings uses an `IconButton` that opens Today (RN 16c). My trips "New trip" = `actions` `IconButton(add)`, disabled offline with its hint. |
| 4 | Back `ios-back` (glass pill with the previous title) · Set* subpages, PlanRemote, PolicyReader | System back with previous title | `navigationIcon = IconButton { Icon(Icons.AutoMirrored.Filled.ArrowBack) }`, plus the **system back gesture with predictive back** (`android:enableOnBackInvokedCallback="true"`; Navigation Compose's predictive back transitions) | `foreground` | Android shows no previous-title label (idiom). Content description "Back" / "Terug". |
| 5 | Nav buttons `ios-navbtn` (44 glass circles) · Plan, Wizard | Toolbar items | `IconButton` in `TopAppBar.actions` (48 dp target, 40 dp state layer) | `foreground`; disabled = M3 default alpha | |
| 6 | Trip-page header · Plan, PlanWide, PlanRemote | back, route title + subtitle, view toggle, ••• | **`TopAppBar`** (small, pinned; the compact line sits under it). `title` = designed two-line block: `planTitle` 17/22 Figtree 600 route + `planSubtitle` 12/15 muted (date range at rest, "Arrival day · Tue 13 Oct" when scrolled). Clicking it opens the day menu (row 13). `actions`: view-toggle `IconButton` (timeline ⇄ list; content descriptions "Show as list" / "Show as timeline" = "Als lijst tonen" / "Als tijdlijn tonen"), then **`MoreVert`** `IconButton` → trip actions sheet. | `background` container; icons `foreground` | iOS uses "•••" (horizontal); Android idiom is vertical **[OQ 10, default MoreVert]**. Title semantics: label "Amsterdam to Tokyo, 12 to 18 October" (nl "Amsterdam naar Tokio, 12 tot 18 oktober"), hint "Choose a day" / "Kies een dag". |
| 7 | Close × · Wizard, editor, sheets | × on the right | `IconButton(Icons.Default.Close)` in **`actions` (right)**, not `navigationIcon` | `foreground` | Rick's rule, confirmed for Android (WizardCopy, wiz4Rick). |
| 8 | Segmented `ios-seg` · CalendarExport (calendar alerts), Wizard (stops "No, direct · Yes") | `Picker(.segmented)`, system grey | **`SingleChoiceSegmentedButtonRow` + `SegmentedButton`** | `SegmentedButtonDefaults.colors(activeContainerColor = systemSelected, activeContentColor = dawnText, activeBorderColor = mutedForeground, inactiveContainerColor = Color.Transparent, inactiveContentColor = foreground, inactiveBorderColor = mutedForeground)` | "On Android it takes our colours" (reqSystemChrome). Keep M3's check icon in the active segment. The trip page's Timeline/List toggle is no longer segmented: it is the top-bar button in row 6 (planCompactNote). |
| 9 | Switch `ios-switch` (dawn track) · SetReminders, SetPreferences, TripEditor, Wizard; Sign-up "Show password" | `Toggle` `.tint(dawn)` | **`Switch`**; the whole row is `Modifier.toggleable(role = Role.Switch)` and the switch gets `onCheckedChange = null` | `SwitchDefaults.colors(checkedTrackColor = dawn, checkedBorderColor = dawn, checkedThumbColor = dawnForeground, checkedIconColor = dawn, uncheckedTrackColor = muted, uncheckedBorderColor = mutedForeground, uncheckedThumbColor = mutedForeground)` | A white thumb on `dawn` is 2.55:1 in light, so use the dark `dawnForeground` thumb (6.76:1). Haptics `ToggleOn` / `ToggleOff`. |
| 10 | Alert `ios-alert` / `ios-alert-btn` · TripDelete ("Delete Amsterdam → Tokyo?" / Keep trip / Delete trip), TripDiscard, SheetStates (delete failed, offline, not on phone), Wizard (discard), PasswordPrompt | `.alert` | **`AlertDialog`** (M3 basic dialog: `title`, `text`, `confirmButton`, `dismissButton`) | `containerColor = popover`, `titleContentColor = foreground`, `textContentColor = mutedForeground`; buttons are `TextButton` with `contentColor = dawnText`, and **destructive actions in `destructive`** | Same copy as iOS. M3 puts the confirming action on the right ("Keep trip" left, "Delete trip" right). PasswordPrompt: an `AlertDialog` whose `text` slot holds a password field (row 16) and the error/lock line. Dialog shape and font are M3's. |
| 11 | Sheets (36 × 5 grabber) · ItemDetail (step sheet), TripActions, CalendarExport, PickerTime, ReviewAnswer, SetPaywall hand-off, GuestSignup, WizardSheet*, reminders ask | `.sheet` + `presentationDetents` | **`ModalBottomSheet`**. Content-sized: `rememberModalBottomSheetState(skipPartiallyExpanded = true)`. iOS `[.medium, .large]`: partial + expanded. | `containerColor = card`, `contentColor = cardForeground`, `dragHandle = { BottomSheetDefaults.DragHandle(color = mutedForeground.copy(alpha = .4f)) }`, `scrimColor` default | Corners, handle and motion are M3's ("sheets are native sheets"). **Content is designed** (trip actions, step detail, answer lists). Make content scroll inside the sheet, capped at about 85 % height, so buttons stay reachable at fontScale 1.3 (RN 16b lesson). Wizard full-screen cover = a full-screen nav destination with a slide-up transition, not a sheet. |
| 12 | Date and time · PickerDateTime ("Choose departure date and time", "Local time at Amsterdam Schiphol (AMS)", Cancel / Done); flight date and time pills; body-clock times | `DatePicker` graphical + compact time inside a designed sheet; `.tint(dawnText)` | Per WizardCopy: **`DatePickerDialog { DatePicker(state, title = {…}, headline = {…}) }`** then **`TimePickerDialog { TimePicker(state) }`** (`TimePickerDialog` confirmed in the cached material3 1.5.0-beta01). `SelectableDates` for minimum and maximum. `is24Hour` from **the app's time-format preference** (Settings pills), falling back to `DateFormat.is24HourFormat(ctx)`. Locale = app language. | `DatePickerDefaults.colors(containerColor = popover, selectedDayContainerColor = dawn, selectedDayContentColor = dawnForeground, todayDateBorderColor = dawn, todayContentColor = dawnText, selectedYearContainerColor = dawn, selectedYearContentColor = dawnForeground, headlineContentColor = foreground, …)`; `TimePickerDefaults.colors(clockDialColor = muted, selectorColor = dawn, clockDialSelectedContentColor = dawnForeground, timeSelectorSelectedContainerColor = systemSelected, timeSelectorSelectedContentColor = dawnText, periodSelectorSelectedContainerColor = systemSelected, periodSelectorSelectedContentColor = dawnText)` | The airport-local-time summary goes in the dialog `title` / `headline` slot. Confirm / Cancel = "Done" / "Cancel" (M3 `TextButton`, dawnText). The pills that open the dialogs are designed. |
| 13 | Day menu · PlanDayMenu (redrawn as the system menu) | SwiftUI `Menu` | **`DropdownMenu`** anchored to the title. Items show weekday and day label, with the date as a second line and a leading **check** on the current day. material3 1.5 adds `DropdownMenuGroup`, `SelectableDropdownMenuItem` and `DropdownMenuItemChecked` (present in the cached AAR, probably experimental); otherwise `DropdownMenuItem(text = { Column { … } }, leadingIcon = { if (current) Icon(Check) })`. | `containerColor = popover`; `MenuDefaults.itemColors(textColor = foreground, leadingIconColor = dawnText)` | Menu title "Go to day" / "Ga naar dag" as a non-interactive header (group label if the API allows; otherwise a `labelSmall` row with `heading()` semantics). System font (it is the system menu). |
| 14 | Wheels · PickerTime ("Choose your usual bedtime", "Steps of 5 minutes") | `DatePicker(.wheel)` | Compose M3 has no wheel. Default: **`TimePickerDialog` + `TimePicker`** (dial, with the M3 keyboard-input toggle), minutes **rounded to the nearest 5 on Done** | as 12 | **[OQ 11]** The alternative is a native `android.widget.NumberPicker` pair (real Android wheels with a 5-minute `displayedValues` step) through `AndroidView`. |
| 15 | Determinate progress (Today day progress; wizard 4 pt bar; credits) | `ProgressView(.linear).tint(dawn)` | **`LinearProgressIndicator(progress = { p }, color = dawn, trackColor = muted, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})`**, `Modifier.height(4.dp)` | dawn / muted | WizardCopy: "the native determinate linear progress indicator, styled like the 4 pt bar". |
| 16 | Spinners (button loading, Checking, sheet loading) | `ProgressView()` tinted per context | **`CircularProgressIndicator`** (indeterminate), small 16–20 dp in buttons | contextual: `primaryForeground` on primary buttons, `mutedForeground` inline, `dawnText` large | Not M3 Expressive `LoadingIndicator` (shape morphing, springs). Reduce Motion: the spinner keeps spinning (README), except where WizardSpec says "the spinner becomes a static dot" (Checking). |
| 17 | Pull to refresh (Today, My trips, Settings plan, passkeys) | `.refreshable` | **`PullToRefreshBox`** with `PullToRefreshDefaults.Indicator(containerColor = popover, color = dawnText)` | | Sync feedback stays quiet ("Synced 2 min ago" in Settings). |
| 18 | Text fields (Auth, Profile, password prompt, 2FA code, airport search) | Native `TextField` / `SecureField` in a designed field bundle | **`BasicTextField(state = TextFieldState)`** plus a **designed decoration**: label above, 1 dp `input` border, focused `ring`, invalid `destructive`, radius `md` 12 (touch fields 14 radius, 48 dp high, 14 dp padding, 16 sp, `card` fill, `.cf-touch`). `SecureTextField` / `OutlinedSecureTextField` for passwords. | cursor `ring`; selection handles `dawnText`; error text `destructive` | Hybrid: native editing (IME, autofill, selection), designed container. Autofill through `semantics { contentType = ContentType.EmailAddress / Password / NewPassword / SmsOtpCode }` (Compose 1.8+). The IME action chain follows iOS's return chain (plan.md auth). Do not use an M3 floating label; the board puts the label above. |
| 19 | Toast / feedback ("Saved", refused change) | Custom `FeedbackToast` (designed) | **[OQ 12]** Default: **M3 `Snackbar`** in a `SnackbarHost` above the navigation bar | `containerColor = foreground`, `contentColor = background`, `actionColor = dawn` | README: "Toasts: one channel, bottom on phones". |
| 20 | Share (data download, recovery codes) | System share | `Intent.createChooser(Intent(ACTION_SEND …))` | system | |
| 21 | Confirmation-style action sheets | iOS 16b used alerts | `AlertDialog` (row 10). Trip actions stays a **designed** `ModalBottomSheet` (row 11). | | |
| 22 | Live Activity (LiveActivity board; lock screen / Dynamic Island) | ActivityKit | No direct equivalent. Nearest: an **ongoing notification / Android 16 "Live Updates"** (`Notification.ProgressStyle`, promoted ongoing). | notification accent `dawn` | Separate research topic; the board's look cannot be carried over 1:1. **[OQ 13]** |
| 23 | Notifications permission and reminders | iOS permission alert | Android 13+ `POST_NOTIFICATIONS` system dialog, after the designed "Turn on reminders?" sheet (wizTodo7 #43) | system | Needs a **status-bar small icon** (11.3). |
| 24 | Status bar / home indicator | System | `enableEdgeToEdge()` (targetSdk 35+ enforces it); `WindowInsets` padding in designed content | Light status-bar icons on sky screens (Welcome, Auth, paywall header); otherwise they follow the theme | Gesture navigation gets a transparent bar; 3-button navigation gets the system scrim. |
| 25 | Splash `SplashScreen` board, Android 12+ | Storyboard frame 0 | **`androidx.core:core-splashscreen`**: `windowSplashScreenBackground = @color/launch_sky`, `windowSplashScreenAnimatedIcon = @drawable/splash_moon`, `postSplashScreenTheme`; `setKeepOnScreenCondition` until the first frame, then the in-app animation | `launch_sky` light `#1F285A`, night `#06091C` (= RN `values-night`) | Section 11.4. |
| 26 | Stepper (preparation days) | — | **Designed** − and + `IconButton`s, 48 dp (WizardCopy) | `foreground` on `muted` circles | Not a system control on Android. |
| 27 | Keyboard and IME | System | System; `imePadding()`; `WindowInsets.ime` | — | Keep the focused field and submit visible (iOS auth rule). |
| 28 | Scroll physics and overscroll | iOS bounce | Android stretch overscroll (default) | — | Never imitate the iOS bounce. |

### 10.2 Designed surfaces: stay faithful to the boards [F dirNative, reqSystemChrome, iOS settings-board-fidelity; R list]

These are drawn with `CfTheme` tokens, Figtree/Fraunces, CfShapes, `shadowSoft` and `cfPress`. **No M3 component styling leaks into them** (M3 components may be used underneath for semantics, but with every colour, shape and text style passed explicitly):

1. Splash animation (SplashScreen, SplashSpec) and Welcome on the sky (`Main.dc.html`: eyebrow, hero, copy plate, "Plan my trip" dawn pill, "Sign in" pill).
2. Auth screens on the sky: brand row, heading, card with banner/notice, field bundle, buttons, footer pill (AuthSignIn/SignUp/TwoFactor/Forgot/Reset/Verify, AuthStates).
3. Today panel: TodayPanel, TodayRow (done and skipped states), day progress text, active trip card (on sky, or `cf-trip-flat`), empty and first-load skeletons.
4. My trips: TripCard (IATA codes in Fraunces `pageTitle`), status badges, sections, empty state.
5. Trip page **content**: day pills (DayStrip), day headers (DayHeader, StatusPill, DayProgress, collapse), the **compact line** (opaque `card`), **timeline** (four lanes on `surface2`, hour gutter, blocks with category fill/fg/icon/pattern/dashed border, priority borders and tags, now line, zone-change divider and pill), the list view, ZoneChangeCard, notices and warnings, end-of-plan, PlanLegend, the remote/loading states.
6. Step sheet content (ItemDetail and its states), **trip actions sheet content**, calendar export content (except the segmented control), paywall/billing hand-off content.
7. Settings: home cards and rows (icon circles, subtitles), **Preferences theme and time-format pills**, language ChoiceCards, Body clock tiles, chronotype ChoiceCards and drink counters, Security, 2FA (QR on white tile, recovery codes, mono), Plan & credits hero, Notices.
8. Wizard content: airport rows and swap button, flight rows, date/time pills (they open system dialogs), choice cards, **answer lists** (RadioGroup rows with descriptions), the Checking arc, the Ready summary, the stepper, notices.
9. Primitives: CfButton (variants default/secondary/outline/ghost/link/destructive/**dawn**; pill by default; sizes sm 32, default 36, lg 40, icon 36 and touch 44 (`bundle.js` `Ba`), all raised to a **48 dp minimum touch target**), Badge, MetaChip, PriorityTag, StatusPill, CategorySwatch, Notice, Skeleton (91 uses on boards), Wordmark, Card.
10. Error and empty pages (ErrorNotFound, ErrorServer, GuestExcluded …).

---

## 11. Icons

### 11.1 UI icons [R]

- **Content and category icons: lucide** (DS rule, board source, web parity). Generate `ImageVector`s from a pinned lucide release (ISC licence) with a small converter in `:tools`, for only the glyphs the README category table and boards use: Moon, Bed, Armchair, Sun, CloudSun, Lamp, Glasses, Coffee (+ the 45° slash variant), Plane, ArrowLeftRight, Footprints, Globe, Timer, Info, Check, X …
  - Stroke 2, round caps and joins; 16 dp in UI, 14 dp in dense blocks, 20 dp in bars.
  - iOS substituted SF Symbols (`PlanAppearance.swift`: `bed.double`, `chair.lounge`, `eyeglasses`, `cloud.sun`, `cup.and.saucer`, `timer`). Android has no SF Symbols; lucide matches the boards exactly. **[OQ 14]**
- **System chrome icons: Material Symbols Rounded** (Android idiom; RN used `md` symbols `today` / `flight` / `settings`): tabs, `arrow_back` (auto-mirrored), `close`, `more_vert`, `add`, `check`, list/timeline toggle (`view_agenda` / `view_timeline`).
  - Import only the needed glyphs as vector drawables or `ImageVector`s. **Do not depend on `material-icons-extended`**: it is large and no longer updated.
- Decorative icons get `contentDescription = null`; icon-only buttons get a localised label.

### 11.2 Adaptive launcher icon [F `AppIcon.dc.html`, `icon/android/*.svg`, `icon/colors.json`; R]

Facts:
- `adaptive-background.svg`: 1024², `linearGradient` vertical `#06091c` 0 → `#221a40` 0.6 → `#aa5830` 1. These are the **dark-theme** sky values (`colors.json` top/mid/hor; they equal the dark `skyTop/skyMid/skyHorizon` hexes). The icon is always the night sky.
- `adaptive-foreground.svg`: lucide Moon path `M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401`, stroke `#f2ad73` (dawn dark) width 2.0, round caps and joins, `transform="translate(341.6 341.6) scale(14.200)"`.
- `adaptive-monochrome.svg`: the same path in `#ffffff`, stroke **2.2**, same transform.
- The PNG versions are 1024 × 1024 (background RGB, foreground and monochrome RGBA).
- iOS layers (`layers/moon.svg`) use `translate(256.4 256.4) scale(21.300)`.

**Safe-zone check** (computed from the path: a circle of r 9 at (12, 12) in the 24 box plus a 1-unit half-stroke, so outer radius 10 units):
- Foreground outer radius = 10 × 14.2 = **142 px of 1024 = 14.98 dp** from the centre on the 108 dp canvas.
- The Android safe zone is a **66 dp circle (radius 33 dp)**, and launchers show the middle **72 dp**. The moon fits with 18 dp to spare, with no clipping under any mask (circle, squircle, teardrop) and with parallax shifts.
- Visible size: moon diameter about 30 dp of the visible 72 dp = **41.6 %**. iOS: 426 / 1024 = **41.6 %**. The board's "drawn smaller so it looks the same size as on iOS" holds exactly.
- Monochrome uses the same geometry, so it is safe too.
- **Background crop**: launchers show only rows 16.7 %–83.3 % of the gradient. The visible bottom colour is about `#713E37` (sRGB interpolation, 58 % from mid to horizon) instead of iOS's full `#AA5830` horizon, so Android icons look less orange than iOS. The board's "Circle launcher / Squircle launcher" previews use these exact layers, so the designer has seen this. **[OQ 15, default: ship as supplied]** (README: "Use the file as supplied; never redraw or recolour it").

Recommendation:
- Convert the three SVGs to **VectorDrawables**:
  - `res/drawable/ic_launcher_background.xml`: viewport 1024, size 108 dp, `<aapt:attr name="android:fillColor"><gradient android:type="linear" startX=0 startY=0 endX=0 endY=1024>` with three `<item>`s.
  - `ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml`: one `<group translateX/Y="341.6" scaleX/Y="14.2">` with a `<path strokeColor strokeWidth strokeLineCap="round" strokeLineJoin="round" fillColor="#00000000">`.
  - Vectors are crisp at every density and tiny; the PNGs remain the reference for a pixel test.
- `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`: `<adaptive-icon>` with `<background>`, `<foreground>` and **`<monochrome>`** (themed icons, Android 13+). With minSdk ≥ 26, no raster mipmaps are needed.
- **Play Store icon**: a 512 × 512 PNG rendered full-bleed from the layers by the tool (Play applies the mask). It is a listing asset, not an APK resource.
- Add a unit test that parses the vectors and asserts the stroke bounds stay inside the 66 dp circle, so any future icon change is checked against the safe zone.

### 11.3 Notification small icon [R]

Reminders (local notifications) need a status-bar icon that is white on transparent and alpha only:
- `res/drawable/ic_stat_moon.xml`, 24 dp, viewport 24, the lucide Moon path at stroke 2 (from `layers/moon-white.svg` geometry).
- Notification accent colour: `dawn`.

### 11.4 Android 12+ splash [F SplashSpec, iOS D7, RN `android/app/src/main/res`; R]

- Facts:
  - iOS LaunchMoon: a 288 pt canvas with a 104 pt glyph at offset 92, glow σ 9 α 0.45, stroke light `#E78A45` / dark `#F2AD73`, on LaunchSky = `skyTop` (light `#1F285A`, dark `#06091C`).
  - Source SVGs: `chronoflow-ios/Assets/moon/launch-moon-{light,dark}.svg`. They use the older lucide moon path `M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z`.
  - RN: `splashscreen_background` `#1f285a` / night `#06091c`, `windowSplashScreenAnimatedIcon` a PNG, "imageWidth 288 keeps the 104 pt moon and glow inside Android's 192 dp".
- Android 12's splash icon without an icon background is a **288 dp canvas with a visible 192 dp circle**. That maps one to one onto the iOS 288 pt canvas: the 104 dp glyph and its glow fit.
- Recommendation:
  - Rasterise the iOS launch-moon SVGs (glow baked in, because VectorDrawable cannot blur) to `drawable-nodpi/splash_moon.png` at 1152 px (288 dp × 4), with a `drawable-night-nodpi/` variant for the dark stroke.
  - `launch_sky` colour in `values` / `values-night`, generated.
  - Keep the system splash until the first frame (`setKeepOnScreenCondition`), then the in-app Compose splash redraws frame 0 and animates (SplashSpec timings, separate topic).
  - Splash follows the app's theme choice on API 31+ through `UiModeManager.setApplicationNightMode` (9.1).

---

## 12. Accessibility, motion and haptics

- **Touch targets**: iOS ≥ 44 pt becomes Android **≥ 48 dp**. M3 components enforce it through `LocalMinimumInteractiveComponentSize`. Designed controls use `Modifier.minimumInteractiveComponentSize()` or explicit `sizeIn(minWidth = 48.dp, minHeight = 48.dp)` with the **visual size unchanged** (day chips are 44 visually, Today rows 56). WizardCopy already specifies 48 dp − / + buttons.
- **Font scale**: cap 1.3 (5.4); no per-view caps.
- **Contrast**: fixes live in tokens, never per component (README). The known gaps (`input`, `cat-dark` dark, `cat-light`/`cat-moderate` light) are accepted, and those blocks must always carry their icon, pattern and label. Add a generator check that fails if any `*-fg` on its fill, or `foreground` / `mutedForeground` / `dawnText` on `background` / `card` / `popover`, drops under 4.5:1.
- **Focus**: designed pressables draw a 2 dp `ring` outline on keyboard focus.
- **TalkBack**:
  - Headings via `semantics { heading() }` (iOS header trait).
  - Merge row semantics (`mergeDescendants`); state descriptions for done and skipped steps.
  - Spoken durations as in WizardCopy ("3 hours · 45 minutes" / "3 uur · 45 minuten").
  - Queued announcements: `LiveRegionMode.Polite`, or `announceForAccessibility` sparingly.
  - Category blocks are announced with their category name, so colour is never the only signal.
- **Reduce Motion**:
  - Android has no direct switch. Treat "Remove animations" (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, or `!ValueAnimator.areAnimatorsEnabled()`) as Reduce Motion.
  - Expose it as `LocalReduceMotion`, re-read on resume.
  - Under it: `cfPress` uses opacity only; the compact line swaps instantly (planCompactNote); the splash cross-fades in 300 ms; the spinner stays except where WizardSpec says static.
  - Compose animations already scale with the animator duration scale **[verify]**. Branches whose *values* change (not only duration) must read the local.
- **Motion**:
  - `tween` with `CubicBezierEasing` or `FastOutSlowInEasing` for designed motion.
  - No springs, no parallax (README). Avoid `MotionScheme.expressive()`.
  - M3 system components keep their own motion.
- **Haptics** [F WizardSpec "Haptics" → R `LocalHapticFeedback` types, all confirmed in ui 1.12.1]:

| WizardSpec event | iOS | Compose `HapticFeedbackType` |
|---|---|---|
| Selection: choice card, chronotype, answer, picker value, stepper step | selection | `SegmentTick` |
| The switch | selection | `ToggleOn` / `ToggleOff` |
| Light impact: swap, add stop, remove stop | light impact | `ContextClick` |
| Success: Checking finished; plan saved | success | `Confirm` |
| Warning: inline error after Continue; trip-wide message; no plan available | warning | `Reject` (Android has no separate warning type) |
| Error: Checking or saving failed (offline, server, practice, too many attempts) | error | `Reject` |
| Medium impact: "Make my plan", "Update my plan" | medium impact | `LongPress` **[OQ 16]** |

Splash and Welcome have no haptics (iOS plan l.294).

---

## 13. Open questions (each has a default so the plan can proceed)

1. **No Android chrome boards** (reqSystemChrome: "Android Material not drawn yet"). *Default:* build section 10 as specified, capture emulator screenshots of each mapped component in light/dark and en/nl, and leave a canvas note asking the designer to confirm or draw Material boards.
2. **M3 version.** The cached artifacts are Compose UI 1.12.1 and material3 1.5.0-beta01 (likely from the RN build). *Default:* the latest **stable** Compose BOM at project start, with a material3 release that has `TimePickerDialog` and `SecureTextField` (both present in the cached 1.5.0-beta01; confirm the first stable that ships them). Use experimental expressive menu APIs only behind `@OptIn` where they give the board's two-line checked items.
3. **Selected-tab colour**: the boards, README and dirNative say amber (`dawn-text` + 16 % dawn tint); iOS ships `.tint(primary)` (indigo in light, dawn in dark) and RN did the same. *Default:* **amber** (`dawnText` + `systemSelected` indicator), following the design source and dirNative's explicit "amber selected colour". Note the difference from iOS light mode in the canvas note.
4. **Press feedback on designed surfaces**: Android idiom is ripple; DS/iOS is scale 0.97 + opacity 0.85. *Default:* `cfPress` on designed pressables, ripple on M3 components.
5. **Font-scale cap value.** *Default:* 1.3 (≈ iOS xxLarge). The alternative is 1.15 (below xxLarge).
6. **Display P3.** *Default:* sRGB only, no wide-colour window mode; P3 values recorded in the report.
7. **Glass/blur.** *Default:* opaque surfaces, no backdrop blur. Optionally Haze on API 31+ for non-timeline footers after a frame-time check.
8. **`dayTitle` line height**: tokens 18/28 vs iOS planDayTitle 18/26 (from the Plan board). *Default:* 18/26, as iOS.
9. **Fraunces 500 cuts**: unused on iOS. *Default:* ship them anyway (parity, ~220 KB); drop later if still unused.
10. **More button glyph**: iOS "•••" vs Android `more_vert`. *Default:* `more_vert` (Android idiom).
11. **Bedtime picker 5-minute steps** without native Compose wheels. *Default:* M3 `TimePicker` dial with rounding to 5 min on Done. The alternative is a `NumberPicker` wheel pair.
12. **Toast**: designed floating toast (iOS FeedbackToast) or M3 Snackbar. *Default:* M3 Snackbar with inverse token colours.
13. **Live Activity on Android**: ongoing notification / Live Updates. *Default:* out of scope for the design system; handled in the reminders/notifications research.
14. **Content icon set**: lucide (boards/web) vs Material Symbols. *Default:* lucide for content and categories, Material Symbols Rounded for system chrome.
15. **Adaptive icon horizon crop**: Android shows less of the orange horizon than iOS. *Default:* ship the supplied layers unchanged.
16. **Medium-impact haptic** has no exact Android type. *Default:* `LongPress`; alternatively `Confirm`.
17. **minSdk** (affects adaptive icons at 26+, the splash API at 31, themed icons at 33, RenderEffect at 31). *Default assumed here:* minSdk ≥ 26, with nothing in the design system needing more. This is decided in the platform research.

## 14. Risks

- **Compose `dropShadow` fidelity** (blur-radius semantics, negative spread) is unverified against CSS. Shadows may look heavier or lighter than the boards. Mitigation: a screenshot comparison of one card per theme against the board render before building more cards.
- **Gradient banding** on the dark sky in 8-bit sRGB. Mitigation: native `Paint` dithering if bands show.
- **Font first-use stall** on cold start (resource fonts load synchronously). Mitigation: warm the font cache off-main; measure with Macrobenchmark.
- **The M3 role mapping is unusual** (M3 `primary` = `dawnText`, not token `primary`). Developers may reach for `MaterialTheme.colorScheme.primary` on designed surfaces and get amber instead of indigo. Mitigation: a lint/detekt rule or code-review checklist: designed code reads only `CfTheme`, and `MaterialTheme.colorScheme` is referenced only in `CfSystemControls.kt`.
- **No Android boards**: every system-chrome screenshot is "derived". The designer may later redraw and require changes (OQ 1).
- **fontScale cap through `attachBaseContext`** interacts with per-app language handling (`AppCompatDelegate.setApplicationLocales`). If AppCompat is used, both overrides must be combined in one `createConfigurationContext`.
- **Fraunces lacks "→"**: any Fraunces string with an arrow mixes in a system-font glyph. Trip routes in Fraunces should draw the arrow as an icon or set it in Figtree.
- **lucide version drift**: the icon (newer Moon path) and the launch moon (older Moon path) already differ in source. Pin one lucide version for UI icons and keep the icon/splash assets as supplied.
- **Out-of-gamut clipping** differs from the RN app (culori chroma reduction), so RN screenshots are not a valid colour oracle. Use iOS `TokenGenReport.md`.
