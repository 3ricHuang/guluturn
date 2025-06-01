package com.eric.guluturn.ui.roulette

import AcceptedRestaurant
import InteractionRound
import InteractionSession
import LocationInfo
import RestaurantCandidate
import UserFeedback
import androidx.lifecycle.*
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.iface.IInteractionSessionRepository
import com.eric.guluturn.semantic.iface.ISemanticEngine
import kotlinx.coroutines.launch

/**
 * RouletteViewModel – orchestrates the spin-to-choose flow.
 *
 * Key change (2025-05):
 *   • loadRecommendedRestaurants() delegates to StatefulFilterEngineImpl.filter()
 *     which handles:  parsing → prefilter (open hours) → ranking → constraint compose.
 */
class RouletteViewModel(
    private val filterEngine: StatefulFilterEngineImpl,
    private val semanticEngine: ISemanticEngine,                       // still needed for feedback logging
    private val interactionSessionRepository: IInteractionSessionRepository,
    private val currentUserUuid: String
) : ViewModel() {

    /* ---------- LiveData ---------- */
    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> get() = _restaurants

    private val _spinCount = MutableLiveData(0)
    val spinCount: LiveData<Int> get() = _spinCount

    private val _sessionEnd = MutableLiveData<Unit>()
    val sessionEnd: LiveData<Unit> get() = _sessionEnd

    /* ---------- session state ---------- */
    private var restaurantPool: List<Restaurant> = emptyList()
    private val interactionRounds = mutableListOf<InteractionRound>()
    private var acceptedRestaurant: AcceptedRestaurant? = null
    private var currentSelection: Restaurant? = null

    /* ---------- public API ---------- */

    fun setSelectedRestaurant(r: Restaurant) {
        currentSelection = r
    }

    /** Initialise / reset a new spin session. */
    fun initialize(pool: List<Restaurant>) {
        restaurantPool = pool
        _restaurants.value = pool.shuffled().take(6)
        _spinCount.value = 0
        interactionRounds.clear()
        acceptedRestaurant = null
        currentSelection = null
    }

    /**
     * Main entry: fetch 6 recommendations based on optional reason.
     * Delegates to StatefulFilterEngineImpl.filter().
     */
    fun loadRecommendedRestaurants(reason: String = "") {
        viewModelScope.launch {
            val list = if (reason.isBlank()) {
                restaurantPool.shuffled().take(6)
            } else {
                filterEngine.filter(reason, restaurantPool)        // <-- single call does all the work
            }
            _restaurants.value = list
        }
    }

    /** User rejects the currently selected card with given reason. */
    fun reject(reason: String) {
        val cur = currentSelection ?: return
        rejectRestaurant(cur, reason)
    }

    /** Accept current selection -> end session immediately. */
    fun acceptCurrent() {
        val cur = currentSelection ?: return
        val spinIdx = _spinCount.value ?: 0

        acceptedRestaurant = cur.toAcceptedModel()

        recordRound(
            roundIndex = spinIdx,
            candidates = _restaurants.value.orEmpty(),
            selectedId = cur.id,
            feedback = UserFeedback(accepted = true, null, emptyList(), emptyList())
        )

        _spinCount.value = spinIdx + 1
        commitSession()
    }

    /* ---------- internal helpers ---------- */

    private fun rejectRestaurant(restaurant: Restaurant, reason: String) {
        viewModelScope.launch {
            /* we still parse reason for analytics (stored in InteractionRound) */
            val parsed = semanticEngine.parseInput(reason)

            val spinIdx = _spinCount.value ?: 0
            val selectedId = restaurant.id

            recordRound(
                roundIndex = spinIdx,
                candidates = _restaurants.value.orEmpty(),
                selectedId = selectedId,
                feedback = UserFeedback(
                    accepted = false,
                    reasonInput = reason,
                    parsedGeneralTags = parsed.generalTags,
                    parsedSpecificTags = parsed.specificTags
                )
            )

            /* mark as rejected in filter engine memory */
            filterEngine.reject(selectedId)

            /* next step */
            if (spinIdx >= 5) {                     // reached 6 spins
                _spinCount.value = spinIdx + 1
                commitSession()
            } else {
                _spinCount.value = spinIdx + 1
                loadRecommendedRestaurants(reason) // re-spin with same reason
            }
        }
    }

    private fun Restaurant.toAcceptedModel() = AcceptedRestaurant(
        id = id,
        name = name,
        generalTags = general_tags,
        specificTags = specific_tags,
        summary = summary,
        location = LocationInfo(location.lat, location.lng, location.address),
        rating = rating
    )

    private fun recordRound(
        roundIndex: Int,
        candidates: List<Restaurant>,
        selectedId: String?,
        feedback: UserFeedback?
    ) {
        interactionRounds += InteractionRound(
            roundIndex = roundIndex,
            selectedRestaurantId = selectedId,
            userFeedback = feedback,
            candidates = candidates.map {
                RestaurantCandidate(
                    id = it.id,
                    name = it.name,
                    tags = it.general_tags,
                    summary = it.summary,
                    rating = it.rating,
                    isSelected = it.id == selectedId
                )
            }
        )
    }

    /** Persist session & reset filter engine cache. */
    private fun commitSession() {
        viewModelScope.launch {
            val session = InteractionSession(
                ownerUuid = currentUserUuid,
                timestamp = System.currentTimeMillis(),
                acceptedRestaurant = acceptedRestaurant,
                rounds = interactionRounds.toList()
            )
            interactionSessionRepository.saveSession(currentUserUuid, session)
            filterEngine.reset()
            _sessionEnd.value = Unit
        }
    }

    fun getAcceptedRestaurant(): AcceptedRestaurant? = acceptedRestaurant
}
