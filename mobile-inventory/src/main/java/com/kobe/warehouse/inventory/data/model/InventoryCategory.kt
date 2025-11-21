package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Inventory Category enum
 * Matches backend InventoryCategory enum
 */
enum class InventoryCategory {
    @SerializedName("MAGASIN")
    MAGASIN,

    @SerializedName("RAYON")
    RAYON,

    @SerializedName("STORAGE")
    STORAGE,

    @SerializedName("FAMILLY")
    FAMILLY
}
