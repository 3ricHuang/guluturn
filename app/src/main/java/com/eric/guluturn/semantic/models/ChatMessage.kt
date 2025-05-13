package com.eric.guluturn.semantic.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
