import unittest

from helpers import SCRIPTS, run, temp_dir


class BumpVersionTest(unittest.TestCase):
    def setUp(self):
        self.path = temp_dir(self) / "version.properties"

    def write(self, values):
        self.path.write_text("".join(f"{key}={value}\n" for key, value in zip(
            ("versionMajor", "versionMinor", "versionPatch", "versionBuild"), values)))

    def cli(self, part, *args):
        return run(["python3", "-I", str(SCRIPTS / "bump-version.py"), part, "--file", str(self.path), *args])

    def assert_bump(self, part, old, expected):
        self.write(old)
        result = self.cli(part)
        self.assertEqual(result.returncode, 0, result.stderr)
        values = [int(line.split("=")[1]) for line in self.path.read_text().splitlines()]
        self.assertEqual(values, expected)
        # This is VersionProperties.parse's 0a rule, independent of the script's code().
        major, minor, patch, build = expected
        expected_code = major * 1_000_000 + minor * 10_000 + patch * 100 + build
        self.assertIn(f"-> {major}.{minor}.{patch} ({expected_code})", result.stdout)

    def test_build(self): self.assert_bump("build", [0, 1, 0, 0], [0, 1, 0, 1])
    def test_patch(self): self.assert_bump("patch", [0, 1, 0, 0], [0, 1, 1, 0])
    def test_minor(self): self.assert_bump("minor", [0, 1, 0, 0], [0, 2, 0, 0])
    def test_major(self): self.assert_bump("major", [0, 1, 0, 0], [1, 0, 0, 0])
    def test_patch_resets_build(self): self.assert_bump("patch", [1, 2, 3, 99], [1, 2, 4, 0])
    def test_minor_resets_patch_and_build(self): self.assert_bump("minor", [1, 2, 99, 99], [1, 3, 0, 0])
    def test_major_resets_all(self): self.assert_bump("major", [1, 99, 99, 99], [2, 0, 0, 0])

    def assert_limit(self, part, values, message):
        self.write(values)
        before = self.path.read_bytes()
        result = self.cli(part)
        self.assertEqual(result.returncode, 1)
        self.assertIn(message, result.stderr)
        self.assertEqual(self.path.read_bytes(), before)

    def test_build_limit(self): self.assert_limit("build", [0, 1, 0, 99], "versionBuild is 99: bump patch")
    def test_patch_limit(self): self.assert_limit("patch", [0, 1, 99, 0], "versionPatch is 99: bump minor")
    def test_minor_limit(self): self.assert_limit("minor", [0, 99, 0, 0], "versionMinor is 99: bump major")
    def test_major_limit(self): self.assert_limit("major", [2099, 0, 0, 0], "versionMajor is 2099")

    def test_missing_build(self):
        self.path.write_text("versionMajor=0\nversionMinor=1\nversionPatch=0\n")
        result = self.cli("build")
        self.assertEqual(result.returncode, 2)
        self.assertIn("versionBuild", result.stderr)

    def test_non_integer(self):
        self.write([0, "bad", 0, 0])
        result = self.cli("build")
        self.assertEqual(result.returncode, 2)
        self.assertIn("versionMinor", result.stderr)

    def test_out_of_range(self):
        self.write([0, 100, 0, 0])
        self.assertEqual(self.cli("build").returncode, 2)

    def test_dry_run_preserves_bytes(self):
        self.write([0, 1, 0, 0])
        before = self.path.read_bytes()
        result = self.cli("build", "--dry-run")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), "version: 0.1.0 (10000) -> 0.1.0 (10001)")
        self.assertEqual(self.path.read_bytes(), before)

    def test_empty_part_is_usage_error(self):
        self.assertEqual(self.cli("").returncode, 2)
