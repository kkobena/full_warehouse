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

    @Query("SELECT * FROM store_inventory_lines WHERE storeInventoryId = :inventoryId AND rayonId = :rayonId")
    fun getInventoryLinesByRayon(inventoryId: Long, rayonId: Long): Flow<List<InventoryLineEntity>>

    @Query("SELECT * FROM store_inventory_lines WHERE id = :id")
    suspend fun getInventoryLineById(id: Long): InventoryLineEntity?

    @Query("SELECT * FROM store_inventory_lines WHERE storeInventoryId = :inventoryId AND (produitCip = :barcode OR produitEan = :barcode OR produitCipsJson LIKE '%' || :barcode || '%')")
    suspend fun findLineByBarcode(inventoryId: Long, barcode: String): InventoryLineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: InventoryLineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<InventoryLineEntity>)

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

    @Query("SELECT COUNT(*) FROM store_inventory_lines WHERE storeInventoryId = :inventoryId AND updated = 1")
    suspend fun getCountedLinesCount(inventoryId: Long): Int

    @Query("SELECT COUNT(*) FROM store_inventory_lines WHERE storeInventoryId = :inventoryId")
    suspend fun getTotalLinesCount(inventoryId: Long): Int
}
