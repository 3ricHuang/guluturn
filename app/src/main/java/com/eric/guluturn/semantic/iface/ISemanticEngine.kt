package com.eric.guluturn.semantic.iface

import com.eric.guluturn.common.models.SpecificTag

/**
 * Parses free-form user text into structured intent.
 */
interface ISemanticEngine {
    suspend fun parseInput(reason: String): ParsedUserInput
}

/**
 * Unified result returned by SemanticEngine.
 */
data class ParsedUserInput(
    val generalTags: List<String>,          // normalized tags (preference / quality / system)
    val specificTags: List<SpecificTag>,    // short phrases with polarity
    val preferredRestaurants: List<String>, // user-specified restaurant names (fuzzy match)
)
