import contextlib
import hashlib
import io
import tempfile
import unittest
from pathlib import Path

import copy_fixtures as cf

PIN = "f391a1fa" + "0" * 32
BASE = cf.IOS
TREE = {
    f"{BASE}/wizard/b.json": b'{"k":"\xc3\xa9"}\r\n',
    f"{BASE}/wizard/a.json": b"{}\n",
    f"{BASE}/wizard/README.md": b"ignored",
    f"{BASE}/errors/x.json": b"[]",
}


def fake_read(tree):
    def read(repo, pin, args):
        kind, path = args
        if kind == "ls-tree":
            return "".join(f"{p}\n" for p in tree if p.startswith(path + "/")).encode()
        return tree[path]

    return read


def run(root, *extra, tree=TREE):
    out, err = io.StringIO(), io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        code = cf.main(["--ios-repo", "x", "--pin", PIN, *extra], read=fake_read(tree), root=root)
    return code, out.getvalue(), err.getvalue()


def small_sources():
    return [(BASE, ["errors", "wizard"])]


class CopyFixturesTest(unittest.TestCase):
    def setUp(self):
        self.saved = cf.SOURCES
        cf.SOURCES = small_sources()
        self.addCleanup(setattr, cf, "SOURCES", self.saved)
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = tmp.name
        self.out = Path(self.root) / cf.FIXTURES

    def test_manifest_format_and_sort_order(self):
        self.assertEqual(run(self.root)[0], 0)
        sha = lambda d: hashlib.sha256(d).hexdigest()
        expected = (
            "# ios-pin f391a1fa\n# sha256  resource  ios-source\n"
            f"{sha(TREE[f'{BASE}/errors/x.json'])}  errors/x.json  {BASE}/errors/x.json\n"
            f"{sha(TREE[f'{BASE}/wizard/a.json'])}  wizard/a.json  {BASE}/wizard/a.json\n"
            f"{sha(TREE[f'{BASE}/wizard/b.json'])}  wizard/b.json  {BASE}/wizard/b.json\n"
        )
        self.assertEqual((self.out / cf.MANIFEST).read_text(), expected)
        self.assertFalse((self.out / "wizard/README.md").exists())

    def test_bytes_are_written_unchanged(self):
        run(self.root)
        self.assertEqual((self.out / "wizard/b.json").read_bytes(), TREE[f"{BASE}/wizard/b.json"])

    def test_check_passes_after_write(self):
        run(self.root)
        code, out, _ = run(self.root, "--check")
        self.assertEqual(code, 0)
        self.assertEqual(out, "Fixtures match iOS f391a1fa: 3 files.\n")

    def test_check_fails_on_changed_byte(self):
        run(self.root)
        (self.out / "wizard/a.json").write_bytes(b"{ }\n")
        code, _, err = run(self.root, "--check")
        self.assertEqual(code, 1)
        self.assertIn("wizard/a.json", err)

    def test_check_fails_on_missing_file(self):
        run(self.root)
        (self.out / "errors/x.json").unlink()
        code, _, err = run(self.root, "--check")
        self.assertEqual(code, 1)
        self.assertIn("missing", err)

    def test_check_fails_on_extra_json_under_owned_directory(self):
        run(self.root)
        (self.out / "wizard/extra.json").write_bytes(b"{}")
        code, _, err = run(self.root, "--check")
        self.assertEqual(code, 1)
        self.assertIn("wizard/extra.json", err)

    def test_foreign_lines_and_files_survive_and_do_not_fail_check(self):
        run(self.root)
        foreign = "a" * 64 + "  mock-seed.json  tools/x\n"
        manifest = self.out / cf.MANIFEST
        manifest.write_text(manifest.read_text() + foreign)
        (self.out / "mock-seed.json").write_bytes(b"{}")
        self.assertEqual(run(self.root)[0], 0)
        self.assertIn(foreign, manifest.read_text())
        self.assertTrue((self.out / "mock-seed.json").exists())
        self.assertEqual(run(self.root, "--check")[0], 0)

    def test_root_file_entry_is_written_to_the_fixtures_root(self):
        cf.SOURCES = [("Other/Fixtures", ["intl-cases.json"])]
        tree = {"Other/Fixtures/intl-cases.json": b"[1]"}
        self.assertEqual(run(self.root, tree=tree)[0], 0)
        self.assertEqual((self.out / "intl-cases.json").read_bytes(), b"[1]")
        self.assertIn("  intl-cases.json  Other/Fixtures/intl-cases.json\n", (self.out / cf.MANIFEST).read_text())

    def test_row_with_destination_subdirectory(self):
        cf.SOURCES = [("X/Fixtures", ["selection.json"], "reminders")]
        data = b'{"k":"\xc3\xa9"}\r\n'
        tree = {"X/Fixtures/selection.json": data}
        self.assertEqual(run(self.root, tree=tree)[0], 0)
        path = self.out / "reminders/selection.json"
        self.assertEqual(path.read_bytes(), data)
        line = cf.manifest_line(data, "reminders/selection.json", "X/Fixtures/selection.json")
        self.assertEqual(cf.parse_manifest((self.out / cf.MANIFEST).read_text()), {"reminders/selection.json": line})
        self.assertEqual(run(self.root, "--check", tree=tree)[0], 0)
        path.write_bytes(data + b" ")
        self.assertEqual(run(self.root, "--check", tree=tree)[0], 1)
        path.write_bytes(data)
        (path.parent / "orphan.json").write_bytes(b"{}")
        self.assertEqual(run(self.root, "--check", tree=tree)[0], 1)

    def test_missing_repo_exits_two(self):
        with contextlib.redirect_stderr(io.StringIO()):
            saved = cf.os.environ.pop("IOS_REPO", None)
            try:
                self.assertEqual(cf.main([], read=fake_read(TREE), root=self.root), 2)
            finally:
                if saved:
                    cf.os.environ["IOS_REPO"] = saved


if __name__ == "__main__":
    unittest.main()
