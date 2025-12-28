package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.model.ActivityReport
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel for Activity Report (Rapport d'Activité) screen.
 */
class ActivityReportViewModel(
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

    private val _activityReport = MutableLiveData<ActivityReport?>(null)
    val activityReport: LiveData<ActivityReport?> = _activityReport

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Period state
    private val _currentPeriod = MutableLiveData(Period.TODAY)
    val currentPeriod: LiveData<Period> = _currentPeriod

    private var fromDate: LocalDate = LocalDate.now()
    private var toDate: LocalDate = LocalDate.now()

    // Expansion state for sections
    private val _chiffreAffaireExpanded = MutableLiveData(true)
    val chiffreAffaireExpanded: LiveData<Boolean> = _chiffreAffaireExpanded

    private val _recettesExpanded = MutableLiveData(true)
    val recettesExpanded: LiveData<Boolean> = _recettesExpanded

    private val _mouvementsExpanded = MutableLiveData(false)
    val mouvementsExpanded: LiveData<Boolean> = _mouvementsExpanded

    private val _achatsExpanded = MutableLiveData(false)
    val achatsExpanded: LiveData<Boolean> = _achatsExpanded

    private val _tiersPayantsExpanded = MutableLiveData(false)
    val tiersPayantsExpanded: LiveData<Boolean> = _tiersPayantsExpanded

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
     * Load activity report data for the current period.
     */
    fun loadActivityReport() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getActivityReport(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter)
            )

            result.onSuccess { report ->
                _activityReport.value = report
                _isEmpty.value = report.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Erreur lors du chargement"
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh activity report data.
     */
    fun refreshActivityReport() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getActivityReport(
                fromDate = fromDate.format(dateFormatter),
                toDate = toDate.format(dateFormatter)
            )

            result.onSuccess { report ->
                _activityReport.value = report
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

        loadActivityReport()
    }

    /**
     * Set custom date range.
     */
    fun setDateRange(from: LocalDate, to: LocalDate) {
        fromDate = from
        toDate = to
        _currentPeriod.value = null  // Custom range
        loadActivityReport()
    }

    /**
     * Toggle chiffre d'affaires section expansion.
     */
    fun toggleChiffreAffaire() {
        _chiffreAffaireExpanded.value = !(_chiffreAffaireExpanded.value ?: true)
    }

    /**
     * Toggle recettes section expansion.
     */
    fun toggleRecettes() {
        _recettesExpanded.value = !(_recettesExpanded.value ?: true)
    }

    /**
     * Toggle mouvements section expansion.
     */
    fun toggleMouvements() {
        _mouvementsExpanded.value = !(_mouvementsExpanded.value ?: false)
    }

    /**
     * Toggle achats section expansion.
     */
    fun toggleAchats() {
        _achatsExpanded.value = !(_achatsExpanded.value ?: false)
    }

    /**
     * Toggle tiers payants section expansion.
     */
    fun toggleTiersPayants() {
        _tiersPayantsExpanded.value = !(_tiersPayantsExpanded.value ?: false)
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
            Period.LAST_WEEK -> "Semaine dernière"
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
 * Factory for creating ActivityReportViewModel instances.
 */
class ActivityReportViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityReportViewModel::class.java)) {
            return ActivityReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
