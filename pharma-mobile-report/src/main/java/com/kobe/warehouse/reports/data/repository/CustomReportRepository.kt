package com.kobe.warehouse.reports.data.repository

import android.content.Context
import com.google.gson.Gson
import com.kobe.warehouse.reports.data.local.AppDatabase
import com.kobe.warehouse.reports.data.local.entity.CachedReportEntity
import com.kobe.warehouse.reports.data.model.CustomReport
import com.kobe.warehouse.reports.data.model.CustomReportData
import com.kobe.warehouse.reports.service.CustomReportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing custom reports
 * Handles report generation, storage, and retrieval
 */
class CustomReportRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val customReportService: CustomReportService
) {

    private val gson = Gson()

    companion object {
        private const val REPORT_TYPE_CUSTOM = "custom_report"
        private const val REPORT_TYPE_CUSTOM_TEMPLATE = "custom_template"
    }

    /**
     * Generate custom report data
     */
    suspend fun generateReport(report: CustomReport): Result<CustomReportData> {
        return customReportService.generateReport(report)
    }

    /**
     * Save generated report to database
     */
    suspend fun saveGeneratedReport(report: CustomReport, data: CustomReportData): Long = withContext(Dispatchers.IO) {
        val reportJson = gson.toJson(data)
        val reportKey = "report_${System.currentTimeMillis()}"

        val entity = CachedReportEntity(
            reportType = REPORT_TYPE_CUSTOM,
            reportKey = reportKey,
            dataJson = reportJson,
            cachedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (7 * 24 * 3600 * 1000) // 7 days
        )

        database.cachedReportDao().insert(entity)
    }

    /**
     * Save custom report template
     */
    suspend fun saveReportTemplate(report: CustomReport): Long = withContext(Dispatchers.IO) {
        val reportJson = gson.toJson(report)
        val reportKey = "template_${System.currentTimeMillis()}"

        val entity = CachedReportEntity(
            reportType = REPORT_TYPE_CUSTOM_TEMPLATE,
            reportKey = reportKey,
            dataJson = reportJson,
            cachedAt = System.currentTimeMillis(),
            expiresAt = Long.MAX_VALUE // Templates don't expire
        )

        database.cachedReportDao().insert(entity)
    }

    /**
     * Get all saved report templates
     */
    suspend fun getSavedReports(): List<CustomReport> = withContext(Dispatchers.IO) {
        val entities = database.cachedReportDao().getReportsByType(REPORT_TYPE_CUSTOM_TEMPLATE)

        entities.mapNotNull { entity ->
            try {
                gson.fromJson(entity.dataJson, CustomReport::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Get generated report by ID
     */
    suspend fun getGeneratedReport(reportId: Long): CustomReportData? = withContext(Dispatchers.IO) {
        val entity = database.cachedReportDao().getReportById(reportId)

        entity?.let {
            try {
                gson.fromJson(it.dataJson, CustomReportData::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Update report template
     */
    suspend fun updateReport(report: CustomReport) = withContext(Dispatchers.IO) {
        // Find and update existing template
        val entities = database.cachedReportDao().getReportsByType(REPORT_TYPE_CUSTOM_TEMPLATE)
        val existingEntity = entities.find { entity ->
            try {
                val existingReport = gson.fromJson(entity.dataJson, CustomReport::class.java)
                existingReport.id == report.id
            } catch (e: Exception) {
                false
            }
        }

        if (existingEntity != null) {
            val updatedEntity = existingEntity.copy(
                dataJson = gson.toJson(report),
                cachedAt = System.currentTimeMillis()
            )
            database.cachedReportDao().update(updatedEntity)
        }
    }

    /**
     * Delete report template
     */
    suspend fun deleteReport(report: CustomReport) = withContext(Dispatchers.IO) {
        val entities = database.cachedReportDao().getReportsByType(REPORT_TYPE_CUSTOM_TEMPLATE)
        val entityToDelete = entities.find { entity ->
            try {
                val existingReport = gson.fromJson(entity.dataJson, CustomReport::class.java)
                existingReport.id == report.id
            } catch (e: Exception) {
                false
            }
        }

        entityToDelete?.let {
            database.cachedReportDao().delete(it)
        }
    }

    /**
     * Get recent generated reports
     */
    suspend fun getRecentReports(limit: Int = 10): List<CustomReportData> = withContext(Dispatchers.IO) {
        val entities = database.cachedReportDao()
            .getReportsByType(REPORT_TYPE_CUSTOM)
            .sortedByDescending { it.cachedAt }
            .take(limit)

        entities.mapNotNull { entity ->
            try {
                gson.fromJson(entity.dataJson, CustomReportData::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Clear old generated reports (keep only last 30 days)
     */
    suspend fun clearOldReports() = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 3600 * 1000L)
        database.cachedReportDao().deleteExpiredReports(thirtyDaysAgo)
    }
}
