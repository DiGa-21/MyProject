package com.myhomechores.app.features.family

import com.myhomechores.app.data.Actor
import com.myhomechores.app.data.AppRepository
import com.myhomechores.app.data.ChildProfile
import com.myhomechores.app.data.Chore
import com.myhomechores.app.data.Completion
import com.myhomechores.app.data.HeroId
import com.myhomechores.app.features.auth.AuthSessionState
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InviteCodeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: FakeFamilyGateway
    private lateinit var local: FakeLocalRepository
    private lateinit var viewModel: InviteCodeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gateway = FakeFamilyGateway()
        local = FakeLocalRepository()
        viewModel = InviteCodeViewModel(gateway, local)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun only_six_digits_are_kept() {
        viewModel.updateCode("12a34567")
        assertEquals("123456", viewModel.state.value.code)
    }

    @Test
    fun successful_code_imports_remote_child() = runTest(dispatcher) {
        gateway.consumeResult = InviteConsumeResult.Linked(
            RemoteChild("remote-1", "family-1", display_name = "Саша", hero = "GIRL"),
        )
        viewModel.updateCode("123456")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("remote-1", local.lastReplacement?.id)
        assertEquals(HeroId.GIRL, local.lastReplacement?.hero)
        assertEquals(false, local.lastReplacement?.heroSelected)
    }

    @Test
    fun rate_limit_disables_submit_and_counts_down() = runTest(dispatcher) {
        gateway.consumeResult = InviteConsumeResult.RateLimited(300)
        viewModel.updateCode("000000")

        viewModel.submit()
        runCurrent()

        assertEquals(300, viewModel.state.value.retryAfterSeconds)
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(299, viewModel.state.value.retryAfterSeconds)
    }
}

private class FakeFamilyGateway : FamilyGateway {
    override val session = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
    var consumeResult: InviteConsumeResult = InviteConsumeResult.Invalid
    override suspend fun ensureAnonymousChildSession() = Unit
    override suspend fun createChildProfile(displayName: String, parentLabel: String?) = error("unused")
    override suspend fun childrenForParent() = emptyList<RemoteParentChild>()
    override suspend fun createInvite(childId: String) = error("unused")
    override suspend fun consumeInvite(code: String) = consumeResult
    override suspend fun childProfile(): RemoteChild? = null
    override suspend fun disconnectChildDevice(childId: String) = Unit
    override suspend fun updateChildIdentity(displayName: String, hero: String) = error("unused")
    override suspend fun progressForParent(childId: String, date: String) = emptyList<RemoteChoreProgress>()
    override suspend fun setCompletionAsParent(childId: String, choreKey: String, date: String, completed: Boolean) = Unit
    override suspend fun setCompletionAsChild(childId: String, choreKey: String, date: String, completed: Boolean) = Unit
}

private class FakeLocalRepository : AppRepository {
    var lastReplacement: ChildProfile? = null
    override suspend fun replaceLinkedChild(profile: ChildProfile) { lastReplacement = profile }
    override suspend fun clearLinkedChild() = Unit
    override suspend fun createChildProfile(id: String, displayName: String, parentLabel: String?, hero: HeroId) = Unit
    override fun observeChild(): Flow<ChildProfile?> = flowOf(lastReplacement)
    override fun observeChores(childId: String, date: LocalDate): Flow<List<Chore>> = flowOf(emptyList())
    override suspend fun completions(childId: String): List<Completion> = emptyList()
    override suspend fun completeChore(choreId: String, childId: String, date: LocalDate) = Unit
    override suspend fun undoCompletion(completionId: String, actor: Actor) = Unit
    override suspend fun updateChildDisplayName(childId: String, value: String) = Unit
    override suspend fun updateParentLabel(childId: String, value: String?) = Unit
    override suspend fun selectHero(childId: String, hero: HeroId) = Unit
}
