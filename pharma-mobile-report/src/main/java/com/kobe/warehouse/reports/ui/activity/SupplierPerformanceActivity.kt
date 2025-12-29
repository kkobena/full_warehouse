package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivitySupplierPerformanceBinding
import com.kobe.warehouse.reports.ui.adapter.SupplierPerformanceAdapter
import com.kobe.warehouse.reports.ui.viewmodel.SupplierPerformanceViewModel
import com.kobe.warehouse.reports.ui.viewmodel.SupplierPerformanceViewModelFactory
import com.kobe.warehouse.reports.utils.NumberFormatUtils

/**
 * Supplier Performance Activity - displays supplier performance metrics.
 * Extends BaseActivity for session management and logout functionality.
 */
class SupplierPerformanceActivity : BaseActivity() {

    private lateinit var binding: ActivitySupplierPerformanceBinding
    private lateinit var viewModel: SupplierPerformanceViewModel
    private lateinit var adapter: SupplierPerformanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupFilterChips()
        setupRecyclerView()
        setupExpandableCard()
        setupListeners()
        observeViewModel()

        viewModel.loadData()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = SupplierPerformanceViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SupplierPerformanceViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            when (checkedIds.first()) {
                R.id.chipFilterAll -> viewModel.setFilter(SupplierPerformanceViewModel.FilterType.ALL)
                R.id.chipFilterTop -> viewModel.setFilter(SupplierPerformanceViewModel.FilterType.TOP_10)
                R.id.chipFilterExcellent -> viewModel.setFilter(SupplierPerformanceViewModel.FilterType.EXCELLENT)
                R.id.chipFilterGood -> viewModel.setFilter(SupplierPerformanceViewModel.FilterType.GOOD)
                R.id.chipFilterIssues -> viewModel.setFilter(SupplierPerformanceViewModel.FilterType.ISSUES)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = SupplierPerformanceAdapter()
        binding.rvSuppliers.apply {
            layoutManager = LinearLayoutManager(this@SupplierPerformanceActivity)
            adapter = this@SupplierPerformanceActivity.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupExpandableCard() {
        binding.headerSummary.setOnClickListener {
            viewModel.toggleSummary()
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
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

        viewModel.suppliers.observe(this) { suppliers ->
            showContent()
            adapter.submitList(suppliers)
        }

        viewModel.summary.observe(this) { summary ->
            summary?.let {
                binding.tvTotalSuppliers.text = it.totalSuppliers.toString()
                binding.tvAvgScore.text = String.format("%.1f", it.avgPerformanceScore?.toFloat() ?: 0f)
                binding.tvTotalPurchases.text = NumberFormatUtils.formatCompact(it.totalPurchaseAmount12Months)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
        }

        viewModel.summaryExpanded.observe(this) { expanded ->
            binding.contentSummary.isVisible = expanded
            binding.iconSummaryExpand.setImageResource(
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

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshData()
            }
            .show()
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }
}
