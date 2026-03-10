package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Marge produit — mappe MargeDTO (sans classification BCG).
 */
data class MargeDTO(
    @SerializedName("produitId")   val produitId: Int,
    @SerializedName("libelle")     val libelle: String,
    @SerializedName("codeCip")     val codeCip: String?,
    @SerializedName("categorie")   val categorie: String?,
    @SerializedName("nbVentes")    val nbVentes: Int,
    @SerializedName("qteVendue")   val qteVendue: Int,
    @SerializedName("caTotal")         val caTotal: Long,
    @SerializedName("coutAchatTotal")  val coutAchatTotal: Long,
    @SerializedName("margeBrute")      val margeBrute: Long,
    @SerializedName("tauxMargePct")    val tauxMargePct: BigDecimal?,
    @SerializedName("prixVenteMoyen")  val prixVenteMoyen: Int?,
    @SerializedName("prixAchatMoyen")  val prixAchatMoyen: Int?,
    @SerializedName("stockQuantity")   val stockQuantity: Int?,
    @SerializedName("prixAchatUnitaire")  val prixAchatUnitaire: Int?,
    @SerializedName("prixVenteUnitaire")  val prixVenteUnitaire: Int?,
    @SerializedName("tauxRotationAnnuel") val tauxRotationAnnuel: BigDecimal?
) {
    fun getMarginColor(): Int {
        val margin = tauxMargePct?.toFloat() ?: 0f
        return when {
            margin >= 30 -> android.graphics.Color.parseColor("#4CAF50")
            margin >= 20 -> android.graphics.Color.parseColor("#8BC34A")
            margin >= 10 -> android.graphics.Color.parseColor("#FF9800")
            else         -> android.graphics.Color.parseColor("#F44336")
        }
    }

    fun getMarginFormatted(): String = String.format("%.1f%%", tauxMargePct?.toFloat() ?: 0f)
}

/**
 * Résumé global des marges — mappe MargeSummaryDTO (sans BCG).
 */
data class MargeSummary(
    @SerializedName("totalProduits")               val totalProduits: Int,
    @SerializedName("caTotalGlobal")               val caTotalGlobal: Long,
    @SerializedName("coutAchatGlobal")             val coutAchatGlobal: Long,
    @SerializedName("margeBruteGlobale")           val margeBruteGlobale: Long,
    @SerializedName("tauxMargeMoyen")              val tauxMargeMoyen: BigDecimal?,
    @SerializedName("nbProduitsMargeInsuffisante") val nbProduitsMargeInsuffisante: Int,
    @SerializedName("caProduitsFaibleMarge")       val caProduitsFaibleMarge: Long,
    @SerializedName("nbProduitsMargeConfortable")  val nbProduitsMargeConfortable: Int,
    @SerializedName("caProduitsBonneMarge")        val caProduitsBonneMarge: Long
)

// Aliases de compatibilité pour éviter de casser d'autres références éventuelles
typealias ProductProfitability = MargeDTO
typealias ProfitabilitySummary = MargeSummary
