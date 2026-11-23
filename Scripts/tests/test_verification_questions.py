import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('verification_questions', Path(__file__).resolve().parents[1] / 'verification-questions.py')
REGISTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REGISTER)


class VerificationQuestionsTest(unittest.TestCase):
    def test_four_column_row_preserves_default_and_phases(self):
        rows = REGISTER.questions('| OQ-A620 | Question? | Use `en\\|nl`; keep the default. | 13, 14 |')
        self.assertEqual([('OQ-A620', 'Use `en\\|nl`; keep the default.', '13, 14')], rows)

    def test_legacy_row_extracts_affected_phase_without_changing_default(self):
        default = 'Keep the native implementation. Affects 1c.'
        self.assertEqual([('OQ-A50', default, '1c')],
                         REGISTER.questions('| OQ-A50 | Old question | ' + default + ' |'))

    def test_missing_evidence_is_rejected(self):
        for content in ('# no rows', '| OQ-A1 | Question | Missing phases |',
                        '| OQ-A68 | Reserved | Default | 14 |',
                        '| OQ-A1 | Q |  | 14 |',
                        '| OQ-A1 | Q | D | 14 |\n| OQ-A1 | Q | D | 14 |'):
            with self.subTest(content=content):
                with self.assertRaises(ValueError):
                    REGISTER.questions(content)

    def test_check_rejects_a_stale_register_and_generation_repairs_it(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'Scripts').mkdir()
            (root / 'docs').mkdir()
            script = root / 'Scripts' / 'verification-questions.py'
            script.write_text(Path(SPEC.origin).read_text())
            (root / 'plan.md').write_text('| OQ-A620 | Q | Default | 14 |\n')
            output = root / 'docs' / 'verification-questions.md'
            output.write_text('old evidence')
            stale = subprocess.run([sys.executable, '-I', str(script), '--check'], capture_output=True, text=True, timeout=5)
            self.assertEqual(1, stale.returncode)
            self.assertIn('stale register', stale.stderr)
            generated = subprocess.run([sys.executable, '-I', str(script)], capture_output=True, text=True, timeout=5)
            self.assertEqual(0, generated.returncode, generated.stderr)
            self.assertIn('**OQ-A620** — Default — Affected phases: 14.', output.read_text())
            checked = subprocess.run([sys.executable, '-I', str(script), '--check'], capture_output=True, text=True, timeout=5)
            self.assertEqual(0, checked.returncode, checked.stderr)
