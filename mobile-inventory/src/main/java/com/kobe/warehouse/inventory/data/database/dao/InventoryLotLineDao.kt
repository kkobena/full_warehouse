package com.kobe.warehouse.inventory.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.kobe.warehouse.inventory.data.database.entity.InventoryLotLineEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accès local à la vue à plat des lots (mode gestion des lots).
 *
 * L'ordre reprend celui du backend (code_cip, libellé, n° de lot) pour que le mode
 * hors ligne présente les lignes dans la même séquence que le mode connecté.
 */
@Dao
interface InventoryLotLineDao {

    companion object {
        private const val BACKEND_ORDER =
            " ORDER BY produitCip IS NULL, produitCip, produitLibelle, numLot"
        const val BY_INVENTORY =
            "SELECT * FROM store_inventory_lot_lines WHERE storeInventoryId = :inventoryId" + BACKEND_ORDER
        const val BY_RAYON =
            "SELECT * FROM store_inventory_lot_lines WHERE storeInventoryId = :inventoryId AND rayonId = :rayonId" + BACKEND_ORDER
    }

    @Query(BY_INVENTORY)
    suspend fun getLotLinesOnce(inventoryId: Long): List<InventoryLotLineEntity>

    @Query(BY_RAYON)
    suspend fun getLotLinesByRayonOnce(inventoryId: Long, rayonId: Long): List<InventoryLotLineEntity>

    @Query("SELECT * FROM store_inventory_lot_lines WHERE id = :id")
    suspend fun getLotLineById(id: Long): InventoryLotLineEntity?

    @Upsert
    suspend fun upsertLotLine(line: InventoryLotLineEntity)

    @Upsert
    suspend fun upsertLotLines(lines: List<InventoryLotLineEntity>)

    @Update
    suspend fun updateLotLine(line: InventoryLotLineEntity)

    @Query("SELECT * FROM store_inventory_lot_lines WHERE locallyModified = 1 AND syncStatus IN ('PENDING', 'ERROR')")
    suspend fun getUnsyncedLotLines(): List<InventoryLotLineEntity>

    @Query("SELECT COUNT(*) FROM store_inventory_lot_lines WHERE locallyModified = 1 AND syncStatus IN ('PENDING', 'ERROR')")
    fun getPendingSyncCount(): Flow<Int>

    @Query("SELECT id FROM store_inventory_lot_lines WHERE locallyModified = 1 AND syncStatus IN ('PENDING', 'ERROR')")
    fun getPendingSyncIds(): Flow<List<Long>>

    @Query("DELETE FROM store_inventory_lot_lines WHERE storeInventoryId = :inventoryId")
    suspend fun deleteByInventoryId(inventoryId: Long)
}
