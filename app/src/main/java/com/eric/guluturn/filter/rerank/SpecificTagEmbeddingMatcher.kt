package com.eric.guluturn.filter.rerank

import android.util.Log
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.filter.models.ScoredRestaurant
import com.eric.guluturn.utils.EmbeddingUtils.cosine

/**
 * Second-stage scoring based on dish / keyword semantics.
 *
 * Rule set (single threshold):
 *  ─ cosSim <  THRESHOLD                →  –3
 *  ─ cosSim ≥ THRESHOLD &  positive     →  +5
 *  ─ cosSim ≥ THRESHOLD & !positive     →  +1
 *
 * Only the top-K prelim results are inspected (K = [topN]).
 */
object SpecificTagEmbeddingMatcher {

    /* ---------- tunable params ---------- */
    private const val THRESHOLD        = 0.85
    private const val BONUS_POSITIVE   = 20
    private const val BONUS_OTHER      = 10
    private const val PENALTY_MISMATCH = -5
    /* ------------------------------------ */

    /**
     * @param prelim   coarse ranking list (must embed Restaurant)
     * @param userTags user's specific-tags (with embeddings)
     * @param topN     how many leading items to re-score (default = all)
     *
     * @return a new list with updated scores, already re-sorted DESC.
     */
    fun adjustScores(
        prelim : List<ScoredRestaurant>,
        userTags: List<SpecificTag>,
        topN   : Int = prelim.size
    ): List<ScoredRestaurant> {

        if (userTags.isEmpty() || topN == 0) return prelim

        /* ① identify the top-K slice to inspect */
        val slice = prelim.take(topN).toMutableList()
        Log.i("SpecificMatcher", "===== Rerank pool (${prelim.size} total) =====")
        prelim.forEachIndexed { i, entry ->
            Log.i("SpecificMatcher", "   #$i: ${entry.restaurant.name} | originalScore=${entry.score}")
        }
        val untouched = prelim.drop(topN)  // 保持原分
        if (untouched.isNotEmpty()) {
            Log.i("SpecificMatcher", "↓ Untouched entries (not re-ranked): ${untouched.size}")
            untouched.forEach {
                Log.i("SpecificMatcher", "    – ${it.restaurant.name} | originalScore=${it.score}")
            }
        }

        /* ② iterate & mutate scores */
        slice.indices.forEach { idx ->
            val entry = slice[idx]
            val restTags = entry.restaurant.specific_tags

            if (restTags.isEmpty()) {
                Log.i("SpecificMatcher", "[${entry.restaurant.name}] skipped (no specific_tags)")
                return@forEach
            }

            var delta = 0
            for (userTag in userTags) {

                val emb = userTag.embedding

                if (emb == null) {
                    Log.w("SpecificMatcher", "⚠️  UserTag '${userTag.tag}' → embedding is NULL")
                    continue
                } else if (emb.all { it.toDouble() == 0.0 }) {
                    Log.w("SpecificMatcher", "⚠️  UserTag '${userTag.tag}' → embedding is all zero")
                } else {
                    Log.i("SpecificMatcher", "✓ UserTag '${userTag.tag}' → embedding sample = ${emb.take(5)}")
                }

                val best = restTags.maxByOrNull { restTag ->
                    cosine(userTag.embedding, restTag.embedding)
                } ?: continue

                val sim = cosine(userTag.embedding, best.embedding)
                val scoreDelta = when {
                    sim < THRESHOLD -> {
                        Log.i("SpecificMatcher", "[${entry.restaurant.name}] ✗ '${userTag.tag}' vs '${best.tag}' (sim=$sim) < $THRESHOLD → −$PENALTY_MISMATCH")
                        PENALTY_MISMATCH
                    }
                    best.polarity.equals("positive", true) -> {
                        Log.i("SpecificMatcher", "[${entry.restaurant.name}] ✓ '${userTag.tag}' vs '${best.tag}' (sim=$sim, polarity=positive) → +$BONUS_POSITIVE")
                        BONUS_POSITIVE
                    }
                    else -> {
                        Log.i("SpecificMatcher", "[${entry.restaurant.name}] ✓ '${userTag.tag}' vs '${best.tag}' (sim=$sim, polarity=negative) → +$BONUS_OTHER")
                        BONUS_OTHER
                    }
                }

                delta += scoreDelta
            }

            if (delta != 0) {
                Log.i("SpecificMatcher", "[${entry.restaurant.name}] ↑ total delta = $delta → new score = ${entry.score + delta}")
            }

            slice[idx] = entry.copy(score = entry.score + delta)
        }

        /* ③ merge + global reorder */
        return (slice + untouched).sortedByDescending { it.score }
    }
}
