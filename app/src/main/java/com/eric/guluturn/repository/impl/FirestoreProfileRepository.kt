package com.eric.guluturn.repository.impl

import android.content.Context
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.data.firestore.FirestoreHelper
import com.eric.guluturn.repository.iface.IProfileRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Firestore-based implementation of IProfileRepository using split schema.
 * Stores user profiles in /user_profiles and links via /api_user_links/{apiKey}/uuids/{uuid}.
 */
class FirestoreProfileRepository(
    private val context: Context
) : IProfileRepository {

    private var currentProfile: UserProfile? = null

    override suspend fun getAllProfiles(apiKey: String): List<UserProfile> {
        return try {
            val uuidSnapshots = FirestoreHelper.linkedUuids(apiKey).get(Source.SERVER).await()
            val uuidList = uuidSnapshots.documents.mapNotNull { it.id }

            uuidList.mapNotNull { uuid ->
                val doc = FirestoreHelper.userProfileDocument(uuid).get(Source.SERVER).await()
                doc.toObject(UserProfile::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveProfile(profile: UserProfile) {
        val apiKey = getApiKeyOrThrow()
        println("DEBUG: saveProfile() invoked with apiKey = $apiKey, uuid = ${profile.uuid}")

        try {
            // Attempt to write the API key root document (metadata, optional)
            try {
                FirestoreHelper.apiUserLinkDocument(apiKey)
                    .set(
                        mapOf("createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()
                println("DEBUG: apiUserLinkDocument write successful")
            } catch (e: Exception) {
                println("WARNING: apiUserLinkDocument write failed: ${e.message}")
            }

            // Main user profile write (required for system correctness)
            try {
                FirestoreHelper.userProfileDocument(profile.uuid)
                    .set(profile)
                    .await()
                println("DEBUG: userProfileDocument write successful")
            } catch (e: Exception) {
                println("ERROR: Failed to write userProfile document. Aborting saveProfile.")
                throw e
            }

            // Create reverse link for profile lookup under the API key (optional, recoverable)
            try {
                FirestoreHelper.apiKeyToUserLink(apiKey, profile.uuid)
                    .set(mapOf("linked" to true))
                    .await()
                println("DEBUG: apiKeyToUserLink write successful")
            } catch (e: Exception) {
                println("WARNING: apiKeyToUserLink write failed (non-blocking): ${e.message}")
            }

        } catch (e: Exception) {
            println("ERROR: saveProfile terminated due to exception: ${e.message}")
            throw e
        }
    }



    override suspend fun deleteProfile(uuid: String) {
        val apiKey = getApiKeyOrThrow()

        FirestoreHelper.userProfileDocument(uuid).delete().await()
        FirestoreHelper.apiKeyToUserLink(apiKey, uuid).delete().await()
    }

    override suspend fun getCurrentProfile(): UserProfile? = currentProfile

    override suspend fun setCurrentProfile(uuid: String) {
        val doc = FirestoreHelper.userProfileDocument(uuid).get().await()
        currentProfile = doc.toObject(UserProfile::class.java)
    }

    private fun getApiKeyOrThrow(): String {
        val shared = context.getSharedPreferences("guluturn_prefs", Context.MODE_PRIVATE)
        val key = shared.getString("openai_api_key", null)
        println("DEBUG: getApiKeyOrThrow() context = ${context::class.java.name}, apiKey = $key")
        require(!key.isNullOrBlank()) { "API key not set in SharedPreferences" }
        return key
    }

    suspend fun apiKeyExists(apiKey: String): Boolean {
        return try {
            val doc = FirestoreHelper.apiUserLinkDocument(apiKey).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns a list of all API keys that have any linked profile (used for login quick-check).
     */
    suspend fun getAllLinkedApiKeys(): List<String> {
        return try {
            val snapshot = FirestoreHelper.apiUserLinks().get().await()
            snapshot.documents.mapNotNull { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
