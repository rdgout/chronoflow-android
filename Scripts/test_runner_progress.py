#!/usr/bin/env python3
"""Forward real instrumentation progress while a connected Gradle task is quiet."""
import argparse
import re
import signal
import subprocess
import sys
import threading


def forward_progress(lines, test_class, output):
    event = re.compile(
        r"^I/TestRunner\s*\(\s*\d+\):\s+"
        r"((?:started|finished): [^()\r\n]+\(" + re.escape(test_class) + r"\))$"
    )
    for line in lines:
        match = event.fullmatch(line.rstrip("\r\n"))
        if match:
            print("perf-timeline: " + match.group(1), file=output, flush=True)


def run(adb, serial, test_class, command):
    monitor = subprocess.Popen(
        [adb, "-s", serial, "logcat", "-T", "1", "-v", "brief", "-s", "TestRunner:I", "*:S"],
        stdin=subprocess.DEVNULL,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        text=True,
    )
    reader = threading.Thread(target=forward_progress, args=(monitor.stdout, test_class, sys.stdout), daemon=True)
    reader.start()
    try:
        return subprocess.call(command)
    finally:
        monitor.terminate()
        try:
            monitor.wait(timeout=5)
        except subprocess.TimeoutExpired:
            monitor.kill()
            monitor.wait(timeout=5)
        reader.join(timeout=5)
        if reader.is_alive():
            raise RuntimeError("TestRunner progress reader did not stop")
        monitor.stdout.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--test-class", required=True)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command
    if command[:1] == ["--"]:
        command = command[1:]
    if not command:
        parser.error("a command is required after --")

    def terminate(signum, _frame):
        raise SystemExit(128 + signum)

    signal.signal(signal.SIGTERM, terminate)
    return run(args.adb, args.serial, args.test_class, command)


if __name__ == "__main__":
    sys.exit(main())
