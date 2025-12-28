package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.PharmacistDashboard
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel for Pharmacist Dashboard (Tableau Pharmacien) screen.
 */
class PharmacistDashboardViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _dashboard = MutableLiveData<PharmacistDashboard?>()
    val dashboard: LiveData<PharmacistDashboard?> = _dashboard

    // Date range state
    private val _fromDate = MutableLiveData<LocalDate>()
    val fromDate: LiveData<LocalDate> = _fromDate

    private val _toDate = MutableLiveData<LocalDate>()
    val toDate: LiveData<LocalDate> = _toDate

    val isEmpty: LiveData<Boolean> = _dashboard.map { it == null }

    init {
        // Default to today
        val today = LocalDate.now()
        _fromDate.value = today
        _toDate.value = today
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Load dashboard data for the current date range.
     */
    fun loadDashboard() {
        val from = _fromDate.value ?: LocalDate.now()
        val to = _toDate.value ?: from
        loadDashboardForDates(from, to)
    }

    /**
     * Load dashboard data for a specific date range.
     */
    fun loadDashboardForDates(fromDate: LocalDate, toDate: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _fromDate.value = fromDate
            _toDate.value = toDate

            val fromDateStr = fromDate.format(DATE_FORMATTER)
            val toDateStr = toDate.format(DATE_FORMATTER)

            val result = repository.getPharmacistDashboard(fromDateStr, toDateStr)

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
    fun refreshDashboard() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val from = _fromDate.value ?: LocalDate.now()
            val to = _toDate.value ?: from
            val fromDateStr = from.format(DATE_FORMATTER)
            val toDateStr = to.format(DATE_FORMATTER)

            val result = repository.getPharmacistDashboard(fromDateStr, toDateStr)

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
     * Set date range for the dashboard.
     */
    fun setDateRange(fromDate: LocalDate, toDate: LocalDate) {
        _fromDate.value = fromDate
        _toDate.value = toDate
        loadDashboard()
    }

    /**
     * Set preset period.
     */
    fun setPeriod(period: Period) {
        val today = LocalDate.now()
        val (from, to) = when (period) {
            Period.TODAY -> today to today
            Period.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
            Period.THIS_WEEK -> today.with(java.time.DayOfWeek.MONDAY) to today
            Period.LAST_WEEK -> {
                val lastMonday = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY)
                val lastSunday = lastMonday.plusDays(6)
                lastMonday to lastSunday
            }
            Period.THIS_MONTH -> today.withDayOfMonth(1) to today
            Period.LAST_MONTH -> {
                val lastMonth = today.minusMonths(1)
                lastMonth.withDayOfMonth(1) to lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
            }
        }
        setDateRange(from, to)
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Preset periods for quick selection.
     */
    enum class Period {
        TODAY,
        YESTERDAY,
        THIS_WEEK,
        LAST_WEEK,
        THIS_MONTH,
        LAST_MONTH
    }
}

/**
 * Factory for PharmacistDashboardViewModel.
 */
class PharmacistDashboardViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PharmacistDashboardViewModel::class.java)) {
            return PharmacistDashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
