package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.AuthApiService
import com.kobe.warehouse.sales.data.api.CustomerApiService
import com.kobe.warehouse.sales.data.api.PaymentApiService
import com.kobe.warehouse.sales.data.api.ProductApiService
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.model.SaleType
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.databinding.ActivityUnifiedSaleBinding
import com.kobe.warehouse.sales.ui.adapter.CartAdapter
import com.kobe.warehouse.sales.ui.adapter.ProductAdapter
import com.kobe.warehouse.sales.ui.dialog.AuthorizationDialogFragment
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModel
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModelFactory
import com.kobe.warehouse.sales.printer.ReceiptPrinter
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import com.kobe.warehouse.sales.utils.onTextChangedDebounced
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unified Sale Activity
 *
 * Handles all types of sales (Comptant, Assurance, Carnet) in a single unified interface
 * Uses UnifiedSaleViewModel to manage state and business logic
 */
class UnifiedSaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnifiedSaleBinding
    private lateinit var viewModel: UnifiedSaleViewModel

    private lateinit var productAdapter: ProductAdapter
    private lateinit var cartAdapter: CartAdapter
    private lateinit var customerSearchAdapter: com.kobe.warehouse.sales.ui.adapter.CustomerSearchAdapter
    private lateinit var tiersPayantsAdapter: com.kobe.warehouse.sales.ui.adapter.CustomerTiersPayantAdapter
    private var currentAdapterMode: Boolean? = null  // Track if adapter is in Assurance mode

    companion object {
        const val EXTRA_SALE_TYPE = "extra_sale_type"
        const val EXTRA_SALE_ID = "extra_sale_id"
        const val EXTRA_SALE_DATE = "extra_sale_date"

        // Sale type constants
        const val SALE_TYPE_COMPTANT = "COMPTANT"
        const val SALE_TYPE_ASSURANCE = "ASSURANCE"
        const val SALE_TYPE_CARNET = "CARNET"

        // Request codes
        private const val REQUEST_SELECT_CUSTOMER = 1001
    }

    // Customer selection launcher
    private val customerSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getParcelableExtra<Customer>("selected_customer")?.let { customer ->
                viewModel.selectCustomer(customer)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnifiedSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewModel()
        setupAdapters()
        setupListeners()
        observeViewModel()

        // Load sale if editing
        handleIntent(intent)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Vente"
        }
    }

    private fun setupViewModel() {
        val tokenManager = TokenManager(this)
        val apiClient = ApiClient.create(tokenManager = tokenManager)

        val salesApi = apiClient.create(SalesApiService::class.java)
        val productApi = apiClient.create(ProductApiService::class.java)
        val paymentApi = apiClient.create(PaymentApiService::class.java)
        val customerApi = apiClient.create(CustomerApiService::class.java)
        val authApi = apiClient.create(AuthApiService::class.java)

        val salesRepository = SalesRepository(salesApi)
        val productRepository = ProductRepository(productApi)
        val paymentRepository = PaymentRepository(paymentApi)
        val customerRepository = CustomerRepository(customerApi)
        val authRepository = AuthRepository(authApi, tokenManager)

        val factory = UnifiedSaleViewModelFactory(
            salesRepository,
            productRepository,
            paymentRepository,
            customerRepository,
            authRepository,
            tokenManager
        )

        viewModel = ViewModelProvider(this, factory)[UnifiedSaleViewModel::class.java]
    }

    private fun setupAdapters() {
        // Customer search adapter
        customerSearchAdapter = com.kobe.warehouse.sales.ui.adapter.CustomerSearchAdapter { customer ->
            handleCustomerSelection(customer)
        }

        // Setup customer search RecyclerView
        binding.includeCustomerZone.rvCustomerSearchResults.apply {
            layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
            adapter = customerSearchAdapter
        }

        // Product search adapter
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                showAddToCartDialog(product)
            }
        )

        // Setup product search RecyclerView
        binding.includeProductCart.rvProductSearchResults.apply {
            layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
            adapter = productAdapter
        }

        // Cart adapter
        cartAdapter = CartAdapter(
            onIncrementClick = { line ->
                handleQuantityChange(line, line.quantitySold + 1)
            },
            onDecrementClick = { line ->
                val newQty = (line.quantitySold - 1).coerceAtLeast(1)
                handleQuantityChange(line, newQty)
            },
            onRemoveClick = { line ->
                handleRemoveLine(line)
            },
            onQuantityChange = { line, newQuantity ->
                handleQuantityChange(line, newQuantity)
            },
            onPriceClick = { line ->
                handlePriceModification(line)
            }
        )

        // Setup cart RecyclerView
        binding.includeProductCart.rvCart.apply {
            layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
            adapter = cartAdapter
        }

        // Tiers Payants adapter (will be configured per sale type)
        tiersPayantsAdapter = com.kobe.warehouse.sales.ui.adapter.CustomerTiersPayantAdapter(
            isAssuranceMode = false,
            onNumeroBonChanged = { tiersPayant, numBon ->
                viewModel.updateTiersPayantNumBon(tiersPayant, numBon)
            },
            onTauxModifyClicked = { tiersPayant ->
                showModifyTauxDialog(tiersPayant)
            },
            onRemoveClicked = { tiersPayant ->
                confirmRemoveTiersPayant(tiersPayant)
            }
        )

        binding.includeCustomerInfoDisplay.rvTiersPayants.apply {
            layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
            adapter = tiersPayantsAdapter
        }
    }

    private fun setupListeners() {
        // Sale type ChipGroup selection
        binding.includeSaleTypeSelector.chipGroupSaleType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val newType = when (checkedIds[0]) {
                R.id.chipComptant -> SaleType.Comptant
                R.id.chipAssurance -> {
                    // Allow selection, customer will be validated before adding products
                    val customer = viewModel.selectedCustomer.value
                    if (customer != null) {
                        SaleType.Assurance(customer, emptyList())
                    } else {
                        // Show customer selection zone but don't block type change
                        SaleType.Assurance(null, emptyList())
                    }
                }
                R.id.chipCarnet -> {
                    // Allow selection, customer will be validated before adding products
                    val customer = viewModel.selectedCustomer.value
                    if (customer != null) {
                        SaleType.Carnet(customer)
                    } else {
                        // Show customer selection zone but don't block type change
                        SaleType.Carnet(null)
                    }
                }
                else -> SaleType.Comptant
            }
            viewModel.changeSaleType(newType)
        }

        // Customer search - Inline search with debounce (300ms)
        binding.includeCustomerZone.etCustomerSearch.onTextChangedDebounced(
            lifecycleOwner = this,
            debounceMs = 300,
            minLength = 2
        ) { query ->
            if (query.length >= 2) {
                viewModel.searchCustomers(query)
            } else {
                viewModel.clearCustomerSearchResults()
            }
        }

        // Handle IME action (Search button on keyboard)
        binding.includeCustomerZone.etCustomerSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.includeCustomerZone.etCustomerSearch.text.toString().trim()
                if (query.length >= 2) {
                    viewModel.searchCustomers(query)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }

        // Expand customer zone when clicking on header
        binding.includeCustomerZone.layoutCustomerHeader.setOnClickListener {
            toggleCustomerZoneExpansion()
        }

        // Product search - Block if customer required but not selected
        binding.includeProductCart.etProductSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val saleType = viewModel.currentSaleType.value
                if (saleType?.requiresCustomer() == true && viewModel.selectedCustomer.value == null) {
                    // Customer required but not selected - block product search
                    binding.includeProductCart.etProductSearch.clearFocus()
                    showClientRequiredDialog()
                }
            }
        }

        // Product search - Debounced search (300ms)
        binding.includeProductCart.etProductSearch.onTextChangedDebounced(
            lifecycleOwner = this,
            debounceMs = 300,
            minLength = 2
        ) { query ->
            // Double-check customer requirement before searching
            val saleType = viewModel.currentSaleType.value
            if (saleType?.requiresCustomer() == true && viewModel.selectedCustomer.value == null) {
                // Block search, clear field
                binding.includeProductCart.etProductSearch.setText("")
                return@onTextChangedDebounced
            }

            if (query.length >= 2) {
                viewModel.searchProducts(query)
            } else {
                // Clear search results
                viewModel.searchProducts("")
            }
        }

        // Product search - Handle IME action
        binding.includeProductCart.etProductSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Barcode scanner icon
        binding.includeProductCart.tilProductSearch.setEndIconOnClickListener {
            // TODO: Implement ZXing barcode scanner
            // For now, show manual barcode input dialog
            showBarcodeInputDialog()
        }

        // Finalize sale action
        binding.btnFinalizeSale.setOnClickListener {
            finalizeSale()
        }

        // Put on hold action
        binding.btnPutOnHold.setOnClickListener {
            putOnHold()
        }

        // Customer Info expand/collapse
        binding.includeCustomerInfoDisplay.headerCustomerInfo.setOnClickListener {
            toggleCustomerInfoExpansion()
        }

        // Cart expand/collapse
        binding.includeProductCart.headerCart.setOnClickListener {
            toggleCartExpansion()
        }
    }

    /**
     * Toggle Customer Info section expansion
     */
    private fun toggleCustomerInfoExpansion() {
        val content = binding.includeCustomerInfoDisplay.contentCustomerInfo
        val icon = binding.includeCustomerInfoDisplay.ivExpandCustomerInfo

        if (content.visibility == View.VISIBLE) {
            // Collapse
            content.visibility = View.GONE
            icon.setImageResource(R.drawable.ic_expand_more)
        } else {
            // Expand
            content.visibility = View.VISIBLE
            icon.setImageResource(R.drawable.ic_expand_less)
        }
    }

    /**
     * Toggle Cart section expansion
     */
    private fun toggleCartExpansion() {
        val content = binding.includeProductCart.contentCart
        val icon = binding.includeProductCart.ivExpandCart

        if (content.visibility == View.VISIBLE) {
            // Collapse
            content.visibility = View.GONE
            icon.setImageResource(R.drawable.ic_expand_more)
        } else {
            // Expand
            content.visibility = View.VISIBLE
            icon.setImageResource(R.drawable.ic_expand_less)
        }
    }

    private fun observeViewModel() {
        // Sale type
        viewModel.currentSaleType.observe(this) { saleType ->
            updateUIForSaleType(saleType)
        }

        // Customer
        viewModel.selectedCustomer.observe(this) { customer ->
            if (customer != null) {
                // Clear search field when customer is selected
                // Clear focus first to avoid stealing focus from other fields
                binding.includeCustomerZone.etCustomerSearch.clearFocus()
                binding.includeCustomerZone.etCustomerSearch.setText("")

                // Display detailed customer info
                // displayCustomerInfo will call displayTiersPayants which updates the adapter
                displayCustomerInfo(customer)
            } else {
                binding.includeCustomerInfoDisplay.root.visibility = View.GONE
            }
        }

        // Client Tiers Payants - observe separately to avoid recreating adapter
        viewModel.clientTiersPayants.observe(this) { tiersPayants ->
            // Only update adapter list for Assurance mode
            val saleType = viewModel.currentSaleType.value
            if (::tiersPayantsAdapter.isInitialized && saleType is SaleType.Assurance) {
                tiersPayantsAdapter.submitList(tiersPayants)
            }
        }

        // Sale Type - refresh display when mode changes
        viewModel.currentSaleType.observe(this) { saleType ->
            // If a customer is already selected, refresh their info to show/hide sections
            val customer = viewModel.selectedCustomer.value
            if (customer != null) {
                displayCustomerInfo(customer)
            }
        }

        // Ayant Droit (Assurance only)
        viewModel.selectedAyantDroit.observe(this) { ayantDroit ->
            if (ayantDroit != null) {
                binding.includeCustomerInfoDisplay.layoutSelectedAyantDroit.visibility = View.VISIBLE
                binding.includeCustomerInfoDisplay.tvAyantDroitName.text =
                    "${ayantDroit.firstName} ${ayantDroit.lastName}"
            } else {
                binding.includeCustomerInfoDisplay.layoutSelectedAyantDroit.visibility = View.GONE
            }
        }

        // Customer search results
        viewModel.customerSearchResults.observe(this) { customers ->
            handleCustomerSearchResults(customers)
        }

        // Customer search loading
        viewModel.isSearchingCustomer.observe(this) { isSearching ->
            binding.includeCustomerZone.layoutCustomerSearchLoading.visibility =
                if (isSearching) View.VISIBLE else View.GONE

            // Hide other states while loading
            if (isSearching) {
                binding.includeCustomerZone.rvCustomerSearchResults.visibility = View.GONE
                binding.includeCustomerZone.tvCustomerSearchEmpty.visibility = View.GONE
            }
        }

        // Customer required
        viewModel.customerRequired.observe(this) { required ->
            if (required) {
                binding.includeCustomerZone.tvCustomerRequired.visibility = View.VISIBLE
            } else {
                binding.includeCustomerZone.tvCustomerRequired.visibility = View.GONE
            }
        }

        // Products
        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)
            binding.includeProductCart.rvProductSearchResults.visibility =
                if (products.isEmpty()) View.GONE else View.VISIBLE
        }

        // Cart
        viewModel.currentSale.observe(this) { sale ->
            cartAdapter.submitList(sale.salesLines)

            // Update totals and cart UI
            binding.tvTotal.text = formatAmount(sale.salesAmount) + " FCFA"
            binding.includeProductCart.tvCartItemCount.text = "${sale.salesLines.size}"

            // Show/hide empty cart view
            binding.includeProductCart.emptyCartState.visibility =
                if (sale.salesLines.isEmpty()) View.VISIBLE else View.GONE
            binding.includeProductCart.rvCart.visibility =
                if (sale.salesLines.isEmpty()) View.GONE else View.VISIBLE
            binding.includeProductCart.tvCartItemCount.visibility =
                if (sale.salesLines.isEmpty()) View.GONE else View.VISIBLE

            // Enable/disable finalize button
            binding.btnFinalizeSale.isEnabled = sale.salesLines.isNotEmpty()
        }

        // Errors and success messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()

                // If product was added successfully, clear search field and refocus
                if (it == "Produit ajouté") {
                    binding.includeProductCart.etProductSearch.setText("")
                    binding.includeProductCart.etProductSearch.requestFocus()
                }
            }
        }

        viewModel.customerValidationError.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearCustomerValidationError()
            }
        }

        viewModel.stockValidationError.observe(this) { error ->
            error?.let {
                showForceStockDialog(it)
            }
        }

        // Sale saved
        viewModel.saleSaved.observe(this) { sale ->
            sale?.let {
                Toast.makeText(this, "Vente mise en attente", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Sale finalized
        viewModel.saleFinalized.observe(this) { sale ->
            sale?.let {
                showSaleCompletedDialog(it.numberTransaction ?: "")
            }
        }

        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnFinalizeSale.isEnabled = !isLoading
            binding.btnPutOnHold.isEnabled = !isLoading
        }
    }

    private fun updateUIForSaleType(saleType: SaleType) {
        supportActionBar?.title = when (saleType) {
            is SaleType.Comptant -> "Vente Comptant"
            is SaleType.Assurance -> "Vente Assurance"
            is SaleType.Carnet -> "Vente Carnet"
        }

        // Auto-expand customer zone for Carnet/Assurance (customer required)
        // Implements UX spec: customer zone must be visible when customer is mandatory
        when (saleType) {
            is SaleType.Assurance, is SaleType.Carnet -> {
                // Expand customer zone automatically with animation
                if (binding.includeCustomerZone.layoutCustomerContent.visibility != View.VISIBLE) {
                    expandView(binding.includeCustomerZone.layoutCustomerContent)
                    binding.includeCustomerZone.ivExpandCustomer.animate()
                        .rotation(180f)
                        .setDuration(300)
                        .start()
                }

                // Show "Obligatoire" badge
                binding.includeCustomerZone.tvCustomerRequired.visibility = View.VISIBLE

                // Focus on customer search if no customer selected
                if (viewModel.selectedCustomer.value == null) {
                    binding.includeCustomerZone.etCustomerSearch.postDelayed({
                        binding.includeCustomerZone.etCustomerSearch.requestFocus()
                    }, 350) // After animation
                }
            }
            is SaleType.Comptant -> {
                // For Comptant, keep collapsed unless customer is required for specific conditions
                // (deferred payment or avoir - validated at finalization)
                binding.includeCustomerZone.tvCustomerRequired.visibility = View.GONE
            }
        }

        // Note: Customer info is now handled by displayCustomerInfo() method
        // which is called from the selectedCustomer observer
    }

    private fun showAddToCartDialog(product: Product) {
        // Check if customer is required but not selected
        val saleType = viewModel.currentSaleType.value ?: SaleType.Comptant
        if (saleType.requiresCustomer() && !saleType.hasCustomer()) {
            // Customer required but not selected
            MaterialAlertDialogBuilder(this)
                .setTitle("Client requis")
                .setMessage("Veuillez sélectionner un client avant d'ajouter des produits pour une vente ${saleType.getDisplayName()}.")
                .setPositiveButton("Sélectionner client") { _, _ ->
                    // Open customer selection dialog
                    binding.includeCustomerZone.etCustomerSearch.performClick()
                }
                .setNegativeButton("Annuler", null)
                .show()
            return
        }

        // Create custom view for quantity input
        val inputLayout = layoutInflater.inflate(R.layout.dialog_quantity_input, null)
        val etQuantity = inputLayout.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etQuantity)
        val tvStock = inputLayout.findViewById<TextView>(R.id.tvStock)

        // Set initial values
        etQuantity.setText("1")
        etQuantity.selectAll()
        tvStock.text = "Stock disponible: ${product.totalQuantity}"

        MaterialAlertDialogBuilder(this)
            .setTitle("Ajouter au panier")
            .setMessage("${product.libelle}\nPrix: ${formatAmount(product.regularUnitPrice)} FCFA")
            .setView(inputLayout)
            .setPositiveButton("Ajouter") { dialog, which ->
                val quantityText = etQuantity.text.toString()
                val quantity = quantityText.toIntOrNull() ?: 1
                if (quantity > 0) {
                    // Validate stock before adding
                    validateAndAddProduct(product, quantity)
                } else {
                    Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()

        // Focus on quantity input
        etQuantity.requestFocus()
    }

    private fun showBarcodeInputDialog() {
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.hint = "Code-barres"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 16, 48, 16)
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Rechercher par code-barres")
            .setMessage("Entrez le code-barres du produit")
            .setView(container)
            .setPositiveButton("Rechercher") { dialog, which ->
                val barcode = input.text.toString().trim()
                if (barcode.isNotEmpty()) {
                    // Search product by barcode
                    viewModel.searchProducts(barcode)
                } else {
                    Toast.makeText(this, "Code-barres vide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()

        // Focus on input
        input.requestFocus()
    }

    private fun showForceStockDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Stock insuffisant")
            .setMessage(message + "\n\nVoulez-vous forcer le stock ?")
            .setPositiveButton("Forcer") { _, _ ->
                // TODO: Add product with force stock
                viewModel.clearStockValidationError()
            }
            .setNegativeButton("Annuler") { _, _ ->
                viewModel.clearStockValidationError()
            }
            .show()
    }

    private fun confirmPutOnHold() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Mettre en attente")
            .setMessage("Voulez-vous mettre cette vente en attente (prévente) ?")
            .setPositiveButton("Oui") { _, _ ->
                viewModel.putOnHold()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun handleIntent(intent: Intent) {
        // Check if editing existing sale
        val saleId = intent.getLongExtra(EXTRA_SALE_ID, 0L)
        val saleDate = intent.getStringExtra(EXTRA_SALE_DATE)

        if (saleId > 0 && saleDate != null) {
            viewModel.loadSale(saleId, saleDate)
        }

        // Set initial sale type via ChipGroup
        val saleType = intent.getStringExtra(EXTRA_SALE_TYPE)
        val initialChipId = when (saleType) {
            SALE_TYPE_ASSURANCE -> R.id.chipAssurance
            SALE_TYPE_CARNET -> R.id.chipCarnet
            else -> R.id.chipComptant
        }
        binding.includeSaleTypeSelector.chipGroupSaleType.check(initialChipId)
    }

    private fun finalizeSale() {
        val sale = viewModel.currentSale.value ?: return

        if (sale.salesLines.isEmpty()) {
            Toast.makeText(this, "Le panier est vide", Toast.LENGTH_SHORT).show()
            return
        }

        // Open payment dialog
        val payrollAmount = sale.salesAmount - (sale.discountAmount ?: 0)
        val dialog = com.kobe.warehouse.sales.ui.dialog.PaymentDialogFragment.newInstance(payrollAmount)
        dialog.show(supportFragmentManager, "PaymentDialog")
    }

    /**
     * Show sale completed dialog with receipt printing option
     */
    private fun showSaleCompletedDialog(transactionNumber: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Vente réussie")
            .setMessage("Vente $transactionNumber enregistrée avec succès.\n\nVoulez-vous imprimer le reçu ?")
            .setPositiveButton("Imprimer") { _, _ ->
                printReceipt()
            }
            .setNegativeButton("Non") { _, _ ->
                viewModel.resetAfterSale()
                Toast.makeText(this, "Prêt pour une nouvelle vente", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Print receipt for the completed sale
     */
    private fun printReceipt() {
        val sale = viewModel.saleFinalized.value
        if (sale == null) {
            Toast.makeText(this, "Erreur: Vente introuvable", Toast.LENGTH_SHORT).show()
            viewModel.resetAfterSale()
            return
        }

        Toast.makeText(this, "Impression du reçu en cours...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val receiptPrinter = ReceiptPrinter(this@UnifiedSaleActivity)
                    receiptPrinter.printReceipt(sale = sale)
                } catch (e: Exception) {
                    android.util.Log.e("UnifiedSaleActivity", "Failed to print receipt", e)
                    false
                }
            }

            if (success) {
                viewModel.resetAfterSale()
                Toast.makeText(
                    this@UnifiedSaleActivity,
                    "Reçu imprimé avec succès. Prêt pour une nouvelle vente",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                MaterialAlertDialogBuilder(this@UnifiedSaleActivity)
                    .setTitle("Erreur d'impression")
                    .setMessage("Impossible d'imprimer le reçu. Vérifiez que l'imprimante est connectée.")
                    .setPositiveButton("OK") { _, _ ->
                        viewModel.resetAfterSale()
                        Toast.makeText(
                            this@UnifiedSaleActivity,
                            "Prêt pour une nouvelle vente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
            }
        }
    }

    private fun putOnHold() {
        val sale = viewModel.currentSale.value ?: return

        if (sale.salesLines.isEmpty()) {
            Toast.makeText(this, "Le panier est vide", Toast.LENGTH_SHORT).show()
            return
        }

        // Call ViewModel to put sale on hold
        viewModel.putOnHold()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_unified_sale, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_transform -> {
                showTransformDialog()
                true
            }
            R.id.action_put_on_hold -> {
                confirmPutOnHold()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showTransformDialog() {
        val currentType = viewModel.currentSaleType.value ?: return

        val options = when (currentType) {
            is SaleType.Comptant -> arrayOf("Transformer en Assurance", "Transformer en Carnet")
            is SaleType.Assurance -> arrayOf("Transformer en Comptant", "Transformer en Carnet")
            is SaleType.Carnet -> arrayOf("Transformer en Comptant", "Transformer en Assurance")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Transformer la vente")
            .setItems(options) { dialog, which ->
                val newType = when (currentType) {
                    is SaleType.Comptant -> if (which == 0) SALE_TYPE_ASSURANCE else SALE_TYPE_CARNET
                    is SaleType.Assurance -> if (which == 0) SALE_TYPE_COMPTANT else SALE_TYPE_CARNET
                    is SaleType.Carnet -> if (which == 0) SALE_TYPE_COMPTANT else SALE_TYPE_ASSURANCE
                }
                performTransformation(newType)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun performTransformation(newType: String) {
        // TODO: Implement actual transformation with backend call
        Toast.makeText(
            this,
            "Transformation vers $newType - Backend à implémenter",
            Toast.LENGTH_SHORT
        ).show()

        // For now, just change the chip selection
        val chipId = when (newType) {
            SALE_TYPE_ASSURANCE -> R.id.chipAssurance
            SALE_TYPE_CARNET -> R.id.chipCarnet
            else -> R.id.chipComptant
        }
        binding.includeSaleTypeSelector.chipGroupSaleType.check(chipId)
    }

    /**
     * Handle customer search results - Implements UX workflow from expert_ux.agent.md
     * 3 cases:
     * - 0 clients: Show creation confirmation dialog
     * - 1 client: Auto-select customer
     * - Multiple clients: Show list for manual selection
     */
    private fun handleCustomerSearchResults(customers: List<Customer>) {
        when (customers.size) {
            0 -> {
                // No results - show empty state
                binding.includeCustomerZone.rvCustomerSearchResults.visibility = View.GONE
                customerSearchAdapter.submitList(emptyList())

                val query = binding.includeCustomerZone.etCustomerSearch.text.toString().trim()
                if (query.length >= 2) {
                    // Show empty state message
                    binding.includeCustomerZone.tvCustomerSearchEmpty.visibility = View.VISIBLE
                    binding.includeCustomerZone.tvCustomerSearchEmpty.text =
                        "Aucun client trouvé pour \"$query\""

                    // Show create confirmation dialog with delay for better UX
                    binding.includeCustomerZone.tvCustomerSearchEmpty.postDelayed({
                        showCustomerCreateConfirmDialog()
                    }, 500)
                } else {
                    binding.includeCustomerZone.tvCustomerSearchEmpty.visibility = View.GONE
                }
            }
            1 -> {
                // Single result - Auto-select with smooth transition
                binding.includeCustomerZone.rvCustomerSearchResults.visibility = View.GONE
                binding.includeCustomerZone.tvCustomerSearchEmpty.visibility = View.GONE
                customerSearchAdapter.submitList(emptyList())

                // Small delay for better UX (user sees the result briefly)
                binding.root.postDelayed({
                    handleCustomerSelection(customers[0])
                }, 200)
            }
            else -> {
                // Multiple results - Show list for selection
                binding.includeCustomerZone.tvCustomerSearchEmpty.visibility = View.GONE
                binding.includeCustomerZone.rvCustomerSearchResults.visibility = View.VISIBLE
                customerSearchAdapter.submitList(customers)

                // Animate list appearance
                binding.includeCustomerZone.rvCustomerSearchResults.alpha = 0f
                binding.includeCustomerZone.rvCustomerSearchResults.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    /**
     * Handle customer selection
     * Checks if customer replacement is needed (if a customer is already selected)
     */
    private fun handleCustomerSelection(customer: Customer) {
        // Hide keyboard after selection
        hideKeyboard()

        val currentCustomer = viewModel.selectedCustomer.value

        if (currentCustomer != null && currentCustomer.id != customer.id) {
            // Customer replacement - show confirmation dialog
            showCustomerReplacementConfirmDialog(customer)
        } else {
            // Normal selection
            viewModel.selectCustomer(customer)

            // Clear search results and focus
            viewModel.clearCustomerSearchResults()
            clearAllFocus()
        }
    }

    /**
     * Show customer replacement confirmation dialog
     * Implements UX workflow from expert_ux.agent.md (lines 256-268)
     */
    private fun showCustomerReplacementConfirmDialog(newCustomer: Customer) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remplacer le client ?")
            .setMessage("Remplacer le client actuel ? Toutes les données liées au client précédent sur cette vente seront remplacées.")
            .setPositiveButton("Remplacer") { _, _ ->
                viewModel.replaceCustomer(newCustomer)
            }
            .setNegativeButton("Annuler") { _, _ ->
                // Keep current customer - clear search
                binding.includeCustomerZone.etCustomerSearch.setText("")
                viewModel.clearCustomerSearchResults()
            }
            .show()
    }

    /**
     * Show customer creation confirmation dialog
     * Implements UX workflow: 0 clients found → ask to create
     */
    private fun showCustomerCreateConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Créer un nouveau client ?")
            .setMessage("Aucun client trouvé. Voulez-vous créer un nouveau client ?")
            .setPositiveButton("Créer") { _, _ ->
                openCustomerCreateDialog()
            }
            .setNegativeButton("Annuler") { _, _ ->
                binding.includeCustomerZone.etCustomerSearch.setText("")
            }
            .show()
    }

    /**
     * Open appropriate customer creation dialog based on sale type
     * Implements UX spec: different forms for Comptant/Carnet/Assurance
     */
    private fun openCustomerCreateDialog() {
        val saleType = viewModel.currentSaleType.value ?: SaleType.Comptant

        when (saleType) {
            is SaleType.Comptant -> {
                // Open Uninsured (Comptant) customer creation
                val dialog = com.kobe.warehouse.sales.ui.dialog.UninsuredCustomerCreateDialogFragment.newInstance { customer ->
                    viewModel.selectCustomer(customer)
                }
                dialog.show(supportFragmentManager, com.kobe.warehouse.sales.ui.dialog.UninsuredCustomerCreateDialogFragment.TAG)
            }
            is SaleType.Carnet -> {
                // Open Carnet customer creation
                val dialog = com.kobe.warehouse.sales.ui.dialog.CarnetCustomerCreateDialogFragment.newInstance { customer ->
                    viewModel.selectCustomer(customer)
                }
                dialog.show(supportFragmentManager, com.kobe.warehouse.sales.ui.dialog.CarnetCustomerCreateDialogFragment.TAG)
            }
            is SaleType.Assurance -> {
                // Open Assurance customer creation (with steps)
                val dialog = com.kobe.warehouse.sales.ui.dialog.AssureCustomerCreateDialogFragment.newInstance { customer ->
                    viewModel.selectCustomer(customer)
                }
                dialog.show(supportFragmentManager, com.kobe.warehouse.sales.ui.dialog.AssureCustomerCreateDialogFragment.TAG)
            }
        }
    }

    /**
     * Show dialog when user tries to add products without selecting customer
     * (for Carnet/Assurance sale types)
     */
    private fun showClientRequiredDialog() {
        val saleType = viewModel.currentSaleType.value ?: SaleType.Comptant
        val saleTypeName = when (saleType) {
            is SaleType.Assurance -> "Assurance"
            is SaleType.Carnet -> "Carnet"
            else -> ""
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Client requis")
            .setMessage("Veuillez sélectionner un client avant d'ajouter des produits pour une vente $saleTypeName.")
            .setPositiveButton("Sélectionner client") { _, _ ->
                // Expand customer zone if needed
                if (binding.includeCustomerZone.layoutCustomerContent.visibility != View.VISIBLE) {
                    expandView(binding.includeCustomerZone.layoutCustomerContent)
                    binding.includeCustomerZone.ivExpandCustomer.animate()
                        .rotation(180f)
                        .setDuration(300)
                        .start()
                }
                // Focus on search with keyboard
                showKeyboard(binding.includeCustomerZone.etCustomerSearch)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Toggle customer zone expansion with smooth animation
     */
    private fun toggleCustomerZoneExpansion() {
        val contentLayout = binding.includeCustomerZone.layoutCustomerContent
        val expandIcon = binding.includeCustomerZone.ivExpandCustomer

        if (contentLayout.visibility == View.VISIBLE) {
            // Collapse with animation
            collapseView(contentLayout)
            expandIcon.animate()
                .rotation(0f)
                .setDuration(300)
                .start()
        } else {
            // Expand with animation
            expandView(contentLayout)
            expandIcon.animate()
                .rotation(180f)
                .setDuration(300)
                .start()
        }
    }

    /**
     * Expand view with slide down animation
     */
    private fun expandView(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight

        view.layoutParams.height = 0
        view.visibility = View.VISIBLE

        view.animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
            .start()

        // Animate height
        val animator = android.animation.ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        animator.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        })
        animator.duration = 300
        animator.start()
    }

    /**
     * Collapse view with slide up animation
     */
    private fun collapseView(view: View) {
        val initialHeight = view.measuredHeight

        val animator = android.animation.ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        animator.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.visibility = View.GONE
            }
        })
        animator.duration = 300
        animator.start()

        view.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(null)
            .start()
    }

    // ========================================
    // Customer Info Display (new workflow)
    // ========================================

    private fun displayCustomerInfo(customer: Customer) {
        android.util.Log.d("UnifiedSale", "displayCustomerInfo called for customer: ${customer.lastName}")

        binding.includeCustomerInfoDisplay.apply {
            root.visibility = View.VISIBLE

            // Basic info
            tvCustomerLastName.text = customer.lastName
            tvCustomerFirstName.text = customer.firstName

            // Get sale type to determine what to display
            val saleType = viewModel.currentSaleType.value
            android.util.Log.d("UnifiedSale", "Current sale type: $saleType")

            // Matricule (visible for Carnet and Assurance)
            if (saleType is SaleType.Carnet || saleType is SaleType.Assurance) {
                layoutMatricule.visibility = View.VISIBLE
                // For Carnet: use customer.tiersPayants
                // For Assurance: use clientTiersPayants from ViewModel
                val matricule = if (saleType is SaleType.Assurance) {
                    viewModel.clientTiersPayants.value?.firstOrNull()?.num
                } else {
                    customer.tiersPayants.firstOrNull()?.num
                }
                tvCustomerMatricule.text = matricule ?: "N/A"
            } else {
                layoutMatricule.visibility = View.GONE
            }

            // Tiers Payants section
            if (saleType is SaleType.Carnet || saleType is SaleType.Assurance) {
                android.util.Log.d("UnifiedSale", "Showing tiers payants section for: $saleType")
                layoutTiersPayantsSection.visibility = View.VISIBLE
                displayTiersPayants(customer, saleType)
            } else {
                android.util.Log.d("UnifiedSale", "Hiding tiers payants section for: $saleType")
                layoutTiersPayantsSection.visibility = View.GONE
            }

            // Ayant Droit section (Assurance only)
            if (saleType is SaleType.Assurance) {
                layoutAyantDroitSection.visibility = View.VISIBLE
                setupAyantDroitSection(customer)
            } else {
                layoutAyantDroitSection.visibility = View.GONE
            }
        }
    }

    private fun displayTiersPayants(customer: Customer, saleType: SaleType) {
        val isAssuranceMode = saleType is SaleType.Assurance

        android.util.Log.d("UnifiedSale", "displayTiersPayants called - isAssuranceMode: $isAssuranceMode")

        // Ensure RecyclerView is always visible
        binding.includeCustomerInfoDisplay.rvTiersPayants.visibility = View.VISIBLE

        // Recreate adapter if it's not initialized OR mode has changed (Carnet ↔ Assurance)
        if (!::tiersPayantsAdapter.isInitialized || currentAdapterMode != isAssuranceMode) {
            android.util.Log.d("UnifiedSale", "Creating new adapter - isAssuranceMode: $isAssuranceMode")

            tiersPayantsAdapter = com.kobe.warehouse.sales.ui.adapter.CustomerTiersPayantAdapter(
                isAssuranceMode = isAssuranceMode,
                onNumeroBonChanged = { tiersPayant, numBon ->
                    viewModel.updateTiersPayantNumBon(tiersPayant, numBon)
                },
                onTauxModifyClicked = { tiersPayant ->
                    showModifyTauxDialog(tiersPayant)
                },
                onRemoveClicked = { tiersPayant ->
                    confirmRemoveTiersPayant(tiersPayant)
                }
            )
            binding.includeCustomerInfoDisplay.rvTiersPayants.adapter = tiersPayantsAdapter
            currentAdapterMode = isAssuranceMode  // Track current mode
        }

        // For Carnet: Use customer.tiersPayants directly (pre-configured, read-only)
        // For Assurance: Use clientTiersPayants from ViewModel (can be modified for this sale)
        val tiersPayantsList = if (isAssuranceMode) {
            // Initialize clientTiersPayants from customer if empty
            if (viewModel.clientTiersPayants.value.isNullOrEmpty() && customer.tiersPayants.isNotEmpty()) {
                android.util.Log.d("UnifiedSale", "Initializing clientTiersPayants from customer")
                customer.tiersPayants.forEach { tp ->
                    viewModel.addClientTiersPayant(
                        tiersPayantId = tp.tiersPayantId,
                        tiersPayantName = tp.tiersPayantName ?: "",
                        num = tp.num ?: "",
                        taux = tp.taux,
                        numBon = tp.numBon,
                        priorite = tp.priorite?.value ?: 0
                    )
                }
            }
            viewModel.clientTiersPayants.value ?: emptyList()
        } else {
            customer.tiersPayants
        }

        android.util.Log.d("UnifiedSale", "Submitting list - size: ${tiersPayantsList.size}")
        tiersPayantsAdapter.submitList(tiersPayantsList)

        // Add button (Assurance only)
        if (isAssuranceMode) {
            android.util.Log.d("UnifiedSale", "Showing Add button for Assurance mode")
            binding.includeCustomerInfoDisplay.btnAddTiersPayant.visibility = View.VISIBLE
            binding.includeCustomerInfoDisplay.btnAddTiersPayant.setOnClickListener {
                showAddTiersPayantDialog(customer)
            }
        } else {
            binding.includeCustomerInfoDisplay.btnAddTiersPayant.visibility = View.GONE
        }
    }

    private fun setupAyantDroitSection(customer: Customer) {
        binding.includeCustomerInfoDisplay.apply {
            // Select ayant droit button
            btnSelectAyantDroit.setOnClickListener {
                showSelectAyantDroitDialog(customer)
            }

            // Create ayant droit button
            btnAddAyantDroit.setOnClickListener {
                showCreateAyantDroitDialog(customer)
            }

            // Remove ayant droit button
            btnRemoveAyantDroit.setOnClickListener {
                viewModel.removeAyantDroit()
            }
        }
    }

    private fun showModifyTauxDialog(tiersPayant: com.kobe.warehouse.sales.data.model.ClientTiersPayant) {
        val input = android.widget.EditText(this)
        input.setText(tiersPayant.taux.toString())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        MaterialAlertDialogBuilder(this)
            .setTitle("Modifier le taux de couverture")
            .setMessage(tiersPayant.tiersPayantName)
            .setView(input)
            .setPositiveButton("Modifier") { _, _ ->
                val newTaux = input.text.toString().toIntOrNull()
                if (newTaux != null && newTaux in 0..100) {
                    viewModel.updateTiersPayantTaux(tiersPayant, newTaux)
                    Toast.makeText(this, "Taux modifié : $newTaux%", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Taux invalide (0-100)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmRemoveTiersPayant(tiersPayant: com.kobe.warehouse.sales.data.model.ClientTiersPayant) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Retirer le tiers payant ?")
            .setMessage("Voulez-vous retirer ${tiersPayant.tiersPayantName} de cette vente ?")
            .setPositiveButton("Retirer") { _, _ ->
                viewModel.removeTiersPayant(tiersPayant)
                Toast.makeText(this, "Tiers payant retiré", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Show dialog to add a tiers payant to the customer
     */
    private fun showAddTiersPayantDialog(customer: Customer) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE

        // Fetch available tiers payants
        viewModel.loadAvailableTiersPayants()

        // Observe the result
        viewModel.availableTiersPayants.observe(this) { tiersPayantsList ->
            binding.progressBar.visibility = View.GONE

            if (tiersPayantsList.isEmpty()) {
                Toast.makeText(this, "Aucun tiers payant disponible", Toast.LENGTH_SHORT).show()
                return@observe
            }

            // Filter out already added tiers payants
            val customerTiersPayantIds = customer.tiersPayants.map { it.tiersPayantId }.toSet()
            val availableTiersPayants = tiersPayantsList.filter { it.id !in customerTiersPayantIds }

            if (availableTiersPayants.isEmpty()) {
                Toast.makeText(this, "Tous les tiers payants sont déjà ajoutés", Toast.LENGTH_SHORT).show()
                return@observe
            }

            // Create adapter for dialog
            val tiersPayantNames = availableTiersPayants.map { it.getDisplayName() }.toTypedArray()

            MaterialAlertDialogBuilder(this)
                .setTitle("Ajouter un tiers payant")
                .setItems(tiersPayantNames) { _, which ->
                    val selectedTiersPayant = availableTiersPayants[which]
                    showAddTiersPayantDetailsDialog(customer, selectedTiersPayant)
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    /**
     * Show dialog to configure tiers payant details (taux and priorité)
     */
    private fun showAddTiersPayantDetailsDialog(
        customer: Customer,
        tiersPayant: com.kobe.warehouse.sales.data.model.TiersPayant
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_tiers_payant_details, null)
        val etTaux = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTaux)
        val spinnerPriorite = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerPriorite)

        // Setup priorité spinner
        val priorites = listOf("R0 (Principal)", "C1 (Complémentaire 1)", "C2 (Complémentaire 2)", "C3 (Complémentaire 3)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorites)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriorite.adapter = adapter

        // Set default values
        etTaux.setText("100")
        spinnerPriorite.setSelection(0) // Default to R0

        MaterialAlertDialogBuilder(this)
            .setTitle("Configurer ${tiersPayant.getDisplayName()}")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val taux = etTaux.text.toString().toIntOrNull()
                if (taux == null || taux !in 0..100) {
                    Toast.makeText(this, "Taux invalide (0-100)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val priorite = when (spinnerPriorite.selectedItemPosition) {
                    0 -> com.kobe.warehouse.sales.data.model.PrioriteTiersPayant.R0
                    1 -> com.kobe.warehouse.sales.data.model.PrioriteTiersPayant.R1
                    2 -> com.kobe.warehouse.sales.data.model.PrioriteTiersPayant.R2
                    3 -> com.kobe.warehouse.sales.data.model.PrioriteTiersPayant.R3
                    else -> com.kobe.warehouse.sales.data.model.PrioriteTiersPayant.R0
                }

                // Create ClientTiersPayant
                val clientTiersPayant = com.kobe.warehouse.sales.data.model.ClientTiersPayant(
                    customerId = customer.id,
                    tiersPayantId = tiersPayant.id ?: 0L,
                    tiersPayantName = tiersPayant.getDisplayName(),
                    num = "", // Will be filled in the main form
                    taux = taux,
                    priorite = priorite,
                    typeTiersPayant = if (priorite.isPrincipal()) "PRINCIPAL" else "COMPLEMENTAIRE"
                )

                viewModel.addTiersPayant(clientTiersPayant)
                Toast.makeText(this, "Tiers payant ajouté", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Show dialog to select an ayant droit from customer's beneficiaries
     */
    private fun showSelectAyantDroitDialog(customer: Customer) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE

        // Fetch ayants droits from backend
        viewModel.loadAyantDroits(customer.id.toInt())

        // Observe the result (one-time observation)
        viewModel.ayantDroitsList.observe(this) { ayantDroits ->
            binding.progressBar.visibility = View.GONE

            if (ayantDroits.isEmpty()) {
                Toast.makeText(this, "Aucun ayant droit trouvé pour ce client", Toast.LENGTH_SHORT).show()
                return@observe
            }

            // Create list of names for dialog
            val ayantDroitNames = ayantDroits.map { "${it.firstName} ${it.lastName}" }.toTypedArray()

            MaterialAlertDialogBuilder(this)
                .setTitle("Sélectionner un ayant droit")
                .setItems(ayantDroitNames) { _, which ->
                    val selectedAyantDroit = ayantDroits[which]
                    viewModel.selectAyantDroit(selectedAyantDroit)
                    Toast.makeText(
                        this,
                        "Ayant droit sélectionné : ${selectedAyantDroit.firstName} ${selectedAyantDroit.lastName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    /**
     * Show dialog to create a new ayant droit for the customer
     */
    private fun showCreateAyantDroitDialog(customer: Customer) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_ayant_droit, null)
        val etFirstName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFirstName)
        val etLastName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etLastName)
        val etPhone = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPhone)

        MaterialAlertDialogBuilder(this)
            .setTitle("Créer un ayant droit")
            .setView(dialogView)
            .setPositiveButton("Créer") { _, _ ->
                val firstName = etFirstName.text.toString().trim()
                val lastName = etLastName.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                // Validate
                if (firstName.isEmpty()) {
                    Toast.makeText(this, "Le prénom est requis", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (lastName.isEmpty()) {
                    Toast.makeText(this, "Le nom est requis", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Create ayant droit object
                val ayantDroit = Customer(
                    id = 0, // Will be assigned by backend
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone.ifEmpty { null },
                    // Copy tiers payants from parent customer
                    tiersPayants = customer.tiersPayants
                )

                // For now, just select it locally (backend integration needed)
                viewModel.selectAyantDroit(ayantDroit)
                Toast.makeText(this, "Ayant droit créé : $firstName $lastName", Toast.LENGTH_SHORT).show()

                // TODO: Send to backend when API is ready
                // viewModel.createAyantDroit(customer.id, ayantDroit)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Hide soft keyboard
     */
    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Show soft keyboard for a specific view
     */
    private fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        view.postDelayed({
            imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    /**
     * Clear focus from all input fields
     */
    private fun clearAllFocus() {
        binding.includeCustomerZone.etCustomerSearch.clearFocus()
        binding.includeProductCart.etProductSearch.clearFocus()
    }

    private fun formatAmount(amount: Int): String {
        return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
    }

    // ========================================
    // Stock Validation
    // ========================================

    /**
     * Validate stock and add product to cart
     * Handles stock validation with force stock and deconditioning dialogs
     */
    private fun validateAndAddProduct(product: Product, quantity: Int, forceStock: Boolean = false) {
        // Check if user has force stock permission
        val hasForceStockPermission = viewModel.checkUserPermission(
            AuthorizationDialogFragment.PERMISSION_FORCE_STOCK
        )

        // Validate stock
        val validationResult = viewModel.validateStock(product, quantity, hasForceStockPermission)

        when {
            validationResult.isValid() -> {
                // Stock OK, add directly
                viewModel.addProductToCart(product, quantity, forceStock)
            }

            validationResult.isBlocked() -> {
                // Blocked, show error
                Toast.makeText(this, validationResult.getErrorMessage(), Toast.LENGTH_LONG).show()
            }

            validationResult.requiresConfirmation() -> {
                // Show confirmation dialog
                showStockValidationDialog(validationResult, product, quantity)
            }
        }
    }

    /**
     * Show stock validation dialog for confirmation
     */
    private fun showStockValidationDialog(
        validationResult: com.kobe.warehouse.sales.domain.validation.StockValidationResult,
        product: Product,
        quantity: Int
    ) {
        val dialog = com.kobe.warehouse.sales.ui.dialog.StockValidationDialogFragment(
            validationResult = validationResult,
            onConfirm = { forceStock ->
                // User confirmed, add product with force stock flag
                viewModel.addProductToCart(product, quantity, forceStock)
            },
            onCancel = {
                // User cancelled, do nothing
            }
        )
        dialog.show(supportFragmentManager, com.kobe.warehouse.sales.ui.dialog.StockValidationDialogFragment.TAG)
    }

    // ========================================
    // Cart Actions with Authorization
    // ========================================

    /**
     * Handle quantity change with API call for saved sales
     */
    private fun handleQuantityChange(line: SaleLine, newQuantity: Int) {
        if (newQuantity <= 0) {
            Toast.makeText(this, "La quantité doit être supérieure à 0", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if sale is saved
        val sale = viewModel.currentSale.value
        if (sale?.id != null && sale.saleId?.saleDate != null) {
            // Saved sale - use API
            viewModel.updateProductQuantityWithApi(line, newQuantity)
        } else {
            // New sale - update locally
            viewModel.updateLineQuantity(line, newQuantity)
        }
    }

    /**
     * Handle line removal with authorization check
     */
    private fun handleRemoveLine(line: SaleLine) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Supprimer le produit")
            .setMessage("Voulez-vous supprimer ${line.produitLibelle ?: "ce produit"} du panier ?")
            .setPositiveButton("Supprimer") { _, _ ->
                val sale = viewModel.currentSale.value

                // Check if sale is saved
                if (sale?.id != null && sale.saleId?.saleDate != null) {
                    // Saved sale - check permission
                    if (viewModel.checkUserPermission(com.kobe.warehouse.sales.ui.dialog.AuthorizationDialogFragment.PERMISSION_DELETE_PRODUCT)) {
                        // User has permission
                        viewModel.deleteProductLineWithApi(line, null)
                    } else {
                        // User needs authorization
                        showAuthorizationDialogForDelete(line)
                    }
                } else {
                    // New sale - remove locally
                    viewModel.removeLineFromCart(line)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Handle price modification with authorization check
     */
    private fun handlePriceModification(line: SaleLine) {
        val sale = viewModel.currentSale.value

        // Check permission first
        if (sale?.id != null && sale.saleId?.saleDate != null) {
            // Saved sale - check permission
            if (!viewModel.checkUserPermission(com.kobe.warehouse.sales.ui.dialog.AuthorizationDialogFragment.PERMISSION_MODIFY_PRICE)) {
                // User doesn't have permission - request authorization
                showAuthorizationDialogForPrice(line)
                return
            }
        }

        // User has permission or it's a new sale - show price dialog
        showPriceModificationDialog(line, null)
    }

    /**
     * Show price modification dialog
     */
    private fun showPriceModificationDialog(line: SaleLine, authUserId: Int?) {
        val dialogBinding = com.kobe.warehouse.sales.databinding.DialogProductPriceBinding.inflate(layoutInflater)

        dialogBinding.tvProductName.text = line.produitLibelle ?: "Produit"
        dialogBinding.tvCurrentPrice.text = "Prix actuel : ${line.regularUnitPrice} FCFA"
        dialogBinding.etNewPrice.setText(line.regularUnitPrice.toString())
        dialogBinding.etNewPrice.selectAll()

        MaterialAlertDialogBuilder(this)
            .setTitle("Modifier le prix")
            .setView(dialogBinding.root)
            .setPositiveButton("Valider") { _, _ ->
                val newPriceStr = dialogBinding.etNewPrice.text.toString()
                val newPrice = newPriceStr.toIntOrNull()

                if (newPrice == null || newPrice <= 0) {
                    Toast.makeText(this, "Prix invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sale = viewModel.currentSale.value
                if (sale?.id != null && sale.saleId?.saleDate != null) {
                    // Saved sale - use API
                    viewModel.updateProductPriceWithApi(line, newPrice, authUserId)
                } else {
                    // New sale - update locally
                    val updatedLine = line.copy(
                        regularUnitPrice = newPrice,
                        salesAmount = line.quantitySold * newPrice
                    )
                    viewModel.updateLineQuantity(updatedLine, updatedLine.quantitySold)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Show authorization dialog for price modification
     */
    private fun showAuthorizationDialogForPrice(line: SaleLine) {
        val dialog = AuthorizationDialogFragment(
            requiredPermission = AuthorizationDialogFragment.PERMISSION_MODIFY_PRICE,
            operationName = "Modification de prix",
            onAuthorized = { userId ->
                showPriceModificationDialog(line, userId)
            }
        )
        dialog.show(supportFragmentManager, "authorization_price")
    }

    /**
     * Show authorization dialog for line deletion
     */
    private fun showAuthorizationDialogForDelete(line: SaleLine) {
        val dialog = AuthorizationDialogFragment(
            requiredPermission =AuthorizationDialogFragment.PERMISSION_DELETE_PRODUCT,
            operationName = "Suppression de produit",
            onAuthorized = { userId ->
                viewModel.deleteProductLineWithApi(line, userId)
            }
        )
        dialog.show(supportFragmentManager, "authorization_delete")
    }
}
