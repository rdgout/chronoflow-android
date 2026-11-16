import hashlib
import os
import sys
import unittest

from helpers import SCRIPTS, ROOT, run, temp_dir

CHECK = str(SCRIPTS / "check-sha256-manifest.py")


class CheckSha256ManifestTest(unittest.TestCase):
    def setUp(self):
        self.dir = temp_dir(self)
        self.payload = self.dir / "payload.txt"
        self.payload.write_bytes(b"hello")
        self.digest = hashlib.sha256(b"hello").hexdigest()

    def manifest(self, *lines):
        path = self.dir / "manifest.sha256"
        path.write_text("\n".join(lines) + "\n")
        return path

    def check(self, manifest):
        return run([sys.executable, "-I", CHECK, str(manifest)])

    def local(self):
        # Manifest paths are relative to the repo root.
        return os.path.relpath(self.payload, ROOT)

    def test_clean_manifest_passes(self):
        result = self.check(self.manifest("# ios-pin abc", f"{self.digest}  -  {self.local()}"))
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_flipped_byte_fails_and_names_the_path(self):
        manifest = self.manifest(f"{self.digest}  -  {self.local()}")
        self.payload.write_bytes(b"hellp")
        result = self.check(manifest)
        self.assertEqual(result.returncode, 1)
        self.assertIn(self.local(), result.stderr)

    def test_missing_file_fails(self):
        manifest = self.manifest(f"{self.digest}  -  {self.local()}")
        self.payload.unlink()
        result = self.check(manifest)
        self.assertEqual(result.returncode, 1)
        self.assertIn("missing", result.stderr)

    def test_comments_and_dash_entries_are_ignored(self):
        manifest = self.manifest("# a comment", f"{self.digest}  some/ios/path  -")
        self.assertEqual(self.check(manifest).returncode, 0)


if __name__ == "__main__":
    unittest.main()
