package com.eric.guluturn.ui.profile

import androidx.lifecycle.*
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.repository.iface.IProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user profiles.
 * Supports loading, saving, and deleting profiles using an abstract repository interface.
 */
class ProfileViewModel(
    private val repository: IProfileRepository
) : ViewModel() {

    private val _profiles = MutableLiveData<List<UserProfile>>()
    val profiles: LiveData<List<UserProfile>> get() = _profiles

    private val _currentProfile = MutableLiveData<UserProfile?>()
    val currentProfile: LiveData<UserProfile?> get() = _currentProfile

    /**
     * Loads all profiles associated with the given API key.
     * This version performs a single fetch without retries.
     */
    fun loadProfiles(apiKey: String) = viewModelScope.launch {
        val result = repository.getAllProfiles(apiKey)
        _profiles.postValue(result)
    }

    /**
     * Loads all profiles associated with the given API key,
     * with a limited retry mechanism to tolerate eventual consistency delays in remote data stores.
     *
     * @param apiKey the API key used to identify the profile family
     * @param retries maximum number of fetch attempts (default: 3)
     * @param delayMillis interval in milliseconds between attempts (default: 300ms)
     */
    fun loadProfilesWithRetry(apiKey: String, retries: Int = 3, delayMillis: Long = 300L) {
        viewModelScope.launch {
            var lastResult: List<UserProfile> = emptyList()
            repeat(retries) { attempt ->
                val result = repository.getAllProfiles(apiKey)
                if (result.size > lastResult.size) {
                    _profiles.postValue(result)
                    return@launch
                }
                lastResult = result
                delay(delayMillis)
            }
            // If no improved result after retries, emit the last known result
            _profiles.postValue(lastResult)
        }
    }

    /**
     * Saves or updates a profile and reloads the profile list upon completion.
     */
    fun saveProfile(profile: UserProfile) = viewModelScope.launch {
        repository.saveProfile(profile)
        loadProfiles(profile.currentApiKey)
    }

    /**
     * Deletes a profile and reloads the profile list upon completion.
     */
    fun deleteProfile(profile: UserProfile) = viewModelScope.launch {
        repository.deleteProfile(profile.uuid)
        loadProfiles(profile.currentApiKey)
    }

    /**
     * Sets the currently active profile in memory.
     * Also persists the selection in the underlying repository if applicable.
     */
    fun selectProfile(profile: UserProfile) = viewModelScope.launch {
        repository.setCurrentProfile(profile.uuid)
        _currentProfile.postValue(profile)
    }

    /**
     * Loads the previously selected profile, if any exists in the repository.
     */
    fun loadCurrentProfile() = viewModelScope.launch {
        val current = repository.getCurrentProfile()
        _currentProfile.postValue(current)
    }
}
