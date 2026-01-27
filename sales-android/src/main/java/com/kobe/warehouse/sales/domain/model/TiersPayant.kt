package com.kobe.warehouse.sales.domain.model

import com.kobe.warehouse.sales.data.model.Customer

/**
 * Tiers Payant (Third-Party Payer / Insurance Provider)
 *
 * Represents an insurance provider or mutual insurance company that covers
 * part of the pharmaceutical costs for insured customers.
 */
data class TiersPayant(
    val id: Long,
    val name: String,
    val code: String? = null,
    val enabled: Boolean = true,
    val phone: String? = null,
    val mobile: String? = null,
    val address: String? = null,

    /**
     * Type of tiers payant:
     * - PRINCIPAL: Main insurance provider
     * - COMPLEMENTAIRE: Secondary/complementary insurance
     */
    val type: TiersPayantType = TiersPayantType.PRINCIPAL,

    /**
     * Default coverage rate (percentage)
     * Example: 80 means insurance covers 80%, client pays 20%
     */
    val tauxCouverture: Int = 0,

    /**
     * Plafond (ceiling/limit) for this tiers payant
     * Maximum amount the insurance will cover
     */
    val plafond: Int? = null,

    /**
     * Remote customer reference (for insurance system integration)
     */
    val remoteCustomer: Customer? = null
) {
    /**
     * Check if this tiers payant is the principal insurance provider
     */
    fun isPrincipal(): Boolean = type == TiersPayantType.PRINCIPAL

    /**
     * Check if this tiers payant is a complementary insurance provider
     */
    fun isComplementaire(): Boolean = type == TiersPayantType.COMPLEMENTAIRE

    /**
     * Format tiers payant info for display
     */
    fun getDisplayName(): String {
        val typeLabel = if (isPrincipal()) "Principal" else "Complémentaire"
        return "$name ($typeLabel - $tauxCouverture%)"
    }
}

/**
 * Type of Tiers Payant
 */
enum class TiersPayantType {
    PRINCIPAL,        // Main insurance provider
    COMPLEMENTAIRE    // Secondary/complementary insurance
}
