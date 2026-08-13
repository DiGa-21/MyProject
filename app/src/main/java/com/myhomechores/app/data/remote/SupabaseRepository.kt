package com.myhomechores.app.data.remote

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import com.myhomechores.app.data.local.OutboxEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.myhomechores.app.features.auth.AuthGateway
import com.myhomechores.app.features.auth.AuthSessionState
import com.myhomechores.app.features.auth.RegistrationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.myhomechores.app.features.family.FamilyGateway
import com.myhomechores.app.features.family.InviteCodeResponse
import com.myhomechores.app.features.family.InviteConsumeResponse
import com.myhomechores.app.features.family.InviteConsumeResult
import com.myhomechores.app.features.family.RemoteChild
import com.myhomechores.app.features.family.RemoteParentChild
import com.myhomechores.app.features.family.mapInviteResponse

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

@Serializable
private data class RemoteProfile(val display_name: String)

class SupabaseRepository(private val client: io.github.jan.supabase.SupabaseClient) : AuthGateway, FamilyGateway {
    override val session: Flow<AuthSessionState> = client.auth.sessionStatus.map { status ->
        when (status) {
            SessionStatus.Initializing -> AuthSessionState.Initializing
            is SessionStatus.Authenticated -> AuthSessionState.Authenticated(
                userId = status.session.user?.id.orEmpty(),
                anonymous = status.session.user?.email.isNullOrBlank(),
            )
            is SessionStatus.NotAuthenticated -> AuthSessionState.Unauthenticated
            is SessionStatus.RefreshFailure -> AuthSessionState.RefreshFailed("Сессия закончилась. Войди ещё раз")
        }
    }

    override suspend fun signUpParent(email: String, password: String, displayName: String): RegistrationResult {
        client.auth.signUpWith(
            provider = Email,
            redirectUrl = "myway://auth-callback/email-confirmation",
        ) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("display_name", displayName) }
        }
        return if (client.auth.currentSessionOrNull() == null) {
            RegistrationResult.EmailConfirmationRequired
        } else {
            RegistrationResult.SignedIn
        }
    }

    override suspend fun signInParent(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() = client.auth.signOut()

    override suspend fun sendPasswordReset(email: String) {
        client.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = "myway://auth-callback/password-recovery",
        )
    }

    override suspend fun updatePassword(password: String) {
        client.auth.updateUser { this.password = password }
    }

    override suspend fun parentDisplayName(): String =
        client.from("profiles").select { filter { eq("id", client.auth.currentUserOrNull()?.id.orEmpty()) } }
            .decodeSingle<RemoteProfile>().display_name

    override suspend fun updateParentDisplayName(displayName: String): String {
        client.from("profiles").update({ set("display_name", displayName.trim()) }) {
            filter { eq("id", client.auth.currentUserOrNull()?.id.orEmpty()) }
        }
        return displayName.trim()
    }

    fun currentUser(): UserInfo? = client.auth.currentUserOrNull()

    override suspend fun ensureAnonymousChildSession() {
        val user = client.auth.currentUserOrNull()
        if (user == null) {
            client.auth.signInAnonymously()
        } else {
            require(user.email.isNullOrBlank()) { "На устройстве активен другой аккаунт" }
        }
    }

    override suspend fun createChildProfile(displayName: String, parentLabel: String?): RemoteParentChild =
        client.postgrest.rpc(
            "create_child_profile",
            buildJsonObject {
                put("input_display_name", displayName)
                parentLabel?.let { put("input_parent_label", it) }
            },
        ).decodeSingle()

    override suspend fun childrenForParent(): List<RemoteParentChild> =
        client.from("parent_children").select().decodeList<RemoteParentChild>()

    override suspend fun createInvite(childId: String): InviteCodeResponse =
        client.postgrest.rpc(
            "create_child_invite",
            buildJsonObject { put("input_child_id", childId) },
        ).decodeSingle()

    override suspend fun childProfile(): RemoteChild? =
        client.from("child_profile").select().decodeSingleOrNull<RemoteChild>()

    override suspend fun consumeInvite(code: String): InviteConsumeResult = mapInviteResponse(
        client.postgrest.rpc(
            "consume_child_invite",
            buildJsonObject { put("input_code", code) },
        ).decodeSingle<InviteConsumeResponse>(),
    )

    override suspend fun disconnectChildDevice(childId: String) {
        client.postgrest.rpc(
            "disconnect_child_device",
            buildJsonObject { put("input_child_id", childId) },
        )
    }

    override suspend fun updateChildIdentity(displayName: String, hero: String): RemoteChild =
        client.postgrest.rpc(
            "update_child_identity",
            buildJsonObject {
                put("input_display_name", displayName)
                put("input_hero", hero)
            },
        ).decodeSingle()

    override suspend fun progressForParent(childId: String, date: String): List<com.myhomechores.app.features.family.RemoteChoreProgress> =
        client.postgrest.rpc(
            "parent_child_progress",
            buildJsonObject {
                put("input_child_id", childId)
                put("input_date", date)
            },
        ).decodeList()

    override suspend fun setCompletionAsParent(childId: String, choreKey: String, date: String, completed: Boolean) {
        client.postgrest.rpc(
            "set_child_completion_as_parent",
            buildJsonObject {
                put("input_child_id", childId)
                put("input_chore_key", choreKey)
                put("input_date", date)
                put("input_completed", completed)
            },
        )
    }

    override suspend fun setCompletionAsChild(childId: String, choreKey: String, date: String, completed: Boolean) {
        client.postgrest.rpc(
            "set_child_completion_as_child",
            buildJsonObject {
                put("input_child_id", childId)
                put("input_chore_key", choreKey)
                put("input_date", date)
                put("input_completed", completed)
            },
        )
    }

    suspend fun insertCompletion(completion: RemoteCompletion) {
        client.from("completions").upsert(completion)
    }

    suspend fun pushOutbox(entry: OutboxEntity) {
        when (entry.entityType) {
            "completion" -> {
                val parts = entry.payload.split('|')
                require(parts.size == 4) { "invalid completion outbox payload" }
                if (entry.actor == "PARENT") {
                    error("parent completion requires a child identifier")
                } else {
                    setCompletionAsChild(parts[0], parts[1], parts[2], parts[3].toBooleanStrict())
                }
            }
            "child" -> Unit
            else -> Unit
        }
    }
}
