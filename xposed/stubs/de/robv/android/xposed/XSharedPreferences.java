package de.robv.android.xposed;

/** Compile-only stub. Real class is provided by the LSPosed framework at runtime. */
public final class XSharedPreferences {
    public XSharedPreferences(String packageName, String prefFileName) {
    }

    public boolean makeWorldReadable() {
        return false;
    }

    public void reload() {
    }

    public boolean getBoolean(String key, boolean defValue) {
        return defValue;
    }

    public String getString(String key, String defValue) {
        return defValue;
    }
}
