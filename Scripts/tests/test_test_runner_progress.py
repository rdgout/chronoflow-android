import importlib.util
import io
import os
import sys
import unittest

from helpers import SCRIPTS, run, temp_dir

PROGRESS = str(SCRIPTS / "test_runner_progress.py")
TEST_CLASS = "net.chronoflow.benchmark.TimelineBenchmark"
spec = importlib.util.spec_from_file_location("test_runner_progress", PROGRESS)
progress = importlib.util.module_from_spec(spec)
spec.loader.exec_module(progress)

ADB = r"""#!/usr/bin/env python3
import os
import signal
print('I/TestRunner(123): started: timelineScrollWide(net.chronoflow.benchmark.TimelineBenchmark)', flush=True)
with open(os.environ['PROGRESS_FIFO'], 'w') as ready:
    ready.write(str(os.getpid()))
signal.pause()
"""

COMMAND = r"""import os
from pathlib import Path
import signal
import sys
with open(os.environ['PROGRESS_FIFO']) as ready:
    Path(os.environ['MONITOR_PID']).write_text(ready.read())
if os.environ.get('HANG_COMMAND') == '1':
    signal.pause()
sys.exit(int(sys.argv[1]))
"""


class TestRunnerProgressTest(unittest.TestCase):
    def setUp(self):
        self.tmp = temp_dir(self)

    def fixture(self, status=0):
        fifo = self.tmp / "ready"
        os.mkfifo(fifo)
        adb = self.tmp / "adb"
        adb.write_text(ADB)
        adb.chmod(0o755)
        env = {"PROGRESS_FIFO": str(fifo), "MONITOR_PID": str(self.tmp / "monitor.pid")}
        command = [sys.executable, "-I", PROGRESS, "--adb", str(adb), "--serial", "test-device",
                   "--test-class", TEST_CLASS, "--", sys.executable, "-I", "-c", COMMAND, str(status)]
        return command, env

    def assert_monitor_stopped(self):
        pid = int((self.tmp / "monitor.pid").read_text())
        with self.assertRaises(ProcessLookupError):
            os.kill(pid, 0)

    def test_only_started_and_finished_events_for_the_exact_class_are_forwarded(self):
        events = io.StringIO(
            "--------- beginning of main\n"
            "I/TestRunner(123): run started: 5 tests\n"
            "D/UiDevice(123): started: timelineScrollWide(net.chronoflow.benchmark.TimelineBenchmark)\n"
            "I/TestRunner(123): started: other(net.chronoflow.benchmark.StartupBenchmark)\n"
            "I/TestRunner(123): started: other(net.chronoflow.benchmark.TimelineBenchmarkExtra)\n"
            "I/TestRunner(123): started: timelineScrollWide(net.chronoflow.benchmark.TimelineBenchmark)\n"
            "I/TestRunner( 123): finished: timelineScrollWide(net.chronoflow.benchmark.TimelineBenchmark)\r\n"
            "I/TestRunner(123): run finished: 5 tests, 0 failed, 0 ignored\n"
        )
        output = io.StringIO()
        progress.forward_progress(events, TEST_CLASS, output)
        self.assertEqual(output.getvalue(),
                         "perf-timeline: started: timelineScrollWide(net.chronoflow.benchmark.TimelineBenchmark)\n"
                         "perf-timeline: finished: timelineScrollWide(net.chronoflow.benchmark.TimelineBenchmark)\n")

    def test_silence_produces_no_heartbeat(self):
        output = io.StringIO()
        progress.forward_progress(io.StringIO(""), TEST_CLASS, output)
        self.assertEqual(output.getvalue(), "")

    def test_progress_is_forwarded_and_the_monitor_is_reaped_after_success(self):
        command, env = self.fixture()
        result = run(command, env, timeout=10)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("perf-timeline: started: timelineScrollWide", result.stdout)
        self.assert_monitor_stopped()

    def test_gradle_failure_is_preserved_and_the_monitor_is_reaped(self):
        command, env = self.fixture(status=7)
        result = run(command, env, timeout=10)
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assert_monitor_stopped()

    def test_progress_does_not_hide_a_stalled_method_from_the_idle_cap(self):
        command, env = self.fixture()
        env.update({"HANG_COMMAND": "1", "RUN_TIER_ROOT": str(self.tmp), "TIER_IDLE_TIMEOUT": "0.3",
                    "TIER_TOTAL_TIMEOUT": "5", "TIER_KILL_GRACE": "0.2", "TIER_STOP_CMD": "true"})
        result = run([sys.executable, "-I", str(SCRIPTS / "run_tier.py"), "progress", "--results", "none",
                      "--", *command], env, timeout=10)
        self.assertEqual(result.returncode, 124, result.stderr)
        self.assertIn("perf-timeline: started: timelineScrollWide", result.stdout)
        log = (self.tmp / "build/test-results/progress/run.log").read_text()
        self.assertIn("idle cap", log)
        self.assertLess(result.elapsed, 3)
        self.assert_monitor_stopped()


if __name__ == "__main__":
    unittest.main()
