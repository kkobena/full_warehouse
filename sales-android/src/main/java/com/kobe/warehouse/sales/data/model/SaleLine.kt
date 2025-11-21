package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlin.math.ceil

/**
 * Sale Line model
 * Represents a line item in a sale transaction
 */
@Parcelize
data class SaleLine(
  @SerializedName("id")
  val id: Long? = null,
  @SerializedName("produitId")
  val produitId: Long = 0,

  @SerializedName("code")
  val code: String? = null,

  @SerializedName("produitLibelle")
  val produitLibelle: String? = null,

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
  @SerializedName("taxValue")
  val taxValue: Int? = null,

  @SerializedName("taxAmountUg")
  val taxAmountUg: Int = 0,

  @SerializedName("costAmount")
  val costAmount: Int = 0,

  @SerializedName("montantRemise")
  val montantRemise: Int = 0,

  @SerializedName("montantTvaUg")
  val montantTvaUg: Int = 0,
  @SerializedName("qtyStock")
  val qtyStock: Int = 0,


  @SerializedName("forceStock")
  val forceStock: Boolean = false,
  @SerializedName("saleLineId")
  var saleLineId: SaleLineId? = null,

  // Timestamp for ordering cart items (not sent to backend)
  @Transient
  val addedOrModifiedAt: Long = System.currentTimeMillis()

) : Parcelable {

  /**
   * Get formatted unit price
   */
  fun getFormattedUnitPrice(): String {
    return "${formatAmount(regularUnitPrice)} FCFA"
  }

  /**
   * Get formatted total amount
   */
  fun getFormattedTotal(): String {
    return "${formatAmount(salesAmount)} FCFA"
  }

  /**
   * Format amount with space as thousand separator
   */
  private fun formatAmount(amount: Int): String {
    return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
  }


  /**
   * Check if quantity is available in stock
   */
  fun hasInsufficientStock(): Boolean {
    return quantitySold < quantityRequested && !forceStock
  }

  /**
   * Update quantity and recalculate amounts
   * Also updates the timestamp to reflect modification time
   */
  fun updateQuantity(newQuantity: Int): SaleLine {
    val newSalesAmount = regularUnitPrice * newQuantity
    val newNetAmount = netUnitPrice * newQuantity

    // Recalculate tax using backend formula if we have a stored tax rate
    val newTaxAmount = if (taxValue != null && taxValue > 0) {
      // Backend formula: value = 1 + (vatRate / 100)
      // taxAmount = salesAmount - ceil(salesAmount / value)
      val value = 1.0 + (taxValue.toDouble() / 100.0)
      val priceWithoutVat = ceil(newSalesAmount / value).toInt()
      newSalesAmount - priceWithoutVat
    } else {
      0
    }

    return copy(
      quantityRequested = newQuantity,
      quantitySold = newQuantity,
      salesAmount = newSalesAmount,
      netAmount = newNetAmount,
      taxAmount = newTaxAmount,
      addedOrModifiedAt = System.currentTimeMillis() // Update timestamp on modification
    )
  }

}

@Parcelize
data class SaleLineId(
  @SerializedName("id")
  val id: Long = 0,

  @SerializedName("saleDate")
  val saleDate: String = ""
) : Parcelable
