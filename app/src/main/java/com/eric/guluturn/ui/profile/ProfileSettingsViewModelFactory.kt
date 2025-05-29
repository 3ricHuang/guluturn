package com.eric.guluturn.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eric.guluturn.repository.iface.IProfileRepository
import com.eric.guluturn.ui.login.ApiKeyVerifier

class ProfileSettingsViewModelFactory(
    private val repository: IProfileRepository,
    private val verifier: ApiKeyVerifier
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileSettingsViewModel::class.java)) {
            return ProfileSettingsViewModel(repository, verifier) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
