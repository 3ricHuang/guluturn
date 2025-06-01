package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.registry.TagRegistry

/**
 * 依「使用者硬性偏好」剔除 / 保留餐廳
 *
 * 規則 2025-05 版：
 * 1. hard-negative (polarity=negative, strength=hard, tag_type=preference)
 *    → banned = 該 tag + 其 opposite_tags
 * 2. hard-positive (polarity=positive, strength=hard, tag_type=preference)
 *    → required = 該 tag   (opposite 不需強求)
 *
 * Safety tag_type (若有) 亦可視同 hard-negative。
 */
object HardFilter {

    fun apply(
        userGeneralTags: List<String>,
        candidates: List<Restaurant>
    ): List<Restaurant> {

        /* ---------- banned set ---------- */
        val banned: Set<String> = userGeneralTags.flatMap { tag ->
            val meta = TagRegistry.get(tag)
            if (meta != null && meta.tagType == "preference"
                && meta.polarity == "negative" && meta.strength == "hard") {
                listOf(tag) + meta.opposite          // 自身 + 所有互斥標籤
            } else emptyList()
        }.toSet()

        /* ---------- required set ---------- */
        val required: Set<String> = userGeneralTags.filter { TagRegistry.isHardPositive(it) }.toSet()

        /* ---------- filter ---------- */
        return candidates.filter { shop ->
            val tags = shop.general_tags.toSet()
            banned.intersect(tags).isEmpty() &&          // 不含任何被禁止
                    required.all { it in tags }          // 含有所有必須
        }
    }
}
