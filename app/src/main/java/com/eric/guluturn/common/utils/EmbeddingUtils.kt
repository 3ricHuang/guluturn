package com.eric.guluturn.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.math.sqrt

object EmbeddingUtils {

    private val client = OkHttpClient()
    private val JSON   = "application/json".toMediaType()
    private const val URL = "https://api.openai.com/v1/embeddings"

    /** One-shot call to text-embedding-3-small */
    suspend fun embed(apiKey: String, text: String): List<Double> = withContext(Dispatchers.IO) {
        val body = """{"model":"text-embedding-3-small","input":${JSONObject.quote(text)}}"""
            .toRequestBody(JSON)

        val req = Request.Builder()
            .url(URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body!!.string())
            val arr  = json.getJSONArray("data")
                .getJSONObject(0)
                .getJSONArray("embedding")
            return@withContext List(arr.length()) { i -> arr.getDouble(i) }
        }
    }

    /** Cosine similarity for equal-length vectors */
    fun cosine(a: List<Double>, b: List<Double>): Double {
        val dot = a.indices.sumOf { a[it] * b[it] }
        val na  = sqrt(a.sumOf { it*it })
        val nb  = sqrt(b.sumOf { it*it })
        return if (na == 0.0 || nb == 0.0) 0.0 else dot / (na * nb)
    }
}
