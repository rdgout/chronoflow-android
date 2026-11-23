import re
import shutil
import unittest

from helpers import ROOT, run, temp_dir

DEVICE_MARKERS = re.compile(r"connected\w*AndroidTest|androidTest-results/connected|am instrument")


def recipes(text):
    joined = text.replace("\\\n", " ")
    return [line for line in joined.splitlines() if line.startswith("\t") or re.match(r"^\w[\w-]*:", line)]


class MakefileTest(unittest.TestCase):
    def test_today_title_checks_the_fresh_archived_metrics(self):
        result = run(['make', '-n', 'perf-today-title'], cwd=ROOT)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('Scripts/check-benchmarks.py --suite today-title --device-class unknown --results-dir "$RESULTS_DIR"', result.stdout)
        self.assertLess(result.stdout.index('finish_perf_archive;'), result.stdout.index('Scripts/check-benchmarks.py'))
        self.assertIn('|| status=$?;', result.stdout)

    def test_permission_dialog_is_excluded_then_runs_alone_with_apks_kept(self):
        text = (ROOT / "Makefile").read_text()
        self.assertIn("notClass=net.chronoflow.app.reminders.RemindersPermissionDeviceTest", text)
        self.assertIn("leaveApksInstalledAfterRun=true", text)
        self.assertIn("reminders-permission --device --emulator", text)

    def test_strictmode_sequence_is_locked_serial_and_dry_run_safe(self):
        recipe = (ROOT / "Makefile").read_text().split("strictmode-run:\n", 1)[1]
        self.assertIn("device_lock.py", recipe)
        self.assertIn("--device --emulator", recipe)
        self.assertIn("--max-workers=1 --continue", recipe)
        self.assertIn("logcat -G 16M", recipe)
        self.assertIn("Scripts/ios-parity.py", recipe)
        self.assertNotIn("$(MAKE)", recipe)
        self.assertLess(recipe.index("Scripts/ios-parity.py"), recipe.index("logcat -d -v threadtime"))
        self.assertLess(recipe.index("logcat -d -v threadtime"), recipe.index("Scripts/strictmode-scan.py"))

    def test_strictmode_collects_only_the_debug_modules_it_runs(self):
        result = run(["make", "-n", "strictmode-run"], cwd=ROOT)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('--results "app/build/outputs/androidTest-results/connected/debug/**/*.xml"', result.stdout)
        self.assertIn('--results "core/ui/build/outputs/androidTest-results/connected/debug/**/*.xml"', result.stdout)
        self.assertNotIn('benchmark/build/', result.stdout)
        self.assertNotIn('--results "*/build/', result.stdout)

    def test_audit_suites_include_the_isolated_permission_case_without_annotation_filters(self):
        text = (ROOT / "Makefile").read_text()
        audit = text.split("audit-suite:\n", 1)[1].split("\n.PHONY:", 1)[0]
        strict = text.split("strictmode-run:\n", 1)[1]
        for recipe in (audit, strict):
            self.assertIn("notClass=net.chronoflow.app.reminders.RemindersPermissionDeviceTest", recipe)
            self.assertIn("Scripts/reminders_permission.py", recipe)
            self.assertIn("leaveApksInstalledAfterRun=true", recipe)
            self.assertNotIn("RunnerArguments.annotation=", recipe)
        self.assertIn("Scripts/reminders_permission.py --audit", audit)

    def test_matrix_is_locked_and_supports_only_one_failed_avd(self):
        result = run(["make", "-n", "matrix", "ONLY=small"], cwd=ROOT)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("device_lock.py -- bash -ec", result.stdout)
        self.assertEqual(2, result.stdout.count("for avd in small;"))
        self.assertIn("--device --emulator", result.stdout)
        self.assertNotIn("for avd in fold", result.stdout)
        syntax = run(["bash", "-n", "-c", result.stdout])
        self.assertEqual(0, syntax.returncode, syntax.stderr)

    def test_matrix_stops_the_selected_avd_before_returning_a_failure(self):
        result = run(['make', '-n', 'matrix', 'ONLY=small'], cwd=ROOT)
        self.assertEqual(0, result.returncode, result.stderr)
        stop = 'Scripts/emulator.sh down "$avd"'
        failed = 'if [ "$status" != 0 ]; then exit "$status"; fi;'
        self.assertLess(result.stdout.index(stop), result.stdout.index(failed))

    def test_matrix_boot_failure_skips_tests_and_still_stops_the_avd(self):
        root = temp_dir(self)
        shutil.copyfile(ROOT / 'Makefile', root / 'Makefile')
        trace = root / 'calls.txt'
        stubs = {
            'Scripts/device_lock.py': 'import subprocess, sys\nraise SystemExit(subprocess.call(sys.argv[2:]))\n',
            'Scripts/emulator.sh': '#!/bin/sh\nprintf "%s\\n" "$*" >> "$CF_MATRIX_TEST_TRACE"\n'
                                   'if [ "$1" = ensure ]; then exit 7; fi\n',
            'Scripts/run-tier.sh': '#!/bin/sh\necho smoke >> "$CF_MATRIX_TEST_TRACE"\n',
            'Scripts/matrix-result.py': 'import os\nfrom pathlib import Path\n'
                                       'with Path(os.environ["CF_MATRIX_TEST_TRACE"]).open("a") as f: f.write("result\\n")\n',
            'sdk/cmdline-tools/latest/bin/avdmanager': '#!/bin/sh\necho small\n',
        }
        for name, body in stubs.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            path.chmod(0o755)
        result = run(['make', 'matrix', 'ONLY=small', 'ANDROID_HOME=' + str(root / 'sdk'),
                      'JAVA_HOME=/unused-matrix-fixture'], cwd=root,
                     env={'CF_MATRIX_TEST_TRACE': str(trace)})
        self.assertEqual(2, result.returncode, result.stdout + result.stderr)
        self.assertIn('Error 7', result.stderr)
        self.assertEqual(['ensure small', 'down small'], trace.read_text().splitlines())

    def test_matrix_rejects_pixel_before_any_device_command(self):
        result = run(["make", "-n", "matrix", "ONLY=pixel"], cwd=ROOT)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("ONLY must name fold, small or api29", result.stderr)
        self.assertNotIn("device_lock.py", result.stdout)

    def test_device_recipes_take_the_lock(self):
        text = (ROOT / "Makefile").read_text()
        offenders = [
            line.strip()
            for line in recipes(text)
            if DEVICE_MARKERS.search(line) and "--device" not in line and "device_lock.py" not in line
        ]
        self.assertEqual(offenders, [])


if __name__ == "__main__":
    unittest.main()
