package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.ProductSearchResult
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for product search screen.
 */
class ProductSearchViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    private val _products = MutableLiveData<List<ProductSearchResult>>(emptyList())
    val products: LiveData<List<ProductSearchResult>> = _products

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private var searchJob: Job? = null

    companion object {
        private const val DEBOUNCE_DELAY = 300L
        private const val MIN_QUERY_LENGTH = 2
        private const val DEFAULT_LIMIT = 50
    }

    /**
     * Search products with debounce.
     */
    fun search(query: String) {
        _searchQuery.value = query

        // Cancel previous search
        searchJob?.cancel()

        if (query.length < MIN_QUERY_LENGTH) {
            _products.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            executeSearch(query)
        }
    }

    /**
     * Execute immediate search.
     */
    fun searchImmediate(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            _products.value = emptyList()
            return
        }

        viewModelScope.launch {
            executeSearch(query)
        }
    }

    /**
     * Execute search request.
     */
    private suspend fun executeSearch(query: String) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.searchProducts(query, DEFAULT_LIMIT)
            .onSuccess { results ->
                _products.value = results
            }
            .onFailure { error ->
                _errorMessage.value = error.message
                _products.value = emptyList()
            }

        _isLoading.value = false
    }

    /**
     * Clear search results.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _products.value = emptyList()
        _errorMessage.value = null
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refresh current search.
     */
    fun refresh() {
        val currentQuery = _searchQuery.value
        if (!currentQuery.isNullOrBlank() && currentQuery.length >= MIN_QUERY_LENGTH) {
            viewModelScope.launch {
                executeSearch(currentQuery)
            }
        }
    }
}

/**
 * Factory for ProductSearchViewModel.
 */
class ProductSearchViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductSearchViewModel::class.java)) {
            return ProductSearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
