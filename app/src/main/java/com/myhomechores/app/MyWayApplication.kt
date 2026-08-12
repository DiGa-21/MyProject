package com.myhomechores.app

import android.app.Application

class MyWayApplication : Application() {
    val container by lazy { AppContainer(applicationContext) }
}
