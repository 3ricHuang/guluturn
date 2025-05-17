package com.eric.guluturn.filter.models

import kotlinx.serialization.Serializable

@Serializable
data class FilterState(
    val accumulatedGeneralTags: List<String> = emptyList(),
    val accumulatedSpecificTags: List<SpecificTag> = emptyList(),
    val rejectedRestaurantIds: Set<String> = emptySet()
)
