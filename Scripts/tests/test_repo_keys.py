import re
import subprocess
import unittest

from helpers import ROOT


def unexpected_keys(paths):
    return [p for p in paths if p != "app/debug.keystore"]


def upload_assignments(text):
    return re.findall(r"^[ \t]*CF_UPLOAD_[A-Z_]+=[^\s].*$", text, re.MULTILINE)


class RepoKeysTest(unittest.TestCase):
    def test_only_debug_key_is_tracked(self):
        paths = subprocess.check_output(["git", "ls-files", "--", "*.jks", "*.keystore", "*.p12", "*.pk8", "*.pem", "*.der"], cwd=ROOT, text=True).splitlines()
        self.assertEqual(unexpected_keys(paths), [])
        self.assertEqual(paths, ["app/debug.keystore"])

    def test_planted_upload_key_is_rejected(self):
        self.assertEqual(unexpected_keys(["app/debug.keystore", "upload.p12"]), ["upload.p12"])

    def test_no_tracked_property_contains_upload_credentials(self):
        result = subprocess.run(["git", "grep", "-n", "-E", r"^[[:space:]]*CF_UPLOAD_[A-Z_]+=[^[:space:]]", "--", "*.properties"], cwd=ROOT, capture_output=True, text=True)
        self.assertIn(result.returncode, (0, 1), result.stderr)
        self.assertEqual(result.stdout, "")

    def test_assignment_matcher_rejects_a_value(self):
        self.assertEqual(upload_assignments("CF_UPLOAD_STORE_PASSWORD=secret"), ["CF_UPLOAD_STORE_PASSWORD=secret"])
        self.assertEqual(upload_assignments("CF_UPLOAD_STORE_PASSWORD="), [])
