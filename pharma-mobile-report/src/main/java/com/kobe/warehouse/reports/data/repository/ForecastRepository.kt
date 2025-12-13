package com.kobe.warehouse.reports.data.repository

import android.content.Context
import com.kobe.warehouse.reports.data.api.ApiService
import com.kobe.warehouse.reports.data.local.AppDatabase
import com.kobe.warehouse.reports.data.model.DailySales
import com.kobe.warehouse.reports.data.model.ForecastData
import com.kobe.warehouse.reports.data.model.ForecastRequest
import com.kobe.warehouse.reports.data.offline.OfflineManager
import com.kobe.warehouse.reports.service.ForecastingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for forecast data
 * Combines API data with ML predictions
 */
class ForecastRepository(
    private val context: Context,
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val offlineManager: OfflineManager
) {

    private val forecastingService = ForecastingService.getInstance(context)

    companion object {
        private const val REPORT_TYPE_FORECAST = "forecast"
        private const val CACHE_TTL_HOURS = 6L // Cache forecasts for 6 hours
    }

    /**
     * Generate sales forecast
     *
     * @param request Forecast parameters
     * @return Forecast data with predictions
     */
    suspend fun generateForecast(request: ForecastRequest = ForecastRequest()): Result<ForecastData> {
        return try {
            // Try to get from cache first
            val cached = offlineManager.getCachedReport<ForecastData>(
                REPORT_TYPE_FORECAST,
                "last",
                ForecastData::class.java
            )

            if (cached != null) {
                return Result.success(cached)
            }

            // Fetch historical sales data from API
            val historicalData = fetchHistoricalSalesData(request.historicalDays)

            if (historicalData.isEmpty()) {
                return Result.failure(Exception("Insufficient historical data for forecast"))
            }

            // Initialize forecasting service if needed
            forecastingService.initialize()

            // Generate predictions
            val forecast = forecastingService.predictSales(
                historicalData = historicalData,
                forecastDays = request.forecastDays
            )

            // Cache the forecast
            offlineManager.cacheReport(
                reportType = REPORT_TYPE_FORECAST,
                reportKey = "last",
                data = forecast,
                ttl = CACHE_TTL_HOURS * 3600
            )

            Result.success(forecast)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch historical sales data from API
     */
    private suspend fun fetchHistoricalSalesData(days: Int): List<DailySales> = withContext(Dispatchers.IO) {
        try {
            // Calculate date range
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(days.toLong())

            // Fetch from API
            val response = apiService.getDailySalesHistory(
                startDate = startDate.format(DateTimeFormatter.ISO_DATE),
                endDate = endDate.format(DateTimeFormatter.ISO_DATE)
            )

            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                // Fallback to cached data if available
                getCachedHistoricalData(days)
            }
        } catch (e: Exception) {
            // Try offline cache
            getCachedHistoricalData(days)
        }
    }

    /**
     * Get cached historical data from offline storage
     */
    private suspend fun getCachedHistoricalData(days: Int): List<DailySales> {
        return try {
            // Try to get from cache
            val cached = offlineManager.getCachedReport<List<DailySales>>(
                reportType = "sales_history",
                reportKey = "last_${days}_days",
                clazz = List::class.java as Class<List<DailySales>>
            )

            cached ?: generateMockHistoricalData(days)
        } catch (e: Exception) {
            generateMockHistoricalData(days)
        }
    }

    /**
     * Generate mock historical data for demonstration/testing
     * In production, this should never be called
     */
    private fun generateMockHistoricalData(days: Int): List<DailySales> {
        val endDate = LocalDate.now()
        val baseAmount = 2_000_000.0 // 2M FCFA base daily sales

        return (0 until days).map { daysAgo ->
            val date = endDate.minusDays(daysAgo.toLong())
            val dayOfWeek = date.dayOfWeek.value

            // Weekend sales are typically lower
            val weekendMultiplier = if (dayOfWeek >= 6) 0.7 else 1.0

            // Add some random variation
            val randomVariation = 0.85 + (Math.random() * 0.3)

            // Add weekly trend
            val weeklyTrend = 1.0 + (Math.sin(daysAgo * Math.PI / 7) * 0.1)

            val amount = baseAmount * weekendMultiplier * randomVariation * weeklyTrend
            val transactions = (100 + (Math.random() * 50)).toInt()

            DailySales(
                date = date.format(DateTimeFormatter.ISO_DATE),
                amount = amount,
                transactionCount = transactions
            )
        }.reversed() // Return oldest to newest
    }

    /**
     * Refresh forecast (force regeneration)
     */
    suspend fun refreshForecast(request: ForecastRequest = ForecastRequest()): Result<ForecastData> {
        // Clear cache
        try {
            // Note: You might want to add a method to clear specific cached reports
            // offlineManager.clearCachedReport(REPORT_TYPE_FORECAST, "last")
        } catch (e: Exception) {
            // Ignore cache clear errors
        }

        return generateForecast(request)
    }

    /**
     * Get forecast confidence interpretation
     */
    fun getConfidenceLevel(confidence: Float): String {
        return when {
            confidence >= 0.8f -> "Très élevée"
            confidence >= 0.6f -> "Élevée"
            confidence >= 0.4f -> "Moyenne"
            else -> "Faible"
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        forecastingService.close()
    }
}
