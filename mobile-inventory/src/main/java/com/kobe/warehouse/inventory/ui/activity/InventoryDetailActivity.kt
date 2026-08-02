package com.kobe.warehouse.inventory.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.model.InventoryAbilities
import com.kobe.warehouse.inventory.data.model.InventoryLot
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.databinding.ActivityInventoryDetailBinding
import com.kobe.warehouse.inventory.scanner.BarcodeScanner
import com.kobe.warehouse.inventory.scanner.ScanResult
import com.kobe.warehouse.inventory.ui.adapter.InventoryLineAdapter
import com.kobe.warehouse.inventory.ui.adapter.InventoryLotAdapter
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryDetailState
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryDetailViewModel
import com.kobe.warehouse.inventory.utils.NetworkMonitor
import java.time.LocalDate
import java.time.format.DateTimeParseException

class InventoryDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityInventoryDetailBinding
    private val inventoryDetailViewModel: InventoryDetailViewModel by viewModels {
        InventoryDetailViewModelFactory(InventoryRepository(this))
    }
    private lateinit var inventoryLineAdapter: InventoryLineAdapter
    private lateinit var barcodeScanner: BarcodeScanner
    private var inventoryId: Long = -1
    private var lotsDialog: AlertDialog? = null
    private var lotAdapter: InventoryLotAdapter? = null
    private var tvNoLots: android.widget.TextView? = null
    private var rayons: List<Rayon> = emptyList()
    private var selectedRayonId: Long? = null

    // Scan continu (caméra embarquée)
    private var continuousScanActive = false
    private var lastScanText: String? = null
    private var lastScanTime = 0L
    private val beepManager by lazy { BeepManager(this) }

    // Capture douchette HID (émulation clavier)
    private val hidBuffer = StringBuilder()
    private var hidLastKeyTime = 0L

    private val lineFilterValues = listOf("NONE", "NOT_UPDATED", "UPDATED", "GAP", "GAP_POSITIF", "GAP_NEGATIF")

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

        setupToolbar(binding.toolbar)
        binding.toolbar.title = inventoryName ?: getString(R.string.inventory_detail_title)

        // Initialize barcode scanner
        barcodeScanner = BarcodeScanner(this)

        // Scan continu : formats pharma (EAN, Code128, DataMatrix GS1, QR)
        binding.barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(
            listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.CODE_128,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.QR_CODE
            )
        )
        binding.barcodeView.setStatusText("")

        setupRecyclerView()
        setupListeners()
        setupObservers()

        // Load user abilities (close permission, blind mode) then inventory
        inventoryDetailViewModel.loadAbilities()
        inventoryDetailViewModel.loadInventory(inventoryId)
    }

    private fun setupRecyclerView() {
        inventoryLineAdapter = InventoryLineAdapter { line ->
            onLineSelected(line)
        }
        binding.rvInventoryLines.adapter = inventoryLineAdapter
    }

    /**
     * Produit suivi par lots → comptage lot par lot ; sinon saisie directe
     */
    private fun onLineSelected(line: StoreInventoryLine) {
        if (line.lotCount > 0) {
            inventoryDetailViewModel.loadLots(line)
        } else {
            showQuantityDialog(line)
        }
    }

    private fun setupListeners() {
        binding.btnScan.setOnClickListener {
            barcodeScanner.startScan()
        }

        binding.btnRayon.setOnClickListener {
            showRayonPicker()
        }

        binding.btnScanContinuous.setOnClickListener {
            toggleContinuousScan()
        }

        binding.btnLineFilter.setOnClickListener {
            showLineFilterPicker()
        }

        // Recherche avec debounce (400 ms)
        var searchRunnable: Runnable? = null
        binding.etSearch.doAfterTextChanged { text ->
            searchRunnable?.let { binding.etSearch.removeCallbacks(it) }
            val runnable = Runnable { inventoryDetailViewModel.setSearch(text?.toString()) }
            searchRunnable = runnable
            binding.etSearch.postDelayed(runnable, 400)
        }

        binding.btnSynchronize.setOnClickListener {
            inventoryDetailViewModel.synchronizeLines()
        }

        binding.btnCloseInventory.setOnClickListener {
            showCloseInventoryConfirmation()
        }
    }

    private fun setupObservers() {
        // Connectivité : bannière hors ligne
        NetworkMonitor.isOnline.observe(this) { online ->
            binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
        }

        // Permissions : clôture réservée + mode aveugle (stock masqué sans privilège)
        inventoryDetailViewModel.abilities.observe(this) { abilities ->
            binding.btnCloseInventory.visibility =
                if (InventoryAbilities.CLOSE_INVENTORY in abilities) View.VISIBLE else View.GONE

            // Les adaptateurs ne rebindent que les champs concernés, et seulement si
            // la valeur change réellement (voir InventoryLineAdapter.showStock)
            val showStock = InventoryAbilities.VIEW_STOCK in abilities
            inventoryLineAdapter.showStock = showStock
            lotAdapter?.showStock = showStock
        }

        // Lignes en attente de synchronisation : compteur sur le bouton
        inventoryDetailViewModel.pendingSyncCount.observe(this) { count ->
            binding.btnSynchronize.text = if (count > 0) {
                getString(R.string.synchronize_with_count, count)
            } else {
                getString(R.string.synchronize)
            }
        }

        // Comptages concurrents : bandeau cliquable pour arbitrer
        inventoryDetailViewModel.conflictCount.observe(this) { count ->
            if (count > 0) {
                binding.tvConflictBanner.visibility = View.VISIBLE
                binding.tvConflictBanner.text = getString(R.string.conflict_banner, count)
                binding.tvConflictBanner.setOnClickListener { showConflictDialog(count) }
            } else {
                binding.tvConflictBanner.visibility = View.GONE
            }
        }

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
                    // Rayons auto-loaded by the ViewModel; load all lines + progress
                    inventoryDetailViewModel.loadInventoryLines(inventoryId, null)
                    inventoryDetailViewModel.loadProgress()
                }
                is InventoryDetailState.RayonsLoaded -> {
                    binding.progressBar.visibility = View.GONE
                    rayons = state.rayons
                    binding.btnRayon.isEnabled = rayons.isNotEmpty()
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
                    inventoryDetailViewModel.loadProgress()
                }
                is InventoryDetailState.LineFound -> {
                    binding.progressBar.visibility = View.GONE
                    onLineSelected(state.line)
                }
                is InventoryDetailState.LotsLoaded -> {
                    binding.progressBar.visibility = View.GONE
                    showLotsDialog(state.line, state.lots)
                }
                is InventoryDetailState.LineSaved -> {
                    binding.progressBar.visibility = View.GONE
                    val message = if (state.synced) R.string.line_saved else R.string.line_saved_offline
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    // Reload lines keeping the current rayon filter
                    inventoryDetailViewModel.reloadLines()
                }
                is InventoryDetailState.LineIncremented -> {
                    // Mise à jour en place (pas de rechargement complet : cadence de scan)
                    inventoryLineAdapter.submitList(inventoryDetailViewModel.getCurrentLines().toList())
                    binding.barcodeView.setStatusText(
                        getString(
                            R.string.scan_increment_format,
                            state.line.produitLibelle ?: "",
                            state.line.quantityOnHand ?: 0
                        )
                    )
                }
                is InventoryDetailState.SyncSuccess -> {
                    binding.progressBar.visibility = View.GONE
                    val message = when {
                        state.conflicted > 0 -> getString(
                            R.string.sync_result_with_conflicts, state.saved, state.conflicted
                        )
                        state.failed > 0 ->
                            "Synchronisation: ${state.saved} enregistrées, ${state.failed} en échec"
                        else -> "Synchronisation réussie (${state.saved} lignes)"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
                is InventoryDetailState.InventoryClosed -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Inventaire clôturé (${state.itemsCount} articles)",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                is InventoryDetailState.CloseSummaryReady -> {
                    binding.progressBar.visibility = View.GONE
                    showCloseSummaryDialog(state.summary)
                }
                is InventoryDetailState.ProgressLoaded -> {
                    val progress = state.progress
                    binding.lpiProgress.setProgressCompat(progress.progressPercent, true)
                    binding.tvProgressCount.text = getString(
                        R.string.inventory_progress,
                        progress.updatedLines,
                        progress.totalLines,
                        progress.progressPercent
                    )
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
        val currentQuantity = line.quantityOnHand ?: 0
        if (currentQuantity > 0) {
            etQuantity.setText(currentQuantity.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(line.produitLibelle ?: getString(R.string.product_name))
            .setMessage(getString(R.string.current_quantity, currentQuantity))
            .setView(dialogView)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val quantityStr = etQuantity.text.toString()
                if (quantityStr.isNotBlank()) {
                    val quantity = quantityStr.toIntOrNull()
                    if (quantity != null && quantity >= 0) {
                        inventoryDetailViewModel.updateLineQuantity(line, quantity)
                    } else {
                        Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ── Scan continu (caméra embarquée, chaque scan = +1) ────────────────────

    private val continuousCallback = BarcodeCallback { result: BarcodeResult ->
        val text = result.text ?: return@BarcodeCallback
        val now = SystemClock.elapsedRealtime()
        // Anti-rebond : ignore le même code pendant 1,5 s
        if (text == lastScanText && now - lastScanTime < 1500) return@BarcodeCallback
        lastScanText = text
        lastScanTime = now
        beepManager.playBeepSoundAndVibrate()
        inventoryDetailViewModel.onBarcodeScanned(text, autoIncrement = true)
    }

    private fun toggleContinuousScan() {
        if (!continuousScanActive &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }
        setContinuousScan(!continuousScanActive)
    }

    private fun setContinuousScan(active: Boolean) {
        continuousScanActive = active
        binding.barcodeView.visibility = if (active) View.VISIBLE else View.GONE
        binding.btnScanContinuous.setText(
            if (active) R.string.continuous_scan_stop else R.string.continuous_scan
        )
        if (active) {
            binding.barcodeView.setStatusText("")
            binding.barcodeView.decodeContinuous(continuousCallback)
            binding.barcodeView.resume()
        } else {
            binding.barcodeView.pause()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setContinuousScan(true)
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (continuousScanActive) binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        if (continuousScanActive) binding.barcodeView.pause()
    }

    // ── Douchette HID (émulation clavier) ────────────────────────────────────
    // Un scan douchette = rafale de caractères (< 100 ms entre touches) + Entrée.
    // Chaque scan douchette incrémente la quantité de 1.

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val now = SystemClock.elapsedRealtime()
            if (now - hidLastKeyTime > 100) {
                hidBuffer.clear()
            }
            hidLastKeyTime = now

            if (event.keyCode == KeyEvent.KEYCODE_ENTER ||
                event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                if (hidBuffer.length >= 6) {
                    val code = hidBuffer.toString()
                    hidBuffer.clear()
                    beepManager.playBeepSoundAndVibrate()
                    inventoryDetailViewModel.onBarcodeScanned(code, autoIncrement = true)
                    return true
                }
                hidBuffer.clear()
            } else {
                val ch = event.unicodeChar
                if (ch != 0) {
                    hidBuffer.append(ch.toChar())
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ── Filtres de lignes ────────────────────────────────────────────────────

    private fun showLineFilterPicker() {
        val labels = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_not_updated),
            getString(R.string.filter_updated),
            getString(R.string.filter_gap),
            getString(R.string.filter_gap_positive),
            getString(R.string.filter_gap_negative)
        )
        val current = inventoryDetailViewModel.getLineFilter() ?: "NONE"
        val checkedIndex = lineFilterValues.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_filter)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                inventoryDetailViewModel.setLineFilter(lineFilterValues[which])
                binding.btnLineFilter.text = labels[which]
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Arbitrage d'un comptage concurrent : on ne fusionne pas silencieusement.
     * L'opérateur choisit de recharger les valeurs serveur (sa saisie sur ces
     * lignes est abandonnée et devra être recomptée).
     */
    private fun showConflictDialog(count: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.conflict_title)
            .setMessage(getString(R.string.conflict_message, count))
            .setPositiveButton(R.string.conflict_reload) { _, _ ->
                inventoryDetailViewModel.resolveConflicts()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRayonPicker() {
        val labels = mutableListOf(getString(R.string.all_rayons))
        labels.addAll(rayons.map { it.getDisplayName() })
        val checkedIndex = if (selectedRayonId == null) 0
        else rayons.indexOfFirst { it.id == selectedRayonId } + 1

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_rayon)
            .setSingleChoiceItems(labels.toTypedArray(), checkedIndex) { dialog, which ->
                selectedRayonId = if (which == 0) null else rayons[which - 1].id
                binding.btnRayon.text = labels[which]
                inventoryDetailViewModel.loadInventoryLines(inventoryId, selectedRayonId)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLotsDialog(line: StoreInventoryLine, lots: List<InventoryLot>) {
        val existingDialog = lotsDialog
        if (existingDialog != null && existingDialog.isShowing) {
            // Rafraîchit le contenu sans recréer la boîte de dialogue
            lotAdapter?.submitList(lots)
            tvNoLots?.visibility = if (lots.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_inventory_lots, null)
        val rvLots = dialogView.findViewById<RecyclerView>(R.id.rv_lots)
        tvNoLots = dialogView.findViewById(R.id.tv_no_lots)

        lotAdapter = InventoryLotAdapter(
            onLotClick = { lot -> showLotQuantityDialog(lot) },
            onLotDelete = { lot -> confirmDeleteLot(lot) }
        ).also {
            it.showStock = InventoryAbilities.VIEW_STOCK in
                (inventoryDetailViewModel.abilities.value ?: emptySet())
        }
        rvLots.layoutManager = LinearLayoutManager(this)
        rvLots.adapter = lotAdapter
        lotAdapter?.submitList(lots)
        tvNoLots?.visibility = if (lots.isEmpty()) View.VISIBLE else View.GONE

        dialogView.findViewById<View>(R.id.btn_add_lot).setOnClickListener {
            showAddLotDialog()
        }

        lotsDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.lots_title, line.produitLibelle ?: ""))
            .setView(dialogView)
            .setPositiveButton(R.string.close, null)
            .setOnDismissListener {
                lotsDialog = null
                lotAdapter = null
                tvNoLots = null
                // Rafraîchit la liste des lignes (quantité de la ligne = somme des lots)
                inventoryLineAdapter.submitList(inventoryDetailViewModel.getCurrentLines().toList())
                inventoryDetailViewModel.loadProgress()
            }
            .show()
    }

    private fun showLotQuantityDialog(lot: InventoryLot) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quantity_input, null)
        val etQuantity = dialogView.findViewById<EditText>(R.id.et_quantity)

        val currentQuantity = lot.quantityOnHand ?: 0
        if (currentQuantity > 0) {
            etQuantity.setText(currentQuantity.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(lot.numLot ?: getString(R.string.lot_number))
            .setMessage(getString(R.string.current_quantity, currentQuantity))
            .setView(dialogView)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val quantity = etQuantity.text.toString().toIntOrNull()
                if (quantity != null && quantity >= 0) {
                    inventoryDetailViewModel.updateLotQuantity(lot, quantity)
                } else {
                    Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddLotDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_lot, null)
        val etNumLot = dialogView.findViewById<EditText>(R.id.et_lot_number)
        val etExpiry = dialogView.findViewById<EditText>(R.id.et_lot_expiry)
        val etQuantity = dialogView.findViewById<EditText>(R.id.et_lot_quantity)

        // Pré-remplissage depuis le dernier scan GS1 DataMatrix (AI 10 = lot, AI 17 = péremption)
        inventoryDetailViewModel.lastScannedGs1?.let { gs1 ->
            gs1.lotNumber?.let { etNumLot.setText(it) }
            gs1.expiryIso?.let { etExpiry.setText(it) }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_lot)
            .setView(dialogView)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val numLot = etNumLot.text.toString().trim()
                val expiry = etExpiry.text.toString().trim().ifEmpty { null }
                val quantity = etQuantity.text.toString().toIntOrNull()

                if (numLot.isEmpty()) {
                    Toast.makeText(this, R.string.lot_number, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (quantity == null || quantity < 0) {
                    Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (expiry != null) {
                    try {
                        LocalDate.parse(expiry)
                    } catch (_: DateTimeParseException) {
                        Toast.makeText(this, R.string.lot_invalid_expiry, Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }
                inventoryDetailViewModel.addLot(numLot, expiry, quantity)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteLot(lot: InventoryLot) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lot_delete)
            .setMessage(getString(R.string.lot_delete_confirm, lot.numLot ?: ""))
            .setPositiveButton(R.string.yes) { _, _ ->
                inventoryDetailViewModel.deleteLot(lot)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showCloseInventoryConfirmation() {
        // Récapitulatif avant clôture (la clôture est irréversible)
        inventoryDetailViewModel.prepareCloseSummary()
    }

    private fun showCloseSummaryDialog(summary: com.kobe.warehouse.inventory.ui.viewmodel.CloseSummary) {
        val nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.FRENCH)
        val message = buildString {
            append(getString(R.string.close_summary_counted, summary.countedLines, summary.totalLines))
            append('\n')
            append(getString(R.string.close_summary_remaining, summary.remainingLines))
            append('\n')
            append(getString(R.string.close_summary_gap_lines, summary.gapLineCount))
            append('\n')
            append(
                getString(
                    R.string.close_summary_gap_values,
                    nf.format(summary.gapValueAchat),
                    nf.format(summary.gapValueVente)
                )
            )
            if (summary.remainingLines > 0) {
                append("\n\n")
                append(getString(R.string.close_summary_warning_remaining))
            }
            append("\n\n")
            append(getString(R.string.close_inventory_confirm))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.close_inventory)
            .setMessage(message)
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
                inventoryDetailViewModel.onBarcodeScanned(result.barcode, autoIncrement = false)
            }
            is ScanResult.Cancelled -> {
                Toast.makeText(this, R.string.scan_cancelled, Toast.LENGTH_SHORT).show()
            }
            is ScanResult.Error -> {
                Toast.makeText(this, "Erreur: ${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
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
