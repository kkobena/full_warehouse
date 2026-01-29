package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.domain.model.SaleType
import com.kobe.warehouse.sales.domain.model.TiersPayant
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Unified Sale ViewModel
 *
 * Manages all types of sales (Comptant, Assurance, Carnet) with unified logic
 * Delegates type-specific operations to appropriate handlers
 */
class UnifiedSaleViewModel(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel(), ISaleViewModel {

    // ===== Sale Type State =====

    private val _currentSaleType = MutableLiveData<SaleType>(SaleType.Comptant)
    val currentSaleType: LiveData<SaleType> = _currentSaleType

    /**
     * Change the sale type
     * Note: Changing type will clear the cart if customer requirements differ
     */
    fun changeSaleType(newType: SaleType) {
        val oldType = _currentSaleType.value

        // If switching from non-customer to customer-required type, validate
        if (oldType?.requiresCustomer() == false && newType.requiresCustomer()) {
            if (_currentSale.value?.salesLines?.isNotEmpty() == true) {
                _errorMessage.value = "Veuillez vider le panier avant de changer le type de vente"
                return
            }
        }

        _currentSaleType.value = newType

        // Update sale object with new type information
        updateSaleWithType(newType)
    }

    // ===== Current Sale State =====

    private val _currentSale = MutableLiveData<Sale>(Sale())
    override val currentSale: LiveData<Sale> = _currentSale

    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    // ===== Customer State =====

    private val _selectedCustomer = MutableLiveData<Customer?>()
    val selectedCustomer: LiveData<Customer?> = _selectedCustomer

    private val _customerRequired = MutableLiveData(false)
    val customerRequired: LiveData<Boolean> = _customerRequired

    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
        _customerValidationError.value = null

        // Update sale with customer
        val updatedSale = _currentSale.value?.copy(customer = customer)
        _currentSale.value = updatedSale

        // Update sale type with customer
        when (val saleType = _currentSaleType.value) {
            is SaleType.Assurance -> {
                // Keep existing tiers payants if any
                _currentSaleType.value = saleType.copy(saleCustomer = customer)
            }
            is SaleType.Carnet -> {
                _currentSaleType.value = SaleType.Carnet(customer)
            }
            else -> {
                // Comptant - customer optional
            }
        }
    }

    fun clearCustomer() {
        if (_currentSaleType.value?.requiresCustomer() == true) {
            _customerValidationError.value = "Client obligatoire pour ce type de vente"
            return
        }
        _selectedCustomer.value = null
        val updatedSale = _currentSale.value?.copy(customer = null)
        _currentSale.value = updatedSale
    }

    private val _customerValidationError = MutableLiveData<String?>()
    val customerValidationError: LiveData<String?> = _customerValidationError

    // ===== Insurance (Assurance) State =====

    private val _tiersPayants = MutableLiveData<List<TiersPayant>>(emptyList())
    val tiersPayants: LiveData<List<TiersPayant>> = _tiersPayants

    fun addTiersPayant(tiersPayant: TiersPayant) {
        val current = _tiersPayants.value ?: emptyList()
        _tiersPayants.value = current + tiersPayant

        // Update sale type
        val customer = _selectedCustomer.value
        if (customer != null) {
            _currentSaleType.value = SaleType.Assurance(
                saleCustomer = customer,
                tiersPayants = _tiersPayants.value ?: emptyList()
            )
        }
    }

    fun removeTiersPayant(tiersPayant: TiersPayant) {
        val current = _tiersPayants.value ?: emptyList()
        _tiersPayants.value = current.filter { it.id != tiersPayant.id }

        // Update sale type
        val customer = _selectedCustomer.value
        if (customer != null && _tiersPayants.value?.isNotEmpty() == true) {
            _currentSaleType.value = SaleType.Assurance(
                saleCustomer = customer,
                tiersPayants = _tiersPayants.value ?: emptyList()
            )
        }
    }

    fun updateTiersPayantTaux(tiersPayant: TiersPayant, newTaux: Int) {
        val current = _tiersPayants.value ?: emptyList()
        _tiersPayants.value = current.map { tp ->
            if (tp.id == tiersPayant.id) {
                tp.copy(tauxCouverture = newTaux)
            } else {
                tp
            }
        }

        // Update sale type
        val customer = _selectedCustomer.value
        if (customer != null) {
            _currentSaleType.value = SaleType.Assurance(
                saleCustomer = customer,
                tiersPayants = _tiersPayants.value ?: emptyList()
            )
        }
    }

    fun updateTiersPayantNumeroBon(tiersPayant: TiersPayant, numeroBon: String) {
        val current = _tiersPayants.value ?: emptyList()
        _tiersPayants.value = current.map { tp ->
            if (tp.id == tiersPayant.id) {
                tp.copy(numeroBon = numeroBon.ifEmpty { null })
            } else {
                tp
            }
        }

        // Update sale type
        val customer = _selectedCustomer.value
        if (customer != null) {
            _currentSaleType.value = SaleType.Assurance(
                saleCustomer = customer,
                tiersPayants = _tiersPayants.value ?: emptyList()
            )
        }
    }

    // ===== Product Search =====

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    fun searchProducts(query: String) {
        if (query.length < 2) {
            _products.value = emptyList()
            return
        }

        _isSearching.value = true
        viewModelScope.launch {
            productRepository.searchProducts(query).fold(
                onSuccess = { productList ->
                    _products.value = productList
                    _isSearching.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "Erreur de recherche: ${error.message}"
                    _isSearching.value = false
                }
            )
        }
    }

    // ===== Cart Management =====

    fun addProductToCart(product: Product, quantity: Int, forceStock: Boolean = false) {
        // Validate customer if required
        if (_currentSaleType.value?.requiresCustomer() == true && _selectedCustomer.value == null) {
            _customerValidationError.value = "Veuillez sélectionner un client d'abord"
            return
        }

        // Validate stock
        if (!forceStock && product.totalQuantity < quantity) {
            _stockValidationError.value = "Stock insuffisant (disponible: ${product.totalQuantity})"
            return
        }

        val currentSale = _currentSale.value ?: Sale()
        val existingLines = currentSale.salesLines.toMutableList()

        // Check if product already in cart
        val existingLineIndex = existingLines.indexOfFirst { it.produitId == product.id }

        if (existingLineIndex >= 0) {
            // Update existing line
            val existingLine = existingLines[existingLineIndex]
            existingLines[existingLineIndex] = existingLine.copy(
                quantitySold = existingLine.quantitySold + quantity,
                salesAmount = (existingLine.quantitySold + quantity) * product.regularUnitPrice
            )
        } else {
            // Add new line
            val newLine = SaleLine(
                produitId = product.id,
                produitLibelle = product.libelle ?: "",
                quantitySold = quantity,
                regularUnitPrice = product.regularUnitPrice,
                salesAmount = quantity * product.regularUnitPrice,
                netUnitPrice = product.netUnitPrice
            )
            existingLines.add(newLine)
        }

        // Recalculate total
        val newTotal = existingLines.sumOf { it.salesAmount }

        _currentSale.value = currentSale.copy(
            salesLines = existingLines,
            salesAmount = newTotal
        )

        // Clear products search
        _products.value = emptyList()
    }

    fun updateLineQuantity(line: SaleLine, newQuantity: Int) {
        val currentSale = _currentSale.value ?: return
        val updatedLines = currentSale.salesLines.map { existingLine ->
            if (existingLine.produitId == line.produitId) {
                existingLine.copy(
                    quantitySold = newQuantity,
                    salesAmount = newQuantity * existingLine.regularUnitPrice
                )
            } else {
                existingLine
            }
        }.filter { it.quantitySold > 0 } // Remove lines with 0 quantity

        val newTotal = updatedLines.sumOf { it.salesAmount }

        _currentSale.value = currentSale.copy(
            salesLines = updatedLines.toMutableList(),
            salesAmount = newTotal
        )
    }

    fun removeLineFromCart(line: SaleLine) {
        val currentSale = _currentSale.value ?: return
        val updatedLines = currentSale.salesLines.filter { it.produitId != line.produitId }
        val newTotal = updatedLines.sumOf { it.salesAmount }

        _currentSale.value = currentSale.copy(
            salesLines = updatedLines.toMutableList(),
            salesAmount = newTotal
        )
    }

    // ===== Sale Operations =====

    private val _saleSaved = MutableLiveData<Sale?>()
    val saleSaved: LiveData<Sale?> = _saleSaved

    private val _saleFinalized = MutableLiveData<Sale?>()
    val saleFinalized: LiveData<Sale?> = _saleFinalized

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun putOnHold() {
        val sale = _currentSale.value

        if (sale == null || sale.salesLines.isEmpty()) {
            _errorMessage.value = "Le panier est vide"
            return
        }

        viewModelScope.launch {
            salesRepository.putCashSaleOnHold(sale).fold(
                onSuccess = { savedSale ->
                    _saleSaved.value = savedSale
                    resetCart()
                },
                onFailure = { error ->
                    _errorMessage.value = "Erreur de sauvegarde: ${error.message}"
                }
            )
        }
    }

    fun loadSale(saleId: Long, saleDate: String) {
        _isEditMode.value = true
        viewModelScope.launch {
            salesRepository.getSaleById(saleId, saleDate).fold(
                onSuccess = { sale ->
                    _currentSale.value = sale
                    _selectedCustomer.value = sale.customer

                    // Restore sale type based on sale data
                    // TODO: Backend should provide sale type info
                    // For now, assume comptant if no specific data
                },
                onFailure = { error ->
                    _errorMessage.value = "Erreur de chargement: ${error.message}"
                }
            )
        }
    }

    fun transformSale(newType: SaleType) {
        val sale = _currentSale.value
        if (sale == null || sale.id == 0L) {
            _errorMessage.value = "Aucune vente à transformer"
            return
        }

        viewModelScope.launch {
            // TODO: Implement transformation API call
            // salesRepository.transformSale(sale.id, newType)
            _errorMessage.value = "Transformation de vente non encore implémentée"
        }
    }

    override fun finalizeSale(payments: List<com.kobe.warehouse.sales.data.model.Payment>, montantVerse: Int, montantRendu: Int) {
        if (!validateSaleBeforeFinalize()) {
            return
        }

        val currentSaleValue = _currentSale.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Get cassier ID from current account
            val accountResult = authRepository.getAccount()
            val cassierId = accountResult.getOrNull()?.id

            if (cassierId == null) {
                _errorMessage.value = "Impossible de récupérer l'ID du caissier"
                _isLoading.value = false
                return@launch
            }

            // Calculate payrollAmount = salesAmount - discountAmount
            val calculatedPayrollAmount = currentSaleValue.salesAmount - (currentSaleValue.discountAmount ?: 0)

            // Build complete sale object with all data
            val completeSale = currentSaleValue.copy(
                customerId = _selectedCustomer.value?.id,
                customer = _selectedCustomer.value,
                cassierId = cassierId,
                payrollAmount = calculatedPayrollAmount,
                payments = payments.toMutableList(),
                montantVerse = montantVerse
            )

            // Call appropriate endpoint based on sale type
            val result = when (_currentSaleType.value) {
                is SaleType.Comptant -> {
                    salesRepository.createCashSale(completeSale)
                }
                is SaleType.Assurance -> {
                    salesRepository.finalizeAssuranceSale(completeSale)
                }
                is SaleType.Carnet -> {
                    salesRepository.finalizeCarnetSale(completeSale)
                }
                else -> {
                    salesRepository.createCashSale(completeSale)
                }
            }

            result.fold(
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

    private fun resetCart() {
        _currentSale.value = Sale()
        _selectedCustomer.value = null
        _tiersPayants.value = emptyList()
        _currentSaleType.value = SaleType.Comptant
        _isEditMode.value = false
    }

    // ===== Validation & Error Handling =====

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _stockValidationError = MutableLiveData<String?>()
    val stockValidationError: LiveData<String?> = _stockValidationError

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearStockValidationError() {
        _stockValidationError.value = null
    }

    fun clearCustomerValidationError() {
        _customerValidationError.value = null
    }

    private fun validateSaleBeforeFinalize(): Boolean {
        val sale = _currentSale.value

        // Check cart not empty
        if (sale == null || sale.salesLines.isEmpty()) {
            _errorMessage.value = "Le panier est vide"
            return false
        }

        // Check customer if required
        if (_currentSaleType.value?.requiresCustomer() == true && _selectedCustomer.value == null) {
            _customerValidationError.value = "Client obligatoire pour ce type de vente"
            return false
        }

        // Type-specific validations
        when (val saleType = _currentSaleType.value) {
            is SaleType.Assurance -> {
                if (saleType.tiersPayants.isEmpty()) {
                    _errorMessage.value = "Au moins un tiers payant est requis"
                    return false
                }
            }
            is SaleType.Carnet -> {
                // TODO: Validate credit limit
            }
            else -> {
                // Comptant - no additional validation
            }
        }

        return true
    }

    // ===== Helper Methods =====

    private fun updateSaleWithType(saleType: SaleType) {
        val sale = _currentSale.value ?: Sale()

        // Update customer requirement indicator
        _customerRequired.value = saleType.requiresCustomer()

        // Type-specific updates
        when (saleType) {
            is SaleType.Comptant -> {
                // Comptant doesn't require specific fields
            }
            is SaleType.Assurance -> {
                _selectedCustomer.value = saleType.saleCustomer
                _tiersPayants.value = saleType.tiersPayants
            }
            is SaleType.Carnet -> {
                _selectedCustomer.value = saleType.saleCustomer
            }
        }

        _currentSale.value = sale.copy(
            customer = saleType.getCustomer()
        )
    }
}
