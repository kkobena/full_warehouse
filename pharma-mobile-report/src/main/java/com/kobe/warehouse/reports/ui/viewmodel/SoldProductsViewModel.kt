package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.api.PaginationInfo
import com.kobe.warehouse.reports.data.model.RecapProduitVendu
import com.kobe.warehouse.reports.data.model.RecapProduitVenduSummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SoldProductsViewModel(private val repository: ReportRepository) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 50
        private const val SEARCH_DEBOUNCE_MS = 400L
        private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE
    }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _products = MutableLiveData<List<RecapProduitVendu>>(emptyList())
    val products: LiveData<List<RecapProduitVendu>> = _products

    private val _summary = MutableLiveData<RecapProduitVenduSummary?>(null)
    val summary: LiveData<RecapProduitVenduSummary?> = _summary

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _canLoadMore = MutableLiveData(false)
    val canLoadMore: LiveData<Boolean> = _canLoadMore

    private val _pagination = MutableLiveData(PaginationInfo())
    val pagination: LiveData<PaginationInfo> = _pagination

    // Filters
    var startDate: LocalDate = LocalDate.now()
        private set
    var endDate: LocalDate = LocalDate.now()
        private set
    private var searchTerm: String? = null
    private var searchJob: Job? = null
    private var currentPage = 0

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0
            loadSummary()
            loadPage(0)
            _isLoading.value = false
        }
    }

    fun applyDateFilter(start: LocalDate, end: LocalDate) {
        startDate = start
        endDate = end
        resetAndReload()
    }

    fun onSearchChanged(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchTerm = query.trim().takeIf { it.isNotEmpty() }
            resetAndReload()
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value == true || _canLoadMore.value == false) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val next = currentPage + 1
            repository.getSoldProducts(
                startDate = startDate.format(DATE_FMT),
                endDate = endDate.format(DATE_FMT),
                search = searchTerm,
                page = next,
                size = PAGE_SIZE
            ).onSuccess { result ->
                currentPage = next
                _products.value = _products.value.orEmpty() + result.items
                _pagination.value = result.pagination
                _canLoadMore.value = result.pagination.hasNext
            }.onFailure { _errorMessage.value = it.message }
            _isLoadingMore.value = false
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0
            loadSummary()
            loadPage(0)
            _isRefreshing.value = false
        }
    }

    private fun resetAndReload() {
        viewModelScope.launch {
            _isLoading.value = true
            currentPage = 0
            loadSummary()
            loadPage(0)
            _isLoading.value = false
        }
    }

    private suspend fun loadPage(page: Int) {
        repository.getSoldProducts(
            startDate = startDate.format(DATE_FMT),
            endDate = endDate.format(DATE_FMT),
            search = searchTerm,
            page = page,
            size = PAGE_SIZE
        ).onSuccess { result ->
            _products.value = result.items
            _pagination.value = result.pagination
            _canLoadMore.value = result.pagination.hasNext
            _isEmpty.value = result.items.isEmpty()
        }.onFailure { _errorMessage.value = it.message }
    }

    private suspend fun loadSummary() {
        repository.getSoldProductsSummary(
            startDate = startDate.format(DATE_FMT),
            endDate = endDate.format(DATE_FMT),
            search = searchTerm
        ).onSuccess { _summary.value = it }
    }

    fun clearError() { _errorMessage.value = null }
}

class SoldProductsViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoldProductsViewModel::class.java)) {
            return SoldProductsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
