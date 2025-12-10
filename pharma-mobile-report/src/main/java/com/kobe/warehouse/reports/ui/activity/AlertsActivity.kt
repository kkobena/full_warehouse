package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.Alert
import com.kobe.warehouse.reports.databinding.ActivityAlertsBinding
import com.kobe.warehouse.reports.ui.adapter.AlertDetailAdapter
import com.kobe.warehouse.reports.ui.viewmodel.AlertsViewModel
import com.kobe.warehouse.reports.ui.viewmodel.AlertsViewModelFactory

/**
 * Alerts activity - displays all alerts with filtering.
 */
class AlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsBinding
    private lateinit var viewModel: AlertsViewModel
    private lateinit var alertsAdapter: AlertDetailAdapter

    companion object {
        const val EXTRA_FILTER_TYPE = "filter_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupListeners()
        observeViewModel()

        // Check for initial filter from intent
        intent.getStringExtra(EXTRA_FILTER_TYPE)?.let { filterType ->
            viewModel.setFilter(filterType)
            selectFilterChip(filterType)
        }

        // Load data
        viewModel.loadAlerts()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = AlertsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AlertsViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertDetailAdapter { alert ->
            handleAlertClick(alert)
        }
        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(this@AlertsActivity)
            adapter = alertsAdapter
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(R.id.chipStockRupture) -> Alert.TYPE_STOCK_RUPTURE
                checkedIds.contains(R.id.chipExpiry) -> Alert.TYPE_EXPIRY
                checkedIds.contains(R.id.chipCash) -> Alert.TYPE_CASH_DISCREPANCY
                checkedIds.contains(R.id.chipInvoice) -> Alert.TYPE_INVOICE_OVERDUE
                else -> null
            }
            viewModel.setFilter(filter)
        }
    }

    private fun selectFilterChip(filterType: String) {
        when (filterType) {
            Alert.TYPE_STOCK_RUPTURE -> binding.chipStockRupture.isChecked = true
            Alert.TYPE_EXPIRY -> binding.chipExpiry.isChecked = true
            Alert.TYPE_CASH_DISCREPANCY -> binding.chipCash.isChecked = true
            Alert.TYPE_INVOICE_OVERDUE -> binding.chipInvoice.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAlerts()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingOverlay.isVisible = isLoading
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.alerts.observe(this) { alerts ->
            alertsAdapter.submitList(alerts)
            updateEmptyState(alerts.isEmpty())
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.isVisible = isEmpty
        binding.rvAlerts.isVisible = !isEmpty
    }

    private fun handleAlertClick(alert: Alert) {
        // Navigate based on alert type
        when {
            alert.relatedEntityType == "PRODUCT" && alert.relatedEntityId != null -> {
                navigateToProductDetail(alert.relatedEntityId)
            }
            else -> {
                // Show alert details or perform action
            }
        }
    }

    private fun navigateToProductDetail(productId: Long) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId)
        }
        startActivity(intent)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.loadAlerts()
            }
            .show()
    }
}
