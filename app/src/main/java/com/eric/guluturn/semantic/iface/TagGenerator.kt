package com.eric.guluturn.semantic.iface

/**
 * Interface for generating semantic tags from user input.
 *
 * All returned tags should be standardized, lowercase English strings.
 * For example: ["too_greasy", "already_tried", "too_expensive"]
 */
interface TagGenerator {

    /**
     * Generate standardized tags from a free-form user reason (typically in Chinese).
     *
     * The generated tags should be mapped to a predefined set of standardized tags
     * to ensure consistency in downstream processing.
     *
     * @param input Natural language text describing why a restaurant was rejected.
     * @return A list of standardized English tags. May be empty if no valid tag is generated.
     */
    suspend fun generateTags(input: String): List<String>
}
