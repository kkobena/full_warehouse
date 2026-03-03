package com.kobe.warehouse.sales.data.model

import com.google.gson.annotations.SerializedName

/**
 * Decondition model
 * Represents a deconditioning operation (splitting a CH/box into detail units)
 */
data class Decondition(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("qtyMvt")
    val qtyMvt: Int,

    @SerializedName("produitId")
    val produitId: Long
)
