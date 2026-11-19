import importlib.util
import json
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

    def test_warm_start_has_its_own_reference_budget(self):
        root = temp_dir(self)
        fixture = next((FIXTURES / "startup/pass").glob("*.json"))
        data = json.loads(fixture.read_text())
        data["benchmarks"][1]["metrics"]["timeToInitialDisplayMs"] = {
            "median": 201.0, "minimum": 201.0, "maximum": 201.0, "runs": [201.0] * 10,
        }
        (root / fixture.name).write_text(json.dumps(data))
        result = check("--suite", "startup", "--device-class", "reference", "--results-dir", str(root))
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("startupWarm timeToInitialDisplayMs median=201.0 ms hard=200 FAIL", result.stdout)

    def test_missing_hand_off_metrics_fail_even_when_report_only(self):
        for metric in ["timeToFullDisplayMs", "readyFirstMs", "exitFirstMs"]:
            with self.subTest(metric=metric):
                root = temp_dir(self)
                fixture = next((FIXTURES / "startup/pass").glob("*.json"))
                data = json.loads(fixture.read_text())
                del data["benchmarks"][0]["metrics"][metric]
                (root / fixture.name).write_text(json.dumps(data))
                result = check("--suite", "startup", "--device-class", "emulator", "--results-dir", str(root))
                self.assertEqual(result.returncode, 1, result.stderr)
                self.assertIn(metric + " is missing", result.stderr)

    def test_missing_ttid_fails_naming_the_metric(self):
        result = check(
            "--suite", "startup", "--device-class", "emulator", "--results-dir", str(FIXTURES / "startup/missing-ttid")
        )
        self.assertEqual(result.returncode, 1)
        self.assertIn("timeToInitialDisplayMs", result.stderr)

    def test_nan_fails(self):
        result = check("--suite", "startup", "--device-class", "emulator", "--results-dir", str(FIXTURES / "startup/nan"))
        self.assertEqual(result.returncode, 1)
        self.assertIn("invalid runs", result.stderr)

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
        self.assertIn("timelineFirstFrameMs each=810.0 ms [hard=800] FAIL", result.stdout)

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
        self.assertEqual(added[0].strip("|").split("|")[10].strip().split("/")[0], "512.3")

    def test_launch_requires_every_iteration_to_animate_at_least_75_frames(self):
        result = check("--suite", "launch", "--device-class", "emulator", "--results-dir", str(FIXTURES / "launch/frame-count-74"))
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("frameCount each=74.0 count [min=75] FAIL", result.stdout)
        passed = check("--suite", "launch", "--device-class", "emulator", "--results-dir", str(FIXTURES / "launch/pass"))
        self.assertEqual(passed.returncode, 0, passed.stderr)



TIMELINE = FIXTURES / "timeline"
TIMELINE_HEADER = (
    "| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Page | Build | First frame ms med/max | "
    "CPU P50/P90/P99 ms | Main/Render P99 ms | Overrun P99 ms | Jank % mean/max | Hitch ms/s mean/max | "
    "Placeholder max | Replay max ms | RSS anon MiB max | RSS growth MiB | Recompose max | GC max | Scroll | Last day | Result |"
)


def timeline_check(results, *extra, device_class="emulator", history=None, row_dir=None):
    args = ["--suite", "timeline", "--device-class", device_class, "--results-dir", str(results), "--refresh-hz", "60"]
    if history:
        args += ["--history", str(history)]
    if row_dir:
        args += ["--row-file", str(row_dir / "row.md")]
    return check(*args, *extra)


def history_row(page="host", result="PASS", ff="400.0/410.0", cpu="3.0/5.0/9.0", placeholder="0", gc="0", theme="light", workload="bounded-swipe-v3"):
    cells = [
        "2026-11-01", "abc1234", "emulator", "sdk_gphone64_arm64", "36", "60", "host", "1.0", theme, page,
        "benchmarkRelease, Partial(Require)" + (", " + workload if workload else ""), ff, cpu, "3.0/3.5", "-1.2", "1.0/2.0", "0.6/1.0", placeholder, "0.5",
        "68.0", "1.0", "5", gc, "ok", "ok", result,
    ]
    return "| " + " | ".join(cells) + " |"


class TimelineCheckTest(unittest.TestCase):
    def variant(self, mutate, base="pass"):
        """A copy of a fixture with its JSON changed by `mutate(benchmarks_by_name)`."""
        root = temp_dir(self)
        data = json.loads((TIMELINE / base / "fixture-benchmarkData.json").read_text())
        mutate({b["name"]: b for b in data["benchmarks"]}, data)
        (root / "results").mkdir()
        (root / "results" / "fixture-benchmarkData.json").write_text(json.dumps(data))
        return root / "results"

    def history(self, rows):
        root = temp_dir(self)
        path = root / "perf-timeline.md"
        path.write_text("# T\n\n## Runs\n\n%s\n|%s\n%s\n" % (TIMELINE_HEADER, "---|" * 26, "\n".join(rows)))
        return path

    def test_timeline_pass_emulator(self):
        result = timeline_check(TIMELINE / "pass", row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 0, result.stderr)
        for name in (
            "timelineFirstFrameMs", "frameDurationCpuMs", "frameOverrunMs", "jankPercent", "hitchRatioMsPerS",
            "timelinePlaceholderCount", "timelineMainFrameMsP50", "timelineMainFrameMsP99", "timelineRenderThreadMsP99",
            "timelineTargetsMaxMs", "memoryRssAnonMaxKb", "memoryRssAnonGrowthKb", "timelineBuildOnMainCount",
            "frameCount", "timelineScrollWindowMs", "timelineBuildCount", "timelineRecomposeCount", "timelineGcCount",
            "timelineReplayMaxMs", "timelineReplayCount", "memoryHeapSizeMaxKb", "timeToInitialDisplayMs",
            "timelineDecodeMs", "timelinePrepareMs", "timelineTypefaceMs",
        ):
            self.assertIn("Timeline metric: ", [l for l in result.stdout.splitlines() if (" %s " % name) in l][0], name)

    def test_timeline_derived_values_are_computed_per_iteration(self):
        result = timeline_check(TIMELINE / "pass", row_dir=temp_dir(self))
        self.assertIn("jankPercent each=2.0 %", result.stdout)
        self.assertIn("hitchRatioMsPerS each=0.6 ms/s", result.stdout)
        self.assertIn("memoryRssAnonGrowthKb single=1000.0 KiB", result.stdout)

    def test_hitch_uses_active_frames_despite_idle_waits(self):
        module = load_checker()
        bench = {"metrics": {"timelineScrollWindowMs": {"runs": [73000]}},
                 "sampledMetrics": {"frameOverrunMs": {"runs": [[-1, 2, 0, 5]]}}}
        values, reason = module.derived_values({"metric": "hitchRatioMsPerS"}, bench, "60")
        self.assertEqual(reason, "")
        self.assertAlmostEqual(values[0], 105.0)

    def test_timeline_rss_over_fails(self):
        result = timeline_check(TIMELINE / "rss-153601", row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 1)
        self.assertIn("memoryRssAnonMaxKb each=153601.0 KiB [hard=153600] FAIL", result.stdout)

    def test_timeline_build_on_main_fails(self):
        for device_class in ("emulator", "reference", "lowEnd"):
            result = timeline_check(TIMELINE / "build-on-main", device_class=device_class, row_dir=temp_dir(self))
            self.assertEqual(result.returncode, 1, device_class)
            self.assertIn("timelineBuildOnMainCount each=1.0 count [hard=0] FAIL", result.stdout)

    def test_timeline_missing_report_only_fails(self):
        result = timeline_check(TIMELINE / "no-scroll-window", row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 1)
        self.assertIn("timelineScrollWindowMs", result.stderr)

    def test_timeline_zero_builds_fails(self):
        def empty(b, _):
            b["timelineScroll"]["metrics"]["timelineBuildCount"]["runs"][1] = 0
        result = timeline_check(self.variant(empty), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 1)
        self.assertIn("timelineBuildCount each=0.0 count [min=1] FAIL", result.stdout)

    def test_timeline_zero_replays_fails(self):
        def empty(b, _):
            b["timelineScroll"]["metrics"]["timelineReplayCount"]["runs"][1] = 0
        result = timeline_check(self.variant(empty), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 1)
        self.assertIn("timelineReplayCount each=0.0 count [min=1] FAIL", result.stdout)

    def test_timeline_empty_jank_iteration_fails(self):
        def empty(b, _):
            b["timelineScroll"]["sampledMetrics"]["frameOverrunMs"]["runs"][1] = []
        result = timeline_check(self.variant(empty), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 1)
        self.assertIn("frameOverrunMs has an empty iteration", result.stderr)

    def test_timeline_nan_fails(self):
        def nan(b, _):
            b["timelineScroll"]["metrics"]["timelineBuildCount"]["runs"][0] = float("nan")

        result = timeline_check(self.variant(nan), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 1)
        self.assertIn("invalid runs", result.stderr)

    def test_timeline_negative_count_fails(self):
        def negative(b, _):
            b["timelineScroll"]["metrics"]["timelinePlaceholderCount"]["runs"][0] = -1.0

        result = timeline_check(self.variant(negative), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 1)
        self.assertIn("invalid runs", result.stderr)

    def test_timeline_missing_last_day_fails(self):
        result = timeline_check(
            self.variant(lambda b, d: d["benchmarks"].remove(b["timelineLastDay"])), row_dir=temp_dir(self)
        )
        self.assertEqual(result.returncode, 1)
        self.assertIn("timelineLastDay", result.stderr)

    def test_timeline_negative_overrun_allowed(self):
        def overrun(b, _):
            b["timelineScroll"]["sampledMetrics"]["frameOverrunMs"]["P99"] = -2.1

        result = timeline_check(self.variant(overrun), device_class="reference", row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_timeline_device_gates(self):
        reference = timeline_check(TIMELINE / "jank-4", device_class="reference", row_dir=temp_dir(self))
        self.assertEqual(reference.returncode, 1)
        self.assertTrue(any("jankPercent" in l and "FAIL" in l for l in reference.stdout.splitlines()))
        for device_class in ("lowEnd", "emulator"):
            result = timeline_check(TIMELINE / "jank-4", device_class=device_class, row_dir=temp_dir(self))
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertNotIn("FAIL", result.stdout)

    def test_timeline_growth_warns(self):
        def grow(b, _):
            b["timelineScroll"]["metrics"]["memoryRssAnonMaxKb"]["runs"] = [70000.0, 70500.0, 111000.0]

        result = timeline_check(self.variant(grow), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("memoryRssAnonGrowthKb single=41000.0 KiB [warn=40960] WARN", result.stdout)

    def test_timeline_negative_growth_is_allowed(self):
        def shrink(b, _):
            b["timelineScroll"]["metrics"]["memoryRssAnonMaxKb"]["runs"] = [71000.0, 70500.0, 70000.0]

        result = timeline_check(self.variant(shrink), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("memoryRssAnonGrowthKb single=-1000.0 KiB", result.stdout)

    def test_timeline_trend_and_append(self):
        history = self.history([history_row(ff="%s/450.0" % m) for m in ("380.0", "400.0", "420.0")])
        before = history.read_text().splitlines()

        def slower(b, _):
            b["timelineFirstFrame"]["metrics"]["timelineFirstFrameFirstMs"]["runs"] = [520.0] * 10

        result = timeline_check(
            self.variant(slower), "--append", "--theme", "light", "--font-scale", "1.0", "--commit", "deadbee",
            history=history, row_dir=temp_dir(self),
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("Timeline trend: firstFrameMedianMs 400.0 -> 520.0 (+30%) FLAGGED", result.stdout)
        after = history.read_text().splitlines()
        added = [line for line in after if line not in before]
        self.assertEqual(len(added), 1)
        self.assertEqual(len(added[0].strip("|").split("|")), 26)
        self.assertEqual(after.index(added[0]), len(before))

    def test_timeline_append_creates_the_table(self):
        root = temp_dir(self)
        history = root / "perf-timeline.md"
        history.write_text("# T\n\n## Runs\n\nnotes\n")
        result = timeline_check(TIMELINE / "pass", "--append", history=history, row_dir=root)
        self.assertEqual(result.returncode, 0, result.stderr)
        lines = history.read_text().splitlines()
        self.assertIn(TIMELINE_HEADER, lines)
        self.assertEqual([len(l.strip("|").split("|")) for l in lines if l.startswith("| 20")], [26])

    def band_fixture(self, p99, rss=None):
        def mutate(b, _):
            b["timelineScroll"]["sampledMetrics"]["frameDurationCpuMs"]["P99"] = p99
            if rss is not None:
                b["timelineScroll"]["metrics"]["memoryRssAnonMaxKb"]["runs"][0] = rss

        return self.variant(mutate)

    def test_timeline_cpu_band_exits_4(self):
        rows = [history_row(cpu="3.0/5.0/%s" % p) for p in ("8.0", "9.0", "10.0")]
        history = self.history(rows)
        over = timeline_check(self.band_fixture(11.3), history=history, row_dir=temp_dir(self))
        self.assertEqual(over.returncode, 4, over.stdout + over.stderr)
        self.assertIn("Timeline band: frameDurationCpuMsP99 11.3 > 11.2 (baseline 9.0) BAND", over.stdout)
        under = timeline_check(self.band_fixture(11.2), history=history, row_dir=temp_dir(self))
        self.assertEqual(under.returncode, 0, under.stdout + under.stderr)
        both = timeline_check(self.band_fixture(11.3, rss=153601.0), history=history, row_dir=temp_dir(self))
        self.assertEqual(both.returncode, 1)

    def test_timeline_band_needs_history_of_the_same_page(self):
        rows = [history_row(page="trip", cpu="3.0/5.0/%s" % p) for p in ("8.0", "9.0", "10.0")]
        result = timeline_check(self.band_fixture(11.3), history=self.history(rows), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertNotIn("BAND", result.stdout)

    def test_timeline_band_ignores_the_old_workload(self):
        rows = [history_row(cpu="1.0/1.0/1.0", workload="bounded-swipe-v2")]
        result = timeline_check(self.band_fixture(11.3), history=self.history(rows), row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertNotIn("BAND", result.stdout)

    def test_timeline_placeholder_gate_emulator(self):
        def placeholders(count):
            def mutate(b, _):
                b["timelineScroll"]["metrics"]["timelinePlaceholderCount"]["runs"][0] = float(count)

            return self.variant(mutate)

        self.assertEqual(timeline_check(placeholders(7), row_dir=temp_dir(self)).returncode, 1)
        self.assertEqual(timeline_check(placeholders(6), row_dir=temp_dir(self)).returncode, 0)

    def test_timeline_zero_baseline_counter_flags(self):
        history = self.history([history_row(placeholder="0", gc="0") for _ in range(3)])

        def rises(b, _):
            b["timelineScroll"]["metrics"]["timelinePlaceholderCount"]["runs"] = [2.0, 0.0, 0.0]
            b["timelineScroll"]["metrics"]["timelineGcCount"]["runs"] = [1.0, 0.0, 0.0]

        result = timeline_check(self.variant(rises), history=history, row_dir=temp_dir(self))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("Timeline trend: placeholderMax 0.0 -> 2.0 (+2.0) FLAGGED", result.stdout)
        self.assertIn("Timeline trend: gcMax 0.0 -> 1.0 (+1.0) FLAGGED", result.stdout)


if __name__ == "__main__":
    unittest.main()
