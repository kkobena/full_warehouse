package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.SupplierPerformance
import com.kobe.warehouse.reports.data.model.SupplierPerformanceSummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Supplier Performance screen.
 */
class SupplierPerformanceViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    // State
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _suppliers = MutableLiveData<List<SupplierPerformance>>(emptyList())
    val suppliers: LiveData<List<SupplierPerformance>> = _suppliers

    private val _summary = MutableLiveData<SupplierPerformanceSummary?>(null)
    val summary: LiveData<SupplierPerformanceSummary?> = _summary

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Filter state
    private val _selectedFilter = MutableLiveData(FilterType.ALL)
    val selectedFilter: LiveData<FilterType> = _selectedFilter

    // Expansion state
    private val _summaryExpanded = MutableLiveData(true)
    val summaryExpanded: LiveData<Boolean> = _summaryExpanded

    enum class FilterType {
        ALL,
        TOP_10,
        EXCELLENT,  // Score >= 80
        GOOD,       // Score 60-79
        ISSUES      // Score < 60
    }

    /**
     * Load supplier performance data.
     */
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Load summary
            val summaryResult = repository.getSupplierPerformanceSummary()
            summaryResult.onSuccess { s ->
                _summary.value = s
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            // Load suppliers based on filter
            loadSuppliers()

            _isLoading.value = false
        }
    }

    /**
     * Refresh data.
     */
    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val summaryResult = repository.getSupplierPerformanceSummary()
            summaryResult.onSuccess { s ->
                _summary.value = s
            }

            loadSuppliers()

            _isRefreshing.value = false
        }
    }

    /**
     * Load suppliers based on current filter.
     */
    private suspend fun loadSuppliers() {
        val result = repository.getAllSupplierPerformance()

        result.onSuccess { list ->
            val filtered = when (_selectedFilter.value) {
                FilterType.ALL -> list
                FilterType.TOP_10 -> list.sortedByDescending { it.purchaseAmountLast12Months }.take(10)
                FilterType.EXCELLENT -> list.filter { (it.performanceScore?.toDouble() ?: 0.0) >= 80 }
                FilterType.GOOD -> list.filter {
                    val score = it.performanceScore?.toDouble() ?: 0.0
                    score >= 60 && score < 80
                }
                FilterType.ISSUES -> list.filter { (it.performanceScore?.toDouble() ?: 0.0) < 60 }
                null -> list
            }
            _suppliers.value = filtered
            _isEmpty.value = filtered.isEmpty()
        }.onFailure { error ->
            _errorMessage.value = error.message ?: "Erreur lors du chargement"
        }
    }

    /**
     * Set filter type.
     */
    fun setFilter(filter: FilterType) {
        if (_selectedFilter.value != filter) {
            _selectedFilter.value = filter
            viewModelScope.launch {
                _isLoading.value = true
                loadSuppliers()
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle summary expansion.
     */
    fun toggleSummary() {
        _summaryExpanded.value = !(_summaryExpanded.value ?: true)
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for creating SupplierPerformanceViewModel instances.
 */
class SupplierPerformanceViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierPerformanceViewModel::class.java)) {
            return SupplierPerformanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
