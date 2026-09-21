package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Performance report model - matches MobilePerformanceDTO from backend.
 */
@Parcelize
data class Performance(
    @SerializedName("period") val period: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("caTotal") val caTotal: Long,
    @SerializedName("caPreviousPeriod") val caPreviousPeriod: Long,
    @SerializedName("variationPercent") val variationPercent: Double? = null,
    @SerializedName("previousStartDate") val previousStartDate: String? = null,
    @SerializedName("previousEndDate") val previousEndDate: String? = null,
    @SerializedName("caSamePeriodLastYear") val caSamePeriodLastYear: Long = 0,
    @SerializedName("variationVsLastYearPercent") val variationVsLastYearPercent: Double? = null,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("averageBasket") val averageBasket: Long,
    @SerializedName("customersCount") val customersCount: Int,
    @SerializedName("marginTotal") val marginTotal: Long,
    @SerializedName("marginPercent") val marginPercent: Double,
    @SerializedName("paymentMethods") val paymentMethods: List<PaymentMethodSummary>,
    @SerializedName("topProducts") val topProducts: List<TopProductPerformance>,
    @SerializedName("dataPoints") val dataPoints: List<PeriodDataPoint>,
    @SerializedName("generatedAt") val generatedAt: String
) : Parcelable {

    /**
     * Format CA total for display.
     */
    fun getFormattedCATotal(): String {
        return Dashboard.formatAmount(caTotal)
    }

    /**
     * Indicateur de tête : l'écart à la même période de l'an passé.
     *
     * L'activité d'une officine est trop saisonnière pour qu'un mois se compare utilement au mois
     * précédent — épidémies, saison des pluies, rentrée, congés. C'est la comparaison annuelle qui
     * dit si le comptoir progresse.
     */
    fun getVariationIndicator(): String {
        return indicateur(variationVsLastYearPercent)
    }

    /**
     * Vrai si l'officine progresse par rapport à la même période de l'an passé.
     *
     * Faux aussi quand la comparaison n'existe pas : sans point de référence, il n'y a rien à
     * célébrer — la couleur de la progression serait un compliment inventé.
     */
    fun isVariationPositive(): Boolean {
        return variationVsLastYearPercent != null && variationVsLastYearPercent >= 0
    }

    /** Vrai quand la période de référence est vide : il n'y a pas de variation à afficher. */
    fun hasNoComparisonBasis(): Boolean {
        return variationVsLastYearPercent == null
    }

    /**
     * Indicateur secondaire : l'écart à la période précédente, qui mesure l'effet d'une action
     * récente plutôt qu'une tendance de fond.
     */
    fun getPreviousPeriodIndicator(): String {
        return indicateur(variationPercent)
    }

    /**
     * Le pourcentage tel qu'il doit se lire : un tiret quand il n'y a rien à comparer.
     *
     * Le serveur rend maintenant une variation nulle dans ce cas, là où il envoyait cent. Une
     * officine ouverte depuis six mois lisait « +100 % vs l'an passé » toute sa première année.
     */
    fun formatVariation(variation: Double?): String {
        return if (variation == null) "–" else String.format("%+.1f%%", variation)
    }

    private fun indicateur(variation: Double?): String {
        return when {
            variation == null -> "–"
            variation >= 0 -> "↗"
            else -> "↘"
        }
    }

    /**
     * Libellé de la période réellement comparée.
     *
     * Une période en cours se compare au même avancement de la période de référence : il faut le
     * dire, sans quoi le lecteur croit comparer deux périodes entières.
     */
    fun getComparisonRangeLabel(): String {
        val debut = previousStartDate
        val fin = previousEndDate
        return if (debut != null && fin != null) "$debut → $fin" else ""
    }

    /**
     * Get period display name.
     */
    fun getPeriodDisplayName(): String {
        return when (period) {
            "WEEK" -> "Cette semaine"
            "MONTH" -> "Ce mois"
            "YEAR" -> "Cette année"
            else -> period
        }
    }

    companion object {
        const val PERIOD_WEEK = "WEEK"
        const val PERIOD_MONTH = "MONTH"
        const val PERIOD_YEAR = "YEAR"
    }
}

/**
 * Payment method summary.
 */
@Parcelize
data class PaymentMethodSummary(
    @SerializedName("code") val code: String,
    @SerializedName("label") val label: String,
    @SerializedName("amount") val amount: Long,
    @SerializedName("percent") val percent: Double,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("color") val color: String
) : Parcelable {

    fun getFormattedAmount(): String {
        return Dashboard.formatAmount(amount)
    }

    fun getFormattedPercent(): String {
        return String.format("%.1f%%", percent)
    }
}

/**
 * Top product with performance comparison.
 */
@Parcelize
data class TopProductPerformance(
    @SerializedName("rank") val rank: Int,
    @SerializedName("productId") val productId: Long,
    @SerializedName("productName") val productName: String,
    @SerializedName("codeCip") val codeCip: String?,
    @SerializedName("salesAmount") val salesAmount: Long,
    @SerializedName("quantitySold") val quantitySold: Int,
    @SerializedName("percentOfTotal") val percentOfTotal: Double,
    @SerializedName("variationPercent") val variationPercent: Double
) : Parcelable {

    fun getFormattedSalesAmount(): String {
        return Dashboard.formatAmount(salesAmount)
    }

    fun getVariationIndicator(): String {
        return when {
            variationPercent > 0 -> "↗"
            variationPercent < 0 -> "↘"
            else -> "→"
        }
    }

    fun isVariationPositive(): Boolean {
        return variationPercent >= 0
    }
}

/**
 * Data point for period chart.
 */
@Parcelize
data class PeriodDataPoint(
    @SerializedName("date") val date: String,
    @SerializedName("label") val label: String,
    @SerializedName("caAmount") val caAmount: Long,
    @SerializedName("transactionsCount") val transactionsCount: Int,
    @SerializedName("marginAmount") val marginAmount: Long
) : Parcelable
