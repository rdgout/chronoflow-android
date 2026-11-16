import os
import unittest

from helpers import SCRIPTS, LockHolder, run, temp_dir

EMULATOR_SH = str(SCRIPTS / "emulator.sh")

SHIM = """#!/bin/bash
echo "$(basename "$0") $*" >> "$FAKE_LOG"
%s
"""

ADB_BODY = r"""case "$*" in
  devices*) printf 'List of devices attached\n%b\n' "${FAKE_DEVICES:-}" ;;
  *getprop*sys.boot_completed*) echo "${FAKE_BOOT-1}" ;;
esac"""


class EmulatorTest(unittest.TestCase):
    def setUp(self):
        self.tmp = temp_dir(self)
        self.bin = self.tmp / "bin"
        self.bin.mkdir()
        self.log = self.tmp / "calls.log"
        self.log.write_text("")
        for name, body in {
            "adb": ADB_BODY,
            "emulator": "true",
            "avdmanager": 'case "$*" in "list avd -c") ;; esac',
            "sdkmanager": "true",
            "pgrep": "exit 1",
        }.items():
            path = self.bin / name
            path.write_text(SHIM % body)
            path.chmod(0o755)
        self.env = {
            "ADB": str(self.bin / "adb"),
            "EMULATOR": str(self.bin / "emulator"),
            "AVDMANAGER": str(self.bin / "avdmanager"),
            "SDKMANAGER": str(self.bin / "sdkmanager"),
            "PGREP": str(self.bin / "pgrep"),
            "FAKE_LOG": str(self.log),
            "CF_DEVICE_LOCK": str(self.tmp / "lock"),
            "CF_DEVICE_LOCK_HELD": "",
            "FAKE_DEVICES": "",
        }

    def calls(self):
        return self.log.read_text().splitlines()

    def test_wipe_refuses_while_gradle_tests_run(self):
        (self.bin / "pgrep").write_text(SHIM % "exit 0")
        result = run([EMULATOR_SH, "wipe"], self.env)
        self.assertEqual(result.returncode, 1)
        self.assertIn("tests are running", result.stderr)
        self.assertFalse(any(c.startswith("emulator ") for c in self.calls()))

    def test_ensure_skips_boot_when_booted(self):
        env = dict(self.env, FAKE_DEVICES="emulator-5554\\tdevice")
        result = run([EMULATOR_SH, "ensure"], env)
        self.assertEqual(result.returncode, 0, result.stderr)
        calls = self.calls()
        self.assertFalse(any(c.startswith("emulator ") for c in calls))
        for expected in (
            "shell settings put global window_animation_scale 0",
            "shell settings put global transition_animation_scale 0",
            "shell settings put global animator_duration_scale 0",
            "shell settings put secure show_ime_with_hard_keyboard 1",
            "shell settings put secure autofill_service null",
            "shell cmd uimode night no",
            "shell input keyevent 82",
        ):
            self.assertIn("adb -s emulator-5554 " + expected, calls)

    def test_ensure_fails_fast_when_the_emulator_dies_at_start(self):
        (self.bin / "emulator").write_text(SHIM % "echo boom; exit 1")
        env = dict(self.env, FAKE_DEVICES="", FAKE_BOOT="")
        result = run([EMULATOR_SH, "ensure"], env, timeout=20)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("exited before boot", result.stderr)
        self.assertLess(result.elapsed, 15)

    def test_create_rejects_unknown_avd(self):
        self.assertEqual(run([EMULATOR_SH, "create", "tablet"], self.env).returncode, 2)

    def test_create_api29_installs_the_image_first(self):
        result = run([EMULATOR_SH, "create", "api29"], self.env)
        self.assertEqual(result.returncode, 0, result.stderr)
        order = [c.split()[0] for c in self.calls() if c.startswith(("sdkmanager", "avdmanager create"))]
        self.assertEqual(order[:2], ["sdkmanager", "avdmanager"])

    def test_busy_while_the_lock_is_held(self):
        holder = LockHolder(self.tmp / "lock")
        self.addCleanup(holder.close)
        self.assertEqual(run([EMULATOR_SH, "busy"], self.env).returncode, 0)
        self.assertEqual(run([EMULATOR_SH, "wipe"], self.env).returncode, 1)
        self.assertFalse(any(c.startswith("emulator ") for c in self.calls()))


if __name__ == "__main__":
    unittest.main()
