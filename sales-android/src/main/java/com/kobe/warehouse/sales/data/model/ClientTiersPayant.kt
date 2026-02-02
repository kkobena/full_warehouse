package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * ClientTiersPayant - Association between Customer and Insurance Provider
 * Represents a customer's insurance relationship with specific coverage details
 */
@Parcelize
data class ClientTiersPayant(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("customerId")
    val customerId: Int? = null,

    @SerializedName("tiersPayantId")
    val tiersPayantId: Long,

    @SerializedName("tiersPayantName")
    val tiersPayantName: String? = null,

    @SerializedName("tiersPayantFullName")
    val tiersPayantFullName: String? = null,

    /**
     * Customer's insurance number (matricule)
     */
    @SerializedName("num")
    val num: String? = null,

    /**
     * Coverage rate (percentage)
     * Example: 80 means insurance covers 80%, client pays 20%
     */
    @SerializedName("taux")
    val taux: Int = 0,

    /**
     * Priority: R0 (0) = Principal, R1-R3 (1-3) = Complementary
     * Corresponds to PrioriteTiersPayant enum
     */
    @SerializedName("priorite")
    @JsonAdapter(PrioriteTiersPayantDeserializer::class)
    val priorite: PrioriteTiersPayant? = null,

    /**
     * Prescription number (numéro de bon)
     */
    @SerializedName("numBon")
    val numBon: String? = null,

    /**
     * Type: "PRINCIPAL" or "COMPLEMENTAIRE"
     */
    @SerializedName("typeTiersPayant")
    val typeTiersPayant: String? = null,

    /**
     * Monthly consumption limit
     */
    @SerializedName("plafondConso")
    val plafondConso: Int? = null,

    /**
     * Daily consumption limit
     */
    @SerializedName("plafondJournalier")
    val plafondJournalier: Int? = null,

    /**
     * Whether the limit is absolute
     */
    @SerializedName("plafondAbsolu")
    val plafondAbsolu: Boolean = false,

    /**
     * Status: "ENABLE" or "DISABLE"
     */
    @SerializedName("statut")
    val statut: String? = null,

    /**
     * Category
     */
    @SerializedName("categorie")
    val categorie: Int? = null
) : Parcelable {
    /**
     * Check if this is the principal insurance
     */
    fun isPrincipal(): Boolean = priorite?.isPrincipal() == true || typeTiersPayant == "PRINCIPAL"

    /**
     * Check if this is complementary insurance
     */
    fun isComplementaire(): Boolean = priorite?.isComplementaire() == true || typeTiersPayant == "COMPLEMENTAIRE"

    /**
     * Get display name with details
     */
    fun getDisplayName(): String {
        val typeLabel = if (isPrincipal()) "Principal" else "Complémentaire"
        val bonInfo = if (!numBon.isNullOrEmpty()) " - Bon: $numBon" else ""
        return "${tiersPayantName ?: tiersPayantFullName} ($typeLabel - $taux%)$bonInfo"
    }
}
