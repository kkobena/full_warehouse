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
     * Get alerts list.
     */
    suspend fun getAlerts(types: List<String>? = null): Result<List<Alert>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAlerts(types)

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

    private fun handleException(e: Exception): Exception {
        return when (e) {
            is java.net.UnknownHostException -> Exception("Serveur inaccessible - vérifiez votre connexion")
            is java.net.SocketTimeoutException -> Exception("Délai d'attente dépassé")
            is java.net.ConnectException -> Exception("Impossible de se connecter au serveur")
            else -> Exception("Erreur: ${e.message}")
        }
    }
}
