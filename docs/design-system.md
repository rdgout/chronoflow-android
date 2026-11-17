# Design system (Phase 2)

Behaviour is ported from iOS `f391a1fa`; designed surfaces use the current `_design/canvas/` sources. Colors, metrics and font faces come from the committed generated tokens. Material components keep Material typography, shapes, accessibility and interaction behaviour. Dynamic color, Expressive, backdrop blur and icon fonts are not used.

## Sources and public primitives

| Primitive | Board | Pinned iOS source under `Packages/ChronoFlowKit/` |
|---|---|---|
| `ChronoFlowTheme`, `CfType`, shapes, spacing | token set, `Main`, `Plan` | `Sources/DesignSystem/Typography.swift`, generated tokens |
| `cfPress`, `cfClickable`, focus ring | `theme.css` | `Sources/DesignSystem/PressStyle.swift`, `AuthControls.swift` |
| `CfSky`, `skyBackground` | `Main` | `Sources/DesignSystem/CfSky.swift` |
| `softShadow` | `AuthStates`, `SetStates` | `Sources/Auth/AuthCardShadowLayer.swift`, `Sources/Settings/SettingsBoardComponents.swift` |
| `CfButton`, `CfIconButton`, `CfSpinner` | bundle Button, `Main`, `ItemDetail`, `SheetStates` | `Sources/DesignSystem/AuthControls.swift`, `Buttons.swift` |
| `CfTextField`, `CfSecureTextField` | auth boards | `Sources/DesignSystem/AuthControls.swift` (`AuthField`) |
| `CfCard`, `CfDivider`, `CfRow` | `SetStates`, `SetHome`, `Set2FA` | `Sources/Settings/SettingsBoardComponents.swift` |
| `CfBadge`, `CfPill`, `CfPillGroup` | `SetStates`, `ItemDetail` | `SettingsBadge`, `SettingsRadioGroup` |
| `CfBanner`, `CfNotice` | `AuthStates`, `SetStates` | `AuthBanner`, `AuthNotice` |
| `CfSkeleton` | `SheetStates`, `SetStates` | `SettingsLoadingRows` |
| `CfTopAppBar`, `CfLargeTopAppBar`, `CfBackButton` | Material mapping; no Android boards | iOS plan navigation-title decision: system font, bold, collapsing |
| `CfSwitch`, `cfToggleable`, `CfSegmentedButtons` | Material mapping | native Toggle / Picker |
| `CfLinearProgress`, `CfPullToRefreshBox` | Material mapping | native ProgressView / refresh |
| `CfModalBottomSheet`, `CfAlertDialog`, `CfSnackbarHost`, picker dialogs | Material mapping, `ItemDetail` | native sheets, alerts and pickers; Snackbar is Android's replacement for FeedbackToast |

Each Fraunces optical size has one cached family. The generator has already resolved optical sizes; Kotlin does not select them a second time. Figtree is one family with four weights. Designed text excludes Android font padding and uses centered line height with no trim. The root's 1.3 font-scale cap applies unchanged.

`LocalSafeInsets` and `ellipsizable()` moved from `:core:ui` to `:core:designsystem`; there are no aliases. The design system has no production dependency on another project module. Tests needing resources, profiles or the gallery live in `:core:ui` to avoid a test dependency cycle.

## Material role mapping

| Material roles | Generated token |
|---|---|
| primary, secondary | dawnText |
| onPrimary, onSecondary | primaryForeground (light), dawnForeground (dark) |
| primary/secondary/tertiaryContainer | systemSelected |
| onPrimary/onSecondary/onTertiaryContainer | dawnText |
| tertiary / onTertiary | dawn / dawnForeground |
| background, surface / onBackground, onSurface | background / foreground |
| surfaceVariant / onSurfaceVariant | muted / mutedForeground |
| surfaceContainerLowest, Low | card |
| surfaceContainer, High, Bright | popover |
| surfaceContainerHighest | muted |
| surfaceDim | surface2 |
| error / onError | destructive / destructiveForeground |
| errorContainer / onErrorContainer | authErrorBackground / cardForeground |
| outline / outlineVariant | mutedForeground / border |
| inverseSurface / inverseOnSurface / inversePrimary | foreground / background / dawn |
| surfaceTint / scrim | transparent / black (Material applies alpha) |
| all primary/secondary/tertiary Fixed and FixedDim | systemSelected |
| all on-Fixed and on-FixedVariant | dawnText |

The landed Material 3 1.4.0 exposes 48 color roles. `ToM3Test` checks every public color getter, so a newly added role fails until mapped. Designed code reads `CfTheme`, not Material color roles.

System overrides: checked switch track = dawn and thumb = dawnForeground; progress and refresh fill = dawn; active segment = systemSelected; sheet = card, 38 dp top corners, 640 dp maximum width and the default Material handle; alert = popover; Snackbar = foreground with background text and action. Date/time controls retain their native structure. The Material 3 version has a public `TimePickerDialog`; no custom dialog fallback is needed.

## Contrast: KNOWN_GAPS

`ContrastTest` uses WCAG relative luminance on encoded sRGB channels, compositing alpha first. Text requires 4.5:1, UI 3:1. The tests also require each accepted gap below to remain a gap, so a token update prompts review.

| Pair | Light | Dark | Default |
|---|---|---|---|
| input / background | 1.42 | 1.63 | accepted board border |
| border / background | 1.33 | 1.51 | decorative |
| border / card | 1.42 | 1.40 | decorative |
| input / card | 1.51 | 1.51 | accepted board border |
| dawn / card | 2.55 | passes | OQ-2-7: retain fill |
| dawn / background | 2.40 | passes | OQ-2-7: retain fill |
| dawn / muted | 2.17 | passes | OQ-2-7: retain fill |
| dawn / popover | 2.55 | passes | OQ-2-7: retain fill |
| dawn / foreground | passes | 1.61 | OQ-2-5: Snackbar action uses background |

No token is changed to repair a board contrast gap. `dawnText` is used for text and selected checks; `dawn` is a fill.

## Motion, focus, haptics and secrets

Press feedback uses one graphics layer with scale 0.97, alpha 0.85, 120 ms and the generated cubic Bézier curve. Reduce Motion snaps alpha and keeps scale 1. Spinners remain animated; skeletons are always static, as on iOS. Material controls keep their native motion and ripple.

Keyboard focus rings occupy the 0–2 dp band outside a control. Text fields use the same geometry, no fill, an above-field label, native editing/autofill and a designed border. The secure field has no eye button. Callers own field state; secret states must never be saveable.

Font preloading waits for the first frame to commit and then runs on IO. Reduce Motion is resolved on activity creation and resume, with debug-only seed parsing. Haptics are behind `Haptics`; only switch/toggle and segmented controls fire haptics here. Launch and Welcome are guarded against haptics by Konsist.

## Shadow fidelity

The oracle is iOS `AuthCardShadowTests.cardShadowMatchesBoardInkBelowAndBesideTheCard`, with the `AuthShadowBoard` scene: 375 × 812 dp at 3×, card at (16, 300), size 343 × 428, radius 26. Samples are below (555, 2188, 11, 31) and beside (1080, 1495, 41, 11), in pixels. Board targets are light RGB (186.01, 126.20, 110.77) / (95.55, 77.76, 125.51), dark (106.94, 58.82, 46.92) / (35.00, 26.12, 59.03). Desired channel residual ≤ 4; minimum red ink below/beside is light 8/1, dark 20/2.

Worker, 2026-11-17, host Apple M4 Max, Robolectric native graphics SDK 36, density 3, debug unit-test build. Command: `make targeted ONLY=core:designsystem:SoftShadowFidelityTest` (8 measurement cases, 0 failures; 10.09 s wall, 5.431 s test time). These are JVM render measurements, not device claims.

| Theme | Candidate | Below RGB / red ink | Beside RGB / red ink | Maximum channel residual |
|---|---|---|---|---|
| Light | a-css | 185.94, 126.72, 111.51 / 13.15 | 96.01, 78.49, 125.65 / 2.10 | 0.74 |
| Light | b-half | 191.64, 129.93, 112.52 / 7.45 | 97.98, 79.67, 126.92 / 0.13 | 5.63 |
| Light | c-css | 185.94, 126.72, 111.51 / 13.15 | 96.01, 78.49, 125.65 / 2.10 | 0.74 |
| Light | c-half | 191.64, 129.93, 112.52 / 7.45 | 97.98, 79.67, 126.92 / 0.13 | 5.63 |
| Dark | a-css | 105.91, 58.70, 46.97 / 28.76 | 34.65, 26.34, 58.16 / 3.83 | 1.03 |
| Dark | b-half | 115.64, 64.07, 51.28 / 19.02 | 37.99, 28.85, 63.82 / 0.48 | 8.70 |
| Dark | c-css | 105.91, 58.70, 46.97 / 28.76 | 34.65, 26.34, 58.16 / 3.83 | 1.03 |
| Dark | c-half | 115.64, 64.07, 51.28 / 19.02 | 37.99, 28.85, 63.82 / 0.48 | 8.70 |

**Chosen: A (`a-css`)**, the first passing mapping: radius 30/40 dp, spread −12/−14 dp, Y offset 10/12 dp, generated shadow color/alpha. C gives identical pixels, proving Compose honors negative spread here. B and C-half miss the required ink. No extra inset shape or device fallback is needed.

Chosen signed residuals (actual − board), RGB: light below (−0.07, +0.52, +0.74), beside (+0.46, +0.73, +0.14); dark below (−1.03, −0.12, +0.05), beside (−0.35, +0.22, −0.87). Both samples meet their minimum ink in each theme. The strict regression test now checks the production modifier against the board targets and ink minima.

Crops: [light](design-system/shadow-light.png), [dark](design-system/shadow-dark.png), source region (0, 1986, 1125, 450) px. The `AuthStates` and `SetStates` CSS both use this one `shadow-soft` token. The crops show the rounded card bottom and soft ink below it; neither is a `Main` card.

## Guards, captures and board differences

Geometry uses `LayoutMatrix.phone`: small-nl, largeFont-nl, pixel8-en, light only. Wide native-window checks additionally use foldInner-en and landscapeSideNav-en. Captures cover en/nl and light/dark separately. Each parameter is an individual test. No screen test asserts copy. Stored JVM references are opt-in and compare JVM with JVM only.

Gallery captures and visual comparison are pending. Intended source-priority differences, to verify in the captures:

- `Main`: no pattern-dots layer (user decision); no card shadow.
- `SetStates`: selected pills include the iOS checkmark; the board has none.
- `SheetStates`: skeletons are static, as on iOS; the board bundle pulses.
- `ItemDetail` and `SheetStates`: Android uses native Material sheet chrome and the Material handle, retaining the designed card surface and 38 dp top corners from iOS/ItemDetail.
- `AuthStates`: the caller can suppress the banner live region because auth owns its announcer; the default remains polite for standalone usage.

## Brief reconciliation

The brief predates M0/M1. This phase extends the existing 0b generated tokens and 1c `tools/icons` pipeline, retains `make vectors`, and uses 0e's tiers, `ProfileRule`, `LayoutMatrix`, `Guards` and `BoardCapture`. It does not replace those systems. The Material Symbols source is `@material-symbols/svg-400` 0.48.2, published 2026-10-27, selected with the two-week cutoff on 2026-11-17 and verified against npm's archive hash. Lucide remains the boards' 0.511.0 shapes.

Validation results are pending; no performance or fidelity claim is made until the corresponding measurement is recorded.
