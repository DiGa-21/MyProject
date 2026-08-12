package com.myhomechores.app.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myhomechores.app.data.remote.authMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate

data class ParentChildLinkUiState(
    val loading: Boolean = false,
    val children: List<RemoteParentChild> = emptyList(),
    val invite: InviteCodeResponse? = null,
    val selectedChildId: String? = null,
    val progress: List<RemoteChoreProgress> = emptyList(),
    val error: String? = null,
)

class ParentChildLinkViewModel(private val gateway: FamilyGateway) : ViewModel() {
    private val mutableState = MutableStateFlow(ParentChildLinkUiState())
    val state: StateFlow<ParentChildLinkUiState> = mutableState.asStateFlow()
    private var progressJob: Job? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            setLoading()
            runCatching { gateway.childrenForParent() }
                .onSuccess { children ->
                    val selected = mutableState.value.selectedChildId
                        ?.takeIf { id -> children.any { it.id == id } }
                        ?: children.firstOrNull()?.id
                    mutableState.update { it.copy(loading = false, children = children, selectedChildId = selected) }
                    selected?.let { scheduleProgressLoad(it) }
                }
                .onFailure(::showFailure)
        }
    }

    fun createChild(displayName: String, parentLabel: String?) {
        val cleanName = displayName.trim()
        if (cleanName.isBlank()) {
            mutableState.update { it.copy(error = "Введи имя ребёнка") }
            return
        }
        viewModelScope.launch {
            setLoading()
            runCatching { gateway.createChildProfile(cleanName, parentLabel?.trim()?.ifBlank { null }) }
                .onSuccess { child ->
                    mutableState.update {
                        it.copy(loading = false, children = it.children + child, selectedChildId = child.id, progress = emptyList())
                    }
                    scheduleProgressLoad(child.id)
                }
                .onFailure(::showFailure)
        }
    }

    fun createInvite(childId: String) {
        viewModelScope.launch {
            setLoading()
            runCatching { gateway.createInvite(childId) }
                .onSuccess { invite ->
                    mutableState.update { it.copy(loading = false, invite = invite) }
                    clearInviteAtExpiry(invite)
                }
                .onFailure(::showFailure)
        }
    }

    fun disconnect(childId: String) {
        viewModelScope.launch {
            setLoading()
            runCatching {
                gateway.disconnectChildDevice(childId)
                gateway.childrenForParent()
            }.onSuccess { children ->
                val selected = mutableState.value.selectedChildId
                    ?.takeIf { id -> children.any { it.id == id } }
                    ?: children.firstOrNull()?.id
                mutableState.update { it.copy(loading = false, children = children, invite = null, selectedChildId = selected) }
                selected?.let { scheduleProgressLoad(it) }
            }.onFailure(::showFailure)
        }
    }

    fun selectChild(childId: String) {
        if (mutableState.value.children.none { it.id == childId }) return
        mutableState.update { it.copy(selectedChildId = childId, progress = emptyList(), error = null) }
        scheduleProgressLoad(childId)
    }

    fun setCompletion(choreKey: String, completed: Boolean) {
        val childId = mutableState.value.selectedChildId ?: return
        viewModelScope.launch {
            setLoading()
            runCatching {
                gateway.setCompletionAsParent(childId, choreKey, LocalDate.now().toString(), completed)
                gateway.progressForParent(childId, LocalDate.now().toString())
            }.onSuccess { progress ->
                mutableState.update { current ->
                    if (current.selectedChildId == childId) current.copy(loading = false, progress = progress) else current
                }
            }.onFailure(::showFailure)
        }
    }

    fun refreshSelectedProgress() {
        mutableState.value.selectedChildId?.let(::scheduleProgressLoad)
    }

    fun clearInvite() {
        mutableState.update { it.copy(invite = null) }
    }

    private suspend fun loadProgress(childId: String) {
        runCatching { gateway.progressForParent(childId, LocalDate.now().toString()) }
            .onSuccess { progress ->
                mutableState.update { current ->
                    if (current.selectedChildId == childId) current.copy(loading = false, progress = progress) else current
                }
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                showFailure(error)
            }
    }

    private fun scheduleProgressLoad(childId: String) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch { loadProgress(childId) }
    }

    private fun setLoading() = mutableState.update { it.copy(loading = true, error = null) }

    private fun clearInviteAtExpiry(invite: InviteCodeResponse) {
        viewModelScope.launch {
            val waitMillis = runCatching {
                (Instant.parse(invite.expires_at).toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0)
            }.getOrDefault(15 * 60 * 1_000L)
            delay(waitMillis)
            mutableState.update { current ->
                if (current.invite == invite) current.copy(invite = null) else current
            }
        }
    }
    private fun showFailure(error: Throwable) = mutableState.update {
        it.copy(loading = false, error = authMessage(error))
    }

    class Factory(private val gateway: FamilyGateway) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ParentChildLinkViewModel(gateway) as T
    }
}
