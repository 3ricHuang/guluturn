package com.eric.guluturn.integration

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eric.guluturn.common.models.RestaurantCandidate
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.impl.FirestoreRestaurantRepository
import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullPipelineInstrumentationTest {
    /*
        private val testInputs = listOf(
        "我想吃牛肉麵，熱熱的最好",
        "我想吃大大牛肉麵、拉亞、品翔、左岸、瑪西所、御之味",
        "It's too hot today, need something cold and not greasy.",
        "隨便啦不知道，你決定",
        "我需要清真的，但不要牛肉，太油也不行",
        "那間店今天沒開，我記得週二公休",
        "我想吃拉亞和頂呱呱，其他都可以",
        "我要吃麥當勞和王品",
        "我不吃牛、不吃羊、不吃雞、不吃豬、不吃蛋、不吃蔥薑蒜、不吃辣、要純素且店內要播放古典樂"
    )
     */
    private val testInputs = listOf(
        "我想吃牛肉麵，熱熱的最好"
    )

    @Test
    fun fullPipeline_withVariousInputs_shouldReturnSixCardsAndCorrectLogs() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val apiKey = ApiKeyStorage.getSavedApiKey(context)
            ?: error("請先在 App 內儲存 API key")

        val repo = FirestoreRestaurantRepository()
        val pool = repo.getAllRestaurants()
        assertTrue("Firestore 中店家數量不足", pool.size >= 20)

        testInputs.forEachIndexed { idx, input ->
            Log.i("FullPipelineTest", "===== CASE #$idx =====")
            Log.i("FullPipelineTest", "User Input: $input")

            val semantic = buildSemanticEngine(apiKey)
            val parsed = semantic.parseInput(input)

            val engine = buildFilterEngine(apiKey, semantic)
            val result: List<RestaurantCandidate> = engine.filterWithSource(input, pool)

            assertEquals("應回傳 6 間餐廳 (case #$idx)", 6, result.size)

            Log.i("FullPipelineTest", "generalTags = ${parsed.generalTags}")
            Log.i("FullPipelineTest", "specificTags = ${parsed.specificTags.map { it.tag + "/" + it.polarity }}")
            Log.i("FullPipelineTest", "preferredRestaurants = ${parsed.preferredRestaurants}")

            result.forEachIndexed { i, r ->
                val tags = r.restaurant.general_tags.joinToString()
                val scoreStr = r.score?.toString() ?: "N/A"
                Log.i("FullPipelineTest", "${i + 1}. ${r.restaurant.name} [${r.source}] | score=$scoreStr | tags: $tags")
            }

            if ("avoid_beef" in parsed.generalTags) {
                result.forEach {
                    assertFalse(
                        "含 avoid_beef 應不推薦含 prefer_beef 餐廳",
                        it.restaurant.general_tags.contains("prefer_beef")
                    )
                }
            }
        }
        delay(2000)
    }

    private fun buildSemanticEngine(apiKey: String): OpenAiTagGenerator {
        return OpenAiTagGenerator(OpenAiModels(apiKey))
    }

    private fun buildFilterEngine(apiKey: String, semantic: OpenAiTagGenerator): StatefulFilterEngineImpl {
        return StatefulFilterEngineImpl(
            apiKey = apiKey,
            semantic = semantic
        )
    }


}
