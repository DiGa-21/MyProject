# Настройка Supabase

1. Создайте проект Supabase и примените `migrations/001_family_data.sql`, затем `migrations/002_child_invite_onboarding.sql`.
2. В Android настройте только URL проекта и publishable key. Service-role key запрещено помещать в приложение или GitHub.
3. Включите **Authentication → Providers → Anonymous Sign-Ins**. Анонимная сессия даёт детскому устройству собственный идентификатор; родительский пароль ребёнку не нужен.
4. Добавьте `myway://auth-callback/password-recovery` для Kotlin-клиента и `myway://reset-password` для React Native-клиента в разрешённые Redirect URLs.
5. Проверьте контракт базы через `tests/rls_checks.sql` и `tests/child_invite_checks.sql`.

## React Native Auth

Для текущей семейной сборки в **Authentication → Providers → Email** отключите обязательное подтверждение email: регистрация должна сразу возвращать сессию. Минимальная длина пароля — `6`.

React Native-клиент использует только:

```ini
EXPO_PUBLIC_SUPABASE_URL=
EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY=
```

Существующий триггер `handle_new_user` переносит `display_name` из metadata в `public.profiles`. Дополнительная миграция базы данных для родительской авторизации React Native не нужна.

Анонимные пользователи имеют роль `authenticated`, но родительские RPC дополнительно проверяют JWT `is_anonymous = false`. Детский профиль не содержит `parent_label`, а прямое изменение строки `children` для ребёнка запрещено. Одноразовый код генерируется криптографически, хранится как SHA-256-хеш, действует 15 минут и после пяти ошибок блокирует попытки на 5 минут по сетевому адресу (если его передал прокси Supabase) или по анонимной сессии.

Перед общедоступным выпуском включите CAPTCHA/Turnstile, задайте безопасный лимит анонимных регистраций и периодически удаляйте старые непривязанные анонимные аккаунты.
