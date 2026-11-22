import os
import unittest

from helpers import SCRIPTS, run, temp_dir

PERF = str(SCRIPTS / "perf-startup.sh")

ADB = r"""#!/bin/bash
echo "adb $*" >> "$FAKE_LOG"
case "$*" in
  get-serialno*) echo "${FAKE_SERIAL:-emulator-5554}" ;;
  devices*) printf 'List of devices attached\nemulator-5554\tdevice\n' ;;
  *getprop*qemu*) echo "${FAKE_QEMU:-1}" ;;
  *animator_duration_scale*) echo "${FAKE_SCALE:-1.0}" ;;
  *font_scale*) echo 1.0 ;;
esac
"""


class PerfStartupTest(unittest.TestCase):
    def setUp(self):
        self.tmp = temp_dir(self)
        self.log = self.tmp / "calls.log"
        self.log.write_text("")
        self.state = self.tmp / "state"
        self.state.mkdir()
        (self.state / "gpu-mode").write_text("host\n")
        self.stubs = {
            "adb": ADB,
            "gradlew": '#!/bin/bash\necho "gradlew $*" >> "$FAKE_LOG"\nexit "${FAKE_GRADLE_EXIT:-0}"\n',
            "emulator.sh": '#!/bin/bash\necho "emulator.sh $*" >> "$FAKE_LOG"\n',
            "checker": '#!/bin/bash\necho "checker $*" >> "$FAKE_LOG"\nexit "${FAKE_CHECK_EXIT:-0}"\n',
        }
        for name, body in self.stubs.items():
            path = self.tmp / name
            path.write_text(body)
            path.chmod(0o755)

    def perf(self, script=PERF, **extra):
        env = {
            "ADB": str(self.tmp / "adb"),
            "ANDROID_SERIAL": "",
            "GRADLEW": str(self.tmp / "gradlew"),
            "EMULATOR_SH": str(self.tmp / "emulator.sh"),
            "CHECK_BENCHMARKS": str(self.tmp / "checker"),
            "CF_EMULATOR_STATE_DIR": str(self.state),
            "FAKE_LOG": str(self.log),
            "PERF_OUTPUT_DIR": str(self.tmp / "output"),
            "PERF_ARCHIVE_ROOT": str(self.tmp / "archives"),
        }
        env.update(extra)
        return run([script], env, timeout=30)

    def calls(self):
        return self.log.read_text().splitlines()

    def test_suppress_errors_only_on_an_emulator(self):
        self.assertEqual(self.perf(FAKE_QEMU="1").returncode, 0)
        gradle = [c for c in self.calls() if c.startswith("gradlew")]
        self.assertIn("androidx.benchmark.suppressErrors=EMULATOR", gradle[0])
        self.log.write_text("")
        self.assertEqual(self.perf(FAKE_QEMU="0").returncode, 0)
        gradle = [c for c in self.calls() if c.startswith("gradlew")]
        self.assertNotIn("suppressErrors", gradle[0])
        self.assertIn("--device-class reference", [c for c in self.calls() if c.startswith("checker")][0])

    def test_preserves_evidence_from_the_previous_run(self):
        output = self.tmp / "output"
        output.mkdir()
        (output / "old.trace").write_text("previous evidence")
        result = self.perf()
        self.assertEqual(result.returncode, 0, result.stderr)
        archives = list((self.tmp / "archives").iterdir())
        self.assertEqual(len(archives), 1)
        self.assertTrue((archives[0] / "previous-output/old.trace").exists())

    def test_checker_runs_after_a_gradle_failure(self):
        result = self.perf(FAKE_GRADLE_EXIT="3")
        self.assertEqual(result.returncode, 3)
        self.assertTrue(any(c.startswith("checker") for c in self.calls()))

    def test_trap_restores_the_scales_after_a_failure(self):
        self.perf(FAKE_GRADLE_EXIT="3")
        calls = self.calls()
        self.assertIn("emulator.sh prep-perf emulator-5554", calls)
        self.assertEqual(calls[-1], "emulator.sh prep-functional emulator-5554")

    def test_a_non_host_gpu_on_an_emulator_refuses(self):
        (self.state / "gpu-mode").write_text("swiftshader_indirect\n")
        result = self.perf()
        self.assertEqual(result.returncode, 1)
        self.assertIn("-gpu host", result.stderr)
        self.assertFalse(any(c.startswith("gradlew") for c in self.calls()))

    def test_animator_scales_of_zero_stop_the_run(self):
        result = self.perf(FAKE_SCALE="0.0")
        self.assertEqual(result.returncode, 1)
        self.assertFalse(any(c.startswith("gradlew") for c in self.calls()))

    def test_dry_run_prints_the_commands_without_running_them(self):
        result = self.perf(PERF_STARTUP_DRY_RUN="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("connectedBenchmarkReleaseAndroidTest", result.stdout)
        self.assertIn("net.chronoflow.benchmark.ProcessDeathLaunchTest", result.stdout)
        self.assertFalse(any(c.startswith(("gradlew", "checker")) for c in self.calls()))

    def test_profile_collection_only_runs_the_generator(self):
        result = self.perf(script=str(SCRIPTS / "baseline-profile.sh"), BASELINE_PROFILE_DRY_RUN="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(
            "-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.benchmark.BaselineProfileGenerator",
            result.stdout,
        )
        self.assertIn("androidx.benchmark.enabledRules=BaselineProfile", result.stdout)
        self.assertNotIn("ProcessDeathLaunchTest", result.stdout)
        self.assertFalse(any(c.startswith("gradlew") for c in self.calls()))

    def test_clear_runs_once_after_perf_prep_and_before_gradle(self):
        self.assertEqual(self.perf().returncode, 0)
        calls = self.calls()
        clear = [i for i, c in enumerate(calls) if "shell pm clear net.chronoflow.app.native" in c]
        self.assertEqual(len(clear), 1)
        self.assertLess(calls.index("emulator.sh prep-perf emulator-5554"), clear[0])
        self.assertLess(clear[0], next(i for i, c in enumerate(calls) if c.startswith("gradlew")))

    def test_physical_clear_requires_opt_in(self):
        result = self.perf(FAKE_QEMU="0")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("state: as found", result.stdout)
        self.assertFalse(any("shell pm clear" in c for c in self.calls()))

    def test_profile_dry_run_prints_one_clear_after_perf_prep(self):
        result = self.perf(script=str(SCRIPTS / "baseline-profile.sh"), BASELINE_PROFILE_DRY_RUN="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.count("shell pm clear net.chronoflow.app.native"), 1)
        self.assertIn("emulator.sh prep-perf emulator-5554", self.calls())
        self.assertFalse(any("shell pm clear" in c for c in self.calls()))


if __name__ == "__main__":
    unittest.main()
