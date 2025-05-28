package com.eric.guluturn.semantic.iface

import com.eric.guluturn.common.models.SpecificTag

interface ISemanticEngine {
    suspend fun parseInput(reason: String): ParsedUserInput
}

data class ParsedUserInput(
    val generalTags: List<String>,
    val specificTags: List<SpecificTag>
)
