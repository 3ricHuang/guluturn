package com.eric.guluturn.semantic.models

import android.util.Log
import com.eric.guluturn.semantic.exceptions.OpenAiTagException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

class OpenAiModels(val apiKey: String, val model: String = "gpt-4o") {

    suspend fun callOpenAiApi(prompt: String): String = withContext(Dispatchers.IO) {
        val requestBody = buildRequestBody(prompt)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw OpenAiTagException("HTTP ${response.code}: ${response.message}")
            }
            response.body?.string() ?: throw OpenAiTagException("Empty response from OpenAI")
        }
    }

    suspend fun callOpenAiApiWithRetry(prompt: String, maxRetries: Int = 3): String {
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return callOpenAiApi(prompt)
            } catch (e: Exception) {
                Log.w("OpenAiRetry", "Attempt ${attempt + 1} failed: ${e.message}")
                lastError = e
            }
        }

        throw lastError ?: OpenAiTagException("Unknown error during OpenAI retry")
    }

    private fun buildRequestBody(prompt: String): okhttp3.RequestBody {
        val json = Json.encodeToString(
            OpenAiRequestBody(
                model = model,
                messages = listOf(ChatMessage("user", prompt)),
                temperature = 0.0,
                topP = 1.0,
                frequencyPenalty = 0.0,
                presencePenalty = 0.0
            )
        )
        return json.toRequestBody("application/json".toMediaType())
    }
}
