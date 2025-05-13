package com.eric.guluturn.semantic.models

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiRequestBody(
    val model: String,
    val messages: List<ChatMessage>
)
