package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.TodoItem
import com.kobe.warehouse.reports.data.model.TodoList
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Todos screen.
 */
class TodosViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isLastPage = MutableLiveData<Boolean>()
    val isLastPage: LiveData<Boolean> = _isLastPage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _todoList = MutableLiveData<TodoList>()
    val todoList: LiveData<TodoList> = _todoList

    // For paginated items (flat list)
    private val _paginatedItems = MutableLiveData<List<TodoItem>>()
    val paginatedItems: LiveData<List<TodoItem>> = _paginatedItems

    val allItems: LiveData<List<TodoItem>> = _todoList.map { it.getAllItems() }

    private val _urgentCount = MutableLiveData<Int>()
    val urgentCount: LiveData<Int> = _urgentCount

    private val _importantCount = MutableLiveData<Int>()
    val importantCount: LiveData<Int> = _importantCount

    private val _normalCount = MutableLiveData<Int>()
    val normalCount: LiveData<Int> = _normalCount

    val isEmpty: LiveData<Boolean> = _paginatedItems.map { it.isEmpty() }

    // Pagination state
    private var currentPage = 0

    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Load todos from server (initial load with pagination).
     */
    fun loadTodos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0
            _isLastPage.value = false

            // Load counts
            loadCounts()

            // Load first page of items
            val result = repository.getTodoItems(page = 0, size = PAGE_SIZE)

            result.fold(
                onSuccess = { items ->
                    _paginatedItems.value = items
                    _isLastPage.value = items.size < PAGE_SIZE
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Load more todos (infinite scroll).
     */
    fun loadMoreTodos() {
        if (_isLoadingMore.value == true || _isLastPage.value == true) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true
            currentPage++

            val result = repository.getTodoItems(page = currentPage, size = PAGE_SIZE)

            result.fold(
                onSuccess = { newItems ->
                    if (newItems.isEmpty()) {
                        _isLastPage.value = true
                    } else {
                        val currentList = _paginatedItems.value.orEmpty().toMutableList()
                        currentList.addAll(newItems)
                        _paginatedItems.value = currentList
                        _isLastPage.value = newItems.size < PAGE_SIZE
                    }
                },
                onFailure = { exception ->
                    // Revert page on error
                    currentPage--
                    _errorMessage.value = exception.message
                }
            )

            _isLoadingMore.value = false
        }
    }

    /**
     * Refresh todos (pull-to-refresh).
     */
    fun refreshTodos() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            currentPage = 0
            _isLastPage.value = false

            // Reload counts
            loadCounts()

            // Reload first page
            val result = repository.getTodoItems(page = 0, size = PAGE_SIZE)

            result.fold(
                onSuccess = { items ->
                    _paginatedItems.value = items
                    _isLastPage.value = items.size < PAGE_SIZE
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isRefreshing.value = false
        }
    }

    /**
     * Load counts by priority.
     */
    private suspend fun loadCounts() {
        val countsResult = repository.getTodoCounts()
        countsResult.fold(
            onSuccess = { counts ->
                _urgentCount.value = counts.urgent
                _importantCount.value = counts.important
                _normalCount.value = counts.normal
            },
            onFailure = {
                // Fallback: keep existing counts or set to 0
            }
        )
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for TodosViewModel.
 */
class TodosViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodosViewModel::class.java)) {
            return TodosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
