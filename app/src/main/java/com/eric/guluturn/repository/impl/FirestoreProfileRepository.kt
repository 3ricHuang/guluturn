package com.eric.guluturn.repository.impl

import android.content.Context
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.data.firestore.FirestoreHelper
import com.eric.guluturn.repository.iface.IProfileRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.eric.guluturn.common.constants.MAX_PROFILES_PER_API_KEY
import com.eric.guluturn.common.errors.MaxProfilesExceededException

/**
 * Firestore-based implementation of IProfileRepository using split schema.
 * Stores user profiles in /user_profiles and links via /api_user_links/{apiKey}/uuids/{uuid}.
 */
class FirestoreProfileRepository(
    private val context: Context
) : IProfileRepository {

    private val firestore = FirebaseFirestore.getInstance()
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
        val uuid = profile.uuid

        firestore.runTransaction { txn ->
            // Get document references
            val linkDocRef = FirestoreHelper.apiUserLinkDocument(apiKey)
            val userProfileRef = FirestoreHelper.userProfileDocument(uuid)
            val reverseLinkRef = FirestoreHelper.apiKeyToUserLink(apiKey, uuid)

            // Get current profile count (if doc exists)
            val linkDocSnap = txn.get(linkDocRef)
            val currentCount = linkDocSnap.getLong("profileCount")?.toInt() ?: 0

            if (currentCount >= MAX_PROFILES_PER_API_KEY) {
                throw MaxProfilesExceededException()
            }

            // 1. Write profile
            txn.set(userProfileRef, profile)

            // 2. Write reverse link
            txn.set(reverseLinkRef, mapOf("createdAt" to FieldValue.serverTimestamp()))

            // 3. Update count in root doc
            txn.set(linkDocRef, mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "profileCount" to currentCount + 1,
                "uuids.$uuid" to true
            ), SetOptions.merge())
        }.await()
    }

    override suspend fun deleteProfile(uuid: String) {
        val apiKey = getApiKeyOrThrow()

        firestore.runTransaction { txn ->
            val userProfileRef = FirestoreHelper.userProfileDocument(uuid)
            val reverseLinkRef = FirestoreHelper.apiKeyToUserLink(apiKey, uuid)
            val linkDocRef = FirestoreHelper.apiUserLinkDocument(apiKey)

            val linkDocSnap = txn.get(linkDocRef)
            val currentCount = linkDocSnap.getLong("profileCount")?.toInt() ?: 1
            val newCount = (currentCount - 1).coerceAtLeast(0)

            txn.delete(userProfileRef)
            txn.delete(reverseLinkRef)

            txn.set(linkDocRef, mapOf(
                "profileCount" to newCount,
                "uuids.$uuid" to FieldValue.delete()
            ), SetOptions.merge())
        }.await()
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
