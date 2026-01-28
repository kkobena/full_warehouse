package com.kobe.warehouse.sales.domain.service

import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine

/**
 * Déconditionnement Service
 * Business logic for product déconditionnement (breaking boxes into individual units)
 *
 * Déconditionnement is the process of breaking a box (parent product) into
 * individual units (detail/child product) during a sale. This is useful when:
 * - Stock detail is insufficient to fulfill a sale
 * - Boxes are available in stock
 * - Product is marked as "deconditionnable"
 *
 * Process:
 * 1. Check if product is deconditionnable
 * 2. Check if sufficient box stock is available
 * 3. Calculate quantity of boxes needed
 * 4. Add sale line for box quantity (negative to reduce stock)
 * 5. Add sale line for detail quantity (positive to increase detail stock)
 *
 * Example:
 * - Product: Paracetamol 500mg (10 tablets per box)
 * - Stock box: 5 boxes
 * - Stock detail: 3 tablets
 * - Customer wants: 15 tablets
 * - Solution: Decondition 2 boxes → 20 tablets + 3 existing = 23 tablets available
 */
class DeconditionnementService {

    /**
     * Check if product can be deconditioned
     *
     * A product can be deconditioned if:
     * - It has the "deconditionnable" flag set to true
     * - It has an itemQty > 1 (multiple units per box)
     * - Box stock is available
     */
    fun canDecondition(product: Product): Boolean {
        return product.deconditionnable == true &&
                (product.itemQty ?: 1) > 1 &&
                getAvailableBoxes(product) > 0
    }

    /**
     * Check if product should auto-trigger déconditionnement
     *
     * Auto-trigger when:
     * - Product is deconditionnable
     * - Detail stock is below threshold (seuilDeconditionnement)
     * - Box stock is available
     */
    fun shouldAutoDecondition(product: Product, requestedQuantity: Int): Boolean {
        if (!canDecondition(product)) {
            return false
        }

        val detailStock = getDetailStock(product)
        val threshold = product.seuilDeconditionnement ?: 0

        // Auto-trigger if detail stock is below threshold OR insufficient for request
        return detailStock < threshold || detailStock < requestedQuantity
    }

    /**
     * Calculate number of boxes needed to fulfill requested quantity
     *
     * @param product The product
     * @param requestedQuantity Quantity requested by customer
     * @return Number of boxes to décondition
     */
    fun calculateBoxesNeeded(product: Product, requestedQuantity: Int): Int {
        val itemQty = product.itemQty ?: 1
        val detailStock = getDetailStock(product)
        val shortage = requestedQuantity - detailStock

        if (shortage <= 0) {
            return 0 // No déconditionnement needed
        }

        // Calculate boxes needed (round up)
        return (shortage + itemQty - 1) / itemQty
    }

    /**
     * Get number of available boxes in stock
     */
    fun getAvailableBoxes(product: Product): Int {
        val itemQty = product.itemQty ?: 1
        return product.totalQuantity / itemQty
    }

    /**
     * Get number of detail units in stock
     */
    fun getDetailStock(product: Product): Int {
        val itemQty = product.itemQty ?: 1
        return product.totalQuantity % itemQty
    }

    /**
     * Calculate total detail units after déconditionnement
     *
     * @param product The product
     * @param boxQuantity Number of boxes to décondition
     * @return Total detail units available after déconditionnement
     */
    fun calculateDetailAfterDecondition(product: Product, boxQuantity: Int): Int {
        val itemQty = product.itemQty ?: 1
        val currentDetail = getDetailStock(product)
        val newDetailUnits = boxQuantity * itemQty

        return currentDetail + newDetailUnits
    }

    /**
     * Validate déconditionnement request
     *
     * @param product The product
     * @param boxQuantity Number of boxes to décondition
     * @return Result with error message if invalid, or success if valid
     */
    fun validateDeconditionRequest(product: Product, boxQuantity: Int): Result<Unit> {
        // Check if product is deconditionnable
        if (product.deconditionnable != true) {
            return Result.failure(
                Exception("Ce produit ne peut pas être déconditionné")
            )
        }

        // Check item qty
        val itemQty = product.itemQty ?: 1
        if (itemQty <= 1) {
            return Result.failure(
                Exception("Ce produit n'a pas d'unités individuelles")
            )
        }

        // Check box quantity
        if (boxQuantity <= 0) {
            return Result.failure(
                Exception("La quantité doit être supérieure à 0")
            )
        }

        // Check available boxes
        val availableBoxes = getAvailableBoxes(product)
        if (boxQuantity > availableBoxes) {
            return Result.failure(
                Exception("Stock insuffisant. Seulement $availableBoxes boîte(s) disponible(s)")
            )
        }

        return Result.success(Unit)
    }

    /**
     * Apply déconditionnement to sale
     *
     * This method adds the necessary sale lines to perform déconditionnement:
     * 1. Remove boxes from stock (negative quantity)
     * 2. Add detail units to stock (positive quantity)
     *
     * Note: In the actual implementation, you would typically:
     * - Call backend API to perform déconditionnement
     * - Backend handles stock updates and creates déconditionnement record
     * - Return updated sale with new sale lines
     *
     * For now, this is a helper method to prepare the data structure
     *
     * @param sale Current sale
     * @param product Product to décondition
     * @param boxQuantity Number of boxes to décondition
     * @return Updated sale with déconditionnement sale lines
     */
    fun applyDeconditionnementToSale(
        sale: Sale,
        product: Product,
        boxQuantity: Int
    ): Result<Sale> {
        // Validate first
        val validation = validateDeconditionRequest(product, boxQuantity)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        // In real implementation, this would call backend API
        // Backend endpoint: POST /api/sales/add-item/comptant?deconditionner=true
        //
        // Backend would:
        // 1. Create déconditionnement record
        // 2. Update product stock (reduce boxes, increase detail)
        // 3. Add sale lines to the sale
        // 4. Return updated sale
        //
        // For now, return success indicating the request is valid
        return Result.success(sale)
    }

    /**
     * Get formatted déconditionnement information for display
     */
    fun getDeconditionInfo(product: Product, boxQuantity: Int): DeconditionInfo {
        val itemQty = product.itemQty ?: 1
        val totalDetailUnits = boxQuantity * itemQty
        val availableBoxes = getAvailableBoxes(product)
        val currentDetail = getDetailStock(product)
        val newDetail = currentDetail + totalDetailUnits

        return DeconditionInfo(
            productName = product.libelle ?: product.code ?: "",
            boxQuantity = boxQuantity,
            detailUnitsPerBox = itemQty,
            totalDetailUnits = totalDetailUnits,
            currentBoxStock = availableBoxes,
            currentDetailStock = currentDetail,
            newDetailStock = newDetail
        )
    }
}

/**
 * Déconditionnement information for display
 */
data class DeconditionInfo(
    val productName: String,
    val boxQuantity: Int,
    val detailUnitsPerBox: Int,
    val totalDetailUnits: Int,
    val currentBoxStock: Int,
    val currentDetailStock: Int,
    val newDetailStock: Int
) {
    fun getFormattedSummary(): String {
        return "$boxQuantity boîte(s) × $detailUnitsPerBox unités = $totalDetailUnits unité(s)"
    }

    fun getFormattedStockChange(): String {
        return "Stock détail: $currentDetailStock → $newDetailStock unités"
    }
}
