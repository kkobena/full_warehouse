package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Product model (simplified for inventory)
 * Matches backend ProduitSearch (returned by GET /api/produits/code/{code})
 */
data class Product(
    @SerializedName("id")
    val id: Long,

    @SerializedName("libelle")
    val libelle: String? = null,

    /** CIP principal (alias @JsonProperty("codeProduit") côté backend) */
    @SerializedName("codeProduit")
    val codeProduit: String? = null,

    @SerializedName("codeEanLabo")
    val codeEanLabo: String? = null,

    @SerializedName("regularUnitPrice")
    val regularUnitPrice: Int = 0,

    @SerializedName("costAmount")
    val costAmount: Int = 0,

    /** Stock rayon (PRINCIPAL) disponible */
    @SerializedName("totalQuantity")
    val totalQuantity: Int = 0
)
