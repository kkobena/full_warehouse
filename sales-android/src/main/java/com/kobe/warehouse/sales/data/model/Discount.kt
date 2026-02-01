package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Discount model
 * Represents a discount applied to a sale or sale line
 */
@Parcelize
data class Discount(
    @SerializedName("id")
    val id: SaleId? = null,

    @SerializedName("value")
    val value: Int = 0,

    @SerializedName("type")
    val type: DiscountType = DiscountType.PERCENTAGE,

    @SerializedName("appliedTo")
    val appliedTo: DiscountTarget = DiscountTarget.SALE
) : Parcelable {

    /**
     * Calculate discount amount based on original amount
     */
    fun calculateDiscountAmount(originalAmount: Int): Int {
        return when (type) {
            DiscountType.PERCENTAGE -> (originalAmount * value) / 100
            DiscountType.FIXED -> minOf(value, originalAmount) // Can't discount more than original amount
        }
    }

    /**
     * Calculate net amount after discount
     */
    fun calculateNetAmount(originalAmount: Int): Int {
        return originalAmount - calculateDiscountAmount(originalAmount)
    }

    /**
     * Format discount value for display
     */
    fun getFormattedValue(): String {
        return when (type) {
            DiscountType.PERCENTAGE -> "$value %"
            DiscountType.FIXED -> "${formatAmount(value)} FCFA"
        }
    }

    /**
     * Format amount with space as thousand separator
     */
    private fun formatAmount(amount: Int): String {
        return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
    }

    /**
     * Validate discount value
     */
    fun isValid(): Boolean {
        return when (type) {
            DiscountType.PERCENTAGE -> value in 1..100
            DiscountType.FIXED -> value > 0
        }
    }
}

/**
 * Discount type enum
 */
enum class DiscountType {
    @SerializedName("PERCENTAGE")
    PERCENTAGE,

    @SerializedName("FIXED")
    FIXED
}

/**
 * Discount target enum
 */
enum class DiscountTarget {
    @SerializedName("SALE")
    SALE,

    @SerializedName("SALE_LINE")
    SALE_LINE
}

/**
 * Update Sale Info DTO for discount API
 * Maps to backend UpdateSaleInfo record
 */
@Parcelize
data class UpdateSaleInfo(
    @SerializedName("id")
    val id: SaleId,

    @SerializedName("value")
    val value: Int
) : Parcelable
