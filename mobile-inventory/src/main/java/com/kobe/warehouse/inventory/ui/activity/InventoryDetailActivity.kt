package com.kobe.warehouse.inventory.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.model.Product
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.databinding.ActivityInventoryDetailBinding
import com.kobe.warehouse.inventory.scanner.BarcodeScanner
import com.kobe.warehouse.inventory.scanner.ScanResult
import com.kobe.warehouse.inventory.ui.adapter.InventoryLineAdapter
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryDetailState
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryDetailViewModel
import com.kobe.warehouse.inventory.utils.TokenManager

class InventoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventoryDetailBinding
    private val inventoryDetailViewModel: InventoryDetailViewModel by viewModels {
        InventoryDetailViewModelFactory(InventoryRepository(TokenManager(this)))
    }
    private lateinit var inventoryLineAdapter: InventoryLineAdapter
    private lateinit var barcodeScanner: BarcodeScanner
    private var inventoryId: Long = -1
    private var currentScannedProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get inventory ID from intent
        inventoryId = intent.getLongExtra(InventoryListActivity.EXTRA_INVENTORY_ID, -1)
        val inventoryName = intent.getStringExtra(InventoryListActivity.EXTRA_INVENTORY_NAME)

        if (inventoryId == -1L) {
            Toast.makeText(this, "Invalid inventory ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvInventoryTitle.text = inventoryName ?: "Inventaire"

        // Initialize barcode scanner
        barcodeScanner = BarcodeScanner(this)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        // Load inventory
        inventoryDetailViewModel.loadInventory(inventoryId)
    }

    private fun setupRecyclerView() {
        inventoryLineAdapter = InventoryLineAdapter { line ->
            // Edit existing line
            showQuantityDialog(line)
        }
        binding.rvInventoryLines.adapter = inventoryLineAdapter
    }

    private fun setupListeners() {
        binding.btnScan.setOnClickListener {
            barcodeScanner.startScan()
        }

        binding.btnSynchronize.setOnClickListener {
            inventoryDetailViewModel.synchronizeLines()
        }

        binding.btnCloseInventory.setOnClickListener {
            showCloseInventoryConfirmation()
        }
    }

    private fun setupObservers() {
        inventoryDetailViewModel.inventoryDetailState.observe(this) { state ->
            when (state) {
                is InventoryDetailState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
                is InventoryDetailState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is InventoryDetailState.InventoryLoaded -> {
                    binding.progressBar.visibility = View.GONE
                    // Inventory loaded, rayons will be auto-loaded
                }
                is InventoryDetailState.RayonsLoaded -> {
                    binding.progressBar.visibility = View.GONE
                    // Rayons loaded (if applicable)
                }
                is InventoryDetailState.LinesLoaded -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.lines.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvInventoryLines.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvInventoryLines.visibility = View.VISIBLE
                        inventoryLineAdapter.submitList(state.lines)
                    }
                }
                is InventoryDetailState.ProductFound -> {
                    binding.progressBar.visibility = View.GONE
                    currentScannedProduct = state.product
                    // Check if product is already in the list
                    val existingLine = inventoryDetailViewModel.getCurrentLines()
                        .find { it.produitId == state.product.id }

                    if (existingLine != null) {
                        showQuantityDialog(existingLine)
                    } else {
                        // Create new line
                        val newLine = StoreInventoryLine(
                            id = 0, // Will be set by server
                            storeInventoryId = inventoryId,
                            produitId = state.product.id,
                            produitLibelle = state.product.libelle,
                            produitCip = state.product.codeEan,
                            quantityInit = 0,
                            quantityOnHand = 0,
                            gap = 0,
                            rayonId = null,
                            rayonLibelle = null,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        showQuantityDialog(newLine)
                    }
                }
                is InventoryDetailState.LineSaved -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Quantité enregistrée", Toast.LENGTH_SHORT).show()
                    // Reload lines to show updated list
                    inventoryDetailViewModel.loadInventoryLines(inventoryId, null)
                }
                is InventoryDetailState.SyncSuccess -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, R.string.sync_success, Toast.LENGTH_SHORT).show()
                }
                is InventoryDetailState.InventoryClosed -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Inventaire clôturé", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is InventoryDetailState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showQuantityDialog(line: StoreInventoryLine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quantity_input, null)
        val etQuantity = dialogView.findViewById<EditText>(R.id.et_quantity)

        // Pre-fill with current quantity if editing
        if (line.quantityOnHand > 0) {
            etQuantity.setText(line.quantityOnHand.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(line.produitLibelle ?: getString(R.string.product_name))
            .setMessage(getString(R.string.current_quantity, line.quantityOnHand))
            .setView(dialogView)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val quantityStr = etQuantity.text.toString()
                if (quantityStr.isNotBlank()) {
                    val quantity = quantityStr.toIntOrNull()
                    if (quantity != null && quantity >= 0) {
                        val updatedLine = line.copy(
                            quantityOnHand = quantity,
                            gap = quantity - line.quantityInit,
                            updatedAt = System.currentTimeMillis()
                        )
                        inventoryDetailViewModel.updateInventoryLine(updatedLine)
                    } else {
                        Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCloseInventoryConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.close_inventory)
            .setMessage(R.string.close_inventory_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                inventoryDetailViewModel.closeInventory(inventoryId)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (val result = barcodeScanner.parseScanResult(requestCode, resultCode, data)) {
            is ScanResult.Success -> {
                inventoryDetailViewModel.searchProductByBarcode(result.barcode)
            }
            is ScanResult.Cancelled -> {
                Toast.makeText(this, R.string.scan_cancelled, Toast.LENGTH_SHORT).show()
            }
            is ScanResult.Error -> {
                Toast.makeText(this, "Erreur: ${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ViewModelFactory for InventoryDetailViewModel
    class InventoryDetailViewModelFactory(
        private val inventoryRepository: InventoryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InventoryDetailViewModel(inventoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
