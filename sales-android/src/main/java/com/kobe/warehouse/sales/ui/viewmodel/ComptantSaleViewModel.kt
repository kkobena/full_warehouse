package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.api.PaymentRequest
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.PaymentMode
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.repository.CustomerRepository
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
 */
class ComptantSaleViewModel(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val customerRepository: CustomerRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // Stock validator instance
    private val stockValidator = SaleStockValidator()

    // Current sale state
    private val _currentSale = MutableLiveData<Sale>()
    val currentSale: LiveData<Sale> = _currentSale

    // Product search results
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    // Payment modes
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
        loadPaymentModes()
        loadDefaultCustomer()
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
     * Follows Angular business logic from PRODUCT_ADDITION_BUSINESS_RULES.md
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
     * Saves to backend immediately
     */
    private fun addProductDirectly(product: Product, quantity: Int, forceStock: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val currentSaleValue = _currentSale.value

            // Create sale line
            val saleLine = SaleLine(
                id = product.id,
                produitId = product.produitId,
                code = product.code,
                produitLibelle = product.libelle,
                quantityRequested = quantity,
                quantitySold = if (forceStock) quantity else minOf(quantity, product.totalQuantity),
                regularUnitPrice = product.regularUnitPrice,
                netUnitPrice = product.netUnitPrice,
                salesAmount = product.regularUnitPrice * (if (forceStock) quantity else minOf(quantity, product.totalQuantity)),
                netAmount = product.netUnitPrice * (if (forceStock) quantity else minOf(quantity, product.totalQuantity)),
                qtyStock = product.totalQuantity,
                forceStock = forceStock
            )

            if (currentSaleValue == null || currentSaleValue.id == 0L) {
                // No sale exists - create new sale on backend
                createNewSale(saleLine)
            } else {
                // Sale exists - add product to existing sale on backend
                addProductToBackend(currentSaleValue, saleLine)
            }

            // Clear pending state
            _pendingProduct.value = null
            _validationResult.value = null
        }
    }

    /**
     * Create new sale on backend with first product
     */
    private suspend fun createNewSale(saleLine: SaleLine) {
        val newSale = Sale(
            customerId = _selectedCustomer.value?.id,
            customer = _selectedCustomer.value,
            salesLines = mutableListOf(saleLine),
            natureVente = "VNO"
        ).recalculateTotals()

        salesRepository.createCashSale(newSale).fold(
            onSuccess = { createdSale ->
                _currentSale.value = createdSale
                _isLoading.value = false
            },
            onFailure = { error ->
                _errorMessage.value = error.message ?: "Erreur lors de la création de la vente"
                _isLoading.value = false
            }
        )
    }

    /**
     * Add product to existing sale on backend
     */
    private suspend fun addProductToBackend(currentSale: Sale, saleLine: SaleLine) {
        val saleId = currentSale.saleId?.id ?: currentSale.id
        val saleDate = currentSale.saleId?.saleDate ?: ""

        salesRepository.addSaleLine(saleId, saleDate, saleLine).fold(
            onSuccess = { updatedSale ->
                _currentSale.value = updatedSale
                _isLoading.value = false
            },
            onFailure = { error ->
                _errorMessage.value = error.message ?: "Erreur lors de l'ajout du produit"
                _isLoading.value = false
            }
        )
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
     * Syncs with backend
     */
    fun removeProductFromCart(saleLine: SaleLine) {
        val currentSaleValue = _currentSale.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val saleId = currentSaleValue.saleId?.id ?: currentSaleValue.id
            val saleDate = currentSaleValue.saleId?.saleDate ?: ""

            salesRepository.removeSaleLine(saleId, saleDate, saleLine.id).fold(
                onSuccess = { updatedSale ->
                    _currentSale.value = updatedSale
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur lors de la suppression du produit"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Update product quantity in cart with validation
     * Syncs with backend
     */
    fun updateProductQuantity(saleLine: SaleLine, newQuantity: Int) {
        if (newQuantity < 1) {
            removeProductFromCart(saleLine)
            return
        }

        val currentSaleValue = _currentSale.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val saleId = currentSaleValue.saleId?.id ?: currentSaleValue.id
            val saleDate = currentSaleValue.saleId?.saleDate ?: ""

            // Update sale line with new quantity
            val updatedLine = saleLine.updateQuantity(newQuantity)

            salesRepository.updateSaleLine(saleId, saleDate, saleLine.id, updatedLine).fold(
                onSuccess = { updatedSale ->
                    _currentSale.value = updatedSale
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur lors de la mise à jour de la quantité"
                    _isLoading.value = false
                }
            )
        }
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
     * Deletes ongoing sale from backend if it exists
     */
    fun clearCart() {
        val currentSaleValue = _currentSale.value

        // If sale exists on backend, delete it
        if (currentSaleValue != null && currentSaleValue.id > 0) {
            viewModelScope.launch {
                salesRepository.deleteOngoingSale(currentSaleValue.id).fold(
                    onSuccess = {
                        // Sale deleted, reset local state
                        _currentSale.value = Sale()
                        loadDefaultCustomer()
                    },
                    onFailure = { error ->
                        // Even if deletion fails, reset local state
                        _currentSale.value = Sale()
                        loadDefaultCustomer()
                        _errorMessage.value = "Erreur lors de la suppression de la vente"
                    }
                )
            }
        } else {
            // No backend sale, just reset local state
            _currentSale.value = Sale()
            loadDefaultCustomer()
        }
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
     * Load default customer (client comptant)
     */
    private fun loadDefaultCustomer() {
        viewModelScope.launch {
            customerRepository.getDefaultCustomer().fold(
                onSuccess = { customer ->
                    _selectedCustomer.value = customer
                },
                onFailure = {
                    // Default customer not required, ignore error
                }
            )
        }
    }

    /**
     * Finalize sale (checkout)
     * Sale already exists on backend with products, just add payments and finalize
     */
    fun finalizeSale(
        payments: List<PaymentRequest>,
        montantVerse: Int,
        montantRendu: Int
    ) {
        val currentSaleValue = _currentSale.value
        if (currentSaleValue == null || currentSaleValue.salesLines.isEmpty()) {
            _errorMessage.value = "Le panier est vide"
            return
        }

        if (currentSaleValue.id == 0L) {
            _errorMessage.value = "Aucune vente à finaliser"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val saleId = currentSaleValue.saleId?.id ?: currentSaleValue.id
            val saleDate = currentSaleValue.saleId?.saleDate ?: ""

            salesRepository.finalizeSale(
                id = saleId,
                date = saleDate,
                customerId = _selectedCustomer.value?.id,
                payments = payments,
                montantVerse = montantVerse,
                montantRendu = montantRendu
            ).fold(
                onSuccess = { finalizedSale ->
                    _saleFinalized.value = finalizedSale
                    _isLoading.value = false
                    // Clear cart after successful sale
                    clearCart()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Erreur de finalisation"
                    _isLoading.value = false
                }
            )
        }
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
     * Clear finalized sale
     */
    fun clearFinalizedSale() {
        _saleFinalized.value = null
    }
}
