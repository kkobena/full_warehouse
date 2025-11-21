package com.kobe.warehouse.inventory.data.database.dao

import androidx.room.*
import com.kobe.warehouse.inventory.data.database.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for inventory operations
 * Provides offline access to inventory data
 */
@Dao
interface InventoryDao {

    @Query("SELECT * FROM store_inventories WHERE statut = 'OPEN' ORDER BY updatedAt DESC")
    fun getActiveInventories(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM store_inventories WHERE id = :id")
    suspend fun getInventoryById(id: Long): InventoryEntity?

    @Query("SELECT * FROM store_inventories WHERE id = :id")
    fun getInventoryByIdFlow(id: Long): Flow<InventoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: InventoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventories(inventories: List<InventoryEntity>)

    @Update
    suspend fun updateInventory(inventory: InventoryEntity)

    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)

    @Query("DELETE FROM store_inventories")
    suspend fun deleteAllInventories()

    @Query("SELECT * FROM store_inventories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncInventories(): List<InventoryEntity>
}
