import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('strictmode', ROOT / 'Scripts/strictmode-scan.py')
strict = importlib.util.module_from_spec(spec)
spec.loader.exec_module(strict)
FIXTURES = ROOT / 'Scripts/fixtures/strictmode'


class StrictModeScanTest(unittest.TestCase):
    def scan(self, name):
        return strict.scan((FIXTURES / name).read_text())

    def test_disk_read_with_any_app_frame_is_ours(self):
        result = self.scan('ours.txt')
        self.assertEqual([{'type':'DiskReadViolation','top':'net.chronoflow.app.Example.read(Example.kt:3)', 'count':1}],
                         result['ours'])
        self.assertEqual([], result['thirdParty'])

    def test_framework_only_stack_is_third_party(self):
        result = self.scan('third-party.txt')
        self.assertEqual([], result['ours'])
        self.assertEqual('com.google.example.Library.write(Library.java:2)', result['thirdParty'][0]['top'])

    def test_interleaved_pids_and_threads_do_not_mix_frames(self):
        result = self.scan('interleaved.txt')
        self.assertEqual(['DiskReadViolation'], [r['type'] for r in result['ours']])
        self.assertEqual(['DiskWriteViolation','NetworkViolation'], [r['type'] for r in result['thirdParty']])

    def test_no_violation_exits_zero_and_writes_empty_groups(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / 'result.json'
            self.assertEqual(0, strict.main([str(FIXTURES / 'clean.txt'), '--out', str(out)]))
            self.assertEqual({'ours':[], 'thirdParty':[]}, json.loads(out.read_text()))

    def test_repeated_blocks_are_counted_and_ours_exits_one(self):
        with tempfile.TemporaryDirectory() as tmp:
            log, out = Path(tmp) / 'log', Path(tmp) / 'result.json'
            log.write_text((FIXTURES / 'ours.txt').read_text() * 2)
            self.assertEqual(1, strict.main([str(log), '--out', str(out)]))
            self.assertEqual(2, json.loads(out.read_text())['ours'][0]['count'])


if __name__ == '__main__':
    unittest.main()
