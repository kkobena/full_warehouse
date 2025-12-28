package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.TvaReport
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel for TVA Report screen.
 */
class TvaReportViewModel(
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

    private val _tvaReport = MutableLiveData<TvaReport?>(null)
    val tvaReport: LiveData<TvaReport?> = _tvaReport

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Period state
    private val _currentPeriod = MutableLiveData(Period.TODAY)
    val currentPeriod: LiveData<Period> = _currentPeriod

    private var fromDate: LocalDate = LocalDate.now()
    private var toDate: LocalDate = LocalDate.now()

    // Group by date option
    private val _groupByDate = MutableLiveData(false)
    val groupByDate: LiveData<Boolean> = _groupByDate

    // Expansion state for sections
    private val _totauxExpanded = MutableLiveData(true)
    val totauxExpanded: LiveData<Boolean> = _totauxExpanded

    private val _breakdownExpanded = MutableLiveData(true)
    val breakdownExpanded: LiveData<Boolean> = _breakdownExpanded

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
     * Load TVA report data for the current period.
     */
    fun loadTvaReport() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getTvaReport(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter),
                groupByDate = _groupByDate.value ?: false
            )

            result.onSuccess { report ->
                _tvaReport.value = report
                _isEmpty.value = report.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh TVA report data.
     */
    fun refreshTvaReport() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getTvaReport(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter),
                groupByDate = _groupByDate.value ?: false
            )

            result.onSuccess { report ->
                _tvaReport.value = report
                _isEmpty.value = report.isEmpty()
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

        loadTvaReport()
    }

    /**
     * Set custom date range.
     */
    fun setDateRange(from: LocalDate, to: LocalDate) {
        fromDate = from
        toDate = to
        _currentPeriod.value = null  // Custom range
        loadTvaReport()
    }

    /**
     * Toggle group by date and reload.
     */
    fun toggleGroupByDate() {
        _groupByDate.value = !(_groupByDate.value ?: false)
        loadTvaReport()
    }

    /**
     * Set group by date value.
     */
    fun setGroupByDate(value: Boolean) {
        if (_groupByDate.value != value) {
            _groupByDate.value = value
            loadTvaReport()
        }
    }

    /**
     * Toggle totaux section expansion.
     */
    fun toggleTotaux() {
        _totauxExpanded.value = !(_totauxExpanded.value ?: true)
    }

    /**
     * Toggle breakdown section expansion.
     */
    fun toggleBreakdown() {
        _breakdownExpanded.value = !(_breakdownExpanded.value ?: true)
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
 * Factory for creating TvaReportViewModel instances.
 */
class TvaReportViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TvaReportViewModel::class.java)) {
            return TvaReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
