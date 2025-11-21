package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Store Inventory Line model
 * Represents a single product in an inventory
 * Matches backend StoreInventoryLine entity
 */
data class StoreInventoryLine(
    @SerializedName("id")
    val id: Long,

    @SerializedName("quantityOnHand")
    var quantityOnHand: Int = 0,

    @SerializedName("quantityInit")
    val quantityInit: Int = 0,

    @SerializedName("quantitySold")
    val quantitySold: Int = 0,

    @SerializedName("gap")
    val gap: Int = 0,

    @SerializedName("inventoryValueCost")
    val inventoryValueCost: Int = 0,

    @SerializedName("lastUnitPrice")
    val lastUnitPrice: Int = 0,

    @SerializedName("updated")
    var updated: Boolean = false,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("storeInventoryId")
    val storeInventoryId: Long,

    @SerializedName("produitId")
    val produitId: Long,

    @SerializedName("produitLibelle")
    val produitLibelle: String,

    @SerializedName("produitCip")
    val produitCip: String? = null,

    @SerializedName("produitEan")
    val produitEan: String? = null,

    @SerializedName("produitCips")
    val produitCips: Set<String>? = null,

    @SerializedName("rayonId")
    val rayonId: Long? = null,

    @SerializedName("rayonLibelle")
    val rayonLibelle: String? = null
) {
    /**
     * Calculate gap between quantity on hand and initial quantity
     */
    fun calculateGap(): Int {
        return quantityOnHand - quantityInit
    }

    /**
     * Check if this line has been counted/updated
     */
    fun isCounted(): Boolean = updated

    /**
     * Get all barcodes for this product
     */
    fun getAllBarcodes(): List<String> {
        val barcodes = mutableListOf<String>()
        produitCip?.let { barcodes.add(it) }
        produitEan?.let { barcodes.add(it) }
        produitCips?.let { barcodes.addAll(it) }
        return barcodes.distinct()
    }
}
