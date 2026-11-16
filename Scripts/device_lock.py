#!/usr/bin/env python3
"""Host-wide lock that serializes every device tier (OQ-0a-21)."""
import fcntl
import os
import subprocess
import sys
import time

EXIT_TIMEOUT = 75


def lock_path():
    return os.environ.get("CF_DEVICE_LOCK") or os.path.expanduser("~/.android/cf-emulator.lock")


def _holder(path):
    try:
        with open(path) as handle:
            return handle.read().strip() or "another process"
    except OSError:
        return "another process"


def acquire(path, wait_s, label="unknown"):
    """Returns the locked descriptor, or None when wait_s passed. The descriptor is not inheritable."""
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    fd = os.open(path, os.O_RDWR | os.O_CREAT, 0o644)
    deadline = time.monotonic() + wait_s
    warned = False
    while True:
        try:
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
            break
        except OSError:
            if time.monotonic() >= deadline:
                os.close(fd)
                return None
            if not warned:
                print("device-lock: waiting for " + _holder(path), file=sys.stderr, flush=True)
                warned = True
            time.sleep(1)
    os.ftruncate(fd, 0)
    os.write(fd, ("pid %d %s" % (os.getpid(), label)).encode())
    return fd


def release(fd):
    if fd is not None:
        fcntl.flock(fd, fcntl.LOCK_UN)
        os.close(fd)


def is_held(path):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    fd = os.open(path, os.O_RDWR | os.O_CREAT, 0o644)
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        return True
    finally:
        os.close(fd)
    return False


def main(argv):
    if "--probe" in argv:
        return 0 if is_held(lock_path()) else 1
    wait_s = float(os.environ.get("TIER_LOCK_TIMEOUT", "3600"))
    command = []
    if "--" in argv:
        split = argv.index("--")
        command = argv[split + 1 :]
        argv = argv[:split]
    if "--wait" in argv:
        wait_s = float(argv[argv.index("--wait") + 1])
    if not command:
        print("usage: device_lock.py [--wait <s>] -- <command...> | --probe", file=sys.stderr)
        return 2
    env = dict(os.environ, CF_DEVICE_LOCK_HELD="1")
    if os.environ.get("CF_DEVICE_LOCK_HELD") == "1":
        return subprocess.call(command, env=env)
    fd = acquire(lock_path(), wait_s, " ".join(command))
    if fd is None:
        print("device-lock: timed out after %s s" % int(wait_s), file=sys.stderr)
        return EXIT_TIMEOUT
    try:
        return subprocess.call(command, env=env)
    finally:
        release(fd)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
