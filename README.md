# RuStore Unlocked

Мод RuStore (`ru.vk.store`). Добавляет в настройки пункт **Unlocked**, который открывает экран настроек мода.

Базовая версия: **RuStore 1.112.0.4** (`RuStore-mobile-1.112.0.4.apk`, versionCode 1112004, minSdk 26).

## Два варианта

- **Переподписанный APK** (этот README, без root) — патчит smali и пересобирает
  RuStore. Ставится на любой телефон, но RuStore подписан другим ключом, поэтому
  **вход через VK ID не работает** (VK проверяет подпись приложения).
- **LSPosed-модуль** (`xposed/`, нужен root + LSPosed) — ставится поверх
  **оригинального** RuStore и применяет те же функции хуками. RuStore остаётся
  настоящим и подписанным, поэтому **VK ID работает**. См. `xposed/README.md`.

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
- **Функции**: переключатели из `Unlocked.FEATURES`, все по умолчанию выключены. Изменения применяются после перезапуска RuStore, для этого внизу раздела есть кнопка «Перезапустить RuStore».
  - «Убрать рекламу» (`no_ads`). См. раздел [Реклама](#реклама).
  - «Скрыть VPN от RuStore» (`no_vpn_detect`). См. раздел [Обнаружение VPN](#обнаружение-vpn).
  - «Всегда показывать Коннект» (`connect_always`). Коннект показывается в режиме «доступ к российским сервисам из-за границы», даже если сервер или удалённый конфиг его скрывают. См. раздел [Коннект](#коннект).
- **Скрытые разделы**: экраны из `Unlocked.SECRETS`, которые открываются напрямую, без кода в звонилке. См. раздел [Секретные коды](#секретные-коды).
- **О моде**: версия мода, версия RuStore и сброс настроек.

Как добавить функцию:

1. Добавить элемент в `Unlocked.FEATURES`: `new Feature("key", "Название", "Описание", false)`.
2. В нужном месте smali RuStore вызвать `invoke-static {v0}, Lru/unlocked/rustore/Unlocked;->feature(Ljava/lang/String;)Z` (в `v0` лежит ключ).

## Реклама

Реклама приходит с бэкенда RuStore (`v2/ads`, `v3/ads`, `v3/ads/config`, `v1/showcase/ad-placements`) и от VK Ads (`https://m.mradx.net/mobile/{slotId}`). `patches/no-ads.patch` перехватывает три репозитория:

| Класс | Источник | Что возвращает патч |
|---|---|---|
| `z81/e0` (`RawAdvertisementRepositoryImpl`) | `v2/ads`, `v3/ads`, mradx.net | пустой список, как при отсутствии слотов |
| `l91/s` (`RawAdRepository`, post-view) | `v2/ads`, `v3/ads` | `Result.failure`, как при ошибке сети |
| `z81/n` (`AdminAdSlotsRepository`) | `v1/showcase/ad-placements` | `Result.failure` |

## Обнаружение VPN

Модуль `ru.vk.store.lib.tundetector`. Сетевая библиотека `u90/h` помечает сеть как `UNSTABLE`, когда видит туннель. Это состояние читают `TunDetectedStateProvider` (`gz2/c`) и `DeviceTunDetector` (`gz2/b`), а включает проверку удалённый конфиг `tunDetectEnabled`. Результат показывается пользователю как окна и подсказки «Похоже, у вас включен VPN» (`TunDetectedDialogDestination`, стартовый флоу, кнопки установки).

`patches/no-vpn-detect.patch` делает три вещи:

- `TunDetectorRemoteConfig.isTunDetectEnabled` возвращает `false`;
- `gz2/c.a()` возвращает `false`;
- колбэк `gz2/i` всегда передаёт в `StateFlow` состояние `STABLE`.

## Секретные коды

В 1.112.0.4 в звонилке работает только один код: `*#*#8186525725#*#*`. Он состоит из APP_ID VK SDK (`8186525`) и `725`. Код обрабатывает `com.vk.superapp.logs.SuperappLogsBroadcastReceiver` и открывает `SuperappLogsActivity` — логи VK SuperApp Kit.

Собственного отладочного меню у RuStore в релизе нет. `SduiTestScreenDestination` остался в коде только как класс маршрута, перейти на него нельзя.

## Коннект

Модуль `ru.vk.store.feature.connect`. Коннект виден, когда выполнены оба условия:

1. Удалённый конфиг `featureConnectEnabled` равен `true` (`ConnectAvailabilityRemoteConfig.isEnabled`). Иначе use case'ы `RefreshConnectAvailability` и `AwaitConnectAvailability` вообще не проверяют доступность.
2. `ConnectAvailabilityRepositoryImpl` (`mc1/i`) получил статус, отличный от `UNAVAILABLE`. Статус приходит с сервера: `POST v2/connect/availability/check` возвращает `DIRECT` (Россия), `REVERSE` (из-за границы) или `UNAVAILABLE`.

У репозитория есть отладочное переопределение статуса `mc1/i.d()`. В релизе оно всегда возвращает `null`. Если оно вернёт статус, проверка на сервере пропускается.

`patches/connect-always-visible.patch` при включённом `connect_always` делает две вещи: `isEnabled` возвращает `true`, а `d()` возвращает `REVERSE`.

Патч влияет только на показ Коннекта в интерфейсе. Само подключение по-прежнему идёт через серверы VK.

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
