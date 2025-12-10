package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.Performance
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Performance screen.
 */
class PerformanceViewModel(
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

    private val _performance = MutableLiveData<Performance>()
    val performance: LiveData<Performance> = _performance

    private val _currentPeriod = MutableLiveData(Performance.PERIOD_WEEK)
    val currentPeriod: LiveData<String> = _currentPeriod

    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Load performance data for current period.
     */
    fun loadPerformance(period: String = Performance.PERIOD_WEEK) {
        _currentPeriod.value = period
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getPerformance(period)

            result.fold(
                onSuccess = { performanceData ->
                    _performance.value = performanceData
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Refresh performance data (pull-to-refresh).
     */
    fun refreshPerformance() {
        val period = _currentPeriod.value ?: Performance.PERIOD_WEEK
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getPerformance(period)

            result.fold(
                onSuccess = { performanceData ->
                    _performance.value = performanceData
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isRefreshing.value = false
        }
    }

    /**
     * Change period and reload data.
     */
    fun setPeriod(period: String) {
        if (_currentPeriod.value != period) {
            loadPerformance(period)
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for PerformanceViewModel.
 */
class PerformanceViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerformanceViewModel::class.java)) {
            return PerformanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
