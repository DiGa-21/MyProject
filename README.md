# Мои домашние дела — Android-каркас

Первый технический билд приложения для детей 8–13 лет и их родителей. Сейчас проект содержит стартовый экран, разделение окружений и архитектурные границы; продуктовая логика будет добавляться следующими задачами.

## Технологии

- Kotlin с built-in Kotlin в Android Gradle Plugin 9.
- Jetpack Compose + Material 3.
- `minSdk 28`, `targetSdk 37`, `compileSdk 37`.
- Варианты сборки `dev` и `prod`.

## Быстрый старт

1. Открыть корневую папку в Android Studio.
2. Дождаться Gradle Sync.
3. Запустить вариант `devDebug` на эмуляторе или Android-устройстве.

Полная инструкция находится в [docs/setup.md](docs/setup.md), архитектурные договорённости — в [docs/architecture.md](docs/architecture.md).

Готовый локальный APK создаётся по пути `app/build/outputs/apk/dev/debug/app-dev-debug.apk`.

Рабочее имя приложения и package `com.myhomechores.app` временные и могут быть заменены до подключения Firebase.

## React Native-клиент

Новая кроссплатформенная версия находится в папке `mobile-react-native`. На первом этапе в ней доступны стартовый экран и переходы в режимы ребёнка и родителя. Kotlin-приложение остаётся резервной рабочей версией в папке `app`.

Установка зависимостей и запуск Android-версии:

```powershell
cd "C:\Android\MyWay\New project\mobile-react-native"
npm install
npm run android
```

Автоматические проверки React Native-клиента:

```powershell
npm run typecheck
npm run test:ci
npm run lint
npx expo export --platform android --output-dir dist
```

Резервная Kotlin-версия по-прежнему собирается из корня проекта:

```powershell
.\gradlew.bat :app:assembleDevDebug
```
