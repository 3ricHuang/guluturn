package com.eric.guluturn.filter.impl

import com.eric.guluturn.filter.models.ScoredRestaurant

/**
 * AdvancementSelector selects the top 6 restaurants based on score.
 *
 * The result is a fixed-size list used for final recommendation.
 */
object AdvancementSelector {

    private const val FINAL_COUNT = 6

    /**
     * Selects the top 6 restaurants from the scored candidate list.
     *
     * @param candidates A list of scored restaurants.
     * @return A list of exactly 6 restaurants, or fewer if the input is smaller.
     */
    fun select(candidates: List<ScoredRestaurant>): List<ScoredRestaurant> {
        return candidates
            .sortedByDescending { it.score }
            .take(FINAL_COUNT)
    }
}
