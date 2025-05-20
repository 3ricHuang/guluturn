package com.eric.guluturn.repository.impl

import android.content.Context
import android.content.SharedPreferences
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.repository.iface.IProfileRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Local implementation of [IProfileRepository] using SharedPreferences.
 *
 * Profiles are stored as a serialized JSON list. The current active profile UUID
 * is stored separately to support session-based behavior.
 */
class LocalProfileRepository(private val context: Context) : IProfileRepository {

    companion object {
        private const val PREFS_NAME = "guluturn_profiles"
        private const val KEY_PROFILES = "profile_list"
        private const val KEY_CURRENT_UUID = "current_profile_uuid"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Loads all saved user profiles.
     * API key is ignored in local mode, but accepted to conform to interface.
     */
    override suspend fun getAllProfiles(apiKey: String): List<UserProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveProfile(profile: UserProfile) {
        val profiles = getAllProfiles(profile.currentApiKey).toMutableList()
        val index = profiles.indexOfFirst { it.uuid == profile.uuid }

        if (index >= 0) {
            profiles[index] = profile
        } else {
            profiles.add(profile)
        }

        val updatedJson = json.encodeToString(profiles)
        prefs.edit().putString(KEY_PROFILES, updatedJson).apply()
    }

    override suspend fun deleteProfile(uuid: String) {
        val updated = getAllProfiles("").filterNot { it.uuid == uuid }
        prefs.edit().putString(KEY_PROFILES, json.encodeToString(updated)).apply()

        if (getCurrentProfile()?.uuid == uuid) {
            prefs.edit().remove(KEY_CURRENT_UUID).apply()
        }
    }

    override suspend fun getCurrentProfile(): UserProfile? {
        val uuid = prefs.getString(KEY_CURRENT_UUID, null) ?: return null
        return getAllProfiles("").find { it.uuid == uuid }
    }

    override suspend fun setCurrentProfile(uuid: String) {
        prefs.edit().putString(KEY_CURRENT_UUID, uuid).apply()
    }
}
