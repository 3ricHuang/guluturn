package com.eric.guluturn.ui.roulette

import AcceptedRestaurant
import InteractionRound
import InteractionSession
import LocationInfo
import RestaurantCandidate
import UserFeedback
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.iface.IInteractionSessionRepository
import com.eric.guluturn.semantic.iface.ISemanticEngine
import kotlinx.coroutines.launch

class RouletteViewModel(
    private val filterEngine: StatefulFilterEngineImpl,
    private val semanticEngine: ISemanticEngine,
    private val interactionSessionRepository: IInteractionSessionRepository,
    private val currentUserUuid: String
) : ViewModel() {

    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> get() = _restaurants

    private var fullRestaurantPool: List<Restaurant> = emptyList()
    private val _spinCount = MutableLiveData(0)
    val spinCount: LiveData<Int> get() = _spinCount

    private val _sessionEndEvent = MutableLiveData<Unit>()
    val sessionEndEvent: LiveData<Unit> get() = _sessionEndEvent

    private val interactionRounds = mutableListOf<InteractionRound>()
    private var acceptedRestaurant: AcceptedRestaurant? = null

    private var selectedRestaurant: Restaurant? = null

    fun setSelectedRestaurant(restaurant: Restaurant) {
        selectedRestaurant = restaurant
    }

    fun initialize(restaurants: List<Restaurant>) {
        fullRestaurantPool = restaurants
        _restaurants.value = restaurants.shuffled().take(6)
        _spinCount.value = 0
        interactionRounds.clear()
        acceptedRestaurant = null
        selectedRestaurant = null
    }

    fun loadRecommendedRestaurants(reason: String = "") {
        viewModelScope.launch {
            val result = if (reason.isBlank()) {
                fullRestaurantPool.shuffled().take(6)
            } else {
                val parsed = semanticEngine.parseInput(reason)
                filterEngine.updateAndFilter(
                    userGeneralTags = parsed.generalTags,
                    userSpecificTags = parsed.specificTags,
                    allRestaurants = fullRestaurantPool
                ).take(6)
            }
            _restaurants.value = result
        }
    }

    fun rejectRestaurant(restaurant: Restaurant, reason: String) {
        viewModelScope.launch {
            val parsed = semanticEngine.parseInput(reason)
            val selectedId = restaurant.id

            val feedback = UserFeedback(
                accepted = false,
                reasonInput = reason,
                parsedGeneralTags = parsed.generalTags,
                parsedSpecificTags = parsed.specificTags
            )

            val currentSpin = _spinCount.value ?: 0

            recordRound(
                roundIndex = currentSpin,
                candidates = _restaurants.value ?: emptyList(),
                selectedId = selectedId,
                feedback = feedback
            )

            filterEngine.reject(restaurant.id)

            if (currentSpin >= 5) {
                _spinCount.value = currentSpin + 1
                commitSession()
            } else {
                _spinCount.value = currentSpin + 1
                loadRecommendedRestaurants(reason)
            }
        }
    }

    fun reject(reason: String) {
        val current = selectedRestaurant ?: return
        rejectRestaurant(current, reason)
    }

    fun acceptCurrent() {
        val current = selectedRestaurant ?: return
        val currentSpin = _spinCount.value ?: 0

        acceptedRestaurant = AcceptedRestaurant(
            id = current.id,
            name = current.name,
            generalTags = current.general_tags,
            specificTags = current.specific_tags,
            summary = current.summary,
            location = LocationInfo(
                lat = current.location.lat,
                lng = current.location.lng,
                address = current.location.address
            ),
            rating = current.rating
        )

        recordRound(
            roundIndex = currentSpin,
            candidates = _restaurants.value ?: emptyList(),
            selectedId = current.id,
            feedback = UserFeedback(
                accepted = true,
                reasonInput = null,
                parsedGeneralTags = emptyList(),
                parsedSpecificTags = emptyList()
            )
        )

        _spinCount.value = currentSpin + 1
        commitSession()
    }

    fun recordRound(
        roundIndex: Int,
        candidates: List<Restaurant>,
        selectedId: String?,
        feedback: UserFeedback? = null
    ) {
        val round = InteractionRound(
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
        interactionRounds.add(round)
    }

    fun commitSession() {
        viewModelScope.launch {
            val session = InteractionSession(
                ownerUuid = currentUserUuid,
                timestamp = System.currentTimeMillis(),
                acceptedRestaurant = acceptedRestaurant,
                rounds = interactionRounds.toList()
            )
            interactionSessionRepository.saveSession(currentUserUuid, session)
            filterEngine.reset()
            _sessionEndEvent.value = Unit
        }
        println("DEBUG: Writing session to Firestore for user $currentUserUuid")
    }

    fun getAcceptedRestaurant(): AcceptedRestaurant? {
        return acceptedRestaurant
    }
}
