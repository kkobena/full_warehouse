package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.databinding.DialogSaleTransformationBinding
import com.kobe.warehouse.sales.data.model.SaleType

/**
 * Dialog for transforming a sale from one type to another
 *
 * Transformations supported:
 * - Comptant → Assurance
 * - Comptant → Carnet
 * - Assurance → Comptant
 * - Carnet → Comptant
 */
class SaleTransformationDialog : DialogFragment() {

    private var _binding: DialogSaleTransformationBinding? = null
    private val binding get() = _binding!!

    private var currentType: SaleType = SaleType.Comptant
    private var onTransformationConfirmed: ((SaleType) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSaleTransformationBinding.inflate(layoutInflater)

        setupViews()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.transformer_vente)
            .setView(binding.root)
            .setPositiveButton(R.string.transformer) { _, _ ->
                val selectedType = getSelectedType()
                selectedType?.let { onTransformationConfirmed?.invoke(it) }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    private fun setupViews() {
        binding.apply {
            // Display current type
            tvCurrentType.text = getString(R.string.type_actuel, getCurrentTypeName())

            // Setup transformation options based on current type
            when (currentType) {
                is SaleType.Comptant -> {
                    // Can transform to Assurance or Carnet
                    chipAssurance.visibility = View.VISIBLE
                    chipCarnet.visibility = View.VISIBLE
                    chipComptant.visibility = View.GONE
                }
                is SaleType.Assurance -> {
                    // Can transform to Comptant only
                    chipComptant.visibility = View.VISIBLE
                    chipAssurance.visibility = View.GONE
                    chipCarnet.visibility = View.GONE
                }
                is SaleType.Carnet -> {
                    // Can transform to Comptant only
                    chipComptant.visibility = View.VISIBLE
                    chipAssurance.visibility = View.GONE
                    chipCarnet.visibility = View.GONE
                }
            }

            // Show transformation info
            tvTransformationInfo.text = getTransformationInfo()
        }
    }

    private fun getCurrentTypeName(): String {
        return when (currentType) {
            is SaleType.Comptant -> getString(R.string.comptant)
            is SaleType.Assurance -> getString(R.string.assurance)
            is SaleType.Carnet -> getString(R.string.carnet)
        }
    }

    private fun getSelectedType(): SaleType? {
        binding.apply {
            return when {
                chipComptant.isChecked -> SaleType.Comptant
                chipAssurance.isChecked -> {
                    // Will need to select customer and tiers payants after transformation
                    SaleType.Comptant // Placeholder, will be set to Assurance after customer selection
                }
                chipCarnet.isChecked -> {
                    // Will need to select customer after transformation
                    SaleType.Comptant // Placeholder, will be set to Carnet after customer selection
                }
                else -> null
            }
        }
    }

    private fun getTransformationInfo(): String {
        return when (currentType) {
            is SaleType.Comptant -> getString(R.string.transformation_comptant_info)
            is SaleType.Assurance -> getString(R.string.transformation_assurance_info)
            is SaleType.Carnet -> getString(R.string.transformation_carnet_info)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            currentType: SaleType,
            onTransformationConfirmed: (SaleType) -> Unit
        ): SaleTransformationDialog {
            return SaleTransformationDialog().apply {
                this.currentType = currentType
                this.onTransformationConfirmed = onTransformationConfirmed
            }
        }
    }
}
