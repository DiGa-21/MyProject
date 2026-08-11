package com.myhomechores.app.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    fun create(url: String, publishableKey: String) = createSupabaseClient(url, publishableKey) {
        install(Auth)
        install(Postgrest)
    }
}
