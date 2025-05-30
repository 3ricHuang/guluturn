import com.eric.guluturn.common.models.SpecificTag

data class InteractionSession(
    val ownerUuid: String,
    val timestamp: Long = System.currentTimeMillis(),
    val acceptedRestaurant: AcceptedRestaurant? = null,
    val rounds: List<InteractionRound> = emptyList()
)

data class AcceptedRestaurant(
    val id: String,
    val name: String,
    val generalTags: List<String>,
    val specificTags: List<SpecificTag>,
    val summary: String,
    val location: LocationInfo,
    val rating: Double?
)

data class InteractionRound(
    val roundIndex: Int,
    val candidates: List<RestaurantCandidate>,
    val selectedRestaurantId: String?,
    val userFeedback: UserFeedback? = null
)

data class RestaurantCandidate(
    val id: String,
    val name: String,
    val tags: List<String>,
    val isSelected: Boolean = false,
    val summary: String? = null,
    val rating: Double? = null
)


data class UserFeedback(
    val accepted: Boolean,
    val reasonInput: String?,
    val parsedGeneralTags: List<String> = emptyList(),
    val parsedSpecificTags: List<SpecificTag> = emptyList()
)

data class LocationInfo(
    val lat: Double,
    val lng: Double,
    val address: String
)
