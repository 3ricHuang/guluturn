package com.eric.guluturn.filter.registry

import com.eric.guluturn.filter.exceptions.FilterRuleException
import com.eric.guluturn.filter.models.StandardTag
import com.eric.guluturn.common.models.SpecificTag
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.InputStream

/**
 * TagRegistry loads and provides access to standardized general tag metadata.
 * It supports tag lookups, polarity/strength checks, and string-based parsing from user input.
 */
object TagRegistry {
    private const val TAG_FILE = "tags.yaml"

    private val tagMap: Map<String, StandardTag> = loadTags()

    /**
     * Conflict map for general tags, e.g., prefer_spicy_dishes ↔ avoid_spicy_dishes
     */
    val conflictMap: Map<String, String> by lazy {
        val m = mutableMapOf<String, String>()
        for ((tag, _) in tagMap) {
            val opposite = when {
                tag.startsWith("avoid_")  -> tag.replaceFirst("avoid_", "prefer_")
                tag.startsWith("prefer_") -> tag.replaceFirst("prefer_", "avoid_")
                else                      -> null
            }
            if (opposite != null && tagMap.containsKey(opposite)) {
                m[tag] = opposite
            }
        }
        m
    }

    /**
     * Returns metadata of a specific tag, or null if not found.
     */
    fun get(tag: String): StandardTag? = tagMap[tag]

    /**
     * Checks if a tag is defined as a hard negative (polarity = negative, strength = hard).
     */
    fun isHardNegative(tag: String): Boolean {
        val t = tagMap[tag] ?: return false
        return t.polarity == "negative" && t.strength == "hard"
    }

    /**
     * Checks if a tag is defined as a hard positive (polarity = positive, strength = hard).
     */
    fun isHardPositive(tag: String): Boolean {
        val t = tagMap[tag] ?: return false
        return t.polarity == "positive" && t.strength == "hard"
    }

    /**
     * Checks if a tag is defined as a soft positive (polarity = positive, strength = soft).
     */
    fun isSoftPositive(tag: String): Boolean {
        val t = tagMap[tag] ?: return false
        return t.polarity == "positive" && t.strength == "soft"
    }

    /**
     * Attempts to extract standardized general tags from user input by simple keyword matching.
     *
     * This is a naive implementation. Replace with semantic NLP or embedding in production.
     */
    fun extractGeneralTags(reason: String): List<String> {
        val lower = reason.lowercase()
        return tagMap.keys.filter { tag ->
            val keywords = tag.replace("_", " ")
            lower.contains(keywords)
        }
    }

    /**
     * Attempts to extract specific tags from user input.
     * Currently returns a single-tag fallback with positive polarity.
     */
    fun extractSpecificTags(reason: String): List<SpecificTag> {
        return if (reason.isNotBlank()) {
            listOf(SpecificTag(tag = reason.trim(), polarity = "positive"))
        } else {
            emptyList()
        }
    }

    /**
     * Loads and flattens tags.yaml into a flat lookup map from tag name to metadata.
     */
    private fun loadTags(): Map<String, StandardTag> {
        val inputStream: InputStream = javaClass.classLoader
            ?.getResourceAsStream(TAG_FILE)
            ?: throw FilterRuleException("tags.yaml not found in resources")

        val yaml = Yaml(SafeConstructor(LoaderOptions()))
        val raw = yaml.load<Map<String, Any>>(inputStream)
        val flatMap = mutableMapOf<String, StandardTag>()

        fun flatten(node: Any?) {
            when (node) {
                is List<*> -> node.forEach { flatten(it) }
                is Map<*, *> -> {
                    val hasTag = node.containsKey("tag") &&
                            node.containsKey("polarity") &&
                            node.containsKey("strength")

                    if (hasTag) {
                        val tag = node["tag"] as String
                        val polarity = node["polarity"] as String
                        val strength = node["strength"] as String
                        val description = node["description"] as? String ?: ""
                        flatMap[tag] = StandardTag(tag, polarity, strength, description)
                    } else {
                        node.values.forEach { flatten(it) }
                    }
                }
            }
        }

        raw.values.forEach { flatten(it) }
        return flatMap
    }
}
