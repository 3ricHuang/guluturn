package com.eric.guluturn.repository.iface

import InteractionSession

interface IInteractionSessionRepository {
    suspend fun saveSession(userUuid: String, session: InteractionSession)
}
