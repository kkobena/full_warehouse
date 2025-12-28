package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import android.view.Menu
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.CashSummary
import com.kobe.warehouse.reports.databinding.ActivityCashSummaryBinding
import com.kobe.warehouse.reports.ui.adapter.CashierRecapAdapter
import com.kobe.warehouse.reports.ui.adapter.SummaryItemAdapter
import com.kobe.warehouse.reports.ui.viewmodel.CashSummaryViewModel
import com.kobe.warehouse.reports.ui.viewmodel.CashSummaryViewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cash Summary Activity - displays Ticket Z / Récapitulatif Caisse report.
 * Extends BaseActivity for session management and logout functionality.
 */
class CashSummaryActivity : BaseActivity() {

    private lateinit var binding: ActivityCashSummaryBinding
    private lateinit var viewModel: CashSummaryViewModel
    private lateinit var globalSummaryAdapter: SummaryItemAdapter
    private lateinit var cashierRecapsAdapter: CashierRecapAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupPeriodSelector()
        setupRecyclerViews()
        setupListeners()
        observeViewModel()

        viewModel.loadCashSummary()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = CashSummaryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CashSummaryViewModel::class.java]
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
                R.id.chipToday -> viewModel.setPeriod(CashSummaryViewModel.Period.TODAY)
                R.id.chipYesterday -> viewModel.setPeriod(CashSummaryViewModel.Period.YESTERDAY)
                R.id.chipThisWeek -> viewModel.setPeriod(CashSummaryViewModel.Period.THIS_WEEK)
                R.id.chipThisMonth -> viewModel.setPeriod(CashSummaryViewModel.Period.THIS_MONTH)
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
            // Reset to today if cancelled
            binding.chipToday.isChecked = true
        }

        picker.show(supportFragmentManager, "date_range_picker")
    }

    private fun setupRecyclerViews() {
        // Global summary adapter
        globalSummaryAdapter = SummaryItemAdapter()
        binding.rvGlobalSummary.apply {
            layoutManager = LinearLayoutManager(this@CashSummaryActivity)
            adapter = globalSummaryAdapter
            isNestedScrollingEnabled = false
        }

        // Cashier recaps adapter
        cashierRecapsAdapter = CashierRecapAdapter()
        binding.rvCashierRecaps.apply {
            layoutManager = LinearLayoutManager(this@CashSummaryActivity)
            adapter = cashierRecapsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCashSummary()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)

        binding.switchOnlyVente.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setOnlyVente(isChecked)
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

        viewModel.cashSummary.observe(this) { summary ->
            summary?.let {
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

    private fun updateUI(summary: CashSummary) {
        // Period label
        binding.tvPeriodLabel.text = summary.periodLabel

        // Global summary
        if (summary.globalSummary.isNotEmpty()) {
            binding.cardGlobalSummary.isVisible = true
            globalSummaryAdapter.submitList(summary.globalSummary)
        } else {
            binding.cardGlobalSummary.isVisible = false
        }

        // Payment totals
        binding.tvTotalEspeces.text = summary.getFormattedTotalEspeces()
        binding.tvTotalCartes.text = summary.getFormattedTotalCartes()
        binding.tvTotalMobileMoney.text = summary.getFormattedTotalMobileMoney()
        binding.tvTotalCheques.text = summary.getFormattedTotalCheques()
        binding.tvTotalVirements.text = summary.getFormattedTotalVirements()
        binding.tvTotalCredit.text = summary.getFormattedTotalCredit()
        binding.tvTotalTtc.text = summary.getFormattedTotalTtc()

        // Cashier count
        binding.tvCashierCount.text = resources.getQuantityString(
            R.plurals.cashier_count,
            summary.cashierCount,
            summary.cashierCount
        )

        // Cashier recaps
        cashierRecapsAdapter.submitList(summary.cashierRecaps)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshCashSummary()
            }
            .show()
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }
}
