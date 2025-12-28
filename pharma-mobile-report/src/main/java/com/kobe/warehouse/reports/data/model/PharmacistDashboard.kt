package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Pharmacist Dashboard data model - matches MobilePharmacistDashboardDTO from backend.
 * Provides comprehensive sales vs purchases analysis.
 */
@Parcelize
data class PharmacistDashboard(
    // Period
    @SerializedName("fromDate") val fromDate: String,
    @SerializedName("toDate") val toDate: String,
    @SerializedName("periodLabel") val periodLabel: String,

    // Sales (Ventes)
    @SerializedName("montantVenteComptant") val montantVenteComptant: Long,
    @SerializedName("montantVenteCredit") val montantVenteCredit: Long,
    @SerializedName("montantVenteRemise") val montantVenteRemise: Long,
    @SerializedName("montantVenteNet") val montantVenteNet: Long,
    @SerializedName("montantVenteTtc") val montantVenteTtc: Long,
    @SerializedName("montantVenteHt") val montantVenteHt: Long,
    @SerializedName("montantVenteTaxe") val montantVenteTaxe: Long,
    @SerializedName("transactionsCount") val transactionsCount: Int,

    // Purchases (Achats)
    @SerializedName("montantAchatNet") val montantAchatNet: Long,
    @SerializedName("montantAchatTtc") val montantAchatTtc: Long,
    @SerializedName("montantAchatHt") val montantAchatHt: Long,
    @SerializedName("montantAchatTaxe") val montantAchatTaxe: Long,
    @SerializedName("montantAchatRemise") val montantAchatRemise: Long,
    @SerializedName("montantAvoirFournisseur") val montantAvoirFournisseur: Long,

    // Ratios
    @SerializedName("ratioVenteAchat") val ratioVenteAchat: Double,
    @SerializedName("ratioAchatVente") val ratioAchatVente: Double,

    // Margin
    @SerializedName("marge") val marge: Long,
    @SerializedName("margePercent") val margePercent: Double,

    // Variations vs previous period
    @SerializedName("ventesVariation") val ventesVariation: Double?,
    @SerializedName("achatsVariation") val achatsVariation: Double?,

    // Top suppliers
    @SerializedName("topFournisseurs") val topFournisseurs: List<FournisseurAchat>,

    // Chart data
    @SerializedName("chartVentesAchats") val chartVentesAchats: List<ChartDataPoint>
) : Parcelable {

    /**
     * Format an amount for display with thousand separators.
     */
    fun formatAmount(amount: Long): String {
        return String.format("%,d", amount).replace(",", " ") + " F"
    }

    /**
     * Get formatted ventes net.
     */
    fun getFormattedVentesNet(): String = formatAmount(montantVenteNet)

    /**
     * Get formatted achats net.
     */
    fun getFormattedAchatsNet(): String = formatAmount(montantAchatNet)

    /**
     * Get formatted marge.
     */
    fun getFormattedMarge(): String = formatAmount(marge)

    /**
     * Get formatted margin percentage.
     */
    fun getFormattedMargePercent(): String = String.format("%.1f%%", margePercent)

    /**
     * Get formatted ratio vente/achat.
     */
    fun getFormattedRatioVenteAchat(): String = String.format("%.2f", ratioVenteAchat)

    /**
     * Get formatted ratio achat/vente.
     */
    fun getFormattedRatioAchatVente(): String = String.format("%.2f", ratioAchatVente)

    /**
     * Get ventes variation indicator.
     */
    fun getVentesVariationIndicator(): String {
        return when {
            ventesVariation == null -> ""
            ventesVariation >= 0 -> "+"
            else -> ""
        }
    }

    /**
     * Get formatted ventes variation.
     */
    fun getFormattedVentesVariation(): String {
        return ventesVariation?.let { String.format("%+.1f%%", it) } ?: "-"
    }

    /**
     * Check if ventes variation is positive.
     */
    fun isVentesVariationPositive(): Boolean = (ventesVariation ?: 0.0) >= 0

    /**
     * Get formatted achats variation.
     */
    fun getFormattedAchatsVariation(): String {
        return achatsVariation?.let { String.format("%+.1f%%", it) } ?: "-"
    }

    /**
     * Check if margin is healthy (positive).
     */
    fun isMarginHealthy(): Boolean = marge > 0
}

/**
 * Supplier purchase data.
 */
@Parcelize
data class FournisseurAchat(
    @SerializedName("id") val id: Int,
    @SerializedName("libelle") val libelle: String,
    @SerializedName("montantNet") val montantNet: Long,
    @SerializedName("montantTtc") val montantTtc: Long,
    @SerializedName("montantHt") val montantHt: Long,
    @SerializedName("montantTaxe") val montantTaxe: Long,
    @SerializedName("montantRemise") val montantRemise: Long,
    @SerializedName("percentTotal") val percentTotal: Double
) : Parcelable {

    /**
     * Get formatted montant net.
     */
    fun getFormattedMontantNet(): String {
        return String.format("%,d", montantNet).replace(",", " ") + " F"
    }

    /**
     * Get formatted percentage.
     */
    fun getFormattedPercent(): String = String.format("%.1f%%", percentTotal)
}

/**
 * Chart data point for various charts.
 */
@Parcelize
data class ChartDataPoint(
    @SerializedName("label") val label: String,
    @SerializedName("value") val value: Double,
    @SerializedName("color") val color: String?,
    @SerializedName("type") val type: String?
) : Parcelable {

    companion object {
        const val TYPE_SALES = "SALES"
        const val TYPE_PURCHASES = "PURCHASES"
        const val TYPE_PAYMENT = "PAYMENT"
        const val TYPE_VAT = "VAT"
    }

    /**
     * Check if this is a sales data point.
     */
    fun isSales(): Boolean = type == TYPE_SALES

    /**
     * Check if this is a purchases data point.
     */
    fun isPurchases(): Boolean = type == TYPE_PURCHASES
}
