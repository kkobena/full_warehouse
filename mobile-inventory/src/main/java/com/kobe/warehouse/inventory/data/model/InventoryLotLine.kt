package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Ligne de la vue à plat « un lot = une ligne »
 * (backend StoreInventoryLotLineRecord, GET /api/store-inventory-lines/lots).
 *
 * Mode de saisie retenu quand APP_GESTION_LOT_INVENTAIRE est actif : le comptage
 * se fait lot par lot, comme la grille lots du front Angular. Chaque ligne porte
 * l'identité du produit *et* celle du lot, ce qui permet la saisie directe — la
 * liste par produit ne connaît, elle, que le nombre de lots.
 */
data class InventoryLotLine(
    /**
     * Identifiant du lot d'inventaire, cible de l'écriture. Nul pour un produit du
     * périmètre dépourvu de lot (aucun lot, ou tous à zéro) : la saisie porte alors sur
     * la ligne produit — voir [isLotLess].
     */
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("storeInventoryLineId")
    val storeInventoryLineId: Long? = null,

    @SerializedName("produitId")
    val produitId: Long = 0,

    @SerializedName("produitCip")
    val produitCip: String? = null,

    @SerializedName("produitLibelle")
    val produitLibelle: String? = null,

    @SerializedName("numLot")
    val numLot: String? = null,

    /** ISO yyyy-MM-dd */
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

    @SerializedName("classePareto")
    val classePareto: String? = null,

    /** Verrou optimiste de la ligne produit — renseigné pour les lignes sans lot */
    @SerializedName("version")
    val version: Long? = null
) {
    fun calculateGap(): Int = (quantityOnHand ?: 0) - (quantityInit ?: 0)

    fun isCounted(): Boolean = updated

    /**
     * Produit du périmètre sans aucun lot d'inventaire. Il n'y a rien à saisir au niveau
     * du lot : le comptage passe par l'API ligne produit.
     */
    fun isLotLess(): Boolean = id == null
}

/**
 * Paramètre applicatif (backend AppConfiguration, GET /api/app/{id})
 */
data class AppConfig(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("value")
    val value: String? = null
) {
    /** Convention backend : « 1 » vaut activé */
    fun isEnabled(): Boolean = value == "1"
}
