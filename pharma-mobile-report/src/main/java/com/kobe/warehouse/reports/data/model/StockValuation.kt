package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Stock valuation model.
 * Maps to backend StockValuationDTO.
 */
data class StockValuation(
    @SerializedName("produitId")
    val produitId: Int,
    @SerializedName("libelle")
    val libelle: String,
    @SerializedName("codeCip")
    val codeCip: String?,
    @SerializedName("categorie")
    val categorie: String?,
    @SerializedName("storageLocation")
    val storageLocation: String?,
    @SerializedName("stockQuantity")
    val stockQuantity: Int,
    @SerializedName("purchasePrice")
    val purchasePrice: Int,
    @SerializedName("salesPrice")
    val salesPrice: Int,
    @SerializedName("totalPurchaseValue")
    val totalPurchaseValue: Long,
    @SerializedName("totalSalesValue")
    val totalSalesValue: Long,
    @SerializedName("potentialMargin")
    val potentialMargin: Long,
    @SerializedName("marginPercentage")
    val marginPercentage: BigDecimal?
) {
    /**
     * Get margin percentage as formatted string.
     */
    fun getMarginPercentageFormatted(): String {
        return String.format("%.1f%%", marginPercentage?.toFloat() ?: 0f)
    }

    /**
     * Get color for margin.
     */
    fun getMarginColor(): Int {
        val margin = marginPercentage?.toFloat() ?: 0f
        return when {
            margin >= 30 -> android.graphics.Color.parseColor("#4CAF50")   // High margin - Green
            margin >= 15 -> android.graphics.Color.parseColor("#8BC34A")   // Good margin - Light Green
            margin >= 5 -> android.graphics.Color.parseColor("#FF9800")    // Low margin - Orange
            else -> android.graphics.Color.parseColor("#F44336")           // Very low/negative - Red
        }
    }
}

/**
 * Stock valuation summary.
 * Maps to backend StockValuationSummaryDTO.
 */
data class StockValuationSummary(
    @SerializedName("totalProducts")
    val totalProducts: Int,
    @SerializedName("totalQuantity")
    val totalQuantity: Long,
    @SerializedName("totalPurchaseValue")
    val totalPurchaseValue: Long,
    @SerializedName("totalSalesValue")
    val totalSalesValue: Long,
    @SerializedName("totalPotentialMargin")
    val totalPotentialMargin: Long,
    @SerializedName("avgMarginPercentage")
    val avgMarginPercentage: BigDecimal?,
    @SerializedName("categoryBreakdown")
    val categoryBreakdown: List<CategoryValuation>?
)

/**
 * Category valuation breakdown.
 */
data class CategoryValuation(
    @SerializedName("category")
    val category: String,
    @SerializedName("productCount")
    val productCount: Int,
    @SerializedName("totalQuantity")
    val totalQuantity: Long,
    @SerializedName("totalPurchaseValue")
    val totalPurchaseValue: Long,
    @SerializedName("totalSalesValue")
    val totalSalesValue: Long,
    @SerializedName("percentage")
    val percentage: BigDecimal?
)
