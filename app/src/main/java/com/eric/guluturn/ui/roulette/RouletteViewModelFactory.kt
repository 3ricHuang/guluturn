package com.eric.guluturn.ui.roulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.iface.IInteractionSessionRepository
import com.eric.guluturn.semantic.iface.ISemanticEngine

/**
 * Factory for constructing RouletteViewModel with injected dependencies.
 */
class RouletteViewModelFactory(
    private val filterEngine: StatefulFilterEngineImpl,
    private val semanticEngine: ISemanticEngine,
    private val interactionSessionRepository: IInteractionSessionRepository,
    private val currentUserUuid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RouletteViewModel(
            filterEngine = filterEngine,
            semanticEngine = semanticEngine,
            interactionSessionRepository = interactionSessionRepository,
            currentUserUuid = currentUserUuid
        ) as T
    }
}
