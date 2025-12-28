package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.Dashboard
import com.kobe.warehouse.reports.data.model.DailyCASummary
import com.kobe.warehouse.reports.databinding.ActivityDashboardBinding
import com.kobe.warehouse.reports.service.PharmaFirebaseMessagingService
import com.kobe.warehouse.reports.ui.adapter.AlertSummaryAdapter
import com.kobe.warehouse.reports.ui.adapter.TopProductAdapter
import com.kobe.warehouse.reports.ui.viewmodel.DashboardViewModel
import com.kobe.warehouse.reports.ui.viewmodel.DashboardViewModelFactory

/**
 * Dashboard activity - main screen showing KPIs and summaries.
 * Extends BaseActivity for session management and logout functionality.
 */
class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel
    private lateinit var alertsAdapter: AlertSummaryAdapter
    private lateinit var topProductsAdapter: TopProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupRecyclerViews()
        setupChart()
        setupListeners()
        observeViewModel()

        // Reset badge count when dashboard opens
        PharmaFirebaseMessagingService.resetBadgeCount(this)

        // Load initial data
        viewModel.loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        // Reset badge count when returning to dashboard
        PharmaFirebaseMessagingService.resetBadgeCount(this)
    }

    /**
     * Setup ViewModel with dependencies.
     */
    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = DashboardViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Setup toolbar.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    viewModel.refreshDashboard()
                    true
                }
                R.id.action_settings -> {
                    navigateToSettings()
                    true
                }
                R.id.action_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Setup RecyclerViews for alerts and top products.
     */
    private fun setupRecyclerViews() {
        // Alerts RecyclerView
        alertsAdapter = AlertSummaryAdapter { alert ->
            navigateToAlerts(alert.type)
        }
        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = alertsAdapter
            isNestedScrollingEnabled = false
        }

        // Top Products RecyclerView
        topProductsAdapter = TopProductAdapter { product ->
            navigateToProductDetail(product.id)
        }
        binding.rvTopProducts.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = topProductsAdapter
            isNestedScrollingEnabled = false
        }
    }

    /**
     * Setup bar chart for CA trend.
     */
    private fun setupChart() {
        binding.chartCATrend.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setFitBars(true)
            setTouchEnabled(true)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false

            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(this@DashboardActivity, R.color.text_secondary)
            }

            // Left Y Axis
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textColor = ContextCompat.getColor(this@DashboardActivity, R.color.text_secondary)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatLargeNumber(value.toLong())
                    }
                }
            }

            // Right Y Axis
            axisRight.isEnabled = false

            // Animation
            animateY(500)
        }
    }

    /**
     * Setup click listeners.
     */
    private fun setupListeners() {
        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshDashboard()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)

        // Alerts card click
        binding.cardAlerts.setOnClickListener {
            navigateToAlerts()
        }

        // See all products button
        binding.btnSeeAllProducts.setOnClickListener {
            navigateToProducts()
        }

        // Report cards
        binding.cardPharmacistDashboard.setOnClickListener {
            navigateToPharmacistDashboard()
        }
        binding.cardCashSummary.setOnClickListener {
            navigateToCashSummary()
        }
        binding.cardActivityReport.setOnClickListener {
            navigateToActivityReport()
        }
        binding.cardCashBalance.setOnClickListener {
            navigateToCashBalance()
        }
        binding.cardTvaReport.setOnClickListener {
            navigateToTvaReport()
        }

        // Bottom navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true // Already on dashboard
                R.id.nav_alerts -> {
                    navigateToAlerts()
                    true
                }
                R.id.nav_todos -> {
                    navigateToTodos()
                    true
                }
                R.id.nav_performance -> {
                    navigateToPerformance()
                    true
                }
                else -> false
            }
        }

        // Set dashboard as selected in bottom nav
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
    }

    /**
     * Observe ViewModel LiveData.
     */
    private fun observeViewModel() {
        // Loading state - use ViewFlipper for skeleton loading
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showSkeleton()
            }
            // Hide loadingOverlay (kept for compatibility)
            binding.loadingOverlay.isVisible = false
        }

        // Refreshing state
        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        // Error message
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                // If we have no data, show error state
                if (viewModel.dashboard.value == null) {
                    showErrorState(it)
                } else {
                    // We have data, just show snackbar
                    showErrorSnackbar(it)
                }
                viewModel.clearError()
            }
        }

        // Dashboard data
        viewModel.dashboard.observe(this) { dashboard ->
            showContent()
            updateUI(dashboard)
        }
    }

    /**
     * Show skeleton loading view.
     */
    private fun showSkeleton() {
        binding.viewFlipper.displayedChild = VIEW_SKELETON
    }

    /**
     * Show actual content view.
     */
    private fun showContent() {
        binding.viewFlipper.displayedChild = VIEW_CONTENT
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
        private const val VIEW_ERROR = 2
    }

    /**
     * Update UI with dashboard data.
     */
    private fun updateUI(dashboard: Dashboard) {
        // CA section
        binding.tvDailyCA.text = dashboard.getFormattedDailyCA()

        // Variation
        val variationPercent = dashboard.variationPercent
        val isPositive = variationPercent >= 0
        binding.tvVariationIndicator.text = dashboard.getVariationIndicator()
        binding.tvVariation.text = String.format("%+.1f%%", variationPercent)

        val variationColor = if (isPositive) R.color.success else R.color.error
        binding.tvVariationIndicator.setTextColor(ContextCompat.getColor(this, variationColor))
        binding.tvVariation.setTextColor(ContextCompat.getColor(this, variationColor))

        val variationBackground = if (isPositive) {
            R.drawable.bg_variation_positive
        } else {
            R.drawable.bg_variation_negative
        }
        binding.llVariation.setBackgroundResource(variationBackground)

        // Target progress
        binding.tvTarget.text = getString(R.string.dashboard_target_format, Dashboard.formatAmount(dashboard.dailyTarget))
        binding.progressTarget.progress = dashboard.progressPercent
        binding.tvProgressPercent.text = "${dashboard.progressPercent}%"

        // Stats cards
        binding.tvTransactions.text = dashboard.transactionsCount.toString()
        binding.tvAverageBasket.text = dashboard.getFormattedAverageBasket()

        // Alerts
        binding.chipAlertsCount.text = dashboard.alertsCount.toString()
        alertsAdapter.submitList(dashboard.alerts)

        // Top products
        topProductsAdapter.submitList(dashboard.topProducts)

        // CA Trend chart
        updateChart(dashboard.caTrend)
    }

    /**
     * Update chart with CA trend data.
     */
    private fun updateChart(caTrend: List<DailyCASummary>) {
        if (caTrend.isEmpty()) {
            binding.chartCATrend.clear()
            return
        }

        val entries = caTrend.mapIndexed { index, summary ->
            BarEntry(index.toFloat(), summary.caTotal.toFloat())
        }

        val labels = caTrend.map { it.dayLabel }

        val dataSet = BarDataSet(entries, "CA").apply {
            color = ContextCompat.getColor(this@DashboardActivity, R.color.primary)
            valueTextColor = ContextCompat.getColor(this@DashboardActivity, R.color.text_secondary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatLargeNumber(value.toLong())
                }
            }
        }

        binding.chartCATrend.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            data = BarData(dataSet).apply {
                barWidth = 0.7f
            }
            invalidate()
        }
    }

    /**
     * Format large numbers (e.g., 2500000 -> "2.5M").
     */
    private fun formatLargeNumber(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.0fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    /**
     * Show error state view.
     */
    private fun showErrorState(message: String) {
        binding.viewFlipper.displayedChild = VIEW_ERROR
        binding.tvErrorMessage.text = message
        binding.btnRetry.setOnClickListener {
            viewModel.loadDashboard()
        }
    }

    /**
     * Show error as snackbar (when we have data).
     */
    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.loadDashboard()
            }
            .show()
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    private fun navigateToAlerts(filterType: String? = null) {
        val intent = Intent(this, AlertsActivity::class.java).apply {
            filterType?.let { putExtra(AlertsActivity.EXTRA_FILTER_TYPE, it) }
        }
        startActivity(intent)
    }

    private fun navigateToProducts() {
        val intent = Intent(this, ProductSearchActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToProductDetail(productId: Long) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId)
        }
        startActivity(intent)
    }

    private fun navigateToTodos() {
        val intent = Intent(this, TodosActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPerformance() {
        val intent = Intent(this, PerformanceActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPharmacistDashboard() {
        val intent = Intent(this, PharmacistDashboardActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCashSummary() {
        val intent = Intent(this, CashSummaryActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToActivityReport() {
        val intent = Intent(this, ActivityReportActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCashBalance() {
        val intent = Intent(this, CashBalanceActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToTvaReport() {
        val intent = Intent(this, TvaReportActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSettings() {
        val intent = Intent(this, ServerConfigActivity::class.java)
        startActivity(intent)
    }
}
