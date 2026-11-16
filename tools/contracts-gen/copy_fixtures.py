#!/usr/bin/env python3
"""Copies the iOS API fixtures byte for byte into :core:testing and keeps their SHA-256 manifest.

Usage: python3 -I tools/contracts-gen/copy_fixtures.py [--ios-repo <path>] [--pin <sha>] [--check]
Only `git ls-tree` and `git show` touch the iOS repository.
"""
import argparse
import hashlib
import os
import posixpath
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = "core/testing/src/main/resources/fixtures"
MANIFEST = "fixtures.sha256"
IOS = "Packages/ChronoFlowKit/Tests/DataAccessTests/Fixtures"
SOURCES = [(IOS, ["account", "auth", "errors", "plans", "trips", "wizard"])]


def read(repo, pin, args):
    """The only function that runs git: ("ls-tree", dir) lists *.json names, ("show", path) returns the bytes."""
    kind, path = args
    if kind == "ls-tree":
        out = subprocess.check_output(["git", "-C", str(repo), "ls-tree", "-r", "--name-only", pin, path + "/"])
        return out
    return subprocess.check_output(["git", "-C", str(repo), "show", f"{pin}:{path}"])


def collect(repo, pin, reader):
    """Returns {resource: (bytes, ios_source)} and the owned sub-directories."""
    files, owned_dirs = {}, []
    for base, entries in SOURCES:
        for entry in entries:
            if entry.endswith(".json"):
                source = f"{base}/{entry}"
                files[entry] = (reader(repo, pin, ("show", source)), source)
                continue
            owned_dirs.append(entry)
            listing = reader(repo, pin, ("ls-tree", f"{base}/{entry}")).decode().splitlines()
            for source in sorted(listing):
                if source.endswith(".json"):
                    resource = f"{entry}/{posixpath.basename(source)}"
                    files[resource] = (reader(repo, pin, ("show", source)), source)
    return files, owned_dirs


def manifest_line(data, resource, source):
    return f"{hashlib.sha256(data).hexdigest()}  {resource}  {source}"


def parse_manifest(text):
    lines = {}
    for line in text.splitlines():
        if line and not line.startswith("#"):
            lines[line.split("  ")[1]] = line
    return lines


def is_owned(resource, owned, owned_dirs):
    return resource in owned or resource.split("/")[0] in owned_dirs and "/" in resource


def main(argv=None, read=read, root=ROOT):
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
    files, owned_dirs = collect(args.ios_repo, pin, read)
    out = root / FIXTURES
    manifest = out / MANIFEST
    existing = parse_manifest(manifest.read_text()) if manifest.exists() else {}
    own_lines = {r: manifest_line(d, r, s) for r, (d, s) in files.items()}
    if args.check:
        problems = []
        for resource, (data, _) in sorted(files.items()):
            path = out / resource
            if not path.exists():
                problems.append(f"missing {FIXTURES}/{resource}")
            elif path.read_bytes() != data:
                problems.append(f"differs {FIXTURES}/{resource}")
            if existing.get(resource) != own_lines[resource]:
                problems.append(f"manifest line differs for {resource}")
        for sub in owned_dirs:
            for path in sorted((out / sub).glob("*.json")) if (out / sub).is_dir() else []:
                if f"{sub}/{path.name}" not in files:
                    problems.append(f"not from iOS {FIXTURES}/{sub}/{path.name}")
        if problems:
            print("\n".join(problems), file=sys.stderr)
            return 1
        print(f"Fixtures match iOS {pin[:8]}: {len(files)} files.")
        return 0
    for resource, (data, _) in files.items():
        path = out / resource
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    foreign = {r: line for r, line in existing.items() if not is_owned(r, files, owned_dirs)}
    lines = {**foreign, **own_lines}
    text = f"# ios-pin {pin[:8]}\n# sha256  resource  ios-source\n" + "".join(f"{lines[r]}\n" for r in sorted(lines))
    manifest.write_text(text)
    print(f"Copied {len(files)} fixtures from iOS {pin[:8]}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
