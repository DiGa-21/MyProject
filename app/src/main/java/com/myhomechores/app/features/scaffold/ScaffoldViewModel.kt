package com.myhomechores.app.features.scaffold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myhomechores.app.data.Actor
import com.myhomechores.app.data.AppRepository
import com.myhomechores.app.data.ChildProfile
import com.myhomechores.app.data.Completion
import com.myhomechores.app.data.HeroId
import com.myhomechores.app.domain.model.AppRole
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScaffoldUiState(
    val selectedRole: AppRole? = null,
    val childCompletedIds: Set<String> = emptySet(),
    val childName: String = "Алекс",
    val parentName: String = "Родитель",
)

class ScaffoldViewModel(private val repository: AppRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ScaffoldUiState())
    val state: StateFlow<ScaffoldUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.observeChild().first() == null) {
                repository.createChildProfile("local-child", mutableState.value.childName)
            }
            repository.observeChild().collectLatest { profile ->
                if (profile != null) {
                    mutableState.update { it.copy(childName = profile.displayName) }
                    refreshCompletions(profile.id)
                }
            }
        }
    }

    fun selectRole(role: AppRole?) {
        mutableState.update { it.copy(selectedRole = role) }
    }

    fun updateChildName(value: String) {
        mutableState.update { it.copy(childName = value) }
        viewModelScope.launch {
            repository.observeChild().first()?.id?.let { repository.updateChildDisplayName(it, value) }
        }
    }

    fun updateParentName(value: String) {
        mutableState.update { it.copy(parentName = value) }
    }

    fun updateChildCompletedIds(next: Set<String>) {
        val previous = mutableState.value.childCompletedIds
        mutableState.update { it.copy(childCompletedIds = next) }
        viewModelScope.launch {
            val profile = repository.observeChild().first() ?: run {
                repository.createChildProfile("local-child", mutableState.value.childName)
                repository.observeChild().first()
            }
            if (profile == null) return@launch
            val date = LocalDate.now()
            (next - previous).forEach { repository.completeChore(it, profile.id, date) }
            val removed = previous - next
            if (removed.isNotEmpty()) {
                repository.completions(profile.id)
                    .filter { it.choreId in removed && it.date == date && it.status != com.myhomechores.app.data.CompletionStatus.CANCELLED }
                    .forEach { repository.undoCompletion(it.id, Actor.CHILD) }
            }
        }
    }

    fun selectHero(hero: HeroId) {
        viewModelScope.launch { repository.observeChild().first()?.id?.let { repository.selectHero(it, hero) } }
    }

    private suspend fun refreshCompletions(childId: String) {
        val today = LocalDate.now()
        val ids = repository.completions(childId)
            .filter { it.date == today && it.status != com.myhomechores.app.data.CompletionStatus.CANCELLED }
            .map(Completion::choreId)
            .toSet()
        mutableState.update { it.copy(childCompletedIds = ids) }
    }

    class Factory(private val repository: AppRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ScaffoldViewModel(repository) as T
    }
}

object NoOpAppRepository : AppRepository {
    override suspend fun createChildProfile(id: String, displayName: String, parentLabel: String?, hero: HeroId) = Unit
    override fun observeChild() = flowOf<ChildProfile?>(null)
    override fun observeChores(childId: String, date: LocalDate) = flowOf(emptyList<com.myhomechores.app.data.Chore>())
    override suspend fun completions(childId: String) = emptyList<Completion>()
    override suspend fun completeChore(choreId: String, childId: String, date: LocalDate) = Unit
    override suspend fun undoCompletion(completionId: String, actor: Actor) = Unit
    override suspend fun updateChildDisplayName(childId: String, value: String) = Unit
    override suspend fun updateParentLabel(childId: String, value: String?) = Unit
    override suspend fun selectHero(childId: String, hero: HeroId) = Unit
}
