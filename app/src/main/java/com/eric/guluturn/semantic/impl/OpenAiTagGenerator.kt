package com.eric.guluturn.semantic.impl

import com.eric.guluturn.semantic.exceptions.OpenAiTagException
import com.eric.guluturn.semantic.iface.TagGenerator
import com.eric.guluturn.semantic.models.*
import com.eric.guluturn.semantic.templates.PromptTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class OpenAiTagGenerator(
    private val apiModel: OpenAiModels
) : TagGenerator {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Return only general tags (for legacy interface use).
     */
    override suspend fun generateTags(input: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val parsed = generateStructuredTags(input)
            parsed.generalTags
        } catch (e: Exception) {
            println("Error extracting general tags: ${e.message}")
            emptyList()
        }
    }

    /**
     * Return structured result: user_input + general_tags + specific_tags.
     */
    suspend fun generateStructuredTags(input: String): OpenAiResponseParsed = withContext(Dispatchers.IO) {
        try {
            val prompt = PromptTemplates.generatePrompt(input)
            val responseJson = apiModel.callOpenAiApi(prompt)

            val outer = jsonParser.decodeFromString<OpenAiResponseBody>(responseJson)
            val content = outer.choices.firstOrNull()?.message?.content
                ?: throw OpenAiTagException("Missing content in OpenAI response")

            return@withContext jsonParser.decodeFromString<OpenAiResponseParsed>(content)
        } catch (e: SerializationException) {
            throw OpenAiTagException("Failed to parse OpenAI response: ${e.message}")
        }
    }
}
