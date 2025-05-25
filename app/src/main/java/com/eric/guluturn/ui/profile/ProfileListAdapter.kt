package com.eric.guluturn.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eric.guluturn.R
import com.eric.guluturn.common.models.UserProfile
import com.eric.guluturn.databinding.ItemUserProfileBinding

/**
 * Adapter for displaying a list of user profiles.
 * Invokes a click callback when a profile is selected.
 */
class ProfileListAdapter(
    private val onClick: (UserProfile) -> Unit
) : ListAdapter<UserProfile, ProfileListAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemUserProfileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: UserProfile) {
            val context = binding.root.context

            binding.profileIcon.setImageResource(R.drawable.baseline_account_box_24)

            val ageGenderLabel = context.getString(
                R.string.profile_age_gender,
                profile.age,
                profile.gender.name.lowercase().replaceFirstChar { it.uppercase() }
            )
            binding.nameText.text = profile.name
            binding.ageGenderText.text = ageGenderLabel

            binding.root.setOnClickListener {
                onClick(profile)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemUserProfileBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(old: UserProfile, new: UserProfile): Boolean =
            old.uuid == new.uuid

        override fun areContentsTheSame(old: UserProfile, new: UserProfile): Boolean =
            old == new
    }
}
