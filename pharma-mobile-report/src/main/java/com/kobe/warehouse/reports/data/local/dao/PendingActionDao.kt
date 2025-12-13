package com.kobe.warehouse.reports.data.local.dao

import androidx.room.*
import com.kobe.warehouse.reports.data.local.entity.PendingActionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for pending actions.
 */
@Dao
interface PendingActionDao {

    @Query("SELECT * FROM pending_actions WHERE status = :status ORDER BY created_at ASC")
    suspend fun getPendingActionsByStatus(status: String): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions WHERE status != 'completed' ORDER BY created_at ASC")
    suspend fun getAllPendingActions(): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions WHERE status != 'completed' ORDER BY created_at ASC")
    fun observePendingActions(): Flow<List<PendingActionEntity>>

    @Query("SELECT * FROM pending_actions WHERE id = :id")
    suspend fun getPendingActionById(id: Long): PendingActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingActionEntity): Long

    @Update
    suspend fun updatePendingAction(action: PendingActionEntity)

    @Delete
    suspend fun deletePendingAction(action: PendingActionEntity)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deletePendingActionById(id: Long)

    @Query("DELETE FROM pending_actions WHERE status = 'completed'")
    suspend fun deleteCompletedActions()

    @Query("DELETE FROM pending_actions WHERE status = 'failed' AND retry_count >= :maxRetry")
    suspend fun deleteFailedActions(maxRetry: Int = PendingActionEntity.MAX_RETRY_COUNT)

    @Query("DELETE FROM pending_actions")
    suspend fun deleteAllPendingActions()

    @Query("SELECT COUNT(*) FROM pending_actions WHERE status = :status")
    suspend fun getPendingActionCountByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM pending_actions WHERE status != 'completed'")
    suspend fun getActivePendingActionCount(): Int

    /**
     * Get actions that can be retried (not failed permanently).
     */
    @Query("SELECT * FROM pending_actions WHERE status = 'pending' AND retry_count < :maxRetry ORDER BY created_at ASC")
    suspend fun getRetryableActions(maxRetry: Int = PendingActionEntity.MAX_RETRY_COUNT): List<PendingActionEntity>

    /**
     * Get failed actions for review.
     */
    @Query("SELECT * FROM pending_actions WHERE status = 'failed' ORDER BY created_at DESC")
    suspend fun getFailedActions(): List<PendingActionEntity>
}
