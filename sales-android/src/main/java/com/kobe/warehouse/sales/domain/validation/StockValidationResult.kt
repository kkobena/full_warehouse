package com.kobe.warehouse.sales.domain.validation

import com.kobe.warehouse.sales.data.model.Product

/**
 * Stock validation result types
 */
enum class StockValidationStatus {
    /** Stock is sufficient and quantity is within limits */
    VALID,

    /** Stock is insufficient but force stock is allowed */
    INSUFFICIENT_STOCK_CAN_FORCE,

    /** Stock is insufficient and force stock is NOT allowed */
    INSUFFICIENT_STOCK_BLOCKED,

    /** Quantity exceeds maximum allowed but force stock is allowed */
    QUANTITY_EXCESSIVE_CAN_FORCE,

    /** Quantity exceeds maximum allowed and force stock is NOT allowed */
    QUANTITY_EXCESSIVE_BLOCKED,

    /** Product requires deconditioning (quantity not multiple of packaging) */
    REQUIRES_DECONDITIONING
}

/**
 * Stock validation result
 * Contains validation status and relevant information for user feedback
 */
data class StockValidationResult(
    val status: StockValidationStatus,
    val product: Product,
    val requestedQuantity: Int,
    val availableStock: Int,
    val quantityInCart: Int = 0,
    val maximumAllowed: Int? = null,
    val packagingSize: Int? = null,
    val message: String = ""
) {
    /**
     * Check if validation passed (no user action required)
     */
    fun isValid(): Boolean = status == StockValidationStatus.VALID

    /**
     * Check if user confirmation is required
     */
    fun requiresConfirmation(): Boolean = when (status) {
        StockValidationStatus.INSUFFICIENT_STOCK_CAN_FORCE,
        StockValidationStatus.QUANTITY_EXCESSIVE_CAN_FORCE,
        StockValidationStatus.REQUIRES_DECONDITIONING -> true
        else -> false
    }

    /**
     * Check if operation is blocked (cannot proceed)
     */
    fun isBlocked(): Boolean = when (status) {
        StockValidationStatus.INSUFFICIENT_STOCK_BLOCKED,
        StockValidationStatus.QUANTITY_EXCESSIVE_BLOCKED -> true
        else -> false
    }

    /**
     * Get confirmation message for dialog
     */
    fun getConfirmationMessage(): String = when (status) {
        StockValidationStatus.INSUFFICIENT_STOCK_CAN_FORCE ->
            "Stock insuffisant (disponible: $availableStock, demandé: $requestedQuantity).\n\nVoulez-vous forcer le stock ?"

        StockValidationStatus.QUANTITY_EXCESSIVE_CAN_FORCE ->
            "Quantité excessive (maximum: $maximumAllowed, total: ${quantityInCart + requestedQuantity}).\n\nVoulez-vous forcer le stock ?"

        StockValidationStatus.REQUIRES_DECONDITIONING -> {
            val remainder = requestedQuantity % (packagingSize ?: 1)
            "Ce produit nécessite un déconditionnement.\n\nConditionnement: $packagingSize unités\nQuantité demandée: $requestedQuantity\nReste: $remainder unité(s)\n\nVoulez-vous déconditionner ?"
        }

        else -> message
    }

    /**
     * Get error message for blocked operations
     */
    fun getErrorMessage(): String = when (status) {
        StockValidationStatus.INSUFFICIENT_STOCK_BLOCKED ->
            "Stock insuffisant (disponible: $availableStock, demandé: $requestedQuantity). Force stock non autorisé."

        StockValidationStatus.QUANTITY_EXCESSIVE_BLOCKED ->
            "Quantité excessive (maximum: $maximumAllowed, total: ${quantityInCart + requestedQuantity}). Force stock non autorisé."

        else -> message
    }
}
