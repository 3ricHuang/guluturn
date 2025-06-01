package com.eric.guluturn.ui.roulette

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.databinding.ActivityRouletteBinding
import com.eric.guluturn.common.storage.ApiKeyStorage
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.impl.FirestoreInteractionSessionRepository
import com.eric.guluturn.repository.impl.FirestoreRestaurantRepository
import com.eric.guluturn.semantic.impl.OpenAiTagGenerator
import com.eric.guluturn.semantic.models.OpenAiModels
import com.eric.guluturn.ui.profile.ProfileSelectorActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RouletteActivity : ComponentActivity() {

    private lateinit var binding: ActivityRouletteBinding
    private lateinit var viewModel: RouletteViewModel
    private lateinit var adapter: RestaurantCardAdapter
    private lateinit var dialogManager: RouletteDialogManager
    private lateinit var fullPool: List<Restaurant>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profileUuid = ApiKeyStorage.getSelectedProfileUuid(applicationContext)
        if (profileUuid == null) {
            startActivity(Intent(this, ProfileSelectorActivity::class.java))
            finish(); return
        }

        binding = ActivityRouletteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnMenu.setOnClickListener {
            startActivity(
                Intent(this, com.eric.guluturn.ui.profile.ProfileSettingsActivity::class.java)
            )
        }

        adapter = RestaurantCardAdapter()
        binding.objectArea.layoutManager = GridLayoutManager(this, 2)
        binding.objectArea.adapter = adapter

        lifecycleScope.launch {
            fullPool = FirestoreRestaurantRepository().getAllRestaurants()

            val apiKey = ApiKeyStorage.getSavedApiKey(applicationContext)
                ?: throw IllegalStateException("API key not found")
            val semanticEngine = OpenAiTagGenerator(OpenAiModels(apiKey))
            val filterEngine = StatefulFilterEngineImpl(
                apiKey = apiKey,
                semantic = semanticEngine
            )

            val firestore = FirebaseFirestore.getInstance()
            val interactionRepo = FirestoreInteractionSessionRepository(firestore)

            viewModel = ViewModelProvider(
                this@RouletteActivity,
                RouletteViewModelFactory(filterEngine, semanticEngine, interactionRepo, profileUuid)
            )[RouletteViewModel::class.java]

            viewModel.initialize(fullPool)

            dialogManager = RouletteDialogManager(
                context = this@RouletteActivity,
                parent = binding.root,
                onRejectConfirmed = { reason -> viewModel.reject(reason) },
                onAcceptConfirmed = { viewModel.acceptCurrent() },
                onPlayAgain = {
                    viewModel.initialize(fullPool)
                }
            )

            viewModel.restaurants.observe(this@RouletteActivity) { list ->
                adapter.submitList(list)
                binding.spinWheelView.setItems(list)
            }
            viewModel.spinCount.observe(this@RouletteActivity) { count ->
                binding.decisionSectionView.updateSpinCount(count)
            }
            viewModel.sessionEnd.observe(this@RouletteActivity) {
                dialogManager.showSessionEndDialog(viewModel.getAcceptedRestaurant())
            }

            binding.spinButton.setOnClickListener { binding.spinWheelView.spin() }
            binding.spinWheelView.onSpinEnd = { r ->
                viewModel.setSelectedRestaurant(r)
                dialogManager.showResultDialog(r)
            }
        }
    }
}
