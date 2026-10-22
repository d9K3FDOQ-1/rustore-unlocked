#!/usr/bin/env bash
# Собирает LSPosed-модуль xposed/ в work/xposed-out/rustore-unlocked-xposed.apk.
#
# Код мода компилируется против заглушек Xposed API (xposed/stubs), но в dex
# попадают только классы модуля — настоящие de.robv.android.xposed.* даёт
# фреймворк LSPosed в рантайме. APK собирается apktool (его встроенным aapt2)
# и подписывается (debug-ключ или свой, как в scripts/build.sh).
set -euo pipefail
source "$(dirname "$0")/env.sh"

SRC="$ROOT/xposed"
OUT="$WORK/xposed-out"
STUBS="$OUT/stubs"
CLASSES="$OUT/classes"
PROJECT="$OUT/project"
MIN_API=26

rm -rf "$OUT"
mkdir -p "$STUBS" "$CLASSES"

# 1. Заглушки Xposed API — только для компиляции, в dex не идут.
javac --release 8 -nowarn -encoding UTF-8 -d "$STUBS" \
  $(find "$SRC/stubs" -name '*.java')

# 2. Классы модуля компилируем против заглушек + android.jar.
javac --release 8 -nowarn -encoding UTF-8 -cp "$ANDROID_JAR:$STUBS" -d "$CLASSES" \
  $(find "$SRC/src" -name '*.java')

# 3. Dex'им ТОЛЬКО классы модуля (заглушки исключены).
java -cp "$DX_JAR" com.android.dx.command.Main --dex --min-sdk-version=$MIN_API \
  --output="$OUT/classes.dex" "$CLASSES"

# 4. dex -> smali для сборки apktool.
java -cp "$APKTOOL_JAR" "$ROOT/tools-src/Dex2Smali.java" "$OUT/classes.dex" "$OUT/smali" $MIN_API

# 5. Собираем проект apktool: шаблон + smali.
cp -a "$SRC/apktool" "$PROJECT"
cp -a "$OUT/smali" "$PROJECT/smali"
apktool b -o "$OUT/unsigned.apk" "$PROJECT"

# 6. Подпись (debug-ключ по умолчанию; свой — через KEYSTORE/KEY_ALIAS/...).
sign_args=(-a "$OUT/unsigned.apk" -o "$OUT")
if [[ -n ${KEYSTORE:-} ]]; then
  sign_args+=(--ks "$KEYSTORE" --ksAlias "$KEY_ALIAS" --ksPass "$KEYSTORE_PASS" --ksKeyPass "${KEY_PASS:-$KEYSTORE_PASS}")
fi
java -jar "$SIGNER_JAR" "${sign_args[@]}"
signed=("$OUT"/unsigned-aligned-*Signed.apk)
mv "${signed[0]}" "$OUT/rustore-unlocked-xposed.apk"
rm -f "$OUT/unsigned.apk" "$OUT"/*.idsig
echo "Готово: $OUT/rustore-unlocked-xposed.apk"
