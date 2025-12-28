package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.ProductQuickInfo
import com.kobe.warehouse.reports.databinding.ActivityProductDetailBinding
import com.kobe.warehouse.reports.ui.adapter.LotAdapter
import com.kobe.warehouse.reports.ui.viewmodel.ProductDetailViewModel
import com.kobe.warehouse.reports.ui.viewmodel.ProductDetailViewModelFactory

/**
 * Product detail activity - displays product information.
 * Extends BaseActivity for session management and logout functionality.
 */
class ProductDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var viewModel: ProductDetailViewModel
    private lateinit var lotAdapter: LotAdapter

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productId = intent.getLongExtra(EXTRA_PRODUCT_ID, -1)
        if (productId == -1L) {
            finish()
            return
        }

        setupViewModel()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadProduct(productId)
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = ProductDetailViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ProductDetailViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        lotAdapter = LotAdapter()
        binding.rvLots.apply {
            layoutManager = LinearLayoutManager(this@ProductDetailActivity)
            adapter = lotAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshProduct()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingOverlay.isVisible = isLoading
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.product.observe(this) { product ->
            updateUI(product)
        }
    }

    private fun updateUI(product: ProductQuickInfo) {
        // Header
        binding.tvProductName.text = product.name
        binding.tvProductCode.text = product.codeCip?.let { "CIP: $it" }
            ?: product.codeEan?.let { "EAN: $it" }
            ?: ""

        // Price info
        binding.tvPriceAchat.text = product.price.getFormattedPurchasePrice()
        binding.tvPriceVente.text = product.price.getFormattedSellingPrice()
        binding.tvMargin.text = product.price.getFormattedMargin()

        // Stock info
        binding.tvStockQuantity.text = product.stock.totalQuantity.toString()
        binding.tvStockValue.text = "${product.stock.totalQuantity * product.price.purchasePrice / 100} F"

        // Stock status
        val stockStatusText = when (product.stock.status) {
            "RUPTURE" -> "Rupture de stock"
            "LOW" -> "Stock bas"
            "OK" -> "Stock optimal"
            else -> product.stock.status
        }
        binding.tvStockStatus.text = stockStatusText

        val stockStatusColor = when (product.stock.status) {
            "RUPTURE" -> R.color.error
            "LOW" -> R.color.warning
            else -> R.color.success
        }
        binding.tvStockStatus.backgroundTintList =
            ContextCompat.getColorStateList(this, stockStatusColor)

        // Lots
        binding.chipLotsCount.text = "${product.lots.size} lots"
        lotAdapter.submitList(product.lots)
        binding.cardLots.isVisible = product.lots.isNotEmpty()

        // Sales stats
        binding.tvSalesToday.text = product.salesStats.todayQuantity.toString()
        binding.tvSalesWeek.text = product.salesStats.weekQuantity.toString()
        binding.tvSalesMonth.text = product.salesStats.monthQuantity.toString()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshProduct()
            }
            .show()
    }
}
