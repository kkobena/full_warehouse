package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.TiersPayantCreancesSummary
import com.kobe.warehouse.reports.data.model.TiersPayantInvoice
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Tiers Payant Créances (Receivables) screen.
 */
class TiersPayantCreancesViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    // State
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _summaries = MutableLiveData<List<TiersPayantCreancesSummary>>(emptyList())
    val summaries: LiveData<List<TiersPayantCreancesSummary>> = _summaries

    private val _invoices = MutableLiveData<List<TiersPayantInvoice>>(emptyList())
    val invoices: LiveData<List<TiersPayantInvoice>> = _invoices

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Filter state
    private val _selectedGroupeId = MutableLiveData<Int?>(null)
    val selectedGroupeId: LiveData<Int?> = _selectedGroupeId

    private val _selectedAgeCategory = MutableLiveData<String?>(null)
    val selectedAgeCategory: LiveData<String?> = _selectedAgeCategory

    // View mode state
    private val _viewMode = MutableLiveData(ViewMode.SUMMARY)
    val viewMode: LiveData<ViewMode> = _viewMode

    // Expansion state for sections
    private val _summaryExpanded = MutableLiveData(true)
    val summaryExpanded: LiveData<Boolean> = _summaryExpanded

    private val _invoicesExpanded = MutableLiveData(false)
    val invoicesExpanded: LiveData<Boolean> = _invoicesExpanded

    // Totals
    private val _totalMontantRestant = MutableLiveData(0L)
    val totalMontantRestant: LiveData<Long> = _totalMontantRestant

    private val _totalFactures = MutableLiveData(0)
    val totalFactures: LiveData<Int> = _totalFactures

    enum class ViewMode {
        SUMMARY,    // Show summaries by groupe
        INVOICES    // Show individual invoices
    }

    /**
     * Load créances data.
     */
    fun loadCreances() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Load summaries
            val summaryResult = repository.getCreancesSummary()
            summaryResult.onSuccess { list ->
                _summaries.value = list
                _totalMontantRestant.value = list.sumOf { it.montantRestant }
                _totalFactures.value = list.sumOf { it.totalFactures }
                _isEmpty.value = list.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh créances data.
     */
    fun refreshCreances() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            when (_viewMode.value) {
                ViewMode.SUMMARY -> {
                    val result = repository.getCreancesSummary()
                    result.onSuccess { list ->
                        _summaries.value = list
                        _totalMontantRestant.value = list.sumOf { it.montantRestant }
                        _totalFactures.value = list.sumOf { it.totalFactures }
                        _isEmpty.value = list.isEmpty()
                    }.onFailure { error ->
                        _errorMessage.value = error.message ?: "Erreur lors du chargement"
                    }
                }
                ViewMode.INVOICES, null -> {
                    loadInvoices()
                }
            }

            _isRefreshing.value = false
        }
    }

    /**
     * Load invoices with current filters.
     */
    fun loadInvoices() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getUnpaidInvoices(
                groupeId = _selectedGroupeId.value,
                ageCategory = _selectedAgeCategory.value
            )

            result.onSuccess { list ->
                _invoices.value = list
                _isEmpty.value = list.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            _isLoading.value = false
        }
    }

    /**
     * Set view mode.
     */
    fun setViewMode(mode: ViewMode) {
        if (_viewMode.value != mode) {
            _viewMode.value = mode
            when (mode) {
                ViewMode.SUMMARY -> loadCreances()
                ViewMode.INVOICES -> loadInvoices()
            }
        }
    }

    /**
     * Filter by groupe tiers payant.
     */
    fun setGroupeFilter(groupeId: Int?) {
        _selectedGroupeId.value = groupeId
        if (_viewMode.value == ViewMode.INVOICES) {
            loadInvoices()
        }
    }

    /**
     * Filter by age category.
     */
    fun setAgeCategoryFilter(ageCategory: String?) {
        _selectedAgeCategory.value = ageCategory
        if (_viewMode.value == ViewMode.INVOICES) {
            loadInvoices()
        }
    }

    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _selectedGroupeId.value = null
        _selectedAgeCategory.value = null
        if (_viewMode.value == ViewMode.INVOICES) {
            loadInvoices()
        }
    }

    /**
     * View invoices for a specific groupe.
     */
    fun viewInvoicesForGroupe(groupeId: Int) {
        _selectedGroupeId.value = groupeId
        _viewMode.value = ViewMode.INVOICES
        loadInvoices()
    }

    /**
     * Toggle summary section expansion.
     */
    fun toggleSummary() {
        _summaryExpanded.value = !(_summaryExpanded.value ?: true)
    }

    /**
     * Toggle invoices section expansion.
     */
    fun toggleInvoices() {
        _invoicesExpanded.value = !(_invoicesExpanded.value ?: false)
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for creating TiersPayantCreancesViewModel instances.
 */
class TiersPayantCreancesViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TiersPayantCreancesViewModel::class.java)) {
            return TiersPayantCreancesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
