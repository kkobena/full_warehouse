package com.kobe.warehouse.inventory.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kobe.warehouse.inventory.data.model.InventoryLot
import com.kobe.warehouse.inventory.data.model.InventoryLotLine

/**
 * Cache local de la vue à plat « un lot = une ligne ».
 *
 * Sans lui, le mode lot — seul mode actif quand APP_GESTION_LOT_INVENTAIRE est
 * vrai — était inutilisable hors ligne : la liste ne se chargeait pas et chaque
 * quantité saisie partait directement au serveur. Le comptage doit pouvoir se
 * faire dans un rayon sans réseau, la transmission venant après.
 */
@Entity(
    tableName = "store_inventory_lot_lines",
    indices = [Index("storeInventoryId"), Index("produitId")]
)
data class InventoryLotLineEntity(
    @PrimaryKey
    val id: Long,
    val storeInventoryId: Long,
    val storeInventoryLineId: Long?,
    val produitId: Long,
    val produitCip: String?,
    val produitLibelle: String?,
    val numLot: String?,
    val expiryDate: String?,
    val quantityOnHand: Int?,
    val quantityInit: Int?,
    val gap: Int?,
    val updated: Boolean,
    val classePareto: String?,
    /** Contexte de chargement (filtre rayon), non renvoyé par l'API */
    val rayonId: Long? = null,
    val locallyModified: Boolean = false,
    val syncStatus: String = "PENDING"
) {
    fun toModel(): InventoryLotLine = InventoryLotLine(
        id = id,
        storeInventoryLineId = storeInventoryLineId,
        produitId = produitId,
        produitCip = produitCip,
        produitLibelle = produitLibelle,
        numLot = numLot,
        expiryDate = expiryDate,
        quantityOnHand = quantityOnHand,
        quantityInit = quantityInit,
        gap = gap,
        updated = updated,
        classePareto = classePareto
    )

    /** Charge utile de l'écriture PUT /store-inventory-lines/lots/{id} */
    fun toSyncPayload(): InventoryLot = InventoryLot(
        id = id,
        storeInventoryLineId = storeInventoryLineId,
        numLot = numLot,
        expiryDate = expiryDate,
        quantityOnHand = quantityOnHand,
        quantityInit = quantityInit,
        gap = gap,
        updated = true
    )

    companion object {
        fun fromModel(
            model: InventoryLotLine,
            storeInventoryId: Long,
            rayonId: Long? = null
        ): InventoryLotLineEntity = InventoryLotLineEntity(
            id = model.id ?: 0L,
            storeInventoryId = storeInventoryId,
            storeInventoryLineId = model.storeInventoryLineId,
            produitId = model.produitId,
            produitCip = model.produitCip,
            produitLibelle = model.produitLibelle,
            numLot = model.numLot,
            expiryDate = model.expiryDate,
            quantityOnHand = model.quantityOnHand,
            quantityInit = model.quantityInit,
            gap = model.gap,
            updated = model.updated,
            classePareto = model.classePareto,
            rayonId = rayonId,
            locallyModified = false,
            syncStatus = "SYNCED"
        )
    }
}
