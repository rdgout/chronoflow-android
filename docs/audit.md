# Audit

2026-11-22. Stack top: `92781204`; Phase 14 branch after the committed brief `b2d26cf3`. This record distinguishes source comparison, JVM capture evidence and device/manual gates. No new product screen, state, string, token or icon is introduced.

Boards were read from the committed HTML and the master branch selected by each wrapper. The canvas runtime/support.js and blob assets are absent: no wrapper browser render or pixel-perfect browser/font match is claimed (OQ-14-1). Captures use the existing production fixtures in fresh, accumulated area runs; large outputs remain under `build/`.

Commands: `make captures APPEND=1 ONLY=Splash,Main,Auth,Walkthrough,Terms,PolicyReader`; `make captures APPEND=1 ONLY=Plan,Trips,Today,ItemDetail,SheetStates,TripActions,Error`; `make captures APPEND=1 ONLY=Wizard,Guest,TripAirportSearch,TripSearchStates,Picker`; `make captures APPEND=1 ONLY=Set,DarkSet,PasswordPrompt,SignOutPending,WizardReminders`. Affected groups were refreshed after each fix. Logs/timings are under `build/phase14/`; `build/audit/coverage.{json,md}` lists every actual path.

The priority comparison sheets under `build/audit/priority/` also place Plan, PlanList, Today, Trips, ItemDetail, SetHome, WizardOutbound, AuthSignIn and Main beside their iOS references; WizardReady has no screenshot counterpart. Fixture dates/positions differ, so this is a structural comparison. The iOS Outbound reference shows an incomplete multi-leg route, not the direct-flight Outbound board; Task 4 records the closest existing seed. The Sign-in board fixture explicitly shows the capability-on design, while the shipped capability stays false.

The 47 contact sheets under `build/audit/contact/` were inspected across English/light, Dutch/dark, wide and large-font cells, with full-size checks of the two product defects. A collage is a review aid; geometry, effective 48 dp targets and the 1.3 root cap are proven by the production-component layout guards, not inferred from scaled PNGs. Native bars/windows, motion and service behavior require the separate device evidence.

## Summary

| Area | Groups | Covered | Exempt | Not built | Fixed in this phase | Open | Recorded decisions | Needs a device |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| auth | 11 | 11/11 | 0 | 0 | 0 | 0 | 11 | 1 |
| wizard | 96 | 94/94 | 0 | 0 | 7 | 0 | 95 | 2 |
| trip | 34 | 34/34 | 0 | 0 | 2 | 0 | 34 | 1 |
| settings | 39 | 39/39 | 0 | 0 | 0 | 0 | 39 | 2 |
| other | 14 | 10/10 | 0 | 2 | 0 | 0 | 14 | 4 |

Counts: 334 canvas entries; 42 excluded; 292 in-scope board files; 194 state groups. Four specification/icon groups have no matrix, and GuestExcluded/PolicyReader remain unbuilt. All 188 required screen groups have their required cells; there are no cell exemptions. Summary verdict counts count groups, so a group may have both a decision and a device gate. Two product fixes and one capture-fixture correction are distinct commits.

## Board comparison

### AppIcon (AppIcon.dc.html)

Canvas title: App icon · Default, Dark, Tinted, Liquid Glass, Android. Screen: `app/src/main/AndroidManifest.xml`. Owner phase: 3a. Kind: `nonScreen`. Captures: not required (see kind/reason); exact cells/files in the coverage report. iOS reference: none.

Checks: Source specification/scope and the named acceptance evidence; no fabricated screen capture.

- **Recorded decision:** Launcher icon; evidenced by the launcher shot in Task 4. This specification board is not an application screen. (source: Phase 14 brief §5.1.)
- **Needs a device:** Launcher mask and monochrome/themed appearance; Task 4 records an uncropped launcher shot. iOS Liquid Glass appearances are iOS-only.

### SplashSpec (SplashSpec.dc.html)

Canvas title: Splash · spec. Screen: `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/LaunchOverlay.kt`. Owner phase: 3a. Kind: `nonScreen`. Captures: not required (see kind/reason); exact cells/files in the coverage report. iOS reference: none.

Checks: Source specification/scope and the named acceptance evidence; no fabricated screen capture.

- **Recorded decision:** Geometry specification; covered by `feature:launch:LaunchLayoutTests` (0b/3a). This specification board is not an application screen. (source: Phase 14 brief §5.1.)

### SplashScreen (SplashScreen.dc.html)

Canvas title: Splash · data ready (animated). Screen: `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/LaunchOverlay.kt`. Owner phase: 3b. Kind: `standard`. Captures: `build/captures/SplashScreen*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `01-launch-t00400ms.png`, `40-signedin-launch-motion-1s.png`.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** The user removed dots/stars. Launch uses the pinned iOS timeline and measured moon destination rather than the canvas looping preview; Welcome uses the settled three-line headline, unshadowed copy plate and guest-wizard destination. (source: `docs/phase-3c-validation.md:105–107`, `docs/design-system.md:141–147`, OQ-A108.)

### SplashSlow (SplashSlow.dc.html)

Canvas title: Splash · first load, still loading. Screen: `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/LaunchOverlay.kt`. Owner phase: 3b. Kind: `standard`. Captures: `build/captures/SplashSlow*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** The user removed dots/stars. Launch uses the pinned iOS timeline and measured moon destination rather than the canvas looping preview; Welcome uses the settled three-line headline, unshadowed copy plate and guest-wizard destination. (source: `docs/phase-3c-validation.md:105–107`, `docs/design-system.md:141–147`, OQ-A108.)

### SplashWelcome (SplashWelcome.dc.html)

Canvas title: Splash · signed out, into Welcome. Screen: `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/LaunchOverlay.kt`. Owner phase: 3c. Kind: `standard`. Captures: `build/captures/SplashWelcome*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** The user removed dots/stars. Launch uses the pinned iOS timeline and measured moon destination rather than the canvas looping preview; Welcome uses the settled three-line headline, unshadowed copy plate and guest-wizard destination. (source: `docs/phase-3c-validation.md:105–107`, `docs/design-system.md:141–147`, OQ-A108.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

### AuthSignIn (AuthSignIn.dc.html)

Canvas title: Sign in. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/signin/SignInScreen.kt`. Owner phase: 5a. Kind: `standard`. Captures: `build/captures/AuthSignIn*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `13-sign-in.png`.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

### AuthSignInError (AuthSignInError.dc.html)

Canvas title: Sign in · error banner. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/signin/SignInScreen.kt`. Owner phase: 5a. Kind: `standard`. Captures: `build/captures/AuthSignInError*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)

### AuthStates (AuthStates.dc.html)

Canvas title: Sign-in error banners. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/shell/AuthScreen.kt`. Owner phase: 5b. Kind: `standard`. Captures: `build/captures/AuthStates*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Error-banner fill/border, 18 dp radius, 18 dp circle-alert and stacked 14/20 title/message match the source. Dutch wraps naturally. Fields have no fill; error rows wrap without ellipsis. The caller can suppress announcements as on iOS. (source: `docs/design-system.md:131`.)

### AuthTwoFactor (AuthTwoFactor.dc.html)

Canvas title: Sign in · two-factor code. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/twofactor/TwoFactorScreen.kt`. Owner phase: 5b. Kind: `standard`. Captures: `build/captures/AuthTwoFactor*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Build instead; Why (source: `docs/auth.md:271`.)

- **Recorded decision:** The proof starts empty and clears after attempts; recovery xxxx-xxxx is a placeholder only. The active server field key chooses its error copy; inactive-field errors do not leak across modes. No footer, as pinned TwoFactorView. (source: `docs/auth.md:273–280`, pinned `TwoFactorModel.swift:56–59` / `TwoFactorView.swift:60–62`.)

### AuthForgot (AuthForgot.dc.html)

Canvas title: Forgot password. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/forgot/ForgotPasswordScreen.kt`. Owner phase: 5c. Kind: `standard`. Captures: `build/captures/AuthForgot*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Settled: starts empty rather than `rick@example.com`; notice uses the submitted recipient and the catalog's “Check your inbox.” including its period; throttled banner uses “Too many requests.” and the request-specific body (source: `docs/auth.md:439`.)

### AuthReset (AuthReset.dc.html)

Canvas title: Set a new password. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/reset/ResetPasswordScreen.kt`. Owner phase: 5c. Kind: `standard`. Captures: `build/captures/AuthReset*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Settled: honest “Sign in with your new password once it's saved.” subtitle; “Save new password”; full 12-character/upper/lower/number/symbol/data-leak policy replaces the old 10-character hint; no drawn back (source: `docs/auth.md:440`.)
- **Recorded decision:** Native adaptation: long policy wraps and increases card height; fields remain at least 48 dp and scroll rather than shrinking type (source: `docs/auth.md:441`.)

### AuthResetDone (AuthResetDone.dc.html)

Canvas title: Password saved. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/reset/ResetPasswordScreen.kt`. Owner phase: 5c. Kind: `standard`. Captures: `build/captures/AuthResetDone*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Settled: “Password updated. Sign in with your new password.”; other devices are signed out; Continue → Sign in, no session; check glyph; no drawn back (source: `docs/auth.md:442`.)
- **Recorded decision:** Settled: 48 dp minimum recovery/Continue buttons, 48 dp heading circle with 24 dp glyph and 8 dp below (source: `docs/auth.md:444`.)

### AuthResetExpired (AuthResetExpired.dc.html)

Canvas title: Reset link expired. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/reset/ResetPasswordScreen.kt`. Owner phase: 5c. Kind: `standard`. Captures: `build/captures/AuthResetExpired*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Settled: editable link email; resend happens directly and switches to Forgot sent in place, including its heading/subtitle and no glyph (source: `docs/auth.md:443`.)
- **Recorded decision:** Settled: 48 dp minimum recovery/Continue buttons, 48 dp heading circle with 24 dp glyph and 8 dp below (source: `docs/auth.md:444`.)

### AuthSignUp (AuthSignUp.dc.html)

Canvas title: Create an account. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/signup/SignUpScreen.kt`. Owner phase: 5d. Kind: `standard`. Captures: `build/captures/AuthSignUp*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `14-sign-up.png`.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Settled: real form starts empty; capture alone uses board samples; full 12-character policy replaces the short 10-character hint; two policy links and the native switch row replace the board's single link and eye button (source: `docs/auth.md:568`.)
- **Recorded decision:** Native adaptation: 48 dp fields and wrapped policy increase card height. At 375 × 812 the consent/footer and error-state submit are reached by scrolling; no type or control shrinks. Guards prove reachability, including Dutch/large font (source: `docs/auth.md:569`.)

### AuthVerify (AuthVerify.dc.html)

Canvas title: Verify your email. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/verify/VerifyEmailScreen.kt`. Owner phase: 5d. Kind: `standard`. Captures: `build/captures/AuthVerify*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Settled: real account email/deadline; capture uses 42 seconds and disabled Resend. Normal waiting has no countdown at zero. The catalog's Dutch sentence takes precedence over board wording (source: `docs/auth.md:570`.)

### AuthVerifyFailed (AuthVerifyFailed.dc.html)

Canvas title: Verify email failed. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/verify/VerifyEmailScreen.kt`. Owner phase: 5d. Kind: `standard`. Captures: `build/captures/AuthVerifyFailed*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Brand/header alignment, field/banner order, catalog-backed copy roles, card radius, footer and scroll reachability; Dutch/large-font wrapping and centered wide column.

- **Recorded decision:** Plain sky without dots; generated en/nl catalogs and the pinned auth models replace board sample credentials/copy. Fields, links and actions keep native 48 dp targets; short screens scroll. The shipped passkey capability remains false. (source: `docs/auth.md:7–40`, `plan.md` A15 / OQ-A12 / OQ-A21.)
- **Recorded decision:** Settled: 48 dp problem disc, generated destructive glyph and 8 dp below it; both actions have a 48 dp visual minimum; no different-email action/countdown (source: `docs/auth.md:571`.)

### Main (Main.dc.html)

Canvas title: Welcome. Screen: `feature/launch/src/main/kotlin/net/chronoflow/feature/launch/welcome/WelcomeScreen.kt`. Owner phase: 6a. Kind: `standard`. Captures: `build/captures/Main*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `15-welcome-en.png`.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** The user removed dots/stars. Launch uses the pinned iOS timeline and measured moon destination rather than the canvas looping preview; Welcome uses the settled three-line headline, unshadowed copy plate and guest-wizard destination. (source: `docs/phase-3c-validation.md:105–107`, `docs/design-system.md:141–147`, OQ-A108.)
- **Recorded decision:** Welcome in its final state, without replaying the intro (source: `docs/auth.md:34`.)
- **Recorded decision:** Dawn and OnSky use the token fills, 52 dp minimum height and 16 sp 600/500 labels. The neutral gallery background makes the translucent OnSky fill less visible than on the board sky. Sky samples are separately checked; no dots or shadow are added. (source: `docs/design-system.md:130`.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

### Trips (Trips.dc.html)

Canvas title: My trips. Screen: `feature/trips/src/main/kotlin/net/chronoflow/feature/trips/TripsScreen.kt`. Owner phase: 6a. Kind: `standard`. Captures: `build/captures/Trips*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `03-my-trips.png`, `33-trips-dark.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)

### TripsEmpty (TripsEmpty.dc.html)

Canvas title: My trips · no trips. Screen: `feature/trips/src/main/kotlin/net/chronoflow/feature/trips/TripsScreen.kt`. Owner phase: 6a. Kind: `standard`. Captures: `build/captures/TripsEmpty*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)

### TripsLoading (TripsLoading.dc.html)

Canvas title: My trips · first load. Screen: `feature/trips/src/main/kotlin/net/chronoflow/feature/trips/TripsScreen.kt`. Owner phase: 6a. Kind: `standard`. Captures: `build/captures/TripsLoading*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)

### Plan (Plan.dc.html, PlanDark.dc.html)

Canvas title: Day plan · lands on today (mid-scroll) / Day plan, dark. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6b. Kind: `phoneWithWideSibling`. Captures: `build/captures/Plan*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `04-trip-timeline-top.png`, `30-trip-timeline-dark.png`, `34-trip-timeline-nl.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)

### PlanDayMenu (PlanDayMenu.dc.html)

Canvas title: Day plan · day menu (system menu). Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6b. Kind: `phoneWithWideSibling`. Captures: `build/captures/PlanDayMenu*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)

### PlanEnd (PlanEnd.dc.html)

Canvas title: Trip page · last day, end of plan. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6b. Kind: `standard`. Captures: `build/captures/PlanEnd*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Fixed:** Phone capture fixtures incorrectly rendered timeline mode although these wrappers select view=list. Use the list index to reach the ending; the mode/ending guard covers phone and the existing wide timeline policy. Production behavior is unchanged. Commit `5d4bf198`.

### PlanEndNoText (PlanEndNoText.dc.html)

Canvas title: End card without end text. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6b. Kind: `standard`. Captures: `build/captures/PlanEndNoText*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Fixed:** Phone capture fixtures incorrectly rendered timeline mode although these wrappers select view=list. Use the list index to reach the ending; the mode/ending guard covers phone and the existing wide timeline policy. Production behavior is unchanged. Commit `5d4bf198`.

### PlanScrolled (PlanScrolled.dc.html, PlanScrolledDark.dc.html)

Canvas title: Day plan · scrolled: compact day line, tab bar minimised / Day plan · scrolled, dark. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6b. Kind: `phoneWithWideSibling`. Captures: `build/captures/PlanScrolled*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)

### PlanTimelineTop (PlanTimelineTop.dc.html)

Canvas title: Day plan · timeline, from the top. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6b. Kind: `standard`. Captures: `build/captures/PlanTimelineTop*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `06-trip-summary.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)

### ErrorNotFound (ErrorNotFound.dc.html)

Canvas title: Not found (404). Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/state/TripPageErrorPage.kt`. Owner phase: 6c. Kind: `standard`. Captures: `build/captures/ErrorNotFound*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** State cards and phone skeletons retain iOS metrics/copy; error pages keep iOS text with board buttons and a native back arrow. Fixture action availability follows the supplied callbacks; it is not evidence that the live entry disables an online wizard action. (source: `plan.md` OQ-A273–A283.)

### ErrorServer (ErrorServer.dc.html)

Canvas title: Server error (500, 503). Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/state/TripPageErrorPage.kt`. Owner phase: 6c. Kind: `standard`. Captures: `build/captures/ErrorServer*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** State cards and phone skeletons retain iOS metrics/copy; error pages keep iOS text with board buttons and a native back arrow. Fixture action availability follows the supplied callbacks; it is not evidence that the live entry disables an online wizard action. (source: `plan.md` OQ-A273–A283.)

### PlanList (PlanList.dc.html)

Canvas title: Day plan · list, from the top. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6c. Kind: `standard`. Captures: `build/captures/PlanList*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `05-trip-list-mode.png`, `31-trip-list-dark.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** List rows use pinned iOS 14/20 title, 13/18 interval and 14/12 padding. The phone has no selected-row outline; foldInner renders the timeline plus list column. (source: `plan.md` OQ-A272 / OQ-A309 / OQ-A318 / OQ-A323.)

### PlanListScrolled (PlanListScrolled.dc.html)

Canvas title: Day plan · list, scrolled. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6c. Kind: `standard`. Captures: `build/captures/PlanListScrolled*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** List rows use pinned iOS 14/20 title, 13/18 interval and 14/12 padding. The phone has no selected-row outline; foldInner renders the timeline plus list column. (source: `plan.md` OQ-A272 / OQ-A309 / OQ-A318 / OQ-A323.)

### PlanLoading (PlanLoading.dc.html)

Canvas title: Trip page · first load. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6c. Kind: `standard`. Captures: `build/captures/PlanLoading*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** State cards and phone skeletons retain iOS metrics/copy; error pages keep iOS text with board buttons and a native back arrow. Fixture action availability follows the supplied callbacks; it is not evidence that the live entry disables an online wizard action. (source: `plan.md` OQ-A273–A283.)

### PlanNoPlan (PlanNoPlan.dc.html)

Canvas title: Trip page · no plan yet. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6c. Kind: `standard`. Captures: `build/captures/PlanNoPlan*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** State cards and phone skeletons retain iOS metrics/copy; error pages keep iOS text with board buttons and a native back arrow. Fixture action availability follows the supplied callbacks; it is not evidence that the live entry disables an online wizard action. (source: `plan.md` OQ-A273–A283.)

### PlanRemote (PlanRemote.dc.html)

Canvas title: Plan downloading. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6c. Kind: `phoneWithWideSibling`. Captures: `build/captures/PlanRemote*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** State cards and phone skeletons retain iOS metrics/copy; error pages keep iOS text with board buttons and a native back arrow. Fixture action availability follows the supplied callbacks; it is not evidence that the live entry disables an online wizard action. (source: `plan.md` OQ-A273–A283.)

### PlanRemoteLoadingNL (PlanRemoteLoadingNL.dc.html)

Canvas title: Plan downloading (NL). Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6c. Kind: `standard`. Captures: `build/captures/PlanRemoteLoadingNL*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** State cards and phone skeletons retain iOS metrics/copy; error pages keep iOS text with board buttons and a native back arrow. Fixture action availability follows the supplied callbacks; it is not evidence that the live entry disables an online wizard action. (source: `plan.md` OQ-A273–A283.)

### PlanRemoteOffline (PlanRemoteOffline.dc.html, PlanRemoteOfflineNL.dc.html)

Canvas title: Plan not available offline / Plan not available offline (NL). Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6c. Kind: `phoneWithWideSibling`. Captures: `build/captures/PlanRemoteOffline*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** State cards and phone skeletons retain iOS metrics/copy; error pages keep iOS text with board buttons and a native back arrow. Fixture action availability follows the supplied callbacks; it is not evidence that the live entry disables an online wizard action. (source: `plan.md` OQ-A273–A283.)

### ItemDetail (ItemDetail.dc.html)

Canvas title: Plan step detail. Screen: `core/ui/src/main/kotlin/net/chronoflow/core/ui/plan/StepSheetContent.kt`. Owner phase: 6d. Kind: `standard`. Captures: `build/captures/ItemDetail*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `07-step-sheet.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native sheet chrome and 48 dp actions; JVM sheet-content captures omit the underlying trip. Pinned iOS swatch/section spacing, generated legend names and priority-only trip legend remain. Calendar, complete and delete-trip actions are outside scope; fixture step data has no Why lines. (source: `docs/phase-6d-validation.md:42`, OQ-A294 / OQ-A298 / OQ-A299.)
- **Recorded decision:** Sheet surface is card with 38 dp top corners. The handle, dialog buttons, calendar, clock dial, switch, segmented buttons and refresh indicator are native Material; their geometry intentionally differs from the drawn iOS chrome. (source: `docs/design-system.md:134`.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

### PlanLegend (PlanLegend.dc.html)

Canvas title: Legend sheet. Screen: `core/ui/src/main/kotlin/net/chronoflow/core/ui/plan/LegendSheetContent.kt`. Owner phase: 6d. Kind: `standard`. Captures: `build/captures/PlanLegend*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `08-legend-sheet.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native sheet chrome and 48 dp actions; JVM sheet-content captures omit the underlying trip. Pinned iOS swatch/section spacing, generated legend names and priority-only trip legend remain. Calendar, complete and delete-trip actions are outside scope; fixture step data has no Why lines. (source: `docs/phase-6d-validation.md:42`, OQ-A294 / OQ-A298 / OQ-A299.)

### SheetStates (SheetStates.dc.html)

Canvas title: Sheet states. Screen: `core/ui/src/main/kotlin/net/chronoflow/core/ui/plan/StepGoneContent.kt`. Owner phase: 6d. Kind: `standard`. Captures: `build/captures/SheetStates*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native sheet chrome and 48 dp actions; JVM sheet-content captures omit the underlying trip. Pinned iOS swatch/section spacing, generated legend names and priority-only trip legend remain. Calendar, complete and delete-trip actions are outside scope; fixture step data has no Why lines. (source: `docs/phase-6d-validation.md:42`, OQ-A294 / OQ-A298 / OQ-A299.)
- **Recorded decision:** Skeleton sizes/radii and low-contrast muted fill match. Skeletons are static because iOS wins over the pulse in the bundle. Small buttons retain a 32 dp visual box with a 48 dp target. (source: `docs/design-system.md:133`.)
- **Recorded decision:** Sheet surface is card with 38 dp top corners. The handle, dialog buttons, calendar, clock dial, switch, segmented buttons and refresh indicator are native Material; their geometry intentionally differs from the drawn iOS chrome. (source: `docs/design-system.md:134`.)

### TripActions (TripActions.dc.html)

Canvas title: Trip actions. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/actions/TripActionsSheet.kt`. Owner phase: 6d. Kind: `standard`. Captures: `build/captures/TripActions*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `29-trip-actions.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native sheet chrome and 48 dp actions; JVM sheet-content captures omit the underlying trip. Pinned iOS swatch/section spacing, generated legend names and priority-only trip legend remain. Calendar, complete and delete-trip actions are outside scope; fixture step data has no Why lines. (source: `docs/phase-6d-validation.md:42`, OQ-A294 / OQ-A298 / OQ-A299.)

### TripActionsNoPlan (TripActionsNoPlan.dc.html)

Canvas title: Trip actions · no plan yet. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/actions/TripActionsSheet.kt`. Owner phase: 6d. Kind: `standard`. Captures: `build/captures/TripActionsNoPlan*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native sheet chrome and 48 dp actions; JVM sheet-content captures omit the underlying trip. Pinned iOS swatch/section spacing, generated legend names and priority-only trip legend remain. Calendar, complete and delete-trip actions are outside scope; fixture step data has no Why lines. (source: `docs/phase-6d-validation.md:42`, OQ-A294 / OQ-A298 / OQ-A299.)

### PlanRemoteWide (PlanRemoteWide.dc.html)

Canvas title: Plan downloading · wide. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6e. Kind: `wideOnly`. Captures: `build/captures/PlanRemoteWide*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** Wide mode uses 40 dp/hour, no Steps label, iOS spacing/row metrics and the compact-band layout. The right list pins below that band. Remote skeletons are static with a 22 dp bar radius; the dropdown is native, titled by the catalog. (source: `docs/phase-6e-validation.md:79`, `plan.md` OQ-A305–A323.)

### PlanRemoteWideOffline (PlanRemoteWideOffline.dc.html)

Canvas title: Plan not available offline · wide. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6e. Kind: `wideOnly`. Captures: `build/captures/PlanRemoteWideOffline*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** Wide mode uses 40 dp/hour, no Steps label, iOS spacing/row metrics and the compact-band layout. The right list pins below that band. Remote skeletons are static with a 22 dp bar radius; the dropdown is native, titled by the catalog. (source: `docs/phase-6e-validation.md:79`, `plan.md` OQ-A305–A323.)

### PlanWide (PlanWide.dc.html, PlanWideDark.dc.html)

Canvas title: Day plan · wide, from the top / Day plan · wide, dark, on today. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6e. Kind: `wideOnly`. Captures: `build/captures/PlanWide*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** Wide mode uses 40 dp/hour, no Steps label, iOS spacing/row metrics and the compact-band layout. The right list pins below that band. Remote skeletons are static with a 22 dp bar radius; the dropdown is native, titled by the catalog. (source: `docs/phase-6e-validation.md:79`, `plan.md` OQ-A305–A323.)

### PlanWideDayMenu (PlanWideDayMenu.dc.html)

Canvas title: Day plan · wide, day menu. Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6e. Kind: `wideOnly`. Captures: `build/captures/PlanWideDayMenu*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** Wide mode uses 40 dp/hour, no Steps label, iOS spacing/row metrics and the compact-band layout. The right list pins below that band. Remote skeletons are static with a 22 dp bar radius; the dropdown is native, titled by the catalog. (source: `docs/phase-6e-validation.md:79`, `plan.md` OQ-A305–A323.)

### PlanWideScrolled (PlanWideScrolled.dc.html)

Canvas title: Day plan · wide, scrolled (compact line). Screen: `feature/trip/src/main/kotlin/net/chronoflow/feature/trip/page/TripPageScreen.kt`. Owner phase: 6e. Kind: `wideOnly`. Captures: `build/captures/PlanWideScrolled*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Material back/menu/navigation replaces drawn glass; pinned IATA titles, catalog copy and plan fixtures replace illustrative cities, dates and schedules. Summary cards have no shadow; headers scroll away into the compact overlay and notices expand with the day. (source: `plan.md` OQ-A248–A254 / OQ-A272–A283 / OQ-A316.)
- **Recorded decision:** Wide mode uses 40 dp/hour, no Steps label, iOS spacing/row metrics and the compact-band layout. The right list pins below that band. Remote skeletons are static with a 22 dp bar radius; the dropdown is native, titled by the catalog. (source: `docs/phase-6e-validation.md:79`, `plan.md` OQ-A305–A323.)

### Today (Today.dc.html)

Canvas title: Today. Screen: `feature/today/src/main/kotlin/net/chronoflow/feature/today/ui/TodayScreen.kt`. Owner phase: 7. Kind: `standard`. Captures: `build/captures/Today*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `02-today-en-7s.png`, `22-today-nl.png`, `32-today-dark.png`.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)
- **Recorded decision:** Top-left glass icon; Decorative 44 dp glass item; No item; No action on pinned iOS; OQ-A328. (source: `docs/board-deltas.md:6`.)
- **Recorded decision:** App bar and tabs; iOS system chrome; Native M3 app bar and navigation; A9; board surfaces retain their tokens. (source: `docs/board-deltas.md:7`.)
- **Recorded decision:** Panel swatch; Default `md`, 36 dp; Dedicated row board's `sm`, 28 dp; OQ-A326; matches TodayRowStates. (source: `docs/board-deltas.md:8`.)
- **Recorded decision:** Running subtitle; Start and end time; End time only, prefixed with →; Pinned TodayStepRow copy/behaviour. (source: `docs/board-deltas.md:9`.)
- **Recorded decision:** Hidden categories notice; Sample “Travel steps are hidden”; Generated some-important/all-categories copy; Pinned iOS; Settings tab is the destination. (source: `docs/board-deltas.md:10`.)
- **Recorded decision:** Adaptation label; Sample “Adapting”; Generated “Adjusting” and its Dutch value; Pinned iOS copy. (source: `docs/board-deltas.md:11`.)
- **Recorded decision:** Adapting pill text; Foreground; Foreground; Board appearance; iOS uses dawnText. (source: `docs/board-deltas.md:14`.)
- **Recorded decision:** Shared hero Next title; Medium 500; Regular 400; Preserve pinned iOS and the landed OQ-A240/6a choice. (source: `docs/board-deltas.md:25`.)

### TodayCompleted (TodayCompleted.dc.html)

Canvas title: Today · only completed trips. Screen: `feature/today/src/main/kotlin/net/chronoflow/feature/today/ui/TodayScreen.kt`. Owner phase: 7. Kind: `standard`. Captures: `build/captures/TodayCompleted*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)
- **Recorded decision:** App bar and tabs; iOS system chrome; Native M3 app bar and navigation; A9; board surfaces retain their tokens. (source: `docs/board-deltas.md:7`.)
- **Recorded decision:** Card padding; 32 dp vertical, 20 dp horizontal; Same; Board appearance; no shadow. (source: `docs/board-deltas.md:18`.)
- **Recorded decision:** Native title ink in JVM captures; Title visible; Missing in these captures; Unresolved after idle/frame/idle; OQ-A336 retains CfLargeTopAppBar and requires device verification. Its structural tag is present in every state. (source: `docs/board-deltas.md:27`.) Historical JVM limitation; this phase's refreshed Today empty/future/completed captures show the title. Device chrome review remains separate.

### TodayEmpty (TodayEmpty.dc.html)

Canvas title: Today · no trips yet. Screen: `feature/today/src/main/kotlin/net/chronoflow/feature/today/ui/TodayScreen.kt`. Owner phase: 7. Kind: `standard`. Captures: `build/captures/TodayEmpty*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)
- **Recorded decision:** App bar and tabs; iOS system chrome; Native M3 app bar and navigation; A9; board surfaces retain their tokens. (source: `docs/board-deltas.md:7`.)
- **Recorded decision:** Primary action; 40 dp visual height; 40 dp visual / ≥48 dp touch target; Native CfButton Lg and minimum target. (source: `docs/board-deltas.md:17`.)
- **Recorded decision:** Native title ink in JVM captures; Title visible; Missing in these captures; Unresolved after idle/frame/idle; OQ-A336 retains CfLargeTopAppBar and requires device verification. Its structural tag is present in every state. (source: `docs/board-deltas.md:27`.) Historical JVM limitation; this phase's refreshed Today empty/future/completed captures show the title. Device chrome review remains separate.

### TodayFuture (TodayFuture.dc.html)

Canvas title: Today · only a future trip. Screen: `feature/today/src/main/kotlin/net/chronoflow/feature/today/ui/TodayScreen.kt`. Owner phase: 7. Kind: `standard`. Captures: `build/captures/TodayFuture*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)
- **Recorded decision:** App bar and tabs; iOS system chrome; Native M3 app bar and navigation; A9; board surfaces retain their tokens. (source: `docs/board-deltas.md:7`.)
- **Recorded decision:** Reminder sentence; Promises an evening reminder; Omitted; Pinned iOS drops this promise. The rest stays one board-style paragraph. (source: `docs/board-deltas.md:12`.)
- **Recorded decision:** Upcoming badge; Outline; Outline; Board appearance; iOS uses secondary. (source: `docs/board-deltas.md:13`.)
- **Recorded decision:** Optional Next row in capture; Present in sample; Absent with the Swift snapshot fixture; The fixture supplies the active trip detail while filtering trips to upcoming; the required stale-detail guard rejects it. The screen test separately renders the non-null Next branch; the live source observes the selected hero detail. (source: `docs/board-deltas.md:26`.)
- **Recorded decision:** Native title ink in JVM captures; Title visible; Missing in these captures; Unresolved after idle/frame/idle; OQ-A336 retains CfLargeTopAppBar and requires device verification. Its structural tag is present in every state. (source: `docs/board-deltas.md:27`.) Historical JVM limitation; this phase's refreshed Today empty/future/completed captures show the title. Device chrome review remains separate.

### TodayLoading (TodayLoading.dc.html)

Canvas title: Today · first load. Screen: `feature/today/src/main/kotlin/net/chronoflow/feature/today/ui/TodayScreen.kt`. Owner phase: 7. Kind: `standard`. Captures: `build/captures/TodayLoading*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)
- **Recorded decision:** App bar and tabs; iOS system chrome; Native M3 app bar and navigation; A9; board surfaces retain their tokens. (source: `docs/board-deltas.md:7`.)
- **Recorded decision:** Heading skeleton; 40% × 20 dp line; Included; Board appearance; iOS omits it. (source: `docs/board-deltas.md:16`.)

### TodayRowStates (TodayRowStates.dc.html)

Canvas title: Today · done and skipped steps. Screen: `feature/today/src/main/kotlin/net/chronoflow/feature/today/ui/TodayScreen.kt`. Owner phase: 7. Kind: `standard`. Captures: `build/captures/TodayRowStates*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Summary/header/day order, lane or row mode, category/priority/status treatment, source schedule, scroll state and ending; wide list and large-font containment.

- **Recorded decision:** Native system large title, no decorative glass logo; the Material bar/rail stays visible while scrolling. Cards retain the explicit board badge/check/shadow/meta-chip/connector choices and pinned iOS Next title weight. (source: `plan.md` OQ-A236 / OQ-A237 / OQ-A240, `docs/board-deltas.md`.)
- **Recorded decision:** Running subtitle; Start and end time; End time only, prefixed with →; Pinned TodayStepRow copy/behaviour. (source: `docs/board-deltas.md:9`.)
- **Recorded decision:** Row title; 16/22, medium; 16/22, medium; Board appearance; iOS uses 16/24 regular. (source: `docs/board-deltas.md:15`.)
- **Recorded decision:** Documentation headings and notes; Board includes explanations; Only the three app row states; The headings and notes are board documentation, not app copy. (source: `docs/board-deltas.md:19`.)

### PolicyReader (PolicyReader.dc.html)

Canvas title: Policy reader. Screen: `core/ui/src/main/kotlin/net/chronoflow/core/ui/links/PolicyLinks.kt`. Owner phase: 8. Kind: `notBuilt`. Captures: not required (see kind/reason); exact cells/files in the coverage report. iOS reference: none.

Checks: Source specification/scope and the named acceptance evidence; no fabricated screen capture.

- **Recorded decision:** No in-app policy-reader screen is built: PolicyLinks opens the external policy URL in a native Custom Tab (OQ-A32/OQ-A339/OQ-A346). The board draws an in-app reader that the pinned iOS/landed Phase 8 scope does not build. External browser content and chrome need device review. (source: `tools/audit/board-scope.json`, phase 8, OQ-14-7.)
- **Needs a device:** External Custom Tab content/toolbar and browser accessibility; no in-app legal reader is invented. Open the production policy URL from Terms and return with system Back. (OQ-A32 / OQ-A339 / OQ-A346.)

### TermsFailed (TermsFailed.dc.html)

Canvas title: Terms update · saving failed. Screen: `feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/terms/TermsScreen.kt`. Owner phase: 8. Kind: `standard`. Captures: `build/captures/TermsFailed*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** Pinned generic legal summary and three single-style bullets replace the board placeholders; footnote omits the designer annotation; policy type is 400; section heading uses the generated zero-tracking style. At 375 × 812, the longer legal copy uses whole-page scrolling: some actions/footnote are below the captured viewport, especially the failed Dutch state. The 411 × 914 ready-English guard proves Accept visible without scrolling. The guards prove both actions can be reached in all five profiles. (source: `docs/phase-8-results.md:76`.)

### TermsUpdate (TermsUpdate.dc.html)

Canvas title: Terms update (before walkthrough). Screen: `feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/terms/TermsScreen.kt`. Owner phase: 8. Kind: `standard`. Captures: `build/captures/TermsUpdate*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** Pinned generic legal summary and three single-style bullets replace the board placeholders; footnote omits the designer annotation; policy type is 400; section heading uses the generated zero-tracking style. At 375 × 812, the longer legal copy uses whole-page scrolling: some actions/footnote are below the captured viewport, especially the failed Dutch state. The 411 × 914 ready-English guard proves Accept visible without scrolling. The guards prove both actions can be reached in all five profiles. (source: `docs/phase-8-results.md:76`.)

### Walkthrough (Walkthrough.dc.html)

Canvas title: Walkthrough · 1. Screen: `feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/walkthrough/WalkthroughScreen.kt`. Owner phase: 8. Kind: `standard`. Captures: `build/captures/Walkthrough*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `23-walkthrough.png`.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** Compact icon-only blocks follow pinned iOS; caffeine is dotted. Insets follow the pixel fixture. The footer was corrected to 40 dp visual height with a 48 dp native touch target. Replay uses the same page-1 appearance. (source: `docs/phase-8-results.md:73`.)

### Walkthrough2 (Walkthrough2.dc.html)

Canvas title: Walkthrough · 2. Screen: `feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/walkthrough/WalkthroughScreen.kt`. Owner phase: 8. Kind: `standard`. Captures: `build/captures/Walkthrough2*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** Existing 32 dp category swatches, as pinned iOS / OQ-8-1; raw board swatches are 36 dp. (source: `docs/phase-8-results.md:74`.)

### Walkthrough3 (Walkthrough3.dc.html)

Canvas title: Walkthrough · 3. Screen: `feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/walkthrough/WalkthroughScreen.kt`. Owner phase: 8. Kind: `standard`. Captures: `build/captures/Walkthrough3*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** Existing category swatches and the shared 80 × 6 dp progress bar (OQ-A334 / OQ-A348). (source: `docs/phase-8-results.md:75`.)

### WalkthroughReplay (WalkthroughReplay.dc.html)

Canvas title: Walkthrough · replay from Preferences. Screen: `feature/onboarding/src/main/kotlin/net/chronoflow/feature/onboarding/walkthrough/WalkthroughScreen.kt`. Owner phase: 8. Kind: `standard`. Captures: `build/captures/WalkthroughReplay*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected board branch, element order, resource/type/color roles, spacing/corners, state, wrapping and window adaptation.

- **Recorded decision:** Compact icon-only blocks follow pinned iOS; caffeine is dotted. Insets follow the pixel fixture. The footer was corrected to 40 dp visual height with a 48 dp native touch target. Replay uses the same page-1 appearance. (source: `docs/phase-8-results.md:73`.)

### WizardSpec (WizardSpec.dc.html)

Canvas title: Wizard · flow, rules, haptics and decisions (updated 4 Nov). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9a. Kind: `nonScreen`. Captures: not required (see kind/reason); exact cells/files in the coverage report. iOS reference: none.

Checks: Source specification/scope and the named acceptance evidence; no fabricated screen capture.

- **Recorded decision:** Rules deck; covered by `core:wizard:WizardProgressTest` and the ported wizard model suites (9a–9h). This specification board is not an application screen. (source: Phase 14 brief §5.1.)

### TripAirportSearch (TripAirportSearch.dc.html)

Canvas title: Airport search · results. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/search/AirportSearchSheet.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/TripAirportSearch*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Fixed:** Three-letter airport codes wrapped at large font. The code column now grows from its existing board minimum; a line-count/width guard fails before and passes after the fix (`8faee9b3`). Fresh affected captures were inspected. No type, token or root font-cap change.
- **Recorded decision:** `WizardSheet` rows (code 15/700, 60 high, radius 16, no count line); `WizardSheet` is newer and iOS built it [R]; OQ-9c-4 (source: `docs/wizard.md:41`.)

### TripSearchStates (TripSearchStates.dc.html)

Canvas title: Airport search · other states. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/search/AirportSearchSheet.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/TripSearchStates*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Hint/searching/empty/error states render inside the current WizardSheet search surface, replacing the older TripSearchStates catalogue framing. Generated server/catalog copy, native fields and the 60 dp result-row mapping stay shared with WizardAirportFrom. (source: `docs/wizard.md:28–51`, OQ-9c-4.)

### WizardAirportFrom (WizardAirportFrom.dc.html, WizardAirportFromDarkNL.dc.html)

Canvas title: Wizard · Airport search · From / Wizard · Airport search · From · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/search/AirportSearchSheet.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardAirportFrom*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Fixed:** Three-letter airport codes wrapped at large font. The code column now grows from its existing board minimum; a line-count/width guard fails before and passes after the fix (`8faee9b3`). Fresh affected captures were inspected. No type, token or root font-cap change.
- **Recorded decision:** The model's value for the state shown: 8 on Same and AirportFrom, 50 on AirportHome and StopSearchHome, and 30 on Discard (Outbound, from 9d); progress comes from 9a, never per board [R]; OQ-9c-6 (source: `docs/wizard.md:42`.)

### WizardAirportHome (WizardAirportHome.dc.html, WizardAirportHomeDarkNL.dc.html)

Canvas title: Wizard · Airport search · flight home To / Wizard · Airport search · flight home To · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/search/AirportSearchSheet.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardAirportHome*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Fixed:** Three-letter airport codes wrapped at large font. The code column now grows from its existing board minimum; a line-count/width guard fails before and passes after the fix (`8faee9b3`). Fresh affected captures were inspected. No type, token or root font-cap change.
- **Recorded decision:** The model's value for the state shown: 8 on Same and AirportFrom, 50 on AirportHome and StopSearchHome, and 30 on Discard (Outbound, from 9d); progress comes from 9a, never per board [R]; OQ-9c-6 (source: `docs/wizard.md:42`.)
- **Recorded decision:** Captured over the flight-home route (`WizardHomeRoute`), which is where the To row opens it; iOS opens it from the route [F]; times are 9d (source: `docs/wizard.md:44`.)

### WizardCopy (WizardCopy.dc.html)

Canvas title: Wizard · copy tables, accessibility, Android, layout. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `nonScreen`. Captures: not required (see kind/reason); exact cells/files in the coverage report. iOS reference: none.

Checks: Source specification/scope and the named acceptance evidence; no fabricated screen capture.

- **Recorded decision:** Copy deck; covered by `core:ui:StringParityTest` and `core:wizard:WizardCopyKeysTest` (0c/9a–9h). This specification board is not an application screen. (source: Phase 14 brief §5.1.)

### WizardDiscard (WizardDiscard.dc.html, WizardDiscardDarkNL.dc.html)

Canvas title: Wizard · Close · discard this trip? / Wizard · Close · discard this trip? · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardDiscard*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** The model's value for the state shown: 8 on Same and AirportFrom, 50 on AirportHome and StopSearchHome, and 30 on Discard (Outbound, from 9d); progress comes from 9a, never per board [R]; OQ-9c-6 (source: `docs/wizard.md:42`.)
- **Recorded decision:** Captured over the base Outbound draft from 9d; `fromLastTrip = false` (9c used Where temporarily); Outbound is 9d [R]; OQ-9c-5 (source: `docs/wizard.md:43`.)
- **Recorded decision:** Now Outbound at 30, replacing the 9c temporary Where backdrop (source: `docs/wizard.md:409`.)

### WizardHomeRoute (WizardHomeRoute.dc.html, WizardHomeRouteDarkNL.dc.html)

Canvas title: Wizard · Your flight home · route / Wizard · Your flight home · route · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardHomeRoute*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardHomeRouteStop (WizardHomeRouteStop.dc.html, WizardHomeRouteStopDarkNL.dc.html)

Canvas title: Wizard · Your flight home · route with a stop / Wizard · Your flight home · route with a stop · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardHomeRouteStop*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardStopSearch (WizardStopSearch.dc.html, WizardStopSearchDarkNL.dc.html)

Canvas title: Wizard · Stop search sheet (from Where) / Wizard · Stop search sheet (from Where) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/search/AirportSearchSheet.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardStopSearch*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Fixed:** Three-letter airport codes wrapped at large font. The code column now grows from its existing board minimum; a line-count/width guard fails before and passes after the fix (`8faee9b3`). Fresh affected captures were inspected. No type, token or root font-cap change.

### WizardStopSearchHome (WizardStopSearchHome.dc.html, WizardStopSearchHomeDarkNL.dc.html)

Canvas title: Wizard · Stop search · flight home / Wizard · Stop search · flight home · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/search/AirportSearchSheet.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardStopSearchHome*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Fixed:** Three-letter airport codes wrapped at large font. The code column now grows from its existing board minimum; a line-count/width guard fails before and passes after the fix (`8faee9b3`). Fresh affected captures were inspected. No type, token or root font-cap change.
- **Recorded decision:** The model's value for the state shown: 8 on Same and AirportFrom, 50 on AirportHome and StopSearchHome, and 30 on Discard (Outbound, from 9d); progress comes from 9a, never per board [R]; OQ-9c-6 (source: `docs/wizard.md:42`.)

### WizardStopSearchSecond (WizardStopSearchSecond.dc.html, WizardStopSearchSecondDarkNL.dc.html)

Canvas title: Wizard · Stop search · second stop / Wizard · Stop search · second stop · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/search/AirportSearchSheet.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardStopSearchSecond*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Fixed:** Three-letter airport codes wrapped at large font. The code column now grows from its existing board minimum; a line-count/width guard fails before and passes after the fix (`8faee9b3`). Fresh affected captures were inspected. No type, token or root font-cap change.

### WizardWhere (WizardWhere.dc.html, WizardWhereDark.dc.html, WizardWhereNL.dc.html)

Canvas title: Wizard · 1 · Where / Wizard · 1 · Where (dark) / Wizard · 1 · Where (NL). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardWhere*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** The pinned no-health-data audience notice is below the route card; the raw board omits it. Only age band and shift work are collected. The additional copy can move stop controls below the initial large-font viewport; they remain in the scrolling content. (source: `docs/wizard.md:45`, OQ-A374 / OQ-A375.)

### WizardWhereEmpty (WizardWhereEmpty.dc.html, WizardWhereEmptyDarkNL.dc.html)

Canvas title: Wizard · Where · empty (guest or first trip) / Wizard · Where · empty (guest or first trip) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardWhereEmpty*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `16-wizard-from.png`.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** The pinned no-health-data audience notice is below the route card; the raw board omits it. Only age band and shift work are collected. The additional copy can move stop controls below the initial large-font viewport; they remain in the scrolling content. (source: `docs/wizard.md:45`, OQ-A374 / OQ-A375.)

### WizardWhereSame (WizardWhereSame.dc.html)

Canvas title: Wizard · Same airport twice. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardWhereSame*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** The pinned no-health-data audience notice is below the route card; the raw board omits it. Only age band and shift work are collected. The additional copy can move stop controls below the initial large-font viewport; they remain in the scrolling content. (source: `docs/wizard.md:45`, OQ-A374 / OQ-A375.)
- **Recorded decision:** The model's value for the state shown: 8 on Same and AirportFrom, 50 on AirportHome and StopSearchHome, and 30 on Discard (Outbound, from 9d); progress comes from 9a, never per board [R]; OQ-9c-6 (source: `docs/wizard.md:42`.)

### WizardWhereStop (WizardWhereStop.dc.html, WizardWhereStopDarkNL.dc.html)

Canvas title: Wizard · Where · one stop / Wizard · Where · one stop · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardWhereStop*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** The pinned no-health-data audience notice is below the route card; the raw board omits it. Only age band and shift work are collected. The additional copy can move stop controls below the initial large-font viewport; they remain in the scrolling content. (source: `docs/wizard.md:45`, OQ-A374 / OQ-A375.)

### WizardWhereStops (WizardWhereStops.dc.html, WizardWhereStopsDarkNL.dc.html)

Canvas title: Wizard · Where · two stops / Wizard · Where · two stops · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9c. Kind: `standard`. Captures: `build/captures/WizardWhereStops*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** The pinned no-health-data audience notice is below the route card; the raw board omits it. Only age band and shift work are collected. The additional copy can move stop controls below the initial large-font viewport; they remain in the scrolling content. (source: `docs/wizard.md:45`, OQ-A374 / OQ-A375.)

### PickerDateTime (PickerDateTime.dc.html)

Canvas title: Date and time · system picker in our sheet. Screen: `core/designsystem/src/main/kotlin/net/chronoflow/core/designsystem/system/CfPickers.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/PickerDateTime*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Material calendar/time dialogs or the native dial replace the combined iOS-shaped calendar/wheel. Context and actions use existing resources; there is no invented combined picker or summary chip. Sleep replaces the retired GuestSleep backdrop, and PickerTime uses stored 23:30. (source: `docs/wizard.md:408`, `docs/wizard.md:750–756`, A9 / OQ-A8.)
- **Fixed:** The custom date-picker title began at the rounded dialog edge and lost its first letter. Restored native title insets with existing space6/space4 tokens; four-profile containment guard fails before and passes after (`4e5ecf47`). The affected JVM date/time captures were refreshed; the device pre-fix image independently showed the clipping.
- **Recorded decision:** Separate Material date/time dialogs replace the combined iOS-shaped sheet; custom title/context, native calendar/dial, Cancel/Done, no summary chip. M3 marks the phone's today (OQ-A394, OQ-A396, OQ-A397) (source: `docs/wizard.md:408`.)

### WizardOutbound (WizardOutbound.dc.html, WizardOutboundDark.dc.html, WizardOutboundNL.dc.html)

Canvas title: Wizard · 2 · Outbound flight / Wizard · 2 · Outbound flight (dark) / Wizard · 2 · Outbound flight (NL). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutbound*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### WizardOutboundEmpty (WizardOutboundEmpty.dc.html, WizardOutboundEmptyDarkNL.dc.html)

Canvas title: Wizard · Outbound · nothing picked yet / Wizard · Outbound · nothing picked yet · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundEmpty*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardOutboundErrors (WizardOutboundErrors.dc.html, WizardOutboundErrorsDarkNL.dc.html)

Canvas title: Wizard · Outbound · arrival before departure / Wizard · Outbound · arrival before departure · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundErrors*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### WizardOutboundOvernight (WizardOutboundOvernight.dc.html, WizardOutboundOvernightDarkNL.dc.html)

Canvas title: Wizard · Times · two flights, overnight stop / Wizard · Times · two flights, overnight stop · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundOvernight*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** 22:25 first arrival, matching the pinned seed and a real complete night (OQ-A391) (source: `docs/wizard.md:403`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### WizardOutboundPast (WizardOutboundPast.dc.html, WizardOutboundPastDarkNL.dc.html)

Canvas title: Wizard · Outbound · arrival in the past (member, after Checking) / Wizard · Outbound · arrival in the past (member, after Checking) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundPast*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### WizardOutboundStop (WizardOutboundStop.dc.html, WizardOutboundStopDarkNL.dc.html)

Canvas title: Wizard · Times · two flights, with a connection / Wizard · Times · two flights, with a connection · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundStop*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `17-wizard-outbound.png`.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### WizardOutboundStopEmpty (WizardOutboundStopEmpty.dc.html, WizardOutboundStopEmptyDarkNL.dc.html)

Canvas title: Wizard · Times · two flights, nothing picked yet / Wizard · Times · two flights, nothing picked yet · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundStopEmpty*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Model progress 22 while incomplete, instead of the illustrated 30 (OQ-A393) (source: `docs/wizard.md:404`.)

### WizardOutboundThree (WizardOutboundThree.dc.html, WizardOutboundThreeDarkNL.dc.html)

Canvas title: Wizard · Times · three flights (scrolls) / Wizard · Times · three flights (scrolls) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundThree*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Model progress 22 while incomplete, instead of the illustrated 30 (OQ-A393) (source: `docs/wizard.md:404`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### WizardOutboundTwoErrors (WizardOutboundTwoErrors.dc.html, WizardOutboundTwoErrorsDarkNL.dc.html)

Canvas title: Wizard · Times · error on the second flight / Wizard · Times · error on the second flight · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardOutboundTwoErrors*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### WizardReturn (WizardReturn.dc.html, WizardReturnDark.dc.html, WizardReturnNL.dc.html)

Canvas title: Wizard · 3 · Coming back? / Wizard · 3 · Coming back? (dark) / Wizard · 3 · Coming back? (NL). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardReturn*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `27-wizard-return.png`.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** One way selected by default, following iOS (OQ-A388) (source: `docs/wizard.md:405`.)

### WizardReturnFlight (WizardReturnFlight.dc.html, WizardReturnFlightDarkNL.dc.html)

Canvas title: Wizard · Your flight home · times / Wizard · Your flight home · times · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardReturnFlight*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardReturnFlightPast (WizardReturnFlightPast.dc.html, WizardReturnFlightPastDarkNL.dc.html)

Canvas title: Wizard · Your flight home · arrival in the past (after Checking) / Wizard · Your flight home · arrival in the past (after Checking) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9d. Kind: `standard`. Captures: `build/captures/WizardReturnFlightPast*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Review round 2 corrects the pill font mapping: the board uses `.ios` system text with −0.01 em tracking, rather than the labels’ Figtree. Filled English Amsterdam rows remain inline at 390 dp with the generated platform picker style. OQ-A398 still stacks rows whose visual content cannot fit, including 280 dp / 1.3; 48 dp targets do not enlarge the drawn pill (source: `docs/wizard.md:406`.)

### PickerTime (PickerTime.dc.html)

Canvas title: Time picker. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/sleep/WizardSleepTimeSheet.kt`. Owner phase: 9e. Kind: `standard`. Captures: `build/captures/PickerTime*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Material calendar/time dialogs or the native dial replace the combined iOS-shaped calendar/wheel. Context and actions use existing resources; there is no invented combined picker or summary chip. Sleep replaces the retired GuestSleep backdrop, and PickerTime uses stored 23:30. (source: `docs/wizard.md:408`, `docs/wizard.md:750–756`, A9 / OQ-A8.)

### WizardExcluded (WizardExcluded.dc.html, WizardExcludedDarkNL.dc.html)

Canvas title: Wizard · No plan · under 18 (guest) / Wizard · No plan · under 18 (guest) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9e. Kind: `standard`. Captures: `build/captures/WizardExcluded*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardExcludedShift (WizardExcludedShift.dc.html, WizardExcludedShiftDarkNL.dc.html)

Canvas title: Wizard · No plan · saved answer (member) / Wizard · No plan · saved answer (member) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9e. Kind: `standard`. Captures: `build/captures/WizardExcludedShift*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardQAge (WizardQAge.dc.html, WizardQAgeDarkNL.dc.html)

Canvas title: Wizard · Question · age (guest, with disclaimer) / Wizard · Question · age (guest, with disclaimer) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9e. Kind: `standard`. Captures: `build/captures/WizardQAge*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardQShift (WizardQShift.dc.html, WizardQShiftNL.dc.html)

Canvas title: Guest · 6 · Shift work / Wizard · question · Shift work · Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9e. Kind: `standard`. Captures: `build/captures/WizardQShift*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `19-wizard-qshift.png`.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardQShiftLarge (WizardQShiftLarge.dc.html)

Canvas title: Wizard · question · Shift work · Dutch, larger text. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9e. Kind: `largeFontOnly`. Captures: `build/captures/WizardQShiftLarge*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetFreeSleep (WizardSheetFreeSleep.dc.html, WizardSheetFreeSleepDarkNL.dc.html)

Canvas title: Wizard · Sheet · sleep on free days / Wizard · Sheet · sleep on free days · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9e. Kind: `standard`. Captures: `build/captures/WizardSheetFreeSleep*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSleep (WizardSleep.dc.html)

Canvas title: Guest · 3 · Your sleep. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9e. Kind: `standard`. Captures: `build/captures/WizardSleep*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardCheckOffline (WizardCheckOffline.dc.html, WizardCheckOfflineDarkNL.dc.html)

Canvas title: Wizard · Checking · offline / Wizard · Checking · offline · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardCheckOffline*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardCheckServer (WizardCheckServer.dc.html, WizardCheckServerDarkNL.dc.html)

Canvas title: Wizard · Checking · server error or timeout / Wizard · Checking · server error or timeout · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardCheckServer*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardCheckThrottle (WizardCheckThrottle.dc.html, WizardCheckThrottleDarkNL.dc.html)

Canvas title: Wizard · Checking · too many attempts / Wizard · Checking · too many attempts · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardCheckThrottle*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardChecking (WizardChecking.dc.html, WizardCheckingDark.dc.html, WizardCheckingNL.dc.html)

Canvas title: Wizard · 4 · Checking / Wizard · 4 · Checking (dark) / Wizard · 4 · Checking (NL). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardChecking*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `18-wizard-check.png`.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

### WizardCheckingAgain (WizardCheckingAgain.dc.html, WizardCheckingAgainDarkNL.dc.html)

Canvas title: Wizard · Checking again after the last question / Wizard · Checking again after the last question · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardCheckingAgain*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardCheckingSlow (WizardCheckingSlow.dc.html, WizardCheckingSlowDarkNL.dc.html)

Canvas title: Wizard · Checking · slow (after ~10 s) / Wizard · Checking · slow (after ~10 s) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardCheckingSlow*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardFineTune (WizardFineTune.dc.html, WizardFineTuneDark.dc.html, WizardFineTuneDarkNL.dc.html, WizardFineTuneNL.dc.html)

Canvas title: Wizard · Fine-tune · needed extras marked / Wizard · 5 · Fine-tune (dark) / Wizard · Fine-tune · needed extras marked · dark, Dutch / Wizard · 5 · Fine-tune (NL). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardFineTune*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardFineTuneBusy (WizardFineTuneBusy.dc.html, WizardFineTuneBusyDarkNL.dc.html)

Canvas title: Wizard · Fine-tune · re-check after a change / Wizard · Fine-tune · re-check after a change · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardFineTuneBusy*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardFineTuneMulti (WizardFineTuneMulti.dc.html, WizardFineTuneMultiDarkNL.dc.html)

Canvas title: Wizard · Fine-tune · several flights and stays / Wizard · Fine-tune · several flights and stays · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardFineTuneMulti*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardFineTuneNone (WizardFineTuneNone.dc.html, WizardFineTuneNoneDarkNL.dc.html)

Canvas title: Wizard · Fine-tune · no extras (from Ready) / Wizard · Fine-tune · no extras (from Ready) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardFineTuneNone*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardFineTuneSheet (WizardFineTuneSheet.dc.html, WizardFineTuneSheetDarkNL.dc.html)

Canvas title: Wizard · Sheet · side of the plane / Wizard · Sheet · side of the plane · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardFineTuneSheet*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardFineTuneSleep (WizardFineTuneSleep.dc.html, WizardFineTuneSleepDarkNL.dc.html)

Canvas title: Wizard · Fine-tune · different sleep times on / Wizard · Fine-tune · different sleep times on · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardFineTuneSleep*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetBlue (WizardSheetBlue.dc.html, WizardSheetBlueDarkNL.dc.html)

Canvas title: Wizard · Sheet · blue-blocking glasses / Wizard · Sheet · blue-blocking glasses · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetBlue*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetBlueError (WizardSheetBlueError.dc.html, WizardSheetBlueErrorDarkNL.dc.html)

Canvas title: Wizard · Sheet · blue-blocking · rating out of range / Wizard · Sheet · blue-blocking · rating out of range · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetBlueError*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetBusy (WizardSheetBusy.dc.html, WizardSheetBusyDarkNL.dc.html)

Canvas title: Wizard · Sheet · busy hours / Wizard · Sheet · busy hours · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetBusy*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetCheckout (WizardSheetCheckout.dc.html, WizardSheetCheckoutDarkNL.dc.html)

Canvas title: Wizard · Sheet · late check-out / Wizard · Sheet · late check-out · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetCheckout*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetDrinks (WizardSheetDrinks.dc.html, WizardSheetDrinksDarkNL.dc.html)

Canvas title: Wizard · Sheet · drinks / Wizard · Sheet · drinks · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetDrinks*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetDrinksError (WizardSheetDrinksError.dc.html, WizardSheetDrinksErrorDarkNL.dc.html)

Canvas title: Wizard · Sheet · drinks · nothing added / Wizard · Sheet · drinks · nothing added · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetDrinksError*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetLight (WizardSheetLight.dc.html, WizardSheetLightDarkNL.dc.html)

Canvas title: Wizard · Sheet · light box or glasses / Wizard · Sheet · light box or glasses · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetLight*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetRoom (WizardSheetRoom.dc.html, WizardSheetRoomDarkNL.dc.html)

Canvas title: Wizard · Sheet · dark room / Wizard · Sheet · dark room · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetRoom*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardSheetWake (WizardSheetWake.dc.html, WizardSheetWakeDarkNL.dc.html)

Canvas title: Wizard · Sheet · latest wake-up / Wizard · Sheet · latest wake-up · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/answers/WizardAnswerSheet.kt`. Owner phase: 9f. Kind: `standard`. Captures: `build/captures/WizardSheetWake*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Material calendar/time dialogs or the native dial replace the combined iOS-shaped calendar/wheel. Context and actions use existing resources; there is no invented combined picker or summary chip. Sleep replaces the retired GuestSleep backdrop, and PickerTime uses stored 23:30. (source: `docs/wizard.md:408`, `docs/wizard.md:750–756`, A9 / OQ-A8.)

### WizardCalculating (WizardCalculating.dc.html, WizardCalculatingDarkNL.dc.html)

Canvas title: Wizard · Calculating (Make my plan, Update my plan) / Wizard · Calculating (Make my plan, Update my plan) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardCalculating*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Native spinner; computed **10** days from 9–18 Oct inclusive, versus board sample **6**; Host layout review passed with the seed's day count; round 2 captures use the closest native upper-right gap: start **348.5°**, sweep **307.3°**, versus board **0°**, **288°** (clockwise from 3 o'clock) (source: `docs/wizard.md:1215`.)

### WizardReady (WizardReady.dc.html, WizardReadyDark.dc.html, WizardReadyNL.dc.html)

Canvas title: Wizard · 6 · Ready / Wizard · 6 · Ready (dark) / Wizard · 6 · Ready (NL). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReady*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Create, cost 1, balance 3; Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips described here (source: `docs/wizard.md:1205`.)

### WizardReadyAdjusted (WizardReadyAdjusted.dc.html, WizardReadyAdjustedDarkNL.dc.html)

Canvas title: Wizard · Ready · we adjusted your plan / Wizard · Ready · we adjusted your plan · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReadyAdjusted*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Modified review, Latest wake-up 08:00; body is the server reason, rather than the board's illustrative sentence; Host reviewed: server reason and seed data follow iOS; zero-inset footer and 9c chips (source: `docs/wizard.md:1210`.)

### WizardReadyChecking (WizardReadyChecking.dc.html, WizardReadyCheckingDarkNL.dc.html)

Canvas title: Wizard · Ready · re-check (expired, credits bought, recovery) / Wizard · Ready · re-check (expired, credits bought, recovery) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReadyChecking*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Busy primary at 0.7 alpha; Host layout review passed with the documented seed, footer and chip differences; round 2 captures use the closest native upper-right gap: start **348.5°**, sweep **307.3°**, versus board **0°**, **288°** (clockwise from 3 o'clock) (source: `docs/wizard.md:1211`.)

### WizardReadyFree (WizardReadyFree.dc.html, WizardReadyFreeDarkNL.dc.html)

Canvas title: Wizard · Ready · free trip / Wizard · Ready · free trip · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReadyFree*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Free-trip line; Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips (source: `docs/wizard.md:1208`.)

### WizardReadyNoCredits (WizardReadyNoCredits.dc.html, WizardReadyNoCreditsDarkNL.dc.html)

Canvas title: Wizard · Ready · no credits / Wizard · Ready · no credits · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReadyNoCredits*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Close only under A14; website action intentionally absent; Host reviewed: Close only follows A14; pinned seed data, zero-inset footer and 9c chips as above (source: `docs/wizard.md:1212`.)

### WizardReadyOneFree (WizardReadyOneFree.dc.html, WizardReadyOneFreeDarkNL.dc.html)

Canvas title: Wizard · Ready · flight home free / Wizard · Ready · flight home free · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReadyOneFree*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Return direction free, balance 3; Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips (source: `docs/wizard.md:1209`.)

### WizardReadyReturn (WizardReadyReturn.dc.html, WizardReadyReturnDarkNL.dc.html)

Canvas title: Wizard · Ready · with stops and a return / Wizard · Ready · with stops and a return · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReadyReturn*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** DXB stop and confirmed return; pinned seed cost **2**, board sample **1**; Host reviewed: uses the pinned one-way summary instead of the board's return summary; end date **Sun 18 Oct / zo 18 okt**, versus **Mon 26 Oct / ma 26 okt**; zero-inset footer and 9c chips (source: `docs/wizard.md:1206`.)

### WizardReadyUnlimited (WizardReadyUnlimited.dc.html, WizardReadyUnlimitedDarkNL.dc.html)

Canvas title: Wizard · Ready · unlimited plan / Wizard · Ready · unlimited plan · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardReadyUnlimited*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Unlimited, no balance; Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips (source: `docs/wizard.md:1207`.)

### WizardSaveOffline (WizardSaveOffline.dc.html, WizardSaveOfflineDarkNL.dc.html)

Canvas title: Wizard · Ready · save failed, offline / Wizard · Ready · save failed, offline · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardSaveOffline*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Offline failure; Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips (source: `docs/wizard.md:1213`.)

### WizardSaveServer (WizardSaveServer.dc.html, WizardSaveServerDarkNL.dc.html)

Canvas title: Wizard · Ready · save failed, server error / Wizard · Ready · save failed, server error · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9g. Kind: `standard`. Captures: `build/captures/WizardSaveServer*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Server failure; Host reviewed: matches apart from pinned seed data, zero-inset footer and 9c chips (source: `docs/wizard.md:1214`.)

### WizardCheckOfflineEdit (WizardCheckOfflineEdit.dc.html, WizardCheckOfflineEditDarkNL.dc.html)

Canvas title: Wizard · Edit · checking offline / Wizard · Edit · checking offline · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardCheckOfflineEdit*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Edit failure body, no back chevron. (source: `docs/wizard.md:1410`.)

### WizardCheckServerEdit (WizardCheckServerEdit.dc.html, WizardCheckServerEditDarkNL.dc.html)

Canvas title: Wizard · Edit · checking server error / Wizard · Edit · checking server error · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardCheckServerEdit*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Edit failure body, no back chevron. (source: `docs/wizard.md:1411`.)

### WizardCheckingEdit (WizardCheckingEdit.dc.html, WizardCheckingEditDarkNL.dc.html)

Canvas title: Wizard · Edit · checking the changes / Wizard · Edit · checking the changes · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardCheckingEdit*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** 100% initial floor, edit title, deterministic native spinner frame. (source: `docs/wizard.md:1409`.)

### WizardDiscardEdit (WizardDiscardEdit.dc.html, WizardDiscardEditDarkNL.dc.html)

Canvas title: Wizard · Close · edit, discard changes? / Wizard · Close · edit, discard changes? · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardDiscardEdit*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Native M3 dialog; Dutch confirmation is Wijzigingen verwijderen from the catalog. Native button arrangement replaces the board's custom vertical arrangement. The underlying summary uses the iOS seed `summary.line.key` ending “Your trip home is planned too.” rather than the board’s “and back again after your flight home.” (source: `docs/wizard.md:1413`.)

### WizardDuplicate (WizardDuplicate.dc.html, WizardDuplicateDarkNL.dc.html)

Canvas title: Wizard · Duplicate · opens on Outbound / Wizard · Duplicate · opens on Outbound · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardDuplicate*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Empty dates, copied-route subtitle, disabled Continue and no back chevron. (source: `docs/wizard.md:1414`.)

### WizardEdit (WizardEdit.dc.html, WizardEditDarkNL.dc.html)

Canvas title: Wizard · Edit · nothing changed yet / Wizard · Edit · nothing changed yet · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardEdit*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Disabled Update; pinned seed clears the profile, so Sleep is empty instead of the board's sample times. Runtime uses the stored body-clock fallback. (source: `docs/wizard.md:1402`.)

### WizardEditChanged (WizardEditChanged.dc.html, WizardEditChangedDarkNL.dc.html)

Canvas title: Wizard · Edit · changed on another device / Wizard · Edit · changed on another device · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardEditChanged*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Pinned baseline already has a return, so EditNote replaces the board's EditReturnNote (OQ-A455). Source-change sentence and return date use catalog copy. The summary uses the iOS seed `summary.line.key` ending “Your trip home is planned too.” rather than the board’s “and back again after your flight home.” (source: `docs/wizard.md:1403`.)

### WizardEditNoReturn (WizardEditNoReturn.dc.html, WizardEditNoReturnDarkNL.dc.html)

Canvas title: Wizard · Edit · return removed (no refund) / Wizard · Edit · return removed (no refund) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardEditNoReturn*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** NoRefund then EditNote; source baseline has a return while the current form is one way. (source: `docs/wizard.md:1404`.)

### WizardEditReturn (WizardEditReturn.dc.html)

Canvas title: Wizard · edit, adding a return flight (1 credit). Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardEditReturn*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Added return and EditReturnNote; no DarkNL wrapper exists. Its iOS seed `summary.line.key` ends “Your trip home is planned too.”; the board ends “and back again after your flight home.” (source: `docs/wizard.md:1405`.)

### WizardEditReturnNoCredits (WizardEditReturnNoCredits.dc.html, WizardEditReturnNoCreditsDarkNL.dc.html)

Canvas title: Wizard · Edit · add return, no credits / Wizard · Edit · add return, no credits · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardEditReturnNoCredits*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Close only; the board's website purchase action is omitted under A14. Its iOS seed `summary.line.key` ends “Your trip home is planned too.” rather than the board’s “and back again after your flight home.” (source: `docs/wizard.md:1406`.)

### WizardEditWeb (WizardEditWeb.dc.html, WizardEditWebDarkNL.dc.html)

Canvas title: Wizard · Edit · web trip that ends after nights / Wizard · Edit · web trip that ends after nights · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardEditWeb*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)
- **Recorded decision:** Nine-night end and stay-end summary; the pinned seat change makes this edit changed. (source: `docs/wizard.md:1407`.)

### WizardExcludedEdit (WizardExcludedEdit.dc.html, WizardExcludedEditDarkNL.dc.html)

Canvas title: Wizard · No plan · edit / Wizard · No plan · edit · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardExcludedEdit*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Server shift-work reason without the board's doctor line; replaces the pinned seed's under-18 reason (OQ-A456). (source: `docs/wizard.md:1412`.)

### WizardReturnWeb (WizardReturnWeb.dc.html, WizardReturnWebDarkNL.dc.html)

Canvas title: Wizard · Coming back? · web trip that ends after nights (edit) / Wizard · Coming back? · web trip that ends after nights (edit) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 9h. Kind: `standard`. Captures: `build/captures/WizardReturnWeb*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Edit context follows the board, unlike the pinned iOS seed; selected web card with the generated dawnText check at `space5` (other cards retain muted `space4` chevrons), and 45% progress. Review round 1 re-captures both boards after correcting the chevron and compares the host renders. Native text metrics wrap the web hint differently; the Dutch catalog also omits the board’s comma after “zo”. The earlier toolbar-chip difference remains. Back follows the model to Times, rather than the board link to Ready (OQ-A458). (source: `docs/wizard.md:1408`.)

### SetAccount (SetAccount.dc.html)

Canvas title: Account. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/account/AccountScreen.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SetAccount*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Data/policies rows remain outside the landed account screen; Profile, Language and Delete remain. (source: `docs/settings-home.md:31`.)
- **Recorded decision:** Profile, Language and Delete account; data/policies remain deferred. Native large app bar and navigation chrome. (source: `docs/settings-home.md:30`.)

### SetHome (DarkSetHome.dc.html, SetHome.dc.html, SetHomeNL.dc.html)

Canvas title: Settings · dark / Settings / Settings · Dutch (long labels). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/home/SettingsHomeScreen.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SetHome*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `10-settings-home.png`.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Row-card radius 22, full-width dividers, 36 dp discs, 16 dp glyphs, 20 dp chevrons and disabled opacity 0.5 match. Lock and chevron can coexist. Selected pills include the iOS check absent on the board. Badges use the board fills and outlines. (source: `docs/design-system.md:132`.)
- **Recorded decision:** Native M3 system large title and bar/navigation chrome. Account subtitle omits data/policies; Security omits passkeys. Readiness badge is below the subtitle in both languages; English board's trailing badge is not reproduced. Sign-out is the 40 dp outline pill with a native minimum touch target. Bottom padding is 40 dp because the Android bar occupies layout space. Sync type is generated 13/18; the syncing glyph stays static. (source: `docs/settings-home.md:27`.)
- **Recorded decision:** The fixture now paints the bottom inset with the app's dark background, matching `CfRoot`. The near-white band below the dark navigation bar was a fixture defect; light/dark pixel guards cover the supplied bottom inset. (source: `docs/settings-home.md:28`.)

### SetLanguage (SetLanguage.dc.html)

Canvas title: Language · saved toast. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/language/LanguageScreen.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SetLanguage*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Native large app bar and navigation chrome; saved feedback uses the shared inverse M3 Snackbar. The static capture shows the System selection; the device test verifies saved presentation through its counter. (source: `docs/settings-home.md:32`.)

### SetProfile (SetProfile.dc.html)

Canvas title: Profile · email change, validation error. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/profile/ProfileScreen.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SetProfile*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Name-only editor, selectable read-only email and Save changes replace the deferred email-change/password/link flow. (source: `docs/settings-home.md:32`.)
- **Recorded decision:** Name-only editor, selectable read-only email and “Save changes”; the board's new-email/password/link flow is deferred. Native large app bar and navigation chrome. (source: `docs/settings-home.md:31`.)

### SetStates (SetStates.dc.html)

Canvas title: Shared states. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/home/SettingsHomeScreen.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SetStates*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Row-card radius 22, full-width dividers, 36 dp discs, 16 dp glyphs, 20 dp chevrons and disabled opacity 0.5 match. Lock and chevron can coexist. Selected pills include the iOS check absent on the board. Badges use the board fills and outlines. (source: `docs/design-system.md:132`.)
- **Recorded decision:** The implementation uses the specified native Snackbar instead of the designed toast. Its action is background-colored in both themes to avoid the dark-theme dawn contrast failure. (source: `docs/design-system.md:135`.)
- **Recorded decision:** Four sync lines plus the offline-disabled row and connection hint. Review 1 removes the disabled row's chevron and groups its hint 12 dp below the card, matching the board; a geometry guard covers the gap. Generic saved/failed toasts, refusal Details and health/readiness examples are outside 10a. (source: `docs/settings-home.md:33`.)

### SetStatesMore (SetStatesMore.dc.html)

Canvas title: More settings states. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/home/SettingsHomeScreen.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SetStatesMore*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Loading and unavailable states only. Generated/iOS skeleton widths 150/190/130 and row height 60 replace board 140/180/120 and 64. Refusal feedback remains the existing shell's responsibility; no Details action. (source: `docs/settings-home.md:34`.)

### SignOutPending (SignOutPending.dc.html)

Canvas title: Sign out with unsynced changes. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/signout/SignOutPendingSheet.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SignOutPending*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native M3 sheet, scrim and handle. Pending text uses generated 14/20; footer uses board 13/18. Title counts the two displayed entries. `SignOutPendingSettings` has “2 changes waiting to sync” behind the sheet; its board has “Synced 2 min ago”. The capture follows iOS `SettingsHomeState`'s pending-over-age priority. `SignOutPending` uses the landed Phase 8 generic Terms background. (source: `docs/settings-home.md:29`.)

### SignOutPendingSettings (SignOutPendingSettings.dc.html)

Canvas title: Sign out from Settings · changes waiting. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/signout/SignOutPendingSheet.kt`. Owner phase: 10a. Kind: `standard`. Captures: `build/captures/SignOutPendingSettings*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native M3 sheet, scrim and handle. Pending text uses generated 14/20; footer uses board 13/18. Title counts the two displayed entries. `SignOutPendingSettings` has “2 changes waiting to sync” behind the sheet; its board has “Synced 2 min ago”. The capture follows iOS `SettingsHomeState`'s pending-over-age priority. `SignOutPending` uses the landed Phase 8 generic Terms background. (source: `docs/settings-home.md:29`.)

### SetBodyClock (DarkSetBodyClock.dc.html, SetBodyClock.dc.html)

Canvas title: Body clock · dark / Body clock · full scroll. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/bodyclock/BodyClockScreen.kt`. Owner phase: 10b. Kind: `standard`. Captures: `build/captures/SetBodyClock*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `11-settings-bodyclock.png`.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Two server-required answers, three age bands, neutral caffeine/flight-movement preferences, unified light equipment and Safety notices replace the legacy medical questionnaire. Section captures isolate the production cards; readiness is server-driven. No health answers are collected. (source: `docs/settings-preferences-body-clock.md:159–169`, `docs/settings-preferences-body-clock.md:221–234`.)
- **Recorded decision:** Native title/chrome. Readiness uses the server's two required answers and invalid-section count. Excluded replaces the board's medical “blocked” wording; ready/adjusted use the pinned iOS copy. (source: `docs/settings-preferences-body-clock.md:162`.)
- **Recorded decision:** Age bands are under 18 / 18–64 / 65+, with missing badges only for genuinely missing answers. Chronotype retains the empty “Not sure” id. (source: `docs/settings-preferences-body-clock.md:163`.)
- **Recorded decision:** iOS free-day choices and 128 dp adaptive time tiles. The separate native time sheet uses a dial and five-minute steps; the static section PNG does not show that window. (source: `docs/settings-preferences-body-clock.md:164`.)
- **Recorded decision:** Boolean sensitivity choices; medical questions removed; neutral “Leave caffeine out” switch. Drink controls retain 48 dp targets and wrap their label only when required at minimum width. (source: `docs/settings-preferences-body-clock.md:165`.)
- **Recorded decision:** One unified light-device question with its eye-caution caption, rather than separate light-box/visor questions. Rated blockers use the mDFD field and catalog hint. (source: `docs/settings-preferences-body-clock.md:166`.)
- **Recorded decision:** “Safety checks”: shift work, neutral flight-movement preference and the two notices. Medical questions and the board's required-health caption are removed. (source: `docs/settings-preferences-body-clock.md:167`.)
- **Recorded decision:** The full-width readiness pill is fixed and matches the board's width rule. The app's progress track, checkmark and “0/2 done” metadata are iOS parity (`SettingsBodyClockView.swift:425–436`) and remain despite their absence from the board. The app asks for two required answers, replacing the board's legacy count of three. Native page chrome and 411 dp wrapping shift the card and later sections. (source: `docs/settings-preferences-body-clock.md:228`.)
- **Recorded decision:** The supplied host render leaves the chronotype option area blank; the template and iOS define four cards. The app paints those cards, their hints and selection controls, which makes About taller. Age bands remain Under 18 / 18–64 / 65+, replacing the board's four legacy bands. Missing-answer decoration appears on age and shift work only when missing. These are pinned iOS / no-health-data rules. (source: `docs/settings-preferences-body-clock.md:229`.)
- **Recorded decision:** The provided host PNG shows needs answers. Ready and adjusted discs are now centered on their copy, as the pinned iOS HStack specifies; both regenerated variants were inspected and have geometry guards. Their copy, adjustment lines and excluded state follow server readiness and the existing §4 overrides, not the board's legacy complete/blocked wording. (source: `docs/settings-preferences-body-clock.md:230`.)
- **Recorded decision:** Native captures use the seed's 23:00 / 07:00 and same-as-workdays choice. Optional free-day tiles appear only when declared. The time sheet remains a native M3 dial with five-minute rounding; it is a separate window outside these static section captures. Adaptive tile width, not a fixed phone layout, controls stacking. (source: `docs/settings-preferences-body-clock.md:231`.)
- **Recorded decision:** Retains boolean sensitivity, the neutral exclusion preference and catalog drink labels; the removed medical questions and third sensitivity answer are absent. Native switches have the same deliberate Material appearance described above. Static seeds show no Saved marker until a local edit succeeds; the board's Caffeine Saved example is not manufactured. (source: `docs/settings-preferences-body-clock.md:232`.)
- **Recorded decision:** One unified light-device question and its caution replace separate light-box/visor questions. Safety contains shift work, the neutral flight-movement preference and the two notices, replacing the medical questionnaire and its required-health caption. Switch appearance remains native M3. The notices are information, not collected health answers. (source: `docs/settings-preferences-body-clock.md:233`.)
- **Recorded decision:** Non-intro PNGs isolate the complete production card with its page padding and phone-height background; they do not repeat the app bar or shell. Dark captures received the same width/centering fixes and pass light/dark geometry guards. No dark host render was supplied, so no side-by-side dark match is claimed. (source: `docs/settings-preferences-body-clock.md:234`.)

### SetPreferences (SetPreferences.dc.html, SetPreferencesNL.dc.html)

Canvas title: Preferences / Preferences · Dutch (long labels). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/preferences/PreferencesScreen.kt`. Owner phase: 10b. Kind: `standard`. Captures: `build/captures/SetPreferences*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `12-settings-preferences.png`.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Native M3 large title and system/navigation chrome; dawn switch tracks through `CfSwitch`; the historical 10b capture had no reminders subtitle; 12c now supplies the live permission/ahead-time subtitle. Native bars occupy layout space, so lower links may require scrolling. Dutch uses catalog “Slapen”, rather than board “Slaap”. (source: `docs/settings-preferences-body-clock.md:161`.)
- **Recorded decision:** Native M3 back control, system large title, safe insets and bottom navigation replace the board's glass labelled back pill and Fraunces page title (A9). This shifts the first card and the visible fold. The app has the category swatches required by iOS `SettingsPreferencesView.swift:104`; the board rows show text alone. Swatches and their label inset are retained as iOS parity. Reminders and replay remain below the initial app viewport and are reachable by scrolling. (source: `docs/settings-preferences-body-clock.md:223`.)
- **Recorded decision:** The board wraps “12-hour” below “24-hour”. Both implementations use intrinsic wrapping, not a forced line break. Production geometry tests prove wrapping at the board's 375 dp width in English and Dutch. At the wider 411 dp pixel8 width, the two English pills fit beside one another. This responsive difference is retained; no fixed board-width cap was introduced. (source: `docs/settings-preferences-body-clock.md:224`.)
- **Recorded decision:** Same native chrome and swatch differences. Dutch time-format pills wrap at both 375 and 411 dp, matching the supplied board's two rows. Copy uses catalog “Slapen” instead of board “Slaap” (OQ-A498). The larger native chrome and wrapped copy put Travel and the links farther below the fold; they remain scrollable. (source: `docs/settings-preferences-body-clock.md:225`.)
- **Recorded decision:** The board's `ios-switch` CSS paints white thumbs and a borderless translucent off track. Android retains the Material 3 switch through `CfSwitch` (A9 / OQ-A489): dawn checked track, `dawnForeground` checked thumb, and `input`/`mutedForeground` off track, outline and thumb. Those dark thumbs and outlined off tracks are an explicit retained native-control difference. The shared wrapper and earlier-phase controls were not changed. (source: `docs/settings-preferences-body-clock.md:226`.)
- **Recorded decision:** The historical 10b reminder subtitle was empty; 12c now derives it from permission/settings instead of the board example status. Replay has its catalog subtitle and no trailing chevron, matching iOS. Native navigation occupies layout space, so these rows are not necessarily visible in the initial capture. (source: `docs/settings-preferences-body-clock.md:227`.)

### PasswordPrompt (PasswordPrompt.dc.html, PasswordPromptNL.dc.html)

Canvas title: Prompt · show recovery codes / Prompt · wrong password (NL). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `phoneWithWideSibling`. Captures: `build/captures/PasswordPrompt*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)

### PasswordPrompt2FAOn (PasswordPrompt2FAOn.dc.html)

Canvas title: Prompt · turn on two-factor. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/PasswordPrompt2FAOn*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)

### PasswordPromptChecking (PasswordPromptChecking.dc.html)

Canvas title: Prompt · checking. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/PasswordPromptChecking*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)

### PasswordPromptForgot (PasswordPromptForgot.dc.html)

Canvas title: Prompt · forgot password (signed in). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/PasswordPromptForgot*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)

### PasswordPromptLocked (PasswordPromptLocked.dc.html)

Canvas title: Prompt · too many attempts. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/PasswordPromptLocked*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)
- **Recorded decision:** Shared native sheet/dialog chrome, safe insets and the existing 540 dp wide prompt. (source: `docs/settings-account-plan-acceptance.md:49`.)

### PasswordPromptNewCodes (PasswordPromptNewCodes.dc.html)

Canvas title: Prompt · new recovery codes. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/PasswordPromptNewCodes*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)

### PasswordPromptWide (PasswordPromptWide.dc.html)

Canvas title: Prompt · wide (form sheet). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `wideOnly`. Captures: `build/captures/PasswordPromptWide*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)

### PasswordPromptWrong (PasswordPromptWrong.dc.html)

Canvas title: Prompt · wrong password. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/PasswordPromptWrong*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)

### Set2FA (Set2FA.dc.html, Set2FANL.dc.html)

Canvas title: Two-factor · off, password first / Two-factor · off (NL). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/TwoFactorScreen.kt`. Owner phase: 10c. Kind: `phoneWithWideSibling`. Captures: `build/captures/Set2FA*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pinned iOS 48 dp actions, raw setup key and real QR replace smaller board controls/placeholder. CfRow uses 64 dp minimum and 14/20 subtitle; the fresh-codes toast is the shared Snackbar. Release omits passkeys while its capability is gated. (source: `docs/settings-security-acceptance.md:17–28`.)
- **Recorded decision:** Row-card radius 22, full-width dividers, 36 dp discs, 16 dp glyphs, 20 dp chevrons and disabled opacity 0.5 match. Lock and chevron can coexist. Selected pills include the iOS check absent on the board. Badges use the board fills and outlines. (source: `docs/design-system.md:132`.)

### Set2FACodes (Set2FACodes.dc.html)

Canvas title: Two-factor · codes shown after confirming. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/TwoFactorScreen.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/Set2FACodes*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pinned iOS 48 dp actions, raw setup key and real QR replace smaller board controls/placeholder. CfRow uses 64 dp minimum and 14/20 subtitle; the fresh-codes toast is the shared Snackbar. Release omits passkeys while its capability is gated. (source: `docs/settings-security-acceptance.md:17–28`.)

### Set2FAManage (Set2FAManage.dc.html, Set2FAManageNL.dc.html)

Canvas title: Two-factor · on, actions ask for password / Two-factor · on (NL). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/TwoFactorScreen.kt`. Owner phase: 10c. Kind: `phoneWithWideSibling`. Captures: `build/captures/Set2FAManage*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pinned iOS 48 dp actions, raw setup key and real QR replace smaller board controls/placeholder. CfRow uses 64 dp minimum and 14/20 subtitle; the fresh-codes toast is the shared Snackbar. Release omits passkeys while its capability is gated. (source: `docs/settings-security-acceptance.md:17–28`.)

### Set2FAManageWide (Set2FAManageWide.dc.html)

Canvas title: Two-factor · on, wide. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/TwoFactorScreen.kt`. Owner phase: 10c. Kind: `wideOnly`. Captures: `build/captures/Set2FAManageWide*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pinned iOS 48 dp actions, raw setup key and real QR replace smaller board controls/placeholder. CfRow uses 64 dp minimum and 14/20 subtitle; the fresh-codes toast is the shared Snackbar. Release omits passkeys while its capability is gated. (source: `docs/settings-security-acceptance.md:17–28`.)

### Set2FAOn (Set2FAOn.dc.html)

Canvas title: Two-factor · just turned on (codes + toast). Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/TwoFactorScreen.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/Set2FAOn*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pinned iOS 48 dp actions, raw setup key and real QR replace smaller board controls/placeholder. CfRow uses 64 dp minimum and 14/20 subtitle; the fresh-codes toast is the shared Snackbar. Release omits passkeys while its capability is gated. (source: `docs/settings-security-acceptance.md:17–28`.)

### Set2FASetup (Set2FASetup.dc.html)

Canvas title: Two-factor · turn on. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/TwoFactorScreen.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/Set2FASetup*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pinned iOS 48 dp actions, raw setup key and real QR replace smaller board controls/placeholder. CfRow uses 64 dp minimum and 14/20 subtitle; the fresh-codes toast is the shared Snackbar. Release omits passkeys while its capability is gated. (source: `docs/settings-security-acceptance.md:17–28`.)

### Set2FAWide (Set2FAWide.dc.html)

Canvas title: Two-factor · off, wide. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/TwoFactorScreen.kt`. Owner phase: 10c. Kind: `wideOnly`. Captures: `build/captures/Set2FAWide*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pinned iOS 48 dp actions, raw setup key and real QR replace smaller board controls/placeholder. CfRow uses 64 dp minimum and 14/20 subtitle; the fresh-codes toast is the shared Snackbar. Release omits passkeys while its capability is gated. (source: `docs/settings-security-acceptance.md:17–28`.)

### SetPassword (SetPassword.dc.html)

Canvas title: Password · validation error. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/PasswordScreen.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/SetPassword*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)

### SetSecurity (SetSecurity.dc.html)

Canvas title: Security. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/SecurityScreen.kt`. Owner phase: 10c. Kind: `standard`. Captures: `build/captures/SetSecurity*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)

### PasswordPromptDelete (PasswordPromptDelete.dc.html)

Canvas title: Prompt · delete account. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/security/prompt/SettingsPasswordPromptHost.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/PasswordPromptDelete*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Shared native sheet/dialog, 16/24 token title and native Cancel; surface2 field and 18 dp lock. Wide dialog is 540 dp, at most 90% high. Checking/Forgot use Enable, Locked uses Delete with the 2,820 s lockout. Catalog Dutch wording takes precedence. (source: `docs/settings-security-acceptance.md:17–28`, `docs/settings-account-plan-acceptance.md:40–50`, OQ-A502.)
- **Recorded decision:** Shared native sheet/dialog chrome, safe insets and the existing 540 dp wide prompt. (source: `docs/settings-account-plan-acceptance.md:49`.)

### SetDelete (SetDelete.dc.html)

Canvas title: Delete account · confirmation. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/delete/DeleteAccountSheet.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetDelete*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Shared native sheet/dialog chrome, safe insets and the existing 540 dp wide prompt. (source: `docs/settings-account-plan-acceptance.md:49`.)

### SetPasskeys (SetPasskeys.dc.html)

Canvas title: Passkeys. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/passkeys/PasskeysScreen.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetPasskeys*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Generated Android fingerprint/face/screen-lock intro replaces Face ID copy. (source: `docs/settings-account-plan-acceptance.md:45`.)

### SetPasskeysEmpty (SetPasskeysEmpty.dc.html)

Canvas title: Passkeys · empty. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/passkeys/PasskeysScreen.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetPasskeysEmpty*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)

### SetPlan (DarkSetPlan.dc.html, SetPlan.dc.html)

Canvas title: Plan & credits · dark / Plan & credits · unlimited, payment failed. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/plan/PlanScreen.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetPlan*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `36-settings-plan.png`.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Website-free payment-failed sentence; no date in that notice, Billing rows or Action needed badge. (source: `docs/settings-account-plan-acceptance.md:47`.)

### SetPlanCredits (SetPlanCredits.dc.html)

Canvas title: Plan & credits · credits, processing. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/plan/PlanScreen.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetPlanCredits*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Pending-checkout notice, credits hero and NoLink paragraph; no board purchase button, caption or Billing section. (source: `docs/settings-account-plan-acceptance.md:46`.)

### SetPlanLoading (SetPlanLoading.dc.html)

Canvas title: Plan & credits · loading. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/plan/PlanScreen.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetPlanLoading*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)

### SetPlanNoLink (SetPlanNoLink.dc.html)

Canvas title: Plan & credits · where store rules allow no link. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/plan/PlanScreen.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetPlanNoLink*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)

### SetPlanOffline (SetPlanOffline.dc.html)

Canvas title: Plan & credits · offline, cancelled. Screen: `feature/settings/src/main/kotlin/net/chronoflow/feature/settings/plan/PlanScreen.kt`. Owner phase: 10d. Kind: `standard`. Captures: `build/captures/SetPlanOffline*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Cancelled hero and ends line, with the offline paragraph below; no disabled Billing rows. (source: `docs/settings-account-plan-acceptance.md:48`.)

### WizardCheckingGuest (WizardCheckingGuest.dc.html)

Canvas title: Guest · 4 · Checking. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardCheckingGuest*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardDiscardGuest (WizardDiscardGuest.dc.html, WizardDiscardGuestDarkNL.dc.html)

Canvas title: Wizard · Close · guest / Wizard · Close · guest · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardDiscardGuest*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardFineTuneGuest (WizardFineTuneGuest.dc.html)

Canvas title: Guest · 8 · Fine-tune. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardFineTuneGuest*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardOutboundGuest (WizardOutboundGuest.dc.html)

Canvas title: Guest · 2 · Outbound. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardOutboundGuest*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardOutboundPastGuest (WizardOutboundPastGuest.dc.html, WizardOutboundPastGuestDarkNL.dc.html)

Canvas title: Wizard · Outbound · arrival in the past (guest, after Checking) / Wizard · Outbound · arrival in the past (guest, after Checking) · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardOutboundPastGuest*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)

### WizardReadyGuest (WizardReadyGuest.dc.html, WizardReadyGuestDarkNL.dc.html)

Canvas title: Wizard · Ready · guest / Wizard · Ready · guest · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardReadyGuest*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)

### WizardReadyGuestFree (WizardReadyGuestFree.dc.html, WizardReadyGuestFreeDarkNL.dc.html)

Canvas title: Wizard · Ready · guest, free trip / Wizard · Ready · guest, free trip · dark, Dutch. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardReadyGuestFree*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** Pinned summary arguments format 420 min, and Extras counts actual answered prompts (7, or 11 for the multi-flight return), rather than board sample hours/counts. Edit seeds have an empty Sleep value while runtime falls back to the profile. Server price/balance controls the price block; no website purchase action under A14. (source: `docs/wizard.md:1203–1220`, `docs/wizard.md:1400–1419`.)

### WizardWhereGuest (WizardWhereGuest.dc.html)

Canvas title: Guest · 1 · Where. Screen: `feature/wizard/src/main/kotlin/net/chronoflow/feature/wizard/shell/WizardScreen.kt`. Owner phase: 11a. Kind: `standard`. Captures: `build/captures/WizardWhereGuest*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Native header actions, progress, sheets/dialogs and safe insets replace simulated iOS chrome. Model progress, server question wording and generated catalog answers override illustrative wrapper percentages/copy. The content scrolls above a fixed footer; 1.3 capped text can wrap and Ready labels stack. (source: `docs/wizard.md:28–51`, `docs/wizard.md:909–918`, `docs/wizard.md:1148–1162`.)
- **Recorded decision:** The pinned no-health-data audience notice is below the route card; the raw board omits it. Only age band and shift work are collected. The additional copy can move stop controls below the initial large-font viewport; they remain in the scrolling content. (source: `docs/wizard.md:45`, OQ-A374 / OQ-A375.)

### GuestExcluded (GuestExcluded.dc.html)

Canvas title: Guest · no plan available. Screen: `unbuilt`. Owner phase: 11b. Kind: `notBuilt`. Captures: not required (see kind/reason); exact cells/files in the coverage report. iOS reference: none.

Checks: Source specification/scope and the named acceptance evidence; no fabricated screen capture.

- **Recorded decision:** Retired health-answers screen; iOS OQ 47 and OQ-A544 leave it unbuilt. Guests use WizardExcluded. (source: `tools/audit/board-scope.json`, phase 11b, OQ-14-7.)

### GuestSignup (GuestSignup.dc.html)

Canvas title: Guest · save asks for an account. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/guest/GuestSignupSheet.kt`. Owner phase: 11b. Kind: `standard`. Captures: `build/captures/GuestSignup*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** The live backdrop is guest Ready, replacing retired GuestReview. JVM captures isolate the sheet window and do not evidence its backdrop. The 12-character server policy replaces the board's 10-character hint. Native handle slot/color, 48 dp fields/actions and 38 dp corners are retained; no generated token matches the board grabber color. Review disables creation and gives its banner action a 48 dp row. (source: `docs/wizard.md:1643–1657`.)

### GuestSignupFailed (GuestSignupFailed.dc.html)

Canvas title: Guest sign-up failed. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/guest/GuestSignupSheet.kt`. Owner phase: 11b. Kind: `standard`. Captures: `build/captures/GuestSignupFailed*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** The live backdrop is guest Ready, replacing retired GuestReview. JVM captures isolate the sheet window and do not evidence its backdrop. The 12-character server policy replaces the board's 10-character hint. Native handle slot/color, 48 dp fields/actions and 38 dp corners are retained; no generated token matches the board grabber color. Review disables creation and gives its banner action a 48 dp row. (source: `docs/wizard.md:1643–1657`.)

### GuestSignupRetry (GuestSignupRetry.dc.html)

Canvas title: Guest sign-up · account made, trip not saved yet. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/guest/GuestSignupSheet.kt`. Owner phase: 11b. Kind: `standard`. Captures: `build/captures/GuestSignupRetry*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** The live backdrop is guest Ready, replacing retired GuestReview. JVM captures isolate the sheet window and do not evidence its backdrop. The 12-character server policy replaces the board's 10-character hint. Native handle slot/color, 48 dp fields/actions and 38 dp corners are retained; no generated token matches the board grabber color. Review disables creation and gives its banner action a 48 dp row. (source: `docs/wizard.md:1643–1657`.)

### GuestSignupReview (GuestSignupReview.dc.html)

Canvas title: Guest sign-up · trip needs a new check. Screen: `feature/auth/src/main/kotlin/net/chronoflow/feature/auth/guest/GuestSignupSheet.kt`. Owner phase: 11b. Kind: `standard`. Captures: `build/captures/GuestSignupReview*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** The live backdrop is guest Ready, replacing retired GuestReview. JVM captures isolate the sheet window and do not evidence its backdrop. The 12-character server policy replaces the board's 10-character hint. Native handle slot/color, 48 dp fields/actions and 38 dp corners are retained; no generated token matches the board grabber color. Review disables creation and gives its banner action a 48 dp row. (source: `docs/wizard.md:1643–1657`.)

### SetReminders (DarkSetReminders.dc.html, SetReminders.dc.html)

Canvas title: Reminders · dark / Connectivity & reminders · on. Screen: `feature/reminders/src/main/kotlin/net/chronoflow/feature/reminders/RemindersScreen.kt`. Owner phase: 12c. Kind: `standard`. Captures: `build/captures/SetReminders*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: `35-settings-reminders.png`.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Native large app bar and icon back button replace the Fraunces page title and labelled iOS back button. Native dawn-tinted switch and joined M3 segmented buttons replace the drawn switch and separate pills. Live Activity and practice cards are omitted (A24). Window and safe insets determine the geometry. (source: `docs/reminders-ui.md:27`.)
- **Recorded decision:** Same native app bar, window geometry and omitted cards; only the header and unsupported notice remain, as specified for this status. (source: `docs/reminders-ui.md:28`.)
- **Recorded decision:** Same native app bar, omitted cards and native controls as the on state; native selected fill/shared borders replace the outlined board pills. All dark colors come from the generated theme. (source: `docs/reminders-ui.md:31`.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

### SetRemindersAsk (SetRemindersAsk.dc.html)

Canvas title: Reminders · permission not asked yet. Screen: `feature/reminders/src/main/kotlin/net/chronoflow/feature/reminders/RemindersScreen.kt`. Owner phase: 12c. Kind: `standard`. Captures: `build/captures/SetRemindersAsk*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Same native app bar, window geometry and omitted cards. The request stays behind the designed 48 dp action. (source: `docs/reminders-ui.md:29`.)

### SetRemindersBlocked (SetRemindersBlocked.dc.html)

Canvas title: Reminders · permission blocked. Screen: `feature/reminders/src/main/kotlin/net/chronoflow/feature/reminders/RemindersScreen.kt`. Owner phase: 12c. Kind: `standard`. Captures: `build/captures/SetRemindersBlocked*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Title/readiness and row/card order, catalog/type/color roles, separators, action availability, native control state, Dutch wrapping and wide centering.

- **Recorded decision:** Native system large title/back and bar/rail occupy layout space; settings content stays centered within its 560 dp cap. Generated catalog labels, native controls and the existing scroll reachability rules are retained. (source: `plan.md:357–371`, A9, `docs/settings-home.md:21–37`.)
- **Recorded decision:** Same native app bar, window geometry and omitted cards. Four iOS instruction rows become two Android rows using the existing blocked-step copy. The disabled native switch uses system disabled colors rather than a CSS opacity. (source: `docs/reminders-ui.md:30`.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

### WizardReminders (WizardReminders.dc.html, WizardRemindersDarkNL.dc.html)

Canvas title: Reminders sheet · after the first saved trip / Reminders sheet · after the first saved trip · dark, Dutch. Screen: `feature/reminders/src/main/kotlin/net/chronoflow/feature/reminders/FirstTripRemindersSheet.kt`. Owner phase: 12c. Kind: `standard`. Captures: `build/captures/WizardReminders*_<profile>_<lang>_<theme>.png`; exact cells/files in the coverage report. iOS reference: none.

Checks: Selected master screen/v/guest branch, title/subtitle/banner order, model progress, cards/options/pills, fixed actions and scrolling; native windows, Dutch/large-font wrapping and wide column.

- **Recorded decision:** Content capture omits the underlying trip and scrim. The native drag handle and its system hit area replace the board's 36 × 5 grabber/padding. Body is the nearest generated 16/24 style rather than 16/23; title is the board's 24/30. Android safe insets replace the 34 px iPhone home-indicator padding. Production uses the M3 modal window and scrim. (source: `docs/reminders-ui.md:32`.)
- **Recorded decision:** Same sheet differences. Catalog “Nu niet” replaces board “Niet nu”. Dark colors and the 16% bell tint match the generated theme/board values. (source: `docs/reminders-ui.md:33`.)
- **Needs a device:** Manual TalkBack focus/announcement or native-window/permission behavior remains pending (orchestrator/user); automated bounds/captures do not prove a manual service pass. See the accessibility and verification procedures.

## iOS parity (Task 4)

Pending Task 4: all 56 references and the launcher entry will be listed with their actual capture/pair or a reproducibility reason. No device parity result is claimed by the JVM board matrix.

## Not built

| Group | Phase | Reason |
|---|---|---|
| GuestExcluded | 11b | Retired health-answers screen; iOS OQ 47 and OQ-A544 leave it unbuilt. Guests use WizardExcluded. |
| PolicyReader | 8 | No in-app policy-reader screen is built: PolicyLinks opens the external policy URL in a native Custom Tab (OQ-A32/OQ-A339/OQ-A346). The board draws an in-app reader that the pinned iOS/landed Phase 8 scope does not build. External browser content and chrome need device review. |

## Accessibility (Tasks 7–11)

Pending automated audit tier, keyboard, contrast and device font-scale evidence. Manual TalkBack, Switch Access and hardware-keyboard rows remain **pending (orchestrator/user)** until observed; no pass is inferred from captures. Accepted Welcome/sign-in-guest contrast and native disabled controls follow the exact policy in brief §5.5.

## Robustness (Tasks 13–15)

Pending process-death/StrictMode/resize tooling and recorded runs. Fold/T2 and performance tiers belong to the orchestrator. Calculating save resume is not swept on device because the mock delay is shorter than the kill procedure; the existing WizardProcessDeathTest is the required model/Robolectric evidence (OQ-14-5).
