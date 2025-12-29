package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.ProductProfitability
import com.kobe.warehouse.reports.data.model.ProfitabilitySummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Profitability screen.
 */
class ProfitabilityViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _products = MutableLiveData<List<ProductProfitability>>(emptyList())
    val products: LiveData<List<ProductProfitability>> = _products

    private val _summary = MutableLiveData<ProfitabilitySummary?>(null)
    val summary: LiveData<ProfitabilitySummary?> = _summary

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _selectedFilter = MutableLiveData(FilterType.ALL)
    val selectedFilter: LiveData<FilterType> = _selectedFilter

    private val _summaryExpanded = MutableLiveData(true)
    val summaryExpanded: LiveData<Boolean> = _summaryExpanded

    enum class FilterType {
        ALL, STAR, CASH_COW, QUESTION_MARK, DOG, LOW_MARGIN
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val summaryResult = repository.getProfitabilitySummary()
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
        val result = when (_selectedFilter.value) {
            FilterType.STAR -> repository.getByBCGCategory("STAR")
            FilterType.CASH_COW -> repository.getByBCGCategory("CASH_COW")
            FilterType.QUESTION_MARK -> repository.getByBCGCategory("QUESTION_MARK")
            FilterType.DOG -> repository.getByBCGCategory("DOG")
            else -> repository.getAllProductProfitability()
        }

        result.onSuccess { list ->
            val filtered = if (_selectedFilter.value == FilterType.LOW_MARGIN) {
                list.filter { (it.tauxMargePct?.toFloat() ?: 0f) < 10f }
            } else {
                list
            }
            _products.value = filtered
            _isEmpty.value = filtered.isEmpty()
        }.onFailure { error ->
            _errorMessage.value = error.message
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
            loadData()
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

class ProfitabilityViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfitabilityViewModel::class.java)) {
            return ProfitabilityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
