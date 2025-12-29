package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName

/**
 * Tiers Payant invoice (creance) model.
 * Maps to backend TiersPayantInvoiceDTO.
 */
data class TiersPayantInvoice(
    @SerializedName("factureId")
    val factureId: Long,
    @SerializedName("numeroFacture")
    val numeroFacture: String,
    @SerializedName("dateFacture")
    val dateFacture: String,
    @SerializedName("tiersPayantLibelle")
    val tiersPayantLibelle: String,
    @SerializedName("groupeTiersPayantLibelle")
    val groupeTiersPayantLibelle: String,
    @SerializedName("montantFacture")
    val montantFacture: Int,
    @SerializedName("montantPaye")
    val montantPaye: Int,
    @SerializedName("montantRestant")
    val montantRestant: Int,
    @SerializedName("statut")
    val statut: InvoiceStatus,
    @SerializedName("daysSinceInvoice")
    val daysSinceInvoice: Int,
    @SerializedName("ageCategory")
    val ageCategory: AgeCategory
) {
    enum class InvoiceStatus {
        PAID, UNPAID, PARTIAL
    }

    enum class AgeCategory {
        LESS_THAN_30,      // < 30 days
        BETWEEN_30_60,     // 30-60 days
        BETWEEN_60_90,     // 60-90 days
        MORE_THAN_90       // > 90 days
    }

    /**
     * Get color resource for age category.
     */
    fun getAgeCategoryColor(): Int {
        return when (ageCategory) {
            AgeCategory.LESS_THAN_30 -> android.graphics.Color.parseColor("#4CAF50")   // Green
            AgeCategory.BETWEEN_30_60 -> android.graphics.Color.parseColor("#FF9800")  // Orange
            AgeCategory.BETWEEN_60_90 -> android.graphics.Color.parseColor("#FF5722")  // Deep Orange
            AgeCategory.MORE_THAN_90 -> android.graphics.Color.parseColor("#F44336")   // Red
        }
    }

    /**
     * Get formatted age category label.
     */
    fun getAgeCategoryLabel(): String {
        return when (ageCategory) {
            AgeCategory.LESS_THAN_30 -> "< 30 jours"
            AgeCategory.BETWEEN_30_60 -> "30-60 jours"
            AgeCategory.BETWEEN_60_90 -> "60-90 jours"
            AgeCategory.MORE_THAN_90 -> "> 90 jours"
        }
    }
}

/**
 * Creances summary by tiers payant group.
 * Maps to backend TiersPayantCreancesSummaryDTO.
 */
data class TiersPayantCreancesSummary(
    @SerializedName("groupeTiersPayantId")
    val groupeTiersPayantId: Int,
    @SerializedName("groupeTiersPayantLibelle")
    val groupeTiersPayantLibelle: String,
    @SerializedName("totalFactures")
    val totalFactures: Int,
    @SerializedName("montantTotal")
    val montantTotal: Long,
    @SerializedName("montantPaye")
    val montantPaye: Long,
    @SerializedName("montantRestant")
    val montantRestant: Long,
    @SerializedName("countLessThan30")
    val countLessThan30: Int,
    @SerializedName("countBetween30And60")
    val countBetween30And60: Int,
    @SerializedName("countBetween60And90")
    val countBetween60And90: Int,
    @SerializedName("countMoreThan90")
    val countMoreThan90: Int
) {
    /**
     * Get payment progress percentage.
     */
    fun getPaymentProgress(): Float {
        return if (montantTotal > 0) {
            (montantPaye.toFloat() / montantTotal.toFloat()) * 100f
        } else 0f
    }
}
