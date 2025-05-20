package com.eric.guluturn.data.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Provides centralized access to Firestore collections and documents
 * using the split schema: api_user_links and user_profiles.
 */
object FirestoreHelper {

    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- Collection references ---

    /** Root: /api_user_links */
    fun apiUserLinks(): CollectionReference = db().collection(FirestoreSchema.Collections.API_USER_LINKS)

    /** Root: /user_profiles */
    fun userProfiles(): CollectionReference = db().collection(FirestoreSchema.Collections.USER_PROFILES)

    // --- Document references ---

    /**
     * Returns the document reference for the mapping:
     * /api_user_links/{apiKey}/{uuid}
     */
    fun apiKeyToUserLink(apiKey: String, uuid: String): DocumentReference =
        apiUserLinks().document(apiKey).collection("uuids").document(uuid)

    /**
     * Returns the user profile document:
     * /user_profiles/{uuid}
     */
    fun userProfileDocument(uuid: String): DocumentReference =
        userProfiles().document(uuid)

    /**
     * Returns the subcollection of UUIDs linked to an API key:
     * /api_user_links/{apiKey}/uuids
     */
    fun linkedUuids(apiKey: String): CollectionReference =
        apiUserLinks().document(apiKey).collection("uuids")

    fun apiUserLinkDocument(apiKey: String): DocumentReference {
        return FirebaseFirestore.getInstance()
            .collection("api_user_links")
            .document(apiKey)
    }

}
