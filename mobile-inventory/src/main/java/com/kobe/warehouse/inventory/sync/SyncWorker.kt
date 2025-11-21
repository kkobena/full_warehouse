package com.kobe.warehouse.inventory.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kobe.warehouse.inventory.data.database.InventoryDatabase
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.utils.TokenManager

/**
 * Background worker for synchronizing inventory lines with the server
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tokenManager = TokenManager(context)
    private val inventoryRepository = InventoryRepository(tokenManager)
    private val database = InventoryDatabase.getInstance(context)
    private val inventoryLineDao = database.inventoryLineDao()

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")

        return try {
            // Check if user is authenticated
            if (!tokenManager.isAuthenticated()) {
                Log.w(TAG, "User not authenticated, skipping sync")
                return Result.success()
            }

            // Get pending lines from local database
            val pendingLines = inventoryLineDao.getPendingSyncLines()

            if (pendingLines.isEmpty()) {
                Log.d(TAG, "No pending lines to sync")
                return Result.success()
            }

            Log.d(TAG, "Found ${pendingLines.size} pending lines to sync")

            // Convert entities to models
            val linesToSync = pendingLines.map { entity ->
                StoreInventoryLine(
                    id = entity.id,
                    storeInventoryId = entity.storeInventoryId,
                    produitId = entity.produitId,
                    produitLibelle = entity.produitLibelle,
                    produitCip = entity.produitCip,
                    quantityInit = entity.quantityInit,
                    quantityOnHand = entity.quantityOnHand,
                    gap = entity.gap,
                    rayonId = entity.rayonId,
                    rayonLibelle = entity.rayonLibelle,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }

            // Sync to server
            inventoryRepository.synchronizeInventoryLines(linesToSync).fold(
                onSuccess = {
                    Log.d(TAG, "Successfully synced ${linesToSync.size} lines")

                    // Mark lines as synced in local database
                    pendingLines.forEach { entity ->
                        entity.syncStatus = "SYNCED"
                        entity.locallyModified = false
                        inventoryLineDao.updateLine(entity)
                    }

                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to sync lines: ${error.message}", error)

                    // Mark as error but keep for retry
                    pendingLines.forEach { entity ->
                        entity.syncStatus = "ERROR"
                        inventoryLineDao.updateLine(entity)
                    }

                    // Retry on failure
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sync: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "inventory_sync_work"
    }
}
