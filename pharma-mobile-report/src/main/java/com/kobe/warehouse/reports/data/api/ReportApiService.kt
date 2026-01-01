package com.kobe.warehouse.reports.data.api

import com.kobe.warehouse.reports.data.model.AbcPareto
import com.kobe.warehouse.reports.data.model.AbcParetoSummary
import com.kobe.warehouse.reports.data.model.ActivityReport
import com.kobe.warehouse.reports.data.model.Alert
import com.kobe.warehouse.reports.data.model.CashBalance
import com.kobe.warehouse.reports.data.model.CashSummary
import com.kobe.warehouse.reports.data.model.DailyCASummary
import com.kobe.warehouse.reports.data.model.DailySales
import com.kobe.warehouse.reports.data.model.Dashboard
import com.kobe.warehouse.reports.data.model.Performance
import com.kobe.warehouse.reports.data.model.PharmacistDashboard
import com.kobe.warehouse.reports.data.model.ProductProfitability
import com.kobe.warehouse.reports.data.model.ProductQuickInfo
import com.kobe.warehouse.reports.data.model.ProductSearchResult
import com.kobe.warehouse.reports.data.model.ProfitabilitySummary
import com.kobe.warehouse.reports.data.model.StockRotation
import com.kobe.warehouse.reports.data.model.StockValuation
import com.kobe.warehouse.reports.data.model.StockValuationSummary
import com.kobe.warehouse.reports.data.model.SupplierPerformance
import com.kobe.warehouse.reports.data.model.SupplierPerformanceSummary
import com.kobe.warehouse.reports.data.model.TiersPayantCreancesSummary
import com.kobe.warehouse.reports.data.model.TiersPayantInvoice
import com.kobe.warehouse.reports.data.model.TodoList
import com.kobe.warehouse.reports.data.model.TvaReport
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
    // STATISTICAL REPORTS (Phase 4)
    // =========================================================================

    // -------------------------------------------------------------------------
    // Créances Tiers Payant (Third-Party Payer Receivables)
    // -------------------------------------------------------------------------

    /**
     * Get créances summary grouped by tiers payant.
     */
    @GET("api/mobile/reports/tiers-payant/creances/summary")
    suspend fun getCreancesSummary(): Response<List<TiersPayantCreancesSummary>>

    /**
     * Get unpaid invoices with optional filters.
     * @param groupeId Optional groupe tiers payant ID filter
     * @param ageCategory Optional age category filter (LESS_THAN_30, BETWEEN_30_60, BETWEEN_60_90, MORE_THAN_90)
     */
    @GET("api/mobile/reports/tiers-payant/creances/unpaid")
    suspend fun getUnpaidInvoices(
        @Query("groupeId") groupeId: Int? = null,
        @Query("ageCategory") ageCategory: String? = null
    ): Response<List<TiersPayantInvoice>>

    /**
     * Get payment history for a specific groupe tiers payant.
     */
    @GET("api/mobile/reports/tiers-payant/payment-history")
    suspend fun getPaymentHistory(
        @Query("groupeId") groupeId: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<TiersPayantInvoice>>

    // -------------------------------------------------------------------------
    // Performance Fournisseurs (Supplier Performance)
    // -------------------------------------------------------------------------

    /**
     * Get all supplier performance data.
     */
    @GET("api/mobile/reports/supplier-performance/all")
    suspend fun getAllSupplierPerformance(): Response<List<SupplierPerformance>>

    /**
     * Get top suppliers by purchase volume.
     * @param limit Max number of suppliers (default 10)
     */
    @GET("api/mobile/reports/supplier-performance/top")
    suspend fun getTopSuppliers(
        @Query("limit") limit: Int = 10
    ): Response<List<SupplierPerformance>>

    /**
     * Get aggregated supplier performance summary.
     */
    @GET("api/mobile/reports/supplier-performance/summary")
    suspend fun getSupplierPerformanceSummary(): Response<SupplierPerformanceSummary>

    /**
     * Get suppliers filtered by minimum performance score.
     * @param minScore Minimum performance score (0-100)
     */
    @GET("api/mobile/reports/supplier-performance/by-score")
    suspend fun getSuppliersByScore(
        @Query("minScore") minScore: Double
    ): Response<List<SupplierPerformance>>

    /**
     * Get suppliers with delivery issues.
     */
    @GET("api/mobile/reports/supplier-performance/issues")
    suspend fun getSuppliersWithIssues(): Response<List<SupplierPerformance>>

    // -------------------------------------------------------------------------
    // Valorisation Stock (Stock Valuation)
    // -------------------------------------------------------------------------

    /**
     * Get all stock valuation data with pagination.
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    @GET("api/mobile/reports/stock-valuation/all")
    suspend fun getAllStockValuation(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<StockValuation>>

    /**
     * Get aggregated stock valuation summary.
     */
    @GET("api/mobile/reports/stock-valuation/summary")
    suspend fun getStockValuationSummary(): Response<StockValuationSummary>

    /**
     * Get stock valuation filtered by category.
     */
    @GET("api/mobile/reports/stock-valuation/by-category")
    suspend fun getStockValuationByCategory(
        @Query("category") category: String
    ): Response<List<StockValuation>>

    /**
     * Get stock valuation filtered by storage location.
     */
    @GET("api/mobile/reports/stock-valuation/by-storage")
    suspend fun getStockValuationByStorage(
        @Query("storage") storage: String
    ): Response<List<StockValuation>>

    // -------------------------------------------------------------------------
    // Rentabilité (Profitability)
    // -------------------------------------------------------------------------

    /**
     * Get all product profitability data.
     */
    @GET("api/mobile/reports/profitability/all")
    suspend fun getAllProductProfitability(): Response<List<ProductProfitability>>

    /**
     * Get aggregated profitability summary.
     */
    @GET("api/mobile/reports/profitability/summary")
    suspend fun getProfitabilitySummary(): Response<ProfitabilitySummary>

    /**
     * Get products filtered by BCG category.
     * @param category BCG category (STAR, CASH_COW, QUESTION_MARK, DOG)
     */
    @GET("api/mobile/reports/profitability/by-bcg")
    suspend fun getByBCGCategory(
        @Query("category") category: String
    ): Response<List<ProductProfitability>>

    /**
     * Get top N most profitable products.
     * @param limit Number of products to return (default 20)
     */
    @GET("api/mobile/reports/profitability/top")
    suspend fun getTopProfitableProducts(
        @Query("limit") limit: Int = 20
    ): Response<List<ProductProfitability>>

    /**
     * Get products with low margin (< 10%).
     */
    @GET("api/mobile/reports/profitability/low-margin")
    suspend fun getLowMarginProducts(): Response<List<ProductProfitability>>

    // -------------------------------------------------------------------------
    // Rotation Stock (Stock Rotation)
    // -------------------------------------------------------------------------

    /**
     * Get all stock rotation data with pagination.
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    @GET("api/mobile/reports/stock-rotation/all")
    suspend fun getAllStockRotation(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<StockRotation>>

    /**
     * Get slow moving products (ABC category C).
     */
    @GET("api/mobile/reports/stock-rotation/slow-moving")
    suspend fun getSlowMovingProducts(): Response<List<StockRotation>>

    /**
     * Get product counts by ABC classification.
     */
    @GET("api/mobile/reports/stock-rotation/abc-counts")
    suspend fun getStockRotationABCCounts(): Response<Map<String, Long>>

    /**
     * Get products filtered by ABC classification with pagination.
     * @param category ABC category (A, B, C)
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    @GET("api/mobile/reports/stock-rotation/by-abc")
    suspend fun getStockRotationByABC(
        @Query("category") category: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<StockRotation>>

    // -------------------------------------------------------------------------
    // ABC Pareto Analysis
    // -------------------------------------------------------------------------

    /**
     * Get all ABC Pareto analysis data with pagination.
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    @GET("api/mobile/reports/abc-pareto/all")
    suspend fun getAllABCPareto(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<AbcPareto>>

    /**
     * Get ABC Pareto summary.
     */
    @GET("api/mobile/reports/abc-pareto/summary")
    suspend fun getABCParetoSummary(): Response<AbcParetoSummary>

    /**
     * Get products filtered by Pareto class with pagination.
     * @param classePareto Pareto class (A, B, C)
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    @GET("api/mobile/reports/abc-pareto/by-class")
    suspend fun getByParetoClass(
        @Query("classePareto") classePareto: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<AbcPareto>>

    /**
     * Get top N revenue contributors.
     * @param limit Number of products to return (default 20)
     */
    @GET("api/mobile/reports/abc-pareto/top")
    suspend fun getTopRevenueContributors(
        @Query("limit") limit: Int = 20
    ): Response<List<AbcPareto>>

    // =========================================================================
    // FORECASTING (Phase 5 - Disabled until TensorFlow 2.16.0+)
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
    @com.google.gson.annotations.SerializedName("username")
    val username: String,
    @com.google.gson.annotations.SerializedName("password")
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
    @com.google.gson.annotations.SerializedName("refreshToken")
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

// =========================================================================
// PAGINATION MODELS
// =========================================================================

/**
 * Pagination information extracted from response headers.
 */
data class PaginationInfo(
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val pageSize: Int = 50,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
) {
    companion object {
        const val HEADER_TOTAL_COUNT = "X-Total-Count"
        const val HEADER_TOTAL_PAGES = "X-Total-Pages"
        const val HEADER_CURRENT_PAGE = "X-Current-Page"
        const val HEADER_PAGE_SIZE = "X-Page-Size"
        const val HEADER_HAS_NEXT = "X-Has-Next"
        const val HEADER_HAS_PREVIOUS = "X-Has-Previous"

        fun <T> fromResponse(response: Response<T>): PaginationInfo {
            val headers = response.headers()
            return PaginationInfo(
                totalCount = headers[HEADER_TOTAL_COUNT]?.toIntOrNull() ?: 0,
                totalPages = headers[HEADER_TOTAL_PAGES]?.toIntOrNull() ?: 0,
                currentPage = headers[HEADER_CURRENT_PAGE]?.toIntOrNull() ?: 0,
                pageSize = headers[HEADER_PAGE_SIZE]?.toIntOrNull() ?: 50,
                hasNext = headers[HEADER_HAS_NEXT]?.toBoolean() ?: false,
                hasPrevious = headers[HEADER_HAS_PREVIOUS]?.toBoolean() ?: false
            )
        }
    }
}

/**
 * Generic paginated response wrapper.
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val pagination: PaginationInfo
)
