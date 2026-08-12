package com.myhomechores.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.myhomechores.app.features.auth.AuthGateway
import com.myhomechores.app.features.auth.AuthSessionState
import com.myhomechores.app.features.auth.RegistrationResult
import com.myhomechores.app.features.family.FamilyGateway
import com.myhomechores.app.features.family.InviteCodeResponse
import com.myhomechores.app.features.family.InviteConsumeResult
import com.myhomechores.app.features.family.RemoteChild
import com.myhomechores.app.features.family.RemoteParentChild
import com.myhomechores.app.features.scaffold.ScaffoldScreen
import com.myhomechores.app.ui.theme.MyHomeChoresTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ScaffoldScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun start_screen_shows_both_roles() {
        composeRule.setContent { MyHomeChoresTheme { ScaffoldScreen(environment = "test") } }
        composeRule.onNodeWithText("Режим ребёнка").assertIsDisplayed()
        composeRule.onNodeWithText("Режим родителя").assertIsDisplayed()
    }

    @Test
    fun parent_role_is_guarded_by_login() {
        composeRule.setContent {
            MyHomeChoresTheme {
                ScaffoldScreen(environment = "test", parentAuthGateway = TestAuthGateway())
            }
        }
        composeRule.onNodeWithText("Режим родителя").performClick()
        composeRule.onNodeWithText("Кабинет родителя").assertIsDisplayed()
        composeRule.onNodeWithText("Войти").assertIsDisplayed()
    }

    @Test
    fun unlinked_child_sees_only_connection_code() {
        composeRule.setContent {
            MyHomeChoresTheme {
                ScaffoldScreen(environment = "test", childFamilyGateway = TestFamilyGateway())
            }
        }
        composeRule.onNodeWithText("Режим ребёнка").performClick()
        composeRule.onNodeWithText("Код подключения").assertIsDisplayed()
    }
}

private class TestAuthGateway : AuthGateway {
    override val session = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
    override suspend fun signUpParent(email: String, password: String, displayName: String) = RegistrationResult.SignedIn
    override suspend fun signInParent(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun sendPasswordReset(email: String) = Unit
    override suspend fun updatePassword(password: String) = Unit
    override suspend fun parentDisplayName() = "Родитель"
    override suspend fun updateParentDisplayName(displayName: String) = displayName
}

private class TestFamilyGateway : FamilyGateway {
    override val session = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
    override suspend fun ensureAnonymousChildSession() = Unit
    override suspend fun createChildProfile(displayName: String, parentLabel: String?) = error("unused")
    override suspend fun childrenForParent() = emptyList<RemoteParentChild>()
    override suspend fun createInvite(childId: String): InviteCodeResponse = error("unused")
    override suspend fun consumeInvite(code: String) = InviteConsumeResult.Invalid
    override suspend fun childProfile(): RemoteChild? = null
    override suspend fun disconnectChildDevice(childId: String) = Unit
    override suspend fun updateChildIdentity(displayName: String, hero: String): RemoteChild = error("unused")
    override suspend fun progressForParent(childId: String, date: String) = emptyList<com.myhomechores.app.features.family.RemoteChoreProgress>()
    override suspend fun setCompletionAsParent(childId: String, choreKey: String, date: String, completed: Boolean) = Unit
    override suspend fun setCompletionAsChild(childId: String, choreKey: String, date: String, completed: Boolean) = Unit
}
