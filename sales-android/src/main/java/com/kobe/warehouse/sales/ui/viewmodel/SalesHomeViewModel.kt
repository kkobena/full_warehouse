package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.repository.SalesRepository
import kotlinx.coroutines.launch

/**
 * SalesHomeViewModel
 * Manages state for sales list screen
 */
class SalesHomeViewModel(
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _sales = MutableLiveData<List<Sale>>()
    val sales: LiveData<List<Sale>> = _sales

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedSale = MutableLiveData<Sale?>()
    val selectedSale: LiveData<Sale?> = _selectedSale

    // Filter state
    private var currentSearch: String? = null

    init {
        loadSales()
    }

    /**
     * Load sales
     */
    fun loadSales(search: String? = null) {
        currentSearch = search

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            salesRepository.getSales(search).fold(
                onSuccess = { salesList ->
                    _sales.value = salesList
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur de chargement"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Refresh sales list
     */
    fun refresh() {
        loadSales(currentSearch)
    }

    /**
     * Search sales
     */
    fun searchSales(query: String) {
        loadSales(query)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Load sale by ID and date
     */
    fun loadSaleById(saleId: Long, saleDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            salesRepository.getSaleById(saleId, saleDate).fold(
                onSuccess = { sale ->
                    _selectedSale.value = sale
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur de chargement"
                    _isLoading.value = false
                }
            )
        }
    }
}
