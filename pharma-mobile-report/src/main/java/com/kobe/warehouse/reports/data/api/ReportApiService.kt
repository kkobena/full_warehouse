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
