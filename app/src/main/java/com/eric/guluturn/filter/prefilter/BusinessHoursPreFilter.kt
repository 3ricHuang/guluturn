package com.eric.guluturn.filter.prefilter

import com.eric.guluturn.common.models.BusinessHour
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.semantic.iface.ParsedUserInput
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Filters out restaurants that should be excluded because
 * the user explicitly mentioned the store is closed *and*
 * the store is not currently within its opening hours.
 *
 * Behaviour change (2025-05-31):
 *   • The open/close check is executed **only** when the user's
 *     generalTags contains "user_report_closed".
 *   • Otherwise the candidate list is returned untouched.
 *
 * Works down to minSdk 24 by using java.util.Calendar.
 */
object BusinessHoursPreFilter {

    /** Calendar.DAY_OF_WEEK (1..7) → key inside JSON business_hours */
    private val DOW_KEYS = mapOf(
        Calendar.MONDAY    to "monday",
        Calendar.TUESDAY   to "tuesday",
        Calendar.WEDNESDAY to "wednesday",
        Calendar.THURSDAY  to "thursday",
        Calendar.FRIDAY    to "friday",
        Calendar.SATURDAY  to "saturday",
        Calendar.SUNDAY    to "sunday"
    )

    /**
     * @param semantic Parsed user request (tags + preferred names)
     * @param candidates Full candidate list
     * @param tz TimeZone, default device time-zone
     * @return Filtered list – unchanged when user did not say “closed”.
     */
    fun filter(
        semantic: ParsedUserInput,
        candidates: List<Restaurant>,
        tz: TimeZone = TimeZone.getDefault()
    ): List<Restaurant> {

        /* Execute only if user explicitly said “closed”. */
        if ("user_report_closed" !in semantic.generalTags) return candidates

        /* --- current time in minutes --- */
        val cal = Calendar.getInstance(tz, Locale.getDefault())
        val todayKey = DOW_KEYS[cal.get(Calendar.DAY_OF_WEEK)] ?: "monday"
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        fun isOpen(bh: BusinessHour?): Boolean {
            if (bh == null || bh.open == null || bh.close == null) return false
            val start = bh.open.toMinutes()
            val end   = bh.close.toMinutes()
            return if (start == end) true else start <= nowMin && nowMin <= end
        }

        val preferredNames = semantic.preferredRestaurants

        return candidates.filter { shop ->
            val matchesNamed = preferredNames.any { shop.name.contains(it) }
            val openNow      = isOpen(shop.business_hours[todayKey])
            /* keep shop only when:  (not named as closed) OR (still open now)  */
            !(matchesNamed && !openNow)
        }
    }

    /** "HH:mm" -> minutes since 00:00  */
    private fun String.toMinutes(): Int = try {
        val p = split(':'); p[0].toInt() * 60 + p[1].toInt()
    } catch (_: Exception) { -1 }
}
