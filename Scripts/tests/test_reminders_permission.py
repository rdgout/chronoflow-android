import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from helpers import ROOT

spec = importlib.util.spec_from_file_location("reminders_permission", ROOT / "Scripts/reminders_permission.py")
permission = importlib.util.module_from_spec(spec)
spec.loader.exec_module(permission)


class RemindersPermissionTest(unittest.TestCase):
    def test_one_success_with_a_metric_passes(self):
        self.assertEqual((2.5, None), permission.result("Time: 2.5\n\nOK (1 test)\n", 0))

    def test_zero_adb_exit_does_not_hide_instrumentation_failure(self):
        self.assertIsNotNone(permission.result("Time: 2.5\nFAILURES!!!\n", 0)[1])

    def test_missing_success_fails(self):
        self.assertIsNotNone(permission.result("Time: 2.5\n", 0)[1])

    def test_missing_metric_fails(self):
        self.assertIsNotNone(permission.result("OK (1 test)\n", 0)[1])

    def test_over_budget_fails(self):
        self.assertIsNotNone(permission.result("Time: 10.01\nOK (1 test)\n", 0)[1])

    def test_no_device_lock_refuses_to_run(self):
        with patch.dict("os.environ", {"CF_DEVICE_LOCK_HELD": ""}), patch.object(permission.subprocess, "run") as run:
            self.assertEqual(2, permission.main([]))
            run.assert_not_called()

    def test_missing_installed_apk_fails_and_writes_evidence(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict("os.environ", {"CF_DEVICE_LOCK_HELD": "1"}), \
                patch.object(permission, "REPORT", Path(directory) / "result.xml"), \
                patch.object(permission.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "", "")):
            self.assertEqual(1, permission.main([]))
            self.assertIn("leaveApksInstalledAfterRun", permission.REPORT.read_text())

    def test_revoke_and_clear_flags_precede_the_lone_instrumentation(self):
        replies = [subprocess.CompletedProcess([], 0, "package:/debug.apk\n", ""),
                   subprocess.CompletedProcess([], 0, "", ""), subprocess.CompletedProcess([], 0, "", ""),
                   subprocess.CompletedProcess([], 0, "Time: 2.5\nOK (1 test)\n", "")]
        with tempfile.TemporaryDirectory() as directory, patch.dict("os.environ", {"CF_DEVICE_LOCK_HELD": "1"}), \
                patch.object(permission, "REPORT", Path(directory) / "result.xml"), \
                patch.object(permission.subprocess, "run", side_effect=replies) as run:
            self.assertEqual(0, permission.main([]))
            self.assertIn("revoke", run.call_args_list[1].args[0])
            self.assertIn("clear-permission-flags", run.call_args_list[2].args[0])
            self.assertIn(permission.TEST_CLASS, run.call_args_list[3].args[0])
            self.assertIn('time="2.5"', permission.REPORT.read_text())

    def test_audit_defaults_are_forwarded_to_the_isolated_class(self):
        command = permission.instrumentation_command(["adb-shell"], audit=True)
        self.assertEqual(["cf.audit.language", "nl", "-e", "cf.audit.theme", "dark", "-e",
                          "cf.audit.reduceMotion", "1", permission.RUNNER], command[-9:])
        self.assertIn(permission.TEST_CLASS, command)
        self.assertNotIn("cf.audit.theme", permission.instrumentation_command(["adb-shell"]))
