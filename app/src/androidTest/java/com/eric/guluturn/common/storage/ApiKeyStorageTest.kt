package com.eric.guluturn.common.storage

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ApiKeyStorage.
 * Verifies saving, reading, and clearing OpenAI API key using SharedPreferences.
 */
@RunWith(AndroidJUnit4::class)
class ApiKeyStorageTest {

    private lateinit var context: Context
    private val sampleKey = "sk-test-123456"

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ApiKeyStorage.clearApiKey(context) // Start fresh
    }

    @After
    fun teardown() {
        ApiKeyStorage.clearApiKey(context)
    }

    @Test
    fun testSaveAndReadApiKey() {
        ApiKeyStorage.saveApiKey(context, sampleKey)
        val saved = ApiKeyStorage.getSavedApiKey(context)
        assertEquals(sampleKey, saved)
    }

    @Test
    fun testClearApiKey() {
        ApiKeyStorage.saveApiKey(context, sampleKey)
        ApiKeyStorage.clearApiKey(context)
        val result = ApiKeyStorage.getSavedApiKey(context)
        assertNull(result)
    }

    @Test
    fun testReadBeforeSaveReturnsNull() {
        val result = ApiKeyStorage.getSavedApiKey(context)
        assertNull(result)
    }
}
