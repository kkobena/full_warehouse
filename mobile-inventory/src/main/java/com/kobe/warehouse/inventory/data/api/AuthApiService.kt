package com.kobe.warehouse.inventory.data.api

import com.kobe.warehouse.inventory.data.model.auth.Account
import com.kobe.warehouse.inventory.data.model.auth.JwtTokenResponse
import com.kobe.warehouse.inventory.data.model.auth.LoginRequest
import com.kobe.warehouse.inventory.data.model.auth.RefreshTokenRequest
import retrofit2.Response
import retrofit2.http.*

/**
 * Authentication API service
 * Handles login and account operations
 */
interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<JwtTokenResponse>

    @GET("api/account")
    suspend fun getAccount(): Response<Account>

    @POST("api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<JwtTokenResponse>

    /**
     * Variante synchrone : appelée depuis l'Authenticator OkHttp, qui s'exécute
     * hors coroutine sur le thread du dispatcher.
     */
    @POST("api/auth/refresh")
    fun refreshTokenSync(
        @Body request: RefreshTokenRequest
    ): retrofit2.Call<JwtTokenResponse>

    @POST("api/logout")
    suspend fun logout(): Response<Void>
}
