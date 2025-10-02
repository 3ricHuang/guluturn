package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.filter.registry.TagRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class TagScorerTest {

    private fun restaurant(
        id: String,
        generalTags: List<String> = emptyList(),
        specificTags: List<SpecificTag> = emptyList()
    ) = Restaurant(id, name = id, general_tags = generalTags, specific_tags = specificTags)

    @Test
    fun debugTagRegistry() {
        println("---------- TAG REGISTRY DEBUG ----------")

        val tag = "avoid_spicy_dishes"
        val opposite = TagRegistry.oppositeMap[tag]

        println("Loaded tag metadata: ${TagRegistry.get(tag)}")
        println("Conflict map[$tag] = $opposite")

        println("All conflict keys (sample):")
        TagRegistry.oppositeMap.keys.take(5).forEach { k ->
            println("  $k → ${TagRegistry.oppositeMap[k]}")
        }

        println("Total tags loaded: ${TagRegistry.oppositeMap.size}")
        println("----------------------------------------")
    }

    @Test
    fun debugConflictMap() {
        println("conflictMap[avoid_spicy_dishes] = ${TagRegistry.oppositeMap["avoid_spicy_dishes"]}")
        println("TagRegistry.get('avoid_spicy_dishes') = ${TagRegistry.get("avoid_spicy_dishes")}")
    }

    @Test
    fun `scores general tags positively for preference match`() {
        val result = TagScorer.score(
            userGeneralTags = listOf("prefer_fresh_dishes"),
            userSpecificTags = emptyList(),
            candidates = listOf(
                restaurant("r1", generalTags = listOf("prefer_fresh_dishes")),
                restaurant("r2", generalTags = emptyList())
            )
        )
        assertEquals(2, result[0].score)
        assertEquals(0, result[1].score)
    }

    @Test
    fun `scores general tags negatively for avoid match`() {
        val result = TagScorer.score(
            userGeneralTags = listOf("avoid_spicy_dishes"),
            userSpecificTags = emptyList(),
            candidates = listOf(
                restaurant("r1", generalTags = listOf("prefer_spicy_dishes")),
                restaurant("r2", generalTags = emptyList())
            )
        )
        assertEquals(0,  result[0].score)
        assertEquals(-2, result[1].score)
    }

    @Test
    fun `scores specific tags positively when polarity matches`() {
        val result = TagScorer.score(
            userGeneralTags = emptyList(),
            userSpecificTags = listOf(SpecificTag("too_salty", "negative")),
            candidates = listOf(
                restaurant("r1", specificTags = listOf(SpecificTag("too_salty", "negative"))),
                restaurant("r2", specificTags = emptyList())
            )
        )
        assertEquals(3, result[0].score)
        assertEquals(0, result[1].score)
    }

    @Test
    fun `scores specific tags negatively when polarity mismatches`() {
        val result = TagScorer.score(
            userGeneralTags = emptyList(),
            userSpecificTags = listOf(SpecificTag("too_salty", "negative")),
            candidates = listOf(
                restaurant("r1", specificTags = listOf(SpecificTag("too_salty", "positive"))),
                restaurant("r2", specificTags = emptyList())
            )
        )
        assertEquals(0, result[0].score)
        assertEquals(-3, result[1].score)
    }

    @Test
    fun `mixed tag scoring accumulates correctly`() {
        val result = TagScorer.score(
            userGeneralTags = listOf("prefer_fresh_dishes"),
            userSpecificTags = listOf(SpecificTag("too_sweet", "negative")),
            candidates = listOf(
                restaurant(
                    "r1",
                    generalTags = listOf("prefer_fresh_dishes"),
                    specificTags = listOf(SpecificTag("too_sweet", "negative"))
                )
            )
        )
        assertEquals(2 + 3, result[0].score)
    }

    @Test
    fun `avoid tag matches restaurant avoid tag adds points`() {
        val result = TagScorer.score(
            userGeneralTags = listOf("avoid_spicy_dishes"),
            userSpecificTags = emptyList(),
            candidates = listOf(
                restaurant("r1", generalTags = listOf("avoid_spicy_dishes"))
            )
        )
        assertEquals(2, result.first().score)
    }

    @Test
    fun `prefer tag conflicts with restaurant avoid tag subtracts points`() {
        val result = TagScorer.score(
            userGeneralTags = listOf("prefer_spicy_dishes"),
            userSpecificTags = emptyList(),
            candidates = listOf(
                restaurant("r1", generalTags = listOf("avoid_spicy_dishes"))
            )
        )
        assertEquals(-2, result.first().score)
    }
}
