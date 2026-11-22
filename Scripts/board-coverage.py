"""Report reference-free board capture coverage; never render or modify a board."""
import argparse
from fnmatch import fnmatchcase
import json
from pathlib import Path
import re
import sys

EXPECTED_COUNTS = {"total": 334, "excluded": 42, "inScope": 292, "groups": 194}
PROFILES = {"small", "pixel8", "foldInner", "landscapeSideNav", "largeFont", "pixel", "device"}
IGNORED = {"designsystem", "harness", "timeline", "material", "ios-parity", "fold-check",
           "resize", "font-scale", "process-death"}
VARIANTS = ("en_light", "en_dark", "nl_light", "nl_dark")
STRESS = ("nl_light", "en_dark")


def board_names(canvas):
    return {Path(name).name.removesuffix(".dc.html") for name in canvas["boards"]}


def group_name(board, names):
    name = board
    if name.startswith("Dark") and name[4:] in names:
        name = name[4:]
    for suffix in ("DarkNL", "Dark", "NL"):
        if name.endswith(suffix) and name[:-len(suffix)] in names:
            return name[:-len(suffix)]
    return name


def excluded_reason(board, scope):
    return next((reason for reason, patterns in scope["excluded"].items()
                 if any(fnmatchcase(board, pattern) for pattern in patterns)), None)


def required_cells(kind):
    phone = [f"pixel8_{v}" for v in VARIANTS]
    large = [f"largeFont_{v}" for v in STRESS]
    if kind in {"nonScreen", "notBuilt"}:
        return []
    if kind == "wideOnly":
        return [f"foldInner_{v}" for v in VARIANTS]
    if kind == "phoneWithWideSibling":
        return phone + large
    if kind == "largeFontOnly":
        return large
    if kind == "standard":
        return phone + [f"foldInner_{v}" for v in STRESS] + large
    raise ValueError(f"unknown group kind: {kind}")


def parse_capture(relative):
    path = Path(relative)
    parts = path.parts
    directories = parts[:-1]
    if set(directories) & IGNORED:
        return None
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._/-]*", path.as_posix()) or ".." in parts:
        raise ValueError(f"invalid capture name: {relative}")
    fields = path.stem.split("_")
    if len(fields) < 3 or fields[-2] not in {"en", "nl"} or fields[-1] not in {"light", "dark"}:
        raise ValueError(f"invalid capture name: {relative}")
    profile = fields[-3] if fields[-3] in PROFILES else "pixel8"
    if profile in {"pixel", "device"}:
        profile = "pixel8"
    return fields[0], f"{profile}_{fields[-2]}_{fields[-1]}"


def owners_from_plan(text, names):
    owners = {}
    phase = None
    for line in text.splitlines():
        heading = re.match(r"### Phase ([^:]+):", line)
        if heading:
            phase = heading[1]
        if line.startswith("- **Boards:**"):
            for board in re.findall(r"`([^`]+)`", line):
                if board in names:
                    owners[board] = phase
    return owners


def report(canvas, scope, captures, plan=""):
    if scope.get("schema") != 1:
        raise ValueError("unsupported board scope schema")
    names = board_names(canvas)
    excluded = {b: excluded_reason(b, scope) for b in names if excluded_reason(b, scope)}
    groups = {}
    owners = owners_from_plan(plan, names)
    for board in sorted(names - excluded.keys()):
        name = group_name(board, names)
        groups.setdefault(name, {"kind": "standard", "boards": [], "cells": {}, "exempt": {}})["boards"].append(board)
    for kind, entries in scope["kinds"].items():
        required_cells(kind)
        for name, reason in entries.items():
            if name not in groups or not isinstance(reason, str) or not reason.strip():
                raise ValueError(f"invalid {kind} group or reason: {name}")
            if groups[name]["kind"] != "standard":
                raise ValueError(f"multiple kinds for {name}")
            groups[name].update(kind=kind, reason=reason)
    for name, entry in scope["notBuilt"].items():
        if name not in groups or not entry.get("reason", "").strip() or not entry.get("phase"):
            raise ValueError(f"notBuilt needs a group, phase and reason: {name}")
        groups[name].update(kind="notBuilt", reason=entry["reason"], ownerPhase=entry["phase"])
    for name, exemptions in scope["cellExemptions"].items():
        if name not in groups:
            raise ValueError(f"unknown exemption group: {name}")
        for cell, reason in exemptions.items():
            if cell not in required_cells(groups[name]["kind"]) or not isinstance(reason, str) or not reason.strip():
                raise ValueError(f"exemption needs a required cell and non-empty reason: {name}/{cell}")
        groups[name]["exempt"] = exemptions
    invalid = []
    unknown = []
    for path in sorted(captures.rglob("*.png")) if captures.exists() else []:
        relative = path.relative_to(captures).as_posix()
        try:
            parsed = parse_capture(relative)
        except ValueError as error:
            invalid.append(str(error))
            continue
        if parsed is None:
            continue
        board, cell = parsed
        if board not in names:
            unknown.append(relative)
            continue
        if board in excluded:
            continue
        group = groups[group_name(board, names)]
        group["cells"].setdefault(cell, []).append(relative)
    for name, group in groups.items():
        group.setdefault("ownerPhase", owners.get(name) or next(
            (owners[b] for b in group["boards"] if b in owners), None))
        group["required"] = required_cells(group["kind"])
        group["missing"] = [c for c in group["required"] if c not in group["cells"] and c not in group["exempt"]]
    return {"counts": {"total": len(names), "excluded": len(excluded), "inScope": len(names) - len(excluded),
                       "groups": len(groups)}, "groups": groups, "excluded": excluded,
            "invalidNames": invalid, "unknownBoards": unknown}


def write_report(result, out):
    out.mkdir(parents=True, exist_ok=True)
    (out / "coverage.json").write_text(json.dumps(result, indent=2) + "\n")
    lines = ["# Board capture coverage", "", "Counts: " + json.dumps(result["counts"], sort_keys=True), "",
             "| Group | Kind | Owner phase | Covered/required cells | Missing cells |",
             "|---|---|---|---|---|"]
    for name, group in result["groups"].items():
        required = len(group["required"])
        covered = required - len(group["missing"])
        lines.append(f"| {name} | {group['kind']} | {group['ownerPhase'] or 'unowned'} | "
                     f"{covered}/{required} | {', '.join(group['missing']) or '—'} |")
    if result["invalidNames"]:
        lines += ["", "## Invalid capture names", ""] + result["invalidNames"]
    if result["unknownBoards"]:
        lines += ["", "## Non-board captures", ""] + result["unknownBoards"]
    (out / "coverage.md").write_text("\n".join(lines) + "\n")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--canvas", type=Path, default=Path("_design/canvas/canvas.json"))
    parser.add_argument("--scope", type=Path, default=Path("tools/audit/board-scope.json"))
    parser.add_argument("--captures", type=Path, default=Path("build/captures"))
    parser.add_argument("--out", type=Path, default=Path("build/audit"))
    parser.add_argument("--plan", type=Path, default=Path("plan.md"))
    args = parser.parse_args(argv)
    try:
        result = report(json.loads(args.canvas.read_text()), json.loads(args.scope.read_text()),
                        args.captures, args.plan.read_text())
        if result["counts"] != EXPECTED_COUNTS:
            print(f"board-coverage: canvas changed ({result['counts']})", file=sys.stderr)
            return 3
        write_report(result, args.out)
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"board-coverage: {error}", file=sys.stderr)
        return 2
    required = [g for g in result["groups"].values() if g["required"]]
    missing = sum(len(g["missing"]) for g in required)
    covered = sum(not g["missing"] for g in required)
    print(f"board-coverage: {covered}/{len(required)} groups covered, {missing} missing cells")
    for error in result["invalidNames"]:
        print(f"board-coverage: {error}", file=sys.stderr)
    return int(bool(missing or result["invalidNames"]))


if __name__ == "__main__":
    sys.exit(main())
