package com.eric.guluturn.ui.roulette

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.eric.guluturn.databinding.ActivityRouletteBinding
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.impl.FirestoreRestaurantRepository
import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.ui.profile.ProfileSelectorActivity
import kotlinx.coroutines.launch

class RouletteActivity : ComponentActivity() {

    private lateinit var binding: ActivityRouletteBinding
    private lateinit var viewModel: RouletteViewModel
    private lateinit var adapter: RestaurantCardAdapter
    private lateinit var dialogManager: RouletteDialogManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedProfileUuid = ApiKeyStorage.getSelectedProfileUuid(applicationContext)
        if (selectedProfileUuid == null) {
            startActivity(Intent(this, ProfileSelectorActivity::class.java))
            finish()
            return
        }

        binding = ActivityRouletteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMenu.setOnClickListener {
            val intent = Intent(this, com.eric.guluturn.ui.profile.ProfileSettingsActivity::class.java)
            startActivity(intent)
        }

        adapter = RestaurantCardAdapter()
        binding.objectArea.layoutManager = GridLayoutManager(this, 2)
        binding.objectArea.adapter = adapter

        dialogManager = RouletteDialogManager(
            context = this,
            parent = binding.root,
            onRejectConfirmed = { reason ->
                viewModel.reject(reason)
            },
            onAcceptConfirmed = {
                viewModel.acceptCurrent()
            }
        )

        lifecycleScope.launch {
            val fullPool = FirestoreRestaurantRepository().getAllRestaurants()
            val filterEngine = StatefulFilterEngineImpl()
            val apiKey = ApiKeyStorage.getSavedApiKey(applicationContext)
                ?: throw IllegalStateException("API key not found")
            val semanticEngine = OpenAiTagGenerator(OpenAiModels(apiKey))

            viewModel = ViewModelProvider(
                this@RouletteActivity,
                RouletteViewModelFactory(filterEngine, semanticEngine)
            )[RouletteViewModel::class.java]

            viewModel.initialize(fullPool)

            viewModel.restaurants.observe(this@RouletteActivity) { list ->
                println("DEBUG: Activity observed ${list.size} restaurants")
                adapter.submitList(list)
                binding.spinWheelView.setItems(list)
            }

            viewModel.spinCount.observe(this@RouletteActivity) { count ->
                binding.decisionSectionView.updateSpinCount(count)
            }

            binding.spinButton.setOnClickListener {
                binding.spinWheelView.spin()
            }

            binding.spinWheelView.onSpinEnd = { selectedRestaurant ->
                dialogManager.showResultDialog(selectedRestaurant)
                viewModel.incrementSpinCount()
            }
        }
    }
}
