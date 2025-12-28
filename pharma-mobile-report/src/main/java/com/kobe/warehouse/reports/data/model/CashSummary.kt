package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.reports.utils.NumberFormatUtils
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalTime

/**
 * Cash Summary (Ticket Z / Récapitulatif Caisse) data model.
 */
@Parcelize
data class CashSummary(
    @SerializedName("fromDate")
    val fromDate: String,

    @SerializedName("toDate")
    val toDate: String,

    @SerializedName("fromTime")
    val fromTime: String?,

    @SerializedName("toTime")
    val toTime: String?,

    @SerializedName("periodLabel")
    val periodLabel: String,

    @SerializedName("globalSummary")
    val globalSummary: List<SummaryItem>,

    @SerializedName("cashierRecaps")
    val cashierRecaps: List<CashierRecap>,

    @SerializedName("totalTtc")
    val totalTtc: Long,

    @SerializedName("totalEspeces")
    val totalEspeces: Long,

    @SerializedName("totalCartes")
    val totalCartes: Long,

    @SerializedName("totalMobileMoney")
    val totalMobileMoney: Long,

    @SerializedName("totalCheques")
    val totalCheques: Long,

    @SerializedName("totalVirements")
    val totalVirements: Long,

    @SerializedName("totalCredit")
    val totalCredit: Long,

    @SerializedName("totalMobile")
    val totalMobile: Long,

    @SerializedName("cashierCount")
    val cashierCount: Int
) : Parcelable {

    fun getFormattedTotalTtc(): String = NumberFormatUtils.formatCurrency(totalTtc)

    fun getFormattedTotalEspeces(): String = NumberFormatUtils.formatCurrency(totalEspeces)

    fun getFormattedTotalCartes(): String = NumberFormatUtils.formatCurrency(totalCartes)

    fun getFormattedTotalMobileMoney(): String = NumberFormatUtils.formatCurrency(totalMobileMoney)

    fun getFormattedTotalCheques(): String = NumberFormatUtils.formatCurrency(totalCheques)

    fun getFormattedTotalVirements(): String = NumberFormatUtils.formatCurrency(totalVirements)

    fun getFormattedTotalCredit(): String = NumberFormatUtils.formatCurrency(totalCredit)

    fun isEmpty(): Boolean = cashierRecaps.isEmpty() && globalSummary.isEmpty()
}

/**
 * Summary item representing a single line in the cash summary.
 */
@Parcelize
data class SummaryItem(
    @SerializedName("key")
    val key: String,

    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("value")
    val value: Long,

    @SerializedName("secondValue")
    val secondValue: Long,

    @SerializedName("type")
    val type: String
) : Parcelable {

    fun getFormattedValue(): String = NumberFormatUtils.formatCurrency(value)

    fun getFormattedSecondValue(): String = NumberFormatUtils.formatCurrency(secondValue)

    fun isAmount(): Boolean = type == TYPE_AMOUNT

    fun isCount(): Boolean = type == TYPE_COUNT

    fun isPercent(): Boolean = type == TYPE_PERCENT

    companion object {
        const val TYPE_AMOUNT = "AMOUNT"
        const val TYPE_COUNT = "COUNT"
        const val TYPE_PERCENT = "PERCENT"
    }
}

/**
 * Cashier recap containing details for a single cashier.
 */
@Parcelize
data class CashierRecap(
    @SerializedName("userId")
    val userId: Long,

    @SerializedName("userName")
    val userName: String,

    @SerializedName("userInitials")
    val userInitials: String,

    @SerializedName("details")
    val details: List<SummaryItem>,

    @SerializedName("summary")
    val summary: List<SummaryItem>
) : Parcelable {

    /**
     * Get total amount for this cashier from details.
     */
    fun getTotal(): Long = details.sumOf { it.value }

    /**
     * Get formatted total.
     */
    fun getFormattedTotal(): String = NumberFormatUtils.formatCurrency(getTotal())

    /**
     * Check if this cashier has mobile payment totals.
     */
    fun hasMobileTotal(): Boolean = summary.isNotEmpty()
}
