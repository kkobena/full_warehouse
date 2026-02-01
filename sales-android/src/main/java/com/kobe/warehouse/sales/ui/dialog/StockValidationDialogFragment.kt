package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.domain.validation.StockValidationResult
import com.kobe.warehouse.sales.domain.validation.StockValidationStatus

/**
 * Stock Validation Dialog
 * Shows confirmation dialog for stock issues (force stock, deconditioning)
 */
class StockValidationDialogFragment(
    private val validationResult: StockValidationResult,
    private val onConfirm: (forceStock: Boolean) -> Unit,
    private val onCancel: () -> Unit = {}
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = when (validationResult.status) {
            StockValidationStatus.INSUFFICIENT_STOCK_CAN_FORCE -> "Stock Insuffisant"
            StockValidationStatus.QUANTITY_EXCESSIVE_CAN_FORCE -> "Quantité Excessive"
            StockValidationStatus.REQUIRES_DECONDITIONING -> "Déconditionnement Requis"
            else -> "Validation Stock"
        }

        val message = validationResult.getConfirmationMessage()

        val positiveButtonText = when (validationResult.status) {
            StockValidationStatus.INSUFFICIENT_STOCK_CAN_FORCE,
            StockValidationStatus.QUANTITY_EXCESSIVE_CAN_FORCE -> "Forcer le stock"
            StockValidationStatus.REQUIRES_DECONDITIONING -> "Déconditionner"
            else -> "Confirmer"
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                // Force stock for all confirmation cases
                val forceStock = when (validationResult.status) {
                    StockValidationStatus.INSUFFICIENT_STOCK_CAN_FORCE,
                    StockValidationStatus.QUANTITY_EXCESSIVE_CAN_FORCE -> true
                    StockValidationStatus.REQUIRES_DECONDITIONING -> false  // Deconditioning doesn't need force stock flag
                    else -> false
                }
                onConfirm(forceStock)
            }
            .setNegativeButton("Annuler") { _, _ ->
                onCancel()
            }
            .setCancelable(false)
            .create()
    }

    companion object {
        const val TAG = "StockValidationDialog"
    }
}
