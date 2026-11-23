import importlib.util
import json
import subprocess
import sys
import tempfile
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location('matrix_result', Path(__file__).resolve().parents[1] / 'matrix-result.py')
MATRIX = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MATRIX)


def evidence(**changes):
    return dict(tier='smoke-small', tests=[{'status': 'passed'}, {'status': 'skipped'}],
                started='2026-11-23T12:00:00Z', wallSeconds=2.5, exitCode=0,
                overBudget=0, errors=[], hangs=[], **changes)


class MatrixResultTest(unittest.TestCase):
    def test_preserves_counts_date_and_wall_time(self):
        result = MATRIX.summarize(evidence(), 'small')
        self.assertEqual(('passed', 2, 1, 1, 2.5),
                         (result['status'], result['tests'], result['passed'], result['skipped'], result['wallSeconds']))
        self.assertEqual('2026-11-23T12:00:00Z', result['started'])

    def test_rejects_wrong_avd_tier(self):
        with self.assertRaisesRegex(ValueError, 'AVD'):
            MATRIX.summarize(evidence(), 'fold')

    def test_empty_tests_cannot_pass(self):
        report = evidence(); report['tests'] = []
        with self.assertRaisesRegex(ValueError, 'no test evidence'):
            MATRIX.summarize(report, 'small')

    def test_all_skipped_cannot_pass(self):
        report = evidence(); report['tests'] = [{'status': 'skipped'}]
        self.assertEqual('failed', MATRIX.summarize(report, 'small')['status'])

    def test_failed_test_cannot_pass_even_with_zero_exit(self):
        report = evidence(); report['tests'].append({'status': 'failed'})
        self.assertEqual('failed', MATRIX.summarize(report, 'small')['status'])

    def test_missing_or_nonfinite_measurement_cannot_pass(self):
        for value in (None, float('nan'), -1, True):
            with self.subTest(value=value):
                report = evidence(); report['wallSeconds'] = value
                with self.assertRaisesRegex(ValueError, 'wall time'):
                    MATRIX.summarize(report, 'small')

    def test_missing_input_replaces_a_stale_success(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / 'result.json'
            output.write_text('{"status":"passed"}')
            result = subprocess.run([sys.executable, '-I', str(SPEC.origin), '--avd', 'small',
                                     '--input', str(root / 'missing.json'), '--output', str(output)],
                                    capture_output=True, text=True, timeout=5)
            self.assertEqual(1, result.returncode)
            self.assertEqual('failed', json.loads(output.read_text())['status'])

    def test_hang_or_timing_failure_cannot_pass(self):
        for field, value in [('hangs', ['stack.txt']), ('overBudget', 1), ('exitCode', 3)]:
            with self.subTest(field=field):
                report = evidence(); report[field] = value
                self.assertEqual('failed', MATRIX.summarize(report, 'small')['status'])
