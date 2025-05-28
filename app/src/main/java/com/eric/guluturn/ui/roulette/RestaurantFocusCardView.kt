package com.eric.guluturn.ui.roulette

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.eric.guluturn.R
import com.eric.guluturn.common.models.Restaurant

/**
 * A custom view that displays detailed information about a restaurant.
 * Used inside dialog_result.xml when a restaurant is selected.
 */
class RestaurantFocusCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.restaurant_focus_card_view, this, true)
    }

    /**
     * Binds restaurant data to the view elements.
     *
     * @param restaurant the restaurant object to render
     */
    fun bind(restaurant: Restaurant) {
        findViewById<TextView>(R.id.restaurantName).text = restaurant.name
        findViewById<TextView>(R.id.restaurantSummary).text = restaurant.summary ?: ""
        findViewById<TextView>(R.id.restaurantTags).text =
            restaurant.general_tags.joinToString(", ")

        findViewById<TextView>(R.id.restaurantRating).text = buildString {
            append("⭐ ${restaurant.rating ?: "N/A"}")
            if ((restaurant.review_count ?: 0) > 0) {
                append(" (${restaurant.review_count} reviews)")
            }
        }

        findViewById<TextView>(R.id.restaurantPriceRange).text =
            "Price: ${restaurant.price_range ?: "Unknown"}"

        findViewById<TextView>(R.id.restaurantAddress).text =
            "Address: ${restaurant.location?.address ?: "Unknown"}"

        findViewById<TextView>(R.id.restaurantBusinessDays).text =
            "Open days: ${formatOpenDays(restaurant)}"
    }

    private fun formatOpenDays(restaurant: Restaurant): String {
        val hours = restaurant.business_hours ?: return "Unknown"
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val keys = listOf(
            "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday"
        )

        return keys.zip(days).joinToString(", ") { (key, label) ->
            val dayInfo = hours[key]
            if (dayInfo?.open != null && dayInfo.close != null) "$label ✔" else "$label ✘"
        }
    }
}
