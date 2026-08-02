package com.kobe.warehouse.inventory.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kobe.warehouse.inventory.data.model.CategoryInventory
import com.kobe.warehouse.inventory.data.model.InventoryCategory
import com.kobe.warehouse.inventory.data.model.InventoryStatut
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StorageRef
import com.kobe.warehouse.inventory.data.model.StoreInventory

/**
 * Room entity for StoreInventory
 * Offline storage for inventory data
 */
@Entity(tableName = "store_inventories")
data class InventoryEntity(
    @PrimaryKey
    val id: Long,
    val categoryName: String?,
    val categoryLabel: String?,
    val statut: String,
    val inventoryType: String?,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?,
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
    val abbrName: String?,
    val syncStatus: String = "PENDING" // PENDING, SYNCED, ERROR
) {
    companion object {
        fun fromModel(model: StoreInventory): InventoryEntity {
            return InventoryEntity(
                id = model.id,
                categoryName = model.inventoryCategory?.name?.name,
                categoryLabel = model.inventoryCategory?.label,
                statut = model.statut.name,
                inventoryType = model.inventoryType,
                description = model.description,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                inventoryAmountBegin = model.inventoryAmountBegin,
                inventoryAmountAfter = model.inventoryAmountAfter,
                inventoryValueCostBegin = model.inventoryValueCostBegin,
                inventoryValueCostAfter = model.inventoryValueCostAfter,
                gapCost = model.gapCost,
                gapAmount = model.gapAmount,
                storageId = model.storage?.id,
                storageName = model.storage?.name,
                rayonId = model.rayon?.id,
                rayonLibelle = model.rayon?.libelle,
                abbrName = model.abbrName
            )
        }
    }

    fun toModel(): StoreInventory {
        return StoreInventory(
            id = id,
            inventoryCategory = CategoryInventory(
                name = categoryName?.let { runCatching { InventoryCategory.valueOf(it) }.getOrNull() },
                label = categoryLabel
            ),
            statut = runCatching { InventoryStatut.valueOf(statut) }.getOrDefault(InventoryStatut.CREATE),
            inventoryType = inventoryType,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            inventoryAmountBegin = inventoryAmountBegin,
            inventoryAmountAfter = inventoryAmountAfter,
            inventoryValueCostBegin = inventoryValueCostBegin,
            inventoryValueCostAfter = inventoryValueCostAfter,
            gapCost = gapCost,
            gapAmount = gapAmount,
            storage = storageId?.let { StorageRef(id = it, name = storageName) },
            rayon = rayonId?.let { Rayon(id = it, libelle = rayonLibelle) },
            abbrName = abbrName
        )
    }
}
