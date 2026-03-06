package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Remise model
 * Maps to backend IRemise / Remise entity
 * Represents a predefined discount that can be applied to a sale
 */
@Parcelize
data class Remise(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("valeur")
    val valeur: String = "",

    @SerializedName("remiseValue")
    val remiseValue: Int? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("typeLibelle")
    val typeLibelle: String? = null,

    @SerializedName("enable")
    val enable: Boolean = true,

    @SerializedName("vnoDiscountRate")
    val vnoDiscountRate: Int? = null,

    @SerializedName("voDiscountRate")
    val voDiscountRate: Int? = null
) : Parcelable {

    /**
     * Get discount rate based on sale type
     * VNO = Comptant, VO = Assurance/Carnet
     */
    fun getDiscountRate(saleType: String): Int? {
        return when (saleType) {
            "COMPTANT" -> vnoDiscountRate
            else -> voDiscountRate
        }
    }

    /**
     * Display string: name + rate
     */
    fun getDisplayText(saleType: String): String {
        val rate = getDiscountRate(saleType)
        return if (rate != null) "$valeur ($rate%)" else valeur
    }
}
