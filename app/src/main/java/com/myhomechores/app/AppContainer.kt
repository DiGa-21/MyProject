package com.myhomechores.app

import android.content.Context
import androidx.room.Room
import com.myhomechores.app.data.AppRepository
import com.myhomechores.app.data.RoomAppRepository
import com.myhomechores.app.data.local.AppDatabase
import com.myhomechores.app.data.local.Migration1To2
import com.myhomechores.app.data.local.Migration2To3
import com.myhomechores.app.data.remote.SupabaseClientProvider
import com.myhomechores.app.data.remote.SupabaseRepository
import com.myhomechores.app.data.sync.SyncScheduler

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "myhomechores.db")
        .addMigrations(Migration1To2, Migration2To3)
        .build()

    val localRepository: AppRepository = RoomAppRepository(database) { SyncScheduler.requestNow(context) }
    val parentSupabase = SupabaseClientProvider.create(
        BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        sessionKey = "myway-parent-session",
    )
    val parentRemoteRepository = SupabaseRepository(parentSupabase)
    val childSupabase = SupabaseClientProvider.create(
        BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        sessionKey = "myway-child-session",
    )
    val childRemoteRepository = SupabaseRepository(childSupabase)
}
