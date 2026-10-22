package ru.unlocked.xposed;

/** Ключи настроек, общие для экрана модуля и хука в процессе RuStore. */
public final class Prefs {
    public static final String PACKAGE = "ru.unlocked.xposed";
    public static final String FILE = "settings";

    public static final String ENABLED = "enabled";
    public static final String NO_ADS = "no_ads";
    public static final String NO_VPN = "no_vpn_detect";
    public static final String CONNECT = "connect_always";

    private Prefs() {
    }
}
