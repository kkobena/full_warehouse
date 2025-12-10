package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Dashboard data model - matches MobileDashboardDTO from backend.
 */
@Parcelize
data class Dashboard(
    @SerializedName("dailyCA") val dailyCA: Long,
    @SerializedName("dailyTarget") val dailyTarget: Long,
    @SerializedName("variationPercent") val variationPercent: Double,
    @SerializedName("progressPercent") val progressPercent: Int,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("averageBasket") val averageBasket: Long,
    @SerializedName("customersCount") val customersCount: Int,
    @SerializedName("amountCollected") val amountCollected: Long,
    @SerializedName("amountCredit") val amountCredit: Long,
    @SerializedName("marginAmount") val marginAmount: Long,
    @SerializedName("marginPercent") val marginPercent: Double,
    @SerializedName("alerts") val alerts: List<AlertSummary>,
    @SerializedName("alertsCount") val alertsCount: Int,
    @SerializedName("topProducts") val topProducts: List<TopProduct>,
    @SerializedName("caTrend") val caTrend: List<DailyCASummary>,
    @SerializedName("date") val date: String,
    @SerializedName("lastUpdate") val lastUpdate: String
) : Parcelable {

    /**
     * Format CA amount for display.
     */
    fun getFormattedDailyCA(): String {
        return formatAmount(dailyCA)
    }

    /**
     * Format average basket for display.
     */
    fun getFormattedAverageBasket(): String {
        return formatAmount(averageBasket)
    }

    /**
     * Get variation indicator (up/down arrow).
     */
    fun getVariationIndicator(): String {
        return if (variationPercent >= 0) "↗" else "↘"
    }

    /**
     * Get variation color resource name.
     */
    fun isVariationPositive(): Boolean {
        return variationPercent >= 0
    }

    companion object {
        fun formatAmount(amount: Long): String {
            return String.format("%,d", amount).replace(",", " ") + " F"
        }
    }
}

/**
 * Alert summary for dashboard cards.
 */
@Parcelize
data class AlertSummary(
    @SerializedName("type") val type: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("message") val message: String,
    @SerializedName("count") val count: Int,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String
) : Parcelable {

    /**
     * Get severity indicator emoji.
     */
    fun getSeverityEmoji(): String {
        return when (severity) {
            "CRITICAL" -> "🔴"
            "WARNING" -> "🟠"
            "INFO" -> "🟡"
            else -> "⚪"
        }
    }
}

/**
 * Top product summary for dashboard.
 */
@Parcelize
data class TopProduct(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("codeCip") val codeCip: String?,
    @SerializedName("salesAmount") val salesAmount: Long,
    @SerializedName("quantitySold") val quantitySold: Int,
    @SerializedName("rank") val rank: Int
) : Parcelable {

    fun getFormattedSalesAmount(): String {
        return Dashboard.formatAmount(salesAmount)
    }
}

/**
 * Daily CA summary for trend chart.
 */
@Parcelize
data class DailyCASummary(
    @SerializedName("date") val date: String,
    @SerializedName("dayLabel") val dayLabel: String,
    @SerializedName("caTotal") val caTotal: Long,
    @SerializedName("transactionsCount") val transactionsCount: Int
) : Parcelable
