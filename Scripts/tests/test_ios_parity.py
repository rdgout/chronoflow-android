import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('ios_parity', ROOT / 'Scripts/ios-parity.py')
parity = importlib.util.module_from_spec(spec)
spec.loader.exec_module(parity)
from audit_device import Device, DeviceFailure, first_sleep_block


class FakeClock:
    def __init__(self):
        self.time = 0

    def now(self):
        return self.time

    def sleep(self, seconds):
        self.time += seconds


class IosParityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.log = self.root / 'commands.jsonl'
        self.refs = self.root / 'refs'
        self.refs.mkdir()
        fixture = ROOT / 'Scripts/fixtures/ios-parity/bin'
        self.env = patch.dict(os.environ, {'PATH': str(fixture) + os.pathsep + os.environ['PATH'],
                                           'ADB': str(fixture / 'adb'), 'CF_FAKE_LOG': str(self.log)})
        self.env.start()
        self.addCleanup(self.env.stop)
        self.device = Device()
        self.runner = parity.Parity(self.device, self.root / 'out', self.refs)

    def entry(self, name='test.png', seeds=None, wait='today.screen'):
        (self.refs / name).write_bytes(b'ios-placeholder')
        return {'ios': name, 'seeds': seeds or {}, 'extras': {'cf.settings.language': 'nl'},
                'profile': 'screen', 'wait': wait, 'frames': None, 'skip': None}

    def commands(self):
        return [json.loads(line) for line in self.log.read_text().splitlines()]

    def assert_launch(self, entry):
        self.runner.sweep([entry])
        command = next(c for c in self.commands() if 'start' in c)
        self.assertEqual(['adb', '-s', 'emulator-5554', 'shell', 'am', 'start', '-W', '-n',
                          'net.chronoflow.app.native.debug/net.chronoflow.debug.SeedReceiverActivity'], command[:9])
        extras = dict(zip(command[10::3], command[11::3]))
        self.assertEqual('screen', extras['CF_UI_TEST_PROFILE'])
        self.assertEqual('nl', extras['cf.settings.language'])
        for key, value in entry['seeds'].items():
            self.assertEqual(value, extras[key])
        import uuid
        self.assertEqual(str(uuid.UUID(extras['CF_AUTH_TEST_ID'])), extras['CF_AUTH_TEST_ID'])

    def test_today_start_arguments(self):
        self.assert_launch(self.entry(seeds={'CF_AUTH_TEST_SIGNED_IN': 'demo'}))

    def test_trip_start_arguments(self):
        self.assert_launch(self.entry(seeds={'CF_PLAN_TEST_ROUTE': 'trip:0199a3c0-0000-7000-8000-000000000001',
                                            'CF_PLAN_TEST_FREEZE_CLOCK': '1'}))

    def test_settings_start_arguments(self):
        self.assert_launch(self.entry(seeds={'CF_SETTINGS_ROUTE': 'none'}))

    def test_poll_gives_up_at_twenty_seconds(self):
        clock = FakeClock()
        d = Device(now=clock.now, sleep=clock.sleep)
        d.dump = lambda timeout: '<hierarchy/>'
        with self.assertRaisesRegex(DeviceFailure, 'within 20 s'):
            d.wait_tag('missing', timeout=20)
        self.assertEqual(20, clock.time)

    def test_failed_entry_does_not_stop_the_run(self):
        clock = FakeClock()
        self.device.now, self.device.sleep = clock.now, clock.sleep
        self.device.dump = lambda timeout: '<hierarchy><node resource-id="good"/></hierarchy>'
        rows = self.runner.sweep([self.entry('bad.png', wait='bad'), self.entry('good.png', wait='good')])
        self.assertEqual(['failed', 'captured'], [r['status'] for r in rows])
        self.assertEqual(rows, json.loads((self.runner.out / 'results.json').read_text())['entries'])

    def test_partial_rerun_preserves_other_entries_and_replaces_attempted_entry(self):
        first = self.entry('first.png')
        second = self.entry('second.png')
        self.runner.sweep([first, second])
        first['skip'] = 'documented unavailable fixture'
        attempted = self.runner.sweep([first], preserve=True)
        self.assertEqual(1, len(attempted))
        rows = json.loads((self.runner.out / 'results.json').read_text())['entries']
        self.assertEqual({'first.png': 'skipped', 'second.png': 'captured'},
                         {r['ios']: r['status'] for r in rows})

    def test_skip_writes_no_files(self):
        entry = self.entry()
        entry['skip'] = 'historical diagnostic cannot be reproduced'
        row = self.runner.entry(entry)
        self.assertEqual('skipped', row['status'])
        self.assertFalse(self.log.exists())
        self.assertFalse(self.runner.out.exists())

    def test_ffmpeg_pair_command(self):
        expected = ['ffmpeg', '-y', '-loglevel', 'error', '-i', 'ios.png', '-i', 'android.png',
                    '-filter_complex', '[0:v]scale=-2:1600[a];[1:v]scale=-2:1600[b];[a][b]hstack=inputs=2',
                    '-frames:v', '1', 'pair.png']
        self.assertEqual(expected, parity.pair_command('ios.png', 'android.png', 'pair.png'))

    def test_motion_extracts_the_held_final_frame_after_decoding(self):
        command = parity.frame_command('video.mp4', 6.0, 'frame.png')
        self.assertLess(command.index('-i'), command.index('-ss'))
        self.assertIn('tpad=stop_mode=clone:stop_duration=8', command)
        self.assertEqual('6.0', command[command.index('-ss') + 1])

    def test_recording_duration_rejects_missing_evidence(self):
        self.assertEqual(7.22, parity.recording_duration('Duration: 00:00:07.22, start: 0.000000'))
        with self.assertRaisesRegex(DeviceFailure, 'duration is unavailable'):
            parity.recording_duration('no duration')

    def test_recording_duration_includes_the_observed_unchanged_tail(self):
        self.assertEqual(8.1, parity.recording_duration('Duration: 00:00:03.99', 8.1))

    def test_motion_alignment_uses_initial_black_end(self):
        self.assertEqual((0.32, 'frames relative to first non-black frame'),
                         parity.first_nonblack_offset('black_start:0 black_end:0.32 black_duration:0.32'))

    def test_motion_alignment_records_the_fallback(self):
        offset, note = parity.first_nonblack_offset('no initial black frames')
        self.assertEqual(0, offset)
        self.assertIn('recording start', note)

    def test_motion_failure_restores_every_animation_scale(self):
        class FailedRecorder:
            def wait(self, timeout):
                return 1

            def poll(self):
                return 1

        self.runner.popen = lambda *args, **kwargs: FailedRecorder()
        entry = self.entry()
        entry['frames'] = [400]
        with self.assertRaisesRegex(DeviceFailure, 'screenrecord failed'):
            self.runner.record(entry)
        restored = [c[-2:] for c in self.commands() if c[3:7] == ['shell', 'settings', 'put', 'global'] and c[-1] == '0']
        self.assertEqual([['animator_duration_scale', '0'], ['transition_animation_scale', '0'],
                          ['window_animation_scale', '0']], restored)

    def test_manifest_has_every_reference_and_launcher_once(self):
        entries = json.loads((ROOT / 'tools/audit/ios-parity.json').read_text())['entries']
        expected = {p.name for p in (ROOT / 'docs/research/screens/ios').glob('*.png')} | {'launcher-icon'}
        self.assertEqual(57, len(entries))
        self.assertEqual(expected, {e['ios'] for e in entries})
        self.assertEqual(57, len({e['ios'] for e in entries}))

    def test_sleep_id_is_resolved_from_the_opening_day(self):
        self.assertEqual('sleep-sleep-29865450', first_sleep_block())

    def test_failed_refresh_removes_stale_images(self):
        self.runner.out.mkdir()
        (self.runner.out / 'pairs').mkdir()
        for target in [self.runner.out / 'test.png', self.runner.out / 'pairs/test.png']:
            target.write_bytes(b'stale')
        self.device.uptime = lambda: (_ for _ in ()).throw(DeviceFailure('unavailable'))
        self.assertEqual('failed', self.runner.sweep([self.entry()])[0]['status'])
        self.assertFalse((self.runner.out / 'test.png').exists())
        self.assertFalse((self.runner.out / 'pairs/test.png').exists())


if __name__ == '__main__':
    unittest.main()
