package com.kobe.warehouse.reports.data.api

import com.kobe.warehouse.reports.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service for mobile report endpoints.
 * All endpoints require JWT Bearer token authentication.
 */
interface ReportApiService {

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    /**
     * Login to get JWT tokens.
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<JwtTokenResponse>

    /**
     * Get current user account info.
     */
    @GET("api/account")
    suspend fun getAccount(): Response<Account>

    /**
     * Refresh JWT token.
     */
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<JwtTokenResponse>

    // =========================================================================
    // DASHBOARD (Phase 1)
    // =========================================================================

    /**
     * Get complete dashboard data.
     * @param date Optional date (defaults to today on backend)
     */
    @GET("api/mobile/dashboard")
    suspend fun getDashboard(@Query("date") date: String? = null): Response<Dashboard>

    /**
     * Get CA trend for a date range.
     */
    @GET("api/mobile/ca-trend")
    suspend fun getCATrend(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<DailyCASummary>>

    // =========================================================================
    // ALERTS (Phase 1)
    // =========================================================================

    /**
     * Get detailed list of alerts.
     * @param types Optional list of alert types to filter
     */
    @GET("api/mobile/alerts")
    suspend fun getAlerts(@Query("types") types: List<String>? = null): Response<List<Alert>>

    // =========================================================================
    // PRODUCTS (Phase 1)
    // =========================================================================

    /**
     * Get quick product information.
     */
    @GET("api/mobile/products/{id}/quick-info")
    suspend fun getProductQuickInfo(@Path("id") productId: Long): Response<ProductQuickInfo>

    /**
     * Search products by name or code.
     */
    @GET("api/mobile/products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<List<ProductSearchResult>>

    // =========================================================================
    // TODOS (Phase 1)
    // =========================================================================

    /**
     * Get prioritized todo list.
     */
    @GET("api/mobile/todos")
    suspend fun getTodoList(): Response<TodoList>

    // =========================================================================
    // PERFORMANCE (Phase 2)
    // =========================================================================

    /**
     * Get performance analytics data.
     * @param period WEEK, MONTH, or YEAR
     * @param date Reference date (defaults to today on backend)
     */
    @GET("api/mobile/performance")
    suspend fun getPerformance(
        @Query("period") period: String = "WEEK",
        @Query("date") date: String? = null
    ): Response<Performance>

    // =========================================================================
    // FORECASTING (Phase 4)
    // =========================================================================

    /**
     * Get daily sales history for ML forecasting.
     */
    @GET("api/mobile/forecast/history")
    suspend fun getDailySalesHistory(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<DailySales>>

    /**
     * Get sales statistics for validation.
     */
    @GET("api/mobile/forecast/statistics")
    suspend fun getForecastStatistics(@Query("days") days: Int = 30): Response<SalesStatistics>

    // =========================================================================
    // CUSTOM REPORTS (Phase 4)
    // =========================================================================

    /**
     * Generate custom report with selected metrics.
     */
    @POST("api/mobile/custom-reports/generate")
    suspend fun generateCustomReport(@Body request: CustomReportGenerateRequest): Response<CustomReportMetricsResponse>

    /**
     * Get available metrics for custom reports.
     */
    @GET("api/mobile/custom-reports/available-metrics")
    suspend fun getAvailableMetrics(): Response<List<MetricInfo>>

    // =========================================================================
    // DEVICE MANAGEMENT (Phase 4)
    // =========================================================================

    /**
     * Register device for push notifications.
     */
    @POST("api/mobile/devices/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>

    /**
     * Update device notification settings.
     */
    @PUT("api/mobile/devices/settings")
    suspend fun updateDeviceSettings(@Body request: DeviceSettingsRequest): Response<DeviceSettingsResponse>

    /**
     * Unregister device.
     */
    @DELETE("api/mobile/devices")
    suspend fun unregisterDevice(@Query("fcmToken") fcmToken: String): Response<Unit>

    /**
     * Get device status.
     */
    @GET("api/mobile/devices/status")
    suspend fun getDeviceStatus(@Query("fcmToken") fcmToken: String): Response<DeviceStatusResponse>

    /**
     * Update device last active timestamp.
     */
    @PUT("api/mobile/devices/heartbeat")
    suspend fun deviceHeartbeat(@Query("fcmToken") fcmToken: String): Response<Unit>

    // =========================================================================
    // HEALTH CHECK
    // =========================================================================

    /**
     * Health check endpoint.
     */
    @GET("api/mobile/health")
    suspend fun healthCheck(): Response<HealthResponse>
}

// =========================================================================
// AUTH MODELS
// =========================================================================

/**
 * Login request.
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * JWT token response.
 */
data class JwtTokenResponse(
    @com.google.gson.annotations.SerializedName("accessToken")
    val accessToken: String,
    @com.google.gson.annotations.SerializedName("refreshToken")
    val refreshToken: String? = null,
    @com.google.gson.annotations.SerializedName("tokenType")
    val tokenType: String = "Bearer",
    @com.google.gson.annotations.SerializedName("expiresIn")
    val expiresIn: Long = 28800
)

/**
 * Refresh token request.
 */
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * User account info.
 */
data class Account(
    val id: Long,
    val login: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val authorities: List<String>
) {
    fun getFullName(): String {
        return "${firstName ?: ""} ${lastName ?: ""}".trim()
    }
}

/**
 * Health check response.
 */
data class HealthResponse(
    val status: String,
    val timestamp: Long
)

// =========================================================================
// PHASE 4 MODELS
// =========================================================================

/**
 * Custom report generate request.
 */
data class CustomReportGenerateRequest(
    val metricCodes: List<String>,
    val startDate: String,
    val endDate: String
)

/**
 * Custom report metrics response.
 */
data class CustomReportMetricsResponse(
    val metrics: Map<String, CustomReportMetric>
)

/**
 * Custom report metric data.
 */
data class CustomReportMetric(
    val metricCode: String,
    val metricName: String,
    val value: String,
    val trend: Double? = null,
    val chartData: List<ChartDataPoint>? = null,
    val details: String? = null
)

/**
 * Chart data point.
 */
data class ChartDataPoint(
    val label: String,
    val value: Double,
    val color: Int? = null
)

/**
 * Metric info.
 */
data class MetricInfo(
    val code: String,
    val displayName: String,
    val icon: String,
    val description: String
)

/**
 * Sales statistics for forecasting validation.
 */
data class SalesStatistics(
    val mean: Double,
    val stdDev: Double,
    val cv: Double,
    val min: Double,
    val max: Double,
    val suitability: String
)

/**
 * Device registration request.
 */
data class DeviceRegistrationRequest(
    val fcmToken: String,
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String
)

/**
 * Device registration response.
 */
data class DeviceRegistrationResponse(
    val deviceId: Long,
    val message: String,
    val notificationsEnabled: Boolean
)

/**
 * Device settings request.
 */
data class DeviceSettingsRequest(
    val fcmToken: String,
    val notificationsEnabled: Boolean
)

/**
 * Device settings response.
 */
data class DeviceSettingsResponse(
    val message: String,
    val notificationsEnabled: Boolean
)

/**
 * Device status response.
 */
data class DeviceStatusResponse(
    val isRegistered: Boolean,
    val deviceId: Long?,
    val notificationsEnabled: Boolean
)
