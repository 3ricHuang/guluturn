package com.eric.guluturn.ui.roulette

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eric.guluturn.common.models.Restaurant
import com.eric.guluturn.databinding.ItemRestaurantCardBinding

/**
 * Adapter for displaying a list of candidate restaurants in the roulette UI.
 * Each card corresponds to a restaurant segment on the roulette wheel.
 */
class RestaurantCardAdapter : RecyclerView.Adapter<RestaurantCardAdapter.CardViewHolder>() {

    private var items: List<Restaurant> = emptyList()

    fun submitList(newItems: List<Restaurant>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class CardViewHolder(private val binding: ItemRestaurantCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Restaurant) {
            binding.tvItem.text = item.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRestaurantCardBinding.inflate(inflater, parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
