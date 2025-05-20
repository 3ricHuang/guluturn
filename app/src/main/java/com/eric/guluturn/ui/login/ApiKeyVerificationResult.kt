package com.eric.guluturn.ui.login

sealed class ApiKeyVerificationResult {
    object Success : ApiKeyVerificationResult()
    data class Failure(val reason: Reason) : ApiKeyVerificationResult()

    enum class Reason {
        UNAUTHORIZED,        // 401
        NETWORK_ERROR,       // IOException or timeout
        UNKNOWN_ERROR         // Any other status code or exception
    }
}
