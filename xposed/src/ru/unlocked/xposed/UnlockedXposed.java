package ru.unlocked.xposed;

import java.util.Collections;

import android.app.Activity;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed-модуль для RuStore. Применяет функции мода хуками, не трогая APK, —
 * RuStore остаётся оригинальным и подписанным, поэтому вход через VK ID работает.
 *
 * Настройки берутся из префов модуля через {@link XSharedPreferences}; если
 * прочитать не удалось, все функции считаются включёнными. Целевые классы
 * обфусцированы R8 и привязаны к RuStore 1.112.0.4.
 */
public final class UnlockedXposed implements IXposedHookLoadPackage {
    static final String TARGET = "ru.vk.store";
    static final String TAG = "UnlockedXposed";

    private XSharedPreferences prefs;
    private boolean toastShown;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) {
            return;
        }
        ClassLoader cl = lpparam.classLoader;
        XposedBridge.log(TAG + ": loaded into " + lpparam.packageName);

        prefs = new XSharedPreferences(Prefs.PACKAGE, Prefs.FILE);
        try {
            prefs.makeWorldReadable();
        } catch (Throwable ignored) {
        }

        installLoadedToast(cl);

        // Убрать рекламу: основной репозиторий отдаёт пустой список слотов.
        hook(cl, "z81.e0", "a", Prefs.NO_ADS, Collections.emptyList());

        // Скрыть VPN от RuStore: детектор туннеля всегда «ничего не нашёл».
        hook(cl, "ru.vk.store.lib.tundetector.TunDetectorRemoteConfig",
                "isTunDetectEnabled", Prefs.NO_VPN, Boolean.FALSE);
        hook(cl, "gz2.c", "a", Prefs.NO_VPN, Boolean.FALSE);

        // Всегда показывать Коннект: фича включена и статус — «из-за границы».
        hook(cl, "ru.vk.store.feature.connect.availability.api.domain.ConnectAvailabilityRemoteConfig",
                "isEnabled", Prefs.CONNECT, Boolean.TRUE);
        hookConnectStatus(cl);
    }

    /** true, если включён главный выключатель и конкретная функция. */
    private boolean on(String key) {
        try {
            prefs.reload();
            return prefs.getBoolean(Prefs.ENABLED, true) && prefs.getBoolean(key, true);
        } catch (Throwable t) {
            return true;
        }
    }

    /** Хук, который подменяет результат метода константой, пока функция включена. */
    private XC_MethodHook gate(final String key, final Object result) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (on(key)) {
                    param.setResult(result);
                }
            }
        };
    }

    private void hook(ClassLoader cl, String className, String method, String key, Object result) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, cl);
            XposedHelpers.hookAllMethods(clazz, method, gate(key, result));
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook failed " + className + "." + method + ": " + t);
        }
    }

    /** mc1.i.d() — отладочное переопределение статуса доступности Коннекта → REVERSE. */
    private void hookConnectStatus(ClassLoader cl) {
        try {
            Class<?> statusCls = XposedHelpers.findClass(
                    "ru.vk.store.feature.connect.availability.api.domain.ConnectAvailabilityStatus", cl);
            Object reverse = XposedHelpers.getStaticObjectField(statusCls, "REVERSE");
            Class<?> repo = XposedHelpers.findClass("mc1.i", cl);
            XposedHelpers.hookAllMethods(repo, "d", gate(Prefs.CONNECT, reverse));
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": connect status hook failed: " + t);
        }
    }

    /** Один раз за запуск RuStore показывает тост-подтверждение, что модуль загрузился. */
    private void installLoadedToast(ClassLoader cl) {
        try {
            Class<?> activity = XposedHelpers.findClass("android.app.Activity", cl);
            XposedHelpers.hookAllMethods(activity, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (toastShown) {
                        return;
                    }
                    toastShown = true;
                    try {
                        Activity a = (Activity) param.thisObject;
                        boolean enabled;
                        try {
                            prefs.reload();
                            enabled = prefs.getBoolean(Prefs.ENABLED, true);
                        } catch (Throwable t) {
                            enabled = true;
                        }
                        String msg = enabled
                                ? "RuStore Unlocked ✅ загружен"
                                : "RuStore Unlocked ⏸ выключен в настройках модуля";
                        Toast.makeText(a, msg, Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": toast hook failed: " + t);
        }
    }
}
