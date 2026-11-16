# 13 — Testing and tooling discipline (iOS → Android)

Status: research, 2026-11-15. Sources are read-only. Paths are relative to `/Users/rickgout/Sites/chronoflow-ios` (shown as `iOS …`) unless they say otherwise.

How to read this file:
- **Fact** means verified in the iOS source, with a citation.
- **Rec.** is a recommendation for the Android app.
- **OQ-T*n*** is an open question with the default the plan should use (§10).

Related research: `01-ios-plan-reuse.md` §7–§8 (perf budgets, lessons) and `09-rn-android-lessons.md` §4 (timeline perf). Where this file and those disagree, this file cites the newer source (§4.3).

---

## 0. Summary

The iOS testing discipline exists because of incidents. Its rules carry over almost unchanged:

1. Use the cheapest layer that can fail.
2. Write at most one UI journey per feature.
3. Launch every UI test seeded.
4. Use exact queries and back-off waits.
5. Give every test a hard time budget.
6. Never run a device matrix by default.
7. Keep reference snapshots opt-in until a person has approved them.
8. Run only what you touched.
9. Stop after a fix round that changes nothing.

Android makes several of the hardest iOS mechanisms *simpler*:
- Instrumented Compose tests run **in the app process**. The iOS out-of-process reseed transport (custom URL scheme, UUID authorization, PID check) shrinks to a debug-only in-process `TestSeeds.apply()` plus an `ActivityScenario` relaunch.
- Robolectric with Roborazzi gives the analogue of hosted `AppTests` on the JVM, with no emulator.
- `DeviceConfigurationOverride` replaces `SnapshotContainer`.

The perf gates map onto Macrobenchmark:
- `StartupTimingMetric`;
- `TraceSectionMetric` for the first timeline frame;
- `FrameTimingMetric`;
- `MemoryUsageMetric`.

Those gates need a small results checker that fails on a missing metric. iOS reached the same conclusion: "missing metrics … cannot silently disable a gate".

---

## 1. The iOS rules, with the incidents behind them (Fact)

### 1.1 The Tests section of `AGENTS.md` (user, 2026-11-10)

Source: `iOS AGENTS.md:30-58`. The preamble gives the reason: "Wizard tests that ignored these rules cost a morning of 3–10 minute hangs and five fix rounds aimed at a harness artifact."

| Rule (verbatim intent) | Incident / reason |
|---|---|
| **Pick the cheapest layer that can fail.** Logic (validation, timing, retries, offline, persistence, computed values, multi-step flows) goes in SwiftPM unit tests. Layout and geometry go in hosted AppTests. XCUITest is only for what needs the real app: native controls, navigation, system UI and appearance. "Never test logic through a screen." | Wizard t6 (`Docs/wizard-tests-t6.md`): the long UI journey moved to SwiftPM `WizardReviewFlowTests.stopOneWayUnsureSleepAndScreeningReachFineTuneThenSave`. `Docs/ui-test-tiers.md:11-35` maps every removed UI loop to the host test that covers it. |
| **One UI journey per feature, at most,** as a smoke test. Every other UI test seeds a state and checks one screen. | The streamline saved 15 app launches and 42 typed sign-ins (66 with the full audit) (`ui-test-tiers.md:41`). |
| **UI tests launch seeded.** Use `ScreenTestApp` with `AuthUIFlow.launchSignedIn` plus a board or fixture env (e.g. `CF_WIZARD_BOARD_CAPTURE`). Launch once per class and reseed between tests. Never sign in through the form or click through earlier steps. Only auth tests drive sign-in. | Typed sign-in and click-through dominated the wall time. A cold launch costs p50 3.3 s (§3.4). |
| **Query exactly:** `app.buttons["id"]`, `.firstMatch` when needed. No `descendants(matching: .any)`, no whole-tree predicates, no loops over element properties. | Whole-tree snapshots are slow and flaky in XCUITest. The streamline made every query role-typed (`ui-test-tiers.md:135`). |
| **Wait with `UIWait`** (back-off polling), never fixed sleeps. | Policy: an immediate check, then 50 ms × 1.5 back-off capped at 1 s, with snapshot time counted in the timeout. It never spins on the app's main actor (`ui-test-tiers.md:119`, `AppUITests/UIWait.swift`). |
| **Budgets:** a UI test < 10 s, a hosted test < 5 s. `executionTimeAllowance = 60` (XCTest's minimum), "so a hang fails in a minute rather than 10". Split profile × language × size loops into one test per profile. "A test that needs more time is a design smell: fix the test, don't raise the allowance." | 3–10 min wizard hangs. The 60-day swipe test needed ≈ 689 s against a 600 s allowance (§1.4). Allowances now in source: 28× `60`, 4× `120`, 1× `1200` (the opt-in full audit) (`grep executionTimeAllowance AppUITests AppTests`). |
| **Know what the harness can show.** `AuthTestHost` simulates insets with `safeAreaInset` outside the view, so horizontal insets do not reach screens inside a `NavigationStack`. Check Duo rail fit with one seeded `simctl` screenshot instead. "When a check can't fail for the real reason, don't keep fixing the app to satisfy it." | Wizard hosted horizontal-inset assertions were removed because "that harness cannot exercise the claimed safe-area behavior" (`wizard-tests-t6.md`). The user's real Duo screenshots supplied the evidence instead. |
| **Running tests:** run only the failing method (`ONLY="AppTests/Suite/testMethod"`). Run only the suites you touched. Each fix round runs only what the change touches. Run `make quick` once, before pushing a PR, not every round. Read the suspect diff before bisecting, and bisect only failing methods, one build per step. **After one fix round that changes nothing, or a hang, stop.** | Five fix rounds aimed at a harness artifact. Repeated full-suite reruns (a 1,244 s default tier). |

`CLAUDE.md` (iOS) adds an orchestration rule. Every brief that adds or changes tests must point to these rules, and "Reject a test that breaks them in review, before it reaches the simulator."

### 1.2 Test tiers (user decision 2026-11-06)

Facts: `plan.md:68-86`, `Docs/tooling.md:179-196`, `Makefile`.

| Tier | Command | Coverage | Target |
|---|---|---|---|
| Inner loop | `make quick` = `lint host-tests` | Strict `swift format lint`, all SwiftPM host tests (ChronoFlowKit, TokenGen, AssetGen), Python resource/runner fixtures | **< 1 min warm** |
| Targeted | `make targeted DEVICES="17e" ONLY="…" [REUSE_BUILD=1]` | Build once, then run the selected suites/methods | **< 2–3 min after build** |
| Commit gate | `make check` = `quick generators-check simulator-check` | + generator/font freshness. On 17e: hosted AppTests with fixed worst-case fixtures, the UI smoke set, isolated Release readiness | **< 5 min** |
| Snapshots | `make snapshots` / `SNAPSHOT_RECORD=1 make snapshots` | Reference comparisons on 17e (opt-in) | — |
| Perf | `make perf` (launch), `make perf-timeline` | Release `Perf` scheme on 17e | 300 s allowance per timeline test |
| Duo | `make duo` | Opt-in, only for Duo-specific runtime changes | — |
| Matrix | `make matrix` | All seven runtimes, **only on request** | never in default/CI |

- User quote: **"I want you to skip the 7 simulator matrix unless asked for"** (`plan.md:70`). Device fit moved to:
  - pure Mac device-table tests over `Packages/ChronoFlowKit/Tests/SnapshotSupport/profiles.json` (seven devices: points, scale, all four insets, Duo's 19 pt right rail);
  - hosted worst-case fixtures inside one 17e.
- `Scripts/ci.sh` = `make generate check`. Nothing runs on PRs ("Local builds only", `plan.md:130`).
- The suite policy registry is `Scripts/launch-simulators.py` (`SUITES`, policies `logic | matrix | release | snapshots | reference | perf | capture`). A Python fixture (`Tests/test_launch_simulators.py`) fails when a suite is unregistered or a default run selects an opt-in suite.
- **Honest status:** the < 5 min check target was **never met** on the user host:
  - 408 s failing (round 2, `plan.md:85`);
  - 470 s for selected suites including builds (`ui-test-tiers.md:82`);
  - cold `make check` 1,104 s on `04727a2` (`plan.md`, Forgot flake section).

  Android should treat the targets as hard design constraints from day one, not as aspirations.

### 1.3 The hosted-tier policy (user decision 2026-11-09)

Source: `ui-test-tiers.md:51`.
- "Package tests own logic."
- "Hosted screen tests render fixed states and assert only native rendering, measured layout, scrolling or App-target wiring."
- "The root xxLarge cap makes xxLarge the layout stress case; appearance changes pixels, not geometry."

What changed:
- Large device × language × category loops became **one test class per state** on SE + asymmetric Duo at xxLarge (`ui-test-tiers.md:59-66`).
- Dutch is the stress language for plan copy, so there are no 17e duplicates.
- Geometry-only waits stopped rasterising. The rule: wait for three identical, nonempty frame sets and capture pixels once per root (`:63`).

### 1.4 Incident log (why each rule exists)

| # | Incident | Source | Rule it produced |
|---|---|---|---|
| I1 | Wizard UI tests that walked flows and signed in through the form: a morning of 3–10 min hangs and five fix rounds aimed at a harness artifact | `AGENTS.md:30` | The whole Tests section |
| I2 | A 60-day timeline swipe test timed out. At ≈ 2.7 s/gesture + 0.17 s/end-query, 240 gestures ≈ 689 s, more than the 600 s allowance. `.fast` sets velocity, not distance. No offset samples were logged, so nothing could be diagnosed. | `Docs/perf-timeline.md` "Why the old end-to-end swipe test timed out" | A fixed 15 gestures per measured iteration, a separate "reach last day" test through the day picker, and progress logged / failing if no progress |
| I3 | Reseed acceptance: the default UI tier ran 1,244 s with 2 workers and 47 failure lines, 43 of them missing reset acknowledgements. Cause: `XCUIApplication.open(_:)` *launches* instead of delivering to the running scene. | `ui-test-tiers.md:172-193` | Deliver through `XCUIDevice.shared.system.open`. Acknowledge with a nonce window and require the same PID. Fail, never fall back silently. After the fix: 840 s (`:261`). |
| I4 | Each `MockDataSource` cached its own server snapshot; the shared memory store went stale across factories | `ui-test-tiers.md:206-210` | Requests refresh the shared snapshot. Mutations apply atomically to the latest state. |
| I5 | `CF_MOCK_DELAY` was silently clamped to 300–800 ms by the ordinary mock | `ui-test-tiers.md:214-217` | Gated UI mock delay is exactly the validated value, default **0** |
| I6 | Erasing simulators while tests or clones run corrupts the shared dyld cache ("no dyld cache") | `AGENTS.md` Traps | `make sim-clean` refuses while a run is active |
| I7 | Headless simulators minimise the software keyboard when a hardware keyboard is attached | `AGENTS.md` Traps, `tooling.md` | The runner sets `AutomaticMinimizationEnabled=false` per device. Keyboard helpers require ≥ 100 × 100 pt of keyboard inside the window. |
| I8 | Disk: 23 worktrees > 150 GB (2026-11-10). Xcode test clones 203 GB (2026-11-11). | iOS `CLAUDE.md` | Remove worktrees, `build/` and clones at the end of each branch, and report `du -sh` |
| I9 | Hosted harness horizontal insets never reach `NavigationStack` content | `AGENTS.md`, `wizard-tests-t6.md` | Don't assert what the harness can't show; use one real-device screenshot |
| I10 | Timeline host budget failed under concurrent compiler load | `plan.md:1977`, `plan.md` contrast follow-up | Run budgets on an idle machine; "investigate a failure instead of automatically increasing limits" |
| I11 | Runner attached every log failure to every suite when an xcresult was unreadable | `ui-test-tiers.md:302-309` | One pass-level extraction failure. Missing results still fail. "a passing log alone never converts missing result evidence into a pass" |
| I12 | Xcode's automatic diagnostics collection slowed every failing pass | `tooling.md`, `AGENTS.md` | `-collect-test-diagnostics never` by default. `TEST_DIAGNOSTICS=1` opts in. |
| I13 | Pull-to-refresh "Up to date" capsule (2 s) dismissed before XCTest returned from its gesture wait | `plan.md` streamline batch 1 repair | A Debug-only `onAppear` counter (`CF_REFRESH_TEST_COUNTER`) proves presentation; don't race transient UI |
| I14 | Hosted `AppTests` that linked package products again duplicated resource-bundle accessors and crashed | `AGENTS.md` Traps | The test target depends only on the app |
| I15 | Implementer sandbox (Codex) could not run CoreSimulator, so dozens of rounds ended "No simulator was run" | iOS `CLAUDE.md`, all phase notes | The orchestrator runs the simulator tiers and feeds back exact output |
| I16 | Auth audit cost exploded with five variants per state | `tooling.md:188-200` | One method per distinct screen state (37), en/light/default, one launch each; full variants opt-in (`make audits`, 5,400 s cap); suite target < 15 min |
| I17 | Runner per-pass hard limit (1,200 s) killed long but healthy passes | `tooling.md` (timeouts) | Idle timeout 600 s on log growth, overall cap 3,600 s, then SIGTERM → 5 s → SIGKILL to the process group |

---

## 2. Budgets (Fact)

### 2.1 Test duration budgets

| Budget | Value | Source |
|---|---|---|
| UI test | < 10 s | `AGENTS.md:46` |
| Hosted test | < 5 s | `AGENTS.md:46` |
| Hang allowance | `executionTimeAllowance = 60` (screen default; wizard 180 s earlier, later bounded to 60; full audit variants 600 s) | `AGENTS.md:47`, `ui-test-tiers.md:133`, `wizard-tests-t6.md` |
| Swift Testing hang | `@Suite(.timeLimit(.minutes(1)))` on async suites | e.g. `Tests/DataAccessTests/APIClientTests.swift:7` |
| Per-test reseed overhead | "target under roughly 1 s" (reset begin → first screen element) | `ui-test-tiers.md:166` |
| Auth audit suite | < 15 min on 17e | `tooling.md:191` |
| quick / targeted / check | < 1 min warm / < 2–3 min after build / < 5 min | `tooling.md:186-190` |
| Runner pass | idle 600 s, overall 3,600 s (`make duo` 600 s) | `tooling.md` timeouts table |

### 2.2 Measured UI launch timings (17e simulator, user host)

On `caa72aa` (`ui-test-tiers.md:266-275`), p50 / p95 in seconds:

| Interval | p50 | p95 |
|---|---:|---:|
| launch begin → process entry | 2.070 | 3.438 |
| process entry → overlay committed | 0.212 | 0.250 |
| overlay committed → session ready | 0.717 | 1.161 |
| session ready → exposed root | 0.077 | 0.212 |
| launch begin → launch return | 3.330 | 4.752 |
| reset begin → reset return | 0.816 | 1.293 |

Reading: a cold launch costs ≈ 3.3 s and a reseed ≈ 0.8 s. Most of the cold-launch cost is XCUITest/process startup, not the app.

### 2.3 Launch performance budget

Source: `plan.md:2286-2304` ("Proposals; OQ 14 confirms").

| Metric | Target |
|---|---|
| Launch to first frame | ≤ 400 ms on the reference device and below the RN median (`XCTApplicationLaunchMetric`, 10 runs, Release) |
| Scene connect → overlay committed | ≤ 16 ms (`first-commit` signpost) |
| First frame → first animated change | 300 ms + ≤ 1 frame |
| Session known, signed out | ≤ 50 ms after connect |
| Hand-off end (ready before 1500) | 1900 ms ± 1 frame |
| Welcome interactive | 2550 ms ± 1 frame |
| Last intro item | 3200 ms ± 1 frame |
| Hitches 0–3.2 s | < 1 ms/s, none longer than 1 frame, also with a 500 ms main-thread stall |
| Frame rate | ≈ 8.3 ms intervals on ProMotion |
| Peak memory / CPU to Welcome | below RN |

Notes:
- Measured so far: AppLaunch average **2.09 s** on the 17e simulator, from the Perf run (`perf-timeline.md` results table). Simulator values are not device claims.
- The device D3 stall proof is still **open** (`Docs/perf/2026-11-06-d3-spike.md`: "needs a device").

### 2.4 Timeline performance budgets

Source: `Docs/perf-timeline.md`.

Host (every sample; Debug and Release use identical limits; ten XCTest samples, each a cold decode + `layoutPlan` + `preparePlan`):

| Fixture | Budget per sample | Latest mean |
|---|---:|---:|
| S3 | **40 ms** | 19 ms |
| 30 days | **100 ms** | 50 ms |
| 60 days | **200 ms** | 92 ms |

Optimized UI (`PerfUITests/TimelinePerfTests`, Perf configuration, 17e):

| Metric | Hard gate per iteration | Aim |
|---|---:|---:|
| First visible 60-day timeline frame (`time-to-first-timeline-frame` signpost) | **≤ 0.8 s** | ≤ 0.45 s (sim 0.34–0.41 s; supplied run 0.530 s) |
| Scrolling hitch time ratio | **≤ 5 ms/s** | ≤ 1 ms/s |
| Peak physical memory during 15 gestures | **≤ 150 MiB** | ≤ 80 MiB (sim 57.9 MB) |
| Vector drawing / cache (host) | ≤ 512 KiB per drawing; ≤ 4 MiB and 4 entries | — |

How the gates are enforced:
- **Invalidation guard (host, in quick):** `TimelineInvalidationTests` probes all 60 days in Timeline and List modes.
  - 120 offset changes plus an anchor boundary: **0** static rows and **0** clock children invalidated.
  - One minute tick: 0 rows, **1** clock child.
  - Crossing a day boundary: 0 rows, **2** clock children.
- **Shared metric adapter (`TimelineBudgetMetric`):** missing, non-finite or unknown-unit metrics fail. An expected-failure test injects 0.81 s to prove the gate fires. **No baseline plist.**
- The hitch gate is enforced only where the metric is reported: on a device, not the simulator.
- **Fourth layer:** on-device Instruments review by the user before release (Animation Hitches, Time Profiler, Allocations; S3 and large60).

### 2.5 Conflicting memory budget (flagged)

- `plan.md:1733` says "interval physical-memory growth ≤ 40 MiB".
- The newer `Docs/perf-timeline.md` replaced it with **peak** physical memory ≤ 150 MiB. The reason: "on the simulator the `.physical` value equals the absolute footprint, so a growth budget would mislead".
- `01-ios-plan-reuse.md` §7 and `09-rn-android-lessons.md` §4 quote the 40 MiB growth figure.

**Rec.:** use the newer rule, peak ≤ 150 MiB hard and ≤ 80 MiB aim. Track growth only as a reported value (OQ-T5).

---

## 3. The seeded-launch mechanism (Fact)

### 3.1 Pieces

**`AppUITests/ScreenTestCase.swift`**
- The base class sets `continueAfterFailure = false` and `executionTimeAllowance = 60`, and calls `ScreenTestApp.beginClass`.
- `ScreenTestApp` keeps **one static `XCUIApplication` per active class**. It counts `coldLaunches` and `reseeds`, and terminates the app in class teardown.
- `reseedIfPossible` compares the requested seed with the immutable cold-launch seed (`UITestSeedCommand.changedLaunchSettings`). If only `CF_*` fixture keys changed, it reseeds in-process. If any launch setting changed (`-Apple*`/`-UI*` args, `-cf.settings.language/theme`, `-cf.reduceMotion`, `-cf.mode`, `-cf.freezeAt`, `-cf.readyAt`, `-cf.mainThreadStall`, `-cf.trip-page`, `-cf.session`, `-cf.timeline-fixtures`, `CF_UI_TEST_PROFILE` or any non-`CF_` env), it cold-launches and logs the changed key names.

**`AppUITests/AuthUIFlow.swift`**
- `launchSignedIn(app, walkthroughSeen:, account: .demo|.twofactor, extraEnv:)` sets:
  - `CF_AUTH_TEST_ID` (UUID);
  - `CF_AUTH_TEST_WALKTHROUGH_SEEN` (an explicit argument is **required**; unset means not seen);
  - `CF_AUTH_TEST_SIGNED_IN`;
  - default args `-cf.reduceMotion -AppleLanguages (en) -AppleLocale en_US -UIPreferredContentSizeCategoryName UICTContentSizeCategoryL` (XXL with `xxLarge: true`).
- `launchScreen(_ screen:, state:)` seeds `CF_AUTH_TEST_AUDIT_SCREEN`/`_STATE` and requires the first field of that screen within 5 s.

**`AppUITests/UITestTiming.swift`**
- `launchMeasured(profile: .screen|.launch)` sets `CF_UI_TEST_PROFILE`. It encodes the caller's launch arguments into `CF_UI_TEST_LAUNCH_ARGUMENTS`, because XCTest augments argv.
- It prints `CF_UI_TIMELINE id=… side=runner event=… wall=… uptime=…`.

**Debug-only `AuthUITestConfiguration`** (`Sources/DataAccess/AuthUITestConfiguration.swift`)
- Seeds are honoured only with a valid UUID in `CF_AUTH_TEST_ID`.
- `screen` profile = fast launch. `CF_MOCK_DELAY` accepts 0–60 s and defaults to 0.
- Each test gets an isolated Application Support directory `AuthUITests/<id>`.

**`screen` profile (`ui-test-tiers.md:121`)**
1. Commit the overlay before loading services.
2. Restore the seeded account through the **real** path: mock sign-in → optional mock 2FA → `SessionController.accept` → `FullPull`. This uses a real per-account SQLite and an isolated test Keychain.
3. Remove the overlay with **no splash/readiness deadlines**.

Animations are disabled (UIKit off, root transaction disabled, window speed 20). The `launch` profile keeps the real overlay and timings and is used by the LaunchScene, LaunchToWelcome, Readiness, Welcome and AuthSession suites.

**`UITestSeedCommand`** (`Sources/DataAccess/UITestSeedCommand.swift`, `#if DEBUG`)
- Codable `{authorization, nonce, environment, arguments}`.
- Transported as `chronoflow-uitest://reseed?payload=<base64 JSON>`, under 32 KiB.
- Validation rejects:
  - a bad endpoint or payload;
  - missing or foreign authorization (UUID compared by value);
  - a bad nonce or test ID;
  - a profile other than `screen`;
  - any non-`CF_` env key.

**`AppUITests/UIReseed.swift`**
1. Sends the URL through `XCUIDevice.shared.system.open` (I3).
2. Waits for window `uitest.ready.<nonce>` or `uitest.failed.<nonce>`.
3. Requires the ready window's label (`uitest.process.<pid>`) to be unchanged.
4. On failure, attaches a screenshot and `debugDescription`, then fails.

**App-side reset (`ui-test-tiers.md:127`)** runs in this order:
1. Serialize requests.
2. Unmount the hosting tree (navigation and sheets included) and end editing.
3. Stop observations and sync, and invalidate the old session's mutation permits.
4. Close the DB, then erase the fixture directory and the test Keychain item.
5. Remove the settings domain.
6. Rebuild the navigator, mock store, services, session and root through the **same cold-launch preparation**.

A host test (`MockSessionSeedTests.reseedRemovesPreviousAccountQueueAndCompletionMarkerAndMatchesColdSeed`) proves that a reseed equals a cold seed.

**Release safety**
- `ReleaseReadinessTests` (`#if !DEBUG`, its own hosted target) asserts:
  - no `CFBundleURLTypes`;
  - valid `CF_*` keys create **no** mock accounts;
  - Release `LaunchArguments` keeps only `-cf.mode ready` and `-cf.mainThreadStall`.
- Release ABI inventories contain no seed, transport or timeline symbols (`ui-test-tiers.md:249`).

### 3.2 The seed vocabulary

Fact: `grep '"CF_…"'` and `'"-cf.…"'` over `App`, `Packages`, `AppUITests`.

Environment keys (`CF_*`):

| Group | Keys |
|---|---|
| Gate / identity | `CF_AUTH_TEST_ID`, `CF_UI_TEST_PROFILE` (`screen`/`launch`), `CF_UI_TEST_LAUNCH_ARGUMENTS`, `CF_MOCK_DELAY` |
| Auth state | `CF_AUTH_TEST_SIGNED_IN` (`demo`/`twofactor`), `CF_AUTH_TEST_WALKTHROUGH_SEEN`, `CF_AUTH_TEST_UNVERIFIED`, `CF_AUTH_TEST_OFFLINE`, `CF_AUTH_TEST_TERMS_PENDING`, `CF_AUTH_TEST_TERMS_CHANGES_PENDING`, `CF_AUTH_TEST_AUDIT_SCREEN` (`sign-in`, `sign-up`, `forgot`, `reset`, `two-factor`), `CF_AUTH_TEST_AUDIT_STATE`, `CF_AUTH_TEST_VERIFY_STATE` (`throttle`, `server`, `offline`, `failed*`), `CF_AUTH_TEST_LINK`, `CF_AUTH_TEST_RESET_FAILURE`, `CF_AUTH_TEST_SIGNUP_FAILURE` |
| Settings | `CF_SETTINGS_ROUTE` (`account`, `profile`, `language`, `bodyClock`, `preferences`, `security`, `password`, `twoFactor`, `twoFactorSetup`, `plan`, `delete`), `CF_SETTINGS_TEST_PENDING`, `CF_SETTINGS_BOARD_STATE` |
| Plan | `CF_PLAN_TEST_ROUTE` (`trips`, `trip:<id>[:list]`), `CF_PLAN_TEST_SHEET` (`step:<blockID>`), `CF_PLAN_TEST_BLOCK`, `CF_PLAN_TEST_POSITION` (`summary`), `CF_PLAN_TEST_FREEZE_CLOCK` |
| Wizard | `CF_WIZARD_BOARD_CAPTURE` (a board name, e.g. `WizardSaveJourney`), `CF_WIZARD_AUTO_OPEN`, `CF_WIZARD_TEST_REVIEW`, `CF_GUEST_WIZARD`, `CF_GUEST_WIZARD_JOURNEY`, `CF_GUEST_RECOVERY_BOARD` |
| Walkthrough / refresh | `CF_WALKTHROUGH_TEST_PENDING`, `CF_REFRESH_TEST_EMPTY_TRIPS`, `CF_REFRESH_TEST_COUNTER` |

Launch arguments (`-cf.*`):
- **Launch, Debug:** `-cf.mode`, `-cf.freezeAt`, `-cf.readyAt`, `-cf.readyDelay`, `-cf.mainThreadStall`, `-cf.session`, `-cf.gate`, `-cf.stored yes|no|never`, `-cf.reduceMotion`.
- **Settings:** `-cf.settings.language`, `-cf.settings.theme`.
- **Timeline:** `-cf.trip-page S1…S6|large30|large60|<uuid>`, `-cf.timeline-fixtures`.
- **Fault and system injection:** `-cf.isolatedMockServer`, `-cf.forgotServerFailure`, `-cf.forgotThrottleFailure`, `-cf.twoFactorServerFailure`, `-cf.firstTripReminders`, `-cf.notificationAuthorization`.

### 3.3 The mock server

Fact: `Sources/DataAccess/Mock/*`, about 2.6k lines.

**Accounts** (`MockScenarioValues`):

| Account | ID | Email | Password | Other |
|---|---|---|---|---|
| demo | `0199a3c0-0000-7000-8000-000000000000` | `demo@example.com` | `password` | 2 passkeys |
| twofactor | `0199a3c0-0000-7000-8000-000000000001` | `twofactor@example.com` | `password` | 2FA on; code `123456`; recovery `AbCdEf1234-GhIjKl5678` |

**`MockAppSeed.make(now:unverified:walkthroughSeen:termsPending:includeTimelinePerformanceTrips:)`**
- Decodes trip fixtures (from the RN seed) and **rebases them to `now`**.
- Builds both accounts.
- With the flag set, appends `large30`/`large60` (`MockTimelineFixtures`: S3's first six days repeated 5× or 10×, plus S1's dense caffeine; 60 days = **54,000 pt** of grid at 40 pt/h).

**Other mock pieces:** rate limiter, inbox (mail for verify/reset), guest review, trip generation.

**Data-source selection (D20)**
- `CHRONOFLOW_DATA_SOURCE = mock|api`: mock in Debug, simulator and tests; never a silent fallback.
- Tests use an injected source or an ephemeral `URLSession` with a **deny-by-default** `URLProtocol`.
- `AppServices.mockDelay` is 0.3–0.8 s for ungated Debug launches and 0 in fixtures.
- Release builds can still construct the mock by explicit setting, but **never seed accounts** (`ReleaseReadinessTests.testReleaseDefaultMockDoesNotSeedAccounts`).

### 3.4 Snapshot policy

Facts: `AGENTS.md` Traps, `CLAUDE.md`, `ui-test-tiers.md:51-57`, `tooling.md` "Device fit".

- **Reference-comparing snapshots are opt-in until a person approves the references.** `make snapshots` compares; `SNAPSHOT_RECORD=1 make snapshots` records on 17e only. New references need review.
- "Never name references after the executing simulator." Profiles are injected (`SnapshotContainer`: explicit point size, insets, display scale, with `safeAreaRegions = []`).
- Approval state:
  - All per-simulator references were deleted. Replacement recording "remains open".
  - The user said "We don't need the screenshots now" (2026-11-08, `plan.md:1950`).
  - `TripPageSnapshotTests` "intentionally compares without recording even if `SNAPSHOT_RECORD` is set" (`perf-timeline.md`).
- **Reference-free guards run by default.** They render fixed stress fixtures (SE / nl / xxLarge, light + dark) and assert:
  - required controls are present;
  - opaque full-size pixels;
  - horizontal fit of measured elements;
  - visible "ink".

  They need no reference files (`ui-test-tiers.md:57`).
- **Board captures** (`WizardBoardCaptureTests`, 71 boards × en/nl × light/dark, one per test; `SettingsBoardCaptureTests`) are an opt-in `capture` policy, used to compare against the design side by side.
- Contrast is measured, not eyeballed. Examples:
  - `RenderedTextContrast`, AA ≥ 4.5:1;
  - token-pair contrast on the host (`AuthBoardContrastTests`, 1,001 samples of the sky ramp).

---

## 4. Android testing strategy (Rec.)

### 4.1 Layer map

| iOS layer | Android layer | Runs on | Owns |
|---|---|---|---|
| SwiftPM host tests (`make quick`) | **JVM unit tests** in pure Kotlin modules (`:core:*`: plan layout, readiness reducer, reminders, contracts, sync rules, outbox, l10n formatting) plus data-layer tests with the SQLite JVM driver (SQLDelight `JdbcSqliteDriver`, or Room with `androidx.sqlite` bundled driver) | JVM | All logic, persistence, timing (injected `Clock` / `TestScope`), computed values, multi-step flows, the timeline host budgets |
| Python runner/resource fixtures | Gradle `buildSrc`/convention-plugin tests plus a `ToolingTest` module (token freshness, string-catalog en/nl parity, contract-generator freshness, test-registry checks) | JVM | Generators, catalogs, "is every test class registered in a tier" |
| Hosted `AppTests` (layout, geometry, wiring) | **Robolectric + Compose UI test + Roborazzi** (`@GraphicsMode(NATIVE)`), with `DeviceConfigurationOverride` (`ForcedSize`, `FontScale`, `Locales`, `DarkMode`, `LayoutDirection`) and a fixture inset provider (§4.4) | JVM | Measured bounds, scroll reachability, tap-target size, min-height, recomposition/invalidation probes, reference-free render guards |
| Reference snapshots (opt-in) | Roborazzi `verifyRoborazzi*` / `recordRoborazzi*`, same Robolectric tests behind a flag | JVM | Pixel comparisons, only after approval |
| XCUITest (screen + one journey) | **Instrumented Compose tests** (`androidx.compose.ui.test.junit4` + `ActivityScenario`) on the AVD `pixel` (Pixel 8, API 36), plus **UiAutomator** only for system UI | Emulator | Native controls (pickers, IME, back/predictive back, permission dialogs), navigation, system appearance/locale, notification permission. One journey per feature. |
| `ReleaseReadinessTests` | `testReleaseUnitTest` in `:app` plus a merged-manifest check task | JVM | The release variant has no seed entry point, no test scheme or exported debug activity, mock never seeds, `BuildConfig.DEBUG == false` paths |
| `PerfUITests` (`make perf`, `perf-timeline`) | **Macrobenchmark** module (`:benchmark`) against a `benchmark` build type, plus a Baseline Profile generator | Emulator for regression; physical device for gates | Startup, first timeline frame, scroll jank, memory |
| On-device Instruments review | Android Studio profiler / Perfetto system trace on a physical phone | Device | Release-candidate review |

The rationale for Robolectric over Paparazzi for the hosted tier: Paparazzi (layoutlib) renders but cannot run Compose test interactions such as scroll, click and measured-semantics assertions. iOS hosted tests *mostly assert measured layout*, and only some capture pixels. Roborazzi runs inside Robolectric Compose tests, so one tier covers both, which mirrors iOS's split of "reference-free guards by default, references opt-in". Compose Preview Screenshot Testing (AGP) is the other candidate; see OQ-T2.

### 4.2 Rules for Android (a direct translation of `AGENTS.md` Tests)

Put these verbatim in the Android `AGENTS.md`:

1. **Cheapest layer that can fail.**
   - Logic → JVM unit tests.
   - Layout and geometry → Robolectric Compose tests.
   - Emulator → only for native controls, navigation, system UI, IME, permissions and system appearance/locale.
   - Never test logic through a screen.
2. **One emulator journey per feature, at most.** Every other emulator test seeds a state and checks one screen.
3. **Emulator tests launch seeded** (§5). Never sign in through the form or click through earlier steps. Only the auth tests type credentials.
4. **Exact queries and waits.**
   - Use `onNodeWithTag("sign-in.email")` (plus `useUnmergedTree = true` only when needed) or `onNode(hasTestTag(..) and hasClickAction())`.
   - No `onAllNodes(isRoot())`/`printToLog` traversals, no loops over `fetchSemanticsNodes()` to find something, and no text-prefix matching unless the content is genuinely variable.
   - Wait with `composeTestRule.waitUntil(timeoutMillis) {…}` / `waitUntilExactlyOneExists(…)`. For UiAutomator, use `device.wait(Until.hasObject(By.res(..)), t)`.
   - **Never `Thread.sleep` / `SystemClock.sleep`.**
5. **Budgets.**
   - An emulator test < 10 s; a Robolectric test < 5 s; a JVM unit test < 1 s (except the declared perf-budget tests).
   - Hang caps (§4.6): 60 s per test.
   - Split profile × language × size loops into one test per profile (JUnit `Parameterized` or `TestParameterInjector` is fine: each parameter is its own test case).
   - Needing more time is a design smell; fix the test.
6. **Know what the harness can show.**
   - Robolectric does not lay out real system bars, cutouts, IME or hinge events. Insets come only from the fixture provider (§4.4), and IME behaviour belongs on the emulator.
   - When a check cannot fail for the real reason, delete it and capture one seeded emulator screenshot instead.
7. **Running tests.**
   - Fixing a failure → run only that method (`make targeted ONLY=…#method`).
   - Each fix round runs only what it touched.
   - Run `make quick` once before pushing, not every round.
   - Read the suspect diff before bisecting.
   - **After one fix round that changes nothing, or a hang, stop and rethink.**
8. **Transient UI** (snackbars, the 2 s "Up to date" capsule): assert through a Debug-only "was presented" counter (iOS I13), not by racing the animation.
9. **Missing evidence is failure.** A tier that produced no result file, or a benchmark without its metric, fails.

### 4.3 Test identifiers: reuse the iOS ones

**Fact:** the iOS UI tests use stable dotted identifiers, for example `sign-in.email`, `sign-in.password`, `sign-up.name`, `forgot.email`, `reset.password`, `two-factor.code`, `welcome.signIn`, `auth.back`, `auth.banner`, `trip.plan.end`, `refresh.feedback`, `wizard.extras`, `launch.opening` (`AppUITests/AuthUIFlow.swift`, `perf-timeline.md`, `wizard-tests-t6.md`).

**Rec.:** use **exactly the same strings** as Compose `testTag`s. Set `Modifier.semantics { testTagsAsResourceId = true }` once at the root, so UiAutomator and Macrobenchmark can find them as resource IDs. This makes the iOS test inventory directly portable and keeps cross-platform bug reports aligned.

### 4.4 Device profiles: an Android `profiles.json`

iOS has one committed device table that drives pure geometry tests and hosted fixtures, with no simulator matrix. Mirror it in `core/testing/src/main/resources/profiles.json`:

| Alias | dp (w × h) | Density | Insets top/left/bottom/right (dp) | Purpose (iOS analogue) |
|---|---|---|---|---|
| `small` | 360 × 640 | 2.0 (xhdpi) | 24 / 0 / 48 / 0 (status + 3-button nav) | Worst-case height (SE) |
| `pixel8` | 411 × 914 | 2.625 (420 dpi) | record from AVD / 0 / record / 0 | Reference phone (17e) |
| `foldInner` | 841 × 701 | 2.625 | record / 0 / record / 0 | ≥ 600 dp wide layout (Duo open / wide) |
| `landscapeSideNav` | 914 × 411 | 2.625 | 24 / 0 / 0 / 48 (nav bar on the side) | Asymmetric side inset (Duo 19 pt rail) |
| `largeFont` | `pixel8` at system `fontScale = 2.0` | — | as `pixel8` | Root font-scale cap stress (xxLarge/AX5) |

Notes:
- **Fact needed:** the Pixel 8 AVD's real status-bar and gesture-bar insets are not recorded anywhere. Phase 0 task: read them once (`adb shell dumpsys window windows | grep -i inset`, or log `WindowInsets` in a debug build) and commit them.
- Production never reads this file. Production reads `WindowInsets.safeDrawing` once at the root into an app `LocalSafeInsets`, and Robolectric tests provide the fixture insets through that same local. This is the iOS `SnapshotContainer` → `WelcomeGeometry` pattern ("the internal Welcome capture initializer passes the fixture's four insets straight into the same `WelcomeGeometry` used by production"). It avoids the iOS I9 trap of insets simulated outside the view.
- **Font-scale cap:** iOS caps Dynamic Type at xxLarge at the root and tests that AX5 == xxLarge. Android: clamp `LocalDensity.fontScale` at the root (OQ-A6, default 1.3). The `largeFont` profile renders at 2.0 and asserts **identical bounds to the capped scale**, the direct analogue of `ContentSizeCapTests` / "AX5 versus xxLarge".
- Dutch is the stress language for plan copy, as on iOS. Run layout tests at `small`/nl/`largeFont` plus `foldInner` for wide screens. Do not duplicate them on `pixel8`.
- Pure geometry (splash `LaunchLayout`, Welcome geometry, timeline `layoutDay` heights such as 60 days = 54,000 dp at 40 dp/h) is tested on the JVM over **all** profiles. This mirrors the iOS D1c device-table tests.
- Foldables: unit-test the posture logic with fake `WindowLayoutInfo` / `FoldingFeature` (`androidx.window:window-testing`, `WindowLayoutInfoPublisherRule`). iOS found real bugs only on Duo resize (`01-ios-plan-reuse.md` §8 item 15).

### 4.5 The timeline is the critical screen: four guarantee layers on Android

This mirrors `Docs/perf-timeline.md` "Four guarantee layers".

**1. JVM host budgets (in `make quick`).**
- `TimelinePreparationBudgetTest` decodes the detail JSON, then runs `layoutPlan` → `indexTimeline` → `preparePlan` for S3 / large30 / large60.
- Run 1 unmeasured warm-up (JIT), then assert **each** of 10 samples against **40 / 100 / 200 ms** with `System.nanoTime()`. Use the same limits for every build type.
- Fixture construction and I/O stay outside the sample.
- Also test the fixtures: large60 grid height == 54,000 dp; the six-day cycle is 960/960/540/1,020/960/960 dp; IDs are unique; there are 740 list rows (720 blocks + 20 zone cards).

**2. Recomposition guard (Robolectric, in `make quick` for `:feature:plan`).**
- Add a production `staticCompositionLocalOf<TimelineProbe?> { null }`, read in each day's static drawing composable and the clock child (a no-op when null).
- The test installs a counting probe, composes all 60 days (Timeline and List), and asserts:
  - 120 scroll-offset changes plus one anchor change → **0** day recompositions and **0** clock recompositions;
  - one minute tick → 0 days and **1** clock child;
  - a UTC day boundary → 0 days and **2** clock children;
  - positive control: the anchor change *is* observed.
- This proves that Compose's skipping and stability hold: `@Immutable` day models and no per-frame state reads in composition.

**3. Drawing / cache bounds (JVM).**
- If day bodies are drawn from a precomputed display list (a list of draw ops, or `Picture` recorded off-main), cap the payload per day (≤ 512 KiB) and the cache at ≤ 4 MiB / 4 entries.
- Traverse all 60 days at the `small` and `foldInner` widths.

**4. Macrobenchmark (`make perf-timeline`)** against the `benchmark` build type:
- `timelineFirstFrame`:
  - `startActivityAndWait(intent with extra cf.trip-page=large60)`;
  - `TraceSectionMetric("cf.timeline.firstFrame", Mode.First)` around observation → decode → layout → first drawn frame (end it in a `Choreographer` post-frame callback after 2 frames, like iOS's "two display-link callbacks");
  - plus `StartupTimingMetric()`;
  - 3 iterations; **≤ 0.8 s hard, ≤ 0.45 s aim**.
- `timelineScroll`:
  - setup (outside measurement): launch, wait for the `trip.plan` resource ID;
  - measured: **exactly 15 flings** (`UiObject2.fling(Direction.DOWN)` with a gesture margin);
  - `FrameTimingMetric()` + `MemoryUsageMetric(Mode.Max)`;
  - 3 iterations, `CompilationMode.Partial(BaselineProfileMode.Require)`;
  - after measuring, assert that the visible day label advanced and log the first/last days.
- `timelineLastDay`: open the day picker, select index 59, and reach `trip.plan.end` in ≤ 6 flings. This is not measured.
- **Never** a traversal by hundreds of swipes (I2).

**Gates:**

| Metric | Android source | Hard | Aim |
|---|---|---|---|
| First timeline frame | `TraceSectionMetric` `cf.timeline.firstFrame` | ≤ 800 ms | ≤ 450 ms |
| Scroll jank | `FrameTimingMetric.frameOverrunMs` P99 (API 31+) and the jank ratio from the trace (sum of positive overrun / scroll duration) | ≤ 5 ms/s; P99 overrun ≤ 0 | ≤ 1 ms/s |
| Peak memory | `MemoryUsageMetric` `memoryRssAnonMaxKb` (+ `memoryHeapSizeMaxKb` reported) | ≤ 150 MiB | ≤ 80 MiB |
| Startup to Today | `StartupTimingMetric` `timeToInitialDisplayMs` (cold), `timeToFullDisplayMs` with `reportFullyDrawn()` after the Today content is drawn | ≤ 400 ms TTID on the reference device; below RN Android | — |

**Results checker.** Macrobenchmark does not assert budgets. Add `Scripts/check-benchmarks.py`, run by `make perf*`. It:
- reads `*-benchmarkData.json` from `build/outputs/connected_android_test_additional_output/`;
- fails when any budgeted metric is **missing**, non-finite or over budget;
- prints every raw value as `Timeline metric: …`.

Its own JVM/Python fixtures include an injected 0.81 s sample that must fail (iOS's expected-failure test).

**Emulator vs device.** iOS enforces the hitch gate "when the ratio is reported (a device run) and skipped on the simulator". Android equivalent:
- On the emulator, run with `androidx.benchmark.suppressErrors=EMULATOR`. Enforce the first-frame and memory gates; **report** jank but do not gate it.
- On a physical reference device (OQ-A11), enforce everything.
- Record results in `docs/perf-timeline.md` with device, OS, refresh rate, font scale and theme.

Required when: every change touching `:core:plan`, `:feature:plan` (layout, drawing, scrolling), the fixtures, the benchmark harness, the timeline tokens/fonts or the budgets. This is the iOS rule.

**Baseline Profiles:** the `:benchmark` module also hosts `BaselineProfileGenerator` for cold start → Today and open trip → scroll timeline (`09-rn-android-lessons.md` §2). Commit the generated profile, and regenerate it when those flows change.

**Debug-time jank:** enable `JankStats` in debug builds only, logging to logcat. Never ship it.

### 4.6 Hang caps and timeouts (Rec.)

| Layer | Mechanism |
|---|---|
| Coroutine tests | `runTest(timeout = 60.seconds)` (kotlinx-coroutines-test's default overall timeout is 60 s); inject `TestScope`/`StandardTestDispatcher` and a fake `Clock` for 1500/2000/8000 ms readiness rules, 1 s debounce, `min(1 s × 2ⁿ, 5 min)` retry |
| JVM/Robolectric | Gradle `Test` task: `timeout.set(Duration.ofMinutes(10))` per task as a backstop. A JUnit4 `Timeout` rule runs the test on another thread, which breaks Robolectric's main looper, so **don't** use it in Robolectric tests. Rely on `runTest` timeouts and the per-task cap, and print per-test durations with a Gradle test listener that fails any non-perf test > 5 s (hosted budget) as a report, not a hard fail, at first. |
| Instrumented | AndroidJUnitRunner argument `timeout_msec=60000` (per-test; verify on AGP/test-runner versions, OQ-T6) and `composeTestRule.waitUntil` bounds ≤ 5 s by default |
| Make/Gradle runner | Mirror I17: an idle timeout (no log growth for 600 s) and an overall cap (3,600 s) in the `Scripts/run-tier.sh` wrapper, then TERM → 5 s → KILL to the process group (`./gradlew --stop` after) |

### 4.7 What stays on the emulator (the smoke set)

One journey per feature, plus seeded single-screen checks that need the real system.

| Feature | Journey (max one) | Seeded single-screen checks that need the emulator |
|---|---|---|
| Launch / Welcome | cold launch (`launch` profile) → Welcome → Sign in | System splash → Compose overlay hand-off (SplashScreen API); dark/light **system** appearance (serial) |
| Sign in / 2FA | typed sign-in demo → Today; twofactor code → Today | IME visible with the password field and submit fully visible (`imePadding`); autofill hints; IME action chain (email → password → go) |
| Sign up / Verify / Forgot / Reset | one each where flows differ: forgot → mock mail → reset | Error banner visible above the IME |
| Terms / Walkthrough | accept → walkthrough → Today | — |
| Today / My trips | Today → trip page → back (system back + predictive back) | Pull-to-refresh with the presented counter |
| Trip page / timeline | open step sheet → Done → Undo | Bottom sheet (`ModalBottomSheet`) states; day-picker menu |
| Wizard | one-way review → save → trip page (seed `CF_WIZARD_BOARD_CAPTURE=WizardSaveJourney`) | Native date/time picker (`DatePickerDialog`/`TimePicker`) keeps the app language and the 12/24 h setting |
| Settings | sign out with a pending change → Stay → Discard | Per-app language (`AppCompatDelegate.setApplicationLocales`) changes copy; delete-account password prompt |
| Reminders | — | `POST_NOTIFICATIONS` system dialog (UiAutomator), exact-alarm settings deep link |
| Accessibility | — | Run Compose's accessibility checks (`enableAccessibilityChecks()` in ui-test 1.8+, or ATF via Espresso `AccessibilityChecks.enable()`) on the five plan screens and the auth screens, en only by default; full variants opt-in (iOS I16) |

### 4.8 Release-readiness (Rec.)

Mirror `ReleaseReadinessTests`:
- Put all seed code (`TestSeeds`, `SeedReceiverActivity`, mock delay overrides, `TimelineProbe` implementations, timing logs) in the **`src/debug/`** source set, or a `:testing:seeds` module that is only `debugImplementation`. Release then cannot reference it, and R8 never sees it.
- `testReleaseUnitTest` (`:app`) asserts:
  - `Class.forName("…SeedReceiverActivity")` throws;
  - the mock data source, when explicitly constructed, seeds no accounts from `CF_*` extras;
  - only the allowed release diagnostics remain (if any; see OQ-A13 about the stall flag).
- A Gradle task `verifyReleaseManifest` parses `build/intermediates/merged_manifests/release/…/AndroidManifest.xml`. It fails on:
  - any `cf`/`chronoflow-uitest` scheme;
  - any exported debug activity;
  - `android:debuggable`;
  - `android:testOnly`.

  `make check` runs it.
- The `benchmark` build type (`initWith(release)`, debug-signed, `isDebuggable = false`, `matchingFallbacks += "release"`) has its own `src/benchmark/` that adds **only** the timeline fixture entry (`cf.trip-page`, `cf.timeline-fixtures`), like the iOS Perf configuration's `CHRONOFLOW_TIMELINE_PERF`.

---

## 5. Seeded launch on Android (Rec.)

### 5.1 Why Android is simpler

**Fact:** XCUITest drives a separate process, so iOS needed:
- a URL scheme transport;
- UUID authorization;
- a nonce acknowledgement window;
- a PID check;
- a launch-settings comparison;
- careful teardown.

Instrumented Compose tests run **inside the app process** with `ActivityScenario`, so the test can call the seeding API directly.

### 5.2 Design

1. **`TestSeed`** (debug source set): a data class parsed from a `Map<String, String>`. It uses the **same key names** as iOS (`CF_AUTH_TEST_SIGNED_IN`, `CF_PLAN_TEST_ROUTE`, …), so the iOS tests and docs map 1:1. Launch-argument equivalents become string extras named after the iOS argument without the dash (`cf.trip-page`, `cf.settings.language`, `cf.reduceMotion`, `cf.freezeAt`, …).
2. **`TestSeeds.apply(seed)`**, `suspend`, off-main, mirroring the iOS app-side reset (§3.1):
   1. Cancel the session scope and sync.
   2. Close the DB.
   3. Delete the per-test directory `filesDir/uitests/<CF_AUTH_TEST_ID>/`.
   4. Clear the test credential store and the test DataStore.
   5. Rebuild the app container (DI graph) with the mock data source (shared in-memory `MockServerState`, `mockDelay = CF_MOCK_DELAY ?: 0`).
   6. Run the **real** restore path: mock sign-in → optional 2FA → `SessionController.accept` → full pull.
   7. Write the stored gate.

   **Architecture requirement this imposes:** the app container must be resettable in debug. No process-global singletons with state, and every `CoroutineScope` owned and cancellable. That is good hygiene anyway (D26: "no eager singleton … I/O").
3. **`ScreenTest` base class (instrumented):**
   - `createEmptyComposeRule()`, so no Activity launches before seeding (`createAndroidComposeRule<MainActivity>()` would launch first).
   - `@Before`: `runBlocking { TestSeeds.apply(seedFor(test)) }` then `ActivityScenario.launch<MainActivity>(intent.putExtra("CF_UI_TEST_PROFILE", "screen"))`.
   - `@After`: `scenario.close()`.
   - The process persists across tests, so the per-test cost is an Activity launch plus a seed. **Target ≤ 1 s from seed begin to first screen node** (iOS target). Log `CF_UI_TIMELINE` lines with `SystemClock.uptimeMillis()` so the same `summarize-ui-timing` idea works.
4. **`screen` profile:** `MainActivity` reads `CF_UI_TEST_PROFILE=screen` (debug only). It:
   - skips the splash overlay and readiness deadlines;
   - disables in-app animations (a `LocalReduceMotion`-style flag and `MotionDurationScale` = 0 via a debug `CompositionLocal`);
   - routes straight to the stored gate.

   The `launch` profile keeps everything real (splash/readiness/Welcome suites).
5. **System-level settings force a cold path**, which iOS also does: system dark mode (`UiModeManager.setNightMode` / `adb shell cmd uimode night yes`), system font scale, and system locale. Group these tests in their own class and run them serially. In-app language and theme (`cf.settings.language/theme`) are app settings and are seeded like any other key.
6. **Manual and adb seeding.** In the debug manifest only, an exported `SeedReceiverActivity` accepts the same extras, applies them and forwards to `MainActivity`. It requires `CF_AUTH_TEST_ID` to be a UUID, as iOS does. Use it for screenshots, design-fidelity captures and Macrobenchmark-free manual checks:
   ```sh
   adb shell am start -n net.chronoflow.app.native.debug/net.chronoflow.debug.SeedReceiverActivity \
     --es CF_AUTH_TEST_ID $(uuidgen) --es CF_UI_TEST_PROFILE screen \
     --es CF_AUTH_TEST_SIGNED_IN demo --es CF_AUTH_TEST_WALKTHROUGH_SEEN 1 \
     --es CF_PLAN_TEST_ROUTE trip:<id>
   adb exec-out screencap -p > build/evidence/trip.png
   ```
   This is the analogue of iOS "`simctl launch` with the seed env, then `simctl io … screenshot`" for checks the harness cannot make.
7. **Host tests for the seed itself** (JVM):
   - parse/validate every key;
   - invalid UUID → seed ignored;
   - "reseed equals cold seed": a dirty other-account seed, then a reseed, gives the same account, gates, trips, outbox and sync marker (`MockSessionSeedTests.reseedRemoves…MatchesColdSeed` analogue).

### 5.3 Mock server and HTTP tests

- **Port the mock data source** (`MockDataSource`, `MockServerState`, `MockAppSeed`, `MockInbox`, `MockRateLimiter`, trip fixtures rebased to `now`, `large30`/`large60`) into a `:core:data-mock` module. Keep the same account IDs, emails, password, 2FA code and recovery code (§3.3) so fixtures, docs and screenshots match iOS. The trip fixture JSON is the same RN seed file; copy it byte-for-byte and add a checksum test.
- **Shared in-memory state:**
  - every data-source instance reads and writes through one `MockServerState` holder;
  - mutations are atomic against the latest state (I4);
  - mail and credentials are shared across instances.
- **Delay:** ungated debug runs use 300–800 ms; seeded runs use exactly `CF_MOCK_DELAY` (0–60 s), default 0 (I5).
- **API contract tests:** OkHttp **MockWebServer** (`mockwebserver3`) with a **deny-by-default** dispatcher that fails on any unexpected request, so no test touches the network (iOS D20). Golden JSON request/response fixtures come from `03-api-contract.md`.
- **Release never seeds** (§4.8).

---

## 6. Snapshot policy for Android (Rec.)

1. **Default tier = reference-free render guards** (Robolectric + Roborazzi capture *without* comparison). For each screen state, on `small` / nl / `largeFont` (light and dark), plus `foldInner` for wide layouts, assert:
   - required nodes exist and have non-empty bounds;
   - every required node's bounds lie inside the window minus the fixture insets, horizontally always and vertically after `performScrollTo()`;
   - touch targets ≥ 48 × 48 dp (the Material/Android minimum; iOS used 44 pt);
   - the captured bitmap is full-size and not blank: some pixels differ from the background colour in the node's bounds (the "ink" check).
2. **Reference comparisons are opt-in** (`make snapshots`). They are recorded with `SNAPSHOT_RECORD=1 make snapshots` → `./gradlew recordRoborazziDebug` and reviewed by a person before they count. File names come from `<Screen>_<state>_<profile>_<lang>_<theme>`, **never** from the executing device.
3. **References are JVM renders.** Robolectric native-graphics text and shadow rendering differs from a device. Never compare a Roborazzi reference with an emulator screenshot.
4. **Design-fidelity captures** (the analogue of `WizardBoardCaptureTests`): an opt-in tier renders each seeded board state per en/nl × light/dark into `build/captures/`, named after the canvas board (`WizardOutbound`, `SetBodyClock`, …), for side-by-side review against `_design/canvas`. It never runs by default.
5. **Contrast is computed, not eyeballed.** JVM tests check every designed token pair (light and dark) for ≥ 4.5:1 (text) and ≥ 3:1 (large text/UI), including samples along the cf-sky ramp (`AuthBoardContrastTests` analogue).
6. **Lean set.** No per-device reference sets, no matrix, and no recording in the default flow.

---

## 7. Tooling: Makefile and Gradle (Rec.)

### 7.1 Gradle settings for speed

In `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
kotlin.incremental=true
android.nonTransitiveRClass=true
```

Other settings:
- Unit tests: `testOptions.unitTests.isIncludeAndroidResources = true` (Robolectric resources/fonts), `isReturnDefaultValues = false`, and `maxParallelForks = (cores / 2)` for JVM test tasks.
- Instrumented tests: `testOptions.animationsDisabled = true`. **No Android Test Orchestrator**: it restarts the process per test, which is exactly the per-test cold launch the iOS reseed work removed. Use in-process `TestSeeds` isolation instead (OQ-T3).
- Gradle wrapper only (no Gradle on `PATH`). **Fact:** the RN project's wrapper uses Gradle 9.4.1 (`chronoflow-mobile/android/gradle/wrapper/gradle-wrapper.properties`), so that distribution is likely cached. Pin AGP/Kotlin/Compose BOM/Robolectric/Roborazzi/Macrobenchmark to current stable releases at Phase 0 in `gradle/libs.versions.toml` (OQ-T1).
- JDK 17 (Zulu, installed). SDK `/opt/homebrew/share/android-commandlinetools`; compileSdk/targetSdk 36.

### 7.2 Module test layout

```
:core:model, :core:plan, :core:readiness, :core:reminders, :core:contracts, :core:sync   → JVM `test`
:core:data (SQLDelight/Room JVM driver), :core:data-mock                                → JVM `test`
:core:testing  (profiles.json, fixtures, fake Clock, MockServerState builders)           → test helpers
:feature:*     (Compose screens)                       → `testDebugUnitTest` (Robolectric/Roborazzi)
:app           (wiring, seeds in src/debug)            → `testDebugUnitTest`, `testReleaseUnitTest`,
                                                         `connectedDebugAndroidTest` (smoke)
:benchmark     (com.android.test, Macrobenchmark + BaselineProfileGenerator)
:tools:tokengen, :tools:copygen                        → JVM `test` + `--check` freshness tasks
```

Tag Robolectric tests by tier with JUnit4 categories or a `@LayoutTest`/`@SnapshotTest` annotation, filtered in Gradle:
- **logic:** default, in quick;
- **layout:** reference-free, in check;
- **snapshot:** reference compare, opt-in;
- **capture:** opt-in.

A **registry check** (JVM `ToolingTest`) fails when a test class has no tier annotation or when the default tiers include an opt-in class. This is the analogue of `test_launch_simulators.py`.

### 7.3 Makefile targets

Each target is a thin wrapper over `./gradlew`.

| Target | Runs | Target time |
|---|---|---|
| `make quick` | `./gradlew ktlintCheck detekt` + JVM `test` of all `:core:*`/`:tools:*` modules + Robolectric **logic**-tier tests (incl. the timeline invalidation guard) + `ToolingTest` (registry, en/nl catalog parity, generator freshness) | **< 1 min warm** (configuration cache hit, daemon warm) |
| `make check` | `quick` + Android Lint (`lintDebug`, warnings as errors) + `testReleaseUnitTest` + `verifyReleaseManifest` + Robolectric **layout** tier + emulator smoke set (`connectedDebugAndroidTest` filtered to the `@Smoke` annotation) on the running `pixel` AVD | **< 5 min** with a warm build and a booted emulator |
| `make targeted ONLY="<module>:<Class>[#method] …" [REUSE_BUILD=1]` | JVM/Robolectric → `./gradlew :mod:testDebugUnitTest --tests '…'`; instrumented → `-Pandroid.testInstrumentationRunnerArguments.class=…`; with `REUSE_BUILD=1` → `adb shell am instrument -w -e class … <testPkg>/androidx.test.runner.AndroidJUnitRunner` (no rebuild/reinstall) | seconds to < 2 min |
| `make snapshots` / `SNAPSHOT_RECORD=1 make snapshots` | `verifyRoborazziDebug` / `recordRoborazziDebug` on the **snapshot** tier | — |
| `make captures` | Design-fidelity capture tier → `build/captures/` | — |
| `make perf` | Macrobenchmark startup (`StartupBenchmark`: cold, 10 iterations, `CompilationMode.Partial`) + `check-benchmarks.py` | — |
| `make perf-timeline` | `TimelineBenchmark` (first frame, 15-fling scroll, last day) + `check-benchmarks.py` | required for any timeline change |
| `make baseline-profile` | `:app:generateBaselineProfile` | — |
| `make audits` | Full accessibility-check variants (en/nl × light/dark + large font) on the emulator, opt-in | < 15 min |
| `make emu` | `Scripts/emulator.sh ensure`: boot `pixel` headless if no device (`-no-window -no-audio -no-boot-anim`), wait for `sys.boot_completed`, then apply the device prep below | — |
| `make emu-clean` | Wipe AVD data / prune snapshots; **refuses** while `am instrument`/Gradle test runs (iOS I6) | — |
| `make tokens` / `make copy` / `make contracts` (+ `*-check`) | Generators and their freshness checks | — |
| `make format` / `make lint` | `ktlintFormat` / `ktlintCheck detekt lintDebug` | — |

Device prep run by `make emu` (each idempotent, per device, never host-global):
- `settings put global window_animation_scale 0`, `transition_animation_scale 0`, `animator_duration_scale 0`;
- `settings put secure show_ime_with_hard_keyboard 1` (the analogue of iOS I7: the emulator's hardware keyboard otherwise hides the soft IME);
- disable the autofill service for the test user (the analogue of the iOS strong-password sheet);
- `cmd uimode night no`.

Runner hygiene (mirrors the iOS runner):
- Logs and results go to `build/test-results/<tier>/`, with a `timings.json` per run. Preserve them before the next targeted run.
- Missing result XML/JSON fails the tier (I11).
- Per-tier idle/overall timeouts (§4.6).
- Diagnostics are off by default: no `adb bugreport` on failure unless `TEST_DIAGNOSTICS=1` (I12).
- `Scripts/ci.sh` = `make check`. Nothing runs on PRs until a remote exists.

### 7.4 Housekeeping

These follow incident I8:
- At the end of each branch, remove its worktree and its `build/`/`.gradle/` directories.
- Run `./gradlew --stop`.
- Report `du -sh ~/.gradle/caches ~/.android/avd build`.
- Never wipe the `pixel` AVD while tests run (`make emu-clean` refuses).
- Keep the AVD on a Quick Boot snapshot, so boot is seconds, not a minute.

### 7.5 Orchestration lesson (I15)

Before Phase 0 ends, prove that the implementing agent can run `./gradlew quick` (JVM + Robolectric) **inside its sandbox**. If it cannot run the emulator, the orchestrator runs `make check`/`perf` outside and feeds back exact output, as on iOS. Do not accept "no emulator was run" as a closing state for UI work.

---

## 8. Phase-0 checklist (Rec.)

1. `AGENTS.md`/`CLAUDE.md` for Android: copy §4.2 rules, the tier table, Traps (AVD wipe, IME, autofill, configuration cache, Robolectric + `Timeout` rule) and housekeeping.
2. Gradle settings (§7.1), version catalog, module skeleton (§7.2), Makefile (§7.3), `Scripts/emulator.sh`, `Scripts/check-benchmarks.py` (with its own failing-sample fixture).
3. `core/testing/profiles.json` with measured Pixel 8 insets; `LocalSafeInsets`; the root font-scale cap; `largeFont == cap` test.
4. Debug `TestSeeds` + `ScreenTest` base + `SeedReceiverActivity`; "reseed == cold seed" JVM test; `testReleaseUnitTest` + `verifyReleaseManifest`.
5. Mock data source port with identical accounts and fixtures; MockWebServer deny-by-default harness.
6. `:benchmark` with `StartupBenchmark` (empty app is fine) so the perf tier exists from day one. Then add the timeline benchmarks when the trip page lands.
7. Measure and record a warm `make quick` and `make check` on this Mac. If over budget, fix before Phase 1. iOS never met its 5 min target (§1.2).

---

## 9. Facts vs recommendations at a glance

| Item | iOS fact | Android rec. |
|---|---|---|
| Logic tier | SwiftPM host tests, ~605 Swift Testing + 19 XCTest methods (`ui-test-tiers.md:319`) | JVM unit tests in pure Kotlin modules |
| Layout tier | Hosted AppTests on one 17e with injected profiles | Robolectric Compose tests with `DeviceConfigurationOverride` and fixture insets |
| UI tier | XCUITest, ~297 test methods in `AppUITests`; smoke set `RUNTIME_SMOKE` (`launch-simulators.py:367`) | Instrumented Compose on the `pixel` AVD, one journey per feature |
| Seed | Env + args, URL reseed, UUID gate, PID ack | Intent extras with the same names; in-process `TestSeeds.apply`; debug-only source set |
| Waits | `UIWait` 50 ms × 1.5, cap 1 s | `waitUntil` / `Until.hasObject`; no sleeps |
| Hang cap | 60 s allowance | `runTest` 60 s, `timeout_msec`, per-task cap |
| Snapshots | Opt-in references; reference-free guards default | Same, via Roborazzi |
| Perf | XCTest metrics + adapter that fails on missing values | Macrobenchmark + `check-benchmarks.py` that fails on missing values |
| Matrix | Never by default | Never by default (no Gradle Managed Device matrix) |

---

## 10. Open questions (each with the default the plan should proceed on)

| # | Question | Recommended default |
|---|---|---|
| OQ-T1 | Exact versions (AGP, Kotlin, Compose BOM, Robolectric, Roborazzi, Macrobenchmark, UiAutomator)? | Latest **stable** at Phase 0, pinned in `libs.versions.toml`, at least two weeks old. Robolectric must support SDK 36; if it doesn't, run Robolectric at `@Config(sdk = [35])` while compiling against 36. |
| OQ-T2 | Roborazzi vs Paparazzi vs AGP Compose Preview Screenshot Testing? | **Roborazzi.** It shares the Robolectric Compose test (interactions plus measured-semantics assertions plus capture), matching the iOS hosted tier. Revisit if Robolectric native graphics proves flaky. |
| OQ-T3 | Android Test Orchestrator for isolation? | **No.** Per-test process restarts reintroduce the cold-launch cost iOS removed. Isolate through `TestSeeds.apply` (DB, DataStore, credentials, DI graph reset) plus a "reseed == cold seed" test. |
| OQ-T4 | Emulator for `make check`: the existing AVD `pixel` or a Gradle Managed Device? | **The existing `pixel` AVD** (Pixel 8, API 36 google_apis arm64), booted once and kept running with Quick Boot. A GMD boots a fresh emulator per run, which breaks the < 5 min budget. A GMD is acceptable for a future CI. |
| OQ-T5 | Memory budget: growth ≤ 40 MiB (`plan.md:1733`) or peak ≤ 150 MiB (`perf-timeline.md`)? | **Peak `memoryRssAnonMaxKb` ≤ 150 MiB hard, ≤ 80 MiB aim** (the newer iOS rule). Report heap and growth without gating. Android RSS ≠ iOS physical footprint; calibrate on the reference device before tightening. |
| OQ-T6 | Is `timeout_msec` honoured by the current AndroidJUnitRunner? | Assume yes and verify in Phase 0 with a deliberately hanging test. If it is not honoured, use a custom `RunListener` watchdog that fails after 60 s. |
| OQ-T7 | Jank gate definition, given Macrobenchmark reports only percentiles? | Gate `frameOverrunMs` P99 ≤ 0 ms on a physical device, and compute ms/s from the Perfetto trace (`androidx.benchmark` TraceProcessor) for the ≤ 5 ms/s gate. On the emulator, report only. |
| OQ-T8 | Run the recomposition guard in `quick` (Robolectric start-up cost)? | **Yes for `:feature:plan` only.** If it pushes warm quick over 1 min, move it to `check` and require `make targeted` on plan changes. |
| OQ-T9 | Reference device for the hard perf gates? | Same as OQ-A11: a mid-range Pixel (8a). Until one is attached, emulator results are regression signals only, never device claims (the iOS rule). |
| OQ-T10 | Accessibility audits: Compose `enableAccessibilityChecks()` or ATF via Espresso? | Compose's built-in checks (ui-test ≥ 1.8) on the emulator smoke screens, en/light/default by default; the full matrix through `make audits`. |
| OQ-T11 | Should seeded keys use iOS's `CF_*` names on Android too? | **Yes, identical names.** The iOS tests, docs and bug reports then map 1:1. Launch arguments become extras named `cf.<name>` without the dash. |

---

## 11. Risks

- **Robolectric fidelity.** Native graphics text metrics differ slightly from a device, so layout assertions must use tolerances (±1 dp) and never pixel equality against device output. Mitigation: reference-free guards plus one seeded emulator screenshot for anything the harness can't show.
- **In-process reseed leaks.** A singleton that survives `TestSeeds.apply` will bleed state between tests. Mitigation: the "reseed == cold seed" host test, and a debug assertion that the old container's scopes are cancelled.
- **Emulator perf noise.** Apple-silicon emulator frame timing is not device timing. Mitigation: gate only first-frame and memory on the emulator, and everything on a device (§4.5).
- **The check budget creeps,** as on iOS (1,104 s cold). Mitigation: measure every tier from Phase 0, keep `timings.json`, and fail review on a new emulator test that a cheaper layer could cover.
