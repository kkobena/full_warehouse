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

    @Query("SELECT * FROM store_inventories WHERE statut IN ('CREATE', 'PROCESSING') ORDER BY updatedAt DESC")
    fun getActiveInventories(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM store_inventories WHERE statut IN ('CREATE', 'PROCESSING') ORDER BY updatedAt DESC")
    suspend fun getActiveInventoriesOnce(): List<InventoryEntity>

    @Query("SELECT * FROM store_inventories WHERE id = :id")
    suspend fun getInventoryById(id: Long): InventoryEntity?

    @Query("SELECT * FROM store_inventories WHERE id = :id")
    fun getInventoryByIdFlow(id: Long): Flow<InventoryEntity?>

    // Upsert (pas REPLACE) : un REPLACE supprime puis réinsère la ligne parente,
    // ce qui déclencherait la suppression en cascade des lignes d'inventaire locales
    @Upsert
    suspend fun upsertInventory(inventory: InventoryEntity)

    @Upsert
    suspend fun upsertInventories(inventories: List<InventoryEntity>)

    @Update
    suspend fun updateInventory(inventory: InventoryEntity)

    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)

    @Query("DELETE FROM store_inventories")
    suspend fun deleteAllInventories()

    @Query("SELECT * FROM store_inventories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncInventories(): List<InventoryEntity>
}
