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
            "TIER_KILL_GRACE": "1",
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
            "t", "--results", "none", "--", "sh", "-c", 'trap "" TERM; sleep 30', env={"TIER_IDLE_TIMEOUT": "1"}
        )
        self.assertEqual(result.returncode, 124)
        self.assertLess(result.elapsed, 4)
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
            env={"TIER_TOTAL_TIMEOUT": "2", "TIER_IDLE_TIMEOUT": "10"},
        )
        self.assertEqual(result.returncode, 124)
        self.assertLess(result.elapsed, 5)
        self.assertIn("total cap", self.log("t"))

    def test_kills_the_whole_process_group(self):
        pid_file = self.root / "child.pid"
        script = "sleep 300 & echo $! > %s; wait" % pid_file
        result = self.tier("t", "--results", "none", "--", "sh", "-c", script, env={"TIER_IDLE_TIMEOUT": "1"})
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
            env={"TIER_LOCK_TIMEOUT": "1"},
        )
        self.assertEqual(result.returncode, 75)
        self.assertLess(result.elapsed, 3)
        self.assertFalse(marker.exists())

    def _xml(self, tier_dir="out", cases=(("a.B", "t1", "0.1"),)):
        (self.root / tier_dir).mkdir(exist_ok=True)
        body = "".join('<testcase classname="%s" name="%s" time="%s"/>' % c for c in cases)
        (self.root / tier_dir / "TEST-a.xml").write_text("<testsuite>%s</testsuite>" % body)

    def _timings(self, entries, name="1.json"):
        import json

        (self.root / "tm").mkdir(exist_ok=True)
        (self.root / "tm" / name).write_text(json.dumps({"tier": "t", "tests": entries}))

    @staticmethod
    def _entry(kind="jvm", ms=10, budget=1000, status="ok", method="t1"):
        return {"class": "a.B", "method": method, "kind": kind, "sdk": "default", "ms": ms, "budgetMs": budget, "status": status}

    def _report(self):
        import json

        return json.loads((self.root / "build" / "test-results" / "t" / "timings.json").read_text())

    def test_listener_over_budget_fails(self):
        self._xml()
        self._timings([self._entry(ms=1200, status="over")])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--", "true")
        self.assertEqual(result.returncode, 3)
        self.assertIn("over budget: jvm a.B#t1 1200 ms > 1000 ms", result.stderr)

    def test_exempt_and_warmup_pass(self):
        self._xml(cases=(("a.B", "t1", "0.1"), ("a.B", "t2", "0.1")))
        self._timings(
            [
                self._entry(kind="robolectric", ms=12000, budget=20000, status="warmup"),
                self._entry(ms=6000, budget=None, status="exempt", method="t2"),
            ]
        )
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--", "true")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self._report()["overBudget"], 0)

    def test_emulator_xml_over_budget_fails(self):
        self._xml(cases=(("a.B", "t1", "10.5"),))
        result = self.tier("t", "--results", "out/*.xml", "--emulator", "--", "true")
        self.assertEqual(result.returncode, 3)
        self.assertIn("over budget: emulator a.B#t1 10500 ms > 10000 ms", result.stderr)

    def test_missing_timing_files_fail(self):
        self._xml()
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--", "true")
        self.assertEqual(result.returncode, 2)
        self.assertIn("wrote no timing files", result.stderr)

    def test_xml_without_listener_entry_is_not_budgeted(self):
        self._xml(cases=(("a.B", "t1", "0.1"), ("c.D", "other", "0.1")))
        self._timings([self._entry()])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--", "true")
        self.assertEqual(result.returncode, 2)
        self.assertIn("without a timing entry: c.D#other", result.stderr)
        report = self._report()
        self.assertEqual(len(report["budgetChecks"]), 1)
        self.assertEqual(report["unbudgeted"], ["c.D#other"])

    def test_slowest_lists_top_ten(self):
        self._xml()
        self._timings([self._entry(ms=i, method="m%d" % i) for i in range(1, 16)] + [self._entry(ms=0)])
        result = self.tier("t", "--results", "out/*.xml", "--timings", "tm/*.json", "--", "true")
        self.assertEqual(result.returncode, 0, result.stderr)
        slowest = self._report()["slowest"]
        self.assertEqual([c["ms"] for c in slowest], list(range(15, 5, -1)))


if __name__ == "__main__":
    unittest.main()
