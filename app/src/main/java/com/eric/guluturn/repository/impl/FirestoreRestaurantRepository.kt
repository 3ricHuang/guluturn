package com.eric.guluturn.repository.impl

import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.repository.iface.IRestaurantRepository
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
                .mapNotNull { doc ->
                    doc.toObject(Restaurant::class.java)?.copy(id = doc.id)
                }
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
                .mapNotNull { doc ->
                    doc.toObject(Restaurant::class.java)?.copy(id = doc.id)
                }
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

            if (doc.exists()) {
                doc.toObject(Restaurant::class.java)?.copy(id = doc.id)
            } else null
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
                .shuffled()  // 🔹 Randomize order
                .take(limit)
                .mapNotNull { doc ->
                    doc.toObject(Restaurant::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
