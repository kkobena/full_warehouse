package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.ProductSearchResult
import com.kobe.warehouse.reports.databinding.ActivityProductSearchBinding
import com.kobe.warehouse.reports.ui.adapter.ProductSearchAdapter
import com.kobe.warehouse.reports.ui.viewmodel.ProductSearchViewModel
import com.kobe.warehouse.reports.ui.viewmodel.ProductSearchViewModelFactory

/**
 * Activity for searching products.
 */
class ProductSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductSearchBinding
    private lateinit var viewModel: ProductSearchViewModel
    private lateinit var productAdapter: ProductSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupSearchInput()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        // Check for initial query from intent
        intent.getStringExtra(EXTRA_INITIAL_QUERY)?.let { query ->
            binding.etSearch.setText(query)
            viewModel.searchImmediate(query)
        }
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = ProductSearchViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ProductSearchViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSearchInput() {
        binding.etSearch.apply {
            // Text change listener with debounce
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    viewModel.search(s?.toString() ?: "")
                }
            })

            // Handle search action on keyboard
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val query = text?.toString() ?: ""
                    if (query.length >= 2) {
                        viewModel.searchImmediate(query)
                    }
                    true
                } else {
                    false
                }
            }
        }

        // Search icon click
        binding.tilSearch.setEndIconOnClickListener {
            val query = binding.etSearch.text?.toString() ?: ""
            if (query.length >= 2) {
                viewModel.searchImmediate(query)
            }
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductSearchAdapter { product ->
            navigateToProductDetail(product)
        }

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@ProductSearchActivity)
            adapter = productAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.primary)
            setOnRefreshListener {
                viewModel.refresh()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingOverlay.isVisible = isLoading && productAdapter.itemCount == 0
            binding.swipeRefresh.isRefreshing = isLoading && productAdapter.itemCount > 0
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)
            updateUI(products)
        }

        viewModel.searchQuery.observe(this) { query ->
            updateEmptyState(query, productAdapter.currentList)
        }
    }

    private fun updateUI(products: List<ProductSearchResult>) {
        val query = viewModel.searchQuery.value ?: ""

        // Update results count
        if (products.isNotEmpty()) {
            binding.tvResultsCount.text = getString(R.string.product_search_results_count, products.size)
            binding.tvResultsCount.isVisible = true
        } else {
            binding.tvResultsCount.isVisible = false
        }

        // Update visibility
        binding.rvProducts.isVisible = products.isNotEmpty()
        updateEmptyState(query, products)
    }

    private fun updateEmptyState(query: String, products: List<ProductSearchResult>) {
        val showEmpty = products.isEmpty() && query.length >= 2

        binding.llEmptyState.isVisible = showEmpty
        binding.rvProducts.isVisible = products.isNotEmpty()

        if (showEmpty) {
            binding.tvEmptyTitle.text = getString(R.string.product_search_no_results_title)
            binding.tvEmptyMessage.text = getString(R.string.product_search_no_results_message, query)
        } else if (query.length < 2 && products.isEmpty()) {
            binding.llEmptyState.isVisible = true
            binding.tvEmptyTitle.text = getString(R.string.product_search_empty_title)
            binding.tvEmptyMessage.text = getString(R.string.product_search_empty_message)
        }
    }

    private fun navigateToProductDetail(product: ProductSearchResult) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id.toLong())
        }
        startActivity(intent)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refresh()
            }
            .show()
    }

    companion object {
        const val EXTRA_INITIAL_QUERY = "initial_query"
    }
}
