import importlib.util
import shutil
import unittest

from helpers import ROOT, temp_dir

SPEC = importlib.util.spec_from_file_location("build_assets", ROOT / "tools/assets/build_assets.py")
build_assets = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(build_assets)

ICON_DIR = build_assets.ICON_DIR


class BuildAssetsTest(unittest.TestCase):
    def make_root(self):
        root = temp_dir(self)
        shutil.copytree(ROOT / ICON_DIR, root / ICON_DIR)
        return root

    def test_icons_twice_give_identical_bytes(self):
        root = self.make_root()
        first = build_assets.generate_icons(root)
        second = build_assets.generate_icons(root)
        self.assertEqual(first, second)
        self.assertIn("app/src/main/res/mipmap-anydpi/ic_launcher.xml", first)
        self.assertIn("app/src/main/res/drawable/ic_stat_moon.xml", first)

    def test_background_stop_that_differs_from_colors_json_fails(self):
        root = self.make_root()
        svg = root / ICON_DIR / "android/adaptive-background.svg"
        svg.write_text(svg.read_text().replace("#06091c", "#06091d"))
        with self.assertRaises(build_assets.AssetError):
            build_assets.generate_icons(root)

    def test_check_passes_then_fails_when_a_committed_byte_changes(self):
        root = self.make_root()
        self.assertEqual(build_assets.main(["icons"], root), 0)
        self.assertEqual(build_assets.main(["check"], root), 0)
        committed = root / "app/src/main/res/drawable/ic_launcher_foreground.xml"
        committed.write_text(committed.read_text().replace("341.6", "341.7"))
        self.assertEqual(build_assets.main(["check"], root), 1)


if __name__ == "__main__":
    unittest.main()
