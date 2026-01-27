package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.databinding.DialogAyantDroitBinding
import com.kobe.warehouse.sales.ui.adapter.AyantDroitAdapter

/**
 * Dialog for selecting an Ayant Droit (beneficiary)
 *
 * Used in Assurance sales when the customer wants to make a purchase
 * for a dependent (family member covered by their insurance)
 */
class AyantDroitDialog : DialogFragment() {

    private var _binding: DialogAyantDroitBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AyantDroitAdapter
    private var ayantDroits: List<Customer> = emptyList()
    private var onAyantDroitSelected: ((Customer) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAyantDroitBinding.inflate(layoutInflater)

        setupRecyclerView()
        loadAyantDroits()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.selectionner_ayant_droit)
            .setView(binding.root)
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

    private fun setupRecyclerView() {
        adapter = AyantDroitAdapter { ayantDroit ->
            onAyantDroitSelected?.invoke(ayantDroit)
            dismiss()
        }

        binding.rvAyantDroits.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AyantDroitDialog.adapter
        }
    }

    private fun loadAyantDroits() {
        if (ayantDroits.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvAyantDroits.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvAyantDroits.visibility = View.VISIBLE
            adapter.submitList(ayantDroits)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            ayantDroits: List<Customer>,
            onAyantDroitSelected: (Customer) -> Unit
        ): AyantDroitDialog {
            return AyantDroitDialog().apply {
                this.ayantDroits = ayantDroits
                this.onAyantDroitSelected = onAyantDroitSelected
            }
        }
    }
}
