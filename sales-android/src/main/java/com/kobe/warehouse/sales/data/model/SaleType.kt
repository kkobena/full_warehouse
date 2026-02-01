package com.kobe.warehouse.sales.data.model

/**
 * Sealed class representing different types of sales in the pharmacy system
 *
 * Types:
 * - Comptant: Cash sales (simple payment, optional customer)
 * - Assurance: Insurance sales (requires customer, insurance data, tiers payants)
 * - Carnet: Credit/Notebook sales (requires customer, credit limit management)
 */
sealed class SaleType {
    /**
     * Cash/Comptant sale
     * - Simple payment flow
     * - Customer optional
     * - Direct payment (cash, card, mobile money)
     */
    object Comptant : SaleType() {
        override fun toString(): String = "COMPTANT"
    }

    /**
     * Insurance/Assurance sale
     * - Customer mandatory (must be insured)
     * - Tiers payant(s) required (insurance providers)
     * - Prescription data required
     * - Split payment: client part + insurance part
     *
     * @property saleCustomer The insured customer (nullable during type selection, required before adding products)
     * @property tiersPayants List of insurance providers (principal + optional complementaires)
     */
    data class Assurance(
        val saleCustomer: Customer?,
        val tiersPayants: List<TiersPayant>
    ) : SaleType() {
        override fun toString(): String = "ASSURANCE"

        fun getPrincipalTiersPayant(): TiersPayant? = tiersPayants.firstOrNull()
        fun getComplementaireTiersPayants(): List<TiersPayant> = tiersPayants.drop(1)
    }

    /**
     * Credit/Carnet sale
     * - Customer mandatory (must have carnet account)
     * - Credit limit check required
     * - Payment deferred (added to customer's carnet)
     *
     * @property saleCustomer The customer with carnet account (nullable during type selection, required before adding products)
     */
    data class Carnet(
        val saleCustomer: Customer?
    ) : SaleType() {
        override fun toString(): String = "CARNET"
    }

    /**
     * Check if this sale type requires a customer before adding products
     */
    fun requiresCustomer(): Boolean = when (this) {
        is Comptant -> false
        is Assurance -> true
        is Carnet -> true
    }

    /**
     * Get the customer associated with this sale type (if any)
     */
    fun getCustomer(): Customer? = when (this) {
        is Comptant -> null
        is Assurance -> saleCustomer
        is Carnet -> saleCustomer
    }

    /**
     * Check if customer is set (for Assurance/Carnet types)
     */
    fun hasCustomer(): Boolean = when (this) {
        is Comptant -> true  // Always true for Comptant (customer optional)
        is Assurance -> saleCustomer != null
        is Carnet -> saleCustomer != null
    }

    /**
     * Get display name for this sale type
     */
    fun getDisplayName(): String = when (this) {
        is Comptant -> "Vente Comptant"
        is Assurance -> "Vente Assurance"
        is Carnet -> "Vente Carnet"
    }
}
