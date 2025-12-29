package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Stock rotation model.
 * Maps to backend StockRotationDTO.
 */
data class StockRotation(
    @SerializedName("produitId")
    val produitId: Int,
    @SerializedName("libelle")
    val libelle: String,
    @SerializedName("codeCip")
    val codeCip: String?,
    @SerializedName("categorie")
    val categorie: String?,
    @SerializedName("stockQuantity")
    val stockQuantity: Int,
    @SerializedName("unitCost")
    val unitCost: Int,
    @SerializedName("stockValue")
    val stockValue: Long,
    @SerializedName("caLast30Days")
    val caLast30Days: Int,
    @SerializedName("qtySoldLast30Days")
    val qtySoldLast30Days: Int,
    @SerializedName("nbSalesLast30Days")
    val nbSalesLast30Days: Int,
    @SerializedName("caLast12Months")
    val caLast12Months: Int,
    @SerializedName("qtySoldLast12Months")
    val qtySoldLast12Months: Int,
    @SerializedName("rotationRateAnnual")
    val rotationRateAnnual: BigDecimal?,
    @SerializedName("avgDaysInStock")
    val avgDaysInStock: Int?,
    @SerializedName("categorieABC")
    val categorieABC: CategorieABC
) {
    /**
     * ABC category for stock rotation classification.
     */
    enum class CategorieABC {
        A,  // Fast moving (top 20% revenue contributors)
        B,  // Medium moving (next 30%)
        C   // Slow moving (bottom 50%)
    }

    /**
     * Get ABC category display name.
     */
    fun getAbcCategoryLabel(): String {
        return when (categorieABC) {
            CategorieABC.A -> "Classe A - Rotation rapide"
            CategorieABC.B -> "Classe B - Rotation moyenne"
            CategorieABC.C -> "Classe C - Rotation lente"
        }
    }

    /**
     * Get ABC category short label.
     */
    fun getAbcShortLabel(): String {
        return categorieABC.name
    }

    /**
     * Get ABC category color.
     */
    fun getAbcColor(): Int {
        return when (categorieABC) {
            CategorieABC.A -> android.graphics.Color.parseColor("#4CAF50")  // Green
            CategorieABC.B -> android.graphics.Color.parseColor("#FF9800")  // Orange
            CategorieABC.C -> android.graphics.Color.parseColor("#F44336")  // Red
        }
    }

    /**
     * Get rotation rate formatted.
     */
    fun getRotationRateFormatted(): String {
        return String.format("%.2fx", rotationRateAnnual?.toFloat() ?: 0f)
    }

    /**
     * Check if product is slow moving.
     */
    fun isSlowMoving(): Boolean {
        return categorieABC == CategorieABC.C
    }

    /**
     * Get days in stock indicator color.
     */
    fun getDaysInStockColor(): Int {
        val days = avgDaysInStock ?: 0
        return when {
            days <= 30 -> android.graphics.Color.parseColor("#4CAF50")   // Fast
            days <= 90 -> android.graphics.Color.parseColor("#FF9800")   // Medium
            else -> android.graphics.Color.parseColor("#F44336")         // Slow
        }
    }
}

/**
 * Stock rotation ABC counts.
 */
data class StockRotationCounts(
    @SerializedName("A")
    val countA: Long,
    @SerializedName("B")
    val countB: Long,
    @SerializedName("C")
    val countC: Long
) {
    val total: Long get() = countA + countB + countC

    fun getPercentageA(): Float = if (total > 0) (countA.toFloat() / total) * 100f else 0f
    fun getPercentageB(): Float = if (total > 0) (countB.toFloat() / total) * 100f else 0f
    fun getPercentageC(): Float = if (total > 0) (countC.toFloat() / total) * 100f else 0f
}
