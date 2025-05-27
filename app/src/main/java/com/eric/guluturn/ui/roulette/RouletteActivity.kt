package com.eric.guluturn.ui.roulette

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.eric.guluturn.databinding.ActivityRouletteBinding
import com.eric.guluturn.filter.impl.StatefulFilterEngineImpl
import com.eric.guluturn.repository.impl.FirestoreRestaurantRepository
import kotlinx.coroutines.launch

class RouletteActivity : ComponentActivity() {

    private lateinit var binding: ActivityRouletteBinding
    private lateinit var viewModel: RouletteViewModel
    private lateinit var adapter: RestaurantCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouletteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RestaurantCardAdapter()
        binding.objectArea.layoutManager = GridLayoutManager(this, 2)
        binding.objectArea.adapter = adapter

        lifecycleScope.launch {
            val initialRestaurants = FirestoreRestaurantRepository().getRandomRestaurants(limit = 6)
            val filterEngine = StatefulFilterEngineImpl()

            viewModel = ViewModelProvider(
                this@RouletteActivity,
                RouletteViewModelFactory(filterEngine, initialRestaurants)
            )[RouletteViewModel::class.java]

            viewModel.restaurants.observe(this@RouletteActivity) { list ->
                println("DEBUG: Activity observed ${list.size} restaurants")
                adapter.submitList(list)
                binding.spinWheelView.setItems(list)
            }

            viewModel.spinCount.observe(this@RouletteActivity) { count ->
                binding.decisionSectionView.updateSpinCount(count)
            }

            viewModel.loadRecommendedRestaurants()

            binding.spinButton.setOnClickListener {
                binding.spinWheelView.spin()
            }

            binding.spinWheelView.onSpinEnd = { selectedRestaurant ->
                Toast.makeText(
                    this@RouletteActivity,
                    "Selected: ${selectedRestaurant.name}",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.incrementSpinCount()
            }
        }
    }
}
