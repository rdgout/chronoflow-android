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

System overrides: checked switch track = dawn and thumb = dawnForeground; progress and refresh fill = dawn; active segment = systemSelected; sheet = card, 38 dp top corners, 640 dp maximum width and the default Material handle; alert = popover; Snackbar = foreground with background text and action. Date/time controls retain their native structure. The Material 3 version has a public `TimePickerDialog`; no custom dialog fallback is needed. Its 1.4.0 implementation draws a default Surface over its `containerColor` background (confirmed in the cached library bytecode). The wrapper scopes only the Material `surface` role to popover for that dialog, and passes popover to the picker colors. The root disables tonal color elevation explicitly: Material can replace the alpha of a transparent surfaceTint, which otherwise darkens a default surface. Native shadows remain native. `TimePickerTintTest` checks the actual dialog edge pixels in both themes.

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

The first layout run exposed harness assumptions that do not describe native controls. Centered text rounds both line edges independently (measured Dutch button: text width 241 px, line edges 167/409 px); the text guard permits that one physical pixel, without permitting exceeded lines. Small buttons use 6 dp vertical padding to preserve the bundle's 32 dp visual height, with a separate 48 dp target; the brief's 8 dp padding would make them 36 dp. Other sizes retain 8 dp.

Material segments intentionally share their 1 dp border, and the clock dial's circular targets overlap as rectangles. They are exempt from the inappropriate rectangle-overlap assertion. Native dialog coordinates are window-local, so activity-inset assertions are not applied to them; device captures verify their native rendering. Whole-display system-chrome placement review remains in Phase 14 (OQ-A42). The sheet width is checked directly against 640 dp on the wide cases. Date cells may distribute one physical pixel unevenly; only native calendar targets allow that rounding. The sheet handle retains its Material drawing and gets a 48 dp minimum interactive modifier. Skeleton ink uses a 2-channel-level threshold (muted/background differs by less than the general guard's 16); small native windows use a 0.1% ink floor rather than a whole-page 0.5% floor. Blank renders still fail.

Worker visual review, 2026-11-17: all 13 Robolectric gallery sections in all four language/theme combinations were inspected (four-column contact sheets and full-size Fields/Buttons/Banners plus dialog images). All 13 corresponding device contact sheets were also inspected. The corrected Sheet/TimePicker sections were recaptured after review; a further Dutch calendar review identified the activity-locale issue described below. Comparisons use the named boards' `.dc.html` source values, not a pixel overlay between different renderers. Contact sheets are local evidence under `build/phase2-validation/contact/` and `build/phase2-validation/contact-device/`.

| Board | Verified result / intentional difference |
|---|---|
| `Main` | Dawn and OnSky use the token fills, 52 dp minimum height and 16 sp 600/500 labels. The neutral gallery background makes the translucent OnSky fill less visible than on the board sky. Sky samples are separately checked; no dots or shadow are added. |
| `AuthStates` | Error-banner fill/border, 18 dp radius, 18 dp circle-alert and stacked 14/20 title/message match the source. Dutch wraps naturally. Fields have no fill; error rows wrap without ellipsis. The caller can suppress announcements as on iOS. |
| `SetStates`, `SetHome`, `Set2FA` | Row-card radius 22, full-width dividers, 36 dp discs, 16 dp glyphs, 20 dp chevrons and disabled opacity 0.5 match. Lock and chevron can coexist. Selected pills include the iOS check absent on the board. Badges use the board fills and outlines. |
| `SheetStates` | Skeleton sizes/radii and low-contrast muted fill match. Skeletons are static because iOS wins over the pulse in the bundle. Small buttons retain a 32 dp visual box with a 48 dp target. |
| `ItemDetail`, `SheetStates` | Sheet surface is card with 38 dp top corners. The handle, dialog buttons, calendar, clock dial, switch, segmented buttons and refresh indicator are native Material; their geometry intentionally differs from the drawn iOS chrome. |
| `SetStates` toast | The implementation uses the specified native Snackbar instead of the designed toast. Its action is background-colored in both themes to avoid the dark-theme dawn contrast failure. |

The OQ-2-22 title check uses two additional light-only captures (`make captures ONLY=measurements/phase2`, 8.15 s, two PNGs). The Dutch gallery title “Wachtwoord wijzigen” fits without truncation in both the large and small native app bars on small-nl and largeFont-nl (root cap 1.3). Both PNGs were viewed: `build/captures/measurements/phase2/Bars_{small,largeFont}_nl_light.png`. The title remains single-line/ellipsizable for longer feature-owned titles; those belong to their screen guards. These measurement captures live outside the `designsystem/` prefix, whose 13 × 4 set remains exactly 52 PNGs.

All native windows rendered on Robolectric, so no `DEVICE_ONLY` exemption or skipped capture section was needed. Device captures additionally verify their native rendering. The initial review caught and corrected a gray time-picker surface, English sheet copy and native calendar headings in the Dutch gallery, and the standalone spinner inheriting black in dark mode. The gallery resolves sheet copy before entering the native window because that window supplies a fresh view context. Device captures additionally set and restore the real application locale before launching their activity so Material calendar headings use the same locale as the weekday grid; the real app's locale remains an activity/root configuration policy. No screenshot is asserted for its copy.

Source-priority differences:

- `Main`: no pattern-dots layer (user decision); no card shadow.
- `SetStates`: selected pills include the iOS checkmark; the board has none.
- `SheetStates`: skeletons are static, as on iOS; the board bundle pulses.
- `ItemDetail` and `SheetStates`: Android uses native Material sheet chrome and the Material handle, retaining the designed card surface and 38 dp top corners from iOS/ItemDetail.
- `AuthStates`: the caller can suppress the banner live region because auth owns its announcer; the default remains polite for standalone usage.

## Brief reconciliation

The brief predates M0/M1. This phase extends the existing 0b generated tokens and 1c `tools/icons` pipeline, retains `make vectors`, and uses 0e's tiers, `ProfileRule`, `LayoutMatrix`, `Guards` and `BoardCapture`. It does not replace those systems. The Material Symbols source is `@material-symbols/svg-400` 0.48.2, published 2026-10-27, selected with the two-week cutoff on 2026-11-17 and verified against npm's archive hash. Lucide remains the boards' 0.511.0 shapes.

## Validation

Worker, 2026-11-17, Apple M4 Max; JVM native graphics SDK 36 and pixel API 36, as indicated. Test commands ran once per group, followed by scoped fixes only. Initial failures in color-rule parsing, native-control geometry assumptions and picker surface tint were corrected and their affected methods passed on the first scoped fix for each failure. No hang or two-fix stop threshold occurred.

| Evidence | Result / wall time |
|---|---|
| Phase design-system targeted classes | 24 tests passed; remaining groups and fixes recorded below |
| Konsist raw-color parser regression | scoped method passed, 29.84 s including recompilation |
| Layout/harness scoped fixes | 26 cases passed, 23.02 s |
| RootOverrides parity/release and seeded native-control test | passed, 71.07 s including builds; native test 3.35 s |
| Time-picker surface pixels | both themes passed, 5.47 s |
| `make captures ONLY=designsystem` | 52 PNGs, 57.50 s; affected sections recaptured after fixes |
| Final `make captures DEVICE=1 ONLY=designsystem` | 52 PNGs, 273.11 s wall including device-lock wait (Gradle 28 s); slowest case 0.604 s |
| `make captures DEVICE=1 ONLY=Nope` | passed, 0 PNGs, 36.19 s |
| `SNAPSHOT_RECORD=1 make snapshots` | passed, 8.29 s; exactly the seven requested design-system references added |
| `make baseline-profile` | passed, 467.87 s; outputs committed bfa817f |
| `make perf` | exited 0, 44.41 s; median 95.8 ms, M1-relative 5% acceptance **passed** |
| `make perf-timeline` | passed, 289.96 s; metrics and report-only trend below |
| Standalone `make quick` | failed budget checks, 15.30 s: spacing rule 1193 ms, route rule 1436 ms |
| Scoped rule-budget fix | six methods passed, 3.44 s; spacing rule 267 ms, route rule 2 ms, all planted/control cases pass |
| `make check` quick prerequisite | passed: 876 budget checks, zero over budget, 6.12 s main tier + 1.87 s build logic + tooling + 2.49 s timing |

The user selected M1's 95.0 ms Partial(Require) as the like-for-like startup reference in review round 1: 95.8 ms is +0.8% and passes the 5% limit. The older 0e 88.7 ms result used Partial(UseIfAvailable) and remains historical evidence. See `docs/perf-startup.md` and OQ-A66; emulator measurements are not device claims. The timeline benchmark passed in 289.96 s: first-frame median/max 88.8/97.5 ms, CPU P50/P90/P99 3.3/6.5/8.8 ms, RSS peak 103.2 MiB, zero main-thread builds and placeholders. Replay max 2.9 ms is a retained +262% report-only trend. See `docs/perf-timeline.md` for the matching-workload comparison and archive.

The saved before/after statuses for all three read-only sibling repositories are identical. Their pre-existing untracked files remain untouched. No Android-only copy was added. All exact command output is retained locally in `build/phase2-*.log`; timing/result XML is additionally written by the 0e tier harness. An optional sandbox-blocked process diagnostic is recorded verbatim in `docs/tooling.md`.

The boundary-rule fix scans only candidate feature files for annotated Metrics objects and skips the route hierarchy walk when no NavKey root/reference exists. It changes no limits or exemptions. The single `make check` invocation then stopped on `ModifierFactoryExtensionFunction` for `fieldSemantics` (53.52 s total). The helper was converted to an equivalent Modifier extension (8098dfd), formatted and committed; the remaining exact Makefile steps resumed from lint/release without another top-level quick/check invocation. The resumed steps all passed in **44.40 s** total: lint/release/manifest/R8 10.06 s (27 release tests); layout 19.40 s (197 tests); generator checks passed; emulator smoke 8.79 s (10 tests, slowest 0.742 s). All budget checks were green. Local evidence is retained under `build/phase2-validation/final-gates/` and `build/phase2-check-resumed.log`.

The final StrictMode review found an existing smoke-test-only disk read: `ScreenTestHarnessTest` called `Activity.filesDir` inside `onActivity`. c42aa25 moves that read to the instrumentation test worker. Its exact method passed on the first scoped rerun (5.36 s wall, 0.466 s test time), and `adb logcat -d -T '11-17 09:03:22.000' -s StrictMode` covering the new 09:04:59–09:05:00 run was empty. The earlier violations and clean follow-up are retained in `build/phase2-strictmode{,-after-fix}.log`. No StrictMode policy was relaxed.

Review round 1: OQ-A67 assigns themed status-bar icon appearance to Phase 3a with enableEdgeToEdge (research 08 row 24). The observed white icons on the current light surface remain a documented 3a hand-off; this phase does not add competing window policy.
