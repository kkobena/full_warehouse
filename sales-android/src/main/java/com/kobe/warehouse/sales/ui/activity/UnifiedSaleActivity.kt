package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.databinding.ActivityUnifiedSaleBinding
import com.kobe.warehouse.sales.domain.model.SaleType
import com.kobe.warehouse.sales.ui.adapter.CartAdapter
import com.kobe.warehouse.sales.ui.adapter.ProductAdapter
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModel
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModelFactory
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager

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
                viewModel.updateLineQuantity(line, line.quantitySold + 1)
            },
            onDecrementClick = { line ->
                val newQty = (line.quantitySold - 1).coerceAtLeast(0)
                viewModel.updateLineQuantity(line, newQty)
            },
            onRemoveClick = { line ->
                viewModel.removeLineFromCart(line)
            },
            onQuantityChange = { line, newQuantity ->
                viewModel.updateLineQuantity(line, newQuantity)
            }
        )

        // Setup cart RecyclerView
        binding.includeProductCart.rvCart.apply {
            layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
            adapter = cartAdapter
        }
    }

    private fun setupListeners() {
        // Sale type ChipGroup selection
        binding.includeSaleTypeSelector.chipGroupSaleType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val newType = when (checkedIds[0]) {
                R.id.chipComptant -> SaleType.Comptant
                R.id.chipAssurance -> {
                    val customer = viewModel.selectedCustomer.value
                    if (customer != null) {
                        SaleType.Assurance(customer, emptyList())
                    } else {
                        Toast.makeText(this, "Sélectionnez un client d'abord", Toast.LENGTH_SHORT).show()
                        binding.includeSaleTypeSelector.chipComptant.isChecked = true
                        return@setOnCheckedStateChangeListener
                    }
                }
                R.id.chipCarnet -> {
                    val customer = viewModel.selectedCustomer.value
                    if (customer != null) {
                        SaleType.Carnet(customer)
                    } else {
                        Toast.makeText(this, "Sélectionnez un client d'abord", Toast.LENGTH_SHORT).show()
                        binding.includeSaleTypeSelector.chipComptant.isChecked = true
                        return@setOnCheckedStateChangeListener
                    }
                }
                else -> SaleType.Comptant
            }
            viewModel.changeSaleType(newType)
        }

        // Customer selection - Click on search field opens dialog
        binding.includeCustomerZone.etCustomerSearch.setOnClickListener {
            val dialog = com.kobe.warehouse.sales.ui.dialog.CustomerSelectionDialogFragment.newInstance()
            dialog.show(supportFragmentManager, "CustomerSelectionDialog")
        }

        // Product search - Search on text change
        binding.includeProductCart.etProductSearch.addTextChangedListener { text ->
            val query = text.toString()
            if (query.length >= 2) {
                viewModel.searchProducts(query)
            } else {
                // Clear search results
                viewModel.searchProducts("")
            }
        }

        // Barcode scanner icon
        binding.includeProductCart.tilProductSearch.setEndIconOnClickListener {
            // TODO: Implement ZXing barcode scanner
            // For now, show manual barcode input dialog
            showBarcodeInputDialog()
        }

        // Finalize sale action
        binding.fabFinalizeSale.setOnClickListener {
            finalizeSale()
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
                binding.includeCustomerZone.tvCustomerName.text = "${customer.firstName} ${customer.lastName}"
                binding.includeCustomerZone.tvCustomerPhone.text = customer.phone ?: ""
                binding.includeCustomerZone.layoutSelectedCustomer.visibility = View.VISIBLE
            } else {
                binding.includeCustomerZone.layoutSelectedCustomer.visibility = View.GONE
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
            binding.includePaymentZone.tvTotalAmount.text = formatAmount(sale.salesAmount) + " FCFA"
            binding.includeProductCart.chipCartCount.text = "${sale.salesLines.size} article(s)"

            // Show/hide empty cart view
            binding.includeProductCart.emptyCartView.visibility =
                if (sale.salesLines.isEmpty()) View.VISIBLE else View.GONE
            binding.includeProductCart.rvCart.visibility =
                if (sale.salesLines.isEmpty()) View.GONE else View.VISIBLE

            // Enable/disable finalize button
            binding.fabFinalizeSale.isEnabled = sale.salesLines.isNotEmpty()
        }

        // Errors
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
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
                // TODO: Print receipt (integrate with SunmiPrinterService)
                Toast.makeText(this, "Vente finalisée avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // TODO: Show/hide progress dialog or loading indicator
            binding.fabFinalizeSale.isEnabled = !isLoading
        }
    }

    private fun updateUIForSaleType(saleType: SaleType) {
        supportActionBar?.title = when (saleType) {
            is SaleType.Comptant -> "Vente Comptant"
            is SaleType.Assurance -> "Vente Assurance"
            is SaleType.Carnet -> "Vente Carnet"
        }

        // Show/hide type-specific UI elements
        when (saleType) {
            is SaleType.Assurance -> {
                binding.includeInsuranceData.root.visibility = View.VISIBLE
                binding.includeCarnetInfo.root.visibility = View.GONE
                // Display tiers payants info
                displayInsuranceData(saleType)
            }
            is SaleType.Carnet -> {
                binding.includeInsuranceData.root.visibility = View.GONE
                binding.includeCarnetInfo.root.visibility = View.VISIBLE
                // Display carnet info
                displayCarnetInfo(saleType)
            }
            else -> {
                binding.includeInsuranceData.root.visibility = View.GONE
                binding.includeCarnetInfo.root.visibility = View.GONE
            }
        }
    }

    private fun displayInsuranceData(saleType: SaleType.Assurance) {
        // Display tiers payants list
        if (saleType.tiersPayants.isEmpty()) {
            binding.includeInsuranceData.tvTiersPayants.visibility = View.GONE
        } else {
            val tiersPayantsText = saleType.tiersPayants.joinToString(", ") { it.getDisplayName() }
            binding.includeInsuranceData.tvTiersPayants.text = tiersPayantsText
            binding.includeInsuranceData.tvTiersPayants.visibility = View.VISIBLE
        }
    }

    private fun displayCarnetInfo(saleType: SaleType.Carnet) {
        // Display carnet customer info
        val customer = saleType.saleCustomer
        binding.includeCarnetInfo.tvCarnetCustomer.text = "${customer.firstName} ${customer.lastName}"

        // Display credit info (will be updated by ViewModel)
        // The actual balance will come from viewModel observers
    }

    private fun showAddToCartDialog(product: Product) {
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
                    viewModel.addProductToCart(product, quantity)
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

    private fun formatAmount(amount: Int): String {
        return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
    }
}
