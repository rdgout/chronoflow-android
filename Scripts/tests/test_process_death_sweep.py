import datetime
import importlib.util
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('process_death', ROOT / 'Scripts/process-death-sweep.py')
death = importlib.util.module_from_spec(spec)
spec.loader.exec_module(death)


class FakeDevice(death.Device):
    def __init__(self, xml='<hierarchy><node resource-id="ready" bounds="[10,20][30,40]"/></hierarchy>'):
        self.time, self.calls, self.xml = 0, [], xml
        super().__init__(run=self.adb, now=lambda: self.time, sleep=self.tick)

    def tick(self, seconds):
        self.time += seconds

    def adb(self, command, **kwargs):
        self.calls.append(command[3:])
        output = ''
        if command[-4:-1] == ['settings', 'get', 'global']:
            output = '0'
        if command[-3:] == ['dumpsys', 'activity', 'activities']:
            output = 'ActivityRecord{abc u0 ' + death.ACTIVITY + ' t1} mStopped=true'
        return SimpleNamespace(returncode=0, stdout=output)

    def dump(self, timeout=15):
        return self.xml

    def screenshot(self, path):
        pass


class ProcessDeathSweepTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.device = FakeDevice()
        self.sweep = death.ProcessDeathSweep(self.device, self.root / 'captures', self.root / 'result.json')

    def test_steps_translate_bounds_and_text_without_logging_values(self):
        self.sweep.steps([{'tap': 'ready'}, {'text': 'ready', 'value': 'fixture value'}, {'wait': 'ready'}])
        self.assertIn(['shell', 'input', 'tap', '20', '30'], self.device.calls)
        self.assertIn(['shell', 'input', 'text', 'fixture%svalue'], self.device.calls)
        self.assertFalse(self.sweep.report.exists())

    def test_kill_sequence_waits_for_stopped_then_pid_gone(self):
        self.device.xml = '<hierarchy><node resource-id="launch.opening"/></hierarchy>'
        self.sweep.kill_and_relaunch()
        calls = self.device.calls
        home = calls.index(['shell', 'input', 'keyevent', '3'])
        stopped = calls.index(['shell', 'dumpsys', 'activity', 'activities'])
        kill = calls.index(['shell', 'am', 'kill', death.PACKAGE])
        pid = calls.index(['shell', 'pidof', death.PACKAGE])
        launcher = next(i for i, c in enumerate(calls) if c[:2] == ['shell', 'monkey'])
        self.assertLess(home, stopped)
        self.assertLess(stopped, kill)
        self.assertLess(kill, pid)
        self.assertLess(pid, launcher)

    def test_missing_overlay_is_not_a_cold_launch(self):
        with self.assertRaisesRegex(death.DeviceFailure, 'not a cold launch'):
            self.sweep.kill_and_relaunch()
        self.assertEqual(3, self.device.time)

    def test_absent_guard_fails_when_node_survives(self):
        self.sweep.fresh = lambda signed: None
        self.sweep.kill_and_relaunch = lambda: None
        row = self.sweep.case({'name':'absent','steps':[], 'expectAfter':['ready'], 'expectAbsent':['ready']})
        self.assertEqual('failed', row['status'])
        self.assertIn('unexpected restored node: ready', row['reason'])

    def test_failed_case_does_not_stop_the_other_cases(self):
        self.sweep.fresh = lambda signed: None
        self.sweep.kill_and_relaunch = lambda: None
        rows = self.sweep.run([{'name':'bad','steps':[], 'expectAfter':['missing']},
                               {'name':'good','steps':[], 'expectAfter':['ready']}])
        self.assertEqual(['failed', 'passed'], [r['status'] for r in rows])
        self.assertEqual(rows, json.loads(self.sweep.report.read_text())['cases'])

    def test_demo_rejection_stops_the_sweep_and_records_the_oq(self):
        self.sweep.fresh = lambda signed: (_ for _ in ()).throw(death.DemoRejected('OQ-14-3'))
        with self.assertRaises(death.DemoRejected):
            self.sweep.run([{'name':'first','steps':[]}, {'name':'second','steps':[]}])
        report = json.loads(self.sweep.report.read_text())
        self.assertEqual('OQ-14-3', report['stopped'])
        self.assertEqual(['first'], [r['name'] for r in report['cases']])

    def test_manifest_has_all_nine_cases_and_no_seed_keys(self):
        path = ROOT / 'tools/audit/process-death.json'
        cases = json.loads(path.read_text())['cases']
        self.assertEqual({'welcome', 'sign-in-typed', 'today', 'trips', 'trip-sheet', 'settings-sub',
                          'password-prompt', 'wizard-recovery', 'guest-recovery'}, {c['name'] for c in cases})
        self.assertNotIn('CF_', path.read_text())
        self.assertEqual(9, len(cases))

    def test_sweep_restores_animation_scales_after_rejection(self):
        self.sweep.fresh = lambda signed: (_ for _ in ()).throw(death.DemoRejected('OQ-14-3'))
        with self.assertRaises(death.DemoRejected):
            self.sweep.run([{'name':'first','steps':[]}])
        restored = [c[-2:] for c in self.device.calls if c[:4] == ['shell','settings','put','global'] and c[-1] == '0']
        self.assertEqual([['animator_duration_scale','0'], ['transition_animation_scale','0'],
                          ['window_animation_scale','0']], restored)

    def test_native_calendar_selects_the_exact_variable_date_then_done(self):
        self.device.xml = ('<hierarchy><node resource-id="wizard.date-dialog">'
                           '<node text="Tuesday, November 24, 2026" enabled="true" clickable="true" bounds="[10,20][30,40]"/>'
                           '<node resource-id="wizard.picker.done" bounds="[50,20][70,40]"/></node></hierarchy>')
        self.sweep.native_calendar(datetime.date(2026, 11, 24))
        taps = [c for c in self.device.calls if c[:3] == ['shell','input','tap']]
        self.assertEqual([['shell','input','tap','20','30'], ['shell','input','tap','60','30']], taps)

    def test_native_calendar_does_not_match_another_date_or_a_prefix(self):
        self.device.xml = ('<hierarchy><node resource-id="wizard.date-dialog">'
                           '<node text="Tuesday, November 24, 2026 extra" enabled="true" clickable="true" '
                           'bounds="[10,20][30,40]"/></node></hierarchy>')
        with self.assertRaisesRegex(death.DeviceFailure, 'native next-month control is unavailable'):
            self.sweep.native_calendar(datetime.date(2026, 11, 24))
        self.assertFalse(any(c[:3] == ['shell','input','tap'] for c in self.device.calls))

    def test_launcher_disables_monkeys_physical_system_key_factor(self):
        self.sweep.launcher()
        self.assertEqual(['shell', 'monkey', '--pct-syskeys', '0', '-p', death.PACKAGE,
                          '-c', 'android.intent.category.LAUNCHER', '1'], self.device.calls[-1])

    def test_recovery_identifiers_come_from_the_unseeded_mock_airports(self):
        templates = json.loads((ROOT / 'core/testing/src/main/resources/fixtures/mock-seed.json').read_text())['trips']
        offered = {leg[side]['iata'] for trip in templates for leg in trip['detail']['trip']['legs']
                   for side in ('from', 'to')}
        cases = json.loads((ROOT / 'tools/audit/process-death.json').read_text())['cases']
        for case in cases:
            for airport in case.get('airports', {}).values():
                self.assertIn(airport, offered, case['name'])

    def test_stopped_parser_rejects_another_activity(self):
        self.assertFalse(death.stopped('ActivityRecord{other u0 other/package t1} mStopped=true'))
        self.assertFalse(death.stopped('ActivityRecord{abc u0 ' + death.ACTIVITY + ' t1} mStopped=false'))


if __name__ == '__main__':
    unittest.main()
