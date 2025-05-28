package com.eric.guluturn.ui.roulette

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.impl.FirestoreRestaurantRepository
import com.eric.guluturn.semantic.iface.ISemanticEngine
import com.eric.guluturn.semantic.iface.ParsedUserInput
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing roulette-based restaurant recommendations.
 * Handles initialization, rejection feedback, and semantic filtering across rounds.
 */
class RouletteViewModel(
    private val filterEngine: StatefulFilterEngineImpl,
    private val semanticEngine: ISemanticEngine
) : ViewModel() {

    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> get() = _restaurants

    private var fullRestaurantPool: List<Restaurant> = emptyList()
    val spinCount = MutableLiveData(0)

    /**
     * Initializes the view model with the initial restaurant pool.
     * Randomly selects 6 candidates for the first spin.
     */
    fun initialize(restaurants: List<Restaurant>) {
        fullRestaurantPool = restaurants
        _restaurants.value = restaurants.shuffled().take(6)
        spinCount.value = 0
        println("DEBUG: Initialized with ${restaurants.size} restaurants, showing 6.")
    }

    /**
     * Loads a new batch of recommended restaurants based on user input.
     * If no reason is provided, a random sample is used instead.
     */
    fun loadRecommendedRestaurants(reason: String = "") {
        viewModelScope.launch {
            println("DEBUG: loadRecommendedRestaurants invoked. Reason: \"$reason\"")

            val result = if (reason.isBlank()) {
                val random = fullRestaurantPool.shuffled().take(6)
                println("DEBUG: No reason provided. Using random sample of size ${random.size}")
                random
            } else {
                val parsed: ParsedUserInput = semanticEngine.parseInput(reason)
                println("DEBUG: Parsed general tags: ${parsed.generalTags}")
                println("DEBUG: Parsed specific tags: ${parsed.specificTags.map { "${it.tag}:${it.polarity}" }}")

                val filtered = filterEngine.updateAndFilter(
                    userGeneralTags = parsed.generalTags,
                    userSpecificTags = parsed.specificTags,
                    allRestaurants = fullRestaurantPool
                ).take(6)

                println("DEBUG: Filtered result size = ${filtered.size}")
                println("DEBUG: Filtered restaurant names: ${filtered.map { it.name }}")

                filtered
            }

            _restaurants.value = result
            spinCount.value = 0
        }
    }

    /**
     * Registers a rejection for a specific restaurant, refreshes full pool, then reloads new recommendations.
     */
    fun rejectRestaurant(restaurant: Restaurant, reason: String) {
        viewModelScope.launch {
            println("DEBUG: Rejected restaurant: ${restaurant.name} (ID=${restaurant.id})")
            filterEngine.reject(restaurant.id)

            println("DEBUG: Fetching full restaurant pool from Firestore...")
            fullRestaurantPool = FirestoreRestaurantRepository().getAllRestaurants()
            println("DEBUG: New full pool size: ${fullRestaurantPool.size}")

            loadRecommendedRestaurants(reason)
        }
    }

    /**
     * Rejects the currently selected restaurant, assuming the first in the list.
     */
    fun reject(reason: String) {
        val current = restaurants.value?.getOrNull(0) ?: return
        rejectRestaurant(current, reason)
    }

    /**
     * Registers user acceptance of the current restaurant (extension point).
     */
    fun acceptCurrent() {
        // Optional: implement acceptance tracking if needed
    }

    /**
     * Increments the internal spin counter (e.g., for animation or analytics).
     */
    fun incrementSpinCount() {
        spinCount.value = (spinCount.value ?: 0) + 1
    }
}
