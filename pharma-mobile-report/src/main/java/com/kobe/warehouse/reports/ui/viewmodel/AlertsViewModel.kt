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

    companion object {
        private const val PAGE_SIZE = 20
    }

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isLastPage = MutableLiveData<Boolean>()
    val isLastPage: LiveData<Boolean> = _isLastPage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _allAlerts = MutableLiveData<List<Alert>>()

    private val _currentFilter = MutableLiveData<String?>(null)

    private var currentPage = 0

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
     * Load alerts from server (initial load).
     */
    fun loadAlerts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 0
            _isLastPage.value = false

            val result = repository.getAlerts(page = currentPage, size = PAGE_SIZE)

            result.fold(
                onSuccess = { alertsList ->
                    _allAlerts.value = alertsList
                    _isLastPage.value = alertsList.size < PAGE_SIZE
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Load more alerts (pagination).
     */
    fun loadMoreAlerts() {
        if (_isLoadingMore.value == true || _isLastPage.value == true) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            currentPage++

            val result = repository.getAlerts(page = currentPage, size = PAGE_SIZE)

            result.fold(
                onSuccess = { newAlerts ->
                    val currentList = _allAlerts.value?.toMutableList() ?: mutableListOf()
                    currentList.addAll(newAlerts)
                    _allAlerts.value = currentList
                    _isLastPage.value = newAlerts.size < PAGE_SIZE
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                    currentPage-- // Revert page increment on failure
                }
            )

            _isLoadingMore.value = false
        }
    }

    /**
     * Refresh alerts (pull-to-refresh).
     */
    fun refreshAlerts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            currentPage = 0
            _isLastPage.value = false

            val result = repository.getAlerts(page = currentPage, size = PAGE_SIZE)

            result.fold(
                onSuccess = { alertsList ->
                    _allAlerts.value = alertsList
                    _isLastPage.value = alertsList.size < PAGE_SIZE
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

    /**
     * Resolve an alert (mark as handled).
     */
    fun resolveAlert(alertId: Long) {
        viewModelScope.launch {
            // Call API to resolve alert
            // For now, just remove from local list
            val currentList = _allAlerts.value?.toMutableList() ?: mutableListOf()
            currentList.removeAll { it.id == alertId }
            _allAlerts.value = currentList
        }
    }

    /**
     * Dismiss an alert (mark as read/ignored).
     */
    fun dismissAlert(alertId: Long) {
        viewModelScope.launch {
            // Call API to dismiss alert
            // For now, just remove from local list
            val currentList = _allAlerts.value?.toMutableList() ?: mutableListOf()
            currentList.removeAll { it.id == alertId }
            _allAlerts.value = currentList
        }
    }
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
