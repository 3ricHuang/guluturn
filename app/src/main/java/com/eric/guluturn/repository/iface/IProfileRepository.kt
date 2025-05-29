package com.eric.guluturn.repository.iface

import com.eric.guluturn.common.models.UserProfile

/**
 * Contract for accessing and managing user profiles.
 *
 * This interface is implemented by both local (e.g., SharedPreferences or Room)
 * and remote (e.g., Firebase Firestore) profile repositories.
 *
 * Profile data is modeled as [UserProfile] and typically includes UUID, name, age,
 * gender, and the associated API key.
 */
interface IProfileRepository {

    /**
     * Loads all user profiles linked to the currently active API key.
     *
     * For remote sources, this may involve looking up UUIDs associated with the key
     * and fetching full profile documents.
     *
     * @return A list of available profiles.
     */
    suspend fun getAllProfiles(apiKey: String): List<UserProfile>

    /**
     * Saves or updates a user profile to the underlying data source.
     * In Firestore, this involves storing the profile and linking it to the API key.
     *
     * @param profile The profile to persist.
     */
    suspend fun saveProfile(profile: UserProfile)

    /**
     * Deletes a user profile by UUID.
     * For Firestore, both the profile document and its key linkage are removed.
     *
     * @param uuid The profile UUID to delete.
     */
    suspend fun deleteProfile(uuid: String)

    /**
     * Sets the currently active profile in memory.
     * Does not persist this setting; useful for session-scoped logic.
     *
     * @param uuid The UUID of the profile to activate.
     */
    suspend fun setCurrentProfile(uuid: String)

    /**
     * Gets the currently active in-memory profile, if any.
     *
     * @return The current [UserProfile] or null if unset.
     */
    suspend fun getCurrentProfile(): UserProfile?

    /**
     * Saves a profile under the specified API key (not necessarily current user key).
     * Used for profile migration or reassignment.
     *
     * @param apiKey The API key to associate the profile with.
     * @param profile The profile to save.
     */
    suspend fun saveProfileWithKey(apiKey: String, profile: UserProfile)

    /**
     * Deletes the entire API key linkage entry if no profiles remain.
     *
     * @param apiKey The API key to remove from index.
     */
    suspend fun deleteApiKeyEntry(apiKey: String)

    suspend fun deleteApiKeyLink(apiKey: String)

    /**
     * Removes only the API key to profile UUID mapping, without deleting the profile document itself.
     * Used during API key transfer.
     */
    suspend fun removeProfileLinkFromKey(apiKey: String, uuid: String)

}
