package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Inventory lot model
 * Matches backend InventoryLotRecord
 * (GET/POST /api/store-inventory-lines/{lineId}/lots, PUT/DELETE /api/store-inventory-lines/lots/{lotId})
 */
data class InventoryLot(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("storeInventoryLineId")
    val storeInventoryLineId: Long? = null,

    /** Lot existant en base (référentiel) — null pour un nouveau lot créé par numLot */
    @SerializedName("lotId")
    val lotId: Long? = null,

    @SerializedName("numLot")
    val numLot: String? = null,

    /** Format ISO yyyy-MM-dd (LocalDate côté backend) */
    @SerializedName("expiryDate")
    val expiryDate: String? = null,

    @SerializedName("quantityOnHand")
    val quantityOnHand: Int? = null,

    @SerializedName("quantityInit")
    val quantityInit: Int? = null,

    @SerializedName("gap")
    val gap: Int? = null,

    @SerializedName("updated")
    val updated: Boolean = false,

    @SerializedName("lastUnitPrice")
    val lastUnitPrice: Int? = null
) {
    fun calculateGap(): Int = (quantityOnHand ?: 0) - (quantityInit ?: 0)
}
