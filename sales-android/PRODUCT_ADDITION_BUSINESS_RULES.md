# Product Addition Business Rules - Android Implementation

This document outlines the business logic for adding products to sales in the Android POS app, based on the Angular web application implementation.

## Source Reference

**Angular Component**: `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\comptant-home\comptant-home.component.ts`

**Stock Validator**: `C:\Users\k.kobena\Documents\full_warehouse\src\main\webapp\app\entities\sales\validator\sale-stock-validator.service.ts`

---

## Product Selection Flow

### 1. Product Search
- User can search products by:
  - **Product name** (minimum 3 characters)
  - **Product code** (barcode scanning or manual entry)
- Display maximum `PRODUIT_COMBO_RESULT_SIZE` results
- Show product with:
  - Name (`libelle`)
  - Regular unit price (`regularUnitPrice`)
  - Current stock (`totalQuantity`)

### 2. Product Selection
```kotlin
// Angular: onSelectProduct(selectedProduit?: ProduitSearch)
fun onSelectProduct(product: Product) {
    this.selectedProduct = product
    // Reset quantity to 1
    this.quantity = 1
    // Focus on quantity input
    focusQuantityInput()
    // Update stock severity indicator
    if (product.totalQuantity > 0) {
        stockSeverity = "success" // Green
    } else {
        stockSeverity = "danger"  // Red
    }
}
```

---

## Stock Validation Logic

### Validation Method
```kotlin
// Angular: SaleStockValidator.validate()
data class ValidationResult(
    val isValid: Boolean,
    val reason: String? = null
)

fun validateStock(
    product: Product,
    quantityRequested: Int,
    totalQuantityInCart: Int,  // Existing quantity in cart for this product
    canForceStock: Boolean,    // User has PR_FORCE_STOCK authority
    quantityMax: Int           // Maximum quantity allowed per product
): ValidationResult {

    // 1. Check quantity is positive
    if (quantityRequested <= 0) {
        return ValidationResult(false, "invalidQuantity")
    }

    val totalQtyAfterAdd = totalQuantityInCart + quantityRequested

    // 2. Check stock availability
    if (product.totalQuantity < totalQtyAfterAdd) {
        if (canForceStock) {
            // User can force stock, but check limits
            if (quantityRequested > quantityMax) {
                return ValidationResult(false, "forceStockAndQuantityExceedsMax")
            }
            // Check if product needs deconditionnement (unpacking)
            if (product.parentId != null) {
                return ValidationResult(false, "deconditionnement")
            }
            return ValidationResult(false, "forceStock")
        }
        // User cannot force stock
        return ValidationResult(false, "stockInsuffisant")
    }

    // 3. Check maximum quantity limit
    if (quantityRequested >= quantityMax) {
        if (canForceStock) {
            return ValidationResult(false, "forceStockAndQuantityExceedsMax")
        }
        return ValidationResult(false, "quantityExceedsMax")
    }

    // All validations passed
    return ValidationResult(true)
}
```

---

## Handling Validation Results

### Angular Implementation Flow
```typescript
// Angular: onAddNewQty() and handleInvalidStock()
when (validationResult.reason) {
    "forceStockAndQuantityExceedsMax" -> {
        // Show confirmation dialog
        // Message: "La quantité demandée dépasse le maximum autorisé. Continuer ?"
        // If YES -> add product anyway (force stock)
        // If NO -> reset quantity to 1
    }

    "deconditionnement" -> {
        // Handle unpacking of larger units
        // Show confirmation dialog
        // Load parent product (larger package)
        // If parent has stock -> suggest using parent
        // If YES -> add parent product instead
        // If NO -> cancel
    }

    "forceStock" -> {
        // Show confirmation dialog
        // Message: "La quantité demandée est supérieure au stock. Continuer ?"
        // If YES -> add product with forceStock flag = true
        // If NO -> reset quantity to 1
    }

    "stockInsuffisant" -> {
        // Show error message
        // Message: "Stock insuffisant"
        // Do NOT add product
    }

    "quantityExceedsMax" -> {
        // Show error message
        // Message: "La quantité demandée dépasse le maximum autorisé"
        // Do NOT add product
    }
}
```

---

## Creating Sales Line

### Angular Implementation
```kotlin
// Angular: createSalesLine(produit, quantityRequested)
data class SalesLine(
     val id: Int? = null,
    val produitId: Long,
    val regularUnitPrice: Int,
    val saleId: Long?,
    val quantitySold: Int,      // Actual quantity from stock
    val quantityRequested: Int, // Quantity user wants
    val sales: Sale?,
    val forceStock: Boolean = false
)

fun createSalesLine(product: Product, quantityRequested: Int): SalesLine {
    // Calculate quantity sold (cannot exceed available stock unless forcing)
    val quantitySold = min(product.totalQuantity, quantityRequested)

    return SalesLine(
       id=product.id,
        produitId = product.id,
        regularUnitPrice = product.regularUnitPrice,
        saleId = currentSale?.id,
        quantitySold = if (quantitySold > 0) quantitySold else 0,
        quantityRequested = quantityRequested,
        sales = currentSale,
        forceStock = false  // Set to true only after user confirms
    )
}
```

---

## Adding Product to Cart

### Flow
```kotlin
// Angular: onAddProduit()
fun onAddProduct(salesLine: SalesLine) {
    if (currentSale == null) {
        // Create new sale first
        createNewSale(salesLine)
    } else {
        // Add to existing sale
        addProductToSale(salesLine)
    }
}

// After successful add:
fun onProductAddedSuccessfully(product: Product, quantity: Int) {
    // 1. Update customer display (if available)
    updateCustomerDisplay(
        productName = product.libelle,
        quantity = quantity,
        price = product.regularUnitPrice
    )

    // 2. Reset product selection
    selectedProduct = null
    quantityInput = 1

    // 3. Focus back on product search
    focusProductSearch()
}
```

---

## Stock Error Handling

### Backend Error Responses
```kotlin
// Angular: onStockError()
when (error.errorKey) {
    "stock" -> {
        // Stock insufficient for requested quantity
        if (canForceStock) {
            // Show confirmation dialog to force stock
            salesLine.forceStock = true
            showForceStockDialog(salesLine)
        } else {
            showError("Stock insuffisant")
            reloadSale()  // Refresh to get updated data
        }
    }

    "stockChInsufisant" -> {
        // Stock of child product insufficient
        // Need to check parent product (larger package)
        val parentProductId = error.payload
        loadParentProduct(parentProductId) { parentProduct ->
            if (parentProduct.totalQuantity > 0) {
                showDeconditionnementDialog(
                    salesLine,
                    parentProduct
                )
            } else {
                showError("Stock insuffisant")
            }
        }
    }
}
```

---

## Key Permissions

### Authority Constants
```kotlin
const val PR_FORCE_STOCK = "PR_FORCE_STOCK"
// Allows user to add products even when stock is insufficient
```

---

## Android Implementation Checklist

### Required Components

#### 1. Stock Validator Service
```kotlin
class SaleStockValidator {
    fun validate(
        product: Product,
        quantityRequested: Int,
        totalQuantityInCart: Int,
        canForceStock: Boolean,
        quantityMax: Int
    ): ValidationResult
}
```

#### 2. Deconditionnement Service
```kotlin
class DeconditionnementService {
    fun handleDeconditionnement(
        quantityRequested: Int,
        product: Product,
        onConfirm: (parentProduct: Product) -> Unit,
        onCancel: () -> Unit
    )
}
```

#### 3. Force Stock Service
```kotlin
class ForceStockService {
    fun handleForceStock(
        quantityRequested: Int,
        message: String,
        onConfirm: (quantity: Int) -> Unit,
        onCancel: () -> Unit
    )
}
```

#### 4. Update ComptantSaleViewModel
- Add stock validation before adding products
- Handle validation results
- Show appropriate dialogs
- Make API calls with proper error handling

#### 5. Update ComptantSaleActivity
- Wire up validation flow
- Create confirmation dialogs
- Handle stock errors from backend
- Update UI based on validation results

---

## Constants

```kotlin
// From Angular pagination constants
const val PRODUIT_COMBO_MIN_LENGTH = 3  // Minimum search length
const val PRODUIT_COMBO_RESULT_SIZE = 10 // Max search results

// Quantity limits (may vary by configuration)
const val DEFAULT_QUANTITY_MAX = 1000  // Default max quantity per product
```

---

## Error Messages (French)

```kotlin
val errorMessages = mapOf(
    "stockInsuffisant" to "Stock insuffisant",
    "quantityGreatherThanStock" to "La quantité demandée est supérieure au stock. Continuer ?",
    "quantityGreatherMax" to "La quantité demandée dépasse le maximum autorisé",
    "quantityGreatherMaxCanContinue" to "La quantité demandée dépasse le maximum autorisé. Continuer ?"
)
```

---

## Summary

The Android implementation must follow this exact business logic:

1. **Validate stock** before adding any product
2. **Handle special cases**: force stock, deconditionnement, quantity limits
3. **Show appropriate dialogs** for user confirmation
4. **Handle backend errors** gracefully
5. **Update customer display** after successful additions
6. **Reset UI state** after each operation

This ensures consistency between web and mobile POS experiences.
