import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("wordmark", ROOT / "tools/assets/wordmark.py")
wordmark = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(wordmark)


class WordmarkTests(unittest.TestCase):
    def test_board_guard_rejects_changed_type(self):
        board = (ROOT / "_design/canvas/SplashScreen.dc.html").read_text()
        for before, after in (("font-size: 32px", "font-size: 33px"),
                              ("line-height: 40px", "line-height: 41px"),
                              ("letter-spacing: -0.01em", "letter-spacing: 0em")):
            with self.assertRaises(ValueError):
                wordmark.guard(board.replace(before, after))

    def test_output_is_byte_identical(self):
        self.assertEqual((ROOT / wordmark.OUTPUT).read_bytes(), wordmark.build(ROOT))
