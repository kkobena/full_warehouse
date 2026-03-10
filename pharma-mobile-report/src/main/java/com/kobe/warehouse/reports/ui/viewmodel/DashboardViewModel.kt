package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.*
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Dashboard screen.
 * Manages dashboard data, alerts, and top products.
 */
class DashboardViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _dashboard = MutableLiveData<Dashboard>()
    val dashboard: LiveData<Dashboard> = _dashboard

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // =========================================================================
    // DASHBOARD ACTIONS
    // =========================================================================

    /**
     * Load dashboard data.
     */
    fun loadDashboard(date: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getDashboard(date)

            result.fold(
                onSuccess = { data ->
                    _dashboard.value = data
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Refresh dashboard data (pull-to-refresh).
     */
    fun refreshDashboard(date: String? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getDashboard(date)

            result.fold(
                onSuccess = { data ->
                    _dashboard.value = data
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isRefreshing.value = false
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // =========================================================================
    // COMPUTED PROPERTIES
    // =========================================================================

    /**
     * Get formatted daily CA.
     */
    fun getFormattedDailyCA(): String {
        return _dashboard.value?.getFormattedDailyCA() ?: "0 F"
    }

    /**
     * Get variation percentage.
     */
    fun getVariationPercent(): Double {
        return _dashboard.value?.variationPercent ?: 0.0
    }

    /**
     * Écart % entre le CA du jour et la moyenne des 30 derniers jours.
     */
    fun getTrendVs30j(): Double {
        return _dashboard.value?.trendVs30j ?: 0.0
    }

    /**
     * CA moyen journalier des 30 derniers jours.
     */
    fun getAverageCA30j(): String {
        return _dashboard.value?.getFormattedAverageCA30j() ?: "0 F"
    }

    /**
     * Get alerts count.
     */
    fun getAlertsCount(): Int {
        return _dashboard.value?.alertsCount ?: 0
    }

    /**
     * Check if there are critical alerts.
     */
    fun hasCriticalAlerts(): Boolean {
        return _dashboard.value?.alerts?.any { it.severity == "CRITICAL" } ?: false
    }
}

/**
 * Factory for DashboardViewModel.
 */
class DashboardViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
