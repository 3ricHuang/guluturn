package com.eric.guluturn.filter.models
import kotlinx.serialization.Serializable

/**
 * Represents a semantic tag with associated polarity.
 *
 * @property tag The content of the tag (e.g., "too salty", "refreshing").
 * @property polarity Whether the tag is positive or negative.
 */
@Serializable
data class SpecificTag(
    val tag: String,
    val polarity: String // should be "positive" or "negative"
)
