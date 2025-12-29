package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.api.PaginationInfo
import com.kobe.warehouse.reports.data.model.StockRotation
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Stock Rotation screen with pagination support.
 */
class StockRotationViewModel(
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

    private val _products = MutableLiveData<List<StockRotation>>(emptyList())
    val products: LiveData<List<StockRotation>> = _products

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _selectedFilter = MutableLiveData(FilterType.ALL)
    val selectedFilter: LiveData<FilterType> = _selectedFilter

    // ABC counts
    private val _countA = MutableLiveData(0)
    val countA: LiveData<Int> = _countA

    private val _countB = MutableLiveData(0)
    val countB: LiveData<Int> = _countB

    private val _countC = MutableLiveData(0)
    val countC: LiveData<Int> = _countC

    // Pagination state
    private val _pagination = MutableLiveData(PaginationInfo())
    val pagination: LiveData<PaginationInfo> = _pagination

    private val _canLoadMore = MutableLiveData(false)
    val canLoadMore: LiveData<Boolean> = _canLoadMore

    private var currentPage = 0

    enum class FilterType {
        ALL, CLASS_A, CLASS_B, CLASS_C, SLOW_MOVING
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0

            // Load ABC counts from API
            try {
                val countsResult = repository.getStockRotationABCCounts()
                countsResult.onSuccess { counts ->
                    _countA.value = counts["A"]?.toInt() ?: 0
                    _countB.value = counts["B"]?.toInt() ?: 0
                    _countC.value = counts["C"]?.toInt() ?: 0
                }
            } catch (e: Exception) {
                // Counts loading failed, continue with products
            }

            loadProducts()
            _isLoading.value = false
        }
    }

    private suspend fun loadProducts() {
        currentPage = 0

        when (_selectedFilter.value) {
            FilterType.SLOW_MOVING -> {
                // Slow moving products - no pagination
                val result = repository.getSlowMovingProducts()
                result.onSuccess { list ->
                    _products.value = list
                    _isEmpty.value = list.isEmpty()
                    _canLoadMore.value = false
                    _pagination.value = PaginationInfo(
                        totalCount = list.size,
                        totalPages = 1,
                        currentPage = 0,
                        pageSize = list.size,
                        hasNext = false,
                        hasPrevious = false
                    )
                }.onFailure { error ->
                    _errorMessage.value = error.message
                }
            }
            FilterType.CLASS_A, FilterType.CLASS_B, FilterType.CLASS_C -> {
                val category = when (_selectedFilter.value) {
                    FilterType.CLASS_A -> "A"
                    FilterType.CLASS_B -> "B"
                    FilterType.CLASS_C -> "C"
                    else -> "A"
                }
                val result = repository.getStockRotationByABC(category, page = 0, size = PAGE_SIZE)
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
                val result = repository.getAllStockRotation(page = 0, size = PAGE_SIZE)
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
        if (_selectedFilter.value == FilterType.SLOW_MOVING) return // No pagination for slow moving

        viewModelScope.launch {
            _isLoadingMore.value = true

            val nextPage = currentPage + 1

            val result = when (_selectedFilter.value) {
                FilterType.CLASS_A -> repository.getStockRotationByABC("A", page = nextPage, size = PAGE_SIZE)
                FilterType.CLASS_B -> repository.getStockRotationByABC("B", page = nextPage, size = PAGE_SIZE)
                FilterType.CLASS_C -> repository.getStockRotationByABC("C", page = nextPage, size = PAGE_SIZE)
                else -> repository.getAllStockRotation(page = nextPage, size = PAGE_SIZE)
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

            // Reload ABC counts
            try {
                val countsResult = repository.getStockRotationABCCounts()
                countsResult.onSuccess { counts ->
                    _countA.value = counts["A"]?.toInt() ?: 0
                    _countB.value = counts["B"]?.toInt() ?: 0
                    _countC.value = counts["C"]?.toInt() ?: 0
                }
            } catch (e: Exception) {
                // Continue
            }

            loadProducts()
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun getStockRotationABCCounts(): Result<Map<String, Long>> {
        return try {
            val response = repository.getStockRotationABCCounts()
            response
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class StockRotationViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockRotationViewModel::class.java)) {
            return StockRotationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
