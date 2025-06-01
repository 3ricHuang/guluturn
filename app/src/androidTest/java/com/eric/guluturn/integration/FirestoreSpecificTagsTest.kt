package com.eric.guluturn.integration

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.impl.FirestoreRestaurantRepository
import com.eric.guluturn.semantic.iface.ISemanticEngine
import com.eric.guluturn.semantic.iface.ParsedUserInput
import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoodlesTest {

    private val testInputs = listOf(
        "我想吃牛肉麵，熱熱的最好"
    )

    @Test
    fun fullPipeline_withAndWithoutSpecificOverride_shouldCompareScores() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val apiKey = ApiKeyStorage.getSavedApiKey(context)
            ?: error("請先在 App 內儲存 API key")

        val repo = FirestoreRestaurantRepository()
        val pool = repo.getAllRestaurants()
        assertTrue("Firestore 中店家數量不足", pool.size >= 20)

        testInputs.forEachIndexed { idx, input ->
            Log.i("FullPipelineTest", "===== CASE #$idx ORIGINAL =====")
            Log.i("FullPipelineTest", "User Input: $input")

            // ---- 正常流程 ----
            val semantic = buildSemanticEngine(apiKey)
            val parsedOriginal = semantic.parseInput(input)

            val engine = buildFilterEngine(apiKey, semantic)
            val resultOriginal = engine.filterWithSource(input, pool)

            assertEquals("原始流程應回傳 6 間餐廳", 6, resultOriginal.size)

            Log.i("FullPipelineTest", "generalTags = ${parsedOriginal.generalTags}")
            Log.i("FullPipelineTest", "specificTags = ${parsedOriginal.specificTags.map { it.tag + "/" + it.polarity }}")
            Log.i("FullPipelineTest", "preferredRestaurants = ${parsedOriginal.preferredRestaurants}")
            resultOriginal.forEachIndexed { i, r ->
                val tags = r.restaurant.general_tags.joinToString()
                val scoreStr = r.score?.toString() ?: "N/A"
                Log.i("FullPipelineTest", "[ORIGINAL] ${i + 1}. ${r.restaurant.name} [${r.source}] | score=$scoreStr | tags: $tags")
            }

            if ("avoid_beef" in parsedOriginal.generalTags) {
                resultOriginal.forEach {
                    assertFalse(
                        "含 avoid_beef 應不推薦含 prefer_beef 餐廳",
                        it.restaurant.general_tags.contains("prefer_beef")
                    )
                }
            }

            // ---- 覆寫流程 ----
            Log.i("FullPipelineTest", "===== CASE #$idx FORCED beef noodle =====")

            val parsedForced = ParsedUserInput(
                generalTags = parsedOriginal.generalTags,
                specificTags = listOf(SpecificTag(tag = "beef noodle", polarity = "positive")),
                preferredRestaurants = parsedOriginal.preferredRestaurants
            )

            val dummySemantic = object : ISemanticEngine {
                override suspend fun parseInput(userInput: String): ParsedUserInput = parsedForced
            }

            val engineForced = buildFilterEngine(apiKey, dummySemantic)
            val resultForced = engineForced.filterWithSource(input, pool)

            assertEquals("覆寫流程應回傳 6 間餐廳", 6, resultForced.size)

            resultForced.forEachIndexed { i, r ->
                val tags = r.restaurant.general_tags.joinToString()
                val scoreStr = r.score?.toString() ?: "N/A"
                Log.i("FullPipelineTest", "[FORCED] ${i + 1}. ${r.restaurant.name} [${r.source}] | score=$scoreStr | tags: $tags")
            }
        }

        delay(2000)
    }

    private fun buildSemanticEngine(apiKey: String): ISemanticEngine {
        return OpenAiTagGenerator(OpenAiModels(apiKey))
    }

    private fun buildFilterEngine(apiKey: String, semantic: ISemanticEngine): StatefulFilterEngineImpl {
        return StatefulFilterEngineImpl(apiKey = apiKey, semantic = semantic)
    }
}
