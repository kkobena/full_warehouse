package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * Store Inventory model
 * Matches backend StoreInventory entity
 */
data class StoreInventory(
    @SerializedName("id")
    val id: Long,

    @SerializedName("inventoryCategory")
    val inventoryCategory: InventoryCategory,

    @SerializedName("statut")
    val statut: InventoryStatut = InventoryStatut.OPEN,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("inventoryAmountBegin")
    val inventoryAmountBegin: Long = 0,

    @SerializedName("inventoryAmountAfter")
    val inventoryAmountAfter: Long = 0,

    @SerializedName("inventoryValueCostBegin")
    val inventoryValueCostBegin: Long = 0,

    @SerializedName("inventoryValueCostAfter")
    val inventoryValueCostAfter: Long = 0,

    @SerializedName("gapCost")
    val gapCost: Int = 0,

    @SerializedName("gapAmount")
    val gapAmount: Int = 0,

    @SerializedName("storageId")
    val storageId: Long? = null,

    @SerializedName("storageName")
    val storageName: String? = null,

    @SerializedName("rayonId")
    val rayonId: Long? = null,

    @SerializedName("rayonLibelle")
    val rayonLibelle: String? = null,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("userLogin")
    val userLogin: String
) {
    fun getCategoryDisplay(): String {
        return when (inventoryCategory) {
            InventoryCategory.MAGASIN -> "Magasin"
            InventoryCategory.RAYON -> "Rayon: ${rayonLibelle ?: ""}"
            InventoryCategory.STORAGE -> "Emplacement: ${storageName ?: ""}"
            InventoryCategory.FAMILLY -> "Famille"
        }
    }

    fun isOpen(): Boolean = statut == InventoryStatut.OPEN
    fun isClosed(): Boolean = statut == InventoryStatut.CLOSED
}
