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
 * Manages state for ongoing sales list screen
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

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    // Filter state
    private var currentSearch: String? = null
    private var currentType: String? = null

    init {
        loadOngoingSales()
    }

    /**
     * Load ongoing sales (pré-ventes)
     */
    fun loadOngoingSales(search: String? = null, type: String? = null) {
        currentSearch = search
        currentType = type

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            salesRepository.getOngoingSales(search, type).fold(
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
        loadOngoingSales(currentSearch, currentType)
    }

    /**
     * Search sales
     */
    fun searchSales(query: String) {
        loadOngoingSales(query, currentType)
    }

    /**
     * Filter sales by type
     */
    fun filterByType(type: String) {
        loadOngoingSales(currentSearch, type)
    }

    /**
     * Delete ongoing sale
     */
    fun deleteOngoingSale(saleId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            salesRepository.deleteOngoingSale(saleId).fold(
                onSuccess = {
                    _deleteSuccess.value = true
                    _isLoading.value = false
                    // Refresh list after deletion
                    refresh()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur de suppression"
                    _isLoading.value = false
                    _deleteSuccess.value = false
                }
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
