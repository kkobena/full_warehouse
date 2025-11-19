package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Product model for sales
 * Represents a pharmaceutical product available for sale
 */
@Parcelize
data class Product(
  @SerializedName("id")
    val id: Long = 0, // product id in database



  @SerializedName("produitId")
    val produitId: Long = 0,

  @SerializedName("codeProduit")
    val code: String = "",

  @SerializedName("libelle")
    val libelle: String = "",

  @SerializedName("regularUnitPrice")
    val regularUnitPrice: Int = 0,

  @SerializedName("netUnitPrice")
    val netUnitPrice: Int = 0,



  @SerializedName("totalQuantity")
    val totalQuantity: Int = 0,



  @SerializedName("forceStock")
    val forceStock: Boolean = false,

  @SerializedName("deconditionnable")
    val deconditionnable: Boolean = false
) : Parcelable {

    /**
     * Get display name (libelle)
     */
    fun getDisplayName(): String = libelle

    /**
     * Get formatted price
     */
    fun getFormattedPrice(): String {
      return "%,d FCFA".format(regularUnitPrice).replace(',', ' ')

    }

    /**
     * Check if product is in stock
     */
    fun isInStock(): Boolean {
        return totalQuantity > 0
    }


}


