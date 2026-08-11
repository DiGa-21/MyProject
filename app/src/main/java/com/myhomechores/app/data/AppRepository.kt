package com.myhomechores.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class Actor { CHILD, PARENT }

enum class HeroId { BOY, GIRL }

data class ChildProfile(
    val id: String,
    val displayName: String,
    val parentLabel: String?,
    val hero: HeroId,
)

data class Chore(
    val id: String,
    val childId: String,
    val title: String,
    val category: String,
    val reward: Int,
    val hint: String,
    val colorArgb: Long,
    val required: Boolean,
)

enum class CompletionStatus { PENDING, CONFIRMED, CANCELLED }

data class Completion(
    val id: String,
    val choreId: String,
    val childId: String,
    val date: LocalDate,
    val status: CompletionStatus,
)

interface AppRepository {
    fun observeChild(): Flow<ChildProfile?>

    fun observeChores(childId: String, date: LocalDate): Flow<List<Chore>>

    suspend fun completions(childId: String): List<Completion>

    suspend fun completeChore(choreId: String, childId: String, date: LocalDate)

    suspend fun undoCompletion(completionId: String, actor: Actor)

    suspend fun updateChildDisplayName(childId: String, value: String)

    suspend fun updateParentLabel(childId: String, value: String?)

    suspend fun selectHero(childId: String, hero: HeroId)
}
