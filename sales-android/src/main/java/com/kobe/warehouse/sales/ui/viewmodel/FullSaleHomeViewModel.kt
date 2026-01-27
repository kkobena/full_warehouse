package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.repository.SalesRepository
import kotlinx.coroutines.launch

/**
 * FullSaleHomeViewModel
 * Manages state for full sales home screen with tabs (Ventes en cours + Préventes)
 *
 * Features:
 * - Load ongoing sales
 * - Load preventes (sales on hold)
 * - Search/filter support
 * - Delete sales
 */
class FullSaleHomeViewModel(
    private val salesRepository: SalesRepository
) : ViewModel() {

    // Ongoing sales state
    private val _ongoingSales = MutableLiveData<List<Sale>>()
    val ongoingSales: LiveData<List<Sale>> = _ongoingSales

    // Preventes state
    private val _preventes = MutableLiveData<List<Sale>>()
    val preventes: LiveData<List<Sale>> = _preventes

    // Loading state
    private val _isLoadingOngoing = MutableLiveData<Boolean>()
    val isLoadingOngoing: LiveData<Boolean> = _isLoadingOngoing

    private val _isLoadingPreventes = MutableLiveData<Boolean>()
    val isLoadingPreventes: LiveData<Boolean> = _isLoadingPreventes

    // Error state
    private val _ongoingError = MutableLiveData<String?>()
    val ongoingError: LiveData<String?> = _ongoingError

    private val _preventesError = MutableLiveData<String?>()
    val preventesError: LiveData<String?> = _preventesError

    // Search query
    private var currentOngoingSearch: String? = null
    private var currentPreventesSearch: String? = null

    // Selected sale for navigation
    private val _selectedSale = MutableLiveData<Sale?>()
    val selectedSale: LiveData<Sale?> = _selectedSale

    init {
        loadOngoingSales()
        loadPreventes()
    }

    /**
     * Load ongoing sales (ventes en cours)
     */
    fun loadOngoingSales(search: String? = null) {
        currentOngoingSearch = search

        viewModelScope.launch {
            _isLoadingOngoing.value = true
            _ongoingError.value = null

            salesRepository.getSales(search).fold(
                onSuccess = { salesList ->
                    _ongoingSales.value = salesList
                    _isLoadingOngoing.value = false
                },
                onFailure = { error ->
                    _ongoingError.value = error.message ?: "Erreur de chargement des ventes"
                    _isLoadingOngoing.value = false
                }
            )
        }
    }

    /**
     * Load preventes (ventes mises en attente)
     */
    fun loadPreventes(search: String? = null) {
        currentPreventesSearch = search

        viewModelScope.launch {
            _isLoadingPreventes.value = true
            _preventesError.value = null

            salesRepository.getPreventes(search).fold(
                onSuccess = { salesList ->
                    _preventes.value = salesList
                    _isLoadingPreventes.value = false
                },
                onFailure = { error ->
                    _preventesError.value = error.message ?: "Erreur de chargement des préventes"
                    _isLoadingPreventes.value = false
                }
            )
        }
    }

    /**
     * Refresh ongoing sales
     */
    fun refreshOngoingSales() {
        loadOngoingSales(currentOngoingSearch)
    }

    /**
     * Refresh preventes
     */
    fun refreshPreventes() {
        loadPreventes(currentPreventesSearch)
    }

    /**
     * Search in ongoing sales
     */
    fun searchOngoingSales(query: String) {
        loadOngoingSales(query)
    }

    /**
     * Search in preventes
     */
    fun searchPreventes(query: String) {
        loadPreventes(query)
    }

    /**
     * Delete sale
     */
    fun deleteSale(saleId: Long, saleDate: String, isPrevente: Boolean = false) {
        viewModelScope.launch {
            salesRepository.deleteSale(saleId, saleDate).fold(
                onSuccess = {
                    // Refresh the appropriate list
                    if (isPrevente) {
                        refreshPreventes()
                    } else {
                        refreshOngoingSales()
                    }
                },
                onFailure = { error ->
                    if (isPrevente) {
                        _preventesError.value = error.message ?: "Erreur de suppression"
                    } else {
                        _ongoingError.value = error.message ?: "Erreur de suppression"
                    }
                }
            )
        }
    }

    /**
     * Select sale for navigation
     */
    fun selectSale(sale: Sale) {
        _selectedSale.value = sale
    }

    /**
     * Clear selected sale
     */
    fun clearSelectedSale() {
        _selectedSale.value = null
    }

    /**
     * Clear errors
     */
    fun clearOngoingError() {
        _ongoingError.value = null
    }

    fun clearPreventesError() {
        _preventesError.value = null
    }
}
