package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.filter.registry.TagRegistry
import com.eric.guluturn.filter.iface.StatefulFilterEngine

/**
 * A higher-level orchestrator that wraps StatefulFilterEngine and injects tag parsing logic.
 *
 * This is the entry point for semantic-freeform user input (e.g., "I want spicy food").
 * It translates user input into structured tags, then delegates to StatefulFilterEngine.
 */
class StatefulFilterEngineImpl(
    private val engine: StatefulFilterEngine = StatefulFilterEngine()
) {

    /**
     * Runs the full filtering pipeline based on the given reason and available restaurants.
     *
     * @param reason A user input string (e.g., rejection explanation or preference)
     * @param restaurants The full list of candidate restaurants
     * @return Filtered restaurant list matching user intent
     */
    fun filter(reason: String, restaurants: List<Restaurant>): List<Restaurant> {
        val generalTags: List<String> = TagRegistry.extractGeneralTags(reason)
        val specificTags: List<SpecificTag> = TagRegistry.extractSpecificTags(reason)

        val filtered = engine.updateAndFilter(
            userGeneralTags = generalTags,
            userSpecificTags = specificTags,
            allRestaurants = restaurants
        ).mapNotNull { scored ->
            restaurants.find { it.id == scored.id }
        }

        return if (filtered.isEmpty()) {
            println("DEBUG: Fallback triggered – filter returned empty, randomly selecting 6")
            restaurants.shuffled().take(6)
        } else {
            filtered
        }
    }

    /**
     * Marks a restaurant as rejected in the internal state.
     */
    fun reject(id: String) {
        engine.reject(id)
    }

    /**
     * Exposes the current filtering state (for debug or history).
     */
    fun getState() = engine.getState()

    fun updateAndFilter(
        userGeneralTags: List<String>,
        userSpecificTags: List<SpecificTag>,
        allRestaurants: List<Restaurant>
    ): List<Restaurant> {
        return engine.updateAndFilter(userGeneralTags, userSpecificTags, allRestaurants)
            .mapNotNull { scored ->
                allRestaurants.find { it.id == scored.id }
            }
    }
}
