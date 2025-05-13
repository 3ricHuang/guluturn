package com.eric.guluturn.semantic.impl

import com.eric.guluturn.semantic.iface.TagGenerator
import com.eric.guluturn.common.utils.TagConfigLoader

/**
 * Static implementation of TagGenerator using predefined tags loaded from configuration.
 *
 * This generator is used when the user directly selects from a list of standardized tags.
 */
class StaticTagGenerator : TagGenerator {

    private val standardTags: List<String> = TagConfigLoader.loadTagNames()

    /**
     * Assume user selects one or more tag strings from the UI.
     */
    override suspend fun generateTags(input: String): List<String> {
        return input
            .split(",")
            .map { it.trim() }
            .filter { it in standardTags }
    }

    fun getAvailableTags(): List<String> = standardTags
}
