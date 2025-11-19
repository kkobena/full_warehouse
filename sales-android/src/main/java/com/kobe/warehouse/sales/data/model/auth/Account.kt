package com.kobe.warehouse.sales.data.model.auth

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * User Account model
 * Received from GET /api/account
 */
@Parcelize
data class Account(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("login")
    val login: String? = null,

    @SerializedName("firstName")
    val firstName: String? = null,

    @SerializedName("lastName")
    val lastName: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("activated")
    val activated: Boolean = false,

    @SerializedName("langKey")
    val langKey: String? = "fr",

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("authorities")
    val authorities: List<String>? = null
) : Parcelable {

    /**
     * Get display name for user
     */
    fun getDisplayName(): String {
        return "${firstName ?: ""} ${lastName ?: ""}".trim().ifEmpty { login ?: "Utilisateur" }
    }

    /**
     * Check if user has admin authority
     */
    fun isAdmin(): Boolean {
        return authorities?.contains("ROLE_ADMIN") == true
    }

    /**
     * Check if user has specific authority
     */
    fun hasAuthority(authority: String): Boolean {
        return authorities?.contains(authority) == true
    }
}
