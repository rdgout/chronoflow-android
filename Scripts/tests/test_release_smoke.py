import unittest

from helpers import SCRIPTS, run, temp_dir

ADB = r'''#!/bin/bash
echo "adb $*" >> "$FAKE_LOG"
case "$*" in
 devices*) printf 'List of devices attached\nemulator-5554\tdevice\n' ;;
 get-serialno*) echo emulator-5554 ;;
 *getprop*qemu*) echo "${FAKE_QEMU-1}" ;;
esac
'''


class ReleaseSmokeTest(unittest.TestCase):
    def setUp(self):
        self.root = temp_dir(self)
        self.log = self.root / "calls.log"
        self.log.write_text("")
        stubs = {
            "adb": ADB,
            "gradlew": '#!/bin/bash\necho "gradlew $*" >> "$FAKE_LOG"\nexit "${FAKE_GRADLE_EXIT:-0}"\n',
            "emulator.sh": '#!/bin/bash\necho "emulator.sh $*" >> "$FAKE_LOG"\n',
        }
        for name, source in stubs.items():
            file = self.root / name
            file.write_text(source)
            file.chmod(0o755)

    def smoke(self, **extra):
        env = {"ADB": str(self.root / "adb"), "GRADLEW": str(self.root / "gradlew"), "EMULATOR_SH": str(self.root / "emulator.sh"), "FAKE_LOG": str(self.log), "ANDROID_SERIAL": "", "PERF_CLEAR": "0"}
        return run([str(SCRIPTS / "release-smoke.sh")], env | extra)

    def test_dry_run_selects_only_the_plain_smoke_class(self):
        result = self.smoke(RELEASE_SMOKE_DRY_RUN="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("class=net.chronoflow.benchmark.ReleaseSmokeTest", result.stdout)
        self.assertIn("timeout_msec=60000", result.stdout)
        self.assertNotIn("enabledRules", result.stdout)
        self.assertNotIn("suppressErrors", result.stdout)
        self.assertNotIn("gradlew", self.log.read_text())

    def test_gradle_status_is_preserved(self):
        result = self.smoke(FAKE_GRADLE_EXIT="7")
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertTrue(self.log.read_text().splitlines()[-1].startswith("emulator.sh prep-functional"))

    def test_physical_device_refuses_without_clear_opt_in(self):
        result = self.smoke(FAKE_QEMU="")
        self.assertEqual(result.returncode, 2)
        self.assertIn("release-smoke clears app data: set PERF_CLEAR=1", result.stderr)
        self.assertNotIn("gradlew", self.log.read_text())

    def test_physical_opt_in_reaches_the_test_guard(self):
        result = self.smoke(FAKE_QEMU="", PERF_CLEAR="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("cf.perfClear=1", self.log.read_text())
