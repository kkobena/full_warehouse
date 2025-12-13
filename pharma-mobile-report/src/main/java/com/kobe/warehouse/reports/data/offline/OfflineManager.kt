package com.kobe.warehouse.reports.data.offline

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.kobe.warehouse.reports.data.local.AppDatabase
import com.kobe.warehouse.reports.data.local.entity.CachedReportEntity
import com.kobe.warehouse.reports.data.local.entity.PendingActionEntity
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages offline caching and pending actions synchronization.
 * Implements offline-first architecture.
 */
class OfflineManager private constructor(
    context: Context,
    private val repository: ReportRepository
) {
    private val database = AppDatabase.getInstance(context)
    private val cachedReportDao = database.cachedReportDao()
    private val pendingActionDao = database.pendingActionDao()
    private val gson = Gson()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "OfflineManager"

        @Volatile
        private var INSTANCE: OfflineManager? = null

        fun getInstance(context: Context, repository: ReportRepository): OfflineManager {
            return INSTANCE ?: synchronized(this) {
                val instance = OfflineManager(context.applicationContext, repository)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Cache a report with TTL.
     */
    suspend fun cacheReport(
        reportType: String,
        reportKey: String,
        data: Any,
        ttl: Long = CachedReportEntity.TTL_MEDIUM
    ) {
        withContext(Dispatchers.IO) {
            try {
                val dataJson = gson.toJson(data)
                val currentTime = System.currentTimeMillis()

                val cachedReport = CachedReportEntity(
                    reportType = reportType,
                    reportKey = reportKey,
                    dataJson = dataJson,
                    cachedAt = currentTime,
                    expiresAt = currentTime + ttl
                )

                cachedReportDao.insertCachedReport(cachedReport)
                Log.d(TAG, "Cached report: $reportType/$reportKey (TTL: ${ttl / 1000}s)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache report: $reportType/$reportKey", e)
            }
        }
    }

    /**
     * Get cached report if valid.
     */
    suspend fun <T> getCachedReport(
        reportType: String,
        reportKey: String,
        clazz: Class<T>
    ): T? {
        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val cachedReport = cachedReportDao.getValidCachedReport(
                    reportType,
                    reportKey,
                    currentTime
                )

                if (cachedReport != null) {
                    Log.d(TAG, "Cache hit: $reportType/$reportKey")
                    gson.fromJson(cachedReport.dataJson, clazz)
                } else {
                    Log.d(TAG, "Cache miss: $reportType/$reportKey")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get cached report: $reportType/$reportKey", e)
                null
            }
        }
    }

    /**
     * Queue an action to be executed when online.
     */
    suspend fun queueAction(actionType: String, payload: Any): Long {
        return withContext(Dispatchers.IO) {
            try {
                val payloadJson = gson.toJson(payload)

                val action = PendingActionEntity(
                    actionType = actionType,
                    payloadJson = payloadJson,
                    createdAt = System.currentTimeMillis()
                )

                val actionId = pendingActionDao.insertPendingAction(action)
                Log.d(TAG, "Queued action: $actionType (id: $actionId)")
                actionId
            } catch (e: Exception) {
                Log.e(TAG, "Failed to queue action: $actionType", e)
                -1L
            }
        }
    }

    /**
     * Sync all pending actions when online.
     */
    suspend fun syncPendingActions(): SyncResult {
        return withContext(Dispatchers.IO) {
            val result = SyncResult()

            try {
                val pendingActions = pendingActionDao.getRetryableActions()
                Log.d(TAG, "Syncing ${pendingActions.size} pending actions")

                for (action in pendingActions) {
                    try {
                        // Mark as in progress
                        pendingActionDao.updatePendingAction(action.markInProgress())

                        // Execute action
                        executeAction(action)

                        // Mark as completed and delete
                        pendingActionDao.deletePendingAction(action)
                        result.successCount++

                        Log.d(TAG, "Synced action: ${action.actionType} (id: ${action.id})")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync action: ${action.actionType}", e)

                        // Update with error
                        val updatedAction = action.withRetry(e.message ?: "Unknown error")
                        pendingActionDao.updatePendingAction(updatedAction)

                        if (updatedAction.hasFailed()) {
                            result.failedCount++
                        } else {
                            result.retryCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                result.error = e.message
            }

            result
        }
    }

    /**
     * Execute a pending action.
     */
    private suspend fun executeAction(action: PendingActionEntity) {
        when (action.actionType) {
            PendingActionEntity.ACTION_RESOLVE_ALERT -> {
                val payload = gson.fromJson(action.payloadJson, ResolveAlertPayload::class.java)
                repository.resolveAlert(payload.alertId)
            }

            PendingActionEntity.ACTION_MARK_TODO_DONE -> {
                val payload = gson.fromJson(action.payloadJson, MarkTodoDonePayload::class.java)
                repository.markTodoDone(payload.todoId)
            }

            PendingActionEntity.ACTION_CREATE_ORDER -> {
                val payload = gson.fromJson(action.payloadJson, CreateOrderPayload::class.java)
                // Implement order creation
                Log.w(TAG, "Order creation not yet implemented")
            }

            PendingActionEntity.ACTION_UPDATE_PRODUCT -> {
                val payload = gson.fromJson(action.payloadJson, UpdateProductPayload::class.java)
                // Implement product update
                Log.w(TAG, "Product update not yet implemented")
            }

            else -> {
                Log.w(TAG, "Unknown action type: ${action.actionType}")
            }
        }
    }

    /**
     * Observe pending actions count.
     */
    fun observePendingActionsCount(): Flow<Int> {
        return kotlinx.coroutines.flow.flow {
            pendingActionDao.observePendingActions().collect { actions ->
                emit(actions.size)
            }
        }
    }

    /**
     * Get pending actions count.
     */
    suspend fun getPendingActionsCount(): Int {
        return pendingActionDao.getActivePendingActionCount()
    }

    /**
     * Clear expired cache.
     */
    suspend fun clearExpiredCache() {
        withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                cachedReportDao.deleteExpiredReports(currentTime)
                Log.d(TAG, "Cleared expired cache")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear expired cache", e)
            }
        }
    }

    /**
     * Clear all cache.
     */
    suspend fun clearAllCache() {
        withContext(Dispatchers.IO) {
            try {
                cachedReportDao.deleteAllCachedReports()
                Log.d(TAG, "Cleared all cache")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all cache", e)
            }
        }
    }

    /**
     * Delete completed actions.
     */
    suspend fun cleanupCompletedActions() {
        withContext(Dispatchers.IO) {
            try {
                pendingActionDao.deleteCompletedActions()
                Log.d(TAG, "Cleaned up completed actions")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup completed actions", e)
            }
        }
    }

    /**
     * Get cache statistics.
     */
    suspend fun getCacheStats(): CacheStats {
        return withContext(Dispatchers.IO) {
            val totalReports = cachedReportDao.getCachedReportCount()
            val validReports = cachedReportDao.getValidCachedReportCount(System.currentTimeMillis())
            val cacheSizeBytes = cachedReportDao.getCacheSizeBytes() ?: 0L
            val pendingActions = pendingActionDao.getActivePendingActionCount()

            CacheStats(
                totalCachedReports = totalReports,
                validCachedReports = validReports,
                cacheSizeBytes = cacheSizeBytes,
                pendingActionsCount = pendingActions
            )
        }
    }

    // Payload classes
    data class ResolveAlertPayload(val alertId: Long)
    data class MarkTodoDonePayload(val todoId: Long)
    data class CreateOrderPayload(val productId: Long, val quantity: Int)
    data class UpdateProductPayload(val productId: Long, val field: String, val value: String)

    // Result classes
    data class SyncResult(
        var successCount: Int = 0,
        var failedCount: Int = 0,
        var retryCount: Int = 0,
        var error: String? = null
    ) {
        val isSuccess: Boolean
            get() = error == null && failedCount == 0

        val total: Int
            get() = successCount + failedCount + retryCount
    }

    data class CacheStats(
        val totalCachedReports: Int,
        val validCachedReports: Int,
        val cacheSizeBytes: Long,
        val pendingActionsCount: Int
    ) {
        val cacheSizeMB: Float
            get() = cacheSizeBytes / (1024f * 1024f)

        val expiredReports: Int
            get() = totalCachedReports - validCachedReports
    }
}
