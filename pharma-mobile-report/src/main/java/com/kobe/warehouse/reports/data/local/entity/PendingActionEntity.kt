package com.kobe.warehouse.reports.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing pending actions to be synced when online.
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "action_type")
    val actionType: String,

    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "status")
    val status: String = STATUS_PENDING
) {
    companion object {
        // Action types
        const val ACTION_RESOLVE_ALERT = "resolve_alert"
        const val ACTION_CREATE_ORDER = "create_order"
        const val ACTION_UPDATE_PRODUCT = "update_product"
        const val ACTION_MARK_TODO_DONE = "mark_todo_done"

        // Status
        const val STATUS_PENDING = "pending"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_FAILED = "failed"
        const val STATUS_COMPLETED = "completed"

        // Max retry count
        const val MAX_RETRY_COUNT = 3
    }

    /**
     * Check if action can be retried.
     */
    fun canRetry(): Boolean {
        return retryCount < MAX_RETRY_COUNT && status != STATUS_COMPLETED
    }

    /**
     * Check if action has failed permanently.
     */
    fun hasFailed(): Boolean {
        return retryCount >= MAX_RETRY_COUNT || status == STATUS_FAILED
    }

    /**
     * Create a copy with incremented retry count.
     */
    fun withRetry(errorMessage: String): PendingActionEntity {
        return copy(
            retryCount = retryCount + 1,
            lastAttemptAt = System.currentTimeMillis(),
            errorMessage = errorMessage,
            status = if (retryCount + 1 >= MAX_RETRY_COUNT) STATUS_FAILED else STATUS_PENDING
        )
    }

    /**
     * Mark as in progress.
     */
    fun markInProgress(): PendingActionEntity {
        return copy(
            status = STATUS_IN_PROGRESS,
            lastAttemptAt = System.currentTimeMillis()
        )
    }

    /**
     * Mark as completed.
     */
    fun markCompleted(): PendingActionEntity {
        return copy(status = STATUS_COMPLETED)
    }
}
