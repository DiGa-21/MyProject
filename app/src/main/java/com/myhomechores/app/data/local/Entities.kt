package com.myhomechores.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myhomechores.app.data.CompletionStatus
import com.myhomechores.app.data.HeroId

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val parentLabel: String?,
    val hero: HeroId,
    val updatedAt: Long,
)

@Entity(
    tableName = "chores",
    indices = [Index(value = ["childId"])]
)
data class ChoreEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val title: String,
    val category: String,
    val reward: Int,
    val hint: String,
    val colorArgb: Long,
    val required: Boolean,
    val updatedAt: Long,
)

@Entity(
    tableName = "completions",
    indices = [
        Index(value = ["choreId", "completionDate"], unique = true),
        Index(value = ["childId", "completionDate"]),
    ]
)
data class CompletionEntity(
    @PrimaryKey val id: String,
    val choreId: String,
    val childId: String,
    val completionDate: String,
    val status: CompletionStatus,
    val updatedAt: Long,
)

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val completionId: String,
    val stars: Int,
    val fragments: Int,
    val updatedAt: Long,
)

@Entity(tableName = "outbox", indices = [Index(value = ["entityId", "operation"], unique = true)])
data class OutboxEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
)
