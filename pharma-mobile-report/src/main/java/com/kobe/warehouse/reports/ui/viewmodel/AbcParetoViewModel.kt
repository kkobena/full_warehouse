package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.api.PaginationInfo
import com.kobe.warehouse.reports.data.model.AbcPareto
import com.kobe.warehouse.reports.data.model.AbcParetoSummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for ABC Pareto screen with pagination support.
 */
class AbcParetoViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 50
    }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _products = MutableLiveData<List<AbcPareto>>(emptyList())
    val products: LiveData<List<AbcPareto>> = _products

    private val _summary = MutableLiveData<AbcParetoSummary?>(null)
    val summary: LiveData<AbcParetoSummary?> = _summary

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _selectedFilter = MutableLiveData(FilterType.ALL)
    val selectedFilter: LiveData<FilterType> = _selectedFilter

    private val _summaryExpanded = MutableLiveData(true)
    val summaryExpanded: LiveData<Boolean> = _summaryExpanded

    // Pagination state
    private val _pagination = MutableLiveData(PaginationInfo())
    val pagination: LiveData<PaginationInfo> = _pagination

    private val _canLoadMore = MutableLiveData(false)
    val canLoadMore: LiveData<Boolean> = _canLoadMore

    private var currentPage = 0

    enum class FilterType {
        ALL, CLASS_A, CLASS_B, CLASS_C, TOP_20
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0

            // Load summary
            val summaryResult = repository.getABCParetoSummary()
            summaryResult.onSuccess { s ->
                _summary.value = s
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

            loadProducts()
            _isLoading.value = false
        }
    }

    private suspend fun loadProducts() {
        currentPage = 0

        when (_selectedFilter.value) {
            FilterType.TOP_20 -> {
                // Top 20 - no pagination needed
                val result = repository.getAllABCPareto(page = 0, size = 20)
                result.onSuccess { paginatedResult ->
                    _products.value = paginatedResult.items
                    _isEmpty.value = paginatedResult.items.isEmpty()
                    _canLoadMore.value = false
                    _pagination.value = PaginationInfo(
                        totalCount = paginatedResult.items.size,
                        totalPages = 1,
                        currentPage = 0,
                        pageSize = 20,
                        hasNext = false,
                        hasPrevious = false
                    )
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
            FilterType.CLASS_A, FilterType.CLASS_B, FilterType.CLASS_C -> {
                val classePareto = when (_selectedFilter.value) {
                    FilterType.CLASS_A -> "A"
                    FilterType.CLASS_B -> "B"
                    FilterType.CLASS_C -> "C"
                    else -> "A"
                }
                val result = repository.getByParetoClass(classePareto, page = 0, size = PAGE_SIZE)
                result.onSuccess { paginatedResult ->
                    _products.value = paginatedResult.items
                    _pagination.value = paginatedResult.pagination
                    _canLoadMore.value = paginatedResult.pagination.hasNext
                    _isEmpty.value = paginatedResult.items.isEmpty()
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
            else -> {
                // ALL - paginated
                val result = repository.getAllABCPareto(page = 0, size = PAGE_SIZE)
                result.onSuccess { paginatedResult ->
                    _products.value = paginatedResult.items
                    _pagination.value = paginatedResult.pagination
                    _canLoadMore.value = paginatedResult.pagination.hasNext
                    _isEmpty.value = paginatedResult.items.isEmpty()
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value == true || _canLoadMore.value == false) return
        if (_selectedFilter.value == FilterType.TOP_20) return // No pagination for top 20

        viewModelScope.launch {
            _isLoadingMore.value = true

            val nextPage = currentPage + 1

            val result = when (_selectedFilter.value) {
                FilterType.CLASS_A -> repository.getByParetoClass("A", page = nextPage, size = PAGE_SIZE)
                FilterType.CLASS_B -> repository.getByParetoClass("B", page = nextPage, size = PAGE_SIZE)
                FilterType.CLASS_C -> repository.getByParetoClass("C", page = nextPage, size = PAGE_SIZE)
                else -> repository.getAllABCPareto(page = nextPage, size = PAGE_SIZE)
            }

            result.onSuccess { paginatedResult ->
                currentPage = nextPage
                val currentList = _products.value.orEmpty()
                _products.value = currentList + paginatedResult.items
                _pagination.value = paginatedResult.pagination
                _canLoadMore.value = paginatedResult.pagination.hasNext
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

            _isLoadingMore.value = false
        }
    }

    fun setFilter(filter: FilterType) {
        if (_selectedFilter.value != filter) {
            _selectedFilter.value = filter
            viewModelScope.launch {
                _isLoading.value = true
                loadProducts()
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0

            // Reload summary
            val summaryResult = repository.getABCParetoSummary()
            summaryResult.onSuccess { s ->
                _summary.value = s
            }

            loadProducts()
            _isRefreshing.value = false
        }
    }

    fun toggleSummary() {
        _summaryExpanded.value = !(_summaryExpanded.value ?: true)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class AbcParetoViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AbcParetoViewModel::class.java)) {
            return AbcParetoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
