package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Supplier performance model.
 * Maps to backend SupplierPerformanceDTO.
 */
data class SupplierPerformance(
    @SerializedName("fournisseurId")
    val fournisseurId: Int,
    @SerializedName("fournisseurName")
    val fournisseurName: String,
    @SerializedName("fournisseurCode")
    val fournisseurCode: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("mobile")
    val mobile: String?,
    @SerializedName("nbOrdersLast30Days")
    val nbOrdersLast30Days: Int,
    @SerializedName("purchaseAmountLast30Days")
    val purchaseAmountLast30Days: Long,
    @SerializedName("nbOrdersLast12Months")
    val nbOrdersLast12Months: Int,
    @SerializedName("purchaseAmountLast12Months")
    val purchaseAmountLast12Months: Long,
    @SerializedName("avgDeliveryDays")
    val avgDeliveryDays: Int?,
    @SerializedName("minDeliveryDays")
    val minDeliveryDays: Int?,
    @SerializedName("maxDeliveryDays")
    val maxDeliveryDays: Int?,
    @SerializedName("conformityRatePct")
    val conformityRatePct: BigDecimal?,
    @SerializedName("performanceScore")
    val performanceScore: BigDecimal?
) {
    /**
     * Get performance score as integer (0-100).
     */
    fun getScoreAsInt(): Int {
        return performanceScore?.toInt() ?: 0
    }

    /**
     * Get performance level based on score.
     */
    fun getPerformanceLevel(): PerformanceLevel {
        val score = getScoreAsInt()
        return when {
            score >= 80 -> PerformanceLevel.EXCELLENT
            score >= 60 -> PerformanceLevel.GOOD
            score >= 40 -> PerformanceLevel.AVERAGE
            else -> PerformanceLevel.POOR
        }
    }

    /**
     * Get color for performance score.
     */
    fun getScoreColor(): Int {
        return when (getPerformanceLevel()) {
            PerformanceLevel.EXCELLENT -> android.graphics.Color.parseColor("#4CAF50")
            PerformanceLevel.GOOD -> android.graphics.Color.parseColor("#8BC34A")
            PerformanceLevel.AVERAGE -> android.graphics.Color.parseColor("#FF9800")
            PerformanceLevel.POOR -> android.graphics.Color.parseColor("#F44336")
        }
    }

    enum class PerformanceLevel {
        EXCELLENT, GOOD, AVERAGE, POOR
    }
}

/**
 * Supplier performance summary.
 * Maps to backend SupplierPerformanceSummaryDTO.
 */
data class SupplierPerformanceSummary(
    @SerializedName("totalSuppliers")
    val totalSuppliers: Int,
    @SerializedName("totalPurchaseAmount30Days")
    val totalPurchaseAmount30Days: Long,
    @SerializedName("totalPurchaseAmount12Months")
    val totalPurchaseAmount12Months: Long,
    @SerializedName("avgPerformanceScore")
    val avgPerformanceScore: BigDecimal?,
    @SerializedName("avgConformityRate")
    val avgConformityRate: BigDecimal?,
    @SerializedName("avgDeliveryDays")
    val avgDeliveryDays: BigDecimal?,
    @SerializedName("suppliersWithIssues")
    val suppliersWithIssues: Int,
    @SerializedName("excellentSuppliers")
    val excellentSuppliers: Int,
    @SerializedName("goodSuppliers")
    val goodSuppliers: Int,
    @SerializedName("averageSuppliers")
    val averageSuppliers: Int,
    @SerializedName("poorSuppliers")
    val poorSuppliers: Int
)
