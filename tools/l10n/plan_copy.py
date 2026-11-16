#!/usr/bin/env python3
"""Copies the iOS runtime plan-copy table byte for byte and records its SHA-256.

Usage: python3 -I tools/l10n/plan_copy.py [--ios <path>] [--pin <sha>] [--check] [--root <dir>]
"""
import argparse
import hashlib
import os
import pathlib
import subprocess
import sys

REPO = pathlib.Path(__file__).resolve().parents[2]
DEFAULT_IOS = "/Users/rickgout/Sites/chronoflow-ios"
SOURCE = "Packages/ChronoFlowKit/Sources/PlanCore/Resources/PlanCopy.json"
TARGET = "core/plan/src/main/resources/plan_copy.json"
HASH_FILE = "tools/l10n/plan_copy.sha256"


class PinNotFound(Exception):
    pass


def read_plan_copy(ios, pin):
    def git(*args):
        return subprocess.run(["git", "-C", str(ios), *args], capture_output=True)

    if git("cat-file", "-e", pin + "^{commit}").returncode != 0:
        raise PinNotFound(f"iOS pin {pin} not found in {ios}")
    shown = git("show", f"{pin}:{SOURCE}")
    if shown.returncode != 0:
        raise SystemExit(f"plan_copy: cannot read {SOURCE} at {pin[:8]}")
    return shown.stdout


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def main(argv=None, read=read_plan_copy):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--ios", default=os.environ.get("IOS_REPO", DEFAULT_IOS))
    ap.add_argument("--pin")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--root", help="output root (tests only)")
    a = ap.parse_args(argv)
    root = pathlib.Path(a.root) if a.root else REPO
    pin = a.pin or (REPO / "tools/ios-pin.txt").read_text(encoding="utf-8").strip()
    try:
        source = read(a.ios, pin)
    except PinNotFound as e:
        print(str(e), file=sys.stderr)
        return 2
    target = root / TARGET
    hash_file = root / HASH_FILE
    if not a.check:
        target.parent.mkdir(parents=True, exist_ok=True)
        with open(target, "wb") as f:
            f.write(source)
        hash_file.parent.mkdir(parents=True, exist_ok=True)
        with open(hash_file, "w", encoding="utf-8", newline="\n") as f:
            f.write(f"{sha256(source)}  {TARGET}\n")
        print(f"{TARGET}: {len(source)} bytes, sha256 {sha256(source)}")
        return 0
    committed = target.read_bytes() if target.exists() else b""
    recorded = hash_file.read_text(encoding="utf-8").split()[0] if hash_file.exists() else ""
    failed = []
    if sha256(committed) != recorded:
        failed.append(f"committed {TARGET} ({len(committed)} bytes) hashes to {sha256(committed)}, recorded {recorded or 'nothing'}")
    if sha256(source) != recorded:
        failed.append(f"iOS {SOURCE} at {pin[:8]} hashes to {sha256(source)}, recorded {recorded or 'nothing'}")
    for line in failed:
        print("plan_copy: " + line, file=sys.stderr)
    if failed:
        print("run make plan-copy", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
