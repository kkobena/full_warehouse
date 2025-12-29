package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Product profitability model.
 * Maps to backend ProductProfitabilityDTO.
 */
data class ProductProfitability(
    @SerializedName("produitId")
    val produitId: Int,
    @SerializedName("libelle")
    val libelle: String,
    @SerializedName("codeCip")
    val codeCip: String?,
    @SerializedName("categorie")
    val categorie: String?,
    @SerializedName("nbVentes")
    val nbVentes: Int,
    @SerializedName("qteVendue")
    val qteVendue: Int,
    @SerializedName("caTotal")
    val caTotal: Int,
    @SerializedName("coutAchatTotal")
    val coutAchatTotal: Int,
    @SerializedName("margeBrute")
    val margeBrute: Int,
    @SerializedName("tauxMargePct")
    val tauxMargePct: BigDecimal?,
    @SerializedName("prixVenteMoyen")
    val prixVenteMoyen: Int?,
    @SerializedName("prixAchatMoyen")
    val prixAchatMoyen: Int?,
    @SerializedName("stockQuantity")
    val stockQuantity: Int?,
    @SerializedName("prixAchatUnitaire")
    val prixAchatUnitaire: Int?,
    @SerializedName("prixVenteUnitaire")
    val prixVenteUnitaire: Int?,
    @SerializedName("tauxRotationAnnuel")
    val tauxRotationAnnuel: BigDecimal?,
    @SerializedName("bcgCategory")
    val bcgCategory: BCGCategory
) {
    /**
     * BCG Matrix categories.
     */
    enum class BCGCategory {
        STAR,           // High growth, high market share
        CASH_COW,       // Low growth, high market share
        QUESTION_MARK,  // High growth, low market share
        DOG             // Low growth, low market share
    }

    /**
     * Get BCG category display name.
     */
    fun getBcgCategoryLabel(): String {
        return when (bcgCategory) {
            BCGCategory.STAR -> "Star"
            BCGCategory.CASH_COW -> "Vache à lait"
            BCGCategory.QUESTION_MARK -> "Dilemme"
            BCGCategory.DOG -> "Poids mort"
        }
    }

    /**
     * Get BCG category color.
     */
    fun getBcgColor(): Int {
        return when (bcgCategory) {
            BCGCategory.STAR -> android.graphics.Color.parseColor("#FFD700")        // Gold
            BCGCategory.CASH_COW -> android.graphics.Color.parseColor("#4CAF50")    // Green
            BCGCategory.QUESTION_MARK -> android.graphics.Color.parseColor("#FF9800") // Orange
            BCGCategory.DOG -> android.graphics.Color.parseColor("#9E9E9E")          // Grey
        }
    }

    /**
     * Get margin color based on percentage.
     */
    fun getMarginColor(): Int {
        val margin = tauxMargePct?.toFloat() ?: 0f
        return when {
            margin >= 30 -> android.graphics.Color.parseColor("#4CAF50")
            margin >= 15 -> android.graphics.Color.parseColor("#8BC34A")
            margin >= 5 -> android.graphics.Color.parseColor("#FF9800")
            else -> android.graphics.Color.parseColor("#F44336")
        }
    }

    /**
     * Get margin percentage formatted.
     */
    fun getMarginFormatted(): String {
        return String.format("%.1f%%", tauxMargePct?.toFloat() ?: 0f)
    }
}

/**
 * Profitability summary.
 * Maps to backend ProfitabilitySummaryDTO.
 */
data class ProfitabilitySummary(
    @SerializedName("totalProducts")
    val totalProducts: Int,
    @SerializedName("totalRevenue")
    val totalRevenue: Long,
    @SerializedName("totalCost")
    val totalCost: Long,
    @SerializedName("totalMargin")
    val totalMargin: Long,
    @SerializedName("avgMarginPercentage")
    val avgMarginPercentage: BigDecimal?,
    @SerializedName("starCount")
    val starCount: Int,
    @SerializedName("cashCowCount")
    val cashCowCount: Int,
    @SerializedName("questionMarkCount")
    val questionMarkCount: Int,
    @SerializedName("dogCount")
    val dogCount: Int,
    @SerializedName("lowMarginCount")
    val lowMarginCount: Int
)
