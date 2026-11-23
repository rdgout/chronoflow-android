#!/usr/bin/env python3
"""Measures one installed comparison target under the caller's device lock."""
import argparse
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

TEST_CLASS = "net.chronoflow.benchmark.LaunchComparisonBenchmark"
RUNNER = "net.chronoflow.benchmark/androidx.test.runner.AndroidJUnitRunner"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", choices=("Native", "Rn"))
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--suppression", default="")
    args = parser.parse_args()
    adb = [os.environ["ADB"], "-s", os.environ["ANDROID_SERIAL"]]
    remote = "/sdcard/Android/media/net.chronoflow.benchmark/cf-compare-" + args.target
    command = adb + ["shell", "am", "instrument", "-w", "-r",
                     "-e", "class", TEST_CLASS, "-e", "cf.compareTarget", args.target,
                     "-e", "androidx.benchmark.enabledRules", "Macrobenchmark",
                     "-e", "additionalTestOutputDir", remote,
                     "-e", "androidx.benchmark.output.enable", "true",
                     "-e", "timeout_msec", "300000"]
    if args.suppression:
        command += ["-e", "androidx.benchmark.suppressErrors", args.suppression]
    if os.environ.get("PERF_CLEAR") == "1":
        command += ["-e", "cf.perfClear", "1"]
    command.append(RUNNER)
    if os.environ.get("PERF_COMPARE_RN_DRY_RUN"):
        print("+ " + " ".join(command))
        return 0
    args.output.mkdir(parents=True, exist_ok=True)
    failure, seconds = None, "0"
    try:
        subprocess.run(adb + ["shell", "rm", "-rf", remote], check=True, timeout=30)
        subprocess.run(adb + ["shell", "mkdir", "-p", remote], check=True, timeout=30)
        measured = subprocess.run(command, capture_output=True, text=True, timeout=600)
        output = measured.stdout + measured.stderr
        print(output, end="", flush=True)
        (args.output / "instrumentation.txt").write_text(output)
        elapsed = re.search(r"(?m)^Time:\s*([0-9]+(?:\.[0-9]+)?)\s*$", output)
        if elapsed:
            seconds = elapsed[1]
        if measured.returncode or not re.search(r"(?m)^OK \(1 test\)\s*$", output) or not elapsed:
            failure = "comparison instrumentation did not report one successful timed test"
        # Pull even a failed pass so its traces and partial metrics remain reviewable.
        subprocess.run(adb + ["pull", remote + "/.", str(args.output)], check=True, timeout=120)
        if not list(args.output.rglob("*-benchmarkData.json")):
            failure = failure or "comparison pass produced no benchmark metrics"
    except (OSError, subprocess.SubprocessError) as error:
        failure = str(error)
    suite = ET.Element("testsuite", name=TEST_CLASS, tests="1", failures=str(int(failure is not None)))
    case = ET.SubElement(suite, "testcase", classname=TEST_CLASS,
                         name="coldSignedOut[" + args.target + "]", time=seconds)
    if failure:
        ET.SubElement(case, "failure", message=failure).text = failure
        print(failure, file=sys.stderr)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(suite).write(args.report, encoding="utf-8", xml_declaration=True)
    return int(failure is not None)


if __name__ == "__main__":
    sys.exit(main())
