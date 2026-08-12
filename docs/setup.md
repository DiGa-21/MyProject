# Локальная настройка

## Требования

- Android Studio 2026.1.x или совместимая версия.
- JDK 17 или новее.
- Android SDK Platform 37.
- Android SDK Build Tools 36.0.0.

Android Studio создаст `local.properties` с путём к SDK при первом открытии проекта.

## Проверка

В PowerShell:

```powershell
.\gradlew.bat testDevDebugUnitTest lintDevDebug assembleDevDebug
```

APK после успешной сборки:

```text
app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

## Supabase для входа и детского кода

1. В `gradle.properties` на своём компьютере задайте `supabaseUrl` и `supabasePublishableKey`. Эти значения не попадают в GitHub.
2. Примените миграции по порядку: `001_family_data.sql`, затем `002_child_invite_onboarding.sql`.
3. В Supabase откройте **Authentication → Providers → Anonymous Sign-Ins** и включите анонимный вход. Он нужен только для безопасной привязки детского устройства.
4. В **Authentication → URL Configuration** добавьте `myway://auth-callback/password-recovery` в Redirect URLs.
5. Для публичного выпуска добавьте CAPTCHA/Turnstile и явный лимит анонимных входов.

Пароль родителя никогда не вводится на детском экране. Код состоит из 6 цифр, действует 15 минут и хранится в базе только как SHA-256-хеш.

## Особенность путей Windows

Android Gradle Plugin умеет собирать проект из текущего пути с кириллицей благодаря `android.overridePathCheck=true`. На некоторых версиях Java отдельный процесс unit-тестов всё же неверно читает classpath с кириллицей и сообщает `ClassNotFoundException`.

Если это происходит, используйте текущий ASCII-путь проекта: `C:\Android\MyWay\New project`.

Предупреждение о различии версий SDK XML не блокирует сборку. Позже его можно убрать обновлением Android SDK Command-line Tools через SDK Manager.
