package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Alert type enumeration.
 */
enum class AlertTypeEnum(val code: String, val libelle: String, val icon: String) {
    STOCK_RUPTURE("STOCK_RUPTURE", "Rupture de stock", "alert-circle"),
    STOCK_LOW("STOCK_LOW", "Stock faible", "alert-triangle"),
    EXPIRY("EXPIRY", "Peremption proche", "clock-alert"),
    CASH_DISCREPANCY("CASH_DISCREPANCY", "Ecart de caisse", "cash-register"),
    INVOICE_OVERDUE("INVOICE_OVERDUE", "Facture impayee", "file-document-alert");

    companion object {
        fun fromCode(code: String): AlertTypeEnum? {
            return entries.find { it.code == code }
        }
    }
}

/**
 * Alert severity enumeration.
 */
enum class AlertSeverityEnum(val code: String, val libelle: String, val color: String) {
    CRITICAL("CRITICAL", "Critique", "#991B1B"),
    WARNING("WARNING", "Attention", "#FFC107"),
    INFO("INFO", "Information", "#17A2B8");

    companion object {
        fun fromCode(code: String): AlertSeverityEnum {
            return entries.find { it.code == code } ?: INFO
        }
    }

    fun getEmoji(): String {
        return when (this) {
            CRITICAL -> "🔴"
            WARNING -> "🟠"
            INFO -> "🔵"
        }
    }
}

/**
 * Detailed alert model - matches MobileAlertDetailDTO from backend.
 */
@Parcelize
data class Alert(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("isResolved") val isResolved: Boolean,
    @SerializedName("actionType") val actionType: String?,
    @SerializedName("actionData") val actionData: @RawValue Map<String, Any>?,
    @SerializedName("relatedEntityId") val relatedEntityId: Long?,
    @SerializedName("relatedEntityType") val relatedEntityType: String?,
    @SerializedName("relatedEntityName") val relatedEntityName: String?
) : Parcelable {

    /**
     * Get the alert type as enum.
     */
    fun getAlertTypeEnum(): AlertTypeEnum? {
        return AlertTypeEnum.fromCode(type)
    }

    /**
     * Get the severity as enum.
     */
    fun getSeverityEnum(): AlertSeverityEnum {
        return AlertSeverityEnum.fromCode(severity)
    }

    /**
     * Get severity indicator emoji.
     */
    fun getSeverityEmoji(): String {
        return getSeverityEnum().getEmoji()
    }

    /**
     * Get severity label.
     */
    fun getSeverityLabel(): String {
        return getSeverityEnum().libelle
    }

    /**
     * Check if alert is critical.
     */
    fun isCritical(): Boolean {
        return getSeverityEnum() == AlertSeverityEnum.CRITICAL
    }

    /**
     * Check if alert has an action.
     */
    fun hasAction(): Boolean {
        return actionType != null && actionData != null
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

    companion object {
        // Alert types
        const val TYPE_STOCK_RUPTURE = "STOCK_RUPTURE"
        const val TYPE_STOCK_LOW = "STOCK_LOW"
        const val TYPE_EXPIRY = "EXPIRY"
        const val TYPE_CASH_DISCREPANCY = "CASH_DISCREPANCY"
        const val TYPE_INVOICE_OVERDUE = "INVOICE_OVERDUE"

        // Severities
        const val SEVERITY_CRITICAL = "CRITICAL"
        const val SEVERITY_WARNING = "WARNING"
        const val SEVERITY_INFO = "INFO"

        // Action types
        const val ACTION_VIEW_PRODUCT = "VIEW_PRODUCT"
        const val ACTION_CALL_CLIENT = "CALL_CLIENT"
        const val ACTION_CREATE_ORDER = "CREATE_ORDER"
    }
}
