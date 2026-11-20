# Phase 8 validation — 2026-11-20

Phase 8 implements the terms gate and the three walkthrough pages, including replay. All three plan boxes are checked. The gate advances after the local outbox transaction; neither producer requests or awaits network sync. Terms sign-out exposes the Phase 10a confirmation slot. There is no new Android-only copy.

The single pixel emulator journey passed in **2.352 s** (Gradle command 10 s). The once-only `make quick` invocation completed all Gradle assertions and static checks, then its timing gate rejected one untouched Konsist case: `HapticsRulesTest#noHapticsInLaunchRejectsPlantedViolation`, **1,118 ms > 1,000 ms**. It was not retried and no budget was changed. No watchdog hang occurred. Quick was not repeated after the final KTX dependency and motion-policy wiring fixes; their focused tests, final Lint and release build are recorded below. The existing `TimelineInvalidationTest` guards passed in quick.

## Commands and evidence

Times are the runner's wall time where available, otherwise Gradle's rounded elapsed time. Python times are unittest's suite times. Raw local logs are retained under `build/phase8-evidence/logs/`; tier reports remain under `build/test-results/`.

| Command / selection | Result | Wall time |
|---|---|---|
| `make targeted ONLY="core:ui:PolicyLinksTest"` (Task 1) | 4 passed; first Robolectric case 2,544 ms, warm cases 19–23 ms | 27 s |
| `make targeted ONLY="core:designsystem:LucideGlyphsTest"` | Passed; 44 glyphs, FileText has five paths | 6 s |
| `make generate` | Passed; committed FileText output with its manifest change | — |
| Task 4 targeted: `WalkthroughModelTests WalkthroughSkipTests WalkthroughFlushTests WalkthroughGeometryTests OnboardingSessionTests` in `feature:onboarding` | 36 passed; 886 ms total test time, maximum 376 ms | 3 s |
| Task 5 correction: `WalkthroughDotPolicyTest WalkthroughScreenTest#dotsAreHiddenFromAccessibility` in `feature:onboarding` | 3 passed; snap has zero duration; ordinary tween has an intermediate width at 16 ms | 7 s |
| Task 6 targeted: `core:navigation:HidesShellChromeTest app:WalkthroughReplayRoutingTest app:AppShellTest` | Passed; 27 route variants, existing shell checks retained | 2 s + 10 s |
| `app:WalkthroughReplayRoutingTest` with phone, foldInner, landscapeSideNav | 6 passed; fresh replay entry, hidden chrome, full bottom inset, zero writes | 6 s |
| Task 7 targeted: `TermsAcceptanceTests TermsUpdateModelTests TermsPresentationTests` in `feature:onboarding` | 23 of 25 passed initially; two revoked-session cases had an assertion beyond the pinned test. Focused correction passed both; all 25 passed again in quick | 5 s + 3 s focused |
| Task 8 targeted: `TermsViewModelTests TermsBrandAssetTest TermsCopyTests TermsScreenTest TermsScreenGuardsTest` in `feature:onboarding` | 23 passed; maximum warm case 315 ms; cold SDK 1,608 ms, first guards class 1,116 ms | 7 s |
| `make targeted ONLY="app:RootContentTest#recreationKeepsTheProcessRouteWithoutAnotherNavigation"` | Passed with GuestWizard seam; cold Robolectric 2,471 ms | 9 s |
| Task 10 targeted: `app:TestSeedSessionKeysTest app:OnboardingSeedFlagsTest app:TermsPendingSeedTest app:MockSessionSeedTests` | 67 passed; maximum 541 ms. Pending preference seed retains the same row ID on repeated application | 9 s |
| `make captures ONLY=Walkthrough,TermsUpdate,TermsFailed` | Passed; 24 PNGs, all six boards × en/nl × light/dark | 16 s |
| Task 11 focused: `app:WalkthroughReplayRoutingTest#systemBackExitsReplayWithoutWrites` | 3 profiles passed; Back completes the replay model before returning | 8 s |
| Task 11 footer correction: `feature:onboarding:WalkthroughScreenGuardsTest#fixedControlsAndScrollableContentFit feature:onboarding:OnboardingBoardCaptureTest` | 39 passed; regenerated captures; maximum warm case 105 ms | 9 s |
| Task 11 teardown: `feature:onboarding:WalkthroughViewModelTests feature:onboarding:WalkthroughReplayTest` | 6 passed after making cleanup non-cancellable | 2 s |
| `make targeted ONLY="app:OnboardingJourneyTest"` | One run, one pass on pixel/API 36/debug; terms → Next → Next → Done → Today | 10 s command; 2.352 s test |
| `make quick` (one invocation) | 2,441 measured cases; 3,841 report rows including reused output; all assertions pass, one untouched timing overrun above | 33.51 s |
| Remaining quick step: `Scripts/run-tier.sh build-logic --results 'build-logic/convention/build/test-results/test/*.xml' -- ./gradlew --console=plain :build-logic:convention:test` | Passed; 37 reported cases, existing test output reused | 2.37 s |
| Remaining quick step: `Scripts/test_scripts.sh` | Scripts fixtures: 168 passed (19.141 s); contracts: 24 passed and one environment-only parity failure. Make's `IOS_REPO`/`WEB_REPO` exports were missing from this direct invocation | 19.141 s + 0.019 s suites |
| `IOS_REPO=/Users/rickgout/Sites/chronoflow-ios WEB_REPO=/Users/rickgout/Sites/chronoflow-new python3 -I -m unittest discover -s tools/contracts-gen -t tools/contracts-gen -p 'test_*.py' -k test_matches_ios_generated_swift` | Focused parity rerun passed | 0.097 s |
| Remaining tooling: `python3 -I -m unittest discover -s tools/l10n -t tools/l10n -p 'test_*.py'` and the equivalent `tools/icons` command | 49 and 9 passed | 0.017 s + 0.054 s |
| `make timing` | Passed; seven existing metric cases reused, no new timing/device claim | 2.14 s |
| Layout command below | Passed: 359 measured cases, 685 report rows including reused output; 25 onboarding guards, maximum warm onboarding case 143 ms | 19.34 s |
| Lint/release command below, after explicit KTX dependency fix | All five touched modules' Lint passed; 151 release tests passed; release manifest verified | 30 s |
| `make targeted ONLY="core:ui:PolicyLinksTest"` after KTX dependency fix | 4 passed | 7 s |
| Final motion-policy wiring: `feature:onboarding:WalkthroughDotPolicyTest feature:onboarding:WalkthroughScreenTest#dotsAreHiddenFromAccessibility` | 3 passed; screen now branches directly on the tested spec choice | 6 s |
| `./gradlew --console=plain :feature:onboarding:lintDebug` after final policy wiring | Passed | 6 s |
| `make generators-check` (final) | Passed, including all regenerated-file comparisons and the corrected Python glyph count | ≈ 6.10 s log interval |
| `make size` before / after | 3,226,785 → 3,250,521 bytes, **+23,736 bytes**; native-library gate passed | 52 s / 43 s |

The layout invocation used the same result/timing arguments as the Makefile, restricted to the touched modules:

```sh
Scripts/run-tier.sh phase8-layout \
  --results 'app/build/test-results/testDebugUnitTest/*.xml' \
  --results 'core/*/build/test-results/testDebugUnitTest/*.xml' \
  --results 'feature/onboarding/build/test-results/testDebugUnitTest/*.xml' \
  --timings 'app/build/cf-timings/testDebugUnitTest/*.json' \
  --timings 'core/*/build/cf-timings/testDebugUnitTest/*.json' \
  --timings 'feature/onboarding/build/cf-timings/testDebugUnitTest/*.json' \
  -- ./gradlew --console=plain :app:testDebugUnitTest :feature:onboarding:testDebugUnitTest \
  :core:ui:testDebugUnitTest :core:designsystem:testDebugUnitTest :core:navigation:testDebugUnitTest -Pcf.tier=layout
```

Navigation has no `@Layout` cases; its 27 pure routing cases passed in the JVM checks. Existing App, core:ui and design-system layout guards passed. The onboarding layout fork used 2,542 ms for its first SDK case and 71 ms for its first walkthrough-guards case; later onboarding cases were at most 143 ms.

```sh
./gradlew --console=plain :app:lintDebug :feature:onboarding:lintDebug :core:ui:lintDebug \
  :core:designsystem:lintDebug :core:navigation:lintDebug \
  :app:testReleaseUnitTest :app:verifyReleaseManifest -Pcf.tier=quick
```

Initial release compilation failed because `PolicyLinks.toUri` needed an explicit `:core:ui` KTX dependency. Declaring the existing catalog artifact fixed release compilation; policy tests, all requested Lint, release tests and manifest verification then passed. The final bundle contains only the existing `libandroidx.graphics.path.so` native library. Browser and core-ktx already existed in the app dependency graph through auth; the whole phase's measured growth is below the brief's 100 KB allowance.

## Capture review

Files are `build/captures/<Board>_pixel8_<en|nl>_<light|dark>.png`, exactly 24 PNGs at the test-only 375 × 812 dp board size. Representative page-1/page-2/page-3, ready/failed, light/dark and Dutch captures were inspected locally. External board acceptance remains with the orchestrator's reviewer.

| Board | Difference / disposition |
|---|---|
| Walkthrough, WalkthroughReplay | Compact icon-only blocks follow pinned iOS; caffeine is dotted. Insets follow the pixel fixture. The footer was corrected to 40 dp visual height with a 48 dp native touch target. Replay uses the same page-1 appearance. |
| Walkthrough2 | Existing 32 dp category swatches, as pinned iOS / OQ-8-1; raw board swatches are 36 dp. |
| Walkthrough3 | Existing category swatches and the shared 80 × 6 dp progress bar (OQ-A334 / OQ-A348). |
| TermsUpdate, TermsFailed | Pinned generic legal summary and three single-style bullets replace the board placeholders; footnote omits the designer annotation; policy type is 400; section heading uses the generated zero-tracking style. At 375 × 812, the longer legal copy uses whole-page scrolling: some actions/footnote are below the captured viewport, especially the failed Dutch state. The 411 × 914 ready-English guard proves Accept visible without scrolling. The guards prove both actions can be reached in all five profiles. |

`make captures` ran once. A targeted capture-class run regenerated the PNGs after the footer correction. The sole emulator journey preceded that visual-height correction; its 48 dp target, callbacks and routing were unchanged. The final policy-spec wiring preserves that same layout and uses the JVM-tested decision directly.

## StrictMode

```sh
/opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 logcat -d -s StrictMode
```

The full buffer contained historical non-ChronoFlow entries at 03:55 and **zero `net.chronoflow` frames**. Raw output is `build/phase8-evidence/strictmode-full.log`. The journey-window query returned empty output:

```sh
/opt/homebrew/share/android-commandlinetools/platform-tools/adb -s emulator-5554 \
  logcat -d -T '11-20 06:29:00.000' -s StrictMode
```

```text
(no output)
```

## Brief adaptations and decisions

- Preflight checked the exact branch and upstream APIs. The one initial refresh records `activeAccountDatabase()` for SQL fixtures and the existing `SqlSyncDatabase` adapter for sync tests.
- The icon generator's Python test also needed its glyph count changed from 43 to 44. The final generator check passes.
- Typography uses the landed `CfTheme.type.style(CfTypeScale.… )` API. `ApiJson.response` replaces the brief's unused Json parameter. No earlier API was renamed.
- Caller cancellation is rethrown while inactive; foreign cancellation maps to failure, with revoked permits handled first as session changes. Cleanup joins use non-cancellable teardown.
- The original dot test could not observe the update through its cleared-semantics/manual-frame path. It was replaced with pure JVM motion-policy tests (including the ordinary-motion control) and one structural check on the cleared container. Dots still stay hidden from accessibility.
- Root entry decorators/revision keys already existed; the deleted real-graph gate tests were not restored. App tests use the shell with fakes, and only the allowlisted recreation case reaches MainActivity.
- OQ-A337–OQ-A347 copy all eleven brief defaults. OQ-A348 settles progress at 80 × 6 dp. No Android-only copy was added.
- Terms' pending-sign-out sheet remains the Phase 10a slot; Preferences' replay entry remains Phase 10b. Both plan cross-references and the seeded tooling command are added.

## Orchestrator checks

Per the user's limit, `make check`, `make perf`, `make perf-timeline` and `make baseline-profile` were not run. No Phase 8 benchmark number is claimed. For the startup path/profile follow-up, run:

```sh
make baseline-profile
make perf
```

No timeline implementation or benchmark journey was changed. The emulator remains running. No push, rebase or post-task history rewrite was performed.
