package com.eric.guluturn.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eric.guluturn.repository.iface.IProfileRepository
import com.eric.guluturn.repository.impl.FirestoreProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class LoginViewModel(
    private val profileRepository: IProfileRepository,
    private val client: OkHttpClient = OkHttpClient()
) : ViewModel() {

    /**
     * Retrieves all known API keys from Firestore.
     * This is used to skip verification for previously used keys.
     */
    fun getStoredApiKeys(callback: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val keys = (profileRepository as? FirestoreProfileRepository)
                ?.getAllLinkedApiKeys() ?: emptyList()
            withContext(Dispatchers.Main) {
                callback(keys)
            }
        }
    }

    fun isApiKeyLinked(apiKey: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = (profileRepository as? FirestoreProfileRepository)
                ?.apiKeyExists(apiKey) ?: false
            println("DEBUG: API key [$apiKey] exists in Firestore: $exists")
            withContext(Dispatchers.Main) {
                callback(exists)
            }
        }
    }

    fun verifyApiKey(apiKey: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback(false) }
                return@launch
            }

            withContext(Dispatchers.Main) {
                callback(response.isSuccessful)
            }
        }
    }
}
