"""Clear selected outputs before an opt-in accumulated capture run."""
import argparse
from pathlib import Path
import shutil


def prepare(root, only, append):
    prefixes = [part.strip() for part in only.split(',') if part.strip()]
    if not append or not prefixes:
        shutil.rmtree(root, ignore_errors=True)
        return
    for path in root.rglob('*.png'):
        if any(path.relative_to(root).as_posix().startswith(prefix) for prefix in prefixes):
            path.unlink()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root', type=Path)
    parser.add_argument('--only', default='')
    parser.add_argument('--append', choices=('0', '1'), default='0')
    args = parser.parse_args()
    prepare(args.root, args.only, args.append == '1')


if __name__ == '__main__':
    main()
