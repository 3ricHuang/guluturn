package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import kotlin.test.Test
import kotlin.test.assertEquals

class HardFilterTest {

    private val shop1 = Restaurant(
        id = "r1",
        name = "Pork Heaven",
        general_tags = listOf("prefer_pork"),
        specific_tags = emptyList()
    )

    private val shop2 = Restaurant(
        id = "r2",
        name = "Halal King",
        general_tags = listOf("no_halal_options"), // hard negative
        specific_tags = emptyList()
    )

    private val shop3 = Restaurant(
        id = "r3",
        name = "Veggie Garden",
        general_tags = listOf("prefer_vegetarian"),
        specific_tags = emptyList()
    )

    @Test
    fun `returns all restaurants when user has no general tags`() {
        val result = HardFilter.apply(
            userGeneralTags = emptyList(),
            candidates = listOf(shop1, shop2, shop3)
        )
        assertEquals(3, result.size)
    }

    @Test
    fun `does not exclude restaurants for soft negative tag`() {
        val result = HardFilter.apply(
            userGeneralTags = listOf("avoid_pork"), // soft
            candidates = listOf(shop1, shop2, shop3)
        )
        assertEquals(3, result.size)
    }

    @Test
    fun `excludes restaurants that conflict with hard negative tags`() {
        val result = HardFilter.apply(
            userGeneralTags = listOf("no_halal_options"), // hard negative
            candidates = listOf(shop1, shop2, shop3)
        )
        assertEquals(2, result.size)
        val remainingIds = result.map { it.id }
        assert(!remainingIds.contains("r2"))
    }

    @Test
    fun `retains restaurants that do not conflict`() {
        val result = HardFilter.apply(
            userGeneralTags = listOf("no_halal_options"),
            candidates = listOf(shop3)
        )
        assertEquals(1, result.size)
        assertEquals("r3", result[0].id)
    }
}
