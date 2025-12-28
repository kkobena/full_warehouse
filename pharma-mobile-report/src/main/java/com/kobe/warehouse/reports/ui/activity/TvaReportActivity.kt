package com.kobe.warehouse.reports.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.TvaChartData
import com.kobe.warehouse.reports.data.model.TvaReport
import com.kobe.warehouse.reports.databinding.ActivityTvaReportBinding
import com.kobe.warehouse.reports.ui.adapter.TvaRateAdapter
import com.kobe.warehouse.reports.ui.viewmodel.TvaReportViewModel
import com.kobe.warehouse.reports.ui.viewmodel.TvaReportViewModelFactory
import java.time.Instant
import java.time.ZoneId

/**
 * TVA Report Activity - displays Rapport TVA.
 * Extends BaseActivity for session management and logout functionality.
 */
class TvaReportActivity : BaseActivity() {

    private lateinit var binding: ActivityTvaReportBinding
    private lateinit var viewModel: TvaReportViewModel
    private lateinit var tvaRateAdapter: TvaRateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvaReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupPeriodSelector()
        setupRecyclerViews()
        setupPieChart()
        setupExpandableCards()
        setupListeners()
        observeViewModel()

        viewModel.loadTvaReport()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = TvaReportViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TvaReportViewModel::class.java]
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
                R.id.chipToday -> viewModel.setPeriod(TvaReportViewModel.Period.TODAY)
                R.id.chipYesterday -> viewModel.setPeriod(TvaReportViewModel.Period.YESTERDAY)
                R.id.chipThisWeek -> viewModel.setPeriod(TvaReportViewModel.Period.THIS_WEEK)
                R.id.chipThisMonth -> viewModel.setPeriod(TvaReportViewModel.Period.THIS_MONTH)
                R.id.chipCustom -> showDateRangePicker()
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_period))
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
            binding.chipToday.isChecked = true
        }

        picker.show(supportFragmentManager, "date_range_picker")
    }

    private fun setupRecyclerViews() {
        tvaRateAdapter = TvaRateAdapter()
        binding.rvTvaBreakdown.apply {
            layoutManager = LinearLayoutManager(this@TvaReportActivity)
            adapter = tvaRateAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupPieChart() {
        binding.pieChartTva.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = getString(R.string.tva)
            setCenterTextSize(14f)
            setEntryLabelTextSize(10f)
            setEntryLabelColor(Color.DKGRAY)
            legend.isEnabled = false
            setNoDataText(getString(R.string.no_data))
        }
    }

    private fun setupExpandableCards() {
        binding.headerTotaux.setOnClickListener {
            viewModel.toggleTotaux()
        }

        binding.headerBreakdown.setOnClickListener {
            viewModel.toggleBreakdown()
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTvaReport()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)

        binding.switchGroupByDate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setGroupByDate(isChecked)
        }
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

        viewModel.tvaReport.observe(this) { report ->
            report?.let {
                showContent()
                updateUI(it)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
        }

        viewModel.groupByDate.observe(this) { groupByDate ->
            binding.switchGroupByDate.isChecked = groupByDate
        }

        // Expansion states
        viewModel.totauxExpanded.observe(this) { expanded ->
            binding.contentTotaux.isVisible = expanded
            binding.iconTotauxExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.breakdownExpanded.observe(this) { expanded ->
            binding.contentBreakdown.isVisible = expanded
            binding.iconBreakdownExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }
    }

    private fun showSkeleton() {
        binding.viewFlipper.displayedChild = VIEW_SKELETON
    }

    private fun showContent() {
        binding.viewFlipper.displayedChild = VIEW_CONTENT
    }

    private fun updateUI(report: TvaReport) {
        // Period label
        binding.tvPeriodLabel.text = report.periodLabel

        // Totaux header
        binding.tvTotauxHeader.text = report.getFormattedMontantTtc()

        // Totaux detail rows
        setRowData(binding.rowMontantHt.root, getString(R.string.montant_ht), report.getFormattedMontantHt())
        setRowData(binding.rowMontantTva.root, getString(R.string.montant_tva), report.getFormattedMontantTva())
        setRowData(binding.rowMontantTtc.root, getString(R.string.montant_ttc), report.getFormattedMontantTtc())
        setRowData(binding.rowMontantNet.root, getString(R.string.montant_net), report.getFormattedMontantNet())
        setRowData(binding.rowRemise.root, getString(R.string.remise), report.getFormattedMontantRemise())
        setRowData(binding.rowAmountToAccount.root, getString(R.string.amount_to_account), report.getFormattedAmountToBeTakenIntoAccount())

        // Update pie chart
        updatePieChart(report.chartData)

        // Update TVA breakdown list
        tvaRateAdapter.updateChartData(report.chartData, report.montantTtc)
        tvaRateAdapter.submitList(report.tvaBreakdown)
    }

    private fun updatePieChart(chartData: List<TvaChartData>) {
        if (chartData.isEmpty()) {
            binding.pieChartTva.setNoDataText(getString(R.string.no_data))
            binding.pieChartTva.invalidate()
            return
        }

        val entries = chartData.map { PieEntry(it.percent.toFloat(), it.label) }
        val colors = chartData.mapNotNull {
            try {
                Color.parseColor(it.color)
            } catch (e: Exception) {
                Color.GRAY
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(binding.pieChartTva)
            sliceSpace = 2f
        }

        binding.pieChartTva.apply {
            data = PieData(dataSet)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun setRowData(rowView: View, label: String, value: String) {
        rowView.findViewById<TextView>(R.id.tvLabel).text = label
        rowView.findViewById<TextView>(R.id.tvValue).text = value
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshTvaReport()
            }
            .show()
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }
}
