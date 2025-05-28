package com.eric.guluturn.ui.roulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.semantic.iface.ISemanticEngine

/**
 * Factory for constructing RouletteViewModel with injected dependencies.
 */
class RouletteViewModelFactory(
    private val filterEngine: StatefulFilterEngineImpl,
    private val semanticEngine: ISemanticEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RouletteViewModel(filterEngine, semanticEngine) as T
    }
}