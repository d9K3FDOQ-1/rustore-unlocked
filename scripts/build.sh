#!/usr/bin/env bash
# Собирает мод: компилирует mod/src, добавляет его в work/decoded, применяет
# patches/*.patch, пересобирает и подписывает APK.
#   scripts/build.sh             — полная сборка в work/out/rustore-unlocked.apk
#   scripts/build.sh --mod-only  — только скомпилировать код мода в smali
# Своя подпись: KEYSTORE, KEY_ALIAS, KEYSTORE_PASS, KEY_PASS (иначе debug-ключ).
set -euo pipefail
source "$(dirname "$0")/env.sh"
shopt -s nullglob

MOD_OUT="$WORK/mod"
BUILD="$WORK/build"
OUT="$WORK/out"
MIN_API=21

rm -rf "$MOD_OUT"
mkdir -p "$MOD_OUT/classes" "$MOD_OUT/stubs"
# mod/stubs — заглушки классов, которые уже есть в RuStore (Kotlin stdlib); в dex не попадают.
javac --release 8 -nowarn -encoding UTF-8 -d "$MOD_OUT/stubs" $(find "$ROOT/mod/stubs" -name '*.java')
find "$ROOT/mod/src" -name '*.java' > "$MOD_OUT/sources.txt"
javac --release 8 -nowarn -encoding UTF-8 -cp "$ANDROID_JAR:$MOD_OUT/stubs" -d "$MOD_OUT/classes" @"$MOD_OUT/sources.txt"
java -cp "$DX_JAR" com.android.dx.command.Main --dex --min-sdk-version=$MIN_API \
  --output="$MOD_OUT/classes.dex" "$MOD_OUT/classes"
java -cp "$APKTOOL_JAR" "$ROOT/tools-src/Dex2Smali.java" "$MOD_OUT/classes.dex" "$MOD_OUT/smali" $MIN_API
echo "Код мода: $MOD_OUT/smali"
[[ ${1:-} == --mod-only ]] && exit 0

[[ -d $DECODED ]] || { echo "Нет $DECODED — сначала scripts/decompile.sh" >&2; exit 1; }
rm -rf "$BUILD"
cp -a "$DECODED" "$BUILD"

# Код мода кладём в отдельный dex, чтобы не упереться в лимит 64K методов.
last=1
for dir in "$BUILD"/smali_classes*; do
  n=${dir##*smali_classes}
  (( n > last )) && last=$n
done
cp -a "$MOD_OUT/smali" "$BUILD/smali_classes$((last + 1))"

for patch in "$ROOT"/patches/*.patch; do
  echo ">> $(basename "$patch")"
  patch -d "$BUILD" -p1 --forward --no-backup-if-mismatch < "$patch"
done
python3 -I "$ROOT/scripts/inject-manifest.py" "$BUILD/AndroidManifest.xml"

rm -rf "$OUT"
mkdir -p "$OUT"
apktool b -o "$OUT/unsigned.apk" "$BUILD"

sign_args=(-a "$OUT/unsigned.apk" -o "$OUT")
if [[ -n ${KEYSTORE:-} ]]; then
  sign_args+=(--ks "$KEYSTORE" --ksAlias "$KEY_ALIAS" --ksPass "$KEYSTORE_PASS" --ksKeyPass "${KEY_PASS:-$KEYSTORE_PASS}")
fi
java -jar "$SIGNER_JAR" "${sign_args[@]}"
signed=("$OUT"/unsigned-aligned-*Signed.apk)
mv "${signed[0]}" "$OUT/rustore-unlocked.apk"
rm -f "$OUT/unsigned.apk" "$OUT"/*.idsig
echo "Готово: $OUT/rustore-unlocked.apk"
