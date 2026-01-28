package com.kobe.warehouse.sales.domain.model

import com.kobe.warehouse.sales.data.model.Customer

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
        fun getDisplayName(): String = "Vente Comptant"
    }

    /**
     * Insurance/Assurance sale
     * - Customer mandatory (must be insured)
     * - Tiers payant(s) required (insurance providers)
     * - Prescription data required
     * - Split payment: client part + insurance part
     *
     * @property saleCustomer The insured customer
     * @property tiersPayants List of insurance providers (principal + optional complementaires)
     */
    data class Assurance(
        val saleCustomer: Customer,
        val tiersPayants: List<TiersPayant>
    ) : SaleType() {
        override fun toString(): String = "ASSURANCE"
        fun getDisplayName(): String = "Vente Assurance"

        fun getPrincipalTiersPayant(): TiersPayant? = tiersPayants.firstOrNull()
        fun getComplementaireTiersPayants(): List<TiersPayant> = tiersPayants.drop(1)
    }

    /**
     * Credit/Carnet sale
     * - Customer mandatory (must have carnet account)
     * - Credit limit check required
     * - Payment deferred (added to customer's carnet)
     *
     * @property saleCustomer The customer with carnet account
     */
    data class Carnet(
        val saleCustomer: Customer
    ) : SaleType() {
        override fun toString(): String = "CARNET"
        fun getDisplayName(): String = "Vente Carnet"
    }

    /**
     * Check if this sale type requires a customer
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
}
