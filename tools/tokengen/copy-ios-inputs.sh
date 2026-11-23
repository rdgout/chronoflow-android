#!/bin/sh
# Copies the iOS token inputs and goldens at the pinned commit (read-only: only `git show`).
set -eu

here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
repo=${CF_IOS_REPO:-/Users/rickgout/Sites/chronoflow-ios}
pin=$(tr -d ' \n' < "$root/tools/ios-pin.txt")

if [ ! -d "$repo/.git" ] && [ ! -f "$repo/.git" ]; then
    echo "copy-ios-inputs: iOS repo not found at $repo (set CF_IOS_REPO)" >&2
    exit 1
fi
if ! git -C "$repo" cat-file -e "$pin^{commit}" 2>/dev/null; then
    echo "copy-ios-inputs: pin $pin is missing in $repo" >&2
    exit 1
fi

kit=Packages/ChronoFlowKit/Sources
entries="
Tools/TokenGen/Plan.source.json|tools/tokengen/ios/Plan.source.json
Tools/TokenGen/PlanUI.source.json|tools/tokengen/ios/PlanUI.source.json
Tools/TokenGen/Settings.source.json|tools/tokengen/ios/Settings.source.json
Tools/TokenGen/TokenGenReport.md|tools/tokengen/src/test/resources/ios/TokenGenReport.md
$kit/DesignSystem/CfSky.generated.swift|tools/tokengen/src/test/resources/ios/CfSky.generated.swift
$kit/DesignSystem/PlanUITokens.generated.swift|tools/tokengen/src/test/resources/ios/PlanUITokens.generated.swift
$kit/DesignSystem/AuthTokens.generated.swift|tools/tokengen/src/test/resources/ios/AuthTokens.generated.swift
$kit/DesignSystem/SettingsTokens.generated.swift|tools/tokengen/src/test/resources/ios/SettingsTokens.generated.swift
$kit/DesignSystem/TermsTokens.generated.swift|tools/tokengen/src/test/resources/ios/TermsTokens.generated.swift
$kit/DesignSystem/WelcomeTokens.generated.swift|tools/tokengen/src/test/resources/ios/WelcomeTokens.generated.swift
$kit/DesignSystem/PlanTypeMetrics.generated.swift|tools/tokengen/src/test/resources/ios/PlanTypeMetrics.generated.swift
$kit/DesignSystem/TypeMetrics.generated.swift|tools/tokengen/src/test/resources/ios/TypeMetrics.generated.swift
$kit/PlanCore/PlanMetrics.generated.swift|tools/tokengen/src/test/resources/ios/PlanMetrics.generated.swift
"

manifest="$root/tools/tokengen/ios-inputs.sha256"
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

for entry in $entries; do
    src=${entry%%|*}
    dst=${entry##*|}
    mkdir -p "$root/$(dirname "$dst")"
    git -C "$repo" show "$pin:$src" > "$root/$dst"
    printf '%s  %s  %s\n' "$(shasum -a 256 < "$root/$dst" | cut -d' ' -f1)" "$src" "$dst" >> "$tmp"
done

{
    echo "# ios-pin $pin"
    sort -k3 "$tmp"
} > "$manifest"
echo "copy-ios-inputs: copied inputs at $pin"
