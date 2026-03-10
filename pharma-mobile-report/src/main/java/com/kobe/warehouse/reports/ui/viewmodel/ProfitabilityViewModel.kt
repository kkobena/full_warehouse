package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.MargeDTO
import com.kobe.warehouse.reports.data.model.MargeSummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Profitability screen.
 */
class ProfitabilityViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    private val _isLoading     = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing  = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage  = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _products      = MutableLiveData<List<MargeDTO>>(emptyList())
    val products: LiveData<List<MargeDTO>> = _products

    private val _summary       = MutableLiveData<MargeSummary?>(null)
    val summary: LiveData<MargeSummary?> = _summary

    private val _isEmpty       = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _summaryExpanded = MutableLiveData(true)
    val summaryExpanded: LiveData<Boolean> = _summaryExpanded

    // Filtres
    private var showLowMargin  = false
    private var currentPage    = 0
    private val pageSize       = 20

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0

            repository.getProfitabilitySummary().onSuccess { s ->
                _summary.value = s
            }.onFailure { e ->
                _errorMessage.value = e.message
            }

            loadProducts()
            _isLoading.value = false
        }
    }

    private suspend fun loadProducts() {
        val result = if (showLowMargin) {
            repository.getLowMarginProducts(page = currentPage, size = pageSize)
        } else {
            repository.getAllProductProfitability(page = currentPage, size = pageSize)
        }

        result.onSuccess { paginated ->
            _products.value = paginated.items
            _isEmpty.value  = paginated.items.isEmpty()
        }.onFailure { e ->
            _errorMessage.value = e.message
        }
    }

    fun setLowMarginFilter(enabled: Boolean) {
        if (showLowMargin != enabled) {
            showLowMargin = enabled
            viewModelScope.launch {
                _isLoading.value = true
                currentPage = 0
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
