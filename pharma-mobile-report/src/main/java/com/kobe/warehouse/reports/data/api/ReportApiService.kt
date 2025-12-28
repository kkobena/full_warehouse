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
     * Get detailed list of alerts with pagination.
     * @param types Optional list of alert types to filter
     * @param page Page number (0-indexed)
     * @param size Page size
     */
    @GET("api/mobile/alerts")
    suspend fun getAlerts(
        @Query("types") types: List<String>? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<List<Alert>>

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

    /**
     * Get all todo items as a flat list with pagination.
     * @param page Page number (0-indexed)
     * @param size Page size
     */
    @GET("api/mobile/todos/items")
    suspend fun getTodoItems(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<List<com.kobe.warehouse.reports.data.model.TodoItem>>

    /**
     * Get todo counts by priority.
     */
    @GET("api/mobile/todos/counts")
    suspend fun getTodoCounts(): Response<TodoCounts>

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
    // PHARMACIST REPORTS (Phase 3)
    // =========================================================================

    /**
     * Get pharmacist dashboard (Tableau Pharmacien) data.
     * Provides comprehensive sales vs purchases analysis.
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd)
     */
    @GET("api/mobile/reports/pharmacist-dashboard")
    suspend fun getPharmacistDashboard(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String? = null
    ): Response<PharmacistDashboard>

    /**
     * Get cash summary (Ticket Z / Récapitulatif Caisse) data.
     * Provides comprehensive cash summary with per-cashier breakdown.
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd)
     * @param fromTime Start time for intra-day filtering (format: HH:mm:ss)
     * @param toTime End time for intra-day filtering (format: HH:mm:ss)
     * @param userIds Filter by specific user IDs
     * @param onlyVente If true, only include sales payments
     */
    @GET("api/mobile/reports/cash-summary")
    suspend fun getCashSummary(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String? = null,
        @Query("fromTime") fromTime: String? = null,
        @Query("toTime") toTime: String? = null,
        @Query("userIds") userIds: List<Int>? = null,
        @Query("onlyVente") onlyVente: Boolean = false
    ): Response<CashSummary>

    /**
     * Get activity report (Rapport d'Activité) data.
     * Provides comprehensive activity summary with:
     * - Chiffre d'affaires (revenue summary)
     * - Recettes by payment mode
     * - Mouvements de caisse
     * - Achats by supplier group
     * - Tiers payants summary
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd)
     */
    @GET("api/mobile/reports/activity")
    suspend fun getActivityReport(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String? = null
    ): Response<ActivityReport>

    /**
     * Get cash balance (Balance Caisse) data.
     * Provides comprehensive cash balance with:
     * - Totals (TTC, HT, Net, Remise, TVA)
     * - Payment breakdown (cash, cards, mobile money, etc.)
     * - Category balances (VO, VNO)
     * - Cash movements (entries/exits)
     * - Margin and ratio analysis
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd)
     */
    @GET("api/mobile/reports/cash-balance")
    suspend fun getCashBalance(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String? = null
    ): Response<CashBalance>

    /**
     * Get TVA (VAT) report data.
     * Provides comprehensive TVA breakdown with:
     * - Totals (HT, TVA, TTC, Net)
     * - Breakdown by TVA rate
     * - Chart data for visualization
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd)
     * @param groupByDate Whether to group results by date
     */
    @GET("api/mobile/reports/tva")
    suspend fun getTvaReport(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String? = null,
        @Query("groupByDate") groupByDate: Boolean = false
    ): Response<TvaReport>

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
    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Resolve/dismiss an alert.
     */
    @PUT("api/mobile/alerts/{id}/resolve")
    suspend fun resolveAlert(@Path("id") alertId: Long): retrofit2.Response<Unit>

    /**
     * Mark a todo item as done.
     */
    @PUT("api/mobile/todos/{id}/done")
    suspend fun markTodoDone(@Path("id") todoId: Long): retrofit2.Response<Unit>

    /**
     * Register FCM token.
     */
    @POST("api/mobile/devices/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): retrofit2.Response<Unit>

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
    val chartData: List<ApiChartDataPoint>? = null,
    val details: String? = null
)

/**
 * Chart data point for custom reports.
 */
data class ApiChartDataPoint(
    val label: String,
    val value: Double,
    val color: String? = null
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

/**
 * Todo counts by priority.
 */
data class TodoCounts(
    val urgent: Int,
    val important: Int,
    val normal: Int
) {
    val total: Int get() = urgent + important + normal
}

// Extension methods for missing endpoints

/**
 * FCM token registration request.
 */
data class FcmTokenRequest(
    val fcmToken: String
)
