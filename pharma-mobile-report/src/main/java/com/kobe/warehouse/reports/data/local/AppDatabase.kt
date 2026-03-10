package com.kobe.warehouse.reports.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kobe.warehouse.reports.data.local.dao.CachedReportDao
import com.kobe.warehouse.reports.data.local.dao.PendingActionDao
import com.kobe.warehouse.reports.data.local.entity.CachedReportEntity
import com.kobe.warehouse.reports.data.local.entity.PendingActionEntity

/**
 * Room database for offline caching.
 */
@Database(
    entities = [
        CachedReportEntity::class,
        PendingActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cachedReportDao(): CachedReportDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        private const val DATABASE_NAME = "pharma_reports.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
