package com.kobe.warehouse.reports.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility class for number formatting.
 */
object NumberFormatUtils {
    
    private val currencyFormat = DecimalFormat("#,##0").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            groupingSeparator = ' '
        }
    }
    
    private val decimalFormat = DecimalFormat("#,##0.00").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }
    }
    
    private val percentFormat = DecimalFormat("#0.0").apply {
        decimalFormatSymbols = decimalFormatSymbols.apply {
            decimalSeparator = ','
        }
    }
    
    /**
     * Formats a number as currency (e.g., "1 500 000 F")
     */
    fun formatCurrency(amount: Number?): String {
        if (amount == null) return "0 F"
        return "${currencyFormat.format(amount)} F"
    }
    
    /**
     * Formats a number as currency without the F suffix
     */
    fun formatAmount(amount: Number?): String {
        if (amount == null) return "0"
        return currencyFormat.format(amount)
    }
    
    /**
     * Formats a number with decimal places
     */
    fun formatDecimal(value: Number?): String {
        if (value == null) return "0,00"
        return decimalFormat.format(value)
    }
    
    /**
     * Formats a number as percentage (e.g., "45,5%")
     */
    fun formatPercent(value: Number?): String {
        if (value == null) return "0%"
        return "${percentFormat.format(value)}%"
    }
    
    /**
     * Formats a quantity
     */
    fun formatQuantity(quantity: Number?): String {
        if (quantity == null) return "0"
        return currencyFormat.format(quantity)
    }

    /**
     * Formats a number in compact form (e.g., "1.5M", "125K")
     */
    fun formatCompact(amount: Number?): String {
        if (amount == null) return "0"
        val value = amount.toLong()
        return when {
            value >= 1_000_000_000 -> String.format(Locale.FRENCH, "%.1fMd", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format(Locale.FRENCH, "%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format(Locale.FRENCH, "%.0fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
