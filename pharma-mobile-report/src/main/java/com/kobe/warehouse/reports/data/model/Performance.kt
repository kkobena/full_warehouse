package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Performance report model - matches MobilePerformanceDTO from backend.
 */
@Parcelize
data class Performance(
    @SerializedName("period") val period: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("caTotal") val caTotal: Long,
    @SerializedName("caPreviousPeriod") val caPreviousPeriod: Long,
    @SerializedName("variationPercent") val variationPercent: Double,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("averageBasket") val averageBasket: Long,
    @SerializedName("customersCount") val customersCount: Int,
    @SerializedName("marginTotal") val marginTotal: Long,
    @SerializedName("marginPercent") val marginPercent: Double,
    @SerializedName("paymentMethods") val paymentMethods: List<PaymentMethodSummary>,
    @SerializedName("topProducts") val topProducts: List<TopProductPerformance>,
    @SerializedName("dataPoints") val dataPoints: List<PeriodDataPoint>,
    @SerializedName("generatedAt") val generatedAt: String
) : Parcelable {

    /**
     * Format CA total for display.
     */
    fun getFormattedCATotal(): String {
        return Dashboard.formatAmount(caTotal)
    }

    /**
     * Get variation indicator.
     */
    fun getVariationIndicator(): String {
        return if (variationPercent >= 0) "↗" else "↘"
    }

    /**
     * Check if variation is positive.
     */
    fun isVariationPositive(): Boolean {
        return variationPercent >= 0
    }

    /**
     * Get period display name.
     */
    fun getPeriodDisplayName(): String {
        return when (period) {
            "WEEK" -> "Cette semaine"
            "MONTH" -> "Ce mois"
            "YEAR" -> "Cette année"
            else -> period
        }
    }

    companion object {
        const val PERIOD_WEEK = "WEEK"
        const val PERIOD_MONTH = "MONTH"
        const val PERIOD_YEAR = "YEAR"
    }
}

/**
 * Payment method summary.
 */
@Parcelize
data class PaymentMethodSummary(
    @SerializedName("code") val code: String,
    @SerializedName("label") val label: String,
    @SerializedName("amount") val amount: Long,
    @SerializedName("percent") val percent: Double,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("color") val color: String
) : Parcelable {

    fun getFormattedAmount(): String {
        return Dashboard.formatAmount(amount)
    }

    fun getFormattedPercent(): String {
        return String.format("%.1f%%", percent)
    }
}

/**
 * Top product with performance comparison.
 */
@Parcelize
data class TopProductPerformance(
    @SerializedName("rank") val rank: Int,
    @SerializedName("productId") val productId: Long,
    @SerializedName("productName") val productName: String,
    @SerializedName("codeCip") val codeCip: String?,
    @SerializedName("salesAmount") val salesAmount: Long,
    @SerializedName("quantitySold") val quantitySold: Int,
    @SerializedName("percentOfTotal") val percentOfTotal: Double,
    @SerializedName("variationPercent") val variationPercent: Double
) : Parcelable {

    fun getFormattedSalesAmount(): String {
        return Dashboard.formatAmount(salesAmount)
    }

    fun getVariationIndicator(): String {
        return when {
            variationPercent > 0 -> "↗"
            variationPercent < 0 -> "↘"
            else -> "→"
        }
    }

    fun isVariationPositive(): Boolean {
        return variationPercent >= 0
    }
}

/**
 * Data point for period chart.
 */
@Parcelize
data class PeriodDataPoint(
    @SerializedName("date") val date: String,
    @SerializedName("label") val label: String,
    @SerializedName("caAmount") val caAmount: Long,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("marginAmount") val marginAmount: Long
) : Parcelable
