package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.reports.utils.NumberFormatUtils
import kotlinx.parcelize.Parcelize

/**
 * Activity Report (Rapport d'Activité) data model.
 */
@Parcelize
data class ActivityReport(
    @SerializedName("fromDate")
    val fromDate: String,

    @SerializedName("toDate")
    val toDate: String,

    @SerializedName("periodLabel")
    val periodLabel: String,

    @SerializedName("chiffreAffaire")
    val chiffreAffaire: ChiffreAffaire,

    @SerializedName("recettes")
    val recettes: List<Recette>,

    @SerializedName("totalRecettes")
    val totalRecettes: Long,

    @SerializedName("mouvementsCaisse")
    val mouvementsCaisse: List<MouvementCaisse>,

    @SerializedName("totalEntrees")
    val totalEntrees: Long,

    @SerializedName("totalSorties")
    val totalSorties: Long,

    @SerializedName("achatsFournisseurs")
    val achatsFournisseurs: List<GroupeFournisseurAchat>,

    @SerializedName("totalAchats")
    val totalAchats: Long,

    @SerializedName("tiersPayants")
    val tiersPayants: TiersPayantSummary
) : Parcelable {

    fun getFormattedTotalRecettes(): String = NumberFormatUtils.formatCurrency(totalRecettes)

    fun getFormattedTotalEntrees(): String = NumberFormatUtils.formatCurrency(totalEntrees)

    fun getFormattedTotalSorties(): String = NumberFormatUtils.formatCurrency(totalSorties)

    fun getFormattedTotalAchats(): String = NumberFormatUtils.formatCurrency(totalAchats)

    fun isEmpty(): Boolean = chiffreAffaire.montantTtc == 0L && recettes.isEmpty()
}

/**
 * Revenue summary (Chiffre d'Affaires).
 */
@Parcelize
data class ChiffreAffaire(
    @SerializedName("montantTtc")
    val montantTtc: Long,

    @SerializedName("montantTva")
    val montantTva: Long,

    @SerializedName("montantHt")
    val montantHt: Long,

    @SerializedName("montantRemise")
    val montantRemise: Long,

    @SerializedName("montantNet")
    val montantNet: Long,

    @SerializedName("montantEspece")
    val montantEspece: Long,

    @SerializedName("montantAutreMode")
    val montantAutreMode: Long,

    @SerializedName("montantCredit")
    val montantCredit: Long,

    @SerializedName("montantRegle")
    val montantRegle: Long,

    @SerializedName("marge")
    val marge: Long,

    @SerializedName("margePercent")
    val margePercent: Double
) : Parcelable {

    fun getFormattedMontantTtc(): String = NumberFormatUtils.formatCurrency(montantTtc)

    fun getFormattedMontantHt(): String = NumberFormatUtils.formatCurrency(montantHt)

    fun getFormattedMontantTva(): String = NumberFormatUtils.formatCurrency(montantTva)

    fun getFormattedMontantRemise(): String = NumberFormatUtils.formatCurrency(montantRemise)

    fun getFormattedMontantNet(): String = NumberFormatUtils.formatCurrency(montantNet)

    fun getFormattedMontantEspece(): String = NumberFormatUtils.formatCurrency(montantEspece)

    fun getFormattedMontantCredit(): String = NumberFormatUtils.formatCurrency(montantCredit)

    fun getFormattedMarge(): String = NumberFormatUtils.formatCurrency(marge)

    fun getFormattedMargePercent(): String = String.format("%.1f%%", margePercent)

    fun isMargeHealthy(): Boolean = margePercent >= 20.0
}

/**
 * Receipt by payment mode.
 */
@Parcelize
data class Recette(
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

    fun getFormattedPercent(): String = String.format("%.1f%%", percent)
}

/**
 * Cash movement.
 */
@Parcelize
data class MouvementCaisse(
    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("montant")
    val montant: Long,

    @SerializedName("type")
    val type: String
) : Parcelable {

    fun getFormattedMontant(): String = NumberFormatUtils.formatCurrency(montant)

    fun isEntree(): Boolean = type == TYPE_ENTREE

    fun isSortie(): Boolean = type == TYPE_SORTIE

    companion object {
        const val TYPE_ENTREE = "ENTREE"
        const val TYPE_SORTIE = "SORTIE"
    }
}

/**
 * Supplier group purchase.
 */
@Parcelize
data class GroupeFournisseurAchat(
    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("montantTtc")
    val montantTtc: Long,

    @SerializedName("montantTva")
    val montantTva: Long,

    @SerializedName("montantHt")
    val montantHt: Long,

    @SerializedName("percentTotal")
    val percentTotal: Double
) : Parcelable {

    fun getFormattedMontantTtc(): String = NumberFormatUtils.formatCurrency(montantTtc)

    fun getFormattedPercent(): String = String.format("%.1f%%", percentTotal)
}

/**
 * Third-party payer summary.
 */
@Parcelize
data class TiersPayantSummary(
    @SerializedName("reglements")
    val reglements: List<ReglementTiersPayant>,

    @SerializedName("totalFacture")
    val totalFacture: Long,

    @SerializedName("totalRegle")
    val totalRegle: Long,

    @SerializedName("totalRestant")
    val totalRestant: Long,

    @SerializedName("achats")
    val achats: List<AchatTiersPayant>,

    @SerializedName("totalBons")
    val totalBons: Int,

    @SerializedName("totalMontantAchats")
    val totalMontantAchats: Long,

    @SerializedName("totalClients")
    val totalClients: Int
) : Parcelable {

    fun getFormattedTotalFacture(): String = NumberFormatUtils.formatCurrency(totalFacture)

    fun getFormattedTotalRegle(): String = NumberFormatUtils.formatCurrency(totalRegle)

    fun getFormattedTotalRestant(): String = NumberFormatUtils.formatCurrency(totalRestant)

    fun getFormattedTotalMontantAchats(): String = NumberFormatUtils.formatCurrency(totalMontantAchats)

    fun hasReglements(): Boolean = reglements.isNotEmpty()

    fun hasAchats(): Boolean = achats.isNotEmpty()
}

/**
 * Third-party payment.
 */
@Parcelize
data class ReglementTiersPayant(
    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("categorie")
    val categorie: String,

    @SerializedName("numFacture")
    val numFacture: String?,

    @SerializedName("montantFacture")
    val montantFacture: Long,

    @SerializedName("montantReglement")
    val montantReglement: Long,

    @SerializedName("montantRestant")
    val montantRestant: Long
) : Parcelable {

    fun getFormattedMontantFacture(): String = NumberFormatUtils.formatCurrency(montantFacture)

    fun getFormattedMontantReglement(): String = NumberFormatUtils.formatCurrency(montantReglement)

    fun getFormattedMontantRestant(): String = NumberFormatUtils.formatCurrency(montantRestant)
}

/**
 * Third-party payer purchase.
 */
@Parcelize
data class AchatTiersPayant(
    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("categorie")
    val categorie: String,

    @SerializedName("bonsCount")
    val bonsCount: Int,

    @SerializedName("montant")
    val montant: Long,

    @SerializedName("clientCount")
    val clientCount: Int
) : Parcelable {

    fun getFormattedMontant(): String = NumberFormatUtils.formatCurrency(montant)
}
