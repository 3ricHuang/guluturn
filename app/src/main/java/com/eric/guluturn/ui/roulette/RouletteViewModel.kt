package com.eric.guluturn.ui.roulette

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.common.models.SpecificTag
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.semantic.iface.ISemanticEngine
import com.eric.guluturn.semantic.iface.ParsedUserInput
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the state and logic of the roulette screen.
 * It integrates with the filtering and semantic engines to fetch and update
 * restaurant recommendations based on user input.
 *
 * @param filterEngine The filtering engine handling tag-based recommendations.
 * @param allRestaurants The full list of available restaurants from Firestore.
 * @param semanticEngine The semantic engine that parses natural language input into tags.
 */
class RouletteViewModel(
    private val filterEngine: StatefulFilterEngineImpl,
    private val allRestaurants: List<Restaurant>,
    private val semanticEngine: ISemanticEngine
) : ViewModel() {

    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> get() = _restaurants

    val spinCount = MutableLiveData(0)

    /**
     * Loads a new batch of recommended restaurants.
     *
     * If the reason is empty, fallback to random 6 restaurants.
     * Otherwise, parse semantic tags and apply filter.
     */
    fun loadRecommendedRestaurants(reason: String = "") {
        viewModelScope.launch {
            val result = if (reason.isBlank()) {
                allRestaurants.shuffled().take(6)
            } else {
                val parsed: ParsedUserInput = semanticEngine.parseInput(reason)
                filterEngine.updateAndFilter(
                    userGeneralTags = parsed.generalTags,
                    userSpecificTags = parsed.specificTags,
                    allRestaurants = allRestaurants
                ).mapNotNull { scored ->
                    allRestaurants.find { it.id == scored.id }
                }.take(6)
            }

            _restaurants.value = result
            spinCount.value = 0
        }
    }

    /**
     * Marks a restaurant as rejected and reloads new recommendations based on reason.
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
