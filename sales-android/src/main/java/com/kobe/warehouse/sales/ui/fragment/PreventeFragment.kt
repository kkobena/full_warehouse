package com.kobe.warehouse.sales.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.databinding.FragmentPreventeBinding
import com.kobe.warehouse.sales.ui.activity.ComptantSaleActivity
import com.kobe.warehouse.sales.ui.adapter.SalesAdapter
import com.kobe.warehouse.sales.ui.viewmodel.FullSaleHomeViewModel

/**
 * PreventeFragment
 * Fragment displaying list of preventes (sales on hold)
 *
 * Features:
 * - Pull-to-refresh
 * - Swipe-to-delete with confirmation
 * - Click to resume sale
 * - Empty state display
 * - Error handling with retry
 */
class PreventeFragment : Fragment() {

    private var _binding: FragmentPreventeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FullSaleHomeViewModel
    private lateinit var salesAdapter: SalesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreventeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get ViewModel from parent activity
        viewModel = ViewModelProvider(requireActivity())[FullSaleHomeViewModel::class.java]

        setupRecyclerView()
        setupSwipeRefresh()
        setupListeners()
        observeViewModel()
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        salesAdapter = SalesAdapter(
            onSaleClick = { sale -> resumeSale(sale) },
            onSaleDelete = { sale -> confirmDeletePrevente(sale) }
        )

        binding.rvPreventes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = salesAdapter
        }
    }

    /**
     * Setup SwipeRefreshLayout
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPreventes()
        }

        // Set color scheme
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.success,
            R.color.warning
        )
    }

    /**
     * Setup listeners
     */
    private fun setupListeners() {
        // Retry button
        binding.btnRetry.setOnClickListener {
            viewModel.refreshPreventes()
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe preventes
        viewModel.preventes.observe(viewLifecycleOwner) { preventes ->
            salesAdapter.submitList(preventes)
            updateUI(preventes)
        }

        // Observe loading state
        viewModel.isLoadingPreventes.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        // Observe error state
        viewModel.preventesError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                showError(error)
            } else {
                hideError()
            }
        }
    }

    /**
     * Update UI based on preventes list
     */
    private fun updateUI(preventes: List<Sale>) {
        if (preventes.isEmpty()) {
            binding.rvPreventes.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.errorState.visibility = View.GONE
        } else {
            binding.rvPreventes.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.errorState.visibility = View.GONE
        }
    }

    /**
     * Show error state
     */
    private fun showError(errorMessage: String) {
        binding.rvPreventes.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
        binding.tvErrorMessage.text = errorMessage
    }

    /**
     * Hide error state
     */
    private fun hideError() {
        binding.errorState.visibility = View.GONE
    }

    /**
     * Resume prevente (load it for editing)
     */
    private fun resumeSale(sale: Sale) {
        val intent = Intent(requireContext(), ComptantSaleActivity::class.java).apply {
            putExtra("saleId", sale.id)
            putExtra("saleDate", sale.updatedAt ?: sale.createdAt)
            putExtra("isPrevente", true)
        }
        startActivity(intent)
    }

    /**
     * Confirm prevente deletion
     */
    private fun confirmDeletePrevente(sale: Sale) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supprimer la prévente")
            .setMessage("Voulez-vous vraiment supprimer la prévente ${sale.numberTransaction} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                deletePrevente(sale)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Delete prevente
     */
    private fun deletePrevente(sale: Sale) {
        val saleDate = sale.updatedAt ?: sale.createdAt
        val saleId = sale.id
        if (saleDate != null && saleId != null) {
            viewModel.deleteSale(saleId, saleDate, isPrevente = true)

            // Show success message
            Snackbar.make(
                binding.root,
                "Prévente supprimée",
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            Snackbar.make(
                binding.root,
                "Erreur: date de vente manquante",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = PreventeFragment()
    }
}
