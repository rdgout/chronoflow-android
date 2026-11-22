import importlib.util
import io
import json
import os
import signal
import sys
import time
import unittest
from contextlib import redirect_stderr
from pathlib import Path
from unittest.mock import Mock, patch

from helpers import SCRIPTS, LockHolder, run, temp_dir

RUN_TIER = str(SCRIPTS / "run_tier.py")


def runner_module():
    spec = importlib.util.spec_from_file_location("run_tier", RUN_TIER)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

JUNIT = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="s" tests="2">
  <testcase classname="a.B" name="passes" time="0.5"/>
  <testcase classname="a.B" name="fails" time="1.25"><failure message="x"/></testcase>
</testsuite>
"""


class RunTierTest(unittest.TestCase):
    def setUp(self):
        self.root = temp_dir(self)
        self.env = {
            "RUN_TIER_ROOT": str(self.root),
            "TIER_STOP_CMD": "true",
            "TIER_KILL_GRACE": "0.2",
            "CF_DEVICE_LOCK": str(self.root / "lock"),
            "CF_DEVICE_LOCK_HELD": "",
        }

    def tier(self, *args, env=None, timeout=30):
        merged = dict(self.env, **(env or {}))
        return run([sys.executable, "-I", RUN_TIER, *args], merged, timeout=timeout)

    def log(self, tier):
        return (self.root / "build" / "test-results" / tier / "run.log").read_text()

    def test_idle_cap_stops_a_silent_command(self):
        result = self.tier(
            "t", "--results", "none", "--", "sh", "-c", 'trap "" TERM; sleep 30', env={"TIER_IDLE_TIMEOUT": "0.3"}
        )
        self.assertEqual(result.returncode, 124)
        self.assertLess(result.elapsed, 2)
        self.assertIn("idle cap", self.log("t"))

    def test_total_cap_stops_a_chatty_command(self):
        result = self.tier(
            "t",
            "--results",
            "none",
            "--",
            "sh",
            "-c",
            "while true; do echo tick; sleep 0.2; done",
            env={"TIER_TOTAL_TIMEOUT": "0.4", "TIER_IDLE_TIMEOUT": "10"},
        )
        self.assertEqual(result.returncode, 124)
        self.assertLess(result.elapsed, 2)
        self.assertIn("total cap", self.log("t"))

    def test_kills_the_whole_process_group(self):
        pid_file = self.root / "child.pid"
        script = "sleep 300 & echo $! > %s; wait" % pid_file
        result = self.tier("t", "--results", "none", "--", "sh", "-c", script, env={"TIER_IDLE_TIMEOUT": "0.3"})
        self.assertEqual(result.returncode, 124)
        pid = int(pid_file.read_text())
        with self.assertRaises(ProcessLookupError):
            for _ in range(20):
                os.kill(pid, 0)
                time.sleep(0.1)
            os.kill(pid, signal.SIGKILL)
            raise AssertionError("background child survived the cap")

    def test_missing_results_fail(self):
        result = self.tier("t", "--results", "nothing/*.xml", "--", "true")
        self.assertEqual(result.returncode, 2)
        self.assertIn("produced no result files", result.stderr)

    def test_empty_emulator_results_fail(self):
        result = self.tier(
            "t", "--emulator", "--results", "empty.xml", "--", sys.executable, "-c",
            "from pathlib import Path; Path('empty.xml').write_text('<testsuites tests=\"0\"/>')",
        )
        self.assertEqual(result.returncode, 2, result.stderr)
        self.assertIn("emulator tier selected no test cases", result.stderr)
        report = json.loads((self.root / "build/test-results/t/timings.json").read_text())
        self.assertEqual(0, report["measuredTests"])
        self.assertIn("emulator tier selected no test cases", report["missingEvidence"])

    def test_worker_hang_evidence_is_surfaced_and_preserved(self):
        directory = self.root / "module/build/cf-timings/test"
        directory.mkdir(parents=True)
        evidence = "cf test hang: [test:blocked] exceeded 100ms\n    at Example.blocked(Example.kt:4)\n"
        result = self.tier(
            "t", "--results", "none", "--timings", "module/build/cf-timings/test/*.json", "--",
            sys.executable, "-c",
            "from pathlib import Path; Path('module/build/cf-timings/test/hang-blocked.txt').write_text(%r); raise SystemExit(86)" % evidence,
        )
        self.assertEqual(result.returncode, 86, result.stderr)
        self.assertIn("test hang evidence: module/build/cf-timings/test/hang-blocked.txt", result.stderr)
        report_dir = self.root / "build/test-results/t"
        self.assertEqual(next((report_dir / "hangs").glob("*.txt")).read_text(), evidence)
        self.assertEqual(json.loads((report_dir / "timings.json").read_text())["hangs"],
                         ["module/build/cf-timings/test/hang-blocked.txt"])

    def test_collects_results_and_timings(self):
        (self.root / "out").mkdir()
        script = "cat > out/TEST-a.xml <<'EOF'\n%s\nEOF\n" % JUNIT
        result = self.tier("t", "--results", "out/*.xml", "--", "sh", "-c", script)
        self.assertEqual(result.returncode, 0, result.stderr)
        base = self.root / "build" / "test-results" / "t"
        self.assertTrue(any((base / "junit").iterdir()))
        import json

        data = json.loads((base / "timings.json").read_text())
        statuses = {t["name"]: (t["status"], t["seconds"]) for t in data["tests"]}
        self.assertEqual(statuses, {"passes": ("passed", 0.5), "fails": ("failed", 1.25)})

    def test_command_failure_propagates(self):
        (self.root / "out").mkdir()
        (self.root / "out" / "TEST-a.xml").write_text(JUNIT)
        result = self.tier("t", "--results", "out/*.xml", "--", "sh", "-c", "exit 3")
        self.assertEqual(result.returncode, 3)
        self.assertTrue((self.root / "build" / "test-results" / "t" / "timings.json").exists())

    def test_device_lock_timeout(self):
        holder = LockHolder(self.root / "lock")
        self.addCleanup(holder.close)
        marker = self.root / "ran"
        result = self.tier(
            "t",
            "--device",
            "--results",
            "none",
            "--",
            "sh",
            "-c",
            "touch %s" % marker,
            env={"TIER_LOCK_TIMEOUT": "0.3"},
        )
        self.assertEqual(result.returncode, 75)
        self.assertLess(result.elapsed, 2)
        self.assertFalse(marker.exists())

    def _xml(self, tier_dir="out", cases=(("a.B", "t1", "0.1"),)):
        (self.root / tier_dir).mkdir(parents=True, exist_ok=True)
        body = "".join('<testcase classname="%s" name="%s" time="%s"/>' % c for c in cases)
        (self.root / tier_dir / "TEST-a.xml").write_text("<testsuite>%s</testsuite>" % body)

    def _timings(self, entries, name="1.json", tier_dir="tm", passed=False):
        import json

        (self.root / tier_dir).mkdir(parents=True, exist_ok=True)
        (self.root / tier_dir / name).write_text(json.dumps({"tier": "t", "tests": entries}))
        if passed:
            module = runner_module()
            directory = self.root / tier_dir
            module.gate_marker(directory, module.PASSED_GATE_SUFFIX).write_text(json.dumps({
                "fingerprint": module.timing_fingerprint(directory),
            }))

    def _write_during_run(self, *paths, coarse=False):
        contents = {p: (self.root / p).read_text() if (self.root / p).exists() else "" for p in paths}
        script = "from pathlib import Path; import json, os, sys\n" \
                 "for name, contents in json.loads(sys.argv[1]).items():\n" \
                 " p=Path(name); p.parent.mkdir(parents=True, exist_ok=True); p.write_text(contents)\n"
        if coarse:
            script += " ns=p.stat().st_mtime_ns//1000000000*1000000000; os.utime(p,ns=(ns,ns))\n"
        return [sys.executable, "-I", "-c", script, json.dumps(contents)]

    @staticmethod
    def _entry(kind="jvm", ms=10, budget=1000, status="ok", method="t1"):
        return {"class": "a.B", "method": method, "kind": kind, "sdk": "default", "ms": ms, "budgetMs": budget, "status": status}

    def _report(self):
        import json

        return json.loads((self.root / "build" / "test-results" / "t" / "timings.json").read_text())

    def test_listener_over_budget_fails(self):
        self._xml()
        self._timings([self._entry(ms=1200, status="over")])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 3)
        self.assertIn("over budget: jvm a.B#t1 1200 ms > 1000 ms", result.stderr)

    def test_successful_stale_timings_are_not_judged_again(self):
        self._xml()
        self._timings([self._entry()])
        first = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                          *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(first.returncode, 0, first.stderr)
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--", "true")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self._report()["budgetChecks"], [])
        self.assertEqual(self._report()["overBudget"], 0)

    def _task_outputs(self, entries, cases=(("a.B", "t1", "0.1"),)):
        self._xml("a/build/test-results/test", cases)
        self._timings(entries, tier_dir="a/build/cf-timings/test")
        return self._write_during_run(
            "a/build/cf-timings/test/execution.started", "a/build/test-results/test/TEST-a.xml",
            "a/build/cf-timings/test/1.json",
        )

    def _task_tier(self, command):
        return self.tier("t", "--results", "*/build/test-results/test/*.xml",
                         "--timings", "*/build/cf-timings/test/*.json", "--", *command)

    def _assert_task_outputs_invalidated(self):
        self.assertFalse((self.root / "a/build/cf-timings/test").exists())
        self.assertFalse((self.root / "a/build/test-results/test").exists())

    def test_failed_budget_requires_fresh_task_outputs_before_a_retry_can_pass(self):
        self._xml("b/build/test-results/test")
        self._timings([self._entry()], tier_dir="b/build/cf-timings/test", passed=True)
        first = self._task_tier(self._task_outputs([self._entry(ms=1200, status="over")]))
        self.assertEqual(first.returncode, 3, first.stderr)
        self._assert_task_outputs_invalidated()
        self.assertTrue((self.root / "b/build/cf-timings/test/1.json").is_file())
        self.assertTrue((self.root / "b/build/test-results/test/TEST-a.xml").is_file())

        retry_without_execution = self._task_tier(["true"])
        self.assertNotEqual(retry_without_execution.returncode, 0)

        quiet_retry = self._task_tier(self._task_outputs([self._entry()]))
        self.assertEqual(quiet_retry.returncode, 0, quiet_retry.stderr)
        cached_retry = self._task_tier(["true"])
        self.assertEqual(cached_retry.returncode, 0, cached_retry.stderr)
        self.assertEqual(self._report()["budgetChecks"], [])

    def test_a_fresh_overrun_still_fails_on_the_next_execution(self):
        first = self._task_tier(self._task_outputs([self._entry(ms=1200, status="over")]))
        self.assertEqual(first.returncode, 3, first.stderr)
        self._assert_task_outputs_invalidated()
        second = self._task_tier(self._task_outputs([self._entry(ms=1200, status="over")]))
        self.assertEqual(second.returncode, 3, second.stderr)
        self._assert_task_outputs_invalidated()

    def test_another_command_failure_does_not_preserve_failed_timing_outputs(self):
        command = self._task_outputs([self._entry(ms=1200, status="over")])
        command[3] += "\nsys.exit(1)\n"
        first = self._task_tier(command)
        self.assertEqual(first.returncode, 1, first.stderr)
        self._assert_task_outputs_invalidated()
        self.assertNotEqual(self._task_tier(["true"]).returncode, 0)

    def test_missing_evidence_invalidates_outputs_and_cannot_pass_without_execution(self):
        self._xml("b/build/test-results/test")
        self._task_outputs([self._entry()])
        first = self._task_tier(self._write_during_run(
            "a/build/cf-timings/test/execution.started", "a/build/test-results/test/TEST-a.xml",
        ))
        self.assertEqual(first.returncode, 2, first.stderr)
        self._assert_task_outputs_invalidated()
        self.assertNotEqual(self._task_tier(["true"]).returncode, 0)

    def test_unbudgeted_cases_invalidate_outputs_and_cannot_pass_without_execution(self):
        self._xml("b/build/test-results/test")
        first = self._task_tier(self._task_outputs(
            [self._entry()], cases=(("a.B", "t1", "0.1"), ("a.B", "missing", "0.1")),
        ))
        self.assertEqual(first.returncode, 2, first.stderr)
        self._assert_task_outputs_invalidated()
        self.assertNotEqual(self._task_tier(["true"]).returncode, 0)

    def test_coarse_filesystem_timestamps_still_judge_current_evidence(self):
        self._xml()
        self._timings([self._entry(ms=1200, status="over")])
        wrapper = """
import importlib.util, os, sys
from pathlib import Path
spec = importlib.util.spec_from_file_location('run_tier', sys.argv[1])
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
original_touch = Path.touch
def coarse_touch(path, *args, **kwargs):
    original_touch(path, *args, **kwargs)
    ns = path.stat().st_mtime_ns // 1_000_000_000 * 1_000_000_000
    os.utime(path, ns=(ns, ns))
Path.touch = coarse_touch
sys.exit(module.main(sys.argv[2:]))
"""
        result = run([sys.executable, "-I", "-c", wrapper, RUN_TIER, "t", "--results", "out/*.xml",
                      "--timings", "tm/*.json", "--",
                      *self._write_during_run("out/TEST-a.xml", "tm/1.json", coarse=True)], self.env)
        self.assertEqual(result.returncode, 3, result.stderr)
        self.assertEqual(self._report()["overBudget"], 1)
        reference = self.root / "build/test-results/t/invocation.started"
        self.assertEqual(reference.stat().st_mtime_ns % 1_000_000_000, 0)

    def test_stale_timing_cannot_cover_tests_that_ran_now(self):
        self._xml()
        self._timings([self._entry()])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml"))
        self.assertEqual(result.returncode, 2)
        self.assertIn("wrote no timing files", result.stderr)

    def test_only_current_tests_need_current_timing_entries(self):
        self._xml()
        self._timings([self._entry()], tier_dir="tm/old", passed=True)
        self._xml("new", cases=(("c.D", "other", "0.1"),))
        self._timings([{**self._entry(method="other"), "class": "c.D"}], tier_dir="tm/new")
        result = self.tier("t", "--results", "out/*.xml", "--results", "new/*.xml",
                           "--timings", "tm/*/*.json", "--",
                           *self._write_during_run("new/TEST-a.xml", "tm/new/1.json"))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self._report()["measuredTests"], 1)
        self.assertEqual(len(self._report()["tests"]), 2)
        self.assertEqual([e["class"] for e in self._report()["budgetChecks"]], ["c.D"])

    def test_executed_task_cannot_borrow_another_tasks_timing_evidence(self):
        for module in ("a", "b"):
            self._xml(module + "/build/test-results/test")
            self._timings([self._entry()], tier_dir=module + "/build/cf-timings/test")
        result = self.tier(
            "t", "--results", "*/build/test-results/test/*.xml", "--timings", "*/build/cf-timings/test/*.json", "--",
            *self._write_during_run(
                "a/build/cf-timings/test/execution.started", "a/build/test-results/test/TEST-a.xml",
                "b/build/cf-timings/test/execution.started", "b/build/test-results/test/TEST-a.xml",
                "b/build/cf-timings/test/1.json",
            ),
        )
        self.assertEqual(result.returncode, 2)
        self.assertIn("a/build/cf-timings/test wrote no timing files", result.stderr)

    def test_executed_task_cannot_reuse_old_result_files(self):
        self._xml("a/build/test-results/test")
        self._timings([self._entry()], tier_dir="a/build/cf-timings/test")
        result = self.tier(
            "t", "--results", "a/build/test-results/test/*.xml", "--timings", "a/build/cf-timings/test/*.json", "--",
            *self._write_during_run("a/build/cf-timings/test/execution.started", "a/build/cf-timings/test/1.json"),
        )
        self.assertEqual(result.returncode, 2)
        self.assertIn("a/build/test-results/test wrote no result files", result.stderr)

    def test_executed_task_with_no_selected_cases_has_explicit_empty_timings(self):
        self._xml()
        self._timings([], tier_dir="a/build/cf-timings/test")
        result = self.tier(
            "t", "--results", "out/*.xml", "--timings", "a/build/cf-timings/test/*.json", "--",
            *self._write_during_run("a/build/cf-timings/test/execution.started", "a/build/cf-timings/test/1.json"),
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self._report()["measuredTests"], 0)

    def test_class_warmups_are_reported_separately_from_cases(self):
        self._xml()
        self._timings([self._entry()])
        path = self.root / "tm/1.json"
        data = json.loads(path.read_text())
        warmup = {"class": "a.B", "sdk": "36", "name": "fonts", "ms": 750}
        data["warmups"] = [warmup]
        path.write_text(json.dumps(data))
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self._report()["warmups"], [warmup])
        self.assertEqual(self._report()["measuredTests"], 1)
        self.assertEqual(len(self._report()["budgetChecks"]), 1)

    def test_class_warmup_cannot_replace_test_timing_evidence(self):
        self._xml()
        self._timings([])
        path = self.root / "tm/1.json"
        data = json.loads(path.read_text())
        data["warmups"] = [{"class": "a.B", "sdk": "36", "name": "fonts", "ms": 750}]
        path.write_text(json.dumps(data))
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 2, result.stderr)
        self.assertIn("without a timing entry", result.stderr)

    def test_exempt_and_warmup_pass(self):
        self._xml(cases=(("a.B", "t1", "0.1"), ("a.B", "t2", "0.1"), ("a.B", "t3", "2.5")))
        self._timings(
            [
                self._entry(kind="robolectric", ms=12000, budget=20000, status="warmup"),
                self._entry(ms=6000, budget=None, status="exempt", method="t2"),
                self._entry(kind="robolectric", ms=2500, budget=3000, status="class-warmup", method="t3"),
            ]
        )
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self._report()["overBudget"], 0)

    def test_emulator_xml_over_budget_fails(self):
        self._xml(cases=(("a.B", "t1", "10.5"),))
        result = self.tier("t", "--results", "out/*.xml", "--emulator", "--",
                           *self._write_during_run("out/TEST-a.xml"))
        self.assertEqual(result.returncode, 3)
        self.assertIn("over budget: emulator a.B#t1 10500 ms > 10000 ms", result.stderr)

    def test_missing_timing_files_fail(self):
        self._xml()
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml"))
        self.assertEqual(result.returncode, 2)
        self.assertIn("wrote no timing files", result.stderr)

    def test_xml_without_listener_entry_is_not_budgeted(self):
        self._xml(cases=(("a.B", "t1", "0.1"), ("c.D", "other", "0.1")))
        self._timings([self._entry()])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 2)
        self.assertIn("without a timing entry: c.D#other", result.stderr)
        report = self._report()
        self.assertEqual(len(report["budgetChecks"]), 1)
        self.assertEqual(report["unbudgeted"], ["c.D#other"])

    def test_ignored_xml_case_needs_no_listener_entry(self):
        self._xml()
        xml = self.root / "out/TEST-a.xml"
        xml.write_text(xml.read_text().replace(
            "</testsuite>", '<testcase classname="c.D" name="unsupported" time="0"><skipped/></testcase></testsuite>'
        ))
        self._timings([self._entry()])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 0, result.stderr)
        report = self._report()
        self.assertEqual(len(report["budgetChecks"]), 1)
        self.assertEqual(report["tests"][1]["status"], "skipped")
        self.assertNotIn("unbudgeted", report)

    def test_slowest_lists_top_ten(self):
        self._xml()
        self._timings([self._entry(ms=i, method="m%d" % i) for i in range(1, 16)] + [self._entry(ms=0)])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 0, result.stderr)
        slowest = self._report()["slowest"]
        self.assertEqual([c["ms"] for c in slowest], list(range(15, 5, -1)))


class PassedStampTest(unittest.TestCase):
    def setUp(self):
        self.root = temp_dir(self).resolve()
        self.runner = runner_module()
        self.started = time.monotonic()

    def tearDown(self):
        self.assertLess(time.monotonic() - self.started, 1)

    def _outputs(self, module="a", entries=None):
        if entries is None:
            entries = [RunTierTest._entry()]
        timings = self.root / module / "build/cf-timings/test"
        timings.mkdir(parents=True, exist_ok=True)
        (timings / "execution.started").write_text("")
        (timings / "1.json").write_text(json.dumps({"tests": entries}))
        if entries:
            results = self.root / module / "build/test-results/test"
            results.mkdir(parents=True, exist_ok=True)
            (results / "TEST-a.xml").write_text('<testsuite><testcase classname="a.B" name="t1" time="0.1"/></testsuite>')
        return timings

    def _invoke(self, produce=None, flags=(), device=False):
        lock = Mock()
        lock.acquire.return_value = 17
        lock.lock_path.return_value = self.root / "lock"
        errors = io.StringIO()

        def execute(*_):
            if produce is not None:
                produce()
            return 0

        args = ["t", *(["--device"] if device else []),
                "--results", "*/build/test-results/test/*.xml", "--timings", "*/build/cf-timings/test/*.json",
                "--", "fake-gradle", *flags]
        with patch.dict(os.environ, {"RUN_TIER_ROOT": str(self.root), "CF_DEVICE_LOCK_HELD": ""}), \
                patch.object(self.runner, "run_command", side_effect=execute), \
                patch.object(self.runner, "load_device_lock", return_value=lock), redirect_stderr(errors):
            code = self.runner.main(args)
        report = json.loads((self.root / "build/test-results/t/timings.json").read_text())
        self.assertEqual(report["exitCode"], code)
        if device:
            lock.release.assert_called_once_with(17)
        return code, report, errors.getvalue()

    def test_invalidation_error_still_marks_other_tasks_reports_and_unlocks(self):
        a = self.root / "a/build/cf-timings/test"
        b = self.root / "b/build/cf-timings/test"
        original = self.runner.shutil.rmtree

        def remove(path, *args, **kwargs):
            if Path(path) == a:
                self.assertTrue(self.runner.gate_marker(a).is_file())
                self.assertTrue(self.runner.gate_marker(b).is_file())
                raise PermissionError("simulated undeletable timing directory")
            return original(path, *args, **kwargs)

        def produce():
            self._outputs("a", [RunTierTest._entry(ms=1200, status="over")])
            self._outputs("b", [RunTierTest._entry(ms=1200, status="over")])

        with patch.object(self.runner.shutil, "rmtree", side_effect=remove):
            code, report, errors = self._invoke(produce, device=True)
        self.assertEqual(code, 2)
        self.assertIn("simulated undeletable", errors)
        self.assertTrue(report["errors"])
        self.assertTrue(self.runner.gate_marker(b).is_file())
        self.assertFalse(b.exists())

    def test_path_validation_error_does_not_prevent_marking_other_tasks(self):
        a = self.root / "a/build/cf-timings/test"
        b = self.root / "b/build/cf-timings/test"
        original = Path.resolve

        def resolve(path, *args, **kwargs):
            if path == a:
                raise ValueError("simulated path validation error")
            return original(path, *args, **kwargs)

        def produce():
            self._outputs("a", [RunTierTest._entry(ms=1200, status="over")])
            self._outputs("b", [RunTierTest._entry(ms=1200, status="over")])

        with patch.object(Path, "resolve", resolve):
            code, _, errors = self._invoke(produce, device=True)
        self.assertEqual(code, 2)
        self.assertIn("simulated path validation error", errors)
        self.assertTrue(self.runner.gate_marker(b).is_file())

    def test_up_to_date_outputs_from_outside_the_runner_need_a_passed_stamp(self):
        directory = self._outputs()
        code, _, errors = self._invoke()
        self.assertEqual(code, 2)
        self.assertIn("rerun", errors)
        self.assertFalse(directory.exists())

    def test_matching_passed_stamp_allows_reuse(self):
        first, _, _ = self._invoke(self._outputs)
        self.assertEqual(first, 0)
        directory = self.root / "a/build/cf-timings/test"
        self.assertTrue(self.runner.gate_marker(directory, self.runner.PASSED_GATE_SUFFIX).is_file())
        second, report, _ = self._invoke()
        self.assertEqual(second, 0)
        self.assertEqual(report["budgetChecks"], [])

    def test_changed_fingerprint_rejects_a_previously_passed_stamp(self):
        self.assertEqual(self._invoke(self._outputs)[0], 0)
        directory = self.root / "a/build/cf-timings/test"
        with (directory / "1.json").open("a") as output:
            output.write("\n")
        code, _, errors = self._invoke()
        self.assertEqual(code, 2)
        self.assertIn("matching passed stamp", errors)
        self.assertFalse(directory.exists())

    def test_filtered_run_cannot_create_a_passed_stamp(self):
        code, _, _ = self._invoke(self._outputs, flags=("--tests", "a.B"))
        self.assertEqual(code, 0)
        directory = self.root / "a/build/cf-timings/test"
        self.assertFalse(self.runner.gate_marker(directory, self.runner.PASSED_GATE_SUFFIX).exists())

    def test_filtered_run_revokes_an_old_stamp_even_when_contents_match(self):
        self.assertEqual(self._invoke(self._outputs)[0], 0)
        self.assertEqual(self._invoke(self._outputs, flags=("--tests=a.B",))[0], 0)
        directory = self.root / "a/build/cf-timings/test"
        self.assertFalse(self.runner.gate_marker(directory, self.runner.PASSED_GATE_SUFFIX).exists())

    def test_zero_test_run_cannot_create_a_passed_stamp(self):
        def produce():
            self._outputs("a", [])
            self._outputs("b")

        code, _, _ = self._invoke(produce)
        self.assertEqual(code, 0)
        directory = self.root / "a/build/cf-timings/test"
        self.assertFalse(self.runner.gate_marker(directory, self.runner.PASSED_GATE_SUFFIX).exists())

    def test_unexpected_validation_error_still_writes_report_and_unlocks(self):
        def fail():
            raise ValueError("simulated validator error")

        code, report, errors = self._invoke(fail, device=True)
        self.assertEqual(code, 2)
        self.assertIn("simulated validator error", errors)
        self.assertTrue(report["errors"])


if __name__ == "__main__":
    unittest.main()
