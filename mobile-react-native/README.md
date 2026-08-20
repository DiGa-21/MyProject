# Мой путь — React Native

Кроссплатформенный клиент приложения «Мой путь» на Expo SDK 57 и React Native. Резервная Kotlin-версия находится в соседнем модуле `app` и продолжает собираться отдельно.

## Локальная настройка

1. Установите зависимости:

   ```powershell
   npm.cmd install
   ```

2. Скопируйте `.env.example` в `.env` и заполните только две публичные клиентские переменные:

   ```ini
   EXPO_PUBLIC_SUPABASE_URL=
   EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY=
   ```

   `.env` игнорируется Git. Publishable key предназначен для клиентского приложения; доступ к данным всё равно ограничивают Supabase Auth и RLS. Никогда не помещайте в приложение `service_role` или secret key.

3. Запустите приложение:

   ```powershell
   npx.cmd expo start
   ```

## Авторизация родителя

- `/parent` проверяет сохранённую сессию и направляет на вход или в кабинет.
- Регистрация передаёт имя в Supabase metadata как `display_name`.
- Минимальная длина пароля — 6 символов.
- Сессия хранится в AsyncStorage и восстанавливается после перезапуска.
- Восстановление пароля использует deep link `myway://reset-password`.
- Ответ после запроса recovery-письма намеренно нейтральный и не сообщает, существует ли аккаунт.

## Проверки

```powershell
npm.cmd run test:ci
npm.cmd run typecheck
npm.cmd run lint
npx.cmd expo export --platform android --output-dir dist-auth-check
```

Нативная debug-сборка Android:

```powershell
npx.cmd expo prebuild --platform android
Set-Location android
.\gradlew.bat app:assembleDebug
```

APK создаётся в `android/app/build/outputs/apk/debug/app-debug.apk`.
