package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.Payment
import com.kobe.warehouse.sales.data.model.PaymentMode
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.model.auth.Account
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.service.SaleStockValidator
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * ComptantSaleViewModel
 * Manages state for POS (Point of Sale) screen
 * Handles cart, product search, payments, and checkout with stock validation
 *
 * Note: Backend uses single-step finalization.
 * Cart is managed locally and sent to backend only when finalizing.
 */
class ComptantSaleViewModel(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // Stock validator instance
    private val stockValidator = SaleStockValidator()

    // Cached user account (loaded once at init)
    private var cachedAccount: Account? = null
    private val _currentAccount = MutableLiveData<Account?>()
    val currentAccount: LiveData<Account?> = _currentAccount

    // Current sale state (managed locally)
    private val _currentSale = MutableLiveData<Sale>()
    val currentSale: LiveData<Sale> = _currentSale

    // Product search results
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    // Payment modes (loaded once at init)
    private val _paymentModes = MutableLiveData<List<PaymentMode>>()
    val paymentModes: LiveData<List<PaymentMode>> = _paymentModes

    // Selected customer
    private val _selectedCustomer = MutableLiveData<Customer?>()
    val selectedCustomer: LiveData<Customer?> = _selectedCustomer

    // Loading states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching

    // Error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Sale finalized successfully
    private val _saleFinalized = MutableLiveData<Sale?>()
    val saleFinalized: LiveData<Sale?> = _saleFinalized

    // Validation result for product addition
    private val _validationResult = MutableLiveData<SaleStockValidator.ValidationResult?>()
    val validationResult: LiveData<SaleStockValidator.ValidationResult?> = _validationResult

    // Pending product to add (waiting for user confirmation)
    private val _pendingProduct = MutableLiveData<Pair<Product, Int>?>()
    val pendingProduct: LiveData<Pair<Product, Int>?> = _pendingProduct

    // Cart total (calculated from sale)
    val cartTotal: LiveData<Int> = MutableLiveData<Int>().apply {
        _currentSale.observeForever { sale ->
            value = sale?.salesAmount ?: 0
        }
    }

    // Cart item count
    val cartItemCount: LiveData<Int> = MutableLiveData<Int>().apply {
        _currentSale.observeForever { sale ->
            value = sale?.salesLines?.size ?: 0
        }
    }

    init {
        // Initialize with empty sale
        _currentSale.value = Sale()

        // Load static data once (cached for session)
        loadCurrentAccount()
        loadPaymentModes()
    }

    /**
     * Load current user account (cached for session)
     * Called once at initialization
     */
    private fun loadCurrentAccount() {
        viewModelScope.launch {
            authRepository.getAccount().fold(
                onSuccess = { account ->
                    cachedAccount = account
                    _currentAccount.value = account
                },
                onFailure = { error ->
                    android.util.Log.e("ComptantSaleViewModel", "Failed to load account: ${error.message}")
                    // Don't show error to user - account will be fetched again if needed
                }
            )
        }
    }

    /**
     * Search products by name or code
     */
    fun searchProducts(query: String) {
        if (query.length < 2) {
            _products.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _errorMessage.value = null

            productRepository.searchProducts(query).fold(
                onSuccess = { productList ->
                    _products.value = productList
                    _isSearching.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur de recherche"
                    _isSearching.value = false
                }
            )
        }
    }

    /**
     * Search product by barcode
     */
    fun searchByBarcode(barcode: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _errorMessage.value = null

            productRepository.getProductByCode(barcode).fold(
                onSuccess = { product ->
                    // Automatically add scanned product to cart
                    addProductToCart(product)
                    _isSearching.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "Produit non trouvé: $barcode"
                    _isSearching.value = false
                }
            )
        }
    }

    /**
     * Add product to cart with stock validation
     * Manages cart locally (no backend call)
     */
    fun addProductToCart(product: Product, quantity: Int = 1) {
        // Check if user has force stock authority
        val canForceStock = tokenManager.hasAuthority("PR_FORCE_STOCK")

        // Validate stock
        val validation = stockValidator.validate(
            product = product,
            quantityRequested = quantity,
            currentSale = _currentSale.value,
            canForceStock = canForceStock
        )

        if (validation.isValid) {
            // Validation passed - add product directly
            addProductDirectly(product, quantity, forceStock = false)
        } else {
            // Validation failed - emit result for UI to handle
            _validationResult.value = validation
            _pendingProduct.value = Pair(product, quantity)
        }
    }

    /**
     * Add product directly to cart without validation
     * Used after user confirms force stock or other validation overrides
     * Updates local cart only (no backend call)
     */
    private fun addProductDirectly(product: Product, quantity: Int, forceStock: Boolean = false) {
        val currentSaleValue = _currentSale.value ?: Sale()

        // Add product to cart (local update)
        val updatedSale = currentSaleValue.addProduct(product, quantity)

        // Update local state
        _currentSale.value = updatedSale

        // Clear pending state
        _pendingProduct.value = null
        _validationResult.value = null
    }

    /**
     * Confirm force stock and add product
     * Called when user confirms they want to add product despite insufficient stock
     */
    fun confirmForceStock() {
        val pending = _pendingProduct.value ?: return
        val (product, quantity) = pending
        addProductDirectly(product, quantity, forceStock = true)
    }

    /**
     * Cancel product addition
     * Called when user cancels the force stock or other confirmation dialogs
     */
    fun cancelProductAddition() {
        _pendingProduct.value = null
        _validationResult.value = null
    }

    /**
     * Remove product from cart
     * Updates local cart only (no backend call)
     */
    fun removeProductFromCart(saleLine: SaleLine) {
        val currentSaleValue = _currentSale.value ?: return

        // Remove product from cart (local update)
        val updatedSale = currentSaleValue.removeProduct(saleLine)

        // Update local state
        _currentSale.value = updatedSale
    }

    /**
     * Update product quantity in cart
     * Updates local cart only (no backend call)
     */
    fun updateProductQuantity(saleLine: SaleLine, newQuantity: Int) {
        if (newQuantity < 1) {
            removeProductFromCart(saleLine)
            return
        }

        val currentSaleValue = _currentSale.value ?: return

        // Update quantity (local update)
        val updatedSale = currentSaleValue.updateProductQuantity(saleLine, newQuantity)

        // Update local state
        _currentSale.value = updatedSale
    }

    /**
     * Increment product quantity
     */
    fun incrementQuantity(saleLine: SaleLine) {
        updateProductQuantity(saleLine, saleLine.quantityRequested + 1)
    }

    /**
     * Decrement product quantity
     */
    fun decrementQuantity(saleLine: SaleLine) {
        updateProductQuantity(saleLine, saleLine.quantityRequested - 1)
    }

    /**
     * Clear cart
     * Clears local cart (no backend call)
     */
    fun clearCart() {
        _currentSale.value = Sale()
        _selectedCustomer.value = null
    }

    /**
     * Select customer
     */
    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
    }

    /**
     * Load payment modes
     */
    fun loadPaymentModes() {
        viewModelScope.launch {
            paymentRepository.getPaymentModes().fold(
                onSuccess = { modes ->
                    _paymentModes.value = modes
                },
                onFailure = { _ ->
                    _errorMessage.value = "Erreur de chargement des modes de paiement"
                }
            )
        }
    }

    /**
     * Finalize sale (checkout)
     * Sends complete sale object with products and payments to backend in one call
     *
     * Note: Cart is NOT cleared automatically. The Activity should:
     * 1. Observe saleFinalized
     * 2. Print receipt (optional)
     * 3. Call resetAfterSale() to clear and start new sale
     */
    fun finalizeSale(payments: List<Payment>, montantVerse: Int, montantRendu: Int) {
        val currentSaleValue = _currentSale.value
        if (currentSaleValue == null || currentSaleValue.salesLines.isEmpty()) {
            _errorMessage.value = "Le panier est vide"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Use cached account to retrieve cassierId (optimized - no API call)
            var cassierId = cachedAccount?.id

            if (cassierId == null) {
                // Fallback: try to load account if cache failed
                val accountResult = authRepository.getAccount()
                cassierId = accountResult.getOrNull()?.id
                if (cassierId == null) {
                    _errorMessage.value = "Impossible de récupérer l'ID du caissier"
                    _isLoading.value = false
                    return@launch
                }
                // Update cache for next time
                cachedAccount = accountResult.getOrNull()
                _currentAccount.value = cachedAccount
            }

            // Calculate payrollAmount = salesAmount - discountAmount (for all payment modes)
            val calculatedPayrollAmount = currentSaleValue.salesAmount - currentSaleValue.discountAmount

            // Build complete sale object with all data including cassierId and payrollAmount
            val completeSale = currentSaleValue.copy(
                customerId = _selectedCustomer.value?.id,
                customer = _selectedCustomer.value,
                cassierId = cassierId,
                payrollAmount = calculatedPayrollAmount,
                payments = payments.toMutableList(),
                montantVerse = montantVerse
            )

            // Send complete sale to backend (single call)
            salesRepository.createCashSale(completeSale).fold(
                onSuccess = { finalizedSale ->
                    _saleFinalized.value = finalizedSale
                    _isLoading.value = false
                    // DON'T clear cart here - let Activity handle it after printing
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur de finalisation"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Reset after sale completion (after printing receipt)
     * Clears finalized sale and cart, ready for new sale
     */
    fun resetAfterSale() {
        _saleFinalized.value = null
        _currentSale.value = Sale()
        _selectedCustomer.value = null
    }

    /**
     * Load existing sale (for editing)
     */
    fun loadSale(id: Long, date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            salesRepository.getSaleById(id, date).fold(
                onSuccess = { sale ->
                    _currentSale.value = sale
                    _selectedCustomer.value = sale.customer
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur de chargement"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Get current user account (from cache)
     * Used for retrieving cashier information for receipts
     * Optimized: uses cached account, no API call
     */
    fun getCachedAccount() = cachedAccount
}
