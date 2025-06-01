package com.eric.guluturn.filter.models

import com.eric.guluturn.common.models.Restaurant

/**
 * Represents a restaurant with an associated matching score.
 *
 * @property id Unique identifier of the restaurant.
 * @property score Computed score indicating how well the restaurant matches user preferences.
 */
data class ScoredRestaurant(
    val restaurant: Restaurant,
    val score: Int
)
