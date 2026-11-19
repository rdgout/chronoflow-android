import importlib.util
import json
from pathlib import Path
import unittest

from helpers import ROOT

spec = importlib.util.spec_from_file_location("fold_check", ROOT / "Scripts/fold_check.py")
fold = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fold)


def dump(day="day-a", sheet=True, wide=True, day_top=180):
    sheet_node = '<node resource-id="step.sheet" bounds="[100,400][900,900]"/>' if sheet else ""
    list_node = '<node resource-id="trip.wide.list.%s" bounds="[600,180][980,800]"/>' % day if wide else ""
    return ('<displays><window><hierarchy><node resource-id="trip.scroll" bounds="[0,100][1000,1000]"/>'
            '<node resource-id="trip.day.%s" bounds="[20,%s][980,1300]"/>%s</hierarchy></window>'
            '<window><hierarchy>%s</hierarchy></window></displays>') % (day, day_top, list_node, sheet_node)


class FoldCheckToolsTest(unittest.TestCase):
    def test_all_window_dump_keeps_the_day_and_sheet(self):
        self.assertEqual(fold.anchor_from_dump(dump(), "trip.day.day-a", wide=True), "trip.day.day-a")

    def test_missing_sheet_fails(self):
        with self.assertRaisesRegex(ValueError, "step.sheet is absent"):
            fold.anchor_from_dump(dump(sheet=False))

    def test_a_changed_day_fails(self):
        with self.assertRaisesRegex(ValueError, "opening day changed"):
            fold.anchor_from_dump(dump(day="day-b"), "trip.day.day-a")

    def test_a_day_far_from_the_top_fails(self):
        with self.assertRaisesRegex(ValueError, "no longer near"):
            fold.anchor_from_dump(dump(day_top=700))

    def test_an_unchanged_width_mode_is_not_a_successful_fold(self):
        with self.assertRaisesRegex(ValueError, "requested width mode"):
            fold.anchor_from_dump(dump(wide=True), wide=False)

    def test_a_folded_window_has_no_wide_list(self):
        self.assertEqual(fold.anchor_from_dump(dump(wide=False), wide=False), "trip.day.day-a")

    def test_probe_does_not_need_a_sheet(self):
        self.assertEqual(fold.anchor_from_dump(dump(sheet=False), require_sheet=False), "trip.day.day-a")

    def test_activity_record_and_task_are_both_kept(self):
        record = "mResumedActivity: ActivityRecord{abc123 u0 " + fold.ACTIVITY + " t42}"
        self.assertEqual(fold.activity_record(record), ("abc123", "42"))

    def test_missing_activity_identity_fails(self):
        with self.assertRaisesRegex(ValueError, "identity is absent"):
            fold.activity_record("no resumed MainActivity")

    def test_closed_state_ignores_half_folded(self):
        states = "DeviceState{identifier=2, name='HALF_FOLDED'}\nDeviceState{identifier=1, name='CLOSED'}"
        self.assertEqual(fold.state_id(states, True), "1")

    def test_open_state_is_not_mistaken_for_folded(self):
        states = "DeviceState{identifier=0, name='FOLDED'}\nDeviceState{identifier=3, name='UNFOLDED'}"
        self.assertEqual(fold.state_id(states, False), "3")

    def test_unrecognized_states_fail_with_the_command_output(self):
        with self.assertRaisesRegex(ValueError, "print-states"):
            fold.state_id("unknown device state format", False)

    def test_fixture_selection_uses_the_observed_day_id(self):
        template = json.loads((ROOT / "core/testing/src/main/resources/fixtures/mock-seed.json").read_text())["trips"][0]
        self.assertEqual(fold.block_for_day(template, "trip.day.day-29865930"), "light-avoid_light-29865930")

    def test_duplicate_relaunch_events_still_count(self):
        line = "I/am_relaunch_activity: " + fold.ACTIVITY
        self.assertEqual(sum((fold.relaunches(line + "\n" + line) - fold.relaunches(line)).values()), 1)


if __name__ == "__main__":
    unittest.main()
