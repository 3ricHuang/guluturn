package com.eric.guluturn.semantic.models

import com.eric.guluturn.semantic.exceptions.OpenAiTagException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

class OpenAiModels(private val apiKey: String, private val model: String = "gpt-3.5-turbo") {

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

    private fun buildRequestBody(prompt: String): okhttp3.RequestBody {
        val json = Json.encodeToString(
            OpenAiRequestBody(
                messages = listOf(ChatMessage("user", prompt)),
                model = model
            )
        )
        return json.toRequestBody("application/json".toMediaType())
    }
}
