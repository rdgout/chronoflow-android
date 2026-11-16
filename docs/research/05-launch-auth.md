# 05 — Launch, splash, Welcome, walkthrough, auth and guest sign-up

Research for the native Android app (Kotlin + Jetpack Compose). Date: 2026-11-15.

**Legend.** **[V]** = verified in source (path and line given). **[R]** = recommendation for Android.
**[OQ n]** = open question, listed in §17 with the default the plan proceeds on.

Path prefixes: `iOS` = `/Users/rickgout/Sites/chronoflow-ios`, `Kit` = `iOS/Packages/ChronoFlowKit/Sources`,
`RN` = `/Users/rickgout/Sites/chronoflow-mobile`, `W` = `/Users/rickgout/Sites/chronoflow-new` (read on branch
`main` with `git show`/`git grep`; the working tree is on `feat/mobile-api-passkey-management`),
`C` = `/Users/rickgout/Sites/chronoflow-android/_design/canvas` (canvas version 1794774555-3fd6).

---

## 0. Short answer: what the iOS work gives Android for this area

Most of it transfers. The iOS plan has settled the behaviour, numbers and copy for launch, Welcome and auth.
Android needs new platform mechanics, not new decisions.

| Transfers as-is (port, keep the same numbers and tests) | Needs an Android replacement |
|---|---|
| Splash geometry for any `w × h` and the composition-centring rule (iOS `plan.md:188-203`, S1 at `:92`) | Core Animation layer tree in its own `UIWindow` (D3/D4). Android: Compose canvas overlay plus the SplashScreen API (§15.1–15.2) |
| Opening timeline, three endings, curves, Welcome intro offsets (`plan.md:205-256`) | `LaunchScreen.storyboard` (D7). Android: `Theme.SplashScreen` with the same moon PNG |
| Readiness rules 1–7 and the pure reducer (`Kit/AppCore/LaunchReadiness.swift`, 190 lines) | Keychain session store (D21). Android: Keystore-wrapped storage |
| Gate order verify → terms → walkthrough → app (`Kit/LocalStore/AccountReadinessRepository.swift:13-25`) | AuthenticationServices passkeys. Android: Credential Manager (§15.7) |
| Auth geometry, tokens, typography, banner rules (`plan.md:471-503`, `:630`) | Universal links + AASA. Android: App Links + `assetlinks.json` (server work, §15.6) |
| Failure → banner/field mapping (`Kit/Auth/AuthFormModel.swift:1-90`) | Dynamic Type xxLarge cap. Android: a font-scale cap (§15.9) |
| All en/nl copy with settled Dutch (OQ 21/22/27/28), §16 | `.xcstrings` English-as-key. Android `strings.xml` needs IDs (§16.0) |
| Link parsing for reset/verify URLs (`Kit/AppCore/AuthLink.swift`) and the routing rules (reset waits for sign-out; verify inbox per account) | `scrollDismissesKeyboard`, `AuthKeyboardReveal` (UIKit). Android: `imePadding` + `BringIntoViewRequester` |
| API contracts, error envelope, timeouts (`plan.md:435-455`) | Liquid Glass (D1b), iPhone Duo specifics (D1c) |
| Guest wizard claim rules (Phase 18, `plan.md:2062-2210`) | `NSUserActivity`/`SceneDelegate` link capture |
| Settled OQs: 10 (xxLarge cap), 20 (passkeys), 21 (server password policy), 22/27 (honest reset copy), 23 (offline banner), 25 (links open the app), 28 (Dutch source order), 29 (sign-out confirm), 30 (hidden-field banner), 33 (accepted board contrast), 39 (walkthrough), 47 (GuestSignup sheet) | — |

The boards in the current canvas are **older in places than the iOS decisions**: they still draw stars and
dot patterns, the 10-character password hint, the auto-sign-in reset copy and the stale `GuestFlight` link.
The iOS decisions win (§2.2). Android follows the iOS decisions, not the raw boards.

---

## 1. Sources read

- Boards [V]: `C/SplashSpec`, `SplashScreen`, `SplashSlow`, `SplashWelcome`, `Main`, `Walkthrough`, `Walkthrough2`,
  `Walkthrough3`, `WalkthroughReplay`, `AuthSignIn`, `AuthSignInError`, `AuthSignUp`, `AuthTwoFactor` (8 `screen`
  variants), `AuthForgot`, `AuthReset`, `AuthResetDone`, `AuthResetExpired`, `AuthVerify`, `AuthVerifyFailed`,
  `AuthEmailChanged`, `AuthStates`, `GuestSignup` (+ `Failed`/`Retry`/`Review`), `GuestExcluded`, `GuestStates`,
  `GuestFlight/Sleep/Health/Review` (all marked "superseded" in `canvas.json`), `PasswordPrompt*` (11 boards),
  `TermsUpdate`, `TermsFailed`, `PolicyReader`, `ErrorNotFound`, `ErrorServer`, `SignOutPending`,
  `SignOutPendingSettings`; `C/theme.css`; `C/canvas.json` notes `gapAuth`, `gapPages`, `reqSignup`, `reqTerms`,
  `decErrors`, `reqPasswordPrompt`, `reqPasswordLockLine`, `wiz4Rick`, `wiz24cCopy`.
- iOS [V]: `iOS/plan.md` (spec `:137-360`, auth spec `:362-630`, Phases 2–5, 9d, 10–11, 17f/g, 18, OQ table
  `:2376-2426`), `iOS/Docs/phase-17f-walkthrough.md`, `phase-17g-terms.md`, `auth-gate.md`; sources
  `Kit/AppCore/{LaunchReadiness,Route,AuthLink,PendingLaunchIntent,SessionGateRouter}.swift`,
  `Kit/Auth/*.swift` (29 files), `Kit/Settings/{TermsUpdateView,SettingsPasswordPrompt,SignOutPendingSheet}.swift`,
  `iOS/App/SceneDelegate.swift:655-695`; catalogs `Kit/{Launch,Welcome,Auth}/Resources/Localizable.xcstrings`,
  `Kit/PlanUI/Resources/Walkthrough.xcstrings`, `Kit/Settings/Resources/Terms.xcstrings`.
- RN [V]: `RN/plan.md` Phase 18 (`:1039-1090`), Phase 21 (`:1149-1167`), Phase 23a (`:1195-1214`);
  `RN/android/app/src/main/AndroidManifest.xml`, `res/values*/{styles,colors}.xml`, `res/drawable-*/splashscreen_logo.png`;
  `RN/src/config/web.ts`.
- Web [V]: `W/routes/api.php:46-114`; on `main`: `app/Providers/FortifyServiceProvider.php:78-99`,
  `app/Http/AppUrls.php`, `app/Providers/AppServiceProvider.php:108-207`, `config/fortify.php:149-150`,
  `bootstrap/app.php:49-50`, `.ai/rules/api.md:73`, `.ai/rules/deploy.md:15`,
  `docs/ui-plan/phase-18-mobile-api-passkeys.md:11-12,134`.

---

## 2. Screen and state inventory

### 2.1 Every screen in scope [V boards; iOS status from plan]

| Board(s) | States | iOS status | Android |
|---|---|---|---|
| System launch (SplashSpec "Platforms") | moon on sky-top, light/dark | Storyboard (Phase 2) | `Theme.SplashScreen` (§15.1) |
| SplashScreen `mode=ready` | opening 0–1500, lift-away 1500–1900 | Done (3a–4) | Compose overlay (§15.2) |
| SplashScreen `mode=slow` / SplashSlow | status line at 2000, dots, hand-off when ready, forced at 8000 | Done | same |
| SplashScreen `mode=welcome` / SplashWelcome | moon flight 1600–2300, dissolve to 2550 | Done | same |
| Main (Welcome) `intro=true/false` | intro stagger 2300–3200; plain on later visits | Done (Phase 5) | Compose screen |
| Walkthrough 1/2/3, WalkthroughReplay | first run → Today; replay → Preferences | Done (17f), replay entry deferred | port |
| TermsUpdate / TermsFailed | ready, failed (local save failed) | Done (17g) | port |
| SignOutPending / SignOutPendingSettings | sheet: Stay / Sign out and discard | Done (17e/17g) | M3 `ModalBottomSheet` |
| PolicyReader | placeholder legal text | **Not built**; system browser instead | Custom Tab (OQ A6) |
| AuthSignIn (+Error) | none / wrong / locked / server (+ offline, per settled OQ 23) | Done (10a) | port |
| AuthTwoFactor `twofactor` | code / recovery × none / field error / attempts / server | Done (10b) | port |
| AuthTwoFactor `forgot` (AuthForgot) | form / sent notice / throttled / server | Done (11a) | port |
| `reset` (AuthReset), `resetExpired`, `resetDone` | form / field errors / server; expired; done | Done (11b) | port |
| AuthSignUp | none / taken / server / field errors | Done (11c) | port |
| `verify` (AuthVerify), `verifyFailed` | waiting (countdown), failed | Done (11d) | port |
| `changed` (AuthEmailChanged) | email-change confirmation | **Out of scope** on iOS (D27) | out of scope |
| AuthStates | banner catalogue + placement rule | Done | port |
| GuestSignup (+Failed/Retry/Review) | ready / failed / retry / review sheet over WizardReadyGuest | Done (18i–18l) | port |
| GuestExcluded | health-answers exclusion | Not imported; retired by "no health data" (`plan.md:2201-2204`) | do not build |
| GuestFlight/Sleep/Health/Review/GuestStates | superseded guest flow (`canvas.json` titles) | Not built | do not build |
| PasswordPrompt (+9 variants, Wide) | email/delete/2FA-on/2FA-off/new codes/show codes × empty/checking/wrong/locked; forgot alert | Done in Settings (`Kit/Settings/SettingsPasswordPrompt.swift`) | Settings doc; summary §13 |
| ErrorNotFound / ErrorServer | 404 / 500-503 | Used only on the trip page for a failed remote plan fetch (`iOS/plan.md:1417,1695`) | not used in launch/auth |

### 2.2 Board values that the iOS decisions override [V]

Android must follow the right-hand column.

| Board says | Settled value | Source |
|---|---|---|
| Stars (`pattern-dots`, opacity 0.55 → 0 → 0.2) in SplashScreen; dots at 0.2 on Main, Auth* | **No stars, no dots anywhere.** Plain `cf-sky` only | User 2026-11-06, `iOS/plan.md:901-906`, S2 `:94` |
| Moon at board y 392 (30 above centre) | Moon centred in the window | S1 `:93` |
| Ready hand-off keyframes 1700–2100 | 1500–1900 (prose rule) | S3 `:95` |
| Moon flight ease-out | ease-in-out | S4 `:96` |
| Path `stroke-dasharray 560` (ends at 1313 ms) | `strokeEnd` = the light's arc-length fraction | S5 `:97` |
| Welcome stagger "about 100 ms" | 2300/2400/2450/2600/2700/2750 | S6 `:98` |
| Bottom-anchored `h − k` geometry | `cy + (422 − k)` everywhere (composition centred) | User 2026-11-06, `:892-899` |
| Password hint "At least 10 characters." | "Use at least 12 characters, with uppercase and lowercase letters, numbers and symbols. Choose a password that has not appeared in a data leak." | OQ 21, `:2398` |
| Reset sub "You'll be signed in on this phone once it's saved."; button "Save and sign in" | "Sign in with your new password once it's saved."; "Save new password"; no session from reset | OQ 22/27 |
| Reset done "You're signed in on this phone…", "Other devices stay signed in." | "Password updated. Sign in with your new password." / "Other devices are signed out. If you didn't make this change, reset your password again and contact us."; Continue → Sign in | OQ 22/27 |
| Two-factor sample wrong code shown for every error | Real server key; the 30-second copy only for "The authentication code was not accepted. Try again." | `Kit/Auth/AuthFormModel.swift:69-73` |
| Sign-up consent: one link "terms and privacy policy" → PolicyReader | Two links `<terms>`/`<privacy>` to `https://chronoflow.net/terms` and `/privacy` | `Kit/Auth/SignUpModel.swift:134-137` |
| Sample prefilled credentials (`rick@example.com`, `password12`) | All forms start empty | `plan.md:498` |
| Main "Plan my trip" → `GuestFlight` | `.guestWizard` → WizardWhereGuest | OQ 12, `:2387` |
| Verify back → `GuestSignup` | Back / "Use a different email" = explicit sign-out with discard | `plan.md:501` |
| Walkthrough Skip/Done → `Today.dc.html` / `SetPreferences.dc.html` | First run → Today; replay → Preferences | `canvas.json` note `gapPages`; 17f doc |
| TermsUpdate placeholders `[effective date]`, `[Change one]…` | iOS **code** ships RN's generic copy (see §7) | `Kit/Settings/TermsUpdateView.swift:66-98` |

---

## 3. Launch: system splash, animated splash, readiness

### 3.1 Coordinates and frame 0 [V `iOS/plan.md:137-139,188-203`]

- Board frame 390 × 844. `w`, `h` = window size, `cy = h / 2`. Every board `h − k` becomes `cy + (422 − k)`.
  All times are ms from `t0` = the moment the first overlay frame is committed.
- **Frame 0 = the system launch frame:** sky-top colour, moon image `LaunchMoon` 288 × 288 at its natural size,
  centred on the window (not the safe area), pixel-aligned. No text. Same in en and nl.
- Moon artwork: Lucide moon `M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z`, viewBox 24, glyph 104 pt, stroke 1.6, round
  caps/joins, colour `dawn`; glow dawn α0.45, Gaussian σ 9 (CSS `drop-shadow(0 0 18px …)`), baked into the image.
- RN already produced the Android form [V `RN/plan.md:1153,1164`]: `splashscreen_logo.png` 288 px mdpi → 1152 px
  xxxhdpi, light and night variants; "`imageWidth: 288` keeps the 104 pt moon and glow inside Android's 192 dp mask".
  Splash background colours [V `RN/android/.../values/colors.xml`, `values-night/colors.xml`]: light `#1f285a`,
  dark `#06091c` — identical to the iOS skyTop tokens (`plan.md:145`).

### 3.2 Layer tree and geometry for any `w × h` [V `iOS/plan.md:188-203`]

Container = splash root: full window, ignores insets, clips, background skyTop, transform origin (0.5, 0.4).
Children in order:

| Layer | Geometry and paint |
|---|---|
| sky | `cf-sky` vertical gradient, full window: skyTop 0 %, skyMid 55 % light / 60 % dark, skyHorizon 100 %, interpolated in **OKLab** (`plan.md:158-165`); opacity animated |
| halo | radial, centred on the sun: approximates `box-shadow 0 0 80px 20px dawn α0.45` (disc r 110, blur σ 40), **alpha 0 for r < 90** |
| sun | 180 × 180 circle, centre `(w/2, cy + 228)`. Radial dawn: solid to r 70, α0 at r 91.6 (gradient radius 127.3). On short windows only its bottom may leave the window; sides inside horizontal safe bounds, top inside the window, ≥ 1/3 of its height visible |
| band | top `cy + 48`, to the window bottom; skyHorizon at α 0 / 0.22 / 0.70 / 1.0 at stops 0 / 0.40 / 0.75 / 1.0; drawn above the sun |
| path | cubic `(−10, cy+218)` → c1 `(90·w/390, cy+48)`, c2 `(300·w/390, cy+48)` → `(w+10, cy+218)`; stroke 1.5, butt caps, white α0.55 |
| plane | 8 pt white dot + glow disc r 6, blur σ 5, white α0.8; follows the path by arc length, no rotation |
| moon | `LaunchMoon` 288 × 288, centre `(w/2, cy)` |
| wordmark | baked "ChronoFlow" image: Fraunces 400 opsz 32, 32/40, tracking −0.32, skyTitle; box top `cy + 40`, height 40, centred |
| status | top `cy + 96`, height 20, centred: "Getting your plan ready" Figtree 400 14/20 skyTitle + three 4 pt dots (gap 3, margin-left 6) |

On the 844 board this gives wordmark top 462, band top 470, path apex 512.5, status top 518 (`plan.md:93`).

### 3.3 Shared opening (every mode) [V `iOS/plan.md:205-219`]

| Element | Property | Keys (ms: value) | Easing |
|---|---|---|---|
| Sky | opacity | 300: 0 → 1100: 1 | ease-out |
| Band | opacity | 300: 0 → 1100: 1 | ease-out |
| Sun + halo | translateY, opacity | 400: +110, 0 → 1300: 0, 1 | ease-out |
| Path | strokeEnd | 500: 0 → 560: 0.03 → 1400: 1 | linear |
| Plane | arc-length fraction | 500: 0 → 560: 0.03 → 1400: 1 | linear |
| Plane | opacity | 500: 0 → 560: 1; 1400: 1 → 1500: 0 | linear |
| Moon | translateY, scale | 300: 0, 1 → 1100: −28, 0.86 | ease-out |
| Wordmark | opacity, translateY | 1000: 0, +8 → 1400: 1, 0 | ease-out |

Animation done at 1500. Plane position: 128 arc-length samples; `strokeEnd` uses the same fraction.
Curves (`plan.md:223`): ease-out `(0, 0, 0.58, 1)`, ease-in-out `(0.42, 0, 0.58, 1)`, intro `(0.2, 0.8, 0.2, 1)`;
`cf-press` `(0.25, 0.1, 0.25, 1)`.

### 3.4 Endings [V `iOS/plan.md:221-241`]

- **ready** (signed in, ready): `E = max(1500, readyAt)`. E → E+400: container opacity 1 → 0, scale 1 → 1.08,
  origin (0.5, 0.4), ease-out. Known before 1500 → committed at 1500.
- **slow** (signed in, `hasStoredData == false`, not ready at 2000): status opacity 0 → 1 over 2000–2300 ease-out,
  latched until hand-off ends. Dots loop 1200 ms ease-in-out: 0.25 at 0 → 1 at 480 → 0.25 at 960, held to 1200;
  delays 0/200/400; started at overlay mount. Hand-off as `ready` from readyAt; forced at 8000.
- **welcome** (signed out): `W = max(1600, endingKnownAt)`.

  | Element | Change | ms | Easing |
  |---|---|---|---|
  | Band | opacity 1 → 0 | W → W+700 | ease-out |
  | Sun + halo | translateY 0 → +120, opacity 1 → 0 | W → W+600 | ease-out |
  | Path | opacity 1 → 0 | W → W+400 | linear |
  | Wordmark | opacity 1 → 0, translateY 0 → −6 | W → W+300 | ease-out |
  | Moon | centre `(w/2, cy − 28)` → measured target centre; scale 0.86 → target.width / 104 (board 0.27); one straight line | W → W+700 | ease-in-out |
  | Container | opacity 1 → 0, no scale | W+700 → W+950 | ease-out |

  Overlay removed at W+950, on the animation's own completion. The flight target comes from Welcome's measured
  moon glyph frame; rebuilt if it changes before W; fallback (if Welcome has not laid out by W − 50) centre
  `x = 24 + 14`, `y = max(safeTop + 16, 64) + rowHeight / 2`, `rowHeight = max(28, wordmark line height at the
  capped size)` (`plan.md:265`, `:101-106`).

### 3.5 Readiness rules [V `iOS/plan.md:267-275`; reducer `Kit/AppCore/LaunchReadiness.swift:107-189`]

1. The ending is chosen once, the first time the session is known: signed out → `welcome`, else `app`.
2. Ready = signed out, or signed in + gate resolved + (gate ≠ `app` or `hasStoredData == true`).
3. Never cut short: `app` needs ready (or 8 s) and t ≥ 1500; `welcome` starts at W. Reduce Motion skips the wait
   (`exitStart = now`).
4. Status shows when t ≥ 2000, `hasStoredData == false` (not unknown) and not ready; latched until hand-off ends.
   Cancelled if ready arrives before 2000.
5. At 8000 readiness is forced: `allowFirstLoad()` scoped to the current session (never fakes `synced_at`); if the
   session is still unknown → signed out → welcome (fixes the RN hang, OQ 7).
6. The hand-off happens exactly once.
7. A cold-start link or notification is captured (`PendingLaunchIntent`) and **never changes the ending**; it is
   consumed after the overlay is gone (`iOS/App/SceneDelegate.swift:532,679-695`).

`hasStoredData` = the account row's `synced_at != nil` (set after the first full pull). Verify/terms/walkthrough
are ready without it. Forced load uses `firstLoadGate`: walkthrough if not seen, else app
(`Kit/LocalStore/AccountReadinessRepository.swift:28-30`). SplashSpec's rule "Never wait for the network" [V
`C/SplashSpec.dc.html` "Loading while it plays"] is the reason readiness is a local DB read only.

Port the reducer as-is: pure Kotlin `reduce(state, event, reduceMotion): List<LaunchEffect>` with events
`Session`, `Gate`, `StoredData`, `MoonAnchor`, `Reached(t)`, `ExitCompleted` and effects `CommitStatus(at)`,
`CancelStatus`, `CommitExit(ending, at, moonTarget)`, `ForceReady` [R]. Port the iOS test list verbatim
(`plan.md:770` Phase 4 "Tests:").

### 3.6 Reduce Motion [V `iOS/plan.md:277-283`]

Frame: skyTop + moon only, no lift, no wordmark. Hand-off when ready: 300 ms container fade, ease-out, no scale;
no Welcome intro. Slow: status appears with a 300 ms fade; dots hold at opacity 1.0 (OQ 6 default). `cf-press`
becomes opacity 0.85 with no scale and no transition (`C/theme.css:142-146`).

### 3.7 Appearance, status bar, accessibility [V `iOS/plan.md:285-294`]

- The splash follows the phone's appearance **captured once** at launch. Welcome follows the system.
- Status-bar content light until the hand-off ends.
- The overlay is one image element "ChronoFlow is opening"; the app below is hidden from accessibility until the
  hand-off ends. The status line is announced once, queued. After teardown, focus moves to Welcome's first element.
  No haptics.
- The overlay mounts only on the **first** connection of the process (D14); every cold process start gets the
  splash, reconnects do not replay it (OQ 16).

### 3.8 Launch copy [V `Kit/Launch/Resources/Localizable.xcstrings`]

| en | nl |
|---|---|
| ChronoFlow is opening | ChronoFlow wordt geopend |
| Getting your plan ready | Je plan wordt klaargezet |

### 3.9 Performance budget to carry over [V `iOS/plan.md:2286-2302`]

Hand-off ends at 1900 ms (ready) ± 1 frame; Welcome interactive at 2550 ms; last intro item lands at 3200 ms;
first frame → first animated change exactly 300 ms; no dropped frames 0–3.2 s, also under a 500 ms main-thread
stall; signed-out session known ≤ 50 ms after launch. Android equivalents in §15.15.

RN lessons that explain the budget [V `iOS/plan.md:7-13`]: RN's overlay started only after the JS bundle loaded,
fonts loaded at runtime, readiness was a sequential chain, hand-off points were JS timers, five full-screen SVG
layers, per-frame bezier allocation, and the moon landing depended on a JS layout fallback with a fixed
390 × 844 offset.

---

## 4. Welcome (Main board)

### 4.1 Layout [V `C/Main.dc.html:30-46`; `iOS/plan.md:258-265`]

- Background: one `cf-sky` gradient; no dots.
- Padding: top `max(safeTop + 16, 64)`, sides 24 (+ horizontal insets), bottom `max(safeBottom, 40)`. Column with
  space-between inside a scroll view.
- Top group, gap 16: moon row (Lucide moon 28 × 28, stroke 2, dawn, no glow; gap 8; wordmark Fraunces 20/28,
  −0.20 tracking, skyTitle), eyebrow (margin-top 56; Figtree 600 12/16, +1.2 tracking, uppercase, dawn),
  headline (Fraunces 400 48/48, −0.48, skyTitle, heading semantics).
- Bottom group, gap 12: copy card (Figtree 16/24, skyCopy on skyCopyBackground α0.9, padding 16/20, radius 22,
  margin-bottom 12), "Plan my trip" (52 high pill, dawn / dawnForeground, Figtree 600 16), "Sign in" (52 high pill,
  skyCopyBackground / skyCopy, Figtree 500 16). Both with `cf-press`.
- Board geometry on 390 × 844: moon row y 64–92, eyebrow 164–180, headline from 196, "Plan my trip" 688–740,
  "Sign in" 752–804.
- Reading order: wordmark, eyebrow, headline, copy, Plan my trip, Sign in.

### 4.2 Intro (first Welcome of the process only) [V `iOS/plan.md:243-256`; `C/Main.dc.html:22-27`]

Each item: opacity 0 → 1, translateY +14 → 0, 450 ms, intro curve `(0.2, 0.8, 0.2, 1)`, offsets from W+700:
wordmark text +0 (2300), eyebrow +100 (2400), headline +150 (2450), copy +300 (2600), Plan my trip +400 (2700),
Sign in +450 (2750). The Welcome moon glyph never animates; it is revealed by the dissolve. Later Welcome visits
(after sign-out, back from Sign in) show the final screen.

### 4.3 Actions

"Plan my trip" → guest wizard (`.guestWizard`, WizardWhereGuest). "Sign in" → AuthSignIn. Quiet guest recovery:
a live guest draft < 30 min opens the wizard on its step **after** the hand-off and intro finish, never during the
moon flight (`iOS/plan.md:2178-2181`, OQ 17).

### 4.4 Copy [V `Kit/Welcome/Resources/Localizable.xcstrings`]

| en | nl |
|---|---|
| ChronoFlow | ChronoFlow |
| Jet-lag planner | Jetlagplanner |
| Plan your clock shift for your next trip | Plan de verschuiving van je biologische klok voor je volgende reis |
| Tell us your flights and how you sleep. You get a day-by-day plan for sleep, light and caffeine that helps your body clock shift. | Vertel ons welke vluchten je neemt en hoe je slaapt. Je krijgt een dagplan voor slaap, licht en cafeïne dat je biologische klok helpt verschuiven. |
| Plan my trip | Plan mijn reis |
| Sign in | Inloggen |

### 4.5 Contrast items still open on iOS [V `iOS/plan.md:833-837`]

Five Welcome pairs (eyebrow, headline, copy, signIn, plan) fail the platform audit at board values; no corrective
design exists. Android keeps board colours and records the same exceptions (OQ A10).

---

## 5. Routing after launch and after auth

[V `Kit/AppCore/Route.swift`; `Kit/Auth/AuthNavigation.swift:37-42`; `Kit/AppCore/SessionGateRouter.swift`]

- Routes: `welcome, today, signIn, guestWizard, terms, verify, walkthrough, trips, settings`.
- After a session is accepted: `!hasVerifiedEmail` → verify; `acceptedTermsVersion != termsVersion` → terms;
  `!walkthroughSeen` → walkthrough; else Today. At launch the same order comes from SQLite, and a pending
  walkthrough outbox row counts as seen (`AccountReadinessRepository.swift:13-25`).
- Gate changes are observed: verifying email or accepting terms advances the route without a restart.
- Sign-out (voluntary or forced 401) → **Sign in directly**, no splash, no Welcome intro (`SessionGateRouter.swift:27-31`).
- SplashSpec "Lands on": Today, or the screen a notification or link opened; sign-in, terms and walkthrough come
  after the splash when needed [V `C/SplashSpec.dc.html`].

---

## 6. Walkthrough

[V `C/Walkthrough.dc.html`; `iOS/Docs/phase-17f-walkthrough.md`; `Kit/PlanUI/Resources/Walkthrough.xcstrings`]

- Board 375 × 812, background `background`. Padding 8 / 16 / 32. Header 48 high: counter "1 of 3" (Figtree 600
  12/16, +0.1em, uppercase, dawnText, tabular) and Skip (44 high pill, 14/20 500).
- Content centred vertically, gap 28: preview card (padding 20, radius 26, 1 px border, card, shadow-soft, gap 10;
  decorative, hidden from accessibility) and text (title Fraunces 30/36, body 16/24 muted, gap 10, inset 4).
  Content region is `aria-live="polite"`.
- Page 1 preview: four TimelineBlocks (Sleep 23:00–07:00; Seek bright light 08:00–10:00; 1 cup of coffee 09:30;
  KL 861 14:40–09:15). Page 2: five category swatches with labels (Sleep, Seek bright light, Avoid light, Caffeine,
  Flight), rows ≥ 44. Page 3: DayProgress 3/7 + two TodayRows.
- Footer gap 20: page dots (8 high; active 24 wide dawn, others 8 wide border; gap 8; width transition 150 ms
  ease, disabled under Reduce Motion), Next (Button lg, 40 high) or Done on page 3 (primary pill 40 high).
- Behaviour [V iOS 17f]: Next/Done and Skip only, **no swipe** (boards do not draw swipe). Skip and Done write the
  local `walkthrough_seen` flag and one `walkthrough` outbox row in one transaction, then the gate opens Today;
  never waits for the network (`POST walkthrough` later; a 401 on it keeps the row). Replay mode (from
  Preferences) returns to Preferences without writing again. Safe-area: top inset + 8, horizontal insets + 16,
  bottom `max(inset, 32)`; header and footer fixed, middle scrolls.

| en | nl |
|---|---|
| {current} of {total} | {current} van {total} |
| Skip / Next / Done | Overslaan / Volgende / Klaar |
| Your plan on one timeline | Je plan op één tijdlijn |
| Sleep, light, caffeine and your flights side by side, each at the right local time. | Slaap, licht, cafeïne en je vluchten naast elkaar, steeds op de juiste lokale tijd. |
| Know each step at a glance | Herken elke stap in één oogopslag |
| Every kind of step has its own icon and pattern, so you never need colour to tell them apart. | Elke soort stap heeft een eigen pictogram en patroon, zodat je ze ook zonder kleur uit elkaar kunt houden. |
| Tick off what you did | Vink af wat je hebt gedaan |
| Checking off changes your progress, never your plan. Turn on reminders to get a nudge before each step. | Afvinken verandert je voortgang, nooit je plan. Zet herinneringen aan om vóór elke stap een seintje te krijgen. |

Preview strings (Sleep/Slapen, Seek bright light/Zoek fel licht op, Avoid light/Vermijd licht, Caffeine/Cafeïne,
Flight/Vlucht, 1 cup of coffee/1 kop koffie, Seek bright light outdoors/Zoek buiten fel licht op, 1 cup of filter
coffee/1 kop filterkoffie, In 15 min/Over 15 min, Tokyo/Tokio, `{done}/{total} done`/`{done}/{total} gedaan`) are in
the same catalog.

---

## 7. Terms update, policy reader, sign-out with pending changes

**TermsUpdate / TermsFailed** [V `C/TermsUpdate.dc.html`; `iOS/Docs/phase-17g-terms.md`;
`Kit/Settings/TermsUpdateView.swift:55-130`]

- Board: padding 24/16/32, gap 20. Header 44: 28 pt brand icon + eyebrow "Before you continue" (dawnText).
  Title Fraunces 32/38; body 16/24 muted. Summary block `surface-2`, radius 22, padding 18, gap 12, heading "What
  changed" (14/20 600 uppercase muted), dawn 6 pt bullets, policy row (min 52, radius 16, card) "Read the full
  terms and privacy policy". Spacer. Failure banner (AuthStates style) "Couldn't save your answer." / "Check your
  connection and try again. You can keep reading the policy meanwhile." Primary 48 high "Accept and continue" (→
  "Try again" when failed), ghost 48 "Sign out", footnote 12/16.
- iOS ships, in code, RN's generic body and bullets rather than the board placeholders:
  body "Please take a moment to read what changed. You'll need to agree before carrying on.", bullets "ChronoFlow
  helps you plan sleep, light and caffeine around your trip." / "ChronoFlow gives general timing guidance, not
  medical advice. Check with a doctor if you have a health condition, are pregnant or take prescription medicine."
  / "Your account stores your trips, profile and preferences.", footnote "Your trips stay saved if you sign out."
  (`TermsUpdateView.swift:71,85-87`). The 17g doc says placeholders were kept; **the code wins** (OQ A7).
- Accept writes the displayed version + timestamp + `settings:policies` outbox row in one transaction; the gate
  advances at once, offline too; sync sends `POST terms/acceptance {accepted: true, version}`. "Failed" = the local
  transaction failed. A newer version arriving later asks again.
- Policy link: `https://chronoflow.net/terms` in the browser (`TermsUpdateView.swift:98`); PolicyReader is not built.

| en | nl |
|---|---|
| Before you continue | Voordat je verdergaat |
| We've updated our terms | We hebben onze voorwaarden bijgewerkt |
| What changed | Wat is er veranderd |
| Read the full terms and privacy policy | Lees de volledige voorwaarden en het privacybeleid |
| Accept and continue | Akkoord en verder |
| Couldn't save your answer. | Je antwoord kon niet worden opgeslagen. |
| Check your connection and try again. You can keep reading the policy meanwhile. | Controleer je verbinding en probeer het opnieuw. Je kunt de voorwaarden intussen blijven lezen. |
| Sign out | Uitloggen |
| Your trips stay saved if you sign out. | Je reizen blijven bewaard als je uitlogt. |
| (generic body and 3 bullets above) | Lees even wat er is veranderd. Je moet akkoord gaan om verder te gaan. / ChronoFlow helpt je slaap, licht en cafeïne rond je reis te plannen. / ChronoFlow geeft algemene timingadviezen, geen medisch advies. Overleg met een arts als je een aandoening hebt, zwanger bent of medicijnen op recept gebruikt. / Je account bewaart je reizen, profiel en voorkeuren. |

**PolicyReader** [V `C/PolicyReader.dc.html`]: iOS-style nav ("Your data" back), large title "Privacy policy",
MetaChips "Version 2.4" / "Accepted 2 Sep 2026", a 6-item table of contents (What we collect, Why we use it, Your
health answers, How long we keep it, Your rights, How to reach us), every body "[Policy text from your legal team]",
outline "Open on the web". No real content exists → not built on iOS; Android same (OQ A6).

**SignOutPending** [V `C/SignOutPending.dc.html`; `Kit/Settings/SignOutPendingSheet.swift`]: bottom sheet
(radius 38 top, padding 12/20/32, gap 14, grabber 36 × 5). Title Fraunces 24/32 "Sign out and lose %d changes?"
(plural), body 16/24 muted "These haven't reached your account yet. If you sign out now, they're removed from this
phone.", list on surface-2 (radius 18) of pending changes with category swatch, "Stay signed in" (primary 48),
"Sign out and discard" (ghost, destructive text, 48), footnote "Changes sync by themselves when you're online.
Staying signed in keeps them." Reached from Terms and Settings. Voluntary sign-out first tries to flush; it only asks
when rows remain (D25, OQ 29).

---

## 8. Auth shell (shared by every auth screen)

### 8.1 Geometry [V `iOS/plan.md:471-480`; boards `C/AuthSignIn.dc.html`, `AuthTwoFactor.dc.html`]

- Reference frame 375 × 812. Only the sky fills the window. Scroll view with min content height = viewport;
  flexible spacer between heading and card (min `space-6` = 24).
- Insets: top `safeTop + 16`, start `safeStart + 16`, end `safeEnd + 16`, bottom `safeBottom + 24`.
- Brand row ≥ 44 high: back control 44 × 44 (22 glyph chevron `m15 18-6-6 6-6`, stroke 2, skyTitle), centred
  brand (moon 20, dawn, gap 6, "ChronoFlow" Fraunces 18/24 skyTitle), a 44 spacer opposite. Screens without back
  keep an empty 44 spacer (reset form, reset done).
- Heading block: margin-top 32 (24 on Sign up), gap 8. Problem/done variants add a 48 disc (problem: card bg,
  destructive triangle-alert 24; done: dawn bg, dawnForeground check 24, stroke 2.5) with 8 below.
- Card: `card`/`card-foreground`, padding 20, radius 26, gap 14, `shadow-soft` (light `0 10 30 −12 oklch(0.3 0.08
  272 / 0.25)`, dark `0 12 40 −14 oklch(0 0 0 / 0.6)`, `C/theme.css:57,108`). Card width = available width.
- Footer pill: margin-top 12, min 48, full pill, skyCopyBackground / skyCopy, Figtree 500 14/20.
- Field: label / input / hint-or-error gap 8; border `input`, focused `ring`, invalid `destructive`; radius 12;
  horizontal inset 12. Board input 36 and button 40 are web sizes; native hit regions ≥ 44 (Android: 48, §15.11).
- Banner (AuthStates): **first inside the card**, above every control. Padding 12/14, gap 10, radius 18; 18 alert
  circle icon (destructive) 1 down; background OKLab mix destructive 10 % + card 90 %; 1 px border destructive α35 %;
  title Figtree 600 14/20, body 400 14/20, card-foreground. Clears when the user edits a field.
- Notice (Forgot sent): info tone, card/background, border, radius 22, padding 12, gap 12, 16 info icon muted, title 600.

### 8.2 Typography [V `iOS/plan.md:481-492`]

| Element | Font | Size/line | Tracking | Colour |
|---|---|---|---|---|
| Brand | Fraunces 400 (opsz 20 cut) | 18/24 | 0 | skyTitle |
| Heading | Fraunces 400 (opsz 32 cut) | 36/40 | −0.36 | skyTitle |
| Description / input | Figtree 400 | 16/24 | 0 | skyTitle / cardForeground |
| Field label / button / link | Figtree 500 | 14/20 | 0 | cardForeground / button fg |
| Create-account link, banner/notice title | Figtree 600 | 14/20 | 0 | dawnText / cardForeground |
| Hint, field error, banner body, notice, countdown | Figtree 400 | 14/20 | 0 | muted / destructive / cardForeground |
| Divider, consent | Figtree 400 | 12/16 | 0 | muted (links foreground) |
| Footer | Figtree 500 | 14/20 | 0 | skyCopy |

Countdown uses tabular figures. Accepted board values (OQ 33): input/outline borders and the guest pill keep their
low non-text contrast; `(sign-in.guest, contrast)` is the one accepted text exception; auth subtitle contrast is
measured ≥ 4.5:1 on real pixels.

### 8.3 Form behaviour [V `iOS/plan.md:494-503`, `:2234-2246`; `Kit/Auth/AuthFormModel.swift`]

- **No client validation.** The server (or mock) returns all field messages. No offline-disabled submit.
- Double-submit lock across tap and keyboard action (`AuthFormModel.begin`); fields and actions disabled while a
  request runs; button keeps label and size and shows a spinner.
- An edit clears the banner and that field's error, **only when the value actually changed** (focus rewriting an
  unchanged value must not clear feedback, fix 2026-11-08).
- After a failure: banner-only failures dismiss the keyboard, scroll the banner into view and move accessibility
  focus to it; field failures focus the first invalid field. Every failure gets a banner or a visible field message
  and one announcement per failed submit (`AuthFailureFeedback.swift`).
- Keyboard: keep the focused input **and** the submit button fully visible above the keyboard, plus the field's
  hint/error where space allows; recompute on every keyboard/inset change. If a small screen at the max text size
  cannot fit both, record it, do not shrink text or pin a second button (OQ 32).
- Return chains: Sign in email → password → go; Sign up name → email → password → confirmation → go; Reset new →
  confirmation → go; Forgot email → send; recovery code → go. Code field (number pad): no auto-submit at 6 digits.
- AutoFill hints: email/username, current password, new password, one-time code; no autocorrect or capitalisation
  for email, password, codes.
- Secrets: passwords, tokens, 2FA codes, challenge IDs and link secrets never go to the DB, outbox, logs or
  navigation state.

### 8.4 Failure → banner/field mapping [V `Kit/Auth/AuthFormModel.swift:5-90`; `plan.md:630`]

| Failure | Banner (title / body) or field |
|---|---|
| offline (transport/timeout) | "Needs a connection" (title only), every auth form (settled OQ 23, 2026-11-08) |
| 429 `throttled` | Sign in: "Too many attempts." / "Wait a few minutes before you try again, or reset your password now."; Two-factor, Reset, Sign up, Verify: "Too many attempts." / "Wait a few minutes before you try again."; Forgot: "Too many requests." / "Wait a few minutes before you ask for another link." |
| 422 with `These sign-in details don't match our records.` (Sign in) | "That email and password don't match." / "Check both and try again, or reset your password." |
| 422 email `This email address is already in use.` (Sign up) | "There's already an account with this email." / "Sign in instead, or reset your password." (other field errors shown too) |
| 422 on a displayed field with a known key | under that field |
| 422 two-factor `code` "The authentication code was not accepted. Try again." | field: "That code didn't work. Codes change every 30 seconds, so try the newest one." |
| 422 on an undisplayed field / unknown key / empty validation | server banner |
| other (5xx, malformed, session store failure) | "Something went wrong on our side." + body per context: default "Try again in a moment."; Sign up "Your details are kept. Try again in a moment."; Reset "Your new password wasn't saved. Try again in a moment."; Verify "We couldn't send the link. Try again in a moment." |
| passkey user cancel | silent |

Error envelope [V `iOS/plan.md:439`]: `{code, message, errors?, args?, retryAfterSeconds?}`; 401 `unauthenticated`
→ unauthorized; 403 `email_unverified` → verify gate (never wipe); 429 retry-after from body, then header (seconds
or date), then 60 s; Laravel `:min/:max` arguments become typed placeholders; never show an untranslated backend
sentence in Dutch.

---

## 9. Auth screens one by one

### 9.1 Sign in [V `C/AuthSignIn.dc.html`; `Kit/Auth/SignInModel.swift`]

Order in the card: banner, "Sign in with a passkey" (Button lg, 16 pt key icon), divider "or with email", Email,
Password, "Sign in" (outline, full width), links row (Forgot your password? 14/20 500 underlined; Create an account
14/20 600 dawnText underlined; each ≥ 44). Footer pill "New here? Plan a trip first" → guest wizard. Back → Welcome.

Behaviour: starts empty; password cleared after **every** attempt; email and feedback kept. `POST auth/login` →
session (→ accept → gate route) or two-factor challenge (→ push Two-factor; no session yet). With a live guest draft
the request carries `draft_token` + `guest_secret` (§12).

### 9.2 Passkey sign-in [V `iOS/plan.md:444-445,453`; `Kit/Auth/PasskeySignIn.swift`]

`POST auth/passkey/options` (public, no body, no bearer) → `{challengeId, options}` with 32-byte unpadded base64url
challenge, `rpId: "app.chronoflow.net"`, `timeout: 60000`, `userVerification: "required"`, `allowCredentials: []`;
challenge ID 64 chars, single use, 120 s. Then the platform assertion, then `POST auth/passkey/login {challengeId,
credential: {id, rawId, type: "public-key", response: {clientDataJSON, authenticatorData, signature, userHandle?}},
device_id, device_name, draft_token?, guest_secret?}` → the same session shape as password login. `id` and
`clientDataJSON` unpadded base64url; ≤ 8192 chars each; every retry fetches fresh options. Cancel → silent.
Credential/challenge validation errors → the generic server banner (OQ 30). Limits: options 10/min/IP; login
5/min per credential+IP, 20/min/IP, 20/h per credential counting 422s (`W app/Providers/AppServiceProvider.php:128-137`).

### 9.3 Two-factor [V `C/AuthTwoFactor.dc.html` screen `twofactor`; `Kit/Auth/TwoFactorModel.swift`]

Title "Enter your code", sub "Open your authenticator app and type the 6-digit code for ChronoFlow.". Code mode:
field "6-digit code", numeric, one-time-code autofill, input filtered to ASCII digits, max 6. Recovery mode:
field "Recovery code", regular keyboard, placeholder `xxxx-xxxx` (not translated), hint "Each recovery code works
once.". Primary "Verify"; ghost toggle "Use a recovery code instead" ⇄ "Use your authenticator app". Toggling
clears code and errors. Code cleared after every attempt. `POST auth/two-factor {challengeId, code}` or
`{challengeId, recovery_code}`. Challenge lives only in memory; missing/blank challenge → back to Sign in. Back →
Sign in, discarding challenge and code. Server: 5 min, 5 failures; limits 5/min + 10/h. Expired/exhausted
challenges use the returned field message; no extra screen.

### 9.4 Forgot password [V screen `forgot`; `Kit/Auth/ForgotPasswordModel.swift`]

Title "Reset your password", sub "Enter your email and we'll send you a link to choose a new one.". Email +
"Send reset link". `POST auth/forgot-password {email}` → 204 for any address (no account enumeration). Sent state:
info Notice "Check your inbox." / "If {email} has an account, a link to choose a new password is on its way."
(email = the submitted value), primary "Open email app", ghost "Back to sign in". Limits: 1/min per email+IP,
5/min/IP. Back → Sign in.

### 9.5 Reset, expired, done [V screens `reset`, `resetExpired`, `resetDone`; `Kit/Auth/ResetPasswordModel.swift`]

- Entry only from a reset link (§11). Link without token or email → expired view directly.
- Form: no back button (44 spacer). Title "Choose a new password", sub "Sign in with your new password once it's
  saved.", "New password" (+ policy hint), "Confirm new password", primary "Save new password". Both cleared after
  every attempt. `POST auth/reset-password {token, email, password, password_confirmation}` → 204. Invalid/expired
  token key "This password reset link is invalid or has expired." → expired view.
- Expired: problem disc, "This link has expired", "Reset links work once and expire after an hour. We can send you a
  new one.", editable Email prefilled from the link, primary 48 "Send a new link" (sends directly, then the Forgot
  sent notice), ghost "Back to sign in".
- Done: done disc, "Password saved", "Password updated. Sign in with your new password.", muted line "Other devices
  are signed out. If you didn't make this change, reset your password again and contact us.", primary 48 "Continue"
  → Sign in. **No session** is created (the server revokes all tokens, `W app/Actions/Fortify/ResetUserPassword.php:35`).

### 9.6 Sign up [V `C/AuthSignUp.dc.html`; `Kit/Auth/SignUpModel.swift`]

Back "Back to sign in". Title "Create an account" (heading margin 24), sub "Save your trips and get reminders on this
phone.". Fields Name, Email, Password (policy hint), Confirm password; one "Show password"/"Hide password" switch
(eye 18, ≥ 44, `role=switch`) for both password fields; primary "Create account"; consent 12/16 centred "By creating an
account you agree to the <terms>terms</terms> and <privacy>privacy policy</privacy>." Footer "Already have an account?
Sign in". `POST auth/register {name, email, password, password_confirmation, device_id, device_name, language: en|nl,
+guest claim fields}` (90 s timeout) → session, unverified → Verify. Passwords cleared after every attempt; name and
email kept. Limit 5/min/IP.

### 9.7 Verify email [V screens `verify`, `verifyFailed`; `Kit/Auth/VerifyEmailModel.swift`]

- Waiting: back (= sign out with discard), "Check your inbox", "We sent a link to {email}. Open it on this phone to
  finish.", primary "Open email app", outline "Resend link" (disabled while counting), muted centred tabular "You can
  resend in {m:ss}.", underlined "Use a different email" (= sign out with discard).
- Failed: problem disc, "We couldn't check your email", "The link may have expired, or we couldn't reach the server.",
  primary 48 "Try again" (re-check), outline 48 "Send a new link" (resend).
- Behaviour: a verify link → `POST email/verify {id, hash, expires, signature}` (authenticated); otherwise
  `GET users/me` status. Re-check on every return to foreground (after the email app or web verification). Resend
  `POST email/verification-notification` → `{email, hasVerifiedEmail, resendAfterSeconds}`; the deadline uses
  wall-clock time, keeps the larger of current and new deadlines, survives backgrounding; a 429 extends it. All
  results must match account + email + session epoch before `has_verified_email` is stored; then the gate route.
  Limits: send 1/min per user; verify 6/min.

### 9.8 Email changed — out of scope (iOS D27).

### 9.9 Auth copy (en / nl) [V `Kit/Auth/Resources/Localizable.xcstrings`]

| en | nl |
|---|---|
| Back | Terug |
| Back to sign in | Terug naar inloggen |
| Welcome back | Welkom terug |
| Sign in to see your trips and plans. | Log in om je reizen en plannen te bekijken. |
| Email | E-mailadres |
| Password | Wachtwoord |
| Sign in | Inloggen |
| Sign in with a passkey | Inloggen met een passkey |
| or with email | of met e-mail |
| Forgot your password? | Wachtwoord vergeten? |
| Create an account | Een account aanmaken |
| New here? Plan a trip first | Nieuw hier? Plan eerst een reis |
| That email and password don't match. | Dit e-mailadres en dit wachtwoord horen niet bij elkaar. |
| Check both and try again, or reset your password. | Controleer ze allebei en probeer het opnieuw, of herstel je wachtwoord. |
| Too many attempts. | Te veel pogingen. |
| Wait a few minutes before you try again, or reset your password now. | Wacht een paar minuten voordat je het opnieuw probeert, of herstel nu je wachtwoord. |
| Wait a few minutes before you try again. | Wacht een paar minuten voordat je het opnieuw probeert. |
| Something went wrong on our side. | Er ging bij ons iets mis. |
| Try again in a moment. | Probeer het zo meteen opnieuw. |
| Needs a connection | Verbinding nodig |
| Enter your code | Voer je code in |
| Open your authenticator app and type the 6-digit code for ChronoFlow. | Open je authenticator-app en voer de 6-cijferige code voor ChronoFlow in. |
| 6-digit code | 6-cijferige code |
| Recovery code | Herstelcode |
| Each recovery code works once. | Elke herstelcode kun je één keer gebruiken. |
| Verify | Bevestigen |
| Use a recovery code instead | Gebruik een herstelcode |
| Use your authenticator app | Gebruik je authenticator-app |
| That code didn't work. Codes change every 30 seconds, so try the newest one. | Die code werkte niet. Codes veranderen elke 30 seconden, dus probeer de nieuwste. |
| Reset your password | Stel je wachtwoord opnieuw in |
| Enter your email and we'll send you a link to choose a new one. | Vul je e-mailadres in. We sturen je een link om een nieuw wachtwoord te kiezen. |
| Send reset link | Verstuur resetlink |
| Check your inbox. | Bekijk je inbox. |
| If {email} has an account, a link to choose a new password is on its way. | Als er een account bestaat voor {email}, is er een link onderweg om een nieuw wachtwoord te kiezen. |
| Open email app | Open je e-mailapp |
| Too many requests. | Te veel verzoeken. |
| Wait a few minutes before you ask for another link. | Wacht een paar minuten voordat je een nieuwe link aanvraagt. |
| Choose a new password | Kies een nieuw wachtwoord |
| Sign in with your new password once it's saved. | Log in met je nieuwe wachtwoord zodra het is opgeslagen. |
| New password | Nieuw wachtwoord |
| Use at least 12 characters, with uppercase and lowercase letters, numbers and symbols. Choose a password that has not appeared in a data leak. | Gebruik minstens 12 tekens, met hoofdletters, kleine letters, cijfers en symbolen. Kies een wachtwoord dat niet in een datalek voorkomt. |
| Confirm new password | Bevestig je nieuwe wachtwoord |
| Save new password | Nieuw wachtwoord opslaan |
| This link has expired | Deze link is verlopen |
| Reset links work once and expire after an hour. We can send you a new one. | Een herstellink werkt één keer en verloopt na een uur. We kunnen je een nieuwe sturen. |
| Send a new link | Nieuwe link sturen |
| Password saved | Wachtwoord opgeslagen |
| Password updated. Sign in with your new password. | Je wachtwoord is gewijzigd. Log in met je nieuwe wachtwoord. |
| Other devices are signed out. If you didn't make this change, reset your password again and contact us. | Andere apparaten zijn uitgelogd. Heb je dit niet zelf gedaan? Herstel je wachtwoord dan opnieuw en neem contact met ons op. |
| Continue | Doorgaan |
| Your new password wasn't saved. Try again in a moment. | Je nieuwe wachtwoord is niet opgeslagen. Probeer het zo meteen opnieuw. |
| Save your trips and get reminders on this phone. | Bewaar je reizen en ontvang herinneringen op deze telefoon. |
| Name | Naam |
| Confirm password | Bevestig je wachtwoord |
| Show password / Hide password | Wachtwoord tonen / Wachtwoord verbergen |
| Create account | Account aanmaken |
| By creating an account you agree to the <terms>terms</terms> and <privacy>privacy policy</privacy>. | Door een account aan te maken ga je akkoord met de <terms>voorwaarden</terms> en het <privacy>privacybeleid</privacy>. |
| Already have an account? Sign in | Heb je al een account? Log in |
| There's already an account with this email. | Er is al een account met dit e-mailadres. |
| Sign in instead, or reset your password. | Log in of herstel je wachtwoord. |
| Your details are kept. Try again in a moment. | Je gegevens blijven bewaard. Probeer het zo meteen opnieuw. |
| Check your inbox | Bekijk je inbox |
| We sent a link to {email}. Open it on this phone to finish. | We hebben een link naar {email} gestuurd. Open die op deze telefoon om het af te ronden. |
| Resend link | Link opnieuw versturen |
| You can resend in {time}. | Je kunt de link over {time} opnieuw versturen. |
| Use a different email | Gebruik een ander e-mailadres |
| We couldn't check your email | We konden je e-mailadres niet controleren |
| The link may have expired, or we couldn't reach the server. | De link is misschien verlopen, of we konden de server niet bereiken. |
| Try again | Opnieuw proberen |
| We couldn't send the link. Try again in a moment. | We konden de link niet versturen. Probeer het zo meteen opnieuw. |
| Something went wrong | Er ging iets mis |

Server field messages (keys from `W/app/Http/Requests/Auth/AuthMessageKeys.php`):

| en | nl |
|---|---|
| Please fill this in. | Vul dit veld in. |
| Enter a valid email address, like name@example.com. | Vul een geldig e-mailadres in, zoals naam@voorbeeld.nl. |
| Use no more than {max} characters. | Gebruik niet meer dan {max} tekens. |
| Use at least {min} characters. | Gebruik minstens {min} tekens. |
| The passwords don't match yet. | De wachtwoorden komen nog niet overeen. |
| Use both uppercase and lowercase letters. | Gebruik hoofdletters en kleine letters. |
| Use at least one letter. | Gebruik minstens één letter. |
| Use at least one number. | Gebruik minstens één cijfer. |
| Use at least one symbol. | Gebruik minstens één symbool. |
| This password has appeared in a data leak. Choose a different password. | Dit wachtwoord is gevonden in een datalek. Kies een ander wachtwoord. |
| Enter the 6-digit code from your authenticator app. | Vul de 6-cijferige code uit je authenticator-app in. |
| Enter one of the recovery codes you saved when you set up two-factor authentication. | Vul een van de herstelcodes in die je hebt bewaard bij het instellen van tweefactorauthenticatie. |
| The authentication code was not accepted. Try again. | De verificatiecode is niet geaccepteerd. Probeer het opnieuw. |
| This recovery code was not accepted. Try another saved code. | Deze herstelcode is niet geaccepteerd. Probeer een andere bewaarde code. |
| This password reset link is invalid or has expired. | Deze link om je wachtwoord te herstellen is ongeldig of verlopen. |
| This email verification link is invalid or has expired. | Deze link om je e-mailadres te verifiëren is ongeldig of verlopen. |

Classification keys (not displayed): "These sign-in details don't match our records." → credentials banner; "This
email address is already in use." → email-taken banner.

---

## 10. API contract summary for this area [V `W/routes/api.php:46-114`; `iOS/plan.md:437-451`]

Base `https://api.chronoflow.net/v1` (iOS default; `W/config/domains.php` derives `api.` + APP_URL host).

| Operation | Request → response | Auth / timeout |
|---|---|---|
| Login | `POST auth/login {email,password,device_name,device_id,draft_token?,guest_secret?}` → `{kind:"session",token,account,tripId}` or `{kind:"two-factor",challengeId}` | public, 15 s |
| Two-factor | `POST auth/two-factor {challengeId, code}` or `{challengeId, recovery_code}` → session | public |
| Passkey options / login | §9.2 | public |
| Register | `POST auth/register {name,email,password,password_confirmation,device_id,device_name,language,draft_token?,input_revision?,review_revision?,guest_secret?}` → session (unverified) | public, 90 s |
| Forgot | `POST auth/forgot-password {email}` → 204 | public |
| Reset | `POST auth/reset-password {token,email,password,password_confirmation}` → 204 | public |
| Verify status | `GET users/me` → identity incl. `hasVerifiedEmail`, policies, walkthrough | bearer |
| Resend | `POST email/verification-notification` → `{email,hasVerifiedEmail,resendAfterSeconds}` | bearer |
| Verify | `POST email/verify {id,hash,expires,signature}` → same status | bearer |
| Terms | `POST terms/acceptance {accepted:true, version}` | bearer, verified |
| Walkthrough | `POST walkthrough` | bearer, verified |
| Logout | `POST auth/logout` (best effort, 3 s) | bearer |
| Guest review | `POST guest/trips/review` | public, 90 s |

Client rules [V `plan.md:437-438`]: no cookies, no HTTP cache, **decline every redirect** (no bearer leaks
across origins), HTTPS only in release, public auth calls never carry an old bearer, never log secrets or signed
links. One token per device, 90 days sliding (`W app/Actions/Api/IssueMobileToken.php:12`). `device_id` = a
UUIDv7 created once on the device and kept across sign-out; `device_name` trimmed to 255 characters (D22).

---

## 11. Email links: reset and verify

### 11.1 URL shapes [V `W main:app/Providers/FortifyServiceProvider.php:78-91`, `app/Http/AppUrls.php`]

URLs are built on `config('app.url')` (`app.chronoflow.net`, iOS plan `:502`) with Fortify's default routes:

- Reset: `https://app.chronoflow.net/reset-password/{token}?email={urlencoded email}`.
- Verify: `https://app.chronoflow.net/email/verify/{id}/{hash}?expires={unix}&signature={hex}` (temporary signed
  route, expiry `auth.verification.expire`, default 60 min; `hash = sha1(email)`).

### 11.2 Parsing rules — port `Kit/AppCore/AuthLink.swift` exactly [V]

Accept only scheme `https`, host `app.chronoflow.net`, no port, no user/password, no fragment.
- Verify: path `/email/verify/{id}/{hash}` (exactly 5 segments); `id` must parse as a UUID; `hash` 40 hex; exactly
  two query items `expires` (digits only) and `signature` (64 hex) → `SignedVerificationInput`. Anything else under
  `/email/verify` → `verify(nil)` (malformed → the failed state).
- Reset: path `/reset-password/{token}` (3 segments, non-empty token) with exactly one query item `email`; else a
  reset link with empty token/email → expired view.
- Percent-decode path parts; pass signed values through untouched; never re-sign.

### 11.3 Routing rules [V `Kit/Auth/GuestResetLinkRouter.swift`, `VerificationLinkInbox.swift`, `SceneDelegate.swift:663-695`]

- Links are captured at cold start and when the app is already running; consumed only **after** the splash is gone.
- Reset links wait until the user is **signed out**; a signed-in user keeps the link pending and it opens on the
  next sign-out (`GuestResetLinkRouter`). Opening resets the auth stack to Sign in → Reset.
- Verify links: ignored for a verified account; kept for the first sign-in when signed out; consumed only for the
  matching account ID; cleared on sign-out, session replacement and verified sign-in (`VerificationLinkInbox`).
- Links never change the launch ending.

---

## 12. Guest flow and claim [V `iOS/plan.md:2062-2210`; `Kit/Auth/SignUpModel.swift:65-112`; `C/GuestSignup.dc.html`]

- Entry: Welcome "Plan my trip" and Sign in footer "New here? Plan a trip first" → the member wizard in guest mode
  (one `WizardMode` flag, no second set of screens). One-way only: Where → Times → Your sleep → Checking → [Age,
  Shift work] → [Fine-tune] → Ready → sign-up. Exits (×, excluded, failures, no plan) → Welcome.
- Review: `POST guest/trips/review` (public, no bearer, 90 s), requires `device_id` + `profile`
  (`habitual_bed`/`habitual_wake`), forbids `return`/`override`; first call mints `identity.guestSecret`.
  Limits 5/min per IP+device, 30/min per IP. The guest secret lives only in secure storage, one per draft token;
  rotated when the server returns a new one. Guest draft: one recovery record, 30-minute sliding expiry, never
  listed, excluded from backup, never contains the secret.
- **GuestSignup sheet** over WizardReadyGuest/Free: grabber, title Fraunces 24/32 "Save your plan", sub "Create an
  account to save it and open it on any device.", banner per state, Name / Email / Password (policy hint) /
  Confirm password, primary "Create account and save", ghost "I already have an account", consent "By creating an
  account you accept the <terms>terms</terms> and <privacy>privacy policy</privacy>.".

  | State | When | Banner |
  |---|---|---|
  | ready | default | none |
  | failed | register failed with a banner | "We couldn't create your account." / "Try again in a moment." |
  | retry | account created but local session acceptance failed | "Your account is ready." / "Try saving again shortly." |
  | review | `review_expired` (also inside `validation_failed`) or no live claim | "Your trip needs a new check." / "Review your trip again before creating your account." + link "Review my trip" → back to Ready and re-check |

  nl: Sla je plan op / Maak een account om het te bewaren en op elk apparaat te openen. / We konden je account niet
  aanmaken. / Je account is klaar. / Probeer het opslaan zo opnieuw. / Je reis moet opnieuw worden gecontroleerd. /
  Bekijk je reis opnieuw voordat je je account maakt. / Account aanmaken en opslaan / Ik heb al een account / Mijn
  reis bekijken / Door een account aan te maken, ga je akkoord met de <terms>voorwaarden</terms> en het
  <privacy>privacybeleid</privacy>.
- **Claim:** register sends `draft_token`, `input_revision`, `review_revision`, `guest_secret`; on success `tripId`
  = the draft token. Password and passkey login send `draft_token` + `guest_secret` (they only replay an earlier
  registration receipt). Two-factor completion sends no claim fields.
  - `tripId` set → keep a per-account continuation through verify/terms/walkthrough, then open that trip with
    `awaitingSync`; clear the guest draft and secret.
  - `tripId` null after password/passkey login with a live draft → seed the member wizard at Checking with
    `returnToReady` (guest answers and sleep override dropped).
  - null after two-factor → clear draft and secret, open My trips, let sync surface it.
- "I already have an account" → Sign in carrying the claim. Sign-out deletes every guest secret and the guest draft.

---

## 13. Password prompt (signed in, Settings) — summary [V `C/PasswordPrompt.dc.html`; `Kit/Settings/SettingsPasswordPrompt.swift`]

Shared sheet for: change email, delete account, 2FA on/off, new codes, show codes. Header: Cancel (start, dawnText) +
title "Confirm it's you"; action line; one password field (52 high, radius 12, fill `rgba(118,118,128,0.12)`);
error line; "Forgot password?" link → alert "Reset your password?" / "We'll email a reset link to {email}. You stay
signed in on this phone." / Cancel, Send link; button (50 high, primary; destructive for delete and 2FA off).
States: empty (button disabled α0.45), checking (field α0.55, spinner, "Checking…"), wrong ("The provided password
was incorrect.", 2 px destructive inset), locked ("Too many attempts. Try again in {n} …": < 60 s seconds, ≥ 1 min
whole minutes rounded up, ≥ 60 min hours rounded up — canvas note `reqPasswordLockLine`). Wide: centred 540 sheet
(radius 38). nl copy is on the board. Details belong to the Settings research doc.

---

## 14. Error boards

ErrorNotFound/ErrorServer (SkyFrame, eyebrow/title/body, dawn primary + sky-copy secondary) are **not** used by
launch or auth on iOS; only the trip page uses them for a failed remote plan fetch while online
(`iOS/plan.md:1417`). Canvas decision `decErrors`: no offline screen. Android: same.

---

## 15. Android approach [R unless marked]

### 15.1 Android 12+ system splash and hand-off

Constraints of the platform splash (Android 12 / API 31+, backported by `androidx.core:core-splashscreen` 1.0.1):
- It shows only a background colour, a centred icon (static or animated vector), an optional icon background and an
  optional branding image. No custom layout, no text, no gradients.
- Icon without background: 288 × 288 dp, visible inside a 192 dp circle. Our moon image (104 dp glyph + baked glow)
  fits; RN already proved the 288 dp / 1152 px xxxhdpi asset [V `RN/plan.md:1164`].
- Shown on cold and warm starts (activity re-created while the process lives), not on hot starts.
- Theme-driven; dark/light only through `-night` resources (system setting).
- Android 12/12L show the icon only for launcher starts; Android 13+ shows it for other starts (notification, link)
  when `android:windowSplashScreenBehavior="icon_preferred"`.

Plan:
1. `Theme.App.Starting` (parent `Theme.SplashScreen`): `windowSplashScreenBackground` = sky-top (`#1F285A` /
   night `#06091C`), `windowSplashScreenAnimatedIcon` = `@drawable/launch_moon` (288 dp PNG, light + night),
   `android:windowSplashScreenBehavior=icon_preferred`, `postSplashScreenTheme` = app theme whose
   `android:windowBackground` is also sky-top (no flash between system splash and first Compose frame).
2. `MainActivity.onCreate`: `installSplashScreen()` **before** `super.onCreate`; `setKeepOnScreenCondition {
   !launch.overlayFrameDrawn }`; `setOnExitAnimationListener { it.remove() }` (instant removal; the overlay below is
   pixel-identical, so nothing visible changes). `enableEdgeToEdge()`.
3. First composition draws only the overlay frame 0 (sky-top + the same `launch_moon` drawable centred in the
   **window**, not in the insets). The app's NavHost is composed one frame later (mirrors iOS D3). `t0` = the
   Choreographer frame time of the first overlay frame.
4. Use `SplashScreenViewProvider.iconView` bounds (API 31+) in an instrumented test to assert the system icon centre
   equals the overlay moon centre ± 0.5 px, light and dark, gesture and 3-button navigation, cutout devices.
5. Warm start (activity re-created, process alive, overlay already used): no animation replay (D14). Remove the
   system splash with a 300 ms ease-out fade of the splash view into the current screen (OQ A2).
6. Android 12/12L link or notification cold start may show the background without the icon; the overlay frame 0
   then brings the moon in. Accept (OQ A3).
7. Never use an extra "splash activity".

### 15.2 The animated overlay: rendering and performance

- One composable `LaunchOverlay` above the app in the same window. All drawing in a single `Canvas`/`drawWithCache`
  whose draw lambda reads one `State<Long>` (elapsed ms). No recomposition per frame; only the draw phase runs.
- Port `CubicBezier`, `LaunchLayout`, `FlightPath` (128 arc-length samples) and `LaunchTimeline` (`opening`,
  `withReadyExit`, `withWelcomeExit`, `withStatus`, `sample(t)`) as pure Kotlin in a JVM module; reuse the iOS
  tests and reference values. Precompute paths, gradients and sample tables once per size; **no allocation per
  frame** (RN lesson).
- Blurred shapes (halo, plane glow) as radial gradients, like iOS (no blur filters, no `RenderEffect`, no masks);
  the moon glow is baked in the PNG; the wordmark is a pre-rendered vector drawable (Fraunces outlines) so fonts
  are off the critical path (iOS D8).
- The clock is wall time from `t0` (`System.nanoTime`), not the frame count, so a stall skips frames instead of
  stretching the timeline and backgrounding mid-splash ends it naturally. Animator duration scale is ignored except
  0 (= Reduce Motion, §15.10).
- Main thread before hand-off: no disk, Keystore, DataStore, Room or network on the main thread; session read and
  DB open on background dispatchers start right after the overlay's first frame (iOS D26). Fonts load lazily (first
  use is the status at 2000 or Welcome at 2300).
- Hand-off completion removes the overlay node; then `reportFullyDrawn()`.
- Welcome reports its moon glyph frame via `onGloballyPositioned { it.boundsInWindow() }` → `MoonAnchor` event.
  Overlay and Welcome share one window coordinate space, so no conversion is needed.
- Process-level `LaunchController` (application scope) owns `t0`, the captured appearance, the ending and the
  "overlay used" flag, so a configuration change or activity re-creation never replays the splash.

### 15.3 Edge-to-edge and system bars

Edge-to-edge is enforced for targetSdk 35+ and the opt-out is gone at 36. Call `enableEdgeToEdge()`.
- Splash, Welcome, auth: `SystemBarStyle.dark(TRANSPARENT)` for the status bar (light icons on the night sky, as
  iOS "status bar light until hand-off"). Navigation bar: transparent; icon colour from the bottom of the sky
  (light theme horizon `#EB9666` → dark icons; dark theme `#AA5830` → light icons) (OQ A9).
- Background screens (walkthrough, terms): `SystemBarStyle.auto` from the theme.
- `window.isNavigationBarContrastEnforced = false` on sky screens so the sky runs under 3-button navigation.
- All content uses `WindowInsets.safeDrawing` (system bars + cutout + IME) for padding, matching iOS's four insets.

### 15.4 Insets and keyboard (IME)

- With edge-to-edge, `adjustResize` no longer resizes the window; handle the IME in Compose: `imePadding()` on the
  auth scroll container; `windowSoftInputMode="adjustResize"` stays declared.
- Keep **field + submit** visible: on focus and on every `WindowInsets.ime` change, call
  `BringIntoViewRequester.bringIntoView(rect)` with the union of the focused field (incl. its hint/error) and the
  submit button, as iOS `AuthLayout.reveal` does (`Kit/Auth/AuthKeyboardReveal.swift`). Use
  `WindowInsets.imeAnimationTarget` to scroll once, not every animation frame.
- After a banner-only failure: `LocalSoftwareKeyboardController.hide()`, scroll the banner into view, move TalkBack
  focus there. Field failure: request focus on the first invalid field.
- Interactive keyboard dismissal (iOS `.interactively`) has no stable Compose equivalent; back closes the IME on
  Android (OQ A11).
- `KeyboardOptions`/`KeyboardActions`: email `KeyboardType.Email` + `ImeAction.Next`, autocorrect off, no
  capitalisation; password `KeyboardType.Password` + `Next`/`Go`; name `KeyboardCapitalization.Words`; code
  `KeyboardType.NumberPassword` is wrong (hides digits) → `KeyboardType.Number`, `ImeAction.Go` submits on explicit
  press only; recovery code `KeyboardType.Ascii`, `ImeAction.Go`. Ignore the action while busy.
- Fields: `BasicTextField(TextFieldState)` / `BasicSecureTextField` with `InputTransformation` (digits only, max 6)
  styled to the board's Field; not Material `OutlinedTextField` (its label/outline differs from the board).

### 15.5 Predictive back

targetSdk 36: predictive back is on by default (`enableOnBackInvokedCallback`; RN had it set to `false` [V
`RN/android/.../AndroidManifest.xml:16`]). Use Navigation Compose with predictive-back transitions and
`PredictiveBackHandler`/`BackHandler` only where behaviour differs. Map (OQ A4 for the gate screens):

| Screen | System back |
|---|---|
| Splash overlay | system default (back to home); returning later finds the hand-off done |
| Welcome | back to home (root) |
| Sign in | → Welcome (plain, no intro), also when Sign in is the root after sign-out |
| Two-factor | → Sign in; discard challenge and code |
| Forgot (form or sent), Sign up, Reset expired | → Sign in |
| Reset form, Reset done (no drawn back) | → Sign in; nothing saved (form) / same as Continue (done) |
| Verify waiting/failed | **back to home**; only the drawn back arrow / "Use a different email" signs out with discard |
| Terms | back to home (gate) |
| Walkthrough first run | page 2/3 → previous page; page 1 → back to home. Replay → Preferences |
| Guest sign-up sheet, password prompt, sign-out sheet | dismiss (= close / Cancel / Stay signed in) |
| While a request runs | back still works; the in-flight request is cancelled for that screen's model |

### 15.6 App Links for reset and verify (server work needed)

- Manifest: one `<intent-filter android:autoVerify="true">` with `VIEW`, `DEFAULT`, `BROWSABLE`, `https`, host
  `app.chronoflow.net`, `android:pathPrefix="/reset-password/"` and `"/email/verify/"`. No custom scheme
  (RN's `chronoflow` scheme would collide, iOS D15/OQ 15). `launchMode="singleTask"` (as RN) + `onNewIntent`.
- Parse with the ported `AuthLink` (§11.2), capture into `PendingLaunchIntent`, consume after hand-off (§11.3).
- **Server:** serve `https://app.chronoflow.net/.well-known/assetlinks.json` like the AASA route [V `W main:
  bootstrap/app.php:49-50`, `.ai/rules/api.md:73`, `.ai/rules/deploy.md:15`]: outside the web group, only
  `RejectApiHost`, `application/json`, no redirect/cookies, exempt from maintenance mode, not a static file:

  ```json
  [{
    "relation": ["delegate_permission/common.handle_all_urls",
                 "delegate_permission/common.get_login_creds"],
    "target": {"namespace": "android_app", "package_name": "<applicationId>",
               "sha256_cert_fingerprints": ["<Play app-signing SHA-256>", "<upload/debug SHA-256>"]}
  }]
  ```

  Today the server serves only AASA with `webcredentials` (no `applinks`), and the passkey plan explicitly left
  `assetlinks.json` and Android out [V `W main:docs/ui-plan/phase-18-mobile-api-passkeys.md:11,134`]. Until it is
  deployed, links open the web reset/verify pages, which work (graceful fallback); the app re-checks `users/me` on
  resume, so web verification still advances the gate.
- Verification on device: `adb shell pm get-app-links <pkg>`; tests feed intents directly (no mail, no network).

### 15.7 Credential Manager: passwords and passkeys

- `androidx.credentials:credentials` + `credentials-play-services-auth` (Android 9+ through Play services; framework
  provider on 14+). Always call with the Activity context.
- **Passkey button** (keep the board's separate button and divider, like iOS):
  `GetCredentialRequest(listOf(GetPublicKeyCredentialOption(requestJson)), preferImmediatelyAvailableCredentials =
  false)` where `requestJson` is the server's `options` object as-is (challenge, rpId, timeout, userVerification,
  allowCredentials). Read `PublicKeyCredential.authenticationResponseJson` and send only the documented fields
  (`id`, `rawId`, `type`, `response.{clientDataJSON, authenticatorData, signature, userHandle}`); drop
  `authenticatorAttachment` and `clientExtensionResults` unless the server accepts them (OQ A12). Normalise to
  unpadded base64url.
  - `GetCredentialCancellationException` → silent. `NoCredentialException` → silent (OQ A13).
    `GetCredentialUnsupportedException`/other → generic server banner (iOS maps an unavailable provider the same way).
- **Server for Android passkeys:** the assertion's origin is `android:apk-key-hash:<base64url SHA-256 of the
  signing cert>`. Fortify `passkeys.allowed_origins` is `[config('app.url')]` [V `W main:config/fortify.php:150`];
  `CheckAllowedOrigins` "already matches `android:apk-key-hash:` entries verbatim" [V phase-18 doc `:12`], so the
  server must add the release (and debug) hashes, plus `get_login_creds` in `assetlinks.json` for RP
  `app.chronoflow.net`.
- **Passwords:** rely on Autofill (Google Password Manager and third-party providers) with content types
  (§15.8), and call `LocalAutofillManager.current?.commit()` after a successful sign-in/sign-up so the provider can
  offer to save — the iOS-like flow. No forced `CreatePasswordRequest` sheet (OQ A14).

### 15.8 Autofill hints (Compose `Modifier.semantics { contentType = … }`)

| Field | Content type | Keyboard |
|---|---|---|
| Sign in email | `ContentType.EmailAddress + ContentType.Username` | Email |
| Sign in password | `ContentType.Password` | Password |
| Sign up / guest name | `ContentType.PersonFullName` | Text, Words |
| Sign up / guest email | `ContentType.EmailAddress + ContentType.NewUsername` | Email |
| Sign up / reset new password + confirmation | `ContentType.NewPassword` | Password |
| Forgot / reset-expired email | `ContentType.EmailAddress` | Email |
| Two-factor code | app OTP hint `HintConstants.AUTOFILL_HINT_2FA_APP_OTP` if the Compose API accepts custom hints; else none (OQ A15) | Number |
| Recovery code | none | Ascii |
| Password prompt | `ContentType.Password` | Password |

Never use `rememberSaveable`/`rememberTextFieldState()` for passwords, codes or challenge IDs: they would be
written to the saved-instance Bundle. Use `remember { TextFieldState() }`. Auth forms start empty after process death
anyway (cold start → splash, OQ 16).

### 15.9 Font-scale cap (iOS "xxLarge")

iOS caps Dynamic Type at xxLarge for the whole app (user 2026-11-06, OQ 10); xxLarge body is 21 vs 17 at the
default size (+23.5 %). Android: cap the effective font scale at **1.3** (the pre-Android-14 "Largest" step) at the
root: `CompositionLocalProvider(LocalDensity provides cappedDensity)`, where `cappedDensity.fontScale =
min(system, 1.3f)`. On Android 14+ keep non-linear scaling below the cap by using the platform's font-scale
converter for the capped scale (so 48 sp headlines grow less than 14 sp body, as the system intends). Do not cap
display size (density). Test at 1.0, 1.3 and 2.0 (2.0 must equal 1.3) — the iOS "AX5 == xxLarge" check (OQ A8).

### 15.10 Reduce Motion

Treat `ValueAnimator.areAnimatorsEnabled() == false` ("Remove animations", animator scale 0) as Reduce Motion, read
once at launch for the splash and observed for Welcome/press feedback. Apply §3.6. A debug-only intent extra
mirrors iOS `-cf.reduceMotion`.

### 15.11 Accessibility (TalkBack)

- Overlay: `semantics { contentDescription = "ChronoFlow is opening" }` (image role); app below
  `semantics { hideFromAccessibility() }` until hand-off. Status: `liveRegion = Polite`, announced once.
- `View.announceForAccessibility` is deprecated in API 36: use live regions for banners and `paneTitle` for screen
  changes. After hand-off, move focus to Welcome's first element (wordmark).
- Headings: `semantics { heading() }` on every screen title. Password visibility: `Role.Switch` with state.
  Field errors via `semantics { error(message) }` on the field.
- Touch targets: Android minimum 48 dp (`minimumInteractiveComponentSize`), keeping drawn visual sizes (iOS uses 44).
- Same TalkBack reading order as iOS VoiceOver (`plan.md:503`).

### 15.12 Window sizes, orientation, foldables

- Phones (smallest width < 600 dp): portrait-locked like iOS D16.
- targetSdk 36 ignores orientation and resizability restrictions on displays ≥ 600 dp: tablets and unfolded
  foldables get any orientation and multi-window sizes. The splash geometry is already size-generic (§3.2); Welcome
  and auth are scrolling columns. No board exists for wide layouts: use the phone layout at every width (iOS did the
  same for Duo, OQ 35) (OQ A5). Rebuild the timeline geometry on size change without resetting `t0`.

### 15.13 Language, secrets, process death

- Language: phone language `nl` → Dutch, else English (`plan.md:309`). Android 13+ per-app language
  (`locales_config.xml` with `en`, `nl`) replaces RN's in-app override, like iOS's per-app language. `register`
  sends `language` from the resolved app locale.
- Session token and guest secrets: Keystore-backed encryption, `allowBackup` exclusions; never in logs (the storage
  doc decides the mechanism).
- Process death: always the splash, no navigation restore (OQ 16); only DB-backed records (guest/member wizard
  recovery, pending outbox) survive.

### 15.14 Strings

Android resource IDs instead of English keys. Generate `values/strings.xml` and `values-nl/strings.xml` from the iOS
catalogs (exact en/nl, OQ 28 order already applied), keeping an English→ID map for review. `%@` → `%1$s`, `%lld` →
`%1$d`, plurals for "Sign out and lose %d changes?", and `<terms>…</terms>` tags as `<annotation>` spans for links.

### 15.15 Testing and measuring

- JVM: reducer, timeline/sample, bezier, layout, `AuthLink`, failure mapping, routing (port the iOS lists).
- Screenshot tests with a frozen clock (iOS `-cf.freezeAt`) for SplashSpec key frames 0/700/1400/1900, slow
  2600/4900, welcome 1500/1950/2400/3300, both themes, en/nl, font scale 1.0/1.3/2.0, narrow/tall/short/tablet.
- Instrumented: system-splash icon parity (§15.1), App Link intents cold/warm, IME visibility of field+submit with a
  real software keyboard (the iOS lesson: make sure the keyboard is really shown, OQ 32).
- Macrobenchmark: `StartupTimingMetric` (cold, warm), `FrameTimingMetric` over 0–3.2 s, with a Baseline Profile.
  Targets mirror §3.9: hand-off end 1900 ms ± 1 frame from `t0`; no janky frames.

---

## 16. Copy pipeline note

All copy in this document is the iOS catalog text, which already follows OQ 28 (RN Dutch → backend `lang/nl.json`
→ new translation in RN's je/jij tone, `e-mailadres`, `wachtwoord`, `inloggen`). The clock-shift rule applies:
promise a clock shift, never symptom relief.

---

## 17. Open questions (defaults the plan proceeds on)

| # | Question | Default |
|---|---|---|
| A1 | Package name: RN ships `net.chronoflow.app`; iOS native develops as `net.chronoflow.app.native`. | `net.chronoflow.app.native` during development (side-by-side with RN), switch at release like iOS OQ 4; both listed in `assetlinks.json`. |
| A2 | Warm start (activity re-created, process alive): how to leave the system splash? | No replay; 300 ms ease-out fade of the system splash into the current screen. |
| A3 | Android 12/12L link/notification cold start shows no icon. | Accept; overlay frame 0 shows the moon. |
| A4 | System back on Verify / Terms / Walkthrough page 1. | Back to home; only the drawn controls sign out or skip. |
| A5 | Layout on ≥ 600 dp and landscape (forced by targetSdk 36). | Phone layout at every width, no max width, until wide boards exist. |
| A6 | PolicyReader board exists but holds only placeholders. | Do not build; open `https://chronoflow.net/terms` and `/privacy` in a Custom Tab (iOS opens the system browser). |
| A7 | TermsUpdate copy: board placeholders vs iOS code's generic RN copy. | Follow iOS code (§7). |
| A8 | Font-scale cap value. | 1.3 (≈ iOS xxLarge), non-linear below the cap on Android 14+. |
| A9 | Navigation-bar icon colour on sky screens. | From the sky's bottom colour per theme; verify by screenshot. |
| A10 | Welcome contrast pairs that fail audits (iOS open items). | Keep board colours; record the same exceptions. |
| A11 | Interactive keyboard dismissal. | Not implemented; system back closes the IME. |
| A12 | Does `auth/passkey/login` accept extra Android JSON fields (`authenticatorAttachment`, `clientExtensionResults`)? | Strip to the documented shape. |
| A13 | `NoCredentialException` (no passkey/provider). | Silent, like a cancel; log a non-sensitive event. |
| A14 | Offer "save password" explicitly? | Autofill commit only; no forced Credential Manager save sheet. |
| A15 | Two-factor autofill hint on Android. | 2FA app OTP hint if the Compose API supports custom hints; otherwise none. |
| A16 | Server work for Android: `assetlinks.json` route + `allowed_origins` APK hash. | Write a server request (not implemented here); app ships with links/passkeys degrading gracefully until deployed. |
| A17 | `BasicSecureTextField` obfuscation: reveal last typed (Android default) vs hidden (iOS). | Reveal-last-typed (respects the system "Show passwords" setting). |
| A18 | Auth back affordance: board chevron vs Material arrow. | Material auto-mirrored arrow-back in a 48 dp button at the drawn position, tinted skyTitle. |
| A19 | Terms/privacy host: `chronoflow.net` while marketing moved to a `www` host (`W` commits `92bab962`, `b275e962`). | Keep `https://chronoflow.net/terms` and `/privacy` like iOS/RN; confirm before release. |

Inherited from iOS and still open there: OQ 4 (final IDs), OQ 6 (Reduce Motion dot opacity: hold at 1.0), OQ 26
(backup exclusion of unsent changes), OQ 30 (sync failure presentation), OQ 32 (keyboard fit on the smallest
screens), Phase 18 "Valet claim gate" (live claim acceptance).

---

## 18. Risks

- **System icon vs overlay moon mismatch** (scaling, cutouts, OEM launchers). Mitigation: same drawable for both,
  `iconView` parity test, screen recording on a physical device.
- **Main-thread contention during the splash** (Compose first composition, Room/Keystore on main). Mitigation:
  overlay-first composition, all I/O off-main, Baseline Profile, Macrobenchmark frame timing.
- **App Links not verified** (no `assetlinks.json` on the server yet). Links open the web; flows still complete.
- **Android passkeys rejected by the server** until `allowed_origins` includes the APK hash.
- **Large-screen/landscape forced by API 36** with no boards. Mitigation: size-generic geometry, scroll everywhere.
- **Saved-state leaks of secrets** via `rememberSaveable`/`SavedStateHandle`. Mitigation: lint rule or review check.
- **Board drift:** the canvas still draws pre-decision values (§2.2); implementers must use this document, not the
  raw boards, for those items.
