package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.Alert
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Alerts screen.
 */
class AlertsViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _allAlerts = MutableLiveData<List<Alert>>()

    private val _currentFilter = MutableLiveData<String?>(null)

    val alerts: LiveData<List<Alert>> = MediatorLiveData<List<Alert>>().apply {
        addSource(_allAlerts) { updateFilteredAlerts() }
        addSource(_currentFilter) { updateFilteredAlerts() }
    }

    private fun MediatorLiveData<List<Alert>>.updateFilteredAlerts() {
        val allAlerts = _allAlerts.value ?: emptyList()
        val filter = _currentFilter.value
        value = if (filter == null) {
            allAlerts
        } else {
            allAlerts.filter { it.type == filter }
        }
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Load alerts from server.
     */
    fun loadAlerts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getAlerts()

            result.fold(
                onSuccess = { alertsList ->
                    _allAlerts.value = alertsList
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Refresh alerts (pull-to-refresh).
     */
    fun refreshAlerts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getAlerts()

            result.fold(
                onSuccess = { alertsList ->
                    _allAlerts.value = alertsList
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isRefreshing.value = false
        }
    }

    /**
     * Set filter type.
     */
    fun setFilter(filterType: String?) {
        _currentFilter.value = filterType
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Get current filter.
     */
    fun getCurrentFilter(): String? = _currentFilter.value
}

/**
 * Factory for AlertsViewModel.
 */
class AlertsViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
            return AlertsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
