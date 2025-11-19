package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.auth.Account
import com.kobe.warehouse.sales.data.model.auth.JwtTokenResponse
import com.kobe.warehouse.sales.data.model.auth.LoginRequest
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for Authentication endpoints
 * Following the same logic as the web application
 */
interface AuthApiService {

    /**
     * Login with username and password
     * POST /api/auth/login
     *
     * Request body: { username, password }
     * Response: { accessToken, refreshToken, tokenType, expiresIn }
     *
     * @param loginRequest Login credentials
     * @return JWT token response
     */
    @POST("api/auth/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<JwtTokenResponse>

    /**
     * Get current user account details
     * GET /api/account
     *
     * Requires: Authorization: Bearer {accessToken}
     *
     * @return User account details
     */
    @GET("api/account")
    suspend fun getAccount(): Response<Account>

    /**
     * Refresh access token using refresh token
     * POST /api/auth/refresh
     *
     * Request body: refreshToken
     * Response: { accessToken, refreshToken, tokenType, expiresIn }
     *
     * @param refreshToken Refresh token
     * @return New JWT token response
     */
    @POST("api/auth/refresh")
    suspend fun refreshToken(
        @Body refreshToken: String
    ): Response<JwtTokenResponse>

    /**
     * Logout (optional - JWT is stateless)
     * POST /api/logout
     *
     * Note: Since JWT is stateless, logout is typically handled client-side
     * by clearing stored tokens
     */
    @POST("api/logout")
    suspend fun logout(): Response<Void>
}
