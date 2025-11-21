package com.kobe.warehouse.sales.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.*
import com.kobe.warehouse.sales.data.repository.*
import com.kobe.warehouse.sales.databinding.ActivityComptantSaleBinding
import com.kobe.warehouse.sales.ui.adapter.CartAdapter
import com.kobe.warehouse.sales.ui.adapter.ProductAdapter
import com.kobe.warehouse.sales.ui.dialog.PaymentDialogFragment
import com.kobe.warehouse.sales.ui.dialog.ValidationConfirmationDialog
import com.kobe.warehouse.sales.service.SaleStockValidator
import com.kobe.warehouse.sales.ui.viewmodel.ComptantSaleViewModel
import com.kobe.warehouse.sales.ui.viewmodel.ComptantSaleViewModelFactory
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import com.kobe.warehouse.sales.printer.ReceiptPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ComptantSaleActivity
 * POS (Point of Sale) screen for cash sales
 * Extends BaseActivity for automatic session management
 *
 * Features:
 * - Product search with autocomplete
 * - Barcode scanning
 * - Shopping cart management
 * - Quantity adjustments
 * - Checkout with payment
 * - Responsive layout (phone/tablet)
 * - View-only mode for finalized sales
 */
class ComptantSaleActivity : BaseActivity() {

    private lateinit var binding: ActivityComptantSaleBinding
    private lateinit var viewModel: ComptantSaleViewModel
    private lateinit var productAdapter: ProductAdapter
    private lateinit var cartAdapter: CartAdapter
    private lateinit var storeRepository: StoreRepository

    private var isTablet = false
    private var isViewMode = false  // Read-only mode for finalized sales
    private var currentStore: com.kobe.warehouse.sales.data.model.Store? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityComptantSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if tablet (screen width >= 600dp)
        isTablet = resources.getBoolean(R.bool.isTablet)

        setupViewModel()
        setupToolbar()
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        loadStoreInfo()

        // Load existing sale if editing
        handleIntent()
    }

    /**
     * Setup ViewModel with dependencies
     */
    private fun setupViewModel() {
        val tokenManager = TokenManager(this)
        val retrofit = ApiClient.create(tokenManager = tokenManager)

        val salesApiService = retrofit.create(SalesApiService::class.java)
        val productApiService = retrofit.create(ProductApiService::class.java)
        val paymentApiService = retrofit.create(PaymentApiService::class.java)
        val authApiService = retrofit.create(AuthApiService::class.java)
        val storeApiService = retrofit.create(com.kobe.warehouse.sales.data.api.StoreApiService::class.java)

        val salesRepository = SalesRepository(salesApiService)
        val productRepository = ProductRepository(productApiService)
        val paymentRepository = PaymentRepository(paymentApiService)
        val authRepository = AuthRepository(authApiService, tokenManager)
        storeRepository = StoreRepository(storeApiService, this)

        val factory = ComptantSaleViewModelFactory(
            salesRepository,
            productRepository,
            paymentRepository,
            authRepository,
            tokenManager
        )
        viewModel = ViewModelProvider(this, factory)[ComptantSaleViewModel::class.java]
    }

    /**
     * Setup toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Vente Comptant"
        }
    }

    /**
     * Setup RecyclerViews
     */
    private fun setupRecyclerViews() {
        // Product search results adapter
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                viewModel.addProductToCart(product)
                binding.etSearch.setText("")
                binding.rvProducts.visibility = View.GONE
            },
            isGridLayout = isTablet
        )

        binding.rvProducts.apply {
            layoutManager = if (isTablet) {
                GridLayoutManager(this@ComptantSaleActivity, 2)
            } else {
                LinearLayoutManager(this@ComptantSaleActivity)
            }
            adapter = productAdapter
        }

        // Cart adapter
        cartAdapter = CartAdapter(
            onIncrementClick = { saleLine -> viewModel.incrementQuantity(saleLine) },
            onDecrementClick = { saleLine -> viewModel.decrementQuantity(saleLine) },
            onRemoveClick = { saleLine -> confirmRemoveProduct(saleLine) },
            onQuantityChange = { saleLine, newQuantity -> viewModel.updateProductQuantity(saleLine, newQuantity) }
        )

        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(this@ComptantSaleActivity)
            adapter = cartAdapter
        }
    }

    /**
     * Setup listeners
     */
    private fun setupListeners() {
        // Search text listener
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString()
            if (query.length >= 2) {
                viewModel.searchProducts(query)
            } else {
                binding.rvProducts.visibility = View.GONE
            }
        }

        // Barcode scanner button
        binding.searchInputLayout.setEndIconOnClickListener {
            // TODO: Implement barcode scanner using ZXing
            Toast.makeText(this, "Scanner de code-barres bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        // Clear cart button
        binding.btnClearCart.setOnClickListener {
            confirmClearCart()
        }

        // Checkout button
        binding.btnCheckout.setOnClickListener {
            openPaymentDialog()
        }

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe product search results
        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)
            binding.rvProducts.visibility = if (products.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Observe cart changes
        viewModel.currentSale.observe(this) { sale ->
            cartAdapter.submitList(sale.salesLines)

            // Update cart item count
            val itemCount = sale.salesLines.size
            binding.tvCartItemCount.text = itemCount.toString()

            // Update VAT display
            val vatAmount = sale.getTotalTax()
            if (vatAmount > 0) {
                binding.vatRow.visibility = View.VISIBLE
                binding.tvVat.text = sale.getFormattedTaxAmount()
            } else {
                binding.vatRow.visibility = View.GONE
            }

            // Update total
            binding.tvTotal.text = sale.getFormattedSalesAmount()

            // Show/hide empty cart state and buttons
            if (sale.salesLines.isEmpty()) {
                binding.emptyCartState.visibility = View.VISIBLE
                binding.rvCart.visibility = View.GONE
                binding.btnCheckout.isEnabled = false
                binding.btnClearCart.visibility = View.GONE
                binding.vatRow.visibility = View.GONE
            } else {
                binding.emptyCartState.visibility = View.GONE
                binding.rvCart.visibility = View.VISIBLE
                binding.btnCheckout.isEnabled = true
                binding.btnClearCart.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe search loading
        viewModel.isSearching.observe(this) { isSearching ->
            // Could show a small progress indicator in search field
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe sale finalized
        viewModel.saleFinalized.observe(this) { finalizedSale ->
            finalizedSale?.let {
                showSaleCompletedDialog(it.numberTransaction)
                // Don't clear here - will be cleared after printing or user declines
            }
        }

        // Observe validation results
        viewModel.validationResult.observe(this) { validationResult ->
            validationResult?.let {
                handleValidationResult(it)
            }
        }
    }

    /**
     * Handle stock validation results
     * Shows appropriate dialog based on validation reason
     */
    private fun handleValidationResult(result: SaleStockValidator.ValidationResult) {
        when (result.reason) {
            SaleStockValidator.REASON_FORCE_STOCK,
            SaleStockValidator.REASON_FORCE_STOCK_AND_QUANTITY_EXCEEDS_MAX -> {
                // Show confirmation dialog for force stock
                ValidationConfirmationDialog.forceStock(
                    message = result.message ?: "La quantité demandée est supérieure au stock. Continuer ?",
                    onConfirm = {
                        viewModel.confirmForceStock()
                    },
                    onCancel = {
                        viewModel.cancelProductAddition()
                    }
                ).show(supportFragmentManager, "force_stock_dialog")
            }

            SaleStockValidator.REASON_QUANTITY_EXCEEDS_MAX -> {
                // Show error for quantity exceeds max (cannot force)
                ValidationConfirmationDialog.error(
                    title = "Quantité maximale dépassée",
                    message = result.message ?: "La quantité demandée dépasse le maximum autorisé",
                    onDismiss = {
                        viewModel.cancelProductAddition()
                    }
                ).show(supportFragmentManager, "quantity_max_error")
            }

            SaleStockValidator.REASON_STOCK_INSUFFISANT -> {
                // Show error for insufficient stock (cannot force)
                ValidationConfirmationDialog.error(
                    title = "Stock insuffisant",
                    message = result.message ?: "Stock insuffisant",
                    onDismiss = {
                        viewModel.cancelProductAddition()
                    }
                ).show(supportFragmentManager, "stock_error")
            }

            SaleStockValidator.REASON_DECONDITIONNEMENT -> {
                // TODO: Implement deconditionnement dialog
                // For now, just show error
                ValidationConfirmationDialog.error(
                    title = "Déconditionnement requis",
                    message = result.message ?: "Stock insuffisant. Utiliser le conditionnement supérieur ?",
                    onDismiss = {
                        viewModel.cancelProductAddition()
                    }
                ).show(supportFragmentManager, "deconditionnement_dialog")
            }

            SaleStockValidator.REASON_INVALID_QUANTITY -> {
                // Show error for invalid quantity
                Toast.makeText(this, result.message ?: "Quantité invalide", Toast.LENGTH_SHORT).show()
                viewModel.cancelProductAddition()
            }
        }
    }

    /**
     * Load store information from backend with caching
     */
    private fun loadStoreInfo() {
        CoroutineScope(Dispatchers.Main).launch {
            storeRepository.getStore().fold(
                onSuccess = { store ->
                    currentStore = store
                    android.util.Log.d("ComptantSaleActivity", "Store info loaded: ${store.getDisplayName()}")
                },
                onFailure = { error ->
                    // Log error but don't block UI since store info is not critical for POS operations
                    android.util.Log.e("ComptantSaleActivity", "Failed to load store info: ${error.message}")
                    // App can continue without store info - will use fallback values in receipt
                }
            )
        }
    }

    /**
     * Handle intent extras (for viewing finalized sale)
     */
    private fun handleIntent() {
        val saleId = intent.getLongExtra("SALE_ID", 0)
        val saleDate = intent.getStringExtra("SALE_DATE")
        isViewMode = intent.getBooleanExtra("IS_VIEW_MODE", false)

        if (isViewMode && saleId > 0 && !saleDate.isNullOrEmpty()) {
            // Load sale for viewing
            viewModel.loadSale(saleId, saleDate)

            // Disable all editing features
            disableEditing()
        }
    }

    /**
     * Disable editing features when in view-only mode
     */
    private fun disableEditing() {
        // Change toolbar title
        supportActionBar?.title = "Détails de la vente"

        // Disable product search
        binding.etSearch.isEnabled = false
        binding.searchInputLayout.isEnabled = false

        // Hide product search results
        binding.rvProducts.visibility = View.GONE

        // Hide checkout button (sale already finalized)
        binding.btnCheckout.visibility = View.GONE

        // Hide clear cart button
        binding.btnClearCart.visibility = View.GONE

        // Recreate cart adapter in view-only mode
        cartAdapter = CartAdapter(
            onIncrementClick = { },  // No-op
            onDecrementClick = { },  // No-op
            onRemoveClick = { },     // No-op
            onQuantityChange = { _, _ -> },  // No-op
            isViewMode = true        // Read-only mode
        )
        binding.rvCart.adapter = cartAdapter

        // Refresh cart display
        viewModel.currentSale.value?.let { sale ->
            cartAdapter.submitList(sale.salesLines)
        }
    }

    /**
     * Confirm remove product from cart
     */
    private fun confirmRemoveProduct(saleLine: com.kobe.warehouse.sales.data.model.SaleLine) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Retirer du panier")
            .setMessage("Voulez-vous retirer ${saleLine.produitLibelle ?: "ce produit"} du panier ?")
            .setPositiveButton("Retirer") { _, _ ->
                viewModel.removeProductFromCart(saleLine)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Confirm clear entire cart
     */
    private fun confirmClearCart() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Vider le panier")
            .setMessage("Voulez-vous vider tout le panier ?")
            .setPositiveButton("Vider") { _, _ ->
                viewModel.clearCart()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Open payment dialog
     */
    private fun openPaymentDialog() {
        val currentSale = viewModel.currentSale.value
        if (currentSale == null || currentSale.salesLines.isEmpty()) {
            Toast.makeText(this, "Le panier est vide", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = PaymentDialogFragment.newInstance(currentSale.salesAmount)
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
                // User declined to print - reset cart and stay on screen
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

        // Show printing progress
        Toast.makeText(this, "Impression du reçu en cours...", Toast.LENGTH_SHORT).show()

        // Print receipt on background thread
        CoroutineScope(Dispatchers.Main).launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val receiptPrinter = ReceiptPrinter(this@ComptantSaleActivity)
                    // Print receipt (ReceiptPrinter automatically loads store and account info)
                    receiptPrinter.printReceipt(sale = sale)
                } catch (e: Exception) {
                    android.util.Log.e("ComptantSaleActivity", "Failed to print receipt", e)
                    false
                }
            }

            // Show result to user and reset cart (stay on screen)
            if (success) {
                // Reset cart after successful printing and stay on screen
                viewModel.resetAfterSale()
                Toast.makeText(
                    this@ComptantSaleActivity,
                    "Reçu imprimé avec succès. Prêt pour une nouvelle vente",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                MaterialAlertDialogBuilder(this@ComptantSaleActivity)
                    .setTitle("Erreur d'impression")
                    .setMessage("Impossible d'imprimer le reçu. Vérifiez que l'imprimante est connectée.")
                    .setPositiveButton("OK") { _, _ ->
                        // Reset cart even if printing failed and stay on screen
                        viewModel.resetAfterSale()
                        Toast.makeText(
                            this@ComptantSaleActivity,
                            "Prêt pour une nouvelle vente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
            }
        }
    }
}
