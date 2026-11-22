#!/usr/bin/env python3
"""Runs the notification-dialog class alone, under the caller's device lock."""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = "net.chronoflow.app.native.debug"
TEST_CLASS = "net.chronoflow.app.reminders.RemindersPermissionDeviceTest"
RUNNER = APP + ".test/androidx.test.runner.AndroidJUnitRunner"
REPORT = ROOT / "build/reminders-permission/result.xml"


def result(output, code):
    metric = re.search(r"(?m)^Time:\s*([0-9]+(?:\.[0-9]+)?)\s*$", output)
    seconds = float(metric[1]) if metric else None
    if code or "FAILURES!!!" in output or "INSTRUMENTATION_FAILED" in output:
        return seconds, "permission instrumentation failed"
    if not re.search(r"(?m)^OK \(1 test\)\s*$", output):
        return seconds, "permission instrumentation produced no single-test success result"
    if seconds is None:
        return None, "permission instrumentation produced no time metric"
    if seconds >= 10:
        return seconds, "permission instrumentation exceeded 10 seconds"
    return seconds, None


def write_report(seconds, failure):
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    suite = ET.Element("testsuite", name=TEST_CLASS, tests="1", failures=str(int(failure is not None)))
    case = ET.SubElement(suite, "testcase", classname=TEST_CLASS,
                         name="askOpensTheSystemDialogAndTurnsRemindersOn", time=str(seconds or 0))
    if failure:
        ET.SubElement(case, "failure", message=failure).text = failure
    ET.ElementTree(suite).write(REPORT, encoding="utf-8", xml_declaration=True)


def main(argv):
    if os.environ.get("CF_DEVICE_LOCK_HELD") != "1":
        print("reminders-permission: run inside Scripts/device_lock.py or a --device tier", file=sys.stderr)
        return 2
    adb = os.environ.get("ADB") or str(Path(os.environ.get(
        "ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools")) / "platform-tools/adb")
    shell = [adb, "-s", os.environ.get("ANDROID_SERIAL", "emulator-5554"), "shell"]
    started = time.monotonic()
    failure = None
    seconds = None
    try:
        if "--install" in argv:
            code = subprocess.call(["./gradlew", "--console=plain", ":app:installDebug", ":app:installDebugAndroidTest"], cwd=ROOT)
            if code:
                raise RuntimeError("permission APK install failed")
        package = subprocess.run(shell + ["pm", "path", APP], capture_output=True, text=True, timeout=15)
        print(package.stdout + package.stderr, end="", flush=True)
        if package.returncode or not package.stdout.startswith("package:"):
            raise RuntimeError("debug APK missing; leaveApksInstalledAfterRun was not honoured")
        for args in (["pm", "revoke", APP, "android.permission.POST_NOTIFICATIONS"],
                     ["pm", "clear-permission-flags", APP, "android.permission.POST_NOTIFICATIONS", "user-set", "user-fixed"]):
            command = subprocess.run(shell + args, capture_output=True, text=True, timeout=15)
            print(command.stdout + command.stderr, end="", flush=True)
            if command.returncode:
                raise RuntimeError("permission preparation failed")
        run = subprocess.run(shell + ["am", "instrument", "-w", "-r", "-e", "class", TEST_CLASS,
                                      "-e", "timeout_msec", "60000", RUNNER], capture_output=True, text=True, timeout=90)
        output = run.stdout + run.stderr
        print(output, end="", flush=True)
        seconds, failure = result(output, run.returncode)
    except (OSError, subprocess.TimeoutExpired, RuntimeError) as error:
        failure = str(error)
    write_report(seconds, failure)
    print("reminders-permission: %s (runner=%s s, wall=%.3f s)" %
          (failure or "PASS", seconds, time.monotonic() - started), flush=True)
    return 1 if failure else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
