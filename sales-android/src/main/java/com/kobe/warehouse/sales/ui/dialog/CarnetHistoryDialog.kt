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
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.databinding.DialogCarnetHistoryBinding
import com.kobe.warehouse.sales.ui.adapter.SalesAdapter

/**
 * Dialog showing carnet purchase history for a customer
 */
class CarnetHistoryDialog : DialogFragment() {

    private var _binding: DialogCarnetHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SalesAdapter
    private var carnetSales: List<Sale> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCarnetHistoryBinding.inflate(layoutInflater)

        setupRecyclerView()
        loadHistory()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.historique_carnet)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { dialog, _ ->
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
        adapter = SalesAdapter(
            onSaleClick = { /* View sale details */ },
            onSaleDelete = null // No delete in history view
        )

        binding.rvCarnetHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CarnetHistoryDialog.adapter
        }
    }

    private fun loadHistory() {
        if (carnetSales.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCarnetHistory.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCarnetHistory.visibility = View.VISIBLE
            adapter.submitList(carnetSales)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(carnetSales: List<Sale>): CarnetHistoryDialog {
            return CarnetHistoryDialog().apply {
                this.carnetSales = carnetSales
            }
        }
    }
}
