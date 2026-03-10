package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.api.PaginationInfo
import com.kobe.warehouse.reports.data.model.FamilleProduit
import com.kobe.warehouse.reports.data.model.Rayon
import com.kobe.warehouse.reports.data.model.StockValuation
import com.kobe.warehouse.reports.data.model.StockValuationSummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

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

    // Filter reference data
    private val _familleProduits = MutableLiveData<List<FamilleProduit>>(emptyList())
    val familleProduits: LiveData<List<FamilleProduit>> = _familleProduits

    private val _rayons = MutableLiveData<List<Rayon>>(emptyList())
    val rayons: LiveData<List<Rayon>> = _rayons

    // Active filters
    private val _selectedFamilleProduit = MutableLiveData<FamilleProduit?>(null)
    val selectedFamilleProduit: LiveData<FamilleProduit?> = _selectedFamilleProduit

    private val _selectedRayon = MutableLiveData<Rayon?>(null)
    val selectedRayon: LiveData<Rayon?> = _selectedRayon

    private var currentPage = 0

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0

            // Load filter reference data in parallel with first data load
            launch { loadFamilleProduits() }
            launch { loadRayons() }

            // Load summary
            repository.getStockValuationSummary(
                familleProduitId = _selectedFamilleProduit.value?.id,
                rayonId = _selectedRayon.value?.id
            ).onSuccess { _summary.value = it }
             .onFailure { _errorMessage.value = it.message }

            // Load first page
            loadPage(0)

            _isLoading.value = false
        }
    }

    private suspend fun loadFamilleProduits() {
        repository.getFamilleProduits()
            .onSuccess { _familleProduits.value = it }
    }

    private suspend fun loadRayons() {
        repository.getRayons()
            .onSuccess { _rayons.value = it }
    }

    fun applyFamilleFilter(famille: FamilleProduit?) {
        _selectedFamilleProduit.value = famille
        resetAndReload()
    }

    fun applyRayonFilter(rayon: Rayon?) {
        _selectedRayon.value = rayon
        resetAndReload()
    }

    private fun resetAndReload() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0
            repository.getStockValuationSummary(
                familleProduitId = _selectedFamilleProduit.value?.id,
                rayonId = _selectedRayon.value?.id
            ).onSuccess { _summary.value = it }
            loadPage(0)
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value == true || _canLoadMore.value == false) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = currentPage + 1
            repository.getAllStockValuation(
                page = nextPage,
                size = PAGE_SIZE,
                familleProduitId = _selectedFamilleProduit.value?.id,
                rayonId = _selectedRayon.value?.id
            ).onSuccess { result ->
                currentPage = nextPage
                _products.value = _products.value.orEmpty() + result.items
                _pagination.value = result.pagination
                _canLoadMore.value = result.pagination.hasNext
            }.onFailure {
                _errorMessage.value = it.message
            }
            _isLoadingMore.value = false
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0
            repository.getStockValuationSummary(
                familleProduitId = _selectedFamilleProduit.value?.id,
                rayonId = _selectedRayon.value?.id
            ).onSuccess { _summary.value = it }
            loadPage(0)
            _isRefreshing.value = false
        }
    }

    private suspend fun loadPage(page: Int) {
        repository.getAllStockValuation(
            page = page,
            size = PAGE_SIZE,
            familleProduitId = _selectedFamilleProduit.value?.id,
            rayonId = _selectedRayon.value?.id
        ).onSuccess { result ->
            _products.value = result.items
            _pagination.value = result.pagination
            _canLoadMore.value = result.pagination.hasNext
            _isEmpty.value = result.items.isEmpty()
        }.onFailure {
            _errorMessage.value = it.message
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
