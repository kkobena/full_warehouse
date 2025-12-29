package com.kobe.warehouse.reports.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivityProfitabilityBinding
import com.kobe.warehouse.reports.ui.adapter.ProfitabilityAdapter
import com.kobe.warehouse.reports.ui.viewmodel.ProfitabilityViewModel
import com.kobe.warehouse.reports.ui.viewmodel.ProfitabilityViewModelFactory
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class ProfitabilityActivity : BaseActivity() {

    private lateinit var binding: ActivityProfitabilityBinding
    private lateinit var viewModel: ProfitabilityViewModel
    private lateinit var adapter: ProfitabilityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfitabilityBinding.inflate(layoutInflater)
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
        val factory = ProfitabilityViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ProfitabilityViewModel::class.java]
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
                R.id.chipFilterAll -> viewModel.setFilter(ProfitabilityViewModel.FilterType.ALL)
                R.id.chipFilterStar -> viewModel.setFilter(ProfitabilityViewModel.FilterType.STAR)
                R.id.chipFilterCashCow -> viewModel.setFilter(ProfitabilityViewModel.FilterType.CASH_COW)
                R.id.chipFilterQuestion -> viewModel.setFilter(ProfitabilityViewModel.FilterType.QUESTION_MARK)
                R.id.chipFilterDog -> viewModel.setFilter(ProfitabilityViewModel.FilterType.DOG)
                R.id.chipFilterLowMargin -> viewModel.setFilter(ProfitabilityViewModel.FilterType.LOW_MARGIN)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProfitabilityAdapter()
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@ProfitabilityActivity)
            adapter = this@ProfitabilityActivity.adapter
            isNestedScrollingEnabled = false
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

    @SuppressLint("DefaultLocale")
    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) binding.viewFlipper.displayedChild = 0
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
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
                binding.tvTotalRevenue.text = NumberFormatUtils.formatCompact(it.totalRevenue)
                binding.tvTotalMargin.text = NumberFormatUtils.formatCompact(it.totalMargin)
                binding.tvAvgMargin.text = String.format("%.1f%%", it.avgMarginPercentage?.toFloat() ?: 0f)
                binding.tvStarCount.text = it.starCount.toString()
                binding.tvCashCowCount.text = it.cashCowCount.toString()
                binding.tvQuestionCount.text = it.questionMarkCount.toString()
                binding.tvDogCount.text = it.dogCount.toString()
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
}
