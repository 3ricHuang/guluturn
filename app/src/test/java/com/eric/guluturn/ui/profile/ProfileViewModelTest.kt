package com.eric.guluturn.ui.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.eric.guluturn.common.enums.Gender
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.repository.iface.IProfileRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

class ProfileViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var repository: IProfileRepository
    private lateinit var viewModel: ProfileViewModel

    private val mockProfiles = listOf(
        UserProfile(name = "Eric", age = 20, gender = Gender.MALE, currentApiKey = "key123"),
        UserProfile(name = "Anna", age = 22, gender = Gender.FEMALE, currentApiKey = "key123"),
        UserProfile(name = "Tom", age = 25, gender = Gender.OTHER, currentApiKey = "key456")
    )

    @Before
    fun setUp() {
        repository = mock()
        viewModel = ProfileViewModel(repository)
    }

    @Test
    fun `loadProfiles filters by apiKey and updates LiveData`() {
        whenever(repository.getAllProfiles()).thenReturn(mockProfiles)

        val observer = mock<Observer<List<UserProfile>>>()
        viewModel.profiles.observeForever(observer)

        viewModel.loadProfiles("key123")

        verify(observer).onChanged(check {
            assertEquals(2, it.size)
            assertTrue(it.all { p -> p.currentApiKey == "key123" })
        })

        viewModel.profiles.removeObserver(observer)
    }

    @Test
    fun `saveProfile triggers repository and reloads profiles`() {
        val profile = mockProfiles[0]
        whenever(repository.getAllProfiles()).thenReturn(listOf(profile))

        viewModel.saveProfile(profile)

        verify(repository).saveProfile(profile)
        assertEquals(1, viewModel.profiles.value?.size)
    }

    @Test
    fun `deleteProfile triggers repository and reloads profiles`() {
        val profile = mockProfiles[1]
        whenever(repository.getAllProfiles()).thenReturn(listOf(profile))

        viewModel.deleteProfile(profile)

        verify(repository).deleteProfile(profile.uuid)
        assertEquals(1, viewModel.profiles.value?.size)
    }

    @Test
    fun `selectProfile sets currentProfile and saves to repository`() {
        val profile = mockProfiles[2]

        val observer = mock<Observer<UserProfile?>>()
        viewModel.currentProfile.observeForever(observer)

        viewModel.selectProfile(profile)

        verify(repository).setCurrentProfile(profile.uuid)
        verify(observer).onChanged(profile)

        viewModel.currentProfile.removeObserver(observer)
    }

    @Test
    fun `loadCurrentProfile loads existing current profile`() {
        val profile = mockProfiles[0]
        whenever(repository.getCurrentProfile()).thenReturn(profile)

        val observer = mock<Observer<UserProfile?>>()
        viewModel.currentProfile.observeForever(observer)

        viewModel.loadCurrentProfile()

        verify(observer).onChanged(profile)

        viewModel.currentProfile.removeObserver(observer)
    }
}
