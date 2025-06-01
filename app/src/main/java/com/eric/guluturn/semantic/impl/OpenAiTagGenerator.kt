package com.eric.guluturn.semantic.impl

import com.eric.guluturn.semantic.exceptions.OpenAiTagException
import com.eric.guluturn.semantic.iface.TagGenerator
import com.eric.guluturn.semantic.iface.ISemanticEngine
import com.eric.guluturn.semantic.iface.ParsedUserInput
import com.eric.guluturn.semantic.models.*
import com.eric.guluturn.semantic.templates.PromptTemplates
import com.eric.guluturn.utils.EmbeddingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Calls OpenAI, returns structured tags + restaurant constraints.
 */
class OpenAiTagGenerator(
    private val apiModel: OpenAiModels
) : TagGenerator, ISemanticEngine {

    private val json = Json { ignoreUnknownKeys = true }

    /* legacy TagGenerator interface -------------------------------------- */
    override suspend fun generateTags(input: String): List<String> =
        parseInput(input).generalTags

    /* full structured parse --------------------------------------------- */
    override suspend fun parseInput(reason: String): ParsedUserInput = withContext(Dispatchers.IO) {
        val prompt = PromptTemplates.generatePrompt(reason)
        val raw = apiModel.callOpenAiApiWithRetry(prompt)

        val outer = json.decodeFromString<OpenAiResponseBody>(raw)
        val content = outer.choices.first().message.content
            ?: throw OpenAiTagException("Empty OpenAI content")

        val parsed = json.decodeFromString<OpenAiResponseParsed>(content)

        val enrichedSpecific = parsed.specificTags.map { tag ->
            val vec = EmbeddingUtils.embed(apiModel.apiKey, tag.tag)
            tag.copy(embedding = vec)          // 假設 SpecificTag 有 embedding 欄位
        }

        return@withContext ParsedUserInput(
            generalTags          = parsed.generalTags,
            specificTags         = enrichedSpecific,
            preferredRestaurants = parsed.preferredRestaurants
        )
    }
}
