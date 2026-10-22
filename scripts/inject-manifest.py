"""Регистрирует компоненты мода в декомпилированном AndroidManifest.xml."""
import re
import sys

path = sys.argv[1]
with open(path, encoding="utf-8") as f:
    manifest = f.read()

if "ru.unlocked.rustore." in manifest:
    sys.exit(0)

package = re.search(r'<manifest\b[^>]*\bpackage="([^"]+)"', manifest).group(1)
components = f"""
        <activity android:name="ru.unlocked.rustore.UnlockedSettingsActivity" android:exported="false" android:label="Unlocked"/>
        <provider android:name="ru.unlocked.rustore.UnlockedInitProvider" android:authorities="{package}.unlocked.init" android:exported="false"/>
    </application>"""

if manifest.count("</application>") != 1:
    sys.exit("expected exactly one </application> in " + path)
manifest = manifest.replace("</application>", components.lstrip("\n"), 1)

with open(path, "w", encoding="utf-8") as f:
    f.write(manifest)
