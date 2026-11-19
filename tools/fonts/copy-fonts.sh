#!/bin/sh
# Copies the ten Figtree and Fraunces TTFs and their OFL texts from iOS at the pinned commit (read-only: only `git show`).
set -eu

here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
repo=${CF_IOS_REPO:-/Users/rickgout/Sites/chronoflow-ios}
pin=$(tr -d ' \n' < "$root/tools/ios-pin.txt")

if [ ! -d "$repo/.git" ] && [ ! -f "$repo/.git" ]; then
    echo "copy-fonts: iOS repo not found at $repo (set CF_IOS_REPO)" >&2
    exit 1
fi
if ! git -C "$repo" cat-file -e "$pin^{commit}" 2>/dev/null; then
    echo "copy-fonts: pin $pin is missing in $repo" >&2
    exit 1
fi

src=Packages/ChronoFlowKit/Sources/DesignSystem/Resources/Fonts
module=core/designsystem
entries="
CFFigtree-400.ttf|src/main/res/font/figtree_400.ttf
CFFigtree-500.ttf|src/main/res/font/figtree_500.ttf
CFFigtree-600.ttf|src/main/res/font/figtree_600.ttf
CFFigtree-700.ttf|src/main/res/font/figtree_700.ttf
CFFraunces-400-Opsz20.ttf|src/main/res/font/fraunces_400_opsz20.ttf
CFFraunces-400-Opsz32.ttf|src/main/res/font/fraunces_400_opsz32.ttf
CFFraunces-400-Opsz48.ttf|src/main/res/font/fraunces_400_opsz48.ttf
CFFraunces-500-Opsz20.ttf|src/main/res/font/fraunces_500_opsz20.ttf
CFFraunces-500-Opsz32.ttf|src/main/res/font/fraunces_500_opsz32.ttf
CFFraunces-500-Opsz48.ttf|src/main/res/font/fraunces_500_opsz48.ttf
Figtree-OFL.txt|src/main/assets/licenses/Figtree-OFL.txt
Fraunces-OFL.txt|src/main/assets/licenses/Fraunces-OFL.txt
"

tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

for entry in $entries; do
    name=${entry%%|*}
    dst=$module/${entry##*|}
    mkdir -p "$root/$(dirname "$dst")"
    git -C "$repo" show "$pin:$src/$name" > "$root/$dst"
    printf '%s  %s  %s\n' "$(shasum -a 256 < "$root/$dst" | cut -d' ' -f1)" "$src/$name" "$dst" >> "$tmp"
done

{
    echo "# ios-pin $pin"
    echo "# source google/fonts 24ddc1f324cac5d9f5aedca126d298f56e3f1640, fonttools 4.60.1"
    sort -k3 "$tmp"
} > "$root/$module/fonts.sha256"
echo "copy-fonts: copied fonts at $pin"
