"""Validate one T2 tier's evidence without booting or contacting a device."""
import argparse
from collections import Counter
import json
import math
from pathlib import Path
import sys


def summarize(report, avd):
    if report.get("tier") != "smoke-" + avd:
        raise ValueError("tier does not match the requested AVD")
    tests = report.get("tests", [])
    if not tests:
        raise ValueError("no test evidence")
    counts = Counter(test.get("status") for test in tests)
    if set(counts) - {"passed", "failed", "skipped"}:
        raise ValueError("unknown test verdict")
    wall = report.get("wallSeconds")
    if isinstance(wall, bool) or not isinstance(wall, (float, int)) or not math.isfinite(wall) or wall < 0:
        raise ValueError("missing or invalid wall time")
    if not report.get("started"):
        raise ValueError("missing run date")
    passed = (report.get("exitCode") == 0 and counts["passed"] > 0 and counts["failed"] == 0
              and not report.get("errors") and not report.get("hangs") and report.get("overBudget") == 0)
    return {"avd": avd, "started": report["started"], "tests": len(tests),
            "passed": counts["passed"], "skipped": counts["skipped"], "failed": counts["failed"],
            "wallSeconds": wall, "status": "passed" if passed else "failed"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--avd", choices=("fold", "small", "api29"), required=True)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        result = summarize(json.loads(args.input.read_text()), args.avd)
    except (OSError, ValueError, TypeError, AttributeError) as error:
        result = {"avd": args.avd, "status": "failed", "reason": str(error)}
    result["source"] = str(args.input)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print("matrix-result:", args.avd, result["status"])
    return 0 if result["status"] == "passed" else 1


if __name__ == "__main__":
    sys.exit(main())
