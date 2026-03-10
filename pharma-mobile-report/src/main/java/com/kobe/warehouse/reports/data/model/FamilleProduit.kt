package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName

data class FamilleProduit(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String?,
    @SerializedName("libelle") val libelle: String
) {
    override fun toString(): String = libelle
}
