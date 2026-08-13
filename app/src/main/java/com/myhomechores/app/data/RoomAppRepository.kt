package com.myhomechores.app.data

import androidx.room.withTransaction
import com.myhomechores.app.data.local.AppDatabase
import com.myhomechores.app.data.local.ChildEntity
import com.myhomechores.app.data.local.CompletionEntity
import com.myhomechores.app.data.local.OutboxEntity
import com.myhomechores.app.data.local.RewardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID

class RoomAppRepository(
    private val database: AppDatabase,
    private val onSyncNeeded: () -> Unit = {},
) : AppRepository {
    override suspend fun createChildProfile(id: String, displayName: String, parentLabel: String?, hero: HeroId) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.childDao().upsert(ChildEntity(id, displayName, parentLabel, hero, updatedAt = now, heroSelected = true))
            database.outboxDao().insert(OutboxEntity(UUID.randomUUID().toString(), "child", id, "UPSERT", "{}", now))
        }
    }

    override fun observeChild(): Flow<ChildProfile?> = database.childDao().observeFirst().map { entity ->
        entity?.let { ChildProfile(it.id, it.displayName, it.parentLabel, it.hero, it.heroSelected) }
    }

    override suspend fun replaceLinkedChild(profile: ChildProfile) {
        database.withTransaction {
            val current = database.childDao().findById(profile.id)
            if (current == null) {
                database.outboxDao().deleteCompletionEntries()
            }
            database.childDao().deleteAll()
            database.childDao().upsert(
                ChildEntity(
                    id = profile.id,
                    displayName = profile.displayName,
                    parentLabel = null,
                    hero = profile.hero,
                    updatedAt = System.currentTimeMillis(),
                    heroSelected = profile.heroSelected,
                ),
            )
        }
        onSyncNeeded()
    }

    override suspend fun clearLinkedChild() {
        database.withTransaction {
            val current = database.childDao().observeFirst().first()
            current?.let { database.completionDao().deleteForChild(it.id) }
            database.outboxDao().deleteCompletionEntries()
            database.childDao().deleteAll()
        }
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
            database.completionDao().upsert(completion)
            database.outboxDao().deleteForEntity("completion", completionId)
            database.outboxDao().insert(
                OutboxEntity(UUID.randomUUID().toString(), "completion", completionId, "UPSERT", "$childId|$choreId|$date|true", now)
            )
        }
        onSyncNeeded()
    }

    override suspend fun undoCompletion(completionId: String, actor: Actor) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val completion = database.completionDao().findById(completionId) ?: return@withTransaction
            database.completionDao().updateStatus(completionId, CompletionStatus.CANCELLED, now)
            database.outboxDao().deleteForEntity("completion", completionId)
            database.outboxDao().insert(
                OutboxEntity(
                    UUID.randomUUID().toString(), "completion", completionId, "CANCEL",
                    "${completion.childId}|${completion.choreId}|${completion.completionDate}|false", now, actor = actor.name,
                )
            )
        }
        onSyncNeeded()
    }

    override suspend fun updateChildDisplayName(childId: String, value: String) = updateChild(childId) { it.copy(displayName = value) }

    override suspend fun updateParentLabel(childId: String, value: String?) = updateChild(childId) { it.copy(parentLabel = value) }

    override suspend fun selectHero(childId: String, hero: HeroId) = updateChild(childId) {
        it.copy(hero = hero, heroSelected = true)
    }

    override fun observeActivityRewardStars(childId: String): Flow<Int> =
        database.rewardDao().observeStarTotal(childId)

    override suspend fun grantDailyActivityReward(
        childId: String,
        activityId: String,
        date: LocalDate,
        stars: Int,
    ): Boolean {
        require(stars > 0) { "Reward must be positive" }
        val rewardId = "activity:$childId:$activityId:$date"
        val inserted = database.rewardDao().insertIfMissing(
            RewardEntity(
                id = rewardId,
                childId = childId,
                completionId = rewardId,
                stars = stars,
                fragments = 0,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return inserted != -1L
    }

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
        onSyncNeeded()
    }
}
