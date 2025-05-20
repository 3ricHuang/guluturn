package com.eric.guluturn.common.models

import com.eric.guluturn.common.enums.Gender
import java.util.UUID

/**
 * Represents a user profile associated with a specific API key.
 *
 * This model is used for both local (SharedPreferences) and remote (Firestore) storage.
 */
data class UserProfile(
    val uuid: String = UUID.randomUUID().toString(),

    val name: String = "",

    val age: Int = 0,

    val gender: Gender = Gender.OTHER,

    var currentApiKey: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    var lastUsedAt: Long = System.currentTimeMillis()
)
