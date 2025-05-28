package com.eric.guluturn.ui.roulette

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.eric.guluturn.R
import com.eric.guluturn.common.models.Restaurant

/**
 * Handles the display and interaction logic for the roulette decision dialogs,
 * including the result card view and the rejection reason input dialog.
 *
 * @param context the UI context to inflate dialogs
 * @param parent the parent view group used for layout inflation
 * @param onRejectConfirmed callback invoked with a rejection reason
 * @param onAcceptConfirmed callback invoked when the selection is accepted
 */
class RouletteDialogManager(
    private val context: Context,
    private val parent: ViewGroup,
    private val onRejectConfirmed: (String) -> Unit,
    private val onAcceptConfirmed: () -> Unit
) {
    /**
     * Shows the result dialog with a focus card for the selected restaurant.
     * Users can choose to accept or reject the restaurant.
     */
    fun showResultDialog(restaurant: Restaurant) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_result, parent, false)
        val dialog = AlertDialog.Builder(context, R.style.DialogTheme)
            .setView(view)
            .setCancelable(false)
            .create()

        // Inject restaurant card into dialog container
        val container = view.findViewById<FrameLayout>(R.id.dialogResultContainer)
        val cardView = RestaurantFocusCardView(context)
        cardView.bind(restaurant)
        container.removeAllViews()
        container.addView(cardView)

        // Confirm selection ("Accept")
        view.findViewById<Button>(R.id.btnResultConfirm).setOnClickListener {
            dialog.dismiss()
            onAcceptConfirmed()
        }

        // Reject selection → open reason input dialog
        view.findViewById<Button>(R.id.btnResultCancel).setOnClickListener {
            dialog.dismiss()
            showReasonDialog(restaurant)
        }

        dialog.show()
    }

    /**
     * Shows a secondary dialog prompting the user to enter a reason for rejection.
     * If the reason is non-empty, the rejection callback is triggered.
     */
    private fun showReasonDialog(restaurant: Restaurant) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_reason, parent, false)
        val dialog = AlertDialog.Builder(context, R.style.DialogTheme)
            .setView(view)
            .setCancelable(false)
            .create()

        val input = view.findViewById<EditText>(R.id.dialog_reason_input)

        // Confirm rejection and pass input reason
        view.findViewById<Button>(R.id.btnReasonConfirm).setOnClickListener {
            val reason = input.text.toString().trim()
            if (reason.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.roulette_reason_required), Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                onRejectConfirmed(reason)
            }
        }

        // Cancel input and return to result dialog
        view.findViewById<Button>(R.id.btnReasonCancel).setOnClickListener {
            dialog.dismiss()
            showResultDialog(restaurant)
        }

        dialog.show()
    }
}
