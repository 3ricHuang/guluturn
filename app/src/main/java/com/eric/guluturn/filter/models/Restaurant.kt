package com.eric.guluturn.filter.models
import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val generalTags: List<String>,
    val specificTags: List<SpecificTag>
)
