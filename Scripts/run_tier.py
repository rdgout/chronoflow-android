#!/usr/bin/env python3
"""Runs one test tier with an idle cap, a total cap, process-group kill and result collection."""
import datetime
import glob
import importlib.util
import json
import os
import shutil
import signal
import subprocess
import sys
import threading
import time
import xml.etree.ElementTree as ET
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def load_device_lock():
    spec = importlib.util.spec_from_file_location("device_lock", SCRIPTS / "device_lock.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


BUDGETS = {"jvm": 1000, "robolectric": 5000, "robolectricWarmup": 20000, "emulator": 10000}
FAILED_GATE_SUFFIX = ".gate-failed"


def parse_args(argv):
    if "--" not in argv:
        raise SystemExit(
            "usage: run-tier.sh <tier> [--device] [--emulator] --results <glob> [--timings <glob>] -- <command...>"
        )
    split = argv.index("--")
    head, command = argv[:split], argv[split + 1 :]
    tier, device, results, timings, emulator = None, False, [], [], False
    i = 0
    while i < len(head):
        if head[i] == "--device":
            device = True
        elif head[i] == "--emulator":
            emulator = True
        elif head[i] == "--results":
            i += 1
            results.append(head[i])
        elif head[i] == "--timings":
            i += 1
            timings.append(head[i])
        elif tier is None:
            tier = head[i]
        i += 1
    if tier is None or not command:
        raise SystemExit("run-tier: a tier name and a command are required")
    return tier, device, results, command, timings, emulator


def collect(root, out_dir, patterns):
    junit = out_dir / "junit"
    junit.mkdir(parents=True, exist_ok=True)
    files = []
    for pattern in patterns:
        for match in sorted(glob.glob(str(root / pattern), recursive=True)):
            path = Path(match)
            if path.is_file():
                files.append(path)
    for path in files:
        target = junit / str(path.relative_to(root)).replace(os.sep, "__")
        shutil.copyfile(path, target)
    return files, read_tests(files)


def read_tests(files):
    tests = []
    for path in files:
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        for case in tree.getroot().iter("testcase"):
            status = "passed"
            if case.find("failure") is not None or case.find("error") is not None:
                status = "failed"
            elif case.find("skipped") is not None:
                status = "skipped"
            tests.append(
                {
                    "class": case.get("classname", ""),
                    "name": case.get("name", ""),
                    "seconds": float(case.get("time", "0") or 0),
                    "status": status,
                }
            )
    return tests


def collect_timings(root, patterns, started_ns):
    files, entries, measured_dirs, failed_dirs = [], [], set(), set()
    for pattern in patterns:
        for match in sorted(glob.glob(str(root / pattern), recursive=True)):
            path = Path(match)
            if not path.is_file() or path.stat().st_mtime_ns < started_ns:
                continue
            try:
                data = json.loads(path.read_text())
            except (OSError, ValueError):
                continue
            if not isinstance(data, dict) or not isinstance(data.get("tests"), list):
                continue
            files.append(path)
            entries.extend(data["tests"])
            if data["tests"]:
                measured_dirs.add(path.parent)
            if any(entry["status"] == "over" for entry in data["tests"]):
                failed_dirs.add(path.parent)
    return files, entries, measured_dirs, failed_dirs


def missing_execution_evidence(root, patterns, started_ns, result_files, timing_files, measured_dirs):
    missing, failed_dirs = [], set()
    for pattern in patterns:
        marker_pattern = str(root / Path(pattern).parent / "execution.started")
        for match in sorted(glob.glob(marker_pattern, recursive=True)):
            marker = Path(match)
            if marker.stat().st_mtime_ns < started_ns:
                continue
            timing_dir = marker.parent
            result_dir = timing_dir.parent.parent / "test-results" / timing_dir.name
            if timing_dir in measured_dirs and not any(path.parent == result_dir for path in result_files):
                missing.append("%s wrote no result files" % result_dir.relative_to(root))
                failed_dirs.add(timing_dir)
            if not any(path.parent == timing_dir for path in timing_files):
                missing.append("%s wrote no timing files" % timing_dir.relative_to(root))
                failed_dirs.add(timing_dir)
    return sorted(set(missing)), failed_dirs


def unbudgeted_task_dirs(root, result_files, unbudgeted, timing_patterns):
    failed_dirs = set()
    for path in result_files:
        if not any("%s#%s" % (t["class"], t["name"]) in unbudgeted for t in read_tests([path])):
            continue
        result_dir = path.parent
        if result_dir.parent.name == "test-results":
            failed_dirs.add(result_dir.parent.parent / "cf-timings" / result_dir.name)
        else:
            for pattern in timing_patterns:
                parent = str(root / Path(pattern).parent)
                failed_dirs.update(Path(p) for p in glob.glob(parent, recursive=True))
                if not glob.has_magic(parent):
                    failed_dirs.add(Path(parent))
    return failed_dirs


def pending_gate_markers(root, patterns):
    markers = set()
    for pattern in patterns:
        marker_pattern = str(root / Path(pattern).parent) + FAILED_GATE_SUFFIX
        markers.update(Path(p) for p in glob.glob(marker_pattern, recursive=True))
    return markers


def gate_marker(timing_dir):
    return timing_dir.with_name(timing_dir.name + FAILED_GATE_SUFFIX)


def invalidate_task_outputs(root, timing_dirs):
    for directory in sorted(timing_dirs):
        directory.resolve().relative_to(root)
        directory.parent.mkdir(parents=True, exist_ok=True)
        # The retry marker must survive deletion of the Gradle task's declared outputs.
        gate_marker(directory).write_text("A fresh measured run must pass before this task can be reused.\n")
        if directory.parent.name != "cf-timings" or directory.parent.parent.name != "build":
            continue
        result_dir = directory.parent.parent / "test-results" / directory.name
        for output in (directory, result_dir):
            if output.exists():
                output.resolve().relative_to(root)
                shutil.rmtree(output)


def _bare(method):
    return method.split("[", 1)[0]


def budget_checks(tests, listener_entries, emulator):
    """Returns (checks, unbudgeted) for the run: listener entries, or one emulator entry per XML testcase."""
    if emulator:
        checks = []
        for test in tests:
            ms = int(round(test["seconds"] * 1000))
            status = "over" if ms > BUDGETS["emulator"] else "ok"
            checks.append(
                {"class": test["class"], "method": test["name"], "kind": "emulator", "sdk": "default",
                 "ms": ms, "budgetMs": BUDGETS["emulator"], "status": status}
            )
        return checks, []
    known = {(e["class"], e["method"]) for e in listener_entries}
    known |= {(e["class"], _bare(e["method"])) for e in listener_entries}
    unbudgeted = sorted(
        "%s#%s" % (t["class"], t["name"])
        for t in tests
        if (t["class"], t["name"]) not in known and (t["class"], _bare(t["name"])) not in known
    )
    return list(listener_entries), unbudgeted


def save_logcat(out_dir):
    android_home = os.environ.get("ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools")
    adb = os.environ.get("ADB", os.path.join(android_home, "platform-tools", "adb"))
    serial = os.environ.get("ANDROID_SERIAL", "emulator-5554")
    try:
        result = subprocess.run([adb, "-s", serial, "logcat", "-d"], capture_output=True, timeout=60)
        (out_dir / "logcat.txt").write_bytes(result.stdout)
    except (OSError, subprocess.SubprocessError):
        pass


def main(argv):
    tier, device, patterns, command, timing_patterns, emulator = parse_args(argv)
    root = Path(os.environ.get("RUN_TIER_ROOT") or SCRIPTS.parent).resolve()
    idle_cap = float(os.environ.get("TIER_IDLE_TIMEOUT", "600"))
    total_cap = float(os.environ.get("TIER_TOTAL_TIMEOUT", "3600"))
    grace = float(os.environ.get("TIER_KILL_GRACE", "5"))
    stop_cmd = os.environ.get("TIER_STOP_CMD", "./gradlew --stop")

    out_dir = root / "build" / "test-results" / tier
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    log_path = out_dir / "run.log"

    env = dict(os.environ)
    lock_fd = None
    lock_module = None
    if device:
        env["CF_DEVICE_LOCK_HELD"] = "1"
        if os.environ.get("CF_DEVICE_LOCK_HELD") != "1":
            lock_module = load_device_lock()
            wait_s = float(os.environ.get("TIER_LOCK_TIMEOUT", "3600"))
            lock_fd = lock_module.acquire(lock_module.lock_path(), wait_s, "run-tier " + tier)
            if lock_fd is None:
                print("device-lock: timed out after %d s" % int(wait_s), file=sys.stderr)
                return 75

    started = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    reference = out_dir / "invocation.started"
    reference.touch()
    started_ns = reference.stat().st_mtime_ns
    t0 = time.monotonic()
    state = {"last": time.monotonic()}
    log = open(log_path, "wb")
    proc = subprocess.Popen(
        command,
        cwd=str(root),
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )

    def pump():
        for chunk in iter(lambda: proc.stdout.read1(65536), b""):
            sys.stdout.buffer.write(chunk)
            sys.stdout.buffer.flush()
            log.write(chunk)
            log.flush()
            state["last"] = time.monotonic()

    reader = threading.Thread(target=pump, daemon=True)
    reader.start()

    cap = None
    while proc.poll() is None:
        now = time.monotonic()
        if now - t0 > total_cap:
            cap = "total cap"
        elif now - state["last"] > idle_cap:
            cap = "idle cap"
        if cap:
            break
        time.sleep(0.1)

    if cap:
        try:
            os.killpg(proc.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        end = time.monotonic() + grace
        while time.monotonic() < end and proc.poll() is None:
            time.sleep(0.1)
        try:
            os.killpg(proc.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        proc.wait()
        subprocess.run(stop_cmd, shell=True, cwd=str(root), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    reader.join(timeout=3)
    wall = time.monotonic() - t0
    if cap:
        message = "run-tier: %s stopped after %d s (%s)" % (tier, int(wall), cap)
        print(message, file=sys.stderr, flush=True)
        log.write((message + "\n").encode())
    log.close()

    code = proc.returncode
    if cap:
        exit_code = 124
    else:
        exit_code = code if code >= 0 else 128 - code
    files, tests = collect(root, out_dir, patterns)
    current_files = [path for path in files if path.stat().st_mtime_ns >= started_ns]
    current_tests = read_tests(current_files)
    timing_files, listener_entries, measured_dirs, failed_dirs = collect_timings(root, timing_patterns, started_ns)
    missing_evidence, missing_dirs = missing_execution_evidence(
        root, timing_patterns, started_ns, current_files, timing_files, measured_dirs
    )
    failed_dirs.update(missing_dirs)
    checks, unbudgeted = budget_checks(current_tests, listener_entries, emulator)
    over = [c for c in checks if c["status"] == "over"]
    pending = pending_gate_markers(root, timing_patterns)
    unresolved = set()
    if timing_patterns:
        failed_dirs.update(unbudgeted_task_dirs(root, current_files, set(unbudgeted), timing_patterns))
        if not files:
            failed_dirs.update(path.parent for path in timing_files)
    invalidate_task_outputs(root, failed_dirs)
    if exit_code == 0:
        fresh_dirs = {path.parent for path in timing_files}
        for marker in pending:
            directory = marker.with_name(marker.name.removesuffix(FAILED_GATE_SUFFIX))
            if directory in fresh_dirs and directory not in failed_dirs:
                marker.unlink()
            else:
                unresolved.add(directory)
        if not files:
            print("run-tier: %s produced no result files" % tier, file=sys.stderr)
            exit_code = 2
        elif missing_evidence:
            print("run-tier: " + "; ".join(missing_evidence), file=sys.stderr)
            exit_code = 2
        elif timing_patterns and current_files and not timing_files:
            print("run-tier: %s wrote no timing files" % tier, file=sys.stderr)
            exit_code = 2
        elif timing_patterns and unbudgeted:
            print("run-tier: %s ran tests without a timing entry: %s" % (tier, ", ".join(unbudgeted)), file=sys.stderr)
            exit_code = 2
        elif over:
            exit_code = 3
        elif unresolved:
            print("run-tier: requires fresh timing measurements: " + ", ".join(
                str(path.relative_to(root)) for path in sorted(unresolved)
            ), file=sys.stderr)
            exit_code = 2
    report = {
        "tier": tier,
        "command": command,
        "started": started,
        "wallSeconds": round(wall, 2),
        "exitCode": exit_code,
        "tests": tests,
        "measuredTests": len(current_tests),
        "budgets": BUDGETS,
        "budgetChecks": checks,
        "overBudget": len(over),
        "slowest": sorted(checks, key=lambda c: c["ms"], reverse=True)[:10],
    }
    if unbudgeted:
        report["unbudgeted"] = unbudgeted
    if missing_evidence:
        report["missingEvidence"] = missing_evidence
    if failed_dirs:
        report["failedTaskOutputs"] = [str(path.relative_to(root)) for path in sorted(failed_dirs)]
    if unresolved:
        report["pendingMeasurements"] = [str(path.relative_to(root)) for path in sorted(unresolved)]
    (out_dir / "timings.json").write_text(json.dumps(report, indent=2) + "\n")
    for c in over:
        print(
            "run-tier: over budget: %s %s#%s %d ms > %d ms" % (c["kind"], c["class"], c["method"], c["ms"], c["budgetMs"]),
            file=sys.stderr,
        )
    if exit_code != 0 and os.environ.get("TEST_DIAGNOSTICS") == "1" and tier.startswith(("smoke", "emu")):
        save_logcat(out_dir)
    if lock_fd is not None:
        lock_module.release(lock_fd)
    return exit_code


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
