package com.kobe.warehouse.inventory

import android.app.Application
import com.kobe.warehouse.inventory.sync.SyncManager
import com.kobe.warehouse.inventory.utils.ApiClient

/**
 * Pharma Smart Inventory Application
 * Application class for global initialization
 */
class PharmaSmartInventoryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize API client
        ApiClient.init(this)

        // Schedule periodic background sync
        SyncManager.schedulePeriodicSync(this)
    }
}
