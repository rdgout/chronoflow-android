import importlib.util
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('font_scale', ROOT / 'Scripts/font-scale-check.py')
font = importlib.util.module_from_spec(spec)
spec.loader.exec_module(font)


class FontScaleCheckTest(unittest.TestCase):
    def setUp(self):
        self.capped = (ROOT / 'Scripts/fixtures/font-scale/capped.xml').read_text()
        self.small = self.capped.replace('[500,100]', '[400,80]')

    def test_bounds_from_fixture(self):
        self.assertEqual((20, 40, 500, 100), font.geometry(self.capped)[0]['screen.heading'])

    def test_equal_cap_passes(self):
        self.assertEqual(2, font.compare({'1.0': self.small, '1.3': self.capped, '2.0': self.capped}))

    def test_one_pixel_difference_names_tag(self):
        with self.assertRaisesRegex(font.DeviceFailure, 'screen.heading'):
            font.compare({'1.0': self.small, '1.3': self.capped, '2.0': self.capped.replace('[500,100]', '[501,100]')})

    def test_unchanged_small_scale_fails(self):
        with self.assertRaisesRegex(font.DeviceFailure, 'scale not applied'):
            font.compare(dict.fromkeys(('1.0', '1.3', '2.0'), self.capped))

    def test_system_ui_node_change_does_not_prove_scale_was_applied(self):
        unchanged_small = self.capped.replace(
            '<hierarchy>', '<hierarchy><node package="com.android.systemui" class="android.widget.TextView" '
            'resource-id="clock" bounds="[0,0][20,20]"/>')
        with self.assertRaisesRegex(font.DeviceFailure, 'scale not applied'):
            font.compare({'1.0': unchanged_small, '1.3': self.capped, '2.0': self.capped})

    def test_exception_restores_scale(self):
        calls = []
        scale = ['1.0']

        def adb(command, **kwargs):
            calls.append(command)
            if 'put' in command:
                scale[0] = command[-1]
            return SimpleNamespace(returncode=0, stdout=scale[0])

        device = font.Device(run=adb)
        with tempfile.TemporaryDirectory() as tmp:
            audit = font.FontScaleAudit(device, Path(tmp) / 'captures', Path(tmp) / 'result.json')
            audit.capture = lambda *args: (_ for _ in ()).throw(RuntimeError('fixture fault'))
            with self.assertRaisesRegex(RuntimeError, 'fixture fault'):
                audit.run([{'name': 'one'}])
        self.assertEqual(['shell', 'settings', 'put', 'system', 'font_scale', '1.0'], calls[-2][-6:])

    def test_manifest_has_the_nine_named_screens_in_dutch(self):
        rows = json.loads((ROOT / 'tools/audit/font-scale.json').read_text())['screens']
        self.assertEqual({'welcome', 'sign-in', 'today', 'trips', 'trip-timeline', 'trip-list', 'settings',
                          'preferences', 'wizard-outbound'}, {r['name'] for r in rows})
        self.assertEqual(9, len(rows))


if __name__ == '__main__':
    unittest.main()
