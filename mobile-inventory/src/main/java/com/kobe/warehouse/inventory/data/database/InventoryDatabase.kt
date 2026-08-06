package com.kobe.warehouse.inventory.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kobe.warehouse.inventory.data.database.dao.InventoryDao
import com.kobe.warehouse.inventory.data.database.dao.InventoryLineDao
import com.kobe.warehouse.inventory.data.database.dao.InventoryLotLineDao
import com.kobe.warehouse.inventory.data.database.entity.InventoryEntity
import com.kobe.warehouse.inventory.data.database.entity.InventoryLineEntity
import com.kobe.warehouse.inventory.data.database.entity.InventoryLotLineEntity

/**
 * Room Database for offline inventory storage
 * Provides local caching and offline functionality
 */
@Database(
    entities = [
        InventoryEntity::class,
        InventoryLineEntity::class,
        InventoryLotLineEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao
    abstract fun inventoryLineDao(): InventoryLineDao
    abstract fun inventoryLotLineDao(): InventoryLotLineDao

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

                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
