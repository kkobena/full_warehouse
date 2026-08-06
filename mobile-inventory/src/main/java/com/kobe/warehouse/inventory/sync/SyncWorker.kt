package com.kobe.warehouse.inventory.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.utils.TokenManager

/**
 * Background worker for synchronizing locally modified inventory lines.
 * The offline-first write path is in InventoryRepository; this worker only
 * pushes what is still pending (batch endpoint).
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tokenManager = TokenManager(applicationContext)
    private val inventoryRepository = InventoryRepository(applicationContext)

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")

        if (!tokenManager.isAuthenticated()) {
            Log.w(TAG, "User not authenticated, skipping sync")
            return Result.success()
        }

        // Les lots comptés hors ligne partent aussi : en mode gestion des lots,
        // c'est la seule forme de saisie
        inventoryRepository.syncPendingLotLines()

        return inventoryRepository.syncPendingLines(includeErrors = false).fold(
            onSuccess = { batchResult ->
                Log.d(TAG, "Synced: ${batchResult.saved} saved, ${batchResult.failed} failed")
                // Server-rejected lines are marked ERROR (retried via manual sync only)
                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "Sync failed, will retry: ${error.message}", error)
                Result.retry()
            }
        )
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "inventory_sync_work"
    }
}
