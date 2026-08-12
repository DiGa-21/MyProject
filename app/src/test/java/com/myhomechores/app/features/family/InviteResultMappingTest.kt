package com.myhomechores.app.features.family

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteResultMappingTest {
    @Test
    fun linked_response_maps_to_child_without_parent_label() {
        val mapped = mapInviteResponse(
            InviteConsumeResponse(
                status = "LINKED",
                child_id = "child-1",
                display_name = "Саша",
                hero = "GIRL",
            ),
        )

        assertTrue(mapped is InviteConsumeResult.Linked)
        assertEquals("Саша", (mapped as InviteConsumeResult.Linked).child.displayName)
    }

    @Test
    fun rate_limit_keeps_retry_seconds() {
        assertEquals(
            InviteConsumeResult.RateLimited(240),
            mapInviteResponse(InviteConsumeResponse("RATE_LIMITED", retry_after_seconds = 240)),
        )
    }

    @Test
    fun unknown_or_invalid_status_is_invalid() {
        assertEquals(
            InviteConsumeResult.Invalid,
            mapInviteResponse(InviteConsumeResponse("INVALID")),
        )
    }
}
