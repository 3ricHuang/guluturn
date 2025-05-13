package com.eric.guluturn.common.utils

import kotlin.math.sqrt

object EmbeddingUtils {

    private const val EPSILON = 1e-6f

    /**
     * Calculate the cosine similarity between two embedding vectors.
     *
     * @param vec1 The first embedding vector.
     * @param vec2 The second embedding vector.
     * @return The cosine similarity as a Float value between -1.0 and 1.0.
     */
    fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Float {
        // 計算內積
        val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()

        // 計算向量長度
        val magnitude1 = sqrt(vec1.map { it * it }.sum())
        val magnitude2 = sqrt(vec2.map { it * it }.sum())

        // 檢查向量長度是否接近零，避免除以零
        if (magnitude1 < EPSILON || magnitude2 < EPSILON) {
            return 0.0f
        }

        return dotProduct / (magnitude1 * magnitude2)
    }

    /**
     * Normalize an embedding vector to unit length.
     *
     * @param vec The input embedding vector.
     * @return The normalized vector.
     */
    fun normalizeVector(vec: List<Float>): List<Float> {
        val magnitude = sqrt(vec.map { it * it }.sum())
        if (magnitude < EPSILON) return vec
        return vec.map { it / magnitude }
    }
}
