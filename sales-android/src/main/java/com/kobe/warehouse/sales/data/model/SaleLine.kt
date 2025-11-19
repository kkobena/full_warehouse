package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Sale Line model
 * Represents a line item in a sale transaction
 */
@Parcelize
data class SaleLine(
  @SerializedName("id")
  val id: Long = 0,


  @SerializedName("produitId")
  val produitId: Long = 0,

  @SerializedName("code")
  val code: String = "",

  @SerializedName("produitLibelle")
  val produitLibelle: String = "",

  @SerializedName("quantityRequested")
  var quantityRequested: Int = 1,

  @SerializedName("quantitySold")
  var quantitySold: Int = 1,

  @SerializedName("quantityUg")
  val quantityUg: Int = 0,

  @SerializedName("regularUnitPrice")
  val regularUnitPrice: Int = 0,

  @SerializedName("netUnitPrice")
  val netUnitPrice: Int = 0,

  @SerializedName("discountAmount")
  val discountAmount: Int = 0,

  @SerializedName("discountAmountUg")
  val discountAmountUg: Int = 0,

  @SerializedName("salesAmount")
  var salesAmount: Int = 0,

  @SerializedName("netAmount")
  var netAmount: Int = 0,

  @SerializedName("taxAmount")
  val taxAmount: Int = 0,

  @SerializedName("costAmount")
  val costAmount: Int = 0,

  @SerializedName("montantRemise")
  val montantRemise: Int = 0,

  @SerializedName("montantTvaUg")
  val montantTvaUg: Int = 0,



  @SerializedName("forceStock")
  val forceStock: Boolean = false,
  @SerializedName("saleLineId")
  var saleLineId: SaleLineId

) : Parcelable {

  /**
   * Get formatted unit price
   */
  fun getFormattedUnitPrice(): String {
    return "${regularUnitPrice} FCFA"
  }

  /**
   * Get formatted total amount
   */
  fun getFormattedTotal(): String {
    return "${salesAmount} FCFA"
  }

  /**
   * Calculate line total
   */
  fun calculateTotal(): Int {
    return regularUnitPrice * quantitySold
  }

  /**
   * Check if quantity is available in stock
   */
  fun hasInsufficientStock(): Boolean {
    return quantitySold < quantityRequested && !forceStock
  }

  /**
   * Update quantity and recalculate amounts
   */
  fun updateQuantity(newQuantity: Int): SaleLine {

    return copy(
      quantityRequested = newQuantity,
      quantitySold = newQuantity,
      salesAmount = regularUnitPrice * newQuantity,
      netAmount = netUnitPrice * newQuantity
    )
  }

  /**
   * Increase quantity by 1
   */
  fun incrementQuantity(): SaleLine {
    return updateQuantity(quantityRequested + 1)
  }

  /**
   * Decrease quantity by 1 (minimum 1)
   */
  fun decrementQuantity(): SaleLine {
    return updateQuantity(maxOf(1, quantityRequested - 1))
  }
}

@Parcelize
data class SaleLineId(
  @SerializedName("id")
  val id: Long = 0,

  @SerializedName("saleDate")
  val saleDate: String = ""
) : Parcelable
