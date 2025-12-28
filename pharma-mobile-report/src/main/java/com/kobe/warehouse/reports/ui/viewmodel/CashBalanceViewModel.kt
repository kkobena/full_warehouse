package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.CashBalance
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel for Cash Balance (Balance Caisse) screen.
 */
class CashBalanceViewModel(
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

    private val _cashBalance = MutableLiveData<CashBalance?>(null)
    val cashBalance: LiveData<CashBalance?> = _cashBalance

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Period state
    private val _currentPeriod = MutableLiveData(Period.TODAY)
    val currentPeriod: LiveData<Period> = _currentPeriod

    private var fromDate: LocalDate = LocalDate.now()
    private var toDate: LocalDate = LocalDate.now()

    // Expansion state for sections
    private val _totauxExpanded = MutableLiveData(true)
    val totauxExpanded: LiveData<Boolean> = _totauxExpanded

    private val _paymentBreakdownExpanded = MutableLiveData(true)
    val paymentBreakdownExpanded: LiveData<Boolean> = _paymentBreakdownExpanded

    private val _categoriesExpanded = MutableLiveData(false)
    val categoriesExpanded: LiveData<Boolean> = _categoriesExpanded

    private val _mouvementsExpanded = MutableLiveData(false)
    val mouvementsExpanded: LiveData<Boolean> = _mouvementsExpanded

    private val _margeExpanded = MutableLiveData(false)
    val margeExpanded: LiveData<Boolean> = _margeExpanded

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
     * Load cash balance data for the current period.
     */
    fun loadCashBalance() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getCashBalance(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter)
            )

            result.onSuccess { balance ->
                _cashBalance.value = balance
                _isEmpty.value = balance.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh cash balance data.
     */
    fun refreshCashBalance() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getCashBalance(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter)
            )

            result.onSuccess { balance ->
                _cashBalance.value = balance
                _isEmpty.value = balance.isEmpty()
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

        loadCashBalance()
    }

    /**
     * Set custom date range.
     */
    fun setDateRange(from: LocalDate, to: LocalDate) {
        fromDate = from
        toDate = to
        _currentPeriod.value = null  // Custom range
        loadCashBalance()
    }

    /**
     * Toggle totaux section expansion.
     */
    fun toggleTotaux() {
        _totauxExpanded.value = !(_totauxExpanded.value ?: true)
    }

    /**
     * Toggle payment breakdown section expansion.
     */
    fun togglePaymentBreakdown() {
        _paymentBreakdownExpanded.value = !(_paymentBreakdownExpanded.value ?: true)
    }

    /**
     * Toggle categories section expansion.
     */
    fun toggleCategories() {
        _categoriesExpanded.value = !(_categoriesExpanded.value ?: false)
    }

    /**
     * Toggle mouvements section expansion.
     */
    fun toggleMouvements() {
        _mouvementsExpanded.value = !(_mouvementsExpanded.value ?: false)
    }

    /**
     * Toggle marge section expansion.
     */
    fun toggleMarge() {
        _margeExpanded.value = !(_margeExpanded.value ?: false)
    }

    /**
     * Get the current from date.
     */
    fun getFromDate(): LocalDate = fromDate

    /**
     * Get the current to date.
     */
    fun getToDate(): LocalDate = toDate

    /**
     * Get formatted period label.
     */
    fun getPeriodLabel(): String {
        val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return when (_currentPeriod.value) {
            Period.TODAY -> "Aujourd'hui"
            Period.YESTERDAY -> "Hier"
            Period.THIS_WEEK -> "Cette semaine"
            Period.LAST_WEEK -> "Semaine derniere"
            Period.THIS_MONTH -> "Ce mois"
            Period.LAST_MONTH -> "Mois dernier"
            else -> "${fromDate.format(displayFormatter)} - ${toDate.format(displayFormatter)}"
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
 * Factory for creating CashBalanceViewModel instances.
 */
class CashBalanceViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CashBalanceViewModel::class.java)) {
            return CashBalanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
