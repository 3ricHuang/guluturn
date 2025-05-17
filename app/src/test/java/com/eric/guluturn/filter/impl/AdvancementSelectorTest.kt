package com.eric.guluturn.filter.impl

import com.eric.guluturn.filter.models.ScoredRestaurant
import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancementSelectorTest {

    private fun scored(id: String, score: Int) =
        ScoredRestaurant(id = id, score = score)

    @Test
    fun `returns all restaurants when count is less than minimum`() {
        val input = listOf(
            scored("A", 8), scored("B", 7), scored("C", 6)
        )
        val result = AdvancementSelector.select(input)
        assertEquals(3, result.size)
    }

    @Test
    fun `selects top 5 when all scores differ`() {
        val input = listOf(
            scored("A", 9), scored("B", 8), scored("C", 7),
            scored("D", 6), scored("E", 5), scored("F", 4), scored("G", 3)
        )
        val result = AdvancementSelector.select(input)
        assertEquals(5, result.size)
        assertEquals(listOf("A", "B", "C", "D", "E"), result.map { it.id })
    }

    @Test
    fun `includes ties at the cutoff up to 7`() {
        val input = listOf(
            scored("A", 9), scored("B", 8), scored("C", 8),
            scored("D", 7), scored("E", 6), scored("F", 6), scored("G", 6), scored("H", 5)
        )
        val result = AdvancementSelector.select(input)
        assertEquals(7, result.size)
        assertEquals(setOf("A", "B", "C", "D", "E", "F", "G"), result.map { it.id }.toSet())
    }

    @Test
    fun `stops at 7 even if more ties exist`() {
        val input = listOf(
            scored("A", 9), scored("B", 9), scored("C", 9),
            scored("D", 8), scored("E", 8), scored("F", 8),
            scored("G", 8), scored("H", 8)
        )
        val result = AdvancementSelector.select(input)
        assertEquals(7, result.size)
    }
}
