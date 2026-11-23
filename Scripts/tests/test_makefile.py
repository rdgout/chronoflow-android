import re
import unittest

from helpers import ROOT

DEVICE_MARKERS = re.compile(r"connected\w*AndroidTest|androidTest-results/connected|am instrument")


def recipes(text):
    joined = text.replace("\\\n", " ")
    return [line for line in joined.splitlines() if line.startswith("\t") or re.match(r"^\w[\w-]*:", line)]


class MakefileTest(unittest.TestCase):
    def test_permission_dialog_is_excluded_then_runs_alone_with_apks_kept(self):
        text = (ROOT / "Makefile").read_text()
        self.assertIn("notClass=net.chronoflow.app.reminders.RemindersPermissionDeviceTest", text)
        self.assertIn("leaveApksInstalledAfterRun=true", text)
        self.assertIn("reminders-permission --device --emulator", text)

    def test_device_recipes_take_the_lock(self):
        text = (ROOT / "Makefile").read_text()
        offenders = [
            line.strip()
            for line in recipes(text)
            if DEVICE_MARKERS.search(line) and "--device" not in line and "device_lock.py" not in line
        ]
        self.assertEqual(offenders, [])


if __name__ == "__main__":
    unittest.main()
