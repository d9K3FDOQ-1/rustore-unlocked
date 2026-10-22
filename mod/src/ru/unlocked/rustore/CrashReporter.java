package ru.unlocked.rustore;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Ловит исключения мода, кладёт трейс в буфер обмена и сохраняет его, чтобы
 * экран настроек мог показать и скопировать трейс последнего сбоя.
 *
 * Все методы безопасны: сами ничего не бросают, чтобы не маскировать исходную
 * ошибку и не вызвать повторный краш внутри обработчика.
 */
public final class CrashReporter {
    static final String KEY_LAST_CRASH = "last_crash";
    static final String CLIP_LABEL = "RuStore Unlocked crash";

    private static volatile boolean installed;
    private static volatile String lastReport;

    private CrashReporter() {
    }

    /** Ставит глобальный обработчик необработанных исключений. Идемпотентно. */
    static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        final Thread.UncaughtExceptionHandler previous;
        Thread.UncaughtExceptionHandler current = null;
        try {
            current = Thread.getDefaultUncaughtExceptionHandler();
        } catch (Throwable ignored) {
        }
        previous = current;
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    try {
                        // Только сохраняем отчёт: буфер обмена трогаем лишь в UI, чтобы не
                        // затирать его на чужих крашах RuStore и не упереться в запрет
                        // фонового доступа к буферу на Android 10+.
                        capture(throwable, "uncaught:" + thread.getName(), Unlocked.context(), false);
                    } catch (Throwable ignored) {
                    }
                    // Сохраняем штатное поведение (показ системного диалога, завершение).
                    if (previous != null) {
                        previous.uncaughtException(thread, throwable);
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** Сохраняет и копирует отчёт, используя контекст мода. */
    static String capture(Throwable throwable, String where) {
        return capture(throwable, where, Unlocked.context(), true);
    }

    /**
     * Формирует отчёт, сохраняет его, при copyNow кладёт в буфер и пишет в logcat.
     * Никогда не бросает исключений. Возвращает текст отчёта.
     *
     * @param context контекст для буфера/prefs; при сбоях инициализации передаётся
     *                явно, т.к. {@link Unlocked#context()} может быть ещё null.
     */
    static String capture(Throwable throwable, String where, Context context, boolean copyNow) {
        String report;
        try {
            report = build(throwable, where, context);
        } catch (Throwable inner) {
            try {
                report = String.valueOf(throwable);
            } catch (Throwable t2) {
                report = "RuStore Unlocked: не удалось сформировать отчёт";
            }
        }
        lastReport = report;
        save(context, report);
        if (copyNow) {
            copyToClipboard(context, report);
        }
        try {
            Log.e(Unlocked.TAG, "crash (" + where + ")", throwable);
        } catch (Throwable ignored) {
        }
        return report;
    }

    /** Трейс последнего сбоя: из памяти, иначе из prefs. null, если сбоев не было. */
    static String lastReport() {
        if (lastReport != null) {
            return lastReport;
        }
        Context context = Unlocked.context();
        if (context != null) {
            try {
                return Unlocked.prefs(context).getString(KEY_LAST_CRASH, null);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    static boolean copyToClipboard(Context context, String text) {
        if (context == null || text == null) {
            return false;
        }
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                return false;
            }
            clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, text));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void save(Context context, String report) {
        if (context == null) {
            return;
        }
        try {
            // commit(), а не apply(): на пути необработанного исключения процесс
            // убивается сразу после, и асинхронная запись apply() может потеряться.
            Unlocked.prefs(context).edit().putString(KEY_LAST_CRASH, report).commit();
        } catch (Throwable ignored) {
        }
    }

    private static String build(Throwable throwable, String where, Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("RuStore Unlocked — отчёт об ошибке\n");
        sb.append("mod: ").append(Unlocked.VERSION).append('\n');
        sb.append("where: ").append(where).append('\n');
        if (context != null) {
            try {
                sb.append("app: ")
                        .append(context.getPackageManager()
                                .getPackageInfo(context.getPackageName(), 0).versionName)
                        .append('\n');
            } catch (Throwable ignored) {
            }
        }
        sb.append("device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        sb.append("android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("uptimeMillis: ").append(android.os.SystemClock.uptimeMillis()).append("\n\n");
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        sb.append(sw);
        return sb.toString();
    }
}
