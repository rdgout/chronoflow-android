"""Classify StrictMode stack blocks from threadtime logcat; app frames determine ownership."""
import argparse
from collections import Counter
import json
from pathlib import Path
import re

LINE = re.compile(r'^\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d+\s+(\d+)\s+(\d+)\s+[VDIWEAF]\s+StrictMode\s*:\s*(.*)$')
TYPE = re.compile(r'\b(\w+Violation)\b')


def scan(text):
    active, blocks = {}, []
    for line in text.splitlines():
        match = LINE.match(line)
        if not match:
            continue
        pid, tid, body = match.groups()
        key = (pid, tid)
        header = 'StrictMode policy violation' in body or body.startswith('android.os.strictmode.')
        if header:
            if key in active:
                blocks.append(active.pop(key))
            active[key] = {'header': body, 'frames': []}
        elif key in active:
            if body.startswith('at '):
                active[key]['frames'].append(body[3:].strip())
            elif body.startswith('Caused by:'):
                active[key]['header'] += ' ' + body
            elif body.strip():
                blocks.append(active.pop(key))
    blocks.extend(active.values())
    counts = {'ours': Counter(), 'thirdParty': Counter()}
    for block in blocks:
        kind = TYPE.search(block['header'])
        kind = kind.group(1) if kind else 'UnknownViolation'
        ours = next((frame for frame in block['frames'] if frame.startswith('net.chronoflow.')), None)
        external = next((frame for frame in block['frames'] if frame.startswith(('androidx.', 'com.google.'))), None)
        first = block['frames'][0] if block['frames'] else '(no stack frame)'
        counts['ours' if ours else 'thirdParty'][(kind, ours or external or first)] += 1
    return {group: [{'type': kind, 'top': top, 'count': count}
                    for (kind, top), count in sorted(values.items())] for group, values in counts.items()}


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument('log', type=Path)
    parser.add_argument('--out', type=Path, default=Path('build/audit/strictmode.json'))
    args = parser.parse_args(argv)
    result = scan(args.log.read_text(errors='replace'))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, indent=2) + '\n')
    print('StrictMode: ours=' + str(sum(r['count'] for r in result['ours'])) +
          ' thirdParty=' + str(sum(r['count'] for r in result['thirdParty'])))
    return int(bool(result['ours']))


if __name__ == '__main__':
    raise SystemExit(main())
