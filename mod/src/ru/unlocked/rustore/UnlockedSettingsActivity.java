package ru.unlocked.rustore;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Вкладка «Unlocked» — настройки мода. Разметка строится в коде, чтобы мод
 * не зависел от ресурсов RuStore и не требовал правки resources.arsc.
 */
public final class UnlockedSettingsActivity extends Activity {
    private SharedPreferences prefs;
    private boolean night;
    private int colorBackground;
    private int colorCard;
    private int colorText;
    private int colorTextSecondary;
    private int colorAccent;
    private int colorDivider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int theme = getResources().getIdentifier(
                night ? "Theme.DeviceDefault" : "Theme.DeviceDefault.Light", "style", "android");
        if (theme != 0) {
            setTheme(theme);
        }
        super.onCreate(savedInstanceState);
        if (Unlocked.context() == null) {
            Unlocked.init(this);
        }
        prefs = Unlocked.prefs(this);

        colorBackground = night ? 0xFF121212 : 0xFFF2F3F5;
        colorCard = night ? 0xFF1E1E20 : 0xFFFFFFFF;
        colorText = night ? 0xFFFFFFFF : 0xFF19191A;
        colorTextSecondary = night ? 0x99FFFFFF : 0xFF818C99;
        colorAccent = 0xFF0077FF;
        colorDivider = night ? 0x1FFFFFFF : 0x1F000000;

        setTitle("Unlocked");
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(buildContent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private View buildContent() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(8), dp(16), dp(24));

        LinearLayout main = section(list, "Основное");
        switchRow(main, Unlocked.KEY_ENABLED, "Мод включён",
                "Главный выключатель: когда он выключен, все функции мода отключаются", true);
        divider(main);
        switchRow(main, Unlocked.KEY_DEBUG_LOG, "Журнал мода",
                "Писать отладочные сообщения в logcat с тегом " + Unlocked.TAG, false);

        if (Unlocked.FEATURES.length > 0) {
            LinearLayout features = section(list, "Функции");
            for (int i = 0; i < Unlocked.FEATURES.length; i++) {
                Unlocked.Feature feature = Unlocked.FEATURES[i];
                if (i > 0) {
                    divider(features);
                }
                switchRow(features, feature.key, feature.title, feature.summary, feature.defaultValue);
            }
            divider(features);
            View restart = textRow(features, "Перезапустить RuStore", "Изменения применяются после перезапуска");
            ((TextView) ((ViewGroup) restart).getChildAt(0)).setTextColor(colorAccent);
            restart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    restartApp();
                }
            });
        }

        if (Unlocked.SECRETS.length > 0) {
            LinearLayout secrets = section(list, "Скрытые разделы");
            for (int i = 0; i < Unlocked.SECRETS.length; i++) {
                final Unlocked.Secret secret = Unlocked.SECRETS[i];
                if (i > 0) {
                    divider(secrets);
                }
                textRow(secrets, secret.title, secret.summary).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openSecret(secret);
                    }
                });
            }
        }

        LinearLayout about = section(list, "О моде");
        infoRow(about, "Версия мода", Unlocked.VERSION);
        divider(about);
        infoRow(about, "Версия RuStore", appVersion());
        divider(about);
        View reset = textRow(about, "Сбросить настройки мода", null);
        ((TextView) ((ViewGroup) reset).getChildAt(0)).setTextColor(0xFFE64646);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmReset();
            }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(colorBackground);
        scroll.addView(list);
        return scroll;
    }

    private LinearLayout section(LinearLayout parent, String title) {
        TextView header = new TextView(this);
        header.setText(title.toUpperCase());
        header.setTextColor(colorAccent);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(dp(16), dp(20), dp(16), dp(8));
        parent.addView(header);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable();
        background.setColor(colorCard);
        background.setCornerRadius(dp(16));
        card.setBackgroundDrawable(background);
        parent.addView(card);
        return card;
    }

    private void switchRow(LinearLayout card, final String key, String title, String summary,
                           boolean defaultValue) {
        LinearLayout row = textRow(card, title, summary);
        final Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                prefs.edit().putBoolean(key, checked).apply();
                Unlocked.log(key + " = " + checked);
            }
        });
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle.toggle();
            }
        });
    }

    private void infoRow(LinearLayout card, String title, String value) {
        LinearLayout row = textRow(card, title, null);
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(colorTextSecondary);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        row.addView(valueView);
    }

    private LinearLayout textRow(LinearLayout card, String title, String summary) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        TypedValue ripple = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) {
            row.setBackgroundResource(ripple.resourceId);
        }

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(colorText);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        texts.addView(titleView);
        if (summary != null) {
            TextView summaryView = new TextView(this);
            summaryView.setText(summary);
            summaryView.setTextColor(colorTextSecondary);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            summaryView.setPadding(0, dp(2), dp(12), 0);
            texts.addView(summaryView);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        return row;
    }

    private void divider(LinearLayout card) {
        View line = new View(this);
        line.setBackgroundColor(colorDivider);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2));
        params.leftMargin = dp(16);
        card.addView(line, params);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Сбросить настройки мода?")
                .setMessage("Все переключатели Unlocked вернутся к значениям по умолчанию.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().clear().apply();
                        recreate();
                    }
                })
                .show();
    }

    private void openSecret(Unlocked.Secret secret) {
        try {
            startActivity(new Intent().setClassName(getPackageName(), secret.activity));
        } catch (RuntimeException e) {
            Unlocked.log("open " + secret.activity + ": " + e);
            Toast.makeText(this, "Не удалось открыть: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restartApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(launch);
        Runtime.getRuntime().exit(0);
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
