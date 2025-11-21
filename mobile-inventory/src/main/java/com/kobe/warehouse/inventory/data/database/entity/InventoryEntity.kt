package com.kobe.warehouse.inventory.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kobe.warehouse.inventory.data.model.InventoryCategory
import com.kobe.warehouse.inventory.data.model.InventoryStatut
import com.kobe.warehouse.inventory.data.model.StoreInventory

/**
 * Room entity for StoreInventory
 * Offline storage for inventory data
 */
@Entity(tableName = "store_inventories")
data class InventoryEntity(
    @PrimaryKey
    val id: Long,
    val inventoryCategory: String,
    val statut: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val inventoryAmountBegin: Long,
    val inventoryAmountAfter: Long,
    val inventoryValueCostBegin: Long,
    val inventoryValueCostAfter: Long,
    val gapCost: Int,
    val gapAmount: Int,
    val storageId: Long?,
    val storageName: String?,
    val rayonId: Long?,
    val rayonLibelle: String?,
    val userId: Long,
    val userLogin: String,
    val syncStatus: String = "PENDING" // PENDING, SYNCED, ERROR
) {
    companion object {
        fun fromModel(model: StoreInventory): InventoryEntity {
            return InventoryEntity(
                id = model.id,
                inventoryCategory = model.inventoryCategory.name,
                statut = model.statut.name,
                description = model.description,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                inventoryAmountBegin = model.inventoryAmountBegin,
                inventoryAmountAfter = model.inventoryAmountAfter,
                inventoryValueCostBegin = model.inventoryValueCostBegin,
                inventoryValueCostAfter = model.inventoryValueCostAfter,
                gapCost = model.gapCost,
                gapAmount = model.gapAmount,
                storageId = model.storageId,
                storageName = model.storageName,
                rayonId = model.rayonId,
                rayonLibelle = model.rayonLibelle,
                userId = model.userId,
                userLogin = model.userLogin
            )
        }
    }

    fun toModel(): StoreInventory {
        return StoreInventory(
            id = id,
            inventoryCategory = InventoryCategory.valueOf(inventoryCategory),
            statut = InventoryStatut.valueOf(statut),
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            inventoryAmountBegin = inventoryAmountBegin,
            inventoryAmountAfter = inventoryAmountAfter,
            inventoryValueCostBegin = inventoryValueCostBegin,
            inventoryValueCostAfter = inventoryValueCostAfter,
            gapCost = gapCost,
            gapAmount = gapAmount,
            storageId = storageId,
            storageName = storageName,
            rayonId = rayonId,
            rayonLibelle = rayonLibelle,
            userId = userId,
            userLogin = userLogin
        )
    }
}
