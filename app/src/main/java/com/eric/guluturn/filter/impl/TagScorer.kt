package com.eric.guluturn.filter.impl

import com.eric.guluturn.filter.models.Restaurant
import com.eric.guluturn.filter.models.SpecificTag
import com.eric.guluturn.filter.models.ScoredRestaurant
import com.eric.guluturn.filter.registry.TagRegistry

/**
 * TagScorer assigns scores to candidate restaurants based on the user's
 * general and specific tags. The score reflects semantic alignment and
 * will be used for downstream filtering and selection.
 *
 * Current implementation uses exact tag matching only.
 */
object TagScorer {

    /**
     * Scores each restaurant using user-provided tags.
     * @param userGeneralTags List of standardized general tags from user input.
     * @param userSpecificTags List of user-generated free-text tags with polarity.
     * @param candidates List of restaurants to be scored.
     * @return List of scored restaurants (id + score), sorted by descending score.
     */
    fun score(
        userGeneralTags: List<String>,
        userSpecificTags: List<SpecificTag>,
        candidates: List<Restaurant>
    ): List<ScoredRestaurant> {
        return candidates.map { restaurant ->
            val generalScore = scoreGeneral(userGeneralTags, restaurant.generalTags)
            val specificScore = scoreSpecific(userSpecificTags, restaurant.specificTags)
            ScoredRestaurant(
                id = restaurant.id,
                score = generalScore + specificScore
            )
        }.sortedByDescending { it.score }
    }

    /**
     * Exact match scoring for general tags based on polarity and strength.
     * Matching tags receive +2, opposite tags receive -2.
     * Hard tags are ignored (they are handled by HardFilter).
     */
    private fun scoreGeneral(
        userTags: List<String>,
        restaurantTags: List<String>
    ): Int {
        var score = 0
        for (tag in userTags) {
            val meta = TagRegistry.get(tag) ?: continue
            if (meta.strength == "hard") continue

            val opp = TagRegistry.conflictMap[tag]
            when (meta.polarity) {
                "positive" -> {
                    if (tag in restaurantTags) score += 2
                    if (opp != null && opp in restaurantTags) score -= 2
                }
                "negative" -> {
                    if (tag in restaurantTags) score += 2
                    if (opp != null && opp in restaurantTags) score -= 2
                }
            }
        }
        return score
    }

    /**
     * Exact match scoring for specific tags.
     * If a tag with the same polarity is found → +3
     * If a tag with the opposite polarity is found → -3
     * Otherwise → 0
     */
    private fun scoreSpecific(
        userTags: List<SpecificTag>,
        restaurantTags: List<SpecificTag>
    ): Int {
        var score = 0
        for (userTag in userTags) {
            for (shopTag in restaurantTags) {
                if (userTag.tag == shopTag.tag) {
                    val userPol = userTag.polarity.lowercase()
                    val shopPol = shopTag.polarity.lowercase()
                    score += if (userPol == shopPol) 3 else -3
                }
            }
        }
        return score
    }
}
