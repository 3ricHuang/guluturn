package com.eric.guluturn.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.eric.guluturn.R
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.databinding.ActivityProfileSelectorBinding
import com.eric.guluturn.repository.impl.FirestoreProfileRepository
import com.eric.guluturn.ui.common.GridSpacingItemDecoration
import com.eric.guluturn.ui.roulette.RouletteActivity
import kotlinx.coroutines.launch

class ProfileSelectorActivity : ComponentActivity() {

    private lateinit var binding: ActivityProfileSelectorBinding
    private lateinit var adapter: ProfileListAdapter
    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiKey = ApiKeyStorage.getSavedApiKey(applicationContext)
        if (apiKey.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.error_missing_api_key), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val repo = FirestoreProfileRepository(applicationContext)
        viewModel = ProfileViewModel(repo)

        setupRecyclerView()
        setupFabButton()

        viewModel.profiles.observe(this) { profiles ->
            println("DEBUG: observed profiles = ${profiles.map { it.uuid }}")
            adapter.submitList(profiles)
            binding.emptyStateText.isVisible = profiles.isEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        val apiKey = ApiKeyStorage.getSavedApiKey(applicationContext)
        if (!apiKey.isNullOrBlank()) {
            viewModel.loadProfilesWithRetry(apiKey)
        }
    }

    private fun setupRecyclerView() {
        adapter = ProfileListAdapter { profile ->
            lifecycleScope.launch {
                viewModel.selectProfile(profile)
                ApiKeyStorage.saveSelectedProfileUuid(applicationContext, profile.uuid)
                Toast.makeText(this@ProfileSelectorActivity, "Selected: ${profile.name}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@ProfileSelectorActivity, RouletteActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.profile_grid_spacing)
        binding.recyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacingInPixels))
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFabButton() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, ProfileCreationActivity::class.java))
        }
    }
}
