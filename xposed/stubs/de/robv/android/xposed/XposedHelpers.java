package de.robv.android.xposed;

import java.util.Set;

/** Compile-only stub. Real class is provided by the LSPosed framework at runtime. */
public final class XposedHelpers {
    private XposedHelpers() {
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        return null;
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return null;
    }

    public static Set hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        return null;
    }
}
