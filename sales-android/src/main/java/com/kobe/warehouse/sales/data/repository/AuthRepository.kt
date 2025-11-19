package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.AuthApiService
import com.kobe.warehouse.sales.data.model.auth.Account
import com.kobe.warehouse.sales.data.model.auth.JwtTokenResponse
import com.kobe.warehouse.sales.data.model.auth.LoginRequest
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authentication Repository
 * Handles authentication operations following the same logic as web's AuthServerProvider
 */
class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Login with username and password
     * Same logic as web's AuthServerProvider.login()
     *
     * 1. POST to /api/auth/login
     * 2. Store JWT tokens
     * 3. Fetch user account details
     * 4. Return account
     *
     * @param username Username
     * @param password Password
     * @param rememberMe Remember me flag
     * @return Result with Account or error
     */
    suspend fun login(
        username: String,
        password: String,
        rememberMe: Boolean
    ): Result<Account> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Login and get JWT tokens
            val loginRequest = LoginRequest(username, password)
            val loginResponse = apiService.login(loginRequest)

            if (!loginResponse.isSuccessful || loginResponse.body() == null) {
                return@withContext Result.failure(
                    Exception("Erreur de connexion: ${loginResponse.code()} - ${loginResponse.message()}")
                )
            }

            val tokenResponse = loginResponse.body()!!

            // Step 2: Store JWT tokens
            tokenManager.storeTokens(tokenResponse)

            // Step 3: Store remember me preference
            tokenManager.storeRememberMe(username, password, rememberMe)

            // Step 4: Fetch user account details
            val accountResult = getAccount()

            if (accountResult.isSuccess) {
                val account = accountResult.getOrThrow()
                // Step 5: Store user authorities for permission checks
                tokenManager.storeAuthorities(account.authorities)
                Result.success(account)
            } else {
                // Clear tokens if account fetch fails
                tokenManager.clearTokens()
                Result.failure(
                    accountResult.exceptionOrNull()
                        ?: Exception("Erreur lors de la récupération du compte utilisateur")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current user account details
     * Requires valid JWT token in Authorization header
     *
     * @return Result with Account or error
     */
    suspend fun getAccount(): Result<Account> = withContext(Dispatchers.IO) {
        try {
            if (!tokenManager.isAuthenticated()) {
                return@withContext Result.failure(Exception("Non authentifié"))
            }

            val response = apiService.getAccount()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Erreur: ${response.code()} - ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout user
     * Clears stored tokens (JWT is stateless, so no server call needed)
     * Same logic as web's AuthServerProvider.logout()
     */
    suspend fun logout(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Clear JWT tokens from storage
            tokenManager.clearTokens()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh access token using refresh token
     *
     * @return Result with new token response or error
     */
    suspend fun refreshToken(): Result<JwtTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return@withContext Result.failure(Exception("Aucun refresh token disponible"))

            val response = apiService.refreshToken(refreshToken)

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                tokenManager.storeTokens(tokenResponse)
                Result.success(tokenResponse)
            } else {
                Result.failure(
                    Exception("Erreur: ${response.code()} - ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return tokenManager.isAuthenticated()
    }

    /**
     * Get saved credentials for auto-login (if remember me was checked)
     */
    fun getSavedCredentials(): Pair<String, String>? {
        if (!tokenManager.getRememberMe()) {
            return null
        }

        val username = tokenManager.getSavedUsername()
        val password = tokenManager.getSavedPassword()

        return if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            Pair(username, password)
        } else null
    }
}
