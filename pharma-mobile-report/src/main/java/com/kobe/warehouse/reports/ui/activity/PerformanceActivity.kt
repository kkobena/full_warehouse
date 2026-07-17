package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.Dashboard
import com.kobe.warehouse.reports.data.model.Performance
import com.kobe.warehouse.reports.data.model.PeriodDataPoint
import com.kobe.warehouse.reports.data.model.TopProduct
import com.kobe.warehouse.reports.databinding.ActivityPerformanceBinding
import com.kobe.warehouse.reports.ui.adapter.TopProductAdapter
import com.kobe.warehouse.reports.ui.viewmodel.PerformanceViewModel
import com.kobe.warehouse.reports.ui.viewmodel.PerformanceViewModelFactory
import com.kobe.warehouse.reports.ui.widget.SimpleLineChartView
import com.kobe.warehouse.reports.ui.widget.SimplePieChartView

/**
 * Performance activity - displays performance analytics.
 * Extends BaseActivity for session management and logout functionality.
 */
class PerformanceActivity : BaseActivity() {

    private lateinit var binding: ActivityPerformanceBinding
    private lateinit var viewModel: PerformanceViewModel
    private lateinit var topProductsAdapter: TopProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupTabs()
        setupRecyclerView()
        setupCharts()
        setupListeners()
        observeViewModel()

        viewModel.loadPerformance()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = PerformanceViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PerformanceViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTabs() {
        binding.tabPeriod.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val period = when (tab?.position) {
                    0 -> Performance.PERIOD_WEEK
                    1 -> Performance.PERIOD_MONTH
                    2 -> Performance.PERIOD_YEAR
                    else -> Performance.PERIOD_WEEK
                }
                viewModel.setPeriod(period)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        topProductsAdapter = TopProductAdapter { product ->
            navigateToProductDetail(product.id)
        }
        binding.rvTopProducts.apply {
            layoutManager = LinearLayoutManager(this@PerformanceActivity)
            adapter = topProductsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupCharts() {
        binding.chartCATrend.clearChart()
        binding.chartCategories.setLegendEnabled(true)
        binding.chartCategories.clearChart()
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPerformance()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showSkeleton()
            }
            // Hide loadingOverlay (kept for compatibility)
            binding.loadingOverlay.isVisible = false
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showContent()
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.performance.observe(this) { performance ->
            showContent()
            updateUI(performance)
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
    }

    private fun updateUI(performance: Performance) {
        // Main metrics
        binding.tvCATotal.text = performance.getFormattedCATotal()

        // Variation
        val variationText = String.format("%+.1f%% vs période précédente", performance.variationPercent)
        binding.tvVariation.text = variationText
        val variationColor = if (performance.isVariationPositive()) R.color.success else R.color.error
        binding.tvVariation.setTextColor(ContextCompat.getColor(this, variationColor))
        binding.llVariation.setBackgroundResource(
            if (performance.isVariationPositive()) R.drawable.bg_variation_positive
            else R.drawable.bg_variation_negative
        )

        // Secondary metrics
        binding.tvTransactions.text = performance.transactionsCount.toString()
        binding.tvAverageBasket.text = Dashboard.formatAmount(performance.averageBasket)
        binding.tvMargin.text = String.format("%.1f%%", performance.marginPercent)

        // Charts
        updateLineChart(performance.dataPoints)
        updatePieChart(performance.paymentMethods)

        // Top products - convert to TopProduct format
        val topProducts = performance.topProducts.map { perf ->
            TopProduct(
                id = perf.productId,
                name = perf.productName,
                codeCip = perf.codeCip,
                salesAmount = perf.salesAmount,
                quantitySold = perf.quantitySold,
                rank = perf.rank
            )
        }
        topProductsAdapter.submitList(topProducts)
    }

    private fun updateLineChart(dataPoints: List<PeriodDataPoint>) {
        if (dataPoints.isEmpty()) {
            binding.chartCATrend.clearChart()
            return
        }
        binding.chartCATrend.setPoints(
            dataPoints.map {
                SimpleLineChartView.Point(
                    label = it.label.take(4),
                    value = it.caAmount.toFloat()
                )
            }
        )
    }

    private fun updatePieChart(paymentMethods: List<com.kobe.warehouse.reports.data.model.PaymentMethodSummary>) {
        if (paymentMethods.isEmpty()) {
            binding.chartCategories.clearChart()
            return
        }
        binding.chartCategories.setSlices(
            paymentMethods.mapIndexed { index, method ->
                SimplePieChartView.Slice(
                    label = method.label,
                    value = method.amount.toFloat(),
                    color = paletteColor(index)
                )
            }
        )
    }

    private fun paletteColor(index: Int): Int {
        val palette = intArrayOf(
            Color.parseColor("#1976D2"),
            Color.parseColor("#2E7D32"),
            Color.parseColor("#F57C00"),
            Color.parseColor("#7B1FA2"),
            Color.parseColor("#C62828"),
            Color.parseColor("#00838F")
        )
        return palette[index % palette.size]
    }

    private fun formatLargeNumber(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.0fK", value / 1_000.0)
            else -> value.toString()
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
                viewModel.refreshPerformance()
            }
            .show()
    }
}
