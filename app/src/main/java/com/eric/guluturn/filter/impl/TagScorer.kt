package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.filter.models.ScoredRestaurant
import com.eric.guluturn.filter.registry.TagRegistry

/**
 * TagScorer (2025/06) – only evaluates **general tags**.
 *
 *  Scoring rule recap
 *  ─────────────────────────────────────────────
 *   • skip "system"  &  strength = hard
 *   • preference  vs  same-named preference   : +3 / –3
 *   • preference  vs  quality (opposites)     : +1 (align) / –2 (conflict)
 *   • all other combinations                  :  0
 *  ─────────────────────────────────────────────
 */
object TagScorer {

    /* tunable weights */
    private const val SAME_PREF_POS  =  3
    private const val SAME_PREF_NEG  = -3
    private const val CROSS_ALIGN    =  1
    private const val CROSS_CONFLICT = -2

    /**
     * @return a DESC-sorted list of [ScoredRestaurant] with full Restaurant payload.
     */
    fun score(
        userGeneralTags : List<String>,
        userSpecificTags: List<SpecificTag>,   // kept for API compatibility
        candidates      : List<Restaurant>
    ): List<ScoredRestaurant> {

        return candidates.map { shop ->
            val gScore = scoreGeneral(userGeneralTags, shop.general_tags)
            /* >>>>>>>>>>> 這一行改為注入 Restaurant 本體  <<<<<<<<<<< */
            ScoredRestaurant(restaurant = shop, score = gScore)
        }.sortedByDescending { it.score }
    }

    /* ---------------- internal ---------------- */
    private fun scoreGeneral(
        userTags : List<String>,
        shopTags : List<String>
    ): Int {
        var score = 0

        userTags.forEach { uTag ->
            val uMeta = TagRegistry.get(uTag) ?: return@forEach
            if (uMeta.tagType == "system" || uMeta.strength == "hard") return@forEach

            /* ① same-name preference */
            if (uMeta.tagType == "preference" && uTag in shopTags) {
                score += if (uMeta.polarity == "positive") SAME_PREF_POS else SAME_PREF_NEG
            }

            /* ② preference ↔ quality opposite mapping */
            if (uMeta.tagType == "preference") {
                shopTags.forEach { sTag ->
                    val sMeta = TagRegistry.get(sTag) ?: return@forEach
                    if (sMeta.tagType != "quality") return@forEach

                    when {
                        sTag in uMeta.opposite -> score += CROSS_CONFLICT
                        uTag in sMeta.opposite -> score += CROSS_ALIGN
                    }
                }
            }
        }
        return score
    }
}
