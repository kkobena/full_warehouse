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
import com.kobe.warehouse.sales.databinding.DialogDeconditionnementBinding

/**
 * Déconditionnement Dialog
 * Dialog for confirming product déconditionnement (breaking boxes into individual units)
 *
 * Déconditionnement allows breaking a box (parent product) into individual units (child product)
 * during a sale. This is useful when stock detail is insufficient but boxes are available.
 *
 * Features:
 * - Display product information (name, stock box, stock detail, item qty)
 * - Allow user to specify quantity to décondition
 * - Preview result before confirmation
 * - Input validation
 */
class DeconditionnementDialog : DialogFragment() {

    private var _binding: DialogDeconditionnementBinding? = null
    private val binding get() = _binding!!

    private var onDeconditionnementConfirmed: ((quantity: Int) -> Unit)? = null
    private lateinit var product: Product
    private var quantityToDecondition: Int = 1

    companion object {
        private const val ARG_PRODUCT = "product"

        /**
         * Create déconditionnement dialog
         *
         * @param product The product to décondition
         * @param onConfirmed Callback when user confirms déconditionnement
         */
        fun newInstance(
            product: Product,
            onConfirmed: (quantity: Int) -> Unit
        ): DeconditionnementDialog {
            return DeconditionnementDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRODUCT, product)
                }
                this.onDeconditionnementConfirmed = onConfirmed
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            product = args.getParcelable(ARG_PRODUCT)
                ?: throw IllegalArgumentException("Product is required")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDeconditionnementBinding.inflate(inflater, container, false)
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
        binding.tvStockBoite.text = (product.totalQuantity / (product.itemQty ?: 1)).toString()
        binding.tvStockDetail.text = (product.totalQuantity % (product.itemQty ?: 1)).toString()
        binding.tvItemQty.text = (product.itemQty ?: 1).toString()

        // Initialize quantity
        binding.etQuantity.setText("1")
        updateResultPreview()

        // Custom explanation based on product
        val itemQty = product.itemQty ?: 1
        val explanation = getString(
            R.string.deconditionnement_explanation
        )
        binding.tvExplanation.text = explanation
    }

    private fun setupListeners() {
        // Quantity input
        binding.etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val qty = s?.toString()?.toIntOrNull() ?: 1
                quantityToDecondition = qty
                updateResultPreview()
                validateInput()
            }
        })

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Confirm button
        binding.btnConfirm.setOnClickListener {
            if (validateInput()) {
                onDeconditionnementConfirmed?.invoke(quantityToDecondition)
                dismiss()
            }
        }
    }

    private fun updateResultPreview() {
        val itemQty = product.itemQty ?: 1
        val totalUnits = quantityToDecondition * itemQty
        val resultText = getString(
            R.string.deconditionnement_result_format,
            quantityToDecondition,
            totalUnits
        )
        binding.tvResultPreview.text = resultText
    }

    private fun validateInput(): Boolean {
        val qty = binding.etQuantity.text?.toString()?.toIntOrNull()

        if (qty == null || qty <= 0) {
            binding.tilQuantity.error = getString(R.string.deconditionnement_error_qty)
            binding.btnConfirm.isEnabled = false
            return false
        }

        // Check if enough stock boxes available
        val itemQty = product.itemQty ?: 1
        val availableBoxes = product.totalQuantity / itemQty

        if (qty > availableBoxes) {
            binding.tilQuantity.error = getString(R.string.deconditionnement_error_stock)
            binding.btnConfirm.isEnabled = false
            return false
        }

        binding.tilQuantity.error = null
        binding.btnConfirm.isEnabled = true
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
