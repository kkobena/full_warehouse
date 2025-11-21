package com.kobe.warehouse.inventory.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * JWT Token Response from backend
 * Matches the response from /api/auth/login endpoint
 */
data class JwtTokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String? = null,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("expiresIn")
    val expiresIn: Long
) {
    /**
     * Get expiration timestamp in milliseconds
     * Used to check if token is expired
     */
    fun getExpirationTimestamp(): Long {
        return System.currentTimeMillis() + (expiresIn * 1000)
    }
}
