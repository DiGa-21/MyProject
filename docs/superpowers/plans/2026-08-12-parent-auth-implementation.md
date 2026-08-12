# Parent Authentication Implementation Plan

> Проверка владельцем приложения на русском языке: [`docs/review/parent-auth-and-child-invite-review.md`](../../review/parent-auth-and-child-invite-review.md). Английские названия и код ниже предназначены только для программиста.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the approved native parent registration, persistent login, logout, and password-recovery flow before the existing parent cabinet.

**Architecture:** Keep Supabase-specific session types behind an `AuthGateway`, map the persisted `sessionStatus` flow into app-owned states, and let a focused `AuthViewModel` drive a stateless Compose screen. `ScaffoldScreen` remains the role selector and existing feature host; only the parent branch receives an authentication gate. Android deep links are handled by a parent Supabase client created in an application container; the child-invite plan adds a second client with a separate storage key so parent and child sessions cannot overwrite one another on a shared family device.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose Material 3, Android ViewModel/StateFlow, supabase-kt 3.2.4 Auth, JUnit 4, kotlinx-coroutines-test, Compose UI tests.

## Global Constraints

- Parent password minimum length is exactly 6 characters in Android validation and Supabase Dashboard configuration.
- Keep `autoLoadFromStorage = true` and `alwaysAutoRefresh = true`; never decide initial navigation from `currentUserOrNull()` before `sessionStatus` leaves `Initializing`.
- Use native Supabase Android authentication; do not add a WebView.
- The mode-selection screen remains the app entry screen. A valid saved parent session bypasses the form after the parent selects “Режим родителя”.
- Use the approved Variant A layout and the approved framed helper asset `parent-helper-framed-medium.png`.
- Passwords, access tokens, refresh tokens, and child connection codes must never be logged.
- Preserve the existing child screens and the design/content of the existing parent cabinet.
- Every interactive control must have a touch target of at least 48 dp, remain scrollable with the keyboard open, and keep the submit button size stable while loading.
- Error and confirmation copy shown to the user must be in Russian.

---

## File Map

- `features/auth/AuthContract.kt`: app-owned session, form, tab, validation, and gateway contracts.
- `features/auth/AuthViewModel.kt`: form actions and mapping of the session flow to UI state.
- `features/auth/AuthScreens.kt`: stateless approved parent auth UI plus its small ViewModel host.
- `features/auth/PasswordRecoveryScreen.kt`: new-password form opened from the deep link.
- `data/remote/SupabaseRepository.kt`: concrete `AuthGateway` implementation and existing remote data API.
- `data/remote/SupabaseClientProvider.kt`: persistent Auth configuration for `myway://auth-callback`.
- `AppContainer.kt`: one shared Room repository, Supabase client, and Supabase repository per app process.
- `MyWayApplication.kt`: owns `AppContainer`.
- `MainActivity.kt`: handles startup and incoming auth deep links.
- `features/scaffold/ScaffoldScreen.kt`: gates only the parent branch and adds sign-out to the parent profile.
- `res/drawable-nodpi/parent_auth_helper.png`: approved helper portrait asset.
- `AndroidManifest.xml`: application class and auth callback intent filter.

### Task 1: Auth Contracts and Form Validation

**Files:**
- Create: `app/src/main/java/com/myhomechores/app/features/auth/AuthContract.kt`
- Create: `app/src/test/java/com/myhomechores/app/features/auth/AuthValidationTest.kt`

**Interfaces:**
- Produces: `AuthGateway.session: Flow<AuthSessionState>`
- Produces: `AuthGateway.signUpParent(email: String, password: String, displayName: String): RegistrationResult`
- Produces: `AuthGateway.signInParent(email: String, password: String)`
- Produces: `AuthGateway.signOut()`, `sendPasswordReset(email: String)`, and `updatePassword(password: String)`
- Produces: `validateSignIn`, `validateRegistration`, and `validateNewPassword`

- [ ] **Step 1: Add the failing validation tests**

```kotlin
package com.myhomechores.app.features.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {
    @Test fun registration_trims_fields_and_accepts_six_characters() {
        val result = validateRegistration("  Мама  ", " parent@example.com ", "123456", "123456")
        assertEquals("Мама", result.value?.displayName)
        assertEquals("parent@example.com", result.value?.email)
        assertNull(result.errors.general)
    }

    @Test fun registration_rejects_short_and_different_passwords() {
        val result = validateRegistration("Мама", "parent@example.com", "12345", "54321")
        assertEquals("Минимум 6 символов", result.errors.password)
        assertEquals("Пароли не совпадают", result.errors.passwordRepeat)
    }

    @Test fun sign_in_rejects_invalid_email_before_network() {
        val result = validateSignIn("not-an-email", "123456")
        assertEquals("Проверь адрес электронной почты", result.errors.email)
        assertNull(result.value)
    }

    @Test fun new_password_requires_matching_six_character_values() {
        val result = validateNewPassword("abcdef", "abcdef")
        assertEquals("abcdef", result.value)
    }
}
```

- [ ] **Step 2: Run the test and confirm the new symbols are missing**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.AuthValidationTest"`

Expected: FAIL because `validateRegistration`, `validateSignIn`, and `validateNewPassword` are unresolved.

- [ ] **Step 3: Implement the app-owned contract and pure validators**

```kotlin
sealed interface AuthSessionState {
    data object Initializing : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(val userId: String, val anonymous: Boolean) : AuthSessionState
    data class RefreshFailed(val message: String) : AuthSessionState
}

enum class AuthTab { SIGN_IN, REGISTRATION }

sealed interface RegistrationResult {
    data object SignedIn : RegistrationResult
    data object EmailConfirmationRequired : RegistrationResult
}

interface AuthGateway {
    val session: Flow<AuthSessionState>
    suspend fun signUpParent(email: String, password: String, displayName: String): RegistrationResult
    suspend fun signInParent(email: String, password: String)
    suspend fun signOut()
    suspend fun sendPasswordReset(email: String)
    suspend fun updatePassword(password: String)
}

data class AuthFieldErrors(
    val displayName: String? = null,
    val email: String? = null,
    val password: String? = null,
    val passwordRepeat: String? = null,
    val general: String? = null,
)

data class RegistrationInput(val displayName: String, val email: String, val password: String)
data class SignInInput(val email: String, val password: String)
data class ValidationResult<T>(val value: T? = null, val errors: AuthFieldErrors = AuthFieldErrors())

private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

fun validateSignIn(email: String, password: String): ValidationResult<SignInInput> {
    val cleanEmail = email.trim()
    val errors = AuthFieldErrors(
        email = when { cleanEmail.isBlank() -> "Введите электронную почту"; !emailPattern.matches(cleanEmail) -> "Проверь адрес электронной почты"; else -> null },
        password = when { password.isBlank() -> "Введите пароль"; password.length < 6 -> "Минимум 6 символов"; else -> null },
    )
    return if (errors.email == null && errors.password == null) ValidationResult(SignInInput(cleanEmail, password)) else ValidationResult(errors = errors)
}

fun validateRegistration(name: String, email: String, password: String, repeat: String): ValidationResult<RegistrationInput> {
    val signIn = validateSignIn(email, password)
    val cleanName = name.trim()
    val errors = signIn.errors.copy(
        displayName = if (cleanName.isBlank()) "Введите имя" else null,
        passwordRepeat = if (password != repeat) "Пароли не совпадают" else null,
    )
    return if (errors.displayName == null && errors.email == null && errors.password == null && errors.passwordRepeat == null) {
        ValidationResult(RegistrationInput(cleanName, email.trim(), password))
    } else ValidationResult(errors = errors)
}

fun validateNewPassword(password: String, repeat: String): ValidationResult<String> {
    val errors = AuthFieldErrors(
        password = when { password.isBlank() -> "Введите новый пароль"; password.length < 6 -> "Минимум 6 символов"; else -> null },
        passwordRepeat = if (password != repeat) "Пароли не совпадают" else null,
    )
    return if (errors.password == null && errors.passwordRepeat == null) ValidationResult(password) else ValidationResult(errors = errors)
}
```

- [ ] **Step 4: Run the validator tests**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.AuthValidationTest"`

Expected: PASS.

- [ ] **Step 5: Commit the contracts**

```powershell
git add app/src/main/java/com/myhomechores/app/features/auth/AuthContract.kt app/src/test/java/com/myhomechores/app/features/auth/AuthValidationTest.kt
git commit -m "feat: define parent auth contract"
```

### Task 2: Persistent Supabase Session and Android Deep Links

**Files:**
- Modify: `app/src/main/java/com/myhomechores/app/data/remote/SupabaseClientProvider.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/remote/SupabaseRepository.kt`
- Create: `app/src/main/java/com/myhomechores/app/AppContainer.kt`
- Create: `app/src/main/java/com/myhomechores/app/MyWayApplication.kt`
- Modify: `app/src/main/java/com/myhomechores/app/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/myhomechores/app/data/remote/AuthErrorMapperTest.kt`

**Interfaces:**
- Consumes: `AuthGateway` and `AuthSessionState` from Task 1.
- Produces: `SupabaseRepository : AuthGateway`.
- Produces: `AppContainer.localRepository`, `AppContainer.remoteRepository`, and `AppContainer.supabase`.

- [ ] **Step 1: Add failing Russian error-mapping tests**

```kotlin
class AuthErrorMapperTest {
    @Test fun maps_invalid_credentials() = assertEquals(
        "Неверная почта или пароль",
        authMessage(IllegalStateException("Invalid login credentials")),
    )

    @Test fun maps_network_failure() = assertEquals(
        "Нет связи с интернетом. Попробуй ещё раз",
        authMessage(IllegalStateException("Unable to resolve host")),
    )

    @Test fun never_returns_tokens_or_raw_server_text() {
        val message = authMessage(IllegalStateException("refresh_token=secret"))
        assertEquals("Не удалось выполнить действие. Попробуй ещё раз", message)
    }
}
```

- [ ] **Step 2: Run the mapper test and verify failure**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.AuthErrorMapperTest"`

Expected: FAIL because `authMessage` does not exist.

- [ ] **Step 3: Configure Auth persistence and deep-link host**

```kotlin
install(Auth) {
    scheme = "myway"
    host = "auth-callback"
    sessionManager = SettingsSessionManager(key = sessionKey)
    autoLoadFromStorage = true
    alwaysAutoRefresh = true
}
```

Change the provider signature to `create(url: String, publishableKey: String, sessionKey: String)` and import `io.github.jan.supabase.auth.SettingsSessionManager`. This is the exact session-manager API present in supabase-kt 3.2.4.

Map `client.auth.sessionStatus` exactly as follows:

```kotlin
override val session: Flow<AuthSessionState> = client.auth.sessionStatus.map { status ->
    when (status) {
        SessionStatus.Initializing -> AuthSessionState.Initializing
        is SessionStatus.Authenticated -> AuthSessionState.Authenticated(
            userId = status.session.user?.id.orEmpty(),
            anonymous = status.session.user?.isAnonymous == true,
        )
        is SessionStatus.NotAuthenticated -> AuthSessionState.Unauthenticated
        is SessionStatus.RefreshFailure -> AuthSessionState.RefreshFailed(
            "Сессия закончилась. Войди ещё раз",
        )
    }
}
```

Implement repository operations with these supabase-kt calls:

```kotlin
client.auth.signUpWith(Email) { this.email = email; this.password = password; data = buildJsonObject { put("display_name", displayName) } }
client.auth.signInWith(Email) { this.email = email; this.password = password }
client.auth.signOut()
client.auth.resetPasswordForEmail(email = email, redirectUrl = "myway://auth-callback/password-recovery")
client.auth.updateUser { this.password = password }
```

Determine `RegistrationResult` after signup from `currentSessionOrNull()`: a non-null session returns `SignedIn`; a null session returns `EmailConfirmationRequired`.

- [ ] **Step 4: Add the process-wide app container**

```kotlin
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "myhomechores.db")
        .fallbackToDestructiveMigration()
        .build()
    val localRepository: AppRepository = RoomAppRepository(database)
    val supabase = SupabaseClientProvider.create(
        BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        sessionKey = "myway-parent-session",
    )
    val remoteRepository = SupabaseRepository(supabase)
}

class MyWayApplication : Application() {
    val container by lazy { AppContainer(applicationContext) }
}
```

Set `android:name=".MyWayApplication"` on `<application>`.

- [ ] **Step 5: Handle both cold-start and already-open deep links**

```kotlin
class MainActivity : ComponentActivity() {
    private val container get() = (application as MyWayApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container.supabase.handleDeeplinks(intent)
        // existing theme/content setup uses container.localRepository and container.remoteRepository
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        container.supabase.handleDeeplinks(intent)
    }
}
```

Add a second `VIEW` intent filter to `MainActivity`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="myway" android:host="auth-callback" />
</intent-filter>
```

- [ ] **Step 6: Implement safe Russian error mapping and rerun tests**

`authMessage(Throwable)` must lowercase the exception message only for matching and return one of these fixed strings: invalid credentials, account already registered, rate limit, network unavailable, or generic failure. It must never return the raw exception text.

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.AuthErrorMapperTest"`

Expected: PASS.

- [ ] **Step 7: Commit Supabase session infrastructure**

```powershell
git add app/src/main/java/com/myhomechores/app/data/remote app/src/main/java/com/myhomechores/app/AppContainer.kt app/src/main/java/com/myhomechores/app/MyWayApplication.kt app/src/main/java/com/myhomechores/app/MainActivity.kt app/src/main/AndroidManifest.xml app/src/test/java/com/myhomechores/app/data/remote/AuthErrorMapperTest.kt
git commit -m "feat: persist parent auth session"
```

### Task 3: AuthViewModel State Machine

**Files:**
- Replace: `app/src/main/java/com/myhomechores/app/features/auth/AuthViewModel.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/com/myhomechores/app/features/auth/AuthViewModelTest.kt`

**Interfaces:**
- Consumes: `AuthGateway`, validators, `AuthSessionState`, and `authMessage`.
- Produces: `AuthUiState`, `AuthViewModel.selectTab`, `signIn`, `signUp`, `requestPasswordReset`, `saveNewPassword`, and `signOut`.

- [ ] **Step 1: Add the coroutine test dependency**

Add `coroutines = "1.10.2"` and:

```toml
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
```

Then add `testImplementation(libs.kotlinx.coroutines.test)`.

- [ ] **Step 2: Write failing ViewModel tests with a fake gateway**

The fake holds `MutableStateFlow<AuthSessionState>(Initializing)` and counters for every call. Tests must assert:

```kotlin
@Test fun saved_session_opens_parent_without_form() = runTest {
    gateway.sessionState.value = AuthSessionState.Authenticated("parent-1", anonymous = false)
    advanceUntilIdle()
    assertEquals(AuthStage.AUTHENTICATED, viewModel.state.value.stage)
}

@Test fun invalid_registration_never_calls_gateway() = runTest {
    viewModel.signUp("", "bad", "123", "456")
    advanceUntilIdle()
    assertEquals(0, gateway.signUpCalls)
    assertEquals("Введите имя", viewModel.state.value.errors.displayName)
}

@Test fun confirmation_required_is_visible() = runTest {
    gateway.registrationResult = RegistrationResult.EmailConfirmationRequired
    viewModel.signUp("Мама", "parent@example.com", "123456", "123456")
    advanceUntilIdle()
    assertEquals(AuthStage.AWAITING_EMAIL_CONFIRMATION, viewModel.state.value.stage)
}

@Test fun reset_confirmation_is_neutral() = runTest {
    viewModel.requestPasswordReset("unknown@example.com")
    advanceUntilIdle()
    assertEquals("Если аккаунт с такой почтой существует, мы отправили письмо", viewModel.state.value.notice)
}
```

- [ ] **Step 3: Run the ViewModel test and verify failure**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.AuthViewModelTest"`

Expected: FAIL because the new state machine API is missing.

- [ ] **Step 4: Implement the state machine**

Use these exact stages and state fields:

```kotlin
enum class AuthStage { INITIALIZING, UNAUTHENTICATED, SUBMITTING, AWAITING_EMAIL_CONFIRMATION, PASSWORD_RECOVERY, AUTHENTICATED }

data class AuthUiState(
    val stage: AuthStage = AuthStage.INITIALIZING,
    val tab: AuthTab = AuthTab.SIGN_IN,
    val errors: AuthFieldErrors = AuthFieldErrors(),
    val notice: String? = null,
    val resetDialogVisible: Boolean = false,
)
```

Collect `gateway.session` in `init`. Ignore anonymous sessions for the parent gate by mapping `Authenticated(anonymous = true)` to `UNAUTHENTICATED`. Clear field errors on tab change, preserve typed field values inside the stateless screen with `rememberSaveable`, and map all caught exceptions through `authMessage`.

- [ ] **Step 5: Run the ViewModel tests**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.AuthViewModelTest"`

Expected: PASS.

- [ ] **Step 6: Commit the state machine**

```powershell
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/myhomechores/app/features/auth/AuthViewModel.kt app/src/test/java/com/myhomechores/app/features/auth/AuthViewModelTest.kt
git commit -m "feat: add parent auth state machine"
```

### Task 4: Approved Parent Auth UI and Parent Navigation Gate

**Files:**
- Replace: `app/src/main/java/com/myhomechores/app/features/auth/AuthScreens.kt`
- Add: `app/src/main/res/drawable-nodpi/parent_auth_helper.png`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt:128-169`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt:1108-1640`
- Modify: `app/src/main/java/com/myhomechores/app/MainActivity.kt`
- Create: `app/src/androidTest/java/com/myhomechores/app/features/auth/ParentAuthScreenTest.kt`

**Interfaces:**
- Consumes: `AuthUiState` and `AuthViewModel` from Task 3.
- Produces: `ParentAuthRoute(repository, onBack, parentContent)`.
- Changes: `ScaffoldScreen(environment, repository, remoteRepository)`.

- [ ] **Step 1: Copy the approved portrait into Android resources**

Copy:

`C:\Users\ПК\.codex\visualizations\2026\07\31\019fb79b-2ee8-70f3-950d-c778ba91d4f8\parent-helper-framed-medium.png`

to:

`app/src/main/res/drawable-nodpi/parent_auth_helper.png`

- [ ] **Step 2: Write failing Compose tests against a stateless screen**

```kotlin
@Test fun registration_tab_shows_all_fields() {
    composeRule.setContent {
        ParentAuthContent(state = AuthUiState(tab = AuthTab.REGISTRATION, stage = AuthStage.UNAUTHENTICATED), callbacks = noOpCallbacks)
    }
    composeRule.onNodeWithText("Имя").assertExists()
    composeRule.onNodeWithText("Повтори пароль").assertExists()
    composeRule.onNodeWithText("Создать аккаунт").assertExists()
}

@Test fun submitting_disables_stable_primary_button() {
    composeRule.setContent {
        ParentAuthContent(state = AuthUiState(stage = AuthStage.SUBMITTING), callbacks = noOpCallbacks)
    }
    composeRule.onNodeWithText("Войти").assertIsNotEnabled()
    composeRule.onNodeWithContentDescription("Загрузка").assertExists()
}
```

- [ ] **Step 3: Run the Compose test and verify failure**

Run: `./gradlew.bat connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.myhomechores.app.features.auth.ParentAuthScreenTest`

Expected: FAIL because `ParentAuthContent` and its callbacks do not exist.

- [ ] **Step 4: Implement the approved stateless UI**

Build `ParentAuthContent` as a `LazyColumn` with `imePadding()` and this order:

1. 48 dp “Назад” control.
2. `parent_auth_helper` in a fixed 196 dp square; do not draw a second frame in Compose because the approved frame is already part of the asset.
3. “Кабинет родителя” and “Войди, чтобы управлять делами семьи”.
4. One rounded lilac tab container with “Вход” and “Регистрация”.
5. Fields for the active tab with field-specific supporting text.
6. Fixed-height 56 dp turquoise submit button; place the progress indicator inside the button without replacing its layout.
7. “Забыли пароль?” only on the sign-in tab.

Password fields use `PasswordVisualTransformation`, a show/hide icon, `KeyboardType.Password`, and `ImeAction.Done`. Fields keep their typed values through `rememberSaveable` and are cleared only after a successful corresponding operation.

- [ ] **Step 5: Gate only the parent role**

Change the parent branch of `ScaffoldScreen` to:

```kotlin
AppRole.PARENT -> ParentAuthRoute(
    repository = remoteRepository,
    onBack = { viewModel.selectRole(null) },
) {
    ParentModeScreen(
        childName = state.childName,
        parentName = state.parentName,
        onParentNameChange = viewModel::updateParentName,
        onChildNameChange = viewModel::updateChildName,
        onBack = { viewModel.selectRole(null) },
        onSignOut = authViewModel::signOut,
    )
}
```

Keep role selection first. `ParentAuthRoute` shows a neutral loading surface during `INITIALIZING`, the auth form during `UNAUTHENTICATED`, and invokes `parentContent` only for `AUTHENTICATED`.

- [ ] **Step 6: Add sign-out to the existing parent profile**

Add a full-width outlined “Выйти из аккаунта” button to `ParentProfileTab`. After `gateway.signOut()` emits `Unauthenticated`, the gate shows the login tab without returning to child mode.

- [ ] **Step 7: Run existing and new Compose tests**

Run: `./gradlew.bat connectedDevDebugAndroidTest`

Expected: all existing scaffold tests and `ParentAuthScreenTest` PASS.

- [ ] **Step 8: Commit the approved UI**

```powershell
git add app/src/main/java/com/myhomechores/app/features/auth/AuthScreens.kt app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt app/src/main/java/com/myhomechores/app/MainActivity.kt app/src/main/res/drawable-nodpi/parent_auth_helper.png app/src/androidTest/java/com/myhomechores/app/features/auth/ParentAuthScreenTest.kt
git commit -m "feat: gate parent cabinet with auth"
```

### Task 5: Password Reset Dialog and New Password Screen

**Files:**
- Create: `app/src/main/java/com/myhomechores/app/features/auth/PasswordRecoveryScreen.kt`
- Modify: `app/src/main/java/com/myhomechores/app/features/auth/AuthScreens.kt`
- Modify: `app/src/main/java/com/myhomechores/app/MainActivity.kt`
- Create: `app/src/androidTest/java/com/myhomechores/app/features/auth/PasswordRecoveryScreenTest.kt`

**Interfaces:**
- Consumes: `AuthViewModel.requestPasswordReset` and `saveNewPassword`.
- Produces: `PasswordRecoveryScreen(state, onSave, onBack)`.

- [ ] **Step 1: Write failing password-recovery UI tests**

```kotlin
@Test fun reset_dialog_uses_neutral_confirmation() {
    composeRule.setContent { ParentAuthContent(stateWithResetDialog, callbacks) }
    composeRule.onNodeWithText("Восстановить пароль").assertExists()
    composeRule.onNodeWithText("Отправить письмо").performClick()
    composeRule.onNodeWithText("Если аккаунт с такой почтой существует, мы отправили письмо").assertExists()
}

@Test fun new_password_screen_rejects_five_characters() {
    composeRule.setContent { PasswordRecoveryContent(AuthUiState(stage = AuthStage.PASSWORD_RECOVERY), callbacks) }
    composeRule.onNodeWithText("Новый пароль").performTextInput("12345")
    composeRule.onNodeWithText("Повтори пароль").performTextInput("12345")
    composeRule.onNodeWithText("Сохранить пароль").performClick()
    composeRule.onNodeWithText("Минимум 6 символов").assertExists()
}
```

- [ ] **Step 2: Run the targeted test and verify failure**

Run: `./gradlew.bat connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.myhomechores.app.features.auth.PasswordRecoveryScreenTest`

Expected: FAIL because the recovery UI is missing.

- [ ] **Step 3: Implement reset request and deep-link recovery state**

The reset dialog contains only email, “Отмена”, and “Отправить письмо”. It always shows the neutral confirmation after a successful request. Detect `intent.data?.path == "/password-recovery"` in `MainActivity` and pass `passwordRecoveryRequested = true` into the root; after `handleDeeplinks(intent)` authenticates the recovery session, show `PasswordRecoveryScreen` instead of the cabinet.

- [ ] **Step 4: Implement the new-password screen**

Use the Task 1 validator. On success call `client.auth.updateUser { password = password }`, clear both password fields, clear recovery mode, and show the parent cabinet. On failure keep the fields and display a fixed Russian error from `authMessage`.

- [ ] **Step 5: Run recovery and full unit tests**

Run: `./gradlew.bat testDevDebugUnitTest`

Run: `./gradlew.bat connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.myhomechores.app.features.auth.PasswordRecoveryScreenTest`

Expected: PASS.

- [ ] **Step 6: Commit password recovery**

```powershell
git add app/src/main/java/com/myhomechores/app/features/auth app/src/main/java/com/myhomechores/app/MainActivity.kt app/src/androidTest/java/com/myhomechores/app/features/auth/PasswordRecoveryScreenTest.kt
git commit -m "feat: add parent password recovery"
```

### Task 6: Documentation, Build, and Device Verification

**Files:**
- Modify: `supabase/README.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: the completed parent auth flow.
- Produces: repeatable Supabase Dashboard and Android verification instructions.

- [ ] **Step 1: Document the exact Supabase settings**

Add these settings to `supabase/README.md`:

```text
Authentication > Providers > Email: enabled
Authentication > Password security > Minimum password length: 6
Authentication > URL Configuration > Redirect URLs: myway://auth-callback
Authentication > Email Templates: confirmation and password recovery enabled
```

State that production release requires a configured SMTP provider and that the public publishable key is allowed in the Android client while the secret/service-role key is forbidden.

- [ ] **Step 2: Run static verification**

Run: `./gradlew.bat testDevDebugUnitTest lintDevDebug assembleDevDebug`

Expected: `BUILD SUCCESSFUL` with no lint errors.

- [ ] **Step 3: Run emulator verification**

Run: `./gradlew.bat connectedDevDebugAndroidTest`

Expected: all instrumentation tests PASS.

- [ ] **Step 4: Perform the manual parent checklist**

Verify on the emulator and physical phone:

1. First parent selection shows Variant A, no login-form flash before session initialization finishes.
2. Registration rejects a 5-character password and accepts 6 characters.
3. Reopening the app and selecting parent mode opens the cabinet without another login.
4. Explicit logout shows the login tab.
5. Wrong password, network loss, and existing email show short Russian errors without raw server content.
6. Reset email uses neutral confirmation; the link opens “Новый пароль”; the new password opens the parent cabinet.
7. Child mode still opens its existing flow unchanged.

- [ ] **Step 5: Commit docs and verification state**

```powershell
git add README.md supabase/README.md
git commit -m "docs: add parent auth setup and checks"
```
