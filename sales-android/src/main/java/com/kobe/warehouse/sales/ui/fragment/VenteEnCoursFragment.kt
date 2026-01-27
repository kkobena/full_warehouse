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
import com.kobe.warehouse.sales.databinding.FragmentVenteEnCoursBinding
import com.kobe.warehouse.sales.ui.activity.ComptantSaleActivity
import com.kobe.warehouse.sales.ui.adapter.SalesAdapter
import com.kobe.warehouse.sales.ui.viewmodel.FullSaleHomeViewModel

/**
 * VenteEnCoursFragment
 * Fragment displaying list of ongoing sales (ventes en cours)
 *
 * Features:
 * - Pull-to-refresh
 * - Swipe-to-delete with confirmation
 * - Click to open sale
 * - Empty state display
 * - Error handling with retry
 */
class VenteEnCoursFragment : Fragment() {

    private var _binding: FragmentVenteEnCoursBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FullSaleHomeViewModel
    private lateinit var salesAdapter: SalesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVenteEnCoursBinding.inflate(inflater, container, false)
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
            onSaleClick = { sale -> openSale(sale) },
            onSaleDelete = { sale -> confirmDeleteSale(sale) }
        )

        binding.rvSales.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = salesAdapter
        }
    }

    /**
     * Setup SwipeRefreshLayout
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshOngoingSales()
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
            viewModel.refreshOngoingSales()
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe ongoing sales
        viewModel.ongoingSales.observe(viewLifecycleOwner) { sales ->
            salesAdapter.submitList(sales)
            updateUI(sales)
        }

        // Observe loading state
        viewModel.isLoadingOngoing.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        // Observe error state
        viewModel.ongoingError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                showError(error)
            } else {
                hideError()
            }
        }
    }

    /**
     * Update UI based on sales list
     */
    private fun updateUI(sales: List<Sale>) {
        if (sales.isEmpty()) {
            binding.rvSales.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.errorState.visibility = View.GONE
        } else {
            binding.rvSales.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.errorState.visibility = View.GONE
        }
    }

    /**
     * Show error state
     */
    private fun showError(errorMessage: String) {
        binding.rvSales.visibility = View.GONE
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
     * Open sale for editing
     */
    private fun openSale(sale: Sale) {
        val intent = Intent(requireContext(), ComptantSaleActivity::class.java).apply {
            putExtra("saleId", sale.id)
            putExtra("saleDate", sale.updatedAt ?: sale.createdAt)
        }
        startActivity(intent)
    }

    /**
     * Confirm sale deletion
     */
    private fun confirmDeleteSale(sale: Sale) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supprimer la vente")
            .setMessage("Voulez-vous vraiment supprimer la vente ${sale.numberTransaction} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteSale(sale)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Delete sale
     */
    private fun deleteSale(sale: Sale) {
        val saleDate = sale.updatedAt ?: sale.createdAt
        if (saleDate != null) {
            viewModel.deleteSale(sale.id, saleDate, isPrevente = false)

            // Show success message
            Snackbar.make(
                binding.root,
                "Vente supprimée",
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
        fun newInstance() = VenteEnCoursFragment()
    }
}
