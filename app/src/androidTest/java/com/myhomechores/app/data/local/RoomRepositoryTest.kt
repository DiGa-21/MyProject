package com.myhomechores.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myhomechores.app.data.Actor
import com.myhomechores.app.data.CompletionStatus
import com.myhomechores.app.data.HeroId
import com.myhomechores.app.data.RoomAppRepository
import com.myhomechores.app.data.ChildProfile
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class RoomRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomAppRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomAppRepository(database)
        database.childDao().upsert(ChildEntity("child-1", "Аня", "Младшая", HeroId.BOY, 1L))
        database.choreDao().upsertAll(
            listOf(ChoreEntity("teeth", "child-1", "Почистить зубы", "Здоровье", 2, "Утром и вечером", 0xFFD9F5EAL, true, 1L))
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun profileKeepsChildNameParentLabelAndChildHero() = runBlocking {
        repository.updateChildDisplayName("child-1", "Мила")
        repository.updateParentLabel("child-1", "Мила (старшая)")
        repository.selectHero("child-1", HeroId.GIRL)

        val profile = repository.observeChild().first()
        assertNotNull(profile)
        assertEquals("Мила", profile?.displayName)
        assertEquals("Мила (старшая)", profile?.parentLabel)
        assertEquals(HeroId.GIRL, profile?.hero)
    }

    @Test
    fun completionIsUniquePerChoreAndDateAndCanBeCancelled() = runBlocking {
        val date = LocalDate.of(2026, 8, 11)
        repository.completeChore("teeth", "child-1", date)
        repository.completeChore("teeth", "child-1", date)

        val first = repository.completions("child-1")
        assertEquals(1, first.size)
        assertEquals(CompletionStatus.PENDING, first.single().status)

        repository.undoCompletion(first.single().id, Actor.CHILD)
        assertEquals(CompletionStatus.CANCELLED, repository.completions("child-1").single().status)

        repository.completeChore("teeth", "child-1", date)
        assertEquals(CompletionStatus.PENDING, repository.completions("child-1").single().status)
    }

    @Test
    fun sameChildRefreshPreservesPendingOutboxButRelinkClearsIt() = runBlocking {
        val date = LocalDate.of(2026, 8, 11)
        repository.completeChore("teeth", "child-1", date)
        assertEquals(1, database.outboxDao().pending().count { it.entityType == "completion" })

        repository.replaceLinkedChild(ChildProfile("child-1", "Аня", null, HeroId.BOY, true))
        assertEquals(1, database.outboxDao().pending().count { it.entityType == "completion" })

        repository.replaceLinkedChild(ChildProfile("child-2", "Маша", null, HeroId.GIRL, true))
        assertEquals(0, database.outboxDao().pending().count { it.entityType == "completion" })
    }
}
