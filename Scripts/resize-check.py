"""On-request in-place resize audit on fold; invoke through device_lock.py."""
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_device import Device, DeviceFailure, ROOT, PACKAGE, TRIP, nodes
from fold_check import activity_record, anchor_from_dump, block_for_day, relaunches

PROFILES = (('split-narrow', 320, 700), ('landscape-short', 800, 360))


def density(output):
    matches = re.findall(r'(?:Physical|Override) density:\s*(\d+)', output)
    if not matches or int(matches[-1]) <= 0:
        raise DeviceFailure('wm density is unavailable')
    return int(matches[-1])


def pixels(dp, dpi):
    return int(dp * dpi / 160 + 0.5)


def validate(xml, day=None):
    tagged = nodes(xml)
    for tag in ('step.sheet', 'trip.page', 'trip.scroll'):
        if tag not in tagged:
            raise DeviceFailure('resize lost node: ' + tag)
    if 'launch.opening' in tagged:
        raise DeviceFailure('resize replayed the launch overlay')
    return anchor_from_dump(xml, expected_day=day)


class ResizeCheck:
    def __init__(self, device, out):
        self.device, self.out = device, Path(out)

    def identity(self):
        d = self.device
        return (activity_record(d.command('shell', 'dumpsys', 'activity', 'activities').stdout),
                d.command('shell', 'pidof', PACKAGE).stdout.strip())

    def events(self):
        return relaunches(self.device.command('logcat', '-d', '-b', 'events', '-v', 'brief', '-s',
                                              'am_relaunch_activity', 'am_relaunch_resume_activity').stdout)

    def seeded_sheet(self):
        d = self.device
        seeds = {'CF_AUTH_TEST_SIGNED_IN': 'demo', 'CF_AUTH_TEST_WALKTHROUGH_SEEN': '1',
                 'CF_PLAN_TEST_FREEZE_CLOCK': '1', 'CF_PLAN_TEST_ROUTE': 'trip:' + TRIP, 'CF_MOCK_DELAY': '0'}
        d.launch(seeds)
        xml = d.wait_tag('trip.scroll', 20)
        day = anchor_from_dump(xml, require_sheet=False)
        templates = json.loads((ROOT / 'core/testing/src/main/resources/fixtures/mock-seed.json').read_text())['trips']
        template = next(t for t in templates if t['card']['id'] == TRIP)
        d.launch({**seeds, 'CF_PLAN_TEST_SHEET': 'step:' + block_for_day(template, day)})
        xml = d.wait_tag('step.sheet', 20)
        validate(xml, day)
        return day

    def run(self):
        d = self.device
        self.out.mkdir(parents=True, exist_ok=True)
        result = {'device': d.serial, 'status': 'failed', 'profiles': []}
        try:
            dpi = density(d.command('shell', 'wm', 'density').stdout)
            day = self.seeded_sheet()
            identity, events = self.identity(), self.events()
            for name, width, height in PROFILES:
                size = str(pixels(width, dpi)) + 'x' + str(pixels(height, dpi))
                d.command('shell', 'wm', 'size', size)
                last = None

                def settled(remaining):
                    nonlocal last
                    xml = d.dump(timeout=min(15, remaining))
                    validate(xml, day)
                    current = {key: value.get('bounds') for key, value in nodes(xml).items()}
                    stable = last == current
                    last = current
                    return xml if stable else None

                xml = d.wait(settled, 20, 'resize did not settle: ' + name)
                if self.identity() != identity:
                    raise DeviceFailure('MainActivity task/record/process changed: ' + name)
                if self.events() - events:
                    raise DeviceFailure('MainActivity relaunched: ' + name)
                (self.out / (name + '.xml')).write_text(xml)
                d.screenshot(self.out / (name + '.png'))
                result['profiles'].append({'name': name, 'dp': [width, height], 'px': size, 'status': 'passed'})
            result.update(status='passed', density=dpi, openingDay=day)
        except (DeviceFailure, ValueError, ET.ParseError, OSError, StopIteration) as error:
            result['reason'] = str(error)
        finally:
            try:
                d.command('shell', 'wm', 'size', 'reset')
                result['sizeReset'] = True
            except DeviceFailure:
                result.update(status='failed', reason='wm size reset failed', sizeReset=False)
            finally:
                (self.out / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
        return result


def main():
    d = Device(serial='emulator-5556')
    d.require()
    name = d.command('emu', 'avd', 'name').stdout.splitlines()
    if not name or name[0].strip() != 'fold':
        raise DeviceFailure('boot fold first: Scripts/emulator.sh ensure fold')
    result = ResizeCheck(d, ROOT / 'build/captures/resize').run()
    print('resize-check: ' + result['status'])
    return int(result['status'] != 'passed')


if __name__ == '__main__':
    raise SystemExit(main())
