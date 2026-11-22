#!/usr/bin/env python3
"""Inspect defined release dex code using R8 names, not obfuscated package guesses."""
import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

PREFIXES = (
    "net.chronoflow.debug.", "net.chronoflow.core.datamock.", "net.chronoflow.core.testing",
    "leakcanary.", "androidx.metrics.performance.", "androidx.test.", "org.junit.",
    "org.robolectric.", "mockwebserver3.", "okhttp3.mockwebserver.",
)
STRINGS = (
    "SeedReceiverActivity", "demo@example.com", "twofactor@example.com", "AbCdEf1234-GhIjKl5678",
    "cf.trip-page", "cf.timeline-fixtures", "cf.perf-account", "cf.perf-wizard", "uitests/",
    "cf.notificationAuthorization", "cf.notificationPermissionRequested", "cf.firstTripReminders",
    "cf.passkeyCapability", "cf.holdSplash",
)
CALLS = (
    "Landroid/util/Log;.v:", "Landroid/util/Log;.d:", "Landroid/util/Log;.i:",
    "Landroid/app/ActivityManager;.getHistoricalProcessExitReasons:",
)
DEBUG_PACKAGE = re.compile(r"^net\.chronoflow\.(?:\w+\.)*debug(?:\.|$)")
CLASS_LINE = re.compile(r"^([^\s#].*?) -> ([^\s]+):$")
DESCRIPTOR = re.compile(r"Class descriptor\s*:\s*'L([^;]+);'")
PACKAGE_LINE = re.compile(r"^([PC])\s+\w\s+\d+\s+\d+\s+\d+\s+(\S+)")
STRING = re.compile(r'const-string(?:/jumbo)?\b.*?("(?:[^"\\]|\\.)*")')
ROOT = Path(__file__).resolve().parent.parent


def mapping_names(text):
    return {match[2]: match[1] for line in text.splitlines() if (match := CLASS_LINE.match(line))}


def allowed_stubs(directory):
    names = set()
    for file in directory.glob("*.kt"):
        text = file.read_text()
        package = re.search(r"^package\s+(\S+)", text, re.MULTILINE)
        if not package:
            raise ValueError(f"stub has no package: {file.name}")
        for name in re.findall(r"^(?:(?:internal|public|private|data|sealed)\s+)*(?:object|class|interface)\s+(\w+)", text, re.MULTILINE):
            names.add(f"{package[1]}.{name}")
    if not names:
        raise ValueError("allowed perf stub directory contains no declarations")
    return names


def forbidden_class(name, stubs, package=False):
    if name == "androidx.compose.ui.tooling.preview" or name.startswith("androidx.compose.ui.tooling.preview."):
        return False
    if name == "net.chronoflow.app.perf" and package:
        return False
    if name.startswith("net.chronoflow.app.perf."):
        return name not in stubs
    return bool(DEBUG_PACKAGE.match(name)) or any(
        name == prefix.rstrip(".") or name.startswith(prefix) for prefix in PREFIXES + ("androidx.compose.ui.tooling.",)
    )


def inspect_packages(text, names, stubs):
    findings, classes = [], set()
    for line in text.splitlines():
        match = PACKAGE_LINE.match(line.strip())
        if not match:
            continue
        kind, name = match.groups()
        name = names.get(name, name)
        if kind == "C":
            classes.add(name)
        if forbidden_class(name, stubs, package=kind == "P"):
            findings.append(f"FAIL class: {name}")
    return findings, classes


def inspect_dex(lines, names, stubs):
    findings, classes = [], set()
    owner = "<unknown>"
    for line in lines:
        descriptor = DESCRIPTOR.search(line)
        if descriptor:
            raw = descriptor[1].replace("/", ".")
            owner = names.get(raw, raw)
            classes.add(owner)
            if forbidden_class(owner, stubs):
                findings.append(f"FAIL class: {owner}")
        if match := STRING.search(line):
            try:
                value = json.loads(match[1])
            except json.JSONDecodeError:
                value = match[1][1:-1]
            marker = "CF_ prefix" if value.startswith("CF_") else next((s for s in STRINGS if s in value), None)
            if marker:
                findings.append(f"FAIL string: {owner}")
        if "invoke-" in line and owner.startswith("net.chronoflow."):
            for call in CALLS:
                if call in line:
                    findings.append(f"FAIL call: {owner} {call}")
    return findings, classes


def executable(path):
    if not path.is_file() or not os.access(path, os.X_OK):
        raise ValueError(f"missing tool: {path}")
    return str(path)


def main():
    home = Path(os.environ.get("ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools"))
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--mapping", required=True, type=Path)
    parser.add_argument("--apkanalyzer", type=Path, default=home / "cmdline-tools/latest/bin/apkanalyzer")
    parser.add_argument("--dexdump", type=Path, default=home / "build-tools/36.0.0/dexdump")
    parser.add_argument("--allowed-perf-stubs", type=Path, default=ROOT / "app/src/releaseNoPerf/kotlin/net/chronoflow/app/perf")
    args = parser.parse_args()
    try:
        analyzer, dump = executable(args.apkanalyzer), executable(args.dexdump)
        stubs = allowed_stubs(args.allowed_perf_stubs)
        names = mapping_names(args.mapping.read_text())
        packages = subprocess.run([analyzer, "dex", "packages", "--defined-only", "--proguard-mappings", str(args.mapping), str(args.apk)], check=True, capture_output=True, text=True)
        findings, package_classes = inspect_packages(packages.stdout, names, stubs)
        dex_classes = set()
        with tempfile.TemporaryDirectory(prefix="cf-release-apk-") as root, zipfile.ZipFile(args.apk) as apk:
            dex_names = sorted(name for name in apk.namelist() if re.fullmatch(r"classes(?:\d+)?\.dex", name))
            if not dex_names:
                findings.append("FAIL evidence: no dex files in APK")
            for name in dex_names:
                file = Path(root) / name
                file.write_bytes(apk.read(name))
                output = Path(root) / f"{name}.txt"
                with output.open("wb") as stream:
                    subprocess.run([dump, "-d", str(file)], stdout=stream, stderr=subprocess.PIPE, check=True)
                # dexdump emits modified UTF-8 strings, including overlong NUL; preserve bytes without losing ASCII markers.
                with output.open(encoding="utf-8", errors="backslashreplace") as stream:
                    found, classes = inspect_dex(stream, names, stubs)
                if not classes:
                    findings.append(f"FAIL evidence: no class descriptors in {name}")
                findings.extend(found)
                dex_classes.update(classes)
        classes = package_classes | dex_classes
        billing = next((name for name in sorted(classes) if name.startswith("com.android.billingclient.")), None)
        print(f"INFO billing: present ({billing})" if billing else "INFO billing: absent")
        for finding in dict.fromkeys(findings):
            print(finding)
        if findings:
            return 1
        print(f"release apk: OK ({len(dex_names)} dex files, {len(dex_classes)} classes checked)")
        return 0
    except (OSError, ValueError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        print(f"release apk: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
