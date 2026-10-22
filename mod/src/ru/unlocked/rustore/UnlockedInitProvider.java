package ru.unlocked.rustore;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Пустой провайдер: система создаёт его при старте процесса, что даёт моду
 * Context без правки Application-класса RuStore.
 */
public final class UnlockedInitProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        // Ставим обработчик как можно раньше, чтобы ловить и сбои инициализации.
        CrashReporter.install();
        try {
            Unlocked.init(getContext());
        } catch (Throwable t) {
            CrashReporter.capture(t, "init");
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
