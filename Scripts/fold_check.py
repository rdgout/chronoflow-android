"""On-request fold journey; its shell entry holds the device lock. No other AVD is touched."""
import collections
import datetime
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
SERIAL = "emulator-5556"
PACKAGE = "net.chronoflow.app.debug"
ACTIVITY = PACKAGE + "/net.chronoflow.app.MainActivity"
RECEIVER = PACKAGE + "/net.chronoflow.debug.SeedReceiverActivity"
TRIP = "0199a3c0-0000-7000-8000-000000000001"
TEST_ID = "019a56e0-0000-7000-8000-00000000006e"
REMOTE_XML = "/sdcard/cf-fold-check.xml"
STAGES = ("0-open", "1-folded", "2-open", "3-folded", "4-open")


def bounds(value):
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", value)
    if not match:
        raise ValueError("missing node bounds")
    return tuple(map(int, match.groups()))


def anchor_from_dump(xml, expected_day=None, require_sheet=True, wide=None):
    nodes = list(ET.fromstring(xml).iter("node"))
    tagged = {n.get("resource-id", ""): n for n in nodes}
    if require_sheet and "step.sheet" not in tagged:
        raise ValueError("step.sheet is absent")
    page = tagged.get("trip.scroll")
    if page is None:
        raise ValueError("trip.scroll is absent from the all-window dump")
    viewport = bounds(page.get("bounds", ""))
    days = []
    for tag, node in tagged.items():
        if tag.startswith("trip.day."):
            rect = bounds(node.get("bounds", ""))
            if rect[2] > rect[0] and rect[3] > viewport[1]:
                days.append((max(rect[1], viewport[1]), tag, rect))
    if not days:
        raise ValueError("no visible day in the all-window dump")
    _, tag, rect = min(days)
    if expected_day is not None and tag != expected_day:
        raise ValueError("the opening day changed: " + tag)
    if rect[1] > viewport[1] + (viewport[3] - viewport[1]) / 2:
        raise ValueError("the opening day is no longer near the page top")
    has_list = "trip.wide.list." + tag.removeprefix("trip.day.") in tagged
    if wide is not None and has_list != wide:
        raise ValueError("the window has not reached the requested width mode")
    return tag


def block_for_day(template, tag):
    day_id = tag.removeprefix("trip.day.")
    plan = template["detail"]["plan"]
    day = next(d for d in plan["days"] if d["id"] == day_id)
    instant = lambda text: datetime.datetime.fromisoformat(text.replace("Z", "+00:00"))
    start, end = instant(day["startUtc"]), instant(day["endUtc"])
    return next(b["id"] for b in plan["blocks"]
                if instant(b["startUtc"]) < end and instant(b["endUtc"]) > start
                and b["type"] != "zone_change")


def activity_record(text):
    component = re.escape(ACTIVITY)
    match = re.search(r"ActivityRecord\{([0-9a-f]+)\s+u\d+\s+" + component + r"\s+t(\d+)", text)
    if not match:
        raise ValueError("MainActivity record/task identity is absent")
    return match.group(1), match.group(2)


def state_id(text, folded):
    patterns = (r"\bCLOSED\b", r"\bFOLDED\b") if folded else (r"\bOPEN(?:ED)?\b", r"\bUNFOLDED\b", r"\bFLAT\b")
    for pattern in patterns:
        for line in text.splitlines():
            if "HALF" in line.upper() or not re.search(pattern, line, re.I):
                continue
            match = re.search(r"(?:identifier\s*[=:]\s*|^\s*)(\d+)", line)
            if match:
                return match.group(1)
    raise ValueError("no closed/open device state in cmd device_state print-states:\n" + text)


def relaunches(text):
    return collections.Counter(line for line in text.splitlines()
                               if PACKAGE in line and "MainActivity" in line)


class FoldCheck:
    def __init__(self):
        sdk = os.environ.get("ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools")
        self.adb = os.environ.get("ADB", sdk + "/platform-tools/adb")
        self.output = ROOT / "build/captures/fold-check"
        self.completed = []
        self.record = None
        self.pid = None
        self.events = None

    def dev(self, *args, timeout=15, binary=False, check=True):
        command = [self.adb, "-s", SERIAL, *args]
        result = subprocess.run(command, capture_output=True, text=not binary, timeout=timeout)
        if check and result.returncode:
            detail = result.stderr.decode(errors="replace") if binary else result.stderr
            raise RuntimeError("command failed: " + " ".join(command) + "\n" + detail)
        return result

    def require_fold(self):
        try:
            booted = self.dev("shell", "getprop", "sys.boot_completed").stdout.strip() == "1"
            name = self.dev("emu", "avd", "name").stdout.splitlines()[0].strip()
        except (OSError, RuntimeError, subprocess.TimeoutExpired, IndexError):
            booted, name = False, ""
        if not booted or name != "fold":
            print("fold-check: boot the fold AVD first: Scripts/emulator.sh ensure fold", file=sys.stderr)
            raise SystemExit(2)

    def build_and_install(self):
        command = ["./gradlew", "--console=plain", ":app:assembleDebug"]
        try:
            subprocess.run(command, cwd=ROOT, check=True, timeout=180)
        except subprocess.TimeoutExpired:
            subprocess.run(["./gradlew", "--stop"], cwd=ROOT, check=True, timeout=30)
            subprocess.run(command, cwd=ROOT, check=True, timeout=180)
        self.dev("install", "-r", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk"), timeout=60)

    def pose(self, folded):
        action = "fold" if folded else "unfold"
        result = self.dev("emu", action, check=False)
        if result.returncode or re.search(r"\b(?:KO|ERROR)\b", result.stdout + result.stderr):
            states = self.dev("shell", "cmd", "device_state", "print-states").stdout
            self.dev("shell", "cmd", "device_state", "state", state_id(states, folded))

    def seed(self, block=None):
        self.dev("shell", "am", "force-stop", PACKAGE)
        extras = {
            "CF_UI_TEST_PROFILE": "screen", "CF_AUTH_TEST_ID": TEST_ID,
            "CF_AUTH_TEST_SIGNED_IN": "demo", "CF_AUTH_TEST_WALKTHROUGH_SEEN": "1",
            "CF_PLAN_TEST_ROUTE": "trip:" + TRIP, "CF_PLAN_TEST_FREEZE_CLOCK": "1",
            "CF_MOCK_DELAY": "0", "cf.reduceMotion": "1", "cf.settings.language": "en",
        }
        if block is not None:
            extras["CF_PLAN_TEST_SHEET"] = "step:" + block
        arguments = ["shell", "am", "start", "-W", "-n", RECEIVER]
        for key, value in extras.items():
            arguments.extend(("--es", key, value))
        self.dev(*arguments, timeout=30)

    def wait_dump(self, name, day=None, sheet=True, wide=True):
        deadline = time.monotonic() + 30
        reason = "no dump"
        while time.monotonic() < deadline:
            try:
                remaining = deadline - time.monotonic()
                self.dev("shell", "uiautomator", "dump", "--windows", REMOTE_XML, timeout=min(12, remaining))
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    break
                xml = self.dev("exec-out", "cat", REMOTE_XML, timeout=min(5, remaining)).stdout
                current = anchor_from_dump(xml, day, sheet, wide)
                (self.output / (name + ".xml")).write_text(xml)
                return current
            except (ValueError, RuntimeError, ET.ParseError, subprocess.TimeoutExpired) as error:
                reason = str(error)
                time.sleep(min(0.25, max(0, deadline - time.monotonic())))
        raise RuntimeError(name + " did not settle within 30 s: " + reason)

    def identity(self):
        text = self.dev("shell", "dumpsys", "activity", "activities").stdout
        return activity_record(text), self.dev("shell", "pidof", PACKAGE).stdout.strip()

    def event_log(self):
        return self.dev("logcat", "-d", "-b", "events", "-v", "brief", "-s",
                        "am_relaunch_activity", "am_relaunch_resume_activity").stdout

    def capture(self, name, day, wide):
        self.wait_dump(name, day, wide=wide)
        identity, pid = self.identity()
        if identity != self.record or pid != self.pid:
            raise RuntimeError("MainActivity task/record/process changed during " + name)
        events = self.event_log()
        (self.output / "relaunch-events.log").write_text(events)
        if relaunches(events) - self.events:
            raise RuntimeError("MainActivity relaunched during " + name)
        png = self.dev("exec-out", "screencap", "-p", binary=True).stdout
        if not png.startswith(b"\x89PNG\r\n\x1a\n"):
            raise RuntimeError("no PNG from screencap for " + name)
        (self.output / (name + ".png")).write_bytes(png)
        self.completed.append(name)
        self.manifest("running")

    def manifest(self, status, error=None):
        (self.output / "result.json").write_text(json.dumps({
            "status": status, "serial": SERIAL, "stages": self.completed,
            "activityRecordAndTask": self.record, "pid": self.pid, "error": error,
        }, indent=2) + "\n")

    def run(self):
        self.require_fold()
        self.output.mkdir(parents=True, exist_ok=True)
        self.manifest("running")
        self.build_and_install()
        self.pose(False)
        # Probe before the measured journey: date rebasing keeps ids, but the opening day depends on launch time.
        self.seed()
        day = self.wait_dump("opening-probe", sheet=False)
        templates = json.loads((ROOT / "core/testing/src/main/resources/fixtures/mock-seed.json").read_text())["trips"]
        template = next(t for t in templates if t["card"]["id"] == TRIP)
        self.seed(block_for_day(template, day))
        self.wait_dump("presented-step", day)
        self.record, self.pid = self.identity()
        self.events = relaunches(self.event_log())
        self.capture(STAGES[0], day, True)
        self.pose(True)
        self.capture(STAGES[1], day, False)
        self.pose(False)
        self.capture(STAGES[2], day, True)
        self.pose(True)
        self.capture(STAGES[3], day, False)
        self.pose(False)
        self.capture(STAGES[4], day, True)
        logs = self.dev("logcat", "-d", "-s", "ActivityTaskManager").stdout
        (self.output / "activity-task-manager.log").write_text(logs)
        self.manifest("passed")
        print("fold-check: PASS; five screenshots and XML dumps in " + str(self.output))


def main():
    check = FoldCheck()
    try:
        check.run()
        return 0
    except (OSError, ValueError, RuntimeError, StopIteration, subprocess.SubprocessError) as error:
        if check.output.exists():
            check.manifest("failed", str(error))
        print("fold-check: " + str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
