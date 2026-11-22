import hashlib
import unittest

from helpers import SCRIPTS, run, temp_dir

ADB = r'''#!/bin/bash
echo "adb $*" >> "$FAKE_LOG"
case "$*" in
 devices*) printf 'List of devices attached\nemulator-5554\tdevice\n' ;;
 get-serialno*) echo emulator-5554 ;;
 *getprop*qemu*) echo "${FAKE_QEMU-1}" ;;
 *animator_duration_scale*) echo 1.0 ;;
 *font_scale*) echo 1.0 ;;
 *am\ start*) printf 'Status: ok\nTotalTime: 111\n' ;;
esac
'''


class PerfCompareRnTest(unittest.TestCase):
    def setUp(self):
        self.root = temp_dir(self)
        self.log = self.root / "calls.log"
        self.log.write_text("")
        self.apk = self.root / "rn.apk"
        self.apk.write_bytes(b"fixture RN release APK")
        self.sha = hashlib.sha256(self.apk.read_bytes()).hexdigest()
        self.state = self.root / "state"
        self.state.mkdir()
        (self.state / "gpu-mode").write_text("host\n")
        self.history = self.root / "history.md"
        self.history.write_text("# Fixture history\n")
        for name, text in {
            "adb": ADB,
            "gradlew": '#!/bin/bash\necho "gradlew $*" >> "$FAKE_LOG"\nexit "${FAKE_GRADLE_EXIT:-0}"\n',
            "checker": '#!/bin/bash\necho "checker $*" >> "$FAKE_LOG"\nexit "${FAKE_CHECK_EXIT:-0}"\n',
            "emulator.sh": '#!/bin/bash\necho "emulator.sh $*" >> "$FAKE_LOG"\n',
        }.items():
            file = self.root / name
            file.write_text(text)
            file.chmod(0o755)

    def perf(self, **extra):
        env = {"ADB": str(self.root / "adb"), "GRADLEW": str(self.root / "gradlew"), "EMULATOR_SH": str(self.root / "emulator.sh"), "CHECK_BENCHMARKS": str(self.root / "checker"), "FAKE_LOG": str(self.log), "ANDROID_SERIAL": "", "PERF_CLEAR": "0", "RN_APK": str(self.apk), "RN_APK_SHA": self.sha, "CF_EMULATOR_STATE_DIR": str(self.state), "PERF_OUTPUT_DIR": str(self.root / "output"), "PERF_ARCHIVE_ROOT": str(self.root / "archives"), "PERF_COMPARE_HISTORY": str(self.history), "RN_NOT_PROFILEABLE": "0", "RN_AM_START_FALLBACK": "0"}
        return run([str(SCRIPTS / "perf-compare-rn.sh")], env | extra)

    def test_missing_apk_refuses_before_install_or_gradle(self):
        result = self.perf(RN_APK=str(self.root / "missing.apk"))
        self.assertEqual(result.returncode, 3)
        self.assertIn("OQ-A591", result.stderr)
        self.assertNotIn("install -r", self.log.read_text())
        self.assertNotIn("gradlew", self.log.read_text())

    def test_wrong_hash_refuses_before_install(self):
        result = self.perf(RN_APK_SHA="0" * 64)
        self.assertEqual(result.returncode, 3)
        self.assertNotIn("install -r", self.log.read_text())

    def test_dry_run_prints_verified_sha_and_emulator_suppression(self):
        result = self.perf(PERF_COMPARE_RN_DRY_RUN="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(self.sha, result.stdout)
        self.assertIn("suppressErrors=EMULATOR", result.stdout)
        self.assertIn("LaunchComparisonBenchmark", result.stdout)
        self.assertIn("--rn-apk-sha " + self.sha, result.stdout)
        self.assertNotIn("gradlew", self.log.read_text())

    def test_physical_run_does_not_suppress_emulator_errors(self):
        result = self.perf(PERF_COMPARE_RN_DRY_RUN="1", FAKE_QEMU="", PERF_CLEAR="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn("suppressErrors", result.stdout)
        self.assertIn("cf.perfClear=1", result.stdout)

    def test_physical_clear_requires_opt_in(self):
        result = self.perf(FAKE_QEMU="")
        self.assertEqual(result.returncode, 2)
        self.assertNotIn("install -r", self.log.read_text())

    def test_checker_still_runs_after_gradle_failure_and_trap_restores(self):
        result = self.perf(FAKE_GRADLE_EXIT="7")
        self.assertEqual(result.returncode, 7, result.stderr)
        calls = self.log.read_text().splitlines()
        self.assertLess(next(i for i, c in enumerate(calls) if c.startswith("gradlew")), next(i for i, c in enumerate(calls) if c.startswith("checker")))
        self.assertTrue(calls[-1].startswith("emulator.sh prep-functional"))

    def test_non_profileable_override_is_explicit(self):
        result = self.perf(PERF_COMPARE_RN_DRY_RUN="1", RN_NOT_PROFILEABLE="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("suppressErrors=EMULATOR,NOT-PROFILEABLE", result.stdout)

    def test_am_start_fallback_is_report_only_and_keeps_comparison_open(self):
        result = self.perf(RN_AM_START_FALLBACK="1", RN_NOT_PROFILEABLE="1")
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("rnAmStartTotalTimeMs median=111.0 ms", result.stdout)
        self.assertIn("never a below-RN TTID verdict", result.stdout)
        calls = self.log.read_text().splitlines()
        self.assertEqual(sum("am start -W -S -n net.chronoflow.app/.MainActivity" in c for c in calls), 10)
        self.assertIn("Comparison remains open", self.history.read_text())
