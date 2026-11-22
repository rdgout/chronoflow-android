"""Unseeded mock process-death audit; caller holds the device lock."""
import argparse
import datetime
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_device import Device, DeviceFailure, ROOT, PACKAGE, ACTIVITY, bounds, nodes


class DemoRejected(RuntimeError):
    pass


def tap_node(device, node):
    left, top, right, bottom = bounds(node.get('bounds', ''))
    if right <= left or bottom <= top:
        raise DeviceFailure('control has no visible bounds')
    device.command('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))


def stopped(text):
    blocks = re.split(r'(?=ActivityRecord\{)', text)
    return any(ACTIVITY in block and re.search(r'(?:mStopped=true|state=STOPPED)', block)
               for block in blocks)


class ProcessDeathSweep:
    def __init__(self, device, captures, report):
        self.device, self.captures, self.report = device, Path(captures), Path(report)

    def launcher(self):
        self.device.command('shell', 'monkey', '--pct-syskeys', '0', '-p', PACKAGE,
                            '-c', 'android.intent.category.LAUNCHER', '1')

    def tag(self, tag, timeout=15):
        if not tag.endswith('*'):
            xml = self.device.wait_tag(tag, timeout)
            return nodes(xml)[tag]

        def resolve(remaining):
            found = nodes(self.device.dump(timeout=min(15, remaining)))
            node = next((node for key, node in found.items() if key.startswith(tag[:-1])
                         and node.get('clickable') == 'true' and node.get('enabled') == 'true'), None)
            return (node,) if node is not None else None
        # Trip block identifiers depend on the current mock plan; the prefix is structural, not copy.
        return self.device.wait(resolve, timeout, 'no visible variable trip block')[0]

    def hide_ime(self):
        d = self.device
        if 'mInputShown=true' in d.command('shell', 'dumpsys', 'input_method').stdout:
            d.command('shell', 'input', 'keyevent', '4')
            d.wait(lambda remaining: 'mInputShown=true' not in
                   d.command('shell', 'dumpsys', 'input_method', timeout=min(5, remaining)).stdout,
                   5, 'IME did not settle')

    def steps(self, steps):
        for step in steps:
            if 'wait' in step:
                self.tag(step['wait'])
            elif 'tap' in step:
                self.hide_ime()
                tap_node(self.device, self.tag(step['tap']))
            elif 'text' in step:
                tap_node(self.device, self.tag(step['text']))
                # Device.command redacts adb failures; form values never enter the report or a command log.
                self.device.command('shell', 'input', 'text', step['value'].replace(' ', '%s'))
            else:
                raise ValueError('unknown process-death step kind')

    def fresh(self, signed_in):
        d = self.device
        d.command('shell', 'pm', 'clear', PACKAGE)
        # Native calendar dates use the app's explicit en-US locale, not translated app copy.
        d.command('shell', 'cmd', 'locale', 'set-app-locales', PACKAGE, '--locales', 'en-US')
        self.launcher()
        d.wait(lambda remaining: 'launch.opening' not in nodes(d.dump(timeout=min(15, remaining))),
               15, 'initial overlay did not close')
        d.wait_tag('welcome.plan', 15)
        if signed_in:
            self.steps([{'tap': 'welcome.signIn'}, {'text': 'sign-in.email', 'value': 'demo@example.com'},
                        {'text': 'sign-in.password', 'value': 'password'}, {'tap': 'sign-in.submit'}])

            def signed(remaining):
                found = nodes(d.dump(timeout=min(15, remaining)))
                if 'sign-in.banner' in found:
                    raise DemoRejected('unseeded mock rejected the demo account (OQ-14-3)')
                return found if 'today.screen' in found or 'walkthrough.skip' in found else None
            try:
                found = d.wait(signed, 15, 'unseeded demo sign-in did not reach the shell')
            except DeviceFailure as error:
                # Device.wait treats DeviceFailure as a transient transport error; recheck the form once.
                if 'sign-in.banner' in nodes(d.dump()):
                    raise DemoRejected('unseeded mock rejected the demo account (OQ-14-3)') from None
                raise error
            if 'walkthrough.skip' in found:
                self.steps([{'tap': 'walkthrough.skip'}])
            d.wait_tag('today.screen', 15)

    def kill_and_relaunch(self):
        d = self.device
        self.hide_ime()
        d.command('shell', 'input', 'keyevent', '3')
        d.wait(lambda remaining: stopped(d.command('shell', 'dumpsys', 'activity', 'activities',
                                                 timeout=min(5, remaining)).stdout),
               10, 'app did not enter stopped state')
        d.command('shell', 'am', 'kill', PACKAGE)
        d.wait(lambda remaining: not d.command('shell', 'pidof', PACKAGE, check=False,
                                               timeout=min(5, remaining)).stdout.strip(),
               10, 'process is still alive after am kill')
        self.launcher()
        try:
            d.wait_tag('launch.opening', 3)
        except DeviceFailure:
            raise DeviceFailure('not a cold launch: launch.opening was not observed within 3 s') from None

    def native_calendar(self, date):
        d = self.device
        expected = date.strftime('%A, %B ') + str(date.day) + date.strftime(', %Y')
        for attempt in range(3):
            xml = d.wait_tag('wizard.date-dialog')
            matches = [n for n in ET.fromstring(xml).iter('node') if n.get('text') == expected
                       and n.get('enabled') == 'true' and n.get('clickable') == 'true']
            if len(matches) == 1:
                tap_node(d, matches[0])
                self.steps([{'tap': 'wizard.picker.done'}])
                return
            if attempt < 2:
                label = d.command('shell', 'cmd', 'overlay', 'lookup', PACKAGE,
                                  PACKAGE + ':string/m3c_date_picker_switch_to_next_month').stdout.strip()
                matches = [n for n in ET.fromstring(xml).iter('node')
                           if label and n.get('content-desc') == label and n.get('clickable') == 'true']
                if len(matches) != 1:
                    raise DeviceFailure('native next-month control is unavailable')
                tap_node(d, matches[0])
        raise DeviceFailure('native calendar did not expose the requested future date')

    def guest_sleep(self):
        d = self.device
        today = datetime.date.fromisoformat(d.command('shell', 'date', '+%Y-%m-%d').stdout.strip())
        for field, days in (('departure', 2), ('arrival', 4)):
            self.steps([{'tap': 'wizard.0.' + field + '.date'}])
            self.native_calendar(today + datetime.timedelta(days=days))
            self.steps([{'tap': 'wizard.0.' + field + '.time'}, {'wait': 'wizard.time-dialog'},
                        {'tap': 'wizard.picker.done'}])
        self.steps([{'tap': 'wizard.outbound.continue'}, {'wait': 'wizard.sleep.continue'}])

    def case(self, case):
        d = self.device
        started = d.now()
        row = {'name': case['name'], 'status': 'failed'}
        try:
            self.fresh(case.get('signedIn', False))
            self.steps(case['steps'])
            if case.get('prepareGuestSleep'):
                self.guest_sleep()
            self.hide_ime()
            d.screenshot(self.captures / (case['name'] + '_before.png'))
            self.kill_and_relaunch()
            for tag in case['expectAfter']:
                d.wait_tag(tag, 15)
            xml = d.dump()
            found = nodes(xml)
            for tag in case.get('expectAbsent', []):
                if tag in found:
                    raise DeviceFailure('unexpected restored node: ' + tag)
            if case.get('emptyEmail'):
                self.steps([{'tap': 'welcome.signIn'}])
                email = self.tag('sign-in.email')
                if email.get('text', ''):
                    raise DeviceFailure('sign-in email was restored after process death')
            d.screenshot(self.captures / (case['name'] + '_after.png'))
            self.steps(case.get('inspectSteps', []))
            if case.get('airports'):
                for tag, airport in case['airports'].items():
                    node = self.tag(tag)
                    data = ' '.join(n.get('text', '') + ' ' + n.get('content-desc', '') for n in node.iter())
                    if not re.search(r'\b' + re.escape(airport) + r'\b', data):
                        raise DeviceFailure('restored airport identifier differs at ' + tag)
            self.steps(case.get('finishSteps', []))
            if case.get('airports'):
                row['airportsVerified'] = list(case['airports'].values())
            row['status'] = 'passed'
            row['coldLaunchObserved'] = True
        except DemoRejected:
            raise
        except (DeviceFailure, ValueError, ET.ParseError, OSError) as error:
            row['reason'] = str(error)
        row['wallSeconds'] = round(d.now() - started, 3)
        return row

    def run(self, cases, preserve=False):
        d = self.device
        names = ('animator_duration_scale', 'transition_animation_scale', 'window_animation_scale')
        previous = {name: d.command('shell', 'settings', 'get', 'global', name).stdout.strip() for name in names}
        try:
            # Functional prep sets 0, which reduces the real launch overlay to 300 ms, shorter than a shell dump.
            for name in names:
                d.command('shell', 'settings', 'put', 'global', name, '1')
            return self.run_cases(cases, preserve)
        finally:
            failed = []
            for name, value in previous.items():
                try:
                    d.command('shell', 'settings', 'put', 'global', name, value)
                except DeviceFailure:
                    failed.append(name)
            if failed:
                raise DeviceFailure('could not restore animator settings: ' + ', '.join(failed))

    def run_cases(self, cases, preserve=False):
        previous = json.loads(self.report.read_text()).get('cases', []) if preserve and self.report.exists() else []
        attempted = {case['name'] for case in cases}
        previous = [row for row in previous if row['name'] not in attempted]
        self.captures.mkdir(parents=True, exist_ok=True)
        self.report.parent.mkdir(parents=True, exist_ok=True)
        rows = []
        for case in cases:
            try:
                row = self.case(case)
            except DemoRejected as error:
                rows.append({'name': case['name'], 'status': 'failed', 'reason': str(error)})
                self.report.write_text(json.dumps({'cases': previous + rows, 'stopped': 'OQ-14-3'}, indent=2) + '\n')
                raise
            rows.append(row)
            self.report.write_text(json.dumps({'device': self.device.serial, 'cases': previous + rows}, indent=2) + '\n')
            print('process-death: ' + row['name'] + ' ' + row['status'], flush=True)
        return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--only', help='comma-separated case names for a focused fix run')
    args = parser.parse_args()
    d = Device()
    d.require()
    cases = json.loads((ROOT / 'tools/audit/process-death.json').read_text())['cases']
    if args.only:
        selected = set(args.only.split(','))
        if selected - {case['name'] for case in cases}:
            parser.error('unknown process-death case')
        cases = [case for case in cases if case['name'] in selected]
    rows = ProcessDeathSweep(d, ROOT / 'build/captures/process-death', ROOT / 'build/audit/process-death.json').run(
        cases, preserve=bool(args.only))
    return int(any(row['status'] != 'passed' for row in rows))


if __name__ == '__main__':
    raise SystemExit(main())
