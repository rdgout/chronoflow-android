#!/usr/bin/env python3
"""Checks Macrobenchmark results against benchmark/budgets.json and appends history rows.

Exit 0: every budgeted value passed (or is report-only). Exit 1: a missing file, test or metric, a non-finite or
negative value, or a value over its hard budget. Exit 2: a usage error.
"""
import argparse
import datetime
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW_KEYS = {
    "timeToInitialDisplayMs": "timeToInitialDisplayMs",
    "timelineFirstFrameMs": "timelineFirstFrameFirstMs",
}
CLASS_SUFFIX = {"startup": ".StartupBenchmark", "timeline": ".TimelineBenchmark"}
HISTORY_HEADER = "| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Build | TTID ms med/min/max | Result |"
BUILD = "benchmarkRelease, Partial(UseIfAvailable)"
TREND_LIMIT = 0.25
STAT_KEYS = {"median": "median", "min": "minimum", "max": "maximum"}


def jank_percent(runs):
    return 100.0 * sum(1 for v in runs if v > 0) / len(runs)


def hitch_ms_per_s(runs, window_ms=None, refresh_hz=60.0):
    if window_ms is None:
        window_ms = len(runs) * 1000.0 / refresh_hz
    return sum(max(0.0, v) for v in runs) / (window_ms / 1000.0)


def load_benchmarks(results_dir, suite):
    found = {}
    context = {}
    for path in sorted(Path(results_dir).rglob("*-benchmarkData.json")):
        data = json.loads(path.read_text())
        context = context or data.get("context", {})
        for bench in data.get("benchmarks", []):
            if str(bench.get("className", "")).endswith(CLASS_SUFFIX[suite]):
                found[bench["name"]] = bench
    return found, context


def stat_values(bench, raw_key, stat):
    """Returns the list of values for a stat, or None when the metric or stat is missing."""
    if stat in STAT_KEYS:
        metric = bench.get("metrics", {}).get(raw_key)
        value = None if metric is None else metric.get(STAT_KEYS[stat])
        return None if value is None else [value]
    if stat == "each":
        metric = bench.get("metrics", {}).get(raw_key)
        runs = None if metric is None else metric.get("runs")
        return runs if runs else None
    if stat == "present":
        return [0.0] if raw_key in bench.get("metrics", {}) or raw_key in bench.get("sampledMetrics", {}) else None
    sampled = bench.get("sampledMetrics", {}).get(raw_key)
    value = None if sampled is None else sampled.get(stat)
    return None if value is None else [value]


def judge(value, gate):
    if not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(value):
        return "FAIL", "non-finite"
    if value < 0:
        return "FAIL", "negative"
    if gate is None or gate.get("report"):
        return "REPORT", ""
    if value > gate["hard"]:
        return "FAIL", "over hard"
    if "aim" in gate and value > gate["aim"]:
        return "AIM-MISSED", ""
    return "PASS", ""


def check(args, budgets):
    label = budgets["labels"][args.suite]
    benches, context = load_benchmarks(args.results_dir, args.suite)
    if not list(Path(args.results_dir).rglob("*-benchmarkData.json")):
        print("no benchmark results in %s" % args.results_dir, file=sys.stderr)
        return 1, [], {}, context
    failed = False
    outcomes = []
    values = {}
    for entry in budgets[args.suite]:
        bench = benches.get(entry["test"])
        if bench is None:
            print("%s metric: test %s is absent from the results" % (label, entry["test"]), file=sys.stderr)
            failed = True
            continue
        raw_key = RAW_KEYS[entry["metric"]]
        vals = stat_values(bench, raw_key, entry["stat"])
        if vals is None:
            print(
                "%s metric: %s %s is missing (%s)" % (label, entry["test"], entry["metric"], raw_key), file=sys.stderr
            )
            failed = True
            continue
        gate = entry.get(args.device_class)
        for value in vals:
            outcome, why = judge(value, gate)
            suffix = ""
            if gate and not gate.get("report"):
                suffix = " hard=%s" % gate["hard"] + (" aim=%s" % gate["aim"] if "aim" in gate else "")
            shown = "%.1f" % value if isinstance(value, (int, float)) and math.isfinite(value) else str(value)
            print(
                "%s metric: %s %s %s=%s %s%s %s%s"
                % (label, entry["test"], entry["metric"], entry["stat"], shown, entry["unit"], suffix, outcome,
                   " (%s)" % why if why else "")
            )
            outcomes.append(outcome)
            failed = failed or outcome == "FAIL"
        if args.suite == "startup":
            metrics = bench.get("metrics", {}).get(raw_key, {})
            values = {k: metrics.get(k) for k in ("median", "minimum", "maximum")}
    return (1 if failed else 0), outcomes, values, context


def result_of(code, outcomes):
    if code != 0:
        return "FAIL"
    return "PASS" if "PASS" in outcomes or "AIM-MISSED" in outcomes else "REPORT"


def history_rows(lines):
    start = next((i for i, l in enumerate(lines) if l.startswith("| Date |")), None)
    if start is None:
        raise SystemExit("check-benchmarks: history file has no Runs table")
    end = start + 1
    while end < len(lines) and lines[end].startswith("|"):
        end += 1
    return start, end


def cells(row):
    return [c.strip() for c in row.strip().strip("|").split("|")]


def trend(lines, args, median):
    start, end = history_rows(lines)
    for row in reversed(lines[start + 2 : end]):
        c = cells(row)
        if len(c) != 12 or c[11] not in ("PASS", "REPORT"):
            continue
        if (c[2], c[8], c[7]) != (args.device_class, args.theme, args.font_scale):
            continue
        old = float(c[10].split("/")[0])
        change = (median - old) / old
        if abs(change) > TREND_LIMIT:
            print(
                "Startup trend: timeToInitialDisplayMs %.1f -> %.1f (%+.0f%%) FLAGGED" % (old, median, change * 100)
            )
        return


def write_history(args, code, outcomes, values, context):
    path = Path(args.history)
    lines = path.read_text().splitlines()
    median = values.get("median")
    if isinstance(median, (int, float)) and math.isfinite(median):
        trend(lines, args, median)
    build = context.get("build", {})
    ttid = "/".join(
        "%.1f" % values[k] if isinstance(values.get(k), (int, float)) and math.isfinite(values[k]) else "n/a"
        for k in ("median", "minimum", "maximum")
    )
    row = "| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (
        datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d"),
        args.commit, args.device_class, build.get("model", "unknown"), build.get("version", {}).get("sdk", "unknown"),
        args.refresh_hz, args.gpu, args.font_scale, args.theme, BUILD, ttid, result_of(code, outcomes),
    )
    if args.append:
        _, end = history_rows(lines)
        lines.insert(end, row)
        path.write_text("\n".join(lines) + "\n")


def main(argv):
    parser = argparse.ArgumentParser(prog="check-benchmarks.py")
    parser.add_argument("--suite", required=True, choices=sorted(CLASS_SUFFIX))
    parser.add_argument("--device-class", required=True, choices=["emulator", "reference", "lowEnd", "unknown"])
    parser.add_argument("--results-dir", required=True)
    parser.add_argument("--budgets", default=str(ROOT / "benchmark" / "budgets.json"))
    parser.add_argument("--history")
    parser.add_argument("--theme", default="light", choices=["light", "dark"])
    parser.add_argument("--font-scale", default="1.0")
    parser.add_argument("--refresh-hz", default="unknown")
    parser.add_argument("--gpu", default="unknown")
    parser.add_argument("--commit", default="unknown")
    parser.add_argument("--append", action="store_true")
    args = parser.parse_args(argv)
    budgets = json.loads(Path(args.budgets).read_text())
    code, outcomes, values, context = check(args, budgets)
    if args.history and args.suite == "startup" and values:
        write_history(args, code, outcomes, values, context)
    return code


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
