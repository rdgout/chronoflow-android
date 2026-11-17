#!/usr/bin/env python3
"""Checks Macrobenchmark results against benchmark/budgets.json and appends history rows.

Exit 0: every budgeted value passed (or is report-only). Exit 1: a missing file, test or metric, a non-finite or
negative value, or a value over its hard budget. Exit 2: a usage error. Exit 4: the emulator CPU band is the only failure.
"""
import argparse
import datetime
import json
import math
import statistics
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# Metric ids that differ from the raw Macrobenchmark key; every other id maps to itself.
RAW_KEYS = {
    "timeToInitialDisplayMs": "timeToInitialDisplayMs",
    "timelineFirstFrameMs": "timelineFirstFrameFirstMs",
}
SIGNED_RAW = {"frameOverrunMs"}
CLASS_SUFFIX = {"startup": ".StartupBenchmark", "timeline": ".TimelineBenchmark"}
TIMELINE_HEADER = (
    "| Date | Commit | Class | Device | API | Hz | GPU | Font | Theme | Page | Build | First frame ms med/max | "
    "CPU P50/P90/P99 ms | Main/Render P99 ms | Overrun P99 ms | Jank % mean/max | Hitch ms/s mean/max | "
    "Placeholder max | Replay max ms | RSS anon MiB max | RSS growth MiB | Recompose max | GC max | Scroll | Last day | Result |"
)
BUILD = "benchmarkRelease, Partial(Require)"
TIMELINE_BUILD = "benchmarkRelease, Partial(Require), bounded-swipe-v2"
BAND_RATIO = 1.25
BAND_FLOOR_MS = 1.0
ABSOLUTE_RISES = {"placeholder": 2, "gc": 1}
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


def _flatten(runs):
    for run in runs or []:
        if isinstance(run, list):
            yield from _flatten(run)
        else:
            yield run


def invalid_runs(bench, raw_key, signed=False):
    """Every run value of the metric (and its sampled twin) that is not a finite, non-negative number."""
    runs = list(_flatten((bench.get("metrics", {}).get(raw_key) or {}).get("runs")))
    runs += list(_flatten((bench.get("sampledMetrics", {}).get(raw_key) or {}).get("runs")))
    return [
        v
        for v in runs
        if not isinstance(v, (int, float))
        or isinstance(v, bool)
        or not math.isfinite(v)
        or (v < 0 and not signed)
    ]


def judge(value, gate, signed=False):
    if not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(value):
        return "FAIL", "non-finite"
    if value < 0 and not signed:
        return "FAIL", "negative"
    if gate is None or gate.get("report") or gate.get("band"):
        return "REPORT", ""
    if "warn" in gate:
        return ("WARN", "") if value > gate["warn"] else ("PASS", "")
    if "hard" in gate and value > gate["hard"]:
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
        bad = invalid_runs(bench, raw_key)
        if bad:
            print(
                "%s metric: %s %s has invalid runs %s FAIL (non-finite or negative)"
                % (label, entry["test"], entry["metric"], bad),
                file=sys.stderr,
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


def _num(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _fmt(value):
    return "%.1f" % value if _num(value) else str(value)


def _suffix(gate):
    parts = []
    if gate:
        for key in ("hard", "aim", "warn"):
            if key in gate:
                parts.append("%s=%s" % (key, gate[key]))
    return (" [" + " ".join(parts) + "]") if parts else ""


def timeline_gate(entry, device_class):
    """The `all` gate applies to every class and wins over the class cell."""
    return entry.get("all") or entry.get(device_class)


def derived_values(entry, bench, refresh_hz="60"):
    """Per-iteration derived metrics, or (None, reason) when an input is missing."""
    metrics = bench.get("metrics", {})
    sampled = bench.get("sampledMetrics", {})
    metric = entry["metric"]
    if metric in ("jankPercent", "hitchRatioMsPerS"):
        overrun = (sampled.get("frameOverrunMs") or metrics.get("frameOverrunMs") or {}).get("runs")
        if not overrun:
            return None, "frameOverrunMs is missing (needs API 31+)"
        if metric == "jankPercent":
            return [jank_percent(list(_flatten(r))) for r in overrun if list(_flatten(r))], ""
        try:
            hz = float(refresh_hz)
        except (TypeError, ValueError):
            return None, "refresh Hz is missing for active frame time"
        if not math.isfinite(hz) or hz <= 0:
            return None, "refresh Hz must be finite and positive"
        return [hitch_ms_per_s(list(_flatten(r)), refresh_hz=hz) for r in overrun], ""
    if metric == "memoryRssAnonGrowthKb":
        runs = (metrics.get("memoryRssAnonMaxKb") or {}).get("runs")
        if not runs:
            return None, "memoryRssAnonMaxKb is missing"
        return [runs[-1] - runs[0]], ""
    return None, "unknown derived metric %s" % metric


def history_features(c):
    """The numeric cells that the trend and the band compare, from one 26-cell history row."""

    def part(cell, index):
        pieces = cell.split("/")
        try:
            return float(pieces[index])
        except (IndexError, ValueError):
            return None

    return {
        "firstFrameMedianMs": part(c[11], 0),
        "frameDurationCpuMsP50": part(c[12], 0),
        "frameDurationCpuMsP90": part(c[12], 1),
        "frameDurationCpuMsP99": part(c[12], 2),
        "timelineMainFrameMsP99": part(c[13], 0),
        "timelineRenderThreadMsP99": part(c[13], 1),
        "memoryRssAnonMaxMiB": part(c[19], 0),
        "hitchMeanMsPerS": part(c[16], 0),
        "timelineReplayMaxMs": part(c[18], 0),
        "placeholderMax": part(c[17], 0),
        "gcMax": part(c[22], 0),
    }


def timeline_baseline(path, args, page):
    """Per feature, the median of the last three PASS rows of the same class, theme, font and page."""
    if not path or not Path(path).exists():
        return {}
    lines = Path(path).read_text().splitlines()
    start = next((i for i, l in enumerate(lines) if l.startswith("| Date |")), None)
    if start is None:
        return {}
    rows = []
    for line in lines[start + 2 :]:
        if not line.startswith("|"):
            break
        c = cells(line)
        if len(c) != 26 or c[25] != "PASS" or c[10] != TIMELINE_BUILD:
            continue
        if (c[2], c[8], c[7], c[9]) != (args.device_class, args.theme, args.font_scale, page):
            continue
        rows.append(history_features(c))
    rows = rows[-3:]
    baseline = {}
    for key in rows[0] if rows else []:
        values = [r[key] for r in rows if r[key] is not None]
        if values:
            baseline[key] = statistics.median(values)
    return baseline


def check_timeline(args, budgets):
    """Returns (exit code, values of this run by (test, metric, stat), context, benchmarks)."""
    label = budgets["labels"]["timeline"]
    benches, context = load_benchmarks(args.results_dir, "timeline")
    if not list(Path(args.results_dir).rglob("*-benchmarkData.json")):
        print("no benchmark results in %s" % args.results_dir, file=sys.stderr)
        return 1, {}, context, {}
    page = budgets.get("timelinePage", "host")
    baseline = timeline_baseline(args.history, args, page)
    failed = False
    band_missed = False
    features = {}
    for test in ("timelineFirstFrame", "timelineScroll", "timelineLastDay"):
        if test not in benches:
            print("%s metric: test %s is absent from the results" % (label, test), file=sys.stderr)
            failed = True

    def emit(test, metric, stat, value, unit, gate, outcome, why=""):
        print(
            "%s metric: %s %s %s=%s%s%s %s%s"
            % (label, test, metric, stat, _fmt(value), " " + unit if unit else "", _suffix(gate), outcome,
               " (%s)" % why if why else " (lower bound: pausable prefetch bodies excluded)" if metric == "timelineTargetsMaxMs" else "")
        )

    for entry in budgets["timeline"]:
        bench = benches.get(entry["test"])
        if bench is None:
            continue
        test, metric, stat, unit = entry["test"], entry["metric"], entry["stat"], entry.get("unit", "")
        raw_key = RAW_KEYS.get(metric, metric)
        if entry.get("derived"):
            vals, why = derived_values(entry, bench, args.refresh_hz)
            if vals is None:
                print("%s metric: %s %s %s" % (label, test, metric, why), file=sys.stderr)
                failed = True
                continue
        else:
            vals = stat_values(bench, raw_key, stat)
            if vals is None:
                note = " (needs API 31+)" if raw_key == "frameOverrunMs" else ""
                print("%s metric: %s %s is missing (%s)%s" % (label, test, metric, raw_key, note), file=sys.stderr)
                failed = True
                continue
            bad = invalid_runs(bench, raw_key, signed=bool(entry.get("signed")))
            if bad:
                print(
                    "%s metric: %s %s has invalid runs %s FAIL (non-finite or negative)" % (label, test, metric, bad),
                    file=sys.stderr,
                )
                failed = True
                continue
        gate = timeline_gate(entry, args.device_class)
        feature = None
        if metric == "frameDurationCpuMs" and stat in ("P90", "P99"):
            feature = "frameDurationCpuMs" + stat
        for value in vals:
            outcome, why = judge(value, gate, signed=bool(entry.get("signed")))
            if gate and gate.get("band") and feature in baseline and _num(value):
                base = baseline[feature]
                limit = max(BAND_RATIO * base, base + BAND_FLOOR_MS)
                if value > limit:
                    outcome = "BAND"
                    band_missed = True
                    print(
                        "Timeline band: %s %s > %s (baseline %s) BAND"
                        % (feature, _fmt(value), _fmt(limit), _fmt(base))
                    )
            emit(test, metric, stat, value, unit, gate, outcome, why)
            failed = failed or outcome == "FAIL"
        if stat == "each" and len(vals) > 1 and all(_num(v) for v in vals):
            emit(test, metric, "mean", sum(vals) / len(vals), unit, None, "REPORT")
            emit(test, metric, "max", max(vals), unit, None, "REPORT")
        features[(test, metric, stat)] = vals

    for test, names in budgets.get("timelineReportOnly", {}).items():
        bench = benches.get(test)
        if bench is None:
            continue
        for name in names:
            vals = stat_values(bench, name, "each")
            if vals is None:
                print("%s metric: %s %s is missing (%s)" % (label, test, name, name), file=sys.stderr)
                failed = True
                continue
            if invalid_runs(bench, name):
                print("%s metric: %s %s has invalid runs FAIL (non-finite or negative)" % (label, test, name),
                      file=sys.stderr)
                failed = True
                continue
            emit(test, name, "mean", sum(vals) / len(vals), "", None, "REPORT")
            emit(test, name, "max", max(vals), "", None, "REPORT")
            features[(test, name, "each")] = vals
    if band_missed and not failed:
        return 4, features, context, benches
    return (1 if failed else 0), features, context, benches


def timeline_row(args, budgets, code, features, context, benches):
    def got(test, metric, stat):
        return features.get((test, metric, stat))

    def one(vals, fn):
        return fn(vals) if vals and all(_num(v) for v in vals) else None

    def pair(a, b, fmt="%.1f"):
        return "/".join(fmt % v if v is not None else "n/a" for v in (a, b))

    def single(v, fmt="%.1f"):
        return fmt % v if v is not None else "n/a"

    ff = got("timelineFirstFrame", "timelineFirstFrameMs", "each")
    ff_med = one(ff, statistics.median)
    ff_max = one(ff, max)
    cpu = [one(got("timelineScroll", "frameDurationCpuMs", s), lambda v: v[0]) for s in ("P50", "P90", "P99")]
    main99 = one(got("timelineScroll", "timelineMainFrameMsP99", "each"), max)
    render99 = one(got("timelineScroll", "timelineRenderThreadMsP99", "each"), max)
    overrun = one(got("timelineScroll", "frameOverrunMs", "P99"), lambda v: v[0])
    jank = got("timelineScroll", "jankPercent", "each")
    hitch = got("timelineScroll", "hitchRatioMsPerS", "each")
    placeholder = one(got("timelineScroll", "timelinePlaceholderCount", "each"), max)
    replay = one(got("timelineScroll", "timelineReplayMaxMs", "each"), max)
    rss = one(got("timelineScroll", "memoryRssAnonMaxKb", "each"), lambda v: max(v) / 1024.0)
    growth = one(got("timelineScroll", "memoryRssAnonGrowthKb", "single"), lambda v: v[0] / 1024.0)
    recompose = one(got("timelineScroll", "timelineRecomposeCount", "each"), max)
    gc = one(got("timelineScroll", "timelineGcCount", "each"), max)
    build = context.get("build", {}) if isinstance(context, dict) else {}
    result = "FAIL" if code == 1 else ("BAND" if code == 4 else "PASS")
    columns = [
        datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d"),
        args.commit, args.device_class, build.get("model", "unknown"), str(build.get("version", {}).get("sdk", "unknown")),
        args.refresh_hz, args.gpu, args.font_scale, args.theme, budgets.get("timelinePage", "host"), TIMELINE_BUILD,
        pair(ff_med, ff_max),
        "/".join(single(v) for v in cpu),
        pair(main99, render99),
        single(overrun),
        pair(one(jank, lambda v: sum(v) / len(v)), one(jank, max)),
        pair(one(hitch, lambda v: sum(v) / len(v)), one(hitch, max)),
        single(placeholder, "%d"),
        single(replay),
        single(rss),
        single(growth),
        single(recompose, "%d"),
        single(gc, "%d"),
        "ok" if "timelineScroll" in benches else "missing",
        "ok" if "timelineLastDay" in benches else "missing",
        result,
    ]
    return "| " + " | ".join(columns) + " |"


def timeline_trend(row, baseline):
    now = history_features(cells(row))
    for key, new in now.items():
        old = baseline.get(key)
        if old is None or new is None:
            continue
        if key in ("placeholderMax", "gcMax"):
            rise = ABSOLUTE_RISES["placeholder" if key == "placeholderMax" else "gc"]
            if new - old >= rise:
                print("Timeline trend: %s %s -> %s (+%s) FLAGGED" % (key, _fmt(old), _fmt(new), _fmt(new - old)))
            continue
        if old == 0:
            continue
        change = (new - old) / old
        if abs(change) > TREND_LIMIT:
            print("Timeline trend: %s %s -> %s (%+.0f%%) FLAGGED" % (key, _fmt(old), _fmt(new), change * 100))


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


def write_timeline_history(args, budgets, code, features, context, benches):
    row = timeline_row(args, budgets, code, features, context, benches)
    page = budgets.get("timelinePage", "host")
    timeline_trend(row, timeline_baseline(args.history, args, page))
    row_file = Path(args.row_file)
    row_file.parent.mkdir(parents=True, exist_ok=True)
    row_file.write_text(row + "\n")
    if not (args.append and args.history):
        return
    path = Path(args.history)
    lines = path.read_text().splitlines() if path.exists() else []
    if any(l.startswith("| Date |") for l in lines):
        _, end = history_rows(lines)
        lines.insert(end, row)
    else:
        at = next((i for i, l in enumerate(lines) if l.strip() == "## Runs"), None)
        table = [TIMELINE_HEADER, "|" + "---|" * 26, row]
        if at is None:
            lines += ["", "## Runs", ""] + table
        else:
            lines[at + 1 : at + 1] = [""] + table
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
    parser.add_argument("--row-file", default=str(ROOT / "build" / "perf" / "timeline-row.md"))
    args = parser.parse_args(argv)
    budgets = json.loads(Path(args.budgets).read_text())
    if args.suite == "timeline":
        code, features, context, benches = check_timeline(args, budgets)
        if args.history or code != 0 or features:
            write_timeline_history(args, budgets, code, features, context, benches)
        return code
    code, outcomes, values, context = check(args, budgets)
    if args.history and args.suite == "startup" and values:
        write_history(args, code, outcomes, values, context)
    return code


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
