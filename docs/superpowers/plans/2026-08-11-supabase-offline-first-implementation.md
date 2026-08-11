# Supabase Offline-First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Перевести текущее Android-приложение с временного состояния на Room, добавить родительский Supabase-аккаунт с кодом подключения ребёнка и синхронизацию, работающую после восстановления сети.

**Architecture:** UI продолжает обращаться к `AppRepository`, но реализация репозитория становится Room-backed. Room является источником данных для UI и содержит outbox для несинхронизированных изменений. Supabase Auth/Postgres/RLS хранит семейные данные, а WorkManager отправляет и получает изменения при наличии сети.

**Tech Stack:** Kotlin, Jetpack Compose, Room, WorkManager, Supabase Kotlin client, Ktor, Supabase Auth/Postgres/RLS, JUnit, Compose UI tests.

## Global Constraints

- Основная платформа: Android/Kotlin; Next.js и `@supabase/supabase-js` не используются.
- Минимальная версия Android: API 28; текущие build flavors `dev` и `prod` сохраняются.
- UI работает offline-first и не ждёт сеть для отметки дела.
- `display_name` редактирует ребёнок; `parent_label` видит только родитель.
- Дракона выбирает ребёнок.
- Отмена выполнения доступна ребёнку до подтверждения родителя и родителю в кабинете; отмена пересчитывает звёзды и игровой прогресс.
- В Android-клиент попадает только publishable key; service role key запрещён.
- Все операции синхронизации должны быть идемпотентными.
- После каждой задачи запускаются её тесты и создаётся отдельный коммит.

---

## File Map

### Existing files to modify

- `gradle/libs.versions.toml` — версии и aliases для Room, WorkManager, Ktor и Supabase Kotlin.
- `app/build.gradle.kts` — плагины KSP, зависимости и BuildConfig-параметры окружения.
- `app/src/main/java/com/myhomechores/app/MainActivity.kt` — создание `AppRepository` и передача его в корневой Compose-экран.
- `app/src/main/java/com/myhomechores/app/data/AppRepository.kt` — интерфейс потоков чтения и команд изменения.
- `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt` — заменить `rememberSaveable` для профиля, дел и звёзд на состояние из ViewModel, сохранив текущий визуальный дизайн.
- `app/src/main/AndroidManifest.xml` — добавить разрешение сети и необходимые настройки WorkManager, если они потребуются текущей версии библиотеки.

### New local-data files

- `app/src/main/java/com/myhomechores/app/data/local/AppDatabase.kt` — Room database и migrations.
- `app/src/main/java/com/myhomechores/app/data/local/Entities.kt` — локальные таблицы профилей, дел, выполнений, наград и outbox.
- `app/src/main/java/com/myhomechores/app/data/local/Daos.kt` — DAO-интерфейсы и запросы.
- `app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt` — реализация `AppRepository` поверх DAO.
- `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt` — единое UI-состояние и команды экранов.

### New remote/sync files

- `supabase/migrations/001_family_data.sql` — таблицы, индексы, RLS и функции invite-кода.
- `supabase/tests/rls_checks.sql` — ручные SQL-проверки политик и одноразового кода.
- `app/src/main/java/com/myhomechores/app/data/remote/SupabaseClientProvider.kt` — один настроенный Supabase client.
- `app/src/main/java/com/myhomechores/app/data/remote/SupabaseRepository.kt` — Auth и операции Postgres.
- `app/src/main/java/com/myhomechores/app/data/sync/SyncWorker.kt` — один WorkManager worker с retry/backoff.
- `app/src/main/java/com/myhomechores/app/data/sync/SyncScheduler.kt` — unique periodic/one-shot work.
- `app/src/main/java/com/myhomechores/app/features/auth/AuthViewModel.kt` — регистрация, вход и выход родителя.
- `app/src/main/java/com/myhomechores/app/features/auth/AuthScreens.kt` — экраны авторизации без изменения детской комнаты.
- `app/src/main/java/com/myhomechores/app/features/family/InviteCodeViewModel.kt` — создание и принятие кода.
- `app/src/main/java/com/myhomechores/app/features/family/InviteCodeScreens.kt` — UI кода приглашения.

### Test files

- `app/src/test/java/com/myhomechores/app/data/local/RoomRepositoryTest.kt`
- `app/src/test/java/com/myhomechores/app/data/sync/SyncWorkerTest.kt`
- `app/src/test/java/com/myhomechores/app/features/auth/AuthViewModelTest.kt`
- `app/src/test/java/com/myhomechores/app/features/family/InviteCodeTest.kt`
- `app/src/androidTest/java/com/myhomechores/app/OfflinePersistenceTest.kt`
- `app/src/androidTest/java/com/myhomechores/app/UndoCompletionTest.kt`

---

## Task 1: Add Android persistence dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces dependency aliases and KSP configuration consumed by Task 2.

- [ ] **Step 1: Add dependency aliases and KSP configuration.**

Add Room runtime/compiler, WorkManager, Ktor and Supabase Kotlin aliases to the version catalog. Apply the KSP plugin already compatible with the project Kotlin version. Keep `dev` and `prod` flavors unchanged.

- [ ] **Step 2: Resolve dependencies offline before implementation continues.**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.12'
$env:GRADLE_USER_HOME='C:\Users\ПК\Documents\New project\.gradle-user'
.\gradlew.bat --no-daemon --offline :app:compileDevDebugKotlin --console=plain
```

Expected: dependency resolution and Kotlin compilation complete without missing-artifact errors.

- [ ] **Step 3: Commit.**

```powershell
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: add Room and sync dependencies"
```

## Task 2: Model local entities and repository contract

**Files:**
- Modify: `app/src/main/java/com/myhomechores/app/data/AppRepository.kt`
- Create: `app/src/main/java/com/myhomechores/app/data/local/Entities.kt`
- Create: `app/src/main/java/com/myhomechores/app/data/local/Daos.kt`
- Create: `app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/local/AppDatabase.kt`
- Test: `app/src/test/java/com/myhomechores/app/data/local/RoomRepositoryTest.kt`

**Interfaces:**
- `enum class Actor { CHILD, PARENT }`
- `enum class HeroId { BOY, GIRL }`
- `AppRepository.observeChild(): Flow<ChildProfile?>`
- `AppRepository.observeChores(childId: String, date: LocalDate): Flow<List<Chore>>`
- `AppRepository.completions(childId: String): List<Completion>`
- `AppRepository.completeChore(choreId: String, childId: String, date: LocalDate)`
- `AppRepository.undoCompletion(completionId: String, actor: Actor)`
- `AppRepository.updateChildDisplayName(childId: String, value: String)`
- `AppRepository.updateParentLabel(childId: String, value: String?)`
- `AppRepository.selectHero(childId: String, hero: HeroId)`

- [ ] **Step 1: Write repository tests first.**

Cover creation/readback of `display_name`, private `parent_label`, child-owned hero selection, one completion per chore/day, and an undo that changes status to `CANCELLED`.

```kotlin
@Test
fun `completion is unique per chore and date`() = runTest {
    repository.completeChore("teeth", "child-1", LocalDate.of(2026, 8, 11))
    repository.completeChore("teeth", "child-1", LocalDate.of(2026, 8, 11))
    assertEquals(1, repository.completions("child-1").size)
}
```

- [ ] **Step 2: Define Room entities.**

Use stable UUID/string IDs, UTC `updatedAt`, and explicit sync fields. `ChildEntity` must contain both `displayName` and nullable `parentLabel`; `hero` is changed by the child command. `CompletionEntity.status` supports `PENDING`, `CONFIRMED`, and `CANCELLED`.

Create `AppDatabase` in this task, after the entity and DAO types exist:

```kotlin
@Database(
    entities = [ChildEntity::class, ChoreEntity::class, CompletionEntity::class,
        RewardEntity::class, OutboxEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase()
```

The concrete database must expose `childDao()`, `choreDao()`, `completionDao()`, `rewardDao()`, and `outboxDao()`; the snippet omits only the generated implementation.

- [ ] **Step 3: Define DAO queries.**

Add observable `Flow` queries, a unique `(choreId, completionDate)` index, and transactional methods for completion plus reward outbox creation.

- [ ] **Step 4: Implement `RoomAppRepository`.**

Map entities to the existing domain `Chore`/profile models. Every write must update `updatedAt`, write an outbox row, and be wrapped in one Room transaction when it affects completion and rewards together.

- [ ] **Step 5: Run the focused tests.**

```powershell
.\gradlew.bat --no-daemon --offline :app:testDevDebugUnitTest --tests "*RoomRepositoryTest" --console=plain
```

Expected: PASS, including duplicate-completion and cancellation cases.

- [ ] **Step 6: Commit.**

```powershell
git add app/src/main/java/com/myhomechores/app/data
git commit -m "feat: add Room repository and chore entities"
```

## Task 3: Move Compose state to the local repository

**Files:**
- Modify: `app/src/main/java/com/myhomechores/app/MainActivity.kt`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt`
- Create: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt`
- Test: `app/src/androidTest/java/com/myhomechores/app/OfflinePersistenceTest.kt`

**Interfaces:**
- `ScaffoldViewModel.uiState: StateFlow<ScaffoldUiState>`
- `ScaffoldViewModel.onChildNameChanged(value: String)`
- `ScaffoldViewModel.onParentLabelChanged(value: String?)`
- `ScaffoldViewModel.onHeroSelected(hero: HeroId)`
- `ScaffoldViewModel.onChoreCompleted(choreId: String)`
- `ScaffoldViewModel.onUndoCompletion(completionId: String, actor: Actor)`

- [ ] **Step 1: Add a failing instrumentation test.**

Create a repository with Room, launch the screen, enter a child name, mark `teeth`, destroy/recreate the activity, and assert that the name and completion are still present.

- [ ] **Step 2: Create `ScaffoldUiState` and ViewModel.**

Expose immutable state from repository flows. Keep blink animation local to `RoomTab`; it must not be persisted.

- [ ] **Step 3: Replace root `rememberSaveable` data.**

Remove persistence-sensitive state for names, completed IDs and stars from `ScaffoldScreen`. Keep only navigation and transient dialog state in Compose. Forward completion, profile edits and hero selection to the ViewModel.

- [ ] **Step 4: Add explicit Undo action.**

After a child completion, show an undo affordance for the current day. Parent completion controls must call the same repository command with `Actor.PARENT`, causing a confirmation dialog before cancellation.

- [ ] **Step 5: Run Android tests and build.**

```powershell
.\gradlew.bat --no-daemon --offline :app:connectedDevDebugAndroidTest --console=plain
.\gradlew.bat --no-daemon --offline assembleDevDebug --console=plain
```

Expected: persistence test passes and the current room/hero UI remains visually unchanged.

- [ ] **Step 6: Commit.**

```powershell
git add app/src/main/java/com/myhomechores/app/MainActivity.kt app/src/main/java/com/myhomechores/app/features/scaffold
git commit -m "feat: persist app state with Room"
```

## Task 4: Create Supabase schema and RLS

**Files:**
- Create: `supabase/migrations/001_family_data.sql`
- Create: `supabase/README.md`
- Test: `supabase/tests/rls_checks.sql`

**Interfaces:**
- Tables: `families`, `children`, `chores`, `chore_completions`, `rewards`, `invite_codes`.
- RPC: `consume_invite_code(code text, device_id text)` returns only the assigned child ID.

- [ ] **Step 1: Write SQL checks before applying migration.**

Create `supabase/tests/rls_checks.sql` with queries that verify the child session cannot select `parent_label`, completion uniqueness rejects a duplicate date, and a consumed invite code returns no second assignment.

The migration must fail review if `children.parent_label` is exposed to a child session, if completion uniqueness is missing, or if a service-role-only operation is exposed to the publishable client.

- [ ] **Step 2: Create tables and constraints.**

Use UUID primary keys, `timestamptz` `created_at`/`updated_at`, foreign keys with explicit delete behavior, `CHECK (hero IN ('boy', 'girl'))`, and unique `(chore_id, completion_date)`.

- [ ] **Step 3: Add RLS policies.**

Parent policies use `auth.uid() = families.owner_user_id`. Child policies use the server-created device/child association and allow child-visible columns only. Do not create a policy that lets a child update `parent_label`.

- [ ] **Step 4: Add secure invite consumption.**

Store only a hash, require `used_at IS NULL` and `expires_at > now()`, mark the code used in the same transaction, and return no family-wide data.

- [ ] **Step 5: Apply and verify in Supabase.**

Run the migration in the Supabase SQL editor. Verify with a parent session, a child session, an expired code, and a second attempt to consume the same code. Record the results in `supabase/README.md`.

- [ ] **Step 6: Commit.**

```powershell
git add supabase/migrations/001_family_data.sql supabase/README.md
git commit -m "feat: add Supabase family schema and RLS"
```

## Task 5: Add Android Supabase client and parent Auth

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/myhomechores/app/data/remote/SupabaseClientProvider.kt`
- Create: `app/src/main/java/com/myhomechores/app/data/remote/SupabaseRepository.kt`
- Create: `app/src/main/java/com/myhomechores/app/features/auth/AuthViewModel.kt`
- Create: `app/src/main/java/com/myhomechores/app/features/auth/AuthScreens.kt`
- Modify: `app/src/main/java/com/myhomechores/app/MainActivity.kt`
- Test: `app/src/test/java/com/myhomechores/app/features/auth/AuthViewModelTest.kt`

**Interfaces:**
- `AuthViewModel.register(email: String, password: String)`
- `AuthViewModel.signIn(email: String, password: String)`
- `AuthViewModel.signOut()`
- `AuthState.Unauthenticated`, `AuthState.Loading`, `AuthState.Authenticated(userId: String)`.

- [ ] **Step 1: Add failing AuthViewModel tests.**

Use a fake `SupabaseRepository` to assert empty email/password validation, successful registration, failed login message, and sign-out state.

- [ ] **Step 2: Add configuration without secrets in source.**

Expose `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` through `local.properties`/Gradle `BuildConfig` fields for local builds. Add placeholders that fail fast with a readable configuration error when absent; never add a service role key.

- [ ] **Step 3: Implement client and Auth repository.**

Create one application-scoped client, persist the Auth session using the library’s Android storage adapter, and expose typed `Result` errors to the ViewModel.

- [ ] **Step 4: Add parent sign-in UI.**

Show register/sign-in before parent dashboard, keep child mode available locally, and show loading/error states without logging passwords or tokens.

- [ ] **Step 5: Run unit tests and build.**

```powershell
.\gradlew.bat --no-daemon --offline :app:testDevDebugUnitTest --tests "*AuthViewModelTest" --console=plain
```

Expected: PASS. If the Supabase SDK is not in the offline cache, stop and obtain dependency approval before changing the build further.

- [ ] **Step 6: Commit.**

```powershell
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/myhomechores/app
git commit -m "feat: add parent Supabase authentication"
```

## Task 6: Add family profiles, child-owned hero and invite code

**Files:**
- Modify: `app/src/main/java/com/myhomechores/app/data/AppRepository.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt`
- Create: `app/src/main/java/com/myhomechores/app/features/family/InviteCodeViewModel.kt`
- Create: `app/src/main/java/com/myhomechores/app/features/family/InviteCodeScreens.kt`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt`
- Test: `app/src/test/java/com/myhomechores/app/features/family/InviteCodeTest.kt`

**Interfaces:**
- `InviteCodeViewModel.create(childId: String): StateFlow<InviteCodeState>`
- `InviteCodeViewModel.consume(code: String): StateFlow<InviteCodeState>`
- `RoomAppRepository.updateChildDisplayName(childId, value)`
- `RoomAppRepository.updateParentLabel(childId, value)`
- `RoomAppRepository.selectHero(childId, hero)`

- [ ] **Step 1: Write tests for ownership rules.**

Assert that a child can update `displayName` and `hero`, that a child command cannot update `parentLabel`, that a parent can update `parentLabel`, and that a consumed/expired code returns an error without attaching a second device.

- [ ] **Step 2: Implement profile fields and invite state.**

Keep the child-visible profile separate from the parent-only label. Store the selected hero only after the child confirms it. Keep invite code values out of logs and clear them after display/consumption.

- [ ] **Step 3: Add parent and child screens.**

Parent sees child cards with `display_name` plus optional `parent_label`. Child sees only `display_name` and selects Tom/Lily independently. Add edit-name controls in both profiles with validation for blank/overlong names.

- [ ] **Step 4: Run focused tests.**

```powershell
.\gradlew.bat --no-daemon --offline :app:testDevDebugUnitTest --tests "*InviteCodeTest" --console=plain
```

- [ ] **Step 5: Commit.**

```powershell
git add app/src/main/java/com/myhomechores/app/data app/src/main/java/com/myhomechores/app/features/family app/src/main/java/com/myhomechores/app/features/scaffold
git commit -m "feat: add family profiles and invite codes"
```

## Task 7: Implement outbox sync and undo conflict rules

**Files:**
- Create: `app/src/main/java/com/myhomechores/app/data/sync/SyncWorker.kt`
- Create: `app/src/main/java/com/myhomechores/app/data/sync/SyncScheduler.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/local/Entities.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt`
- Test: `app/src/test/java/com/myhomechores/app/data/sync/SyncWorkerTest.kt`
- Test: `app/src/androidTest/java/com/myhomechores/app/UndoCompletionTest.kt`

**Interfaces:**
- `SyncWorker` returns `Result.retry()` for network failures and `Result.failure()` for non-retryable authorization/schema errors.
- `SyncScheduler.enqueueNow()` schedules unique one-shot work; `enqueuePeriodic()` schedules network-constrained periodic work.
- `SyncState.Idle`, `SyncState.Running`, `SyncState.WaitingForNetwork`, `SyncState.Error(message)`.

- [ ] **Step 1: Write sync tests first.**

Test pending insert upload, retry on network exception, idempotent replay, server pull, and parent cancellation winning over a child completion.

- [ ] **Step 2: Implement outbox serialization.**

Persist operation type, stable entity ID, payload, actor, attempt count, and `updatedAt`. Mark an operation synced only after the server confirms it.

- [ ] **Step 3: Implement `SyncWorker`.**

Process operations in timestamp order, use deterministic upsert keys, pull remote changes after upload, and map errors to retry/failure. Never increment stars from the worker without checking the completion ID.

- [ ] **Step 4: Implement cancellation.**

Child undo is allowed before parent confirmation; parent undo is allowed from the dashboard. Store `CANCELLED`, enqueue the inverse operation, and recompute rewards in the same local transaction.

- [ ] **Step 5: Add WorkManager scheduling and status UI.**

Schedule on app start, after every local write, and on network restoration. Display `Синхронизировано`, `Ожидает сети`, or a retry action; do not block chore completion on a network request.

- [ ] **Step 6: Run tests.**

```powershell
.\gradlew.bat --no-daemon --offline :app:testDevDebugUnitTest --tests "*SyncWorkerTest" --console=plain
.\gradlew.bat --no-daemon --offline :app:connectedDevDebugAndroidTest --console=plain
```

- [ ] **Step 7: Commit.**

```powershell
git add app/src/main/java/com/myhomechores/app/data app/src/main/java/com/myhomechores/app/features/scaffold app/src/androidTest
git commit -m "feat: sync offline changes and support undo"
```

## Task 8: End-to-end verification and release handoff

**Files:**
- Modify: `docs/setup.md`
- Modify: `README.md`
- Create: `docs/superpowers/plans/2026-08-11-supabase-offline-first-verification.md`

- [ ] **Step 1: Run the complete local verification.**

```powershell
.\gradlew.bat --no-daemon --offline testDevDebugUnitTest --console=plain
.\gradlew.bat --no-daemon --offline connectedDevDebugAndroidTest --console=plain
.\gradlew.bat --no-daemon --offline assembleDevDebug --console=plain
```

- [ ] **Step 2: Test the real-device flows.**

On two devices, register a parent, create a child, connect with a one-time code, edit the child name, select a hero, complete a chore offline, undo it, reconnect, and verify the parent sees the final state exactly once.

- [ ] **Step 3: Test reinstall recovery.**

Sign in again after uninstall/reinstall and confirm that the family, child name, parent-only label, selected hero, chores, completions and rewards are restored from Supabase.

- [ ] **Step 4: Document setup and safe configuration.**

Document Supabase migration order, local property names, how to run without credentials, how to rotate a publishable key, and that service role keys must never be committed.

- [ ] **Step 5: Commit the verification documentation.**

```powershell
git add docs/setup.md README.md docs/superpowers/plans/2026-08-11-supabase-offline-first-verification.md
git commit -m "docs: add Supabase setup and verification guide"
```

## Handoff

After this plan is approved, execute tasks in order with `superpowers:subagent-driven-development` or `superpowers:executing-plans`. Do not start Supabase implementation before Task 1 confirms the Android dependency cache and the user has supplied/approved the project’s Supabase environment configuration.
