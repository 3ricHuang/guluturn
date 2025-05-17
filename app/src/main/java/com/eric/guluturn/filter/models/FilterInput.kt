package com.eric.guluturn.filter.models

import kotlinx.serialization.Serializable

@Serializable
data class FilterInput(
    val userGeneralTags: List<String>,
    val userSpecificTags: List<SpecificTag>,
    val restaurants: List<Restaurant>
)
