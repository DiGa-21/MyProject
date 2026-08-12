package com.myhomechores.app.features.family

import com.myhomechores.app.features.auth.AuthSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class RemoteChild(
    val id: String,
    val family_id: String,
    val user_id: String? = null,
    val display_name: String,
    val hero: String = "BOY",
    val updated_at: String? = null,
) {
    val displayName: String get() = display_name
}

@Serializable
data class RemoteParentChild(
    val id: String,
    val family_id: String,
    val user_id: String? = null,
    val display_name: String,
    val parent_label: String? = null,
    val hero: String = "BOY",
    val updated_at: String? = null,
) {
    val deviceConnected: Boolean get() = user_id != null
}

@Serializable
data class InviteCodeResponse(
    val code: String,
    val expires_at: String,
)

@Serializable
data class InviteConsumeResponse(
    val status: String,
    val child_id: String? = null,
    val family_id: String? = null,
    val display_name: String? = null,
    val hero: String? = null,
    val retry_after_seconds: Int = 0,
)

@Serializable
data class RemoteChoreProgress(
    val client_key: String,
    val title: String,
    val category: String,
    val reward: Int,
    val hint: String,
    val required: Boolean,
    val completion_id: String? = null,
    val status: String? = null,
) {
    val completed: Boolean get() = completion_id != null && status != "CANCELLED"
}

sealed interface InviteConsumeResult {
    data class Linked(val child: RemoteChild) : InviteConsumeResult
    data object Invalid : InviteConsumeResult
    data class RateLimited(val retryAfterSeconds: Int) : InviteConsumeResult
}

fun mapInviteResponse(response: InviteConsumeResponse): InviteConsumeResult = when (response.status) {
    "LINKED" -> {
        val id = response.child_id
        val name = response.display_name
        if (id == null || name == null) {
            InviteConsumeResult.Invalid
        } else {
            InviteConsumeResult.Linked(
                RemoteChild(
                    id = id,
                    family_id = response.family_id.orEmpty(),
                    display_name = name,
                    hero = response.hero ?: "BOY",
                ),
            )
        }
    }
    "RATE_LIMITED" -> InviteConsumeResult.RateLimited(response.retry_after_seconds.coerceAtLeast(1))
    else -> InviteConsumeResult.Invalid
}

interface FamilyGateway {
    val session: Flow<AuthSessionState>

    suspend fun ensureAnonymousChildSession()
    suspend fun createChildProfile(displayName: String, parentLabel: String?): RemoteParentChild
    suspend fun childrenForParent(): List<RemoteParentChild>
    suspend fun createInvite(childId: String): InviteCodeResponse
    suspend fun consumeInvite(code: String): InviteConsumeResult
    suspend fun childProfile(): RemoteChild?
    suspend fun disconnectChildDevice(childId: String)
    suspend fun updateChildIdentity(displayName: String, hero: String): RemoteChild
    suspend fun progressForParent(childId: String, date: String): List<RemoteChoreProgress>
    suspend fun setCompletionAsParent(childId: String, choreKey: String, date: String, completed: Boolean)
    suspend fun setCompletionAsChild(childId: String, choreKey: String, date: String, completed: Boolean)
}
