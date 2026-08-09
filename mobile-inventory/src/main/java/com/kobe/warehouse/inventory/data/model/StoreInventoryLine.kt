package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Store Inventory Line model (read side)
 * Matches backend StoreInventoryLineRecord (returned by GET /api/store-inventory-lines/v2)
 */
data class StoreInventoryLine(
    @SerializedName("id")
    val id: Long,

    @SerializedName("produitId")
    val produitId: Long,

    @SerializedName("produitLibelle")
    val produitLibelle: String? = null,

    @SerializedName("produitCip")
    val produitCip: String? = null,

    @SerializedName("produitEan")
    val produitEan: String? = null,

    @SerializedName("quantityOnHand")
    val quantityOnHand: Int? = null,

    @SerializedName("quantityInit")
    val quantityInit: Int = 0,

    @SerializedName("gap")
    val gap: Int? = null,

    @SerializedName("updated")
    val updated: Boolean = false,

    @SerializedName("prixAchat")
    val prixAchat: Int? = null,

    @SerializedName("prixUni")
    val prixUni: Int? = null,

    @SerializedName("storageId")
    val storageId: Long? = null,

    @SerializedName("seuilMini")
    val seuilMini: Int? = null,

    @SerializedName("lotCount")
    val lotCount: Int = 0,

    @SerializedName("classePareto")
    val classePareto: String? = null,

    /** Traçabilité : abréviation du compteur (null si non comptée) */
    @SerializedName("countedBy")
    val countedBy: String? = null,

    /** Traçabilité : date du dernier comptage (ISO LocalDateTime) */
    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    /** Verrou optimiste : version lue, renvoyée à l'écriture */
    @SerializedName("version")
    val version: Long? = null
) {
    /**
     * Gap between counted quantity and initial quantity
     */
    fun calculateGap(): Int = (quantityOnHand ?: 0) - quantityInit

    /**
     * Check if this line has been counted/updated
     */
    fun isCounted(): Boolean = updated

    /**
     * All barcodes attached to this product
     */
    fun getAllBarcodes(): List<String> =
        listOfNotNull(produitCip, produitEan).distinct()

    /**
     * GTIN-14, EAN-13 et UPC-12 ne diffèrent que par des zéros de tête : le même
     * article se présente en `04056649511830` dans un DataMatrix et en
     * `4056649511830` dans le référentiel. Les codes purement numériques sont donc
     * comparés sans leurs zéros initiaux ; les codes alphanumériques restent
     * comparés tels quels.
     */
    fun matchesBarcode(barcode: String): Boolean =
        getAllBarcodes().any { it.equals(barcode, ignoreCase = true) } ||
            getAllBarcodes().any { normalizeCode(it) != null && normalizeCode(it) == normalizeCode(barcode) }

    private fun normalizeCode(code: String): String? =
        code.takeIf { it.isNotBlank() && it.all(Char::isDigit) }
            ?.trimStart('0')
            ?.takeIf { it.isNotEmpty() }
}

/**
 * Write payload for PUT /api/store-inventory-lines and PUT /api/store-inventory-lines/batch
 * Matches backend StoreInventoryLineDTO (produitId is mandatory)
 */
data class StoreInventoryLineSync(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("produitId")
    val produitId: Long,

    @SerializedName("storeInventoryId")
    val storeInventoryId: Long,

    @SerializedName("quantityOnHand")
    val quantityOnHand: Int,

    @SerializedName("quantityInit")
    val quantityInit: Int = 0,

    @SerializedName("gap")
    val gap: Int? = null,

    @SerializedName("updated")
    val updated: Boolean = true,

    /** Verrou optimiste : version de la ligne telle que lue par ce terminal */
    @SerializedName("version")
    val version: Long? = null
)

/**
 * Result of PUT /api/store-inventory-lines/batch (backend BatchSyncResultRecord)
 */
data class BatchSyncResult(
    @SerializedName("saved")
    val saved: Int = 0,

    @SerializedName("failed")
    val failed: Int = 0,

    @SerializedName("failedIds")
    val failedIds: List<Long>? = null,

    /** Lignes rejetées car comptées entre-temps par un autre opérateur */
    @SerializedName("conflictedIds")
    val conflictedIds: List<Long>? = null
) {
    val conflicted: Int get() = conflictedIds?.size ?: 0
}

/**
 * Result of GET /api/store-inventories/{id}/progress (backend InventoryProgressRecord)
 */
data class InventoryProgress(
    @SerializedName("inventoryId")
    val inventoryId: Long? = null,

    @SerializedName("totalLines")
    val totalLines: Long = 0,

    @SerializedName("updatedLines")
    val updatedLines: Long = 0,

    @SerializedName("linesWithGap")
    val linesWithGap: Long = 0,

    @SerializedName("progressPercent")
    val progressPercent: Int = 0
)
