package com.myhomechores.app.features.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: FakeAuthGateway
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gateway = FakeAuthGateway()
        viewModel = AuthViewModel(gateway)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saved_session_opens_parent_without_form() = runTest(dispatcher) {
        gateway.sessionState.value = AuthSessionState.Authenticated("parent-1", anonymous = false)

        advanceUntilIdle()

        assertEquals(AuthStage.AUTHENTICATED, viewModel.state.value.stage)
    }

    @Test
    fun invalid_registration_never_calls_gateway() = runTest(dispatcher) {
        viewModel.signUp("", "bad", "123", "456")

        advanceUntilIdle()

        assertEquals(0, gateway.signUpCalls)
        assertEquals("Введите имя", viewModel.state.value.errors.displayName)
    }

    @Test
    fun confirmation_required_is_visible() = runTest(dispatcher) {
        gateway.registrationResult = RegistrationResult.EmailConfirmationRequired

        viewModel.signUp("Мама", "parent@example.com", "123456", "123456")
        advanceUntilIdle()

        assertEquals(AuthStage.AWAITING_EMAIL_CONFIRMATION, viewModel.state.value.stage)
    }

    @Test
    fun selecting_auth_tab_after_confirmation_returns_to_form() = runTest(dispatcher) {
        gateway.registrationResult = RegistrationResult.EmailConfirmationRequired
        viewModel.signUp("Parent", "parent@example.com", "123456", "123456")
        advanceUntilIdle()

        viewModel.selectTab(AuthTab.SIGN_IN)

        assertEquals(AuthStage.UNAUTHENTICATED, viewModel.state.value.stage)
    }

    @Test
    fun reset_confirmation_is_neutral() = runTest(dispatcher) {
        viewModel.requestPasswordReset("unknown@example.com")

        advanceUntilIdle()

        assertEquals(
            "Если аккаунт с такой почтой существует, мы отправили письмо",
            viewModel.state.value.notice,
        )
    }
}

private class FakeAuthGateway : AuthGateway {
    val sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Initializing)
    override val session: Flow<AuthSessionState> = sessionState
    var signUpCalls = 0
    var registrationResult: RegistrationResult = RegistrationResult.SignedIn

    override suspend fun signUpParent(
        email: String,
        password: String,
        displayName: String,
    ): RegistrationResult {
        signUpCalls += 1
        return registrationResult
    }

    override suspend fun signInParent(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun sendPasswordReset(email: String) = Unit
    override suspend fun updatePassword(password: String) = Unit
    override suspend fun parentDisplayName() = "Мама"
    override suspend fun updateParentDisplayName(displayName: String) = displayName
}
