package com.eric.guluturn.manual

import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import com.eric.guluturn.semantic.templates.PromptTemplates
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.fail

class OpenAiApiSmokeTest {

    private val apiKey = System.getenv("OPENAI_API_KEY") ?: ""

    @Test
    fun smoke() = runBlocking {
        if (apiKey.isBlank()) {
            println("SKIPPED: OPENAI_API_KEY is not set.")
            return@runBlocking
        }

        val model = OpenAiModels(apiKey, model = "gpt-3.5-turbo")
        val generator = OpenAiTagGenerator(model)

        val prompt = PromptTemplates.generatePrompt("太油膩")
        println("Prompt:\n$prompt")

        val tags = generator.generateTags("太油膩")
        println("Tags returned: $tags")

        if (tags.isEmpty()) {
            fail("Test failed: OpenAI returned an empty list.")
        }
    }
}
