package com.kobe.warehouse.sales

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.kobe.warehouse.sales.utils.ApiClient

/**
 * Pharma Smart Application
 * Application class for global initialization
 */
class PharmaSmartApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Force light mode — prevents "getPackageNightMode is not allowed" error
        // This app only uses Theme.Material3.Light, no dark mode support
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        ApiClient.init(this)
    }
}
