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
        (root / "tools/assets").mkdir(parents=True)
        (root / build_assets.MANIFEST).write_text("# ios-pin test\n")
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


class MoonSubstitutionTest(unittest.TestCase):
    P3 = "0.8542512390 0.5599361895 0.3257756868"
    SVG = f'<svg><g stroke="color(display-p3 {P3})" stroke-width="1.6"/></svg>'

    def test_substitutes_only_the_stroke(self):
        result = build_assets.substitute_stroke(self.SVG, self.P3, "#E78A45")
        self.assertEqual(result, '<svg><g stroke="#E78A45" stroke-width="1.6"/></svg>')

    def test_svg_without_a_display_p3_stroke_fails(self):
        with self.assertRaises(build_assets.AssetError):
            build_assets.substitute_stroke('<svg><g stroke="#E78A45"/></svg>', self.P3, "#E78A45")

    def test_p3_triple_that_differs_from_the_report_fails(self):
        with self.assertRaises(build_assets.AssetError):
            build_assets.substitute_stroke(self.SVG, "0.9082625891 0.6882337800 0.4860896207", "#F2AD73")

    def test_dawn_cells_come_from_the_report(self):
        cells = build_assets.dawn_cells((ROOT / build_assets.REPORT).read_text())
        self.assertEqual(cells["light"][0], "#E78A45")
        self.assertEqual(cells["dark"][0], "#F2AD73")
        self.assertEqual(cells["light"][1], self.P3)

    def test_check_fails_when_a_manifest_entry_changes(self):
        root = temp_dir(self)
        shutil.copytree(ROOT / ICON_DIR, root / ICON_DIR)
        (root / "tools/assets").mkdir(parents=True)
        payload = root / "tools/assets/payload.png"
        payload.write_bytes(b"png")
        digest = build_assets.sha256(b"png")
        (root / build_assets.MANIFEST).write_text(f"# ios-pin test\n{digest}  -  tools/assets/payload.png\n")
        self.assertEqual(build_assets.main(["icons"], root), 0)
        self.assertEqual(build_assets.main(["check"], root), 0)
        payload.write_bytes(b"pnG")
        self.assertEqual(build_assets.main(["check"], root), 1)


if __name__ == "__main__":
    unittest.main()
