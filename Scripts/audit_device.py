"""Bounded device operations shared by the opt-in audit commands."""
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "net.chronoflow.app.debug"
RECEIVER = PACKAGE + "/net.chronoflow.debug.SeedReceiverActivity"
ACTIVITY = PACKAGE + "/net.chronoflow.app.MainActivity"
TRIP = "0199a3c0-0000-7000-8000-000000000001"


class DeviceFailure(RuntimeError):
    pass


def bounds(raw):
    match = re.fullmatch(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", raw)
    if not match:
        raise ValueError("invalid node bounds")
    return tuple(map(int, match.groups()))


def nodes(xml):
    return {node.get("resource-id", "").split("/")[-1]: node
            for node in ET.fromstring(xml).iter("node") if node.get("resource-id")}


def first_sleep_block():
    import datetime
    import json
    templates = json.loads((ROOT / "core/testing/src/main/resources/fixtures/mock-seed.json").read_text())
    plan = next(t for t in templates["trips"] if t["card"]["id"] == TRIP)["detail"]["plan"]
    day = plan["days"][0]
    instant = lambda value: datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
    return next(b["id"] for b in plan["blocks"] if b["type"] == "sleep"
                and instant(b["startUtc"]) < instant(day["endUtc"])
                and instant(b["endUtc"]) > instant(day["startUtc"]))


class Device:
    def __init__(self, serial=None, run=subprocess.run, now=time.monotonic, sleep=time.sleep):
        sdk = os.environ.get("ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools")
        self.adb = os.environ.get("ADB") or shutil.which("adb") or sdk + "/platform-tools/adb"
        self.serial = serial or os.environ.get("ANDROID_SERIAL", "emulator-5554")
        self.run_command, self.now, self.sleep = run, now, sleep

    def command(self, *args, timeout=20, binary=False, check=True):
        try:
            result = self.run_command([self.adb, "-s", self.serial, *args], capture_output=True,
                                      text=not binary, timeout=timeout)
        except subprocess.TimeoutExpired:
            # Commands can contain form secrets in the process-death procedure.
            raise DeviceFailure("adb operation timed out") from None
        if check and result.returncode:
            raise DeviceFailure("adb operation failed (exit " + str(result.returncode) + ")")
        return result

    def require(self):
        try:
            state = self.command("get-state").stdout.strip()
        except (DeviceFailure, OSError):
            state = ""
        if state != "device":
            raise DeviceFailure("no booted device; run Scripts/emulator.sh ensure")

    def dump(self, timeout=15):
        remote = "/sdcard/cf-audit-window.xml"
        started = self.now()
        self.command("shell", "uiautomator", "dump", "--windows", remote, timeout=timeout)
        remaining = max(0.1, timeout - (self.now() - started))
        return self.command("exec-out", "cat", remote, timeout=remaining).stdout

    def wait(self, predicate, timeout=20, message="condition was not reached"):
        deadline = self.now() + timeout
        while self.now() < deadline:
            try:
                value = predicate(max(0.1, deadline - self.now()))
                if value:
                    return value
            except (DeviceFailure, ET.ParseError):
                pass
            self.sleep(min(0.5, max(0, deadline - self.now())))
        raise DeviceFailure(message)

    def wait_tag(self, tag, timeout=20):
        def present(remaining):
            xml = self.dump(timeout=min(15, remaining))
            return xml if tag in nodes(xml) else None
        return self.wait(present, timeout, "tag not reached within " + str(timeout) + " s: " + tag)

    def launch(self, seeds, profile="screen", extras=None, test_id=None):
        self.command("shell", "am", "force-stop", PACKAGE)
        values = {"CF_AUTH_TEST_ID": test_id or str(uuid.uuid4()), "CF_UI_TEST_PROFILE": profile,
                  **seeds, **(extras or {})}
        command = ["shell", "am", "start", "-W", "-n", RECEIVER]
        for key, value in values.items():
            command.extend(("--es", key, str(value)))
        self.command(*command, timeout=30)
        return values

    def screenshot(self, path):
        png = self.command("exec-out", "screencap", "-p", binary=True).stdout
        if not png.startswith(b"\x89PNG\r\n\x1a\n") or len(png) < 24:
            raise DeviceFailure("screencap produced no PNG")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)

    def uptime(self):
        try:
            return float(self.command("shell", "cat", "/proc/uptime").stdout.split()[0])
        except (ValueError, IndexError):
            raise DeviceFailure("device uptime is unavailable") from None

    def tap(self, tag):
        node = nodes(self.wait_tag(tag))[tag]
        left, top, right, bottom = bounds(node.get("bounds", ""))
        self.command("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
