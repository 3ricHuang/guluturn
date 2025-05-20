package com.eric.guluturn.data.firestore

/**
 * Defines Firestore collection and field names for schema consistency.
 * Used to centralize Firestore paths and reduce hardcoded strings.
 */
object FirestoreSchema {

    object Collections {
        const val API_USER_LINKS = "api_user_links"     // Maps apiKey → UUIDs
        const val USER_PROFILES = "user_profiles"       // Stores actual profile info
    }

    object Fields {
        // user_profiles/{uuid}
        const val UUID = "uuid"
        const val NAME = "name"
        const val AGE = "age"
        const val GENDER = "gender"
        const val CURRENT_API_KEY = "currentApiKey"

        // api_user_links/{apiKey}/{uuid: true}
        const val LINK_VALUE = "linked"  // optional flag if needed, usually set to true
    }
}
