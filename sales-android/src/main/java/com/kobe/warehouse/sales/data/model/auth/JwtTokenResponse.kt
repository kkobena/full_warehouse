package com.kobe.warehouse.sales.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * JWT Token Response model
 * Received from POST /api/auth/login
 */
data class JwtTokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String? = null,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("expiresIn")
    val expiresIn: Long = 28800 // 8 hours in seconds
) {
    /**
     * Get token expiration timestamp in milliseconds
     */
    fun getExpirationTimestamp(): Long {
        return System.currentTimeMillis() + (expiresIn * 1000)
    }

    /**
     * Check if token is expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() >= getExpirationTimestamp()
    }
}
