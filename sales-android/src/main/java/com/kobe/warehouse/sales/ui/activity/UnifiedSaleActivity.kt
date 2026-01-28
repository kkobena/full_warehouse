package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
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
        // TODO: Setup product recycler view with includeProductCart binding
        // binding.includeProductCart.rvProductSearchResults.apply {
        //     layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
        //     adapter = productAdapter
        // }

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
        // TODO: Setup cart recycler view with includeProductCart binding
        // binding.includeProductCart.rvCart.apply {
        //     layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
        //     adapter = cartAdapter
        // }
    }

    private fun setupListeners() {
        // TODO: Sale type spinner - Setup with includeSaleTypeSelector binding
        // val saleTypes = arrayOf("Comptant", "Assurance", "Carnet")
        // val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, saleTypes)
        // spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // binding.includeSaleTypeSelector.spinnerSaleType.adapter = spinnerAdapter

        /* TODO: Enable this when spinner is properly bound
        binding.includeSaleTypeSelector.spinnerSaleType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newType = when (position) {
                    0 -> SaleType.Comptant
                    1 -> {
                        if (viewModel.selectedCustomer.value != null) {
                            SaleType.Assurance(viewModel.selectedCustomer.value!!, emptyList())
                        } else {
                            Toast.makeText(this@UnifiedSaleActivity, "Sélectionnez un client d'abord", Toast.LENGTH_SHORT).show()
                            binding.spinnerSaleType.setSelection(0)
                            return
                        }
                    }
                    2 -> {
                        if (viewModel.selectedCustomer.value != null) {
                            SaleType.Carnet(viewModel.selectedCustomer.value!!)
                        } else {
                            Toast.makeText(this@UnifiedSaleActivity, "Sélectionnez un client d'abord", Toast.LENGTH_SHORT).show()
                            binding.spinnerSaleType.setSelection(0)
                            return
                        }
                    }
                    else -> SaleType.Comptant
                }
                viewModel.changeSaleType(newType)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
        */

        // TODO: Customer selection - Bind to proper layout include
        // binding.includeCustomerZone.btnSelectCustomer.setOnClickListener {
        //     val intent = Intent(this, CustomerSelectionActivity::class.java)
        //     customerSelectionLauncher.launch(intent)
        // }

        // TODO: Product search - Bind to proper layout include
        // binding.includeProductCart.btnSearchProduct.setOnClickListener {
        //     val query = binding.includeProductCart.etProductSearch.text.toString()
        //     viewModel.searchProducts(query)
        // }

        // TODO: Clear search - Bind to proper layout include
        // binding.includeProductCart.btnClearSearch.setOnClickListener {
        //     binding.includeProductCart.etProductSearch.text?.clear()
        // }

        // TODO: Put on hold action - Bind to proper button
        // binding.btnPutOnHold.setOnClickListener {
        //     confirmPutOnHold()
        // }

        // Finalize sale action (using FAB which exists in main layout)
        binding.fabFinalizeSale.setOnClickListener {
            // TODO: Navigate to payment screen
            Toast.makeText(this, "Finalisation - à implémenter", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Sale type
        viewModel.currentSaleType.observe(this) { saleType ->
            updateUIForSaleType(saleType)
        }

        // Customer
        viewModel.selectedCustomer.observe(this) { customer ->
            // TODO: Update customer UI with proper binding
            // if (customer != null) {
            //     binding.includeCustomerZone.tvCustomerName.text = "${customer.firstName} ${customer.lastName}"
            //     binding.includeCustomerZone.layoutCustomerInfo.visibility = View.VISIBLE
            // } else {
            //     binding.includeCustomerZone.layoutCustomerInfo.visibility = View.GONE
            // }
        }

        // Customer required
        viewModel.customerRequired.observe(this) { required ->
            // TODO: Update customer required indicator with proper binding
            // binding.includeCustomerZone.btnSelectCustomer.isEnabled = true
            // if (required) {
            //     binding.includeCustomerZone.tvCustomerRequired.visibility = View.VISIBLE
            // } else {
            //     binding.includeCustomerZone.tvCustomerRequired.visibility = View.GONE
            // }
        }

        // Products
        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)
            // TODO: Update UI with product search results
            // binding.includeProductCart.rvProductSearchResults.visibility =
            //     if (products.isEmpty()) View.GONE else View.VISIBLE
        }

        // Cart
        viewModel.currentSale.observe(this) { sale ->
            cartAdapter.submitList(sale.salesLines)

            // TODO: Update totals and cart UI
            // binding.includePaymentZone.tvTotalAmount.text = formatAmount(sale.salesAmount) + " FCFA"
            // binding.includeProductCart.chipCartCount.text = "${sale.salesLines.size} article(s)"

            // Enable/disable finalize button
            val hasItems = sale.salesLines.isNotEmpty()
            binding.fabFinalizeSale.isEnabled = hasItems
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
    }

    private fun updateUIForSaleType(saleType: SaleType) {
        supportActionBar?.title = when (saleType) {
            is SaleType.Comptant -> "Vente Comptant"
            is SaleType.Assurance -> "Vente Assurance"
            is SaleType.Carnet -> "Vente Carnet"
        }

        // TODO: Show/hide type-specific UI elements
        // when (saleType) {
        //     is SaleType.Assurance -> {
        //         binding.includeInsuranceData.root.visibility = View.VISIBLE
        //         // Display tiers payants
        //     }
        //     is SaleType.Carnet -> {
        //         binding.includeCarnetInfo.root.visibility = View.VISIBLE
        //     }
        //     else -> {
        //         binding.includeInsuranceData.root.visibility = View.GONE
        //         binding.includeCarnetInfo.root.visibility = View.GONE
        //     }
        // }
    }

    private fun showAddToCartDialog(product: Product) {
        // TODO: Create proper dialog with quantity input
        // For now, use simple dialog
        MaterialAlertDialogBuilder(this)
            .setTitle("Ajouter au panier")
            .setMessage("${product.libelle}\nPrix: ${formatAmount(product.regularUnitPrice)} FCFA")
            .setPositiveButton("Ajouter") { dialog, which ->
                // TODO: Get quantity from dialog input
                val quantity = 1
                viewModel.addProductToCart(product, quantity)
            }
            .setNegativeButton("Annuler", null)
            .show()
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

        // TODO: Set initial sale type via spinner
        // val saleType = intent.getStringExtra(EXTRA_SALE_TYPE)
        // val initialType = when (saleType) {
        //     SALE_TYPE_ASSURANCE -> 1
        //     SALE_TYPE_CARNET -> 2
        //     else -> 0 // COMPTANT
        // }
        // binding.includeSaleTypeSelector.spinnerSaleType.setSelection(initialType)
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
                // TODO: Show transform sale dialog
                Toast.makeText(this, "Transformation - à implémenter", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun formatAmount(amount: Int): String {
        return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
    }
}
