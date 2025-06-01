package com.eric.guluturn.filter.models

/**
 * Immutable metadata for a single tag loaded from tags.yaml
 */
data class StandardTag(
    val tag: String,
    val tagType: String,          // preference | quality | safety | system
    val polarity: String,         // positive | negative | neutral
    val strength: String,         // hard | soft
    val opposite: Set<String>,    // 0~N opposite tags (pre-computed)
    val description: String       // for debug / prompt
)

