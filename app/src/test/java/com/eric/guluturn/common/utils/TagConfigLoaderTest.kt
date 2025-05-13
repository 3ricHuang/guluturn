package com.eric.guluturn.common.utils

import kotlin.test.*
import org.junit.Test

class TagConfigLoaderTest {

    @Test
    fun testLoadAllTags_parsesCorrectly() {
        val tags = TagConfigLoader.loadAllTags()
        assertTrue(tags.isNotEmpty(), "Tag list should not be empty")
        assertTrue(tags.any { it.tag == "avoid_pork" && it.polarity == "negative" })
    }

    @Test
    fun testLoadTagNames_containsExpectedTags() {
        val tagNames = TagConfigLoader.loadTagNames()
        assertTrue("avoid_pork" in tagNames)
        assertTrue("prefer_sweet_dishes" in tagNames)
    }
}
