package com.kobe.warehouse.sales.domain.validator

import com.kobe.warehouse.sales.data.model.CarnetData

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
        if (carnetData.customer.id == 0) {
            errors.add("Un client avec compte carnet est obligatoire")
        }

        return ValidationResult(errors.isEmpty(), errors)
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
