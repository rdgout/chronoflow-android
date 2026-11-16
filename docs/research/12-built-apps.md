# 12 — Observing the built apps (iOS native + React Native)

Date: 2026-11-15. Scope: observe existing build outputs **without rebuilding**, capture reference screenshots, and record behaviour the Android (Kotlin + Compose) app must match.

Legend used below:

- **[V]** verified — seen in a screenshot taken today, or read in source (path cited).
- **[R]** recommendation for the Android app.
- **[?]** open question, with the default the plan should proceed on.

All screenshots live in `docs/research/screens/ios/` (iPhone 17e simulator, 1170×2532 px, @3x, so 390×844 pt) and `docs/research/screens/rn/` (Pixel 8 AVD, 1080×2400 px).

---

## 1. What build outputs exist

| Output | Path | Notes |
| --- | --- | --- |
| iOS Debug simulator app **(used)** | `/Users/rickgout/Sites/chronoflow-ios/build/DerivedData/Build/Products/Debug-iphonesimulator/ChronoFlow.app` | Bundle id `net.chronoflow.app.native`, built 2026-11-12 12:03. Debug, so every `-cf.*` launch argument and `CF_*` UI-test seed works. |
| iOS UI-test runner | same folder, `AppUITests-Runner.app` | Was already installed on the 17e simulator (`net.chronoflow.app.native.uitests.xctrunner`). Not touched. |
| iOS Release simulator app | `build/DerivedData-Release/Build/Products/Release-iphonesimulator/ChronoFlow.app` | Not used. Release ignores most launch arguments (`App/LaunchArguments.swift` `#if DEBUG` branches). |
| iOS device Release | `build/DeviceRelease/Build/Products/Release-iphoneos/ChronoFlow.app` | Not usable on a simulator. |
| RN release APK **(used)** | `/Users/rickgout/Sites/chronoflow-mobile/android/app/build/outputs/apk/release/app-release.apk` | `applicationId net.chronoflow.app`, versionName 1.0.0, versionCode 1, minSdk 24, targetSdk 36, compileSdk 37, built 2026-11-05 12:33, 125 MB. Ships baseline profiles (`baselineProfiles/0/app-release.dm` for API 31+). Mock data source (`.env`: `EXPO_PUBLIC_DATA_SOURCE=mock`). |

[V] The same APK build was **already installed** on the `pixel` AVD (`lastUpdateTime=2026-11-05 12:33:50`, 10 s after the APK timestamp), already signed in as the mock demo user. I did **not** reinstall it, so the AVD's app data is unchanged apart from opening screens.

[R] Use applicationId **`net.chronoflow.app`** for the native Android app only if the RN app is being retired; otherwise both cannot coexist on one device. See open question Q1.

---

## 2. How the iOS app was driven (reproducible)

### 2.1 Safety checks and lifecycle

```sh
pgrep -fl 'xcodebuild|launch-simulators'   # empty → no test run active
U=D788B715-AA0A-4B5A-882B-323E1F984769       # iPhone 17e, iOS 27.2, was Shutdown
xcrun simctl boot $U && xcrun simctl bootstatus $U -b
xcrun simctl install $U .../Debug-iphonesimulator/ChronoFlow.app
xcrun simctl status_bar $U override --time 9:41
# ... captures ...
xcrun simctl terminate $U net.chronoflow.app.native
xcrun simctl status_bar $U clear
xcrun simctl uninstall $U net.chronoflow.app.native   # it was NOT installed before; the runner app was left alone
xcrun simctl shutdown $U                               # left Shutdown, as found
```

No simulator was erased, created or deleted.

### 2.2 Launch arguments and seed environment

Environment variables reach the app with the `SIMCTL_CHILD_` prefix, e.g. `SIMCTL_CHILD_CF_AUTH_TEST_ID=<uuid> xcrun simctl launch $U net.chronoflow.app.native -cf.reduceMotion`.

[V] Seeds used (all `#if DEBUG`):

| Purpose | Env / argument | Source |
| --- | --- | --- |
| Enable UI-test seeding | `CF_AUTH_TEST_ID=<fresh UUID>` (must parse as UUID; gives an isolated Application Support folder `AuthUITests/<id>`) | `Packages/ChronoFlowKit/Sources/DataAccess/AuthUITestConfiguration.swift:20` |
| Fast "screen" profile (skip launch intro, seed routes) | `CF_UI_TEST_PROFILE=screen` | same file, line 21 |
| Signed in as mock demo | `CF_AUTH_TEST_SIGNED_IN=demo` (or `twofactor`) | same file, line 34; `AppUITests/AuthUIFlow.swift:43` |
| Skip/force walkthrough | `CF_AUTH_TEST_WALKTHROUGH_SEEN=1` / `0` | `AuthUITestConfiguration.swift:30` |
| Auth screens | `CF_AUTH_TEST_AUDIT_SCREEN=sign-in|sign-up|forgot|reset|two-factor`, `CF_AUTH_TEST_AUDIT_STATE=form` | `App/Session/AuthSessionServices.swift:263` |
| My trips tab | `CF_PLAN_TEST_ROUTE=trips` | `App/AppTabs.swift:266` |
| Trip page (timeline) | `CF_PLAN_TEST_ROUTE=trip:<tripID>` | `App/AppTabs.swift:268`; trip IDs from `AppUITests/PlanUIFlow.swift:4-7` (`0199a3c0-0000-7000-8000-000000000001` = AMS→HND active, `…0002` = AMS→JFK upcoming) |
| Trip page in List mode | `CF_PLAN_TEST_ROUTE=trip:<tripID>:list` | `App/TripPageDestination.swift:88` |
| Trip page scrolled to summary | `CF_PLAN_TEST_POSITION=summary` | `App/AppTabs.swift:94` |
| Step sheet | `CF_PLAN_TEST_SHEET=step:sleep-sleep-29865450` | `App/AppTabs.swift:61` |
| Legend sheet | `CF_PLAN_TEST_SHEET=legend` | `App/AppTabs.swift:60` |
| Settings sub-page | `CF_SETTINGS_ROUTE=bodyClock|preferences|plan|reminders|profile|language|password|twoFactor|…` (any other value lands on Settings home) | `App/AppTabs.swift:246-259`; `Settings/SettingsHomeView.swift:6` |
| Wizard boards | `CF_WIZARD_AUTO_OPEN=1` + `CF_WIZARD_BOARD_CAPTURE=<Board>` (≈50 boards: `WizardAirportFrom`, `WizardOutboundThree`, `WizardReturn`, `WizardQShift`, `WizardCheck`, `WizardCheckOffline`, …) | `App/AppTabs.swift:314`, `App/Debug/WizardBoardCaptureSeed.swift` |
| Trip actions sheet | `CF_WIZARD_BOARD_CAPTURE=TripActions` with a trip route | `App/TripPageDestination.swift:135` |
| Direct trip page, no session | `-cf.trip-page S3` / `-cf.timeline-fixtures -cf.trip-page large60` | `App/Debug/TripPageDiagnosticRoot.swift`, `Docs/perf-timeline.md` |
| Theme | `-cf.settings.theme dark` (UserDefaults argument domain) | `Settings/SettingsDevicePreferences.swift:13` |
| Language | `-AppleLanguages (en)` / `(nl)`, `-AppleLocale en_US` / `nl_NL` | `AppUITests/AuthUIFlow.swift:57` |
| No launch animation | `-cf.reduceMotion` | `App/LaunchArguments.swift:37` |

The capture helper (kept in the session scratchpad; copy it if needed) did: terminate → `simctl launch` with `SIMCTL_CHILD_*` env → `simctl io <udid> screenshot` at fixed wall-clock offsets from the launch call.

```python
# shot.py name "K=V;K2=V" "args…" t1,t2  (V == "UUID" → fresh uuid4)
env["SIMCTL_CHILD_"+k] = v
subprocess.run(["xcrun","simctl","launch",U,"net.chronoflow.app.native", *args], env=env)
subprocess.run(["xcrun","simctl","io",U,"screenshot",path])
```

Example (trip timeline, light, English):

```sh
SIMCTL_CHILD_CF_AUTH_TEST_ID=$(uuidgen) SIMCTL_CHILD_CF_UI_TEST_PROFILE=screen \
SIMCTL_CHILD_CF_AUTH_TEST_SIGNED_IN=demo SIMCTL_CHILD_CF_AUTH_TEST_WALKTHROUGH_SEEN=1 \
SIMCTL_CHILD_CF_PLAN_TEST_ROUTE=trip:0199a3c0-0000-7000-8000-000000000001 \
xcrun simctl launch $U net.chronoflow.app.native -cf.reduceMotion -AppleLanguages '(en)' -AppleLocale en_US
```

### 2.3 What could not be driven

- **Touch / scroll gestures:** no `idb`, `cliclick`, `axe` or PyObjC/Quartz on this Mac, and `simctl` has no touch injection. So iOS "scrolled timeline", day-pill jumps and tap-through flows were **not** captured; I did not open the Simulator GUI or run XCTest (that would write into the iOS repo's DerivedData). Scroll behaviour below is taken from source and `Docs/perf-timeline.md`, and from the RN app on Android.
- `-AppleInterfaceStyle Dark` is **ignored** (the app forces its own theme from `cf.settings.theme`); `-cf.settings.theme dark` works.
- `CF_PLAN_TEST_BLOCK=<id>` with a trip route opened the trip but **no sheet** within 5 s or 9 s (it only enlarges the step sheet / highlights; it is not an "open detail" seed). Recorded as failed twice; moved on.
- `CF_WIZARD_BOARD_CAPTURE=WizardSaveJourney` showed the "Coming back?" step (the board needs a UI-test-driven continuation).
- `-cf.trip-page large60` still showed the splash at 1.5/3/4/6 s; the timeline was visible at 12 s (Debug simulator; see §4.1).

---

## 3. iOS screenshots captured (`docs/research/screens/ios/`)

| File | Seed | What it shows |
| --- | --- | --- |
| `01-launch-t00400ms…t06000ms.png` | none (fresh install, phone language Dutch) | Cold launch with motion: navy splash + orange moon → sky gradient + sun rising → wordmark + arc → moon flies to top-left → **Welcome in Dutch** ("Plan de verschuiving van je biologische klok…", "Plan mijn reis", "Inloggen"). |
| `15-welcome-en.png` | `-AppleLanguages (en)` | Welcome EN: eyebrow "JET-LAG PLANNER", headline "Plan your clock shift for your next trip", body card, primary "Plan my trip" (orange), secondary "Sign in". |
| `40-signedin-launch-motion-0.5s…6s.png` | signed-in, motion on | Splash → sunrise intro → exit straight into **Today** (by 4 s wall clock incl. process spawn). |
| `02-today-en-3s/7s.png`, `22-today-nl.png`, `32-today-dark.png` | demo | Today: large title, active trip card (orange outline, "Active" chip, "+8 h", AMS→HND with serif IATA codes, "Next · now — Sleep on the flight" row), "Travel day" section with progress "0/3 done", RIGHT NOW (Flight "→ 01:45 · Amsterdam · 4 hr 10 min left"), UP NEXT. Floating glass tab bar (Today / My trips / Settings). NL: "Vandaag", "Reisdag", "OP DIT MOMENT", "HIERNA", "Nog 4 uur 6 min", "+8 u". |
| `03-my-trips.png`, `33-trips-dark.png` | `CF_PLAN_TEST_ROUTE=trips` | My trips: "+" button top-right, ACTIVE / UPCOMING sections of trip cards. |
| `04-trip-timeline-top.png`, `30-trip-timeline-dark.png`, `34-trip-timeline-nl.png` | `trip:…0001` | **Trip timeline**: nav bar "AMS → HND ⌄ / Travel day · Sun 15 Nov", list-toggle + "…" buttons; sticky legend Sleep/Light/Caffeine/Travel; "Show more"; 4 lane grid with hour labels every 2 h (07:30 start, 10:00, 12:00 …); caffeine tile + dashed "no caffeine" blocks, hatched "avoid light" block, teal transfer + flight tiles; **red now line with time pill "21:35"** across all lanes. |
| `05-trip-list-mode.png`, `31-trip-list-dark.png` | `trip:…0001:list` | List mode: per-day header ("Travel day · Sun 15 Nov", zone chip "Amsterdam"), rows with category icon, title, time range, "Important" chip, "now" marker, zone-change header "Amsterdam → Tokyo". Toggle icon switches to a columns glyph. |
| `06-trip-summary.png` | `CF_PLAN_TEST_POSITION=summary` | Plan summary card ("Your plan — We advance your clock 7 hours", explanation, "You're here · Amsterdam 21:36" vs "Destination time · Tokyo 05:36"), day pill row (Before departure / **Travel day** selected in orange / Arrival day) with chevrons. |
| `07-step-sheet.png` | `CF_PLAN_TEST_SHEET=step:sleep-sleep-29865450` | Step detail sheet: icon, "Important" chip, serif title "Sleep", time range + "Amsterdam time", HOW bullets, Skip / ✓ Mark as done, footnote "Checking off changes your progress, never your plan." |
| `08-legend-sheet.png` | `CF_PLAN_TEST_SHEET=legend` | "What the steps mean" legend sheet (SLEEP/LIGHT/CAFFEINE/TRAVEL, each kind with icon + pattern). |
| `09-block-route-trip2-5s.png`, `26-…-9s.png` | `trip:…0002` + block | AMS→JFK "Before departure · Thu 19 Nov" timeline (no sheet opened). |
| `29-trip-actions.png` | `TripActions` | Trip actions sheet over dimmed timeline: "Amsterdam → Tokyo 15–26 Nov", Edit trip, Duplicate trip ("Same flights and answers, new dates"), Cancel. (Active trip: no Delete / Add to calendar.) |
| `10-settings-home.png` | `CF_SETTINGS_ROUTE=none` | Settings: profile card (DT avatar, Demo Traveller, demo@example.com), "Synced just now", Account / Security / Body clock / Preferences / Plan & credits rows with subtitles, Sign out. |
| `11-settings-bodyclock.png`, `12-settings-preferences.png`, `35-settings-reminders.png`, `36-settings-plan.png` | settings routes | Body clock chronotype radio cards; Preferences (Theme System/Light/Dark chips, Time format 24 h/12 h, timeline category toggles); Reminders; "Plan & credits — 3 credits left". |
| `13-sign-in.png`, `14-sign-up.png` | auth audit seeds | Sign in: "Welcome back", passkey button, "or with email", Email/Password, Sign in, "Forgot your password?", "Create an account", "New here? Plan a trip first". |
| `23-walkthrough.png` | `WALKTHROUGH_SEEN=0` | Walkthrough 1 of 3 "Your plan on one timeline", Skip, page dots, Next. |
| `16-wizard-from.png`, `17-wizard-outbound.png`, `27-wizard-return.png`, `19-wizard-qshift.png`, `18-wizard-check.png` | wizard boards | Wizard: "Where are you flying?" (From/To + safety notice), "When are your flights?" (multi-leg with connection pills), "Coming back?", shift-work question, "Checking your trip" animated checklist with dotted arc AMS→NRT. |
| `24b-large60-diagnostic-12s.png`, `25-S3-diagnostic-AMS-SYD.png` | `-cf.trip-page …` | Direct diagnostic trip page (S3 = AMS→SYD 14–30 Oct; large60 = repeated S3, dense caffeine). |

---

## 4. Observed behaviour the Android app must match

### 4.1 Launch / splash (iOS) — [V] source `Packages/ChronoFlowKit/Sources/Launch/LaunchTimeline.swift:54-98`, `LaunchTimeline+Ready.swift:5-16`

Timeline in seconds from the overlay's start (no Reduce Motion):

| Element | Keyframes |
| --- | --- |
| Sky + band opacity | 0 → 0 at 0.3 → 1 at 1.1 (ease-out) |
| Sun + halo | opacity 0 → 1 and translateY 110 → 0 between 0.4 and 1.3 (ease-out) |
| Flight path stroke | strokeEnd 0 at 0.5 → 0.03 at 0.56 → 1 at 1.4; plane visible 0.56–1.4, gone by 1.5 |
| Moon | moves up 28 pt and scales 1 → 0.86 between 0.3 and 1.1 (ease-out) |
| Wordmark | opacity 0 → 1 and translateY 8 → 0 between 1.0 and 1.4 |
| Ready exit | at max(1.5 s, ready): container opacity → 0 over 0.4 s and scale → 1.08 (ease-out). Reduce Motion: no intro, 0.3 s fade. |
| "Slow" status | after 2.0 s a status line fades in (0.3 s) with three pulsing dots (period 1.2 s, 0.2 s stagger) |
| Welcome exit | the moon flies to the wordmark position top-left of Welcome (`withWelcomeExit`, `MoonTarget`) — seen in `01-launch-t04000ms.png` (moon top-left over an empty gradient) then Welcome content. |

Observed wall clock (from `simctl launch`, includes ~0.5–1 s process spawn, Debug build): splash at 0.4 s, sunrise at 1.8 s, wordmark+arc at 2.8 s, moon in top-left at 4.0 s, Welcome / Today fully shown by ≈4–6 s.

[R] Android: use the platform `SplashScreen` API (navy background + moon icon, matching `ds/chronoflow/icon/android`) for the system splash, then hand over to a Compose overlay that replays the same keyframes (same times/curves, `CubicBezier` = `Easing`), with the moon shared-element-style flight to Welcome. Honour `Settings.Global.ANIMATOR_DURATION_SCALE == 0` / reduce-motion like iOS's `-cf.reduceMotion` (skip intro, 0.3 s fade). Do not hold the splash longer than iOS: exit at max(1.5 s, data ready).

### 4.2 Trip page / timeline — the performance-critical screen

[V] From screenshots and `PlanUI/TripPage/TripPageView.swift`:

- Opens with the **current/anchor day scrolled to the top** (`proxy.scrollTo(dayID, anchor: .top)`, line 177), not centred on the now line. In `04-trip-timeline-top.png` the now line (21:35) sits near the bottom of the first screen.
- The day-pill row keeps the selected pill **centred** (`TripPageComponents.swift:105`, `anchor: .center`); the selected pill is filled orange.
- Grid scale: **40 pt per hour** (`Docs/perf-timeline.md`; `PlanCore/PlanMetrics.generated.swift:102`), hour labels every 2 h, four lanes (Sleep, Light, Caffeine, Travel) with a sticky lane legend.
- Now line: red full-width rule with a red pill holding the time, tabular font, only the clock child re-renders each minute.
- Compact header: the day title fades into the navigation subtitle ("Travel day · Sun 15 Nov") when the day header scrolls under the bar; the nav subtitle changes only at day boundaries. (Visible in screenshots as a translucent duplicate of the day title under the glass bar.)
- Timeline ↔ List toggle in the top bar is transient (default Timeline). List mode **replaces** the grid with rows (66 pt nominal one-line row, 8 pt gaps).
- Blocks: filled tiles per category, dashed outline + crossed-cup icon for "avoid caffeine", diagonal hatch for "avoid light", teal travel tiles; tapping opens the step sheet.
- Large plans: 60-day fixture ≈ 54,000 pt of grid; iOS uses lazy day rows and vector display lists capped at 4 MiB / 4 entries.

[R] Android:

- `LazyColumn` with one item per day (keys = day IDs, `contentType` per row kind), each day drawn by **one `Canvas`/`drawWithCache`** per day (or per half-viewport strip for very tall days) using pre-laid-out geometry computed off the main thread (`Dispatchers.Default`), cached by trip/inputHash/detailVersion like `PlanDecoder`.
- Now line as a **separate tiny composable** reading a minute-tick `State` so the day drawings never recompose on a tick.
- Scroll position: read `LazyListState` via `snapshotFlow` / `derivedStateOf` only for day-boundary changes (header subtitle) — never write per-frame scroll state into Compose state.
- Initial position: `scrollToItem(anchorDayIndex)` (top-aligned), day pill row `animateScrollToItem` centred.
- Budgets to adopt (mirroring iOS): S3 decode+layout+prepare ≤ 40 ms, 30 days ≤ 100 ms, 60 days ≤ 200 ms on host/JVM; Macrobenchmark `FrameTimingMetric` on large60 scroll with a baseline profile.

### 4.3 Sheets and navigation

- [V] iOS presents step detail, legend and trip actions as **bottom sheets** with a grabber, dimming the page; RN does the same on Android (`rn/09-item-detail.png`, `rn/10-trip-actions.png`).
- [R] Compose `ModalBottomSheet` with skipPartiallyExpanded=false for the step sheet (iOS medium detent), full for the legend.
- [V] Tab bar: iOS 26 floating glass tab bar that minimises on scroll down (`App/AppTabs.swift` `tabBarMinimizeBehavior(.onScrollDown)`). RN used a Material bottom navigation bar.
- [R] Android: Material 3 `NavigationBar` (3 items: Today, My trips, Settings), optionally hiding on scroll in the trip page to keep the timeline tall; do not imitate the iOS glass capsule.

### 4.4 Copy and localisation observations

- [V] Phone language Dutch → whole app Dutch on first launch (simulator default); English when forced. Dutch tab labels: "Vandaag", "Mijn reizen", "Instellingen"; lane legend "Slapen, Licht, Cafeïne, Reizen"; "Meer tonen".
- [V] iOS shows the zone-chip ("Amsterdam", "Amsterdam → Tokyo") per day header in list mode, and "+8 h" / "+8 u" shift chip on trip cards.
- [V] Date formats: "15 Nov 14:30 → 16 Nov 08:45" on cards; list rows use "15 Nov 2026 14:30 – 16 Nov 2026 01:45".

---

## 5. React Native app on the Pixel 8 AVD (`docs/research/screens/rn/`)

Commands:

```sh
SDK=/opt/homebrew/share/android-commandlinetools
$SDK/emulator/emulator -avd pixel -no-window -no-audio -no-snapshot-save -no-boot-anim &
$SDK/platform-tools/adb wait-for-device   # then poll getprop sys.boot_completed
adb shell am start -W -n net.chronoflow.app/.MainActivity
adb exec-out screencap -p > rn/NN-name.png
adb shell uiautomator dump /sdcard/ui.xml   # to find labels/bounds, then adb shell input tap X Y
adb shell input swipe 540 1900 540 500 120  # scroll
adb emu kill                                 # shut down afterwards (done)
```

Captured: `01-launch-0.5s…6s` (navy splash with moon, gradient), `02-today-empty` ("No trips coming up — Your last trip, Los Angeles → Amsterdam, ended on 7 Nov", Plan a trip, See past trips), `03-my-trips` (COMPLETED section), `04-trip-page-top` (opened on **Day 13**, the last day of a completed trip), `05-trip-scrolled-up`, `06-trip-scrolled-down` ("End of this plan" card), `07-…` (day-pill tap by label failed), `08-trip-list-mode`, `09-item-detail(-opening-150ms)`, `10-trip-actions` (Edit, Duplicate, Add to calendar ›, Delete trip in red, Cancel), `11-settings-home`.

[V] Measurements (headless emulator, SwiftShader software GPU — **not** representative of a device, only relative):

- Cold start `am start -W` TotalTime: 755 ms first, then 341 / 394 / 416 ms (process force-stopped each time).
- 16 fling swipes on the trip timeline (`dumpsys gfxinfo net.chronoflow.app`): 654 frames, 50th/90th/95th/99th percentile frame time **20 / 22 / 25 / 32 ms**, "Janky frames 0.76%" (new metric) but **legacy janky 50%**; GPU 50th 17 ms. The software GPU dominates; re-measure on a real device or a GPU-accelerated AVD.

[V] Behavioural differences between RN and iOS worth noting:

- RN trip page shows an explicit **Timeline | List segmented control and an "Optional items" switch** inside the page; iOS uses a nav-bar toggle icon and hides categories via Settings → Preferences → Timeline categories.
- RN day pills are "Day 11 / 26 Oct"; iOS pills are phase names ("Before departure", "Travel day", "Arrival day") with dates.
- RN's "End of this plan" card exposes planner internals ("Required bright-light sessions end around 22 Oct…", "about 270 min remaining", "Based on 21 nights") — this conflicts with the copy rule in `chronoflow-mobile/plan.md` ("never shows planner internals"). [R] Do not port that card text; follow the iOS end card.
- RN completed-trip actions include **Add to calendar** and **Delete trip**; the iOS active-trip actions show only Edit / Duplicate. [?] see Q3.
- RN uses an Android top app bar with ← and ⋮, sheets with Material styling; the general visual system (navy primary button, orange selection, serif titles, lane colours/patterns) is shared with iOS.

---

## 6. Recommendations summary for the Android app

1. Build the same **debug seed seam** as iOS from day one: intent extras / `adb shell am start -e CF_PLAN_TEST_ROUTE trip:<id>` etc. with identical key names (`CF_AUTH_TEST_ID`, `CF_AUTH_TEST_SIGNED_IN`, `CF_PLAN_TEST_ROUTE`, `CF_PLAN_TEST_SHEET`, `CF_SETTINGS_ROUTE`, `CF_WIZARD_BOARD_CAPTURE`, `cf.trip-page`, `cf.timeline-fixtures`, `cf.reduceMotion`), debug-only (`BuildConfig.DEBUG`), so the same screenshot matrix can be captured on both platforms with one script and compared side by side.
2. Use the iOS screenshots in `screens/ios/` as the visual acceptance reference per screen (light, dark, NL), not the RN ones.
3. Reproduce the launch keyframes exactly (§4.1) with a reduce-motion path.
4. Timeline architecture per §4.2; adopt iOS's host budgets (40/100/200 ms) and add a Macrobenchmark for large60 scroll + time-to-first-timeline-frame; generate a baseline profile (RN already shipped one).
5. Capture device-level frame timing on real hardware; emulator numbers above are only a floor-noise reference.

---

## 7. Open questions (with defaults)

- **Q1 applicationId.** RN uses `net.chronoflow.app`; iOS uses `net.chronoflow.app.native`. Default: **`net.chronoflow.app`** for release (replaces RN in the Play listing), with `.debug` suffix for debug builds so it can coexist on the AVD with the RN install during development.
- **Q2 Initial trip-page scroll.** iOS opens with the anchor day at the top (now line may be off-screen lower down). Default: **match iOS** (day top), not "scroll to now".
- **Q3 Delete / Add to calendar in trip actions.** iOS active trip shows Edit + Duplicate only; RN shows Delete + Add to calendar (calendar is listed as deferred in the RN plan). Default: **follow iOS plan.md** for which actions exist per trip state; omit Add to calendar.
- **Q4 Tab bar style.** Default: Material 3 `NavigationBar`, not a floating glass capsule; hide-on-scroll only on the trip page.
- **Q5 Dark mode source.** iOS ignores the system `AppleInterfaceStyle` launch override but follows the system when theme = System. Default: Android follows `isSystemInDarkTheme()` when theme = System, with the same System/Light/Dark preference.

---

## 8. Housekeeping / state left behind

- iPhone 17e simulator: booted for the session, ChronoFlow Debug app installed then **uninstalled** (it was not installed before), status-bar override cleared, simulator **shut down** (as found). The pre-existing UI-test runner app was not touched.
- Pixel AVD: booted headless, RN app (already installed, same build) opened and navigated read-only (no sign-out, no trip edits), emulator killed with `adb emu kill`.
- No files written in `chronoflow-ios`, `chronoflow-new` or `chronoflow-mobile`.
