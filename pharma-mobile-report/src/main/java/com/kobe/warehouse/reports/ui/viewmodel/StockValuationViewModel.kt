package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.api.PaginationInfo
import com.kobe.warehouse.reports.data.model.StockValuation
import com.kobe.warehouse.reports.data.model.StockValuationSummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Stock Valuation screen with pagination support.
 */
class StockValuationViewModel(
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

    private val _products = MutableLiveData<List<StockValuation>>(emptyList())
    val products: LiveData<List<StockValuation>> = _products

    private val _summary = MutableLiveData<StockValuationSummary?>(null)
    val summary: LiveData<StockValuationSummary?> = _summary

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _summaryExpanded = MutableLiveData(true)
    val summaryExpanded: LiveData<Boolean> = _summaryExpanded

    // Pagination state
    private val _pagination = MutableLiveData(PaginationInfo())
    val pagination: LiveData<PaginationInfo> = _pagination

    private val _canLoadMore = MutableLiveData(false)
    val canLoadMore: LiveData<Boolean> = _canLoadMore

    private var currentPage = 0

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0

            // Load summary
            val summaryResult = repository.getStockValuationSummary()
            summaryResult.onSuccess { s ->
                _summary.value = s
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

            // Load first page of products
            val result = repository.getAllStockValuation(page = 0, size = PAGE_SIZE)
            result.onSuccess { paginatedResult ->
                _products.value = paginatedResult.items
                _pagination.value = paginatedResult.pagination
                _canLoadMore.value = paginatedResult.pagination.hasNext
                _isEmpty.value = paginatedResult.items.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value == true || _canLoadMore.value == false) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            val nextPage = currentPage + 1
            val result = repository.getAllStockValuation(page = nextPage, size = PAGE_SIZE)

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

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0

            // Load summary
            val summaryResult = repository.getStockValuationSummary()
            summaryResult.onSuccess { s ->
                _summary.value = s
            }

            // Load first page of products
            val result = repository.getAllStockValuation(page = 0, size = PAGE_SIZE)
            result.onSuccess { paginatedResult ->
                _products.value = paginatedResult.items
                _pagination.value = paginatedResult.pagination
                _canLoadMore.value = paginatedResult.pagination.hasNext
                _isEmpty.value = paginatedResult.items.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

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

class StockValuationViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockValuationViewModel::class.java)) {
            return StockValuationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
