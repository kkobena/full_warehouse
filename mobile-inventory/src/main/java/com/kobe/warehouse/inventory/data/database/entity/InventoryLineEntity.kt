package com.kobe.warehouse.inventory.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.model.StoreInventoryLineSync

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
    val storeInventoryId: Long,
    val produitId: Long,
    val produitLibelle: String?,
    val produitCip: String?,
    val produitEan: String?,
    val quantityOnHand: Int?,
    val quantityInit: Int,
    val gap: Int?,
    val updated: Boolean,
    val prixAchat: Int?,
    val prixUni: Int?,
    val lotCount: Int,
    val classePareto: String?,
    val rayonId: Long?, // contexte de chargement (filtre rayon), pas renvoyé par l'API
    val countedBy: String? = null,
    val serverUpdatedAt: String? = null,
    /** Verrou optimiste : version serveur au moment de la lecture */
    val version: Long? = null,
    val locallyModified: Boolean = false, // Track if modified offline
    val syncStatus: String = "PENDING" // PENDING, SYNCED, ERROR, CONFLICT
) {
    companion object {
        fun fromModel(
            model: StoreInventoryLine,
            storeInventoryId: Long,
            rayonId: Long? = null
        ): InventoryLineEntity {
            return InventoryLineEntity(
                id = model.id,
                storeInventoryId = storeInventoryId,
                produitId = model.produitId,
                produitLibelle = model.produitLibelle,
                produitCip = model.produitCip,
                produitEan = model.produitEan,
                quantityOnHand = model.quantityOnHand,
                quantityInit = model.quantityInit,
                gap = model.gap,
                updated = model.updated,
                prixAchat = model.prixAchat,
                prixUni = model.prixUni,
                lotCount = model.lotCount,
                classePareto = model.classePareto,
                rayonId = rayonId,
                countedBy = model.countedBy,
                serverUpdatedAt = model.updatedAt,
                version = model.version
            )
        }
    }

    fun toModel(): StoreInventoryLine {
        return StoreInventoryLine(
            id = id,
            produitId = produitId,
            produitLibelle = produitLibelle,
            produitCip = produitCip,
            produitEan = produitEan,
            quantityOnHand = quantityOnHand,
            quantityInit = quantityInit,
            gap = gap,
            updated = updated,
            prixAchat = prixAchat,
            prixUni = prixUni,
            lotCount = lotCount,
            classePareto = classePareto,
            countedBy = countedBy,
            updatedAt = serverUpdatedAt,
            version = version
        )
    }

    fun toSyncPayload(): StoreInventoryLineSync {
        return StoreInventoryLineSync(
            id = id,
            produitId = produitId,
            storeInventoryId = storeInventoryId,
            quantityOnHand = quantityOnHand ?: 0,
            quantityInit = quantityInit,
            gap = gap,
            updated = updated,
            version = version
        )
    }
}
