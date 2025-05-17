package com.eric.guluturn.filter.iface

import com.eric.guluturn.filter.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatelessFilterTest {

    private fun restaurant(
        id: String,
        generalTags: List<String> = emptyList(),
        specificTags: List<SpecificTag> = emptyList()
    ) = Restaurant(id = id, name = id, generalTags = generalTags, specificTags = specificTags)

    @Test
    fun `end-to-end filtering and scoring selects correct subset`() {
        val input = FilterInput(
            userGeneralTags = listOf("avoid_spicy_dishes", "prefer_fresh_dishes"),
            userSpecificTags = listOf(SpecificTag("too_sweet", "negative")),
            restaurants = listOf(
                restaurant("r1", generalTags = listOf("prefer_spicy_dishes")),
                restaurant("r2", generalTags = listOf("prefer_fresh_dishes")),
                restaurant("r3", generalTags = listOf("prefer_fresh_dishes"), specificTags = listOf(SpecificTag("too_sweet", "negative"))),
                restaurant("r4", generalTags = listOf("prefer_fresh_dishes")),
                restaurant("r5", generalTags = listOf("prefer_fresh_dishes")),
                restaurant("r6", generalTags = listOf("prefer_fresh_dishes")),
                restaurant("r7", generalTags = listOf("prefer_fresh_dishes"))
            )
        )

        val result = StatelessFilter.filterAndSelect(input)

        // Ensure the final result contains 5 to 7 restaurants
        assertTrue(result.size in 5..7)

        // Restaurant r1 should be excluded due to hard negative tag conflict
        assertTrue(result.none { it.id == "r1" })

        // Restaurant r3 should have the highest score due to double match
        assertEquals("r3", result.maxByOrNull { it.score }?.id)
    }
}
