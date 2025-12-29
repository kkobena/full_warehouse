package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivityAbcParetoBinding
import com.kobe.warehouse.reports.ui.adapter.AbcParetoAdapter
import com.kobe.warehouse.reports.ui.adapter.PaginationScrollListener
import com.kobe.warehouse.reports.ui.viewmodel.AbcParetoViewModel
import com.kobe.warehouse.reports.ui.viewmodel.AbcParetoViewModelFactory
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class AbcParetoActivity : BaseActivity() {

    private lateinit var binding: ActivityAbcParetoBinding
    private lateinit var viewModel: AbcParetoViewModel
    private lateinit var adapter: AbcParetoAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbcParetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupFilterChips()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadData()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = AbcParetoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AbcParetoViewModel::class.java]
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
                R.id.chipFilterAll -> viewModel.setFilter(AbcParetoViewModel.FilterType.ALL)
                R.id.chipFilterA -> viewModel.setFilter(AbcParetoViewModel.FilterType.CLASS_A)
                R.id.chipFilterB -> viewModel.setFilter(AbcParetoViewModel.FilterType.CLASS_B)
                R.id.chipFilterC -> viewModel.setFilter(AbcParetoViewModel.FilterType.CLASS_C)
                R.id.chipFilterTop -> viewModel.setFilter(AbcParetoViewModel.FilterType.TOP_20)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AbcParetoAdapter()
        layoutManager = LinearLayoutManager(this)

        binding.rvProducts.apply {
            this.layoutManager = this@AbcParetoActivity.layoutManager
            adapter = this@AbcParetoActivity.adapter
            isNestedScrollingEnabled = false

            // Add pagination scroll listener
            addOnScrollListener(object : PaginationScrollListener(this@AbcParetoActivity.layoutManager) {
                override fun loadMoreItems() {
                    viewModel.loadMore()
                }

                override fun isLoading(): Boolean {
                    return viewModel.isLoadingMore.value == true
                }

                override fun isLastPage(): Boolean {
                    return viewModel.canLoadMore.value == false
                }
            })
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)

        binding.headerSummary.setOnClickListener {
            viewModel.toggleSummary()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) binding.viewFlipper.displayedChild = 0
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.isLoadingMore.observe(this) { isLoadingMore ->
            adapter.setLoadingMore(isLoadingMore)
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                binding.viewFlipper.displayedChild = 1
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { viewModel.refreshData() }
                    .show()
                viewModel.clearError()
            }
        }

        viewModel.products.observe(this) { products ->
            binding.viewFlipper.displayedChild = 1
            adapter.submitList(products)
        }

        viewModel.summary.observe(this) { summary ->
            summary?.let {
                binding.tvTotalProducts.text = it.totalProducts.toString()
                binding.tvTotalRevenue.text = NumberFormatUtils.formatCompact(it.totalRevenue)
                binding.tvClassACount.text = it.classACount.toString()
                binding.tvClassARevenue.text = String.format("%.1f%%", it.classAPercentage?.toFloat() ?: 0f)
                binding.tvClassBCount.text = it.classBCount.toString()
                binding.tvClassCCount.text = it.classCCount.toString()
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

        viewModel.pagination.observe(this) { _ ->
            // Pagination info available if needed for UI updates
        }
    }
}
