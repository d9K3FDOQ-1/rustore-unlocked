package ru.unlocked.xposed;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Экран модуля в лаунчере. Сам по себе ничего не включает — функции применяет
 * хук {@link UnlockedXposed} в процессе RuStore. Здесь только статус и справка.
 */
public final class InfoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        addTitle(root, "RuStore Unlocked (Xposed)");
        addText(root, "Модуль активируется в LSPosed. Область действия: ru.vk.store.\n"
                + "Поставьте галку на RuStore в LSPosed и перезапустите RuStore.");
        addText(root, "\nЧто делает:\n"
                + "• Убирает рекламу на витрине, в поиске и рекомендациях\n"
                + "• Прячет окна «Похоже, у вас включен VPN»\n"
                + "• Всегда показывает Коннект (доступ из-за границы)");
        addText(root, "\nRuStore при этом остаётся оригинальным и подписанным, "
                + "поэтому вход через VK ID работает как обычно.");
        addText(root, "\nПривязано к RuStore 1.112.0.4. После обновления магазина "
                + "хуки могут перестать срабатывать — тогда нужна новая версия модуля.");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void addTitle(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        parent.addView(view);
    }

    private void addText(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setPadding(0, Math.round(8 * getResources().getDisplayMetrics().density), 0, 0);
        parent.addView(view);
    }
}
