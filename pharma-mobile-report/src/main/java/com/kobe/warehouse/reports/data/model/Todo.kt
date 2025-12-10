package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Todo type enumeration.
 */
enum class TodoTypeEnum(val code: String, val libelle: String, val icon: String, val actionType: String) {
    REORDER("REORDER", "Commander", "package-variant", "CREATE_ORDER"),
    CALL_CLIENT("CALL_CLIENT", "Relancer client", "phone", "CALL"),
    CREATE_DISCOUNT("CREATE_DISCOUNT", "Creer promotion", "tag-outline", "CREATE_DISCOUNT"),
    INVENTORY("INVENTORY", "Inventaire", "clipboard-check-outline", "START_INVENTORY");

    companion object {
        fun fromCode(code: String): TodoTypeEnum? {
            return entries.find { it.code == code }
        }
    }
}

/**
 * Todo priority enumeration.
 */
enum class TodoPriorityEnum(val code: String, val libelle: String, val color: String) {
    URGENT("URGENT", "Urgent", "#DC3545"),
    IMPORTANT("IMPORTANT", "Important", "#FFC107"),
    NORMAL("NORMAL", "Normal", "#007BFF");

    companion object {
        fun fromCode(code: String): TodoPriorityEnum {
            return entries.find { it.code == code } ?: NORMAL
        }
    }

    fun getEmoji(): String {
        return when (this) {
            URGENT -> "🔴"
            IMPORTANT -> "🟠"
            NORMAL -> "🔵"
        }
    }
}

/**
 * Todo list model - matches MobileTodoDTO from backend.
 */
@Parcelize
data class TodoList(
    @SerializedName("urgent") val urgent: List<TodoItem>,
    @SerializedName("important") val important: List<TodoItem>,
    @SerializedName("normal") val normal: List<TodoItem>,
    @SerializedName("totalCount") val totalCount: Int,
    @SerializedName("generatedAt") val generatedAt: String
) : Parcelable {

    /**
     * Get all items as a flat list.
     */
    fun getAllItems(): List<TodoItem> {
        return urgent + important + normal
    }

    /**
     * Check if there are any items.
     */
    fun isEmpty(): Boolean {
        return totalCount == 0
    }

    /**
     * Get urgent count.
     */
    fun getUrgentCount(): Int {
        return urgent.size
    }
}

/**
 * Single todo item.
 */
@Parcelize
data class TodoItem(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String?,
    @SerializedName("actionLabel") val actionLabel: String,
    @SerializedName("actionType") val actionType: String,
    @SerializedName("actionData") val actionData: @RawValue Map<String, Any>?,
    @SerializedName("relatedEntityId") val relatedEntityId: Long?,
    @SerializedName("relatedEntityType") val relatedEntityType: String?,
    @SerializedName("relatedEntityName") val relatedEntityName: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("dueDate") val dueDate: String?,
    @SerializedName("isDismissed") val isDismissed: Boolean
) : Parcelable {

    /**
     * Get the todo type as enum.
     */
    fun getTodoTypeEnum(): TodoTypeEnum? {
        return TodoTypeEnum.fromCode(type)
    }

    /**
     * Get the priority as enum.
     */
    fun getPriorityEnum(): TodoPriorityEnum {
        return TodoPriorityEnum.fromCode(priority)
    }

    /**
     * Get priority emoji.
     */
    fun getPriorityEmoji(): String {
        return getPriorityEnum().getEmoji()
    }

    /**
     * Get priority color resource.
     */
    fun getPriorityColor(): String {
        return getPriorityEnum().color
    }

    /**
     * Get priority label.
     */
    fun getPriorityLabel(): String {
        return getPriorityEnum().libelle
    }

    /**
     * Check if this is an urgent item.
     */
    fun isUrgent(): Boolean {
        return getPriorityEnum() == TodoPriorityEnum.URGENT
    }

    /**
     * Get product ID from action data if available.
     */
    fun getProductId(): Long? {
        return actionData?.get("productId")?.let {
            when (it) {
                is Double -> it.toLong()
                is Long -> it
                is Int -> it.toLong()
                else -> null
            }
        }
    }

    /**
     * Get phone number from action data if available.
     */
    fun getPhone(): String? {
        return actionData?.get("phone") as? String
    }

    /**
     * Get suggested quantity from action data.
     */
    fun getSuggestedQuantity(): Int? {
        return actionData?.get("suggestedQuantity")?.let {
            when (it) {
                is Double -> it.toInt()
                is Int -> it
                else -> null
            }
        }
    }

    companion object {
        // Todo types
        const val TYPE_REORDER = "REORDER"
        const val TYPE_CALL_CLIENT = "CALL_CLIENT"
        const val TYPE_CREATE_DISCOUNT = "CREATE_DISCOUNT"
        const val TYPE_INVENTORY = "INVENTORY"

        // Action types
        const val ACTION_CREATE_ORDER = "CREATE_ORDER"
        const val ACTION_CALL = "CALL"
        const val ACTION_CREATE_DISCOUNT = "CREATE_DISCOUNT"
        const val ACTION_NAVIGATE = "NAVIGATE"

        // Priorities
        const val PRIORITY_URGENT = "URGENT"
        const val PRIORITY_IMPORTANT = "IMPORTANT"
        const val PRIORITY_NORMAL = "NORMAL"
    }
}
