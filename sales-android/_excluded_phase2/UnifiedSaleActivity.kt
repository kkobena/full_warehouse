package com.kobe.warehouse.sales.ui.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.*
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.*
import com.kobe.warehouse.sales.databinding.ActivityUnifiedSaleBinding
import com.kobe.warehouse.sales.domain.model.SaleType
import com.kobe.warehouse.sales.ui.adapter.CustomerSearchAdapter
import com.kobe.warehouse.sales.ui.dialog.AyantDroitDialog
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModel
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModelFactory
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Unified Sale Activity
 *
 * Handles all types of sales: Comptant, Assurance, Carnet
 * Provides adaptive UI based on selected sale type
 */
class UnifiedSaleActivity : BaseActivity() {

    private lateinit var binding: ActivityUnifiedSaleBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var customerSearchAdapter: CustomerSearchAdapter
    private var selectedAyantDroit: Customer? = null

    private val viewModel: UnifiedSaleViewModel by lazy {
        val apiClient = ApiClient.getRetrofitInstance(tokenManager.getBaseUrl())
        val salesApi = apiClient.create(SalesApiService::class.java)
        val productApi = apiClient.create(ProductApiService::class.java)
        val customerApi = apiClient.create(CustomerApiService::class.java)
        val paymentApi = apiClient.create(PaymentApiService::class.java)
        val authApi = apiClient.create(AuthApiService::class.java)

        val salesRepository = SalesRepository(salesApi)
        val productRepository = ProductRepository(productApi)
        val customerRepository = CustomerRepository(customerApi)
        val paymentRepository = PaymentRepository(paymentApi)
        val authRepository = AuthRepository(authApi, tokenManager)

        val factory = UnifiedSaleViewModelFactory(
            salesRepository = salesRepository,
            productRepository = productRepository,
            customerRepository = customerRepository,
            paymentRepository = paymentRepository,
            authRepository = authRepository,
            tokenManager = tokenManager
        )
        ViewModelProvider(this, factory)[UnifiedSaleViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnifiedSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupToolbar()
        setupSaleTypeSelector()
        setupCustomerZone()
        setupInsuranceDataZone()
        setupCarnetInfoZone()
        setupProductCartZone()
        setupPaymentZone()
        setupFabButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.nouvelle_vente)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    /**
     * Setup Sale Type Selector (Chips)
     */
    private fun setupSaleTypeSelector() {
        val saleTypeSelector = binding.includeSaleTypeSelector

        saleTypeSelector.chipGroupSaleType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val selectedSaleType = when (checkedIds.first()) {
                R.id.chipComptant -> SaleType.Comptant
                R.id.chipAssurance -> {
                    // Will be set to Assurance once customer and tiers payants are selected
                    // For now, just notify that customer is required
                    showCustomerRequired()
                    SaleType.Comptant // Temporary, will change when customer selected
                }
                R.id.chipCarnet -> {
                    // Will be set to Carnet once customer is selected
                    showCustomerRequired()
                    SaleType.Comptant // Temporary, will change when customer selected
                }
                else -> SaleType.Comptant
            }

            viewModel.onSaleTypeChanged(selectedSaleType)
        }
    }

    /**
     * Setup Customer Zone
     */
    private fun setupCustomerZone() {
        val customerZone = binding.includeCustomerZone

        // Expand/Collapse customer zone
        customerZone.layoutCustomerHeader.setOnClickListener {
            toggleCustomerZone()
        }

        // Customer search
        customerZone.etCustomerSearch.setOnEditorActionListener { _, _, _ ->
            val query = customerZone.etCustomerSearch.text.toString()
            if (query.length >= 2) {
                viewModel.searchCustomers(query)
            }
            true
        }

        // Change customer button
        customerZone.btnChangeCustomer.setOnClickListener {
            clearSelectedCustomer()
        }

        // Setup customer search results RecyclerView
        customerSearchAdapter = CustomerSearchAdapter { customer ->
            viewModel.selectCustomer(customer)
        }
        customerZone.rvCustomerSearchResults.apply {
            layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
            adapter = customerSearchAdapter
        }
    }

    /**
     * Setup Insurance Data Zone
     */
    private fun setupInsuranceDataZone() {
        val insuranceZone = binding.includeInsuranceData

        // Select ayant droit button
        insuranceZone.btnSelectAyantDroit.setOnClickListener {
            val customer = viewModel.selectedCustomer.value
            if (customer != null) {
                openAyantDroitDialog(customer.id)
            } else {
                showSnackbar("Veuillez sélectionner un client d'abord")
            }
        }

        // Remove ayant droit button
        insuranceZone.btnRemoveAyantDroit.setOnClickListener {
            clearAyantDroit()
        }

        // TODO: Setup other insurance data fields in Task #5
        // - Tiers payant selection
        // - Prescription type selection
        // - Coverage rate input
    }

    /**
     * Open ayant droit selection dialog
     */
    private fun openAyantDroitDialog(customerId: Long) {
        // TODO: Load ayants-droit from ViewModel
        // For now, show placeholder dialog
        showSnackbar("Chargement des ayants-droit...")
        // This will be fully implemented in Task #5
    }

    /**
     * Show selected ayant droit
     */
    private fun showSelectedAyantDroit(ayantDroit: Customer) {
        selectedAyantDroit = ayantDroit
        val insuranceZone = binding.includeInsuranceData

        insuranceZone.layoutSelectedAyantDroit.visibility = View.VISIBLE
        insuranceZone.tvAyantDroitName.text = "${ayantDroit.firstName} ${ayantDroit.lastName} (Bénéficiaire)"
    }

    /**
     * Clear selected ayant droit
     */
    private fun clearAyantDroit() {
        selectedAyantDroit = null
        binding.includeInsuranceData.layoutSelectedAyantDroit.visibility = View.GONE
    }

    /**
     * Setup Carnet Info Zone
     */
    private fun setupCarnetInfoZone() {
        val carnetZone = binding.includeCarnetInfo

        // View carnet history button
        carnetZone.btnViewCarnetHistory.setOnClickListener {
            // TODO: Open carnet history dialog in Task #6
            showSnackbar("Historique carnet à implémenter")
        }
    }

    /**
     * Setup Product & Cart Zone
     */
    private fun setupProductCartZone() {
        val productCartZone = binding.includeProductCart

        // Product search
        productCartZone.etProductSearch.setOnEditorActionListener { _, _, _ ->
            val query = productCartZone.etProductSearch.text.toString()
            if (query.length >= 2) {
                viewModel.searchProducts(query)
            }
            true
        }

        // Barcode scanner button
        productCartZone.tilProductSearch.setEndIconOnClickListener {
            // TODO: Launch barcode scanner
            showSnackbar("Scanner code-barres à implémenter")
        }

        // Setup product search results RecyclerView
        productCartZone.rvProductSearchResults.layoutManager = LinearLayoutManager(this)
        // TODO: Set adapter

        // Setup cart RecyclerView
        productCartZone.rvCart.layoutManager = LinearLayoutManager(this)
        // TODO: Set adapter
    }

    /**
     * Setup Payment Zone
     */
    private fun setupPaymentZone() {
        val paymentZone = binding.includePaymentZone

        // Setup payment modes RecyclerView
        paymentZone.rvPaymentModes.layoutManager = LinearLayoutManager(this)
        // TODO: Set adapter when created
    }

    /**
     * Setup Finalize Sale FAB Button
     */
    private fun setupFabButton() {
        binding.fabFinalizeSale.setOnClickListener {
            viewModel.finalizeSale()
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe current sale type
        viewModel.currentSaleType.observe(this) { saleType ->
            updateUIForSaleType(saleType)
        }

        // Observe customer required state
        viewModel.isCustomerRequired.observe(this) { isRequired ->
            binding.includeCustomerZone.tvCustomerRequired.visibility =
                if (isRequired) View.VISIBLE else View.GONE
        }

        // Observe selected customer
        viewModel.selectedCustomer.observe(this) { customer ->
            if (customer != null) {
                showSelectedCustomer(customer)
            } else {
                clearSelectedCustomerUI()
            }
        }

        // Observe customer search results
        viewModel.customerSearchResults.observe(this) { customers ->
            val customerZone = binding.includeCustomerZone
            if (customers.isEmpty()) {
                customerZone.rvCustomerSearchResults.visibility = View.GONE
            } else {
                customerZone.rvCustomerSearchResults.visibility = View.VISIBLE
                customerSearchAdapter.submitList(customers)
            }
        }

        // Observe cart total
        viewModel.cartTotal.observe(this) { total ->
            binding.includePaymentZone.tvTotalAmount.text = total.toString()
        }

        // Observe carnet data
        viewModel.carnetData.observe(this) { carnetData ->
            carnetData?.let { updateCarnetInfo(it) }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showSnackbar(it)
                viewModel.clearError()
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // TODO: Show/hide loading indicator
        }
    }

    /**
     * Update UI based on selected sale type
     */
    private fun updateUIForSaleType(saleType: SaleType) {
        when (saleType) {
            is SaleType.Comptant -> {
                // Customer optional
                binding.includeCustomerZone.tvCustomerRequired.visibility = View.GONE
                // Hide insurance and carnet zones
                binding.includeInsuranceData.root.visibility = View.GONE
                binding.includeCarnetInfo.root.visibility = View.GONE
                // Show payment mode selection
                binding.includePaymentZone.layoutPaymentModeSelection.visibility = View.VISIBLE
                binding.includePaymentZone.layoutCarnetPaymentInfo.visibility = View.GONE
            }
            is SaleType.Assurance -> {
                // Customer mandatory
                binding.includeCustomerZone.tvCustomerRequired.visibility = View.VISIBLE
                // Show insurance zone, hide carnet zone
                binding.includeInsuranceData.root.visibility = View.VISIBLE
                binding.includeCarnetInfo.root.visibility = View.GONE
                // Show payment mode selection (for client part)
                binding.includePaymentZone.layoutPaymentModeSelection.visibility = View.VISIBLE
                binding.includePaymentZone.layoutCarnetPaymentInfo.visibility = View.GONE
            }
            is SaleType.Carnet -> {
                // Customer mandatory
                binding.includeCustomerZone.tvCustomerRequired.visibility = View.VISIBLE
                // Hide insurance zone, show carnet zone
                binding.includeInsuranceData.root.visibility = View.GONE
                binding.includeCarnetInfo.root.visibility = View.VISIBLE
                // Hide payment mode selection, show carnet info
                binding.includePaymentZone.layoutPaymentModeSelection.visibility = View.GONE
                binding.includePaymentZone.layoutCarnetPaymentInfo.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Toggle Customer Zone expansion
     */
    private fun toggleCustomerZone() {
        val customerZone = binding.includeCustomerZone
        val isExpanded = customerZone.layoutCustomerContent.visibility == View.VISIBLE

        if (isExpanded) {
            customerZone.layoutCustomerContent.visibility = View.GONE
            customerZone.ivExpandCustomer.rotation = 0f
        } else {
            customerZone.layoutCustomerContent.visibility = View.VISIBLE
            customerZone.ivExpandCustomer.rotation = 180f
        }
    }

    /**
     * Show customer required message
     */
    private fun showCustomerRequired() {
        showSnackbar(getString(R.string.obligatoire) + ": " + getString(R.string.client))
        // Expand customer zone
        binding.includeCustomerZone.layoutCustomerContent.visibility = View.VISIBLE
        binding.includeCustomerZone.ivExpandCustomer.rotation = 180f
    }

    /**
     * Show selected customer info
     */
    private fun showSelectedCustomer(customer: com.kobe.warehouse.sales.data.model.Customer) {
        val customerZone = binding.includeCustomerZone

        // Hide search field, show selected customer
        customerZone.tilCustomerSearch.visibility = View.GONE
        customerZone.rvCustomerSearchResults.visibility = View.GONE
        customerZone.layoutSelectedCustomer.visibility = View.VISIBLE

        // Set customer info
        customerZone.tvCustomerName.text = "${customer.firstName} ${customer.lastName}"
        customerZone.tvCustomerPhone.text = "Tel: ${customer.phone}"

        // Show insurance info if available
        // TODO: Implement when customer insurance data is available
    }

    /**
     * Clear selected customer UI
     */
    private fun clearSelectedCustomerUI() {
        val customerZone = binding.includeCustomerZone

        // Show search field, hide selected customer
        customerZone.tilCustomerSearch.visibility = View.VISIBLE
        customerZone.layoutSelectedCustomer.visibility = View.GONE
        customerZone.etCustomerSearch.text?.clear()
        customerZone.rvCustomerSearchResults.visibility = View.GONE

        // Clear ayant droit if any
        clearAyantDroit()
    }

    /**
     * Clear selected customer (call ViewModel)
     */
    private fun clearSelectedCustomer() {
        viewModel.clearCustomer()
    }

    /**
     * Update carnet info display
     */
    private fun updateCarnetInfo(carnetData: com.kobe.warehouse.sales.domain.model.CarnetData) {
        val carnetZone = binding.includeCarnetInfo

        carnetZone.tvLimiteCredit.text = "${carnetData.limiteCredit} FCFA"
        carnetZone.tvEncours.text = "${carnetData.encours} FCFA"
        carnetZone.tvCreditDisponible.text = "${carnetData.creditDisponible} FCFA"

        // Credit usage progress
        val usagePercentage = carnetData.getCreditUsagePercentage()
        carnetZone.tvCreditUsagePercentage.text = "$usagePercentage%"
        carnetZone.progressCreditUsage.progress = usagePercentage

        // Show warning if usage > 90%
        if (usagePercentage >= 90) {
            carnetZone.cardCreditWarning.visibility = View.VISIBLE
            carnetZone.tvCreditWarning.text = "Attention: le client utilise $usagePercentage% de sa limite de crédit"
        } else {
            carnetZone.cardCreditWarning.visibility = View.GONE
        }
    }

    /**
     * Show snackbar message
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
