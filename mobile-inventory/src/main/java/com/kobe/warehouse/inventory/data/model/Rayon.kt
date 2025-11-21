package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Rayon (Section/Aisle) model
 * Represents a section or aisle in the warehouse
 */
data class Rayon(
    @SerializedName("id")
    val id: Long,

    @SerializedName("code")
    val code: String,

    @SerializedName("libelle")
    val libelle: String,

    @SerializedName("storageId")
    val storageId: Long,

    @SerializedName("storageName")
    val storageName: String,

    @SerializedName("storeInventoryId")
    val storeInventoryId: Long
) {
    fun getDisplayName(): String = "$code - $libelle"
}
