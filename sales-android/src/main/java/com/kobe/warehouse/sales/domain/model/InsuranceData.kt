package com.kobe.warehouse.sales.domain.model

import com.kobe.warehouse.sales.data.model.Customer

/**
 * Insurance Data for Assurance Sales
 *
 * Contains all insurance-related information required for insurance sales:
 * - Tiers payant(s) (insurance providers)
 * - Prescription type and details
 * - Coverage rates
 * - Customer beneficiary information
 */
data class InsuranceData(
    /**
     * Principal tiers payant (main insurance provider)
     * Always required for assurance sales
     */
    val tiersPayantPrincipal: TiersPayant,

    /**
     * Complementary tiers payants (optional)
     * Additional insurance providers for extra coverage
     */
    val tiersPayantsComplementaires: List<TiersPayant> = emptyList(),

    /**
     * Type of prescription (Ordonnance, Bon de prise en charge, etc.)
     */
    val prescriptionType: PrescriptionType,

    /**
     * Numéro de bon (Authorization number)
     * Required for certain prescription types (BPC, Protocole, etc.)
     */
    val numeroBon: String? = null,

    /**
     * Coverage rate for principal tiers payant (percentage)
     * Example: 80 means insurance covers 80%, client pays 20%
     */
    val tauxCouverturePrincipal: Int,

    /**
     * Coverage rates for complementary tiers payants (if any)
     * Map of TiersPayant ID to coverage rate
     */
    val tauxCouvertureComplementaires: Map<Long, Int> = emptyMap(),

    /**
     * Ayant droit (beneficiary)
     * If sale is for a beneficiary (dependent) of the main insured customer
     * Can be null if sale is for the main customer themselves
     */
    val ayantDroit: Customer? = null,

    /**
     * Notes/remarks about insurance coverage
     */
    val remarques: String? = null
) {
    /**
     * Get all tiers payants (principal + complementaires)
     */
    fun getAllTiersPayants(): List<TiersPayant> {
        return listOf(tiersPayantPrincipal) + tiersPayantsComplementaires
    }

    /**
     * Get total coverage rate (sum of all tiers payants)
     * Capped at 100%
     */
    fun getTotalCoverageRate(): Int {
        val total = tauxCouverturePrincipal + tauxCouvertureComplementaires.values.sum()
        return total.coerceAtMost(100)
    }

    /**
     * Get client's share rate (100% - total coverage)
     */
    fun getClientShareRate(): Int {
        return 100 - getTotalCoverageRate()
    }

    /**
     * Validate insurance data
     * @return Pair<Boolean, String?> - (isValid, errorMessage)
     */
    fun validate(): Pair<Boolean, String?> {
        // Check if numero bon is required but missing
        if (prescriptionType.requiresNumeroBon() && numeroBon.isNullOrBlank()) {
            return false to "Le numéro de bon est obligatoire pour ce type de prescription"
        }

        // Check if coverage rates are valid
        if (tauxCouverturePrincipal !in 0..100) {
            return false to "Le taux de couverture principal doit être entre 0 et 100%"
        }

        tauxCouvertureComplementaires.forEach { (_, rate) ->
            if (rate !in 0..100) {
                return false to "Les taux de couverture complémentaires doivent être entre 0 et 100%"
            }
        }

        // Check if total coverage doesn't exceed 100%
        if (getTotalCoverageRate() > 100) {
            return false to "Le taux de couverture total ne peut pas dépasser 100%"
        }

        return true to null
    }

    /**
     * Calculate insurance part for a given amount
     */
    fun calculateInsurancePart(amount: Int): Int {
        return (amount * getTotalCoverageRate()) / 100
    }

    /**
     * Calculate client part for a given amount
     */
    fun calculateClientPart(amount: Int): Int {
        return amount - calculateInsurancePart(amount)
    }
}
