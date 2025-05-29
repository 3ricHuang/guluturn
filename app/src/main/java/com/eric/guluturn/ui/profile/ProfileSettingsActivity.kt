package com.eric.guluturn.ui.profile

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.eric.guluturn.R
import com.eric.guluturn.common.enums.Gender
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.databinding.ActivityProfileSettingsBinding
import com.eric.guluturn.repository.impl.FirestoreProfileRepository
import com.eric.guluturn.ui.login.ApiKeyVerifier
import com.eric.guluturn.ui.login.LoginActivity
import com.eric.guluturn.ui.roulette.RouletteActivity
import kotlinx.coroutines.launch
import java.util.*

class ProfileSettingsActivity : ComponentActivity() {

    private lateinit var binding: ActivityProfileSettingsBinding
    private val viewModel: ProfileSettingsViewModel by viewModels {
        ProfileSettingsViewModelFactory(
            FirestoreProfileRepository(applicationContext),
            ApiKeyVerifier
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGenderDropdown()
        observeProfile()
        bindActions()

        viewModel.loadCurrentProfile(applicationContext)
    }

    private fun setupGenderDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            Gender.values().map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        )
        binding.genderDropdown.setAdapter(adapter)
    }

    private fun observeProfile() {
        lifecycleScope.launch {
            viewModel.currentProfile.collect { profile ->
                profile?.let { populateFields(it) }
            }
        }
    }

    private fun populateFields(profile: UserProfile) {
        binding.nameInput.setText(profile.name)
        binding.ageInput.setText(profile.age.toString())
        binding.genderDropdown.setText(profile.gender.name.lowercase().replaceFirstChar(Char::uppercase), false)
        binding.apiKeyInput.setText(profile.currentApiKey)
    }

    private fun bindActions() {
        binding.saveButton.setOnClickListener {
            val updated = extractInput()
            if (updated == null) {
                Toast.makeText(this, getString(R.string.profile_fill_warning), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val (profile, newApiKey) = updated
            viewModel.saveProfileChanges(applicationContext, profile, newApiKey) { success, errorMsg ->
                if (success) {
                    Toast.makeText(this, getString(R.string.profile_update_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, RouletteActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, errorMsg ?: getString(R.string.profile_update_failure), Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout(applicationContext)
            Toast.makeText(this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.deleteButton.setOnClickListener {
            showCustomDeleteDialog()
        }
    }

    private fun showCustomDeleteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_profile, null)
        val dialog = Dialog(this, R.style.DialogTheme)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        dialogView.findViewById<TextView>(R.id.deleteMessage)?.text =
            getString(R.string.delete_profile_confirmation)

        dialogView.findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.confirmButton)?.setOnClickListener {
            viewModel.deleteCurrentProfile(
                context = applicationContext,
                onComplete = {
                    dialog.dismiss()
                    Toast.makeText(this, getString(R.string.profile_deleted), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
                onError = {
                    dialog.dismiss()
                    Toast.makeText(this, getString(R.string.profile_delete_failed, it.message), Toast.LENGTH_LONG).show()
                }
            )
        }

        dialog.show()
    }

    private fun extractInput(): Pair<UserProfile, String>? {
        val name = binding.nameInput.text?.toString()?.trim()
        val age = binding.ageInput.text?.toString()?.toIntOrNull()
        val genderStr = binding.genderDropdown.text?.toString()?.uppercase()
        val apiKey = binding.apiKeyInput.text?.toString()?.trim()

        val uuid = viewModel.currentProfile.value?.uuid ?: UUID.randomUUID().toString()

        if (name.isNullOrBlank() || age == null || genderStr.isNullOrBlank() || apiKey.isNullOrBlank()) {
            return null
        }

        val gender = runCatching { Gender.valueOf(genderStr) }.getOrNull() ?: return null

        val profile = UserProfile(
            uuid = uuid,
            name = name,
            age = age,
            gender = gender,
            currentApiKey = apiKey
        )

        return profile to apiKey
    }
}
