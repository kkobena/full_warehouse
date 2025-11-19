# Stock Validation Implementation Summary

This document summarizes the stock validation implementation for the Android POS application, aligning with the Angular web application's business logic.

## Overview

The stock validation system validates product additions to sales, checking stock availability, quantity limits, and user permissions before adding items to the cart. This mirrors the Angular implementation documented in `PRODUCT_ADDITION_BUSINESS_RULES.md`.

---

## Implementation Components

### 1. SaleStockValidator Service
**Location**: `src/main/java/com/kobe/warehouse/sales/service/SaleStockValidator.kt`

**Purpose**: Core validation logic for stock and quantity checks

**Key Features**:
- Validates positive quantities
- Checks stock availability against requested quantity
- Enforces quantity limits (default: 1000 per product)
- Handles force stock permission (`PR_FORCE_STOCK` authority)
- Detects products requiring deconditionnement (unpacking from larger units)

**Validation Reasons**:
```kotlin
REASON_INVALID_QUANTITY              // Quantity <= 0
REASON_STOCK_INSUFFISANT             // Insufficient stock, user cannot force
REASON_FORCE_STOCK                   // Insufficient stock, user can force
REASON_FORCE_STOCK_AND_QUANTITY_EXCEEDS_MAX  // Stock + quantity limit issues
REASON_DECONDITIONNEMENT             // Need to unpack larger units
REASON_QUANTITY_EXCEEDS_MAX          // Exceeds max limit, user cannot force
```

**Usage Example**:
```kotlin
val validator = SaleStockValidator()
val result = validator.validate(
    product = product,
    quantityRequested = 10,
    currentSale = currentSale,
    canForceStock = true,
    quantityMax = 1000
)

if (result.isValid) {
    // Add product
} else {
    // Handle result.reason
}
```

---

### 2. TokenManager Extensions
**Location**: `src/main/java/com/kobe/warehouse/sales/utils/TokenManager.kt`

**New Methods**:
- `storeAuthorities(authorities: List<String>?)` - Store user permissions
- `getAuthorities(): List<String>` - Retrieve stored permissions
- `hasAuthority(authority: String): Boolean` - Check specific permission

**Purpose**: Persist user authorities for permission checks during stock validation

**Updated in AuthRepository**:
```kotlin
// After successful login, store authorities
tokenManager.storeAuthorities(account.authorities)
```

---

### 3. ComptantSaleViewModel Updates
**Location**: `src/main/java/com/kobe/warehouse/sales/ui/viewmodel/ComptantSaleViewModel.kt`

**New Dependencies**:
- `TokenManager` - For checking user authorities
- `SaleStockValidator` - For validation logic

**New LiveData**:
- `validationResult: LiveData<ValidationResult?>` - Emits validation results
- `pendingProduct: LiveData<Pair<Product, Int>?>` - Product waiting for confirmation

**Updated Methods**:
- `addProductToCart(product, quantity)` - Now validates before adding
- `confirmForceStock()` - Adds product with force stock flag after confirmation
- `cancelProductAddition()` - Cancels pending product addition

**Validation Flow**:
```kotlin
fun addProductToCart(product: Product, quantity: Int) {
    val canForceStock = tokenManager.hasAuthority("PR_FORCE_STOCK")
    val validation = stockValidator.validate(...)

    if (validation.isValid) {
        addProductDirectly(product, quantity, forceStock = false)
    } else {
        _validationResult.value = validation
        _pendingProduct.value = Pair(product, quantity)
    }
}
```

---

### 4. ValidationConfirmationDialog
**Location**: `src/main/java/com/kobe/warehouse/sales/ui/dialog/ValidationConfirmationDialog.kt`

**Purpose**: Generic confirmation dialog for validation scenarios

**Factory Methods**:
- `forceStock(message, onConfirm, onCancel)` - Force stock confirmation
- `quantityExceedsMax(message, onConfirm, onCancel)` - Quantity limit confirmation
- `error(title, message, onDismiss)` - Error message (no confirmation)

**Features**:
- Material Design 3 AlertDialog
- Customizable title, message, and button text
- Warning icon for critical confirmations
- Callback-based action handling

---

### 5. ComptantSaleActivity Updates
**Location**: `src/main/java/com/kobe/warehouse/sales/ui/activity/ComptantSaleActivity.kt`

**New Observers**:
```kotlin
viewModel.validationResult.observe(this) { validationResult ->
    validationResult?.let {
        handleValidationResult(it)
    }
}
```

**New Method**: `handleValidationResult(result: ValidationResult)`

Handles all validation scenarios:
- **Force Stock**: Shows confirmation dialog, adds product if user confirms
- **Quantity Exceeds Max**: Shows error dialog (no force option)
- **Stock Insuffisant**: Shows error dialog
- **Deconditionnement**: Shows error (TODO: implement full deconditionnement flow)
- **Invalid Quantity**: Shows toast message

**Updated Dependencies**:
- Added `TokenManager` parameter to `ComptantSaleViewModelFactory`

---

### 6. ViewModelFactory Updates
**Location**: `src/main/java/com/kobe/warehouse/sales/ui/viewmodel/ViewModelFactory.kt`

**Changes**:
- Added `TokenManager` parameter to `ComptantSaleViewModelFactory`
- Updated constructor to pass `tokenManager` to `ComptantSaleViewModel`

---

## User Permissions

### PR_FORCE_STOCK Authority

Users with `PR_FORCE_STOCK` authority can:
1. Add products even when stock is insufficient (with confirmation)
2. Override quantity limits (with confirmation)
3. See force stock confirmation dialogs instead of error messages

Users WITHOUT this authority will:
1. See error messages for insufficient stock
2. See error messages for quantity limit violations
3. Cannot proceed with product addition in these cases

**Checking Authority**:
```kotlin
val canForceStock = tokenManager.hasAuthority("PR_FORCE_STOCK")
```

---

## Validation Flow Diagram

```
User adds product to cart
    ↓
Check if user has PR_FORCE_STOCK authority
    ↓
Validate stock and quantity
    ↓
    ├─ Valid? → Add product directly
    ├─ Invalid + canForceStock?
    │   ├─ Force Stock → Show confirmation dialog
    │   ├─ Quantity Max + Force → Show confirmation dialog
    │   └─ Deconditionnement → Show deconditionnement dialog (TODO)
    └─ Invalid + !canForceStock?
        ├─ Stock Insuffisant → Show error
        └─ Quantity Max → Show error
```

---

## Testing Scenarios

### Scenario 1: Normal Addition (Stock Available)
- Product has stock: 50
- User requests: 10
- Current in cart: 0
- **Result**: Product added directly, no dialog

### Scenario 2: Force Stock (User has authority)
- Product has stock: 5
- User requests: 10
- User has PR_FORCE_STOCK
- **Result**: Confirmation dialog shown
  - If YES → Product added with `forceStock = true`
  - If NO → Product not added

### Scenario 3: Insufficient Stock (User lacks authority)
- Product has stock: 5
- User requests: 10
- User does NOT have PR_FORCE_STOCK
- **Result**: Error dialog "Stock insuffisant"

### Scenario 4: Quantity Exceeds Max (User has authority)
- User requests: 1500
- Max allowed: 1000
- User has PR_FORCE_STOCK
- **Result**: Confirmation dialog
  - If YES → Product added with override
  - If NO → Product not added

### Scenario 5: Quantity Exceeds Max (User lacks authority)
- User requests: 1500
- Max allowed: 1000
- User does NOT have PR_FORCE_STOCK
- **Result**: Error dialog "La quantité demandée dépasse le maximum autorisé"

---

## Known Limitations and TODOs

1. **Deconditionnement Not Fully Implemented**:
   - Currently shows error dialog for products with `deconditionnable = true`
   - TODO: Implement parent product lookup and unpacking logic
   - TODO: Create dialog to suggest using larger package

2. **Quantity Update Validation**:
   - `updateProductQuantity()` method has TODO for validation
   - Currently updates directly without validation
   - Should use `validateUpdate()` method from SaleStockValidator

3. **Backend Error Handling**:
   - Need to handle backend stock errors (errorKey: "stock", "stockChInsufisant")
   - Angular implementation has detailed backend error handling
   - TODO: Implement in repository layer

4. **Product Parent Relationship**:
   - Angular uses `parentId` field for deconditionnement
   - Android Product model has `deconditionnable` boolean
   - May need to add `parentId` field to Product model for full deconditionnement support

---

## Files Created

1. `SaleStockValidator.kt` - Stock validation service
2. `ValidationConfirmationDialog.kt` - Confirmation dialog
3. `PRODUCT_ADDITION_BUSINESS_RULES.md` - Business rules documentation
4. `STOCK_VALIDATION_IMPLEMENTATION.md` - This summary

## Files Modified

1. `TokenManager.kt` - Added authority storage methods
2. `AuthRepository.kt` - Store authorities after login
3. `ComptantSaleViewModel.kt` - Integrated validation logic
4. `ViewModelFactory.kt` - Added TokenManager parameter
5. `ComptantSaleActivity.kt` - Added validation observers and dialog handling

---

## Next Steps

1. **Test the implementation**:
   - Test with user that has PR_FORCE_STOCK authority
   - Test with user that does NOT have PR_FORCE_STOCK authority
   - Test all validation scenarios (see Testing Scenarios above)

2. **Implement Deconditionnement**:
   - Create dialog for suggesting parent product
   - Load parent product from backend
   - Add parent product to cart instead of child

3. **Add Validation to Quantity Updates**:
   - Update `updateProductQuantity()` to use validation
   - Show confirmation dialogs when updating quantity

4. **Backend Error Handling**:
   - Handle stock error responses from backend
   - Map backend error keys to validation reasons
   - Show appropriate dialogs for backend errors

5. **Add Unit Tests**:
   - Test SaleStockValidator with various scenarios
   - Test ViewModel validation flow
   - Test dialog interactions

---

## References

- Angular Implementation: `src/main/webapp/app/entities/sales/comptant-home/comptant-home.component.ts`
- Angular Validator: `src/main/webapp/app/entities/sales/validator/sale-stock-validator.service.ts`
- Business Rules: `PRODUCT_ADDITION_BUSINESS_RULES.md`
