package com.eric.guluturn.filter.compose

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.utils.EmbeddingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compose six cards:
 *   1. lock-in user-named restaurants (exact → embedding fallback) from fullPool
 *   2. pad with top-ranked remainder from ranked
 */
object UserConstraintComposer {

    private const val CARD_COUNT = 6
    private const val COSINE_THRESHOLD = 0.5

    suspend fun compose(
        fullPool: List<Restaurant>,      // all candidates in Firestore
        ranked: List<Restaurant>,        // already score-sorted
        preferredNames: List<String>,
        apiKey: String
    ): List<Restaurant> = withContext(Dispatchers.Default) {

        val result = mutableListOf<Restaurant>()
        val remain = ranked.toMutableList()
        val all = fullPool.toMutableList()

        for (name in preferredNames) {
            println(">>> now checking preferred name: '$name'")

            // 1. exact or substring match from fullPool
            val idx = all.indexOfFirst { it.name.contains(name, ignoreCase = true) }
            if (idx >= 0) {
                val match = all.removeAt(idx)
                println("✓ Exact match: '$name' → ${match.name}")
                result += match
                remain.removeIf { it.name == match.name }
                continue
            }

            // 2. fallback to embedding match
            println("No exact match for '$name', try embedding fallback")
            val queryVec = EmbeddingUtils.embed(apiKey, name)

            val best = all.maxByOrNull {
                EmbeddingUtils.cosine(queryVec, it.name_embedding)
            }
            val sim = best?.let {
                EmbeddingUtils.cosine(queryVec, it.name_embedding)
            } ?: 0.0

            if (best != null && sim >= COSINE_THRESHOLD) {
                println("Fallback match: '$name' → ${best.name} (sim=$sim)")
                result += best
                all.remove(best)
                remain.removeIf { it.name == best.name }
            } else {
                println("✗ Fallback failed: '$name' (sim=$sim)")
            }
        }

        // 3. pad with top-ranked remainder
        result += remain.take(CARD_COUNT - result.size)
        return@withContext result.take(CARD_COUNT)
    }
}
