package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R

/**
 * Discount Authorization Dialog
 * Dialog for confirming discount application with user authority check
 *
 * This dialog should be shown before applying discount to verify that
 * the user has the PR_REMISE authority (discount permission)
 *
 * In production, you would integrate with your authentication/authorization system
 * to verify the user's permissions before allowing the discount
 */
class DiscountAuthDialog : DialogFragment() {

    private var onAuthorized: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null
    private var discountAmount: Int = 0
    private var originalAmount: Int = 0

    companion object {
        private const val ARG_DISCOUNT_AMOUNT = "discount_amount"
        private const val ARG_ORIGINAL_AMOUNT = "original_amount"

        /**
         * Create authorization dialog for discount
         *
         * @param discountAmount The amount of discount to be applied
         * @param originalAmount The original amount before discount
         * @param onAuthorized Callback when user confirms authorization
         * @param onDenied Callback when user denies or cancels
         */
        fun newInstance(
            discountAmount: Int,
            originalAmount: Int,
            onAuthorized: () -> Unit,
            onDenied: () -> Unit
        ): DiscountAuthDialog {
            return DiscountAuthDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DISCOUNT_AMOUNT, discountAmount)
                    putInt(ARG_ORIGINAL_AMOUNT, originalAmount)
                }
                this.onAuthorized = onAuthorized
                this.onDenied = onDenied
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            discountAmount = args.getInt(ARG_DISCOUNT_AMOUNT, 0)
            originalAmount = args.getInt(ARG_ORIGINAL_AMOUNT, 0)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val netAmount = originalAmount - discountAmount
        val formattedDiscount = formatAmount(discountAmount)
        val formattedOriginal = formatAmount(originalAmount)
        val formattedNet = formatAmount(netAmount)

        val message = buildString {
            append("Montant original : $formattedOriginal\n")
            append("Remise : $formattedDiscount\n")
            append("Montant net : $formattedNet\n\n")
            append("Cette opération nécessite l'autorisation PR_REMISE.\n")
            append("Confirmez-vous l'application de cette remise ?")
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Autorisation de remise")
            .setMessage(message)
            .setIcon(R.drawable.ic_info)
            .setPositiveButton("Autoriser") { _, _ ->
                // TODO: In production, verify user has PR_REMISE authority
                // For now, we assume authorization is granted
                onAuthorized?.invoke()
                dismiss()
            }
            .setNegativeButton("Refuser") { _, _ ->
                onDenied?.invoke()
                dismiss()
            }
            .setCancelable(false) // Force user to make a choice

        return builder.create()
    }

    private fun formatAmount(amount: Int): String {
        val formatted = amount.toString().reversed().chunked(3).joinToString(" ").reversed()
        return "$formatted FCFA"
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        onDenied?.invoke()
    }
}
