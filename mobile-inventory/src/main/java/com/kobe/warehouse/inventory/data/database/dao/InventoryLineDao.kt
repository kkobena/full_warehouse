package com.kobe.warehouse.inventory.data.database.dao

import androidx.room.*
import com.kobe.warehouse.inventory.data.database.entity.InventoryLineEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for inventory line operations
 * Provides offline access to inventory line data
 */
@Dao
interface InventoryLineDao {

    @Query("SELECT * FROM store_inventory_lines WHERE storeInventoryId = :inventoryId")
    fun getInventoryLines(inventoryId: Long): Flow<List<InventoryLineEntity>>

    @Query("SELECT * FROM store_inventory_lines WHERE storeInventoryId = :inventoryId")
    suspend fun getInventoryLinesOnce(inventoryId: Long): List<InventoryLineEntity>

    @Query("SELECT * FROM store_inventory_lines WHERE storeInventoryId = :inventoryId AND rayonId = :rayonId")
    fun getInventoryLinesByRayon(inventoryId: Long, rayonId: Long): Flow<List<InventoryLineEntity>>

    @Query("SELECT * FROM store_inventory_lines WHERE storeInventoryId = :inventoryId AND rayonId = :rayonId")
    suspend fun getInventoryLinesByRayonOnce(inventoryId: Long, rayonId: Long): List<InventoryLineEntity>

    @Query("SELECT * FROM store_inventory_lines WHERE id = :id")
    suspend fun getInventoryLineById(id: Long): InventoryLineEntity?

    @Query("SELECT * FROM store_inventory_lines WHERE storeInventoryId = :inventoryId AND (produitCip = :barcode OR produitEan = :barcode)")
    suspend fun findLineByBarcode(inventoryId: Long, barcode: String): InventoryLineEntity?

    @Upsert
    suspend fun upsertLine(line: InventoryLineEntity)

    @Upsert
    suspend fun upsertLines(lines: List<InventoryLineEntity>)

    @Update
    suspend fun updateLine(line: InventoryLineEntity)

    @Delete
    suspend fun deleteLine(line: InventoryLineEntity)

    @Query("DELETE FROM store_inventory_lines WHERE storeInventoryId = :inventoryId")
    suspend fun deleteLinesByInventoryId(inventoryId: Long)

    @Query("DELETE FROM store_inventory_lines")
    suspend fun deleteAllLines()

    @Query("SELECT * FROM store_inventory_lines WHERE locallyModified = 1 AND syncStatus = 'PENDING'")
    suspend fun getPendingSyncLines(): List<InventoryLineEntity>

    @Query("SELECT * FROM store_inventory_lines WHERE locallyModified = 1 AND syncStatus IN ('PENDING', 'ERROR')")
    suspend fun getUnsyncedLines(): List<InventoryLineEntity>

    @Query("SELECT COUNT(*) FROM store_inventory_lines WHERE locallyModified = 1 AND syncStatus IN ('PENDING', 'ERROR')")
    fun getPendingSyncCount(): Flow<Int>

    /** Lignes rejetées pour comptage concurrent : nécessitent un arbitrage manuel */
    @Query("SELECT COUNT(*) FROM store_inventory_lines WHERE syncStatus = 'CONFLICT'")
    fun getConflictCount(): Flow<Int>

    @Query("UPDATE store_inventory_lines SET syncStatus = 'SYNCED', locallyModified = 0 WHERE syncStatus = 'CONFLICT'")
    suspend fun clearConflicts()

    @Query("SELECT COUNT(*) FROM store_inventory_lines WHERE storeInventoryId = :inventoryId AND updated = 1")
    suspend fun getCountedLinesCount(inventoryId: Long): Int

    @Query("SELECT COUNT(*) FROM store_inventory_lines WHERE storeInventoryId = :inventoryId")
    suspend fun getTotalLinesCount(inventoryId: Long): Int
}
