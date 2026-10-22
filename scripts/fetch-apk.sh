#!/usr/bin/env bash
# Кладёт оригинальный APK RuStore в work/rustore.apk.
#   scripts/fetch-apk.sh               — скачать с официального сайта
#   scripts/fetch-apk.sh path/to.apk   — взять уже скачанный файл
set -euo pipefail
source "$(dirname "$0")/env.sh"
mkdir -p "$WORK"

if [[ $# -ge 1 ]]; then
  cp "$1" "$APK"
else
  url=${RUSTORE_APK_URL:-https://static.rustore.ru/rustore.apk}
  echo ">> $url"
  curl -fSL --retry 3 -o "$APK.part" "$url"
  mv "$APK.part" "$APK"
fi

unzip -tq "$APK" >/dev/null
java -jar "$SIGNER_JAR" --onlyVerify --apks "$APK" | grep -iE 'subject|sha256|verify' || true
sha256sum "$APK"
