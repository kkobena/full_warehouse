package com.kobe.warehouse.inventory.data.repository

import com.kobe.warehouse.inventory.data.api.AuthApiService
import com.kobe.warehouse.inventory.data.model.auth.Account
import com.kobe.warehouse.inventory.data.model.auth.JwtTokenResponse
import com.kobe.warehouse.inventory.data.model.auth.LoginRequest
import com.kobe.warehouse.inventory.utils.ApiClient
import com.kobe.warehouse.inventory.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authentication Repository
 * Handles authentication operations
 */
class AuthRepository(private val tokenManager: TokenManager) {

    private val apiService: AuthApiService by lazy {
        ApiClient.create(tokenManager = tokenManager).create(AuthApiService::class.java)
    }

    /**
     * Login with username and password
     */
    suspend fun login(username: String, password: String): Result<JwtTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val loginRequest = LoginRequest(username, password)
                val response = apiService.login(loginRequest)

                if (response.isSuccessful && response.body() != null) {
                    val tokenResponse = response.body()!!
                    tokenManager.storeTokens(tokenResponse)
                    Result.success(tokenResponse)
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Identifiants incorrects"
                        403 -> "Accès refusé"
                        else -> "Erreur de connexion: ${response.code()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get current user account
     */
    suspend fun getAccount(): Result<Account> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAccount()

                if (response.isSuccessful && response.body() != null) {
                    val account = response.body()!!
                    tokenManager.storeAuthorities(account.authorities)
                    Result.success(account)
                } else {
                    Result.failure(Exception("Failed to get account: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Logout
     */
    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.logout()
                tokenManager.clearAll()
                Result.success(Unit)
            } catch (e: Exception) {
                // Clear tokens even if logout fails
                tokenManager.clearAll()
                Result.success(Unit)
            }
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return tokenManager.isAuthenticated()
    }
}
