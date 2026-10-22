#!/usr/bin/env bash
# Разбирает work/rustore.apk:
#   work/decoded — smali + ресурсы (apktool), из этого собирается мод
#   work/jadx    — Java-исходники (jadx), только для чтения кода
set -euo pipefail
source "$(dirname "$0")/env.sh"

[[ -f $APK ]] || { echo "Нет $APK — сначала scripts/fetch-apk.sh" >&2; exit 1; }

apktool d -f -o "$DECODED" "$APK"
if [[ ${1:-} != --no-jadx ]]; then
  rm -rf "$JADX_OUT"
  # jadx всегда находит часть «битых» методов — это не мешает чтению кода.
  "$JADX_BIN" --log-level error --show-bad-code --deobf -d "$JADX_OUT" "$APK" || true
fi
grep -oE 'package="[^"]+"|versionName="[^"]+"' "$DECODED/AndroidManifest.xml" || true
grep -E 'versionName|versionCode|minSdk|targetSdk' "$DECODED/apktool.yml" || true
