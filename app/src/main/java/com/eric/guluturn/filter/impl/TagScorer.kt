package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.filter.models.ScoredRestaurant
import com.eric.guluturn.filter.registry.TagRegistry

/**
 * TagScorer (2025-06)
 * ─────────────────────────────────────────────────────────────
 *  只評分 general_tags；specific_tags 交由後續 Reranker。
 *  規則見 Score Matrix（文件下方）或程式內註解。
 */
object TagScorer {

    /* ─── 1. Weight constants ──────────────────────────────── */
    private const val SAME_MATCH             =  3   // A1
    private const val OPPOSITE_STRONG        = -3   // A2 / A3
    private const val QUAL_ALIGN_BONUS       =  1   // B1 / C1
    private const val QUAL_TOO_BONUS_PREF    =  1   // C2 (user prefer)
    private const val QUAL_TOO_PENALTY_AVOID = -2   // C2 (user avoid)

    /* ─── 2. Public entry ──────────────────────────────────── */
    fun score(
        userGeneralTags : List<String>,
        userSpecificTags: List<SpecificTag>,   // 預留 API 相容
        candidates      : List<Restaurant>
    ): List<ScoredRestaurant> = candidates
        .map { shop ->
            val gScore = scoreGeneral(userGeneralTags, shop.general_tags)
            ScoredRestaurant(restaurant = shop, score = gScore)
        }
        .sortedByDescending { it.score }

    /* ─── 3. Core logic ────────────────────────────────────── */
    private fun scoreGeneral(user: List<String>, shop: List<String>): Int {

        var sum = 0

        user.forEach { uTag ->
            val uMeta = TagRegistry.get(uTag) ?: return@forEach
            // 跳過 system 或 hard 級
            if (uMeta.tagType == "system" || uMeta.strength == "hard") return@forEach

            shop.forEach { sTag ->
                val sMeta = TagRegistry.get(sTag) ?: return@forEach

                /* ── A1：同名一致 (+3) ───────────────────────── */
                if (uTag == sTag) {
                    sum += SAME_MATCH
                    return@forEach         // 換下一個 shopTag
                }

                /* ── A2 / A3：opposite-tags 強衝突 (-3) ──────── */
                val isOpposite = sTag in uMeta.opposite || uTag in sMeta.opposite
                if (isOpposite) {
                    when {
                        // A3：quality ± vs quality ±
                        uMeta.tagType == "quality" && sMeta.tagType == "quality" -> {
                            sum += OPPOSITE_STRONG; return@forEach
                        }
                        // A2-1：preference ± vs preference ±
                        uMeta.tagType == "preference" && sMeta.tagType == "preference" -> {
                            sum += OPPOSITE_STRONG; return@forEach
                        }
                        // A2-2：preference (任意) vs quality (negative)
                        uMeta.tagType == "preference" &&
                                sMeta.tagType == "quality" && sMeta.polarity == "negative" -> {
                            sum += OPPOSITE_STRONG; return@forEach
                        }
                        // B1：avoid_X  ↔ quality (+)
                        uMeta.tagType == "preference" && uMeta.polarity == "negative" &&
                                sMeta.tagType == "quality" && sMeta.polarity == "positive" -> {
                            sum += QUAL_ALIGN_BONUS; return@forEach
                        }
                    }
                }

                /* ── C1 / C2：quality 系列 ─────────────────── */
                if (sMeta.tagType == "quality" && sameTopic(uTag, sTag)) {
                    when (sMeta.polarity) {
                        "positive" -> {                         // quality_ok_* 或同義正向品質
                            sum += QUAL_ALIGN_BONUS
                        }
                        "negative" -> {
                            if (isIntensifier(sTag)) {          // quality_too_* / *_too_* …
                                sum += if (uMeta.polarity == "positive")
                                    QUAL_TOO_BONUS_PREF         // 使用者愛吃辣  +  too_spicy
                                else
                                    QUAL_TOO_PENALTY_AVOID      // 使用者避辣   +  too_spicy
                            }
                            // 若是一般負向品質 (dirty_environment 等) 與偏好關聯度低 → 0 分
                        }
                    }
                }
            }
        }
        return sum
    }

    /* ─── 4. Helpers ───────────────────────────────────────── */

    /** 將 prefer_/avoid_/quality_ok_/quality_too_/quality_/too_ 前綴剝除後比對核心詞 */
    private fun canonical(tag: String): String =
        tag.removePrefix("prefer_")
            .removePrefix("avoid_")
            .removePrefix("quality_ok_")
            .removePrefix("quality_too_")
            .removePrefix("quality_")
            .removePrefix("too_")

    /** 判斷兩標籤是否討論同一主題（辣度、價格…） */
    private fun sameTopic(a: String, b: String): Boolean =
        canonical(a) == canonical(b)

    /** 強化詞：帶有 'too' 的負向品質，例 quality_too_spicy, portion_too_small */
    private fun isIntensifier(tag: String): Boolean =
        tag.startsWith("quality_too_") ||
                tag.contains("_too_") ||
                tag.startsWith("too_")
}
