package com.eric.guluturn.repository.iface

import com.eric.guluturn.common.models.Restaurant

/**
 * Repository interface for accessing restaurant data from a data source (e.g., Firestore).
 * This abstraction allows decoupling the data access layer from the UI or domain layers.
 */
interface IRestaurantRepository {

    /**
     * Retrieves all restaurants from the underlying data source.
     *
     * @return A list of all available restaurants.
     */
    suspend fun getAllRestaurants(): List<Restaurant>

    /**
     * Queries restaurants that contain a specific general tag.
     *
     * @param tag The general tag to filter restaurants by (e.g., "prefer_spicy_dishes").
     * @return A list of restaurants matching the given tag.
     */
    suspend fun getByTag(tag: String): List<Restaurant>

    /**
     * Fetches a single restaurant by its unique document ID.
     *
     * @param id The Firestore document ID of the restaurant.
     * @return The corresponding Restaurant object, or null if not found.
     */
    suspend fun getById(id: String): Restaurant?
}
