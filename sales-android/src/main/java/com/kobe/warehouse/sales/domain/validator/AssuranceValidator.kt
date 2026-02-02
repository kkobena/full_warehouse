package com.kobe.warehouse.sales.domain.validator

import com.kobe.warehouse.sales.data.model.InsuranceData
import com.kobe.warehouse.sales.data.model.TiersPayant

/**
 * Validator for Assurance (Insurance) Sales
 *
 * Validates insurance data and business rules for insurance sales
 */
class AssuranceValidator {

    /**
     * Validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    ) {
        fun hasErrors(): Boolean = errors.isNotEmpty()
        fun getErrorMessage(): String? = errors.firstOrNull()
        fun getAllErrorMessages(): String = errors.joinToString("\n")
    }

    /**
     * Validate insurance data
     */
    fun validateInsuranceData(insuranceData: InsuranceData?): ValidationResult {
        if (insuranceData == null) {
            return ValidationResult(false, listOf("Les données d'assurance sont obligatoires"))
        }

        val errors = mutableListOf<String>()

        // Validate tiers payant principal
        if (!insuranceData.tiersPayantPrincipal.isEnabled()) {
            errors.add("Le tiers payant principal n'est pas actif")
        }

        // Validate coverage rates
        val coverageErrors = validateCoverageRates(
            insuranceData.tauxCouverturePrincipal,
            insuranceData.tauxCouvertureComplementaires
        )
        errors.addAll(coverageErrors)

        // Validate complementary tiers payants
        insuranceData.tiersPayantsComplementaires.forEach { tp ->
            if (!tp.isEnabled()) {
                errors.add("Le tiers payant complémentaire '${tp.getDisplayName()}' n'est pas actif")
            }
        }

        // Use InsuranceData's own validation
        val (dataValid, dataError) = insuranceData.validate()
        if (!dataValid && dataError != null) {
            errors.add(dataError)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validate coverage rates
     */
    private fun validateCoverageRates(
        tauxPrincipal: Int,
        tauxComplementaires: Map<Long, Int>
    ): List<String> {
        val errors = mutableListOf<String>()

        // Validate principal coverage rate
        if (tauxPrincipal < 0) {
            errors.add("Le taux de couverture principal ne peut pas être négatif")
        } else if (tauxPrincipal > 100) {
            errors.add("Le taux de couverture principal ne peut pas dépasser 100%")
        } else if (tauxPrincipal == 0) {
            errors.add("Le taux de couverture principal doit être supérieur à 0%")
        }

        // Validate complementary coverage rates
        tauxComplementaires.forEach { (id, rate) ->
            if (rate < 0) {
                errors.add("Le taux de couverture complémentaire ne peut pas être négatif")
            } else if (rate > 100) {
                errors.add("Le taux de couverture complémentaire ne peut pas dépasser 100%")
            }
        }

        // Validate total coverage
        val totalCoverage = tauxPrincipal + tauxComplementaires.values.sum()
        if (totalCoverage > 100) {
            errors.add("Le taux de couverture total ($totalCoverage%) ne peut pas dépasser 100%")
        }

        return errors
    }

    /**
     * Validate tiers payant
     */
    fun validateTiersPayant(tiersPayant: TiersPayant?): ValidationResult {
        if (tiersPayant == null) {
            return ValidationResult(false, listOf("Le tiers payant est obligatoire"))
        }

        val errors = mutableListOf<String>()

        if (tiersPayant.name.isNullOrBlank()) {
            errors.add("Le nom du tiers payant ne peut pas être vide")
        }

        if (!tiersPayant.isEnabled()) {
            errors.add("Le tiers payant '${tiersPayant.getDisplayName()}' n'est pas actif")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validate that customer is insured
     */
    fun validateCustomerIsInsured(customerId: Int?): ValidationResult {
        if (customerId == null) {
            return ValidationResult(
                false,
                listOf("Un client assuré est obligatoire pour une vente assurance")
            )
        }
        return ValidationResult(true)
    }

    /**
     * Validate sale amount for insurance
     */
    fun validateSaleAmount(amount: Int): ValidationResult {
        val errors = mutableListOf<String>()

        if (amount <= 0) {
            errors.add("Le montant de la vente doit être supérieur à 0")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validate plafond (ceiling) is not exceeded
     */
    fun validatePlafond(
        currentEncours: Int,
        saleAmount: Int,
        plafond: Int?
    ): ValidationResult {
        if (plafond == null) {
            return ValidationResult(true) // No plafond limit
        }

        val newEncours = currentEncours + saleAmount
        if (newEncours > plafond) {
            return ValidationResult(
                false,
                listOf(
                    "Plafond dépassé. Encours actuel: $currentEncours FCFA, " +
                            "Plafond: $plafond FCFA, Montant disponible: ${plafond - currentEncours} FCFA"
                )
            )
        }

        return ValidationResult(true)
    }
}
