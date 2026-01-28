package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.PaymentMode
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.domain.model.CarnetData
import com.kobe.warehouse.sales.domain.model.InsuranceData
import com.kobe.warehouse.sales.domain.model.SaleType
import com.kobe.warehouse.sales.domain.validator.AssuranceValidator
import com.kobe.warehouse.sales.domain.validator.CarnetValidator
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified Sale ViewModel
 *
 * Manages state for all types of sales: Comptant, Assurance, Carnet
 * Provides business logic for:
 * - Sale type selection and switching
 * - Customer selection (mandatory for Assurance/Carnet)
 * - Product search and cart management
 * - Insurance data management (for Assurance sales)
 * - Carnet credit management (for Carnet sales)
 * - Payment processing
 * - Sale finalization
 */
class UnifiedSaleViewModel(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // ========== Sale Type Management ==========

    private val _currentSaleType = MutableLiveData<SaleType>(SaleType.Comptant)
    val currentSaleType: LiveData<SaleType> = _currentSaleType

    private val _isCustomerRequired = MutableLiveData<Boolean>(false)
    val isCustomerRequired: LiveData<Boolean> = _isCustomerRequired

    // ========== Customer Management ==========

    private val _selectedCustomer = MutableLiveData<Customer?>()
    val selectedCustomer: LiveData<Customer?> = _selectedCustomer

    private val _customerSearchResults = MutableLiveData<List<Customer>>()
    val customerSearchResults: LiveData<List<Customer>> = _customerSearchResults

    private var customerSearchJob: Job? = null

    // ========== Insurance Data (for Assurance sales) ==========

    private val _insuranceData = MutableLiveData<InsuranceData?>()
    val insuranceData: LiveData<InsuranceData?> = _insuranceData

    private val _insurancePart = MutableLiveData<Int>(0)
    val insurancePart: LiveData<Int> = _insurancePart

    private val _clientPart = MutableLiveData<Int>(0)
    val clientPart: LiveData<Int> = _clientPart

    // ========== Carnet Data (for Carnet sales) ==========

    private val _carnetData = MutableLiveData<CarnetData?>()
    val carnetData: LiveData<CarnetData?> = _carnetData

    // ========== Product & Cart Management ==========

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _currentSale = MutableLiveData<Sale>(Sale())
    val currentSale: LiveData<Sale> = _currentSale

    private val _cartTotal = MutableLiveData<Int>(0)
    val cartTotal: LiveData<Int> = _cartTotal

    private val _cartItemCount = MutableLiveData<Int>(0)
    val cartItemCount: LiveData<Int> = _cartItemCount

    private var productSearchJob: Job? = null

    // ========== Payment Management ==========

    private val _paymentModes = MutableLiveData<List<PaymentMode>>()
    val paymentModes: LiveData<List<PaymentMode>> = _paymentModes

    // ========== Loading & Error States ==========

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // ========== Sale Finalization ==========

    private val _saleFinalized = MutableLiveData<Sale?>()
    val saleFinalized: LiveData<Sale?> = _saleFinalized

    // Validators
    private val assuranceValidator = AssuranceValidator()
    private val carnetValidator = CarnetValidator()

    init {
        loadPaymentModes()
    }

    // ========== Sale Type Methods ==========

    /**
     * Handle sale type change
     */
    fun onSaleTypeChanged(newSaleType: SaleType) {
        _currentSaleType.value = newSaleType
        _isCustomerRequired.value = newSaleType.requiresCustomer()

        // Reset type-specific data when changing type
        when (newSaleType) {
            is SaleType.Comptant -> {
                // Can keep customer if already selected (optional for Comptant)
                _insuranceData.value = null
                _carnetData.value = null
            }
            is SaleType.Assurance -> {
                // Customer is mandatory
                _carnetData.value = null
                // Insurance data will be set when tiers payants are selected
            }
            is SaleType.Carnet -> {
                // Customer is mandatory
                _insuranceData.value = null
                // Carnet data will be loaded when customer is selected
            }
        }

        // Recalculate totals based on new sale type
        recalculateTotals()
    }

    // ========== Customer Methods ==========

    /**
     * Search customers with debounce
     */
    fun searchCustomers(query: String) {
        customerSearchJob?.cancel()
        customerSearchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms

            if (query.length < 2) {
                _customerSearchResults.value = emptyList()
                return@launch
            }

            _isLoading.value = true
            customerRepository.searchCustomers(query)
                .fold(
                    onSuccess = { customers ->
                        _customerSearchResults.value = customers
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de recherche client: ${error.message}"
                        _customerSearchResults.value = emptyList()
                        _isLoading.value = false
                    }
                )
        }
    }

    /**
     * Select a customer
     */
    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
        _customerSearchResults.value = emptyList()

        // Load additional data based on sale type
        when (_currentSaleType.value) {
            is SaleType.Assurance -> {
                // Load customer's tiers payants and plafond info
                loadCustomerInsuranceData(customer.id)
            }
            is SaleType.Carnet -> {
                // Load customer's carnet info (credit limit, encours)
                loadCustomerCarnetData(customer.id)
            }
            else -> {
                // For Comptant, no additional data needed
            }
        }
    }

    /**
     * Clear selected customer
     */
    fun clearCustomer() {
        _selectedCustomer.value = null
        _insuranceData.value = null
        _carnetData.value = null
    }

    /**
     * Load customer insurance data (for Assurance sales)
     */
    private fun loadCustomerInsuranceData(customerId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            customerRepository.getCustomerInsuranceData(customerId)
                .fold(
                    onSuccess = { data ->
                        // Set insurance data with customer's tiers payants
                        // This will be fully implemented in Task #5
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de chargement des données d'assurance: ${error.message}"
                        _isLoading.value = false
                    }
                )
        }
    }

    /**
     * Load customer carnet data (for Carnet sales)
     */
    private fun loadCustomerCarnetData(customerId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            customerRepository.getCustomerCarnetData(customerId)
                .fold(
                    onSuccess = { data ->
                        _carnetData.value = data
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de chargement des données carnet: ${error.message}"
                        _isLoading.value = false
                    }
                )
        }
    }

    // ========== Product & Cart Methods ==========

    /**
     * Search products with debounce
     */
    fun searchProducts(query: String) {
        productSearchJob?.cancel()
        productSearchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms

            if (query.length < 2) {
                _products.value = emptyList()
                return@launch
            }

            _isLoading.value = true
            productRepository.searchProducts(query)
                .fold(
                    onSuccess = { products ->
                        _products.value = products
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de recherche produit: ${error.message}"
                        _products.value = emptyList()
                        _isLoading.value = false
                    }
                )
        }
    }

    /**
     * Add product to cart
     */
    fun addProductToCart(product: Product, quantity: Int = 1) {
        // Validate stock
        if (product.currentStockQuantity < quantity) {
            _errorMessage.value = "Stock insuffisant. Disponible: ${product.currentStockQuantity}"
            return
        }

        val sale = _currentSale.value ?: Sale()

        // Check if product already in cart
        val existingLine = sale.salesLines.find { it.productId == product.id }

        if (existingLine != null) {
            // Update quantity
            val newQuantity = existingLine.quantitySold + quantity
            if (newQuantity > product.currentStockQuantity) {
                _errorMessage.value = "Stock insuffisant"
                return
            }
            existingLine.quantitySold = newQuantity
            existingLine.salesAmount = existingLine.regularUnitPrice * newQuantity
        } else {
            // Add new line
            val newLine = com.kobe.warehouse.sales.data.model.SaleLine(
                productId = product.id,
                productName = product.productName,
                productCode = product.productCode,
                quantitySold = quantity,
                regularUnitPrice = product.regularUnitPrice,
                salesAmount = product.regularUnitPrice * quantity
            )
            sale.salesLines.add(newLine)
        }

        _currentSale.value = sale
        recalculateTotals()
    }

    /**
     * Remove product from cart
     */
    fun removeProductFromCart(productId: Long) {
        val sale = _currentSale.value ?: return
        sale.salesLines.removeIf { it.productId == productId }
        _currentSale.value = sale
        recalculateTotals()
    }

    /**
     * Update product quantity in cart
     */
    fun updateProductQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeProductFromCart(productId)
            return
        }

        val sale = _currentSale.value ?: return
        val line = sale.salesLines.find { it.productId == productId } ?: return

        line.quantitySold = newQuantity
        line.salesAmount = line.regularUnitPrice * newQuantity

        _currentSale.value = sale
        recalculateTotals()
    }

    /**
     * Recalculate cart totals
     */
    private fun recalculateTotals() {
        val sale = _currentSale.value ?: Sale()
        val total = sale.salesLines.sumOf { it.salesAmount }

        sale.salesAmount = total
        _cartTotal.value = total
        _cartItemCount.value = sale.salesLines.size

        // Recalculate insurance/client parts if Assurance sale
        if (_currentSaleType.value is SaleType.Assurance) {
            val insuranceData = _insuranceData.value
            if (insuranceData != null) {
                _insurancePart.value = insuranceData.calculateInsurancePart(total)
                _clientPart.value = insuranceData.calculateClientPart(total)
            }
        }

        _currentSale.value = sale
    }

    // ========== Payment Methods ==========

    /**
     * Load available payment modes
     */
    private fun loadPaymentModes() {
        viewModelScope.launch {
            paymentRepository.getPaymentModes()
                .fold(
                    onSuccess = { modes ->
                        _paymentModes.value = modes.filter { it.enable }
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur de chargement des modes de paiement: ${error.message}"
                    }
                )
        }
    }

    // ========== Sale Finalization ==========

    /**
     * Finalize sale
     */
    fun finalizeSale() {
        val sale = _currentSale.value ?: return

        // Validate cart not empty
        if (sale.salesLines.isEmpty()) {
            _errorMessage.value = "Le panier est vide"
            return
        }

        // Validate based on sale type
        val validation = validateSale()
        if (!validation.first) {
            _errorMessage.value = validation.second
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            // Get cassier from cached account
            authRepository.getAccount()
                .fold(
                    onSuccess = { account ->
                        sale.cassierId = account.id

                        // Set sale type and customer
                        when (val saleType = _currentSaleType.value) {
                            is SaleType.Comptant -> {
                                sale.natureVente = "COMPTANT"
                                sale.customer = _selectedCustomer.value
                            }
                            is SaleType.Assurance -> {
                                sale.natureVente = "ASSURANCE"
                                sale.customer = saleType.customer
                            }
                            is SaleType.Carnet -> {
                                sale.natureVente = "CARNET"
                                sale.customer = saleType.customer
                            }
                        }

                        // Call appropriate repository method based on sale type
                        finalizeSaleByType(sale)
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur d'authentification: ${error.message}"
                        _isLoading.value = false
                    }
                )
        }
    }

    /**
     * Finalize sale by type
     */
    private suspend fun finalizeSaleByType(sale: Sale) {
        when (val saleType = _currentSaleType.value) {
            is SaleType.Comptant -> {
                // Cash sale finalization
                // TODO: Implement finalizeCashSale in SalesRepository
                _saleFinalized.value = sale
                _isLoading.value = false
                resetAfterSale()
            }
            is SaleType.Assurance -> {
                salesRepository.finalizeAssuranceSale(sale)
                    .fold(
                        onSuccess = { finalizedSale ->
                            _saleFinalized.value = finalizedSale
                            _isLoading.value = false
                            resetAfterSale()
                        },
                        onFailure = { error ->
                            _errorMessage.value = "Erreur de finalisation assurance: ${error.message}"
                            _isLoading.value = false
                        }
                    )
            }
            is SaleType.Carnet -> {
                salesRepository.finalizeCarnetSale(sale)
                    .fold(
                        onSuccess = { finalizedSale ->
                            _saleFinalized.value = finalizedSale
                            _isLoading.value = false
                            resetAfterSale()
                        },
                        onFailure = { error ->
                            _errorMessage.value = "Erreur de finalisation carnet: ${error.message}"
                            _isLoading.value = false
                        }
                    )
            }
            null -> {
                _errorMessage.value = "Type de vente non défini"
                _isLoading.value = false
            }
        }
    }

    /**
     * Validate sale before finalization
     */
    private fun validateSale(): Pair<Boolean, String?> {
        val sale = _currentSale.value ?: return false to "Vente non initialisée"

        if (sale.salesLines.isEmpty()) {
            return false to "Le panier est vide"
        }

        return when (_currentSaleType.value) {
            is SaleType.Comptant -> {
                // No specific validation for Comptant
                true to null
            }
            is SaleType.Assurance -> {
                // Validate customer
                if (_selectedCustomer.value == null) {
                    return false to "Client obligatoire pour vente assurance"
                }

                // Validate insurance data
                val insuranceData = _insuranceData.value
                val validation = assuranceValidator.validateInsuranceData(insuranceData)
                if (!validation.isValid) {
                    return false to validation.getErrorMessage()
                }

                true to null
            }
            is SaleType.Carnet -> {
                // Validate customer
                if (_selectedCustomer.value == null) {
                    return false to "Client obligatoire pour vente carnet"
                }

                // Validate carnet data and credit limit
                val carnetData = _carnetData.value
                val validation = carnetValidator.validateSaleAmount(carnetData, sale.salesAmount)
                if (!validation.isValid) {
                    return false to validation.getErrorMessage()
                }

                true to null
            }
            null -> {
                // No sale type selected
                false to "Type de vente non sélectionné"
            }
        }
    }

    /**
     * Reset after successful sale
     */
    private fun resetAfterSale() {
        _currentSale.value = Sale()
        _cartTotal.value = 0
        _cartItemCount.value = 0
        _products.value = emptyList()
        _selectedCustomer.value = null
        _insuranceData.value = null
        _carnetData.value = null
    }

    // ========== Sale Transformation ==========

    /**
     * Transform sale to a different type
     * Note: This maintains the cart but may recalculate prices based on new type
     */
    fun transformSale(newType: SaleType) {
        val currentSale = _currentSale.value ?: return

        // Can't transform if cart is empty
        if (currentSale.salesLines.isEmpty()) {
            _errorMessage.value = "Impossible de transformer une vente vide"
            return
        }

        // Validate transformation based on current and target types
        val validation = validateTransformation(newType)
        if (!validation.first) {
            _errorMessage.value = validation.second
            return
        }

        // Update sale type
        _currentSaleType.value = newType
        _isCustomerRequired.value = newType.requiresCustomer()

        // Reset type-specific data
        when (newType) {
            is SaleType.Comptant -> {
                _insuranceData.value = null
                _carnetData.value = null
            }
            is SaleType.Assurance -> {
                _carnetData.value = null
                // Insurance data will be set when tiers payants are selected
            }
            is SaleType.Carnet -> {
                _insuranceData.value = null
                // Carnet data will be loaded when customer is selected
            }
        }

        // Recalculate totals (prices may change based on type)
        recalculateTotals()
    }

    /**
     * Validate transformation
     */
    private fun validateTransformation(newType: SaleType): Pair<Boolean, String?> {
        val currentType = _currentSaleType.value

        // Can't transform to same type
        if (currentType?.javaClass == newType.javaClass) {
            return false to "La vente est déjà de ce type"
        }

        // Validate customer requirement for target type
        if (newType.requiresCustomer() && _selectedCustomer.value == null) {
            return false to "Un client est obligatoire pour ce type de vente"
        }

        return true to null
    }

    // ========== Utility Methods ==========

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearFinalizedSale() {
        _saleFinalized.value = null
    }
}
