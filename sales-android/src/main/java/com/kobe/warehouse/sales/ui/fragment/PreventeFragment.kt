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
import com.kobe.warehouse.sales.data.model.SalesStatut
import com.kobe.warehouse.sales.databinding.FragmentPreventeBinding
import com.kobe.warehouse.sales.ui.activity.UnifiedSaleActivity
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
 * - User filter Spinner
 */
class PreventeFragment : Fragment() {

    private var _binding: FragmentPreventeBinding? = null
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
        _binding = FragmentPreventeBinding.inflate(inflater, container, false)
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
            viewModel.loadPreventes(userId = selectedUserId)
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
                    viewModel.loadPreventes(userId = selectedUserId)
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
            viewModel.loadPreventes(userId = selectedUserId)
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

        // Observe users list for Spinner
        viewModel.users.observe(viewLifecycleOwner) { users ->
            populateUserSpinner(users)
        }

        // Observe navigation event to switch to vente en cours tab
        viewModel.navigateToVenteEnCours.observe(viewLifecycleOwner) { event ->
            if (event != null) {
                viewModel.clearNavigateToVenteEnCours()
                navigateToVenteEnCours(event)
            }
        }
    }

    /**
     * Navigate to Vente en cours tab and open the sale
     */
    private fun navigateToVenteEnCours(event: FullSaleHomeViewModel.NavigateToSaleEvent) {
        // Switch to vente en cours tab (index 0) via parent activity
        (activity as? OnTabSwitchListener)?.switchToTab(0)

        // Open the sale for editing in UnifiedSaleActivity
        val intent = Intent(requireContext(), UnifiedSaleActivity::class.java).apply {
            putExtra(UnifiedSaleActivity.EXTRA_SALE_ID, event.saleId)
            putExtra(UnifiedSaleActivity.EXTRA_SALE_DATE, event.saleDate)
            putExtra(UnifiedSaleActivity.EXTRA_SALE_TYPE, event.natureVente)
        }
        startActivity(intent)
    }

    /**
     * Interface for tab switching (implemented by parent activity)
     */
    interface OnTabSwitchListener {
        fun switchToTab(index: Int)
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
     * - PENDING: Show dialog to transform to vente en cours
     * - PROCESSING: Open for editing directly
     */
    private fun resumeSale(sale: Sale) {
        when (sale.statut) {
            SalesStatut.PENDING -> {
                // Show dialog to transform prevente to vente en cours
                showTransformDialog(sale)
            }
            SalesStatut.PROCESSING -> {
                // Open for editing directly
                openSaleForEditing(sale)
            }
            else -> {
                // Default: open for editing
                openSaleForEditing(sale)
            }
        }
    }

    /**
     * Show dialog to confirm transformation of prevente to vente en cours
     */
    private fun showTransformDialog(sale: Sale) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.transform_prevente_title)
            .setMessage(R.string.transform_prevente_message)
            .setPositiveButton(R.string.transformer) { _, _ ->
                transformPreventeToVenteEnCours(sale)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Transform prevente to vente en cours and redirect to corresponding tab
     */
    private fun transformPreventeToVenteEnCours(sale: Sale) {
        val saleId = sale.id ?: return
        val saleDate = sale.saleId?.saleDate ?: return

        // Call ViewModel to transform the sale (set statut to PROCESSING)
        viewModel.transformPreventeToVenteEnCours(saleId, saleDate, sale.natureVente ?: "COMPTANT")
    }

    /**
     * Open sale for editing in UnifiedSaleActivity
     */
    private fun openSaleForEditing(sale: Sale) {
        val intent = Intent(requireContext(), UnifiedSaleActivity::class.java).apply {
            putExtra(UnifiedSaleActivity.EXTRA_SALE_ID, sale.id ?: 0L)
            putExtra(UnifiedSaleActivity.EXTRA_SALE_DATE, sale.saleId?.saleDate)
            putExtra(UnifiedSaleActivity.EXTRA_SALE_TYPE, sale.natureVente)
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
        val saleDate = sale.saleId?.saleDate
        val saleId = sale.id
        if (!saleDate.isNullOrEmpty() && saleId != null) {
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
