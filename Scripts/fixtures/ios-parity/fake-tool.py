"""Executable audit fixture; records argv and produces tiny deterministic artifacts."""
import base64
import json
import os
from pathlib import Path
import sys

PNG = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg==')
tool, *args = sys.argv[1:]
with open(os.environ['CF_FAKE_LOG'], 'a') as log:
    log.write(json.dumps([tool, *args]) + '\n')
if tool == 'ffmpeg':
    if args[-1] != '-':
        Path(args[-1]).write_bytes(PNG)
    raise SystemExit(0)
args = args[2:]
if args == ['get-state']:
    print('device')
elif args[:2] == ['exec-out', 'cat']:
    print(os.environ.get('CF_FAKE_XML', '<hierarchy><node resource-id="today.screen" bounds="[0,0][100,100]"/></hierarchy>'))
elif args[:2] == ['exec-out', 'screencap']:
    sys.stdout.buffer.write(PNG)
elif args[:3] == ['shell', 'cat', '/proc/uptime']:
    print('10.0 10.0')
elif args[:4] == ['shell', 'settings', 'get', 'global']:
    print('0')
elif args[:3] == ['shell', 'pidof', 'screenrecord']:
    print('42')
elif args and args[0] == 'pull':
    Path(args[-1]).write_bytes(b'fake-video')
