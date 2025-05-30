package com.eric.guluturn.repository.impl

import InteractionSession
import com.eric.guluturn.repository.iface.IInteractionSessionRepository
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreInteractionSessionRepository(
    private val firestore: FirebaseFirestore
) : IInteractionSessionRepository {

    override suspend fun saveSession(userUuid: String, session: InteractionSession) {
        firestore.collection("users")
            .document(userUuid)
            .collection("interaction_sessions")
            .document()
            .set(session)
    }
}
