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

    // Carnet credit fields
    @SerializedName("creditLimit")
    val creditLimit: Int? = null,

    @SerializedName("currentBalance")
    val currentBalance: Int? = null

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

    /**
     * Get available credit for carnet sales
     * Returns creditLimit - currentBalance
     */
    fun getAvailableCredit(): Int {
        val limit = creditLimit ?: 0
        val balance = currentBalance ?: 0
        return limit - balance
    }

    /**
     * Check if customer is eligible for carnet sales
     * Eligible if creditLimit is set and > 0
     */
    fun isEligibleForCarnet(): Boolean {
        return (creditLimit ?: 0) > 0
    }

    /**
     * Get formatted credit limit
     */
    fun getFormattedCreditLimit(): String {
        val limit = creditLimit ?: 0
        return "${formatAmount(limit)} FCFA"
    }

    /**
     * Get formatted current balance
     */
    fun getFormattedCurrentBalance(): String {
        val balance = currentBalance ?: 0
        return "${formatAmount(balance)} FCFA"
    }

    /**
     * Get formatted available credit
     */
    fun getFormattedAvailableCredit(): String {
        return "${formatAmount(getAvailableCredit())} FCFA"
    }

    /**
     * Format amount with space as thousand separator
     */
    private fun formatAmount(amount: Int): String {
        return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
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
