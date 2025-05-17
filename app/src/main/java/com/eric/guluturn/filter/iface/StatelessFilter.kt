package com.eric.guluturn.filter.iface

import com.eric.guluturn.filter.impl.AdvancementSelector
import com.eric.guluturn.filter.impl.HardFilter
import com.eric.guluturn.filter.impl.TagScorer
import com.eric.guluturn.filter.models.FilterInput
import com.eric.guluturn.filter.models.ScoredRestaurant

/**
 * StatelessFilter is the primary entry point for one-shot filtering and ranking.
 *
 * It applies:
 * 1. Hard filtering based on critical tag conflicts
 * 2. Scoring based on semantic and preference alignment
 * 3. Top-k selection for downstream randomization or UI presentation
 */
object StatelessFilter {

    /**
     * Filters and ranks restaurants in a single pass.
     *
     * @param input The full input object containing user tags and candidate restaurants.
     * @return A list of 5–7 scored restaurants.
     */
    fun filterAndSelect(input: FilterInput): List<ScoredRestaurant> {
        val filtered = HardFilter.apply(
            userGeneralTags = input.userGeneralTags,
            candidates = input.restaurants
        )

        val scored = TagScorer.score(
            userGeneralTags = input.userGeneralTags,
            userSpecificTags = input.userSpecificTags,
            candidates = filtered
        )

        return AdvancementSelector.select(scored)
    }
}
