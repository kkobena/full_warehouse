package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.CashSummary
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel for Cash Summary (Ticket Z / Récapitulatif Caisse) screen.
 */
class CashSummaryViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // State
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _cashSummary = MutableLiveData<CashSummary?>(null)
    val cashSummary: LiveData<CashSummary?> = _cashSummary

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Period state
    private val _currentPeriod = MutableLiveData(Period.TODAY)
    val currentPeriod: LiveData<Period> = _currentPeriod

    private var fromDate: LocalDate = LocalDate.now()
    private var toDate: LocalDate = LocalDate.now()

    // Filter options
    private var onlyVente: Boolean = false
    private var selectedUserIds: List<Int>? = null

    /**
     * Period options for the date selector.
     */
    enum class Period {
        TODAY,
        YESTERDAY,
        THIS_WEEK,
        LAST_WEEK,
        THIS_MONTH,
        LAST_MONTH
    }

    /**
     * Load cash summary data for the current period.
     */
    fun loadCashSummary() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getCashSummary(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter),
                userIds = selectedUserIds,
                onlyVente = onlyVente
            )

            result.onSuccess { summary ->
                _cashSummary.value = summary
                _isEmpty.value = summary.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh cash summary data.
     */
    fun refreshCashSummary() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getCashSummary(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter),
                userIds = selectedUserIds,
                onlyVente = onlyVente
            )

            result.onSuccess { summary ->
                _cashSummary.value = summary
                _isEmpty.value = summary.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            _isRefreshing.value = false
        }
    }

    /**
     * Set the period and reload data.
     */
    fun setPeriod(period: Period) {
        val today = LocalDate.now()
        _currentPeriod.value = period

        when (period) {
            Period.TODAY -> {
                fromDate = today
                toDate = today
            }
            Period.YESTERDAY -> {
                fromDate = today.minusDays(1)
                toDate = today.minusDays(1)
            }
            Period.THIS_WEEK -> {
                fromDate = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                toDate = today
            }
            Period.LAST_WEEK -> {
                val startOfThisWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                fromDate = startOfThisWeek.minusDays(7)
                toDate = startOfThisWeek.minusDays(1)
            }
            Period.THIS_MONTH -> {
                fromDate = today.withDayOfMonth(1)
                toDate = today
            }
            Period.LAST_MONTH -> {
                val firstOfThisMonth = today.withDayOfMonth(1)
                fromDate = firstOfThisMonth.minusMonths(1)
                toDate = firstOfThisMonth.minusDays(1)
            }
        }

        loadCashSummary()
    }

    /**
     * Set custom date range.
     */
    fun setDateRange(from: LocalDate, to: LocalDate) {
        fromDate = from
        toDate = to
        _currentPeriod.value = null  // Custom range
        loadCashSummary()
    }

    /**
     * Set onlyVente filter.
     */
    fun setOnlyVente(onlyVente: Boolean) {
        this.onlyVente = onlyVente
        loadCashSummary()
    }

    /**
     * Set user filter.
     */
    fun setUserFilter(userIds: List<Int>?) {
        this.selectedUserIds = userIds
        loadCashSummary()
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for creating CashSummaryViewModel instances.
 */
class CashSummaryViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CashSummaryViewModel::class.java)) {
            return CashSummaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
