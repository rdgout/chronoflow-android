#!/bin/bash
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
RUNS="${1:-3}"
now() { python3 -I -c 'import time; print(time.time())'; }

make emu
make quick >/dev/null
make check >/dev/null

mkdir -p build
for tier in quick check; do
  : >"build/.tier-$tier.times"
  for _ in $(seq 1 "$RUNS"); do
    start="$(now)"
    make "$tier" >/dev/null
    end="$(now)"
    python3 -I -c "print('%.2f' % ($end - $start))" >>"build/.tier-$tier.times"
  done
done

CPU="$(sysctl -n machdep.cpu.brand_string)"
MEM="$(sysctl -n hw.memsize)"
COMMIT="$(git rev-parse --short HEAD)"
DATE="$(date +%Y-%m-%d)"
python3 -I - "$CPU" "$MEM" "$COMMIT" "$DATE" <<'PY'
import json
import statistics
import sys

cpu, mem, commit, date = sys.argv[1:5]
runs = {t: [float(x) for x in open("build/.tier-%s.times" % t).read().split()] for t in ("quick", "check")}
median = {t: round(statistics.median(v), 2) for t, v in runs.items()}
budget = {"quick": 60, "check": 300}
data = {
    "date": date,
    "commit": commit,
    "host": {"cpu": cpu, "memGiB": round(int(mem) / 2**30)},
    "emulator": "pixel (Pixel 8, API 36)",
    "runs": runs,
    "median": median,
    "budget": budget,
}
with open("build/tier-timings.json", "w") as handle:
    json.dump(data, handle, indent=2)
    handle.write("\n")
print(json.dumps(data, indent=2))
over = [t for t in ("quick", "check") if median[t] > budget[t]]
if over:
    print("measure-tiers: median over budget for: " + ", ".join(over), file=sys.stderr)
    sys.exit(1)
PY
