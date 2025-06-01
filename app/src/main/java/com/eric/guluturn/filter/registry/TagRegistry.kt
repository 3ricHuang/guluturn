package com.eric.guluturn.filter.registry

import com.eric.guluturn.filter.exceptions.FilterRuleException
import com.eric.guluturn.filter.models.StandardTag
import com.eric.guluturn.common.models.SpecificTag
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.InputStream

/**
 * • 解析 2-level tags.yaml
 * • 提供 tag → metadata 查詢
 * • 提供硬性判斷（hard+/hard-）
 * • 提供簡易關鍵字反查 (demo only)
 */
object TagRegistry {

    // ---------- constant ---------- //
    private const val TAG_FILE = "tags.yaml"

    // ---------- internal cache ---------- //
    private val tagMap: Map<String, StandardTag> = loadTags()

    /** 全域對映：tag -> Set(oppositeTags)；用於 Scorer / HardFilter */
    val oppositeMap: Map<String, Set<String>> by lazy {
        tagMap.mapValues { it.value.opposite }
    }

    // ---------- public lookup ---------- //
    fun get(tag: String): StandardTag? = tagMap[tag]

    fun isHardNegative(tag: String): Boolean =
        tagMap[tag]?.let { it.tagType == "preference" && it.polarity == "negative" && it.strength == "hard" } == true

    fun isHardPositive(tag: String): Boolean =
        tagMap[tag]?.let { it.tagType == "preference" && it.polarity == "positive" && it.strength == "hard" } == true

    // ---------- naive keyword extractor (demo) ---------- //
    fun extractGeneralTags(text: String): List<String> {
        val lower = text.lowercase()
        return tagMap.values
            .filter { it.tagType != "system" }               // 不把 system 標籤回給算法
            .map { it.tag to it.tag.replace("_", " ") }
            .filter { (_, kw) -> lower.contains(kw) }
            .map { it.first }
    }

    fun extractSpecificTags(text: String): List<SpecificTag> =
        if (text.isBlank()) emptyList()
        else listOf(SpecificTag(tag = text.trim(), polarity = "positive"))

    // ---------- YAML loader ---------- //
    private fun loadTags(): Map<String, StandardTag> {
        val stream: InputStream = javaClass.classLoader?.getResourceAsStream(TAG_FILE)
            ?: throw FilterRuleException("tags.yaml not found in resources")

        val yaml = Yaml(SafeConstructor(LoaderOptions()))
        val root = yaml.load<Map<String, Any>>(stream)
        val map  = mutableMapOf<String, StandardTag>()

        fun flatten(node: Any?) {
            when (node) {
                is List<*> -> node.forEach(::flatten)
                is Map<*, *> -> {
                    if (node["tag"] != null) {
                        // ----- leaf -----
                        map[node["tag"] as String] = StandardTag(
                            tag        = node["tag"]              as String,
                            tagType    = node["tag_type"]         as String,
                            polarity   = node["polarity"]         as String,
                            strength   = node["strength"]         as String,
                            opposite   = (node["opposite_tags"] as? List<*>)?.map { it as String }?.toSet() ?: emptySet(),
                            description= node["description"]      as? String ?: ""
                        )
                    } else {
                        // ----- inner node -----
                        node.values.forEach(::flatten)
                    }
                }
            }
        }
        root.values.forEach(::flatten)
        return map
    }
}
