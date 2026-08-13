package com.myhomechores.app.features.scaffold

import com.myhomechores.app.data.AppRepository
import com.myhomechores.app.data.Actor
import com.myhomechores.app.data.ChildProfile
import com.myhomechores.app.data.Chore
import com.myhomechores.app.data.Completion
import com.myhomechores.app.data.HeroId
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
class ActivityRewardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun completion_grants_five_stars_only_once_for_the_day() = runTest(dispatcher) {
        val repository = RewardFakeRepository()
        val viewModel = ScaffoldViewModel(repository)
        advanceUntilIdle()

        viewModel.completeActivity("english-nature", LocalDate.of(2026, 8, 13), 5)
        advanceUntilIdle()
        assertEquals(5, viewModel.state.value.activityRewardStars)
        assertEquals(ActivityRewardStatus.Granted, viewModel.state.value.activityRewardStatus)

        viewModel.beginActivity()
        viewModel.completeActivity("english-nature", LocalDate.of(2026, 8, 13), 5)
        advanceUntilIdle()
        assertEquals(5, viewModel.state.value.activityRewardStars)
        assertEquals(ActivityRewardStatus.AlreadyClaimed, viewModel.state.value.activityRewardStatus)
    }
}

private class RewardFakeRepository : AppRepository {
    private val rewards = MutableStateFlow(0)
    private val claimed = mutableSetOf<String>()

    override fun observeChild(): Flow<ChildProfile?> = flowOf(
        ChildProfile("child-1", "Мила", null, HeroId.GIRL, true),
    )
    override suspend fun createChildProfile(id: String, displayName: String, parentLabel: String?, hero: HeroId) = Unit
    override suspend fun replaceLinkedChild(profile: ChildProfile) = Unit
    override suspend fun clearLinkedChild() = Unit
    override fun observeChores(childId: String, date: LocalDate): Flow<List<Chore>> = flowOf(emptyList())
    override suspend fun completions(childId: String): List<Completion> = emptyList()
    override suspend fun completeChore(choreId: String, childId: String, date: LocalDate) = Unit
    override suspend fun undoCompletion(completionId: String, actor: Actor) = Unit
    override suspend fun updateChildDisplayName(childId: String, value: String) = Unit
    override suspend fun updateParentLabel(childId: String, value: String?) = Unit
    override suspend fun selectHero(childId: String, hero: HeroId) = Unit

    override fun observeActivityRewardStars(childId: String): Flow<Int> = rewards

    override suspend fun grantDailyActivityReward(
        childId: String,
        activityId: String,
        date: LocalDate,
        stars: Int,
    ): Boolean {
        val key = "$childId:$activityId:$date"
        if (!claimed.add(key)) return false
        rewards.value += stars
        return true
    }
}
