package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Sale model
 * Represents a complete sale transaction
 */
@Parcelize
data class Sale(
  @SerializedName("id")
  val id: Long? = null,

  @SerializedName("numberTransaction")
  val numberTransaction: String = "",

  @SerializedName("saleId")
  val saleId: SaleId? = null,

  @SerializedName("natureVente")
  val natureVente: String = "COMPTANT", // COMPTANT, ASSURANCE, CARNET

  @SerializedName("statut")
  val statut: String = "PENDING", // PENDING, CLOSED, CANCELED

  @SerializedName("salesAmount")
  var salesAmount: Int = 0,

  @SerializedName("netAmount")
  var netAmount: Int = 0,

  @SerializedName("discountAmount")
  var discountAmount: Int = 0,

  @SerializedName("taxAmount")
  var taxAmount: Int = 0,
  @SerializedName("payrollAmount")
  var payrollAmount: Int = 0,

  @SerializedName("costAmount")
  val costAmount: Int = 0,

  @SerializedName("montantVerse")
  var montantVerse: Int = 0,

  @SerializedName("montantRendu")
  val montantRendu: Int = 0,

  @SerializedName("restToPay")
  val restToPay: Int = 0,

  @SerializedName("customer")
  val customer: Customer? = null,

  @SerializedName("customerId")
  val customerId: Long? = null,

  @SerializedName("seller")
  val seller: User? = null,

  @SerializedName("cassier")
  val cassier: User? = null,
  @SerializedName("cassierId")
  val cassierId: Long? = null,

  @SerializedName("salesLines")
  var salesLines: MutableList<SaleLine> = mutableListOf(),

  @SerializedName("payments")
  var payments: MutableList<Payment> = mutableListOf(),

  @SerializedName("createdAt")
  val createdAt: String? = null,

  @SerializedName("updatedAt")
  val updatedAt: String? = null,
  @SerializedName("type")
  val type: String = "VNO",
  @SerializedName("typePrescription")
  val typePrescription: String = "PRESCRIPTION",

  ) : Parcelable {

  /**
   * Get formatted sales amount
   */
  fun getFormattedSalesAmount(): String {
    return "${formatAmount(salesAmount)} FCFA"
  }

  /**
   * Get formatted tax amount
   */
  fun getFormattedTaxAmount(): String {
    return "${formatAmount(taxAmount)} FCFA"
  }

  /**
   * Get total tax amount (VAT)
   */
  fun getTotalTax(): Int {
    return taxAmount
  }

  /**
   * Format amount with space as thousand separator
   */
  private fun formatAmount(amount: Int): String {
    return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
  }

  /**
   * Get number of items
   */
  fun getItemCount(): Int {
    return salesLines.size
  }

  /**
   * Get total quantity
   */
  fun getTotalQuantity(): Int {
    return salesLines.sumOf { it.quantitySold }
  }

  /**
   * Get customer display name
   */
  fun getCustomerName(): String {
    return customer?.getDisplayName() ?: "Client comptant"
  }

  /**
   * Get nature label
   */
  fun getNatureLabel(): String {
    return when (natureVente) {
      "ASSURANCE" -> "Assurance"
      "CARNET" -> "Carnet"
      else -> "VNO"
    }
  }

  /**
   * Format updated date
   */
  fun getFormattedUpdatedDate(): String {
    if (updatedAt == null) return ""

    return try {
      val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
      val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
      val date = inputFormat.parse(updatedAt)
      outputFormat.format(date ?: Date())
    } catch (e: Exception) {
      updatedAt
    }
  }

  /**
   * Check if sale is pending
   */
  fun isPending(): Boolean {
    return statut == "PENDING"
  }

  /**
   * Check if sale is validated
   */
  fun isValidated(): Boolean {
    return statut == "VALIDATED"
  }

  /**
   * Sort sales lines by timestamp descending (most recent first)
   */
  private fun sortSalesLinesByTimestamp(lines: MutableList<SaleLine>): MutableList<SaleLine> {
    return lines.sortedByDescending { it.addedOrModifiedAt }.toMutableList()
  }

  /**
   * Add product to cart
   * Items are ordered by addition/modification time (most recent at top)
   */
  fun addProduct(product: Product, quantity: Int = 1): Sale {

    val existingLine = salesLines.find { it.produitId == product.id }


    if (existingLine != null) {
      // Update quantity (this updates the timestamp automatically)
      val updatedLines = salesLines.map {
        if (it.produitId == product.id) {
          it.updateQuantity(it.quantityRequested + quantity)
        } else {
          it
        }
      }.toMutableList()

      // Sort by timestamp descending (most recent first)
      val sortedLines = sortSalesLinesByTimestamp(updatedLines)
      return copy(salesLines = sortedLines).recalculateTotals()
    } else {
      // Add new line
      val quantitySold = minOf(quantity, product.totalQuantity)
      val salesAmount = product.regularUnitPrice * quantitySold
      val netAmount = product.netUnitPrice * quantitySold

      // Calculate VAT using backend formula:
      // value = 1 + (vatRate / 100)
      // taxAmount = salesAmount - ceil(salesAmount / value)
      val taxAmount = if (product.vatRate > 0) {
        val value = 1.0 + (product.vatRate.toDouble() / 100.0)
        val priceWithoutVat = kotlin.math.ceil(salesAmount / value).toInt()
        salesAmount - priceWithoutVat
      } else {
        0
      }

      val newLine = SaleLine(
        produitId = product.id,
        code = product.code ?: "",
        produitLibelle = product.libelle ?: "",
        quantityRequested = quantity,
        quantitySold = quantitySold,
        regularUnitPrice = product.regularUnitPrice,
        netUnitPrice = product.netUnitPrice,
        salesAmount = salesAmount,
        netAmount = netAmount,
        taxAmount = taxAmount,
        taxValue = if (product.vatRate > 0) product.vatRate else null,
        qtyStock = product.totalQuantity,
        forceStock = product.forceStock
      )

      // Create new list with added line at the beginning (most recent)
      val updatedLines = salesLines.toMutableList()
      updatedLines.add(0, newLine) // Add at index 0 to put it at top

      return copy(salesLines = updatedLines).recalculateTotals()
    }
  }

  /**
   * Remove product from cart
   */
  fun removeProduct(saleLine: SaleLine): Sale {
    val updatedLines = salesLines.filter { it.produitId != saleLine.produitId }.toMutableList()
    return copy(salesLines = updatedLines).recalculateTotals()
  }

  /**
   * Update product quantity in cart
   * Modified item will be moved to top of the list
   */
  fun updateProductQuantity(saleLine: SaleLine, newQuantity: Int): Sale {
    val updatedLines = salesLines.map {
      if (it.produitId == saleLine.produitId) {
        it.updateQuantity(newQuantity) // This updates the timestamp
      } else {
        it
      }
    }.toMutableList()

    // Sort by timestamp descending (most recent first)
    val sortedLines = sortSalesLinesByTimestamp(updatedLines)
    return copy(salesLines = sortedLines).recalculateTotals()
  }

  /**
   * Clear all cart items
   */
  fun clearCart(): Sale {
    return copy(
      salesLines = mutableListOf(),
      salesAmount = 0,
      netAmount = 0,
      discountAmount = 0,
      taxAmount = 0
    )
  }

  /**
   * Recalculate totals
   */
  fun recalculateTotals(): Sale {
    val totalSales = salesLines.sumOf { it.salesAmount }
    val totalNet = salesLines.sumOf { it.netAmount }
    val totalDiscount = salesLines.sumOf { it.discountAmount }
    val totalTax = salesLines.sumOf { it.taxAmount }

    return copy(
      salesAmount = totalSales,
      netAmount = totalNet,
      discountAmount = totalDiscount,
      taxAmount = totalTax
    )
  }

  /**
   * Calculate payroll amount
   * payrollAmount = salesAmount - discountAmount (for all payment modes)
   */
  fun calculatePayrollAmount(): Int {
    return salesAmount - discountAmount
  }

  /**
   * Get formatted payroll amount
   */
  fun getFormattedPayrollAmount(): String {
    return "${formatAmount(payrollAmount)} FCFA"
  }
}

/**
 * Sale ID composite key
 */
@Parcelize
data class SaleId(
  @SerializedName("id")
  val id: Long = 0,

  @SerializedName("saleDate")
  val saleDate: String = ""
) : Parcelable

/**
 * Payment model
 */
@Parcelize
data class Payment(
  @SerializedName("id")
  val id: Long = 0,

  @SerializedName("pkey")
  val pkey: String = "",

  @SerializedName("amount")
  val amount: Int = 0,

  @SerializedName("paymentMode")
  val paymentMode: PaymentMode? = null,

  @SerializedName("paymentModeId")
  val paymentModeId: Long? = null,

  @SerializedName("paymentModeCode")
  val paymentModeCode: String = "CASH",

  @SerializedName("netAmount")
  val netAmount: Int = 0,

  @SerializedName("paidAmount")
  val paidAmount: Int = 0,

  @SerializedName("montantVerse")
  val montantVerse: Int = 0,

  @SerializedName("montantRendu")
  val montantRendu: Int = 0,

  @SerializedName("createdAt")
  val createdAt: String? = null,

  @SerializedName("updatedAt")
  val updatedAt: String? = null
) : Parcelable

/**
 * User model (simplified for sales)
 */
@Parcelize
data class User(
  @SerializedName("id")
  val id: Long = 0,

  @SerializedName("firstName")
  val firstName: String = "",

  @SerializedName("lastName")
  val lastName: String = "",

  @SerializedName("abbrName")
  val abbrName: String = "",

  @SerializedName("fullName")
  val fullName: String = ""
) : Parcelable
