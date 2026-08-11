package com.myhomechores.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.myhomechores.app.data.RoomAppRepository
import com.myhomechores.app.data.local.AppDatabase
import com.myhomechores.app.data.sync.SyncScheduler
import com.myhomechores.app.features.scaffold.ScaffoldScreen
import com.myhomechores.app.ui.theme.MyHomeChoresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "myhomechores.db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = RoomAppRepository(database)
        SyncScheduler.schedule(this)
        setContent {
            MyHomeChoresTheme {
                ScaffoldScreen(environment = BuildConfig.APP_ENVIRONMENT, repository = repository)
            }
        }
    }
}

