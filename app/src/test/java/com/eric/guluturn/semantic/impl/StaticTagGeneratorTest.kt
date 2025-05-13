package com.eric.guluturn.semantic.impl

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class StaticTagGeneratorTest {

    private val generator = StaticTagGenerator()

    @Test
    fun testValidSingleTagInput() = runBlocking {
        val result = generator.generateTags("avoid_pork")
        assertEquals(listOf("avoid_pork"), result)
    }

    @Test
    fun testMultipleTagInput() = runBlocking {
        val result = generator.generateTags("avoid_pork,quality_too_salty")
        assertEquals(setOf("avoid_pork", "quality_too_salty"), result.toSet())
    }

    @Test
    fun testInvalidTagIgnored() = runBlocking {
        val result = generator.generateTags("totally_invalid_tag")
        assertTrue(result.isEmpty())
    }
}
