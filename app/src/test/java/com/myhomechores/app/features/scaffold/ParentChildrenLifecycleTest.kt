package com.myhomechores.app.features.scaffold

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Test

class ParentChildrenLifecycleTest {
    @Test
    fun returning_to_children_screen_refreshes_child_profiles() {
        var refreshCount = 0

        refreshParentChildrenOnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
            refreshCount += 1
        }

        assertEquals(1, refreshCount)
    }
}
