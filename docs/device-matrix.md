# Device matrix: measured pixel insets

Measured 2026-11-17 on the `pixel` AVD (Phase 0e, Step 0, worker sandbox).

## AVD facts

- `hw.device.name=pixel_8`, `hw.lcd.width=1080`, `hw.lcd.height=2400`, `hw.lcd.density=420`, `hw.mainKeys=no`.
- System image `system-images/android-36/google_apis/arm64-v8a/`.
- The window is 411 x 914 dp at density 2.625, so the profile is 411 x 914 dp.
- `adb shell wm size` prints `Physical size: 1080x2400`; `wm density` prints `Physical density: 420`.
- `adb shell settings get secure navigation_mode` prints `2`: gesture navigation.

## `dumpsys window` (0a debug app in the foreground)

```
mDisplayCutout=DisplayCutout{insets=Rect(0, 132 - 0, 0) ... boundingRect={Bounds=[Rect(0, 0 - 0, 0), Rect(479, 0 - 601, 132), ...]}
InsetsSource id=81050001 type=navigationBars frame=[0,2337][1080,2400] visible=true sideHint=BOTTOM
InsetsSource id=81050004 type=systemGestures frame=[0,0][78,2400] visible=true sideHint=LEFT
InsetsSource id=81050005 type=mandatorySystemGestures frame=[0,2316][1080,2400] visible=true sideHint=BOTTOM
InsetsSource id=81050024 type=systemGestures frame=[1002,0][1080,2400] visible=true sideHint=RIGHT
InsetsSource id=27 type=displayCutout frame=[0,0][1080,132] visible=true sideHint=TOP
InsetsSource id=67c90000 type=statusBars frame=[0,0][1080,132] visible=true sideHint=TOP
InsetsSourceControl: {81050001 mType=navigationBars ... mInsetsHint=Insets{left=0, top=0, right=0, bottom=63}}
InsetsSourceControl: {67c90000 mType=statusBars ... mInsetsHint=Insets{left=0, top=132, right=0, bottom=0}}
```

## Insets

| Side | px | dp (px / 2.625) | Source |
|---|---|---|---|
| top | 132 | 50.29 | `statusBars` and `displayCutout` frames, both 132 px tall |
| left | 0 | 0 | no left system bar |
| bottom | 63 | 24.00 | `navigationBars` frame 2337..2400 (gesture handle) |
| right | 0 | 0 | no right system bar |

`systemGestures` (reference only, not part of the profile): left 78 px (29.71 dp), right 78 px (29.71 dp).

`PixelProfileInsetsTest` (`@Smoke`, emulator) checks that `WindowInsets.safeDrawing` and the root window insets equal these px values, and skips on any other device.

## Profiles

`core/testing/src/main/resources/profiles.json` holds five profiles. Only `pixel8` is measured. `small` and `landscapeSideNav` come from research 13 §4.4, `foldInner` copies the `pixel8` insets and is derived, and `largeFont` is `pixel8` with a font scale of 2.0 (the root cap renders it at 1.3).

## Layout guard matrix (user decision, 2026-11-16)

Robolectric layout guards run in light only, because theme never changes geometry: `small` and `largeFont` in nl, `pixel8` once in en. `foldInner` and `landscapeSideNav` run only for screens with a wide or side-navigation layout. Captures cover light and dark. The helper `LayoutMatrix` in `:core:testing-compose` names these cases.


## Fold check (on request)

`Scripts/fold-check.sh` is **not run in Phase 6e unless requested**. It is not part of `make quick`, `make check` or `make matrix`. The script holds the shared device lock and addresses only the `fold` AVD at `emulator-5556`; it never creates/wipes an AVD, resets another device, changes App Links state or stops `pixel`.

On the first requested run:

```sh
make avd-create AVD=fold                 # only if it does not exist
Scripts/emulator.sh ensure fold
Scripts/fold-check.sh
```

An absent or unbooted `fold` exits 2 naming the ensure command. The script builds/installs debug, uses the existing string intent-extra seed contract with a valid UUID test id, signs into the demo fixture without forms and freezes the route clock with `CF_PLAN_TEST_FREEZE_CLOCK=1`. A preliminary seed observes the opening day (mock dates move with launch time); the measured seed then opens a step from that day's original fixture ids. All evidence identity checks begin after this second launch has settled. No production seed/parser behavior changes.

The sequence is open → folded → open → folded → open. Console fold/unfold commands are attempted first; on rejection, the script parses CLOSED/FOLDED and OPEN/UNFOLDED/FLAT ids from `cmd device_state print-states`. These console/state formats require verification on that first requested run. Every step has a 30 s structural poll cap, with bounded adb calls and a 250 ms poll interval. The poll requires the same day near the page top, the step sheet and the expected phone/wide mode. It checks MainActivity's task/record and process identity and rejects new MainActivity relaunch events; ActivityTaskManager output is also retained for review.

The script uses `uiautomator dump --windows`: the [Android command implementation](https://android.googlesource.com/platform/frameworks/base/+/2940697c9c7d6edecc9ce1d14d9945d92139bf4e/cmds/uiautomator/cmds/uiautomator/src/com/android/commands/uiautomator/DumpCommand.java) enables interactive-window retrieval for that option. An active-window-only dump cannot provide both the sheet and underlying page as evidence. Missing nodes are a failure, not a successful fold claim.

Expected files under `build/captures/fold-check/`:

- `0-open.png`, `1-folded.png`, `2-open.png`, `3-folded.png`, `4-open.png`, with a corresponding XML dump for each;
- `opening-probe.xml` and `presented-step.xml` (pre-journey setup);
- `relaunch-events.log`, `activity-task-manager.log` and `result.json` (status, completed stages, activity/task/process identity).

Only `result.json` with status `passed` and all five freshly listed stages is a completed run. Runtime fold/sheet verification and the five screenshots remain pending until requested; the parser tests exercise synthetic XML/activity/state fixtures without running the fold script or touching adb.

## Phase 14 fold and resize handoff (2026-11-22)

Status: **pending (orchestrator/user)**. The worker did not create a T2 AVD, boot fold, run the fold script or run resize on a device. `Scripts/fold-check.sh` and its implementation remain unchanged. After the existing fold procedure above, run:

```sh
python3 -I Scripts/device_lock.py -- python3 -I Scripts/resize-check.py
```

The resize procedure addresses only `fold` / `emulator-5556`. It reads the active density (override wins), resolves an opening-day block from the existing debug mock fixture, then seeds that step sheet. It applies `split-narrow` 320 × 700 dp and `landscape-short` 800 × 360 dp as rounded physical pixels, requiring the same visible day anchor, `step.sheet`, `trip.page` and `trip.scroll`, no launch overlay, stable task/record/PID and no new MainActivity relaunch events. It waits for consecutive matching tagged geometry and always resets `wm size` in `finally`. Output: `build/captures/resize/{split-narrow,landscape-short}.{xml,png}` and `result.json`; both profile rows and `sizeReset=true` are required. These are planned checks, not device results.

At 420 dpi the requested sizes are 840×1838 and 2100×945 px; at 390 dpi they are 780×1706 and 1950×878 px. Eight fake-device/parser tests cover density and px conversion, retained nodes/anchor, overlay rejection and reset after a failure. The reset guard first exposed an incorrect fake-adb command suffix; after correcting the fixture it verifies a real size override before the failure and reset afterward.

Real split screen: **pending (orchestrator/user)**. Under the device lock, try `adb -s emulator-5556 shell am start --windowingMode 3 -n net.chronoflow.app.native.debug/net.chronoflow.app.MainActivity` after opening a trip sheet. If the shell cannot enter split screen, use Overview’s Split screen action and select a second app. Resize the divider to a width ≥280 dp; confirm the day anchor and open step sheet survive. Record the successful method and one screenshot in `build/captures/resize/real-split.png`. A `wm size` override alone is not evidence of real split screen.

T2 creation/matrix commands remain with the orchestrator: `make avd-create AVD=fold`, `make avd-create AVD=small`, `make avd-create AVD=api29`, `make matrix`. Record per-AVD results and `make du` before/after the downloads; none is inferred from JVM profiles.
