package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.databinding.DialogTiersPayantSelectorBinding
import com.kobe.warehouse.sales.data.model.TiersPayant
import com.kobe.warehouse.sales.ui.adapter.TiersPayantAdapter
import com.kobe.warehouse.sales.ui.viewmodel.InsuranceDataViewModel
import com.kobe.warehouse.sales.ui.viewmodel.InsuranceDataViewModelFactory

/**
 * Dialog for selecting a Tiers Payant (insurance provider)
 */
class TiersPayantSelectorDialog : DialogFragment() {

    private var _binding: DialogTiersPayantSelectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TiersPayantAdapter
    private var onTiersPayantSelected: ((TiersPayant) -> Unit)? = null

    private val viewModel: InsuranceDataViewModel by viewModels {
        InsuranceDataViewModelFactory()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTiersPayantSelectorBinding.inflate(layoutInflater)

        setupRecyclerView()
        setupSearch()
        observeViewModel()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.selectionner_tiers_payant)
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
        adapter = TiersPayantAdapter { tiersPayant ->
            onTiersPayantSelected?.invoke(tiersPayant)
            dismiss()
        }

        binding.rvTiersPayants.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@TiersPayantSelectorDialog.adapter
        }
    }

    private fun setupSearch() {
        // TODO: Implement tiers payant search when InsuranceDataViewModel has search methods
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // val query = s?.toString() ?: ""
                // if (query.length >= 2) {
                //     viewModel.searchTiersPayants(query)
                // }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        // TODO: Implement observers when InsuranceDataViewModel has tiersPayantSearchResults and isLoading LiveData
        // viewModel.tiersPayantSearchResults.observe(this) { results ->
        //     if (results.isEmpty()) {
        //         binding.tvEmptyState.visibility = View.VISIBLE
        //         binding.rvTiersPayants.visibility = View.GONE
        //     } else {
        //         binding.tvEmptyState.visibility = View.GONE
        //         binding.rvTiersPayants.visibility = View.VISIBLE
        //         adapter.submitList(results)
        //     }
        // }
        //
        // viewModel.isLoading.observe(this) { isLoading ->
        //     binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            onTiersPayantSelected: (TiersPayant) -> Unit
        ): TiersPayantSelectorDialog {
            return TiersPayantSelectorDialog().apply {
                this.onTiersPayantSelected = onTiersPayantSelected
            }
        }
    }
}
