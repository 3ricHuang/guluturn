package com.eric.guluturn.ui.roulette

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.view.LayoutInflater
import com.eric.guluturn.databinding.ViewDecisionSectionBinding

/**
 * View showing the roulette spin progress count.
 * Wraps a single RouletteProgressView via view_decision_section.xml.
 */
class DecisionSectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewDecisionSectionBinding

    init {
        orientation = HORIZONTAL
        binding = ViewDecisionSectionBinding.inflate(LayoutInflater.from(context), this)
    }

    fun updateSpinCount(count: Int) {
        binding.spinProgressView.spinCount = count
    }
}
