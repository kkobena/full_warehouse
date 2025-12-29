package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivityStockRotationBinding
import com.kobe.warehouse.reports.ui.adapter.PaginationScrollListener
import com.kobe.warehouse.reports.ui.adapter.StockRotationAdapter
import com.kobe.warehouse.reports.ui.viewmodel.StockRotationViewModel
import com.kobe.warehouse.reports.ui.viewmodel.StockRotationViewModelFactory

class StockRotationActivity : BaseActivity() {

    private lateinit var binding: ActivityStockRotationBinding
    private lateinit var viewModel: StockRotationViewModel
    private lateinit var adapter: StockRotationAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockRotationBinding.inflate(layoutInflater)
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
        val factory = StockRotationViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StockRotationViewModel::class.java]
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
                R.id.chipFilterAll -> viewModel.setFilter(StockRotationViewModel.FilterType.ALL)
                R.id.chipFilterA -> viewModel.setFilter(StockRotationViewModel.FilterType.CLASS_A)
                R.id.chipFilterB -> viewModel.setFilter(StockRotationViewModel.FilterType.CLASS_B)
                R.id.chipFilterC -> viewModel.setFilter(StockRotationViewModel.FilterType.CLASS_C)
                R.id.chipFilterSlow -> viewModel.setFilter(StockRotationViewModel.FilterType.SLOW_MOVING)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StockRotationAdapter()
        layoutManager = LinearLayoutManager(this)

        binding.rvProducts.apply {
            this.layoutManager = this@StockRotationActivity.layoutManager
            adapter = this@StockRotationActivity.adapter
            isNestedScrollingEnabled = false

            // Add pagination scroll listener
            addOnScrollListener(object : PaginationScrollListener(this@StockRotationActivity.layoutManager) {
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

        viewModel.countA.observe(this) { count ->
            binding.tvCountA.text = count.toString()
        }

        viewModel.countB.observe(this) { count ->
            binding.tvCountB.text = count.toString()
        }

        viewModel.countC.observe(this) { count ->
            binding.tvCountC.text = count.toString()
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
        }

        viewModel.pagination.observe(this) { _ ->
            // Pagination info available if needed for UI updates
        }
    }
}
