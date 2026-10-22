# RuStore Unlocked

Мод RuStore (`ru.vk.store`). Добавляет в настройки пункт **Unlocked**, который открывает экран настроек мода.

Базовая версия: **RuStore 1.112.0.4** (`RuStore-mobile-1.112.0.4.apk`, versionCode 1112004, minSdk 26).

## Сборка

Нужны JDK 17+, `python3`, `curl`, `unzip`, `patch`.

```bash
scripts/fetch-tools.sh                                 # apktool, jadx, uber-apk-signer, dx → tools/
scripts/fetch-apk.sh RuStore-mobile-1.112.0.4.apk      # оригинальный APK → work/rustore.apk
scripts/decompile.sh                                   # smali → work/decoded, Java → work/jadx
scripts/build.sh                                       # → work/out/rustore-unlocked.apk
```

По умолчанию APK подписывается debug-ключом. Своя подпись задаётся так: `KEYSTORE=… KEY_ALIAS=… KEYSTORE_PASS=… scripts/build.sh`.

Мод подписан другим ключом, поэтому поверх официального RuStore он не установится. Официальную версию нужно сначала удалить.

## Устройство

| Путь | Что там |
|---|---|
| `mod/src/ru/unlocked/rustore/` | Код мода на Java. Собирается в отдельный `classesN.dex` |
| `mod/stubs/` | Заглушки классов, которые уже есть в RuStore (Kotlin `Function0`). Нужны только для компиляции |
| `patches/*.patch` | Правки smali RuStore. Применяются к `work/decoded` |
| `scripts/inject-manifest.py` | Регистрирует в манифесте Activity и ContentProvider мода |
| `tools-src/Dex2Smali.java` | Перевод dex → smali через baksmali из apktool |

- `Unlocked`: статический API для патчей. Методы `isEnabled()`, `feature(key)`, `log()`, `openSettings()`. Настройки лежат в SharedPreferences `unlocked_prefs`.
- `UnlockedInitProvider`: инициализирует мод при старте процесса. Application RuStore при этом не правится.
- `UnlockedSettingsActivity`: вкладка «Unlocked». Интерфейс собирается в коде и следует светлой или тёмной теме системы.
- `OpenSettingsClick`: обработчик нажатия на пункт в настройках RuStore.

### Вкладка «Unlocked»

- **Основное**: «Мод включён» (главный выключатель) и «Журнал мода» (logcat, тег `Unlocked`).
- **Функции**: появляется, когда в `Unlocked.FEATURES` есть хотя бы одна функция.
- **О моде**: версия мода, версия RuStore и сброс настроек.

Как добавить функцию:

1. Добавить элемент в `Unlocked.FEATURES`: `new Feature("key", "Название", "Описание", false)`.
2. В нужном месте smali RuStore вызвать `invoke-static {v0}, Lru/unlocked/rustore/Unlocked;->feature(Ljava/lang/String;)Z` (в `v0` лежит ключ).

## Экран настроек RuStore 1.112.0.4

Экран написан на Compose: `ru.vk.store.feature.user.profile.impl.presentation`, файл `UserProfileScreen.kt`. После R8 он лежит в пакете `es2`, строки берутся из `as2/a` (`R.string.user_profile_*`).

Группы сверху вниз:

1. Аккаунт: вход и профиль.
2. «Перенос галереи в Облако», «Настройки обновлений и загрузок», «Безопасность», «Детский режим». Composable `e0.j`.
3. «Приложения для iOS», «Тема», «Язык».
4. «Помощь», «О RuStore», **«Unlocked»**. Лямбда `es2/e0$f`, сюда вставлен пункт мода.
5. «Выйти из аккаунта».

Пункт строится тем же компонентом, что и соседние (`ps2/i.a`), с иконкой `GearOutline24` (`x03/z1`). Подробности в `patches/settings-unlocked-item.patch`.

### Перенос на новую версию RuStore

После обновления RuStore обфусцированные имена (`es2`, `ps2/i`, `x03/z1`, `r2/g2`) поменяются. Порядок такой:

1. Выполнить `scripts/decompile.sh`.
2. Найти класс, который читает строку «О RuStore»: `grep -rl 'user_profile_about_rustore:I' work/decoded/smali*`. Нужна лямбда со вторым вызовом `invoke-static/range {v1 .. v19}`.
3. Поправить в патче имена классов и сигнатуру composable.
4. Иконку найти по имени: `grep -rl '"GearOutline24"' work/decoded/smali*`.
