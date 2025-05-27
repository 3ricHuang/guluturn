package com.eric.guluturn.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eric.guluturn.repository.impl.FirestoreRestaurantRepository
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import com.eric.guluturn.filter.iface.StatefulFilterEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreFilterIntegrationTest {

    private val repository = FirestoreRestaurantRepository()

    @Test
    fun fullPipeline_returnsSixRestaurants_whenUsingSemanticTags() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val testApiKey = "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"
        ApiKeyStorage.saveApiKey(context, testApiKey)
        val apiKey = ApiKeyStorage.getSavedApiKey(context)
            ?: error("Missing API key. Please login before running this test.")

        val semantic = OpenAiTagGenerator(OpenAiModels(apiKey))
        val reason = "I want a vegetarian place but not too salty or greasy."
        val parsedTags = semantic.generateStructuredTags(reason)

        val allRestaurants = repository.getAllRestaurants()
        assertTrue("Should have enough restaurant data", allRestaurants.size >= 6)

        val engine = StatefulFilterEngine()
        val filtered = engine.updateAndFilter(
            userGeneralTags = parsedTags.generalTags,
            userSpecificTags = parsedTags.specificTags,
            allRestaurants = allRestaurants
        ).mapNotNull { scored ->
            allRestaurants.find { it.name == scored.id }
        }

        assertEquals(6, filtered.size)
        println("Input: $reason")
        println("Parsed tags: ${parsedTags.generalTags} / ${parsedTags.specificTags.map { it.tag }}")
        println("Recommended restaurants:")
        filtered.forEach { println(it.name) }
    }
}
