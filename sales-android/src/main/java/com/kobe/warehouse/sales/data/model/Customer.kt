package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Customer model
 * Represents a customer in the pharmacy system
 */
@Parcelize
data class Customer(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("firstName")
    val firstName: String = "",

    @SerializedName("lastName")
    val lastName: String = "",

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("fullName")
    val fullName: String = "",

    @SerializedName("remiseId")
    val remiseId: Long? = null,


) : Parcelable {

    /**
     * Get display name
     */
    fun getDisplayName(): String {
        return if (fullName.isNotEmpty()) {
            fullName
        } else {
            "$firstName $lastName".trim()
        }
    }

    /**
     * Get initials
     */
    fun getInitials(): String {
        val first = firstName.firstOrNull()?.uppercase() ?: ""
        val last = lastName.firstOrNull()?.uppercase() ?: ""
        return "$first$last"
    }


}

/**
 * Customer search request
 */
data class CustomerSearchRequest(
    @SerializedName("search")
    val search: String = "",

    @SerializedName("limit")
    val limit: Int = 20
)
