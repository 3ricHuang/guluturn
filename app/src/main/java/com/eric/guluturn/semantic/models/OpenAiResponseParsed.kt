package com.eric.guluturn.semantic.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class OpenAiResponseParsed(
    @SerialName("user_input")
    val userInput: String,

    @SerialName("general_tags")
    val generalTags: List<String>,

    @SerialName("specific_tags")
    val specificTags: List<SpecificTag>
)

@Serializable
data class SpecificTag(
    val tag: String,
    val polarity: String
)
