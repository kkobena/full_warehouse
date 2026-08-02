package com.kobe.warehouse.inventory.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.inventory.data.model.InventoryLot
import com.kobe.warehouse.inventory.data.model.InventoryProgress
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.repository.CountingConflictException
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.utils.Gs1Data
import com.kobe.warehouse.inventory.utils.Gs1Parser
import kotlinx.coroutines.launch

sealed class InventoryDetailState {
    object Idle : InventoryDetailState()
    object Loading : InventoryDetailState()
    data class InventoryLoaded(val inventory: StoreInventory) : InventoryDetailState()
    data class RayonsLoaded(val rayons: List<Rayon>) : InventoryDetailState()
    data class LinesLoaded(val lines: List<StoreInventoryLine>) : InventoryDetailState()
    data class LineFound(val line: StoreInventoryLine) : InventoryDetailState()
    data class LineSaved(val synced: Boolean) : InventoryDetailState()
    data class LineIncremented(val line: StoreInventoryLine, val synced: Boolean) : InventoryDetailState()
    data class LotsLoaded(val line: StoreInventoryLine, val lots: List<InventoryLot>) : InventoryDetailState()
    data class SyncSuccess(val saved: Int, val failed: Int, val conflicted: Int = 0) : InventoryDetailState()
    data class InventoryClosed(val itemsCount: Int) : InventoryDetailState()
    data class ProgressLoaded(val progress: InventoryProgress) : InventoryDetailState()
    data class CloseSummaryReady(val summary: CloseSummary) : InventoryDetailState()
    data class Error(val message: String) : InventoryDetailState()
}

/**
 * Récapitulatif présenté avant la clôture (irréversible)
 */
data class CloseSummary(
    val totalLines: Long,
    val countedLines: Long,
    val remainingLines: Long,
    val gapLineCount: Int,
    val gapValueAchat: Long,
    val gapValueVente: Long
)

class InventoryDetailViewModel(private val inventoryRepository: InventoryRepository) : ViewModel() {

    private val _inventoryDetailState = MutableLiveData<InventoryDetailState>(InventoryDetailState.Idle)
    val inventoryDetailState: LiveData<InventoryDetailState> = _inventoryDetailState

    /** Nombre de lignes modifiées localement en attente de synchronisation */
    val pendingSyncCount: LiveData<Int> = inventoryRepository.pendingSyncCount().asLiveData()

    /** Nombre de lignes rejetées pour comptage concurrent (arbitrage requis) */
    val conflictCount: LiveData<Int> = inventoryRepository.conflictCount().asLiveData()

    /** Recharge les lignes en conflit depuis le serveur (abandon de la saisie locale) */
    fun resolveConflicts() {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.resolveConflicts(inventoryId)
            refreshLines()
        }
    }

    /** Permissions ACTION de l'utilisateur (pr-cloture-inventaire, pr-voir-stock-inventaire…) */
    private val _abilities = MutableLiveData<Set<String>>(emptySet())
    val abilities: LiveData<Set<String>> = _abilities

    fun loadAbilities() {
        viewModelScope.launch {
            inventoryRepository.fetchAbilities().fold(
                onSuccess = { _abilities.value = it },
                onFailure = { /* fail-closed : permissions restreintes */ }
            )
        }
    }

    private var currentInventory: StoreInventory? = null
    private var currentInventoryId: Long? = null
    private var currentRayonId: Long? = null
    private var currentSearch: String? = null
    private var currentFilter: String? = null
    private var currentLines = mutableListOf<StoreInventoryLine>()
    private var currentLotLine: StoreInventoryLine? = null

    /** Dernier scan GS1 décodé (pour préremplir le dialogue d'ajout de lot) */
    var lastScannedGs1: Gs1Data? = null
        private set

    fun loadInventory(inventoryId: Long) {
        currentInventoryId = inventoryId
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.getInventory(inventoryId).fold(
                onSuccess = { inventory ->
                    currentInventory = inventory
                    _inventoryDetailState.value = InventoryDetailState.InventoryLoaded(inventory)
                    // Auto-load rayons restricted to the inventory's storage (if any)
                    loadRayons()
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement de l'inventaire"
                    )
                }
            )
        }
    }

    fun loadRayons() {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.getRayons(storageId = currentInventory?.storage?.id).fold(
                onSuccess = { rayons ->
                    _inventoryDetailState.value = InventoryDetailState.RayonsLoaded(rayons)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement des rayons"
                    )
                }
            )
        }
    }

    fun loadInventoryLines(inventoryId: Long, rayonId: Long? = null) {
        currentInventoryId = inventoryId
        currentRayonId = rayonId
        refreshLines()
    }

    /** Recherche texte (serveur en ligne, cache hors ligne) */
    fun setSearch(query: String?) {
        currentSearch = query?.trim()?.takeIf { it.isNotEmpty() }
        refreshLines()
    }

    /** Filtre de comptage : NONE / UPDATED / NOT_UPDATED / GAP / GAP_POSITIF / GAP_NEGATIF */
    fun setLineFilter(filter: String?) {
        currentFilter = filter?.takeIf { it != "NONE" }
        refreshLines()
    }

    fun getLineFilter(): String? = currentFilter

    /**
     * Reload lines keeping the current rayon/search/filter context
     */
    fun reloadLines() = refreshLines()

    private fun refreshLines() {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.getInventoryLines(
                inventoryId,
                currentRayonId,
                currentSearch,
                currentFilter
            ).fold(
                onSuccess = { lines ->
                    val sorted = lines.sortedBy { it.produitLibelle?.lowercase().orEmpty() }
                    currentLines.clear()
                    currentLines.addAll(sorted)
                    _inventoryDetailState.value = InventoryDetailState.LinesLoaded(sorted)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement des lignes"
                    )
                }
            )
        }
    }

    /**
     * Unified scan entry point (camera single/continuous, HID scanner).
     * Decodes GS1 DataMatrix when present (CIP13, lot, expiry), finds the
     * inventory line, then either surfaces it (dialog) or auto-increments.
     * A product absent from the inventory cannot be counted.
     */
    fun onBarcodeScanned(raw: String, autoIncrement: Boolean = false) {
        val gs1 = Gs1Parser.parse(raw)
        lastScannedGs1 = gs1
        val candidates = gs1?.barcodeCandidates() ?: buildList {
            add(raw)
            if (raw.length == 14 && raw.all { it.isDigit() }) add(raw.substring(1))
        }

        val localMatch = currentLines.firstOrNull { line ->
            candidates.any { line.matchesBarcode(it) }
        }
        if (localMatch != null) {
            handleScannedLine(localMatch, gs1, autoIncrement)
            return
        }

        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.searchProductByBarcode(candidates.first()).fold(
                onSuccess = { product ->
                    val line = currentLines.firstOrNull { it.produitId == product.id }
                    if (line != null) {
                        handleScannedLine(line, gs1, autoIncrement)
                    } else {
                        _inventoryDetailState.value = InventoryDetailState.Error(
                            "${product.libelle ?: "Ce produit"} n'est pas dans cet inventaire"
                        )
                    }
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Produit non trouvé"
                    )
                }
            )
        }
    }

    private fun handleScannedLine(line: StoreInventoryLine, gs1: Gs1Data?, autoIncrement: Boolean) {
        if (!autoIncrement) {
            _inventoryDetailState.value = InventoryDetailState.LineFound(line)
            return
        }
        // AI 37 (colisage) : un scan de carton incrémente de la quantité contenue
        val step = gs1?.quantity ?: 1
        if (line.lotCount > 0 && gs1?.lotNumber != null) {
            incrementLotFromScan(line, gs1, step)
        } else {
            incrementLine(line, step)
        }
    }

    /** Comptage « chaque scan = +step » sur la ligne (local d'abord) */
    private fun incrementLine(line: StoreInventoryLine, step: Int = 1) {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            inventoryRepository.saveLineQuantity(
                inventoryId,
                line,
                (line.quantityOnHand ?: 0) + step
            ).fold(
                onSuccess = { result ->
                    replaceCurrentLine(result.line)
                    _inventoryDetailState.value =
                        InventoryDetailState.LineIncremented(result.line, result.synced)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la sauvegarde"
                    )
                }
            )
        }
    }

    /**
     * « +1 » sur le lot identifié par le n° de lot du DataMatrix : incrémente le
     * lot existant, ou le crée (péremption pré-remplie) s'il est inconnu.
     * Le backend recalcule la quantité de la ligne parente.
     */
    private fun incrementLotFromScan(line: StoreInventoryLine, gs1: Gs1Data, step: Int = 1) {
        viewModelScope.launch {
            inventoryRepository.getInventoryLots(line.id).fold(
                onSuccess = { lots ->
                    val existing = lots.firstOrNull {
                        it.numLot?.equals(gs1.lotNumber, ignoreCase = true) == true
                    }
                    val operation = if (existing != null) {
                        inventoryRepository.updateInventoryLot(
                            existing.copy(quantityOnHand = (existing.quantityOnHand ?: 0) + step)
                        )
                    } else {
                        inventoryRepository.createInventoryLot(
                            line.id,
                            InventoryLot(
                                storeInventoryLineId = line.id,
                                numLot = gs1.lotNumber,
                                expiryDate = gs1.expiryIso,
                                quantityOnHand = step
                            )
                        )
                    }
                    operation.fold(
                        onSuccess = {
                            inventoryRepository.getInventoryLots(line.id).fold(
                                onSuccess = { fresh ->
                                    val refreshed = refreshLineFromLots(line, fresh)
                                    _inventoryDetailState.value =
                                        InventoryDetailState.LineIncremented(refreshed, true)
                                },
                                onFailure = {
                                    _inventoryDetailState.value =
                                        InventoryDetailState.LineIncremented(line, true)
                                }
                            )
                        },
                        onFailure = { error ->
                            _inventoryDetailState.value = InventoryDetailState.Error(
                                error.message ?: "Erreur lors de la sauvegarde du lot"
                            )
                        }
                    )
                },
                onFailure = {
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        "Hors ligne : comptage par lot indisponible pour ce produit"
                    )
                }
            )
        }
    }

    private fun replaceCurrentLine(line: StoreInventoryLine) {
        val index = currentLines.indexOfFirst { it.id == line.id }
        if (index >= 0) {
            currentLines[index] = line
        } else {
            currentLines.add(line)
        }
    }

    /**
     * Local-first save: never fails on network errors — the line is persisted
     * in Room and pushed in background if the server is unreachable.
     */
    fun updateLineQuantity(line: StoreInventoryLine, quantity: Int) {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.saveLineQuantity(inventoryId, line, quantity).fold(
                onSuccess = { result ->
                    val index = currentLines.indexOfFirst { it.id == line.id }
                    if (index >= 0) {
                        currentLines[index] = result.line
                    } else {
                        currentLines.add(result.line)
                    }
                    _inventoryDetailState.value = InventoryDetailState.LineSaved(result.synced)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la sauvegarde"
                    )
                    // Conflit : la ligne serveur fait foi, on la recharge immédiatement
                    if (error is CountingConflictException) {
                        refreshLines()
                    }
                }
            )
        }
    }

    /**
     * Manual synchronization of all locally modified lines (including those
     * previously rejected by the server)
     */
    fun synchronizeLines() {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.syncPendingLines(includeErrors = true).fold(
                onSuccess = { result ->
                    if (result.saved == 0 && result.failed == 0) {
                        _inventoryDetailState.value = InventoryDetailState.Error(
                            "Aucune ligne en attente de synchronisation"
                        )
                    } else {
                        _inventoryDetailState.value = InventoryDetailState.SyncSuccess(
                            saved = result.saved,
                            failed = result.failed,
                            conflicted = result.conflicted
                        )
                        reloadLines()
                    }
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur de synchronisation"
                    )
                }
            )
        }
    }

    /**
     * Load the lots of a line (products tracked by lot are counted lot by lot)
     */
    fun loadLots(line: StoreInventoryLine) {
        currentLotLine = line
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.getInventoryLots(line.id).fold(
                onSuccess = { lots ->
                    refreshLineFromLots(line, lots)
                    _inventoryDetailState.value = InventoryDetailState.LotsLoaded(line, lots)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement des lots"
                    )
                }
            )
        }
    }

    fun updateLotQuantity(lot: InventoryLot, quantity: Int) {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.updateInventoryLot(lot.copy(quantityOnHand = quantity)).fold(
                onSuccess = { reloadCurrentLots() },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la sauvegarde du lot"
                    )
                }
            )
        }
    }

    fun addLot(numLot: String, expiryDate: String?, quantity: Int) {
        val line = currentLotLine ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            val lot = InventoryLot(
                storeInventoryLineId = line.id,
                numLot = numLot,
                expiryDate = expiryDate,
                quantityOnHand = quantity
            )
            inventoryRepository.createInventoryLot(line.id, lot).fold(
                onSuccess = { reloadCurrentLots() },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la création du lot"
                    )
                }
            )
        }
    }

    fun deleteLot(lot: InventoryLot) {
        val lotId = lot.id ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.deleteInventoryLot(lotId).fold(
                onSuccess = { reloadCurrentLots() },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la suppression du lot"
                    )
                }
            )
        }
    }

    private suspend fun reloadCurrentLots() {
        val line = currentLotLine ?: return
        inventoryRepository.getInventoryLots(line.id).fold(
            onSuccess = { lots ->
                val refreshed = refreshLineFromLots(line, lots)
                _inventoryDetailState.value = InventoryDetailState.LotsLoaded(refreshed, lots)
            },
            onFailure = { error ->
                _inventoryDetailState.value = InventoryDetailState.Error(
                    error.message ?: "Erreur lors du chargement des lots"
                )
            }
        )
    }

    /**
     * Keep the local line consistent with its lots: quantityOnHand = SUM(lots),
     * mirroring the backend sync performed on lot update.
     */
    private fun refreshLineFromLots(line: StoreInventoryLine, lots: List<InventoryLot>): StoreInventoryLine {
        if (lots.none { it.quantityOnHand != null }) return line
        val sum = lots.sumOf { it.quantityOnHand ?: 0 }
        val refreshed = line.copy(
            quantityOnHand = sum,
            gap = sum - line.quantityInit,
            updated = true,
            lotCount = lots.size
        )
        val index = currentLines.indexOfFirst { it.id == line.id }
        if (index >= 0) {
            currentLines[index] = refreshed
        }
        currentLotLine = refreshed
        return refreshed
    }

    fun loadProgress() {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            inventoryRepository.getProgress(inventoryId).fold(
                onSuccess = { progress ->
                    _inventoryDetailState.value = InventoryDetailState.ProgressLoaded(progress)
                },
                onFailure = { /* non bloquant */ }
            )
        }
    }

    /**
     * Prépare le récapitulatif pré-clôture : progression (total/comptés/restants)
     * + écarts valorisés calculés sur les lignes avec écart (achat et vente)
     */
    fun prepareCloseSummary() {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            val progress = inventoryRepository.getProgress(inventoryId).getOrNull()
            val gapLines = inventoryRepository
                .getInventoryLines(inventoryId, selectedFilter = "GAP")
                .getOrDefault(emptyList())

            val summary = CloseSummary(
                totalLines = progress?.totalLines ?: currentLines.size.toLong(),
                countedLines = progress?.updatedLines ?: currentLines.count { it.updated }.toLong(),
                remainingLines = (progress?.totalLines ?: 0) - (progress?.updatedLines ?: 0),
                gapLineCount = gapLines.size,
                gapValueAchat = gapLines.sumOf { (it.gap ?: 0).toLong() * (it.prixAchat ?: 0) },
                gapValueVente = gapLines.sumOf { (it.gap ?: 0).toLong() * (it.prixUni ?: 0) }
            )
            _inventoryDetailState.value = InventoryDetailState.CloseSummaryReady(summary)
        }
    }

    fun closeInventory(inventoryId: Long) {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.closeInventory(inventoryId).fold(
                onSuccess = { count ->
                    _inventoryDetailState.value = InventoryDetailState.InventoryClosed(count)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la clôture"
                    )
                }
            )
        }
    }

    fun getCurrentLines(): List<StoreInventoryLine> = currentLines
}
