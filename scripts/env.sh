# Общие пути и версии инструментов. Подключается через `source`.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="$ROOT/tools"
WORK="$ROOT/work"

APK="$WORK/rustore.apk"
DECODED="$WORK/decoded"
JADX_OUT="$WORK/jadx"

APKTOOL_VERSION=2.12.1
JADX_VERSION=1.5.3
UBER_SIGNER_VERSION=1.3.0
DX_VERSION=16.0.1
# Заглушка Android API для компиляции. com.google.android:android доступен на
# Maven Central только до 4.1.1.4 (API 16) — это потолок компиляции мода, хотя
# minSdk 26. Код мода намеренно использует только API <= 16; любой вызов API 17+
# здесь не скомпилируется. Для проверки API 26+ подставьте настоящий android.jar.
ANDROID_STUB_VERSION=4.1.1.4

APKTOOL_JAR="$TOOLS/apktool.jar"
JADX_BIN="$TOOLS/jadx/bin/jadx"
SIGNER_JAR="$TOOLS/uber-apk-signer.jar"
DX_JAR="$TOOLS/dx.jar"
ANDROID_JAR="$TOOLS/android-stub.jar"

apktool() { java -jar "$APKTOOL_JAR" "$@"; }
