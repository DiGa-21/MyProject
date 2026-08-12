package com.myhomechores.app.features.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun loads_and_persists_parent_name() = runTest(dispatcher) {
        val gateway = ProfileFakeGateway()
        val viewModel = ParentProfileViewModel(gateway)
        advanceUntilIdle()
        assertEquals("Мама", viewModel.state.value.displayName)

        viewModel.updateName("Елена")
        advanceUntilIdle()
        assertEquals("Елена", gateway.savedName)
        assertEquals("Елена", viewModel.state.value.displayName)
    }
}

private class ProfileFakeGateway : AuthGateway {
    override val session = MutableStateFlow<AuthSessionState>(AuthSessionState.Authenticated("parent", false))
    var savedName = "Мама"
    override suspend fun parentDisplayName() = savedName
    override suspend fun updateParentDisplayName(displayName: String): String { savedName = displayName; return displayName }
    override suspend fun signUpParent(email: String, password: String, displayName: String) = RegistrationResult.SignedIn
    override suspend fun signInParent(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun sendPasswordReset(email: String) = Unit
    override suspend fun updatePassword(password: String) = Unit
}
