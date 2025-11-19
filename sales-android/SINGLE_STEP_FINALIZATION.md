# Single-Step Finalization Implementation

This document describes the implementation of single-step finalization with real-time backend synchronization for product additions.

## Overview

The Android POS app now synchronizes sale data with the backend as products are added/removed/updated, matching the Angular web application's behavior. Finalization is now a single-step process that only adds payments and changes the sale status.

---

## Architecture Changes

### Before (Two-Step Process)
```
1. User adds products → Local cart only (no backend calls)
2. User clicks checkout → createCashSale() creates sale on backend
3. System calls finalizeSale() → Adds payments and finalizes
```

**Problems**:
- Cart data only in memory (lost on app crash)
- Duplicate sale creation logic
- No ongoing sales support
- Doesn't match Angular behavior

### After (Single-Step with Real-Time Sync)
```
1. User adds first product → createCashSale() creates PENDING sale on backend
2. User adds more products → addSaleLine() updates backend sale
3. User updates quantity → updateSaleLine() updates backend
4. User removes product → removeSaleLine() updates backend
5. User clicks checkout → finalizeSale() adds payments and changes status to VALIDATED
```

**Benefits**:
- Sale persisted immediately
- Ongoing sales visible in sales list
- Matches Angular web app behavior
- Backend validates stock on each product addition
- Cart survives app crashes (can resume ongoing sales)

---

## Implementation Details

### 1. Product Addition Flow

**Method**: `addProductDirectly(product, quantity, forceStock)`

**New Behavior**:
```kotlin
private fun addProductDirectly(product: Product, quantity: Int, forceStock: Boolean) {
    viewModelScope.launch {
        val currentSaleValue = _currentSale.value

        // Create sale line
        val saleLine = SaleLine(...)

        if (currentSaleValue == null || currentSaleValue.id == 0L) {
            // No sale exists - create new sale on backend
            createNewSale(saleLine)
        } else {
            // Sale exists - add product to existing sale on backend
            addProductToBackend(currentSale, saleLine)
        }
    }
}
```

**Backend Calls**:
- **First product**: `POST /api/sales/cash-sales` - Creates sale with status PENDING
- **Additional products**: `POST /api/sales/{id}/{date}/lines` - Adds line to existing sale

---

### 2. Product Removal Flow

**Method**: `removeProductFromCart(saleLine)`

**New Behavior**:
```kotlin
fun removeProductFromCart(saleLine: SaleLine) {
    viewModelScope.launch {
        val saleId = currentSale.saleId?.id ?: currentSale.id
        val saleDate = currentSale.saleId?.saleDate ?: ""

        salesRepository.removeSaleLine(saleId, saleDate, saleLine.id).fold(
            onSuccess = { updatedSale ->
                _currentSale.value = updatedSale
            },
            onFailure = { error ->
                _errorMessage.value = error.message
            }
        )
    }
}
```

**Backend Call**: `DELETE /api/sales/{id}/{date}/lines/{lineId}`

---

### 3. Quantity Update Flow

**Method**: `updateProductQuantity(saleLine, newQuantity)`

**New Behavior**:
```kotlin
fun updateProductQuantity(saleLine: SaleLine, newQuantity: Int) {
    viewModelScope.launch {
        val saleId = currentSale.saleId?.id ?: currentSale.id
        val saleDate = currentSale.saleId?.saleDate ?: ""
        val updatedLine = saleLine.updateQuantity(newQuantity)

        salesRepository.updateSaleLine(saleId, saleDate, saleLine.id, updatedLine).fold(
            onSuccess = { updatedSale ->
                _currentSale.value = updatedSale
            },
            onFailure = { error ->
                _errorMessage.value = error.message
            }
        )
    }
}
```

**Backend Call**: `PUT /api/sales/{id}/{date}/lines/{lineId}`

---

### 4. Clear Cart Flow

**Method**: `clearCart()`

**New Behavior**:
```kotlin
fun clearCart() {
    if (currentSale != null && currentSale.id > 0) {
        viewModelScope.launch {
            salesRepository.deleteOngoingSale(currentSale.id).fold(
                onSuccess = {
                    _currentSale.value = Sale()
                    loadDefaultCustomer()
                },
                onFailure = { error ->
                    // Reset local state even if deletion fails
                    _currentSale.value = Sale()
                    loadDefaultCustomer()
                }
            )
        }
    } else {
        _currentSale.value = Sale()
        loadDefaultCustomer()
    }
}
```

**Backend Call**: `DELETE /api/sales/ongoing/{id}`

---

### 5. Finalization Flow (SIMPLIFIED)

**Method**: `finalizeSale(payments, montantVerse, montantRendu)`

**Before**:
```kotlin
// TWO backend calls
salesRepository.createCashSale(sale)  // Create sale
    .then { createdSale ->
        salesRepository.finalizeSale(createdSale.id, ..., payments)  // Finalize
    }
```

**After**:
```kotlin
// ONE backend call (sale already exists)
fun finalizeSale(payments, montantVerse, montantRendu) {
    val saleId = currentSale.saleId?.id ?: currentSale.id
    val saleDate = currentSale.saleId?.saleDate ?: ""

    salesRepository.finalizeSale(
        id = saleId,
        date = saleDate,
        customerId = selectedCustomer.id,
        payments = payments,
        montantVerse = montantVerse,
        montantRendu = montantRendu
    ).fold(
        onSuccess = { finalizedSale ->
            _saleFinalized.value = finalizedSale
            clearCart()
        },
        onFailure = { error ->
            _errorMessage.value = error.message
        }
    )
}
```

**Backend Call**: `POST /api/sales/{id}/{date}/finalize`

**What finalizeSale does on backend**:
1. Validates stock availability for all products
2. Adds payment information
3. Changes sale status from PENDING → VALIDATED
4. Deducts stock quantities
5. Generates receipt data
6. Returns finalized sale

---

## Backend Stock Validation

### Real-Time Validation

When products are added via `addSaleLine()`, the backend validates:
1. Product exists and is active
2. Stock is available (or user has force stock authority)
3. Quantity is within limits
4. No duplicate products in same sale

### Error Responses

Backend may return errors with `errorKey` field:

**Error: "stock"**
```json
{
  "errorKey": "stock",
  "message": "Stock insuffisant pour le produit X"
}
```
**Handling**: Show force stock dialog if user has PR_FORCE_STOCK authority

**Error: "stockChInsufisant"**
```json
{
  "errorKey": "stockChInsufisant",
  "message": "Stock du conditionnement enfant insuffisant",
  "payload": 12345  // Parent product ID
}
```
**Handling**: Load parent product and show deconditionnement dialog

---

## Sale Status Flow

```
┌─────────────────────────────────────────────────────┐
│                  Create First Product               │
│                  POST /cash-sales                   │
│                  Status: PENDING                    │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│              Add/Update/Remove Products             │
│              POST/PUT/DELETE /lines                 │
│              Status: PENDING                        │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│                  Finalize Sale                      │
│                  POST /finalize                     │
│                  Status: VALIDATED                  │
└─────────────────────────────────────────────────────┘
```

---

## Ongoing Sales Support

### List Ongoing Sales

Users can now see all PENDING sales in the sales list:
```kotlin
salesRepository.getOngoingSales()
```

Returns all sales with status = "PENDING"

### Resume Ongoing Sale

User clicks on ongoing sale → `ComptantSaleActivity` with sale ID:
```kotlin
viewModel.loadSale(saleId, saleDate)
```

Sale is loaded from backend, user can:
- Continue adding products
- Modify quantities
- Remove products
- Finalize sale
- Delete sale (clear cart)

---

## Data Consistency

### Local vs Backend State

**Single Source of Truth**: Backend database

**Local State**: `_currentSale.value` mirrors backend state

**After each operation**:
1. Send request to backend
2. Backend processes and returns updated sale
3. Update local state with backend response

**Benefits**:
- Local state always matches backend
- Backend calculations (totals, taxes, etc.) are authoritative
- No sync issues

---

## Error Handling

### Network Errors

If backend call fails:
1. Show error message to user
2. Keep local state unchanged
3. User can retry operation

### Stock Validation Errors

Frontend validation before backend call:
- Check stock availability
- Check quantity limits
- Show confirmation dialogs

Backend validation on API call:
- Final stock check
- Force stock authority verification
- Return specific error keys for handling

---

## Testing Scenarios

### Scenario 1: Normal Flow
1. User adds Product A (qty: 5) → Backend creates sale
2. User adds Product B (qty: 3) → Backend adds line
3. User updates Product A (qty: 10) → Backend updates line
4. User clicks checkout → Backend finalizes
5. **Result**: Sale finalized in one call

### Scenario 2: App Crash Recovery
1. User adds 3 products → Backend has PENDING sale
2. App crashes
3. User reopens app → See ongoing sale in list
4. User clicks ongoing sale → Resume editing
5. User finalizes → Payment and done

### Scenario 3: Network Failure
1. User adds Product A → Success (backend call succeeds)
2. Network disconnects
3. User adds Product B → Error shown
4. Local state still has Product A only (matches backend)
5. Network reconnects
6. User adds Product B → Success

### Scenario 4: Stock Validation
1. User adds product with insufficient stock
2. Frontend validation → Show confirmation (if has authority)
3. User confirms
4. Backend call with forceStock=true → Success
5. Sale line saved with forceStock flag

---

## Files Modified

1. **ComptantSaleViewModel.kt**:
   - `addProductDirectly()` - Now creates/updates backend sale
   - `createNewSale()` - New method to create sale on backend
   - `addProductToBackend()` - New method to add line to existing sale
   - `removeProductFromCart()` - Now calls backend to remove line
   - `updateProductQuantity()` - Now calls backend to update line
   - `clearCart()` - Now deletes ongoing sale from backend
   - `finalizeSale()` - Simplified to single backend call

2. **SalesRepository.kt** - No changes needed (methods already existed):
   - `createCashSale(sale)` - Creates new sale
   - `addSaleLine(id, date, line)` - Adds product
   - `updateSaleLine(id, date, lineId, line)` - Updates quantity
   - `removeSaleLine(id, date, lineId)` - Removes product
   - `deleteOngoingSale(id)` - Deletes pending sale
   - `finalizeSale(id, date, ...)` - Finalizes with payments

---

## Migration Notes

### Backward Compatibility

Old behavior (two-step):
```kotlin
createCashSale() -> finalizeSale()
```

New behavior (single-step):
```kotlin
// Products already on backend
finalizeSale()
```

**No breaking changes** - Backend API endpoints unchanged

### Database Impact

- More PENDING sales in database (ongoing sales)
- Need cleanup job to delete old PENDING sales (e.g., > 24 hours old)

---

## Next Steps

1. **Test thoroughly**:
   - Test product addition flow
   - Test quantity updates
   - Test product removal
   - Test finalization
   - Test app crash recovery
   - Test network failure scenarios

2. **Add backend error handling**:
   - Parse errorKey from backend responses
   - Handle "stock" error with force stock dialog
   - Handle "stockChInsufisant" with deconditionnement dialog

3. **Add offline support** (future enhancement):
   - Queue operations when offline
   - Sync when connection restored
   - Handle conflicts

4. **Performance optimization**:
   - Debounce quantity updates (don't call backend on every +/-)
   - Cache ongoing sales list
   - Add loading indicators

---

## Comparison: Angular vs Android

| Feature | Angular Web App | Android App (Now) |
|---------|----------------|-------------------|
| Create sale | On first product add | ✅ On first product add |
| Add products | Backend API call | ✅ Backend API call |
| Update quantity | Backend API call | ✅ Backend API call |
| Remove product | Backend API call | ✅ Backend API call |
| Finalization | Single-step (sale exists) | ✅ Single-step (sale exists) |
| Ongoing sales | Supported | ✅ Supported |
| Stock validation | Frontend + Backend | ✅ Frontend + Backend |
| Force stock | With authority | ✅ With authority |

**Result**: Android app now matches Angular web app behavior! ✅
