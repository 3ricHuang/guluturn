package com.eric.guluturn.repository.impl

import com.eric.guluturn.common.models.*
import com.eric.guluturn.repository.iface.IRestaurantRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore-based implementation of IRestaurantRepository.
 * This repository handles retrieval of restaurant data from the Firestore "restaurants" collection.
 */
class FirestoreRestaurantRepository : IRestaurantRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Fetches all restaurant documents from Firestore.
     *
     * @return A list of all available Restaurant objects with their Firestore document IDs,
     *         or an empty list on failure.
     */
    override suspend fun getAllRestaurants(): List<Restaurant> {
        return try {
            db.collection("restaurants")
                .get()
                .await()
                .mapNotNull { parseRestaurant(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Retrieves restaurants that contain the given general tag.
     *
     * @param tag A general tag used to filter restaurants (e.g., "prefer_rice").
     * @return A list of Restaurant objects with Firestore document IDs, or an empty list on failure.
     */
    override suspend fun getByTag(tag: String): List<Restaurant> {
        return try {
            db.collection("restaurants")
                .whereArrayContains("general_tags", tag)
                .get()
                .await()
                .mapNotNull { parseRestaurant(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Retrieves a single restaurant document by its Firestore document ID.
     *
     * @param id The Firestore document ID to look up.
     * @return The corresponding Restaurant object with ID if found; null otherwise.
     */
    override suspend fun getById(id: String): Restaurant? {
        return try {
            val doc = db.collection("restaurants")
                .document(id)
                .get()
                .await()
            if (doc.exists()) parseRestaurant(doc) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retrieves a random subset of restaurants from Firestore.
     *
     * @param limit The number of restaurants to retrieve (default is 6).
     * @return A randomly selected list of Restaurant objects.
     */
    suspend fun getRandomRestaurants(limit: Int = 6): List<Restaurant> {
        return try {
            db.collection("restaurants")
                .get()
                .await()
                .shuffled()
                .take(limit)
                .mapNotNull { parseRestaurant(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses a Firestore document into a Restaurant object, with fallbacks for:
     * - name_embedding: ensures List<Double> even if raw Firestore stores List<Long> or empty
     * - specific_tags: parses from nested map objects (tag + polarity)
     * - location: gracefully handles missing fields or null values
     * - business_hours: supports Map<String, Map<String, String>> structure
     */
    private fun parseRestaurant(doc: DocumentSnapshot): Restaurant? {
        return try {
            val nameEmbedding = (doc.get("name_embedding") as? List<*>)?.mapNotNull {
                when (it) {
                    is Number -> it.toDouble()
                    is String -> it.toDoubleOrNull()
                    else -> null
                }
            } ?: emptyList()
            if (nameEmbedding.isEmpty()) {
                println("Empty name_embedding for restaurant ${doc.id} - name = ${doc.getString("name")}")
            }

            val specificTagRaw = doc.get("specific_tags") as? List<*>
            val specificTags = specificTagRaw?.mapNotNull { item ->
                if (item is Map<*, *>) {
                    val tag = item["tag"] as? String
                    val polarity = item["polarity"] as? String
                    val embedding = (item["embedding"] as? List<*>)?.mapNotNull {
                        when (it) {
                            is Number -> it.toDouble()
                            is String -> it.toDoubleOrNull()
                            else -> null
                        }
                    } ?: emptyList()

                    if (tag != null && polarity != null)
                        SpecificTag(tag, polarity, embedding)
                    else null
                } else null
            } ?: emptyList()

            val locationMap = doc.get("location") as? Map<*, *>
            val location = Location(
                lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: 0.0,
                lng = (locationMap?.get("lng") as? Number)?.toDouble() ?: 0.0,
                address = locationMap?.get("address") as? String ?: ""
            )

            val businessHoursMap = doc.get("business_hours") as? Map<*, *>
            val businessHours = businessHoursMap?.mapNotNull { (weekday, timeMap) ->
                if (weekday is String && timeMap is Map<*, *>) {
                    val open = timeMap["open"] as? String
                    val close = timeMap["close"] as? String
                    weekday to BusinessHour(open, close)
                } else null
            }?.toMap() ?: emptyMap()

            Restaurant(
                id = doc.id,
                name = doc.getString("name") ?: "",
                summary = doc.getString("summary") ?: "",
                general_tags = doc.get("general_tags") as? List<String> ?: emptyList(),
                specific_tags = specificTags,
                location = location,
                price_range = doc.getString("price_range"),
                rating = doc.getDouble("rating"),
                review_count = doc.getLong("review_count")?.toInt() ?: 0,
                business_hours = businessHours,
                name_embedding = nameEmbedding
            )
        } catch (e: Exception) {
            println("Error parsing restaurant document ${doc.id}: ${e.message}")
            null
        }
    }
}
