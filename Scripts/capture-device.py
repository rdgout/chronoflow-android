"""Runs the opt-in device capture hosts; the caller holds the host-wide device lock."""
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def capture_modules(only):
    if not only:
        return ['core/ui', 'app']
    app_prefixes = ('SplashScreen', 'ItemDetail', 'PlanLegend', 'SheetStates', 'TripActions', 'PickerDateTime', 'material')
    selected = {'app' if token.strip().startswith(app_prefixes) else 'core/ui' for token in only.split(',')}
    return [module for module in ['core/ui', 'app'] if module in selected]


def capture_relative(relative):
    parts = relative.parts
    start = next((index for index, part in enumerate(parts) if part in ('designsystem', 'material')), len(parts) - 1)
    return Path(*parts[start:])


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
    if not only or any(token.strip().startswith("material") for token in only.split(",")):
        (target / "material/timings.jsonl").unlink(missing_ok=True)
    modules = capture_modules(only)
    outputs = [ROOT / module / 'build/outputs/connected_android_test_additional_output' for module in modules]
    for output in outputs:
        shutil.rmtree(output, ignore_errors=True)
    command = ['./gradlew', '--console=plain'] + [':' + module.replace('/', ':') + ':connectedDebugAndroidTest' for module in modules]
    command += ['-Pandroid.testInstrumentationRunnerArguments.annotation=net.chronoflow.core.testing.tiers.Capture',
                '-Pandroid.testInstrumentationRunnerArguments.timeout_msec=60000']
    if only.startswith('PickerDateTime'):
        command.append('-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.app.wizard.PickerDateTimeDeviceCaptureTest')
    if only.startswith(('ItemDetail', 'PlanLegend', 'SheetStates', 'TripActions')):
        command.append('-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.app.trip.TripSheetsCaptureTest')
    if only.startswith('material'):
        command.append('-Pandroid.testInstrumentationRunnerArguments.class=net.chronoflow.app.audit.MaterialChromeCaptureTest')
    if only:
        command.append('-Pandroid.testInstrumentationRunnerArguments.cf.captureOnly=' + only)
    code = subprocess.run(command, cwd=ROOT).returncode
    fresh_pngs = 0
    for output in outputs:
        for source in output.rglob('*'):
            if source.is_file() and (source.suffix == '.png' or source.name == 'timings.jsonl'):
                destination = target / capture_relative(source.relative_to(output))
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, destination)
                fresh_pngs += int(source.suffix == ".png")
    if code:
        return code
    count = len(list(target.rglob('*.png')))
    print(f'captures: {count} PNGs in build/captures/device')
    if fresh_pngs == 0:
        print("captures: no fresh PNG evidence", file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else ''))
