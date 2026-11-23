import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("board_coverage", ROOT / "Scripts/board-coverage.py")
COVERAGE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(COVERAGE)
FIXTURE = ROOT / "Scripts/fixtures/board-coverage"


def mini_scope():
    return {"schema": 1, "excluded": {"superseded": ["TripEditor*"]},
            "kinds": {"nonScreen": {"AppIcon": "Launcher capture"},
                      "wideOnly": {"PlanWide": "Wide sibling"}},
            "notBuilt": {"GuestExcluded": {"phase": "11b", "reason": "Retired health"}},
            "cellExemptions": {}}


class BoardCoverageTest(unittest.TestCase):
    def test_exclusions_and_grouping(self):
        canvas = json.loads((FIXTURE / "canvas.json").read_text())
        result = COVERAGE.report(canvas, mini_scope(), FIXTURE / "captures")
        self.assertEqual(result["counts"], {"total": 10, "excluded": 1, "inScope": 9, "groups": 5})
        self.assertEqual(result["groups"]["Plan"]["boards"],
                         ["DarkPlan", "Plan", "PlanDark", "PlanDarkNL", "PlanNL"])
        self.assertIn("PlanRemoteLoadingNL", result["groups"])
        self.assertEqual(result["groups"]["Plan"]["cells"], {
            "pixel8_en_light": ["Plan_en_light.png"],
            "pixel8_nl_dark": ["device/PlanDarkNL_device_nl_dark.png"]})

    def test_name_parsing(self):
        self.assertEqual(COVERAGE.parse_capture("Plan_en_light.png"), ("Plan", "pixel8_en_light"))
        self.assertEqual(COVERAGE.parse_capture("Plan_scrolled_foldInner_nl_dark.png"),
                         ("Plan", "foldInner_nl_dark"))
        self.assertEqual(COVERAGE.parse_capture("device/PlanDark_pixel_en_dark.png"),
                         ("PlanDark", "pixel8_en_dark"))
        self.assertIsNone(COVERAGE.parse_capture("material/Switch_pixel_en_light.png"))
        self.assertIsNone(COVERAGE.parse_capture("device/designsystem/Buttons_en_light.png"))
        with self.assertRaisesRegex(ValueError, "invalid capture name"):
            COVERAGE.parse_capture("Plan_en_blue.png")
        with self.assertRaisesRegex(ValueError, "invalid capture name"):
            COVERAGE.parse_capture("../Plan_en_light.png")

    def test_suffix_only_when_base_exists(self):
        self.assertEqual(COVERAGE.group_name("GhostDarkNL", {"GhostDarkNL"}), "GhostDarkNL")
        self.assertEqual(COVERAGE.group_name("DarkGhost", {"DarkGhost"}), "DarkGhost")
        self.assertEqual(COVERAGE.group_name("PlanRemoteLoadingNL", {"PlanRemoteLoadingNL"}), "PlanRemoteLoadingNL")

    def test_cells_by_kind(self):
        self.assertEqual(COVERAGE.required_cells("standard"), [
            "pixel8_en_light", "pixel8_en_dark", "pixel8_nl_light", "pixel8_nl_dark",
            "foldInner_nl_light", "foldInner_en_dark", "largeFont_nl_light", "largeFont_en_dark"])
        self.assertEqual(len(COVERAGE.required_cells("wideOnly")), 4)
        self.assertEqual(len(COVERAGE.required_cells("phoneWithWideSibling")), 6)
        self.assertEqual(len(COVERAGE.required_cells("largeFontOnly")), 2)
        self.assertEqual(COVERAGE.required_cells("nonScreen"), [])
        self.assertEqual(COVERAGE.required_cells("notBuilt"), [])

    def test_exemption_needs_reason(self):
        scope = mini_scope()
        scope["cellExemptions"] = {"Plan": {"pixel8_en_light": " "}}
        with self.assertRaisesRegex(ValueError, "non-empty reason"):
            COVERAGE.report(json.loads((FIXTURE / "canvas.json").read_text()), scope, FIXTURE / "captures")

    def test_exemption_covers_only_its_cell(self):
        scope = mini_scope()
        scope["cellExemptions"] = {"Plan": {"foldInner_nl_light": "System window; device evidence"}}
        result = COVERAGE.report(json.loads((FIXTURE / "canvas.json").read_text()), scope, FIXTURE / "captures")
        self.assertNotIn("foldInner_nl_light", result["groups"]["Plan"]["missing"])
        self.assertIn("foldInner_en_dark", result["groups"]["Plan"]["missing"])

    def test_invalid_names_are_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            captures = Path(directory)
            (captures / "Plan_en_blue.png").touch()
            result = COVERAGE.report(json.loads((FIXTURE / "canvas.json").read_text()), mini_scope(), captures)
            self.assertEqual(result["invalidNames"], ["invalid capture name: Plan_en_blue.png"])

    def test_canvas_drift_exits_three(self):
        with tempfile.TemporaryDirectory() as directory:
            scope = Path(directory) / "scope.json"
            scope.write_text(json.dumps(mini_scope()))
            result = subprocess.run([sys.executable, "-I", str(ROOT / "Scripts/board-coverage.py"),
                                     "--canvas", str(FIXTURE / "canvas.json"), "--scope", str(scope),
                                     "--captures", str(FIXTURE / "captures"), "--out", directory],
                                    cwd=ROOT, capture_output=True, text=True, timeout=5)
            self.assertEqual(result.returncode, 3)
            self.assertIn("board-coverage: canvas changed", result.stderr)
            self.assertFalse((Path(directory) / "coverage.json").exists())

    def test_real_canvas_counts(self):
        result = COVERAGE.report(json.loads((ROOT / "_design/canvas/canvas.json").read_text()),
                                 json.loads((ROOT / "tools/audit/board-scope.json").read_text()),
                                 ROOT / "build/does-not-exist", (ROOT / "plan.md").read_text())
        self.assertEqual(result["counts"], COVERAGE.EXPECTED_COUNTS)
        self.assertEqual(result["groups"]["Plan"]["ownerPhase"], "6b")
        self.assertEqual(result["groups"]["WizardQAge"]["ownerPhase"], "9e")
        self.assertFalse({name for name, group in result["groups"].items() if not group["ownerPhase"]})


if __name__ == "__main__":
    unittest.main()
