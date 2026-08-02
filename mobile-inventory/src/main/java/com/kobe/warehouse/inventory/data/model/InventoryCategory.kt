package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Inventory Category enum
 * Matches backend com.kobe.warehouse.domain.enumeration.InventoryCategory
 */
enum class InventoryCategory {
    @SerializedName("MAGASIN")
    MAGASIN,

    @SerializedName("STORAGE")
    STORAGE,

    @SerializedName("RAYON")
    RAYON,

    @SerializedName("FAMILLY")
    FAMILLY,

    @SerializedName("PERIME")
    PERIME,

    @SerializedName("ALERTE_PEREMPTION")
    ALERTE_PEREMPTION,

    @SerializedName("VENDU")
    VENDU,

    @SerializedName("INVENDU")
    INVENDU,

    @SerializedName("SOUS_SEUIL")
    SOUS_SEUIL,

    @SerializedName("EN_RUPTURE")
    EN_RUPTURE,

    @SerializedName("SELECTION_PRODUIT")
    SELECTION_PRODUIT,

    @SerializedName("GROSSISTE")
    GROSSISTE,

    @SerializedName("ABC")
    ABC
}

/**
 * Category wrapper as serialized by the backend (CategoryInventory DTO):
 * { "name": "MAGASIN", "label": "Inventaire global" }
 */
data class CategoryInventory(
    @SerializedName("name")
    val name: InventoryCategory? = null,

    @SerializedName("label")
    val label: String? = null
)
