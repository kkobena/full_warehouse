package com.kobe.warehouse.inventory.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Body of POST /api/auth/refresh — matches backend RefreshTokenRequestDTO
 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)
