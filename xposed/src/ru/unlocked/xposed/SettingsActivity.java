package ru.unlocked.xposed;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Экран модуля в лаунчере: переключатели функций + справка. Пишет настройки в
 * мир-читаемый файл, который хук {@link UnlockedXposed} читает в процессе RuStore.
 * Сам экран ничего не хукает — изменения применяются после перезапуска RuStore.
 */
public final class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private int colorCard;
    private int colorText;
    private int colorSecondary;
    private int colorAccent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = openPrefs();

        colorCard = 0xFF1E1E20;
        colorText = 0xFFFFFFFF;
        colorSecondary = 0x99FFFFFF;
        colorAccent = 0xFF0077FF;

        int pad = dp(16);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(pad, dp(8), pad, dp(24));

        title(list, "RuStore Unlocked");
        subtitle(list, "LSPosed-модуль · функции применяются в оригинальном RuStore");

        LinearLayout main = card(list, "Основное");
        switchRow(main, Prefs.ENABLED, "Мод включён",
                "Главный выключатель всех функций модуля", true);

        LinearLayout features = card(list, "Функции");
        switchRow(features, Prefs.NO_ADS, "Убрать рекламу",
                "Пустые рекламные блоки на витрине, в поиске и рекомендациях", true);
        divider(features);
        switchRow(features, Prefs.NO_VPN, "Скрыть VPN",
                "Без окон «Похоже, у вас включён VPN»", true);
        divider(features);
        switchRow(features, Prefs.CONNECT, "Всегда показывать Коннект",
                "Доступ к российским сервисам из-за границы", true);

        LinearLayout about = card(list, "Как пользоваться");
        info(about, "1. В LSPosed включите модуль и область действия — RuStore.\n"
                + "2. После смены переключателей перезапустите RuStore "
                + "(Настройки → Приложения → RuStore → Остановить).\n"
                + "3. При старте RuStore покажет тост «✅ загружен».");
        divider(about);
        info(about, "RuStore остаётся оригинальным и подписанным — вход через VK ID работает.\n"
                + "Привязано к RuStore 1.112.0.4; после обновления магазина хуки могут потребовать правки.");

        if (!worldReadable) {
            LinearLayout warn = card(list, "Внимание");
            info(warn, "Не удалось сделать настройки доступными процессу RuStore. "
                    + "Переключатели могут не применяться — тогда функции работают по умолчанию (включены). "
                    + "Убедитесь, что модуль включён в LSPosed.");
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF121212);
        scroll.addView(list);
        setContentView(scroll);
    }

    private boolean worldReadable = true;

    @SuppressWarnings("deprecation")
    private SharedPreferences openPrefs() {
        try {
            // MODE_WORLD_READABLE: LSPosed делает файл читаемым процессу RuStore.
            return getSharedPreferences(Prefs.FILE, Context.MODE_WORLD_READABLE);
        } catch (Throwable t) {
            worldReadable = false;
            return getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE);
        }
    }

    private void title(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        view.setPadding(dp(4), dp(12), dp(4), 0);
        parent.addView(view);
    }

    private void subtitle(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(colorSecondary);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setPadding(dp(4), dp(4), dp(4), dp(8));
        parent.addView(view);
    }

    private LinearLayout card(LinearLayout parent, String header) {
        TextView label = new TextView(this);
        label.setText(header.toUpperCase());
        label.setTextColor(colorAccent);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        label.setPadding(dp(16), dp(20), dp(16), dp(8));
        parent.addView(label);

        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorCard);
        bg.setCornerRadius(dp(16));
        c.setBackgroundDrawable(bg);
        parent.addView(c);
        return c;
    }

    private void switchRow(LinearLayout card, final String key, String titleText, String summary,
                           boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(titleText);
        t.setTextColor(colorText);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        texts.addView(t);
        TextView s = new TextView(this);
        s.setText(summary);
        s.setTextColor(colorSecondary);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        s.setPadding(0, dp(2), dp(12), 0);
        texts.addView(s);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, def));
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                prefs.edit().putBoolean(key, checked).apply();
            }
        });
        row.addView(toggle);
        row.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                toggle.toggle();
            }
        });
        card.addView(row);
    }

    private void info(LinearLayout card, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(colorSecondary);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.addView(view);
    }

    private void divider(LinearLayout card) {
        android.view.View line = new android.view.View(this);
        line.setBackgroundColor(0x1FFFFFFF);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2));
        p.leftMargin = dp(16);
        card.addView(line, p);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
