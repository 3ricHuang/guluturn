package com.eric.guluturn.semantic.templates

import kotlin.test.*
import org.junit.Test
import com.eric.guluturn.common.utils.TagConfigLoader

class PromptTemplatesTest {

    @Test
    fun testPromptIncludesStructureAndTags() {
        val input = "我吃素"
        val prompt = PromptTemplates.generatePrompt(input)
        val knownTags = TagConfigLoader.loadTagNames()

        println("----- Prompt Start -----")
        println(prompt)
        println("----- Prompt End -------")
        println("Loaded tags: $knownTags")

        assertTrue(prompt.contains("## Allowed tags"), "Prompt should contain allowed tags section")
        assertTrue(prompt.contains("## Instructions"), "Prompt should contain instructions section")
        assertTrue(prompt.contains("User input"), "Prompt should contain user input section")
        assertTrue(prompt.contains(input), "Prompt should include the original input")

        assertTrue(
            knownTags.any { prompt.contains(it) },
            "Prompt should list at least one known tag. If this fails, check if the YAML file was loaded correctly."
        )
    }
}
