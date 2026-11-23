#!/usr/bin/env python3
"""Extracts the iOS mock seed JSON literal into :core:testing and keeps its line in fixtures.sha256.

Usage: python3 -I tools/fixtures/extract_mock_seed.py [--ios-repo <path>] [--pin <sha>] [--check]
Only `git show` touches the iOS repository.
"""
import argparse
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESOURCE = "mock-seed.json"
FIXTURES = "core/testing/src/main/resources/fixtures"
SOURCE = "Packages/ChronoFlowKit/Sources/DataAccess/Mock/MockAppSeed+Fixtures.swift"


def git_show(repo, pin, path):
    return subprocess.check_output(["git", "-C", str(repo), "show", f"{pin}:{path}"]).decode("utf-8")


def extract(swift):
    """The lines strictly between the `#\"\"\"` opener and the `\"\"\"#` closer, minus the closer's indentation."""
    lines = swift.split("\n")
    opener = next(i for i, line in enumerate(lines) if line.rstrip().endswith('#"""'))
    closer = next(i for i in range(opener + 1, len(lines)) if lines[i].lstrip().startswith('"""#'))
    indent = lines[closer][: len(lines[closer]) - len(lines[closer].lstrip())]
    body = []
    for line in lines[opener + 1 : closer]:
        body.append(line[len(indent) :] if line.startswith(indent) else line)
    text = "\n".join(body) + "\n"
    json.loads(text)
    return text.encode("utf-8")


def manifest_line(data):
    return f"{hashlib.sha256(data).hexdigest()}  {RESOURCE}  {SOURCE}"


def main(argv=None, read=git_show, root=ROOT):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ios-repo", default=os.environ.get("IOS_REPO"))
    parser.add_argument("--pin")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if not args.ios_repo:
        print("set IOS_REPO or pass --ios-repo", file=sys.stderr)
        return 2
    root = Path(root)
    pin = args.pin or (root / "tools/ios-pin.txt").read_text().split()[0]
    data = extract(read(args.ios_repo, pin, SOURCE))
    out = root / FIXTURES / RESOURCE
    manifest = root / FIXTURES / "fixtures.sha256"
    line = manifest_line(data)
    lines = manifest.read_text().split("\n")
    present = [i for i, existing in enumerate(lines) if existing.split("  ")[1:2] == [RESOURCE]]
    if args.check:
        problems = []
        if not out.exists() or out.read_bytes() != data:
            problems.append(f"differs {FIXTURES}/{RESOURCE}")
        if not present or lines[present[0]] != line:
            problems.append(f"manifest line differs for {RESOURCE}")
        if problems:
            print("\n".join(problems), file=sys.stderr)
            return 1
        print(f"Mock seed matches iOS {pin[:8]}.")
        return 0
    out.write_bytes(data)
    body = [existing for existing in lines if existing and not existing.startswith("#")]
    body = [existing for existing in body if existing.split("  ")[1] != RESOURCE] + [line]
    header = [existing for existing in lines if existing.startswith("#")]
    manifest.write_text("\n".join(header + sorted(body, key=lambda entry: entry.split("  ")[1])) + "\n")
    print(f"Wrote {RESOURCE} from iOS {pin[:8]}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
