package com.eric.guluturn.filter.models

import kotlinx.serialization.Serializable
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.SpecificTag

@Serializable
data class FilterInput(
    val userGeneralTags: List<String>,
    val userSpecificTags: List<SpecificTag>,
    val restaurants: List<Restaurant>
)
