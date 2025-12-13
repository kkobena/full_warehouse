package com.kobe.warehouse.reports.data.local.dao

import androidx.room.*
import com.kobe.warehouse.reports.data.local.entity.CachedReportEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached reports.
 */
@Dao
interface CachedReportDao {

    @Query("SELECT * FROM cached_reports WHERE report_type = :reportType AND report_key = :reportKey ORDER BY cached_at DESC LIMIT 1")
    suspend fun getCachedReport(reportType: String, reportKey: String): CachedReportEntity?

    @Query("SELECT * FROM cached_reports WHERE report_type = :reportType AND report_key = :reportKey AND expires_at > :currentTime ORDER BY cached_at DESC LIMIT 1")
    suspend fun getValidCachedReport(reportType: String, reportKey: String, currentTime: Long): CachedReportEntity?

    @Query("SELECT * FROM cached_reports WHERE report_type = :reportType ORDER BY cached_at DESC")
    fun getCachedReportsByType(reportType: String): Flow<List<CachedReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedReport(report: CachedReportEntity): Long

    @Update
    suspend fun updateCachedReport(report: CachedReportEntity)

    @Delete
    suspend fun deleteCachedReport(report: CachedReportEntity)

    @Query("DELETE FROM cached_reports WHERE report_type = :reportType AND report_key = :reportKey")
    suspend fun deleteCachedReportByKey(reportType: String, reportKey: String)

    @Query("DELETE FROM cached_reports WHERE expires_at < :currentTime")
    suspend fun deleteExpiredReports(currentTime: Long)

    @Query("DELETE FROM cached_reports WHERE report_type = :reportType")
    suspend fun deleteReportsByType(reportType: String)

    @Query("DELETE FROM cached_reports")
    suspend fun deleteAllCachedReports()

    @Query("SELECT COUNT(*) FROM cached_reports")
    suspend fun getCachedReportCount(): Int

    @Query("SELECT COUNT(*) FROM cached_reports WHERE expires_at > :currentTime")
    suspend fun getValidCachedReportCount(currentTime: Long): Int

    /**
     * Get cache size in bytes (approximate).
     */
    @Query("SELECT SUM(LENGTH(data_json)) FROM cached_reports")
    suspend fun getCacheSizeBytes(): Long?
}
