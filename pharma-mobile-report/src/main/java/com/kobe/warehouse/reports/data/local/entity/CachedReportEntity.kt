package com.kobe.warehouse.reports.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for caching reports locally.
 */
@Entity(tableName = "cached_reports")
data class CachedReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "report_type")
    val reportType: String,

    @ColumnInfo(name = "report_key")
    val reportKey: String,

    @ColumnInfo(name = "data_json")
    val dataJson: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long
) {
    companion object {
        // Report types
        const val TYPE_DASHBOARD = "dashboard"
        const val TYPE_PERFORMANCE = "performance"
        const val TYPE_ALERTS = "alerts"
        const val TYPE_TODOS = "todos"
        const val TYPE_PRODUCT_DETAIL = "product_detail"

        // TTL (Time To Live) in milliseconds
        const val TTL_SHORT = 5 * 60 * 1000L       // 5 minutes
        const val TTL_MEDIUM = 30 * 60 * 1000L     // 30 minutes
        const val TTL_LONG = 60 * 60 * 1000L       // 1 hour
        const val TTL_DAY = 24 * 60 * 60 * 1000L   // 24 hours
    }

    /**
     * Check if cache is expired.
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    /**
     * Check if cache is still valid.
     */
    fun isValid(): Boolean {
        return !isExpired()
    }

    /**
     * Get remaining time in milliseconds.
     */
    fun getRemainingTime(): Long {
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    }
}
