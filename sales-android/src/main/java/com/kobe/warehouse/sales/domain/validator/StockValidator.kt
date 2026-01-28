package com.kobe.warehouse.sales.domain.validator

import com.kobe.warehouse.sales.data.model.Product

/**
 * Stock Validator
 * Business logic for validating stock availability and force stock conditions
 *
 * This validator handles:
 * - Stock availability checks
 * - Force stock validation
 * - Stock shortage calculations
 * - Stock warnings and alerts
 */
class StockValidator {

    /**
     * Stock validation result
     */
    sealed class StockValidationResult {
        /**
         * Stock is sufficient for the requested quantity
         */
        data class Sufficient(val availableStock: Int) : StockValidationResult()

        /**
         * Stock is insufficient, but product allows force stock
         */
        data class InsufficientButForceable(
            val availableStock: Int,
            val requestedQuantity: Int,
            val shortage: Int
        ) : StockValidationResult()

        /**
         * Stock is insufficient and cannot be forced
         */
        data class InsufficientNotForceable(
            val availableStock: Int,
            val requestedQuantity: Int,
            val shortage: Int
        ) : StockValidationResult()

        /**
         * Product is out of stock
         */
        data class OutOfStock(val productName: String) : StockValidationResult()
    }

    /**
     * Validate stock availability for a product
     *
     * @param product The product to validate
     * @param requestedQuantity Quantity requested by user
     * @return StockValidationResult indicating the validation outcome
     */
    fun validateStock(product: Product, requestedQuantity: Int): StockValidationResult {
        val availableStock = product.totalQuantity

        // Out of stock
        if (availableStock == 0) {
            return StockValidationResult.OutOfStock(
                productName = product.libelle ?: product.code ?: "Produit"
            )
        }

        // Sufficient stock
        if (availableStock >= requestedQuantity) {
            return StockValidationResult.Sufficient(availableStock)
        }

        // Insufficient stock
        val shortage = requestedQuantity - availableStock

        return if (product.forceStock) {
            // Product allows force stock (auto-approved)
            StockValidationResult.InsufficientButForceable(
                availableStock = availableStock,
                requestedQuantity = requestedQuantity,
                shortage = shortage
            )
        } else {
            // Product does NOT allow force stock (requires authorization)
            StockValidationResult.InsufficientNotForceable(
                availableStock = availableStock,
                requestedQuantity = requestedQuantity,
                shortage = shortage
            )
        }
    }

    /**
     * Check if product requires force stock confirmation
     *
     * Returns true if:
     * - Stock is insufficient
     * - Product does NOT have forceStock=true
     * - User authorization is required
     */
    fun requiresForceStockConfirmation(product: Product, requestedQuantity: Int): Boolean {
        val result = validateStock(product, requestedQuantity)
        return result is StockValidationResult.InsufficientNotForceable
    }

    /**
     * Check if product can be added despite insufficient stock
     *
     * Returns true if:
     * - Product has forceStock=true (auto-approved)
     * OR
     * - User has PR_FORCE_STOCK authority and provides a reason
     */
    fun canForceStock(product: Product): Boolean {
        return product.forceStock
    }

    /**
     * Calculate maximum quantity that can be added without forcing stock
     */
    fun getMaxQuantityWithoutForce(product: Product): Int {
        return product.totalQuantity
    }

    /**
     * Get formatted error message for insufficient stock
     */
    fun getInsufficientStockMessage(
        product: Product,
        requestedQuantity: Int
    ): String {
        val availableStock = product.totalQuantity
        val shortage = requestedQuantity - availableStock
        val productName = product.libelle ?: product.code ?: "ce produit"

        return buildString {
            append("Stock insuffisant pour $productName.\n")
            append("Disponible: $availableStock\n")
            append("Demandé: $requestedQuantity\n")
            append("Manque: $shortage")
        }
    }

    /**
     * Get formatted warning message when stock is low
     *
     * @param threshold Percentage threshold for low stock warning (e.g., 0.2 = 20%)
     */
    fun getLowStockWarning(product: Product, threshold: Double = 0.2): String? {
        val totalStock = product.totalQuantity

        // Define what "low" means (you can adjust this logic)
        // For now, we'll consider stock low if it's below a certain absolute value
        // In a real scenario, you'd compare against reorder level or similar
        val lowStockThreshold = 10 // Example: less than 10 units

        return if (totalStock > 0 && totalStock <= lowStockThreshold) {
            "Attention: Stock faible ($totalStock unité(s) restante(s))"
        } else {
            null
        }
    }

    /**
     * Check if product is eligible for déconditionnement instead of force stock
     *
     * This is useful when:
     * - Detail stock is insufficient
     * - Product is deconditionnable
     * - Box stock is available
     */
    fun shouldSuggestDeconditionnement(product: Product, requestedQuantity: Int): Boolean {
        if (product.deconditionnable != true) {
            return false
        }

        val itemQty = product.itemQty ?: 1
        if (itemQty <= 1) {
            return false
        }

        // Calculate detail stock and box stock
        val detailStock = product.totalQuantity % itemQty
        val boxStock = product.totalQuantity / itemQty

        // Suggest déconditionnement if:
        // - Detail stock is insufficient for request
        // - But box stock is available
        return detailStock < requestedQuantity && boxStock > 0
    }

    /**
     * Audit log information for force stock action
     */
    data class ForceStockAudit(
        val productId: Long,
        val productName: String,
        val requestedQuantity: Int,
        val availableStock: Int,
        val shortage: Int,
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun toLogString(): String {
            return buildString {
                append("FORCE_STOCK - ")
                append("Product: $productName ($productId), ")
                append("Requested: $requestedQuantity, ")
                append("Available: $availableStock, ")
                append("Shortage: $shortage, ")
                append("Reason: $reason, ")
                append("Timestamp: $timestamp")
            }
        }
    }

    /**
     * Create audit log for force stock action
     *
     * This should be sent to backend for persistent logging
     */
    fun createForceStockAudit(
        product: Product,
        requestedQuantity: Int,
        reason: String
    ): ForceStockAudit {
        val availableStock = product.totalQuantity
        val shortage = requestedQuantity - availableStock

        return ForceStockAudit(
            productId = product.id,
            productName = product.libelle ?: product.code ?: "Unknown",
            requestedQuantity = requestedQuantity,
            availableStock = availableStock,
            shortage = shortage,
            reason = reason
        )
    }
}
