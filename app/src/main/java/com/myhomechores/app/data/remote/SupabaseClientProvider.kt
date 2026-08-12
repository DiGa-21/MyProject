package com.myhomechores.app.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    fun create(url: String, publishableKey: String, sessionKey: String) = createSupabaseClient(url, publishableKey) {
        install(Auth) {
            scheme = "myway"
            host = "auth-callback"
            sessionManager = SettingsSessionManager(key = sessionKey)
            autoLoadFromStorage = true
            alwaysAutoRefresh = true
        }
        install(Postgrest)
    }
}
