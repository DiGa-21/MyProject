# Первый commit и публикация на GitHub

## Что уже подготовлено

- локальный Git-репозиторий и ветка `main`;
- `.gitignore` с исключениями для `local.properties`, ключей, сборок и `.gradle-user`;
- GitHub Actions в `.github/workflows/android.yml`;
- все файлы проекта и ТЗ staged для первого commit.

## Что нужно сделать владельцу проекта

1. На GitHub создать новый пустой репозиторий, например `MyHomeChores`.
2. Не добавлять при создании README, `.gitignore` и License — они уже есть в проекте.
3. Передать разработчику URL вида `https://github.com/USERNAME/MyHomeChores.git`.
4. Указать имя и e-mail автора commit. Для приватности можно использовать GitHub noreply e-mail.

## Команды после получения URL и данных автора

```powershell
git config user.name "Имя автора"
git config user.email "email@example.com"
git commit -m "chore: initial Android scaffold and product epics"
git remote add origin https://github.com/USERNAME/MyHomeChores.git
git push -u origin main
```

При первом `push` GitHub может открыть окно входа или запросить Personal Access Token. Пароль от аккаунта GitHub в терминал вводить не нужно.
