package com.eric.guluturn.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.repository.iface.IProfileRepository
import com.eric.guluturn.ui.login.ApiKeyVerifier
import com.eric.guluturn.ui.login.ApiKeyVerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing user profile settings, including
 * loading current profile data, saving updated information, handling
 * API key changes, and logout logic.
 */
class ProfileSettingsViewModel(
    private val profileRepository: IProfileRepository,
    private val apiKeyVerifier: ApiKeyVerifier
) : ViewModel() {

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile

    private var originalApiKey: String? = null

    /**
     * Loads the current active user profile using the saved API key and UUID.
     * Ensures the correct profile is selected when entering settings screen.
     */
    fun loadCurrentProfile(context: Context) {
        val apiKey = ApiKeyStorage.getSavedApiKey(context) ?: return
        val selectedUuid = ApiKeyStorage.getSelectedProfileUuid(context) ?: return
        originalApiKey = apiKey

        viewModelScope.launch {
            val profiles = profileRepository.getAllProfiles(apiKey)
            _currentProfile.value = profiles.find { it.uuid == selectedUuid }
        }
    }

    /**
     * Saves profile updates and handles API key changes.
     *
     * If the API key has changed, this method:
     * 1. Verifies the new key if not already in Firestore
     * 2. Saves the profile under the new key
     * 3. Removes the old linkage (but keeps the profile document)
     * 4. Updates local storage and ViewModel state
     */
    fun saveProfileChanges(
        context: Context,
        newProfile: UserProfile,
        newApiKey: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val oldApiKey = originalApiKey ?: return@launch
            val oldUuid = _currentProfile.value?.uuid ?: return@launch

            // No API key change: update profile normally
            if (newApiKey == oldApiKey) {
                profileRepository.saveProfile(newProfile)
                ApiKeyStorage.saveSelectedProfileUuid(context, newProfile.uuid)
                _currentProfile.value = newProfile
                onResult(true, null)
                return@launch
            }

            // Core logic to transfer ownership
            val updatedProfile = newProfile.copy(currentApiKey = newApiKey)
            val performTransfer: suspend () -> Unit = {
                profileRepository.saveProfileWithKey(newApiKey, updatedProfile)
                profileRepository.removeProfileLinkFromKey(oldApiKey, oldUuid)

                val remaining = profileRepository.getAllProfiles(oldApiKey)
                if (remaining.isEmpty()) {
                    profileRepository.deleteApiKeyEntry(oldApiKey)
                }

                ApiKeyStorage.saveApiKey(context, newApiKey)
                ApiKeyStorage.saveSelectedProfileUuid(context, updatedProfile.uuid)
                _currentProfile.value = updatedProfile
                onResult(true, null)
            }

            // Branch based on new key presence
            val newProfiles = profileRepository.getAllProfiles(newApiKey)
            if (newProfiles.isNotEmpty()) {
                performTransfer()
                return@launch
            }

            // Validate new key if necessary
            when (val result = apiKeyVerifier.verify(newApiKey)) {
                is ApiKeyVerificationResult.Success -> performTransfer()
                is ApiKeyVerificationResult.Failure -> {
                    val message = when (result.reason) {
                        ApiKeyVerificationResult.Reason.UNAUTHORIZED -> "The API key is invalid. Please check and try again."
                        ApiKeyVerificationResult.Reason.NETWORK_ERROR -> "Network error. Please check your internet connection."
                        ApiKeyVerificationResult.Reason.UNKNOWN_ERROR -> "Unknown error occurred during verification."
                    }
                    onResult(false, message)
                }
            }
        }
    }

    /**
     * Clears the saved API key from local storage, effectively logging the user out.
     */
    fun logout(context: Context) {
        ApiKeyStorage.clearApiKey(context)
        ApiKeyStorage.clearSelectedProfileUuid(context)
    }

    /**
     * Deletes the current user profile from Firestore and removes the API key
     * linkage if it no longer has any associated profiles.
     *
     * @param context App context for API key handling
     * @param onComplete Callback for successful deletion
     * @param onError Callback for exception during deletion
     */
    fun deleteCurrentProfile(
        context: Context,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = _currentProfile.value
                val apiKey = ApiKeyStorage.getSavedApiKey(context)

                if (profile != null && apiKey != null) {
                    profileRepository.deleteProfile(profile.uuid)

                    val remainingProfiles = profileRepository.getAllProfiles(apiKey)
                    if (remainingProfiles.isEmpty()) {
                        profileRepository.deleteApiKeyEntry(apiKey)
                        ApiKeyStorage.clearApiKey(context)
                    }

                    onComplete()
                } else {
                    onError(IllegalStateException("No profile or API key available"))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
