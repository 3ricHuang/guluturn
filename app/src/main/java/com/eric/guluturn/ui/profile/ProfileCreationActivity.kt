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
import com.eric.guluturn.common.errors.MaxProfilesExceededException
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

        val apiKey = ApiKeyStorage.getSavedApiKey(applicationContext)
        if (apiKey == null) {
            Toast.makeText(this, R.string.error_missing_api_key, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.checkProfileLimit(apiKey)

        viewModel.isProfileLimitReached.observe(this) { isReached ->
            binding.createProfileButton.isEnabled = !isReached
            if (isReached) {
                Toast.makeText(
                    this,
                    "You’ve reached the profile limit. Please delete an existing profile before creating a new one.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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

            val newProfile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                currentApiKey = apiKey
            )

            lifecycleScope.launch {
                try {
                    viewModel.saveProfile(newProfile)
                    viewModel.selectProfile(newProfile)

                    Toast.makeText(
                        this@ProfileCreationActivity,
                        R.string.success_profile_created,
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@ProfileCreationActivity, com.eric.guluturn.ui.roulette.RouletteActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    when (e) {
                        is MaxProfilesExceededException -> {
                            Toast.makeText(
                                this@ProfileCreationActivity,
                                "You’ve reached the limit of 6 profiles for this API key. Please delete an existing one before creating a new profile.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
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
        }

        // Back button logic
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupGenderDropdown() {
        val genderOptions = Gender.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.genderDropdown.setAdapter(adapter)
    }
}
