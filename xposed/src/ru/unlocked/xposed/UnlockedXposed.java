package ru.unlocked.xposed;

import java.util.Collections;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed-модуль для RuStore. Применяет функции мода хуками, не трогая APK, —
 * RuStore остаётся оригинальным и подписанным, поэтому вход через VK ID работает.
 *
 * Целевые классы обфусцированы R8 и привязаны к версии RuStore 1.112.0.4; при
 * обновлении магазина имена (z81.e0, mc1.i, gz2.c) нужно сверять заново.
 */
public final class UnlockedXposed implements IXposedHookLoadPackage {
    static final String TARGET = "ru.vk.store";
    static final String TAG = "UnlockedXposed";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) {
            return;
        }
        ClassLoader cl = lpparam.classLoader;
        XposedBridge.log(TAG + ": loaded into " + lpparam.packageName);

        // Убрать рекламу: основной репозиторий отдаёт пустой список слотов.
        hookConstant(cl, "z81.e0", "a", Collections.emptyList());

        // Скрыть VPN от RuStore: детектор туннеля всегда «ничего не нашёл».
        hookConstant(cl, "ru.vk.store.lib.tundetector.TunDetectorRemoteConfig",
                "isTunDetectEnabled", Boolean.FALSE);
        hookConstant(cl, "gz2.c", "a", Boolean.FALSE);

        // Всегда показывать Коннект: фича включена и статус — «из-за границы».
        hookConstant(cl, "ru.vk.store.feature.connect.availability.api.domain.ConnectAvailabilityRemoteConfig",
                "isEnabled", Boolean.TRUE);
        hookConnectStatus(cl);
    }

    /** Хукает все перегрузки метода, заставляя его вернуть константу. */
    private static void hookConstant(ClassLoader cl, String className, String method, Object value) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, cl);
            XposedHelpers.hookAllMethods(clazz, method, XC_MethodReplacement.returnConstant(value));
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook failed " + className + "." + method + ": " + t);
        }
    }

    /** mc1.i.d() — отладочное переопределение статуса доступности Коннекта → REVERSE. */
    private static void hookConnectStatus(ClassLoader cl) {
        try {
            Class<?> statusCls = XposedHelpers.findClass(
                    "ru.vk.store.feature.connect.availability.api.domain.ConnectAvailabilityStatus", cl);
            Object reverse = XposedHelpers.getStaticObjectField(statusCls, "REVERSE");
            Class<?> repo = XposedHelpers.findClass("mc1.i", cl);
            XposedHelpers.hookAllMethods(repo, "d", XC_MethodReplacement.returnConstant(reverse));
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": connect status hook failed: " + t);
        }
    }
}
