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

data class ParentProfileUiState(
    val displayName: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

class ParentProfileViewModel(private val gateway: AuthGateway) : ViewModel() {
    private val mutableState = MutableStateFlow(ParentProfileUiState())
    val state: StateFlow<ParentProfileUiState> = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { gateway.parentDisplayName() }
                .onSuccess { name -> mutableState.value = ParentProfileUiState(name, loading = false) }
                .onFailure { error -> mutableState.value = ParentProfileUiState(loading = false, error = authMessage(error)) }
        }
    }

    fun updateName(value: String) {
        val clean = value.trim()
        if (clean.length < 2) return
        viewModelScope.launch {
            mutableState.update { it.copy(displayName = clean, loading = true, error = null) }
            runCatching { gateway.updateParentDisplayName(clean) }
                .onSuccess { saved -> mutableState.value = ParentProfileUiState(saved, loading = false) }
                .onFailure { error -> mutableState.update { it.copy(loading = false, error = authMessage(error)) } }
        }
    }

    class Factory(private val gateway: AuthGateway) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ParentProfileViewModel(gateway) as T
    }
}
