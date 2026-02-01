package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.databinding.DialogDiscountBinding
import com.kobe.warehouse.sales.data.model.Discount
import com.kobe.warehouse.sales.data.model.DiscountType

/**
 * Discount Dialog
 * Dialog for applying discount to sale or sale line
 *
 * Features:
 * - Percentage or fixed amount discount
 * - Quick amount buttons for common fixed amounts
 * - Real-time discount preview
 * - Input validation
 * - Apply/Cancel actions
 */
class DiscountDialog : DialogFragment() {

    private var _binding: DialogDiscountBinding? = null
    private val binding get() = _binding!!

    private var onDiscountApplied: ((Discount) -> Unit)? = null
    private var originalAmount: Int = 0
    private var currentDiscountType: DiscountType = DiscountType.PERCENTAGE
    private var currentDiscountValue: Int = 0

    companion object {
        private const val ARG_ORIGINAL_AMOUNT = "original_amount"
        private const val ARG_EXISTING_DISCOUNT = "existing_discount"

        /**
         * Create discount dialog for new discount
         */
        fun newInstance(
            originalAmount: Int,
            onApplied: (Discount) -> Unit
        ): DiscountDialog {
            return DiscountDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ORIGINAL_AMOUNT, originalAmount)
                }
                this.onDiscountApplied = onApplied
            }
        }

        /**
         * Create discount dialog for editing existing discount
         */
        fun editInstance(
            originalAmount: Int,
            existingDiscount: Discount,
            onApplied: (Discount) -> Unit
        ): DiscountDialog {
            return DiscountDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ORIGINAL_AMOUNT, originalAmount)
                    putParcelable(ARG_EXISTING_DISCOUNT, existingDiscount)
                }
                this.onDiscountApplied = onApplied
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            originalAmount = args.getInt(ARG_ORIGINAL_AMOUNT, 0)

            // Load existing discount if editing
            args.getParcelable<Discount>(ARG_EXISTING_DISCOUNT)?.let { discount ->
                currentDiscountType = discount.type
                currentDiscountValue = discount.value
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDiscountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
        loadExistingDiscount()
    }

    private fun setupViews() {
        // Display original amount
        binding.tvOriginalAmount.text = formatAmount(originalAmount)

        // Set default discount type
        when (currentDiscountType) {
            DiscountType.PERCENTAGE -> {
                binding.chipPercentage.isChecked = true
                updateDiscountTypeUI(DiscountType.PERCENTAGE)
            }
            DiscountType.FIXED -> {
                binding.chipFixed.isChecked = true
                updateDiscountTypeUI(DiscountType.FIXED)
            }
        }
    }

    private fun setupListeners() {
        // Discount type selection
        binding.chipGroupDiscountType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_percentage -> {
                    currentDiscountType = DiscountType.PERCENTAGE
                    updateDiscountTypeUI(DiscountType.PERCENTAGE)
                }
                R.id.chip_fixed -> {
                    currentDiscountType = DiscountType.FIXED
                    updateDiscountTypeUI(DiscountType.FIXED)
                }
            }
        }

        // Discount value input
        binding.etDiscountValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toIntOrNull() ?: 0
                currentDiscountValue = value
                updateDiscountPreview()
                validateInput()
            }
        })

        // Quick amount buttons (for fixed discount)
        binding.chipAmount100.setOnClickListener {
            binding.etDiscountValue.setText("100")
        }
        binding.chipAmount500.setOnClickListener {
            binding.etDiscountValue.setText("500")
        }
        binding.chipAmount1000.setOnClickListener {
            binding.etDiscountValue.setText("1000")
        }
        binding.chipAmount5000.setOnClickListener {
            binding.etDiscountValue.setText("5000")
        }

        // Action buttons
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnApply.setOnClickListener {
            applyDiscount()
        }
    }

    private fun loadExistingDiscount() {
        if (currentDiscountValue > 0) {
            binding.etDiscountValue.setText(currentDiscountValue.toString())
            updateDiscountPreview()
        }
    }

    private fun updateDiscountTypeUI(type: DiscountType) {
        when (type) {
            DiscountType.PERCENTAGE -> {
                // Show percentage suffix
                binding.tilDiscountValue.prefixText = ""
                binding.tilDiscountValue.suffixText = "%"
                binding.tilDiscountValue.hint = getString(R.string.discount_value_hint) + " (1-100)"

                // Hide quick amounts
                binding.tvQuickAmountsLabel.isVisible = false
                binding.chipGroupQuickAmounts.isVisible = false

                // Clear input if switching from fixed
                if (currentDiscountValue > 100) {
                    binding.etDiscountValue.text?.clear()
                }
            }
            DiscountType.FIXED -> {
                // Show FCFA suffix
                binding.tilDiscountValue.prefixText = ""
                binding.tilDiscountValue.suffixText = " FCFA"
                binding.tilDiscountValue.hint = getString(R.string.discount_value_hint)

                // Show quick amounts
                binding.tvQuickAmountsLabel.isVisible = true
                binding.chipGroupQuickAmounts.isVisible = true
            }
        }

        updateDiscountPreview()
    }

    private fun updateDiscountPreview() {
        val discountAmount = calculateDiscountAmount()
        val netAmount = originalAmount - discountAmount

        binding.tvDiscountAmount.text = formatAmount(discountAmount)
        binding.tvNetAmount.text = formatAmount(netAmount)
    }

    private fun calculateDiscountAmount(): Int {
        return when (currentDiscountType) {
            DiscountType.PERCENTAGE -> {
                if (currentDiscountValue in 1..100) {
                    (originalAmount * currentDiscountValue) / 100
                } else {
                    0
                }
            }
            DiscountType.FIXED -> {
                minOf(currentDiscountValue, originalAmount)
            }
        }
    }

    private fun validateInput(): Boolean {
        val value = binding.etDiscountValue.text?.toString()?.toIntOrNull()

        if (value == null || value == 0) {
            binding.tilDiscountValue.error = getString(R.string.discount_error_value)
            binding.btnApply.isEnabled = false
            return false
        }

        when (currentDiscountType) {
            DiscountType.PERCENTAGE -> {
                if (value !in 1..100) {
                    binding.tilDiscountValue.error = getString(R.string.discount_error_percentage_range)
                    binding.btnApply.isEnabled = false
                    return false
                }
            }
            DiscountType.FIXED -> {
                if (value <= 0) {
                    binding.tilDiscountValue.error = getString(R.string.discount_error_fixed_amount)
                    binding.btnApply.isEnabled = false
                    return false
                }
                if (value > originalAmount) {
                    binding.tilDiscountValue.error = getString(R.string.discount_error_exceeds_amount)
                    binding.btnApply.isEnabled = false
                    return false
                }
            }
        }

        binding.tilDiscountValue.error = null
        binding.btnApply.isEnabled = true
        return true
    }

    private fun applyDiscount() {
        if (!validateInput()) {
            return
        }

        val discount = Discount(
            value = currentDiscountValue,
            type = currentDiscountType
        )

        onDiscountApplied?.invoke(discount)
        dismiss()
    }

    private fun formatAmount(amount: Int): String {
        val formatted = amount.toString().reversed().chunked(3).joinToString(" ").reversed()
        return "$formatted FCFA"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
