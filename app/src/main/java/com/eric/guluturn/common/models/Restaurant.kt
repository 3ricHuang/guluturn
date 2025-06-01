package com.eric.guluturn.common.models
import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val id: String = "",
    val name: String = "",
    val summary: String = "",
    val general_tags: List<String> = emptyList(),
    val specific_tags: List<SpecificTag> = emptyList(),
    val location: Location = Location(),
    val price_range: String? = null,
    val rating: Double? = null,
    val review_count: Int = 0,
    val business_hours: Map<String, BusinessHour> = emptyMap(),
    val name_embedding: List<Double> = emptyList()
)

@Serializable
data class SpecificTag(
    val tag: String = "",
    val polarity: String = "",  // "positive" or "negative"
    val embedding: List<Double> = emptyList()
)

@Serializable
data class Location(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = ""
)

@Serializable
data class BusinessHour(
    val open: String? = null,
    val close: String? = null
)
