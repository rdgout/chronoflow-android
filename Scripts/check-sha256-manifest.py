"""Verifies a sha256 manifest: `<sha256>  <ios path>  <local path>` per line, `-` when n/a."""

import hashlib
import sys
from pathlib import Path


def check(manifest: Path, root: Path) -> list[str]:
    problems = []
    for line in manifest.read_text().splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        fields = line.split("  ")
        if len(fields) != 3:
            problems.append(f"malformed manifest line: {line}")
            continue
        digest, _, local = fields
        if local == "-":
            continue
        path = root / local
        if not path.is_file():
            problems.append(f"missing: {local}")
        elif hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            problems.append(f"hash mismatch: {local}")
    return problems


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("usage: check-sha256-manifest.py <manifest>", file=sys.stderr)
        return 2
    root = Path(__file__).resolve().parent.parent
    problems = check(Path(argv[1]), root)
    for problem in problems:
        print(problem, file=sys.stderr)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
