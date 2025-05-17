package com.eric.guluturn.filter.impl

import com.eric.guluturn.filter.models.Restaurant
import com.eric.guluturn.filter.registry.TagRegistry

/**
 * HardFilter applies strict exclusion rules based on user general tags.
 *
 * It filters out restaurants that conflict with the user's "hard negative" preferences.
 * These tags are defined in the tag metadata (tags.yaml) as having:
 *   - polarity = "negative"
 *   - strength = "hard"
 *
 * Example use case:
 *   - If a user has "no_halal_options" as a hard negative tag,
 *     all restaurants with that tag will be excluded from the candidate list.
 */
object HardFilter {

    /**
     * Filters out restaurants that violate any of the user's hard negative general tags.
     *
     * @param userGeneralTags The list of general tags extracted from user input.
     *                        Only tags that are marked as hard negative in metadata will be considered.
     * @param candidates The list of restaurants to be filtered.
     * @return A list of restaurants that do not contain any conflicting hard negative tags.
     */
    fun apply(
        userGeneralTags: List<String>,
        candidates: List<Restaurant>
    ): List<Restaurant> {
        // Extract user tags that are defined as hard negative (e.g., no_halal_options)
        val bannedTags = userGeneralTags
            .filter { TagRegistry.isHardNegative(it) }
            .toSet()

        // Keep restaurants that do NOT contain any of the banned tags
        return candidates.filter { restaurant ->
            val restaurantTags = restaurant.generalTags.toSet()
            bannedTags.intersect(restaurantTags).isEmpty()
        }
    }
}
