package com.kobe.warehouse.reports.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.ChartDataPoint
import com.kobe.warehouse.reports.data.model.PharmacistDashboard
import com.kobe.warehouse.reports.databinding.ActivityPharmacistDashboardBinding
import com.kobe.warehouse.reports.ui.adapter.SupplierPurchaseAdapter
import com.kobe.warehouse.reports.ui.viewmodel.PharmacistDashboardViewModel
import com.kobe.warehouse.reports.ui.viewmodel.PharmacistDashboardViewModelFactory
import com.kobe.warehouse.reports.ui.widget.SimpleBarChartView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pharmacist Dashboard activity - displays Tableau Pharmacien report.
 * Extends BaseActivity for session management and logout functionality.
 */
class PharmacistDashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityPharmacistDashboardBinding
    private lateinit var viewModel: PharmacistDashboardViewModel
    private lateinit var suppliersAdapter: SupplierPurchaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPharmacistDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupPeriodSelector()
        setupRecyclerView()
        setupChart()
        setupListeners()
        observeViewModel()

        viewModel.loadDashboard()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = PharmacistDashboardViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PharmacistDashboardViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupPeriodSelector() {
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            when (checkedIds.first()) {
                R.id.chipToday -> viewModel.setPeriod(PharmacistDashboardViewModel.Period.TODAY)
                R.id.chipYesterday -> viewModel.setPeriod(PharmacistDashboardViewModel.Period.YESTERDAY)
                R.id.chipThisWeek -> viewModel.setPeriod(PharmacistDashboardViewModel.Period.THIS_WEEK)
                R.id.chipThisMonth -> viewModel.setPeriod(PharmacistDashboardViewModel.Period.THIS_MONTH)
                R.id.chipCustom -> showDateRangePicker()
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Sélectionner une période")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val fromDate = Instant.ofEpochMilli(selection.first)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val toDate = Instant.ofEpochMilli(selection.second)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            viewModel.setDateRange(fromDate, toDate)
        }

        picker.addOnNegativeButtonClickListener {
            // Reset to today if cancelled
            binding.chipToday.isChecked = true
        }

        picker.show(supportFragmentManager, "date_range_picker")
    }

    private fun setupRecyclerView() {
        suppliersAdapter = SupplierPurchaseAdapter()
        binding.rvTopSuppliers.apply {
            layoutManager = LinearLayoutManager(this@PharmacistDashboardActivity)
            adapter = suppliersAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupChart() {
        binding.chartVentesAchats.clearChart()
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshDashboard()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showSkeleton()
            }
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

        viewModel.dashboard.observe(this) { dashboard ->
            dashboard?.let {
                showContent()
                updateUI(it)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
        }
    }

    private fun showSkeleton() {
        binding.viewFlipper.displayedChild = VIEW_SKELETON
    }

    private fun showContent() {
        binding.viewFlipper.displayedChild = VIEW_CONTENT
    }

    private fun updateUI(dashboard: PharmacistDashboard) {
        // Period label
        binding.tvPeriodLabel.text = dashboard.periodLabel

        // Sales
        binding.tvVentesNet.text = dashboard.getFormattedVentesNet()
        binding.tvVentesComptant.text = dashboard.formatAmount(dashboard.montantVenteComptant)
        binding.tvVentesCredit.text = dashboard.formatAmount(dashboard.montantVenteCredit)
        binding.tvVentesRemise.text = "-${dashboard.formatAmount(dashboard.montantVenteRemise)}"
        binding.tvTransactionsCount.text = getString(R.string.transactions_count, dashboard.transactionsCount)

        // Sales variation
        binding.tvVentesVariation.text = dashboard.getFormattedVentesVariation()
        val salesVariationColor = if (dashboard.isVentesVariationPositive()) R.color.success else R.color.error
        binding.tvVentesVariation.setTextColor(ContextCompat.getColor(this, salesVariationColor))

        // Purchases
        binding.tvAchatsNet.text = dashboard.getFormattedAchatsNet()
        binding.tvAvoirsFournisseur.text = dashboard.formatAmount(dashboard.montantAvoirFournisseur)
        binding.tvAchatsVariation.text = dashboard.getFormattedAchatsVariation()

        // Margin
        binding.tvMarge.text = dashboard.getFormattedMarge()
        binding.tvMargePercent.text = dashboard.getFormattedMargePercent()

        // Set margin color based on health
        val margeColor = if (dashboard.isMarginHealthy()) R.color.success else R.color.error
        binding.tvMarge.setTextColor(ContextCompat.getColor(this, margeColor))

        // Ratios
        binding.tvRatioVA.text = dashboard.getFormattedRatioVenteAchat()
        binding.tvRatioAV.text = "A/V: ${dashboard.getFormattedRatioAchatVente()}"

        // Chart
        updateChart(dashboard.chartVentesAchats)

        // Top suppliers
        suppliersAdapter.submitList(dashboard.topFournisseurs)
    }

    private fun updateChart(dataPoints: List<ChartDataPoint>) {
        if (dataPoints.isEmpty()) {
            binding.chartVentesAchats.clearChart()
            return
        }
        binding.chartVentesAchats.setBars(
            dataPoints.map { point ->
                SimpleBarChartView.BarItem(
                    label = point.label.take(4),
                    value = point.value.toFloat(),
                    color = resolveColor(point)
                )
            }
        )
    }

    private fun resolveColor(point: ChartDataPoint): Int {
        point.color?.let {
            try {
                return Color.parseColor(it)
            } catch (_: IllegalArgumentException) {
            }
        }
        return if (point.isSales()) {
            ContextCompat.getColor(this, R.color.success)
        } else {
            ContextCompat.getColor(this, R.color.info)
        }
    }

    private fun formatLargeNumber(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.0fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshDashboard()
            }
            .show()
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }
}
