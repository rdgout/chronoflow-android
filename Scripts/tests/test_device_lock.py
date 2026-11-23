import sys
import unittest

from helpers import SCRIPTS, LockHolder, run, temp_dir

LOCK = str(SCRIPTS / "device_lock.py")


class DeviceLockTest(unittest.TestCase):
    def setUp(self):
        self.lock_file = temp_dir(self) / "lock"
        self.env = {"CF_DEVICE_LOCK": str(self.lock_file), "CF_DEVICE_LOCK_HELD": ""}

    def test_runs_when_free(self):
        result = run([sys.executable, "-I", LOCK, "--", "sh", "-c", "echo $CF_DEVICE_LOCK_HELD"], self.env)
        self.assertEqual(result.returncode, 0)
        self.assertEqual(result.stdout.strip(), "1")

    def test_waits_then_times_out(self):
        holder = LockHolder(self.lock_file)
        self.addCleanup(holder.close)
        result = run([sys.executable, "-I", LOCK, "--wait", "1", "--", "true"], self.env)
        self.assertEqual(result.returncode, 75)
        self.assertIn("waiting for", result.stderr)
        self.assertIn("timed out", result.stderr)
        self.assertLess(result.elapsed, 3)

    def test_reentry_does_not_wait(self):
        holder = LockHolder(self.lock_file)
        self.addCleanup(holder.close)
        env = dict(self.env, CF_DEVICE_LOCK_HELD="1")
        result = run([sys.executable, "-I", LOCK, "--wait", "1", "--", "true"], env)
        self.assertEqual(result.returncode, 0)
        self.assertLess(result.elapsed, 1)

    def test_probe(self):
        self.assertEqual(run([sys.executable, "-I", LOCK, "--probe"], self.env).returncode, 1)
        holder = LockHolder(self.lock_file)
        self.addCleanup(holder.close)
        self.assertEqual(run([sys.executable, "-I", LOCK, "--probe"], self.env).returncode, 0)


if __name__ == "__main__":
    unittest.main()
