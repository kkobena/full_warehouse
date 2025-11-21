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

  @SerializedName("codeProduit")
  val code: String? = null,

  @SerializedName("libelle")
  val libelle: String? = null,

  @SerializedName("regularUnitPrice")
  val regularUnitPrice: Int = 0,

  @SerializedName("netUnitPrice")
  val netUnitPrice: Int = 0,

  @SerializedName("totalQuantity")
  val totalQuantity: Int = 0,
  @SerializedName("vatRate")
  val vatRate:Int = 0,
  @SerializedName("forceStock")
  val forceStock: Boolean = false,

  @SerializedName("deconditionnable")
  val deconditionnable: Boolean = false
) : Parcelable {

  /**
   * Get display name (libelle)
   */
  fun getDisplayName(): String = libelle ?: ""

  /**
   * Get formatted price
   */
  fun getFormattedPrice(): String {
    return formatAmount(regularUnitPrice) + " FCFA"
  }

  /**
   * Format amount with space as thousand separator
   */
  private fun formatAmount(amount: Int): String {
    return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
  }

  /**
   * Check if product is in stock
   */
  fun isInStock(): Boolean {
    return totalQuantity > 0
  }


}


