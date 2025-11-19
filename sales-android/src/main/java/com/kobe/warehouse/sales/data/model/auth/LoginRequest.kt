package com.kobe.warehouse.sales.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Login request model
 * Sent to POST /api/auth/login
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)
