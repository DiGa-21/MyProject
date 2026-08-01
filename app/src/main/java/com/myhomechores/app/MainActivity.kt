package com.myhomechores.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.myhomechores.app.features.scaffold.ScaffoldScreen
import com.myhomechores.app.ui.theme.MyHomeChoresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyHomeChoresTheme {
                ScaffoldScreen(environment = BuildConfig.APP_ENVIRONMENT)
            }
        }
    }
}

