package com.myhomechores.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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

val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE children ADD COLUMN heroSelected INTEGER NOT NULL DEFAULT 0")
    }
}

val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE outbox ADD COLUMN actor TEXT NOT NULL DEFAULT 'CHILD'")
    }
}
