package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
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

/**
 * Performance activity - displays performance analytics.
 */
class PerformanceActivity : AppCompatActivity() {

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
        // Line Chart (CA Trend)
        binding.chartCATrend.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(this@PerformanceActivity, R.color.text_secondary)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textColor = ContextCompat.getColor(this@PerformanceActivity, R.color.text_secondary)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatLargeNumber(value.toLong())
                    }
                }
            }

            axisRight.isEnabled = false
            animateX(500)
        }

        // Pie Chart (Categories)
        binding.chartCategories.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            legend.isEnabled = true
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 50f
            animateY(500)
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPerformance()
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

        viewModel.performance.observe(this) { performance ->
            updateUI(performance)
        }
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
            binding.chartCATrend.clear()
            return
        }

        val entries = dataPoints.mapIndexed { index, point ->
            Entry(index.toFloat(), point.caAmount.toFloat())
        }

        val labels = dataPoints.map { it.label }

        val dataSet = LineDataSet(entries, "CA").apply {
            color = ContextCompat.getColor(this@PerformanceActivity, R.color.primary)
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(this@PerformanceActivity, R.color.primary))
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@PerformanceActivity, R.color.primary_light)
            fillAlpha = 50
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
            data = LineData(dataSet)
            invalidate()
        }
    }

    private fun updatePieChart(paymentMethods: List<com.kobe.warehouse.reports.data.model.PaymentMethodSummary>) {
        if (paymentMethods.isEmpty()) {
            binding.chartCategories.clear()
            return
        }

        val entries = paymentMethods.map { method ->
            PieEntry(method.percent.toFloat(), method.label)
        }

        val colors = paymentMethods.mapNotNull { method ->
            try {
                Color.parseColor(method.color)
            } catch (e: Exception) {
                ColorTemplate.MATERIAL_COLORS[paymentMethods.indexOf(method) % ColorTemplate.MATERIAL_COLORS.size]
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(binding.chartCategories)
        }

        binding.chartCategories.apply {
            data = PieData(dataSet)
            invalidate()
        }
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
