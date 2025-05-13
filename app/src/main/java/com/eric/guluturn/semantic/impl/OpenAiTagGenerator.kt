package com.eric.guluturn.semantic.impl

import com.eric.guluturn.semantic.iface.TagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import com.eric.guluturn.semantic.templates.PromptTemplates
import com.eric.guluturn.semantic.exceptions.OpenAiTagException
import com.eric.guluturn.semantic.models.OpenAiResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class OpenAiTagGenerator(
    private val apiModel: OpenAiModels
) : TagGenerator {

    // JSON parser with safe fallback
    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun generateTags(input: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = PromptTemplates.generatePrompt(input)
            val response = apiModel.callOpenAiApi(prompt)
            parseTagsFromResponse(response)
        } catch (e: OpenAiTagException) {
            println("Error during OpenAI API call: ${e.message}")
            emptyList()
        } catch (e: SerializationException) {
            println("Error parsing OpenAI response: ${e.message}")
            emptyList()
        }
    }

    private fun parseTagsFromResponse(json: String): List<String> {
        return try {
            val root = jsonParser.decodeFromString<OpenAiResponseBody>(json)
            val content = root.choices.firstOrNull()?.message?.content ?: return emptyList()
            jsonParser.decodeFromString(content)
        } catch (e: SerializationException) {
            throw OpenAiTagException("Invalid response format: ${e.message}")
        }
    }
}
