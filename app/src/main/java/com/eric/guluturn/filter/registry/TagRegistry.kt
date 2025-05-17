package com.eric.guluturn.filter.registry

import com.eric.guluturn.filter.models.StandardTag
import com.eric.guluturn.filter.exceptions.FilterRuleException
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.LoaderOptions
import java.io.InputStream

object TagRegistry {
    private const val TAG_FILE = "tags.yaml"

    private val tagMap: Map<String, StandardTag> = loadTags()

    /** e.g. prefer_spicy_dishes ↔ avoid_spicy_dishes*/
    val conflictMap: Map<String, String> by lazy {
        val m = mutableMapOf<String, String>()
        for ((tag, _) in tagMap) {
            val opposite = when {
                tag.startsWith("avoid_")   -> tag.replaceFirst("avoid_",  "prefer_")
                tag.startsWith("prefer_")  -> tag.replaceFirst("prefer_", "avoid_")
                else                       -> null
            }
            if (opposite != null && tagMap.containsKey(opposite)) {
                m[tag] = opposite
            }
        }
        m
    }

    fun get(tag: String): StandardTag? = tagMap[tag]

    fun isHardNegative(tag: String): Boolean {
        val t = tagMap[tag] ?: return false
        return t.polarity == "negative" && t.strength == "hard"
    }

    fun isSoftPositive(tag: String): Boolean {
        val t = tagMap[tag] ?: return false
        return t.polarity == "positive" && t.strength == "soft"
    }

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
