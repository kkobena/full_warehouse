package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.FamilleProduit
import com.kobe.warehouse.reports.data.model.Rayon
import com.kobe.warehouse.reports.databinding.ActivityStockValuationBinding
import com.kobe.warehouse.reports.ui.adapter.PaginationScrollListener
import com.kobe.warehouse.reports.ui.adapter.StockValuationAdapter
import com.kobe.warehouse.reports.ui.viewmodel.StockValuationViewModel
import com.kobe.warehouse.reports.ui.viewmodel.StockValuationViewModelFactory
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class StockValuationActivity : BaseActivity() {

    private lateinit var binding: ActivityStockValuationBinding
    private lateinit var viewModel: StockValuationViewModel
    private lateinit var adapter: StockValuationAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockValuationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadData()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = StockValuationViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StockValuationViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = StockValuationAdapter()
        layoutManager = LinearLayoutManager(this)

        binding.rvProducts.apply {
            this.layoutManager = this@StockValuationActivity.layoutManager
            adapter = this@StockValuationActivity.adapter
            isNestedScrollingEnabled = false

            addOnScrollListener(object : PaginationScrollListener(this@StockValuationActivity.layoutManager) {
                override fun loadMoreItems() { viewModel.loadMore() }
                override fun isLoading() = viewModel.isLoadingMore.value == true
                override fun isLastPage() = viewModel.canLoadMore.value == false
            })
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshData() }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)

        binding.headerSummary.setOnClickListener { viewModel.toggleSummary() }
    }

    private fun setupFamilleDropdown(familles: List<FamilleProduit>) {
        val items = mutableListOf(getString(R.string.filter_all)) + familles.map { it.libelle }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        binding.actvFamille.setAdapter(adapter)
        binding.actvFamille.setOnItemClickListener { _, _, position, _ ->
            viewModel.applyFamilleFilter(if (position == 0) null else familles[position - 1])
        }
    }

    private fun setupRayonDropdown(rayons: List<Rayon>) {
        val items = mutableListOf(getString(R.string.filter_all)) + rayons.map { it.libelle }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        binding.actvRayon.setAdapter(adapter)
        binding.actvRayon.setOnItemClickListener { _, _, position, _ ->
            viewModel.applyRayonFilter(if (position == 0) null else rayons[position - 1])
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
                binding.tvTotalPurchaseValue.text = NumberFormatUtils.formatCompact(it.totalPurchaseValue)
                binding.tvTotalSalesValue.text = NumberFormatUtils.formatCompact(it.totalSalesValue)
                binding.tvAvgMargin.text = String.format("%.1f%%", it.avgMarginPercentage?.toFloat() ?: 0f)
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

        viewModel.familleProduits.observe(this) { familles ->
            if (familles.isNotEmpty()) setupFamilleDropdown(familles)
        }

        viewModel.rayons.observe(this) { rayons ->
            if (rayons.isNotEmpty()) setupRayonDropdown(rayons)
        }

        viewModel.selectedFamilleProduit.observe(this) { famille ->
            binding.actvFamille.setText(famille?.libelle ?: getString(R.string.filter_all), false)
        }

        viewModel.selectedRayon.observe(this) { rayon ->
            binding.actvRayon.setText(rayon?.libelle ?: getString(R.string.filter_all), false)
        }

        viewModel.pagination.observe(this) { _ -> }
    }
}
