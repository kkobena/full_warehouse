package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * TiersPayant DTO - Insurance Provider / Third-Party Payer
 * Corresponds to backend ITiersPayant model
 *
 * Represents an insurance provider or mutual insurance company
 */
@Parcelize
data class TiersPayant(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("fullName")
    val fullName: String? = null,

    @SerializedName("codeOrganisme")
    val codeOrganisme: String? = null,

    @SerializedName("consoMensuelle")
    val consoMensuelle: Int? = null,
    @SerializedName("telephone")
    val telephone: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("plafondConso")
    val plafondConso: Int? = null,

    @SerializedName("categorie")
    val categorie: String? = null,


    @SerializedName("plafondJournalierClient")
    val plafondJournalierClient: Int? = null,

    @SerializedName("plafondConsoClient")
    val plafondConsoClient: Int? = null,

    @SerializedName("statut")
    val statut: String? = null,

    @SerializedName("telephoneFixe")
    val telephoneFixe: String? = null,

    @SerializedName("adresse")
    val adresse: String? = null,

    @SerializedName("nbreBordereaux")
    val nbreBordereaux: Int? = null

) : Parcelable {

    /**
     * Get display name for UI
     */
    fun getDisplayName(): String {
        return fullName ?: name ?: "Tiers payant #$id"
    }

    /**
     * Check if enabled
     */
    fun isEnabled(): Boolean {
        return statut != "DISABLE"
    }

    /**
     * Get category display label
     */
    fun getCategoryLabel(): String {
        return when (categorie) {
            "ASSURANCE" -> "Assurance"
            "CARNET" -> "Carnet"
            else -> categorie ?: "Non défini"
        }
    }
}
