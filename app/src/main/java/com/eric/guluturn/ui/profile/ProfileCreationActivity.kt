package com.eric.guluturn.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.eric.guluturn.R
import com.eric.guluturn.common.enums.Gender
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.databinding.ActivityProfileCreationBinding
import com.eric.guluturn.repository.impl.FirestoreProfileRepository
import kotlinx.coroutines.launch

/**
 * Activity for creating a new user profile.
 * Collects name, age, gender and saves to local storage.
 */
class ProfileCreationActivity : ComponentActivity() {

    private lateinit var binding: ActivityProfileCreationBinding

    private val viewModel: ProfileViewModel by viewModels {
        val repo = FirestoreProfileRepository(applicationContext)
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGenderDropdown()

        binding.createProfileButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val ageText = binding.ageInput.text.toString().trim()
            val genderString = binding.genderDropdown.text.toString()

            if (name.isEmpty() || ageText.isEmpty() || genderString.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull()
            val gender = Gender.values().find { it.name.equals(genderString, ignoreCase = true) }

            if (age == null || age !in 1..120 || gender == null) {
                Toast.makeText(this, R.string.error_invalid_data, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val apiKey = ApiKeyStorage.getSavedApiKey(applicationContext)
            println("DEBUG: Read API key in ProfileCreationActivity = $apiKey")
            if (apiKey == null) {
                Toast.makeText(this, R.string.error_missing_api_key, Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val newProfile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                currentApiKey = apiKey
            )

            lifecycleScope.launch {
                try {
                    // Save the newly created user profile to the remote repository (Firestore)
                    viewModel.saveProfile(newProfile)

                    // Mark this profile as the currently selected one for downstream usage
                    viewModel.selectProfile(newProfile)

                    Toast.makeText(
                        this@ProfileCreationActivity,
                        R.string.success_profile_created,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reserve transition to the main recommendation screen after profile creation
                    // TODO: Replace GuluRecommendationActivity with your actual recommendation activity
                    startActivity(intent)

                    // Finish this activity to remove it from the back stack
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ProfileCreationActivity,
                        "Failed to create profile: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupGenderDropdown() {
        val genderOptions = Gender.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.genderDropdown.setAdapter(adapter)
    }
}
