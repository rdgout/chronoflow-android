import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent.parent
ROOT = SCRIPTS.parent


class LockHolder:
    """Holds the device lock in a helper process until closed."""

    def __init__(self, lock_file):
        code = (
            "import fcntl,os,sys\n"
            "fd=os.open(sys.argv[1],os.O_RDWR|os.O_CREAT,0o644)\n"
            "fcntl.flock(fd,fcntl.LOCK_EX)\n"
            "os.write(fd,b'pid 1 test-holder')\n"
            "print('ready',flush=True)\n"
            "sys.stdin.read()\n"
        )
        self.proc = subprocess.Popen(
            [sys.executable, "-I", "-c", code, str(lock_file)],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            text=True,
        )
        assert self.proc.stdout.readline().strip() == "ready"

    def close(self):
        self.proc.stdin.close()
        self.proc.wait(timeout=5)
        self.proc.stdout.close()


def run(args, env=None, timeout=30, cwd=None):
    start = time.monotonic()
    result = subprocess.run(
        args,
        env=dict(os.environ, **(env or {})),
        capture_output=True,
        text=True,
        timeout=timeout,
        cwd=cwd,
    )
    result.elapsed = time.monotonic() - start
    return result


def temp_dir(testcase):
    tmp = tempfile.TemporaryDirectory()
    testcase.addCleanup(tmp.cleanup)
    return Path(tmp.name)
