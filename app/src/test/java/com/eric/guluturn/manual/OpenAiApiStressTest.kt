package com.eric.guluturn.manual

import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import com.eric.guluturn.semantic.models.OpenAiResponseParsed
import com.eric.guluturn.common.utils.TagConfigLoader
import com.eric.guluturn.common.utils.TestInputLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Manual stress test for OpenAI-based tag generator.
 *
 * To run manually, remove the @Ignore annotation.
 */
// @Ignore("Manual stress test – remove @Ignore to run")
class OpenAiApiStressTest {

    /** API key must be set via environment variable OPENAI_API_KEY. */
    private val apiKey = System.getenv("OPENAI_API_KEY") ?: ""

    /** The OpenAI model instance configured with authentication and model type. */
    private val model = OpenAiModels(apiKey, model = "gpt-4o")

    /** The tag generator implementation that uses the OpenAI API. */
    private val generator = OpenAiTagGenerator(model)

    /** Allowed general tags loaded from the tag YAML configuration. */
    private val allowedGeneralTags = TagConfigLoader.loadTagNames()

    /** Load test inputs from JSON resource. */
    private val inputs by lazy { TestInputLoader.loadJsonInputs("user_inputs.json") }

    @Test
    fun stressTestWithDiverseInputs() = runBlocking {
        if (apiKey.isBlank()) {
            println("SKIPPED: OPENAI_API_KEY is not set.")
            return@runBlocking
        }

        var failedCount = 0

        inputs.forEachIndexed { index, input ->
            println("\n[$index] Input: $input")

            val elapsed = measureTimeMillis {
                try {
                    val result: OpenAiResponseParsed = generator.generateStructuredTags(input)

                    println("Parsed general tags: ${result.generalTags}")
                    println("Parsed specific tags: " + result.specificTags.joinToString { "${it.tag} (${it.polarity})" })

                    result.generalTags.forEach {
                        assertTrue(it in allowedGeneralTags, "Invalid general tag: $it")
                    }

                    result.specificTags.forEach {
                        assertTrue(it.tag.isNotBlank(), "Specific tag is blank")
                        assertTrue(it.polarity in listOf("positive", "negative"), "Invalid polarity: ${it.polarity}")
                    }

                } catch (e: Exception) {
                    failedCount++
                    println("Error parsing input: ${e.message}")
                }
            }

            println("Response time: ${elapsed}ms")
            delay(1500) // Prevent rate-limiting
        }

        if (failedCount > 0) {
            fail("Stress test failed on $failedCount out of ${inputs.size} cases.")
        }
    }
}
