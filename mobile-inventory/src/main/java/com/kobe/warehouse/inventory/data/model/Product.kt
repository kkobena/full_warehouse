package com.kobe.warehouse.inventory.data.model

import com.google.gson.annotations.SerializedName

/**
 * Product model (simplified for inventory)
 * Contains only fields needed for inventory counting
 */
data class Product(
    @SerializedName("id")
    val id: Long,

    @SerializedName("productCode")
    val productCode: String,

    @SerializedName("productName")
    val productName: String,

    @SerializedName("codeCip")
    val codeCip: String? = null,

    @SerializedName("codeEan")
    val codeEan: String? = null,

    @SerializedName("regularUnitPrice")
    val regularUnitPrice: Int = 0,

    @SerializedName("costAmount")
    val costAmount: Int = 0,

    @SerializedName("currentStockQuantity")
    val currentStockQuantity: Int = 0
)
