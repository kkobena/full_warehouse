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
import com.kobe.warehouse.sales.data.model.Decondition
import com.kobe.warehouse.sales.data.model.Payment
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.model.Remise
import com.kobe.warehouse.sales.data.repository.DeconditionRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.RemiseRepository
import com.kobe.warehouse.sales.data.repository.SalesApiException
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.data.model.SaleType
import com.kobe.warehouse.sales.data.model.SalesStatut
import com.kobe.warehouse.sales.data.model.TiersPayant
import com.kobe.warehouse.sales.data.model.UpdateSaleInfo
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.ceil

/**
 * Unified Sale ViewModel
 *
 * Manages all types of sales (Comptant, Assurance, Carnet) with unified logic
 * Delegates type-specific operations to appropriate handlers
 */
/**
 * Event data for stock force or deconditionnement required by backend
 */
data class StockActionRequired(
    val product: Product,
    val quantity: Int,
    val errorMessage: String
)

/**
 * Event data for stock errors on quantity update (existing sale lines)
 */
data class QuantityUpdateStockError(
    val saleLine: SaleLine,
    val newQuantity: Int,
    val errorMessage: String
)

class UnifiedSaleViewModel(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val deconditionRepository: DeconditionRepository? = null,
    private val remiseRepository: RemiseRepository? = null
) : ViewModel(), ISaleViewModel {

    // ===== Backend Stock Error Events =====

    private val _stockForceRequired = MutableLiveData<StockActionRequired?>()
    val stockForceRequired: LiveData<StockActionRequired?> = _stockForceRequired

    private val _deconditionnementRequired = MutableLiveData<StockActionRequired?>()
    val deconditionnementRequired: LiveData<StockActionRequired?> = _deconditionnementRequired

    private val _quantityUpdateStockError = MutableLiveData<QuantityUpdateStockError?>()
    val quantityUpdateStockError: LiveData<QuantityUpdateStockError?> = _quantityUpdateStockError

    private val _quantityUpdateDeconditionnementRequired = MutableLiveData<QuantityUpdateStockError?>()
    val quantityUpdateDeconditionnementRequired: LiveData<QuantityUpdateStockError?> = _quantityUpdateDeconditionnementRequired

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
        if (query.length < 3) {
            _customerSearchResults.value = emptyList()
            return
        }

        _isSearchingCustomer.value = true
        val typeTiersPayant = when (_currentSaleType.value) {
            is SaleType.Assurance -> "ASSURANCE"
            is SaleType.Carnet -> "CARNET"
            else -> null
        }
        viewModelScope.launch {
            customerRepository.searchAssuredCustomers(query, typeTiersPayant).fold(
                onSuccess = { customers ->
                    _customerSearchResults.value = customers
                    _isSearchingCustomer.value = false
                },
                onFailure = { error ->
                    postServerError(error, "Erreur de recherche client")
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
        _currentSale.value?.copy(customer = customer)?.let {
            _currentSale.value = it
        }

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
        _currentSale.value?.copy(customer = null)?.let {
            _currentSale.value = it
        }
    }

    /**
     * Reset customer when changing sale type tab (unconditional).
     */
    fun resetCustomerForTypeChange() {
        _selectedCustomer.value = null
        _clientTiersPayants.value = emptyList()
        _customerSearchResults.value = emptyList()
        _customerValidationError.value = null
        _currentSale.value?.copy(customer = null, customerId = null, tiersPayants = mutableListOf(), thirdPartySaleLines = mutableListOf())?.let {
            _currentSale.value = it
        }
    }

    private val _customerValidationError = MutableLiveData<String?>()
    val customerValidationError: LiveData<String?> = _customerValidationError

    // ===== Insurance (Assurance) State =====

    private val _clientTiersPayants = MutableLiveData<List<ClientTiersPayant>>(emptyList())
    val clientTiersPayants: LiveData<List<ClientTiersPayant>> = _clientTiersPayants

    /**
     * Initialize clientTiersPayants directly from customer's tiers payants list.
     * Preserves all fields including id.
     */
    fun initClientTiersPayantsFromCustomer(tiersPayants: List<ClientTiersPayant>) {
        _clientTiersPayants.value = tiersPayants.toList()
    }

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
            priorite = PrioriteTiersPayant.fromValue(priorite)
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
        if (query.length < 3) {
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
                    postServerError(error, "Erreur de recherche")
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
            quantityRequested = quantity,
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
                        _currentRemise.value = updatedSale.remise
                        _currentSale.value = updatedSale
                        _products.value = emptyList() // Clear search
                        _errorMessage.value = "Produit ajouté"
                    },
                    onFailure = { error ->
                        handleAddProductError(error, product, quantity, forceStock)
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle errors from addProductToCart API calls
     * Detects stock and deconditionnement errors from backend errorKey
     * If forceStock was already true, don't show the dialog again (backend rejected despite force)
     */
    private fun handleAddProductError(error: Throwable, product: Product, quantity: Int, forceStock: Boolean) {
        if (error is SalesApiException) {
            when (error.errorKey) {
                "customerInsuranceCreditLimit" -> {
                    handlePlafondWarning(error)
                    return
                }
                "stock" -> {
                    if (forceStock) {
                        // Already tried with forceStock=true, backend still rejects
                        postServerError(error, "Impossible de forcer le stock")
                        return
                    }
                    val hasForceStockPermission = tokenManager.hasAuthority("PR_FORCE_STOCK")
                    if (hasForceStockPermission) {
                        _stockForceRequired.value = StockActionRequired(
                            product = product,
                            quantity = quantity,
                            errorMessage = error.message ?: "Stock insuffisant"
                        )
                    } else {
                        _errorMessage.value = "Stock insuffisant. Vous n'avez pas la permission de forcer le stock."
                    }
                    return
                }
                "stockChInsufisant" -> {
                    // Deconditioning dialog always shown, regardless of forceStock
                    _deconditionnementRequired.value = StockActionRequired(
                        product = product,
                        quantity = quantity,
                        errorMessage = error.message ?: "Déconditionnement nécessaire"
                    )
                    return
                }
            }
        }
        postServerError(error)
    }

    /**
     * Retry adding product with forceStock=true (after user confirmation)
     */
    fun retryWithForceStock(product: Product, quantity: Int) {
        _stockForceRequired.value = null // Clear the event
        addProductToCart(product, quantity, forceStock = true)
    }

    /**
     * Perform deconditionnement flow:
     * 1. Resolve parentId (from product or via API)
     * 2. Fetch CH parent product
     * 3. Verify CH parent has stock
     * 4. Calculate qty to decondition
     * 5. Call decondition API
     * 6. Retry the original add product operation
     */
    fun performDeconditionnement(product: Product, quantity: Int) {
        _deconditionnementRequired.value = null // Clear the event

        if (deconditionRepository == null) {
            _errorMessage.value = "Service de déconditionnement non disponible"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Step 1: Resolve parentId
                val parentId = product.parentId
                if (parentId == null) {
                    // Try to fetch product details to get parentId
                    val productResult = productRepository.getProductById(product.id)
                    val fetchedProduct = productResult.getOrNull()
                    if (fetchedProduct?.parentId == null) {
                        _errorMessage.value = "Ce produit n'a pas de conditionnement parent (CH)"
                        return@launch
                    }
                    doDeconditionnement(fetchedProduct.parentId, product, quantity)
                } else {
                    doDeconditionnement(parentId, product, quantity)
                }
            } catch (e: Exception) {
                postServerError(e, "Erreur de déconditionnement")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Execute the deconditionnement with resolved parentId
     */
    private suspend fun doDeconditionnement(parentId: Long, product: Product, quantity: Int) {
        // Step 2: Fetch CH parent product to check stock
        val parentResult = productRepository.getProductById(parentId)
        val parentProduct = parentResult.getOrNull()
        if (parentProduct == null) {
            _errorMessage.value = "Impossible de récupérer le produit parent (CH)"
            return
        }

        // Step 3: Verify CH parent has stock
        if (parentProduct.totalQuantity <= 0) {
            _errorMessage.value = "Stock du conditionnement parent (CH) insuffisant"
            return
        }

        // Step 4: Calculate qty to decondition
        val itemQty = product.itemQty ?: 1
        val qtyToDecondition = if (itemQty > 0) {
            ceil(quantity.toDouble() / itemQty.toDouble()).toInt()
        } else {
            1
        }

        // Step 5: Call decondition API
        val decondition = Decondition(
            qtyMvt = qtyToDecondition,
            produitId = parentId
        )
        val deconditionResult = deconditionRepository!!.create(decondition)

        deconditionResult.fold(
            onSuccess = {
                // Step 6: Retry with forceStock=true to avoid another stock rejection
                _isLoading.value = false // Reset loading before retry (addProductToCart sets it again)
                addProductToCart(product, quantity, forceStock = true)
            },
            onFailure = { error ->
                postServerError(error, "Erreur de déconditionnement")
            }
        )
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


        if (cassierId == null) {
            // Fallback: try to load account from API
            val accountResult = authRepository.getAccount()
            val account = accountResult.getOrNull()
            cassierId = account?.id
            if (cassierId != null) {
                tokenManager.storeUserId(cassierId)
            }
        }

        if (cassierId == null) {
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
                // For VO sales: reload full sale from backend to get updated
                // partTiersPayant, thirdPartySaleLines, amountToBePaid, etc.
                if (saleType is SaleType.Assurance || saleType is SaleType.Carnet) {
                    val saleId = currentSale.id
                    val saleDate = currentSale.saleId?.saleDate
                    if (saleId != null && saleDate != null) {
                        return salesRepository.getSaleById(saleId, saleDate)
                    }
                }

                // For Comptant: update locally (no TP calculations needed)
                val existingIndex = currentSale.salesLines.indexOfFirst { it.produitId == addedLine.produitId }
                val updatedLines = if (existingIndex >= 0) {
                    currentSale.salesLines.toMutableList().apply {
                        set(existingIndex, addedLine)
                    }
                } else {
                    currentSale.salesLines.toMutableList().apply {
                        add(addedLine)
                    }
                }
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
        val currentSale = _currentSale.value ?: run {
            _errorMessage.value = "Aucune vente en cours"
            return
        }
        val saleType = _currentSaleType.value
        val hasSaleId = currentSale.id != null && currentSale.saleId?.saleDate != null

        if (hasSaleId) {
            // Sale exists on backend: call backend then reload full sale
            val updatedLine = line.copy(
                quantityRequested = newQuantity,
                quantitySold = newQuantity,
                salesAmount = newQuantity * line.regularUnitPrice
            )
            viewModelScope.launch {
                val natureVente = when (saleType) {
                    is SaleType.Assurance -> "ASSURANCE"
                    is SaleType.Carnet -> "CARNET"
                    else -> "COMPTANT"
                }
                salesRepository.updateItemQuantity(updatedLine, natureVente).fold(
                    onSuccess = {
                        reloadCurrentSale()
                    },
                    onFailure = { error ->
                        postServerError(error, "Erreur mise à jour quantité")
                    }
                )
            }
        } else {
            // Local sale (not yet saved): update locally
            val updatedLines = currentSale.salesLines.map { existingLine ->
                if (existingLine.produitId == line.produitId) {
                    existingLine.copy(
                        quantitySold = newQuantity,
                        salesAmount = newQuantity * existingLine.regularUnitPrice
                    )
                } else {
                    existingLine
                }
            }.filter { it.quantityRequested > 0 }

            val newTotal = updatedLines.sumOf { it.salesAmount }
            _currentSale.value = currentSale.copy(
                salesLines = updatedLines.toMutableList(),
                salesAmount = newTotal
            )
        }
    }

    fun removeLineFromCart(line: SaleLine) {
        val currentSale = _currentSale.value ?: run {
            _errorMessage.value = "Aucune vente en cours"
            return
        }
        val saleType = _currentSaleType.value
        val saleLineId = line.saleLineId?.id
        val saleDate = currentSale.saleId?.saleDate

        if (saleLineId != null && saleDate != null) {
            // Sale exists on backend: call backend then reload full sale
            viewModelScope.launch {
                salesRepository.deleteItem(saleLineId, saleDate).fold(
                    onSuccess = {
                        reloadCurrentSale()
                    },
                    onFailure = { error ->
                        postServerError(error, "Erreur suppression ligne")
                    }
                )
            }
        } else {
            // Local sale (not yet saved): update locally
            val updatedLines = currentSale.salesLines.filter { it.produitId != line.produitId }
            val newTotal = updatedLines.sumOf { it.salesAmount }
            _currentSale.value = currentSale.copy(
                salesLines = updatedLines.toMutableList(),
                salesAmount = newTotal
            )
        }
    }

    /**
     * Reload the current sale from backend to get updated calculated fields
     * (partTiersPayant, thirdPartySaleLines, amountToBePaid, taxAmount, etc.)
     */
    private fun reloadCurrentSale() {
        val currentSale = _currentSale.value ?: return
        val saleId = currentSale.id ?: return
        val saleDate = currentSale.saleId?.saleDate ?: return

        viewModelScope.launch {
            salesRepository.getSaleById(saleId, saleDate).fold(
                onSuccess = { reloadedSale ->
                    _currentRemise.value = reloadedSale.remise
                    _currentSale.value = reloadedSale
                },
                onFailure = { error ->
                    postServerError(error, "Erreur rechargement vente")
                }
            )
        }
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
            val result = when (_currentSaleType.value) {
                is SaleType.Assurance, is SaleType.Carnet -> salesRepository.putAssuranceSaleOnHold(sale)
                else -> salesRepository.putCashSaleOnHold(sale)
            }
            result.fold(
                onSuccess = { savedSale ->
                    _saleSaved.value = savedSale
                    resetCart()
                },
                onFailure = { error ->
                    if (error is SalesApiException && isPlafondError(error)) {
                        handlePlafondWarning(error)
                    } else {
                        postServerError(error, "Erreur de sauvegarde")
                    }
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

        val saleId = sale.saleId
        if (saleId == null || saleId.id == 0L || saleId.saleDate.isEmpty()) {
            _errorMessage.value = "La prévente n'a pas encore été sauvegardée"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = when (_currentSaleType.value) {
                is SaleType.Assurance, is SaleType.Carnet -> {
                    salesRepository.transformAssurancePrevente(saleId)
                }
                else -> {
                    salesRepository.transformComptantPrevente(saleId)
                }
            }

            result.fold(
                onSuccess = { returnedSaleId ->
                    _isLoading.value = false
                    _isPrevente.value = false
                    // Use saleFinalized so Activity shows print dialog then switches to Comptant
                    _saleFinalized.value = sale
                },
                onFailure = { error ->
                    _isLoading.value = false
                    postServerError(error, "Erreur de finalisation")
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
                    // Set sale type BEFORE currentSale so observers see the correct type
                    // when updateTotalDetails is triggered by the currentSale observer
                    val saleType = when (sale.natureVente) {
                        "ASSURANCE" -> SaleType.Assurance(sale.customer, emptyList())
                        "CARNET" -> SaleType.Carnet(sale.customer)
                        else -> SaleType.Comptant
                    }
                    _currentSaleType.value = saleType
                    updateCustomerRequired()

                    // Detect if it's a prevente (statut PENDING or PROCESSING)
                    _isPrevente.value = sale.statut == SalesStatut.PENDING || sale.statut == SalesStatut.PROCESSING

                    _selectedCustomer.value = sale.customer

                    // Restore tiers payants from sale
                    _clientTiersPayants.value = sale.tiersPayants?.toList() ?: emptyList()

                    // Restore remise from sale
                    _currentRemise.value = sale.remise

                    // Set currentSale last — its observer uses currentSaleType for TP display
                    _currentSale.value = sale
                },
                onFailure = { error ->
                    postServerError(error, "Erreur de chargement")
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

    override fun finalizeSale(payments: List<Payment>, montantVerse: Int, montantRendu: Int) {
        if (!validateSaleBeforeFinalize()) {
            return
        }

        val currentSaleValue = _currentSale.value ?: run {
            _errorMessage.value = "Aucune vente en cours"
            return
        }

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

            if (cassierId == null) {

                val accountResult = authRepository.getAccount()
                val account = accountResult.getOrNull()

                cassierId = account?.id
                if (cassierId == null) {
                    _errorMessage.value = "Impossible de récupérer l'ID du caissier. Veuillez vous reconnecter."
                    _isLoading.value = false
                    return@launch
                }

                tokenManager.storeUserId(cassierId)
            }



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
                    _isLoading.value = false
                    if (error is SalesApiException && isPlafondError(error)) {
                        handlePlafondWarning(error)
                    } else {
                        postServerError(error, "Erreur de finalisation")
                    }
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
        // Reset plafond warning state
        plafondAlreadyShown = false
        _plafondWarning.value = null
        // Reset remise
        clearRemise()
    }

    /**
     * Handle plafond vente warning from backend.
     * The backend saves the sale with capped amounts (noRollbackFor PlafondVenteException),
     * so we show a warning and reload the sale to get updated amounts.
     *
     * For sale creation (no local sale ID yet), uses saleId from the error payload.
     */
    private fun handlePlafondWarning(error: SalesApiException) {
        if (!plafondAlreadyShown) {
            plafondAlreadyShown = true
            _plafondWarning.value = error.message ?: "Plafond de vente atteint"
        }

        val currentSale = _currentSale.value
        val localSaleId = currentSale?.id
        val localSaleDate = currentSale?.saleId?.saleDate

        if (localSaleId != null && localSaleDate != null) {
            // Sale already exists locally — reload it
            reloadCurrentSale()
        } else if (error.saleId != null && error.saleId.id != 0L && error.saleId.saleDate.isNotEmpty()) {
            // Sale was just created by backend (plafond on first line) — load via payload saleId
            viewModelScope.launch {
                salesRepository.getSaleById(error.saleId.id, error.saleId.saleDate).fold(
                    onSuccess = { reloadedSale ->
                        _currentSale.value = reloadedSale
                    },
                    onFailure = { reloadError ->
                        postServerError(reloadError, "Erreur rechargement vente")
                    }
                )
            }
        }
    }

    /**
     * Check if error is a plafond vente warning (customerInsuranceCreditLimit)
     */
    private fun isPlafondError(error: Throwable): Boolean {
        return error is SalesApiException && error.errorKey == "customerInsuranceCreditLimit"
    }

    // ===== Validation & Error Handling =====

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /** Server/network errors that need a prominent dialog (not just a snackbar) */
    private val _serverError = MutableLiveData<String?>()
    val serverError: LiveData<String?> = _serverError

    private val _stockValidationError = MutableLiveData<String?>()
    val stockValidationError: LiveData<String?> = _stockValidationError

    // ===== Plafond Vente Warning =====
    private val _plafondWarning = MutableLiveData<String?>()
    val plafondWarning: LiveData<String?> = _plafondWarning
    private var plafondAlreadyShown = false

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearServerError() {
        _serverError.value = null
    }

    /**
     * Convert an exception to a user-friendly error message.
     * Handles network errors, API errors, and unknown exceptions.
     */
    private fun userFriendlyMessage(error: Throwable, fallback: String = "Une erreur est survenue"): String {
        return when (error) {
            is ConnectException -> "Impossible de se connecter au serveur. Vérifiez votre connexion réseau."
            is SocketTimeoutException -> "Le serveur met trop de temps à répondre. Réessayez."
            is UnknownHostException -> "Serveur introuvable. Vérifiez l'adresse du serveur."
            is SalesApiException -> error.message ?: fallback
            else -> error.message?.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    /**
     * Post an error to the serverError LiveData (shown as a dialog in the Activity).
     * Use this for backend/network errors that must not be missed.
     */
    private fun postServerError(error: Throwable, fallback: String = "Une erreur est survenue") {
        _serverError.value = userFriendlyMessage(error, fallback)
    }

    fun clearSaleSaved() {
        _saleSaved.value = null
    }

    fun clearStockValidationError() {
        _stockValidationError.value = null
    }

    fun clearCustomerValidationError() {
        _customerValidationError.value = null
    }

    fun clearPlafondWarning() {
        _plafondWarning.value = null
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
            is SaleType.Assurance, is SaleType.Carnet -> {
                // Check the ViewModel's tiers payants list (not the SaleType's which may be empty)
                if (_clientTiersPayants.value.isNullOrEmpty()) {
                    _errorMessage.value = "Au moins un tiers payant est requis pour ${saleType.getDisplayName()}"
                    return false
                }
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
      payments: List<Payment>,
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
        val currentSale = _currentSale.value ?: return
        val saleId = currentSale.saleId ?: return

        viewModelScope.launch {
            val updateSaleInfo = UpdateSaleInfo(
                id = saleId,
                value = ayantDroit.id
            )
            salesRepository.addAyantDroitToSale(updateSaleInfo).fold(
                onSuccess = {
                    _selectedAyantDroit.value = ayantDroit
                    reloadCurrentSale()
                },
                onFailure = { error ->
                    postServerError(error, "Erreur ajout ayant droit")
                }
            )
        }
    }

    fun removeAyantDroit() {
        _selectedAyantDroit.value = null
    }

    // ========================================
    // Remise (Predefined Discount) Management
    // ========================================

    private val _remises = MutableLiveData<List<Remise>>()
    val remises: LiveData<List<Remise>> = _remises

    private val _currentRemise = MutableLiveData<Remise?>()
    val currentRemise: LiveData<Remise?> = _currentRemise

    /**
     * Load available remises from backend (cached)
     */
    fun loadRemises() {
        if (remiseRepository == null) return
        viewModelScope.launch {
            remiseRepository.getRemises().fold(
                onSuccess = { _remises.value = it },
                onFailure = { _remises.value = emptyList() }
            )
        }
    }

    /**
     * Apply a predefined remise to the current sale
     * Calls backend PUT /api/sales/{comptant|assurance}/add-remise
     * Then reloads sale to get recalculated amounts
     */
    fun applyRemise(remise: Remise) {
        val currentSale = _currentSale.value ?: return
        val saleId = currentSale.saleId ?: return

        val updateSaleInfo = UpdateSaleInfo(id = saleId, value = remise.id)
        val saleType = when (_currentSaleType.value) {
            is SaleType.Assurance -> "ASSURANCE"
            is SaleType.Carnet -> "CARNET"
            else -> "COMPTANT"
        }

        viewModelScope.launch {
            salesRepository.addDiscountToSale(updateSaleInfo, saleType).fold(
                onSuccess = {
                    _currentRemise.value = remise
                    reloadCurrentSale()
                },
                onFailure = { error ->
                    postServerError(error, "Erreur application remise")
                }
            )
        }
    }

    /**
     * Remove the current remise from the sale
     * Calls backend DELETE /api/sales/{comptant|assurance}/remove-remise/{id}/{date}
     */
    fun removeRemise() {
        val currentSale = _currentSale.value ?: return
        val saleId = currentSale.id ?: return
        val saleDate = currentSale.saleId?.saleDate ?: return

        val isComptant = _currentSaleType.value is SaleType.Comptant

        viewModelScope.launch {
            val result = if (isComptant) {
                salesRepository.removeDiscountFromSale(saleId, saleDate)
            } else {
                salesRepository.removeDiscountFromAssuranceSale(saleId, saleDate)
            }
            result.fold(
                onSuccess = {
                    _currentRemise.value = null
                    reloadCurrentSale()
                },
                onFailure = { error ->
                    postServerError(error, "Erreur suppression remise")
                }
            )
        }
    }

    /**
     * Clear remise state (called when resetting sale)
     */
    fun clearRemise() {
        _currentRemise.value = null
    }

    // ========================================
    // Tiers Payant Management
    // ========================================

    fun updateTiersPayantNumBon(tiersPayant: ClientTiersPayant, numBon: String) {
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
                // For Carnet: Update both customer.tiersPayants and _clientTiersPayants
                val customer = _selectedCustomer.value ?: return
                val updatedTiersPayants = customer.tiersPayants.map {
                    if (it.tiersPayantId == tiersPayant.tiersPayantId) {
                        it.copy(numBon = numBon)
                    } else {
                        it
                    }
                }
                _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)

                // Also update _clientTiersPayants (used when submitting sale to backend)
                val current = _clientTiersPayants.value ?: emptyList()
                _clientTiersPayants.value = current.map {
                    if (it.tiersPayantId == tiersPayant.tiersPayantId) {
                        it.copy(numBon = numBon)
                    } else {
                        it
                    }
                }
            }
            else -> {
                // COMPTANT - no tiers payants
            }
        }
    }

    fun updateTiersPayantTaux(tiersPayant: ClientTiersPayant, newTaux: Int) {
        val currentSale = _currentSale.value
        val saleId = currentSale?.saleId

        if (saleId != null && saleId.id > 0 && tiersPayant.id != null) {
            _isLoading.value = true
            viewModelScope.launch {
                salesRepository.updateTiersPayantTaux(saleId, tiersPayant.id.toInt(), newTaux).fold(
                    onSuccess = {
                        salesRepository.getSaleById(saleId.id, saleId.saleDate).fold(
                            onSuccess = { refreshedSale ->
                                _currentSale.value = refreshedSale
                                _clientTiersPayants.value = refreshedSale.tiersPayants ?: emptyList()
                            },
                            onFailure = { error -> postServerError(error, "Erreur rechargement vente") }
                        )
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        postServerError(error, "Erreur modification taux")
                        _isLoading.value = false
                    }
                )
            }
        } else {
            val current = _clientTiersPayants.value ?: emptyList()
            _clientTiersPayants.value = current.map {
                if (it.tiersPayantId == tiersPayant.tiersPayantId) {
                    it.copy(taux = newTaux)
                } else {
                    it
                }
            }
        }
    }

    fun removeTiersPayant(tiersPayant: ClientTiersPayant) {
        val currentSale = _currentSale.value
        val saleId = currentSale?.saleId

        if (saleId != null && saleId.id > 0 && tiersPayant.id != null) {
            _isLoading.value = true
            viewModelScope.launch {
                salesRepository.removeTiersPayantFromSale(tiersPayant.id, saleId.id, saleId.saleDate).fold(
                    onSuccess = {
                        salesRepository.getSaleById(saleId.id, saleId.saleDate).fold(
                            onSuccess = { refreshedSale ->
                                _currentSale.value = refreshedSale
                                _clientTiersPayants.value = refreshedSale.tiersPayants ?: emptyList()
                            },
                            onFailure = { error -> postServerError(error, "Erreur rechargement vente") }
                        )
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        postServerError(error, "Erreur suppression tiers payant")
                        _isLoading.value = false
                    }
                )
            }
        } else {
            val current = _clientTiersPayants.value ?: emptyList()
            _clientTiersPayants.value = current.filter {
                it.tiersPayantId != tiersPayant.tiersPayantId
            }
        }
    }

    fun addTiersPayant(tiersPayant: ClientTiersPayant) {
        val currentSale = _currentSale.value
        val saleId = currentSale?.saleId

        if (saleId != null && saleId.id > 0) {
            _isLoading.value = true
            viewModelScope.launch {
                salesRepository.addTiersPayantToSale(saleId.id, saleId.saleDate, tiersPayant).fold(
                    onSuccess = {
                        salesRepository.getSaleById(saleId.id, saleId.saleDate).fold(
                            onSuccess = { refreshedSale ->
                                _currentSale.value = refreshedSale
                                _clientTiersPayants.value = refreshedSale.tiersPayants ?: emptyList()
                            },
                            onFailure = { error -> postServerError(error, "Erreur rechargement vente") }
                        )
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        postServerError(error, "Erreur ajout tiers payant")
                        _isLoading.value = false
                    }
                )
            }
        } else {
            val current = _clientTiersPayants.value ?: emptyList()
            _clientTiersPayants.value = current + tiersPayant
        }
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
                        postServerError(error, "Erreur chargement tiers payants")
                        _availableTiersPayants.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
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
                        postServerError(error, "Erreur chargement ayants droits")
                        _ayantDroitsList.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
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

        // Note: Déconditionnement is handled by backend errors (errorKey='stockChInsufisant')
        // No client-side deconditioning check needed — the backend decides when deconditioning is required

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
        val sale = _currentSale.value ?: run {
            _errorMessage.value = "Aucune vente en cours"
            return
        }

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
                    saleDate = sale.saleId.saleDate
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



                // Pass natureVente to use correct endpoint (comptant vs assurance)
                val result = salesRepository.updateItemQuantity(updatedSaleLine, sale.natureVente)

                result.fold(
                    onSuccess = { updatedLine ->
                        val isVO = _currentSaleType.value !is SaleType.Comptant
                        if (isVO) {
                            // VO sales: reload full sale to get updated TP amounts
                            reloadCurrentSale()
                        } else {
                            // Comptant: update locally (no TP amounts to refresh)
                            val updatedLines = sale.salesLines.map { existingLine ->
                                if (existingLine.id == updatedLine.id) updatedLine else existingLine
                            }
                            _currentSale.value = sale.copy(
                                salesLines = updatedLines.toMutableList(),
                                salesAmount = updatedLines.sumOf { it.salesAmount }
                            )
                        }
                        _errorMessage.value = "Quantité mise à jour"
                    },
                    onFailure = { error ->
                        if (error is SalesApiException) {
                            when (error.errorKey) {
                                "customerInsuranceCreditLimit" -> {
                                    handlePlafondWarning(error)
                                }
                                "stock" -> {
                                    val hasForceStockPermission = tokenManager.hasAuthority("PR_FORCE_STOCK")
                                    if (hasForceStockPermission) {
                                        _quantityUpdateStockError.value = QuantityUpdateStockError(
                                            saleLine = saleLine,
                                            newQuantity = newQuantity,
                                            errorMessage = error.message ?: "Stock insuffisant"
                                        )
                                    } else {
                                        _errorMessage.value = "Stock insuffisant. Vous n'avez pas la permission de forcer le stock."
                                    }
                                }
                                "stockChInsufisant" -> {
                                    _quantityUpdateDeconditionnementRequired.value = QuantityUpdateStockError(
                                        saleLine = saleLine,
                                        newQuantity = newQuantity,
                                        errorMessage = error.message ?: "Déconditionnement nécessaire"
                                    )
                                }
                                else -> postServerError(error)
                            }
                        } else {
                            postServerError(error)
                        }
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Retry quantity update with forceStock=true
     */
    fun retryQuantityUpdateWithForceStock(saleLine: SaleLine, newQuantity: Int) {
        _quantityUpdateStockError.value = null
        val sale = _currentSale.value ?: run {
            _errorMessage.value = "Aucune vente en cours"
            return
        }

        if (sale.id == null || sale.saleId?.saleDate == null) {
            _errorMessage.value = "La vente n'a pas encore été sauvegardée"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val saleLineId = SaleLineId(
                    id = saleLine.id ?: 0L,
                    saleDate = sale.saleId!!.saleDate
                )
                val saleCompositeId = sale.saleId

                val updatedSaleLine = saleLine.copy(
                    quantityRequested = newQuantity,
                    quantitySold = newQuantity,
                    saleLineId = saleLineId,
                    saleCompositeId = saleCompositeId,
                    forceStock = true
                )

                val result = salesRepository.updateItemQuantity(updatedSaleLine, sale.natureVente)

                result.fold(
                    onSuccess = { updatedLine ->
                        val isVO = _currentSaleType.value !is SaleType.Comptant
                        if (isVO) {
                            reloadCurrentSale()
                        } else {
                            val updatedLines = sale.salesLines.map { existingLine ->
                                if (existingLine.id == updatedLine.id) updatedLine else existingLine
                            }
                            _currentSale.value = sale.copy(
                                salesLines = updatedLines.toMutableList(),
                                salesAmount = updatedLines.sumOf { it.salesAmount }
                            )
                        }
                        _errorMessage.value = "Quantité mise à jour (stock forcé)"
                    },
                    onFailure = { error ->
                        postServerError(error)
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Perform deconditionnement then retry quantity update
     * Flow: fetch product → resolve parentId → decondition → retry update
     */
    fun performDeconditionnementForQuantityUpdate(saleLine: SaleLine, newQuantity: Int) {
        _quantityUpdateDeconditionnementRequired.value = null

        if (deconditionRepository == null) {
            _errorMessage.value = "Service de déconditionnement non disponible"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch the product to get parentId and itemQty
                val productResult = productRepository.getProductById(saleLine.produitId)
                val product = productResult.getOrNull()
                if (product == null) {
                    _errorMessage.value = "Impossible de récupérer les informations du produit"
                    return@launch
                }

                val parentId = product.parentId
                if (parentId == null) {
                    _errorMessage.value = "Ce produit n'a pas de conditionnement parent (CH)"
                    return@launch
                }

                // Fetch CH parent to check stock
                val parentResult = productRepository.getProductById(parentId)
                val parentProduct = parentResult.getOrNull()
                if (parentProduct == null) {
                    _errorMessage.value = "Impossible de récupérer le produit parent (CH)"
                    return@launch
                }

                if (parentProduct.totalQuantity <= 0) {
                    _errorMessage.value = "Stock du conditionnement parent (CH) insuffisant"
                    return@launch
                }

                // Calculate qty to decondition
                val itemQty = product.itemQty ?: 1
                val qtyToDecondition = if (itemQty > 0) {
                    ceil(newQuantity.toDouble() / itemQty.toDouble()).toInt()
                } else {
                    1
                }

                // Call decondition API
                val decondition = Decondition(qtyMvt = qtyToDecondition, produitId = parentId)
                val deconditionResult = deconditionRepository!!.create(decondition)

                deconditionResult.fold(
                    onSuccess = {
                        // Retry the quantity update (stock should now be available)
                        _isLoading.value = false
                        updateProductQuantityWithApi(saleLine, newQuantity)
                    },
                    onFailure = { error ->
                        postServerError(error, "Erreur de déconditionnement")
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
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
        val sale = _currentSale.value ?: run {
            _errorMessage.value = "Aucune vente en cours"
            return
        }

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
                        postServerError(error)
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
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
        val sale = _currentSale.value ?: run {
            _errorMessage.value = "Aucune vente en cours"
            return
        }

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
                        postServerError(error)
                    }
                )
            } catch (e: Exception) {
                postServerError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
