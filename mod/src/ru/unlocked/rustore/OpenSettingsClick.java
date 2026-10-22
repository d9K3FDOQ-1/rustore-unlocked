package ru.unlocked.rustore;

import kotlin.jvm.functions.Function0;

/** onClick пункта «Unlocked» в настройках RuStore (см. patches/settings-unlocked-item.patch). */
public final class OpenSettingsClick implements Function0<Object> {
    @Override
    public Object invoke() {
        Unlocked.openSettings();
        return null;
    }
}
