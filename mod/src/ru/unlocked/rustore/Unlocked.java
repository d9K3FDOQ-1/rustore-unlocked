package ru.unlocked.rustore;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Точка входа мода. Патчи в smali RuStore обращаются только к статическим
 * методам этого класса, чтобы изменения в оригинальном коде были минимальными.
 */
public final class Unlocked {
    public static final String VERSION = "0.1.0";
    public static final String TAG = "Unlocked";

    static final String PREFS = "unlocked_prefs";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_DEBUG_LOG = "debug_log";

    /** Переключатели функций мода, показываются в разделе «Функции». */
    static final Feature[] FEATURES = {};

    private static Context appContext;
    private static WeakReference<Activity> resumedActivity = new WeakReference<Activity>(null);

    private Unlocked() {
    }

    static void init(Context context) {
        appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        if (appContext instanceof Application) {
            ((Application) appContext).registerActivityLifecycleCallbacks(new ActivityTracker());
        }
        log("init, version " + VERSION);
    }

    public static Context context() {
        return appContext;
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Главный выключатель мода. */
    public static boolean isEnabled() {
        return appContext == null || prefs(appContext).getBoolean(KEY_ENABLED, true);
    }

    /** Состояние функции с учётом главного выключателя. */
    public static boolean feature(String key) {
        if (appContext == null || !isEnabled()) {
            return false;
        }
        for (Feature feature : FEATURES) {
            if (feature.key.equals(key)) {
                return prefs(appContext).getBoolean(key, feature.defaultValue);
            }
        }
        return false;
    }

    public static void log(String message) {
        if (appContext != null && prefs(appContext).getBoolean(KEY_DEBUG_LOG, false)) {
            Log.d(TAG, message);
        }
    }

    /** Открывает экран настроек мода поверх текущего экрана RuStore. */
    public static void openSettings() {
        Activity activity = resumedActivity.get();
        Context context = activity != null ? activity : appContext;
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, UnlockedSettingsActivity.class);
        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private static final class ActivityTracker implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            resumedActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (resumedActivity.get() == activity) {
                resumedActivity.clear();
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }

    static final class Feature {
        final String key;
        final String title;
        final String summary;
        final boolean defaultValue;

        Feature(String key, String title, String summary, boolean defaultValue) {
            this.key = key;
            this.title = title;
            this.summary = summary;
            this.defaultValue = defaultValue;
        }
    }
}
