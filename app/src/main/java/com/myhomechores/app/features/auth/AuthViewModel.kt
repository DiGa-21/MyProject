package com.myhomechores.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myhomechores.app.data.remote.authMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStage {
    INITIALIZING,
    UNAUTHENTICATED,
    SUBMITTING,
    AWAITING_EMAIL_CONFIRMATION,
    PASSWORD_RECOVERY,
    AUTHENTICATED,
}

data class AuthUiState(
    val stage: AuthStage = AuthStage.INITIALIZING,
    val tab: AuthTab = AuthTab.SIGN_IN,
    val errors: AuthFieldErrors = AuthFieldErrors(),
    val notice: String? = null,
    val resetDialogVisible: Boolean = false,
)

class AuthViewModel(private val gateway: AuthGateway) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            gateway.session.collect { session ->
                val stage = when (session) {
                    AuthSessionState.Initializing -> AuthStage.INITIALIZING
                    AuthSessionState.Unauthenticated -> AuthStage.UNAUTHENTICATED
                    is AuthSessionState.Authenticated -> if (session.anonymous) AuthStage.UNAUTHENTICATED else AuthStage.AUTHENTICATED
                    is AuthSessionState.RefreshFailed -> AuthStage.UNAUTHENTICATED
                }
                mutableState.update {
                    it.copy(
                        stage = stage,
                        errors = if (session is AuthSessionState.RefreshFailed) {
                            it.errors.copy(general = session.message)
                        } else {
                            it.errors
                        },
                    )
                }
            }
        }
    }

    fun selectTab(tab: AuthTab) {
        mutableState.update {
            it.copy(
                stage = if (it.stage == AuthStage.AWAITING_EMAIL_CONFIRMATION) {
                    AuthStage.UNAUTHENTICATED
                } else {
                    it.stage
                },
                tab = tab,
                errors = AuthFieldErrors(),
                notice = null,
            )
        }
    }

    fun showResetDialog() {
        mutableState.update { it.copy(resetDialogVisible = true, errors = AuthFieldErrors(), notice = null) }
    }

    fun hideResetDialog() {
        mutableState.update { it.copy(resetDialogVisible = false, errors = AuthFieldErrors()) }
    }

    fun enterPasswordRecovery() {
        mutableState.update { it.copy(stage = AuthStage.PASSWORD_RECOVERY, errors = AuthFieldErrors(), notice = null) }
    }

    fun cancelPasswordRecovery() {
        mutableState.update { it.copy(stage = AuthStage.AUTHENTICATED, errors = AuthFieldErrors()) }
    }

    fun signUp(name: String, email: String, password: String, repeat: String) {
        val validation = validateRegistration(name, email, password, repeat)
        val input = validation.value
        if (input == null) {
            mutableState.update { it.copy(errors = validation.errors, notice = null) }
            return
        }
        submit {
            when (gateway.signUpParent(input.email, input.password, input.displayName)) {
                RegistrationResult.SignedIn -> Unit
                RegistrationResult.EmailConfirmationRequired -> mutableState.update {
                    it.copy(
                        stage = AuthStage.AWAITING_EMAIL_CONFIRMATION,
                        notice = "Открой письмо и подтверди электронную почту",
                    )
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        val validation = validateSignIn(email, password)
        val input = validation.value
        if (input == null) {
            mutableState.update { it.copy(errors = validation.errors, notice = null) }
            return
        }
        submit { gateway.signInParent(input.email, input.password) }
    }

    fun requestPasswordReset(email: String) {
        val cleanEmail = email.trim()
        val emailError = when {
            cleanEmail.isBlank() -> "Введите электронную почту"
            !cleanEmail.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) -> "Проверь адрес электронной почты"
            else -> null
        }
        if (emailError != null) {
            mutableState.update { it.copy(errors = AuthFieldErrors(email = emailError)) }
            return
        }
        submit {
            gateway.sendPasswordReset(cleanEmail)
            mutableState.update {
                it.copy(
                    stage = AuthStage.UNAUTHENTICATED,
                    notice = "Если аккаунт с такой почтой существует, мы отправили письмо",
                    resetDialogVisible = false,
                )
            }
        }
    }

    fun saveNewPassword(password: String, repeat: String) {
        val validation = validateNewPassword(password, repeat)
        val value = validation.value
        if (value == null) {
            mutableState.update { it.copy(errors = validation.errors) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(stage = AuthStage.SUBMITTING, errors = AuthFieldErrors(), notice = null) }
            runCatching { gateway.updatePassword(value) }
                .onSuccess {
                    mutableState.update {
                        it.copy(stage = AuthStage.AUTHENTICATED, notice = "Пароль обновлён")
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(stage = AuthStage.PASSWORD_RECOVERY, errors = AuthFieldErrors(general = authMessage(error)))
                    }
                }
        }
    }

    fun signOut() {
        submit { gateway.signOut() }
    }

    private fun submit(action: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.update { it.copy(stage = AuthStage.SUBMITTING, errors = AuthFieldErrors(), notice = null) }
            runCatching { action() }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            stage = AuthStage.UNAUTHENTICATED,
                            errors = it.errors.copy(general = authMessage(error)),
                        )
                    }
                }
        }
    }

    class Factory(private val gateway: AuthGateway) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(gateway) as T
    }
}
