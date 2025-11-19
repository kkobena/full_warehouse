package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.databinding.ActivitySalesHomeBinding
import com.kobe.warehouse.sales.ui.adapter.SalesAdapter
import com.kobe.warehouse.sales.ui.viewmodel.SalesHomeViewModel
import com.kobe.warehouse.sales.ui.viewmodel.SalesHomeViewModelFactory
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * SalesHomeActivity
 * Displays list of ongoing sales (pré-ventes)
 * Extends BaseActivity for automatic session management
 *
 * Features:
 * - Search field
 * - Sales list with RecyclerView
 * - Click sale to open ComptantSaleActivity
 */
class SalesHomeActivity : BaseActivity() {

    private lateinit var binding: ActivitySalesHomeBinding
    private lateinit var viewModel: SalesHomeViewModel
    private lateinit var salesAdapter: SalesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySalesHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupListeners()
        observeViewModel()
    }

    /**
     * Setup ViewModel with dependencies
     */
    private fun setupViewModel() {
        val tokenManager = TokenManager(this)
        val retrofit = ApiClient.create(tokenManager = tokenManager)
        val salesApiService = retrofit.create(SalesApiService::class.java)
        val salesRepository = SalesRepository(salesApiService)

        val factory = SalesHomeViewModelFactory(salesRepository)
        viewModel = ViewModelProvider(this, factory)[SalesHomeViewModel::class.java]
    }

    /**
     * Setup toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)  // No back button - this is the main screen
            title = "Ventes en cours"
        }
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        salesAdapter = SalesAdapter(
            onSaleClick = { sale -> openSale(sale) },
            onEditClick = { sale -> editSale(sale) },
            onDeleteClick = { sale -> confirmDeleteSale(sale) }
        )

        binding.rvSales.apply {
            layoutManager = LinearLayoutManager(this@SalesHomeActivity)
            adapter = salesAdapter
        }
    }

    /**
     * Setup search filter
     */
    private fun setupFilters() {
        // Search text listener (debounced)
        binding.etSearch.addTextChangedListener { text ->
            // Simple search without debounce for now
            viewModel.searchSales(text.toString())
        }
    }

    /**
     * Setup button listeners
     */
    private fun setupListeners() {
        binding.btnNewSale.setOnClickListener {
            openComptantSale()
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe sales list
        viewModel.sales.observe(this) { sales ->
            salesAdapter.submitList(sales)

            // Show/hide empty state
            if (sales.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvSales.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvSales.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe delete success
        viewModel.deleteSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Vente supprimée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Open sale for viewing/editing
     */
    private fun openSale(sale: Sale) {
        val intent = Intent(this, ComptantSaleActivity::class.java).apply {
            putExtra("SALE_ID", sale.saleId?.id ?: sale.id)
            putExtra("SALE_DATE", sale.saleId?.saleDate ?: "")
            putExtra("IS_EDIT_MODE", false)
        }
        startActivity(intent)
    }

    /**
     * Edit sale
     */
    private fun editSale(sale: Sale) {
        val intent = Intent(this, ComptantSaleActivity::class.java).apply {
            putExtra("SALE_ID", sale.saleId?.id ?: sale.id)
            putExtra("SALE_DATE", sale.saleId?.saleDate ?: "")
            putExtra("IS_EDIT_MODE", true)
        }
        startActivity(intent)
    }

    /**
     * Confirm delete sale
     */
    private fun confirmDeleteSale(sale: Sale) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Supprimer la vente")
            .setMessage("Voulez-vous vraiment supprimer cette vente ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteOngoingSale(sale.saleId?.id ?: sale.id)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Open new comptant sale
     */
    private fun openComptantSale() {
        val intent = Intent(this, ComptantSaleActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh sales list when returning to this activity
        viewModel.refresh()
    }

    // Note: Logout menu is inherited from BaseActivity
    // All authenticated activities automatically get the logout option
}
