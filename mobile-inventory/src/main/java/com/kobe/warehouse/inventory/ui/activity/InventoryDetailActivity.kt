package com.kobe.warehouse.inventory.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.kobe.warehouse.inventory.scanner.CameraScannerSession
import com.kobe.warehouse.inventory.scanner.ScanFeedback
import com.kobe.warehouse.inventory.scanner.ScanResult
import com.kobe.warehouse.inventory.ui.adapter.InventoryLineAdapter
import com.kobe.warehouse.inventory.ui.adapter.InventoryLotAdapter
import com.kobe.warehouse.inventory.ui.adapter.InventoryLotLineAdapter
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

    /** Mode lot : la liste affiche un lot par ligne (voir InventoryLotLineAdapter) */
    private var lotLineAdapter: InventoryLotLineAdapter? = null
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
    private var scannerSession: CameraScannerSession? = null
    private val feedback by lazy { ScanFeedback(this) }

    // Capture douchette HID (émulation clavier)
    private val hidBuffer = StringBuilder()
    private var hidLastKeyTime = 0L

    /**
     * Mêmes filtres que les grilles web (LINE_FILTERS). Les trois filtres d'écart
     * exposent le stock théorique : ils ne sont proposés qu'avec le privilège
     * pr-voir-stock-inventaire, sinon « avec écart » suffirait à contourner le
     * mode aveugle.
     */
    private val lineFilters = listOf(
        "NONE" to R.string.filter_all,
        "NOT_UPDATED" to R.string.filter_not_updated,
        "UPDATED" to R.string.filter_updated,
        "GAP" to R.string.filter_gap,
        "GAP_POSITIF" to R.string.filter_gap_positive,
        "GAP_NEGATIF" to R.string.filter_gap_negative
    )
    private val gapFilters = setOf("GAP", "GAP_POSITIF", "GAP_NEGATIF")
    private var canViewStock = false

    /**
     * Ligne à ramener dans le champ de vision à la prochaine publication de liste.
     * Le défilement ne peut pas être déclenché au moment de la saisie : ListAdapter
     * calcule son diff en arrière-plan, l'index n'est fiable qu'une fois la liste
     * effectivement publiée (callback de submitList).
     */
    private var pendingScrollLineId: Long? = null

    /** Une quantité est en cours de saisie dans la liste */
    private var isEditingQuantity = false

    /** Le prochain retour de sauvegarde ne doit pas repositionner la liste */
    private var suppressNextAutoScroll = false
    private var undoAvailable = false
    private var closeAllowed = false

    /** Toutes les lignes de l'inventaire ont reçu un comptage */
    private var countingComplete = false
    private var remainingLines = 0L

    private fun availableLineFilters() =
        if (canViewStock) lineFilters else lineFilters.filterNot { it.first in gapFilters }

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

        // Un comptage se fait les mains prises : l'écran ne doit pas s'éteindre
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize barcode scanner
        barcodeScanner = BarcodeScanner(this) { result -> onScanResult(result) }

        setupRecyclerView()
        setupListeners()
        setupObservers()
        observeQuantityFocus()

        // Load user abilities (close permission, blind mode) then inventory
        inventoryDetailViewModel.loadAbilities()
        inventoryDetailViewModel.loadInventory(inventoryId)
    }

    /**
     * Boutons du bas masqués pendant une saisie de quantité.
     *
     * Avec `adjustResize`, le clavier ampute la hauteur utile et ces trois boutons
     * mangeraient le peu qui reste à la liste. Ils sont rendus GONE et non
     * INVISIBLE : ConstraintLayout les réduit alors à un point, la contrainte
     * basse de la liste retombe sur le bord de l'écran et la liste récupère
     * réellement la place. Aucun d'eux ne sert pendant la frappe — corriger une
     * quantité se fait dans le champ lui-même, pas par l'annulation.
     */
    private fun applyBottomActionsVisibility() {
        binding.btnUndo.visibility = if (undoAvailable && !isEditingQuantity) View.VISIBLE else View.GONE
        binding.btnSynchronize.visibility = if (isEditingQuantity) View.GONE else View.VISIBLE
        // La clôture est irréversible : le bouton reste visible pour qui détient le
        // privilège, mais grisé tant que le comptage n'est pas complet, avec le
        // nombre de lignes restantes — un bouton simplement absent laisserait
        // l'opérateur le chercher.
        binding.btnCloseInventory.visibility = if (closeAllowed && !isEditingQuantity) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.btnCloseInventory.isEnabled = countingComplete
        binding.btnCloseInventory.text = if (countingComplete) {
            getString(R.string.close_inventory)
        } else {
            getString(R.string.close_inventory_remaining, remainingLines)
        }
        // Toute la rangée disparaît pendant la saisie : des boutons GONE laisseraient
        // le conteneur occuper ses marges au lieu de rendre la place à la liste
        binding.llBottomActions.visibility = if (isEditingQuantity) View.GONE else View.VISIBLE
    }

    /**
     * Le focus passe d'une ligne à l'autre en repassant par « aucun focus » : on
     * écoute le focus de la fenêtre entière plutôt que chaque champ, et on décide
     * selon que la vue focalisée appartient ou non à la liste.
     */
    private fun observeQuantityFocus() {
        binding.root.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            val editing = newFocus != null && newFocus.isInside(binding.rvInventoryLines)
            if (editing == isEditingQuantity) return@addOnGlobalFocusChangeListener
            isEditingQuantity = editing
            applyBottomActionsVisibility()
        }
    }

    private fun View.isInside(ancestor: View): Boolean {
        var current: android.view.ViewParent? = parent
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent
        }
        return false
    }

    private fun setupRecyclerView() {
        inventoryLineAdapter = InventoryLineAdapter(
            onQuantityEntered = { line, quantity ->
                inventoryDetailViewModel.updateLineQuantity(line, quantity)
            },
            onLotsClick = { line -> inventoryDetailViewModel.loadLots(line) },
            onAdvance = { from -> focusNextQuantityField(from) }
        )
        binding.rvInventoryLines.adapter = inventoryLineAdapter
        // Le champ de saisie garde le focus pendant le défilement : sans cela, le
        // recyclage d'une ligne éditée redonnerait le focus à un autre produit
        binding.rvInventoryLines.itemAnimator = null
    }

    /**
     * Bascule la liste en mode lot. Les deux modes s'excluent — ils n'exploitent
     * pas le même endpoint — et l'adaptateur n'est monté qu'au premier passage.
     */
    private fun ensureLotAdapter(): InventoryLotLineAdapter {
        lotLineAdapter?.let { return it }
        val adapter = InventoryLotLineAdapter(
            onQuantityEntered = { lotLine, quantity ->
                inventoryDetailViewModel.updateLotLineQuantity(lotLine, quantity)
            },
            onAddLot = { lotLine ->
                lotLine.storeInventoryLineId?.let { showAddLotDialog(it) }
            },
            onAdvance = { from -> focusNextQuantityField(from) }
        )
        adapter.showStock = canViewStock
        lotLineAdapter = adapter
        binding.rvInventoryLines.adapter = adapter
        return adapter
    }

    /**
     * Après validation, le comptage enchaîne sur la ligne suivante : son champ prend
     * le focus et la liste défile juste ce qu'il faut pour l'amener à l'écran.
     *
     * Les lignes suivies par lots sont sautées — elles n'ont pas de quantité propre
     * et ouvrent un dialogue, ce qui interromprait la série au lieu de la poursuivre.
     * En fin de liste, le clavier se referme plutôt que de laisser un focus orphelin.
     */
    private fun focusNextQuantityField(fromPosition: Int) {
        if (fromPosition == RecyclerView.NO_POSITION) return
        // En mode lot toutes les lignes sont saisissables ; en mode produit, celles
        // suivies par lots ouvrent un dialogue et sont donc sautées
        val next = lotLineAdapter?.let { adapter ->
            (fromPosition + 1 until adapter.itemCount).firstOrNull()
        } ?: inventoryLineAdapter.currentList.let { lines ->
            (fromPosition + 1 until lines.size).firstOrNull { lines[it].lotCount == 0 }
        }
        if (next == null) {
            currentFocus?.clearFocus()
            getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(binding.root.windowToken, 0)
            return
        }

        // Le retour de sauvegarde republie la liste : sans ce drapeau, il ramènerait
        // la vue sur la ligne qu'on vient de quitter
        suppressNextAutoScroll = true

        val layoutManager = binding.rvInventoryLines.layoutManager as? LinearLayoutManager
        val last = layoutManager?.findLastCompletelyVisibleItemPosition() ?: RecyclerView.NO_POSITION
        if (layoutManager != null && (last == RecyclerView.NO_POSITION || next > last)) {
            layoutManager.scrollToPositionWithOffset(next, 0)
        }
        // La vue de la ligne visée n'existe qu'après la passe de disposition
        binding.rvInventoryLines.post {
            binding.rvInventoryLines.findViewHolderForAdapterPosition(next)
                ?.itemView
                ?.findViewById<EditText>(R.id.et_quantity)
                ?.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.btnScan.setOnClickListener {
            // ProcessCameraProvider est un singleton de processus : l'écran de scan
            // plein écran délie tous les cas d'usage, y compris l'aperçu continu.
            // On l'arrête proprement plutôt que de le laisser revenir en image figée.
            if (continuousScanActive) setContinuousScan(false)
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

        binding.btnUndo.setOnClickListener {
            inventoryDetailViewModel.undoLastCount()
        }

        binding.btnTorch.setOnClickListener {
            scannerSession?.toggleTorch()
        }
    }

    private fun setupObservers() {
        // Connectivité : bannière hors ligne
        NetworkMonitor.isOnline.observe(this) { online ->
            binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
        }

        // Permissions : clôture réservée + mode aveugle (stock masqué sans privilège)
        inventoryDetailViewModel.abilities.observe(this) { abilities ->
            closeAllowed = InventoryAbilities.CLOSE_INVENTORY in abilities
            applyBottomActionsVisibility()

            // Les adaptateurs ne rebindent que les champs concernés, et seulement si
            // la valeur change réellement (voir InventoryLineAdapter.showStock)
            val showStock = InventoryAbilities.VIEW_STOCK in abilities
            inventoryLineAdapter.showStock = showStock
            lotAdapter?.showStock = showStock
            lotLineAdapter?.showStock = showStock

            canViewStock = showStock
            // Filtre d'écart déjà actif (état restauré) alors que le privilège manque :
            // on retombe sur « Tous » plutôt que de laisser une liste filtrée sur une
            // information interdite
            if (!showStock && inventoryDetailViewModel.getLineFilter() in gapFilters) {
                inventoryDetailViewModel.setLineFilter("NONE")
                binding.btnLineFilter.setText(R.string.filter_all)
            }
        }

        // Annulation disponible dès le premier comptage de la session
        inventoryDetailViewModel.canUndo.observe(this) { canUndo ->
            undoAvailable = canUndo
            applyBottomActionsVisibility()
        }

        // Pastille « à transmettre » sur les lignes non encore synchronisées
        inventoryDetailViewModel.pendingSyncIds.observe(this) { ids ->
            inventoryLineAdapter.pendingSyncIds = ids
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
                        inventoryLineAdapter.submitList(state.lines) { scrollToPendingLine() }
                    }
                    inventoryDetailViewModel.loadProgress()
                }
                is InventoryDetailState.LotLinesLoaded -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.lines.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvInventoryLines.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvInventoryLines.visibility = View.VISIBLE
                        ensureLotAdapter().submitList(state.lines)
                    }
                    inventoryDetailViewModel.loadProgress()
                }
                is InventoryDetailState.LotLineFound -> {
                    // Scan ponctuel en mode lot : la ligne du lot est amenée à l'écran
                    binding.progressBar.visibility = View.GONE
                    val index = inventoryDetailViewModel.getCurrentLotLines()
                        .indexOfFirst { it.id == state.line.id }
                    if (index >= 0) {
                        (binding.rvInventoryLines.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(index, 0)
                    }
                }
                is InventoryDetailState.LotLineSaved -> {
                    // Aucune notification : à raison d'une par quantité saisie, elles
                    // masquaient la liste sans rien apprendre. L'état de transmission
                    // se lit sur la pastille de la ligne et sur le compteur du bouton
                    // de synchronisation.
                    // Le backend recalcule la ligne produit : seule la progression bouge
                    ensureLotAdapter().submitList(
                        inventoryDetailViewModel.getCurrentLotLines().toList()
                    )
                    inventoryDetailViewModel.loadProgress()
                    if (continuousScanActive) {
                        binding.tvScanStatus.visibility = View.VISIBLE
                        binding.tvScanStatus.setBackgroundColor(0x99000000.toInt())
                        binding.tvScanStatus.text = getString(
                            R.string.scan_increment_format,
                            state.line.produitLibelle ?: "",
                            state.line.quantityOnHand ?: 0
                        )
                    }
                }
                is InventoryDetailState.LineFound -> {
                    binding.progressBar.visibility = View.GONE
                    // Scan ponctuel : la ligne est amenée à l'écran, la saisie se
                    // fait dans la liste
                    if (state.line.lotCount > 0) {
                        inventoryDetailViewModel.loadLots(state.line)
                    } else {
                        pendingScrollLineId = state.line.id
                        scrollToPendingLine()
                    }
                }
                is InventoryDetailState.LotsLoaded -> {
                    binding.progressBar.visibility = View.GONE
                    showLotsDialog(state.line, state.lots)
                }
                is InventoryDetailState.LineSaved -> {
                    // Mise à jour en place : recharger la page à chaque quantité
                    // saisie rendrait la liste inutilisable
                    refreshListInPlace(state.line)
                    inventoryDetailViewModel.loadProgress()
                }
                is InventoryDetailState.CountUndone -> {
                    refreshListInPlace(state.line)
                    inventoryDetailViewModel.loadProgress()
                    Toast.makeText(
                        this,
                        getString(R.string.count_undone, state.line.produitLibelle ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is InventoryDetailState.LineIncremented -> {
                    // Mise à jour en place (pas de rechargement complet : cadence de scan)
                    refreshListInPlace(state.line)
                    binding.tvScanStatus.visibility = View.VISIBLE
                    // Rétablit le fond neutre après un éventuel message d'échec
                    binding.tvScanStatus.setBackgroundColor(0x99000000.toInt())
                    binding.tvScanStatus.text = getString(
                        R.string.scan_increment_format,
                        state.line.produitLibelle ?: "",
                        state.line.quantityOnHand ?: 0
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
                    // La progression vient du serveur : une ligne comptée hors ligne
                    // n'y figure qu'une fois synchronisée, ce qui est cohérent —
                    // clôturer avec des saisies non transmises les perdrait.
                    remainingLines = (progress.totalLines - progress.updatedLines).coerceAtLeast(0)
                    countingComplete = progress.totalLines > 0 && remainingLines == 0L
                    applyBottomActionsVisibility()
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
                    // Pendant le scan continu, le message reste affiché sur l'aperçu
                    // et le signal sonore d'échec évite de croire le produit compté
                    if (continuousScanActive) {
                        feedback.failure()
                        binding.tvScanStatus.visibility = View.VISIBLE
                        binding.tvScanStatus.setBackgroundColor(getColor(R.color.warning))
                        binding.tvScanStatus.text = state.message
                    } else {
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Republie la liste après un comptage, sans aller-retour serveur, et ramène la
     * ligne concernée à l'écran. Le rechargement complet ne subsiste que pour les
     * changements de contexte (rayon, recherche, filtre).
     */
    private fun refreshListInPlace(line: StoreInventoryLine) {
        if (suppressNextAutoScroll) {
            suppressNextAutoScroll = false
            inventoryLineAdapter.submitList(inventoryDetailViewModel.getCurrentLines().toList())
            return
        }
        pendingScrollLineId = line.id
        inventoryLineAdapter.submitList(
            inventoryDetailViewModel.getCurrentLines().toList()
        ) { scrollToPendingLine() }
    }

    /**
     * Ramène la dernière ligne saisie ou scannée dans le champ de vision.
     *
     * Une ligne déjà entièrement visible n'est pas déplacée : pendant une rafale de
     * scans, recentrer à chaque code ferait sauter la liste sous les yeux de
     * l'opérateur. Au-delà d'un écran d'écart on repositionne sèchement plutôt que
     * d'animer, un smooth scroll sur plusieurs centaines de lignes étant interminable.
     */
    private fun scrollToPendingLine() {
        val lineId = pendingScrollLineId ?: return
        val index = inventoryLineAdapter.currentList.indexOfFirst { it.id == lineId }
        if (index < 0) return
        pendingScrollLineId = null

        val layoutManager = binding.rvInventoryLines.layoutManager as? LinearLayoutManager ?: return
        val first = layoutManager.findFirstCompletelyVisibleItemPosition()
        val last = layoutManager.findLastCompletelyVisibleItemPosition()
        if (first != RecyclerView.NO_POSITION && index in first..last) return

        val visibleCount = if (first == RecyclerView.NO_POSITION) 0 else last - first + 1
        val reference = if (first == RecyclerView.NO_POSITION) index else first
        if (visibleCount == 0 || kotlin.math.abs(index - reference) > visibleCount) {
            // Ligne en tête de liste : le contexte utile est ce qui suit
            layoutManager.scrollToPositionWithOffset(index, 0)
        } else {
            binding.rvInventoryLines.smoothScrollToPosition(index)
        }
    }

    /**
     * Ouvre le dialogue avec le champ prêt à la saisie : focus, clavier déployé et
     * valeur existante sélectionnée, pour que la frappe la remplace sans effacement
     * préalable.
     *
     * Le mode de saisie logicielle se règle avant `show()` (la fenêtre est créée à
     * ce moment), et le focus se demande après, une vue non attachée ne pouvant pas
     * le prendre. SOFT_INPUT_STATE_VISIBLE plutôt que ALWAYS_VISIBLE : sur les
     * terminaux à clavier physique, le clavier logiciel reste inutile.
     */
    private fun AlertDialog.showFocused(field: EditText) {
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        show()
        field.requestFocus()
        field.selectAll()
        // « Terminé » vaut validation : compter sans jamais quitter le pavé numérique.
        // Le bouton n'existe qu'une fois le dialogue affiché, d'où le câblage ici.
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }
    }

    // ── Scan continu (caméra embarquée, chaque scan = +1) ────────────────────

    /**
     * ML Kit analyse sur un thread dédié : le traitement d'un code doit repasser sur
     * le thread principal (ViewModel, vues).
     */
    private fun onContinuousScan(text: String) = runOnUiThread {
        val now = SystemClock.elapsedRealtime()
        // Anti-rebond : ignore le même code pendant 1,5 s
        if (text == lastScanText && now - lastScanTime < 1500) return@runOnUiThread
        lastScanText = text
        lastScanTime = now
        feedback.success()
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
        binding.previewView.visibility = if (active) View.VISIBLE else View.GONE
        binding.btnTorch.visibility = if (active) View.VISIBLE else View.GONE
        binding.tvScanStatus.visibility = View.GONE
        binding.btnScanContinuous.setText(
            if (active) R.string.continuous_scan_stop else R.string.continuous_scan
        )
        if (active) {
            // La session se lie au cycle de vie de l'activité : CameraX suspend et
            // reprend l'aperçu de lui-même, sans onResume/onPause manuels
            scannerSession = CameraScannerSession(
                this,
                this,
                binding.previewView
            ) { barcode, _ -> onContinuousScan(barcode) }.also { it.start() }
        } else {
            scannerSession?.stop()
            scannerSession = null
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

    override fun onDestroy() {
        scannerSession?.stop()
        scannerSession = null
        feedback.release()
        super.onDestroy()
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
                    feedback.success()
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
        val options = availableLineFilters()
        val labels = options.map { getString(it.second) }.toTypedArray()
        val current = inventoryDetailViewModel.getLineFilter() ?: "NONE"
        val checkedIndex = options.indexOfFirst { it.first == current }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_filter)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                inventoryDetailViewModel.setLineFilter(options[which].first)
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
            .create()
            .showFocused(etQuantity)
    }

    /**
     * @param targetLineId ligne d'inventaire à laquelle rattacher le lot. Null en
     *   mode produit : le dialogue des lots porte déjà la ligne courante.
     */
    private fun showAddLotDialog(targetLineId: Long? = null) {
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
                if (targetLineId != null) {
                    inventoryDetailViewModel.addLotToLine(targetLineId, numLot, expiry, quantity)
                } else {
                    inventoryDetailViewModel.addLot(numLot, expiry, quantity)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            // Après un scan GS1, le n° de lot et la péremption sont déjà remplis :
            // seule la quantité reste à saisir. Sans scan, le n° de lot est vide et
            // c'est par lui qu'il faut commencer.
            .showFocused(if (etNumLot.text.isNullOrBlank()) etNumLot else etQuantity)
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

    /** Résultat du scan ponctuel (caméra plein écran) */
    private fun onScanResult(result: ScanResult) {
        when (result) {
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
