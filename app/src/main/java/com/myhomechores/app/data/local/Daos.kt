package com.myhomechores.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM children ORDER BY updatedAt DESC LIMIT 1")
    fun observeFirst(): Flow<ChildEntity?>

    @Query("SELECT * FROM children WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ChildEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(child: ChildEntity)
}

@Dao
interface ChoreDao {
    @Query("SELECT * FROM chores WHERE childId = :childId ORDER BY required DESC, id")
    fun observeForChild(childId: String): Flow<List<ChoreEntity>>

    @Query("SELECT * FROM chores WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ChoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chores: List<ChoreEntity>)
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions WHERE childId = :childId ORDER BY completionDate DESC, updatedAt DESC")
    suspend fun findForChild(childId: String): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE choreId = :choreId AND completionDate = :date LIMIT 1")
    suspend fun findForChoreAndDate(choreId: String, date: String): CompletionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(completion: CompletionEntity): Long

    @Query("UPDATE completions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: com.myhomechores.app.data.CompletionStatus, updatedAt: Long)
}

@Dao
interface RewardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: RewardEntity)
}

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: OutboxEntity)

    @Query("SELECT * FROM outbox ORDER BY createdAt LIMIT 50")
    suspend fun pending(): List<OutboxEntity>

    @Query("UPDATE outbox SET attempts = attempts + 1 WHERE id = :id")
    suspend fun markAttempt(id: String)

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun delete(id: String)
}
