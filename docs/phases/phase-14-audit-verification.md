# Phase 14 brief: Audit and verification

- **Branch:** `feat/phase-14-audit-verification`, from the stack top once Phase 13 is in it (and through it every earlier phase). **Wave:** 17. **Milestone:** M6. This is the last phase.
- **Plan entry:** `plan.md` "### Phase 14: Audit and verification" (l.2785–2824). This brief expands it. Where they differ, plan.md wins; report the difference in your hand-back.
- **Boards:** the phase builds **no new screen, state, string or token**. It audits every in-scope board (§5.1: 292 boards in 194 state groups) and fixes differences in screens that earlier phases built. A fix may only move a built screen closer to its board or to iOS; it never adds a screen or state that has no board. If a fix needs a new string or token, stop and add an open question instead.
- **API endpoints:** none. Every seeded run uses the debug mock data source (plan A6). `docs/verification.md` cites endpoints only for the server dependencies S1–S6.
- **Tests:** every test follows AGENTS.md "Tests" (cheapest layer; JVM and warm Robolectric < 1 s, first Robolectric case per SDK 20 s and per class/SDK 3 s, emulator < 10 s; exact queries; `waitUntil…` / `device.wait(Until…)`, never `Thread.sleep` / `SystemClock.sleep`; seeded launches; one fix round that changes nothing means stop and rethink). Read it before Task 1.
- **Who runs what:** your sandbox may be unable to run the Gradle daemon, `adb` or the emulator (`docs/tooling.md`). Then write the code, run what you can, and hand the exact commands to the orchestrator, who runs them and feeds back the output (CLAUDE.md). Most of this phase is emulator work; "no emulator was run" never closes it.
- **Checked 2026-11-16** against iOS `f391a1fa`, web `e36368ec`, the canvas and the earlier briefs. Corrected: the ported auth audit method names (class prefix), the iOS PlanAudit seeds and content tags (`CF_REFRESH_TEST_EMPTY_TRIPS`, `CF_PLAN_TEST_FREEZE_CLOCK`, empty trips waits on `trips.screen`), suppression rule 3 (iOS also ignores touch-target findings on disabled buttons/switches), the 10 signed-in launch-motion files, the trip-actions, settings-home and wizard-from parity seeds, the `launch.opening` tag, the `device` capture segment (6d), the font-scale screen count (9), the window-policy widths, the `CfSegmentedButtons` row, the `@Audit` tier registry rule, the `make a11y` filter, the source sets of `AuditScreens`/`AuditScreensTest`/`mergeAuditDefaults`, the save-resume evidence (9g `WizardProcessDeathTest`) and the canvas file count.
- **Brief written:** 2026-11-16, against plan.md, AGENTS.md, research 12/13/14, the 0a, 0e, 2, 3a–3c, 4c, 4e, 5a–5d, 6a–6e, 7, 9b, 9c, 9f, 10a–10d, 11a, 11b, 12c and 13 briefs, iOS at `f391a1fa`, the web repo at `origin/main` `e36368ec`, the RN `docs/plan-gate.md`, `_design/canvas/canvas.json` and this Mac's toolchain.

Tags: **[F]** a fact verified in a source file or on this Mac (path given). **[R]** a decision of the plan or of this brief. **[verify]** a tool or library detail you confirm on the first run; if it differs, take the stated fallback and record what you found in `docs/verification.md` "Tooling notes".

---

## 1. Goal

When this phase ends:

1. **Every in-scope board state was captured and compared.** `make audit-coverage` reports every one of the 194 state groups (§5.1) as covered by captures in the required cells (§5.2), or exempt with a written reason. Each group was compared with its board HTML and, where one exists, with the iOS reference screenshot. Each difference was fixed in its own commit, or is recorded in `docs/audit.md` (§5.4 format) with its board and the reason.
2. **The iOS reference screenshots have Android twins.** `make ios-parity` produces, for each of the 56 files in `docs/research/screens/ios/`, the seeded Android capture and a side-by-side PNG, or a recorded reason why the state cannot be reproduced.
3. **The Material chrome is captured** for the designer: every row of plan.md "Android spec: Material mapping" in light and dark, en and nl, on the `pixel` emulator, listed in `docs/material-chrome.md` with status "awaiting designer (OQ-A42)".
4. **Accessibility is audited.** The iOS audit suites are ported and pass (Compose/ATF accessibility checks); every other seeded screen passes the same checks; keyboard traversal and Esc work; every designed text pair has a computed contrast or a recorded exception; the font scale renders identically at system 1.3 and 2.0 on the emulator; manual TalkBack, Switch Access and keyboard passes are recorded.
5. **Robustness is proven on the emulator.** Process death on every top-level route ends in a cold launch at the expected route; wizard recovery resumes; fold, split and resize keep the anchor and open sheets; a full emulator run is StrictMode-clean for `net.chronoflow` frames; the T2 matrix (`fold`, `small`, `api29`) passed once.
6. **`docs/verification.md` is the record:** every command with its result, the open device gates, the server dependencies S1–S6 with their verified status, and every open question still on its default.

---

## 2. Preconditions (check first; do not rebuild earlier phases)

Run each check. If one fails, **stop and report it to the orchestrator** with the command and its output. Use the names you find; the ones below are the earlier briefs' names.

| # | Must exist | From | Check |
|---|---|---|---|
| P1 | Phase 13 is in the stack: `docs/release.md`, `docs/play.md`, `make release-check`, `make release-smoke`, `make perf-rn` | 13 | `ls docs/release.md docs/play.md`; `grep -n "^release-check\|^release-smoke\|^perf-rn" Makefile` |
| P2 | `BoardCapture.capture(rule, name)` (`core/testing-compose/…/BoardCapture.kt`), `make captures [ONLY=…]` (Robolectric) and `make captures DEVICE=1` (emulator, output `build/captures/device/`) | 0e T9, 2 Task 18 | `grep -n "^captures" -A12 Makefile` |
| P3 | `ProfileRule`, `ProfileHost`, `Guards`, the five profiles `small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont` in `core/testing/src/main/resources/profiles.json` | 0e | `python3 -I -c "import json;print(sorted(json.load(open('core/testing/src/main/resources/profiles.json'))['profiles']))"` |
| P4 | Tier annotations in `net.chronoflow.core.testing.tiers` (`Logic`, `Layout`, `Snapshot`, `Capture`, `Perf`, `Smoke`, plus the non-tier `PerfBudget`) and 0e's `TierRegistryTest` (emulator classes carry exactly one of `@Smoke`, `@Capture`) | 0e | `ls core/testing/src/main/kotlin/net/chronoflow/core/testing/tiers/` |
| P5 | 0e's `ScreenTest` base class (seeds through `TestSeeds.apply`, then `ActivityScenario.launch<MainActivity>`), `SeedReceiverActivity`, `DebugSeedRouting` with `CF_PLAN_TEST_ROUTE`, `CF_PLAN_TEST_SHEET`, `CF_PLAN_TEST_POSITION`, `CF_SETTINGS_ROUTE`, `CF_WIZARD_BOARD_CAPTURE`, `CF_AUTH_TEST_AUDIT_SCREEN`/`STATE` | 0e, 4e, 5a–5d, 6a–6d, 9c, 10a | `grep -rn "class ScreenTest\|object DebugSeedRouting" app core` |
| P6 | Phase 2's `ContrastTest` (JVM, `core/designsystem/src/test`), `SystemTintTest`, `SystemDialogTintTest`, the design-system gallery captures (`designsystem/<Section>_…`) | 2 | `grep -rln "class ContrastTest\|class SystemTintTest\|class DesignSystemGalleryCaptureTest" core` |
| P7 | `Scripts/emulator.sh` (`ensure`, `down`, `create`, `prep-functional`, ports `pixel` 5554, `fold` 5556, `small` 5558, `api29` 5560), `make avd-create`, `make matrix`, `Scripts/run-tier.sh`/`run_tier.py` with the 600 s idle and 3,600 s total caps | 0a | `grep -n "^matrix\|^avd-create" -A6 Makefile` |
| P8 | `Scripts/fold-check.sh` (written, never run) and its section in `docs/device-matrix.md` | 6e | `ls Scripts/fold-check.sh`; `grep -n "Fold check" docs/device-matrix.md` |
| P9 | `ProcessDeathRestoreTest` (Robolectric) and `:benchmark` `ProcessDeathLaunchTest.killedProcessColdLaunchesWithTheSplash` | 3b | `grep -rn "ProcessDeathRestoreTest\|killedProcessColdLaunchesWithTheSplash" app benchmark` |
| P10 | `DebugStrictMode` (`detectAll().penaltyLog()`, launch-path `penaltyDeath`) | 0a | `grep -rn "object DebugStrictMode" app/src` |
| P11 | Delta records written by earlier phases: `docs/board-deltas.md`, `docs/wizard.md`, `docs/auth.md`, `docs/design-system.md` (contrast gaps, five accepted Welcome pairs), plus each brief's "differences" sections that the phases recorded in the docs they name | 2, 3c, 5a–5d, 6a–6e, 7, 9c–9h, 10a–10d, 11a–11b, 12c | `ls docs/*.md` |
| P12 | `Scripts/test_scripts.sh` runs `Scripts/tests/test_*.py` in `make quick`; `Scripts/tests/test_check_benchmarks.py` shows how a hyphenated script is imported (`importlib`) | 0a, 0e | `cat Scripts/test_scripts.sh` |

Also read `docs/tooling.md` (sandbox capabilities, emulator facts, the seed commands each phase recorded) and `docs/device-matrix.md` before Task 1.

---

## 3. Inputs to read

### 3.1 This repository

- `AGENTS.md`: all of it, in particular "Working rules" (no design, no invention; native components), "Performance" (StrictMode), "Tests", "Commands", "Traps" (animator scales, `native` keyword, announcements, `toM3()` maps `primary` to `dawnText`).
- `plan.md`:
  - "How conflicts between sources are settled" (user decision > iOS at the pin > current boards > web > RN). Every audit verdict uses this order.
  - "Android spec: configuration and appearance", "window policy" (l.357–371), "state survival" (l.373–383), "navigation and routes", "Material mapping" (l.414–448), "seeds and test hooks" (l.465–471).
  - "Performance budget" (device rows), "Risks" 2, 3, 5, 13, 16, "Open questions for the user" (OQ-A1…OQ-A43 and every phase OQ appended since), "Tasks" → "How a phase works" (the never-build board lists, l.623–629), "Standard acceptance for every phase".
  - Every phase's `- **Boards:**` line: the owner of each board (Task 1 uses it).
- Every delta record of P11, and the "Differences"/"Board deltas" sections the earlier briefs asked for (`grep -rln "board-deltas\|Recorded decision\|differs from the board" docs`).
- `docs/research/screens/ios/*.png` (56 files) and `docs/research/12-built-apps.md` §2–§4 (the seeds behind each file).
- `_design/canvas/`: `canvas.json` (334 board entries; 338 `*.dc.html` files: the base boards `Wizard.dc.html` and `WizardSheet.dc.html` and the never-build `WizardOutboundStopNew{,DarkNL}.dc.html` are files without entries; coverage counts entries only), every in-scope `*.dc.html`, `theme.css`, `ds/chronoflow/tokens.json`, `ds/chronoflow/components/bundle.{css,js}`.

### 3.2 Research, quoted

- **`14 §13` (accessibility beyond TalkBack) [R]:** "Every interactive element must be reachable with Tab and arrows in reading order: the timeline's semantics overlay nodes must be focusable, in the same order TalkBack uses; Enter/Space activates; Esc closes sheets and dialogs, as back does; the wizard's 250 ms auto-advance must not fire when focus moves with the keyboard, only on activation." "Switch Access … is covered automatically if the keyboard rules hold … Add one smoke pass." "Expect warnings for: text size, from the 1.3 cap: an intended difference that mirrors iOS xxLarge; contrast on the Welcome eyebrow … Do not 'fix' either away from iOS without a design decision." "Touch targets. 48 dp." "Test at system 1.0, 1.3 and 2.0 (the 2.0 run should look identical to 1.3)."
- **`14 §14` (test and device gaps):** "Fold transitions: the AVD `pixel_9_pro_fold` … with the anchor and an open step sheet." "Process death test: `adb shell am kill net.chronoflow.app.native.debug` while the app is in the background, then relaunch from recents. Assert a cold launch (overlay shown), the Today root, and wizard recovery when it is within 30 min." Profiles named there: `split-narrow` 320×700 dp, `landscape-short` 800×360 dp.
- **`14 §3.4` survival table** — the expected results of Task 13 (§5.6 copies the process-death column).
- **`14 §12` server items S1–S6** (§5.10 has their verified status).
- **`13 §6` snapshot policy:** "Default tier = reference-free render guards … Reference comparisons are opt-in … References are JVM renders … never compare a Roborazzi reference with an emulator screenshot." "Design-fidelity captures … named after the canvas board … for side-by-side review against `_design/canvas`. It never runs by default." "Contrast is computed, not eyeballed. JVM tests check every designed token pair (light and dark) for ≥ 4.5:1 (text) and ≥ 3:1 (large text/UI)."
- **`13 §4.7`:** accessibility row: "Run Compose's accessibility checks (`enableAccessibilityChecks()` in ui-test 1.8+, or ATF via Espresso `AccessibilityChecks.enable()`) on the five plan screens and the auth screens, en only by default; full variants opt-in (iOS I16)."
- **`12 §3`:** the table mapping each iOS screenshot to its seed (Task 4 builds `tools/audit/ios-parity.json` from it).

### 3.3 Facts verified while writing this brief [F]

- **Board scope** (`python3` over `_design/canvas/canvas.json`, 2026-11-16): 334 entries; the never-build lists of plan.md l.623–629 remove **42** (§5.1); **292** remain; grouping wrappers onto their base state (§5.1 rule) gives **194** state groups. Every one of the 292 is named on some phase's `**Boards:**` line in plan.md (no unowned board).
- **Board rendering:** `_design/canvas/` has no `support.js` (every board's `<script src="./support.js">` 404s) and fonts/images load from `/_blob/…`, which is absent. Headless Chrome (`/Applications/Google Chrome.app`) renders a **base** board (`Today.dc.html`) with fallback fonts and broken images; a **wrapper** board (`WizardQAgeDarkNL.dc.html`, a `<dc-import name="Wizard" …>`) renders blank (2.7 KB PNG). So wrappers are compared by reading the base board's branch for the wrapper's props, not by a render (OQ-14-1).
- **Image tools:** `/opt/homebrew/bin/ffmpeg` exists; Python has no PIL; ImageMagick is absent. Side-by-sides use ffmpeg (§5.8).
- **AVDs:** only `pixel` exists (`~/.android/avd/pixel.avd`); `system-images/` has only `android-36`. `make avd-create AVD=api29` downloads `system-images;android-29;google_apis;arm64-v8a` first (0a). `adb devices` lists nothing (no emulator running, no physical device).
- **RN plan-gate format** (`/Users/rickgout/Sites/chronoflow-mobile/docs/plan-gate.md`, read-only): one `### <Board>.dc.html` heading per board, then "Canvas title: … Screen: …", "Checks: …", and bullets **Fixed**, **Open**, **Recorded decision**, **Needs a device**. iOS Phase 8 asks for the same format. `docs/audit.md` uses it (§5.4).
- **Web server state** (read with `git show`/`git grep` on local refs only; **never `git fetch`** — it would modify the read-only repo): see §5.10.

### 3.4 iOS references (read-only: `git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:<path>`)

- **`plan.md:1013` "Phase 6: Audit"** [F]: "Reduce Motion, dark mode, VoiceOver and strings are built in the phases above; this phase only audits. Full UI suite under `-AppleLanguages (nl)`, `-cf.reduceMotion`, and `xcrun simctl ui booted appearance dark`. Files: `AppUITests/AuditTests.swift`. Test: all pass; Dutch strings not truncated at default size." **Android form:** Task 12 (the emulator suite under nl + dark + reduce motion).
- **`plan.md:1037` "Phase 8: Verification"** [F]: "`Docs/plan-gate.md` in RN format (per board: Canvas title / Screen / Checks; **Fixed**, **Open**, **Recorded decision** S1–S6, **Needs a device**) …"; "Device run on the reference ProMotion iPhone: … light/dark … Reduce Motion; VoiceOver; Dutch wrapping; 120 Hz …"; "Comparison report against the budget and an `**Outcome of Phase 8.**` paragraph". **Android form:** `docs/audit.md` (§5.4), Task 16 (device gates), Task 17 (`docs/verification.md` with an "Outcome of Phase 14" paragraph, also appended to plan.md under the Phase 14 entry).
- **`plan.md:2040` "Settings board fidelity (2026-11-09)"** and **`Docs/settings-board-fidelity.md`** [F]: a group table "| Group | Findings addressed | Differences retained |", the rule that native controls keep their system appearance while "the surrounding cards, choices, typography, buttons and notices follow the boards", and an opt-in Debug capture suite that "navigates the real destinations in English/Dutch and light/dark … uses a board name plus language/appearance … No reference images, comparison or recording mode". Transient states use "Debug-only, UUID-gated mock fixtures". **Android form:** the same retained-difference reasoning in `docs/audit.md`; captures stay opt-in and reference-free.
- **`Docs/wizard-fidelity.md`** [F]: "Navigation/glass/search chrome uses real system controls, so its OS rendering differs from the HTML's simulated iOS chrome"; "Server question/exclusion wording remains authoritative … including wording that differs from illustrative boards"; "Progress follows the live question count … so illustrative wrapper percentages are not imposed"; captures "in light/dark and EN/NL (136 attachments)" named "with `Dark`, `NL` or `DarkNL` suffixes". **Android form:** these are valid "Recorded decision" reasons; reuse them verbatim when they apply.
- **`AppUITests/PlanAuditTests.swift`** [F]: methods `testTodayPrimaryState` (content `today.screen`; `today.progress` exists), `testTripsPrimaryState` (content `trips.screen`, route `trips`; button `trip.0199a3c0-0000-7000-8000-000000000001` exists), `testTripsEmptyState` (content `trips.screen`, route `trips`, seed `CF_REFRESH_TEST_EMPTY_TRIPS=1`; `trips.empty` exists and `trips.create-empty` is hittable), `testStepSheetPrimaryState` (content `step.sheet`, `CF_PLAN_TEST_SHEET=step:sleep-sleep-29865450`), `testLegendPrimaryState` (content `legend.sheet`, `CF_PLAN_TEST_SHEET=legend`); `launchPlanAccount` (`AppUITests/PlanUIFlow.swift:10–25`) sets `CF_PLAN_TEST_FREEZE_CLOCK=1`; light; audit types "`.all.subtracting(.dynamicType)`" ("The root xxLarge cap excludes dynamicType"); a finding is ignored only when its measured text passes (contrast) or it is a disabled control: `disabledControl` (l.305–313) is true only for a `.contrast` or `.hitRegion` finding on a button or switch with an identifier that is not enabled. Android's ATF computes contrast itself, so the "measured text passes" branch has no Android counterpart.
- **`AppUITests/AuthAuditTests.swift`** [F]: classes and methods `AuthAuditTests.{testSignInAudit, testTwoFactorCodeAudit, testTwoFactorRecoveryAudit}`, `ForgotAuditTests.testForgot{Form,Sent,Field,Server,Throttle,Offline}Audit`, `ResetAuditTests.testReset{Form,Password,Confirmation,Expired,Done,Server,Throttle,Offline}Audit`, `SignUpAuditTests.testSignUp{Form,Empty,Email,Taken,Password,Confirmation,Visible,Server,Throttle,Offline}Audit`, `VerifyAuditTests.testVerify{Waiting,Countdown,Failed,Server,Throttle,Offline,SignOutError,FailedServer,FailedThrottle,FailedOffline}Audit` (l.650–771; the class prefix is part of every method name, e.g. `testForgotFormAudit`). Default variant en/light only; `AUTH_AUDIT_FULL=1` adds nl/light, en/dark, nl/dark and en/light xxLarge. Seeds `CF_AUTH_TEST_AUDIT_SCREEN` (`sign-in`, `two-factor`, `forgot`, `reset`, `sign-up`), `CF_AUTH_TEST_AUDIT_STATE`, `CF_AUTH_TEST_RESET_FAILURE`, `CF_AUTH_TEST_SIGNUP_FAILURE`, `CF_AUTH_TEST_VERIFY_STATE`, `CF_AUTH_TEST_UNVERIFIED`, `CF_AUTH_TEST_LINK`.
- **`AppTests/WelcomeAuditPolicy.swift`** [F]: contrast findings on `welcome.eyebrow`, `welcome.headline`, `welcome.copy`, `welcome.plan`, `welcome.signIn` are accepted board contrast; contrast findings on elements not fully inside the window are ignored. iOS OQ 33: `(sign-in.guest, contrast)` is the only unconditional auth exception.

---

## 4. Out of scope

- New screens, states, strings, tokens, icons or animations. A board state that was never built stays unbuilt and is listed (OQ-14-7).
- The never-build boards (plan.md l.623–629): superseded, retired health questions, A24 out-of-scope. They are excluded from coverage, not audited.
- Changing any value in plan.md "Performance budget" or re-running Phase 13's perf work. Task 16 runs device rows only if a physical device is attached.
- Stored Roborazzi references (`SNAPSHOT_RECORD=1`) and pixel comparisons between JVM renders and emulator screenshots (13 §6 item 3). Captures here are for human comparison only.
- Fixing server items S1–S6, publishing `assetlinks.json`, Play uploads, any change in `chronoflow-ios`, `chronoflow-new` or `chronoflow-mobile` (read-only; `git fetch` there is forbidden).
- ChromeOS and desktop windowing (OQ-A34), posture UI (`14 §4.2`), RTL beyond the existing LTR coercion.
- LeakCanary (OQ-13-12; OQ-14-9 keeps it out).
- Wiping the `pixel` AVD (only `make emu-clean`, and only if a run demands it) or deleting the T2 AVDs after use (keep them; record their disk use with `make du`).

---

## 5. Fixed values

### 5.1 Board scope and state groups (Task 1)

**Excluded** (plan.md l.623–629; exact names, or prefixes marked `*`):
- superseded: `TripEditor`, `TripEditorErrors`, `EditorStates`, `TripReview*`, `ReviewAnswer`, `ReviewStates`, `TripDiscard*`, `TripCalculating`, `TripNoCredits`, `DarkTripEditor`, `DarkTripReview`, `GuestFlight`, `GuestHealth`, `GuestSleep`, `GuestReview`, `GuestStates`, `WizardOutboundStopNew*`;
- retired health: `WizardQBipolar*`, `WizardQCaffeine*`, `WizardQClot*`, `WizardQEye*`, `WizardQPregnant*`;
- out of scope (A24): `Calendar*`, `LiveActivity`, `DataDownload*`, `SetData`, `SetPaywall`, `SetCancel`, `TripDelete`, `AuthEmailChanged`, `PasswordPromptEmail`, `WizardCheckPractice*`.

This removes exactly 42 of the 334 entries (§3.3).

**Grouping rule** for the 292 in-scope boards: a board `B` belongs to group `G` where `G` is `B` with a leading `Dark` removed (only if the rest is a board, e.g. `DarkSetHome` → `SetHome`), then one trailing `DarkNL`, `Dark` or `NL` removed (only if the rest is a board, e.g. `WizardQAgeDarkNL` → `WizardQAge`; `PlanRemoteLoadingNL` stays its own group because `PlanRemoteLoading` is not a board). Result: 194 groups. The script must reproduce 334 / 42 / 292 / 194; any other count means the canvas changed: stop and report.

**Group kinds** (in `tools/audit/board-scope.json`, each with its reason):

| Kind | Groups | Required cells |
|---|---|---|
| `nonScreen` | `SplashSpec` (geometry spec; 0b/3a `LaunchLayout` tests), `WizardSpec` and `WizardCopy` (rules and copy decks; 9a–9h model and string tests), `AppIcon` (launcher icon; Task 4 launcher shot) | none; listed in `docs/audit.md` with the test that covers them |
| `notBuilt` | `GuestExcluded` (iOS OQ 47; guests use `WizardExcluded`, 11b) plus any group Task 2 proves was never built | none; listed in `docs/audit.md` "Not built" with the phase and reason |
| `wideOnly` | `PlanWide`, `PlanWideScrolled`, `PlanWideDayMenu`, `PlanRemoteWide`, `PlanRemoteWideOffline`, `PasswordPromptWide`, `Set2FAWide`, `Set2FAManageWide` | `foldInner` × {en, nl} × {light, dark} (4) |
| `phoneWithWideSibling` | `Plan`, `PlanScrolled`, `PlanDayMenu`, `PlanRemote`, `PlanRemoteOffline`, `PasswordPrompt`, `Set2FA`, `Set2FAManage` | `pixel8` × 4, `largeFont` × {nl_light, en_dark} (6) |
| `largeFontOnly` | `WizardQShiftLarge` | `largeFont` × {nl_light, en_dark} (2) |
| `standard` | every other group (172) | the 8 cells of §5.2 |

### 5.2 Capture matrix (Tasks 1–2)

A `standard` group is covered when PNGs exist for these 8 cells [R, OQ-14-2]:

| Profile | Cells |
|---|---|
| `pixel8` | `en_light`, `en_dark`, `nl_light`, `nl_dark` |
| `foldInner` | `nl_light`, `en_dark` |
| `largeFont` | `nl_light`, `en_dark` |

- A capture without a profile segment (9f's board-size captures, 390 × 844 dp, density 3) counts for the `pixel8` column. A device capture (`build/captures/device/…`, profile segment `pixel` or `device`) also counts for `pixel8`.
- **Cell exemptions** live in `board-scope.json` `"cellExemptions": {"<Group>": {"<profile>_<lang>_<theme>": "<reason>"}}`. Allowed reasons only: the board itself fixes the language or theme and iOS does the same (e.g. a splash frame has no text); the state needs a system window Robolectric cannot render and the device capture covers it. Each exemption is reviewed (§9).
- Multi-state boards (`AuthStates`, `SetStates`, `SetStatesMore`, `SheetStates`, `TodayRowStates`, `TripSearchStates`, `SplashScreen`): coverage counts the group as covered when every cell has at least one `<Board>_<state>_…` capture; the reviewer checks the state list against the board.

### 5.3 Capture names (0e grammar)

`<Board>[_<state>][_<profile>]_<lang>_<theme>.png` under `build/captures/` (Robolectric) or `build/captures/device/` (emulator); regex `^[A-Za-z0-9][A-Za-z0-9._/-]*$`; `<lang>` ∈ {`en`, `nl`}, `<theme>` ∈ {`light`, `dark`}; `<profile>` ∈ {`small`, `pixel8`, `foldInner`, `landscapeSideNav`, `largeFont`, `pixel`, `device`} (`device` is 6d's segment for its emulator sheet captures, e.g. `ItemDetail_running_device_en_light`; it counts like `pixel`). Parsing: the last two segments are lang and theme; the segment before them is the profile if it is in the profile set; the first segment is the board (a wrapper name such as `WizardCheckingDark` maps to its group). Files under the subdirectories `designsystem/`, `harness/`, `timeline/`, `material/`, `ios-parity/`, `fold-check/`, `resize/`, `font-scale/`, `process-death/` are not board captures and are ignored by coverage.

### 5.4 `docs/audit.md` format (Task 3)

```markdown
# Audit

Date, commit, stack top, how captures were made (commands), how boards were read (§OQ-14-1).

## Summary
| Area | Groups | Covered | Exempt | Not built | Fixed in this phase | Open | Recorded decisions | Needs a device |

## Board comparison
### <Group> (<board files: Plan.dc.html, PlanDark.dc.html>)
Canvas title: <title from canvas.json>. Screen: `<Kotlin file path>`. Owner phase: <id>. Captures: `<names>`. iOS reference: `<file>` or none.

Checks: <what was compared: order, copy keys, tokens, type, spacing, radii, icons, states, 48 dp targets, wrapping at nl/largeFont, foldInner layout>.

- **Fixed:** <difference> (commit `<sha>`).
- **Open:** <difference not fixed and why; needs a decision>.
- **Recorded decision:** <difference kept, the rule that keeps it: iOS at the pin, a user decision, an OQ, a server item S1–S6; cite the source and line>.
- **Needs a device:** <what the emulator/JVM cannot prove>.

## iOS parity (Task 4)
| iOS file | Android capture | Pair | Verdict | Note |

## Not built
| Group | Phase | Reason |

## Accessibility (Tasks 7–11)
## Robustness (Tasks 13–15)
```

Every group gets a heading, even when nothing differs ("No difference."). Merge every delta already recorded in P11 docs into the matching group as **Recorded decision** with "(source: `docs/<file>.md`)"; do not delete the originals.

### 5.5 Accessibility policy (Tasks 6–8)

**Checks:** all ATF checks that Compose's `enableAccessibilityChecks()` runs, thrown for result type `ERROR` [verify the default validator; fallback below].

**Suppressions** (pure function `AuditPolicy.shouldIgnore`, §6 Task 6). A finding is ignored only if one of these is true; everything else fails the test:

| # | Check | Condition | Source |
|---|---|---|---|
| 1 | text contrast | test tag ∈ {`welcome.eyebrow`, `welcome.headline`, `welcome.copy`, `welcome.plan`, `welcome.signIn`} | iOS `WelcomeAuditPolicy.isBoardContrast`; 3c OQ-3c-8 (five accepted Welcome pairs) |
| 2 | text contrast | test tag == `sign-in.guest` | iOS OQ 33; 5a |
| 3 | text or image contrast, or touch-target size | the node is not enabled (`SemanticsProperties.Disabled` present) and has a click action or is toggleable (iOS: button or switch) | iOS `PlanAuditTests.disabledControl` (`.contrast` or `.hitRegion`, l.305–313) |
| 4 | text or image contrast | the node's bounds are not fully inside the window bounds | iOS `WelcomeAuditPolicy.isFullyVisible` |
| 5 | text size / text scaling (ATF `TextSizeCheck` or the check that reports unscaled text) | always | the 1.3 root cap mirrors iOS "`.all.subtracting(.dynamicType)`" (`14 §13`) |

No other suppression without an open question. Each ignored finding is logged once with tag `CfAudit`: `check=<class> tag=<testTag or none> rule=<1–5> bounds=<l,t,r,b>`.

### 5.6 Process-death expectations (Task 13)

From plan.md "state survival" and `14 §3.4`. "Cold launch" = the system splash then the launch overlay are shown (overlay node tag `launch.opening`, 3a l.177, the iOS identifier), then readiness, then the route below. The relaunch is from the launcher (`monkey -p <pkg> -c android.intent.category.LAUNCHER 1`), which restores the task with a saved-state bundle (3b's `ProcessDeathRestore`).

| Case | Reached before the kill (by tapping tags) | Expected after relaunch |
|---|---|---|
| `welcome` | signed out, Welcome | cold launch → Welcome (intro plays: first Welcome of the process) |
| `sign-in-typed` | Sign in, email typed `demo@example.com` | cold launch → Welcome; opening Sign in shows an empty email field |
| `today` | signed in, Today | cold launch → Today |
| `trips` | My trips tab | cold launch → Today (tab root) |
| `trip-sheet` | trip `0199a3c0-0000-7000-8000-000000000001`, a step sheet open | cold launch → Today; no sheet |
| `settings-sub` | Settings → Preferences | cold launch → Today |
| `password-prompt` | Settings → Security → a password prompt open, field typed | cold launch → Today; no prompt |
| `wizard-recovery` | member wizard, From AMS and To NRT chosen, on Flight times | cold launch → Today, then 9c opens the wizard from the `member-wizard` recovery row with AMS and NRT [verify the step 9c restores; record it] |
| `guest-recovery` | signed out, Plan my trip, AMS → NRT, Continue to Sleep | cold launch → Welcome intro → the guest wizard on Sleep with the same airports (11a acceptance 8) |

Not swept on the device (record the reason in `docs/audit.md`): the save resuming in Calculating (plan.md Robustness "Wizard recovery and save resume"). The unseeded debug mock delays each call 300–800 ms (4c l.97), too short to reach Home → stopped → kill mid-save, and a seeded `CF_MOCK_DELAY` launch stores its data under `filesDir/uitests/<id>/`, which the unseeded relaunch does not read. The resume is proven by 9g's Robolectric `@Logic` `app:WizardProcessDeathTest` (`calculatingRowResumesTheSaveWithTheSameIdentity` and its siblings, 9g l.382–389): Task 13 re-runs it (`make targeted ONLY="app:WizardProcessDeathTest"`) and records the result next to the sweep (OQ-14-5); 2FA setup and recovery codes (Gone by design; covered by 10c's ViewModel tests).

### 5.7 Font-scale screens (Task 11)

Seeded on `pixel`, profile `screen`: Welcome (signed out; the settled-Welcome seed 3c's tests use), Sign in (`CF_AUTH_TEST_AUDIT_SCREEN=sign-in`), Today (demo), My trips (`CF_PLAN_TEST_ROUTE=trips`), trip timeline (`trip:0199a3c0-0000-7000-8000-000000000001`, `CF_PLAN_TEST_FREEZE_CLOCK=1`), trip list (`…:list`, frozen clock), Settings home (`CF_SETTINGS_ROUTE=none`), Preferences (`CF_SETTINGS_ROUTE=preferences`), wizard Outbound (`CF_WIZARD_BOARD_CAPTURE=WizardOutbound`), in `nl` (the stress language): 9 screens. System font scales 1.0, 1.3, 2.0 via `adb shell settings put system font_scale <s>`; restore 1.0 in `finally`.

Pass rule: for every node with a `resource-id` (test tags are resource ids, 0e), the bounds in the `uiautomator dump` at 2.0 equal those at 1.3; at 1.0 at least one text node's bounds differ from 1.3 (the scale took effect). Screens whose content depends on the wall clock use the frozen-clock seed (6b `CF_PLAN_TEST_FREEZE_CLOCK=1`) or are compared on tagged nodes only.

### 5.8 iOS parity (Task 4)

`tools/audit/ios-parity.json`: one entry per file in `docs/research/screens/ios/` (56), `{"ios": "<file>", "seeds": {…}, "extras": {…}, "profile": "screen|launch", "wait": "<tag>", "frames": [<ms>…] | null, "skip": null | "<reason>"}`. Seeds come from `12 §3`, mapped to the Android keys of plan.md "seeds and test hooks":

| iOS files | Android seeds and capture |
|---|---|
| `01-launch-t00400ms … t06000ms` (6) | fresh data (`pm clear`), app locale nl (`adb shell cmd locale set-app-locales <pkg> --locales nl-NL` [verify]; fallback extra `cf.settings.language=nl`), profile `launch`, animator scales 1 for this case only (restore 0); `adb shell screenrecord --time-limit 8` from launch; frames at 400, 1000, 1800, 2800, 4000, 6000 ms after the first non-black frame |
| `15-welcome-en` | signed out, `cf.settings.language=en`, `launch`; screenshot after `welcome.plan` is visible and 3.5 s have passed on the device clock (poll `uptime`, no host sleep beyond the poll interval) |
| `40-signedin-launch-motion-{0.5s,1s,1.5s,2s,2.5s,3s,3.5s,4s,5s,6s}` (10) | `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1`, `launch`, animator scales 1 for this case only (restore 0), screenrecord, frames 500/1000/1500/2000/2500/3000/3500/4000/5000/6000 ms |
| `02-today-en-3s`, `02-today-en-7s`, `22-today-nl`, `32-today-dark`, `21-today-…-light` | demo + walkthrough seen, `screen`, `cf.settings.language`/`cf.settings.theme` per file; `wait: today.screen` |
| `03-my-trips`, `33-trips-dark` | `CF_PLAN_TEST_ROUTE=trips` |
| `04-trip-timeline-top`, `30-trip-timeline-dark`, `34-trip-timeline-nl`, `20-trip-timeline-…-light` | `CF_PLAN_TEST_ROUTE=trip:0199a3c0-0000-7000-8000-000000000001`, `CF_PLAN_TEST_FREEZE_CLOCK=1` (6b) |
| `05-trip-list-mode`, `31-trip-list-dark` | `…0001:list` |
| `06-trip-summary` | `…0001` + `CF_PLAN_TEST_POSITION=summary` |
| `07-step-sheet` | `…0001` + `CF_PLAN_TEST_SHEET=step:<id>`; iOS used `sleep-sleep-29865450`; use the first sleep block id of the seeded trip's opening day (as 6e's `fold-check.sh` resolves it) |
| `08-legend-sheet` | `CF_PLAN_TEST_SHEET=legend` |
| `09-block-route-trip2-5s`, `26-block-route-trip2-no-sheet-9s` | `CF_PLAN_TEST_ROUTE=trip:0199a3c0-0000-7000-8000-000000000002` |
| `29-trip-actions` | `…0001` + `CF_WIZARD_BOARD_CAPTURE=TripActions` (the iOS seed, 12 §2.2; 6d opens the sheet once the card exists); `wait: trip.actions.cancel` |
| `10-settings-home`, `11-settings-bodyclock`, `12-settings-preferences`, `35-settings-reminders`, `36-settings-plan` | `CF_SETTINGS_ROUTE` = `none` (iOS used `none`, 12 §3; 10a §5.10: unknown → Settings root), `bodyClock`, `preferences`, `reminders`, `plan` |
| `13-sign-in`, `14-sign-up` | `CF_AUTH_TEST_AUDIT_SCREEN=sign-in` / `sign-up` |
| `23-walkthrough` | demo, `CF_AUTH_TEST_WALKTHROUGH_SEEN=0` |
| `16-wizard-from`, `17-wizard-outbound`, `27-wizard-return`, `19-wizard-qshift`, `18-wizard-check` | `CF_WIZARD_BOARD_CAPTURE` = `WizardWhereEmpty` (the iOS file shows both airports empty, "Choose an airport", Continue disabled; iOS `WizardBoardCaptureSeed.swift:41` empties them for `WizardWhereEmpty` and `WizardAirportFrom`), `WizardOutboundStop`, `WizardReturn`, `WizardQShift`, `WizardChecking` |
| `28-wizard-savejourney-seed-shows-return-step` | `CF_WIZARD_BOARD_CAPTURE=WizardSaveJourney` (9g; diagnostic) |
| `24-large60-diagnostic-*` (3), `24b-large60-*` (2), `25-S3-diagnostic-AMS-SYD` | extra `cf.trip-page=large60` / `S3` (1d entry) |

Every entry whose route is a trip page (`trip:…`) also sets `CF_PLAN_TEST_FREEZE_CLOCK=1`; every signed-in entry sets `CF_AUTH_TEST_SIGNED_IN=demo` and `CF_AUTH_TEST_WALKTHROUGH_SEEN=1` unless its row says otherwise. The table maps all 56 files: 6 + 1 + 10 + 5 + 2 + 4 + 2 + 1 + 1 + 1 + 2 + 1 + 5 + 2 + 1 + 5 + 1 + 6.

Output: `build/audit/ios-parity/<ios basename>` (Android PNG, same name) and `build/audit/ios-parity/pairs/<ios basename>` from `ffmpeg -y -loglevel error -i <ios> -i <android> -filter_complex "[0:v]scale=-2:1600[a];[1:v]scale=-2:1600[b];[a][b]hstack=inputs=2" <pair>`. App launcher icon: one extra entry `launcher-icon` (press Home, screenshot the launcher, crop nothing) for the `AppIcon` group.

### 5.9 Material chrome rows (Task 5)

One row per line of plan.md "Material mapping" (l.414–448). For each, capture at least one in-app instance on `pixel` in en/nl × light/dark (full screen, including status and navigation bars), name `material/<Row>_<context>_pixel_<lang>_<theme>.png`:

| Row | In-app instance (find the call site with `grep -rn "<Wrapper>(" feature app`) |
|---|---|
| `TabBar` | Today with `NavigationBar` (pixel). The rail is evidenced by the Robolectric `foldInner` captures of Today/Trips/Settings; list their names |
| `LargeTitle` | Settings home expanded, and collapsed after one `swipeUp` on its scroll container |
| `Sheet` | step sheet (`CF_PLAN_TEST_SHEET=step:<id>`) and legend sheet |
| `Alert` | every distinct `CfAlertDialog` call site (up to 3; e.g. the password prompt's reset alert), destructive one included |
| `Switch` | Preferences category switches, on and off |
| `Segmented` | every `CfSegmentedButtons` call site (Phase 2's wrapper of `SingleChoiceSegmentedButtonRow`) |
| `DatePicker`, `TimePicker` | wizard Outbound date and time dialogs (`CF_WIZARD_BOARD_CAPTURE=WizardOutbound`, tap the date/time field tags) |
| `Snackbar` | one deterministic trigger; document which (e.g. 10a's language-saved toast) |
| `PullToRefresh` | My trips mid-refresh (`CF_MOCK_DELAY=5`, UiAutomator `swipe`), indicator plus the designed capsule |
| `Progress` | the wizard's 4 dp bar on `WizardWhere` |
| `SystemBars` | status-bar and navigation-bar icon contrast is visible in every full-screen capture; no separate file |

### 5.10 Server dependencies, verified 2026-11-16 at web `origin/main` `e36368ec` (local refs, no fetch)

| # | Status | Evidence |
|---|---|---|
| S1 `assetlinks.json` | **Open** | No `assetlinks` route or file on `origin/main`; only `/.well-known/passkey-endpoints` (`routes/settings.php:48–53`) and the AASA controller (`app/Http/Controllers/AppleAppSiteAssociationController.php`) |
| S2 `android:apk-key-hash` origin | **Open** | `config/fortify.php:150` `'allowed_origins' => [config('app.url')]` |
| S3 passkey management merge | **Open** | `feat/mobile-api-passkey-management` at `1f4a2684` is not an ancestor of `origin/main` (`git merge-base --is-ancestor` fails) |
| S4 delete without `verified` | **Open** | `routes/api.php:83–84` `Route::delete('account', …)->middleware(['verified', 'throttle:api-credential'])`; web `settings/profile` delete sits in the `['auth', 'verified']` group (`routes/settings.php`) |
| S5 production demo account | **Open** | Not visible in code; production data |
| S6 iOS AASA `applinks` | **Open** | `config/services.php:29–35` lists only `webcredentials` apps `2UMFLSCBZ6.net.chronoflow.app.native`, `2UMFLSCBZ6.net.chronoflow.app` |

Task 17 re-runs these read-only commands (`git -C /Users/rickgout/Sites/chronoflow-new grep -n … origin/main -- …`, `merge-base --is-ancestor`) and updates the table if a local ref changed.

---

## 6. Tasks

Each task is one commit unless it says otherwise. Fix commits found by an audit sit between the task commits, one difference (or one tight cluster on one screen) per commit, as `fix(<feature>): <match board|match ios> …`, each with the cheapest test that would have caught it (usually a Robolectric guard or a JVM test; a capture is not a test).

### Task 1: Board scope and capture coverage report

**Commit:** `chore(audit): add board scope and capture coverage report`.

- `tools/audit/board-scope.json` (`"schema": 1`): `excluded` (§5.1, three named lists of exact names and `*` prefixes), `kinds` (§5.1 table), `cellExemptions` (start empty), `notBuilt` (`GuestExcluded` with its reason).
- `Scripts/board-coverage.py` (`python3 -I`, standard library only):
  - inputs `--canvas _design/canvas/canvas.json --scope tools/audit/board-scope.json --captures build/captures --out build/audit`;
  - computes the 334/42/292/194 counts and exits 3 with `board-coverage: canvas changed (<counts>)` if they differ from the constants in the script;
  - parses capture names (§5.3), maps them to groups, computes covered cells per group (§5.2);
  - writes `build/audit/coverage.json` (`{"counts": {…}, "groups": {"<G>": {"kind": …, "boards": […], "cells": {"pixel8_en_light": ["<file>", …], …}, "missing": […], "exempt": {…}}}}`) and `build/audit/coverage.md` (one table row per group: group, kind, owner phase from plan.md's `**Boards:**` lines, covered/required, missing cells);
  - exit 0 when nothing is missing, 1 otherwise, printing `board-coverage: <covered>/<required> groups covered, <n> missing cells`.
- `Scripts/tests/test_board_coverage.py` (import as `test_check_benchmarks.py` does), fixtures under `Scripts/fixtures/board-coverage/` (a 10-board mini canvas and a few empty PNG files):
  - `test_exclusions_and_grouping` (Dark prefix, DarkNL/Dark/NL suffix only when the base exists, `PlanRemoteLoadingNL` stays alone);
  - `test_name_parsing` (with and without profile, wrapper names, device `pixel`, ignored subdirectories, invalid names reported);
  - `test_cells_by_kind` (standard 8, wideOnly 4, phoneWithWideSibling 6, largeFontOnly 2, nonScreen/notBuilt 0);
  - `test_exemption_needs_reason` (an empty reason is an error);
  - `test_real_canvas_counts` reads the real `canvas.json` and asserts 334/42/292/194.
- Makefile `audit-coverage`: `python3 -I Scripts/board-coverage.py --canvas … --scope … --captures build/captures --out build/audit`. Not in `quick` or `check`.

### Task 2: Complete the capture matrix (four commits)

**Commits:** `test(launch): complete the audit capture matrix` (launch overlay, Welcome, walkthrough, terms, policy, auth), `test(trips): complete the audit capture matrix` (shell, Today, My trips, trip page, sheets, errors), `test(wizard): complete the audit capture matrix` (member and guest wizard, guest sign-up), `test(settings): complete the audit capture matrix` (settings, password prompt, reminders).

1. Run `make captures ONLY=<area boards>` per area (keeps each run under the 3,600 s tier cap) and `make captures DEVICE=1 ONLY=<sheets and dialogs>` where Phase 2 recorded that Robolectric cannot render a window, then `make audit-coverage`.
2. For each missing cell, **extend the owning `@Capture` test's parameters** (profile × language × theme through `ProfileRule`) so it renders the cell. Never add a second rendering path, a new seed state or a new screen fixture; if a state needs data the existing fixture lacks, record it as a cell exemption with the reason "fixture lacks <x>" and an open question.
3. A group with no capture test at all and no built screen goes to `notBuilt` with its phase. A group with a built screen but no capture test gets a capture test in its owner module's `src/test/…` (`@Capture`), named `<Feature>AuditCaptureTest`, using the screen's existing preview/fixture state.
4. Each capture method follows AGENTS.md's < 1 s warm Robolectric or < 10 s device budget; split loops into parameters and record the SDK/class warm-ups separately.

Done when `make audit-coverage` exits 0.

### Task 3: Board comparison and `docs/audit.md`

**Commit:** `docs(audit): record the board comparison` (after the fix commits it found).

For every group in `build/audit/coverage.md`, in owner-phase order:
1. Read the base board's HTML branch that the group's wrappers select (`<dc-import name=… screen=… v=… lang=… theme=…>`, then the base board's markup for those props). For base boards you may render a structural preview: `"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu --allow-file-access-from-files --window-size=<w>,<h> --virtual-time-budget=5000 --screenshot=build/audit/boards/<Board>.png file://$PWD/_design/canvas/<Board>.dc.html` (fonts fall back and `/_blob` images are missing: structure only, never a type or color verdict). Never write inside `_design/`.
2. Open the `pixel8` en_light and nl_dark captures, then scan the `foldInner` and `largeFont` captures for clipping, overlap, truncation and the window-policy widths (plan.md l.357–371: Welcome copy card ≤ 560; walkthrough, terms and auth columns ≤ 560, guest sign-up 520; Today/My trips cards ≤ 640; settings 560; wizard 520 from 669; password prompt 540 dp dialog, max height 90% (OQ-A502); sheets ≤ 640; trip list 280, 320 from 900; rail from 600).
3. Compare: element order; every visible string against the en/nl string resources (copy keys, not English text); tokens (color roles via `CfTheme`), type style names, spacing, radii, icons; touch targets ≥ 48 dp; native components per the Material mapping (native chrome rendering differs from the HTML's simulated iOS chrome: that is a Recorded decision, `Docs/wizard-fidelity.md`).
4. Decide with plan.md "How conflicts between sources are settled": board and iOS agree → fix to match; iOS differs from the board → iOS wins, record it; a user decision or OQ default → record it with its number.
5. Fix each "fix" difference in its own `fix(…)` commit (with a test), re-capture that group, then write its `docs/audit.md` section (§5.4).

### Task 4: iOS parity captures

**Commit:** `test(audit): capture ios reference parity`. **Ticks:** "Board audit" (Tasks 1–4 deliver it).

- `tools/audit/ios-parity.json` (§5.8; 56 entries plus `launcher-icon`).
- `Scripts/ios-parity.py`: one device (`ANDROID_SERIAL`, default `emulator-5554`); installs nothing (expects `./gradlew :app:installDebug` first); per entry: `am force-stop`, start `net.chronoflow.app.native.debug/net.chronoflow.debug.SeedReceiverActivity` with `--es CF_AUTH_TEST_ID <uuid4>` plus the entry's seeds and extras; wait for `wait` by polling `uiautomator dump` every 500 ms up to 20 s (fail the entry, continue the rest); `adb exec-out screencap -p`; for `frames`, `screenrecord` then `ffmpeg -ss <t>` extraction relative to the first non-black frame (`ffmpeg … -vf blackframe` [verify]; fallback: relative to the recording start, noted in the verdict); then the ffmpeg pair (§5.8). Writes `build/audit/ios-parity/results.json`. Exit 1 if any non-skipped entry failed.
- `Scripts/tests/test_ios_parity.py` with a fake `adb`/`ffmpeg` on `PATH` (shell stubs in `Scripts/fixtures/ios-parity/bin/`): the exact `am start` arguments for three entries, the poll gives up at 20 s (fake clock), a failed entry does not stop the run, `skip` entries write no files, the ffmpeg command line.
- Makefile `ios-parity` (needs a booted `pixel`; prints `Scripts/emulator.sh ensure` advice otherwise).
- `docs/audit.md` "iOS parity" table: one row per file with a verdict `match`, `difference → <group section>`, or `not reproducible: <reason>` (e.g. iOS glass tab bar vs Material bar: Recorded decision OQ-A6/plan Material mapping; "Floating glass tab bar" is iOS-only).

### Task 5: Material chrome captures

**Commit:** `test(audit): capture the material chrome`. **Ticks:** "Material chrome".

- `app/src/androidTest/kotlin/net/chronoflow/app/audit/MaterialChromeCaptureTest.kt` (`@Capture`, extends `ScreenTest`, parameterized over en/nl × light/dark through `cf.settings.language`/`cf.settings.theme` seeds): one method per §5.9 row. Full-screen shots through `InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()`, written by a helper `DeviceScreenCapture.capture(name: String)` (`app/src/androidTest/kotlin/net/chronoflow/app/audit/DeviceScreenCapture.kt`) into the same additional-output directory Phase 2's device `BoardCapture` uses (Phase 2 Task 18; same `cf.captureOnly` prefix rule with an `assumeTrue` skip), so `make captures DEVICE=1 ONLY=material` copies them to `build/captures/device/material/`. If `:app:connectedDebugAndroidTest` is not yet on the `DEVICE=1` Gradle line (Phase 2: "Later phases add their modules to the Gradle line"), add it in this commit. Each method < 10 s per parameter.
- `docs/material-chrome.md`: the plan.md Material mapping table plus columns "Wrapper", "Where in the app", "Captures", "Tint test" (`SystemTintTest`/`SystemDialogTintTest` method names), "Designer status" = "awaiting designer (OQ-A42)". Add the haptics table rows with "not observable on the emulator; device gate".

### Task 6: Audit tier and accessibility policy

**Commit:** `test(testing): add the audit tier and accessibility policy`.

- `core/testing/src/main/kotlin/net/chronoflow/core/testing/tiers/Audit.kt`: `@Target(AnnotationTarget.CLASS) @Retention(AnnotationRetention.RUNTIME) annotation class Audit` (emulator-only tier, like `Smoke`). Update 0e's tier registry rule and `TierRegistryTest`: an emulator class carries exactly one of `@Smoke`, `@Capture`, `@Audit`; `@Audit` on a `src/test*` class is an error.
- `core/testing/src/main/kotlin/net/chronoflow/core/testing/audit/AuditPolicy.kt` (pure Kotlin, no Android types): `enum class AuditCheck { TextContrast, ImageContrast, TouchTarget, TextSize, Other }`, `data class AuditFinding(val check: AuditCheck, val checkName: String, val testTag: String?, val enabled: Boolean, val actionable: Boolean, val fullyVisible: Boolean)` (`actionable` = has a click action or is toggleable), `object AuditPolicy { fun shouldIgnore(f: AuditFinding): Int? }` returning the §5.5 rule number or `null`.
- `core/testing/src/test/kotlin/net/chronoflow/core/testing/audit/AuditPolicyTest.kt` (JVM): one test per rule (ignored with the right number), `welcomeContrastOnOtherTagFails`, `signInGuestOnlyContrast` (a touch-target finding on `sign-in.guest` is not ignored), `disabledOnlyForContrastAndTouchTarget` (a disabled actionable node's contrast and touch-target findings are rule 3; a disabled non-actionable node's are not; a disabled node's other findings fail), `textSizeAlwaysIgnored`, `unknownCheckNeverIgnored`.
- `app/src/androidTest/kotlin/net/chronoflow/app/audit/AccessibilityAudit.kt`: `fun ComposeTestRule.runAccessibilityAudit(scenario: ActivityScenario<*>)` that runs the checks on the current window, maps each ATF result to `AuditFinding` (test tag from the node's resource id, `enabled`, `actionable` from the view's clickable/checkable state, bounds vs window; the touch-target check class → `TouchTarget`), applies `AuditPolicy`, logs ignored ones (§5.5) and fails with the list of the rest. Use `enableAccessibilityChecks()`/`tryPerformAccessibilityChecks()` [verify the receiver type in ui-test 1.12 for `createEmptyComposeRule()`]; fallback: ATF directly, `AccessibilityValidator().setRunChecksFromRootView(true)` and `validator.check(activity.window.decorView)` inside `scenario.onActivity`, with `androidTestImplementation("com.google.android.apps.common.testing.accessibility.framework:accessibility-test-framework")` pinned to the newest 4.x at least two weeks old in `gradle/libs.versions.toml`. Record which path you used.
- Makefile `a11y`: `Scripts/emulator.sh ensure`, then `$(RUN_TIER) a11y --device --results 'app/build/outputs/androidTest-results/connected/**/*.xml' -- $(GRADLE) :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=net.chronoflow.app.audit -Pandroid.testInstrumentationRunnerArguments.notAnnotation=net.chronoflow.core.testing.tiers.Capture` plus `-Pandroid.testInstrumentationRunnerArguments.cf.auditFull=1` when `FULL=1`. The package filter (not `annotation=…Audit`) is deliberate: it also runs the `@Smoke` `PlanAuditTest` and `AuthAuditTest`, whose full variants exist only under `FULL=1`, and `EscapeKeyTest`; `notAnnotation` keeps Task 5's capture class out. Put every Task 7–9 emulator class in `net.chronoflow.app.audit`.

### Task 7: Port the plan and auth audits

**Commit:** `test(app): port the plan and auth accessibility audits`.

In `app/src/androidTest/kotlin/net/chronoflow/app/audit/`, each extending `ScreenTest`, each method seeds, waits for its content tag with `waitUntilExactlyOneExists(hasTestTag(…), 5_000)`, then `runAccessibilityAudit`:

- `PlanAuditTest` (`@Smoke`, OQ-14-4), every method seeded with `CF_AUTH_TEST_SIGNED_IN=demo`, `CF_AUTH_TEST_WALKTHROUGH_SEEN=1`, `CF_PLAN_TEST_FREEZE_CLOCK=1`, `cf.settings.theme=light`, `cf.settings.language=en` (iOS `launchPlanAccount`): `testTodayPrimaryState` (content `today.screen`; `today.progress` exists), `testTripsPrimaryState` (content `trips.screen`, `CF_PLAN_TEST_ROUTE=trips`; `trip.0199a3c0-0000-7000-8000-000000000001` exists), `testTripsEmptyState` (content `trips.screen`, `CF_PLAN_TEST_ROUTE=trips`, `CF_REFRESH_TEST_EMPTY_TRIPS=1` (iOS `PlanUIFlow.swift:25`; 4c §"out of scope" assigns it to 4e; if `grep -rn CF_REFRESH_TEST_EMPTY_TRIPS app/src/debug` finds nothing, OQ-14-6 applies); `trips.empty` exists and `trips.create-empty` has a click action), `testStepSheetPrimaryState` (content `step.sheet`, `CF_PLAN_TEST_ROUTE=trip:0199a3c0-0000-7000-8000-000000000001`, `CF_PLAN_TEST_SHEET=step:<first sleep block id of that trip's opening day>`; iOS used `sleep-sleep-29865450`, which the rebased Android fixture does not have), `testLegendPrimaryState` (content `legend.sheet`, the same trip route, `CF_PLAN_TEST_SHEET=legend`).
- `AuthAuditTest` (`@Smoke`): `testSignInAudit`, `testTwoFactorCodeAudit`, `testTwoFactorRecoveryAudit`.
- `ForgotAuditTest`, `ResetAuditTest`, `SignUpAuditTest`, `VerifyAuditTest` (`@Audit`): the Swift methods of §3.4, same names. Use the Android seed keys 5a–5d implemented for each state (`grep -rn "CF_AUTH_TEST_AUDIT_STATE\|CF_AUTH_TEST_RESET_FAILURE\|CF_AUTH_TEST_SIGNUP_FAILURE\|CF_AUTH_TEST_VERIFY_STATE" app/src/debug`). A state with no Android seed: add the iOS key to `TestSeed` in `app/src/debug` only if the screen already renders that state from its model (no new state); otherwise mark the method `@Ignore("no seed: <state>, OQ-14-6")` and list it.
- Variants: en/light by default; with the instrumentation argument `cf.auditFull=1`, JUnit `Parameterized` adds nl/light, en/dark, nl/dark (one test case per variant). The iOS xxLarge variant is Task 11's job.
- Typed text (Forgot field errors, Sign up prefills): only through the seeds' prefill paths, as iOS `submitPrefilled`; never type credentials (AGENTS Tests 3).

### Task 8: Accessibility on every other seeded screen

**Commit:** `test(app): audit accessibility on every seeded screen`.

- `core/testing/src/main/kotlin/net/chronoflow/core/testing/audit/AuditScreens.kt` (pure Kotlin, next to `AuditPolicy`, so both the emulator test and a JVM test can see it; androidTest sources are invisible to `src/test*`): `data class AuditScreen(val name: String, val seeds: Map<String, String>, val contentTag: String)` and `val auditScreens: List<AuditScreen>`: one entry per seeded route of the app not covered by Task 7, at least: Welcome (settled), Walkthrough (page 1), Terms update, trip timeline, trip list, trip summary position, trip actions sheet, Today empty/future/completed (the seeds 7 uses), every `CF_SETTINGS_ROUTE` value 10a–10d and 12c route, guest sign-up sheet, and every `CF_WIZARD_BOARD_CAPTURE` value whose `canvas.json` page is `trip` and whose group is in scope. Name = the board name.
- `ScreenAccessibilityAuditTest` (`@Audit`, `Parameterized` over `auditScreens`): seed, wait for `contentTag`, `runAccessibilityAudit`. One case < 10 s.
- `AuditScreensTest` (plain JVM, `app/src/testDebug`: it needs the debug `TestSeed`, and `src/test` is also compiled for `testReleaseUnitTest`, AGENTS "Traps"): names are unique; every name is an in-scope board (read `tools/audit/board-scope.json` and `canvas.json` from the repo root); every seed key is one `TestSeed` parses.

### Task 9: Keyboard, D-pad and Esc

**Commit:** `test(app): check keyboard traversal and escape`.

- Robolectric (`@Layout`, owner module's `src/test`), each with an explicit expected tag list read from the screen code in visual reading order (no `fetchSemanticsNodes` loops): `SignInKeyboardTraversalTest` (Tab from the first field to the guest footer; Enter on `sign-in.submit` submits), `WelcomeKeyboardTraversalTest` (`welcome.plan` → `welcome.signIn`), `WizardChoiceKeyboardTest` (Tab onto a choice card does not select it; Enter selects it and the step advances only after `mainClock.advanceTimeBy(250)`). Reuse 6d's timeline keyboard tests; do not duplicate them.
- Emulator (`@Audit`, `EscapeKeyTest`): `testEscapeClosesStepSheet`, `testEscapeClosesLegendSheet`, `testEscapeClosesPasswordPrompt`, `testEscapeClosesAlert`: seed, `device.pressKeyCode(KeyEvent.KEYCODE_ESCAPE)`, `device.wait(Until.gone(By.res("<tag>")), 5_000)`. If the platform does not route Escape to back [verify], fix it once in the `Cf*` sheet/dialog wrappers (`onPreviewKeyEvent { Key.Escape → dismiss }`) in a `fix(designsystem): close sheets and dialogs with escape` commit with a Robolectric test on the wrapper.
- **Manual passes** (record in `docs/audit.md` "Accessibility", with date, device, steps, result): hardware keyboard on `pixel` (`adb shell input keyevent TAB|ENTER|ESCAPE|DPAD_DOWN`) over Welcome → Sign in, Today → trip page → step sheet, wizard Where → Outbound, Settings → Preferences; Switch Access, one smoke pass (`settings put secure enabled_accessibility_services …` [verify the Switch Access component]; if the image lacks it, "open: device only").

### Task 10: Contrast for every designed pair

**Commit:** `test(designsystem): cover every designed text pair`.

- Extend Phase 2's `ContrastTest` with every foreground/background token pair that a built screen draws and the test does not yet cover (collect them while doing Task 3: text role on surface role, both themes, plus 9 samples along the `cf-sky` ramp for on-sky text). Assert ≥ 4.5:1 for body text and ≥ 3:1 for large text (≥ 24 sp, or ≥ 18.66 sp at weight ≥ 700) and non-text UI (borders of inputs, focus ring).
- A failing pair is never changed in this phase: it becomes an `assertBelow` case with a comment naming its accepted exception (the five Welcome pairs, `sign-in.guest`) or an open question. List every exception in `docs/audit.md` "Accessibility → Contrast".

### Task 11: Font-scale cap on the emulator

**Commit:** `test(audit): check the font-scale cap on device`. **Ticks:** "Accessibility" (Tasks 6–11 deliver it, after `make a11y FULL=1` and the manual passes are recorded).

- `tools/audit/font-scale.json` (§5.7 screens and seeds), `Scripts/font-scale-check.py` (one device; per scale 1.0, 1.3, 2.0: `settings put system font_scale`, wait until `settings get system font_scale` echoes it, seed each screen, wait for its tag, `uiautomator dump` + screencap into `build/captures/font-scale/<screen>_<scale>.{xml,png}`; restore 1.0 in `finally`); pass rule §5.7; writes `build/audit/font-scale.json`.
- `Scripts/tests/test_font_scale_check.py` (fake adb): bounds parsing from a fixture dump, equality at 1.3 vs 2.0 passes, a 1 px difference fails naming the tag, 1.0 identical to 1.3 fails ("scale not applied"), `finally` restores 1.0 after an exception.
- Makefile `font-scale-check`.

### Task 12: Emulator suite under nl, dark and Reduce Motion

**Commit:** `test(app): run the emulator suite under nl, dark and reduce motion`.

- 0e's `ScreenTest`: read instrumentation arguments `cf.audit.language`, `cf.audit.theme`, `cf.audit.reduceMotion`; when set, add `cf.settings.language`, `cf.settings.theme`, `cf.reduceMotion` to every seeded launch **unless the test sets that key itself**. Put the merge in a pure function `mergeAuditDefaults(testSeeds: Map<String, String>, args: Map<String, String>): Map<String, String>` in `core/testing/src/main/kotlin/net/chronoflow/core/testing/audit/AuditDefaults.kt` (`ScreenTest` lives in `app/src/androidTest`, which no JVM test can see) and test it with a plain JVM `AuditDefaultsTest` in `core/testing/src/test/…/audit/` (cases: no args → seeds unchanged; each of the three args added; a key the test sets wins; unknown args ignored).
- Makefile `audit-suite`: the full `:app:connectedDebugAndroidTest` (no annotation filter) with `cf.audit.language=nl`, `cf.audit.theme=dark`, `cf.audit.reduceMotion=1`. Like every device target of this phase (`a11y`, `audit-suite`, `strictmode-run`, `font-scale-check`, `process-death`), it runs under the host-wide device lock: a run-tier call takes `--device`, and anything else is wrapped in `python3 -I Scripts/device_lock.py --` (0a OQ-0a-21, checked by 0a's `test_device_recipes_take_the_lock`).
- A test that fails only because it asserts English text: change it to resolve the string resource in the seeded language (a `test(…)` fix commit). Any other failure is a product bug: `fix(…)` with a test. Dutch truncation seen in the run's screenshots or failures is fixed or recorded (iOS Phase 6 "Dutch strings not truncated").

### Task 13: Process-death sweep

**Commit:** `test(audit): sweep process death on every top-level route`.

- `tools/audit/process-death.json` (the §5.6 cases: `name`, `steps` as a list of `{"tap": "<tag>"} | {"text": "<tag>", "value": "…"} | {"wait": "<tag>"}`, `expectAfter` as an ordered list of tags, `expectAbsent` tags).
- `Scripts/process-death-sweep.py` on the **unseeded debug build** (OQ-14-3): `pm clear net.chronoflow.app.native.debug`; launcher start; for signed-in cases, sign in once through the form with `demo@example.com` / `password` (tags `welcome.signIn`, `sign-in.email`, `sign-in.password`, `sign-in.submit`) and pass the walkthrough if shown; per case: perform `steps`, `input keyevent HOME`, wait until `dumpsys activity activities` shows the app stopped, `am kill <pkg>`, confirm `pidof <pkg>` is empty (poll ≤ 10 s), relaunch from the launcher, then check: the overlay tag appears within 3 s (cold launch), every `expectAfter` tag appears in order within 15 s, no `expectAbsent` tag; screenshots `build/captures/process-death/<case>_{before,after}.png`; `build/audit/process-death.json` with per-case pass/fail and timings. Each poll every 500 ms, bounded.
- `Scripts/tests/test_process_death_sweep.py` (fake adb, fake clock): step translation to `input tap`/`input text` from dump bounds; the kill sequence order (HOME → stopped → kill → pid gone → launcher); a case whose overlay never appears fails as "not a cold launch"; `expectAbsent` violation fails; one failed case does not stop the others.
- Makefile `process-death`.
- Run `make targeted ONLY="app:WizardProcessDeathTest"` once (save resume, §5.6, OQ-14-5).
- Record results in `docs/audit.md` "Robustness".

### Task 14: Fold, split and resize

**Commit:** `test(audit): check fold, split and resize in place`.

- `make avd-create AVD=fold` (if missing), `Scripts/emulator.sh ensure fold`, run 6e's `Scripts/fold-check.sh` unchanged; attach `build/captures/fold-check/*.png` to the record. A failure is a `fix(…)` commit in the trip page.
- `Scripts/resize-check.py` (on `fold`, `emulator-5556`): seed the trip page with a step sheet (6e's seeds), read `wm density`, then apply `wm size` for `split-narrow` 320 × 700 dp and `landscape-short` 800 × 360 dp (px = dp × density / 160, rounded), and `wm size reset` in `finally`; after each: `step.sheet` and `trip.page` still present, the overlay tag absent (no recreation, since `screenSize` is handled in place), screenshot `build/captures/resize/<profile>.png`. Tests `Scripts/tests/test_resize_check.py` (px math at 420 and 390 dpi, reset in `finally`, the assertions).
- Real split screen: try `adb shell am start --windowingMode 3 -n <pkg>/net.chronoflow.app.MainActivity` [verify]; if no shell command produces a split, the orchestrator does it once by hand from Overview and adds one screenshot; record the method used. Expected: the timeline anchor and open sheet survive; width < 280 dp is not required.
- `docs/device-matrix.md` "Fold check" and "Resize": date, AVD, commands, results.

### Task 15: StrictMode scan

**Commit:** `test(audit): add the strictmode log scan`.

- `Scripts/strictmode-scan.py`: reads a `logcat -v threadtime` file; groups each `StrictMode` violation block (the `StrictMode policy violation` line and its following `at …` frames, tag `StrictMode`); classifies **ours** when any frame starts with `net.chronoflow.`, else **third-party**; writes `build/audit/strictmode.json` (`{"ours": [{"type": "DiskReadViolation", "top": "<first net.chronoflow frame>", "count": n}], "thirdParty": […]}`); exit 1 if `ours` is non-empty.
- `Scripts/tests/test_strictmode_scan.py` with fixture logs: a disk-read block with our frame → ours; a block with only `androidx.`/`com.google.` frames → third-party; interleaved threads; a log with no violation → exit 0.
- Makefile `strictmode-run`: `Scripts/emulator.sh ensure`; `adb logcat -G 16M`; `adb logcat -c`; the full `connectedDebugAndroidTest` of every module that has `src/androidTest` (no annotation filter) through `$(RUN_TIER) strictmode`; then `make ios-parity`; then `adb logcat -d -v threadtime > build/audit/strictmode-logcat.txt`; then the scan. Run the whole sequence, from `ensure` to the logcat dump, as one command under the device lock (`python3 -I Scripts/device_lock.py -- sh -c '…'`, 0a OQ-0a-21); its inner `$(RUN_TIER) strictmode` passes `--device` and re-enters the held lock.

### Task 16: T2 matrix, the full runs and the device gates

**Commit:** `docs(audit): record the matrix, strictmode and device runs`. **Ticks:** "Robustness" (Tasks 12–16 deliver it).

Run, in this order, and record each command, exit code, duration and result path:
1. `make avd-create AVD=small`, `make avd-create AVD=api29` (downloads the API 29 image; record `make du` before and after).
2. `make matrix` once. A failure on one AVD is a `fix(…)` commit (likely API 29–30: compat splash, the `device-config` locale path, `POST_NOTIFICATIONS` absent below 33), then rerun only that AVD's smoke. Record per AVD: tests run, passed, wall time.
3. `make strictmode-run`. Any **ours** entry is a `fix(…)` commit (move the work to an IO dispatcher), then rerun. Third-party entries: list each in `docs/verification.md` with its library and why it is not ours.
4. `make audit-suite`, `make a11y FULL=1`, `make font-scale-check`, `make process-death`, `make ios-parity`, `make captures`, `make captures DEVICE=1`, `make audit-coverage`: final green runs on the final commit.
5. **Device gates:** `adb devices -l`; if a physical device is listed, run Phase 13's device rows (`DEVICE_CLASS=reference|lowEnd make perf` and `make perf-timeline`, 1e R5) plus the manual TalkBack pass on it, and record them. Otherwise every device gate stays "open: device only (OQ-A22)". TalkBack on the emulator: `adb shell pm list packages com.google.android.marvin.talkback` [verify]; if present, do the manual TalkBack pass (Welcome reading order and first focus after the overlay (3c OQ-3c-3), Sign in banner announcement (5a), timeline custom actions (6d), wizard Checking live region (9f)); if absent, record "open: device only".

### Task 17: Verification record

**Commit:** `docs(verification): record phase 14 verification`. **Ticks:** "Verification record".

`docs/verification.md`:
1. **Summary:** date, commit, stack top, AVDs and their system images, physical devices (or none), build types.
2. **Commands and results:** one row per command run in Tasks 1–16 and Phase 13's final gates re-run once here (`make quick`, `make check` with warm time, `make test`, `make size`, `make release-check`, `make release-smoke`, `make perf`, `make perf-timeline`): command, date, exit code, wall time, result file (`build/test-results/<tier>/timings.json`, `build/audit/*.json`), verdict.
3. **Audit outcome:** counts from `build/audit/coverage.json`; Fixed / Open / Recorded decision / Needs a device counts from `docs/audit.md`; link to `docs/material-chrome.md`.
4. **Open device gates:** collect from `docs/perf-startup.md`, `docs/perf-timeline.md`, `docs/play.md`, `docs/release.md`, `docs/app-links.md`, `docs/passkeys.md`, `docs/audit.md` ("Needs a device") and the briefs (`grep -rn "open: device only\|device only\|Needs a device" docs`): gate, source, why open, command or procedure that closes it. At least: the reference and low-end perf rows (OQ-A22; the `make perf-timeline` ones block the first production upload unless the user waived them, OQ-A45), 120 Hz frame pacing, passkey sign-in and registration (S1–S3, a Play-signed build), App Link verification (S1), haptics, TalkBack/Switch Access if not done on the emulator, reminder delivery timing under Doze, the RN comparison on hardware.
5. **Server dependencies S1–S6:** §5.10, re-verified with the same read-only commands.
6. **Open questions on their defaults:** every `OQ-A*` row in plan.md and every phase OQ appended there, as "OQ id — default in effect — affected phases"; mark the ones this phase resolved by evidence.
7. **Tooling notes:** every `[verify]` outcome of this brief.
8. **Outcome of Phase 14.** One paragraph (also appended under the plan.md Phase 14 entry): what is verified, what is open, what needs the user.

Also in this commit: plan.md "Open questions for the user" gains OQ-14-1…OQ-14-9 (§10) as the next free `OQ-A` numbers (check the table; never reuse a number); AGENTS.md "Commands" gains rows for `make audit-coverage`, `make ios-parity`, `make a11y [FULL=1]`, `make font-scale-check`, `make audit-suite`, `make process-death`, `make strictmode-run` (all: "audit only; never in `make check`; needs a booted `pixel` unless JVM").

---

## 7. Plan boxes and commits

| plan.md checkbox | Ticked in |
|---|---|
| Board audit | Task 4 (Tasks 1–4) |
| Material chrome | Task 5 |
| Accessibility | Task 11 (Tasks 6–11, plus `make a11y FULL=1` green and the manual passes recorded) |
| Robustness | Task 16 (Tasks 12–16) |
| Verification record | Task 17 |

Fix commits sit between task commits and tick nothing. Every commit is a Conventional Commit with no trailer; generated files are committed with their generator change.

---

## 8. Acceptance criteria

1. `make test` (and so `make check`) runs `AuditScreensTest` green (`:app` JVM tests are not in `quick`). `make quick` passes (< 60 s warm), including `AuditPolicyTest`, `AuditDefaultsTest` and the Python tests `test_board_coverage.py`, `test_ios_parity.py`, `test_font_scale_check.py`, `test_process_death_sweep.py`, `test_resize_check.py`, `test_strictmode_scan.py`.
2. `make check` passes with `pixel` booted, < 5 min warm, including the new `@Smoke` `PlanAuditTest` (5) and `AuthAuditTest` (3). If these push `make check` over 5 min, move them to `@Audit` and record it (OQ-14-4).
3. `make audit-coverage` exits 0: 194 groups, every required cell covered or exempt with a reason; `build/audit/coverage.md` attached to the PR.
4. `docs/audit.md` has a section for each of the 194 groups (§5.4), the iOS parity table with 56 rows plus the launcher icon, "Not built", "Accessibility" (automated results, contrast exceptions, manual passes) and "Robustness".
5. `make ios-parity` exits 0 (skips only with reasons); `build/audit/ios-parity/pairs/` has a pair for every non-skipped file.
6. `make captures DEVICE=1 ONLY=material` writes every §5.9 row in 4 variants; `docs/material-chrome.md` lists them with "awaiting designer (OQ-A42)".
7. `make a11y FULL=1` passes; every ignored finding carries a §5.5 rule number in the log; `ContrastTest` covers every pair found in Task 3.
8. `make font-scale-check` passes: bounds at 2.0 equal 1.3 on all 9 screens of §5.7, and 1.0 differs.
9. `make process-death` passes every §5.6 case; `make audit-suite` (nl, dark, Reduce Motion) passes.
10. `fold-check.sh` and `resize-check.py` pass on `fold`; the split-screen method and screenshot are recorded.
11. `make matrix` passed on `fold`, `small` and `api29` once, with results in `docs/device-matrix.md`.
12. `make strictmode-run` reports zero **ours** violations; third-party ones are listed and explained.
13. `docs/verification.md` has all eight sections; every device gate is either passed with numbers or "open: device only" with the closing command; S1–S6 carry the evidence of §5.10.
14. All five Phase 14 boxes in plan.md are ticked in the commits of §7; `git status` in the three source repos is unchanged (check `git -C <repo> status --short` before and after).

---

## 9. Review checklist (Opus reviewer)

- [ ] **Scope held.** No new screen, state, string, token or seed state. Every `fix(…)` moves a built screen toward its board or iOS and names the board group; none adds UI without a board. Diff `res/values*/strings.xml` and the token outputs: unchanged, or each change has an approved open question.
- [ ] **Coverage is honest.** Open `coverage.json`: counts 334/42/292/194; spot-check 10 random groups' PNGs exist and show the named state; every `cellExemptions` and `notBuilt` entry has a reason that holds (typical mistake: exempting a dark cell because the capture test was inconvenient).
- [ ] **Audit verdicts follow the conflict order.** Pick 10 "Recorded decision" bullets and check the cited source and line (iOS file at `f391a1fa`, OQ number, user decision). A board-vs-iOS difference resolved toward the board without a user decision is a mistake. Native chrome differences are recorded, not "fixed" with hand-drawn chrome.
- [ ] **Compare it yourself.** For Plan, PlanList, Today, Trips, ItemDetail, SetHome, WizardOutbound, WizardReady, AuthSignIn and Main, put the capture next to the base board (HTML read or headless render) and the iOS pair; any unrecorded difference fails the review.
- [ ] **Window policy at `foldInner`:** Welcome card ≤ 560 dp, auth/walkthrough/terms columns 560 (guest sign-up 520), cards ≤ 640 dp, settings 560, wizard 520, trip list 280/320, password prompt dialog 540 (OQ-A502), sheets ≤ 640. **`largeFont`:** no clipped Dutch text; layouts equal the 1.3 cap.
- [ ] **Accessibility suppressions** are exactly §5.5; no tag added to the Welcome or guest lists; no blanket `setSuppressingResultMatcher` for a check class (except rule 5). Audit tests seed and wait on exact tags; no `Thread.sleep`; each case < 10 s (`timings.json`).
- [ ] **Ported audit names** equal the Swift method names; `@Ignore`d methods list a missing seed, not a failing check.
- [ ] **Keyboard tests** use explicit expected tag lists, not traversals; the Escape fix (if any) is in the wrappers, not per screen.
- [ ] **Contrast**: no token value changed; exceptions are asserted as exceptions.
- [ ] **Scripts**: Python with `-I`, standard library only, every wait bounded, `font_scale` and `wm size` restored in `finally`, animator scales restored after the screenrecord cases, fake-adb tests cover the failure paths. No script writes into `_design/` or the source repos.
- [ ] **Process death** runs unseeded and proves a cold launch (overlay seen) for every case; the wizard case checks the restored airports, not just "the wizard is open".
- [ ] **StrictMode**: the scan's "ours" rule reads frames, not the tag; the logcat buffer was enlarged; a fix moved work to IO rather than allowing the violation.
- [ ] **Matrix and device claims**: emulator numbers never written as device numbers; every "open: device only" names the command that closes it; T2 AVDs kept and `make du` recorded.
- [ ] **Source repos untouched**: no `git fetch`/checkout in `chronoflow-new`, `chronoflow-ios`, `chronoflow-mobile`; S1–S6 evidence cites `origin/main` `e36368ec` paths.
- [ ] **Docs**: `docs/audit.md` follows §5.4 for every group; `docs/verification.md` has the "Outcome of Phase 14" paragraph, mirrored in plan.md; AGENTS.md command rows added; new OQs numbered as the next free `OQ-A` ids.

---

## 10. Open questions (proceed on the default; Task 17 adds them to plan.md)

| # | Question | Default |
|---|---|---|
| OQ-14-1 | The local canvas lacks `support.js` and the `/_blob` fonts and images, so wrapper boards render blank and base boards render with fallback fonts. Can the user export board PNGs from the claude.ai Design artifact? | Compare against the base board's HTML values (read the branch the wrapper's props select) and the iOS screenshots; headless renders of base boards are structural previews only. Ask the user for PNG exports in `docs/verification.md`. |
| OQ-14-2 | Plan says "en and nl, light and dark, at pixel8, foldInner and largeFont"; the full cross is 12 cells × 194 groups. | 8 cells (§5.2): all four at `pixel8`, `nl_light` and `en_dark` at `foldInner` and `largeFont`; wide and large-font-only groups per §5.1. |
| OQ-14-3 | Seeds live in in-memory `GraphOverrides`, so a process restarted after `am kill` is unseeded. | Sweep process death on the unseeded debug mock build, signing in once through the form (an audit procedure, like 13's OQ-13-8). If the unseeded mock rejects the demo account, stop and report. |
| OQ-14-4 | Should the plan and auth audits run in `make check`? | Yes for `PlanAuditTest` (5) and `AuthAuditTest` (3), en/light, `@Smoke` (13 §4.7); everything else `@Audit` via `make a11y`. Move them to `@Audit` if `make check` exceeds 5 min. |
| OQ-14-5 | Save resume in Calculating after process death cannot be timed against the 300–800 ms debug mock delay. | Not swept on the device; proven by 9g's Robolectric `app:WizardProcessDeathTest`, re-run in Task 13; recorded as "not swept on device" with that reason. |
| OQ-14-6 | Some iOS auth audit states may have no Android seed. | Add the iOS key in `app/src/debug` only if the screen already renders the state; otherwise `@Ignore` with the reason and list it. |
| OQ-14-7 | A board group that no phase built (for example `GuestExcluded`). | Stays unbuilt; listed in `docs/audit.md` "Not built" with phase and reason; never built here. |
| OQ-14-8 | The emulator image (`google_apis`) may lack TalkBack and Switch Access. | Do the manual passes where the services exist; otherwise "open: device only (OQ-A22)". The automated semantics checks still run. |
| OQ-14-9 | LeakCanary (OQ-13-12). | Not added: it installs an eager initializer, which AGENTS.md "Performance" forbids; record in `docs/verification.md`. |

Inherited questions on their defaults for this phase: OQ-A22 (device gates open until attached), OQ-A34 (ChromeOS not targeted), OQ-A42 (designer confirms the Material chrome), OQ-A12 (Android-only copy "needs review"), OQ-A3 (selected tab color, iOS parity), OQ-A5 (Snackbar toast), OQ-A6 (no glass), OQ-A8 (M3 time picker).

## Refresh notes (2026-11-22, against 92781204)

Refreshed by the orchestrator against `feat/phase-13-release-perf` at `92781204` (top of 13; every phase through 13 is in the stack). **Where a note and the body disagree, the note wins.** Before Task 1, repeat these checks (the greps are in each note) and stop on any further mismatch.

**R1. plan.md line numbers.** Body: Phase 14 entry l.2785–2824. HEAD: Phase 14 entry `plan.md:3676–3716`; "Performance budget" l.513; "Risks" l.568; "Open questions for the user" l.600; "Tasks" → "How a phase works" l.1306. The spec sections the body cites still sit at the cited lines: window policy l.357, state survival l.373, Material mapping l.414, seeds and test hooks l.466. **Instruction:** cite the HEAD numbers. The highest OQ-A number at HEAD is **OQ-A616**. Task 17 appends OQ-14-1… as the next free numbers at commit time (expected OQ-A617 onward; re-check the table, never reuse a number).

**R2. Preconditions P1–P12** all hold at HEAD. Paths that differ from the body's wording:
- P2: `core/testing-compose/src/main/kotlin/net/chronoflow/core/testing/compose/BoardCapture.kt`; `captures:` is at `Makefile:172`.
- P3: `profiles.json` is `{"schema":…, "profiles": {"small", "pixel8", "foldInner", "landscapeSideNav", "largeFont"}}` (a map keyed by alias, not a list).
- P4: the tiers directory also holds `TimingGate.kt`. No `Audit` annotation exists yet; Task 6 adds it.
- P5: `app/src/androidTest/kotlin/net/chronoflow/app/testing/ScreenTest.kt`; `app/src/debug/kotlin/net/chronoflow/debug/DebugSeedRouting.kt`.
- P6: `ContrastTest` is in `core/designsystem/src/test`; `SystemTintTest` and `SystemDialogTintTest` are in `core/ui/src/test/.../designsystem/`; `DesignSystemGalleryCaptureTest` is in `core/ui/src/androidTest/.../designsystem/`.
- P9: `app/src/testDebug/kotlin/net/chronoflow/app/ProcessDeathRestoreTest.kt`; `benchmark/src/main/kotlin/net/chronoflow/benchmark/ProcessDeathLaunchTest.kt`.
- P10: `DebugStrictMode` is `app/src/main/kotlin/net/chronoflow/app/diagnostics/DebugStrictMode.kt` (main source set, debug-gated), not `src/debug`.
- 9g's `WizardProcessDeathTest` is `app/src/testDebug/kotlin/net/chronoflow/app/wizard/WizardProcessDeathTest.kt`.

**R3. Seeds.** `CF_PLAN_TEST_FREEZE_CLOCK` exists (`TestSeed.kt:146`). `CF_REFRESH_TEST_EMPTY_TRIPS` does **not** exist, so OQ-14-6 applies to `testTripsEmptyState` as the body says. The auth seeds at HEAD are `CF_AUTH_TEST_{AUDIT_SCREEN, AUDIT_STATE, ID, LINK, SIGNED_IN, TERMS_CHANGES_PENDING, TERMS_PENDING, UNVERIFIED, WALKTHROUGH_SEEN}`. There is no `CF_AUTH_TEST_RESET_FAILURE`, `CF_AUTH_TEST_SIGNUP_FAILURE` or `CF_AUTH_TEST_VERIFY_STATE`: Android routes those states through `CF_AUTH_TEST_AUDIT_STATE`. **Instruction:** read the accepted `CF_AUTH_TEST_AUDIT_STATE` values in `TestSeed.kt` and the auth debug routing before Task 7, and follow the body's rule for a state with no seed.

**R4. Toolchain and devices.** Only the `pixel` AVD exists, and `system-images/` holds only `android-36`. `Scripts/emulator.sh` maps ports pixel 5554, fold 5556, small 5558, api29 5560. `docs/research/screens/ios/` holds 56 files. `canvas.json` has 334 entries and the canvas 338 `*.dc.html` files. **Instruction:** the T2 AVDs (`make avd-create`, `make matrix`) and the fold check are host work: write the scripts and hand the exact commands to the orchestrator, who runs them under the device lock.

**R5. Phase 13 hand-ins.** `make release-check`, `make release-smoke` and `make perf-rn` exist (`Makefile:202–215`). Test workers have a 3 GB heap and bounded contention (13, `TestConcurrency.kt`). `MediaLaunchClock.sleepUntil` now rechecks the nanosecond deadline (89f3dd7f); launch timing windows are unchanged. Profiles and perf numbers still need a refresh after 89f3dd7f (`docs/play.md`, plan.md Phase 13). That refresh stays with the orchestrator; Task 16/17 lists it as an open gate if it is still open.


**R6. Worker preflight (2026-11-22).** P1–P12 and R1–R5 were repeated on `b2d26cf3`; the source counts and highest OQ number still match. These additional mechanical corrections apply before implementation:
- The wide password prompt is **540 dp**, not 560: `plan.md:368`, OQ-A502 and `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt:47` agree. Task 3 and the review checklist use 540; no production geometry changes.
- AGENTS.md's current budget is **1 s for warm Robolectric cases**, with 20 s for the first case per SDK and 3 s for the first case per class/SDK. The introductory test summary and Task 2 now use those limits instead of the superseded 5 s limit.
- P3's command now reads the profile map keys directly, matching R2.
- Task 2's area runs need an **opt-in accumulation mode**: `Makefile:176` removes all Robolectric captures, and `Scripts/capture-device.py` likewise clears the device directory on every invocation. Add that mode in Task 2, preserve the existing default clean behavior, refresh selected captures rather than counting stale files, and accumulate only fresh results from this audit. Otherwise the four required area runs erase one another before the coverage check.
- Task 5's device routing lives in **`Scripts/capture-device.py`**, not a Gradle command in Makefile. That helper currently chooses either `:app` or `:core:ui`, routes `material` to `:core:ui`, and preserves a subdirectory only for `designsystem`. Extend it in Task 5 to run the app Material capture class, preserve `material/`, and include both modules for an unfiltered device capture. Keep the outer Makefile device lock.

**R7. Task 4 source/seed checks (2026-11-22).** The following refinements belong to the parity task and do not add a product screen or state:
- `CF_SETTINGS_ROUTE=none` is the documented iOS/Android root selector, but `TestSeed.settingsRoutes` omitted it. `DebugShellStart` already converts a recognized selector with no child path to Settings home. Restore the documented selector in the debug parser and prove that routing without booting a graph; use it for parity and font-scale home captures.
- The static mock template contains `sleep-sleep-29865450`, and `MockTripFixtures.rebase` shifts date values while retaining identifiers. Resolve the opening-day sleep block from the committed template rather than relying on the body's assertion that this ID is absent. Tasks 4/7 use the resolved identifier.
- `17-wizard-outbound.png` visibly shows the incomplete multi-leg AMS→DXB→BKK route and Bangkok stop; research 12 §2.2 names `WizardOutboundThree`. Use that existing seed for its twin, instead of the two-flight `WizardOutboundStop` listed in §5.8. Record the platform date/time-control differences in the parity verdict.
