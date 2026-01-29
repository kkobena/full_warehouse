package com.kobe.warehouse.sales.domain.validator

import com.kobe.warehouse.sales.domain.model.CarnetData

/**
 * Validator for Carnet (Credit) Sales
 *
 * Validates carnet data and business rules for credit sales
 */
class CarnetValidator {

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
     * Validate carnet data
     */
    fun validateCarnetData(carnetData: CarnetData?): ValidationResult {
        if (carnetData == null) {
            return ValidationResult(false, listOf("Les données carnet sont obligatoires"))
        }

        val errors = mutableListOf<String>()

        // Validate credit limit
        if (carnetData.limiteCredit <= 0) {
            errors.add("La limite de crédit doit être supérieure à 0")
        }

        // Validate encours
        if (carnetData.encours < 0) {
            errors.add("L'encours ne peut pas être négatif")
        }

        // Check if customer has reached credit limit
        if (carnetData.hasReachedCreditLimit()) {
            errors.add(
                "Le client a atteint sa limite de crédit. " +
                        "Encours: ${carnetData.encours} FCFA, Limite: ${carnetData.limiteCredit} FCFA"
            )
        }

        // Validate customer
        if (carnetData.customer.id == 0L) {
            errors.add("Un client avec compte carnet est obligatoire")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }



    /**
     * Validate customer has carnet account
     */
    fun validateCustomerHasCarnet(customerId: Long?): ValidationResult {
        if (customerId == null) {
            return ValidationResult(
                false,
                listOf("Un client avec compte carnet est obligatoire pour une vente carnet")
            )
        }
        return ValidationResult(true)
    }

    /**
     * Validate credit usage
     */
    fun validateCreditUsage(carnetData: CarnetData): ValidationResult {
        val errors = mutableListOf<String>()

        val usagePercentage = carnetData.getCreditUsagePercentage()

        // Warn if credit usage is above 90%
        if (usagePercentage >= 90) {
            errors.add(
                "Attention: le client utilise $usagePercentage% de sa limite de crédit " +
                        "(${carnetData.encours}/${carnetData.limiteCredit} FCFA)"
            )
        }

        // Hard limit at 100%
        if (usagePercentage >= 100) {
            return ValidationResult(
                false,
                listOf("Limite de crédit atteinte (${carnetData.encours}/${carnetData.limiteCredit} FCFA)")
            )
        }

        // Return true even with warnings (errors list contains warnings)
        return ValidationResult(true, errors)
    }

    /**
     * Validate that no payments are required (credit sale = deferred payment)
     */
    fun validateNoPaymentRequired(): ValidationResult {
        // Carnet sales should not have immediate payments
        // Payment is deferred and added to customer's carnet balance
        return ValidationResult(true)
    }

    /**
     * Get suggested maximum sale amount based on credit available
     */
    fun getSuggestedMaxAmount(carnetData: CarnetData?): Int {
        return carnetData?.getMaxSaleAmount() ?: 0
    }
}
