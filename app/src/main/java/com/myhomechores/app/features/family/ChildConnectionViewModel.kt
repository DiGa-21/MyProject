package com.myhomechores.app.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myhomechores.app.data.AppRepository
import com.myhomechores.app.data.ChildProfile
import com.myhomechores.app.data.HeroId
import com.myhomechores.app.data.remote.authMessage
import com.myhomechores.app.features.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChildConnectionStage { CHECKING, NEEDS_CODE, NEEDS_HERO, READY }

data class ChildConnectionUiState(
    val stage: ChildConnectionStage = ChildConnectionStage.CHECKING,
    val profile: ChildProfile? = null,
    val error: String? = null,
)

class ChildConnectionViewModel(
    private val gateway: FamilyGateway,
    private val localRepository: AppRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChildConnectionUiState())
    val state: StateFlow<ChildConnectionUiState> = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            mutableState.update { it.copy(stage = ChildConnectionStage.CHECKING, error = null) }
            when (val session = gateway.session.first { it !is AuthSessionState.Initializing }) {
                is AuthSessionState.Authenticated -> {
                    if (!session.anonymous) {
                        mutableState.update { it.copy(stage = ChildConnectionStage.NEEDS_CODE) }
                        return@launch
                    }
                    try {
                        applyRemoteProfile(gateway.childProfile())
                    } catch (error: Throwable) {
                        mutableState.update {
                            it.copy(stage = ChildConnectionStage.NEEDS_CODE, error = authMessage(error))
                        }
                    }
                }
                else -> mutableState.update { it.copy(stage = ChildConnectionStage.NEEDS_CODE) }
            }
        }
    }

    fun revalidateReadyConnection() {
        if (mutableState.value.stage != ChildConnectionStage.READY) return
        viewModelScope.launch {
            runCatching { gateway.childProfile() }
                .onSuccess { remote ->
                    if (remote == null) applyRemoteProfile(null)
                }
        }
    }

    fun chooseHero(hero: HeroId) {
        val profile = mutableState.value.profile ?: return
        viewModelScope.launch {
            runCatching {
                val remote = gateway.updateChildIdentity(profile.displayName, hero.name)
                val selected = ChildProfile(remote.id, remote.displayName, null, hero, heroSelected = true)
                localRepository.replaceLinkedChild(selected)
                selected
            }.onSuccess { selected ->
                mutableState.update { it.copy(stage = ChildConnectionStage.READY, profile = selected, error = null) }
            }.onFailure { error ->
                mutableState.update { it.copy(error = authMessage(error)) }
            }
        }
    }

    fun updateName(value: String) {
        val cleanName = value.trim()
        val profile = mutableState.value.profile ?: return
        if (cleanName.length < 2) return
        viewModelScope.launch {
            runCatching {
                val remote = gateway.updateChildIdentity(cleanName, profile.hero.name)
                val updated = profile.copy(displayName = remote.displayName)
                localRepository.replaceLinkedChild(updated)
                updated
            }.onSuccess { updated ->
                mutableState.update { it.copy(profile = updated, error = null) }
            }.onFailure { error ->
                mutableState.update { it.copy(error = authMessage(error)) }
            }
        }
    }

    private suspend fun applyRemoteProfile(remote: RemoteChild?) {
        if (remote == null) {
            localRepository.clearLinkedChild()
            mutableState.update { it.copy(stage = ChildConnectionStage.NEEDS_CODE, profile = null) }
            return
        }
        val current = localRepository.observeChild().first()
        val hero = if (remote.hero == "GIRL") HeroId.GIRL else HeroId.BOY
        val profile = ChildProfile(
            id = remote.id,
            displayName = remote.displayName,
            parentLabel = null,
            hero = hero,
            heroSelected = current?.id == remote.id && current.heroSelected,
        )
        localRepository.replaceLinkedChild(profile)
        mutableState.update {
            it.copy(
                stage = if (profile.heroSelected) ChildConnectionStage.READY else ChildConnectionStage.NEEDS_HERO,
                profile = profile,
            )
        }
    }

    class Factory(
        private val gateway: FamilyGateway,
        private val localRepository: AppRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChildConnectionViewModel(gateway, localRepository) as T
    }
}
