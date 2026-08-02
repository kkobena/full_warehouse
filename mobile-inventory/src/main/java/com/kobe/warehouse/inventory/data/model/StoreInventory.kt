package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Storage reference as serialized by backend StorageDTO
 */
data class StorageRef(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("storageType")
    val storageType: String? = null
)

/**
 * Store Inventory model
 * Matches backend StoreInventoryDTO (returned by GET /api/store-inventories)
 */
data class StoreInventory(
    @SerializedName("id")
    val id: Long,

    @SerializedName("statut")
    val statut: InventoryStatut = InventoryStatut.CREATE,

    @SerializedName("inventoryType")
    val inventoryType: String? = null,

    @SerializedName("inventoryCategory")
    val inventoryCategory: CategoryInventory? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

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

    @SerializedName("storage")
    val storage: StorageRef? = null,

    @SerializedName("rayon")
    val rayon: Rayon? = null,

    @SerializedName("abbrName")
    val abbrName: String? = null,

    @SerializedName("userFullName")
    val userFullName: String? = null
) {
    fun getCategoryDisplay(): String =
        inventoryCategory?.label ?: inventoryCategory?.name?.name ?: ""

    fun getDisplayName(): String =
        description ?: getCategoryDisplay()

    fun isClosed(): Boolean = statut == InventoryStatut.CLOSED
    fun isActive(): Boolean = statut != InventoryStatut.CLOSED
}
