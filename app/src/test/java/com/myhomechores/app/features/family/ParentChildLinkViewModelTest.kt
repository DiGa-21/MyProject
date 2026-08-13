package com.myhomechores.app.features.family

import com.myhomechores.app.features.auth.AuthSessionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentChildLinkViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: ParentFakeGateway
    private lateinit var viewModel: ParentChildLinkViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gateway = ParentFakeGateway()
        viewModel = ParentChildLinkViewModel(gateway)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun parent_creates_name_and_private_label() = runTest(dispatcher) {
        viewModel.createChild("Саша", "Саша, школа")
        advanceUntilIdle()
        assertEquals("Саша", gateway.createdDisplayName)
        assertEquals("Саша, школа", gateway.createdParentLabel)
    }

    @Test
    fun repeated_create_while_request_is_running_creates_only_one_profile() = runTest(dispatcher) {
        advanceUntilIdle()
        gateway.createChildGate = CompletableDeferred()

        viewModel.createChild("lk", "ghj")
        runCurrent()
        viewModel.createChild("lk", "ghj")
        runCurrent()

        assertEquals(1, gateway.createChildCallCount)
        gateway.createChildGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun repeated_create_after_success_does_not_create_an_identical_profile() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.createChild("lk", "ghj")
        advanceUntilIdle()
        viewModel.createChild("lk", "ghj")
        advanceUntilIdle()

        assertEquals(1, gateway.createChildCallCount)
        assertEquals("Такой профиль ребёнка уже создан", viewModel.state.value.error)
    }

    @Test
    fun new_code_replaces_old_displayed_code() = runTest(dispatcher) {
        gateway.codes.addAll(listOf("111111", "222222"))
        viewModel.createInvite("child-1")
        runCurrent()
        viewModel.createInvite("child-1")
        runCurrent()
        assertEquals("222222", viewModel.state.value.invite?.code)
    }

    @Test
    fun disconnect_clears_link_status() = runTest(dispatcher) {
        gateway.children = listOf(RemoteParentChild("child-1", "family", "device", "Саша"))
        viewModel.refresh()
        advanceUntilIdle()
        viewModel.disconnect("child-1")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.children.single().deviceConnected)
    }

    @Test
    fun selecting_child_loads_its_remote_progress_and_parent_can_undo() = runTest(dispatcher) {
        gateway.children = listOf(
            RemoteParentChild("child-1", "family", "device-1", "Саша"),
            RemoteParentChild("child-2", "family", "device-2", "Маша"),
        )
        gateway.progress["child-2"] = listOf(
            RemoteChoreProgress("teeth", "Почистить зубы", "Здоровье", 2, "", true, "done-1", "PENDING"),
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.selectChild("child-2")
        advanceUntilIdle()
        assertEquals("child-2", viewModel.state.value.selectedChildId)
        assertEquals(true, viewModel.state.value.progress.single().completed)

        viewModel.setCompletion("teeth", completed = false)
        advanceUntilIdle()
        assertEquals(ParentCompletionAction("child-2", "teeth", false), gateway.lastAction)
    }
}

private data class ParentCompletionAction(val childId: String, val choreKey: String, val completed: Boolean)

private class ParentFakeGateway : FamilyGateway {
    override val session = MutableStateFlow<AuthSessionState>(AuthSessionState.Authenticated("parent", false))
    var createdDisplayName: String? = null
    var createdParentLabel: String? = null
    var createChildCallCount = 0
    var createChildGate: CompletableDeferred<Unit>? = null
    var children = emptyList<RemoteParentChild>()
    val codes = ArrayDeque<String>()
    val progress = mutableMapOf<String, List<RemoteChoreProgress>>()
    var lastAction: ParentCompletionAction? = null
    override suspend fun ensureAnonymousChildSession() = Unit
    override suspend fun createChildProfile(displayName: String, parentLabel: String?): RemoteParentChild {
        createChildCallCount += 1
        createChildGate?.await()
        createdDisplayName = displayName
        createdParentLabel = parentLabel
        return RemoteParentChild("child-1", "family", display_name = displayName, parent_label = parentLabel)
    }
    override suspend fun childrenForParent() = children
    override suspend fun createInvite(childId: String) = InviteCodeResponse(codes.removeFirst(), "2099-01-01T00:00:00Z")
    override suspend fun consumeInvite(code: String) = InviteConsumeResult.Invalid
    override suspend fun childProfile(): RemoteChild? = null
    override suspend fun disconnectChildDevice(childId: String) {
        children = children.map { if (it.id == childId) it.copy(user_id = null) else it }
    }
    override suspend fun updateChildIdentity(displayName: String, hero: String) = error("unused")
    override suspend fun progressForParent(childId: String, date: String) = progress[childId].orEmpty()
    override suspend fun setCompletionAsParent(childId: String, choreKey: String, date: String, completed: Boolean) {
        lastAction = ParentCompletionAction(childId, choreKey, completed)
        progress[childId] = progress[childId].orEmpty().map {
            if (it.client_key == choreKey) it.copy(
                completion_id = if (completed) "parent-completion" else it.completion_id,
                status = if (completed) "PENDING" else "CANCELLED",
            ) else it
        }
    }
    override suspend fun setCompletionAsChild(childId: String, choreKey: String, date: String, completed: Boolean) = Unit
}
