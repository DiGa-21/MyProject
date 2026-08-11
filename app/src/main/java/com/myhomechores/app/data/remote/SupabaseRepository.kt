package com.myhomechores.app.data.remote

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RemoteChild(
    val id: String,
    val family_id: String,
    val user_id: String? = null,
    val display_name: String,
    val parent_label: String? = null,
    val hero: String = "BOY",
    val updated_at: String? = null,
)

@Serializable
data class RemoteCompletion(
    val id: String,
    val chore_id: String,
    val child_id: String,
    val completion_date: String,
    val status: String,
    val completed_by: String? = null,
    val updated_at: String? = null,
)

class SupabaseRepository(private val client: io.github.jan.supabase.SupabaseClient) {
    suspend fun signUpParent(email: String, password: String, displayName: String): UserInfo? {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("display_name", displayName) }
        }
        return client.auth.currentUserOrNull()
    }

    suspend fun signInParent(email: String, password: String): UserInfo? {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return client.auth.currentUserOrNull()
    }

    suspend fun signOut() = client.auth.signOut()

    fun currentUser(): UserInfo? = client.auth.currentUserOrNull()

    suspend fun childrenForParent(): List<RemoteChild> =
        client.from("parent_children").select().decodeList<RemoteChild>()

    suspend fun childProfile(): RemoteChild? =
        client.from("child_profile").select().decodeSingleOrNull<RemoteChild>()

    suspend fun consumeInviteCode(code: String): RemoteChild =
        client.postgrest.rpc("consume_invite_code", buildJsonObject { put("input_code", code) })
            .decodeSingle<RemoteChild>()

    suspend fun insertCompletion(completion: RemoteCompletion) {
        client.from("completions").upsert(completion)
    }
}
