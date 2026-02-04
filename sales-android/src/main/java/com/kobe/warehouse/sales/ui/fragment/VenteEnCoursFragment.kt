package com.kobe.warehouse.sales.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.databinding.FragmentVenteEnCoursBinding
import com.kobe.warehouse.sales.ui.activity.UnifiedSaleActivity
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
 * - User filter Spinner
 */
class VenteEnCoursFragment : Fragment() {

    private var _binding: FragmentVenteEnCoursBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FullSaleHomeViewModel
    private lateinit var salesAdapter: SalesAdapter

    private var userList: List<com.kobe.warehouse.sales.data.model.User> = emptyList()
    private var selectedUserId: Int? = null
    private var isSpinnerInitialized = false

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
        selectedUserId = viewModel.defaultUserId

        setupRecyclerView()
        setupSwipeRefresh()
        setupSpinner()
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
            viewModel.loadOngoingSales(userId = selectedUserId)
        }

        // Set color scheme
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.success,
            R.color.warning
        )
    }

    /**
     * Setup user filter Spinner
     */
    private fun setupSpinner() {
        binding.spinnerUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }
                if (position < userList.size) {
                    val user = userList[position]
                    selectedUserId = user.id.toInt()
                    viewModel.loadOngoingSales(userId = selectedUserId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Setup listeners
     */
    private fun setupListeners() {
        // Retry button
        binding.btnRetry.setOnClickListener {
            viewModel.loadOngoingSales(userId = selectedUserId)
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

        // Observe users list for Spinner
        viewModel.users.observe(viewLifecycleOwner) { users ->
            populateUserSpinner(users)
        }
    }

    /**
     * Populate user Spinner with users list
     */
    private fun populateUserSpinner(users: List<com.kobe.warehouse.sales.data.model.User>) {
        userList = users
        val displayNames = users.map { it.getDisplayName() }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            displayNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        isSpinnerInitialized = false
        binding.spinnerUser.adapter = adapter

        // Select the default user (connected user)
        val defaultUserId = viewModel.defaultUserId
        if (defaultUserId != null) {
            val defaultIndex = users.indexOfFirst { it.id.toInt() == defaultUserId }
            if (defaultIndex >= 0) {
                binding.spinnerUser.setSelection(defaultIndex)
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
        val intent = Intent(requireContext(), UnifiedSaleActivity::class.java).apply {
            putExtra(UnifiedSaleActivity.EXTRA_SALE_ID, sale.id ?: 0L)
            putExtra(UnifiedSaleActivity.EXTRA_SALE_DATE, sale.saleId?.saleDate)
            putExtra(UnifiedSaleActivity.EXTRA_SALE_TYPE, sale.natureVente)
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
        val saleDate = sale.saleId?.saleDate
        val saleId = sale.id
        if (!saleDate.isNullOrEmpty() && saleId != null) {
            viewModel.deleteSale(saleId, saleDate, isPrevente = false)

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
