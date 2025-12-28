package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.TodoItem
import com.kobe.warehouse.reports.databinding.ActivityTodosBinding
import com.kobe.warehouse.reports.ui.adapter.PaginationScrollListener
import com.kobe.warehouse.reports.ui.adapter.TodoAdapter
import com.kobe.warehouse.reports.ui.viewmodel.TodosViewModel
import com.kobe.warehouse.reports.ui.viewmodel.TodosViewModelFactory

/**
 * Todos activity - displays prioritized action items.
 */
class TodosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodosBinding
    private lateinit var viewModel: TodosViewModel
    private lateinit var todoAdapter: TodoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadTodos()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = TodosViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TodosViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onItemClick = { todo -> handleTodoClick(todo) },
            onItemChecked = { todo, isChecked -> handleTodoChecked(todo, isChecked) }
        )
        binding.rvTodos.apply {
            layoutManager = LinearLayoutManager(this@TodosActivity)
            adapter = todoAdapter
        }

        // Setup pagination
        setupPagination()
    }

    private fun setupPagination() {
        val layoutManager = binding.rvTodos.layoutManager as LinearLayoutManager
        binding.rvTodos.addOnScrollListener(object : PaginationScrollListener(layoutManager) {
            override fun loadMoreItems() {
                viewModel.loadMoreTodos()
            }

            override fun isLoading(): Boolean {
                return viewModel.isLoadingMore.value == true
            }

            override fun isLastPage(): Boolean {
                return viewModel.isLastPage.value == true
            }
        })
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTodos()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showSkeleton()
            }
            // Hide loadingOverlay (kept for compatibility)
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

        viewModel.paginatedItems.observe(this) { items ->
            showContent()
            todoAdapter.submitList(items)
            updateEmptyState(items.isEmpty())
        }

        viewModel.urgentCount.observe(this) { count ->
            binding.tvUrgentCount.text = count.toString()
        }

        viewModel.importantCount.observe(this) { count ->
            binding.tvImportantCount.text = count.toString()
        }

        viewModel.normalCount.observe(this) { count ->
            binding.tvNormalCount.text = count.toString()
        }
    }

    /**
     * Show skeleton loading view.
     */
    private fun showSkeleton() {
        binding.viewFlipper.displayedChild = VIEW_SKELETON
    }

    /**
     * Show actual content view.
     */
    private fun showContent() {
        binding.viewFlipper.displayedChild = VIEW_CONTENT
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.isVisible = isEmpty
        binding.rvTodos.isVisible = !isEmpty
    }

    private fun handleTodoClick(todo: TodoItem) {
        when (todo.actionType) {
            TodoItem.ACTION_CREATE_ORDER -> {
                todo.getProductId()?.let { productId ->
                    navigateToProductDetail(productId)
                }
            }
            TodoItem.ACTION_CALL -> {
                todo.getPhone()?.let { phone ->
                    dialPhone(phone)
                }
            }
            TodoItem.ACTION_NAVIGATE -> {
                todo.relatedEntityId?.let { entityId ->
                    when (todo.relatedEntityType) {
                        "PRODUCT" -> navigateToProductDetail(entityId)
                    }
                }
            }
        }
    }

    private fun handleTodoChecked(todo: TodoItem, isChecked: Boolean) {
        // TODO: Implement dismiss/complete action
        if (isChecked) {
            Snackbar.make(binding.root, "Tâche marquée comme terminée", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun navigateToProductDetail(productId: Long) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId)
        }
        startActivity(intent)
    }

    private fun dialPhone(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        startActivity(intent)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.loadTodos()
            }
            .show()
    }
}
