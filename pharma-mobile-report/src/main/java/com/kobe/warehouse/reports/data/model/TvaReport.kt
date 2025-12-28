package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.reports.utils.NumberFormatUtils
import kotlinx.parcelize.Parcelize

/**
 * TVA Report data model.
 * Provides VAT breakdown by rate with totals.
 */
@Parcelize
data class TvaReport(
    @SerializedName("fromDate")
    val fromDate: String,

    @SerializedName("toDate")
    val toDate: String,

    @SerializedName("periodLabel")
    val periodLabel: String,

    // Totaux
    @SerializedName("montantHt")
    val montantHt: Long,

    @SerializedName("montantTva")
    val montantTva: Long,

    @SerializedName("montantTtc")
    val montantTtc: Long,

    @SerializedName("montantNet")
    val montantNet: Long,

    @SerializedName("montantRemise")
    val montantRemise: Long,

    @SerializedName("montantAchat")
    val montantAchat: Long,

    // UG (Unites Gratuites)
    @SerializedName("montantTvaUg")
    val montantTvaUg: Long,

    @SerializedName("montantTtcUg")
    val montantTtcUg: Long,

    @SerializedName("montantRemiseUg")
    val montantRemiseUg: Long,

    @SerializedName("amountToBeTakenIntoAccount")
    val amountToBeTakenIntoAccount: Long,

    // Breakdown by TVA rate
    @SerializedName("tvaBreakdown")
    val tvaBreakdown: List<TvaRateBreakdown>,

    // Chart data
    @SerializedName("chartData")
    val chartData: List<TvaChartData>
) : Parcelable {

    fun getFormattedMontantHt(): String = NumberFormatUtils.formatCurrency(montantHt)

    fun getFormattedMontantTva(): String = NumberFormatUtils.formatCurrency(montantTva)

    fun getFormattedMontantTtc(): String = NumberFormatUtils.formatCurrency(montantTtc)

    fun getFormattedMontantNet(): String = NumberFormatUtils.formatCurrency(montantNet)

    fun getFormattedMontantRemise(): String = NumberFormatUtils.formatCurrency(montantRemise)

    fun getFormattedMontantAchat(): String = NumberFormatUtils.formatCurrency(montantAchat)

    fun getFormattedAmountToBeTakenIntoAccount(): String = NumberFormatUtils.formatCurrency(amountToBeTakenIntoAccount)

    fun isEmpty(): Boolean = montantTtc == 0L && tvaBreakdown.isEmpty()
}

/**
 * TVA breakdown by rate.
 */
@Parcelize
data class TvaRateBreakdown(
    @SerializedName("codeTva")
    val codeTva: Int,

    @SerializedName("rateName")
    val rateName: String,

    @SerializedName("montantHt")
    val montantHt: Long,

    @SerializedName("montantTva")
    val montantTva: Long,

    @SerializedName("montantTtc")
    val montantTtc: Long,

    @SerializedName("montantAchat")
    val montantAchat: Long,

    @SerializedName("amountToBeTakenIntoAccount")
    val amountToBeTakenIntoAccount: Long,

    @SerializedName("date")
    val date: String?
) : Parcelable {

    fun getFormattedMontantHt(): String = NumberFormatUtils.formatCurrency(montantHt)

    fun getFormattedMontantTva(): String = NumberFormatUtils.formatCurrency(montantTva)

    fun getFormattedMontantTtc(): String = NumberFormatUtils.formatCurrency(montantTtc)

    fun getFormattedMontantAchat(): String = NumberFormatUtils.formatCurrency(montantAchat)

    fun getPercent(totalTtc: Long): Double {
        if (totalTtc == 0L) return 0.0
        return kotlin.math.round(montantTtc * 1000.0 / totalTtc) / 10.0
    }
}

/**
 * TVA chart data point.
 */
@Parcelize
data class TvaChartData(
    @SerializedName("label")
    val label: String,

    @SerializedName("value")
    val value: Long,

    @SerializedName("percent")
    val percent: Double,

    @SerializedName("color")
    val color: String
) : Parcelable {

    fun getFormattedValue(): String = NumberFormatUtils.formatCurrency(value)

    fun getFormattedPercent(): String = NumberFormatUtils.formatPercent(percent)
}
