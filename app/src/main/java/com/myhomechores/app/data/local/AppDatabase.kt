package com.myhomechores.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.myhomechores.app.data.CompletionStatus
import com.myhomechores.app.data.HeroId

class AppDatabaseConverters {
    @TypeConverter
    fun heroToString(value: HeroId): String = value.name

    @TypeConverter
    fun stringToHero(value: String): HeroId = HeroId.valueOf(value)

    @TypeConverter
    fun statusToString(value: CompletionStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): CompletionStatus = CompletionStatus.valueOf(value)
}

@Database(
    entities = [ChildEntity::class, ChoreEntity::class, CompletionEntity::class, RewardEntity::class, OutboxEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AppDatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun choreDao(): ChoreDao
    abstract fun completionDao(): CompletionDao
    abstract fun rewardDao(): RewardDao
    abstract fun outboxDao(): OutboxDao
}
