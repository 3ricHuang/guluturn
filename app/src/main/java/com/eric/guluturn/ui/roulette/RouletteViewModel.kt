package com.eric.guluturn.ui.roulette

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the state and logic of the roulette screen.
 * It integrates with the StatefulFilterEngineImpl to fetch and update
 * restaurant recommendations based on user preferences and rejection reasons.
 *
 * @param filterEngine The filtering engine handling tag-based recommendations.
 * @param allRestaurants The full list of available restaurants from Firestore.
 */
class RouletteViewModel(
    private val filterEngine: StatefulFilterEngineImpl,
    private val allRestaurants: List<Restaurant>
) : ViewModel() {

    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> get() = _restaurants

    val spinCount = MutableLiveData(0)

    /**
     * Loads a new batch of recommended restaurants.
     *
     * @param reason Optional natural language input from the user (e.g. rejection reason).
     * If empty, a generic recommendation is generated.
     */
    fun loadRecommendedRestaurants(reason: String = "") {
        viewModelScope.launch {
            val result = filterEngine.filter(reason, allRestaurants)
            println("DEBUG: filter returned ${result.size} restaurants")
            _restaurants.value = result
            spinCount.value = 0
        }
    }

    /**
     * Marks the specified restaurant as rejected and reloads new recommendations
     * based on the provided rejection reason.
     *
     * @param restaurant The restaurant to reject.
     * @param reason A natural language explanation for rejection.
     */
    fun rejectRestaurant(restaurant: Restaurant, reason: String) {
        viewModelScope.launch {
            filterEngine.reject(restaurant.id)
            loadRecommendedRestaurants(reason)
        }
    }

    /**
     * Increments the internal spin count (used to track rounds).
     */
    fun incrementSpinCount() {
        spinCount.value = (spinCount.value ?: 0) + 1
    }
}
