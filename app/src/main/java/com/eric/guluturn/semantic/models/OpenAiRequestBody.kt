package com.eric.guluturn.semantic.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiRequestBody(
    val model: String,
    val messages: List<ChatMessage>,

    val temperature: Double = 0.0,

    @SerialName("top_p")
    val topP: Double = 1.0,

    @SerialName("frequency_penalty")
    val frequencyPenalty: Double = 0.0,

    @SerialName("presence_penalty")
    val presencePenalty: Double = 0.0
)
