package com.myhomechores.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myhomechores.app.data.remote.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val signedIn: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(private val repository: SupabaseRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState(signedIn = repository.currentUser() != null))
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    fun signUp(email: String, password: String, displayName: String) = runAuth {
        repository.signUpParent(email.trim(), password, displayName.trim())
    }

    fun signIn(email: String, password: String) = runAuth {
        repository.signInParent(email.trim(), password)
    }

    fun signOut() = runAuth { repository.signOut() }

    private fun runAuth(action: suspend () -> Any?) {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            runCatching { action() }
                .onSuccess { mutableState.update { it.copy(loading = false, signedIn = repository.currentUser() != null) } }
                .onFailure { error -> mutableState.update { it.copy(loading = false, error = error.message ?: "Не удалось выполнить действие") } }
        }
    }

    class Factory(private val repository: SupabaseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
    }
}
