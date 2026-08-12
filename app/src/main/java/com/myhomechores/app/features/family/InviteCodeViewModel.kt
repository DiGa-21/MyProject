package com.myhomechores.app.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myhomechores.app.data.AppRepository
import com.myhomechores.app.data.ChildProfile
import com.myhomechores.app.data.HeroId
import com.myhomechores.app.data.remote.authMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InviteCodeUiState(
    val code: String = "",
    val loading: Boolean = false,
    val linkedChild: RemoteChild? = null,
    val error: String? = null,
    val retryAfterSeconds: Int = 0,
)

class InviteCodeViewModel(
    private val gateway: FamilyGateway,
    private val localRepository: AppRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(InviteCodeUiState())
    val state: StateFlow<InviteCodeUiState> = mutableState.asStateFlow()

    fun updateCode(value: String) {
        mutableState.update { it.copy(code = value.filter(Char::isDigit).take(6), error = null) }
    }

    fun submit() {
        val code = mutableState.value.code
        if (code.length != 6 || mutableState.value.retryAfterSeconds > 0) {
            mutableState.update { it.copy(error = "Введи 6 цифр из кода") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            runCatching {
                gateway.ensureAnonymousChildSession()
                gateway.consumeInvite(code)
            }.onSuccess { result ->
                when (result) {
                    is InviteConsumeResult.Linked -> {
                        val profile = ChildProfile(
                            id = result.child.id,
                            displayName = result.child.displayName,
                            parentLabel = null,
                            hero = if (result.child.hero == "GIRL") HeroId.GIRL else HeroId.BOY,
                            heroSelected = false,
                        )
                        localRepository.replaceLinkedChild(profile)
                        mutableState.update { it.copy(loading = false, linkedChild = result.child) }
                    }
                    InviteConsumeResult.Invalid -> mutableState.update {
                        it.copy(loading = false, error = "Код неверный или уже не действует")
                    }
                    is InviteConsumeResult.RateLimited -> {
                        mutableState.update {
                            it.copy(loading = false, retryAfterSeconds = result.retryAfterSeconds)
                        }
                        startCountdown()
                    }
                }
            }.onFailure { error ->
                mutableState.update { it.copy(loading = false, error = authMessage(error)) }
            }
        }
    }

    fun acknowledgeLinked() {
        mutableState.value = InviteCodeUiState()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (mutableState.value.retryAfterSeconds > 0) {
                delay(1_000)
                mutableState.update { it.copy(retryAfterSeconds = (it.retryAfterSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    class Factory(
        private val gateway: FamilyGateway,
        private val localRepository: AppRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InviteCodeViewModel(gateway, localRepository) as T
    }
}
