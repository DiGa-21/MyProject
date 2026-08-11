package com.myhomechores.app.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myhomechores.app.data.remote.RemoteChild
import com.myhomechores.app.data.remote.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InviteCodeUiState(val loading: Boolean = false, val linkedChild: RemoteChild? = null, val error: String? = null)

class InviteCodeViewModel(private val repository: SupabaseRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(InviteCodeUiState())
    val state: StateFlow<InviteCodeUiState> = mutableState.asStateFlow()

    fun consume(code: String) {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            runCatching { repository.consumeInviteCode(code.trim()) }
                .onSuccess { child -> mutableState.update { it.copy(loading = false, linkedChild = child) } }
                .onFailure { error -> mutableState.update { it.copy(loading = false, error = error.message ?: "Код не принят") } }
        }
    }

    class Factory(private val repository: SupabaseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = InviteCodeViewModel(repository) as T
    }
}
