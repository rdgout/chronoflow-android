import sys
import unittest

from helpers import ROOT, run

TARGETED = str(ROOT / "Scripts" / "targeted.py")


def targeted(*args, env=None):
    return run([sys.executable, "-I", TARGETED, "--print", *args], env, cwd=str(ROOT))


class TargetedTest(unittest.TestCase):
    def test_jvm_test_uses_tests_filter(self):
        result = targeted("konsist:KonsistRulesTest#noGlobalScope")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(":konsist:test -Pcf.tier=targeted --tests '*.KonsistRulesTest.noGlobalScope'", result.stdout)
        self.assertIn("--timings 'konsist/build/cf-timings/test/*.json'", result.stdout)
        self.assertIn("run-tier.sh targeted --results", result.stdout)

    def test_build_logic_uses_xml_without_an_unavailable_timing_listener(self):
        result = targeted("build-logic:convention:VerifyReleaseManifestTest")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(":build-logic:convention:test", result.stdout)
        self.assertIn("build-logic/convention/build/test-results/test/*.xml", result.stdout)
        self.assertNotIn("--timings", result.stdout)

    def test_emulator_test_runs_through_the_device_tier(self):
        result = targeted("app:AppLaunchSmokeTest", env={"REUSE_BUILD": ""})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("run-tier.sh targeted-emu --device --emulator", result.stdout)
        self.assertIn(":app:connectedDebugAndroidTest", result.stdout)
        self.assertIn("class=net.chronoflow.app.AppLaunchSmokeTest", result.stdout)

    def test_reuse_build_instruments_under_the_lock(self):
        result = targeted("app:AppLaunchSmokeTest", env={"REUSE_BUILD": "1"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(result.stdout.startswith("python3 -I Scripts/device_lock.py --"), result.stdout)
        self.assertIn("am instrument", result.stdout)
        self.assertIn("class net.chronoflow.app.AppLaunchSmokeTest", result.stdout)
        self.assertIn("net.chronoflow.app.debug.test/androidx.test.runner.AndroidJUnitRunner", result.stdout)

    def test_permission_dialog_runs_alone_after_install_and_external_revoke(self):
        result = targeted("app:RemindersPermissionDeviceTest", env={"REUSE_BUILD": ""})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("targeted-permission --device --emulator", result.stdout)
        self.assertIn("reminders_permission.py --install", result.stdout)
        self.assertIn("build/reminders-permission/result.xml", result.stdout)

    def test_reused_permission_dialog_preserves_the_installed_apks(self):
        result = targeted("app:RemindersPermissionDeviceTest", env={"REUSE_BUILD": "1"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("reminders_permission.py", result.stdout)
        self.assertNotIn("--install", result.stdout)

    def test_permission_dialog_cannot_share_an_instrumentation_run(self):
        result = targeted("app:RemindersPermissionDeviceTest", "app:AppLaunchSmokeTest")
        self.assertEqual(result.returncode, 2)
        self.assertIn("requires a revoked permission", result.stderr)

    def test_release_source_set_runs_through_the_release_task(self):
        result = targeted("app:ReleaseAppIdentityTest")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(":app:testReleaseUnitTest -Pcf.tier=targeted --tests '*.ReleaseAppIdentityTest'", result.stdout)

    def test_unknown_module_exits_2(self):
        self.assertEqual(targeted("nope:X").returncode, 2)

    def test_unknown_class_exits_2(self):
        self.assertEqual(targeted("konsist:NoSuchClass").returncode, 2)


if __name__ == "__main__":
    unittest.main()
