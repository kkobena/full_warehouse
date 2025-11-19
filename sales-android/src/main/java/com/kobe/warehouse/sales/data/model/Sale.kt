package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sale model
 * Represents a complete sale transaction
 */
@Parcelize
data class Sale(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("numberTransaction")
    val numberTransaction: String = "",

    @SerializedName("saleId")
    val saleId: SaleId? = null,

    @SerializedName("natureVente")
    val natureVente: String = "VNO", // VNO, ASSURANCE, CARNET

    @SerializedName("statut")
    val statut: String = "PENDING", // PENDING, VALIDATED, CANCELED

    @SerializedName("salesAmount")
    var salesAmount: Int = 0,

    @SerializedName("netAmount")
    var netAmount: Int = 0,

    @SerializedName("discountAmount")
    var discountAmount: Int = 0,

    @SerializedName("taxAmount")
    var taxAmount: Int = 0,

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

    @SerializedName("salesLines")
    var salesLines: MutableList<SaleLine> = mutableListOf(),

    @SerializedName("payments")
    var payments: MutableList<Payment> = mutableListOf(),

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,


) : Parcelable {

    /**
     * Get formatted sales amount
     */
    fun getFormattedSalesAmount(): String {
        return "${salesAmount} FCFA"
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
     * Add product to cart
     */
    fun addProduct(product: Product, quantity: Int = 1): Sale {
        // Check if product already exists in cart
        val existingLine = salesLines.find { it.id == product.id }

        if (existingLine != null) {
            // Update quantity
            val updatedLines = salesLines.map {
                if (it.id == product.id) {
                    it.updateQuantity(it.quantityRequested + quantity)
                } else {
                    it
                }
            }.toMutableList()

            return copy(salesLines = updatedLines).recalculateTotals()
        } else {
            // Add new line
            val newLine = SaleLine(
                id = product.id,
                produitId = product.produitId,
                code = product.code,
                produitLibelle = product.libelle,
                quantityRequested = quantity,
                quantitySold = minOf(quantity, product.totalQuantity),
                regularUnitPrice = product.regularUnitPrice,
                netUnitPrice = product.netUnitPrice,
                salesAmount = product.regularUnitPrice * minOf(quantity, product.totalQuantity),
                netAmount = product.netUnitPrice * minOf(quantity, product.totalQuantity),
                qtyStock = product.totalQuantity,
                forceStock = product.forceStock
            )

            salesLines.add(newLine)
            return recalculateTotals()
        }
    }

    /**
     * Remove product from cart
     */
    fun removeProduct(saleLine: SaleLine): Sale {
        salesLines.remove(saleLine)
        return recalculateTotals()
    }

    /**
     * Update product quantity in cart
     */
    fun updateProductQuantity(saleLine: SaleLine, newQuantity: Int): Sale {
        val updatedLines = salesLines.map {
            if (it.produitId == saleLine.produitId) {
                it.updateQuantity(newQuantity)
            } else {
                it
            }
        }.toMutableList()

        return copy(salesLines = updatedLines).recalculateTotals()
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
