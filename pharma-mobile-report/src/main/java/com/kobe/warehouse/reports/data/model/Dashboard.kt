package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Dashboard data model - matches MobileDashboardDTO from backend.
 */
@Parcelize
data class Dashboard(
    @SerializedName("dailyCA") val dailyCA: Long,
    /** CA moyen journalier des 30 derniers jours — référence glissante */
    @SerializedName("averageCA30j") val averageCA30j: Long,
    @SerializedName("variationPercent") val variationPercent: Double? = null,
    /** Écart % entre le CA du jour et la moyenne des 30 derniers jours */
    @SerializedName("trendVs30j") val trendVs30j: Double? = null,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("averageBasket") val averageBasket: Long,
    @SerializedName("customersCount") val customersCount: Int,
    @SerializedName("amountCollected") val amountCollected: Long,
    @SerializedName("amountCredit") val amountCredit: Long,
    @SerializedName("marginAmount") val marginAmount: Long,
    @SerializedName("marginPercent") val marginPercent: Double,
    @SerializedName("alerts") val alerts: List<AlertSummary>,
    @SerializedName("alertsCount") val alertsCount: Int,
    @SerializedName("topProducts") val topProducts: List<TopProduct>,
    @SerializedName("caTrend") val caTrend: List<DailyCASummary>,
    @SerializedName("date") val date: String,
    @SerializedName("lastUpdate") val lastUpdate: String
) : Parcelable {

    fun getFormattedDailyCA(): String = formatAmount(dailyCA)

    fun getFormattedAverageBasket(): String = formatAmount(averageBasket)

    fun getFormattedAverageCA30j(): String = formatAmount(averageCA30j)

    fun getVariationIndicator(): String = indicateur(variationPercent)

    fun isVariationPositive(): Boolean = variationPercent != null && variationPercent >= 0

    /** Vrai si le CA du jour dépasse la moyenne des 30 derniers jours. */
    fun isTrendPositive(): Boolean = trendVs30j != null && trendVs30j >= 0

    /**
     * Vrai quand la veille est vide : il n'y a pas de variation à afficher.
     *
     * Un premier jour d'exploitation, ou un lendemain de jour de fermeture, n'a pas de référence.
     * Le serveur ne rend alors aucune variation, là où il envoyait cent.
     */
    fun hasNoVariationBasis(): Boolean = variationPercent == null

    /** Vrai quand la moyenne glissante est encore vide : la tendance ne veut rien dire. */
    fun hasNoTrendBasis(): Boolean = trendVs30j == null

    /**
     * Texte d'interprétation de la tendance pour l'affichage mobile.
     * Ex : "+12,3% vs moy. 30j", "-5,1% vs moy. 30j", ou "– vs moy. 30j" sans historique.
     */
    fun getTrendLabel(): String = String.format("%s vs moy. 30j", formatVariation(trendVs30j))

    /** Le pourcentage tel qu'il doit se lire : un tiret quand il n'y a rien à comparer. */
    fun formatVariation(variation: Double?): String =
        if (variation == null) "–" else String.format("%+.1f%%", variation)

    private fun indicateur(variation: Double?): String = when {
        variation == null -> "–"
        variation >= 0 -> "↗"
        else -> "↘"
    }

    companion object {
        fun formatAmount(amount: Long): String {
            return String.format("%,d", amount).replace(",", " ") + " F"
        }
    }
}

/**
 * Alert summary for dashboard cards.
 */
@Parcelize
data class AlertSummary(
    @SerializedName("type") val type: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("message") val message: String,
    @SerializedName("count") val count: Int,
    @SerializedName("icon") val icon: String,
    @SerializedName("color") val color: String
) : Parcelable {

    /**
     * Get severity indicator emoji.
     */
    fun getSeverityEmoji(): String {
        return when (severity) {
            "CRITICAL" -> "🔴"
            "WARNING" -> "🟠"
            "INFO" -> "🟡"
            else -> "⚪"
        }
    }
}

/**
 * Top product summary for dashboard.
 */
@Parcelize
data class TopProduct(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("codeCip") val codeCip: String?,
    @SerializedName("salesAmount") val salesAmount: Long,
    @SerializedName("quantitySold") val quantitySold: Int,
    @SerializedName("rank") val rank: Int
) : Parcelable {

    fun getFormattedSalesAmount(): String {
        return Dashboard.formatAmount(salesAmount)
    }
}

/**
 * Daily CA summary for trend chart.
 */
@Parcelize
data class DailyCASummary(
    @SerializedName("date") val date: String,
    @SerializedName("dayLabel") val dayLabel: String,
    @SerializedName("caTotal") val caTotal: Long,
    @SerializedName("transactionsCount") val transactionsCount: Int
) : Parcelable
