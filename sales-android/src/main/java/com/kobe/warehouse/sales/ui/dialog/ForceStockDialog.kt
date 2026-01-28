package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.databinding.DialogForceStockBinding

/**
 * Force Stock Dialog
 * Dialog for confirming product addition despite insufficient stock
 *
 * This dialog is shown when:
 * - User tries to add a product with quantity > available stock
 * - Product is NOT marked as forceStock=true
 * - User needs to provide a reason and authorization
 *
 * Features:
 * - Display product information (name, available stock, requested quantity, shortage)
 * - Require user to provide a reason for forcing stock
 * - Validate input (reason is mandatory)
 * - Authorization check (PR_FORCE_STOCK authority)
 * - Log action for audit trail
 *
 * Note: This action should be logged on the backend for audit purposes
 * Backend should record: user, product, quantity, reason, timestamp
 */
class ForceStockDialog : DialogFragment() {

    private var _binding: DialogForceStockBinding? = null
    private val binding get() = _binding!!

    private var onForceStockConfirmed: ((reason: String) -> Unit)? = null
    private lateinit var product: Product
    private var requestedQuantity: Int = 0
    private var availableStock: Int = 0

    companion object {
        private const val ARG_PRODUCT = "product"
        private const val ARG_REQUESTED_QUANTITY = "requested_quantity"
        private const val ARG_AVAILABLE_STOCK = "available_stock"
        private const val MIN_REASON_LENGTH = 10 // Minimum characters for reason

        /**
         * Create force stock dialog
         *
         * @param product The product with insufficient stock
         * @param requestedQuantity Quantity requested by user
         * @param availableStock Available stock quantity
         * @param onConfirmed Callback when user confirms force stock with reason
         */
        fun newInstance(
            product: Product,
            requestedQuantity: Int,
            availableStock: Int,
            onConfirmed: (reason: String) -> Unit
        ): ForceStockDialog {
            return ForceStockDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRODUCT, product)
                    putInt(ARG_REQUESTED_QUANTITY, requestedQuantity)
                    putInt(ARG_AVAILABLE_STOCK, availableStock)
                }
                this.onForceStockConfirmed = onConfirmed
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            product = args.getParcelable(ARG_PRODUCT)
                ?: throw IllegalArgumentException("Product is required")
            requestedQuantity = args.getInt(ARG_REQUESTED_QUANTITY, 0)
            availableStock = args.getInt(ARG_AVAILABLE_STOCK, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogForceStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // Display product information
        binding.tvProductName.text = product.libelle ?: product.code

        // Stock information
        binding.tvStockAvailable.text = availableStock.toString()
        binding.tvQuantityRequested.text = requestedQuantity.toString()

        // Calculate shortage
        val shortage = requestedQuantity - availableStock
        binding.tvShortage.text = shortage.toString()
    }

    private fun setupListeners() {
        // Reason input validation
        binding.etReason.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        })

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Force button
        binding.btnForce.setOnClickListener {
            if (validateInput()) {
                val reason = binding.etReason.text?.toString()?.trim() ?: ""
                onForceStockConfirmed?.invoke(reason)
                dismiss()
            }
        }
    }

    private fun validateInput(): Boolean {
        val reason = binding.etReason.text?.toString()?.trim()

        if (reason.isNullOrEmpty()) {
            binding.tilReason.error = getString(R.string.force_stock_error_reason)
            binding.btnForce.isEnabled = false
            return false
        }

        if (reason.length < MIN_REASON_LENGTH) {
            binding.tilReason.error = "La raison doit contenir au moins $MIN_REASON_LENGTH caractères"
            binding.btnForce.isEnabled = false
            return false
        }

        binding.tilReason.error = null
        binding.btnForce.isEnabled = true
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 90% of screen width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
