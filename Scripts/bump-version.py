#!/usr/bin/env python3
"""Bump the four-part Android upload version without third-party dependencies."""
import argparse
import sys
from pathlib import Path

KEYS = ("versionMajor", "versionMinor", "versionPatch", "versionBuild")
LIMITS = (2099, 99, 99, 99)


def read_version(path):
    properties = {}
    for line in path.read_text().splitlines():
        if "=" in line and not line.lstrip().startswith(("#", "!")):
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip()
    values = []
    for key, limit in zip(KEYS, LIMITS):
        try:
            value = int(properties[key])
        except KeyError:
            raise ValueError(f"{key} is missing") from None
        except ValueError:
            raise ValueError(f"{key} must be an integer") from None
        if not 0 <= value <= limit:
            raise ValueError(f"{key} must be in 0..{limit}")
        values.append(value)
    return values


def code(values):
    major, minor, patch, build = values
    return major * 1_000_000 + minor * 10_000 + patch * 100 + build


def label(values):
    return f"{values[0]}.{values[1]}.{values[2]} ({code(values)})"


def bump(values, part):
    index = ("major", "minor", "patch", "build").index(part)
    if values[index] == LIMITS[index]:
        message = f"{KEYS[index]} is {LIMITS[index]}"
        if index:
            message += f": bump {('major', 'minor', 'patch', 'build')[index - 1]}"
        raise OverflowError(message)
    result = values[:]
    result[index] += 1
    result[index + 1:] = [0] * (3 - index)
    assert code(result) > code(values)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("part", choices=("build", "patch", "minor", "major"))
    parser.add_argument("--file", type=Path, default=Path("version.properties"))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    try:
        old = read_version(args.file)
        new = bump(old, args.part)
        if not args.dry_run:
            args.file.write_text("".join(f"{key}={value}\n" for key, value in zip(KEYS, new)))
    except (ValueError, OSError) as error:
        print(error, file=sys.stderr)
        return 2
    except OverflowError as error:
        print(error, file=sys.stderr)
        return 1
    print(f"version: {label(old)} -> {label(new)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
