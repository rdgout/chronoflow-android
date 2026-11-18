import os
import signal
import sys
import time
import unittest

from helpers import SCRIPTS, LockHolder, run, temp_dir

RUN_TIER = str(SCRIPTS / "run_tier.py")

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

    def _timings(self, entries, name="1.json", tier_dir="tm"):
        import json

        (self.root / tier_dir).mkdir(parents=True, exist_ok=True)
        (self.root / tier_dir / name).write_text(json.dumps({"tier": "t", "tests": entries}))

    @staticmethod
    def _write_during_run(*paths):
        return [sys.executable, "-I", "-c", "from pathlib import Path; import sys; "
                "[Path(p).touch() for p in sys.argv[1:]]", *paths]

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
        self._timings([self._entry()], tier_dir="b/build/cf-timings/test")
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
        command[3] += "; sys.exit(1)"
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
        write = "from pathlib import Path; import os, sys\n" \
                "for name in sys.argv[1:]:\n" \
                " p=Path(name); p.touch(); ns=p.stat().st_mtime_ns//1000000000*1000000000; os.utime(p,ns=(ns,ns))"
        result = run([sys.executable, "-I", "-c", wrapper, RUN_TIER, "t", "--results", "out/*.xml",
                      "--timings", "tm/*.json", "--", sys.executable, "-I", "-c", write,
                      "out/TEST-a.xml", "tm/1.json"], self.env)
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
        self._timings([self._entry()])
        self._xml("new", cases=(("c.D", "other", "0.1"),))
        self._timings([{**self._entry(method="other"), "class": "c.D"}], name="2.json")
        result = self.tier("t", "--results", "out/*.xml", "--results", "new/*.xml",
                           "--timings", "tm/*.json", "--",
                           *self._write_during_run("new/TEST-a.xml", "tm/2.json"))
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

    def test_exempt_and_warmup_pass(self):
        self._xml(cases=(("a.B", "t1", "0.1"), ("a.B", "t2", "0.1")))
        self._timings(
            [
                self._entry(kind="robolectric", ms=12000, budget=20000, status="warmup"),
                self._entry(ms=6000, budget=None, status="exempt", method="t2"),
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

    def test_slowest_lists_top_ten(self):
        self._xml()
        self._timings([self._entry(ms=i, method="m%d" % i) for i in range(1, 16)] + [self._entry(ms=0)])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--",
                           *self._write_during_run("out/TEST-a.xml", "tm/1.json"))
        self.assertEqual(result.returncode, 0, result.stderr)
        slowest = self._report()["slowest"]
        self.assertEqual([c["ms"] for c in slowest], list(range(15, 5, -1)))


if __name__ == "__main__":
    unittest.main()
