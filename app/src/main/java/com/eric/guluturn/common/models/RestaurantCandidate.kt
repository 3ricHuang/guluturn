package com.eric.guluturn.common.models

data class RestaurantCandidate(
    val restaurant: Restaurant,
    val source: SourceType,
    val score: Int? = null
)

enum class SourceType {
    FIXED, FILTERED, RANDOM
}
