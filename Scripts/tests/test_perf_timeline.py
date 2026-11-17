import unittest

from helpers import SCRIPTS, run, temp_dir

PERF = str(SCRIPTS / "perf-timeline.sh")

ADB = r"""#!/bin/bash
echo "adb $*" >> "$FAKE_LOG"
case "$*" in
  devices*) printf 'List of devices attached\nemulator-5554\tdevice\n' ;;
  *getprop*qemu*) echo "${FAKE_QEMU:-1}" ;;
  *persist.sys.locale*) echo "${FAKE_LOCALE:-en-US}" ;;
  *animator_duration_scale*) echo "${FAKE_SCALE:-1.0}" ;;
esac
"""

# Exits with the next code of FAKE_CHECK_EXITS (space separated), then with the last one.
CHECKER = r"""#!/bin/bash
echo "checker $*" >> "$FAKE_LOG"
n=$(grep -c '^checker' "$FAKE_LOG")
codes=(${FAKE_CHECK_EXITS:-0})
i=$((n - 1))
if [ "$i" -ge "${#codes[@]}" ]; then i=$((${#codes[@]} - 1)); fi
exit "${codes[$i]}"
"""

EMULATOR = r"""#!/bin/bash
echo "emulator.sh $* THEME=${THEME:-} FONT_SCALE=${FONT_SCALE:-}" >> "$FAKE_LOG"
"""


class PerfTimelineTest(unittest.TestCase):
    def setUp(self):
        self.tmp = temp_dir(self)
        self.log = self.tmp / "calls.log"
        self.log.write_text("")
        self.state = self.tmp / "state"
        self.state.mkdir()
        (self.state / "gpu-mode").write_text("host\n")
        stubs = {
            "adb": ADB,
            "gradlew": '#!/bin/bash\necho "gradlew $*" >> "$FAKE_LOG"\nexit "${FAKE_GRADLE_EXIT:-0}"\n',
            "emulator.sh": EMULATOR,
            "checker": CHECKER,
        }
        for name, body in stubs.items():
            path = self.tmp / name
            path.write_text(body)
            path.chmod(0o755)

    def perf(self, **extra):
        env = {
            "ADB": str(self.tmp / "adb"),
            "GRADLEW": str(self.tmp / "gradlew"),
            "EMULATOR_SH": str(self.tmp / "emulator.sh"),
            "CHECK_BENCHMARKS": str(self.tmp / "checker"),
            "CF_EMULATOR_STATE_DIR": str(self.state),
            "FAKE_LOG": str(self.log),
        }
        env.update(extra)
        return run([PERF], env, timeout=30)

    def calls(self, prefix):
        return [c for c in self.log.read_text().splitlines() if c.startswith(prefix)]

    def test_suppress_errors_only_on_an_emulator(self):
        self.assertEqual(self.perf(FAKE_QEMU="1").returncode, 0)
        self.assertIn("androidx.benchmark.suppressErrors=EMULATOR", self.calls("gradlew")[0])
        self.assertIn("--device-class emulator", self.calls("checker")[0])
        self.log.write_text("")
        self.assertEqual(self.perf(FAKE_QEMU="0").returncode, 0)
        self.assertNotIn("suppressErrors", self.calls("gradlew")[0])
        self.assertIn("--device-class reference", self.calls("checker")[0])

    def test_a_non_host_gpu_on_an_emulator_refuses(self):
        (self.state / "gpu-mode").write_text("swiftshader_indirect\n")
        result = self.perf()
        self.assertEqual(result.returncode, 1)
        self.assertIn("-gpu host", result.stderr)
        self.assertEqual(self.calls("gradlew"), [])

    def test_a_dutch_locale_refuses(self):
        result = self.perf(FAKE_LOCALE="nl-NL")
        self.assertEqual(result.returncode, 1)
        self.assertIn("locale", result.stderr)
        self.assertEqual(self.calls("gradlew"), [])

    def test_theme_and_font_scale_reach_prep_and_the_checker(self):
        self.assertEqual(self.perf(THEME="dark", FONT_SCALE="1.3").returncode, 0)
        self.assertIn("emulator.sh prep-perf THEME=dark FONT_SCALE=1.3", self.calls("emulator.sh"))
        checker = self.calls("checker")[0]
        self.assertIn("--theme dark", checker)
        self.assertIn("--font-scale 1.3", checker)

    def test_extra_gradle_args_and_no_append(self):
        self.assertEqual(self.perf(PERF_GRADLE_ARGS="-Pa=b", PERF_NO_APPEND="1").returncode, 0)
        self.assertIn("-Pa=b", self.calls("gradlew")[0])
        self.assertNotIn("--append", self.calls("checker")[0])
        self.log.write_text("")
        self.perf()
        self.assertIn("--append", self.calls("checker")[0])

    def test_the_checker_still_runs_after_a_gradle_failure(self):
        result = self.perf(FAKE_GRADLE_EXIT="3")
        self.assertEqual(result.returncode, 3)
        self.assertEqual(len(self.calls("checker")), 1)
        self.assertTrue(self.calls("emulator.sh")[-1].startswith("emulator.sh prep-functional"))

    def test_a_band_miss_reruns_once(self):
        result = self.perf(FAKE_CHECK_EXITS="4 0")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(self.calls("gradlew")), 2)
        self.assertIn("band miss; re-running once", result.stdout)

    def test_a_second_band_miss_keeps_exit_4(self):
        result = self.perf(FAKE_CHECK_EXITS="4 4")
        self.assertEqual(result.returncode, 4)
        self.assertEqual(len(self.calls("gradlew")), 2)

    def test_animator_scales_of_zero_stop_the_run(self):
        result = self.perf(FAKE_SCALE="0.0")
        self.assertEqual(result.returncode, 1)
        self.assertEqual(self.calls("gradlew"), [])

    def test_dry_run_prints_the_commands_without_running_them(self):
        result = self.perf(PERF_TIMELINE_DRY_RUN="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("connectedBenchmarkReleaseAndroidTest", result.stdout)
        self.assertEqual(self.calls("gradlew") + self.calls("checker"), [])


if __name__ == "__main__":
    unittest.main()
