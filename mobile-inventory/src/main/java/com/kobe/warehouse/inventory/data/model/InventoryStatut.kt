package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Inventory Status enum
 * Matches backend InventoryStatut enum
 */
enum class InventoryStatut {
    @SerializedName("OPEN")
    OPEN,

    @SerializedName("CLOSED")
    CLOSED
}
