import importlib.util
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('prepare_captures', Path(__file__).resolve().parents[1] / 'prepare-captures.py')
PREPARE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREPARE)


class PrepareCapturesTest(unittest.TestCase):
    def test_accumulation_removes_stale_selected_outputs_only(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            selected = root / 'Plan_pixel8_en_light.png'
            other = root / 'Main_pixel8_en_light.png'
            selected.touch()
            other.touch()
            PREPARE.prepare(root, 'Plan,Auth', True)
            self.assertFalse(selected.exists())
            self.assertTrue(other.exists())

    def test_default_run_clears_the_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / 'captures'
            root.mkdir()
            (root / 'Main_en_light.png').touch()
            PREPARE.prepare(root, 'Plan', False)
            self.assertFalse(root.exists())

    def test_unfiltered_run_clears_even_when_append_is_set(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / 'captures'
            root.mkdir()
            (root / 'Main_en_light.png').touch()
            PREPARE.prepare(root, '', True)
            self.assertFalse(root.exists())
