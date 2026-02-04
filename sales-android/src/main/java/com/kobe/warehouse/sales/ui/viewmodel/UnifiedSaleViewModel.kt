package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.ClientTiersPayant
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.PrioriteTiersPayant
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.model.SaleLineId
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.data.model.SaleType
import com.kobe.warehouse.sales.data.model.SalesStatut
import com.kobe.warehouse.sales.data.model.TiersPayant
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

        // Update customer required flag
        updateCustomerRequired()

        // Update sale object with new type information
        updateSaleWithType(newType)
    }

    /**
     * Update customer required flag based on sale type
     * - Always true for Assurance and Carnet
     * - For Comptant: true if deferred payment or credit (checked during finalization)
     */
    private fun updateCustomerRequired() {
        _customerRequired.value = _currentSaleType.value?.requiresCustomer() == true
    }

    // ===== Current Sale State =====

    private val _currentSale = MutableLiveData<Sale>(Sale())
    override val currentSale: LiveData<Sale> = _currentSale

    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    // ===== Prevente State =====
    private val _isPrevente = MutableLiveData(false)
    val isPrevente: LiveData<Boolean> = _isPrevente

    /**
     * Set prevente mode (for creating new prevente)
     * When creating a new prevente, sets the sale status to PROCESSING
     */
    fun setPreventeMode(isPrevente: Boolean) {
        _isPrevente.value = isPrevente
        if (isPrevente) {
            // Set the sale status to PROCESSING for new prevente
            val currentSale = _currentSale.value ?: Sale()
            _currentSale.value = currentSale.copy(statut = SalesStatut.PROCESSING)
        }
    }

    /**
     * Check if a sale is in progress (has items in cart or has been saved to backend)
     */
    fun isSaleInProgress(): Boolean {
        val sale = _currentSale.value ?: return false
        // Sale is in progress if it has items or has been saved (has an ID)
        return sale.salesLines.isNotEmpty() || (sale.id != null && sale.id != 0L)
    }

    /**
     * Cancel and reset the current sale
     */
    fun cancelCurrentSale() {
        resetCart()
        _isPrevente.value = false
    }

    // ===== Customer State =====

    private val _selectedCustomer = MutableLiveData<Customer?>()
    val selectedCustomer: LiveData<Customer?> = _selectedCustomer

    private val _customerRequired = MutableLiveData(false)
    val customerRequired: LiveData<Boolean> = _customerRequired

    // Customer search results
    private val _customerSearchResults = MutableLiveData<List<Customer>>()
    val customerSearchResults: LiveData<List<Customer>> = _customerSearchResults

    private val _isSearchingCustomer = MutableLiveData(false)
    val isSearchingCustomer: LiveData<Boolean> = _isSearchingCustomer

    /**
     * Search customers by query
     * Returns results via customerSearchResults LiveData
     */
    fun searchCustomers(query: String) {
        if (query.length < 2) {
            _customerSearchResults.value = emptyList()
            return
        }

        _isSearchingCustomer.value = true
        viewModelScope.launch {
            customerRepository.searchAssuredCustomers(query).fold(
                onSuccess = { customers ->
                    _customerSearchResults.value = customers
                    _isSearchingCustomer.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "Erreur de recherche client: ${error.message}"
                    _customerSearchResults.value = emptyList()
                    _isSearchingCustomer.value = false
                }
            )
        }
    }

    /**
     * Clear customer search results
     */
    fun clearCustomerSearchResults() {
        _customerSearchResults.value = emptyList()
    }

    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
        _customerValidationError.value = null
        _customerSearchResults.value = emptyList() // Clear search results after selection

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

    /**
     * Replace current customer with a new one
     * Resets all customer-related data (tiers payants, ayant droits)
     */
    fun replaceCustomer(newCustomer: Customer) {
        // Reset customer-related data
        _clientTiersPayants.value = emptyList()
        // TODO: Reset ayant droits when implemented

        // Select new customer
        selectCustomer(newCustomer)
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

    private val _clientTiersPayants = MutableLiveData<List<ClientTiersPayant>>(emptyList())
    val clientTiersPayants: LiveData<List<ClientTiersPayant>> = _clientTiersPayants

    /**
     * Add tiers payant with customer insurance details
     * @param tiersPayantId Insurance provider ID
     * @param tiersPayantName Insurance provider name
     * @param num Customer's insurance number (matricule)
     * @param taux Coverage rate
     * @param numBon Prescription number
     * @param priorite Priority (0 = principal, 1+ = complementary)
     */
    fun addClientTiersPayant(
        tiersPayantId: Long,
        tiersPayantName: String,
        num: String,
        taux: Int,
        numBon: String? = null,
        priorite: Int = 0
    ) {
        val customer = _selectedCustomer.value
        if (customer == null) {
            _errorMessage.value = "Veuillez sélectionner un client d'abord"
            return
        }

        val clientTiersPayant = ClientTiersPayant(
            customerId = customer.id,
            tiersPayantId = tiersPayantId,
            tiersPayantName = tiersPayantName,
            num = num,
            taux = taux,
            numBon = numBon,
            priorite = PrioriteTiersPayant.fromValue(priorite),
            typeTiersPayant = if (priorite == 0) "PRINCIPAL" else "COMPLEMENTAIRE"
        )

        val current = _clientTiersPayants.value ?: emptyList()
        _clientTiersPayants.value = current + clientTiersPayant
    }

    fun removeClientTiersPayant(clientTiersPayant: ClientTiersPayant) {
        val current = _clientTiersPayants.value ?: emptyList()
        _clientTiersPayants.value = current.filter { it.tiersPayantId != clientTiersPayant.tiersPayantId }
    }

    fun updateClientTiersPayantTaux(clientTiersPayant: ClientTiersPayant, newTaux: Int) {
        val current = _clientTiersPayants.value ?: emptyList()
        _clientTiersPayants.value = current.map { ctp ->
            if (ctp.tiersPayantId == clientTiersPayant.tiersPayantId) {
                ctp.copy(taux = newTaux)
            } else {
                ctp
            }
        }
    }

    fun updateClientTiersPayantNumeroBon(clientTiersPayant: ClientTiersPayant, numeroBon: String) {
        val current = _clientTiersPayants.value ?: emptyList()
        _clientTiersPayants.value = current.map { ctp ->
            if (ctp.tiersPayantId == clientTiersPayant.tiersPayantId) {
                ctp.copy(numBon = numeroBon.ifEmpty { null })
            } else {
                ctp
            }
        }
    }

    fun updateClientTiersPayantNum(clientTiersPayant: ClientTiersPayant, num: String) {
        val current = _clientTiersPayants.value ?: emptyList()
        _clientTiersPayants.value = current.map { ctp ->
            if (ctp.tiersPayantId == clientTiersPayant.tiersPayantId) {
                ctp.copy(num = num)
            } else {
                ctp
            }
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

    /**
     * Add product to cart with API call (Unified Sale Workflow)
     * - For COMPTANT: calls comptant/create or comptant/add-item
     * - For ASSURANCE/CARNET: calls vo/create or vo/add-item
     * - First line creates new sale, subsequent lines add to existing sale
     *
     * NOTE: Stock validation should be done BEFORE calling this method.
     * This method assumes stock validation has passed or forceStock is true.
     */
    fun addProductToCart(product: Product, quantity: Int, forceStock: Boolean = false) {
        // Validate customer if required for ASSURANCE/CARNET
        if (_currentSaleType.value?.requiresCustomer() == true && _selectedCustomer.value == null) {
            _customerValidationError.value = "Veuillez sélectionner un client d'abord"
            return
        }

        val currentSale = _currentSale.value ?: Sale()
        val saleType = _currentSaleType.value ?: SaleType.Comptant

        // Create SaleLine for the product
        val newLine = SaleLine(
            produitId = product.id,
            produitLibelle = product.libelle ?: "",
            code = product.code ?: "",
            quantitySold = quantity,
            regularUnitPrice = product.regularUnitPrice,
            salesAmount = quantity * product.regularUnitPrice,
            netUnitPrice = product.netUnitPrice,
            qtyStock = product.totalQuantity,
            saleCompositeId = currentSale.saleId,  // Set from current sale (null for new sale)
            forceStock = forceStock  // Mark if stock was forced
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = if (currentSale.id == null) {
                    // First line - create new sale
                    createNewSaleWithFirstLine(currentSale, newLine, saleType)
                } else {
                    // Subsequent lines - add to existing sale
                    addLineToExistingSale(currentSale, newLine, saleType)
                }

                result.fold(
                    onSuccess = { updatedSale ->
                        _currentSale.value = updatedSale
                        _products.value = emptyList() // Clear search
                        _errorMessage.value = "Produit ajouté"
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur : ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erreur : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create new sale with first product line
     */
    private suspend fun createNewSaleWithFirstLine(
        currentSale: Sale,
        firstLine: SaleLine,
        saleType: SaleType
    ): Result<Sale> {
        // Get cassierId from TokenManager (required by backend)
        var cassierId = tokenManager.getUserId()
        android.util.Log.d("UnifiedSaleViewModel", "=== CREATE NEW SALE WITH FIRST LINE ===")
        android.util.Log.d("UnifiedSaleViewModel", "cassierId from TokenManager = $cassierId")

        if (cassierId == null) {
            // Fallback: try to load account from API
            android.util.Log.w("UnifiedSaleViewModel", "cassierId is null, trying fallback via API...")
            val accountResult = authRepository.getAccount()
            val account = accountResult.getOrNull()
            cassierId = account?.id
            android.util.Log.d("UnifiedSaleViewModel", "cassierId from API = $cassierId")
            if (cassierId != null) {
                tokenManager.storeUserId(cassierId)
            }
        }

        if (cassierId == null) {
            android.util.Log.e("UnifiedSaleViewModel", "FAILED: cassierId is null - cannot create sale")
            return Result.failure(Exception("Impossible de récupérer l'ID du caissier. Veuillez vous reconnecter."))
        }

        // Set statut based on prevente mode: PROCESSING for prevente, ACTIVE for normal sale
        val saleStatut = if (_isPrevente.value == true) SalesStatut.PROCESSING else SalesStatut.ACTIVE

        val saleToCreate = currentSale.copy(
            salesLines = mutableListOf(firstLine),
            salesAmount = firstLine.salesAmount,
            customerId = _selectedCustomer.value?.id,
            customer = _selectedCustomer.value,
            cassierId = cassierId,
            sellerId = cassierId, // seller = cassier for mobile sales
            natureVente = saleType.toString(),  // "COMPTANT", "ASSURANCE", or "CARNET"
            tiersPayants = _clientTiersPayants.value?.toMutableList() ?: mutableListOf(),
            type = if (saleType is SaleType.Comptant) "VNO" else "VO",
            categorie = if (saleType is SaleType.Comptant) "VNO" else "VO",
            statut = saleStatut
        )

        android.util.Log.d("UnifiedSaleViewModel", "saleToCreate.cassierId = ${saleToCreate.cassierId}")
        android.util.Log.d("UnifiedSaleViewModel", "saleToCreate.sellerId = ${saleToCreate.sellerId}")
        android.util.Log.d("UnifiedSaleViewModel", "saleToCreate.statut = ${saleToCreate.statut} (isPrevente=${_isPrevente.value})")

        return when (saleType) {
            is SaleType.Comptant -> salesRepository.createComptantSale(saleToCreate)
            is SaleType.Assurance, is SaleType.Carnet -> {
                // For VO sales, customer and tiers payants must be set
                if (saleToCreate.customerId == null) {
                    return Result.failure(Exception("Client requis pour ${saleType.getDisplayName()}"))
                }
                // Validate tiers payants are present
                if (saleToCreate.tiersPayants.isEmpty()) {
                    return Result.failure(Exception("Au moins un tiers payant est requis pour ${saleType.getDisplayName()}"))
                }
                salesRepository.createVOSale(saleToCreate)
            }
        }
    }

    /**
     * Add line to existing sale
     * IMPORTANT: saleCompositeId must be set for existing sales
     */
    private suspend fun addLineToExistingSale(
        currentSale: Sale,
        newLine: SaleLine,
        saleType: SaleType
    ): Result<Sale> {
        // Ensure saleCompositeId is set (required for backend)
        if (newLine.saleCompositeId == null) {
            return Result.failure(Exception("saleCompositeId requis pour ajouter une ligne à une vente existante"))
        }

        // Set saleId (Long) on the new line - extract from currentSale.id
        val lineWithSaleId = newLine.copy(
            saleId = currentSale.id  // Sale.id is Long?
        )

        val result = when (saleType) {
            is SaleType.Comptant -> salesRepository.addItemToComptantSale(lineWithSaleId)
            is SaleType.Assurance, is SaleType.Carnet -> salesRepository.addItemToVOSale(lineWithSaleId)
        }

        return result.fold(
            onSuccess = { addedLine ->
                // Add the line to current sale and recalculate total
                val updatedLines = currentSale.salesLines.toMutableList()
                updatedLines.add(addedLine)
                val newTotal = updatedLines.sumOf { it.salesAmount }

                Result.success(currentSale.copy(
                    salesLines = updatedLines,
                    salesAmount = newTotal
                ))
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
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

    /**
     * Create a new prevente from current prevente (reset cart, keep as prevente mode)
     * Only available when editing an existing prevente
     */
    fun createNewPrevente() {
        if (_isPrevente.value != true) {
            _errorMessage.value = "Cette action n'est disponible que pour les préventes"
            return
        }

        // Reset cart and sale but stay in prevente mode
        resetCart()
        _isPrevente.value = true
        _isEditMode.value = false
    }

    /**
     * Finalize the prevente
     * Calls different endpoint based on natureVente (COMPTANT vs ASSURANCE/CARNET)
     */
    fun finalizePrevente() {
        val sale = _currentSale.value

        if (sale == null || sale.salesLines.isEmpty()) {
            _errorMessage.value = "Le panier est vide"
            return
        }

        if (_isPrevente.value != true) {
            _errorMessage.value = "Cette vente n'est pas une prévente"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Update sale statut to PROCESSING before finalizing
            val saleToFinalize = sale.copy(statut = SalesStatut.PROCESSING)

            val result = when (_currentSaleType.value) {
                is SaleType.Assurance, is SaleType.Carnet -> {
                    salesRepository.finalizeAssurancePrevente(saleToFinalize)
                }
                else -> {
                    salesRepository.finalizeComptantPrevente(saleToFinalize)
                }
            }

            result.fold(
                onSuccess = {
                    _isLoading.value = false
                    _preventeFinalized.value = true
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = "Erreur de finalisation: ${error.message}"
                }
            )
        }
    }

    // Event for prevente finalized
    private val _preventeFinalized = MutableLiveData<Boolean>()
    val preventeFinalized: LiveData<Boolean> = _preventeFinalized

    fun clearPreventeFinalized() {
        _preventeFinalized.value = false
    }

    fun loadSale(saleId: Long, saleDate: String) {
        _isEditMode.value = true
        viewModelScope.launch {
            salesRepository.getSaleById(saleId, saleDate).fold(
                onSuccess = { sale ->
                    _currentSale.value = sale
                    _selectedCustomer.value = sale.customer

                    // Detect if it's a prevente (statut PENDING or PROCESSING)
                    _isPrevente.value = sale.statut == SalesStatut.PENDING || sale.statut == SalesStatut.PROCESSING

                    // Restore sale type based on natureVente
                    // Note: For Assurance, we pass empty tiersPayants list as the actual data
                    // is already in the Sale object. This just identifies the sale type for UI purposes.
                    val saleType = when (sale.natureVente) {
                        "ASSURANCE" -> SaleType.Assurance(sale.customer, emptyList())
                        "CARNET" -> SaleType.Carnet(sale.customer)
                        else -> SaleType.Comptant
                    }
                    _currentSaleType.value = saleType
                    updateCustomerRequired()
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

        // For Comptant sales, validate customer requirement based on payment conditions
        if (_currentSaleType.value is SaleType.Comptant) {
            if (!validateCustomerForComptant(payments, currentSaleValue.salesAmount)) {
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Get cassierId from TokenManager (stored during login)
            var cassierId = tokenManager.getUserId()
            android.util.Log.d("UnifiedSaleViewModel", "=== FINALIZE SALE DEBUG ===")
            android.util.Log.d("UnifiedSaleViewModel", "Step 1: cassierId from TokenManager.getUserId() = $cassierId")

            if (cassierId == null) {
                android.util.Log.w("UnifiedSaleViewModel", "Step 2: cassierId is null, trying fallback via API...")
                // Fallback: try to load account from API
                val accountResult = authRepository.getAccount()
                val account = accountResult.getOrNull()
                android.util.Log.d("UnifiedSaleViewModel", "Step 3: Account from API = $account")
                android.util.Log.d("UnifiedSaleViewModel", "Step 3: Account.id = ${account?.id}")
                cassierId = account?.id
                if (cassierId == null) {
                    android.util.Log.e("UnifiedSaleViewModel", "Step 4: FAILED - cassierId still null after API call")
                    _errorMessage.value = "Impossible de récupérer l'ID du caissier. Veuillez vous reconnecter."
                    _isLoading.value = false
                    return@launch
                }
                // Store for next time
                android.util.Log.d("UnifiedSaleViewModel", "Step 5: Storing cassierId in TokenManager: $cassierId")
                tokenManager.storeUserId(cassierId)
            }

            android.util.Log.d("UnifiedSaleViewModel", "Step 6: Final cassierId = $cassierId")

            // Calculate payrollAmount = salesAmount - discountAmount
            val calculatedPayrollAmount = currentSaleValue.salesAmount - (currentSaleValue.discountAmount ?: 0)

            // Calculate amountToBePaid based on sale type
            val amountToBePaid = when (_currentSaleType.value) {
                is SaleType.Assurance, is SaleType.Carnet -> {
                    // For insurance/carnet: use partAssure (insured's part) if set, otherwise payroll amount
                    currentSaleValue.partAssure ?: calculatedPayrollAmount
                }
                else -> {
                    // For comptant: full payroll amount
                    calculatedPayrollAmount
                }
            }

            // Determine natureVente, type, and categorie based on sale type
            val (natureVente, type, categorie) = when (_currentSaleType.value) {
                is SaleType.Assurance -> Triple("ASSURANCE", "VO", "VO")  // Vente Ordinaire
                is SaleType.Carnet -> Triple("CARNET", "VO", "VO")     // Vente Ordinaire
                else -> Triple("COMPTANT", "VNO", "VNO")                 // Vente Non Ordinaire (Comptant)
            }

            // Build complete sale object with all data (cassierId and sellerId required by backend)
            val completeSale = currentSaleValue.copy(
                customerId = _selectedCustomer.value?.id,
                customer = _selectedCustomer.value,
                cassierId = cassierId,
                sellerId = cassierId, // seller = cassier for mobile sales
                payrollAmount = calculatedPayrollAmount,
                amountToBePaid = amountToBePaid,  // Required by backend
                payments = payments.toMutableList(),
                montantVerse = montantVerse,
                // Insurance/Carnet specific fields
                tiersPayants = _clientTiersPayants.value?.toMutableList() ?: mutableListOf(),
                natureVente = natureVente,
                type = type,
                categorie = categorie,
                typePrescription = "PRESCRIPTION",  // TODO: Make this configurable
                sansBon = false  // TODO: Make this configurable
            )

            android.util.Log.d("UnifiedSaleViewModel", "Step 7: completeSale.cassierId = ${completeSale.cassierId}")
            android.util.Log.d("UnifiedSaleViewModel", "Step 7: completeSale.sellerId = ${completeSale.sellerId}")
            android.util.Log.d("UnifiedSaleViewModel", "Step 7: completeSale.customerId = ${completeSale.customerId}")
            android.util.Log.d("UnifiedSaleViewModel", "=== END FINALIZE SALE DEBUG ===")

            // Call appropriate finalization endpoint based on sale type
            // Use unified sale finalization endpoints (POST /api/sales/comptant/finalize or /api/sales/vo/finalize)
            val result = when (_currentSaleType.value) {
                is SaleType.Comptant -> {
                    salesRepository.finalizeComptantSale(completeSale)
                }
                is SaleType.Assurance, is SaleType.Carnet -> {
                    salesRepository.finalizeVOSale(completeSale)
                }
                else -> {
                    salesRepository.finalizeComptantSale(completeSale)
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

    /**
     * Reset after sale completion (after printing receipt or user declines)
     * Clears finalized sale, cart, customer, and resets to default state
     */
    fun resetAfterSale() {
        _saleFinalized.value = null
        resetCart()
    }

    private fun resetCart() {
        // If in prevente mode, keep status as PROCESSING
        val statut = if (_isPrevente.value == true) SalesStatut.PROCESSING else SalesStatut.ACTIVE
        _currentSale.value = Sale(statut = statut)
        _selectedCustomer.value = null
        _clientTiersPayants.value = emptyList()
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
                // Comptant - no additional validation here
                // Customer validation for Comptant is done in validateCustomerForComptant()
            }
        }

        return true
    }

    /**
     * Validate customer requirement for Comptant sales
     * Customer is mandatory for Comptant sales in these cases:
     * 1. Sale with "avoir" (unserved products due to stock shortage)
     *    - When quantity sold < quantity requested for any product
     * 2. Deferred payment (client doesn't pay the full amount)
     *    - When total paid < sale amount
     *
     * @param payments List of payments
     * @param saleAmount Total sale amount
     * @return true if validation passes, false otherwise
     */
    private fun validateCustomerForComptant(
        payments: List<com.kobe.warehouse.sales.data.model.Payment>,
        saleAmount: Int
    ): Boolean {
        val totalPaid = payments.sumOf { it.paidAmount ?: 0 }

        // Check for deferred payment (client doesn't pay the full amount)
        val hasDeferredPayment = totalPaid < saleAmount

        // Check for "avoir" (unserved products due to stock shortage)
        // This happens when quantity sold < quantity initially requested
        val sale = _currentSale.value
        val hasAvoir = sale?.salesLines?.any { line ->
            // If quantitySold < quantityRequested, there's an "avoir"
            line.quantitySold < line.quantityRequested
        } ?: false

        // Customer is required if there's deferred payment or avoir
        if ((hasDeferredPayment || hasAvoir) && _selectedCustomer.value == null) {
            _customerValidationError.value = when {
                hasDeferredPayment && hasAvoir ->
                    "Client obligatoire pour une vente avec paiement différé et avoir (produits non servis)"
                hasDeferredPayment ->
                    "Client obligatoire pour une vente avec paiement différé"
                hasAvoir ->
                    "Client obligatoire pour une vente avec avoir (produits non servis)"
                else -> "Client obligatoire"
            }
            return false
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
                // TODO: Convert TiersPayant to ClientTiersPayant when restoring
                // For now, tiers payants need to be added manually after changing type
            }
            is SaleType.Carnet -> {
                _selectedCustomer.value = saleType.saleCustomer
            }
        }

        _currentSale.value = sale.copy(
            customer = saleType.getCustomer()
        )
    }

    // ========================================
    // Ayant Droit Management (Assurance only)
    // ========================================

    private val _selectedAyantDroit = MutableLiveData<Customer?>()
    val selectedAyantDroit: LiveData<Customer?> = _selectedAyantDroit

    fun selectAyantDroit(ayantDroit: Customer) {
        _selectedAyantDroit.value = ayantDroit
    }

    fun removeAyantDroit() {
        _selectedAyantDroit.value = null
    }

    // ========================================
    // Tiers Payant Management
    // ========================================

    fun updateTiersPayantNumBon(tiersPayant: com.kobe.warehouse.sales.data.model.ClientTiersPayant, numBon: String) {
        when (_currentSaleType.value) {
            is SaleType.Assurance -> {
                // For Assurance: Update clientTiersPayants (user-selected for this sale)
                val current = _clientTiersPayants.value ?: emptyList()
                _clientTiersPayants.value = current.map {
                    if (it.tiersPayantId == tiersPayant.tiersPayantId) {
                        it.copy(numBon = numBon)
                    } else {
                        it
                    }
                }
            }
            is SaleType.Carnet -> {
                // For Carnet: Update customer.tiersPayants (pre-configured)
                val customer = _selectedCustomer.value ?: return
                val updatedTiersPayants = customer.tiersPayants.map {
                    if (it.tiersPayantId == tiersPayant.tiersPayantId) {
                        it.copy(numBon = numBon)
                    } else {
                        it
                    }
                }
                // Update customer with new tiersPayants list
                _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
            }
            else -> {
                // COMPTANT - no tiers payants
            }
        }
    }

    fun updateTiersPayantTaux(tiersPayant: com.kobe.warehouse.sales.data.model.ClientTiersPayant, newTaux: Int) {
        val customer = _selectedCustomer.value ?: return
        val updatedTiersPayants = customer.tiersPayants.map {
            if (it.tiersPayantId == tiersPayant.tiersPayantId) {
                it.copy(taux = newTaux)
            } else {
                it
            }
        }
        _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
    }

    fun removeTiersPayant(tiersPayant: com.kobe.warehouse.sales.data.model.ClientTiersPayant) {
        val customer = _selectedCustomer.value ?: return
        val updatedTiersPayants = customer.tiersPayants.filter {
            it.tiersPayantId != tiersPayant.tiersPayantId
        }
        _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
    }

    fun addTiersPayant(tiersPayant: com.kobe.warehouse.sales.data.model.ClientTiersPayant) {
        val customer = _selectedCustomer.value ?: return
        val updatedTiersPayants = (customer.tiersPayants ?: emptyList()) + tiersPayant
        _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
    }

    // ========================================
    // Available Tiers Payants (for adding new)
    // ========================================

    private val _availableTiersPayants = MutableLiveData<List<TiersPayant>>()
    val availableTiersPayants: LiveData<List<TiersPayant>> = _availableTiersPayants

    /**
     * Load available tiers payants for selection
     * Fetches from backend and filters by type (ASSURANCE only)
     */
    fun loadAvailableTiersPayants() {
        viewModelScope.launch {
            try {
                // Create TiersPayantRepository instance
                val retrofit = com.kobe.warehouse.sales.utils.ApiClient.create(tokenManager = tokenManager)
                val tiersPayantApi = retrofit.create(com.kobe.warehouse.sales.data.api.TiersPayantApiService::class.java)
                val tiersPayantRepository = com.kobe.warehouse.sales.data.repository.TiersPayantRepository(tiersPayantApi)

                // Fetch tiers payants of type ASSURANCE
                val result = tiersPayantRepository.searchTiersPayants(search = "", type = "ASSURANCE")
                result.fold(
                    onSuccess = { tiersPayants ->
                        // Filter enabled only
                        _availableTiersPayants.value = tiersPayants.filter { it.isEnabled() }
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur chargement tiers payants : ${error.message}"
                        _availableTiersPayants.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erreur : ${e.message}"
                _availableTiersPayants.value = emptyList()
            }
        }
    }

    // ========================================
    // Ayants Droits List (for selection)
    // ========================================

    private val _ayantDroitsList = MutableLiveData<List<Customer>>()
    val ayantDroitsList: LiveData<List<Customer>> = _ayantDroitsList

    /**
     * Load ayants droits for a customer
     * Fetches from backend
     */
    fun loadAyantDroits(customerId: Int) {
        viewModelScope.launch {
            try {
                val result = customerRepository.getAyantDroits(customerId)
                result.fold(
                    onSuccess = { ayantDroits ->
                        _ayantDroitsList.value = ayantDroits
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur chargement ayants droits : ${error.message}"
                        _ayantDroitsList.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erreur : ${e.message}"
                _ayantDroitsList.value = emptyList()
            }
        }
    }

    // ========================================
    // Stock Validation
    // ========================================

    /**
     * Validate stock availability for a product
     * Checks stock quantity, maximum allowed, and deconditioning requirements
     *
     * @param product Product to validate
     * @param requestedQuantity Quantity user wants to add
     * @param userHasForceStockPermission Whether current user can force stock
     * @return StockValidationResult with validation status and messages
     */
    fun validateStock(
        product: Product,
        requestedQuantity: Int,
        userHasForceStockPermission: Boolean
    ): com.kobe.warehouse.sales.domain.validation.StockValidationResult {
        val currentSale = _currentSale.value ?: Sale()

        // Get quantity already in cart for this product
        val quantityInCart = currentSale.salesLines
            .filter { it.produitId == product.id }
            .sumOf { it.quantitySold }

        val totalQuantity = quantityInCart + requestedQuantity
        val availableStock = product.totalQuantity

        // Check 1: Stock insuffisant
        if (availableStock < requestedQuantity) {
            return if (userHasForceStockPermission) {
                com.kobe.warehouse.sales.domain.validation.StockValidationResult(
                    status = com.kobe.warehouse.sales.domain.validation.StockValidationStatus.INSUFFICIENT_STOCK_CAN_FORCE,
                    product = product,
                    requestedQuantity = requestedQuantity,
                    availableStock = availableStock,
                    quantityInCart = quantityInCart
                )
            } else {
                com.kobe.warehouse.sales.domain.validation.StockValidationResult(
                    status = com.kobe.warehouse.sales.domain.validation.StockValidationStatus.INSUFFICIENT_STOCK_BLOCKED,
                    product = product,
                    requestedQuantity = requestedQuantity,
                    availableStock = availableStock,
                    quantityInCart = quantityInCart
                )
            }
        }

        // Check 2: Déconditionnement (if product has packaging and quantity is not a multiple)
        product.itemQty?.let { packagingSize ->
            if (packagingSize > 1 && requestedQuantity % packagingSize != 0) {
                return com.kobe.warehouse.sales.domain.validation.StockValidationResult(
                    status = com.kobe.warehouse.sales.domain.validation.StockValidationStatus.REQUIRES_DECONDITIONING,
                    product = product,
                    requestedQuantity = requestedQuantity,
                    availableStock = availableStock,
                    quantityInCart = quantityInCart,
                    packagingSize = packagingSize
                )
            }
        }

        // All checks passed
        return com.kobe.warehouse.sales.domain.validation.StockValidationResult(
            status = com.kobe.warehouse.sales.domain.validation.StockValidationStatus.VALID,
            product = product,
            requestedQuantity = requestedQuantity,
            availableStock = availableStock,
            quantityInCart = quantityInCart
        )
    }

    // ========================================
    // Cart Actions with API (for saved sales)
    // ========================================

    /**
     * Check if current user has specific permission
     */
    fun checkUserPermission(permission: String): Boolean {
        return tokenManager.hasAuthority(permission)
    }

    /**
     * Update product quantity via API (for saved sales)
     * For new sales, use updateLineQuantity() instead
     */
    fun updateProductQuantityWithApi(saleLine: SaleLine, newQuantity: Int) {
        val sale = _currentSale.value ?: return

        // Check if sale is saved (has ID)
        if (sale.id == null || sale.saleId?.saleDate == null) {
            // Not saved yet, update locally
            updateLineQuantity(saleLine, newQuantity)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Build saleLineId from saleLine.id and sale date
                val saleLineId = SaleLineId(
                    id = saleLine.id ?: 0L,
                    saleDate = sale.saleId!!.saleDate
                )

                // Build saleCompositeId from sale
                val saleCompositeId = sale.saleId

                // Create updated SaleLine with required fields for API
                val updatedSaleLine = saleLine.copy(
                    quantityRequested = newQuantity,
                    quantitySold = newQuantity,
                    saleLineId = saleLineId,
                    saleCompositeId = saleCompositeId
                )

                android.util.Log.d("UnifiedSaleViewModel", "=== UPDATE QUANTITY ===")
                android.util.Log.d("UnifiedSaleViewModel", "saleLineId = $saleLineId")
                android.util.Log.d("UnifiedSaleViewModel", "saleCompositeId = $saleCompositeId")
                android.util.Log.d("UnifiedSaleViewModel", "quantityRequested = $newQuantity")

                // Pass natureVente to use correct endpoint (comptant vs assurance)
                val result = salesRepository.updateItemQuantity(updatedSaleLine, sale.natureVente)

                result.fold(
                    onSuccess = { updatedLine ->
                        // Update local state with API response
                        val updatedLines = sale.salesLines.map { existingLine ->
                            if (existingLine.id == updatedLine.id) updatedLine else existingLine
                        }
                        val newTotal = updatedLines.sumOf { it.salesAmount }

                        _currentSale.value = sale.copy(
                            salesLines = updatedLines.toMutableList(),
                            salesAmount = newTotal
                        )
                        _errorMessage.value = "Quantité mise à jour"
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur : ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erreur : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update product price via API (requires authorization if user doesn't have permission)
     * For new sales, update locally
     * NOTE: authUserId is tracked in audit but not sent to backend endpoint
     */
    fun updateProductPriceWithApi(saleLine: SaleLine, newPrice: Int, authUserId: Int? = null) {
        val sale = _currentSale.value ?: return

        // Check if sale is saved (has ID)
        if (sale.id == null || sale.saleId?.saleDate == null) {
            // Not saved yet, update locally
            val updatedLines = sale.salesLines.map { existingLine ->
                if (existingLine.produitId == saleLine.produitId) {
                    existingLine.copy(
                        regularUnitPrice = newPrice,
                        salesAmount = saleLine.quantitySold * newPrice
                    )
                } else {
                    existingLine
                }
            }
            val newTotal = updatedLines.sumOf { it.salesAmount }

            _currentSale.value = sale.copy(
                salesLines = updatedLines.toMutableList(),
                salesAmount = newTotal
            )
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create updated SaleLine with new price
                val updatedSaleLine = saleLine.copy(regularUnitPrice = newPrice)

                val result = salesRepository.updateItemPrice(updatedSaleLine)

                result.fold(
                    onSuccess = { updatedLine ->
                        // Update local state with API response
                        val updatedLines = sale.salesLines.map { existingLine ->
                            if (existingLine.id == updatedLine.id) updatedLine else existingLine
                        }
                        val newTotal = updatedLines.sumOf { it.salesAmount }

                        _currentSale.value = sale.copy(
                            salesLines = updatedLines.toMutableList(),
                            salesAmount = newTotal
                        )
                        _errorMessage.value = "Prix mis à jour"
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur : ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erreur : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete product line via API (requires authorization if user doesn't have permission)
     * For new sales, use removeLineFromCart()
     * NOTE: authUserId is tracked in audit but not sent to backend endpoint
     */
    fun deleteProductLineWithApi(saleLine: SaleLine, authUserId: Int? = null) {
        val sale = _currentSale.value ?: return

        // Check if sale is saved (has ID)
        if (sale.id == null || sale.saleId?.saleDate == null) {
            // Not saved yet, remove locally
            removeLineFromCart(saleLine)
            return
        }

        // SaleLine has composite key SaleLineId(id, saleDate)
        val saleLineId = saleLine.id ?: 0L
        val saleDate = sale.saleId.saleDate

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = salesRepository.deleteItem(saleLineId, saleDate)

                result.fold(
                    onSuccess = {
                        // Remove from local state
                        val updatedLines = sale.salesLines.filter { it.id != saleLine.id }
                        val newTotal = updatedLines.sumOf { it.salesAmount }

                        _currentSale.value = sale.copy(
                            salesLines = updatedLines.toMutableList(),
                            salesAmount = newTotal
                        )
                        _errorMessage.value = "Ligne supprimée"
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Erreur : ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Erreur : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
