package com.kobe.warehouse.inventory.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * User Account model
 * Response from /api/account endpoint
 */
data class Account(
    @SerializedName("id")
    val id: Long,

    @SerializedName("login")
    val login: String,

    @SerializedName("firstName")
    val firstName: String?,

    @SerializedName("lastName")
    val lastName: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("activated")
    val activated: Boolean,

    @SerializedName("langKey")
    val langKey: String?,

    @SerializedName("authorities")
    val authorities: List<String>? = emptyList()
) {
    fun getFullName(): String {
        return if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty()) {
            "$firstName $lastName"
        } else {
            login
        }
    }
}
