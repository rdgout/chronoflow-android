# Phase 2 brief: Theme, primitives and system-control tinting

- **Branch:** `feat/phase-2-design-system`, from the stack top once 0b and 0e are in it. **Wave 3**: it runs beside 1a, 4a and 9a in its own worktree, so keep edits to shared files (`plan.md`, `Makefile`, `Scripts/test_scripts.sh`, `konsist/…/KonsistRulesTest.kt`, `ModuleGraphTest.kt`) small and on their own lines.
- **Depends on:** 0b (tokens, fonts), 0e (harness). It also reads 0c's generated strings in its guard tests (OQ-2-1). If 0b or 0e is missing from the stack, stop and report.
- **Plan entry:** `plan.md` "### Phase 2: Theme, primitives and system-control tinting" (l.1227–1289). Where this brief and the plan differ, the brief lists the difference as an open question (§9) with its default; build on the default.
- **Reader:** a Sonnet implementer who has not seen the research. **[F]** = fact verified in a source for this brief (path given). **[R]** = decision of the plan or this brief.
- **Conflict order** (AGENTS.md): user decision > iOS at `f391a1fa` > boards > web > RN.
- **API endpoints:** none. **Network:** one npm download in task 10 (Material Symbols), nothing else.
- **Revision 2026-11-16:** re-checked against the revised 0a/0b/0c/0e briefs (0e OQ-0e-18, OQ-0e-20) and the consumers 3a, 3c, 5a, 6c, 6d, 9c. Changes: tier annotations live in `net.chronoflow.core.testing.tiers` (one file each); `ProfileRule` takes `AppLanguage`; the Reduce Motion seed uses 0a's `BuildConfig.DEBUG` pattern (task 5); `CfSky`/`softShadow` take `dark` explicitly (task 8); `CfButton.contentPadding`, `CfCard.border`, `CfBanner.announce` added for 3c/5a (OQ-2-24); captures extend 0e's `BoardCapture` (task 18).
- **Check 2026-11-16 (adversarial pass against iOS `f391a1fa`, boards, plan, 0b/0c/0e briefs):** plan line references corrected; `theme.css` path; module-cycle rule for `:core:designsystem` tests and the test-location split (task 0, §5); press effect as an `IndicationNodeFactory` so `cfClickable` needs no source (task 7, OQ-2-27); focus ring band 0–2 dp outside (iOS); pixel tests moved from `@Logic` to `@Layout`; fixed M3 roles made explicit; three more `KNOWN_GAPS`; `CfRow` `lock` + `chevron` (iOS, `Set2FAManage`); pill check (OQ-2-26); SheetStates skeleton sizes; `BoardCapture` device detection; gallery/device test locations.

---

## 1. Goal

Every later screen draws itself with:
1. `ChronoFlowTheme` + `CfTheme.colors / type / shapes / spacing / reduceMotion`, built only from 0b's generated tokens;
2. `toM3()`, so Material 3 system components are tinted by tokens (M3 `primary` = `dawnText`, not token `primary`);
3. `Modifier.cfPress` / `cfClickable`, `Modifier.softShadow`, `Modifier.skyBackground`;
4. the designed primitives `CfButton`, `CfTextField`/`CfSecureTextField`, `CfCard`, `CfDivider`, `CfRow`, `CfPill`/`CfPillGroup`, `CfBadge`, `CfBanner`, `CfNotice`, `CfSkeleton`, `CfSpinner`;
5. thin `Cf*` wrappers over M3 system components (top app bars, switch, segmented buttons, linear progress, pull to refresh, bottom sheet, alert dialog, snackbar host, date/time picker dialogs) that **only pass colors and defaults**;
6. generated lucide and Material Symbols `ImageVector`s;
7. a `Haptics` interface;
8. Konsist rules that keep `MaterialTheme.colorScheme`, raw colors, Material icons and loose dp out of feature code.

Phase 2 also closes Risk 19 (shadow fidelity) with a measured comparison against the iOS shadow oracle, and it implements `make captures DEVICE=1`, which 0e assigned here (0e OQ-0e-18).

**No screen is built.** Tests and captures render primitives alone.

---

## 2. What the iOS work gives us (reuse, do not re-derive) [F]

Read iOS files at the pin only: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`. `K/` = `Packages/ChronoFlowKit/Sources/`.

| iOS source | Reused for | Key facts |
|---|---|---|
| `K/DesignSystem/PressStyle.swift`, test `Tests/DesignSystemTests/PrimitiveTests.swift` `pressValuesRespectReduceMotion` | task 7 | `values(isPressed, reduceMotion)` = scale `pressed && !rm ? 0.97 : 1`, opacity `pressed ? 0.85 : 1`, duration `rm ? 0 : 0.12 s`; no animation under Reduce Motion |
| `K/DesignSystem/AuthControls.swift` (`AuthButton`, `AuthField`, `AuthBanner`, `AuthNotice`, `AuthGlyph`) | tasks 11, 12, 14 | anatomy below; all numbers come from `AuthTokens`. `AuthButton.Kind` = `primary, destructive, secondary, outline, ghost, footer`; padding h `spacer` 24 / v `fieldGap` 8; `footer` min height `max(footerHeight 48, minimumHeight)`; disabled/busy opacity `disabledOpacity`. `AuthBanner` omits the message when it is empty and has **no** live region of its own |
| `K/DesignSystem/Buttons.swift` (`WelcomeButton`, kinds `primary`/`secondary`) | task 11 | the Welcome pills: `dawn`/`dawnForeground` with `Typography.primaryButton`, `skyCopyBackground`/`skyCopy` with `secondaryButton`; padding h `WelcomeTokens.cardHorizontalPadding` (20) / v `WelcomeTokens.brandGap` (8); min height `WelcomeTokens.buttonHeight` (52); full width; `PressStyle`. 3c builds these with `CfButton(Dawn/OnSky, minHeight, textStyle, contentPadding)` |
| `K/DesignSystem/AuthTokens.generated.swift` | all primitives | touch 44, fieldGap 8, brandGap 6, inset 12, spacer 24, icon 16, fieldRadius 12, noticeRadius 22, noticeIconTop 2, bannerGap 10, bannerVertical 12, bannerHorizontal 14, bannerRadius 18, bannerGlyph 18, bannerIconTop 1, cardRadius 26, cardPadding 20, cardGap 14, footerHeight 48, borderWidth 1, focusWidth 2, disabledOpacity 0.5, lightShadowY 10 / Radius 15 / Spread −12, darkShadowY 12 / Radius 20 / Spread −14 |
| `K/DesignSystem/SettingsTokens.generated.swift` | task 13 | rowHeight 64, rowVertical 10, rowDisc 36, pillHeight 44, pillGap 6, selectionWidth 2, sheetRadius 38, toastHeight 52, disabledOpacity 0.45 (password prompt only) |
| `K/Settings/SettingsBoardComponents.swift` (`SettingsCard`, `SettingsDivider`, `SettingsRow`, `SettingsBadge`, `SettingsRadioGroup`, `SettingsSoftShadow`, `SettingsLoadingRows`) | tasks 8, 13, 14 | anatomy below; static skeleton (no pulse anywhere on iOS) |
| `K/DesignSystem/Typography.swift` | task 2 | the hand-written styles (`planCardTitle`, `planDayTitle`, `planRemoteTitle` on Fraunces 400 opsz 20; `status`, `welcomeWordmark`, `eyebrow`, `headline`, `copy`, `primaryButton`, `secondaryButton`, `splashWordmark`) |
| `K/DesignSystem/CfSky.swift` | task 8 | vertical gradient, stops = `SkyRamp`, no mask/filter/shadow |
| `K/Auth/AuthCardShadowLayer.swift`, `Tests/AuthTests/AuthCardShadowTests.swift`, `Tests/SnapshotSupport/AuthShadowBoard.swift`, `Docs/auth-shadow-round-3.md`, `Docs/auth-shadow-round-4.md` | task 9 | the shadow oracle (geometry, sample rects, board means, tolerance 4, minimum ink) |
| `Tests/DesignSystemTests/AuthBoardContrastTests.swift` | task 4 | test names `subtitleAndBannerTokenPairsPassAAInBothAppearances`, `createAccountTextContrastAdaptationKeepsDawnTextOnBothCards` |
| `K/DesignSystem/FeedbackToast.swift` | not built | Android uses the M3 Snackbar (plan OQ-A5) |
| iOS `plan.md:25` "Decisions", row D1b at l.30 | all wrappers | system controls only through the platform; never a hand-drawn imitation of glass |
| iOS `plan.md:59` "Navigation titles (user decision 2026-11-10)" | task 15 | every tab and Settings title is the plain system large title: system font, bold, standard size, collapses on scroll |
| iOS `plan.md:141–164` | task 8 | sky reference samples (below) |
| iOS `plan.md:169` (`cf-press` spec) and `plan.md:277–283` "Reduce Motion" | tasks 2, 5, 7 | `cf-press`: scale 0.97, opacity 0.85, 120 ms `cubic-bezier(0.25, 0.1, 0.25, 1)`; Reduce Motion: opacity 0.85, scale 1, no transition (`theme.css:143-146`) |
| `Docs/settings-board-fidelity.md` | tasks 8, 13, 16 | "Shadow-soft reuses the already-generated Auth shadow color and light/dark geometry"; native Toggle, Picker, DatePicker, alert keep their system look |

---

## 3. Inputs to read (in this order)

**This repo**
1. `AGENTS.md` (Tests, Commands, Git, Traps — especially the `toM3()` trap and "Measured sizes stay in layout").
2. `plan.md`: Phase 2 (l.1227–1289); A9 (l.96); "Android spec: Material mapping" (l.414–448: element table l.416–427, `toM3()` l.429–437, haptics l.439–448); "configuration and appearance" (l.348–355); window policy (l.357–371, `sheetMaxWidth = 640.dp`); spec table row "### Reduce Motion" (l.261); Risks 18–19 (l.567–568); OQ-A3, A5, A6, A9, A42 (l.570–618; the last number in use is OQ-A43); standard acceptance #1–7 (l.636–646).
3. `docs/research/08-design-system.md` §2.2, §4.2–4.3, §5.3, §6, §7, §9.1–9.3, §10.1 (rows 3–5, 8–12, 15–19), §12.
4. **0b output** (read before writing code; never edit by hand — if a value is missing, extend `:tools:tokengen` and run `make generate`):
   - `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/generated/CfColors.generated.kt`: `@Immutable class CfColors(val isDark: Boolean, …52 Color fields)`, `CfLightColors`, `CfDarkColors`. Field names: the 42 base tokens camelCased (`skyTop … dawnText`) + `authErrorBackground, authErrorBorder, authShadow, settingsWarningBackground, settingsWarningBorder, settingsQrBackground, wizardNoticeBackground, wizardNoticeBorder, systemSelected, wizardPickerFill`.
   - `…/generated/CfSky.generated.kt`: `class SkyStop(location: Float, color: Color)`, `object SkyRamp { light; dark }` (33 stops each), `object PressTokens { scale = 0.97f; opacity = 0.85f; durationMillis = 120; curve = [0.25, 0.1, 0.25, 1] }`, `object LaunchPaint`.
   - `…/generated/CfTypeScale.generated.kt`: `enum class CfFace` (10 faces), `class CfTypeSpec(face, size, lineHeight: Float?, letterSpacingEm)`, `object TypeMetrics`, `object PlanTypeMetrics`, `object CfTypeScale` (12 `screen*`, 20 composite, 7 `auth*`, 9 `settings*`, 3 `terms*`). **The Fraunces optical size is already resolved in `CfFace`** (generator rule ≥ 40 → opsz 48, ≥ 26 → 32, else 20); letter spacing is already in em.
   - `…/generated/CfDimens.generated.kt`: `object PlanUITokens` (`space1…space16`, `radiusSm…radius3Xl`, `radiusFull = 9999`, …), `object AuthTokens`, `object SettingsTokens`, `TermsTokens`, `WelcomeTokens` — all `const val Float` in dp/sp/ratio (0b OQ-0b-5: Phase 2 wraps them in `Dp`).
   - `core/designsystem/src/main/res/font/`: `figtree_{400,500,600,700}.ttf`, `fraunces_{400,500}_opsz{20,32,48}.ttf` (confirm the exact names in the folder).
5. **0e output** (`docs/phases/phase-0e-test-harness.md` T1, T5–T9, T11):
   - tier annotations in package **`net.chronoflow.core.testing.tiers`**, one file each in `core/testing/src/main/kotlin/net/chronoflow/core/testing/tiers/`: `Smoke.kt` (0a), `Logic.kt`, `Layout.kt`, `Snapshot.kt`, `Capture.kt`, `Perf.kt`, `PerfBudget.kt`. There is no `Tiers.kt` and no `…testing.tier` package (OQ-0e-20); never add an alias.
   - `DeviceProfiles` + `core/testing/src/main/resources/profiles.json` (`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`).
   - `:core:testing-compose` (namespace `net.chronoflow.core.testing.compose`; it exposes `:core:testing` and `:core:ui` as `api`): `ProfileRule(profile: DeviceProfile, language: AppLanguage = AppLanguage.En, dark: Boolean = false)`, `ProfileHost(profile) { }`, `object Guards`, `BoardCapture.capture(rule, name)`.
   - `AppLanguage { En("en"), Nl("nl") }` in `net.chronoflow.core.model.config` (OQ-0e-10). The generated wire enum `Language` (0d) is a different type: never pass it to `ProfileRule`.
   - `core/ui/…/LocalSafeInsets.kt`, `core/ui/…/semantics/Ellipsizable.kt` (moved by task 0).
   - `app/src/debug/kotlin/net/chronoflow/debug/TestSeed.kt`: `TestSeed.fromIntent(intent).reduceMotion: Boolean?` (`cf.reduceMotion`: `"1"`/`"true"` → true, `"0"`/`"false"` → false, else null).
   - the Makefile `captures` target: Robolectric path done; `DEVICE=1` prints `make captures DEVICE=1 arrives in Phase 2 (feat/phase-2-design-system)` and exits 1 (OQ-0e-18; task 18 replaces it).
6. **0c output**: `core/ui/src/main/res/values{,-nl}/strings_ios.xml`, `tools/l10n/names.lock.json`. Look resource names up in the lock (names below were computed with 0c's slug rule; the lock wins).
7. **0a output**: `konsist/src/test/kotlin/net/chronoflow/konsist/{KonsistRules,KonsistRulesTest,ModuleGraphTest,ProjectPaths}.kt` and the planted-violation layout `konsist/src/test/resources/{violations,allowed}/<rule>/…`.

**Boards** (`_design/canvas/`, read-only):

| Board | What Phase 2 takes from it [F] |
|---|---|
| `Main` (Welcome) | sky (`cf-sky`); Dawn pill (`height 52; bg dawn; dawn-foreground; 16px 600`) and OnSky pill (`bg sky-copy-background; sky-copy; 16px 500`); old lucide moon path `M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z`. **No shadow.** Its `pattern-dots` layer is removed by the user (2026-11-06): never draw it. |
| `AuthStates` | card `padding 20; radius 26; 1px border; card; shadow-soft`; banner `gap 10; padding 12px 14px; radius 18; bg mix(destructive 10%, card); border mix(destructive 35%, transparent)`, 18 px `circle-alert` destructive `margin-top 1px`, title 14/20 600 block + message 14/20, all `card-foreground` |
| `SetStates` | notice (`ChronoFlow.Notice tone=info`); offline-disabled row (`min-height 64; padding 10px 16px; gap 12; opacity 0.5`, 36 px muted disc with a 16 px glyph, title 16/24 500) with the hint "Needs a connection." **below the card**; row card `radius 22; border; card; overflow hidden`, dividers `border-top: 1px solid border` full width; badges `secondary` "Uit"; pills `min-height 44; padding 0 16px; radius 9999; border 2px border (unselected) / dawn (selected); card; 14px 500 / 600`, wrap gap 8; designed toast (not built, OQ-A5) |
| `SheetStates` | sheet panel `padding 12px 20px 24px; gap 14; radius 26; card`, grabber `36 × 5`, skeleton bars (`ChronoFlow.Skeleton`, radii 10 / 9999 / 12 / 6), primary and outline buttons at `height 48`, iOS alerts (→ `CfAlertDialog`; their copy belongs to 6d) |
| `ItemDetail` | sheet `border-radius 38px 38px 0 0; background card`, grabber `36 × 5 rgba(120,120,128,0.4)`, scrim `oklch(0.15 0.03 272 / 0.5)`; badges `dawn` "Now", `secondary` "Done", `outline` "Skipped"; buttons `outline lg` "Skip" / "Undo" and default `lg` "Mark as done" with a 16 px check (stroke 2.5) |
| `theme.css` (at `_design/canvas/theme.css`, **not** under `ds/chronoflow/`) | `.cf-press` (l.134–146: 0.97 / 0.85 / 120 ms; reduced motion: no transition, no transform) |
| `ds/chronoflow/components/bundle.js` (lucide-react **v0.511.0**) | `Button` variants `default/secondary/outline/ghost/link/destructive/dawn`, sizes `sm h-8 px-3`, `default h-9 px-4`, `lg h-10 px-6`, `icon size-9`, `touch size-11`, label `text-sm font-medium`, spinner `size-4` before the children, `disabled:opacity-50`; `Badge` variants `default/secondary/outline/dawn` `px-2 py-0.5 text-xs font-medium rounded-full border`; `Notice` `rounded-2xl border p-3 gap-3 text-sm`, warning `border-dawn/40 bg-dawn/10`; `Skeleton` `animate-pulse rounded-md bg-muted`; 30 icon nodes (task 10) |

Also read `SetHome.dc.html` for the row chevron (20 px `m9 18 6-6-6-6`, muted-foreground) and `Set2FA.dc.html` for the `lock` glyph. Never build boards listed under plan.md "Never build these".

---

## 4. Out of scope

| Item | Owner |
|---|---|
| Tab bar / `NavigationSuiteScaffold` and its tint (OQ-A3) | 6a |
| Refresh capsule | 6a |
| Day `DropdownMenu` | 6c |
| Splash, launch overlay, `LaunchPaint` use | 3a |
| Welcome composition and its intro (the Dawn / OnSky button styles are built here) | 3c |
| Auth shell, sign-up "Show password" switch row | 5a, 5d |
| Feature-owned components: `DayHeader`, `TodayRow`, `TripCard`, `ChoiceCard`/radio cards, `MetaChip`, `PriorityTag`, `CategorySwatch`, `StatusPill`, `DayProgress`, `SettingsEyebrow`, `SettingsReadOnlyField`, `SettingsLockHint`, offline/sync status lines | feature phases |
| Timeline glyph path data, patterns, category styles | 1c (it may reuse task 10's generator) |
| Wiring the `cf.settings.theme` preference (Phase 2 follows the system) | 10b |
| The designed `FeedbackToast` (SetStates toasts) | not built (OQ-A5) |
| Glass, blur, `MaterialExpressiveTheme`, dynamic color | never (OQ-A6) |
| An "offline" `CfBanner` variant | no in-scope board (OQ-2-14) |
| Captures of system chrome for designer review | 14 (OQ-A42); Phase 2 only provides the gallery |

---

## 5. Conventions for every task

- **Modules.** Production code in `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/<pkg>/` with `<pkg>` ∈ `theme`, `effect`, `haptics`, `icon`, `component`, `system`, `layout`, `semantics`. Primitives take **strings as parameters**; `:core:designsystem` contains no string resources.
- **Values.** Every number comes from a generated object (`PlanUITokens`, `AuthTokens`, `SettingsTokens`, `PressTokens`, `SkyRamp`, `CfTypeScale`, …) or is a board value written once in a `private object <Primitive>Metrics` annotated `@BoardMetrics("<Board>")` (task 17). No other dp literal.
- **Tests.** Tier annotations per 0e (`import net.chronoflow.core.testing.tiers.Logic` etc.): plain JVM tests have none; Robolectric classes carry exactly one of `@Logic` (behaviour, runs in `make quick`) or `@Layout` (pixels, guards; runs in `make check`); emulator classes `@Smoke` or `@Capture`. Robolectric classes: `@RunWith(AndroidJUnit4::class)` (or `ParameterizedRobolectricTestRunner`), `@GraphicsMode(GraphicsMode.Mode.NATIVE)`, `@Config(sdk = [36])`. Budgets: JVM < 1 s, Robolectric < 5 s, emulator < 10 s. Never `Thread.sleep`; use `mainClock`.
- **Where tests live.** `core/designsystem/src/test/kotlin/net/chronoflow/core/designsystem/…` for every test that needs only `:core:designsystem` + `:core:testing` (JVM tests, and Robolectric tests that set their own `@Config(qualifiers = …)`). Every test that uses `ProfileRule`, `ProfileHost`, `DeviceProfiles` profiles through `ProfileRule`, `Guards`, `BoardCapture`, `RecordingHaptics` or en/nl copy lives in `core/ui/src/test/kotlin/net/chronoflow/core/ui/designsystem/…` (and `core/ui/src/androidTest/kotlin/net/chronoflow/core/ui/designsystem/…` for emulator tests), because `:core:ui` holds the strings and may depend on `:core:testing-compose` (task 0 explains the cycle). Concretely in `:core:ui`: every `*GuardsTest`, `SystemWrappersTest`, `SystemDialogsTest`, `SystemTintTest`, `SystemDialogTintTest`, the gallery captures and any device fallback.
- **Guards** = one parameterised `@Layout` class per primitive, 8 cases: profiles `small`, `largeFont` × `AppLanguage.En`, `AppLanguage.Nl` × light, dark, through `@get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language, dark)).around(composeRule)` (`ParameterizedRobolectricTestRunner`, one parameter set per case) and `ProfileHost(profile) { ChronoFlowTheme { GuardHost { … } } }`. `GuardHost` (test helper in `core/ui/src/test/…/designsystem/GuardHost.kt`) = `Column(Modifier.fillMaxSize().background(CfTheme.colors.background).padding(LocalSafeInsets.current.asPaddingValues()).padding(horizontal = 16.dp).verticalScroll(…))`. Call `Guards.assertTextFits`, `assertTouchTargets`, `assertInteractiveInSafeArea`, `assertNoInteractiveOverlap` and `assertInk` (not `assertAll`, which needs a heading tag). Use the copy named in each task, in the case's language.
- **Commits.** One task = one Conventional Commit, lower-case imperative subject, **no trailer of any kind**. Generated files go in the same commit as the generator change. Tick the plan.md checkbox in the commit named under "Tick".

---

## 6. Tasks

### Task 0. Module direction and shared semantics (`refactor(ui): move safe insets and ellipsizable into designsystem`)

Why: primitives in `:core:designsystem` need `Modifier.ellipsizable()` (badge, collapsed title) and `LocalSafeInsets` (app-bar insets), but 0e put both in `:core:ui`, and `:core:ui` must depend on `:core:designsystem`, not the reverse (OQ-2-1).

- Move `LocalSafeInsets` to `net.chronoflow.core.designsystem.layout.LocalSafeInsets` and `Ellipsizable` + `Modifier.ellipsizable()` to `net.chronoflow.core.designsystem.semantics`. Keep their declarations byte-for-byte (same `SemanticsPropertyKey` name `"cf.ellipsizable"`, same error message). Update every import (`MainActivity`, `ProfileHost`, `Guards`, `GuardsSelfTest`, any 0e Konsist allow-list that names the file path). Leave no alias behind.
- `core/ui/build.gradle.kts`: `api(project(":core:designsystem"))` if absent. `:core:testing-compose` already exposes `:core:ui` as `api` (0e T7), so it sees the moved symbols transitively; add nothing there unless the build says otherwise.
- `core/designsystem/build.gradle.kts`: `testImplementation(project(":core:testing"))` (tier annotations, 0e T1) if the convention does not already add it. **Never** `testImplementation(project(":core:testing-compose"))` or `:core:ui` here: `:core:testing-compose` → `:core:ui` → `:core:designsystem` is a cycle. Tests that need `ProfileRule`, `ProfileHost`, `Guards`, `BoardCapture`, `RecordingHaptics` or strings live in `:core:ui` (§5).
- `ModuleGraphTest`: new rule `designSystemDependsOnNoProjectModule` — `core/designsystem/build.gradle.kts` contains no `project(":` other than the single allowed `testImplementation(project(":core:testing"))`. Planted violations under `konsist/src/test/resources/violations/moduleGraph/core/designsystem/build.gradle.kts`: `implementation(project(":core:ui"))` and `testImplementation(project(":core:testing-compose"))` (two files or two lines; each must fail). Allowed control: a file containing only `testImplementation(project(":core:testing"))`.
- **Tests:** existing 0e tests (`ProfileHostTest`, `GuardsSelfTest`) still pass; the new ModuleGraph rule passes on the tree and rejects its planted file.

### Task 1. Token inventory (`test(designsystem): pin the generated token inventory`)

Confirm 0b emitted everything this phase uses. If a value is missing, extend `:tools:tokengen` (port it from the cited iOS file) in this commit; never write it as a Kotlin literal.

`TokenInventoryTest` (JVM) asserts, per theme:

| Value | Light | Dark |
|---|---|---|
| `systemSelected` (mix(card, dawn, 0.16)) | `#FFFAECE3` | `#FF302D38` |
| `authErrorBackground` | `#FFFAEAEA` | `#FF262031` |
| `authErrorBorder` (destructive α0.35) | `#59CC2827` | `#59EF6661` |
| `settingsWarningBackground` | `#FFFBF2EE` | `#FF242433` |
| `settingsWarningBorder` (dawn α0.4) | `#66E78A45` | `#66F2AD73` |
| `authShadow` | `#40202955` | `#99000000` |
| `dawn` / `dawnText` / `dawnForeground` | `#E78A45` / `#A35303` / `#2D1205` | `#F2AD73` / `#F2AD73` / `#101428` |
| `card` / `popover` / `background` | `#FBFEFF` / `#FBFEFF` / `#F3F7FC` | `#12172B` / `#161B32` / `#0A0E1F` |

and: `AuthTokens.lightShadowY/Radius/Spread` = 10 / 15 / −12, dark = 12 / 20 / −14; `AuthTokens.cardRadius` = 26; `PressTokens` = 0.97, 0.85, 120, [0.25, 0.1, 0.25, 1]; `SkyRamp.light.size == SkyRamp.dark.size == 33`, a stop at exactly 0.55 (light) and 0.6 (dark), first stop `#1F285A`/`#06091C`, last `#EB9666`/`#AA5830`; `PlanUITokens.radiusSm…radius3Xl` = 10, 12, 14, 18, 22, 26; spacing tokens 4, 8, 12, 16, 20, 24, 32, 56, 64; `SettingsTokens.rowHeight/rowVertical/rowDisc/pillHeight/pillGap/selectionWidth/sheetRadius` = 64/10/36/44/6/2/38.

- **Edge:** if a generated name differs from this brief (for example `radius2Xl` vs `radiusXxl`), use the generated name everywhere and note it in the PR; never rename generated code.

### Task 2. Theme core (`feat(designsystem): add ChronoFlowTheme and locals`)

Package `…designsystem.theme`.

- **`CfType`** (`@Immutable class`):
  - `fun CfFace.fontFamily(): FontFamily` and `fun CfFace.fontWeight(): FontWeight`: one `FontFamily` for Figtree (`Font(R.font.figtree_400, W400)` … `figtree_700, W700`), and **one `FontFamily` per Fraunces optical size** (`Fraunces20 = FontFamily(Font(fraunces_400_opsz20, W400), Font(fraunces_500_opsz20, W500))`, same for 32 and 48). Families are top-level `val`s, created once.
  - `fun CfTypeSpec.toTextStyle(): TextStyle` = `TextStyle(fontFamily, fontWeight, fontSize = size.sp, lineHeight = lineHeight?.sp ?: TextUnit.Unspecified, letterSpacing = letterSpacingEm.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), platformStyle = PlatformTextStyle(includeFontPadding = false))`.
  - `CfType` exposes `fun style(spec: CfTypeSpec): TextStyle` (memoised in an `IdentityHashMap` filled at construction for every spec in `CfTypeScale`, `TypeMetrics`-based and `PlanTypeMetrics`-based styles; `TypeMetrics`/`PlanTypeMetrics` entries are `CfTypeMetrics(size, lineHeight, letterSpacingEm)` without a face, so build `CfTypeSpec(face, m.size, m.lineHeight, m.letterSpacingEm)` once per named style below) and named properties for the iOS `Typography.swift` styles, ported by name: `planCardTitle`, `planDayTitle`, `planRemoteTitle` (face `Fraunces400Opsz20` with `PlanTypeMetrics` metrics), `planTitle`, `planSubtitle`, `planLane`, `planCompactDate`, `planCompactWeekday`, `planListTitle`, `planBlockLabel`, `planNow`, `planPriority`, `planListLine` (face `PlanTypeMetrics.<name>Face`), `splashWordmark` (Fraunces400Opsz32, `TypeMetrics.splashWordmark`), `status` (Figtree400, `body`), `welcomeWordmark` (Fraunces400Opsz20, `sectionTitle`), `eyebrow` (Figtree600, `eyebrow`), `headline` (Fraunces400Opsz48, `hero`), `copy` (Figtree400, `bodyLg`), `primaryButton` (Figtree600, `button`), `secondaryButton` (Figtree500, `button`); plus shortcuts used here: `authLabel`, `authStrong`, `authHint`, `authBody`, `settingsRow`, `settingsPill`, `settingsBadge`, `screenBody`, `screenLabel`.
  - `fun TextStyle.tnum(): TextStyle = copy(fontFeatureSettings = "tnum")`.
  - No `relativeTo`: Android scales every style by the root-capped font scale (0e `ConfigurationPolicy`).
- **`CfShapes`**: `sm, md, lg, xl, x2l, x3l` = `RoundedCornerShape(PlanUITokens.radiusSm.dp …)`, `full = RoundedCornerShape(50)`; `sheetTop = RoundedCornerShape(topStart = SettingsTokens.sheetRadius.dp, topEnd = …)`. Compose corners are circular, iOS uses continuous corners for some shapes: accepted.
- **`CfSpacing`**: `s1, s2, s3, s4, s5, s6, s8, s14, s16` = `PlanUITokens.space1 … space16` (4, 8, 12, 16, 20, 24, 32, 56, 64) as `Dp`; there is no `s7`, `s9`–`s13` or `s15` token.
- **Reduce Motion**: `object ReduceMotion { fun resolve(animatorsEnabled: Boolean, override: Boolean?): Boolean = override ?: !animatorsEnabled }`.
- **Locals** (all `staticCompositionLocalOf`, default `error("… is provided by ChronoFlowTheme")`, except `LocalReduceMotion` default `false`): `LocalCfColors`, `LocalCfType`, `LocalCfShapes`, `LocalCfSpacing`, `LocalReduceMotion`, `LocalHaptics` (task 6 fills it).
- **`@Composable fun ChronoFlowTheme(darkTheme: Boolean = isSystemInDarkTheme(), reduceMotion: Boolean = LocalReduceMotion.current, content: @Composable () -> Unit)`** provides the locals, plus `LocalRippleConfiguration provides RippleConfiguration(color = colors.foreground)` and `LocalTextSelectionColors provides TextSelectionColors(handleColor = colors.dawnText, backgroundColor = colors.dawn.copy(alpha = 0.4f))`, and wraps `MaterialTheme(colorScheme = colors.toM3(), content = content)` with **M3 default typography and shapes** (system components keep the system font and M3 corners, as iOS keeps system fonts). The `toM3()` result is computed once per theme (`remember(colors)` or two top-level `val`s). No dynamic color.
- **`object CfTheme`**: `colors`, `type`, `shapes`, `spacing`, `reduceMotion` as `@Composable @ReadOnlyComposable` getters.
- **Tests:**
  - `CfTypeTest` (JVM): `CfFace.Fraunces400Opsz48.fontFamily()` is the opsz-48 family and `Fraunces500Opsz20.fontWeight() == W500`; `CfTypeScale.authLabel.toTextStyle()` = Figtree 500, 14.sp/20.sp, 0.em; a spec with `lineHeight == null` gives `TextUnit.Unspecified`; `planDayTitle` uses `Fraunces400Opsz20` and line height 26 (OQ-A9); `tnum()` sets `"tnum"`.
  - `ReduceMotionTest` (JVM): the four combinations plus `override = false` with animators off → `false`.
  - `ChronoFlowThemeTest` (`@Logic`): in light and dark, `CfTheme.colors === CfLightColors/CfDarkColors`, `MaterialTheme.colorScheme.primary == dawnText`, `LocalRippleConfiguration.current?.color == foreground`, `LocalTextSelectionColors.current.handleColor == dawnText`, `LocalReduceMotion` is passed through.

### Task 3. `toM3()` (`feat(designsystem): map tokens to material 3 roles`)

`fun CfColors.toM3(): ColorScheme` in `…designsystem.theme`, built with `lightColorScheme(...)`/`darkColorScheme(...)` by `isDark`, every role passed explicitly. [F plan l.429–437, 08 §9.1]

| Role | Token |
|---|---|
| `primary`, `secondary` | `dawnText` |
| `onPrimary`, `onSecondary` | light `primaryForeground`, dark `dawnForeground` |
| `primaryContainer`, `secondaryContainer`, `tertiaryContainer` | `systemSelected` |
| `onPrimaryContainer`, `onSecondaryContainer`, `onTertiaryContainer` | `dawnText` |
| `tertiary` / `onTertiary` | `dawn` / `dawnForeground` |
| `background`, `surface` / `onBackground`, `onSurface` | `background` / `foreground` |
| `surfaceVariant` / `onSurfaceVariant` | `muted` / `mutedForeground` |
| `surfaceContainerLowest`, `surfaceContainerLow` | `card` |
| `surfaceContainer`, `surfaceContainerHigh`, `surfaceBright` | `popover` |
| `surfaceContainerHighest` | `muted` |
| `surfaceDim` | `surface2` |
| `error` / `onError` / `errorContainer` / `onErrorContainer` | `destructive` / `destructiveForeground` / `authErrorBackground` / `cardForeground` |
| `outline` / `outlineVariant` | `mutedForeground` / `border` |
| `inverseSurface` / `inverseOnSurface` / `inversePrimary` | `foreground` / `background` / `dawn` |
| `surfaceTint` | `Color.Transparent` |
| `scrim` | `Color.Black` (components apply their own alpha) |
| `primaryFixed`, `primaryFixedDim` / `onPrimaryFixed`, `onPrimaryFixedVariant` (and the same for `secondary*`, `tertiary*`), if present in material3 1.4.0 | the matching container role / on-container role: `systemSelected` / `dawnText` (M3 fixed roles are container tones). If `lightColorScheme()` in 1.4.0 has no fixed parameters, set them with `.copy(…)` |

- **Test** `ToM3Test` (JVM): for both themes, every row above. It also lists all public `Color`-typed getters of `ColorScheme` by reflection and fails if one is not in the expected map, so a new M3 role cannot go unmapped. `Color` is an inline class, so use Java reflection: `ColorScheme::class.java.methods.filter { it.parameterCount == 0 && it.returnType == Long::class.javaPrimitiveType && it.name.startsWith("get") }`, role name = `name.removePrefix("get").substringBefore('-')` with the first letter lower-cased (material3 1.5.0-beta01 has 48 such getters; 1.4.0 may have 36).

### Task 4. Contrast (`test(designsystem): check wcag contrast of token pairs`)

`ContrastTest` (JVM). WCAG 2.x relative luminance on 8-bit sRGB (`c ≤ 0.04045 ? c/12.92 : ((c+0.055)/1.055)^2.4`), ratio `(L1+0.05)/(L2+0.05)`. Opaque pairs only; for an alpha color, composite it over its background first.

- **Must pass, both themes** (threshold 4.5 text, 3.0 UI): foreground on background (16.12/16.12), card, popover; cardForeground on card (17.11/14.93) and on authErrorBackground (14.88/13.25); mutedForeground on background (10.91/8.29), card, muted (9.87/6.96), popover; dawnText on background (5.14/10.04), card (5.46/9.30), popover, systemSelected (4.79/7.06); onPrimary on dawnText (light primaryForeground 5.23); dawnForeground on dawn (6.76/9.54); destructive on card (5.31/5.70) and popover; destructiveForeground on destructive (5.07/6.33); secondaryForeground on secondary; skyTitle on skyTop and skyMid; every M3 on-role on its role from task 3; ring on background at 3.0 (4.64/10.04).
- **Ported iOS tests** (same names):
  - `subtitleAndBannerTokenPairsPassAAInBothAppearances`: skyTitle/skyTop, skyTitle/skyMid, cardForeground/card, cardForeground/authErrorBackground ≥ 4.5; skyTitle against the linear interpolation skyTop→skyMid at 1,001 steps ≥ 4.5.
  - `createAccountTextContrastAdaptationKeepsDawnTextOnBothCards`: sRGB values (not the iOS P3 ones): light dawn/card 2.5523, dawnText/card 5.4592; dark both 9.2975 (±0.001); dawnText/card ≥ 4.5; in light dawn/card < 4.5. This guards why `primary = dawnText`.
- **`KNOWN_GAPS`**: a list asserted to **still fail**, each with its OQ. If a token change fixes one, the test fails until the entry is removed.

| Pair | Light | Dark | OQ |
|---|---|---|---|
| input on background (field border) | 1.42 | 1.63 | accepted board design (08 §4.3) |
| border on background | 1.33 | 1.51 | decorative |
| border on card (card border, unselected pill border) | 1.42 | 1.40 | decorative / accepted board design (08 §4.3) |
| input on card (field border and outline button inside a card) | 1.51 | 1.51 | accepted board design (08 §4.3) |
| dawn on card (selected pill border, SetStates; switch track on a card) | 2.55 | — | OQ-2-7 |
| dawn on background | 2.40 | — | OQ-2-7 |
| dawn on muted (progress fill on track) | 2.17 | — | OQ-2-7 |
| dawn on popover (pull-to-refresh arc) | 2.55 | — | OQ-2-7 |
| dawn on foreground (would-be snackbar action) | — | 1.61 | OQ-2-5 (avoided) |

A failing board pair is **never** fixed by changing a token; add it here with an OQ.

Tick **"Material mapping"** in this commit.

### Task 5. Root wiring: Reduce Motion and font warm-up (`feat(app): provide the theme, reduce motion and font warm-up at the root`)

- `…designsystem.theme.FontWarmup.kt`: `suspend fun warmFonts(preload: suspend (FontFamily) -> Unit, dispatcher: CoroutineDispatcher)` calls `preload` for `Figtree`, `Fraunces20`, `Fraunces32`, `Fraunces48` inside `withContext(dispatcher)`. (`FontFamily.Resolver` is sealed, so tests pass a lambda; production passes `resolver::preload`.)
- `MainActivity` root (keep 0e's `LocalSafeInsets`/`testTagsAsResourceId` wrapper): wrap the content in `ChronoFlowTheme(reduceMotion = reduceMotion)`. Add `LaunchedEffect(Unit) { withFrameNanos { }; warmFonts(resolver::preload, io) }` with `resolver = LocalFontFamilyResolver.current` captured outside the effect and `io` = the app's IO dispatcher. Phase 3a moves this call after `first-commit`.
- **Reduce Motion source:** `var reduceMotion by mutableStateOf(…)` in `MainActivity`, set in `onCreate` and again in `onResume` to `ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), RootOverrides.reduceMotion(intent))` (static call, no binder, no disk).
  - `app/src/main/kotlin/net/chronoflow/app/RootOverrides.kt`: `internal object RootOverrides { fun reduceMotion(intent: Intent?): Boolean? = if (BuildConfig.DEBUG) parseFlag(intent?.getStringExtra("cf.reduceMotion")) else null; internal fun parseFlag(raw: String?): Boolean? }` with `parseFlag` = `"1"`/`"true"` → true, `"0"`/`"false"` → false, anything else → null (the 0e `TestSeed` rule).
  - Why this shape (OQ-2-23): `src/main` cannot see `src/debug` (`net.chronoflow.debug.TestSeed`), and the perf build types do not inherit `src/release` (0a OQ-0a-1), so per-build-type files would need three copies. `BuildConfig.DEBUG` is a compile-time constant; R8 removes the branch and the extra name from release, as 0a proves for `DebugStrictMode`. 3a's `LaunchOverrides` may later absorb this; keep the function name.
- **Tests:**
  - `FontWarmupTest` (JVM, `runTest(timeout = 60.seconds)`, `StandardTestDispatcher`): the four families are requested exactly once, on the given dispatcher (record `currentCoroutineContext()[CoroutineDispatcher]` in the lambda).
  - `RootThemeTest` (`@Logic`, `app/src/test`): launching `MainActivity` after `ShadowValueAnimator.setDurationScale(0f)` (`org.robolectric.shadows`; the real `ValueAnimator.setDurationScale` is hidden API) provides `LocalReduceMotion == true`; no preload happens before the first frame (inject a recording preload through a `@VisibleForTesting` hook on the activity).
  - `RootOverridesParityTest` (plain JVM, `app/src/testDebug/kotlin/net/chronoflow/app/`): for raw values `"1"`, `"true"`, `"0"`, `"false"`, `"yes"`, `""`, `null`, `RootOverrides.parseFlag(raw) == TestSeed.parse(mapOf("cf.reduceMotion" to raw)).reduceMotion` (skip the `null` key for `TestSeed`; expect null). This keeps the two parsers identical.
  - `RootOverridesReleaseTest` (plain JVM, `app/src/testRelease/kotlin/net/chronoflow/app/`, runs in `testReleaseUnitTest`): `RootOverrides.reduceMotion(intent with cf.reduceMotion=1) == null`.
Tick **"Theme"** in this commit.

### Task 6. Haptics (`feat(designsystem): add the haptics interface`)

Package `…designsystem.haptics`.

- `enum class HapticEvent { Selection, ToggleOn, ToggleOff, Light, Success, Warning, Error, Medium }`; `fun interface Haptics { fun perform(event: HapticEvent) }`; `object NoHaptics : Haptics`.
- `class AndroidHaptics(private val feedback: HapticFeedback) : Haptics` maps [F plan l.439–448, 08 §12]:

| Event | `HapticFeedbackType` |
|---|---|
| Selection | `SegmentTick` |
| ToggleOn / ToggleOff | `ToggleOn` / `ToggleOff` |
| Light | `ContextClick` |
| Success | `Confirm` |
| Warning, Error | `Reject` |
| Medium | `LongPress` (OQ 16 of 08) |

- `ChronoFlowTheme` reads `val feedback = LocalHapticFeedback.current` and provides `LocalHaptics provides remember(feedback) { AndroidHaptics(feedback) }`. Compose maps these types to older-API constants itself; do not add version branches.
- `class RecordingHaptics : Haptics` (records events) in `:core:testing-compose` main.
- iOS fires haptics only in the wizard (`TripWizardUI/*`), so **no primitive here fires one except `CfSwitch`/`cfToggleable` and `CfSegmentedButtons`** (system-control parity).
- **Test** `AndroidHapticsTest` (JVM, fake `HapticFeedback`): each event → its type.

Tick **"Haptics"** after task 17 adds its Konsist rule.

### Task 7. Press and focus (`feat(designsystem): add the cfpress modifier node`)

Package `…designsystem.effect`.

Why this shape (OQ-2-27): consumers call `cfClickable(onClick, Role.Button)` with no interaction source (3c, 5a, 6a, 6c, 7, 8, 9c), and a plain modifier factory cannot `remember` one; a `composed {}` or `@Composable` factory is banned (ktlint compose rules). Foundation's `Modifier.clickable(interactionSource = null, indication = <IndicationNodeFactory>)` creates the source lazily and hands it to the factory, so the press effect is an `IndicationNodeFactory` whose node draws the scale and alpha in the **draw phase**.

- `fun pressValues(isPressed: Boolean, reduceMotion: Boolean): PressValues` (`data class PressValues(scale: Float, alpha: Float, durationMillis: Int)`), port of `PressStyle.values`, from `PressTokens`.
- `object CfPressIndication : IndicationNodeFactory` (`create(interactionSource)` returns `CfPressNode(interactionSource)`; `equals`/`hashCode` = identity of the object). `CfPressNode : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode`:
  - `onAttach`: create one `GraphicsLayer` with `requireGraphicsContext().createGraphicsLayer()` (release it in `onDetach`), and launch on the node's `coroutineScope` a collector of `interactionSource.interactions` that tracks `PressInteraction.Press/Release/Cancel` and animates an `Animatable(0f)` "pressed progress" to 1/0 with `tween(PressTokens.durationMillis, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`;
  - `ContentDrawScope.draw()`: `val p = progress.value` (read **only here**, so a press invalidates draw only: no recomposition, no relayout), then `layer.record { this@draw.drawContent() }`, `layer.scaleX = layer.scaleY = lerp(1f, PressTokens.scale, p)`, `layer.alpha = lerp(1f, PressTokens.opacity, p)`, pivot = centre (the layer's default), `drawLayer(layer)`. No allocation per frame;
  - Reduce Motion (`currentValueOf(LocalReduceMotion)` read when the press arrives): scale stays 1 (`scaleX/Y = 1`) and the alpha progress `snapTo`s with no animation;
  - a disabled `clickable` emits no `PressInteraction`, so disabled is never pressed.
- `fun Modifier.cfPress(interactionSource: InteractionSource): Modifier = indication(interactionSource, CfPressIndication)`, for callers that already own a source (for example an M3-free custom gesture).
- `fun Modifier.cfClickable(onClick: () -> Unit, role: Role = Role.Button, enabled: Boolean = true, onClickLabel: String? = null, interactionSource: MutableInteractionSource? = null, focusShape: Shape = RectangleShape): Modifier` = `cfFocusRing(focusShape)` then `clickable(interactionSource = interactionSource, indication = CfPressIndication, enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick)`. Designed pressables never show a ripple (08 §9.2); M3 components keep theirs. Primitives pass their shape as `focusShape` (`CfButton` the pill, `CfPill` the pill, `CfRow` a rectangle).
- `fun Modifier.cfFocusRing(shape: Shape): Modifier` = a `ModifierNodeElement` whose node is `FocusEventModifierNode + DrawModifierNode`: `onFocusEvent` stores `isFocused` and calls `invalidateDraw()`; `draw()` draws the content and, while focused, an `AuthTokens.focusWidth` (2 dp) `ring` stroke in the band **0–2 dp outside** the bounds (the shape's outline grown by 1 dp, stroke width 2 dp: iOS `inset(by: -focusWidth).strokeBorder(ring, lineWidth: focusWidth)`, `AuthControls.swift`; the bundle's `outline-offset-2` loses to iOS). No layout change. It must come **before** the focus target (the `clickable`) in the chain to receive its focus events. Compose focuses a `clickable` only from a keyboard, D-pad or switch access, never from a touch, so `isFocused` alone is the condition.
- **Tests:**
  - `CfPressValuesTest` (JVM): `pressValuesRespectReduceMotion` — (pressed, normal) = (0.97, 0.85, 120); (pressed, RM) = (1, 0.85, 0); (released, normal) = (1, 1, 120); (released, RM) = (1, 1, 0); curve = [0.25, 0.1, 0.25, 1].
  - `CfPressTest` (`@Layout`, pixels; `@Config(qualifiers = "w411dp-h914dp-420dpi")`): a 100 × 100 dp box filled `foreground` on a `background` page, with `cfClickable`; `mainClock.autoAdvance = false`; `performTouchInput { down(center) }`; advance 120 ms; `captureToImage()`: the run of non-background pixels on the centre row is 97 dp ± 1 px wide, and the centre pixel = `lerp(background, foreground, 0.85)` ± 2 per channel; `up()`, advance 120 ms → 100 dp ± 1 px and the full fill. Under `LocalReduceMotion provides true`: after `down` and one frame (`mainClock.advanceTimeByFrame()`), the run is 100 dp ± 1 px and the centre pixel is already the 0.85 blend.
  - `CfPressRecompositionTest` (`@Logic`): a counter incremented in the content's composition is unchanged across press, 120 ms and release.
  - `CfFocusRingTest` (`@Layout`, same qualifiers): after `requestFocus()` in keyboard mode (`InputModeManager.requestInputMode(InputMode.Keyboard)`), the pixel 1 dp outside the box edge is `ring` ± 2; unfocused it is the background.

### Task 8. Soft shadow and sky (`feat(designsystem): add the soft shadow and sky brush`)

- **[F]** There is one shadow token, `shadow-soft`: light `0 10px 30px -12px` `authShadow`, dark `0 12px 40px -14px` `authShadow`. iOS uses it on cards (`SettingsSoftShadow`), the toast, auth and walkthrough/wizard checking cards; never on the timeline, sticky day headers or the compact line. The plan's "softShadow1/softShadow2" are its light and dark values (OQ-2-10).
- `…designsystem.effect.SoftShadow.kt`: `fun softShadowFor(dark: Boolean): Shadow` (two cached top-level `val`s) and `fun Modifier.softShadow(shape: Shape, dark: Boolean): Modifier = this.dropShadow(shape, softShadowFor(dark))`, built on Compose UI 1.12.1 `Modifier.dropShadow(shape: Shape, shadow: Shadow)` with `Shadow(radius, color = authShadow.copy(alpha = 1f), spread, offset = DpOffset(0.dp, y), alpha = authShadow.alpha)`. Callers pass `CfTheme.colors.isDark` (a theme change recomposes them anyway). No `composed {}`, no `@Composable` modifier factory (ktlint compose rules), no `Modifier.shadow(elevation)`.
  - Start mapping (task 9 decides the final one): `radius = 2 × AuthTokens.<theme>ShadowRadius` (the CSS blur: 30 / 40), `spread = AuthTokens.<theme>ShadowSpread`, `y = AuthTokens.<theme>ShadowY`.
- `…designsystem.effect.CfSky.kt`: `object CfSky { fun brush(dark: Boolean): Brush; fun colorAt(dark: Boolean, t: Float): Color }` (name as iOS `CfSky.swift` and as 3c expects). `brush` = `Brush.verticalGradient(colorStops = SkyRamp.<theme>.map { it.location to it.color }.toTypedArray())`, two cached `val`s; `colorAt` = linear interpolation of encoded sRGB channels over the stops (tests and 3a's frame-0 parity). `fun Modifier.skyBackground(dark: Boolean): Modifier` draws the brush full-size with `drawBehind`. No blur, no dots, no mask. (0b's `CfSky.generated.kt` declares `SkyStop`/`SkyRamp`/`PressTokens`/`LaunchPaint`, not `CfSky`, so the names do not clash.)
- **Tests:**
  - `SkyRampSamplesTest` (JVM): `CfSky.colorAt` at t = 0.0, 0.1 … 1.0 equals the iOS reference samples (iOS `plan.md:161–164`) within ±1/255 per channel — light `#1F285A #272D61 #303167 #38366E #403B75 #493F7C #5D4C7F #7E607E #A1737A #C58572 #EB9666`; dark `#06091C #0A0C22 #0F0F27 #13112D #181434 #1D173A #221A40 #402B42 #623B41 #854A3C #AA5830`.
  - `SkyBrushRenderTest` (`@Layout`, `core/designsystem/src/test`, pixel8 geometry through `@Config(qualifiers = "w411dp-h914dp-420dpi")`, light and dark): a full-screen `skyBackground(dark)` box; the centre-column pixel at row `round(t·(H−1))` matches the same samples within ±2/255. If dark-sky banding exceeds that, switch to a `ShaderBrush` whose paint dithers and record it in `docs/design-system.md`.
  - `SoftShadowTest` (JVM): the `Shadow` parameters per theme.

Tick **"Effects"** in this commit.

### Task 9. Shadow fidelity (`test(designsystem): measure the soft shadow against the ios oracle`)

Port iOS `AuthCardShadowTests.cardShadowMatchesBoardInkBelowAndBesideTheCard` + `AuthShadowBoard` as `SoftShadowFidelityTest` (`@Layout`, `core/designsystem/src/test`), light and dark.

- **Scene:** `RuntimeEnvironment.setQualifiers("w375dp-h812dp-xxhdpi")` (density 3 → 1125 × 2436 px). Full-size `skyBackground(dark)`; a card at `x 16, y 300, w 343, h 428` dp, shape `RoundedCornerShape(AuthTokens.cardRadius.dp)`, `.softShadow(shape, dark).background(card, shape)`. Render a second time without the shadow.
- **Samples** (pixel rects `x, y, w, h`; mean of R, G, B over the rect) [F `AuthShadowBoard.swift`]:

| Sample | Rect | Light board mean | Dark board mean | Minimum red ink light / dark |
|---|---|---|---|---|
| below | 555, 2188, 11, 31 | 186.01, 126.20, 110.77 | 106.94, 58.82, 46.92 | 8 / 20 |
| beside | 1080, 1495, 41, 11 | 95.55, 77.76, 125.51 | 35.00, 26.12, 59.03 | 1 / 2 |

- Pass = every channel within **4** levels of the board mean, and red(unshadowed) − red(shadowed) ≥ the minimum ink.
- If the plain sky at a sample differs from iOS's sky by more than 2 levels, also assert on ink only and record why.
- **Mapping search** (record every candidate's means in a table): (a) `radius = CSS blur` (30/40) with negative spread; (b) `radius = blur/2` (15/20); (c) iOS's own geometry: shape inset by |spread| (radius `26 + spread` = 14/12 dp) with spread 0 and the radius from (a) or (b). Keep the first that passes; if none passes, keep the closest and record the residual.
- If Robolectric's native graphics draws no `dropShadow` (ink = 0 in both samples), move the assertions to `SoftShadowFidelityDeviceTest` (`@Capture`, `core/ui/src/androidTest/kotlin/net/chronoflow/core/ui/designsystem/`, so task 18's `:core:ui:connectedDebugAndroidTest` line runs it; scene in a 375 × 812 dp `Box`, samples scaled by the device density as iOS `Sample.mean` does) on the `pixel` AVD, run it with `make targeted ONLY="core:ui:SoftShadowFidelityDeviceTest"`, delete the Robolectric copy (AGENTS Tests 6) and record that.
- Create `docs/design-system.md` § "Shadow fidelity": the candidate table, the chosen mapping, the residual per channel, and one light and one dark PNG crop (`docs/design-system/shadow-{light,dark}.png`). Compare against the `AuthStates` and `SetStates` cards (`Main` has no shadow; OQ-2-11).

Tick **"Shadow fidelity"** in this commit.

### Task 10. Icons (`feat(tools): generate lucide and material symbols vectors`)

`tools/icons/generate_icons.py` (`python3 -I`, stdlib only; its `--check` mode regenerates into `build/` and diffs). Makefile, in the 0b/0c pattern (one `+=` line each, directly under 0a's definitions; no other target touched): `vectors:` = `python3 -I tools/icons/generate_icons.py`; `vectors-check:` (`.PHONY`) = the same with `--check`, then the `tools/icons` unittest discover below; `GENERATORS += vectors` and `GENERATOR_CHECKS += vectors-check`. **Do not name them `icons`**: 0b's `make icons` builds the launcher icons (`tools/assets/build_assets.py icons`).

**Lucide** — the boards' shapes, lucide-react **0.511.0** (ISC) [F bundle header]. 7 of the 30 bundle glyphs differ from lucide-static 1.66.0 (`calendar-check`, `circle-check`, `house`, `moon`, `settings`, `skip-forward`, `sparkles`), so the boards are the source, not a newer release (OQ-2-15).
- Manifest `tools/icons/lucide.json`: `{"version": "0.511.0", "glyphs": {"<name>": {"source": "bundle"} | {"source": "board", "board": "<Board>", "first": "<exact d or tag of the svg's first child>"}}}`.
  - `bundle`: read the icon node array `var X=[[…]],y=r("<name>",X)` from `_design/canvas/ds/chronoflow/components/bundle.js`.
  - `board`: take the first `<svg viewBox="0 0 24 24">` in `<Board>.dc.html` whose first child matches `first`; all its children.
  - An entry that matches nothing fails with its name and board.
- Initial entries: the 30 bundle glyphs (`armchair, arrow-left-right, bed, calendar-check, check, chevron-down, chevron-up, circle-alert, circle-check, cloud-sun, coffee, ellipsis, footprints, glasses, globe, house, info, lamp, loader-circle, map-pin, moon, plane, settings, shield-check, skip-forward, sparkles, sun, ticket, timer, triangle-alert`) + `chevron-right` (board `SetHome`, `m9 18 6-6-6-6`) + `lock` (board `Set2FA`, `<rect width="18" height="11" x="3" y="11" rx="2" ry="2">`). Later phases add entries for their boards.
- Convert `circle` (two arcs), `line`, `rect` (with `rx`/`ry` arcs), `polyline`, `polygon` (+`Z`) to path data in the 24-unit space; keep a per-element `filled` flag for `fill="currentColor"` children.
- Output `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/icon/LucideGlyphs.generated.kt`: `class LucideGlyph(val name: String, val paths: List<LucidePath>)`, `class LucidePath(val d: String, val filled: Boolean)`, `object LucideGlyphs { val Moon = LucideGlyph("moon", …) … }` (PascalCase names). No Compose import, so 1c can reuse the data.
- Hand-written `…/icon/Lucide.kt`: `fun LucideGlyph.imageVector(strokeWidth: Float = 2f): ImageVector` → `ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)` with one `addPath(addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = strokeWidth, strokeLineCap = Round, strokeLineJoin = Round, fill = if (filled) SolidColor(Color.Black) else null)` per path; cached per (glyph, stroke width). Callers tint with `Icon(tint = …)`. Boards draw `check` at stroke 2.5 in buttons and toasts (ItemDetail, iOS `AuthCheck`).

**Material Symbols Rounded** (system chrome only).
- Source: npm `@material-symbols/svg-400`, the newest version at least two weeks old on the day you run it; record the exact version. Download once (`curl -fsSLo … https://registry.npmjs.org/@material-symbols/svg-400/-/svg-400-<ver>.tgz`, verify the npm `shasum`), and vendor only the needed SVGs into `tools/icons/material-symbols/rounded/` with `LICENSE` (Apache-2.0) and `MANIFEST.sha256`.
- Glyphs: `arrow_back`, `close`, `more_vert`, `add`, `check`, `space_dashboard` (+`-fill`), `flight` (+`-fill`), `settings` (+`-fill`), `view_agenda`, `view_timeline`.
- viewBox `0 -960 960 960`: translate y by +960; emit `ImageVector` 24 × 24 dp, viewport 960 × 960, filled paths. Output `…/icon/MaterialSymbols.generated.kt` (`object MaterialSymbols { val ArrowBack: ImageVector by lazy { … } … }`).
- **No network in your sandbox:** skip this sub-step, keep the lucide part, and report the exact `curl` + `shasum` commands to the orchestrator (OQ-2-12). Task 15 then waits for `ArrowBack`.

**Both:** no icon font, no `material-icons-extended`, no `androidx.compose.material.icons` (Konsist, task 17). Record "lucide 0.511.0 (ISC), source: boards bundle and inline SVGs" and "Material Symbols Rounded `@material-symbols/svg-400` <ver> (Apache-2.0)" with the license texts in `docs/third-party.md`.

**Tests**
- `tools/icons/test_generate_icons.py`. Discovery is not generic: add one line to `Scripts/test_scripts.sh`, after 0c's `tools/l10n` line, `python3 -I -m unittest discover -s "$ROOT/tools/icons" -t "$ROOT/tools/icons" -p 'test_*.py'` (so `make quick` runs it); `vectors-check` runs the same discover. Cases: circle → `M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0` (or the equivalent your converter emits, asserted exactly); rect with rx; the viewBox flip; an unmatched board entry fails naming the board; two runs are byte-identical; `moon` = `M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z`.
- `LucideGlyphsTest` (JVM): every path parses with `PathParser().parsePathString(d)`; 32 glyphs; `LucideGlyphs.Moon.paths.single().d` is the board path.
- `MaterialSymbolsTest` (JVM): every vector is 24 dp with viewport 960.

Tick **"Icons"** in this commit.

### Task 11. Button and spinner (`feat(designsystem): add cfbutton and cfspinner`)

Package `…designsystem.component`. Mirrors iOS `AuthButton` + bundle `Button`.

```kotlin
@Composable fun CfButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    style: CfButtonStyle = CfButtonStyle.Primary, size: CfButtonSize = CfButtonSize.Default,
    enabled: Boolean = true, loading: Boolean = false, loadingDescription: String? = null,
    leadingIcon: ImageVector? = null, fillWidth: Boolean = false, minHeight: Dp = Dp.Unspecified,
    textStyle: TextStyle = CfTheme.type.authLabel, contentPadding: PaddingValues? = null,
)
@Composable fun CfIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: CfButtonStyle = CfButtonStyle.Ghost, size: CfButtonSize = CfButtonSize.Touch, enabled: Boolean = true)
```

| `CfButtonStyle` | Container | Content | Border | Source |
|---|---|---|---|---|
| `Primary` | primary | primaryForeground | — | bundle `default`, iOS `.primary` |
| `Secondary` | secondary | secondaryForeground | — | both |
| `Outline` | background | foreground | 1 dp `input` | both |
| `Ghost` | transparent | foreground | — | both (the plan's "quiet") |
| `Destructive` | destructive | destructiveForeground | — | both |
| `Dawn` | dawn | dawnForeground | — | bundle `dawn`, Main "Plan my trip" |
| `OnSky` | skyCopyBackground | skyCopy | — | iOS `.footer`, Main "Sign in" |

| `CfButtonSize` | Visual min height | Horizontal padding | Source |
|---|---|---|---|
| `Sm` | 32 | 12 | bundle `h-8 px-3` |
| `Default` | 36 | 16 | `h-9 px-4` |
| `Lg` | 40 | 24 | `h-10 px-6` |
| `Icon` | 36 × 36 | 0 | `size-9` |
| `Touch` | 44 (44 × 44 for `CfIconButton`) | 24 | `size-11`; iOS `AuthButton` (`AuthTokens.touch`, `spacer`) |

- Anatomy: pill (`CfTheme.shapes.full`); vertical padding `AuthTokens.fieldGap` (8) so wrapped text grows the button; row gap 8; spinner and icon 16 dp (`AuthTokens.icon`), spinner **before** the icon (iOS and bundle); label centred, wraps to more lines, **never ellipsized**; `minHeight` overrides the visual height (SheetStates 48, Main 52, iOS `footerHeight` 48); `contentPadding`, when given, replaces the size's horizontal padding and the 8 dp vertical padding (Welcome: 20 / 8 from `WelcomeTokens`). Welcome usage, for 3c and the guard: `CfButton(style = Dawn, minHeight = WelcomeTokens.buttonHeight.dp, fillWidth = true, textStyle = CfTheme.type.primaryButton, contentPadding = PaddingValues(horizontal = WelcomeTokens.cardHorizontalPadding.dp, vertical = WelcomeTokens.brandGap.dp))`, and `OnSky` with `secondaryButton`.
- Every size also gets `Modifier.minimumInteractiveComponentSize()` → ≥ 48 dp target, visual size unchanged.
- States: pressed = `cfClickable`; disabled or loading → alpha `AuthTokens.disabledOpacity` (0.5), not clickable, `semantics { disabled() }`; loading also sets `stateDescription = loadingDescription` (`require(loadingDescription != null)` when `loading`); focus ring on the pill shape.
- `CfSpinner(modifier, size: Dp = AuthTokens.icon.dp, color: Color = LocalContentColor.current)` = `CircularProgressIndicator(color, strokeWidth = 2.dp, trackColor = Color.Transparent)`; it keeps spinning under Reduce Motion (system affordance, 08 row 16). Hidden from accessibility.
- **Tests:**
  - `CfButtonTest` (`@Logic`): click fires once; no click while disabled or loading; `Sm` touch bounds ≥ 48 dp while its drawn height is 32 dp; loading exposes the state description and `disabled`.
  - `CfButtonColorsTest` (`@Layout`): container centre pixel and border pixel per style, light and dark (±2).
  - `CfButtonGuardsTest` (`@Layout`, 8 cases): every style at `Lg`, `Sm` and `Touch` with Screens "Mark as done" / "Markeer als gedaan" (`screens_mark_as_done`), "Skip" / "Overslaan" (`screens_skip`), "Undo" / "Ongedaan maken" (`screens_undo`), Auth "Sign in" / "Inloggen" (`auth_sign_in`), Welcome "Plan my trip" / "Plan mijn reis" (`welcome_plan_my_trip`, `Dawn`) and Welcome "Sign in" / "Inloggen" (`welcome_sign_in`, `OnSky`) in the Welcome usage above (52 dp); `fillWidth` and non-fill; one case with a `loading = true` button, a standalone `CfSpinner()` and a `CfIconButton` (`LucideGlyphs.Ellipsis.imageVector()`, content description Auth "Back" / "Terug": test copy only, any existing string works).

### Task 12. Text fields (`feat(designsystem): add the text field decoration`)

Mirrors iOS `AuthField` [F `AuthControls.swift`] on `BasicTextField(TextFieldState)`.

```kotlin
@Composable fun CfTextField(state: TextFieldState, label: String, modifier: Modifier = Modifier, testTag: String? = null,
    placeholder: String? = null, hint: String? = null, error: String? = null, enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, onKeyboardAction: KeyboardActionHandler? = null,
    contentType: ContentType? = null)
@Composable fun CfSecureTextField(state: TextFieldState, label: String, modifier: Modifier = Modifier, testTag: String? = null,
    revealed: Boolean = false, hint: String? = null, error: String? = null, enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), onKeyboardAction: KeyboardActionHandler? = null,
    contentType: ContentType? = ContentType.Password)
```

- Column, gap `AuthTokens.fieldGap` (8): label (`authLabel`, `cardForeground`, `clearAndSetSemantics {}`), box, then error row or hint.
- Box (`internal fun cfFieldDecorator(focused, error, enabled): TextFieldDecorator`, internal so task 17's guard rule does not count it as a primitive): min height 48 dp (iOS 44 → Android target 48); padding horizontal `AuthTokens.inset` (12), vertical 8; radius `AuthTokens.fieldRadius` (12); **no fill** (iOS draws none; OQ-2-16); border 1 dp `destructive` if error, else `ring` if focused, else `input`; when focused an extra `ring` stroke of `AuthTokens.focusWidth` (2 dp) in the band 0–2 dp outside the box (iOS `inset(by: -focusWidth).strokeBorder`, same drawing as `cfFocusRing`); text `authBody` (16/24) `cardForeground`; placeholder `mutedForeground`; `cursorBrush = SolidColor(ring)`; disabled alpha 0.5.
- Error row: `Row(gap = AuthTokens.brandGap 6, centre)` with 16 dp `circle-alert` + text `authHint` in `destructive`, `clearAndSetSemantics {}` (the field announces it). Hint: `authHint` `mutedForeground`, a normal readable node. [R] iOS hides the hint text and passes `error ?? hint` as the field's `accessibilityHint`; Compose has no hint property, so TalkBack reads the visible hint node right after the field instead (the error keeps iOS behaviour through `error()`).
- Field semantics: `contentDescription = label`, `error(error)` when present, `contentType` when given, `testTag` (iOS dotted id, for example `sign-in.email`).
- Secure: `BasicSecureTextField(textObfuscationMode = if (revealed) Visible else RevealLastTyped)`. **No eye button** — iOS reveals passwords with a separate switch row on sign-up (`SignUpView.swift:60–68`), which 5d builds.
- **Secrets:** KDoc on both: the caller owns the `TextFieldState`; a secret field's state is created with `remember { TextFieldState() }` or held in a ViewModel, **never** `rememberTextFieldState()` (saveable), `rememberSaveable`, `SavedStateHandle`, a `Bundle` or a log (AGENTS Secrets). Konsist enforces it (task 17).
- **Tests:**
  - `CfTextFieldTest` (`@Logic`): the label is the field's content description; an error sets `SemanticsProperties.Error`; typing updates `state.text`; the secure field's displayed text is obfuscated after `RevealLastTyped` times out and plain with `revealed = true`; `contentType` is exposed.
  - `CfTextFieldColorsTest` (`@Layout`): border midpoint pixel = input / ring (focused) / destructive (error), light and dark, ±2; the focus outline pixel 1 dp outside the box = ring.
  - `CfTextFieldGuardsTest` (`@Layout`, 8 cases): label Auth "Email" / "E-mailadres", Auth "Password" / "Wachtwoord" (secure), error Auth "Enter a valid email address, like name@example.com." / "Vul een geldig e-mailadres in, zoals naam@voorbeeld.nl." and "Check both and try again, or reset your password." / "Controleer ze allebei en probeer het opnieuw, of herstel je wachtwoord." — never ellipsized.

### Task 13. Card, divider, row, badge, pill (`feat(designsystem): add card row badge and pill`)

**`CfCard(modifier = Modifier, rows: Boolean = false, shadow: Boolean = true, border: Boolean = true, spacing: Dp = PlanUITokens.space4.dp, content: @Composable ColumnScope.() -> Unit)`** — port of `SettingsCard`: radius `radius2Xl` (22) if `rows` else `radius3Xl` (26); padding 0 if `rows` else `space5` (20); vertical spacing 0 if `rows` else `spacing`; fill `card`, content color `cardForeground`; border 1 dp `border` when `border` (5a's auth card passes `false`, `spacing = AuthTokens.cardGap.dp`); `softShadow` only when `shadow && !rows`; fill max width; `rows` clips content to the shape.

**`CfDivider(modifier)`** — port of `SettingsDivider`: 1 dp `border`, **full width**, hidden from accessibility.

**`CfRow(title: String, modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, badge: String? = null, badgeDawn: Boolean = false, chevron: Boolean = true, lock: Boolean = false, destructive: Boolean = false, enabled: Boolean = true, onClick: (() -> Unit)? = null, testTag: String? = null)`** — port of `SettingsRow` with the SetStates disabled state:
- Row, gap `space3` (12), padding horizontal `space4` (16) / vertical `SettingsTokens.rowVertical` (10), min height `SettingsTokens.rowHeight` (64), vertically centred.
- Leading disc: 36 dp (`rowDisc`) `muted` circle with a 16 dp icon in `cardForeground` (SetStates).
- Texts: title `settingsRow` (16/24 500) in `cardForeground`, or `destructive`; subtitle `screenBody` (14/20) `mutedForeground`; both wrap.
- `badge` → `CfBadge(badge, if (badgeDawn) Dawn else Secondary)`.
- Trailing, in this order (iOS `SettingsRow` flags `lock`, `chevron`; both can show at once, as on `Set2FAManage` and iOS `SettingsSecurityView.swift:285`): `lock` → 16 dp lucide `lock` `mutedForeground`; `chevron` → 20 dp lucide `chevron-right` `mutedForeground` (SetHome, OQ-2-19). Both are `Icon(contentDescription = null)` (nothing announced) with `Modifier.testTag("cf.row.lock")` / `testTag("cf.row.chevron")` for tests.
- Disabled: whole-row alpha 0.5 (SetStates, OQ-2-8), not clickable, `disabled()`. The board's "Needs a connection." hint sits **below the card**, so it is the caller's text, not a row parameter.
- Semantics `mergeDescendants`; `Role.Button` + `cfClickable` when `onClick != null`.

**`CfBadge(text: String, modifier = Modifier, variant: CfBadgeVariant = CfBadgeVariant.Secondary)`** — bundle `Badge` + iOS `SettingsBadge`: pill; padding horizontal 8 (`space2`) / vertical 2 (`2 × borderWidth`); `settingsBadge` (12/16 500); 1 dp border (transparent for filled variants, `border` for `Outline`); `Default` primary/primaryForeground, `Secondary` secondary/secondaryForeground, `Outline` transparent/foreground, `Dawn` dawn/dawnForeground. One line; may ellipsize and carries `ellipsizable()`.

**`CfPill(text: String, selected: Boolean, onClick: () -> Unit, modifier = Modifier, testTag: String? = null)`** and **`CfPillGroup(options: List<CfPillOption>, selectedId: String, onSelect: (String) -> Unit, modifier = Modifier, testTag: String)`** — port of `SettingsRadioGroup` (non-card) + SetStates pills:
- pill shape; fill `card`; border `SettingsTokens.selectionWidth` (2 dp) `dawn` when selected, else **`border`**; min height `pillHeight` (44) + 48 dp target; padding horizontal 16 / vertical 8; content row gap `pillGap` (6).
- Selected: 16 dp lucide `check` in `dawnText` before the label, label `screenLabel` (600); unselected: label `settingsPill` (500), no check. Label wraps. [R] The check comes from iOS `SettingsRadioGroup` (SF `checkmark`); the SetStates pills draw none, and no board gives its size, so it uses `AuthTokens.icon` (16) (OQ-2-26).
- Group: `FlowRow` with horizontal and vertical spacing `space2` (8) and `Modifier.selectableGroup()`; option test tags `"$testTag.${option.id}"` (iOS identifier + "." + id).
- Semantics `Role.RadioButton`, `selected`. **No haptic** (iOS Settings pills have none; OQ-2-20).

**Tests**
- `CfRowTest` (`@Logic`): disabled → no click and `disabled()`; merged node has `Role.Button`; `lock = true` with the default `chevron` shows both glyphs (two icon nodes found with `useUnmergedTree = true` by test tag `cf.row.lock` / `cf.row.chevron`).
- `CfPillGroupTest` (`@Logic`): clicking an option calls `onSelect(id)`; the selected node has `selected = true`; tags are `<group>.<id>`.
- `CfCardRowColorsTest` (`@Layout`): card corner radius (pixel just inside the corner is background), pill border pixel dawn/border, badge fills, row disabled alpha (pixel), destructive row title colour (pixel), light and dark.
- `CfCardTest` (`@Layout`, pixels): `border = false` draws no border (pixel at the edge midpoint equals the card fill ±2 against a contrasting background); `rows = true` has no shadow and clips.
- Guards (`@Layout`, 8 cases each): `CfRowGuardsTest` (rows inside `CfCard(rows = true)` separated by `CfDivider`) with Settings "Two-factor authentication" / "Tweestapsverificatie" + subtitle Settings "When it's on, you sign in with your password and a code from an authenticator app." / "Als dit aanstaat, log je in met je wachtwoord en een code uit een authenticator-app." + badge Settings "Off" / "Uit" + `lock = true`, and the disabled "Add a passkey" / "Passkey toevoegen" row; `CfPillGuardsTest` (`CfPillGroup` and a standalone `CfPill`, so both names appear for task 17) with Settings "Light" / "Licht", "Dark" / "Donker", "System" / "Systeem"; `CfBadgeGuardsTest` with Screens "Now" / "Nu" (Dawn), "Skipped" / "Overgeslagen" (Outline), Screens "Done" / "Klaar" (Secondary).

### Task 14. Banner, notice and skeleton (`feat(designsystem): add banner notice and skeleton`)

**`CfBanner(title: String, message: String?, modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null, announce: Boolean = true, testTag: String? = null, actionTestTag: String? = null)`** — port of `AuthBanner` (AuthStates):
- Row, top-aligned, gap `bannerGap` (10); padding vertical 12 / horizontal 14; radius `bannerRadius` (18); fill **`authErrorBackground`**; border 1 dp **`authErrorBorder`** (the generated mixes, not a runtime blend).
- Glyph: 18 dp lucide `circle-alert` in `destructive`, top padding 1 (`bannerIconTop`).
- Column: title `authStrong` (600 14/20) + message `authHint` (omitted when null or empty: iOS offline banner = title only), both `cardForeground`; optional action = text `authStrong`, underlined, `foreground`, start-aligned, min height 44 + 48 dp target, `cfClickable`.
- Semantics: `liveRegion = LiveRegionMode.Polite` on the container when `announce` (AuthStates: "announced to screen readers"; 8 relies on it); `announce = false` sets none (5a has its own announcer; iOS `AuthBanner` has no live region); `mergeDescendants` when there is no action. iOS tags `auth.banner` / `auth.banner.action` are passed by 5a.
- The plan's "info" banner is `CfNotice(Info)`; "offline" has no in-scope board (OQ-2-14).

**`CfNotice(title: String, message: String, modifier = Modifier, tone: CfNoticeTone = CfNoticeTone.Info)`** — port of `AuthNotice` + bundle `Notice` + SetStates:
- Row, top-aligned, gap `inset` (12); padding 12; radius `noticeRadius` (22).
- Info: fill `card`, border `border`, 16 dp `info` in `mutedForeground`. Warning: fill `settingsWarningBackground`, border `settingsWarningBorder`, 16 dp **`circle-alert`** in `dawn` (iOS uses `AuthGlyph.alert` = circle-alert; the bundle's triangle loses to iOS). Glyph top padding 2 (`noticeIconTop`).
- Text: one `Text` with `AnnotatedString(title + " " + message)`, whole string `authHint`, the title span `authStrong`; color `cardForeground`. `mergeDescendants`.

**`CfSkeleton(modifier: Modifier, shape: Shape = CfTheme.shapes.md)`** — a static `muted` fill (iOS has no pulse anywhere; OQ-2-6); `clearAndSetSemantics {}`. The parent sets the loading description.

**Tests**
- `CfBannerTest` (`@Logic`): live region `Polite` by default and absent with `announce = false`; `message = null` and `""` render only the title; action click fires; no action → one merged node; `testTag`/`actionTestTag` land on the container/action.
- `CfBannerNoticeColorsTest` (`@Layout`): banner fill pixel = `authErrorBackground` ±2 and border pixel ≈ `authErrorBorder` over card; notice fills/borders per tone; light and dark.
- `CfSkeletonTest` (`@Layout`, pixels): two captures 1 s apart (`mainClock.advanceTimeBy(1000)`) are identical; no semantics node.
- Guards (`@Layout`, 8 cases each): `CfBannerGuardsTest`, `CfNoticeGuardsTest`, `CfSkeletonGuardsTest` (the SheetStates loading bars as the board draws them: 60 % × 24 radius 10, 40 % × 14 full, 20 × 20 radius 6 + 90 × 14 full in a 56 dp row, 40 × 40 radius 12, 80 × 20 full; plus one iOS `SettingsLoadingRows` row: 36 dp disc and a 16 dp-high capsule capped at `SettingsTokens.skeletonRowWidths` 150 / 190 / 130; ink only). Copy: banner Auth "That email and password don't match." / "Dit e-mailadres en dit wachtwoord horen niet bij elkaar." + "Check both and try again, or reset your password." with action Auth "Try again" / "Opnieuw proberen"; Auth "Something went wrong on our side." / "Er ging bij ons iets mis." + "Try again in a moment." / "Probeer het zo meteen opnieuw."; notice Reminders "This device can't show reminders." / "Dit apparaat kan geen herinneringen tonen." + "You can still follow every step in your plan." / "Je kunt nog steeds elke stap in je plan volgen.", both tones.

Tick **"Designed primitives"** in this commit.

### Task 15. System wrappers A (`feat(designsystem): tint app bars switch segmented buttons progress and refresh`)

Package `…designsystem.system`. Each wrapper passes colors and defaults only; it never redraws the control. `MaterialTheme.colorScheme` may be read here and in `theme/` only.

- **`CfTopAppBar(title: String, modifier, navigationIcon: (@Composable () -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, scrollBehavior: TopAppBarScrollBehavior? = null)`** (small, pinned) and **`CfLargeTopAppBar(title: String, scrollBehavior: TopAppBarScrollBehavior, modifier, navigationIcon, actions)`** (callers pass `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`):
  - colors: container `background`, scrolledContainer `popover`, title/navigation/action content `foreground` (08 row 3);
  - title: the M3 default style for that bar with `fontWeight = FontWeight.Bold`, system font (iOS nav-title decision); `maxLines = 1`, `TextOverflow.Ellipsis`, `ellipsizable()` (OQ-2-22);
  - `windowInsets = LocalSafeInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)` (never `WindowInsets.statusBars`: 0e Konsist rule);
  - `CfBackButton(contentDescription: String, onClick)` = `IconButton` with `MaterialSymbols.ArrowBack`, `foreground`; callers pass Auth "Back" / "Terug".
- **`CfSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier, enabled = true)`**: `SwitchDefaults.colors(checkedTrackColor = dawn, checkedBorderColor = dawn, checkedThumbColor = dawnForeground, checkedIconColor = dawn, uncheckedTrackColor = input, uncheckedBorderColor = mutedForeground, uncheckedThumbColor = mutedForeground)` (plan: unchecked track `input`; OQ-2-18). When `onCheckedChange != null` it performs `ToggleOn`/`ToggleOff`. `fun Modifier.cfToggleable(value: Boolean, onValueChange: (Boolean) -> Unit, enabled: Boolean = true)` = `toggleable(role = Role.Switch)` + the same haptics, for whole-row toggles where the switch gets `onCheckedChange = null` (08 row 9).
- **`CfSegmentedButtons(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier, testTag: String? = null)`**: `SingleChoiceSegmentedButtonRow` + `SegmentedButton(shape = SegmentedButtonDefaults.itemShape(i, n))`, `SegmentedButtonDefaults.colors(activeContainerColor = systemSelected, activeContentColor = dawnText, activeBorderColor = mutedForeground, inactiveContainerColor = Color.Transparent, inactiveContentColor = foreground, inactiveBorderColor = mutedForeground)`; keeps M3's check icon; `Selection` haptic on change; label M3 default typography.
- **`CfLinearProgress(progress: () -> Float, modifier)`** and an indeterminate overload: `LinearProgressIndicator(progress, color = dawn, trackColor = muted, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})`, height 4 dp, full width (08 row 15).
- **`CfPullToRefreshBox(isRefreshing: Boolean, onRefresh: () -> Unit, modifier, state = rememberPullToRefreshState(), content)`**: `PullToRefreshBox(…, indicator = { PullToRefreshDefaults.Indicator(state, isRefreshing, Modifier.align(TopCenter), containerColor = popover, color = dawn) })` (plan; OQ-2-7).
- **Tests:**
  - `SystemWrappersTest` (`@Logic`, `core/ui/src/test/…/designsystem/`, because it uses `RecordingHaptics`): switch toggles fire `ToggleOn`/`ToggleOff` in `RecordingHaptics`; `cfToggleable` exposes `Role.Switch` and toggles; segmented `onSelect` + `Selection` haptic; large app bar collapses (title node height shrinks) after `swipeUp` on a tagged `LazyColumn` with `nestedScroll(scrollBehavior.nestedScrollConnection)`.
  - `SystemTintTest` (`@Layout`, light and dark, ±2 per channel): switch track pixel checked (dawn) and unchecked (input); thumb checked (dawnForeground); active segment fill (systemSelected); progress fill (dawn) and track (muted); app bar container before (background) and after scroll (popover); pull indicator container (popover) with `isRefreshing = true`.
  - Guards (`@Layout`, 8 cases each): `SystemBarsGuardsTest` (`CfLargeTopAppBar` and `CfTopAppBar` with `CfBackButton`, `CfSwitch` in a `cfToggleable` row, `CfLinearProgress`, `CfPullToRefreshBox` idle) and `CfSegmentedButtonsGuardsTest`. Copy: `CfLargeTopAppBar` with Settings "Change password" / "Wachtwoord wijzigen" and a back button; `CfSegmentedButtons` with Settings "Light", "Dark", "System" in both languages.

### Task 16. System wrappers B (`feat(designsystem): tint sheet dialog snackbar and pickers`)

- **`CfModalBottomSheet(onDismissRequest: () -> Unit, modifier, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), content: @Composable ColumnScope.() -> Unit)`**: `ModalBottomSheet(sheetMaxWidth = 640.dp, shape = CfTheme.shapes.sheetTop (38), containerColor = card, contentColor = cardForeground, dragHandle = { BottomSheetDefaults.DragHandle() }, scrimColor = BottomSheetDefaults.ScrimColor)`. Container `card` (iOS `presentationBackground(card)`, ItemDetail; OQ-2-2); radius 38 (iOS `SettingsTokens.sheetRadius`, ItemDetail; OQ-2-4); drag handle = M3 default, whose color is `onSurfaceVariant` = `mutedForeground` at M3's alpha (system grabber; OQ-2-3). Content padding belongs to the caller.
- **`CfAlertDialog(title: String, text: String?, confirm: CfDialogAction, onDismissRequest: () -> Unit, dismiss: CfDialogAction? = null)`** with `data class CfDialogAction(val label: String, val onClick: () -> Unit, val destructive: Boolean = false)`: `AlertDialog(containerColor = popover, titleContentColor = foreground, textContentColor = mutedForeground)`; buttons `TextButton(colors = textButtonColors(contentColor = if (destructive) destructive else dawnText))`; confirm on the right (M3 order); title/text M3 default typography (08 row 10).
- **`CfSnackbarHost(hostState: SnackbarHostState, modifier, onPresented: (SnackbarData) -> Unit = {})`**: `SnackbarHost(hostState) { data -> LaunchedEffect(data) { onPresented(data) }; Snackbar(data, containerColor = foreground, contentColor = background, actionColor = background, actionContentColor = background, dismissActionContentColor = background) }`. Action color `background`, not `inversePrimary` (dawn on foreground is 1.61:1 in dark; OQ-2-5). Bottom placement is the caller's `Scaffold` (OQ-A5). `onPresented` feeds the Debug "was presented" counter of later phases (AGENTS Tests 8).
- **`CfDatePickerDialog(state: DatePickerState, onDismiss, onConfirm, confirmLabel: String, dismissLabel: String, title: String? = null)`** and **`CfTimePickerDialog(state: TimePickerState, onDismiss, onConfirm, confirmLabel, dismissLabel, title: String? = null)`**:
  - `DatePickerDefaults.colors(containerColor = popover, selectedDayContainerColor = dawn, selectedDayContentColor = dawnForeground, todayDateBorderColor = dawn, todayContentColor = dawnText, selectedYearContainerColor = dawn, selectedYearContentColor = dawnForeground, headlineContentColor = foreground)`;
  - `TimePickerDefaults.colors(clockDialColor = muted, selectorColor = dawn, clockDialSelectedContentColor = dawnForeground, timeSelectorSelectedContainerColor = systemSelected, timeSelectorSelectedContentColor = dawnText, periodSelectorSelectedContainerColor = systemSelected, periodSelectorSelectedContentColor = dawnText)` (08 row 12);
  - buttons are `TextButton`s in `dawnText`; typography M3 default. If material3 1.4.0 has no public `TimePickerDialog`, build the dialog from `BasicAlertDialog` with a `popover` `Surface` (M3 dialog shape `MaterialTheme.shapes.extraLarge`), `TimePicker(state, colors)` and the two buttons; record which.
- **Tests:**
  - `SystemDialogsTest` (`@Logic`, `core/ui/src/test/…/designsystem/`): alert confirm/dismiss callbacks; destructive confirm text color is `destructive` (no pixels: run `SemanticsActions.GetTextLayoutResult` on the label node and read `layoutInput.style.color`, which carries the `TextButton`'s content color); snackbar `onPresented` fires exactly once per `showSnackbar`; the sheet content node's width on `foldInner` ≤ 640 dp.
  - `SystemDialogTintTest` (`@Layout`, light/dark): sheet container (card), alert container (popover), snackbar container/text/action colors. If Robolectric cannot capture the dialog window (sheet, alert, pickers), delete those pixel checks and cover them in the device gallery (task 18); record it (AGENTS Tests 6).
  - Guards (`@Layout`, 8 cases each): `SystemDialogsGuardsTest` (alert, snackbar; the sheet, date and time pickers only if Robolectric renders their windows, else gallery-only and recorded). Copy: alert with Settings "Cancel" / "Annuleren" + "Done" / "Klaar" and a long nl body (Settings "We couldn't save your change. Please try again." / "Je wijziging kon niet worden opgeslagen. Probeer het opnieuw."); snackbar with Settings "Saved" / "Opgeslagen" and the same error text with action Auth "Try again".

Tick **"System-component wrappers"** in this commit.

### Task 17. Konsist rules (`test(konsist): guard design-system boundaries`)

Add to `KonsistRules.kt` / `KonsistRulesTest.kt` with the 0a pattern: `<rule>()` passes on the project scope, `<rule>RejectsPlantedViolation()` throws on `konsist/src/test/resources/violations/<rule>/…`, `<rule>AllowsControl()` passes on `…/allowed/<rule>/…`. Scope = `src/main` of every module, excluding `**/generated/**` and `*.generated.kt`.

| Rule | Forbids | Where | Planted violation | Allowed control |
|---|---|---|---|---|
| `noMaterialColorSchemeOutsideDesignSystem` | `MaterialTheme.colorScheme` | everywhere except `core/designsystem/…/designsystem/{theme,system}/` | `feature/today/src/main/kotlin/…/ColorSchemeUse.kt` | the same text in `core/designsystem/…/system/X.kt` |
| `noRawColorsOutsideDesignSystem` | `Color(0x…)`, `Color(<number>, …)`, `Color(red = …)`, any `Color.<Name>` except `Color.Transparent`/`Color.Unspecified` (OQ-2-9) | outside `core/designsystem` | `feature/settings/…/RawColor.kt`, `core/ui/…/NamedColor.kt` | `Color.Transparent` in a feature file |
| `featureDpLiteralsComeFromBoardMetrics` | a `<number>.dp` / `.sp` literal other than `0.dp` outside an `object *Metrics` annotated `@BoardMetrics(...)` | `feature/*/src/main`, `core/ui/src/main` | `feature/trip/…/LooseDp.kt`; `…/MetricsWithoutBoard.kt` (object without the annotation) | an annotated `object TripHeaderMetrics` |
| `boardMetricsNamesExist` | `@BoardMetrics("X")` where `_design/canvas/X.dc.html` does not exist | everywhere | `…/MetricsUnknownBoard.kt` (`@BoardMetrics("NoSuchBoard")`) | `@BoardMetrics("SetStates")` |
| `noMaterialIconsLibrary` | imports starting with `androidx.compose.material.icons` | everywhere (main and test) | `feature/today/…/MaterialIconsImport.kt` | — |
| `noHapticsInLaunch` | imports of `androidx.compose.ui.hapticfeedback.*`, `net.chronoflow.core.designsystem.haptics.*`, `android.view.HapticFeedbackConstants`; text `performHapticFeedback(`, `LocalHaptics` | `feature/launch/src/main` | `feature/launch/…/LaunchHaptics.kt` | the same import in `feature/today` |
| `secureFieldStateIsNeverSaved` | in a file that contains `CfSecureTextField(`: `rememberTextFieldState(`, `rememberSaveable`, `TextFieldState.Saver` | everywhere | `feature/auth/…/SavedSecret.kt` | a file with `CfSecureTextField(` and `remember { TextFieldState() }` |
| `designSystemPrimitivesHaveGuards` | a public top-level `@Composable fun Cf*` in `core/designsystem/…/{component,system}/` whose name does not appear in any `*GuardsTest.kt` under `core/ui/src/test/kotlin/net/chronoflow/core/ui/designsystem/`, unless it is in the rule's `DEVICE_ONLY` set (start empty; add `CfModalBottomSheet`, `CfDatePickerDialog`, `CfTimePickerDialog` only if task 16 proves Robolectric cannot render their windows, and then the device gallery covers them) | `:core:designsystem` | a planted `component/CfOrphan.kt` in the violation tree with an empty test tree | — |

- `@BoardMetrics(vararg boards: String)`: `@Retention(SOURCE)`, `@Target(CLASS)`, in `…designsystem.layout`. Use it in this phase's own `private object …Metrics` for board-only numbers (ItemDetail 38/36×5 if not from a token, Main 52, SheetStates 48).
- If `featureDpLiteralsComeFromBoardMetrics` fails on code from a phase merged before this one, fix that code by moving its numbers into an annotated `*Metrics` object in this commit and list the files in the PR.

Tick **"Konsist"** and **"Haptics"** in this commit.

### Task 18. Device captures and the primitive gallery (`test(ui): add device captures and the primitive gallery`)

0e left `make captures DEVICE=1` to this phase (OQ-0e-18) and asked that `BoardCapture.capture(rule, name)` keep its signature.

- **`BoardCapture` device path** (`core/testing-compose/src/main/kotlin/net/chronoflow/core/testing/compose/BoardCapture.kt`): decide the path by `Build.FINGERPRINT == "robolectric"` (Robolectric's fixed value), **not** by whether `InstrumentationRegistry` exists (Robolectric provides one too) or by the `cf.capturesDir` property alone. On Robolectric keep 0e's behaviour exactly (including the `IllegalStateException` when `cf.capturesDir` is absent). On a device, read `cf.captureOnly` from `InstrumentationRegistry.getArguments()` (same prefix rule, `assumeTrue` skip) and write the PNG under the instrumentation's `additionalTestOutputDir` argument (AGP fills it and pulls the files to `<module>/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device>/`) [verify on the first run]. Fallback if that directory is null: `targetContext.getExternalFilesDir("captures")`, pulled with `adb pull`. Record which one works in `docs/tooling.md`. The Robolectric path is unchanged. `:core:ui` adds `androidTestImplementation(project(":core:testing-compose"))`. `:core:testing-compose` exposes Robolectric and Roborazzi as `api` (0e T7); if the androidTest APK then fails to build or dex, exclude them on that line (`{ exclude(group = "org.robolectric"); exclude(group = "io.github.takahirom.roborazzi") }`) and record it in `docs/tooling.md`; device code must not touch `ProfileRule` (it calls `RuntimeEnvironment`).
- **`Makefile`**, replacing only 0e's `DEVICE=1` stub lines: `./gradlew :core:ui:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=net.chronoflow.core.testing.tiers.Capture` plus `-Pandroid.testInstrumentationRunnerArguments.cf.captureOnly=$(ONLY)` **only when `ONLY` is non-empty**; then copy every PNG from the additional-output directory (or the `adb pull` fallback) into `build/captures/device/`, keeping subdirectories, and print `captures: <n> PNGs in build/captures/device`. Same exit rules as 0e's Robolectric path (0 PNGs with empty `ONLY` → exit 1; with non-empty `ONLY` → exit 0). Without a booted device: exit 1 and print `Scripts/emulator.sh ensure` advice. Later phases add their modules to the Gradle line. Run the Gradle call and the PNG copy (or `adb pull`) as one command under the device lock, `python3 -I Scripts/device_lock.py -- sh -c '…'` (0a OQ-0a-21), so another worktree's device tier cannot interleave.
- **Gallery** (test helper, not a screen): `core/testing-compose/src/main/kotlin/net/chronoflow/core/testing/compose/designsystem/DesignSystemGallery.kt` — `:core:testing-compose` main is test-helper code that sees `:core:ui` (strings via `net.chronoflow.core.ui.R`) and is consumed by both `core/ui/src/test` and `core/ui/src/androidTest`, so there is one copy. One `@Composable` per section — `Buttons`, `Fields`, `CardsRows`, `PillsBadges`, `BannersNotices`, `Skeleton`, `Bars`, `Controls` (switch, segmented, progress, pull indicator refreshing), `Sheet` (open), `Alert`, `Snackbar`, `DatePicker`, `TimePicker` — each inside `GalleryHost(language: AppLanguage, dark: Boolean)`, which wraps `ChronoFlowTheme(darkTheme = dark)` in a `LocalContext`/`LocalConfiguration` override built with `createConfigurationContext` for the locale. Copy is the strings named in tasks 11–16.
- `DesignSystemGalleryCaptureTest` (`@Capture`, emulator, `core/ui/src/androidTest/kotlin/net/chronoflow/core/ui/designsystem/`): one test method per section, each capturing the 4 combinations (light/dark × en/nl) with `BoardCapture.capture(rule, "designsystem/<Section>_pixel_<lang>_<theme>")` (0e naming: non-board captures go under a subdirectory). Each method < 10 s.
- `DesignSystemGalleryRobolectricCaptureTest` (`@Capture`, `core/ui/src/test/…/designsystem/`): the same sections at `pixel8` through `ProfileRule`, names `designsystem/<Section>_pixel8_<lang>_<theme>` (`make captures ONLY=designsystem`). If task 16 recorded that Robolectric cannot render dialog windows, the `Sheet`, `Alert`, `DatePicker` and `TimePicker` methods skip with `assumeTrue("<reason>, device gallery only", false)` (only the windows Robolectric cannot render), and acceptance 5 counts the remaining sections.

### Task 19. Docs and close-out (`docs(designsystem): record the design system mapping`)

- `docs/design-system.md`: the primitive list with each one's board and iOS source; the `toM3()` table; the `KNOWN_GAPS` table; Reduce Motion behaviour (press alpha-only, spinner keeps spinning, skeleton static); the shadow section from task 9; which dialog tints are verified on Robolectric vs device.
- `docs/third-party.md`: lucide and Material Symbols entries (task 10).
- `plan.md`: tick **"Guards"**; append this brief's plan-changing OQs (OQ-2-2, 2-3, 2-5, 2-6, 2-14, 2-15) to "Open questions for the user" with the next free `OQ-A` numbers (OQ-A48 onward: the 2026-11-16 review took OQ-A44–A47; check the table, since a phase merged earlier may have taken more) and their defaults, one row each on its own line.
- Stored Roborazzi references stay opt-in; record only `CfButton_styles_pixel8_en_{light,dark}`, `CfCard_shadow_pixel8_en_{light,dark}`, `CfRow_states_pixel8_en_light`, `CfBanner_default_pixel8_en_light`, `CfTextField_states_pixel8_en_light` (`@Snapshot`, `SNAPSHOT_RECORD=1 make snapshots`).

---

## 7. Acceptance criteria

1. `make quick` passes (< 60 s warm) and `make check` passes with `pixel` booted (orchestrator runs the emulator tiers if your sandbox cannot; report the command and its exact output).
2. `make generators-check` shows no diff after `make generate` (tokens unchanged, vectors regenerated byte-identically); `make vectors-check` passes, including `python3 -I -m unittest discover -s tools/icons -t tools/icons -p 'test_*.py'`; `Scripts/test_scripts.sh` runs the `tools/icons` tests.
3. `make targeted ONLY="core:designsystem:ToM3Test core:designsystem:ContrastTest core:designsystem:CfPressValuesTest core:designsystem:CfPressTest core:designsystem:CfPressRecompositionTest core:designsystem:SkyRampSamplesTest core:designsystem:SoftShadowFidelityTest konsist:KonsistRulesTest konsist:ModuleGraphTest"` is green (with `core:ui:SoftShadowFidelityDeviceTest` instead of the Robolectric shadow test if task 9 fell back to the device); every planted violation is rejected and every control allowed.
4. Every primitive and wrapper has a guards class with 8 passing cases (`designSystemPrimitivesHaveGuards` green); timings within budget in `build/test-results/*/timings.json`.
5. `make captures ONLY=designsystem` writes 13 sections × 4 PNGs to `build/captures/designsystem/` (fewer only for the dialog sections task 16/18 recorded as device-only), and `make captures DEVICE=1 ONLY=designsystem` (with `pixel` booted) writes the same set to `build/captures/device/designsystem/`; attach both to the PR. `make captures DEVICE=1 ONLY=Nope` exits 0 with 0 PNGs. The Opus reviewer compares the designed primitives with `AuthStates`, `SetStates`, `SheetStates`, `ItemDetail` and `Main`.
6. **Performance:** a press causes **0 recompositions** (`CfPressRecompositionTest`); font preload never runs on the main thread nor before the first frame (`FontWarmupTest`, `RootThemeTest`); `make perf` cold start has not regressed by more than 5 % against the 0e number in `docs/perf-startup.md` (record the new number with device and build type). No `dropShadow` is used on anything in `:feature:trip` timeline code.
7. `docs/design-system.md` holds the shadow measurement (candidates, chosen mapping, residual, crops), the toM3 table and the contrast gaps; `docs/third-party.md` lists both icon sources.
8. `git -C <repo> --no-optional-locks status --porcelain` (the flag keeps `git status` from refreshing the read-only repo's index) for `chronoflow-ios`, `chronoflow-new` and `chronoflow-mobile` is identical before and after the phase (save it to `build/readonly-status-before.txt` at the start). Baseline on 2026-11-16, not caused by this phase: `chronoflow-ios` has 6 untracked `Packages_ChronoFlowKit_Sources_*_Localizable.xcstrings` files at its root; `chronoflow-new` has untracked `docs/ui/claude-design/` and `research.md`; `chronoflow-mobile` is clean. Never delete them (read-only repos); report them to the orchestrator.
9. All 10 Phase 2 checkboxes in `plan.md` are ticked; commits are Conventional Commits with no trailer; StrictMode is clean in a debug run.

---

## 8. Review checklist (Opus reviewer)

- [ ] No hand edit under `generated/`. Every number traces to a generated object, or to a `@BoardMetrics` object that names an in-scope board.
- [ ] `CfType` maps the generated `CfFace` (no second opsz rule in Kotlin); `includeFontPadding = false`, `LineHeightStyle(Center, Trim.None)`, letter spacing in **em**; Fraunces families are three objects, created once.
- [ ] `toM3()` matches the table role by role; `ToM3Test` fails when an unmapped `ColorScheme` role exists. `primary == dawnText`, never token `primary`.
- [ ] `KNOWN_GAPS` is honest; no token changed to make a pair pass.
- [ ] `cfPress` is the `CfPressIndication` node: it reads the `Animatable` only inside `draw()`, reuses one `GraphicsLayer`, and `cfClickable` needs no caller-supplied interaction source; no `composed {}`, no `@Composable` modifier factory; Reduce Motion gives alpha only with no animation; values equal iOS exactly; no ripple on designed pressables; disabled shows no press.
- [ ] The shadow mapping was measured (candidate table present), not guessed; any fallback is recorded with its reason. No shadow on `Main`-like sky content.
- [ ] Banner uses the generated `authErrorBackground`/`authErrorBorder`, notice warning uses `settingsWarning*` and the **circle-alert** glyph; pill unselected border is **`border`** (not `input`) and the group gap is 8; divider is full width; row disabled hint is not a row parameter; `CfRow` can show `lock` and `chevron` together.
- [ ] Focus rings (pressables and text field) sit in the 0–2 dp band outside the bounds; `cfFocusRing` is a node before the `clickable`.
- [ ] `:core:designsystem` tests use only `:core:testing`; everything that needs `ProfileRule`/`Guards`/`RecordingHaptics`/strings is in `:core:ui`; no `@Logic` class asserts pixels.
- [ ] Text field: no fill, label is the content description, error announced via `error()`, **no eye button**, secure state never saveable (KDoc + Konsist).
- [ ] Wrappers only tint: no redrawn switch/sheet/dialog, no glass, no blur; app bar insets come from `LocalSafeInsets`; sheet = card, radius 38, `sheetMaxWidth = 640.dp`; snackbar action = background; unchecked switch track = input.
- [ ] Every interactive primitive has a ≥ 48 dp target (CfButton `Sm`, pills, rows, banner action); text never ellipsizes except badge and app-bar title, which are marked `ellipsizable()`.
- [ ] Icons come from the boards' 0.511.0 shapes (moon is the old path); unmatched entries fail; no `androidx.compose.material.icons` anywhere; Material Symbols vendored with version, license and SHA manifest.
- [ ] Haptics: only switch/toggle and segmented buttons fire; none in `:feature:launch` (rule present).
- [ ] Tier annotations: Robolectric classes `@Logic`/`@Layout`/`@Capture`/`@Snapshot` only (never `@Smoke`); emulator classes `@Smoke`/`@Capture`; imported from `net.chronoflow.core.testing.tiers` (no `…testing.tier`, no `Tiers.kt`, no alias); `ProfileRule` gets `AppLanguage`, never the wire `Language`; no `Thread.sleep`, no `printToLog`, no node-enumeration outside `Guards`.
- [ ] `RootOverrides` is the only Reduce Motion seed reader in `src/main`, guarded by `BuildConfig.DEBUG`; `src/main` imports nothing from `net.chronoflow.debug`; `RootOverridesParityTest` and `RootOverridesReleaseTest` exist and pass.
- [ ] Makefile: Phase 2 adds only `vectors`/`vectors-check`, their two `+=` lines and the `captures` `DEVICE=1` branch; 0b's `icons` target is untouched. Captures use `BoardCapture.capture(rule, name)` with `designsystem/…` names.
- [ ] The downstream parameters exist with the stated defaults: `CfButton.contentPadding`, `CfCard.border`, `CfBanner.announce` and nullable `message`, `CfSky.brush(dark)`.
- [ ] Task 0 left no alias; `:core:designsystem` depends on no project module.
- [ ] Nothing out of scope was built (designed toast, Welcome screen, tab bar, ChoiceCard, offline banner, eyebrow components).
- [ ] Read-only repos untouched; commits without trailers; shared files edited on their own lines.

Typical mistakes: `MaterialTheme.colorScheme.primary` used for a designed indigo button; `Modifier.shadow(elevation)` instead of `dropShadow`; `rememberTextFieldState()` for a password; `onSizeChanged` + state for sizes; `Color(0x…)` in a test helper under `src/main`; ripple left on `cfClickable`; `WindowInsets.statusBars` in a wrapper; `assertAll` used without a heading tag; pixel tests in `@Logic` (slows `make quick`; `CfPressTest`, `CfFocusRingTest`, `CfCardTest`, `CfSkeletonTest` are `@Layout`); `testImplementation(project(":core:testing-compose"))` in `:core:designsystem` (dependency cycle); a `BoardCapture` that picks the device path under Robolectric; a Makefile target named `icons` (clobbers 0b's); a new `captureOnDevice` API next to `BoardCapture`; `softShadow` implemented with `composed {}` to read the theme.

---

## 9. Open questions (proceed on the default)

| # | Question | Default |
|---|---|---|
| OQ-2-1 | Designsystem primitives need `ellipsizable()` and `LocalSafeInsets`, which 0e put in `:core:ui`; guard tests need 0c strings, but the plan lists only 0b and 0e as dependencies. | Task 0 moves both into `:core:designsystem`; `:core:ui` depends on it. Guards live in `:core:ui` tests. If 0c is not in the stack, put temporary test-only strings with the same names in `core/ui/src/test/res/values{,-nl}/` and delete them when 0c lands. |
| OQ-2-2 | Sheet container: plan element table says `popover`; iOS (`presentationBackground(card)`), ItemDetail and SheetStates use `card`. | `card` (iOS wins). Add a plan OQ row. |
| OQ-2-3 | Drag handle: plan says `border`; the boards draw the iOS system grabber (`rgba(120,120,128,0.4)`); iOS uses the system grabber. | M3 default `DragHandle()` (the Android system grabber, `mutedForeground` at M3 alpha). Add a plan OQ row. |
| OQ-2-4 | Sheet corner radius: M3 default 28 vs iOS `sheetRadius` 38 / ItemDetail 38. | 38. |
| OQ-2-5 | Snackbar action as `inversePrimary` (dawn) is 1.61:1 on `foreground` in dark. | Action color `background`; `toM3` keeps `inversePrimary = dawn`. Add a plan OQ row. |
| OQ-2-6 | Skeleton: board pulses (2 s), plan says "static with Reduce Motion" (implies a pulse), iOS is static everywhere. | Static always (iOS wins). Add a plan OQ row. |
| OQ-2-7 | `dawn` fills fail 3:1 in light: on muted 2.17 (progress), popover 2.55 (pull-to-refresh), background 2.40. | Keep `dawn` as the plan says; list in `KNOWN_GAPS`; designer question, no token change. |
| OQ-2-8 | Disabled opacity: boards and `AuthTokens` 0.5 vs `SettingsTokens.disabledOpacity` 0.45 (iOS password prompt only). | 0.5 for every primitive. |
| OQ-2-9 | Are `Color.Transparent` / `Color.Unspecified` allowed in features? | Allowed; every other named `Color.*` is banned. |
| OQ-2-10 | Plan says "softShadow1/softShadow2"; there is one token with light and dark values. | `softShadow` with the per-theme `Shadow`. |
| OQ-2-11 | Plan's shadow comparison names `Main`, which has no shadow. | Compare with the iOS oracle and the `AuthStates`/`SetStates` cards. |
| OQ-2-12 | Material Symbols need one npm download. | One download, vendored with SHA manifest; without network, hand the commands to the orchestrator and continue with lucide. |
| OQ-2-13 | `CfButton`: plan lists 4 styles; boards and iOS use 7. | 7 (`Primary, Secondary, Outline, Ghost, Destructive, Dawn, OnSky`); "quiet" = `Ghost`. No `Link` style (no iOS use). |
| OQ-2-14 | Plan's `CfBanner` "info" and "offline". | "info" = `CfNotice(Info)`; "offline" has no in-scope board, so the feature phase that owns an offline board builds it. Add a plan OQ row. |
| OQ-2-15 | Lucide version: boards are lucide-react 0.511.0; 1c planned lucide-static 1.66.0; 7 of 30 glyphs differ. | Board shapes (manifest of bundle + board sources); 1c extends the same manifest. Add a plan OQ row. |
| OQ-2-16 | Text field fill: iOS none, 08 suggested `card`. | None (iOS). |
| OQ-2-17 | Reduce Motion: plan "read once at activity creation"; 08 "re-read on resume". | Read in `onCreate` and `onResume` (static call). 3a captures its own value once. |
| OQ-2-18 | Unchecked switch track: plan `input`, 08 `muted`; checked border unspecified in the plan. | `input`; checked border `dawn` (08). |
| OQ-2-19 | Row chevron: SetHome draws 20 px; iOS SF chevron ≈ 16 pt. | 20 dp (looks come from boards). |
| OQ-2-20 | Pill selection haptic. | None (iOS Settings pills have none). |
| OQ-2-21 | `make captures DEVICE=1` had no owner (0e OQ-0e-18). | Built here (task 18), by extending 0e's `BoardCapture` with the same signature. |
| OQ-2-22 | Long Dutch titles in the large top app bar. | One line, ellipsis allowed and marked `ellipsizable()`; record which titles truncate at `small` + `largeFont` in `docs/design-system.md`. |
| OQ-2-23 | How does `src/main` read the debug seed `cf.reduceMotion`? Per-build-type files would need `src/debug`, `src/release` and `src/perf` copies, because perf build types do not inherit `src/release` (0a OQ-0a-1). | `RootOverrides` in `src/main` behind `BuildConfig.DEBUG` (0a's `DebugStrictMode` pattern), with a parity test against `TestSeed` and a release test. 3a may fold it into `LaunchOverrides`. |
| OQ-2-24 | 3c and 5a (Task 4) expect `CfButton.contentPadding`, `CfCard(border)`, `CfBanner(announce)` and a title-only banner, which the first version of this brief lacked. | Add them here with defaults that keep the board look (`contentPadding = null`, `border = true`, `announce = true`, `message: String?`), so 3c/5a need no design-system commit. 5a's optional `CfSecureTextField(visibilityToggleLabel)` is **not** added: iOS reveals passwords only through 5d's switch row. |
| OQ-2-25 | `softShadow`/`skyBackground` reading the theme inside a modifier node vs taking `dark`. | Take `dark: Boolean` explicitly: simpler, no `composed {}`, no node delegation; callers already read `CfTheme.colors.isDark`. |
| OQ-2-26 | Selected settings pill: iOS `SettingsRadioGroup` draws a checkmark before the label; the SetStates pills draw none and give no size. | Draw it (iOS behaviour wins), lucide `check` at `AuthTokens.icon` 16 dp in `dawnText`; the reviewer records the board difference. |
| OQ-2-27 | `cfPress` as a `LayoutModifierNode` with `placeWithLayer` (plan A9 wording) needs a caller-owned `InteractionSource`, but consumers call `cfClickable(onClick, Role.Button)` without one. | `CfPressIndication` (`IndicationNodeFactory`), drawn through one reused `GraphicsLayer` in `draw()`: still a `Modifier.Node`, draw-only invalidation, and `clickable(interactionSource = null, …)` creates the source lazily. |
