package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.reports.utils.NumberFormatUtils
import kotlinx.parcelize.Parcelize

/**
 * Cash Balance (Balance Caisse) data model.
 * Provides comprehensive breakdown by payment mode and sales category.
 */
@Parcelize
data class CashBalance(
    @SerializedName("fromDate")
    val fromDate: String,

    @SerializedName("toDate")
    val toDate: String,

    @SerializedName("periodLabel")
    val periodLabel: String,

    // Totaux
    @SerializedName("transactionsCount")
    val transactionsCount: Int,

    @SerializedName("montantTtc")
    val montantTtc: Long,

    @SerializedName("montantHt")
    val montantHt: Long,

    @SerializedName("montantNet")
    val montantNet: Long,

    @SerializedName("montantRemise")
    val montantRemise: Long,

    @SerializedName("montantTva")
    val montantTva: Long,

    @SerializedName("panierMoyen")
    val panierMoyen: Long,

    // Par mode de paiement
    @SerializedName("montantEspeces")
    val montantEspeces: Long,

    @SerializedName("montantCartes")
    val montantCartes: Long,

    @SerializedName("montantCheques")
    val montantCheques: Long,

    @SerializedName("montantVirements")
    val montantVirements: Long,

    @SerializedName("montantMobileMoney")
    val montantMobileMoney: Long,

    @SerializedName("montantCredit")
    val montantCredit: Long,

    @SerializedName("montantDiffere")
    val montantDiffere: Long,

    @SerializedName("montantTiersPayant")
    val montantTiersPayant: Long,

    // Metriques
    @SerializedName("montantAchats")
    val montantAchats: Long,

    @SerializedName("montantMarge")
    val montantMarge: Long,

    @SerializedName("ratioVenteAchat")
    val ratioVenteAchat: Double,

    @SerializedName("ratioAchatVente")
    val ratioAchatVente: Double,

    // Repartition par mode (pour graphique)
    @SerializedName("paymentBreakdown")
    val paymentBreakdown: List<PaymentModeBreakdown>,

    // Balance par categorie de vente
    @SerializedName("categoryBalances")
    val categoryBalances: List<CategoryBalance>,

    // Mouvements de caisse
    @SerializedName("cashMovements")
    val cashMovements: List<CashMovement>
) : Parcelable {

    fun getFormattedMontantTtc(): String = NumberFormatUtils.formatCurrency(montantTtc)

    fun getFormattedMontantHt(): String = NumberFormatUtils.formatCurrency(montantHt)

    fun getFormattedMontantNet(): String = NumberFormatUtils.formatCurrency(montantNet)

    fun getFormattedMontantRemise(): String = NumberFormatUtils.formatCurrency(montantRemise)

    fun getFormattedMontantTva(): String = NumberFormatUtils.formatCurrency(montantTva)

    fun getFormattedPanierMoyen(): String = NumberFormatUtils.formatCurrency(panierMoyen)

    fun getFormattedMontantEspeces(): String = NumberFormatUtils.formatCurrency(montantEspeces)

    fun getFormattedMontantCartes(): String = NumberFormatUtils.formatCurrency(montantCartes)

    fun getFormattedMontantCheques(): String = NumberFormatUtils.formatCurrency(montantCheques)

    fun getFormattedMontantVirements(): String = NumberFormatUtils.formatCurrency(montantVirements)

    fun getFormattedMontantMobileMoney(): String = NumberFormatUtils.formatCurrency(montantMobileMoney)

    fun getFormattedMontantCredit(): String = NumberFormatUtils.formatCurrency(montantCredit)

    fun getFormattedMontantDiffere(): String = NumberFormatUtils.formatCurrency(montantDiffere)

    fun getFormattedMontantTiersPayant(): String = NumberFormatUtils.formatCurrency(montantTiersPayant)

    fun getFormattedMontantAchats(): String = NumberFormatUtils.formatCurrency(montantAchats)

    fun getFormattedMontantMarge(): String = NumberFormatUtils.formatCurrency(montantMarge)

    fun getFormattedRatioVenteAchat(): String = NumberFormatUtils.formatPercent(ratioVenteAchat)

    fun getFormattedRatioAchatVente(): String = NumberFormatUtils.formatPercent(ratioAchatVente)

    fun isEmpty(): Boolean = transactionsCount == 0 && montantTtc == 0L
}

/**
 * Payment mode breakdown for pie chart.
 */
@Parcelize
data class PaymentModeBreakdown(
    @SerializedName("code")
    val code: String,

    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("montant")
    val montant: Long,

    @SerializedName("percent")
    val percent: Double,

    @SerializedName("color")
    val color: String
) : Parcelable {

    fun getFormattedMontant(): String = NumberFormatUtils.formatCurrency(montant)

    fun getFormattedPercent(): String = NumberFormatUtils.formatPercent(percent)
}

/**
 * Category balance (VO, VNO, etc.).
 */
@Parcelize
data class CategoryBalance(
    @SerializedName("categoryCode")
    val categoryCode: String,

    @SerializedName("categoryLabel")
    val categoryLabel: String,

    @SerializedName("count")
    val count: Int,

    @SerializedName("montantTtc")
    val montantTtc: Long,

    @SerializedName("montantHt")
    val montantHt: Long,

    @SerializedName("montantNet")
    val montantNet: Long,

    @SerializedName("montantRemise")
    val montantRemise: Long,

    @SerializedName("montantTva")
    val montantTva: Long,

    @SerializedName("montantAchat")
    val montantAchat: Long,

    @SerializedName("montantMarge")
    val montantMarge: Long,

    @SerializedName("panierMoyen")
    val panierMoyen: Long,

    // Payment breakdown for this category
    @SerializedName("montantCash")
    val montantCash: Long,

    @SerializedName("montantCard")
    val montantCard: Long,

    @SerializedName("montantCheque")
    val montantCheque: Long,

    @SerializedName("montantVirement")
    val montantVirement: Long,

    @SerializedName("montantMobileMoney")
    val montantMobileMoney: Long,

    @SerializedName("montantCredit")
    val montantCredit: Long,

    @SerializedName("montantDiffere")
    val montantDiffere: Long,

    @SerializedName("montantTiersPayant")
    val montantTiersPayant: Long
) : Parcelable {

    fun getFormattedMontantTtc(): String = NumberFormatUtils.formatCurrency(montantTtc)

    fun getFormattedMontantHt(): String = NumberFormatUtils.formatCurrency(montantHt)

    fun getFormattedMontantNet(): String = NumberFormatUtils.formatCurrency(montantNet)

    fun getFormattedMontantMarge(): String = NumberFormatUtils.formatCurrency(montantMarge)

    fun getFormattedPanierMoyen(): String = NumberFormatUtils.formatCurrency(panierMoyen)
}

/**
 * Cash movement (entry or exit).
 */
@Parcelize
data class CashMovement(
    @SerializedName("id")
    val id: Long,

    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("montant")
    val montant: Long,

    @SerializedName("type")
    val type: String,  // ENTREE | SORTIE

    @SerializedName("date")
    val date: String?,

    @SerializedName("comment")
    val comment: String?
) : Parcelable {

    fun getFormattedMontant(): String = NumberFormatUtils.formatCurrency(kotlin.math.abs(montant))

    fun isEntree(): Boolean = type == TYPE_ENTREE

    fun isSortie(): Boolean = type == TYPE_SORTIE

    companion object {
        const val TYPE_ENTREE = "ENTREE"
        const val TYPE_SORTIE = "SORTIE"
    }
}
