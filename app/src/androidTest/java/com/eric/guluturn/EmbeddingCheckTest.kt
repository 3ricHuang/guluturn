package com.eric.guluturn

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmbeddingCheckTest {

    @Test
    fun checkAllRestaurantEmbeddings() = runBlocking {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("restaurants").get().await()

        val failed = mutableListOf<String>()
        var total = 0

        for (doc in snapshot.documents) {
            total++
            val name = doc.getString("name") ?: "(no name)"
            val embedding = doc.get("name_embedding") as? List<*>

            if (embedding == null) {
                println("❌ $name → name_embedding = null")
                failed += name
                continue
            }

            if (embedding.any { it !is Number }) {
                println("❌ $name → name_embedding contains non-numeric values")
                failed += name
                continue
            }

            if (embedding.size != 1536) {
                println("⚠️  $name → embedding size = ${embedding.size}")
                failed += name
            }
        }

        println("\n====== Summary ======")
        println("Total restaurants: $total")
        println("Invalid embeddings : ${failed.size}")
        if (failed.isNotEmpty()) {
            println("Problematic entries:")
            failed.forEach { println(" - $it") }
        }

        assert(failed.isEmpty()) { "Found ${failed.size} problematic restaurants." }
    }

    @Test
    fun checkSpecificTagEmbeddings() = runBlocking {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("restaurants").get().await()

        val failed = mutableListOf<String>()
        var totalTags = 0

        for (doc in snapshot.documents) {
            val name = doc.getString("name") ?: "(no name)"
            val specificTags = doc.get("specific_tags") as? List<Map<String, Any>> ?: continue

            for ((i, tag) in specificTags.withIndex()) {
                totalTags++
                val tagText = tag["tag"]?.toString() ?: "(no tag string)"
                val embedding = tag["embedding"] as? List<*>

                if (embedding == null) {
                    println("❌ [$name] tag[$i] = '$tagText' → embedding is NULL")
                    failed += "$name :: $tagText"
                    continue
                }

                if (embedding.isEmpty()) {
                    println("❌ [$name] tag[$i] = '$tagText' → embedding is EMPTY")
                    failed += "$name :: $tagText"
                    continue
                }

                if (embedding.size != 1536) {
                    println("❌ [$name] tag[$i] = '$tagText' → embedding size = ${embedding.size}")
                    failed += "$name :: $tagText"
                    continue
                }

                if (embedding.any { it !is Number }) {
                    println("❌ [$name] tag[$i] = '$tagText' → embedding contains non-numeric values")
                    failed += "$name :: $tagText"
                    continue
                }
            }
        }

        println("\n====== Specific Tag Embedding Summary ======")
        println("Total specific tags: $totalTags")
        println("Invalid embeddings : ${failed.size}")
        if (failed.isNotEmpty()) {
            println("Problematic entries:")
            failed.forEach { println(" - $it") }
        }

        assert(failed.isEmpty()) { "Found ${failed.size} invalid specific tag embeddings." }
    }

}
