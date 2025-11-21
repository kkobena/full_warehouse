package com.kobe.warehouse.inventory.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine

/**
 * Room entity for StoreInventoryLine
 * Offline storage for inventory line data
 */
@Entity(
    tableName = "store_inventory_lines",
    foreignKeys = [
        ForeignKey(
            entity = InventoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storeInventoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storeInventoryId"), Index("produitId")]
)
data class InventoryLineEntity(
    @PrimaryKey
    val id: Long,
    val quantityOnHand: Int,
    val quantityInit: Int,
    val quantitySold: Int,
    val gap: Int,
    val inventoryValueCost: Int,
    val lastUnitPrice: Int,
    val updated: Boolean,
    val updatedAt: String?,
    val storeInventoryId: Long,
    val produitId: Long,
    val produitLibelle: String,
    val produitCip: String?,
    val produitEan: String?,
    val produitCipsJson: String?, // JSON array of CIPs
    val rayonId: Long?,
    val rayonLibelle: String?,
    val locallyModified: Boolean = false, // Track if modified offline
    val syncStatus: String = "PENDING" // PENDING, SYNCED, ERROR
) {
    companion object {
        fun fromModel(model: StoreInventoryLine): InventoryLineEntity {
            return InventoryLineEntity(
                id = model.id,
                quantityOnHand = model.quantityOnHand,
                quantityInit = model.quantityInit,
                quantitySold = model.quantitySold,
                gap = model.gap,
                inventoryValueCost = model.inventoryValueCost,
                lastUnitPrice = model.lastUnitPrice,
                updated = model.updated,
                updatedAt = model.updatedAt,
                storeInventoryId = model.storeInventoryId,
                produitId = model.produitId,
                produitLibelle = model.produitLibelle,
                produitCip = model.produitCip,
                produitEan = model.produitEan,
                produitCipsJson = model.produitCips?.joinToString(","),
                rayonId = model.rayonId,
                rayonLibelle = model.rayonLibelle
            )
        }
    }

    fun toModel(): StoreInventoryLine {
        return StoreInventoryLine(
            id = id,
            quantityOnHand = quantityOnHand,
            quantityInit = quantityInit,
            quantitySold = quantitySold,
            gap = gap,
            inventoryValueCost = inventoryValueCost,
            lastUnitPrice = lastUnitPrice,
            updated = updated,
            updatedAt = updatedAt,
            storeInventoryId = storeInventoryId,
            produitId = produitId,
            produitLibelle = produitLibelle,
            produitCip = produitCip,
            produitEan = produitEan,
            produitCips = produitCipsJson?.split(",")?.toSet(),
            rayonId = rayonId,
            rayonLibelle = rayonLibelle
        )
    }
}
