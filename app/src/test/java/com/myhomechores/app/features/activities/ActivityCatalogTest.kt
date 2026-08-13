package com.myhomechores.app.features.activities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityCatalogTest {
    @Test
    fun catalog_contains_six_approved_activities_and_only_english_is_available() {
        assertEquals(
            listOf("Английский", "Математика", "Медитация", "Дыхание", "Природа", "Обо мне"),
            tryActivities.map { it.title },
        )
        assertTrue(tryActivities.first { it.id == "english" }.available)
        assertFalse(tryActivities.filterNot { it.id == "english" }.any { it.available })
    }
}
