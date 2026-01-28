package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Dashboard Data model
 * Represents sales dashboard statistics and KPIs
 */
@Parcelize
data class DashboardData(
    @SerializedName("totalSales")
    val totalSales: Int = 0, // Total revenue (chiffre d'affaires)

    @SerializedName("salesCount")
    val salesCount: Int = 0, // Number of sales transactions

    @SerializedName("averageBasket")
    val averageBasket: Int = 0, // Average basket value (panier moyen)

    @SerializedName("objective")
    val objective: Int = 0, // Sales objective/target for the period

    @SerializedName("objectiveProgress")
    val objectiveProgress: Double = 0.0, // Progress towards objective (0.0 - 1.0)

    @SerializedName("topProducts")
    val topProducts: List<TopProduct> = emptyList(), // Top selling products

    @SerializedName("period")
    val period: Period = Period.TODAY, // Selected period

    @SerializedName("updatedAt")
    val updatedAt: String? = null // Last update timestamp
) : Parcelable {

    /**
     * Get formatted total sales
     */
    fun getFormattedTotalSales(): String {
        return "${formatAmount(totalSales)} FCFA"
    }

    /**
     * Get formatted average basket
     */
    fun getFormattedAverageBasket(): String {
        return "${formatAmount(averageBasket)} FCFA"
    }

    /**
     * Get objective progress percentage
     */
    fun getObjectiveProgressPercentage(): Int {
        return (objectiveProgress * 100).toInt()
    }

    /**
     * Get formatted objective progress
     */
    fun getFormattedObjectiveProgress(): String {
        return "${getObjectiveProgressPercentage()}%"
    }

    /**
     * Check if objective is reached
     */
    fun isObjectiveReached(): Boolean {
        return totalSales >= objective
    }

    /**
     * Get remaining amount to reach objective
     */
    fun getRemainingToObjective(): Int {
        return maxOf(0, objective - totalSales)
    }

    /**
     * Format amount with space as thousand separator
     */
    private fun formatAmount(amount: Int): String {
        return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
    }

    /**
     * Calculate statistics
     */
    fun calculateStats(): DashboardStats {
        return DashboardStats(
            totalSales = totalSales,
            salesCount = salesCount,
            averageBasket = if (salesCount > 0) totalSales / salesCount else 0,
            objectiveProgress = if (objective > 0) (totalSales.toDouble() / objective.toDouble()) else 0.0
        )
    }
}

/**
 * Top Product model
 * Represents a top-selling product with statistics
 */
@Parcelize
data class TopProduct(
    @SerializedName("productId")
    val productId: Long = 0,

    @SerializedName("productName")
    val productName: String = "",

    @SerializedName("productCode")
    val productCode: String? = null,

    @SerializedName("quantitySold")
    val quantitySold: Int = 0,

    @SerializedName("totalSales")
    val totalSales: Int = 0, // Total revenue for this product

    @SerializedName("rank")
    val rank: Int = 0 // Rank in top products (1, 2, 3, etc.)
) : Parcelable {

    /**
     * Get formatted total sales
     */
    fun getFormattedTotalSales(): String {
        val formatted = totalSales.toString().reversed().chunked(3).joinToString(" ").reversed()
        return "$formatted FCFA"
    }

    /**
     * Get formatted quantity sold
     */
    fun getFormattedQuantitySold(): String {
        return "$quantitySold vendu${if (quantitySold > 1) "s" else ""}"
    }
}

/**
 * Dashboard statistics
 */
data class DashboardStats(
    val totalSales: Int,
    val salesCount: Int,
    val averageBasket: Int,
    val objectiveProgress: Double
)

/**
 * Period enum for dashboard filter
 */
enum class Period {
    @SerializedName("TODAY")
    TODAY,

    @SerializedName("WEEK")
    WEEK,

    @SerializedName("MONTH")
    MONTH
}
