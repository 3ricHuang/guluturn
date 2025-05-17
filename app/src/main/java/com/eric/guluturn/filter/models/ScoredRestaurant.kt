package com.eric.guluturn.filter.models

/**
 * Represents a restaurant with an associated matching score.
 *
 * @property id Unique identifier of the restaurant.
 * @property score Computed score indicating how well the restaurant matches user preferences.
 */
data class ScoredRestaurant(
    val id: String,
    val score: Int
)
