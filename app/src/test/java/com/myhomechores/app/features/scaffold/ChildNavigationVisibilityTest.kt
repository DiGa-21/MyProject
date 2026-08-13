package com.myhomechores.app.features.scaffold

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildNavigationVisibilityTest {
    @Test
    fun bottom_navigation_is_hidden_while_an_activity_lesson_is_open() {
        assertFalse(shouldShowChildBottomBar(isActivityLessonOpen = true))
    }

    @Test
    fun bottom_navigation_is_visible_outside_an_activity_lesson() {
        assertTrue(shouldShowChildBottomBar(isActivityLessonOpen = false))
    }
}
