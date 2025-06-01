package com.eric.guluturn.filter.iface

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.filter.models.ScoredRestaurant
import com.eric.guluturn.filter.models.FilterState
import com.eric.guluturn.filter.impl.HardFilter
import com.eric.guluturn.filter.impl.TagScorer
import com.eric.guluturn.filter.impl.AdvancementSelector

/**
 * StatefulFilterEngine maintains user preferences and previously rejected restaurants.
 *
 * It allows incremental filtering across multiple interaction rounds.
 */
class StatefulFilterEngine {

    private var state: FilterState = FilterState()

    /**
     * Adds new user tags and returns a fresh filtered recommendation batch.
     *
     * @param userGeneralTags Newly extracted general tags from this turn.
     * @param userSpecificTags Newly extracted specific tags from this turn.
     * @param allRestaurants The full list of candidate restaurants.
     * @return A list of 6 restaurants matching current accumulated preferences.
     */
    fun updateAndFilter(
        userGeneralTags: List<String>,
        userSpecificTags: List<SpecificTag>,
        allRestaurants: List<Restaurant>
    ): List<ScoredRestaurant> {
        // Update internal state
        state = state.copy(
            accumulatedGeneralTags = state.accumulatedGeneralTags + userGeneralTags,
            accumulatedSpecificTags = state.accumulatedSpecificTags + userSpecificTags
        )

        // Exclude rejected restaurants
        val remaining = allRestaurants.filter { it.id !in state.rejectedRestaurantIds }

        // Run full stateless filtering
        val scored = TagScorer.score(
            userGeneralTags = state.accumulatedGeneralTags,
            userSpecificTags = state.accumulatedSpecificTags,
            candidates = HardFilter.apply(state.accumulatedGeneralTags, remaining)
        )

        return scored.sortedByDescending { it.score }
    }

    /**
     * Marks a restaurant as rejected and updates the internal state.
     *
     * @param restaurantId The ID of the restaurant rejected by the user.
     */
    fun reject(restaurantId: String) {
        state = state.copy(
            rejectedRestaurantIds = state.rejectedRestaurantIds + restaurantId
        )
    }

    /**
     * Returns the current filter state.
     */
    fun getState(): FilterState = state

    fun reset() {
        state = FilterState()
    }

}
