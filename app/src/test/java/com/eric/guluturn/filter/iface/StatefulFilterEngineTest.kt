package com.eric.guluturn.filter.iface

import com.eric.guluturn.filter.models.*
import kotlin.test.*

class StatefulFilterEngineTest {

    private fun restaurant(
        id: String,
        generalTags: List<String> = emptyList(),
        specificTags: List<SpecificTag> = emptyList()
    ) = Restaurant(id, name = id, generalTags = generalTags, specificTags = specificTags)

    @Test
    fun `engine filters and accumulates preferences over multiple rounds`() {
        val engine = StatefulFilterEngine()

        val allRestaurants = listOf(
            restaurant("r1", generalTags = listOf("prefer_fresh_dishes")),
            restaurant("r2", generalTags = listOf("prefer_spicy_dishes")),
            restaurant("r3", generalTags = listOf("prefer_fresh_dishes"), specificTags = listOf(SpecificTag("too_sweet", "negative")))
        )

        // Round 1
        val round1 = engine.updateAndFilter(
            userGeneralTags = listOf("prefer_fresh_dishes"),
            userSpecificTags = emptyList(),
            allRestaurants = allRestaurants
        )
        assertTrue(round1.any { it.id == "r1" })

        // Reject r1
        engine.reject("r1")

        // Round 2 — add more tags
        val round2 = engine.updateAndFilter(
            userGeneralTags = emptyList(),
            userSpecificTags = listOf(SpecificTag("too_sweet", "negative")),
            allRestaurants = allRestaurants
        )
        assertTrue(round2.none { it.id == "r1" }) // r1 was rejected
        assertTrue(round2.any { it.id == "r3" })  // r3 should be recommended now

        // Verify internal state
        val state = engine.getState()
        assertEquals(setOf("r1"), state.rejectedRestaurantIds)
        assertEquals(listOf("prefer_fresh_dishes"), state.accumulatedGeneralTags)
        assertEquals(listOf(SpecificTag("too_sweet", "negative")), state.accumulatedSpecificTags)
    }
}
