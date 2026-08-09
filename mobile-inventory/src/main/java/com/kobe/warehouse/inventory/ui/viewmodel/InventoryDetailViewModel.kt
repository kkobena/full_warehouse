package com.kobe.warehouse.inventory.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.inventory.data.model.InventoryLot
import com.kobe.warehouse.inventory.data.model.InventoryLotLine
import com.kobe.warehouse.inventory.data.model.InventoryProgress
import com.kobe.warehouse.inventory.data.model.Rayon
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.data.repository.CountingConflictException
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.utils.Gs1Data
import com.kobe.warehouse.inventory.utils.Gs1Parser
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class InventoryDetailState {
    object Idle : InventoryDetailState()
    object Loading : InventoryDetailState()
    data class InventoryLoaded(val inventory: StoreInventory) : InventoryDetailState()
    data class RayonsLoaded(val rayons: List<Rayon>) : InventoryDetailState()
    data class LinesLoaded(val lines: List<StoreInventoryLine>) : InventoryDetailState()
    data class LotLinesLoaded(val lines: List<InventoryLotLine>) : InventoryDetailState()
    data class LineFound(val line: StoreInventoryLine) : InventoryDetailState()
    data class LineSaved(val line: StoreInventoryLine, val synced: Boolean) : InventoryDetailState()
    data class LineIncremented(val line: StoreInventoryLine, val synced: Boolean) : InventoryDetailState()
    data class CountUndone(val line: StoreInventoryLine) : InventoryDetailState()
    data class LotLineSaved(val line: InventoryLotLine, val synced: Boolean) : InventoryDetailState()
    data class LotLineFound(val line: InventoryLotLine) : InventoryDetailState()
    data class LotsLoaded(val line: StoreInventoryLine, val lots: List<InventoryLot>) : InventoryDetailState()
    data class SyncSuccess(val saved: Int, val failed: Int, val conflicted: Int = 0) : InventoryDetailState()
    data class ProgressLoaded(val progress: InventoryProgress) : InventoryDetailState()
    data class Error(val message: String) : InventoryDetailState()
}

class InventoryDetailViewModel(private val inventoryRepository: InventoryRepository) : ViewModel() {

    private val _inventoryDetailState = MutableLiveData<InventoryDetailState>(InventoryDetailState.Idle)
    val inventoryDetailState: LiveData<InventoryDetailState> = _inventoryDetailState

    /** Nombre de lignes modifiées localement en attente de synchronisation */
    val pendingSyncCount: LiveData<Int> = inventoryRepository.pendingSyncCount().asLiveData()

    /** Nombre de lignes rejetées pour comptage concurrent (arbitrage requis) */
    val conflictCount: LiveData<Int> = inventoryRepository.conflictCount().asLiveData()

    /** Lignes saisies non encore transmises, signalées par une pastille */
    val pendingLotSyncIds: LiveData<Set<Long>> =
        inventoryRepository.pendingLotSyncIds().map { it.toSet() }.asLiveData()

    /** Idem pour la vue produit */
    val pendingSyncIds: LiveData<Set<Long>> =
        inventoryRepository.pendingSyncIds().map { it.toSet() }.asLiveData()

    /** Recharge les lignes en conflit depuis le serveur (abandon de la saisie locale) */
    fun resolveConflicts() {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.resolveConflicts(inventoryId)
            refreshLines()
        }
    }

    /** Permissions ACTION de l'utilisateur (pr-voir-stock-inventaire…) */
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

    /**
     * Mode de saisie, résolu avant le premier chargement : par lot quand
     * APP_GESTION_LOT_INVENTAIRE est actif, par produit sinon. Les deux modes
     * s'excluent — ils n'appellent pas le même endpoint — d'où l'attente.
     */
    var gestionLot: Boolean = false
        private set

    fun loadInventory(inventoryId: Long) {
        currentInventoryId = inventoryId
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            gestionLot = inventoryRepository.isGestionLotEnabled()
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
     * Bloc de commandes (scan / rayon / recherche) replié. Porté par le ViewModel
     * pour survivre à une rotation : l'opérateur qui a replié une fois n'a pas à
     * recommencer parce que l'écran a tourné.
     */
    var controlsCollapsed = false

    /**
     * Reload lines keeping the current rayon/search/filter context
     */
    fun reloadLines() = refreshLines()

    private fun refreshLines() {
        if (gestionLot) {
            refreshLotLines()
            return
        }
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
                    // Ordre du backend conservé tel quel (ORDER BY code_cip, libelle, id
                    // dans StoreInventoryLineFilterBuilder.buildPage) : un tri local par
                    // libellé le remplaçait et désynchronisait l'appli du listing papier.
                    currentLines.clear()
                    currentLines.addAll(lines)
                    _inventoryDetailState.value = InventoryDetailState.LinesLoaded(lines)
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
        if (gestionLot) {
            onBarcodeScannedLotMode(gs1, candidates, autoIncrement)
            return
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
            // Tous les candidats sont essayés, pas seulement le premier : le
            // référentiel peut porter le GTIN-14 avec son zéro de tête là où le
            // DataMatrix donne l'EAN-13, ou l'inverse
            val product = candidates.firstNotNullOfOrNull { code ->
                inventoryRepository.searchProductByBarcode(code).getOrNull()
            }
            if (product == null) {
                _inventoryDetailState.value = InventoryDetailState.Error(
                    "Produit inconnu pour le code ${candidates.joinToString(" / ")}"
                )
                return@launch
            }
            val line = currentLines.firstOrNull { it.produitId == product.id }
            if (line != null) {
                handleScannedLine(line, gs1, autoIncrement)
            } else {
                _inventoryDetailState.value = InventoryDetailState.Error(
                    "${product.libelle ?: "Ce produit"} n'est pas dans cet inventaire"
                )
            }
        }
    }

    /**
     * Scan en mode lot : la cible est un lot, pas un produit.
     *
     * Le DataMatrix pharma porte le n° de lot (AI 10), qui désigne la ligne
     * exactement. Sans lui — code-barres linéaire — on ne peut trancher que si le
     * produit n'a qu'un lot dans l'inventaire ; au-delà, désigner un lot au hasard
     * fausserait le comptage, donc on demande à l'opérateur de choisir.
     */
    private fun onBarcodeScannedLotMode(
        gs1: Gs1Data?,
        candidates: List<String>,
        autoIncrement: Boolean
    ) {
        viewModelScope.launch {
            var matches = currentLotLines.filter { lotLine ->
                candidates.any { it.equals(lotLine.produitCip, ignoreCase = true) }
            }
            if (matches.isEmpty()) {
                // La vue à plat ne porte pas l'EAN : on passe par la recherche produit
                val product = candidates.firstNotNullOfOrNull { code ->
                    inventoryRepository.searchProductByBarcode(code).getOrNull()
                }
                if (product == null) {
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        "Produit inconnu pour le code ${candidates.joinToString(" / ")}"
                    )
                    return@launch
                }
                matches = currentLotLines.filter { it.produitId == product.id }
                if (matches.isEmpty()) {
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        "${product.libelle ?: "Ce produit"} n'est pas dans cet inventaire"
                    )
                    return@launch
                }
            }

            val scannedLot = gs1?.lotNumber
            val target = when {
                scannedLot != null ->
                    matches.firstOrNull { it.numLot.equals(scannedLot, ignoreCase = true) }

                matches.size == 1 -> matches.first()
                else -> null
            }

            if (target == null) {
                val label = matches.first().produitLibelle ?: "Ce produit"
                _inventoryDetailState.value = InventoryDetailState.Error(
                    if (scannedLot != null) {
                        "Lot $scannedLot inconnu pour $label — utilisez « + Lot »"
                    } else {
                        "$label a ${matches.size} lots : choisissez la ligne à compter"
                    }
                )
                return@launch
            }

            if (!autoIncrement) {
                _inventoryDetailState.value = InventoryDetailState.LotLineFound(target)
                return@launch
            }
            updateLotLineQuantity(target, (target.quantityOnHand ?: 0) + (gs1?.quantity ?: 1))
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
                    rememberUndo(line)
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

    /**
     * Comptage précédent de la dernière ligne saisie, pour l'annulation.
     *
     * On mémorise l'état *avant* écriture : en scan continu, un code lu deux fois
     * ou une boîte voisine captée par erreur ne se rattrape autrement qu'en
     * recalculant la quantité de tête. Un seul niveau d'annulation — au-delà,
     * l'opérateur ne sait plus ce qu'il défait.
     */
    private var undoTarget: StoreInventoryLine? = null

    /** Équivalent en mode lot : l'annulation rétablit la quantité du lot */
    private var undoLotTarget: InventoryLotLine? = null

    private val _canUndo = MutableLiveData(false)
    val canUndo: LiveData<Boolean> = _canUndo

    private fun rememberUndo(before: StoreInventoryLine) {
        undoTarget = before
        undoLotTarget = null
        _canUndo.value = true
    }

    private fun rememberLotUndo(before: InventoryLotLine) {
        undoLotTarget = before
        undoTarget = null
        _canUndo.value = true
    }

    /** Libellé du dernier produit compté, pour l'annonce d'annulation */
    fun undoTargetLabel(): String? = undoTarget?.produitLibelle ?: undoLotTarget?.produitLibelle

    /** Rétablit la quantité qu'avait la ligne (ou le lot) avant le dernier comptage */
    fun undoLastCount() {
        undoLotTarget?.let { lotTarget ->
            undoLotTarget = null
            _canUndo.value = false
            updateLotLineQuantity(lotTarget, lotTarget.quantityOnHand ?: 0)
            return
        }
        val target = undoTarget ?: return
        val inventoryId = currentInventoryId ?: return
        undoTarget = null
        _canUndo.value = false
        viewModelScope.launch {
            inventoryRepository.saveLineQuantity(
                inventoryId,
                currentLines.firstOrNull { it.id == target.id } ?: target,
                target.quantityOnHand ?: 0
            ).fold(
                onSuccess = { result ->
                    replaceCurrentLine(result.line)
                    _inventoryDetailState.value =
                        InventoryDetailState.CountUndone(result.line)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de l'annulation"
                    )
                }
            )
        }
    }

    /** Lignes de la vue à plat actuellement affichées (mode lot) */
    private var currentLotLines = mutableListOf<InventoryLotLine>()

    fun getCurrentLotLines(): List<InventoryLotLine> = currentLotLines

    private fun refreshLotLines() {
        val inventoryId = currentInventoryId ?: return
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.getInventoryLotLines(
                inventoryId,
                currentRayonId,
                currentSearch,
                currentFilter
            ).fold(
                onSuccess = { lines ->
                    currentLotLines.clear()
                    currentLotLines.addAll(lines)
                    _inventoryDetailState.value = InventoryDetailState.LotLinesLoaded(lines)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors du chargement des lots"
                    )
                }
            )
        }
    }

    /**
     * Comptage direct d'un lot depuis la vue à plat.
     *
     * Le backend recalcule la quantité de la ligne produit à partir de ses lots ;
     * seule la progression est donc rechargée ensuite, pas toute la liste.
     *
     * Une ligne sans lot n'a rien à écrire côté lot : la saisie part sur l'API ligne
     * produit — cf. [updateLotLessLineQuantity].
     */
    fun updateLotLineQuantity(lotLine: InventoryLotLine, quantity: Int) {
        if (lotLine.isLotLess()) {
            updateLotLessLineQuantity(lotLine, quantity)
            return
        }
        val lotId = lotLine.id ?: return
        val inventoryId = currentInventoryId ?: return
        rememberLotUndo(lotLine)
        viewModelScope.launch {
            // Local d'abord : hors ligne, la saisie est conservee et transmise plus tard
            inventoryRepository.saveLotLineQuantity(inventoryId, lotLine, quantity).fold(
                onSuccess = { result ->
                    val index = currentLotLines.indexOfFirst { it.id == lotId }
                    if (index >= 0) currentLotLines[index] = result.line
                    _inventoryDetailState.value =
                        InventoryDetailState.LotLineSaved(result.line, result.synced)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la sauvegarde du lot"
                    )
                }
            )
        }
    }

    /**
     * Comptage d'une ligne de la grille lot qui n'a aucun lot rattaché.
     *
     * Ces produits sont dans le périmètre de l'inventaire mais n'ont pas d'`inventory_lot`
     * (aucun lot, ou tous à zéro). Ils se comptent comme en vue produit — même API, même
     * cache local, donc même tenue hors ligne et même arbitrage des comptages concurrents.
     */
    private fun updateLotLessLineQuantity(lotLine: InventoryLotLine, quantity: Int) {
        val lineId = lotLine.storeInventoryLineId ?: return
        val inventoryId = currentInventoryId ?: return
        rememberLotUndo(lotLine)
        viewModelScope.launch {
            inventoryRepository.saveLineQuantity(
                inventoryId,
                StoreInventoryLine(
                    id = lineId,
                    produitId = lotLine.produitId,
                    produitLibelle = lotLine.produitLibelle,
                    produitCip = lotLine.produitCip,
                    quantityOnHand = lotLine.quantityOnHand,
                    quantityInit = lotLine.quantityInit ?: 0,
                    gap = lotLine.gap,
                    updated = lotLine.updated,
                    version = lotLine.version
                ),
                quantity
            ).fold(
                onSuccess = { result ->
                    val saved = lotLine.copy(
                        quantityOnHand = result.line.quantityOnHand,
                        quantityInit = result.line.quantityInit,
                        gap = result.line.gap,
                        updated = result.line.updated,
                        version = result.line.version
                    )
                    val index = currentLotLines.indexOfFirst {
                        it.isLotLess() && it.storeInventoryLineId == lineId
                    }
                    if (index >= 0) currentLotLines[index] = saved
                    _inventoryDetailState.value =
                        InventoryDetailState.LotLineSaved(saved, result.synced)
                },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la sauvegarde de la ligne"
                    )
                }
            )
        }
    }

    /** Ajout d'un lot depuis la vue à plat (lot trouvé en rayon, absent de l'inventaire) */
    fun addLotToLine(storeInventoryLineId: Long, numLot: String, expiry: String?, quantity: Int) {
        viewModelScope.launch {
            _inventoryDetailState.value = InventoryDetailState.Loading
            inventoryRepository.createInventoryLot(
                storeInventoryLineId,
                InventoryLot(
                    storeInventoryLineId = storeInventoryLineId,
                    numLot = numLot,
                    expiryDate = expiry,
                    quantityOnHand = quantity
                )
            ).fold(
                onSuccess = { refreshLotLines() },
                onFailure = { error ->
                    _inventoryDetailState.value = InventoryDetailState.Error(
                        error.message ?: "Erreur lors de la création du lot"
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
        // Pas d'état Loading : la saisie enchaîne ligne après ligne, un indicateur
        // plein écran à chaque quantité rendrait la liste inutilisable
        viewModelScope.launch {
            inventoryRepository.saveLineQuantity(inventoryId, line, quantity).fold(
                onSuccess = { result ->
                    rememberUndo(line)
                    replaceCurrentLine(result.line)
                    _inventoryDetailState.value =
                        InventoryDetailState.LineSaved(result.line, result.synced)
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
            val lotsSaved = inventoryRepository.syncPendingLotLines().getOrDefault(0)
            inventoryRepository.syncPendingLines(includeErrors = true).fold(
                onSuccess = { result ->
                    if (result.saved == 0 && result.failed == 0 && lotsSaved == 0) {
                        _inventoryDetailState.value = InventoryDetailState.Error(
                            "Aucune ligne en attente de synchronisation"
                        )
                    } else {
                        _inventoryDetailState.value = InventoryDetailState.SyncSuccess(
                            saved = result.saved + lotsSaved,
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

    fun getCurrentLines(): List<StoreInventoryLine> = currentLines
}
