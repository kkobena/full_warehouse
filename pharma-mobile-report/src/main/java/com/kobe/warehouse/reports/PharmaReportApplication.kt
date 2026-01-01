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

    // Mutable apiService that can be recreated when URL changes
    @Volatile
    private var _apiService: ReportApiService? = null

    val apiService: ReportApiService
        get() {
            return _apiService ?: synchronized(this) {
                _apiService ?: createApiService().also { _apiService = it }
            }
        }

    // Mutable repository that depends on apiService
    @Volatile
    private var _repository: ReportRepository? = null

    val repository: ReportRepository
        get() {
            return _repository ?: synchronized(this) {
                _repository ?: ReportRepository(apiService, tokenManager).also { _repository = it }
            }
        }

    private fun createApiService(): ReportApiService {
        return ApiClient.create(tokenManager = tokenManager)
            .create(ReportApiService::class.java)
    }

    /**
     * Reset API service and repository to use new base URL.
     * Call this after changing the server URL in settings.
     */
    fun resetApiService() {
        synchronized(this) {
            _apiService = null
            _repository = null
        }
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

        /**
         * Reset API service to use new URL configuration.
         */
        fun resetApiService() {
            instance.resetApiService()
        }
    }
}
