package com.eric.guluturn.common.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * ApiKeyStorage handles storing, retrieving, and clearing
 * the current user's OpenAI API key and selected profile UUID
 * in local storage (SharedPreferences).
 */
object ApiKeyStorage {
    private const val PREFS_NAME = "guluturn_prefs"
    private const val KEY_API = "openai_api_key"
    private const val KEY_SELECTED_PROFILE_UUID = "selected_profile_uuid"

    /**
     * Save the API key to SharedPreferences.
     *
     * @param context Application context
     * @param key The validated OpenAI API key
     */
    fun saveApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_API, key).apply()
    }

    /**
     * Retrieve the stored API key.
     *
     * @param context Application context
     * @return The saved API key or null if not set
     */
    fun getSavedApiKey(context: Context): String? {
        return getPrefs(context).getString(KEY_API, null)
    }

    /**
     * Clear the stored API key.
     *
     * @param context Application context
     */
    fun clearApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_API).apply()
    }

    /**
     * Save the selected profile UUID to SharedPreferences.
     *
     * @param context Application context
     * @param uuid The UUID of the selected profile
     */
    fun saveSelectedProfileUuid(context: Context, uuid: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_PROFILE_UUID, uuid).apply()
    }

    /**
     * Retrieve the selected profile UUID from SharedPreferences.
     *
     * @param context Application context
     * @return The UUID of the selected profile or null if not set
     */
    fun getSelectedProfileUuid(context: Context): String? {
        return getPrefs(context).getString(KEY_SELECTED_PROFILE_UUID, null)
    }

    /**
     * Clear the selected profile UUID.
     *
     * @param context Application context
     */
    fun clearSelectedProfileUuid(context: Context) {
        getPrefs(context).edit().remove(KEY_SELECTED_PROFILE_UUID).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
