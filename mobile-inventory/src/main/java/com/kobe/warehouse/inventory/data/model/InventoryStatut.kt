package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Inventory Status enum
 * Matches backend com.kobe.warehouse.domain.enumeration.InventoryStatut
 */
enum class InventoryStatut {
    @SerializedName("CREATE")
    CREATE,

    @SerializedName("PROCESSING")
    PROCESSING,

    @SerializedName("CLOSED")
    CLOSED;

    fun displayLabel(): String = when (this) {
        CREATE -> "Créé"
        PROCESSING -> "En cours"
        CLOSED -> "Clôturé"
    }
}
