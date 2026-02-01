package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * ThirdPartySaleLine - Insurance Payment Breakdown
 * Represents the amount that each insurance provider will pay for this sale
 */
@Parcelize
data class ThirdPartySaleLine(
    @SerializedName("id")
    val id: Long? = null,

    /**
     * Amount this insurance will pay
     */
    @SerializedName("montant")
    val montant: Int = 0,

    /**
     * Reference to ClientTiersPayant
     */
    @SerializedName("clientTiersPayantId")
    val clientTiersPayantId: Long? = null,

    /**
     * Customer ID
     */
    @SerializedName("customerId")
    val customerId: Long? = null,

    /**
     * Insurance provider ID
     */
    @SerializedName("tiersPayantId")
    val tiersPayantId: Long? = null,

    /**
     * Coverage rate for this insurance
     */
    @SerializedName("taux")
    val taux: Int = 0,

    /**
     * Status
     */
    @SerializedName("statut")
    val statut: String? = null,

    /**
     * Customer full name
     */
    @SerializedName("customerFullName")
    val customerFullName: String? = null,



    /**
     * Prescription number
     */
    @SerializedName("numBon")
    val numBon: String? = null,

    /**
     * Customer's insurance number (matricule)
     */
    @SerializedName("num")
    val num: String? = null,

    /**
     * Insurance provider full name
     */
    @SerializedName("tiersPayantFullName")
    val tiersPayantFullName: String? = null,

    /**
     * Name
     */
    @SerializedName("name")
    val name: String? = null
) : Parcelable
