package com.eric.guluturn.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.databinding.ActivityLoginBinding
import com.eric.guluturn.repository.impl.FirestoreProfileRepository
import com.eric.guluturn.ui.profile.ProfileSelectorActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels {
        val repo = FirestoreProfileRepository(applicationContext)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(profileRepository = repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val apiKey = binding.apiKeyEditText.text.toString().trim()

            if (apiKey.isBlank()) {
                Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.isApiKeyLinked(apiKey) { exists ->
                println("DEBUG: isApiKeyLinked returned = $exists")
                if (exists) {
                    println("DEBUG: Skipping verification, proceeding with login")
                    ApiKeyStorage.saveApiKey(applicationContext, apiKey)
                    println("DEBUG: Saved API key = ${ApiKeyStorage.getSavedApiKey(applicationContext)}")
                    startActivity(Intent(this, ProfileSelectorActivity::class.java))
                    finish()
                } else {
                    println("DEBUG: Not found in Firestore, verifying via OpenAI...")
                    viewModel.verifyApiKey(apiKey) { isValid ->
                        if (isValid) {
                            ApiKeyStorage.saveApiKey(applicationContext, apiKey)
                            println("DEBUG: Saved API key = ${ApiKeyStorage.getSavedApiKey(applicationContext)}")
                            startActivity(Intent(this, ProfileSelectorActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Invalid API key", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
