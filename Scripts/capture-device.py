"""Runs the isolated primitive capture host; the caller holds the host-wide device lock."""
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def main(only):
    sdk = Path(os.environ.get('ANDROID_HOME', '/opt/homebrew/share/android-commandlinetools'))
    adb = str(sdk / 'platform-tools/adb')
    serial = os.environ.get('ANDROID_SERIAL', 'emulator-5554')
    result = subprocess.run([adb, '-s', serial, 'get-state'], capture_output=True, text=True)
    if result.returncode or result.stdout.strip() != 'device':
        print('captures: no booted device; run Scripts/emulator.sh ensure', file=sys.stderr)
        return 1
    target = ROOT / 'build/captures/device'
    subprocess.run([sys.executable, '-I', str(ROOT / 'Scripts/prepare-captures.py'), str(target),
                    '--only', only, '--append', '1' if os.environ.get('APPEND') == '1' else '0'], check=True)
    trip_sheets = only.startswith(('ItemDetail', 'PlanLegend', 'SheetStates', 'TripActions'))
    wizard_pickers = only.startswith('PickerDateTime')
    module = 'app' if only.startswith('SplashScreen') or trip_sheets or wizard_pickers else 'core/ui'
    output = ROOT / module / 'build/outputs/connected_android_test_additional_output'
    shutil.rmtree(output, ignore_errors=True)
    command = ['./gradlew', '--console=plain', ':' + module.replace('/', ':') + ':connectedDebugAndroidTest',
               '-Pandroid.testInstrumentationRunnerArguments.annotation=net.chronoflow.core.testing.tiers.Capture',
               '-Pandroid.testInstrumentationRunnerArguments.timeout_msec=60000']
    if wizard_pickers:
        command.append('-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.app.wizard.PickerDateTimeDeviceCaptureTest')
    if trip_sheets:
        command.append('-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.app.trip.TripSheetsCaptureTest')
    if only:
        command.append('-Pandroid.testInstrumentationRunnerArguments.cf.captureOnly=' + only)
    code = subprocess.run(command, cwd=ROOT).returncode
    if code:
        return code
    for source in output.rglob('*.png'):
        # AGP adds variant/device folders before the caller's capture name.
        relative = source.relative_to(output)
        parts = relative.parts
        start = parts.index('designsystem') if 'designsystem' in parts else len(parts) - 1
        destination = target.joinpath(*parts[start:])
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
    count = len(list(target.rglob('*.png')))
    print(f'captures: {count} PNGs in build/captures/device')
    return int(count == 0 and not only)


if __name__ == '__main__':
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else ''))
