package com.eric.guluturn.ui.roulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl

/**
 * Factory for constructing RouletteViewModel with injected dependencies.
 */
class RouletteViewModelFactory(
    private val filterEngine: StatefulFilterEngineImpl,
    private val allRestaurants: List<Restaurant>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouletteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouletteViewModel(filterEngine, allRestaurants) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
