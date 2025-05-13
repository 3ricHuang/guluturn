package com.eric.guluturn.common.utils

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.LoaderOptions
import java.io.InputStreamReader

/**
 * Represents a standardized tag loaded from YAML.
 */
data class StandardTag(
    val tag: String,
    val polarity: String,
    val strength: String,
    val description: String,
)

object TagConfigLoader {

    private const val RESOURCE_PATH = "/tags.yaml" // Put under app/src/main/resources

    /**
     * Load all tags with metadata from YAML configuration.
     */
    fun loadAllTags(): List<StandardTag> {
        val inputStream = TagConfigLoader::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: throw IllegalStateException("tags.yaml not found in classpath at $RESOURCE_PATH")

        val reader = InputStreamReader(inputStream, Charsets.UTF_8)
        val yaml = Yaml(SafeConstructor(LoaderOptions()))
        val root = yaml.load<Any>(reader) ?: return emptyList()

        val result = mutableListOf<StandardTag>()
        walkYaml(root, result)
        return result
    }

    /**
     * Load only the tag names for use in prompt construction.
     */
    fun loadTagNames(): List<String> = loadAllTags().map { it.tag }

    /**
     * Recursively traverses YAML structure to extract tag entries.
     */
    private fun walkYaml(node: Any, out: MutableList<StandardTag>) {
        when (node) {
            is Map<*, *> -> {
                if (node.containsKey("tag")) {
                    val tag = node["tag"]?.toString() ?: return
                    val polarity = node["polarity"]?.toString() ?: "negative"
                    val strength = node["strength"]?.toString() ?: "soft"
                    val description = node["description"]?.toString() ?: ""
                    out += StandardTag(tag, polarity, strength, description)
                } else {
                    node.values.forEach { child -> if (child != null) walkYaml(child, out) }
                }
            }

            is Iterable<*> -> node.forEach { child -> if (child != null) walkYaml(child, out) }

            else -> {}
        }
    }
}
