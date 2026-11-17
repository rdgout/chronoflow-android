import importlib.util
import shutil
import sys
import unittest

from helpers import ROOT, SCRIPTS, run, temp_dir

CHECKER = str(SCRIPTS / "check-benchmarks.py")
FIXTURES = ROOT / "Scripts" / "fixtures" / "benchmarks"


def load_checker():
    spec = importlib.util.spec_from_file_location("check_benchmarks", CHECKER)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def check(*args):
    return run([sys.executable, "-I", CHECKER, *args])


class CheckBenchmarksTest(unittest.TestCase):
    def test_startup_pass_on_emulator_reports(self):
        result = check("--suite", "startup", "--device-class", "emulator", "--results-dir", str(FIXTURES / "startup/pass"))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Startup metric: startupCold timeToInitialDisplayMs median=512.3 ms REPORT", result.stdout)

    def test_missing_ttid_fails_naming_the_metric(self):
        result = check(
            "--suite", "startup", "--device-class", "emulator", "--results-dir", str(FIXTURES / "startup/missing-ttid")
        )
        self.assertEqual(result.returncode, 1)
        self.assertIn("timeToInitialDisplayMs", result.stderr)

    def test_nan_fails(self):
        result = check("--suite", "startup", "--device-class", "emulator", "--results-dir", str(FIXTURES / "startup/nan"))
        self.assertEqual(result.returncode, 1)

    def test_median_over_the_reference_hard_budget_fails(self):
        result = check(
            "--suite", "startup", "--device-class", "reference", "--results-dir", str(FIXTURES / "startup/median-450")
        )
        self.assertEqual(result.returncode, 1)
        self.assertIn("FAIL", result.stdout)

    def test_first_frame_810_fails_the_800_ms_gate(self):
        result = check(
            "--suite", "timeline", "--device-class", "emulator", "--results-dir", str(FIXTURES / "timeline/first-frame-810")
        )
        self.assertEqual(result.returncode, 1)
        self.assertIn("timelineFirstFrameMs each=810.0 ms hard=800 FAIL", result.stdout)

    def test_empty_directory_fails(self):
        empty = temp_dir(self)
        result = check("--suite", "startup", "--device-class", "emulator", "--results-dir", str(empty))
        self.assertEqual(result.returncode, 1)
        self.assertIn("no benchmark results", result.stderr)

    def test_unknown_class_only_reports(self):
        result = check(
            "--suite", "startup", "--device-class", "unknown", "--results-dir", str(FIXTURES / "startup/median-450")
        )
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_usage_error_exits_2(self):
        self.assertEqual(check("--suite", "bogus").returncode, 2)

    def test_derived_frame_math(self):
        module = load_checker()
        runs = [[-1, 2, 0, 5], [0, 0, 0, 0], [3, -4, 1, 0]]
        windows = [1000, 2000, 500]
        self.assertEqual([module.jank_percent(r) for r in runs], [50.0, 0.0, 50.0])
        self.assertEqual([module.hitch_ms_per_s(r, w) for r, w in zip(runs, windows)], [7.0, 0.0, 8.0])
        slow = [[-2, 1, 3, -1], [0, -5]]
        hitches = [module.hitch_ms_per_s(r, refresh_hz=60.0) for r in slow]
        self.assertAlmostEqual(hitches[0], 60.0)
        self.assertEqual(hitches[1], 0.0)

    def test_append_and_trend(self):
        root = temp_dir(self)
        history = root / "perf-startup.md"
        shutil.copyfile(FIXTURES / "history" / "perf-startup.md", history)
        before = history.read_text().splitlines()
        result = check(
            "--suite", "startup", "--device-class", "emulator", "--results-dir", str(FIXTURES / "startup/pass"),
            "--history", str(history), "--append", "--theme", "light", "--font-scale", "1.0", "--commit", "deadbee",
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("FLAGGED", result.stdout)
        after = history.read_text().splitlines()
        self.assertEqual(len(after), len(before) + 1)
        added = [line for line in after if line not in before]
        self.assertEqual(len(added), 1)
        self.assertEqual(len(added[0].strip("|").split("|")), 12)
        self.assertEqual(after.index(added[0]), 7)


if __name__ == "__main__":
    unittest.main()
