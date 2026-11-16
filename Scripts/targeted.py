#!/usr/bin/env python3
"""Runs only the named tests: targeted.py <module>:<Class>[#method] ... [--print]."""
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path

ROOT = Path(os.environ.get("TARGETED_ROOT") or Path(__file__).resolve().parent.parent)
JVM_MODULES = {
    "core:model", "core:plan", "core:sync", "core:reminders", "core:wizard", "core:network",
    "core:data-mock", "core:testing", "konsist", "tools:tokengen", "build-logic:convention",
}
GRADLE = ["./gradlew", "--console=plain"]
APP_TEST_RUNNER = "net.chronoflow.app.native.debug.test/androidx.test.runner.AndroidJUnitRunner"


def fail(message):
    print("targeted: " + message, file=sys.stderr)
    raise SystemExit(2)


def find_class(module_dir, name, source_set=None):
    base = module_dir / "src"
    pattern = "%s/**/%s.kt" % (source_set, name) if source_set else "**/%s.kt" % name
    return next(iter(sorted(base.glob(pattern))), None)


def package_of(path):
    match = re.search(r"^package\s+([\w.]+)", path.read_text(), re.MULTILINE)
    return match.group(1) if match else ""


def parse(selector):
    if ":" not in selector:
        fail("selector '%s' must look like <module>:<Class>[#method]" % selector)
    module, _, rest = selector.rpartition(":")
    cls, _, method = rest.partition("#")
    module_dir = ROOT / module.replace(":", "/")
    if not (module_dir / "build.gradle.kts").exists():
        fail("unknown module '%s'" % module)
    return module, module_dir, cls, method


def plan(selectors):
    groups = {}
    for selector in selectors:
        module, module_dir, cls, method = parse(selector)
        instrumented = find_class(module_dir, cls, "androidTest")
        if instrumented:
            fqcn = package_of(instrumented) + "." + cls
            key = (module, "instrumented")
            groups.setdefault(key, []).append(fqcn + ("#" + method if method else ""))
        elif find_class(module_dir, cls):
            if module in JVM_MODULES:
                kind = "jvm"
            elif find_class(module_dir, cls, "testRelease"):
                kind = "android-release"
            else:
                kind = "android"
            key = (module, kind)
            groups.setdefault(key, []).append("*.%s%s" % (cls, "." + method if method else ""))
        else:
            fail("class '%s' not found in %s" % (cls, module))
    commands = []
    for (module, kind), items in groups.items():
        gradle_path = ":" + module
        if kind == "instrumented":
            if os.environ.get("REUSE_BUILD") == "1":
                adb = os.environ.get("ADB") or os.path.join(
                    os.environ.get("ANDROID_HOME", "/opt/homebrew/share/android-commandlinetools"), "platform-tools", "adb"
                )
                serial = os.environ.get("ANDROID_SERIAL", "emulator-5554")
                commands.append(
                    ["python3", "-I", "Scripts/device_lock.py", "--", adb, "-s", serial, "shell", "am", "instrument",
                     "-w", "-r", "-e", "class", ",".join(items), "-e", "timeout_msec", "60000", APP_TEST_RUNNER]
                )
            else:
                results = "%s/build/outputs/androidTest-results/connected/**/*.xml" % module.replace(":", "/")
                commands.append(
                    ["Scripts/run-tier.sh", "targeted-emu", "--device", "--results", results, "--"]
                    + GRADLE
                    + [gradle_path + ":connectedDebugAndroidTest",
                       "-Pandroid.testInstrumentationRunnerArguments.class=" + ",".join(items)]
                )
        else:
            task = {"jvm": ":test", "android-release": ":testReleaseUnitTest"}.get(kind, ":testDebugUnitTest")
            results = "%s/build/test-results/**/*.xml" % module.replace(":", "/")
            tests = []
            for item in items:
                tests += ["--tests", item]
            commands.append(
                ["Scripts/run-tier.sh", "targeted", "--results", results, "--"] + GRADLE + [gradle_path + task] + tests
            )
    return commands


def main(argv):
    print_only = "--print" in argv
    selectors = [a for a in argv if a != "--print"]
    if not selectors:
        fail("usage: make targeted ONLY=\"<module>:<Class>[#method] ...\"")
    commands = plan(selectors)
    if print_only:
        for command in commands:
            print(" ".join(shlex.quote(part) for part in command))
        return 0
    for command in commands:
        code = subprocess.call(command, cwd=str(ROOT))
        if code != 0:
            return code
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
