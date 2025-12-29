package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivityTiersPayantCreancesBinding
import com.kobe.warehouse.reports.ui.adapter.TiersPayantInvoiceAdapter
import com.kobe.warehouse.reports.ui.adapter.TiersPayantSummaryAdapter
import com.kobe.warehouse.reports.ui.viewmodel.TiersPayantCreancesViewModel
import com.kobe.warehouse.reports.ui.viewmodel.TiersPayantCreancesViewModelFactory
import com.kobe.warehouse.reports.utils.NumberFormatUtils

/**
 * Tiers Payant Créances Activity - displays receivables from third-party payers.
 * Extends BaseActivity for session management and logout functionality.
 */
class TiersPayantCreancesActivity : BaseActivity() {

    private lateinit var binding: ActivityTiersPayantCreancesBinding
    private lateinit var viewModel: TiersPayantCreancesViewModel
    private lateinit var summaryAdapter: TiersPayantSummaryAdapter
    private lateinit var invoiceAdapter: TiersPayantInvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiersPayantCreancesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupViewModeSelector()
        setupFilterChips()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadCreances()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = TiersPayantCreancesViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TiersPayantCreancesViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupViewModeSelector() {
        binding.chipGroupViewMode.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            when (checkedIds.first()) {
                R.id.chipSummary -> {
                    viewModel.setViewMode(TiersPayantCreancesViewModel.ViewMode.SUMMARY)
                    binding.filterChipsContainer.isVisible = false
                }
                R.id.chipInvoices -> {
                    viewModel.setViewMode(TiersPayantCreancesViewModel.ViewMode.INVOICES)
                    binding.filterChipsContainer.isVisible = true
                }
            }
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                viewModel.setAgeCategoryFilter(null)
                return@setOnCheckedStateChangeListener
            }

            when (checkedIds.first()) {
                R.id.chipFilterAll -> viewModel.setAgeCategoryFilter(null)
                R.id.chipFilterLess30 -> viewModel.setAgeCategoryFilter("LESS_THAN_30")
                R.id.chipFilter30_60 -> viewModel.setAgeCategoryFilter("BETWEEN_30_60")
                R.id.chipFilter60_90 -> viewModel.setAgeCategoryFilter("BETWEEN_60_90")
                R.id.chipFilterMore90 -> viewModel.setAgeCategoryFilter("MORE_THAN_90")
            }
        }
    }

    private fun setupRecyclerView() {
        // Summary adapter with click handler
        summaryAdapter = TiersPayantSummaryAdapter { summary ->
            viewModel.viewInvoicesForGroupe(summary.groupeTiersPayantId)
            binding.chipInvoices.isChecked = true
        }

        // Invoice adapter
        invoiceAdapter = TiersPayantInvoiceAdapter()

        binding.rvContent.apply {
            layoutManager = LinearLayoutManager(this@TiersPayantCreancesActivity)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCreances()
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

        viewModel.viewMode.observe(this) { mode ->
            when (mode) {
                TiersPayantCreancesViewModel.ViewMode.SUMMARY -> {
                    binding.rvContent.adapter = summaryAdapter
                }
                TiersPayantCreancesViewModel.ViewMode.INVOICES -> {
                    binding.rvContent.adapter = invoiceAdapter
                }
                null -> {}
            }
        }

        viewModel.summaries.observe(this) { summaries ->
            if (viewModel.viewMode.value == TiersPayantCreancesViewModel.ViewMode.SUMMARY) {
                showContent()
                summaryAdapter.submitList(summaries)
            }
        }

        viewModel.invoices.observe(this) { invoices ->
            if (viewModel.viewMode.value == TiersPayantCreancesViewModel.ViewMode.INVOICES) {
                showContent()
                invoiceAdapter.submitList(invoices)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
        }

        viewModel.totalMontantRestant.observe(this) { total ->
            binding.tvTotalMontant.text = NumberFormatUtils.formatCurrency(total)
        }

        viewModel.totalFactures.observe(this) { count ->
            binding.tvTotalFactures.text = count.toString()
        }
    }

    private fun showSkeleton() {
        binding.viewFlipper.displayedChild = VIEW_SKELETON
    }

    private fun showContent() {
        binding.viewFlipper.displayedChild = VIEW_CONTENT
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshCreances()
            }
            .show()
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }
}
