package com.myhomechores.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.myhomechores.app.data.sync.SyncScheduler
import com.myhomechores.app.features.scaffold.ScaffoldScreen
import com.myhomechores.app.ui.theme.MyHomeChoresTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val container get() = (application as MyWayApplication).container
    private var passwordRecoveryRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
        SyncScheduler.schedule(this)
        setContent {
            MyHomeChoresTheme {
                ScaffoldScreen(
                    environment = BuildConfig.APP_ENVIRONMENT,
                    repository = container.localRepository,
                    parentAuthGateway = container.parentRemoteRepository,
                    parentFamilyGateway = container.parentRemoteRepository,
                    childFamilyGateway = container.childRemoteRepository,
                    passwordRecoveryRequested = passwordRecoveryRequested,
                    onPasswordRecoveryHandled = { passwordRecoveryRequested = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        passwordRecoveryRequested = intent.data?.path == "/password-recovery"
        container.parentSupabase.handleDeeplinks(intent)
    }
}
