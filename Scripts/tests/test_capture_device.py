import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('capture_device', ROOT / 'Scripts/capture-device.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class CaptureDeviceTest(unittest.TestCase):
    def test_unfiltered_run_selects_both_capture_hosts(self):
        self.assertEqual(['core/ui', 'app'], module.capture_modules(''))

    def test_material_routes_to_app(self):
        self.assertEqual(['app'], module.capture_modules('material'))

    def test_mixed_filters_select_both_hosts(self):
        self.assertEqual(['core/ui', 'app'], module.capture_modules('designsystem,PickerDateTime'))

    def test_material_subdirectory_is_preserved(self):
        self.assertEqual(Path('material/Alert_reset_pixel_nl_dark.png'),
                         module.capture_relative(Path('debug/pixel/material/Alert_reset_pixel_nl_dark.png')))

    def test_primitive_subdirectory_is_preserved(self):
        self.assertEqual(Path('designsystem/Buttons_pixel_en_light.png'),
                         module.capture_relative(Path('debug/pixel/designsystem/Buttons_pixel_en_light.png')))

    def test_flat_board_name_stays_flat(self):
        self.assertEqual(Path('PickerDateTime_date_en_light.png'),
                         module.capture_relative(Path('debug/pixel/PickerDateTime_date_en_light.png')))

    def test_timing_artifact_stays_with_material_captures(self):
        self.assertEqual(Path('material/timings.jsonl'),
                         module.capture_relative(Path('debug/pixel/material/timings.jsonl')))


if __name__ == '__main__':
    unittest.main()
