package com.eric.guluturn.filter.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TagRegistryTest {

    @Test
    fun `can load known tag and verify metadata`() {
        val tag = TagRegistry.get("avoid_pork")
        assertNotNull(tag)
        assertEquals("negative", tag.polarity)
        assertEquals("soft", tag.strength)
    }

    @Test
    fun `returns null for nonexistent tag`() {
        val tag = TagRegistry.get("non_existent_tag")
        assertNull(tag)
    }

    @Test
    fun `isHardNegative returns true for hard negative tag`() {
        val isHard = TagRegistry.isHardNegative("no_halal_options")
        assertTrue(isHard)
    }

    @Test
    fun `isHardNegative returns false for soft or positive tags`() {
        assertFalse(TagRegistry.isHardNegative("avoid_pork"))       // soft
        assertFalse(TagRegistry.isHardNegative("prefer_fresh_dishes")) // positive
    }
}
