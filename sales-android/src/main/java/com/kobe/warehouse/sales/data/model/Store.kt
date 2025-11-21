package com.kobe.warehouse.sales.data.model

import com.google.gson.annotations.SerializedName

/**
 * Store (Magasin) model
 * Represents pharmacy store information
 */
data class Store(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("fullName")
    val fullName: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("registre")
    val registre: String? = null,

    @SerializedName("compteContribuable")
    val compteContribuable: String? = null,

    @SerializedName("numComptable")
    val numComptable: String? = null,

    @SerializedName("compteBancaire")
    val compteBancaire: String? = null,

    @SerializedName("registreImposition")
    val registreImposition: String? = null,

    @SerializedName("welcomeMessage")
    val welcomeMessage: String? = null,

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("managerFirstName")
    val managerFirstName: String? = null,

    @SerializedName("managerLastName")
    val managerLastName: String? = null


) {
    /**
     * Get display name (prioritize fullName over name)
     */
    fun getDisplayName(): String {
        return fullName ?: name ?: "Pharmacie"
    }

    /**
     * Get manager full name
     */
    fun getManagerName(): String {
        val firstName = managerFirstName ?: ""
        val lastName = managerLastName ?: ""
        return "$firstName $lastName".trim().ifEmpty { "Gérant" }
    }

    /**
     * Get formatted address
     */
    fun getFormattedAddress(): String {
        return address ?: ""
    }

    /**
     * Get formatted phone
     */
    fun getFormattedPhone(): String {
        return phone ?: ""
    }
}
