package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Rayon (section) model
 * Matches backend RayonDTO (returned by GET /api/rayons)
 */
data class Rayon(
    @SerializedName("id")
    val id: Long,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("libelle")
    val libelle: String? = null,

    @SerializedName("storageId")
    val storageId: Long? = null,

    @SerializedName("storageLibelle")
    val storageLibelle: String? = null,

    @SerializedName("storageType")
    val storageType: String? = null
) {
    fun getDisplayName(): String =
        listOfNotNull(code, libelle).joinToString(" - ").ifEmpty { "Rayon $id" }
}
