package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing forecast predictions with historical context
 */
@Parcelize
data class ForecastData(
    val historical: List<DailySales>,
    val predicted: List<DailySales>,
    val confidence: Float,
    val recommendations: List<ForecastRecommendation>,
    val generatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Get total predicted sales amount for forecast period
     */
    fun getTotalPredictedSales(): Double {
        return predicted.sumOf { it.amount }
    }

    /**
     * Get average predicted daily sales
     */
    fun getAveragePredictedDaily(): Double {
        return if (predicted.isNotEmpty()) {
            getTotalPredictedSales() / predicted.size
        } else 0.0
    }

    /**
     * Calculate predicted growth rate vs historical average
     */
    fun getGrowthRate(): Double {
        val historicalAvg = historical.takeLast(7).map { it.amount }.average()
        val predictedAvg = getAveragePredictedDaily()
        return if (historicalAvg > 0) {
            ((predictedAvg - historicalAvg) / historicalAvg) * 100
        } else 0.0
    }
}

/**
 * Daily sales data point
 */
@Parcelize
data class DailySales(
    val date: String,          // Format: YYYY-MM-DD
    val amount: Double,
    val transactionCount: Int = 0,
    val label: String? = null  // Optional label (e.g., "J+1", "J+2")
) : Parcelable

/**
 * ML-based recommendation for business actions
 */
@Parcelize
data class ForecastRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val impact: RecommendationImpact,
    val productId: Long? = null,
    val productName: String? = null,
    val expectedChange: Double? = null  // Expected % change if recommendation is followed
) : Parcelable

/**
 * Types of recommendations
 */
enum class RecommendationType {
    INCREASE_STOCK,     // Augmenter le stock
    DECREASE_STOCK,     // Réduire le stock
    PRICE_ADJUST,       // Ajuster le prix
    PROMOTION,          // Créer une promotion
    REORDER_SOON,       // Commander bientôt
    HIGH_DEMAND         // Forte demande prévue
}

/**
 * Impact level of recommendation
 */
enum class RecommendationImpact {
    LOW,      // Faible impact
    MEDIUM,   // Impact moyen
    HIGH,     // Impact élevé
    CRITICAL  // Impact critique
}

/**
 * Request for forecast generation
 */
data class ForecastRequest(
    val historicalDays: Int = 30,
    val forecastDays: Int = 7,
    val includeRecommendations: Boolean = true
)

/**
 * Forecast result with metadata
 */
data class ForecastResult(
    val forecast: ForecastData,
    val modelVersion: String,
    val accuracy: Float,
    val trainingDate: String?
)
