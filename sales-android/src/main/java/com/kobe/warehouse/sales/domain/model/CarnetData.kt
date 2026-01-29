package com.kobe.warehouse.sales.domain.model

import com.kobe.warehouse.sales.data.model.Customer

/**
 * Carnet Data for Credit Sales
 *
 * Contains credit/carnet-related information for carnet sales:
 * - Credit limit
 * - Current balance (encours)
 * - Available credit
 */
data class CarnetData(
    /**
     * Customer with carnet account
     */
    val customer: Customer,

    /**
     * Credit limit (plafond crédit) for this customer
     * Maximum amount the customer can owe
     */
    val limiteCredit: Int,

    /**
     * Current balance/encours (amount currently owed)
     * Sum of all unpaid carnet sales
     */
    val encours: Int,

    /**
     * Available credit (remaining credit)
     * Calculated as: limiteCredit - encours
     */
    val creditDisponible: Int = limiteCredit - encours
) {
    /**
     * Check if customer has reached their credit limit
     */
    fun hasReachedCreditLimit(): Boolean {
        return encours >= limiteCredit
    }



    /**
     * Check if a sale amount can be added without exceeding credit limit
     * @param amount Sale amount to check
     * @return true if sale can be made, false otherwise
     */
    fun canAddSale(amount: Int): Boolean {
        return (encours + amount) <= limiteCredit
    }

    /**
     * Get the maximum sale amount that can be made
     */
    fun getMaxSaleAmount(): Int {
        return creditDisponible.coerceAtLeast(0)
    }



    /**
     * Get credit usage percentage
     * @return Percentage of credit used (0-100)
     */
    fun getCreditUsagePercentage(): Int {
        if (limiteCredit == 0) return 0
        return ((encours.toFloat() / limiteCredit.toFloat()) * 100).toInt()
    }

    /**
     * Validate carnet data for a sale
     * @param saleAmount Amount of the sale to validate
     * @return Pair<Boolean, String?> - (isValid, errorMessage)
     */
    fun validate(saleAmount: Int): Pair<Boolean, String?> {
        if (saleAmount <= 0) {
            return false to "Le montant de la vente doit être supérieur à 0"
        }

        if (!canAddSale(saleAmount)) {
            return false to "Limite de crédit dépassée. Crédit disponible: $creditDisponible FCFA"
        }

        return true to null
    }

    companion object {
        /**
         * Create CarnetData with calculated available credit
         */
        fun create(customer: Customer, limiteCredit: Int, encours: Int): CarnetData {
            return CarnetData(
                customer = customer,
                limiteCredit = limiteCredit,
                encours = encours,
                creditDisponible = limiteCredit - encours
            )
        }
    }
}
