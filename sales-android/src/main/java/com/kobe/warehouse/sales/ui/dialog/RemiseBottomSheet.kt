package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kobe.warehouse.sales.data.model.Remise
import com.kobe.warehouse.sales.databinding.BottomSheetRemiseBinding
import com.kobe.warehouse.sales.ui.adapter.RemiseAdapter

/**
 * BottomSheet for selecting a predefined remise (discount)
 *
 * Compact UI optimized for small mobile screens:
 * - Slides up from bottom, takes minimal space
 * - Simple list with name + rate badge
 * - Single tap to select
 */
class RemiseBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRemiseBinding? = null
    private val binding get() = _binding!!

    private var remises: List<Remise> = emptyList()
    private var saleType: String = "COMPTANT"
    private var currentRemiseId: Int? = null
    private var onRemiseSelected: ((Remise) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRemiseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Filter out current remise if editing
        val availableRemises = if (currentRemiseId != null) {
            remises.filter { it.id != currentRemiseId }
        } else {
            remises
        }

        if (availableRemises.isEmpty()) {
            binding.tvEmptyRemises.visibility = View.VISIBLE
            binding.rvRemises.visibility = View.GONE
            return
        }

        binding.tvEmptyRemises.visibility = View.GONE
        binding.rvRemises.visibility = View.VISIBLE

        val adapter = RemiseAdapter(saleType) { remise ->
            onRemiseSelected?.invoke(remise)
            dismiss()
        }

        binding.rvRemises.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        adapter.submitList(availableRemises)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            remises: List<Remise>,
            saleType: String,
            currentRemiseId: Int? = null,
            onRemiseSelected: (Remise) -> Unit
        ): RemiseBottomSheet {
            return RemiseBottomSheet().apply {
                this.remises = remises
                this.saleType = saleType
                this.currentRemiseId = currentRemiseId
                this.onRemiseSelected = onRemiseSelected
            }
        }
    }
}
