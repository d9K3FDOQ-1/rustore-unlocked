#!/usr/bin/env bash
# Скачивает apktool, jadx, uber-apk-signer, dx и заглушки Android API в tools/.
set -euo pipefail
source "$(dirname "$0")/env.sh"
mkdir -p "$TOOLS"

fetch() { # url dest sha256
  local url=$1 dest=$2 sha=$3
  if [[ -f $dest ]] && echo "$sha  $dest" | sha256sum -c --status; then
    return
  fi
  echo ">> $url"
  curl -fSL --retry 3 -o "$dest.part" "$url"
  echo "$sha  $dest.part" | sha256sum -c --quiet
  mv "$dest.part" "$dest"
}

fetch "https://github.com/iBotPeaches/Apktool/releases/download/v$APKTOOL_VERSION/apktool_$APKTOOL_VERSION.jar" \
  "$APKTOOL_JAR" 66cf4524a4a45a7f56567d08b2c9b6ec237bcdd78cee69fd4a59c8a0243aeafa
fetch "https://github.com/skylot/jadx/releases/download/v$JADX_VERSION/jadx-$JADX_VERSION.zip" \
  "$TOOLS/jadx.zip" 8280f3799c0273fe797a2bcd90258c943e451fd195f13d05400de5e6451d15ec
fetch "https://github.com/patrickfav/uber-apk-signer/releases/download/v$UBER_SIGNER_VERSION/uber-apk-signer-$UBER_SIGNER_VERSION.jar" \
  "$SIGNER_JAR" e1299fd6fcf4da527dd53735b56127e8ea922a321128123b9c32d619bba1d835
fetch "https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/$DX_VERSION/dalvik-dx-$DX_VERSION.jar" \
  "$DX_JAR" 1e4b645628e3bdb097b5331d669e177ef235a551582a8c646dbe36865e541907
fetch "https://repo1.maven.org/maven2/com/google/android/android/$ANDROID_STUB_VERSION/android-$ANDROID_STUB_VERSION.jar" \
  "$ANDROID_JAR" 84072541cbb711eff89f7277100ff854929a446dba7ceb1b195c340e0b4fd3cb

if [[ ! -x $JADX_BIN ]]; then
  rm -rf "$TOOLS/jadx"
  unzip -q "$TOOLS/jadx.zip" -d "$TOOLS/jadx"
  chmod +x "$JADX_BIN"
fi
echo "Инструменты готовы: $TOOLS"
