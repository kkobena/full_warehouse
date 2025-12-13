package com.kobe.warehouse.reports.data.offline

import android.content.Context
import android.util.Log
import androidx.work.*
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Manages automatic synchronization of pending actions when online.
 * Uses WorkManager for reliable background sync.
 */
class SyncManager private constructor(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val networkManager: NetworkManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_WORK_NAME = "pharma_sync_work"
        private const val PERIODIC_SYNC_INTERVAL_MINUTES = 30L

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(
            context: Context,
            offlineManager: OfflineManager,
            networkManager: NetworkManager
        ): SyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SyncManager(
                    context.applicationContext,
                    offlineManager,
                    networkManager
                )
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        // Observe network connectivity and sync when online
        scope.launch {
            networkManager.observeNetworkConnectivity().collect { isOnline ->
                if (isOnline) {
                    Log.d(TAG, "Network available, triggering sync")
                    syncPendingActions()
                }
            }
        }

        // Schedule periodic sync
        schedulePeriodicSync()
    }

    /**
     * Sync pending actions immediately.
     */
    suspend fun syncPendingActions() {
        if (!networkManager.isOnline()) {
            Log.d(TAG, "Device offline, skipping sync")
            _syncState.value = SyncState.Error("Device offline")
            return
        }

        if (_syncState.value is SyncState.Syncing) {
            Log.d(TAG, "Sync already in progress")
            return
        }

        try {
            _syncState.value = SyncState.Syncing

            val result = offlineManager.syncPendingActions()

            if (result.isSuccess) {
                _syncState.value = SyncState.Success(result)
                _lastSyncTime.value = System.currentTimeMillis()
                Log.d(TAG, "Sync completed: ${result.successCount} successful")
            } else {
                _syncState.value = SyncState.Error(result.error ?: "Unknown error")
                Log.e(TAG, "Sync failed: ${result.error}")
            }

            // Cleanup completed actions
            offlineManager.cleanupCompletedActions()

        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            Log.e(TAG, "Sync exception", e)
        } finally {
            // Reset to idle after a delay
            kotlinx.coroutines.delay(2000)
            if (_syncState.value !is SyncState.Syncing) {
                _syncState.value = SyncState.Idle
            }
        }
    }

    /**
     * Schedule periodic background sync using WorkManager.
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )

        Log.d(TAG, "Periodic sync scheduled (every $PERIODIC_SYNC_INTERVAL_MINUTES minutes)")
    }

    /**
     * Trigger immediate one-time sync using WorkManager.
     */
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${SYNC_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            oneTimeSyncRequest
        )

        Log.d(TAG, "Immediate sync triggered")
    }

    /**
     * Cancel all scheduled sync work.
     */
    fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        Log.d(TAG, "Sync cancelled")
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        data class Success(val result: OfflineManager.SyncResult) : SyncState()
        data class Error(val message: String) : SyncState()
    }
}

/**
 * WorkManager Worker for background synchronization.
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Sync work started")

        return try {
            // Get dependencies (in production, use DI like Hilt)
            val repository = ReportRepository.getInstance(applicationContext)
            val offlineManager = OfflineManager.getInstance(applicationContext, repository)
            val networkManager = NetworkManager.getInstance(applicationContext)

            if (!networkManager.isOnline()) {
                Log.d(TAG, "Device offline, rescheduling work")
                return Result.retry()
            }

            val syncResult = offlineManager.syncPendingActions()

            if (syncResult.isSuccess) {
                Log.d(TAG, "Sync work succeeded: ${syncResult.successCount} actions synced")
                Result.success()
            } else if (syncResult.retryCount > 0) {
                Log.d(TAG, "Sync work partially failed, retrying")
                Result.retry()
            } else {
                Log.e(TAG, "Sync work failed: ${syncResult.error}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync work exception", e)
            Result.retry()
        }
    }
}
