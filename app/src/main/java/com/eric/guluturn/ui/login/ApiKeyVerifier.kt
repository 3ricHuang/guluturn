package com.eric.guluturn.ui.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Verifies an OpenAI API key by performing a simple GET request to the models endpoint.
 */
object ApiKeyVerifier {

    private const val VERIFY_URL = "https://api.openai.com/v1/models"
    private const val MAX_RETRIES = 2

    private val client: OkHttpClient = OkHttpClient()

    suspend fun verify(apiKey: String): ApiKeyVerificationResult = withContext(Dispatchers.IO) {
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val request = Request.Builder()
                    .url(VERIFY_URL)
                    .header("Authorization", "Bearer $apiKey")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                return@withContext when (response.code) {
                    200 -> ApiKeyVerificationResult.Success
                    401 -> ApiKeyVerificationResult.Failure(ApiKeyVerificationResult.Reason.UNAUTHORIZED)
                    else -> ApiKeyVerificationResult.Failure(ApiKeyVerificationResult.Reason.UNKNOWN_ERROR)
                }
            } catch (e: IOException) {
                if (attempt == MAX_RETRIES) {
                    return@withContext ApiKeyVerificationResult.Failure(ApiKeyVerificationResult.Reason.NETWORK_ERROR)
                }
                // otherwise, retry
            }
        }
        return@withContext ApiKeyVerificationResult.Failure(ApiKeyVerificationResult.Reason.UNKNOWN_ERROR)
    }
}
