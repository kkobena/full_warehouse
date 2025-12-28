package com.kobe.warehouse.reports

import android.app.Application
import com.kobe.warehouse.reports.data.api.ApiClient
import com.kobe.warehouse.reports.data.api.ReportApiService
import com.kobe.warehouse.reports.data.repository.ReportRepository
import com.kobe.warehouse.reports.utils.TokenManager

/**
 * Application class for Pharma Report mobile app.
 * Initializes global dependencies.
 */
class PharmaReportApplication : Application() {

    // Lazy-initialized dependencies
    val tokenManager: TokenManager by lazy { TokenManager(this) }

    val apiService: ReportApiService by lazy {
        ApiClient.create(tokenManager = tokenManager)
            .create(ReportApiService::class.java)
    }

    val repository: ReportRepository by lazy {
        ReportRepository(apiService, tokenManager)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize ApiClient with context for broadcasting events
        ApiClient.init(this)
    }

    companion object {
        lateinit var instance: PharmaReportApplication
            private set

        /**
         * Get TokenManager instance.
         */
        fun getTokenManager(): TokenManager {
            return instance.tokenManager
        }

        /**
         * Get ReportRepository instance.
         */
        fun getRepository(): ReportRepository {
            return instance.repository
        }

        /**
         * Get ReportApiService instance.
         */
        fun getApiService(): ReportApiService {
            return instance.apiService
        }
    }
}
