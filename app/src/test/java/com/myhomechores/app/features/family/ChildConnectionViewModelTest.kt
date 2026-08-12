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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChildConnectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: ChildFakeGateway
    private lateinit var local: ChildFakeLocal
    private lateinit var viewModel: ChildConnectionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gateway = ChildFakeGateway()
        local = ChildFakeLocal()
        viewModel = ChildConnectionViewModel(gateway, local)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun linked_saved_session_asks_for_hero_once() = runTest(dispatcher) {
        gateway.session.value = AuthSessionState.Authenticated("anonymous-1", anonymous = true)
        gateway.child = RemoteChild("remote-1", "family-1", display_name = "Саша", hero = "BOY")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(ChildConnectionStage.NEEDS_HERO, viewModel.state.value.stage)
    }

    @Test
    fun saved_selected_hero_opens_room() = runTest(dispatcher) {
        gateway.session.value = AuthSessionState.Authenticated("anonymous-1", anonymous = true)
        gateway.child = RemoteChild("remote-1", "family-1", display_name = "Саша", hero = "GIRL")
        local.profile = ChildProfile("remote-1", "Саша", null, HeroId.GIRL, heroSelected = true)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(ChildConnectionStage.READY, viewModel.state.value.stage)
    }

    @Test
    fun disconnected_child_returns_to_code_and_clears_local_profile() = runTest(dispatcher) {
        gateway.session.value = AuthSessionState.Authenticated("anonymous-1", anonymous = true)
        gateway.child = null
        local.profile = ChildProfile("old", "Саша", null, HeroId.BOY, true)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(ChildConnectionStage.NEEDS_CODE, viewModel.state.value.stage)
        assertEquals(null, local.profile)
    }

    @Test
    fun ready_child_detects_remote_disconnect_without_reopening_screen() = runTest(dispatcher) {
        gateway.session.value = AuthSessionState.Authenticated("anonymous-1", anonymous = true)
        gateway.child = RemoteChild("remote-1", "family-1", display_name = "Саша", hero = "BOY")
        local.profile = ChildProfile("remote-1", "Саша", null, HeroId.BOY, heroSelected = true)
        viewModel.refresh()
        advanceUntilIdle()
        gateway.child = null

        viewModel.revalidateReadyConnection()
        advanceUntilIdle()

        assertEquals(ChildConnectionStage.NEEDS_CODE, viewModel.state.value.stage)
        assertEquals(null, local.profile)
    }
}

private class ChildFakeGateway : FamilyGateway {
    override val session = MutableStateFlow<AuthSessionState>(AuthSessionState.Initializing)
    var child: RemoteChild? = null
    override suspend fun ensureAnonymousChildSession() = Unit
    override suspend fun createChildProfile(displayName: String, parentLabel: String?) = error("unused")
    override suspend fun childrenForParent() = emptyList<RemoteParentChild>()
    override suspend fun createInvite(childId: String) = error("unused")
    override suspend fun consumeInvite(code: String) = InviteConsumeResult.Invalid
    override suspend fun childProfile() = child
    override suspend fun disconnectChildDevice(childId: String) = Unit
    override suspend fun updateChildIdentity(displayName: String, hero: String): RemoteChild {
        child = child!!.copy(display_name = displayName, hero = hero)
        return child!!
    }
    override suspend fun progressForParent(childId: String, date: String) = emptyList<RemoteChoreProgress>()
    override suspend fun setCompletionAsParent(childId: String, choreKey: String, date: String, completed: Boolean) = Unit
    override suspend fun setCompletionAsChild(childId: String, choreKey: String, date: String, completed: Boolean) = Unit
}

private class ChildFakeLocal : AppRepository {
    var profile: ChildProfile? = null
    override suspend fun replaceLinkedChild(profile: ChildProfile) { this.profile = profile }
    override suspend fun clearLinkedChild() { profile = null }
    override suspend fun createChildProfile(id: String, displayName: String, parentLabel: String?, hero: HeroId) = Unit
    override fun observeChild(): Flow<ChildProfile?> = flowOf(profile)
    override fun observeChores(childId: String, date: LocalDate): Flow<List<Chore>> = flowOf(emptyList())
    override suspend fun completions(childId: String): List<Completion> = emptyList()
    override suspend fun completeChore(choreId: String, childId: String, date: LocalDate) = Unit
    override suspend fun undoCompletion(completionId: String, actor: Actor) = Unit
    override suspend fun updateChildDisplayName(childId: String, value: String) = Unit
    override suspend fun updateParentLabel(childId: String, value: String?) = Unit
    override suspend fun selectHero(childId: String, hero: HeroId) = Unit
}
