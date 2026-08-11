package com.myhomechores.app.data

import androidx.room.withTransaction
import com.myhomechores.app.data.local.AppDatabase
import com.myhomechores.app.data.local.ChildEntity
import com.myhomechores.app.data.local.CompletionEntity
import com.myhomechores.app.data.local.OutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class RoomAppRepository(private val database: AppDatabase) : AppRepository {
    override fun observeChild(): Flow<ChildProfile?> = database.childDao().observeFirst().map { entity ->
        entity?.let { ChildProfile(it.id, it.displayName, it.parentLabel, it.hero) }
    }

    override fun observeChores(childId: String, date: LocalDate): Flow<List<Chore>> =
        database.choreDao().observeForChild(childId).map { entities ->
            entities.map {
                Chore(it.id, it.childId, it.title, it.category, it.reward, it.hint, it.colorArgb, it.required)
            }
        }

    override suspend fun completions(childId: String): List<Completion> =
        database.completionDao().findForChild(childId).map {
            Completion(it.id, it.choreId, it.childId, LocalDate.parse(it.completionDate), it.status)
        }

    override suspend fun completeChore(choreId: String, childId: String, date: LocalDate) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val completionId = UUID.nameUUIDFromBytes("$childId:$choreId:$date".toByteArray()).toString()
            val completion = CompletionEntity(completionId, choreId, childId, date.toString(), CompletionStatus.PENDING, now)
            if (database.completionDao().insertIfMissing(completion) != -1L) {
                database.outboxDao().insert(
                    OutboxEntity(UUID.randomUUID().toString(), "completion", completionId, "UPSERT", "{\"status\":\"PENDING\"}", now)
                )
            }
        }
    }

    override suspend fun undoCompletion(completionId: String, actor: Actor) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.completionDao().updateStatus(completionId, CompletionStatus.CANCELLED, now)
            database.outboxDao().insert(
                OutboxEntity(UUID.randomUUID().toString(), "completion", completionId, "CANCEL", "{\"actor\":\"${actor.name}\"}", now)
            )
        }
    }

    override suspend fun updateChildDisplayName(childId: String, value: String) = updateChild(childId) { it.copy(displayName = value) }

    override suspend fun updateParentLabel(childId: String, value: String?) = updateChild(childId) { it.copy(parentLabel = value) }

    override suspend fun selectHero(childId: String, hero: HeroId) = updateChild(childId) { it.copy(hero = hero) }

    private suspend fun updateChild(childId: String, transform: (ChildEntity) -> ChildEntity) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val current = database.childDao().findById(childId) ?: return@withTransaction
            val updated = transform(current).copy(updatedAt = now)
            database.childDao().upsert(updated)
            database.outboxDao().insert(
                OutboxEntity(UUID.randomUUID().toString(), "child", childId, "UPSERT", "{}", now)
            )
        }
    }
}
