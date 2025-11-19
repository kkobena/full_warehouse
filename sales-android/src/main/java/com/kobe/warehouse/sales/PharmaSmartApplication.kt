package com.kobe.warehouse.sales

import android.app.Application
import com.kobe.warehouse.sales.utils.ApiClient

/**
 * Pharma Smart Application
 * Application class for global initialization
 */
class PharmaSmartApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize ApiClient with application context
        // This is needed for SessionManager to broadcast events
        ApiClient.init(this)
    }
}
