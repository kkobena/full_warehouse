package com.kobe.warehouse.reports.data.repository

import com.kobe.warehouse.reports.data.api.*
import com.kobe.warehouse.reports.data.model.*
import com.kobe.warehouse.reports.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for report data access.
 * Handles API calls with proper error handling.
 */
/**
 * Companion object providing singleton access.
 */
@Suppress("unused")
private var instance: ReportRepository? = null

class ReportRepository(
    private val apiService: ReportApiService,
    private val tokenManager: TokenManager
) {
    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    /**
     * Login and store tokens.
     */
    suspend fun login(
        username: String,
        password: String,
        rememberMe: Boolean
    ): Result<Account> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Login
            val loginResponse = apiService.login(LoginRequest(username, password))

            if (!loginResponse.isSuccessful || loginResponse.body() == null) {
                return@withContext Result.failure(
                    Exception(getErrorMessage(loginResponse.code()))
                )
            }

            // Step 2: Store tokens
            val tokenResponse = loginResponse.body()!!
            tokenManager.storeTokens(
                tokenResponse.accessToken,
                tokenResponse.refreshToken,
                tokenResponse.expiresIn
            )

            // Step 3: Store remember me
            tokenManager.storeRememberMe(username, password, rememberMe)

            // Step 4: Get account info
            val accountResult = getAccount()

            if (accountResult.isSuccess) {
                val account = accountResult.getOrThrow()
                tokenManager.storeAuthorities(account.authorities)
                Result.success(account)
            } else {
                tokenManager.clearTokens()
                accountResult
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get current user account.
     */
    suspend fun getAccount(): Result<Account> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAccount()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Logout - clear tokens.
     */
    fun logout() {
        tokenManager.clearTokens()
    }

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    /**
     * Get dashboard data.
     */
    suspend fun getDashboard(date: String? = null): Result<Dashboard> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDashboard(date)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get CA trend.
     */
    suspend fun getCATrend(
        startDate: String,
        endDate: String
    ): Result<List<DailyCASummary>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCATrend(startDate, endDate)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // ALERTS
    // =========================================================================

    /**
     * Get alerts list with optional pagination.
     */
    suspend fun getAlerts(
        types: List<String>? = null,
        page: Int = 0,
        size: Int = 20
    ): Result<List<Alert>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAlerts(types, page, size)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // PRODUCTS
    // =========================================================================

    /**
     * Get product quick info.
     */
    suspend fun getProductQuickInfo(productId: Long): Result<ProductQuickInfo> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProductQuickInfo(productId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Search products.
     */
    suspend fun searchProducts(
        query: String,
        limit: Int = 20
    ): Result<List<ProductSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchProducts(query, limit)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // TODOS
    // =========================================================================

    /**
     * Get todo list.
     */
    suspend fun getTodos(): Result<TodoList> = getTodoList()

    suspend fun getTodoList(): Result<TodoList> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTodoList()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get todo items with pagination.
     */
    suspend fun getTodoItems(
        page: Int = 0,
        size: Int = 20
    ): Result<List<com.kobe.warehouse.reports.data.model.TodoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTodoItems(page, size)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get todo counts by priority.
     */
    suspend fun getTodoCounts(): Result<com.kobe.warehouse.reports.data.api.TodoCounts> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTodoCounts()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // PERFORMANCE
    // =========================================================================

    /**
     * Get performance data.
     */
    suspend fun getPerformance(
        period: String = "WEEK",
        date: String? = null
    ): Result<Performance> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPerformance(period, date)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // PHARMACIST REPORTS
    // =========================================================================

    /**
     * Get pharmacist dashboard (Tableau Pharmacien) data.
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd), defaults to fromDate if null
     */
    suspend fun getPharmacistDashboard(
        fromDate: String,
        toDate: String? = null
    ): Result<PharmacistDashboard> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPharmacistDashboard(fromDate, toDate)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get cash summary (Ticket Z / Récapitulatif Caisse) data.
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd), defaults to fromDate if null
     * @param fromTime Start time for intra-day filtering (format: HH:mm:ss)
     * @param toTime End time for intra-day filtering (format: HH:mm:ss)
     * @param userIds Filter by specific user IDs
     * @param onlyVente If true, only include sales payments
     */
    suspend fun getCashSummary(
        fromDate: String,
        toDate: String? = null,
        fromTime: String? = null,
        toTime: String? = null,
        userIds: List<Int>? = null,
        onlyVente: Boolean = false
    ): Result<CashSummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCashSummary(fromDate, toDate, fromTime, toTime, userIds, onlyVente)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get activity report (Rapport d'Activité) data.
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd), defaults to fromDate if null
     */
    suspend fun getActivityReport(
        fromDate: String,
        toDate: String? = null
    ): Result<ActivityReport> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getActivityReport(fromDate, toDate)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get cash balance (Balance Caisse) data.
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd), defaults to fromDate if null
     */
    suspend fun getCashBalance(
        fromDate: String,
        toDate: String? = null
    ): Result<CashBalance> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCashBalance(fromDate, toDate)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get TVA report data.
     *
     * @param fromDate Start date of the period (format: yyyy-MM-dd)
     * @param toDate End date of the period (format: yyyy-MM-dd), defaults to fromDate if null
     * @param groupByDate Whether to group results by date
     */
    suspend fun getTvaReport(
        fromDate: String,
        toDate: String? = null,
        groupByDate: Boolean = false
    ): Result<TvaReport> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTvaReport(fromDate, toDate, groupByDate)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // STATISTICAL REPORTS (Phase 4)
    // =========================================================================

    // -------------------------------------------------------------------------
    // Créances Tiers Payant (Third-Party Payer Receivables)
    // -------------------------------------------------------------------------

    /**
     * Get créances summary grouped by tiers payant.
     */
    suspend fun getCreancesSummary(): Result<List<TiersPayantCreancesSummary>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCreancesSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get unpaid invoices with optional filters.
     */
    suspend fun getUnpaidInvoices(
        groupeId: Int? = null,
        ageCategory: String? = null
    ): Result<List<TiersPayantInvoice>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUnpaidInvoices(groupeId, ageCategory)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // -------------------------------------------------------------------------
    // Performance Fournisseurs (Supplier Performance)
    // -------------------------------------------------------------------------

    /**
     * Get all supplier performance data.
     */
    suspend fun getAllSupplierPerformance(): Result<List<SupplierPerformance>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllSupplierPerformance()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get supplier performance summary.
     */
    suspend fun getSupplierPerformanceSummary(): Result<SupplierPerformanceSummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSupplierPerformanceSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // -------------------------------------------------------------------------
    // Valorisation Stock (Stock Valuation)
    // -------------------------------------------------------------------------

    /**
     * Get all stock valuation data with pagination.
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    suspend fun getAllStockValuation(
        page: Int = 0,
        size: Int = 50
    ): Result<PaginatedResult<StockValuation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllStockValuation(page, size)
            if (response.isSuccessful && response.body() != null) {
                val pagination = PaginationInfo.fromResponse(response)
                Result.success(PaginatedResult(response.body()!!, pagination))
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get stock valuation summary.
     */
    suspend fun getStockValuationSummary(): Result<StockValuationSummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStockValuationSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // -------------------------------------------------------------------------
    // Rentabilité (Profitability)
    // -------------------------------------------------------------------------

    /**
     * Get all product profitability data.
     */
    suspend fun getAllProductProfitability(): Result<List<ProductProfitability>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllProductProfitability()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get profitability summary.
     */
    suspend fun getProfitabilitySummary(): Result<ProfitabilitySummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProfitabilitySummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get products by BCG category.
     */
    suspend fun getByBCGCategory(category: String): Result<List<ProductProfitability>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getByBCGCategory(category)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // -------------------------------------------------------------------------
    // Rotation Stock (Stock Rotation)
    // -------------------------------------------------------------------------

    /**
     * Get all stock rotation data with pagination.
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    suspend fun getAllStockRotation(
        page: Int = 0,
        size: Int = 50
    ): Result<PaginatedResult<StockRotation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllStockRotation(page, size)
            if (response.isSuccessful && response.body() != null) {
                val pagination = PaginationInfo.fromResponse(response)
                Result.success(PaginatedResult(response.body()!!, pagination))
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get slow moving products.
     */
    suspend fun getSlowMovingProducts(): Result<List<StockRotation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSlowMovingProducts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get stock rotation by ABC category with pagination.
     * @param category ABC category (A, B, C)
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    suspend fun getStockRotationByABC(
        category: String,
        page: Int = 0,
        size: Int = 50
    ): Result<PaginatedResult<StockRotation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStockRotationByABC(category, page, size)
            if (response.isSuccessful && response.body() != null) {
                val pagination = PaginationInfo.fromResponse(response)
                Result.success(PaginatedResult(response.body()!!, pagination))
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get stock rotation ABC counts.
     */
    suspend fun getStockRotationABCCounts(): Result<Map<String, Long>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStockRotationABCCounts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // -------------------------------------------------------------------------
    // ABC Pareto Analysis
    // -------------------------------------------------------------------------

    /**
     * Get all ABC Pareto data with pagination.
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    suspend fun getAllABCPareto(
        page: Int = 0,
        size: Int = 50
    ): Result<PaginatedResult<AbcPareto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllABCPareto(page, size)
            if (response.isSuccessful && response.body() != null) {
                val pagination = PaginationInfo.fromResponse(response)
                Result.success(PaginatedResult(response.body()!!, pagination))
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get ABC Pareto summary.
     */
    suspend fun getABCParetoSummary(): Result<AbcParetoSummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getABCParetoSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Get products by Pareto class with pagination.
     * @param classePareto Pareto class (A, B, C)
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     */
    suspend fun getByParetoClass(
        classePareto: String,
        page: Int = 0,
        size: Int = 50
    ): Result<PaginatedResult<AbcPareto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getByParetoClass(classePareto, page, size)
            if (response.isSuccessful && response.body() != null) {
                val pagination = PaginationInfo.fromResponse(response)
                Result.success(PaginatedResult(response.body()!!, pagination))
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // HEALTH CHECK
    // =========================================================================

    /**
     * Check server health.
     */
    suspend fun healthCheck(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Test connection to a server URL.
     */
    suspend fun testConnection(serverUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Create a temporary OkHttp client to test the connection
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url("${serverUrl.trimEnd('/')}/api/mobile/health")
                .get()
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    // =========================================================================
    // ERROR HANDLING
    // =========================================================================

    private fun getErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Requête invalide"
            401 -> "Non autorisé - veuillez vous reconnecter"
            403 -> "Accès refusé"
            404 -> "Ressource non trouvée"
            500 -> "Erreur serveur interne"
            502 -> "Serveur indisponible"
            503 -> "Service temporairement indisponible"
            else -> "Erreur inattendue (code: $code)"
        }
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Resolve/dismiss an alert.
     */
    suspend fun resolveAlert(alertId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resolveAlert(alertId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Mark a todo item as done.
     */
    suspend fun markTodoDone(todoId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.markTodoDone(todoId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Register FCM token.
     */
    suspend fun registerFcmToken(fcmToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.registerFcmToken(FcmTokenRequest(fcmToken))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(getErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    private fun handleException(e: Exception): Exception {
        return when (e) {
            is java.net.UnknownHostException -> Exception("Serveur inaccessible - vérifiez votre connexion")
            is java.net.SocketTimeoutException -> Exception("Délai d'attente dépassé")
            is java.net.ConnectException -> Exception("Impossible de se connecter au serveur")
            else -> Exception("Erreur: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ReportRepository? = null

        fun getInstance(context: android.content.Context): ReportRepository {
            return INSTANCE ?: synchronized(this) {
                val tokenManager = com.kobe.warehouse.reports.utils.TokenManager(context)
                val apiService = ApiClient.create(tokenManager = tokenManager)
                    .create(ReportApiService::class.java)
                val instance = ReportRepository(apiService, tokenManager)
                INSTANCE = instance
                instance
            }
        }
    }

}
