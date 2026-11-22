import importlib.util
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('resize', ROOT / 'Scripts/resize-check.py')
resize = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resize)
XML = ('<hierarchy><node resource-id="trip.page"/><node resource-id="trip.scroll" bounds="[0,100][1080,2300]"/>'
       '<node resource-id="trip.day.one" bounds="[0,100][1080,1900]"/><node resource-id="step.sheet"/></hierarchy>')


class ResizeCheckTest(unittest.TestCase):
    def test_px_math_at_420_dpi(self):
        self.assertEqual((840, 1838), (resize.pixels(320, 420), resize.pixels(700, 420)))
        self.assertEqual((2100, 945), (resize.pixels(800, 420), resize.pixels(360, 420)))

    def test_px_math_at_390_dpi(self):
        self.assertEqual((780, 1706), (resize.pixels(320, 390), resize.pixels(700, 390)))
        self.assertEqual((1950, 878), (resize.pixels(800, 390), resize.pixels(360, 390)))

    def test_override_density_wins(self):
        self.assertEqual(390, resize.density('Physical density: 420\nOverride density: 390\n'))

    def test_present_sheet_page_and_anchor_pass(self):
        self.assertEqual('trip.day.one', resize.validate(XML))

    def test_missing_sheet_fails(self):
        with self.assertRaisesRegex(resize.DeviceFailure, 'step.sheet'):
            resize.validate(XML.replace('<node resource-id="step.sheet"/>', ''))

    def test_overlay_fails(self):
        with self.assertRaisesRegex(resize.DeviceFailure, 'launch overlay'):
            resize.validate(XML.replace('</hierarchy>', '<node resource-id="launch.opening"/></hierarchy>'))

    def test_changed_anchor_fails(self):
        with self.assertRaisesRegex(ValueError, 'opening day changed'):
            resize.validate(XML, 'trip.day.other')

    def test_reset_in_finally_after_a_resize_error(self):
        commands = []

        def adb(command, **kwargs):
            commands.append(command[3:])
            if command[-2:] == ['wm', 'density']:
                output = 'Physical density: 420'
            else:
                output = ''
            return SimpleNamespace(returncode=0, stdout=output)

        device = resize.Device(serial='emulator-5556', run=adb)
        with tempfile.TemporaryDirectory() as tmp:
            check = resize.ResizeCheck(device, Path(tmp))
            check.seeded_sheet = lambda: 'trip.day.one'
            check.identity = lambda: (('record', 'task'), 'pid')
            check.events = lambda: {}
            device.dump = lambda **kwargs: (_ for _ in ()).throw(ValueError('fixture failure'))
            result = check.run()
            self.assertEqual('failed', result['status'])
            self.assertTrue(json.loads((Path(tmp) / 'result.json').read_text())['sizeReset'])
        self.assertEqual(['shell', 'wm', 'size', 'reset'], commands[-1])
        self.assertIn(['shell', 'wm', 'size', '840x1838'], commands)


if __name__ == '__main__':
    unittest.main()
