package com.eric.guluturn.filter.impl

import com.eric.guluturn.filter.models.ScoredRestaurant
import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancementSelectorTest {

    private fun scored(id: String, score: Int) =
        ScoredRestaurant(id = id, score = score)

    @Test
    fun `returns all restaurants when less than 6 available`() {
        val input = listOf(
            scored("A", 8), scored("B", 7), scored("C", 6)
        )
        val result = AdvancementSelector.select(input)
        assertEquals(3, result.size)
    }

    @Test
    fun `selects top 6 when more than 6 available`() {
        val input = listOf(
            scored("A", 9), scored("B", 8), scored("C", 7),
            scored("D", 6), scored("E", 5), scored("F", 4), scored("G", 3)
        )
        val result = AdvancementSelector.select(input)
        assertEquals(6, result.size)
        assertEquals(listOf("A", "B", "C", "D", "E", "F"), result.map { it.id })
    }

    @Test
    fun `always returns 6 highest scored restaurants without tie logic`() {
        val input = listOf(
            scored("A", 10), scored("B", 9), scored("C", 9),
            scored("D", 9), scored("E", 8), scored("F", 8),
            scored("G", 8), scored("H", 7)
        )
        val result = AdvancementSelector.select(input)
        assertEquals(6, result.size)
        assertEquals(listOf("A", "B", "C", "D", "E", "F"), result.map { it.id })
    }
}
