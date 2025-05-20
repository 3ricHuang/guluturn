package com.eric.guluturn.common.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * ApiKeyStorage handles storing, retrieving, and clearing
 * the current user's OpenAI API key in local storage (SharedPreferences).
 */
object ApiKeyStorage {
    private const val PREFS_NAME = "guluturn_prefs"
    private const val KEY_API = "openai_api_key"

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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
