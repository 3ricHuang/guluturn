package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.registry.TagRegistry

/**
 * HardFilter applies strict exclusion rules based on user general tags.
 *
 * It filters out restaurants that conflict with the user's "hard" preferences.
 * These tags are defined in the tag metadata (tags.yaml) as having:
 *   - polarity = "negative", strength = "hard" → must be excluded
 *   - polarity = "positive", strength = "hard" → must be included
 *
 * Example use case:
 *   - If a user has "no_halal_options" as a hard negative tag,
 *     all restaurants with that tag will be excluded from the candidate list.
 *   - If a user has "need_vegetarian_options" as a hard positive tag,
 *     all restaurants lacking that tag will be excluded.
 */
object HardFilter {

    /**
     * Filters out restaurants that violate any of the user's hard general tags.
     *
     * @param userGeneralTags The list of general tags extracted from user input.
     *                        Only tags that are marked as hard in metadata will be considered.
     * @param candidates The list of restaurants to be filtered.
     * @return A list of restaurants that satisfy all required tags and do not contain any banned tags.
     */
    fun apply(
        userGeneralTags: List<String>,
        candidates: List<Restaurant>
    ): List<Restaurant> {
        // Extract user tags that are defined as hard negative (e.g., no_halal_options)
        val bannedTags = userGeneralTags
            .filter { TagRegistry.isHardNegative(it) }
            .toSet()

        // Extract user tags that are defined as hard positive (e.g., need_vegetarian_options)
        val requiredTags = userGeneralTags
            .filter { TagRegistry.isHardPositive(it) }
            .toSet()

        // Keep restaurants that:
        // - do NOT contain any banned tags
        // - DO contain all required tags
        return candidates.filter { restaurant ->
            val restaurantTags = restaurant.general_tags.toSet()
            bannedTags.intersect(restaurantTags).isEmpty() &&
                    requiredTags.all { it in restaurantTags }
        }
    }
}
