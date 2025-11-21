package com.kobe.warehouse.inventory.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Login Request
 * Request body for /api/auth/login endpoint
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)
