package com.eric.guluturn.filter.impl

import com.eric.guluturn.filter.models.ScoredRestaurant

/**
 * AdvancementSelector determines which restaurants advance to the final candidate pool.
 *
 * It selects the top N restaurants based on their score, ensuring:
 * - Minimum: 5 restaurants
 * - Maximum: 7 restaurants
 * - Tie handling: All restaurants with the same score as the N-th are included.
 */
object AdvancementSelector {

    private const val MIN_COUNT = 5
    private const val MAX_COUNT = 7

    /**
     * Selects a top subset of restaurants based on score.
     * Ties at the cut-off score are included.
     *
     * @param candidates A list of restaurants that have been scored.
     * @return A filtered and sorted list of 5–7 restaurants for final recommendation.
     */
    fun select(candidates: List<ScoredRestaurant>): List<ScoredRestaurant> {
        if (candidates.size <= MIN_COUNT) return candidates

        val sorted = candidates.sortedByDescending { it.score }

        val topN = sorted.take(MIN_COUNT).toMutableList()
        val lastScore = topN.last().score

        for (i in MIN_COUNT until sorted.size) {
            val next = sorted[i]
            if (next.score == lastScore && topN.size < MAX_COUNT) {
                topN.add(next)
            } else {
                break
            }
        }

        return topN
    }
}
