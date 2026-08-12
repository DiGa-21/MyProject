package com.myhomechores.app.features.auth

import kotlinx.coroutines.flow.Flow

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
    suspend fun parentDisplayName(): String
    suspend fun updateParentDisplayName(displayName: String): String
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
        email = when {
            cleanEmail.isBlank() -> "Введите электронную почту"
            !emailPattern.matches(cleanEmail) -> "Проверь адрес электронной почты"
            else -> null
        },
        password = when {
            password.isBlank() -> "Введите пароль"
            password.length < 6 -> "Минимум 6 символов"
            else -> null
        },
    )
    return if (errors.email == null && errors.password == null) {
        ValidationResult(SignInInput(cleanEmail, password))
    } else {
        ValidationResult(errors = errors)
    }
}

fun validateRegistration(
    name: String,
    email: String,
    password: String,
    repeat: String,
): ValidationResult<RegistrationInput> {
    val signIn = validateSignIn(email, password)
    val cleanName = name.trim()
    val errors = signIn.errors.copy(
        displayName = if (cleanName.isBlank()) "Введите имя" else null,
        passwordRepeat = if (password != repeat) "Пароли не совпадают" else null,
    )
    return if (
        errors.displayName == null &&
        errors.email == null &&
        errors.password == null &&
        errors.passwordRepeat == null
    ) {
        ValidationResult(RegistrationInput(cleanName, email.trim(), password))
    } else {
        ValidationResult(errors = errors)
    }
}

fun validateNewPassword(password: String, repeat: String): ValidationResult<String> {
    val errors = AuthFieldErrors(
        password = when {
            password.isBlank() -> "Введите новый пароль"
            password.length < 6 -> "Минимум 6 символов"
            else -> null
        },
        passwordRepeat = if (password != repeat) "Пароли не совпадают" else null,
    )
    return if (errors.password == null && errors.passwordRepeat == null) {
        ValidationResult(password)
    } else {
        ValidationResult(errors = errors)
    }
}
