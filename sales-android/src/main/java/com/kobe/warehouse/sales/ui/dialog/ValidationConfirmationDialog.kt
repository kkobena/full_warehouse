package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R

/**
 * Validation Confirmation Dialog
 * Generic dialog for confirming stock validation actions
 * Used for force stock, deconditionnement, and quantity limit confirmations
 */
class ValidationConfirmationDialog : DialogFragment() {

    private var onConfirm: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    private var dialogTitle: String = "Confirmation"
    private var dialogMessage: String = ""
    private var confirmButtonText: String = "Oui"
    private var cancelButtonText: String = "Non"
    private var isWarning: Boolean = false

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_CONFIRM_TEXT = "confirm_text"
        private const val ARG_CANCEL_TEXT = "cancel_text"
        private const val ARG_IS_WARNING = "is_warning"

        /**
         * Create force stock confirmation dialog
         */
        fun forceStock(
            message: String,
            onConfirm: () -> Unit,
            onCancel: () -> Unit
        ): ValidationConfirmationDialog {
            return ValidationConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, "Stock insuffisant")
                    putString(ARG_MESSAGE, message)
                    putString(ARG_CONFIRM_TEXT, "Continuer")
                    putString(ARG_CANCEL_TEXT, "Annuler")
                    putBoolean(ARG_IS_WARNING, true)
                }
                this.onConfirm = onConfirm
                this.onCancel = onCancel
            }
        }

        /**
         * Create quantity exceeds max confirmation dialog
         */
        fun quantityExceedsMax(
            message: String,
            onConfirm: () -> Unit,
            onCancel: () -> Unit
        ): ValidationConfirmationDialog {
            return ValidationConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, "Quantité maximale dépassée")
                    putString(ARG_MESSAGE, message)
                    putString(ARG_CONFIRM_TEXT, "Continuer")
                    putString(ARG_CANCEL_TEXT, "Annuler")
                    putBoolean(ARG_IS_WARNING, true)
                }
                this.onConfirm = onConfirm
                this.onCancel = onCancel
            }
        }

        /**
         * Create error dialog (no confirmation, just error message)
         */
        fun error(
            title: String,
            message: String,
            onDismiss: () -> Unit
        ): ValidationConfirmationDialog {
            return ValidationConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_CONFIRM_TEXT, "OK")
                    putString(ARG_CANCEL_TEXT, "")
                    putBoolean(ARG_IS_WARNING, false)
                }
                this.onConfirm = onDismiss
                this.onCancel = onDismiss
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            dialogTitle = args.getString(ARG_TITLE, "Confirmation")
            dialogMessage = args.getString(ARG_MESSAGE, "")
            confirmButtonText = args.getString(ARG_CONFIRM_TEXT, "Oui")
            cancelButtonText = args.getString(ARG_CANCEL_TEXT, "Non")
            isWarning = args.getBoolean(ARG_IS_WARNING, false)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setPositiveButton(confirmButtonText) { _, _ ->
                onConfirm?.invoke()
                dismiss()
            }

        // Add cancel button only if text is not empty
        if (cancelButtonText.isNotEmpty()) {
            builder.setNegativeButton(cancelButtonText) { _, _ ->
                onCancel?.invoke()
                dismiss()
            }
        }

        // Set icon based on warning flag
        if (isWarning) {
            builder.setIcon(R.drawable.ic_error)
        }

        return builder.create()
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        onCancel?.invoke()
    }
}
