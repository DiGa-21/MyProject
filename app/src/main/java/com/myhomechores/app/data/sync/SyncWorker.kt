package com.myhomechores.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myhomechores.app.BuildConfig
import com.myhomechores.app.data.local.AppDatabase
import com.myhomechores.app.data.local.Migration1To2
import com.myhomechores.app.data.local.Migration2To3
import com.myhomechores.app.data.remote.SupabaseClientProvider
import com.myhomechores.app.data.remote.SupabaseRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) return Result.success()

        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "myhomechores.db")
            .addMigrations(Migration1To2, Migration2To3)
            .build()
        return try {
            val clients = mutableMapOf<String, io.github.jan.supabase.SupabaseClient>()
            database.outboxDao().pending().forEach { entry ->
                try {
                    val sessionKey = if (entry.actor == "PARENT") "myway-parent-session" else "myway-child-session"
                    val client = clients.getOrPut(sessionKey) {
                        SupabaseClientProvider.create(
                            BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY, sessionKey,
                        )
                    }
                    client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
                    val remote = SupabaseRepository(client)
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
