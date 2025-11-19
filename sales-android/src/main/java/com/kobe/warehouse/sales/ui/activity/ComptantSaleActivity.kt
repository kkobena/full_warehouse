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
 */
class ComptantSaleActivity : BaseActivity() {

    private lateinit var binding: ActivityComptantSaleBinding
    private lateinit var viewModel: ComptantSaleViewModel
    private lateinit var productAdapter: ProductAdapter
    private lateinit var cartAdapter: CartAdapter

    private var isTablet = false

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
        val customerApiService = retrofit.create(CustomerApiService::class.java)

        val salesRepository = SalesRepository(salesApiService)
        val productRepository = ProductRepository(productApiService)
        val paymentRepository = PaymentRepository(paymentApiService)
        val customerRepository = CustomerRepository(customerApiService)

        val factory = ComptantSaleViewModelFactory(
            salesRepository,
            productRepository,
            paymentRepository,
            customerRepository,
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
            onRemoveClick = { saleLine -> confirmRemoveProduct(saleLine) }
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

            // Update total
            binding.tvTotal.text = sale.getFormattedSalesAmount()

            // Show/hide empty cart state
            if (sale.salesLines.isEmpty()) {
                binding.emptyCartState.visibility = View.VISIBLE
                binding.rvCart.visibility = View.GONE
                binding.btnCheckout.isEnabled = false
            } else {
                binding.emptyCartState.visibility = View.GONE
                binding.rvCart.visibility = View.VISIBLE
                binding.btnCheckout.isEnabled = true
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
                viewModel.clearFinalizedSale()
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
     * Handle intent extras (for editing existing sale)
     */
    private fun handleIntent() {
        val saleId = intent.getLongExtra("SALE_ID", 0)
        val saleDate = intent.getStringExtra("SALE_DATE")
        val isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)

        if (isEditMode && saleId > 0 && !saleDate.isNullOrEmpty()) {
            viewModel.loadSale(saleId, saleDate)
        }
    }

    /**
     * Confirm remove product from cart
     */
    private fun confirmRemoveProduct(saleLine: com.kobe.warehouse.sales.data.model.SaleLine) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Retirer du panier")
            .setMessage("Voulez-vous retirer ${saleLine.produitLibelle} du panier ?")
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
                finish()
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
            finish()
            return
        }

        // Show printing progress
        Toast.makeText(this, "Impression du reçu en cours...", Toast.LENGTH_SHORT).show()

        // Print receipt on background thread
        CoroutineScope(Dispatchers.Main).launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val receiptPrinter = ReceiptPrinter(this@ComptantSaleActivity)

                    // Get store information from preferences or constants
                    // TODO: Load from backend configuration or SharedPreferences
                    val storeName = getString(R.string.app_name)
                    val storeAddress = "Adresse de la pharmacie" // TODO: Load from config
                    val storePhone = "Tél: +XXX XXX XXX" // TODO: Load from config
                    val welcomeMsg = "Bienvenue dans notre pharmacie"
                    val footerNote = "Merci pour votre visite!"

                    // Get paper roll size from preferences (default: 58mm)
                    val prefs = getSharedPreferences("printer_settings", MODE_PRIVATE)
                    val paperRollSize = prefs.getString("paper_roll_size", "58") ?: "58"
                    val paperRoll = if (paperRollSize == "80") {
                        ReceiptPrinter.PaperRoll.MM_80
                    } else {
                        ReceiptPrinter.PaperRoll.MM_58
                    }

                    // Get cashier/seller info
                    // TODO: Load from user session
                    val cassierName = "Caissier" // TODO: Get from TokenManager or session
                    val sellerName = null // TODO: Get from sale or session

                    // Prepare payment details
                    val payments = sale.payments.mapNotNull { payment ->
                        payment.paymentMode?.let { mode ->
                            ReceiptPrinter.PaymentDetail(
                                paymentModeLabel = mode.libelle,
                                amount = payment.paidAmount,
                                isCash = mode.isCash()
                            )
                        }
                    }

                    // Get montant versé (amount given by customer for cash payments)
                    val montantVerse = sale.payments
                        .firstOrNull { it.paymentMode?.isCash() == true }
                        ?.montantVerse ?: sale.montantVerse

                    // Print receipt
                    receiptPrinter.printReceipt(
                        sale = sale,
                        storeName = storeName,
                        storeAddress = storeAddress,
                        storePhone = storePhone,
                        welcomeMsg = welcomeMsg,
                        footerNote = footerNote,
                        cassierName = cassierName,
                        sellerName = sellerName,
                        payments = payments,
                        montantVerse = montantVerse,
                        paperRoll = paperRoll
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ComptantSaleActivity", "Failed to print receipt", e)
                    false
                }
            }

            // Show result to user
            if (success) {
                Toast.makeText(this@ComptantSaleActivity, "Reçu imprimé avec succès", Toast.LENGTH_SHORT).show()
            } else {
                MaterialAlertDialogBuilder(this@ComptantSaleActivity)
                    .setTitle("Erreur d'impression")
                    .setMessage("Impossible d'imprimer le reçu. Vérifiez que l'imprimante est connectée.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .show()
                return@launch
            }

            finish()
        }
    }
}
