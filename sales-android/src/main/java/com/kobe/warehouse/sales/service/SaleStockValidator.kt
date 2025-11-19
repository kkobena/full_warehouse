package com.kobe.warehouse.sales.service

import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale

/**
 * Stock Validator Service
 * Validates stock availability and quantity limits when adding products to sales
 * Mirrors the Angular SaleStockValidator business logic
 *
 * @see PRODUCT_ADDITION_BUSINESS_RULES.md for complete business rules
 */
class SaleStockValidator {

    companion object {
        // Default maximum quantity per product
        const val DEFAULT_QUANTITY_MAX = 1000

        // Validation reason codes
        const val REASON_INVALID_QUANTITY = "invalidQuantity"
        const val REASON_STOCK_INSUFFISANT = "stockInsuffisant"
        const val REASON_FORCE_STOCK = "forceStock"
        const val REASON_FORCE_STOCK_AND_QUANTITY_EXCEEDS_MAX = "forceStockAndQuantityExceedsMax"
        const val REASON_DECONDITIONNEMENT = "deconditionnement"
        const val REASON_QUANTITY_EXCEEDS_MAX = "quantityExceedsMax"
    }

    /**
     * Validation Result
     * Contains validation status and reason code for handling
     */
    data class ValidationResult(
        val isValid: Boolean,
        val reason: String? = null,
        val message: String? = null
    )

    /**
     * Validate stock availability and quantity limits
     *
     * @param product Product to add
     * @param quantityRequested Quantity user wants to add
     * @param currentSale Current sale/cart state
     * @param canForceStock User has PR_FORCE_STOCK authority
     * @param quantityMax Maximum quantity allowed per product (default: 1000)
     * @return ValidationResult with isValid flag and reason code
     */
    fun validate(
        product: Product,
        quantityRequested: Int,
        currentSale: Sale?,
        canForceStock: Boolean,
        quantityMax: Int = DEFAULT_QUANTITY_MAX
    ): ValidationResult {

        // 1. Check quantity is positive
        if (quantityRequested <= 0) {
            return ValidationResult(
                isValid = false,
                reason = REASON_INVALID_QUANTITY,
                message = "La quantité doit être supérieure à zéro"
            )
        }

        // 2. Calculate total quantity in cart for this product
        val totalQuantityInCart = currentSale?.salesLines
            ?.filter { it.id == product.id }
            ?.sumOf { it.quantityRequested }
            ?: 0

        val totalQtyAfterAdd = totalQuantityInCart + quantityRequested

        // 3. Check stock availability
        if (product.totalQuantity < totalQtyAfterAdd) {
            if (canForceStock) {
                // User can force stock, but check limits
                if (quantityRequested > quantityMax) {
                    return ValidationResult(
                        isValid = false,
                        reason = REASON_FORCE_STOCK_AND_QUANTITY_EXCEEDS_MAX,
                        message = "La quantité demandée dépasse le maximum autorisé. Continuer ?"
                    )
                }

                // Check if product needs deconditionnement (unpacking)
                // Product with parentId means it's a child product that can be unpacked from parent
                if (product.deconditionnable) {
                    return ValidationResult(
                        isValid = false,
                        reason = REASON_DECONDITIONNEMENT,
                        message = "Stock insuffisant. Utiliser le conditionnement supérieur ?"
                    )
                }

                return ValidationResult(
                    isValid = false,
                    reason = REASON_FORCE_STOCK,
                    message = "La quantité demandée est supérieure au stock. Continuer ?"
                )
            }

            // User cannot force stock
            return ValidationResult(
                isValid = false,
                reason = REASON_STOCK_INSUFFISANT,
                message = "Stock insuffisant"
            )
        }

        // 4. Check maximum quantity limit
        if (quantityRequested >= quantityMax) {
            if (canForceStock) {
                return ValidationResult(
                    isValid = false,
                    reason = REASON_FORCE_STOCK_AND_QUANTITY_EXCEEDS_MAX,
                    message = "La quantité demandée dépasse le maximum autorisé. Continuer ?"
                )
            }

            return ValidationResult(
                isValid = false,
                reason = REASON_QUANTITY_EXCEEDS_MAX,
                message = "La quantité demandée dépasse le maximum autorisé"
            )
        }

        // All validations passed
        return ValidationResult(isValid = true)
    }

    /**
     * Validate before updating quantity in cart
     *
     * @param product Product being updated
     * @param newQuantity New quantity requested
     * @param currentQuantityInLine Current quantity in the specific line
     * @param currentSale Current sale state
     * @param canForceStock User has force stock authority
     * @param quantityMax Maximum quantity allowed
     * @return ValidationResult
     */
    fun validateUpdate(
        product: Product,
        newQuantity: Int,
        currentQuantityInLine: Int,
        currentSale: Sale?,
        canForceStock: Boolean,
        quantityMax: Int = DEFAULT_QUANTITY_MAX
    ): ValidationResult {

        if (newQuantity <= 0) {
            return ValidationResult(
                isValid = false,
                reason = REASON_INVALID_QUANTITY,
                message = "La quantité doit être supérieure à zéro"
            )
        }

        // Calculate total quantity in cart excluding current line
        val totalQuantityInOtherLines = currentSale?.salesLines
            ?.filter { it.id == product.id }
            ?.filter { it.quantityRequested != currentQuantityInLine } // Exclude current line
            ?.sumOf { it.quantityRequested }
            ?: 0

        val totalQtyAfterUpdate = totalQuantityInOtherLines + newQuantity

        // Check stock availability
        if (product.totalQuantity < totalQtyAfterUpdate) {
            if (canForceStock) {
                if (newQuantity > quantityMax) {
                    return ValidationResult(
                        isValid = false,
                        reason = REASON_FORCE_STOCK_AND_QUANTITY_EXCEEDS_MAX,
                        message = "La quantité demandée dépasse le maximum autorisé. Continuer ?"
                    )
                }

                return ValidationResult(
                    isValid = false,
                    reason = REASON_FORCE_STOCK,
                    message = "La quantité demandée est supérieure au stock. Continuer ?"
                )
            }

            return ValidationResult(
                isValid = false,
                reason = REASON_STOCK_INSUFFISANT,
                message = "Stock insuffisant"
            )
        }

        // Check maximum quantity limit
        if (newQuantity >= quantityMax) {
            if (canForceStock) {
                return ValidationResult(
                    isValid = false,
                    reason = REASON_FORCE_STOCK_AND_QUANTITY_EXCEEDS_MAX,
                    message = "La quantité demandée dépasse le maximum autorisé. Continuer ?"
                )
            }

            return ValidationResult(
                isValid = false,
                reason = REASON_QUANTITY_EXCEEDS_MAX,
                message = "La quantité demandée dépasse le maximum autorisé"
            )
        }

        return ValidationResult(isValid = true)
    }
}
