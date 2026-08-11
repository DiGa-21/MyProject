package com.myhomechores.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myhomechores.app.BuildConfig
import com.myhomechores.app.data.local.AppDatabase
import com.myhomechores.app.data.remote.SupabaseClientProvider
import com.myhomechores.app.data.remote.SupabaseRepository

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) return Result.success()

        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "myhomechores.db").build()
        return try {
            val client = SupabaseClientProvider.create(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            val remote = SupabaseRepository(client)
            database.outboxDao().pending().forEach { entry ->
                try {
                    remote.pushOutbox(entry)
                    database.outboxDao().delete(entry.id)
                } catch (error: Throwable) {
                    database.outboxDao().markAttempt(entry.id)
                    return if (runAttemptCount < 5) Result.retry() else Result.failure()
                }
            }
            Result.success()
        } finally {
            database.close()
        }
    }
}
