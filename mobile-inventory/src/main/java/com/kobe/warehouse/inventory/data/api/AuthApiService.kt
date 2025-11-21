package com.kobe.warehouse.inventory.data.api

import com.kobe.warehouse.inventory.data.model.auth.Account
import com.kobe.warehouse.inventory.data.model.auth.JwtTokenResponse
import com.kobe.warehouse.inventory.data.model.auth.LoginRequest
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
        @Body refreshToken: String
    ): Response<JwtTokenResponse>

    @POST("api/logout")
    suspend fun logout(): Response<Void>
}
