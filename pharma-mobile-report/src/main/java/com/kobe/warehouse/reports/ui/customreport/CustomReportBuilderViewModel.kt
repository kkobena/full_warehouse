package com.kobe.warehouse.reports.ui.customreport

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.api.RetrofitClient
import com.kobe.warehouse.reports.data.local.AppDatabase
import com.kobe.warehouse.reports.data.model.CustomReport
import com.kobe.warehouse.reports.data.model.ReportMetric
import com.kobe.warehouse.reports.data.model.ReportPeriod
import com.kobe.warehouse.reports.data.repository.CustomReportRepository
import com.kobe.warehouse.reports.service.CustomReportService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Custom Report Builder
 * Manages custom report creation and saved reports
 */
class CustomReportBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val customReportService: CustomReportService
    private val repository: CustomReportRepository

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    private val _savedReports = MutableStateFlow<List<CustomReport>>(emptyList())
    val savedReports: StateFlow<List<CustomReport>> = _savedReports.asStateFlow()

    init {
        val apiService = RetrofitClient.getInstance(application).create()
        val database = AppDatabase.getInstance(application)

        customReportService = CustomReportService(application, apiService)
        repository = CustomReportRepository(application, database, customReportService)

        loadSavedReports()
    }

    /**
     * Generate a custom report
     */
    fun generateReport(
        name: String,
        metrics: List<ReportMetric>,
        period: ReportPeriod
    ) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading

            val report = CustomReport(
                name = name,
                selectedMetrics = metrics,
                period = period
            )

            val result = repository.generateReport(report)

            _reportState.value = result.fold(
                onSuccess = { data ->
                    // Save generated report to database
                    val savedId = repository.saveGeneratedReport(report, data)
                    ReportState.Success(savedId)
                },
                onFailure = { error ->
                    ReportState.Error(error.message ?: "Erreur lors de la génération du rapport")
                }
            )
        }
    }

    /**
     * Save a custom report template
     */
    fun saveReport(
        name: String,
        metrics: List<ReportMetric>,
        period: ReportPeriod,
        description: String = ""
    ) {
        viewModelScope.launch {
            val report = CustomReport(
                name = name,
                description = description,
                selectedMetrics = metrics,
                period = period
            )

            repository.saveReportTemplate(report)
            loadSavedReports()
        }
    }

    /**
     * Load saved report templates
     */
    private fun loadSavedReports() {
        viewModelScope.launch {
            _savedReports.value = repository.getSavedReports()
        }
    }

    /**
     * Delete a saved report
     */
    fun deleteReport(report: CustomReport) {
        viewModelScope.launch {
            repository.deleteReport(report)
            loadSavedReports()
        }
    }

    /**
     * Toggle favorite status
     */
    fun toggleFavorite(report: CustomReport) {
        viewModelScope.launch {
            repository.updateReport(report.copy(isFavorite = !report.isFavorite))
            loadSavedReports()
        }
    }

    /**
     * Report generation state
     */
    sealed class ReportState {
        object Idle : ReportState()
        object Loading : ReportState()
        data class Success(val reportId: Long) : ReportState()
        data class Error(val message: String) : ReportState()
    }
}
