package com.eric.guluturn.filter.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.RestaurantCandidate
import com.eric.guluturn.common.models.SourceType
import com.eric.guluturn.filter.compose.UserConstraintComposer
import com.eric.guluturn.filter.iface.StatefulFilterEngine
import com.eric.guluturn.filter.models.ScoredRestaurant
import com.eric.guluturn.filter.prefilter.BusinessHoursPreFilter
import com.eric.guluturn.filter.registry.TagRegistry
import com.eric.guluturn.filter.rerank.SpecificTagEmbeddingMatcher
import com.eric.guluturn.semantic.iface.ISemanticEngine
import com.eric.guluturn.semantic.iface.ParsedUserInput

/**
 * High-level orchestrator – free-form text ➜ exactly 6 restaurant cards.
 *
 * Sticky state (valid **within one roulette round**):
 *   • [rejectedIds] — cards the user explicitly discarded
 *   • [persistentGeneralTags] — “hard” tags + user_report_closed (survive spins)
 *
 * Soft preference tags are **NOT** accumulated across spins.
 */
class StatefulFilterEngineImpl(
    private val apiKey: String,
    private val engineFactory: () -> StatefulFilterEngine = { StatefulFilterEngine() },
    private val semantic: ISemanticEngine
) {
    /* ------------------- per-round caches ------------------- */
    private val rejectedIds = mutableSetOf<String>()
    private val persistentGeneralTags = mutableSetOf<String>()

    /* -------------------------------------------------------- */
    suspend fun filter(
        reason: String,
        restaurants: List<Restaurant>
    ): List<Restaurant> {

        /* ① semantic parse */
        val parsed: ParsedUserInput = semantic.parseInput(reason)

        /* ② sticky tag update (hard tags + user_report_closed) */
        updateStickyTags(parsed.generalTags)

        /* ③ merge soft + sticky */
        val effectiveGeneralTags = (parsed.generalTags + persistentGeneralTags).distinct()

        /* ④ open-hours pre-filter (only if user_report_closed present) */
        val parsedForPrefilter = parsed.copy(generalTags = effectiveGeneralTags)
        val poolAfterHours = BusinessHoursPreFilter.filter(parsedForPrefilter, restaurants)

        /* ⑤ run core scoring (fresh engine → no soft-tag carry-over) */
        val core = engineFactory().apply { rejectedIds.forEach { reject(it) } }

        var prelim: List<ScoredRestaurant> = core.updateAndFilter(
            userGeneralTags  = effectiveGeneralTags,
            userSpecificTags = parsed.specificTags,
            allRestaurants   = poolAfterHours
        )

        /* ⑥ optional: re-rank前 TOP_N by specific-tag embedding similarity */
        prelim = SpecificTagEmbeddingMatcher.adjustScores(
            prelim,
            parsed.specificTags,
            topN = 40         // 可調
        )

        /* ⑦ 同分隨機打散 → 取得順位 */
        val grouped     = prelim.groupBy { it.score }.toSortedMap(compareByDescending { it })
        val shuffled    = grouped.flatMap { (_, list) -> list.shuffled() }
        val rankedShops = shuffled.map { it.restaurant }

        /* ⑧ compose 6 cards (固定 + 填充) */
        val final = UserConstraintComposer.compose(
            fullPool = restaurants,
            ranked           = rankedShops,
            preferredNames   = parsed.preferredRestaurants,
            apiKey           = apiKey
        )

        return if (final.isEmpty()) restaurants.shuffled().take(6) else final
    }

    /* ---------------- helper: sticky tag maintenance ------------------- */
    private fun updateStickyTags(tags: List<String>) {
        tags.forEach { tag ->
            if (tag == "user_report_closed") {
                persistentGeneralTags += tag; return@forEach
            }
            TagRegistry.get(tag)?.let { meta ->
                if (meta.strength == "hard") persistentGeneralTags += tag
            }
        }
    }

    /* ---------------- rejection / reset ------------------- */
    fun reject(id: String) { rejectedIds += id }
    fun reset() {
        rejectedIds.clear()
        persistentGeneralTags.clear()
    }

    /* ------------------------------------------------------------------ */
    /**
     * Debug / instrumentation helper – returns card list with來源註記與分數.
     */
    suspend fun filterWithSource(
        reason: String,
        restaurants: List<Restaurant>
    ): List<RestaurantCandidate> {

        val parsed: ParsedUserInput = semantic.parseInput(reason)
        updateStickyTags(parsed.generalTags)
        val effectiveGeneralTags = (parsed.generalTags + persistentGeneralTags).distinct()

        val poolAfterHours = BusinessHoursPreFilter.filter(
            parsed.copy(generalTags = effectiveGeneralTags),
            restaurants
        )

        val core = engineFactory().apply { rejectedIds.forEach { reject(it) } }

        var prelim = core.updateAndFilter(
            userGeneralTags  = effectiveGeneralTags,
            userSpecificTags = parsed.specificTags,
            allRestaurants   = poolAfterHours
        )

        prelim = SpecificTagEmbeddingMatcher.adjustScores(
            prelim,
            parsed.specificTags,
            topN = 40
        )

        val scoreMap = prelim.associateBy({ it.restaurant.id }, { it.score })

        val grouped  = prelim.groupBy { it.score }.toSortedMap(compareByDescending { it })
        val shuffled = grouped.flatMap { (_, l) -> l.shuffled() }
        val ranked   = shuffled.map { it.restaurant }

        val composed = UserConstraintComposer.compose(
            fullPool = restaurants,
            ranked         = ranked,
            preferredNames = parsed.preferredRestaurants,
            apiKey         = apiKey
        )

        if (composed.isEmpty()) {
            return restaurants.shuffled().take(6).map {
                RestaurantCandidate(it, SourceType.RANDOM, score = null)
            }
        }

        return composed.map { shop ->
            val src = when {
                parsed.preferredRestaurants.any { shop.name.contains(it, true) } -> SourceType.FIXED
                else -> SourceType.FILTERED
            }
            RestaurantCandidate(shop, src, scoreMap[shop.id])
        }
    }
}
