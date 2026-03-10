package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivitySoldProductsBinding
import com.kobe.warehouse.reports.ui.adapter.PaginationScrollListener
import com.kobe.warehouse.reports.ui.adapter.SoldProductsAdapter
import com.kobe.warehouse.reports.ui.viewmodel.SoldProductsViewModel
import com.kobe.warehouse.reports.ui.viewmodel.SoldProductsViewModelFactory
import com.kobe.warehouse.reports.utils.NumberFormatUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SoldProductsActivity : BaseActivity() {

    private lateinit var binding: ActivitySoldProductsBinding
    private lateinit var viewModel: SoldProductsViewModel
    private lateinit var adapter: SoldProductsAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoldProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupPeriodChips()
        setupListeners()
        observeViewModel()

        viewModel.loadData()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = SoldProductsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SoldProductsViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = SoldProductsAdapter()
        layoutManager = LinearLayoutManager(this)
        binding.rvProducts.apply {
            this.layoutManager = this@SoldProductsActivity.layoutManager
            adapter = this@SoldProductsActivity.adapter
            isNestedScrollingEnabled = false
            addOnScrollListener(object : PaginationScrollListener(this@SoldProductsActivity.layoutManager) {
                override fun loadMoreItems() { viewModel.loadMore() }
                override fun isLoading() = viewModel.isLoadingMore.value == true
                override fun isLastPage() = viewModel.canLoadMore.value == false
            })
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.onSearchChanged(s?.toString() ?: "")
            }
        })
    }

    private fun setupPeriodChips() {
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            val today = LocalDate.now()
            when {
                checkedIds.contains(R.id.chipToday) ->
                    viewModel.applyDateFilter(today, today)
                checkedIds.contains(R.id.chipYesterday) ->
                    viewModel.applyDateFilter(today.minusDays(1), today.minusDays(1))
                checkedIds.contains(R.id.chipThisWeek) ->
                    viewModel.applyDateFilter(today.with(java.time.DayOfWeek.MONDAY), today)
                checkedIds.contains(R.id.chipThisMonth) ->
                    viewModel.applyDateFilter(today.withDayOfMonth(1), today)
                checkedIds.contains(R.id.chipCustom) ->
                    showDateRangePicker()
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.period_custom)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val start = Instant.ofEpochMilli(selection.first)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val end = Instant.ofEpochMilli(selection.second)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            viewModel.applyDateFilter(start, end)
        }
        picker.show(supportFragmentManager, "date_range_picker")
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshData() }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            if (loading) binding.viewFlipper.displayedChild = 0
        }

        viewModel.isRefreshing.observe(this) { refreshing ->
            binding.swipeRefresh.isRefreshing = refreshing
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

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
            binding.rvProducts.isVisible = !isEmpty
        }

        viewModel.summary.observe(this) { summary ->
            summary?.let {
                binding.tvTotalProducts.text = it.totalProducts.toString()
                binding.tvTotalQty.text = it.getNetQuantity().toString()
                binding.tvTotalSales.text = NumberFormatUtils.formatCompact(it.totalSalesAmount)
            }
        }
    }
}
