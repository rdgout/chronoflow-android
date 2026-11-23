"""Capture Android twins of the pinned iOS references; caller holds the device lock."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_device import Device, DeviceFailure, PACKAGE, ROOT


def pair_command(ios, android, output, ffmpeg="ffmpeg"):
    return [ffmpeg, "-y", "-loglevel", "error", "-i", str(ios), "-i", str(android),
            "-filter_complex", "[0:v]scale=-2:1600[a];[1:v]scale=-2:1600[b];[a][b]hstack=inputs=2",
            "-frames:v", "1", str(output)]


def frame_command(video, moment, output, ffmpeg="ffmpeg"):
    # VFR screenrecord holds its final frame until the container duration.
    return [ffmpeg, "-y", "-loglevel", "error", "-i", str(video),
            "-vf", "tpad=stop_mode=clone:stop_duration=8", "-ss", str(moment),
            "-frames:v", "1", str(output)]


def recording_duration(log, recorded_span=0):
    match = re.search(r"Duration: (\d+):(\d+):(\d+(?:\.\d+)?)", log)
    if not match:
        raise DeviceFailure("recording duration is unavailable")
    hours, minutes, seconds = map(float, match.groups())
    return max(hours * 3600 + minutes * 60 + seconds, recorded_span)


def first_nonblack_offset(log):
    for start, end in re.findall(r"black_start:([\d.]+)\s+black_end:([\d.]+)", log):
        if float(start) <= 0.05:
            return float(end), "frames relative to first non-black frame"
    return 0.0, "alignment fallback: frames relative to recording start (no initial black interval)"


class Parity:
    def __init__(self, device, out, refs, ffmpeg="ffmpeg", run=subprocess.run, popen=subprocess.Popen):
        self.device, self.out, self.refs = device, Path(out), Path(refs)
        self.ffmpeg, self.run, self.popen = ffmpeg, run, popen
        self.videos = {}

    def ff(self, command):
        result = self.run(command, capture_output=True, text=True, timeout=45)
        if result.returncode:
            raise DeviceFailure("ffmpeg operation failed (exit " + str(result.returncode) + ")")
        return result

    def record(self, entry):
        d = self.device
        key = json.dumps([entry["seeds"], entry["extras"], entry["profile"], entry.get("fresh", False)], sort_keys=True)
        if key in self.videos:
            return self.videos[key]
        index = len(list((self.out / "recordings").glob("*.mp4")))
        video = self.out / "recordings" / (str(index) + ".mp4")
        video.parent.mkdir(parents=True, exist_ok=True)
        remote = "/sdcard/cf-ios-parity.mp4"
        old_scales = {}
        recorder = None
        try:
            if entry.get("fresh"):
                d.command("shell", "pm", "clear", PACKAGE)
                # The debug preference extra is also supplied as the API-independent fallback.
                d.command("shell", "cmd", "locale", "set-app-locales", PACKAGE, "--locales", "nl-NL", check=False)
            for setting in ("animator_duration_scale", "transition_animation_scale", "window_animation_scale"):
                old_scales[setting] = d.command("shell", "settings", "get", "global", setting).stdout.strip()
                d.command("shell", "settings", "put", "global", setting, "1")
            d.command("shell", "am", "force-stop", PACKAGE)
            recording_started = d.now()
            recorder = self.popen([d.adb, "-s", d.serial, "shell", "screenrecord", "--time-limit", "8", remote],
                                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            d.wait(lambda remaining: d.command("shell", "pidof", "screenrecord", timeout=min(2, remaining),
                                               check=False).stdout.strip(), 3, "screenrecord did not start")
            d.launch(entry["seeds"], entry["profile"], entry["extras"])
            if recorder.wait(timeout=15):
                raise DeviceFailure("screenrecord failed")
            recording_span = d.now() - recording_started
            d.command("pull", remote, str(video))
        finally:
            cleanup_failures = []
            if recorder is not None and recorder.poll() is None:
                try:
                    recorder.terminate()
                    try:
                        recorder.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        recorder.kill()
                        recorder.wait(timeout=5)
                    d.command("shell", "pkill", "-INT", "screenrecord", check=False)
                except (OSError, subprocess.TimeoutExpired, DeviceFailure):
                    cleanup_failures.append("screenrecord")
            for setting, value in old_scales.items():
                try:
                    if value == "null":
                        d.command("shell", "settings", "delete", "global", setting)
                    else:
                        d.command("shell", "settings", "put", "global", setting, value)
                except (OSError, DeviceFailure):
                    cleanup_failures.append(setting)
            if cleanup_failures:
                raise DeviceFailure("could not restore device state: " + ", ".join(cleanup_failures))
        result = self.ff([self.ffmpeg, "-hide_banner", "-i", str(video), "-vf",
                          "blackdetect=d=0.02:pix_th=0.02", "-an", "-f", "null", "-"])
        offset, note = first_nonblack_offset(result.stderr)
        # screenrecord omits unchanged trailing frames and can shorten the MP4 duration.
        # Its successful, bounded recording interval still contains those held frames.
        note += "; recording wall span " + str(round(recording_span, 3)) + " s"
        self.videos[key] = video, offset, note, recording_duration(result.stderr, recording_span)
        return self.videos[key]

    def entry(self, entry):
        name = entry["ios"]
        if name != Path(name).name or (name != "launcher-icon" and not name.endswith(".png")):
            raise ValueError("invalid iOS reference name")
        if entry.get("skip"):
            return {"ios": name, "status": "skipped", "note": entry["skip"]}
        output = self.out / (name if name.endswith(".png") else name + ".png")
        pair = self.out / "pairs" / output.name
        # A failed refresh must not leave an earlier PNG appearing to be new evidence.
        output.unlink(missing_ok=True)
        pair.unlink(missing_ok=True)
        note = entry.get("note", "")
        if name == "launcher-icon":
            self.device.command("shell", "input", "keyevent", "HOME")
            self.device.wait_tag("workspace", timeout=5)
            self.device.screenshot(output)
            return {"ios": name, "status": "captured", "android": str(output),
                    "pair": None, "note": "Uncropped launcher; inspect icon visibility/mask separately."}
        if not (self.refs / name).is_file():
            raise ValueError("iOS reference is missing: " + name)
        if entry.get("frames"):
            video, offset, alignment, duration = self.record(entry)
            moment = entry["frames"][0] / 1000 + offset
            if moment >= duration:
                raise DeviceFailure("requested frame is outside the recording")
            self.ff(frame_command(video, moment, output, self.ffmpeg))
            note = (note + "; " + alignment).strip("; ")
        else:
            started = self.device.uptime()
            self.device.launch(entry["seeds"], entry["profile"], entry["extras"])
            xml = self.device.wait_tag(entry["wait"], timeout=20)
            self.out.joinpath(Path(name).stem + ".xml").write_text(xml)
            settled = entry.get("settleUptimeSeconds", 0)
            if settled:
                self.device.wait(lambda _: self.device.uptime() >= started + settled,
                                 10, "device clock did not reach settled Welcome")
            self.device.screenshot(output)
        if not output.is_file() or output.stat().st_size < 24:
            raise DeviceFailure("capture produced no image")
        pair.parent.mkdir(parents=True, exist_ok=True)
        self.ff(pair_command(self.refs / name, output, pair, self.ffmpeg))
        if not pair.is_file() or pair.stat().st_size < 24:
            raise DeviceFailure("pair produced no image")
        return {"ios": name, "status": "captured", "android": str(output), "pair": str(pair), "note": note}

    def sweep(self, entries, preserve=False):
        self.out.mkdir(parents=True, exist_ok=True)
        result_file = self.out / "results.json"
        previous = json.loads(result_file.read_text()) if preserve and result_file.exists() else {}
        if previous and previous.get("serial") != self.device.serial:
            raise ValueError("cannot combine evidence from different devices")
        results = previous.get("entries", [])
        attempted = []
        for entry in entries:
            started = time.monotonic()
            try:
                row = self.entry(entry)
            except (DeviceFailure, OSError, ValueError, subprocess.TimeoutExpired) as error:
                row = {"ios": entry["ios"], "status": "failed",
                       "note": str(error) if isinstance(error, (DeviceFailure, ValueError)) else type(error).__name__}
            row["wallSeconds"] = round(time.monotonic() - started, 3)
            results = [old for old in results if old["ios"] != row["ios"]]
            results.append(row)
            attempted.append(row)
            (self.out / "results.json").write_text(json.dumps({"serial": self.device.serial, "entries": results}, indent=2) + "\n")
            print("ios-parity:", row["ios"], row["status"], row["wallSeconds"], "s", flush=True)
        return attempted


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=ROOT / "tools/audit/ios-parity.json")
    parser.add_argument("--out", type=Path, default=ROOT / "build/audit/ios-parity")
    parser.add_argument("--refs", type=Path, default=ROOT / "docs/research/screens/ios")
    parser.add_argument("--only", default="")
    args = parser.parse_args()
    d = Device()
    try:
        d.require()
    except DeviceFailure as error:
        print("ios-parity:", error, file=sys.stderr)
        return 2
    if not shutil.which("ffmpeg"):
        print("ios-parity: ffmpeg is required", file=sys.stderr)
        return 2
    entries = json.loads(args.config.read_text())["entries"]
    if args.only:
        entries = [e for e in entries if e["ios"].startswith(tuple(args.only.split(",")))]
        if not entries:
            parser.error("--only matched no references")
    results = Parity(d, args.out, args.refs).sweep(entries, preserve=bool(args.only))
    return int(any(row["status"] == "failed" for row in results))


if __name__ == "__main__":
    raise SystemExit(main())
