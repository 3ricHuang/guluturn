package com.eric.guluturn.common.utils

object SimilarityCalculator {

    /**
     * Compare the similarity between two sets of embeddings.
     *
     * @param tagEmbedding The embedding of the generated tag.
     * @param restaurantEmbedding The embedding of the restaurant tag.
     * @return The similarity score as a Float.
     */
    fun compare(tagEmbedding: List<Float>, restaurantEmbedding: List<Float>): Float {
        return EmbeddingUtils.cosineSimilarity(tagEmbedding, restaurantEmbedding)
    }

    /**
     * Find the most similar restaurant tag from a list.
     *
     * @param tagEmbedding The embedding of the generated tag.
     * @param restaurantEmbeddings A map of restaurant tags and their embeddings.
     * @return The restaurant tag with the highest similarity score.
     */
    fun findMostSimilar(tagEmbedding: List<Float>, restaurantEmbeddings: Map<String, List<Float>>): String {
        return restaurantEmbeddings.maxByOrNull { (_, embedding) ->
            compare(tagEmbedding, embedding)
        }?.key ?: "unknown"
    }
}
