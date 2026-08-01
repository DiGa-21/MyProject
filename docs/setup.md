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

## Особенность путей Windows

Android Gradle Plugin умеет собирать проект из текущего пути с кириллицей благодаря `android.overridePathCheck=true`. На некоторых версиях Java отдельный процесс unit-тестов всё же неверно читает classpath с кириллицей и сообщает `ClassNotFoundException`.

Если это происходит, для локального запуска тестов используйте копию проекта в ASCII-пути, например `C:\Android\MyHomeChores`. Обычная сборка APK и Android Lint в текущей папке работают. В GitHub Actions проект запускается в ASCII-пути, поэтому ограничение отсутствует.

Предупреждение о различии версий SDK XML не блокирует сборку. Позже его можно убрать обновлением Android SDK Command-line Tools через SDK Manager.
