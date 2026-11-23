import json
import unittest
import zipfile
from pathlib import Path

from helpers import ROOT, SCRIPTS, run, temp_dir

FIXTURES = SCRIPTS / "fixtures/release-apk"


class CheckReleaseApkTest(unittest.TestCase):
    def setUp(self):
        self.root = temp_dir(self)
        self.apk = self.root / "app.apk"
        with zipfile.ZipFile(self.apk, "w") as apk:
            apk.writestr("classes.dex", b"fixture")
        self.mapping = self.root / "mapping.txt"
        self.mapping.write_text((FIXTURES / "mapping.txt").read_text())
        self.packages = (FIXTURES / "packages.txt").read_text()
        self.dex = (FIXTURES / "dexdump.txt").read_bytes()

    def tool(self, name, content):
        file = self.root / name
        file.write_text("#!/usr/bin/env python3\nimport sys\nsys.stdout.buffer.write(" + repr(content) + ")\n")
        file.chmod(0o755)
        return file

    def check(self, missing_dump=False):
        analyzer = self.tool("apkanalyzer", self.packages.encode())
        dump = self.root / "missing" if missing_dump else self.tool("dexdump", self.dex)
        return run(["python3", "-I", str(SCRIPTS / "check-release-apk.py"), "--apk", str(self.apk), "--mapping", str(self.mapping), "--apkanalyzer", str(analyzer), "--dexdump", str(dump), "--allowed-perf-stubs", str(ROOT / "app/src/releaseNoPerf/kotlin/net/chronoflow/app/perf")])

    def assert_clean(self):
        result = self.check()
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertIn("release apk: OK (1 dex files, 1 classes checked)", result.stdout)
        return result

    def test_clean(self):
        self.assertIn("INFO billing: absent", self.assert_clean().stdout)

    def assert_forbidden_package(self, name):
        self.packages += f"P d 1 1 20 {name}\n"
        result = self.check()
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn(name, result.stdout)

    def test_debug_package(self): self.assert_forbidden_package("net.chronoflow.debug")
    def test_feature_debug_package(self): self.assert_forbidden_package("net.chronoflow.feature.wizard.debug")
    def test_mock_package(self): self.assert_forbidden_package("net.chronoflow.core.datamock")
    def test_unapproved_perf_class(self): self.assert_forbidden_package("net.chronoflow.app.perf.PerfMockSources")
    def test_tooling_runtime(self): self.assert_forbidden_package("androidx.compose.ui.tooling.SomeTool")

    def test_preview_annotation_package_is_allowed(self):
        self.packages += "P d 1 1 20 androidx.compose.ui.tooling\nP d 1 1 20 androidx.compose.ui.tooling.preview\nC d 1 1 20 androidx.compose.ui.tooling.preview.Preview\n"
        self.assert_clean()

    def test_bare_tooling_class_is_forbidden(self):
        self.packages += "P d 1 1 20 androidx.compose.ui.tooling\nC d 1 1 20 androidx.compose.ui.tooling\n"
        result = self.check()
        self.assertEqual(result.returncode, 1, result.stderr + result.stdout)
        self.assertIn("FAIL class: androidx.compose.ui.tooling", result.stdout)

    def test_tooling_subpackage_is_forbidden(self):
        self.assert_forbidden_package("androidx.compose.ui.tooling.data")

    def test_only_declared_release_perf_stub_is_allowed(self):
        self.packages += "P d 1 1 20 net.chronoflow.app.perf\nC d 1 1 20 net.chronoflow.app.perf.TripPageEntry\n"
        self.assert_clean()

    def assert_seed(self, value):
        self.dex += f'000004: const-string/jumbo v1, {json.dumps(value)} // string@0002\n'.encode()
        result = self.check()
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("FAIL string: net.chronoflow.app.X", result.stdout)
        return result

    def test_auth_seed(self): self.assert_seed("CF_AUTH_TEST_ID")
    def test_unlisted_seed_uses_prefix(self): self.assert_seed("CF_GUEST_WIZARD")
    def test_perf_extra_string(self): self.assert_seed("cf.perf-wizard")
    def test_reminder_string(self): self.assert_seed("cf.notificationAuthorization")

    def test_modified_utf8_does_not_hide_ascii_markers(self):
        self.dex += b'000004: const-string v1, "nul\xc0\x80"\n'
        self.assert_seed("CF_ANOTHER_SEED")

    def test_billing_is_informational(self):
        self.packages += "C d 1 1 20 com.android.billingclient.api.BillingClient\n"
        self.assertIn("INFO billing: present (com.android.billingclient.api.BillingClient)", self.assert_clean().stdout)

    def assert_call(self, descriptor, call, expected):
        self.dex = f"Class descriptor : '{descriptor}'\n000000: invoke-static {{v0}}, {call}:(Ljava/lang/String;)I\n".encode()
        result = self.check()
        self.assertEqual(result.returncode, expected, result.stderr + result.stdout)
        return result

    def test_app_debug_log_call(self):
        self.assertIn("net.chronoflow.app.X", self.assert_call("La/b;", "Landroid/util/Log;.d", 1).stdout)

    def test_library_debug_log_call(self): self.assert_call("Lc/d;", "Landroid/util/Log;.d", 0)
    def test_app_warning_is_retained(self): self.assert_call("La/b;", "Landroid/util/Log;.w", 0)
    def test_exit_history_call_is_forbidden(self): self.assert_call("La/b;", "Landroid/app/ActivityManager;.getHistoricalProcessExitReasons", 1)

    def test_missing_tool_is_usage_error(self):
        result = self.check(missing_dump=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn("missing tool", result.stderr)

    def test_missing_dex_evidence_fails(self):
        self.dex = b""
        result = self.check()
        self.assertEqual(result.returncode, 1)
        self.assertIn("no class descriptors", result.stdout)
