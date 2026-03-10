package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName
import com.kobe.warehouse.reports.data.model.Dashboard.Companion.formatAmount

data class RecapProduitVendu(
    @SerializedName("id") val id: Int,
    @SerializedName("libelle") val libelle: String,
    @SerializedName("codeCip") val codeCip: String?,
    @SerializedName("codeEanLaboratoire") val codeEanLaboratoire: String?,
    @SerializedName("rayonName") val rayonName: String?,
    @SerializedName("quantitySold") val quantitySold: Int,
    @SerializedName("quantityAvoir") val quantityAvoir: Int,
    @SerializedName("totalSalesAmount") val totalSalesAmount: Int,
    @SerializedName("totalPurchaseAmount") val totalPurchaseAmount: Int,
    @SerializedName("stock") val stock: Int
) {
    fun getFormattedSalesAmount(): String = formatAmount(totalSalesAmount.toLong())
    fun getNetQuantity(): Int = quantitySold - quantityAvoir
}

data class RecapProduitVenduSummary(
    @SerializedName("totalProducts") val totalProducts: Long,
    @SerializedName("quantitySold") val quantitySold: Int,
    @SerializedName("quantityAvoir") val quantityAvoir: Int,
    @SerializedName("totalSalesAmount") val totalSalesAmount: Long,
    @SerializedName("totalPurchaseAmount") val totalPurchaseAmount: Long,
    @SerializedName("totalStock") val totalStock: Long
) {
    fun getFormattedSalesAmount(): String = formatAmount(totalSalesAmount)
    fun getNetQuantity(): Int = quantitySold - quantityAvoir
}
