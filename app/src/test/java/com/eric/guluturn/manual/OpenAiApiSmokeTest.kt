package com.eric.guluturn.manual

import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import com.eric.guluturn.semantic.models.OpenAiResponseParsed
import com.eric.guluturn.common.utils.TagConfigLoader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class OpenAiApiSmokeTest {

    private val apiKey = System.getenv("OPENAI_API_KEY") ?: ""
    private val model = OpenAiModels(apiKey, model = "gpt-3.5-turbo")
    private val generator = OpenAiTagGenerator(model)
    private val allowedGeneralTags = TagConfigLoader.loadTagNames()

    @Test
    fun smokeStructuredTagParsing() = runBlocking {
        if (apiKey.isBlank()) {
            println("SKIPPED: OPENAI_API_KEY is not set.")
            return@runBlocking
        }

        val input = "太油膩"
        val result: OpenAiResponseParsed

        try {
            result = generator.generateStructuredTags(input)
        } catch (e: Exception) {
            fail("OpenAI call or parse failed: ${e.message}")
        }

        println("Parsed response: $result")

        assertTrue(result.userInput.isNotBlank(), "user_input should not be blank")

        result.generalTags.forEach {
            assertTrue(it in allowedGeneralTags, "Invalid general tag: $it")
        }

        result.specificTags.forEach {
            assertTrue(it.tag.isNotBlank(), "specific tag should not be blank")
            assertTrue(it.polarity in listOf("positive", "negative"), "Invalid polarity: ${it.polarity}")
        }
    }
}
