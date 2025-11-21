package com.kobe.warehouse.inventory.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kobe.warehouse.inventory.data.database.dao.InventoryDao
import com.kobe.warehouse.inventory.data.database.dao.InventoryLineDao
import com.kobe.warehouse.inventory.data.database.entity.InventoryEntity
import com.kobe.warehouse.inventory.data.database.entity.InventoryLineEntity

/**
 * Room Database for offline inventory storage
 * Provides local caching and offline functionality
 */
@Database(
    entities = [InventoryEntity::class, InventoryLineEntity::class],
    version = 1,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao
    abstract fun inventoryLineDao(): InventoryLineDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getInstance(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "pharma_smart_inventory_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
