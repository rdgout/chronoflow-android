"""Audit the root font cap on pixel; caller holds the device lock."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_device import Device, DeviceFailure, ROOT, PACKAGE, bounds


def geometry(xml):
    tagged, text = {}, {}
    app_nodes = (node for node in ET.fromstring(xml).iter('node') if node.get('package', PACKAGE) == PACKAGE)
    for index, node in enumerate(app_nodes):
        raw = node.get('bounds')
        if not raw:
            continue
        box = bounds(raw)
        tag = node.get('resource-id', '')
        if tag:
            tagged[tag] = box
        if node.get('class') in ('android.widget.TextView', 'android.widget.EditText'):
            # Match structural positions, never compare the displayed copy.
            text[(tag, index)] = box
    return tagged, text


def compare(dumps):
    small, capped, large = (geometry(dumps[s]) for s in ('1.0', '1.3', '2.0'))
    if not capped[0]:
        raise DeviceFailure('font-scale dump contains no app test tags')
    for tag in sorted(capped[0].keys() | large[0].keys()):
        if capped[0].get(tag) != large[0].get(tag):
            raise DeviceFailure('font cap bounds differ for tag ' + tag)
    if not capped[1] or small[1] == capped[1]:
        raise DeviceFailure('scale not applied: 1.0 and 1.3 text bounds are identical')
    return len(capped[0])


class FontScaleAudit:
    def __init__(self, device, captures, report):
        self.device, self.captures, self.report = device, Path(captures), Path(report)

    def set_scale(self, value):
        d = self.device
        d.command('shell', 'settings', 'put', 'system', 'font_scale', value)
        d.wait(lambda remaining: d.command('shell', 'settings', 'get', 'system', 'font_scale',
                                          timeout=min(5, remaining)).stdout.strip() == value,
               5, 'font_scale did not echo ' + value)

    def capture(self, screen, scale):
        d = self.device
        d.launch(screen['seeds'], extras={'cf.settings.language': 'nl', 'cf.reduceMotion': '1'})
        xml = d.wait_tag(screen['wait'], 20)
        previous = None

        def stable(remaining):
            nonlocal previous, xml
            xml = d.dump(timeout=min(15, remaining))
            current = geometry(xml)
            ready = previous == current
            previous = current
            return ready

        d.wait(stable, 10, 'font-scale geometry did not settle: ' + screen['name'])
        target = self.captures / (screen['name'] + '_' + scale)
        # Keep the decimal scale in the filename (Path.with_suffix would remove it).
        Path(str(target) + '.xml').write_text(xml)
        d.screenshot(Path(str(target) + '.png'))
        return xml

    def run(self, screens):
        self.captures.mkdir(parents=True, exist_ok=True)
        self.report.parent.mkdir(parents=True, exist_ok=True)
        dumps, failures, started = {}, {}, self.device.now()
        try:
            for scale in ('1.0', '1.3', '2.0'):
                self.set_scale(scale)
                for screen in screens:
                    name = screen['name']
                    try:
                        dumps.setdefault(name, {})[scale] = self.capture(screen, scale)
                    except (DeviceFailure, ValueError, ET.ParseError, OSError) as error:
                        failures[name] = str(error)
                    print('font-scale: ' + name + ' ' + scale, flush=True)
        finally:
            self.set_scale('1.0')
        rows = []
        for screen in screens:
            name = screen['name']
            try:
                if name in failures:
                    raise DeviceFailure(failures[name])
                count = compare(dumps.get(name, {}))
                rows.append({'name': name, 'status': 'passed', 'taggedNodes': count})
            except (DeviceFailure, KeyError) as error:
                rows.append({'name': name, 'status': 'failed', 'reason': str(error)})
        result = {'device': self.device.serial, 'wallSeconds': round(self.device.now() - started, 3),
                  'scales': [1.0, 1.3, 2.0], 'restoredScale': 1.0, 'screens': rows}
        self.report.write_text(json.dumps(result, indent=2) + '\n')
        return result


def main():
    device = Device()
    device.require()
    screens = json.loads((ROOT / 'tools/audit/font-scale.json').read_text())['screens']
    result = FontScaleAudit(device, ROOT / 'build/captures/font-scale', ROOT / 'build/audit/font-scale.json').run(screens)
    return int(any(row['status'] != 'passed' for row in result['screens']))


if __name__ == '__main__':
    raise SystemExit(main())
